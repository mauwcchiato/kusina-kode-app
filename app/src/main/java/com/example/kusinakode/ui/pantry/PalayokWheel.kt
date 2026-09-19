package com.example.kusinakode.ui.pantry

import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.toArgb
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.nativeCanvas
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.ui.theme.BeVietnamPro
import kotlin.math.cos
import kotlin.math.sin

/**
 * The prizes drawn on the wheel, in the order they sit around it.
 *
 * Two slices per prize, alternating, so no two neighbours match and the
 * wheel reads as a wheel rather than three fat wedges. The server decides
 * what was won — this list only says where to stop.
 */
private val WheelSlices = listOf(3, 5, 8, 3, 5, 8)

private val WheelInk = Color(0xFF3E2723)
private val SliceWarm = Color(0xFFF3E2C2)
private val SliceDeep = Color(0xFFCC6B1F)
private val WheelRim = Color(0xFF6B3A1F)
private val WheelGold = Color(0xFFE8C36A)

// ---- Narra hardwood frame, palayok-clay slices, brass fittings ----
private val RimDark = Color(0xFF5A2405)
private val RimMid = Color(0xFF3A1502)
private val RimDeep = Color(0xFF240A01)
private val RimEdge = Color(0xFF1B0701)
private val BaseShadow = Color(0xFF200B02)
private val GrooveTan = Color(0xFF7A380F)
private val SliceStroke = Color(0xFF4A1E04)
private val SpokeBrown = Color(0xFF481B03)
private val ClayLight = Color(0xFFF28834)
private val ClayMid = Color(0xFFD96618)
private val ClayDeep = Color(0xFFAC4808)
private val ParchTop = Color(0xFFFFFDF7)
private val ParchMid = Color(0xFFF8EEDC)
private val ParchLow = Color(0xFFE9D8B6)
private val BrassHot = Color(0xFFFFF4C7)
private val BrassMid = Color(0xFFE5AB3A)
private val BrassLow = Color(0xFF915910)
private val BrassDeep = Color(0xFF543005)
private val NeedleTop = Color(0xFFFFF2B0)
private val NeedleMid = Color(0xFFF3B328)
private val NeedleLow = Color(0xFFA65F04)
private val NeedleEdge = Color(0xFF4E2702)
private val HubRing = Color(0xFF461B04)
private val HubEdge = Color(0xFF260C01)
private val HubDiscHot = Color(0xFF7A340C)
private val HubDiscMid = Color(0xFF592204)
private val HubDiscLow = Color(0xFF3D1502)
private val HubDiscDeep = Color(0xFF280C01)
private val HubDash = Color(0xFFB85D24)
private val CarveLight = Color(0xFFE4CCA6)
private val CarveDark = Color(0xFF481B03)
private val NumberInk = Color(0xFF3B1804)
private val NumberCream = Color(0xFFFFFDF8)

/** Wheel colours plus a warm white — the paper matches the prize board. */
private val WheelConfetti = listOf(
    Color(0xFFE8C36A),
    Color(0xFFCC6B1F),
    Color(0xFFF3E2C2),
    Color(0xFFFFD24A),
    Color(0xFFFFFFFF)
)

/**
 * Daily-login wheel. [won] is the prize the server already rolled: null while
 * the player has not spun yet, set the moment the answer comes back.
 *
 * The spin is choreography, not a lottery — the wheel is animated to land on
 * the slice that matches [won], so what the player watches and what the
 * server recorded can never disagree.
 */
@Composable
internal fun PalayokWheelOverlay(
    spinsAvailable: Int,
    spinning: Boolean,
    won: Int?,
    onSpin: () -> Unit,
    onDone: () -> Unit
) {
    HoldsTheScreen()
    val ctx = LocalContext.current
    val turn = remember { Animatable(0f) }
    // The server answers long before the wheel finishes turning, so the result
    // is gated on the animation landing rather than on the prize arriving —
    // otherwise the wheel announces the win while it is still spinning.
    var settled by remember(won) { mutableStateOf(false) }

    LaunchedEffect(won) {
        val prize = won ?: return@LaunchedEffect
        // Land the pointer in the middle of a slice paying this prize.
        val slice = WheelSlices.indexOf(prize).takeIf { it >= 0 } ?: 0
        val per = 360f / WheelSlices.size
        val target = 360f * 5 - (slice * per + per / 2f)
        turn.snapTo(0f)
        turn.animateTo(target, tween(2600, easing = FastOutSlowInEasing))
        settled = true
        SoundFx.play(ctx, SoundFx.Cue.Badge)
        SoundFx.vibrate(ctx, 30)
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(Color(0xF50A0604))
            // Once it has landed the whole sheet is the dismiss target; a
            // button would only be a second place to tap for the same thing.
            .clickable(
                enabled = settled,
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
                onClick = onDone
            ),
        contentAlignment = Alignment.Center
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .padding(24.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Text(
                if (settled) "THE WHEEL STOPS" else "DAILY SPIN",
                color = WheelGold,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 22.sp,
                letterSpacing = 1.6.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(6.dp))
            Text(
                when {
                    settled && won != null -> "You won $won palayoks. They are on your shelf."
                    spinning || won != null -> "Round and round…"
                    spinsAvailable > 1 -> "$spinsAvailable spins banked. Each one pays 3, 5 or 8 palayoks."
                    else -> "One free spin from today's login. It pays 3, 5 or 8 palayoks."
                },
                color = Color(0xFFE8C9A0),
                fontFamily = BeVietnamPro,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier.fillMaxWidth()
            )
            Spacer(Modifier.height(20.dp))

            BoxWithConstraints(
                Modifier
                    .fillMaxWidth(0.86f)
                    .aspectRatio(1f),
                contentAlignment = Alignment.Center
            ) {
                // The artwork's own 700-unit board, shrunk a touch so the
                // needle and the extruded edge both stay inside the square.
                val boardSide = maxWidth
                Canvas(Modifier.fillMaxSize()) {
                    // The artwork reserves 90 units of shadow margin we do not
                    // need. Its real extent is the needle pivot at the top
                    // (273 units up) and the extruded edge below (270 down), so
                    // fitting to 560 rather than 700 fills the square properly.
                    val unit = (size.minDimension / 560f) * 0.98f
                    val board = WheelBoard(
                        cx = size.width / 2f,
                        cy = size.height / 2f + unit * 1.5f,
                        unit = unit
                    )
                    drawWheelFrame(board)
                    // Only the face turns; the frame, hub and needle are the
                    // cabinet the wheel spins inside.
                    rotate(turn.value) { drawWheelFace(board) }
                    drawWheelFittings(board)
                }
                // The palayok rides the hub the canvas just drew.
                Image(
                    painter = painterResource(R.drawable.baul_closed),
                    contentDescription = null,
                    modifier = Modifier
                        .size(boardSide * 0.15f)
                        .offset(y = boardSide * 0.003f)
                )
            }

            Spacer(Modifier.height(24.dp))
            when {
                // Landed: no control at all — the sheet itself is the target.
                settled -> Text(
                    "TAP ANYWHERE TO COLLECT",
                    color = WheelGold.copy(alpha = 0.85f),
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.8.sp
                )

                spinning || won != null -> Text(
                    "SPINNING…",
                    color = Color(0xFFD4B48A),
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp,
                    letterSpacing = 1.8.sp
                )

                else -> Surface(
                    shape = RoundedCornerShape(50),
                    color = SliceDeep,
                    shadowElevation = 6.dp,
                    modifier = Modifier.clickable(onClick = onSpin)
                ) {
                    Text(
                        "SPIN",
                        color = Color.White,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 14.sp,
                        letterSpacing = 1.4.sp,
                        modifier = Modifier.padding(horizontal = 40.dp, vertical = 12.dp)
                    )
                }
            }
            if (!settled && !spinning && won == null) {
                Spacer(Modifier.height(12.dp))
                Text(
                    "Not now",
                    color = Color(0xFFD4B48A),
                    fontFamily = BeVietnamPro,
                    fontSize = 11.sp,
                    modifier = Modifier
                        .clickable(onClick = onDone)
                        .padding(8.dp)
                )
            }
        }

        // Paper only once the wheel has actually stopped, and more of it for a
        // bigger prize so an 8 feels different from a 3.
        if (settled && won != null) {
            ConfettiBurst(
                colors = WheelConfetti,
                count = when (won) {
                    8 -> 64
                    5 -> 44
                    else -> 30
                },
                modifier = Modifier.fillMaxSize()
            )
        }
    }
}

/**
 * Geometry is carried straight over from the wheel artwork, which is drawn on
 * a 700×700 board with the hub at (350, 350) and a 260-unit outer rim. Keeping
 * those numbers and scaling by [unit] means the proportions survive at any
 * size, and any future tweak to the artwork maps across without re-deriving.
 */
private class WheelBoard(val cx: Float, val cy: Float, val unit: Float) {
    /** An artwork length in real pixels. */
    fun len(v: Float) = v * unit

    /** An artwork point in real pixels. */
    fun at(x: Float, y: Float) = Offset(cx + (x - 350f) * unit, cy + (y - 350f) * unit)
}

/** The fixed frame: extruded edge, narra rim, carved groove, brass studs. */
private fun DrawScope.drawWheelFrame(b: WheelBoard) {
    // A second disc nudged down under the rim reads as thickness rather than
    // as a drop shadow, which is what a wooden wheel actually has.
    drawCircle(BaseShadow, radius = b.len(260f), center = b.at(350f, 360f))

    drawCircle(
        brush = Brush.linearGradient(
            listOf(RimDark, RimMid, RimDeep),
            start = b.at(90f, 90f),
            end = b.at(610f, 610f)
        ),
        radius = b.len(260f),
        center = b.at(350f, 350f)
    )
    drawCircle(
        color = RimEdge,
        radius = b.len(260f),
        center = b.at(350f, 350f),
        style = Stroke(width = b.len(4.5f))
    )

    // Carved banig stitching around the inner lip.
    drawCircle(
        color = GrooveTan.copy(alpha = 0.85f),
        radius = b.len(244f),
        center = b.at(350f, 350f),
        style = Stroke(
            width = b.len(2.5f),
            pathEffect = PathEffect.dashPathEffect(
                floatArrayOf(b.len(9f), b.len(5f))
            )
        )
    )

    // Twelve brass studs, one every 30°.
    repeat(12) { i ->
        val a = Math.toRadians((i * 30f).toDouble())
        val sx = b.cx + (b.len(252f) * sin(a)).toFloat()
        val sy = b.cy - (b.len(252f) * cos(a)).toFloat()
        val r = b.len(6f)
        drawCircle(
            brush = Brush.radialGradient(
                0f to BrassHot, 0.4f to BrassMid, 0.85f to BrassLow, 1f to BrassDeep,
                center = Offset(sx - r * 0.3f, sy - r * 0.4f),
                radius = r * 1.4f
            ),
            radius = r,
            center = Offset(sx, sy)
        )
    }
}

/** The turning face: clay and parchment wedges, spokes, carved numerals. */
private fun DrawScope.drawWheelFace(b: WheelBoard) {
    val per = 360f / WheelSlices.size
    val faceR = b.len(230f)
    val centre = b.at(350f, 350f)
    val box = Offset(centre.x - faceR, centre.y - faceR)
    val span = Size(faceR * 2f, faceR * 2f)

    WheelSlices.forEachIndexed { i, _ ->
        val parchment = i % 2 == 0
        drawArc(
            brush = if (parchment) {
                Brush.linearGradient(
                    listOf(ParchTop, ParchMid, ParchLow),
                    start = box,
                    end = Offset(box.x + span.width, box.y + span.height)
                )
            } else {
                Brush.linearGradient(
                    listOf(ClayLight, ClayMid, ClayDeep),
                    start = box,
                    end = Offset(box.x + span.width, box.y + span.height)
                )
            },
            startAngle = i * per - 90f,
            sweepAngle = per,
            useCenter = true,
            topLeft = box,
            size = span
        )
        drawArc(
            color = SliceStroke,
            startAngle = i * per - 90f,
            sweepAngle = per,
            useCenter = true,
            topLeft = box,
            size = span,
            style = Stroke(width = b.len(2.5f))
        )
    }

    // Spokes along every wedge boundary.
    WheelSlices.indices.forEach { i ->
        val a = Math.toRadians((i * per).toDouble())
        drawLine(
            color = SpokeBrown,
            start = centre,
            end = Offset(
                centre.x + (faceR * sin(a)).toFloat(),
                centre.y - (faceR * cos(a)).toFloat()
            ),
            strokeWidth = b.len(3.5f)
        )
    }

    // Numerals ride their own wedge, so each is rotated to face outward. Drawn
    // twice: an offset copy behind gives the carved-into-wood edge.
    drawContext.canvas.nativeCanvas.apply {
        val paint = android.graphics.Paint().apply {
            isAntiAlias = true
            textAlign = android.graphics.Paint.Align.CENTER
            textSize = b.len(56f)
            typeface = android.graphics.Typeface.create(
                android.graphics.Typeface.DEFAULT, android.graphics.Typeface.BOLD
            )
        }
        val baseline = paint.textSize * 0.35f
        WheelSlices.forEachIndexed { i, prize ->
            val parchment = i % 2 == 0
            val y = centre.y - b.len(150f) + baseline
            save()
            rotate(i * per + per / 2f, centre.x, centre.y)
            paint.color = (if (parchment) CarveLight else CarveDark).toArgb()
            drawText("$prize", centre.x, y + b.len(2.5f), paint)
            paint.color = (if (parchment) NumberInk else NumberCream).toArgb()
            drawText("$prize", centre.x, y, paint)
            restore()
        }
    }
}

/** Hub rings and the gold needle — both sit still while the face turns. */
private fun DrawScope.drawWheelFittings(b: WheelBoard) {
    val centre = b.at(350f, 350f)

    drawCircle(BaseShadow, radius = b.len(56f), center = b.at(350f, 355f))
    drawCircle(HubRing, radius = b.len(56f), center = centre)
    drawCircle(
        color = HubEdge,
        radius = b.len(56f),
        center = centre,
        style = Stroke(width = b.len(3f))
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to HubDiscHot, 0.5f to HubDiscMid, 0.85f to HubDiscLow, 1f to HubDiscDeep,
            center = b.at(330f, 333f),
            radius = b.len(70f)
        ),
        radius = b.len(50f),
        center = centre
    )
    drawCircle(
        color = HubDash.copy(alpha = 0.6f),
        radius = b.len(46f),
        center = centre,
        style = Stroke(
            width = b.len(1.5f),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(b.len(6f), b.len(3f)))
        )
    )

    // ---- Needle, hung above 12 o'clock and pointing down into the rim ----
    val pivot = b.at(350f, 92f)
    drawCircle(RimMid, radius = b.len(15f), center = pivot)
    drawCircle(
        color = RimEdge,
        radius = b.len(15f),
        center = pivot,
        style = Stroke(width = b.len(2.5f))
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to BrassHot, 0.4f to BrassMid, 0.85f to BrassLow, 1f to BrassDeep,
            center = b.at(347f, 89f),
            radius = b.len(11f)
        ),
        radius = b.len(8f),
        center = pivot
    )

    fun needle(tip: Float, half: Float, back: Float) = Path().apply {
        val t = b.at(350f, 114f + tip)
        val l = b.at(350f - half, 114f + back)
        val r = b.at(350f + half, 114f + back)
        moveTo(t.x, t.y); lineTo(l.x, l.y); lineTo(r.x, r.y); close()
    }

    drawPath(needle(32f, 22f, -14f), Color(0xFF522402))
    val head = needle(28f, 20f, -16f)
    drawPath(
        head,
        Brush.linearGradient(
            listOf(NeedleTop, NeedleMid, NeedleLow),
            start = b.at(350f, 98f),
            end = b.at(350f, 142f)
        )
    )
    drawPath(head, NeedleEdge, style = Stroke(width = b.len(2f)))

    // Left facet catches the light; the crease sells the fold.
    val facet = Path().apply {
        val t = b.at(350f, 142f)
        val l = b.at(330f, 98f)
        val c = b.at(350f, 98f)
        moveTo(t.x, t.y); lineTo(l.x, l.y); lineTo(c.x, c.y); close()
    }
    drawPath(facet, Color.White.copy(alpha = 0.32f))
    drawLine(
        color = Color(0xFF7A3D02),
        start = b.at(350f, 98f),
        end = b.at(350f, 142f),
        strokeWidth = b.len(1.6f)
    )
    drawCircle(Color(0xFFFFFCE8), radius = b.len(2.2f), center = b.at(350f, 134f))
}

