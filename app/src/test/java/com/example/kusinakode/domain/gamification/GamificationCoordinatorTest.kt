package com.example.kusinakode.domain.gamification

import com.example.kusinakode.domain.model.Badge
import com.example.kusinakode.domain.model.PlayerProgress
import com.example.kusinakode.domain.model.RoundCompleted
import com.example.kusinakode.domain.repository.GamificationRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GamificationCoordinatorTest {

    private class FakeRepo(
        initialProgress: PlayerProgress = PlayerProgress()
    ) : GamificationRepository {
        val progressState = MutableStateFlow(initialProgress)
        val badgeState = MutableStateFlow<List<Badge>>(emptyList())
        var seeded = false

        override fun progress(userId: Int?): Flow<PlayerProgress> = progressState
        override fun badges(userId: Int?): Flow<List<Badge>> = badgeState
        override suspend fun saveProgress(userId: Int?, progress: PlayerProgress) {
            progressState.value = progress
        }

        override suspend fun awardBadges(userId: Int?, badges: List<Badge>) {
            val held = badgeState.value.map { it.badgeId }.toSet()
            badgeState.value = badgeState.value + badges.filter { it.badgeId !in held }
        }

        override suspend fun markBadgeConfirmed(userId: Int?, badgeId: String, txHash: String) = Unit

        override suspend fun replaceFromServer(
            userId: Int?,
            progress: PlayerProgress,
            badges: List<Badge>
        ) {
            seeded = true
            progressState.value = progress
            badgeState.value = badges
        }

        override suspend fun spendPoints(userId: Int?, amount: Int): Boolean {
            val current = progressState.value
            if (current.totalPoints < amount) return false
            progressState.value = current.copy(totalPoints = current.totalPoints - amount)
            return true
        }

        // Mirrors LocalGamificationRepository: credits the base award for
        // dishes cleared before the points system existed.
        override suspend fun ensureSeeded(userId: Int?, solvedLevels: Set<Int>) {
            if (seeded || solvedLevels.isEmpty()) return
            seeded = true
            val current = progressState.value
            val newly = solvedLevels - current.solvedLevels
            if (newly.isEmpty()) return
            progressState.value = current.copy(
                solvedLevels = current.solvedLevels + newly,
                roundsCompleted = current.roundsCompleted + newly.size,
                totalPoints = current.totalPoints + newly.size * ScoreRules.BASE_POINTS
            )
        }
    }

    private fun win(levelId: Int) = RoundCompleted(
        levelId = levelId,
        finalGuess = "ADOBO",
        isCorrect = true,
        timeTakenMs = 200_000,
        attemptsUsed = 3,
        attemptsAllowed = 6
    )

    /** Stands in for the backend: may or may not already know this account. */
    private class FakeSync(
        private val remoteProgress: PlayerProgress? = null,
        private val remoteBadges: List<Badge> = emptyList()
    ) : GamificationSync {
        val pushedProgress = mutableListOf<PlayerProgress>()
        override suspend fun pushProgress(userId: Int, progress: PlayerProgress) {
            pushedProgress += progress
        }

        override suspend fun pushBadge(userId: Int, badge: Badge) = Unit
        override suspend fun fetchProgress(userId: Int) = remoteProgress
        override suspend fun fetchBadges(userId: Int) = remoteBadges
    }

    /**
     * A backend that actually remembers what the device pushed, so a test can
     * spend and then sync the way the app does.
     */
    private class MirrorSync(var stored: PlayerProgress?) : GamificationSync {
        override suspend fun pushProgress(userId: Int, progress: PlayerProgress) {
            stored = progress
        }

        override suspend fun pushBadge(userId: Int, badge: Badge) = Unit
        override suspend fun fetchProgress(userId: Int) = stored
        override suspend fun fetchBadges(userId: Int) = emptyList<Badge>()
    }

    private fun coordinator(
        repo: FakeRepo,
        previouslySolved: Set<Int> = emptySet(),
        sync: GamificationSync? = null
    ) = GamificationCoordinator(
        repository = repo,
        currentUserId = { 1 },
        previouslySolved = { previouslySolved },
        sync = sync
    )

    @Test
    fun `signing in on a new device restores the account's real state`() = runTest {
        // Fresh install: nothing stored locally, but the account has history.
        val repo = FakeRepo()
        val server = PlayerProgress(
            totalPoints = 940,
            roundsCompleted = 16,
            currentStreak = 7,
            bestStreak = 7,
            perfectRounds = 2,
            solvedLevels = (1..16).toSet()
        )
        val coordinator = coordinator(
            repo,
            // The device would otherwise recompute a wrong baseline from these.
            previouslySolved = (1..16).toSet(),
            sync = FakeSync(remoteProgress = server)
        )

        coordinator.refresh()

        val restored = repo.progressState.value
        assertEquals("points must match the account, not a local recompute", 940, restored.totalPoints)
        assertEquals(7, restored.currentStreak)
        assertEquals(2, restored.perfectRounds)
        assertEquals((1..16).toSet(), restored.solvedLevels)
    }

    @Test
    fun `an account the server has never seen still bootstraps locally`() = runTest {
        val repo = FakeRepo()
        val sync = FakeSync(remoteProgress = null)
        val coordinator = coordinator(repo, previouslySolved = (1..4).toSet(), sync = sync)

        coordinator.refresh()

        assertEquals(4 * ScoreRules.BASE_POINTS, repo.progressState.value.totalPoints)
        assertTrue("the bootstrapped total should be pushed up", sync.pushedProgress.isNotEmpty())
    }

    @Test
    fun `restored solved levels still block replay farming`() = runTest {
        val repo = FakeRepo()
        val server = PlayerProgress(
            totalPoints = 500,
            roundsCompleted = 5,
            solvedLevels = (1..5).toSet()
        )
        val coordinator = coordinator(repo, sync = FakeSync(remoteProgress = server))

        coordinator.refresh()
        coordinator.handle(win(3))

        assertEquals(500, repo.progressState.value.totalPoints)
    }

    @Test
    fun `replaying the same level does not award points twice`() = runTest {
        val repo = FakeRepo()
        val coordinator = coordinator(repo)

        coordinator.handle(win(1))
        val afterFirst = repo.progressState.value

        coordinator.handle(win(1))
        val afterReplay = repo.progressState.value

        assertEquals(afterFirst.totalPoints, afterReplay.totalPoints)
        assertEquals(1, afterReplay.roundsCompleted)
    }

    @Test
    fun `dishes solved before gamification existed are credited once`() = runTest {
        // The player already cleared levels 1-19 under the old build.
        val repo = FakeRepo()
        val coordinator = coordinator(repo, previouslySolved = (1..19).toSet())

        coordinator.refresh()
        val seeded = repo.progressState.value
        assertEquals(19 * ScoreRules.BASE_POINTS, seeded.totalPoints)
        assertEquals(19, seeded.roundsCompleted)

        // Replaying one of them must not pay out again.
        coordinator.handle(win(5))
        assertEquals(seeded.totalPoints, repo.progressState.value.totalPoints)
        assertEquals(19, repo.progressState.value.roundsCompleted)
    }

    @Test
    fun `seeding grants the badges those old solves already earned`() = runTest {
        val repo = FakeRepo()
        val coordinator = coordinator(repo, previouslySolved = (1..16).toSet())

        coordinator.refresh()

        val ids = repo.badgeState.value.map { it.badgeId }
        assertTrue("16 solves should unlock the 1, 5 and 10 round badges",
            ids.containsAll(listOf("rounds_1", "rounds_5", "rounds_10")))
        assertTrue("but not the 20-round badge", "rounds_20" !in ids)
    }

    @Test
    fun `a genuinely new level still scores after seeding`() = runTest {
        val repo = FakeRepo()
        val coordinator = coordinator(repo, previouslySolved = (1..19).toSet())

        coordinator.handle(win(20))

        val progress = repo.progressState.value
        assertEquals(19 * ScoreRules.BASE_POINTS + 160, progress.totalPoints)
        assertEquals(20, progress.roundsCompleted)
    }

    @Test
    fun `badges are awarded once even across repeated rounds`() = runTest {
        val repo = FakeRepo()
        val coordinator = coordinator(repo)

        coordinator.handle(win(1))
        coordinator.handle(win(1))
        coordinator.handle(win(1))

        assertEquals(1, repo.badgeState.value.count { it.badgeId == "rounds_1" })
    }

    @Test
    fun `points spent on a power-up are not handed back by the next sync`() = runTest {
        val start = PlayerProgress(
            totalPoints = 300,
            roundsCompleted = 3,
            solvedLevels = setOf(1, 2, 3)
        )
        val repo = FakeRepo(start)
        val sync = MirrorSync(start)
        val coordinator = coordinator(repo, sync = sync)

        assertTrue(coordinator.spend(75))
        assertEquals(225, repo.progressState.value.totalPoints)
        assertEquals(
            "the debit has to reach the backend, or server-wins undoes it",
            225,
            sync.stored?.totalPoints
        )

        // Home refreshes whenever it opens.
        coordinator.refresh()

        assertEquals(225, repo.progressState.value.totalPoints)
    }

    @Test
    fun `a power-up the player cannot afford charges nothing`() = runTest {
        val start = PlayerProgress(totalPoints = 40, roundsCompleted = 1)
        val repo = FakeRepo(start)
        val sync = MirrorSync(start)
        val coordinator = coordinator(repo, sync = sync)

        assertFalse(coordinator.spend(75))
        assertEquals(40, repo.progressState.value.totalPoints)
        assertEquals(40, sync.stored?.totalPoints)
    }
}
