package com.aki.lnga.data

import android.content.Context
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
internal data class BilibiliPreview(
    val url: String,
    val title: String,
    val coverUrl: String = "",
    val uploader: String = "",
    val durationSeconds: Int = 0,
    val savedAtMillis: Long = System.currentTimeMillis(),
)

internal fun isBilibiliUrl(url: String): Boolean = runCatching {
    val host = java.net.URI(url).host.orEmpty().lowercase()
    host == "b23.tv" || host == "bilibili.com" || host.endsWith(".bilibili.com")
}.getOrDefault(false)

internal class BilibiliPreviewLoader(context: Context) {
    private val preferences = context.getSharedPreferences("bilibili_preview_cache", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun load(url: String): BilibiliPreview? = withContext(Dispatchers.IO) {
        val cacheKey = "preview_${url.hashCode()}"
        preferences.getString(cacheKey, null)?.let { cached ->
            runCatching { json.decodeFromString<BilibiliPreview>(cached) }.getOrNull()?.let { preview ->
                if (System.currentTimeMillis() - preview.savedAtMillis < CACHE_MILLIS) return@withContext preview
            }
        }
        runCatching {
            client.newCall(
                Request.Builder().url(url)
                    .header("User-Agent", "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36")
                    .build(),
            ).execute().use { response ->
                check(response.isSuccessful) { "HTTP ${response.code}" }
                val resolvedUrl = response.request.url.toString()
                check(isBilibiliUrl(resolvedUrl)) { "非 Bilibili 跳转" }
                val html = response.body?.string().orEmpty()
                parseBilibiliPreviewHtml(html, resolvedUrl)
            }
        }.getOrNull()?.also { preview ->
            preferences.edit().putString(cacheKey, json.encodeToString(BilibiliPreview.serializer(), preview)).apply()
        }
    }

    private companion object {
        const val CACHE_MILLIS = 7L * 24 * 60 * 60 * 1000
    }
}

internal fun parseBilibiliPreviewHtml(html: String, fallbackUrl: String): BilibiliPreview? {
    val metadata = buildMap {
        META.findAll(html).forEach { meta ->
            val attributes = ATTR.findAll(meta.groupValues[1]).associate {
                it.groupValues[1].lowercase() to decodeHtml(it.groupValues[3])
            }
            val key = attributes["property"] ?: attributes["name"] ?: return@forEach
            attributes["content"]?.let { put(key.lowercase(), it) }
        }
    }
    val title = metadata["og:title"]?.substringBefore("_哔哩哔哩")
        ?: TITLE.find(html)?.groupValues?.get(1)?.let(::decodeHtml)
        ?: return null
    val cover = metadata["og:image"].orEmpty().let { if (it.startsWith("//")) "https:$it" else it }
    val duration = metadata["og:video:duration"]?.toIntOrNull()
        ?: DURATION.find(html)?.groupValues?.get(1)?.toIntOrNull()
        ?: 0
    return BilibiliPreview(
        url = metadata["og:url"]?.takeIf(::isBilibiliUrl) ?: fallbackUrl,
        title = title.trim(),
        coverUrl = cover,
        uploader = metadata["author"].orEmpty().trim(),
        durationSeconds = duration,
    )
}

private fun decodeHtml(value: String): String = value
    .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
    .replace("&lt;", "<").replace("&gt;", ">")

private val META = Regex("<meta\\s+([^>]+)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val ATTR = Regex("([:\\w-]+)\\s*=\\s*(['\"])(.*?)\\2", setOf(RegexOption.DOT_MATCHES_ALL))
private val TITLE = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val DURATION = Regex("\"duration\"\\s*:\\s*(\\d+)")
