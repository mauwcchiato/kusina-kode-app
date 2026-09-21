package com.example.kusinakode.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.widthIn
import androidx.compose.runtime.Composable
import androidx.compose.runtime.ReadOnlyComposable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalConfiguration
import androidx.compose.ui.unit.Density
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

/**
 * How much width the app has to work with.
 *
 * dp already handles pixel density — a 1080p and a 1440p phone of the same
 * size get identical layouts for free. What dp does not handle is a device
 * that is genuinely *bigger*: on an 800dp tablet a phone layout does not
 * break, it just strands everything in a field of empty space and stretches
 * paragraphs to a width nobody can read comfortably.
 *
 * Breakpoints follow Material 3's window size classes so they match what
 * the platform and every other Android app already assume.
 */
enum class WindowWidth {
    /** Phones in portrait, and anything narrow. Under 600dp. */
    Compact,

    /** Large phones in landscape, small tablets. 600dp to 840dp. */
    Medium,

    /** Tablets, and a phone plugged into a display. 840dp and up. */
    Expanded;

    val isPhone: Boolean get() = this == Compact
    val isLarge: Boolean get() = this != Compact
}

/**
 * The width every screen in this app was laid out against.
 *
 * Android's Medium Phone — the emulator the design was signed off on. Sizes
 * throughout the app are dp figures chosen to look right at this width.
 */
const val DesignWidthDp = 411f

/**
 * Renders the whole app as if every phone were [DesignWidthDp] wide.
 *
 * dp is an absolute unit, so the same layout on a narrower phone does not
 * shrink — it simply shows less. At 320dp the Home hero eats the screen and
 * "Continue Learning" falls off the bottom entirely, while the same build on
 * a 411dp phone shows it. Scaling the density instead means the design is
 * reproduced at every phone size: identical composition, just smaller or
 * larger, and nothing drops off.
 *
 * Only phones. A tablet scaled this way would be a 2x phone, and tablets are
 * already handled by capping content width, which is the better answer there.
 *
 * fontScale is passed through untouched, so someone who has set a larger
 * system font still gets it — this multiplies the design, not the person's
 * accessibility preference.
 */
@Composable
fun DesignScaled(content: @Composable () -> Unit) {
    val config = LocalConfiguration.current
    val density = LocalDensity.current
    val scale = if (config.smallestScreenWidthDp >= 600) {
        1f
    } else {
        // Clamped: past these bounds legibility suffers more than the exact
        // match is worth, and an unbounded scale would shrink text on a very
        // narrow phone until it could not be read.
        (config.screenWidthDp / DesignWidthDp).coerceIn(0.80f, 1.15f)
    }
    CompositionLocalProvider(
        LocalDensity provides Density(
            density = density.density * scale,
            fontScale = density.fontScale
        )
    ) { content() }
}

@Composable
@ReadOnlyComposable
fun rememberWindowWidth(): WindowWidth {
    val w = LocalConfiguration.current.screenWidthDp
    return when {
        w < 600 -> WindowWidth.Compact
        w < 840 -> WindowWidth.Medium
        else -> WindowWidth.Expanded
    }
}

/**
 * The widest a column of text or cards should ever get.
 *
 * Past roughly 600dp a line of body text becomes hard to track back to the
 * start of the next one, which is why this caps rather than filling. On a
 * phone the cap is above the screen width and does nothing at all, so this
 * is safe to apply anywhere — it only takes effect where there is surplus.
 */
val ReadableWidth: Dp = 600.dp

/**
 * Fills a phone, and on anything wider centres a readable column.
 *
 * Order matters and is easy to get backwards: `fillMaxWidth()` pins both the
 * minimum *and* the maximum to the parent's width, so a `widthIn(max=)` after
 * it can never shrink anything — the minimum wins and the cap silently does
 * nothing. Capping first, then filling up to that cap, is what actually
 * bounds the content.
 */
fun Modifier.readableWidth(max: Dp = ReadableWidth): Modifier =
    this.widthIn(max = max).fillMaxWidth()

/**
 * The widest the whole app shell is allowed to get.
 *
 * Looser than [ReadableWidth] because this bounds entire screens, headers
 * and artwork included, not a column of prose. On any phone — portrait or
 * landscape — it is wider than the screen and changes nothing. On a tablet
 * it stops a layout designed for 400dp from being smeared across 1280dp,
 * which is the difference between "made for this" and "phone app, enlarged".
 */
val AppShellWidth: Dp = 760.dp

/**
 * True for a real tablet, false for any phone in any orientation.
 *
 * Deliberately not current width: a large phone on its side is close to
 * 900dp, so a width test would letterbox it with margins the moment it
 * rotated. smallestScreenWidthDp describes the *device* — it does not
 * change when the phone turns — which is the same signal behind the
 * platform's own `sw600dp` resource bucket.
 */
@Composable
@ReadOnlyComposable
fun isTabletDevice(): Boolean = LocalConfiguration.current.smallestScreenWidthDp >= 600

/**
 * Caps the app shell on tablets, centred; phones are left entirely alone.
 *
 * Returns a bare Modifier on a phone rather than a wide [widthIn], so there
 * is nothing to accidentally clip on a device this was never meant for.
 */
@Composable
fun appShellModifier(max: Dp = AppShellWidth): Modifier =
    if (isTabletDevice()) Modifier.widthIn(max = max) else Modifier

/**
 * A centred column that stops growing once it has enough room.
 *
 * Drop-in for a screen's outermost `Column`: identical on a phone, and on a
 * tablet it keeps the content together in the middle instead of letting it
 * span the whole panel.
 */
@Composable
fun ReadableColumn(
    modifier: Modifier = Modifier,
    maxWidth: Dp = ReadableWidth,
    horizontalAlignment: Alignment.Horizontal = Alignment.CenterHorizontally,
    verticalArrangement: Arrangement.Vertical = Arrangement.Top,
    content: @Composable ColumnScope.() -> Unit
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Column(
            modifier = Modifier.readableWidth(maxWidth),
            horizontalAlignment = horizontalAlignment,
            verticalArrangement = verticalArrangement,
            content = content
        )
    }
}

/**
 * Picks a value for the current width.
 *
 * Keeps the three cases next to each other at the call site rather than
 * scattering `if (isTablet)` through a layout.
 */
@Composable
fun <T> byWidth(compact: T, medium: T = compact, expanded: T = medium): T =
    when (rememberWindowWidth()) {
        WindowWidth.Compact -> compact
        WindowWidth.Medium -> medium
        WindowWidth.Expanded -> expanded
    }

/** Scales a dimension up on roomier screens — hero art, mostly. */
@Composable
fun Dp.scaledForWidth(medium: Float = 1.15f, expanded: Float = 1.3f): Dp =
    when (rememberWindowWidth()) {
        WindowWidth.Compact -> this
        WindowWidth.Medium -> this * medium
        WindowWidth.Expanded -> this * expanded
    }
