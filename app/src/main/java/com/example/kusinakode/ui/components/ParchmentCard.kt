package com.example.kusinakode.ui.components

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

// ---- Capiz-shell parchment over a narra ledge ----
private val Ledge = Color(0xFF421905)
private val ParchTop = Color(0xFFFFFDF8)
private val ParchMid = Color(0xFFF9F1E2)
private val ParchLow = Color(0xFFEEDDC0)
private val CardEdge = Color(0xFF5A2808)
private val SeamTan = Color(0xFFCBB48C)
private val HairlineCream = Color(0xFFEFE3CF)
private val GlowWhite = Color(0xFFFFFFFF)
private val GlowWarm = Color(0xFFFFF9EE)
private val GlowLow = Color(0xFFE5D0AD)

/**
 * The shared dialog plate: a warm parchment card on a raised narra ledge,
 * stitched like a banig.
 *
 * Replaces the flat cream surfaces the overlays used to sit on. Purely a
 * backdrop — it takes no clicks and imposes nothing on its content beyond the
 * inset needed to clear the border.
 *
 * Geometry follows the source artwork, which is drawn on a 540-unit-wide board
 * with a 490-unit card, so the border weights stay in the same proportion the
 * design was cut at.
 */
@Composable
fun ParchmentCard(
    modifier: Modifier = Modifier,
    contentPadding: Dp = 22.dp,
    content: @Composable BoxScope.() -> Unit
) {
    Box(
        modifier
            .fillMaxWidth()
            .drawBehind { drawParchmentPlate() }
            .padding(contentPadding)
    ) {
        content()
    }
}

/**
 * The plate on its own, for hosts that already own their layout and only need
 * the background painted behind what they draw.
 */
fun Modifier.parchmentPlate(): Modifier = drawBehind { drawParchmentPlate() }

private fun DrawScope.drawParchmentPlate() {
    // The artwork's card is 490 units wide; scaling by that keeps every
    // border weight proportional however wide the dialog ends up.
    val u = size.width / 490f
    fun len(v: Float) = v * u

    val radius = len(38f)
    val ledgeDrop = len(10f)
    val bodyH = size.height - ledgeDrop

    // Extruded ledge peeking out below the card gives it thickness.
    drawRoundRect(
        color = Ledge,
        topLeft = Offset(0f, ledgeDrop),
        size = Size(size.width, bodyH),
        cornerRadius = CornerRadius(radius)
    )

    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to ParchTop, 0.6f to ParchMid, 1f to ParchLow,
            startY = 0f, endY = bodyH
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, bodyH),
        cornerRadius = CornerRadius(radius)
    )

    // Paper grain: bright at the top, settling warm at the edges.
    drawRoundRect(
        brush = Brush.radialGradient(
            0f to GlowWhite.copy(alpha = 0.6f),
            0.6f to GlowWarm.copy(alpha = 0.2f),
            1f to GlowLow.copy(alpha = 0.4f),
            center = Offset(size.width / 2f, bodyH * 0.3f),
            radius = size.width * 0.7f
        ),
        topLeft = Offset.Zero,
        size = Size(size.width, bodyH),
        cornerRadius = CornerRadius(radius)
    )

    val edge = len(3.5f)
    drawRoundRect(
        color = CardEdge,
        topLeft = Offset(edge / 2f, edge / 2f),
        size = Size(size.width - edge, bodyH - edge),
        cornerRadius = CornerRadius(radius - edge / 2f),
        style = Stroke(width = edge)
    )

    // Stitched banig seam.
    val seam = len(9f)
    drawRoundRect(
        color = SeamTan,
        topLeft = Offset(seam, seam),
        size = Size(size.width - seam * 2f, bodyH - seam * 2f),
        cornerRadius = CornerRadius(len(30f)),
        style = Stroke(
            width = len(2f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(len(8f), len(5f)))
        )
    )

    val hair = len(13f)
    drawRoundRect(
        color = HairlineCream.copy(alpha = 0.6f),
        topLeft = Offset(hair, hair),
        size = Size(size.width - hair * 2f, bodyH - hair * 2f),
        cornerRadius = CornerRadius(len(28f)),
        style = Stroke(width = len(1f))
    )
}
