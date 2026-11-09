# Trivia bonus spin — `pantry/trivia_spin.php`

The dish page (Kodex → dish → Steps tab) asks one multiple-choice trivia
question: *Which ingredient goes into {dish}?* A right **first** answer banks
one palayok wheel spin, and the app opens the wheel on the dish page so the
player can turn it at once.

Implemented in the KusinaKode-Web backend at `api/pantry/trivia_spin.php`.
Until it is deployed (the app gets a 404), a right answer just turns green and
the wheel does not open.

## Request

`POST {BASE}pantry/trivia_spin.php`, bearer-authenticated.

```json
{ "level_id": 12, "answer": "Pork", "correct": true }
```

The dish ingredient lists live only in the app, so the app reports `correct`.
The server owns the "first answer only" rule, which caps what any player can
take at one spin per dish.

## How it works

1. `level_id` must exist in `levels`.
2. The first answer for `(user, level)` inserts a `reward_claims` pointer:
   kind `trivia`, key `L<level_id>`, `tx_ref` = `correct` or `wrong`.
   Later calls find the pointer and pay nothing.
3. If that first answer is correct, `kk_pantry_award_spin($pdo, $userId,
   'trivia', 'L<level_id>')` banks the spin in `ingredient_spins`, which is
   itself idempotent on `(user, source, key)`.

No new tables: both are already in the MySQL on-demand DDL and the Azure SQL
schema.

## Response

```json
{
  "status": "success",
  "data": {
    "granted": true,
    "already_answered": false,
    "snapshot": { "spins_available": 2, "...": "same shape as pantry/snapshot.php" }
  }
}
```

`snapshot` is only sent when `granted` is true. Errors use `400`/`401` with
`{ "status": "error", "message": "..." }` — never `404`, which the app reads as
"endpoint not deployed yet".
