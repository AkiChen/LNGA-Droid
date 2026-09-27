package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.time.LocalDateTime
import java.time.ZoneOffset

class UserActivityTest {
    @Test
    fun aggregatesForumsAndUsesChinaStandardTime() {
        val morning = LocalDateTime.of(2026, 8, 27, 9, 0).toEpochSecond(ZoneOffset.ofHours(8))
        val night = LocalDateTime.of(2026, 8, 27, 22, 0).toEpochSecond(ZoneOffset.ofHours(8))

        val analysis = aggregateUserActivity(
            topics = listOf(UserActivityRecord(1, "主题", "硬件", morning)),
            replies = listOf(
                UserActivityRecord(2, "回复一", "硬件", night, "内容"),
                UserActivityRecord(3, "回复二", "生活", night, "内容"),
            ),
        )

        assertEquals(UserForumStat("硬件", 1, 1), analysis.forumStats.first())
        assertEquals(1, analysis.hourDistribution[9])
        assertEquals(2, analysis.hourDistribution[22])
    }

    @Test
    fun sanitizesQuotedMediaAndPrivateUrlsFromAiSample() {
        val value = sanitizeUserReply("[quote]旧内容[/quote]正文[img]https://secret/x.jpg[/img] [flash]v[/flash] https://example.com")

        assertTrue(value.contains("正文"))
        assertTrue(value.contains("[图片]"))
        assertTrue(value.contains("[视频]"))
        assertFalse(value.contains("旧内容"))
        assertFalse(value.contains("https://"))
    }
}
