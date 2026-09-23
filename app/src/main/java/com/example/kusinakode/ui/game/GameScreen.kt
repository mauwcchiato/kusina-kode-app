package com.example.kusinakode.ui.game

import androidx.activity.compose.BackHandler
import androidx.annotation.DrawableRes
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.slideInVertically
import androidx.compose.animation.slideOutVertically
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.interaction.collectIsPressedAsState
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.KeyboardArrowDown
import androidx.compose.material.icons.filled.KeyboardArrowUp
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.AcUnit
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lightbulb
import androidx.compose.material.icons.filled.Map
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.PriorityHigh
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.TrendingUp
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Paid
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.scale
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Shadow
import androidx.compose.ui.graphics.TransformOrigin
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.zIndex
import androidx.compose.ui.graphics.drawscope.rotate
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.ColorFilter
import androidx.compose.ui.graphics.ColorMatrix
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import com.example.kusinakode.ui.components.clickSfx
import com.example.kusinakode.ui.components.byWidth
import com.example.kusinakode.ui.components.readableWidth
import com.example.kusinakode.ui.components.DialogueInk
import com.example.kusinakode.ui.components.dialoguePlate
import com.example.kusinakode.ui.components.ParchmentCard
import com.example.kusinakode.ui.components.PauseMenuButton
import com.example.kusinakode.ui.components.PauseMenuTone
import com.example.kusinakode.CustomKeyboard
import com.example.kusinakode.KusinaSettings
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.KusinaToast
import com.example.kusinakode.SoundFx
import com.example.kusinakode.R
import com.example.kusinakode.domain.RoundScored
import com.example.kusinakode.domain.gamification.PowerUp
import com.example.kusinakode.domain.gamification.PowerUpRules
import com.example.kusinakode.domain.model.TileState
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.CardSurface
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.DarkHintBrown
import com.example.kusinakode.ui.theme.GrayBrown
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.tutorial.HowToPlayContent
import kotlinx.coroutines.delay
import kotlin.random.Random
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.ui.gamification.BadgeArt
import com.example.kusinakode.domain.pantry.PalayokGrantRules
import com.example.kusinakode.ui.pantry.PantryDrawOverlay
import com.example.kusinakode.ui.pantry.PantryViewModel
import com.example.kusinakode.ui.settings.SfxVolumeSlider
import com.example.kusinakode.ui.components.LevelImage

private val BurntOrange = Color(0xFFCC6B1F)
private val GlassDark = Color(0xFF2A1608)
/** Win-stack cards — nearly solid so the blurred photo does not show through. */
private val RewardCardFill = Color(0xF22A1608)

/** First solve of a dish. Replays pay nothing. */
private const val KK_PER_NEW_DISH = 10
/** Each badge minted on this win. */
private const val KK_PER_BADGE = 25

/** KK this round only — never the wallet total. Null until scoring reports. */
private fun kkEarnedThisRound(scored: RoundScored?): Int? {
    if (scored == null) return null
    if (scored.wasReplay) return 0
    return KK_PER_NEW_DISH + KK_PER_BADGE * scored.badges.size
}

/**
 * Frame 7 game screen: region title + level chip, timer chip, attempt dots,
 * glass tiles, GET HINT pill, dark keyboard — plus the Frame 8 full-screen
 * win overlay. All rules live in [GameViewModel]; this only renders state.
 */
@Composable
fun GameScreen(
    uiState: GameUiState,
    onKey: (Char) -> Unit,
    onBackspace: () -> Unit,
    onEnter: () -> Unit,
    onRestart: () -> Unit,
    onReveal: () -> Unit,
    onBomb: () -> Unit,
    onSolve: () -> Unit,
    onDismissPowerUpMessage: () -> Unit,
    onSetPaused: (Boolean) -> Unit,
    onExit: () -> Unit,
    onBackToMap: () -> Unit,
    /** Abandoning a round drops the player home, not back onto the map. */
    onGoHome: () -> Unit,
    /** Null on the final level — the win screen then only offers the map. */
    onPlayNext: (() -> Unit)?,
    onViewDish: () -> Unit,
    /** Kusina Reel — its own row under the reward stack, above Next Level. */
    onOpenDocumentary: () -> Unit = {},
    /** What Module 2 credited for this round; null until it reports. */
    scored: RoundScored? = null
) {
    val ctx = LocalContext.current
    val prefs by KusinaSettings.prefs.collectAsState()
    val level = uiState.level.number
    // The dish's own photograph. Previously this looked up bgN by level
    // number, which silently mismatched once the level list came from the
    // dataset rather than being hand-ordered.
    val bgRes = uiState.level.imageRes

    var showHelp by remember { mutableStateOf(false) }
    var showPause by remember { mutableStateOf(false) }
    /** Guards the way out: the round's clock is running and leaving abandons it. */
    var showConfirmExit by remember { mutableStateOf(false) }
    var dockOpen by remember { mutableStateOf(false) }

    // Let the winning row finish flipping to green (and the losing row finish
    // revealing) before any overlay covers the board.
    val rowRevealMs = remember(uiState.wordLength) {
        TILE_STAGGER_MS * (uiState.wordLength - 1) + TILE_FLIP_MS + TILE_ACCENT_MS
    }
    var showWin by remember { mutableStateOf(false) }
    /** The closing film, shown once after the final dish is cleared. */
    var showFinale by remember { mutableStateOf(false) }
    var showCardReveal by remember { mutableStateOf(false) }
    var showPot by remember { mutableStateOf(false) }
    var showGameOver by remember { mutableStateOf(false) }

    val pantry: PantryViewModel = viewModel(
        key = "pantry_win_$level",
        factory = PantryViewModel.factory(level)
    )
    val pantryUi by pantry.uiState.collectAsState()
    var potPolling by remember(level) { mutableStateOf(false) }
    var potSpent by remember(level) { mutableStateOf(false) }
    var awaitingPot by remember(level) { mutableStateOf(false) }

    // Grant lands a beat after the win posts. Start asking during the
    // heritage card so the pot is ready the moment it is claimed.
    LaunchedEffect(uiState.hasWon, level) {
        if (!uiState.hasWon) return@LaunchedEffect
        potPolling = true
        pantry.load(powerUpsUsed = uiState.powerUpsUsed)
        var tries = 0
        while (tries < 5 && pantry.uiState.value.snapshot.drawsAvailable <= 0) {
            delay(600)
            pantry.load(powerUpsUsed = uiState.powerUpsUsed)
            tries++
        }
        potPolling = false
    }
    val hasJar = !pantryUi.isGuest &&
        (pantryUi.snapshot.drawsAvailable > 0 || pantryUi.drawing || pantryUi.reveal != null)

    fun goToWin() {
        awaitingPot = false
        showPot = false
        showWin = true
    }
    fun finishPot() {
        potSpent = true
        pantry.dismissReveal()
        goToWin()
    }

    val soundOn by KusinaSettings.prefs.collectAsState()
    // isGameOver belongs in here as much as hasWon does. Without it, nudging
    // the volume slider — or anything else that re-runs this — restarted the
    // island bed on top of the Game Over sheet after it had been stopped.
    LaunchedEffect(
        uiState.level.region,
        uiState.hasWon,
        uiState.isGameOver,
        soundOn.soundEffects,
        soundOn.sfxVolume
    ) {
        if (!soundOn.soundEffects || uiState.hasWon || uiState.isGameOver) return@LaunchedEffect
        SoundFx.setBgm(ctx, SoundFx.Bgm.forGame(uiState.level.region))
    }
    LaunchedEffect(uiState.hasWon) {
        if (uiState.hasWon) {
            SoundFx.play(ctx, SoundFx.Cue.Win)
            SoundFx.vibrate(ctx, 40)
            delay(420)
            SoundFx.play(ctx, SoundFx.Cue.Sarap)
            // currentRow stays on the winning guess, so 0 means first try.
            // Loop until Next Level / Back to Map — the sting used to die
            // during the card and pot, so CORRECT! arrived in silence.
            SoundFx.setBgm(ctx, SoundFx.Bgm.forWin(firstTry = uiState.currentRow == 0))
            delay(rowRevealMs + 80L)
            showCardReveal = true
        } else {
            showCardReveal = false
            showPot = false
            awaitingPot = false
            showWin = false
        }
    }
    LaunchedEffect(showWin, uiState.hasWon) {
        if (showWin && uiState.hasWon && soundOn.soundEffects) {
            SoundFx.setBgm(ctx, SoundFx.Bgm.forWin(firstTry = uiState.currentRow == 0))
        }
    }
    LaunchedEffect(uiState.isGameOver) {
        if (uiState.isGameOver) {
            // The island bed stops the moment the round is lost. Winning
            // swaps to a win loop, so a win has continuous audio and feels
            // scored; losing left the same cheerful island music running
            // underneath the Game Over sheet as though nothing had happened.
            SoundFx.stopBgm()
            // The sting used to fire here, at the top of the last row's
            // flip — competing with the tile sounds, and finished well
            // before the sheet appeared. It belongs on the sheet.
            delay(rowRevealMs + 250L)
            showGameOver = true
            SoundFx.setBgm(ctx, SoundFx.Bgm.GameOver)
            SoundFx.vibrate(ctx, 40)
        } else {
            showGameOver = false
        }
    }
    LaunchedEffect(showGameOver, uiState.isGameOver, soundOn.soundEffects) {
        if (showGameOver && uiState.isGameOver && soundOn.soundEffects) {
            SoundFx.setBgm(ctx, SoundFx.Bgm.GameOver)
        }
    }
    var judgedRow by remember { mutableIntStateOf(uiState.currentRow) }
    LaunchedEffect(uiState.currentRow, uiState.hasWon, uiState.isGameOver) {
        if (uiState.hasWon || uiState.isGameOver) {
            judgedRow = uiState.currentRow
            return@LaunchedEffect
        }
        if (uiState.currentRow > judgedRow) {
            val row = uiState.tileStates.getOrNull(judgedRow).orEmpty()
            val greens = row.count { it == TileState.Correct }
            val ambers = row.count { it == TileState.SemiCorrect }
            delay(rowRevealMs.toLong())
            // Last remaining try is only "Last Taste". The usual coaching
            // line used to fire on the same beat and talk over it.
            if (uiState.currentRow == uiState.maxAttempts - 1) {
                SoundFx.play(ctx, SoundFx.Cue.LastTaste)
            } else {
                when {
                    greens >= 2 -> SoundFx.play(ctx, SoundFx.Cue.Nice)
                    ambers >= 2 -> SoundFx.play(ctx, SoundFx.Cue.Malapit)
                    else -> SoundFx.play(ctx, SoundFx.Cue.KeepGoing)
                }
            }
        }
        judgedRow = uiState.currentRow
    }

    Box(Modifier.fillMaxSize()) {
        // A regional motif, not the dish. The photograph used to sit here and
        // simply gave the answer away.
        RegionBackdrop(uiState.level.region, Modifier.fillMaxSize())
        // Vignette, so the weave sits back and the board reads.
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to GlassDark.copy(alpha = 0.55f),
                        0.35f to GlassDark.copy(alpha = 0.25f),
                        0.7f to GlassDark.copy(alpha = 0.5f),
                        1f to GlassDark.copy(alpha = 0.88f)
                    )
                )
        )

        // The round is one column, capped on a big screen.
        //
        // The wood table still fills the display, but the board, dock and
        // keyboard stay together. Left uncapped on an unfolded foldable the
        // keyboard stretched the full 841dp while the board sat at phone
        // size in the middle of it, so the two halves of the same screen
        // looked unrelated.
        Column(
            Modifier
                .readableWidth(560.dp)
                .fillMaxHeight()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(horizontal = 14.dp)
                .align(Alignment.TopCenter),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            // ---- Header. Title is centered on the screen, not on the space
            // left between the buttons, so it never drifts off-axis. ----
            Box(
                Modifier
                    .fillMaxWidth()
                    .padding(top = 6.dp)
            ) {
                Column(
                    Modifier
                        .align(Alignment.Center)
                        .padding(horizontal = 104.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        uiState.level.region.displayName,
                        color = Color.White,
                        style = MaterialTheme.typography.titleLarge.copy(
                            fontWeight = FontWeight.ExtraBold,
                            shadow = Shadow(Color.Black.copy(alpha = 0.6f), Offset(0f, 3f), blurRadius = 8f)
                        ),
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    Text(
                        "LEVEL $level",
                        color = LightOrange,
                        fontSize = 10.sp,
                        fontWeight = FontWeight.Black,
                        letterSpacing = 2.sp,
                        maxLines = 1
                    )
                }

                CircleIconButton(
                    onClick = {
                        // Stop the clock while the question is on screen —
                        // asking "are you sure" and charging the player time to
                        // answer it would be its own small betrayal.
                        onSetPaused(true)
                        showConfirmExit = true
                    },
                    modifier = Modifier.align(Alignment.CenterStart)
                ) {
                    Icon(
                        Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back to levels",
                        tint = Color.White,
                        modifier = Modifier.size(20.dp)
                    )
                }

                Row(
                    Modifier.align(Alignment.CenterEnd),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    CircleIconButton(onClick = {
                        onSetPaused(true)
                        showPause = true
                    }) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = "Pause",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(Modifier.width(8.dp))
                    CircleIconButton(onClick = { showHelp = true }) {
                        Text("?", color = Color.White, style = MaterialTheme.typography.titleMedium)
                    }
                }
            }

            Spacer(Modifier.height(10.dp))

            // ---- One HUD strip: timer + attempts remaining ----
            val showTimer = prefs.showTimer
            val reduceMotion = prefs.reduceMotion
            Surface(shape = RoundedCornerShape(18.dp), color = Color.Black.copy(alpha = 0.35f)) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (showTimer) {
                        Icon(
                            Icons.Default.Timer,
                            contentDescription = null,
                            tint = LightOrange,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "%02d:%02d".format(uiState.elapsedSeconds / 60, uiState.elapsedSeconds % 60),
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 15.sp,
                            letterSpacing = 1.5.sp
                        )
                        Spacer(Modifier.width(10.dp))
                    }
                    Box(
                        Modifier
                            .width(1.dp)
                            .height(14.dp)
                            .background(Color.White.copy(alpha = 0.25f))
                    )
                    Spacer(Modifier.width(10.dp))
                    Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                        val attemptPulse by rememberInfiniteTransition(label = "attempt").animateFloat(
                            initialValue = 0.72f,
                            targetValue = 1f,
                            animationSpec = infiniteRepeatable(tween(900), RepeatMode.Reverse),
                            label = "attempt_pulse"
                        )
                        repeat(uiState.maxAttempts) { i ->
                            val current = i == uiState.currentRow && uiState.isRoundActive
                            Box(
                                Modifier
                                    .size(if (current) 8.dp else 6.dp)
                                    .graphicsLayer {
                                        scaleX = if (current && !reduceMotion) attemptPulse else 1f
                                        scaleY = if (current && !reduceMotion) attemptPulse else 1f
                                    }
                                    .clip(CircleShape)
                                    .background(
                                        when {
                                            i < uiState.currentRow -> BurntOrange
                                            current -> LightOrange
                                            else -> Color.White.copy(alpha = 0.28f)
                                        }
                                    )
                            )
                        }
                    }
                }
            }

            // ---- Coaching line. The slot keeps its height whether or not a
            // message is showing, so the grid never jumps. ----
            var lastCoaching by remember { mutableStateOf("") }
            var lastTone by remember { mutableStateOf(CoachTone.Progress) }
            LaunchedEffect(uiState.encouragement) {
                uiState.encouragement?.let {
                    lastCoaching = it
                    lastTone = uiState.encouragementTone
                }
            }
            val coachingVisible = uiState.encouragement != null && uiState.isRoundActive
            val coachingAlpha by animateFloatAsState(
                targetValue = if (coachingVisible) 1f else 0f,
                animationSpec = tween(320),
                label = "coach_fade"
            )
            // A fresh line springs in rather than fading up, so a verdict that
            // lands while the player is staring at the board still registers.
            val pop = remember { Animatable(1f) }
            LaunchedEffect(uiState.encouragement) {
                if (uiState.encouragement != null) {
                    pop.snapTo(0.86f)
                    pop.animateTo(
                        1f,
                        spring(dampingRatio = 0.42f, stiffness = Spring.StiffnessMediumLow)
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxWidth()
                    .heightIn(min = 72.dp)
                    .padding(horizontal = 12.dp),
                contentAlignment = Alignment.Center
            ) {
                CoachBanner(
                    text = lastCoaching,
                    tone = lastTone,
                    modifier = Modifier
                        .alpha(coachingAlpha)
                        .graphicsLayer {
                            scaleX = pop.value
                            scaleY = pop.value
                        }
                )
            }

            val rowFull = uiState.isRoundActive &&
                uiState.grid.getOrNull(uiState.currentRow)?.none { it == ' ' } == true

            // ENTER used to pulse for as long as a row sat full, which read as
            // nagging. Now it waits for the player to actually stall: a full row
            // and no input for a few seconds. Any keystroke resets the wait.
            var idle by remember { mutableStateOf(false) }
            val typedSoFar = uiState.grid.getOrNull(uiState.currentRow)?.joinToString("").orEmpty()
            LaunchedEffect(typedSoFar, rowFull, uiState.currentRow) {
                idle = false
                if (!rowFull) return@LaunchedEffect
                delay(2600)
                idle = true
            }
            val rowShake = remember { Animatable(0f) }
            var shakeToken by remember { mutableIntStateOf(0) }
            LaunchedEffect(shakeToken, reduceMotion) {
                if (shakeToken == 0 || reduceMotion) return@LaunchedEffect
                repeat(3) {
                    rowShake.animateTo(8f, tween(45))
                    rowShake.animateTo(-8f, tween(45))
                }
                rowShake.animateTo(0f, tween(50))
            }

            // ---- Grid — sized from the space actually left over, so it can
            // never overflow the header/keyboard on any phone size ----
            BoxWithConstraints(
                Modifier
                    .fillMaxWidth()
                    .weight(1f)
                    // Side margin the grid can rely on. Without it the tile size
                    // was computed from the whole screen width, so a 6-letter
                    // dish ran to both edges with nothing left for the active
                    // row's border to sit in.
                    .padding(horizontal = 18.dp, vertical = 4.dp),
                contentAlignment = Alignment.Center
            ) {
                val gridGap = 6.dp
                // Every row below carries this much padding on each side. It
                // used to be left out of the sums, so the column measured
                // taller than the box it sits in and the last row was clipped
                // by 2dp per row — 12dp on a six-row board.
                val rowPad = 1.dp
                val cols = uiState.wordLength
                val rows = uiState.maxAttempts
                val tileByWidth = (maxWidth - rowPad * 2 - gridGap * (cols - 1)) / cols
                val tileByHeight =
                    (maxHeight - gridGap * (rows - 1) - rowPad * 2 * rows) / rows
                // No floor: forcing a minimum is what overflows a short screen,
                // and a clipped row is worse than a small one.
                //
                // The ceiling is per width class. 52dp is right for a phone,
                // where it is the height that runs out first, but on an
                // unfolded foldable or a tablet both axes have room to spare
                // and the board sat at phone size marooned in the middle of
                // the screen. Still a ceiling rather than "fill": a board
                // stretched to 800dp would put the keyboard and the grid an
                // uncomfortable distance apart.
                val tileCeiling = byWidth(
                    compact = 52.dp,
                    medium = 72.dp,
                    expanded = 88.dp
                )
                val tileSize = minOf(tileByWidth, tileByHeight)
                    .coerceIn(12.dp, tileCeiling)
                Column(verticalArrangement = Arrangement.spacedBy(gridGap)) {
                    uiState.grid.forEachIndexed { r, row ->
                        val isActive = r == uiState.currentRow && uiState.isRoundActive
                        Row(
                            horizontalArrangement = Arrangement.spacedBy(gridGap),
                            modifier = Modifier
                                .graphicsLayer {
                                    translationX = if (isActive) rowShake.value else 0f
                                }
                                .padding(1.dp)
                        ) {
                            row.forEachIndexed { c, letter ->
                                PlayTile(
                                    letter = letter,
                                    state = uiState.tileStates[r][c],
                                    revealDelayMs = c * TILE_STAGGER_MS,
                                    revealBurst = r == uiState.currentRow && c == uiState.justRevealed,
                                    tileSize = tileSize,
                                    row = r,
                                    col = c,
                                    cols = cols,
                                    roundKey = uiState.level.number,
                                    reduceMotion = reduceMotion
                                )
                            }
                        }
                    }
                }
            }

            // ---- Power-up dock: a satchel that springs open ----
            PowerUpDock(
                expanded = dockOpen,
                previewKey = level,
                onToggle = {
                    SoundFx.play(ctx, SoundFx.Cue.PowerUpOpen)
                    SoundFx.vibrate(ctx, 10)
                    dockOpen = !dockOpen
                },
                pointsBalance = uiState.pointsBalance,
                roundActive = uiState.isRoundActive,
                fullyRevealed = uiState.fullyRevealed,
                revealsLeft = uiState.revealsLeft,
                bombUsed = uiState.bombUsed,
                cooldownMs = uiState.hintCooldownRemainingMs,
                reduceMotion = reduceMotion,
                onReveal = {
                    SoundFx.play(ctx, SoundFx.Cue.Reveal)
                    SoundFx.vibrate(ctx, 18)
                    dockOpen = false
                    onReveal()
                },
                onBomb = {
                    SoundFx.play(ctx, SoundFx.Cue.Bomb)
                    SoundFx.vibrate(ctx, 28)
                    dockOpen = false
                    onBomb()
                },
                onSolve = {
                    SoundFx.play(ctx, SoundFx.Cue.Solve)
                    SoundFx.vibrate(ctx, 22)
                    dockOpen = false
                    onSolve()
                }
            )

            Spacer(Modifier.height(10.dp))

            // ---- Keyboard ----
            Box(Modifier.alpha(if (!uiState.isRoundActive) 0.4f else 1f)) {
                CustomKeyboard(
                    keyStates = uiState.keyStates,
                    pulseEnter = idle && !reduceMotion,
                    onKeyClick = { ch ->
                        SoundFx.play(ctx, SoundFx.Cue.Key)
                        onKey(ch)
                    },
                    onBackspace = {
                        SoundFx.play(ctx, SoundFx.Cue.Backspace)
                        onBackspace()
                    },
                    onEnter = {
                        if (!rowFull && uiState.isRoundActive) {
                            shakeToken++
                            SoundFx.play(ctx, SoundFx.Cue.Invalid)
                            SoundFx.vibrate(ctx, 16)
                        } else if (uiState.isRoundActive) {
                            SoundFx.play(ctx, SoundFx.Cue.Submit)
                        }
                        onEnter()
                    },
                    justBombed = uiState.justBombed
                )
            }
        }

        // Power-up feedback, auto-dismissed so it never sits on the board.
        uiState.powerUpMessage?.let { message ->
            LaunchedEffect(message) {
                KusinaToast.show(ctx, message)
                onDismissPowerUpMessage()
            }
        }

        // ---- Pause veil over the board: no studying the grid off the clock ----
        if (uiState.isPaused && uiState.isRoundActive) {
            Box(
                Modifier
                    .fillMaxSize()
                    .background(GlassDark.copy(alpha = 0.88f)),
                contentAlignment = Alignment.Center
            ) {
                if (!showPause) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Icon(
                            Icons.Default.Pause,
                            contentDescription = null,
                            tint = LightOrange,
                            modifier = Modifier.size(46.dp)
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            "PAUSED",
                            color = LightOrange,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            letterSpacing = 4.sp
                        )
                    }
                }
            }
        }

        // The gesture and the arrow are the same intent, so they ask the same
        // question. Registered before the overlays below, which means any open
        // dialog's own handler still wins.
        BackHandler(enabled = !showPause && !showHelp && !showConfirmExit) {
            onSetPaused(true)
            showConfirmExit = true
        }

        // ---- Pause menu ----
        if (showPause) {
            Dialog(onDismissRequest = { showPause = false; onSetPaused(false) }) {
                ParchmentCard {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "PAUSED",
                            color = DarkBrown,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 4.sp
                        )
                        Spacer(Modifier.height(2.dp))
                        Text(
                            "Level ${uiState.level.number}  ·  ${uiState.level.region.displayName}",
                            color = GrayBrown,
                            fontFamily = BeVietnamPro,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.SemiBold,
                            letterSpacing = 0.4.sp
                        )
                        Spacer(Modifier.height(16.dp))
                        SfxVolumeSlider(mutedColor = GrayBrown)
                        Spacer(Modifier.height(16.dp))

                        PauseMenuButton(
                            "Resume",
                            Icons.Default.PlayArrow,
                            tone = PauseMenuTone.Primary
                        ) {
                            showPause = false
                            onSetPaused(false)
                        }
                        Spacer(Modifier.height(9.dp))
                        PauseMenuButton("Restart Round", Icons.Default.Refresh) {
                            showPause = false
                            onSetPaused(false)
                            onRestart()
                        }
                        Spacer(Modifier.height(9.dp))
                        PauseMenuButton("How to Play", Icons.Default.Lightbulb) {
                            showPause = false
                            showHelp = true
                        }
                        Spacer(Modifier.height(9.dp))
                        PauseMenuButton("Back to Map", Icons.Default.Map) {
                            showPause = false
                            onSetPaused(false)
                            onExit()
                        }
                    }
                }
            }
            BackHandler { showPause = false; onSetPaused(false) }
        }

        // ---- Leave-the-round confirmation ----
        if (showConfirmExit) {
            val stay = {
                showConfirmExit = false
                onSetPaused(false)
            }
            Dialog(onDismissRequest = stay) {
                ParchmentCard {
                    Column(
                        Modifier.fillMaxWidth(),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Text(
                            "LEAVE ROUND?",
                            color = DarkBrown,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 22.sp,
                            letterSpacing = 3.sp
                        )
                        Spacer(Modifier.height(6.dp))
                        Text(
                            "Level ${uiState.level.number} is still cooking. " +
                                "You can start it again anytime.",
                            color = GrayBrown,
                            fontFamily = BeVietnamPro,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            textAlign = TextAlign.Center
                        )
                        Spacer(Modifier.height(16.dp))

                        PauseMenuButton(
                            "Keep Playing",
                            Icons.Default.PlayArrow,
                            tone = PauseMenuTone.Primary
                        ) {
                            stay()
                        }
                        Spacer(Modifier.height(9.dp))
                        PauseMenuButton(
                            "Leave Round",
                            Icons.AutoMirrored.Filled.ArrowBack
                        ) {
                            showConfirmExit = false
                            onSetPaused(false)
                            onGoHome()
                        }
                    }
                }
            }
            BackHandler(onBack = stay)
        }

        // ---- Help dialog ----
        if (showHelp) {
            Dialog(onDismissRequest = { showHelp = false }) {
                ParchmentCard {
                    Column(
                        Modifier
                            .fillMaxWidth()
                            .heightIn(max = 540.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        Column(
                            modifier = Modifier
                                .weight(1f, fill = false)
                                .verticalScroll(rememberScrollState())
                        ) {
                            HowToPlayContent()
                        }
                        Spacer(Modifier.height(16.dp))
                        PauseMenuButton(
                            "OK",
                            tone = PauseMenuTone.Primary,
                            compact = true
                        ) {
                            showHelp = false
                        }
                    }
                }
            }
            BackHandler { showHelp = false }
        }

        // Swallow Back during the reveal so the result can't be skipped away.
        if (!uiState.isRoundActive && !showWin && !showGameOver) {
            BackHandler { }
        }

        // Heritage card → pot → win screen. The baul used to wait until
        // NEXT LEVEL, which buried it under a score the player was leaving.
        if (showCardReveal) {
            HeritageCardReveal(
                levelNumber = uiState.level.number,
                dishName = uiState.level.displayName,
                cardRes = uiState.level.cardRes,
                cardUrl = uiState.level.cardUrl,
                onClaim = {
                    showCardReveal = false
                    if (potSpent || (!potPolling && !hasJar)) {
                        goToWin()
                    } else {
                        awaitingPot = true
                        if (!potPolling && hasJar) showPot = true
                    }
                }
            )
            BackHandler { }
        }

        if (awaitingPot && !showPot && !showWin) {
            Box(Modifier.fillMaxSize().background(Color.Black.copy(alpha = 0.90f)))
            LaunchedEffect(potPolling, hasJar, potSpent) {
                if (potPolling) return@LaunchedEffect
                awaitingPot = false
                if (!potSpent && hasJar) showPot = true else goToWin()
            }
        }

        if (showPot && !showWin) {
            PantryDrawOverlay(
                drawsAvailable = pantryUi.snapshot.drawsAvailable,
                drawing = pantryUi.drawing,
                reveal = pantryUi.reveal,
                onPick = pantry::draw,
                onDismissReveal = pantry::dismissReveal,
                onSkip = { finishPot() },
                oneShot = true,
                earnedBauls = PalayokGrantRules.PER_WIN,
                // The palayok opened straight after a win reveals a card the
                // same way the pantry does, so it offers the same shortcut.
                onSellNow = { drawn -> drawn.ingredient?.let { pantry.sell(it.id, 1) } },
                sellingNow = pantryUi.sellingId != null
            )
            BackHandler { }
        }

        // The last dish has no next level, which is what marks the game as
        // finished. Leaving the win screen then plays the curtain call once
        // before handing back to the map.
        if (showFinale) {
            FinaleScreen(
                onFinish = {
                    showFinale = false
                    SoundFx.stopBgm()
                    onBackToMap()
                }
            )
            BackHandler { }
        }

        if (showWin && !showFinale) {
            WinOverlay(
                uiState = uiState,
                bgRes = bgRes,
                bgUrl = uiState.level.imageUrl,
                scored = scored,
                onViewDish = onViewDish,
                onOpenDocumentary = onOpenDocumentary,
                onPlayNext = onPlayNext,
                onBackToMap = {
                    if (onPlayNext == null) {
                        // Nothing left to play: send them off with the film.
                        showFinale = true
                    } else {
                        SoundFx.stopBgm()
                        onBackToMap()
                    }
                }
            )
            BackHandler { }
        }

        // ---- Game over ----
        if (showGameOver) {
            GameOverOverlay(
                uiState = uiState,
                onRestart = onRestart,
                onBackToMap = onExit
            )
            BackHandler { }
        }
    }
}

@Composable
private fun GameOverOverlay(
    uiState: GameUiState,
    onRestart: () -> Unit,
    onBackToMap: () -> Unit
) {
    val ctx = LocalContext.current
    Box(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(
                    listOf(Color(0xFF1A0C05), GlassDark, Color(0xFF3D1A0A))
                )
            )
    ) {
        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Image(
                painter = painterResource(R.drawable.sad_chef),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(128.dp)
            )
            Spacer(Modifier.height(18.dp))
            Text(
                "GAME OVER",
                color = LightOrange,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
            )
            Spacer(Modifier.height(22.dp))

            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RewardChip(
                    Icons.Default.Timer,
                    "%02d:%02d".format(uiState.elapsedSeconds / 60, uiState.elapsedSeconds % 60),
                    "Time",
                    pending = false,
                    Modifier.weight(1f)
                )
                RewardChip(
                    Icons.Default.Extension,
                    "${uiState.currentRow + 1}/${uiState.maxAttempts}",
                    "Tries",
                    pending = false,
                    Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RewardChip(
                    Icons.Default.Bolt,
                    "+0",
                    "Points",
                    pending = true,
                    Modifier.weight(1f)
                )
                RewardChip(
                    Icons.Default.Paid,
                    "+0",
                    "Earned KK",
                    pending = true,
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = Color.White.copy(alpha = 0.10f),
                border = BorderStroke(1.dp, LightOrange.copy(alpha = 0.28f)),
                modifier = Modifier.fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = LightOrange,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "NO REWARDS",
                            color = LightOrange,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Out of tries · Solve it to earn KK",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))
            Button(
                onClick = clickSfx {
                    SoundFx.tap(ctx)
                    onRestart()
                },
                // The same button the win sheet ends on, so losing and winning
                // leave by a door that looks the same: PlayNowBrown rather
                // than the brighter BurntOrange, and no icon. The refresh
                // glyph made this read as a minor utility next to NEXT LEVEL,
                // which is plain type.
                colors = ButtonDefaults.buttonColors(
                    containerColor = PlayNowBrown,
                    contentColor = Color.White
                ),
                shape = RoundedCornerShape(26.dp),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(52.dp)
            ) {
                Text("RETRY LEVEL", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
            }
            Spacer(Modifier.height(10.dp))
            OutlinedButton(
                onClick = clickSfx(onBackToMap),
                shape = RoundedCornerShape(26.dp),
                border = BorderStroke(1.dp, LightOrange.copy(alpha = 0.5f)),
                colors = ButtonDefaults.outlinedButtonColors(
                    containerColor = Color.White.copy(alpha = 0.08f),
                    contentColor = LightOrange
                ),
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp)
            ) {
                Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(17.dp))
                Spacer(Modifier.width(8.dp))
                Text("Back to Map", fontWeight = FontWeight.SemiBold)
            }
        }
    }
}

@Composable
private fun WinOverlay(
    uiState: GameUiState,
    bgRes: Int,
    /** Server-hosted photo for a panel-added level; null for packaged ones. */
    bgUrl: String? = null,
    scored: RoundScored?,
    onViewDish: () -> Unit,
    onOpenDocumentary: () -> Unit,
    onPlayNext: (() -> Unit)?,
    onBackToMap: () -> Unit
) {
    val ctx = LocalContext.current
    Box(Modifier.fillMaxSize().background(GlassDark)) {
        // Frosted level photo — fully covers the board underneath. A level the
        // panel added has no packaged photo, so this reads the uploaded one;
        // without it the backdrop was the placeholder, blurred into a flat wash.
        LevelImage(
            url = bgUrl,
            fallback = bgRes,
            contentDescription = null,
            modifier = Modifier
                .fillMaxSize()
                .blur(22.dp),
            contentScale = ContentScale.Crop
        )
        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        0f to GlassDark.copy(alpha = 0.78f),
                        0.5f to GlassDark.copy(alpha = 0.66f),
                        1f to GlassDark.copy(alpha = 0.88f)
                    )
                )
        )
        // Celebrate first, then bring in the numbers.
        val emblemScale = remember { Animatable(0.35f) }
        var detailsIn by remember { mutableStateOf(false) }
        LaunchedEffect(Unit) {
            emblemScale.animateTo(
                1f,
                spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessLow)
            )
            delay(420)
            detailsIn = true
        }
        val detailsAlpha by animateFloatAsState(
            targetValue = if (detailsIn) 1f else 0f,
            animationSpec = tween(500),
            label = "win_details_fade"
        )
        val detailsLift by animateFloatAsState(
            targetValue = if (detailsIn) 0f else 40f,
            animationSpec = tween(500),
            label = "win_details_lift"
        )

        Column(
            Modifier
                .fillMaxSize()
                .statusBarsPadding()
                .navigationBarsPadding()
                .padding(20.dp)
                .verticalScroll(rememberScrollState()),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            // Brand emblem
            Box(
                Modifier
                    .graphicsLayer {
                        scaleX = emblemScale.value
                        scaleY = emblemScale.value
                    }
                    .size(120.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(LightOrange)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(20.dp))
                    .background(DarkBrown)
                    .padding(5.dp)
                    .clip(RoundedCornerShape(16.dp))
                    .background(LightOrange)
                    .padding(10.dp),
                contentAlignment = Alignment.Center
            ) {
                Image(
                    painter = painterResource(R.drawable.kk_logo),
                    contentDescription = null,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.height(18.dp))
            Text(
                "CORRECT!",
                color = LightOrange,
                style = MaterialTheme.typography.headlineMedium.copy(
                    fontWeight = FontWeight.Black,
                    letterSpacing = 3.sp
                )
            )
            Text(
                uiState.level.displayName.uppercase(),
                color = Color.White.copy(alpha = 0.9f),
                style = MaterialTheme.typography.titleMedium.copy(letterSpacing = 4.sp)
            )
            Spacer(Modifier.height(22.dp))

            Column(
                Modifier.graphicsLayer {
                    alpha = detailsAlpha
                    translationY = detailsLift
                },
                horizontalAlignment = Alignment.CenterHorizontally
            ) {

            // Reward grid — points and KK earned this round (not wallet total).
            val earnedKk = kkEarnedThisRound(scored)
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                RewardChip(
                    Icons.Default.Timer,
                    "%02d:%02d".format(uiState.elapsedSeconds / 60, uiState.elapsedSeconds % 60),
                    "Time", pending = false, Modifier.weight(1f)
                )
                RewardChip(
                    Icons.Default.Extension,
                    "${uiState.currentRow + 1}/${uiState.maxAttempts}",
                    "Tries", pending = false, Modifier.weight(1f)
                )
            }
            Spacer(Modifier.height(12.dp))
            Row(horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                // Points and KK are both live; the banner narrates the mint itself.
                RewardChip(
                    icon = Icons.Default.Bolt,
                    value = when {
                        scored == null -> "…"
                        scored.wasReplay -> "0"
                        else -> "+${scored.pointsAwarded}"
                    },
                    label = if (scored?.wasReplay == true) "Already solved" else "Points",
                    pending = scored == null || scored.wasReplay,
                    modifier = Modifier.weight(1f)
                )
                RewardChip(
                    Icons.Default.Paid,
                    earnedKk?.let { "+$it" } ?: "…",
                    "Earned KK",
                    pending = earnedKk == null || earnedKk == 0,
                    Modifier.weight(1f)
                )
            }

            Spacer(Modifier.height(14.dp))
            WinRewardStack(
                rewardCount = 1 + (scored?.badges?.size ?: 0),
                onExpandSound = { SoundFx.tap(ctx) }
            ) {
                WinUnlockRow(
                    kicker = "KK EARNED",
                    title = "Solve a New Dish",
                    subtitle = "Once per dish",
                    amountLabel = if (scored?.wasReplay == true) "+0 KK" else "+$KK_PER_NEW_DISH KK",
                    leading = {
                        Image(
                            painter = painterResource(R.drawable.ic_kk_pixel),
                            contentDescription = "KK",
                            contentScale = ContentScale.Fit,
                            modifier = Modifier.size(44.dp)
                        )
                    }
                )
                scored?.badges?.forEach { badge ->
                    Spacer(Modifier.height(10.dp))
                    WinUnlockRow(
                        kicker = "BADGE UNLOCKED",
                        title = badge.title,
                        subtitle = badge.milestoneCriteria,
                        amountLabel = "+$KK_PER_BADGE KK",
                        leading = {
                            val medal = BadgeArt.forBadge(badge.badgeId)
                            if (medal != null) {
                                Image(
                                    painter = painterResource(medal),
                                    contentDescription = badge.title,
                                    contentScale = ContentScale.Fit,
                                    modifier = Modifier.size(44.dp)
                                )
                            } else {
                                Icon(
                                    Icons.Default.MilitaryTech,
                                    contentDescription = null,
                                    tint = TileSemiYellow,
                                    modifier = Modifier.size(22.dp)
                                )
                            }
                        }
                    )
                }
            }

            Spacer(Modifier.height(12.dp))
            Surface(
                shape = RoundedCornerShape(18.dp),
                color = RewardCardFill,
                border = BorderStroke(1.dp, LightOrange.copy(alpha = 0.55f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onViewDish() }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Box(
                        Modifier
                            .size(46.dp)
                            .clip(RoundedCornerShape(12.dp))
                            .background(CardSurface)
                    ) {
                        LevelImage(
                            url = uiState.level.imageUrl,
                            fallback = uiState.level.imageRes,
                            contentDescription = null,
                            modifier = Modifier.fillMaxSize(),
                            contentScale = ContentScale.Crop
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "KODEX UNLOCKED!",
                            color = LightOrange,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            "Classic ${uiState.level.displayName}",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Surface(shape = RoundedCornerShape(12.dp), color = LightOrange) {
                        Row(
                            Modifier.padding(horizontal = 10.dp, vertical = 5.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                "PROCEED",
                                color = DarkBrown,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black
                            )
                            Spacer(Modifier.width(3.dp))
                            Icon(
                                Icons.AutoMirrored.Filled.ArrowForward,
                                contentDescription = null,
                                tint = DarkBrown,
                                modifier = Modifier.size(11.dp)
                            )
                        }
                    }
                }
            }

            Spacer(Modifier.height(10.dp))
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = RewardCardFill,
                border = BorderStroke(1.dp, LightOrange.copy(alpha = 0.45f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable { onOpenDocumentary() }
            ) {
                Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = null,
                        tint = LightOrange.copy(alpha = 0.8f),
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(Modifier.width(8.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "UNLOCK FOOD DOCUMENTARY",
                            color = LightOrange.copy(alpha = 0.9f),
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.sp
                        )
                        Text(
                            "${uiState.level.region.displayName}: Culinary Heritage Tour · With KK tokens",
                            color = Color.White.copy(alpha = 0.6f),
                            fontSize = 10.sp
                        )
                    }
                    Surface(shape = RoundedCornerShape(9.dp), color = Color.White.copy(alpha = 0.15f)) {
                        Text(
                            "OPEN",
                            color = Color.White.copy(alpha = 0.9f),
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                        )
                    }
                }
            }

            Spacer(Modifier.height(20.dp))

            if (onPlayNext != null) {
                Button(
                    onClick = clickSfx {
                        SoundFx.play(ctx, SoundFx.Cue.NextDish)
                        SoundFx.stopBgm()
                        onPlayNext()
                    },
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlayNowBrown,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Text("NEXT LEVEL", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
                Spacer(Modifier.height(10.dp))
                OutlinedButton(
                    onClick = clickSfx(onBackToMap),
                    shape = RoundedCornerShape(26.dp),
                    border = BorderStroke(1.dp, LightOrange.copy(alpha = 0.5f)),
                    colors = ButtonDefaults.outlinedButtonColors(
                        containerColor = Color.White.copy(alpha = 0.08f),
                        contentColor = LightOrange
                    ),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(50.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(17.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("Back to Map", fontWeight = FontWeight.SemiBold)
                }
            } else {
                // Final dish cleared — nothing left to advance to.
                Text(
                    "You've cooked through every dish. Bravo, Chef!",
                    color = LightOrange,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    textAlign = TextAlign.Center
                )
                Spacer(Modifier.height(12.dp))
                Button(
                    onClick = clickSfx(onBackToMap),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = BurntOrange,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(26.dp),
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp)
                ) {
                    Icon(Icons.Default.Map, contentDescription = null, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                    Text("BACK TO MAP", fontWeight = FontWeight.Bold, letterSpacing = 2.sp)
                }
            }
            } // end staged details
        }

        // Celebration rains over everything; decorative, never blocks taps.
        ConfettiBurst(Modifier.fillMaxSize())
    }
}

private data class Confetto(
    val xFraction: Float,
    val delayMs: Int,
    val colorIndex: Int,
    val width: Float,
    val drift: Float,
    val spin: Float
)

/** One-shot celebration burst over the win screen. Purely decorative. */
@Composable
private fun ConfettiBurst(modifier: Modifier = Modifier) {
    val colors = listOf(TileCorrectGreen, TileSemiYellow, LightOrange, BurntOrange, Color.White)
    val confetti = remember {
        List(46) {
            Confetto(
                xFraction = Random.nextFloat(),
                delayMs = Random.nextInt(0, 800),
                colorIndex = Random.nextInt(colors.size),
                width = Random.nextInt(6, 13).toFloat(),
                drift = Random.nextFloat() * 2f - 1f,
                spin = Random.nextFloat() * 720f - 360f
            )
        }
    }
    val totalMs = 2800
    val clock = remember { Animatable(0f) }
    LaunchedEffect(Unit) {
        clock.animateTo(1f, tween(totalMs, easing = LinearEasing))
    }

    Canvas(modifier) {
        confetti.forEach { c ->
            val t = ((clock.value * totalMs - c.delayMs) / 1900f).coerceIn(0f, 1f)
            if (t <= 0f) return@forEach
            val x = size.width * c.xFraction + c.drift * 70f * t
            val y = -30f + (size.height + 80f) * t
            val fade = if (t > 0.75f) (1f - t) / 0.25f else 1f
            rotate(degrees = c.spin * t, pivot = Offset(x, y)) {
                drawRect(
                    color = colors[c.colorIndex].copy(alpha = fade.coerceIn(0f, 1f)),
                    topLeft = Offset(x - c.width / 2f, y - c.width / 2f),
                    size = Size(c.width, c.width * 1.7f)
                )
            }
        }
    }
}

@Composable
private fun WinRewardStack(
    rewardCount: Int,
    onExpandSound: () -> Unit,
    content: @Composable ColumnScope.() -> Unit
) {
    var expanded by remember { mutableStateOf(false) }
    val peek = (rewardCount - 1).coerceIn(0, 2)

    Column(Modifier.fillMaxWidth()) {
        Box(Modifier.fillMaxWidth()) {
            if (!expanded) {
                repeat(peek) { i ->
                    val layer = peek - i
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = RewardCardFill.copy(alpha = 0.78f + i * 0.08f),
                        border = BorderStroke(1.dp, LightOrange.copy(alpha = 0.40f)),
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = (layer * 8).dp)
                            .offset(y = (layer * 8).dp)
                    ) {
                        Spacer(Modifier.height(64.dp))
                    }
                }
            }
            Surface(
                shape = RoundedCornerShape(16.dp),
                color = RewardCardFill,
                border = BorderStroke(1.dp, TileSemiYellow.copy(alpha = 0.75f)),
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable {
                        onExpandSound()
                        expanded = !expanded
                    }
            ) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 13.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.MilitaryTech,
                        contentDescription = null,
                        tint = TileSemiYellow,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (expanded) "REWARDS" else "REWARDS STACKED",
                            color = TileSemiYellow,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 2.sp
                        )
                        Text(
                            if (expanded) "Tap to fold" else "$rewardCount unlocked · Tap to open",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 14.sp
                        )
                    }
                    Icon(
                        if (expanded) Icons.Default.KeyboardArrowUp else Icons.Default.KeyboardArrowDown,
                        contentDescription = if (expanded) "Fold rewards" else "Open rewards",
                        tint = LightOrange,
                        modifier = Modifier.size(22.dp)
                    )
                }
            }
        }
        if (!expanded && peek > 0) {
            Spacer(Modifier.height((peek * 8).dp))
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = expanded,
            enter = fadeIn() + slideInVertically { it / 6 },
            exit = fadeOut() + slideOutVertically { it / 6 }
        ) {
            Column(Modifier.padding(top = 10.dp), content = content)
        }
    }
}

@Composable
private fun WinUnlockRow(
    kicker: String,
    title: String,
    subtitle: String,
    amountLabel: String,
    leading: @Composable () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = RewardCardFill,
        border = BorderStroke(1.dp, TileSemiYellow.copy(alpha = 0.75f)),
        modifier = Modifier.fillMaxWidth()
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            Box(Modifier.size(44.dp), contentAlignment = Alignment.Center) {
                leading()
            }
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    kicker,
                    color = TileSemiYellow,
                    fontSize = 9.sp,
                    fontWeight = FontWeight.Bold,
                    letterSpacing = 2.sp
                )
                Text(
                    title,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp
                )
                Text(
                    subtitle,
                    color = LightOrange.copy(alpha = 0.8f),
                    fontSize = 10.sp
                )
            }
            Text(
                amountLabel,
                color = TileSemiYellow,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp
            )
        }
    }
}

@Composable
private fun RewardChip(
    icon: ImageVector,
    value: String,
    label: String,
    pending: Boolean,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White.copy(alpha = if (pending) 0.08f else 0.14f),
        border = BorderStroke(1.dp, LightOrange.copy(alpha = if (pending) 0.2f else 0.35f)),
        modifier = modifier.alpha(if (pending) 0.65f else 1f)
    ) {
        Column(
            Modifier.padding(vertical = 14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = null, tint = LightOrange, modifier = Modifier.size(18.dp))
            Spacer(Modifier.height(5.dp))
            Text(value, color = Color.White, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
            Text(
                label,
                color = LightOrange.copy(alpha = 0.85f),
                fontSize = 10.sp,
                textAlign = TextAlign.Center
            )
        }
    }
}

/**
 * Power-ups as a hand of cards. Collapsed they sit as a neat stacked deck;
 * tapping deals them out in a fan, each card tilting on its bottom corner
 * the way a held hand splays. Echoes the heritage cards in the KODEX, so
 * the game's two card metaphors match.
 */
@Composable
private fun PowerUpDock(
    expanded: Boolean,
    previewKey: Int,
    onToggle: () -> Unit,
    pointsBalance: Int,
    roundActive: Boolean,
    fullyRevealed: Boolean,
    revealsLeft: Int,
    bombUsed: Boolean,
    cooldownMs: Long,
    reduceMotion: Boolean,
    onReveal: () -> Unit,
    onBomb: () -> Unit,
    onSolve: () -> Unit
) {
    val open by animateFloatAsState(
        targetValue = if (expanded) 1f else 0f,
        animationSpec = spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow),
        label = "deck_open"
    )
    // On Continue / Play Now the hand fans out so all three cards show,
    // then folds back to the stacked deck. Tapping still opens it for real.
    val peek = remember(previewKey) { Animatable(0f) }
    var peekPlayed by remember(previewKey) { mutableStateOf(false) }
    LaunchedEffect(previewKey, reduceMotion, expanded) {
        if (expanded) {
            peek.snapTo(0f)
            peekPlayed = true
            return@LaunchedEffect
        }
        if (peekPlayed || reduceMotion) return@LaunchedEffect
        delay(280)
        peek.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioLowBouncy, stiffness = Spring.StiffnessLow)
        )
        delay(900)
        peek.animateTo(
            0f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
        peekPlayed = true
    }
    val spread = maxOf(open, peek.value)
    // The closed deck breathes so it reads as something you can pick up.
    val idle by rememberInfiniteTransition(label = "deck_idle").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1800), RepeatMode.Reverse),
        label = "deck_idle_lift"
    )

    val onCooldown = cooldownMs > 0L
    val waitLabel = if (onCooldown) PowerUpRules.formatCooldown(cooldownMs) else null
    val slots = listOf(
        PowerUpSlot(
            art = R.drawable.powerup_reveal,
            label = PowerUp.REVEAL_LETTER.title,
            costKk = PowerUp.REVEAL_LETTER.coinCost,
            status = when {
                revealsLeft == 0 -> "USED"
                waitLabel != null -> waitLabel
                else -> null
            },
            accent = TileCorrectGreen,
            enabled = roundActive && !fullyRevealed && revealsLeft > 0 && !onCooldown &&
                pointsBalance >= PowerUp.REVEAL_LETTER.coinCost,
            onClick = onReveal
        ),
        PowerUpSlot(
            art = R.drawable.powerup_bomb,
            label = PowerUp.BOMB.title,
            costKk = PowerUp.BOMB.coinCost,
            status = when {
                bombUsed -> "USED"
                waitLabel != null -> waitLabel
                else -> null
            },
            accent = TileSemiYellow,
            enabled = roundActive && !bombUsed && !onCooldown &&
                pointsBalance >= PowerUp.BOMB.coinCost,
            onClick = onBomb
        ),
        PowerUpSlot(
            art = R.drawable.powerup_solve,
            label = PowerUp.INSTANT_SOLVE.title,
            costKk = PowerUp.INSTANT_SOLVE.coinCost,
            status = null,
            accent = Color(0xFFC9A227),
            enabled = roundActive && !fullyRevealed &&
                pointsBalance >= PowerUp.INSTANT_SOLVE.coinCost,
            onClick = onSolve
        )
    )
    val affordable = slots.count { it.enabled }

    // Sizes itself from the screen: three fanned cards plus breathing room
    // have to fit any phone, so the card scales rather than being fixed.
    BoxWithConstraints(
        Modifier.fillMaxWidth(),
        contentAlignment = Alignment.BottomCenter
    ) {
        val cardWidth = (maxWidth * 0.17f).coerceIn(50.dp, 68.dp)
        val cardHeight = cardWidth * 1.3f
        val spreadOpen = cardWidth * 1.08f
        val spreadClosed = cardWidth * 0.19f
        // Extra room under the fanned cards so YOUR KK sits below them,
        // not on the BOMB plate.
        val dockHeight = cardHeight + 40.dp

        Box(
            Modifier
                .fillMaxWidth()
                .height(dockHeight),
            contentAlignment = Alignment.BottomCenter
        ) {
        slots.forEachIndexed { index, slot ->
            val fromCentre = index - 1
            // Closed: a fanned deck you can see three cards in.
            // Open: splayed wide like a dealt hand.
            val tilt = fromCentre * (7f + 14f * spread)
            val slideX = (spreadClosed + (spreadOpen - spreadClosed) * spread) * fromCentre
            val lift = if (index == 1) cardHeight * 0.34f else cardHeight * 0.22f
            val idleLift = if (spread < 0.05f && !reduceMotion) (idle * 4f).dp else 0.dp
            val dealtBob = if (spread > 0.5f && !reduceMotion) {
                (idle * (3f + index) * (if (index == 1) 1.2f else 0.8f)).dp
            } else 0.dp

            PowerUpCard(
                slot = slot,
                dealt = expanded && open > 0.5f,
                cardWidth = cardWidth,
                cardHeight = cardHeight,
                reduceMotion = reduceMotion,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .offset(x = slideX, y = -(lift * spread) - idleLift - dealtBob - 34.dp)
                    .graphicsLayer {
                        rotationZ = tilt
                        // Pivot at the bottom corner so cards splay from the hand.
                        transformOrigin = TransformOrigin(0.5f, 1f)
                    }
                    // Centre card rides on top of the deck.
                    .zIndex(if (index == 1) 1f else 0f),
                onClick = { if (expanded && open > 0.5f) slot.onClick() else onToggle() }
            )
        }

        // One centred status line: state on the left of the dot, purse on the
        // right, so nothing pulls the dock off-axis.
        Row(
            Modifier
                .align(Alignment.BottomCenter)
                .padding(bottom = 5.dp)
                .clip(RoundedCornerShape(10.dp))
                .background(Color.Black.copy(alpha = 0.35f))
                .clickable(enabled = open > 0.5f, onClick = onToggle)
                .padding(horizontal = 10.dp, vertical = 3.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (open > 0.5f) {
                Icon(
                    Icons.Default.Close,
                    contentDescription = "Close power-ups",
                    tint = LightOrange.copy(alpha = 0.9f),
                    modifier = Modifier.size(11.dp)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                text = when {
                    open > 0.5f -> "TAP A CARD"
                    affordable > 0 -> "$affordable READY"
                    // The number beside this is the purse, so the label names
                    // the purse rather than telling the player to go fill it.
                    else -> "YOUR KK"
                },
                color = LightOrange.copy(alpha = 0.85f),
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.5.sp
            )
            Text(
                "  ·  ",
                color = LightOrange.copy(alpha = 0.4f),
                fontSize = 9.sp
            )
            // The bolt read as energy or stamina, which is not what this is.
            // Naming the currency says it outright, and the gold "KK" is the
            // same yellow the coin wears everywhere else.
            Text(
                "$pointsBalance",
                color = LightOrange,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.width(3.dp))
            Text(
                "KK",
                color = TileSemiYellow,
                fontSize = 11.sp,
                fontWeight = FontWeight.Bold
            )
        }
        }
    }
}

private data class PowerUpSlot(
    /** The power-up's own pixel art, shared with the KK primer. */
    @DrawableRes val art: Int,
    val label: String,
    /** What the power-up costs. Always on the plate, spent or not. */
    val costKk: Int,
    /** Why it can't be played right now — "USED", "0:58". Null when ready. */
    val status: String?,
    val accent: Color,
    val enabled: Boolean,
    val soon: Boolean = false,
    val onClick: () -> Unit
)

/**
 * A collectible-style power-up card: accent header band with the icon
 * medallion, name, and a price plate at the foot. Locked cards keep their
 * shape but drain of colour, so players can see what they're saving for.
 */
@Composable
private fun PowerUpCard(
    slot: PowerUpSlot,
    dealt: Boolean,
    cardWidth: androidx.compose.ui.unit.Dp,
    cardHeight: androidx.compose.ui.unit.Dp,
    reduceMotion: Boolean,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val playable = slot.enabled || slot.soon
    val accent = if (slot.enabled) slot.accent else Color(0xFF9A8B79)
    val unit = cardWidth.value / 68f
    val rimPulse by rememberInfiniteTransition(label = "card_rim").animateFloat(
        initialValue = 0.55f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(1200), RepeatMode.Reverse),
        label = "card_rim_alpha"
    )
    val rimAlpha = if (slot.enabled && dealt && !reduceMotion) rimPulse else if (slot.enabled) 1f else 0.45f

    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (slot.enabled) CardSurface else Color(0xFF4A3826),
        border = BorderStroke(2.dp, accent.copy(alpha = rimAlpha)),
        shadowElevation = if (dealt) 14.dp else 6.dp,
        modifier = modifier
            .size(width = cardWidth, height = cardHeight)
            .clickable(enabled = !dealt || playable, onClick = onClick)
    ) {
        Column(Modifier.fillMaxSize()) {
            // Header band — the card's colour identity.
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(cardHeight * 0.38f)
                    .background(
                        Brush.verticalGradient(
                            listOf(accent.copy(alpha = if (slot.enabled) 0.95f else 0.35f), accent.copy(alpha = 0.55f))
                        )
                    ),
                contentAlignment = Alignment.Center
            ) {
                // The art sits straight on the band. It used to be a tinted
                // glyph needing a white disc to read against the accent;
                // these carry their own colour and outline, so the disc
                // would just be a ring around a picture.
                Image(
                    painter = painterResource(slot.art),
                    contentDescription = slot.label,
                    contentScale = ContentScale.Fit,
                    // Drained rather than dimmed when it cannot be played,
                    // so "unavailable" still reads at this size.
                    colorFilter = if (slot.enabled) null else {
                        ColorFilter.colorMatrix(ColorMatrix().apply { setToSaturation(0.15f) })
                    },
                    alpha = if (slot.enabled) 1f else 0.6f,
                    modifier = Modifier.size(28.dp * unit)
                )
                if (!slot.enabled) {
                    Icon(
                        Icons.Default.Lock,
                        contentDescription = "Locked",
                        tint = Color.White.copy(alpha = 0.85f),
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(3.dp)
                            .size(10.dp * unit)
                    )
                }
            }

            Column(
                Modifier
                    .fillMaxSize()
                    .padding(horizontal = 3.dp, vertical = 5.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    slot.label.uppercase(),
                    color = if (slot.enabled) DarkBrown else Color.White.copy(alpha = 0.7f),
                    fontSize = (10 * unit).sp,
                    fontWeight = FontWeight.Black,
                    maxLines = 1
                )
                // Why the card is out of play. It sits in the gap the card
                // already had between name and plate, so the price below can
                // stay a price instead of doubling as a status line.
                slot.status?.let { state ->
                    Text(
                        state,
                        color = if (slot.enabled) DarkBrown.copy(alpha = 0.75f)
                        else Color.White.copy(alpha = 0.75f),
                        fontSize = (8 * unit).sp,
                        fontWeight = FontWeight.Bold,
                        letterSpacing = 0.5.sp,
                        maxLines = 1
                    )
                }
                Surface(
                    shape = RoundedCornerShape(6.dp),
                    color = if (slot.enabled) accent else Color.White.copy(alpha = 0.14f)
                ) {
                    Row(
                        Modifier.padding(horizontal = 5.dp, vertical = 2.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.Center
                    ) {
                        Image(
                            painter = painterResource(R.drawable.ic_kk_pixel),
                            contentDescription = null,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .size((12 * unit).dp)
                                .alpha(if (slot.enabled) 1f else 0.6f)
                        )
                        Spacer(Modifier.width(3.dp))
                        Text(
                            "${slot.costKk}",
                            color = Color.White,
                            fontSize = (11 * unit).sp,
                            fontWeight = FontWeight.Black,
                            maxLines = 1
                        )
                    }
                }
            }
        }
    }
}


/**
 * The coaching line in the chef's wood speech holder, laid out like the
 * old lozenge: mood icon on the left, words beside it.
 */
@Composable
private fun CoachBanner(
    text: String,
    tone: CoachTone,
    modifier: Modifier = Modifier
) {
    val accent = when (tone) {
        CoachTone.Urgent -> Color(0xFFFF8A3D)
        CoachTone.Cold -> Color(0xFFC0A88C)
        CoachTone.Shuffle -> Color(0xFFE8C25A)
        CoachTone.Close -> Color(0xFF9FD07A)
        CoachTone.Progress -> Color(0xFF9FD07A)
    }
    val icon = when (tone) {
        CoachTone.Urgent -> Icons.Default.PriorityHigh
        CoachTone.Cold -> Icons.Default.AcUnit
        CoachTone.Shuffle -> Icons.Default.SwapHoriz
        CoachTone.Close -> Icons.Default.LocalFireDepartment
        CoachTone.Progress -> Icons.Default.TrendingUp
    }
    val emphatic = tone == CoachTone.Urgent

    Row(
        modifier
            .fillMaxWidth()
            .dialoguePlate()
            .padding(horizontal = 16.dp, vertical = 12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(
            icon,
            contentDescription = null,
            tint = accent,
            modifier = Modifier.size(16.dp)
        )
        Spacer(Modifier.width(10.dp))
        Text(
            text,
            color = if (emphatic) accent else DialogueInk,
            fontSize = 14.sp,
            lineHeight = 20.sp,
            fontWeight = if (emphatic) FontWeight.ExtraBold else FontWeight.Medium,
            textAlign = TextAlign.Start,
            modifier = Modifier.weight(1f)
        )
    }
}

@Composable
private fun PlayTile(
    letter: Char,
    state: TileState,
    revealDelayMs: Int,
    revealBurst: Boolean,
    tileSize: androidx.compose.ui.unit.Dp,
    row: Int,
    col: Int,
    cols: Int,
    roundKey: Int,
    reduceMotion: Boolean
) {
    val appear = remember(roundKey) { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(roundKey, reduceMotion) {
        if (reduceMotion) {
            appear.snapTo(1f)
            return@LaunchedEffect
        }
        delay((row * cols + col) * 18L + 80L)
        appear.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }
    GameTile(
        letter = letter,
        state = state,
        revealDelayMs = revealDelayMs,
        revealBurst = revealBurst,
        animated = !reduceMotion,
        modifier = Modifier
            .size(tileSize)
            .graphicsLayer {
                scaleX = appear.value
                scaleY = appear.value
                alpha = appear.value
            }
    )
}

@Composable
private fun CircleIconButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    size: androidx.compose.ui.unit.Dp = 40.dp,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(size)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}
