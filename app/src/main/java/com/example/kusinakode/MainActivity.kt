// File: app/src/main/java/com/example/kusinakode/MainActivity.kt
package com.example.kusinakode

import android.content.Intent
import android.os.Build
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.EnterTransition
import androidx.compose.animation.ExitTransition
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.systemBarsPadding
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.core.view.WindowCompat
import androidx.lifecycle.lifecycleScope
import androidx.navigation.NavController
import androidx.navigation.NavType
import androidx.navigation.compose.NavHost
import androidx.navigation.compose.composable
import androidx.navigation.compose.currentBackStackEntryAsState
import androidx.navigation.compose.rememberNavController
import androidx.navigation.navArgument

import com.example.kusinakode.api.KusinaApi
import com.example.kusinakode.data.levels.EquipmentSync
import com.example.kusinakode.data.levels.LevelSync
import com.example.kusinakode.data.net.ServerConfig
import com.example.kusinakode.data.repository.DefaultUnlockRepository
import com.example.kusinakode.domain.model.Region
import com.example.kusinakode.ui.components.ChainStatusBanner
import com.example.kusinakode.ui.rewards.mergeIslandEarnRows
import com.example.kusinakode.ui.components.RewardReceiptDialog
import com.example.kusinakode.ui.pantry.PalayokSpinPuck
import com.example.kusinakode.ui.pantry.PantrySnapshotBus
import com.example.kusinakode.ui.settings.SettingsScreen
import com.example.kusinakode.ui.auth.ForgotPasswordScreen
import com.example.kusinakode.ui.auth.LoginScreen
import com.example.kusinakode.ui.auth.SignUpScreen
import com.example.kusinakode.ui.auth.WelcomePosterScreen
import com.example.kusinakode.ui.explore.ExploreRoute
import com.example.kusinakode.ui.gamification.ProgressScreen
import com.example.kusinakode.ui.gamification.RoundsHistoryScreen
import com.example.kusinakode.ui.learn.DishDetailScreen
import com.example.kusinakode.ui.learn.KodexHubScreen
import com.example.kusinakode.ui.learn.LearnScreen
import com.example.kusinakode.ui.onboarding.OnboardingScreen
import com.example.kusinakode.ui.story.StoryModeScreen
import com.example.kusinakode.ui.tutorial.KkTutorialScreen
import com.example.kusinakode.ui.tutorial.TutorialGameScreen
import com.example.kusinakode.ui.rewards.NotificationsScreen
import com.example.kusinakode.ui.rewards.RewardsHistoryScreen
import com.example.kusinakode.ui.rewards.RewardsScreen
import com.example.kusinakode.ui.shop.AvatarMarketScreen
import com.example.kusinakode.ui.shop.ChefLook
import com.example.kusinakode.ui.shop.DocumentaryScreen
import com.example.kusinakode.ui.pantry.PantryScreen
import com.example.kusinakode.ui.shop.EncyclopediaScreen
import com.example.kusinakode.ui.game.GameRoute
import com.example.kusinakode.ui.leaderboard.LeaderboardRoute
import com.example.kusinakode.ui.theme.KusinaKodeTheme
import com.example.kusinakode.MyProfileScreen
import com.example.kusinakode.ui.splash.SplashScreen

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        // Host first: LevelSync and every other call read it immediately.
        ServerConfig.load(this)
        SessionStore.load(this)
        KusinaSettings.load(this)
        // Cached wording first, so the first frame already shows what the
        // panel last published rather than flashing the catalogue's text.
        LevelSync.load(this)
        EquipmentSync.applyCached(this)
        lifecycleScope.launch { LevelSync.refresh(this@MainActivity) }
        // Separate launch: equipment art is cosmetic, so a slow or failed
        // fetch here must not hold up the level wording behind it.
        lifecycleScope.launch { EquipmentSync.refresh(this@MainActivity) }
        // Draw behind system bars
        WindowCompat.setDecorFitsSystemWindows(window, false)
        setContent {
            KusinaKodeTheme {
                AppNavigator()
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
    }

    override fun onPause() {
        super.onPause()
        SoundFx.pauseBgm()
    }

    override fun onResume() {
        super.onResume()
        SoundFx.resumeBgm()
    }
}

@Composable
private fun AppNavigator() {
    val nav = rememberNavController()
    val ctx = LocalContext.current

    // Android 13+ needs the player's say-so before a mint can reach the tray.
    // Asked on Home after login so it never covers the onboarding hero.
    val notificationPermission = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { /* granted or not, play continues */ }
    var askedNotify by remember { mutableStateOf(false) }
    val navBackStackEntry by nav.currentBackStackEntryAsState()
    val soundOn by KusinaSettings.prefs.collectAsState()
    LaunchedEffect(navBackStackEntry?.destination?.route, soundOn.soundEffects, soundOn.sfxVolume) {
        val route = navBackStackEntry?.destination?.route
        if (!soundOn.soundEffects || route == "story") {
            SoundFx.stopBgm()
        } else {
            SoundFx.Bgm.forRoute(route)?.let { SoundFx.setBgm(ctx, it) }
        }
    }
    LaunchedEffect(navBackStackEntry?.destination?.route) {
        val onHome = navBackStackEntry?.destination?.route == "home"
        if (onHome && !askedNotify &&
            Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU &&
            !KusinaNotifications.canPost(ctx)
        ) {
            askedNotify = true
            notificationPermission.launch(android.Manifest.permission.POST_NOTIFICATIONS)
        }
    }

    LaunchedEffect(Unit) {
        SessionStore.load(ctx)
        val uid = Session.userId
        if (uid != null && uid > 0) {
            // Failure is fine here (offline start) — local progress still applies.
            DefaultUnlockRepository(ctx).syncFromServer(uid)
            (ctx.applicationContext as KusinaKodeApp).gamificationCoordinator.refresh()
            ChefLook.hydrate(ctx)
            // Nudge an unclaimed daily or island on the way in. Those still
            // need a tap, so they are the ones worth a reminder.
            runCatching {
                val earn = KusinaApi.getEarnStatus().data
                val daily = earn?.daily
                if (daily?.claimable == true) {
                    KusinaNotifications.dailyClaimReady(ctx, daily.amount_kk)
                }
                val solved = (ctx.applicationContext as KusinaKodeApp).gamification
                    .progress(Session.userId)
                    .first()
                    .solvedLevels
                mergeIslandEarnRows(earn?.islands.orEmpty(), solved)
                    .filter { it.claimable }
                    .forEach { island ->
                        KusinaNotifications.islandClaimReady(ctx, island.name, island.amount_kk)
                    }
            }
        }
    }

    // Cold start still shows the feature intro once. Create Account also
    // routes through it so a new cook on a returning device is not dumped
    // straight into story mode.
    // Where the splash hands over once its sequence finishes.
    val afterSplash = if (OnboardingManager.isDone(ctx)) "welcome" else "onboarding/welcome"
    val startDestination = "splash"
    var sawOnboardingThisSession by remember { mutableStateOf(false) }

    val scope = rememberCoroutineScope()

    // Shared post-auth wiring: persist the session, then land wherever the
    // caller asked. A brand-new account gets the origin story; someone signing
    // back in just wants their kitchen.
    val authenticate: (Int, String, String, String) -> Unit = { userId, displayName, email, destination ->
        Session.userId = userId
        Session.displayName = displayName
        Session.email = email
        SessionStore.save(ctx)
        // Pull the account's points, streak and badges down to this device.
        scope.launch {
            DefaultUnlockRepository(ctx).syncFromServer(userId)
            (ctx.applicationContext as KusinaKodeApp).gamificationCoordinator.refresh()
            ChefLook.hydrate(ctx)
        }
        nav.navigate(destination) {
            popUpTo("welcome") { inclusive = true }
        }
    }

    val onLoggedIn: (Int, String, String) -> Unit = { id, name, email ->
        authenticate(id, name, email, "home")
    }
    // First run of a new account: the story plays before the game does.
    val onSignedUp: (Int, String, String) -> Unit = { id, name, email ->
        authenticate(id, name, email, "story")
    }

    // Shared sign-out wiring, so the Profile button and the Settings row end
    // the session the same way: revoke the token server-side, drop everything
    // this device cached for the account, then back out to the welcome screen.
    val signOut: () -> Unit = {
        scope.launch { runCatching { KusinaApi.logout() } }
        SessionStore.clear(ctx)
        PantrySnapshotBus.clear()
        nav.navigate("welcome") { popUpTo("home") { inclusive = true } }
    }

    Box(Modifier.fillMaxSize().systemBarsPadding()) {
        // navigation-compose defaults to a crossfade on every destination
        // change. Switched off: screens cut straight over, the way they did
        // before the library bump.
        NavHost(
            navController = nav,
            startDestination = startDestination,
            enterTransition = { EnterTransition.None },
            exitTransition = { ExitTransition.None },
            popEnterTransition = { EnterTransition.None },
            popExitTransition = { ExitTransition.None }
        ) {
            composable("splash") {
                SplashScreen(
                    onFinished = {
                        nav.navigate(afterSplash) {
                            // The splash must not be reachable by Back.
                            popUpTo("splash") { inclusive = true }
                            launchSingleTop = true
                        }
                    }
                )
            }


            // 0) ONBOARDING — cold start lands on welcome after; Create Account
            // lands on signup. Same screens, different door out.
            composable(
                route = "onboarding/{next}",
                arguments = listOf(navArgument("next") { type = NavType.StringType })
            ) { entry ->
                val next = entry.arguments?.getString("next").let { dest ->
                    if (dest == "signup") "signup" else "welcome"
                }
                OnboardingScreen(
                    doneLabel = if (next == "signup") "Create my account" else "Start Cooking!",
                    onDone = {
                        sawOnboardingThisSession = true
                        OnboardingManager.markDone(ctx)
                        nav.navigate(next) {
                            popUpTo("onboarding/{next}") { inclusive = true }
                        }
                    }
                )
            }

            // 0a) STORY -> TUTORIAL ROUND -> KK PRIMER.
            // One chain, each step skippable: a player who bails at any point
            // still lands on welcome rather than being bounced back a screen.
            // The same three screens serve first launch and a later replay from
            // the help screen, so where they let out depends on whether anyone
            // is signed in rather than on a duplicated set of routes.
            val leaveIntro: () -> Unit = {
                val uid = Session.userId
                val destination = if (uid != null && uid > 0) "home" else "welcome"
                nav.navigate(destination) {
                    popUpTo(destination) { inclusive = true }
                    launchSingleTop = true
                }
            }

            composable("story") {
                // Skip and the last frame share an exit: replay from Home
                // (or How to Play / Settings) pops back there. First sign-up
                // has no home on the stack, so the intro continues into the
                // practice round.
                val leaveStory: () -> Unit = {
                    val from = nav.previousBackStackEntry?.destination?.route
                    if (from == "home" || from == "instructions" || from == "settings") {
                        nav.popBackStack()
                    } else {
                        nav.navigate("tutorial_play")
                    }
                }
                StoryModeScreen(
                    onFinish = leaveStory,
                    onSkip = leaveStory
                )
            }

            composable("tutorial_play") {
                TutorialGameScreen(
                    onFinish = { nav.navigate("tutorial_kk") },
                    onSkip = leaveIntro
                )
            }

            composable("tutorial_kk") {
                KkTutorialScreen(onFinish = leaveIntro)
            }

            // 1) WELCOME — poster duplicate of the wood-board mockup.
            // WelcomeScreen.kt is the previous landing; switch the call
            // below to WelcomeScreen(...) to restore it.
            composable("welcome") {
                WelcomePosterScreen(
                    onLogIn = { nav.navigate("login") },
                    onCreateAccount = {
                        // New cooks: onboarding → signup → story.
                        // Skip only if they just watched it on this cold start.
                        if (sawOnboardingThisSession) nav.navigate("signup")
                        else nav.navigate("onboarding/signup")
                    }
                )
            }

            // 1a) LOG IN
            composable("login") {
                LoginScreen(
                    onLoginSuccess = onLoggedIn,
                    onForgotPassword = { nav.navigate("forgot_password") },
                    onGoToSignUp = {
                        nav.navigate("signup") { popUpTo("welcome") }
                    }
                )
            }

            // 1b) CREATE ACCOUNT
            composable("signup") {
                SignUpScreen(
                    onSignUpSuccess = onSignedUp,
                    onGoToLogin = {
                        nav.navigate("login") { popUpTo("welcome") }
                    }
                )
            }

            composable("forgot_password") {
                ForgotPasswordScreen(onBack = { nav.popBackStack() })
            }

            // 2) HOME
            composable("home") {
                HomeScreen(
                    onPlayLevel   = { lvl -> nav.navigate("game/$lvl") },
                    onExplore     = { nav.openExplore() },
                    onExploreRegion = { nav.openExplore(it) },
                    onLeaderboard = { nav.navigate("leadership") },
                    onLearn       = { nav.navigate("completed") },
                    onProfile     = { nav.navigate("profile") },
                    onHowToPlay   = { nav.navigate("instructions") },
                    onSettings    = { nav.navigate("settings") },
                    onNotifications = { nav.navigate("notifications") },
                    onStory       = { nav.navigate("story") },
                    onTutorial    = { nav.navigate("tutorial_play") },
                    onKkGuide     = { nav.navigate("tutorial_kk") },
                    onRewards     = { nav.navigate("rewards") },
                    onShopReel    = { nav.navigate("shop_docs") },
                    onShopPantry  = { nav.navigate("shop_pantry/all") },
                    onShopAtelier = { nav.navigate("shop_avatar") }
                )
            }

            // 3) HOW TO PLAY
            composable("instructions") {
                InstructionScreen(
                    onBack       = { nav.popBackStack() },
                    onHome       = { nav.navigate("home") },
                    onProfile    = { nav.navigate("profile") },
                    onLeadership = { nav.navigate("leadership") },
                    onWallet = { nav.navigate("rewards") },
                    onCompleted  = { nav.navigate("completed") },
                    onContinue   = { nav.openExplore() },
                    onStoryAndTutorial = { nav.navigate("story") }
                )
            }

            // 4) EXPLORE — clickable region map (replaces the numbered level grid)
            composable(
                route = "levels/{region}",
                arguments = listOf(navArgument("region") { type = NavType.StringType })
            ) { entry ->
                ExploreRoute(
                    onLevelSelected = { lvl -> nav.navigate("game/$lvl") },
                    onViewDish      = { lvl -> nav.navigate("dish/$lvl") },
                    onHome          = { nav.navigate("home") },
                    onCompleted     = { nav.navigate("completed") },
                    onProfile       = { nav.navigate("profile") },
                    onLeadership    = { nav.navigate("leadership") },
                    onWallet        = { nav.navigate("rewards") },
                    onSettings      = { nav.navigate("settings") },
                    initialRegion   = parseExploreRegion(entry.arguments?.getString("region"))
                )
            }

            // 5) GAME
            composable(
                route     = "game/{level}",
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) { backStackEntry ->
                val level = backStackEntry.arguments?.getInt("level") ?: 1

                /**
                 * Leaves the round for [route], dropping the game from the
                 * back stack.
                 *
                 * Every way out means the round is done with — won, lost, or
                 * saved to disk to resume later. Leaving it behind meant Back
                 * landed on a finished board and replayed its win screen.
                 * launchSingleTop so returning to the map reuses the map
                 * entry the player came from instead of stacking a second.
                 */
                val leaveRound: (String) -> Unit = { route ->
                    nav.navigate(route) {
                        popUpTo("game/{level}") { inclusive = true }
                        launchSingleTop = true
                    }
                }

                GameRoute(
                    level         = level,
                    onLevelSelect = {
                        nav.navigate("levels/ALL") {
                            popUpTo("game/{level}") { inclusive = true }
                            launchSingleTop = true
                        }
                    },
                    onGoHome      = { leaveRound("home") },
                    // Replace the finished round instead of stacking game screens.
                    onPlayLevel   = { lvl ->
                        nav.navigate("game/$lvl") {
                            popUpTo("game/{level}") { inclusive = true }
                        }
                    },
                    onViewDish    = { lvl -> leaveRound("dish/$lvl") },
                    onOpenDocumentary = { leaveRound("shop_docs") }
                )
            }

            // 6) PROFILE (Dynamic!)
            composable("profile") {
                MyProfileScreen(
                    onBack = { nav.popBackStack() },
                    onHome = { nav.navigate("home") },
                    onLevels = { nav.openExplore() },
                    onExploreRegion = { nav.openExplore(it) },
                    onLeadership = { nav.navigate("leadership") },
                    onCompleted = { nav.navigate("completed") },
                    onRewards = { nav.navigate("rewards") },
                    onSettings = { nav.navigate("settings") },
                    onProgress = { nav.navigate("progress") },
                    onAvatarShop = { nav.navigate("shop_avatar") },
                    onLogout = signOut
                )
            }

            // 6c) PROGRESS — points, badges and round history (Module 2)
            composable("progress") {
                ProgressScreen(
                    onBack = { nav.popBackStack() },
                    onHome = { nav.navigate("home") },
                    onExplore = { nav.openExplore() },
                    onLeaderboard = { nav.navigate("leadership") },
                    onWallet = { nav.navigate("rewards") },
                    onLearn = { nav.navigate("completed") },
                    onProfile = { nav.navigate("profile") },
                    onAllRounds = { nav.navigate("rounds_history") }
                )
            }

            composable("rounds_history") {
                RoundsHistoryScreen(onBack = { nav.popBackStack() })
            }

            // 6b) REWARDS — KK token wallet (read-only balance from the API)
            composable("rewards") {
                RewardsScreen(
                    onBack = { nav.popBackStack() },
                    onHome = { nav.navigate("home") },
                    onExplore = { nav.openExplore() },
                    onLeaderboard = { nav.navigate("leadership") },
                    onLearn = { nav.navigate("completed") },
                    onProfile = { nav.navigate("profile") },
                    onDocumentaries = { nav.navigate("shop_docs") },
                    onEncyclopedia = { nav.navigate("shop_pantry/all") },
                    onAvatarMarket = { nav.navigate("shop_avatar") },
                    onSettings = { nav.navigate("settings") },
                    onFullHistory = { nav.navigate("rewards_history") }
                )
            }

            composable("rewards_history") {
                RewardsHistoryScreen(onBack = { nav.popBackStack() })
            }

            composable("settings") {
                SettingsScreen(
                    onBack = { nav.popBackStack() },
                    onLogout = signOut,
                    onReplayStory = { nav.navigate("story") }
                )
            }

            composable("notifications") {
                NotificationsScreen(
                    onBack = { nav.popBackStack() },
                    onOpenRewards = { nav.navigate("rewards") },
                    onOpenMarketRun = { nav.navigate("shop_pantry/all") }
                )
            }

            composable("pantry") {
                PantryScreen(onBack = { nav.popBackStack() })
            }
            composable("shop_docs") {
                DocumentaryScreen(onBack = { nav.popBackStack() })
            }
            composable(
                route = "shop_pantry/{tab}",
                arguments = listOf(navArgument("tab") { type = NavType.StringType })
            ) { entry ->
                val tab = entry.arguments?.getString("tab") ?: "all"
                EncyclopediaScreen(
                    onBack = { nav.popBackStack() },
                    onOpenKodex = { nav.navigate("completed") },
                    initialTab = tab
                )
            }
            composable("shop_avatar") {
                AvatarMarketScreen(onBack = { nav.popBackStack() })
            }

            // 7) LEADERBOARD
            composable("leadership") {
                LeaderboardRoute(
                    onBack = { nav.popBackStack() },
                    onHome = { nav.navigate("home") },
                    onProfile = { nav.navigate("profile") },
                    onLevels = { nav.openExplore() },
                    onCompleted = { nav.navigate("completed") },
                    onWallet = { nav.navigate("rewards") }
                )
            }

            // 8) LEARN — KODEX hub, then the dish encyclopedia list
            composable("completed") {
                KodexHubScreen(
                    onOpenDishes = { nav.navigate("kodex_dishes") },
                    onOpenDish = { lvl -> nav.navigate("dish/$lvl") },
                    onOpenFeaturedIngredients = { nav.navigate("shop_pantry/all") },
                    onOpenRareIngredients = { nav.navigate("shop_pantry/all") },
                    onHome = { nav.navigate("home") },
                    onExplore = { nav.openExplore() },
                    onProfile = { nav.navigate("profile") },
                    onWallet = { nav.navigate("rewards") },
                    onSettings = { nav.navigate("settings") }
                )
            }

            composable("kodex_dishes") {
                LearnScreen(
                    onOpenDish = { lvl -> nav.navigate("dish/$lvl") },
                    onBack = { nav.popBackStack() },
                    onHome = { nav.navigate("home") },
                    onExplore = { nav.openExplore() },
                    onLeaderboard = { nav.navigate("leadership") },
                    onWallet = { nav.navigate("rewards") },
                    onProfile = { nav.navigate("profile") },
                    onOpenPantry = { nav.navigate("shop_pantry/all") },
                    onSettings = { nav.navigate("settings") },
                    onKodexHub = {
                        nav.navigate("completed") {
                            popUpTo("completed") { inclusive = false }
                            launchSingleTop = true
                        }
                    }
                )
            }

            // 8b) DISH DETAIL — masterclass page for a solved dish
            composable(
                route = "dish/{level}",
                arguments = listOf(navArgument("level") { type = NavType.IntType })
            ) { backStackEntry ->
                val level = backStackEntry.arguments?.getInt("level") ?: 1
                DishDetailScreen(
                    level = level,
                    onBack = { nav.popBackStack() },
                    // Swap the page in place so paging doesn't pile up history.
                    onOpenDish = { lvl ->
                        nav.navigate("dish/$lvl") {
                            popUpTo("dish/{level}") { inclusive = true }
                        }
                    },
                    onSettings = { nav.navigate("settings") }
                )
            }
        }

        // Panel revision item 3: the reward pipeline narrates itself from the
        // navigation root, so a mint that starts on the game screen is still
        // visible after the player has moved on.
        ChainStatusBanner(Modifier.align(Alignment.TopCenter))
        RewardReceiptDialog()

        // Mounted at the root so the sheet and wheel can be opened from
        // anywhere — the inbox's spin row does exactly that — while the
        // floating button itself only appears on the pantry, where the
        // palayoks it wins are actually kept.
        val puckRoute = nav.currentBackStackEntryAsState().value?.destination?.route
        PalayokSpinPuck(
            showButton = puckRoute in SpinPuckRoutes,
            // The wheel's last line is "they are on your shelf", so
            // collecting opens the shelf. Same destination the inbox's
            // spin row uses. singleTop so collecting while already on the
            // Kodex does not stack a second copy of it.
            onCollected = {
                nav.navigate("shop_pantry/all") { launchSingleTop = true }
            }
        )
    }
}

/** Opens the island map. Pass a region to land on that island; omit it for All. */
private fun NavController.openExplore(region: Region? = null) {
    val key = region?.name ?: "ALL"
    navigate("levels/$key") {
        popUpTo("levels/{region}") { inclusive = true }
        launchSingleTop = true
    }
}

private fun parseExploreRegion(name: String?): Region? {
    if (name.isNullOrBlank() || name.equals("ALL", ignoreCase = true)) return null
    return Region.entries.find { it.name.equals(name, ignoreCase = true) }
}

/**
 * Where the floating spin puck appears.
 *
 * The pantry only. It used to float over the whole app on the theory that a
 * spin should always be one tap away, but a draggable button riding every
 * screen is something to work around rather than reach for. Both pantry routes
 * are listed because both show the Market Run the spin pays into.
 */
private val SpinPuckRoutes = setOf(
    "pantry",
    "shop_pantry/{tab}"
)
