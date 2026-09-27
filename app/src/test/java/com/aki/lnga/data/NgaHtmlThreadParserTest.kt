package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Test

class NgaHtmlThreadParserTest {
    @Test
    fun restoresPostsAuthorsAndPaginationFromHtml() {
        val html = """
            <script>
            __CURRENT_TID = 47387156;
            __CURRENT_FID = -7;
            __CURRENT_PAGE = 3;
            __CURRENT_PAGE_POSTS = 20;
            commonui.userInfo.setAll({"123":{"username":"测试用户"}});
            commonui.postArg.setDefault(-7,0,47387156,"",42,1785038476,20);
            </script>
            <h1 id='currentTopicName'>测试主题</h1>
            <h2 id='currentForumName'>测试版块</h2>
            <h3 id='postsubject0'>主楼标题</h3>
            <p id='postcontent0'>主楼&amp;正文</p>
            <script>
            commonui.postArg.proc(0,null,null,null,null,null,null,null,null,null,0,0,null,'123',1785038000,'0,0,0','8','','','',null);
            </script>
            <h3 id='postsubject40'>回复标题</h3>
            <span id='postcontent40'>文字&amp;内容&emsp;第二行</span>
            <script>
            commonui.postArg.proc(40,null,null,null,null,null,null,null,null,null,878013195,0,null,'123',1785038476,'2,12','12','','','',null);
            </script>
        """.trimIndent()

        val page = NgaHtmlThreadParser.parse(html, requestedTid = 1, requestedPage = 1)

        assertEquals("测试主题", page.title)
        assertEquals("测试版块", page.forumName)
        assertEquals(3, page.pagination.page)
        assertEquals(3, page.pagination.totalPages)
        assertEquals(43, page.pagination.totalRows)
        assertEquals(2, page.posts.size)
        assertEquals(0, page.posts.first().floor)
        assertEquals(0, page.posts.first().pid)
        assertEquals("主楼&正文", page.posts.first().content)
        assertEquals(40, page.posts.last().floor)
        assertEquals(878013195, page.posts.last().pid)
        assertEquals("测试用户", page.posts.last().author)
        assertEquals("文字&内容\u2003第二行", page.posts.last().content)
        assertEquals(12, page.posts.last().score)
        assertEquals(2, page.posts.last().scoreDown)
    }

    @Test
    fun topLevelArgumentScannerPreservesNestedCommas() {
        assertEquals(
            listOf("1", "'a,b'", "[2,3]", "fn(4,5)"),
            NgaHtmlThreadParser.splitTopLevelArguments("1,'a,b',[2,3],fn(4,5)"),
        )
    }
}
