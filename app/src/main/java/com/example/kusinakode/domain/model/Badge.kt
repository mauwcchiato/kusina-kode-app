package com.example.kusinakode.domain.model

/**
 * Badge families.
 *
 * ROUNDS, STREAK and PERFECT are the three the manuscript's Scope section
 * names. The rest cover the achievement badges the team's badge sheet adds -
 * they are stored in the same `badge_type` column, so the backend's enum
 * needs these values too before badges of these families can sync.
 */
enum class BadgeType { ROUNDS, STREAK, PERFECT, ACCURACY, SPEED, EXPLORATION, KNOWLEDGE }

/**
 * Whether the chain has confirmed this badge yet. Everything is [PENDING]
 * until Module 3's reward pipeline returns a TxHash.
 */
enum class BadgeVerification { PENDING, CONFIRMED }

/**
 * Matches the shared contract exactly:
 * Badge { badge_id, user_id, badge_type, milestone_criteria, tx_hash, awarded_at }
 */
data class Badge(
    val badgeId: String,
    val userId: Int?,
    val badgeType: BadgeType,
    val title: String,
    val milestoneCriteria: String,
    /** Null until the Blockchain Incentive Module confirms the award. */
    val txHash: String? = null,
    val awardedAt: Long = System.currentTimeMillis()
) {
    val verification: BadgeVerification
        get() = if (txHash.isNullOrBlank()) BadgeVerification.PENDING else BadgeVerification.CONFIRMED
}
