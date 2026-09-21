package com.example.kusinakode.ui.learn

import com.example.kusinakode.ui.components.readableWidth
import com.example.kusinakode.R
import com.example.kusinakode.data.levels.EquipmentSync
import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Eco
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FavoriteBorder
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Grain
import androidx.compose.material.icons.filled.LocalDrink
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.SetMeal
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.SoupKitchen
import androidx.compose.material.icons.filled.Blender
import androidx.compose.material.icons.filled.Brush
import androidx.compose.material.icons.filled.ContentCut
import androidx.compose.material.icons.filled.DinnerDining
import androidx.compose.material.icons.filled.FilterAlt
import androidx.compose.material.icons.filled.Handyman
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.OutdoorGrill
import androidx.compose.material.icons.filled.Park
import androidx.compose.material.icons.filled.RamenDining
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Straighten
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.text.withStyle
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.FavoritesManager
import com.example.kusinakode.LevelData
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.Session
import com.example.kusinakode.data.dishes.DishCatalog
import com.example.kusinakode.data.dishes.DishEntry
import com.example.kusinakode.data.repository.LocalDishContentRepository
import com.example.kusinakode.ui.gamification.GamificationViewModel
import com.example.kusinakode.domain.model.DishContent
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.domain.repository.DishContentRepository
import com.example.kusinakode.ui.home.regionIcon
import com.example.kusinakode.ui.rewards.HeaderCircleButton
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.OutlineDefault
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlin.random.Random
import androidx.compose.runtime.LaunchedEffect
import com.example.kusinakode.domain.GameEvents
import com.example.kusinakode.ui.components.LevelImage

private val QuestGold = Color(0xFFFFD24A)
private val CreamBg = Color(0xFFF1E6D2)
private val TextDark = Color(0xFF3E2723)
private val ValidatedGreen = Color(0xFF3D6B3A)
/** Same gold stroke as the inbox rows (Palayok Spin, dish receipts). */
private val CardEdge = Color(0xFFE0C48A)

class DishDetailViewModel(
    levelId: Int,
    contentRepository: DishContentRepository
) : ViewModel() {
    private val _content = MutableStateFlow<DishContent?>(null)
    val content: StateFlow<DishContent?> = _content.asStateFlow()

    init {
        viewModelScope.launch {
            contentRepository.dishContent(levelId).onSuccess { _content.value = it }
        }
    }

    class Factory(private val levelId: Int) : ViewModelProvider.Factory {
        override fun <T : ViewModel> create(modelClass: Class<T>): T {
            @Suppress("UNCHECKED_CAST")
            return DishDetailViewModel(levelId, LocalDishContentRepository()) as T
        }
    }
}

private enum class DetailTab(val label: String) {
    INFORMATION("Information"), STEPS("Steps"), EQUIPMENT("Equipment")
}

/**
 * Masterclass dish page (Frames 9–11): hero, stat strip, and the
 * Information / Steps / Equipment tabs. Content is preview data until
 * Module 4's validated dataset replaces LocalDishContentRepository.
 */
@Composable
fun DishDetailScreen(
    level: Int,
    onBack: () -> Unit,
    onOpenDish: (Int) -> Unit = {},
    onSettings: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val data = remember(level) { LevelProvider.forLevel(level) }
    val viewModel: DishDetailViewModel = viewModel(
        key = "dish_$level",
        factory = DishDetailViewModel.Factory(level)
    )
    val content by viewModel.content.collectAsState()

    // Opening a dish's entry is what the knowledge badges are earned by.
    // Keyed on the level so paging through the KODEX records each one.
    LaunchedEffect(level) { GameEvents.publishDishRead(level) }

    var tab by remember(level) { mutableStateOf(DetailTab.INFORMATION) }
    var isFavorite by remember(level) {
        mutableStateOf(FavoritesManager.isFavorite(ctx, Session.userId, level))
    }

    // The photograph for the hero; the illustrated heritage card keeps its
    // own collectible slot further down. A level added from the admin panel
    // has no packaged photo, only a URL, so both are carried and the image
    // falls back to the resource when there is nothing to fetch.
    val dishPhoto = data.photo
    val dishPhotoUrl = data.photoUrl
    var showPhoto by remember { mutableStateOf(false) }
    var showCard by remember { mutableStateOf(false) }

    // Browse the dishes already unlocked, in KODEX order.
    val gamification: GamificationViewModel = viewModel()
    val game by gamification.uiState.collectAsState()
    val unlockedDishes = remember(level, game.progress.solvedLevels) {
        (game.progress.solvedLevels + level).sorted()
    }
    val position = unlockedDishes.indexOf(level)
    val previousLevel = unlockedDishes.getOrNull(position - 1)
    val nextLevel = unlockedDishes.getOrNull(position + 1)

    // The dataset records how hard the dish is to cook. This used to be
    // guessed from the answer's length, which said nothing about the cooking
    // and happened to agree often enough to look right.
    val difficulty = data.dish.difficulty

    // Seeded by level so the same dish always asks the same question with the
    // options in the same order.
    val trivia = remember(level) { triviaFor(level, data.dish) }
    var triviaChoice by remember(level) { mutableStateOf<Int?>(null) }

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
            .verticalScroll(rememberScrollState())
    ) {
        // No full-bleed header on this one, so the whole body is capped.
        Column(Modifier.align(Alignment.CenterHorizontally).readableWidth()) {

        // ---- Hero (proportional so short phones keep content visible) ----
        val heroHeight = (LocalConfiguration.current.screenHeightDp * 0.34f).dp
            .coerceIn(200.dp, 320.dp)
        Box(
            Modifier
                .fillMaxWidth()
                .height(heroHeight)
                .clickable(onClick = clickSfx { showPhoto = true })
        ) {
            LevelImage(
                url = dishPhotoUrl,
                fallback = dishPhoto,
                contentDescription = data.name,
                modifier = Modifier.fillMaxSize(),
                contentScale = ContentScale.Crop
            )
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.35f),
                            0.55f to Color.Transparent,
                            1f to Color(0xFF2A1608).copy(alpha = 0.92f)
                        )
                    )
            )
            Row(
                Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .padding(14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCircleButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = clickSfx { onBack() }
                )
                Spacer(Modifier.weight(1f))
                HeaderCircleButton(
                    icon = if (isFavorite) Icons.Default.Favorite else Icons.Default.FavoriteBorder,
                    contentDescription = if (isFavorite) "Remove favorite" else "Add to favorites",
                    onClick = clickSfx {
                        isFavorite = FavoritesManager.toggle(ctx, Session.userId, level)
                    }
                )
                Spacer(Modifier.width(10.dp))
                HeaderCircleButton(
                    icon = Icons.Outlined.Settings,
                    contentDescription = "Settings",
                    onClick = clickSfx { onSettings() }
                )
            }
            Column(
                Modifier
                    .align(Alignment.BottomStart)
                    // Bottom clearance covers the stat card's -34dp lift plus a
                    // gap, so the title is never tucked behind it.
                    .padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 54.dp)
            ) {
                Text(
                    "MASTERCLASS SERIES",
                    color = QuestGold,
                    fontFamily = BeVietnamPro,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 3.2.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = QuestGold.copy(alpha = 0.55f),
                            offset = Offset.Zero,
                            blurRadius = 12f
                        )
                    )
                )
                Text(
                    "Mastering ${data.name}",
                    color = Color.White,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 26.sp,
                    style = TextStyle(
                        shadow = Shadow(
                            color = Color.Black.copy(alpha = 0.55f),
                            offset = Offset.Zero,
                            blurRadius = 14f
                        )
                    )
                )
            }
        }

        // Rides up so the stat card laps over the photograph's lower edge
        // rather than sitting in the cream below it.
        Column(
            Modifier
                .offset(y = (-34).dp)
                .padding(horizontal = 16.dp)
        ) {
            // ---- Stat strip (real data only) ----
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color(0xFFFFF8EE),
                border = BorderStroke(1.5.dp, PlayNowBrown.copy(alpha = 0.22f)),
                shadowElevation = 8.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 6.dp)
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .padding(vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    StatCell(
                        "RATING",
                        data.dish.rating,
                        Icons.Default.Star,
                        Modifier.weight(1f)
                    )
                    VerticalStatDivider()
                    StatCell(
                        "TIME",
                        cookingTimeLabel(data.dish.cookingTime),
                        Icons.Default.Schedule,
                        Modifier.weight(1f)
                    )
                    VerticalStatDivider()
                    StatCell(
                        "DIFFICULTY",
                        if (difficulty.equals("Medium", true)) "Med" else difficulty,
                        Icons.Default.AutoAwesome,
                        Modifier.weight(1f)
                    )
                }
            }

            Spacer(Modifier.height(14.dp))

            // ---- Tabs ----
            Surface(
                shape = RoundedCornerShape(50),
                color = Color(0xFFE8D5BC),
                shadowElevation = 2.dp,
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(Modifier.padding(4.dp)) {
                    DetailTab.entries.forEach { t ->
                        val selected = tab == t
                        Surface(
                            shape = RoundedCornerShape(50),
                            color = if (selected) PlayNowBrown else Color.Transparent,
                            shadowElevation = if (selected) 4.dp else 0.dp,
                            modifier = Modifier
                                .weight(1f)
                                .clickable(onClick = clickSfx { tab = t })
                        ) {
                            Text(
                                t.label,
                                color = if (selected) Color.White else DarkBrown,
                                fontFamily = BeVietnamPro,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.ExtraBold,
                                letterSpacing = 0.4.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.padding(vertical = 10.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(14.dp))

            // Preview notice for any dish not yet signed off by our culinary expert.
            if (content?.isValidated == false) {
                Surface(
                    shape = RoundedCornerShape(12.dp),
                    color = PlayNowBrown.copy(alpha = 0.12f),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        "PREVIEW · expert validation in progress",
                        color = PlayNowBrown,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 6.dp)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }

            when (tab) {
                DetailTab.INFORMATION ->
                    InformationTab(data.name, data.region, content, data.dish.reference)
                DetailTab.STEPS -> StepsTab(
                    data = data,
                    trivia = trivia,
                    chosen = triviaChoice,
                    onChoose = { triviaChoice = it },
                    content = content
                )
                DetailTab.EQUIPMENT -> EquipmentTab(content, data.dish.reference)
            }

            if (tab == DetailTab.INFORMATION) {
            // ---- Collectible heritage card ----
            Spacer(Modifier.height(20.dp))
            Text(
                "HERITAGE CARD",
                color = PlayNowBrown,
                fontFamily = BeVietnamPro,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 2.2.sp
            )
            Spacer(Modifier.height(8.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardEdge),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = clickSfx { showCard = true })
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(width = 58.dp, height = 76.dp)
                            .clip(RoundedCornerShape(10.dp))
                            .background(CreamBg)
                    ) {
                        LevelImage(
                            url = data.cardUrl,
                            fallback = data.card,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.width(14.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "History of ${data.name}",
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            color = TextDark,
                            fontSize = 14.sp
                        )
                        Text(
                            heritageTagline(data.dish),
                            color = HintGray,
                            fontFamily = BeVietnamPro,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Medium,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis
                        )
                    }
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PlayNowBrown,
                        shadowElevation = 3.dp
                    ) {
                        Text(
                            "FLIP",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 10.sp,
                            letterSpacing = 1.2.sp,
                            modifier = Modifier.padding(horizontal = 12.dp, vertical = 7.dp)
                        )
                    }
                }
            }

            // ---- Flip through the collection without going back ----
            if (previousLevel != null || nextLevel != null) {
                Spacer(Modifier.height(22.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        "MORE IN YOUR KODEX",
                        color = PlayNowBrown,
                        fontFamily = BeVietnamPro,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.ExtraBold,
                        letterSpacing = 2.2.sp
                    )
                    Spacer(Modifier.weight(1f))
                    Text(
                        "${position + 1} of ${unlockedDishes.size}",
                        color = PlayNowBrown,
                        fontFamily = BeVietnamPro,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.ExtraBold
                    )
                }
                Spacer(Modifier.height(8.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    DishStepButton(
                        level = previousLevel,
                        caption = "Previous",
                        leading = true,
                        modifier = Modifier.weight(1f)
                    ) { previousLevel?.let(onOpenDish) }
                    DishStepButton(
                        level = nextLevel,
                        caption = "Next",
                        leading = false,
                        modifier = Modifier.weight(1f)
                    ) { nextLevel?.let(onOpenDish) }
                }
            }
            }
            Spacer(Modifier.height(20.dp))
        }
    
        }
}

    // ---- Full-bleed dish photo ----
    if (showPhoto) {
        Dialog(onDismissRequest = { showPhoto = false }) {
            Surface(shape = RoundedCornerShape(20.dp), color = Color.Black) {
                LevelImage(
                    url = dishPhotoUrl,
                    fallback = dishPhoto,
                    contentDescription = data.name,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(onClick = clickSfx { showPhoto = false }),
                    contentScale = ContentScale.FillWidth
                )
            }
        }
    }

    // ---- Heritage card, flipping open like a collectible ----
    if (showCard) {
        Dialog(
            onDismissRequest = { showCard = false },
            // A dialog is capped at the platform's default width, which left a
            // tall card rendering postage-stamp small. Going full screen lets
            // it use the whole display, which is the point of a collectible.
            properties = DialogProperties(usePlatformDefaultWidth = false)
        ) {
            val flip = remember { Animatable(90f) }
            LaunchedEffect(Unit) { flip.animateTo(0f, tween(480)) }
            Column(
                Modifier
                    .fillMaxSize()
                    .background(Color.Black.copy(alpha = 0.92f))
                    .clickable(onClick = clickSfx { showCard = false })
                    .padding(horizontal = 12.dp, vertical = 16.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.Center
            ) {
                val ratio = 522f / 924f            // the shape every card is drawn in
                LevelImage(
                    url = data.cardUrl,
                    fallback = data.card,
                    contentDescription = "History of ${data.name}",
                    // Crop, not Fit: a packaged card is already 522x924 so this
                    // changes nothing for the 27, while an uploaded card of any
                    // other shape fills the same frame instead of shrinking
                    // inside it.
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxWidth(0.96f)
                        .weight(1f, fill = false)
                        .aspectRatio(ratio)
                        .clip(RoundedCornerShape(16.dp))
                        .graphicsLayer {
                            rotationY = flip.value
                            cameraDistance = 16f * density
                        }
                        .clickable(onClick = clickSfx { showCard = false })
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tap the card to close",
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp
                )
            }
        }
    }
}

/** Prev/next pager card showing the neighbouring dish's art and name. */
@Composable
private fun DishStepButton(
    level: Int?,
    caption: String,
    leading: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val dish = level?.let { LevelProvider.forLevel(it) }
    val enabled = dish != null
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = if (enabled) Color.White else Color.White.copy(alpha = 0.45f),
        border = BorderStroke(1.dp, CardEdge.copy(alpha = if (enabled) 1f else 0.4f)),
        shadowElevation = if (enabled) 2.dp else 0.dp,
        modifier = modifier.then(if (enabled) Modifier.clickable(onClick = clickSfx(onClick)) else Modifier)
    ) {
        Row(
            Modifier.padding(10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (leading) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = null,
                    tint = if (enabled) PlayNowBrown else HintGray.copy(alpha = 0.4f),
                    modifier = Modifier.size(15.dp)
                )
                Spacer(Modifier.width(6.dp))
            }
            if (dish != null) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(RoundedCornerShape(9.dp))
                        .background(CreamBg)
                ) {
                    LevelImage(
                        url = null,
                        fallback = dish.photo,
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
                Spacer(Modifier.width(8.dp))
            }
            Column(
                Modifier.weight(1f),
                horizontalAlignment = if (leading) Alignment.Start else Alignment.End
            ) {
                Text(
                    caption,
                    color = HintGray,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 1.sp
                )
                Text(
                    dish?.name ?: "The end",
                    color = if (enabled) TextDark else HintGray.copy(alpha = 0.6f),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    maxLines = 1
                )
            }
            if (!leading) {
                Spacer(Modifier.width(6.dp))
                Icon(
                    Icons.AutoMirrored.Filled.ArrowForward,
                    contentDescription = null,
                    tint = if (enabled) PlayNowBrown else HintGray.copy(alpha = 0.4f),
                    modifier = Modifier.size(15.dp)
                )
            }
        }
    }
}

private val CookingTimePart = Regex(
    """(\d+(?:\s*-\s*\d+)?)\s*(hours?|hrs?|minutes?|mins?)""",
    RegexOption.IGNORE_CASE
)

/**
 * The catalog writes cooking time as prose — "45 minutes", "1 Hour",
 * "1 hour & 40 minutes", "5-7 hours" — but a stat cell only gets a third of
 * the row. Fold it to "1h 40m", the numbers large and the suffixes small, so
 * it always sits on one line.
 */
private fun cookingTimeLabel(raw: String): AnnotatedString {
    val parts = CookingTimePart.findAll(raw).toList()
    if (parts.isEmpty()) return AnnotatedString(raw.trim().ifEmpty { "—" })
    return buildAnnotatedString {
        parts.forEachIndexed { i, match ->
            if (i > 0) append(" ")
            append(match.groupValues[1].filterNot { it.isWhitespace() })
            withStyle(SpanStyle(fontSize = 11.sp, fontWeight = FontWeight.Medium)) {
                append(if (match.groupValues[2].startsWith("h", ignoreCase = true)) "h" else "m")
            }
        }
    }
}

/** One-line kicker for the heritage card — taste only, never a place. */
private fun heritageTagline(dish: DishEntry): String {
    val bySlug = when (dish.slug) {
        "adobo" -> "Vinegar, soy, and garlic"
        "sinigang" -> "A sour soup"
        "paksiw" -> "Simmered in vinegar"
        "sisig" -> "Sizzling and citrus-sharp"
        "mechado" -> "A Spanish-style beef stew"
        "menudo" -> "A tomato pork stew"
        "caldereta" -> "A rich meat stew"
        "afritada" -> "A tomato stew"
        "humba" -> "Sweet braised pork"
        "pinikpikan" -> "Smoky chicken with etag"
        "inabraw" -> "Vegetables in bagoong broth"
        "pinuneg" -> "A smoked blood sausage"
        "sinursur" -> "Mashed and cooked in bamboo"
        "binakol" -> "Chicken in young coconut"
        "la_paz_batchoy" -> "A rich noodle soup"
        "inasal" -> "Charcoal-grilled chicken"
        "kansi" -> "A sour beef soup"
        "piaya" -> "A muscovado flatbread"
        "tiyula_itum" -> "A dark toasted-coconut soup"
        "piaparan" -> "Coconut chicken with palapa"
        "pastil" -> "Packed rice with sautéed meat"
        "sinuglaw" -> "Grilled pork and kinilaw"
        "kulma" -> "A peanut curry"
        "satti" -> "Skewers in yellow gravy"
        "pigar_pigar" -> "Hot-oil beef and cabbage"
        "bulalo" -> "Bone-marrow beef soup"
        "chicharon_carcar" -> "Crisp pork rind"
        "lechon" -> "Fiesta roast pig"
        "bicol_express" -> "Coconut and chili pork"
        "laing" -> "Taro leaves in coconut milk"
        else -> null
    }
    if (bySlug != null) return bySlug

    val type = Regex(
        """(?:is an?|is a famous|is widely considered an?)\s+(.+?)(?:\s+(?:of|with|that|whose|which|from|strongly|associated|originating|particularly|commonly|traditionally)\b|[.,])""",
        RegexOption.IGNORE_CASE
    ).find(dish.trivia)?.groupValues?.get(1)
        ?.trim()
        ?.replace(Regex("""^(?:a |an |the |traditional )""", RegexOption.IGNORE_CASE), "")
        ?.trim()
        ?.takeIf { it.length in 6..36 && !it.contains("region", ignoreCase = true) }

    return type?.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase() else ch.toString()
    } ?: "A taste of ${dish.name}"
}

@Composable
private fun StatCell(
    label: String,
    value: String,
    icon: ImageVector,
    modifier: Modifier = Modifier
) = StatCell(label, AnnotatedString(value), icon, modifier)

@Composable
private fun StatCell(
    label: String,
    value: AnnotatedString,
    icon: ImageVector,
    modifier: Modifier = Modifier
) {
    Column(
        modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            label,
            color = PlayNowBrown,
            fontFamily = BeVietnamPro,
            fontSize = 9.sp,
            fontWeight = FontWeight.ExtraBold,
            letterSpacing = 1.4.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(6.dp))
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.Center,
            modifier = Modifier.fillMaxWidth()
        ) {
            Icon(
                icon,
                contentDescription = null,
                tint = PlayNowBrown,
                modifier = Modifier.size(14.dp)
            )
            Spacer(Modifier.width(4.dp))
            Text(
                value,
                color = TextDark,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 17.sp,
                maxLines = 1,
                softWrap = false,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.width(4.dp))
            Spacer(Modifier.size(14.dp))
        }
    }
}

@Composable
private fun VerticalStatDivider() {
    Box(
        Modifier
            .width(1.dp)
            .height(40.dp)
            .background(OutlineDefault.copy(alpha = 0.6f))
    )
}

@Composable
private fun InformationTab(
    dishName: String,
    region: Region,
    content: DishContent?,
    reference: String
) {
    ContentCard {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                "The Origin Story",
                style = MaterialTheme.typography.titleLarge.copy(
                    fontWeight = FontWeight.Bold, color = TextDark
                ),
                modifier = Modifier.weight(1f)
            )
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Image(
                    painter = painterResource(regionIcon(region)),
                    contentDescription = region.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .size(36.dp)
                        .clip(RoundedCornerShape(8.dp))
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    region.displayName,
                    color = PlayNowBrown,
                    fontFamily = BeVietnamPro,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.ExtraBold
                )
            }
        }
        Spacer(Modifier.height(12.dp))
        Text(
            (content?.history ?: "Loading…").withParagraphStops(),
            color = TextDark.copy(alpha = 0.78f),
            fontSize = 13.sp,
            lineHeight = 22.sp,
            // Justified, not centred. Centring suits a caption; across a dozen
            // lines of prose it gives every line a different ragged start, which
            // is what made this block read as misaligned rather than styled.
            textAlign = TextAlign.Justify,
            modifier = Modifier.fillMaxWidth()
        )
    }

    Spacer(Modifier.height(14.dp))
    Text(
        "INGREDIENTS IN ${dishName.uppercase()}",
        color = HintGray,
        fontSize = 12.sp,
        fontWeight = FontWeight.Bold,
        letterSpacing = 2.sp
    )
    Spacer(Modifier.height(8.dp))

    // The sheet interleaves section headers ("For the dough:", "Other Souring
    // Agents for Pork Sinigang:") and bare measurements ("tbsp salt") with the
    // real items. They are not ingredients and must not get a tile - the same
    // filtering the Equipment tab already does for its stray "Optional:" line.
    val ingredients = content?.ingredients.orEmpty().toIngredientList()
    if (ingredients.isEmpty()) {
        PendingCard("The validated ingredient list for this dish is on its way from our culinary experts.")
    } else {
        ingredients.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                modifier = Modifier.padding(bottom = 14.dp)
            ) {
                rowItems.forEach { name ->
                    IngredientTile(name, Modifier.weight(1f))
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    SourceNote(reference)
}

/**
 * One ingredient: a square tile with the caption beneath, per the frame.
 *
 * The dataset ships dish photography but no per-ingredient shots, so the tile
 * is drawn rather than photographed - a warm gradient behind an icon picked
 * from the ingredient's own name. Swapping in real images later means giving
 * this an @DrawableRes and replacing the Box; the caption and grid stay put.
 */
/**
 * Sackcloth behind an ingredient: a warm pool of light where the subject
 * sits, a fine double-diagonal weave, and corners that fall away. Drawn
 * rather than shipped, so it costs no assets and scales with the tile.
 */
private fun Modifier.pantryWeave(): Modifier = drawBehind {
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color(0x16FFD9A0), Color.Transparent),
            center = Offset(size.width * 0.5f, size.height * 0.42f),
            radius = size.minDimension * 0.78f
        )
    )
    // Two passes of thin threads, opposite directions, so it reads as woven
    // cloth rather than hatching.
    val step = size.minDimension / 11f
    val thread = Color(0x0DFFFFFF)
    var x = -size.height
    while (x < size.width + size.height) {
        drawLine(thread, Offset(x, 0f), Offset(x + size.height, size.height), 1f)
        drawLine(thread, Offset(x, size.height), Offset(x + size.height, 0f), 1f)
        x += step
    }
    drawRect(
        Brush.radialGradient(
            colors = listOf(Color.Transparent, Color(0x38000000)),
            center = Offset(size.width / 2f, size.height / 2f),
            radius = size.maxDimension * 0.70f
        )
    )
}

@Composable
private fun IngredientTile(name: String, modifier: Modifier = Modifier) {
    Column(modifier) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1.05f)
                .clip(RoundedCornerShape(16.dp))
                .background(
                    Brush.linearGradient(
                        listOf(Color(0xFF4A2A15), Color(0xFF2A1608))
                    )
                )
                .pantryWeave(),
            contentAlignment = Alignment.Center
        ) {
            // Photography where the dataset's ingredient name matches a shot;
            // the drawn icon stays as the fallback so a newly added or
            // mis-spelled ingredient still renders something sensible.
            val photo = IngredientArt.forName(name)
            if (photo != null) {
                Image(
                    painter = painterResource(photo),
                    contentDescription = name,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(10.dp)
                )
            } else {
                Icon(
                    ingredientIcon(name),
                    contentDescription = null,
                    tint = LightOrange.copy(alpha = 0.92f),
                    modifier = Modifier.size(46.dp)
                )
            }
        }
        Spacer(Modifier.height(7.dp))
        Text(
            name.uppercase(),
            color = HintGray,
            fontSize = 9.sp,
            fontWeight = FontWeight.Bold,
            letterSpacing = 1.sp,
            lineHeight = 13.sp
        )
    }
}

/**
 * True when a dataset ingredient line is an actual ingredient.
 *
 * Mirrors the rule the art generator uses, so what the screen lists and what
 * has photography stay in step.
 */
/**
 * The ingredient lines worth showing, in sheet order.
 *
 * Two problems, both from the source spreadsheet. It interleaves section
 * headers and bare measurements with the real items, and some dishes list the
 * same ingredient twice - Satti names salt and sugar under two sub-recipes,
 * Chicharon Carcar repeats salt. Neither should reach the grid, so this
 * filters and then de-duplicates case-insensitively, keeping the first
 * spelling the sheet used.
 */
internal fun List<String>.toIngredientList(): List<String> {
    val seen = mutableSetOf<String>()
    return filter { it.isIngredient() }
        .filter { seen.add(it.trim().lowercase()) }
}

internal fun String.isIngredient(): Boolean {
    val t = trim()
    if (t.isEmpty() || t.endsWith(":")) return false
    if (t.equals("yellow", ignoreCase = true)) return false
    // A leading number is a quantity line, not an item.
    if (t.first().isDigit()) return false
    val firstWord = t.substringBefore(' ').lowercase().trimEnd('.', ',')
    return firstWord !in setOf(
        "for", "tbsp", "tsp", "cup", "cups", "kg", "g", "ml", "pc", "pcs"
    )
}

/** Best-guess pictogram for an ingredient, from words in its name. */
private fun ingredientIcon(name: String): ImageVector {
    val n = name.lowercase()
    return when {
        listOf("water", "vinegar", "sauce", "milk", "oil", "broth", "stock", "juice")
            .any { it in n } -> Icons.Default.LocalDrink
        listOf("fish", "shrimp", "bagoong", "crab", "squid", "bangus", "tuna")
            .any { it in n } -> Icons.Default.SetMeal
        listOf("rice", "peanut", "corn", "flour", "sugar", "salt", "pepper", "bean")
            .any { it in n } -> Icons.Default.Grain
        listOf("leaf", "leaves", "bok", "pechay", "kangkong", "sitaw", "eggplant",
               "okra", "onion", "garlic", "tomato", "ginger", "vegetable", "malunggay")
            .any { it in n } -> Icons.Default.Eco
        else -> Icons.Default.RestaurantMenu
    }
}

@Composable
private fun StepsTab(
    data: LevelData,
    trivia: TriviaQuestion?,
    chosen: Int?,
    onChoose: (Int) -> Unit,
    content: DishContent?
) {
    val steps = content?.procedure.orEmpty()
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Cooking Steps",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDark),
            modifier = Modifier.weight(1f)
        )
        if (steps.isNotEmpty()) {
            Text("${steps.size} Steps Total", color = PlayNowBrown, fontFamily = BeVietnamPro, fontSize = 11.sp, fontWeight = FontWeight.ExtraBold)
        }
    }
    Spacer(Modifier.height(10.dp))

    if (steps.isEmpty()) {
        PendingCard("The expert-validated cooking procedure will appear here once curation is complete.")
    } else {
        steps.forEachIndexed { i, step ->
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = Color.White,
                border = BorderStroke(1.dp, CardEdge),
                shadowElevation = 2.dp,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(bottom = 10.dp)
            ) {
                Column(Modifier.padding(14.dp)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            String.format("%02d", i + 1),
                            color = PlayNowBrown,
                            fontWeight = FontWeight.Black,
                            fontSize = 15.sp
                        )
                        Spacer(Modifier.width(10.dp))
                        Text(
                            step.title,
                            fontWeight = FontWeight.Bold,
                            color = TextDark,
                            fontSize = 14.sp,
                            lineHeight = 18.sp
                        )
                    }
                    Spacer(Modifier.height(6.dp))
                    Text(step.description, color = HintGray, fontSize = 12.sp, lineHeight = 18.sp)
                }
            }
        }
    }

    if (trivia != null) {
        Spacer(Modifier.height(12.dp))
        TriviaCard(
            question = trivia,
            chosen = chosen,
            onChoose = onChoose,
            footnote = "${trivia.options[trivia.correctIndex]} goes into ${data.name}."
        )
    }
    SourceNote(data.dish.reference)
}

/** Multiple-choice question built from the dish dataset. */
private data class TriviaQuestion(
    val prompt: String,
    val options: List<String>,
    val correctIndex: Int
)

/**
 * Asks which ingredient belongs to this dish. Distractors are drawn from
 * other dishes and filtered against this dish's own list, so a "wrong"
 * option is never quietly also correct.
 */
private fun triviaFor(level: Int, dish: DishEntry): TriviaQuestion? {
    val own = dish.ingredients.toIngredientList()
    if (own.isEmpty()) return null

    val ownKeys = own.map { it.lowercase() }.toSet()
    val random = Random(level)
    val answer = own.random(random)
    val distractors = DishCatalog.all
        .asSequence()
        .filter { it.slug != dish.slug }
        .flatMap { it.ingredients.toIngredientList().asSequence() }
        .filter { it.lowercase() !in ownKeys }
        .distinctBy { it.lowercase() }
        .toList()
        .shuffled(random)
        .take(3)
    if (distractors.size < 3) return null

    val options = (distractors + answer).shuffled(random).map { it.triviaOptionLabel() }
    val labeled = answer.triviaOptionLabel()
    return TriviaQuestion(
        prompt = "Which Ingredient Goes Into ${dish.name}?",
        options = options,
        correctIndex = options.indexOf(labeled)
    )
}

/** Title-case each word and drop a trailing colon from sheet headers. */
private fun String.triviaOptionLabel(): String =
    trim().trimEnd(':').trim()
        .split(Regex("\\s+"))
        .filter { it.isNotBlank() }
        .joinToString(" ") { word ->
            word.split('-').joinToString("-") { part ->
                part.lowercase().replaceFirstChar { ch ->
                    if (ch.isLowerCase()) ch.titlecase() else ch.toString()
                }
            }
        }

@Composable
private fun TriviaMark(
    letter: String,
    answered: Boolean,
    correct: Boolean,
    picked: Boolean,
    markGreen: Color,
    markWrong: Color
) {
    val showResult = answered && (correct || picked)
    Box(
        Modifier
            .size(28.dp)
            .shadow(if (showResult) 2.dp else 0.dp, CircleShape)
            .clip(CircleShape)
            .background(
                when {
                    showResult -> Color(0xFFFFFBF3)
                    else -> Color.White.copy(alpha = 0.16f)
                }
            ),
        contentAlignment = Alignment.Center
    ) {
        when {
            answered && correct -> Icon(
                Icons.Default.Check,
                contentDescription = "Correct answer",
                tint = markGreen,
                modifier = Modifier.size(18.dp)
            )
            picked -> Icon(
                Icons.Default.Close,
                contentDescription = "Wrong answer",
                tint = markWrong,
                modifier = Modifier.size(18.dp)
            )
            else -> Text(
                letter,
                color = Color.White,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
    }
}

@Composable
private fun TriviaCard(
    question: TriviaQuestion,
    chosen: Int?,
    onChoose: (Int) -> Unit,
    footnote: String
) {
    Surface(shape = RoundedCornerShape(22.dp), modifier = Modifier.fillMaxWidth()) {
        Column(
            Modifier
                .background(
                    Brush.verticalGradient(listOf(Color(0xFF9A4A15), Color(0xFF6B2E0C)))
                )
                .padding(18.dp)
        ) {
            Text(
                "TRIVIA QUESTION",
                color = LightOrange.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 2.sp
            )
            Spacer(Modifier.height(8.dp))
            Text(
                question.prompt,
                color = Color.White,
                fontSize = 17.sp,
                fontWeight = FontWeight.ExtraBold,
                lineHeight = 24.sp
            )
            Spacer(Modifier.height(14.dp))

            question.options.forEachIndexed { index, option ->
                val isCorrect = index == question.correctIndex
                val picked = chosen == index
                val answered = chosen != null
                val markGreen = Color(0xFF2F7A38)
                val markWrong = Color(0xFF8B2E1A)
                Surface(
                    shape = RoundedCornerShape(16.dp),
                    color = when {
                        answered && isCorrect -> markGreen
                        picked -> markWrong
                        else -> Color.White.copy(alpha = 0.10f)
                    },
                    border = when {
                        answered && isCorrect -> BorderStroke(1.5.dp, Color.White.copy(alpha = 0.35f))
                        picked -> BorderStroke(1.5.dp, Color.White.copy(alpha = 0.22f))
                        else -> null
                    },
                    shadowElevation = if (answered && (isCorrect || picked)) 3.dp else 0.dp,
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(bottom = 8.dp)
                        .alpha(if (answered && !isCorrect && !picked) 0.48f else 1f)
                        .clickable(enabled = !answered) { onChoose(index) }
                ) {
                    Row(
                        Modifier.padding(horizontal = 12.dp, vertical = 11.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        TriviaMark(
                            letter = ('A' + index).toString(),
                            answered = answered,
                            correct = isCorrect,
                            picked = picked,
                            markGreen = markGreen,
                            markWrong = markWrong
                        )
                        Spacer(Modifier.width(12.dp))
                        Text(
                            option,
                            color = Color.White,
                            fontSize = 13.sp,
                            fontWeight = if (answered && isCorrect) FontWeight.Bold else FontWeight.Medium,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            }

            if (chosen != null) {
                Spacer(Modifier.height(4.dp))
                Row(verticalAlignment = Alignment.Top) {
                    Icon(
                        Icons.Default.Lightbulb,
                        contentDescription = null,
                        tint = LightOrange,
                        modifier = Modifier.size(14.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        footnote,
                        color = LightOrange.copy(alpha = 0.9f),
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun EquipmentTab(content: DishContent?, reference: String) {
    // The sheet lists "Optional:" as its own line before the tool it applies
    // to, which would otherwise render as a nameless card.
    val tools = content?.tools.orEmpty().filter { it.name.toolLabel().isNotBlank() }
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "Kitchen Equipment",
            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold, color = TextDark),
            modifier = Modifier.weight(1f)
        )
        if (tools.isNotEmpty()) {
            Text("${tools.size} Essential Tools", color = PlayNowBrown, fontSize = 11.sp, fontWeight = FontWeight.SemiBold)
        }
    }
    Spacer(Modifier.height(10.dp))

    if (tools.isEmpty()) {
        PendingCard("Traditional tools and equipment for this dish are being documented with our experts.")
    } else {
        tools.chunked(2).forEach { rowItems ->
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                modifier = Modifier
                    .height(IntrinsicSize.Min)
                    .padding(bottom = 10.dp)
            ) {
                rowItems.forEach { tool ->
                    ToolCard(
                        rawName = tool.name,
                        description = tool.description,
                        modifier = Modifier
                            .weight(1f)
                            .fillMaxHeight()
                    )
                }
                if (rowItems.size == 1) Spacer(Modifier.weight(1f))
            }
        }
    }
    SourceNote(reference)
}

@Composable
private fun ToolCard(rawName: String, description: String, modifier: Modifier = Modifier) {
    val optional = rawName.trim().startsWith("Optional", ignoreCase = true)
    val name = rawName.toolLabel()
    val profile = toolProfile(name)
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardEdge),
        shadowElevation = 2.dp,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // The art is the thing worth looking at on this card, and the
            // uploads are now trimmed to their subject rather than sitting on a
            // 1920x1080 canvas, so the extra room goes to the picture instead
            // of to the empty margin that used to come with it.
            Box(
                Modifier.size(108.dp),
                contentAlignment = Alignment.Center
            ) {
                // Three sources, in order of how much they know about this tool:
                //
                //  1. the picture an admin uploaded in the panel, which is the
                //     only one that can know about a tool added after the build
                //  2. artwork packaged in the APK, matched on the dataset's name
                //  3. an icon matched on a keyword in the name
                //
                // The packaged art stays behind the upload rather than being
                // replaced by it, so the screen still looks right offline and on
                // first run, before EquipmentSync has fetched anything.
                val uploaded = EquipmentSync.urlForName(name)
                val art = EquipmentArt.forName(name)

                // One source only, never stacked.
                //
                // These were briefly layered so an upload could draw over the
                // packaged art and leave something behind it if the fetch failed.
                // The uploads have transparent backgrounds, so what actually
                // showed was the icon bleeding through the picture on top of it -
                // a padlock-orange pair of scissors sitting behind the dough
                // cutter. Whichever source is best wins the tile outright.
                when {
                    uploaded != null -> LevelImage(
                        url = uploaded,
                        // Packaged art if this build has any for the tool, and
                        // otherwise nothing: a blank tile under a labelled card
                        // reads better than an unrelated picture.
                        fallback = art ?: R.drawable.transparent_tile,
                        contentDescription = name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    art != null -> Image(
                        painter = painterResource(art),
                        contentDescription = name,
                        contentScale = ContentScale.Fit,
                        modifier = Modifier.fillMaxSize()
                    )
                    else -> Icon(
                        profile.icon,
                        contentDescription = null,
                        tint = PlayNowBrown,
                        modifier = Modifier.size(48.dp)
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            if (optional) {
                Surface(shape = RoundedCornerShape(6.dp), color = PlayNowBrown.copy(alpha = 0.12f)) {
                    Text(
                        "OPTIONAL",
                        color = PlayNowBrown,
                        fontSize = 8.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
                    )
                }
                Spacer(Modifier.height(5.dp))
            }
            Text(
                name,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                fontSize = 13.sp,
                lineHeight = 17.sp,
                textAlign = TextAlign.Center
            )
            Spacer(Modifier.height(4.dp))
            Text(
                description.ifBlank { profile.blurb },
                color = HintGray,
                fontSize = 10.sp,
                lineHeight = 14.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** Drops the sheet's "Optional:" prefix so the card shows the tool itself. */
private fun String.toolLabel(): String = trim()
    .removePrefix("Optional:")
    .removePrefix("optional:")
    .trim()

private data class ToolProfile(val icon: ImageVector, val blurb: String)

/**
 * The dataset names tools but does not describe them, so the icon and the
 * one-liner are matched from the name.
 */
private fun toolProfile(name: String): ToolProfile {
    val n = name.lowercase()
    return when {
        "mortar" in n || "processor" in n -> ToolProfile(
            Icons.Default.Blender,
            "For pounding garlic, ginger and spices into a paste."
        )
        "pressure cooker" in n -> ToolProfile(
            Icons.Default.Timer,
            "Cuts the braising time on tough cuts of meat."
        )
        "grill" in n || "barbecue" in n -> ToolProfile(
            Icons.Default.OutdoorGrill,
            "Gives the meat its char and smoke over live coals."
        )
        "hearth" in n || "wood-fired" in n || "firewood" in n -> ToolProfile(
            Icons.Default.LocalFireDepartment,
            "The open flame this dish is traditionally cooked over."
        )
        "sizzling" in n -> ToolProfile(
            Icons.Default.LocalFireDepartment,
            "Served straight from the heat, still crackling."
        )
        "strainer" in n || "colander" in n -> ToolProfile(
            Icons.Default.FilterAlt,
            "Drains the broth and rinses off excess fat."
        )
        "knife" in n || "cleaver" in n || "cutter" in n -> ToolProfile(
            Icons.Default.ContentCut,
            "A sharp blade for precision cutting of meat and vegetables."
        )
        "chopping" in n || "board" in n -> ToolProfile(
            Icons.Default.Restaurant,
            "A solid surface for prepping aromatics and garnishes."
        )
        "rolling pin" in n -> ToolProfile(
            Icons.Default.Straighten,
            "Rolls the dough out to an even thickness."
        )
        "measuring" in n -> ToolProfile(
            Icons.Default.Straighten,
            "Keeps the proportions exact from batch to batch."
        )
        "brush" in n -> ToolProfile(
            Icons.Default.Brush,
            "Bastes the marinade on while it cooks."
        )
        "bamboo" in n || "leaves" in n || "stick" in n -> ToolProfile(
            Icons.Default.Park,
            "Traditional cookware gathered fresh, not bought."
        )
        "tongs" in n -> ToolProfile(
            Icons.Default.Handyman,
            "Handles the hot pieces without piercing them."
        )
        "bowl" in n -> ToolProfile(
            Icons.Default.RamenDining,
            "For mixing and marinating before it hits the heat."
        )
        "pan" in n || "skillet" in n || "kawali" in n || "griddle" in n -> ToolProfile(
            Icons.Default.DinnerDining,
            "For searing and frying over direct heat."
        )
        "pot" in n || "kaldero" in n || "palayok" in n -> ToolProfile(
            Icons.Default.SoupKitchen,
            "A deep pot for slow-simmering the stew until tender."
        )
        else -> ToolProfile(
            Icons.Default.RestaurantMenu,
            "Part of the traditional setup for this dish."
        )
    }
}

@Composable
private fun ContentCard(
    bottomPadding: androidx.compose.ui.unit.Dp = 0.dp,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        border = BorderStroke(1.dp, CardEdge),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = bottomPadding)
    ) {
        Column(Modifier.padding(16.dp), content = content)
    }
}

@Composable
private fun PendingCard(message: String) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White.copy(alpha = 0.7f),
        border = BorderStroke(1.dp, CardEdge.copy(alpha = 0.6f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(
            Modifier.padding(20.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                Icons.Default.RestaurantMenu,
                contentDescription = null,
                tint = HintGray,
                modifier = Modifier.size(26.dp)
            )
            Spacer(Modifier.height(8.dp))
            Text(
                message,
                color = HintGray,
                fontSize = 12.sp,
                lineHeight = 18.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/** The chef who reviewed the dish write-ups. Named once, used everywhere. */
private const val VALIDATOR = "Chef Karen Nina B. Gicana"

/**
 * The catalog often drops the last stop on a paragraph. Put it back.
 */
private fun String.withParagraphStops(): String =
    split(Regex("""\n[ \t]*\n""")).joinToString("\n\n") { block ->
        // Fold the single newlines inside a block.
        //
        // Stories are pasted into the panel already wrapped at whatever width
        // the source had, so the text arrives with a hard break every hundred
        // characters or so. HTML folds those away, which is why the panel reads
        // correctly and this screen did not: Compose honours every one, leaving
        // the paragraph breaking mid-sentence - "...because its" and then a new
        // line for "traditional filling is muscovado sugar". A blank line is
        // still a paragraph break, since that is a break the author meant.
        val t = block.replace(Regex("""\s+"""), " ").trim()
        if (t.isEmpty() || t.last() in ".!?…") t else "$t."
    }

/**
 * Splits a dataset reference blob into individual citations.
 *
 * The sheet separates entries with a blank line and leaves ragged whitespace
 * behind, so the display trims rather than the data being rewritten.
 */
private fun String.toCitations(): List<String> =
    // Blank-line separated, but "blank" in the sheet often means a line of
    // spaces — splitting on a literal "\n\n" silently welds two citations
    // into one wherever that happens.
    split(Regex("""\n[ \t]*\n"""))
        .map { it.replace(Regex("""\s+"""), " ").trim() }
        .filter { it.isNotBlank() }

/**
 * The quiet footer under a dish tab: who checked the content, and what it was
 * drawn from.
 *
 * Collapsed by default. The validation line is the part that earns its place
 * on screen; the citations are there to be found, not read, so they stay
 * behind a tap rather than pushing the cooking steps up the page.
 */
@Composable
private fun SourceNote(reference: String) {
    val citations = remember(reference) { reference.toCitations() }
    if (citations.isEmpty()) return
    var open by remember(reference) { mutableStateOf(false) }

    Column(
        Modifier
            .fillMaxWidth()
            .padding(top = 14.dp, bottom = 4.dp)
    ) {
        HorizontalDivider(color = OutlineDefault.copy(alpha = 0.45f))
        Row(
            Modifier
                .fillMaxWidth()
                .clickable(onClick = clickSfx { open = !open })
                .padding(vertical = 10.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.Verified,
                contentDescription = null,
                tint = ValidatedGreen,
                modifier = Modifier.size(13.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                "Validated by $VALIDATOR",
                color = HintGray,
                fontSize = 11.sp,
                lineHeight = 15.sp,
                modifier = Modifier.weight(1f)
            )
            Text(
                if (open) "Hide Sources" else "${citations.size} Sources",
                color = HintGray,
                fontSize = 11.sp,
                fontWeight = FontWeight.SemiBold
            )
            Icon(
                if (open) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                contentDescription = null,
                tint = HintGray,
                modifier = Modifier.size(15.dp)
            )
        }
        if (open) {
            Column(Modifier.padding(bottom = 8.dp)) {
                citations.forEach { citation ->
                    Text(
                        citation,
                        color = HintGray,
                        fontSize = 10.sp,
                        lineHeight = 14.sp,
                        modifier = Modifier.padding(bottom = 6.dp)
                    )
                }
            }
        }
    }
}
