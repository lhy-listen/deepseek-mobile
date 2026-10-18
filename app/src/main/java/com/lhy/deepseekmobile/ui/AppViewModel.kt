package com.lhy.deepseekmobile.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.lhy.deepseekmobile.AppContainer
import com.lhy.deepseekmobile.DsApp
import com.lhy.deepseekmobile.data.AppSettings
import com.lhy.deepseekmobile.data.Attachment
import com.lhy.deepseekmobile.data.AttachmentKind
import com.lhy.deepseekmobile.data.Conversation
import com.lhy.deepseekmobile.data.ImageRoute
import com.lhy.deepseekmobile.data.Message
import com.lhy.deepseekmobile.data.Mode
import com.lhy.deepseekmobile.data.ModelSpec
import com.lhy.deepseekmobile.data.Role
import com.lhy.deepseekmobile.data.Usage
import com.lhy.deepseekmobile.data.newId
import com.lhy.deepseekmobile.net.ChatEvent
import com.lhy.deepseekmobile.net.WireMessage
import com.lhy.deepseekmobile.util.AppJson
import com.lhy.deepseekmobile.util.CompactJson
import com.lhy.deepseekmobile.util.Tokens
import com.lhy.deepseekmobile.voice.VoiceController
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.serialization.builtins.ListSerializer
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.put
import java.io.File
import java.util.concurrent.atomic.AtomicInteger

data class ChatSession(
    val conversation: Conversation? = null,
    val modeId: String? = null,
    val modelId: String? = null,
    val streaming: Boolean = false,
    val phase: String? = null,
    val draft: String = "",
    val pending: List<Attachment> = emptyList(),
    val notice: String? = null,
    val contextTokens: Int = 0,
)

class AppViewModel(app: Application) : AndroidViewModel(app) {

    private val container: AppContainer = (app as DsApp).container
    private val context = app.applicationContext

    val settings: StateFlow<AppSettings> = container.settings.state
    val modes: StateFlow<List<Mode>> = container.modes.modes
    val conversations: StateFlow<List<com.lhy.deepseekmobile.data.ConversationSummary>> = container.conversations.summaries

    private val _session = MutableStateFlow(ChatSession())
    val session: StateFlow<ChatSession> = _session.asStateFlow()

    val voice = VoiceController(
        context = context,
        settingsProvider = { settings.value },
        api = container.api,
        scope = viewModelScope,
        onText = { appendVoice(it) },
        onNotice = { message -> _session.update { it.copy(notice = message) } },
    )

    private var streamJob: Job? = null
    private var saveJob: Job? = null
    private val streamGeneration = AtomicInteger(0)
    private var dirty = false
    private var bootstrapped = false

    val activeMode: Mode
        get() = modes.value.firstOrNull { it.id == (_session.value.modeId ?: settings.value.currentModeId) }
            ?: modes.value.firstOrNull()
            ?: com.lhy.deepseekmobile.data.Defaults.modes.first()

    val activeModel: ModelSpec
        get() {
            val id = _session.value.modelId ?: settings.value.currentModelId
            return settings.value.models.firstOrNull { it.id == id } ?: settings.value.currentModel
        }

    init {
        viewModelScope.launch {
            container.modes.load()
            container.conversations.load()
            bootstrapSession()
            bootstrapped = true
        }
    }

    private suspend fun bootstrapSession() {
        val lastId = settings.value.lastConversationId
        val candidate = lastId?.let { container.conversations.get(it) }
            ?: conversations.value.firstOrNull()?.let { container.conversations.get(it.id) }
        if (candidate != null) {
            openLoaded(candidate)
        } else {
            newChatInternal()
        }
    }

    // ------------------------------------------------------------- navigation

    fun newChat() {
        if (_session.value.streaming) stop()
        voice.cancel()
        viewModelScope.launch { persistCurrent() }
        newChatInternal()
    }

    private fun newChatInternal() {
        val s = settings.value
        _session.value = ChatSession(
            conversation = null,
            modeId = s.currentModeId,
            modelId = s.currentModelId,
        )
        updateContextEstimate()
    }

    fun openChat(id: String) {
        if (_session.value.conversation?.id == id) return
        if (_session.value.streaming) stop()
        voice.cancel()
        val previous = _session.value.conversation
        viewModelScope.launch {
            if (previous != null && dirty) persistConversation(previous)
            val conv = container.conversations.get(id) ?: return@launch
            openLoaded(conv)
        }
    }

    private fun openLoaded(conv: Conversation) {
        val modeId = conv.modeId ?: settings.value.currentModeId
        val modelId = conv.modelId ?: settings.value.currentModelId
        // A previous process may have been killed mid-stream: never re-enter a
        // conversation with a message that still claims to be generating.
        val sanitized = conv.copy(
            messages = conv.messages.map { message ->
                if (message.pending) {
                    message.copy(
                        pending = false,
                        stopped = true,
                        error = if (message.text.isBlank() && message.error == null) "上次生成被中断，可以重新发送。" else message.error,
                    )
                } else message
            },
        )
        _session.value = ChatSession(conversation = sanitized, modeId = modeId, modelId = modelId)
        container.settings.update { it.copy(lastConversationId = conv.id) }
        updateContextEstimate()
    }

    fun deleteChat(id: String) {
        viewModelScope.launch {
            container.conversations.delete(id)
            if (_session.value.conversation?.id == id) newChatInternal()
        }
    }

    fun renameChat(id: String, title: String) {
        if (title.isBlank()) return
        viewModelScope.launch {
            container.conversations.rename(id, title.trim())
            _session.update { s ->
                if (s.conversation?.id == id) s.copy(conversation = s.conversation.copy(title = title.trim())) else s
            }
        }
    }

    fun pinChat(id: String, pinned: Boolean) {
        viewModelScope.launch { container.conversations.setPinned(id, pinned) }
    }

    // ------------------------------------------------------------- composing

    fun setDraft(text: String) {
        _session.update { it.copy(draft = text) }
    }

    fun toggleVoice() {
        if (!_session.value.streaming) voice.toggle()
    }

    private fun appendVoice(text: String) {
        val trimmed = text.trim()
        if (trimmed.isEmpty()) return
        _session.update { session ->
            val draft = session.draft
            val separator = when {
                draft.isEmpty() -> ""
                draft.endsWith("\n") || draft.endsWith(" ") -> ""
                else -> " "
            }
            session.copy(draft = draft + separator + trimmed)
        }
    }

    fun removePending(id: String) {
        _session.update { s ->
            val target = s.pending.firstOrNull { it.id == id }
            if (target?.kind == AttachmentKind.IMAGE) target.localPath?.let { runCatching { File(it).delete() } }
            s.copy(pending = s.pending.filterNot { it.id == id })
        }
    }

    fun attachImages(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _session.update { it.copy(phase = if (uris.size > 1) "正在处理 ${uris.size} 张图片…" else "正在处理图片…") }
            val added = mutableListOf<Attachment>()
            for (uri in uris) {
                val id = newId()
                val file = withContext(Dispatchers.IO) { container.images.import(uri, id) }
                if (file != null) {
                    val (w, h) = container.images.sizeOf(file)
                    added += Attachment(
                        id = id,
                        kind = AttachmentKind.IMAGE,
                        name = queryName(uri) ?: file.name,
                        mime = "image/jpeg",
                        localPath = file.absolutePath,
                        uri = uri.toString(),
                        sizeBytes = file.length(),
                        width = w,
                        height = h,
                    )
                }
            }
            _session.update { s ->
                s.copy(pending = s.pending + added, phase = null, notice = if (added.isEmpty()) "图片读取失败" else s.notice)
            }
        }
    }

    fun attachFiles(uris: List<Uri>) {
        if (uris.isEmpty()) return
        viewModelScope.launch {
            _session.update { it.copy(phase = "正在读取文件…") }
            val added = mutableListOf<Attachment>()
            var skipped = 0
            for (uri in uris) {
                val result = container.ingestor.readFile(uri)
                result.onSuccess { f ->
                    added += Attachment(
                        id = newId(),
                        kind = AttachmentKind.FILE,
                        name = f.name,
                        mime = "text/plain",
                        uri = uri.toString(),
                        sizeBytes = f.bytes,
                        text = f.text,
                    )
                }.onFailure { skipped++ }
            }
            _session.update { s ->
                s.copy(
                    pending = s.pending + added,
                    phase = null,
                    notice = if (skipped > 0) "有 $skipped 个文件无法读取（可能是二进制或过大）" else s.notice,
                )
            }
        }
    }

    fun attachFolder(uri: Uri) {
        viewModelScope.launch {
            _session.update { it.copy(phase = "正在扫描文件夹…") }
            val result = container.ingestor.readTree(uri)
            result.onSuccess { folder ->
                val attachment = Attachment(
                    id = newId(),
                    kind = AttachmentKind.FOLDER,
                    name = folder.rootName,
                    mime = "text/x-directory",
                    uri = uri.toString(),
                    text = folder.toPrompt(),
                    fileCount = folder.files.size,
                    sizeBytes = folder.totalChars.toLong(),
                )
                _session.update { s ->
                    s.copy(pending = s.pending + attachment, phase = null)
                }
            }.onFailure { e ->
                _session.update { s -> s.copy(phase = null, notice = e.message ?: "文件夹读取失败") }
            }
        }
    }

    fun clearNotice() = _session.update { it.copy(notice = null) }

    // ------------------------------------------------------------- modes/models

    fun selectMode(modeId: String) {
        _session.update { it.copy(modeId = modeId) }
        container.settings.update { it.copy(currentModeId = modeId) }
        _session.update { s ->
            s.conversation?.let { c -> s.copy(conversation = c.copy(modeId = modeId)) } ?: s
        }.also { markDirty() }
    }

    fun selectModel(modelId: String) {
        _session.update { it.copy(modelId = modelId) }
        container.settings.update { it.copy(currentModelId = modelId) }
        _session.update { s ->
            s.conversation?.let { c -> s.copy(conversation = c.copy(modelId = modelId)) } ?: s
        }.also { markDirty() }
        updateContextEstimate()
    }

    fun saveMode(mode: Mode) {
        container.modes.upsert(mode)
        if (_session.value.modeId == mode.id) _session.update { it.copy(modeId = mode.id) }
    }

    fun deleteMode(id: String) {
        container.modes.delete(id)
        if (_session.value.modeId == id) selectMode(com.lhy.deepseekmobile.data.Defaults.modes.first().id)
    }

    fun duplicateMode(id: String): Mode? = container.modes.duplicate(id)

    fun importModes(raw: String): Int = container.modes.importJson(raw)

    fun resetModes() = container.modes.resetBuiltIns()

    // ------------------------------------------------------------- send / stop

    fun send(text: String = _session.value.draft, reuseUser: Message? = null) {
        val snapshot = _session.value
        if (snapshot.streaming) return
        if (voice.state.value.listening) voice.cancel()
        val trimmed = text.trim()
        if (reuseUser == null && trimmed.isEmpty() && snapshot.pending.isEmpty()) return

        val generation = streamGeneration.incrementAndGet()
        val mode = activeMode
        val model = activeModel
        val now = System.currentTimeMillis()

        val baseConversation = snapshot.conversation ?: Conversation(
            title = autoTitle(trimmed, snapshot.pending),
            modeId = mode.id,
            modelId = model.id,
            createdAt = now,
        )

        val userMessage = reuseUser ?: Message(
            role = Role.USER,
            text = trimmed,
            attachments = snapshot.pending,
            createdAt = now,
        )
        val assistantMessage = Message(
            role = Role.ASSISTANT,
            text = "",
            model = model.id,
            createdAt = now + 1,
            pending = true,
        )
        val baseMessages = if (reuseUser != null) baseConversation.messages else baseConversation.messages + userMessage
        val withMessages = baseConversation.copy(
            modeId = mode.id,
            modelId = model.id,
            messages = baseMessages + assistantMessage,
            updatedAt = now,
        )

        _session.value = snapshot.copy(
            conversation = withMessages,
            modeId = mode.id,
            modelId = model.id,
            draft = if (reuseUser == null) "" else snapshot.draft,
            pending = if (reuseUser == null) emptyList() else snapshot.pending,
            streaming = true,
            phase = null,
            notice = null,
        )
        dirty = true
        updateContextEstimate()

        val apiKey = settings.value.apiKey
        if (apiKey.isBlank()) {
            failStream(
                generation,
                assistantMessage.id,
                "还没有配置 API Key。点右上角设置填入后即可对话；Key 只保存在本机。",
                now,
            )
            return
        }

        streamJob = viewModelScope.launch {
            try {
                val prepared = prepareAttachments(userMessage.attachments, model, generation)
                // Replace the persisted user message with the prepared one.
                updateMessage(userMessage.id) { it.copy(attachments = prepared) }

                val systemText = buildSystemPrompt(mode, model)
                val wire = withContext(Dispatchers.IO) {
                    buildWireMessages(withMessages, userMessage.id, prepared, model, systemText)
                }
                updateContextEstimate()

                val started = System.currentTimeMillis()
                container.api.stream(
                    baseUrl = settings.value.baseUrl,
                    apiKey = settings.value.apiKey,
                    model = model.id,
                    messages = wire,
                    temperature = mode.temperature,
                    topP = mode.topP,
                    maxTokens = mode.maxTokens,
                ) { event ->
                    if (generation != streamGeneration.get()) return@stream
                    when (event) {
                        is ChatEvent.Delta -> updateMessage(assistantMessage.id) {
                            it.copy(text = it.text + event.text, reasoning = it.reasoning + event.reasoning)
                        }
                        is ChatEvent.Done -> finishStream(generation, assistantMessage.id, event.usage, started)
                        is ChatEvent.Failure -> failStream(generation, assistantMessage.id, event.message, started)
                    }
                }
                // Some providers close the stream without a Done frame.
                finishStream(generation, assistantMessage.id, null, started)
            } catch (e: Exception) {
                if (generation == streamGeneration.get()) {
                    failStream(generation, assistantMessage.id, e.message ?: "发送失败", System.currentTimeMillis())
                }
            }
        }
    }

    private fun finishStream(generation: Int, assistantId: String, usage: Usage?, started: Long) {
        if (generation != streamGeneration.get()) return
        val session = _session.value
        if (session.streaming.not()) return
        val duration = System.currentTimeMillis() - started
        _session.update { s ->
            s.copy(
                streaming = false,
                phase = null,
                conversation = s.conversation?.let { c ->
                    c.copy(
                        messages = c.messages.map { m ->
                            if (m.id == assistantId) m.copy(pending = false, usage = usage ?: m.usage, durationMs = duration)
                            else m
                        },
                        updatedAt = System.currentTimeMillis(),
                    )
                },
            )
        }
        viewModelScope.launch { persistCurrent() }
    }

    private fun failStream(generation: Int, assistantId: String, message: String, started: Long) {
        if (generation != streamGeneration.get()) return
        if (!_session.value.streaming) return
        _session.update { s ->
            s.copy(
                streaming = false,
                phase = null,
                conversation = s.conversation?.let { c ->
                    c.copy(messages = c.messages.map { m ->
                        if (m.id == assistantId) m.copy(
                            pending = false,
                            error = message,
                            durationMs = System.currentTimeMillis() - started,
                        ) else m
                    })
                },
            )
        }
        viewModelScope.launch { persistCurrent() }
    }

    fun stop() {
        if (!_session.value.streaming) return
        streamGeneration.incrementAndGet()
        container.api.cancelActive()
        streamJob?.cancel()
        streamJob = null
        val started = System.currentTimeMillis()
        _session.update { s ->
            s.copy(
                streaming = false,
                phase = null,
                conversation = s.conversation?.let { c ->
                    c.copy(messages = c.messages.map { m ->
                        if (m.pending) m.copy(pending = false, stopped = true) else m
                    })
                },
            )
        }
        viewModelScope.launch { persistCurrent() }
    }

    fun regenerate() {
        val session = _session.value
        if (session.streaming) return
        val conv = session.conversation ?: return
        val lastUserIndex = conv.messages.indexOfLast { it.role == Role.USER }
        if (lastUserIndex < 0) return
        val lastUser = conv.messages[lastUserIndex]
        val truncated = conv.copy(messages = conv.messages.take(lastUserIndex + 1))
        _session.update { it.copy(conversation = truncated) }
        send(lastUser.text, reuseUser = lastUser)
    }

    fun editMessage(messageId: String, newText: String) {
        val session = _session.value
        val conv = session.conversation ?: return
        val index = conv.messages.indexOfFirst { it.id == messageId }
        if (index < 0) return
        val trimmed = newText.trim()
        val original = conv.messages[index]
        val kept = conv.messages.take(index)
        _session.update {
            it.copy(
                conversation = conv.copy(messages = kept),
                pending = original.attachments,
                draft = "",
            )
        }
        send(trimmed)
    }

    fun deleteMessage(messageId: String) {
        val session = _session.value
        val conv = session.conversation ?: return
        _session.update {
            it.copy(conversation = conv.copy(messages = conv.messages.filterNot { m -> m.id == messageId }))
        }
        dirty = true
        updateContextEstimate()
    }

    // ------------------------------------------------------------- attachments

    private suspend fun prepareAttachments(
        attachments: List<Attachment>,
        model: ModelSpec,
        generation: Int,
    ): List<Attachment> = withContext(Dispatchers.IO) {
        attachments.map { attachment ->
            if (attachment.kind != AttachmentKind.IMAGE) return@map attachment
            if (attachment.route == ImageRoute.DIRECT && model.vision) return@map attachment
            if (attachment.route != ImageRoute.NONE && attachment.text != null) return@map attachment
            if (model.vision) return@map attachment.copy(route = ImageRoute.DIRECT)

            val s = settings.value
            val canUseVision = s.visionEnabled && s.visionModel.isNotBlank() && s.visionApiKey.isNotBlank()
            if (canUseVision) {
                updatePhase(generation, "正在理解图片…")
                val path = attachment.localPath
                val dataUrl = path?.let { container.images.dataUrl(File(it), 1568, 86) }
                if (dataUrl != null) {
                    val result = container.api.complete(
                        baseUrl = s.visionBaseUrl.ifBlank { s.baseUrl },
                        apiKey = s.visionApiKey,
                        model = s.visionModel,
                        messages = listOf(
                            WireMessage(
                                role = "user",
                                text = "请用中文详细描述这张图片：先逐字转录其中所有文字（保留排版结构），再说明界面/图表/场景的关键信息。只输出描述内容。",
                                images = listOf(dataUrl),
                            )
                        ),
                        maxTokens = 1200,
                    )
                    result.getOrNull()?.let { caption ->
                        return@map attachment.copy(text = caption, route = ImageRoute.VISION_MODEL)
                    }
                }
            }

            updatePhase(generation, "正在识别图片文字…")
            val path = attachment.localPath
            val ocr = path?.let { container.ocr.recognize(File(it)) }
            attachment.copy(
                text = ocr ?: "（未能识别图片中的文字，请直接在消息里说明你想问什么）",
                route = ImageRoute.OCR,
            )
        }
    }

    private fun updatePhase(generation: Int, phase: String) {
        if (generation == streamGeneration.get()) _session.update { it.copy(phase = phase) }
    }

    // ------------------------------------------------------------- prompt build

    private fun buildSystemPrompt(mode: Mode, model: ModelSpec): String = buildString {
        val global = settings.value.globalSystemPrompt.trim()
        if (global.isNotEmpty()) append(global).append("\n\n")
        if (mode.systemPrompt.isNotBlank()) append(mode.systemPrompt.trim())
        if (isEmpty()) append(com.lhy.deepseekmobile.data.Defaults.GENERAL_PROMPT)
        append("\n\n用户消息中可能带有【图片】、【文件】或【文件夹】内容，都是用户主动提供的上下文；请直接据此回答，不要假装看不到。")
    }

    private fun buildWireMessages(
        conversation: Conversation,
        currentUserId: String,
        preparedAttachments: List<Attachment>,
        model: ModelSpec,
        systemText: String,
    ): List<WireMessage> {
        val messages = mutableListOf<WireMessage>()
        if (systemText.isNotBlank()) messages += WireMessage("system", systemText)

        val history = conversation.messages.filter { !it.pending && it.role != Role.SYSTEM }
        val budgetTokens = (model.contextWindow - (activeMode.maxTokens ?: 8192) - Tokens.estimate(systemText) - 400)
            .coerceAtLeast(2_000)

        val chosen = mutableListOf<Message>()
        var used = 0
        for (message in history.asReversed()) {
            val isCurrent = message.id == currentUserId
            val attachments = if (isCurrent) preparedAttachments else message.attachments
            val content = renderUserText(message.text, attachments, model)
            val images = if (model.vision) imagesFor(attachments) else emptyList()
            val cost = Tokens.estimate(content) + images.size * 900
            if (!isCurrent && used + cost > budgetTokens) break
            chosen += message
            used += cost
        }
        chosen.asReversed().forEach { message ->
            val attachments = if (message.id == currentUserId) preparedAttachments else message.attachments
            when (message.role) {
                Role.USER -> {
                    val images = if (model.vision) imagesFor(attachments) else emptyList()
                    val fullText = renderUserText(message.text, attachments, model)
                    val allowed = (budgetTokens - images.size * 900 - 200).coerceAtLeast(600)
                    val text = if (Tokens.estimate(fullText) > allowed) {
                        fullText.take(allowed) + "\n\n…（内容过长，已按上下文上限截断）"
                    } else fullText
                    messages += WireMessage(role = "user", text = text, images = images)
                }
                Role.ASSISTANT -> if (message.text.isNotBlank()) {
                    messages += WireMessage("assistant", message.text)
                }
                Role.SYSTEM -> Unit
            }
        }
        return messages
    }

    private fun renderUserText(text: String, attachments: List<Attachment>, model: ModelSpec): String {
        if (attachments.isEmpty()) return text
        val extras = attachments.mapNotNull { attachment ->
            when (attachment.kind) {
                AttachmentKind.IMAGE -> attachment.text
                    ?.takeIf { attachment.route != ImageRoute.DIRECT }
                    ?.let { "【图片：${attachment.name}】\n$it" }
                AttachmentKind.FILE -> attachment.text?.let { "【文件：${attachment.name}】\n$it" }
                AttachmentKind.FOLDER -> attachment.text
            }
        }
        if (extras.isEmpty()) return text
        return buildString {
            if (text.isNotBlank()) append(text).append("\n\n")
            append(extras.joinToString("\n\n"))
        }
    }

    private fun imagesFor(attachments: List<Attachment>): List<String> = attachments
        .filter { it.kind == AttachmentKind.IMAGE && it.localPath != null }
        .take(6)
        .mapNotNull { a -> runCatching { File(a.localPath!!) }.getOrNull()?.let { container.images.dataUrl(it, 1536, 84) } }

    // ------------------------------------------------------------- persistence

    private suspend fun persistCurrent() {
        val conv = _session.value.conversation ?: return
        persistConversation(conv)
    }

    private suspend fun persistConversation(conversation: Conversation) {
        val emoji = modes.value.firstOrNull { it.id == conversation.modeId }?.emoji ?: "✨"
        container.conversations.save(conversation, emoji)
        dirty = false
    }

    fun flush() {
        if (!bootstrapped) return
        viewModelScope.launch {
            if (dirty) persistCurrent()
        }
    }

    fun onAppPaused() {
        if (!bootstrapped) return
        viewModelScope.launch {
            container.settings.persistNow()
            if (dirty) persistCurrent()
        }
    }

    private fun markDirty() {
        dirty = true
        saveJob?.cancel()
        saveJob = viewModelScope.launch {
            delay(600)
            persistCurrent()
        }
    }

    private fun updateMessage(id: String, transform: (Message) -> Message) {
        _session.update { s ->
            val conv = s.conversation ?: return@update s
            s.copy(conversation = conv.copy(messages = conv.messages.map { if (it.id == id) transform(it) else it }))
        }
        dirty = true
    }

    private fun updateContextEstimate() {
        val s = _session.value
        val conv = s.conversation ?: run {
            _session.update { it.copy(contextTokens = Tokens.estimate(buildSystemPrompt(activeMode, activeModel))) }
            return
        }
        val model = activeModel
        val total = Tokens.estimate(buildSystemPrompt(activeMode, model)) +
            conv.messages.filter { !it.pending }.sumOf { m ->
                Tokens.estimate(m.text) + m.attachments.sumOf { a -> Tokens.estimate(a.text.orEmpty()) }
            }
        _session.update { it.copy(contextTokens = total) }
    }

    private fun autoTitle(text: String, attachments: List<Attachment>): String {
        val base = text.trim().ifBlank {
            attachments.firstOrNull()?.let { a ->
                when (a.kind) {
                    AttachmentKind.IMAGE -> "图片对话"
                    AttachmentKind.FILE -> a.name
                    AttachmentKind.FOLDER -> "文件夹：${a.name}"
                }
            } ?: "新对话"
        }
        return base.replace("\n", " ").take(24)
    }

    private fun queryName(uri: Uri): String? = runCatching {
        context.contentResolver.query(uri, null, null, null, null)?.use { c ->
            val index = c.getColumnIndex(android.provider.OpenableColumns.DISPLAY_NAME)
            if (index >= 0 && c.moveToFirst()) c.getString(index) else null
        }
    }.getOrNull()

    // ------------------------------------------------------------- settings & modes

    fun updateSettings(transform: (AppSettings) -> AppSettings) {
        container.settings.update(transform)
        updateContextEstimate()
    }

    fun addModel(spec: ModelSpec) {
        updateSettings { s ->
            if (s.models.any { it.id == spec.id }) s else s.copy(models = s.models + spec)
        }
    }

    fun removeModel(id: String) {
        updateSettings { s ->
            val remaining = s.models.filterNot { it.id == id }.ifEmpty { com.lhy.deepseekmobile.data.Defaults.models }
            s.copy(
                models = remaining,
                currentModelId = if (s.currentModelId == id) remaining.first().id else s.currentModelId,
            )
        }
    }

    suspend fun testConnection(): Result<String> = withContext(Dispatchers.IO) {
        val s = settings.value
        if (s.apiKey.isBlank()) {
            return@withContext Result.failure(IllegalStateException("请先填写 API Key"))
        }
        container.api.listModels(s.baseUrl, s.apiKey)
            .map { models -> "连接成功 · 服务端报告 ${models.size} 个模型" }
    }

    fun resetAll() {
        viewModelScope.launch {
            container.conversations.clearAll()
            newChatInternal()
        }
    }

    // ------------------------------------------------------------- backup

    suspend fun exportBackup(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val all = container.conversations.all()
            val payload = buildJsonObject {
                put("format", "dsm-backup")
                put("version", 1)
                put("exportedAt", System.currentTimeMillis())
                put("settings", AppJson.encodeToString(AppSettings.serializer(), settings.value))
                put("modes", AppJson.encodeToString(ListSerializer(Mode.serializer()), modes.value))
                put("conversations", AppJson.encodeToString(ListSerializer(Conversation.serializer()), all))
            }
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(payload.toString().toByteArray(Charsets.UTF_8))
            } ?: error("无法写入文件")
            "已导出 ${all.size} 个对话、${modes.value.size} 个模式"
        }
    }

    suspend fun exportModes(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            context.contentResolver.openOutputStream(uri)?.use { out ->
                out.write(modes.value.let { AppJson.encodeToString(ListSerializer(Mode.serializer()), it) }.toByteArray())
            } ?: error("无法写入文件")
            "已导出 ${modes.value.size} 个模式"
        }
    }

    suspend fun importBackup(uri: Uri): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val raw = context.contentResolver.openInputStream(uri)?.use { it.readBytes().toString(Charsets.UTF_8) }
                ?: error("无法读取文件")
            val obj = CompactJson.parseToJsonElement(raw) as? kotlinx.serialization.json.JsonObject
                ?: error("不是有效的备份文件")
            var restoredConversations = 0
            var restoredModes = 0
            obj["settings"]?.let { element ->
                runCatching {
                    val incoming = CompactJson.decodeFromString(AppSettings.serializer(), element.toString())
                    // Keep local login unless the backup actually has a key.
                    container.settings.set(
                        if (incoming.apiKey.isBlank()) incoming.copy(
                            apiKey = settings.value.apiKey,
                            baseUrl = settings.value.baseUrl,
                        ) else incoming
                    )
                }
            }
            obj["modes"]?.let { element ->
                val list = runCatching {
                    CompactJson.decodeFromString(ListSerializer(Mode.serializer()), element.toString())
                }.getOrNull()
                if (!list.isNullOrEmpty()) {
                    list.forEach { m -> container.modes.upsert(m.copy(builtIn = false)) }
                    restoredModes = list.size
                }
            }
            obj["conversations"]?.let { element ->
                val list = runCatching {
                    CompactJson.decodeFromString(ListSerializer(Conversation.serializer()), element.toString())
                }.getOrNull()
                if (!list.isNullOrEmpty()) {
                    container.conversations.importAll(list)
                    restoredConversations = list.size
                }
            }
            "已恢复 $restoredConversations 个对话、$restoredModes 个模式"
        }
    }
}
