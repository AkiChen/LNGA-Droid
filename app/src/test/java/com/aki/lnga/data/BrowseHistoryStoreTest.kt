package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class BrowseHistoryStoreTest {
    @Test
    fun newestViewMovesToFrontWithoutDuplicates() {
        val old = entry(1, 1)
        val other = entry(2, 2)
        val revisited = entry(1, 3)

        val result = mergeHistory(listOf(old, other), revisited)

        assertEquals(listOf(1L, 2L), result.map(BrowseHistoryEntry::tid))
        assertEquals(3L, result.first().visitedAtMillis)
    }

    @Test
    fun historyEntrySupportsJsonRoundTrip() {
        val encoded = Json.encodeToString(listOf(entry(7, 9)))
        val decoded = Json.decodeFromString<List<BrowseHistoryEntry>>(encoded)

        assertEquals(7L, decoded.single().tid)
    }

    private fun entry(tid: Long, visited: Long) = BrowseHistoryEntry(
        tid = tid,
        fid = 1,
        subject = "topic $tid",
        author = "author",
        authorId = 1,
        boardName = "board",
        postDateSeconds = 0,
        visitedAtMillis = visited,
    )
}
