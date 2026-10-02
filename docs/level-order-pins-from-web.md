# Pinned island levels: what the app needs to do

From Kyla, 1 Oct 2026. Written against `main` at 1a33c78 (app 1.23).

## What is changing

The admin console can now **pin** a dish to an island level ("Mindanao Level 2"). A pinned dish takes exactly that
slot; every other dish keeps filling the remaining slots in today's order (word length, shortest first, ties by id).
A dish with no pin behaves exactly as it does now, so nothing changes for the app until an admin pins something.

Nothing about identity changes. The global id (the row id / the position the app sends as `level_id`) is still what
progress, unlocks, attempts, rewards and the chain are filed under. A pin only changes the **number shown in the island
and the unlock order inside it**. That is the part of the app that is already display-only: `LevelProvider.regionOrder`
and `regionLevelNumber`.

## The data

`get_levels.php` already returns every column of `levels`. After the server update each row has one more field:

```json
{ "id": "22", "word": "satti", "region": "Mindanao", "level_order": null }
{ "id": "31", "word": "kinilaw", "region": "Mindanao", "level_order": "2" }
```

`level_order` is a whole number of 1 or more, or `null` for automatic. Old rows and old servers have no such field: read
it as null. (It arrives as a string from PHP like `id` does.)

## What the app changes

1. `LevelSync.Cached` / `toRemote` carry `level_order` into `LevelProvider.RemoteText` as `val levelOrder: Int? = null`
   (blank, missing or not a positive whole number means null).
2. A compiled level takes its pin from the server row that matches its word (the same join `applyRemote` already uses to
   re-word it). A panel-added level has it on its own row. The pin has to be stored on `LevelData` (for example
   `val levelOrder: Int? = null`) so `regionOrder` can read it.
3. `LevelProvider.regionOrder(region)` becomes:

```
island = visible levels whose region == region          // as today (visibleIds filtered by region)
n      = island.size
slots  = array of n, all empty

// pinned first, in (pin, global id) order
for level in island.filter { levelOrder != null }.sortedWith(compareBy({ levelOrder }, { id })):
    want = clamp(level.levelOrder, 1, n) - 1            // 0-based
    at   = first free slot at or after `want`; if none, the nearest free slot before `want`
    slots[at] = level

// then everyone else, in today's order, into the free slots from the top
rest = island.filter { levelOrder == null }.sortedWith(compareBy({ answer.length }, { id }))
fill the empty slots, in ascending order, with `rest`
```

`regionLevelNumber`, `nextInRegion`, `nextPlayableInRegion`, `nextPlayable` and the Game Map all read `regionOrder`, so
they follow with no other change. The unlock rule stays "Level 1 of an island is open; Level K opens when Level K-1 is
solved", now by the new order.

The clamp and the "nearest free slot" only matter if the data is inconsistent (a pin past the end, two pins on one slot).
The console and the API refuse both when saving, so they can only happen after a dish is later removed or moved.

## Shared test cases

The console computes the same order (`IslandOrder` in the web repo, `IslandOrderTests`). These cases should pass in the
app's `LevelProvider` tests too. Global id, word, letters in brackets.

**Mindanao today, no pins**: Tiyula 18 (6), Piaparan 19 (8), Pastil 20 (6), Sinuglaw 21 (8), Satti 22 (5), Kulma 29 (5)

| Island level | Dish |
|---|---|
| 1 | Satti |
| 2 | Kulma |
| 3 | Tiyula |
| 4 | Pastil |
| 5 | Piaparan |
| 6 | Sinuglaw |

**Add Kinilaw 31 (7 letters), no pin**: Satti, Kulma, Tiyula, Pastil, **Kinilaw**, Piaparan, Sinuglaw.

**Add Kinilaw 31, pinned to 2**: Satti, **Kinilaw**, Kulma, Tiyula, Pastil, Piaparan, Sinuglaw.

**Pin Satti to 6 and Tiyula to 1** (no Kinilaw): Tiyula, Kulma, Pastil, Piaparan, Sinuglaw, Satti.

**Luzon with adobo 1, sinigang 2, sisig 4 pinned to 2**: Adobo, Sisig, Sinigang.

**Pin past the end** (adobo 1; sinigang 2 pinned to 9): Adobo, Sinigang.

**Two pins on one slot** (adobo 1, sinigang 2 pin 2, sisig 3 pin 2, laing 4): Adobo, Sinigang, Sisig, Laing.

**Two pins on the last slot** (adobo 1, sinigang 2 pin 4, sisig 3 pin 4, laing 4): Adobo, Laing, Sisig, Sinigang.

## What can go wrong for a player

A pin can put a level the player has not solved in front of ones they already have. Solved levels stay solved; "next
level" follows the new order, so the app may offer the pinned level first. The console warns admins that pins reach
phones only with the app version that reads them.

## Order of rollout

1. Server and console first (the column, the API rule, the console UI). Nothing reads the field yet, so nothing changes in
   the app.
2. The app release that reads `level_order`.
3. Admins pin as needed.

An older app ignores the field and keeps the automatic order, so for those phones the number the console shows for a
pinned dish will not match what the player sees.
