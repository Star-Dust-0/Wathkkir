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

## Watch original day anchor discrepancy
- Original shipped Oct26 watch decompile defpackage/d21.java c(context,dayOffset,now) sets HOUR_OF_DAY12,MINUTE0,SECOND0,MILLISECOND0 after adding dayOffset. Recovered Prayer.kt omitted all four sets before calling calculator. No exact historical Kotlin operation recovered, so explicit APK fallback restoration.
- Pure-Java sensitivity fixture against actual recovered watch calculator:9125date/hour/zone inputs,13differing cases before noon normalization across Cairo/London/NewYork/Sydney DST days. This is not a paired-original runtime proof, but demonstrates the omission changes results by60minutes near transitions. Fixture deliberately reports differences between old call shape and original-noon shape; it is not expected to report zero.
- Restore four original calendar setters, no solar formula or approved dynamic-countdown changes. Local Wear buildPASS. Full paired watch transition snapshots still needed.
- Hisn DB content compared from completed utility evidence:135chapters,335items and4initialfavorites exactly equal original/rebuild, including all rows and strings. This is content parity, not complete chapter-action proof.

## Next finite checks prepared
- Verification-only WatchEngineProbe calls actual original APK d21.c and recovered Prayer.day through isolated package contexts.810inputs across5zones,9dates,6hours,3day offsets. This checks exact output milliseconds at DST/leap/day boundaries, not just a local sensitivity model. Probe compiles, execution pending. Reflection or context failure is a setup blocker, never a parity result.
- Hisn chapter suite covers short-chapter counter done/reset, info open/close, separate chapter/item favorites, Read, Add repeated, list return/reopen, empty-query/clear and DB persistence. Pending emulator execution. Source inventory is not an action-coverage claim.

## Hisn chapter action differences and harness ambiguity
- Run41139108099:75checks,4failures,5/32equal semantic scenes. Visual inspection confirms original count1 initial label Read rather than Tap to count, EN Info accessibility label instead of unconditional Arabic, and green Added state/duplicate guard missing in recovery. Restored original Oct20 printed Added button body exactly (builder20 row359), preserving existing alreadyAdded title wiring. Initial label and language condition are explicit APK behavior restorations; original Kotlin operations not yet found. Local phone buildPASS.
- Mark Read in first harness hit original count1 chip labeled Read before bottom Read button. Thus original hisn_read empty vs recovery row is harness ambiguity, not established product mismatch. Fixed harness to use bottommost Read, EN Info, and guarded Added repeat. Actual item/chapter favorite database rows align. Both original and recovery imported135chapters/335items exactly.
- Paired actual-APK watch810case engine run next; chapter repair rerun follows, then Hindi/Latin4mode UI sweep. Do not call this finite suite complete coverage.

## Watch engine probe setup failure
- Run41140916694 built all APKs but original probe createPackageContext returned NameNotFound despite successful adb install. Host target34manifest omitted explicit package visibility queries. Add both exact verified package names and save pm path diagnostic. This run produced no engine outputs and proves no arithmetic result. Corrected Hisn rerun41142551606 proceeds before retry.

## Finite Hisn repairs verified / engine probe route correction
- Hisn41142551606:79checks0fails,32/32semantic pairs equal, actual paired pixels inspected for info/added/read/favorites. Final favorite/read/add DB contents match except real timestamps. One added chapter only after repeat. This is one short chapter inEN/light,AR/dark, not whole-corpus behavior or motion proof.
- Watch engine41144194689 package visibility fixed, but reflection looked up JADX's synthetic defpackage.d21. Original runtime obfuscated root class is d21, so remove synthetic package. No engine outputs yet, no arithmetic verdict. Phone4mode digit sweep proceeds next before retry.

## Four-mode digit fixture invalid, not parity evidence
- Run41145732805:75checks72fail. Every scene fell into language onboarding. Runtime SharedPreferences rewrote XML header with single quotes; harness echo-single-quote transport stripped them when changing digit style and corrupted fixture XML. Use base64 fixture transport and assert retained onboarding preference. No UI parity conclusion from this run. Keep exact failure evidence.

## Paired actual watch APK engine boundary verification
- Run41147235416:810paired actual original d21.c vs recovered Prayer.day inputs,0mismatches across exact6output milliseconds per input(4860times).5zones(Cairo/London/NewYork/Sydney/UTC),9dates(DSTand leap),6local hours,3dayoffsets,default Cairocoords and Egyptian method. This verifies the restored day-anchor behavior within that finite grid on WearAPI33TZDB. No claim for allcoordinates/methods/OSes or next/prev selection, notifications, sensor/hardware/motion. Original fixture runs unchanged; reflection no longer blocked. Invalid earlier setup runs remain failure evidence.

## Global digit import replay finite runtime check
- Run41148704469:83checks0fails,20/28raw semantic pairs equal. All8non-equal scenes attributable to moving prayer countdown(4), differing settings scroll offsets(3), onecalendar lower-text visibility(1), not yet normalized. Actual pixels inspected: AR Hindi Tasbeeh/converter/Hisn, AR Latin calendar, EN Hindi settings. Digit glyphs and scene content align in these samples. Strict scroll/settling/endpoints remain an open item; do not report28/28or whole-phone visual parity. Last AR Latin settings shot even landed About, so route/foreground capture needs improvement there.
- Next finite converter leap/invalid/year/direction/reopen cases and calendar absolute-end/events/reminder toggle/persistence. Fresh identical date fixture used to separate wall-clock changes from output formatting.

## Date/calendar finite parity
- Run41151207220:71checks0fails,32/32semantic scene pairs equal. Actual frames inspected for leap29Feb2028conversion, invalid30Febnoresult, Hijri Today reverse, calendar absolute end and event reminder reopen. One reminder survives force-stop/reopen in both; direct prefs capture filename was wrong, so direct persisted file proof deferred. Year bounds/nonleap/emptyreopen scenes semantically align too. No claim for everydate/calendar algorithm or notification firing.
- Next settings toggle/slider/digit selector and exact persisted reminder files. Add verification-only accessibility scroll endpoint operation to separate user swipe trajectories from absolute-end alignment.

## Settings controls initial finite check and test corrections
- Run41153476528:47checks8fail,15/16semanticpairs equal. Shared failures are wrongArabiclockhelp label and nonexistentSeekBar role(customslider), no product mismatch verdict. Notification vibration false/reopen/true exactly matches prefs. Event reminder IDs exactly match isra_miraj on and empty off in realfajr_prefs. Need captureapp_haptics separately, corrected next. ENendpoint differs; test boolean result serialization may be absent, changed instrumentation output to explicit string plus per-scroll evidence.
- Next corrected finite repeat verifies shared failing paths, custom slider labels/actualscale, persistedhaptics and absolute-end. No broad rerun or product source change from setup failures.

## Settings retry results, endpoint probe itself invalid
- Run41154830954:47checks1fail,10/18rawsemantic pairs equal. Notification vibration and app_haptics false/reopen/true match exactly all4sessions; real calendar reminder IDs also match. Original EN slider not visible due endpoint positioning, no slider max proof. Original/AR and comparison/AR middle tap both save1.2. Prior max tap hit outside effective track and left1.0, not a product defect.
- Accessibility endpoint probe returnedfalse at original top andtrueatcomparison. It omitted HierarchyDump's asynchronous root retry and silently converted nullroot into false. Added explicit nullroot retry/error. Do not diagnose product accessibility/physics from this artifact. Next settings endpoint uses repeated settled-swipe/tree stability with bounded16attempts; visual inspection required, no named endpoint assumed.

## Settled settings endpoints and maximum scale verified
- Run41156114793:47checks0fail,18/20semanticpairs equal. ActualEN/AR settled end frames align, fontmax1.6 persisted all4sessions and paired frames match. The two toggles-on screenshots differ in scroll positions, not savedtogglevalues; trajectory remains unproven. Attempted midreset used stalepre-resizecoordinates and remained1.6 both; do not claimresetproof. No more full settings rerun for thatsetupdetailnow.
- Continue independent watch alert/location/language/permission/circles controls, retaining no physicalGPS/paireddevice/haptic claim from emulator.

## Watch additional controls
- Run41157363967:91checks1initialforegroundmiss(originalARwatchface),23/28semanticpairs equal. Allfour sessions persist equal alertstyleoff,Adhanoff/on,locmodeAuto/phone,languageAR/EN. Actual alert radio/location/language/circles pixels inspected and align.4other rawsemanticdifferences are root settings scroll offsets aroundAdhan, no savedvalue mismatch. No physicalGPS/notificationdelivery/soundclaim.
- Next finite watchfontplus/minusclamps/reopen and alarmUIlaunch/dismiss, not actual alarm firing. Originalcountdown/ring approved scope unchanged.

## Watch font/alert finite batch
- Run41158587480:87checks32sharedfonttargetfailures,16/16semanticpairs equal. Actual alarmUIFajr/Isha EN/ARlaunch/dismiss frames align; noalarmfire/soundclaim. Fontcounteraction target assumed plusright inRTL and strictunclipped squarebounds; sharedfailuresfromresizing/roundclipping, actualsavedstatesmatch original. Capturesnamedmax/minareNOTactualextrema(.8ARmaxlabel,1.2ARminlabel,1.1EN). RetainrawnamesbutdoNOTcallclampsproven. CorrectRTLdirection,bottomclippedbuttonbounds,reopenandscrollbeforeeachpress;nextbatchfontonly.

## Watchfont isolated target failure diagnosed exactly
- Run41159823550:71checks52sharedfail,8/8semanticpairs equal. Prepressscroll moved both100pxbuttons to[95,133][195,233]and[259,133][359,233], excludedbyharnessy>180. No fontchangesoccurred. Removeunneededscroll and allow verified geometryy>80. This is testonly; no productchange. Clampverificationstillpending.

## Font clamps verified and working environment recovered
- Run41160998827:71checks0fail,8/8semanticpairs. Actual original/recoveredEN/ARpersistmax1.3/min.8andmaxreopen align. Pairedpixelsinspected. Earlierfailedtargetruns remain failures, not clamping evidence.
- Workingfilesreset at04:55. Latest0441Drivearchives downloaded/verified and freshcomparisonclone restored. OriginalwatchAPKSHA256unchanged;main200dc32unchanged. Sourcebackupsfulfilled recoverypurpose. Localtoolchain/historicalscratch/evidencefiles must be reacquired as needed; no unrecorded completion claimed.
- current-status.md separates today's scoped evidence from stale historical nativephone/status wording. SOURCE-ORIGINS.csv preserved as historical capture ledger, not falsely regenerated as exact original source.

## Permission refusal and onboarding resume finite batch41162208555
-27checks0fail;12/16rawsemanticpairs. Four root prayer frames differ by sequential live clock, including crossing Iqama boundary; no boundary equivalence claim from these captures.
-EN/light and AR/dark legacy location/files explainer denial persists stage3/done and no reask after restart; actual pairedframes inspected and explanations align.
-Fresh onboarding after clear starts device-languageArabic in both priorlangsessions, then selectingEnglish/Next persists page1/languageen identically and reopens method page. This is not full bilingual onboardingcoverage.

## Onboarding persisted-stage batch41163185279
-55checks0fail,18/24rawsemanticpairs;18scenes align,2OSnotificationlabels differ only deliberate Comparison appname,4prayerrootframes use sequentialclock.
-Seededpage3 calculation/Hanafi/confirmationback/reminderchoice/notificationdenial/filesdismiss and done/reopen verifiedENlight/ARdark;actualpairedpixelsinspected. app_prefsidentical, vibrationfalsepersisted.
-Fajrlabeltap hit noninteractive label/root ancestor, not the switch: allalerts stayedtrue in both. Therefore no off-switch claim; corrected next finite harness to target switchcenter by Fajrrowbounds and assertenabled_0false. Missingfajr_prefsfilename also corrected.

## Onboarding actual switch correction41164184142
-71checks0fail,18/24rawsemanticpairs; actual FajroffswitchpixelsalignENlight/ARdark. enabled_0false, other4alertstrue, vibrationfalse, asr_juristic1, completedonboardingpermissionflagsallidenticaloriginal/recovered. Sixremainingrawpairs are2OSapp-labeldifferencesand4sequentialliveprayerrootframes.
-Seededpage3scopeonly, not GPS/citynetwork or fullpermissionallowmatrix. Closingthisfinitebatch; widerenginecoordinatesnext.

## Wider coordinates41165122005 invalid fixture
-5670sameoutputcases/0mismatches are NOT coordinatecoverage: sensitivityguard failed, every city identical to Cairo in both. ForeignAPKenginecode is loaded under verificationhostUID, so targetappprivateprefs cannot be assumed readable even when adbrootreadback succeeds.
-Nextcorrectedprobeuses explicit ContextWrapper wear_prefsdependency fixture with suppliedlatloninhostprefs. This compares actual engine functions under controlled inputs, not targetapppreference-storageintegration. Sensitivitymustpass.

## Actual watch engine coordinate dependency grid41166141457
-5670cases34020exact-msoutputs,0mismatch.7coordinatefixtures(Cairo/London/NewYork/Sydney/Quito/Tromso/CapeTown),5zones,9DST/leapdates,6hours,3dayoffsets. AllnonCairofixtureschange810/810casesinboth, so sensitivityguardpasses.
-ExplicitContextWrapperwear_prefsdependencyfixture, fixturelatlonrecordedandchecked. Actualoriginal/recoveredenginefunctioncode; notappstorageorphone/GPSdeliveryintegration. FixedEgyptianmethod,noallmethodclaim.

## Phone statistics controlled history41167184724
-39checks0fail,23/24semanticpairs. FourcontrolledRoomhistories(empty/today/streakgap/yesterdaystreak)readbackexactacrossallfouroriginal/recoveredENlightARdark sessions. Actualpairedframesinspected.
-Scopedcounts(today/week/month/streak/total)align:empty0;today3/3/3/1/3;streakgap1/4/5/3/6;yesterday0/4/4/3/5. OneENlowerframehasdifferentintermediatescrolloffset, notcontentverdict.
-Actualworkingresetat06:21recoveredfrom0607Drivebackup/freshGitclone;main200dc32unchanged.
-NextsamefinitehistorysuiteAPI23/36matrix, API34currentproofnotassumedacrossOSversions.

## API23/36 matrix first setup issue
-41168836184API23cannotestablishparity: verificationhost minSdk30 preventsinstallation. OriginalandcomparisonAPKinstallsucceededbutbaselineDBwasabsent, noappcrashlogcapturedbypriorharness. Thatdoesnotproveproductincompatibility.
-LoweredverificationhostminSDK23only; captureinstalldiagnostics/baselinelog/window/pixels, skipinvalidDBfixturesratherthancreatingemptySQLitefile. AwaitAPI36existingjobbeforefurtherdispatch.

## API36 statistics41168836184
-API36job39checks0fail,24/24semanticpairs;8pairedfullDBsnapshotsidentical. ActualpairedENstreakgap/ARyesterdaylowerpixelsinspectedandalign. OverallworkflowredbecauseAPI23setupfailed, notAPI36failure. Finitehistorysuiteonly, notallappAPI36certification.
-NextdispatchsingleAPI23withverificationhostminSDKfixandbaselinefailureevidence, noAPI36repeat.

## API23 correction41170601084 invalid firstboot state
-11checks8fail despitegreenworkflow; harnessnowfailsexitwhenchecksfailed. Hostinstallednow, butbothappsbaselineDBabsentandhierarchyempty. Logsprovefirstbootsystemserver/encrypteddatarestartafterinstallation, packageinvalidafterrestart; originalactivitydisplayedbeforethat. Noappspecificfatalexceptionproven. Thisisnotaparitypassorconfirmedproductdefect.
-Onegroundedsetupcorrection: settleAPI23firstboot120secondsbeforeinstallationandassertallthreepackagesremainregistered. Retainfailurelogs;noguessproductchanges.

## API23 statistics41171691657
-42checks0fail,26/26semanticpairs,actualpairedpixelsinspectedalign. API23uimodedarkrequestnotreflectedinactualpixels:ARwaslight,notARdarkproof.
-8historytablesexact,4ARfullDBsnapshotsidentical;4ENfullDBsnapshotsdifferbyonependingdefaulttag(original4/recovered3). Seedasyncwasnotwaitedfully, nosourcepatchjustified. SubsequentARfourtagsmatch. Nextactionsharnesswaitsforall5dhikr/4tagsbeforedataassertions.
-FiniteAPI23statisticsproofdoesnotcertifyallAPIs/scenarios. Firstboot120secondsettleresolvedpackage/resetsetupissue.

## Read action persistence41173455693
-71checks0fail,9/12rawsemanticpairs. ActualdetailClosewritesnone;twoReadpressesappendtwohistoryrows/bumpcount2/date/timestampsmatchscope;invalidID-1opensnodetailandnoextrahistory. Allfourfullysettled5dhikr4tagssessionspass.
-Pairedactualpixelsinspected,detail/statsalign;twoinvalidrootframessequentialclockandARstatsreadtimestampsdiffer(expectedexecutiontime). Not12/12exactsemanticclaim.
-NextrealAlarmManagerdeliveryandactualnotificationReadbutton, notdirectreceiverinvocation.

## Real notificationRead41174482983 found product omission
-55checks0fail;delivery/actualRead/DBhistorycount1/statsverifiedallfour.4/6rawsemanticpairs,actualpixelsinspected. AlarmReceiversecondaryfallbackstillhardcodedArabicactionlabels(andchannelname), originalusesEN/ARlocalizedS.NotifReadDone/NotifRemindLater/NotifChannelAzkar. OriginalnotificationdumpconfirmsENMarkasread/Remindmein10minutesandARتمالقراءة/ذكرنيبعد10دقائق. RestoredexistingL10nkeys, notnewcopy.
-Comparisonshadealsoincludedoriginalnotificationfromprevioussessionbecauseclockrewindrescheduledit. Testnowforcestopsbothpackagesbeforesession;do notcallthisduplicateproductnotification. Nextbatchassertsexactlocalizedactionlabelsandisolatespackages.

## Reminder localization repair verified41175556383
-63checks0fail,4/6rawsemanticpairs;actualEN/ARlocalizednotificationactionpixelsalignafter8a4b939repair. RemainingrawshadepairsincludeintentionalComparisonapp-labelandOSindicators. Packageisolationremovespriornotificationcontamination. DeliveryandactualReadDBcount1verified. Channelnameusesexistinglocalizedkey;notclaimedallchannelvariants.
-NextactualSnoozebuttonand10minredelivery, no directreceivercall.

## Actual notification snooze41176663090
-75checks0fail,6/10rawsemanticpairs. ActualSnoozebuttonschedulesRTC_WAKEUPat10mininall4sessions,clockadvanceactualredeliveryandReadappendsexactlyonehistory/count1. PairedredeliverypixelsinspectedalignexceptintentionalComparisonlabel/OSindicators. No deepidle/physicalaudio claim.
-Nexttagrename/deleteCancel/Saveandlinked-dhikrpreservation;linkiscontrolledDBfixture,mutationsactualUI.

## Tag mutations41178422264 invalid fixture (79checks20fail)
-ControlledfixturecopiedonlybaseSQLitefile, missingcreatedtagstillinWAL; injectedlinkpointedtononexistenttag5inbothapps. ActualUIthenedited/deleteddefaulttag4, notfixturetag. Do notconverttheseassertfailuresintoproductdefects.
-CorrectedSQLitebackupfromcompletepulledcreated-dbdirectory(automaticWALreplay),addedtag/linkreadbackbeforeUI. No productpatch.

## Tag mutations41179477450 evidence review
-87checks8reportedfail;all8falsefailuresarePythonlist-vs-tupleref-membershipassertions,notbehavior. RecordedDBshows[1,5]atlinkedandCancelstagesinall4sessions. Offlinecorrectedtuplechecks8/8pass;originalworkflowfailurepreserved, notrewritten.10/10semanticpairsandactualpairedpixelsalign.
-TagrenameCancelpreservesname,Savepersists;deleteCancelpreservestag/link,confirmclearsbothbutpreservesdhikr. CorrectedfutureharnessmembershipwithoutnewCIjusttomakegreen.
-Nextphone8method/twoAsr+4Londonhighlatcontrolledprefsand+2Fajr/-3IshaoffsetinputactualUIgrid.

## Phone prayer method input grid41181260465
-83checks0fail,20/20exactdisplayedtimearraypairs.8methods*2AsrCairoand4Londonhighlatrules,+2Fajr/-3Ishaoffsets,June21.40rawsemanticpairs1exact;all39differencesonlyliveH:MM:SScountdown(asexplicitofflinecomparisonconfirmed), nottime-labelmismatch. Pairedactualpixelsinspected. Asr/method/highlatinputssensitiveinboth.
-Thisprovesselecteddisplayedminuteoutputswithsavedprefs,notexact-ms/all-dates/allcoordinates/allsettingsUI. Nextoptionalrowsactualswitchon/reopen/prayerrowpresence/off.

## Optional rows41183315277 setup route failure
-8checks5failonlyoriginalfirstsession. HarnessmistookOptionaltimesfordrawerdirectentry;actualcodeandXMLshowSettingsnestedroute. CorrectedSettings->Optionaltimesandmissing-prefguard;no productchange.

## Optional prayer rows41184408987
-55checks0fail,8/18rawsemanticpairs. All4actualswitchesonpersist/reopen, all4prayerrowsappear;offpersistsandrowsremovedENlightARdark. Actualpixelsinspectedalign.10rawrootpairsdifferclock;2ENintermediatescrollalsoonebellvisibilitydiff,so notallscroll/motionproof. Finalvisibleoptionalminuteoutputsalign.
-NextFajrbelldialogEarlyreminderCancel/Apply/backrollbackwithactualprefs,notjustdismissappearance.
