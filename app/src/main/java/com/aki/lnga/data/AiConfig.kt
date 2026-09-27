package com.aki.lnga.data

import kotlinx.serialization.Serializable

@Serializable
internal enum class AiCapability {
    ChatCompletions,
    Responses,
    ModelList,
    WebSearch,
}

@Serializable
internal data class AiConfig(
    val id: String,
    val name: String,
    val presetId: String,
    val baseUrl: String,
    val model: String,
    val active: Boolean,
    val capabilities: Set<AiCapability>,
)

internal data class AiProviderPreset(
    val id: String,
    val displayName: String,
    val baseUrl: String,
    val defaultModel: String,
    val capabilities: Set<AiCapability>,
)

internal object AiProviderPresets {
    private val common = setOf(AiCapability.ChatCompletions, AiCapability.ModelList)

    val all: List<AiProviderPreset> = listOf(
        AiProviderPreset(
            id = "openai",
            displayName = "OpenAI",
            baseUrl = "https://api.openai.com/v1",
            defaultModel = "gpt-5-mini",
            capabilities = common + AiCapability.Responses,
        ),
        AiProviderPreset(
            id = "modelgate",
            displayName = "Model Gate",
            baseUrl = "https://api.model-gate.com/v1",
            defaultModel = "gpt-5.5",
            capabilities = common + AiCapability.Responses,
        ),
        AiProviderPreset(
            id = "deepseek",
            displayName = "DeepSeek",
            baseUrl = "https://api.deepseek.com",
            defaultModel = "deepseek-v4-flash",
            capabilities = common,
        ),
        AiProviderPreset(
            id = "zhipu",
            displayName = "智谱 GLM",
            baseUrl = "https://open.bigmodel.cn/api/paas/v4",
            defaultModel = "glm-5",
            capabilities = common,
        ),
        AiProviderPreset(
            id = "doubao",
            displayName = "豆包/火山方舟",
            baseUrl = "https://ark.cn-beijing.volces.com/api/v3",
            defaultModel = "doubao-seed-2-0-lite-260215",
            capabilities = common + AiCapability.Responses,
        ),
        AiProviderPreset(
            id = "minimax",
            displayName = "MiniMax",
            baseUrl = "https://api.minimaxi.com/v1",
            defaultModel = "MiniMax-M2.7",
            capabilities = common,
        ),
        AiProviderPreset(
            id = "kimi",
            displayName = "Kimi/Moonshot",
            baseUrl = "https://api.moonshot.cn/v1",
            defaultModel = "kimi-k2.5",
            capabilities = common,
        ),
        AiProviderPreset(
            id = "custom",
            displayName = "自定义 OpenAI-compatible",
            baseUrl = "",
            defaultModel = "",
            capabilities = common,
        ),
    )

    fun find(id: String): AiProviderPreset = all.firstOrNull { it.id == id } ?: all.last()
}

internal fun normalizeAiBaseUrl(value: String): String = value.trim().trimEnd('/')

internal fun AiConfig.withDetectedCapabilities(): AiConfig {
    // Keep the enum value readable for configurations saved by older versions, but migrate it out.
    return copy(capabilities = capabilities - AiCapability.WebSearch)
}

internal fun cloneAiConfigDraft(source: AiConfig, newId: String): AiConfig = source.copy(
    id = newId,
    name = "${source.name} 副本",
    active = false,
)

internal fun validateAiConfig(name: String, baseUrl: String, model: String, hasApiKey: Boolean): String? {
    if (name.trim().isBlank()) return "请输入配置名称"
    val normalized = normalizeAiBaseUrl(baseUrl)
    if (!normalized.startsWith("https://", true) && !normalized.startsWith("http://", true)) {
        return "服务地址必须以 http:// 或 https:// 开头"
    }
    if (model.trim().isBlank()) return "请输入模型名称"
    if (!hasApiKey) return "请输入 API Key"
    return null
}

/**
 * A conversation keeps using its explicitly selected configuration. The global active
 * configuration is only the default for new or legacy conversations without a selection.
 */
internal fun resolveAiConfig(
    configs: List<AiConfig>,
    keyConfigIds: Set<String>,
    conversationConfigId: String?,
): AiConfig? {
    val available = configs.filter { it.id in keyConfigIds }
    return if (conversationConfigId.isNullOrBlank()) {
        available.firstOrNull(AiConfig::active)
    } else {
        available.firstOrNull { it.id == conversationConfigId }
    }
}
