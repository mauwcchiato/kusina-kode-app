package com.example.kusinakode.data.repository

import com.example.kusinakode.LevelProvider
import com.example.kusinakode.domain.model.PuzzleLevel
import com.example.kusinakode.domain.repository.LevelRepository

/**
 * Serves levels from the bundled [LevelProvider] list. Swapping this for a
 * backend/Cosmos-backed source (Module 4) must not touch the ViewModel or UI.
 */
class LocalLevelRepository : LevelRepository {

    override val levelCount: Int get() = LevelProvider.levelCount

    override fun level(number: Int): PuzzleLevel {
        val data = LevelProvider.forLevel(number)
        return PuzzleLevel(
            number = number.coerceIn(1, levelCount),
            answer = data.answer,
            displayName = data.name,
            trivia = data.trivia,
            imageRes = data.photo,
            cardRes = data.card,
            region = data.region,
            imageUrl = data.photoUrl,
            cardUrl = data.cardUrl
        )
    }
}
