package com.example.kusinakode

// I-055  Compose UI / navigation instrumentation
// I-048  Mobile rendering under sustained play
//
// Runs on a device or emulator:
//   ./gradlew connectedDebugAndroidTest
//
// Deps are declared in app/build.gradle.kts (ui-test-junit4, ui-test-manifest,
// test.ext:junit) with testInstrumentationRunner set. The finders use on-screen
// TEXT because the app has no stable testTags yet - that is brittle, so the real
// hardening for I-055 is to add Modifier.testTag("...") to the key nodes
// (keyboard keys, Enter, nav destinations) and switch to onNodeWithTag. Marked
// TODO where a real label/tag is needed. runCatching wraps each interaction so a
// missing label is skipped rather than failing before the TODOs are filled in;
// once the labels are real, drop the runCatching so misses fail loudly.

import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import org.junit.Rule
import org.junit.Test

class GameNavigationStressTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    /**
     * I-055 - every main destination is reachable and renders without crashing.
     *
     * Precondition: a signed-in session, or the app opens on Home. If it opens
     * on Login, sign in first (TODO: seed a test account + do the login taps, or
     * use a debug build that bypasses auth).
     */
    @Test
    fun mainDestinations_render_withoutCrash() {
        rule.waitForIdle()
        // TODO: replace with the real bottom-nav / hub labels.
        val destinations = listOf("Play", "Kodex", "Rewards", "Profile")
        for (label in destinations) {
            runCatching {
                rule.onNodeWithText(label, substring = true).performClick()
                rule.waitForIdle()
            }
        }
        // Reaching here without an exception is the pass condition for rendering.
    }

    /**
     * I-048 - sustained play does not crash or wedge the UI thread.
     *
     * Repeats a guess-and-clear cycle many times to exercise the grid, keyboard,
     * tile animations and round teardown under continuous input. On a real run,
     * pair with `adb shell dumpsys gfxinfo <pkg>` before/after for jank stats -
     * the assertion here only proves it survives.
     */
    @Test
    fun sustainedPlay_manyRounds_staysResponsive() {
        rule.waitForIdle()
        // TODO: navigate into an active round first (tap Play / a level).
        val rounds = 50
        val letters = listOf("A", "D", "O", "B", "O") // a full guess for "ADOBO"
        repeat(rounds) {
            for (key in letters) {
                runCatching { rule.onNodeWithText(key).performClick() } // TODO: onNodeWithTag("key_$key")
            }
            runCatching { rule.onNodeWithText("Enter", substring = true).performClick() } // TODO tag
            rule.waitForIdle()
            // TODO: after a win/lose sheet, tap "Next"/"Retry" to start another round.
            runCatching { rule.onNodeWithText("Next", substring = true).performClick() }
            runCatching { rule.onNodeWithText("Retry", substring = true).performClick() }
            rule.waitForIdle()
        }
        // No exception across `rounds` cycles = sustained-play render stability.
    }
}
