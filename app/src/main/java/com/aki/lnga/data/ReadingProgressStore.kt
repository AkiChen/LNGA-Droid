package com.aki.lnga.data

import android.content.Context
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class ReadingProgressEntry(
    val uid: String,
    val tid: Long,
    val page: Int = 1,
    val floor: Int = 0,
    val updatedAtMillis: Long,
)

internal class ReadingProgressStore(context: Context) {
    private val preferences = context.getSharedPreferences("reading_progress", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun entries(uid: String): List<ReadingProgressEntry> = allEntries().filter { it.uid == uid }

    fun entry(uid: String, tid: Long): ReadingProgressEntry? =
        allEntries().firstOrNull { it.uid == uid && it.tid == tid }

    fun markViewed(
        uid: String,
        tid: Long,
        page: Int = 1,
        floor: Int = 0,
        now: Long = System.currentTimeMillis(),
    ): List<ReadingProgressEntry> {
        val current = allEntries()
        val existing = current.firstOrNull { it.uid == uid && it.tid == tid }
        val viewed = existing?.copy(updatedAtMillis = now)
            ?: ReadingProgressEntry(uid, tid, page.coerceAtLeast(1), floor.coerceAtLeast(0), now)
        val merged = mergeReadingProgress(current, viewed)
        save(merged)
        return merged.filter { it.uid == uid }
    }

    fun record(
        uid: String,
        tid: Long,
        page: Int,
        floor: Int,
        now: Long = System.currentTimeMillis(),
    ): List<ReadingProgressEntry> {
        val merged = mergeReadingProgress(
            allEntries(),
            ReadingProgressEntry(uid, tid, page.coerceAtLeast(1), floor.coerceAtLeast(0), now),
        )
        save(merged)
        return merged.filter { it.uid == uid }
    }

    fun seedFromHistory(uid: String, history: List<BrowseHistoryEntry>): List<ReadingProgressEntry> {
        if (preferences.getBoolean(KEY_HISTORY_MIGRATED, false)) return entries(uid)
        var merged = allEntries()
        val existingIds = merged.asSequence().filter { it.uid == uid }.map { it.tid }.toMutableSet()
        history.asReversed().forEach { item ->
            if (existingIds.add(item.tid)) {
                merged = mergeReadingProgress(
                    merged,
                    ReadingProgressEntry(uid, item.tid, updatedAtMillis = item.visitedAtMillis),
                )
            }
        }
        preferences.edit()
            .putString(KEY_ENTRIES, json.encodeToString(merged))
            .putBoolean(KEY_HISTORY_MIGRATED, true)
            .apply()
        return merged.filter { it.uid == uid }
    }

    private fun allEntries(): List<ReadingProgressEntry> = runCatching {
        json.decodeFromString<List<ReadingProgressEntry>>(preferences.getString(KEY_ENTRIES, "[]").orEmpty())
    }.getOrDefault(emptyList())

    private fun save(entries: List<ReadingProgressEntry>) {
        preferences.edit().putString(KEY_ENTRIES, json.encodeToString(entries)).apply()
    }

    private companion object {
        const val KEY_ENTRIES = "entries_v1"
        const val KEY_HISTORY_MIGRATED = "legacy_history_migrated_v1"
    }
}

internal fun mergeReadingProgress(
    current: List<ReadingProgressEntry>,
    viewed: ReadingProgressEntry,
    limit: Int = 2_000,
): List<ReadingProgressEntry> =
    (listOf(viewed) + current.filterNot { it.uid == viewed.uid && it.tid == viewed.tid })
        .sortedByDescending(ReadingProgressEntry::updatedAtMillis)
        .take(limit)
