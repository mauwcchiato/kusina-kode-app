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

class RemotePantryRepository : PantryRepository {

    override suspend fun snapshot(): Result<Pair<PantrySnapshot, Long>> = runCatching {
        val resp = KusinaApi.getPantrySnapshot()
        val data = resp.data ?: error(resp.message ?: "No pantry data")
        data.toSnapshot() to data.balance_kk
    }

    override suspend fun grant(levelId: Int, powerUpsUsed: Int): Result<Int> = runCatching {
        KusinaApi.pantryGrant(levelId, powerUpsUsed)
    }

    override suspend fun draw(levelId: Int?): Result<Triple<DrawResult, PantrySnapshot, Long>> = runCatching {
        val resp = KusinaApi.pantryDraw(levelId)
        val data = resp.data ?: error(resp.message ?: "The palayok was empty")
        val ingredient = data.ingredient_id?.let { IngredientCatalog.get(it) }
        val snap = (data.snapshot ?: error("Missing pantry snapshot")).toSnapshot()
        Triple(
            DrawResult(
                ingredient = ingredient,
                isDuplicate = data.is_duplicate,
                qty = data.qty,
                drawsLeft = data.draws_left,
                collected = data.collected
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

    override suspend fun sell(ingredientId: String, qty: Int): Result<Triple<SellResult, PantrySnapshot, Long>> = runCatching {
        val resp = KusinaApi.pantrySell(ingredientId, qty)
        val data = resp.data ?: error(resp.message ?: "Could not sell")
        data.tx_ref?.let { ref ->
            ChainQueue.serialized { KusinaApi.settleReward(ref) }
        }
        val ingredient = IngredientCatalog.get(data.ingredient_id)
            ?: error("Unknown ingredient")
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
        val ingredient = IngredientCatalog.get(row.id) ?: return@mapNotNull null
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
