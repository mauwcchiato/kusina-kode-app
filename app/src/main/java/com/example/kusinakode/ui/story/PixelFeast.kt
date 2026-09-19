package com.example.kusinakode.ui.story

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import kotlinx.coroutines.delay

/**
 * A little pixel scene of someone eating, looping.
 *
 * Stands in wherever a real dish photograph would spoil a puzzle — most
 * obviously the Home hero before a player has solved anything, where showing
 * the next level's photo simply handed them the answer.
 */
@Composable
fun PixelFeast(modifier: Modifier = Modifier) {
    var frame by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        while (true) {
            delay(420)
            frame = (frame + 1) % SpriteKain.size
        }
    }

    Box(
        modifier.background(
            Brush.horizontalGradient(listOf(Color(0xFF4A2409), Color(0xFF8A4513)))
        )
    ) {
        // Sits to the right: the hero's headline and Continue button own the
        // left side, and centring the scene just hid it behind the copy.
        PixelArtImage(
            SpriteKain[frame],
            Modifier
                .align(Alignment.CenterEnd)
                .padding(end = 18.dp)
                .fillMaxWidth(0.34f)
        )
    }
}
