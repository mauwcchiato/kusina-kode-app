package com.example.kusinakode

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.requiredSize
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.Home
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.TravelExplore
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.SideEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Rect
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Outline
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Density
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlin.math.atan2
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sqrt

enum class BottomNavTab { Home, Profile, Levels, Wallet, Completed }

/** Bar background. */
private val NavBarBg = Color(0xFFFFFFFF)
/** The raised circle and the selected tab's label. */
private val ActiveInk = Color(0xFF9C3F0F)
/** The strip behind the bump, above the bar. */
private val BehindBarBg = Color(0x00F1E6D2)
/** Inactive icons and labels. */
private val InactiveInk = Color(0xFF9A8F8A)

/** Diameter of the raised circle. */
private val CircleSize = 52.dp
/** Gap between the circle and the bump around it. */
private val BumpGap = 6.dp
/** Radius of the soft curves where the bump meets the bar's top edge. */
private val BumpShoulder = 10.dp
/** How far the circle's centre sits above the bar's top edge. */
private val CircleAboveBar = -10.dp

private val IconSlot = 28.dp
private val RowTopPad = 12.dp
private val RowBottomPad = 8.dp
private val RowSidePad = 8.dp
/** Height of the bump above the bar's top edge. */
private val BumpRoom = CircleSize / 2 + BumpGap + CircleAboveBar
/** How far the selected icon rises to sit in the circle. */
private val Lift = RowTopPad + IconSlot / 2 + CircleAboveBar

/**
 * Bottom navigation: Wallet · Game Map · Home · Learn · Profile.
 *
 * A full-width white bar with a smooth bump. The selected tab's icon rises
 * into a filled burnt-orange circle inside the bump, and the bump and circle
 * glide together to whichever tab the player picks.
 *
 * The space beside the bump is beige, and screens leave room for it so the
 * bump never covers their buttons.
 */
@Composable
fun KusinaBottomNav(
    selected: BottomNavTab,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onLevels: () -> Unit,
    onWallet: () -> Unit,
    onCompleted: () -> Unit,
    walletModifier: Modifier = Modifier,
    levelsModifier: Modifier = Modifier,
    homeModifier: Modifier = Modifier,
    learnModifier: Modifier = Modifier,
    profileModifier: Modifier = Modifier,
) {
    // The tab the player came from. Every screen builds its own bar, so a new
    // bar would otherwise start with its circle already in place and the
    // switch never animated. Read once per bar, before this bar records itself.
    val cameFrom = remember { NavMemory.lastTab }
    SideEffect { NavMemory.lastTab = selected }
    val fromIndex = cameFrom?.let { TabOrder.indexOf(it) }?.takeIf { it >= 0 }
    val activeIndex = TabOrder.indexOf(selected)

    // Where the circle and bump are, in tabs (fractional while gliding).
    val circlePos = remember { Animatable((fromIndex ?: activeIndex).toFloat()) }
    LaunchedEffect(activeIndex) {
        circlePos.animateTo(activeIndex.toFloat(), SlideSpring)
    }

    // The bump's room above the bar is part of the bar's height, so screens
    // keep their content clear of it. It is painted beige ([BehindBarBg]).
    BoxWithConstraints(Modifier.fillMaxWidth()) {
        val density = LocalDensity.current
        val pitchPx = with(density) { ((maxWidth - RowSidePad * 2) / TabOrder.size).toPx() }
        val sidePx = with(density) { RowSidePad.toPx() }

        Box(
            Modifier
                .fillMaxWidth()
                // Beige behind the bump, the same cream as the Home background.
                .background(BehindBarBg)
        ) {
            Surface(
                color = NavBarBg,
                shadowElevation = 10.dp,
                shape = BumpedBarShape(
                    bumpX = sidePx + circlePos.value * pitchPx + pitchPx / 2
                ),
                modifier = Modifier.matchParentSize()
            ) {}

            Row(
                Modifier
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(
                        start = RowSidePad,
                        end = RowSidePad,
                        top = BumpRoom + RowTopPad,
                        bottom = RowBottomPad
                    ),
                verticalAlignment = Alignment.Bottom
            ) {
                TabOrder.forEachIndexed { index, tab ->
                    val (icon, label) = when (tab) {
                        BottomNavTab.Wallet -> Icons.Default.AccountBalanceWallet to "Wallet"
                        BottomNavTab.Levels -> Icons.Default.TravelExplore to "Game Map"
                        BottomNavTab.Home -> Icons.Default.Home to "Home"
                        BottomNavTab.Completed -> Icons.Default.School to "Learn"
                        BottomNavTab.Profile -> Icons.Default.Person to "Profile"
                    }
                    NavItem(
                        icon = icon,
                        label = label,
                        active = selected == tab,
                        wasActive = cameFrom == tab && cameFrom != selected,
                        circleShiftPx = { (circlePos.value - index) * pitchPx },
                        modifier = Modifier.weight(1f),
                        onClick = when (tab) {
                            BottomNavTab.Wallet -> onWallet
                            BottomNavTab.Levels -> onLevels
                            BottomNavTab.Home -> onHome
                            BottomNavTab.Completed -> onCompleted
                            BottomNavTab.Profile -> onProfile
                        },
                        highlightModifier = when (tab) {
                            BottomNavTab.Wallet -> walletModifier
                            BottomNavTab.Levels -> levelsModifier
                            BottomNavTab.Home -> homeModifier
                            BottomNavTab.Completed -> learnModifier
                            BottomNavTab.Profile -> profileModifier
                        }
                    )
                }
            }
        }
    }
}

/** Left-to-right order of the tabs, which the circle travels along. */
private val TabOrder = listOf(
    BottomNavTab.Wallet,
    BottomNavTab.Levels,
    BottomNavTab.Home,
    BottomNavTab.Completed,
    BottomNavTab.Profile
)

/** The last tab a bar was shown for, remembered across screens. */
private object NavMemory {
    var lastTab: BottomNavTab? = null
}

/** The circle's glide between tabs: a touch of overshoot, then settle. */
private val SlideSpring = spring<Float>(
    dampingRatio = 0.8f,
    stiffness = Spring.StiffnessMediumLow
)

/** The icon's rise and fall. */
private val LiftSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * The bar's outline: a full-width rectangle starting [BumpRoom] below the
 * top, with a round bump centred at [bumpX]. The bump joins the top edge
 * through two small concave curves so it reads as one smooth shape. Near the
 * screen edges the shoulders may run past the side; that part is off-screen.
 */
private data class BumpedBarShape(val bumpX: Float) : Shape {
    override fun createOutline(size: Size, layoutDirection: LayoutDirection, density: Density): Outline {
        with(density) {
            val top = BumpRoom.toPx()
            val bottom = size.height
            val w = size.width
            val cx = bumpX
            val cy = top - CircleAboveBar.toPx()
            val rb = (CircleSize / 2 + BumpGap).toPx()
            val rf = BumpShoulder.toPx()

            // Shoulder circles: tangent to the top edge and to the bump.
            val fy = top - rf
            val dy = cy - fy
            val dx = sqrt(((rb + rf) * (rb + rf) - dy * dy).coerceAtLeast(0f))
            val leftF = cx - dx
            val rightF = cx + dx
            val xL = min(0f, leftF)
            val xR = max(w, rightF)

            // Angle (deg) from the left shoulder's centre to the bump's centre.
            val a = Math.toDegrees(atan2(dy.toDouble(), dx.toDouble())).toFloat()
            // Angle on the bump where the left shoulder touches it (180..270).
            val b = 180f + a

            val path = Path().apply {
                moveTo(xL, bottom)
                lineTo(xL, top)
                lineTo(leftF, top)
                arcTo(Rect(leftF - rf, fy - rf, leftF + rf, fy + rf), 90f, a - 90f, false)
                arcTo(Rect(cx - rb, cy - rb, cx + rb, cy + rb), b, 540f - 2 * b, false)
                arcTo(Rect(rightF - rf, fy - rf, rightF + rf, fy + rf), 180f - a, a - 90f, false)
                lineTo(xR, top)
                lineTo(xR, bottom)
                close()
            }
            return Outline.Generic(path)
        }
    }
}

/**
 * One tab: icon over label. The active tab draws the orange circle, shifted
 * by [circleShiftPx] while it glides in from the previous tab; its icon turns
 * white and rises into it. The tab left behind ([wasActive]) settles back.
 */
@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    wasActive: Boolean,
    circleShiftPx: () -> Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    highlightModifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val raise = remember { Animatable(if (active || wasActive) 1f else 0f) }
    LaunchedEffect(active) {
        raise.animateTo(if (active) 1f else 0f, LiftSpring)
    }
    val iconTint by animateColorAsState(
        if (active) Color.White else InactiveInk,
        tween(220), label = "icon_$label"
    )
    val labelInk by animateColorAsState(
        if (active) ActiveInk else InactiveInk,
        tween(220), label = "label_$label"
    )
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier
            .clickable(interactionSource = interaction, indication = null) {
                SoundFx.play(ctx, SoundFx.Cue.Nav)
                onClick()
            }
            .then(highlightModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(Modifier.height(IconSlot), contentAlignment = Alignment.Center) {
            if (active) {
                Box(
                    Modifier
                        .requiredSize(CircleSize)
                        .graphicsLayer {
                            translationX = circleShiftPx()
                            translationY = -Lift.toPx()
                            shadowElevation = 8.dp.toPx()
                            shape = CircleShape
                            ambientShadowColor = ActiveInk
                            spotShadowColor = ActiveInk
                        }
                        .background(ActiveInk, CircleShape)
                )
            }
            Icon(
                icon,
                contentDescription = label,
                tint = iconTint,
                modifier = Modifier
                    .size(24.dp)
                    .graphicsLayer {
                        translationX = if (active) circleShiftPx() else 0f
                        translationY = -Lift.toPx() * raise.value
                    }
            )
        }
        Spacer(Modifier.height(4.dp))
        Text(
            label,
            color = labelInk,
            fontSize = 11.sp,
            fontWeight = if (active) FontWeight.SemiBold else FontWeight.Medium,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}
