package com.lhy.deepseekmobile.data

import com.lhy.deepseekmobile.util.AppJson
import com.lhy.deepseekmobile.util.readTextOrNull
import com.lhy.deepseekmobile.util.writeTextAtomic
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.io.File

class SettingsStore(private val file: File) {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private var saveJob: Job? = null

    private val initial: AppSettings = run {
        val raw = file.readTextOrNull() ?: return@run AppSettings()
        runCatching { AppJson.decodeFromString<AppSettings>(raw) }.getOrDefault(AppSettings())
    }

    private val _state = MutableStateFlow(initial)
    val state: StateFlow<AppSettings> = _state.asStateFlow()

    fun update(transform: (AppSettings) -> AppSettings) {
        val next = transform(_state.value)
        if (next == _state.value) return
        _state.value = next
        scheduleSave()
    }

    fun set(settings: AppSettings) {
        _state.value = settings
        scheduleSave()
    }

    private fun scheduleSave() {
        saveJob?.cancel()
        saveJob = scope.launch {
            delay(180)
            persist(_state.value)
        }
    }

    suspend fun persistNow() = withContext(Dispatchers.IO) { persist(_state.value) }

    private fun persist(settings: AppSettings) {
        runCatching { file.writeTextAtomic(AppJson.encodeToString(AppSettings.serializer(), settings)) }
    }
}
