package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NgaAttachmentParserTest {
    @Test
    fun parsesObjectShapeAndDecodesOriginalFileName() {
        val root = NgaJson.parse(
            """{"attachs":{"0":{"attachurl":"./mon_202609/01/axyfQ49-4pefZx.rar","url_utf8_org_name":"%E6%B5%8B%E8%AF%95%E9%99%84%E4%BB%B6.rar","size":"2048"}}}""",
        )

        val attachment = NgaAttachmentParser.parse(root["attachs"]).single()

        assertEquals("https://img.nga.cn/attachments/mon_202609/01/axyfQ49-4pefZx.rar", attachment.url)
        assertEquals("测试附件.rar", attachment.name)
        assertEquals("2.0 MB", attachment.sizeLabel)
    }

    @Test
    fun parsesArrayShapeAndRemovesDuplicateUrls() {
        val root = NgaJson.parse(
            """{"attachs":[{"url":"//img.nga.cn/attachments/a.zip","name":"a.zip"},{"url":"//img.nga.cn/attachments/a.zip","name":"duplicate.zip"}]}""",
        )

        val attachments = NgaAttachmentParser.parse(root["attachs"])

        assertEquals(1, attachments.size)
        assertEquals("https://img.nga.cn/attachments/a.zip", attachments.single().url)
    }
}
