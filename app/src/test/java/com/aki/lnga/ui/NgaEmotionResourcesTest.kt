package com.aki.lnga.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class NgaEmotionResourcesTest {
    @Test
    fun resolvesNamedAndNumericEmotionCodes() {
        val named = NgaEmotionResources.resolve("ac", "哭笑")

        assertNotNull(named)
        assertEquals(named, NgaEmotionResources.resolve("ac", "15"))
    }

    @Test
    fun normalizesLegacyPstCategoryAndLeadingZeroes() {
        assertEquals(
            NgaEmotionResources.resolve("pst", "0"),
            NgaEmotionResources.resolve("pt", "00"),
        )
    }

    @Test
    fun preservesUnknownEmotionAsFallback() {
        assertNull(NgaEmotionResources.resolve("unknown", "999"))
    }
}
