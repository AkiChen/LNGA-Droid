package com.aki.lnga.data

import com.aki.lnga.model.PostAttachment
import java.net.URLDecoder
import java.nio.charset.StandardCharsets
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object NgaAttachmentParser {
    fun parse(value: JsonElement?): List<PostAttachment> = elements(value).mapNotNull { element ->
        val item = element as? JsonObject ?: return@mapNotNull null
        val rawUrl = sequenceOf("attachurl", "url", "src")
            .map { key -> item[key].text().trim() }
            .firstOrNull(String::isNotBlank)
            .orEmpty()
        val url = normalizeUrl(rawUrl)
        if (url.isBlank()) return@mapNotNull null
        val originalName = decodeName(item["url_utf8_org_name"].text())
        val fallbackName = item["name"].text().trim()
            .ifBlank { url.substringBefore('?').substringAfterLast('/').ifBlank { "NGA 附件" } }
        PostAttachment(
            url = url,
            name = originalName.ifBlank { fallbackName },
            sizeLabel = formatSize(item["size"].text()),
        )
    }.distinctBy { it.url }

    internal fun normalizeUrl(raw: String): String = when {
        raw.startsWith("https://") || raw.startsWith("http://") -> raw
        raw.startsWith("//") -> "https:$raw"
        raw.startsWith("./") -> "https://img.nga.cn/attachments/${raw.removePrefix("./")}"
        raw.startsWith("mon_") -> "https://img.nga.cn/attachments/$raw"
        else -> ""
    }

    internal fun decodeName(value: String): String = runCatching {
        URLDecoder.decode(value, StandardCharsets.UTF_8.name())
    }.getOrDefault(value).trim()

    internal fun formatSize(value: String): String {
        val size = value.trim().toLongOrNull() ?: return value.trim()
        return when {
            size >= 1024 * 1024 -> "%.1f GB".format(size / (1024.0 * 1024.0))
            size >= 1024 -> "%.1f MB".format(size / 1024.0)
            else -> "$size KB"
        }
    }

    private fun elements(value: JsonElement?): Collection<JsonElement> = when (value) {
        is JsonArray -> value
        is JsonObject -> value.values
        else -> emptyList()
    }
}
