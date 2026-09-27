package com.aki.lnga.ui

import android.graphics.drawable.BitmapDrawable
import android.util.Log
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.runtime.staticCompositionLocalOf
import androidx.compose.ui.platform.LocalContext
import coil.request.CachePolicy
import coil.request.ImageRequest
import com.aki.lnga.BuildConfig
import com.aki.lnga.data.ImageDiagnosticStore
import com.aki.lnga.data.NGA_IMAGE_HTTP_FALLBACK_HEADER
import com.aki.lnga.data.normalizeNgaImageUrl
import okhttp3.Headers

private const val IMAGE_LOG_TAG = "LNGA.Image"
internal val LocalImageDiagnosticContext = staticCompositionLocalOf { "unknown-post" }

/**
 * NGA attachments can reject or replace hot-linked requests. Keep the display request headers aligned
 * with the downloader, and use software bitmaps to avoid device-specific GPU rendering failures.
 */
@Composable
internal fun rememberNgaImageRequest(url: String, purpose: String, retryToken: Int = 0): ImageRequest {
    val context = LocalContext.current
    val postContext = LocalImageDiagnosticContext.current
    return remember(context, url, purpose, retryToken, postContext) {
        val diagnosticPurpose = "$purpose $postContext"
        val normalized = normalizeNgaImageUrl(url)
        ImageRequest.Builder(context)
            .data(normalized.request)
            .headers(
                Headers.Builder().apply {
                    add("User-Agent", "Mozilla/5.0 (Android) LNGA-Android")
                    add("Referer", "https://bbs.nga.cn/")
                    if (normalized.upgradedToHttps) add(NGA_IMAGE_HTTP_FALLBACK_HEADER, "1")
                }.build(),
            )
            .allowHardware(false)
            .memoryCacheKey("lnga-software-v3:$retryToken:${normalized.request}")
            .apply {
                if (retryToken > 0) {
                    memoryCachePolicy(CachePolicy.DISABLED)
                    diskCachePolicy(CachePolicy.DISABLED)
                }
            }
            .listener(
                onStart = {
                    imageLog("start purpose=$purpose url=${safeImageUrl(url)} hardware=false")
                    ImageDiagnosticStore.record(
                        context,
                        "start",
                        diagnosticPurpose,
                        url,
                        "hardware=false retry=$retryToken protocolUpgrade=${normalized.upgradedToHttps} " +
                            "hostMigration=${normalized.migratedHost} " +
                            "request=${when {
                                normalized.migratedHost -> "host-migrated"
                                normalized.upgradedToHttps -> "https"
                                else -> "unchanged"
                            }}",
                    )
                },
                onSuccess = { _, result ->
                    val drawable = result.drawable
                    val bitmapDetails = (drawable as? BitmapDrawable)?.bitmap?.let { bitmap ->
                        " config=${bitmap.config} alpha=${bitmap.hasAlpha()} colorSpace=${bitmap.colorSpace?.name} bytes=${bitmap.allocationByteCount}"
                    }.orEmpty()
                    imageLog(
                        "success purpose=$purpose source=${result.dataSource} sampled=${result.isSampled} " +
                            "drawable=${drawable.javaClass.simpleName} size=${drawable.intrinsicWidth}x${drawable.intrinsicHeight}" +
                            "$bitmapDetails url=${safeImageUrl(url)}",
                    )
                    ImageDiagnosticStore.record(
                        context,
                        "success",
                        diagnosticPurpose,
                        url,
                        "source=${result.dataSource} sampled=${result.isSampled} drawable=${drawable.javaClass.simpleName} " +
                            "size=${drawable.intrinsicWidth}x${drawable.intrinsicHeight}$bitmapDetails",
                    )
                },
                onError = { _, result ->
                    imageLog(
                        "error purpose=$purpose type=${result.throwable.javaClass.simpleName} " +
                            "message=${result.throwable.message} url=${safeImageUrl(url)}",
                        result.throwable,
                    )
                    ImageDiagnosticStore.record(
                        context,
                        "decode-error",
                        diagnosticPurpose,
                        url,
                        "type=${result.throwable.javaClass.simpleName} message=${result.throwable.message.orEmpty()} " +
                            "userMessage=${imageFailureMessage(result.throwable)} retry=$retryToken",
                    )
                },
                onCancel = {
                    imageLog("cancel purpose=$purpose url=${safeImageUrl(url)}")
                    ImageDiagnosticStore.record(context, "cancel", diagnosticPurpose, url, "retry=$retryToken")
                },
            )
            .build()
    }
}

internal fun imageFailureMessage(error: Throwable?): String {
    val message = error?.message.orEmpty()
    return when {
        message.contains("CLEARTEXT", ignoreCase = true) ->
            "旧版 HTTP 图片被 Android 安全策略阻止；仅受信任的 NGA 图片域名会自动升级 HTTPS"
        message.contains("403") -> "图片被服务器拒绝，可能存在防盗链或访问权限限制"
        message.contains("CDN placeholder", ignoreCase = true) -> "图片服务器返回临时占位图，请稍后重试"
        message.contains("404") -> "图片不存在或已经失效"
        message.contains("timeout", ignoreCase = true) -> "图片加载超时，请检查网络后重试"
        message.contains("Unable to resolve host", ignoreCase = true) -> "无法连接图片服务器"
        message.contains("decode", ignoreCase = true) || error?.javaClass?.simpleName?.contains("Decode") == true ->
            "图片已下载，但 Android 无法解码这种格式或文件已损坏"
        message.contains("Content-Type", ignoreCase = true) || message.contains("text/html", ignoreCase = true) ->
            "服务器返回的不是图片，可能是登录页或错误页"
        else -> "图片展示失败，可重试或下载原图检查"
    }
}

private fun safeImageUrl(url: String): String = runCatching {
    val uri = java.net.URI(url)
    "${uri.scheme}://${uri.host}${uri.path}".take(240)
}.getOrElse { "invalid-url#${url.hashCode()}" }

private fun imageLog(message: String, error: Throwable? = null) {
    if (!BuildConfig.DEBUG) return
    if (error == null) Log.d(IMAGE_LOG_TAG, message) else Log.e(IMAGE_LOG_TAG, message, error)
}
