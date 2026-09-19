package com.example.kusinakode.ui.learn

import com.example.kusinakode.ui.components.clickSfx
import androidx.annotation.DrawableRes
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.Icon
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.BottomNavTab
import com.example.kusinakode.FavoritesManager
import com.example.kusinakode.KusinaBottomNav
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.R
import com.example.kusinakode.Session
import com.example.kusinakode.ui.gamification.GamificationViewModel
import com.example.kusinakode.ui.home.ChromeCream
import com.example.kusinakode.ui.home.ChromeInk
import com.example.kusinakode.ui.home.ChromeStroke
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.components.LevelImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

private val CreamBg = Color(0xFFF7EFE3)
private val TextDark = Color(0xFF3E2723)
private val BurntOrange = Color(0xFFB4510E)
private val TanBadge = Color(0xFFE4C49A)

/** How many dish cards the deck fans out; the rest wait behind the last one. */
private const val DISH_STACK_DEPTH = 5

private data class FeaturedSlide(
    @DrawableRes val image: Int,
    val kicker: String,
    val title: String,
    val line: String
)

private data class HubTile(
    val title: String,
    val subtitle: String = "",
    @DrawableRes val image: Int,
    val level: Int = 0,
    /** Set for a panel-added dish, whose photo lives on the server. */
    val imageUrl: String? = null
)

private val FeaturedSlides = listOf(
    FeaturedSlide(
        image = R.drawable.feat_bayleaves,
        kicker = "AROMATIC STAPLE",
        title = "Bay Leaves",
        line = "The leaf that steadies the stew"
    ),
    FeaturedSlide(
        image = R.drawable.feat_calamansi,
        kicker = "ISLAND CITRUS",
        title = "Calamansi",
        line = "The squeeze that finishes every plate"
    ),
    FeaturedSlide(
        image = R.drawable.feat_chili,
        kicker = "HEAT OF THE ISLANDS",
        title = "Siling Labuyo",
        line = "Small pepper, full kitchen fire"
    ),
    FeaturedSlide(
        image = R.drawable.feat_eggplant,
        kicker = "GARDEN STAPLE",
        title = "Talong",
        line = "The purple fruit of the everyday pot"
    ),
    FeaturedSlide(
        image = R.drawable.feat_flour,
        kicker = "KUSINA STAPLE",
        title = "Harina",
        line = "The start of every dough and batter"
    )
)

/**
 * KODEX landing: featured ingredients, three signature dishes, three rare
 * pantry ingredients. Dish tiles open the full encyclopedia list.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun KodexHubScreen(
    onOpenDishes: () -> Unit,
    onOpenDish: (Int) -> Unit,
    onOpenFeaturedIngredients: () -> Unit,
    onOpenRareIngredients: () -> Unit,
    onHome: () -> Unit,
    onExplore: () -> Unit,
    onProfile: () -> Unit,
    onWallet: () -> Unit,
    onSettings: () -> Unit = {}
) {
    val gamification: GamificationViewModel = viewModel()
    val game by gamification.uiState.collectAsState()
    // Every dish the player has actually solved, newest unlock at the front of
    // the deck. The stack fans only its top few cards, so the count is free.
    val dishes = remember(game.progress.solvedLevels) {
        game.progress.solvedLevels.sortedDescending().map { level ->
            val data = LevelProvider.forLevel(level)
            HubTile(
                title = data.name,
                subtitle = "${data.region.displayName} · ${data.trivia.substringBefore('.').trim()}",
                image = data.photo,
                level = level,
                imageUrl = data.photoUrl
            )
        }
    }
    val rares = listOf(
        HubTile("Bell Pepper", image = R.drawable.preview_bellpepper),
        HubTile("Carrots", image = R.drawable.preview_carrots),
        HubTile("Coconut", image = R.drawable.preview_coconut)
    )
    val slideCount = FeaturedSlides.size
    val loopPages = slideCount * 2_000
    val startPage = remember(slideCount) { loopPages / 2 }
    val pager = rememberPagerState(initialPage = startPage, pageCount = { loopPages })
    val featuredPage = pager.currentPage % slideCount
    LaunchedEffect(pager, slideCount) {
        if (slideCount < 2) return@LaunchedEffect
        while (true) {
            delay(3_000)
            if (pager.isScrollInProgress) continue
            val turn = launch {
                pager.animateScrollToPage(
                    page = pager.currentPage + 1,
                    animationSpec = tween(durationMillis = 800, easing = FastOutSlowInEasing)
                )
            }
            turn.join()
        }
    }

    Scaffold(
        containerColor = CreamBg,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Completed,
                onHome = onHome,
                onProfile = onProfile,
                onLevels = onExplore,
                onWallet = onWallet,
                onCompleted = { /* already here */ }
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    HubCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onHome
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "KODEX",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Learn everything",
                            color = LightOrange.copy(alpha = 0.92f),
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    HubCircleButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        onClick = onSettings
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {
                SectionLabel("FEATURED INGREDIENTS")
                Spacer(Modifier.height(8.dp))
                HorizontalPager(
                    state = pager,
                    beyondBoundsPageCount = 1,
                    modifier = Modifier.fillMaxWidth()
                ) { page ->
                    FeaturedBanner(FeaturedSlides[page % slideCount], onClick = onOpenFeaturedIngredients)
                }
                Spacer(Modifier.height(10.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Center,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    FeaturedSlides.indices.forEach { i ->
                        val selected = featuredPage == i
                        val dotWidth by animateDpAsState(
                            targetValue = if (selected) 18.dp else 7.dp,
                            animationSpec = tween(400, easing = FastOutSlowInEasing),
                            label = "featured_dot_w_$i"
                        )
                        val dotColor by animateColorAsState(
                            targetValue = if (selected) BurntOrange else Color(0xFFC9B8A6),
                            animationSpec = tween(400, easing = FastOutSlowInEasing),
                            label = "featured_dot_c_$i"
                        )
                        Box(
                            Modifier
                                .padding(horizontal = 3.dp)
                                .height(7.dp)
                                .width(dotWidth)
                                .clip(CircleShape)
                                .background(dotColor)
                        )
                    }
                }

                Spacer(Modifier.height(22.dp))
                SectionRow(
                    "DISHES",
                    onSeeAll = onOpenDishes,
                    count = dishes.size.takeIf { it > 0 }
                )
                Spacer(Modifier.height(10.dp))
                if (dishes.isEmpty()) {
                    DishSolvePlaceholder(onSeeAll = onOpenDishes)
                } else {
                    DishCardStack(dishes, onOpenDish = onOpenDish)
                }

                Spacer(Modifier.height(22.dp))
                SectionRow("KODEX PANTRY", onSeeAll = onOpenRareIngredients)
                Spacer(Modifier.height(10.dp))
                HubTileRow(rares, onTile = onOpenRareIngredients)
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

@Composable
private fun SectionLabel(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = TextDark,
        fontFamily = BeVietnamPro,
        fontSize = 11.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 1.6.sp,
        modifier = modifier
    )
}

@Composable
private fun SectionRow(title: String, onSeeAll: () -> Unit, count: Int? = null) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        SectionLabel(title, modifier = Modifier.clickable(onClick = clickSfx(onSeeAll)))
        // The deck can run deep, so the tally lives up here rather than over
        // the photography.
        if (count != null) {
            Spacer(Modifier.width(8.dp))
            Text(
                count.toString(),
                color = TextDark.copy(alpha = 0.45f),
                fontFamily = BeVietnamPro,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        Spacer(Modifier.weight(1f))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            modifier = Modifier.clickable(onClick = clickSfx(onSeeAll))
        ) {
            Text(
                "See all",
                color = BurntOrange,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.SemiBold,
                fontSize = 13.sp
            )
            Icon(
                Icons.AutoMirrored.Filled.KeyboardArrowRight,
                contentDescription = null,
                tint = BurntOrange,
                modifier = Modifier.size(16.dp)
            )
        }
    }
}

@Composable
private fun FeaturedBanner(slide: FeaturedSlide, onClick: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(168.dp)
            .clip(RoundedCornerShape(28.dp))
            .clickable(onClick = clickSfx(onClick))
    ) {
        Image(
            painter = painterResource(slide.image),
            contentDescription = slide.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        listOf(Color.Transparent, Color(0xFF1A0C04).copy(alpha = 0.78f))
                    )
                )
        )
        Column(
            Modifier
                .align(Alignment.BottomStart)
                .padding(18.dp)
        ) {
            Text(
                slide.kicker,
                color = TanBadge,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Text(
                slide.title,
                color = Color.White,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 20.sp
            )
            Text(
                slide.line,
                color = Color.White.copy(alpha = 0.88f),
                fontSize = 12.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis
            )
        }
    }
}

@Composable
private fun DishSolvePlaceholder(onSeeAll: () -> Unit) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
            .shadow(12.dp, RoundedCornerShape(22.dp))
            .clip(RoundedCornerShape(22.dp))
            .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
            .clickable(onClick = clickSfx(onSeeAll)),
        contentAlignment = Alignment.Center
    ) {
        Image(
            painter = painterResource(R.drawable.dishes_locked),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(width = 168.dp, height = 134.dp)
        )
    }
}

@Composable
private fun DishCardStack(
    dishes: List<HubTile>,
    onOpenDish: (Int) -> Unit
) {
    if (dishes.isEmpty()) return
    var front by remember { mutableIntStateOf(0) }
    val ctx = LocalContext.current
    val peek = 10.dp
    val n = dishes.size
    // The fan is capped: past a handful of cards the extra slivers only eat
    // into the front card's width, and every deeper card would draw a
    // full-bleed photo nobody can see.
    val depth = minOf(n, DISH_STACK_DEPTH)

    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .height(200.dp)
    ) {
        val cardWidth = maxWidth - peek * (depth - 1)
        for (idx in dishes.indices) {
            val rank = ((idx - front) + n) % n
            if (rank >= depth) continue
            val offsetX by animateDpAsState(
                targetValue = peek * rank,
                animationSpec = spring(),
                label = "dish_x_$idx"
            )
            val dish = dishes[idx]
            var isFavorite by remember(dish.level) {
                mutableStateOf(FavoritesManager.isFavorite(ctx, Session.userId, dish.level))
            }
            Box(
                Modifier
                    .zIndex((n - rank).toFloat())
                    .offset(x = offsetX)
                    .width(cardWidth)
                    .fillMaxSize()
                    .shadow(12.dp, RoundedCornerShape(22.dp))
                    .clip(RoundedCornerShape(22.dp))
                    .clickable {
                        if (idx == front) onOpenDish(dish.level) else front = idx
                    }
                    .pointerInput(n) {
                        var total = 0f
                        detectHorizontalDragGestures(
                            onDragStart = { total = 0f },
                            onDragEnd = {
                                when {
                                    total < -48f -> front = (front + 1) % n
                                    total > 48f -> front = (front - 1 + n) % n
                                }
                            },
                            onHorizontalDrag = { change, amount ->
                                change.consume()
                                total += amount
                            }
                        )
                    }
            ) {
                LevelImage(
                    url = dish.imageUrl,
                    fallback = dish.image,
                    contentDescription = dish.title,
                    modifier = Modifier.fillMaxSize(),
                    contentScale = ContentScale.Crop
                )
                Box(
                    Modifier
                        .fillMaxSize()
                        .background(
                            Brush.verticalGradient(
                                0.45f to Color.Transparent,
                                1f to Color(0xFF1A0C04).copy(alpha = 0.82f)
                            )
                        )
                )
                Box(
                    Modifier
                        .align(Alignment.TopEnd)
                        .padding(12.dp)
                        .size(36.dp)
                        .clip(CircleShape)
                        .background(ChromeCream)
                        .clickable {
                            isFavorite = FavoritesManager.toggle(ctx, Session.userId, dish.level)
                        },
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (isFavorite) Icons.Filled.Favorite else Icons.Filled.FavoriteBorder,
                        contentDescription = if (isFavorite) "Remove favorite" else "Add to favorites",
                        tint = ChromeInk,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Column(
                    Modifier
                        .align(Alignment.BottomStart)
                        .padding(16.dp)
                ) {
                    Text(
                        dish.title,
                        color = Color.White,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 20.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (dish.subtitle.isNotBlank()) {
                        Text(
                            dish.subtitle,
                            color = Color.White.copy(alpha = 0.88f),
                            fontSize = 12.sp,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun HubTileRow(tiles: List<HubTile>, onTile: () -> Unit) {
    Row(
        Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.spacedBy(12.dp)
    ) {
        tiles.forEach { tile ->
            Column(
                Modifier.weight(1f),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = Color.White,
                    shadowElevation = 4.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .aspectRatio(1f)
                        .clickable(onClick = clickSfx(onTile))
                ) {
                    Image(
                        painter = painterResource(tile.image),
                        contentDescription = tile.title,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.height(6.dp))
                Text(
                    tile.title,
                    color = TextDark,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.SemiBold,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
        }
        repeat((3 - tiles.size).coerceAtLeast(0)) {
            Spacer(Modifier.weight(1f))
        }
    }
}

@Composable
private fun HubCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(ChromeCream)
            .border(1.dp, ChromeStroke, CircleShape)
            .clickable(onClick = clickSfx(onClick)),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = ChromeInk, modifier = Modifier.size(18.dp))
    }
}
