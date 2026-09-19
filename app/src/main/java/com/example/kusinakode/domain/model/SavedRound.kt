package com.example.kusinakode.domain.model

/**
 * A round in progress, captured so leaving the game screen doesn't throw it
 * away. Only what the player actually earned during the round is kept —
 * transient things like coaching lines and power-up animations are rebuilt
 * fresh on resume.
 *
 * [wordLength] and [maxAttempts] are stored so a snapshot can be rejected if
 * the dish behind the level changed under it (Module 4 will re-seed the
 * dataset), rather than restoring a board that no longer fits.
 */
data class SavedRound(
    val levelId: Int,
    val wordLength: Int,
    val maxAttempts: Int,
    /** One string per row, ' ' for an empty cell. */
    val rows: List<String>,
    val tileStates: List<List<TileState>>,
    val keyStates: Map<Char, TileState>,
    val currentRow: Int,
    val elapsedSeconds: Int,
    val revealedPositions: Map<Int, Char>,
    val revealsUsed: Int,
    val bombUsed: Boolean,
    val lastHintUsedAtMs: Long? = null
) {
    /** A round worth restoring — a pristine board is not worth the noise. */
    val isInProgress: Boolean
        get() = currentRow > 0 ||
            revealsUsed > 0 ||
            bombUsed ||
            elapsedSeconds > 0 ||
            rows.any { row -> row.any { it != ' ' } }
}
