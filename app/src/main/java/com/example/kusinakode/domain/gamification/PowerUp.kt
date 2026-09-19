package com.example.kusinakode.domain.gamification

/**
 * In-round help. Priced in KKCoin only — XP/points never buy power-ups.
 */
enum class PowerUp(
    val title: String,
    val description: String,
    /** Legacy XP field; always 0. Shop currency is [coinCost]. */
    val pointCost: Int = 0,
    /** KK coins charged for this help. */
    val coinCost: Int = 0,
    /** Name sent to POST reward/spend.php; server fixes the price. */
    val spendKey: String = ""
) {
    REVEAL_LETTER(
        title = "Reveal",
        description = "Locks one correct letter into the grid",
        coinCost = 20,
        spendKey = "reveal"
    ),
    BOMB(
        title = "Bomb",
        description = "Blasts away wrong letters, leaving a few decoys",
        coinCost = 40,
        spendKey = "bomb"
    ),
    INSTANT_SOLVE(
        title = "Solve",
        description = "Completes the dish outright — priced high so it stays rare",
        coinCost = 100,
        spendKey = "solve"
    );

    val isCoinPriced: Boolean get() = coinCost > 0
}

object PowerUpRules {

    /**
     * Reveal and Bomb may both be used on one dish, but not back-to-back.
     * Instant Solve is separate: it ends the round, so it has no cooldown,
     * but it costs enough KK that it cannot be used on every dish.
     */
    const val HINT_COOLDOWN_MS = 60_000L

    /** One Reveal per dish — stacking letters would solve short words. */
    const val MAX_REVEALS_PER_ROUND = 1

    fun cooldownRemainingMs(lastHintUsedAtMs: Long?, nowMs: Long = System.currentTimeMillis()): Long {
        if (lastHintUsedAtMs == null || lastHintUsedAtMs <= 0L) return 0L
        return (lastHintUsedAtMs + HINT_COOLDOWN_MS - nowMs).coerceAtLeast(0L)
    }

    fun formatCooldown(remainingMs: Long): String {
        val totalSec = ((remainingMs + 999L) / 1000L).toInt().coerceAtLeast(0)
        return "%d:%02d".format(totalSec / 60, totalSec % 60)
    }

    /**
     * Wrong letters deliberately left on the keyboard after a bomb, so the
     * board keeps some decoys ("panggulo") rather than solving itself.
     */
    const val DECOYS_LEFT_AFTER_BOMB = 5

    /**
     * Which wrong letters a bomb should clear: everything not in the answer
     * and not already ruled out, minus [DECOYS_LEFT_AFTER_BOMB] kept back.
     */
    fun lettersToBomb(
        answer: String,
        alreadyEliminated: Set<Char>,
        decoysToKeep: Int = DECOYS_LEFT_AFTER_BOMB
    ): Set<Char> {
        val inAnswer = answer.uppercase().toSet()
        val candidates = ('A'..'Z')
            .filter { it !in inAnswer && it !in alreadyEliminated }
        val keep = decoysToKeep.coerceAtMost(candidates.size)
        return candidates.shuffled().drop(keep).toSet()
    }
}
