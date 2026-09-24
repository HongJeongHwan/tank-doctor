package io.github.hongjeonghwan.tankdoctor.data

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Matrix
import android.net.Uri
import androidx.exifinterface.media.ExifInterface
import java.io.ByteArrayOutputStream
import kotlin.math.max

object ImageUtil {
    class Loaded(val bitmap: Bitmap, val jpeg: ByteArray)

    /** Decodes, fixes EXIF rotation and shrinks the photo so the upload stays small. */
    fun load(context: Context, uri: Uri, maxEdge: Int = 1536): Loaded {
        val resolver = context.contentResolver
        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        // decodeStream always returns null in bounds-only mode, so check the stream, not the result.
        val boundsStream = resolver.openInputStream(uri) ?: error("파일을 열 수 없어요")
        boundsStream.use { BitmapFactory.decodeStream(it, null, bounds) }
        if (bounds.outWidth <= 0 || bounds.outHeight <= 0) error("지원하지 않는 이미지 형식이에요")

        var sample = 1
        while (max(bounds.outWidth, bounds.outHeight) / (sample * 2) >= maxEdge) sample *= 2
        val decoded = resolver.openInputStream(uri)?.use {
            BitmapFactory.decodeStream(it, null, BitmapFactory.Options().apply { inSampleSize = sample })
        } ?: error("이미지를 읽을 수 없어요")

        val rotation = runCatching {
            resolver.openInputStream(uri)?.use { ExifInterface(it).rotationDegrees } ?: 0
        }.getOrDefault(0)

        val matrix = Matrix()
        val scale = maxEdge.toFloat() / max(decoded.width, decoded.height)
        if (scale < 1f) matrix.postScale(scale, scale)
        if (rotation != 0) matrix.postRotate(rotation.toFloat())
        val bitmap = if (matrix.isIdentity) decoded
        else Bitmap.createBitmap(decoded, 0, 0, decoded.width, decoded.height, matrix, true)

        val out = ByteArrayOutputStream()
        bitmap.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Loaded(bitmap, out.toByteArray())
    }

    /**
     * Cuts one creature out of a tank photo so the list can show what the species looks like.
     *
     * [box] edges run 0~1000 across the image, the way Gemini reports them. A margin is added
     * around the box because the model tends to hug the body, and very small boxes are dropped
     * rather than shown as a blurry smudge.
     */
    fun crop(source: Bitmap, box: BoundingBox, maxEdge: Int = 320, margin: Float = 0.18f): Loaded? {
        if (!box.isValid) return null
        val left = box.xmin / 1000f * source.width
        val top = box.ymin / 1000f * source.height
        val right = box.xmax / 1000f * source.width
        val bottom = box.ymax / 1000f * source.height
        val padX = (right - left) * margin
        val padY = (bottom - top) * margin

        val x = (left - padX).toInt().coerceIn(0, source.width - 1)
        val y = (top - padY).toInt().coerceIn(0, source.height - 1)
        val width = (right + padX).toInt().coerceAtMost(source.width) - x
        val height = (bottom + padY).toInt().coerceAtMost(source.height) - y
        // Under ~24px the crop is unreadable, so no picture beats a misleading one.
        if (width < 24 || height < 24) return null

        val cut = Bitmap.createBitmap(source, x, y, width, height)
        val scale = maxEdge.toFloat() / max(cut.width, cut.height)
        val scaled = if (scale < 1f) {
            Bitmap.createScaledBitmap(cut, (cut.width * scale).toInt(), (cut.height * scale).toInt(), true)
        } else {
            cut
        }

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, 85, out)
        return Loaded(scaled, out.toByteArray())
    }
}
