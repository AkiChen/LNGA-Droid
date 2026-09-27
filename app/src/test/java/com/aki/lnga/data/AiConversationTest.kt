package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.json.Json

class AiConversationTest {
    @Test
    fun contextKeepsNewestMessagesWithinBothLimits() {
        val messages = (1..6).map { index ->
            AiChatMessage(
                id = index.toString(),
                role = if (index % 2 == 0) AiChatRole.Assistant else AiChatRole.User,
                content = index.toString().repeat(10),
                createdAt = index.toLong(),
            )
        }

        val selected = selectAiContext(messages, maxMessages = 3, maxCharacters = 25)

        assertEquals(listOf("4", "5", "6"), selected.messages.map { it.id })
        assertEquals(3, selected.omittedCount)
        assertEquals(25, selected.characterCount)
        assertEquals(5, selected.messages.first().content.length)
    }

    @Test
    fun erroredEmptyMessagesAreNotSentAndTitleUsesFirstUserText() {
        val messages = listOf(
            AiChatMessage("a", AiChatRole.Assistant, "", 1, AiMessageStatus.Error),
            AiChatMessage("u", AiChatRole.User, "  第一条\n提问内容  ", 2),
        )

        assertEquals(listOf("u"), selectAiContext(messages).messages.map { it.id })
        assertEquals("第一条 提问内容", conversationTitle(messages))
    }

    @Test
    fun longSingleMessageIsTruncatedFromTheFront() {
        val selected = selectAiContext(
            listOf(AiChatMessage("u", AiChatRole.User, "abcdefghij", 1)),
            maxCharacters = 4,
        )

        assertEquals("ghij", selected.messages.single().content)
        assertTrue(selected.characterCount <= 4)
    }

    @Test
    fun hiddenSummaryContextIsSentButDoesNotBecomeConversationTitle() {
        val messages = listOf(
            AiChatMessage("s", AiChatRole.System, "帖子原文", 1, contextOnly = true),
            AiChatMessage("u", AiChatRole.User, "请总结这个主题", 2),
        )

        assertEquals(listOf("s", "u"), selectAiContext(messages).messages.map(AiChatMessage::id))
        assertEquals("请总结这个主题", conversationTitle(messages))
    }

    @Test
    fun oldStoredConversationDefaultsToGeneralContext() {
        val legacy = """{"id":"1","title":"旧会话","configId":"c","createdAt":1,"updatedAt":2,"messages":[]}"""

        val conversation = Json { ignoreUnknownKeys = true }.decodeFromString<AiConversation>(legacy)

        assertEquals(AiConversationKind.General, conversation.kind)
        assertEquals(0L, conversation.sourceId)
        assertEquals(false, conversation.webSearchEnabled)
    }

    @Test
    fun topicAndUserConversationsRemainDistinct() {
        val topic = AiConversation("t", "主题", "c", 1, 1, emptyList(), AiConversationKind.TopicSummary, 42)
        val user = AiConversation("u", "用户", "c", 1, 2, emptyList(), AiConversationKind.UserAnalysis, 42)

        assertTrue(topic.kind != user.kind)
        assertEquals(42L, topic.sourceId)
        assertEquals(42L, user.sourceId)
    }

    @Test
    fun contextualConversationKeepsItsRealTitle() {
        val conversation = AiConversation(
            "t",
            "这是需要完整展示的主题标题",
            "c",
            1,
            1,
            listOf(AiChatMessage("u", AiChatRole.User, "请总结主题《旧包装标题》", 1)),
            AiConversationKind.TopicSummary,
            42,
        )

        assertEquals("这是需要完整展示的主题标题", displayConversationTitle(conversation))
    }

    @Test
    fun onlyCompletedNonEmptyAssistantMessageCountsAsGeneratedResult() {
        val base = AiConversation("u", "用户", "c", 1, 1, emptyList(), AiConversationKind.UserAnalysis, 42)

        assertEquals(null, base.copy(messages = listOf(
            AiChatMessage("a", AiChatRole.Assistant, "生成中", 1, AiMessageStatus.Streaming),
        )).completedAssistantText())
        assertEquals(
            "完成的光谱",
            base.copy(messages = listOf(
                AiChatMessage("a", AiChatRole.Assistant, "完成的光谱", 1, AiMessageStatus.Complete),
            )).completedAssistantText(),
        )
    }
}
