package com.aki.lnga.data

import java.io.IOException
import java.net.SocketTimeoutException
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.job
import kotlinx.coroutines.ensureActive
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.buildJsonArray
import kotlinx.serialization.json.buildJsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonArray
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.jsonPrimitive
import kotlinx.serialization.json.put
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull

internal enum class AiErrorKind {
    Unauthorized, RateLimited, ModelNotFound, Timeout, Network, InvalidResponse, Server, Configuration,
}

internal class AiServiceException(val kind: AiErrorKind, message: String) : IOException(message)

internal data class AiConnectionResult(val message: String)

internal data class AiChatRequestMessage(val role: String, val content: String)

internal class AiClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .writeTimeout(15, TimeUnit.SECONDS)
        .build(),
) {
    private val json = Json { ignoreUnknownKeys = true }

    suspend fun listModels(config: AiConfig, apiKey: String): List<String> = withContext(Dispatchers.IO) {
        val request = authorizedRequest(endpoint(config.baseUrl, "models"), apiKey).get().build()
        execute(request) { body ->
            runCatching {
                json.parseToJsonElement(body).jsonObject["data"]?.jsonArray.orEmpty()
                    .mapNotNull { it.jsonObject["id"]?.jsonPrimitive?.contentOrNull }
                    .filter(String::isNotBlank)
                    .distinct()
                    .sorted()
            }.getOrElse { throw AiServiceException(AiErrorKind.InvalidResponse, "服务返回的模型列表格式无法识别") }
                .takeIf(List<String>::isNotEmpty)
                ?: throw AiServiceException(AiErrorKind.InvalidResponse, "服务没有返回可用模型")
        }
    }

    suspend fun testConnection(config: AiConfig, apiKey: String): AiConnectionResult = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("model", config.model)
            put("stream", false)
            if (config.presetId == "openai") put("max_completion_tokens", 1) else put("max_tokens", 1)
            put("messages", buildJsonArray {
                add(buildJsonObject { put("role", "user"); put("content", "Reply OK") })
            })
        }.toString()
        val request = authorizedRequest(endpoint(config.baseUrl, "chat/completions"), apiKey)
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        execute(request) { body ->
            val valid = runCatching {
                json.parseToJsonElement(body).jsonObject["choices"]?.jsonArray?.isNotEmpty() == true
            }.getOrDefault(false)
            if (!valid) throw AiServiceException(AiErrorKind.InvalidResponse, "连接成功，但响应不是兼容的 Chat Completions 格式")
            AiConnectionResult("连接成功，模型 ${config.model} 可用")
        }
    }

    suspend fun streamChat(
        config: AiConfig,
        apiKey: String,
        messages: List<AiChatRequestMessage>,
        onDelta: (String) -> Unit,
    ) = withContext(Dispatchers.IO) {
        val payload = buildJsonObject {
            put("model", config.model)
            put("stream", true)
            put("messages", buildJsonArray {
                messages.forEach { message ->
                    add(buildJsonObject { put("role", message.role); put("content", message.content) })
                }
            })
        }.toString()
        val request = authorizedRequest(endpoint(config.baseUrl, "chat/completions"), apiKey)
            .header("Accept", "text/event-stream")
            .post(payload.toRequestBody(JSON_MEDIA_TYPE))
            .build()
        val call = client.newCall(request)
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                if (!response.isSuccessful) {
                    val body = response.body?.string().orEmpty()
                    throw httpError(response.code, body, apiKey)
                }
                val source = response.body?.source()
                    ?: throw AiServiceException(AiErrorKind.InvalidResponse, "AI 服务没有返回响应内容")
                val pending = StringBuilder()
                var emitted = false
                var lastFlush = System.nanoTime()
                while (!source.exhausted()) {
                    currentCoroutineContext().ensureActive()
                    val line = source.readUtf8Line() ?: break
                    val data = line.removePrefix("data:").trim()
                    if (!line.startsWith("data:") || data.isBlank()) continue
                    if (data == "[DONE]") break
                    val delta = parseChatDelta(data)
                    if (delta.isNotEmpty()) {
                        emitted = true
                        pending.append(delta)
                        val elapsedMillis = (System.nanoTime() - lastFlush) / 1_000_000L
                        if (pending.length >= 48 || elapsedMillis >= 50L) {
                            onDelta(pending.toString())
                            pending.clear()
                            lastFlush = System.nanoTime()
                        }
                    }
                }
                if (pending.isNotEmpty()) onDelta(pending.toString())
                if (!emitted) throw AiServiceException(AiErrorKind.InvalidResponse, "AI 服务未返回可显示的文本")
            }
        } catch (error: AiServiceException) {
            throw error
        } catch (error: SocketTimeoutException) {
            currentCoroutineContext().ensureActive()
            throw AiServiceException(AiErrorKind.Timeout, "连接超时，请检查网络或服务地址")
        } catch (error: IOException) {
            currentCoroutineContext().ensureActive()
            throw AiServiceException(AiErrorKind.Network, "网络连接失败：${error.message.orEmpty().take(120)}")
        } finally {
            cancellation.dispose()
        }
    }

    internal fun parseChatDelta(data: String): String = runCatching {
        val root = json.parseToJsonElement(data).jsonObject
        val choice = root["choices"]?.jsonArray?.firstOrNull()?.jsonObject ?: return@runCatching ""
        val delta = choice["delta"]?.jsonObject
        delta?.get("content")?.jsonPrimitive?.contentOrNull
            ?: delta?.get("reasoning_content")?.jsonPrimitive?.contentOrNull?.let { "" }
            ?: ""
    }.getOrElse {
        throw AiServiceException(AiErrorKind.InvalidResponse, "流式响应格式无法识别")
    }

    private fun endpoint(baseUrl: String, path: String): String {
        val base = normalizeAiBaseUrl(baseUrl).toHttpUrlOrNull()
            ?: throw AiServiceException(AiErrorKind.Configuration, "服务地址格式不正确")
        return base.newBuilder().addPathSegments(path).build().toString()
    }

    private fun authorizedRequest(url: String, apiKey: String): Request.Builder = Request.Builder()
        .url(url)
        .header("Authorization", "Bearer ${apiKey.trim()}")
        .header("Accept", "application/json")
        .header("User-Agent", "LNGA-Android")

    private fun <T> execute(request: Request, parse: (String) -> T): T = try {
        client.newCall(request).execute().use { response ->
            val body = response.body?.string().orEmpty()
            if (!response.isSuccessful) {
                val secret = request.header("Authorization").orEmpty().removePrefix("Bearer ")
                throw httpError(response.code, body, secret)
            }
            parse(body)
        }
    } catch (error: AiServiceException) {
        throw error
    } catch (error: SocketTimeoutException) {
        throw AiServiceException(AiErrorKind.Timeout, "连接超时，请检查网络或服务地址")
    } catch (error: IOException) {
        throw AiServiceException(AiErrorKind.Network, "网络连接失败：${error.message.orEmpty().take(120)}")
    }

    private fun httpError(code: Int, body: String, secret: String): AiServiceException {
        val remoteMessage = runCatching {
            val root = json.parseToJsonElement(body).jsonObject
            val error = root["error"]
            when {
                error == null -> null
                error is kotlinx.serialization.json.JsonObject -> error["message"]?.jsonPrimitive?.contentOrNull
                else -> error.jsonPrimitive.contentOrNull
            }
        }.getOrNull()
            ?.replace(secret, "[redacted]", ignoreCase = false)
            ?.take(160)
        val kind = when (code) {
            401, 403 -> AiErrorKind.Unauthorized
            404 -> AiErrorKind.ModelNotFound
            429 -> AiErrorKind.RateLimited
            in 500..599 -> AiErrorKind.Server
            else -> AiErrorKind.InvalidResponse
        }
        val fallback = when (kind) {
            AiErrorKind.Unauthorized -> "API Key 无效或没有访问权限"
            AiErrorKind.ModelNotFound -> "接口或模型不存在，请检查服务地址和模型名"
            AiErrorKind.RateLimited -> "请求受到限流，请稍后重试"
            AiErrorKind.Server -> "AI 服务暂时不可用"
            else -> "AI 服务返回 HTTP $code"
        }
        return AiServiceException(kind, remoteMessage?.let { "HTTP $code：$it" } ?: fallback)
    }

    private companion object {
        val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()
    }
}
