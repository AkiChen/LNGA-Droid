@file:OptIn(
    androidx.compose.material3.ExperimentalMaterial3Api::class,
    androidx.compose.foundation.ExperimentalFoundationApi::class,
)

package com.aki.lnga.ui

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.MarqueeSpacing
import androidx.compose.foundation.background
import androidx.compose.foundation.basicMarquee
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.lazy.rememberLazyListState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.ClickableText
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.selection.SelectionContainer
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.Send
import androidx.compose.material.icons.filled.AddComment
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Settings
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.FilledIconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.HorizontalDivider
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.style.TextDecoration
import androidx.compose.ui.unit.dp
import com.aki.lnga.data.AiChatMessage
import com.aki.lnga.data.AiChatRole
import com.aki.lnga.data.AiConversation
import com.aki.lnga.data.AiMessageStatus
import com.aki.lnga.data.resolveAiConfig

@Composable
internal fun AiChatScreen(
    state: MainUiState,
    onSend: (String) -> Unit,
    onStop: () -> Unit,
    onRetry: () -> Unit,
    onNewConversation: () -> Unit,
    onSelectConversation: (String) -> Unit,
    onDeleteConversation: (String) -> Unit,
    onSelectConfig: (String) -> Unit,
    onOpenSettings: () -> Unit,
    onOpenLink: (String) -> Unit,
    onBack: () -> Unit,
) {
    BackHandler(onBack = onBack)
    val conversation = state.aiConversations.firstOrNull { it.id == state.aiCurrentConversationId }
    val preparing = state.aiSummaryRunning && state.aiTaskPhase == AiTaskPhase.Preparing
    val selectedConfig = resolveAiConfig(state.aiConfigs, state.aiKeyConfigIds, conversation?.configId)
    val availableConfigs = state.aiConfigs.filter { it.id in state.aiKeyConfigIds }
    val currentConversationStreaming = state.aiStreaming && conversation?.id == state.aiTaskConversationId
    val anotherConversationStreaming = state.aiStreaming && !currentConversationStreaming
    var input by remember(conversation?.id) { mutableStateOf("") }
    var historyExpanded by remember { mutableStateOf(false) }
    var modelExpanded by remember { mutableStateOf(false) }
    var titleMenuExpanded by remember { mutableStateOf(false) }
    var confirmDelete by remember { mutableStateOf(false) }
    val listState = rememberLazyListState()
    val lastLength = conversation?.messages?.lastOrNull()?.content?.length ?: 0
    LaunchedEffect(conversation?.id, conversation?.messages?.size, lastLength / 48) {
        if (conversation?.messages?.isNotEmpty() == true) listState.scrollToItem(conversation.messages.lastIndex)
    }

    Scaffold(
        modifier = Modifier.edgeSwipeBack(onBack),
        topBar = {
            TopAppBar(
                title = {
                    Box {
                        Text(
                            if (preparing) state.aiTaskTitle.ifBlank { "正在准备 AI 任务" } else conversation?.title ?: "AI 对话",
                            modifier = Modifier.fillMaxWidth()
                                .combinedClickable(
                                    enabled = !preparing && !state.aiStreaming,
                                    onClick = {},
                                    onLongClick = { titleMenuExpanded = true },
                                )
                                .basicMarquee(
                                    iterations = Int.MAX_VALUE,
                                    repeatDelayMillis = 1_200,
                                    initialDelayMillis = 1_500,
                                    spacing = MarqueeSpacing(32.dp),
                                    velocity = 18.dp,
                                ),
                            maxLines = 1,
                            softWrap = false,
                        )
                        DropdownMenu(expanded = titleMenuExpanded, onDismissRequest = { titleMenuExpanded = false }) {
                            DropdownMenuItem(
                                text = { Text("新建会话") },
                                leadingIcon = { Icon(Icons.Default.AddComment, null) },
                                onClick = {
                                    titleMenuExpanded = false
                                    onNewConversation()
                                },
                            )
                            DropdownMenuItem(
                                text = { Text("删除当前会话") },
                                leadingIcon = { Icon(Icons.Default.Delete, null) },
                                enabled = conversation != null,
                                onClick = {
                                    titleMenuExpanded = false
                                    confirmDelete = true
                                },
                            )
                        }
                    }
                },
                actions = {
                    if (!preparing) Box {
                        IconButton(
                            onClick = { modelExpanded = true },
                            enabled = !state.aiStreaming && availableConfigs.isNotEmpty(),
                        ) { Icon(Icons.Default.Tune, "切换模型") }
                        DropdownMenu(expanded = modelExpanded, onDismissRequest = { modelExpanded = false }) {
                            if (availableConfigs.isEmpty()) {
                                DropdownMenuItem(text = { Text("暂无包含 API Key 的配置") }, onClick = {})
                            }
                            availableConfigs.forEach { config ->
                                DropdownMenuItem(
                                    text = {
                                        Text(config.model, maxLines = 1, modifier = Modifier.widthIn(max = 240.dp))
                                    },
                                    trailingIcon = if (config.id == selectedConfig?.id) {
                                        { Text("当前", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.primary) }
                                    } else null,
                                    onClick = {
                                        onSelectConfig(config.id)
                                        modelExpanded = false
                                    },
                                )
                            }
                        }
                    }
                    if (!preparing) Box {
                        IconButton(onClick = { historyExpanded = true }) { Icon(Icons.Default.History, "本地会话") }
                        DropdownMenu(expanded = historyExpanded, onDismissRequest = { historyExpanded = false }) {
                            if (state.aiConversations.isEmpty()) DropdownMenuItem(text = { Text("暂无历史会话") }, onClick = {})
                            state.aiConversations.forEach { item ->
                                DropdownMenuItem(
                                    text = { Text(item.title, maxLines = 1, modifier = Modifier.widthIn(max = 220.dp)) },
                                    onClick = { onSelectConversation(item.id); historyExpanded = false },
                                    trailingIcon = {
                                        IconButton(onClick = { onDeleteConversation(item.id) }) {
                                            Icon(Icons.Default.Delete, "删除会话")
                                        }
                                    },
                                )
                            }
                        }
                    }
                },
            )
        },
    ) { padding ->
        if (preparing) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(
                    Modifier.padding(28.dp),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(14.dp),
                ) {
                    PrismaticAiIcon(
                        color = Color(0xFF75C9FF),
                        animated = true,
                        modifier = Modifier.size(64.dp),
                    )
                    CircularProgressIndicator()
                    Text(state.aiTaskTitle.ifBlank { "正在准备 AI 任务" }, style = MaterialTheme.typography.titleMedium)
                    Text(
                        state.aiSummaryMessage.ifBlank { "正在准备发送给 AI 的内容…" },
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    TextButton(onClick = onStop) {
                        Icon(Icons.Default.Stop, null)
                        Text("停止任务")
                    }
                }
            }
        } else if (selectedConfig == null) {
            Box(Modifier.padding(padding).fillMaxSize(), contentAlignment = Alignment.Center) {
                Card(Modifier.padding(24.dp)) {
                    Column(Modifier.padding(20.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                        val unavailableSelection = !conversation?.configId.isNullOrBlank()
                        Text(if (unavailableSelection) "当前会话的 AI 配置不可用" else "尚未启用可用的 AI 配置")
                        Text(
                            if (unavailableSelection) "请使用右上角按钮切换模型，或前往 AI 设置修复配置。"
                            else "请先保存 API Key 并启用一个服务。",
                            style = MaterialTheme.typography.bodySmall,
                        )
                        Button(onClick = onOpenSettings, modifier = Modifier.padding(top = 12.dp)) {
                            Icon(Icons.Default.Settings, null)
                            Text("打开 AI 设置")
                        }
                    }
                }
            }
        } else {
            Column(
                modifier = Modifier.padding(padding).fillMaxSize().imePadding(),
            ) {
                LazyColumn(
                    state = listState,
                    modifier = Modifier.weight(1f).fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                ) {
                if (conversation == null || conversation.messages.isEmpty()) {
                    item {
                        Text(
                            "开始一个新对话。消息历史只保存在本机；发送前会显示本次上下文范围。",
                            modifier = Modifier.padding(24.dp),
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                conversation?.let { current ->
                    items(current.messages.filterNot(AiChatMessage::contextOnly), key = AiChatMessage::id) { message ->
                        AiMessageBubble(message, onOpenLink)
                    }
                }
                state.aiChatError?.let { error ->
                    item {
                        Card(Modifier.padding(horizontal = 12.dp).fillMaxWidth()) {
                            Column(Modifier.padding(14.dp)) {
                                Text(error, color = MaterialTheme.colorScheme.error)
                                TextButton(onClick = onRetry, enabled = !state.aiStreaming) {
                                    Icon(Icons.Default.Refresh, null)
                                    Text("重新生成")
                                }
                            }
                        }
                    }
                }
                }
                AiChatInputBar(
                    input = input,
                    onInputChange = { input = it.take(12_000) },
                    contextMessage = state.aiContextMessage,
                    currentConversationStreaming = currentConversationStreaming,
                    anotherConversationStreaming = anotherConversationStreaming,
                    onSend = {
                        if (input.isNotBlank()) {
                            onSend(input)
                            input = ""
                        }
                    },
                    onStop = onStop,
                )
            }
        }
    }
    if (confirmDelete) {
        AlertDialog(
            onDismissRequest = { confirmDelete = false },
            title = { Text("删除当前会话？") },
            text = { Text("该会话及其本地消息记录将被删除，此操作无法撤销。") },
            confirmButton = {
                TextButton(onClick = {
                    conversation?.id?.let(onDeleteConversation)
                    confirmDelete = false
                }) { Text("删除") }
            },
            dismissButton = { TextButton(onClick = { confirmDelete = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun AiChatInputBar(
    input: String,
    onInputChange: (String) -> Unit,
    contextMessage: String,
    currentConversationStreaming: Boolean,
    anotherConversationStreaming: Boolean,
    onSend: () -> Unit,
    onStop: () -> Unit,
) {
    Column(
        modifier = Modifier.fillMaxWidth()
            .padding(start = 12.dp, top = 4.dp, end = 12.dp, bottom = 10.dp),
    ) {
        if (contextMessage.isNotBlank()) {
            Text(
                contextMessage,
                modifier = Modifier.padding(start = 8.dp, end = 8.dp, bottom = 4.dp),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        if (input.length > 10_000) {
            Text(
                "${input.length}/12000",
                modifier = Modifier.align(Alignment.End),
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
        Surface(
            modifier = Modifier.fillMaxWidth(),
            shape = RoundedCornerShape(30.dp),
            color = MaterialTheme.colorScheme.surfaceContainerHigh,
            tonalElevation = 5.dp,
            shadowElevation = 5.dp,
        ) {
            Row(
                modifier = Modifier.padding(start = 20.dp, end = 4.dp, top = 4.dp, bottom = 4.dp),
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.spacedBy(8.dp),
            ) {
                BasicTextField(
                    value = input,
                    onValueChange = onInputChange,
                    modifier = Modifier.weight(1f).heightIn(min = 48.dp, max = 128.dp)
                        .padding(vertical = 13.dp),
                    enabled = !anotherConversationStreaming,
                    maxLines = 5,
                    textStyle = MaterialTheme.typography.bodyLarge.copy(
                        color = MaterialTheme.colorScheme.onSurface,
                    ),
                    cursorBrush = SolidColor(MaterialTheme.colorScheme.primary),
                    decorationBox = { innerTextField ->
                        Box {
                            if (input.isEmpty()) {
                                Text(
                                    "输入消息",
                                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                                    style = MaterialTheme.typography.bodyLarge,
                                )
                            }
                            innerTextField()
                        }
                    },
                )
                FilledIconButton(
                    onClick = if (currentConversationStreaming) onStop else onSend,
                    modifier = Modifier.size(48.dp),
                    enabled = currentConversationStreaming || !anotherConversationStreaming && input.isNotBlank(),
                ) {
                    Icon(
                        if (currentConversationStreaming) Icons.Default.Stop else Icons.AutoMirrored.Filled.Send,
                        if (currentConversationStreaming) "停止" else "发送",
                    )
                }
            }
        }
        if (anotherConversationStreaming) {
            Text(
                "另一个 AI 会话正在生成，完成或停止后可继续发送。",
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AiMessageBubble(message: AiChatMessage, onOpenLink: (String) -> Unit) {
    val user = message.role == AiChatRole.User
    Row(
        modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp),
        horizontalArrangement = if (user) Arrangement.End else Arrangement.Start,
    ) {
        Surface(
            modifier = Modifier.fillMaxWidth(if (user) 0.86f else 0.96f),
            shape = RoundedCornerShape(16.dp),
            color = if (user) MaterialTheme.colorScheme.primaryContainer else MaterialTheme.colorScheme.surfaceVariant,
        ) {
            Column(Modifier.padding(14.dp)) {
                if (message.content.isBlank() && message.status == AiMessageStatus.Streaming) Text("正在生成…")
                else MarkdownContent(message.content, onOpenLink)
                when (message.status) {
                    AiMessageStatus.Streaming -> Text("生成中", style = MaterialTheme.typography.labelSmall)
                    AiMessageStatus.Stopped -> Text("已停止", style = MaterialTheme.typography.labelSmall)
                    AiMessageStatus.Error -> Text("生成失败", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.error)
                    AiMessageStatus.Complete -> Unit
                }
            }
        }
    }
}

internal enum class MarkdownBlockType { Paragraph, Heading, Divider, List, Quote, Code }
internal data class MarkdownBlock(
    val type: MarkdownBlockType,
    val text: String = "",
    val level: Int = 0,
    val marker: String = "",
)

@Composable
internal fun MarkdownContent(markdown: String, onOpenLink: (String) -> Unit) {
    val blocks = remember(markdown) { parseMarkdownBlocks(markdown) }
    SelectionContainer {
        Column(verticalArrangement = Arrangement.spacedBy(7.dp)) {
            blocks.forEach { block ->
                when (block.type) {
                    MarkdownBlockType.Heading -> MarkdownInlineText(
                        block.text,
                        onOpenLink,
                        style = when (block.level) {
                            1 -> MaterialTheme.typography.titleLarge
                            2 -> MaterialTheme.typography.titleMedium
                            else -> MaterialTheme.typography.titleSmall
                        }.copy(fontWeight = FontWeight.Bold),
                    )
                    MarkdownBlockType.Divider -> HorizontalDivider(Modifier.padding(vertical = 3.dp))
                    MarkdownBlockType.Code -> Text(
                        block.text,
                        modifier = Modifier.fillMaxWidth().background(Color.Black.copy(alpha = 0.08f), RoundedCornerShape(8.dp))
                            .horizontalScroll(rememberScrollState()).padding(10.dp),
                        fontFamily = FontFamily.Monospace,
                        style = MaterialTheme.typography.bodySmall,
                    )
                    MarkdownBlockType.Quote -> Text(
                        block.text,
                        modifier = Modifier.fillMaxWidth().background(MaterialTheme.colorScheme.surface.copy(alpha = 0.55f))
                            .padding(start = 12.dp, top = 6.dp, bottom = 6.dp),
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                    MarkdownBlockType.List -> MarkdownInlineText("${block.marker.ifBlank { "•" }} ${block.text}", onOpenLink)
                    MarkdownBlockType.Paragraph -> MarkdownInlineText(block.text, onOpenLink)
                }
            }
        }
    }
}

@Composable
private fun MarkdownInlineText(
    text: String,
    onOpenLink: (String) -> Unit,
    style: androidx.compose.ui.text.TextStyle = MaterialTheme.typography.bodyLarge,
) {
    val linkColor = MaterialTheme.colorScheme.primary
    val annotated = remember(text, linkColor) { buildMarkdownAnnotatedString(text, linkColor) }
    ClickableText(
        text = annotated,
        style = style.copy(color = MaterialTheme.colorScheme.onSurface),
        onClick = { offset -> annotated.getStringAnnotations("URL", offset, offset).firstOrNull()?.let { onOpenLink(it.item) } },
    )
}

internal fun parseMarkdownBlocks(markdown: String): List<MarkdownBlock> {
    if (markdown.isBlank()) return emptyList()
    val result = mutableListOf<MarkdownBlock>()
    val paragraph = mutableListOf<String>()
    val code = mutableListOf<String>()
    var inCode = false
    fun flushParagraph() {
        if (paragraph.isNotEmpty()) {
            result += MarkdownBlock(MarkdownBlockType.Paragraph, paragraph.joinToString("\n"))
            paragraph.clear()
        }
    }
    val heading = Regex("^\\s{0,3}(#{1,6})\\s+(.+?)\\s*#*\\s*$")
    val divider = Regex("^\\s{0,3}(?:-{3,}|_{3,}|\\*{3,})\\s*$")
    val list = Regex("^\\s*((?:[-*+•‣–—])|(?:\\d+[.)]))\\s+(.+)$")
    markdown.lines().forEach { line ->
        val headingMatch = heading.matchEntire(line)
        val listMatch = list.matchEntire(line)
        when {
            line.trimStart().startsWith("```") -> {
                if (inCode) { result += MarkdownBlock(MarkdownBlockType.Code, code.joinToString("\n")); code.clear() }
                else flushParagraph()
                inCode = !inCode
            }
            inCode -> code += line
            line.isBlank() -> flushParagraph()
            headingMatch != null -> {
                flushParagraph()
                result += MarkdownBlock(MarkdownBlockType.Heading, headingMatch.groupValues[2], headingMatch.groupValues[1].length)
            }
            divider.matches(line) -> { flushParagraph(); result += MarkdownBlock(MarkdownBlockType.Divider) }
            line.trimStart().startsWith(">") -> {
                flushParagraph()
                result += MarkdownBlock(MarkdownBlockType.Quote, line.trimStart().removePrefix(">").trimStart())
            }
            listMatch != null -> {
                flushParagraph()
                val rawMarker = listMatch.groupValues[1]
                val marker = if (rawMarker.firstOrNull()?.isDigit() == true) rawMarker else "•"
                result += MarkdownBlock(MarkdownBlockType.List, listMatch.groupValues[2], marker = marker)
            }
            else -> paragraph += line
        }
    }
    if (inCode && code.isNotEmpty()) result += MarkdownBlock(MarkdownBlockType.Code, code.joinToString("\n"))
    flushParagraph()
    return result
}

internal fun looksLikeMarkdown(text: String): Boolean {
    if (text.isBlank() || NGA_BBCODE_HINT.containsMatchIn(text)) return false
    val lines = text.lines()
    var score = 0
    if (lines.any { MARKDOWN_HEADING.matches(it) }) score += 2
    if (lines.any { it.trimStart().startsWith("```") }) score += 3
    if (lines.count { MARKDOWN_LIST.matches(it) } >= 2) score += 2
    if (lines.any { MARKDOWN_QUOTE.matches(it) }) score += 1
    if (MARKDOWN_STRONG.containsMatchIn(text)) score += 1
    if (MARKDOWN_LINK.containsMatchIn(text)) score += 1
    return score >= 2
}

private val NGA_BBCODE_HINT = Regex(
    "\\[(?:/?(?:b|i|u|s|quote|url|img|flash|video|collapse|table|tr|td|list|code|color|font|size|align|h|album))(?=[=\\]\\s])",
    RegexOption.IGNORE_CASE,
)
private val MARKDOWN_HEADING = Regex("^\\s{0,3}#{1,6}\\s+.+$")
private val MARKDOWN_LIST = Regex("^\\s*(?:[-*+•‣–—]|\\d+[.)])\\s+.+$")
private val MARKDOWN_QUOTE = Regex("^\\s*>\\s?.+$")
private val MARKDOWN_STRONG = Regex("(?:\\*\\*|__)[^\\n]+?(?:\\*\\*|__)")
private val MARKDOWN_LINK = Regex("\\[[^]]+]\\(https?://[^)]+\\)")

internal fun buildMarkdownAnnotatedString(text: String, linkColor: Color): AnnotatedString = buildAnnotatedString {
    val pattern = Regex("(\\[([^]]+)]\\((https?://[^)]+)\\))|(https?://[^\\s，。；！？、）】]+)|(`[^`]+`)|(\\*\\*[^*]+\\*\\*)|(~~[^~]+~~)|(\\*[^*\\n]+\\*)|(_[^_\\n]+_)")
    var cursor = 0
    pattern.findAll(text).forEach { match ->
        append(text.substring(cursor, match.range.first))
        val value = match.value
        when {
            value.startsWith("[") -> {
                val label = match.groupValues[2]
                val url = match.groupValues[3]
                pushStringAnnotation("URL", url)
                pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                append(label)
                pop(); pop()
            }
            value.startsWith("http") -> {
                pushStringAnnotation("URL", value)
                pushStyle(SpanStyle(color = linkColor, textDecoration = TextDecoration.Underline))
                append(value)
                pop(); pop()
            }
            value.startsWith("`") -> {
                pushStyle(SpanStyle(fontFamily = FontFamily.Monospace, background = Color.Black.copy(alpha = 0.08f)))
                append(value.removeSurrounding("`")); pop()
            }
            value.startsWith("**") -> {
                pushStyle(SpanStyle(fontWeight = FontWeight.Bold)); append(value.removeSurrounding("**")); pop()
            }
            value.startsWith("~~") -> {
                pushStyle(SpanStyle(textDecoration = TextDecoration.LineThrough)); append(value.removeSurrounding("~~")); pop()
            }
            value.startsWith("*") -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(value.removeSurrounding("*")); pop()
            }
            value.startsWith("_") -> {
                pushStyle(SpanStyle(fontStyle = FontStyle.Italic)); append(value.removeSurrounding("_")); pop()
            }
        }
        cursor = match.range.last + 1
    }
    append(text.substring(cursor))
}
