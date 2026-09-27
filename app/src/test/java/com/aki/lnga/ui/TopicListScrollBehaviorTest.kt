package com.aki.lnga.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class TopicListScrollBehaviorTest {
    @Test
    fun scrollingTowardLaterTopicsHidesSortBar() {
        assertFalse(
            topicSortBarVisibleAfterScroll(
                previous = TopicListScrollPosition(index = 3, offset = 20),
                current = TopicListScrollPosition(index = 3, offset = 80),
                currentlyVisible = true,
            ),
        )
        assertFalse(
            topicSortBarVisibleAfterScroll(
                previous = TopicListScrollPosition(index = 3, offset = 80),
                current = TopicListScrollPosition(index = 4, offset = 5),
                currentlyVisible = true,
            ),
        )
    }

    @Test
    fun scrollingTowardEarlierTopicsShowsSortBar() {
        assertTrue(
            topicSortBarVisibleAfterScroll(
                previous = TopicListScrollPosition(index = 4, offset = 20),
                current = TopicListScrollPosition(index = 3, offset = 90),
                currentlyVisible = false,
            ),
        )
        assertTrue(
            topicSortBarVisibleAfterScroll(
                previous = TopicListScrollPosition(index = 3, offset = 80),
                current = TopicListScrollPosition(index = 3, offset = 20),
                currentlyVisible = false,
            ),
        )
    }

    @Test
    fun reachingTopAlwaysShowsSortBarAndIdleKeepsState() {
        assertTrue(
            topicSortBarVisibleAfterScroll(
                previous = TopicListScrollPosition(index = 0, offset = 20),
                current = TopicListScrollPosition(index = 0, offset = 0),
                currentlyVisible = false,
            ),
        )
        assertFalse(
            topicSortBarVisibleAfterScroll(
                previous = TopicListScrollPosition(index = 5, offset = 10),
                current = TopicListScrollPosition(index = 5, offset = 10),
                currentlyVisible = false,
            ),
        )
    }

    @Test
    fun inlineHeaderSuppressesFloatingSortBarAtListTop() {
        assertFalse(
            floatingTopicSortBarVisible(
                requestedByScrollDirection = true,
                inlineSortBarVisible = true,
            ),
        )
        assertTrue(
            floatingTopicSortBarVisible(
                requestedByScrollDirection = true,
                inlineSortBarVisible = false,
            ),
        )
    }
}
