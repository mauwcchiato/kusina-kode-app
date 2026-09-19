# Heritage card — export spec

How new dish cards must be exported so they fill the app instead of sitting
as a small stamp on a huge canvas.

## What the app does today

- All **30** cards in `app/src/main/res/drawable/card_*.png` are **522 × 924 px**.
- Aspect ratio: **0.5649** (width ÷ height), about **9:16**.
- On screen (`HeritageCardReveal`): **82% of screen width**, aspect locked to
  `522:924`. On a typical emulator that draws about **886 × 1568 px**.

The Image uses `ContentScale.Fit` on that box. Extra empty canvas around the
card becomes padding in the app, so the card looks tiny.

## For the next cards

- Crop **tight to the card itself** — no extra canvas, no 1920×1080 frame
  with the card floating in the middle.
- Keep ratio **0.5649** (width ÷ height). Exact **522×924 is not required**
  as long as the ratio matches.
- **PNG**. Transparent background if the card has rounded corners.
- **Minimum 522 px wide** (larger is fine; the app downscales).

## Why this exists

Older exports used a **1920×1080** canvas with a small card in the center.
Those looked tiny in-app and all 30 had to be cropped by hand.

The same 1920×1080-canvas problem happened with **equipment** images
(`equip_*.png`). Crop those tight to the object too — no unused frame.
