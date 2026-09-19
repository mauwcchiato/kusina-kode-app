package com.example.kusinakode.domain.model

/**
 * Groups the dish catalog is organized around.
 *
 * Three are island groups; PHILIPPINES holds the dishes the source dataset
 * records as nationwide rather than regional — adobo, sinigang and the
 * Spanish-influenced stews — which belong to no single island.
 */
enum class Region(val displayName: String, val tagline: String) {
    LUZON("Luzon", "Savory & Tangy Northern Flavors"),
    VISAYAS("Visayas", "Fresh & Grilled Island Fare"),
    MINDANAO("Mindanao", "Bold Flavors of the Royal South"),
    PHILIPPINES("Philippines", "Dishes the Whole Country Claims")
}
