package com.example.kusinakode.ui.game

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.unit.dp
import com.example.kusinakode.domain.model.Region

/**
 * The backdrop behind a round in play.
 *
 * This used to be the dish's own photograph, which gave the answer away — you
 * could see the lechon before you had guessed a letter. These are woven and
 * carved motifs instead: they carry the same regional identity without
 * naming the dish, and being drawn rather than photographed they cost the
 * APK nothing and stay sharp at any density.
 *
 * One motif per region, so an island still feels like itself:
 *  - Luzon: banig, the interlaced pandan sleeping mat
 *  - Visayas: capiz shell window panes
 *  - Mindanao: okir and Yakan textile geometry
 *  - Philippines: the parol's starburst
 *
 * Kept deliberately low-contrast — the board and keyboard sit on top of this.
 */
@Composable
fun RegionBackdrop(region: Region, modifier: Modifier = Modifier) {
    Canvas(modifier.fillMaxSize()) {
        when (region) {
            Region.LUZON -> drawBanig()
            Region.VISAYAS -> drawCapiz()
            Region.MINDANAO -> drawOkir()
            Region.PHILIPPINES -> drawParol()
        }
    }
}

/** Luzon — pandan strips crossing over and under, as on a banig. */
private fun DrawScope.drawBanig() {
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF5C3A1C), Color(0xFF7A5024), Color(0xFF4A2C13)))
    )

    val cell = 46.dp.toPx()
    val inset = cell * 0.10f
    val warm = Color(0xFF9A6A33)
    val pale = Color(0xFFB98C4E)

    var row = 0
    var y = -cell
    while (y < size.height + cell) {
        var col = 0
        var x = -cell
        while (x < size.width + cell) {
            // Alternating over/under is what reads as a weave rather than a grid.
            val horizontal = (row + col) % 2 == 0
            val tone = if (horizontal) warm else pale
            if (horizontal) {
                drawRect(
                    color = tone.copy(alpha = 0.30f),
                    topLeft = Offset(x, y + inset),
                    size = Size(cell, cell - inset * 2)
                )
            } else {
                drawRect(
                    color = tone.copy(alpha = 0.22f),
                    topLeft = Offset(x + inset, y),
                    size = Size(cell - inset * 2, cell)
                )
            }
            x += cell
            col++
        }
        y += cell
        row++
    }
}

/** Visayas — the pearly shell panes of a capiz window. */
private fun DrawScope.drawCapiz() {
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF4A3A22), Color(0xFF6E5630), Color(0xFF3E2E1A)))
    )

    val pane = 54.dp.toPx()
    val frame = 3.dp.toPx()
    val shell = Color(0xFFF3E4C4)

    var y = -pane
    var row = 0
    while (y < size.height + pane) {
        var x = -pane
        var col = 0
        while (x < size.width + pane) {
            // Shell is never uniform — the slight per-pane variation is what
            // stops this looking like graph paper.
            val tint = 0.10f + ((row * 7 + col * 3) % 5) * 0.022f
            drawRect(
                color = shell.copy(alpha = tint),
                topLeft = Offset(x + frame, y + frame),
                size = Size(pane - frame * 2, pane - frame * 2)
            )
            drawRect(
                color = Color(0xFF2E1F10).copy(alpha = 0.35f),
                topLeft = Offset(x, y),
                size = Size(pane, frame)
            )
            drawRect(
                color = Color(0xFF2E1F10).copy(alpha = 0.35f),
                topLeft = Offset(x, y),
                size = Size(frame, pane)
            )
            x += pane
            col++
        }
        y += pane
        row++
    }
}

/** Mindanao — okir diamonds banded like a Yakan weave. */
private fun DrawScope.drawOkir() {
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF3F2A1B), Color(0xFF6B3F1E), Color(0xFF35210F)))
    )

    val band = 58.dp.toPx()
    val gold = Color(0xFFD9A227)
    val jade = Color(0xFF2E7D5B)
    val ivory = Color(0xFFF1E2C6)

    var y = -band
    var index = 0
    while (y < size.height + band) {
        val accent = when (index % 3) {
            0 -> gold
            1 -> jade
            else -> ivory
        }
        // Thin rule at the top of each band, then a run of diamonds.
        drawRect(
            color = accent.copy(alpha = 0.20f),
            topLeft = Offset(0f, y),
            size = Size(size.width, 2.dp.toPx())
        )

        val step = band * 0.9f
        var x = -step
        while (x < size.width + step) {
            val cx = x + step / 2f
            val cy = y + band / 2f
            val r = band * 0.26f
            val diamond = Path().apply {
                moveTo(cx, cy - r)
                lineTo(cx + r, cy)
                lineTo(cx, cy + r)
                lineTo(cx - r, cy)
                close()
            }
            drawPath(diamond, color = accent.copy(alpha = 0.16f))
            x += step
        }
        y += band
        index++
    }
}

/** Philippines — the parol, rays out from a single point. */
private fun DrawScope.drawParol() {
    drawRect(
        Brush.verticalGradient(listOf(Color(0xFF5A2E12), Color(0xFF8A4513), Color(0xFF3A1B08)))
    )

    val centre = Offset(size.width / 2f, size.height * 0.34f)
    val reach = size.width * 1.6f
    val gold = Color(0xFFD9A227)
    val cream = Color(0xFFF1E2C6)

    // 24 sectors, every other one lit, like a lantern's paper panels.
    for (i in 0 until 24) {
        val angle = i * 15f
        rotate(degrees = angle, pivot = centre) {
            val half = (reach * 0.13f)
            val ray = Path().apply {
                moveTo(centre.x, centre.y)
                lineTo(centre.x - half, centre.y - reach)
                lineTo(centre.x + half, centre.y - reach)
                close()
            }
            drawPath(ray, color = (if (i % 2 == 0) gold else cream).copy(alpha = 0.07f))
        }
    }

    // Concentric rings give the burst a centre to come from.
    for (i in 1..4) {
        drawCircle(
            color = cream.copy(alpha = 0.05f),
            radius = size.width * (0.10f * i),
            center = centre,
            style = androidx.compose.ui.graphics.drawscope.Stroke(width = 2.dp.toPx())
        )
    }
    drawCircle(
        brush = Brush.radialGradient(
            colors = listOf(gold.copy(alpha = 0.22f), Color.Transparent),
            center = centre,
            radius = size.width * 0.35f
        ),
        radius = size.width * 0.35f,
        center = centre
    )
}
