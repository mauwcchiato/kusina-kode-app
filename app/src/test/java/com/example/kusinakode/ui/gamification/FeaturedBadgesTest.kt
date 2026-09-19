package com.example.kusinakode.ui.gamification

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

/**
 * How the profile shelf decides what to show.
 *
 * The fallbacks matter more than the happy path: a player who has never
 * opened the picker, one whose chosen badge somehow is not held, and one who
 * clears their choice must all still see a sensible shelf.
 */
class FeaturedBadgesTest {

    private data class Slot(val id: String, val earned: Boolean)

    private fun resolve(all: List<Slot>, chosen: List<String>) =
        FeaturedBadges.resolve(all, chosen, { it.id }, { it.earned }).map { it.id }

    private val roster = listOf(
        Slot("rounds_1", true),
        Slot("rounds_5", true),
        Slot("streak_3", true),
        Slot("perfect_1", true),
        Slot("sea_of_green", true),
        Slot("rounds_10", false),
        Slot("streak_5", false)
    )

    @Test
    fun `a chosen showcase is shown in the player's order`() {
        val chosen = listOf("sea_of_green", "perfect_1", "rounds_5", "streak_3")
        assertEquals(chosen, resolve(roster, chosen))
    }

    @Test
    fun `no choice falls back to earned badges first`() {
        val shown = resolve(roster, emptyList())
        assertEquals(FeaturedBadges.SLOTS, shown.size)
        assertTrue(shown.none { id -> roster.first { it.id == id }.earned.not() })
    }

    @Test
    fun `a partial choice is topped up rather than leaving gaps`() {
        val shown = resolve(roster, listOf("sea_of_green"))
        assertEquals(FeaturedBadges.SLOTS, shown.size)
        assertEquals("sea_of_green", shown.first())
        assertEquals(shown.size, shown.distinct().size)
    }

    @Test
    fun `a chosen badge that is not earned is dropped, not shown as locked`() {
        // Can happen if a device's saved choice outlives the badge record.
        val shown = resolve(roster, listOf("rounds_10", "rounds_1"))
        assertTrue("rounds_10" !in shown)
        assertEquals("rounds_1", shown.first())
        assertEquals(FeaturedBadges.SLOTS, shown.size)
    }

    @Test
    fun `more choices than slots are trimmed to the first ones`() {
        val chosen = listOf("rounds_1", "rounds_5", "streak_3", "perfect_1", "sea_of_green")
        val shown = resolve(roster, chosen)
        assertEquals(chosen.take(FeaturedBadges.SLOTS), shown)
    }

    @Test
    fun `a new player with nothing earned still sees locked slots to aim at`() {
        val fresh = listOf(
            Slot("rounds_1", false),
            Slot("rounds_5", false),
            Slot("streak_3", false),
            Slot("perfect_1", false)
        )
        assertEquals(FeaturedBadges.SLOTS, resolve(fresh, emptyList()).size)
    }

    @Test
    fun `a short roster does not pad beyond what exists`() {
        val two = listOf(Slot("rounds_1", true), Slot("rounds_5", true))
        assertEquals(2, resolve(two, emptyList()).size)
    }

    @Test
    fun `the shelf never repeats a badge`() {
        val shown = resolve(roster, listOf("rounds_1", "rounds_1", "rounds_5"))
        assertEquals(shown.size, shown.distinct().size)
    }
}
