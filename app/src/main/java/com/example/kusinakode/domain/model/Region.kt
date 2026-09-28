package com.example.kusinakode.domain.model

/**
 * The three island groups the dish catalog is organized around. Dishes the
 * source dataset once recorded as nationwide (adobo, sinigang and the
 * Spanish-influenced stews) are now filed under the island they are most
 * associated with, so every dish belongs to exactly one island.
 */
enum class Region(val displayName: String, val tagline: String) {
    LUZON("Luzon", "Savory & Tangy Northern Flavors"),
    VISAYAS("Visayas", "Fresh & Grilled Island Fare"),
    MINDANAO("Mindanao", "Bold Flavors of the Royal South")
}
