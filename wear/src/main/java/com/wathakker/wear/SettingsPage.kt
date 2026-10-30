package com.wathakker.wear
import androidx.compose.foundation.layout.offset

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.LocationManager
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.layout.layout
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.IntOffset
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.core.content.ContextCompat
import androidx.wear.compose.foundation.lazy.ScalingLazyColumn
import androidx.wear.compose.foundation.lazy.ScalingLazyListAnchorType
import androidx.wear.compose.foundation.lazy.ScalingLazyListState
import androidx.wear.compose.foundation.lazy.rememberScalingLazyListState
import kotlin.math.roundToInt

fun themeName(i: Int) = T(WTheme.defs[i].name, listOf("Gold", "Turquoise", "Blue", "Green")[i])
fun patName(id: Int): String { val i = Settings.patterns.indexOfFirst { it.first == id }.coerceAtLeast(0); return T(Settings.patterns[i].second, listOf("Rub el Hizb", "Stars and diamonds", "Hex grid", "Crossed octagram", "Round arabesque")[i]) }

/** Small "open externally" glyph: a square with an arrow leaving its top-right corner. */
@Composable
private fun ExternalIcon(w: Float, color: Color) {
    Canvas(Modifier.size((w * 0.055f).dp)) {
        val m = size.minDimension; val sw = m * 0.14f
        val box = Path().apply {
            moveTo(m * 0.10f, m * 0.32f); lineTo(m * 0.10f, m * 0.90f); lineTo(m * 0.68f, m * 0.90f); lineTo(m * 0.68f, m * 0.60f)
        }
        drawPath(box, color, style = Stroke(width = sw, cap = StrokeCap.Round))
        drawLine(color, Offset(m * 0.42f, m * 0.58f), Offset(m * 0.88f, m * 0.12f), strokeWidth = sw, cap = StrokeCap.Round)
        val head = Path().apply { moveTo(m * 0.52f, m * 0.12f); lineTo(m * 0.88f, m * 0.12f); lineTo(m * 0.88f, m * 0.48f) }
        drawPath(head, color, style = Stroke(width = sw, cap = StrokeCap.Round))
    }
}

/** sub: 0 root, 1 pattern list, 2 theme colors, 3 location source, 4 permissions, 5 language,
 *  6 alert style, 7 font style, 8 font size.
 *  The enter/exit transitions live in WearApp (around this composable), because the swipe-to-dismiss
 *  box re-keys its content on navigation and any AnimatedContent inside would restart fresh = no
 *  enter animation (his 19:44 report). The swipe-back gesture is the single swipe-to-dismiss level. */
@Composable
fun SettingsPage(w: Float, sub: Int, go: (Int) -> Unit, rootListState: ScalingLazyListState? = null) {
    BackHandler(enabled = sub != 0) { go(0) }
    SettingsContent(sub, w, go, rootListState)
}

@Composable
fun SettingsContent(sub: Int, w: Float, go: (Int) -> Unit, rootListState: ScalingLazyListState? = null) {
    val ctx = LocalContext.current
    var locMode by remember { mutableIntStateOf(Settings.locMode(ctx)) }
    var perm by remember { mutableIntStateOf(0) }
    val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { perm++; refreshGps(ctx) }
    val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { perm++ }
    fun has(p: String) = ContextCompat.checkSelfPermission(ctx, p) == PackageManager.PERMISSION_GRANTED
    val locOk = perm >= 0 && has(Manifest.permission.ACCESS_COARSE_LOCATION)
    val notifOk = perm >= 0 && has(Manifest.permission.POST_NOTIFICATIONS)

    @Composable
    fun Row1(title: String, value: String?, onClick: () -> Unit, lead: @Composable (() -> Unit)? = null, selected: Boolean = false, trail: @Composable (() -> Unit)? = null) {
        Row(
            Modifier.fillMaxWidth().height((w * 0.17f).dp).clip(RoundedCornerShape(50))
                .then(if (selected) Modifier.background(WTheme.cur.brush()) else Modifier.background(Pill)).clickable { onClick() }.padding(horizontal = (w * 0.06f).dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            if (lead != null) { lead(); Spacer(Modifier.width((w * 0.03f).dp)) }
            Text1(title, tx(w * 0.062f, if (selected) Ink else Color.White, selected))
            Spacer(Modifier.weight(1f))
            if (value != null) Text1(value, tx(w * 0.05f, if (selected) Ink else SoftGrey))
            if (trail != null) { Spacer(Modifier.width((w * 0.02f).dp)); trail() }
        }
    }

    // One UI Watch switch, geometry measured from the owner's own watch screenshots
    // (track 73x40px and knob 35px on a 450px screen). Track color follows the app theme accent.
    @Composable
    fun OneUiSwitch(on: Boolean, onChange: (Boolean) -> Unit, w: Float) {
        // Proportions from his 22:07 One UI reference: thumb ~0.82 of the track height with a clear inset.
        val tw = w * 0.162f; val th = w * 0.089f; val knob = w * 0.073f
        // One UI Watch: the knob slides and the track color crossfades (his 19:45 report: it jumped).
        val frac by animateFloatAsState(if (on) 1f else 0f, tween(180, easing = FastOutSlowInEasing), label = "switchKnob")
        val track by animateColorAsState(if (on) Gold else Color(0xFF5A5A60), tween(180), label = "switchTrack")
        val den = LocalDensity.current
        val travelPx = with(den) { (tw - th).dp.toPx() }
        // The knob is a real child anchored at CenterStart and moved with offset{}: BOTH the
        // anchor and the offset mirror in RTL together, so ON rests at the END side (right in
        // LTR, left in RTL) with no manual direction math. (Build 70 bug: a layout-pass trick
        // shifted the knob's CONTENT inside its own bounds - the white thumb vanished when ON,
        // his 22:07 catch.) offset{} reads frac without recomposing per frame.
        Box(
            Modifier.size(width = tw.dp, height = th.dp).clip(RoundedCornerShape(th.dp))
                .background(track).clickable { onChange(!on) }.padding(horizontal = ((th - knob) / 2f).dp),
            contentAlignment = Alignment.CenterStart
        ) {
            Box(
                Modifier.offset { IntOffset((travelPx * frac).roundToInt(), 0) }
                    .size(knob.dp).clip(CircleShape).background(Color.White)
            )
        }
    }

    // One UI Watch radio option (reference: system radio pickers), ring + filled dot in app accent.
    @Composable
    fun OneUiRadio(on: Boolean, w: Float) {
        val d = w * 0.067f
        val sel by animateFloatAsState(if (on) 1f else 0f, tween(160, easing = FastOutSlowInEasing), label = "radio")
        Canvas(Modifier.size(d.dp)) {
            val r = size.minDimension / 2f
            drawCircle(Color(0xFF8E8E93), radius = r * 0.82f, style = Stroke(width = r * 0.22f))
            if (sel > 0f) {
                drawCircle(Gold.copy(alpha = sel), radius = (r * sel).coerceAtLeast(0.001f))
                drawCircle(Color(0xFF26262B).copy(alpha = sel), radius = (r * 0.42f * sel).coerceAtLeast(0.001f))
            }
        }
    }

    @Composable
    fun OneUiDivider(w: Float) {
        Box(Modifier.fillMaxWidth().padding(horizontal = (w * 0.08f).dp).height(1.dp).background(Color(0xFF3A3A3E)))
    }

    class RadioOpt(val label: String, val selected: Boolean, val onClick: () -> Unit, val lead: (@Composable () -> Unit)? = null, val labelStyle: (TextStyle) -> TextStyle = { it })

    // One UI Watch radio-list card in app colors: dark rounded card, dividers, accent radio rings.
    // Row geometry measured from the owner's watch: row ~110px and label ~34px on a 450px screen.
    @Composable
    fun OneUiRadioCard(w: Float, options: List<RadioOpt>) {
        Column(Modifier.fillMaxWidth().padding(horizontal = (w * 0.027f).dp).clip(RoundedCornerShape((w * 0.09f).dp)).background(Color(0xFF26262B))) {
            options.forEachIndexed { i, opt ->
                if (i > 0) OneUiDivider(w)
                Row(Modifier.fillMaxWidth().heightIn(min = (w * 0.244f).dp).clickable { opt.onClick() }.padding(horizontal = (w * 0.08f).dp, vertical = (w * 0.03f).dp), verticalAlignment = Alignment.CenterVertically) {
                    OneUiRadio(opt.selected, w)
                    Spacer(Modifier.width((w * 0.05f).dp))
                    if (opt.lead != null) { opt.lead(); Spacer(Modifier.width((w * 0.05f).dp)) }
                    Text1(opt.label, opt.labelStyle(tx(w * 0.073f, Color.White, align = TextAlign.Start)), maxLines = 2)
                }
            }
        }
    }

    // One UI Watch settings card row: tall row (~120px on 450), white title, value under it in the
    // app accent color (Samsung renders it blue; we keep the app identity per owner).
    @Composable
    fun OneUiRow(w: Float, title: String, value: String?, onClick: (() -> Unit)?, trail: (@Composable () -> Unit)? = null) {
        Row(Modifier.fillMaxWidth().heightIn(min = (w * 0.267f).dp).then(if (onClick != null) Modifier.clickable { onClick() } else Modifier).padding(horizontal = (w * 0.08f).dp, vertical = (w * 0.03f).dp), verticalAlignment = Alignment.CenterVertically) {
            Column(Modifier.weight(1f)) {
                Text1(title, tx(w * 0.076f, Color.White, align = TextAlign.Start), maxLines = 2)
                if (value != null) Text1(value, tx(w * 0.058f, Gold, align = TextAlign.Start), maxLines = 2)
            }
            if (trail != null) trail()
        }
    }

    // Thin vertical separator before a trailing switch, as in the One UI connections screen.
    @Composable
    fun OneUiVertDivider(w: Float) {
        Box(Modifier.padding(horizontal = (w * 0.04f).dp).width(1.dp).height((w * 0.11f).dp).background(Color(0xFF3A3A3E)))
    }

    // One UI Watch section header (same style everywhere): small grey label above its card.
    @Composable
    fun SectionHeader(text: String) {
        Text1(text, tx(w * 0.062f, SoftGrey, align = TextAlign.Start), Modifier.fillMaxWidth().padding(horizontal = (w * 0.07f).dp).padding(top = (w * 0.04f).dp, bottom = (w * 0.025f).dp))
    }
    @Composable
    fun SectionCard(content: @Composable () -> Unit) {
        Column(Modifier.fillMaxWidth().padding(horizontal = (w * 0.027f).dp).clip(RoundedCornerShape((w * 0.09f).dp)).background(Color(0xFF26262B))) { content() }
    }

    val title = if (langTick < 0) "" else when (sub) { 1 -> T("الزخرفة", "Pattern"); 2 -> T("لون السمة", "Theme color"); 3 -> T("مصدر الموقع", "Location source"); 4 -> T("الأذونات", "Permissions"); 5 -> T("اللغة", "Language"); 6 -> T("نوع التنبيه", "Alert style"); 7 -> T("نوع الخط", "Font style"); 8 -> T("حجم الخط", "Font size"); else -> T("الإعدادات", "Settings") }
    // Always open at the TOP with the title visible (his 19:44 report: it reopened mid-list):
    // the Wear default centers item 1, which pushed the title off the top edge. ItemStart anchoring
    // with item 0 aligned to the start puts the title at the top like One UI Watch.
    val localListState = rememberScalingLazyListState(initialCenterItemIndex = 0, initialCenterItemScrollOffset = 0)
    val listState = if (sub == 0 && rootListState != null) rootListState else localListState
    Box(Modifier.fillMaxSize().background(Color.Black)) {
        ScalingLazyColumn(
            Modifier.fillMaxSize(),
            state = listState,
            anchorType = ScalingLazyListAnchorType.ItemStart,
            autoCentering = null,
            contentPadding = PaddingValues(top = (w * 0.09f).dp, bottom = (w * 0.35f).dp)
        ) {
            item { Text1(title, tx(w * 0.08f, Color(0xFFB9B9BE)), Modifier.padding(top = (w * 0.07f).dp, bottom = (w * 0.05f).dp)) }
            when (sub) {
                0 -> {
                    // Regrouped into One UI sections (his 19:47 order): appearance together,
                    // notifications together, language & location together, then general, then About.
                    item { SectionHeader(T("المظهر", "Appearance")) }
                    item {
                        SectionCard {
                            OneUiRow(w, T("الزخرفة", "Pattern"), patName(starPattern), { go(1) })
                            OneUiDivider(w)
                            OneUiRow(w, T("لون السمة", "Theme color"), themeName(WTheme.idx), { go(2) })
                            OneUiDivider(w)
                            OneUiRow(w, T("نوع الخط", "Font style"), WFonts.cur.label, { go(7) })
                            OneUiDivider(w)
                            OneUiRow(w, T("حجم الخط", "Font size"), null, { go(8) })
                        }
                    }
                    item { SectionHeader(T("الإشعارات", "Notifications")) }
                    item {
                        var on by remember { mutableIntStateOf(if (Adhan.enabled(ctx)) 1 else 0) }
                        var fs by remember { mutableIntStateOf(if (Adhan.fullScreen(ctx)) 1 else 0) }
                        SectionCard {
                            OneUiRow(w, T("تنبيه الأذان", "Adhan alert"), null, { on = 1 - on; Adhan.setEnabled(ctx, on == 1) }, trail = {
                                OneUiVertDivider(w)
                                OneUiSwitch(on == 1, { want -> on = if (want) 1 else 0; Adhan.setEnabled(ctx, want) }, w)
                            })
                            OneUiDivider(w)
                            OneUiRow(w, T("نوع التنبيه", "Alert style"), if (fs == 1) T("تنبيه إشعار بملء الشاشة", "Full-screen notification alert") else T("تنبيه إشعار", "Notification alert"), { go(6) })
                        }
                    }
                    item { SectionHeader(T("اللغة والموقع", "Language & location")) }
                    item {
                        SectionCard {
                            OneUiRow(w, T("اللغة", "Language"), listOf(T("مزامنة", "Sync"), "عربي", "English")[Loc.mode(ctx)], { go(5) })
                            OneUiDivider(w)
                            OneUiRow(w, T("مصدر الموقع", "Location source"), if (locMode == 0) T("من الهاتف", "Phone") else T("تلقائي", "Auto"), { go(3) })
                        }
                    }
                    item { SectionHeader(T("عام", "General")) }
                    item {
                        SectionCard {
                            OneUiRow(w, T("الأذونات", "Permissions"), null, { go(4) })
                            OneUiDivider(w)
                            OneUiRow(w, T("فتح في الجهاز", "Open on phone"), null, { openOnPhone(ctx) {} }, trail = { ExternalIcon(w, SoftGrey) })
                        }
                    }
                    // About section, last (his 19:45/19:47): version read from the build, not hardcoded.
                    item { SectionHeader(T("معلومات حول التطبيق", "About this app")) }
                    item {
                        val appVer = remember {
                            try { ctx.packageManager.getPackageInfo(ctx.packageName, 0).versionName ?: "" } catch (_: Throwable) { "" }
                        }
                        SectionCard {
                            OneUiRow(w, T("إصدار التطبيق", "App version"), appVer, null)
                        }
                    }
                    item { Spacer(Modifier.height((w * 0.12f).dp)) }
                }
                7 -> {
                    item {
                        // Same 8 options as the phone, each label rendered in its own font (live preview).
                        OneUiRadioCard(w, WFonts.choices.mapIndexed { i, ch ->
                            RadioOpt(ch.label, WFonts.idx == i, { WFonts.saveChoice(ctx, i) }, labelStyle = { st -> st.copy(fontFamily = ch.family) })
                        })
                    }
                    item { Spacer(Modifier.height((w * 0.12f).dp)) }
                }
                8 -> {
                    item {
                        // One UI Watch font-size page (owner reference): preview text, dots slider, -/+ buttons in app accent.
                        var step by remember { mutableIntStateOf(WFonts.stepIdx) }
                        fun applyStep(i: Int) { step = i.coerceIn(0, WFonts.STEPS.size - 1); WFonts.saveScale(ctx, WFonts.STEPS[step]) }
                        @Composable
                        fun sizeBtn(minus: Boolean) {
                            Box(Modifier.size((w * 0.22f).dp).clip(CircleShape).background(Color(0xFF3A3A3E)).clickable { applyStep(step + if (minus) -1 else 1) }, contentAlignment = Alignment.Center) {
                                Canvas(Modifier.size((w * 0.07f).dp)) {
                                    val sw = size.minDimension * 0.16f
                                    drawLine(Color.White, Offset(0f, size.height / 2f), Offset(size.width, size.height / 2f), strokeWidth = sw, cap = StrokeCap.Round)
                                    if (!minus) drawLine(Color.White, Offset(size.width / 2f, 0f), Offset(size.width / 2f, size.height), strokeWidth = sw, cap = StrokeCap.Round)
                                }
                            }
                        }
                        Column(Modifier.fillMaxWidth().padding(horizontal = (w * 0.05f).dp), horizontalAlignment = Alignment.CenterHorizontally) {
                            Text1(T("هكذا سيبدو النص الرئيسي.", "Main text will look like this."), tx(w * 0.07f, Color.White), maxLines = 2)
                            Spacer(Modifier.height((w * 0.02f).dp))
                            Text1(WFonts.scaleLabel(WFonts.STEPS[step]), tx(w * 0.05f, Gold))
                            Spacer(Modifier.height((w * 0.05f).dp))
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                WFonts.STEPS.indices.forEach { i ->
                                    val sel = i == step
                                    Box(Modifier.size(if (sel) (w * 0.042f).dp else (w * 0.024f).dp).clip(CircleShape).background(if (sel) Gold else Color(0xFF5A5A60)))
                                    if (i < WFonts.STEPS.size - 1) Spacer(Modifier.width((w * 0.032f).dp))
                                }
                            }
                            Spacer(Modifier.height((w * 0.06f).dp))
                            Row {
                                sizeBtn(true)
                                Spacer(Modifier.width((w * 0.14f).dp))
                                sizeBtn(false)
                            }
                        }
                    }
                    item { Spacer(Modifier.height((w * 0.12f).dp)) }
                }
                6 -> {
                    item {
                        var fs by remember { mutableIntStateOf(if (Adhan.fullScreen(ctx)) 1 else 0) }
                        OneUiRadioCard(w, listOf(
                            RadioOpt(T("تنبيه إشعار", "Notification alert"), fs == 0, onClick = { fs = 0; Adhan.setFullScreen(ctx, false) }),
                            RadioOpt(T("تنبيه إشعار بملء الشاشة", "Full-screen notification alert"), fs == 1, onClick = { fs = 1; Adhan.setFullScreen(ctx, true) })
                        ))
                    }
                    item { Spacer(Modifier.height((w * 0.12f).dp)) }
                }
                1 -> {
                    item {
                        OneUiRadioCard(w, Settings.patterns.map { (id, _) ->
                            RadioOpt(patName(id), starPattern == id, { Settings.savePattern(ctx, id) }, lead = {
                                Box(Modifier.size((w * 0.1f).dp).clip(CircleShape).background(Color.Black)) { StarField(Modifier.fillMaxSize(), id, 3f) }
                            })
                        })
                    }
                    item { Spacer(Modifier.height((w * 0.12f).dp)) }
                }
                2 -> {
                    item {
                        OneUiRadioCard(w, WTheme.defs.mapIndexed { i, d ->
                            RadioOpt(themeName(i), WTheme.idx == i, { Settings.saveTheme(ctx, i) }, lead = {
                                Box(Modifier.size((w * 0.09f).dp).clip(CircleShape).background(d.brush()))
                            })
                        })
                    }
                    item { Spacer(Modifier.height((w * 0.12f).dp)) }
                }
                3 -> {
                    item {
                        OneUiRadioCard(w, listOf(
                            RadioOpt(T("مزامنة مع الهاتف", "Sync with phone"), locMode == 0, onClick = { locMode = 0; Settings.saveLocMode(ctx, 0) }),
                            RadioOpt(T("تلقائي (GPS)", "Automatic (GPS)"), locMode == 1, onClick = {
                                locMode = 1; Settings.saveLocMode(ctx, 1)
                                if (!locOk) locLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION)) else refreshGps(ctx)
                            })
                        ))
                    }
                    item { Spacer(Modifier.height((w * 0.12f).dp)) }
                }
                5 -> {
                    item {
                        OneUiRadioCard(w, listOf(
                            RadioOpt(T("مزامنة (الافتراضي)", "Sync (default)"), Loc.mode(ctx) == 0 && langTick >= 0, onClick = { Loc.save(ctx, 0) }),
                            RadioOpt("عربي", Loc.mode(ctx) == 1 && langTick >= 0, onClick = { Loc.save(ctx, 1) }),
                            RadioOpt("English", Loc.mode(ctx) == 2 && langTick >= 0, onClick = { Loc.save(ctx, 2) })
                        ))
                    }
                    item { Spacer(Modifier.height((w * 0.12f).dp)) }
                }
                else -> {
                    item {
                        Column(Modifier.fillMaxWidth().padding(horizontal = (w * 0.027f).dp).clip(RoundedCornerShape((w * 0.09f).dp)).background(Color(0xFF26262B))) {
                            OneUiRow(w, T("الموقع", "Location"), if (locOk) T("مسموح", "Allowed") else T("غير مسموح", "Not allowed"), {
                                if (!locOk) locLauncher.launch(arrayOf(Manifest.permission.ACCESS_COARSE_LOCATION, Manifest.permission.ACCESS_FINE_LOCATION))
                            })
                            OneUiDivider(w)
                            OneUiRow(w, T("الإشعارات", "Notifications"), if (notifOk) T("مسموح", "Allowed") else T("غير مسموح", "Not allowed"), {
                                if (!notifOk) notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS)
                            })
                        }
                    }
                    item { Spacer(Modifier.height((w * 0.12f).dp)) }
                }
            }
        }
    }
}

@android.annotation.SuppressLint("MissingPermission")
fun refreshGps(c: Context) {
    try {
        if (ContextCompat.checkSelfPermission(c, Manifest.permission.ACCESS_COARSE_LOCATION) != PackageManager.PERMISSION_GRANTED) return
        val lm = c.getSystemService(Context.LOCATION_SERVICE) as LocationManager
        val l = lm.getProviders(true).mapNotNull { lm.getLastKnownLocation(it) }.maxByOrNull { it.time }
        if (l != null) Settings.saveGps(c, l.latitude, l.longitude)
    } catch (_: Throwable) {}
}
