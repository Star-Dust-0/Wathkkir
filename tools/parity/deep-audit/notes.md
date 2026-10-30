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
