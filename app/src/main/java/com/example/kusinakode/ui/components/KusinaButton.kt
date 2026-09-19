package com.example.kusinakode.ui.components

import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
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
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp

/** Which of the three carved pills a button wears. */
enum class KusinaButtonTone { Brown, Terracotta, Parchment }

// ---- Barako brown ----
private val BrownBase = Color(0xFF220B02)
private val BrownFaceTop = Color(0xFF7E411B)
private val BrownFaceUpper = Color(0xFF643011)
private val BrownFaceLower = Color(0xFF461E07)
private val BrownFaceFoot = Color(0xFF321303)
private val BrownEdge = Color(0xFF260C02)
private val BrownInlay = Color(0xFFD78E4B)

// ---- Narra terracotta (same candy as the pause Resume pill) ----
private val ClayBase = Color(0xFF3A1204)
private val ClayFaceTop = Color(0xFFE0893C)
private val ClayFaceUpper = Color(0xFFC85F1A)
private val ClayFaceLower = Color(0xFFA34410)
private val ClayFaceFoot = Color(0xFF6E2808)
private val ClayEdge = Color(0xFF4A1806)
private val ClayInlay = Color(0xFFFFD090)

// ---- Banig parchment ----
private val ParchBase = Color(0xFF8C5C38)
private val ParchFaceTop = Color(0xFFFFFDF8)
private val ParchFaceMid = Color(0xFFF6EEDB)
private val ParchFaceFoot = Color(0xFFE9DCBF)
private val ParchEdge = Color(0xFF6A3B18)
private val ParchSeam = Color(0xFFD1BE9B)
private val ParchDot = Color(0xFFA4724C)

// ---- Medallion ----
private val MedalRimDeep = Color(0xFF2E1102)
private val MedalRim = Color(0xFF522003)
private val MedalRimEdge = Color(0xFF2B0E01)
private val MedalHot = Color(0xFF8C4113)
private val MedalMid = Color(0xFF6E2F09)
private val MedalLow = Color(0xFF4F1E04)
private val MedalDeep = Color(0xFF361302)
private val MedalInner = Color(0xFFA85720)
private val MedalGlyph = Color(0xFFFFFDF5)

private val InkOnDark = Color(0xFFFFF6E6)
private val InkOnParch = Color(0xFF4A2412)

/**
 * The carved pill every dialog button wears.
 *
 * Three tones, all cut to the same artwork: a raised face over an extruded
 * base, a hairline inlay inside the border, and a gloss lip across the top.
 * [icon] adds the wooden medallion from the parchment design — it is optional
 * because most buttons carry only a label.
 *
 * Geometry follows the source artwork's 68-unit face on a 76-unit footprint,
 * so weights and insets hold at any height.
 */
@Composable
fun KusinaButton(
    label: String,
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    tone: KusinaButtonTone = KusinaButtonTone.Terracotta,
    enabled: Boolean = true,
    icon: ImageVector? = null,
    height: Dp = 58.dp,
    fontSize: androidx.compose.ui.unit.TextUnit = 15.sp,
    letterSpacing: androidx.compose.ui.unit.TextUnit = androidx.compose.ui.unit.TextUnit.Unspecified,
    /** Sits before the label, for callers with their own glyph rather than a medallion. */
    leading: (@Composable () -> Unit)? = null,
    /** Sits after the label — a coin glyph, a count, whatever the caller had. */
    trailing: (@Composable () -> Unit)? = null
) {
    val interaction = remember { MutableInteractionSource() }
    val pressed by interaction.collectIsPressedAsState()
    // Every one of these is a real action, so every one of them is audible.
    // Done here rather than at the call sites so no button can be missed.
    val click = clickSfx(onClick)
    // Pressing sinks the face toward its base, the way a real key would.
    val sink by animateFloatAsState(
        targetValue = if (pressed && enabled) 0.975f else 1f,
        animationSpec = spring(stiffness = Spring.StiffnessMediumLow),
        label = "kusinaButtonPress"
    )

    Box(
        modifier
            .fillMaxWidth()
            .height(height)
            .scale(sink)
            .drawBehind { drawPill(tone, enabled, icon != null) }
            .clickable(
                interactionSource = interaction,
                indication = null,
                enabled = enabled,
                onClick = click
            ),
        contentAlignment = Alignment.Center
    ) {
        // The face sits above the extruded base, so the label rides with it.
        val faceLift = height * (8f / 76f) / 2f
        Row(
            Modifier
                .fillMaxWidth()
                .padding(horizontal = if (icon != null) 68.dp else 24.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading != null) {
                Box(Modifier.padding(bottom = faceLift)) { leading() }
                Spacer(Modifier.width(10.dp))
            }
            Text(
                label,
                color = when {
                    !enabled -> if (tone == KusinaButtonTone.Parchment) {
                        InkOnParch.copy(alpha = 0.45f)
                    } else {
                        InkOnDark.copy(alpha = 0.55f)
                    }
                    tone == KusinaButtonTone.Parchment -> InkOnParch
                    else -> InkOnDark
                },
                fontWeight = FontWeight.ExtraBold,
                fontSize = fontSize,
                letterSpacing = letterSpacing,
                textAlign = TextAlign.Center,
                maxLines = 1,
                modifier = Modifier.padding(bottom = faceLift)
            )
            if (trailing != null) {
                Spacer(Modifier.width(6.dp))
                Box(Modifier.padding(bottom = faceLift)) { trailing() }
            }
        }
        if (icon != null) {
            Icon(
                icon,
                contentDescription = null,
                tint = MedalGlyph,
                modifier = Modifier
                    .align(Alignment.CenterStart)
                    .padding(start = height * (30f / 76f) - height * (11f / 76f))
                    .padding(bottom = faceLift * 2f)
                    .size(height * (22f / 76f))
            )
        }
    }
}

private fun DrawScope.drawPill(
    tone: KusinaButtonTone,
    enabled: Boolean,
    hasIcon: Boolean
) {
    // 76 artwork units tall: a 68-unit face sitting 8 units above its base.
    val u = size.height / 76f
    fun len(v: Float) = v * u

    val faceH = len(68f)
    val drop = len(5f)
    val w = size.width
    val r = faceH / 2f
    val dim = if (enabled) 1f else 0.55f

    val base = when (tone) {
        KusinaButtonTone.Brown -> BrownBase
        KusinaButtonTone.Terracotta -> ClayBase
        KusinaButtonTone.Parchment -> ParchBase
    }
    drawRoundRect(
        color = base.copy(alpha = dim),
        topLeft = Offset(0f, drop),
        size = Size(w, faceH),
        cornerRadius = CornerRadius(r)
    )

    val face = when (tone) {
        KusinaButtonTone.Brown -> Brush.verticalGradient(
            0f to BrownFaceTop, 0.15f to BrownFaceUpper,
            0.82f to BrownFaceLower, 1f to BrownFaceFoot,
            startY = 0f, endY = faceH
        )
        KusinaButtonTone.Terracotta -> Brush.verticalGradient(
            0f to ClayFaceTop, 0.14f to ClayFaceUpper,
            0.85f to ClayFaceLower, 1f to ClayFaceFoot,
            startY = 0f, endY = faceH
        )
        KusinaButtonTone.Parchment -> Brush.verticalGradient(
            0f to ParchFaceTop, 0.6f to ParchFaceMid, 1f to ParchFaceFoot,
            startY = 0f, endY = faceH
        )
    }
    drawRoundRect(
        brush = face,
        topLeft = Offset.Zero,
        size = Size(w, faceH),
        cornerRadius = CornerRadius(r),
        alpha = dim
    )

    val edge = len(3.5f)
    drawRoundRect(
        color = when (tone) {
            KusinaButtonTone.Brown -> BrownEdge
            KusinaButtonTone.Terracotta -> ClayEdge
            KusinaButtonTone.Parchment -> ParchEdge
        }.copy(alpha = dim),
        topLeft = Offset(edge / 2f, edge / 2f),
        size = Size(w - edge, faceH - edge),
        cornerRadius = CornerRadius(r - edge / 2f),
        style = Stroke(width = edge)
    )

    // Inner inlay: a solid hairline on the wood tones, stitched on parchment.
    val inset = len(6f)
    val inlayR = r - inset
    if (tone == KusinaButtonTone.Parchment) {
        drawRoundRect(
            color = ParchSeam.copy(alpha = dim),
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2f, faceH - inset * 2f),
            cornerRadius = CornerRadius(inlayR),
            style = Stroke(
                width = len(2f),
                pathEffect = PathEffect.dashPathEffect(floatArrayOf(len(7f), len(4f)))
            )
        )
    } else {
        val inlay = if (tone == KusinaButtonTone.Brown) BrownInlay else ClayInlay
        drawRoundRect(
            color = inlay.copy(alpha = (if (tone == KusinaButtonTone.Brown) 0.55f else 0.6f) * dim),
            topLeft = Offset(inset, inset),
            size = Size(w - inset * 2f, faceH - inset * 2f),
            cornerRadius = CornerRadius(inlayR),
            style = Stroke(width = len(1.8f))
        )
    }

    // Gloss lip across the top of the face.
    val lipX = len(28f)
    val lipW = w - lipX * 2f
    if (lipW > 0f) {
        val gloss = when (tone) {
            KusinaButtonTone.Brown -> Brush.horizontalGradient(
                0f to Color(0xFFFFF0DB).copy(alpha = 0.12f),
                0.35f to Color(0xFFFFE7C4).copy(alpha = 0.45f),
                1f to Color(0xFFFFF0DB).copy(alpha = 0.08f),
                startX = lipX, endX = lipX + lipW
            )
            KusinaButtonTone.Terracotta -> Brush.horizontalGradient(
                0f to Color(0xFFFFF0D4).copy(alpha = 0.15f),
                0.35f to Color(0xFFFFE4B8).copy(alpha = 0.55f),
                1f to Color(0xFFFFF0D4).copy(alpha = 0.1f),
                startX = lipX, endX = lipX + lipW
            )
            KusinaButtonTone.Parchment -> Brush.horizontalGradient(
                0f to Color.White.copy(alpha = 0.8f * 0.65f),
                1f to Color.White.copy(alpha = 0.2f * 0.65f),
                startX = lipX, endX = lipX + lipW
            )
        }
        drawRoundRect(
            brush = gloss,
            topLeft = Offset(lipX, len(6f)),
            size = Size(lipW, len(14f)),
            cornerRadius = CornerRadius(len(7f)),
            alpha = dim
        )
    }

    if (!hasIcon) return

    // ---- Wooden medallion, centred on the face ----
    val mc = Offset(len(50f), faceH / 2f)
    val mr = len(23f)
    drawCircle(MedalRimDeep.copy(alpha = dim), radius = mr, center = Offset(mc.x, mc.y + len(2.5f)))
    drawCircle(MedalRim.copy(alpha = dim), radius = mr, center = mc)
    drawCircle(
        color = MedalRimEdge.copy(alpha = dim),
        radius = mr,
        center = mc,
        style = Stroke(width = len(1.8f))
    )
    drawCircle(
        brush = Brush.radialGradient(
            0f to MedalHot, 0.45f to MedalMid, 0.85f to MedalLow, 1f to MedalDeep,
            center = Offset(mc.x - mr * 0.16f, mc.y - mr * 0.28f),
            radius = mr * 1.4f
        ),
        radius = len(21f),
        center = mc,
        alpha = dim
    )
    drawCircle(
        color = MedalInner.copy(alpha = 0.7f * dim),
        radius = len(19f),
        center = mc,
        style = Stroke(width = len(1f))
    )

    // Right-hand motif dot, the counterweight to the medallion.
    drawCircle(
        color = ParchDot.copy(alpha = 0.6f * dim),
        radius = len(4f),
        center = Offset(w - len(35f), faceH / 2f)
    )
}
