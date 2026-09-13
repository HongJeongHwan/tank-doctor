package io.github.hongjeonghwan.tankdoctor.data

import android.content.Context

/** Outer tank dimensions in cm; 0 means not entered. */
data class TankSize(val width: Int = 0, val depth: Int = 0, val height: Int = 0) {
    val isSet: Boolean get() = width > 0 && depth > 0 && height > 0
    val liters: Int get() = width * depth * height / 1000
    val label: String get() = "${width}×${depth}×${height}cm · 약 ${liters}L"
}

/** App-private storage; allowBackup=false keeps the API key off cloud backups. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("api_key", "").orEmpty()
        set(value) = prefs.edit().putString("api_key", value).apply()

    var tankSize: TankSize
        get() = TankSize(prefs.getInt("tank_w", 0), prefs.getInt("tank_d", 0), prefs.getInt("tank_h", 0))
        set(value) = prefs.edit()
            .putInt("tank_w", value.width)
            .putInt("tank_d", value.depth)
            .putInt("tank_h", value.height)
            .apply()

    var tankType: TankType
        get() = TankType.entries.firstOrNull { it.name == prefs.getString("tank_type", null) } ?: TankType.FRESH
        set(value) = prefs.edit().putString("tank_type", value.name).apply()

    var model: String
        get() = prefs.getString("model", null)?.takeIf { id -> MODELS.any { it.id == id } } ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString("model", value).apply()
}
