package com.example.kusinakode.ui.tutorial

import android.content.Context
import android.media.MediaPlayer
import android.os.Bundle
import androidx.annotation.RawRes
import android.speech.tts.TextToSpeech
import android.speech.tts.UtteranceProgressListener
import android.speech.tts.Voice
import android.util.Log
import kotlinx.coroutines.CancellableContinuation
import kotlinx.coroutines.CompletableDeferred
import kotlinx.coroutines.suspendCancellableCoroutine
import kotlinx.coroutines.withTimeoutOrNull
import java.util.Locale
import java.util.concurrent.ConcurrentHashMap
import java.util.concurrent.atomic.AtomicInteger
import kotlin.coroutines.resume

/**
 * The chef's speaking voice.
 *
 * Scripted lines play their recorded clip ([playClip]). Any line without one
 * falls back to the device's text-to-speech ([speak]), so a line added to the
 * script before its clip is recorded is still heard. If neither can play,
 * both report false and the caller paces the line by reading time — the
 * dialogue never waits on a voice that isn't coming.
 *
 * One text-to-speech engine serves every screen ([Engine]). Each screen used
 * to start its own and shut it down on leaving, and an engine takes a few
 * seconds to start on a real phone - longer than the line was willing to
 * wait - so the first card of every guided tour was silent and only the
 * second one spoke. The engine now starts once, when the app does
 * ([warmUp]), and stays up.
 */
class ChefVoice(context: Context) {

    private val appContext = context.applicationContext
    private val engine = Engine.get(appContext)

    /**
     * Says [text] and suspends until the line has been spoken. Returns false
     * when nothing was heard, so the caller can pace the line itself.
     * Cancelling the caller cuts the chef off mid-sentence.
     */
    suspend fun speak(text: String, volume: Float): Boolean {
        val ok = withTimeoutOrNull(START_TIMEOUT_MS) { engine.ready.await() } ?: false
        if (!ok || text.isBlank()) return false

        val id = "chef_${nextId.incrementAndGet()}"
        return suspendCancellableCoroutine { cont ->
            engine.pending[id] = cont
            cont.invokeOnCancellation {
                engine.pending.remove(id)
                engine.tts.stop()
            }
            val params = Bundle().apply {
                putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, volume.coerceIn(0f, 1f))
            }
            if (engine.tts.speak(text, TextToSpeech.QUEUE_FLUSH, params, id) != TextToSpeech.SUCCESS) {
                engine.finish(id, false)
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

    /**
     * The screen is leaving: stop whatever it was saying. The engine itself
     * stays up for the next screen, so it is never cold again.
     */
    fun shutdown() {
        engine.pending.values.forEach { if (it.isActive) it.resume(false) }
        engine.pending.clear()
        engine.tts.stop()
    }

    /** The one text-to-speech engine. Its listener resolves whichever screen's line finished. */
    private class Engine private constructor(context: Context) {
        val ready = CompletableDeferred<Boolean>()
        val pending = ConcurrentHashMap<String, CancellableContinuation<Boolean>>()
        val tts: TextToSpeech = TextToSpeech(context) { status ->
            ready.complete(status == TextToSpeech.SUCCESS && configure())
        }

        fun finish(id: String, spoke: Boolean) {
            pending.remove(id)?.let { if (it.isActive) it.resume(spoke) }
        }

        private fun configure(): Boolean {
            val lang = tts.setLanguage(Locale.US)
            if (lang == TextToSpeech.LANG_MISSING_DATA || lang == TextToSpeech.LANG_NOT_SUPPORTED) {
                return false
            }
            // The chef is a male narrator, so a male voice is used when the phone
            // has one. Every voice speaks at its natural pitch: pitching the
            // default voice down to stand in for a male one made it sound
            // muffled, and a slowed rate made it drag.
            val male = pickMaleVoice()
            if (male != null) tts.setVoice(male)
            tts.setPitch(1.0f)
            tts.setSpeechRate(0.97f)
            Log.i(
                "ChefVoice",
                "voice=" + (runCatching { tts.voice?.name }.getOrNull() ?: "default") + " male=" + (male != null)
            )
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

        companion object {
            /** Google TTS male voice codes, best-sounding first (en-US, then en-GB, en-AU). */
            private val GOOGLE_MALE_CODES = listOf("iom", "iol", "tpd", "rjs", "gbd", "gbb", "aud", "auc")

            @Volatile private var instance: Engine? = null

            fun get(context: Context): Engine =
                instance ?: synchronized(this) {
                    instance ?: Engine(context.applicationContext).also { instance = it }
                }
        }
    }

    companion object {
        /**
         * How long a line waits for the engine on a cold start. A screen that
         * moves on cancels the wait, so a long one costs nothing there.
         */
        private const val START_TIMEOUT_MS = 6_000L
        private val nextId = AtomicInteger()

        /** Starts the engine early, from the Application, so the first line is never the one that waits. */
        fun warmUp(context: Context) {
            Engine.get(context)
        }
    }
}
