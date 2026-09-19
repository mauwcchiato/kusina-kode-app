package com.example.kusinakode.ui.rewards

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.R
import com.example.kusinakode.api.RewardHistoryItem
import com.example.kusinakode.domain.gamification.BadgeRules
import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.domain.pantry.IngredientCatalog
import com.example.kusinakode.domain.shop.KusinaShop
import com.example.kusinakode.ui.gamification.BadgeArt
import com.example.kusinakode.ui.pantry.PantryIngredientArt
import com.example.kusinakode.ui.shop.ShopArt

/** What a notification row shows in its well. */
sealed interface NotificationGlyph {
    /**
     * [fill] separates the two kinds of picture the app holds. A photograph
     * is a rectangle that has to be cropped to fill the round well, or it
     * sits letterboxed in the middle. A medal, coin, frame or map is already
     * a shape on transparency and must be fitted whole, or its edges are cut.
     */
    data class Art(@DrawableRes val res: Int, val fill: Boolean = false) : NotificationGlyph

    /**
     * A picture that lives on the server rather than in the APK.
     *
     * A level added through the panel has no drawable of its own - its photo is
     * a URL, and [LevelData.photo] is only a placeholder. Without this the row
     * for a panel-added dish wore the generic well while every other row showed
     * the thing it was about. [fallback] is drawn until the fetch lands, and if
     * it never does.
     */
    data class Remote(
        val url: String,
        @DrawableRes val fallback: Int,
        val fill: Boolean = true
    ) : NotificationGlyph

    /** For the few events with no drawing of their own — the power-ups. */
    data class Vector(val icon: ImageVector) : NotificationGlyph
}

/**
 * Picture for a notification.
 *
 * The event type alone cannot answer this: `reward_redemption` covers a
 * power-up, an avatar, a frame, a documentary and a pantry sale, which is why
 * a whole screen of unrelated rows came out wearing the same KK coin. The
 * server writes the real subject into `title` — `dish_11`, `pantry_ing_garlic`,
 * `av_salakot`, `reveal` — so that is what gets read here, and every one of
 * those already has art somewhere in the app.
 *
 * Title formats come from the API: `dish_<level>` (post_attempt.php),
 * `pantry_<ingredientId>` (lib/pantry.php), `island_<id>` and `daily_<key>`
 * (reward/claim.php), and the bare spend key for reward/spend.php.
 */
object NotificationArt {

    fun forEvent(eventType: String?, rawTitle: String?): NotificationGlyph {
        val key = rawTitle?.trim()?.lowercase().orEmpty()

        // Power-ups have icons rather than art — the same three the deck above
        // the keyboard wears, so the receipt matches the card that was tapped.
        PowerUp.entries.firstOrNull { it.spendKey.equals(key, ignoreCase = true) }?.let {
            return NotificationGlyph.Vector(
                when (it) {
                    PowerUp.REVEAL_LETTER -> Icons.Default.Visibility
                    PowerUp.BOMB -> Icons.Default.Whatshot
                    PowerUp.INSTANT_SOLVE -> Icons.Default.Paid
                }
            )
        }

        // Anything bought from the vault shows the thing that was bought.
        ShopArt.character(key)?.let { return NotificationGlyph.Art(it, fill = true) }
        ShopArt.frame(key)?.let { return NotificationGlyph.Art(it) }
        ShopArt.documentary(key)?.let { return NotificationGlyph.Art(it, fill = true) }
        if (KusinaShop.item(key) != null) {
            // A KODEX page, which has an emoji but no drawing of its own.
            return NotificationGlyph.Art(R.drawable.vault_encyclopedia, fill = true)
        }

        ingredientId(key)?.let { id ->
            PantryIngredientArt.forId(id)?.let { return NotificationGlyph.Art(it) }
            IngredientCatalog.get(id)?.let {
                return NotificationGlyph.Art(it.rarity.badge)
            }
            return NotificationGlyph.Art(R.drawable.vault_pantry, fill = true)
        }

        levelNumber(key)?.let { level ->
            // Only ever set on a win, so the dish is already solved and its
            // photograph gives nothing away.
            //
            // A level added through the panel carries its picture as a URL and
            // leaves .photo as the placeholder, so the URL is preferred when
            // there is one and the drawable stays as the fallback.
            val data = LevelProvider.forLevel(level)
            val url = data.photoUrl
            return if (!url.isNullOrBlank()) {
                NotificationGlyph.Remote(url, fallback = data.photo, fill = true)
            } else {
                NotificationGlyph.Art(data.photo, fill = true)
            }
        }

        if (key.startsWith("island_") || eventType == "island_complete") {
            return NotificationGlyph.Art(islandArt(key))
        }

        if (eventType == "badge_milestone") {
            badgeArt(key)?.let { return NotificationGlyph.Art(it) }
            return NotificationGlyph.Art(R.drawable.ribbon_kusina_master)
        }

        if (eventType == "palayok_spin" || key.startsWith("spin_won")) {
            return NotificationGlyph.Art(R.drawable.baul_closed)
        }

        if (eventType == "account_signup" || eventType == "account_login") {
            return NotificationGlyph.Vector(Icons.Default.Verified)
        }

        // Daily claims and anything unrecognised are simply KK moving. The
        // minted coin rather than the flat one: these rows sit bare on the
        // card now, with no tinted well behind them to give the art an edge.
        return NotificationGlyph.Art(R.drawable.ic_kk_pixel)
    }

    /** Convenience for rows that carry the whole ledger entry. */
    fun forRow(row: RewardHistoryItem): NotificationGlyph =
        forEvent(row.event_type, row.title)

    /**
     * Pantry sales arrive as `pantry_ing_garlic`; older rows on some servers
     * carry the bare `ing_garlic`, so both are accepted.
     */
    private fun ingredientId(key: String): String? = when {
        key.startsWith("pantry_") -> key.removePrefix("pantry_").takeIf { it.isNotBlank() }
        key.startsWith("ing_") -> key
        else -> null
    }

    /** `dish_11` is a round win on level 11. */
    private fun levelNumber(key: String): Int? =
        key.takeIf { it.startsWith("dish_") }
            ?.removePrefix("dish_")
            ?.toIntOrNull()
            ?.takeIf { it in 1..LevelProvider.levelCount }

    /**
     * The ledger stores a badge's *display title* when it has one and its id
     * only as a fallback (reward/claim.php), so "Line Cook" arrives far more
     * often than "rounds_5" — which is why every badge was landing on the
     * ribbon. Try the id first, then the catalogue's title index.
     */
    @DrawableRes
    private fun badgeArt(key: String): Int? =
        BadgeArt.forBadge(key) ?: badgeIdByTitle[key]?.let { BadgeArt.forBadge(it) }

    /** Display title (lowercased) to badge id, built from the badge catalogue. */
    private val badgeIdByTitle: Map<String, String> by lazy {
        BadgeRules.catalog().associate { (_, milestone, id) ->
            milestone.title.trim().lowercase() to id
        }
    }

    /** Islands arrive as an id or a display name; both carry the region. */
    @DrawableRes
    private fun islandArt(key: String): Int = when {
        "luzon" in key -> R.drawable.region_luzon
        "visayas" in key -> R.drawable.region_visayas
        "mindanao" in key -> R.drawable.region_mindanao
        else -> R.drawable.region_philippines
    }
}

/**
 * How many rows "WAITING FOR YOU" would show, for the number on the bell.
 *
 * The daily pays KK and a palayok, so it is two inbox rows. After the daily
 * is claimed, a leftover unused spin is one waiting row — not a third on
 * top of the unclaimed pair.
 */
internal fun waitingForYouCount(
    dailyClaimable: Boolean,
    spinsAvailable: Int,
    islandClaimable: Int,
    badgeClaimable: Int
): Int {
    val dailyRows = if (dailyClaimable) 2 else 0
    val spinRow = if (!dailyClaimable && spinsAvailable > 0) 1 else 0
    return dailyRows + spinRow + islandClaimable + badgeClaimable
}

/**
 * How many notifications are waiting, for the dot on the bell.
 *
 * Counts what the inbox itself would call unread — ledger entries not yet
 * opened, ignoring anything deleted — plus the rewards sitting in "WAITING
 * FOR YOU", which have no tx_ref to mark read and are the whole reason to
 * open the screen.
 */
internal fun unreadNotificationCount(
    history: List<RewardHistoryItem>,
    readIds: Set<String>,
    deletedIds: Set<String>,
    claimableCount: Int
): Int {
    val live = history.filter { it.tx_ref !in deletedIds }
    val unread = live.count { it.tx_ref != null && it.tx_ref !in readIds }
    return unread + claimableCount
}
