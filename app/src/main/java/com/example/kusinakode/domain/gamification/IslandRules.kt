package com.example.kusinakode.domain.gamification

/**
 * Which islands (Luzon / Visayas / Mindanao / Philippines) the player has fully solved.
 * Catalogue of level ids is supplied by the app so this stays pure.
 */
object IslandRules {
    fun completed(solved: Set<Int>, dishesByIsland: Map<String, Set<Int>>): Set<String> =
        dishesByIsland
            .filter { (_, ids) -> ids.isNotEmpty() && solved.containsAll(ids) }
            .keys
}
