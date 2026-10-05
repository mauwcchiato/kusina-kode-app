package com.example.kusinakode.ui.explore

import com.example.kusinakode.ui.components.readableWidth
import com.example.kusinakode.ui.components.clickSfx
import androidx.annotation.DrawableRes
import androidx.activity.compose.BackHandler
import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.Animatable
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
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
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
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PlayCircleOutline
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.filled.TouchApp
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.runtime.*
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import android.widget.Toast
import com.example.kusinakode.CoachMarkManager
import com.example.kusinakode.ui.onboarding.CoachMarkOverlay
import com.example.kusinakode.ui.onboarding.CoachStep
import com.example.kusinakode.ui.onboarding.coachAnchor
import com.example.kusinakode.ui.onboarding.rememberCoachAnchors
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.draw.drawWithContent
import androidx.compose.ui.graphics.CompositingStrategy
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
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
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.ui.theme.RegionChipInk
import com.example.kusinakode.ui.theme.RegionChipOff
import com.example.kusinakode.ui.theme.RegionChipOffStroke
import com.example.kusinakode.ui.theme.SuccessGreen
import kotlin.math.hypot
import com.example.kusinakode.ui.home.ChromeCream
import com.example.kusinakode.ui.home.ChromeInk
import com.example.kusinakode.ui.home.ChromeStroke
import com.example.kusinakode.ui.home.regionIcon
import com.example.kusinakode.ui.components.LevelImage
import com.example.kusinakode.ui.components.ParchmentCard
import com.example.kusinakode.ui.components.parchmentPlate
import com.example.kusinakode.ui.components.KusinaButton
import com.example.kusinakode.ui.components.KusinaButtonTone

private val BurntOrange = Color(0xFFCC6B1F)
private val SeaBlue = Color(0xFF5F8A99)
private val CreamBg = Color(0xFFF1E6D2)
private val TextDark = Color(0xFF3E2723)
/** Marks conquered ground on the map — completed regions and sailed trail legs. */
private val GoldAccent = Color(0xFFE8B34A)

/** Font-size knobs — change these to scale the map labels. */
private val MapProgressTitleSize = 17.sp
private val UpNextSize = 12.sp

/** Kitchen sheet still rises from the bottom; the dishes inside use the badge tiles. */
private val SheetHandleWidth = 67.dp
private val SheetHandleTopPad = 10.dp
private val SheetHandleBottomPad = 14.dp
private val LevelThumbSize = 40.dp
private val TileEdge = Color(0xFFE0C48A)
private val TrophyGold = Color(0xFFD9A227)
private val TrophySeam = Color(0xFFC9B08A)

private data class RegionLevel(
    /**
     * The global 1-based level index — the id progress, attempts, unlocks and
     * server sync are all keyed to. This is what gets passed to onPlayLevel /
     * onViewDish; it never changes and is NEVER shown to the player.
     */
    val globalId: Int,
    /**
     * The level's position *within its region* (1..N), after ordering that
     * region's dishes by word length. This is the number the player sees:
     * every region starts at Level 1, and Level 1 is always the shortest word.
     */
    val displayNumber: Int,
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
    /**
     * The global ids of every solved level. Per-region progression is derived
     * from this: within a region a level unlocks once the previous one (by word
     * length) is in this set, so each region advances on its own.
     */
    solvedLevels: Set<Int> = emptySet(),
    /** Levels with a half-finished board, so the CTA can offer to resume. */
    resumableLevels: Set<Int> = emptySet(),
    onPlayLevel: (Int) -> Unit,
    onViewDish: (Int) -> Unit,
    onHome: () -> Unit,
    onProfile: () -> Unit,
    onLeaderboard: () -> Unit,
    onLearn: () -> Unit,
    onViewDishes: () -> Unit,
    onWallet: () -> Unit = {},
    onSettings: () -> Unit = {},
    initialRegion: Region? = null
) {
    val totalLevels = LevelProvider.visibleCount
    // Per-region levels, in LevelProvider.regionOrder: any dish an admin pinned
    // sits at its pinned slot, the rest by word length ascending (Level 1 =
    // shortest tiles), numbered 1..N *within that region*. The global id
    // underneath is preserved for play/progress/server sync — only the
    // numbering the player sees is per-region. Unlock is per-region too: a
    // region's Level 1 is always open, and Level K opens once that region's
    // Level K-1 is solved, so the three islands progress independently.
    val allLevels = remember(solvedLevels) {
        Region.entries.flatMap { region ->
            val inRegion = LevelProvider.regionOrder(region)
                .map { gid -> gid to LevelProvider.forLevel(gid) }
            inRegion.mapIndexed { idx, (gid, data) ->
                val solved = gid in solvedLevels
                val prevSolved = idx == 0 || inRegion[idx - 1].first in solvedLevels
                RegionLevel(
                    globalId = gid,
                    displayNumber = idx + 1,
                    name = data.name,
                    region = region,
                    isSolved = solved,
                    isUnlocked = solved || prevSolved,
                    photo = data.photo,
                    photoUrl = data.photoUrl
                )
            }
        }
    }
    val regionsExplored = Region.entries.count { r ->
        allLevels.any { it.region == r && it.isSolved }
    }

    var selectedRegion by remember(initialRegion) { mutableStateOf(initialRegion) }
    var sheetRegion by remember { mutableStateOf<Region?>(null) }
    LaunchedEffect(initialRegion) { selectedRegion = initialRegion }

    // First-visit tour of the Game Map itself: what the chips do, that the map
    // is tappable, and where the play button is. Runs once, like the Home tour.
    val ctx = LocalContext.current
    val coachAnchors = rememberCoachAnchors()
    var showTour by remember { mutableStateOf(false) }
    var tourAnchor by remember { mutableStateOf<String?>(null) }
    LaunchedEffect(Unit) {
        if (!CoachMarkManager.isDone(ctx, CoachMarkManager.TOUR_EXPLORE)) {
            delay(500)
            showTour = true
            // Seen once it opens, not only when finished: leaving by Back or
            // closing the app used to bring the tour back on every launch.
            CoachMarkManager.markDone(ctx, CoachMarkManager.TOUR_EXPLORE)
        }
    }

    // Drilling into a region is a step within Explore, so going back undoes
    // that step first and only leaves the screen from the whole-map view.
    val goBack: () -> Unit = {
        if (selectedRegion != null) selectedRegion = null else onHome()
    }
    BackHandler(enabled = selectedRegion != null) { selectedRegion = null }

    Box(Modifier.fillMaxSize()) {
    Scaffold(
        containerColor = CreamBg,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Levels,
                onHome = onHome,
                onProfile = onProfile,
                onLevels = { /* already here */ },
                onWallet = onWallet,
                onCompleted = onLearn,
                homeModifier = Modifier.coachAnchor("explore_home", coachAnchors)
            )
        }
    ) { inner ->
        // The map runs under the bottom bar's rounded top, so no page background
        // shows around it; what sits on the map keeps clear of the bar instead.
        val navPad = inner.calculateBottomPadding()
        Column(
            Modifier
                .fillMaxSize()
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
                            "Game Map",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "$completedUpTo of $totalLevels Dishes Solved · $regionsExplored of ${Region.entries.size} Islands",
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

            // The body is the map itself: it fills everything between the
            // header and the bottom bar and does not scroll. The islands are
            // the selector, so there is no row of region chips.
            // The dish the player is being pushed toward: the first level
            // that is open but not yet solved (falling back to the first
            // unsolved, then the very first). When a region is open the CTA
            // follows THAT region, so opening Visayas offers "Visayas Level
            // 1"; otherwise it points at the next level overall.
            val ctaPool = selectedRegion
                ?.let { r -> allLevels.filter { it.region == r } }
                ?: allLevels
            val nextTarget = ctaPool.firstOrNull { it.isUnlocked && !it.isSolved }
                ?: ctaPool.firstOrNull { !it.isSolved }
                ?: ctaPool.firstOrNull()
                ?: allLevels.firstOrNull()
            val nextLevel = nextTarget?.globalId ?: 1
            val nextRegion = nextTarget?.region
            // The island the chef is travelling to, while a road trip is under way.
            var tripTo by remember { mutableStateOf<Region?>(null) }
            // The islands use the map's full height; nothing is docked above them.
            val mapArtTop = 0.dp

            Box(
                Modifier
                    .weight(1f)
                    .fillMaxWidth()
            ) {
                Column(Modifier.fillMaxSize()) {
                    // ---- The map: edge to edge, the whole screen ----
                    Box(
                        Modifier
                            .weight(1f)
                            .fillMaxWidth()
                            .coachAnchor("explore_map", coachAnchors)
                            // A kitchen table: the chart lies on warm wood,
                            // the same browns as Home and the walkthrough cards.
                            .drawBehind { drawKitchenTable() }
                    ) {
                        // The chart's parchment frame, a little larger than the art
                        // so its stitched border shows around it.
                        Box(
                            Modifier
                                .matchParentSize()
                                .padding(
                                    start = MapTableSide - MapFrameBorder,
                                    end = MapTableSide - MapFrameBorder,
                                    top = mapArtTop + MapTableTop - MapFrameBorder,
                                    bottom = navPad + MapCtaRoom - MapFrameBorder - MapFrameLedge
                                )
                                .parchmentPlate(stitched = false)
                        )

                        // The chart is the island art itself: the whole archipelago
                        // until a region is picked, then that region's own map.
                        Crossfade(
                            targetState = selectedRegion,
                            animationSpec = tween(420),
                            label = "map_region",
                            // The art stops above PLAY NOW and the bottom bar, so no island
                            // ever sits under the button; the table carries on below.
                            // The pin layer below uses exactly these insets.
                            modifier = Modifier
                                .matchParentSize()
                                .padding(
                                    start = MapTableSide,
                                    end = MapTableSide,
                                    top = mapArtTop + MapTableTop,
                                    bottom = navPad + MapCtaRoom
                                )
                                .clip(RoundedCornerShape(MapArtCorner))
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

                        // Pins, the road and the chef are placed on the
                        // archipelago art as it is actually drawn. The art is
                        // cropped to fill a card whose shape depends on the
                        // phone, so positions are worked out from the drawn
                        // image rather than from the card.
                        BoxWithConstraints(
                            Modifier
                                .matchParentSize()
                                .padding(
                                    start = MapTableSide,
                                    end = MapTableSide,
                                    top = mapArtTop + MapTableTop,
                                    bottom = navPad + MapCtaRoom
                                )
                        ) {
                            val cardW = maxWidth.value
                            val cardH = maxHeight.value
                            val artScale = maxOf(cardW / PhArtWidth, cardH / PhArtHeight)
                            val artW = PhArtWidth * artScale
                            val artH = PhArtHeight * artScale
                            val artX = (cardW - artW) / 2f
                            val artY = (cardH - artH) / 2f
                            // Island stops as fractions of the archipelago art.
                            val pinSpots = listOf(
                                Triple(Region.LUZON, 0.42f, 0.299f),
                                Triple(Region.VISAYAS, 0.58f, 0.541f),
                                Triple(Region.MINDANAO, 0.715f, 0.815f)
                            )
                            // Stops in dp, in the card's own space.
                            val stops = pinSpots.map { (_, fx, fy) ->
                                Offset(artX + fx * artW, artY + fy * artH)
                            }

                            // ---- The road trip ----
                            // The trail is a road between the islands, and the
                            // Kusina Kode chef stands on it at the island with the
                            // next dish. Tapping an island sends the chef down the
                            // road to it, and the island opens when they arrive.
                            val trailProgress = Region.entries.count { r ->
                                allLevels.any { it.region == r && it.isSolved }
                            }
                            val chefHome = nextRegion ?: Region.LUZON
                            val homeIdx = pinSpots.indexOfFirst { it.first == chefHome }
                            val tripScope = rememberCoroutineScope()
                            val trip = remember { Animatable(0f) }
                            val tripToIdx = tripTo?.let { to -> pinSpots.indexOfFirst { it.first == to } }
                            val goTo: (Region) -> Unit = { to ->
                                val toIdx = pinSpots.indexOfFirst { it.first == to }
                                if (tripTo == null) {
                                    if (toIdx == homeIdx) {
                                        selectedRegion = to
                                    } else {
                                        tripTo = to
                                        tripScope.launch {
                                            trip.snapTo(0f)
                                            // Longer roads take longer: one leg per island passed.
                                            val legs = kotlin.math.abs(toIdx - homeIdx)
                                            trip.animateTo(
                                                1f,
                                                tween(RoadTripLegMs * legs, easing = FastOutSlowInEasing)
                                            )
                                            selectedRegion = to
                                            tripTo = null
                                            trip.snapTo(0f)
                                        }
                                    }
                                }
                            }

                            // The road joins the three regions, so it only means
                            // anything on the full chart. Every line on it is
                            // dashed; gold marks the stretch already travelled.
                            if (selectedRegion == null) Canvas(Modifier.fillMaxSize()) {
                                val pts = stops.map { Offset(it.x.dp.toPx(), it.y.dp.toPx()) }
                                val stroke = (artW * 0.006f).dp.toPx()
                                val dash = PathEffect.dashPathEffect(floatArrayOf(stroke * 2.6f, stroke * 2f))
                                for (i in 1 until pts.size) {
                                    val a = pts[i - 1]
                                    val b = pts[i]
                                    val ctrl = roadCtrl(a, b)
                                    val leg = Path().apply {
                                        moveTo(a.x, a.y)
                                        quadraticBezierTo(ctrl.x, ctrl.y, b.x, b.y)
                                    }
                                    val sailed = i < trailProgress
                                    // A thin, dashed road bed under a dashed
                                    // centre line.
                                    drawPath(
                                        leg,
                                        RoadBed.copy(alpha = 0.55f),
                                        style = Stroke(
                                            width = stroke * 2.6f,
                                            cap = StrokeCap.Round,
                                            pathEffect = dash
                                        )
                                    )
                                    drawPath(
                                        leg,
                                        if (sailed) GoldAccent else RoadLine,
                                        style = Stroke(
                                            width = stroke * 0.9f,
                                            cap = StrokeCap.Round,
                                            pathEffect = dash
                                        )
                                    )
                                }
                                // Footprints left behind the travelling chef.
                                val toIdx = tripToIdx
                                if (toIdx != null) {
                                    for (k in 1..RoadTripPrints) {
                                        val t = trip.value - k * 0.035f
                                        if (t <= 0f) break
                                        val f = roadPoint(stops, homeIdx, toIdx, t)
                                        drawCircle(
                                            color = RoadLine.copy(alpha = 0.85f * (1f - k / (RoadTripPrints + 1f))),
                                            radius = stroke * 1.1f,
                                            center = Offset(f.x.dp.toPx(), f.y.dp.toPx())
                                        )
                                    }
                                }
                                // Waypoint beads mark each landfall.
                                pts.forEachIndexed { i, p ->
                                    val reached = i < trailProgress
                                    drawCircle(
                                        color = if (reached) GoldAccent else RoadLine,
                                        radius = stroke * 1.6f,
                                        center = p
                                    )
                                }
                            }

                            // An invisible frame around the three islands, the
                            // chef and their labels: what the walkthrough's first
                            // step spotlights. Draws nothing and takes no touches.
                            if (selectedRegion == null) {
                                val left = stops.minOf { it.x } - IslandsPadSide
                                val right = stops.maxOf { it.x } + IslandsPadSide
                                val top = stops.minOf { it.y } - IslandsPadTop
                                val bottom = stops.maxOf { it.y } + IslandsPadBottom
                                Box(
                                    Modifier
                                        .offset(x = left.dp, y = top.dp)
                                        .size(width = (right - left).dp, height = (bottom - top).dp)
                                        .coachAnchor("explore_islands", coachAnchors)
                                )
                            }

                            if (selectedRegion == null) pinSpots.forEachIndexed { i, (region, _, _) ->
                                val inRegion = allLevels.filter { it.region == region }
                                val solvedHere = inRegion.count { it.isSolved }
                                RegionPin(
                                    region = region,
                                    selected = tripTo == region,
                                    solvedCount = solvedHere,
                                    totalCount = inRegion.size,
                                    isNext = region == nextRegion,
                                    // The suggestion rides on the island's own label,
                                    // so it can never point at the wrong island.
                                    callToAction = when {
                                        region != nextRegion -> null
                                        solvedHere > 0 -> "CONTINUE"
                                        else -> "START HERE"
                                    },
                                    modifier = Modifier
                                        .offset(x = stops[i].x.dp - 30.dp, y = stops[i].y.dp - 54.dp)
                                        .then(
                                            if (region == Region.LUZON) {
                                                Modifier.coachAnchor("explore_pin", coachAnchors)
                                            } else {
                                                Modifier
                                            }
                                        )
                                ) { goTo(region) }
                            }

                            if (selectedRegion == null) {
                                val toIdx = tripToIdx
                                val at = if (toIdx != null) roadPoint(stops, homeIdx, toIdx, trip.value)
                                else stops[homeIdx]
                                val legs = if (toIdx != null) kotlin.math.abs(toIdx - homeIdx) else 0
                                RoadTripChef(
                                    travelling = toIdx != null,
                                    progress = trip.value,
                                    legs = legs,
                                    facingLeft = toIdx != null && stops[toIdx].x < stops[homeIdx].x,
                                    modifier = Modifier.offset(
                                        // At rest the chef waits to the left of the pin,
                                        // clear of its label, and steps onto the road as a trip starts.
                                        x = at.x.dp - RoadChefWidth / 2 +
                                            RoadChefRestShift * (if (toIdx == null) 1f else (1f - trip.value * 6f).coerceIn(0f, 1f)),
                                        y = at.y.dp - RoadChefHeight + 6.dp
                                    )
                                )
                            }

                        }

                        // Vignette: darkens the card edges so the archipelago sits
                        // in the light. Drawn above the sea, below the UI.
                        Box(
                            Modifier
                                .matchParentSize()
                                .background(
                                    Brush.radialGradient(
                                        colors = listOf(
                                            Color.Transparent,
                                            Color.Transparent,
                                            Color(0xFF1E3742).copy(alpha = 0.32f)
                                        ),
                                        radius = 1100f
                                    )
                                )
                        )

                        // An island open: its levels, laid out on the island as
                        // a path of numbered stops.
                        Crossfade(
                            targetState = selectedRegion,
                            animationSpec = tween(260),
                            label = "map_info",
                            modifier = Modifier.matchParentSize()
                        ) { region ->
                            if (region != null) {
                                val regionLevels = allLevels.filter { it.region == region }
                                IslandLevelsLayer(
                                    region = region,
                                    levels = regionLevels,
                                    dishesModifier = Modifier.coachAnchor("explore_dishes", coachAnchors),
                                    nextLevelModifier = Modifier.coachAnchor("explore_level", coachAnchors),
                                    onDishList = { sheetRegion = region },
                                    onBack = { selectedRegion = null },
                                    topInset = 12.dp,
                                    bottomInset = navPad + MapCtaRoom,
                                    onOpen = { lvl ->
                                        if (lvl.isSolved) onViewDish(lvl.globalId)
                                        else onPlayLevel(lvl.globalId)
                                    }
                                )
                            }
                        }

                        // ---- Jump straight into the next dish ----
                        // Floats on the map's lower edge, over open water.
                        // An abandoned board takes priority over the label: this
                        // button drops you back into that exact round, so calling it
                        // "Play" would misdescribe what happens.
                        val canResume = nextLevel in resumableLevels
                        // Player-facing label: region + per-region level number,
                        // never the global id. e.g. "Play now · Luzon Level 2".
                        val ctaLevel = nextTarget?.let {
                            "${it.region.displayName} Level ${it.displayNumber}"
                        } ?: "Level 1"
                        // The app's carved brown button, narrower than the map
                        // and centred, so it reads as a button on the map rather
                        // than a bar across the bottom of it.
                        KusinaButton(
                            label = when {
                                canResume -> "Continue · $ctaLevel"
                                completedUpTo >= totalLevels -> "Play again · $ctaLevel"
                                else -> "Play now · $ctaLevel"
                            }.uppercase(),
                            onClick = clickSfx { onPlayLevel(nextLevel) },
                            tone = KusinaButtonTone.Brown,
                            height = 50.dp,
                            fontSize = 14.sp,
                            leading = {
                                Icon(
                                    if (canResume) Icons.Default.PlayCircleOutline else Icons.Default.PlayArrow,
                                    contentDescription = null,
                                    // The button's own cream ink.
                                    tint = PlateCream,
                                    modifier = Modifier.size(18.dp)
                                )
                            },
                            modifier = Modifier
                                .align(Alignment.BottomCenter)
                                // Centred on the wood between the chart's frame and
                                // the bottom bar. The bar's padding includes the
                                // clear space its raised circle needs, above the
                                // white bar itself, so this sits a little inside it.
                                .padding(bottom = navPad - MapCtaDrop)
                                .fillMaxWidth(MapCtaWidth)
                                .widthIn(max = 360.dp)
                                .coachAnchor("explore_cta", coachAnchors)
                        )
                    }
                }

                // Nothing sits over the whole map at rest: the walkthrough says
                // what to do, and the islands have the full height. Only while
                // the chef is travelling does a note say where they are headed.
                val dest = tripTo
                if (selectedRegion == null && dest != null) {
                    MapTopPanel(
                        tripTo = dest,
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            .readableWidth()
                            .padding(start = 12.dp, end = 12.dp, top = MapSearchTop)
                    )
                }

            }
        }
    }

    // ---- Region levels sheet ----
    sheetRegion?.let { region ->
        val regionLevels = allLevels.filter { it.region == region }
        ModalBottomSheet(
            onDismissRequest = { sheetRegion = null },
            containerColor = Color.Transparent,
            shape = RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp),
            dragHandle = null
        ) {
            RegionKitchenSheet(
                region = region,
                regionLevels = regionLevels,
                onOpen = { lvl ->
                    sheetRegion = null
                    if (lvl.isSolved) onViewDish(lvl.globalId) else onPlayLevel(lvl.globalId)
                }
            )
        }
    }

    // Drawn in this window, not as a dialog, so the walkthrough can spotlight it.
    if (showTour && tourAnchor == "explore_kitchen") {
        Box(Modifier.fillMaxSize(), contentAlignment = Alignment.BottomCenter) {
            RegionKitchenSheet(
                region = Region.LUZON,
                regionLevels = allLevels.filter { it.region == Region.LUZON },
                modifier = Modifier.fillMaxHeight(0.94f),
                headerModifier = Modifier.coachAnchor("explore_kitchen", coachAnchors),
                onOpen = { _ -> }
            )
        }
    }

    if (showTour) {
        CoachMarkOverlay(
            steps = listOf(
                CoachStep(
                    anchorKey = "explore_islands",
                    title = "Tap an island to start your food trip",
                    body = "Luzon, Visayas and Mindanao each have their own mystery " +
                        "dishes, from Level 1 up.",
                    // The islands fill the map, so the card waits at the foot.
                    cardAtBottom = true
                ),
                CoachStep(
                    anchorKey = "explore_pin",
                    title = "Island pins",
                    body = "Each pin shows how many of that island's dishes you've cooked. " +
                        "The chef waits by your next one, and walks the road to any " +
                        "island you tap."
                ),
                CoachStep(
                    anchorKey = "explore_level",
                    round = true,
                    title = "Island levels",
                    body = "Each circle is a level. Solved ones show their dish, the chef " +
                        "stands on your next one, and the rest are mystery dishes."
                ),
                CoachStep(
                    anchorKey = "explore_kitchen",
                    title = "The kitchen",
                    body = "Dish list opens this: every dish on the island in one place. " +
                        "Solved ones show their photo; the rest stay a mystery until " +
                        "you reach them."
                ),
                CoachStep(
                    anchorKey = "explore_cta",
                    title = "Jump right in",
                    body = "This button drops you into your next unsolved dish, or back " +
                        "into a round you left simmering."
                ),
                CoachStep(
                    anchorKey = "explore_home",
                    title = "Home",
                    body = "Leave the map and return to your kitchen dashboard."
                )
            ),
            anchors = coachAnchors,
            onFinish = {
                CoachMarkManager.markDone(ctx, CoachMarkManager.TOUR_EXPLORE)
                showTour = false
                selectedRegion = null
            },
            onStepChange = { step ->
                tourAnchor = step.anchorKey
                when (step.anchorKey) {
                    "explore_level", "explore_kitchen" -> selectedRegion = Region.LUZON
                    "explore_islands", "explore_pin", "explore_cta", "explore_home" ->
                        selectedRegion = null
                }
            }
        )
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
                    if (lvl.isSolved) onViewDish(lvl.globalId) else onPlayLevel(lvl.globalId)
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
                    "${lvl.displayNumber}",
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

/**
 * Sheet wash from the kitchen screenshot: near-white cream at the top,
 * easing into a soft beige at the bottom.
 */
private fun Modifier.kitchenSheetWash(): Modifier = drawBehind {
    drawRect(
        brush = Brush.verticalGradient(
            0f to Color(0xFFFFF8ED),
            0.18f to Color(0xFFF8F4E9),
            0.40f to Color(0xFFF6EFE4),
            0.62f to Color(0xFFF4E6D4),
            0.82f to Color(0xFFEFE0C6),
            1f to Color(0xFFE6D4B2)
        )
    )
}

/** Dashed stitch from the trophy-case plate, drawn inside the sheet that slides up. */
private fun Modifier.kitchenSheetPlate(): Modifier = drawBehind {
    val inset = 2.dp.toPx()
    drawRoundRect(
        color = TrophySeam,
        topLeft = Offset(inset, inset),
        size = Size(size.width - inset * 2f, size.height - inset * 2f),
        cornerRadius = CornerRadius(18.dp.toPx()),
        style = Stroke(
            width = 1.6.dp.toPx(),
            pathEffect = PathEffect.dashPathEffect(floatArrayOf(7.dp.toPx(), 4.dp.toPx()))
        )
    )
}

/**
 * One dish in the kitchen sheet, in the same three-across tile as a badge.
 * The dish you can play shows the brown play button and no level number.
 * An unsolved name stays hidden.
 */
@Composable
private fun KitchenDishTile(
    lvl: RegionLevel,
    modifier: Modifier = Modifier,
    onOpen: () -> Unit
) {
    val playable = lvl.isUnlocked && !lvl.isSolved
    val open = lvl.isSolved || lvl.isUnlocked
    Column(
        modifier
            .padding(horizontal = 2.dp)
            .then(if (open) Modifier.clickable(onClick = onOpen) else Modifier),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(16.dp))
                .then(
                    if (lvl.isUnlocked) {
                        Modifier.background(Color.White)
                    } else {
                        Modifier.background(
                            Brush.verticalGradient(
                                0f to Color(0xFFFBF7EE),
                                0.35f to Color(0xFFF7F1E4),
                                0.65f to Color(0xFFF3E6CC),
                                1f to Color(0xFFEED9B6)
                            )
                        )
                    }
                )
                .border(1.dp, TileEdge, RoundedCornerShape(16.dp))
                .padding(
                    when {
                        lvl.isSolved -> 6.dp
                        playable -> 9.dp
                        else -> 4.dp
                    }
                ),
            contentAlignment = Alignment.Center
        ) {
            when {
                lvl.isSolved -> LevelImage(
                    url = lvl.photoUrl,
                    fallback = lvl.photo,
                    contentDescription = lvl.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(RoundedCornerShape(12.dp))
                )
                playable -> Box(
                    Modifier
                        .fillMaxWidth(0.5f)
                        .aspectRatio(1f)
                        .clip(CircleShape)
                        .background(PlayNowBrown),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.PlayArrow,
                        contentDescription = "Play",
                        tint = Color.White,
                        modifier = Modifier.fillMaxSize(0.62f)
                    )
                }
                else -> Image(
                    painter = painterResource(R.drawable.dishes_locked),
                    contentDescription = null,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.fillMaxSize()
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            if (lvl.isSolved) lvl.name else "Mystery Dish",
            color = TextDark,
            fontFamily = BeVietnamPro,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold,
            lineHeight = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 2
        )
        Text(
            when {
                lvl.isSolved -> "Solved"
                playable -> "Level ${lvl.displayNumber}"
                else -> "Locked"
            },
            color = HintGray.copy(alpha = 0.7f),
            fontFamily = BeVietnamPro,
            fontSize = 9.sp,
            lineHeight = 11.sp,
            textAlign = TextAlign.Center,
            maxLines = 2,
            modifier = Modifier.padding(top = 2.dp)
        )
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
private fun RegionChip(
    label: String,
    selected: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(50),
        color = if (selected) PlayNowBrown else RegionChipOff,
        border = if (selected) null else BorderStroke(1.dp, RegionChipOffStroke),
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Text(
            label,
            color = if (selected) Color.White else RegionChipInk,
            fontFamily = BeVietnamPro,
            fontWeight = if (selected) FontWeight.Bold else FontWeight.SemiBold,
            fontSize = 13.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 8.dp, vertical = 12.dp)
        )
    }
}

/** The slide-up kitchen: title, dish count, and the three-across tiles. */
@Composable
private fun RegionKitchenSheet(
    region: Region,
    regionLevels: List<RegionLevel>,
    modifier: Modifier = Modifier,
    headerModifier: Modifier = Modifier,
    onOpen: (RegionLevel) -> Unit
) {
    Column(
        modifier
            .fillMaxWidth()
            .fillMaxHeight()
            .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
            .kitchenSheetWash()
    ) {
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
                    .background(TrophySeam)
            )
        }
        Column(
            Modifier
                .weight(1f)
                .fillMaxWidth()
                .padding(horizontal = 12.dp)
                .kitchenSheetPlate()
                .padding(horizontal = 14.dp, vertical = 10.dp)
        ) {
            val lead = regionLevels.take(3)
            val rest = regionLevels.drop(3)
            Column(headerModifier) {
                Text(
                    "${region.displayName} Dish List",
                    color = TextDark,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                Spacer(Modifier.height(4.dp))
                Text(
                    region.tagline,
                    color = TextDark.copy(alpha = 0.62f),
                    fontFamily = BeVietnamPro,
                    fontSize = 12.sp,
                    lineHeight = 17.sp
                )
                Spacer(Modifier.height(6.dp))
                Text(
                    "DISHES · ${regionLevels.size}",
                    color = TrophyGold,
                    fontFamily = BeVietnamPro,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.ExtraBold,
                    letterSpacing = 1.4.sp
                )
                if (lead.isNotEmpty()) {
                    Spacer(Modifier.height(12.dp))
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        lead.forEach { lvl ->
                            KitchenDishTile(
                                lvl = lvl,
                                modifier = Modifier.weight(1f),
                                onOpen = { onOpen(lvl) }
                            )
                        }
                        repeat(3 - lead.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
            Spacer(Modifier.height(10.dp))
            LazyColumn(
                modifier = Modifier
                    .weight(1f)
                    .navigationBarsPadding()
            ) {
                items(rest.chunked(3)) { row ->
                    Row(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(bottom = 10.dp)
                    ) {
                        row.forEach { lvl ->
                            KitchenDishTile(
                                lvl = lvl,
                                modifier = Modifier.weight(1f),
                                onOpen = { onOpen(lvl) }
                            )
                        }
                        repeat(3 - row.size) { Spacer(Modifier.weight(1f)) }
                    }
                }
            }
        }
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
    onExplore: () -> Unit,
    dishesModifier: Modifier = Modifier
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
                    if (complete) "All $total Dishes Solved" else "$solved of $total Dishes Solved",
                    color = if (complete) DarkBrown else HintGray,
                    fontSize = 11.sp,
                    fontWeight = if (complete) FontWeight.SemiBold else FontWeight.Normal
                )
                Spacer(Modifier.weight(1f))
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = dishesModifier.clickable { onExplore() }
                ) {
                    Text(
                        "Game Dishes",
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
    /** "CONTINUE" / "START HERE" on the suggested island; null elsewhere. */
    callToAction: String? = null,
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
        // A fixed footprint: the pulse and the hop are drawn, not laid out, so
        // the pin's measured size never changes. It used to grow and shrink
        // with the ring every frame, and the walkthrough spotlight that
        // measures this pin jumped along with it.
        Box(Modifier.size(PinFootprint), contentAlignment = Alignment.Center) {
            // Expanding ring that fades as it grows. The pulse is read only
            // while drawing, so each frame repaints the ring without rebuilding
            // the pin, its label and its shadow (which held a mid-range phone
            // near 30 fps on the map; stress test, 5 Oct).
            // Its own layer, so repainting the ring each frame does not also
            // repaint the map, the road and every label underneath it.
            Box(
                Modifier
                    .size(PinFootprint)
                    .graphicsLayer()
                    .drawBehind {
                        drawCircle(
                            color = pinColor.copy(alpha = 0.32f * (1f - pulse)),
                            radius = ((28f + 26f * pulse) / 2f).dp.toPx()
                        )
                    }
            )
            Box(
                Modifier
                    .graphicsLayer { translationY = hop.dp.toPx() }
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
        if (callToAction != null) {
            // The suggested island's label becomes the call to action itself.
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = PlayNowBrown,
                border = BorderStroke(1.5.dp, GoldAccent),
                shadowElevation = 5.dp
            ) {
                Column(
                    horizontalAlignment = Alignment.CenterHorizontally,
                    modifier = Modifier.padding(horizontal = 9.dp, vertical = 3.dp)
                ) {
                    Text(
                        callToAction,
                        color = GoldAccent,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 9.sp,
                        letterSpacing = 1.sp
                    )
                    Text(
                        if (totalCount > 0) "${region.displayName} · $solvedCount/$totalCount"
                        else region.displayName,
                        color = Color.White,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
        } else Surface(
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

/**
 * The whole map's resting card. It leads with the instruction (choose an
 * island) and keeps overall progress underneath, rather than being a
 * progress card that leaves the player to work out what to do.
 */
@Composable
private fun ChooseIslandCard(
    completedUpTo: Int,
    totalLevels: Int,
    onViewDishes: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = modifier.fillMaxWidth()
    ) {
        Column(Modifier.padding(14.dp)) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    Modifier
                        .size(34.dp)
                        .clip(CircleShape)
                        .background(BurntOrange),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(Modifier.width(10.dp))
                Column(Modifier.weight(1f)) {
                    Text(
                        "Choose an island",
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        color = TextDark,
                        fontSize = MapProgressTitleSize
                    )
                    Text(
                        "Tap Luzon, Visayas or Mindanao to see its mystery dishes.",
                        color = HintGray,
                        fontFamily = BeVietnamPro,
                        fontSize = 11.sp,
                        lineHeight = 14.sp
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
            ProgressTrack(if (totalLevels == 0) 0f else completedUpTo / totalLevels.toFloat())
            Spacer(Modifier.height(6.dp))
            Row {
                Text(
                    "$completedUpTo / $totalLevels dishes solved",
                    color = HintGray,
                    fontSize = 11.sp
                )
                Spacer(Modifier.weight(1f))
                Text(
                    "View Dishes ›",
                    color = BurntOrange,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.SemiBold,
                    modifier = Modifier.clickable { onViewDishes() }
                )
            }
        }
    }
}

/**
 * An island's levels drawn over its map: a header strip (name, progress, the
 * full dish list) above a winding path of numbered stops.
 */
@Composable
private fun IslandLevelsLayer(
    region: Region,
    levels: List<RegionLevel>,
    dishesModifier: Modifier,
    onDishList: () -> Unit,
    onBack: () -> Unit,
    onOpen: (RegionLevel) -> Unit,
    /** Space kept clear at the top for the map's search bar. */
    topInset: Dp = 12.dp,
    /** Space kept clear at the foot for PLAY NOW and the bottom bar. */
    bottomInset: Dp = 76.dp,
    /** Marks the next level's stop, for the walkthrough to spotlight. */
    nextLevelModifier: Modifier = Modifier
) {
    val solved = levels.count { it.isSolved }
    val total = levels.size
    val complete = total > 0 && solved == total
    Box(Modifier.fillMaxSize()) {
        // Settles the island art back so the stops read on top of it.
        Box(
            Modifier
                .matchParentSize()
                .background(
                    Brush.verticalGradient(
                        listOf(
                            PathScrim.copy(alpha = 0.55f),
                            PathScrim.copy(alpha = 0.25f),
                            PathScrim.copy(alpha = 0.45f)
                        )
                    )
                )
        )
        Column(
            Modifier
                .fillMaxSize()
                // Bottom room for the PLAY NOW button floating on the map.
                .padding(start = 12.dp, end = 12.dp, top = topInset, bottom = bottomInset)
        ) {
            // The island's name plate, on the same parchment as the app's
            // dialogs and the walkthrough.
            ParchmentCard(contentPadding = 12.dp, ledge = false) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    // Wooden back medallion, centred on the island's name and
                    // the count under it, not on the whole plate with its bar.
                    Box(
                        Modifier
                            .align(Alignment.Top)
                            .offset(y = PlateArrowNudge)
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(PlayNowBrown)
                            .border(2.dp, PlateGold, CircleShape)
                            .clickable(onClick = onBack),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back to all islands",
                            tint = PlateCream,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            region.displayName,
                            color = PlateInk,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 18.sp,
                            lineHeight = 20.sp
                        )
                        Text(
                            if (complete) "All $total Dishes Solved" else "$solved of $total Dishes Solved",
                            color = PlateInkSoft,
                            fontFamily = BeVietnamPro,
                            fontSize = 11.sp
                        )
                        Spacer(Modifier.height(5.dp))
                        ProgressTrack(
                            fraction = if (total == 0) 0f else solved / total.toFloat(),
                            gold = complete
                        )
                        // Clear of the plate's stitched lower edge.
                        Spacer(Modifier.height(8.dp))
                    }
                    Spacer(Modifier.width(10.dp))
                    // Stitched wooden tab for the full kitchen list.
                    Box(
                        dishesModifier
                            .clip(RoundedCornerShape(50))
                            .background(PlayNowBrown)
                            .border(1.5.dp, PlateGold, RoundedCornerShape(50))
                            .clickable(onClick = onDishList)
                            .padding(horizontal = 12.dp, vertical = 7.dp)
                    ) {
                        Text(
                            "Dish List ›",
                            color = PlateCream,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 12.sp
                        )
                    }
                }
            }
            Spacer(Modifier.height(6.dp))
            LevelPath(
                levels = levels,
                onOpen = onOpen,
                nextLevelModifier = nextLevelModifier,
                modifier = Modifier
                    .weight(1f)
                    .fillMaxWidth()
            )
        }
    }
}

/**
 * The island's levels as a snaking path: left to right, then back, row by
 * row, joined by a trail that turns gold where it has been cooked through.
 * Level 1 is top-left; the order is the Game Map's own (word length).
 */
@Composable
private fun LevelPath(
    levels: List<RegionLevel>,
    onOpen: (RegionLevel) -> Unit,
    modifier: Modifier = Modifier,
    nextLevelModifier: Modifier = Modifier
) {
    if (levels.isEmpty()) return
    val ctx = LocalContext.current
    val perRow = PathPerRow
    val rows = (levels.size + perRow - 1) / perRow
    val current = levels.firstOrNull { it.isUnlocked && !it.isSolved }

    BoxWithConstraints(modifier) {
        val cellW = maxWidth / perRow
        val rowH = minOf(maxHeight / rows, cellW * 1.35f)
        val top = (maxHeight - rowH * rows) / 2
        val node = minOf(cellW * 0.66f, 60.dp)

        // Centre of stop i, snaking so each row runs the opposite way.
        fun centre(i: Int): Pair<Dp, Dp> {
            val row = i / perRow
            val inRow = i % perRow
            val col = if (row % 2 == 0) inRow else perRow - 1 - inRow
            return (cellW * (col + 0.5f)) to (top + rowH * (row + 0.5f))
        }

        Canvas(Modifier.fillMaxSize()) {
            val stroke = 5.dp.toPx()
            for (i in 1 until levels.size) {
                val (ax, ay) = centre(i - 1)
                val (bx, by) = centre(i)
                val a = Offset(ax.toPx(), ay.toPx())
                val b = Offset(bx.toPx(), by.toPx())
                val leg = Path().apply {
                    moveTo(a.x, a.y)
                    if (a.y == b.y) {
                        lineTo(b.x, b.y)
                    } else {
                        // Row change: bow out past the end stop, like a road turning.
                        val out = if (b.x > size.width / 2f) 1f else -1f
                        val bow = cellW.toPx() * 0.55f
                        cubicTo(a.x + out * bow, a.y, b.x + out * bow, b.y, b.x, b.y)
                    }
                }
                // Gold up to and including the leg into the next level.
                val cooked = levels[i].isSolved || levels[i - 1].isSolved
                drawPath(
                    leg,
                    Color.Black.copy(alpha = 0.25f),
                    style = Stroke(width = stroke * 1.8f, cap = StrokeCap.Round)
                )
                drawPath(
                    leg,
                    if (cooked) GoldAccent else Color.White.copy(alpha = 0.75f),
                    style = Stroke(
                        width = stroke,
                        cap = StrokeCap.Round,
                        pathEffect = if (cooked) null
                        else PathEffect.dashPathEffect(floatArrayOf(stroke * 1.4f, stroke * 1.6f))
                    )
                )
            }
        }

        levels.forEachIndexed { i, lvl ->
            val (cx, cy) = centre(i)
            LevelNode(
                lvl = lvl,
                isCurrent = lvl == current,
                size = node,
                modifier = Modifier
                    .offset(x = cx - node / 2, y = cy - node / 2)
                    .then(if (lvl == current) nextLevelModifier else Modifier)
            ) {
                if (lvl.isSolved || lvl.isUnlocked) {
                    onOpen(lvl)
                } else {
                    Toast.makeText(
                        ctx,
                        "Solve Level ${lvl.displayNumber - 1} first to unlock this dish",
                        Toast.LENGTH_SHORT
                    ).show()
                }
            }
        }

        // The chef arrived here on the road trip and stands on top of the next
        // level's play button, feet on its rim. Drawn after the stops so it is
        // in front, and it takes no touches, so the button under it still plays.
        val currentIdx = levels.indexOf(current)
        if (currentIdx >= 0) {
            val (cx, cy) = centre(currentIdx)
            val chefH = node * 0.9f
            val chefW = chefH * 0.55f
            RoadTripChef(
                travelling = false,
                progress = 0f,
                legs = 0,
                facingLeft = false,
                modifier = Modifier
                    .offset(
                        x = cx - chefW / 2,
                        y = cy - node / 2 - chefH + node * 0.16f
                    )
                    .size(width = chefW, height = chefH)
            )
        }
    }
}

/**
 * One stop on the path. Solved: the dish's photo in a gold ring. Next: a
 * glowing, pulsing play stop with the chef standing on it. Locked: a mystery "?".
 * The level number rides on a small tab under each.
 */
/** The current stop's pulsing glow, drawn past its own bounds without moving anything. */
@Composable
private fun CurrentNodeGlow(size: Dp) {
    val pulse by rememberInfiniteTransition(label = "node_glow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1400, easing = LinearEasing), RepeatMode.Restart),
        label = "node_pulse"
    )
    // Its own layer, so the glow repaints alone rather than with the island.
    Box(
        Modifier
            .size(size)
            .graphicsLayer()
            .drawBehind {
                drawCircle(
                    color = GoldAccent.copy(alpha = 0.45f * (1f - pulse)),
                    radius = this.size.minDimension / 2f * (1f + 0.55f * pulse)
                )
            }
    )
}

@Composable
private fun LevelNode(
    lvl: RegionLevel,
    isCurrent: Boolean,
    size: Dp,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Box(modifier.size(size), contentAlignment = Alignment.Center) {
        if (isCurrent) {
            // Expanding glow ring behind the next level, spreading past the
            // stop's own bounds. Only the current stop animates, and only while
            // drawing: every stop used to run its own loop and rebuild itself
            // (dish photo included) each frame, and this ring re-laid out the
            // island as it grew.
            CurrentNodeGlow(size)
        }
        Box(
            Modifier
                .size(size)
                .scale(if (isCurrent) 1.08f else 1f)
                .clip(CircleShape)
                .background(
                    when {
                        lvl.isSolved -> GoldAccent
                        isCurrent -> PlayNowBrown
                        else -> NodeLockedFill
                    }
                )
                .border(
                    width = 3.dp,
                    color = when {
                        lvl.isSolved -> NodeSolvedRim
                        isCurrent -> GoldAccent
                        else -> NodeLockedRim
                    },
                    shape = CircleShape
                )
                .clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) {
            when {
                lvl.isSolved -> LevelImage(
                    url = lvl.photoUrl,
                    fallback = lvl.photo,
                    contentDescription = lvl.name,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(4.dp)
                        .clip(CircleShape)
                )
                isCurrent -> Icon(
                    Icons.Default.PlayArrow,
                    contentDescription = "Play level ${lvl.displayNumber}",
                    tint = Color.White,
                    modifier = Modifier.fillMaxSize(0.55f)
                )
                else -> Text(
                    "?",
                    color = NodeLockedInk,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = (size.value * 0.42f).sp
                )
            }
        }
        // Level number tab.
        Surface(
            shape = RoundedCornerShape(8.dp),
            color = when {
                lvl.isSolved -> DarkBrown
                isCurrent -> Color.White
                else -> NodeLockedTab
            },
            shadowElevation = 2.dp,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .offset(y = 9.dp)
        ) {
            // Just the number, centred; the "?" face already says it is locked.
            Text(
                "${lvl.displayNumber}",
                color = if (isCurrent) PlayNowBrown else Color.White,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .padding(horizontal = 6.dp, vertical = 1.dp)
                    .widthIn(min = 10.dp)
            )
        }
    }
}

/** Stops per row on an island's level path. */
private const val PathPerRow = 4

private val PathScrim = Color(0xFF1E3742)
private val NodeLockedFill = Color(0xFFEFE3CC)
private val NodeLockedRim = Color(0xFFB9A58A)
private val NodeLockedInk = Color(0xFF9C8466)
private val NodeLockedTab = Color(0xFF7A6852)
private val NodeSolvedRim = Color(0xFFFFE3A0)

/**
 * Control point for the road between two island stops: bowed west into open
 * water, since a straight line just cuts across the islands. Works in any
 * units, as long as both stops use the same ones.
 */
private fun roadCtrl(a: Offset, b: Offset): Offset {
    val dx = b.x - a.x
    val dy = b.y - a.y
    return Offset((a.x + b.x) / 2f - dy * 0.22f, (a.y + b.y) / 2f + dx * 0.22f)
}

private fun quadPoint(a: Offset, c: Offset, b: Offset, t: Float): Offset {
    val u = 1f - t
    return Offset(
        u * u * a.x + 2f * u * t * c.x + t * t * b.x,
        u * u * a.y + 2f * u * t * c.y + t * t * b.y
    )
}

/**
 * Where on the road a trip from stop [from] to stop [to] is at [t] (0..1),
 * passing through every island in between, in the same units as [stops].
 */
private fun roadPoint(stops: List<Offset>, from: Int, to: Int, t: Float): Offset {
    if (from == to) return stops[from]
    val legs = kotlin.math.abs(to - from)
    val s = (t.coerceIn(0f, 1f) * legs)
    val k = minOf(s.toInt(), legs - 1)
    val local = s - k
    return if (to > from) {
        val a = stops[from + k]
        val b = stops[from + k + 1]
        quadPoint(a, roadCtrl(a, b), b, local)
    } else {
        // Walking a leg backwards: same curve, run from its far end.
        val a = stops[from - k - 1]
        val b = stops[from - k]
        quadPoint(a, roadCtrl(a, b), b, 1f - local)
    }
}

/**
 * The Kusina Kode chef on the map. At rest they stand still beside the island
 * with the next dish (or on the next level); on a road trip they hop along,
 * facing the way they go.
 */
@Composable
private fun RoadTripChef(
    travelling: Boolean,
    progress: Float,
    legs: Int,
    facingLeft: Boolean,
    modifier: Modifier = Modifier
) {
    // Four hops per leg of road while travelling; still otherwise.
    val hop = if (travelling) {
        -kotlin.math.abs(kotlin.math.sin(progress * kotlin.math.PI.toFloat() * legs * 4f)) * 9f
    } else {
        0f
    }
    Image(
        painter = painterResource(R.drawable.tutorial_chef_idle),
        contentDescription = null,
        contentScale = ContentScale.Fit,
        modifier = modifier
            .size(width = RoadChefWidth, height = RoadChefHeight)
            .graphicsLayer {
                translationY = hop.dp.toPx()
                scaleX = if (facingLeft) -1f else 1f
            }
    )
}

private val RoadBed = Color(0xFF6B3F1F)
private val RoadLine = Color(0xFFFFF1D6)
private val RoadChefWidth = 30.dp
private val RoadChefHeight = 55.dp
/** Where the resting chef stands relative to its island's road stop: left of the pin. */
private val RoadChefRestShift = (-48).dp
/** Time per leg of road; a trip across two legs takes twice as long. */
private const val RoadTripLegMs = 950
private const val RoadTripPrints = 7

/** The archipelago art's own size, to place pins on it wherever it is cropped. */
private const val PhArtWidth = 688f
private const val PhArtHeight = 1024f


/** Where the top panel sits inside the map. */
private val MapSearchTop = 12.dp
/** Room kept at the map's foot for the floating PLAY NOW button. */
private val MapCtaRoom = 76.dp
/** Share of the map's width the PLAY NOW button takes. */
private const val MapCtaWidth = 0.8f
/**
 * The name (20sp line) and count (about 15sp) stack to roughly 35dp, so a 36dp
 * button whose top sits this far above them has its centre between the two lines.
 */
private val PlateArrowNudge = (-1).dp
/** The island pin's fixed size: the widest its pulse ring grows (28 + 26 dp). */
private val PinFootprint = 54.dp
/** How far the button's foot sits inside the bottom bar's padding, to centre it on the wood. */
private val MapCtaDrop = 6.dp



/**
 * The chart lies on a kitchen table: the art is inset from the screen's edges
 * by these, and the pin layer uses the same insets so pins stay on their islands.
 */
private val MapTableSide = 16.dp
private val MapTableTop = 14.dp
/** How far the parchment frame reaches past the art, and its ledge underneath. */
private val MapFrameBorder = 10.dp
private val MapFrameLedge = 6.dp
private val MapArtCorner = 16.dp

private val TablePlankA = Color(0xFF6E3F1E)
private val TablePlankB = Color(0xFF7A4724)
private val TableSeam = Color(0xFF3B1E0C)
private val TableGrain = Color(0xFF9A6236)

/**
 * Warm wooden planks, running across the screen with dark seams and a little
 * grain, darkening toward the edges. Deterministic, so the grain never shifts.
 */
private fun DrawScope.drawKitchenTable() {
    val plank = 58.dp.toPx()
    val rows = (size.height / plank).toInt() + 1
    for (r in 0..rows) {
        val top = r * plank
        drawRect(if (r % 2 == 0) TablePlankA else TablePlankB, Offset(0f, top), Size(size.width, plank))
        // Grain: a few long, faint strokes per plank, placed from the row number.
        for (g in 0 until 4) {
            val y = top + plank * (0.18f + 0.2f * g) + ((r * 7 + g * 3) % 5) * 0.6f.dp.toPx()
            val startX = ((r * 53 + g * 97) % 100) / 100f * size.width * 0.5f
            drawLine(
                TableGrain.copy(alpha = 0.22f),
                Offset(startX, y),
                Offset(startX + size.width * (0.35f + 0.1f * g), y + 1.dp.toPx()),
                strokeWidth = 1.dp.toPx()
            )
        }
        // Butt joint on alternate rows, so the planks read as boards.
        val joint = if (r % 2 == 0) size.width * 0.3f else size.width * 0.72f
        drawLine(TableSeam.copy(alpha = 0.6f), Offset(joint, top), Offset(joint, top + plank), 1.5.dp.toPx())
        drawLine(TableSeam, Offset(0f, top), Offset(size.width, top), 2.dp.toPx())
    }
    // Warm vignette, so the eye settles on the chart in the middle.
    drawRect(
        Brush.radialGradient(
            0f to Color.Transparent,
            0.65f to Color.Transparent,
            1f to Color.Black.copy(alpha = 0.45f),
            center = center,
            radius = size.maxDimension * 0.75f
        )
    )
}

/**
 * The whole map's single top panel, on parchment: the instruction, or while
 * the chef is on the road, where they are headed.
 */
@Composable
private fun MapTopPanel(
    tripTo: Region?,
    modifier: Modifier = Modifier
) {
    ParchmentCard(modifier = modifier, contentPadding = 12.dp) {
        Column {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                modifier = Modifier.align(Alignment.CenterHorizontally)
            ) {
                Box(
                    Modifier
                        .size(24.dp)
                        .clip(CircleShape)
                        .background(PlayNowBrown),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        if (tripTo != null) Icons.Default.LocationOn else Icons.Default.TouchApp,
                        contentDescription = null,
                        tint = PlateCream,
                        modifier = Modifier.size(14.dp)
                    )
                }
                Spacer(Modifier.width(8.dp))
                Text(
                    if (tripTo != null) "Food trip to ${tripTo.displayName}!"
                    else "Tap an island to start your road trip",
                    color = PlateInk,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
            // Clear of the plate's stitched lower edge.
            Spacer(Modifier.height(4.dp))
        }
    }
}

// Inks and trims for the parchment plates on the map.
private val PlateInk = Color(0xFF4A2412)
private val PlateInkSoft = Color(0xFF7A5A40)
private val PlateCream = Color(0xFFFFF1D6)
private val PlateGold = Color(0xFFE8B34A)

/** Margins (dp) around the island stops for the walkthrough's islands frame:
 *  wide enough for the chef on the left and the labels on the right, tall
 *  enough for a pin above its stop and its label below. */
private const val IslandsPadSide = 80f
private const val IslandsPadTop = 64f
private const val IslandsPadBottom = 44f
