package com.example.kusinakode

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
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
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
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

            Row(
                Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .navigationBarsPadding()
                    .padding(start = 6.dp, end = 6.dp, bottom = 8.dp),
                horizontalArrangement = Arrangement.spacedBy(2.dp),
                verticalAlignment = Alignment.Bottom
            ) {
                NavItem(
                    Icons.Default.AccountBalanceWallet, "Wallet",
                    selected == BottomNavTab.Wallet,
                    Modifier.weight(1f), onWallet,
                    highlightModifier = walletModifier
                )
                NavItem(
                    Icons.Default.TravelExplore, "Explore",
                    selected == BottomNavTab.Levels, Modifier.weight(1f), onLevels
                )
                NavItem(
                    Icons.Default.Home, "Home",
                    selected == BottomNavTab.Home, Modifier.weight(1f), onHome
                )
                NavItem(
                    Icons.Default.School, "Learn",
                    selected == BottomNavTab.Completed, Modifier.weight(1f), onCompleted
                )
                NavItem(
                    Icons.Default.Person, "Profile",
                    selected == BottomNavTab.Profile, Modifier.weight(1f), onProfile
                )
            }
        }
    }
}

/**
 * One tab. Every item reserves the same [SlotHeight], so the five labels sit
 * on one baseline whether or not their icon is currently a raised circle.
 */
@Composable
private fun NavItem(
    icon: ImageVector,
    label: String,
    active: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit,
    highlightModifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    val p by animateFloatAsState(if (active) 1f else 0f, NavSpring, label = "nav_$label")
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
                // The circle grows in behind the glyph and carries it upward, so
                // the icon never jumps between two separate treatments.
                Box(
                    Modifier
                        .size(CircleSize)
                        .graphicsLayer {
                            scaleX = p
                            scaleY = p
                            alpha = p
                            translationY = -RaiseRoom.toPx() * p
                            shadowElevation = 10f * p
                            shape = CircleShape
                            clip = true
                        }
                        .background(DarkBrown, CircleShape)
                )
                Icon(
                    icon,
                    contentDescription = label,
                    tint = tint,
                    modifier = Modifier
                        .size(23.dp)
                        .graphicsLayer {
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
