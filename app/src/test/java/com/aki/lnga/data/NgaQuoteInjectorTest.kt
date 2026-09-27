package com.aki.lnga.data

import com.aki.lnga.model.Post
import org.junit.Assert.assertTrue
import org.junit.Test

class NgaQuoteInjectorTest {
    @Test
    fun fillsQuickReplyBodyFromReferencedPost() {
        val original = post(10, "被回复的完整内容")
        val reply = post(
            11,
            "[b]Reply to [pid=10,20,1]Reply[/pid] Post by [uid=1]原作者[/uid] (2026-08-17 12:00)[/b]<br/>我的回复",
        )

        val result = NgaQuoteInjector.inject(listOf(original, reply))[1].content

        assertTrue(result.contains("[quote]"))
        assertTrue(result.contains("被回复的完整内容"))
        assertTrue(result.contains("我的回复"))
    }

    @Test
    fun completesTruncatedQuoteBody() {
        val original = post(10, "完整内容还有后半段")
        val reply = post(
            11,
            "[quote][pid=10,20,1]Reply[/pid] [b]Post by [uid=1]原作者[/uid] (time):[/b]完整内容[/quote]回复",
        )

        assertTrue(NgaQuoteInjector.inject(listOf(original, reply))[1].content.contains("完整内容还有后半段"))
    }

    @Test
    fun usesPostsCachedFromAnotherPage() {
        val original = post(10, "上一页的原文")
        val reply = post(
            11,
            "[b]Reply to [pid=10,20,1]Reply[/pid] Post by [uid=1]原作者[/uid] (time)[/b]回复",
        )

        assertTrue(NgaQuoteInjector.inject(listOf(reply), listOf(original)).single().content.contains("上一页的原文"))
    }

    private fun post(pid: Long, content: String) = Post(
        tid = 20,
        fid = 1,
        pid = pid,
        floor = 0,
        author = "author",
        authorId = 1,
        subject = "",
        content = content,
        postDateSeconds = 0,
    )
}
