package com.aki.lnga.data

import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.launch
import kotlinx.coroutines.cancelAndJoin
import kotlinx.coroutines.Dispatchers
import okhttp3.OkHttpClient
import okhttp3.mockwebserver.MockResponse
import okhttp3.mockwebserver.MockWebServer
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.concurrent.TimeUnit

class AiClientTest {
    @Test
    fun listsModelsWithBearerAuthentication() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"data":[{"id":"z-model"},{"id":"a-model"}]}"""))
            server.start()
            val client = AiClient(OkHttpClient())

            val models = client.listModels(config(server), "top-secret")

            assertEquals(listOf("a-model", "z-model"), models)
            val request = server.takeRequest()
            assertEquals("/v1/models", request.path)
            assertEquals("Bearer top-secret", request.getHeader("Authorization"))
        }
    }

    @Test
    fun connectionTestUsesSmallNonStreamingChatRequest() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(MockResponse().setResponseCode(200).setBody("""{"choices":[{"message":{"content":"OK"}}]}"""))
            server.start()

            val result = AiClient(OkHttpClient()).testConnection(config(server), "secret")

            assertTrue(result.message.contains("连接成功"))
            val request = server.takeRequest()
            assertEquals("/v1/chat/completions", request.path)
            val body = request.body.readUtf8()
            assertTrue(body.contains("\"max_tokens\":1"))
            assertTrue(body.contains("\"stream\":false"))
            assertFalse(body.contains("secret"))
        }
    }

    @Test
    fun errorsAreTypedAndExactApiKeyIsRedacted() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setResponseCode(401)
                    .setBody("""{"error":{"message":"invalid key sk-private-value"}}"""),
            )
            server.start()

            val error = runCatching {
                AiClient(OkHttpClient()).listModels(config(server), "sk-private-value")
            }.exceptionOrNull()

            assertTrue(error is AiServiceException)
            assertEquals(AiErrorKind.Unauthorized, (error as AiServiceException).kind)
            assertFalse(error.message.orEmpty().contains("sk-private-value"))
            assertTrue(error.message.orEmpty().contains("[redacted]"))
        }
    }

    @Test
    fun streamsSseDeltasAndSendsBoundedConversationPayload() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setResponseCode(200)
                    .setHeader("Content-Type", "text/event-stream")
                    .setBody(
                        "data: {\"choices\":[{\"delta\":{\"content\":\"你好\"}}]}\n\n" +
                            "data: {\"choices\":[{\"delta\":{\"content\":\"！\"}}]}\n\n" +
                            "data: [DONE]\n\n",
                    ),
            )
            server.start()
            val chunks = mutableListOf<String>()

            AiClient(OkHttpClient()).streamChat(
                config(server),
                "secret",
                listOf(AiChatRequestMessage("user", "问题")),
                chunks::add,
            )

            assertEquals("你好！", chunks.joinToString(""))
            val request = server.takeRequest()
            assertEquals("/v1/chat/completions", request.path)
            val body = request.body.readUtf8()
            assertTrue(body.contains("\"stream\":true"))
            assertTrue(body.contains("\"content\":\"问题\""))
            assertFalse(body.contains("web_search"))
            assertFalse(body.contains("secret"))
        }
    }

    @Test
    fun cancellingStreamingRequestStopsTheUnderlyingCall() = runBlocking {
        MockWebServer().use { server ->
            server.enqueue(
                MockResponse().setResponseCode(200)
                    .setHeadersDelay(10, TimeUnit.SECONDS)
                    .setBody("data: {\"choices\":[{\"delta\":{\"content\":\"late\"}}]}\n\n"),
            )
            server.start()
            val chunks = mutableListOf<String>()
            val job = launch(Dispatchers.Default) {
                AiClient(OkHttpClient()).streamChat(
                    config(server),
                    "secret",
                    listOf(AiChatRequestMessage("user", "stop")),
                    chunks::add,
                )
            }
            assertTrue(server.takeRequest(2, TimeUnit.SECONDS) != null)

            job.cancelAndJoin()

            assertTrue(job.isCancelled)
            assertTrue(chunks.isEmpty())
        }
    }

    private fun config(server: MockWebServer) = AiConfig(
        id = "test",
        name = "Test",
        presetId = "custom",
        baseUrl = server.url("/v1").toString().trimEnd('/'),
        model = "test-model",
        active = true,
        capabilities = setOf(AiCapability.ChatCompletions, AiCapability.ModelList),
    )
}
