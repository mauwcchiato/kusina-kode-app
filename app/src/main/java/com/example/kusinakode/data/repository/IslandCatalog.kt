package com.example.kusinakode.data.repository

import com.example.kusinakode.LevelProvider
import com.example.kusinakode.domain.model.Region

/** Level ids per island, matching LevelProvider and api/lib/islands.php. */
object IslandCatalog {
    fun dishesByIsland(): Map<String, Set<Int>> =
        (1..LevelProvider.levelCount)
            .groupBy { LevelProvider.forLevel(it).region.apiKey() }
            .mapValues { it.value.toSet() }
}

private fun Region.apiKey(): String = name.lowercase()
