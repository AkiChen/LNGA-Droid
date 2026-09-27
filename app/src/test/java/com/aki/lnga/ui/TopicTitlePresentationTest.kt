package com.aki.lnga.ui

import org.junit.Assert.assertEquals
import org.junit.Test

class TopicTitlePresentationTest {
    @Test
    fun textPrefixIsUsedWhenIconModeIsDisabled() {
        assertEquals("[PC软硬件] ", topicBoardTextPrefix("PC软硬件", useBoardIcon = false))
    }

    @Test
    fun textPrefixIsOmittedForIconModeOrBlankBoard() {
        assertEquals("", topicBoardTextPrefix("PC软硬件", useBoardIcon = true))
        assertEquals("", topicBoardTextPrefix("", useBoardIcon = false))
    }
}
