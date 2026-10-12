package com.example.kusinakode.ui.leaderboard

import com.example.kusinakode.domain.model.LeaderboardBoard
import com.example.kusinakode.domain.model.LeaderboardRow
import com.example.kusinakode.domain.model.LeaderboardStanding
import com.example.kusinakode.domain.repository.LeaderboardRepository
import com.example.kusinakode.domain.repository.LeaderboardWindow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceUntilIdle
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class LeaderboardViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private val top = (1..10).map { LeaderboardRow(name = "cook$it", correctCount = 29, points = 9000 - it) }

    /** Answers with the top ten and, for the asking player, rank 57. */
    private class FakeRepo(
        private val rows: List<LeaderboardRow>,
        private val me: LeaderboardStanding?
    ) : LeaderboardRepository {
        var askedFor: Int? = null
        override suspend fun topPlayers(limit: Int, window: LeaderboardWindow) = Result.success(rows)
        override suspend fun board(limit: Int, window: LeaderboardWindow, userId: Int?): Result<LeaderboardBoard> {
            askedFor = userId
            return Result.success(LeaderboardBoard(rows, me.takeIf { userId != null }))
        }
    }

    @Before fun setUp() = Dispatchers.setMain(dispatcher)
    @After fun tearDown() = Dispatchers.resetMain()

    @Test
    fun playerOutsideTopTen_keepsTheirOwnRank() = runTest(dispatcher) {
        val mine = LeaderboardStanding(57, LeaderboardRow(name = "mau", correctCount = 12, points = 2400))
        val repo = FakeRepo(top, mine)
        val vm = LeaderboardViewModel(repo, currentUserId = { 22 })
        advanceUntilIdle()

        assertEquals(22, repo.askedFor)
        assertEquals(10, vm.uiState.value.entries.size)
        assertEquals(57, vm.uiState.value.me?.rank)
        assertEquals("mau", vm.uiState.value.me?.row?.name)
    }

    @Test
    fun signedOut_hasNoStanding() = runTest(dispatcher) {
        val repo = FakeRepo(top, LeaderboardStanding(57, LeaderboardRow("mau", 12, 2400)))
        val vm = LeaderboardViewModel(repo, currentUserId = { null })
        advanceUntilIdle()

        assertNull(vm.uiState.value.me)
    }

    @Test
    fun switchingBoards_dropsTheOldStanding() = runTest(dispatcher) {
        val repo = FakeRepo(top, LeaderboardStanding(57, LeaderboardRow("mau", 12, 2400)))
        val vm = LeaderboardViewModel(repo, currentUserId = { 22 })
        advanceUntilIdle()

        vm.selectWindow(LeaderboardWindow.Today)
        // Before the Today board answers, the all-time rank must not linger.
        assertNull(vm.uiState.value.me)
        advanceUntilIdle()
        assertEquals(57, vm.uiState.value.me?.rank)
    }
}
