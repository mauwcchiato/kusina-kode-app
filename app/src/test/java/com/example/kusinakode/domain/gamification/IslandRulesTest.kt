package com.example.kusinakode.domain.gamification

import org.junit.Assert.assertEquals
import org.junit.Test

class IslandRulesTest {

    private val catalog = mapOf(
        "luzon" to setOf(2, 3),
        "visayas" to setOf(1),
        "mindanao" to setOf(6)
    )

    @Test
    fun `an island pays only when every dish there is solved`() {
        assertEquals(emptySet<String>(), IslandRules.completed(setOf(2), catalog))
        assertEquals(setOf("luzon"), IslandRules.completed(setOf(2, 3), catalog))
        assertEquals(
            setOf("luzon", "visayas", "mindanao"),
            IslandRules.completed(setOf(1, 2, 3, 6), catalog)
        )
    }
}
