package com.example.kusinakode.domain.gamification

import com.example.kusinakode.domain.model.Badge
import com.example.kusinakode.domain.model.BadgeType
import com.example.kusinakode.domain.model.PlayerProgress

/**
 * Badge eligibility for the three milestone families the manuscript names
 * (Module 2, element ii): rounds completed, winning streak, perfect score.
 *
 * Pure: given progress and what's already been awarded, it returns only the
 * badges newly earned right now. Awarding is idempotent by construction —
 * a badge whose id is already held is never returned again, which is what
 * "no early/late/duplicate awards" in the acceptance criteria requires.
 */
object BadgeRules {

    data class Milestone(val threshold: Int, val title: String, val criteria: String)

    val ROUND_MILESTONES = listOf(
        Milestone(1, "First Dish", "Solve your first dish"),
        Milestone(5, "Line Cook", "Solve 5 dishes"),
        Milestone(10, "Sous Chef", "Solve 10 dishes"),
        Milestone(20, "Head Chef", "Solve 20 dishes")
    )

    val STREAK_MILESTONES = listOf(
        Milestone(3, "On a Roll", "Win 3 rounds in a row"),
        Milestone(5, "Kitchen Rhythm", "Win 5 rounds in a row"),
        Milestone(10, "Unshakeable", "Win 10 rounds in a row")
    )

    val PERFECT_MILESTONES = listOf(
        Milestone(1, "One-Shot Wonder", "Solve a dish on the first guess"),
        Milestone(5, "Flawless Palate", "Solve 5 dishes on the first guess")
    )

    /** A round has to beat this to count as fast. */
    const val FAST_ROUND_MS = 30_000L

    /** Dishes in the KODEX needed for the two knowledge badges. */
    const val TRIVIA_DETECTIVE_READS = 5
    const val KNOWLEDGE_COLLECTOR_READS = 15

    /**
     * A one-off achievement, as opposed to a counting milestone.
     *
     * These carry an explicit id rather than the `type_threshold` scheme, so
     * their keys stay stable even if a threshold is retuned later.
     */
    data class Achievement(
        val id: String,
        val type: BadgeType,
        val title: String,
        val criteria: String,
        val isMet: (PlayerProgress) -> Boolean
    )

    /** How many regions the game ships; World Explorer needs all of them. */
    const val REGION_COUNT = 4

    val ACHIEVEMENTS = listOf(
        Achievement(
            "sea_of_green", BadgeType.ACCURACY, "Sea of Green",
            "Solve a dish without a single yellow tile"
        ) { it.seaOfGreenRounds >= 1 },
        Achievement(
            "clean_kitchen", BadgeType.ACCURACY, "Clean Kitchen",
            "Solve a dish without spending a power-up"
        ) { it.cleanRounds >= 1 },
        Achievement(
            "last_plating", BadgeType.ACCURACY, "Last Plating",
            "Solve a dish on your very last guess"
        ) { it.lastPlatings >= 1 },
        Achievement(
            "fast_thinker", BadgeType.SPEED, "Fast Thinker",
            "Solve a dish in under 30 seconds"
        ) { it.fastRounds >= 1 },
        Achievement(
            "world_explorer", BadgeType.EXPLORATION, "World Explorer",
            "Solve a dish from every region"
        ) { it.regionsSolved.size >= REGION_COUNT },
        Achievement(
            "trivia_detective", BadgeType.KNOWLEDGE, "Trivia Detective",
            "Read the story behind $TRIVIA_DETECTIVE_READS dishes"
        ) { it.dishesRead.size >= TRIVIA_DETECTIVE_READS },
        Achievement(
            "knowledge_collector", BadgeType.KNOWLEDGE, "Knowledge Collector",
            "Read the story behind $KNOWLEDGE_COLLECTOR_READS dishes"
        ) { it.dishesRead.size >= KNOWLEDGE_COLLECTOR_READS }
    )

    /** Stable id, also used as the primary key server-side. */
    fun badgeId(type: BadgeType, threshold: Int): String =
        "${type.name.lowercase()}_$threshold"

    /**
     * Badges newly earned by [progress] that aren't in [alreadyEarned].
     * Streaks are judged on the best streak ever reached, so a badge already
     * earned is never revoked when the current streak breaks.
     */
    fun newlyEarned(
        progress: PlayerProgress,
        alreadyEarned: Set<String>,
        userId: Int?
    ): List<Badge> {
        val earned = mutableListOf<Badge>()

        fun check(type: BadgeType, milestones: List<Milestone>, value: Int) {
            milestones.filter { value >= it.threshold }
                .forEach { milestone ->
                    val id = badgeId(type, milestone.threshold)
                    if (id !in alreadyEarned) {
                        earned += Badge(
                            badgeId = id,
                            userId = userId,
                            badgeType = type,
                            title = milestone.title,
                            milestoneCriteria = milestone.criteria
                        )
                    }
                }
        }

        check(BadgeType.ROUNDS, ROUND_MILESTONES, progress.roundsCompleted)
        check(BadgeType.STREAK, STREAK_MILESTONES, progress.bestStreak)
        check(BadgeType.PERFECT, PERFECT_MILESTONES, progress.perfectRounds)

        ACHIEVEMENTS.filter { it.isMet(progress) && it.id !in alreadyEarned }
            .forEach { achievement ->
                earned += Badge(
                    badgeId = achievement.id,
                    userId = userId,
                    badgeType = achievement.type,
                    title = achievement.title,
                    milestoneCriteria = achievement.criteria
                )
            }

        return earned
    }

    /** Every badge the game can award, for showing locked slots in the UI. */
    fun catalog(): List<Triple<BadgeType, Milestone, String>> =
        ROUND_MILESTONES.map { Triple(BadgeType.ROUNDS, it, badgeId(BadgeType.ROUNDS, it.threshold)) } +
            STREAK_MILESTONES.map { Triple(BadgeType.STREAK, it, badgeId(BadgeType.STREAK, it.threshold)) } +
            PERFECT_MILESTONES.map { Triple(BadgeType.PERFECT, it, badgeId(BadgeType.PERFECT, it.threshold)) } +
            ACHIEVEMENTS.map { Triple(it.type, Milestone(1, it.title, it.criteria), it.id) }
}
