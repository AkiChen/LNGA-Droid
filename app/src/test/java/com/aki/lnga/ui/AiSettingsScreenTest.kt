package com.aki.lnga.ui

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AiSettingsScreenTest {
    @Test
    fun newEditorIsNotBusyWhenNoOperationExists() {
        assertFalse(isAiEditorBusy("new-config", null))
    }

    @Test
    fun onlyMatchingOperationDisablesEditorActions() {
        assertTrue(isAiEditorBusy("config-1", "config-1"))
        assertFalse(isAiEditorBusy("config-1", "config-2"))
        assertFalse(isAiEditorBusy("", null))
    }
}
