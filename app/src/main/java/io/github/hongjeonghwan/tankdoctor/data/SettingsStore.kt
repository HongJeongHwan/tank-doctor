package io.github.hongjeonghwan.tankdoctor.data

import android.content.Context

/** App-private storage; allowBackup=false keeps the API key off cloud backups. */
class SettingsStore(context: Context) {
    private val prefs = context.getSharedPreferences("settings", Context.MODE_PRIVATE)

    var apiKey: String
        get() = prefs.getString("api_key", "").orEmpty()
        set(value) = prefs.edit().putString("api_key", value).apply()

    var model: String
        get() = prefs.getString("model", null)?.takeIf { id -> MODELS.any { it.id == id } } ?: DEFAULT_MODEL
        set(value) = prefs.edit().putString("model", value).apply()
}
