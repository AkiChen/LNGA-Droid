package com.aki.lnga.ui

internal fun isQuickUpwardSwipe(distancePx: Float, durationMillis: Long, minimumDistancePx: Float): Boolean =
    distancePx >= minimumDistancePx && durationMillis in 0L..360L

internal fun isSecondQuickSwipe(
    previousSwipeTime: Long,
    currentSwipeTime: Long,
    maximumIntervalMillis: Long = 850L,
): Boolean = previousSwipeTime > 0L && currentSwipeTime - previousSwipeTime in 0L..maximumIntervalMillis
