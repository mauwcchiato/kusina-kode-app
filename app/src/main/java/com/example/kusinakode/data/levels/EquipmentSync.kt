package com.example.kusinakode.data.levels

import android.content.Context
import android.util.Log
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.data.net.ServerConfig
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * The equipment catalogue the admin panel owns, keyed by tool name.
 *
 * A dish's equipment is stored as a list of names, so a tool card had only a
 * name to work with: it looked that name up in [com.example.kusinakode.ui.learn.EquipmentArt],
 * the art packaged in the APK, and fell back to a keyword-matched icon when
 * there was no match. Anything an admin added after the build therefore drew a
 * generic icon no matter what picture they uploaded — Dough Cutter and Pastry
 * Cutter both came out as the same pair of scissors.
 *
 * This is the missing half: the name to picture mapping the panel maintains,
 * fetched and cached so the phone can draw what the admin actually uploaded.
 * Packaged art stays the fallback rather than being replaced, so the screen
 * still looks right with no network and on first run.
 *
 * Mirrors [LevelSync] deliberately, including the offline behaviour: the last
 * good response is replayed at startup, and a failed refresh leaves it alone.
 */
object EquipmentSync {

    private const val TAG = "EquipmentSync"
    private const val PREFS_NAME = "kusinakode_prefs"
    private const val KEY_CACHE = "equipment_cache_v1"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    @Serializable
    private data class Cached(
        val name: String,
        val imagePath: String? = null
    )

    /**
     * Normalised name to image URL. Empty until something is loaded, which is
     * the same as "no remote art" — every caller already handles that.
     */
    @Volatile
    private var byName: Map<String, String> = emptyMap()

    val count: Int get() = byName.size

    /**
     * The URL an admin uploaded for this tool, or null.
     *
     * Normalisation matches EquipmentArt's so the two lookups agree on what
     * counts as the same tool: lowercase, and any run of punctuation or spaces
     * folded to one space. The dataset and the panel disagree about wording
     * often enough ("Strainer/colander" against "Strainer or colander") that
     * comparing the raw strings would miss most of them.
     */
    fun urlForName(name: String): String? = byName[normalise(name)]

    private fun normalise(raw: String): String =
        raw.lowercase().replace(Regex("[^a-z0-9]+"), " ").trim()

    /** Applies the cached catalogue, if any. Cheap; safe before first compose. */
    fun applyCached(context: Context) {
        val raw = prefs(context).getString(KEY_CACHE, null) ?: return
        runCatching { json.decodeFromString<List<Cached>>(raw) }
            .onSuccess { apply(it) }
            .onFailure { Log.w(TAG, "could not read cached equipment: ${it.message}") }
    }

    /**
     * Fetches the catalogue and caches it. Returns false when nothing usable
     * came back, leaving whatever is already applied in place.
     */
    suspend fun refresh(context: Context): Boolean {
        val rows = runCatching { KusinaApi.getEquipment() }
            .onFailure { Log.w(TAG, "equipment refresh failed: ${it.message}") }
            .getOrNull() ?: return false

        val cached = rows
            .filter { it.name.isNotBlank() }
            .map { Cached(name = it.name, imagePath = it.image_path) }
        if (cached.isEmpty()) return false

        apply(cached)
        runCatching { prefs(context).edit().putString(KEY_CACHE, json.encodeToString(cached)).apply() }
            .onFailure { Log.w(TAG, "could not cache equipment: ${it.message}") }
        Log.i(TAG, "equipment refreshed: ${cached.size} tools, ${byName.size} with art")
        return true
    }

    /** Forgets the cache and the mapping. Used when switching environments. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_CACHE).apply()
        byName = emptyMap()
    }

    private fun apply(rows: List<Cached>) {
        byName = rows.mapNotNull { row ->
            val url = mediaUrl(row.imagePath) ?: return@mapNotNull null
            normalise(row.name) to url
        }.toMap()
    }

    /**
     * Same rule as LevelSync: only `media/...` is served by the origin the app
     * can reach. An `images/equipment/...` path is inside the ASP.NET app on
     * localhost and is not fetchable from a phone, so it yields null and the
     * card keeps its packaged art.
     */
    private fun mediaUrl(path: String?): String? {
        val p = path?.trim().orEmpty()
        if (p.isEmpty()) return null
        if (p.startsWith("http://", true) || p.startsWith("https://", true)) return p
        if (!p.startsWith("media/", true)) return null
        return "${ServerConfig.scheme}://${ServerConfig.host}/kusinakode/$p"
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
