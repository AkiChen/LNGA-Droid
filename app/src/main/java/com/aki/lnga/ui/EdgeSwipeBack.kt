package com.aki.lnga.ui

import androidx.compose.ui.Modifier
import androidx.compose.foundation.gestures.awaitEachGesture
import androidx.compose.foundation.gestures.awaitFirstDown
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.unit.dp
import kotlin.math.abs

/** Triggers back when a predominantly horizontal gesture starts at either screen edge. */
internal fun Modifier.edgeSwipeBack(onBack: () -> Unit): Modifier = pointerInput(onBack) {
    val edgeWidth = 28.dp.toPx()
    val triggerDistance = 72.dp.toPx()
    awaitEachGesture {
        val down = awaitFirstDown(requireUnconsumed = false)
        var horizontal = 0f
        var vertical = 0f
        var triggered = false
        do {
            val event = awaitPointerEvent()
            val change = event.changes.firstOrNull { it.id == down.id } ?: break
            horizontal = change.position.x - down.position.x
            vertical = change.position.y - down.position.y
            if (!triggered && shouldTriggerEdgeBack(
                    startX = down.position.x,
                    width = size.width.toFloat(),
                    horizontal = horizontal,
                    vertical = vertical,
                    edgeWidth = edgeWidth,
                    triggerDistance = triggerDistance,
                )
            ) {
                triggered = true
                change.consume()
                onBack()
            }
        } while (event.changes.any { it.pressed })
    }
}

internal fun shouldTriggerEdgeBack(
    startX: Float,
    width: Float,
    horizontal: Float,
    vertical: Float,
    edgeWidth: Float,
    triggerDistance: Float,
): Boolean {
    val direction = when {
        startX <= edgeWidth -> 1f
        startX >= width - edgeWidth -> -1f
        else -> return false
    }
    return direction * horizontal >= triggerDistance && abs(horizontal) >= abs(vertical) * 1.35f
}
