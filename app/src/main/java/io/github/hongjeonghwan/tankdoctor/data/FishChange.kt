package io.github.hongjeonghwan.tankdoctor.data

import org.json.JSONArray
import org.json.JSONException
import org.json.JSONObject
import java.time.LocalDate

enum class FishEvent(val label: String, val sign: Int) {
    ADD("추가", 1),
    BIRTH("치어", 1),
    LOSS("폐사", -1),
    AWAY("분양", -1);

    companion object {
        fun of(value: String?): FishEvent = entries.firstOrNull { it.name == value } ?: ADD
    }
}

/** One line of a 물고기 log entry: what came in or went out, and how many. */
data class FishChange(val name: String, val count: Int, val event: FishEvent = FishEvent.ADD) {
    val isValid: Boolean get() = name.isNotBlank() && count > 0
    val label: String get() = "$name ${count}마리 ${event.label}"

    companion object {
        fun encode(changes: List<FishChange>): String = JSONArray().apply {
            changes.filter { it.isValid }.forEach {
                put(
                    JSONObject()
                        .put("name", it.name.trim())
                        .put("count", it.count)
                        .put("event", it.event.name)
                )
            }
        }.toString()

        fun decode(value: String?): List<FishChange> {
            if (value.isNullOrBlank()) return emptyList()
            return try {
                val array = JSONArray(value)
                buildList {
                    for (i in 0 until array.length()) {
                        val item = array.optJSONObject(i) ?: continue
                        val change = FishChange(
                            name = item.optString("name").trim(),
                            count = item.optInt("count", 0),
                            event = FishEvent.of(item.optString("event")),
                        )
                        if (change.isValid) add(change)
                    }
                }
            } catch (_: JSONException) {
                emptyList()
            }
        }
    }
}

/**
 * How many creatures live in the tank right now.
 *
 * The list registered in settings is the count of record, taken on [asOf].
 * 물고기 log entries are there to date each stocking, so only the ones written
 * after that day are applied on top - anything earlier is already included in
 * the registered numbers.
 */
fun currentFish(
    registered: List<FishInfo>,
    asOf: LocalDate?,
    entries: List<LogEntry>,
): List<FishInfo> {
    val later = entries
        .filter { it.category == LogCategory.FISH && (asOf == null || it.date.isAfter(asOf)) }
        .sortedBy { it.date }
        .flatMap { it.changes }
        .filter { it.isValid }
    if (later.isEmpty()) return registered

    val result = registered.toMutableList()
    later.forEach { change ->
        val index = result.indexOfFirst { it.name.trim().equals(change.name.trim(), ignoreCase = true) }
        val delta = change.count * change.event.sign
        if (index >= 0) {
            val existing = result[index]
            result[index] = existing.copy(count = (existing.count + delta).coerceAtLeast(0))
        } else if (delta > 0) {
            // A species stocked after the last registration; size stays unknown until it is registered.
            result += FishInfo(change.name.trim(), delta)
        }
    }
    return result.filter { it.count > 0 }
}
