package com.aki.lnga.ui

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardIconCatalogTest {
    @Test
    fun automaticallyMatchesCommonNgaSubBoards() {
        assertEquals(BoardIconPreset.Computer, automaticBoardIconPreset("PC软硬件"))
        assertEquals(BoardIconPreset.Forum, automaticBoardIconPreset("IT杂谈"))
        assertEquals(BoardIconPreset.Movie, automaticBoardIconPreset("影音讨论区"))
        assertEquals(BoardIconPreset.Soccer, automaticBoardIconPreset("生命之杯"))
        assertEquals(BoardIconPreset.History, automaticBoardIconPreset("历史研究"))
        assertEquals(BoardIconPreset.Offer, automaticBoardIconPreset("优惠信息 购物指南"))
        assertEquals(BoardIconPreset.Sell, automaticBoardIconPreset("二手交易"))
    }

    @Test
    fun unknownBoardUsesLabelAndPresetRoundTrips() {
        assertEquals(BoardIconPreset.Label, automaticBoardIconPreset("尚未收录的新版面"))
        BoardIconPreset.entries.forEach { preset ->
            assertEquals(preset, storedBoardIconPreset(preset.storedValue()))
        }
        assertNull(storedBoardIconPreset(null))
        assertNull(storedBoardIconPreset(BOARD_ICON_OFFICIAL))
        assertNull(storedBoardIconPreset("preset:missing"))
    }

    @Test
    fun catalogContainsAllSixGroupsAndFortyEightChoices() {
        assertEquals(6, BoardIconCategory.entries.size)
        assertEquals(48, BoardIconPreset.entries.size)
        BoardIconCategory.entries.forEach { category ->
            assertEquals(8, BoardIconPreset.entries.count { it.category == category })
        }
    }
}
