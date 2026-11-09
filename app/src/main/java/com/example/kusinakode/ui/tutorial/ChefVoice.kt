package com.example.kusinakode.ui.tutorial

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.annotation.RawRes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import kotlin.coroutines.resume

/**
 * The chef's speaking voice.
 *
 * Scripted lines play their recorded clip ([playClip]). Any line without one
 * falls back to the device's text-to-speech ([speak]), so a line added to the
 * script before its clip is recorded is still heard. If neither can play,
 * both report false and the caller paces the line by reading time — the
 * dialogue never waits on a voice that isn't coming.
 */
class ChefVoice(context: Context) {

    private val appContext = context.applicationContext
    private val ready = CompletableDeferred<Boolean>()
    private val pending = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()
    private var nextId = 0

    private val tts: TextToSpeech = TextToSpeech(context.applicationContext) { status ->
        ready.complete(status == TextToSpeech.SUCCESS && configure())
    }

    private fun configure(): Boolean {
        val lang = tts.setLanguage(Locale.US)
        if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
            return false
        }
        // The chef is a male narrator. A male voice speaks at its natural
        // pitch; without one, the default voice is pitched down to stand in.
        val male = pickMaleVoice()
        if (male != null) tts.setVoice(male)
        tts.setPitch(if (male != null) 0.95f else 0.75f)
        // Unhurried, the pace of someone telling a story.
        tts.setSpeechRate(0.9f)
        tts.setOnUtteranceProgressListener(object : UtteranceProgressListener() {
            override fun onStart(utteranceId: String) = Unit
            override fun onDone(utteranceId: String) = finish(utteranceId, true)
            override fun onStop(utteranceId: String, interrupted: Boolean) = finish(utteranceId, false)
            @Deprecated("Deprecated in Java")
            override fun onError(utteranceId: String) = finish(utteranceId, false)
            override fun onError(utteranceId: String, errorCode: Int) = finish(utteranceId, false)
        })
        return true
    }

    /**
     * Android has no gender field on [Voice], so male voices are found by
     * name: Google's engine uses known codes, other engines usually say
     * "male" outright. Installed voices win over ones that need a network.
     */
    private fun pickMaleVoice(): Voice? {
        val english = runCatching { tts.voices }.getOrNull().orEmpty()
            .filter { it.locale.language == "en" && !it.features.contains(TextToSpeech.Engine.KEY_FEATURE_NOT_INSTALLED) }
            .sortedBy { it.isNetworkConnectionRequired }

        GOOGLE_MALE_CODES.forEach { code ->
            english.firstOrNull { it.name.contains("-x-$code") }?.let { return it }
        }
        return english.firstOrNull {
            val name = it.name.lowercase()
            "male" in name && "female" !in name
        }
    }

    private fun finish(id: String, spoke: Boolean) {
        pending.remove(id)?.let { if (it.isActive) it.resume(spoke) }
    }

    /**
     * Says [text] and suspends until the line has been spoken. Returns false
     * when nothing was heard, so the caller can pace the line itself.
     * Cancelling the caller cuts the chef off mid-sentence.
     */
    suspend fun speak(text: String, volume: Float): Boolean {
        val ok = withTimeoutOrNull(START_TIMEOUT_MS) { ready.await() } ?: false
        if (!ok || text.isBlank()) return false

        val id = "chef_${nextId++}"
        return suspendCancellableCoroutine { cont ->
            pending[id] = cont
            cont.invokeOnCancellation {
                pending.remove(id)
                tts.stop()
            }
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
            }
            if (tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) != TextToSpeech.SUCCESS) {
                finish(id, false)
            }
        }
    }

    /**
     * Plays a recorded line — the chef's own voice, made once in ElevenLabs
     * and packaged as res/raw/chef_*.mp3 — and suspends until it ends. Returns
     * false when the clip cannot play, so the caller can fall back to [speak].
     * Cancelling the caller stops the clip mid-sentence.
     */
    suspend fun playClip(@RawRes clip: Int, volume: Float): Boolean {
        val player = MediaPlayer.create(appContext, clip) ?: return false
        val v = volume.coerceIn(0f, 1f)
        player.setVolume(v, v)
        return try {
            suspendCancellableCoroutine { cont ->
                player.setOnCompletionListener { if (cont.isActive) cont.resume(true) }
                player.setOnErrorListener { _, _, _ ->
                    if (cont.isActive) cont.resume(false)
                    true
                }
                cont.invokeOnCancellation { runCatching { player.stop() } }
                player.start()
            }
        } finally {
            player.release()
        }
    }

    fun shutdown() {
        pending.values.forEach { if (it.isActive) it.resume(false) }
        pending.clear()
        tts.stop()
        tts.shutdown()
    }

    private companion object {
        const val START_TIMEOUT_MS = 2_000L

        /** Google TTS male voice codes, best-sounding first (en-US, then en-GB, en-AU). */
        val GOOGLE_MALE_CODES = listOf("iom", "iol", "tpd", "rjs", "gbd", "gbb", "aud", "auc")
    }
}
