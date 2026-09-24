package io.github.hongjeonghwan.tankdoctor.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import org.json.JSONArray
import java.io.File
import java.util.concurrent.Executors
import java.util.concurrent.atomic.AtomicLong

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

    /** Species thumbnails cut out of a scan photo; named so the fish list can point at them. */
    val speciesDir: File = File(context.filesDir, "species").apply { mkdirs() }

    private val speciesSeq = AtomicLong()

    fun saveSpeciesPhoto(jpeg: ByteArray): String {
        val name = "sp_${System.currentTimeMillis()}_${speciesSeq.incrementAndGet()}.jpg"
        File(speciesDir, name).writeBytes(jpeg)
        return name
    }

    /** Drops every species thumbnail the fish list no longer points at. */
    fun pruneSpeciesPhotosAsync(keep: Set<String>) = writer.execute {
        speciesDir.listFiles()?.forEach { if (it.name !in keep) it.delete() }
    }

    fun readPhoto(name: String): Pair<Bitmap, ByteArray>? {
        val f = File(photoDir, name)
        if (!f.exists()) return null
        val bytes = f.readBytes()
        val bmp = BitmapFactory.decodeByteArray(bytes, 0, bytes.size) ?: return null
        return bmp to bytes
    }
}
