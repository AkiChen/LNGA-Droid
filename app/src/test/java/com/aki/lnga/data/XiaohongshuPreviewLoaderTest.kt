package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class XiaohongshuPreviewLoaderTest {
    @Test
    fun recognizesShortAndDirectLinksOnly() {
        assertTrue(isXiaohongshuUrl("https://xhslink.cn/o/example"))
        assertTrue(isXiaohongshuUrl("https://www.xiaohongshu.com/discovery/item/123"))
        assertFalse(isXiaohongshuUrl("https://example.com/?next=xhslink.cn"))
    }

    @Test
    fun parsesPublicSsrNoteAndUpgradesMediaUrls() {
        val html = """
            <script>window.__INITIAL_STATE__={"noteData":{"data":{"noteData":{
              "noteId":"abc123","type":"video","title":"测试标题","desc":"摘要",
              "user":{"nickName":"作者","avatar":"http:\/\/avatar.example\/a.jpg"},
              "imageList":[{"url":"http:\/\/image.example\/cover.jpg"}]
            }}}}</script>
        """.trimIndent()

        val preview = parseXiaohongshuPreviewHtml(html, "https://xhslink.cn/o/example")!!

        assertEquals("abc123", preview.noteId)
        assertEquals("video", preview.type)
        assertEquals("测试标题", preview.title)
        assertEquals("作者", preview.author)
        assertEquals("https://avatar.example/a.jpg", preview.avatarUrl)
        assertEquals("https://image.example/cover.jpg", preview.coverUrl)
        assertEquals("https://xhslink.cn/o/example", preview.url)
    }
}
