package com.example.kusinakode

import android.content.Context
import android.content.SharedPreferences
import android.util.Log
import androidx.security.crypto.EncryptedSharedPreferences
import androidx.security.crypto.MasterKeys

/**
 * Persists the signed-in session.
 *
 * The bearer token is a live credential — anyone holding it is the account
 * until it expires — so it lives in [EncryptedSharedPreferences], backed by a
 * key in the Android Keystore. Plain SharedPreferences is readable on a rooted
 * or forensically imaged device.
 */
object SessionStore {
    private const val PREFS_NAME = "kusinakode_session"
    private const val SECURE_PREFS_NAME = "kusinakode_session_secure"
    private const val KEY_USER_ID = "user_id"
    private const val KEY_DISPLAY_NAME = "display_name"
    private const val KEY_EMAIL = "email"
    private const val KEY_NICKNAME = "nickname"
    private const val KEY_TOKEN = "token"
    private const val TAG = "SessionStore"

    @Volatile
    private var cached: SharedPreferences? = null

    /**
     * Encrypted preferences, falling back to plain ones if the Keystore is
     * unavailable.
     *
     * The fallback matters: EncryptedSharedPreferences is known to throw on
     * some devices with a damaged keystore, and a crash on launch during a
     * demo is worse than the storage being unencrypted on that one handset.
     */
    private fun prefs(context: Context): SharedPreferences {
        cached?.let { return it }
        val app = context.applicationContext
        val store = try {
            // security-crypto 1.0.0 API: a Keystore alias, not a MasterKey object.
            val masterKeyAlias = MasterKeys.getOrCreate(MasterKeys.AES256_GCM_SPEC)
            EncryptedSharedPreferences.create(
                SECURE_PREFS_NAME,
                masterKeyAlias,
                app,
                EncryptedSharedPreferences.PrefKeyEncryptionScheme.AES256_SIV,
                EncryptedSharedPreferences.PrefValueEncryptionScheme.AES256_GCM
            ).also { migrateFromPlain(app, it) }
        } catch (e: Exception) {
            Log.w(TAG, "Encrypted storage unavailable, using plain preferences", e)
            app.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        }
        cached = store
        return store
    }

    /**
     * Moves an existing plaintext session across once, then deletes it — so an
     * upgrade doesn't sign the player out, and doesn't leave the old token
     * sitting in the clear either.
     */
    private fun migrateFromPlain(context: Context, secure: SharedPreferences) {
        val old = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        if (old.all.isEmpty()) return

        secure.edit().apply {
            putInt(KEY_USER_ID, old.getInt(KEY_USER_ID, 0))
            putString(KEY_DISPLAY_NAME, old.getString(KEY_DISPLAY_NAME, null))
            putString(KEY_EMAIL, old.getString(KEY_EMAIL, null))
            putString(KEY_NICKNAME, old.getString(KEY_NICKNAME, null))
            putString(KEY_TOKEN, old.getString(KEY_TOKEN, null))
        }.apply()
        old.edit().clear().apply()
    }

    fun load(context: Context) {
        val p = prefs(context)
        val token = p.getString(KEY_TOKEN, null)

        // A stored session from before bearer tokens existed can't authenticate
        // anything, so treat it as signed out rather than leaving the app in a
        // half-state where every account call quietly 401s.
        if (token == null) {
            clear(context)
            return
        }

        Session.userId = p.getInt(KEY_USER_ID, 0).takeIf { it > 0 }
        Session.displayName = p.getString(KEY_DISPLAY_NAME, null)
        Session.email = p.getString(KEY_EMAIL, null)
        Session.nickname = p.getString(KEY_NICKNAME, null)
        Session.token = token
    }

    fun save(context: Context) {
        prefs(context).edit()
            .putInt(KEY_USER_ID, Session.userId ?: 0)
            .putString(KEY_DISPLAY_NAME, Session.displayName)
            .putString(KEY_EMAIL, Session.email)
            .putString(KEY_NICKNAME, Session.nickname)
            .putString(KEY_TOKEN, Session.token)
            .apply()
    }

    fun clear(context: Context) {
        prefs(context).edit().clear().apply()
        // Belt and braces: an older build may have left a plaintext copy.
        context.applicationContext
            .getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
            .edit().clear().apply()

        Session.userId = null
        Session.displayName = null
        Session.email = null
        Session.nickname = null
        Session.token = null
    }
}
