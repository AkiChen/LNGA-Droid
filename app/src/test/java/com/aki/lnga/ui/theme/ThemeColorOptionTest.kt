package com.aki.lnga.ui.theme

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeColorOptionTest {
    @Test
    fun `unknown stored color falls back to purple`() {
        assertEquals(ThemeColorOption.Purple, ThemeColorOption.fromId("removed-color"))
        assertEquals(ThemeColorOption.Blue, ThemeColorOption.fromId("blue"))
    }

    @Test
    fun `unknown appearance falls back to system`() {
        assertEquals(ThemeAppearanceOption.System, ThemeAppearanceOption.fromId("removed-theme"))
        assertEquals(ThemeAppearanceOption.UltraDark, ThemeAppearanceOption.fromId("ultra_dark"))
    }

    @Test
    fun `automatic ultra night window starts at 23 and ends at 7`() {
        assertEquals(false, isWithinAutomaticNightWindow(22))
        assertEquals(true, isWithinAutomaticNightWindow(23))
        assertEquals(true, isWithinAutomaticNightWindow(0))
        assertEquals(true, isWithinAutomaticNightWindow(6))
        assertEquals(false, isWithinAutomaticNightWindow(7))
    }
}
