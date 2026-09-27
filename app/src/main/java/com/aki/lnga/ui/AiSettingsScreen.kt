@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aki.lnga.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.Key
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.unit.dp
import com.aki.lnga.data.AiCapability
import com.aki.lnga.data.AiConfig
import com.aki.lnga.data.AiProviderPresets
import com.aki.lnga.data.cloneAiConfigDraft
import com.aki.lnga.data.validateAiConfig
import java.util.UUID

@Composable
internal fun AiSettingsScreen(
    state: MainUiState,
    onSave: (AiConfig, String, String?) -> Unit,
    onActivate: (String) -> Unit,
    onDelete: (String) -> Unit,
    onDeleteKey: (String) -> Unit,
    onLoadModels: (AiConfig, String) -> Unit,
    onTest: (AiConfig, String) -> Unit,
    onClearStatus: () -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    var editing by remember { mutableStateOf<AiConfig?>(null) }
    var editorId by remember { mutableStateOf("") }
    var showEditor by remember { mutableStateOf(false) }
    var pendingDelete by remember { mutableStateOf<AiConfig?>(null) }
    var pendingKeyDelete by remember { mutableStateOf<AiConfig?>(null) }
    var cloneSourceId by remember { mutableStateOf<String?>(null) }

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("AI 服务配置") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
            )
        },
        floatingActionButton = {
            FloatingActionButton(onClick = {
                editing = null
                cloneSourceId = null
                editorId = UUID.randomUUID().toString()
                showEditor = true
                onClearStatus()
            }) {
                Icon(Icons.Default.Add, contentDescription = "添加 AI 配置")
            }
        },
    ) { padding ->
        LazyColumn(
            modifier = Modifier.padding(padding).fillMaxSize(),
            verticalArrangement = Arrangement.spacedBy(10.dp),
        ) {
            item {
                Column(Modifier.padding(horizontal = 18.dp, vertical = 8.dp)) {
                    Text("API Key 仅以 Android Keystore AES-GCM 加密形式保存在本机。")
                    Text(
                        "连接测试会向所选模型发送一个最多 1 token 的请求，可能产生极少量费用。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            state.aiStatusMessage?.let { message ->
                item {
                    Card(Modifier.padding(horizontal = 14.dp).fillMaxWidth()) {
                        Row(
                            Modifier.padding(14.dp),
                            verticalAlignment = Alignment.CenterVertically,
                            horizontalArrangement = Arrangement.spacedBy(10.dp),
                        ) {
                            if (state.aiBusyConfigId != null) CircularProgressIndicator()
                            Text(
                                message,
                                color = if (state.aiStatusIsError) MaterialTheme.colorScheme.error
                                else MaterialTheme.colorScheme.primary,
                            )
                        }
                    }
                }
            }
            if (state.aiConfigs.isEmpty()) {
                item {
                    Text(
                        "还没有 AI 配置。点击右下角 + 添加服务商。",
                        modifier = Modifier.padding(24.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            }
            items(state.aiConfigs, key = AiConfig::id) { config ->
                val hasKey = config.id in state.aiKeyConfigIds
                AiConfigCard(
                    config = config,
                    hasKey = hasKey,
                    busy = state.aiBusyConfigId == config.id,
                    onActivate = { onActivate(config.id) },
                    onEdit = {
                        editing = config
                        cloneSourceId = null
                        editorId = config.id
                        showEditor = true
                        onClearStatus()
                    },
                    onClone = {
                        cloneSourceId = config.id
                        editorId = UUID.randomUUID().toString()
                        editing = cloneAiConfigDraft(config, editorId)
                        showEditor = true
                        onClearStatus()
                    },
                    onTest = { onTest(config, "") },
                    onDeleteKey = { pendingKeyDelete = config },
                    onDelete = { pendingDelete = config },
                )
            }
        }
    }

    if (showEditor) {
        AiConfigEditorDialog(
            existing = editing,
            configId = editorId,
            isClone = cloneSourceId != null,
            hasStoredKey = (cloneSourceId ?: editing?.id) in state.aiKeyConfigIds,
            models = state.aiModels[cloneSourceId ?: editorId].orEmpty(),
            busy = isAiEditorBusy(editorId, state.aiBusyConfigId),
            status = state.aiStatusMessage,
            statusIsError = state.aiStatusIsError,
            onSave = { config, key ->
                onSave(config, key, cloneSourceId)
                showEditor = false
                cloneSourceId = null
            },
            onLoadModels = onLoadModels,
            onTest = onTest,
            onDismiss = { showEditor = false; cloneSourceId = null; onClearStatus() },
        )
    }
    pendingDelete?.let { config ->
        ConfirmDeleteDialog(
            title = "删除 AI 配置？",
            message = "“${config.name}”及其本机加密 API Key 将被删除。",
            onConfirm = { onDelete(config.id); pendingDelete = null },
            onDismiss = { pendingDelete = null },
        )
    }
    pendingKeyDelete?.let { config ->
        ConfirmDeleteDialog(
            title = "删除 API Key？",
            message = "保留“${config.name}”的服务地址和模型，但删除本机保存的密钥。",
            onConfirm = { onDeleteKey(config.id); pendingKeyDelete = null },
            onDismiss = { pendingKeyDelete = null },
        )
    }
}

@Composable
private fun AiConfigCard(
    config: AiConfig,
    hasKey: Boolean,
    busy: Boolean,
    onActivate: () -> Unit,
    onEdit: () -> Unit,
    onClone: () -> Unit,
    onTest: () -> Unit,
    onDeleteKey: () -> Unit,
    onDelete: () -> Unit,
) {
    Card(Modifier.padding(horizontal = 14.dp).fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(7.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                RadioButton(selected = config.active, onClick = onActivate, enabled = hasKey)
                Column(Modifier.weight(1f)) {
                    Text(config.name, style = MaterialTheme.typography.titleMedium)
                    Text(config.model, style = MaterialTheme.typography.bodyMedium)
                }
                if (config.active) Icon(Icons.Default.CheckCircle, null, tint = MaterialTheme.colorScheme.primary)
            }
            Text(config.baseUrl, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
            Text(
                if (hasKey) "API Key：••••••••（已加密保存）" else "API Key：未设置",
                style = MaterialTheme.typography.bodySmall,
                color = if (hasKey) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.error,
            )
            Text(
                capabilityText(config.capabilities),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            HorizontalDivider()
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.End) {
                TextButton(onClick = onTest, enabled = hasKey && !busy) { Text("测试") }
                IconButton(onClick = onClone) { Icon(Icons.Default.ContentCopy, "复制配置") }
                IconButton(onClick = onEdit) { Icon(Icons.Default.Edit, "编辑") }
                IconButton(onClick = onDeleteKey, enabled = hasKey) { Icon(Icons.Default.Key, "删除 API Key") }
                IconButton(onClick = onDelete) { Icon(Icons.Default.Delete, "删除配置") }
            }
        }
    }
}

@Composable
private fun AiConfigEditorDialog(
    existing: AiConfig?,
    configId: String,
    isClone: Boolean,
    hasStoredKey: Boolean,
    models: List<String>,
    busy: Boolean,
    status: String?,
    statusIsError: Boolean,
    onSave: (AiConfig, String) -> Unit,
    onLoadModels: (AiConfig, String) -> Unit,
    onTest: (AiConfig, String) -> Unit,
    onDismiss: () -> Unit,
) {
    val initialPreset = AiProviderPresets.find(existing?.presetId.orEmpty())
    var presetId by remember(existing?.id) { mutableStateOf(initialPreset.id) }
    var name by remember(existing?.id) { mutableStateOf(existing?.name ?: initialPreset.displayName) }
    var baseUrl by remember(existing?.id) { mutableStateOf(existing?.baseUrl ?: initialPreset.baseUrl) }
    var model by remember(existing?.id) { mutableStateOf(existing?.model ?: initialPreset.defaultModel) }
    var apiKey by remember(existing?.id) { mutableStateOf("") }
    var validationMessage by remember(existing?.id) { mutableStateOf<String?>(null) }
    var presetExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    val selectedPreset = AiProviderPresets.find(presetId)

    fun draft() = AiConfig(
        id = configId,
        name = name,
        presetId = presetId,
        baseUrl = baseUrl,
        model = model,
        active = existing?.active ?: false,
        capabilities = selectedPreset.capabilities - AiCapability.WebSearch,
    )

    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(if (isClone) "复制 AI 配置" else if (existing == null) "添加 AI 配置" else "编辑 AI 配置") },
        text = {
            LazyColumn(
                modifier = Modifier.widthIn(max = 520.dp),
                verticalArrangement = Arrangement.spacedBy(10.dp),
            ) {
                item {
                    ExposedDropdownMenuBox(expanded = presetExpanded, onExpandedChange = { presetExpanded = it }) {
                        OutlinedTextField(
                            value = selectedPreset.displayName,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("服务商预设") },
                            trailingIcon = { ExposedDropdownMenuDefaults.TrailingIcon(presetExpanded) },
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = presetExpanded, onDismissRequest = { presetExpanded = false }) {
                            AiProviderPresets.all.forEach { preset ->
                                DropdownMenuItem(
                                    text = { Text(preset.displayName) },
                                    onClick = {
                                        presetId = preset.id
                                        name = preset.displayName
                                        baseUrl = preset.baseUrl
                                        model = preset.defaultModel
                                        presetExpanded = false
                                    },
                                )
                            }
                        }
                    }
                }
                item { OutlinedTextField(name, { name = it }, label = { Text("配置名称") }, singleLine = true, modifier = Modifier.fillMaxWidth()) }
                item {
                    OutlinedTextField(
                        baseUrl,
                        { baseUrl = it },
                        label = { Text("Base URL") },
                        supportingText = { Text("自建服务可使用 http://；公网服务推荐 HTTPS") },
                        singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Uri),
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                item {
                    ExposedDropdownMenuBox(expanded = modelExpanded, onExpandedChange = { if (models.isNotEmpty()) modelExpanded = it }) {
                        OutlinedTextField(
                            model,
                            { model = it },
                            label = { Text("模型名称") },
                            trailingIcon = { if (models.isNotEmpty()) ExposedDropdownMenuDefaults.TrailingIcon(modelExpanded) },
                            singleLine = true,
                            modifier = Modifier.menuAnchor().fillMaxWidth(),
                        )
                        ExposedDropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                            models.forEach { option ->
                                DropdownMenuItem(text = { Text(option) }, onClick = { model = option; modelExpanded = false })
                            }
                        }
                    }
                }
                item {
                    OutlinedTextField(
                        apiKey,
                        { apiKey = it },
                        label = { Text("API Key") },
                        placeholder = { Text(if (hasStoredKey) "已保存；留空表示不替换" else "请输入 API Key") },
                        visualTransformation = PasswordVisualTransformation(),
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth(),
                    )
                }
                (validationMessage ?: status)?.let {
                    item {
                        Text(
                            it,
                            color = if (validationMessage != null || statusIsError) MaterialTheme.colorScheme.error
                            else MaterialTheme.colorScheme.primary,
                            style = MaterialTheme.typography.bodySmall,
                        )
                    }
                }
                item {
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { onLoadModels(draft(), apiKey) }, enabled = !busy) {
                            Icon(Icons.Default.Refresh, null)
                            Text("读取模型")
                        }
                        OutlinedButton(onClick = { onTest(draft(), apiKey) }, enabled = !busy) { Text("连接测试") }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = {
                    val error = validateAiConfig(name, baseUrl, model, hasStoredKey || apiKey.isNotBlank())
                    if (error != null) {
                        validationMessage = error
                    } else {
                        validationMessage = null
                        onSave(draft(), apiKey)
                        apiKey = ""
                    }
                },
                enabled = !busy,
            ) { Text("保存") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

@Composable
private fun ConfirmDeleteDialog(title: String, message: String, onConfirm: () -> Unit, onDismiss: () -> Unit) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(title) },
        text = { Text(message) },
        confirmButton = { TextButton(onClick = onConfirm) { Text("删除", color = MaterialTheme.colorScheme.error) } },
        dismissButton = { TextButton(onClick = onDismiss) { Text("取消") } },
    )
}

private fun capabilityText(capabilities: Set<AiCapability>): String = buildList {
    if (AiCapability.ChatCompletions in capabilities) add("Chat Completions")
    if (AiCapability.Responses in capabilities) add("Responses")
    if (AiCapability.ModelList in capabilities) add("模型列表")
}.joinToString(" · ")

internal fun isAiEditorBusy(editorId: String, busyConfigId: String?): Boolean =
    editorId.isNotBlank() && busyConfigId != null && editorId == busyConfigId
