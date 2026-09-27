package com.aki.lnga.data

import android.content.Context
import com.aki.lnga.model.NgaSession
import java.io.IOException
import java.net.SocketTimeoutException
import java.nio.charset.Charset
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext
import kotlinx.coroutines.currentCoroutineContext
import kotlinx.coroutines.ensureActive
import kotlinx.coroutines.job
import okhttp3.HttpUrl.Companion.toHttpUrl
import okhttp3.OkHttpClient
import okhttp3.Request

internal class NgaClient(
    private val client: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(30, TimeUnit.SECONDS)
        .build(),
    private val domains: List<String> = listOf(
        "https://bbs.nga.cn",
        "https://ngabbs.com",
        "https://nga.178.com",
    ),
    private val minimumIntervalMillis: Long = 350L,
    private val transientRetryDelaysMillis: List<Long> = listOf(600L, 1_400L),
    diagnosticContext: Context? = null,
) {
    private val lastRequestAt = ConcurrentHashMap<String, Long>()
    private val diagnosticContext = diagnosticContext?.applicationContext

    suspend fun get(
        path: String,
        query: Map<String, String>,
        session: NgaSession,
        diagnosticPurpose: String = path,
    ): String = request(path, query, session, addCharset = true, diagnosticPurpose = diagnosticPurpose)

    suspend fun getHtml(
        path: String,
        query: Map<String, String>,
        session: NgaSession,
        diagnosticPurpose: String = path,
    ): String = request(path, query, session, addCharset = false, diagnosticPurpose = diagnosticPurpose)

    private suspend fun request(
        path: String,
        query: Map<String, String>,
        session: NgaSession,
        addCharset: Boolean,
        diagnosticPurpose: String,
    ): String = withContext(Dispatchers.IO) {
        var lastError: IOException? = null
        var lastTemporaryCode: Int? = null
        var attempt = 0
        for (round in 0..transientRetryDelaysMillis.size) {
            var transientFailure = false
            for (domain in domains) {
                attempt++
                val startedAt = System.currentTimeMillis()
                try {
                    throttle(domain)
                    val response = execute(domain, path, query, session, addCharset)
                    recordDiagnostic(
                        diagnosticPurpose,
                        domain,
                        attempt,
                        "success",
                        "code=${response.code} elapsedMs=${System.currentTimeMillis() - startedAt}",
                    )
                    return@withContext response.text
                } catch (error: NgaContentUnavailableException) {
                    throw error
                } catch (error: NgaAuthenticationException) {
                    throw error
                } catch (error: IOException) {
                    currentCoroutineContext().ensureActive()
                    lastError = error
                    val http = error as? NgaHttpStatusException
                    if (http?.isTemporary == true) lastTemporaryCode = http.code
                    transientFailure = transientFailure || http?.isTemporary == true
                    recordDiagnostic(
                        diagnosticPurpose,
                        domain,
                        attempt,
                        "failure",
                        "code=${http?.code ?: 0} type=${error.javaClass.simpleName} " +
                            "elapsedMs=${System.currentTimeMillis() - startedAt} ${http?.diagnosticMetadata.orEmpty()}",
                    )
                }
            }
            if (!transientFailure || round >= transientRetryDelaysMillis.size) break
            val wait = transientRetryDelaysMillis[round].coerceAtLeast(0L)
            recordDiagnostic(diagnosticPurpose, "all", attempt, "backoff", "delayMs=$wait")
            if (wait > 0) delay(wait)
        }
        lastTemporaryCode?.let { throw NgaTemporaryUnavailableException(it) }
        val finalError = lastError
        throw finalError ?: IOException("NGA 请求失败")
    }

    private fun recordDiagnostic(
        purpose: String,
        domain: String,
        attempt: Int,
        stage: String,
        detail: String,
    ) {
        val host = runCatching { domain.toHttpUrl().host }.getOrDefault("unknown")
        NgaNetworkDiagnosticStore.record(diagnosticContext, purpose, host, attempt, stage, detail)
    }

    private data class NgaResponse(val text: String, val code: Int)

    private class NgaHttpStatusException(
        val code: Int,
        val diagnosticMetadata: String,
        message: String,
    ) : IOException(message) {
        val isTemporary: Boolean get() = code == 502 || code == 503 || code == 504
    }

    private suspend fun throttle(domain: String) {
        val now = System.currentTimeMillis()
        val wait = minimumIntervalMillis - (now - (lastRequestAt[domain] ?: 0L))
        if (wait > 0) delay(wait)
        lastRequestAt[domain] = System.currentTimeMillis()
    }

    private suspend fun execute(
        domain: String,
        path: String,
        query: Map<String, String>,
        session: NgaSession,
        addCharset: Boolean,
    ): NgaResponse {
        val base = (domain + path).toHttpUrl()
        val url = base.newBuilder().apply {
            query.forEach { (name, value) -> addQueryParameter(name, value) }
            if (addCharset) addQueryParameter("__inchst", "UTF8")
        }.build()
        val request = Request.Builder()
            .url(url)
            .header("User-Agent", "NGA_WP_JW")
            .header("X-User-Agent", "Nga_Official")
            .header("Cookie", "ngaPassportUid=${session.uid}; ngaPassportCid=${session.cid}")
            .get()
            .build()

        val call = client.newCall(request)
        val cancellation = currentCoroutineContext().job.invokeOnCompletion { call.cancel() }
        try {
            call.execute().use { response ->
                val bytes = response.body?.bytes() ?: ByteArray(0)
                val text = bytes.toString(GB18030)
                if (response.code == 403 && text.contains("未登录")) {
                    throw NgaAuthenticationException(
                        "NGA 未识别这组登录凭据，请重新获取当前会话的 ngaPassportUid 和 ngaPassportCid",
                    )
                }
                if (response.code == 403) {
                    throw NgaContentUnavailableException(
                        "该内容可能已被管理员隐藏、删除，或当前账号没有查看权限（HTTP 403）",
                    )
                }
                if (!response.isSuccessful) {
                    val metadata = "server=${response.header("server").orEmpty().take(60)} " +
                        "cache=${response.header("eo-cache-status").orEmpty().take(60)} " +
                        "requestId=${response.header("x-request-id").orEmpty().take(80)}"
                    throw NgaHttpStatusException(response.code, metadata, "HTTP ${response.code}: ${text.take(160)}")
                }
                if (text.isBlank()) throw SocketTimeoutException("NGA 返回空响应")
                return NgaResponse(text, response.code)
            }
        } finally {
            cancellation.dispose()
        }
    }

    private companion object {
        val GB18030: Charset = Charset.forName("GB18030")
    }
}

internal class NgaAuthenticationException(message: String) : IOException(message)
internal class NgaContentUnavailableException(message: String) : IOException(message)
internal class NgaTemporaryUnavailableException(val statusCode: Int) : IOException(
    "NGA 服务或当前网络节点暂时不可用，请稍后重试（HTTP $statusCode）",
)
