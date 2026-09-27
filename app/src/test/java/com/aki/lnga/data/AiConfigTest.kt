package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

class AiConfigTest {
    @Test
    fun presetsContainRequiredProvidersAndCustomOption() {
        val ids = AiProviderPresets.all.map { it.id }

        assertEquals(listOf("openai", "modelgate", "deepseek", "zhipu", "doubao", "minimax", "kimi", "custom"), ids)
        assertTrue(AiProviderPresets.find("openai").capabilities.contains(AiCapability.Responses))
        assertFalse(AiProviderPresets.find("modelgate").capabilities.contains(AiCapability.WebSearch))
        assertFalse(AiProviderPresets.find("deepseek").capabilities.contains(AiCapability.WebSearch))
        assertEquals("custom", AiProviderPresets.find("unknown").id)
    }

    @Test
    fun validationAcceptsLocalHttpAndRejectsMissingSecret() {
        assertNull(validateAiConfig("Local", "http://192.168.1.2:11434/v1/", "model", true))
        assertEquals("请输入 API Key", validateAiConfig("Local", "https://example.com/v1", "model", false))
        assertEquals("https://example.com/v1", normalizeAiBaseUrl(" https://example.com/v1/ "))
    }

    @Test
    fun existingConfigurationDropsLegacySearchCapability() {
        val detected = config("mg", active = true).copy(
            baseUrl = "https://api.model-gate.com/v1/",
            capabilities = setOf(AiCapability.ChatCompletions, AiCapability.WebSearch),
        ).withDetectedCapabilities()

        assertTrue(AiCapability.ChatCompletions in detected.capabilities)
        assertFalse(AiCapability.WebSearch in detected.capabilities)
    }

    @Test
    fun clonedConfigurationKeepsConnectionButGetsNewIdentityAndIsInactive() {
        val source = config("source", active = true).copy(name = "ModelGate", model = "DeepSeek-V4-Flash")

        val clone = cloneAiConfigDraft(source, "clone")

        assertEquals("clone", clone.id)
        assertEquals("ModelGate 副本", clone.name)
        assertEquals(source.baseUrl, clone.baseUrl)
        assertEquals(source.model, clone.model)
        assertFalse(clone.active)
    }

    @Test
    fun activeConfigurationIsDeterministic() {
        val first = config("1", active = true)
        val second = config("2", active = true)

        val normalized = listOf(first, second).ensureSingleActive()

        assertEquals(listOf(true, false), normalized.map { it.active })
        assertEquals(listOf(false, false), listOf(first.copy(active = false), second.copy(active = false)).ensureSingleActive().map { it.active })
    }

    @Test
    fun serializedMetadataCannotContainAnApiKeyField() {
        val encoded = Json.encodeToString(config("1", active = true))

        assertFalse(encoded.contains("apiKey", ignoreCase = true))
        assertFalse(encoded.contains("secret", ignoreCase = true))
    }

    @Test
    fun conversationSelectionOverridesGlobalActiveConfiguration() {
        val active = config("active", active = true)
        val selected = config("selected", active = false)

        assertEquals(
            selected,
            resolveAiConfig(listOf(active, selected), setOf(active.id, selected.id), selected.id),
        )
        assertEquals(active, resolveAiConfig(listOf(active, selected), setOf(active.id, selected.id), ""))
    }

    @Test
    fun missingConversationConfigurationDoesNotSilentlyFallBack() {
        val active = config("active", active = true)

        assertNull(resolveAiConfig(listOf(active), setOf(active.id), "deleted"))
    }

    private fun config(id: String, active: Boolean) = AiConfig(
        id = id,
        name = id,
        presetId = "custom",
        baseUrl = "https://example.com/v1",
        model = "model",
        active = active,
        capabilities = setOf(AiCapability.ChatCompletions),
    )
}
