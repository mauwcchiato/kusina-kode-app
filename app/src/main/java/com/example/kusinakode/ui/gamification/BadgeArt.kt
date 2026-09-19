package com.example.kusinakode.ui.gamification

import androidx.annotation.DrawableRes
import com.example.kusinakode.R

/**
 * Medal artwork for the badges the game awards.
 *
 * Generated - do not hand-edit. Keyed on the badge id from
 * [com.example.kusinakode.domain.gamification.BadgeRules.badgeId], which is
 * also the server's primary key, so the art follows a badge even if its
 * title is reworded. A badge with no artwork returns null and the caller
 * falls back to the medal icon.
 */
object BadgeArt {

    private val byId: Map<String, Int> = mapOf(
        "rounds_1" to R.drawable.badge_rounds_1,   // First Dish
        "rounds_5" to R.drawable.badge_rounds_5,   // Line Cook
        "rounds_10" to R.drawable.badge_rounds_10,   // Sous Chef
        "rounds_20" to R.drawable.badge_rounds_20,   // Head Chef
        "streak_3" to R.drawable.badge_streak_3,   // On a Roll
        "streak_5" to R.drawable.badge_streak_5,   // Kitchen Rhythm
        "streak_10" to R.drawable.badge_streak_10,   // Unshakeable
        "perfect_1" to R.drawable.badge_perfect_1,   // One-Shot Wonder
        "perfect_5" to R.drawable.badge_perfect_5,   // Flawless Palate
        "sea_of_green" to R.drawable.badge_sea_of_green,   // Sea of Green
        "clean_kitchen" to R.drawable.badge_clean_kitchen,   // Clean Kitchen
        "last_plating" to R.drawable.badge_last_plating,   // Last Plating
        "fast_thinker" to R.drawable.badge_fast_thinker,   // Fast Thinker
        "world_explorer" to R.drawable.badge_world_explorer,   // World Explorer
        "trivia_detective" to R.drawable.badge_trivia_detective,   // Trivia Detective
        "knowledge_collector" to R.drawable.badge_knowledge_collector,   // Knowledge Collector
    )

    /** Medal for [badgeId], or null when we have no art for it. */
    @DrawableRes
    fun forBadge(badgeId: String): Int? = byId[badgeId.trim().lowercase()]
}
