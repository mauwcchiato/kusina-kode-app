#!/usr/bin/env node
/**
 * Builds KusinaKode_Test_Log.xlsx for Module 7.
 *
 * Two kinds of input, deliberately kept apart:
 *   - Unit results are PARSED from Gradle's JUnit XML. Never hand-edited, so
 *     the sheet cannot drift from what the suite actually did.
 *   - Everything else lives in the CSVs beside this file. Those are the
 *     hand-kept logs; edit them in Excel, no JavaScript involved.
 *
 * Usage:  npm install  &&  npm run build:log
 * See README.md.
 */
const ExcelJS = require('exceljs');
const fs = require('fs');
const path = require('path');

const HERE = __dirname;
const RESULTS_DIR = process.env.KK_TEST_RESULTS ||
  path.join(HERE, '..', '..', 'app', 'build', 'test-results', 'testDebugUnitTest');
const OUT = path.join(HERE, 'KusinaKode_Test_Log.xlsx');

// ---------------------------------------------------------------- CSV input
/** Minimal RFC-4180 reader — handles quoted commas and embedded newlines. */
function parseCsv(text) {
  const rows = [];
  let row = [], field = '', inQuotes = false;
  for (let i = 0; i < text.length; i++) {
    const c = text[i];
    if (inQuotes) {
      if (c === '"') {
        if (text[i + 1] === '"') { field += '"'; i++; } else { inQuotes = false; }
      } else field += c;
    } else if (c === '"') inQuotes = true;
    else if (c === ',') { row.push(field); field = ''; }
    else if (c === '\n') { row.push(field); rows.push(row); row = []; field = ''; }
    else if (c !== '\r') field += c;
  }
  if (field !== '' || row.length) { row.push(field); rows.push(row); }
  return rows.filter(r => r.some(c => c.trim() !== ''));
}

function readCsv(name) {
  const file = path.join(HERE, name);
  if (!fs.existsSync(file)) throw new Error(`Missing ${name} — it belongs next to this script.`);
  const rows = parseCsv(fs.readFileSync(file, 'utf8'));
  return { header: rows[0], body: rows.slice(1) };
}

// ------------------------------------------------------------- JUnit input
/** Which module each test class belongs to, and the criterion it proves. */
const CLASS_META = {
  WordleEngineTest:           ['Module 1 — Word Puzzle Engine',  'Letter input validation; colour-coded feedback logic'],
  GameViewModelTest:          ['Module 1 — Word Puzzle Engine',  'Attempt tracking; round state'],
  ScoreRulesTest:             ['Module 2 — Gamification',        'Points calculation correct and consistent'],
  BadgeRulesTest:             ['Module 2 — Gamification',        'Badge awarding exactly at milestone'],
  LeaderboardRulesTest:       ['Module 2 — Gamification',        'Leaderboard ranking accurate'],
  ProgressRulesTest:          ['Module 2 — Gamification',        'Progress tracking; points consistency'],
  PowerUpRulesTest:           ['Module 2 — Gamification (ext.)', 'Power-up pricing and bomb behaviour'],
  IslandRulesTest:            ['Module 2 — Gamification (ext.)', 'Island complete KK eligibility'],
  KusinaShopTest:             ['Module 2 — Gamification (ext.)', 'Shop catalogue lockstep with PHP prices'],
  GamificationCoordinatorTest:['Module 2 — Gamification',        'All three criteria, integrated'],
  ErrorMappingTest:           ['Mobile — error handling',       'Failures classified, no raw exception text shown'],
  ExampleUnitTest:            ['— scaffold',                     'Android template test'],
};

function parseUnitResults(dir) {
  if (!fs.existsSync(dir)) {
    throw new Error(
      `No test results at ${dir}\n` +
      `Run the suite first:  ./gradlew :app:testDebugUnitTest`
    );
  }
  const rows = [];
  for (const file of fs.readdirSync(dir).filter(f => f.endsWith('.xml'))) {
    const xml = fs.readFileSync(path.join(dir, file), 'utf8');
    const suite = (xml.match(/<testsuite name="([^"]+)"/) || [])[1] || file;
    const short = suite.split('.').pop();
    const re = /<testcase name="([^"]+)"[^>]*time="([^"]+)"\s*(\/)?>/g;
    let m;
    while ((m = re.exec(xml)) !== null) {
      let result = 'PASS';
      if (!m[3]) {
        const body = xml.slice(m.index, xml.indexOf('</testcase>', m.index));
        if (body.includes('<failure')) result = 'FAIL';
        else if (body.includes('<skipped')) result = 'SKIPPED';
      }
      const [mod, crit] = CLASS_META[short] || ['—', '—'];
      rows.push({ mod, suite: short, name: m[1].replace(/"$/, ''), crit, result, time: parseFloat(m[2]) });
    }
  }
  rows.sort((a, b) => a.mod.localeCompare(b.mod) || a.suite.localeCompare(b.suite));
  return rows;
}

// ------------------------------------------------------------------ styling
const BRAND = 'FFCC6B1F', DARK = 'FF3E2723';
const PASS_BG = 'FFE3F2E1', FAIL_BG = 'FFFBE0E0', OPEN_BG = 'FFFDF0D5', ZEBRA = 'FFFAF6EE';

function tint(cell) {
  const v = String(cell.value ?? '').toUpperCase();
  if (v === 'PASS' || v === 'FIXED') cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: PASS_BG } };
  else if (v === 'FAIL') cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: FAIL_BG } };
  else if (v === 'OPEN' || v === 'SKIPPED' || v.startsWith('NO ') || v.startsWith('NOT '))
    cell.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: OPEN_BG } };
  else return;
  cell.font = { bold: true, size: 10 };
}

function addSheet(wb, name, columns, rows, statusCols = []) {
  const ws = wb.addWorksheet(name, { views: [{ state: 'frozen', ySplit: 1 }] });
  ws.columns = columns;
  const head = ws.getRow(1);
  head.eachCell(c => {
    c.font = { bold: true, color: { argb: 'FFFFFFFF' }, size: 11 };
    c.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: BRAND } };
    c.alignment = { vertical: 'middle', wrapText: true };
    c.border = { bottom: { style: 'thin', color: { argb: DARK } } };
  });
  head.height = 28;
  rows.forEach(r => ws.addRow(r));
  ws.eachRow((row, i) => {
    if (i === 1) return;
    row.alignment = { vertical: 'top', wrapText: true };
    if (i % 2 === 1) row.eachCell(c => { c.fill = { type: 'pattern', pattern: 'solid', fgColor: { argb: ZEBRA } }; });
    statusCols.forEach(c => tint(row.getCell(c)));
  });
  ws.autoFilter = { from: { row: 1, column: 1 }, to: { row: 1, column: columns.length } };
  return ws;
}

const cols = (specs) => specs.map(([header, width]) => ({ header, width }));

// --------------------------------------------------------------------- main
function main() {
  const unit = parseUnitResults(RESULTS_DIR);
  const api = readCsv('api-tests.csv');
  const defects = readCsv('defects.csv');
  const gaps = readCsv('gaps.csv');

  const statusCol = (header, def) => {
    const i = header.findIndex(h => h.trim().toLowerCase() === def);
    return i === -1 ? [] : [i + 1];
  };

  const wb = new ExcelJS.Workbook();
  wb.creator = 'Kusina Kode team';
  wb.created = new Date();

  // Sheet 1 — Summary, derived from everything else.
  const byModule = {};
  unit.forEach(r => {
    (byModule[r.mod] ||= { t: 0, p: 0, f: 0 }).t++;
    if (r.result === 'PASS') byModule[r.mod].p++; else if (r.result === 'FAIL') byModule[r.mod].f++;
  });
  const apiResult = statusCol(api.header, 'result')[0];
  const apiPass = api.body.filter(r => r[apiResult - 1] === 'PASS').length;
  const defStatus = statusCol(defects.header, 'status')[0];

  const summary = Object.entries(byModule).map(([m, s]) =>
    [m, 'Unit (JUnit)', s.t, s.p, s.f, s.f === 0 ? 'PASS' : 'FAIL']);
  summary.push(['Backend REST API — authentication', 'Manual (curl, live server)',
    api.body.length, apiPass, api.body.length - apiPass, apiPass === api.body.length ? 'PASS' : 'FAIL']);
  summary.push([]);
  summary.push(['TOTAL AUTOMATED UNIT TESTS', '', unit.length,
    unit.filter(r => r.result === 'PASS').length, unit.filter(r => r.result === 'FAIL').length, '']);
  summary.push(['DEFECTS LOGGED', '', defects.body.length,
    `${defects.body.filter(r => r[defStatus - 1] === 'Fixed').length} fixed`,
    `${defects.body.filter(r => r[defStatus - 1] === 'OPEN').length} open`, '']);

  const s1 = addSheet(wb, '1. Summary', cols([
    ['Module / Area', 38], ['Test type', 26], ['Tests', 9], ['Passed', 11], ['Failed', 11], ['Result', 12],
  ]), summary, [6]);
  s1.spliceRows(1, 0,
    ['Kusina Kode — Test Log'],
    [`Generated ${new Date().toISOString().slice(0, 10)} · unit results parsed from Gradle's JUnit XML; other sheets from the CSVs beside the script`],
    []);
  s1.getCell('A1').font = { bold: true, size: 16, color: { argb: DARK } };
  s1.getCell('A2').font = { italic: true, size: 10, color: { argb: 'FF6B5B4D' } };
  s1.mergeCells('A1:F1'); s1.mergeCells('A2:F2');

  // Sheet 2 — every unit test, straight from the XML.
  addSheet(wb, '2. Unit Tests', cols([
    ['#', 6], ['Module', 30], ['Test class', 28], ['Test case', 62],
    ['Acceptance criterion', 40], ['Result', 10], ['Time (s)', 10],
  ]), unit.map((r, i) => [i + 1, r.mod, r.suite, r.name, r.crit, r.result, r.time]), [6]);

  // Sheets 3-5 — the hand-kept CSVs, rendered as-is.
  const widthsFor = (header) => header.map(h => Math.min(64, Math.max(12, h.length + 22)));
  addSheet(wb, '3. API & Security Tests',
    cols(api.header.map((h, i) => [h, widthsFor(api.header)[i]])), api.body,
    statusCol(api.header, 'result'));
  addSheet(wb, '4. Defect Log',
    cols(defects.header.map((h, i) => [h, widthsFor(defects.header)[i]])), defects.body,
    statusCol(defects.header, 'status'));
  addSheet(wb, '5. Not Tested — Gaps',
    cols(gaps.header.map((h, i) => [h, widthsFor(gaps.header)[i]])), gaps.body,
    statusCol(gaps.header, 'status'));

  return wb.xlsx.writeFile(OUT).then(() => {
    const fails = unit.filter(r => r.result === 'FAIL').length;
    console.log(`Wrote ${path.relative(process.cwd(), OUT)}`);
    console.log(`  unit    ${unit.length} tests — ${unit.length - fails} pass, ${fails} fail`);
    console.log(`  api     ${api.body.length} checks`);
    console.log(`  defects ${defects.body.length} (${defects.body.filter(r => r[defStatus - 1] === 'OPEN').length} open)`);
    console.log(`  gaps    ${gaps.body.length}`);
  });
}

main().catch(e => { console.error(`\n${e.message}\n`); process.exit(1); });
