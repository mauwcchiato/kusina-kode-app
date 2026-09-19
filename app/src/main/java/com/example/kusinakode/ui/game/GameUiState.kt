package com.example.kusinakode.ui.game

import com.example.kusinakode.domain.gamification.PowerUpRules
import com.example.kusinakode.domain.model.PuzzleLevel
import com.example.kusinakode.domain.model.TileState

data class GameUiState(
    val level: PuzzleLevel,
    val maxAttempts: Int,
    /** maxAttempts rows × wordLength columns; ' ' = empty cell. */
    val grid: List<List<Char>>,
    val tileStates: List<List<TileState>>,
    val keyStates: Map<Char, TileState>,
    val currentRow: Int,
    val elapsedSeconds: Int,
    val isPaused: Boolean,
    val hasWon: Boolean,
    val isGameOver: Boolean,
    /** Coaching line shown after a guess that didn't win. */
    val encouragement: String? = null,
    /** What that line is reacting to, so the banner can dress itself to match. */
    val encouragementTone: CoachTone = CoachTone.Progress,
    /** Column → letter for positions a Reveal has locked into the grid. */
    val revealedPositions: Map<Int, Char> = emptyMap(),
    /** Reveals bought this round, capped by PowerUpRules. */
    val revealsUsed: Int = 0,
    /** True once a bomb has been used this round. */
    val bombUsed: Boolean = false,
    /** Wall-clock millis of the last Reveal or Bomb; drives the 1-minute wait. */
    val lastHintUsedAtMs: Long? = null,
    /** Remaining cooldown, refreshed once a second from the timer loop. */
    val hintCooldownRemainingMs: Long = 0L,
    /** KKCoin the player can spend on Reveal/Bomb. Not XP. */
    val pointsBalance: Int = 0,
    /** Transient note about a power-up, e.g. "Not enough points". */
    val powerUpMessage: String? = null,
    /** Reveal, Bomb, or Instant Solve spent this round. Palayok grant uses this. */
    val powerUpsUsed: Int = 0,
    /** Letters the bomb just cleared, held briefly to play the blast. */
    val justBombed: Set<Char> = emptySet(),
    /** Grid column a Reveal just filled, held briefly to play the burst. */
    val justRevealed: Int? = null
) {
    val wordLength: Int get() = level.wordLength
    val isRoundActive: Boolean get() = !hasWon && !isGameOver

    /** Reveals still available this round. */
    val revealsLeft: Int
        get() = (PowerUpRules.MAX_REVEALS_PER_ROUND - revealsUsed).coerceAtLeast(0)

    val hintOnCooldown: Boolean get() = hintCooldownRemainingMs > 0L

    /** Columns the player has already confirmed — by a guess or a prior Reveal. */
    val knownCorrectColumns: Set<Int>
        get() {
            val fromGuesses = tileStates.flatMap { row ->
                row.mapIndexedNotNull { column, tile ->
                    if (tile == TileState.Correct) column else null
                }
            }
            return fromGuesses.toSet() + revealedPositions.keys
        }

    /** The whole answer is already locked in (not just the one-hint cap). */
    val fullyRevealed: Boolean
        get() = knownCorrectColumns.size >= wordLength
}

/**
 * How a guess landed. The coaching banner takes its colour, icon and weight
 * from this, so a cold guess and a near-miss no longer read identically.
 */
enum class CoachTone {
    /** One row left — the loudest the banner gets. */
    Urgent,

    /** Nothing in the dish; the guess only ruled letters out. */
    Cold,

    /** Right letters, wrong places. */
    Shuffle,

    /** Something stuck, and the answer is in reach. */
    Close,

    /** Ordinary forward movement. */
    Progress
}
