package com.aki.lnga.data

import android.content.ContentValues
import android.content.Context
import android.media.MediaScannerConnection
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Log
import com.aki.lnga.BuildConfig
import java.io.File
import java.util.Locale
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.OkHttpClient
import okhttp3.Request

internal object ImageDownloader {
    private val client = OkHttpClient.Builder()
        .addInterceptor(NgaImageCdnRecoveryInterceptor())
        .addInterceptor(NgaImageProtocolFallbackInterceptor())
        .build()

    suspend fun download(context: Context, url: String): Result<String> = withContext(Dispatchers.IO) {
        val normalized = normalizeNgaImageUrl(url)
        runCatching {
            val request = normalized.applyFallbackMarker(
                Request.Builder().url(normalized.request)
                    .header("User-Agent", "Mozilla/5.0 (Android) LNGA-Android")
                    .header("Referer", "https://bbs.nga.cn/"),
            ).build()
            val response = client.newCall(request).execute()
            response.use {
                ImageDiagnosticStore.record(
                    context,
                    stage = "download-network",
                    purpose = "manual-download",
                    url = url,
                    detail = "protocolUpgrade=${normalized.upgradedToHttps} hostMigration=${normalized.migratedHost} code=${it.code} " +
                        "protocolFallback=${normalized.upgradedToHttps && it.request.url.scheme == "http"} " +
                        "final=${sanitizedImageUrl(it.request.url.toString())} " +
                        "type=${it.header("Content-Type").orEmpty().substringBefore(';')} " +
                        "length=${it.header("Content-Length").orEmpty()} cache=${it.header("eo-cache-status").orEmpty()} " +
                        "age=${it.header("age").orEmpty()} requestId=${it.header("x-request-id").orEmpty()}",
                )
                if (BuildConfig.DEBUG) {
                    Log.d(
                        "LNGA.Image",
                        "download response=${it.code} type=${it.header("Content-Type")} " +
                            "length=${it.body?.contentLength()} url=${safeImageUrl(url)}",
                    )
                }
                check(it.isSuccessful) { "HTTP ${it.code}" }
                val body = checkNotNull(it.body) { "图片内容为空" }
                val mime = body.contentType()?.toString()?.substringBefore(';') ?: mimeFromUrl(url)
                val extension = extensionFor(mime, url)
                val name = "LNGA_${System.currentTimeMillis()}.$extension"
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val resolver = context.contentResolver
                    val values = ContentValues().apply {
                        put(MediaStore.Images.Media.DISPLAY_NAME, name)
                        put(MediaStore.Images.Media.MIME_TYPE, mime)
                        put(MediaStore.Images.Media.RELATIVE_PATH, "${Environment.DIRECTORY_PICTURES}/LNGA")
                        put(MediaStore.Images.Media.IS_PENDING, 1)
                    }
                    val uri = checkNotNull(
                        resolver.insert(MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values),
                    ) { "无法创建图片文件" }
                    try {
                        resolver.openOutputStream(uri)?.use { output -> body.byteStream().copyTo(output) }
                            ?: error("无法写入图片")
                        values.clear()
                        values.put(MediaStore.Images.Media.IS_PENDING, 0)
                        resolver.update(uri, values, null, null)
                    } catch (error: Throwable) {
                        resolver.delete(uri, null, null)
                        throw error
                    }
                } else {
                    @Suppress("DEPRECATION")
                    val directory = File(
                        Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES),
                        "LNGA",
                    ).apply { mkdirs() }
                    val file = File(directory, name)
                    file.outputStream().use { output -> body.byteStream().copyTo(output) }
                    MediaScannerConnection.scanFile(context, arrayOf(file.absolutePath), arrayOf(mime), null)
                }
                name
            }
        }.also { result ->
            if (result.isFailure) {
                val error = result.exceptionOrNull()
                ImageDiagnosticStore.record(
                    context,
                    stage = "download-error",
                    purpose = "manual-download",
                    url = url,
                    detail = "type=${error?.javaClass?.simpleName} message=${error?.message.orEmpty()}",
                )
            }
            if (BuildConfig.DEBUG && result.isFailure) {
                Log.e("LNGA.Image", "download error url=${safeImageUrl(url)}", result.exceptionOrNull())
            }
        }
    }

    private fun mimeFromUrl(url: String): String = when (url.substringBefore('?').substringAfterLast('.').lowercase(Locale.ROOT)) {
        "png" -> "image/png"
        "gif" -> "image/gif"
        "webp" -> "image/webp"
        else -> "image/jpeg"
    }

    private fun extensionFor(mime: String, url: String): String = when (mime.lowercase(Locale.ROOT)) {
        "image/png" -> "png"
        "image/gif" -> "gif"
        "image/webp" -> "webp"
        "image/heic", "image/heif" -> "heic"
        "image/jpeg", "image/jpg" -> "jpg"
        else -> url.substringBefore('?').substringAfterLast('.', "jpg").takeIf { it.length in 2..5 } ?: "jpg"
    }

    private fun safeImageUrl(url: String): String = runCatching {
        val uri = java.net.URI(url)
        "${uri.scheme}://${uri.host}${uri.path}".take(240)
    }.getOrElse { "invalid-url#${url.hashCode()}" }
}
