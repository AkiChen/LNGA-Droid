package com.aki.lnga.ui

import android.webkit.CookieManager
import android.webkit.WebStorage
import java.util.concurrent.atomic.AtomicInteger

internal val NGA_WEB_ORIGINS = listOf(
    "https://bbs.nga.cn" to ".nga.cn",
    "https://ngabbs.com" to ".ngabbs.com",
    "https://nga.178.com" to ".178.com",
)

/** Clears only NGA passport cookies and NGA web storage; app history and preferences are untouched. */
internal fun clearNgaWebSession(onComplete: () -> Unit) {
    val cookieManager = CookieManager.getInstance()
    val cookieNames = buildSet {
        add("ngaPassportUid")
        add("ngaPassportCid")
        NGA_WEB_ORIGINS.forEach { (origin, _) ->
            cookieManager.getCookie(origin).orEmpty().split(';').forEach { part ->
                part.substringBefore('=').trim().takeIf { it.startsWith("ngaPassport") }?.let(::add)
            }
        }
    }
    val operations = NGA_WEB_ORIGINS.size * cookieNames.size * 2
    if (operations == 0) {
        finishNgaWebSessionClear(cookieManager, onComplete)
        return
    }
    val remaining = AtomicInteger(operations)
    fun completedOne() {
        if (remaining.decrementAndGet() == 0) finishNgaWebSessionClear(cookieManager, onComplete)
    }
    NGA_WEB_ORIGINS.forEach { (origin, parentDomain) ->
        cookieNames.forEach { name ->
            val expiry = "$name=; Path=/; Max-Age=0; Expires=Thu, 01 Jan 1970 00:00:00 GMT; Secure"
            cookieManager.setCookie(origin, expiry) { completedOne() }
            cookieManager.setCookie(origin, "$expiry; Domain=$parentDomain") { completedOne() }
        }
    }
}

internal fun ngaCookiePresence(): String {
    val manager = CookieManager.getInstance()
    return NGA_WEB_ORIGINS.joinToString { (origin, _) ->
        val names = manager.getCookie(origin).orEmpty().split(';')
            .map { it.substringBefore('=').trim() }
            .filter { it == "ngaPassportUid" || it == "ngaPassportCid" }
            .toSet()
        "${origin.substringAfter("https://")}:uid=${"ngaPassportUid" in names},cid=${"ngaPassportCid" in names}"
    }
}

internal fun hasPartialNgaPassportCookies(): Boolean {
    val manager = CookieManager.getInstance()
    return NGA_WEB_ORIGINS.any { (origin, _) ->
        val names = manager.getCookie(origin).orEmpty().split(';')
            .map { it.substringBefore('=').trim() }
            .toSet()
        ("ngaPassportUid" in names) xor ("ngaPassportCid" in names)
    }
}

private fun finishNgaWebSessionClear(manager: CookieManager, onComplete: () -> Unit) {
    manager.flush()
    NGA_WEB_ORIGINS.forEach { (origin, _) -> WebStorage.getInstance().deleteOrigin(origin) }
    onComplete()
}
