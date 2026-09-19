package com.example.kusinakode.data.levels

import android.content.Context
import android.util.Log
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.data.net.ServerConfig
import com.example.kusinakode.domain.model.Region
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Pulls what the admin panel owns and hands it to [LevelProvider]: the wording
 * of the levels that ship in the APK, and any levels the panel has added after
 * them.
 *
 * Offline is the normal case on a phone, not an error, so the last good
 * response is cached and replayed at startup before the network is consulted.
 * A failed refresh leaves whatever is already applied standing; it never
 * clears the overlay, because a dropped connection is not a reason to show
 * the player different content than they saw a minute ago.
 */
object LevelSync {

    private const val TAG = "LevelSync"
    private const val PREFS_NAME = "kusinakode_prefs"

    /** Bumped when [Cached] changes shape, so a stale cache is dropped rather
     *  than half-read. The old key is simply never looked at again. */
    private const val KEY_CACHE = "levels_remote_cache_v3"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** The cached shape, kept separate from the API DTO so a server-side
     *  rename cannot silently invalidate what is already on disk. */
    @Serializable
    private data class Cached(
        val word: String,
        val name: String? = null,
        val trivia: String? = null,
        val story: String? = null,
        val region: String? = null,
        val ingredients: String? = null,
        val steps: String? = null,
        val equipment: String? = null,
        val difficulty: String? = null,
        val cookMinutes: String? = null,
        val rating: String? = null,
        val imagePath: String? = null,
        val cardPath: String? = null,
        val status: String? = null,
        val id: Int = 0
    )

    /**
     * Applies the cached overlay, if any. Synchronous and cheap — call it
     * before the first screen is composed so the player never sees the
     * catalogue content flash over to the server's.
     */
    fun load(context: Context) {
        val raw = prefs(context).getString(KEY_CACHE, null) ?: return
        runCatching { json.decodeFromString<List<Cached>>(raw) }
            .onSuccess { LevelProvider.applyRemote(it.map(::toRemote)) }
            .onFailure {
                // A cache we cannot read is worse than none: drop it so the
                // next refresh writes a clean one.
                Log.w(TAG, "discarding unreadable level cache: ${it.message}")
                prefs(context).edit().remove(KEY_CACHE).apply()
            }
    }

    /**
     * Fetches the current content and applies it. Safe to call on every
     * launch; failure is logged and ignored.
     *
     * @return true when the overlay was refreshed from the network.
     */
    suspend fun refresh(context: Context): Boolean {
        val rows = runCatching { KusinaApi.getLevels() }
            .onFailure { Log.i(TAG, "level refresh skipped: ${it.message}") }
            .getOrNull()
            ?.takeIf { it.status == "success" }
            ?.data
            ?: return false

        val cached = rows
            .filter { it.word.isNotBlank() }
            .map {
                Cached(
                    word = it.word,
                    name = it.name,
                    trivia = it.trivia,
                    story = it.story,
                    region = it.region,
                    ingredients = it.ingredients,
                    steps = it.steps,
                    equipment = it.equipment,
                    difficulty = it.difficulty,
                    cookMinutes = it.cook_time_minutes,
                    rating = it.rating,
                    imagePath = it.image_path,
                    cardPath = it.history_image,
                    status = it.status,
                    id = it.id
                )
            }
        if (cached.isEmpty()) return false

        LevelProvider.applyRemote(cached.map(::toRemote))
        runCatching { prefs(context).edit().putString(KEY_CACHE, json.encodeToString(cached)).apply() }
            .onFailure { Log.w(TAG, "could not cache levels: ${it.message}") }
        Log.i(
            TAG,
            "levels refreshed: ${cached.size} rows -> ${LevelProvider.levelCount} playable " +
                "(${LevelProvider.remoteCount} added by the panel)"
        )
        return true
    }

    /** Forgets the cache and the overlay. Used when switching environments. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_CACHE).apply()
        LevelProvider.clearRemote()
    }

    private fun toRemote(c: Cached) = LevelProvider.RemoteText(
        word = c.word,
        name = c.name,
        trivia = c.trivia,
        story = c.story,
        region = regionOf(c.region),
        ingredients = namesOrNull(c.ingredients),
        steps = stepsOrNull(c.steps),
        tools = namesOrNull(c.equipment),
        difficulty = difficultyOf(c.difficulty),
        cookingTime = cookTimeOf(c.cookMinutes),
        rating = c.rating?.trim()?.trimEnd('0')?.trimEnd('.')?.ifBlank { null },
        photoUrl = mediaUrl(c.imagePath),
        cardUrl = mediaUrl(c.cardPath),
        published = c.status.isNullOrBlank() || c.status.equals("Published", ignoreCase = true),
        sortKey = c.id
    )

    /**
     * Turns a stored image path into something fetchable, or null.
     *
     * Only `media/...` is served by XAMPP, the one origin the app can reach.
     * The older `Images/levels/...` paths live inside the ASP.NET app on
     * localhost, and a bare name like "card_adobo" is a drawable already in
     * the APK — neither is a URL, so both yield null and the caller keeps its
     * compiled art.
     */
    private fun mediaUrl(path: String?): String? {
        val p = path?.trim().orEmpty()
        if (p.isEmpty()) return null
        if (p.startsWith("http://", true) || p.startsWith("https://", true)) return p
        if (!p.startsWith("media/", true)) return null
        return "http://${ServerConfig.host}/kusinakode/$p"
    }

    /**
     * Null means the column was absent; an empty list means the admin really
     * did clear it. Only a null/absent column defers to the catalogue.
     *
     * Newline is the delimiter, because ingredient names legitimately contain
     * commas - "Black Beans, Salted (Tausi)", "speck, etag or any fatty cured
     * smoked pork". A value with no newline is from before that change and is
     * still read as comma separated.
     */
    private fun namesOrNull(raw: String?): List<String>? {
        if (raw == null) return null
        val parts = if (raw.contains('\n')) raw.split('\n', '\r') else raw.split(',')
        return parts.map { it.trim() }.filter { it.isNotEmpty() }
    }

    /**
     * Steps arrive in one of three shapes, and only one of them is a clean
     * "one step per line":
     *
     *  - the catalogue import writes one step per line, unnumbered
     *  - the Add level form's placeholder asks for a single blob numbered
     *    inline, "1. ... 2. ..."
     *  - an admin pastes numbered steps into the textarea, where a long step
     *    wraps onto the next line
     *
     * The third shape is why this cannot simply split on newlines. Piaya was
     * entered as 7 numbered steps and read back as 11, because a wrap inside
     * step 1 became a step of its own: one card ended "...and water gradually,"
     * and the next read "then mix until a dough forms."
     *
     * So when the text numbers itself at the start of a line, those markers are
     * the boundaries and every other line belongs to the step above it. Wrapped
     * lines are folded back into one paragraph, because the author's line breaks
     * were the width of the textarea rather than anything they meant. The
     * author's own numbers are dropped either way, so the app numbers
     * consistently however the row was entered.
     */
    private fun stepsOrNull(raw: String?): List<String>? {
        if (raw == null) return null
        val text = raw.replace("\r\n", "\n").replace('\r', '\n')

        // A marker only counts at the start of a line: "approximately 3 minutes
        // per side" is a continuation, not step 3. Two are needed before this
        // shape is claimed - a single blob that opens "1. ... 2. ..." has
        // exactly one marker at a line start, and belongs to the inline branch
        // below, which is what the Add level form's placeholder asks for.
        val numbered = Regex("""(?m)^[ \t]*\d{1,2}[.)][ \t]+""")
        val chunks = if (numbered.findAll(text).count() > 1) {
            text.split(Regex("""(?m)^(?=[ \t]*\d{1,2}[.)][ \t]+)"""))
        } else {
            val lines = text.split('\n').map { it.trim() }.filter { it.isNotEmpty() }
            if (lines.size == 1) {
                lines[0].split(Regex("""(?=(?:^|\s)\d{1,2}[.)]\s)"""))
            } else {
                lines
            }
        }
        return chunks
            .map {
                it.replace(Regex("""^\s*\d{1,2}[.)]\s*"""), "")
                    .replace(Regex("""\s+"""), " ")
                    .trim()
            }
            .filter { it.isNotEmpty() }
    }

    /**
     * The panel's dropdown says Easy/Medium/Hard; the dataset says
     * Easy/Moderate/Difficult. Translate rather than show the player a word
     * the rest of the app never uses.
     */
    private fun difficultyOf(raw: String?): String? = when (raw?.trim()?.lowercase()) {
        null, "" -> null
        "easy" -> "Easy"
        "medium", "moderate" -> "Moderate"
        "hard", "difficult" -> "Difficult"
        else -> raw.trim()
    }

    /** Minutes on the wire, the dataset's phrasing on screen. */
    private fun cookTimeOf(raw: String?): String? {
        val total = raw?.trim()?.toIntOrNull() ?: return null
        if (total <= 0) return null
        val h = total / 60
        val m = total % 60
        return when {
            h == 0 -> "$m minutes"
            m == 0 -> if (h == 1) "1 hour" else "$h hours"
            else -> (if (h == 1) "1 hour" else "$h hours") + " & $m minutes"
        }
    }

    /**
     * The panel stores a display name ("Visayas"); the app keys off the enum.
     * An unrecognised value leaves the catalogue's region alone rather than
     * guessing, since the island grouping drives progress screens.
     */
    private fun regionOf(raw: String?): Region? {
        val s = raw?.trim().orEmpty()
        if (s.isEmpty()) return null
        return Region.values().firstOrNull {
            it.displayName.equals(s, ignoreCase = true) || it.name.equals(s, ignoreCase = true)
        }
    }

    private fun prefs(context: Context) =
        context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
