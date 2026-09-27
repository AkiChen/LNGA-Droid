package com.aki.lnga.data

import com.aki.lnga.model.NgaSession
import java.nio.charset.Charset
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import okhttp3.mockwebserver.SocketPolicy
import okio.Buffer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class NgaClientTest {
    @Test
    fun sendsNgaHeadersCookieAndDecodesGb18030() = runBlocking {
        MockWebServer().use { server ->
            val encoded = "测试响应".toByteArray(Charset.forName("GB18030"))
            server.enqueue(MockResponse().setResponseCode(200).setBody(Buffer().write(encoded)))
            server.start()
            val baseUrl = server.url("/").toString().removeSuffix("/")
            val client = NgaClient(
                client = OkHttpClient(),
                domains = listOf(baseUrl),
                minimumIntervalMillis = 0,
            )

            val response = client.get(
                path = "/thread.php",
                query = mapOf("fid" to "-7", "lite" to "js"),
                session = NgaSession("123", "secret"),
            )

            assertEquals("测试响应", response)
            val request = server.takeRequest()
            assertEquals("NGA_WP_JW", request.getHeader("User-Agent"))
            assertEquals("Nga_Official", request.getHeader("X-User-Agent"))
            assertEquals("ngaPassportUid=123; ngaPassportCid=secret", request.getHeader("Cookie"))
            assertEquals("-7", request.requestUrl?.queryParameter("fid"))
            assertEquals("UTF8", request.requestUrl?.queryParameter("__inchst"))
        }
    }

    @Test
    fun htmlRequestDoesNotForceJsonCharsetParameter() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody("<html></html>"))
            server.start()
            val client = NgaClient(
                client = OkHttpClient(),
                domains = listOf(server.url("/").toString().removeSuffix("/")),
                minimumIntervalMillis = 0,
            )

            client.getHtml("/read.php", mapOf("tid" to "1"), NgaSession("123", "secret"))

            assertEquals(null, server.takeRequest().requestUrl?.queryParameter("__inchst"))
        }
    }

    @Test
    fun mapsNgaUnauthenticatedResponseToSafeMessage() = runBlocking {
        MockWebServer().use { server ->
            val encoded = "{\"error\":{\"0\":\"1:未登录\"}}".toByteArray(Charset.forName("GB18030"))
            server.enqueue(MockResponse().setResponseCode(403).setBody(Buffer().write(encoded)))
            server.start()
            val client = NgaClient(
                client = OkHttpClient(),
                domains = listOf(server.url("/").toString().removeSuffix("/")),
                minimumIntervalMillis = 0,
            )

            val error = runCatching {
                client.get("/nuke.php", emptyMap(), NgaSession("123", "expired"))
            }.exceptionOrNull()

            assertTrue(error is NgaAuthenticationException)
            assertEquals(
                "NGA 未识别这组登录凭据，请重新获取当前会话的 ngaPassportUid 和 ngaPassportCid",
                error?.message,
            )
        }
    }

    @Test
    fun mapsForbiddenContentToActionableMessage() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(403).setBody("forbidden"))
            server.start()
            val client = NgaClient(
                client = OkHttpClient(),
                domains = listOf(server.url("/").toString().removeSuffix("/")),
                minimumIntervalMillis = 0,
            )

            val error = runCatching {
                client.get("/read.php", mapOf("tid" to "1"), NgaSession("123", "secret"))
            }.exceptionOrNull()

            assertTrue(error is NgaContentUnavailableException)
            assertEquals(
                "该内容可能已被管理员隐藏、删除，或当前账号没有查看权限（HTTP 403）",
                error?.message,
            )
        }
    }

    @Test
    fun retriesTemporaryGatewayErrorsAndThenSucceeds() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(503))
            server.enqueue(MockResponse().setResponseCode(502))
            server.enqueue(MockResponse().setResponseCode(200).setBody("ok"))
            server.start()
            val client = NgaClient(
                client = OkHttpClient(),
                domains = listOf(server.url("/").toString().removeSuffix("/")),
                minimumIntervalMillis = 0,
                transientRetryDelaysMillis = listOf(0, 0),
            )

            assertEquals("ok", client.get("/thread.php", emptyMap(), NgaSession("123", "secret")))
            assertEquals(3, server.requestCount)
        }
    }

    @Test
    fun mapsExhaustedTemporaryErrorsToActionableMessage() = runBlocking {
        MockWebServer().use { server ->
            repeat(3) { server.enqueue(MockResponse().setResponseCode(503)) }
            server.start()
            val client = NgaClient(
                client = OkHttpClient(),
                domains = listOf(server.url("/").toString().removeSuffix("/")),
                minimumIntervalMillis = 0,
                transientRetryDelaysMillis = listOf(0, 0),
            )

            val error = runCatching {
                client.get("/thread.php", emptyMap(), NgaSession("123", "secret"))
            }.exceptionOrNull()

            assertTrue(error is NgaTemporaryUnavailableException)
            assertEquals("NGA 服务或当前网络节点暂时不可用，请稍后重试（HTTP 503）", error?.message)
            assertEquals(3, server.requestCount)
        }
    }

    @Test
    fun cancellingRequestStopsUnderlyingNgaCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setSocketPolicy(SocketPolicy.NO_RESPONSE))
            server.start()
            val client = NgaClient(
                client = OkHttpClient(),
                domains = listOf(server.url("/").toString().removeSuffix("/")),
                minimumIntervalMillis = 0,
            )
            val job = launch(Dispatchers.Default) {
                client.get("/read.php", mapOf("tid" to "1"), NgaSession("123", "secret"))
            }
            assertTrue(server.takeRequest(2, TimeUnit.SECONDS) != null)

            job.cancelAndJoin()

            assertTrue(job.isCancelled)
        }
    }
}
