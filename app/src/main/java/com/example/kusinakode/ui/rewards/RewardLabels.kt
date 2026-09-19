package com.example.kusinakode.ui.rewards

import com.example.kusinakode.LevelProvider
import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.domain.pantry.IngredientCatalog
import com.example.kusinakode.domain.shop.KusinaShop
import com.example.kusinakode.domain.shop.ShopKind

/**
 * Ledger rows are logged under the key the server prices them by — "bomb",
 * "pan_suka", "av_sarimanok" — so every screen that shows history has to turn
 * that key back into the name the player actually saw in the shop.
 *
 * Returns null when there is nothing usable, letting the caller fall back to
 * its own wording for the event.
 */
internal fun rewardTitleOrNull(rawTitle: String?): String? {
    val key = rawTitle?.trim()?.takeIf { it.isNotBlank() } ?: return null

    // Daily claims are keyed daily_YYYY_MM_DD. Show the day as a date,
    // not "Daily 2026 09 17".
    Regex("(?i)^daily[_\\s-]+(\\d{4})[_\\s-]+(\\d{2})[_\\s-]+(\\d{2})$")
        .matchEntire(key)
        ?.let { return "Daily ${it.groupValues[1]}-${it.groupValues[2]}-${it.groupValues[3]}" }

    PowerUp.values().firstOrNull { it.spendKey.equals(key, ignoreCase = true) }
        ?.let { return "${it.title} power-up" }

    KusinaShop.item(key)?.let { item ->
        return when (item.kind) {
            ShopKind.DOCUMENTARY -> "${item.title} · Documentary"
            ShopKind.ENCYCLOPEDIA -> "${item.title} · Pantry entry"
            ShopKind.AVATAR -> "${item.title} · Avatar"
        }
    }

    // A win is logged as dish_<level>. Without this the generic fallback below
    // turns it into "Dish 29", which is the level number rather than the dish -
    // and the number is meaningless to a player.
    Regex("""(?i)^dish[_\s-]+(\d{1,3})$""").matchEntire(key)
        ?.groupValues?.get(1)?.toIntOrNull()
        ?.takeIf { it in 1..LevelProvider.levelCount }
        ?.let { level -> LevelProvider.forLevel(level).name.takeIf { it.isNotBlank() }?.let { return it } }

    // Pantry ingredients are logged under their catalogue id (ing_soy_sauce).
    // The generic fallback below cannot know "ing" is a namespace rather
    // than a word, so without this the row reads "Ing Soy Sauce".
    IngredientCatalog.get(key)?.let { return it.name }
    IngredientCatalog.get(key.lowercase())?.let { return it.name }

    return prettifyRewardKey(key)
}

/** pantry/spin.php keys a roll as spin_won_3 / spin_won_5 / spin_won_8. */
internal fun palayoksWonFromSpinTitle(rawTitle: String?): Int? =
    Regex("(?i)^spin_won_(\\d+)$")
        .matchEntire(rawTitle?.trim().orEmpty())
        ?.groupValues
        ?.get(1)
        ?.toIntOrNull()

/** Last resort for keys with no catalogue entry: "pan_ube" reads as "Pan Ube". */
internal fun prettifyRewardKey(raw: String?): String? {
    val key = raw?.trim()?.takeIf { it.isNotBlank() } ?: return null
    if (key.any { it.isWhitespace() }) return key
    return key.removePrefix("ing_")
        .split('_', '-')
        .filter { it.isNotBlank() }
        .joinToString(" ") { part -> part.replaceFirstChar { it.uppercaseChar() } }
        .takeIf { it.isNotBlank() }
}
