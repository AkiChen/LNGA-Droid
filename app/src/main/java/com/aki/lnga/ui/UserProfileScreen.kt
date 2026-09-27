@file:OptIn(androidx.compose.material3.ExperimentalMaterial3Api::class)

package com.aki.lnga.ui

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
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
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import coil.compose.AsyncImage
import com.aki.lnga.data.AiConversationKind
import com.aki.lnga.data.completedAssistantText
import com.aki.lnga.data.UserActivityAnalysis
import com.aki.lnga.data.UserProfile
import com.aki.lnga.data.NgaNetworkDiagnosticStore
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
internal fun UserProfileScreen(
    state: MainUiState,
    onBack: () -> Unit,
    onAnalyze: () -> Unit,
    onRetryActivity: () -> Unit,
    onOpenAnalysis: () -> Unit,
    onOpenLink: (String) -> Unit,
) {
    BackHandler(onBack = onBack)
    val context = LocalContext.current
    var showConfirmation by remember(state.profileUid) { mutableStateOf(false) }
    val analysisText = state.aiConversations.asSequence()
        .filter { it.kind == AiConversationKind.UserAnalysis && it.sourceId == state.profileUid }
        .sortedByDescending { it.updatedAt }
        .mapNotNull { it.completedAssistantText() }
        .firstOrNull()
    val hasAnalysis = analysisText != null
    val taskMatches = state.aiTaskKind == AiConversationKind.UserAnalysis &&
        state.aiTaskSourceId == state.profileUid && state.aiTaskPhase != AiTaskPhase.Idle
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("用户光谱") },
                navigationIcon = {
                    IconButton(onClick = onBack) {
                        Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = "返回")
                    }
                },
                actions = {
                    AiTaskStatusButton(
                        state = state,
                        kind = AiConversationKind.UserAnalysis,
                        sourceId = state.profileUid,
                        onClick = { if (hasAnalysis || taskMatches) onOpenAnalysis() else showConfirmation = true },
                    )
                },
            )
        },
    ) { padding ->
        when {
            state.profileLoading && state.userProfile == null -> Box(
                Modifier.padding(padding).fillMaxSize(),
                contentAlignment = Alignment.Center,
            ) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    CircularProgressIndicator()
                    Text("正在读取公开资料…", color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
            else -> LazyColumn(
                modifier = Modifier.padding(padding).fillMaxSize(),
                contentPadding = androidx.compose.foundation.layout.PaddingValues(14.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                item {
                    ProfileHeader(
                        state.userProfile,
                        state.profileUid,
                        state.profileSeedName,
                        state.profileSeedAvatar,
                        hasAnalysis,
                    )
                }
                analysisText?.let { content ->
                    item { AnalysisResultCard(content, onOpenLink) }
                }
                state.profileError?.let { error ->
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Text(error, Modifier.padding(14.dp), color = MaterialTheme.colorScheme.error)
                        }
                    }
                }
                if (state.activityLoading) {
                    item {
                        Card(Modifier.fillMaxWidth()) {
                            Row(
                                Modifier.padding(14.dp),
                                verticalAlignment = Alignment.CenterVertically,
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                            ) {
                                CircularProgressIndicator(Modifier.size(20.dp), strokeWidth = 2.dp)
                                Text("正在加载近期主题与回复…")
                            }
                        }
                    }
                }
                state.activityError?.let { error ->
                    item {
                        ActivityLoadIssueCard(
                            message = error,
                            loading = state.activityLoading,
                            onRetry = onRetryActivity,
                            onCopyDiagnostics = {
                                val report = NgaNetworkDiagnosticStore.report(context, "user-activity")
                                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as ClipboardManager
                                clipboard.setPrimaryClip(ClipData.newPlainText("LNGA 用户活动诊断", report))
                                Toast.makeText(context, "脱敏诊断已复制", Toast.LENGTH_SHORT).show()
                            },
                        )
                    }
                }
                state.userActivity?.let { activity ->
                    item { ActivitySummary(activity) }
                    item {
                        Button(
                            onClick = { showConfirmation = true },
                            modifier = Modifier.fillMaxWidth(),
                            enabled = activity.topicCount + activity.replyCount > 0 && !state.aiStreaming && !state.aiSummaryRunning,
                        ) {
                            Icon(Icons.Default.AutoAwesome, contentDescription = null)
                            Text(if (hasAnalysis) "重新生成 AI 用户成分分析" else "AI 用户成分分析", Modifier.padding(start = 8.dp))
                        }
                    }
                    item {
                        Text(
                            "统计来自当前登录态可见的公开主题与回复；AI 结果只是基于有限样本的推断，不代表用户真实身份或立场。",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
    val activity = state.userActivity
    if (showConfirmation && activity != null) {
        AlertDialog(
            onDismissRequest = { showConfirmation = false },
            title = { Text("确认发送公开活动统计") },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    if (hasAnalysis) {
                        Text(
                            "开始生成新分析时会替换该用户之前的光谱分析会话。",
                            color = MaterialTheme.colorScheme.primary,
                        )
                    }
                    if (activity.isPartial) {
                        Text(
                            "注意：部分活动页未能加载，AI 将只分析当前已取得的样本。",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }
                    Text("将向当前启用的 AI 服务发送：")
                    Text("• 用户名与 UID\n• ${activity.topicCount} 个近期主题标题\n• ${activity.replyCount} 条近期回复片段\n• 活跃版面与发帖时段统计")
                    Text(
                        "不会发送头像、Cookie、登录凭据、图片诊断或本地浏览历史。",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                    )
                }
            },
            confirmButton = {
                Button(onClick = { showConfirmation = false; onAnalyze() }) { Text("确认并开始") }
            },
            dismissButton = { TextButton(onClick = { showConfirmation = false }) { Text("取消") } },
        )
    }
}

@Composable
private fun ActivityLoadIssueCard(
    message: String,
    loading: Boolean,
    onRetry: () -> Unit,
    onCopyDiagnostics: () -> Unit,
) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(14.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
            Text(message, color = MaterialTheme.colorScheme.error)
            Text(
                "可以稍后重试；已成功加载的资料和活动记录不会被清空。",
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                TextButton(onClick = onRetry, enabled = !loading) { Text("重试活动记录") }
                TextButton(onClick = onCopyDiagnostics) { Text("复制诊断") }
            }
        }
    }
}

@Composable
private fun ProfileHeader(
    profile: UserProfile?,
    uid: Long,
    seedName: String,
    seedAvatar: String,
    hasAnalysis: Boolean,
) {
    val name = profile?.userName?.ifBlank { seedName } ?: seedName.ifBlank { "用户 $uid" }
    val avatar = profile?.avatarUrl?.ifBlank { seedAvatar } ?: seedAvatar
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(Modifier.size(64.dp)) {
                    Box(
                        Modifier.size(58.dp).align(Alignment.BottomStart).clip(CircleShape)
                            .background(defaultAvatarColor(uid, name)),
                        contentAlignment = Alignment.Center,
                    ) {
                        Text(name.take(1).ifBlank { "?" }, color = Color.White, style = MaterialTheme.typography.headlineSmall)
                        if (avatar.isNotBlank()) {
                            AsyncImage(avatar, "$name 头像", Modifier.fillMaxSize(), contentScale = ContentScale.Crop)
                        }
                    }
                    if (hasAnalysis) {
                        CompletedAiMark(Modifier.align(Alignment.TopEnd).size(18.dp))
                    }
                }
                Column(Modifier.padding(start = 12.dp)) {
                    Text(name, style = MaterialTheme.typography.titleLarge, fontWeight = FontWeight.SemiBold)
                    Text("UID $uid", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.onSurfaceVariant)
                    profile?.ipLocation?.takeIf(String::isNotBlank)?.let { Text("IP属地 $it", style = MaterialTheme.typography.labelMedium) }
                }
            }
            profile?.let {
                val labels = listOfNotNull(
                    it.group.takeIf(String::isNotBlank), it.title.takeIf(String::isNotBlank),
                    it.honor.takeIf(String::isNotBlank), it.posts.takeIf(String::isNotBlank)?.let { posts -> "发帖 $posts" },
                    it.reputation.takeIf { value -> value != 0 }?.let { value -> "声望 $value" },
                    it.registeredAtSeconds.takeIf { value -> value > 0 }?.let { value -> "注册 ${formatProfileDate(value)}" },
                )
                if (labels.isNotEmpty()) Text(labels.joinToString(" · "), style = MaterialTheme.typography.bodySmall)
                if (it.followingCount.isNotBlank() || it.followerCount.isNotBlank()) {
                    Text("关注 ${it.followingCount.ifBlank { "-" }} · 粉丝 ${it.followerCount.ifBlank { "-" }}", style = MaterialTheme.typography.bodySmall)
                }
                if (it.signature.isNotBlank()) Text(it.signature, color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

@Composable
private fun AnalysisResultCard(content: String, onOpenLink: (String) -> Unit) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                CompletedAiMark(Modifier.size(20.dp))
                Text(
                    "上次生成的用户光谱",
                    modifier = Modifier.padding(start = 8.dp),
                    style = MaterialTheme.typography.titleMedium,
                    fontWeight = FontWeight.SemiBold,
                )
            }
            MarkdownContent(content, onOpenLink)
        }
    }
}

@Composable
private fun ActivitySummary(activity: UserActivityAnalysis) {
    Card(Modifier.fillMaxWidth()) {
        Column(Modifier.padding(16.dp), verticalArrangement = Arrangement.spacedBy(10.dp)) {
            Text("公开活动样本", style = MaterialTheme.typography.titleMedium, fontWeight = FontWeight.SemiBold)
            Text("主题 ${activity.topicCount} · 回复 ${activity.replyCount}")
            if (activity.forumStats.isNotEmpty()) {
                Text("活跃版面", style = MaterialTheme.typography.labelLarge)
                activity.forumStats.take(8).forEach {
                    Text("${it.name}  主题 ${it.topicCount} / 回复 ${it.replyCount}", style = MaterialTheme.typography.bodyMedium)
                }
            }
            val periods = listOf(
                "凌晨" to (0..5), "上午" to (6..11), "下午" to (12..17), "晚上" to (18..23),
            ).joinToString(" · ") { (label, hours) ->
                "$label ${hours.sumOf { activity.hourDistribution.getOrElse(it) { 0 } }}"
            }
            Text("发帖时段（东八区）", style = MaterialTheme.typography.labelLarge)
            Text(periods, style = MaterialTheme.typography.bodyMedium)
            if (activity.topics.isNotEmpty()) {
                Text("近期主题", style = MaterialTheme.typography.labelLarge)
                activity.topics.take(8).forEach { Text("• ${it.subject}", style = MaterialTheme.typography.bodyMedium) }
            }
            if (activity.replies.any { it.replySnippet.isNotBlank() }) {
                Text("近期回复片段", style = MaterialTheme.typography.labelLarge)
                activity.replies.asSequence().map { it.replySnippet }.filter(String::isNotBlank).take(8)
                    .forEach { Text("• $it", style = MaterialTheme.typography.bodyMedium, maxLines = 3) }
            }
            if (activity.topicCount + activity.replyCount == 0) {
                Text("没有读取到当前账号可见的近期公开记录。", color = MaterialTheme.colorScheme.onSurfaceVariant)
            }
        }
    }
}

private fun formatProfileDate(seconds: Long): String =
    SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(Date(seconds * 1000))
