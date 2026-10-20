package com.denge.app

import com.denge.app.ui.formatTimeInput
import com.denge.app.ui.parseTimeInput
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class MealTimeInputTest {
    @Test fun `four digits become an HH mm time`() {
        assertEquals("12:20", formatTimeInput("1220"))
        assertEquals("09:30", formatTimeInput("0930"))
        assertEquals("23:59", formatTimeInput("2359"))
    }
    @Test fun `three digits keep the hour readable`() {
        assertEquals("9:30", formatTimeInput("930"))
        assertEquals("12:2", formatTimeInput("122"))
    }
    @Test fun `digits typed one by one end up with a colon`() {
        var text = ""
        listOf("1", "2", "2", "0").forEach { typed -> text = formatTimeInput(text + typed) }
        assertEquals("12:20", text)
    }
    @Test fun `manually typed colon is preserved and bounded`() {
        assertEquals("9:30", formatTimeInput("9:30"))
        assertEquals("09:30", parseTimeInput("9:30"))
        assertEquals("14:5", formatTimeInput("14:5"))
        assertEquals("14:56", formatTimeInput("14:567"))
        assertEquals("", formatTimeInput(":"))
    }
    @Test fun `deleting characters keeps the value editable`() {
        assertEquals("12:2", formatTimeInput("12:2"))
        assertEquals("12:", formatTimeInput("12:"))
        assertEquals("12", formatTimeInput("12"))
        assertEquals("1", formatTimeInput("1"))
    }
    @Test fun `pasted and padded values are normalized on save`() {
        assertEquals("12:20", parseTimeInput("1220"))
        assertEquals("09:30", parseTimeInput("930"))
        assertEquals("00:05", parseTimeInput("0:5"))
        assertEquals("12:00", parseTimeInput("12:00"))
    }
    @Test fun `invalid times are rejected instead of guessed`() {
        assertNull(parseTimeInput("2400"))
        assertNull(parseTimeInput("24:00"))
        assertNull(parseTimeInput("12:60"))
        assertNull(parseTimeInput("12"))
        assertNull(parseTimeInput(""))
        assertNull(parseTimeInput("-1:20"))
        assertNull(parseTimeInput("ab:cd"))
    }
}
