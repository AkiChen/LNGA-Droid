@file:OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)

package com.aki.lnga.ui

import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.aki.lnga.data.AiConversationKind
import com.aki.lnga.data.completedAssistantText
import kotlin.math.cos
import kotlin.math.sin

private val DayIdle = Color(0xFF707078)
private val NightIdle = Color(0xFFC4C4CC)
private val DayComplete = Color(0xFFC68A16)
private val NightComplete = Color(0xFFFFCA57)
private val Failed = Color(0xFFD64D5F)

@Composable
internal fun AiTaskStatusButton(
    state: MainUiState,
    kind: AiConversationKind? = null,
    sourceId: Long = 0,
    onClick: () -> Unit,
    onLongClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
) {
    val darkTheme = isSystemInDarkTheme()
    val taskMatches = kind == null || state.aiTaskKind == kind && state.aiTaskSourceId == sourceId
    val running = taskMatches && (state.aiTaskPhase == AiTaskPhase.Preparing || state.aiTaskPhase == AiTaskPhase.Streaming)
    val hasResult = if (kind == null) {
        state.aiConversations.any { it.kind != AiConversationKind.General && it.completedAssistantText() != null }
    } else {
        state.aiConversations.any {
            it.kind == kind && it.sourceId == sourceId && it.completedAssistantText() != null
        }
    }
    val failed = taskMatches && state.aiTaskPhase == AiTaskPhase.Error
    val color = when {
        failed -> Failed
        hasResult && !running -> if (darkTheme) NightComplete else DayComplete
        else -> if (darkTheme) NightIdle else DayIdle
    }
    val description = when {
        running -> state.aiTaskTitle.ifBlank { "AI 正在生成" }
        failed -> "AI 生成失败"
        hasResult -> "打开 AI 结果"
        else -> "生成 AI 结果"
    }
    Box(
        contentAlignment = Alignment.Center,
        modifier = modifier.size(48.dp)
            .semantics { contentDescription = description }
            .combinedClickable(
                role = Role.Button,
                onClick = onClick,
                onLongClick = onLongClick,
            ),
    ) {
        PrismaticAiIcon(
            color = color,
            animated = running,
            modifier = Modifier.size(32.dp),
        )
    }
}

@Composable
internal fun CompletedAiMark(modifier: Modifier = Modifier) {
    val color = if (isSystemInDarkTheme()) NightComplete else DayComplete
    PrismaticAiIcon(color = color, modifier = modifier, intensity = 1f)
}

/** Flat, single-mark AI icon whose four sides use continuous inward curves. */
@Composable
internal fun PrismaticAiIcon(
    color: Color,
    modifier: Modifier = Modifier,
    animated: Boolean = false,
    intensity: Float = 1f,
) {
    val phase = if (animated) {
        val transition = rememberInfiniteTransition(label = "ai-four-point-flow")
        val animatedPhase by transition.animateFloat(
            initialValue = 0f,
            targetValue = 1f,
            animationSpec = infiniteRepeatable(tween(2_800, easing = LinearEasing)),
            label = "ai-four-point-phase",
        )
        animatedPhase
    } else 0f
    Canvas(modifier) {
        val center = Offset(size.width / 2f, size.height / 2f)
        val radius = size.minDimension * 0.455f
        val star = roundedFourPointPath(center, radius)
        if (!animated) {
            drawPath(star, color.copy(alpha = intensity.coerceIn(0.45f, 1f)))
        } else {
            val angle = phase * Math.PI.toFloat() * 2f
            val orbit = size.minDimension * 0.34f
            drawPath(
                star,
                brush = Brush.linearGradient(
                    colors = listOf(
                        Color(0xFF2D8CFF),
                        Color(0xFF7654FF),
                        Color(0xFFE84DBB),
                        Color(0xFFFF9D3D),
                        Color(0xFF20C7B5),
                        Color(0xFF2D8CFF),
                    ),
                    start = Offset(
                        center.x - radius + cos(angle) * orbit,
                        center.y - radius + sin(angle) * orbit,
                    ),
                    end = Offset(
                        center.x + radius + cos(angle) * orbit,
                        center.y + radius + sin(angle) * orbit,
                    ),
                ),
            )
            drawPath(
                star,
                brush = Brush.radialGradient(
                    colors = listOf(
                        Color(0xFFFFE278).copy(alpha = 0.72f),
                        Color(0xFFE14CF0).copy(alpha = 0.28f),
                        Color.Transparent,
                    ),
                    center = Offset(
                        center.x + cos(angle * 1.35f) * orbit,
                        center.y + sin(angle * 1.10f) * orbit,
                    ),
                    radius = size.minDimension * 0.52f,
                ),
            )
        }
    }
}

private fun roundedFourPointPath(center: Offset, radius: Float): Path = Path().apply {
    val inward = radius * 0.28f
    moveTo(center.x, center.y - radius)
    cubicTo(center.x, center.y - inward, center.x + inward, center.y, center.x + radius, center.y)
    cubicTo(center.x + inward, center.y, center.x, center.y + inward, center.x, center.y + radius)
    cubicTo(center.x, center.y + inward, center.x - inward, center.y, center.x - radius, center.y)
    cubicTo(center.x - inward, center.y, center.x, center.y - inward, center.x, center.y - radius)
    close()
}
