package com.aki.lnga.data

import java.time.Instant
import java.time.ZoneOffset

internal data class UserProfile(
    val uid: Long,
    val userName: String,
    val avatarUrl: String,
    val group: String,
    val title: String,
    val honor: String,
    val reputation: Int,
    val posts: String,
    val registeredAtSeconds: Long,
    val signature: String,
    val ipLocation: String,
    val followingCount: String,
    val followerCount: String,
)

internal data class UserActivityRecord(
    val tid: Long,
    val subject: String,
    val forumName: String,
    val timestampSeconds: Long,
    val replySnippet: String = "",
)

internal data class UserForumStat(val name: String, val topicCount: Int, val replyCount: Int) {
    val total: Int get() = topicCount + replyCount
}

internal data class UserActivityAnalysis(
    val topics: List<UserActivityRecord>,
    val replies: List<UserActivityRecord>,
    val forumStats: List<UserForumStat>,
    val hourDistribution: List<Int>,
    val loadIssues: List<UserActivityLoadIssue> = emptyList(),
) {
    val topicCount: Int get() = topics.size
    val replyCount: Int get() = replies.size
    val isPartial: Boolean get() = loadIssues.isNotEmpty()
}

internal data class UserActivityLoadIssue(
    val section: String,
    val page: Int,
    val message: String,
)

internal fun aggregateUserActivity(
    topics: List<UserActivityRecord>,
    replies: List<UserActivityRecord>,
    loadIssues: List<UserActivityLoadIssue> = emptyList(),
): UserActivityAnalysis {
    val forums = linkedMapOf<String, Pair<Int, Int>>()
    val hours = MutableList(24) { 0 }
    topics.forEach { record ->
        val name = record.forumName.ifBlank { "未知版面" }
        val previous = forums[name] ?: (0 to 0)
        forums[name] = previous.copy(first = previous.first + 1)
        addActivityHour(hours, record.timestampSeconds)
    }
    replies.forEach { record ->
        val name = record.forumName.ifBlank { "未知版面" }
        val previous = forums[name] ?: (0 to 0)
        forums[name] = previous.copy(second = previous.second + 1)
        addActivityHour(hours, record.timestampSeconds)
    }
    return UserActivityAnalysis(
        topics = topics,
        replies = replies,
        forumStats = forums.map { (name, counts) -> UserForumStat(name, counts.first, counts.second) }
            .sortedByDescending(UserForumStat::total),
        hourDistribution = hours,
        loadIssues = loadIssues,
    )
}

private fun addActivityHour(hours: MutableList<Int>, timestampSeconds: Long) {
    if (timestampSeconds <= 0) return
    val hour = Instant.ofEpochSecond(timestampSeconds).atZone(ZoneOffset.ofHours(8)).hour
    hours[hour] = hours[hour] + 1
}

internal fun sanitizeUserReply(raw: String): String = raw
    .replace(Regex("\\[quote(?:=[^]]*)?][\\s\\S]*?\\[/quote]", RegexOption.IGNORE_CASE), " ")
    .replace(Regex("\\[img(?:=[^]]*)?][\\s\\S]*?\\[/img]", RegexOption.IGNORE_CASE), " [图片] ")
    .replace(Regex("\\[flash(?:=[^]]*)?][\\s\\S]*?\\[/flash]", RegexOption.IGNORE_CASE), " [视频] ")
    .replace(Regex("\\[/?[a-z]+(?:=[^]]*)?]", RegexOption.IGNORE_CASE), " ")
    .replace(Regex("\\[s:[^]]+]", RegexOption.IGNORE_CASE), " [表情] ")
    .replace(Regex("<[^>]+>"), " ")
    .replace(Regex("https?://\\S+", RegexOption.IGNORE_CASE), " ")
    .replace(Regex("\\s+"), " ")
    .trim()
    .take(240)
