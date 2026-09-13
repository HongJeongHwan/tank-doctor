package io.github.hongjeonghwan.tankdoctor.data

import org.json.JSONArray
import org.json.JSONObject

enum class Level(val label: String) {
    GOOD("양호"),
    CAUTION("주의"),
    DANGER("위험"),
    UNKNOWN("판단 어려움");

    companion object {
        fun of(value: String?): Level = entries.firstOrNull { it.name == value } ?: UNKNOWN
    }
}

enum class Severity(val label: String) {
    HIGH("심각"),
    MEDIUM("보통"),
    LOW("경미");

    companion object {
        fun of(value: String?): Severity = entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}

data class Category(val name: String, val level: Level, val comment: String)

data class Issue(
    val title: String,
    val severity: Severity,
    val evidence: String,
    val cause: String,
    val solutions: List<String>,
)

data class Diagnosis(
    val isAquarium: Boolean,
    val score: Int,
    val level: Level,
    val headline: String,
    val summary: String,
    val categories: List<Category>,
    val issues: List<Issue>,
    val actions: List<String>,
    val tests: List<String>,
    /** Original JSON so the result can be stored in the care log and re-parsed later. */
    val raw: String = "",
) {
    companion object {
        fun parse(json: String): Diagnosis {
            val o = JSONObject(json)
            return Diagnosis(
                isAquarium = o.optBoolean("isAquarium", true),
                score = o.optInt("overallScore", 0).coerceIn(0, 100),
                level = Level.of(o.optString("overallStatus")),
                headline = o.optString("headline"),
                summary = o.optString("summary"),
                categories = o.optJSONArray("categories").objects().map {
                    Category(it.optString("name"), Level.of(it.optString("status")), it.optString("comment"))
                },
                issues = o.optJSONArray("issues").objects()
                    .map {
                        Issue(
                            title = it.optString("title"),
                            severity = Severity.of(it.optString("severity")),
                            evidence = it.optString("evidence"),
                            cause = it.optString("cause"),
                            solutions = it.optJSONArray("solutions").strings(),
                        )
                    }
                    .sortedBy { it.severity.ordinal },
                actions = o.optJSONArray("actions").strings(),
                tests = o.optJSONArray("recommendedTests").strings(),
                raw = json,
            )
        }

        private fun JSONArray?.objects(): List<JSONObject> =
            if (this == null) emptyList() else (0 until length()).mapNotNull { optJSONObject(it) }

        private fun JSONArray?.strings(): List<String> =
            if (this == null) emptyList() else (0 until length()).map { optString(it) }.filter { it.isNotBlank() }
    }
}
