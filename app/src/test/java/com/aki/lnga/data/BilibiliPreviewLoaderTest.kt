package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Test

class BilibiliPreviewLoaderTest {
    @Test
    fun parsesMetadataRegardlessOfAttributeOrder() {
        val html = """
            <html><head>
              <meta content="一段&amp;视频_哔哩哔哩" property="og:title">
              <meta property="og:image" content="//i0.hdslb.com/cover.jpg">
              <meta name="author" content="测试UP主">
              <meta property="og:url" content="https://www.bilibili.com/video/BV123">
              <meta property="og:video:duration" content="125">
            </head></html>
        """.trimIndent()

        val preview = requireNotNull(parseBilibiliPreviewHtml(html, "https://b23.tv/abc"))
        assertEquals("一段&视频", preview.title)
        assertEquals("https://i0.hdslb.com/cover.jpg", preview.coverUrl)
        assertEquals("测试UP主", preview.uploader)
        assertEquals(125, preview.durationSeconds)
    }
}
