package com.lhy.deepseekmobile.voice

import com.lhy.deepseekmobile.util.CompactJson
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.MultipartBody
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.asRequestBody
import java.io.File
import java.io.IOException
import java.util.concurrent.TimeUnit

/**
 * OpenAI-compatible `/audio/transcriptions` client. Works with OpenAI Whisper,
 * SiliconFlow SenseVoice, Groq whisper-large-v3 and similar gateways.
 */
class CloudTranscriber(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(20, TimeUnit.SECONDS)
        .writeTimeout(120, TimeUnit.SECONDS)
        .readTimeout(180, TimeUnit.SECONDS)
        .build()
) {

    suspend fun transcribe(
        baseUrl: String,
        apiKey: String,
        model: String,
        language: String,
        file: File,
    ): Result<String> = withContext(Dispatchers.IO) {
        runCatching {
            val base = baseUrl.trim().trimEnd('/')
            require(base.startsWith("http://") || base.startsWith("https://")) {
                "语音接口地址需要以 http:// 或 https:// 开头"
            }
            val url = if (base.endsWith("/audio/transcriptions")) base else "$base/audio/transcriptions"
            val body = MultipartBody.Builder()
                .setType(MultipartBody.FORM)
                .addFormDataPart("file", file.name, file.asRequestBody("audio/wav".toMediaType()))
                .addFormDataPart("model", model.ifBlank { "whisper-1" })
                .addFormDataPart("language", language.ifBlank { "zh" })
                .addFormDataPart("response_format", "json")
                .build()
            val request = Request.Builder()
                .url(url)
                .header("Authorization", "Bearer ${apiKey.trim()}")
                .header("User-Agent", "DeepSeekMobile/1.0")
                .post(body)
                .build()
            client.newCall(request).execute().use { response ->
                val raw = response.body?.string().orEmpty()
                if (!response.isSuccessful) {
                    val detail = runCatching {
                        CompactJson.parseToJsonElement(raw).jsonObject["error"]
                            ?.jsonObject?.get("message")?.jsonPrimitive?.contentOrNull
                    }.getOrNull()
                    throw IOException(
                        when (response.code) {
                            401 -> "语音服务 Key 无效"
                            402 -> "语音服务余额不足"
                            404 -> "语音接口或模型不存在"
                            413 -> "录音文件过大"
                            429 -> "语音服务限流，请稍后再试"
                            else -> "语音转写失败（HTTP ${response.code}）" + (detail?.let { " · $it" } ?: "")
                        }
                    )
                }
                val text = runCatching {
                    CompactJson.parseToJsonElement(raw).jsonObject["text"]?.jsonPrimitive?.contentOrNull
                }.getOrNull() ?: raw.takeIf { it.isNotBlank() && !it.trimStart().startsWith("{") }
                ?: throw IOException("语音服务没有返回文字")
                text.trim()
            }
        }
    }
}
