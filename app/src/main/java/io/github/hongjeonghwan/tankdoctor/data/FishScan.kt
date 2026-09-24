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

/**
 * Where one individual sits in a photo, as Gemini reports it:
 * each edge is 0~1000 of the image width or height.
 */
data class BoundingBox(val xmin: Int, val ymin: Int, val xmax: Int, val ymax: Int) {
    val isValid: Boolean get() = xmax > xmin && ymax > ymin

    companion object {
        fun parse(o: JSONObject?): BoundingBox? {
            if (o == null) return null
            fun edge(key: String): Int? = o.optInt(key, -1).takeIf { it in 0..1000 }
            val xmin = edge("xmin") ?: return null
            val ymin = edge("ymin") ?: return null
            val xmax = edge("xmax") ?: return null
            val ymax = edge("ymax") ?: return null
            return BoundingBox(xmin, ymin, xmax, ymax).takeIf { it.isValid }
        }
    }
}

/** One species the model found in the photos, before the user confirms it. */
data class ScannedFish(
    val info: FishInfo,
    val confidence: Confidence,
    val note: String,
    /** 0-based index of the photo [box] refers to. */
    val photoIndex: Int = 0,
    val box: BoundingBox? = null,
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
                    ScannedFish(
                        info = info,
                        confidence = Confidence.of(item.optString("confidence")),
                        note = item.optString("note"),
                        // The model numbers photos from 1, like the labels it was given.
                        photoIndex = (item.optInt("photo", 1) - 1).coerceAtLeast(0),
                        box = BoundingBox.parse(item.optJSONObject("box")),
                    )
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
