package com.hanecuzdan.app.core.media

import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.Build
import java.io.File
import java.io.FileOutputStream
import java.util.UUID

/** Fiş fotoğraflarını WebP formatında sıkıştırıp yerel receipts dizininde saklar. */
class ReceiptImageManager(private val context: Context) {

    private val receiptsDir: File
        get() = File(context.filesDir, "receipts").apply { if (!exists()) mkdirs() }

    fun createTempCaptureFile(): File =
        File(receiptsDir, "temp_${UUID.randomUUID()}.jpg")

    fun compressAndSaveFromUri(sourceUri: Uri, quality: Int = 75): String? {
        return runCatching {
            context.contentResolver.openInputStream(sourceUri)?.use { input ->
                val bitmap = BitmapFactory.decodeStream(input) ?: return null
                saveBitmapAsWebP(bitmap, quality)
            }
        }.getOrNull()
    }

    fun compressAndSaveBitmap(bitmap: Bitmap, quality: Int = 75): String? =
        runCatching { saveBitmapAsWebP(bitmap, quality) }.getOrNull()

    private fun saveBitmapAsWebP(bitmap: Bitmap, quality: Int): String {
        val scaled = scaleDownIfNeeded(bitmap, maxDimension = 1280)
        val outFile = File(receiptsDir, "receipt_${System.currentTimeMillis()}.webp")
        val format = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            Bitmap.CompressFormat.WEBP_LOSSY
        } else {
            @Suppress("DEPRECATION")
            Bitmap.CompressFormat.WEBP
        }
        FileOutputStream(outFile).use { fos ->
            scaled.compress(format, quality, fos)
        }
        if (scaled !== bitmap) scaled.recycle()
        return outFile.absolutePath
    }

    private fun scaleDownIfNeeded(bitmap: Bitmap, maxDimension: Int): Bitmap {
        val width = bitmap.width
        val height = bitmap.height
        if (width <= maxDimension && height <= maxDimension) return bitmap
        val ratio = minOf(maxDimension.toFloat() / width, maxDimension.toFloat() / height)
        return Bitmap.createScaledBitmap(
            bitmap,
            (width * ratio).toInt().coerceAtLeast(1),
            (height * ratio).toInt().coerceAtLeast(1),
            true
        )
    }
}
