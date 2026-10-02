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
    val cardUrl: String? = null,
    /**
     * The island level an admin pinned this dish to (1-based), or null to let
     * [LevelProvider.regionOrder] place it by word length. Display order only;
     * the global id is still what progress is filed under.
     */
    val levelOrder: Int? = null
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
     * The level list and which of its numbers are hidden, swapped together as
     * one value when an overlay arrives, so readers never observe a
     * half-applied list.
     */
    private class State(val levels: List<LevelData>, val hidden: Set<Int>)

    /**
     * Levels that wait for the panel start hidden: with no word from the
     * server yet (first launch, offline), they have not been published.
     */
    private val waitingInBase: Set<Int> =
        base.indices.filter { base[it].dish.waitsForPanel }.mapTo(HashSet()) { it + 1 }

    @Volatile
    private var state = State(base, waitingInBase)

    private val levels: List<LevelData> get() = state.levels

    /**
     * The highest level number, hidden ones included. Numbers are positions,
     * so this is what bounds a level id. For "how many dishes can a player
     * play" use [visibleCount].
     */
    val levelCount get() = state.levels.size

    /** Level numbers a player can see and play, in number order. */
    val visibleIds: List<Int>
        get() = state.let { s -> (1..s.levels.size).filter { it !in s.hidden } }

    /** How many levels are currently shown - what totals and ranks count. */
    val visibleCount: Int get() = state.let { it.levels.size - it.hidden.size }

    /**
     * False for a level that waits for the panel and is not published there.
     * Its number stays reserved; it just is not shown or counted.
     */
    fun isVisible(level: Int): Boolean = level in 1..levelCount && level !in state.hidden

    /**
     * How many of [solved] are currently shown. A dish solved and later set
     * back to Draft stays solved, but is not counted against [visibleCount].
     */
    fun visibleSolved(solved: Set<Int>): Int = solved.count { isVisible(it) }

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
     * The global level ids belonging to [region], in the order the Game Map
     * numbers them Level 1..N. A dish an admin pinned takes exactly its slot;
     * the rest fill the remaining slots by word length ascending (ties broken
     * by the global id), so with no pins Level 1 is the shortest word. Anything
     * that shows a player-facing level number must derive it from here to stay
     * consistent with the map.
     */
    fun regionOrder(region: Region): List<Int> {
        val island = visibleIds.filter { forLevel(it).region == region }
        return islandOrder(
            island,
            length = { forLevel(it).answer.length },
            pin = { forLevel(it).levelOrder }
        )
    }

    /**
     * The ordering rule behind [regionOrder], shared with the admin console's
     * `IslandOrder` (see docs/level-order-pins-from-web.md).
     *
     * Pinned ids go first, in (pin, id) order, each into the first free slot
     * at or after its pin, or failing that the nearest free slot before it. A
     * pin past the end counts as the last slot. Unpinned ids then fill the
     * empty slots from the top in (length, id) order. The clamp and the
     * fallbacks only matter for data the console would refuse to save, such as
     * a pin left past the end after a dish was removed.
     */
    internal fun islandOrder(ids: List<Int>, length: (Int) -> Int, pin: (Int) -> Int?): List<Int> {
        val n = ids.size
        val slots = arrayOfNulls<Int>(n)
        val (pinned, rest) = ids.partition { pin(it) != null }

        pinned.sortedWith(compareBy({ pin(it)!! }, { it })).forEach { id ->
            val want = pin(id)!!.coerceIn(1, n) - 1
            val at = (want until n).firstOrNull { slots[it] == null }
                ?: (want - 1 downTo 0).first { slots[it] == null }
            slots[at] = id
        }

        val fill = rest.sortedWith(compareBy({ length(it) }, { it })).iterator()
        for (i in 0 until n) if (slots[i] == null) slots[i] = fill.next()
        return slots.map { it!! }
    }

    /**
     * The per-region "Level N" a player sees for a global level id — its
     * 1-based position within its own region's [regionOrder]. Falls back to the
     * global id if the level is somehow not found. The global id is still what
     * progress, unlocks and the server are keyed to; this is display only.
     */
    fun regionLevelNumber(globalId: Int): Int {
        val region = runCatching { forLevel(globalId).region }.getOrNull() ?: return globalId
        val idx = regionOrder(region).indexOf(globalId)
        return if (idx >= 0) idx + 1 else globalId
    }

    /**
     * The global id of the level to nudge the player toward next, given the set
     * of solved global ids: the first still-unsolved level that is *unlocked*,
     * scanning regions in order and, within a region, by [regionOrder] (word
     * length). Each region's Level 1 is always unlocked; Level K unlocks once
     * that region's previous level is solved. This is exactly what the Game Map
     * offers as PLAY NOW, so any "continue" entry point (e.g. the Home card)
     * must use this rather than a global maxSolved+1, which would skip across
     * regions. Falls back to the first unsolved level overall, then level 1.
     */
    /**
     * The global id of the next level within the *same* region (by
     * [regionOrder], i.e. the next-longest word), or null if this is that
     * region's last level. The win screen's NEXT LEVEL uses this so finishing
     * "Visayas Level 1" continues to "Visayas Level 2" instead of jumping to
     * whatever global id happens to sit at level+1 in another island.
     */
    fun nextInRegion(globalId: Int): Int? {
        val region = runCatching { forLevel(globalId).region }.getOrNull() ?: return null
        val order = regionOrder(region)
        val idx = order.indexOf(globalId)
        return if (idx >= 0 && idx + 1 < order.size) order[idx + 1] else null
    }

    /**
     * The first unlocked, unsolved level *within one region* (by [regionOrder]),
     * or null if that region is fully solved. Used to keep the Home "Continue"
     * card on the island the player is currently working through.
     */
    fun nextPlayableInRegion(region: Region, solved: Set<Int>): Int? {
        val order = regionOrder(region)
        for (idx in order.indices) {
            val gid = order[idx]
            if (gid in solved) continue
            val unlocked = idx == 0 || order[idx - 1] in solved
            if (unlocked) return gid
        }
        return null
    }

    fun nextPlayable(solved: Set<Int>): Int {
        Region.entries.forEach { region ->
            nextPlayableInRegion(region, solved)?.let { return it }
        }
        // Nothing unlocked-and-unsolved: fall back to the first unsolved level
        // anywhere, then level 1.
        Region.entries.forEach { region ->
            regionOrder(region).firstOrNull { it !in solved }?.let { return it }
        }
        return 1
    }

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
        val sortKey: Int = 0,
        /** The island level an admin pinned the dish to, or null for automatic. */
        val levelOrder: Int? = null
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
            state = State(base, waitingInBase)
            return
        }
        val byWord = rows.associateBy { joinKey(it.word) }

        // 1. the compiled levels, re-worded but never moved. One that waits
        //    for the panel is shown only while its row there is Published.
        val hidden = HashSet<Int>()
        val merged = base.mapIndexed { i, level ->
            val remote = byWord[joinKey(level.word)]
            if (level.dish.waitsForPanel && remote?.published != true) hidden += i + 1
            if (remote == null) level else level.withRemote(remote)
        }

        // 2. anything the panel added, after them
        val baseWords = base.mapTo(HashSet()) { joinKey(it.word) }
        val extras = rows
            .asSequence()
            .filter { joinKey(it.word) !in baseWords }
            .filter { it.published }
            .filter { !it.name.isNullOrBlank() }
            .sortedBy { it.sortKey }
            .mapNotNull { it.toAppendedLevel() }
            .toList()

        state = State(if (extras.isEmpty()) merged else merged + extras, hidden)
    }

    /**
     * The panel may store a word with its hyphen ("pigar-pigar") while the
     * puzzle answer is letters only, so the two are matched on letters alone.
     */
    private fun joinKey(word: String): String = word.lowercase().filter { it.isLetter() }

    /** Drops any overlay and goes back to the compiled-in catalogue. */
    fun clearRemote() {
        state = State(base, waitingInBase)
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
            cardUrl = cardUrl,
            levelOrder = r.levelOrder
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
            region = region ?: Region.LUZON,
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
            cardUrl = cardUrl,
            levelOrder = levelOrder
        )
    }
}
