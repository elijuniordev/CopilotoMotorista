package com.copilotomotorista.service

import android.graphics.Bitmap
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.latin.TextRecognizerOptions

/**
 * OCR executado localmente pelo ML Kit. Nenhuma imagem ou texto é enviado
 * para um serviço remoto por esta classe.
 */
internal class LocalOcrTextRecognizer {
    private val recognizer = TextRecognition.getClient(TextRecognizerOptions.DEFAULT_OPTIONS)

    fun recognize(bitmap: Bitmap, onResult: (String) -> Unit) {
        try {
            val image = InputImage.fromBitmap(bitmap, 0)
            recognizer.process(image)
                .addOnSuccessListener { result ->
                    Log.d("OfferCapture", "mlkit_success blocks=${result.textBlocks.size} chars=${result.text.length}")
                    onResult(result.text)
                }
                .addOnFailureListener { error ->
                    Log.e("OfferCapture", "mlkit_recognition_failed", error)
                    onResult("")
                }
                .addOnCompleteListener {
                    if (!bitmap.isRecycled) bitmap.recycle()
                }
        } catch (error: Exception) {
            Log.e("OfferCapture", "mlkit_start_failed", error)
            if (!bitmap.isRecycled) bitmap.recycle()
            onResult("")
        }
    }

    fun close() {
        recognizer.close()
    }
}
