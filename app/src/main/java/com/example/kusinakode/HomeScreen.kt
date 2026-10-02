package com.example.kusinakode

import com.example.kusinakode.ui.components.readableWidth

import com.example.kusinakode.ui.components.clickSfx
import androidx.annotation.DrawableRes

import androidx.compose.animation.Crossfade
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.animateIntAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Image
import androidx.compose.foundation.horizontalScroll
import androidx.compose.foundation.BorderStroke
import androidx.compose.ui.text.style.TextOverflow
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.ui.home.HomeHeader
import com.example.kusinakode.ui.home.HomeStatsRow
import com.example.kusinakode.ui.home.RegionProgressCard
import com.example.kusinakode.ui.home.SectionHeader
import com.example.kusinakode.ui.shop.ChefLook
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Bolt
import androidx.compose.material.icons.filled.AutoStories
import androidx.compose.material.icons.filled.Explore
import androidx.compose.material.icons.filled.Face
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.MenuBook
import androidx.compose.material.icons.filled.MilitaryTech
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.filled.HourglassBottom
import androidx.compose.material.icons.filled.RestaurantMenu
import androidx.compose.material.icons.filled.School
import androidx.compose.material.icons.filled.Whatshot
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.graphics.vector.ImageVector
import com.example.kusinakode.ui.onboarding.CoachMarkOverlay
import com.example.kusinakode.ui.onboarding.CoachStep
import com.example.kusinakode.ui.onboarding.coachAnchor
import com.example.kusinakode.ui.onboarding.rememberCoachAnchors
import com.example.kusinakode.ui.rewards.NotificationStore
import com.example.kusinakode.ui.rewards.RewardsViewModel
import com.example.kusinakode.ui.rewards.unreadNotificationCount
import com.example.kusinakode.ui.rewards.waitingForYouCount
import com.example.kusinakode.ui.pantry.PantryViewModel
import com.example.kusinakode.data.repository.AttemptOutbox
import com.example.kusinakode.ui.shop.EquippedAvatarPortrait
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.layout.onGloballyPositioned
import androidx.compose.ui.layout.positionInWindow
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.kusinakode.ui.game.ResumeViewModel
import com.example.kusinakode.ui.gamification.GamificationViewModel
import com.example.kusinakode.domain.gamification.ChefRank
import com.example.kusinakode.ui.theme.CardSurface
import com.example.kusinakode.ui.theme.HintGray
import com.example.kusinakode.ui.theme.LightOrange
import com.example.kusinakode.ui.theme.OutlineDefault
import kotlinx.coroutines.delay
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.height

private val OliveBrown = Color(0xFF6F3913)
private val CreamBg = Color(0xFFF1E6D2)
private val TextDark = Color(0xFF3E2723)
/** The Play Now brown. Shared so other primary actions match it. */
internal val PlayNowBrown = Color(0xFF92441D)
private val DailyChipBrown = Color(0xFF7A3C1A)
private val DailyChipStroke = Color(0xFF936243)
private val FrameCream = Color(0xFFF9F5EE)

/**
 * Logged-in dashboard per the mockup: chef header with progress, daily
 * challenge hero, stats row, continue-learning cards, bottom navigation.
 */
@Composable
fun HomeScreen(
    onPlayLevel: (Int) -> Unit,
    onExplore: () -> Unit,
    onExploreRegion: (Region) -> Unit = { onExplore() },
    onLeaderboard: () -> Unit,
    onLearn: () -> Unit,
    onProfile: () -> Unit,
    onHowToPlay: () -> Unit,
    onNotifications: () -> Unit = {},
    onStory: () -> Unit = {},
    onTutorial: () -> Unit = {},
    onKkGuide: () -> Unit = {},
    onRewards: () -> Unit = {},
    onSettings: () -> Unit = {},
    onShopReel: () -> Unit = {},
    onShopPantry: () -> Unit = {},
    onShopAtelier: () -> Unit = {}
) {
    val ctx = LocalContext.current
    val go: (() -> Unit) -> () -> Unit = { action ->
        {
            SoundFx.play(ctx, SoundFx.Cue.Nav)
            action()
        }
    }
    DisposableEffect(Unit) {
        SoundFx.setBgm(ctx, SoundFx.Bgm.Home)
        onDispose { }
    }
    val gamification: GamificationViewModel = viewModel()
    val game by gamification.uiState.collectAsState()
    val totalLevels = LevelProvider.visibleCount
    val completed = game.progress.roundsCompleted
    // The dish the "Play / Continue" card points at. It follows the island the
    // player last opened a round in: after a Visayas level, Home offers the next
    // Visayas level (not back to Luzon). Once that island is fully solved — or
    // before any round is played — it falls back to the first unlocked, unsolved
    // level overall (Luzon→Visayas→Mindanao, each by word length), matching the
    // Game Map's PLAY NOW.
    val lastRegion = LastPlayedStore.region(ctx, Session.userId)
    val nextLevel = remember(game.progress.solvedLevels, lastRegion) {
        lastRegion?.let { LevelProvider.nextPlayableInRegion(it, game.progress.solvedLevels) }
            ?: LevelProvider.nextPlayable(game.progress.solvedLevels)
    }

    val rewards: RewardsViewModel = viewModel()
    val wallet by rewards.uiState.collectAsState()
    val pantry: PantryViewModel = viewModel(factory = PantryViewModel.factory())
    val pantryUi by pantry.uiState.collectAsState()

    // The bell's unread dot. Read/deleted state is on the device, so the count
    // has to be recomputed here rather than read off the wallet payload.
    LaunchedEffect(Unit) { NotificationStore.load(ctx) }
    val readIds by NotificationStore.read.collectAsState()
    val deletedIds by NotificationStore.deleted.collectAsState()
    val unreadNotifications = remember(
        wallet, pantryUi.snapshot.spinsAvailable, pantryUi.snapshot.drawsAvailable, readIds, deletedIds
    ) {
        unreadNotificationCount(
            history = wallet.history,
            readIds = readIds,
            deletedIds = deletedIds,
            claimableCount = waitingForYouCount(
                dailyClaimable = wallet.dailyClaimable,
                spinsAvailable = pantryUi.snapshot.spinsAvailable,
                islandClaimable = wallet.islands.count { it.claimable },
                badgeClaimable = wallet.badges.count { it.claimable },
                palayoksToOpen = pantryUi.snapshot.drawsAvailable
            )
        )
    }

    // A win the server never heard about (the phone was offline) is sent
    // now; the pantry reload after it is where the server pays any palayok
    // that win, or any other, is still owed.
    LaunchedEffect(Unit) {
        AttemptOutbox(ctx).flush()
        pantry.refresh()
    }

    val resume: ResumeViewModel = viewModel()
    val resumableLevels by resume.resumableLevels.collectAsState()
    LaunchedEffect(Unit) { resume.refresh() }
    val canResume = nextLevel in resumableLevels

    val username = Session.displayName ?: "Guest Cook"
    val chefName = Session.nickname?.takeIf { it.isNotBlank() } ?: username

    LaunchedEffect(Session.userId) { ChefLook.hydrate(ctx) }

    // Per-region completion drives the Continue Learning rail.
    val regionProgress = remember(game.progress.solvedLevels) {
        Region.entries.map { region ->
            val levels = LevelProvider.visibleIds.filter { LevelProvider.forLevel(it).region == region }
            Triple(region, levels.count { it in game.progress.solvedLevels }, levels.size)
        }.filter { it.third > 0 }
    }
    val regionsExplored = regionProgress.count { it.second > 0 }

    // Only dishes already solved: leading with the next level's photo would
    // hand the player the answer before they had guessed a letter.
    val heroImages = remember(game.progress.solvedLevels) {
        game.progress.solvedLevels
            .filter { it in 1..LevelProvider.levelCount }
            .sorted()
            .map { LevelProvider.forLevel(it).photo }
    }
    var heroIndex by remember { mutableStateOf(0) }
    LaunchedEffect(heroImages) {
        heroIndex = 0
        if (heroImages.isEmpty()) return@LaunchedEffect
        while (true) {
            delay(4000)
            heroIndex = (heroIndex + 1) % heroImages.size
        }
    }

    val anchors = rememberCoachAnchors()
    var showTour by remember { mutableStateOf(false) }
    val homeScroll = rememberScrollState()
    var tourAnchor by remember { mutableStateOf<String?>(null) }
    var scrollOrigin by remember { mutableIntStateOf(0) }
    var statsTop by remember { mutableIntStateOf(0) }
    var guidesTop by remember { mutableIntStateOf(0) }
    LaunchedEffect(Unit) {
        if (!CoachMarkManager.isDone(ctx, CoachMarkManager.TOUR_HOME)) {
            delay(450)
            showTour = true
            // Seen once it opens, not only when finished: leaving by Back or
            // closing the app used to bring the tour back on every launch.
            CoachMarkManager.markDone(ctx, CoachMarkManager.TOUR_HOME)
        }
    }
    LaunchedEffect(tourAnchor, statsTop, guidesTop) {
        val target = when (tourAnchor) {
            "home_notif" -> 0
            "home_foods", "home_regions", "home_coins", "home_badges" -> statsTop
            "home_story", "home_tutorial", "home_kk" -> guidesTop
            else -> return@LaunchedEffect
        }
        homeScroll.animateScrollTo((target - 16).coerceIn(0, homeScroll.maxValue))
    }

    Box(Modifier.fillMaxSize()) {
      Scaffold(
        containerColor = CreamBg,
        bottomBar = {
            KusinaBottomNav(
                selected = BottomNavTab.Home,
                onHome = { },
                onProfile = onProfile,
                onLevels = onExplore,
                onWallet = onRewards,
                onCompleted = onLearn,
                walletModifier = Modifier.coachAnchor("home_wallet", anchors)
            )
        }
      ) { inner ->
        Box(
            Modifier
                .fillMaxSize()
                .padding(bottom = inner.calculateBottomPadding())
        ) {
            Column(
                Modifier
                    .fillMaxSize()
                    .verticalScroll(homeScroll)
                    .onGloballyPositioned { scrollOrigin = it.positionInWindow().y.toInt() }
            ) {
                // The hero overlaps the header, so the header reserves space at
                // its foot and the card is pinned to the bottom of the same Box.
                Box {
                    HomeHeader(
                        chefName = chefName,
                        rankTitle = ChefRank.forSolved(LevelProvider.visibleSolved(game.progress.solvedLevels), totalLevels).title,
                        balanceKk = wallet.balanceKk,
                        portrait = {
                            EquippedAvatarPortrait(
                                initial = chefName.trim().take(1).uppercase(),
                                size = 54.dp
                            )
                        },
                        onPortrait = onProfile,
                        onNotifications = onNotifications,
                        unreadNotifications = unreadNotifications,
                        onSettings = onSettings,
                        bellModifier = Modifier.coachAnchor("home_notif", anchors),
                        // Deeper than the card's top inset below, so the card
                        // laps into the brown rather than sitting under it.
                        panelDepth = 104.dp
                    )

                    DailyChallengeCard(
                        heroImages = heroImages,
                        heroIndex = heroIndex,
                        canResume = canResume,
                        level = nextLevel,
                        onPlay = go { onPlayLevel(nextLevel) },
                        modifier = Modifier
                            .align(Alignment.TopCenter)
                            // Starts below the header row and laps over the
                            // panel's lower edge, as in the frame.
                            .padding(top = 104.dp, start = 20.dp, end = 20.dp)
                            .coachAnchor("home_play", anchors)
                    )
                }

                // Hero and header keep the full width — they are artwork and
                // want the room. Everything below is a column of rows, so it
                // is capped rather than stretched across a tablet.
                Column(Modifier.align(Alignment.CenterHorizontally).readableWidth()) {

                Spacer(Modifier.height(18.dp))

                Column(Modifier.padding(horizontal = 20.dp)) {
                    HomeStatsRow(
                        foods = completed,
                        regions = regionsExplored,
                        coins = wallet.balanceKk,
                        badges = game.earnedCount,
                        modifier = Modifier
                            .coachAnchor("home_stats", anchors)
                            .onGloballyPositioned {
                                statsTop = (it.positionInWindow().y - scrollOrigin + homeScroll.value).toInt()
                            },
                        foodsModifier = Modifier.coachAnchor("home_foods", anchors),
                        regionsModifier = Modifier.coachAnchor("home_regions", anchors),
                        coinsModifier = Modifier.coachAnchor("home_coins", anchors),
                        badgesModifier = Modifier.coachAnchor("home_badges", anchors)
                    )

                    Spacer(Modifier.height(22.dp))
                    SectionHeader("Continue Learning", "See All") { go(onExplore)() }
                    Spacer(Modifier.height(12.dp))
                }

                Row(
                    Modifier
                        .fillMaxWidth()
                        .horizontalScroll(rememberScrollState())
                        .padding(horizontal = 20.dp),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    regionProgress.forEach { entry ->
                        RegionProgressCard(
                            entry.first,
                            entry.second,
                            entry.third,
                            onClick = go { onExploreRegion(entry.first) }
                        )
                    }
                }

                Spacer(Modifier.height(24.dp))

                // Not in the frame, but the only way back to the story and the
                // practice round once onboarding is behind you.
                Column(
                    Modifier
                        .padding(horizontal = 20.dp)
                        .onGloballyPositioned {
                            guidesTop = (it.positionInWindow().y - scrollOrigin + homeScroll.value).toInt()
                        }
                ) {
                    SectionHeader("Story & Guides")
                    Spacer(Modifier.height(12.dp))
                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        val cell = Modifier.weight(1f)
                        GuideCard(
                            R.drawable.guide_story, "Story", "Why Kusina Kode Exists",
                            cell.coachAnchor("home_story", anchors), go(onStory)
                        )
                        GuideCard(
                            R.drawable.guide_tutorial, "Tutorial", "Practice Round",
                            cell.coachAnchor("home_tutorial", anchors), go(onTutorial)
                        )
                        GuideCard(
                            R.drawable.guide_kk, "KK Guide", "Earn & Spend",
                            cell.coachAnchor("home_kk", anchors), go(onKkGuide)
                        )
                    }
                    Spacer(Modifier.height(24.dp))
                }
                }
            }
        }
      }

      if (showTour) {
          CoachMarkOverlay(
              steps = listOf(
                  CoachStep(
                      anchorKey = "home_notif",
                      round = true,
                      title = "Notifications",
                      body = "The number is how many are waiting. Open them for minted " +
                          if (Features.PLAYER_REPORTS) {
                              "badges and receipts, and report an issue if a reward looks wrong."
                          } else {
                              "badges and receipts."
                          }
                  ),
                  CoachStep(
                      anchorKey = "home_play",
                      title = "Guess the dish",
                      body = "Every round is a Filipino dish name. You get the region " +
                          "it comes from and nothing else - type any real word and the " +
                          "tile colours tell you how close you were."
                  ),
                  CoachStep(
                      anchorKey = "home_foods",
                      title = "Foods",
                      body = "How many dishes you have finished. Each one you cook adds here."
                  ),
                  CoachStep(
                      anchorKey = "home_regions",
                      title = "Regions",
                      body = "How many island groups you have cooked from. Finish every " +
                          "dish in a region and it pays out a bonus."
                  ),
                  CoachStep(
                      anchorKey = "home_coins",
                      title = "Coins",
                      body = "Kusina Kode coins you have earned. Spend them on hints, " +
                          "chef looks, and stories."
                  ),
                  CoachStep(
                      anchorKey = "home_badges",
                      title = "Badges",
                      body = "Each badge you earn is written onto the ledger, not just " +
                          "stored on your phone."
                  ),
                  CoachStep(
                      anchorKey = "home_story",
                      title = "Story",
                      body = "Why Kusina Kode exists, and the kitchen it comes from."
                  ),
                  CoachStep(
                      anchorKey = "home_tutorial",
                      title = "Tutorial",
                      body = "A practice round, so you can learn the tiles before a real dish."
                  ),
                  CoachStep(
                      anchorKey = "home_kk",
                      title = "KK Guide",
                      body = "How to earn coins, and what you can spend them on."
                  ),
                  CoachStep(
                      anchorKey = "home_wallet",
                      title = "Your KK wallet",
                      body = "Coins you earn live here. Spend them on ingredient pages, " +
                          "heritage films and chef looks - or on a hint mid-round."
                  )
              ),
              anchors = anchors,
              onStepChange = { tourAnchor = it.anchorKey },
              onFinish = {
                  CoachMarkManager.markDone(ctx, CoachMarkManager.TOUR_HOME)
                  showTour = false
              }
          )
      }
    }
}

/**
 * The daily challenge card, overlapping the header as in the frame.
 *
 * The backdrop rotates through dishes already solved. Before anything is
 * solved the card stays dark so the next dish is not given away.
 */
@Composable
private fun DailyChallengeCard(
    heroImages: List<Int>,
    heroIndex: Int,
    canResume: Boolean,
    level: Int,
    onPlay: () -> Unit,
    modifier: Modifier = Modifier
) {
    Surface(
        shape = RoundedCornerShape(26.dp),
        color = Color(0xFF20120A),
        shadowElevation = 10.dp,
        modifier = modifier
            .fillMaxWidth()
            .height(460.dp)
    ) {
        Box {
            if (heroImages.isNotEmpty()) {
                Crossfade(
                    targetState = heroIndex.coerceIn(0, heroImages.lastIndex),
                    animationSpec = tween(900),
                    label = "hero_rotate"
                ) { index ->
                    Image(
                        painter = painterResource(heroImages[index]),
                        contentDescription = null,
                        modifier = Modifier.fillMaxSize(),
                        contentScale = ContentScale.Crop
                    )
                }
            }
            Box(
                Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            0f to Color.Black.copy(alpha = 0.55f),
                            0.45f to Color.Black.copy(alpha = 0.25f),
                            1f to Color(0xFF160C05).copy(alpha = 0.92f)
                        )
                    )
            )

            Column(Modifier.fillMaxSize().padding(22.dp)) {
                Surface(
                    shape = RoundedCornerShape(50),
                    color = DailyChipBrown,
                    border = BorderStroke(1.5.dp, DailyChipStroke)
                ) {
                    Row(
                        Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(
                            Icons.Default.LocalFireDepartment,
                            contentDescription = null,
                            tint = FrameCream,
                            modifier = Modifier.size(14.dp)
                        )
                        Spacer(Modifier.width(6.dp))
                        Text(
                            if (canResume) "UNFINISHED ROUND" else "DAILY CHALLENGE",
                            color = FrameCream,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            letterSpacing = 1.2.sp
                        )
                    }
                }

                Spacer(Modifier.weight(1f))

                Text(
                    if (canResume) "Your dish is still simmering"
                    else "Mastering the Art of Filipino Cuisine",
                    color = Color.White,
                    fontSize = 27.sp,
                    lineHeight = 33.sp,
                    fontWeight = FontWeight.Bold
                )
                Spacer(Modifier.height(8.dp))
                Text(
                    if (canResume) {
                        "${LevelProvider.forLevel(level).region.displayName} Level " +
                            "${LevelProvider.regionLevelNumber(level)} is right where you " +
                            "left it. Finish what you started."
                    } else {
                        "${LevelProvider.forLevel(level).region.displayName} Level " +
                            "${LevelProvider.regionLevelNumber(level)} is on the stove. " +
                            "Crack the word, taste the story."
                    },
                    color = Color.White.copy(alpha = 0.78f),
                    fontSize = 15.sp,
                    lineHeight = 21.sp,
                    maxLines = 2,
                    overflow = TextOverflow.Ellipsis
                )

                Spacer(Modifier.height(18.dp))

                Button(
                    onClick = clickSfx(onPlay),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = PlayNowBrown,
                        contentColor = Color.White
                    ),
                    shape = RoundedCornerShape(18.dp),
                    elevation = ButtonDefaults.buttonElevation(
                        defaultElevation = 0.dp,
                        pressedElevation = 0.dp
                    ),
                    contentPadding = PaddingValues(horizontal = 20.dp),
                    modifier = Modifier.fillMaxWidth().height(54.dp)
                ) {
                    Image(
                        painter = painterResource(R.drawable.ic_play_cutout),
                        contentDescription = null,
                        modifier = Modifier.size(22.dp)
                    )
                    Spacer(Modifier.width(10.dp))
                    Text(
                        if (canResume) "Continue" else "Play Now",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp
                    )
                }
            }
        }
    }
}

@Composable
private fun GuideCard(
    @DrawableRes icon: Int,
    title: String,
    subtitle: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFFF3E6D0),
        shadowElevation = 2.dp,
        modifier = modifier.clickable { onClick() }
    ) {
        Column(
            Modifier.padding(bottom = 10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Image(
                painter = painterResource(icon),
                contentDescription = title,
                contentScale = ContentScale.Fit,
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 8.dp)
                    .aspectRatio(1f)
            )
            Text(
                title,
                color = TextDark,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                textAlign = TextAlign.Center,
                maxLines = 1
            )
            Spacer(Modifier.height(2.dp))
            Text(
                subtitle,
                color = HintGray,
                fontSize = 10.sp,
                fontWeight = FontWeight.Medium,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                minLines = 2,
                maxLines = 2
            )
        }
    }
}

@Composable
private fun LearningCard(
    icon: ImageVector,
    title: String,
    subtitle: String,
    progress: Float,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        shape = RoundedCornerShape(18.dp),
        color = CardSurface,
        border = androidx.compose.foundation.BorderStroke(1.dp, OutlineDefault.copy(alpha = 0.6f)),
        shadowElevation = 2.dp,
        modifier = modifier.clickable(onClick = onClick)
    ) {
        Column(Modifier.padding(14.dp)) {
            Box(
                Modifier
                    .size(38.dp)
                    .clip(RoundedCornerShape(12.dp))
                    .background(Color(0xFFCC6B1F).copy(alpha = 0.14f)),
                contentAlignment = Alignment.Center
            ) {
                Icon(icon, contentDescription = null, tint = Color(0xFFCC6B1F), modifier = Modifier.size(20.dp))
            }
            Spacer(Modifier.height(10.dp))
            Text(
                title,
                style = MaterialTheme.typography.titleSmall.copy(
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                ),
                maxLines = 1
            )
            Text(
                subtitle,
                style = MaterialTheme.typography.bodySmall.copy(color = HintGray),
                maxLines = 1
            )
            Spacer(Modifier.height(10.dp))
            var fillStarted by remember { mutableStateOf(false) }
            LaunchedEffect(progress) { fillStarted = true }
            val fill by animateFloatAsState(
                targetValue = if (fillStarted) progress.coerceIn(0f, 1f) else 0f,
                animationSpec = tween(1000),
                label = "card_fill"
            )
            Box(
                Modifier
                    .fillMaxWidth()
                    .height(6.dp)
                    .clip(CircleShape)
                    .background(OutlineDefault.copy(alpha = 0.5f))
            ) {
                Box(
                    Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(fill)
                        .clip(CircleShape)
                        .background(Color(0xFFCC6B1F))
                )
            }
        }
    }
}
