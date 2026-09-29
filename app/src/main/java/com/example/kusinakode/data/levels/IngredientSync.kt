package com.example.kusinakode.data.levels

import android.content.Context
import android.util.Log
import com.example.kusinakode.api.IngredientData
import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.data.net.ServerConfig
import com.example.kusinakode.domain.pantry.IngredientCatalog
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Keeps [IngredientCatalog] in step with the web panel's Ingredients page.
 *
 * Mirrors [LevelSync] and [EquipmentSync]: the last good list is replayed at
 * startup, a fresh one is fetched in the background, and a failed fetch leaves
 * whatever is applied alone. With nothing cached and no network, the book
 * compiled into the APK stands, so the pantry always works offline.
 */
object IngredientSync {

    private const val TAG = "IngredientSync"
    private const val PREFS_NAME = "kusinakode_prefs"
    private const val KEY_CACHE = "ingredient_cache_v1"

    /**
     * Where the panel's pictures are served. Ingredient images are stored as
     * `images/ingredients/...`, a path inside the admin web app.
     */
    private const val ADMIN_ORIGIN = "https://admin.kusinakode.com"

    private val json = Json { ignoreUnknownKeys = true; encodeDefaults = true }

    /** Applies the cached list, if any. Cheap; safe before first compose. */
    fun applyCached(context: Context) {
        val raw = prefs(context).getString(KEY_CACHE, null) ?: return
        runCatching { json.decodeFromString<List<IngredientData>>(raw) }
            .onSuccess { IngredientCatalog.applyRemote(it.map(::toRemote)) }
            .onFailure {
                Log.w(TAG, "discarding unreadable ingredient cache: ${it.message}")
                prefs(context).edit().remove(KEY_CACHE).apply()
            }
    }

    /**
     * Fetches the list and caches it. Returns false when nothing usable came
     * back, leaving the applied list as it was.
     */
    suspend fun refresh(context: Context): Boolean {
        val rows = runCatching { KusinaApi.getIngredients() }
            .onFailure { Log.i(TAG, "ingredient refresh skipped: ${it.message}") }
            .getOrNull()
            ?.takeIf { it.status == "success" }
            ?.data
            ?.filter { it.id.isNotBlank() && it.name.isNotBlank() }
            ?: return false
        if (rows.isEmpty()) return false

        IngredientCatalog.applyRemote(rows.map(::toRemote))
        runCatching { prefs(context).edit().putString(KEY_CACHE, json.encodeToString(rows)).apply() }
            .onFailure { Log.w(TAG, "could not cache ingredients: ${it.message}") }
        Log.i(TAG, "ingredients refreshed: ${rows.size} published")
        return true
    }

    /** Forgets the cache and goes back to the bundled book. */
    fun clear(context: Context) {
        prefs(context).edit().remove(KEY_CACHE).apply()
        IngredientCatalog.clearRemote()
    }

    private fun toRemote(d: IngredientData) = IngredientCatalog.Remote(
        id = d.id.trim(),
        name = d.name,
        localName = d.local_name.orEmpty(),
        category = d.category.orEmpty(),
        rarity = d.rarity.orEmpty().ifBlank { "common" },
        description = d.description.orEmpty(),
        imageUrl = imageUrl(d.image_path)
    )

    /**
     * A full URL passes through; `media/...` is served by the API host, like
     * level art; anything else (`images/ingredients/...`) by the admin site.
     */
    internal fun imageUrl(path: String?): String? {
        val p = path?.trim().orEmpty().removePrefix("/")
        if (p.isEmpty()) return null
        if (p.startsWith("http://", true) || p.startsWith("https://", true)) return p
        if (p.startsWith("media/", true)) return "${ServerConfig.scheme}://${ServerConfig.host}/kusinakode/$p"
        return "$ADMIN_ORIGIN/$p"
    }

    private fun prefs(context: Context) =
        context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
}
