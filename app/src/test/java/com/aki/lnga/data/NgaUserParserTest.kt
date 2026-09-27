package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NgaUserParserTest {
    @Test
    fun decodesAnonymousUserName() {
        assertEquals("甲王王甲王王", decodeAnonymousName("#anony_000000000000"))
    }

    @Test
    fun extractsEmbeddedAndRelativeAvatarUrls() {
        assertEquals(
            "https://example.com/avatar.jpg",
            normalizeAvatarUrl("{\"0\":\"https://example.com/avatar.jpg\"}"),
        )
        assertEquals(
            "https://img4.nga.cn/ngabbs/nga_classic/avatar/user.png",
            normalizeAvatarUrl("user.png"),
        )
    }
}
