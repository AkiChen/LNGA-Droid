package com.aki.lnga.ui

import com.aki.lnga.data.AiConversation
import com.aki.lnga.data.AiConversationKind
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AiNavigationTest {
    private val oldSummary = AiConversation(
        id = "old",
        title = "旧摘要",
        configId = "config",
        createdAt = 1,
        updatedAt = 1,
        messages = emptyList(),
        kind = AiConversationKind.TopicSummary,
        sourceId = 42,
    )

    @Test
    fun preparingRegenerationDoesNotExposeOldConversation() {
        val state = MainUiState(
            aiConversations = listOf(oldSummary),
            aiCurrentConversationId = "old",
            aiTaskKind = AiConversationKind.TopicSummary,
            aiTaskSourceId = 42,
            aiTaskPhase = AiTaskPhase.Preparing,
            aiTaskConversationId = null,
        )

        assertNull(resolveContextConversationId(state, AiConversationKind.TopicSummary, 42))
        assertNull(resolveMostRecentAiConversationId(state))
    }

    @Test
    fun streamingTaskAndCompletedContextOpenTheirBoundConversation() {
        val streaming = MainUiState(
            aiConversations = listOf(oldSummary),
            aiTaskKind = AiConversationKind.TopicSummary,
            aiTaskSourceId = 42,
            aiTaskPhase = AiTaskPhase.Streaming,
            aiTaskConversationId = "new",
        )
        val completed = MainUiState(aiConversations = listOf(oldSummary))

        assertEquals("new", resolveContextConversationId(streaming, AiConversationKind.TopicSummary, 42))
        assertEquals("old", resolveContextConversationId(completed, AiConversationKind.TopicSummary, 42))
    }
}
