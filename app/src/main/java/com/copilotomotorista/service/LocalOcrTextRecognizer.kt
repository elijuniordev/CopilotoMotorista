package com.copilotomotorista.service

import android.graphics.Bitmap
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
        val image = InputImage.fromBitmap(bitmap, 0)
        recognizer.process(image)
            .addOnSuccessListener { result -> onResult(result.text) }
            .addOnFailureListener { onResult("") }
            .addOnCompleteListener {
                if (!bitmap.isRecycled) bitmap.recycle()
            }
    }

    fun close() {
        recognizer.close()
    }
}
