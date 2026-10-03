package io.github.hongjeonghwan.tankdoctor.data

import android.content.Context
import android.content.Intent
import android.net.Uri
import android.provider.Settings
import androidx.core.content.FileProvider
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import org.json.JSONObject
import java.io.File
import java.net.HttpURLConnection
import java.net.URL

/** A newer GitHub release than the running build. */
data class AppRelease(val version: String, val apkUrl: String, val notes: String)

/** Checks GitHub Releases for a newer APK, downloads it and hands it to the system installer. */
object AppUpdate {
    private const val LATEST = "https://api.github.com/repos/HongJeongHwan/tank-doctor/releases/latest"
    private const val APK_NAME = "TankDoctor.apk"

    /** The latest release if it is newer than [current], else null. Throws on network errors. */
    suspend fun check(current: String): AppRelease? = withContext(Dispatchers.IO) {
        val conn = (URL(LATEST).openConnection() as HttpURLConnection).apply {
            connectTimeout = 10_000
            readTimeout = 15_000
            setRequestProperty("Accept", "application/vnd.github+json")
        }
        try {
            if (conn.responseCode != 200) throw IllegalStateException("GitHub 응답 ${conn.responseCode}")
            parseRelease(conn.inputStream.bufferedReader().use { it.readText() }, current)
        } finally {
            conn.disconnect()
        }
    }

    /** Downloads the APK into the cache, reporting progress from 0 to 1. */
    suspend fun download(context: Context, release: AppRelease, onProgress: (Float) -> Unit): File =
        withContext(Dispatchers.IO) {
            val dir = File(context.cacheDir, "updates").apply { mkdirs() }
            dir.listFiles()?.forEach { it.delete() }
            val target = File(dir, APK_NAME)
            val conn = (URL(release.apkUrl).openConnection() as HttpURLConnection).apply {
                connectTimeout = 15_000
                readTimeout = 30_000
            }
            try {
                if (conn.responseCode != 200) throw IllegalStateException("다운로드 응답 ${conn.responseCode}")
                val total = conn.contentLengthLong
                var done = 0L
                conn.inputStream.use { input ->
                    target.outputStream().use { output ->
                        val buf = ByteArray(64 * 1024)
                        while (true) {
                            val n = input.read(buf)
                            if (n < 0) break
                            output.write(buf, 0, n)
                            done += n
                            if (total > 0) onProgress(done.toFloat() / total)
                        }
                    }
                }
            } finally {
                conn.disconnect()
            }
            target
        }

    /** Installing sideloaded APKs needs a one-time per-app allowance from the user. */
    fun canInstall(context: Context): Boolean = context.packageManager.canRequestPackageInstalls()

    fun openInstallPermission(context: Context) = context.startActivity(
        Intent(Settings.ACTION_MANAGE_UNKNOWN_APP_SOURCES, Uri.parse("package:${context.packageName}"))
            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
    )

    /** Opens the system install prompt; Android always asks the user to confirm. */
    fun install(context: Context, apk: File) {
        val uri = FileProvider.getUriForFile(context, "${context.packageName}.fileprovider", apk)
        context.startActivity(
            Intent(Intent.ACTION_VIEW)
                .setDataAndType(uri, "application/vnd.android.package-archive")
                .addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_ACTIVITY_NEW_TASK)
        )
    }

    internal fun parseRelease(json: String, current: String): AppRelease? {
        val o = JSONObject(json)
        val version = o.optString("tag_name").removePrefix("v")
        if (!isNewer(version, current)) return null
        val assets = o.optJSONArray("assets") ?: return null
        val apk = (0 until assets.length()).map { assets.getJSONObject(it) }
            .firstOrNull { it.optString("name") == APK_NAME } ?: return null
        return AppRelease(version, apk.getString("browser_download_url"), o.optString("body"))
    }

    /** Compares dotted numbers, so "1.10.0" beats "1.9.2"; a "-dev" suffix is ignored. */
    internal fun isNewer(candidate: String, current: String): Boolean {
        fun parts(v: String) = v.substringBefore('-').split('.').map { it.toIntOrNull() ?: 0 }
        val a = parts(candidate)
        val b = parts(current)
        for (i in 0 until maxOf(a.size, b.size)) {
            val x = a.getOrElse(i) { 0 }
            val y = b.getOrElse(i) { 0 }
            if (x != y) return x > y
        }
        return false
    }
}
