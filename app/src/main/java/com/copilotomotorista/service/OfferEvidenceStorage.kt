package com.copilotomotorista.service

import android.content.Context
import android.graphics.Bitmap
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Persists compact evidence inside private app storage; it is not exposed to the gallery. */
internal object OfferEvidenceStorage {
    fun save(context: Context, source: Bitmap): String? {
        val directory = File(context.filesDir, "trip_evidence")
        if (!directory.exists() && !directory.mkdirs()) return null
        val maxWidth = 1000
        val bitmap = if (source.width > maxWidth) {
            val height = (source.height * maxWidth.toFloat() / source.width).toInt().coerceAtLeast(1)
            Bitmap.createScaledBitmap(source, maxWidth, height, true)
        } else source
        val file = File(directory, "offer_${UUID.randomUUID()}.jpg")
        return try {
            val compressed = FileOutputStream(file).use { output ->
                bitmap.compress(Bitmap.CompressFormat.JPEG, 72, output)
            }
            if (compressed) {
                file.absolutePath
            } else {
                // A failed compressor may leave a truncated JPEG on disk.
                file.delete()
                null
            }
        } catch (_: Exception) {
            file.delete()
            null
        } finally {
            if (bitmap !== source) bitmap.recycle()
        }
    }
}
