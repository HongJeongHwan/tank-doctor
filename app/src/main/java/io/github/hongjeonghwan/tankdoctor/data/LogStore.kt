package io.github.hongjeonghwan.tankdoctor.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import java.io.File
import java.util.concurrent.Executors

/** Care log persisted as one JSON file; diagnosis photos live next to it as JPEGs. */
class LogStore(context: Context) {
    private val file = File(context.filesDir, "log.json")
    val photoDir: File = File(context.filesDir, "photos").apply { mkdirs() }

    // Single thread keeps writes in order without blocking the UI.
    private val writer = Executors.newSingleThreadExecutor()

    fun load(): List<LogEntry> {
        if (!file.exists()) return emptyList()
        return runCatching {
            val arr = JSONArray(file.readText())
            (0 until arr.length()).mapNotNull { i -> runCatching { LogEntry.fromJson(arr.getJSONObject(i)) }.getOrNull() }
        }.getOrDefault(emptyList())
    }

    fun saveAsync(entries: List<LogEntry>) = writer.execute {
        val json = JSONArray().apply { entries.forEach { put(it.toJson()) } }.toString()
        val tmp = File(file.parentFile, "log.json.tmp")
        tmp.writeText(json)
        if (!tmp.renameTo(file)) {
            file.delete()
            tmp.renameTo(file)
        }
    }

    fun savePhotos(entryId: Long, jpegs: List<ByteArray>): List<String> =
        jpegs.mapIndexed { i, bytes ->
            val name = "${entryId}_$i.jpg"
            File(photoDir, name).writeBytes(bytes)
            name
        }

    fun deletePhotosAsync(names: List<String>) = writer.execute {
        names.forEach { File(photoDir, it).delete() }
    }

    fun readPhoto(name: String): Pair<Bitmap, ByteArray>? {
        val f = File(photoDir, name)
        if (!f.exists()) return null
        val bytes = f.readBytes()
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return bmp to bytes
    }
}
