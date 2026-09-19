package com.example.kusinakode.ui.game

import android.graphics.Color as AndroidColor
import android.net.Uri
import android.view.Gravity
import android.widget.FrameLayout
import android.widget.VideoView
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.ui.components.clickSfx
import com.example.kusinakode.ui.theme.LightOrange
import kotlin.math.ceil

/**
 * The curtain call, played once the last dish has been cleared.
 *
 * Built the same way as the origin film in StoryModeScreen — the same
 * crop-to-fill sizing, the same lifecycle handling and the same skip — so the
 * two bookends of the game behave identically. The win cue fires alongside it
 * rather than being left to the clip's own audio, so the applause lands even
 * with the phone on silent, and the background music is stopped first so the
 * two are not competing.
 *
 * [onFinish] runs whether the film plays out, the player skips, or it fails to
 * open: finishing the game must never leave someone stuck on a black screen.
 */
@Composable
fun FinaleScreen(onFinish: () -> Unit) {
    val ctx = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val finish by rememberUpdatedState(onFinish)

    var videoView by remember { mutableStateOf<VideoView?>(null) }
    val left = remember { mutableStateOf(false) }

    LaunchedEffect(Unit) {
        SoundFx.stopBgm()
        SoundFx.play(ctx, SoundFx.Cue.Win)
    }

    DisposableEffect(Unit) {
        onDispose { videoView?.stopPlayback() }
    }

    DisposableEffect(lifecycleOwner, videoView) {
        val player = videoView
        val observer = LifecycleEventObserver { _, event ->
            when (event) {
                Lifecycle.Event.ON_PAUSE -> player?.pause()
                Lifecycle.Event.ON_RESUME -> if (!left.value) player?.start()
                else -> Unit
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }

    fun leave() {
        if (left.value) return
        left.value = true
        videoView?.stopPlayback()
        finish()
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color.Black)
    ) {
        AndroidView(
            factory = { c ->
                val host = FrameLayout(c).apply {
                    setBackgroundColor(AndroidColor.BLACK)
                    clipChildren = true
                }
                var videoW = 0
                var videoH = 0
                val player = VideoView(c)

                // VideoView letterboxes by default. Sizing it to overflow the host
                // on its short axis turns that into a centre crop, so the film
                // reaches every edge and the host clips what hangs over.
                fun fillHost() {
                    val hw = host.width
                    val hh = host.height
                    if (videoW <= 0 || videoH <= 0 || hw <= 0 || hh <= 0) return
                    val scale = maxOf(hw.toFloat() / videoW, hh.toFloat() / videoH)
                    val w = ceil(videoW * scale).toInt()
                    val h = ceil(videoH * scale).toInt()
                    val lp = player.layoutParams as? FrameLayout.LayoutParams
                    if (lp != null && lp.width == w && lp.height == h) return
                    // Posted because this also runs from a layout pass, where a
                    // synchronous requestLayout would be dropped.
                    player.post {
                        player.layoutParams = FrameLayout.LayoutParams(w, h, Gravity.CENTER)
                    }
                }

                player.apply {
                    layoutParams = FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        Gravity.CENTER
                    )
                    setVideoURI(Uri.parse("android.resource://${c.packageName}/${R.raw.clap}"))
                    setOnPreparedListener { mp ->
                        val volume = SoundFx.sfxGain()
                        mp.setVolume(volume, volume)
                        mp.isLooping = false
                        videoW = mp.videoWidth
                        videoH = mp.videoHeight
                        fillHost()
                        start()
                    }
                    setOnCompletionListener { leave() }
                    setOnErrorListener { _, _, _ ->
                        // A clip that will not open must not trap the player on
                        // the last screen of the game.
                        leave()
                        true
                    }
                }
                host.addView(player)
                host.addOnLayoutChangeListener { _, _, _, _, _, _, _, _, _ -> fillHost() }
                videoView = player
                host
            },
            modifier = Modifier.fillMaxSize()
        )

        Box(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            TextButton(
                onClick = clickSfx { leave() },
                modifier = Modifier.align(Alignment.TopEnd)
            ) {
                Text(
                    "Skip",
                    color = LightOrange.copy(alpha = 0.7f),
                    fontWeight = FontWeight.Medium,
                    fontSize = 14.sp
                )
            }
        }
    }
}
