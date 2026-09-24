package io.github.hongjeonghwan.tankdoctor.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject

/**
 * What kind of creature this is.
 *
 * [bioload] scales the body length before the density estimate: shrimp and
 * snails foul the water far less than a fish of the same length.
 */
enum class FishKind(val label: String, val emoji: String, val bioload: Double, val defaultSizeCm: Double) {
    FISH("물고기", "🐟", 1.0, 5.0),
    SHRIMP("새우", "🦐", 0.2, 3.0),
    SNAIL("달팽이", "🐌", 0.3, 2.0),
    OTHER("기타", "🪸", 1.0, 5.0);

    companion object {
        fun of(value: String?): FishKind = entries.firstOrNull { it.name == value } ?: FISH
    }
}

/**
 * One species living in the tank.
 *
 * [sizeCm] is the adult body length used for the stocking-density estimate;
 * 0 means unknown and [FishKind.defaultSizeCm] is assumed instead.
 */
data class FishInfo(
    val name: String,
    val count: Int,
    val sizeCm: Double = 0.0,
    val kind: FishKind = FishKind.FISH,
    /** File name of a thumbnail cut from a scan photo, so the list can show the species. */
    val photo: String = "",
) {
    val isValid: Boolean get() = name.isNotBlank() && count > 0

    /** Adult length used by the density estimate. */
    val effectiveSizeCm: Double get() = if (sizeCm > 0) sizeCm else kind.defaultSizeCm

    /** "구피 3마리(성어 4cm)" for prompts and summaries. */
    val label: String get() = buildString {
        append("$name ${count}마리")
        if (sizeCm > 0) append("(성어 ${trimNumber(sizeCm)}cm)")
    }

    companion object {
        fun encode(fish: List<FishInfo>): String = JSONArray().apply {
            fish.filter { it.isValid }.forEach {
                put(
                    JSONObject()
                        .put("name", it.name.trim())
                        .put("count", it.count)
                        .put("sizeCm", it.sizeCm)
                        .put("kind", it.kind.name)
                        .put("photo", it.photo)
                )
            }
        }.toString()

        fun decode(value: String): List<FishInfo> = try {
            val array = JSONArray(value)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    // sizeCm and kind are missing in lists saved before the density feature.
                    val size = item.optDouble("sizeCm", 0.0)
                    val fish = FishInfo(
                        name = item.optString("name").trim(),
                        count = item.optInt("count", 0),
                        sizeCm = if (size > 0 && !size.isNaN()) size else 0.0,
                        kind = FishKind.of(item.optString("kind")),
                        photo = item.optString("photo"),
                    )
                    if (fish.isValid) add(fish)
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }

        /** Merges scanned species into the existing list: same name adds up, a new name is appended. */
        fun merge(current: List<FishInfo>, scanned: List<FishInfo>): List<FishInfo> {
            val result = current.toMutableList()
            scanned.filter { it.isValid }.forEach { found ->
                val index = result.indexOfFirst { it.name.trim().equals(found.name.trim(), ignoreCase = true) }
                if (index >= 0) {
                    val existing = result[index]
                    result[index] = existing.copy(
                        count = existing.count + found.count,
                        sizeCm = if (existing.sizeCm > 0) existing.sizeCm else found.sizeCm,
                        photo = existing.photo.ifBlank { found.photo },
                    )
                } else {
                    result += found
                }
            }
            return result
        }
    }
}

/** 4.0 -> "4", 2.5 -> "2.5" */
fun trimNumber(value: Double): String =
    if (value == value.toLong().toDouble()) value.toLong().toString() else String.format("%.1f", value)
