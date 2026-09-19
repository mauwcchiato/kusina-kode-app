// src/main/java/com/example/kusinakode/LevelProvider.kt
package com.example.kusinakode

import androidx.annotation.DrawableRes
import com.example.kusinakode.data.dishes.DishCatalog
import com.example.kusinakode.data.dishes.DishEntry
import com.example.kusinakode.domain.model.Region

/**
 * One playable level.
 *
 * [photo] is the dish photograph — hero images, list thumbnails, the round
 * background. [card] is the illustrated heritage card, the collectible a
 * player earns by solving the dish.
 *
 * [photoUrl] and [cardUrl] are set only for levels that came from the admin
 * panel, whose art lives on the server rather than in the APK. Screens that
 * can load over the network prefer them; the rest fall back to [photo] and
 * [card], which for a server-added level is a placeholder.
 */
data class LevelData(
    val word: String,
    val trivia: String,
    @DrawableRes val photo: Int,
    @DrawableRes val card: Int,
    val name: String,
    val region: Region,
    /** The full dataset row, for screens that want the whole write-up. */
    val dish: DishEntry,
    val photoUrl: String? = null,
    val cardUrl: String? = null
) {
    /** Uppercase answer, for easy comparison/hinting */
    val answer: String get() = word.uppercase()

    /** True for a level the admin panel added; it has no compiled art. */
    val isRemote: Boolean get() = photoUrl != null || cardUrl != null
}

/**
 * The level list: the compiled-in catalogue, plus anything the admin panel
 * has added after it.
 *
 * Levels are 1-based and word length is free-form — the grid sizes itself
 * from the answer, so dishes from 5 to 10 letters all work.
 *
 * ## What the server may and may not change
 *
 * A level's number is its index here, and that number is what progress,
 * unlocks and the island grouping are stored against. So the rule is:
 *
 *  - the compiled catalogue's levels keep their positions, always;
 *  - the server may **append** levels after them, and may re-word any level;
 *  - it may not reorder or remove a compiled level. A payload that would is
 *    rejected wholesale rather than applied in part, because renumbering
 *    silently re-points every player's saved progress at the wrong dish.
 *
 * Appending is safe precisely because it cannot move an existing index. A
 * server-added level that is later withdrawn (set to Draft, or deleted)
 * simply stops appearing, which only ever shortens the tail.
 */
object LevelProvider {

    /** The compiled-in list. Defines the first [baseCount] level numbers. */
    private val base: List<LevelData> = DishCatalog.playable.map { dish ->
        LevelData(
            word = dish.answer.lowercase(),
            trivia = dish.trivia,
            photo = dish.photo,
            card = dish.card,
            name = dish.name,
            region = dish.region,
            dish = dish
        )
    }

    /** How many levels ship in the APK. Positions 1..baseCount never move. */
    val baseCount: Int get() = base.size

    /**
     * What the app actually reads. Swapped wholesale when an overlay arrives,
     * so readers never observe a half-applied list.
     */
    @Volatile
    private var levels: List<LevelData> = base

    /** Total number of levels, including any the panel has added. */
    val levelCount get() = levels.size

    /** Dishes that exist in the catalogue but are not levels. */
    val reservedCount get() = DishCatalog.reserved.size

    /**
     * Returns the [LevelData] for a 1-based level index, clamped to 1..levelCount.
     */
    fun forLevel(level: Int): LevelData {
        val current = levels
        val idx = (level - 1).coerceIn(0, current.lastIndex)
        return current[idx]
    }

    /** 1-based level number for a dish slug, or null if it is not a level. */
    fun levelOf(slug: String): Int? =
        levels.indexOfFirst { it.dish.slug == slug }.takeIf { it >= 0 }?.plus(1)

    /**
     * What the Levels panel owns, keyed by the puzzle word.
     *
     * The server's `word` column holds the answer, which is what
     * [LevelData.word] holds in lower case, so that is the join key — matched
     * case-insensitively because saving a level in the panel used to
     * upper-case it.
     *
     * A null field means "the panel has nothing to say about this", leaving
     * the catalogue's value standing. An *empty* list is not the same as
     * null: clearing every ingredient in the panel is a real edit.
     */
    data class RemoteText(
        val word: String,
        val name: String? = null,
        val trivia: String? = null,
        val story: String? = null,
        val region: Region? = null,
        val ingredients: List<String>? = null,
        val steps: List<String>? = null,
        val tools: List<String>? = null,
        val difficulty: String? = null,
        val cookingTime: String? = null,
        val rating: String? = null,
        /** Absolute URLs; only meaningful for a level the panel added. */
        val photoUrl: String? = null,
        val cardUrl: String? = null,
        /** Anything other than "Published" keeps an appended level hidden. */
        val published: Boolean = true,
        /** Server row id, used only to keep appended levels in a stable order. */
        val sortKey: Int = 0
    )

    /**
     * Overlays panel content onto the catalogue and appends any levels the
     * panel has added beyond it.
     *
     * Rows matching a compiled level re-word it in place. Rows matching
     * nothing are appended, in [RemoteText.sortKey] order, but only if they
     * are published and carry enough to be playable. Passing an empty list
     * restores the catalogue untouched.
     */
    fun applyRemote(rows: List<RemoteText>) {
        if (rows.isEmpty()) {
            levels = base
            return
        }
        val byWord = rows.associateBy { it.word.trim().lowercase() }

        // 1. the compiled levels, re-worded but never moved
        val merged = base.map { level ->
            val remote = byWord[level.word] ?: return@map level
            level.withRemote(remote)
        }

        // 2. anything the panel added, after them
        val baseWords = base.mapTo(HashSet()) { it.word }
        val extras = rows
            .asSequence()
            .filter { it.word.trim().lowercase() !in baseWords }
            .filter { it.published }
            .filter { !it.name.isNullOrBlank() }
            .sortedBy { it.sortKey }
            .mapNotNull { it.toAppendedLevel() }
            .toList()

        levels = if (extras.isEmpty()) merged else merged + extras
    }

    /** Drops any overlay and goes back to the compiled-in catalogue. */
    fun clearRemote() {
        levels = base
    }

    /** True when an overlay is currently applied. For diagnostics. */
    val hasRemote: Boolean get() = levels !== base

    /** How many levels came from the panel rather than the APK. */
    val remoteCount: Int get() = (levels.size - base.size).coerceAtLeast(0)

    // ---- helpers ----

    private fun LevelData.withRemote(r: RemoteText): LevelData {
        val d = dish.copy(
            name = r.name?.ifBlank { null } ?: dish.name,
            trivia = r.trivia?.ifBlank { null } ?: dish.trivia,
            story = r.story?.ifBlank { null } ?: dish.story,
            region = r.region ?: dish.region,
            ingredients = r.ingredients ?: dish.ingredients,
            steps = r.steps ?: dish.steps,
            tools = r.tools ?: dish.tools,
            difficulty = r.difficulty?.ifBlank { null } ?: dish.difficulty,
            cookingTime = r.cookingTime?.ifBlank { null } ?: dish.cookingTime,
            rating = r.rating?.ifBlank { null } ?: dish.rating
        )
        return copy(
            trivia = d.trivia,
            name = d.name,
            region = d.region,
            dish = d,
            // A compiled level keeps its packaged art; a panel upload for it is
            // ignored rather than replacing a known-good asset with a fetch.
            photoUrl = photoUrl,
            cardUrl = cardUrl
        )
    }

    /**
     * Builds a level for a dish that exists only on the server. The word has
     * to be letters-only to be playable on the grid, so a row that fails that
     * is skipped rather than shipped as an unsolvable level.
     */
    private fun RemoteText.toAppendedLevel(): LevelData? {
        val w = word.trim().lowercase()
        if (w.isEmpty() || !w.all { it.isLetter() }) return null
        val display = name?.trim().orEmpty().ifEmpty { return null }
        val dish = DishEntry(
            slug = "remote_$w",
            name = display,
            answer = w.uppercase(),
            region = region ?: Region.PHILIPPINES,
            origin = "",
            rating = rating.orEmpty(),
            cookingTime = cookingTime.orEmpty(),
            difficulty = difficulty.orEmpty().ifEmpty { "Easy" },
            trivia = trivia.orEmpty(),
            story = story.orEmpty(),
            ingredients = ingredients.orEmpty(),
            steps = steps.orEmpty(),
            tools = tools.orEmpty(),
            reference = "",
            photo = R.drawable.dish_placeholder,
            card = R.drawable.dish_placeholder
        )
        return LevelData(
            word = w,
            trivia = dish.trivia,
            photo = R.drawable.dish_placeholder,
            card = R.drawable.dish_placeholder,
            name = display,
            region = dish.region,
            dish = dish,
            photoUrl = photoUrl,
            cardUrl = cardUrl
        )
    }
}
