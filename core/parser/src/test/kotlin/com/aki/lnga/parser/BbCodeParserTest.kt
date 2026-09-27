package com.aki.lnga.parser

import com.aki.lnga.model.BbNode
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertIs

class BbCodeParserTest {
    private val parser = BbCodeParser()

    @Test
    fun parsesNestedInlineFormatting() {
        val nodes = parser.parse("before [b]bold [i]and italic[/i][/b] after")

        assertEquals(3, nodes.size)
        val bold = assertIs<BbNode.Bold>(nodes[1])
        assertIs<BbNode.Italic>(bold.children[1])
    }

    @Test
    fun parsesMediaLinkQuoteAndEmotion() {
        val nodes = parser.parse("[quote]hello[/quote][img]https://img.nga.cn/a.png[/img][url=https://nga.cn]NGA[/url][s:ac:goodjob]")

        assertIs<BbNode.Quote>(nodes[0])
        assertIs<BbNode.Image>(nodes[1])
        assertEquals("https://nga.cn", assertIs<BbNode.Link>(nodes[2]).href)
        assertEquals("goodjob", assertIs<BbNode.Emotion>(nodes[3]).code)
    }

    @Test
    fun parsesPlainBilibiliLinksAndPreservesTrailingPunctuation() {
        val nodes = parser.parse("看看 https://b23.tv/AbCd12，后续")

        assertEquals("看看 ", assertIs<BbNode.Text>(nodes[0]).value)
        assertEquals("https://b23.tv/AbCd12", assertIs<BbNode.Link>(nodes[1]).href)
        assertEquals("，后续", assertIs<BbNode.Text>(nodes[2]).value)
    }

    @Test
    fun parsesFlashVideoAndNumericEmojiEntities() {
        val nodes = parser.parse(
            "[flash]//example.com/video.mp4[/flash]&#128514;&#x1F602;&amp;#128514;",
        )

        assertEquals("https://example.com/video.mp4", assertIs<BbNode.Video>(nodes[0]).url)
        assertEquals("😂😂😂", assertIs<BbNode.Text>(nodes[1]).value)
    }

    @Test
    fun preservesUnknownTags() {
        assertEquals(listOf(BbNode.Text("[unknown]value[/unknown]")), parser.parse("[unknown]value[/unknown]"))
    }

    @Test
    fun normalizesNgaLineBreaksAndRelativeImages() {
        val nodes = parser.parse("第一行<br/><br />第二行[img]./mon_202608/a.jpg[/img]")

        assertEquals("第一行\n\n第二行", assertIs<BbNode.Text>(nodes[0]).value)
        assertEquals(
            "https://img.nga.cn/attachments/mon_202608/a.jpg",
            assertIs<BbNode.Image>(nodes[1]).url,
        )
    }

    @Test
    fun parsesNgaUserPostAndThreadLinks() {
        val nodes = parser.parse(
            "[pid=878687296,47387156,1]Reply[/pid] by [uid=38672400]用户[/uid] " +
                "[tid=47387156]主题[/tid]",
        )

        assertEquals("https://bbs.nga.cn/read.php?pid=878687296", assertIs<BbNode.Link>(nodes[0]).href)
        assertEquals("https://bbs.nga.cn/nuke.php?func=ucp&uid=38672400", assertIs<BbNode.Link>(nodes[2]).href)
        assertEquals("https://bbs.nga.cn/read.php?tid=47387156", assertIs<BbNode.Link>(nodes[4]).href)
    }

    @Test
    fun parsesCommonStylesCodeAndList() {
        val nodes = parser.parse(
            "[u]下划线[/u][del]删除[/del][color=#ff0000]红色[/color]" +
                "[code]a < b[/code][list][*]一[*][b]二[/b][/list]",
        )

        assertIs<BbNode.Underline>(nodes[0])
        assertIs<BbNode.Strikethrough>(nodes[1])
        assertEquals("#ff0000", assertIs<BbNode.Color>(nodes[2]).value)
        assertEquals("a < b", assertIs<BbNode.Code>(nodes[3]).value)
        assertEquals(2, assertIs<BbNode.BulletList>(nodes[4]).items.size)
    }

    @Test
    fun decodesEmSpacesAndParsesNgaTables() {
        val nodes = parser.parse(
            "地区&amp;emsp;&emsp;年份" +
                "[table][tr][td rowspan=2]地区[/td][td]寿命[/td][/tr]" +
                "[tr][td]香港[/td][td][b]85.5[/b][/td][/tr][/table]",
        )

        assertEquals("地区\u2003\u2003年份", assertIs<BbNode.Text>(nodes[0]).value)
        val table = assertIs<BbNode.Table>(nodes[1])
        assertEquals(2, table.rows.size)
        assertEquals(2, table.rows.first().size)
        assertEquals("香港", assertIs<BbNode.Text>(table.rows[1][0].single()).value)
        assertIs<BbNode.Bold>(table.rows[1][1].single())
    }

    @Test
    fun parsesCommonNgaBlockTagsAndAttachments() {
        val nodes = parser.parse(
            "[collapse=详细内容][h]标题[/h][p]正文[/p][/collapse]" +
                "[lessernuke]警告[/lessernuke][album=图片集][img]./a.jpg[/img][/album]" +
                "[align=center]居中[/align][hr]H[sub]2[/sub]O x[sup]2[/sup]" +
                "[attach]./mon_202608/a.zip[/attach][hip][b]提示[/b][/hip]",
        )

        val collapse = assertIs<BbNode.Collapse>(nodes[0])
        assertEquals("详细内容", collapse.title)
        assertIs<BbNode.Heading>(collapse.children[0])
        assertIs<BbNode.Paragraph>(collapse.children[1])
        assertIs<BbNode.Warning>(nodes[1])
        assertEquals("图片集", assertIs<BbNode.Album>(nodes[2]).title)
        assertIs<BbNode.Align>(nodes[3])
        assertIs<BbNode.HorizontalRule>(nodes[4])
        assertEquals(
            "https://img.nga.cn/attachments/mon_202608/a.zip",
            nodes.filterIsInstance<BbNode.Attachment>().single().url,
        )
        assertEquals("a.zip", nodes.filterIsInstance<BbNode.Attachment>().single().name)
        assertIs<BbNode.Bold>(nodes.last())
    }
}
