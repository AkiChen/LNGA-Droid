package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ReadingProgressStoreTest {
    @Test
    fun progressIsIsolatedByUidAndNewestValueReplacesSameTopic() {
        val original = listOf(
            ReadingProgressEntry("100", 7, page = 1, floor = 3, updatedAtMillis = 1),
            ReadingProgressEntry("200", 7, page = 2, floor = 25, updatedAtMillis = 2),
        )

        val result = mergeReadingProgress(
            original,
            ReadingProgressEntry("100", 7, page = 4, floor = 77, updatedAtMillis = 3),
        )

        assertEquals(2, result.size)
        assertEquals(4, result.first { it.uid == "100" }.page)
        assertEquals(77, result.first { it.uid == "100" }.floor)
        assertEquals(25, result.first { it.uid == "200" }.floor)
    }

    @Test
    fun boundedProgressKeepsNewestEntries() {
        val result = (1L..5L).fold(emptyList<ReadingProgressEntry>()) { values, tid ->
            mergeReadingProgress(values, ReadingProgressEntry("100", tid, updatedAtMillis = tid), limit = 3)
        }

        assertEquals(listOf(5L, 4L, 3L), result.map(ReadingProgressEntry::tid))
    }
}
