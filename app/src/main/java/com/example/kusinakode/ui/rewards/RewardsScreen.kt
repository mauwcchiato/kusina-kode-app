package com.example.kusinakode.ui.rewards

import androidx.annotation.DrawableRes
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AccountBalanceWallet
import androidx.compose.material.icons.filled.History
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Popup
import androidx.compose.ui.window.PopupProperties
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.BottomNavTab
import com.example.kusinakode.KusinaBottomNav
import com.example.kusinakode.LevelProvider
import com.example.kusinakode.R
import com.example.kusinakode.SoundFx
import com.example.kusinakode.api.EarnIslandData
import com.example.kusinakode.api.RewardHistoryItem
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.ui.gamification.GamificationViewModel
import com.example.kusinakode.ui.home.ChromeCream
import com.example.kusinakode.ui.home.ChromeInk
import com.example.kusinakode.ui.home.ChromeStroke
import com.example.kusinakode.ui.home.SectionHeader
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import kotlinx.coroutines.delay
import java.time.Instant
import java.time.LocalDate
import java.time.LocalDateTime
import java.time.ZoneId
import java.time.ZoneOffset
import java.time.format.DateTimeFormatter
import java.util.Locale

internal val RewardsBurntOrange = Color(0xFFB4510E)
internal val RewardsCreamBg = Color(0xFFF7EFE3)
internal val RewardsCreamCard = Color(0xFFF3E6D0)
internal val RewardsTextDark = Color(0xFF3E2723)
private val TanBadge = Color(0xFFE4C49A)
internal val RewardsCreditGreen = Color(0xFF2E7D32)
internal val RewardsDebitRed = Color(0xFFC62828)

/**
 * KK token wallet (Frame 15). Balance comes from
 * [RewardsViewModel] via GET /api/wallet/balance.php (custodial, read-only).
 */
@Composable
fun RewardsScreen(
    onBack: () -> Unit,
    onHome: () -> Unit,
    onExplore: () -> Unit,
    onLeaderboard: () -> Unit,
    onLearn: () -> Unit,
    onProfile: () -> Unit,
    onDocumentaries: () -> Unit = {},
    onEncyclopedia: () -> Unit = {},
    onAvatarMarket: () -> Unit = {},
    onSettings: () -> Unit = {},
    onFullHistory: () -> Unit = {},
    viewModel: RewardsViewModel = viewModel()
) {
    val ui by viewModel.uiState.collectAsState()
    val gamification: GamificationViewModel = viewModel()
    val game by gamification.uiState.collectAsState()


    ClaimToast(ui.notice, viewModel::dismissNotice)

    val earnLines = remember(ui, game.progress.currentStreak, game.progress.solvedLevels) {
        buildEarnLines(
            ui,
            game.progress.currentStreak,
            game.progress.solvedLevels,
            viewModel
        )
    }
    val historyNewestFirst = remember(ui.history) {
        ui.history.sortedByDescending { row ->
            parseHistoryLocal(row.created_at.orEmpty()) ?: java.time.LocalDateTime.MIN
        }
    }
    val historyPreview = historyNewestFirst.take(3)

    Scaffold(
        containerColor = RewardsCreamBg,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Wallet,
                onHome = onHome,
                onProfile = onProfile,
                onLevels = onExplore,
                onWallet = { /* already here */ },
                onCompleted = onLearn
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
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
                    HeaderCircleButton(
                        icon = Icons.AutoMirrored.Filled.ArrowBack,
                        contentDescription = "Back",
                        onClick = onBack
                    )
                    Spacer(Modifier.width(12.dp))
                    Column(Modifier.weight(1f)) {
                        Text(
                            "Rewards",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Bold,
                            fontSize = 17.sp
                        )
                        Text(
                            "Get every token",
                            color = LightOrange.copy(alpha = 0.92f),
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.Medium,
                            fontSize = 12.sp
                        )
                    }
                    Spacer(Modifier.width(10.dp))
                    HeaderCircleButton(
                        icon = Icons.Outlined.Settings,
                        contentDescription = "Settings",
                        onClick = onSettings
                    )
                }
            }

            Column(Modifier.padding(16.dp)) {
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    shadowElevation = 8.dp,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(
                        Modifier.background(
                            Brush.horizontalGradient(
                                listOf(Color(0xFF4A2410), Color(0xFF6B3214), Color(0xFF3D1A0A))
                            )
                        )
                    ) {
                        Row(
                            Modifier.padding(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 12.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                Modifier
                                    .size(48.dp)
                                    .clip(RoundedCornerShape(10.dp))
                                    .background(Color.White.copy(alpha = 0.14f)),
                                contentAlignment = Alignment.Center
                            ) {
                                Icon(
                                    Icons.Default.AccountBalanceWallet,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(26.dp)
                                )
                            }
                            Spacer(Modifier.width(14.dp))
                            Column(Modifier.weight(1f)) {
                                Text(
                                    "Token Balance",
                                    color = TanBadge,
                                    fontSize = 12.sp
                                )
                                Row(verticalAlignment = Alignment.Bottom) {
                                    Text(
                                        "${ui.balanceKk}",
                                        color = Color.White,
                                        fontSize = 34.sp,
                                        fontWeight = FontWeight.ExtraBold,
                                        lineHeight = 36.sp
                                    )
                                    Spacer(Modifier.width(6.dp))
                                    Image(
                                        painter = painterResource(R.drawable.ic_kk_pixel),
                                        contentDescription = "KK",
                                        modifier = Modifier
                                            .padding(bottom = 6.dp)
                                            .size(22.dp)
                                    )
                                }
                                if (!ui.isLive) {
                                    Text(
                                        "Offline — last known balance",
                                        color = TanBadge.copy(alpha = 0.85f),
                                        fontSize = 10.sp
                                    )
                                }
                            }
                        }
                        HorizontalDivider(
                            modifier = Modifier.padding(horizontal = 16.dp),
                            thickness = 1.dp,
                            color = Color(0xFFD4B48A)
                        )
                        Spacer(Modifier.height(12.dp))
                    }
                }

                Spacer(Modifier.height(20.dp))
                Text(
                    "How to Earn More Tokens?",
                    style = MaterialTheme.typography.titleMedium.copy(
                        fontWeight = FontWeight.Bold,
                        color = RewardsTextDark
                    )
                )
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = RewardsCreamCard,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Column(Modifier.padding(horizontal = 12.dp, vertical = 8.dp)) {
                        earnLines.forEach { line ->
                            EarnRow(
                                art = line.art,
                                title = line.title,
                                tag = line.tag,
                                reward = line.reward,
                                action = line.action,
                                actionEnabled = line.actionEnabled,
                                onAction = line.onAction
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))
                ChefsVault(
                    onDocumentaries = onDocumentaries,
                    onEncyclopedia = onEncyclopedia,
                    onAvatarMarket = onAvatarMarket
                )

                Spacer(Modifier.height(20.dp))
                // The wallet keeps the three newest receipts and nothing more;
                // the full ledger, with its filters, is its own screen.
                SectionHeader(
                    "Recent History",
                    if (ui.history.size > 3) "See all" else null
                ) { onFullHistory() }
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.White,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (ui.history.isEmpty()) {
                        Column(
                            Modifier.padding(22.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Icon(
                                Icons.Default.History,
                                contentDescription = null,
                                tint = HintGray,
                                modifier = Modifier.size(30.dp)
                            )
                            Spacer(Modifier.height(8.dp))
                            Text(
                                "No receipts yet",
                                fontWeight = FontWeight.Bold,
                                color = RewardsTextDark,
                                style = MaterialTheme.typography.titleSmall
                            )
                            Text(
                                "Earn or spend KK and it shows up here.",
                                color = HintGray,
                                fontSize = 11.sp,
                                textAlign = TextAlign.Center
                            )
                        }
                    } else {
                        Column(Modifier.padding(horizontal = 14.dp, vertical = 8.dp)) {
                            historyPreview.forEach { row ->
                                HistoryRow(row)
                            }
                        }
                    }
                }
                Spacer(Modifier.height(12.dp))
            }
        }
    }
}

private fun buildEarnLines(
    ui: RewardsUiState,
    streak: Int,
    solvedLevels: Set<Int>,
    viewModel: RewardsViewModel
): List<EarnLine> {
    val lines = mutableListOf<EarnLine>()
    val streakTag = if (streak > 0) "STREAK: $streak DAYS" else "ONCE A DAY"
    lines += EarnLine(
        art = R.drawable.earn_daily,
        title = "Daily Login",
        tag = streakTag,
        reward = "+${ui.dailyAmount} KK",
        action = when {
            ui.dailyClaimed -> "CLAIMED"
            ui.dailyClaimable -> "CLAIM"
            else -> null
        },
        actionEnabled = ui.dailyClaimable && !ui.claimBusy,
        onAction = { viewModel.claimDaily() }
    )
    lines += EarnLine(
        art = R.drawable.earn_sell_ingredients,
        title = "Sell Ingredients",
        tag = "TRADE JARS FOR KK",
        reward = "+KK"
    )
    regionEarnRows(ui.islands, solvedLevels).forEach { island ->
        lines += EarnLine(
            art = earnArtForIsland(island.name),
            title = "Complete ${island.name}",
            tag = "ADVENTURE MODE · ${island.solved}/${island.total}",
            reward = "+${island.amount_kk} KK",
            action = when {
                island.claimed -> "CLAIMED"
                island.claimable -> "CLAIM"
                else -> null
            },
            actionEnabled = island.claimable && !ui.claimBusy,
            onAction = { viewModel.claimIsland(island.id) }
        )
    }
    return lines
}

/** Counts from DishCatalog so Adobo (and other nationwide dishes) sit in Philippines, not Visayas. */
internal fun mergeIslandEarnRows(
    islands: List<EarnIslandData>,
    solvedLevels: Set<Int>
): List<EarnIslandData> =
    Region.entries.map { region ->
        val levelIds = (1..LevelProvider.levelCount).filter {
            LevelProvider.forLevel(it).region == region
        }
        val fromApi = islands.firstOrNull { row ->
            row.id.equals(region.name, ignoreCase = true) ||
                row.name.equals(region.displayName, ignoreCase = true)
        }
        val complete = levelIds.isNotEmpty() && solvedLevels.containsAll(levelIds)
        val claimed = fromApi?.claimed == true
        EarnIslandData(
            id = fromApi?.id ?: region.name.lowercase(),
            name = region.displayName,
            solved = levelIds.count { it in solvedLevels },
            total = levelIds.size,
            claimed = claimed,
            claimable = !claimed && (fromApi?.claimable == true || complete),
            amount_kk = fromApi?.amount_kk ?: 15
        )
    }

private fun regionEarnRows(
    islands: List<EarnIslandData>,
    solvedLevels: Set<Int>
): List<EarnIslandData> = mergeIslandEarnRows(islands, solvedLevels)

@Composable
internal fun HeaderCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(ChromeCream)
            .border(1.dp, ChromeStroke, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(icon, contentDescription = contentDescription, tint = ChromeInk, modifier = Modifier.size(18.dp))
    }
}

@DrawableRes
private fun earnArtForIsland(name: String): Int = when (name) {
    Region.LUZON.displayName -> R.drawable.earn_luzon
    Region.VISAYAS.displayName -> R.drawable.earn_visayas
    Region.MINDANAO.displayName -> R.drawable.earn_mindanao
    else -> R.drawable.earn_philippines
}

private data class EarnLine(
    @DrawableRes val art: Int,
    val title: String,
    val tag: String,
    val reward: String,
    val action: String? = null,
    val actionEnabled: Boolean = false,
    val onAction: (() -> Unit)? = null
)

@Composable
private fun EarnRow(
    @DrawableRes art: Int,
    title: String,
    tag: String,
    reward: String,
    action: String? = null,
    actionEnabled: Boolean = false,
    onAction: (() -> Unit)? = null
) {
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Image(
            painter = painterResource(art),
            contentDescription = null,
            contentScale = ContentScale.Fit,
            modifier = Modifier.size(48.dp)
        )
        Spacer(Modifier.width(12.dp))
        Column(Modifier.weight(1f)) {
            Text(title, fontWeight = FontWeight.SemiBold, color = RewardsTextDark, fontSize = 13.sp)
            Text(tag, color = HintGray, fontSize = 9.sp, letterSpacing = 0.6.sp, fontWeight = FontWeight.Bold)
        }
        if (action != null && onAction != null) {
            Surface(
                shape = RoundedCornerShape(10.dp),
                color = if (actionEnabled) RewardsBurntOrange else HintGray.copy(alpha = 0.18f),
                modifier = Modifier.clickable(enabled = actionEnabled, onClick = onAction)
            ) {
                Text(
                    action,
                    color = if (actionEnabled) Color.White else HintGray,
                    fontSize = 10.sp,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 5.dp)
                )
            }
            Spacer(Modifier.width(8.dp))
        }
        Text(reward, color = RewardsBurntOrange, fontWeight = FontWeight.Bold, fontSize = 13.sp)
    }
}

@Composable
private fun ChefsVault(
    onDocumentaries: () -> Unit,
    onEncyclopedia: () -> Unit,
    onAvatarMarket: () -> Unit
) {
    val stall = painterResource(R.drawable.vault_stall)
    val stallAspect = run {
        val size = stall.intrinsicSize
        if (size.width > 0f && size.height > 0f) size.width / size.height else 512f / 660f
    }
    BoxWithConstraints(
        Modifier
            .fillMaxWidth()
            .aspectRatio(stallAspect)
    ) {
        Image(
            painter = stall,
            contentDescription = "Chef's Vault",
            contentScale = ContentScale.Fit,
            alignment = Alignment.TopCenter,
            modifier = Modifier.fillMaxSize()
        )
        Column(
            Modifier
                .fillMaxSize()
                .padding(
                    start = maxWidth * 0.145f,
                    end = maxWidth * 0.145f,
                    top = maxHeight * 0.245f,
                    bottom = maxHeight * 0.105f
                ),
            verticalArrangement = Arrangement.Center,
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            VaultMenuCard(
                art = R.drawable.vault_documentary,
                title = "Food Documentary",
                subtitle = "Heritage films",
                cta = "Watch",
                ctaTone = VaultCtaTone.Gold,
                onClick = onDocumentaries
            )
            Spacer(Modifier.height(12.dp))
            VaultMenuCard(
                art = R.drawable.vault_encyclopedia,
                title = "Kodex Pantry",
                subtitle = "Ingredients of the islands",
                cta = "Trade",
                ctaTone = VaultCtaTone.Maple,
                onClick = onEncyclopedia
            )
            Spacer(Modifier.height(12.dp))
            VaultMenuCard(
                art = R.drawable.vault_avatar,
                title = "Avatar Market",
                subtitle = "Chef's Atelier",
                cta = "Shop",
                ctaTone = VaultCtaTone.Copper,
                onClick = onAvatarMarket
            )
        }
    }
}

private val VaultCardTop = Color(0xFFF6E6C4)
private val VaultCardBot = Color(0xFFE8D09A)
private val VaultTitleInk = Color(0xFF4A2812)

private enum class VaultCtaTone { Gold, Maple, Copper }

@Composable
private fun VaultMenuCard(
    @DrawableRes art: Int,
    title: String,
    subtitle: String,
    cta: String,
    ctaTone: VaultCtaTone,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    val ctx = LocalContext.current
    val shape = RoundedCornerShape(20.dp)
    Surface(
        shape = shape,
        color = VaultCardTop,
        shadowElevation = 0.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(66.dp)
            .shadow(
                elevation = 10.dp,
                shape = shape,
                clip = false,
                ambientColor = Color(0xFF2A1408),
                spotColor = Color(0xFF1A0C04)
            )
            .clickable {
                SoundFx.tap(ctx)
                onClick()
            }
    ) {
        Row(
            Modifier
                .fillMaxSize()
                .background(Brush.verticalGradient(listOf(VaultCardTop, VaultCardBot)))
                .border(1.dp, Color(0x73FFF3DA), shape)
                .padding(start = 8.dp, end = 8.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // The pixel badges carry their own gold frame, so they sit straight
            // on the card — a chip behind them would only frame them twice.
            Image(
                painter = painterResource(art),
                contentDescription = null,
                contentScale = ContentScale.Fit,
                modifier = Modifier.size(50.dp)
            )
            Spacer(Modifier.width(10.dp))
            Column(Modifier.weight(1f)) {
                Text(
                    title,
                    color = VaultTitleInk,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 13.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Spacer(Modifier.height(1.dp))
                Text(
                    subtitle,
                    color = VaultTitleInk.copy(alpha = 0.6f),
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.Medium,
                    fontSize = 10.sp,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
            }
            Spacer(Modifier.width(8.dp))
            VaultActionButton(label = cta, tone = ctaTone)
        }
    }
}

@Composable
private fun VaultActionButton(
    label: String,
    tone: VaultCtaTone
) {
    val (fill, ink) = when (tone) {
        VaultCtaTone.Gold -> Color(0xFFC9A227) to Color.White
        VaultCtaTone.Maple -> Color(0xFFCC6B1F) to Color.White
        VaultCtaTone.Copper -> Color(0xFF8B3A12) to Color.White
    }
    Box(
        modifier = Modifier
            .width(64.dp)
            .height(26.dp)
            .clip(RoundedCornerShape(8.dp))
            .background(fill),
        contentAlignment = Alignment.Center
    ) {
        Text(
            label,
            color = ink,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.Bold,
            fontSize = 11.sp,
            maxLines = 1
        )
    }
}

@Composable
internal fun HistoryRow(row: RewardHistoryItem) {
    val spend = row.event_type == "reward_redemption"
    val amount = row.amount_kk ?: 0
    val won = palayoksWonFromSpinTitle(row.title)
    val signed = when {
        row.event_type == "palayok_spin" && won != null -> "+$won palayoks"
        row.event_type == "account_signup" || row.event_type == "account_login" -> "Secured"
        spend -> "−$amount KK"
        else -> "+$amount KK"
    }
    Row(
        Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(Modifier.weight(1f)) {
            Text(
                historyLabel(row.event_type, row.title),
                fontWeight = FontWeight.SemiBold,
                color = RewardsTextDark,
                fontSize = 13.sp
            )
            Text(
                formatHistoryWhen(row.created_at),
                color = HintGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 0.4.sp
            )
        }
        Surface(
            shape = RoundedCornerShape(20.dp),
            color = if (spend) RewardsDebitRed.copy(alpha = 0.14f) else RewardsCreditGreen.copy(alpha = 0.14f)
        ) {
            Text(
                signed,
                color = if (spend) RewardsDebitRed else RewardsCreditGreen,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 10.dp, vertical = 5.dp)
            )
        }
    }
}

/** Same pill as the system toast, but the KK coin instead of the chef app icon. */
@Composable
private fun ClaimToast(message: String?, onDismiss: () -> Unit) {
    LaunchedEffect(message) {
        if (message.isNullOrBlank()) return@LaunchedEffect
        delay(2_200)
        onDismiss()
    }
    if (message.isNullOrBlank()) return

    Popup(
        alignment = Alignment.BottomCenter,
        properties = PopupProperties(focusable = false, clippingEnabled = false)
    ) {
        Surface(
            shape = RoundedCornerShape(50),
            color = Color(0xFFE9E9ED),
            shadowElevation = 4.dp,
            modifier = Modifier
                .navigationBarsPadding()
                .padding(start = 24.dp, end = 24.dp, bottom = 80.dp)
        ) {
            Row(
                Modifier.padding(horizontal = 16.dp, vertical = 10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                val claimAmount = Regex("""Claimed \+?(\d+) KK""").find(message)?.groupValues?.get(1)
                Text(
                    if (claimAmount != null) "Claimed $claimAmount" else message,
                    color = Color(0xFF1C1B1F),
                    fontFamily = FontFamily.SansSerif,
                    fontSize = 14.sp
                )
                if (claimAmount != null) {
                    Image(
                        painter = painterResource(R.drawable.ic_kk_pixel),
                        contentDescription = "KK",
                        contentScale = ContentScale.Fit,
                        modifier = Modifier
                            .padding(start = 6.dp)
                            .size(22.dp)
                    )
                }
            }
        }
    }
}

internal fun historyLabel(eventType: String?, title: String?): String = when (eventType) {
    "round_win" -> "Puzzle Master"
    "badge_milestone" -> title?.takeIf { it.isNotBlank() } ?: "Badge claim"
    "daily_login" -> "Daily Login Claim"
    "island_complete" -> "Island complete"
    "reward_redemption" -> rewardTitleOrNull(title) ?: "KK spend"
    "palayok_spin" -> "Daily Palayok Spin"
    "account_signup" -> "Account Sign Up"
    "account_login" -> "Account Login"
    else -> prettifyRewardKey(eventType) ?: "Reward"
}

internal fun formatHistoryWhen(raw: String?): String {
    if (raw.isNullOrBlank()) return ""
    val local = parseHistoryLocal(raw) ?: return raw.take(10)
    val date = local.toLocalDate()
    val today = LocalDate.now()
    return when (date) {
        today -> "TODAY, ${local.format(DateTimeFormatter.ofPattern("HH:mm"))}"
        today.minusDays(1) -> "YESTERDAY"
        else -> date.format(DateTimeFormatter.ofPattern("MMM d, yyyy", Locale.US))
            .uppercase(Locale.US)
    }
}

/** Server datetimes are UTC (or have a zone). Compare them in the phone's local day. */
internal fun parseHistoryLocal(raw: String): LocalDateTime? {
    val iso = raw.trim().replace(' ', 'T')
    val zoned = when {
        iso.endsWith("Z", ignoreCase = true) -> iso
        iso.contains(Regex("[+-]\\d{2}:\\d{2}$")) -> iso
        iso.contains(Regex("[+-]\\d{4}$")) -> iso
        else -> {
            val core = iso.take(19)
            if (core.length >= 19) "${core}Z" else "${iso}Z"
        }
    }
    runCatching {
        return Instant.parse(zoned).atZone(ZoneId.systemDefault()).toLocalDateTime()
    }
    return runCatching {
        LocalDateTime.parse(iso.take(19), DateTimeFormatter.ofPattern("yyyy-MM-dd'T'HH:mm:ss"))
            .atOffset(ZoneOffset.UTC)
            .atZoneSameInstant(ZoneId.systemDefault())
            .toLocalDateTime()
    }.getOrNull()
}
