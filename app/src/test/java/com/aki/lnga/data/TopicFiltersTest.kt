package com.aki.lnga.data

import com.aki.lnga.model.Topic
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicFiltersTest {
    @Test
    fun detectsCrashReportTitleAndTradingBoards() {
        assertTrue(topic(subject = "iOS客户端闪退：启动失败").isAlwaysHidden())
        assertFalse(topic(subject = "iOS 客户端闪退").isAlwaysHidden())
        assertTrue(topic(boardName = "优惠信息").isTradingBoardTopic())
        assertTrue(topic(boardName = "优惠信息 购物指南").isTradingBoardTopic())
        assertTrue(topic(boardName = "二手交易").isTradingBoardTopic())
        assertFalse(topic(boardName = "消费电子").isTradingBoardTopic())
    }

    private fun topic(subject: String = "normal", boardName: String = "") = Topic(
        tid = 1,
        fid = 1,
        subject = subject,
        author = "author",
        authorId = 1,
        replies = 0,
        lastPoster = "",
        postDateSeconds = 0,
        boardName = boardName,
    )
}
