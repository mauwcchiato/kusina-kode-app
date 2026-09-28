package com.example.kusinakode.ui.game

import androidx.compose.runtime.Composable
import androidx.compose.runtime.State
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.produceState
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.KusinaKodeApp
import com.example.kusinakode.LastPlayedStore
import com.example.kusinakode.domain.model.PlayerProgress
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.Session
import com.example.kusinakode.domain.GamificationEvents
import com.example.kusinakode.domain.RoundScored
import com.example.kusinakode.ui.learn.IngredientArt
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first

@Composable
fun GameRoute(
    level: Int,
    onLevelSelect: () -> Unit,
    onGoHome: () -> Unit,
    onPlayLevel: (Int) -> Unit,
    onViewDish: (Int) -> Unit,
    onOpenDocumentary: () -> Unit = {}
) {
    val context = LocalContext.current
    // Keyed per level so navigating to another level starts a fresh round.
    val viewModel: GameViewModel = viewModel(
        key = "game_$level",
        factory = GameViewModel.Factory(level, context.applicationContext)
    )
    val uiState by viewModel.uiState.collectAsState()

    // Remember which island this round is in, so the Home card continues in the
    // same region (play a Visayas level → Home offers the next Visayas level).
    LaunchedEffect(level) { LastPlayedStore.setLevel(context, Session.userId, level) }

    // What Module 2 credited for this round, so the win screen can show it.
    val scored by produceState<RoundScored?>(initialValue = null, level) {
        GamificationEvents.roundsScored.collect { event ->
            if (event.levelId == level) value = event
        }
    }

    // NEXT LEVEL continues the current island in per-region order (Visayas
    // Level 1 → Visayas Level 2), and is hidden on a region's last level —
    // rather than stepping to global level+1, which lands in another island.
    val nextInRegion = LevelProvider.nextInRegion(level)

    // Whether this win completes the whole game — every dish on every island,
    // this one included (its solve may not have reached the store yet). An
    // island's last level has no NEXT LEVEL either, so that alone is not
    // enough to play the curtain call. Read straight from the store rather
    // than through GamificationViewModel, whose init re-runs a sync.
    val progressFlow = remember {
        (context.applicationContext as KusinaKodeApp).gamification.progress(Session.userId)
    }
    val progress by progressFlow.collectAsState(initial = PlayerProgress())
    // What was solved when the round opened (the store's first emission,
    // long before any win). A replay of an already-solved dish must not play
    // the finale again for someone who finished the game earlier.
    var solvedAtOpen by remember(level) { mutableStateOf<Set<Int>?>(null) }
    LaunchedEffect(level) { solvedAtOpen = progressFlow.first().solvedLevels }
    val finishesGame = remember(progress.solvedLevels, solvedAtOpen, level) {
        val before = solvedAtOpen
        before != null && level !in before &&
            (1..LevelProvider.levelCount).all { it == level || it in progress.solvedLevels }
    }

    // Every round opens with an intro over a dim; the round only becomes
    // playable once it lifts away. Arriving via the win screen's NEXT LEVEL
    // button, that is the dropping sign with its ingredient burst and the big
    // level number. Any other way in — the map, Home, Continue — it is the
    // level number alone, shorter.
    val arrivingFromNext = remember(level) { NextLevelCue.armedForLevel == level }
    val introMs = if (arrivingFromNext) NEXT_BANNER_MS else NUMBER_INTRO_MS

    // Held as a State and read only by BannerLayer, so showing or hiding the
    // sign recomposes that small layer rather than the whole game screen.
    // Starts false so the sign's drop-in animation still plays.
    val bannerShown = remember(level) { mutableStateOf(false) }
    // The tiles stay hidden under the sign and pop in once it lifts. Held from
    // the very first frame, so they cannot start before the sign appears.
    val tilesHeld = remember(level) { mutableStateOf(true) }
    val reveal: () -> Unit = remember(level) {
        {
            if (bannerShown.value) {
                bannerShown.value = false
                tilesHeld.value = false
                viewModel.setClockHeld(false)
            }
        }
    }
    LaunchedEffect(level) {
        if (arrivingFromNext) NextLevelCue.armedForLevel = null
        bannerShown.value = true
        viewModel.setClockHeld(true)
        try {
            delay(introMs)
            reveal()
        } finally {
            tilesHeld.value = false
            viewModel.setClockHeld(false)
        }
    }

    Box(Modifier.fillMaxSize()) {
        GameScreen(
            uiState = uiState,
            onKey = viewModel::onKey,
            onBackspace = viewModel::onBackspace,
            onEnter = viewModel::onEnter,
            onRestart = viewModel::restart,
            onReveal = viewModel::useReveal,
            onBomb = viewModel::useBomb,
            onSolve = viewModel::useSolve,
            onDismissPowerUpMessage = viewModel::dismissPowerUpMessage,
            onSetPaused = viewModel::setPaused,
            onExit = onLevelSelect,
            onBackToMap = onLevelSelect,
            onGoHome = onGoHome,
            // Arm the sign for the destination round, then navigate to it.
            onPlayNext = nextInRegion?.let { next ->
                {
                    NextLevelCue.armedForLevel = next
                    onPlayLevel(next)
                }
            },
            onViewDish = { onViewDish(level) },
            onOpenDocumentary = onOpenDocumentary,
            scored = scored,
            // The hint cards fan out under the sign and must fold back
            // before it lifts, so their bounce never overlaps the tiles' pop-in.
            // The fan takes ~1s to open, hence the shorter hold.
            hintPeekHoldMs = (introMs - 1_300L).coerceAtLeast(300L),
            holdTiles = { tilesHeld.value },
            finishesGame = finishesGame
        )
        // The next dish's own ingredients burst out of the sign.
        val burstArt = remember(level) {
            LevelProvider.forLevel(level).dish.ingredients
                .mapNotNull { IngredientArt.forName(it) }
                .distinct()
        }
        // The same per-island "Level N" the Game Map shows, not the global id.
        val regionLevel = remember(level) { LevelProvider.regionLevelNumber(level) }
        val region = remember(level) {
            runCatching { LevelProvider.forLevel(level).region }.getOrNull()
        }
        // A tap skips the sign instead of waiting it out.
        BannerLayer(bannerShown, burstArt, regionLevel, region, arrivingFromNext, onSkip = reveal)
    }
}

/** Its own recompose scope, so toggling the sign touches nothing else. */
@Composable
private fun BannerLayer(
    shown: State<Boolean>,
    art: List<Int>,
    levelNumber: Int,
    region: Region?,
    withSign: Boolean,
    onSkip: () -> Unit
) {
    NextLevelBanner(
        show = shown.value,
        ingredientArt = art,
        levelNumber = levelNumber,
        regionName = region?.displayName.orEmpty(),
        region = region,
        withSign = withSign,
        onSkip = onSkip
    )
}

/** How long the NEXT LEVEL sign holds the round before lifting away. */
private const val NEXT_BANNER_MS = 2_800L

/** How long the number-only intro holds a round opened any other way. */
private const val NUMBER_INTRO_MS = 1_800L
