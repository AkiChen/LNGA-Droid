package com.aki.lnga.data

internal enum class TopicSortMode {
    NewReply,
    NewPost,
    Hot,
}

internal enum class TopicHotRange(
    val windowSeconds: Long,
    val fetchPages: Int,
) {
    Day(windowSeconds = 24L * 60 * 60, fetchPages = 5),
    Week(windowSeconds = 7L * 24 * 60 * 60, fetchPages = 8),
    Month(windowSeconds = 30L * 24 * 60 * 60, fetchPages = 10),
}

internal fun buildHotTopicList(
    pages: List<com.aki.lnga.model.TopicPage>,
    range: TopicHotRange,
    fallbackNowSeconds: Long,
    limit: Int = 50,
): List<com.aki.lnga.model.Topic> {
    val serverNow = pages.asSequence().map { it.serverTimeSeconds }.firstOrNull { it > 0 }
        ?: fallbackNowSeconds
    return pages.asSequence()
        .flatMap { it.topics.asSequence() }
        .distinctBy { it.tid }
        .filter { topic ->
            topic.postDateSeconds > 0 && serverNow - topic.postDateSeconds <= range.windowSeconds
        }
        .sortedWith(compareByDescending<com.aki.lnga.model.Topic> { it.replies }.thenByDescending { it.lastPostDateSeconds })
        .take(limit)
        .toList()
}
