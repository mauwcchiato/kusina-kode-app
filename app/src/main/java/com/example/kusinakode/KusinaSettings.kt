package com.example.kusinakode

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Everything the player can turn on or off.
 *
 * Device-wide rather than per-account: these are preferences about the phone
 * in your hand — volume, vibration, motion — not things that should follow a
 * login to somebody else's device.
 *
 * Backed by the same SharedPreferences file the rest of the app uses, and
 * mirrored into a StateFlow so a toggle repaints immediately instead of
 * waiting for the screen to be rebuilt.
 */
data class KusinaPrefs(
    val soundEffects: Boolean = true,
    /** 0 silent … 1 full. Applied live to SFX and BGM. */
    val sfxVolume: Float = 1f,
    val haptics: Boolean = true,
    val notifications: Boolean = true,
    val reduceMotion: Boolean = false,
    val highContrastTiles: Boolean = false,
    val showTimer: Boolean = true,
    val confirmSpending: Boolean = true
)

object KusinaSettings {
    private const val PREFS = "kusinakode_prefs"

    private const val K_SOUND = "set_sound_fx"
    private const val K_VOLUME = "set_sfx_volume"
    private const val K_HAPTIC = "set_haptics"
    private const val K_NOTIF = "set_notifications"
    private const val K_MOTION = "set_reduce_motion"
    private const val K_CONTRAST = "set_high_contrast"
    private const val K_TIMER = "set_show_timer"
    private const val K_CONFIRM = "set_confirm_spend"

    private val _prefs = MutableStateFlow(KusinaPrefs())
    val prefs: StateFlow<KusinaPrefs> = _prefs.asStateFlow()

    fun load(context: Context) {
        val p = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val soundOn = p.getBoolean(K_SOUND, true)
        val storedVolume = p.getFloat(K_VOLUME, -1f)
        val volume = when {
            storedVolume >= 0f -> storedVolume.coerceIn(0f, 1f)
            soundOn -> 1f
            else -> 0f
        }
        _prefs.value = KusinaPrefs(
            soundEffects = volume > 0.01f,
            sfxVolume = volume,
            haptics = p.getBoolean(K_HAPTIC, true),
            notifications = p.getBoolean(K_NOTIF, true),
            reduceMotion = p.getBoolean(K_MOTION, false),
            highContrastTiles = p.getBoolean(K_CONTRAST, false),
            showTimer = p.getBoolean(K_TIMER, true),
            confirmSpending = p.getBoolean(K_CONFIRM, true)
        )
    }

    private fun write(context: Context, key: String, value: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit().putBoolean(key, value).apply()
    }

    fun setSoundEffects(context: Context, on: Boolean) {
        val restore = _prefs.value.sfxVolume.takeIf { it > 0.01f } ?: 1f
        setSfxVolume(context, if (on) restore else 0f)
    }

    fun setSfxVolume(context: Context, volume: Float) {
        val v = volume.coerceIn(0f, 1f)
        val on = v > 0.01f
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .edit()
            .putFloat(K_VOLUME, v)
            .putBoolean(K_SOUND, on)
            .apply()
        _prefs.value = _prefs.value.copy(sfxVolume = v, soundEffects = on)
        SoundFx.applyVolume()
    }

    fun setHaptics(context: Context, on: Boolean) {
        write(context, K_HAPTIC, on); _prefs.value = _prefs.value.copy(haptics = on)
    }

    fun setNotifications(context: Context, on: Boolean) {
        write(context, K_NOTIF, on); _prefs.value = _prefs.value.copy(notifications = on)
        PlayNudgeScheduler.sync(context)
    }

    fun setReduceMotion(context: Context, on: Boolean) {
        write(context, K_MOTION, on); _prefs.value = _prefs.value.copy(reduceMotion = on)
    }

    fun setHighContrastTiles(context: Context, on: Boolean) {
        write(context, K_CONTRAST, on); _prefs.value = _prefs.value.copy(highContrastTiles = on)
    }

    fun setShowTimer(context: Context, on: Boolean) {
        write(context, K_TIMER, on); _prefs.value = _prefs.value.copy(showTimer = on)
    }

    fun setConfirmSpending(context: Context, on: Boolean) {
        write(context, K_CONFIRM, on); _prefs.value = _prefs.value.copy(confirmSpending = on)
    }
}
