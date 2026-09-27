package com.aki.lnga.data

import android.content.Context
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

internal class AiConfigStore(context: Context) {
    private val preferences = context.getSharedPreferences("ai_configurations", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun configs(): List<AiConfig> = runCatching {
        json.decodeFromString<List<AiConfig>>(preferences.getString(KEY_CONFIGS, "[]").orEmpty())
    }.getOrDefault(emptyList()).map(AiConfig::withDetectedCapabilities).ensureSingleActive()

    fun upsert(config: AiConfig): List<AiConfig> {
        val current = configs()
        val merged = if (current.any { it.id == config.id }) {
            current.map { if (it.id == config.id) config else it }
        } else {
            current + config
        }
        val next = merged.let { values ->
            if (config.active) values.map { it.copy(active = it.id == config.id) }
            else values.ensureSingleActive()
        }
        save(next)
        return next
    }

    fun setActive(id: String): List<AiConfig> {
        val next = configs().map { it.copy(active = it.id == id) }
        save(next)
        return next
    }

    fun delete(id: String): List<AiConfig> {
        val next = configs().filterNot { it.id == id }.ensureSingleActive()
        save(next)
        return next
    }

    private fun save(configs: List<AiConfig>) {
        preferences.edit().putString(KEY_CONFIGS, json.encodeToString(configs)).apply()
    }

    private companion object {
        const val KEY_CONFIGS = "configs_v1"
    }
}

internal fun List<AiConfig>.ensureSingleActive(): List<AiConfig> {
    if (isEmpty()) return this
    val selected = firstOrNull { it.active }?.id
    return map { it.copy(active = selected != null && it.id == selected) }
}
