package com.example.kusinakode

// I-055  Compose UI / navigation instrumentation
// I-048  Mobile rendering under sustained play
//
// Runs on a device or emulator that is already signed in (the tests never type
// a password). To keep that sign-in, install and run without the uninstall that
// connectedDebugAndroidTest does at the end:
//   ./gradlew installDebug installDebugAndroidTest
//   adb shell am instrument -w -e class com.example.kusinakode.GameNavigationStressTest \
//       ph.kusinakode.app.test/androidx.test.runner.AndroidJUnitRunner
//
// Read-only: nothing here is sent to the server. Typing and erasing letters stays
// on the phone; only ENTER records an attempt, and ENTER is never pressed. No
// purchase, spin or claim is made.
//
// Compose stops looping animations under test, so these tests prove the screens
// render and the round keeps responding; frame timing (jank) comes from the
// real-phone gfxinfo run in docs/testing/stress-mobile-2026-10-05.

import android.util.Log
import androidx.compose.ui.test.ExperimentalTestApi
import androidx.compose.ui.test.SemanticsNodeInteractionCollection
import androidx.compose.ui.test.hasContentDescription
import androidx.compose.ui.test.hasText
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onAllNodesWithContentDescription
import androidx.compose.ui.test.onAllNodesWithText
import androidx.compose.ui.test.onFirst
import androidx.compose.ui.test.performClick
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalTestApi::class)
class GameNavigationStressTest {

    @get:Rule
    val rule = createAndroidComposeRule<MainActivity>()

    private fun texts(t: String): SemanticsNodeInteractionCollection =
        rule.onAllNodesWithText(t, useUnmergedTree = true)

    private fun descs(d: String): SemanticsNodeInteractionCollection =
        rule.onAllNodesWithContentDescription(d, useUnmergedTree = true)

    private fun count(t: String) = texts(t).fetchSemanticsNodes().size

    /** Waits for the bottom bar; fails clearly if the app opened on Login instead. */
    private fun waitForHome() {
        // Until setContent has run there is no Compose tree to search, which throws.
        rule.waitUntil(30_000) {
            runCatching {
                descs("Home").fetchSemanticsNodes().isNotEmpty() ||
                    texts("LOG IN").fetchSemanticsNodes().isNotEmpty()
            }.getOrDefault(false)
        }
        assertTrue(
            "App opened on Login: sign in on the device once, then rerun.",
            descs("Home").fetchSemanticsNodes().isNotEmpty()
        )
        skipTour()
    }

    /** First-visit coach marks cover the screen; dismiss them if they show. */
    private fun skipTour() {
        rule.waitForIdle()
        if (texts("Skip").fetchSemanticsNodes().isNotEmpty()) {
            texts("Skip").onFirst().performClick()
            rule.waitForIdle()
        }
    }

    private fun tab(label: String) {
        descs(label).onFirst().performClick()
        rule.waitForIdle()
        skipTour()
    }

    /**
     * I-055 - every bottom-bar destination opens and shows its own content,
     * three times round, without a crash.
     */
    @Test
    fun mainDestinations_render_withoutCrash() {
        waitForHome()
        val marks = linkedMapOf(
            "Game Map" to { rule.onAllNodes(hasContentDescription("Luzon") or hasText("Luzon"), useUnmergedTree = true) },
            "Learn" to { texts("See all") },
            "Wallet" to { descs("Wallet") },
            "Profile" to { descs("Profile") },
            "Home" to { rule.onAllNodes(hasText("Continue") or hasText("Play Now"), useUnmergedTree = true) },
        )
        repeat(3) { loop ->
            for ((label, mark) in marks) {
                val t0 = System.nanoTime()
                tab(label)
                rule.waitUntil(15_000) { mark().fetchSemanticsNodes().isNotEmpty() }
                val ms = (System.nanoTime() - t0) / 1_000_000
                Log.i("KKTEST", "I-055 loop ${loop + 1} $label opened in $ms ms")
                // The bottom bar is still there: the screen did not crash or wedge.
                assertTrue("$label: bottom bar missing", descs("Home").fetchSemanticsNodes().isNotEmpty())
            }
        }
    }

    /**
     * I-048 - 50 type-and-erase cycles on a live round. After each word the
     * letters must be on the board, and after erasing they must be gone, so the
     * grid, keyboard and state keep up under continuous input. ENTER is never
     * pressed, so no attempt reaches the server.
     */
    @Test
    fun sustainedPlay_manyRounds_staysResponsive() {
        waitForHome()
        tab("Home")
        rule.onAllNodes(hasText("Continue") or hasText("Play Now"), useUnmergedTree = true)
            .onFirst().performClick()
        rule.waitUntil(20_000) { count("ENTER") > 0 }
        skipTour()

        // The keyboard key is the last node with that letter; tiles come first.
        fun press(key: String) {
            val keys = texts(key)
            keys[keys.fetchSemanticsNodes().size - 1].performClick()
        }
        fun erase(n: Int) = repeat(n) { texts("⌫").onFirst().performClick() }

        // Clear any letters left typed in the current row (only that row erases).
        erase(10)
        rule.waitForIdle()
        // The "LEVEL n" intro swallows taps while it plays; wait for one to land.
        val probe = count("A")
        rule.waitUntil(15_000) {
            press("A"); rule.waitForIdle()
            count("A") > probe
        }
        erase(10)
        rule.waitForIdle()

        val word = listOf("A", "D", "O", "B", "O")
        val distinct = word.distinct()
        val base = distinct.associateWith { count(it) }
        val cycles = 50
        val t0 = System.nanoTime()
        repeat(cycles) { cycle ->
            word.forEach(::press)
            rule.waitForIdle()
            for (l in distinct) {
                val typed = word.count { it == l }
                assertEquals("cycle ${cycle + 1}: '$l' on board", base.getValue(l) + typed, count(l))
            }
            erase(word.size)
            rule.waitForIdle()
            for (l in distinct) {
                assertEquals("cycle ${cycle + 1}: '$l' after erase", base.getValue(l), count(l))
            }
        }
        val ms = (System.nanoTime() - t0) / 1_000_000
        Log.i("KKTEST", "I-048 $cycles cycles (${cycles * word.size * 2} key presses) in $ms ms, ${ms / cycles} ms per cycle")

        // Leave the round the normal way so it is saved, not submitted.
        descs("Back to levels").onFirst().performClick()
        rule.waitForIdle()
        if (count("Leave Round") > 0) texts("Leave Round").onFirst().performClick()
        rule.waitForIdle()
    }
}
