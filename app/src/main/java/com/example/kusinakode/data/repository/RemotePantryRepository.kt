package com.example.kusinakode.data.repository

import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.api.PantrySnapshotData
import com.example.kusinakode.domain.ChainQueue
import com.example.kusinakode.domain.pantry.DrawResult
import com.example.kusinakode.domain.pantry.IngredientCatalog
import com.example.kusinakode.domain.pantry.PantryEntry
import com.example.kusinakode.domain.pantry.PantrySnapshot
import com.example.kusinakode.domain.pantry.SellResult
import com.example.kusinakode.domain.repository.PantryRepository
import com.example.kusinakode.domain.repository.TriviaSpinOutcome

class RemotePantryRepository : PantryRepository {

    override suspend fun snapshot(): Result<Pair<PantrySnapshot, Long>> = runCatching {
        val resp = KusinaApi.getPantrySnapshot()
        val data = resp.data ?: error(resp.message ?: "No pantry data")
        data.toSnapshot() to data.balance_kk
    }

    override suspend fun grant(levelId: Int, powerUpsUsed: Int): Result<Int> = runCatching {
        KusinaApi.pantryGrant(levelId, powerUpsUsed)
    }

    override suspend fun draw(levelId: Int?, hintLevelId: Int?): Result<Triple<DrawResult, PantrySnapshot, Long>> = runCatching {
        val resp = KusinaApi.pantryDraw(levelId, hintLevelId)
        val data = resp.data ?: error(resp.message ?: "The palayok was empty")
        val ingredient = data.ingredient_id?.let { IngredientCatalog.getOrStub(it) }
        val snap = (data.snapshot ?: error("Missing pantry snapshot")).toSnapshot()
        Triple(
            DrawResult(
                ingredient = ingredient,
                isDuplicate = data.is_duplicate,
                qty = data.qty,
                drawsLeft = data.draws_left,
                collected = data.collected,
                isHint = data.is_hint
            ),
            snap,
            data.balance_kk
        )
    }

    override suspend fun spin(): Result<Pair<Int, PantrySnapshot>> = runCatching {
        // Serialized with every other chain write: spin.php now walks a
        // palayok_spin receipt to confirmed so On-Chain Activity can list it.
        val data = ChainQueue.serialized { KusinaApi.pantrySpin() }
        val snap = (data.snapshot ?: error("Missing pantry snapshot")).toSnapshot()
        data.palayoks_won to snap
    }

    override suspend fun buySpin(): Result<Pair<PantrySnapshot, Long>> = runCatching {
        // Serialized with every other chain write: the purchase debits KK
        // through the same pipeline a power-up does. The endpoint runs that
        // pipeline to confirmation itself, so the snapshot it returns already
        // has the charge taken off the balance.
        val data = ChainQueue.serialized { KusinaApi.pantryBuySpin() }
        data.toSnapshot() to data.balance_kk
    }

    override suspend fun triviaSpin(levelId: Int, answer: String, correct: Boolean): Result<TriviaSpinOutcome> = runCatching {
        // Serialized with every other chain write, the same as buying a spin:
        // banking one goes through the pantry the wheel reads from.
        val data = ChainQueue.serialized { KusinaApi.pantryTriviaSpin(levelId, answer, correct) }
            ?: return@runCatching TriviaSpinOutcome.NotAvailable
        val snap = data.snapshot
        when {
            data.granted && snap != null -> TriviaSpinOutcome.Granted(snap.toSnapshot(), snap.balance_kk)
            data.granted -> error("Missing pantry snapshot")
            data.already_answered -> TriviaSpinOutcome.AlreadyAnswered
            else -> TriviaSpinOutcome.NotGranted
        }
    }

    override suspend fun sell(ingredientId: String, qty: Int): Result<Triple<SellResult, PantrySnapshot, Long>> = runCatching {
        val resp = KusinaApi.pantrySell(ingredientId, qty)
        val data = resp.data ?: error(resp.message ?: "Could not sell")
        data.tx_ref?.let { ref ->
            ChainQueue.serialized { KusinaApi.settleReward(ref) }
        }
        val ingredient = IngredientCatalog.getOrStub(data.ingredient_id)
        val (snap, balance) = snapshot().getOrThrow()
        Triple(
            SellResult(
                ingredient = ingredient,
                sold = data.sold,
                kkAwarded = data.kk_awarded,
                qtyLeft = data.qty_left
            ),
            snap,
            balance
        )
    }
}

private fun PantrySnapshotData.toSnapshot(): PantrySnapshot {
    val entries = jars.mapNotNull { row ->
        // Never dropped: an id this app has not heard of still shows (and sells)
        // as a plain ingredient until the next ingredient refresh names it.
        val ingredient = IngredientCatalog.getOrStub(row.id)
        if (row.qty <= 0) return@mapNotNull null
        PantryEntry(ingredient, row.qty, row.found_in_level)
    }
    return PantrySnapshot(
        entries = entries,
        drawsAvailable = draws_available,
        spinsAvailable = spins_available,
        collected = collected,
        total = if (total > 0) total else IngredientCatalog.total,
        jars = jar_count,
        sellValueKk = sell_value_kk
    )
}
