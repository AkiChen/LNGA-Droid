package com.aki.lnga.data

import com.aki.lnga.model.ForumBoard
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BoardPreferenceStoreTest {
    @Test
    fun movesFavoriteWithinBounds() {
        assertEquals(listOf("b", "a", "c"), moveItem(listOf("a", "b", "c"), "a", 1))
        assertEquals(listOf("a", "b", "c"), moveItem(listOf("a", "b", "c"), "a", -1))
        assertEquals(listOf("a", "c", "b"), moveItem(listOf("a", "b", "c"), "b", 99))
    }

    @Test
    fun favoriteBoardSnapshotsRoundTripSearchOnlyBoards() {
        val source = listOf(
            ForumBoard(
                fid = -152678,
                stid = 0,
                name = "[股市]技术分析",
                info = "未列入首页分类",
                iconUrl = "https://img4.nga.cn/board.png",
            ),
        )

        assertEquals(source, decodeFavoriteBoards(encodeFavoriteBoards(source)))
    }

    @Test
    fun createsNavigableFallbackForLegacyFavoriteKeys() {
        assertEquals("版面 -152678", favoriteBoardFallback("fid:-152678")?.name)
        assertEquals(123, favoriteBoardFallback("stid:123")?.stid)
        assertNull(favoriteBoardFallback("broken"))
    }

    @Test
    fun ignoresCorruptFavoriteSnapshotJson() {
        assertEquals(emptyList<ForumBoard>(), decodeFavoriteBoards("not-json"))
    }
}
