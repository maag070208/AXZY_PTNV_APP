package com.axzydev.puertonuevoapp.core.util

import kotlin.test.Test
import kotlin.test.assertEquals

class DateFormatTest {

    @Test
    fun formatIsoUtcRoundTripsWithParser() {
        val iso = "2026-09-22T10:00:00.000Z"
        val millis = parseIsoToEpochMillis(iso)
        assertEquals(iso, formatIsoUtc(millis!!))
    }

    @Test
    fun formatIsoUtcHandlesEpochAndPadding() {
        assertEquals("1970-01-01T00:00:00.000Z", formatIsoUtc(0L))
        assertEquals("2020-01-01T00:00:00.000Z", formatIsoUtc(parseIsoToEpochMillis("2020-01-01T00:00:00.000Z")!!))
    }
}
