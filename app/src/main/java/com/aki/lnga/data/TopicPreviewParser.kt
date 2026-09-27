package com.aki.lnga.data

import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject

internal object TopicPreviewParser {
    private val bbCodeImage = Regex("\\[img(?:=[^]]*)?](.+?)\\[/img]", RegexOption.IGNORE_CASE)
    private val htmlImage = Regex("<img[^>]+src=[\"']([^\"']+)[\"']", RegexOption.IGNORE_CASE)
    private val imageExtension = Regex("\\.(?:jpe?g|png|gif|webp|avif)(?:[?#].*)?$", RegexOption.IGNORE_CASE)

    fun parse(topic: JsonObject): List<String> {
        val result = linkedSetOf<String>()
        val preview = topic.objectOrNull("__P")
        sequenceOf(
            preview?.get("content").text(),
            topic["content"].text(),
        ).filter(String::isNotBlank).forEach { content ->
            bbCodeImage.findAll(content).forEach { add(result, it.groupValues[1]) }
            htmlImage.findAll(content).forEach { add(result, it.groupValues[1]) }
        }
        collectAttachments(preview?.get("attachs"), result)
        collectAttachments(topic["attachs"], result)
        return result.take(3)
    }

    private fun collectAttachments(element: JsonElement?, result: MutableSet<String>) {
        when (element) {
            is JsonObject -> {
                sequenceOf("thumb", "thumbnail", "attachurl", "url", "src")
                    .map { element[it].text() }
                    .firstNotNullOfOrNull(::normalizeImageUrl)
                    ?.let(result::add)
                element.values.forEach { collectAttachments(it, result) }
            }
            is JsonArray -> element.forEach { collectAttachments(it, result) }
            else -> Unit
        }
    }

    private fun add(result: MutableSet<String>, raw: String) {
        normalizeImageUrl(raw)?.let(result::add)
    }

    private fun normalizeImageUrl(raw: String): String? {
        val decoded = raw.trim().replace("&amp;", "&")
        val normalized = when {
            decoded.startsWith("//") -> "https:$decoded"
            decoded.startsWith("http://") -> decoded.replaceFirst("http://", "https://")
            decoded.startsWith("https://") -> decoded
            decoded.startsWith("./") -> "https://img.nga.cn/attachments/${decoded.removePrefix("./")}"
            decoded.startsWith("/attachments/") -> "https://img.nga.cn$decoded"
            decoded.startsWith("attachments/") -> "https://img.nga.cn/$decoded"
            decoded.startsWith("mon_") -> "https://img.nga.cn/attachments/$decoded"
            else -> return null
        }.replace("img.nga.178.com", "img.nga.cn")
            .replace("img.ngacn.cc", "img.nga.cn")
            .replace("img.nga.donews.com", "img.nga.cn")
            .replace("img.ngabbs.com", "img.nga.cn")
        return normalized.takeIf(imageExtension::containsMatchIn)
    }
}
