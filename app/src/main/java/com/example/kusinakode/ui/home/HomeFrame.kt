package com.example.kusinakode.ui.home

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.Image
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.LocalTextStyle
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.Color
import androidx.annotation.DrawableRes
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.ProgressFillEnd
import com.example.kusinakode.ui.theme.ProgressFillStart
import com.example.kusinakode.ui.theme.ProgressTrackIdle

internal val CreamBg = Color(0xFFF7EFE3)
internal val TextDark = Color(0xFF3E2723)
internal val Burnt = Color(0xFFB4510E)
/** Exact cream / stroke / ink from the Frame 5 KK pill + gear crops. */
internal val ChromeCream = Color(0xFFE9E1D2)
internal val ChromeStroke = Color(0xFFBFAE9D)
internal val ChromeInk = Color(0xFF78592B)
private val AvatarRing = Color(0xFFBEAD9C)

/**
 * The level bar's old length, back when the KK pill sat beside it: the header
 * row minus avatar, chrome buttons, the pill's own footprint and the LVL label.
 */
private val LevelBarWidth = 90.dp
/** The unread dot on the bell — the one alarm colour in the header. */
private val NotifyRed = Color(0xFFD1362F)

/** Lets label text sit on photography without dimming the photograph. */
private val CardTextShadow = Shadow(
    color = Color(0xCC0B0603),
    offset = Offset(0f, 2f),
    blurRadius = 7f
)

/**
 * The dashboard header from Frame 5: portrait, name, level bar, balance, bell,
 * gear.
 *
 * The balance used to ride here in a cream pill, which made it look like a
 * button and crowded the row. It is the coin and the number now — the same
 * minted coin the ledger rows wear — sitting on the brown with nothing around
 * it.
 */
@Composable
fun HomeHeader(
    chefName: String,
    level: Int,
    levelProgress: Float,
    balanceKk: Long,
    portrait: @Composable () -> Unit,
    onPortrait: () -> Unit,
    onNotifications: () -> Unit,
    /** Unread inbox entries plus anything waiting to be claimed. */
    unreadNotifications: Int = 0,
    onSettings: () -> Unit,
    /**
     * Extra brown below the header row for the hero card to lap into.
     * Applied to the inner Row, not the outer modifier: padding on the outer
     * modifier is subtracted before the background is drawn, which shrinks the
     * panel instead of deepening it.
     */
    panelDepth: Dp = 0.dp,
    modifier: Modifier = Modifier
) {
    val ctx = LocalContext.current
    Box(
        modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(bottomStart = 19.dp, bottomEnd = 19.dp))
            .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
    ) {
        Row(
            Modifier
                .statusBarsPadding()
                .padding(
                    start = 18.dp,
                    end = 16.dp,
                    top = 14.dp,
                    bottom = 18.dp + panelDepth
                ),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Box(
                Modifier
                    .clip(CircleShape)
                    .border(1.5.dp, AvatarRing, CircleShape)
                    .clickable {
                        SoundFx.play(ctx, SoundFx.Cue.Nav)
                        onPortrait()
                    }
            ) { portrait() }

            Spacer(Modifier.width(14.dp))

            Column(Modifier.weight(1f)) {
                Text(
                    "Chef $chefName",
                    color = Color.White,
                    fontSize = 17.sp,
                    fontWeight = FontWeight.Bold,
                    maxLines = 1
                )
                Spacer(Modifier.height(5.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "LVL $level",
                        color = LightOrange.copy(alpha = 0.95f),
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                    Spacer(Modifier.width(8.dp))
                    LinearProgressIndicator(
                        progress = { levelProgress.coerceIn(0f, 1f) },
                        color = LightOrange,
                        trackColor = Color.Black.copy(alpha = 0.28f),
                        modifier = Modifier
                            // Capped rather than greedy: with the KK pill gone
                            // the bar would stretch across the space the pill
                            // used to hold. The level reads at the length it
                            // always had; the name keeps the freed room.
                            .weight(1f, fill = false)
                            .widthIn(max = LevelBarWidth)
                            .height(8.dp)
                            .clip(CircleShape)
                    )

                    // The balance rides the level line rather than the middle
                    // of the header. Capping the bar left a pocket of empty
                    // column between it and the chrome, and a number sitting
                    // alone in that pocket read as dropped there. Here it is
                    // anchored to something, and the slack falls after it.
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "$balanceKk",
                        // The same cream as LVL beside it: both are quiet
                        // readouts on the level line, and white made the
                        // balance the loudest thing in the row.
                        color = LightOrange.copy(alpha = 0.95f),
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Normal,
                        maxLines = 1
                    )
                    Spacer(Modifier.width(4.dp))
                    Image(
                        painter = painterResource(R.drawable.ic_kk_pixel),
                        contentDescription = null,
                        modifier = Modifier.size(16.dp)
                    )
                }
            }

            Spacer(Modifier.width(10.dp))

            // The inbox had no door: onNotifications was wired from
            // MainActivity but nothing on Home ever called it.
            Box(contentAlignment = Alignment.TopEnd) {
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChromeCream)
                        .border(1.dp, ChromeStroke, CircleShape)
                        .clickable {
                            SoundFx.play(ctx, SoundFx.Cue.Nav)
                            onNotifications()
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (unreadNotifications > 0) Icons.Filled.Notifications
                        else Icons.Outlined.Notifications,
                        contentDescription = if (unreadNotifications > 0) {
                            "Notifications, $unreadNotifications waiting"
                        } else {
                            "Notifications"
                        },
                        tint = ChromeInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
                // Nothing on Home said the inbox had anything in it. The count
                // rides the rim so it reads without opening the screen; past
                // nine it stops counting, because the number stops mattering.
                if (unreadNotifications > 0) {
                    Box(
                        Modifier
                            .offset(x = 3.dp, y = (-3).dp)
                            .defaultMinSize(minWidth = 17.dp, minHeight = 17.dp)
                            .clip(CircleShape)
                            .background(NotifyRed)
                            .border(1.5.dp, ChromeCream, CircleShape)
                            .padding(horizontal = 4.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            if (unreadNotifications > 9) "9+" else "$unreadNotifications",
                            color = Color.White,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Black
                        )
                    }
                }
            }

            Spacer(Modifier.width(8.dp))

            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ChromeCream)
                    .border(1.dp, ChromeStroke, CircleShape)
                    .clickable {
                        SoundFx.play(ctx, SoundFx.Cue.Nav)
                        onSettings()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    tint = ChromeInk,
                    modifier = Modifier.size(18.dp)
                )
            }
        }
    }
}

/** The four-up stats strip: foods, regions, coins, badges. */
@Composable
fun HomeStatsRow(
    foods: Int,
    regions: Int,
    coins: Long,
    badges: Int,
    modifier: Modifier = Modifier,
    regionsModifier: Modifier = Modifier,
    badgesModifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = HeaderTop,
        modifier = modifier.fillMaxWidth()
    ) {
        Row(
            Modifier.padding(vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            StatCell(Icons.Default.Restaurant, "$foods", "FOODS", Modifier.weight(1f))
            CellDivider()
            StatCell(
                Icons.Default.Public, "$regions", "REGIONS",
                modifier = Modifier.weight(1f),
                anchorModifier = regionsModifier
            )
            CellDivider()
            StatCell(
                icon = null,
                value = "$coins",
                label = "COINS",
                modifier = Modifier.weight(1f),
                glyph = "KK"
            )
            CellDivider()
            StatCell(
                Icons.Default.MilitaryTech, "$badges", "BADGES",
                modifier = Modifier.weight(1f),
                anchorModifier = badgesModifier
            )
        }
    }
}

@Composable
private fun CellDivider() {
    Box(
        Modifier
            .height(34.dp)
            .width(1.dp)
            .background(LightOrange.copy(alpha = 0.22f))
    )
}

@Composable
private fun StatCell(
    icon: ImageVector?,
    value: String,
    label: String,
    modifier: Modifier = Modifier,
    /** Letters drawn in a ring, for a currency with no Material icon. */
    glyph: String? = null,
    anchorModifier: Modifier = Modifier
) {
    Column(
        modifier.then(anchorModifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        if (glyph != null) {
            Box(
                Modifier
                    .size(21.dp)
                    .border(1.5.dp, LightOrange, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    glyph,
                    color = LightOrange,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 9.sp,
                    letterSpacing = 0.5.sp
                )
            }
        } else if (icon != null) {
            Icon(icon, contentDescription = label, tint = LightOrange, modifier = Modifier.size(19.dp))
        }
        Spacer(Modifier.height(6.dp))
        Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 17.sp, maxLines = 1)
        Text(
            label,
            color = LightOrange.copy(alpha = 0.85f),
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp
        )
    }
}

/** One region card in the Continue Learning rail. */
@Composable
fun RegionProgressCard(
    region: Region,
    solved: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = if (total == 0) 0f else solved.toFloat() / total
    val shape = RoundedCornerShape(18.dp)
    Box(
        modifier
            .width(242.dp)
            .height(162.dp)
            .shadow(6.dp, shape, clip = false)
            .clip(shape)
            .clickable { onClick() }
    ) {
        // The landscape is the card, not a header strip above one: the place
        // carries it and the numbers sit on top of the place.
        Image(
            painter = painterResource(homeMapArt(region)),
            contentDescription = region.displayName,
            contentScale = ContentScale.Crop,
            modifier = Modifier.fillMaxSize()
        )
        // Two soft scrims, kept light so the landscape stays the loudest thing
        // on the card — the text carries its own shadow instead of asking the
        // art to be dimmed until it reads.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Transparent,
                        0.42f to Color.Transparent,
                        1f to Color(0xAD140A04)
                    )
                )
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.horizontalGradient(
                        0f to Color(0x4D140A04),
                        0.58f to Color.Transparent
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(horizontal = 16.dp, vertical = 14.dp),
            verticalArrangement = Arrangement.Bottom
        ) {
            Text(
                region.displayName,
                color = Color.White,
                fontSize = 22.sp,
                fontWeight = FontWeight.SemiBold,
                maxLines = 1,
                style = LocalTextStyle.current.copy(shadow = CardTextShadow)
            )
            Spacer(Modifier.height(2.dp))
            Text(
                region.tagline,
                color = LightOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Medium,
                maxLines = 2,
                overflow = TextOverflow.Clip,
                style = LocalTextStyle.current.copy(shadow = CardTextShadow)
            )
            Spacer(Modifier.height(12.dp))
            Text(
                "${(pct * 100).toInt()}% Complete",
                color = Color.White,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                style = LocalTextStyle.current.copy(shadow = CardTextShadow)
            )
            Spacer(Modifier.height(6.dp))
            Box(
                Modifier
                    .fillMaxWidth(0.62f)
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(Color.White.copy(alpha = 0.32f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(pct.coerceIn(0f, 1f))
                        .clip(CircleShape)
                        .background(
                            Brush.horizontalGradient(listOf(ProgressFillStart, ProgressFillEnd))
                        )
                )
            }
        }
    }
}

/** The island artwork that stands for a region across the app. */
@androidx.annotation.DrawableRes
fun regionIcon(region: Region): Int = when (region) {
    Region.LUZON -> R.drawable.region_luzon
    Region.VISAYAS -> R.drawable.region_visayas
    Region.MINDANAO -> R.drawable.region_mindanao
    Region.PHILIPPINES -> R.drawable.region_philippines
}

/**
 * The wide pixel-art landscape behind a region on the home rail — Banaue's
 * terraces for Luzon, the karst lagoons for Visayas, Lake Sebu for Mindanao
 * and Luneta under the flag for the dishes the whole country claims.
 */
@androidx.annotation.DrawableRes
fun homeMapArt(region: Region): Int = when (region) {
    Region.LUZON -> R.drawable.home_map_luzon
    Region.VISAYAS -> R.drawable.home_map_visayas
    Region.MINDANAO -> R.drawable.home_map_mindanao
    Region.PHILIPPINES -> R.drawable.home_map_philippines
}

/** Section heading with the "See All ›" affordance. */
@Composable
fun SectionHeader(title: String, actionLabel: String? = null, onAction: () -> Unit = {}) {
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(
            title,
            style = MaterialTheme.typography.titleMedium.copy(
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        )
        Spacer(Modifier.weight(1f))
        if (actionLabel != null) {
            Text(
                "$actionLabel ›",
                color = Burnt,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp,
                modifier = Modifier.clickable { onAction() }
            )
        }
    }
}
