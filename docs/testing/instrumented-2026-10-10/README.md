# Instrumented UI test (I-055, I-048), 10 Oct 2026

Test: `app/src/androidTest/java/com/example/kusinakode/GameNavigationStressTest.kt`
(Jetpack Compose UI test, AndroidJUnitRunner). Device: Android emulator, Android 16,
Kusina Kode 1.27 debug build, signed in as a test player. Run 3 times.

Read-only: letters are typed and erased but ENTER is never pressed, so no attempt,
purchase, spin or claim reaches the server.

| Ref | What it checks | Result (3 runs) |
|---|---|---|
| I-055 | Each bottom-bar tab (Game Map, Learn, Wallet, Profile, Home) opens and shows its own content, 3 times round, bottom bar still present | Pass, 3/3. 45 tab opens, 0 failures. Average 315 to 398 ms per tab, slowest 530 ms (Learn) |
| I-048 | 50 type-and-erase cycles on a live round (500 key presses). After each word the letters must be on the board; after erasing they must be gone | Pass, 3/3. 150 cycles, 0 mismatches. 1.23 to 1.33 s per cycle |

| Tab | Average | Slowest |
|---|---|---|
| Game Map | 321 ms | 448 ms |
| Learn | 398 ms | 530 ms |
| Wallet | 315 ms | 426 ms |
| Profile | 362 ms | 462 ms |
| Home | 367 ms | 427 ms |

Limits: Compose pauses looping animations under test, so this proves the screens
render and the round keeps responding, not frame smoothness; frame timing is in
`../stress-mobile-2026-10-05` (real vivo V17 Pro). Run on an emulator only.

Files: `run1.txt` to `run3.txt` (runner output), `run*-timings.txt` (per-tab and per-cycle times).

How to rerun (device already signed in):
```
./gradlew installDebug installDebugAndroidTest
adb shell am instrument -w -e class com.example.kusinakode.GameNavigationStressTest ph.kusinakode.app.test/androidx.test.runner.AndroidJUnitRunner
```
