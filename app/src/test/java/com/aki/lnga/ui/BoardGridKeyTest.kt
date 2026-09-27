package com.aki.lnga.ui

import com.aki.lnga.model.ForumBoard
import org.junit.Assert.assertEquals
import org.junit.Test

class BoardGridKeyTest {
    @Test
    fun gridKeysStayUniqueWithBlankOrRepeatedServerIdsAndBoards() {
        val board = ForumBoard(fid = 1, stid = 1, name = "重复版面")
        val keys = listOf(
            boardCategoryGridKey(categoryIndex = 0, categoryId = ""),
            boardCategoryGridKey(categoryIndex = 1, categoryId = ""),
            boardGroupGridKey(categoryIndex = 0, groupIndex = 0, categoryId = ""),
            boardGroupGridKey(categoryIndex = 1, groupIndex = 0, categoryId = ""),
            boardItemGridKey(categoryIndex = 0, groupIndex = 0, boardIndex = 0, board = board),
            boardItemGridKey(categoryIndex = 0, groupIndex = 0, boardIndex = 1, board = board),
        )

        assertEquals(keys.size, keys.distinct().size)
    }
}
