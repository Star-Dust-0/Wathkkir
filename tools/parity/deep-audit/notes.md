# Findings
## Confirmed repairs pending focused evidence
Shared dialog Oct20/23 surface/wrapper/staging omitted; font/theme localization, Language staged Apply/Cancel; reminderAM/PM; StatisticsToday; lock-help/About strings. Commit496704c phone/host compiled.2011Drive backup.
## Further visual source-backed gap
Paired Tasbeeh source screenshot shows original solid surface versus recovered frosted circle (black interior/hairline border). Oct19 source operation explicitly reverted TasbeehScreen to v3.1.0 after earlier frost experiment; replay missed. Restored clip(CircleShape).background(cs.surface), local phone compile passed20:12. Source not yet pushed/backed up as of this note.
## Motion evidence
Contact sheets of broad-run recordings inspected20:11 show screen movement and drawer fade, but sparse sampling cannot prove easing/duration equality. Need align actual action onset and compare dense frames, not total clip duration (launch/inspection overhead differs). Original and comparison Tasbeeh increment/target/reopen state behaviors matched selected scenarios. No claim of all press behaviors.

## Oct26 watch source recovery, Oct30 20:28
Recovered original Oct26 application-source records confirm native TimeDifferenceComplicationText STOPWATCH with target-60000ms, minimum MINUTES, displayAsNow false, per-target boundary alarms, midnight date updates, location/language/time/boot refresh. Native countdown text is original functionality. Oct26 original had static ranged ring with request-driven minute pulse; current comparison has later dynamic ring/no recurring pulse. Parent asked whether strict Oct26 supersedes later ring/battery changes; affected restore held only.
Found missing exact Oct26 Settings.saveLocMode requestAllComplications refresh and restored it. Existing saveGps, Loc.save, LocationSync.save and BootReceiver refresh present. Full watch behavior remains unproven; module not claimed complete.

## Corrected provenance20:30
Original watch recovered from owner's existing personalDrive upload, package com.wathakker.app1.1.2/code73, SHA2563f3bb7e107f6e414567624a8a4e97d7cb89b714f3b4e93591cb891ab7289b545. Owner confirmed retaining later dynamic-ring/no-minute-pulse improvements, original message Oct30 20:29 ending...QUM5MjE2NkFFM0UzMDA1QzU0NDIwNzlCNzFGQ0I1RUUA.
Location-mode refresh described in Oct26 patch was never written to file in that operation; shipped APK confirms original only saved mode. Added refresh is an owner-approved repair/improvement, not byte-exact shipped restoration. Reported correction to parent.
New confirmed source replay omission: Settings.patterns retained removed option3 from older source and lacked invalid-pattern fallback, while patName has only five English entries. Original Oct21 printed source and Oct26 paired APK kg1 both have IDs1,8,9,10,11 with fallback1. Restored exact original list/fallback to prevent sixth-option English out-of-range and wrong label mapping. Validation pending.

## Phone focused batch41104578918 finished20:42
143checks pass;27/32paired scenes have identical text tokens. Five differences are settings background scroll positions after Cancel, not proof of functional mismatch or match. Actual EN/AR dialog image sheets inspected: restored plain surfaces, title/button geometry and localized content now visually align. Lock-help AR entire image exactly equal in this capture; EN differs188pixels(status clock). AR font differs349pixels mostly status clock. Other snapshots have large blurred-background differences from nondeterministic swipe/scroll. Need deterministic scroll-state measurement and dense motion analysis, not broad masks. Still not full parity. Appearance picker now genuinely opened, Cancel tests recorded. No claim all dialogs exact.

## 2026-10-30 21:02 localization recovery
- The literal Oct17 bulk-localization operation covered Dialogs.kt and MoreScreen.kt. These edits were omitted from the rebuild although L10n tables and enum were present. Replayed existing exact AR-key mappings for those files; ReminderTime selected explicitly over the later AlertTiming alias. Restored lock widget toast key use in SettingsScreen.
- Local phone debug build passed with verification module. Runtime CRUD/Apply/Cancel/persistence and screenshot comparison remain unproven after this source repair. Compilation is not a parity verdict.

## 2026-10-30 21:05 full Oct19 revert audit
- Recovered Oct19 literal revert names six files: WheelTimePicker, OneUIDialog, DashboardScreen, HijriCalendarScreen, HomeScreen, TasbeehScreen. The Tasbeeh-only fix did not exhaust that operation.
- Current HomeScreen inner dhikr surface and DashboardScreen highlight still held the reverted frost material. WheelTimePicker selection also still had reverted translucent/hairline material. Restored the exact pre-change expressions shown in the original Oct19 diff; later Oct20-23 inspected history had no reapplication of these changes.
- Calendar is now the later Oct23 segmented-control implementation rather than the former inline pill, so no blind rollback of calendar code was made. Dialog already separately restored its later final geometry. Local phone build passes; actual paired stateful screenshots remain required.
- Added phone_stateful.py for isolated emulator create/cancel/save/edit/favorite/reminder/tag/picker persistence evidence, including direct DB snapshots after process stop. Harness syntax checked only, not yet run. No extra CI dispatched while watch batch pending.

## 2026-10-30 21:18 watch batch41109463471 inspection
- Completed455checks,451passed;4early original AR foreground failures showed Wear initial Starting/permission startup, not a demonstrated product discrepancy. 152paired scenes,109equal semantic tokens,78exact PNG matches.
- Inspected actual paired menu, fonts, theme, prayer list/main, tile and ranged images in contact sheet. Many settings screens match exactly; prayer times differ by2hours.
- Logs explicitly show Wear TimeZoneMediator changing original run timezone from NITZ to UTC despite raw Cairo property, while later comparison context differs. This invalidates prayer-time comparison in this batch; do not change prayer calculations. Rerun uses explicit alarm timezone command, broadcast, airplane mode and environment snapshots, deliberately UTC for deterministic app isolation. Cairo DST remains a separate later case.
- Original real provider uses STOPWATCH per c()=1. Original PreviewComp helper s21 defaults to SHORT_DUAL_UNIT unless cd_style=stopwatch, whereas recovered helper defaults to real provider. Thus preview ranged2m vs0:02 is a harness mismatch, not an original provider text difference. Pass stopwatch explicitly in paired rerun.
- Added class attribute to hierarchy reader, required for stateful EditText selection. Host build passes. Existing phone stateful harness would otherwise have recorded field failures, now fixed before launch.
- behavior.yml dispatch returned404 because workflow is not registered on main. No run created. Use existing registered checkpoint.yml on comparison branch only, as with previous focused batches; no main registration change.

## 2026-10-30 21:35 phone harness startup
- Run41114230864 failed before emulator: missing ephemeral debug.keystore. Application Kotlin/Java compilation finished, signing validation stopped packaging. No runtime checks or parity evidence were produced.
- Restored test-only keystore generation in the phone workflow, matching the previously successful checkpoint. This is the standard disposable Android debug identity, not the owner's signing key. Also set headless JVM to avoid irrelevant KSP AWT event-thread errors. One corrected run only after failed run completion.

## 2026-10-30 21:51 runtime harness obstruction
- Phone41116393024 infrastructure green but only11checks ran: four Azkar opens failed because identical system Pixel Launcher ANR dialogs blocked both apps in both languages. XML and actual screenshot are the source, not an app parity mismatch. Four database baselines each contain the same five starter rows. No CRUD claimed.
- Restore prior harness launcher force-stop/error-dialog suppression and detect/dismiss launcher error on launch. Script failures remain evidence; CI green is never treated as test pass.
- Actual failed screenshot also exposed an underlying notification runtime permission dialog. pm clear resets installation-time grants, so explicitly regrant notifications and location after each isolated-data reset for CRUD suite (permissions themselves belong to a separate test matrix). Prepared before next run; do not claim current in-flight run includes this fix.

## 2026-10-30 22:53 stateful phone
- Corrected run41120585766 finished195checks,0failures. Runtime create/cancel/save/edit/favorite/reminder/tag and font/theme cancel/apply persistence exercised in EN/light and AR/dark, original vs rebuilt, with DB snapshots and actual images. This only proves the named finite cases, not all scenarios or visuals.
- Move registered checkpoint to the prepared controlled watch rerun after phone completion. No parallel batch.

## 2026-10-30 23:10 phone visual residuals
- Stateful32pairs:11full PNGs exact. Other tiny residuals include status clock, input caret and navigation gesture animation, need precise classification. Appearance backgrounds differ from scrolling while title/button/text bounds match. Do not hide those differences with masking.
- Actual EN/AR item-overflow popup images show a genuine offset error: recovered popup below anchor, original overlapping anchor and closer to screen edge. Exact Oct20 source operation adds DpOffset(14.dp, -44.dp), omitted in replay. Restored that exact operation; phone build passes, runtime recheck pending.

## 2026-10-30 23:25 controlled watch result and tile source recovery
- Run41126654104 completed455checks with1original first-launch foreground miss;148/152token pairs equal,119/152fullPNG exact. Explicit UTC environment captured for all4sessions. EN main/list prayer-time values and native STOPWATCH now match. Actual screenshot sheet inspected; first original AR frame captured watchface, not app, remains unproven startup.
- Tile has genuine geometry mismatch despite equal strings. Fresh original Oct26 11:44 full Tile.kt print and shipped APK both show13dp arc,18dp top padding,gaps2.5/4.5/4/2. Older replay had16dp,16.5dp,gaps5/9.7/8/3.3. Restored exact original constants, keeping approved native dynamic ring changes untouched. Wear compile passed; paired updated tile screenshot still required.
- Same exact Oct26 full print also includes6dp outer padding and two overlaid arcs; restored both rather than stopping at text constants. Native flag still controls dynamic length/text, not geometry. Legacy fallback now uses original two-arc structure.

## 2026-10-30 23:42 focused tile result
- Run41130875638:15checks0failures,6/6equal semantic pairs. Actual settled main and ranged screenshots exactly identical in both languages. Tile text/spacing now aligned; each tile pair still differs2346pixels, visually in arc end/track rendering. Keep as unresolved, not exact parity. Native dynamic changes are approved but fallback visual must still be accounted for.
- Next batch focuses phone popup repaired offset and observable settings scroll history, not another whole CRUD repetition.

## 2026-10-30 23:58 phone popup/scroll result
- Run41132826977 completed27checks0failures. Actual popup geometry now aligned in both languages after recovered offset. EN/AR popup residual690/710pixels mostly system clock/gesture areas, not a blanket exact-image claim.
- Recorded pre/post swipes establish differing intermediate settings scroll offsets under the same gesture. At absolute list end, screenshot geometry/text matches; appearance-picker end snapshots residual579/840pixels instead of >1million. This isolates earlier large picker discrepancies to different backgrounds from intermediate scroll positions, not dialog geometry. Cause of differing scroll trajectory remains unproven (timing/gesture vs product physics); preserve evidence and later motion study.
- Tile residual is thin arc-edge rasterization throughout arc, not spacing/text;2346pixels per locale, pending renderer/source-structure check. Do not silently label it approved dynamic deviation.
- Next targeted utilities batch: Tasbeeh increment/target/session/restart/reset, converter directions/Today, Hisn default/All/Favorites/search. Newly selected labels checked against exact L10n table before dispatch.

## 2026-10-31 00:15 utility behavioral repairs
- Run41134747866:67checks pass but only23/26semantic pairs equal. Actual Tasbeeh increment/restart/reset surfaces align. Original Hisn defaults to Favorites while rebuild defaulted All, in both languages, with identical user data. Restore initial onlyFavorites=true. Exact old source operation not recovered in current history scan; this small repair is explicitly APK-behavior grounded, not a claim of recovered literal source.
- Arabic converter Today output used Hindi numerals despite saved Latin setting, whereas original consistently used Latin. Apply existing Digits.ui to ConverterResult date text. No date arithmetic change. Local build passes; both repairs need runtime rerun.
- Hisn search keyboard frames can differ due input focus/caret timing; not yet a bug verdict. Hisn chapter actions, search content/favorites sorting, date boundary/input validation and motion remain untested here.

## Original Oct20 global DigitText import operation recovered
- Source transcript builder20 row1090 writes Digits/DigitText and explicitly swaps every exact material3.Text import to com.example.ui.oneui.Text. Later row1267 and Oct23 builder settings row87 confirm only DigitText M3Text alias and deliberate settings-selector fully-qualified exceptions remained. Recovery had omitted this global loop, leaving30imports unconverted. Replay the exact loop against recovered tree, superseding localized ConverterResult Digits.ui repair. This matters beyond converter: Hindi-digit mode must be audited across phone surfaces.
- Source artifacts `/tmp/digit-recovery-builder20.json-1090.txt` and `/tmp/digit-recovery-oct23-builder-settings.json-87.txt` are internal provenance only, not deliverable source files.
