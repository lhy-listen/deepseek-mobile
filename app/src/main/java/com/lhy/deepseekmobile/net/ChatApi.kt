package com.lhy.deepseekmobile.net

import com.lhy.deepseekmobile.data.Usage
import com.lhy.deepseekmobile.util.CompactJson
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.isActive
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.add
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.int
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import kotlinx.serialization.json.putJsonArray
import kotlinx.serialization.json.putJsonObject
import okhttp3.Call
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.Response
import java.io.IOException
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicReference

data class WireMessage(
    val role: String,
    val text: String,
    val images: List<String> = emptyList(),
)

sealed interface ChatEvent {
    data class Delta(val text: String = "", val reasoning: String = "") : ChatEvent
    data class Done(val usage: Usage? = null, val finishReason: String? = null) : ChatEvent
    data class Failure(val message: String, val code: Int = 0) : ChatEvent
}

/**
 * Thin OpenAI-compatible client. DeepSeek's own endpoint is the default, but any
 * compatible gateway works because the path and payload follow the same contract.
 */
class ChatApi(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(25, TimeUnit.SECONDS)
        .readTimeout(600, TimeUnit.SECONDS)
        .writeTimeout(90, TimeUnit.SECONDS)
        .retryOnConnectionFailure(true)
        .build()
) {

    private val active = AtomicReference<Call?>(null)

    fun cancelActive() {
        active.getAndSet(null)?.cancel()
    }

    suspend fun stream(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<WireMessage>,
        temperature: Double? = null,
        topP: Double? = null,
        maxTokens: Int? = null,
        onEvent: (ChatEvent) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val url = chatUrl(baseUrl)
        val payload = buildPayload(model, messages, temperature, topP, maxTokens, stream = true)
        val request = Request.Builder()
            .url(url)
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Content-Type", "application/json")
            .header("Accept", "text/event-stream")
            .header("User-Agent", "DeepSeekMobile/1.0")
            .post(payload.toRequestBody(JSON))
            .build()

        val call = client.newCall(request)
        active.set(call)
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    onEvent(ChatEvent.Failure(httpErrorMessage(response.code, response.body?.string()), response.code))
                    return@use
                }
                val source = response.body?.source()
                if (source == null) {
                    onEvent(ChatEvent.Failure("服务端返回了空响应"))
                    return@use
                }
                while (true) {
                    if (!currentCoroutineContext().isActive) break
                    val line = source.readUtf8Line() ?: break
                    if (line.isEmpty() || line.startsWith(":")) continue
                    if (!line.startsWith("data:")) continue
                    val data = line.substring(5).trim()
                    if (data.isEmpty()) continue
                    if (data == "[DONE]") {
                        onEvent(ChatEvent.Done())
                        return@use
                    }
                    when (val event = parseChunk(data)) {
                        null -> Unit
                        is ChatEvent.Failure -> {
                            onEvent(event)
                            return@use
                        }
                        else -> onEvent(event)
                    }
                }
                onEvent(ChatEvent.Done())
            }
        } catch (e: CancellationException) {
            throw e
        } catch (e: IOException) {
            if (currentCoroutineContext().isActive) {
                onEvent(ChatEvent.Failure(humanNetworkError(e)))
            }
        } catch (e: Exception) {
            if (currentCoroutineContext().isActive) {
                onEvent(ChatEvent.Failure(e.message ?: "请求失败"))
            }
        } finally {
            active.compareAndSet(call, null)
        }
    }

    /** Non-streaming completion — used for image captioning via a vision endpoint. */
    suspend fun complete(
        baseUrl: String,
        apiKey: String,
        model: String,
        messages: List<WireMessage>,
        maxTokens: Int? = null,
    ): Result<String> = withContext(Dispatchers.IO) {
        val request = Request.Builder()
            .url(chatUrl(baseUrl))
            .header("Authorization", "Bearer ${apiKey.trim()}")
            .header("Content-Type", "application/json")
            .header("User-Agent", "DeepSeekMobile/1.0")
            .post(buildPayload(model, messages, null, null, maxTokens, stream = false).toRequestBody(JSON))
            .build()
        try {
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    return@withContext Result.failure(IOException(httpErrorMessage(response.code, body)))
                }
                val obj = runCatching { CompactJson.parseToJsonElement(body).jsonObject }.getOrNull()
                    ?: return@withContext Result.failure(IOException("无法解析模型返回"))
                val text = obj["choices"]?.jsonArray?.firstOrNull()?.jsonObject
                    ?.get("message")?.jsonObject?.get("content")?.jsonPrimitive?.contentOrNull
                    ?: obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                    ?: return@withContext Result.failure(IOException("模型没有返回内容"))
                Result.success(text)
            }
        } catch (e: IOException) {
            Result.failure(IOException(humanNetworkError(e)))
        } catch (e: Exception) {
            Result.failure(e)
        }
    }

    /** GET /models — used by the settings "test connection" action. */
    suspend fun listModels(baseUrl: String, apiKey: String): Result<List<String>> = withContext(Dispatchers.IO) {
        runCatching {
            val base = baseUrl.trim().trimEnd('/')
            require(base.startsWith("http://") || base.startsWith("https://")) { "接口地址需要以 http:// 或 https:// 开头" }
            val url = if (base.endsWith("/chat/completions")) base.removeSuffix("/chat/completions") + "/models" else "$base/models"
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .header("User-Agent", "DeepSeekMobile/1.0")
                .get()
                .build()
            client.newCall(request).execute().use { response ->
                val body = response.body?.string().orEmpty()
                if (!response.isSuccessful) error(httpErrorMessage(response.code, body))
                val array = runCatching {
                    CompactJson.parseToJsonElement(body).jsonObject["data"]?.jsonArray
                }.getOrNull() ?: error("接口没有返回模型列表（可忽略，直接聊天试试）")
                array.mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
            }
        }.recoverCatching { e -> throw IOException(e.message ?: "连接失败") }
    }

    // ---------------------------------------------------------------- parsing

    private fun parseChunk(data: String): ChatEvent? {
        val obj = runCatching { CompactJson.parseToJsonElement(data).jsonObject }.getOrNull() ?: return null

        obj["error"]?.let { err ->
            val message = runCatching {
                err.jsonObject["message"]?.jsonPrimitive?.contentOrNull
            }.getOrNull() ?: "服务端返回错误"
            return ChatEvent.Failure(message)
        }

        val choice = obj["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return null
        val delta = choice["delta"]?.jsonObject
        val text = delta?.get("content")?.jsonPrimitive?.contentOrNull.orEmpty()
        val reasoning = (delta?.get("reasoning_content") ?: delta?.get("reasoning"))
            ?.jsonPrimitive?.contentOrNull.orEmpty()
        val finish = choice["finish_reason"]?.jsonPrimitive?.contentOrNull
        val usage = obj["usage"]?.jsonObject?.let { u ->
            Usage(
                promptTokens = u["prompt_tokens"]?.jsonPrimitive?.int ?: 0,
                completionTokens = u["completion_tokens"]?.jsonPrimitive?.int ?: 0,
                totalTokens = u["total_tokens"]?.jsonPrimitive?.int ?: 0,
            )
        }
        return when {
            text.isNotEmpty() || reasoning.isNotEmpty() -> ChatEvent.Delta(text, reasoning)
            finish != null || usage != null -> ChatEvent.Done(usage, finish)
            else -> null
        }
    }

    // ---------------------------------------------------------------- payload

    private fun buildPayload(
        model: String,
        messages: List<WireMessage>,
        temperature: Double?,
        topP: Double?,
        maxTokens: Int?,
        stream: Boolean,
    ): String = buildJsonObject {
        put("model", model)
        put("stream", stream)
        put("messages", buildJsonArray {
            messages.forEach { message ->
                add(buildJsonObject {
                    put("role", message.role)
                    if (message.images.isEmpty()) {
                        put("content", message.text)
                    } else {
                        put("content", buildJsonArray {
                            if (message.text.isNotBlank()) {
                                add(buildJsonObject {
                                    put("type", "text")
                                    put("text", message.text)
                                })
                            }
                            message.images.forEach { url ->
                                add(buildJsonObject {
                                    put("type", "image_url")
                                    putJsonObject("image_url") { put("url", url) }
                                })
                            }
                        })
                    }
                })
            }
        })
        temperature?.let { put("temperature", it) }
        topP?.let { put("top_p", it) }
        maxTokens?.let { put("max_tokens", it) }
    }.toString()

    private fun chatUrl(baseUrl: String): String {
        val base = baseUrl.trim().trimEnd('/')
        require(base.startsWith("http://") || base.startsWith("https://")) { "接口地址需要以 http:// 或 https:// 开头" }
        return if (base.endsWith("/chat/completions")) base else "$base/chat/completions"
    }

    private fun httpErrorMessage(code: Int, body: String?): String {
        val detail = extractServerMessage(body)
        val base = when (code) {
            400 -> "请求格式有误"
            401 -> "API Key 无效或已过期"
            402 -> "账户余额不足，请先充值"
            403 -> "没有权限调用该模型"
            404 -> "接口地址或模型不存在"
            413 -> "内容过长，超出服务端限制"
            422 -> "请求参数不被接受"
            429 -> "请求太频繁，已被限流"
            in 500..599 -> "服务端暂时不可用，请稍后重试"
            else -> "请求失败（HTTP $code）"
        }
        return if (detail.isNullOrBlank() || detail.length > 180) base else "$base · $detail"
    }

    private fun extractServerMessage(body: String?): String? {
        if (body.isNullOrBlank()) return null
        return runCatching {
            val obj = CompactJson.parseToJsonElement(body).jsonObject
            obj["error"]?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                ?: obj["message"]?.jsonPrimitive?.contentOrNull
        }.getOrNull()
    }

    private fun humanNetworkError(e: IOException): String = when {
        e.message?.contains("timeout", ignoreCase = true) == true -> "连接超时，请检查网络或稍后重试"
        e.message?.contains("Canceled", ignoreCase = true) == true -> "请求已取消"
        e is java.net.UnknownHostException -> "无法解析服务器地址，请检查网络或接口地址"
        e is java.net.ConnectException -> "无法连接到服务器，请检查网络或接口地址"
        e is javax.net.ssl.SSLException -> "安全连接失败（证书问题），请检查接口地址"
        else -> "网络错误：${e.message ?: e.javaClass.simpleName}"
    }

    companion object {
        private val JSON = "application/json; charset=utf-8".toMediaType()
    }
}
