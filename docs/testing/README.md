# Module 7 — test log and progress timetable

Two deliverables, both regenerated from the project rather than maintained by
hand:

| File | Built by | Contains |
|---|---|---|
| `KusinaKode_Test_Log.xlsx` | `npm run build:log` | Unit results, API checks, defect log, gaps |
| `KusinaKode_Timetable.xlsx` | `py build_timetable.py` | Progress Table, Progress dashboard, Integration and Unit — in the adviser's template format |

## The timetable

```bash
gradlew :app:testDebugUnitTest            # from KusinaKode/
cd docs/testing
py -m pip install openpyxl                # once
py build_timetable.py
```

Edit **`features.csv`** to change a feature's status, date, owner or remark;
everything else is derived. Status must be one of `OK`, `In Progress`,
`Not Started`, `Scope Change` — the dashboard counts those exact strings.

The `Progress` tab holds **formulas**, not computed numbers, so changing a
status in `Progress Table` updates the dashboard when Excel opens the file.

**It is a new workbook, not an edit of `IAN-RG_TIMETABLE_CHECKING`.** That file
contains another group's tracker (Fitness Buddy / a cardio health app) plus
sample templates, and its `Progress` formulas already carry `#REF!` errors.
Writing our data into it would destroy their work and inherit their breakage.
We copy the *format*, not the file.

---

# Test log

Produces `KusinaKode_Test_Log.xlsx`, the Passed/Failed record the manuscript's
Unit Testing section asks for, plus the defect log and the list of what is
still untested.

## Rebuilding it

```bash
./gradlew :app:testDebugUnitTest     # from KusinaKode/, produces the JUnit XML
cd docs/testing && npm install && npm run build:log
```

`npm install` is needed once, for `exceljs`. If you can't install it, the three
CSVs open in Excel directly — the workbook is a nicety, the CSVs are the record.

## What comes from where

| Sheet | Source | Edit by hand? |
|---|---|---|
| 1. Summary | Computed from the other sheets | No |
| 2. Unit Tests | **Parsed** from `app/build/test-results/testDebugUnitTest/*.xml` | **No** |
| 3. API & Security Tests | `api-tests.csv` | Yes |
| 4. Defect Log | `defects.csv` | Yes |
| 5. Not Tested — Gaps | `gaps.csv` | Yes |

Sheet 2 is never typed in. It is read from what the suite actually did, so it
cannot drift from reality — if a test fails, the sheet says FAIL. That is the
sheet to trust when someone asks whether the tests really pass.

The other three are judgement calls a person has to record. Open the CSV in
Excel, add a row, save as CSV, rerun the build.

## Who owns this

Per the signed Capstone 1 roles: **Yow** is QA Tester and owns test execution
and defect logging; **Kyla** is Research & Documentation and owns the write-up
and the manuscript-claim reconciliation. The CSVs are deliberately editable in
Excel so neither has to touch the script.

## Keeping the defect log honest

Add a row to `defects.csv` whenever a defect is **found**, not when it is
fixed — a defect found and fixed in the same hour still belongs in the record.
Status is `Fixed` or `OPEN`.

The **How found** column is the one that earns its place. So far most defects
came from play testing rather than from the test suite, and that is the
evidence for what the gaps sheet claims. Don't quietly write "unit test" for
something a person noticed by using the app.

## Conventions

- IDs are sequential: `D-18`, `G-11`, and so on. Don't renumber existing rows —
  commits and discussions reference them.
- Dates are `YYYY-MM-DD`.
- Fields containing a comma must be `"quoted"`. Excel does this for you.
- `Commit` is the short hash of the fix, or `—` while it is open.
- A row is never deleted. If something turns out not to be a defect, set the
  status and say so in the fix column.
