package com.example.kusinakode.ui.components

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import coil.compose.AsyncImage
import coil.request.ImageRequest

/**
 * Draws a level's art, wherever it happens to live.
 *
 * The dishes that ship in the APK have a compiled drawable and no URL, so
 * this is a plain [Image] for them — no network, no loading state, exactly
 * what it was before. A dish added from the admin panel has the opposite: a
 * URL on the XAMPP host, and [fallback] is only a stand-in.
 *
 * ## What is drawn while the fetch is in flight
 *
 * Nothing. [fallback] as the placeholder meant a panel-added dish showed the
 * plate-and-cutlery graphic first and then snapped to the real photo, which
 * read as a bug. A flat cream tint instead was no better: crossfading out of
 * it tints the first frames of the photo, so the card looked veiled. Drawing
 * nothing lets the fade come out of whatever is behind, which every caller
 * already colours correctly. [fallback] is kept for the one case it is honest
 * about — the image could not be loaded at all.
 *
 * This deliberately uses [AsyncImage] rather than driving a painter by hand.
 * `rememberAsyncImagePainter` only begins its request once the painter is
 * drawn, so branching on its state and drawing something *else* while loading
 * means the request never starts and the image never arrives.
 */
@Composable
fun LevelImage(
    url: String?,
    @DrawableRes fallback: Int,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    contentScale: ContentScale = ContentScale.Crop
) {
    if (url.isNullOrBlank()) {
        Image(
            painter = painterResource(fallback),
            contentDescription = contentDescription,
            modifier = modifier,
            contentScale = contentScale
        )
        return
    }

    AsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(url)
            // Fade in rather than cut, so arriving late looks deliberate.
            .crossfade(220)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        error = painterResource(fallback)
    )
}
