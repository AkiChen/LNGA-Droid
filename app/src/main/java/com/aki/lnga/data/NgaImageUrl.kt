package com.aki.lnga.data

import okhttp3.Interceptor
import okhttp3.Request
import okhttp3.Response
import okhttp3.HttpUrl
import okhttp3.HttpUrl.Companion.toHttpUrlOrNull
import java.io.IOException
import java.time.YearMonth
import java.time.temporal.ChronoUnit

internal const val NGA_IMAGE_HTTP_FALLBACK_HEADER = "X-LNGA-Allow-Http-Fallback"

internal data class NormalizedImageUrl(
    val original: String,
    val request: String,
    val upgradedToHttps: Boolean,
    val migratedHost: Boolean,
) {
    fun applyFallbackMarker(builder: Request.Builder): Request.Builder = builder.apply {
        if (upgradedToHttps && !migratedHost) header(NGA_IMAGE_HTTP_FALLBACK_HEADER, "1")
    }
}

internal data class NgaImageCdnRecovery(
    val originalUrl: String,
    val cacheStatus: String,
    val age: String,
    val requestId: String,
    val retryUrl: String,
)

/** Retries one recent NGA attachment when a CDN edge returns its 43-byte 404 GIF placeholder. */
internal class NgaImageCdnRecoveryInterceptor(
    private val now: () -> Long = System::currentTimeMillis,
    private val onRecovery: ((NgaImageCdnRecovery) -> Unit)? = null,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val request = chain.request()
        val response = chain.proceed(request)
        if (!isNgaCdnPlaceholder(response)) return response
        if (!isRecentNgaAttachment(request.url, now()) || request.url.queryParameter(CDN_RETRY_QUERY) != null) {
            return response.asUnavailablePlaceholder()
        }

        val recovery = NgaImageCdnRecovery(
            originalUrl = request.url.toString(),
            cacheStatus = response.header("eo-cache-status").orEmpty(),
            age = response.header("age").orEmpty(),
            requestId = response.header("x-request-id").orEmpty(),
            retryUrl = request.url.newBuilder()
                .addQueryParameter(CDN_RETRY_QUERY, (now() / 60_000L).toString())
                .build().toString(),
        )
        response.close()
        onRecovery?.invoke(recovery)
        try {
            Thread.sleep(250L)
        } catch (interrupted: InterruptedException) {
            Thread.currentThread().interrupt()
            throw IOException("CDN recovery interrupted", interrupted)
        }
        val retried = chain.proceed(
            request.newBuilder()
                .url(recovery.retryUrl)
                .header("Cache-Control", "no-cache")
                .header("Pragma", "no-cache")
                .build(),
        )
        return if (isNgaCdnPlaceholder(retried)) retried.asUnavailablePlaceholder() else retried
    }
}

/**
 * Tries the upgraded HTTPS URL first. Only requests explicitly marked as originating from a trusted
 * NGA HTTP image URL may be downgraded, and only after a transport failure such as an SSL handshake
 * error. The marker is removed before either request is sent to the server.
 */
internal class NgaImageProtocolFallbackInterceptor(
    private val onFallback: ((httpsUrl: String, httpUrl: String, error: Throwable) -> Unit)? = null,
) : Interceptor {
    override fun intercept(chain: Interceptor.Chain): Response {
        val markedRequest = chain.request()
        val allowFallback = markedRequest.header(NGA_IMAGE_HTTP_FALLBACK_HEADER) == "1"
        val primaryRequest = markedRequest.newBuilder().removeHeader(NGA_IMAGE_HTTP_FALLBACK_HEADER).build()
        try {
            return chain.proceed(primaryRequest)
        } catch (primaryError: IOException) {
            if (!allowFallback || primaryRequest.url.scheme != "https" ||
                !isTrustedNgaImageHost(primaryRequest.url.host)
            ) throw primaryError
            val fallbackUrl = primaryRequest.url.newBuilder().scheme("http").port(80).build()
            onFallback?.invoke(primaryRequest.url.toString(), fallbackUrl.toString(), primaryError)
            return try {
                chain.proceed(primaryRequest.newBuilder().url(fallbackUrl).build())
            } catch (fallbackError: IOException) {
                fallbackError.addSuppressed(primaryError)
                throw fallbackError
            }
        }
    }
}

internal fun normalizeNgaImageUrl(url: String): NormalizedImageUrl {
    val parsed = url.toHttpUrlOrNull()
    val host = parsed?.host.orEmpty().lowercase()
    val migratedHost = host in LEGACY_NGA_IMAGE_HOSTS
    val upgraded = runCatching {
        when {
            parsed == null -> url
            migratedHost -> parsed.newBuilder().scheme("https").host(CURRENT_NGA_IMAGE_HOST).port(443).build().toString()
            parsed.scheme.equals("http", ignoreCase = true) && isTrustedNgaImageHost(host) ->
                parsed.newBuilder().scheme("https").port(443).build().toString()
            else -> url
        }
    }.getOrDefault(url)
    return NormalizedImageUrl(
        original = url,
        request = upgraded,
        upgradedToHttps = parsed?.scheme == "http" && upgraded.startsWith("https://"),
        migratedHost = migratedHost,
    )
}

internal fun isTrustedNgaImageHost(host: String): Boolean {
    val normalized = host.lowercase()
    return normalized in LEGACY_NGA_IMAGE_HOSTS || NGA_CN_IMAGE_HOST.matches(normalized)
}

private val LEGACY_NGA_IMAGE_HOSTS = setOf(
    "img.nga.178.com",
    "img.ngacn.cc",
    "img.nga.donews.com",
    "img.ngabbs.com",
)
private const val CURRENT_NGA_IMAGE_HOST = "img.nga.cn"
private const val CDN_RETRY_QUERY = "lnga_cache_retry"
private val NGA_CN_IMAGE_HOST = Regex("img\\d*\\.nga\\.cn")

internal fun isNgaCdnPlaceholder(response: Response): Boolean {
    if (response.code !in setOf(200, 404) || response.request.url.host != CURRENT_NGA_IMAGE_HOST) return false
    val type = response.header("Content-Type").orEmpty().substringBefore(';').trim()
    if (!type.equals("image/gif", ignoreCase = true)) return false
    val bytes = runCatching { response.peekBody(64).bytes() }.getOrDefault(ByteArray(0))
    return bytes.size == 43 && bytes.copyOfRange(0, minOf(6, bytes.size)).toString(Charsets.US_ASCII) == "GIF89a"
}

private fun Response.asUnavailablePlaceholder(): Response = if (code == 404) this else newBuilder()
    .code(404)
    .message("NGA CDN placeholder")
    .build()

internal fun isRecentNgaAttachment(url: HttpUrl, nowMillis: Long): Boolean {
    val match = Regex("/attachments/mon_(\\d{4})(\\d{2})/").find(url.encodedPath) ?: return false
    val attachmentMonth = runCatching { YearMonth.of(match.groupValues[1].toInt(), match.groupValues[2].toInt()) }
        .getOrNull() ?: return false
    val currentMonth = YearMonth.from(java.time.Instant.ofEpochMilli(nowMillis).atZone(java.time.ZoneId.systemDefault()))
    val months = ChronoUnit.MONTHS.between(attachmentMonth, currentMonth)
    return months in 0..2
}
