package com.aki.lnga.data

import android.content.Context
import com.aki.lnga.model.Topic
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class BrowseHistoryEntry(
    val tid: Long,
    val fid: Long,
    val subject: String,
    val author: String,
    val authorId: Long,
    val boardName: String,
    val postDateSeconds: Long,
    val visitedAtMillis: Long,
)

internal class BrowseHistoryStore(context: Context) {
    private val preferences = context.getSharedPreferences("browse_history", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun entries(): List<BrowseHistoryEntry> = runCatching {
        json.decodeFromString<List<BrowseHistoryEntry>>(preferences.getString(KEY_ENTRIES, "[]").orEmpty())
    }.getOrDefault(emptyList())

    fun record(topic: Topic, forumName: String): List<BrowseHistoryEntry> {
        val entry = BrowseHistoryEntry(
            tid = topic.tid,
            fid = topic.fid,
            subject = topic.subject,
            author = topic.author,
            authorId = topic.authorId,
            boardName = topic.boardName.ifBlank { forumName },
            postDateSeconds = topic.postDateSeconds,
            visitedAtMillis = System.currentTimeMillis(),
        )
        return mergeHistory(entries(), entry).also(::save)
    }

    fun clear() {
        preferences.edit().remove(KEY_ENTRIES).apply()
    }

    private fun save(entries: List<BrowseHistoryEntry>) {
        preferences.edit().putString(KEY_ENTRIES, json.encodeToString(entries)).apply()
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
    }
}

internal fun mergeHistory(
    current: List<BrowseHistoryEntry>,
    viewed: BrowseHistoryEntry,
    limit: Int = 100,
): List<BrowseHistoryEntry> =
    (listOf(viewed) + current.filterNot { it.tid == viewed.tid }).take(limit)
