package com.example.kusinakode.data.repository

import android.content.Context
import com.example.kusinakode.domain.gamification.ScoreRules
import com.example.kusinakode.domain.model.Badge
import com.example.kusinakode.domain.model.BadgeType
import com.example.kusinakode.domain.model.PlayerProgress
import com.example.kusinakode.domain.repository.GamificationRepository
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.json.JSONArray
import org.json.JSONObject

/**
 * On-device store for points, streaks and badges, kept per user.
 *
 * Deliberately local: these are computed values for display and eligibility.
 * The authoritative record lands on-chain via Module 3, and a remote
 * implementation of [GamificationRepository] can replace this without the
 * UI noticing.
 */
class LocalGamificationRepository(context: Context) : GamificationRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences("kusinakode_prefs", Context.MODE_PRIVATE)

    private val progressFlows = mutableMapOf<String, MutableStateFlow<PlayerProgress>>()
    private val badgeFlows = mutableMapOf<String, MutableStateFlow<List<Badge>>>()

    private fun scope(userId: Int?) =
        if (userId != null && userId > 0) "user_$userId" else "guest"

    private fun progressFlow(userId: Int?): MutableStateFlow<PlayerProgress> =
        progressFlows.getOrPut(scope(userId)) { MutableStateFlow(readProgress(userId)) }

    private fun badgeFlow(userId: Int?): MutableStateFlow<List<Badge>> =
        badgeFlows.getOrPut(scope(userId)) { MutableStateFlow(readBadges(userId)) }

    override fun progress(userId: Int?): Flow<PlayerProgress> = progressFlow(userId).asStateFlow()

    override fun badges(userId: Int?): Flow<List<Badge>> = badgeFlow(userId).asStateFlow()

    override suspend fun saveProgress(userId: Int?, progress: PlayerProgress) {
        prefs.edit()
            .putString("progress_${scope(userId)}", progress.toJson().toString())
            .apply()
        progressFlow(userId).value = progress
    }

    override suspend fun awardBadges(userId: Int?, badges: List<Badge>) {
        if (badges.isEmpty()) return
        val existing = readBadges(userId)
        val existingIds = existing.map { it.badgeId }.toSet()
        // Guard against double-writes even if a caller re-submits.
        val merged = existing + badges.filter { it.badgeId !in existingIds }
        writeBadges(userId, merged)
    }

    override suspend fun replaceFromServer(
        userId: Int?,
        progress: PlayerProgress,
        badges: List<Badge>
    ) {
        // The account's own record supersedes anything this device computed.
        prefs.edit().putBoolean(seededKey(userId), true).apply()
        saveProgress(userId, progress)
        writeBadges(userId, badges)
    }

    private fun seededKey(userId: Int?) = "gamification_seeded_${scope(userId)}"

    override suspend fun ensureSeeded(userId: Int?, solvedLevels: Set<Int>) {
        val key = seededKey(userId)
        if (prefs.getBoolean(key, false)) return

        // Nothing to seed yet — most likely unlocks haven't synced. Leave the
        // flag unset so this can run again once they arrive, instead of
        // locking in an empty baseline.
        if (solvedLevels.isEmpty()) return

        prefs.edit().putBoolean(key, true).apply()

        val current = progressFlow(userId).value
        val newlyCredited = solvedLevels - current.solvedLevels
        if (newlyCredited.isEmpty()) return

        // Credit the base award for each dish already cleared. Speed and
        // efficiency bonuses are left off because those rounds' timings were
        // never recorded — this pays for the solve, not for how it was done.
        saveProgress(
            userId,
            current.copy(
                solvedLevels = current.solvedLevels + newlyCredited,
                roundsCompleted = current.roundsCompleted + newlyCredited.size,
                totalPoints = current.totalPoints + newlyCredited.size * ScoreRules.BASE_POINTS
            )
        )
    }

    override suspend fun spendPoints(userId: Int?, amount: Int): Boolean {
        if (amount <= 0) return true
        val current = progressFlow(userId).value
        if (current.totalPoints < amount) return false
        saveProgress(userId, current.copy(totalPoints = current.totalPoints - amount))
        return true
    }

    override suspend fun markBadgeConfirmed(userId: Int?, badgeId: String, txHash: String) {
        val updated = readBadges(userId).map {
            if (it.badgeId == badgeId) it.copy(txHash = txHash) else it
        }
        writeBadges(userId, updated)
    }

    private fun writeBadges(userId: Int?, badges: List<Badge>) {
        val array = JSONArray().apply { badges.forEach { put(it.toJson()) } }
        prefs.edit().putString("badges_${scope(userId)}", array.toString()).apply()
        badgeFlow(userId).value = badges
    }

    private fun readProgress(userId: Int?): PlayerProgress {
        val raw = prefs.getString("progress_${scope(userId)}", null) ?: return PlayerProgress()
        return runCatching { JSONObject(raw).toProgress() }.getOrDefault(PlayerProgress())
    }

    private fun readBadges(userId: Int?): List<Badge> {
        val raw = prefs.getString("badges_${scope(userId)}", null) ?: return emptyList()
        return runCatching {
            val array = JSONArray(raw)
            (0 until array.length()).map { array.getJSONObject(it).toBadge() }
        }.getOrDefault(emptyList())
    }

    private fun PlayerProgress.toJson() = JSONObject().apply {
        put("totalPoints", totalPoints)
        put("roundsCompleted", roundsCompleted)
        put("currentStreak", currentStreak)
        put("bestStreak", bestStreak)
        put("perfectRounds", perfectRounds)
        put("solvedLevels", JSONArray().apply { solvedLevels.forEach { put(it) } })
        put("seaOfGreenRounds", seaOfGreenRounds)
        put("cleanRounds", cleanRounds)
        put("lastPlatings", lastPlatings)
        put("fastRounds", fastRounds)
        put("regionsSolved", JSONArray().apply { regionsSolved.forEach { put(it) } })
        put("dishesRead", JSONArray().apply { dishesRead.forEach { put(it) } })
    }

    private fun JSONObject.toProgress(): PlayerProgress {
        val levels = optJSONArray("solvedLevels") ?: JSONArray()
        // Absent on records written before the achievement badges existed,
        // so every new field reads back as its zero value rather than failing.
        val regions = optJSONArray("regionsSolved") ?: JSONArray()
        val read = optJSONArray("dishesRead") ?: JSONArray()
        return PlayerProgress(
            totalPoints = optInt("totalPoints"),
            roundsCompleted = optInt("roundsCompleted"),
            currentStreak = optInt("currentStreak"),
            bestStreak = optInt("bestStreak"),
            perfectRounds = optInt("perfectRounds"),
            solvedLevels = (0 until levels.length()).map { levels.getInt(it) }.toSet(),
            seaOfGreenRounds = optInt("seaOfGreenRounds"),
            cleanRounds = optInt("cleanRounds"),
            lastPlatings = optInt("lastPlatings"),
            fastRounds = optInt("fastRounds"),
            regionsSolved = (0 until regions.length()).map { regions.getString(it) }.toSet(),
            dishesRead = (0 until read.length()).map { read.getInt(it) }.toSet()
        )
    }

    private fun Badge.toJson() = JSONObject().apply {
        put("badge_id", badgeId)
        put("user_id", userId ?: JSONObject.NULL)
        put("badge_type", badgeType.name)
        put("title", title)
        put("milestone_criteria", milestoneCriteria)
        put("tx_hash", txHash ?: JSONObject.NULL)
        put("awarded_at", awardedAt)
    }

    private fun JSONObject.toBadge() = Badge(
        badgeId = getString("badge_id"),
        userId = if (isNull("user_id")) null else optInt("user_id"),
        badgeType = runCatching { BadgeType.valueOf(getString("badge_type")) }
            .getOrDefault(BadgeType.ROUNDS),
        title = optString("title"),
        milestoneCriteria = optString("milestone_criteria"),
        txHash = if (isNull("tx_hash")) null else optString("tx_hash"),
        awardedAt = optLong("awarded_at")
    )
}
