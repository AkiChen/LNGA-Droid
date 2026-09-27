package com.aki.lnga.data

import com.aki.lnga.model.NgaSession
import kotlinx.coroutines.runBlocking
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class NgaRepositoryFallbackTest {
    @Test
    fun loadsForumCategoryTreeFromHomeApi() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"result":{"1":{"_id":"1","name":"General","groups":[{"name":"Popular","forums":[{"fid":-7,"id":0,"name":"Forum","info":""}]}]}}}""",
            ))
            server.start()
            val repository = NgaRepository(testClient(server))

            val categories = repository.loadForumCategories(NgaSession("123", "secret"))

            assertEquals(-7, categories.single().groups.single().boards.single().fid)
            val request = server.takeRequest()
            assertEquals("/app_api.php", request.requestUrl?.encodedPath)
            assertEquals("home", request.requestUrl?.queryParameter("__lib"))
            assertEquals("category", request.requestUrl?.queryParameter("__act"))
        }
    }

    @Test
    fun selectedSpecialBoardUsesStidQuery() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"__T":{},"__F":{"name":"Special"},"__ROWS":0,"__T__ROWS_PAGE":35}}""",
            ))
            server.start()
            val repository = NgaRepository(testClient(server))

            repository.loadTopics(NgaSession("123", "secret"), fid = "", stid = "456", page = 1)

            val request = server.takeRequest()
            assertEquals("456", request.requestUrl?.queryParameter("stid"))
            assertNull(request.requestUrl?.queryParameter("fid"))
        }
    }

    @Test
    fun newPostOrderingUsesServerOrderByQuery() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"time":1000,"data":{"__T":{},"__F":{"name":"Forum"},"__ROWS":0,"__T__ROWS_PAGE":35}}""",
            ))
            server.start()

            val page = NgaRepository(testClient(server)).loadTopics(
                session = NgaSession("123", "secret"),
                fid = "-7",
                stid = "",
                page = 1,
                orderBy = "postdatedesc",
            )

            val request = server.takeRequest()
            assertEquals("postdatedesc", request.requestUrl?.queryParameter("order_by"))
            assertEquals(1000L, page.serverTimeSeconds)
        }
    }

    @Test
    fun parsesTopicTitleStyleAndParentBoard() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"__T":{"0":{"tid":11,"fid":22,"subject":"Styled","author":"User","authorid":1,"titlefont":"34","type":1024,"parent":{"2":"Sub board"}}},"__F":{"name":"Parent"},"__ROWS":1,"__T__ROWS_PAGE":35}}""",
            ))
            server.start()

            val topic = NgaRepository(testClient(server))
                .loadTopics(NgaSession("123", "secret"), fid = "22", stid = "", page = 1)
                .topics.single()

            assertEquals(34, topic.titleFont)
            assertEquals(1024, topic.type)
            assertEquals("Sub board", topic.boardName)
        }
    }

    @Test
    fun fillsPostAuthorAndAvatarFromUserTable() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"__R":{"0":{"pid":0,"tid":12,"fid":22,"lou":0,"authorid":38670554,"subject":"Topic","content":"Body","postdatetimestamp":1}},"__U":{"38670554":{"username":"RealUser","avatar":"/avatars/user.png"}},"__T":{"subject":"Topic"},"__F":{"name":"Forum"},"__ROWS":1,"__R__ROWS_PAGE":20}}""",
            ))
            server.start()

            val post = NgaRepository(testClient(server))
                .loadThread(NgaSession("123", "secret"), tid = 12, page = 1)
                .posts.single()

            assertEquals("RealUser", post.author)
            assertEquals("https://img.nga.cn/avatars/user.png", post.avatarUrl)
        }
    }

    @Test
    fun parsesPostScoresAndUserIpLocation() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"__R":{"0":{"pid":1,"tid":12,"fid":22,"lou":1,"authorid":7,"content":"Body","score":19,"score_2":3}},"__U":{"7":{"username":"User"}},"__T":{"subject":"Topic"},"__ROWS":1,"__R__ROWS_PAGE":20}}""",
            ))
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"0":{"uid":7,"ipLoc":"\u5e7f\u4e1c"}}}""",
            ))
            server.start()
            val repository = NgaRepository(testClient(server))

            val post = repository.loadThread(NgaSession("123", "secret"), tid = 12, page = 1).posts.single()
            val location = repository.loadUserIpLocation(NgaSession("123", "secret"), 7)

            assertEquals(19, post.score)
            assertEquals(3, post.scoreDown)
            assertEquals("广东", location)
            server.takeRequest()
            assertEquals("7", server.takeRequest().requestUrl?.queryParameter("uid"))
        }
    }

    @Test
    fun loadsPublicUserProfileAndActivitySamples() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"0":{"uid":7,"username":"Analyst","avatar":"/a.png","posts":"88","regdate":100,"ipLoc":"\u4e0a\u6d77"}}}""",
            ))
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"__T":{"0":{"tid":11,"authorid":7,"subject":"topic","postdate":100,"parent":{"2":"hardware"}}},"__ROWS":1,"__T__ROWS_PAGE":35}}""",
            ))
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"__T":{"0":{"tid":12,"authorid":7,"subject":"reply target","parent":{"2":"hardware"},"__P":{"postdate":200,"content":"[quote]old[/quote]valid reply"}}},"__ROWS":1,"__T__ROWS_PAGE":35}}""",
            ))
            server.start()
            val repository = NgaRepository(testClient(server))

            val profile = repository.loadUserProfile(NgaSession("123", "secret"), 7)
            val activity = repository.loadUserActivityAnalysis(NgaSession("123", "secret"), 7)

            assertEquals("Analyst", profile.userName)
            assertEquals("上海", profile.ipLocation)
            assertEquals(1, activity.topicCount)
            assertEquals(1, activity.replyCount)
            assertEquals("valid reply", activity.replies.single().replySnippet)
            server.takeRequest()
            assertEquals("1", server.takeRequest().requestUrl?.queryParameter("page"))
        }
    }

    @Test
    fun keepsSuccessfulUserActivityPagesWhenLaterPageFails() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"__T":{"0":{"tid":11,"authorid":7,"subject":"topic","postdate":100,"parent":{"2":"hardware"}}},"__ROWS":70,"__T__ROWS_PAGE":35}}""",
            ))
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"__T":{"0":{"tid":12,"authorid":7,"subject":"reply target","parent":{"2":"hardware"},"__P":{"postdate":200,"content":"valid reply"}}},"__ROWS":1,"__T__ROWS_PAGE":35}}""",
            ))
            server.start()

            val activity = NgaRepository(testClient(server))
                .loadUserActivityAnalysis(NgaSession("123", "secret"), 7)

            assertEquals(1, activity.topicCount)
            assertEquals(1, activity.replyCount)
            assertTrue(activity.isPartial)
            assertEquals("主题", activity.loadIssues.single().section)
            assertEquals(2, activity.loadIssues.single().page)
        }
    }

    @Test
    fun fallsBackToHtmlWhenThreadJsonIsTruncated() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody("{\"data\":{\"__R\":{\"1\":{\"pi"))
            server.enqueue(MockResponse().setResponseCode(200).setBody(HTML_PAGE))
            server.start()
            val client = testClient(server)

            val page = NgaRepository(client).loadThread(NgaSession("123", "secret"), 47387156, 2)

            assertEquals(1, page.posts.size)
            assertEquals(20, page.posts.single().floor)
            val jsonRequest = server.takeRequest()
            val htmlRequest = server.takeRequest()
            assertEquals("8", jsonRequest.requestUrl?.queryParameter("__output"))
            assertNull(htmlRequest.requestUrl?.queryParameter("__output"))
        }
    }

    @Test
    fun parsesNestedRepliesFromCommentTable() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody(
                """{"data":{"__R":{"0":{"pid":1,"tid":12,"fid":22,"lou":0,"authorid":1,"content":"floor","comment":{"0":{"pid":2,"tid":12,"fid":22,"authorid":2,"author":"ReplyUser","content":"nested reply","postdatetimestamp":2}}}},"__U":{"1":{"username":"FloorUser"}},"__T":{"subject":"Topic"},"__ROWS":1,"__R__ROWS_PAGE":20}}""",
            ))
            server.start()

            val post = NgaRepository(testClient(server))
                .loadThread(NgaSession("123", "secret"), tid = 12, page = 1)
                .posts.single()

            assertEquals("nested reply", post.comments.single().content)
            assertEquals("ReplyUser", post.comments.single().author)
        }
    }

    private companion object {
        fun testClient(server: MockWebServer) = NgaClient(
            client = OkHttpClient(),
            domains = listOf(server.url("/").toString().removeSuffix("/")),
            minimumIntervalMillis = 0,
            transientRetryDelaysMillis = emptyList(),
            )

        val HTML_PAGE = """
            <script>
            __CURRENT_TID=47387156; __CURRENT_FID=-7; __CURRENT_PAGE=2; __CURRENT_PAGE_POSTS=20;
            commonui.userInfo.setAll({"123":{"username":"用户"}});
            commonui.postArg.setDefault(-7,0,47387156,"",21,1785038476,20);
            </script>
            <h1 id='currentTopicName'>主题</h1><h2 id='currentForumName'>版块</h2>
            <h3 id='postsubject20'></h3><span id='postcontent20'>正文</span>
            <script>commonui.postArg.proc(20,null,null,null,null,null,null,null,null,null,878013195,0,null,'123',1785038476,'0,0,0','2','','','',null);</script>
        """.trimIndent()
    }
}
