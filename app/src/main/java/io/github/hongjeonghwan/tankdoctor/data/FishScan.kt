package io.github.hongjeonghwan.tankdoctor.data

import org.json.JSONArray
import org.json.JSONObject

enum class Confidence(val label: String) {
    HIGH("확실"),
    MEDIUM("보통"),
    LOW("불확실");

    companion object {
        fun of(value: String?): Confidence = entries.firstOrNull { it.name == value } ?: MEDIUM
    }
}

/** One species the model found in the photos, before the user confirms it. */
data class ScannedFish(
    val info: FishInfo,
    val confidence: Confidence,
    val note: String,
)

/** Result of reading species and head counts off tank photos. */
data class FishScan(
    val isAquarium: Boolean,
    val species: List<ScannedFish>,
    val note: String,
) {
    val fish: List<FishInfo> get() = species.map { it.info }

    companion object {
        fun parse(json: String): FishScan {
            val o = JSONObject(json)
            val array = o.optJSONArray("species") ?: JSONArray()
            val species = (0 until array.length()).mapNotNull { array.optJSONObject(it) }.mapNotNull { item ->
                val size = item.optDouble("adultSizeCm", 0.0)
                val info = FishInfo(
                    name = item.optString("name").trim(),
                    count = item.optInt("count", 0),
                    sizeCm = if (size > 0 && !size.isNaN()) size else 0.0,
                    kind = FishKind.of(item.optString("kind")),
                )
                if (info.isValid) {
                    ScannedFish(info, Confidence.of(item.optString("confidence")), item.optString("note"))
                } else {
                    null
                }
            }
            return FishScan(
                isAquarium = o.optBoolean("isAquarium", true),
                species = species,
                note = o.optString("note"),
            )
        }
    }
}
