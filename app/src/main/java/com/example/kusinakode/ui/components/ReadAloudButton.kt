package com.example.kusinakode.ui.components

import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.VolumeUp
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material3.Icon
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.SoundFx
import com.example.kusinakode.ui.tutorial.ChefVoice
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * The speaker button every read-aloud shares: plays [clip] if given, otherwise
 * speaks each of [parts] in turn. Music sits lower while it reads, and leaving
 * the screen stops it. [what] names the content for screen readers
 * ("Read the steps aloud").
 *
 * Moved out of the dish page so the trivia, ingredient and equipment cards can
 * read themselves too (panel revision: text-to-speech across the interactive
 * parts of the app).
 */
@Composable
fun ReadAloudButton(key: String, parts: List<String>, what: String, clip: Int? = null) {
    val ctx = LocalContext.current
    val voice = remember { ChefVoice(ctx) }
    var playing by remember(key) { mutableStateOf(false) }
    val scope = rememberCoroutineScope()
    var job by remember { mutableStateOf<Job?>(null) }

    DisposableEffect(voice) {
        onDispose {
            job?.cancel()
            voice.shutdown()
            SoundFx.duckBgm(false)
        }
    }

    val toggle: () -> Unit = {
        if (playing) {
            job?.cancel()
        } else if (SoundFx.sfxGain() > 0f) {
            job = scope.launch {
                playing = true
                SoundFx.duckBgm(true)
                try {
                    val volume = SoundFx.sfxGain()
                    if (clip == null || !voice.playClip(clip, volume)) {
                        for (part in parts.filter { it.isNotBlank() }) {
                            if (!voice.speak(part, volume)) break
                            // A breath between parts, as a cook would pause.
                            delay(450)
                        }
                    }
                } finally {
                    SoundFx.duckBgm(false)
                    playing = false
                }
            }
        }
    }

    // While it reads, a ring breathes out from the button so it is plain
    // something is playing even with the sound low.
    val pulse = rememberInfiniteTransition(label = "read_aloud_pulse")
    val ring by pulse.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1100)),
        label = "read_aloud_ring"
    )

    Box(Modifier.size(38.dp), contentAlignment = Alignment.Center) {
        if (playing) {
            Box(
                Modifier
                    .size(38.dp)
                    .graphicsLayer {
                        scaleX = 1f + ring * 0.35f
                        scaleY = 1f + ring * 0.35f
                        alpha = (1f - ring) * 0.45f
                    }
                    .clip(CircleShape)
                    .background(PlayNowBrown)
            )
        }
        Box(
            Modifier
                .size(34.dp)
                .shadow(3.dp, CircleShape)
                .clip(CircleShape)
                .background(PlayNowBrown)
                .border(1.5.dp, Color(0xFFFFD24A).copy(alpha = 0.55f), CircleShape)
                .clickable(onClick = clickSfx(toggle)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                if (playing) Icons.Default.Stop else Icons.AutoMirrored.Filled.VolumeUp,
                contentDescription = if (playing) "Stop reading $what" else "Read $what aloud",
                tint = Color.White,
                modifier = Modifier.size(if (playing) 18.dp else 19.dp)
            )
        }
    }
}
