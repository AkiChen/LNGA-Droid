package com.aki.lnga.data

import com.aki.lnga.model.Pagination
import com.aki.lnga.model.Topic
import com.aki.lnga.model.TopicPage
import org.junit.Assert.assertEquals
import org.junit.Test

class TopicListModeTest {
    @Test
    fun hotListUsesServerTimeFiltersDuplicatesAndSortsByReplies() {
        val now = 1_000_000L
        val firstPage = page(
            serverTime = now,
            topic(tid = 1, replies = 10, postDate = now - 60),
            topic(tid = 2, replies = 90, postDate = now - TopicHotRange.Day.windowSeconds - 1),
            topic(tid = 3, replies = 30, postDate = now - 120),
        )
        val secondPage = page(
            serverTime = now,
            topic(tid = 1, replies = 999, postDate = now - 30),
            topic(tid = 4, replies = 20, postDate = now - 180),
        )

        val result = buildHotTopicList(
            pages = listOf(firstPage, secondPage),
            range = TopicHotRange.Day,
            fallbackNowSeconds = 0,
        )

        assertEquals(listOf(3L, 4L, 1L), result.map(Topic::tid))
    }

    @Test
    fun hotRangesMatchOriginalClientCollectionDepth() {
        assertEquals(5, TopicHotRange.Day.fetchPages)
        assertEquals(8, TopicHotRange.Week.fetchPages)
        assertEquals(10, TopicHotRange.Month.fetchPages)
        assertEquals(86_400L, TopicHotRange.Day.windowSeconds)
        assertEquals(604_800L, TopicHotRange.Week.windowSeconds)
        assertEquals(2_592_000L, TopicHotRange.Month.windowSeconds)
    }

    private fun page(serverTime: Long, vararg topics: Topic) = TopicPage(
        forumName = "Forum",
        topics = topics.toList(),
        pagination = Pagination(page = 1, totalPages = 1, totalRows = topics.size),
        serverTimeSeconds = serverTime,
    )

    private fun topic(tid: Long, replies: Int, postDate: Long) = Topic(
        tid = tid,
        fid = -7,
        subject = "Topic $tid",
        author = "Author",
        authorId = tid,
        replies = replies,
        lastPoster = "Poster",
        postDateSeconds = postDate,
        lastPostDateSeconds = postDate,
    )
}
