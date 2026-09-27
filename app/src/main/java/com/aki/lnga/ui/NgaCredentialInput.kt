package com.aki.lnga.ui

import com.aki.lnga.model.NgaSession

/** Accepts either raw values or cookie fragments copied from browser developer tools. */
internal fun normalizeNgaSessionInput(uidInput: String, cidInput: String): NgaSession? {
    val combined = "$uidInput;$cidInput"
    val uid = extractCookieValue(combined, "ngaPassportUid")
        ?: cleanCredentialValue(uidInput, "ngaPassportUid")
    val cid = extractCookieValue(combined, "ngaPassportCid")
        ?: cleanCredentialValue(cidInput, "ngaPassportCid")

    if (uid.isBlank() || cid.isBlank() || uid.any { !it.isDigit() }) return null
    return NgaSession(uid, cid)
}

private fun extractCookieValue(source: String, name: String): String? = source
    .split(';', '\n', '\r')
    .firstNotNullOfOrNull { part ->
        val separator = part.indexOf('=')
        if (separator < 0 || !part.substring(0, separator).trim().equals(name, ignoreCase = true)) {
            null
        } else {
            part.substring(separator + 1).trim().trimMatchingQuotes().ifBlank { null }
        }
    }

private fun cleanCredentialValue(source: String, name: String): String {
    var value = source.trim()
    val separator = value.indexOf('=')
    if (separator >= 0 && value.substring(0, separator).trim().equals(name, ignoreCase = true)) {
        value = value.substring(separator + 1)
    }
    return value.substringBefore(';').trim().trimMatchingQuotes()
}

private fun String.trimMatchingQuotes(): String {
    if (length >= 2 && ((first() == '"' && last() == '"') || (first() == '\'' && last() == '\''))) {
        return substring(1, length - 1).trim()
    }
    return this
}
