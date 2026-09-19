package com.example.kusinakode.ui.game

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.kusinakode.KusinaKodeApp
import com.example.kusinakode.Session
import com.example.kusinakode.data.repository.DefaultProgressRepository
import com.example.kusinakode.data.repository.LocalLevelRepository
import com.example.kusinakode.data.repository.LocalRoundStateRepository
import com.example.kusinakode.data.repository.RemoteAttemptRepository
import com.example.kusinakode.data.repository.RemoteKkWallet
import com.example.kusinakode.domain.GameEvents
import com.example.kusinakode.domain.engine.WordleEngine
import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.domain.gamification.PowerUpRules
import com.example.kusinakode.domain.repository.PointsWallet
import com.example.kusinakode.domain.model.RoundCompleted
import com.example.kusinakode.domain.model.SavedRound
import com.example.kusinakode.domain.model.TileState
import com.example.kusinakode.domain.repository.AttemptRepository
import com.example.kusinakode.domain.repository.LevelRepository
import com.example.kusinakode.domain.repository.ProgressRepository
import com.example.kusinakode.domain.repository.RoundStateRepository
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.distinctUntilChanged
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

/**
 * Owns the whole round: grid, keyboard, timer, hints, win/lose. The screen
 * only renders [uiState] and forwards input — no game rules live in the UI.
 */
class GameViewModel(
    private val levelNumber: Int,
    levelRepository: LevelRepository,
    private val attemptRepository: AttemptRepository,
    private val progressRepository: ProgressRepository,
    private val wallet: PointsWallet? = null,
    private val roundStore: RoundStateRepository? = null
) : ViewModel() {

    private val puzzle = levelRepository.level(levelNumber)

    // Declared before _uiState: property initializers run in order, and
    // freshState() reads this.
    private val attempts = WordleEngine.attemptsFor(puzzle.wordLength)

    /** Power-ups spent in the current round, for the Clean Kitchen badge. */
    private var powerUpsUsedThisRound = 0

    private val _uiState = MutableStateFlow(restoredState() ?: freshState())
    val uiState: StateFlow<GameUiState> = _uiState.asStateFlow()

    private var coachJob: Job? = null

    init {
        viewModelScope.launch {
            while (true) {
                delay(1000L)
                val now = System.currentTimeMillis()
                _uiState.update { s ->
                    val cooled = PowerUpRules.cooldownRemainingMs(s.lastHintUsedAtMs, now)
                    val next = if (!s.isPaused && s.isRoundActive) {
                        s.copy(elapsedSeconds = s.elapsedSeconds + 1)
                    } else s
                    if (next.hintCooldownRemainingMs == cooled) next
                    else next.copy(hintCooldownRemainingMs = cooled)
                }
            }
        }
        wallet?.let { w ->
            viewModelScope.launch {
                w.refresh()
                w.balance().collect { kk ->
                    _uiState.update { it.copy(pointsBalance = kk) }
                }
            }
        }
        roundStore?.let { store ->
            // Keep the board on disk while it's live, and drop it the moment
            // the round ends so a finished level never resumes.
            viewModelScope.launch {
                _uiState
                    .map { it.toSavedRound(levelNumber) to it.isRoundActive }
                    .distinctUntilChanged()
                    .collect { (snapshot, active) ->
                        if (active) store.save(snapshot) else store.clear(levelNumber)
                    }
            }
        }
    }

    /**
     * Rebuilds the board from a saved round, or returns null when there's
     * nothing worth resuming — no save, a pristine one, or a snapshot whose
     * shape no longer matches the level.
     */
    private fun restoredState(): GameUiState? {
        val saved = roundStore?.load(levelNumber) ?: return null
        if (!saved.isInProgress) return null
        if (saved.wordLength != puzzle.wordLength || saved.maxAttempts != attempts) return null
        if (saved.rows.size != attempts || saved.currentRow !in 0 until attempts) return null

        return freshState().copy(
            grid = saved.rows.map { row -> row.toList() },
            tileStates = saved.tileStates,
            keyStates = saved.keyStates,
            currentRow = saved.currentRow,
            elapsedSeconds = saved.elapsedSeconds,
            revealedPositions = saved.revealedPositions,
            revealsUsed = saved.revealsUsed,
            bombUsed = saved.bombUsed,
            lastHintUsedAtMs = saved.lastHintUsedAtMs,
            hintCooldownRemainingMs = PowerUpRules.cooldownRemainingMs(saved.lastHintUsedAtMs),
            powerUpsUsed = saved.revealsUsed + if (saved.bombUsed) 1 else 0
        )
    }

    private fun GameUiState.toSavedRound(levelId: Int) = SavedRound(
        levelId = levelId,
        wordLength = wordLength,
        maxAttempts = maxAttempts,
        rows = grid.map { it.joinToString("") },
        tileStates = tileStates,
        keyStates = keyStates,
        currentRow = currentRow,
        elapsedSeconds = elapsedSeconds,
        revealedPositions = revealedPositions,
        revealsUsed = revealsUsed,
        bombUsed = bombUsed,
        lastHintUsedAtMs = lastHintUsedAtMs
    )

    private fun freshState() = GameUiState(
        level = puzzle,
        maxAttempts = attempts,
        grid = List(attempts) { List(puzzle.wordLength) { ' ' } },
        tileStates = List(attempts) { List(puzzle.wordLength) { TileState.Empty } },
        keyStates = emptyMap(),
        currentRow = 0,
        elapsedSeconds = 0,
        isPaused = false,
        hasWon = false,
        isGameOver = false,
        encouragement = null
    )

    private val GameUiState.acceptingInput: Boolean
        get() = !isPaused && isRoundActive && currentRow < maxAttempts

    fun onKey(ch: Char) {
        _uiState.update { s ->
            if (!s.acceptingInput || !ch.isLetter()) return@update s
            val row = s.grid[s.currentRow]
            // Revealed cells are locked, so typing skips over them.
            val slot = row.indices.firstOrNull { it !in s.revealedPositions && row[it] == ' ' }
                ?: return@update s
            s.copy(grid = s.grid.replaceCell(s.currentRow, slot, ch.uppercaseChar()))
        }
    }

    fun onBackspace() {
        _uiState.update { s ->
            if (!s.acceptingInput) return@update s
            val row = s.grid[s.currentRow]
            val slot = row.indices.lastOrNull { it !in s.revealedPositions && row[it] != ' ' }
                ?: return@update s
            s.copy(grid = s.grid.replaceCell(s.currentRow, slot, ' '))
        }
    }

    fun onEnter() {
        val s = _uiState.value
        if (!s.acceptingInput) return
        val guess = s.grid[s.currentRow].joinToString("")
        // Letter input validation: incomplete/invalid rows are rejected, not evaluated.
        if (WordleEngine.validate(guess, s.wordLength) != WordleEngine.GuessValidation.Valid) return

        val verdict = WordleEngine.evaluate(puzzle.answer, guess)
        val won = WordleEngine.isWinningVerdict(verdict)
        val outOfRows = !won && s.currentRow == s.maxAttempts - 1
        val timeTakenMs = s.elapsedSeconds * 1000L
        val coaching = if (won || outOfRows) null else coachingFor(
            verdict = verdict,
            attemptsLeft = s.maxAttempts - s.currentRow - 1,
            canAffordReveal = !s.hintOnCooldown && s.revealsLeft > 0 &&
                s.pointsBalance >= PowerUp.REVEAL_LETTER.coinCost
        )

        _uiState.update { cur ->
            val keyStates = cur.keyStates.toMutableMap()
            verdict.forEachIndexed { i, ts ->
                val key = guess[i]
                val old = keyStates[key] ?: TileState.Empty
                if (ts.ordinal > old.ordinal) keyStates[key] = ts
            }
            val nextRow = if (won || outOfRows) cur.currentRow else cur.currentRow + 1
            // Carry locked reveals into the next row so they stay solved.
            val grid = if (nextRow != cur.currentRow && cur.revealedPositions.isNotEmpty()) {
                cur.grid.mapIndexed { r, cells ->
                    if (r != nextRow) cells
                    else cells.mapIndexed { c, ch -> cur.revealedPositions[c] ?: ch }
                }
            } else cur.grid
            val tiles = if (nextRow != cur.currentRow && cur.revealedPositions.isNotEmpty()) {
                cur.tileStates.replaceRow(cur.currentRow, verdict).mapIndexed { r, cells ->
                    if (r != nextRow) cells
                    else cells.mapIndexed { c, st ->
                        if (c in cur.revealedPositions) TileState.Correct else st
                    }
                }
            } else cur.tileStates.replaceRow(cur.currentRow, verdict)

            cur.copy(
                grid = grid,
                tileStates = tiles,
                keyStates = keyStates,
                hasWon = won,
                isGameOver = outOfRows,
                currentRow = nextRow,
                encouragement = coaching?.first,
                encouragementTone = coaching?.second ?: cur.encouragementTone
            )
        }

        // Coaching is a nudge, not a fixture — retire it so the board stays clean.
        coachJob?.cancel()
        if (!won && !outOfRows) {
            coachJob = viewModelScope.launch {
                delay(4000)
                _uiState.update { it.copy(encouragement = null) }
            }
        }

        viewModelScope.launch {
            attemptRepository.recordAttempt(levelNumber, guess, won, timeTakenMs)
            if (won) {
                repeat(4) {
                    delay(500)
                    wallet?.refresh()
                }
            }
        }
        if (won) {
            viewModelScope.launch { progressRepository.onLevelCompleted(levelNumber) }
        }
        if (won || outOfRows) {
            GameEvents.publish(
                RoundCompleted(
                    levelId = levelNumber,
                    finalGuess = guess,
                    isCorrect = won,
                    timeTakenMs = timeTakenMs,
                    attemptsUsed = s.currentRow + 1,
                    attemptsAllowed = s.maxAttempts,
                    region = puzzle.region.name,
                    // Any yellow anywhere on the board rules out Sea of Green.
                    usedPresentTile = s.tileStates.any { row ->
                        row.any { it == TileState.SemiCorrect }
                    },
                    powerUpsUsed = powerUpsUsedThisRound,
                    wasInstantSolve = false
                )
            )
        }
    }

    /**
     * Pays Instant Solve KK, fills the current row with the answer, and ends
     * the round. Scored as a last-row win so it cannot farm Perfect badges.
     */
    fun useSolve() {
        val s = _uiState.value
        if (!s.isRoundActive) return
        viewModelScope.launch {
            val cur = _uiState.value
            if (!cur.isRoundActive) return@launch
            if (!charge(PowerUp.INSTANT_SOLVE)) return@launch

            val answer = puzzle.answer
            val row = cur.currentRow
            val filled = cur.grid.mapIndexed { r, cells ->
                if (r != row) cells else answer.map { it }
            }
            val tiles = cur.tileStates.replaceRow(row, List(cur.wordLength) { TileState.Correct })
            val keys = cur.keyStates + answer.toSet().associateWith { TileState.Correct }
            val timeTakenMs = cur.elapsedSeconds * 1000L

            _uiState.update {
                it.copy(
                    grid = filled,
                    tileStates = tiles,
                    keyStates = keys,
                    hasWon = true,
                    encouragement = null,
                    powerUpMessage = "Solved for ${PowerUp.INSTANT_SOLVE.coinCost} KK"
                )
            }

            attemptRepository.recordAttempt(levelNumber, answer, true, timeTakenMs)
            progressRepository.onLevelCompleted(levelNumber)
            GameEvents.publish(
                RoundCompleted(
                    levelId = levelNumber,
                    finalGuess = answer,
                    isCorrect = true,
                    timeTakenMs = timeTakenMs,
                    attemptsUsed = cur.maxAttempts,
                    attemptsAllowed = cur.maxAttempts,
                    region = puzzle.region.name,
                    usedPresentTile = cur.tileStates.any { row ->
                        row.any { it == TileState.SemiCorrect }
                    },
                    powerUpsUsed = powerUpsUsedThisRound,
                    // The board was filled in, not beaten.
                    wasInstantSolve = true
                )
            )
            repeat(4) {
                delay(500)
                wallet?.refresh()
            }
        }
    }

    fun setPaused(paused: Boolean) {
        _uiState.update { s -> if (s.isRoundActive) s.copy(isPaused = paused) else s }
    }

    fun restart() {
        coachJob?.cancel()
        powerUpsUsedThisRound = 0
        roundStore?.clear(levelNumber)
        _uiState.value = freshState()
    }

    fun dismissPowerUpMessage() {
        _uiState.update { it.copy(powerUpMessage = null) }
    }

    /**
     * Reveals one correct letter straight into the grid, locked in place and
     * shown green. Paid in KKCoin, never XP.
     */
    fun useReveal() {
        val s = _uiState.value
        if (!s.isRoundActive || s.fullyRevealed || s.revealsLeft <= 0) return
        if (s.hintOnCooldown) {
            _uiState.update {
                it.copy(powerUpMessage = "Next hint in ${PowerUpRules.formatCooldown(it.hintCooldownRemainingMs)}")
            }
            return
        }

        viewModelScope.launch {
            val cur = _uiState.value
            if (!cur.isRoundActive || cur.hintOnCooldown || cur.revealsLeft <= 0) return@launch

            val known = cur.knownCorrectColumns
            val alreadyTyped = cur.grid[cur.currentRow].mapIndexedNotNull { column, ch ->
                if (ch != ' ' && ch == puzzle.answer.getOrNull(column)) column else null
            }
            val position = cur.grid[cur.currentRow].indices
                .filter { it !in known && it !in alreadyTyped }
                .randomOrNull()
            if (position == null) {
                _uiState.update {
                    it.copy(powerUpMessage = "Those letters are already green.")
                }
                return@launch
            }

            if (!charge(PowerUp.REVEAL_LETTER)) return@launch

            val letter = puzzle.answer[position]
            _uiState.update { state ->
                state.copy(
                    revealedPositions = state.revealedPositions + (position to letter),
                    revealsUsed = state.revealsUsed + 1,
                    lastHintUsedAtMs = System.currentTimeMillis(),
                    hintCooldownRemainingMs = PowerUpRules.HINT_COOLDOWN_MS,
                    grid = state.grid.replaceCell(state.currentRow, position, letter),
                    tileStates = state.tileStates.replaceCellState(
                        state.currentRow,
                        position,
                        TileState.Correct
                    ),
                    keyStates = state.keyStates + (letter to TileState.Correct),
                    justRevealed = position,
                    powerUpMessage = "Revealed $letter in position ${position + 1}"
                )
            }
            delay(1200)
            _uiState.update { it.copy(justRevealed = null) }
        }
    }

    /**
     * Clears wrong letters off the keyboard but deliberately leaves a few
     * decoys, so the board still takes thinking.
     */
    fun useBomb() {
        val s = _uiState.value
        if (!s.isRoundActive || s.bombUsed) return
        if (s.hintOnCooldown) {
            _uiState.update {
                it.copy(powerUpMessage = "Next hint in ${PowerUpRules.formatCooldown(it.hintCooldownRemainingMs)}")
            }
            return
        }

        viewModelScope.launch {
            val cur = _uiState.value
            if (cur.hintOnCooldown || cur.bombUsed) return@launch
            if (!charge(PowerUp.BOMB)) return@launch

            var blastedCount = 0
            _uiState.update { state ->
                val alreadyOut = state.keyStates.filterValues { it == TileState.Wrong }.keys
                val blasted = PowerUpRules.lettersToBomb(puzzle.answer, alreadyOut)
                blastedCount = blasted.size
                state.copy(
                    bombUsed = true,
                    lastHintUsedAtMs = System.currentTimeMillis(),
                    hintCooldownRemainingMs = PowerUpRules.HINT_COOLDOWN_MS,
                    keyStates = cur.keyStates + blasted.associateWith { TileState.Wrong },
                    justBombed = blasted,
                    powerUpMessage = "Bomb cleared ${blasted.size} letters"
                )
            }
            if (blastedCount > 0) {
                // Hold the flag long enough for the keys to finish exploding.
                delay(1000)
                _uiState.update { it.copy(justBombed = emptySet()) }
            }
        }
    }

    /** Charges KKCoin for a power-up. XP/points are never debited. */
    private suspend fun charge(powerUp: PowerUp): Boolean {
        val cost = powerUp.coinCost
        if (cost <= 0) {
            // Free power-ups still count as help taken.
            powerUpsUsedThisRound++
            _uiState.update { it.copy(powerUpsUsed = it.powerUpsUsed + 1) }
            return true
        }
        val paid = wallet?.spend(powerUp) ?: false
        if (paid) {
            powerUpsUsedThisRound++
            _uiState.update { it.copy(powerUpsUsed = it.powerUpsUsed + 1) }
        }
        if (!paid) {
            _uiState.update {
                it.copy(powerUpMessage = "Need $cost KK for ${powerUp.title}")
            }
        }
        return paid
    }

    /**
     * Picks a coaching line for a guess that didn't win, so a cold streak
     * reads as "keep going" rather than silent failure.
     */
    private fun coachingFor(
        verdict: List<TileState>,
        attemptsLeft: Int,
        canAffordReveal: Boolean
    ): Pair<String, CoachTone> {
        val correct = verdict.count { it == TileState.Correct }
        val misplaced = verdict.count { it == TileState.SemiCorrect }
        val half = (verdict.size + 1) / 2

        return when {
            attemptsLeft == 1 ->
                "Last try, Chef — make it count!" to CoachTone.Urgent

            correct == 0 && misplaced == 0 -> listOf(
                "Cold pan! None of those letters are in the dish — that rules a lot out.",
                "No luck there, but you just eliminated ${verdict.size} letters. Progress!",
                "Not this time. Try a fresh set of vowels.",
                if (canAffordReveal) "Stuck? Spend KK on a Reveal to lock in a letter."
                else "Shake it off and try a completely different word."
            ).random() to CoachTone.Cold

            correct == 0 && misplaced > 0 -> listOf(
                "Right ingredients, wrong order — shuffle them around!",
                "Those letters belong here, just not there. Keep rearranging!"
            ).random() to CoachTone.Shuffle

            correct >= half ->
                "So close! You're almost plating this one." to CoachTone.Close

            else -> listOf(
                "Now we're cooking — you found something!",
                "Good pick. Build on the letters that stuck.",
                "Warming up! Keep that green letter where it is."
            ).random() to CoachTone.Progress
        }
    }

    private fun List<List<Char>>.replaceCell(row: Int, col: Int, value: Char): List<List<Char>> =
        mapIndexed { r, cells ->
            if (r != row) cells
            else cells.mapIndexed { c, old -> if (c == col) value else old }
        }

    private fun List<List<TileState>>.replaceRow(row: Int, values: List<TileState>): List<List<TileState>> =
        mapIndexed { r, cells -> if (r == row) values else cells }

    private fun List<List<TileState>>.replaceCellState(
        row: Int,
        col: Int,
        value: TileState
    ): List<List<TileState>> =
        mapIndexed { r, cells ->
            if (r != row) cells
            else cells.mapIndexed { c, old -> if (c == col) value else old }
        }

    class Factory(
        private val level: Int,
        private val context: Context
    ) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            val app = context.applicationContext as? KusinaKodeApp
            @Suppress("UNCHECKED_CAST")
            return GameViewModel(
                levelNumber = level,
                levelRepository = LocalLevelRepository(),
                attemptRepository = RemoteAttemptRepository(),
                progressRepository = DefaultProgressRepository(context),
                wallet = app?.let { RemoteKkWallet() },
                roundStore = LocalRoundStateRepository(context) { Session.userId }
            ) as T
        }
    }

}
