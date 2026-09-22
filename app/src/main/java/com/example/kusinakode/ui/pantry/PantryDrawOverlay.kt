package com.example.kusinakode.ui.pantry

import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectHorizontalDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.key
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.rememberUpdatedState
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.clipToBounds
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.compose.ui.zIndex
import kotlin.math.abs
import kotlin.math.roundToInt
import kotlinx.coroutines.delay
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.pantry.DrawResult
import com.example.kusinakode.domain.pantry.Ingredient
import com.example.kusinakode.domain.pantry.PantryEntry
import com.example.kusinakode.domain.pantry.Rarity
import com.example.kusinakode.ui.theme.BeVietnamPro
import kotlin.math.cos
import kotlin.math.sin
import kotlinx.coroutines.launch

private val OverlayInk = Color(0xFF3E2723)
private val OverlayGold = Color(0xFFFFD24A)
private val BaulScrim = Color(0xFC050302)

/** Cell footprint in the ritual grid — the pot art sits inside it. */
private val CellSize = 104.dp
private val PotSize = 96.dp

private enum class BaulBeat { Choose, Opening, Reveal }

/**
 * Full-screen baul ritual. [onSkip] leaves tokens on the server.
 * When called from the pantry, pass [asDialog] so only the reveal card
 * is reused as a dialog after the same pick-one grid.
 *
 * With [oneShot] the ritual ends after a single jar — the win screen hands
 * out one ingredient and any remaining market runs wait in the pantry.
 */
@Composable
fun PantryDrawOverlay(
    drawsAvailable: Int,
    drawing: Boolean,
    reveal: DrawResult?,
    onPick: () -> Unit,
    onDismissReveal: () -> Unit,
    onSkip: () -> Unit,
    asDialog: Boolean = false,
    oneShot: Boolean = false,
    drawingAll: Boolean = false,
    /** Jars fetched so far, and how many the OPEN ALL run is fetching. */
    drawnSoFar: Int = 0,
    drawTarget: Int = 0,
    onOpenAll: (() -> Unit)? = null,
    /** How many bauls this win granted. Shown on the post-game ritual. */
    earnedBauls: Int = drawsAvailable,
    /** Sells the just-drawn duplicate without leaving the reveal. */
    onSellNow: ((DrawResult) -> Unit)? = null,
    sellingNow: Boolean = false,
    haul: List<DrawResult> = emptyList(),
    onDismissHaul: () -> Unit = {}
) {
    HoldsTheScreen()
    var beat by remember { mutableStateOf(BaulBeat.Choose) }
    var chosen by remember { mutableIntStateOf(-1) }
    var sawDrawing by remember { mutableStateOf(false) }
    var openingAll by remember { mutableStateOf(false) }
    var armedAll by remember { mutableStateOf(false) }
    val ctx = LocalContext.current

    LaunchedEffect(beat, reveal?.ingredient?.id) {
        if (beat != BaulBeat.Reveal) return@LaunchedEffect
        val rarity = reveal?.ingredient?.rarity ?: return@LaunchedEffect
        val cue = when (rarity) {
            Rarity.COMMON -> SoundFx.Cue.Coin
            Rarity.UNCOMMON -> SoundFx.Cue.Reveal
            Rarity.RARE -> SoundFx.Cue.Badge
            Rarity.LEGENDARY -> SoundFx.Cue.Win
        }
        SoundFx.play(ctx, cue)
        SoundFx.vibrate(ctx, if (rarity == Rarity.LEGENDARY) 40 else 22)
    }

    // One jar per ritual when oneShot: dismissing the reveal closes the whole thing
    // instead of dealing another grid.
    val closeReveal = {
        onDismissReveal()
        if (!oneShot && drawsAvailable > 0) {
            chosen = -1
            beat = BaulBeat.Choose
        } else {
            onSkip()
        }
    }
    val keepIt by rememberUpdatedState(closeReveal)
    var pendingSell by remember { mutableStateOf<SellAsk?>(null) }
    var trade by remember { mutableStateOf<SellAsk?>(null) }

    LaunchedEffect(drawing) {
        if (drawing) sawDrawing = true
    }

    LaunchedEffect(drawsAvailable, reveal, beat, drawingAll, haul.size) {
        if (beat == BaulBeat.Choose &&
            drawsAvailable <= 0 &&
            reveal == null &&
            !drawingAll &&
            haul.isEmpty()
        ) {
            onSkip()
        }
    }

    LaunchedEffect(drawingAll, openingAll) {
        if (openingAll && drawingAll) armedAll = true
    }

    LaunchedEffect(sawDrawing, drawing, reveal, chosen, beat, openingAll, drawingAll, haul.size, armedAll) {
        if (beat != BaulBeat.Opening) return@LaunchedEffect
        if (openingAll) {
            if (armedAll && !drawingAll && haul.isEmpty()) {
                chosen = -1
                openingAll = false
                armedAll = false
                beat = BaulBeat.Choose
            }
            return@LaunchedEffect
        }
        if (sawDrawing &&
            chosen >= 0 &&
            !drawing &&
            reveal == null
        ) {
            chosen = -1
            beat = BaulBeat.Choose
            sawDrawing = false
        }
    }

    Box(
        Modifier
            .fillMaxSize()
            .background(BaulScrim)
    ) {
        when {
            beat == BaulBeat.Opening -> OpeningBeat(
                chosen = chosen,
                ready = if (openingAll) haul.isNotEmpty() && !drawingAll else reveal != null,
                ingredient = if (openingAll) null else reveal?.ingredient,
                haul = if (openingAll) haul.mapNotNull { it.ingredient } else emptyList(),
                solo = !oneShot,
                // Only an OPEN ALL run has anything to count.
                drawnSoFar = if (openingAll) drawnSoFar else 0,
                drawTarget = if (openingAll) drawTarget else 0,
                onViewReward = {
                    beat = BaulBeat.Reveal
                }
            )
            haul.isNotEmpty() -> {
                HaulReview(
                    haul = haul,
                    onDone = {
                        openingAll = false
                        armedAll = false
                        onDismissHaul()
                        onSkip()
                    }
                )
            }
            reveal != null && beat == BaulBeat.Reveal -> {
                // Keep-it lives on a scrim *behind* the card so Sell can actually
                // receive the tap. A parent clickable used to eat it.
                if (asDialog) {
                    Dialog(
                        onDismissRequest = { },
                        properties = DialogProperties(usePlatformDefaultWidth = false)
                    ) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .background(BaulScrim)
                        ) {
                            Box(
                                Modifier
                                    .fillMaxSize()
                                    .clickable(
                                        indication = null,
                                        interactionSource = remember { MutableInteractionSource() },
                                        onClick = closeReveal
                                    )
                            )
                            PantryRevealCard(
                                result = reveal,
                                onSellNow = onSellNow?.let {
                                    {
                                        reveal.ingredient?.let { ing ->
                                            pendingSell = SellAsk.one(PantryEntry(ing, 1, null))
                                        }
                                    }
                                },
                                selling = sellingNow || trade != null
                            )
                        }
                    }
                } else {
                    Box(Modifier.fillMaxSize()) {
                        Box(
                            Modifier
                                .fillMaxSize()
                                .clickable(
                                    indication = null,
                                    interactionSource = remember { MutableInteractionSource() },
                                    onClick = closeReveal
                                )
                        )
                        PantryRevealCard(
                            result = reveal,
                            onSellNow = onSellNow?.let {
                                {
                                    reveal.ingredient?.let { ing ->
                                        pendingSell = SellAsk.one(PantryEntry(ing, 1, null))
                                    }
                                }
                            },
                            selling = sellingNow || trade != null
                        )
                    }
                }
            }
            else -> ChooseGrid(
                chosen = chosen,
                drawing = drawing,
                oneShot = oneShot,
                drawsAvailable = drawsAvailable,
                earnedBauls = earnedBauls,
                drawingAll = drawingAll,
                onOpenAll = onOpenAll?.let { open ->
                    {
                        if (chosen < 0 && !drawing && !drawingAll && !openingAll) {
                            sawDrawing = false
                            openingAll = true
                            chosen = 0
                            beat = BaulBeat.Opening
                            SoundFx.play(ctx, SoundFx.Cue.Baul)
                            SoundFx.vibrate(ctx, 16)
                            open()
                        }
                    }
                },
                onPick = { index ->
                    if (chosen >= 0 || drawing || drawingAll || openingAll) return@ChooseGrid
                    sawDrawing = false
                    openingAll = false
                    chosen = index
                    beat = BaulBeat.Opening
                    SoundFx.play(ctx, SoundFx.Cue.Baul)
                    SoundFx.vibrate(ctx, 16)
                    onPick()
                }
            )
        }

        val choosing = beat != BaulBeat.Opening &&
            haul.isEmpty() &&
            !(reveal != null && beat == BaulBeat.Reveal) &&
            pendingSell == null &&
            trade == null
        if (beat == BaulBeat.Opening) {
            BackHandler { }
        } else if (choosing && !oneShot) {
            BackHandler { onSkip() }
            Box(
                Modifier
                    .align(Alignment.TopStart)
                    .statusBarsPadding()
                    .padding(16.dp)
                    .zIndex(12f)
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(Color(0xFFF1E6D2))
                    .border(1.dp, Color(0xFFE0C48A), CircleShape)
                    .clickable {
                        SoundFx.play(ctx, SoundFx.Cue.Nav)
                        onSkip()
                    },
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = OverlayInk,
                    modifier = Modifier.size(18.dp)
                )
            }
        }

        pendingSell?.let { ask ->
            SellConfirmDialog(
                ask = ask,
                busy = sellingNow,
                onDismiss = { pendingSell = null },
                onConfirm = {
                    val drawn = reveal ?: return@SellConfirmDialog
                    SoundFx.play(ctx, SoundFx.Cue.Coin)
                    trade = ask
                    pendingSell = null
                    onSellNow?.invoke(drawn)
                }
            )
        }
        trade?.let { ask ->
            SellLoadingOverlay(
                ask = ask,
                busy = sellingNow,
                onFinished = {
                    trade = null
                    keepIt()
                }
            )
        }
    }
}

@Composable
private fun ChooseGrid(
    chosen: Int,
    drawing: Boolean,
    oneShot: Boolean,
    drawsAvailable: Int,
    earnedBauls: Int,
    drawingAll: Boolean,
    onOpenAll: (() -> Unit)?,
    onPick: (Int) -> Unit
) {
    Column(
        Modifier
            .fillMaxSize()
            .padding(20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        // The win is the news; picking is the game. Leading with a rule
        // ("you can only open 1") made a reward read like a restriction.
        if (oneShot) {
            Text(
                "LEVEL CLEAR",
                color = OverlayGold,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 11.sp,
                letterSpacing = 3.4.sp
            )
            Spacer(Modifier.height(8.dp))
        }
        // Shrink-to-fit rather than wrap. At 28sp with 2.2sp of letter spacing,
        // "CONGRATULATIONS!" is wider than a narrow phone, and wider still when
        // the system font scale is turned up — so the trailing "S!" dropped to a
        // second line on some handsets and not others. One line always, at
        // whatever size that takes.
        //
        // Done by hand because autoSize arrived in Compose 1.8 and this project
        // is on the 2024.05 BOM.
        val fullSize = if (oneShot) 28.sp else 20.sp
        var headingSize by remember(oneShot) { mutableStateOf(fullSize) }
        var measured by remember(oneShot) { mutableStateOf(false) }

        Text(
            if (oneShot) "CONGRATULATIONS!" else "CHOOSE 1 OR OPEN ALL",
            color = OverlayGold,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = headingSize,
            letterSpacing = if (oneShot) 2.2.sp else 1.4.sp,
            textAlign = TextAlign.Center,
            maxLines = 1,
            softWrap = false,
            onTextLayout = { layout ->
                // 13sp is the floor: past that the heading stops being a
                // heading, and a phone that narrow has bigger problems.
                if (layout.didOverflowWidth && headingSize > 13.sp) {
                    headingSize = headingSize * 0.94f
                } else {
                    measured = true
                }
            },
            style = TextStyle(
                shadow = Shadow(
                    color = OverlayGold.copy(alpha = 0.55f),
                    offset = Offset.Zero,
                    blurRadius = 22f
                )
            ),
            // Hidden for the frame or two it takes to settle, so the heading
            // does not visibly step down through three sizes on first show.
            modifier = Modifier
                .fillMaxWidth()
                .alpha(if (measured) 1f else 0f)
        )
        Spacer(Modifier.height(10.dp))
        if (oneShot) {
            Surface(
                shape = RoundedCornerShape(50),
                color = PlayNowBrown,
                shadowElevation = 6.dp
            ) {
                Text(
                    if (earnedBauls == 1) "YOU EARNED A PALAYOK"
                    else "YOU EARNED $earnedBauls PALAYOKS",
                    color = Color.White,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 12.sp,
                    letterSpacing = 1.2.sp,
                    modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp)
                )
            }
            Spacer(Modifier.height(10.dp))
            Text(
                when {
                    earnedBauls <= 1 -> "Pick the one you like — it opens right now."
                    earnedBauls == 2 -> "Pick one to open now. The other is waiting on your pantry shelf."
                    else -> "Pick one to open now. The other ${earnedBauls - 1} are waiting on your pantry shelf."
                },
                color = Color(0xFFE8C9A0),
                fontFamily = BeVietnamPro,
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        } else {
            Text(
                "Tap the palayok for one ingredient, or open every remaining run at once.",
                color = Color(0xFFE8C9A0),
                fontSize = 12.sp,
                textAlign = TextAlign.Center,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 16.dp)
            )
        }
        Spacer(Modifier.height(18.dp))
        if (oneShot) {
            Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                (0 until 3).forEach { row ->
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        (0 until 3).forEach { col ->
                            val index = row * 3 + col
                            BobbingBaul(
                                index = index,
                                opened = false,
                                enabled = chosen < 0 && !drawing,
                                onClick = { onPick(index) }
                            )
                        }
                    }
                }
            }
        } else {
            BobbingBaul(
                index = 0,
                opened = drawingAll,
                enabled = chosen < 0 && !drawing && !drawingAll,
                onClick = { onPick(0) },
                cell = 176.dp,
                pot = 168.dp
            )
            if (drawsAvailable > 0) {
                Spacer(Modifier.height(8.dp))
                Text(
                    if (drawsAvailable == 1) "1 palayok waiting"
                    else "$drawsAvailable Palayoks Waiting",
                    color = Color(0xFFE8C9A0),
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    textAlign = TextAlign.Center
                )
            }
        }
        if (!oneShot && onOpenAll != null && (drawsAvailable >= 1 || drawingAll)) {
            Spacer(Modifier.height(16.dp))
            Row(
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = OverlayGold,
                    shadowElevation = 4.dp,
                    modifier = Modifier.clickable(
                        enabled = chosen < 0 && !drawing && !drawingAll,
                        onClick = { onPick(0) }
                    )
                ) {
                    Text(
                        "DRAW 1",
                        color = OverlayInk,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
                Surface(
                    shape = RoundedCornerShape(50),
                    color = if (drawingAll) PlayNowBrown.copy(alpha = 0.5f) else PlayNowBrown,
                    shadowElevation = 6.dp,
                    modifier = Modifier.clickable(enabled = !drawingAll && !drawing, onClick = onOpenAll)
                ) {
                    Text(
                        if (drawingAll) "OPENING…" else "OPEN ALL $drawsAvailable",
                        color = Color.White,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 13.sp,
                        letterSpacing = 1.sp,
                        modifier = Modifier.padding(horizontal = 18.dp, vertical = 10.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun BobbingBaul(
    index: Int,
    opened: Boolean = false,
    enabled: Boolean,
    onClick: () -> Unit,
    cell: Dp = CellSize,
    pot: Dp = PotSize
) {
    val transition = rememberInfiniteTransition(label = "baul_$index")
    val bob by transition.animateFloat(
        initialValue = 0f,
        targetValue = 6f,
        animationSpec = infiniteRepeatable(
            animation = tween(1400 + index * 90, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "bob_$index"
    )
    Box(Modifier.size(cell), contentAlignment = Alignment.Center) {
        PixelArt(
            res = if (opened) R.drawable.baul_open else R.drawable.baul_closed,
            contentDescription = if (opened) "Open palayok" else "Closed palayok",
            modifier = Modifier
                .size(pot)
                .offset(y = bob.dp)
                .clickable(enabled = enabled, onClick = onClick)
        )
    }
}

/**
 * The chosen pot slides from its grid seat to the centre, rattles for
 * about three seconds, then bursts. One ingredient stays on the spotlight
 * for two seconds (or until tapped) before the card opens. OPEN ALL keeps
 * the open palayok and shows each drawn ingredient one by one, bigger,
 * then the cards.
 */
private enum class OpenPhase { Travel, Shake, Burst }

/**
 * How many ingredients the burst parades one at a time before handing over to
 * the haul screen. Everything drawn is still awarded and still listed there.
 */
private const val MAX_PARADED = 6

@Composable
private fun OpeningBeat(
    chosen: Int,
    ready: Boolean,
    ingredient: Ingredient?,
    haul: List<Ingredient> = emptyList(),
    solo: Boolean = false,
    drawnSoFar: Int = 0,
    drawTarget: Int = 0,
    onViewReward: () -> Unit
) {
    val viewReward by rememberUpdatedState(onViewReward)

    // One list for both the parade's sound and its picture.
    //
    // These were two lists that disagreed. The animation stepped through the
    // whole haul and played a coin for every item, while the picture came
    // from haul.take(6) - so opening seven or more jars at once gave a chime
    // and an empty stage from the seventh on. The cap is kept, because the
    // parade holds ~1s per item and an OPEN ALL of twenty would be a
    // twenty-second wait; what it must not do is outlive what is drawn.
    // Nothing is lost by capping: the haul screen after this shows every
    // ingredient, and all of them are already shelved either way.
    val paraded = remember(haul) { haul.take(MAX_PARADED) }
    val haulNow by rememberUpdatedState(paraded)
    var phase by remember { mutableStateOf(if (solo) OpenPhase.Shake else OpenPhase.Travel) }
    var shakeDone by remember { mutableStateOf(false) }
    var featured by remember { mutableIntStateOf(-1) }
    val travel = remember { Animatable(if (solo) 1f else 0f) }
    val potFade = remember { Animatable(1f) }
    val pop = remember { Animatable(0.18f) }
    val lift = remember { Animatable(10f) }
    val burst = remember { Animatable(0f) }
    val ctx = LocalContext.current

    val rattle = rememberInfiniteTransition(label = "rattle")
    val shake by rattle.animateFloat(
        initialValue = -11f,
        targetValue = 11f,
        animationSpec = infiniteRepeatable(
            animation = tween(70, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "shake"
    )

    LaunchedEffect(chosen, solo) {
        if (solo) {
            travel.snapTo(1f)
            phase = OpenPhase.Shake
        } else {
            travel.snapTo(0f)
            travel.animateTo(1f, animationSpec = tween(520, easing = FastOutSlowInEasing))
            phase = OpenPhase.Shake
        }
    }
    // The rattle lasts exactly as long as the pot is rattling.
    //
    // It used to be a one-shot fired when the phase began, which was wrong in
    // both directions. OPEN ALL fetches one jar per network round-trip, so the
    // wait is as long as the player has jars — far longer than any one clip —
    // and the pot went on shaking in silence after it ended. When the wait was
    // short instead, the clip outlived the shake and played over the burst.
    // Re-entering the phase stacked another copy on top, which is the
    // "it keeps making the sound and never opens" case.
    //
    // A loop bound to the phase cannot do any of that: it starts with the
    // shake and onDispose stops it, however long or short the wait turns out
    // to be.
    val shaking = phase == OpenPhase.Shake
    DisposableEffect(shaking) {
        val rattle = if (shaking) SoundFx.loop(ctx, SoundFx.Cue.Shake) else null
        onDispose { rattle?.stop() }
    }

    LaunchedEffect(phase) {
        if (phase != OpenPhase.Shake) return@LaunchedEffect
        var elapsed = 0L
        while (elapsed < 3_000L) {
            SoundFx.vibrate(ctx, 14)
            delay(550)
            elapsed += 550
        }
        shakeDone = true
    }
    LaunchedEffect(shakeDone, ready) {
        if (!shakeDone || !ready) return@LaunchedEffect
        phase = OpenPhase.Burst
        SoundFx.play(ctx, SoundFx.Cue.BaulOpen)
        burst.snapTo(0f)
        pop.snapTo(0.18f)
        lift.snapTo(10f)
        potFade.snapTo(1f)
        kotlinx.coroutines.coroutineScope {
            launch { burst.animateTo(1f, animationSpec = tween(640, easing = FastOutSlowInEasing)) }
            launch { pop.animateTo(1.55f, animationSpec = tween(720, easing = FastOutSlowInEasing)) }
            launch { lift.animateTo(-28f, animationSpec = tween(720, easing = FastOutSlowInEasing)) }
        }
        delay(180)
        if (haulNow.isEmpty()) {
            potFade.animateTo(0f, animationSpec = tween(420, easing = FastOutSlowInEasing))
            delay(2_000)
        } else {
            haulNow.forEachIndexed { i, _ ->
                featured = i
                pop.snapTo(0.22f)
                lift.snapTo(18f)
                kotlinx.coroutines.coroutineScope {
                    launch { pop.animateTo(1.08f, animationSpec = tween(360, easing = FastOutSlowInEasing)) }
                    launch { lift.animateTo(-10f, animationSpec = tween(360, easing = FastOutSlowInEasing)) }
                }
                SoundFx.play(ctx, SoundFx.Cue.Coin)
                delay(980)
            }
            featured = -1
            delay(220)
        }
        viewReward()
    }

    val col = chosen.coerceAtLeast(0) % 3
    val row = chosen.coerceAtLeast(0) / 3
    val step = CellSize + 8.dp
    val grid = step * 3 - 8.dp
    val startX = if (solo) 0.dp else step * (col - 1)
    val startY = if (solo) 0.dp else step * (row - 1)
    val haulItems = paraded
    val t = travel.value
    val bursting = phase == OpenPhase.Burst
    val opened = bursting
    val potGone = potFade.value < 0.08f
    val stage = when {
        haulItems.isNotEmpty() -> 300.dp
        solo -> 220.dp
        else -> grid
    }

    Column(
        Modifier.fillMaxSize(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Box(
            Modifier.size(stage),
            contentAlignment = Alignment.Center
        ) {
            if (!solo) {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    (0 until 3).forEach { r ->
                        Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                            (0 until 3).forEach { c ->
                                val index = r * 3 + c
                                Box(
                                    modifier = Modifier.size(CellSize),
                                    contentAlignment = Alignment.Center
                                ) {
                                    if (index != chosen) {
                                        PixelArt(
                                            res = R.drawable.baul_closed,
                                            contentDescription = null,
                                            modifier = Modifier
                                                .size(PotSize)
                                                .alpha(0.16f * (1f - t))
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            val potSize = PotSize * (1f + 0.36f * t)
            Box(
                Modifier
                    .offset(x = startX * (1f - t), y = startY * (1f - t))
                    .size(CellSize * (1f + 0.55f * t)),
                contentAlignment = Alignment.Center
            ) {
                Box(
                    Modifier
                        .matchParentSize()
                        .graphicsLayer {
                            val glow = 0.55f + burst.value * 0.85f
                            scaleX = glow
                            scaleY = glow
                            alpha = 0.2f + t * 0.25f + burst.value * 0.5f
                        }
                        .background(
                            Brush.radialGradient(
                                colors = listOf(
                                    OverlayGold.copy(alpha = 0.55f),
                                    Color.Transparent
                                )
                            )
                        )
                )
                if (opened) {
                    SparkleRing(
                        progress = burst.value,
                        modifier = Modifier.matchParentSize()
                    )
                }
                if (!potGone) {
                    PixelArt(
                        res = if (opened) R.drawable.baul_open else R.drawable.baul_closed,
                        contentDescription = if (opened) "Open palayok" else "Chosen palayok",
                        modifier = Modifier
                            .size(potSize)
                            .graphicsLayer { alpha = potFade.value }
                            .offset(x = if (shaking) shake.dp else 0.dp),
                        contentScale = ContentScale.Fit
                    )
                }
                if (ingredient != null && opened) {
                    IngredientPhoto(
                        ingredient,
                        Modifier
                            .size(132.dp)
                            .align(Alignment.Center)
                            .offset(y = lift.value.dp)
                            .graphicsLayer {
                                scaleX = pop.value
                                scaleY = pop.value
                                alpha = (pop.value * 1.8f).coerceAtMost(1f)
                            }
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = viewReward
                            )
                    )
                }
                val shown = haulItems.getOrNull(featured)
                if (shown != null && opened) {
                    Column(
                        Modifier
                            .align(Alignment.Center)
                            .zIndex(6f)
                            .clickable(
                                indication = null,
                                interactionSource = remember { MutableInteractionSource() },
                                onClick = viewReward
                            ),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        IngredientPhoto(
                            shown,
                            Modifier
                                .size(176.dp)
                                .offset(y = lift.value.dp)
                                .graphicsLayer {
                                    scaleX = pop.value
                                    scaleY = pop.value
                                    alpha = (pop.value * 1.8f).coerceAtMost(1f)
                                }
                        )
                    }
                }
            }
        }
        Spacer(Modifier.height(18.dp))
        Text(
            when {
                phase == OpenPhase.Travel -> "Chosen."
                // A jar at a time, so say which one. The rattle alone cannot
                // tell the player whether a long OPEN ALL is working or hung.
                shaking && drawTarget > 1 ->
                    "Opening ${(drawnSoFar + 1).coerceAtMost(drawTarget)} of $drawTarget…"
                shaking -> "The palayok is waking up…"
                opened -> "It's opening…"
                else -> "The palayok is deciding…"
            },
            color = Color(0xFFE8C9A0),
            fontFamily = BeVietnamPro,
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.alpha(0.4f + 0.6f * t)
        )
    }
}

/** Eight gold motes thrown clear of the pot's mouth as the lid lets go. */
@Composable
private fun SparkleRing(progress: Float, modifier: Modifier = Modifier) {
    Canvas(modifier) {
        val centre = Offset(size.width / 2f, size.height / 2f)
        val spread = size.minDimension * (0.14f + progress * 0.34f)
        val fade = (1f - progress).coerceIn(0f, 1f)
        repeat(8) { i ->
            val angle = (i * 45f + progress * 22f) * (Math.PI / 180f).toFloat()
            drawCircle(
                color = OverlayGold.copy(alpha = fade * 0.9f),
                radius = size.minDimension * 0.022f * (0.5f + fade),
                center = Offset(
                    centre.x + cos(angle) * spread,
                    centre.y + sin(angle) * spread * 0.8f
                )
            )
        }
    }
}

private data class HaulPose(
    val xDp: Float,
    val yDp: Float,
    val rotation: Float,
    val scale: Float,
    val alpha: Float,
    val z: Float
)

/** visual 0 = front, 1–2 = fan, <0 = flying off the left. */
private fun haulPose(visual: Float): HaulPose {
    val t = visual
    return HaulPose(
        xDp = if (t < 0f) t * 280f else t * 18f,
        yDp = if (t < 0f) abs(t) * 14f else t * 5f,
        rotation = if (t < 0f) t * 20f else -1.4f + t * 6.9f,
        scale = (1f - t.coerceAtLeast(0f) * 0.045f).coerceIn(0.86f, 1.02f),
        alpha = when {
            t < 0f -> (1f + t * 1.2f).coerceIn(0f, 1f)
            t > 2f -> (2.45f - t).coerceIn(0.25f, 1f)
            else -> 1f
        },
        z = 12f - t
    )
}

/**
 * The haul is a finite pile, not a carousel: cards already reviewed sit at
 * negative slots (flown off left) and never come back around.
 */
private fun haulSlot(i: Int, index: Int): Int = i - index

@Composable
private fun HaulReview(
    haul: List<DrawResult>,
    onDone: () -> Unit
) {
    val pages = haul.filter { it.ingredient != null }
    var index by remember { mutableIntStateOf(0) }
    val shift = remember { Animatable(0f) }
    val scope = rememberCoroutineScope()
    val ctx = LocalContext.current
    val density = LocalDensity.current
    val commitAt = 0.2f
    LaunchedEffect(pages.size) {
        if (pages.isEmpty()) onDone()
        if (index >= pages.size) index = 0
    }
    fun settleOrTurn() {
        if (pages.size <= 1 || shift.isRunning) return
        scope.launch {
            // A commit past either end is refused and springs back, so the
            // last card stays put instead of wrapping to the first.
            val target = when {
                shift.value >= commitAt && index < pages.lastIndex -> 1f
                shift.value <= -commitAt && index > 0 -> -1f
                else -> 0f
            }
            if (target != 0f) {
                shift.animateTo(target, tween(420, easing = FastOutSlowInEasing))
                index += target.toInt()
                shift.snapTo(0f)
                SoundFx.tap(ctx)
            } else {
                shift.animateTo(0f, tween(220, easing = FastOutSlowInEasing))
            }
        }
    }
    fun turn(delta: Int) {
        if (pages.size <= 1 || shift.isRunning) return
        val next = index + delta
        if (next !in pages.indices) return
        scope.launch {
            shift.animateTo(delta.toFloat(), tween(420, easing = FastOutSlowInEasing))
            index = next
            shift.snapTo(0f)
            SoundFx.tap(ctx)
        }
    }
    if (pages.isEmpty()) return

    // The card on top decides whether this turn is worth celebrating.
    val facing = pages.getOrNull(index)?.ingredient?.rarity
    val bigPull = facing?.isBigPull == true
    LaunchedEffect(index, bigPull) {
        if (!bigPull) return@LaunchedEffect
        SoundFx.play(ctx, if (facing == Rarity.LEGENDARY) SoundFx.Cue.Win else SoundFx.Cue.Badge)
        SoundFx.vibrate(ctx, if (facing == Rarity.LEGENDARY) 40 else 22)
    }

    Box(Modifier.fillMaxSize()) {
    if (bigPull && facing != null) {
        // Behind the stack, so the light spills out from under the frame.
        key(index) { RarityRays(facing, Modifier.fillMaxSize()) }
    }
    Column(
        Modifier
            .fillMaxSize()
            .padding(horizontal = 16.dp, vertical = 20.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            "YOUR HAUL",
            color = OverlayGold,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 22.sp,
            letterSpacing = 1.6.sp,
            textAlign = TextAlign.Center
        )
        Spacer(Modifier.height(6.dp))
        Text(
            if (pages.size == 1) {
                "This ingredient is on your pantry shelf — sell or keep it."
            } else {
                "Tap or swipe the stack. ${pages.size} ingredients — sell or keep what you want."
            },
            color = Color(0xFFE8C9A0),
            fontSize = 12.sp,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 12.dp)
        )
        Spacer(Modifier.height(16.dp))
        val n = pages.size
        Box(
            Modifier
                .fillMaxWidth()
                .height(470.dp)
                .clipToBounds()
                .padding(end = if (n > 1) 22.dp else 0.dp)
                .pointerInput(index, n) {
                    if (n <= 1) return@pointerInput
                    detectTapGestures(onTap = { turn(1) })
                }
                .pointerInput(index, n) {
                    if (n <= 1) return@pointerInput
                    val widthPx = size.width.toFloat().coerceAtLeast(1f)
                    detectHorizontalDragGestures(
                        onDragEnd = { settleOrTurn() },
                        onHorizontalDrag = { change, amount ->
                            change.consume()
                            if (shift.isRunning) return@detectHorizontalDragGestures
                            // Dead ends: no give forward on the last card, none
                            // backward on the first.
                            val lo = if (index > 0) -0.45f else 0f
                            val hi = if (index < n - 1) 0.45f else 0f
                            scope.launch {
                                shift.snapTo(
                                    (shift.value - amount / widthPx).coerceIn(lo, hi)
                                )
                            }
                        }
                    )
                },
            contentAlignment = Alignment.Center
        ) {
            if (n == 1) {
                HaulStackCard(result = pages[0], face = true)
            } else {
                pages.forEachIndexed { i, item ->
                    val slot = haulSlot(i, index)
                    val visual = slot - shift.value
                    if (visual < -1.15f || visual > 2.35f) return@forEachIndexed
                    val pose = haulPose(visual)
                    val id = "${item.ingredient?.id ?: "haul"}_$i"
                    key(id) {
                        HaulStackCard(
                            result = item,
                            face = visual < 0.55f,
                            modifier = Modifier
                                .graphicsLayer {
                                    val px = density.density
                                    translationX = pose.xDp * px
                                    translationY = pose.yDp * px
                                    rotationZ = pose.rotation
                                    scaleX = pose.scale
                                    scaleY = pose.scale
                                    alpha = pose.alpha
                                    cameraDistance = 18f * px
                                }
                                .zIndex(pose.z)
                        )
                    }
                }
            }
        }
        if (pages.size > 1) {
            Spacer(Modifier.height(12.dp))
            // The turn affordance belongs to the stack, not to one card — the
            // framed card has no room for it and it would sit on the art.
            Text(
                if (index == pages.lastIndex) {
                    "THAT'S THE LOT  ·  ${pages.size} / ${pages.size}"
                } else {
                    "TAP OR SWIPE  ·  ${index + 1} / ${pages.size}"
                },
                color = OverlayGold,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                letterSpacing = 1.sp
            )
        }
        Spacer(Modifier.height(16.dp))
        Surface(
            shape = RoundedCornerShape(50),
            color = PlayNowBrown,
            shadowElevation = 6.dp,
            modifier = Modifier.clickable(onClick = onDone)
        ) {
            Text(
                "SEE PANTRY",
                color = Color.White,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                letterSpacing = 1.sp,
                modifier = Modifier.padding(horizontal = 22.dp, vertical = 10.dp)
            )
        }
    }
    if (bigPull && facing != null) {
        key(index) { RarityConfetti(facing, Modifier.fillMaxSize()) }
    }
    }
}

@Composable
private fun HaulStackCard(
    result: DrawResult,
    face: Boolean,
    modifier: Modifier = Modifier
) {
    val ingredient = result.ingredient ?: return
    // Behind the one being read, only the frame shows — same 5:7 box, so the
    // stack's poses land where they did before.
    if (!face) {
        IngredientCardBack(
            ingredient.rarity,
            modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp)
        )
        return
    }
    IngredientTradingCard(
        ingredient = ingredient,
        headline = if (result.isDuplicate) "DUPLICATE" else "INGREDIENT UNLOCKED!",
        body = ingredient.lore,
        footer = if (result.isDuplicate) {
            "YOU NOW HOLD ${result.qty} · SPARES SELL FOR ${ingredient.rarity.sellValue} KK"
        } else {
            "SHELVED IN YOUR PANTRY"
        },
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp)
    )
}

/**
 * The drawn ingredient, as the same collectible card the haul deals — so the
 * one palayok you open after a win and the pile you open later look like the
 * same game. A rare or legendary pull brings its light and paper with it.
 */
@Composable
fun PantryRevealCard(
    result: DrawResult,
    /** Null hides the shortcut — the shelf is the only place to sell. */
    onSellNow: (() -> Unit)? = null,
    selling: Boolean = false
) {
    val ingredient = result.ingredient ?: return
    val rarity = ingredient.rarity

    Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
        if (rarity.isBigPull) {
            key(ingredient.id) { RarityRays(rarity, Modifier.fillMaxSize()) }
        }
        Column(
            Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            IngredientTradingCard(
                ingredient = ingredient,
                headline = if (result.isDuplicate) "DUPLICATE" else "INGREDIENT UNLOCKED!",
                body = ingredient.lore,
                footer = if (result.isDuplicate) {
                    "YOU NOW HOLD ${result.qty} · SPARES SELL FOR ${rarity.sellValue} KK"
                } else {
                    "SHELVED IN YOUR PANTRY"
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 44.dp)
            )
            // Cashing a card in should not mean hunting for it on a shelf of
            // a hundred jars, so the offer stands on every draw. A first copy
            // says so on the button — the shelf already allows selling the
            // last one, and hiding the option here only hid the feature.
            if (onSellNow != null) {
                Spacer(Modifier.height(16.dp))
                Button(
                    onClick = onSellNow,
                    enabled = !selling,
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlayNowBrown,
                        contentColor = Color.White,
                        disabledContainerColor = PlayNowBrown.copy(alpha = 0.45f),
                        disabledContentColor = Color.White
                    ),
                    shape = RoundedCornerShape(26.dp),
                    contentPadding = PaddingValues(horizontal = 22.dp, vertical = 10.dp),
                    modifier = Modifier
                        .wrapContentWidth()
                        .height(44.dp)
                ) {
                    Text(
                        when {
                            selling -> "SELLING…"
                            else -> "SELL THIS ONLY ONE"
                        },
                        color = Color.White,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 12.sp,
                        letterSpacing = 0.6.sp,
                        textAlign = TextAlign.Center,
                        maxLines = 1
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
            Text(
                if (onSellNow != null) "TAP THE SCREEN TO KEEP IT" else "TAP THE SCREEN",
                color = OverlayGold.copy(alpha = 0.75f),
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.Bold,
                fontSize = 10.sp,
                letterSpacing = 1.8.sp
            )
        }
        if (rarity.isBigPull) {
            key(ingredient.id) { RarityConfetti(rarity, Modifier.fillMaxSize()) }
        }
    }
}
