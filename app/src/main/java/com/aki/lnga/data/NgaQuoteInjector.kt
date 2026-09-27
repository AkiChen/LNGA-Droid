package com.aki.lnga.data

import com.aki.lnga.model.Post

internal object NgaQuoteInjector {
    private val replyHeader = Regex(
        "\\[b]Reply to \\[pid=(\\d+),(\\d+),(\\d+)]Reply\\[/pid] Post by \\[uid=(\\d+)](.*?)\\[/uid] \\(([^)]+)\\)",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val pidInQuote = Regex("\\[pid=(\\d+),(\\d+),(\\d+)]", RegexOption.IGNORE_CASE)
    private val simpleReplyHeader = Regex(
        "\\[b]Reply to \\[pid=.*?\\[/pid]\\s*\\[/b]",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )
    private val postByHeader = Regex(
        "\\[pid=.*?Reply\\[/pid]\\s*\\[b]Post by.*?\\(.*?\\):\\[/b]",
        setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
    )

    fun inject(posts: List<Post>, contextPosts: List<Post> = emptyList()): List<Post> {
        val originalByPid = buildMap<Long, String> {
            fun collect(values: List<Post>) {
                values.forEach { post ->
                    if (post.pid > 0) put(post.pid, post.content)
                    collect(post.comments)
                }
            }
            collect(posts)
            collect(contextPosts)
        }

        fun transform(post: Post): Post {
            val injected = injectQuickReply(post.content, originalByPid)
            val completed = completeTruncatedQuote(injected, originalByPid)
            return post.copy(
                content = completed,
                comments = post.comments.map(::transform),
            )
        }
        return posts.map(::transform)
    }

    private fun injectQuickReply(content: String, originals: Map<Long, String>): String {
        if (content.startsWith("[quote]", ignoreCase = true)) return content
        val match = replyHeader.find(content) ?: return content
        val raw = originals[match.groupValues[1].toLongOrNull()] ?: return content
        val cleaned = cleanOriginal(raw)
        if (cleaned.isBlank()) return content
        val headerEnd = content.indexOf("[/b]", match.range.first, ignoreCase = true)
        val replyText = if (headerEnd >= 0) content.substring(headerEnd + 4) else ""
        return buildString {
            append("[quote][pid=${match.groupValues[1]},${match.groupValues[2]},${match.groupValues[3]}]Reply[/pid] ")
            append("[b]Post by [uid=${match.groupValues[4]}]${match.groupValues[5]}[/uid] (${match.groupValues[6]}):[/b]\n")
            append(cleaned)
            append("[/quote]\n<br/><br/>")
            append(replyText)
        }
    }

    private fun completeTruncatedQuote(content: String, originals: Map<Long, String>): String {
        if (!content.startsWith("[quote]", ignoreCase = true)) return content
        val pid = pidInQuote.find(content)?.groupValues?.get(1)?.toLongOrNull() ?: return content
        val full = originals[pid]?.let(::cleanOriginal).orEmpty()
        if (full.isBlank()) return content
        val quoteStart = content.indexOf("[quote]", ignoreCase = true) + 7
        val headerEnd = content.indexOf("[/b]", quoteStart, ignoreCase = true)
        val quoteEnd = content.indexOf("[/quote]", quoteStart, ignoreCase = true)
        if (headerEnd < 0 || quoteEnd < 0) return content
        val existing = content.substring(headerEnd + 4, quoteEnd).removeBreaks().trim()
        if (existing == full || !full.startsWith(existing)) return content
        return content.substring(0, headerEnd + 4) + "\n" + full + content.substring(quoteEnd)
    }

    private fun cleanOriginal(content: String): String = content
        .replace(simpleReplyHeader, "")
        .replace(postByHeader, "")
        .replace(Regex("\\[/?quote]", RegexOption.IGNORE_CASE), "")
        .removeBreaks()
        .trim()

    private fun String.removeBreaks(): String = replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "")
}
