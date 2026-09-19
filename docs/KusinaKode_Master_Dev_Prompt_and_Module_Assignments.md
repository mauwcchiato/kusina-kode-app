# Kusina Kode — Master Development Prompt & Module Assignment
### Capstone 2 Implementation Guide: Blockchain, Gamification & Cloud Integration
Grounded strictly in *Kusina Kode: Integrating Blockchain Incentives in Gamified Filipino Culinary Education* (Final, Post-Defense Manuscript, IT200-1D)

---

## 0. How to Use This Document

1. **Everyone reads Section 1–3 first.** This is the shared truth-source — what the paper actually promised the panel. Every teammate's Claude Code session should be grounded in this, not in the old Capstone 1 build alone.
2. **Each teammate goes to their assigned module in Section 6.** Each module block contains a ready-to-paste **Claude Code prompt** — copy the whole fenced block into Claude Code (Android Studio for mobile modules, VS Code for web/backend/blockchain modules) as your first message in a fresh session.
3. **Before any prompt is run, read Section 5 (Integration Contract) together as a team.** This is what stops four parallel Claude Code sessions from producing four incompatible codebases. Lock the endpoint names, payload shapes, and folder boundaries *before* anyone starts generating code.
4. **Step 0 inside every module prompt is "explore first."** None of us has given Claude Code your actual current repo yet, so every prompt starts by asking Claude Code to inventory what already exists before changing anything. Do not skip this step even if you're in a hurry.

---

## 1. What the Manuscript Actually Committed To

Three specific objectives (Chapter 1) — every module below traces back to one of these:

1. **Word puzzle + cultural learning module** — a Wordle-style game where users guess Filipino dish names, with cultural heritage info, regional origin, ingredients, and expert-validated recipes revealed after each round.
2. **Gamification layer** — points, badges/achievements, leaderboard, and progress tracking, designed to sustain motivation and participation (not just decoration).
3. **Blockchain incentive layer** — user rewards, achievements, and activity records secured through AES-256 encryption, SHA-256 hashing, ECDSA signatures, Proof-of-Authority consensus, and smart contract execution, in a transparent, tamper-resistant, and independently verifiable way.

The **research question** the whole system has to answer: *How can a blockchain-based, gamified Filipino culinary word-puzzle application support vocabulary/heritage learning while maintaining engagement through gamification and securing rewards through a transparent, tamper-resistant blockchain mechanism?*

**Why blockchain at all (don't lose this framing when building):** the paper's own literature review is explicit that conventional gamified apps store points/badges/leaderboard as *editable database rows with no cryptographic proof of legitimacy* — that's the exact weakness Kusina Kode is fixing. Every teammate should keep that contrast in mind: badges and points need to be *provably* legitimate, not just displayed.

**Guiding design principle from the paper itself (Section 3.2):** blockchain reward mechanics must stay *"transparent and intuitive rather than technically complex for end users."* Practical translation: **players never see seed phrases, gas fees, or MetaMask popups.** Wallets are custodial/managed server-side. This constraint should shape every mobile/UX decision in Modules 2 and 3.

---

## 2. Current State vs. Target State

| | Capstone 1 build (what exists) | Capstone 2 target (what the manuscript promises the panel) |
|---|---|---|
| Mobile | Android, Kotlin, Jetpack Compose, MVVM | Same stack, refined + Clean Architecture layering formalized |
| Backend | PHP REST API + MySQL on XAMPP (local) | PHP REST API on **Azure Virtual Machine (IaaS)** |
| Database | Single MySQL instance | **Azure SQL Database** (relational: users, attempts, leaderboard) + **Azure Cosmos DB** (documents: dish trivia, recipes, heritage content) |
| Media | Local/simple storage | **Azure Blob Storage** (dish images, media) |
| Secrets | Hardcoded / local config | **Azure Key Vault** |
| Auth | Basic login | **Azure Active Directory B2C**, role-based (Player vs Administrator) |
| Rewards | Points/badges as plain editable DB rows | **Blockchain-verified** points/badges — private Polygon-compatible sidechain, PoA consensus, smart contracts, TxHash receipts |
| Word length | Fixed 6-letter grid (per old docs) | **Not restricted to 6 letters in the final paper** — example words cited are *pakbet* (6), *adobo* (5), *sinigang* (8). This is a real change to flag: the grid needs to support variable word length, not just resize cosmetically. |
| Admin | Basic CRUD panel | Full admin dashboard + **blockchain audit panel** (view TxHash, verify on-chain records) |

---

## 3. Target System Architecture (Three Tiers — memorize this, it's what Chapter 3's diagram shows)

```
┌─────────────────────────────── CLIENT TIER ───────────────────────────────┐
│  Android App (Kotlin, Jetpack Compose, MVVM + Clean Architecture)         │
│  Web Admin Dashboard (browser-based, Clean/layered structure)             │
└──────────────────────────────────┬────────────────────────────────────────┘
                                    │  HTTPS / TLS
┌───────────────────────── MIDDLEWARE & CLOUD TIER ─────────────────────────┐
│  PHP REST API — Azure VM (IaaS)                                            │
│  Web Admin backend — Azure App Service (PaaS)                             │
│  Azure SQL Database   (users, attempts, unlocks, leaderboard)             │
│  Azure Cosmos DB      (dish trivia, recipes, cultural heritage docs)      │
│  Azure Blob Storage   (dish images, media assets)                         │
│  Azure Key Vault      (encryption keys, secrets)                          │
│  Azure AD B2C         (auth, role-based access control)                   │
└──────────────────────────────────┬────────────────────────────────────────┘
                                    │  Smart contract calls / signed tx
┌───────────────────────── BLOCKCHAIN LEDGER TIER ──────────────────────────┐
│  Private Polygon-compatible sidechain — Proof-of-Authority consensus      │
│  (trusted validator nodes run by the research team, not public mainnet)   │
│  Smart contracts — automated reward distribution                          │
│  TxHash receipts — permanent, publicly verifiable per reward              │
└──────────────────────────────────────────────────────────────────────────┘
```

**The four-phase reward pipeline** (this is the actual sequence every "earn a reward" action must follow — cite this to Claude Code when building the Blockchain Incentive Module so it doesn't collapse steps):

- **Phase 1 — Trigger & Encrypt:** user completes a puzzle/earns a score/redeems a reward → event captured → payload encrypted with **AES-256** → **SHA-256** hash generated as a tamper-detectable fingerprint.
- **Phase 2 — Assemble & Sign:** hash + payload assembled into a transaction → transaction placed into a block (timestamp + previous block hash) → block signed with **ECDSA** → validated by trusted nodes under **Proof-of-Authority** consensus.
- **Phase 3 — Append & Sync:** block appended to the chain (tamper-evident linking — altering one block invalidates every block after it) → synchronized across all participating nodes.
- **Phase 4 — Execute & Receipt:** smart contract reads the validated data → evaluates reward conditions → transfers tokens from the rewards pool to the user's (custodial) wallet → generates a **TxHash** receipt → receipt is retrievable through the admin dashboard's audit panel.

**Security baseline (CIA triad, Chapter 3):**
- Data in transit → TLS/SSL between app and backend.
- Data at rest → AES-256 (Azure SQL + Cosmos DB).
- Key management → Azure Key Vault (no keys in app config or source).
- Integrity → SHA-256 hashing on every reward record.
- Non-repudiation → ECDSA signatures per transaction.
- API → zero-trust model, every request re-verified regardless of origin; Azure AD B2C role separation between Player and Administrator; input validation, rate limiting, minimal data exposure.

---

## 4. Module Map (this is how the paper's own Unit Testing section already splits the system — Chapter 3 literally tests these five as separate units, so we build them as separate units too)

| # | Module | Traces to Objective | What gets unit-tested per the paper |
|---|---|---|---|
| 1 | **Word Puzzle Game Engine** | Objective 1 | Letter input validation, color-coded feedback logic, attempt tracking |
| 2 | **Gamification Module** | Objective 2 | Points calculation, badge awarding, leaderboard ranking |
| 3 | **Blockchain Incentive Module** | Objective 3 | AES-256 encryption trigger, SHA-256 hash generation, ECDSA signature validation, smart contract execution |
| 4 | **Cultural Content Module** | Objective 1 | Dish data retrieval, recipe display |
| 5 | **Administrative Dashboard** | Supports 2 & 3 | Content management, user management, audit log access |

Plus two cross-cutting tracks that don't show up as "unit tests" but everything above depends on them:

| # | Track | Why it's separate |
|---|---|---|
| 6 | **Cloud Infrastructure & Security** | Azure VM/App Service/SQL/Cosmos/Blob/Key Vault/AD B2C setup — everyone else's module is built *on top of* this |
| 7 | **Testing, QA & Documentation** | Unit testing, stress testing (API response time, DB query performance, blockchain node tx speed, mobile rendering under load), and keeping the manuscript's claims and the actual build in sync |

---

## 5. Shared Integration Contract — Agree on This *Before* Anyone Opens Claude Code

This is the part that prevents four people's parallel Claude Code sessions from producing incompatible output. Paste this section into **every** module prompt, not just your own — everyone needs the same contract in view.

### 5.1 Ownership boundaries (don't edit outside your lane without a heads-up in the group chat)
- Mobile app source → owned by whoever's on Module 1/2/3-mobile-side/4-mobile-side.
- `admin/` or web dashboard source → owned by Module 5's owner.
- `contracts/` (Solidity) and blockchain node config → owned by Module 3's owner.
- `api/` PHP backend → shared, but each module only touches its own endpoint files (see 5.2). If your module needs a new shared table/column, propose it in the group chat before running a migration.

### 5.2 REST API endpoint conventions
Before writing new endpoints, **have Claude Code list the existing PHP files first** (the Capstone 1 build already has `register.php`, `login.php`, `get_levels.php`, `get_unlocks.php`, `post_unlock.php`, `post_attempt.php`, `get_leaderboard.php`, `get_profile_stats.php` — confirm these still exist and match this list before extending them). New endpoints needed for this phase, proposed naming (adjust together if the existing convention differs):

| Endpoint | Module | Purpose |
|---|---|---|
| `POST /api/reward/trigger.php` | 3 | Fires Phase 1 of the reward pipeline after a verified game event |
| `GET /api/reward/status.php?tx_ref=` | 3 | Poll status of a pending reward transaction |
| `GET /api/wallet/balance.php?user_id=` | 3 | Read-only token balance for the custodial wallet |
| `GET /api/audit/log.php` | 3 + 5 | Admin-facing list of TxHash receipts, filterable |
| `GET /api/audit/tx.php?hash=` | 3 + 5 | Single transaction detail + verification status |
| `GET /api/badges.php?user_id=` | 2 | Earned badges for a user |
| `GET /api/dish/content.php?level_id=` | 4 | Full cultural content payload (history, region, ingredients, procedure, tools, significance) — extends `get_levels.php` rather than duplicating it if that field set already exists there |
| `POST/PUT/DELETE /api/admin/users.php` | 5 | Admin user CRUD |
| `POST/PUT/DELETE /api/admin/levels.php` | 5 | Admin dish/level CRUD |

### 5.3 Core data objects (keep field names identical across mobile, web, and backend)
```
User        { user_id, name, email, role[player|admin], wallet_address, created_at }
Attempt     { attempt_id, user_id, level_id, is_correct, time_taken_ms, timestamp }
Level       { level_id, word, trivia, history, region, ingredients[], procedure, tools[], image_url }
Badge       { badge_id, user_id, badge_type, milestone_criteria, tx_hash, awarded_at }
RewardTx    { tx_ref, user_id, event_type, payload_hash(sha256), signature(ecdsa), block_ref, tx_hash, status[pending|confirmed|failed] }
```
**Critical boundary from the manuscript's own scope note (memory-worthy):** the relational database (Azure SQL) should hold **no blockchain transaction internals** — no raw hashes-as-source-of-truth, no consensus data. It only holds **pointer fields** (`wallet_address`, `tx_hash`, `badge_id`) that reference the chain. The chain is the source of truth for reward legitimacy; SQL/Cosmos are for fast reads/UI.

### 5.4 Wallet model — decide once, apply everywhere
Custodial/managed wallets: the backend generates and holds a wallet per user (players never see a private key or seed phrase, per Section 3.2's UX principle). Module 3 owns key generation/storage (via Key Vault); Modules 1, 2, 4, 5 only ever call read-only balance/badge/audit endpoints — never handle keys directly.

### 5.5 Git convention (suggested — confirm with team)
Branch per module: `feature/word-puzzle-engine`, `feature/gamification`, `feature/blockchain-incentive`, `feature/cultural-content`, `feature/admin-dashboard`, `feature/cloud-infra`. PRs reviewed by at least one other teammate before merging into `develop`.

---

## 6. Team Assignment — from the signed Capstone 1 roles

> **Corrected 2026-08-12.** An earlier revision of this section proposed an
> ownership split that contradicted the signed Roles and Responsibilities page
> — it put blockchain with Mau, the admin portal with Alyssa, and QA with
> Kyla. The signed page below is the authority. If the two ever disagree
> again, the signature wins.

| Member | Signed role | Signed module | Owns |
|---|---|---|---|
| **Jhan Maurice De Roxas (Mau)** | Project Manager | **Game Systems & Cloud Integration** | Game engine · level & progression · hint system · progress tracking · cloud integration · cross-module integration |
| **Kyla Christelle Caccam** | System Analyst / Research & Documentation | **Infrastructure & User Interface** | Cloud architecture · cultural content view · analytics dashboard view · manuscript documentation |
| **Yowanna Andwele Montibon** | UI/UX Designer & QA Tester | **Governance & System Protection** | Web management portal · system settings · activity monitoring & analytics · QA and test execution |
| **Alyssa Opiña** | Developer & Technical Lead | **Blockchain Rewards & Backend Logic** | Blockchain implementation · rewards & achievements engine · identity & user management |

### 6.1 Two taxonomies — don't confuse them

There are two different ways this system gets divided, and both are real:

- **The manuscript** decomposes the *system* into five units for Chapter 3's
  Unit Testing section: Word Puzzle Engine, Gamification, Blockchain Incentive,
  Cultural Content, Administrative Dashboard. The numbered module briefs in
  Section 8 onward follow this, and so does the test log's Module column.
- **The signed page** assigns *people* to four feature groupings.

They are not 1:1. "Gamification" as a testable unit splits across Mau's
progress tracking and Alyssa's rewards engine. Map between them like this:

| Testable unit (manuscript) | Owner (signed) |
|---|---|
| Module 1 — Word Puzzle Engine | Mau |
| Module 2 — Gamification: points, badges, leaderboard | Alyssa (rewards & achievements engine) |
| Module 2 — Gamification: progress tracking, hints | Mau (game systems) |
| Module 3 — Blockchain Incentive | Alyssa |
| Module 4 — Cultural Content | Kyla (content view) |
| Module 5 — Administrative Dashboard | Yow (web management portal) |
| Module 6 — Cloud Infrastructure | Kyla (architecture) + Mau (integration) |
| Module 7 — Testing & Documentation | Yow (QA execution) + Kyla (documentation) |

### 6.2 Load is not even — plan support accordingly

Mau's lane is largely delivered; Alyssa's is largely ahead of her. Blockchain
plus identity is the single biggest remaining block of work in the project, so
Mau's Project Manager time is best spent supporting 3b (backend pipeline)
rather than starting something new. Support pairings are noted per phase below.

---

## 7. Build Order — chronological, with every member working each phase

Ordered by dependency. Nobody is idle in any phase.

### Phase A — Foundation *(current)*
| Member | Work | Blocked by |
|---|---|---|
| **Kyla** | Provision Azure: VM, App Service, SQL, Cosmos, Blob, Key Vault, AD B2C | — |
| **Mau** | Cloud integration: migrate API + data, replace the hard-coded LAN base URL, wire Key Vault | Kyla's provisioning for the final cutover; prep can start now |
| **Alyssa** | Blockchain 3a: Hardhat, token + badge contracts, local PoA, contract tests | — (deliberately independent of cloud) |
| **Yow** | Stand up the web-side test framework; run the mobile rendering stress test | — (the one stress target that needs no cloud) |

### Phase B — Identity and content
| Member | Work | Blocked by |
|---|---|---|
| **Alyssa** | Identity & user management: swap interim bearer tokens for AD B2C, roles, close the open Tier 2 endpoints | Phase A cloud |
| **Kyla** | Cultural content: validated dish dataset into Cosmos, records, recipes | — |
| **Yow** | Web management portal against real content; admin role gating | Alyssa's roles |
| **Mau** | Point the app at Azure; verify progression, hints and progress against cloud | Phase A |

### Phase C — Rewards on chain
| Member | Work | Blocked by |
|---|---|---|
| **Alyssa** | Blockchain 3b: the four-phase pipeline and its endpoints | Phase A + B |
| **Mau** | Mobile wallet & reward display (3c) — read-only, never handles keys | Alyssa's endpoints |
| **Yow** | Audit panel front-end | Alyssa's `audit/log.php`, `audit/tx.php` |
| **Kyla** | Analytics dashboard view | Data flowing from B |

### Phase D — Formal testing and write-up
| Member | Work |
|---|---|
| **Yow** | Formal unit + stress testing pass across all four stress targets; defect logging |
| **Kyla** | Manuscript claim vs. actual build reconciliation; evidence write-ups |
| **Mau** | Cross-module integration; release |
| **Alyssa** | Technical lead: triage and fix defects across the backend and chain |

**Standing rule:** Module 3 integrates last among the core modules because it
listens for events Modules 1 and 2 emit. The contracts themselves do not have
to wait — only the event wiring does.

---

## 8. Per-Module Claude Code Prompts

Copy the entire fenced block for your module. Each one starts with a mandatory "explore first" step — **do not let Claude Code skip it**, since none of these prompts have seen your actual repo yet.

---

### Module 1 — Word Puzzle Game Engine
**Environment:** Android Studio · **Owner:** Mau (logic) · **Support:** Yow (UI/animation)

```
You are working in Android Studio on the Kusina Kode Android app (Kotlin, Jetpack
Compose, MVVM + Clean Architecture). This is a capstone project: a Wordle-style
Filipino dish-name word puzzle game.

STEP 0 — EXPLORE FIRST: Before changing anything, read through the existing project
structure, identify the current game engine files (letter input handling, tile
coloring logic, attempt tracking, ViewModel(s) for the game screen), and summarize
what you find before proposing any changes. Do not assume file names.

CONTEXT FROM THE FINAL CAPSTONE MANUSCRIPT (ground truth — follow this, not just
whatever the old build currently does):
- The game must let users identify Filipino dish names via a Wordle-style mechanic:
  color-coded feedback per letter (correct position, present-but-misplaced, absent),
  across multiple attempts.
- IMPORTANT CHANGE: the manuscript's own example words are "pakbet" (6 letters),
  "adobo" (5 letters), and "sinigang" (8 letters) — the grid must NOT be hardcoded
  to a fixed 6-letter/6x6 layout. Generalize the grid and keyboard-state logic to
  support variable word length per level, driven by the actual dish name's length.
- Attempt tracking (guess history, correctness, time taken) must be logged per
  round in a way the Gamification and Blockchain modules can consume afterward
  (see the shared Attempt data object below) — do not silently drop this data.

SHARED DATA CONTRACT (match these field names exactly, other modules depend on them):
Attempt { attempt_id, user_id, level_id, is_correct, time_taken_ms, timestamp }
Level   { level_id, word, trivia, history, region, ingredients[], procedure, tools[], image_url }

YOUR TASKS:
1. Refactor the tile/keyboard state logic to support variable-length words safely
   (no crashes on 5, 6, 7, 8+ letter dishes; keyboard max-attempts logic should
   scale sensibly, e.g. word_length + 1, confirm a reasonable rule with the team
   rather than guessing silently).
2. Ensure each completed round emits an Attempt object matching the schema above,
   persisted via the existing attempts API (post_attempt.php) — confirm the current
   payload shape matches this schema and adjust either side minimally, don't
   duplicate logic.
3. On a correct guess, the round completion event must be exposed in a way Module 2
   (Gamification) and Module 3 (Blockchain) can subscribe to — do not embed points
   or reward logic directly into this module; the game engine only reports "this
   round was completed correctly, here's the Attempt," nothing about rewards.
4. Keep MVVM + Clean Architecture layering: no direct network/DB calls from
   Composables; ViewModel exposes StateFlow; use-case/repository layers stay intact.

DO NOT touch: gamification UI (badges/leaderboard/points display), wallet or
blockchain code, admin dashboard, or the cultural content reveal screen's content
logic (you can call into it, but don't rewrite it) — those are other teammates'
modules.

ACCEPTANCE CRITERIA (matches the manuscript's own Unit Testing section for this
module — write/run tests for these explicitly):
- Letter input validation: invalid/incomplete guesses rejected correctly for any
  word length.
- Color-coded feedback logic: correct-position, wrong-position, and absent letters
  all classified correctly, including repeated-letter edge cases.
- Attempt tracking: every guess (correct or not) is recorded with accurate
  timestamp and time_taken_ms.

Explain your plan before making large structural changes, then implement
incrementally, showing me diffs as you go.
```

---

### Module 2 — Gamification Module
**Environment:** Android Studio (+ light backend endpoints) · **Owner:** Alyssa (rewards & achievements engine) · **Support:** Mau (progress tracking + hint system, which sit in his Game Systems lane)

```
You are working in Android Studio on the Kusina Kode Android app (Kotlin, Jetpack
Compose, MVVM + Clean Architecture). You own the gamification layer: points,
badges/achievements, leaderboard, and progress tracking.

STEP 0 — EXPLORE FIRST: Read the existing project structure. Find any existing
points/leaderboard/profile code (the old build had a "Kusina Masters" leaderboard,
a Profile screen showing rank/fastest time/highest level, and a Levels screen).
Summarize what already exists before changing anything.

CONTEXT FROM THE FINAL CAPSTONE MANUSCRIPT (ground truth):
The gamification layer must include, per the paper's Scope section, exactly these
four elements — don't add scope beyond this without checking with the team first:
  i.   Points System — users earn points on successful round completion; points
       accumulate across sessions as the primary progress measure.
  ii.  Badges and Achievements — awarded at specific milestones: completing a set
       number of rounds, maintaining a winning streak, or achieving a perfect score.
  iii. Leaderboards — ranks users by accumulated points, encouraging competition.
  iv.  Game Progress Tracking — records each user's attempts, performance history,
       and progression over time, visible to the user.

CRITICAL FRAMING: the whole reason this project exists is that conventional
gamified apps store points/badges as plain editable database rows with no
cryptographic proof of legitimacy. Your job is to compute points/badge-eligibility
correctly and expose clean events — but the actual *authoritative, tamper-proof*
record lives in the Blockchain Incentive Module (Module 3), not here. Don't treat
your local DB values as the final source of truth for "does this user really have
this badge" — that answer ultimately comes from the chain via Module 3's audit
endpoints. Your UI should be ready to show a "verifying on-chain..." or
"blockchain-verified ✓" state per badge/reward once Module 3's endpoints exist.

SHARED DATA CONTRACT (match exactly):
Badge { badge_id, user_id, badge_type, milestone_criteria, tx_hash, awarded_at }
Endpoints you will call (may not exist yet — coordinate with Module 3/5 owners,
build against these agreed names): GET /api/badges.php?user_id=,
GET /api/leaderboard... (confirm existing get_leaderboard.php still matches),
GET /api/profile_stats... (confirm existing get_profile_stats.php).

YOUR TASKS:
1. Implement/refine points calculation triggered off Module 1's "round completed
   correctly" event — do not duplicate game logic, just subscribe to it.
2. Implement badge-eligibility logic for the three milestone types above (round
   count, win streak, perfect score) — expose an event/interface when a badge is
   newly earned rather than writing it silently, since Module 3 needs to hear about
   it to fire the blockchain reward pipeline.
3. Build/refine the leaderboard screen (ranking by accumulated points) and the
   progress tracking view (attempt history, performance over time).
4. Add a badge_tx_hash-aware UI state: pending verification vs. blockchain-confirmed,
   even if Module 3 isn't done yet — stub it cleanly so integration later doesn't
   require a rewrite.

DO NOT touch: word puzzle engine internals, blockchain/wallet code, admin dashboard,
cultural content reveal screen.

ACCEPTANCE CRITERIA (per the manuscript's Unit Testing section):
- Points calculation is correct and consistent across sessions.
- Badge awarding correctly triggers exactly at each milestone condition (no early/
  late/duplicate awards).
- Leaderboard ranking is accurate and updates correctly after new attempts.

Explain your plan first, then implement incrementally with diffs.
```

---

### Module 3 — Blockchain Incentive Module
**Environment:** VS Code (contracts + backend) with Android Studio touch-points (wallet UI) · **Owner:** Alyssa · **Support:** Mau (3c mobile display only)

This is the primary technical contribution of the paper — split it into three sub-prompts so it doesn't become one unmanageable session. Run them in order.

**3a — Smart Contracts (Solidity)**
```
You are working in VS Code on the Kusina Kode blockchain layer. Target: a private,
permissioned Polygon-compatible sidechain running Proof-of-Authority consensus
(NOT the public Polygon mainnet, and NOT public Proof-of-Stake — this is explicit
in the manuscript: a small set of trusted validator nodes run by the research team).

STEP 0 — EXPLORE FIRST: Check if a `contracts/` directory, Hardhat/Foundry config,
or any existing Solidity files already exist in this repo. Summarize findings
before writing new contracts.

CONTEXT: The reward mechanism must let smart contracts "read validated data,
automatically evaluate reward conditions, transfer tokens from the rewards pool to
the user's wallet, and generate a TxHash receipt" (direct paraphrase of the
manuscript's own description). This is Phase 4 of the four-phase pipeline; phases
1–3 (encryption, hashing, signing, PoA validation) are handled by the backend
before a transaction ever reaches your contracts.

DESIGN DECISION TO CONFIRM WITH THE ADVISER/PANEL (the manuscript describes
"tokens" being transferred and separately discusses badges/achievements — it does
not name specific contract files, so this is a reasonable implementation choice,
not a literal requirement — flag it as such if asked):
- A fungible reward token contract (ERC-20-style) representing the points-backed
  in-platform currency ("KK Coins" or similar — confirm naming with the team),
  minted/held in a rewards-pool balance, transferred to a user's wallet address on
  a verified reward event.
- A non-fungible badge/achievement contract (ERC-721-style) for milestone badges,
  minted to a user's wallet on verified badge-eligibility, matching the paper's
  literature review support for NFT-based, independently verifiable achievement
  records (vs. plain editable DB rows).

YOUR TASKS:
1. Set up the dev environment (Hardhat recommended, since it plays well with a
   custom PoA sidechain config) if not already present.
2. Write the reward token contract with: rewards-pool balance, an owner/validator-
   restricted mint or transfer function (only the backend's validator-signed calls
   should be able to trigger a transfer — this is what makes it "tamper-resistant,"
   don't leave an open mint function), and a standard balanceOf-style read.
3. Write the badge/achievement contract: mint-on-verified-milestone, one badge per
   (user, badge_type) to prevent duplicates, and a read function returning a
   user's earned badges.
4. Write deployment scripts targeting the PoA sidechain config (use environment
   variables for RPC URL / validator keys — nothing hardcoded, this feeds into
   Module 6's Key Vault setup).
5. Write basic contract tests: correct transfer amounts, rejection of unauthorized
   callers, no duplicate badge minting.

Explain contract design before writing code. Flag any place where you're making an
assumption the manuscript doesn't literally specify (like the exact token symbol
or whether badges are ERC-721 vs. a simpler on-chain record) so it can be confirmed
with the adviser.
```

**3b — Backend Integration (PHP REST API)**
```
You are working in VS Code on the Kusina Kode PHP REST API backend (currently PHP
+ MySQL/XAMPP locally, migrating toward Azure SQL Database + Azure Cosmos DB +
Azure Key Vault per the manuscript — coordinate with whoever owns Module 6/Cloud
Infra on connection details, don't assume Azure resources already exist).

STEP 0 — EXPLORE FIRST: List existing PHP files (expect something like db.php,
register.php, login.php, get_levels.php, get_unlocks.php, post_unlock.php,
post_attempt.php, get_leaderboard.php, get_profile_stats.php). Confirm this
matches reality before adding new files.

CONTEXT — implement the full four-phase pipeline server-side, in order, don't
collapse steps:
Phase 1: on a verified reward-triggering event (round win, badge milestone, reward
  redemption) — encrypt the event payload with AES-256, then generate a SHA-256
  hash of the encrypted payload as a tamper-detectable fingerprint.
Phase 2: assemble the hash + payload into a transaction object, package it into a
  block structure (include a timestamp and a reference to the previous block's
  hash), sign it with ECDSA, and submit it for Proof-of-Authority validation by
  the trusted validator node(s) set up in Module 3a/Module 6.
Phase 3: once validated, append the block to the chain (this should be handled by
  your PoA node/client library, not hand-rolled) and confirm synchronization.
Phase 4: call the deployed smart contract (from 3a) to execute the actual token
  transfer / badge mint, capture the resulting TxHash, and store it as a pointer
  (not as source-of-truth duplication) in Azure SQL/Cosmos so the app and admin
  dashboard can display it quickly without hitting the chain on every read.

NEW ENDPOINTS TO BUILD (confirm naming against the team's Integration Contract
before finalizing):
- POST /api/reward/trigger.php — accepts a verified game/badge event, runs phases
  1–4, returns a tx_ref immediately (async) with status=pending.
- GET /api/reward/status.php?tx_ref= — poll for phase completion / final tx_hash.
- GET /api/wallet/balance.php?user_id= — read-only, calls the token contract's
  balanceOf via the wallet manager, does not expose any private key material.
- GET /api/audit/log.php and GET /api/audit/tx.php?hash= — for Module 5's audit
  panel; return structured records (event_type, user, amount/badge, tx_hash,
  timestamp, status).

SECURITY REQUIREMENTS (non-negotiable, from Chapter 3):
- No private keys or seed phrases ever leave the backend — wallets are custodial,
  generated and held server-side, keys stored via Azure Key Vault, never in code
  or plain config.
- All endpoints require Azure AD B2C-authenticated requests with role checks
  (Player can trigger/read their own rewards; only Administrator role can read
  the full audit log).
- Validate and rate-limit every endpoint; minimal data exposure (don't return more
  fields than the caller's role needs).

DO NOT touch: mobile app code, admin dashboard front-end (you're building the
endpoints it will call, not the UI itself), word puzzle/gamification business
logic beyond consuming the events they emit.

ACCEPTANCE CRITERIA (per the manuscript's Unit Testing section):
- AES-256 encryption trigger fires correctly on every qualifying event.
- SHA-256 hash generation produces a correct, tamper-detectable fingerprint (test
  that altering the payload changes the hash).
- ECDSA signature validation correctly accepts valid signatures and rejects
  invalid/forged ones.
- Smart contract execution correctly transfers the right amount/mints the right
  badge and returns a valid TxHash.
Also prepare this module for the stress-testing pass later: blockchain node
transaction processing speed under concurrent load is one of the paper's explicit
stress-test targets.

Explain your plan before implementing, and implement one phase at a time so each
can be verified before moving to the next.
```

**3c — Mobile Wallet & Reward UI**
```
You are working in Android Studio on the Kusina Kode Android app. You're adding
the player-facing side of the blockchain reward system — NOT the contracts or
backend pipeline (that's already being built separately), just the UI/state layer
that talks to the endpoints in 3b.

STEP 0 — EXPLORE FIRST: Check what profile/rewards-related screens already exist
before adding new ones.

CONTEXT: Per the manuscript's own design priority, blockchain mechanics must stay
"transparent and intuitive rather than technically complex for end users." Concrete
rule: the player should NEVER see a seed phrase, private key, gas fee, or wallet-
connect popup. From their perspective, they just see "reward earned," a short
"verifying..." state, then "blockchain-verified ✓" with a way to view/share the
TxHash if curious (optional detail view, not a required step).

YOUR TASKS:
1. Build a lightweight ViewModel/repository that calls
   GET /api/wallet/balance.php and GET /api/reward/status.php?tx_ref= (endpoint
   names per the team's Integration Contract — confirm they match what Module 3b
   actually implemented before wiring up).
2. Add a coin/token balance display somewhere sensible (profile screen is the
   natural fit — coordinate with whoever owns that screen).
3. Add a "verifying on-chain..." → "blockchain-verified ✓" state transition for
   newly earned badges/rewards (Module 2 already stubbed a badge_tx_hash-aware UI
   state — extend that rather than building a parallel one).
4. Add an optional, non-blocking "view details" affordance showing the TxHash for
   a confirmed reward (a simple detail sheet is enough — don't build a blockchain
   explorer).

DO NOT touch: contract code, backend pipeline logic, game engine, gamification
calculation logic (only the display/state layer here).

Explain your plan first, then implement incrementally.
```

---

### Module 4 — Cultural Content Module
**Environment:** Android Studio (display) + VS Code (content/CMS side) · **Owner:** Kyla · **Support:** Yow (portal-side CMS UI)

```
You are working across the Kusina Kode Android app and backend to build/refine the
Cultural Heritage and Recipe content module — the reveal screen shown after a
correct puzzle guess, and the underlying dish dataset it draws from.

STEP 0 — EXPLORE FIRST: Read the existing "level" data model and the post-round
reveal UI. Summarize the current fields (word, trivia, image?) before proposing
changes.

CONTEXT FROM THE FINAL CAPSTONE MANUSCRIPT (this is the exact field set the
reveal screen must present after every completed round, per the Scope section):
- Historical background of the dish
- Regional origin
- Key ingredients
- Cooking procedure
- Required equipment and tools
- Cultural significance

This content must be expert-validated. The manuscript cites a real content
validation source: Chef Karen Nina B. Gicana's interview flagged that Mindanao
cuisine (Maranao, Tausug dishes) is underrepresented in existing culinary
education due to limited local ingredient availability — treat expanding regional
coverage (not just Luzon-centric dishes) as an explicit content priority, not an
afterthought.

SHARED DATA CONTRACT (match exactly — Modules 1, 2, and 5 all read this):
Level { level_id, word, trivia, history, region, ingredients[], procedure, tools[], image_url }

YOUR TASKS:
1. Confirm/expand the dish dataset schema to hold all six fields above per dish —
   Cosmos DB is the intended store for this document-style content per the
   manuscript's cloud design (coordinate with Module 6 on the actual connection
   once it exists; build against a clean repository interface either way so
   swapping the data source later doesn't require touching UI code).
2. Build/refine the post-round reveal UI (Android) to present all six fields
   clearly — this is graded content, keep it accurate and non-cluttered, not
   just "more text."
3. Expand endpoint GET /api/dish/content.php?level_id= (or confirm/extend the
   existing get_levels.php if that already returns enough of this) to return the
   full field set.
4. Curate/expand dish entries with attention to regional balance, per the content
   priority above — flag any dish where sourcing/validation is still pending
   rather than shipping unverified content.

DO NOT touch: game engine input/scoring logic, gamification points/badge logic,
blockchain pipeline, admin CRUD screens (Aly owns the CMS-side UI for this data —
you own the data/content and the player-facing display).

ACCEPTANCE CRITERIA (per the manuscript's Unit Testing section):
- Dish data retrieval returns complete, correctly-typed records for every field.
- Recipe display renders all six content fields correctly with no missing/garbled
  data for any dish in the dataset.

Explain your plan first, then implement incrementally.
```

---

### Module 5 — Admin Dashboard (Web)
**Environment:** VS Code · **Owner:** Yow (web management portal, per her Governance & System Protection lane)

```
You are working in VS Code on the Kusina Kode web-based Admin Dashboard, talking
to the same PHP REST API backend the mobile app uses.

STEP 0 — EXPLORE FIRST: Read the existing admin panel code (the old build had an
admin dashboard with real-time analytics, user CRUD with search, and level/content
management). Summarize what exists before changing anything — don't rebuild from
scratch if a working base is already there.

CONTEXT FROM THE FINAL CAPSTONE MANUSCRIPT: the Scope section defines exactly two
roles — Users/Players and Administrators — and gives Administrators "full access
to platform management, including the ability to add, edit, and delete game
content such as dish names and cultural trivia, oversee registered users, monitor
player activity and performance, and manage overall system configurations," PLUS
(new for this phase) the ability to "review blockchain reward records through the
integrated audit panel." Access must be enforced via Azure AD B2C role-based
access control (coordinate with Module 3b/6 on the auth flow) — an admin route
that's just hidden in the UI without a real backend role check is not acceptable
for this system.

YOUR TASKS:
1. Confirm/refine user management CRUD (create, read w/ search, update, delete) —
   this likely already exists in some form; don't duplicate, extend.
2. Confirm/refine level/dish content management CRUD, working against Module 4's
   Level schema (level_id, word, trivia, history, region, ingredients[], procedure,
   tools[], image_url) — the admin UI needs fields for all six cultural-content
   attributes, not just word + trivia.
3. Build the NEW blockchain audit panel: a table/view backed by
   GET /api/audit/log.php (filterable by user, event type, date, status) and a
   detail view backed by GET /api/audit/tx.php?hash= showing the full transaction
   record (event, user, amount/badge, TxHash, timestamp, status). These endpoints
   are being built by Module 3b — coordinate on exact response shape before hard-
   coding parsing logic.
4. Enforce role-based access at the backend (not just hiding UI elements) — every
   admin-only endpoint must reject non-Administrator tokens.
5. Add basic monitoring views if not already present: player activity, performance
   metrics — reuse existing analytics if the old dashboard already has this.

DO NOT touch: mobile app code, smart contracts, blockchain pipeline internals
(you consume its endpoints, you don't reimplement its logic), game engine.

ACCEPTANCE CRITERIA (per the manuscript's Unit Testing section):
- Content management: admin can add/edit/delete dish content correctly, changes
  reflected in what the mobile app fetches.
- User management: admin CRUD works correctly, including search.
- Audit log access: admin can view and filter blockchain reward records, and drill
  into a single transaction's full detail correctly.

Explain your plan first, then implement incrementally.
```

---

### Module 6 — Cloud Infrastructure & Security
**Environment:** Azure Portal / VS Code (IaC scripts if used) · **Owner:** Kyla (cloud architecture) · **Support:** Mau (cloud integration)

```
You are setting up the Kusina Kode cloud and security infrastructure on Microsoft
Azure, per the final capstone manuscript's Cloud Computing Infrastructure and
Cybersecurity Implementation Framework sections.

STEP 0 — EXPLORE FIRST: Check whether any Azure resources, ARM/Bicep/Terraform
templates, or .env/config files referencing Azure already exist in the repo.
Summarize before provisioning anything new (avoid creating duplicate resources).

TARGET SETUP (hybrid IaaS + PaaS, per the manuscript — don't substitute a fully-
managed PaaS-only approach, the paper is specific about this split):
- PHP REST API backend → Azure Virtual Machine (IaaS) — chosen specifically for
  custom server-level configuration that managed PaaS environments don't reliably
  support.
- Web-facing admin dashboard components → Azure App Service (PaaS) — automatic
  patching/scaling/load balancing.
- Azure SQL Database → structured relational data: user accounts, game progress,
  leaderboard standings.
- Azure Cosmos DB → flexible document-style content: dish trivia, recipes,
  cultural heritage information.
- Azure Blob Storage → binary assets: dish images, media files.
- Azure Key Vault → all encryption keys and secrets (nothing hardcoded or in
  plain .env committed to source control).
- Azure Active Directory B2C → authentication + role-based access control,
  strictly separating Administrator and Player roles.

SECURITY REQUIREMENTS TO CONFIGURE:
- TLS/SSL enforced for all traffic between the Android app / web dashboard and
  the backend.
- AES-256 encryption at rest for Azure SQL Database and Cosmos DB.
- Zero-trust API posture: every request re-verified regardless of origin; no
  implicit trust based on network location.
- Rate limiting and input validation at the API gateway/VM level.
- Minimal data exposure — audit what each endpoint returns and trim anything not
  needed by the calling role.

YOUR TASKS:
1. Provision the resources above (use Infrastructure-as-Code — Bicep or Terraform
   — if the team wants repeatable environments; otherwise document manual Portal
   steps clearly enough that another teammate could reproduce them).
2. Set up Key Vault and migrate any existing secrets/keys out of code/config into
   it; update the PHP backend and any deployment scripts to pull from Key Vault
   at runtime.
3. Set up Azure AD B2C with two roles (Player, Administrator) and document the
   auth flow clearly for Modules 3b and 5 to integrate against.
4. Set up the data partitioning: confirm which existing tables move to Azure SQL
   vs. which content types move to Cosmos DB, and provide connection strings
   (via Key Vault, not plaintext) to Module 3b and Module 4's owners.
5. Prepare the environment for the stress-testing pass described in the paper:
   the team will need to measure REST API response times, DB query performance,
   blockchain node transaction speed, and mobile rendering performance under
   concurrent load — make sure logging/monitoring (e.g., Azure Application
   Insights) is in place to actually capture these metrics later.

Document every resource you create (name, resource group, purpose, which module
depends on it) in a shared README so the rest of the team isn't guessing at your
setup.
```

---

### Module 7 — Testing, QA & Documentation
**Environment:** Cross-cutting · **Owner:** Yow (QA & test execution) · **Support:** Kyla (research & documentation), all

```
You are coordinating testing and documentation for Kusina Kode across mobile, web,
backend, and blockchain components, per the final capstone manuscript's Unit
Testing and Stress Testing section.

STEP 0 — EXPLORE FIRST: Check what test coverage (if any) currently exists across
the Android app and PHP backend before writing new tests.

CONTEXT — the manuscript defines the test plan explicitly; implement it as written,
don't invent a different structure:

UNIT TESTING — verify each module in isolation, Passed/Failed outcome, all
failures resolved before formal evaluation:
- Word Puzzle Game Engine: letter input validation, color-coded feedback logic,
  attempt tracking.
- Gamification Module: points calculation, badge awarding, leaderboard ranking.
- Blockchain Incentive Module: AES-256 encryption trigger, SHA-256 hash
  generation, ECDSA signature validation, smart contract execution.
- Cultural Content Module: dish data retrieval, recipe display.
- Administrative Dashboard: content management, user management, audit log
  access.

STRESS TESTING — evaluate performance/stability under high concurrent usage,
simulating multiple simultaneous users during evaluation:
- REST API response times under load.
- Database query performance under load.
- Blockchain node transaction processing speed under load.
- Mobile application rendering performance under sustained gameplay sessions.

YOUR TASKS:
1. Set up/confirm a test framework per platform (e.g., JUnit for Kotlin/Android,
   PHPUnit for the backend, Hardhat's test runner for contracts) if not already
   present.
2. Write/organize unit tests matching the exact breakdown above — coordinate with
   each module owner rather than writing tests blind against code you don't
   understand; pair with them if needed.
3. Set up a basic load-testing approach (e.g., k6, Apache JMeter, or a simple
   concurrent-request script) targeting the four stress-test dimensions above.
   Requires Module 6's monitoring (Application Insights or equivalent) to be in
   place to capture meaningful results.
4. Track a simple Passed/Failed log per module, and flag failures back to the
   owning teammate rather than silently fixing unfamiliar code.
5. Keep a running "manuscript claim vs. actual build" checklist — this project's
   biggest risk during defense is a gap between what Chapter 1–3 promised the
   panel and what the working system actually does. Flag any drift early.

This module doesn't own its own feature code — its job is making sure every other
module actually does what Chapters 1–3 say it does, and documenting evidence of
that (test logs, screenshots, short write-ups) for eventual inclusion in later
manuscript chapters.
```

---

## 9. A Few Things Worth Flagging to the Adviser Before Heavy Development

These are places where the manuscript is either genuinely ambiguous or represents a real change from the Capstone 1 build — better to confirm now than rebuild later:

1. **Exact token/badge contract design.** The paper describes "tokens" transferring to a user's wallet and separately discusses NFT-based achievement literature, but doesn't name specific contract types. The ERC-20 (points token) + ERC-721 (badges) split proposed in Module 3a is a reasonable engineering interpretation, not a literal requirement — confirm it matches what the panel expects for Capstone 2's implementation chapters.
2. **Variable word length.** The Capstone 1 build was fixed at 6 letters; the final manuscript's own example words are 5, 6, and 8 letters. This is a real functional change (Module 1), not cosmetic — worth a quick sanity check that this interpretation is correct before a large refactor.
3. **"MVVM with Clean Architecture" applied to the web admin dashboard.** Chapter 3's architecture description states both the Android app and the web dashboard are "built on MVVM with Clean Architecture." MVVM is a mobile-native pattern; if the web dashboard is plain PHP, the practical translation is a clean separation of concerns (routing/controller, business logic, presentation) rather than literal MVVM — flag this interpretation with the adviser if it comes up in defense questioning.

---

*This document should be pasted alongside — not instead of — your actual repository context in each Claude Code session. It captures what the manuscript committed to; it does not know your current codebase's exact file structure, which is why every prompt above starts with "explore first."*
