package com.aki.lnga.data

import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import org.junit.Assert.assertEquals
import org.junit.Test

class TopicFavoriteStoreTest {
    @Test
    fun newestFavoriteMovesToFrontWithoutDuplicates() {
        val old = entry(1, 1)
        val other = entry(2, 2)
        val updated = entry(1, 3)

        val result = addFavorite(listOf(old, other), updated)

        assertEquals(listOf(1L, 2L), result.map(TopicFavoriteEntry::tid))
        assertEquals(3L, result.first().savedAtMillis)
    }

    @Test
    fun favoriteEntrySupportsJsonRoundTrip() {
        val encoded = Json.encodeToString(listOf(entry(7, 9)))
        val decoded = Json.decodeFromString<List<TopicFavoriteEntry>>(encoded)

        assertEquals(7L, decoded.single().tid)
    }

    private fun entry(tid: Long, savedAt: Long) = TopicFavoriteEntry(
        tid = tid,
        fid = 1,
        subject = "topic $tid",
        author = "author",
        authorId = 1,
        boardName = "board",
        postDateSeconds = 0,
        savedAtMillis = savedAt,
    )
}
