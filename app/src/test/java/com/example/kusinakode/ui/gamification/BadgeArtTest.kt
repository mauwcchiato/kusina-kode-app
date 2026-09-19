package com.example.kusinakode.ui.gamification

import com.example.kusinakode.domain.gamification.BadgeRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test

/**
 * Every badge the game can award must have a medal.
 *
 * Asserted against [BadgeRules.catalog] rather than a fixed list, so adding a
 * milestone fails here with its badge id instead of shipping a new badge that
 * silently falls back to the generic icon.
 */
class BadgeArtTest {

    @Test
    fun `every badge in the catalogue has artwork`() {
        val missing = BadgeRules.catalog()
            .map { (_, _, id) -> id }
            .filter { BadgeArt.forBadge(it) == null }

        assertEquals("Badges with no medal art: $missing", emptyList<String>(), missing)
    }

    @Test
    fun `art is keyed on the badge id, not the title`() {
        // The id is the server's primary key, so re-wording a title must not
        // detach its medal.
        val id = BadgeRules.badgeId(
            com.example.kusinakode.domain.model.BadgeType.ROUNDS,
            BadgeRules.ROUND_MILESTONES.first().threshold
        )
        assertNotNull(BadgeArt.forBadge(id))
        assertEquals(BadgeArt.forBadge(id), BadgeArt.forBadge("  ROUNDS_1  "))
    }

    @Test
    fun `distinct badges get distinct medals`() {
        val ids = BadgeRules.catalog().map { (_, _, id) -> id }
        val art = ids.mapNotNull { BadgeArt.forBadge(it) }
        assertEquals("Two badges share one medal", art.size, art.distinct().size)
    }

    @Test
    fun `an unknown badge returns null rather than throwing`() {
        assertEquals(null, BadgeArt.forBadge("rounds_999"))
        assertEquals(null, BadgeArt.forBadge(""))
    }
}
