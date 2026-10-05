#!/usr/bin/env bash
# Kusina Kode mobile stress test (emulator only, read-only: no submissions, purchases or spins).
export MSYS_NO_PATHCONV=1
A=/c/Users/jhanm/AppData/Local/Android/Sdk/platform-tools/adb.exe
PKG=ph.kusinakode.app
ACT=ph.kusinakode.app/com.example.kusinakode.MainActivity
OUT="$(dirname "$0")/results"
mkdir -p "$OUT"

dump() { $A shell uiautomator dump /sdcard/u.xml >/dev/null 2>&1; $A shell cat /sdcard/u.xml | tr '>' '\n'; }
tap() {
  local b; b=$(dump | grep -m1 "$1" | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | tr '\n' ' ')
  set -- $b; [ -z "$1" ] && return 1
  $A shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 ))
}
swipe_up()   { $A shell input swipe 540 1800 540 700 250; }
swipe_down() { $A shell input swipe 540 700 540 1800 250; }
skip_tour()  { dump | grep -q 'text="Skip"' && tap 'text="Skip"' && sleep 1; return 0; }
go_home()    { tap 'content-desc="Home"' || { $A shell input keyevent KEYCODE_BACK; sleep 1; tap 'content-desc="Home"'; }; sleep 2; skip_tour; }

# Never act on anything but Kusina Kode: bring it to the front, or stop the test.
ensure_app() {
  local top; top=$($A shell dumpsys activity activities | grep -m1 topResumedActivity)
  case "$top" in *"$PKG/"*) return 0 ;; esac
  $A shell cmd statusbar collapse >/dev/null 2>&1
  $A shell am start -n $ACT >/dev/null 2>&1; sleep 8
  top=$($A shell dumpsys activity activities | grep -m1 topResumedActivity)
  case "$top" in *"$PKG/"*) return 0 ;; esac
  echo "ABORT: Kusina Kode is not in front ($top)" | tee -a "$OUT/abort.txt"; exit 1
}

gfx_reset() { $A shell dumpsys gfxinfo $PKG reset >/dev/null; }
gfx_read()  { $A shell dumpsys gfxinfo $PKG | grep -E 'Total frames rendered|Janky frames:|50th percentile:|90th percentile:|95th percentile:|99th percentile:|Number Slow UI thread|Number Frame deadline missed' | head -8 | tr -d '\r'; }
mem()       { $A shell dumpsys meminfo $PKG | grep -E 'TOTAL PSS|TOTAL:' | head -1 | tr -s ' ' | tr -d '\r'; }

# ---------- screen scenarios ----------
s_home() {
  go_home
  for i in $(seq 1 10); do swipe_up; sleep 0.6; done
  for i in $(seq 1 10); do swipe_down; sleep 0.6; done
}
s_map() {
  tap 'content-desc="Game Map"'; sleep 3; skip_tour
  for isl in Luzon Visayas Mindanao Luzon Visayas Mindanao; do
    tap "content-desc=\"$isl\"" && sleep 3
    skip_tour
    tap 'content-desc="Back to all islands"' || tap 'content-desc="Back to the whole map"'
    sleep 2
  done
}
s_learn() {
  tap 'content-desc="Learn"'; sleep 3; skip_tour
  for i in $(seq 1 6); do swipe_up; sleep 0.6; done
  for i in $(seq 1 6); do swipe_down; sleep 0.6; done
  tap 'text="See all"'; sleep 3
  for i in $(seq 1 8); do swipe_up; sleep 0.5; done
  tap 'text="Adobo"' || tap 'text="Kansi"'; sleep 4; skip_tour
  for i in $(seq 1 8); do swipe_up; sleep 0.6; done
  for i in $(seq 1 8); do swipe_down; sleep 0.6; done
  tap 'content-desc="Back"'; sleep 2
  tap 'content-desc="Back"'; sleep 2
}
s_pantry() {
  tap 'content-desc="Learn"'; sleep 3; skip_tour
  for i in $(seq 1 4); do swipe_up; sleep 0.5; done
  # the second "See all" on the KODEX hub opens the pantry
  b=$(dump | grep 'text="See all"' | sed -n 2p | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | tr '\n' ' ')
  set -- $b; [ -n "$1" ] && $A shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 ))
  sleep 4; skip_tour
  for i in $(seq 1 8); do swipe_up; sleep 0.6; done
  for i in $(seq 1 8); do swipe_down; sleep 0.6; done
  tap 'content-desc="Back"'; sleep 2
}
s_game() {
  go_home
  tap 'text="Continue"' || tap 'text="Play Now"' || return 1
  sleep 6; skip_tour
  local kb; kb=$(dump)
  key() { local b; b=$(echo "$kb" | grep -m1 "text=\"$1\"" | grep -o 'bounds="[^"]*"' | grep -o '[0-9]\+' | tr '\n' ' '); set -- $b; [ -n "$1" ] && $A shell input tap $(( ($1+$3)/2 )) $(( ($2+$4)/2 )); }
  # Type and erase only; ENTER is never pressed, so no guess is sent to the server.
  for round in $(seq 1 6); do
    for c in A D O B O; do key $c; sleep 0.15; done
    for c in 1 2 3 4 5; do key '⌫'; sleep 0.15; done
  done
  $A shell input tap 88 130; sleep 2
  tap 'text="Leave Round"'; sleep 3
}

measure() {
  local name=$1; shift
  ensure_app
  gfx_reset
  local t0=$(date +%s)
  "$@"
  local t1=$(date +%s)
  { echo "== $name ($((t1-t0))s)"; gfx_read; echo "memory: $(mem)"; } | tee -a "$OUT/screens.txt"
}

case "${1:-all}" in
  startup)
    : > "$OUT/startup.txt"
    for i in 1 2 3 4 5; do
      $A shell am force-stop $PKG; sleep 2
      echo "cold $i: $($A shell am start -W -n $ACT | grep TotalTime | tr -d '\r')" | tee -a "$OUT/startup.txt"
      sleep 6
    done
    for i in 1 2 3 4 5; do
      $A shell input keyevent KEYCODE_HOME; sleep 2
      echo "warm $i: $($A shell am start -W -n $ACT | grep TotalTime | tr -d '\r')" | tee -a "$OUT/startup.txt"
      sleep 4
    done
    ;;
  screens)
    : > "$OUT/screens.txt"
    go_home
    measure Home s_home
    measure "Game Map" s_map
    measure "Learn + dish page" s_learn
    measure Pantry s_pantry
    measure "Puzzle board" s_game
    ;;
  sustained)
    : > "$OUT/sustained.txt"
    ensure_app; sleep 3
    go_home
    echo "loop 0 memory: $(mem)" | tee -a "$OUT/sustained.txt"
    gfx_reset
    t0=$(date +%s)
    for loop in 1 2 3 4 5; do
      ensure_app; s_home; ensure_app; s_map; ensure_app; s_learn; ensure_app; s_pantry; ensure_app; s_game
      echo "loop $loop ($(( $(date +%s)-t0 ))s) memory: $(mem)" | tee -a "$OUT/sustained.txt"
    done
    { echo "== whole sustained run"; gfx_read; } | tee -a "$OUT/sustained.txt"
    $A shell dumpsys activity processes $PKG | grep -ci 'crash\|anr' | sed 's/^/crash or ANR mentions: /' | tee -a "$OUT/sustained.txt"
    ;;
esac
