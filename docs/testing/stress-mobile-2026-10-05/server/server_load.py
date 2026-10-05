"""Read-only load test for the Kusina Kode API (option B).

Simulated players repeatedly fetch public, read-only endpoints. Nothing is
written: no sign-ups, guesses, purchases, spins or claims.
Stages ramp the number of simultaneous players; the run stops early if
errors pass 5% or the 95th-percentile response passes 10 s.
"""
import json, random, sys, threading, time, urllib.request, urllib.error
from datetime import datetime, timezone

BASE = "https://api.kusinakode.com/kusinakode/REST/"
MIX = [  # (endpoint, weight) — roughly what an app session reads
    ("get_levels.php", 30),
    ("get_leaderboard.php", 20),
    ("get_ingredients.php", 20),
    ("get_equipment.php", 10),
    ("reels/list.php", 10),
]
STAGES = [5, 10, 20, 40]
STAGE_SECONDS = 60
THINK = (0.5, 1.5)          # pause between a player's requests, seconds
ERR_LIMIT, P95_LIMIT = 0.05, 10.0
OUT = sys.argv[1]

names = [m[0] for m in MIX]
weights = [m[1] for m in MIX]
lock = threading.Lock()


def one(endpoint):
    t0 = time.perf_counter()
    try:
        req = urllib.request.Request(BASE + endpoint, headers={"User-Agent": "KusinaKode-loadtest/1.0"})
        with urllib.request.urlopen(req, timeout=30) as r:
            r.read()
            ok = 200 <= r.status < 300
            code = r.status
    except urllib.error.HTTPError as e:
        ok, code = False, e.code
    except Exception as e:
        ok, code = False, type(e).__name__
    return endpoint, time.perf_counter() - t0, ok, code


def pct(xs, p):
    if not xs:
        return None
    xs = sorted(xs)
    k = max(0, min(len(xs) - 1, int(round(p / 100 * len(xs) + 0.5)) - 1))
    return xs[k]


def run_stage(users):
    results, stop_at = [], time.time() + STAGE_SECONDS

    def player():
        while time.time() < stop_at:
            r = one(random.choices(names, weights)[0])
            with lock:
                results.append(r)
            time.sleep(random.uniform(*THINK))

    started = datetime.now(timezone.utc).isoformat(timespec="seconds")
    threads = [threading.Thread(target=player, daemon=True) for _ in range(users)]
    for t in threads:
        t.start()
        time.sleep(0.1)  # stagger arrivals
    for t in threads:
        t.join()
    lat = [r[1] for r in results]
    errs = [r for r in results if not r[2]]
    per = {}
    for n in names:
        l = [r[1] for r in results if r[0] == n]
        per[n] = {"requests": len(l), "avg_s": sum(l) / len(l) if l else None, "p95_s": pct(l, 95),
                  "errors": sum(1 for r in results if r[0] == n and not r[2])}
    return {
        "users": users, "started_utc": started, "seconds": STAGE_SECONDS,
        "requests": len(results), "req_per_s": len(results) / STAGE_SECONDS,
        "errors": len(errs), "error_rate": len(errs) / len(results) if results else 0,
        "error_codes": sorted({str(e[3]) for e in errs}),
        "avg_s": sum(lat) / len(lat) if lat else None,
        "p50_s": pct(lat, 50), "p90_s": pct(lat, 90), "p95_s": pct(lat, 95), "p99_s": pct(lat, 99),
        "max_s": max(lat) if lat else None, "per_endpoint": per,
    }


report = {"started_utc": datetime.now(timezone.utc).isoformat(timespec="seconds"), "stages": [], "stopped_early": None}
for i in range(3):  # warm the server so its idle wake-up does not count against stage 1
    one("get_levels.php")
for u in STAGES:
    s = run_stage(u)
    report["stages"].append(s)
    print(f"{u:>3} players: {s['requests']} req ({s['req_per_s']:.1f}/s), "
          f"avg {s['avg_s']:.3f}s, p95 {s['p95_s']:.3f}s, max {s['max_s']:.3f}s, errors {s['errors']} {s['error_codes']}", flush=True)
    if s["error_rate"] > ERR_LIMIT or (s["p95_s"] or 0) > P95_LIMIT:
        report["stopped_early"] = f"after {u} players (errors {s['error_rate']:.1%}, p95 {s['p95_s']:.2f}s)"
        print("STOPPING EARLY:", report["stopped_early"], flush=True)
        break
    time.sleep(10)
report["ended_utc"] = datetime.now(timezone.utc).isoformat(timespec="seconds")
json.dump(report, open(OUT, "w"), indent=2)
print("saved", OUT)
