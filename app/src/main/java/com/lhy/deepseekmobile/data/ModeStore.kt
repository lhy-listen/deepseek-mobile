package com.lhy.deepseekmobile.data

import com.lhy.deepseekmobile.util.AppJson
import com.lhy.deepseekmobile.util.readTextOrNull
import com.lhy.deepseekmobile.util.writeTextAtomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.withContext
import java.io.File

class ModeStore(private val file: File) {

    private val _modes = MutableStateFlow<List<Mode>>(emptyList())
    val modes: StateFlow<List<Mode>> = _modes.asStateFlow()

    suspend fun load() = withContext(Dispatchers.IO) {
        val stored = runCatching {
            file.readTextOrNull()?.let { AppJson.decodeFromString<List<Mode>>(it) }
        }.getOrNull()

        if (stored.isNullOrEmpty()) {
            _modes.value = Defaults.modes
            persist(Defaults.modes)
        } else {
            // Re-attach any built-in mode the user deleted, keep custom ordering.
            val missing = Defaults.modes.filter { d -> stored.none { it.id == d.id } }
            val merged = stored + missing
            _modes.value = merged
            if (missing.isNotEmpty()) persist(merged)
        }
    }

    fun byId(id: String?): Mode? = _modes.value.firstOrNull { it.id == id }

    fun upsert(mode: Mode) {
        val current = _modes.value
        val next = if (current.any { it.id == mode.id }) {
            current.map { if (it.id == mode.id) mode.copy(builtIn = it.builtIn) else it }
        } else {
            current + mode
        }
        _modes.value = next
        persist(next)
    }

    fun delete(id: String) {
        val mode = byId(id) ?: return
        if (mode.builtIn) return
        val next = _modes.value.filterNot { it.id == id }
        _modes.value = next
        persist(next)
    }

    fun duplicate(id: String): Mode? {
        val src = byId(id) ?: return null
        val copy = src.copy(
            id = newId(),
            name = "${src.name} 副本",
            builtIn = false,
            createdAt = System.currentTimeMillis(),
        )
        upsert(copy)
        return copy
    }

    fun resetBuiltIns() {
        val customs = _modes.value.filter { !it.builtIn }
        val next = Defaults.modes + customs
        _modes.value = next
        persist(next)
    }

    fun exportJson(): String = AppJson.encodeToString(kotlinx.serialization.builtins.ListSerializer(Mode.serializer()), _modes.value)

    fun importJson(raw: String): Int {
        val incoming = runCatching {
            AppJson.decodeFromString(kotlinx.serialization.builtins.ListSerializer(Mode.serializer()), raw)
        }.getOrNull() ?: return 0
        var count = 0
        var current = _modes.value
        incoming.forEach { m ->
            if (m.name.isNotBlank()) {
                val safe = m.copy(
                    id = if (current.any { it.id == m.id }) newId() else m.id,
                    builtIn = false,
                    createdAt = System.currentTimeMillis(),
                )
                current = current + safe
                count++
            }
        }
        _modes.value = current
        persist(current)
        return count
    }

    private fun persist(modes: List<Mode>) {
        runCatching {
            file.writeTextAtomic(AppJson.encodeToString(kotlinx.serialization.builtins.ListSerializer(Mode.serializer()), modes))
        }
    }
}
