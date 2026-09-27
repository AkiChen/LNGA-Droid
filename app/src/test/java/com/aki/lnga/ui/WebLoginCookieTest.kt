package com.aki.lnga.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class WebLoginCookieTest {
    @Test
    fun extractsUidAndCidFromCookieHeader() {
        assertEquals(
            "12345" to "secret-token",
            parseNgaCookies("foo=bar; ngaPassportUid=12345; ngaPassportCid=secret-token; theme=dark"),
        )
    }

    @Test
    fun rejectsIncompleteCookieHeader() {
        assertNull(parseNgaCookies("ngaPassportUid=12345; theme=dark"))
    }

    @Test
    fun recognizesOnlyNgaAccountIframeUrls() {
        assertEquals(
            true,
            isNgaLoginFrameUrl("https://bbs.nga.cn/nuke/account_copy.html?login"),
        )
        assertEquals(false, isNgaLoginFrameUrl("https://example.com/nuke/account_copy.html"))
    }

    @Test
    fun diagnosticUrlRemovesQueryAndFragment() {
        assertEquals(
            "https://bbs.nga.cn/nuke.php",
            safeWebUrl("https://bbs.nga.cn/nuke.php?ngaPassportCid=secret#section"),
        )
        assertEquals("none", safeWebUrl(null))
    }
}
