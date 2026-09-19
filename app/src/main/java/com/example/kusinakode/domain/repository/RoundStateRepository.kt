package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.SavedRound

/**
 * Keeps an unfinished round so the player can leave the game screen and come
 * back to the same board.
 *
 * Scoped to the signed-in account by the implementation, so two players
 * sharing a device don't inherit each other's half-finished puzzles.
 *
 * Deliberately not suspending: the game screen needs the board before its
 * first frame, and this is a single small read.
 */
interface RoundStateRepository {
    fun load(levelId: Int): SavedRound?
    fun save(round: SavedRound)
    fun clear(levelId: Int)
}
