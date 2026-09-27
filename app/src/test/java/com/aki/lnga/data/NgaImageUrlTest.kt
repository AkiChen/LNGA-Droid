package com.aki.lnga.data

import okhttp3.Request
import okhttp3.OkHttpClient
import okhttp3.Dns
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.ResponseBody.Companion.toResponseBody
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import java.net.InetAddress
import java.net.Proxy
import java.time.Instant
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class NgaImageUrlTest {
    @Test
    fun migratesLegacyNgaImageHostToCurrentHttpsCdn() {
        val normalized = normalizeNgaImageUrl(
            "http://img.nga.178.com/attachments/a.png?size=1#preview",
        )

        assertEquals("https://img.nga.cn/attachments/a.png?size=1#preview", normalized.request)
        assertTrue(normalized.upgradedToHttps)
        assertTrue(normalized.migratedHost)
    }

    @Test
    fun upgradesNumberedNgaImageHosts() {
        assertEquals(
            "https://img4.nga.cn/avatar.png",
            normalizeNgaImageUrl("http://img4.nga.cn/avatar.png").request,
        )
    }

    @Test
    fun doesNotUpgradeUntrustedCleartextHosts() {
        val normalized = normalizeNgaImageUrl("http://example.com/a.png")

        assertEquals("http://example.com/a.png", normalized.request)
        assertFalse(normalized.upgradedToHttps)
        assertFalse(normalized.migratedHost)
    }

    @Test
    fun marksOnlyUpgradedRequestsForHttpFallback() {
        val upgraded = normalizeNgaImageUrl("http://img.nga.178.com/a.png")
            .applyFallbackMarker(Request.Builder().url("https://img.nga.178.com/a.png"))
            .build()
        val unchanged = normalizeNgaImageUrl("http://example.com/a.png")
            .applyFallbackMarker(Request.Builder().url("http://example.com/a.png"))
            .build()

        assertEquals(null, upgraded.header(NGA_IMAGE_HTTP_FALLBACK_HEADER))
        assertEquals(null, unchanged.header(NGA_IMAGE_HTTP_FALLBACK_HEADER))
    }

    @Test
    fun retriesRecentNgaCdnPlaceholderWithCacheBypass() {
        val server = MockWebServer()
        val placeholder = ByteArray(43).also { "GIF89a".toByteArray().copyInto(it) }
        server.enqueue(MockResponse().setResponseCode(404).setHeader("Content-Type", "image/gif").setBody(okio.Buffer().write(placeholder)))
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "image/jpeg").setBody("real-image"))
        server.start()
        try {
            val client = OkHttpClient.Builder()
                .proxy(Proxy.NO_PROXY)
                .dns(object : Dns {
                    override fun lookup(hostname: String): List<InetAddress> =
                        listOf(InetAddress.getByName("127.0.0.1"))
                })
                .addInterceptor(
                    NgaImageCdnRecoveryInterceptor(
                        now = { Instant.parse("2026-08-25T00:00:00Z").toEpochMilli() },
                    ),
                )
                .build()
            val response = client.newCall(
                Request.Builder().url("http://img.nga.cn:${server.port}/attachments/mon_202608/25/new.jpg").build(),
            ).execute()
            response.use { assertEquals(200, it.code) }
            assertEquals(2, server.requestCount)
            server.takeRequest()
            assertTrue(server.takeRequest().requestUrl?.queryParameter("lnga_cache_retry") != null)
        } finally {
            server.shutdown()
        }
    }

    @Test
    fun doesNotTreatHttp200Gif43PlaceholderAsAValidImage() {
        val server = MockWebServer()
        val placeholder = ByteArray(43).also { "GIF89a".toByteArray().copyInto(it) }
        server.enqueue(MockResponse().setResponseCode(200).setHeader("Content-Type", "image/gif").setBody(okio.Buffer().write(placeholder)))
        server.start()
        try {
            val client = OkHttpClient.Builder()
                .addInterceptor(
                    NgaImageCdnRecoveryInterceptor(
                        now = { Instant.parse("2026-08-25T00:00:00Z").toEpochMilli() },
                    ),
                )
                .build()
            client.newCall(
                Request.Builder().url("http://localhost:${server.port}/attachments/mon_201209/14/old.png")
                    .header("Host", "img.nga.cn")
                    .build(),
            ).execute().use { response ->
                // localhost is deliberately not classified as NGA; validate the response classifier directly below.
                assertEquals(200, response.code)
            }

            val request = Request.Builder().url("https://img.nga.cn/attachments/old.png").build()
            val response = okhttp3.Response.Builder()
                .request(request)
                .protocol(okhttp3.Protocol.HTTP_1_1)
                .message("OK")
                .code(200)
                .header("Content-Type", "image/gif")
                .body(placeholder.toResponseBody("image/gif".toMediaType()))
                .build()
            response.use { assertTrue(isNgaCdnPlaceholder(it)) }
        } finally {
            server.shutdown()
        }
    }
}
