# Kusina Kode – Design System & App Structure

This file is a single reference so we can enhance the app as a whole. All UI screens and the shared theme are listed here.

---

## 1. Shared theme (single source of truth)

**Location:** `app/src/main/java/com/example/kusinakode/ui/theme/`

| File      | Purpose |
|-----------|---------|
| **Color.kt**  | App-wide palette: `DarkBrown`, `GrayBrown`, `LightOrange`, `Cream`, `CardSurface`, `OutlineDefault`, `OutlineFocused`, `SuccessGreen`, `ErrorRed`, `HintGray`, `OverlayLight`, `PastelGray`, `GrayOrange`, `DarkHintBrown`, `DarkGreen`, `LightGreen` |
| **Theme.kt**  | `KusinaKodeTheme` – applies `KusinaLightScheme` and `AppTypography` |
| **Type.kt**   | `AppTypography` – display, headline, title, body, label styles |

**Usage in screens:**  
`import com.example.kusinakode.ui.theme.DarkBrown` (etc.) so every screen uses the same colors and the design stays consistent.

---

## 2. Screens and routes

| Screen / route        | File(s) | Purpose |
|------------------------|---------|---------|
| **Login**              | `LoginScreen.kt` (package `ui.auth`), `LoginModelView.kt` | Auth: login/sign-up, validation, strong password, name rules |
| **Home**               | `HomeScreen.kt` | Landing: logo, “Sugod Kusina”, logout |
| **Instructions**       | `InstructionScreen.kt` | How to play, drawer menu, “Start Levels” |
| **Level select**       | `LevelSelectRoute.kt` (ui.levels), `LevelSelectScreen.kt`, `LevelsViewModel.kt` | Grid of 20 levels, unlocked state, drawer, Completed / Home |
| **Game**               | `GameRoute.kt` (ui.game), `GameScreen.kt`, `GameViewModel.kt` | Wordle-style play, timer, hints, drawer, win/lose dialogs |
| **Profile**            | `MyProfileScreen.kt`, `ProfileViewModel.kt`, `ProfileStatData.kt`, `ProfileUiState.kt` | User stats: rank, fastest time, highest level |
| **Leaderboard**        | `LeaderboardRoute.kt` (ui.leaderboard), `LeadershipScreen.kt`, `LeaderboardViewModel.kt` | Top players list |
| **Completed levels**   | `CompletedLevelsRoute.kt` (ui.completed), `CompletedLevelScreen.kt`, `CompletedLevelsViewModel.kt` | Gallery of completed levels with prev/next |

---

## 3. Navigation

**MainActivity.kt** – `AppNavigator()` with `NavHost` and routes:  
`login` → `home` → `instructions` → `levels` → `game/{level}` | `profile` | `leadership` | `completed`.

---

## 4. Other UI / logic

| File               | Purpose |
|--------------------|---------|
| **Tile.kt**        | Letter tile composable (empty, wrong, semi, correct) |
| **CustomKeyboard.kt** | In-game keyboard |
| **LevelProvider.kt**  | Level data (word, trivia, drawable) |
| **WordleLogic.kt**    | Guess evaluation |
| **Session.kt**       | Current user (userId, displayName, email) |
| **Unlocks.kt**        | (If used) local unlock state |
| **KusinaApi.kt**, **KtorClient.kt** | REST API and HTTP client |

---

## 5. Design consistency checklist

- **Top bars / app bars:** `GrayBrown` background, `LightOrange` text/icons.
- **Primary buttons:** `DarkBrown` background, `LightOrange` text, rounded (e.g. 16–20 dp).
- **Cards / surfaces:** `CardSurface` or `OverlayLight`, rounded corners (e.g. 16–28 dp).
- **Borders / outlines:** `OutlineDefault`, focus `OutlineFocused`.
- **Errors:** `ErrorRed`; success / checkmarks: `SuccessGreen`.
- **Backgrounds:** Full-bleed image + overlay (e.g. `DarkBrown.copy(alpha = 0.4f)` or `LightOrange.copy(alpha = 0.2f)`).

When changing design, update **Color.kt** and **Theme.kt** first, then screens that need it. Use this file to see which screens to touch.
