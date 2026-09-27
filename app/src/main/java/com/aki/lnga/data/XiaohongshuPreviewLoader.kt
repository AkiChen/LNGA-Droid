package com.aki.lnga.data

import android.content.Context
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

private val XHS_JSON = Json { ignoreUnknownKeys = true }

@Serializable
internal data class XiaohongshuPreview(
    val url: String,
    val title: String = "",
    val description: String = "",
    val author: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val type: String = "note",
    val noteId: String = "",
    val savedAtMillis: Long = System.currentTimeMillis(),
)

internal fun isXiaohongshuUrl(url: String): Boolean = runCatching {
    val host = URI(url).host.orEmpty().lowercase().removePrefix("www.")
    host == "xhslink.cn" || host == "xiaohongshu.com" || host.endsWith(".xiaohongshu.com")
}.getOrDefault(false)

internal class XiaohongshuPreviewLoader(context: Context) {
    private val preferences = context.getSharedPreferences("xiaohongshu_preview_cache", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun load(url: String): XiaohongshuPreview? = withContext(Dispatchers.IO) {
        if (!isXiaohongshuUrl(url)) return@withContext null
        val cacheKey = "preview_${url.hashCode()}"
        preferences.getString(cacheKey, null)?.let { cached ->
            runCatching { XHS_JSON.decodeFromString<XiaohongshuPreview>(cached) }.getOrNull()?.let { preview ->
                if (System.currentTimeMillis() - preview.savedAtMillis < CACHE_MILLIS) return@withContext preview
            }
        }
        val preview = runCatching {
            client.newCall(
                Request.Builder().url(url)
                    .get()
                    .header("User-Agent", MOBILE_UA)
                    .header("Accept-Language", "zh-CN,zh;q=0.9")
                    .build(),
            ).execute().use { response ->
                if (!response.isSuccessful || !isXiaohongshuUrl(response.request.url.toString())) return@use null
                val html = response.peekBody(MAX_HTML_BYTES).string()
                parseXiaohongshuPreviewHtml(html, url)
            }
        }.getOrNull() ?: return@withContext null
        preferences.edit().putString(cacheKey, XHS_JSON.encodeToString(preview)).apply()
        preview
    }

    private companion object {
        const val CACHE_MILLIS = 6L * 60 * 60 * 1000
        const val MAX_HTML_BYTES = 2L * 1024 * 1024
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/150 Mobile Safari/537.36"
    }
}

internal fun parseXiaohongshuPreviewHtml(html: String, fallbackUrl: String): XiaohongshuPreview? {
    val marker = html.indexOf("\"data\":{\"noteData\":{")
        .takeIf { it >= 0 }
        ?: html.indexOf("\"noteData\":{").takeIf { it >= 0 }
        ?: return null
    val source = html.substring(marker, minOf(html.length, marker + 600_000))
    val noteId = source.xhsField("noteId")
    val title = source.xhsField("title")
    val description = source.xhsField("desc")
    if (noteId.isBlank() || title.isBlank() && description.isBlank()) return null

    val userSource = source.substringAfter("\"user\":{", "").take(12_000)
    val imageSource = source.substringAfter("\"imageList\":[", "").take(80_000)
    val type = source.xhsField("type").ifBlank { "note" }.lowercase()
    return XiaohongshuPreview(
        // Keep the shared URL: its token may be needed to open the public note. It is never logged.
        url = fallbackUrl,
        title = title.trim(),
        description = description.trim(),
        author = userSource.xhsField("nickName").trim(),
        avatarUrl = userSource.xhsField("avatar").toHttpsUrl(),
        coverUrl = imageSource.xhsField("url").toHttpsUrl(),
        type = type,
        noteId = noteId,
    )
}

private fun String.xhsField(name: String): String {
    val match = Regex("\\\"${Regex.escape(name)}\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
        .find(this) ?: return ""
    return runCatching {
        XHS_JSON.parseToJsonElement("\"${match.groupValues[1]}\"").jsonPrimitive.content
    }.getOrDefault(match.groupValues[1])
}

private fun String.toHttpsUrl(): String = when {
    startsWith("http://", ignoreCase = true) -> "https://${substringAfter("://")}"
    startsWith("//") -> "https:$this"
    else -> this
}
