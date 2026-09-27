package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test

class DouyinPreviewLoaderTest {
    @Test
    fun recognizesSupportedDouyinLinks() {
        assertTrue(isDouyinUrl("https://v.douyin.com/UMQCKeX7678"))
        assertTrue(isDouyinUrl("https://www.douyin.com/video/7678958085399370715"))
        assertTrue(isDouyinUrl("https://www.iesdouyin.com/share/video/7678958085399370715/"))
        assertFalse(isDouyinUrl("https://example.com/?next=v.douyin.com"))
    }

    @Test
    fun extractsCanonicalVideoId() {
        assertEquals(
            "7678958085399370715",
            extractDouyinVideoId("https://www.iesdouyin.com/share/video/7678958085399370715/"),
        )
    }

    @Test
    fun parsesMetadataAndHydratedFallbackFields() {
        val html = """
            <html><head>
              <meta property="og:title" content="一段测试视频" />
              <meta property="og:image" content="//example.com/cover.jpg" />
              <link rel="canonical" href="https://www.douyin.com/video/7678958085399370715" />
            </head><body><script>
              {"author":{"nickname":"测试作者","avatar_thumb":{"url_list":["https:\/\/example.com\/avatar.jpg"]}},
               "cover":{"url_list":["https:\/\/example.com\/fallback.jpg"]},"duration":12345}
            </script></body></html>
        """.trimIndent()

        val preview = parseDouyinPreviewHtml(html, "https://v.douyin.com/example")

        assertNotNull(preview)
        assertEquals("一段测试视频", preview?.title)
        assertEquals("测试作者", preview?.author)
        assertEquals("https://example.com/cover.jpg", preview?.coverUrl)
        assertEquals("https://example.com/avatar.jpg", preview?.avatarUrl)
        assertEquals(12, preview?.durationSeconds)
        assertEquals("7678958085399370715", preview?.videoId)
    }

    @Test
    fun keepsBasicCardWhenSharePageWithholdsMetadata() {
        val preview = parseDouyinPreviewHtml(
            "<title>在抖音记录美好生活20260828 - 抖音</title>",
            "https://v.douyin.com/example",
            "https://www.iesdouyin.com/share/video/7678958085399370715/",
        )

        assertNotNull(preview)
        assertEquals("", preview?.title)
        assertEquals("7678958085399370715", preview?.videoId)
    }
}
