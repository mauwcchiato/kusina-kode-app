package com.example.kusinakode.ui.tutorial

import com.example.kusinakode.ui.components.clickSfx
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Animatable
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.expandVertically
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.shrinkVertically
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.ui.unit.Dp
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.defaultMinSize
import androidx.compose.foundation.border
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.material3.Icon
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.draw.clip
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.kusinakode.CustomKeyboard
import com.example.kusinakode.KusinaSettings
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.engine.WordleEngine
import com.example.kusinakode.domain.model.TileState
import com.example.kusinakode.ui.game.GameTile
import com.example.kusinakode.ui.game.TILE_FLIP_MS
import com.example.kusinakode.ui.game.TILE_STAGGER_MS
import com.example.kusinakode.ui.game.TileCorrectGreen
import com.example.kusinakode.ui.game.TileSemiYellow
import com.example.kusinakode.ui.game.TileWrongBrown
import com.example.kusinakode.ui.theme.LightOrange
import kotlinx.coroutines.delay

private const val TUTORIAL_WORD = "LUGAW"

private const val TUTORIAL_ROWS = 6
private val StoryGold = Color(0xFFD9A227)

/**
 * A real round with training wheels.
 *
 * Uses the actual [GameTile], [CustomKeyboard] and [WordleEngine] rather than
 * mock-ups, so what a player learns here is exactly what they will meet in a
 * scored round. The chef talks in short beats, the tiles flip like a real
 * round, and the colour lesson is shown — not dumped as a paragraph — after
 * the first guess lands.
 */
@Composable
fun TutorialGameScreen(
    onFinish: () -> Unit,
    onSkip: () -> Unit = onFinish
) {
    val answer = TUTORIAL_WORD
    var guesses by remember { mutableStateOf(listOf<String>()) }
    var current by remember { mutableStateOf("") }
    var solved by remember { mutableStateOf(false) }
    var showLegend by remember { mutableStateOf(false) }
    var shakeToken by remember { mutableIntStateOf(0) }
    // Which tile colour the player asked about, if any.
    var openLesson by remember { mutableStateOf<TileLesson?>(null) }

    val prefs by KusinaSettings.prefs.collectAsState()
    val reduceMotion = prefs.reduceMotion
    val context = LocalContext.current

    val verdicts = remember(guesses) { guesses.map { WordleEngine.evaluate(answer, it) } }
    val keyStates = remember(guesses) {
        val map = mutableMapOf<Char, TileState>()
        guesses.forEachIndexed { row, guess ->
            val verdict = WordleEngine.evaluate(answer, guess)
            guess.forEachIndexed { i, ch ->
                val next = verdict[i]
                val existing = map[ch]
                val better = existing == null ||
                    next == TileState.Correct ||
                    (next == TileState.SemiCorrect && existing == TileState.Wrong)
                if (better) map[ch] = next
            }
        }
        map.toMap()
    }

    val finished = solved || guesses.size >= TUTORIAL_ROWS
    val watchingFlip = guesses.size == 1 && !solved && !showLegend
    val rowFull = current.length == answer.length

    val coaching = when {
        solved -> "That's the loop. LUGAW — and the badge is minted for you."
        guesses.size >= TUTORIAL_ROWS -> "Nothing lost. This round was never scored. The dish was LUGAW."
        watchingFlip -> "Watch the tiles."
        showLegend -> "Now use them. Guess again."
        guesses.isEmpty() && current.isEmpty() -> "You only get the region. Type any five-letter word."
        guesses.isEmpty() && !rowFull -> "Five letters."
        guesses.isEmpty() && rowFull -> "Press ENTER — I'll colour what you got right."
        rowFull -> "ENTER when you're ready."
        else -> "You have ${TUTORIAL_ROWS - guesses.size} tries left. Use what the colours told you."
    }

    /**
     * The beats the chef stops the round for.
     *
     * Only the teaching moments: the opening, the first ENTER, the flip, the
     * colour lesson and the two endings. The in-play nudges ("five letters")
     * stay in the slim coach line, because blocking the keyboard to say them
     * would interrupt exactly the person who is already doing the right thing.
     */
    val beatKey = when {
        solved -> "solved"
        guesses.size >= TUTORIAL_ROWS -> "lost"
        showLegend -> "legend"
        watchingFlip -> "flip"
        guesses.isEmpty() && rowFull -> "first_enter"
        guesses.isEmpty() && current.isEmpty() -> "open"
        else -> null
    }
    val beat = remember(beatKey) {
        when (beatKey) {
            "open" -> ChefBeat(
                listOf(
                    "Welcome to the practice kitchen. Nothing here is scored, so there's nothing to lose.",
                    "All I'm giving you is the region. Luzon.",
                    "Type any five-letter word to begin."
                )
            )
            "first_enter" -> ChefBeat(
                listOf("Now press ENTER. I'll colour in whatever you got right.")
            )
            "flip" -> ChefBeat(listOf("Watch the tiles."))
            "legend" -> ChefBeat(
                listOf(
                    "Green means the letter is right, and it's in the right spot.",
                    "Yellow means the letter belongs to the dish, but you've put it in the wrong place.",
                    "Brown means it isn't in the dish at all.",
                    "Now use them. Guess again."
                )
            )
            "solved" -> ChefBeat(
                listOf(
                    "That's the loop.",
                    "Lugaw — and your badge is already minted."
                )
            )
            "lost" -> ChefBeat(
                listOf(
                    "Round over. But nothing's lost — this one was never scored.",
                    "The dish was lugaw."
                )
            )
            else -> null
        }
    }

    // A beat is spoken once. Dismissing it hands the board back rather than
    // re-opening every time the same state recomposes.
    var spokenBeats by remember { mutableStateOf(setOf<String>()) }
    val speaking = beat != null && beatKey != null && beatKey !in spokenBeats

    /**
     * A player who stops touching the screen has lost the thread, so after a
     * few seconds the board says the next thing to do out loud and the glow
     * grows to point at where to do it.
     */
    var idleNudge by remember { mutableStateOf(false) }
    LaunchedEffect(current, guesses.size, speaking, finished) {
        idleNudge = false
        if (speaking || finished) return@LaunchedEffect
        delay(5_000)
        idleNudge = true
    }
    val nudge = when {
        rowFull -> "Tap ENTER to cook this guess."
        current.isEmpty() && guesses.isEmpty() -> "Tap any letter below to start — any word will do."
        current.isEmpty() -> "Pick a word that uses what the colours told you."
        else -> "${answer.length - current.length} more letters to go."
    }

    LaunchedEffect(guesses.size, solved, reduceMotion) {
        if (guesses.size != 1 || solved) return@LaunchedEffect
        val wait = if (reduceMotion) 80L
        else (answer.length * TILE_STAGGER_MS + TILE_FLIP_MS + 180).toLong()
        delay(wait)
        showLegend = true
    }

    // The round is over and the board is just there to be admired, so the
    // hint breathes rather than sitting as a dead line of text.
    val outroPulse by rememberInfiniteTransition(label = "outro").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(tween(950), RepeatMode.Reverse),
        label = "outro_hint"
    )
    val outroHint = if (reduceMotion) 0.9f else 0.55f + 0.45f * outroPulse

    val rowShake = remember { Animatable(0f) }
    LaunchedEffect(shakeToken) {
        if (shakeToken == 0 || reduceMotion) return@LaunchedEffect
        repeat(3) {
            rowShake.animateTo(8f, tween(45))
            rowShake.animateTo(-8f, tween(45))
        }
        rowShake.animateTo(0f, tween(50))
    }

    Box(Modifier.fillMaxSize()) {
    Column(
        Modifier
            .fillMaxSize()
            .background(
                Brush.verticalGradient(listOf(Color(0xFF2A1508), Color(0xFF6F3913)))
            )
            .statusBarsPadding()
            .navigationBarsPadding()
            // With the round over, anywhere on the board moves the intro on.
            // Skip and Back sit in an overlay above the chef, so they still
            // win their own taps while he is speaking.
            .then(
                if (finished && !speaking) {
                    Modifier.clickable(
                        interactionSource = remember { MutableInteractionSource() },
                        indication = null,
                        onClick = onFinish
                    )
                } else Modifier
            )
    ) {
        // Holds the same height as [TutorialTopBar] so the board does not
        // jump when that bar is drawn on top of the chef overlay.
        Spacer(Modifier.height(52.dp))

        Spacer(Modifier.height(10.dp))

        Surface(
            shape = RoundedCornerShape(18.dp),
            color = Color.Black.copy(alpha = 0.35f),
            modifier = Modifier.align(Alignment.CenterHorizontally)
        ) {
            Row(
                Modifier.padding(horizontal = 14.dp, vertical = 7.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    "NOT SCORED",
                    color = LightOrange,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Black,
                    letterSpacing = 1.5.sp
                )
                Spacer(Modifier.width(10.dp))
                Box(
                    Modifier
                        .width(1.dp)
                        .height(14.dp)
                        .background(Color.White.copy(alpha = 0.25f))
                )
                Spacer(Modifier.width(10.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(5.dp)) {
                    repeat(TUTORIAL_ROWS) { i ->
                        val active = i == guesses.size && !finished
                        Box(
                            Modifier
                                .size(if (active) 8.dp else 6.dp)
                                .clip(CircleShape)
                                .background(
                                    when {
                                        i < guesses.size -> Color(0xFFCC6B1F)
                                        active -> LightOrange
                                        else -> Color.White.copy(alpha = 0.28f)
                                    }
                                )
                        )
                    }
                }
            }
        }

        Spacer(Modifier.height(14.dp))

        // Held in the layout but faded out while the chef has the floor, so
        // dismissing the dialogue doesn't shunt the board up and down.
        Box(
            Modifier
                .alpha(if (speaking) 0f else 1f)
                .fillMaxWidth()
                .height(40.dp)
                .padding(horizontal = 24.dp),
            contentAlignment = Alignment.Center
        ) {
            Text(
                if (idleNudge) nudge else coaching,
                color = if (idleNudge) StoryGold else LightOrange.copy(alpha = 0.92f),
                fontSize = 13.sp,
                lineHeight = 18.sp,
                fontWeight = if (idleNudge) FontWeight.Bold else FontWeight.Medium,
                textAlign = TextAlign.Center,
                maxLines = 2
            )
        }

        // The board takes what is left after the chrome and the keyboard, and
        // sizes its tiles to fit that — a fixed tile size cropped the action
        // row on short phones and stranded the board on tall ones.
        BoxWithConstraints(
            Modifier
                .fillMaxWidth()
                .weight(1f)
                .padding(horizontal = 18.dp, vertical = 4.dp),
            contentAlignment = Alignment.Center
        ) {
            val gap = 6.dp
            // Matches the per-row padding in TutorialBoard. Leaving it out of
            // the sums made the column 4dp per row taller than its box, so the
            // bottom row was clipped.
            val rowPad = 2.dp
            val cols = answer.length
            val legendRoom = if (showLegend) ColourLegendHeight else 0.dp
            val byWidth = (maxWidth - rowPad * 2 - gap * (cols - 1)) / cols
            val byHeight = (
                maxHeight - legendRoom - gap * (TUTORIAL_ROWS - 1) -
                    rowPad * 2 * TUTORIAL_ROWS
                ) / TUTORIAL_ROWS
            val tileSize = minOf(byWidth, byHeight).coerceIn(12.dp, 62.dp)

            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                TutorialBoard(
                    answerLength = cols,
                    guesses = guesses,
                    verdicts = verdicts,
                    current = current,
                    reduceMotion = reduceMotion,
                    activeRowShake = rowShake.value,
                    highlightActive = !finished,
                    urgent = idleNudge,
                    tileSize = tileSize,
                    gap = gap
                )
                AnimatedVisibility(
                    visible = showLegend,
                    enter = fadeIn(tween(280)) + expandVertically(tween(280)),
                    exit = fadeOut() + shrinkVertically()
                ) {
                    ColourLesson(
                        highContrast = prefs.highContrastTiles,
                        onOpenLesson = { openLesson = it },
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(top = 12.dp)
                    )
                }
            }
        }

        if (finished) {
            Column(Modifier.padding(horizontal = 18.dp, vertical = 10.dp)) {
                Text(
                    if (solved) "You cooked it." else "Round over.",
                    color = Color.White,
                    style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
                Spacer(Modifier.height(10.dp))
                Text(
                    "Tap anywhere for how KK works",
                    color = StoryGold.copy(alpha = outroHint),
                    fontWeight = FontWeight.Bold,
                    fontSize = 13.sp,
                    letterSpacing = 0.4.sp,
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else {
            CustomKeyboard(
                keyStates = keyStates,
                pulseEnter = rowFull && !reduceMotion,
                onKeyClick = { ch ->
                    SoundFx.play(context, SoundFx.Cue.Backspace)
                    if (current.length < answer.length) current += ch
                },
                onBackspace = {
                    SoundFx.play(context, SoundFx.Cue.Backspace)
                    current = current.dropLast(1)
                },
                onEnter = {
                    if (current.length == answer.length) {
                        val word = current.uppercase()
                        val verdict = WordleEngine.evaluate(answer, word)
                        guesses = guesses + word
                        if (WordleEngine.isWinningVerdict(verdict)) {
                            solved = true
                            SoundFx.play(context, SoundFx.Cue.Win)
                            SoundFx.vibrate(context, 28)
                        } else {
                            SoundFx.vibrate(context, 12)
                        }
                        current = ""
                    } else {
                        shakeToken++
                        SoundFx.vibrate(context, 18)
                    }
                }
            )
        }
    }

        // Drawn last so the chef stands in front of the board he is pointing at.
        if (speaking && beat != null && beatKey != null) {
            ChefDialogue(
                beat = beat,
                reduceMotion = reduceMotion,
                // Nothing left to play once the round is over.
                finalLabel = if (finished) "TAP TO SEE THE BOARD" else "TAP TO PLAY",
                onFinished = { spokenBeats = spokenBeats + beatKey }
            )
        }

        openLesson?.let { lesson ->
            TileLessonSheet(
                lesson = lesson,
                green = if (prefs.highContrastTiles) Color(0xFF1D6FB8) else TileCorrectGreen,
                yellow = if (prefs.highContrastTiles) Color(0xFFE07B00) else TileSemiYellow,
                brown = TileWrongBrown,
                onDismiss = { openLesson = null }
            )
        }

        // Drawn last so Skip and Back stay above the chef overlay. Tapping
        // Skip used to land on the dialogue layer and only advance his line.
        TutorialTopBar(
            onSkip = onSkip,
            modifier = Modifier
                .align(Alignment.TopCenter)
                .statusBarsPadding()
        )
    }
}

@Composable
private fun TutorialTopBar(
    onSkip: () -> Unit,
    modifier: Modifier = Modifier
) {
    Box(
        modifier
            .fillMaxWidth()
            .clickable(
                interactionSource = remember { MutableInteractionSource() },
                indication = null,
                onClick = {}
            )
            .padding(horizontal = 12.dp, vertical = 6.dp)
    ) {
        Column(
            Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "LUZON",
                color = Color.White,
                style = MaterialTheme.typography.titleLarge.copy(fontWeight = FontWeight.ExtraBold),
                maxLines = 1
            )
            Text(
                "PRACTICE",
                color = LightOrange,
                fontSize = 10.sp,
                fontWeight = FontWeight.Black,
                letterSpacing = 2.sp,
                maxLines = 1
            )
        }
        TutorialCircleButton(
            onClick = onSkip,
            modifier = Modifier.align(Alignment.CenterStart)
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Leave the practice round",
                tint = Color.White,
                modifier = Modifier.size(20.dp)
            )
        }
        TextButton(onClick = clickSfx(onSkip), modifier = Modifier.align(Alignment.CenterEnd)) {
            Text("Skip", color = LightOrange.copy(alpha = 0.85f), fontWeight = FontWeight.SemiBold)
        }
    }
}

@Composable
private fun TutorialCircleButton(
    onClick: () -> Unit,
    modifier: Modifier = Modifier,
    content: @Composable () -> Unit
) {
    Box(
        modifier
            .size(40.dp)
            .clip(CircleShape)
            .background(Color.White.copy(alpha = 0.18f))
            .border(1.dp, Color.White.copy(alpha = 0.25f), CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        content()
    }
}

@Suppress("unused")
@Composable
private fun RegionCue(reduceMotion: Boolean) {
    val appear = remember { Animatable(if (reduceMotion) 1f else 0.86f) }
    val ring by rememberInfiniteTransition(label = "region_ring").animateFloat(
        initialValue = 0.35f,
        targetValue = 0.85f,
        animationSpec = infiniteRepeatable(tween(1400), RepeatMode.Reverse),
        label = "region_ring_alpha"
    )
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            appear.snapTo(1f)
            return@LaunchedEffect
        }
        appear.animateTo(1f, tween(480, easing = FastOutSlowInEasing))
    }
    val ringAlpha = if (reduceMotion) 0.55f else ring

    Surface(
        shape = RoundedCornerShape(0.dp),
        color = Color.Black.copy(alpha = 0.34f),
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 18.dp)
            .graphicsLayer {
                scaleX = appear.value
                scaleY = appear.value
                alpha = appear.value
            }
            .border(3.dp, StoryGold.copy(alpha = ringAlpha), RoundedCornerShape(0.dp))
    ) {
        Column(
            Modifier
                .fillMaxWidth()
                .padding(14.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                "LUZON",
                color = StoryGold,
                fontSize = 16.sp,
                fontWeight = FontWeight.ExtraBold,
                letterSpacing = 3.sp
            )
            Spacer(Modifier.height(4.dp))
            Text(
                "A 5-letter dish from this region",
                color = Color.White.copy(alpha = 0.75f),
                fontSize = 12.sp
            )
        }
    }
}

@Composable
private fun TutorialBoard(
    answerLength: Int,
    guesses: List<String>,
    verdicts: List<List<TileState>>,
    current: String,
    reduceMotion: Boolean,
    activeRowShake: Float,
    highlightActive: Boolean,
    /** The player has stalled — push the glow until they act. */
    urgent: Boolean,
    tileSize: Dp,
    gap: Dp
) {
    val pulse by rememberInfiniteTransition(label = "row_glow").animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            tween(if (urgent) 620 else 1100),
            RepeatMode.Reverse
        ),
        label = "row_glow_alpha"
    )
    // Idle players get a louder, faster glow; everyone else gets a hint they
    // can ignore while they think.
    val glow = if (reduceMotion) {
        if (urgent) 0.5f else 0.22f
    } else {
        val lo = if (urgent) 0.35f else 0.14f
        val hi = if (urgent) 0.95f else 0.45f
        lo + (hi - lo) * pulse
    }

    Column(
        Modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(gap)
    ) {
        repeat(TUTORIAL_ROWS) { row ->
            val isActive = row == guesses.size
            Row(
                horizontalArrangement = Arrangement.spacedBy(gap),
                modifier = Modifier
                    .graphicsLayer {
                        translationX = if (isActive) activeRowShake else 0f
                    }
                    .then(
                        if (isActive && highlightActive) {
                            Modifier.rowHalo(glow, urgent)
                        } else Modifier
                    )
                    .padding(2.dp)
            ) {
                repeat(answerLength) { col ->
                    val letter: Char
                    val state: TileState
                    when {
                        row < guesses.size -> {
                            letter = guesses[row][col]
                            state = verdicts[row][col]
                        }
                        isActive && col < current.length -> {
                            letter = current[col]
                            state = TileState.Empty
                        }
                        else -> {
                            letter = ' '
                            state = TileState.Empty
                        }
                    }
                    EntranceTile(
                        row = row,
                        col = col,
                        letter = letter,
                        state = state,
                        revealDelayMs = if (row < guesses.size) col * TILE_STAGGER_MS else 0,
                        reduceMotion = reduceMotion,
                        tileSize = tileSize
                    )
                }
            }
        }
    }
}

@Composable
private fun EntranceTile(
    row: Int,
    col: Int,
    letter: Char,
    state: TileState,
    revealDelayMs: Int,
    reduceMotion: Boolean,
    tileSize: Dp
) {
    val appear = remember { Animatable(if (reduceMotion) 1f else 0f) }
    LaunchedEffect(reduceMotion) {
        if (reduceMotion) {
            appear.snapTo(1f)
            return@LaunchedEffect
        }
        delay((row * 5 + col) * 22L + 180L)
        appear.animateTo(
            1f,
            spring(dampingRatio = Spring.DampingRatioMediumBouncy, stiffness = Spring.StiffnessMedium)
        )
    }
    GameTile(
        letter = letter,
        state = state,
        revealDelayMs = revealDelayMs,
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

/**
 * A soft halo around the active row.
 *
 * Concentric strokes fading outward rather than `Modifier.blur`, which needs
 * API 31 and this app ships to 26. Three rings is enough to read as light
 * rather than as a border.
 */
private fun Modifier.rowHalo(strength: Float, urgent: Boolean): Modifier = drawBehind {
    val radius = CornerRadius(13.dp.toPx())
    val rings = if (urgent) 4 else 3
    repeat(rings) { i ->
        val spread = (i + 1) * 2.5f.dp.toPx()
        val fade = strength * (1f - i / rings.toFloat()) * 0.55f
        drawRoundRect(
            color = StoryGold.copy(alpha = fade),
            topLeft = Offset(-spread, -spread),
            size = Size(size.width + spread * 2f, size.height + spread * 2f),
            cornerRadius = radius,
            style = Stroke(width = 2.dp.toPx())
        )
    }
    // The line itself, so the row still has a crisp edge under the glow.
    drawRoundRect(
        color = StoryGold.copy(alpha = (strength * 0.9f).coerceAtMost(1f)),
        cornerRadius = radius,
        style = Stroke(width = 1.5f.dp.toPx())
    )
}
