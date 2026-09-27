package com.aki.lnga.ui

import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AiMarkdownTest {
    @Test
    fun parsesParagraphListQuoteAndFencedCode() {
        val blocks = parseMarkdownBlocks("正文\n\n- 项目\n> 引用\n```kotlin\nval a = 1\n```")

        assertEquals(
            listOf(MarkdownBlockType.Paragraph, MarkdownBlockType.List, MarkdownBlockType.Quote, MarkdownBlockType.Code),
            blocks.map { it.type },
        )
        assertEquals("val a = 1", blocks.last().text)
    }

    @Test
    fun annotatesMarkdownAndBareLinks() {
        val text = buildMarkdownAnnotatedString(
            "访问 [官网](https://example.com) 或 https://example.org，使用 `code` 和 **粗体**",
            Color.Blue,
        )

        val links = text.getStringAnnotations("URL", 0, text.length).map { it.item }
        assertEquals(listOf("https://example.com", "https://example.org"), links)
        assertTrue(text.text.contains("官网"))
        assertTrue(text.text.contains("code"))
    }

    @Test
    fun parsesHeadingsDividerAndChineseBulletCharacters() {
        val blocks = parseMarkdownBlocks("## 核心结论\n---\n• 第一项\n2. 第二项")

        assertEquals(
            listOf(MarkdownBlockType.Heading, MarkdownBlockType.Divider, MarkdownBlockType.List, MarkdownBlockType.List),
            blocks.map { it.type },
        )
        assertEquals(2, blocks.first().level)
        assertEquals("•", blocks[2].marker)
        assertEquals("2.", blocks[3].marker)
    }

    @Test
    fun removesInlineEmphasisMarkers() {
        val text = buildMarkdownAnnotatedString("**粗体** *斜体* _也是斜体_ ~~删除~~", Color.Blue)

        assertEquals("粗体 斜体 也是斜体 删除", text.text)
    }

    @Test
    fun detectsStructuredMarkdownWithoutMisreadingNgaBbCode() {
        assertTrue(looksLikeMarkdown("## 标题\n\n> 引用\n\n- 第一项\n- 第二项\n**结论**"))
        assertTrue(looksLikeMarkdown("```kotlin\nval answer = 42\n```"))
        assertTrue(!looksLikeMarkdown("今天买了 # 新设备，价格是 80-90 元"))
        assertTrue(!looksLikeMarkdown("[quote][b]Reply Post by[/b]普通引用[/quote]"))
    }
}
