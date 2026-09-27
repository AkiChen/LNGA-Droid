package com.aki.lnga.ui

import com.aki.lnga.data.PageControlPosition
import com.aki.lnga.model.BbNode
import com.aki.lnga.model.Post
import androidx.compose.ui.graphics.Color
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PostPresentationTest {
    @Test
    fun formatsRecentPostTimeRelatively() {
        val now = 10_000_000L
        assertEquals("刚刚", formatPostTime(seconds = now / 1000 - 20, nowMillis = now))
        assertEquals("12分钟前", formatPostTime(seconds = now / 1000 - 12 * 60, nowMillis = now))
    }

    @Test
    fun formatsTopicUpdateTimeInHours() {
        val now = 20_000_000L
        assertEquals("3小时前", formatTopicUpdateTime(seconds = now / 1000 - 3 * 3600, nowMillis = now))
    }

    @Test
    fun replyCountColorIsGrayBelowFiveAndDeepensUntilSixty() {
        val neutral = Color(0xFF777777)
        val accent = Color(0xFFCC2222)

        assertEquals(neutral, replyCountColor(4, neutral, accent))
        assertEquals(accent, replyCountColor(60, neutral, accent))
        assertEquals(accent, replyCountColor(600, neutral, accent))
        assertTrue(replyCountColor(5, neutral, accent).red < replyCountColor(30, neutral, accent).red)
    }

    @Test
    fun returningFromUserProfileRestoresExactThreadViewport() {
        val posts = listOf(testPost(20), testPost(21), testPost(22))

        assertEquals(
            ThreadScrollTarget(index = 2, offset = 137),
            threadScrollTarget(
                pendingScrollIndex = 2,
                pendingScrollOffset = 137,
                pendingResumeFloor = null,
                posts = posts,
                hasOpeningPost = true,
            ),
        )
    }

    @Test
    fun normalThreadOpenStillUsesRequestedResumeFloor() {
        val posts = listOf(testPost(20), testPost(21), testPost(22))

        assertEquals(
            ThreadScrollTarget(index = 3, offset = 0),
            threadScrollTarget(
                pendingScrollIndex = null,
                pendingScrollOffset = 0,
                pendingResumeFloor = 22,
                posts = posts,
                hasOpeningPost = true,
            ),
        )
    }

    @Test
    fun pageControlUsesConfiguredSide() {
        assertEquals(PageControlSide.Left, pageControlSide(PageControlPosition.Left))
        assertEquals(PageControlSide.Right, pageControlSide(PageControlPosition.Right))
    }

    @Test
    fun obsoleteOrUnknownStoredPagePositionDefaultsToRight() {
        assertEquals(PageControlPosition.Right, PageControlPosition.fromStored("unknown"))
        assertEquals(PageControlPosition.Right, PageControlPosition.fromStored("Auto"))
    }

    @Test
    fun imageErrorsHaveActionableMessages() {
        assertEquals(
            "旧版 HTTP 图片被 Android 安全策略阻止；仅受信任的 NGA 图片域名会自动升级 HTTPS",
            imageFailureMessage(IllegalStateException("CLEARTEXT communication not permitted")),
        )
        assertEquals(
            "图片被服务器拒绝，可能存在防盗链或访问权限限制",
            imageFailureMessage(IllegalStateException("HTTP 403")),
        )
        assertEquals(
            "图片已下载，但 Android 无法解码这种格式或文件已损坏",
            imageFailureMessage(IllegalStateException("decode failed")),
        )
    }

    @Test
    fun internalBrowserKeepsMediaAppLinksExternal() {
        assertEquals(true, shouldOpenInInternalBrowser("https://bbs.nga.cn/read.php?tid=1", true))
        assertEquals(false, shouldOpenInInternalBrowser("https://b23.tv/abc", true))
        assertEquals(false, shouldOpenInInternalBrowser("https://weibo.com/1/2", true))
        assertEquals(false, shouldOpenInInternalBrowser("https://v.douyin.com/UMQCKeX7678", true))
        assertEquals(false, shouldOpenInInternalBrowser("https://example.com", false))
    }

    @Test
    fun formatsCacheSize() {
        assertEquals("1.5 KB", formatCacheBytes(1536))
        assertEquals("2.0 MB", formatCacheBytes(2L * 1024L * 1024L))
    }

    @Test
    fun longImageContentIncludesQuarterScreenPaddingOnBothEnds() {
        assertEquals(
            2500f,
            longImageContentHeightPx(
                viewportWidthPx = 1000f,
                imageWidthToHeightRatio = 0.5f,
                viewportHeightPx = 1000f,
                scale = 1f,
            ),
        )
    }

    @Test
    fun longImagePanBoundsReachBothBufferedEnds() {
        assertEquals(
            LongImageVerticalPanBounds(minimum = -3000f, maximum = 0f),
            longImageVerticalPanBounds(
                viewportWidthPx = 1000f,
                imageWidthToHeightRatio = 0.25f,
                viewportHeightPx = 2000f,
                scale = 1f,
            ),
        )
        assertEquals(
            LongImageVerticalPanBounds(minimum = -8000f, maximum = 0f),
            longImageVerticalPanBounds(
                viewportWidthPx = 1000f,
                imageWidthToHeightRatio = 0.25f,
                viewportHeightPx = 2000f,
                scale = 2f,
            ),
        )
    }

    @Test
    fun replyGalleryCollectsOwnImagesButExcludesQuotedMedia() {
        val nodes = listOf(
            BbNode.Image("https://img.example/one.jpg"),
            BbNode.Collapse("更多", listOf(BbNode.Image("https://img.example/two.jpg"))),
            BbNode.Quote(listOf(BbNode.Image("https://img.example/quoted.jpg"))),
            BbNode.Image("https://img.example/one.jpg"),
        )

        assertEquals(
            listOf("https://img.example/one.jpg", "https://img.example/two.jpg"),
            collectPostImageUrls(nodes),
        )
    }

    private fun testPost(floor: Int) = Post(
        tid = 1,
        fid = 2,
        pid = floor.toLong(),
        floor = floor,
        author = "user",
        authorId = 3,
        subject = "subject",
        content = "content",
        postDateSeconds = 1,
    )
}
