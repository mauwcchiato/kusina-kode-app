package com.example.kusinakode.ui.rewards

import com.example.kusinakode.ui.components.LevelImage
import com.example.kusinakode.ui.components.clickSfx
import androidx.annotation.DrawableRes
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.filled.Archive
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Checklist
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.SelectAll
import androidx.compose.material.icons.outlined.Circle
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.DoneAll
import androidx.compose.material.icons.filled.MarkEmailRead
import androidx.compose.material.icons.filled.MarkEmailUnread
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material.icons.filled.Unarchive
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.PlayNowBrown
import com.example.kusinakode.R
import com.example.kusinakode.ui.components.FilterPillRow
import com.example.kusinakode.ui.components.RewardReceipt
import com.example.kusinakode.ui.components.RewardReceiptData
import com.example.kusinakode.ui.pantry.PalayokSpinSheet
import com.example.kusinakode.ui.pantry.PantryViewModel
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange

/**
 * Which half of the inbox to show.
 *
 * The screen already had two sections; these chips let a player look at one of
 * them on its own instead of scrolling past the other.
 */
private enum class InboxFilter(val label: String) {
    ALL("All"),
    /** The "waiting for you" block: daily claim, spins, islands, badges. */
    DAILY("Daily Reward"),
    /** The ledger: what has already been written to the chain. */
    CHAIN("On-Chain Activity")
}

private val CreamBg = Color(0xFFF1E6D2)
private val TextDark = Color(0xFF3E2723)
private val CardCream = Color(0xFFFFFBF3)
private val CardEdge = Color(0xFFE0C48A)
private val NoteWash = Color(0xFFF6E2B8)
private val NoteEdge = Color(0xFFD4A24A)

/**
 * In-app activity feed for rewards.
 *
 * System notifications only fire while something is actually happening, and
 * they vanish once dismissed — so there was nowhere to look afterwards. This
 * is that place: what is waiting to be claimed, and every reward the ledger
 * has already recorded, newest first.
 *
 * Reuses [RewardsViewModel] rather than adding a second loader; it already
 * fetches earn status and history for the Rewards screen.
 */
@Composable
fun NotificationsScreen(
    onBack: () -> Unit,
    onOpenRewards: () -> Unit,
    /** Where the palayok spin is redeemed. */
    onOpenMarketRun: () -> Unit = {},
    viewModel: RewardsViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    // The pantry owns the spin count, so the inbox has to ask it directly —
    // the rewards payload knows nothing about palayoks.
    val pantry: PantryViewModel = viewModel(factory = PantryViewModel.factory())
    val pantryUi by pantry.uiState.collectAsState()
    val lifecycleOwner = LocalLifecycleOwner.current
    DisposableEffect(lifecycleOwner) {
        val observer = LifecycleEventObserver { _, event ->
            if (event == Lifecycle.Event.ON_RESUME) {
                viewModel.refresh()
                pantry.load()
            }
        }
        lifecycleOwner.lifecycle.addObserver(observer)
        onDispose { lifecycleOwner.lifecycle.removeObserver(observer) }
    }
    // The wheel overlay sits above this destination, so Activity resume
    // does not fire when the spin lands. Pull history as soon as palayoks
    // are won so On-Chain Activity already has the row.
    LaunchedEffect(pantryUi.spinWon) {
        if (pantryUi.spinWon != null) viewModel.refresh()
    }

    val claimable = buildList {
        // Daily KK and the palayok that ships with it. One palayok row only —
        // never "Free palayok spin" plus "Palayok spin waiting" at once.
        if (ui.dailyClaimable) {
            add(
                ClaimRow(
                    glyph = NotificationArt.forEvent("daily_login", null),
                    title = "Daily Login Claim",
                    detail = "+${ui.dailyAmount} KK to claim",
                    action = onOpenRewards
                )
            )
            add(
                ClaimRow(
                    glyph = NotificationGlyph.Art(R.drawable.baul_closed, fill = false),
                    title = "Daily Palayok Spin",
                    detail = "Comes with today's login claim",
                    action = { PalayokSpinSheet.open() }
                )
            )
        } else if (pantryUi.snapshot.spinsAvailable > 0) {
            add(
                ClaimRow(
                    glyph = NotificationGlyph.Art(R.drawable.baul_closed, fill = false),
                    title = "Daily Palayok Spin",
                    detail = "Spin for 3, 5 or 8 palayoks",
                    action = { PalayokSpinSheet.open() }
                )
            )
        }
        ui.islands.filter { it.claimable }.forEach {
            add(
                ClaimRow(
                    glyph = NotificationArt.forEvent("island_complete", it.id.ifBlank { it.name }),
                    title = "${it.name} complete",
                    detail = "+${it.amount_kk} KK to claim",
                    action = onOpenRewards
                )
            )
        }
        ui.badges.filter { it.claimable }.forEach {
            add(
                ClaimRow(
                    glyph = NotificationArt.forEvent("badge_milestone", it.id),
                    title = it.title,
                    detail = "+${it.amount_kk} KK to claim",
                    action = onOpenRewards
                )
            )
        }
    }

    Column(
        Modifier
            .fillMaxSize()
            .background(CreamBg)
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
                    .padding(horizontal = 16.dp, vertical = 14.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                HeaderCircleButton(
                    icon = Icons.AutoMirrored.Filled.ArrowBack,
                    contentDescription = "Back",
                    onClick = clickSfx { onBack() }
                )
                Spacer(Modifier.width(12.dp))
                Column {
                    Text(
                        "Notifications",
                        color = Color.White,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 17.sp
                    )
                    Text(
                        "Rewards activity and what is waiting",
                        color = LightOrange.copy(alpha = 0.9f),
                        fontFamily = BeVietnamPro,
                        fontSize = 11.sp
                    )
                }
            }
        }

        val ctx = LocalContext.current
        LaunchedEffect(Unit) { NotificationStore.load(ctx) }
        val readIds by NotificationStore.read.collectAsState()
        val archivedIds by NotificationStore.archived.collectAsState()
        val deletedIds by NotificationStore.deleted.collectAsState()
        var showArchived by remember { mutableStateOf(false) }
        var filter by rememberSaveable { mutableStateOf(InboxFilter.ALL) }
        var selecting by remember { mutableStateOf(false) }
        var selected by remember { mutableStateOf(emptySet<String>()) }

        // Switching views changes what is on screen underneath a selection, so
        // the selection goes with it rather than following the player around.
        LaunchedEffect(showArchived, filter) {
            selecting = false
            selected = emptySet()
        }

        Column(Modifier.padding(16.dp)) {

            FilterPillRow(
                options = InboxFilter.entries,
                selected = filter,
                labelOf = { it.label },
                onSelect = { filter = it }
            )
            Spacer(Modifier.height(16.dp))

            if (filter != InboxFilter.CHAIN && claimable.isNotEmpty()) {
                SectionHeading("WAITING FOR YOU")
                claimable.forEach { row ->
                    FeedRow(
                        glyph = row.glyph,
                        tint = Color(0xFFCC6B1F),
                        title = row.title,
                        detail = row.detail,
                        onClick = row.action,
                        // Still sitting here means it hasn't been claimed —
                        // same unread mark the ledger uses for unopened rows.
                        unread = true
                    )
                }
                Spacer(Modifier.height(18.dp))
            }

            // On "All" an empty claim list just means the block is skipped and
            // the ledger follows. Asked for this block on its own, silence has
            // to say something.
            if (filter == InboxFilter.DAILY && claimable.isEmpty()) {
                SectionHeading("WAITING FOR YOU")
                InboxCard {
                    Text(
                        "Nothing to claim right now. Come back tomorrow for the " +
                            "daily claim, or finish an island to unlock its reward.",
                        color = HintGray,
                        fontFamily = BeVietnamPro,
                        fontSize = 12.sp,
                        lineHeight = 18.sp,
                        modifier = Modifier.padding(14.dp)
                    )
                }
            }

            val live = ui.history.filter { it.tx_ref !in deletedIds }
            val inbox = live
                .filter { (it.tx_ref in archivedIds) == showArchived }
                .sortedWith(::onChainOrder)
            val unread = live.count { it.tx_ref != null && it.tx_ref !in readIds }

            if (filter != InboxFilter.DAILY) {
                val inboxIds = inbox.mapNotNull { it.tx_ref }
                // Rows can leave under the selection — archived from another
                // view, deleted, filtered away — so anything no longer on
                // screen is dropped rather than acted on invisibly later.
                val picked = selected intersect inboxIds.toSet()

                Column(Modifier.fillMaxWidth()) {
                    SectionHeading(
                        when {
                            selecting -> if (picked.isEmpty()) {
                                "SELECT ENTRIES"
                            } else {
                                "${picked.size} SELECTED"
                            }
                            showArchived -> "ARCHIVED"
                            else -> "ON-CHAIN ACTIVITY"
                        }
                    )
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp, Alignment.End),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                    if (selecting) {
                        InboxAction(
                            Icons.Default.SelectAll,
                            if (picked.size == inboxIds.size && inboxIds.isNotEmpty()) {
                                "Clear"
                            } else {
                                "Select all"
                            }
                        ) {
                            selected = if (picked.size == inboxIds.size) {
                                emptySet()
                            } else {
                                inboxIds.toSet()
                            }
                        }
                        InboxAction(Icons.Default.Close, "Done") {
                            selecting = false
                            selected = emptySet()
                        }
                    } else {
                        if (unread > 0 && !showArchived) {
                            InboxAction(Icons.Default.DoneAll, "Mark all read") {
                                NotificationStore.markAllRead(ctx, live.mapNotNull { it.tx_ref })
                            }
                        }
                        if (inbox.isNotEmpty()) {
                            InboxAction(Icons.Default.Checklist, "Select") {
                                selecting = true
                                selected = emptySet()
                            }
                        }
                        InboxAction(
                            if (showArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            if (showArchived) "Back to inbox" else "Archived"
                        ) { showArchived = !showArchived }
                    }
                    }
                }

                // The bulk bar only appears once something is actually picked:
                // three disabled buttons above an untouched list is furniture.
                if (selecting && picked.isNotEmpty()) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        BulkAction(
                            Icons.Default.MarkEmailRead,
                            "Read",
                            Modifier.weight(1f)
                        ) {
                            NotificationStore.markAllRead(ctx, picked)
                            selecting = false
                            selected = emptySet()
                        }
                        BulkAction(
                            if (showArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                            if (showArchived) "Restore" else "Archive",
                            Modifier.weight(1f)
                        ) {
                            NotificationStore.archiveAll(ctx, picked, !showArchived)
                            selecting = false
                            selected = emptySet()
                        }
                        BulkAction(
                            Icons.Default.Delete,
                            "Delete",
                            Modifier.weight(1f),
                            danger = true
                        ) {
                            NotificationStore.deleteAll(ctx, picked)
                            selecting = false
                            selected = emptySet()
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
                if (inbox.isEmpty()) {
                    InboxCard {
                        Text(
                            "Nothing recorded yet. Solve a dish or earn a badge and the " +
                                "mint will show up here.",
                            color = HintGray,
                            fontFamily = BeVietnamPro,
                            fontSize = 12.sp,
                            lineHeight = 18.sp,
                            modifier = Modifier.padding(14.dp)
                        )
                    }
                } else {
                    inbox.flatMap { ledgerLines(it) }.forEachIndexed { i, line ->
                            val hash = line.row.tx_hash
                            val ref = line.row.tx_ref
                            key("${ref.orEmpty()}#$i") {
                                FeedRow(
                                    glyph = line.glyph,
                                    tint = if (hash != null) Color(0xFF3D6B3A) else HintGray,
                                    title = line.title,
                                    detail = line.detail,
                                    onClick = {
                                        when {
                                            ref.isNullOrBlank() -> Unit
                                            selecting -> selected =
                                                if (ref in selected) selected - ref else selected + ref
                                            else -> {
                                                NotificationStore.markRead(ctx, ref)
                                                RewardReceipt.show(
                                                    RewardReceiptData(
                                                        title = line.title,
                                                        reference = ref,
                                                        recordedAt = line.row.created_at,
                                                        amountKk = if (line.kind == LedgerKind.Spin) {
                                                            null
                                                        } else {
                                                            line.row.amount_kk
                                                        },
                                                        verified = hash != null,
                                                        amountLine = if (line.kind == LedgerKind.Spin) {
                                                            palayoksWonFromSpinTitle(line.row.title)
                                                                ?.let { "+$it palayoks" }
                                                        } else {
                                                            null
                                                        }
                                                    )
                                                )
                                            }
                                        }
                                    },
                                    unread = ref != null && ref !in readIds,
                                    picked = if (selecting && ref != null) {
                                        ref in selected
                                    } else {
                                        null
                                    },
                                    menu = if (selecting) null else ref?.let { id ->
                                        {
                                            RowMenu(
                                                isRead = id in readIds,
                                                isArchived = id in archivedIds,
                                                onToggleRead = {
                                                    NotificationStore.markRead(ctx, id, id !in readIds)
                                                },
                                                onToggleArchive = {
                                                    NotificationStore.archive(ctx, id, id !in archivedIds)
                                                },
                                                onDelete = { NotificationStore.delete(ctx, id) }
                                            )
                                        }
                                    }
                                )
                            }
                        }
                }
            }

            Spacer(Modifier.height(18.dp))
            InboxCard(color = NoteWash, border = NoteEdge) {
                Row(
                    Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        Icons.Default.NotificationsActive,
                        contentDescription = null,
                        tint = PlayNowBrown,
                        modifier = Modifier.size(18.dp)
                    )
                    Spacer(Modifier.width(12.dp))
                    Text(
                        "These also arrive as phone notifications - a badge finishing " +
                            "its mint, and your daily claim when you open the app.",
                        color = PlayNowBrown,
                        fontFamily = BeVietnamPro,
                        fontSize = 11.sp,
                        lineHeight = 16.sp
                    )
                }
            }
            Spacer(Modifier.height(16.dp))
        }
    }
}

/**
 * Newest first, except the same day's daily pair: login claim sits above
 * the palayok spin so the two read as Daily Login Claim / Daily Palayok Spin.
 */
private fun onChainOrder(
    a: com.example.kusinakode.api.RewardHistoryItem,
    b: com.example.kusinakode.api.RewardHistoryItem
): Int {
    val ta = parseHistoryLocal(a.created_at.orEmpty()) ?: java.time.LocalDateTime.MIN
    val tb = parseHistoryLocal(b.created_at.orEmpty()) ?: java.time.LocalDateTime.MIN
    val daily = setOf("daily_login", "palayok_spin")
    if (a.event_type in daily && b.event_type in daily && ta.toLocalDate() == tb.toLocalDate()) {
        return dailyPairRank(a.event_type).compareTo(dailyPairRank(b.event_type))
    }
    return tb.compareTo(ta)
}

private fun dailyPairRank(eventType: String?): Int = when (eventType) {
    "daily_login" -> 0
    "palayok_spin" -> 1
    else -> 2
}

private fun statusLabel(status: String?): String {
    val raw = status?.trim().orEmpty()
    if (raw.isEmpty()) return "Pending"
    return raw.replaceFirstChar { ch ->
        if (ch.isLowerCase()) ch.titlecase(java.util.Locale.US) else ch.toString()
    }
}

private fun labelForEvent(eventType: String?): String = when (eventType) {
    "round_win" -> "Dish solved"
    "badge_milestone" -> "Badge minted"
    "daily_login" -> "Daily Login Claim"
    "island_complete" -> "Island complete"
    "reward_redemption" -> "Unlocked with KK"
    "palayok_spin" -> "Daily Palayok Spin"
    "account_signup" -> "Account Sign Up"
    "account_login" -> "Account Login"
    else -> "Reward"
}

private enum class LedgerKind { Kk, Spin, Other }

private data class LedgerLine(
    val row: com.example.kusinakode.api.RewardHistoryItem,
    val title: String,
    val detail: String,
    val glyph: NotificationGlyph,
    val kind: LedgerKind
)

/** One ledger row per mint. A daily_login is the KK claim only. */
private fun ledgerLines(row: com.example.kusinakode.api.RewardHistoryItem): List<LedgerLine> {
    val spend = row.event_type == "reward_redemption"
    fun statusTail(): String = buildString {
        append(statusLabel(row.status))
        if (!row.tx_ref.isNullOrBlank()) append(" · View receipt")
    }
    if (row.event_type == "palayok_spin") {
        val won = palayoksWonFromSpinTitle(row.title)
        val palayokDetail = buildString {
            if (won != null) append("+$won palayoks")
            if (isNotEmpty()) append(" · ")
            append(statusTail())
        }
        return listOf(
            LedgerLine(
                row = row,
                title = "Daily Palayok Spin",
                detail = palayokDetail,
                glyph = NotificationArt.forRow(row),
                kind = LedgerKind.Spin
            )
        )
    }
    if (row.event_type == "account_signup" || row.event_type == "account_login") {
        return listOf(
            LedgerLine(
                row = row,
                title = if (row.event_type == "account_signup") "Account Sign Up" else "Account Login",
                detail = statusTail(),
                glyph = NotificationArt.forRow(row),
                kind = LedgerKind.Other
            )
        )
    }
    val title = if (row.tx_hash != null && row.event_type == "badge_milestone") {
        "Badge Successfully Minted"
    } else {
        rewardTitleOrNull(row.title) ?: labelForEvent(row.event_type)
    }
    val kkDetail = buildString {
        row.amount_kk?.let { append(if (spend) "−$it KK" else "+$it KK") }
        if (isNotEmpty()) append(" · ")
        append(statusTail())
    }
    val main = LedgerLine(
        row = row,
        title = if (row.event_type == "daily_login") "Daily Login Claim" else title,
        detail = kkDetail,
        glyph = NotificationArt.forRow(row),
        kind = if (row.event_type == "daily_login") LedgerKind.Kk else LedgerKind.Other
    )
    // The palayok stays in WAITING FOR YOU until the wheel is actually
    // turned. Banking it with the daily KK must not mint a second ledger row.
    return listOf(main)
}

@Composable
private fun SectionHeading(text: String, modifier: Modifier = Modifier) {
    Text(
        text,
        color = TextDark.copy(alpha = 0.55f),
        fontFamily = BeVietnamPro,
        fontSize = 10.sp,
        fontWeight = FontWeight.ExtraBold,
        letterSpacing = 1.6.sp,
        maxLines = 1,
        modifier = modifier.padding(bottom = 8.dp)
    )
}

@Composable
private fun InboxCard(
    color: Color = CardCream,
    border: Color = CardEdge,
    content: @Composable ColumnScope.() -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = color,
        border = BorderStroke(1.dp, border),
        shadowElevation = 2.dp,
        modifier = Modifier.fillMaxWidth()
    ) {
        Column(content = content)
    }
}

/** A small tinted pill for an inbox-wide action. */
@Composable
private fun InboxAction(icon: ImageVector, label: String, onClick: () -> Unit) {
    Surface(
        shape = RoundedCornerShape(20.dp),
        color = CardCream,
        border = BorderStroke(1.dp, CardEdge),
        modifier = Modifier.clickable(onClick = clickSfx { onClick() })
    ) {
        Row(
            Modifier.padding(horizontal = 10.dp, vertical = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = PlayNowBrown, modifier = Modifier.size(14.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = PlayNowBrown,
                fontFamily = BeVietnamPro,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

/**
 * One button in the bulk bar. Wider and squarer than [InboxAction]: these act
 * on a selection the player has just made, so they are targets rather than
 * chips tucked beside a heading.
 */
@Composable
private fun BulkAction(
    icon: ImageVector,
    label: String,
    modifier: Modifier = Modifier,
    danger: Boolean = false,
    onClick: () -> Unit
) {
    val ink = if (danger) Color(0xFF8B1E1E) else PlayNowBrown
    Surface(
        shape = RoundedCornerShape(12.dp),
        color = if (danger) Color(0xFFF8E4E0) else CardCream,
        border = BorderStroke(1.dp, if (danger) Color(0xFF8B1E1E) else CardEdge),
        modifier = modifier.clickable(onClick = clickSfx { onClick() })
    ) {
        Row(
            Modifier.padding(vertical = 9.dp),
            horizontalArrangement = Arrangement.Center,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(icon, contentDescription = null, tint = ink, modifier = Modifier.size(15.dp))
            Spacer(Modifier.width(5.dp))
            Text(
                label,
                color = ink,
                fontFamily = BeVietnamPro,
                fontSize = 11.sp,
                fontWeight = FontWeight.ExtraBold
            )
        }
    }
}

@Composable
private fun FeedRow(
    glyph: NotificationGlyph,
    tint: Color,
    title: String,
    detail: String,
    onClick: () -> Unit,
    unread: Boolean = false,
    /** Null outside select mode; true/false is this row's checkbox. */
    picked: Boolean? = null,
    menu: (@Composable () -> Unit)? = null
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = when {
            picked == true -> Color(0xFFFBE4D1)
            else -> CardCream
        },
        border = BorderStroke(1.dp, CardEdge),
        shadowElevation = 2.dp,
        modifier = Modifier
            .fillMaxWidth()
            .padding(bottom = 8.dp)
            .clickable(onClick = clickSfx { onClick() })
    ) {
        Row(
            Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val photo = (glyph is NotificationGlyph.Art && glyph.fill) ||
                (glyph is NotificationGlyph.Remote && glyph.fill)
            Box(
                Modifier
                    .size(38.dp)
                    .then(if (photo) Modifier.clip(CircleShape) else Modifier),
                contentAlignment = Alignment.Center
            ) {
                when (glyph) {
                    is NotificationGlyph.Art -> Image(
                        painter = painterResource(glyph.res),
                        contentDescription = null,
                        contentScale = if (glyph.fill) ContentScale.Crop else ContentScale.Fit,
                        modifier = if (glyph.fill) {
                            Modifier.matchParentSize()
                        } else {
                            Modifier.size(34.dp)
                        }
                    )

                    is NotificationGlyph.Remote -> LevelImage(
                        url = glyph.url,
                        fallback = glyph.fallback,
                        contentDescription = null,
                        contentScale = if (glyph.fill) ContentScale.Crop else ContentScale.Fit,
                        modifier = if (glyph.fill) Modifier.matchParentSize() else Modifier.size(34.dp)
                    )

                    is NotificationGlyph.Vector -> Icon(
                        glyph.icon,
                        contentDescription = null,
                        tint = tint,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = TextDark,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
                Text(
                    detail,
                    color = HintGray,
                    fontFamily = BeVietnamPro,
                    fontSize = 11.sp,
                    lineHeight = 15.sp
                )
            }
            if (picked != null) {
                Icon(
                    if (picked) Icons.Default.CheckCircle else Icons.Outlined.Circle,
                    contentDescription = if (picked) "Selected" else "Not selected",
                    tint = if (picked) PlayNowBrown else HintGray.copy(alpha = 0.6f),
                    modifier = Modifier.size(22.dp)
                )
            } else {
                if (unread) {
                    Box(
                        Modifier
                            .size(8.dp)
                            .clip(CircleShape)
                            .background(PlayNowBrown)
                    )
                }
                menu?.let {
                    Spacer(Modifier.width(4.dp))
                    it()
                }
            }
        }
    }
}

/** Per-entry actions: read state, archive, delete. */
@Composable
private fun RowMenu(
    isRead: Boolean,
    isArchived: Boolean,
    onToggleRead: () -> Unit,
    onToggleArchive: () -> Unit,
    onDelete: () -> Unit
) {
    var open by remember { mutableStateOf(false) }
    Box {
        Icon(
            Icons.Default.MoreVert,
            contentDescription = "Actions",
            tint = HintGray,
            modifier = Modifier
                .size(20.dp)
                .clickable(onClick = clickSfx { open = true })
        )
        DropdownMenu(expanded = open, onDismissRequest = { open = false }) {
            DropdownMenuItem(
                text = { Text(if (isRead) "Mark as unread" else "Mark as read", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        if (isRead) Icons.Default.MarkEmailUnread else Icons.Default.MarkEmailRead,
                        contentDescription = null
                    )
                },
                onClick = { open = false; onToggleRead() }
            )
            DropdownMenuItem(
                text = { Text(if (isArchived) "Move to inbox" else "Archive", fontSize = 13.sp) },
                leadingIcon = {
                    Icon(
                        if (isArchived) Icons.Default.Unarchive else Icons.Default.Archive,
                        contentDescription = null
                    )
                },
                onClick = { open = false; onToggleArchive() }
            )
            DropdownMenuItem(
                text = { Text("Delete", fontSize = 13.sp, color = Color(0xFFB3261E)) },
                leadingIcon = {
                    Icon(Icons.Default.Delete, contentDescription = null, tint = Color(0xFFB3261E))
                },
                onClick = { open = false; onDelete() }
            )
        }
    }
}

/**
 * A "waiting for you" row. Each carries its own destination — the daily and
 * the badges go to the wallet, the palayok spin goes to the Market Run.
 */
private data class ClaimRow(
    val glyph: NotificationGlyph,
    val title: String,
    val detail: String,
    val action: () -> Unit
)
