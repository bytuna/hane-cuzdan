package com.hanecuzdan.app.core.media

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlin.coroutines.resume

/** ML Kit On-Device Text Recognition kullanarak fiş fotoğrafından metin tarar. */
object ReceiptOcrManager {

    suspend fun scanReceiptUri(context: Context, uri: Uri): ReceiptScanResult {
        val image = runCatching { InputImage.fromFilePath(context, uri) }.getOrNull()
            ?: return ReceiptScanResult()

        val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)
        return suspendCancellableCoroutine { cont ->
            recognizer.process(image)
                .addOnSuccessListener { visionText ->
                    val parsed = ReceiptOcrParser.parse(visionText.text)
                    if (cont.isActive) cont.resume(parsed)
                }
                .addOnFailureListener {
                    if (cont.isActive) cont.resume(ReceiptScanResult())
                }
        }
    }
}
