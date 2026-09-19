package com.example.kusinakode.domain.pantry

import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.Color
import com.example.kusinakode.R

enum class Rarity(
    val label: String,
    val sellValue: Int,
    val tint: Color,
    @DrawableRes val badge: Int
) {
    // Silver, green, blue, purple — the same four the medallions and the card
    // frames wear, so a tier reads the same everywhere it appears.
    COMMON("Common", 1, Color(0xFF98A3B0), R.drawable.rarity_common),
    UNCOMMON("Uncommon", 3, Color(0xFF3D8B4F), R.drawable.rarity_uncommon),
    RARE("Rare", 8, Color(0xFF2F6DB5), R.drawable.rarity_rare),
    LEGENDARY("Legendary", 20, Color(0xFF8E3FB5), R.drawable.rarity_legendary);

    companion object {
        fun fromLabel(raw: String): Rarity =
            entries.firstOrNull { it.name.equals(raw, ignoreCase = true) || it.label.equals(raw, ignoreCase = true) }
                ?: COMMON
    }
}

data class Ingredient(
    val id: String,
    val name: String,
    val localName: String,
    val origin: String,
    val lore: String,
    val rarity: Rarity
)

data class PantryEntry(
    val ingredient: Ingredient,
    val qty: Int,
    val foundInLevel: Int?
) {
    val sellValueKk: Int get() = qty * ingredient.rarity.sellValue
}

data class PantrySnapshot(
    val entries: List<PantryEntry> = emptyList(),
    val drawsAvailable: Int = 0,
    /** Wheel spins banked by daily logins, redeemed in the Market Run. */
    val spinsAvailable: Int = 0,
    val collected: Int = 0,
    val total: Int = 0,
    val jars: Int = 0,
    val sellValueKk: Int = 0
) {
    val ownedIds: Set<String> get() = entries.map { it.ingredient.id }.toSet()
}

data class DrawResult(
    val ingredient: Ingredient?,
    val isDuplicate: Boolean,
    val qty: Int,
    val drawsLeft: Int,
    val collected: Int
)

data class SellResult(
    val ingredient: Ingredient,
    val sold: Int,
    val kkAwarded: Int,
    val qtyLeft: Int
)
