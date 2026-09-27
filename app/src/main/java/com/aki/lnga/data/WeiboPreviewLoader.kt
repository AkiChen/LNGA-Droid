package com.aki.lnga.data

import android.content.Context
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.jsonPrimitive
import okhttp3.OkHttpClient
import okhttp3.Request

private val WEIBO_JSON = Json { ignoreUnknownKeys = true }

@Serializable
internal data class WeiboPreview(
    val url: String,
    val author: String = "",
    val text: String = "",
    val coverUrl: String = "",
    val avatarUrl: String = "",
    val savedAtMillis: Long = System.currentTimeMillis(),
)

internal fun isWeiboUrl(url: String): Boolean = runCatching {
    val host = java.net.URI(url).host.orEmpty().lowercase().removePrefix("www.")
    host == "weibo.com" || host == "m.weibo.cn" || host == "t.cn" || host.endsWith(".weibo.com")
}.getOrDefault(false)

internal fun extractWeiboStatusId(url: String): String? = runCatching {
    val uri = java.net.URI(url)
    uri.rawQuery.orEmpty().split('&').firstNotNullOfOrNull { part ->
        part.substringBefore('=').takeIf { it == "id" }?.let { part.substringAfter('=', "") }
    }?.takeIf(String::isNotBlank) ?: uri.path.orEmpty().trim('/').substringAfterLast('/').takeIf { id ->
        id.length >= 6 && id.all(Char::isLetterOrDigit)
    }
}.getOrNull()

internal class WeiboPreviewLoader(context: Context) {
    private val preferences = context.getSharedPreferences("weibo_preview_cache", Context.MODE_PRIVATE)
    private val json = WEIBO_JSON
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(15, TimeUnit.SECONDS)
        .followRedirects(true)
        .build()

    suspend fun load(url: String): WeiboPreview? = withContext(Dispatchers.IO) {
        val cacheKey = "preview_${url.hashCode()}"
        preferences.getString(cacheKey, null)?.let { cached ->
            runCatching { json.decodeFromString<WeiboPreview>(cached) }.getOrNull()?.let { preview ->
                if (System.currentTimeMillis() - preview.savedAtMillis < CACHE_MILLIS) return@withContext preview
            }
        }
        val preview = runCatching { loadRemote(url) }.getOrNull() ?: return@withContext null
        preferences.edit().putString(cacheKey, json.encodeToString(preview)).apply()
        preview
    }

    private fun loadRemote(url: String): WeiboPreview? {
        val pageResponse = client.newCall(Request.Builder().url(url).header("User-Agent", MOBILE_UA).build()).execute()
        val resolvedUrl: String
        val html: String
        pageResponse.use { response ->
            if (!response.isSuccessful) return null
            resolvedUrl = response.request.url.toString()
            if (!isWeiboUrl(resolvedUrl)) return null
            html = response.body?.string().orEmpty()
        }
        parseWeiboPreviewHtml(html, resolvedUrl)?.let { return it }
        val statusId = extractWeiboStatusId(resolvedUrl) ?: return null
        val apiUrl = "https://m.weibo.cn/statuses/show?id=$statusId"
        return client.newCall(
            Request.Builder().url(apiUrl).header("User-Agent", MOBILE_UA)
                .header("Referer", resolvedUrl).build(),
        ).execute().use { response ->
            if (!response.isSuccessful) null
            else parseWeiboStatusJson(response.body?.string().orEmpty(), resolvedUrl)
        }
    }

    private companion object {
        const val CACHE_MILLIS = 24L * 60 * 60 * 1000
        const val MOBILE_UA =
            "Mozilla/5.0 (Linux; Android 14) AppleWebKit/537.36 Chrome/124 Mobile Safari/537.36"
    }
}

internal fun parseWeiboPreviewHtml(html: String, fallbackUrl: String): WeiboPreview? {
    val metadata = buildMap {
        META.findAll(html).forEach { meta ->
            val attributes = ATTR.findAll(meta.groupValues[1]).associate {
                it.groupValues[1].lowercase() to decodeWeiboHtml(it.groupValues[3])
            }
            val key = attributes["property"] ?: attributes["name"] ?: return@forEach
            attributes["content"]?.let { put(key.lowercase(), it) }
        }
    }
    val text = (metadata["og:description"] ?: metadata["description"]).orEmpty()
        .substringBefore("_微博").trim()
    if (text.isBlank() || text.contains("登录微博")) return null
    val title = metadata["og:title"].orEmpty().substringBefore("的微博").trim()
    val cover = metadata["og:image"].orEmpty().let { if (it.startsWith("//")) "https:$it" else it }
    return WeiboPreview(
        url = metadata["og:url"]?.takeIf(::isWeiboUrl) ?: fallbackUrl,
        author = title.takeUnless { it == "微博" }.orEmpty(),
        text = stripWeiboHtml(text),
        coverUrl = cover,
    )
}

internal fun parseWeiboStatusJson(source: String, fallbackUrl: String): WeiboPreview? = runCatching {
    val root = WEIBO_JSON.parseToJsonElement(source) as? JsonObject ?: return null
    val status = (root["data"] as? JsonObject) ?: root
    val user = status["user"] as? JsonObject
    val text = status.string("text_raw").ifBlank { stripWeiboHtml(status.string("text")) }
    if (text.isBlank()) return null
    val pics = status["pics"] as? JsonArray
    val firstPic = pics?.firstOrNull() as? JsonObject
    val largePic = firstPic?.get("large") as? JsonObject
    val pageInfo = status["page_info"] as? JsonObject
    val pagePic = pageInfo?.get("page_pic") as? JsonObject
    WeiboPreview(
        url = fallbackUrl,
        author = user.string("screen_name"),
        text = text.trim(),
        coverUrl = largePic.string("url").ifBlank {
            firstPic.string("url").ifBlank { pagePic.string("url") }
        },
        avatarUrl = user.string("avatar_hd").ifBlank { user.string("profile_image_url") },
    )
}.getOrNull()

private fun JsonObject?.string(key: String): String =
    this?.get(key)?.jsonPrimitive?.contentOrNull.orEmpty()

private fun stripWeiboHtml(value: String): String = decodeWeiboHtml(value)
    .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
    .replace(Regex("<[^>]+>"), "")
    .trim()

private fun decodeWeiboHtml(value: String): String = value
    .replace("&amp;", "&").replace("&quot;", "\"").replace("&#39;", "'")
    .replace("&lt;", "<").replace("&gt;", ">").replace("&nbsp;", " ")

private val META = Regex("<meta\\s+([^>]+)>", setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL))
private val ATTR = Regex("([:\\w-]+)\\s*=\\s*(['\"])(.*?)\\2", setOf(RegexOption.DOT_MATCHES_ALL))
