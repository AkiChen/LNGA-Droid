package com.aki.lnga.data

import android.app.DownloadManager
import android.content.Context
import android.net.Uri
import android.os.Environment
import android.webkit.MimeTypeMap
import com.aki.lnga.model.NgaSession
import com.aki.lnga.model.PostAttachment
import java.io.IOException
import java.util.Locale
import java.util.concurrent.TimeUnit
import okhttp3.OkHttpClient
import okhttp3.Request

internal data class QueuedAttachmentDownload(val id: Long, val fileName: String)

internal object AttachmentDownloader {
    private val client = OkHttpClient.Builder()
        .connectTimeout(15, TimeUnit.SECONDS)
        .readTimeout(20, TimeUnit.SECONDS)
        .addInterceptor(NgaImageCdnRecoveryInterceptor())
        .addInterceptor(NgaImageProtocolFallbackInterceptor())
        .build()

    fun enqueue(context: Context, attachment: PostAttachment, session: NgaSession): Result<QueuedAttachmentDownload> =
        runCatching {
            val normalized = normalizeNgaImageUrl(attachment.url)
            val response = client.newCall(
                normalized.applyFallbackMarker(
                    Request.Builder()
                        .url(normalized.request)
                        .header("Range", "bytes=0-511")
                        .header("User-Agent", USER_AGENT)
                        .header("Referer", REFERER)
                        .header("Cookie", cookie(session)),
                ).build(),
            ).execute()
            val metadata = response.use {
                if (!it.isSuccessful || it.code == 416) {
                    throw IOException(attachmentHttpMessage(it.code))
                }
                val body = it.body ?: throw IOException("附件内容为空")
                val prefix = body.byteStream().use { input -> readPrefix(input, 512) }
                validateAttachmentPrefix(attachment.name, it.header("Content-Type"), prefix)?.let { reason ->
                    throw IOException(reason)
                }
                AttachmentMetadata(
                    url = it.request.url.toString(),
                    fileName = attachmentFileName(
                        attachment.name.ifBlank { contentDispositionName(it.header("Content-Disposition")) },
                        it.request.url.encodedPath,
                    ),
                    mimeType = attachmentMimeType(it.header("Content-Type"), attachment.name),
                )
            }
            val request = DownloadManager.Request(Uri.parse(metadata.url))
                .setTitle(metadata.fileName)
                .setDescription("LNGA 附件")
                .setMimeType(metadata.mimeType)
                .setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE_NOTIFY_COMPLETED)
                .setAllowedOverMetered(true)
                .setAllowedOverRoaming(false)
                .addRequestHeader("User-Agent", USER_AGENT)
                .addRequestHeader("Referer", REFERER)
                .addRequestHeader("Cookie", cookie(session))
                .setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, "LNGA/${metadata.fileName}")
            val manager = context.getSystemService(Context.DOWNLOAD_SERVICE) as DownloadManager
            QueuedAttachmentDownload(manager.enqueue(request), metadata.fileName)
        }

    private data class AttachmentMetadata(val url: String, val fileName: String, val mimeType: String)

    private fun cookie(session: NgaSession): String =
        "ngaPassportUid=${session.uid}; ngaPassportCid=${session.cid}"

    private fun readPrefix(input: java.io.InputStream, limit: Int): ByteArray {
        val output = java.io.ByteArrayOutputStream(limit)
        val buffer = ByteArray(256)
        while (output.size() < limit) {
            val read = input.read(buffer, 0, minOf(buffer.size, limit - output.size()))
            if (read <= 0) break
            output.write(buffer, 0, read)
        }
        return output.toByteArray()
    }

    private fun contentDispositionName(value: String?): String = value.orEmpty()
        .substringAfter("filename*=UTF-8''", "")
        .substringBefore(';')
        .ifBlank {
            value.orEmpty().substringAfter("filename=", "").trim().trim('"').substringBefore(';')
        }

    private const val USER_AGENT = "Mozilla/5.0 (Android) LNGA-Android"
    private const val REFERER = "https://bbs.nga.cn/"
}

internal fun attachmentHttpMessage(code: Int): String = when (code) {
    401, 403 -> "附件无权访问或登录状态已失效（HTTP $code）"
    404 -> "附件不存在或已经失效（HTTP 404）"
    502, 503, 504 -> "NGA 附件服务暂时不可用，请稍后重试（HTTP $code）"
    else -> "附件请求失败（HTTP $code）"
}

internal fun validateAttachmentPrefix(fileName: String, contentType: String?, prefix: ByteArray): String? {
    val type = contentType.orEmpty().substringBefore(';').trim().lowercase(Locale.ROOT)
    val text = prefix.toString(Charsets.UTF_8).trimStart().lowercase(Locale.ROOT)
    if (type == "text/html" || type == "application/json" || text.startsWith("<!doctype html") ||
        text.startsWith("<html") || text.startsWith("{") && text.contains("error")
    ) return "服务器返回了登录页或错误页面，未保存为附件"
    val extension = fileName.substringBefore('?').substringAfterLast('.', "").lowercase(Locale.ROOT)
    if (type.startsWith("image/") && extension in setOf("rar", "zip", "7z", "pdf")) {
        return "服务器返回了图片占位内容，未保存为附件"
    }
    val validArchive = when (extension) {
        "rar" -> prefix.startsWithBytes(byteArrayOf(0x52, 0x61, 0x72, 0x21, 0x1a, 0x07))
        "zip" -> prefix.startsWithBytes(byteArrayOf(0x50, 0x4b, 0x03, 0x04)) ||
            prefix.startsWithBytes(byteArrayOf(0x50, 0x4b, 0x05, 0x06)) ||
            prefix.startsWithBytes(byteArrayOf(0x50, 0x4b, 0x07, 0x08))
        "7z" -> prefix.startsWithBytes(byteArrayOf(0x37, 0x7a, 0xbc.toByte(), 0xaf.toByte(), 0x27, 0x1c))
        else -> true
    }
    if (!validArchive) {
        return "服务器返回的内容不是有效的 ${extension.uppercase(Locale.ROOT)} 文件"
    }
    return null
}

internal fun attachmentFileName(suggestedName: String, encodedPath: String): String {
    val fallback = encodedPath.substringAfterLast('/').ifBlank { "NGA_attachment" }
    return suggestedName.trim().ifBlank { fallback }
        .replace(Regex("[\\/:*?\"<>|\\p{Cntrl}]"), "_")
        .trim('.', ' ')
        .take(120)
        .ifBlank { "NGA_attachment" }
}

internal fun attachmentMimeType(contentType: String?, fileName: String): String {
    val server = contentType.orEmpty().substringBefore(';').trim()
    if (server.isNotBlank() && server != "application/octet-stream") return server
    val extension = fileName.substringBefore('?').substringAfterLast('.', "").lowercase(Locale.ROOT)
    return MimeTypeMap.getSingleton().getMimeTypeFromExtension(extension) ?: when (extension) {
        "rar" -> "application/vnd.rar"
        "7z" -> "application/x-7z-compressed"
        else -> "application/octet-stream"
    }
}

private fun ByteArray.startsWithBytes(expected: ByteArray): Boolean =
    size >= expected.size && expected.indices.all { this[it] == expected[it] }
