package com.example.kusinakode.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.ui.theme.BeVietnamPro

/** Resume / Keep Playing vs the cream secondary actions. */
enum class PauseMenuTone { Primary, Secondary }

private val ClayBase = Color(0xFF3A1204)
private val ClayFaceTop = Color(0xFFE0893C)
private val ClayFaceUpper = Color(0xFFC85F1A)
private val ClayFaceLower = Color(0xFFA34410)
private val ClayFaceFoot = Color(0xFF6E2808)
private val ClayEdge = Color(0xFF4A1806)
private val ClayInlay = Color(0xFFFFD090)

private val ParchBase = Color(0xFF5A3014)
private val ParchFaceTop = Color(0xFFFFFEFA)
private val ParchFaceMid = Color(0xFFF8F0DE)
private val ParchFaceFoot = Color(0xFFE4D2B0)
private val ParchEdge = Color(0xFF5A3014)
private val ParchSeam = Color(0xFFC4AE86)

private val InkOnDark = Color(0xFFFFF8EC)
private val InkOnParch = Color(0xFF4A2208)
private val GlyphGold = Color(0xFFFFD24A)
private val GlyphBrown = Color(0xFF5A2A0A)

private val FaceRatio = 68f / 76f
private val WellSize = 40.dp
private val WellStart = 7.dp

/**
 * Pause / leave HUD pill. The label sits dead-center on the carved face;
 * the left well is a recessed game token, not a PNG motif.
 */
@Composable
fun PauseMenuButton(
    label: String,
    icon: ImageVector? = null,
    modifier: Modifier = Modifier,
    tone: PauseMenuTone = PauseMenuTone.Secondary,
    compact: Boolean = false,
    onClick: () -> Unit
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    val click = clickSfx(onClick)
    val sink by animateFloatAsState(
        targetValue = if (pressed) 0.97f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "pauseMenuPress"
    )
    val height = if (compact) 44.dp else 58.dp
    val primary = tone == PauseMenuTone.Primary
    val showWell = icon != null && !compact

    Box(
        modifier
            .then(if (compact) Modifier.width(128.dp) else Modifier.fillMaxWidth())
            .height(height)
            .scale(sink)
            .drawBehind { drawPausePill(tone, showWell) }
            .clickable(
                interactionSource = interaction,
                indication = null,
                onClick = click
            )
            .semantics { contentDescription = label }
    ) {
        Row(
            Modifier
                .align(Alignment.TopCenter)
                .fillMaxWidth()
                .fillMaxHeight(FaceRatio)
                .padding(horizontal = if (showWell) WellStart else 16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null && !compact) {
                Box(
                    Modifier.size(WellSize),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        icon,
                        contentDescription = null,
                        tint = if (primary) GlyphGold else GlyphBrown,
                        modifier = Modifier.size(if (primary) 22.dp else 20.dp)
                    )
                }
            }
            Text(
                label,
                color = if (primary) InkOnDark else InkOnParch,
                maxLines = 1,
                style = TextStyle(
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = if (compact) 15.sp else 16.sp,
                    letterSpacing = 0.15.sp,
                    textAlign = TextAlign.Center,
                    platformStyle = PlatformTextStyle(includeFontPadding = false),
                    shadow = Shadow(
                        color = if (primary) Color(0x99000000) else Color(0x33FFFFFF),
                        offset = Offset(0f, if (primary) 1.6f else 0.8f),
                        blurRadius = if (primary) 2.5f else 0f
                    )
                ),
                modifier = Modifier.weight(1f)
            )
            if (showWell) {
                Spacer(Modifier.size(WellSize))
            }
        }
    }
}

private fun DrawScope.drawPausePill(tone: PauseMenuTone, showWell: Boolean) {
    val faceH = size.height * FaceRatio
    val drop = size.height - faceH
    val w = size.width
    val r = faceH / 2f
    val primary = tone == PauseMenuTone.Primary

    drawRoundRect(
        color = if (primary) ClayBase else ParchBase,
        topLeft = Offset(0f, drop),
        size = Size(w, faceH),
        cornerRadius = CornerRadius(r)
    )

    val face = if (primary) {
        Brush.verticalGradient(
            0f to ClayFaceTop, 0.18f to ClayFaceUpper,
            0.78f to ClayFaceLower, 1f to ClayFaceFoot,
            startY = 0f, endY = faceH
        )
    } else {
        Brush.verticalGradient(
            0f to ParchFaceTop, 0.5f to ParchFaceMid, 1f to ParchFaceFoot,
            startY = 0f, endY = faceH
        )
    }
    drawRoundRect(
        brush = face,
        topLeft = Offset.Zero,
        size = Size(w, faceH),
        cornerRadius = CornerRadius(r)
    )

    val edge = 3.2.dp.toPx()
    drawRoundRect(
        color = if (primary) ClayEdge else ParchEdge,
        topLeft = Offset(edge / 2f, edge / 2f),
        size = Size(w - edge, faceH - edge),
        cornerRadius = CornerRadius(r - edge / 2f),
        style = Stroke(width = edge)
    )

    val inset = 6.5.dp.toPx()
    if (primary) {
        drawRoundRect(
            color = ClayInlay.copy(alpha = 0.4f),
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2f, faceH - inset * 2f),
            cornerRadius = CornerRadius(r - inset),
            style = Stroke(width = 1.6.dp.toPx())
        )
    } else {
        drawRoundRect(
            color = ParchSeam,
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2f, faceH - inset * 2f),
            cornerRadius = CornerRadius(r - inset),
            style = Stroke(
                width = 2.dp.toPx(),
                pathEffect = PathEffect.dashPathEffect(
                    floatArrayOf(7.dp.toPx(), 4.dp.toPx())
                )
            )
        )
    }

    val lipX = 28.dp.toPx()
    val lipW = w - lipX * 2f
    if (lipW > 0f) {
        drawRoundRect(
            brush = if (primary) {
                Brush.horizontalGradient(
                    0f to Color(0xFFFFF0D4).copy(alpha = 0.08f),
                    0.45f to Color(0xFFFFE8C0).copy(alpha = 0.55f),
                    1f to Color(0xFFFFF0D4).copy(alpha = 0.06f),
                    startX = lipX, endX = lipX + lipW
                )
            } else {
                Brush.horizontalGradient(
                    0f to Color.White.copy(alpha = 0.7f),
                    1f to Color.White.copy(alpha = 0.08f),
                    startX = lipX, endX = lipX + lipW
                )
            },
            topLeft = Offset(lipX, 5.dp.toPx()),
            size = Size(lipW, 12.dp.toPx()),
            cornerRadius = CornerRadius(6.dp.toPx())
        )
    }

    if (showWell) {
        val wellR = WellSize.toPx() / 2f
        val wellC = Offset(WellStart.toPx() + wellR, faceH / 2f)
        drawIconWell(wellC, wellR, primary)
    }
}

private fun DrawScope.drawIconWell(center: Offset, radius: Float, primary: Boolean) {
    // Drop under the token, then a rim, then a recessed disc — a HUD coin.
    drawCircle(
        color = if (primary) Color(0xFF1A0802) else Color(0xFF8A6A40),
        radius = radius + 1.8.dp.toPx(),
        center = Offset(center.x, center.y + 2.dp.toPx())
    )
    if (primary) {
        drawCircle(
            color = Color(0xFF6A2E0A),
            radius = radius + 1.2.dp.toPx(),
            center = center
        )
        drawCircle(
            brush = Brush.radialGradient(
                0f to Color(0xFF7A320C),
                0.55f to Color(0xFF4A1A06),
                1f to Color(0xFF240C02),
                center = Offset(center.x - radius * 0.2f, center.y - radius * 0.28f),
                radius = radius * 1.35f
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            color = Color(0xFFFFC44A),
            radius = radius - 0.6.dp.toPx(),
            center = center,
            style = Stroke(width = 2.4.dp.toPx())
        )
        drawCircle(
            color = Color(0xFFFFF0C8).copy(alpha = 0.45f),
            radius = radius - 4.dp.toPx(),
            center = Offset(center.x, center.y - 1.dp.toPx()),
            style = Stroke(width = 1.6.dp.toPx(), cap = StrokeCap.Round)
        )
    } else {
        drawCircle(
            color = Color(0xFF6A3B18),
            radius = radius + 1.1.dp.toPx(),
            center = center
        )
        drawCircle(
            brush = Brush.verticalGradient(
                0f to Color(0xFFFFFEFA),
                0.55f to Color(0xFFF7EBD4),
                1f to Color(0xFFE8D4B0),
                startY = center.y - radius,
                endY = center.y + radius
            ),
            radius = radius,
            center = center
        )
        drawCircle(
            color = Color(0xFF5A3014),
            radius = radius - 0.4.dp.toPx(),
            center = center,
            style = Stroke(width = 2.6.dp.toPx())
        )
        drawCircle(
            color = Color.White.copy(alpha = 0.75f),
            radius = radius - 4.5.dp.toPx(),
            center = Offset(center.x, center.y - 1.2.dp.toPx()),
            style = Stroke(width = 1.8.dp.toPx(), cap = StrokeCap.Round)
        )
    }
}
