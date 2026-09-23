package com.axzydev.puertonuevoapp.feature.access

import com.axzydev.puertonuevoapp.core.network.access.AccessEventType
import com.axzydev.puertonuevoapp.core.network.access.AccessLastEventDto
import kotlin.test.Test
import kotlin.test.assertEquals

class AccessSuggestionTest {

    private fun lastEvent(type: String) = AccessLastEventDto(
        id = "evt-1",
        type = type,
        occurredAt = "2026-09-22T10:00:00.000Z",
    )

    @Test
    fun afterEntrySuggestsExit() {
        assertEquals(AccessEventType.EXIT, suggestedTypeFromLastEvent(lastEvent(AccessEventType.ENTRY)))
    }

    @Test
    fun afterExitSuggestsEntry() {
        assertEquals(AccessEventType.ENTRY, suggestedTypeFromLastEvent(lastEvent(AccessEventType.EXIT)))
    }

    @Test
    fun withoutPreviousEventSuggestsEntry() {
        assertEquals(AccessEventType.ENTRY, suggestedTypeFromLastEvent(null))
    }
}
