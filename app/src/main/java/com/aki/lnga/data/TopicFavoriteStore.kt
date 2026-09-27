package com.aki.lnga.data

import android.content.Context
import com.aki.lnga.model.Topic
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal data class TopicFavoriteEntry(
    val tid: Long,
    val fid: Long,
    val subject: String,
    val author: String,
    val authorId: Long,
    val boardName: String,
    val postDateSeconds: Long,
    val savedAtMillis: Long,
)

internal class TopicFavoriteStore(context: Context) {
    private val preferences = context.getSharedPreferences("topic_favorites", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun entries(): List<TopicFavoriteEntry> = runCatching {
        json.decodeFromString<List<TopicFavoriteEntry>>(preferences.getString(KEY_ENTRIES, "[]").orEmpty())
    }.getOrDefault(emptyList())

    fun toggle(topic: Topic, forumName: String): List<TopicFavoriteEntry> {
        val current = entries()
        val updated = if (current.any { it.tid == topic.tid }) {
            current.filterNot { it.tid == topic.tid }
        } else {
            addFavorite(current, topic.toFavoriteEntry(forumName))
        }
        return updated.also(::save)
    }

    fun remove(tid: Long): List<TopicFavoriteEntry> =
        entries().filterNot { it.tid == tid }.also(::save)

    private fun save(entries: List<TopicFavoriteEntry>) {
        preferences.edit().putString(KEY_ENTRIES, json.encodeToString(entries)).apply()
    }

    private companion object {
        const val KEY_ENTRIES = "entries"
    }
}

internal fun addFavorite(
    current: List<TopicFavoriteEntry>,
    favorite: TopicFavoriteEntry,
    limit: Int = 500,
): List<TopicFavoriteEntry> =
    (listOf(favorite) + current.filterNot { it.tid == favorite.tid }).take(limit)

private fun Topic.toFavoriteEntry(forumName: String) = TopicFavoriteEntry(
    tid = tid,
    fid = fid,
    subject = subject,
    author = author,
    authorId = authorId,
    boardName = boardName.ifBlank { forumName },
    postDateSeconds = postDateSeconds,
    savedAtMillis = System.currentTimeMillis(),
)
