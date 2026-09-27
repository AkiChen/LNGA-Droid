package com.aki.lnga.data

import com.aki.lnga.model.Pagination
import com.aki.lnga.model.Post
import com.aki.lnga.model.ThreadPage
import kotlinx.serialization.json.JsonObject

/** Minimal port of LNGA's read.php HTML fallback parser. */
internal object NgaHtmlThreadParser {
    private const val POST_MARKER = "commonui.postArg.proc("
    private const val USER_MARKER = "commonui.userInfo.setAll("
    private const val DEFAULT_MARKER = "commonui.postArg.setDefault("

    fun parse(html: String, requestedTid: Long, requestedPage: Int): ThreadPage {
        val tid = extractIntVariable(html, "__CURRENT_TID", requestedTid)
        val fid = extractIntVariable(html, "__CURRENT_FID", 0)
        val page = extractIntVariable(html, "__CURRENT_PAGE", requestedPage.toLong()).toInt()
        val rowsPerPage = extractIntVariable(html, "__CURRENT_PAGE_POSTS", 20).toInt().coerceAtLeast(1)
        val users = extractUsers(html)
        val posts = extractPostArguments(html).mapNotNull { args ->
            val floor = args[0].trim().toIntOrNull() ?: return@mapNotNull null
            val pid = args.getOrNull(10)?.trim()?.toLongOrNull() ?: return@mapNotNull null
            if (pid < 0 || (pid == 0L && floor != 0)) return@mapNotNull null
            val authorId = args.getOrNull(13)?.unquote()?.toLongOrNull() ?: 0
            val user = users[authorId] ?: NgaUserPresentation("未知用户", "", false)
            val scores = args.getOrNull(15)?.unquote()?.split(',').orEmpty()
            Post(
                tid = tid,
                fid = fid,
                pid = pid,
                floor = floor,
                author = user.name,
                authorId = authorId,
                subject = extractElementContent(html, "postsubject$floor", "h3"),
                content = extractElementContent(html, "postcontent$floor"),
                postDateSeconds = args.getOrNull(14)?.trim()?.trimEnd('t')?.toLongOrNull() ?: 0,
                avatarUrl = user.avatarUrl,
                isAnonymous = user.isAnonymous,
                scoreDown = scores.getOrNull(0)?.toIntOrNull() ?: 0,
                score = scores.getOrNull(1)?.toIntOrNull() ?: 0,
            )
        }.distinctBy { it.pid }.sortedBy { it.floor }

        if (posts.isEmpty()) throw NgaApiException("HTML 降级页面中没有找到帖子楼层")
        val defaultArguments = extractDefaultArguments(html)
        val replies = defaultArguments
            ?.getOrNull(defaultArguments.size - 3)
            ?.trim()?.toIntOrNull()
        val totalRows = replies?.plus(1)?.coerceAtLeast(posts.size) ?: posts.size
        return ThreadPage(
            title = extractElementContent(html, "currentTopicName", "h1").ifBlank { "主题 $tid" },
            forumName = extractElementContent(html, "currentForumName", "h2"),
            posts = posts,
            pagination = Pagination(
                page = page,
                totalPages = ((totalRows + rowsPerPage - 1) / rowsPerPage).coerceAtLeast(1),
                totalRows = totalRows,
            ),
        )
    }

    private fun extractPostArguments(html: String): List<List<String>> {
        val result = mutableListOf<List<String>>()
        var searchFrom = 0
        while (true) {
            val marker = html.indexOf(POST_MARKER, searchFrom)
            if (marker < 0) break
            val open = marker + POST_MARKER.length - 1
            val call = scanBalanced(html, open, '(', ')') ?: break
            result += splitTopLevelArguments(call.substring(1, call.length - 1))
            searchFrom = open + call.length
        }
        return result
    }

    private fun extractDefaultArguments(html: String): List<String>? {
        val marker = html.indexOf(DEFAULT_MARKER)
        if (marker < 0) return null
        val open = marker + DEFAULT_MARKER.length - 1
        val call = scanBalanced(html, open, '(', ')') ?: return null
        return splitTopLevelArguments(call.substring(1, call.length - 1))
    }

    private fun extractUsers(html: String): Map<Long, NgaUserPresentation> {
        val marker = html.indexOf(USER_MARKER)
        if (marker < 0) return emptyMap()
        val objectStart = html.indexOf('{', marker + USER_MARKER.length)
        if (objectStart < 0) return emptyMap()
        val source = scanBalanced(html, objectStart, '{', '}') ?: return emptyMap()
        val root = runCatching { NgaJson.parse(source) }.getOrNull() ?: return emptyMap()
        return root.mapNotNull { (uid, value) ->
            val user = value as? JsonObject ?: return@mapNotNull null
            val encodedName = user["username"].text()
            uid.toLongOrNull()?.let {
                it to NgaUserPresentation(
                    name = decodeAnonymousName(encodedName).ifBlank { "未知用户" },
                    avatarUrl = normalizeAvatarUrl(user["avatar"].text()),
                    isAnonymous = encodedName.startsWith("#anony_"),
                )
            }
        }.toMap()
    }

    private fun extractIntVariable(html: String, name: String, default: Long): Long {
        val marker = html.indexOf(name)
        if (marker < 0) return default
        val equals = html.indexOf('=', marker + name.length)
        if (equals < 0) return default
        var index = equals + 1
        while (index < html.length && html[index].isWhitespace()) index++
        val start = index
        if (index < html.length && html[index] == '-') index++
        while (index < html.length && html[index].isDigit()) index++
        return html.substring(start, index).toLongOrNull() ?: default
    }

    private fun extractElementContent(html: String, id: String, expectedTag: String? = null): String {
        val single = "id='$id'"
        val double = "id=\"$id\""
        val marker = listOf(html.indexOf(single), html.indexOf(double)).filter { it >= 0 }.minOrNull() ?: return ""
        val tagStart = html.lastIndexOf('<', marker)
        if (tagStart < 0) return ""
        val actualTag = html.substring(tagStart + 1)
            .takeWhile { it.isLetterOrDigit() }
            .lowercase()
        val tag = expectedTag ?: actualTag
        if (tag.isBlank() || (expectedTag != null && actualTag != expectedTag)) return ""
        val tagEnd = html.indexOf('>', marker)
        if (tagEnd < 0) return ""
        val end = html.indexOf("</$tag>", tagEnd + 1)
        if (end < 0) return ""
        return decodeHtmlEntities(html.substring(tagEnd + 1, end).trim())
    }

    internal fun splitTopLevelArguments(source: String): List<String> {
        val result = mutableListOf<String>()
        var start = 0
        var depth = 0
        var quote: Char? = null
        var escaped = false
        source.forEachIndexed { index, character ->
            if (quote != null) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == quote -> quote = null
                }
            } else {
                when (character) {
                    '\'', '"' -> quote = character
                    '(', '[', '{' -> depth++
                    ')', ']', '}' -> depth--
                    ',' -> if (depth == 0) {
                        result += source.substring(start, index).trim()
                        start = index + 1
                    }
                }
            }
        }
        if (start <= source.length) result += source.substring(start).trim()
        return result
    }

    private fun scanBalanced(source: String, start: Int, open: Char, close: Char): String? {
        if (start !in source.indices || source[start] != open) return null
        var depth = 0
        var quote: Char? = null
        var escaped = false
        for (index in start until source.length) {
            val character = source[index]
            if (quote != null) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == quote -> quote = null
                }
                continue
            }
            when {
                character == '\'' || character == '"' -> quote = character
                character == open -> depth++
                character == close -> {
                    depth--
                    if (depth == 0) return source.substring(start, index + 1)
                }
            }
        }
        return null
    }

    private fun decodeHtmlEntities(value: String): String {
        var result = value
            .replace("&amp;", "&")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&nbsp;", " ")
            .replace("&ensp;", "\u2002")
            .replace("&emsp;", "\u2003")
        result = Regex("&#(\\d+);").replace(result) { match ->
            match.groupValues[1].toIntOrNull()?.let { Character.toChars(it).concatToString() } ?: match.value
        }
        result = Regex("&#x([0-9a-fA-F]+);").replace(result) { match ->
            match.groupValues[1].toIntOrNull(16)?.let { Character.toChars(it).concatToString() } ?: match.value
        }
        return result
    }

    private fun String.unquote(): String = trim().let { value ->
        if (value.length >= 2 && ((value.first() == '\'' && value.last() == '\'') ||
                (value.first() == '"' && value.last() == '"'))
        ) value.substring(1, value.length - 1) else value
    }
}
