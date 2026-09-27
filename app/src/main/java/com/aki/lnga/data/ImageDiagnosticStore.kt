package com.aki.lnga.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.aki.lnga.BuildConfig
import java.net.URI
import java.util.concurrent.TimeUnit
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import okhttp3.OkHttpClient
import okhttp3.Request

@Serializable
internal data class ImageDiagnosticEvent(
    val timeMillis: Long,
    val stage: String,
    val purpose: String,
    val safeUrl: String,
    val urlHash: String,
    val detail: String,
)

internal object ImageDiagnosticStore {
    private const val PREFS = "image_diagnostics"
    private const val KEY_EVENTS = "events"
    private const val LIMIT = 100
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()
    private val probeClient = OkHttpClient.Builder()
        .addInterceptor(NgaImageCdnRecoveryInterceptor())
        .addInterceptor(NgaImageProtocolFallbackInterceptor())
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .build()

    fun record(context: Context, stage: String, purpose: String, url: String, detail: String) {
        runCatching {
            val event = ImageDiagnosticEvent(
                timeMillis = System.currentTimeMillis(),
                stage = stage.take(40),
                purpose = purpose.replace(Regex("[\r\n]+"), " ").take(120),
                safeUrl = sanitizedImageUrl(url),
                urlHash = imageUrlHash(url),
                detail = redactDiagnosticText(detail).take(600),
            )
            synchronized(lock) {
                val preferences = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
                val current = decode(preferences.getString(KEY_EVENTS, "[]"))
                preferences.edit().putString(
                    KEY_EVENTS,
                    json.encodeToString((listOf(event) + current).take(LIMIT)),
                ).apply()
            }
        }
    }

    fun events(context: Context): List<ImageDiagnosticEvent> = synchronized(lock) {
        decode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EVENTS, "[]"))
    }

    fun clear(context: Context) {
        synchronized(lock) {
            context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().remove(KEY_EVENTS).apply()
        }
    }

    fun report(context: Context, url: String? = null): String {
        val wantedHash = url?.let(::imageUrlHash)
        val selected = events(context).filter { wantedHash == null || it.urlHash == wantedHash }
        return buildString {
            appendLine("LNGA 图片加载与查看诊断")
            appendLine("credential-values=redacted")
            appendLine(imageDiagnosticEnvironment(context))
            appendLine("filter=${wantedHash ?: "all"} events=${selected.size}")
            selected.forEach { event ->
                appendLine(
                    "${event.timeMillis} stage=${event.stage} purpose=${event.purpose} " +
                        "url=${event.safeUrl} hash=${event.urlHash} ${event.detail}",
                )
            }
        }.trimEnd()
    }

    suspend fun probe(context: Context, url: String, purpose: String) = withContext(Dispatchers.IO) {
        val normalized = normalizeNgaImageUrl(url)
        runCatching {
            val request = normalized.applyFallbackMarker(
                Request.Builder().url(normalized.request)
                    .header("User-Agent", "Mozilla/5.0 (Android) LNGA-Android")
                    .header("Referer", "https://bbs.nga.cn/")
                    .header("Range", "bytes=0-31"),
            ).build()
            probeClient.newCall(request).execute().use { response ->
                val prefixBuffer = ByteArray(16)
                val prefixSize = response.body?.byteStream()?.read(prefixBuffer)?.coerceAtLeast(0) ?: 0
                val prefix = prefixBuffer.copyOf(prefixSize)
                record(
                    context,
                    stage = "probe",
                    purpose = purpose,
                    url = url,
                    detail = "protocolUpgrade=${normalized.upgradedToHttps} hostMigration=${normalized.migratedHost} code=${response.code} " +
                        "protocolFallback=${normalized.upgradedToHttps && response.request.url.scheme == "http"} " +
                        "final=${sanitizedImageUrl(response.request.url.toString())} " +
                        "type=${response.header("Content-Type").orEmpty().substringBefore(';')} " +
                        "length=${response.header("Content-Length").orEmpty()} magic=${prefix.toHex()} " +
                        "cache=${response.header("eo-cache-status").orEmpty()} age=${response.header("age").orEmpty()} " +
                        "requestId=${response.header("x-request-id").orEmpty()}",
                )
            }
        }.onFailure { error ->
            record(
                context,
                stage = "probe-error",
                purpose = purpose,
                url = url,
                detail = "protocolUpgrade=${normalized.upgradedToHttps} type=${error.javaClass.simpleName} " +
                    "message=${error.message.orEmpty()}",
            )
        }
    }

    private fun decode(source: String?): List<ImageDiagnosticEvent> = runCatching {
        json.decodeFromString<List<ImageDiagnosticEvent>>(source.orEmpty())
    }.getOrDefault(emptyList())
}

internal fun sanitizedImageUrl(url: String): String = runCatching {
    val uri = URI(url)
    "${uri.scheme}://${uri.host}${uri.path.orEmpty()}".take(260)
}.getOrElse { "invalid-url" }

internal fun imageUrlHash(url: String): String = url.hashCode().toUInt().toString(16)

internal fun redactDiagnosticText(value: String): String = value
    .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), "[url]")
    .replace(Regex("(?i)(cookie|token|cid|uid)=?[^\\s,;]*"), "$1=[redacted]")
    .replace(Regex("[\r\n]+"), " ")

private fun ByteArray.toHex(): String = joinToString("") { byte -> "%02x".format(byte) }

private fun imageDiagnosticEnvironment(context: Context): String {
    val network = runCatching {
        val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
        val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
        buildList {
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add("wifi")
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("cellular")
            if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) add("vpn")
        }.ifEmpty { listOf("unknown") }.joinToString("+")
    }.getOrDefault("unavailable")
    return "app=${BuildConfig.VERSION_NAME} android=${Build.VERSION.RELEASE}/api${Build.VERSION.SDK_INT} " +
        "device=${Build.MANUFACTURER}/${Build.MODEL} network=$network"
}
