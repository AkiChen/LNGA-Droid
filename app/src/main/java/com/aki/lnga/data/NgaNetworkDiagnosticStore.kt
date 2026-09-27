package com.aki.lnga.data

import android.content.Context
import android.net.ConnectivityManager
import android.net.NetworkCapabilities
import android.os.Build
import com.aki.lnga.BuildConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class NgaNetworkDiagnosticEvent(
    val timeMillis: Long,
    val purpose: String,
    val host: String,
    val attempt: Int,
    val stage: String,
    val detail: String,
)

internal object NgaNetworkDiagnosticStore {
    private const val PREFS = "nga_network_diagnostics"
    private const val KEY_EVENTS = "events"
    private const val LIMIT = 100
    private val json = Json { ignoreUnknownKeys = true }
    private val lock = Any()

    fun record(
        context: Context?,
        purpose: String,
        host: String,
        attempt: Int,
        stage: String,
        detail: String,
    ) {
        if (context == null) return
        runCatching {
            val event = NgaNetworkDiagnosticEvent(
                timeMillis = System.currentTimeMillis(),
                purpose = safeField(purpose, 100),
                host = safeField(host, 80),
                attempt = attempt.coerceAtLeast(0),
                stage = safeField(stage, 32),
                detail = redactDiagnosticText(detail).take(400),
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

    fun report(context: Context, purposePrefix: String? = null): String {
        val events = synchronized(lock) {
            decode(context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY_EVENTS, "[]"))
        }.filter { purposePrefix == null || it.purpose.startsWith(purposePrefix) }
        return buildString {
            appendLine("LNGA 网络请求诊断")
            appendLine("credential-values=redacted")
            appendLine(environment(context))
            appendLine("filter=${purposePrefix ?: "all"} events=${events.size}")
            events.forEach { event ->
                appendLine(
                    "${event.timeMillis} purpose=${event.purpose} host=${event.host} " +
                        "attempt=${event.attempt} stage=${event.stage} ${event.detail}",
                )
            }
        }.trimEnd()
    }

    private fun decode(source: String?): List<NgaNetworkDiagnosticEvent> = runCatching {
        json.decodeFromString<List<NgaNetworkDiagnosticEvent>>(source.orEmpty())
    }.getOrDefault(emptyList())

    private fun safeField(value: String, limit: Int): String = value
        .replace(Regex("[\\r\\n=]+"), "-")
        .take(limit)

    private fun environment(context: Context): String {
        val network = runCatching {
            val manager = context.getSystemService(Context.CONNECTIVITY_SERVICE) as ConnectivityManager
            val capabilities = manager.getNetworkCapabilities(manager.activeNetwork)
            buildList {
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_WIFI) == true) add("wifi")
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_CELLULAR) == true) add("cellular")
                if (capabilities?.hasTransport(NetworkCapabilities.TRANSPORT_VPN) == true) add("vpn")
                if (capabilities?.hasCapability(NetworkCapabilities.NET_CAPABILITY_VALIDATED) == true) add("validated")
            }.ifEmpty { listOf("unknown") }.joinToString("+")
        }.getOrDefault("unavailable")
        return "app=${BuildConfig.VERSION_NAME} android=${Build.VERSION.RELEASE}/api${Build.VERSION.SDK_INT} " +
            "device=${Build.MANUFACTURER}/${Build.MODEL} network=$network"
    }
}
