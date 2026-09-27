package com.aki.lnga.data

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.contentOrNull
import kotlinx.serialization.json.doubleOrNull
import kotlinx.serialization.json.jsonObject
import kotlinx.serialization.json.longOrNull

internal object NgaJson {
    private val json = Json { ignoreUnknownKeys = true; isLenient = true }

    fun parseElement(raw: String): JsonElement {
        val scriptValue = extractScriptStoreJson(raw)
        return json.parseToJsonElement(sanitize(scriptValue ?: raw))
    }

    fun parse(raw: String): JsonObject = parseElement(raw).jsonObject

    /**
     * Extracts NGA's script-wrapped JSON without relying on a regular expression.
     * Android's on-device regex parser rejects the previous pattern, and a balanced
     * scanner also correctly handles nested objects and braces inside JSON strings.
     */
    internal fun extractScriptStoreJson(raw: String): String? {
        val marker = "window.script_muti_get_var_store"
        val markerIndex = raw.indexOf(marker)
        if (markerIndex < 0) return null
        val assignmentIndex = raw.indexOf('=', markerIndex + marker.length)
        if (assignmentIndex < 0) return null
        val jsonStart = raw.indexOf('{', assignmentIndex + 1)
        if (jsonStart < 0) return null

        var depth = 0
        var inString = false
        var escaped = false
        for (index in jsonStart until raw.length) {
            val character = raw[index]
            if (inString) {
                when {
                    escaped -> escaped = false
                    character == '\\' -> escaped = true
                    character == '"' -> inString = false
                }
                continue
            }
            when (character) {
                '"' -> inString = true
                '{' -> depth++
                '}' -> {
                    depth--
                    if (depth == 0) return raw.substring(jsonStart, index + 1)
                    if (depth < 0) return null
                }
            }
        }
        return null
    }

    fun sanitize(raw: String): String {
        var text = raw
            .replace("window.script_muti_get_var_store=", "")
            .replace("/*\$js\$*/", "")
            .replace("/*error fill content*/", "")
            .replace(Regex("\"content\":\\+(\\d+),"), "\"content\":\"+\$1\",")
            .replace(Regex("\"subject\":\\+(\\d+),"), "\"subject\":\"+\$1\",")
            .replace(Regex("\"(content|subject|author)\":(0\\d+(?:\\.\\d+)?)([,}])"), "\"\$1\":\"\$2\"\$3")

        val result = StringBuilder(text.length)
        var inString = false
        var escaped = false
        text.forEach { character ->
            when {
                escaped -> {
                    result.append(character)
                    escaped = false
                }
                character == '\\' -> {
                    result.append(character)
                    escaped = true
                }
                character == '"' -> {
                    inString = !inString
                    result.append(character)
                }
                inString && character.code < 0x20 -> when (character) {
                    '\t' -> result.append("\\t")
                    '\n' -> result.append("\\n")
                    '\r' -> result.append("\\r")
                    else -> result.append("\\u%04x".format(character.code))
                }
                else -> result.append(character)
            }
        }
        return result.toString().trim().removeSuffix(";")
    }
}

internal fun JsonObject.objectOrNull(key: String): JsonObject? = this[key] as? JsonObject

internal fun JsonElement?.text(default: String = ""): String =
    (this as? JsonPrimitive)?.contentOrNull ?: default

internal fun JsonElement?.long(default: Long = 0): Long {
    val primitive = this as? JsonPrimitive ?: return default
    return primitive.longOrNull ?: primitive.doubleOrNull?.toLong() ?: primitive.contentOrNull?.toLongOrNull() ?: default
}

internal fun JsonElement?.int(default: Int = 0): Int = long(default.toLong()).toInt()

internal fun JsonElement?.bool(default: Boolean = false): Boolean {
    val primitive = this as? JsonPrimitive ?: return default
    return primitive.booleanOrNull ?: (primitive.intOrNullCompat() != 0)
}

private fun JsonPrimitive.intOrNullCompat(): Int = contentOrNull?.toIntOrNull() ?: 0
