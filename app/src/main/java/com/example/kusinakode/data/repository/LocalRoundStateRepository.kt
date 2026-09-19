package com.example.kusinakode.data.repository

import android.content.Context
import com.example.kusinakode.domain.model.SavedRound
import com.example.kusinakode.domain.model.TileState
import com.example.kusinakode.domain.repository.RoundStateRepository
import org.json.JSONArray
import org.json.JSONObject

/**
 * Stores the in-progress round as JSON in SharedPreferences, one entry per
 * account and level.
 *
 * Enum names rather than ordinals go on disk, so reordering [TileState] can't
 * silently turn a green tile grey in an old save.
 */
class LocalRoundStateRepository(
    context: Context,
    private val userId: () -> Int?
) : RoundStateRepository {

    private val prefs = context.applicationContext
        .getSharedPreferences("kusinakode_prefs", Context.MODE_PRIVATE)

    private fun key(levelId: Int): String {
        val scope = userId()?.takeIf { it > 0 }?.let { "user_$it" } ?: "guest"
        return "round_${scope}_$levelId"
    }

    override fun load(levelId: Int): SavedRound? {
        val raw = prefs.getString(key(levelId), null) ?: return null
        // A corrupt or outdated entry is not worth crashing a round over.
        return runCatching { JSONObject(raw).toSavedRound() }.getOrNull()
    }

    override fun save(round: SavedRound) {
        prefs.edit().putString(key(round.levelId), round.toJson().toString()).apply()
    }

    override fun clear(levelId: Int) {
        prefs.edit().remove(key(levelId)).apply()
    }

    private fun SavedRound.toJson() = JSONObject().apply {
        put("levelId", levelId)
        put("wordLength", wordLength)
        put("maxAttempts", maxAttempts)
        put("rows", JSONArray().apply { rows.forEach { put(it) } })
        put("tileStates", JSONArray().apply {
            tileStates.forEach { row ->
                put(JSONArray().apply { row.forEach { put(it.name) } })
            }
        })
        put("keyStates", JSONObject().apply {
            keyStates.forEach { (ch, state) -> put(ch.toString(), state.name) }
        })
        put("currentRow", currentRow)
        put("elapsedSeconds", elapsedSeconds)
        put("revealedPositions", JSONObject().apply {
            revealedPositions.forEach { (pos, ch) -> put(pos.toString(), ch.toString()) }
        })
        put("revealsUsed", revealsUsed)
        put("bombUsed", bombUsed)
        put("lastHintUsedAtMs", lastHintUsedAtMs ?: 0L)
    }

    private fun JSONObject.toSavedRound(): SavedRound {
        val rowsArray = getJSONArray("rows")
        val statesArray = getJSONArray("tileStates")
        val keysObject = getJSONObject("keyStates")
        val revealsObject = getJSONObject("revealedPositions")

        return SavedRound(
            levelId = getInt("levelId"),
            wordLength = getInt("wordLength"),
            maxAttempts = getInt("maxAttempts"),
            rows = (0 until rowsArray.length()).map { rowsArray.getString(it) },
            tileStates = (0 until statesArray.length()).map { r ->
                val row = statesArray.getJSONArray(r)
                (0 until row.length()).map { TileState.valueOf(row.getString(it)) }
            },
            keyStates = keysObject.keys().asSequence().associate { k ->
                k.first() to TileState.valueOf(keysObject.getString(k))
            },
            currentRow = getInt("currentRow"),
            elapsedSeconds = getInt("elapsedSeconds"),
            revealedPositions = revealsObject.keys().asSequence().associate { k ->
                k.toInt() to revealsObject.getString(k).first()
            },
            revealsUsed = getInt("revealsUsed"),
            bombUsed = getBoolean("bombUsed"),
            lastHintUsedAtMs = optLong("lastHintUsedAtMs", 0L).takeIf { it > 0L }
        )
    }
}
