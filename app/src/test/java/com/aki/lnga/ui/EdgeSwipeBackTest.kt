package com.aki.lnga.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class EdgeSwipeBackTest {
    @Test
    fun acceptsInwardSwipesFromBothEdges() {
        assertTrue(shouldTriggerEdgeBack(10f, 1080f, 100f, 10f, 28f, 72f))
        assertTrue(shouldTriggerEdgeBack(1070f, 1080f, -100f, 10f, 28f, 72f))
    }

    @Test
    fun rejectsMiddleOutwardShortAndVerticalGestures() {
        assertFalse(shouldTriggerEdgeBack(300f, 1080f, 100f, 0f, 28f, 72f))
        assertFalse(shouldTriggerEdgeBack(10f, 1080f, -100f, 0f, 28f, 72f))
        assertFalse(shouldTriggerEdgeBack(10f, 1080f, 60f, 0f, 28f, 72f))
        assertFalse(shouldTriggerEdgeBack(10f, 1080f, 100f, 90f, 28f, 72f))
    }
}
