package com.example.kusinakode.ui.pantry

import com.example.kusinakode.ui.components.readableWidth
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.grid.GridCells
import androidx.compose.foundation.lazy.grid.GridItemSpan
import androidx.compose.foundation.lazy.grid.LazyVerticalGrid
import androidx.compose.foundation.lazy.grid.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.lerp
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.ui.components.KusinaButton
import com.example.kusinakode.ui.components.KusinaButtonTone
import com.example.kusinakode.ui.components.ParchmentCard
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.domain.pantry.PantryEntry
import com.example.kusinakode.domain.pantry.Rarity
import com.example.kusinakode.ui.home.ChromeCream
import com.example.kusinakode.ui.home.ChromeInk
import com.example.kusinakode.ui.home.ChromeStroke
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import kotlinx.coroutines.delay

internal val PantryInk = Color(0xFF3E2723)
internal val PantryBurnt = Color(0xFFCC6B1F)
internal val PantryCream = Color(0xFFFFFBF3)
private val PantryGold = Color(0xFFFFD24A)
private val PantryBg = Color(0xFFF7EFE3)

@Composable
fun PantryScreen(
    onBack: () -> Unit,
    viewModel: PantryViewModel = viewModel(factory = PantryViewModel.factory())
) {
    val ui by viewModel.uiState.collectAsState()
    var ritual by remember { mutableStateOf(false) }
    var showWheel by remember { mutableStateOf(false) }
    var selecting by remember { mutableStateOf(false) }
    var selected by remember { mutableStateOf(setOf<String>()) }
    var pendingSell by remember { mutableStateOf<SellAsk?>(null) }
    var trade by remember { mutableStateOf<SellAsk?>(null) }
    val ctx = LocalContext.current
    val visibleIds = remember(ui.visible) { ui.visible.map { it.ingredient.id }.toSet() }
    LaunchedEffect(visibleIds) {
        selected = selected.intersect(visibleIds)
    }
    val selectedEntries = ui.visible.filter { it.ingredient.id in selected }
    val selectedJars = selectedEntries.sumOf { it.qty }
    val selectedWorth = selectedEntries.sumOf { it.sellValueKk }

    Box(Modifier.fillMaxSize().background(PantryBg)) {
        Column(Modifier.fillMaxSize()) {
            PantryHeader(
                jars = ui.snapshot.jars,
                worth = ui.snapshot.sellValueKk,
                balanceKk = ui.balanceKk,
                onBack = {
                    SoundFx.play(ctx, SoundFx.Cue.Nav)
                    onBack()
                }
            )
            when {
                ui.isGuest -> GuestState()
                else -> LazyVerticalGrid(
                    columns = GridCells.Fixed(2),
                    modifier = Modifier.fillMaxSize(),
                    contentPadding = PaddingValues(
                        start = 16.dp,
                        end = 16.dp,
                        top = 16.dp,
                        bottom = if (selecting && selected.isNotEmpty()) 88.dp else 16.dp
                    ),
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    item(span = { GridItemSpan(2) }) {
                        DrawBanner(
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
                    }
                    item(span = { GridItemSpan(2) }) {
                        RarityFilters(
                            selected = ui.filter,
                            countOf = { ui.countOf(it) },
                            onSelect = {
                                SoundFx.tap(ctx)
                                viewModel.setFilter(it)
                            }
                        )
                    }
                    if (ui.visible.isNotEmpty()) {
                        item(span = { GridItemSpan(2) }) {
                            SelectBar(
                                selecting = selecting,
                                selectedCount = selected.size,
                                allSelected = selected.isNotEmpty() && selected.containsAll(visibleIds),
                                onToggleMode = {
                                    SoundFx.tap(ctx)
                                    selecting = !selecting
                                    if (!selecting) selected = emptySet()
                                },
                                onSelectAll = {
                                    SoundFx.tap(ctx)
                                    selected = if (selected.containsAll(visibleIds)) emptySet() else visibleIds
                                }
                            )
                        }
                    }
                    if (ui.snapshot.entries.isEmpty()) {
                        item(span = { GridItemSpan(2) }) { EmptyShelf() }
                    } else {
                        items(ui.visible, key = { it.ingredient.id }) { entry ->
                            IngredientCard(
                                entry = entry,
                                selling = ui.sellingId == entry.ingredient.id,
                                selecting = selecting,
                                selected = entry.ingredient.id in selected,
                                fresh = entry.ingredient.id in ui.newIngredientIds,
                                onOpen = {
                                    SoundFx.tap(ctx)
                                    if (selecting) {
                                        selected = selected.toggle(entry.ingredient.id)
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
                    }
                }
            }
        }

        if (selecting && selected.isNotEmpty()) {
            Surface(
                color = PantryInk,
                shadowElevation = 10.dp,
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
            ) {
                Row(
                    Modifier
                        .navigationBarsPadding()
                        .padding(horizontal = 16.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(
                            if (selectedJars == 1) "1 ingredient selected"
                            else "$selectedJars ingredients selected",
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
                    Surface(
                        shape = RoundedCornerShape(12.dp),
                        color = PantryBurnt,
                        modifier = Modifier.clickable {
                            SoundFx.tap(ctx)
                            pendingSell = SellAsk.batch(selectedEntries)
                        }
                    ) {
                        Text(
                            "Sell selected",
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 10.dp)
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
            // Open-all empties the stack, so the ritual has nothing left to
            // deal — close it and let the shelf and the toast do the talking.
            LaunchedEffect(ui.drawingAll, ui.snapshot.drawsAvailable, ui.haul.size) {
                if (!ui.drawingAll &&
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
                asDialog = true,
                drawingAll = ui.drawingAll,
                drawnSoFar = ui.drawnSoFar,
                drawTarget = ui.drawTarget,
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
private fun PantryHeader(
    jars: Int,
    worth: Int,
    balanceKk: Long,
    onBack: () -> Unit
) {
    val kicker = if (jars <= 0) "Nothing on the shelf yet"
    else if (jars == 1) "1 ingredient · worth $worth KK"
    else "$jars ingredients · worth $worth KK"
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
            Box(
                Modifier
                    .size(36.dp)
                    .clip(CircleShape)
                    .background(ChromeCream)
                    .border(1.dp, ChromeStroke, CircleShape)
                    .clickable(onClick = onBack),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    tint = ChromeInk,
                    modifier = Modifier.size(18.dp)
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    "PANTRY",
                    color = Color.White,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 17.sp
                )
                Text(
                    kicker,
                    color = Color(0xFFF1E2C6).copy(alpha = 0.92f),
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Medium,
                    fontSize = 12.sp
                )
            }
            Surface(
                shape = RoundedCornerShape(50),
                color = PantryGold,
                border = BorderStroke(1.dp, Color(0xFFE0B83A))
            ) {
                Row(
                    Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_kk_pixel),
                        contentDescription = "KK",
                        modifier = Modifier.size(16.dp)
                    )
                    Spacer(Modifier.width(6.dp))
                    Text(
                        "$balanceKk KK",
                        color = ChromeInk,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 1
                    )
                }
            }
        }
    }
}

@Composable
private fun DrawBanner(
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

@Composable
private fun RarityFilters(
    selected: Rarity?,
    countOf: (Rarity) -> Int,
    onSelect: (Rarity?) -> Unit
) {
    Row(
        Modifier.horizontalScroll(rememberScrollState()),
        horizontalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        FilterPill("All", selected == null, PantryInk, null) { onSelect(null) }
        Rarity.entries.forEach { rarity ->
            FilterPill(
                label = "${rarity.label} ${countOf(rarity)}",
                selected = selected == rarity,
                tint = rarity.tint,
                badge = rarity.badge
            ) { onSelect(rarity) }
        }
    }
}

@Composable
private fun FilterPill(
    label: String,
    selected: Boolean,
    tint: Color,
    badge: Int?,
    onClick: () -> Unit
) {
    val bg = if (selected) tint else Color(0xFFF3E6D4)
    val ink = if (selected) Color.White else PantryInk
    Surface(
        shape = RoundedCornerShape(50),
        color = bg,
        border = BorderStroke(
            1.dp,
            if (selected) tint else Color(0xFFC4A574)
        ),
        modifier = Modifier.clickable(onClick = onClick)
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (badge != null) {
                PixelArt(badge, Modifier.size(18.dp), contentDescription = null)
                Spacer(Modifier.width(6.dp))
            }
            Text(label, color = ink, fontSize = 11.sp, fontWeight = FontWeight.Bold, maxLines = 1)
        }
    }
}

@Composable
private fun IngredientCard(
    entry: PantryEntry,
    selling: Boolean,
    selecting: Boolean,
    selected: Boolean,
    fresh: Boolean,
    onOpen: () -> Unit,
    onSell: () -> Unit
) {
    // Same rarity read as the shelf in the KODEX pantry: a solid rim, a tinted
    // body, and a deeper wash behind the photograph.
    val tint = entry.ingredient.rarity.tint
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = lerp(PantryCream, tint, 0.14f),
        border = BorderStroke(2.dp, tint),
        shadowElevation = 3.dp,
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onOpen)
    ) {
        Column(Modifier.padding(10.dp)) {
            Box(
                Modifier
                    .fillMaxWidth()
                    .aspectRatio(1f)
                    .clip(RoundedCornerShape(12.dp))
                    .background(lerp(Color(0xFFF1E6D2), tint, 0.26f))
            ) {
                IngredientPhoto(entry.ingredient, Modifier.fillMaxSize())
                PixelArt(
                    entry.ingredient.rarity.badge,
                    contentDescription = entry.ingredient.rarity.label,
                    modifier = Modifier
                        .align(Alignment.TopStart)
                        .padding(6.dp)
                        .size(26.dp)
                )
                if (entry.qty > 1) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = PantryInk.copy(alpha = 0.82f),
                        modifier = Modifier
                            .align(if (selecting) Alignment.BottomEnd else Alignment.TopEnd)
                            .padding(6.dp)
                    ) {
                        Text(
                            "x${entry.qty}",
                            color = Color.White,
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            modifier = Modifier.padding(horizontal = 7.dp, vertical = 3.dp)
                        )
                    }
                }
                if (fresh && !selecting) {
                    NewJarBadge(
                        modifier = Modifier
                            .align(Alignment.BottomStart)
                            .padding(6.dp)
                    )
                }
                if (selecting) {
                    SelectTick(
                        selected = selected,
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .padding(6.dp)
                    )
                }
            }
            Spacer(Modifier.height(8.dp))
            Text(
                entry.ingredient.name,
                color = PantryInk,
                fontFamily = BeVietnamPro,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            // Count lives on the x2 badge over the photo; a second copy here
            // only repeats it, and reads as clutter when the count is 1.
            if (!selecting) {
                Spacer(Modifier.height(8.dp))
                Surface(
                    shape = RoundedCornerShape(10.dp),
                    color = if (selling) HintGray.copy(alpha = 0.35f) else PantryBurnt,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable(enabled = !selling, onClick = onSell)
                ) {
                    Text(
                        if (selling) "Selling" else "Sell  ${entry.ingredient.rarity.sellValue}",
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }
        }
    }
}

@Composable
internal fun IngredientSheet(
    entry: PantryEntry,
    onClose: () -> Unit
) {
    Dialog(onDismissRequest = onClose) {
        ParchmentCard(contentPadding = 20.dp) {
            Column(
                Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
            ) {
                Box(
                    Modifier
                        .fillMaxWidth()
                        .aspectRatio(1.35f)
                        .clip(RoundedCornerShape(14.dp))
                        .background(Color(0xFFF1E6D2))
                ) {
                    IngredientPhoto(entry.ingredient, Modifier.fillMaxSize())
                }
                // Header spans the screen; the content below is capped so a tablet
                // gets a readable column rather than full-width rows.
                Column(Modifier.align(Alignment.CenterHorizontally).readableWidth()) {

                Spacer(Modifier.height(12.dp))
                Text(
                    entry.ingredient.name,
                    color = PantryInk,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 20.sp
                )
                if (!entry.ingredient.name.contains(entry.ingredient.localName, ignoreCase = true)) {
                    Text(entry.ingredient.localName, color = HintGray, fontSize = 13.sp)
                }
                Spacer(Modifier.height(8.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    PixelArt(entry.ingredient.rarity.badge, Modifier.size(28.dp), contentDescription = null)
                    Spacer(Modifier.width(8.dp))
                    Text(
                        "${entry.ingredient.rarity.label} · ${entry.qty} On the shelf",
                        color = PantryInk,
                        fontWeight = FontWeight.Bold,
                        fontSize = 12.sp
                    )
                }
                Spacer(Modifier.height(10.dp))
                Text(entry.ingredient.lore, color = HintGray, fontSize = 13.sp, lineHeight = 18.sp)
                Spacer(Modifier.height(6.dp))
                Text(
                    entry.ingredient.origin,
                    color = originTint(entry.ingredient.origin),
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Bold,
                    fontSize = 11.sp
                )
            
                }
            }
        }
    }
}

/** Chili red for condiments, greens for veg, and so on — not the same gray as lore. */
private fun originTint(origin: String): Color {
    val o = origin.lowercase()
    return when {
        "condiment" in o || "seasoning" in o -> Color(0xFFB33A1A)
        "protein" in o -> Color(0xFF8B3A1F)
        "vegetable" in o -> Color(0xFF3D6B3A)
        "fruit" in o -> Color(0xFFC45C26)
        "spice" in o || "herb" in o -> Color(0xFFB07A1A)
        "dairy" in o -> Color(0xFF8B6914)
        "grain" in o || "noodle" in o || "baking" in o -> Color(0xFF9A6B2F)
        "oil" in o || "fat" in o -> Color(0xFFC49A24)
        "tool" in o || "wrapper" in o || "cookware" in o || "utensil" in o -> Color(0xFF6B5B4F)
        "liquid" in o -> Color(0xFF4A6FA5)
        else -> Color(0xFF92441D)
    }
}

@Composable
private fun SellAmountLabel(text: String, showCoin: Boolean) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 10.dp),
        horizontalArrangement = Arrangement.Center,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text,
            color = Color.White,
            fontWeight = FontWeight.Bold
        )
        if (showCoin) {
            Spacer(Modifier.width(4.dp))
            Image(
                painter = painterResource(R.drawable.ic_kk_pixel),
                contentDescription = "KK",
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

internal data class SellAsk(
    val title: String,
    val body: String,
    val sales: List<Pair<String, Int>>,
    val entries: List<PantryEntry> = emptyList()
) {
    val jars: Int get() = sales.sumOf { it.second }
    val worthKk: Int get() = sales.sumOf { (id, qty) ->
        val value = entries.firstOrNull { it.ingredient.id == id }?.ingredient?.rarity?.sellValue ?: 0
        value * qty
    }
    companion object {
        fun one(entry: PantryEntry) = SellAsk(
            title = "Sell ${entry.ingredient.name}?",
            body = "1 ingredient for ${entry.ingredient.rarity.sellValue} KK. This cannot be undone.",
            sales = listOf(entry.ingredient.id to 1),
            entries = listOf(entry)
        )

        fun stack(entry: PantryEntry) = SellAsk(
            title = "Sell all ${entry.ingredient.name}?",
            body = "${entry.qty} ingredients for ${entry.sellValueKk} KK. This cannot be undone.",
            sales = listOf(entry.ingredient.id to entry.qty),
            entries = listOf(entry)
        )

        fun batch(entries: List<PantryEntry>): SellAsk {
            val jars = entries.sumOf { it.qty }
            val worth = entries.sumOf { it.sellValueKk }
            val kinds = entries.size
            return SellAsk(
                title = if (kinds == 1) "Sell this ingredient?" else "Sell $kinds ingredients?",
                body = if (jars == 1) "1 ingredient for $worth KK. This cannot be undone."
                else "$jars ingredients for $worth KK. This cannot be undone.",
                sales = entries.map { it.ingredient.id to it.qty },
                entries = entries
            )
        }
    }
}

@Composable
internal fun SellConfirmDialog(
    ask: SellAsk,
    busy: Boolean,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = { if (!busy) onDismiss() }) {
        ParchmentCard {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    ask.title,
                    color = PantryInk,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 18.sp
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    ask.body,
                    color = HintGray,
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )
                Spacer(Modifier.height(16.dp))
                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    KusinaButton(
                        label = "Cancel",
                        onClick = onDismiss,
                        tone = KusinaButtonTone.Parchment,
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    )
                    KusinaButton(
                        label = if (busy) "Selling…" else "Sell",
                        onClick = onConfirm,
                        tone = KusinaButtonTone.Terracotta,
                        enabled = !busy,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun SelectBar(
    selecting: Boolean,
    selectedCount: Int,
    allSelected: Boolean,
    onToggleMode: () -> Unit,
    onSelectAll: () -> Unit
) {
    Row(
        Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            if (selecting) {
                if (selectedCount == 0) "Tick the ingredients to sell" else "$selectedCount selected"
            } else {
                "Sell several"
            },
            color = HintGray,
            fontSize = 12.sp,
            modifier = Modifier.weight(1f)
        )
        if (selecting) {
            Text(
                if (allSelected) "Clear" else "Select all",
                color = PantryBurnt,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier
                    .clickable(onClick = onSelectAll)
                    .padding(horizontal = 8.dp, vertical = 4.dp)
            )
        }
        Text(
            if (selecting) "Done" else "Select",
            color = PantryInk,
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            modifier = Modifier
                .clickable(onClick = onToggleMode)
                .padding(horizontal = 4.dp, vertical = 4.dp)
        )
    }
}

@Composable
internal fun NewJarBadge(modifier: Modifier = Modifier) {
    Box(
        modifier
            .clip(RoundedCornerShape(50))
            .background(PantryBurnt)
            .padding(horizontal = 6.dp, vertical = 2.dp)
    ) {
        Text(
            "NEW",
            color = Color.White,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.sp,
            letterSpacing = 0.8.sp
        )
    }
}

@Composable
internal fun SelectTick(selected: Boolean, modifier: Modifier = Modifier) {
    Box(
        modifier
            .size(24.dp)
            .clip(CircleShape)
            .background(if (selected) PantryBurnt else Color.White.copy(alpha = 0.92f))
            .border(
                1.5.dp,
                if (selected) PantryBurnt else PantryInk.copy(alpha = 0.35f),
                CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        if (selected) {
            Icon(
                Icons.Default.Check,
                contentDescription = "Selected",
                tint = Color.White,
                modifier = Modifier.size(14.dp)
            )
        }
    }
}

private fun Set<String>.toggle(id: String): Set<String> =
    if (id in this) this - id else this + id

@Composable
internal fun GuestState() {
    Column(
        Modifier
            .fillMaxSize()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text("Sign in to keep a pantry", color = PantryInk, fontWeight = FontWeight.ExtraBold, fontSize = 16.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(8.dp))
        Text(
            "Ingredients are tied to your account, so they survive a reinstall.",
            color = HintGray,
            fontSize = 13.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
private fun EmptyShelf() {
    Column(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 28.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text("The shelf is bare", color = PantryInk, fontWeight = FontWeight.ExtraBold, fontSize = 15.sp)
        Spacer(Modifier.height(6.dp))
        Text(
            "Solve a dish to earn market runs, then open them here.",
            color = HintGray,
            fontSize = 12.sp,
            textAlign = TextAlign.Center
        )
    }
}

@Composable
internal fun SellToast(message: String, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        delay(2_600)
        onDismiss()
    }
    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFE9E9ED),
            shadowElevation = 4.dp,
            modifier = Modifier.padding(start = 24.dp, end = 24.dp, bottom = 28.dp)
        ) {
            Text(
                message,
                color = PantryInk,
                fontSize = 13.sp,
                maxLines = 3,
                overflow = TextOverflow.Ellipsis,
                modifier = Modifier.padding(horizontal = 16.dp, vertical = 10.dp)
            )
        }
    }
}
