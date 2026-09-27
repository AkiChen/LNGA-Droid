package com.aki.lnga.data

import android.content.Context
import java.util.UUID
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

@Serializable
internal enum class AiChatRole { System, User, Assistant }

@Serializable
internal enum class AiMessageStatus { Complete, Streaming, Stopped, Error }

@Serializable
internal enum class AiConversationKind { General, TopicSummary, UserAnalysis }

@Serializable
internal data class AiChatMessage(
    val id: String,
    val role: AiChatRole,
    val content: String,
    val createdAt: Long,
    val status: AiMessageStatus = AiMessageStatus.Complete,
    val contextOnly: Boolean = false,
)

@Serializable
internal data class AiConversation(
    val id: String,
    val title: String,
    val configId: String,
    val createdAt: Long,
    val updatedAt: Long,
    val messages: List<AiChatMessage>,
    val kind: AiConversationKind = AiConversationKind.General,
    val sourceId: Long = 0,
    val webSearchEnabled: Boolean = false,
)

internal fun AiConversation.completedAssistantText(): String? = messages
    .lastOrNull {
        it.role == AiChatRole.Assistant &&
            it.status == AiMessageStatus.Complete &&
            it.content.isNotBlank()
    }
    ?.content

internal data class AiContextSelection(
    val messages: List<AiChatMessage>,
    val omittedCount: Int,
    val characterCount: Int,
)

internal class AiConversationStore(context: Context) {
    private val preferences = context.getSharedPreferences("ai_conversations", Context.MODE_PRIVATE)
    private val json = Json { ignoreUnknownKeys = true }

    fun conversations(): List<AiConversation> = runCatching {
        json.decodeFromString<List<AiConversation>>(preferences.getString(KEY_CONVERSATIONS, "[]").orEmpty())
    }.getOrDefault(emptyList())
        .map { it.copy(webSearchEnabled = false) }
        .sortedByDescending(AiConversation::updatedAt)

    fun create(configId: String, now: Long = System.currentTimeMillis()): AiConversation = AiConversation(
        id = UUID.randomUUID().toString(),
        title = "新对话",
        configId = configId,
        createdAt = now,
        updatedAt = now,
        messages = emptyList(),
    ).also(::save)

    fun save(conversation: AiConversation): List<AiConversation> {
        val sanitized = conversation.copy(
            title = conversation.title.take(MAX_TITLE_CHARS),
            webSearchEnabled = false,
            messages = conversation.messages.takeLast(MAX_STORED_MESSAGES).map { message ->
                message.copy(
                    content = message.content.take(MAX_STORED_MESSAGE_CHARS),
                    status = if (message.status == AiMessageStatus.Streaming) AiMessageStatus.Stopped else message.status,
                )
            },
        )
        val next = (conversations().filterNot { it.id == sanitized.id } + sanitized)
            .sortedByDescending(AiConversation::updatedAt)
            .take(MAX_CONVERSATIONS)
        preferences.edit().putString(KEY_CONVERSATIONS, json.encodeToString(next)).apply()
        return next
    }

    fun delete(id: String): List<AiConversation> {
        val next = conversations().filterNot { it.id == id }
        preferences.edit().putString(KEY_CONVERSATIONS, json.encodeToString(next)).apply()
        return next
    }

    fun deleteBySource(kind: AiConversationKind, sourceId: Long): List<AiConversation> {
        val next = conversations().filterNot { it.kind == kind && it.sourceId == sourceId }
        preferences.edit().putString(KEY_CONVERSATIONS, json.encodeToString(next)).apply()
        return next
    }

    private companion object {
        const val KEY_CONVERSATIONS = "conversations_v1"
        const val MAX_CONVERSATIONS = 30
        const val MAX_STORED_MESSAGES = 100
        const val MAX_STORED_MESSAGE_CHARS = 100_000
        const val MAX_TITLE_CHARS = 80
    }
}

internal fun selectAiContext(
    messages: List<AiChatMessage>,
    maxMessages: Int = 24,
    maxCharacters: Int = 40_000,
): AiContextSelection {
    val eligible = messages.filter { it.content.isNotBlank() && it.status != AiMessageStatus.Error }
    val selected = ArrayDeque<AiChatMessage>()
    var characters = 0
    for (message in eligible.asReversed()) {
        if (selected.size >= maxMessages) break
        val remaining = maxCharacters - characters
        if (remaining <= 0) break
        val content = if (message.content.length > remaining) message.content.takeLast(remaining) else message.content
        selected.addFirst(message.copy(content = content))
        characters += content.length
        if (content.length < message.content.length) break
    }
    return AiContextSelection(
        messages = selected.toList(),
        omittedCount = eligible.size - selected.size,
        characterCount = characters,
    )
}

internal fun conversationTitle(messages: List<AiChatMessage>): String = messages
    .firstOrNull { it.role == AiChatRole.User && !it.contextOnly }
    ?.content
    ?.replace(Regex("\\s+"), " ")
    ?.trim()
    ?.take(30)
    ?.ifBlank { "新对话" }
    ?: "新对话"

internal fun displayConversationTitle(conversation: AiConversation): String =
    if (conversation.kind == AiConversationKind.General) conversationTitle(conversation.messages)
    else conversation.title.trim().ifBlank { conversationTitle(conversation.messages) }
