package com.lhy.deepseekmobile.voice

import android.content.Context
import com.lhy.deepseekmobile.data.AppSettings
import com.lhy.deepseekmobile.net.ChatApi
import com.lhy.deepseekmobile.net.WireMessage
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class VoiceState(
    val listening: Boolean = false,
    val busy: Boolean = false,
    val phase: String? = null,
    val partial: String = "",
    val level: Float = 0f,
    val error: String? = null,
)

/**
 * Orchestrates the two voice routes:
 *   system recognizer (no extra key) or cloud transcription (any Whisper-compatible API),
 * then optionally rewrites the raw transcript with the active chat model.
 */
class VoiceController(
    private val context: Context,
    private val settingsProvider: () -> AppSettings,
    private val api: ChatApi,
    private val scope: CoroutineScope,
    private val onText: (String) -> Unit,
    private val onNotice: (String) -> Unit,
) {

    private val _state = MutableStateFlow(VoiceState())
    val state: StateFlow<VoiceState> = _state.asStateFlow()

    private val recorder = AudioRecorder(context)
    private val transcriber = CloudTranscriber()
    private var system: SystemSpeechRecognizer? = null
    private var autoStopJob: Job? = null

    fun toggle() {
        val s = _state.value
        when {
            s.listening -> stop()
            s.busy -> Unit
            else -> start()
        }
    }

    fun start() {
        if (_state.value.busy || _state.value.listening) return
        if (!recorder.hasPermission()) {
            fail("需要麦克风权限才能语音输入")
            return
        }
        _state.value = VoiceState(listening = true)
        val settings = settingsProvider()
        if (settings.asrMode == "cloud") startCloud(settings) else startSystem(settings)
    }

    fun stop() {
        if (!_state.value.listening) return
        autoStopJob?.cancel()
        val settings = settingsProvider()
        if (settings.asrMode == "cloud") {
            val file = recorder.stop()
            if (file == null || file.length() < 8000) {
                file?.delete()
                fail("录到的声音太短，再试一次")
                return
            }
            _state.update { it.copy(listening = false, busy = true, phase = "正在转写…", partial = "") }
            scope.launch {
                val result = transcriber.transcribe(
                    baseUrl = settings.asrBaseUrl,
                    apiKey = settings.asrApiKey,
                    model = settings.asrModel,
                    language = settings.asrLanguage,
                    file = file,
                )
                file.delete()
                result.onSuccess { handleTranscript(it) }
                    .onFailure { fail(it.message ?: "语音转写失败") }
            }
        } else {
            system?.stop()
            _state.update { it.copy(phase = "正在识别…") }
        }
    }

    fun cancel() {
        autoStopJob?.cancel()
        recorder.cancel()
        system?.cancel()
        system = null
        _state.value = VoiceState()
    }

    fun clearError() {
        _state.update { it.copy(error = null) }
    }

    private fun startSystem(settings: AppSettings) {
        val speech = SystemSpeechRecognizer(context)
        system = speech
        if (!speech.isAvailable()) {
            fail("本机没有可用的系统语音服务，可在「设置 → 语音输入」改用云端转写")
            return
        }
        speech.start(
            if (settings.asrLanguage == "zh") "zh-CN" else settings.asrLanguage,
            object : SystemSpeechRecognizer.Listener {
            override fun onPartial(text: String) {
                _state.update { it.copy(partial = text) }
            }

            override fun onFinal(text: String) {
                system = null
                handleTranscript(text)
            }

            override fun onError(message: String) {
                system = null
                fail(message)
            }
        })
    }

    private fun startCloud(settings: AppSettings) {
        if (settings.asrApiKey.isBlank()) {
            fail("云端转写还没有填 Key（设置 → 语音输入）")
            return
        }
        val file = recorder.start { level ->
            _state.update { it.copy(level = level) }
        }
        if (file == null) {
            fail("无法开始录音，请检查麦克风权限")
            return
        }
        autoStopJob = scope.launch {
            delay(90_000)
            if (_state.value.listening) stop()
        }
    }

    private fun handleTranscript(raw: String) {
        val text = raw.trim()
        if (text.isBlank()) {
            fail("没有识别到内容，再说一次")
            return
        }
        val settings = settingsProvider()
        if (!settings.voiceCorrect || settings.apiKey.isBlank()) {
            _state.value = VoiceState()
            onText(text)
            return
        }
        _state.update { it.copy(listening = false, busy = true, phase = "正在整理文字…", partial = "") }
        scope.launch {
            val corrected = correct(text, settings)
            _state.value = VoiceState()
            if (corrected != null) {
                onText(corrected)
            } else {
                onText(text)
                onNotice("自动纠错失败，已插入原始转写")
            }
        }
    }

    private suspend fun correct(raw: String, settings: AppSettings): String? {
        val glossary = settings.voiceGlossary
            .lines()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .joinToString("、")
        val systemPrompt = buildString {
            append("你是中文语音转写校对员。把用户给你的原始转写整理成通顺的书面中文：\n")
            append("1. 修正同音/近音错别字、错误的断句与标点；\n")
            append("2. 删除“嗯、呃、那个、就是说、然后然后”等口头赘词和重复；\n")
            append("3. 保留原意、语气与信息量，不扩写、不总结、不解释；\n")
            append("4. 专业术语、人名、数字、英文和代码按原样保留。\n")
            if (glossary.isNotEmpty()) append("术语/人名的优先写法：").append(glossary).append("\n")
            append("只输出整理后的文本，不要加引号、标题或任何说明。")
        }
        val maxTokens = (raw.length * 1.6).toInt().coerceIn(256, 2048)
        return api.complete(
            baseUrl = settings.baseUrl,
            apiKey = settings.apiKey,
            model = settings.currentModel.id,
            messages = listOf(
                WireMessage("system", systemPrompt),
                WireMessage("user", raw),
            ),
            maxTokens = maxTokens,
        ).getOrNull()
            ?.trim()
            ?.removeSurrounding("\"")
            ?.removeSurrounding("“", "”")
            ?.takeIf { it.isNotBlank() }
    }

    private fun fail(message: String) {
        autoStopJob?.cancel()
        recorder.cancel()
        system?.cancel()
        system = null
        _state.value = VoiceState(error = message)
    }
}
