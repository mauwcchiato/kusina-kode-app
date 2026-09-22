package com.example.kusinakode

import com.example.kusinakode.ui.components.readableWidth
import com.example.kusinakode.ui.components.KusinaButton
import com.example.kusinakode.ui.components.KusinaButtonTone
import com.example.kusinakode.ui.components.ParchmentCard
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.Canvas
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
import androidx.compose.material.icons.automirrored.filled.Logout
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Extension
import androidx.compose.material.icons.filled.Insights
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Public
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.Schedule
import androidx.compose.material.icons.outlined.Settings
import androidx.compose.material.icons.filled.Terrain
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.WaterDrop
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.draw.drawBehind
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.PathEffect
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.domain.model.BadgeType
import com.example.kusinakode.domain.model.BadgeVerification
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.ui.gamification.BadgeSlot
import com.example.kusinakode.ui.gamification.GamificationViewModel
import com.example.kusinakode.ui.home.ChromeCream
import com.example.kusinakode.ui.home.ChromeInk
import com.example.kusinakode.ui.home.ChromeStroke
import com.example.kusinakode.ui.home.SectionHeader
import com.example.kusinakode.ui.profile.ProfileViewModel
import com.example.kusinakode.ui.shop.ChefLook
import com.example.kusinakode.ui.shop.EquippedAvatarPortrait
import com.example.kusinakode.ui.theme.BeVietnamPro
import com.example.kusinakode.ui.theme.HeaderBottom
import com.example.kusinakode.ui.theme.HeaderTop
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.OutlineDefault
import com.example.kusinakode.ui.theme.SuccessGreen
import androidx.compose.foundation.Image
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.CenterFocusStrong
import androidx.compose.material.icons.filled.Speed
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.res.painterResource
import com.example.kusinakode.ui.gamification.BadgeArt
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.example.kusinakode.ui.gamification.BadgeShowcaseDialog
import com.example.kusinakode.ui.gamification.BadgeSlotView
import com.example.kusinakode.ui.gamification.FeaturedBadges
import com.example.kusinakode.ui.home.regionIcon
import androidx.compose.material.icons.filled.Edit

private val BurntOrange = Color(0xFFB4510E)
private val CreamBg = Color(0xFFF7EFE3)
private val CreamCard = Color(0xFFF3E6D0)
private val CreamButton = Color(0xFFFBF3E4)
private val TextDark = Color(0xFF3E2723)
private val ThemeBrown = Color(0xFF6F3913)
private val TanBadge = Color(0xFFE4C49A)

/**
 * Profile laid out to the mockup: brown header, overlapping stat tiles,
 * badge shelf, wallet card, leaderboard row, and region progress rings.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MyProfileScreen(
    onBack: () -> Unit,
    onHome: () -> Unit = {},
    onLevels: () -> Unit = {},
    onExploreRegion: (Region) -> Unit = { onLevels() },
    onLeadership: () -> Unit = {},
    onCompleted: () -> Unit = {},
    onRewards: () -> Unit = {},
    onSettings: () -> Unit = {},
    onProgress: () -> Unit = {},
    onAvatarShop: () -> Unit = {},
    onLogout: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val viewModel: ProfileViewModel = viewModel(factory = ProfileViewModel.Factory(ctx.applicationContext))
    val ui by viewModel.uiState.collectAsState()
    val stats = ui.stats

    val gamificationVm: GamificationViewModel = viewModel()
    val game by gamificationVm.uiState.collectAsState()

    var askAvatarShop by remember { mutableStateOf(false) }

    val userId = Session.userId
    LaunchedEffect(userId) {
        if (userId != null && userId > 0) {
            viewModel.loadProfile(userId)
            ChefLook.hydrate(ctx)
        }
    }

    val username = Session.displayName ?: stats?.name ?: "Guest Cook"
    val chefName = Session.nickname?.takeIf { it.isNotBlank() } ?: username
    val handle = "@${username.replace(" ", "").lowercase()}"

    val totalLevels = LevelProvider.levelCount
    val completed = game.progress.roundsCompleted
    val solvedLevels = game.progress.solvedLevels
    val nextLevel = remember(solvedLevels) {
        UnlockManager.getUnlockedLevel(ctx, userId)
            .coerceAtLeast((solvedLevels.maxOrNull() ?: 0) + 1)
            .coerceAtMost(totalLevels)
    }
    val regionsExplored = remember(solvedLevels) {
        solvedLevels.map { LevelProvider.forLevel(it).region }.distinct().size
    }
    // Load once per account, then follow the flow so saving the picker
    // repaints the shelf without leaving the screen.
    LaunchedEffect(Session.userId) { FeaturedBadges.load(ctx, Session.userId) }
    val chosenBadgeIds by FeaturedBadges.featured.collectAsState()
    var showBadgePicker by remember { mutableStateOf(false) }

    val featuredBadges = remember(game.badges, chosenBadgeIds) {
        FeaturedBadges.resolve(
            all = game.badges,
            chosenIds = chosenBadgeIds,
            idOf = { it.badgeId },
            isEarned = { it.isEarned }
        )
    }
    val continueRegions = remember(solvedLevels) {
        Region.entries.map { region ->
            val levels = (1..totalLevels).filter { LevelProvider.forLevel(it).region == region }
            Triple(region, levels.count { it in solvedLevels }, levels.size)
        }.filter { it.third > 0 }
            .sortedWith(
                compareByDescending<Triple<Region, Int, Int>> { it.second in 1 until it.third }
                    .thenBy { it.first.ordinal }
            )
    }

    Scaffold(
        containerColor = CreamBg,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Profile,
                onHome = onHome,
                onProfile = { /* already here */ },
                onLevels = onLevels,
                onWallet = onRewards,
                onCompleted = onCompleted
            )
        }
    ) { inner ->
        Column(
            Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
                .verticalScroll(rememberScrollState())
        ) {
            // No full-bleed header on this one, so the whole body is capped.
            Column(Modifier.align(Alignment.CenterHorizontally).readableWidth()) {

            Box {
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(bottomStart = 36.dp, bottomEnd = 36.dp))
                        .background(
                            Brush.verticalGradient(listOf(HeaderTop, HeaderBottom))
                        )
                        .statusBarsPadding()
                        .padding(horizontal = 16.dp)
                        .padding(bottom = 64.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Row(
                        Modifier
                            .fillMaxWidth()
                            .padding(vertical = 10.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        HeaderCircleButton(
                            icon = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            onClick = onBack
                        )
                        Spacer(Modifier.weight(1f))
                        Text(
                            "My Profile",
                            color = Color.White,
                            style = MaterialTheme.typography.titleMedium.copy(fontWeight = FontWeight.Bold)
                        )
                        Spacer(Modifier.weight(1f))
                        HeaderCircleButton(
                            icon = Icons.Outlined.Settings,
                            contentDescription = "Settings",
                            onClick = onSettings
                        )
                    }

                    Spacer(Modifier.height(8.dp))

                    Box(contentAlignment = Alignment.BottomEnd) {
                        Box(
                            Modifier
                                .size(112.dp)
                                .clip(CircleShape)
                                .border(3.dp, Color.White, CircleShape)
                                // The portrait is the look, so it opens the
                                // place you change the look. Renaming still
                                // lives on the pencil row underneath.
                                .clickable {
                                    SoundFx.tap(ctx)
                                    askAvatarShop = true
                                }
                        ) {
                            EquippedAvatarPortrait(
                                initial = chefName.trim().take(1).uppercase(),
                                size = 112.dp
                            )
                        }
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = TanBadge,
                            modifier = Modifier.offset(x = 6.dp, y = 4.dp)
                        ) {
                            Text(
                                "LVL $nextLevel",
                                color = ThemeBrown,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Black,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 3.dp)
                            )
                        }
                    }

                    Spacer(Modifier.height(14.dp))
                    // The name and portrait have always opened the editor, but
                    // nothing said so. The pencil is the affordance.
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        modifier = Modifier.clickable {
                            if (!ui.isSaving) viewModel.startEdit()
                        }
                    ) {
                        Text(
                            "Chef $chefName",
                            color = Color.White,
                            style = MaterialTheme.typography.titleLarge.copy(
                                fontWeight = FontWeight.ExtraBold
                            )
                        )
                        Spacer(Modifier.width(8.dp))
                        Box(
                            Modifier
                                .size(28.dp)
                                .clip(CircleShape)
                                .background(Color.White.copy(alpha = 0.22f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                Icons.Default.Edit,
                                contentDescription = "Edit name and username",
                                tint = Color.White,
                                modifier = Modifier.size(15.dp)
                            )
                        }
                    }
                    Surface(
                        shape = RoundedCornerShape(20.dp),
                        color = Color.White.copy(alpha = 0.18f),
                        modifier = Modifier.padding(top = 6.dp, bottom = 6.dp)
                    ) {
                        Text(
                            handle,
                            color = Color.White,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(horizontal = 14.dp, vertical = 5.dp)
                        )
                    }
                }

                Row(
                    Modifier
                        .align(Alignment.BottomCenter)
                        .padding(horizontal = 16.dp)
                        .offset(y = 26.dp),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    StatTile(Icons.Default.Extension, "$completed", "PUZZLES", Modifier.weight(1f))
                    StatTile(Icons.Default.Public, "$regionsExplored", "REGIONS", Modifier.weight(1f))
                    StatTile(
                        Icons.Default.Bolt,
                        "${game.progress.totalPoints} XP",
                        "POINTS",
                        Modifier.weight(1f)
                    )
                }
            }

            Column(Modifier.padding(start = 16.dp, end = 16.dp, top = 40.dp, bottom = 16.dp)) {
                SectionHeader(
                    "Digital Badges",
                    "Edit"
                ) { showBadgePicker = true }
                Spacer(Modifier.height(10.dp))
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = CreamCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { showBadgePicker = true }
                ) {
                    Row(
                        Modifier.padding(horizontal = 10.dp, vertical = 14.dp),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        featuredBadges.forEach { slot ->
                            BadgeTile(slot, Modifier.weight(1f))
                        }
                        repeat((4 - featuredBadges.size).coerceAtLeast(0)) {
                            Spacer(Modifier.weight(1f))
                        }
                    }
                }

                Spacer(Modifier.height(16.dp))

                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = CreamCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onLeadership() }
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.EmojiEvents,
                            contentDescription = null,
                            tint = BurntOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "Global Leaderboard",
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 14.sp
                            )
                            Text(
                                stats?.currentRank?.let { "Rank #$it" } ?: "Play to get ranked!",
                                color = BurntOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(shape = RoundedCornerShape(18.dp), color = ThemeBrown) {
                            Text(
                                "View All",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(10.dp))

                // My Progress has its own door — Digital Badges Edit opens
                // the trophy case, not this screen.
                Surface(
                    shape = RoundedCornerShape(22.dp),
                    color = CreamCard,
                    modifier = Modifier
                        .fillMaxWidth()
                        .clickable { onProgress() }
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.Insights,
                            contentDescription = null,
                            tint = BurntOrange,
                            modifier = Modifier.size(26.dp)
                        )
                        Spacer(Modifier.width(10.dp))
                        Column(Modifier.weight(1f)) {
                            Text(
                                "My Progress",
                                fontWeight = FontWeight.Bold,
                                color = TextDark,
                                fontSize = 14.sp
                            )
                            Text(
                                "Points, badges and recent rounds",
                                color = BurntOrange,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                        Surface(shape = RoundedCornerShape(18.dp), color = ThemeBrown) {
                            Text(
                                "View All",
                                color = Color.White,
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp)
                            )
                        }
                    }
                }

                Spacer(Modifier.height(20.dp))

                SectionHeader("Continue Learning", "See all", onLevels)
                Spacer(Modifier.height(10.dp))
                if (continueRegions.isEmpty()) {
                    Surface(
                        shape = RoundedCornerShape(16.dp),
                        color = Color.White,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            "No dishes solved yet — your first win shows up here.",
                            color = HintGray,
                            fontSize = 12.sp,
                            modifier = Modifier.padding(16.dp)
                        )
                    }
                } else {
                    continueRegions.forEach { (region, solved, total) ->
                        RegionProgressRow(
                            region = region,
                            solved = solved,
                            total = total,
                            onClick = { onExploreRegion(region) },
                            modifier = Modifier.padding(bottom = 8.dp)
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Same flat pill as the View All chips, left unfilled so the
                // last thing on the page does not read louder than the cards.
                Surface(
                    shape = RoundedCornerShape(18.dp),
                    color = Color.Transparent,
                    border = BorderStroke(1.5.dp, ThemeBrown),
                    modifier = Modifier
                        .align(Alignment.CenterHorizontally)
                        .clickable { onLogout() }
                ) {
                    Row(
                        Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.AutoMirrored.Filled.Logout,
                            contentDescription = null,
                            tint = ThemeBrown,
                            modifier = Modifier.size(15.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            "Sign Out",
                            color = ThemeBrown,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                Spacer(Modifier.height(16.dp))
            }
        
            }
}
    }

    if (showBadgePicker) {
        BadgeShowcaseDialog(
            badges = game.badges.map {
                BadgeSlotView(
                    badgeId = it.badgeId,
                    title = it.title,
                    criteria = it.criteria,
                    isEarned = it.isEarned
                )
            },
            onDismiss = { showBadgePicker = false }
        )
    }

    if (askAvatarShop) {
        ChangeLookDialog(
            chefName = chefName,
            onDismiss = { askAvatarShop = false },
            onConfirm = {
                askAvatarShop = false
                SoundFx.play(ctx, SoundFx.Cue.Nav)
                onAvatarShop()
            }
        )
    }

    if (ui.isEditing) {
        EditProfileTicketDialog(
            username = ui.editUsername,
            nickname = ui.editNickname,
            error = ui.editError,
            saving = ui.isSaving,
            canSave = userId != null,
            onUsernameChange = viewModel::onEditUsernameChange,
            onNicknameChange = viewModel::onEditNicknameChange,
            onSave = { userId?.let { viewModel.saveProfile(it) } },
            onDismiss = { if (!ui.isSaving) viewModel.cancelEdit() }
        )
    }
}

private val TicketGold = Color(0xFFD9A227)
private val TicketInk = Color(0xFF3E2723)
private val TicketFill = Color(0xFFF6E7C8)
private val TicketEdge = Color(0xFFE0C48A)
private val TicketSeam = Color(0xFFC9B08A)

@Composable
private fun EditProfileTicketDialog(
    username: String,
    nickname: String,
    error: String?,
    saving: Boolean,
    canSave: Boolean,
    onUsernameChange: (String) -> Unit,
    onNicknameChange: (String) -> Unit,
    onSave: () -> Unit,
    onDismiss: () -> Unit
) {
    val chefPreview = nickname.trim().ifBlank { username.trim() }.ifBlank { "Chef" }
    val handlePreview = "@${username.replace(" ", "").lowercase().ifBlank { "handle" }}"
    val fieldColors = OutlinedTextFieldDefaults.colors(
        focusedBorderColor = Color(0xFFCC6B1F),
        unfocusedBorderColor = TicketEdge,
        focusedTextColor = TicketInk,
        unfocusedTextColor = TicketInk,
        cursorColor = Color(0xFFCC6B1F),
        focusedContainerColor = Color.White,
        unfocusedContainerColor = Color.White,
        disabledContainerColor = Color.White,
        focusedPlaceholderColor = HintGray,
        unfocusedPlaceholderColor = HintGray
    )
    Dialog(onDismissRequest = { if (!saving) onDismiss() }) {
        ParchmentCard {
            Column(Modifier.fillMaxWidth()) {
                Text(
                    "EDIT PROFILE",
                    color = TicketGold,
                    fontFamily = BeVietnamPro,
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 10.sp,
                    letterSpacing = 2.2.sp
                )
                Spacer(Modifier.height(12.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    ProfilePreviewChip("CHEF", chefPreview, Modifier.weight(1f))
                    ProfilePreviewChip("USERNAME", handlePreview, Modifier.weight(1f))
                }
                Spacer(Modifier.height(14.dp))
                Column(
                    Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(16.dp))
                        .drawBehind {
                            val r = 16.dp.toPx()
                            drawRoundRect(
                                brush = Brush.verticalGradient(
                                    listOf(Color(0xFFFFF8EC), TicketFill)
                                ),
                                cornerRadius = CornerRadius(r)
                            )
                            drawRoundRect(
                                color = TicketEdge,
                                cornerRadius = CornerRadius(r),
                                style = Stroke(width = 1.6.dp.toPx())
                            )
                            val inset = 7.dp.toPx()
                            drawRoundRect(
                                color = TicketSeam,
                                topLeft = Offset(inset, inset),
                                size = Size(size.width - inset * 2f, size.height - inset * 2f),
                                cornerRadius = CornerRadius(r - inset),
                                style = Stroke(
                                    width = 1.6.dp.toPx(),
                                    pathEffect = PathEffect.dashPathEffect(
                                        floatArrayOf(7.dp.toPx(), 4.dp.toPx())
                                    )
                                )
                            )
                        }
                        .padding(14.dp)
                ) {
                    if (error != null) {
                        Text(
                            error,
                            color = Color(0xFFB00020),
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 12.sp
                        )
                        Spacer(Modifier.height(10.dp))
                    }
                    Text(
                        "USERNAME",
                        color = TicketInk.copy(alpha = 0.55f),
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = username,
                        onValueChange = onUsernameChange,
                        placeholder = {
                            Text("Your @handle", fontFamily = BeVietnamPro, fontSize = 14.sp)
                        },
                        singleLine = true,
                        enabled = !saving,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(
                        "NICKNAME  ·  OPTIONAL",
                        color = TicketInk.copy(alpha = 0.55f),
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 10.sp,
                        letterSpacing = 1.4.sp
                    )
                    Spacer(Modifier.height(6.dp))
                    OutlinedTextField(
                        value = nickname,
                        onValueChange = onNicknameChange,
                        placeholder = {
                            Text("Shown as Chef name", fontFamily = BeVietnamPro, fontSize = 14.sp)
                        },
                        singleLine = true,
                        enabled = !saving,
                        shape = RoundedCornerShape(14.dp),
                        colors = fieldColors,
                        modifier = Modifier.fillMaxWidth(),
                        textStyle = LocalTextStyle.current.copy(
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.SemiBold,
                            fontSize = 15.sp
                        )
                    )
                }
                Spacer(Modifier.height(16.dp))
                Row(
                    Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    KusinaButton(
                        label = "Cancel",
                        onClick = onDismiss,
                        tone = KusinaButtonTone.Parchment,
                        enabled = !saving,
                        height = 50.dp,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                    KusinaButton(
                        label = if (saving) "Saving…" else "Save",
                        onClick = onSave,
                        tone = KusinaButtonTone.Terracotta,
                        enabled = canSave && !saving,
                        height = 50.dp,
                        fontSize = 14.sp,
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }
    }
}

@Composable
private fun ProfilePreviewChip(caption: String, value: String, modifier: Modifier = Modifier) {
    Column(
        modifier
            .clip(RoundedCornerShape(14.dp))
            .background(Color(0xFFF6E7C8))
            .border(1.dp, TicketEdge, RoundedCornerShape(14.dp))
            .padding(horizontal = 12.dp, vertical = 8.dp)
    ) {
        Text(
            caption,
            color = TicketGold,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 9.sp,
            letterSpacing = 1.2.sp
        )
        Spacer(Modifier.height(2.dp))
        Text(
            value,
            color = TicketInk,
            fontFamily = BeVietnamPro,
            fontWeight = FontWeight.ExtraBold,
            fontSize = 13.sp,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )
    }
}

/**
 * Tapping the portrait asks before it navigates — the avatar shop is a whole
 * screen away, and a mis-tap on a 112dp circle should not cost the player
 * their place. The portrait itself is the illustration, so the sheet shows it
 * rather than an icon.
 */
@Composable
private fun ChangeLookDialog(
    chefName: String,
    onDismiss: () -> Unit,
    onConfirm: () -> Unit
) {
    Dialog(onDismissRequest = onDismiss) {
        Surface(
            shape = RoundedCornerShape(26.dp),
            color = CreamBg,
            shadowElevation = 16.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                // A band of the app's brown behind the portrait, so the
                // circle reads as a portrait on a wall rather than a cut-out.
                Box(
                    Modifier
                        .fillMaxWidth()
                        .height(74.dp)
                        .background(Brush.verticalGradient(listOf(HeaderTop, HeaderBottom)))
                )
                Box(
                    Modifier
                        .offset(y = (-46).dp)
                        .size(92.dp)
                        .clip(CircleShape)
                        .border(3.dp, CreamBg, CircleShape)
                ) {
                    EquippedAvatarPortrait(
                        initial = chefName.trim().take(1).uppercase(),
                        size = 92.dp
                    )
                }
                Column(
                    Modifier
                        .offset(y = (-34).dp)
                        .padding(horizontal = 24.dp),
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    Text(
                        "Change your look?",
                        color = TextDark,
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 19.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(6.dp))
                    Text(
                        "Would you like to change avatars and frames?",
                        color = TextDark.copy(alpha = 0.72f),
                        fontFamily = BeVietnamPro,
                        fontSize = 13.sp,
                        lineHeight = 18.sp,
                        textAlign = TextAlign.Center
                    )
                    Spacer(Modifier.height(18.dp))
                    Surface(
                        shape = RoundedCornerShape(14.dp),
                        color = BurntOrange,
                        shadowElevation = 3.dp,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clickable(onClick = onConfirm)
                    ) {
                        Text(
                            "Open Avatar Shop",
                            color = Color.White,
                            fontFamily = BeVietnamPro,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 14.sp,
                            textAlign = TextAlign.Center,
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(vertical = 13.dp)
                        )
                    }
                    Spacer(Modifier.height(8.dp))
                    Text(
                        "Not now",
                        color = TextDark.copy(alpha = 0.55f),
                        fontFamily = BeVietnamPro,
                        fontWeight = FontWeight.Bold,
                        fontSize = 13.sp,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .clip(RoundedCornerShape(14.dp))
                            .clickable(onClick = onDismiss)
                            .padding(vertical = 11.dp)
                    )
                    Spacer(Modifier.height(6.dp))
                }
            }
        }
    }
}

@Composable
private fun HeaderCircleButton(
    icon: ImageVector,
    contentDescription: String,
    onClick: () -> Unit
) {
    // Same chrome disc as the Home and KODEX headers: 36dp cream, hairline
    // stroke, 18dp icon. Only the row arrangement differs — the title here
    // stays centred between the two buttons.
    Box(
        Modifier
            .size(36.dp)
            .clip(CircleShape)
            .background(ChromeCream)
            .border(1.dp, ChromeStroke, CircleShape)
            .clickable(onClick = onClick),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            icon,
            contentDescription = contentDescription,
            tint = ChromeInk,
            modifier = Modifier.size(18.dp)
        )
    }
}

@Composable
private fun StatTile(icon: ImageVector, value: String, label: String, modifier: Modifier = Modifier) {
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        shadowElevation = 8.dp,
        modifier = modifier
    ) {
        Column(
            Modifier.padding(vertical = 14.dp, horizontal = 4.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(icon, contentDescription = label, tint = BurntOrange, modifier = Modifier.size(20.dp))
            Spacer(Modifier.height(6.dp))
            Text(
                value,
                color = TextDark,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                maxLines = 1
            )
            Text(
                label,
                color = HintGray,
                fontSize = 9.sp,
                fontWeight = FontWeight.Bold,
                letterSpacing = 1.sp
            )
        }
    }
}

@Composable
private fun BadgeTile(slot: BadgeSlot, modifier: Modifier = Modifier) {
    // Fallback only - a badge with real medal art never reaches this.
    val icon = when (slot.type) {
        BadgeType.ROUNDS -> Icons.Default.RestaurantMenu
        BadgeType.STREAK -> Icons.Default.LocalFireDepartment
        BadgeType.PERFECT -> Icons.Default.Bolt
        BadgeType.ACCURACY -> Icons.Default.CenterFocusStrong
        BadgeType.SPEED -> Icons.Default.Speed
        BadgeType.EXPLORATION -> Icons.Default.Public
        BadgeType.KNOWLEDGE -> Icons.Default.AutoStories
    }
    val medal = BadgeArt.forBadge(slot.badgeId)
    val confirmed = slot.earned?.verification == BadgeVerification.CONFIRMED

    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Box(contentAlignment = Alignment.TopEnd) {
            if (medal != null && slot.isEarned) {
                Image(
                    painter = painterResource(medal),
                    contentDescription = slot.title,
                    contentScale = ContentScale.Fit,
                    modifier = Modifier.size(56.dp)
                )
            } else {
                Icon(
                    icon,
                    contentDescription = slot.title,
                    tint = if (slot.isEarned) BurntOrange else HintGray.copy(alpha = 0.45f),
                    modifier = Modifier.size(28.dp)
                )
            }
            when {
                !slot.isEarned -> Icon(
                    Icons.Default.Lock,
                    contentDescription = "Locked",
                    tint = HintGray,
                    modifier = Modifier
                        .size(14.dp)
                        .offset(x = 3.dp, y = (-3).dp)
                )
                confirmed -> Icon(
                    Icons.Default.Verified,
                    contentDescription = "Blockchain verified",
                    tint = SuccessGreen,
                    modifier = Modifier
                        .size(15.dp)
                        .offset(x = 3.dp, y = (-3).dp)
                )
                else -> Icon(
                    Icons.Default.Schedule,
                    contentDescription = "Verifying on-chain",
                    tint = Color(0xFFE8A93A),
                    modifier = Modifier
                        .size(15.dp)
                        .offset(x = 3.dp, y = (-3).dp)
                )
            }
        }
        if (slot.isEarned) {
            Spacer(Modifier.height(6.dp))
            Text(
                slot.title,
                color = TextDark,
                fontSize = 10.sp,
                fontWeight = FontWeight.Bold,
                lineHeight = 12.sp,
                maxLines = 2,
                textAlign = TextAlign.Center
            )
        }
    }
}

@Composable
private fun RegionProgressRow(
    region: Region,
    solved: Int,
    total: Int,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    val pct = if (total == 0) 0 else ((solved * 100f) / total).toInt()
    Surface(
        shape = RoundedCornerShape(16.dp),
        color = Color.White,
        border = BorderStroke(1.dp, OutlineDefault.copy(alpha = 0.45f)),
        modifier = modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
    ) {
        Row(Modifier.padding(12.dp), verticalAlignment = Alignment.CenterVertically) {
            // The island art carries its own sea, so it fills the tile the
            // same way it does on Home rather than floating in a tinted box.
            Box(
                Modifier
                    .size(44.dp)
                    .clip(RoundedCornerShape(12.dp))
            ) {
                Image(
                    painter = painterResource(regionIcon(region)),
                    contentDescription = region.displayName,
                    contentScale = ContentScale.Crop,
                    modifier = Modifier.fillMaxSize()
                )
            }
            Spacer(Modifier.width(12.dp))
            Column(Modifier.weight(1f)) {
                Text(region.displayName, fontWeight = FontWeight.Bold, color = TextDark, fontSize = 15.sp)
                Text(
                    region.tagline,
                    color = HintGray,
                    fontSize = 11.sp,
                    lineHeight = 14.sp,
                    maxLines = 2
                )
                Text(
                    "$solved/$total Dishes Complete",
                    color = BurntOrange,
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Medium
                )
            }
            PercentRing(percent = pct)
        }
    }
}

@Composable
private fun PercentRing(percent: Int, modifier: Modifier = Modifier) {
    val sweep = (percent.coerceIn(0, 100) / 100f) * 360f
    Box(modifier.size(48.dp), contentAlignment = Alignment.Center) {
        Canvas(Modifier.fillMaxSize()) {
            val stroke = 4.5.dp.toPx()
            drawCircle(
                color = Color(0xFFE8D9C4),
                style = Stroke(width = stroke)
            )
            drawArc(
                color = BurntOrange,
                startAngle = -90f,
                sweepAngle = sweep,
                useCenter = false,
                style = Stroke(width = stroke, cap = StrokeCap.Round)
            )
        }
        Text(
            "$percent%",
            color = TextDark,
            fontSize = 10.sp,
            fontWeight = FontWeight.Bold
        )
    }
}
