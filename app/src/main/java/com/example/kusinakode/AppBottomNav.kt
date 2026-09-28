package com.example.kusinakode

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.ui.theme.CardSurface
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange

enum class BottomNavTab { Home, Profile, Levels, Wallet, Completed }

private val Burnt = Color(0xFFB4510E)

/** Transparent room above the bar for the raised circle to rise into. */
private val RaiseRoom = 22.dp
private val SlotHeight = 46.dp
private val CircleSize = 46.dp

// One spring for everything that moves, so the bar animates as a single
// object rather than five things easing at their own rates.
private val NavSpring = spring<Float>(
    dampingRatio = Spring.DampingRatioLowBouncy,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * Bottom navigation: Wallet · Explore · Home · Learn · Profile.
 *
 * The selected tab lifts out of the bar as a filled circle and its label goes
 * bold; everything else stays a plain glyph. One signal, and it travels — an
 * earlier version gave the active tab a pill *and* an underline while Home
 * kept a permanent circle, which put two brown shapes side by side and read
 * as clutter.
 *
 * The circle has to draw outside the bar, so the bar Surface and the row of
 * items are siblings in a Box rather than parent and child: a Surface clips to
 * its shape and would cut the circle off at the top edge.
 *
 * Ranks is deliberately absent: the leaderboard is reached from Profile, so
 * the bar carries the wallet instead — KK is spent far more often than the
 * standings are checked.
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
    Column(Modifier.fillMaxWidth()) {
        // Nothing is painted here; it is the headroom the lifted circle needs.
        Spacer(Modifier.height(RaiseRoom))

        Box(Modifier.fillMaxWidth()) {
            Surface(
                color = CardSurface,
                shadowElevation = 16.dp,
                shape = RoundedCornerShape(topStart = 22.dp, topEnd = 22.dp),
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Column {
                    // Warm top edge, fading out at both ends.
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .height(2.dp)
                            .background(
                                Brush.horizontalGradient(
                                    listOf(
                                        Burnt.copy(alpha = 0f),
                                        Burnt.copy(alpha = 0.5f),
                                        Burnt.copy(alpha = 0.7f),
                                        Burnt.copy(alpha = 0.5f),
                                        Burnt.copy(alpha = 0f)
                                    )
                                )
                            )
                    )
                    // Reserves the bar's height; the real items sit on top of
                    // it as a sibling so the raised circle is not clipped.
                    Spacer(
                        Modifier
                            .fillMaxWidth()
                            .navigationBarsPadding()
                            .height(SlotHeight + 24.dp)
                    )
                }
            }


            // The tab the player came from. Every screen builds its own bar, so
            // a new bar would otherwise start with its circle already in place
            // and the switch never animated. Read once per bar, before this
            // bar records itself as the last one.
            val cameFrom = remember { NavMemory.lastTab }
            SideEffect { NavMemory.lastTab = selected }
            val fromIndex = cameFrom?.let { TabOrder.indexOf(it) }?.takeIf { it >= 0 }

            BoxWithConstraints(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                // Distance between two tabs' centres: the row's inner width
                // split five ways, plus the gap between them.
                val pitchPx = with(LocalDensity.current) {
                    ((maxWidth - RowSidePad * 2 - ItemGap * (TabOrder.size - 1)) / TabOrder.size + ItemGap).toPx()
                }
                Row(
                    Modifier
                        .fillMaxWidth()
                        .navigationBarsPadding()
                        .padding(start = RowSidePad, end = RowSidePad, bottom = 8.dp),
                    horizontalArrangement = Arrangement.spacedBy(ItemGap),
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
                            index = index,
                            active = selected == tab,
                            // Only the tab left behind starts raised, to settle down.
                            wasActive = cameFrom == tab && cameFrom != selected,
                            fromIndex = fromIndex,
                            pitchPx = pitchPx,
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
}

/** Left-to-right order of the tabs, which the sliding circle travels along. */
private val TabOrder = listOf(
    BottomNavTab.Wallet,
    BottomNavTab.Levels,
    BottomNavTab.Home,
    BottomNavTab.Completed,
    BottomNavTab.Profile
)

private val RowSidePad = 6.dp
private val ItemGap = 2.dp

/** The last tab a bar was shown for, remembered across screens. */
private object NavMemory {
    var lastTab: BottomNavTab? = null
}

/** The circle's glide between tabs: a touch of overshoot, then settle. */
private val SlideSpring = spring<Float>(
    // Enough bounce to feel alive, little enough that a glide into Wallet or
    // Profile does not swing past the screen edge.
    dampingRatio = 0.78f,
    stiffness = Spring.StiffnessMediumLow
)

/**
 * One tab. Every item reserves the same [SlotHeight], so the five labels sit
 * on one baseline whether or not their icon is currently a raised circle.
 *
 * The raised brown circle belongs to the active tab, but when the player has
 * just come from another tab it starts over that one ([fromIndex], one
 * [pitchPx] per tab away) and glides across; the icon rises into it as it
 * arrives. The tab left behind ([wasActive]) starts raised and settles back.
 */
@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    index: Int,
    active: Boolean,
    wasActive: Boolean,
    fromIndex: Int?,
    pitchPx: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    highlightModifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val slides = active && fromIndex != null && fromIndex != index
    // How far the circle has come (0 = over the old tab, 1 = home). A tab that
    // simply is the active one, with nothing to travel from, starts home.
    val arrive = remember { Animatable(if (slides) 0f else 1f) }
    // The raise of this tab's icon: up while active, down otherwise.
    val raise = remember { Animatable(if (active || wasActive) 1f else 0f) }
    LaunchedEffect(active) {
        if (active) {
            arrive.animateTo(1f, SlideSpring)
        } else {
            arrive.snapTo(1f)
        }
    }
    LaunchedEffect(active) {
        raise.animateTo(if (active) 1f else 0f, NavSpring)
    }
    val tint by animateColorAsState(
        if (active) LightOrange else HintGray,
        tween(220), label = "tint_$label"
    )
    val interaction = remember { MutableInteractionSource() }

    Column(
        modifier.clickable(interactionSource = interaction, indication = null) {
            SoundFx.play(ctx, SoundFx.Cue.Nav)
            onClick()
        },
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            highlightModifier.wrapContentWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(Modifier.height(SlotHeight), contentAlignment = Alignment.Center) {
                if (active) {
                    // The circle, raised, gliding in from the tab the player left.
                    Box(
                        Modifier
                            .size(CircleSize)
                            .graphicsLayer {
                                val from = fromIndex ?: index
                                translationX = (from - index) * pitchPx * (1f - arrive.value)
                                translationY = -RaiseRoom.toPx()
                                shadowElevation = 10f
                                shape = CircleShape
                                clip = true
                            }
                            .background(DarkBrown, CircleShape)
                    )
                }
                Icon(
                    icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier
                        .size(23.dp)
                        .graphicsLayer {
                            // An arriving tab's icon rises with the circle; a
                            // departing one settles on its own spring.
                            val p = if (slides) arrive.value.coerceIn(0f, 1f) else raise.value
                            val sc = 1f + 0.06f * p
                            scaleX = sc
                            scaleY = sc
                            translationY = -RaiseRoom.toPx() * p
                        }
                )
            }
            Text(
                label,
                color = if (active) Burnt else HintGray,
                fontSize = 10.sp,
                fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}
