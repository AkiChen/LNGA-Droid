package com.aki.lnga.parser

import com.aki.lnga.model.BbNode

/** Platform-independent parser for the most common NGA post markup. */
class BbCodeParser {
    fun parse(source: String): List<BbNode> = Cursor(normalize(source)).parseNodes()

    private class Cursor(private val source: String) {
        private var position: Int = 0

        fun parseNodes(closeTag: String? = null): List<BbNode> {
            val result = mutableListOf<BbNode>()
            val text = StringBuilder()

            fun flushText() {
                if (text.isNotEmpty()) {
                    result += BbNode.Text(text.toString())
                    text.clear()
                }
            }

            while (position < source.length) {
                if (closeTag != null && matches(closeTag)) {
                    flushText()
                    position += closeTag.length
                    return result
                }

                when {
                    consumeExact("[b]") -> {
                        flushText()
                        result += BbNode.Bold(parseNodes("[/b]"))
                    }
                    consumeExact("[i]") -> {
                        flushText()
                        result += BbNode.Italic(parseNodes("[/i]"))
                    }
                    consumeExact("[u]") -> {
                        flushText()
                        result += BbNode.Underline(parseNodes("[/u]"))
                    }
                    consumeExact("[sub]") -> {
                        flushText()
                        result += BbNode.Subscript(parseNodes("[/sub]"))
                    }
                    consumeExact("[sup]") -> {
                        flushText()
                        result += BbNode.Superscript(parseNodes("[/sup]"))
                    }
                    consumeExact("[del]") || consumeExact("[strike]") -> {
                        flushText()
                        val closing = if (source.regionMatches(position - 5, "[del]", 0, 5, true)) "[/del]" else "[/strike]"
                        result += BbNode.Strikethrough(parseNodes(closing))
                    }
                    peekOpenTag("quote") != null -> {
                        consumeOpenTag("quote")
                        flushText()
                        result += BbNode.Quote(parseNodes("[/quote]"))
                    }
                    peekOpenTag("collapse") != null -> {
                        val title = consumeOpenTag("collapse").orEmpty().ifBlank { "展开/折叠" }
                        flushText()
                        result += BbNode.Collapse(title, parseNodes("[/collapse]"))
                    }
                    consumeExact("[lessernuke]") -> {
                        flushText()
                        result += BbNode.Warning(parseNodes("[/lessernuke]"))
                    }
                    peekOpenTag("album") != null -> {
                        val title = consumeOpenTag("album").orEmpty().ifBlank { "相册" }
                        flushText()
                        result += BbNode.Album(title, parseNodes("[/album]"))
                    }
                    consumeExact("[h]") -> {
                        flushText()
                        result += BbNode.Heading(parseNodes("[/h]"))
                    }
                    consumeExact("[p]") -> {
                        flushText()
                        result += BbNode.Paragraph(parseNodes("[/p]"))
                    }
                    consumeHorizontalRule() -> {
                        flushText()
                        result += BbNode.HorizontalRule
                    }
                    peekOpenTag("color") != null -> {
                        val attribute = consumeOpenTag("color").orEmpty()
                        flushText()
                        result += BbNode.Color(attribute, parseNodes("[/color]"))
                    }
                    peekOpenTag("font") != null -> {
                        consumeOpenTag("font")
                        flushText()
                        result += BbNode.Span(parseNodes("[/font]"))
                    }
                    peekOpenTag("size") != null -> {
                        consumeOpenTag("size")
                        flushText()
                        result += BbNode.Span(parseNodes("[/size]"))
                    }
                    peekOpenTag("align") != null -> {
                        val alignment = consumeOpenTag("align").orEmpty().lowercase()
                        flushText()
                        result += BbNode.Align(alignment, parseNodes("[/align]"))
                    }
                    consumeExact("[hip]") -> {
                        flushText()
                        result += parseNodes("[/hip]")
                    }
                    matches("[code]") -> {
                        val end = source.indexOf("[/code]", position + 6, ignoreCase = true)
                        if (end >= 0) {
                            flushText()
                            result += BbNode.Code(source.substring(position + 6, end))
                            position = end + 7
                        } else text.append(source[position++])
                    }
                    peekOpenTag("list") != null -> {
                        val openEnd = source.indexOf(']', position)
                        val end = source.indexOf("[/list]", openEnd + 1, ignoreCase = true)
                        if (openEnd >= 0 && end >= 0) {
                            flushText()
                            val body = source.substring(openEnd + 1, end)
                            val items = body.split("[*]").dropWhile { it.isBlank() }
                                .map { Cursor(it.trim()).parseNodes() }
                            result += BbNode.BulletList(items)
                            position = end + 7
                        } else text.append(source[position++])
                    }
                    peekOpenTag("table") != null -> parseTable(text, result)
                    peekOpenTag("flash") != null -> parseFlash(text, result)
                    peekOpenTag("img") != null -> parseImage(text, result)
                    peekOpenTag("url") != null -> parseLinkTag("url", text, result) { attribute, label ->
                        normalizeWebUrl(attribute.ifBlank { plainText(label).trim() })
                    }
                    peekOpenTag("attach") != null -> parseAttachment(text, result)
                    peekOpenTag("uid") != null -> parseLinkTag("uid", text, result) { attribute, _ ->
                        "https://bbs.nga.cn/nuke.php?func=ucp&uid=${attribute.substringBefore(',')}"
                    }
                    peekOpenTag("pid") != null -> parseLinkTag("pid", text, result) { attribute, _ ->
                        "https://bbs.nga.cn/read.php?pid=${attribute.substringBefore(',')}"
                    }
                    peekOpenTag("tid") != null -> parseLinkTag("tid", text, result) { attribute, _ ->
                        "https://bbs.nga.cn/read.php?tid=${attribute.substringBefore(',')}"
                    }
                    matches("[s:") -> parseEmotion(text, result)
                    matches("https://") || matches("http://") -> parsePlainUrl(text, result)
                    else -> text.append(source[position++])
                }
            }

            flushText()
            return result
        }

        private fun parseImage(text: StringBuilder, result: MutableList<BbNode>) {
            val openEnd = source.indexOf(']', position)
            val end = if (openEnd >= 0) source.indexOf("[/img]", openEnd + 1, ignoreCase = true) else -1
            if (openEnd >= 0 && end >= 0) {
                flush(text, result)
                val url = normalizeImageUrl(source.substring(openEnd + 1, end).trim())
                position = end + 6
                if (url.isNotEmpty()) result += BbNode.Image(url)
            } else text.append(source[position++])
        }

        private fun parseFlash(text: StringBuilder, result: MutableList<BbNode>) {
            val openEnd = source.indexOf(']', position)
            val end = if (openEnd >= 0) source.indexOf("[/flash]", openEnd + 1, ignoreCase = true) else -1
            if (openEnd >= 0 && end >= 0) {
                flush(text, result)
                val url = normalizeMediaUrl(source.substring(openEnd + 1, end).trim())
                position = end + 8
                if (url.isNotEmpty()) result += BbNode.Video(url)
            } else text.append(source[position++])
        }

        private fun parseAttachment(text: StringBuilder, result: MutableList<BbNode>) {
            val openEnd = source.indexOf(']', position)
            val end = if (openEnd >= 0) source.indexOf("[/attach]", openEnd + 1, ignoreCase = true) else -1
            if (openEnd < 0 || end < 0) {
                text.append(source[position++])
                return
            }
            flush(text, result)
            val open = source.substring(position + "attach".length + 1, openEnd)
            val attribute = open.substringAfter('=', "").trim().trim('"', '\'')
            val rawLabel = source.substring(openEnd + 1, end).trim()
            position = end + "[/attach]".length
            if (attribute.isNotBlank()) {
                result += Cursor(rawLabel).parseNodes()
                return
            }
            val url = normalizeAttachmentUrl(rawLabel)
            if (url.isNotBlank()) {
                result += BbNode.Attachment(url = url, name = attachmentName(rawLabel, url))
            } else {
                result += BbNode.Text(rawLabel)
            }
        }

        private fun parseTable(text: StringBuilder, result: MutableList<BbNode>) {
            val openEnd = source.indexOf(']', position)
            val end = if (openEnd >= 0) source.indexOf("[/table]", openEnd + 1, ignoreCase = true) else -1
            if (openEnd < 0 || end < 0) {
                text.append(source[position++])
                return
            }
            flush(text, result)
            val body = source.substring(openEnd + 1, end)
            val rows = TABLE_ROW.findAll(body).mapNotNull { rowMatch ->
                val cells = TABLE_CELL.findAll(rowMatch.groupValues[1]).map { cellMatch ->
                    Cursor(cellMatch.groupValues[1].trim()).parseNodes()
                }.toList()
                cells.takeIf { it.isNotEmpty() }
            }.toList()
            position = end + 8
            if (rows.isNotEmpty()) result += BbNode.Table(rows)
            else result += BbNode.Text(body)
        }

        private fun parseLinkTag(
            name: String,
            text: StringBuilder,
            result: MutableList<BbNode>,
            href: (attribute: String, label: List<BbNode>) -> String,
        ) {
            val openEnd = source.indexOf(']', position)
            val end = if (openEnd >= 0) source.indexOf("[/$name]", openEnd + 1, ignoreCase = true) else -1
            if (openEnd >= 0 && end >= 0) {
                flush(text, result)
                val open = source.substring(position + name.length + 1, openEnd)
                val attribute = open.substringAfter('=', "").trim().trim('"', '\'')
                val label = Cursor(source.substring(openEnd + 1, end)).parseNodes()
                val target = href(attribute, label)
                position = end + name.length + 3
                if (target.isNotBlank()) result += BbNode.Link(target, label) else result += label
            } else text.append(source[position++])
        }

        private fun parseEmotion(text: StringBuilder, result: MutableList<BbNode>) {
            val end = source.indexOf(']', position + 3)
            if (end > position) {
                val parts = source.substring(position + 3, end).split(':', limit = 2)
                if (parts.size == 2) {
                    flush(text, result)
                    result += BbNode.Emotion(parts[0], parts[1])
                    position = end + 1
                    return
                }
            }
            text.append(source[position++])
        }

        private fun parsePlainUrl(text: StringBuilder, result: MutableList<BbNode>) {
            val end = source.indexOfFirstFrom(position) { char ->
                char.isWhitespace() || char == '[' || char == '<' || char in URL_BOUNDARIES
            }.let { if (it < 0) source.length else it }
            val candidate = source.substring(position, end)
            val target = candidate.trimEnd(*TRAILING_URL_PUNCTUATION)
            if (target.isEmpty()) {
                text.append(source[position++])
                return
            }
            flush(text, result)
            result += BbNode.Link(target, listOf(BbNode.Text(target)))
            if (target.length < candidate.length) {
                result += BbNode.Text(candidate.substring(target.length))
            }
            position = end
        }

        private fun peekOpenTag(name: String): String? {
            if (!matches("[$name")) return null
            val end = source.indexOf(']', position + name.length + 1)
            if (end < 0) return null
            val suffix = source.substring(position + name.length + 1, end).trim()
            return if (suffix.isEmpty() || suffix.startsWith('=')) suffix.substringAfter('=', "").trim().trim('"', '\'') else null
        }

        private fun consumeOpenTag(name: String): String? {
            val attribute = peekOpenTag(name) ?: return null
            position = source.indexOf(']', position) + 1
            return attribute
        }

        private fun consumeExact(tag: String): Boolean {
            if (!matches(tag)) return false
            position += tag.length
            return true
        }

        private fun consumeHorizontalRule(): Boolean {
            if (!source.regionMatches(position, "[hr", 0, 3, ignoreCase = true)) return false
            val end = source.indexOf(']', position + 3)
            if (end < 0) return false
            val suffix = source.substring(position + 3, end).trim()
            if (suffix.isNotEmpty() && suffix != "/") return false
            position = end + 1
            return true
        }

        private fun matches(value: String): Boolean =
            source.regionMatches(position, value, 0, value.length, ignoreCase = true)

        private fun flush(text: StringBuilder, result: MutableList<BbNode>) {
            if (text.isNotEmpty()) {
                result += BbNode.Text(text.toString())
                text.clear()
            }
        }
    }

    private companion object {
        val TRAILING_URL_PUNCTUATION = charArrayOf('.', ',', ';', ':', '!', '?', ')', ']', '}', '，', '。', '；', '：', '！', '？')
        val URL_BOUNDARIES = charArrayOf('，', '。', '；', '：', '！', '？', '）', '】', '》')

        inline fun String.indexOfFirstFrom(start: Int, predicate: (Char) -> Boolean): Int {
            for (index in start until length) if (predicate(this[index])) return index
            return -1
        }
        fun normalize(source: String): String = source
            .replace(Regex("<br\\s*/?>", RegexOption.IGNORE_CASE), "\n")
            .replace("&amp;", "&")
            .replace("&nbsp;", " ")
            .replace("&ensp;", "\u2002")
            .replace("&emsp;", "\u2003")
            .replace("&quot;", "\"")
            .replace("&#39;", "'")
            .replace("&lt;", "<")
            .replace("&gt;", ">")
            .replace(Regex("&#x([0-9a-f]+);", RegexOption.IGNORE_CASE)) { match ->
                decodeCodePoint(match.groupValues[1].toIntOrNull(16), match.value)
            }
            .replace(Regex("&#(\\d+);")) { match ->
                decodeCodePoint(match.groupValues[1].toIntOrNull(), match.value)
            }
            .replace("\r\n", "\n")
            .replace('\r', '\n')

        fun decodeCodePoint(value: Int?, fallback: String): String =
            if (value != null && Character.isValidCodePoint(value)) String(Character.toChars(value)) else fallback

        fun normalizeImageUrl(raw: String): String {
            val value = raw.trim()
            return when {
                value.startsWith("https://") || value.startsWith("http://") -> value
                value.startsWith("//") -> "https:$value"
                value.startsWith("./") -> "https://img.nga.cn/attachments/${value.removePrefix("./")}"
                value.startsWith("/") -> "https://img.nga.cn$value"
                value.startsWith("mon_") -> "https://img.nga.cn/attachments/$value"
                else -> value
            }
        }

        fun normalizeWebUrl(raw: String): String = when {
            raw.startsWith("https://") || raw.startsWith("http://") -> raw
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith('/') -> "https://bbs.nga.cn$raw"
            else -> raw
        }

        fun normalizeAttachmentUrl(raw: String): String = when {
            raw.startsWith("https://") || raw.startsWith("http://") -> raw
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("./") -> "https://img.nga.cn/attachments/${raw.removePrefix("./")}"
            raw.startsWith("mon_") -> "https://img.nga.cn/attachments/$raw"
            else -> ""
        }

        fun attachmentName(raw: String, normalizedUrl: String): String = raw
            .substringBefore('?')
            .substringAfterLast('/')
            .takeIf(String::isNotBlank)
            ?: normalizedUrl.substringBefore('?').substringAfterLast('/').ifBlank { "NGA 附件" }

        fun normalizeMediaUrl(raw: String): String = when {
            raw.startsWith("https://") || raw.startsWith("http://") -> raw
            raw.startsWith("//") -> "https:$raw"
            raw.startsWith("./") -> "https://img.nga.cn/attachments/${raw.removePrefix("./")}"
            raw.startsWith('/') -> "https://bbs.nga.cn$raw"
            raw.startsWith("mon_") -> "https://img.nga.cn/attachments/$raw"
            else -> raw
        }

        fun plainText(nodes: List<BbNode>): String = buildString {
            fun appendAll(values: List<BbNode>): Unit {
                values.forEach { node ->
                    when (node) {
                        is BbNode.Text -> append(node.value)
                        is BbNode.Bold -> appendAll(node.children)
                        is BbNode.Italic -> appendAll(node.children)
                        is BbNode.Underline -> appendAll(node.children)
                        is BbNode.Strikethrough -> appendAll(node.children)
                        is BbNode.Color -> appendAll(node.children)
                        is BbNode.Span -> appendAll(node.children)
                        is BbNode.Quote -> appendAll(node.children)
                        is BbNode.Collapse -> appendAll(node.children)
                        is BbNode.Warning -> appendAll(node.children)
                        is BbNode.Album -> appendAll(node.children)
                        is BbNode.Heading -> appendAll(node.children)
                        is BbNode.Paragraph -> appendAll(node.children)
                        is BbNode.Align -> appendAll(node.children)
                        is BbNode.Subscript -> appendAll(node.children)
                        is BbNode.Superscript -> appendAll(node.children)
                        BbNode.HorizontalRule -> Unit
                        is BbNode.Link -> appendAll(node.children)
                        is BbNode.Attachment -> append(node.name)
                        is BbNode.Code -> append(node.value)
                        is BbNode.BulletList -> node.items.forEach { appendAll(it) }
                        is BbNode.Table -> node.rows.forEach { row -> row.forEach(::appendAll) }
                        is BbNode.Image -> append(node.url)
                        is BbNode.Video -> append(node.url)
                        is BbNode.Emotion -> append("[s:${node.category}:${node.code}]")
                    }
                }
            }
            appendAll(nodes)
        }

        val TABLE_ROW = Regex(
            "\\[tr(?:\\s+[^]]*|=[^]]*)?](.*?)\\[/tr]",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
        val TABLE_CELL = Regex(
            "\\[td(?:\\s+[^]]*|=[^]]*)?](.*?)\\[/td]",
            setOf(RegexOption.IGNORE_CASE, RegexOption.DOT_MATCHES_ALL),
        )
    }
}
