package com.aki.lnga.data

import com.aki.lnga.model.BbNode
import com.aki.lnga.model.Post
import com.aki.lnga.parser.BbCodeParser

internal data class SummaryPage(val page: Int, val posts: List<Post>)

internal data class TopicSummarySource(
    val text: String,
    val includedPostCount: Int,
    val totalPostCount: Int,
    val omittedPostCount: Int,
    val characterCount: Int,
    val pageRange: String,
    val imageCount: Int,
    val videoCount: Int,
    val truncated: Boolean,
)

internal fun buildTopicSummarySource(
    title: String,
    pages: List<SummaryPage>,
    maxCharacters: Int = 32_000,
): TopicSummarySource {
    val parser = BbCodeParser()
    var imageCount = 0
    var videoCount = 0
    val seen = mutableSetOf<String>()
    val blocks = pages.sortedBy(SummaryPage::page).flatMap { page ->
        page.posts.sortedBy(Post::floor).mapNotNull { post ->
            val rendered = renderSummaryNodes(parser.parse(post.content)) { images, videos ->
                imageCount += images
                videoCount += videos
            }.trim()
            if (rendered.isBlank()) return@mapNotNull null
            val fingerprint = rendered.lowercase().replace(Regex("\\s+"), " ").trim()
            if (!seen.add(fingerprint)) return@mapNotNull null
            SummaryBlock(
                order = page.page * 100_000 + post.floor,
                text = "#${post.floor} ${post.author.ifBlank { "匿名" }}：\n$rendered",
            )
        }
    }
    val contentBudget = (maxCharacters - 600).coerceAtLeast((maxCharacters / 3).coerceAtLeast(1))
    val selected = selectSummaryBlocks(blocks, contentBudget)
    val selectedOrders = selected.mapTo(mutableSetOf(), SummaryBlock::order)
    val contentWasTrimmed = selected.sumOf { it.text.length } < blocks.sumOf { it.text.length }
    val body = buildString {
        append("主题：").append(title.trim()).append('\n')
        append("范围：").append(formatPageRange(pages.map(SummaryPage::page))).append('\n')
        append("楼层：发送 ").append(selected.size).append(" / ").append(blocks.size).append(" 条")
        if (selected.size < blocks.size) append("（超出预算的楼层已省略）")
        else if (contentWasTrimmed) append("（部分超长楼层已截断）")
        append("\n\n")
        var previousIndex = -1
        selected.forEachIndexed { index, block ->
            val sourceIndex = blocks.indexOfFirst { it.order == block.order }
            if (previousIndex >= 0 && sourceIndex > previousIndex + 1) append("\n……中间部分楼层因长度限制省略……\n\n")
            append(block.text.take(maxCharacters.coerceAtLeast(1))).append("\n\n")
            previousIndex = sourceIndex
        }
    }.trim().take(maxCharacters)
    return TopicSummarySource(
        text = body,
        includedPostCount = selectedOrders.size,
        totalPostCount = blocks.size,
        omittedPostCount = (blocks.size - selectedOrders.size).coerceAtLeast(0),
        characterCount = body.length,
        pageRange = formatPageRange(pages.map(SummaryPage::page)),
        imageCount = imageCount,
        videoCount = videoCount,
        truncated = selected.size < blocks.size || contentWasTrimmed ||
            body.length >= maxCharacters,
    )
}

private data class SummaryBlock(val order: Int, val text: String)

private fun selectSummaryBlocks(blocks: List<SummaryBlock>, budget: Int): List<SummaryBlock> {
    if (blocks.sumOf { it.text.length + 2 } <= budget) return blocks
    val selected = linkedMapOf<Int, SummaryBlock>()
    var remaining = budget.coerceAtLeast(1)
    val frontBudget = (remaining * 0.6f).toInt()
    var frontUsed = 0
    for (block in blocks) {
        if (frontUsed + block.text.length > frontBudget && selected.isNotEmpty()) break
        val fitted = block.copy(text = block.text.take(remaining))
        selected[block.order] = fitted
        frontUsed += fitted.text.length
        remaining -= fitted.text.length
        if (remaining <= 0) break
    }
    for (block in blocks.asReversed()) {
        if (remaining <= 0 || block.order in selected) continue
        val fitted = block.copy(text = block.text.takeLast(remaining))
        selected[block.order] = fitted
        remaining -= fitted.text.length
    }
    return selected.values.sortedBy(SummaryBlock::order)
}

private fun formatPageRange(pages: List<Int>): String {
    val sorted = pages.distinct().sorted()
    return when {
        sorted.isEmpty() -> "无可用页面"
        sorted.size == 1 -> "第 ${sorted.single()} 页"
        else -> "第 ${sorted.first()}–${sorted.last()} 页"
    }
}

private fun renderSummaryNodes(nodes: List<BbNode>, onMedia: (Int, Int) -> Unit): String = buildString {
    fun render(values: List<BbNode>) {
        values.forEach { node ->
            when (node) {
                is BbNode.Text -> append(node.value)
                is BbNode.Bold -> render(node.children)
                is BbNode.Italic -> render(node.children)
                is BbNode.Underline -> render(node.children)
                is BbNode.Strikethrough -> render(node.children)
                is BbNode.Color -> render(node.children)
                is BbNode.Span -> render(node.children)
                is BbNode.Quote -> { append("\n[引用] "); render(node.children); append('\n') }
                is BbNode.Collapse -> { append("\n[折叠：${node.title}] "); render(node.children); append('\n') }
                is BbNode.Warning -> { append("\n[提醒] "); render(node.children); append('\n') }
                is BbNode.Album -> { append("\n[相册：${node.title}] "); render(node.children); append('\n') }
                is BbNode.Heading -> { append('\n'); render(node.children); append('\n') }
                is BbNode.Paragraph -> { append('\n'); render(node.children); append('\n') }
                is BbNode.Align -> render(node.children)
                is BbNode.Subscript -> render(node.children)
                is BbNode.Superscript -> render(node.children)
                BbNode.HorizontalRule -> append("\n---\n")
                is BbNode.Code -> append("\n[代码]\n${node.value}\n")
                is BbNode.BulletList -> node.items.forEach { item -> append("\n- "); render(item) }
                is BbNode.Table -> {
                    append("\n[表格]\n")
                    node.rows.forEach { row ->
                        append("| ")
                        row.forEachIndexed { index, cell ->
                            if (index > 0) append(" | ")
                            render(cell)
                        }
                        append(" |\n")
                    }
                }
                is BbNode.Link -> {
                    val start = length
                    render(node.children)
                    val label = substring(start).trim()
                    if (node.href.isNotBlank() && !label.equals(node.href, ignoreCase = true)) append(" (${node.href})")
                }
                is BbNode.Attachment -> append("[附件：${node.name}]")
                is BbNode.Image -> { append("[图片]"); onMedia(1, 0) }
                is BbNode.Video -> { append("[视频：${node.url}]"); onMedia(0, 1) }
                is BbNode.Emotion -> append("[表情]")
            }
        }
    }
    render(nodes)
}.replace(Regex("[ \\t]+\\n"), "\n")
    .replace(Regex("\\n{3,}"), "\n\n")
    .trim()
