package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.PuzzleLevel

interface LevelRepository {
    val levelCount: Int
    fun level(number: Int): PuzzleLevel
}
