package com.example.kusinakode

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.annotation.RawRes
import com.example.kusinakode.domain.model.Region

/**
 * Recorded kitchen SFX, voicelines and BGM from the Sfx pack.
 *
 * One-shots use [MediaPlayer] rather than SoundPool — short MP3 clicks
 * (key, coin, bomb) lose their attack in SoundPool's decoder and play
 * as silence, which is why delete could be heard while letters could not.
 * Looping beds share a separate player and the same Settings toggle.
 */
object SoundFx {

    private val main = Handler(Looper.getMainLooper())
    private val shotLock = Any()
    private val shots = ArrayDeque<MediaPlayer>()
    private var bgmPlayer: MediaPlayer? = null
    private var currentBgm: Bgm? = null
    private var bgmPaused = false

    private val sfxAttrs = AudioAttributes.Builder()
        .setUsage(AudioAttributes.USAGE_GAME)
        .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
        .build()


    enum class Cue(@RawRes val res: Int) {
        Key(R.raw.sfx_key),
        TileCorrect(R.raw.sfx_tile_correct),
        TilePresent(R.raw.sfx_tile_present),
        TileAbsent(R.raw.sfx_tile_absent),
        TileFlip(R.raw.sfx_tile_flip),
        Win(R.raw.sfx_win),
        Lose(R.raw.sfx_lose),
        Coin(R.raw.sfx_coin),
        Badge(R.raw.sfx_badge),
        Baul(R.raw.sfx_key),
        BaulOpen(R.raw.sfx_reveal),
        Reveal(R.raw.sfx_reveal),
        Backspace(R.raw.sfx_backspace),
        Bomb(R.raw.sfx_bomb),
        Solve(R.raw.sfx_badge),
        Invalid(R.raw.sfx_invalid),
        Nav(R.raw.sfx_nav),
        Submit(R.raw.sfx_submit),
        Sarap(R.raw.vo_sarap),
        Nice(R.raw.vo_nice),
        GotIt(R.raw.vo_got_it),
        KeepGoing(R.raw.vo_keep_going),
        LastTaste(R.raw.vo_last_taste),
        Malapit(R.raw.vo_malapit),
        NextDish(R.raw.vo_next_dish)
    }

    enum class Bgm(@RawRes val res: Int, val loop: Boolean) {
        Welcome(R.raw.bg_welcome, true),
        Home(R.raw.bg_home, true),
        Map(R.raw.bg_map, true),
        Game(R.raw.bg_game, true),
        GameLuzon(R.raw.bg_game_luzon, true),
        GameVisayas(R.raw.bg_game_visayas, true),
        GameMindanao(R.raw.bg_game_mindanao, true),
        GamePhilippines(R.raw.bg_game_philippines, true),
        Win(R.raw.bg_win, true),
        WinFirst(R.raw.bg_win_first, true),
        Learn(R.raw.bg_learn, true),
        Wallet(R.raw.bg_wallet, true),
        Profile(R.raw.bg_profile, true);

        companion object {
            fun forGame(region: Region): Bgm = when (region) {
                Region.LUZON -> GameLuzon
                Region.VISAYAS -> GameVisayas
                Region.MINDANAO -> GameMindanao
                Region.PHILIPPINES -> GamePhilippines
            }

            fun forWin(firstTry: Boolean): Bgm = if (firstTry) WinFirst else Win

            fun forRoute(route: String?): Bgm? = when {
                route.isNullOrBlank() -> Welcome
                // The round picks its island bed; the story film carries its own audio.
                route.startsWith("game") || route == "story" -> null
                route == "tutorial_play" -> Game
                route.startsWith("levels") -> Map
                route == "completed" || route == "kodex_dishes" || route.startsWith("dish") -> Learn
                route == "rewards" ||
                    route == "shop_avatar" ||
                    route == "shop_docs" ||
                    route == "pantry" ||
                    route.startsWith("shop_pantry") -> Wallet
                route == "profile" || route == "leadership" -> Profile
                route == "welcome" || route == "login" || route == "signup" ||
                    route == "forgot_password" || route == "splash" ||
                    route.startsWith("onboarding") -> Welcome
                else -> Home
            }
        }
    }

    @Suppress("UNUSED_PARAMETER")
    fun warm(context: Context) {
        // MediaPlayer one-shots are created on demand; nothing to preload.
    }

    fun play(context: Context, cue: Cue) {
        if (sfxGain() <= 0f) return
        val app = context.applicationContext
        main.post { playRes(app, cue.res) }
    }

    /** Live gain 0..1 from Settings. Mute is 0, not a separate flag. */
    fun sfxGain(): Float {
        val prefs = KusinaSettings.prefs.value
        if (!prefs.soundEffects) return 0f
        return prefs.sfxVolume.coerceIn(0f, 1f)
    }

    /** Push the current gain onto the looping bed without restarting it. */
    fun applyVolume() {
        val gain = sfxGain()
        main.post {
            bgmPlayer?.setVolume(0.45f * gain, 0.45f * gain)
        }
    }

    fun tap(context: Context) {
        val prefs = KusinaSettings.prefs.value
        if (prefs.haptics) vibrate(context, 12)
        if (prefs.soundEffects) play(context, Cue.Key)
    }

    fun setBgm(context: Context, bgm: Bgm) {
        if (!KusinaSettings.prefs.value.soundEffects) {
            stopBgm()
            return
        }
        val app = context.applicationContext
        main.post {
            if (currentBgm == bgm && bgmPlayer != null) {
                if (bgmPaused) {
                    runCatching { bgmPlayer?.start() }
                    bgmPaused = false
                }
                return@post
            }
            releaseBgm()
            currentBgm = bgm
            bgmPaused = false
            val player = MediaPlayer.create(app, bgm.res) ?: return@post
            player.isLooping = bgm.loop
            val gain = sfxGain()
            player.setVolume(0.45f * gain, 0.45f * gain)
            if (!bgm.loop) {
                player.setOnCompletionListener {
                    if (currentBgm == bgm) releaseBgm()
                }
            }
            bgmPlayer = player
            runCatching { player.start() }
        }
    }

    fun pauseBgm() {
        main.post {
            val player = bgmPlayer ?: return@post
            if (player.isPlaying) {
                runCatching { player.pause() }
                bgmPaused = true
            }
        }
    }

    fun resumeBgm() {
        if (!KusinaSettings.prefs.value.soundEffects) return
        main.post {
            val player = bgmPlayer ?: return@post
            if (!player.isPlaying) {
                runCatching { player.start() }
            }
            bgmPaused = false
        }
    }

    fun stopBgm() {
        main.post { releaseBgm() }
    }

    fun vibrate(context: Context, millis: Long) {
        if (!KusinaSettings.prefs.value.haptics) return
        runCatching {
            val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
                (context.getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as? VibratorManager)
                    ?.defaultVibrator
            } else {
                @Suppress("DEPRECATION")
                context.getSystemService(Context.VIBRATOR_SERVICE) as? Vibrator
            } ?: return@runCatching
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                vibrator.vibrate(
                    VibrationEffect.createOneShot(millis, VibrationEffect.DEFAULT_AMPLITUDE)
                )
            } else {
                @Suppress("DEPRECATION")
                vibrator.vibrate(millis)
            }
        }
    }

    private fun playRes(context: Context, @RawRes res: Int) {
        runCatching {
            val player = MediaPlayer()
            player.setAudioAttributes(sfxAttrs)
            context.resources.openRawResourceFd(res).use { fd ->
                player.setDataSource(fd.fileDescriptor, fd.startOffset, fd.length)
            }
            val gain = sfxGain()
            player.setVolume(gain, gain)
            player.setOnCompletionListener { done -> releaseShot(done) }
            player.setOnErrorListener { done, _, _ ->
                releaseShot(done)
                true
            }
            player.prepare()
            synchronized(shotLock) {
                while (shots.size >= 8) {
                    shots.removeFirstOrNull()?.let { old ->
                        runCatching { old.stop() }
                        runCatching { old.release() }
                    }
                }
                shots.addLast(player)
            }
            player.start()
        }
    }

    private fun releaseShot(player: MediaPlayer) {
        synchronized(shotLock) { shots.remove(player) }
        runCatching { player.release() }
    }

    private fun releaseBgm() {
        runCatching {
            bgmPlayer?.setOnCompletionListener(null)
            bgmPlayer?.stop()
            bgmPlayer?.release()
        }
        bgmPlayer = null
        currentBgm = null
        bgmPaused = false
    }
}
