package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Test

class ImageDiagnosticStoreTest {
    @Test
    fun diagnosticUrlDropsQueryAndFragment() {
        val result = sanitizedImageUrl("https://img.example/a/b.jpg?token=secret#part")

        assertEquals("https://img.example/a/b.jpg", result)
        assertFalse(result.contains("secret"))
    }

    @Test
    fun diagnosticTextRedactsUrlsAndCredentials() {
        val result = redactDiagnosticText("failed https://img/a.jpg?token=secret cookie=abc cid=123")

        assertFalse(result.contains("secret"))
        assertFalse(result.contains("abc"))
        assertFalse(result.contains("123"))
    }
}
