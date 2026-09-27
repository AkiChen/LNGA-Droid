package com.aki.lnga.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ThreadGestureRulesTest {
    @Test
    fun requiresFastUpwardMovementForSwipe() {
        assertTrue(isQuickUpwardSwipe(distancePx = 50f, durationMillis = 250L, minimumDistancePx = 44f))
        assertFalse(isQuickUpwardSwipe(distancePx = 30f, durationMillis = 250L, minimumDistancePx = 44f))
        assertFalse(isQuickUpwardSwipe(distancePx = 50f, durationMillis = 500L, minimumDistancePx = 44f))
    }

    @Test
    fun recognizesOnlyCloselySpacedSecondSwipe() {
        assertTrue(isSecondQuickSwipe(previousSwipeTime = 1_000L, currentSwipeTime = 1_700L))
        assertFalse(isSecondQuickSwipe(previousSwipeTime = 1_000L, currentSwipeTime = 2_000L))
        assertFalse(isSecondQuickSwipe(previousSwipeTime = 0L, currentSwipeTime = 500L))
    }

    @Test
    fun honorsUserConfiguredDoubleSwipeInterval() {
        assertFalse(isSecondQuickSwipe(1_000L, 1_700L, maximumIntervalMillis = 600L))
        assertTrue(isSecondQuickSwipe(1_000L, 1_700L, maximumIntervalMillis = 1_200L))
    }
}
