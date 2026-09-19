# Report-an-issue: mobile handoff

Backend + admin dashboard for the user report system are already built and
verified (curl + live browser testing) on the web side. This note is for
whoever picks up the two mobile files below — they were written and reviewed
by hand, but **never compiled**, because this machine has no working JDK 11+
for Gradle (only a Java 8 JRE, one broken Android Studio JBR, and one JDK 25
JBR that fails every Gradle invocation with `Unable to establish loopback
connection` — daemon, no-daemon, sandboxed, unsandboxed, all the same
result). That's an environment problem on this machine, not necessarily a
code problem, but it means **this code has zero build verification**. Please
open it in Android Studio and confirm it compiles and runs before assuming
it works.

## What changed

- `app/src/main/java/com/example/kusinakode/KusinaApi.kt`
  - Added `ReportReason` (three constants: `WRONG_AMOUNT`, `NOT_RECEIVED`,
    `OTHER`), `ReportSubmitRequest`, `ReportSubmitData`, `ReportSubmitResponse`.
  - Added `KusinaApi.submitReport(txRef, reason, message)` — `POST
    report/submit.php`, same `expectSuccess = false` + status-check pattern
    as `pantryDraw`/`pantrySell` in the same file.
- `app/src/main/java/com/example/kusinakode/ui/components/RewardReceipt.kt`
  - Added a "Report an issue with this reward" link inside the existing
    receipt dialog. Tapping it expands the same dialog into a reason picker
    (radio rows) + optional details field + Cancel/Submit, calling
    `KusinaApi.submitReport` and showing a confirmation or inline error.
  - Nothing else needed changing — `RewardsHistoryScreen.kt` and
    `NotificationsScreen.kt` both call the same `RewardReceipt.show(...)`
    singleton, so the new button reaches all three entry points for free.

No other mobile files were touched.

## Server contract (already live, already tested)

`POST report/submit.php`, bearer token required:

```json
// request
{ "tx_ref": "kk_...", "reason": "wrong_amount" | "not_received" | "other", "message": "optional, ≤500 chars" }

// success (matches ReportSubmitResponse)
{ "status": "success", "data": { "id": 123 } }

// error (400/401/404) — same shape KusinaApi throws error(message) from
{ "status": "error", "message": "..." }
```

The server resolves `tx_ref` itself and 404s (with an identical message) if
it doesn't belong to the caller — the app never needs to know the difference
between "not found" and "not yours."

## To verify on your machine

1. Open the project in Android Studio (bundled JBR should just work there,
   unlike this shell).
2. Build the `app` module — confirm `KusinaApi.kt` and `RewardReceipt.kt`
   compile clean.
3. Trigger any reward that shows the receipt dialog (e.g. finish a level,
   claim a daily), tap "Report an issue with this reward," pick a reason,
   submit.
4. Confirm the new row shows up in the web admin's **Reports** tab
   (`AdminDashboard.aspx`, requires an admin with the "Reports" permission
   section) and that its embedded receipt matches what the app showed.
5. Sanity-check the reason labels — they're `wrong_amount` / `not_received`
   / `other` in the DB and in `KK_REPORT_REASONS`
   (`api/lib/reports.php`) — if these ever need to change, that constant,
   the mobile `ReportReason` object, and two spots in `AdminDashboard.aspx`
   / `.aspx.cs` (`ddlReportsReason`, `ReportReasonLabel()`) all need to
   move together.

## Explicitly out of scope for this pass

"My Reports" (a player-facing screen to see resolution status) was
deliberately skipped for v1 — agreed as a later, real screen with no
existing scaffolding to reuse.
