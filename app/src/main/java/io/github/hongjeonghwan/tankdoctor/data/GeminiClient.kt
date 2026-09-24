package io.github.hongjeonghwan.tankdoctor.data

import android.util.Base64
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.net.HttpURLConnection
import java.net.SocketTimeoutException
import java.net.URL

class GeminiException(message: String) : Exception(message)

data class ModelOption(val id: String, val label: String, val description: String)

val MODELS = listOf(
    ModelOption("gemini-3.8-flash", "Gemini 3.8 Flash (추천)", "가장 정확해요 · 무료 할당량 있음"),
    ModelOption("gemini-3.6-flash", "Gemini 3.6 Flash", "빠르고 균형 잡힌 모델"),
    ModelOption("gemini-3.1-flash-lite", "Gemini 3.1 Flash-Lite", "가장 빠르고 저렴 · 정확도는 낮음"),
)
val DEFAULT_MODEL = MODELS.first().id

enum class TankType(val label: String, val promptLabel: String) {
    FRESH("담수", "일반 담수 어항"),
    PLANTED("수초항", "수초 어항(담수)"),
    MARINE("해수", "해수어/산호 어항"),
}

object GeminiClient {
    private const val BASE_URL = "https://generativelanguage.googleapis.com/v1beta/models/"

    suspend fun diagnose(
        apiKey: String,
        model: String,
        jpegs: List<ByteArray>,
        tankType: TankType,
        memo: String,
        history: String,
        tankSize: String,
        fish: String = "",
        stocking: String = "",
    ): Diagnosis = withContext(Dispatchers.IO) {
        val request = buildRequest(jpegs, tankType, memo, history, tankSize, fish, stocking)
        Diagnosis.parse(post(apiKey, model, request))
    }

    /** Reads species and head counts off the photos so the user does not have to type them. */
    suspend fun identifyFish(
        apiKey: String,
        model: String,
        jpegs: List<ByteArray>,
        tankType: TankType,
    ): FishScan = withContext(Dispatchers.IO) {
        FishScan.parse(post(apiKey, model, buildFishRequest(jpegs, tankType)))
    }

    /** Server-side hiccup (5xx): the same request usually works a moment later. */
    private class BusyException(val friendly: String) : Exception(friendly)

    /** Waits between retries, so a busy model gets three chances in about 7 seconds. */
    private val RETRY_DELAYS = longArrayOf(1_500, 5_000)

    /** Sends one generateContent call, retrying while the server reports itself busy. */
    private suspend fun post(apiKey: String, model: String, request: JSONObject): String {
        var lastBusy = ""
        repeat(RETRY_DELAYS.size + 1) { attempt ->
            try {
                return send(apiKey, model, request)
            } catch (e: BusyException) {
                lastBusy = e.friendly
                if (attempt < RETRY_DELAYS.size) delay(RETRY_DELAYS[attempt])
            }
        }
        throw GeminiException("$lastBusy 설정에서 다른 모델을 골라 보셔도 좋아요.")
    }

    private fun send(apiKey: String, model: String, request: JSONObject): String {
        val body = request.toString().toByteArray(Charsets.UTF_8)
        val conn = (URL("$BASE_URL$model:generateContent").openConnection() as HttpURLConnection).apply {
            requestMethod = "POST"
            connectTimeout = 20_000
            readTimeout = 120_000
            doOutput = true
            setRequestProperty("Content-Type", "application/json; charset=utf-8")
            setRequestProperty("x-goog-api-key", apiKey)
        }
        try {
            conn.outputStream.use { it.write(body) }
            val code = conn.responseCode
            val stream = if (code in 200..299) conn.inputStream else conn.errorStream
            val text = stream?.bufferedReader(Charsets.UTF_8)?.use { it.readText() }.orEmpty()
            if (code >= 500) throw BusyException(errorMessage(code, text))
            if (code !in 200..299) throw GeminiException(errorMessage(code, text))
            return extractAnswer(text)
        } catch (e: SocketTimeoutException) {
            throw GeminiException("응답이 너무 오래 걸려요. 잠시 후 다시 시도해 주세요.")
        } catch (e: IOException) {
            throw GeminiException("인터넷 연결을 확인해 주세요. (${e.javaClass.simpleName})")
        } catch (e: JSONException) {
            throw GeminiException("AI 응답을 해석하지 못했어요. 다시 시도해 주세요.")
        } finally {
            conn.disconnect()
        }
    }

    private fun imageParts(jpegs: List<ByteArray>): JSONArray {
        val parts = JSONArray()
        jpegs.forEachIndexed { i, jpeg ->
            val image = JSONObject()
                .put("mimeType", "image/jpeg")
                .put("data", Base64.encodeToString(jpeg, Base64.NO_WRAP))
            // Label each image so the model can cite "사진 N" in its evidence.
            parts.put(JSONObject().put("text", "사진 ${i + 1}"))
            parts.put(JSONObject().put("inlineData", image))
        }
        return parts
    }

    private fun envelope(system: String, parts: JSONArray, schema: JSONObject): JSONObject = JSONObject()
        .put("systemInstruction", JSONObject().put("parts", JSONArray().put(JSONObject().put("text", system))))
        .put("contents", JSONArray().put(JSONObject().put("role", "user").put("parts", parts)))
        .put(
            "generationConfig", JSONObject()
                .put("responseMimeType", "application/json")
                .put("responseSchema", schema)
        )

    private fun buildFishRequest(jpegs: List<ByteArray>, tankType: TankType): JSONObject {
        val parts = imageParts(jpegs)
        parts.put(
            JSONObject().put(
                "text", buildString {
                    append("어항 종류: ${tankType.promptLabel}\n")
                    append("사진 ${jpegs.size}장은 모두 같은 어항입니다. ")
                    append("사진에 보이는 생물을 종류별로 구분해서 마릿수를 세어 주세요.")
                }
            )
        )
        return envelope(FISH_SYSTEM_PROMPT, parts, FISH_SCHEMA)
    }

    private fun buildRequest(
        jpegs: List<ByteArray>,
        tankType: TankType,
        memo: String,
        history: String,
        tankSize: String,
        fish: String,
        stocking: String,
    ): JSONObject {
        val parts = imageParts(jpegs)
        parts.put(JSONObject().put("text", userPrompt(tankType, memo, jpegs.size, history, tankSize, fish, stocking)))
        return envelope(SYSTEM_PROMPT, parts, RESPONSE_SCHEMA)
    }

    private fun userPrompt(
        tankType: TankType,
        memo: String,
        photoCount: Int,
        history: String,
        tankSize: String,
        fish: String,
        stocking: String,
    ): String = buildString {
        append("어항 종류: ${tankType.promptLabel}\n")
        append("어항 크기: ${tankSize.ifBlank { "모름" }}\n")
        append("현재 물고기: ${fish.ifBlank { "정보 없음" }}\n")
        if (stocking.isNotBlank()) append("사육 밀집도(앱 계산): $stocking\n")
        append("오늘 날짜: ${java.time.LocalDate.now()}\n")
        if (history.isNotBlank()) {
            append("\n## 최근 30일 관리 기록 (최신순)\n")
            append(history)
            append("\n\n")
        } else {
            append("관리 기록: 없음\n")
        }
        if (memo.isNotBlank()) append("사용자 메모(오늘 증상): ${memo.trim()}\n")
        if (photoCount > 1) {
            append("사진 ${photoCount}장은 모두 같은 어항을 다른 각도나 가까이에서 찍은 것입니다. ")
            append("모든 사진을 종합해서 하나의 진단을 내려 주세요.")
        } else {
            append("이 어항 사진을 진단해 주세요.")
        }
    }

    private fun extractAnswer(raw: String): String {
        val root = JSONObject(raw)
        val candidate = root.optJSONArray("candidates")?.optJSONObject(0)
        if (candidate == null) {
            val reason = root.optJSONObject("promptFeedback")?.optString("blockReason").orEmpty()
            throw GeminiException("AI가 이 사진을 분석하지 않았어요. ${if (reason.isNotEmpty()) "($reason)" else ""}".trim())
        }
        val parts = candidate.optJSONObject("content")?.optJSONArray("parts")
        val answer = buildString {
            if (parts != null) for (i in 0 until parts.length()) {
                val p = parts.optJSONObject(i) ?: continue
                if (!p.optBoolean("thought", false)) append(p.optString("text"))
            }
        }
        if (answer.isBlank()) {
            throw GeminiException("AI 응답이 비어 있어요. (${candidate.optString("finishReason", "UNKNOWN")})")
        }
        return answer
    }

    private fun errorMessage(code: Int, body: String): String {
        val detail = runCatching { JSONObject(body).getJSONObject("error").optString("message") }.getOrDefault("")
        return when {
            code == 400 && detail.contains("API key", ignoreCase = true) ->
                "API 키가 올바르지 않아요. 설정에서 키를 다시 확인해 주세요."
            code == 401 || code == 403 ->
                "API 키 권한이 없어요. 설정에서 키를 다시 확인해 주세요."
            code == 404 ->
                "선택한 모델을 찾을 수 없어요. 설정에서 다른 모델을 골라 주세요."
            code == 429 ->
                "사용량 한도를 넘었어요. 1분쯤 뒤 다시 시도하거나 설정에서 다른 모델을 골라 주세요."
            code >= 500 ->
                "Gemini 서버가 지금 바빠요($code)."
            else -> "요청 실패($code): ${detail.ifBlank { "알 수 없는 오류" }}"
        }
    }

    private val SYSTEM_PROMPT = """
        당신은 20년 경력의 관상어 수의사이자 아쿠아리움 전문가입니다.
        사용자가 보낸 어항 사진을 보고 어항 상태를 진단하세요.

        반드시 살펴볼 것:
        - 물 상태: 탁도(백탁/녹수/갈색 타닌), 유막, 거품, 수위
        - 이끼(조류): 녹점, 갈색 규조류, 사상조, 흑사조(BBA), 남조류(시아노박테리아) 등 종류와 정도
        - 물고기: 흰점(백점병), 지느러미 녹음/갈라짐, 솜털(곰팡이), 복부 팽창, 비늘 일어남(솔방울병), 수면 호흡, 지느러미 접음, 변색, 과밀 사육, 죽은 개체
        - 수초: 잎 누렇게 변함, 녹아내림, 구멍 → 영양 결핍이나 조명 문제 추정
        - 바닥재·장식: 찌꺼기, 먹이 잔여물, 썩은 수초
        - 장비: 여과기, 히터, 조명, 에어레이션 상태(보이는 경우)

        규칙:
        - 사진에서 실제로 보이는 근거(evidence)만 바탕으로 판단하세요. 추측이면 추측이라고 쓰세요.
        - 사진이 여러 장이면 evidence에 "사진 2에서"처럼 몇 번째 사진에서 보였는지 적으세요.
        - 근접 사진은 정면 사진에서 잘 안 보이는 부분을 확인하는 데 쓰세요. 같은 문제를 중복으로 세지 마세요.
        - "최근 관리 기록"이 있으면 사진에서 본 증상과 연결해서 원인을 추정하세요.
          예: 며칠 전 새 물고기 추가 후 흰점 → 입수 스트레스·검역 부족 / 마지막 환수가 2주 이상 전 → 질산염 누적 가능 /
          최근 약품·첨가제 투입 → 약해 가능성 / 최근 여과재를 수돗물로 세척 → 박테리아 손실.
        - 기록에 근거한 추정이면 cause에 "기록을 보면 ~"처럼 밝히세요. 기록에 없는 사실은 지어내지 마세요.
        - 어항 크기(리터)를 알면 보이는 물고기 수·크기와 비교해 과밀 여부를 판단하고,
          환수량·약품·첨가제 양을 "약 20L(30%) 환수"처럼 리터 기준으로 구체적으로 안내하세요.
          표시된 리터는 외부 치수 기준이라 실제 물은 그보다 10~20% 적다는 점을 감안하세요.
        - "사육 밀집도(앱 계산)"가 주어지면 그 수치를 그대로 인용하지 말고, 사진에서 센 개체 수와 맞는지 먼저 확인하세요.
          등록된 목록보다 사진에 확실히 더 많거나 적게 보이면 그 차이를 summary에 알려 주세요.
          밀집도가 100%를 넘으면 여과·환수 주기를 어떻게 올려야 하는지 구체적으로 안내하세요.
        - 이전 AI 진단 기록이 있으면 그때보다 나아졌는지 나빠졌는지 summary에 짧게 언급하세요.
        - 암모니아·아질산·pH·수온처럼 사진으로 알 수 없는 것은 단정하지 말고 recommendedTests에 검사를 권하세요.
        - 해결책(solutions)은 초보자도 바로 따라 할 수 있게 구체적으로 쓰세요. (예: "물의 30%를 수온 맞춘 물로 환수")
        - actions는 오늘 당장 할 일부터 우선순위 순서로 3~5개.
        - categories는 "물 상태", "이끼", "물고기", "수초", "바닥·장비" 5개를 이 순서로. 보이지 않으면 status를 UNKNOWN으로.
        - overallScore: 100=매우 건강, 70 이상=양호, 40~69=주의, 40 미만=위험.
        - 어항 사진이 아니면 isAquarium=false, overallScore=0, 나머지는 짧게.
        - 모든 텍스트는 친절한 한국어 존댓말로 쓰세요.
    """.trimIndent()

    private val RESPONSE_SCHEMA: JSONObject by lazy {
        fun str() = JSONObject().put("type", "STRING")
        fun strEnum(vararg values: String) = str().put("enum", JSONArray(values.toList()))
        fun arr(items: JSONObject) = JSONObject().put("type", "ARRAY").put("items", items)
        fun obj(props: Map<String, JSONObject>) = JSONObject()
            .put("type", "OBJECT")
            .put("properties", JSONObject(props))
            .put("required", JSONArray(props.keys.toList()))
            .put("propertyOrdering", JSONArray(props.keys.toList()))

        obj(
            linkedMapOf(
                "isAquarium" to JSONObject().put("type", "BOOLEAN"),
                "overallScore" to JSONObject().put("type", "INTEGER"),
                "overallStatus" to strEnum("GOOD", "CAUTION", "DANGER"),
                "headline" to str(),
                "summary" to str(),
                "categories" to arr(
                    obj(
                        linkedMapOf(
                            "name" to str(),
                            "status" to strEnum("GOOD", "CAUTION", "DANGER", "UNKNOWN"),
                            "comment" to str(),
                        )
                    )
                ),
                "issues" to arr(
                    obj(
                        linkedMapOf(
                            "title" to str(),
                            "severity" to strEnum("HIGH", "MEDIUM", "LOW"),
                            "evidence" to str(),
                            "cause" to str(),
                            "solutions" to arr(str()),
                        )
                    )
                ),
                "actions" to arr(str()),
                "recommendedTests" to arr(str()),
            )
        )
    }

    private val FISH_SYSTEM_PROMPT = """
        당신은 관상어 종 동정(identification) 전문가입니다.
        사용자가 보낸 어항 사진에서 안에 사는 생물을 종류별로 구분하고 마릿수를 세세요.

        규칙:
        - 물고기뿐 아니라 새우(생이·체리새우 등), 달팽이, 그 밖의 생물도 모두 찾으세요.
        - kind는 FISH(물고기) / SHRIMP(새우) / SNAIL(달팽이) / OTHER(그 외) 중 하나로 정하세요.
        - name은 한국 수족관에서 흔히 쓰는 한글 이름으로 쓰세요. (예: 구피, 네온테트라, 코리도라스, 체리새우)
          품종까지 확실하면 "구피(턱시도)"처럼 괄호로 덧붙이세요.
        - count는 사진에서 실제로 센 개체 수입니다. 사진이 여러 장이면 같은 개체를 두 번 세지 마세요.
          여러 장에 같은 무리가 보이면 가장 많이 보인 사진의 수를 기준으로 하세요.
        - 수초 뒤나 가장자리에 가려 정확히 셀 수 없으면 보이는 만큼만 세고, note에 "더 있을 수 있어요"라고 적으세요.
        - adultSizeCm은 그 종이 다 컸을 때의 몸길이(꼬리 제외, cm)입니다. 종 도감 기준의 일반적인 값을 쓰세요.
          예: 구피 4, 네온테트라 3.5, 코리도라스 6, 베타 6, 체리새우 3, 골든애플스네일 5.
        - confidence는 종을 얼마나 확신하는지입니다. HIGH / MEDIUM / LOW.
          비슷한 종이 많아 헷갈리면 LOW로 두고 note에 후보를 적으세요. (예: "카디널테트라일 수도 있어요")
        - 확실하지 않다고 목록에서 빼지는 마세요. 대신 confidence를 낮추세요.
        - 어항 사진이 아니거나 생물이 안 보이면 isAquarium=false, species는 빈 배열로 두고 note에 이유를 쓰세요.
        - note는 짧은 한국어 존댓말 한두 문장.
    """.trimIndent()

    private val FISH_SCHEMA: JSONObject by lazy {
        fun str() = JSONObject().put("type", "STRING")
        fun strEnum(vararg values: String) = str().put("enum", JSONArray(values.toList()))
        val species = JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties", JSONObject(
                    linkedMapOf(
                        "name" to str(),
                        "kind" to strEnum("FISH", "SHRIMP", "SNAIL", "OTHER"),
                        "count" to JSONObject().put("type", "INTEGER"),
                        "adultSizeCm" to JSONObject().put("type", "NUMBER"),
                        "confidence" to strEnum("HIGH", "MEDIUM", "LOW"),
                        "note" to str(),
                    )
                )
            )
            .put("required", JSONArray(listOf("name", "kind", "count", "adultSizeCm", "confidence", "note")))
            .put("propertyOrdering", JSONArray(listOf("name", "kind", "count", "adultSizeCm", "confidence", "note")))

        JSONObject()
            .put("type", "OBJECT")
            .put(
                "properties", JSONObject(
                    linkedMapOf(
                        "isAquarium" to JSONObject().put("type", "BOOLEAN"),
                        "species" to JSONObject().put("type", "ARRAY").put("items", species),
                        "note" to str(),
                    )
                )
            )
            .put("required", JSONArray(listOf("isAquarium", "species", "note")))
            .put("propertyOrdering", JSONArray(listOf("isAquarium", "species", "note")))
    }
}
