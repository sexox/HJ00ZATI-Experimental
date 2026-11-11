package com.hjoozati.experimental

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class BookingLogicTest {
    @Test fun parsesValidTime() {
        assertEquals(19 * 60 + 30, parseMinutes("19:30") ?: -1)
    }

    @Test fun rejectsInvalidTime() {
        assertNull(parseMinutes("25:90"))
        assertNull(parseMinutes("7pm"))
    }
}
