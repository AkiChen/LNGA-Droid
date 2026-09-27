package com.aki.lnga.ui

import com.aki.lnga.model.BbNode
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentPresentationTest {
    @Test
    fun collectsInlineAttachmentsAndMediaToAvoidDuplicateCards() {
        val urls = collectPostAttachmentUrls(
            listOf(
                BbNode.Attachment("https://img.nga.cn/attachments/a.rar", "a.rar"),
                BbNode.Image("http://img.nga.cn/attachments/a.jpg"),
                BbNode.Quote(listOf(BbNode.Video("https://img.nga.cn/attachments/a.mp4"))),
            ),
        )

        assertEquals(3, urls.size)
        assertTrue("https://img.nga.cn/attachments/a.rar" in urls)
    }

    @Test
    fun attachmentIdentityIgnoresProtocolAndFragment() {
        assertEquals(
            attachmentUrlKey("http://IMG.NGA.CN/attachments/a.rar#download"),
            attachmentUrlKey("https://img.nga.cn/attachments/a.rar"),
        )
    }
}
