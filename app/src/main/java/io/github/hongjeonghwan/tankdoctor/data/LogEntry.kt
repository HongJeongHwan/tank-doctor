package io.github.hongjeonghwan.tankdoctor.data

import org.json.JSONArray
import org.json.JSONObject
import java.time.LocalDate
import java.time.format.TextStyle
import java.time.temporal.ChronoUnit
import java.util.Locale

enum class LogCategory(
    val label: String,
    val emoji: String,
    val hint: String,
    val suggestions: List<String> = emptyList(),
) {
    WATER("환수", "💧", "예: 30% 환수, 컨디셔너 투입", listOf("10% 환수", "30% 환수", "50% 환수", "컨디셔너 투입")),
    PLANT("수초", "🌿", "예: 아마존소드 2촉 식재, 트리밍", listOf("새 수초 식재", "트리밍", "액비 투입", "뿌리비료")),
    FISH("물고기", "🐟", "예: 구피 2마리 추가 / 네온 1마리 폐사", listOf("추가", "폐사", "분양 보냄", "치어 발견")),
    FEED("먹이", "🍤", "예: 냉동 브라인쉬림프 급여", listOf("사료", "냉동 먹이", "금식일")),
    CLEAN("청소·장비", "🧽", "예: 여과기 스펀지 세척, 조명 교체", listOf("여과기 청소", "유리 이끼 제거", "바닥재 청소", "장비 교체")),
    TEST("수질검사", "🧪", "예: 암모니아 0, 아질산 0.25, pH 7.0, 수온 26도", listOf("암모니아 ", "아질산 ", "질산염 ", "pH ", "수온 ")),
    MEDS("약품·첨가제", "💊", "예: 백점병 약 1회차, 박테리아제 5ml", listOf("치료제", "박테리아제", "수질 안정제")),
    NOTE("메모", "📝", "예: 물이 약간 뿌옇게 보임"),
    DIAGNOSIS("AI 진단", "🩺", "");

    companion object {
        /** Categories the user can pick; DIAGNOSIS entries are created by the app. */
        val userCategories = entries.filter { it != DIAGNOSIS }

        fun of(value: String?): LogCategory = entries.firstOrNull { it.name == value } ?: NOTE
    }
}

data class LogEntry(
    val id: Long,
    val date: LocalDate,
    val category: LogCategory,
    val note: String,
    val photos: List<String> = emptyList(),
    val diagnosisJson: String? = null,
    /** Stocking changes, for 물고기 entries. */
    val changesJson: String? = null,
) {
    val diagnosis: Diagnosis? by lazy {
        diagnosisJson?.let { runCatching { Diagnosis.parse(it) }.getOrNull() }
    }

    val changes: List<FishChange> by lazy { FishChange.decode(changesJson) }

    /** What the log list shows: the structured lines first, then any free note. */
    val summary: String
        get() = listOfNotNull(
            changes.joinToString(", ") { it.label }.ifBlank { null },
            note.ifBlank { null },
        ).joinToString(" · ")

    fun toJson(): JSONObject = JSONObject()
        .put("id", id)
        .put("date", date.toString())
        .put("category", category.name)
        .put("note", note)
        .put("photos", JSONArray(photos))
        .put("diagnosis", diagnosisJson ?: JSONObject.NULL)
        .put("changes", changesJson ?: JSONObject.NULL)

    companion object {
        fun fromJson(o: JSONObject): LogEntry {
            val photos = o.optJSONArray("photos")
            return LogEntry(
                id = o.getLong("id"),
                date = LocalDate.parse(o.getString("date")),
                category = LogCategory.of(o.optString("category")),
                note = o.optString("note"),
                photos = if (photos == null) emptyList() else (0 until photos.length()).map { photos.getString(it) },
                diagnosisJson = if (o.isNull("diagnosis")) null else o.optString("diagnosis").ifBlank { null },
                changesJson = if (o.isNull("changes")) null else o.optString("changes").ifBlank { null },
            )
        }
    }
}

fun daysAgo(date: LocalDate, today: LocalDate = LocalDate.now()): Long = ChronoUnit.DAYS.between(date, today)

fun relativeDay(days: Long): String = when {
    days <= 0L -> "오늘"
    days == 1L -> "어제"
    else -> "${days}일 전"
}

/** e.g. "9월 13일 (일)" */
fun LocalDate.koreanLabel(): String =
    "${monthValue}월 ${dayOfMonth}일 (${dayOfWeek.getDisplayName(TextStyle.SHORT, Locale.KOREAN)})"
