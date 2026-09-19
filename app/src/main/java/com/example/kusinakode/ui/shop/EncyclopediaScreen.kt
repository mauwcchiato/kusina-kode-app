package com.example.kusinakode.ui.shop

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Search
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Inventory2
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.PlatformTextStyle
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.pantry.PantryEntry
import com.example.kusinakode.domain.pantry.PalayokGrantRules
import com.example.kusinakode.domain.pantry.Rarity
import com.example.kusinakode.ui.pantry.BaulMarketRunCard
import com.example.kusinakode.ui.pantry.GuestState
import com.example.kusinakode.ui.pantry.IngredientPhoto
import com.example.kusinakode.ui.pantry.NewJarBadge
import com.example.kusinakode.ui.pantry.IngredientSheet
import com.example.kusinakode.ui.pantry.PalayokWheelOverlay
import com.example.kusinakode.ui.pantry.PantryDrawOverlay
import com.example.kusinakode.ui.pantry.PantryViewModel
import com.example.kusinakode.ui.pantry.PixelArt
import com.example.kusinakode.ui.pantry.SelectTick
import com.example.kusinakode.ui.pantry.SellAsk
import com.example.kusinakode.ui.pantry.SellConfirmDialog
import com.example.kusinakode.ui.pantry.SellLoadingOverlay
import com.example.kusinakode.ui.pantry.SellToast
import com.example.kusinakode.ui.components.ParchmentCard
import com.example.kusinakode.ui.components.PauseMenuButton
import com.example.kusinakode.ui.components.PauseMenuTone
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.DarkBrown
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.OutlineDefault

private val PanelBrown = Color(0xFF6B3A1F)
private val TabIdle = Color(0xFFF3E6D4)
private val TabStroke = Color(0xFFC4A574)
private val CardWhite = Color(0xFFFFFDF9)
private val HelpInk = DarkBrown.copy(alpha = 0.88f)
private val HelpSeam = DarkBrown.copy(alpha = 0.22f)

/** `null` is the All tab — every rarity at once. */
private val RarityTabs: List<Rarity?> = listOf(null) + Rarity.entries

private val Rarity?.tabLabel: String get() = this?.label ?: "All"

/**
 * KODEX Pantry — the shelf you actually own, as rarity tabs over a 3-column
 * grid, with the sell price on every card.
 *
 * This used to be a shop counter selling six hardcoded jars for KK. The jars
 * now come from the bauls instead, so the counter runs the other way: the
 * cards are the player's own stock and the pill banks a duplicate.
 */
@Composable
fun EncyclopediaScreen(
    onBack: () -> Unit,
    onOpenKodex: () -> Unit,
    initialTab: String = "all",
    viewModel: PantryViewModel = viewModel(factory = PantryViewModel.factory())
) {
    val ui by viewModel.uiState.collectAsState()
    val ctx = LocalContext.current

    var tab by remember {
        mutableStateOf(RarityTabs.firstOrNull { it?.name.equals(initialTab, ignoreCase = true) })
    }
    var query by remember { mutableStateOf("") }
    var ritual by remember { mutableStateOf(false) }
    var showWheel by remember { mutableStateOf(false) }
    var showHelp by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var pendingSell by remember { mutableStateOf<SellAsk?>(null) }
    var trade by remember { mutableStateOf<SellAsk?>(null) }

    val visible = ui.pinNewToTop(
        ui.snapshot.entries.filter { entry ->
            val tabOk = tab == null || entry.ingredient.rarity == tab
            val q = query.trim()
            val searchOk = q.isBlank() ||
                entry.ingredient.name.contains(q, ignoreCase = true) ||
                entry.ingredient.localName.contains(q, ignoreCase = true) ||
                entry.ingredient.origin.contains(q, ignoreCase = true)
            tabOk && searchOk
        }
    )
    val visibleIds = remember(visible) { visible.map { it.ingredient.id }.toSet() }
    LaunchedEffect(visibleIds) {
        selected = selected.intersect(visibleIds)
    }
    val selectedEntries = visible.filter { it.ingredient.id in selected }
    val selectedJars = selectedEntries.sumOf { it.qty }
    val selectedWorth = selectedEntries.sumOf { it.sellValueKk }
    val shelfScroll = rememberScrollState()
    var lastNewCount by remember { mutableIntStateOf(0) }
    LaunchedEffect(ui.newIngredientIds.size) {
        val n = ui.newIngredientIds.size
        if (n > lastNewCount) shelfScroll.animateScrollTo(0)
        lastNewCount = n
    }

    Box(Modifier.fillMaxSize()) {
        Column(
            Modifier
                .fillMaxSize()
                .background(ShopCream)
                .verticalScroll(shelfScroll)
        ) {
            ShopHeader(
                title = "KODEX Pantry",
                kicker = "Ingredients ${ui.snapshot.jars}",
                balanceKk = ui.balanceKk,
                onBack = onBack,
                onHelp = {
                    SoundFx.tap(ctx)
                    showHelp = true
                },
                kickerShowsCoin = false
            )

            if (ui.isGuest) {
                GuestState()
                return@Column
            }

            Column(Modifier.padding(horizontal = 16.dp, vertical = 14.dp)) {
                OutlinedTextField(
                    value = query,
                    onValueChange = { query = it },
                    placeholder = {
                        Text(
                            "Search for foods or ingredients...",
                            color = HintGray.copy(alpha = 0.7f),
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Normal
                        )
                    },
                    leadingIcon = {
                        Icon(
                            Icons.Default.Search,
                            contentDescription = null,
                            tint = HintGray,
                            modifier = Modifier.padding(start = 9.dp)
                        )
                    },
                    singleLine = true,
                    shape = RoundedCornerShape(16.dp),
                    textStyle = TextStyle(
                        color = ShopText,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.Normal
                    ),
                    modifier = Modifier.fillMaxWidth(),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedContainerColor = Color.White,
                        unfocusedContainerColor = Color.White,
                        focusedBorderColor = Color(0xFFB4510E),
                        unfocusedBorderColor = OutlineDefault,
                        cursorColor = ShopText
                    )
                )

                Spacer(Modifier.height(12.dp))
                MarketRunBar(
                    draws = ui.snapshot.drawsAvailable,
                    drawing = ui.drawing,
                    drawingAll = ui.drawingAll,
                    spins = ui.snapshot.spinsAvailable,
                    onSpin = { showWheel = true },
                    onDraw = {
                        SoundFx.play(ctx, SoundFx.Cue.Baul)
                        ritual = true
                    },
                    onOpenAll = {
                        SoundFx.play(ctx, SoundFx.Cue.BaulOpen)
                        SoundFx.vibrate(ctx, 22)
                        viewModel.drawAll()
                    }
                )
                Spacer(Modifier.height(14.dp))

                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState()),
                    verticalAlignment = Alignment.Bottom
                ) {
                    RarityTabs.forEach { entry ->
                        val selected = tab == entry
                        Box(
                            Modifier
                                .padding(end = 4.dp)
                                .clip(RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp))
                                .background(if (selected) PanelBrown else TabIdle)
                                .border(
                                    1.dp,
                                    if (selected) PanelBrown else TabStroke,
                                    RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                                )
                                .clickable {
                                    SoundFx.tap(ctx)
                                    tab = entry
                                }
                                .padding(horizontal = 14.dp, vertical = 10.dp)
                        ) {
                            Text(
                                entry.tabLabel,
                                color = if (selected) Color.White else ShopText,
                                fontFamily = BeVietnamPro,
                                fontWeight = FontWeight.Normal,
                                fontSize = 12.sp
                            )
                        }
                    }
                }

                // Folder tabs: the strip spans the full width, so a tab always
                // sits on both top corners. Rounding either one leaves the
                // outermost tab's square bottom overhanging a curve, so the top
                // edge stays flat and only the bottom is rounded.
                val panelShape = RoundedCornerShape(
                    topStart = 0.dp,
                    topEnd = 0.dp,
                    bottomStart = 16.dp,
                    bottomEnd = 16.dp
                )
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(panelShape)
                        .background(PanelBrown)
                        .padding(14.dp)
                ) {
                    Row(
                        Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        if (selecting) {
                            Text(
                                if (selected.isEmpty()) "TAP JARS TO SELL"
                                else "${selected.size} SELECTED",
                                color = Color.White.copy(alpha = 0.88f),
                                fontFamily = BeVietnamPro,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 10.sp,
                                letterSpacing = 1.2.sp,
                                modifier = Modifier.weight(1f)
                            )
                        } else {
                            Spacer(Modifier.weight(1f))
                        }
                        if (visible.isNotEmpty()) {
                            if (selecting) {
                                ShelfAction(
                                    label = if (selected.containsAll(visibleIds) && selected.isNotEmpty()) {
                                        "Clear"
                                    } else {
                                        "Select all"
                                    },
                                    icon = Icons.Default.SelectAll,
                                    tone = ShelfActionTone.Gold
                                ) {
                                    SoundFx.tap(ctx)
                                    selected = if (selected.containsAll(visibleIds)) {
                                        emptySet()
                                    } else {
                                        visibleIds
                                    }
                                }
                                Spacer(Modifier.width(8.dp))
                            }
                            ShelfAction(
                                label = if (selecting) "Done" else "Select",
                                icon = if (selecting) Icons.Default.Close else Icons.Default.Checklist,
                                tone = ShelfActionTone.Cream
                            ) {
                                SoundFx.tap(ctx)
                                selecting = !selecting
                                if (!selecting) selected = emptySet()
                            }
                        }
                    }
                    Spacer(Modifier.height(12.dp))
                    if (visible.isEmpty()) {
                        // A lone left-aligned sentence read as a mistake in a
                        // panel this wide. Centred under a faded jar, the empty
                        // shelf looks deliberate.
                        val bare = ui.snapshot.entries.isEmpty()
                        Column(
                            Modifier
                                .fillMaxWidth()
                                .padding(vertical = 26.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Outlined.Inventory2,
                                contentDescription = null,
                                tint = Color.White.copy(alpha = 0.26f),
                                modifier = Modifier.size(34.dp)
                            )
                            Spacer(Modifier.height(10.dp))
                            Text(
                                if (bare) "The shelf is bare" else "Nothing in this tab yet",
                                color = Color.White,
                                fontFamily = BeVietnamPro,
                                fontWeight = FontWeight.Bold,
                                fontSize = 13.sp,
                                textAlign = TextAlign.Center
                            )
                            Spacer(Modifier.height(4.dp))
                            Text(
                                if (bare) {
                                    "Solve a dish to earn market runs."
                                } else {
                                    "Draw a palayok, or try another rarity."
                                },
                                color = Color.White.copy(alpha = 0.62f),
                                fontFamily = BeVietnamPro,
                                fontSize = 11.sp,
                                lineHeight = 16.sp,
                                textAlign = TextAlign.Center,
                                modifier = Modifier.fillMaxWidth(0.82f)
                            )
                        }
                    } else {
                        visible.chunked(3).forEach { row ->
                            Row(
                                Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.spacedBy(8.dp)
                            ) {
                                row.forEach { entry ->
                                    PantryStockCard(
                                        entry = entry,
                                        selling = ui.sellingId == entry.ingredient.id,
                                        selecting = selecting,
                                        selected = entry.ingredient.id in selected,
                                        fresh = entry.ingredient.id in ui.newIngredientIds,
                                        modifier = Modifier.weight(1f),
                                        onOpen = {
                                            SoundFx.tap(ctx)
                                            if (selecting) {
                                                selected = if (entry.ingredient.id in selected) {
                                                    selected - entry.ingredient.id
                                                } else {
                                                    selected + entry.ingredient.id
                                                }
                                            } else {
                                                viewModel.inspect(entry.ingredient)
                                            }
                                        },
                                        onSell = {
                                            SoundFx.tap(ctx)
                                            pendingSell = SellAsk.one(entry)
                                        }
                                    )
                                }
                                repeat((3 - row.size).coerceAtLeast(0)) {
                                    Spacer(Modifier.weight(1f))
                                }
                            }
                            Spacer(Modifier.height(8.dp))
                        }
                    }
                }

                Spacer(Modifier.height(8.dp))
                Text(
                    "Part of your KODEX — tap a card to read the page.",
                    color = ShopText.copy(alpha = 0.7f),
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Normal,
                    fontSize = 11.sp,
                    modifier = Modifier.clickable {
                        SoundFx.tap(ctx)
                        onOpenKodex()
                    }
                )
                if (selecting && selected.isNotEmpty()) {
                    Spacer(Modifier.height(72.dp))
                }
            }
        }

        if (selecting && selected.isNotEmpty()) {
            Surface(
                color = ShopText,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier.padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (selectedJars == 1) "1 Ingredient Selected"
                            else "$selectedJars Ingredients Selected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp
                        )
                        Text(
                            "Worth $selectedWorth KK",
                            color = Color(0xFFF1E2C6),
                            fontSize = 11.sp
                        )
                    }
                    Box(
                        Modifier
                            .clip(RoundedCornerShape(12.dp))
                            .background(PlayNowBrown)
                            .clickable {
                                SoundFx.tap(ctx)
                                pendingSell = SellAsk.batch(selectedEntries)
                            }
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            "Sell",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        ui.notice?.let { msg ->
            SellToast(msg, onDismiss = viewModel::dismissNotice)
        }

        pendingSell?.let { ask ->
            SellConfirmDialog(
                ask = ask,
                busy = ui.sellingId != null,
                onDismiss = { pendingSell = null },
                onConfirm = {
                    SoundFx.play(ctx, SoundFx.Cue.Coin)
                    trade = ask
                    viewModel.sellMany(ask.sales)
                    pendingSell = null
                    if (ask.sales.size > 1) {
                        selecting = false
                        selected = emptySet()
                    }
                }
            )
        }

        trade?.let { ask ->
            SellLoadingOverlay(
                ask = ask,
                busy = ui.sellingId != null,
                onFinished = { trade = null }
            )
        }

        ui.inspecting?.let { ingredient ->
            val entry = ui.snapshot.entries.firstOrNull { it.ingredient.id == ingredient.id }
            if (entry != null) {
                IngredientSheet(
                    entry = entry,
                    onClose = {
                        SoundFx.tap(ctx)
                        viewModel.inspect(null)
                    }
                )
            }
        }

        if (showHelp) {
            PantryHelpDialog(onClose = {
                SoundFx.tap(ctx)
                showHelp = false
            })
        }

        // Daily-login wheel. Lives beside the ritual because both hand out
        // palayoks; this one hands out several at once.
        if (showWheel) {
            PalayokWheelOverlay(
                spinsAvailable = ui.snapshot.spinsAvailable,
                spinning = ui.spinning,
                won = ui.spinWon,
                onSpin = viewModel::spin,
                onDone = {
                    viewModel.dismissSpin()
                    showWheel = false
                }
            )
        }

        if (ritual) {
            LaunchedEffect(ui.drawingAll, ui.drawing, ui.snapshot.drawsAvailable, ui.haul.size, ui.reveal) {
                if (!ui.drawingAll &&
                    !ui.drawing &&
                    ui.snapshot.drawsAvailable <= 0 &&
                    ui.reveal == null &&
                    ui.haul.isEmpty()
                ) {
                    ritual = false
                }
            }
            PantryDrawOverlay(
                drawsAvailable = ui.snapshot.drawsAvailable,
                drawing = ui.drawing,
                reveal = ui.reveal,
                onPick = viewModel::draw,
                onDismissReveal = viewModel::dismissReveal,
                onSkip = {
                    ritual = false
                    viewModel.dismissReveal()
                    viewModel.dismissHaul()
                },
                asDialog = false,
                drawingAll = ui.drawingAll,
                onOpenAll = viewModel::drawAll,
                onSellNow = { drawn -> drawn.ingredient?.let { viewModel.sell(it.id, 1) } },
                sellingNow = ui.sellingId != null,
                haul = ui.haul,
                onDismissHaul = viewModel::dismissHaul
            )
        }
    }
}

@Composable
private fun MarketRunBar(
    draws: Int,
    drawing: Boolean,
    drawingAll: Boolean,
    spins: Int,
    onSpin: () -> Unit,
    onDraw: () -> Unit,
    onOpenAll: () -> Unit
) {
    BaulMarketRunCard(
        draws = draws,
        drawing = drawing,
        drawingAll = drawingAll,
        spins = spins,
        onSpin = onSpin,
        onDraw = onDraw,
        onOpenAll = onOpenAll
    )
}

/**
 * Same parchment plate and terracotta OK as in-game How to Play.
 */
@Composable
private fun PantryHelpDialog(onClose: () -> Unit) {
    Dialog(onDismissRequest = onClose) {
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
                    HelpSectionTitle("How the Pantry Works")
                    HelpBodyText("Win dishes, open palayoks, keep what you like and sell the rest.")
                    Spacer(Modifier.height(12.dp))
                    HelpLine(
                        "1",
                        "Win a dish to earn a palayok — open it straight away at the pot."
                    )
                    HelpLine("2", "Open them to collect ingredients.")
                    HelpLine("3", "Got a spare? Sell it for KK.")
                    HelpLine("4", "Tap any card to read its page.")

                    HelpSectionDivider()
                    HelpSectionTitle("What a Jar is Worth")
                    HelpBodyText("Rarity sets the price. Sell one spare and it pays:")
                    Spacer(Modifier.height(14.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Rarity.entries.forEach { rarity ->
                            RarityHelpChip(rarity, Modifier.weight(1f))
                        }
                    }
                }
                Spacer(Modifier.height(16.dp))
                PauseMenuButton(
                    "OK",
                    tone = PauseMenuTone.Primary,
                    compact = true,
                    onClick = onClose
                )
            }
        }
    }
}

@Composable
private fun HelpSectionTitle(text: String) {
    Text(
        text,
        style = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 18.sp,
            color = DarkBrown,
            letterSpacing = 0.2.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    )
    Spacer(Modifier.height(8.dp))
}

@Composable
private fun HelpBodyText(text: String) {
    Text(
        text,
        style = TextStyle(
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Medium,
            fontSize = 15.sp,
            color = HelpInk,
            lineHeight = 23.sp,
            platformStyle = PlatformTextStyle(includeFontPadding = false)
        )
    )
}

@Composable
private fun HelpSectionDivider() {
    Spacer(Modifier.height(16.dp))
    HorizontalDivider(color = HelpSeam, thickness = 1.dp)
    Spacer(Modifier.height(16.dp))
}

/**
 * One rarity tier in the help sheet. No plate or border — the medallion
 * already names its own tier on the ribbon, so it stands on the sheet and
 * carries the price underneath it.
 */
@Composable
private fun RarityHelpChip(rarity: Rarity, modifier: Modifier = Modifier) {
    Column(
        modifier,
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        PixelArt(
            rarity.badge,
            Modifier.size(54.dp),
            contentDescription = rarity.label
        )
        Spacer(Modifier.height(2.dp))
        KkAmount(rarity.sellValue)
    }
}

@Composable
private fun HelpLine(step: String, body: String) {
    Row(
        Modifier.padding(bottom = 10.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(
            step,
            style = TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 15.sp,
                color = DarkBrown,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            modifier = Modifier.width(18.dp)
        )
        Spacer(Modifier.width(8.dp))
        Text(
            body,
            style = TextStyle(
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.SemiBold,
                fontSize = 15.sp,
                color = DarkBrown,
                lineHeight = 20.sp,
                platformStyle = PlatformTextStyle(includeFontPadding = false)
            ),
            modifier = Modifier.weight(1f)
        )
    }
}

/** One owned jar: photograph, count, sell price as a coin, and the sell pill. */
@Composable
private fun PantryStockCard(
    entry: PantryEntry,
    selling: Boolean,
    selecting: Boolean,
    selected: Boolean,
    fresh: Boolean,
    onOpen: () -> Unit,
    onSell: () -> Unit,
    modifier: Modifier = Modifier
) {
    // Rarity has to read from across the grid, so the tier owns the whole
    // card: a solid rim, a tinted body, a deeper wash behind the photograph,
    // and its medallion in the corner.
    val rarity = entry.ingredient.rarity
    val tint = rarity.tint
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(lerp(CardWhite, tint, 0.14f))
            .border(2.dp, tint, RoundedCornerShape(14.dp))
            .clickable(onClick = onOpen)
            .padding(8.dp)
    ) {
        Box(
            Modifier
                .fillMaxWidth()
                .aspectRatio(1f)
                .clip(RoundedCornerShape(10.dp))
                .background(lerp(Color(0xFFF3E6D4), tint, 0.26f))
        ) {
            IngredientPhoto(
                entry.ingredient,
                Modifier
                    .fillMaxSize()
                    .padding(6.dp)
            )
            if (entry.qty > 1) {
                Box(
                    Modifier
                        .align(if (selecting) Alignment.BottomEnd else Alignment.TopEnd)
                        .clip(RoundedCornerShape(50))
                        .background(ShopText.copy(alpha = 0.82f))
                        .padding(horizontal = 5.dp, vertical = 1.dp)
                ) {
                    Text(
                        "x${entry.qty}",
                        color = Color.White,
                        fontSize = 9.sp,
                        fontWeight = FontWeight.Bold
                    )
                }
            }
            // Bottom-start is the one corner nothing else claims: NEW takes
            // the top-start, the count and the tick share the ends.
            PixelArt(
                rarity.badge,
                contentDescription = rarity.label,
                modifier = Modifier
                    .align(Alignment.BottomStart)
                    .padding(3.dp)
                    .size(26.dp)
            )
            if (fresh && !selecting) {
                NewJarBadge(
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(4.dp)
                )
            }
            if (selecting) {
                SelectTick(
                    selected = selected,
                    modifier = Modifier
                        .align(Alignment.TopEnd)
                        .padding(4.dp)
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        Text(
            entry.ingredient.name,
            color = ShopText,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Bold,
            fontSize = 10.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        if (!selecting) {
            Spacer(Modifier.height(6.dp))
            Row(
                Modifier.fillMaxWidth(),
                verticalAlignment = Alignment.CenterVertically
            ) {
                KkAmount(
                    amount = entry.ingredient.rarity.sellValue,
                    modifier = Modifier.weight(1f)
                )
                Box(
                    Modifier
                        .clip(RoundedCornerShape(50))
                        .background(if (selling) HintGray.copy(alpha = 0.5f) else PanelBrown)
                        .clickable(enabled = !selling, onClick = onSell)
                        .padding(horizontal = 10.dp, vertical = 4.dp)
                ) {
                    Text(
                        if (selling) "…" else "Sell",
                        color = Color.White,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.Normal,
                        fontSize = 10.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun KkAmount(amount: Int, modifier: Modifier = Modifier) {
    Row(
        modifier,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            "$amount",
            color = ShopText,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Normal,
            fontSize = 11.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
        Spacer(Modifier.width(3.dp))
        Image(
            painter = painterResource(R.drawable.ic_kk_pixel),
            contentDescription = "KK",
            modifier = Modifier.size(13.dp)
        )
    }
}

private enum class ShelfActionTone { Cream, Gold }

@Composable
private fun ShelfAction(
    label: String,
    icon: ImageVector? = null,
    tone: ShelfActionTone = ShelfActionTone.Cream,
    onClick: () -> Unit
) {
    val gold = tone == ShelfActionTone.Gold
    Surface(
        shape = RoundedCornerShape(50),
        color = if (gold) Color(0xFFFFD24A) else CardWhite,
        border = BorderStroke(1.dp, if (gold) Color(0xFFE0B13A) else Color(0xFFE0C48A)),
        shadowElevation = 2.dp,
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 11.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (icon != null) {
                Icon(
                    icon,
                    contentDescription = null,
                    tint = if (gold) DarkBrown else PlayNowBrown,
                    modifier = Modifier.size(13.dp)
                )
                Spacer(Modifier.width(5.dp))
            }
            Text(
                label,
                color = if (gold) DarkBrown else PlayNowBrown,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 10.sp
            )
        }
    }
}
