package io.github.hongjeonghwan.tankdoctor.data

import org.json.JSONArray
import org.json.JSONException

data class FishInfo(val name: String, val count: Int) {
    companion object {
        fun encode(fish: List<FishInfo>): String = JSONArray().apply {
            fish.filter { it.name.isNotBlank() && it.count > 0 }.forEach {
                put(org.json.JSONObject().put("name", it.name.trim()).put("count", it.count))
            }
        }.toString()

        fun decode(value: String): List<FishInfo> = try {
            val array = JSONArray(value)
            buildList {
                for (i in 0 until array.length()) {
                    val item = array.optJSONObject(i) ?: continue
                    val name = item.optString("name").trim()
                    val count = item.optInt("count", 0)
                    if (name.isNotBlank() && count > 0) add(FishInfo(name, count))
                }
            }
        } catch (_: JSONException) {
            emptyList()
        }
    }
}