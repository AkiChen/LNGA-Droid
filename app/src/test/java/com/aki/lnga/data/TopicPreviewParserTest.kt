package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Test

class TopicPreviewParserTest {
    @Test
    fun `parses and normalizes first-post image previews`() {
        val topic = NgaJson.parse(
            """{"__P":{"content":"before [img]./mon_202608/17/a.jpg[/img] <img src='//img.nga.178.com/attachments/mon_202608/17/b.webp'>","attachs":{"0":{"attachurl":"mon_202608/17/c.png"}}}}""",
        )

        assertEquals(
            listOf(
                "https://img.nga.cn/attachments/mon_202608/17/a.jpg",
                "https://img.nga.cn/attachments/mon_202608/17/b.webp",
                "https://img.nga.cn/attachments/mon_202608/17/c.png",
            ),
            TopicPreviewParser.parse(topic),
        )
    }

    @Test
    fun `ignores non-image attachments and caps previews at three`() {
        val topic = NgaJson.parse(
            """{"__P":{"content":"[img]https://img.nga.cn/attachments/a.jpg[/img][img]https://img.nga.cn/attachments/b.png[/img][img]https://img.nga.cn/attachments/c.gif[/img][img]https://img.nga.cn/attachments/d.webp[/img]","attachs":{"0":{"attachurl":"mon_202608/17/movie.mp4"}}}}""",
        )

        assertEquals(3, TopicPreviewParser.parse(topic).size)
    }

    @Test
    fun `numeric thumb flag does not hide attachment url`() {
        val topic = NgaJson.parse(
            """{"attachs":{"0":{"thumb":1,"attachurl":"mon_202608/17/photo.jpg"}}}""",
        )

        assertEquals(
            listOf("https://img.nga.cn/attachments/mon_202608/17/photo.jpg"),
            TopicPreviewParser.parse(topic),
        )
    }
}
