package com.example.kusinakode.ui.settings

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.translate
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.KusinaSettings
import com.example.kusinakode.SoundFx
import com.example.kusinakode.ui.theme.BeVietnamPro
import kotlin.math.roundToInt

// ---- Capsule palette: warm parchment over narra-wood terracotta ----
private val EdgeClay      = Color(0xFF8C5C38)
private val FaceTop       = Color(0xFFFFFDF8)
private val FaceMid       = Color(0xFFF6EEDB)
private val FaceBottom    = Color(0xFFE9DCBF)
private val FaceStroke    = Color(0xFF6A3B18)
private val StitchTan     = Color(0xFFD1BE9B)
private val SpeakerTop    = Color(0xFF6A2603)
private val SpeakerBottom = Color(0xFF4B2005)
private val SpeakerInk    = Color(0xFF4B2005)
private val GrooveFill    = Color(0xFFDDD0BC)
private val GrooveStroke  = Color(0xFF522204)
private val GrooveShade   = Color(0xFF321404)
private val FillStart     = Color(0xFFCA5B17)
private val FillMid       = Color(0xFFE27329)
private val FillEnd       = Color(0xFFF59338)
private val KnobHot       = Color(0xFFFFA860)
private val KnobDeep      = Color(0xFF9C3E08)
private val KnobDeepest   = Color(0xFF6A2603)
private val KnobRim       = Color(0xFF4B2005)
private val KnobBevel     = Color(0xFFFFA756)
private val KnobCore      = Color(0xFFFFF2D6)
private val KnobCoreEdge  = Color(0xFF883407)
private val ValueInk      = Color(0xFF4B2005)

// Geometry follows the capsule artwork at 1 SVG unit : 1 dp, so the proportions
// hold at any width — only the track between the icon and the readout stretches.
private val CapsuleHeight   = 52.dp
private val CapsuleTop      = 4.dp
private val BaseLift        = 6.dp
private val ComponentHeight = 66.dp
private val IconCenterX     = 27.dp
private val TrackStartX     = 59.dp
private val TrackEndInset   = 58.dp
private val ValueBoxWidth   = 46.dp
private val ValueEndPad     = 6.dp
private val KnobRadius      = 14.dp
private val GrooveHeight    = 12.dp

/**
 * Live Sound FX bar. Dragging writes immediately so the bed and the next
 * tap already use the new gain — pause or Settings, same control.
 */
@Composable
fun SfxVolumeSlider(
    modifier: Modifier = Modifier,
    mutedColor: Color = Color(0xFF8D6E63),
    showCaption: Boolean = true
) {
    val ctx = LocalContext.current
    val prefs by KusinaSettings.prefs.collectAsState()
    var lastAudible by remember {
        mutableFloatStateOf(prefs.sfxVolume.takeIf { it > 0.01f } ?: 1f)
    }
    val volume = prefs.sfxVolume

    Column(modifier.fillMaxWidth()) {
        if (showCaption) {
            Text(
                "SOUND FX",
                color = mutedColor,
                fontFamily = BeVietnamPro,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.4.sp
            )
            Spacer(Modifier.height(2.dp))
        }

        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .height(ComponentHeight)
        ) {
            val trackWidth = (maxWidth - TrackStartX - TrackEndInset).coerceAtLeast(48.dp)
            val density = LocalDensity.current
            val knobPx = with(density) { KnobRadius.toPx() }
            val trackPx = with(density) { trackWidth.toPx() }

            Canvas(Modifier.fillMaxSize()) { drawCapsule(volume, trackPx) }

            // Tapping the speaker mutes and restores, so the last audible level
            // survives a round trip through zero.
            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = IconCenterX - 18.dp)
                    .size(36.dp)
                    .clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null
                    ) {
                        KusinaSettings.setSfxVolume(
                            ctx,
                            if (volume > 0.01f) 0f else lastAudible
                        )
                    }
                    .semantics {
                        contentDescription =
                            if (volume > 0.01f) "Mute sound" else "Unmute sound"
                    }
            )

            val commit = { x: Float ->
                val usable = (trackPx - 2 * knobPx).coerceAtLeast(1f)
                val v = ((x - knobPx) / usable).coerceIn(0f, 1f)
                if (v > 0.01f) lastAudible = v
                KusinaSettings.setSfxVolume(ctx, v)
            }
            val chime = {
                if (KusinaSettings.prefs.value.sfxVolume > 0.01f) {
                    SoundFx.play(ctx, SoundFx.Cue.TileCorrect)
                }
            }

            Box(
                Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = TrackStartX)
                    .width(trackWidth)
                    .fillMaxHeight()
                    .pointerInput(trackPx) {
                        detectTapGestures { commit(it.x); chime() }
                    }
                    .pointerInput(trackPx) {
                        detectHorizontalDragGestures(
                            onDragStart = { commit(it.x) },
                            onDragEnd = { chime() }
                        ) { change, _ -> commit(change.position.x) }
                    }
            )

            Text(
                "${(volume * 100).roundToInt()}%",
                color = ValueInk,
                style = TextStyle(
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center,
                    platformStyle = PlatformTextStyle(includeFontPadding = false)
                ),
                modifier = Modifier
                    .align(Alignment.TopEnd)
                    .padding(top = CapsuleTop, end = ValueEndPad)
                    .width(ValueBoxWidth)
                    .height(CapsuleHeight)
                    .wrapContentHeight(Alignment.CenterVertically)
            )
        }
    }
}

/** Draws the capsule, groove, fill and knob for [volume] over a [trackPx] run. */
private fun DrawScope.drawCapsule(volume: Float, trackPx: Float) {
    val capTop = CapsuleTop.toPx()
    val capH = CapsuleHeight.toPx()
    val r = capH / 2f
    val w = size.width

    // A chunky bottom edge peeking out under the face gives the capsule its depth.
    drawRoundRect(
        color = EdgeClay,
        topLeft = Offset(0f, capTop + BaseLift.toPx()),
        size = Size(w, capH),
        cornerRadius = CornerRadius(r)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            0f to FaceTop, 0.6f to FaceMid, 1f to FaceBottom,
            startY = capTop, endY = capTop + capH
        ),
        topLeft = Offset(0f, capTop),
        size = Size(w, capH),
        cornerRadius = CornerRadius(r)
    )
    val edge = 1.5f.dp.toPx()
    drawRoundRect(
        color = FaceStroke,
        topLeft = Offset(edge, capTop + edge),
        size = Size(w - 2 * edge, capH - 2 * edge),
        cornerRadius = CornerRadius(r - edge),
        style = Stroke(width = 3.dp.toPx())
    )
    val stitch = 5.dp.toPx()
    drawRoundRect(
        color = StitchTan,
        topLeft = Offset(stitch, capTop + stitch),
        size = Size(w - 2 * stitch, capH - 2 * stitch),
        cornerRadius = CornerRadius(r - stitch),
        style = Stroke(
            width = 1.8f.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(6.dp.toPx(), 3.5f.dp.toPx())
            )
        )
    )

    val midY = capTop + capH / 2f
    drawSpeaker(IconCenterX.toPx(), midY, volume)

    // ---- Groove ----
    val gx = TrackStartX.toPx()
    val gh = GrooveHeight.toPx()
    val gr = gh / 2f
    drawRoundRect(
        color = GrooveFill,
        topLeft = Offset(gx, midY - gr),
        size = Size(trackPx, gh),
        cornerRadius = CornerRadius(gr)
    )
    drawRoundRect(
        brush = Brush.verticalGradient(
            listOf(GrooveShade.copy(alpha = 0.28f), Color.White.copy(alpha = 0.35f)),
            startY = midY - gr, endY = midY + gr
        ),
        topLeft = Offset(gx, midY - gr),
        size = Size(trackPx, gh),
        cornerRadius = CornerRadius(gr)
    )
    val gEdge = 1.dp.toPx()
    drawRoundRect(
        color = GrooveStroke,
        topLeft = Offset(gx + gEdge, midY - gr + gEdge),
        size = Size(trackPx - 2 * gEdge, gh - 2 * gEdge),
        cornerRadius = CornerRadius(gr - gEdge),
        style = Stroke(width = 2.dp.toPx())
    )

    // ---- Active fill, stopping under the knob ----
    val knobR = KnobRadius.toPx()
    val usable = (trackPx - 2 * knobR).coerceAtLeast(1f)
    val knobX = gx + knobR + usable * volume.coerceIn(0f, 1f)
    val inset = 2.dp.toPx()
    val fillW = (knobX - gx - inset).coerceAtLeast(0f)
    if (fillW > 0f) {
        val fh = gh - 2 * inset
        drawRoundRect(
            brush = Brush.horizontalGradient(
                0f to FillStart, 0.45f to FillMid, 1f to FillEnd,
                startX = gx, endX = gx + trackPx
            ),
            topLeft = Offset(gx + inset, midY - fh / 2f),
            size = Size(fillW, fh),
            cornerRadius = CornerRadius(fh / 2f)
        )
        val glossW = fillW - 2 * inset
        if (glossW > 0f) {
            drawRoundRect(
                color = Color.White.copy(alpha = 0.72f),
                topLeft = Offset(gx + 2 * inset, midY - fh / 2f + 1.dp.toPx()),
                size = Size(glossW, 2.5f.dp.toPx()),
                cornerRadius = CornerRadius(1.25f.dp.toPx())
            )
        }
    }

    // ---- Knob ----
    drawCircle(
        color = KnobDeepest.copy(alpha = 0.45f),
        radius = knobR + 1.2f.dp.toPx(),
        center = Offset(knobX, midY + 1.4f.dp.toPx())
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to KnobHot, 0.32f to FillMid, 0.82f to KnobDeep, 1f to KnobDeepest,
            center = Offset(knobX - knobR * 0.22f, midY - knobR * 0.32f),
            radius = knobR * 1.35f
        ),
        radius = knobR,
        center = Offset(knobX, midY)
    )
    drawCircle(
        color = KnobRim,
        radius = knobR - 1.3f.dp.toPx(),
        center = Offset(knobX, midY),
        style = Stroke(width = 2.6f.dp.toPx())
    )
    drawCircle(
        color = KnobBevel.copy(alpha = 0.8f),
        radius = 10.5f.dp.toPx(),
        center = Offset(knobX, midY),
        style = Stroke(width = 1.5f.dp.toPx())
    )
    drawCircle(color = KnobCore, radius = 5.dp.toPx(), center = Offset(knobX, midY))
    drawCircle(
        color = KnobCoreEdge,
        radius = 5.dp.toPx(),
        center = Offset(knobX, midY),
        style = Stroke(width = 1.dp.toPx())
    )
    drawCircle(
        color = Color.White.copy(alpha = 0.9f),
        radius = 1.5f.dp.toPx(),
        center = Offset(knobX - 2.dp.toPx(), midY - 2.dp.toPx())
    )
}

/** Cone plus wave arcs; the arcs drop away as the level falls, and mute gets a slash. */
private fun DrawScope.drawSpeaker(cx: Float, cy: Float, volume: Float) {
    translate(cx, cy) {
        val u = 1.dp.toPx()
        val body = Brush.verticalGradient(
            listOf(SpeakerTop, SpeakerBottom),
            startY = -9 * u, endY = 9 * u
        )
        val cone = Path().apply {
            moveTo(-5 * u, -4 * u)
            lineTo(-1 * u, -4 * u)
            lineTo(6 * u, -9 * u)
            lineTo(6 * u, 9 * u)
            lineTo(-1 * u, 4 * u)
            lineTo(-5 * u, 4 * u)
            close()
        }
        drawPath(cone, body)
        drawPath(cone, SpeakerInk, style = Stroke(width = 1.4f * u))
        drawRoundRect(
            brush = body,
            topLeft = Offset(-9 * u, -4 * u),
            size = Size(4 * u, 8 * u),
            cornerRadius = CornerRadius(1.5f * u)
        )
        drawRoundRect(
            color = SpeakerInk,
            topLeft = Offset(-9 * u, -4 * u),
            size = Size(4 * u, 8 * u),
            cornerRadius = CornerRadius(1.5f * u),
            style = Stroke(width = 1.2f * u)
        )

        if (volume > 0.01f) {
            drawArc(
                color = SpeakerInk,
                startAngle = -60f, sweepAngle = 120f, useCenter = false,
                topLeft = Offset(2 * u, -6 * u),
                size = Size(12 * u, 12 * u),
                style = Stroke(width = 2.2f * u, cap = StrokeCap.Round)
            )
        }
        if (volume >= 0.4f) {
            drawArc(
                color = SpeakerInk,
                startAngle = -60f, sweepAngle = 120f, useCenter = false,
                topLeft = Offset(4 * u, -9 * u),
                size = Size(18 * u, 18 * u),
                style = Stroke(width = 2.2f * u, cap = StrokeCap.Round)
            )
        }
        if (volume <= 0.01f) {
            drawLine(
                color = SpeakerInk,
                start = Offset(9 * u, -6 * u),
                end = Offset(17 * u, 6 * u),
                strokeWidth = 2.2f * u,
                cap = StrokeCap.Round
            )
        }
    }
}
