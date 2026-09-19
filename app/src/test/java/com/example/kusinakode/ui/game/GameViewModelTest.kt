package com.example.kusinakode.ui.game

import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.domain.model.PuzzleLevel
import com.example.kusinakode.domain.model.SavedRound
import com.example.kusinakode.domain.model.TileState
import com.example.kusinakode.domain.repository.AttemptRepository
import com.example.kusinakode.domain.repository.LevelRepository
import com.example.kusinakode.domain.repository.PointsWallet
import com.example.kusinakode.domain.repository.ProgressRepository
import com.example.kusinakode.domain.repository.RoundStateRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class GameViewModelTest {

    private val dispatcher = StandardTestDispatcher()

    private class FakeLevelRepository(private val answer: String) : LevelRepository {
        override val levelCount = 20
        override fun level(number: Int) =
            PuzzleLevel(number, answer.uppercase(), "Test", "trivia", imageRes = 0, cardRes = 0)
    }

    private class RecordingAttemptRepository : AttemptRepository {
        data class Recorded(val levelId: Int, val guess: String, val isCorrect: Boolean, val timeTakenMs: Long)
        val recorded = mutableListOf<Recorded>()
        override suspend fun recordAttempt(levelId: Int, guess: String, isCorrect: Boolean, timeTakenMs: Long) {
            recorded += Recorded(levelId, guess, isCorrect, timeTakenMs)
        }
    }

    private class RecordingProgressRepository : ProgressRepository {
        val completedLevels = mutableListOf<Int>()
        override suspend fun onLevelCompleted(levelId: Int) {
            completedLevels += levelId
        }
    }

    /** In-memory stand-in for the SharedPreferences-backed round store. */
    private class InMemoryRoundStore : RoundStateRepository {
        val saved = mutableMapOf<Int, SavedRound>()
        var clears = 0
        override fun load(levelId: Int) = saved[levelId]
        override fun save(round: SavedRound) {
            saved[round.levelId] = round
        }

        override fun clear(levelId: Int) {
            clears++
            saved.remove(levelId)
        }
    }

    private class FakeWallet(start: Int = 200) : PointsWallet {
        private val _bal = MutableStateFlow(start)
        override fun balance() = _bal.asStateFlow()
        override suspend fun spend(powerUp: PowerUp): Boolean {
            val cost = powerUp.coinCost
            if (_bal.value < cost) return false
            _bal.value -= cost
            return true
        }
        override suspend fun refresh() {}
    }

    private lateinit var attempts: RecordingAttemptRepository
    private lateinit var progress: RecordingProgressRepository

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        attempts = RecordingAttemptRepository()
        progress = RecordingProgressRepository()
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    private fun viewModel(
        answer: String = "ADOBO",
        level: Int = 1,
        roundStore: RoundStateRepository? = null,
        wallet: PointsWallet? = null
    ) = GameViewModel(
        levelNumber = level,
        levelRepository = FakeLevelRepository(answer),
        attemptRepository = attempts,
        progressRepository = progress,
        wallet = wallet,
        roundStore = roundStore
    )

    private fun GameViewModel.type(word: String) = word.forEach { onKey(it) }

    @Test
    fun `grid dimensions follow word length, not a fixed 6x6`() {
        val vm = viewModel(answer = "SINIGANG")
        val state = vm.uiState.value
        assertEquals(8, state.wordLength)
        assertTrue(state.grid.all { it.size == 8 })

        val vm5 = viewModel(answer = "ADOBO")
        assertTrue(vm5.uiState.value.grid.all { it.size == 5 })
    }

    @Test
    fun `incomplete guess is not evaluated or recorded`() {
        val vm = viewModel()
        vm.type("ADO")
        vm.onEnter()
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertEquals(0, state.currentRow)
        assertTrue(state.tileStates[0].all { it == TileState.Empty })
        assertTrue(attempts.recorded.isEmpty())
    }

    @Test
    fun `every submitted guess is recorded with level and correctness`() {
        val vm = viewModel()
        vm.type("BOMBA")
        vm.onEnter()
        vm.type("ADOBO")
        vm.onEnter()
        dispatcher.scheduler.runCurrent()

        assertEquals(2, attempts.recorded.size)
        assertEquals(false, attempts.recorded[0].isCorrect)
        assertEquals("BOMBA", attempts.recorded[0].guess)
        assertEquals(true, attempts.recorded[1].isCorrect)
        assertEquals(1, attempts.recorded[1].levelId)
    }

    @Test
    fun `correct guess wins the round and completes the level`() {
        val vm = viewModel(answer = "PAKBET", level = 5)
        vm.type("PAKBET")
        vm.onEnter()
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertTrue(state.hasWon)
        assertFalse(state.isGameOver)
        assertTrue(state.tileStates[0].all { it == TileState.Correct })
        assertEquals(listOf(5), progress.completedLevels)
    }

    @Test
    fun `round ends in game over after max attempts of wrong guesses`() {
        val vm = viewModel(answer = "ADOBO")
        val rows = vm.uiState.value.maxAttempts
        repeat(rows) {
            vm.type("BOMBA")
            vm.onEnter()
        }
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertTrue(state.isGameOver)
        assertFalse(state.hasWon)
        assertEquals(rows, attempts.recorded.size)
        assertTrue(progress.completedLevels.isEmpty())
    }

    @Test
    fun `board is six rows tall regardless of word length`() {
        assertEquals(6, viewModel(answer = "ADOBO").uiState.value.maxAttempts)
        assertEquals(6, viewModel(answer = "PAKBET").uiState.value.maxAttempts)
        assertEquals(6, viewModel(answer = "SINIGANG").uiState.value.maxAttempts)
    }

    @Test
    fun `attempt duration is reported in milliseconds`() {
        val vm = viewModel()
        dispatcher.scheduler.advanceTimeBy(3_001)
        vm.type("ADOBO")
        vm.onEnter()
        dispatcher.scheduler.runCurrent()

        assertEquals(3_000L, attempts.recorded.single().timeTakenMs)
    }

    @Test
    fun `no input accepted after the round ends`() {
        val vm = viewModel()
        vm.type("ADOBO")
        vm.onEnter()
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.hasWon)

        vm.onKey('X')
        vm.onEnter()
        dispatcher.scheduler.runCurrent()
        assertEquals(1, attempts.recorded.size)
    }

    @Test
    fun `restart resets the round completely`() {
        val vm = viewModel()
        vm.type("BOMBA")
        vm.onEnter()
        dispatcher.scheduler.runCurrent()
        vm.restart()

        val state = vm.uiState.value
        assertEquals(0, state.currentRow)
        assertTrue(state.grid.all { row -> row.all { it == ' ' } })
        assertTrue(state.tileStates.all { row -> row.all { it == TileState.Empty } })
        assertTrue(state.keyStates.isEmpty())
        assertEquals(0, state.elapsedSeconds)
    }

    @Test
    fun `timer ticks only while unpaused and round is active`() {
        val vm = viewModel()
        dispatcher.scheduler.advanceTimeBy(3_001)
        assertEquals(3, vm.uiState.value.elapsedSeconds)

        vm.setPaused(true)
        dispatcher.scheduler.advanceTimeBy(5_000)
        assertEquals(3, vm.uiState.value.elapsedSeconds)

        vm.setPaused(false)
        dispatcher.scheduler.advanceTimeBy(2_000)
        assertEquals(5, vm.uiState.value.elapsedSeconds)
    }

    @Test
    fun `leaving mid-round and coming back resumes the same board`() {
        val store = InMemoryRoundStore()

        val first = viewModel(roundStore = store)
        first.type("PANCI")
        first.onEnter()
        dispatcher.scheduler.advanceTimeBy(4_000)
        dispatcher.scheduler.runCurrent()

        val left = first.uiState.value
        assertEquals("guess should have advanced the board", 1, left.currentRow)

        // Navigating away destroys the ViewModel; opening the level again
        // builds a new one against the same store.
        val resumed = viewModel(roundStore = store).uiState.value

        assertEquals(left.currentRow, resumed.currentRow)
        assertEquals(left.grid, resumed.grid)
        assertEquals(left.tileStates, resumed.tileStates)
        assertEquals(left.keyStates, resumed.keyStates)
        assertEquals("the clock should not restart either", left.elapsedSeconds, resumed.elapsedSeconds)
    }

    @Test
    fun `a finished round is dropped rather than resumed`() {
        val store = InMemoryRoundStore()

        val vm = viewModel(roundStore = store)
        vm.type("ADOBO")
        vm.onEnter()
        dispatcher.scheduler.runCurrent()
        assertTrue(vm.uiState.value.hasWon)

        assertTrue("a won round must not stay on disk", store.saved.isEmpty())

        val replay = viewModel(roundStore = store).uiState.value
        assertEquals(0, replay.currentRow)
        assertFalse(replay.hasWon)
        assertTrue(replay.grid.all { row -> row.all { it == ' ' } })
    }

    @Test
    fun `an untouched board is not treated as something to resume`() {
        val store = InMemoryRoundStore()
        viewModel(roundStore = store)
        dispatcher.scheduler.runCurrent()

        // Whatever was written, a pristine snapshot must not restore.
        val fresh = viewModel(roundStore = store).uiState.value
        assertEquals(0, fresh.currentRow)
        assertEquals(0, fresh.elapsedSeconds)
    }

    @Test
    fun `a snapshot from a different word length is discarded`() {
        val store = InMemoryRoundStore()

        val old = viewModel(answer = "ADOBO", roundStore = store)
        old.type("PANCI")
        old.onEnter()
        dispatcher.scheduler.runCurrent()
        assertTrue(store.saved.containsKey(1))

        // Module 4 re-seeds the dataset and level 1 becomes an 8-letter dish.
        val rebuilt = viewModel(answer = "SINIGANG", roundStore = store).uiState.value
        assertEquals(0, rebuilt.currentRow)
        assertTrue(rebuilt.grid.all { it.size == 8 })
    }

    @Test
    fun `instant solve fills the answer and ends the round`() {
        val vm = viewModel(wallet = FakeWallet(PowerUp.INSTANT_SOLVE.coinCost))
        vm.useSolve()
        dispatcher.scheduler.runCurrent()

        val state = vm.uiState.value
        assertTrue(state.hasWon)
        assertEquals("ADOBO", state.grid[0].joinToString(""))
        assertTrue(state.tileStates[0].all { it == TileState.Correct })
        assertEquals(listOf(1), progress.completedLevels)
        assertTrue(attempts.recorded.single().isCorrect)
    }

    @Test
    fun `instant solve does nothing without enough KK`() {
        val vm = viewModel(wallet = FakeWallet(PowerUp.INSTANT_SOLVE.coinCost - 1))
        vm.useSolve()
        dispatcher.scheduler.runCurrent()

        assertFalse(vm.uiState.value.hasWon)
        assertTrue(progress.completedLevels.isEmpty())
    }

    @Test
    fun `reveal does not lock a column already marked green`() {
        val vm = viewModel(answer = "ADOBO", wallet = FakeWallet(200))
        vm.type("ADXZZ")
        vm.onEnter()
        dispatcher.scheduler.runCurrent()

        assertEquals(TileState.Correct, vm.uiState.value.tileStates[0][0])
        assertEquals(TileState.Correct, vm.uiState.value.tileStates[0][1])

        vm.useReveal()
        dispatcher.scheduler.runCurrent()

        val revealed = vm.uiState.value.revealedPositions
        assertEquals(1, revealed.size)
        assertFalse("must not re-reveal a letter already seen green", 0 in revealed)
        assertFalse("must not re-reveal a letter already seen green", 1 in revealed)
        assertTrue(revealed.keys.single() in 2..4)
        assertEquals(' ', vm.uiState.value.grid[1][0])
        assertEquals(' ', vm.uiState.value.grid[1][1])
    }

    @Test
    fun `reveal does not spend KK when every column is already green`() {
        val vm = viewModel(answer = "ADOBO", wallet = FakeWallet(200))
        dispatcher.scheduler.runCurrent()
        val before = vm.uiState.value.pointsBalance

        listOf("AZZZZ", "ZDZZZ", "ZZOZZ", "ZZZBZ", "ZZZZO").forEach { guess ->
            vm.type(guess)
            vm.onEnter()
            dispatcher.scheduler.runCurrent()
        }

        assertTrue(vm.uiState.value.fullyRevealed)
        assertTrue(vm.uiState.value.isRoundActive)

        vm.useReveal()
        dispatcher.scheduler.runCurrent()

        assertEquals(before, vm.uiState.value.pointsBalance)
        assertTrue(vm.uiState.value.revealedPositions.isEmpty())
    }
}
