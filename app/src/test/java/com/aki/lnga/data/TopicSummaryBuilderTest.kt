package com.aki.lnga.data

import com.aki.lnga.model.Post
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicSummaryBuilderTest {
    @Test
    fun exportsTablesLinksAndMediaWithoutRawImageUrls() {
        val source = buildTopicSummarySource(
            title = "测试主题",
            pages = listOf(
                SummaryPage(
                    1,
                    listOf(
                        post(
                            0,
                            "[table][tr][td]名称[/td][td]数值[/td][/tr][/table]" +
                                "[url=https://example.com]资料[/url][img]https://img.nga.cn/a.jpg[/img]" +
                                "[flash]https://example.com/a.mp4[/flash]",
                        ),
                    ),
                ),
            ),
        )

        assertTrue(source.text.contains("[表格]"))
        assertTrue(source.text.contains("| 名称 | 数值 |"))
        assertTrue(source.text.contains("资料 (https://example.com)"))
        assertTrue(source.text.contains("[图片]"))
        assertFalse(source.text.contains("a.jpg"))
        assertEquals(1, source.imageCount)
        assertEquals(1, source.videoCount)
    }

    @Test
    fun duplicatePostsAreRemovedAndLongSourcesKeepBothEnds() {
        val posts = listOf(
            post(0, "开头的重要内容"),
            post(1, "重复内容"),
            post(2, "重复内容"),
            post(3, "中间内容".repeat(80)),
            post(4, "结尾的重要内容"),
        )

        val source = buildTopicSummarySource("长主题", listOf(SummaryPage(1, posts)), maxCharacters = 180)

        assertEquals(4, source.totalPostCount)
        assertTrue(source.truncated)
        assertTrue(source.text.contains("开头的重要内容"))
        assertTrue(source.text.contains("结尾的重要内容"))
    }

    private fun post(floor: Int, content: String) = Post(
        tid = 1,
        fid = 2,
        pid = floor.toLong() + 10,
        floor = floor,
        author = "用户$floor",
        authorId = floor.toLong(),
        subject = "",
        content = content,
        postDateSeconds = 0,
    )
}
