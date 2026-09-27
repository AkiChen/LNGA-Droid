package com.aki.lnga.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotEquals
import org.junit.Test

class AvatarPaletteTest {
    @Test
    fun colorIndexIsStableAndUsesIdentity() {
        assertEquals(avatarColorIndex(123, "name"), avatarColorIndex(123, "changed"))
        assertNotEquals(avatarColorIndex(123, "name"), avatarColorIndex(124, "name"))
        assertEquals(avatarColorIndex(0, "name"), avatarColorIndex(0, "name"))
    }
}
