# Kusina Kode — instructions for AI coding assistants

Read this first. It applies to **any** assistant working in this repo — Cursor,
Claude Code, Copilot, Codex — and it is version-controlled so it travels with
the project.

**Where the work currently stands: [`docs/STATUS.md`](docs/STATUS.md).** That
file is generated, so it is accurate rather than remembered. Read it before
planning anything.

---

## 1. Standing instruction: keep the tracking current

This project is graded partly on evidence of progress, so the logs are a
deliverable, not bookkeeping. **After any substantive change, update the
relevant CSV in `docs/testing/` and rebuild.**

| You did this | Update this |
|---|---|
| Finished, started, or changed the scope of a feature | `docs/testing/features.csv` |
| Found or fixed a bug | `docs/testing/defects.csv` |
| Tested an endpoint or a security property by hand | `docs/testing/api-tests.csv` |
| Noticed something with no test coverage | `docs/testing/gaps.csv` |

Then regenerate both workbooks:

```bash
cd docs/testing
npm run build:log        # KusinaKode_Test_Log.xlsx
py build_timetable.py    # KusinaKode_Timetable.xlsx
```

A `pre-commit` hook rebuilds them for you and warns when code changed but
`features.csv` did not. It cannot decide *for* you whether a feature is now
done — that judgement is yours, and skipping it is how the tracker rots.

**Rules for the logs:**
- Log a defect when it is **found**, not when it is fixed. One found and fixed
  in the same hour still belongs in the record.
- Never renumber IDs (`D-01`, `G-01`, `U-001`). They are referenced elsewhere.
- Be truthful in the **How found** column. Most defects so far came from
  play testing, not from the test suite, and that honesty is the only evidence
  behind the "not tested" sheet. Never write "unit test" for something a
  person noticed by using the app.
- `features.csv` Status must be exactly `OK`, `In Progress`, `Not Started`, or
  `Scope Change` — the dashboard counts those strings.

**Do not write into `IAN-RG_TIMETABLE_CHECKING*.xlsx`.** That is the adviser's
shared file containing another group's tracker and sample templates. We copy
its format into our own workbook; we never edit it.

---

## 2. Building and testing

```bash
# from the repo root
gradlew :app:testDebugUnitTest
```

On the project owner's machine `JAVA_HOME` is not set and there is no
standalone JDK — prefix with Android Studio's bundled runtime:

```bash
JAVA_HOME="/c/Program Files/Android/Android Studio/jbr" ./gradlew :app:testDebugUnitTest
```

Gradle prints only `BUILD SUCCESSFUL` without a test count. To confirm tests
actually ran, read `app/build/test-results/testDebugUnitTest/*.xml`.

Plain `python` hits a Microsoft Store stub on Windows here. Use `py`, or the
full path to `python.exe`.

---

## 3. Architecture rules that are not negotiable

- **MVVM + Clean Architecture.** ViewModels expose `StateFlow`; composables are
  stateless and take state plus callbacks. No network or database calls from a
  composable.
- **Variable word length.** The puzzle grid must support 5, 6, 8+ letters.
  Never reintroduce a fixed 6×6 grid.
- **Attempts are a flat 6** for every dish, by team decision, even though an
  early feature list said attempts scale with word length.
- **Players never see** a seed phrase, private key, gas fee, or wallet-connect
  prompt. Wallets are custodial and server-side. Blockchain UI is read-only.
- **Identity comes from the credential, never the payload.** API endpoints
  derive the user from the bearer token and ignore any `user_id` in the
  request body. Reintroducing a body-supplied `user_id` re-opens a critical
  vulnerability (see defect D-14).
- **The relational database holds no blockchain internals** — only pointer
  fields (`wallet_address`, `tx_hash`, `badge_id`). The chain is the source of
  truth for reward legitimacy.
- **Don't hardcode secrets or hosts.** Backend config comes from the
  environment via `api/config.php`; this is the seam Azure Key Vault plugs into.

## 4. Before making changes

Explore the actual code first. File names and screens do not always match what
the design documents describe. Summarise what you find before proposing
changes, and do not assume cloud endpoints exist — the backend is still local
XAMPP.

## 5. Module ownership

From the **signed Capstone 1 Roles and Responsibilities page**, which is the
authority — not any assignment inferred from the code:

| Member | Owns |
|---|---|
| **Mau** (Project Manager) | Game engine, level & progression, hint system, progress tracking, cloud integration, cross-module integration |
| **Kyla** (System Analyst / Research & Documentation) | Cloud architecture, cultural content view, analytics dashboard view, documentation |
| **Yow** (UI/UX Designer & QA Tester) | Web management portal, system settings, activity monitoring, audit panel, QA & test execution |
| **Alyssa** (Developer & Technical Lead) | Blockchain implementation, rewards & achievements engine, identity & user management |

Two areas are **built but unassigned** — the mobile auth screens and the
onboarding/tutorial. They are flagged `UNASSIGNED` in `features.csv` pending a
team decision. Don't quietly claim them for a module.

### Working split (agreed 2026-08-17)

**Mau and Alyssa focus on mobile; Yow and Kyla focus on web.** That is a focus,
not a wall, and three things deliberately sit outside it:

- **Alyssa owns the server-side blockchain work** — the Solidity contracts and
  the four-phase PHP pipeline — because it is her signed module, even though it
  is not mobile. It is also the largest remaining block of work.
- **Alyssa owns the shared PHP REST API.** Most of it is auth, progress and
  badges, all in her lane. Without one owner this folder stalls: defect D-17
  is a mobile-side security fix that cannot land until Yow's dashboard sends a
  token.
- **Kyla's cultural content lands in a mobile screen** (the KODEX / dish detail
  page). She produces the validated data; getting it into the app is a mobile
  job, so that hand-off needs coordinating rather than assuming.

`features.csv` has a **Platform** column (`Mobile`, `Web`, `Backend`,
`Backend / Chain`, `Cloud`, `Content`, `Both`) so you can see at a glance where
a feature actually runs versus who owns it. Of 8 modules only 3 are cleanly one
platform, which is why the column exists.

Full task briefs and acceptance criteria:
`docs/KusinaKode_Master_Dev_Prompt_and_Module_Assignments.md`.

## 6. Shared data contract

Field names must match the backend and web team exactly:

```
User    { user_id, name, email, role[player|admin], wallet_address, created_at }
Attempt { attempt_id, user_id, level_id, is_correct, time_taken_ms, timestamp }
Level   { level_id, word, trivia, history, region, ingredients[], procedure, tools[], image_url }
Badge   { badge_id, user_id, badge_type, milestone_criteria, tx_hash, awarded_at }
```

## 7. First-time setup on a new machine

The hooks live in a tracked directory, but git needs telling once per clone:

```bash
git config core.hooksPath .githooks
```

Without that, the workbooks will not rebuild automatically and commits will not
be checked.
