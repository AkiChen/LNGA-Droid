package com.aki.lnga.data

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class NgaJsonTest {
    @Test
    fun sanitizesNgaNumericAndControlCharacterQuirks() {
        val raw = "{\"content\":+123,\"author\":0123,\"subject\":\"line\nbreak\"}"
        val sanitized = NgaJson.sanitize(raw)

        assertTrue(sanitized.contains("\"content\":\"+123\""))
        assertTrue(sanitized.contains("\"author\":\"0123\""))
        assertTrue(sanitized.contains("line\\nbreak"))
    }

    @Test
    fun extractsScriptWrappedJson() {
        val parsed = NgaJson.parse(
            "<script>window.script_muti_get_var_store = {\"data\":{\"value\":1,\"text\":\"} and </script>\"}};</script>",
        )

        assertEquals(1L, parsed.objectOrNull("data")?.get("value").long())
        assertEquals("} and </script>", parsed.objectOrNull("data")?.get("text").text())
    }

    @Test
    fun rejectsIncompleteScriptStoreJson() {
        assertEquals(
            null,
            NgaJson.extractScriptStoreJson("window.script_muti_get_var_store={\"data\":{\"value\":1}"),
        )
    }
}
