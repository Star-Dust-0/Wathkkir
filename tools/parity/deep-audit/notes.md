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
