package com.example.kusinakode.ui.explore

import com.example.kusinakode.ui.components.clickSfx
import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.BottomNavTab
import com.example.kusinakode.KusinaBottomNav
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.R
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.ui.theme.CardSurface
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.OutlineDefault
import com.example.kusinakode.ui.theme.ProgressFillEnd
import com.example.kusinakode.ui.theme.ProgressFillStart
import com.example.kusinakode.ui.theme.ProgressGoldEnd
import com.example.kusinakode.ui.theme.ProgressGoldStart
import com.example.kusinakode.ui.theme.ProgressTrackIdle
import com.example.kusinakode.ui.theme.RegionChipInk
import com.example.kusinakode.ui.theme.RegionChipOff
import com.example.kusinakode.ui.theme.RegionChipOffStroke
import com.example.kusinakode.ui.theme.RegionChipOn
import com.example.kusinakode.ui.theme.SuccessGreen
import kotlin.math.hypot
import com.example.kusinakode.ui.home.ChromeCream
import com.example.kusinakode.ui.home.ChromeInk
import com.example.kusinakode.ui.home.ChromeStroke
import com.example.kusinakode.ui.home.regionIcon
import com.example.kusinakode.ui.components.LevelImage

private val BurntOrange = Color(0xFFCC6B1F)
private val SeaBlue = Color(0xFF5F8A99)
private val CreamBg = Color(0xFFF1E6D2)
private val TextDark = Color(0xFF3E2723)
/** Marks conquered ground on the map — completed regions and sailed trail legs. */
private val GoldAccent = Color(0xFFE8B34A)

/** Font-size knobs — change these to scale the map labels. */
private val MapProgressTitleSize = 17.sp
private val UpNextSize = 12.sp

/** Visayas Kitchen sheet: longer handle, tighter gap, smaller dish thumbs. */
private val SheetHandleWidth = 67.dp
private val SheetHandleTopPad = 10.dp
private val SheetHandleBottomPad = 20.dp
private val LevelThumbSize = 40.dp

private data class RegionLevel(
    val number: Int,
    val name: String,
    val region: Region,
    val isSolved: Boolean,
    val isUnlocked: Boolean,
    @DrawableRes val photo: Int,
    /** Set only for a level the admin panel added; its art is on the server. */
    val photoUrl: String? = null
)

/**
 * Explore tab: clickable Philippine map — tap a region pin (or chip) for its
 * dish levels. Region assignments are placeholders until Module 4's
 * validated dataset lands; the map replaces the old numbered level grid.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun ExploreScreen(
    unlockedUpTo: Int,
    completedUpTo: Int,
    /** Levels with a half-finished board, so the CTA can offer to resume. */
    resumableLevels: Set<Int> = emptySet(),
    onPlayLevel: (Int) -> Unit,
    onViewDish: (Int) -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onLeaderboard: () -> Unit,
    onLearn: () -> Unit,
    onWallet: () -> Unit = {},
    onSettings: () -> Unit = {},
    initialRegion: Region? = null
) {
    val totalLevels = LevelProvider.levelCount
    val allLevels = remember(unlockedUpTo, completedUpTo) {
        (1..totalLevels).map { n ->
            val data = LevelProvider.forLevel(n)
            RegionLevel(
                number = n,
                name = data.name,
                region = data.region,
                isSolved = n <= completedUpTo,
                isUnlocked = n <= unlockedUpTo,
                photo = data.photo,
                photoUrl = data.photoUrl
            )
        }
    }
    val regionsExplored = Region.entries.count { r ->
        allLevels.any { it.region == r && it.isSolved }
    }

    var selectedRegion by remember(initialRegion) { mutableStateOf(initialRegion) }
    var sheetRegion by remember { mutableStateOf<Region?>(null) }
    var query by remember { mutableStateOf("") }
    LaunchedEffect(initialRegion) { selectedRegion = initialRegion }

    // Drilling into a region is a step within Explore, so going back undoes
    // that step first and only leaves the screen from the whole-map view.
    val goBack: () -> Unit = {
        if (selectedRegion != null) selectedRegion = null else onHome()
    }
    BackHandler(enabled = selectedRegion != null) { selectedRegion = null }

    Scaffold(
        containerColor = CreamBg,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Levels,
                onHome = onHome,
                onProfile = onProfile,
                onLevels = { /* already here */ },
                onWallet = onWallet,
                onCompleted = onLearn
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
        ) {
            // ---- Gradient header ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(bottomStart = 0.dp, bottomEnd = 0.dp))
                    .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
            ) {
                Row(
                    Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(start = 16.dp, end = 16.dp, top = 14.dp, bottom = 22.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChromeCream)
                            .border(1.dp, ChromeStroke, CircleShape)
                            .clickable(onClick = goBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription =
                                if (selectedRegion != null) "Back to the whole map" else "Back",
                            tint = ChromeInk,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Explore the Philippines",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "$regionsExplored of ${Region.entries.size} regions explored",
                            color = LightOrange.copy(alpha = 0.92f),
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Box(
                        Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(ChromeCream)
                            .border(1.dp, ChromeStroke, CircleShape)
                            .clickable { onSettings() },
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

            // Only the body scrolls — the header stays put so the title never
            // slides under the status bar.
            Column(
                Modifier
                    .verticalScroll(rememberScrollState())
                    .padding(16.dp)
            ) {
                // ---- Search (matches solved dishes only — no spoilers) ----
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = { Text("Search a dish you've solved…", color = HintGray.copy(alpha = 0.7f)) },
                    leadingIcon = { Icon(Icons.Default.Search, contentDescription = null, tint = HintGray, modifier = Modifier.padding(start = 9.dp)) },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = BurntOrange,
                        unfocusedBorderColor = OutlineDefault,
                        cursorColor = DarkBrown
                    )
                )

                if (query.isNotBlank()) {
                    val matches = allLevels.filter {
                        it.isSolved && it.name.contains(query.trim(), ignoreCase = true)
                    }
                    Spacer(Modifier.height(10.dp))
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        border = BorderStroke(1.dp, OutlineDefault.copy(alpha = 0.6f)),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Column(Modifier.padding(6.dp)) {
                            if (matches.isEmpty()) {
                                Text(
                                    "No solved dishes match — solve more to fill your map!",
                                    color = HintGray,
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp)
                                )
                            } else {
                                matches.forEach { lvl ->
                                    LevelRow(lvl, onPlayLevel, onViewDish)
                                }
                            }
                        }
                    }
                }

                Spacer(Modifier.height(12.dp))

                // ---- Region chips (scroll sideways on narrow phones) ----
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.horizontalScroll(rememberScrollState())
                ) {
                    RegionChip("All", selectedRegion == null) { selectedRegion = null }
                    Region.entries.forEach { r ->
                        RegionChip(r.displayName, selectedRegion == r) { selectedRegion = r }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // The dish the player is being pushed toward. Declared here
                // rather than at the Play button because the map marks that
                // region as the objective too.
                val nextLevel = (completedUpTo + 1).coerceAtMost(totalLevels)
                val nextRegion = allLevels.firstOrNull { it.number == nextLevel }?.region

                // ---- Map card ----
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(0.74f)
                        .clip(RoundedCornerShape(26.dp))
                        .background(
                            Brush.verticalGradient(
                                listOf(
                                    Color(0xFF6E9AA8),
                                    SeaBlue,
                                    Color(0xFF3F6675)
                                )
                            )
                        )
                ) {
                    // Open water: graticule + depth contours + a slow drifting
                    // sheen, so the sea reads as a charted ocean, not a backdrop.
                    OceanBackdrop(Modifier.matchParentSize())

                    // The chart is the island art itself: the whole archipelago
                    // until a region is picked, then that region's own map.
                    // It fills the whole card rather than an inner square, so
                    // the drawing's own sea IS the card's sea - no band of a
                    // different blue along the top and bottom. Trail and pins
                    // draw over it.
                    Crossfade(
                        targetState = selectedRegion,
                        animationSpec = tween(420),
                        label = "map_region",
                        modifier = Modifier.matchParentSize()
                    ) { region ->
                        Image(
                            painter = painterResource(
                                region?.let { regionIcon(it) }
                                    ?: R.drawable.region_philippines
                            ),
                            contentDescription = region?.displayName
                                ?: "Map of the Philippines",
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }

                    // Geographic silhouette sits in a centered square so pin
                    // fractions track the actual landmasses.
                    BoxWithConstraints(
                        Modifier
                            .fillMaxWidth()
                            .aspectRatio(1f)
                            .align(Alignment.Center)
                    ) {
                        val mw = maxWidth
                        val mh = maxHeight
                        val pinSpots = listOf(
                            Triple(Region.LUZON, 0.42f, 0.20f),
                            Triple(Region.VISAYAS, 0.58f, 0.56f),
                            Triple(Region.MINDANAO, 0.66f, 0.76f)
                        )

                        // Culinary trail: the leg you've already sailed is solid
                        // gold, the leg ahead stays dashed.
                        val trailProgress = Region.entries.count { r ->
                            allLevels.any { it.region == r && it.isSolved }
                        }
                        // The trail joins the three regions, so it only means
                        // anything on the full chart.
                        if (selectedRegion == null) Canvas(Modifier.fillMaxSize()) {
                            val pts = pinSpots.map { (_, fx, fy) ->
                                Offset(size.width * fx, size.height * fy)
                            }
                            val stroke = size.width * 0.006f
                            for (i in 1 until pts.size) {
                                val a = pts[i - 1]
                                val b = pts[i]
                                val dx = b.x - a.x
                                val dy = b.y - a.y
                                val len = hypot(dx, dy)
                                // Bow each leg west into open water; a straight
                                // line just cuts across the islands.
                                val ctrl = Offset(
                                    (a.x + b.x) / 2f - dy / len * len * 0.22f,
                                    (a.y + b.y) / 2f + dx / len * len * 0.22f
                                )
                                val leg = Path().apply {
                                    moveTo(a.x, a.y)
                                    quadraticBezierTo(ctrl.x, ctrl.y, b.x, b.y)
                                }
                                val sailed = i < trailProgress
                                val tint = if (sailed) GoldAccent else Color.White

                                // Soft underlay reads as a glow around the route.
                                drawPath(
                                    leg,
                                    tint.copy(alpha = if (sailed) 0.28f else 0.12f),
                                    style = Stroke(width = stroke * 3.2f, cap = StrokeCap.Round)
                                )
                                drawPath(
                                    leg,
                                    tint.copy(alpha = if (sailed) 0.95f else 0.5f),
                                    style = Stroke(
                                        width = if (sailed) stroke * 1.4f else stroke,
                                        cap = StrokeCap.Round,
                                        pathEffect = if (sailed) null
                                        else PathEffect.dashPathEffect(floatArrayOf(11f, 13f))
                                    )
                                )
                            }
                            // Waypoint beads mark each landfall.
                            pts.forEachIndexed { i, p ->
                                val reached = i < trailProgress
                                drawCircle(
                                    color = if (reached) GoldAccent else Color.White.copy(alpha = 0.6f),
                                    radius = stroke * 1.6f,
                                    center = p
                                )
                            }
                        }

                        if (selectedRegion == null) pinSpots.forEach { (region, fx, fy) ->
                            val inRegion = allLevels.filter { it.region == region }
                            val solvedHere = inRegion.count { it.isSolved }
                            RegionPin(
                                region = region,
                                selected = selectedRegion == region,
                                solvedCount = solvedHere,
                                totalCount = inRegion.size,
                                isNext = region == nextRegion,
                                modifier = Modifier.offset(x = mw * fx - 30.dp, y = mh * fy - 46.dp)
                            ) { selectedRegion = region }
                        }
                    }

                    // Vignette: darkens the card edges so the archipelago sits
                    // in the light. Drawn above the sea, below the UI cards.
                    Box(
                        Modifier
                            .matchParentSize()
                            .background(
                                Brush.radialGradient(
                                    colors = listOf(
                                        Color.Transparent,
                                        Color.Transparent,
                                        Color(0xFF1E3742).copy(alpha = 0.38f)
                                    ),
                                    radius = 900f
                                )
                            )
                    )

                    // Overall progress and region detail share one slot at the
                    // bottom, so a selected pin is never hidden behind its own
                    // popup — which is what happened when this sat on the map.
                    Crossfade(
                        targetState = selectedRegion,
                        animationSpec = tween(220),
                        label = "map_info",
                        modifier = Modifier
                            .align(Alignment.BottomCenter)
                            .padding(12.dp)
                            .fillMaxWidth()
                    ) { region ->
                        if (region == null) {
                            MapProgressCard(completedUpTo, totalLevels, onLearn)
                        } else {
                            RegionPanel(
                                region = region,
                                solved = allLevels.count { it.region == region && it.isSolved },
                                total = allLevels.count { it.region == region },
                                onExplore = { sheetRegion = region }
                            )
                        }
                    }
                }

                Spacer(Modifier.height(14.dp))

                // ---- Jump straight into the next dish ----
                // An abandoned board takes priority over the label: this
                // button drops you back into that exact round, so calling it
                // "Play" would misdescribe what happens.
                val canResume = nextLevel in resumableLevels
                Button(
                    onClick = clickSfx { onPlayLevel(nextLevel) },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = HeaderTop,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(26.dp),
                    elevation = ButtonDefaults.buttonElevation(defaultElevation = 6.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(54.dp)
                ) {
                    Icon(
                        if (canResume) Icons.Default.PlayCircleOutline else Icons.Default.PlayArrow,
                        contentDescription = null,
                        modifier = Modifier.size(20.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Text(
                        when {
                            canResume -> "CONTINUE · LEVEL $nextLevel"
                            completedUpTo >= totalLevels -> "PLAY AGAIN · LEVEL $nextLevel"
                            else -> "PLAY NOW · LEVEL $nextLevel"
                        },
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 1.sp
                    )
                }
                nextRegion?.let { region ->
                    Spacer(Modifier.height(10.dp))
                    Text(
                        when {
                            canResume -> "Pick up where you left off in ${region.displayName}"
                            completedUpTo == 0 -> "Guess the first dish"
                            else -> "Guess the next dish"
                        },
                        color = HintGray,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.Normal,
                        fontSize = UpNextSize,
                        modifier = Modifier.align(Alignment.CenterHorizontally)
                    )
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }

    // ---- Region levels sheet ----
    sheetRegion?.let { region ->
        val regionLevels = allLevels.filter { it.region == region }
        ModalBottomSheet(
            onDismissRequest = { sheetRegion = null },
            containerColor = CreamBg,
            dragHandle = {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .padding(top = SheetHandleTopPad, bottom = SheetHandleBottomPad),
                    contentAlignment = Alignment.Center
                ) {
                    Box(
                        Modifier
                            .width(SheetHandleWidth)
                            .height(4.dp)
                            .clip(CircleShape)
                            .background(HintGray.copy(alpha = 0.45f))
                    )
                }
            }
        ) {
            Column(Modifier.padding(horizontal = 16.dp)) {
                Text(
                    "${region.displayName} Kitchen",
                    style = MaterialTheme.typography.titleLarge.copy(
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark
                    )
                )
                Text(region.tagline, color = HintGray, fontSize = 12.sp)
                Spacer(Modifier.height(12.dp))
                LazyColumn(
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    modifier = Modifier.padding(bottom = 24.dp)
                ) {
                    items(regionLevels) { lvl ->
                        Surface(shape = RoundedCornerShape(16.dp), color = Color.White) {
                            LevelRow(lvl, onPlayLevel, onViewDish) {
                                sheetRegion = null
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LevelRow(
    lvl: RegionLevel,
    onPlayLevel: (Int) -> Unit,
    onViewDish: (Int) -> Unit,
    onAfterClick: () -> Unit = {}
) {
    val clickable = lvl.isSolved || lvl.isUnlocked
    Row(
        Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .then(
                if (clickable) Modifier.clickable {
                    onAfterClick()
                    if (lvl.isSolved) onViewDish(lvl.number) else onPlayLevel(lvl.number)
                } else Modifier
            )
            .padding(horizontal = 12.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (lvl.isSolved) {
            LevelImage(
                url = lvl.photoUrl,
                fallback = lvl.photo,
                contentDescription = lvl.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .size(LevelThumbSize)
                    .clip(CircleShape)
            )
        } else {
            Box(
                Modifier
                    .size(LevelThumbSize)
                    .clip(CircleShape)
                    .background(if (lvl.isUnlocked) DarkBrown else OutlineDefault),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    "${lvl.number}",
                    color = if (lvl.isUnlocked) LightOrange else HintGray,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
            }
        }
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(
                // Never reveal an unsolved answer.
                if (lvl.isSolved) lvl.name else "Mystery Dish",
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                fontSize = 15.sp
            )
            Text(
                lvl.region.displayName,
                color = HintGray,
                fontFamily = BeVietnamPro,
                fontSize = 11.sp
            )
        }
        when {
            lvl.isSolved -> StatusChip("Solved", SuccessGreen, Icons.Default.Check)
            lvl.isUnlocked -> StatusChip("Play", BurntOrange, Icons.Default.PlayArrow)
            else -> StatusChip("Locked", HintGray, Icons.Default.Lock)
        }
    }
}

@Composable
private fun StatusChip(label: String, color: Color, icon: androidx.compose.ui.graphics.vector.ImageVector) {
    Surface(shape = RoundedCornerShape(12.dp), color = color.copy(alpha = 0.14f)) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = color, modifier = Modifier.size(13.dp))
            Spacer(Modifier.width(4.dp))
            Text(label, color = color, fontSize = 11.sp, fontWeight = FontWeight.Bold)
        }
    }
}

@Composable
private fun RegionChip(label: String, selected: Boolean, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) RegionChipOn else RegionChipOff,
        border = if (selected) null else BorderStroke(1.dp, RegionChipOffStroke),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) Color.White else RegionChipInk,
            fontFamily = BeVietnamPro,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            modifier = Modifier.padding(horizontal = 16.dp, vertical = 12.dp)
        )
    }
}

/** Whole-archipelago progress — the map's resting state. */
@Composable
private fun MapProgressCard(
    completedUpTo: Int,
    totalLevels: Int,
    onLearn: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Default.Map,
                    contentDescription = null,
                    tint = BurntOrange,
                    modifier = Modifier.size(24.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    "Map progress",
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    color = TextDark,
                    fontSize = MapProgressTitleSize
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "${(completedUpTo * 100 / totalLevels)}%",
                    color = BurntOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(8.dp))
            ProgressTrack(completedUpTo / totalLevels.toFloat())
            Spacer(Modifier.height(6.dp))
            Row {
                Text(
                    "$completedUpTo / $totalLevels dishes explored",
                    color = HintGray,
                    fontSize = 11.sp
                )
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onLearn() }
                ) {
                    Text(
                        "View Dishes",
                        color = BurntOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        " ›",
                        color = BurntOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            }
        }
    }
}

/** Detail for the tapped region, in the same slot as the progress card. */
@Composable
private fun RegionPanel(
    region: Region,
    solved: Int,
    total: Int,
    onExplore: () -> Unit
) {
    val complete = total > 0 && solved == total
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 10.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(horizontal = 14.dp, vertical = 10.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(if (complete) GoldAccent else BurntOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (complete) Icons.Default.Star else Icons.Default.RestaurantMenu,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(18.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        region.displayName,
                        fontWeight = FontWeight.Bold,
                        color = TextDark,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        region.tagline,
                        color = HintGray,
                        fontSize = 11.sp,
                        lineHeight = 14.sp,
                        maxLines = 2
                    )
                }
                Text(
                    "${if (total == 0) 0 else solved * 100 / total}%",
                    color = BurntOrange,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp
                )
            }
            Spacer(Modifier.height(6.dp))
            ProgressTrack(
                fraction = if (total == 0) 0f else solved / total.toFloat(),
                gold = complete
            )
            Spacer(Modifier.height(6.dp))
            Row {
                Text(
                    if (complete) "All $total dishes solved" else "$solved of $total dishes solved",
                    color = if (complete) DarkBrown else HintGray,
                    fontSize = 11.sp,
                    fontWeight = if (complete) FontWeight.SemiBold else FontWeight.Normal
                )
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.clickable { onExplore() }
                ) {
                    Text(
                        "Explore",
                        color = BurntOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                    Text(
                        " ›",
                        color = BurntOrange,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        modifier = Modifier.offset(y = 1.dp)
                    )
                }
            }
        }
    }
}

/** Shared bar so both panels fill at the same weight and radius. */
@Composable
private fun ProgressTrack(fraction: Float, gold: Boolean = false) {
    Box(
        Modifier
            .fillMaxWidth()
            .height(5.dp)
            .clip(CircleShape)
            .background(ProgressTrackIdle)
    ) {
        Box(
            Modifier
                .fillMaxHeight()
                .fillMaxWidth(fraction.coerceIn(0f, 1f))
                .clip(CircleShape)
                .background(
                    Brush.horizontalGradient(
                        if (gold) listOf(ProgressGoldStart, ProgressGoldEnd)
                        else listOf(ProgressFillStart, ProgressFillEnd)
                    )
                )
        )
    }
}

/**
 * Charted open water: a graticule, depth contours ringing the archipelago, and
 * a sheen that drifts across the surface. Purely decorative — it exists to make
 * the empty half of the card feel like sea rather than dead space.
 */
@Composable
private fun OceanBackdrop(modifier: Modifier = Modifier) {
    val drift by rememberInfiniteTransition(label = "ocean").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(9000, easing = LinearEasing), RepeatMode.Reverse),
        label = "ocean_drift"
    )

    Canvas(modifier) {
        val grid = Color.White.copy(alpha = 0.07f)
        val step = size.width / 5f

        // Graticule.
        var x = step
        while (x < size.width) {
            drawLine(grid, Offset(x, 0f), Offset(x, size.height), strokeWidth = 1f)
            x += step
        }
        var y = step
        while (y < size.height) {
            drawLine(grid, Offset(0f, y), Offset(size.width, y), strokeWidth = 1f)
            y += step
        }

        // Depth contours centred on the islands.
        val heart = Offset(size.width * 0.56f, size.height * 0.5f)
        val dash = PathEffect.dashPathEffect(floatArrayOf(9f, 12f))
        listOf(0.34f, 0.46f, 0.58f, 0.70f).forEachIndexed { i, r ->
            drawCircle(
                color = Color.White.copy(alpha = 0.10f - i * 0.015f),
                radius = size.minDimension * r,
                center = heart,
                style = Stroke(width = 1.4f, pathEffect = dash)
            )
        }

        // Slow sheen sweeping the surface.
        drawRect(
            brush = Brush.linearGradient(
                colors = listOf(
                    Color.Transparent,
                    Color.White.copy(alpha = 0.06f),
                    Color.Transparent
                ),
                start = Offset(size.width * (drift - 0.35f), 0f),
                end = Offset(size.width * (drift + 0.35f), size.height)
            )
        )
    }
}

/** Cartographer's compass rose — a four-point star in a double ring. */
@Composable
private fun CompassRose(modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val c = Offset(size.width / 2f, size.height / 2f)
        val r = size.minDimension / 2f
        val ink = Color.White.copy(alpha = 0.55f)

        drawCircle(ink, radius = r, center = c, style = Stroke(width = 1.4f))
        drawCircle(ink.copy(alpha = 0.3f), radius = r * 0.72f, center = c, style = Stroke(width = 1f))

        // Four-point star: each arm is a narrow kite from the centre.
        val arms = listOf(0f to -1f, 1f to 0f, 0f to 1f, -1f to 0f)
        arms.forEachIndexed { i, (dx, dy) ->
            val tip = Offset(c.x + dx * r * 0.86f, c.y + dy * r * 0.86f)
            val px = -dy * r * 0.17f
            val py = dx * r * 0.17f
            val star = Path().apply {
                moveTo(tip.x, tip.y)
                lineTo(c.x + px, c.y + py)
                lineTo(c.x - px, c.y - py)
                close()
            }
            // North reads gold so the rose has an orientation at a glance.
            drawPath(star, if (i == 0) GoldAccent.copy(alpha = 0.9f) else Color.White.copy(alpha = 0.42f))
        }
    }
}

/**
 * Map pin carrying its region's state: gold with a star once every dish there is
 * solved, and a bouncing objective ring when it holds the next dish to play.
 */
@Composable
private fun RegionPin(
    region: Region,
    selected: Boolean,
    solvedCount: Int,
    totalCount: Int,
    isNext: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val complete = totalCount > 0 && solvedCount == totalCount
    val transition = rememberInfiniteTransition(label = "pin_${region.name}")
    val pulse by transition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1900, easing = LinearEasing), RepeatMode.Restart),
        label = "pin_pulse"
    )
    // Only the objective pin hops — everything hopping would be noise.
    val hop by transition.animateFloat(
        initialValue = 0f,
        targetValue = if (isNext) -5f else 0f,
        animationSpec = infiniteRepeatable(tween(700, easing = FastOutSlowInEasing), RepeatMode.Reverse),
        label = "pin_hop"
    )

    val pinColor = when {
        selected -> DarkBrown
        complete -> GoldAccent
        else -> BurntOrange
    }

    Column(
        modifier = modifier.clickable(onClick = onClick),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(contentAlignment = Alignment.Center) {
            // Expanding ring that fades as it grows.
            Box(
                Modifier
                    .size((28 + 26 * pulse).dp)
                    .clip(CircleShape)
                    .background(pinColor.copy(alpha = 0.32f * (1f - pulse)))
            )
            Box(
                Modifier
                    .offset(y = hop.dp)
                    .size(38.dp),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.Default.LocationOn,
                    contentDescription = region.displayName,
                    tint = pinColor,
                    modifier = Modifier
                        .size(36.dp)
                        .scale(if (selected) 1.15f else 1f)
                )
                // Badge sits in the pin's head.
                if (complete) {
                    Icon(
                        Icons.Default.Star,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier
                            .size(13.dp)
                            .offset(y = (-4).dp)
                    )
                }
            }
        }
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                selected -> DarkBrown
                complete -> GoldAccent
                else -> Color.White.copy(alpha = 0.92f)
            },
            shadowElevation = 3.dp
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
            ) {
                Text(
                    if (totalCount > 0) "${region.displayName} · $solvedCount/$totalCount"
                    else region.displayName,
                    color = when {
                        selected -> LightOrange
                        complete -> DarkBrown
                        else -> DarkBrown
                    },
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold
                )
                if (isNext) {
                    Spacer(Modifier.width(4.dp))
                    Box(
                        Modifier
                            .size(5.dp)
                            .clip(CircleShape)
                            .background(BurntOrange)
                    )
                }
            }
        }
    }
}
