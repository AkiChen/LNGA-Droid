package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class WeiboPreviewLoaderTest {
    @Test
    fun recognizesSupportedWeiboLinks() {
        assertTrue(isWeiboUrl("https://weibo.com/1433680664/5335242921215104"))
        assertTrue(isWeiboUrl("https://m.weibo.cn/detail/5335242921215104"))
        assertTrue(isWeiboUrl("https://t.cn/A123456"))
        assertFalse(isWeiboUrl("https://example.com/weibo.com"))
        assertEquals("5335242921215104", extractWeiboStatusId("https://weibo.com/1433680664/5335242921215104"))
    }

    @Test
    fun parsesPublicStatusJson() {
        val preview = parseWeiboStatusJson(
            """{"id":5335242921215104,"text":"测试&lt;br&gt;正文","user":{"screen_name":"作者","avatar_hd":"https://img/avatar.jpg"},"pics":[{"large":{"url":"https://img/large.jpg"}}]}""",
            "https://weibo.com/1433680664/5335242921215104",
        )

        assertEquals("作者", preview?.author)
        assertEquals("测试\n正文", preview?.text)
        assertEquals("https://img/large.jpg", preview?.coverUrl)
    }

    @Test
    fun parsesOpenGraphFallback() {
        val preview = parseWeiboPreviewHtml(
            """<meta property="og:title" content="示例作者的微博"><meta property="og:description" content="一条公开微博"><meta property="og:image" content="//img.example/cover.jpg">""",
            "https://weibo.com/1/ABC123",
        )

        assertEquals("示例作者", preview?.author)
        assertEquals("一条公开微博", preview?.text)
        assertEquals("https://img.example/cover.jpg", preview?.coverUrl)
    }
}
