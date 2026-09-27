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

private val DOUYIN_JSON = Json { ignoreUnknownKeys = true }

@Serializable
internal data class DouyinPreview(
    val url: String,
    val title: String = "",
    val author: String = "",
    val avatarUrl: String = "",
    val coverUrl: String = "",
    val durationSeconds: Int = 0,
    val videoId: String = "",
    val savedAtMillis: Long = System.currentTimeMillis(),
)

internal fun isDouyinUrl(url: String): Boolean = runCatching {
    val host = URI(url).host.orEmpty().lowercase().removePrefix("www.")
    host == "v.douyin.com" || host == "douyin.com" || host.endsWith(".douyin.com") ||
        host == "iesdouyin.com" || host.endsWith(".iesdouyin.com")
}.getOrDefault(false)

internal fun extractDouyinVideoId(url: String): String? = runCatching {
    URI(url).path.orEmpty().split('/').filter(String::isNotBlank)
        .let { parts ->
            val marker = parts.indexOfLast { it == "video" }
            marker.takeIf { it >= 0 }?.let { parts.getOrNull(it + 1) }
                ?.takeIf { it.all(Char::isDigit) }
        }
}.getOrNull()

internal class DouyinPreviewLoader(context: Context) {
    private val preferences = context.getSharedPreferences("douyin_preview_cache", Context.MODE_PRIVATE)
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(18, TimeUnit.SECONDS)
        .followRedirects(true)
        .followSslRedirects(true)
        .build()

    suspend fun load(url: String): DouyinPreview? = withContext(Dispatchers.IO) {
        if (!isDouyinUrl(url)) return@withContext null
        val cacheKey = "preview_${url.hashCode()}"
        preferences.getString(cacheKey, null)?.let { cached ->
            runCatching { DOUYIN_JSON.decodeFromString<DouyinPreview>(cached) }.getOrNull()?.let { preview ->
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
                if (!response.isSuccessful) return@use null
                val resolvedUrl = response.request.url.toString()
                if (!isDouyinUrl(resolvedUrl)) return@use null
                val html = response.peekBody(MAX_HTML_BYTES).string()
                parseDouyinPreviewHtml(html, fallbackUrl = url, resolvedUrl = resolvedUrl)
            }
        }.getOrNull() ?: return@withContext null
        preferences.edit().putString(cacheKey, DOUYIN_JSON.encodeToString(preview)).apply()
        preview
    }

    private companion object {
        const val CACHE_MILLIS = 6L * 60 * 60 * 1000
        const val MAX_HTML_BYTES = 2L * 1024 * 1024
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 16) AppleWebKit/537.36 Chrome/150 Mobile Safari/537.36"
    }
}

internal fun parseDouyinPreviewHtml(
    html: String,
    fallbackUrl: String,
    resolvedUrl: String = fallbackUrl,
): DouyinPreview? {
    val metadata = parseDouyinMetadata(html)
    val canonical = parseDouyinCanonical(html)
        ?.takeIf(::isDouyinUrl)
        ?: resolvedUrl.takeIf(::isDouyinUrl)
        ?: fallbackUrl
    val videoId = sequenceOf(canonical, resolvedUrl, fallbackUrl)
        .mapNotNull(::extractDouyinVideoId)
        .firstOrNull()
        .orEmpty()

    val authorSource = html.substringAfter("\"author\":{", "").take(80_000)
    val coverSource = html.substringAfter("\"cover\":{", "").take(80_000)
    val title = sequenceOf(
        metadata["og:title"],
        metadata["twitter:title"],
        html.douyinJsonField("desc"),
        DOUYIN_TITLE.find(html)?.groupValues?.getOrNull(1)?.let(::decodeDouyinHtml),
    ).filterNotNull().map(String::trim).firstOrNull { it.isNotBlank() && !it.isGenericDouyinTitle() }.orEmpty()
    val author = sequenceOf(
        metadata["author"],
        authorSource.douyinJsonField("nickname"),
        html.douyinJsonField("nickname"),
    ).filterNotNull().map(String::trim).firstOrNull(String::isNotBlank).orEmpty()
    val cover = sequenceOf(
        metadata["og:image"],
        metadata["twitter:image"],
        coverSource.douyinFirstUrl(),
    ).filterNotNull().map(String::toHttpsDouyinUrl).firstOrNull(String::isNotBlank).orEmpty()
    val avatar = authorSource.substringAfter("\"avatar_thumb\":{", "").take(30_000)
        .douyinFirstUrl().orEmpty().toHttpsDouyinUrl()
    val metadataDurationSeconds = metadata["og:video:duration"]?.toLongOrNull()
    val hydratedDurationMillis = DOUYIN_DURATION.find(html)?.groupValues?.getOrNull(1)?.toLongOrNull()
    val durationSeconds = metadataDurationSeconds ?: hydratedDurationMillis?.div(1000L) ?: 0L

    if (videoId.isBlank() && title.isBlank() && cover.isBlank()) return null
    return DouyinPreview(
        url = canonical,
        title = title,
        author = author,
        avatarUrl = avatar,
        coverUrl = cover,
        durationSeconds = durationSeconds.coerceIn(0, Int.MAX_VALUE.toLong()).toInt(),
        videoId = videoId,
    )
}

private fun parseDouyinMetadata(html: String): Map<String, String> = buildMap {
    DOUYIN_META.findAll(html).forEach { meta ->
        val attributes = DOUYIN_ATTR.findAll(meta.groupValues[1]).associate {
            it.groupValues[1].lowercase() to decodeDouyinHtml(it.groupValues[3])
        }
        val key = attributes["property"] ?: attributes["name"] ?: return@forEach
        attributes["content"]?.let { put(key.lowercase(), it) }
    }
}

private fun parseDouyinCanonical(html: String): String? = DOUYIN_LINK.findAll(html).firstNotNullOfOrNull { link ->
    val attributes = DOUYIN_ATTR.findAll(link.groupValues[1]).associate {
        it.groupValues[1].lowercase() to decodeDouyinHtml(it.groupValues[3])
    }
    attributes["href"]?.takeIf { attributes["rel"].equals("canonical", ignoreCase = true) }
}

private fun String.douyinJsonField(name: String): String? {
    val encoded = Regex("\\\"${Regex.escape(name)}\\\"\\s*:\\s*\\\"((?:\\\\.|[^\\\"\\\\])*)\\\"")
        .find(this)?.groupValues?.getOrNull(1) ?: return null
    return runCatching {
        DOUYIN_JSON.parseToJsonElement("\"$encoded\"").jsonPrimitive.content
    }.getOrDefault(encoded)
}

private fun String.douyinFirstUrl(): String? {
    val encoded = DOUYIN_URL_LIST.find(this)?.groupValues?.getOrNull(1) ?: return null
    return runCatching {
        DOUYIN_JSON.parseToJsonElement("\"$encoded\"").jsonPrimitive.content
    }.getOrDefault(encoded)
}

private fun String.toHttpsDouyinUrl(): String = when {
    startsWith("http://", ignoreCase = true) -> "https://${substringAfter("://")}"
    startsWith("//") -> "https:$this"
    else -> this
}

private fun String.isGenericDouyinTitle(): Boolean =
    equals("抖音", ignoreCase = true) || contains("在抖音记录美好生活") || equals("抖音-记录美好生活")

private fun decodeDouyinHtml(value: String): String = value
    .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
    .replace("&lt;", "<").replace("&gt;", ">")

private val DOUYIN_META = Regex("<meta\\s+([^>]+)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val DOUYIN_LINK = Regex("<link\\s+([^>]+)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val DOUYIN_ATTR = Regex("([:\\w-]+)\\s*=\\s*(['\"])(.*?)\\2", setOf(RegexOption.DOT_MATCHES_ALL))
private val DOUYIN_TITLE = Regex("<title[^>]*>(.*?)</title>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val DOUYIN_DURATION = Regex("\"duration\"\\s*:\\s*(\\d+)")
private val DOUYIN_URL_LIST = Regex("\"url_list\"\\s*:\\s*\\[\\s*\"((?:\\\\.|[^\"\\\\])*)\"")
