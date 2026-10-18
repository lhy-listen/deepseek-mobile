package com.lhy.deepseekmobile.media

import android.content.Context
import android.net.Uri
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.text.TextRecognition
import com.google.mlkit.vision.text.TextRecognizer
import com.google.mlkit.vision.text.chinese.ChineseTextRecognizerOptions
import kotlinx.coroutines.suspendCancellableCoroutine
import java.io.File
import kotlin.coroutines.resume

/**
 * On-device OCR (Chinese model also handles Latin). It is the offline fallback
 * when the active model cannot see images and no vision endpoint is configured.
 */
class OcrEngine(private val context: Context) {

    private val recognizer: TextRecognizer by lazy {
        TextRecognition.getClient(ChineseTextRecognizerOptions.Builder().build())
    }

    suspend fun recognize(file: File): String? = suspendCancellableCoroutine { cont ->
        val image = InputImage.fromFilePath(context, Uri.fromFile(file))
        recognizer.process(image)
            .addOnSuccessListener { result ->
                val text = result.text.trim()
                if (cont.isActive) cont.resume(text.ifBlank { null })
            }
            .addOnFailureListener {
                if (cont.isActive) cont.resume(null)
            }
    }

    fun close() {
        runCatching { recognizer.close() }
    }
}
