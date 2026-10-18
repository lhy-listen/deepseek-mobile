package com.lhy.deepseekmobile.data

import com.lhy.deepseekmobile.util.AppJson
import com.lhy.deepseekmobile.util.readTextOrNull
import com.lhy.deepseekmobile.util.writeTextAtomic
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import java.io.File

class ConversationStore(private val dir: File) {

    private val indexFile = File(dir, "index.json")
    private val mutex = Mutex()

    private val _summaries = MutableStateFlow<List<ConversationSummary>>(emptyList())
    val summaries: StateFlow<List<ConversationSummary>> = _summaries.asStateFlow()

    private val summarySerializer = ListSerializer(ConversationSummary.serializer())

    suspend fun load() = withContext(Dispatchers.IO) {
        dir.mkdirs()
        val list = runCatching {
            indexFile.readTextOrNull()?.let { AppJson.decodeFromString(summarySerializer, it) }
        }.getOrNull()

        if (list == null) {
            val rebuilt = rebuildFromDisk()
            writeIndex(rebuilt)
            _summaries.value = sort(rebuilt)
        } else {
            _summaries.value = sort(list)
        }
    }

    suspend fun get(id: String): Conversation? = withContext(Dispatchers.IO) {
        val f = File(dir, "$id.json")
        val raw = f.readTextOrNull() ?: return@withContext null
        runCatching { AppJson.decodeFromString<Conversation>(raw) }.getOrNull()
    }

    suspend fun save(conversation: Conversation, modeEmoji: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            File(dir, "${conversation.id}.json").writeTextAtomic(
                AppJson.encodeToString(Conversation.serializer(), conversation)
            )
            val summary = conversation.toSummary(modeEmoji)
            val current = _summaries.value.filterNot { it.id == conversation.id }
            val next = sort(current + summary)
            _summaries.value = next
            writeIndex(next)
        }
    }

    suspend fun delete(id: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            File(dir, "$id.json").delete()
            val next = _summaries.value.filterNot { it.id == id }
            _summaries.value = next
            writeIndex(next)
        }
    }

    suspend fun rename(id: String, title: String) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val f = File(dir, "$id.json")
            val conversation = f.readTextOrNull()?.let {
                runCatching { AppJson.decodeFromString<Conversation>(it) }.getOrNull()
            } ?: return@withContext
            val updated = conversation.copy(title = title, updatedAt = System.currentTimeMillis())
            f.writeTextAtomic(AppJson.encodeToString(Conversation.serializer(), updated))
            val next = sort(
                _summaries.value.map { if (it.id == id) it.copy(title = title, updatedAt = updated.updatedAt) else it }
            )
            _summaries.value = next
            writeIndex(next)
        }
    }

    suspend fun setPinned(id: String, pinned: Boolean) = mutex.withLock {
        withContext(Dispatchers.IO) {
            val f = File(dir, "$id.json")
            val conversation = f.readTextOrNull()?.let {
                runCatching { AppJson.decodeFromString<Conversation>(it) }.getOrNull()
            } ?: return@withContext
            val updated = conversation.copy(pinned = pinned)
            f.writeTextAtomic(AppJson.encodeToString(Conversation.serializer(), updated))
            val next = sort(_summaries.value.map { if (it.id == id) it.copy(pinned = pinned) else it })
            _summaries.value = next
            writeIndex(next)
        }
    }

    suspend fun clearAll() = mutex.withLock {
        withContext(Dispatchers.IO) {
            dir.listFiles()?.forEach { it.delete() }
            _summaries.value = emptyList()
        }
    }

    suspend fun all(): List<Conversation> = withContext(Dispatchers.IO) {
        dir.listFiles { f -> f.extension == "json" && f.name != "index.json" }
            ?.mapNotNull { f ->
                f.readTextOrNull()?.let { runCatching { AppJson.decodeFromString<Conversation>(it) }.getOrNull() }
            }
            ?.sortedByDescending { it.updatedAt }
            ?: emptyList()
    }

    suspend fun importAll(items: List<Conversation>) = mutex.withLock {
        withContext(Dispatchers.IO) {
            dir.mkdirs()
            var next = _summaries.value
            items.forEach { c ->
                val existing = File(dir, "${c.id}.json")
                val safe = if (existing.exists()) c.copy(id = newId()) else c
                File(dir, "${safe.id}.json").writeTextAtomic(AppJson.encodeToString(Conversation.serializer(), safe))
                next = next.filterNot { it.id == safe.id } + safe.toSummary("✨")
            }
            next = sort(next)
            _summaries.value = next
            writeIndex(next)
        }
    }

    private fun rebuildFromDisk(): List<ConversationSummary> {
        val files = dir.listFiles { f -> f.extension == "json" && f.name != "index.json" } ?: return emptyList()
        return files.mapNotNull { f ->
            f.readTextOrNull()?.let { runCatching { AppJson.decodeFromString<Conversation>(it) }.getOrNull() }
                ?.toSummary("✨")
        }
    }

    private fun writeIndex(list: List<ConversationSummary>) {
        runCatching { indexFile.writeTextAtomic(AppJson.encodeToString(summarySerializer, list)) }
    }

    private fun sort(list: List<ConversationSummary>) =
        list.sortedWith(compareByDescending<ConversationSummary> { it.pinned }.thenByDescending { it.updatedAt })
}
