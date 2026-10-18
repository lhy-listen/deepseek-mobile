package com.lhy.deepseekmobile.data

import kotlinx.serialization.Serializable
import java.util.UUID

@Serializable
enum class Role { USER, ASSISTANT, SYSTEM }

@Serializable
enum class AttachmentKind { IMAGE, FILE, FOLDER }

/** How an image reached the model — shown as a tiny badge on the attachment chip. */
@Serializable
enum class ImageRoute { DIRECT, VISION_MODEL, OCR, NONE }

@Serializable
data class Attachment(
    val id: String = newId(),
    val kind: AttachmentKind,
    val name: String,
    val mime: String = "",
    val uri: String? = null,
    val localPath: String? = null,
    val sizeBytes: Long = 0,
    /** Extracted text for FILE/FOLDER, OCR or vision description for IMAGE. */
    val text: String? = null,
    val route: ImageRoute = ImageRoute.NONE,
    val width: Int = 0,
    val height: Int = 0,
    val fileCount: Int = 0,
)

@Serializable
data class Usage(
    val promptTokens: Int = 0,
    val completionTokens: Int = 0,
    val totalTokens: Int = 0,
)

@Serializable
data class Message(
    val id: String = newId(),
    val role: Role,
    val text: String = "",
    val reasoning: String = "",
    val attachments: List<Attachment> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val model: String? = null,
    val usage: Usage? = null,
    val error: String? = null,
    val pending: Boolean = false,
    val durationMs: Long = 0,
    val stopped: Boolean = false,
)

@Serializable
data class Conversation(
    val id: String = newId(),
    val title: String = "新对话",
    val modeId: String? = null,
    val modelId: String? = null,
    val messages: List<Message> = emptyList(),
    val createdAt: Long = System.currentTimeMillis(),
    val updatedAt: Long = System.currentTimeMillis(),
    val pinned: Boolean = false,
)

@Serializable
data class ConversationSummary(
    val id: String,
    val title: String,
    val updatedAt: Long,
    val messageCount: Int,
    val pinned: Boolean = false,
    val modeEmoji: String = "✨",
    val preview: String = "",
)

@Serializable
data class Mode(
    val id: String = newId(),
    val name: String,
    val emoji: String = "✨",
    val tagline: String = "",
    val systemPrompt: String = "",
    val modelId: String? = null,
    val temperature: Double? = null,
    val topP: Double? = null,
    val maxTokens: Int? = null,
    val showReasoning: Boolean = true,
    val quickPrompts: List<String> = emptyList(),
    val builtIn: Boolean = false,
    val createdAt: Long = System.currentTimeMillis(),
)

@Serializable
data class ModelSpec(
    val id: String,
    val label: String = "",
    val reasoning: Boolean = false,
    val vision: Boolean = false,
    val contextWindow: Int = 128_000,
    val builtIn: Boolean = false,
) {
    val displayName: String get() = label.ifBlank { id }
}

@Serializable
data class AppSettings(
    val baseUrl: String = "https://api.deepseek.com",
    val apiKey: String = "",
    val models: List<ModelSpec> = Defaults.models,
    val currentModelId: String = "deepseek-chat",
    val currentModeId: String = "builtin-general",
    val themeMode: String = "system", // system | dark | light
    val accentId: String = "blue",
    val amoled: Boolean = false,
    val fontScale: Float = 1f,
    val haptics: Boolean = true,
    val showUsage: Boolean = true,
    val globalSystemPrompt: String = "",
    val visionEnabled: Boolean = false,
    val visionBaseUrl: String = "",
    val visionApiKey: String = "",
    val visionModel: String = "",
    val onboardingDone: Boolean = false,
    val lastConversationId: String? = null,
) {
    val currentModel: ModelSpec get() = models.firstOrNull { it.id == currentModelId } ?: models.firstOrNull() ?: Defaults.models.first()
    val configured: Boolean get() = apiKey.isNotBlank()
}

fun newId(): String = UUID.randomUUID().toString().replace("-", "").take(20)

fun Conversation.toSummary(modeEmoji: String): ConversationSummary = ConversationSummary(
    id = id,
    title = title,
    updatedAt = updatedAt,
    messageCount = messages.size,
    pinned = pinned,
    modeEmoji = modeEmoji,
    preview = messages.lastOrNull { it.role != Role.SYSTEM }?.let {
        (it.text.ifBlank { it.reasoning }).replace("\n", " ").trim().take(60)
    } ?: "",
)

fun Message.shortText(max: Int = 60): String =
    text.replace("\n", " ").trim().take(max)
