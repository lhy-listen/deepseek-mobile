package com.lhy.deepseekmobile.voice

import android.content.Context
import android.content.Intent
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.speech.RecognitionListener
import android.speech.RecognizerIntent
import android.speech.SpeechRecognizer

/**
 * Thin wrapper around Android's on-device/system speech service. This keeps the
 * recognizer alive for partial results so the composer can show live captions.
 */
class SystemSpeechRecognizer(private val context: Context) {

    interface Listener {
        fun onPartial(text: String)
        fun onFinal(text: String)
        fun onError(message: String)
    }

    private val handler = Handler(Looper.getMainLooper())
    private var recognizer: SpeechRecognizer? = null
    private var listener: Listener? = null
    @Volatile private var finished = false

    fun isAvailable(): Boolean = runCatching {
        SpeechRecognizer.isRecognitionAvailable(context)
    }.getOrDefault(false)

    fun start(language: String, listener: Listener) {
        this.listener = listener
        finished = false
        handler.post {
            if (finished) return@post
            val speech = runCatching { SpeechRecognizer.createSpeechRecognizer(context) }.getOrNull()
            if (speech == null) {
                listener.onError("无法启动系统语音识别")
                return@post
            }
            recognizer = speech
            speech.setRecognitionListener(object : RecognitionListener {
                override fun onReadyForSpeech(params: Bundle?) = Unit
                override fun onBeginningOfSpeech() = Unit
                override fun onRmsChanged(rmsdB: Float) = Unit
                override fun onBufferReceived(buffer: ByteArray?) = Unit
                override fun onEndOfSpeech() = Unit
                override fun onEvent(eventType: Int, params: Bundle?) = Unit

                override fun onPartialResults(partialResults: Bundle?) {
                    val text = partialResults
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    if (text.isNotBlank()) listener.onPartial(text)
                }

                override fun onResults(results: Bundle?) {
                    val text = results
                        ?.getStringArrayList(SpeechRecognizer.RESULTS_RECOGNITION)
                        ?.firstOrNull()
                        .orEmpty()
                    finish()
                    listener.onFinal(text)
                }

                override fun onError(error: Int) {
                    val message = when (error) {
                        SpeechRecognizer.ERROR_AUDIO -> "录音出错，请重试"
                        SpeechRecognizer.ERROR_CLIENT -> "语音识别被中断"
                        SpeechRecognizer.ERROR_INSUFFICIENT_PERMISSIONS -> "没有麦克风权限"
                        SpeechRecognizer.ERROR_NETWORK,
                        SpeechRecognizer.ERROR_NETWORK_TIMEOUT -> "语音服务网络异常"
                        SpeechRecognizer.ERROR_NO_MATCH -> "没听清，再说一次"
                        SpeechRecognizer.ERROR_RECOGNIZER_BUSY -> "语音识别忙，请稍后再试"
                        SpeechRecognizer.ERROR_SERVER -> "语音服务返回错误"
                        SpeechRecognizer.ERROR_SPEECH_TIMEOUT -> "没有听到声音"
                        else -> "语音识别失败（错误码 $error）"
                    }
                    finish()
                    listener.onError(message)
                }
            })
            val intent = Intent(RecognizerIntent.ACTION_RECOGNIZE_SPEECH).apply {
                putExtra(RecognizerIntent.EXTRA_LANGUAGE_MODEL, RecognizerIntent.LANGUAGE_MODEL_FREE_FORM)
                putExtra(RecognizerIntent.EXTRA_LANGUAGE, language.ifBlank { "zh-CN" })
                putExtra(RecognizerIntent.EXTRA_MAX_RESULTS, 1)
                putExtra(RecognizerIntent.EXTRA_PARTIAL_RESULTS, true)
                putExtra(RecognizerIntent.EXTRA_CALLING_PACKAGE, context.packageName)
            }
            runCatching { speech.startListening(intent) }.onFailure {
                finish()
                listener.onError("无法启动系统语音识别")
            }
        }
    }

    fun stop() {
        handler.post { runCatching { recognizer?.stopListening() } }
    }

    fun cancel() {
        finished = true
        handler.post {
            runCatching { recognizer?.cancel() }
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }

    fun destroy() {
        finished = true
        handler.post {
            runCatching { recognizer?.destroy() }
            recognizer = null
        }
    }

    private fun finish() {
        finished = true
        runCatching { recognizer?.destroy() }
        recognizer = null
    }
}
