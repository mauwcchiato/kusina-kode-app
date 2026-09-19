package com.example.kusinakode.domain.repository

import com.example.kusinakode.domain.model.DishContent

/**
 * Source of dish cultural content. Local placeholder implementation today;
 * Module 4 swaps in a remote impl backed by GET /api/dish/content.php
 * (or the extended get_levels.php) without touching the UI.
 */
interface DishContentRepository {
    suspend fun dishContent(levelId: Int): Result<DishContent>
}
