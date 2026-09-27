package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class AttachmentDownloaderTest {
    @Test
    fun rejectsHtmlErrorPages() {
        val result = validateAttachmentPrefix("archive.rar", "text/html", "<html>login</html>".toByteArray())

        assertTrue(result.orEmpty().contains("错误页面"))
    }

    @Test
    fun validatesRarMagicBeforeDownload() {
        assertNull(validateAttachmentPrefix("archive.rar", "application/octet-stream", byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1a, 0x07, 0x01)))
        assertTrue(validateAttachmentPrefix("archive.rar", null, "not-rar".toByteArray()).orEmpty().contains("RAR"))
    }

    @Test
    fun rejectsCdnPlaceholderAndInvalidZip() {
        assertTrue(
            validateAttachmentPrefix("archive.rar", "image/gif", "GIF89a".toByteArray())
                .orEmpty().contains("占位"),
        )
        assertTrue(validateAttachmentPrefix("archive.zip", null, "not-zip".toByteArray()).orEmpty().contains("ZIP"))
    }

    @Test
    fun sanitizesFileNameAndExplainsTemporaryServerFailures() {
        assertEquals("bad_name_.rar", attachmentFileName("bad/name?.rar", "/fallback.rar"))
        assertTrue(attachmentHttpMessage(503).contains("暂时不可用"))
    }
}
