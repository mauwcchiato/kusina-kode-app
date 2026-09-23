package com.example.kusinakode.data.repository

import android.content.Context
import com.example.kusinakode.api.ReelData
import com.example.kusinakode.domain.shop.KusinaShop
import com.example.kusinakode.domain.shop.ShopItem
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The last reel catalogue the server gave us, kept on the device.
 *
 * Without this the app would know about an admin-added reel only after a
 * successful fetch, every launch - so the first paint of a player's reward
 * history, before any request lands, would still show a raw id for anything
 * not compiled into the APK. Writing the response down means the second
 * launch already knows, including with no network at all.
 *
 * Stored as the server's own JSON rather than a parsed shape: the endpoint
 * does not exist yet and its columns will move while the web side settles it,
 * and re-reading a stale blob through a tolerant parser is safer than
 * migrating a format of our own invention.
 */
object ReelCatalogStore {

    private const val PREFS = "kusinakode_prefs"
    private const val KEY = "reel_catalog_json"

    private val json = Json {
        ignoreUnknownKeys = true   // the server may grow columns we do not read
        coerceInputValues = true   // a null where an Int is expected becomes 0
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    /**
     * Hands the cached catalogue to [KusinaShop], if there is one.
     *
     * Called at startup, before anything can ask item() a question. A absent
     * or unreadable cache is not an error - it is the normal state until the
     * server endpoint exists - so it fails quietly and leaves the seeds in
     * place.
     */
    fun warm(context: Context) {
        val raw = runCatching { prefs(context).getString(KEY, null) }.getOrNull() ?: return
        val reels = runCatching { json.decodeFromString<List<ReelData>>(raw) }.getOrNull() ?: return
        if (reels.isNotEmpty()) KusinaShop.applyRemoteReels(reels.toShopItems(), reels.publishedIds())
    }

    /** Remembers a fetched catalogue. An empty list clears it. */
    fun save(context: Context, reels: List<ReelData>) {
        runCatching {
            prefs(context).edit().apply {
                if (reels.isEmpty()) remove(KEY)
                else putString(KEY, json.encodeToString(reels))
            }.apply()
        }
    }
}

/**
 * Server rows to catalogue entries.
 *
 * A row with no id is dropped: it cannot be bought, cannot be looked up, and
 * would only sit on the shelf as a blank card. A row with no title keeps its
 * id as the title so at least something addressable renders - that case means
 * the admin saved an incomplete draft, and showing the id is a louder bug
 * report than showing an empty card.
 */
/**
 * Ids the shelf may sell.
 *
 * Anything not spelled "Published" is treated as withdrawn, which is the safe
 * way round: a status the app has not been taught yet - "Draft", or whatever
 * the admin console grows later - keeps a reel off the shelf rather than
 * putting an unfinished one in front of a player.
 */
internal fun List<ReelData>.publishedIds(): Set<String> =
    filter { it.status.equals("Published", ignoreCase = true) }
        .mapTo(mutableSetOf()) { it.id }

internal fun List<ReelData>.toShopItems(): List<ShopItem> =
    filter { it.id.isNotBlank() }.map { row ->
        KusinaShop.remoteReel(
            id = row.id,
            title = row.title.ifBlank { row.id },
            subtitle = row.subtitle,
            coinCost = row.coin_cost,
            youtubeQuery = row.youtube_query.ifBlank { row.dish_slug },
            emoji = row.emoji
        )
    }
