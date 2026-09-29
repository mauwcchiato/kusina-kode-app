# Ingredients: make the app read the web's ingredients

**For:** Mau (app) · **Also needs:** Alyssa (draw and sell rules) and the API/VM owner
**Status:** proposal; the API half is live (see section 8) · **Written:** 2026-09-29

> The file and class names below were checked against `main` at commit `65b65c5` (29 Sep 2026).
> If `main` has moved on since, please re-check them before starting.

## 1. The problem

There are three separate copies of the ingredient list, and only one of them can be edited:

| Copy | Where | Used for | Editable on the web? |
|---|---|---|---|
| `ingredients` table | Azure SQL, edited on the web **Ingredients** page | nothing in the game | yes |
| `lib/ingredients.php` | API, *generated, "do not hand-edit"* | draws, sell prices, rejecting unknown ids | no |
| `IngredientCatalog.kt` | compiled into the app (123 items) | names, local names, origin, rarity, lore | no |

So an ingredient added, renamed or unpublished on the web never reaches the game. The only thing that gets across is the ingredient *names* ticked on a dish, which arrive as plain dish text.

## 2. Where the app depends on the hardcoded book

`IngredientCatalog.get(id)` is called from:

- `RemotePantryRepository`: mapping a draw, a sell and the pantry snapshot to `Ingredient`. Note that `toSnapshot()` **silently drops** any jar whose id is not in the book, and `sell()` fails with "Unknown ingredient".
- `NotificationArt` and `RewardLabels`: names for reward notifications.
- `PantryIngredientArt` (art is a bundled drawable per id) and `IngredientArt` (dish ingredient lines).

The API returns only ids and quantities (plus name, local name, rarity and sell value in `kk_pantry_public`); lore never leaves the app.

## 3. What has to be decided before the app can read the web

1. **A stable id.** Web rows have an integer `id`. The game keys everything by a string such as `ing_bagoong_fermented_fish_paste`. Nothing on the web stores that string. Options: (a) add a permanent `code` column, set once when the ingredient is created (recommended: a rename on the web must not change what players own), or (b) derive it from the name (`ing_` + lowercase, apostrophes removed, other characters to `_`). The existing 123 ids look like (b), but that needs a test, and (b) breaks the moment someone renames an ingredient.
2. **Who decides draw odds and sell price for a new ingredient.** Today both come from the rarity tier in `lib/ingredients.php` (sell 1 / 3 / 8 / 20 KK, with draw weights). The web table also has a `kk_price` column that the game ignores. Pick one source of truth. This is **Alyssa's** call.
3. **Art for a new ingredient.** The app's pictures are bundled drawables keyed by id, and the web stores an `image_path`. A brand-new web ingredient has no drawable, so the app needs a fallback (the painted jar) or must load the image from a URL.
4. **Status.** The web has Published / Draft. Only Published ingredients should be drawable and visible.
5. **Rarity spelling.** The web stores `Common`, the API `common`, the app `Rarity.COMMON`. Map case-insensitively.
6. **Lore.** The app builds lore from name, local name and category (`tidyLore`). The web has a `short_description`. Decide which wins.

## 4. Proposed change

**API (Kyla / VM owner, not Mau):**
- New `GET ingredient/catalog.php`: Published rows only, with `code`, `name`, `local_name`, `category`, `rarity`, `short_description`, `image_url`, plus a `version` so the app can skip a refetch.
- `lib/ingredients.php` reads the table and falls back to the generated array when the database is unavailable (the same approach `lib/islands.php` took for levels). Sell price and weights stay tier-based unless decision 2 changes that.

**App (Mau):**
- Keep the bundled book as the **offline fallback**. Fetch `catalog.php` on launch and after login, cache it, and serve `all`, `byId`, `total` and `get(id)` from the fetched copy, so existing callers do not change.
- An id that is not in the bundled book must produce a working `Ingredient` built from the server row, and must never be dropped from the pantry or fail a sale.
- Art: bundled drawable when there is one, otherwise the painted jar (or `image_url`).

**Tests:** parsing the response; falling back offline; the 123 existing ids unchanged; an unknown id neither dropped from the pantry nor blocking a sale; art fallback.

## 5. Rollout order (old app versions keep working)

1. API: add `catalog.php` (nothing breaks, it is new).
2. App: ship the version that reads it, with the bundled book as fallback.
3. From then on, a web edit reaches the game. Older app versions keep using their bundled book, and the API still accepts the old ids.

## 6. Smaller alternative for Sprint 12

If this is too big right now: leave the game as is and make the web Ingredients page **reference-only**, with a clear label that edits there do not change the game. That is a web-only change and I can do it in a day.

## 7. Done means

- [ ] Add an ingredient on the web, publish it, and it appears in the app with the right name and rarity.
- [ ] Unpublish or rename one, and the app reflects it after a refresh.
- [ ] A player who owns an ingredient the app has never seen still sees it in the pantry and can sell it.
- [ ] With no network, the bundled book still works.
- [ ] The 123 existing ingredients behave exactly as before.

## 8. Update: the API half is built (29 Sep 2026)

The read-only endpoint from section 4 is **live**: `https://api.kusinakode.com/kusinakode/REST/get_ingredients.php`. It returns all 123 current ingredients, and they match the game's list exactly: the same ids and the same rarities (105 common, 11 uncommon, 6 rare, 1 legendary). Only 9 names differ from `IngredientCatalog.kt`, and only in capital letters (for example "Bay leaves" against "Bay Leaves").

`GET /api/get_ingredients.php`: public and read-only, the same style as `get_equipment.php`.

```json
{
  "status": "success",
  "version": "a1b2c3d4e5f6",
  "count": 123,
  "data": [
    {
      "id": "ing_salt",
      "name": "Salt",
      "local_name": "Asin",
      "category": "Seasoning",
      "rarity": "common",
      "description": "Short description from the web page.",
      "image_path": "images/ingredients/salt.png"
    }
  ]
}
```

- **`id` is the stable code** (decision 1, the recommended option): derived from the name once, when the ingredient is created, then stored and never changed by a rename. The same rule produced all 123 ids the game uses today, and it is tested against every one of them.
- Only **Published** ingredients are listed. An unknown rarity reads as `common`. `image_path` is relative (or `null`), so the app adds its own host, as it does for equipment.
- `version` changes whenever the list changes, so the app can skip a refetch.
- On failure it answers HTTP 500 with `{"status":"error"}`. The app should then keep its bundled book.

**What is deliberately not connected yet (decision 2, Alyssa's call):** the API's draw and sell rules (`lib/ingredients.php`) still use the generated list, by rarity. So an ingredient added on the web will appear in the app's book once this ships, but **cannot be drawn or sold until those rules include it**. The web's `kk_price` is still not used by the game.
