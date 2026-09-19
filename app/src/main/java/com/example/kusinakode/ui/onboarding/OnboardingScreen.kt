package com.example.kusinakode.ui.onboarding

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.rotate
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.R
import com.example.kusinakode.domain.model.TileState
import com.example.kusinakode.ui.game.TileCorrectGreen
import com.example.kusinakode.ui.game.TileSemiYellow
import com.example.kusinakode.ui.game.TileWrongBrown
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.LightOrange
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/** Terracotta brown from the KUSINA / KODE auth wordmark tiles. */
private val KusinaTileBrown = TileWrongBrown

private data class OnboardingPage(
    val backgroundRes: Int,
    val overline: String,
    val headline: String,
    val body: String
)

private val pages = listOf(
    OnboardingPage(
        backgroundRes = R.drawable.bg102,
        overline = "WELCOME TO THE KUSINA",
        headline = "Taste the Philippines.",
        body = "A Filipino word kitchen. Guess the dish — then taste the story behind it."
    ),
    OnboardingPage(
        backgroundRes = R.drawable.bg105,
        overline = "EXPLORE THE REGIONS",
        headline = "Every dish has a home.",
        body = "Ilocos, Cebu, Batangas and beyond. Each win unlocks that dish's region, ingredients, and recipe."
    ),
    OnboardingPage(
        backgroundRes = R.drawable.bg103,
        overline = "MASTER THE PUZZLE",
        headline = "Six tries. One dish.",
        body = "Every guess colors the tiles — that's your clue trail. " +
                "Crack the code before your tries run out!"
    ),
    OnboardingPage(
        backgroundRes = R.drawable.bg107,
        overline = "EARN YOUR APRON",
        headline = "Cook your way to the top.",
        body = "Rack up points with every win, collect badges at milestones, " +
                "and climb the Kusina Masters leaderboard."
    )
)

/**
 * Feature intro shown to new cooks before they create an account.
 * Skippable at any point; Skip and the final CTA both call [onDone].
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun OnboardingScreen(
    onDone: () -> Unit,
    doneLabel: String = "Start Cooking!"
) {
    val pagerState = rememberPagerState(pageCount = { pages.size })
    val scope = rememberCoroutineScope()
    val isLastPage = pagerState.currentPage == pages.lastIndex

    Box(Modifier.fillMaxSize()) {
        Crossfade(
            targetState = pagerState.currentPage,
            animationSpec = tween(500),
            label = "onboarding_bg"
        ) { page ->
            Image(
                painter = painterResource(pages[page].backgroundRes),
                contentDescription = null,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
        }
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to Color.Black.copy(alpha = 0.35f),
                        0.45f to DarkBrown.copy(alpha = 0.35f),
                        1f to Color(0xFF2A1608).copy(alpha = 0.94f)
                    )
                )
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
        ) {
            HorizontalPager(
                state = pagerState,
                modifier = Modifier.weight(1f)
            ) { pageIndex ->
                val page = pages[pageIndex]
                val isActive = pagerState.currentPage == pageIndex

                val config = LocalConfiguration.current
                val fontScale = LocalDensity.current.fontScale
                val headlineSize =
                    ((config.screenWidthDp - 56) / (page.headline.length * 0.58f) / fontScale)
                        .coerceIn(15f, 26f)
                val fitScale = (config.screenHeightDp / 800f).coerceIn(0.72f, 1f)

                val heroScale by animateFloatAsState(
                    targetValue = if (isActive) 1f else 0.6f,
                    animationSpec = spring(
                        dampingRatio = Spring.DampingRatioMediumBouncy,
                        stiffness = Spring.StiffnessLow
                    ),
                    label = "hero_scale"
                )
                val bob by rememberInfiniteTransition(label = "bob").animateFloat(
                    initialValue = 0f,
                    targetValue = 8f,
                    animationSpec = infiniteRepeatable(tween(1700), RepeatMode.Reverse),
                    label = "bob_offset"
                )

                Column(
                    Modifier
                        .fillMaxSize()
                        .padding(horizontal = 28.dp),
                    horizontalAlignment = Alignment.Start,
                    verticalArrangement = Arrangement.Bottom
                ) {
                    Box(
                        Modifier
                            .fillMaxWidth()
                            .weight(1f)
                            .graphicsLayer {
                                scaleX = heroScale * fitScale
                                scaleY = heroScale * fitScale
                                translationY = if (isActive) -bob.dp.toPx() else 0f
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        when (pageIndex) {
                            0 -> WelcomeHero()
                            1 -> DishCardFan()
                            2 -> TileTeaser(isActive)
                            else -> RewardTeaser(isActive)
                        }
                    }

                    Spacer(Modifier.height(20.dp))
                    Text(
                        page.overline,
                        style = MaterialTheme.typography.labelMedium.copy(
                            color = LightOrange,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 3.sp
                        )
                    )
                    Spacer(Modifier.height(10.dp))
                    Text(
                        page.headline,
                        style = MaterialTheme.typography.headlineSmall.copy(
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = headlineSize.sp,
                            lineHeight = (headlineSize * 1.25f).sp
                        ),
                        maxLines = 1,
                        softWrap = false
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        page.body,
                        style = MaterialTheme.typography.bodyLarge.copy(
                            color = LightOrange.copy(alpha = 0.85f),
                            lineHeight = 24.sp
                        )
                    )
                    Spacer(Modifier.height(28.dp))
                }
            }

            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp)
            ) {
                TextButton(
                    onClick = clickSfx(onDone),
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Text(
                        "Skip",
                        color = LightOrange.copy(alpha = 0.9f),
                        fontWeight = FontWeight.SemiBold,
                        letterSpacing = 1.sp
                    )
                }
                Row(
                    Modifier.align(Alignment.Center),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    repeat(pages.size) { i ->
                        val active = pagerState.currentPage == i
                        val width by androidx.compose.animation.core.animateDpAsState(
                            targetValue = if (active) 26.dp else 8.dp,
                            animationSpec = tween(300),
                            label = "dot_width"
                        )
                        Box(
                            Modifier
                                .padding(horizontal = 4.dp)
                                .height(8.dp)
                                .width(width)
                                .clip(CircleShape)
                                .background(
                                    if (active) LightOrange else LightOrange.copy(alpha = 0.35f)
                                )
                        )
                    }
                }
                Button(
                    onClick = clickSfx {
                        if (isLastPage) onDone()
                        else scope.launch { pagerState.animateScrollToPage(pagerState.currentPage + 1) }
                    },
                    modifier = Modifier
                        .align(Alignment.CenterEnd)
                        .size(56.dp),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = DarkBrown,
                        contentColor = LightOrange
                    ),
                    shape = CircleShape,
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    contentPadding = PaddingValues(0.dp)
                ) {
                    Icon(
                        imageVector = Icons.AutoMirrored.Filled.ArrowForward,
                        contentDescription = if (isLastPage) doneLabel else "Next",
                        modifier = Modifier.size(28.dp)
                    )
                }
            }
            Spacer(Modifier.height(20.dp))
        }
    }
}

/** Page 1: a plated logo with a taste of the puzzle tiles underneath. */
@Composable
private fun WelcomeHero() {
    Column(
        modifier = Modifier.offset(y = 40.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            Box(
                Modifier
                    .size(248.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                LightOrange.copy(alpha = 0.45f),
                                Color(0xFFCC6B1F).copy(alpha = 0.12f),
                                Color.Transparent
                            )
                        )
                    )
            )
            Box(
                modifier = Modifier
                    .size(188.dp)
                    .clip(CircleShape)
                    .background(LightOrange)
                    .padding(7.dp)
                    .clip(CircleShape)
                    .background(DarkBrown)
                    .padding(7.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFFFF6E8)),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.kk_logo),
                    contentDescription = null,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(18.dp)
                        .clip(CircleShape),
                    contentScale = ContentScale.Crop
                )
            }
        }
        Spacer(Modifier.height(10.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFF1A0C04).copy(alpha = 0.72f),
            shadowElevation = 6.dp,
            border = BorderStroke(1.5.dp, LightOrange.copy(alpha = 0.9f))
        ) {
            Text(
                "Word puzzle  ·  Filipino kitchen",
                modifier = Modifier.padding(horizontal = 18.dp, vertical = 9.dp),
                color = LightOrange,
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.6.sp
            )
        }
        Spacer(Modifier.height(14.dp))
        // Same poster tiles as KUSINA on the auth screens, spelling ADOBO.
        Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
            val tiles = listOf(
                'A' to TileCorrectGreen,
                'D' to Color.Transparent,
                'O' to TileSemiYellow,
                'B' to Color.Transparent,
                'O' to TileWrongBrown
            )
            tiles.forEach { (ch, bg) ->
                val glass = bg == Color.Transparent
                Box(
                    Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(6.dp))
                        .background(bg)
                        .border(
                            1.dp,
                            if (glass) Color.White.copy(alpha = 0.45f)
                            else Color.Black.copy(alpha = 0.18f),
                            RoundedCornerShape(6.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        ch.toString(),
                        color = Color.White,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                }
            }
        }
    }
}

/** Page 2: three heritage cards, same art as the collectibles in the KODEX. */
@Composable
private fun DishCardFan() {
    BoxWithConstraints(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val ratio = 522f / 924f
        val cardWidth = minOf(maxWidth * 0.46f, maxHeight * ratio * 0.84f)
        val spread = cardWidth * 0.58f
        val drop = 24.dp
        HeritageFanCard(
            cardRes = R.drawable.card_adobo,
            contentDescription = "Adobo",
            rotation = -10f,
            offsetX = -spread,
            offsetY = cardWidth * 0.12f + drop,
            cardWidth = cardWidth
        )
        HeritageFanCard(
            cardRes = R.drawable.card_bulalo,
            contentDescription = "Bulalo from Batangas",
            rotation = 10f,
            offsetX = spread,
            offsetY = cardWidth * 0.12f + drop,
            cardWidth = cardWidth
        )
        HeritageFanCard(
            cardRes = R.drawable.card_lechon,
            contentDescription = "Lechon from Cebu",
            rotation = 0f,
            offsetX = 0.dp,
            offsetY = -(cardWidth * 0.06f) + drop,
            cardWidth = cardWidth
        )
    }
}

@Composable
private fun HeritageFanCard(
    cardRes: Int,
    contentDescription: String,
    rotation: Float,
    offsetX: androidx.compose.ui.unit.Dp,
    offsetY: androidx.compose.ui.unit.Dp,
    cardWidth: androidx.compose.ui.unit.Dp
) {
    Image(
        painter = painterResource(cardRes),
        contentDescription = contentDescription,
        contentScale = ContentScale.Fit,
        modifier = Modifier
            .offset(x = offsetX, y = offsetY)
            .rotate(rotation)
            .width(cardWidth)
            .aspectRatio(522f / 924f)
    )
}

/** Page 3: real game tiles that flip in one-by-one when the page lands. */
@Composable
private fun TileTeaser(isActive: Boolean) {
    val letters = "ADOBO"
    val states = listOf(
        TileState.Correct, TileState.SemiCorrect, TileState.Wrong,
        TileState.Correct, TileState.Correct
    )
    val pops = remember { letters.map { Animatable(0f) } }

    LaunchedEffect(isActive) {
        if (isActive) {
            pops.forEachIndexed { i, anim ->
                launch {
                    delay(120L * i)
                    anim.animateTo(
                        1f,
                        spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                    )
                }
            }
        } else {
            pops.forEach { it.snapTo(0f) }
        }
    }

    // Fill the hero slot so the ADOBO row and legend sit dead-center.
    BoxWithConstraints(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        val gap = 7.dp
        val tile = ((maxWidth - gap * (letters.length - 1)) / letters.length)
            .coerceIn(38.dp, 55.dp)

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .offset(y = 40.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                letters.forEachIndexed { i, ch ->
                    val fill = when (states[i]) {
                        TileState.Correct -> TileCorrectGreen
                        TileState.SemiCorrect -> TileSemiYellow
                        else -> KusinaTileBrown
                    }
                    KusinaPosterTile(
                        letter = ch,
                        fill = fill,
                        fontSize = (tile.value * 0.48f).sp,
                        modifier = Modifier
                            .padding(horizontal = gap / 2)
                            .size(tile)
                            .graphicsLayer {
                                scaleX = pops[i].value
                                scaleY = pops[i].value
                            }
                    )
                }
            }
            Spacer(Modifier.height(14.dp))
            Row(
                Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.Center,
                verticalAlignment = Alignment.CenterVertically
            ) {
                LegendLabel("Right spot", TileCorrectGreen)
                LegendDot()
                LegendLabel("Wrong spot", TileSemiYellow)
                LegendDot()
                LegendLabel("Not in word", TileWrongBrown)
            }
        }
    }
}

/** Auth wordmark chip texture, filled with the in-game tile colours. */
@Composable
private fun KusinaPosterTile(
    letter: Char,
    fill: Color,
    fontSize: androidx.compose.ui.unit.TextUnit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .clip(RoundedCornerShape(6.dp))
            .background(fill)
            .border(1.dp, Color.Black.copy(alpha = 0.18f), RoundedCornerShape(6.dp)),
        contentAlignment = Alignment.Center
    ) {
        Text(
            letter.toString(),
            color = Color.White,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = fontSize
        )
    }
}

@Composable
private fun LegendLabel(text: String, color: Color) {
    Text(
        text,
        color = color,
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp,
        letterSpacing = 0.4.sp
    )
}

@Composable
private fun LegendDot() {
    Text(
        " · ",
        color = Color.White.copy(alpha = 0.55f),
        fontWeight = FontWeight.Bold,
        fontSize = 15.sp
    )
}

private data class RewardMedalSpec(
    val icon: ImageVector,
    val label: String,
    val accent: Color,
    val deep: Color
)

/**
 * Page 4: the archipelago you'll conquer, lit from behind, with the three
 * rewards struck as medallions beneath a winner's ribbon.
 */
@Composable
private fun RewardTeaser(isActive: Boolean) {
    val medals = listOf(
        RewardMedalSpec(Icons.Default.Star, "POINTS", Color(0xFFFFC93C), Color(0xFFC98912)),
        RewardMedalSpec(Icons.Default.MilitaryTech, "BADGES", Color(0xFF5EC85A), Color(0xFF2E8A32)),
        RewardMedalSpec(Icons.Default.EmojiEvents, "RANKS", Color(0xFFFF8A3C), Color(0xFFD45A12))
    )
    // 0 = map, 1 = ribbon, 2..4 = medallions.
    val pops = remember { List(medals.size + 2) { Animatable(0f) } }

    LaunchedEffect(isActive) {
        if (!isActive) {
            pops.forEach { it.snapTo(0f) }
            return@LaunchedEffect
        }
        pops.forEachIndexed { i, anim ->
            launch {
                delay(150L * i)
                anim.animateTo(
                    1f,
                    spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
                )
            }
        }
    }

    // Pins breathe, so the map isn't a still picture.
    val pulse by rememberInfiniteTransition(label = "reward_pulse").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(2000), RepeatMode.Restart),
        label = "reward_pulse_value"
    )

    BoxWithConstraints(
        Modifier.fillMaxSize(),
        contentAlignment = Alignment.Center
    ) {
        // Reserve the medallion strip, give the rest to the map.
        val medalRow = 78.dp
        val ribbonH = 112.dp
        val mapSide = minOf(maxWidth * 0.85f, maxHeight - medalRow - ribbonH - 6.dp)

        Column(
            modifier = Modifier.offset(y = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            BoxWithConstraints(
                Modifier
                    .size(mapSide)
                    .offset(y = 50.dp)
                    .graphicsLayer { scaleX = pops[0].value; scaleY = pops[0].value },
                contentAlignment = Alignment.Center
            ) {
                val mw = maxWidth
                val mh = maxHeight

                // Pixel-art land / mountains / rivers, clear background.
                Image(
                    painter = painterResource(R.drawable.map_philippines_pixel),
                    contentDescription = "Map of the Philippines",
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Fit
                )

                listOf(0.38f to 0.14f, 0.58f to 0.55f, 0.66f to 0.75f).forEachIndexed { i, (fx, fy) ->
                    val pinPulse = ((pulse + i * 0.33f) % 1f)
                    MapBeacon(
                        pulse = pinPulse,
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .offset(x = mw * fx - 22.dp, y = mh * fy - 32.dp)
                    )
                }
            }

            KusinaMasterTitle(
                modifier = Modifier
                    .offset(y = 22.dp)
                    .graphicsLayer {
                        scaleX = pops[1].value
                        scaleY = pops[1].value
                    }
            )

            Spacer(Modifier.height(8.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(20.dp)) {
                medals.forEachIndexed { i, spec ->
                    RewardMedal(
                        spec = spec,
                        modifier = Modifier.graphicsLayer {
                            scaleX = pops[i + 2].value
                            scaleY = pops[i + 2].value
                        }
                    )
                }
            }
        }
    }
}

@Composable
private fun MapBeacon(pulse: Float, modifier: Modifier = Modifier) {
    Box(modifier.size(44.dp), contentAlignment = Alignment.Center) {
        Box(
            Modifier
                .size((16 + 28 * pulse).dp)
                .clip(CircleShape)
                .background(Color(0xFFFFD166).copy(alpha = 0.32f * (1f - pulse)))
        )
        Box(
            Modifier
                .size((10 + 14 * pulse).dp)
                .border(
                    1.5.dp,
                    Color(0xFFFFF6D0).copy(alpha = 0.75f * (1f - pulse)),
                    CircleShape
                )
        )
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(0xFF5C2A12),
            modifier = Modifier.size(32.dp)
        )
        Icon(
            Icons.Default.LocationOn,
            contentDescription = null,
            tint = Color(0xFFFFC93C),
            modifier = Modifier.size(28.dp)
        )
        Box(
            Modifier
                .offset(y = (-5).dp)
                .size(6.dp)
                .clip(CircleShape)
                .background(Color.White.copy(alpha = 0.92f))
        )
    }
}

@Composable
private fun KusinaMasterTitle(modifier: Modifier = Modifier) {
    Image(
        painter = painterResource(R.drawable.ribbon_kusina_master),
        contentDescription = "Kusina Master",
        modifier = modifier
            .fillMaxWidth(0.65f)
            .height(100.dp),
        contentScale = ContentScale.Fit
    )
}

@Composable
private fun RewardMedal(
    spec: RewardMedalSpec,
    modifier: Modifier = Modifier
) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = modifier
    ) {
        Box(
            Modifier
                .size(60.dp)
                .drawBehind {
                    drawCircle(
                        brush = Brush.radialGradient(
                            colors = listOf(
                                spec.accent.copy(alpha = 0.55f),
                                Color.Transparent
                            ),
                            center = center,
                            radius = size.minDimension * 0.62f
                        )
                    )
                }
                .shadow(10.dp, CircleShape)
                .clip(CircleShape)
                .background(
                    Brush.linearGradient(
                        colors = listOf(spec.accent, spec.deep, spec.accent),
                        start = Offset.Zero,
                        end = Offset.Infinite
                    )
                )
                .padding(3.5.dp),
            contentAlignment = Alignment.Center
        ) {
            Box(
                Modifier
                    .fillMaxSize()
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(Color(0xFFFFFDF6), Color(0xFFF0E0B8))
                        )
                    )
                    .border(1.dp, spec.accent.copy(alpha = 0.40f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    spec.icon,
                    contentDescription = spec.label,
                    tint = spec.deep,
                    modifier = Modifier.size(26.dp)
                )
                Box(
                    Modifier
                        .align(Alignment.TopCenter)
                        .offset(y = 7.dp)
                        .size(width = 18.dp, height = 7.dp)
                        .clip(CircleShape)
                        .background(Color.White.copy(alpha = 0.38f))
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            spec.label,
            color = spec.accent,
            fontFamily = BeVietnamPro,
            fontSize = 10.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.6.sp
        )
    }
}
