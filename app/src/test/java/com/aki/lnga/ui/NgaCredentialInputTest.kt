package com.aki.lnga.ui

import com.aki.lnga.model.NgaSession
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class NgaCredentialInputTest {
    @Test
    fun acceptsRawCredentialValues() {
        assertEquals(NgaSession("12345", "secret-token"), normalizeNgaSessionInput(" 12345 ", " secret-token "))
    }

    @Test
    fun acceptsFullCookieInEitherField() {
        val cookie = "foo=bar; ngaPassportUid=12345; ngaPassportCid='secret-token'; theme=dark"
        assertEquals(NgaSession("12345", "secret-token"), normalizeNgaSessionInput(cookie, ""))
    }

    @Test
    fun stripsCookieNamesAndRejectsInvalidUid() {
        assertEquals(
            NgaSession("12345", "secret-token"),
            normalizeNgaSessionInput("ngaPassportUid=12345;", "ngaPassportCid=secret-token;"),
        )
        assertNull(normalizeNgaSessionInput("not-a-number", "secret-token"))
    }
}
