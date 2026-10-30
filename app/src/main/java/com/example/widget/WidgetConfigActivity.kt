package com.example.widget

import android.appwidget.AppWidgetManager
import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.foundation.background
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.foundation.border
import com.example.utils.clickable
import com.example.utils.plainClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import com.example.ui.oneui.Text
import androidx.compose.runtime.Composable
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.foundation.layout.width
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S


/** Progress-bar colour choices: key stored in preferences, Arabic label, swatch colour. */
private val options: List<Pair<String, Pair<String, Color>>> = listOf(
    "auto" to (L10n.t(S.Auto) to Color(0xFFA6A6A6)),
    "amber" to (L10n.t(S.ColorAmber) to Color(0xFFE5B455)),
    "blue" to (L10n.t(S.ColorBlue) to Color(0xFF3E91FF)),
    "green" to (L10n.t(S.ColorGreen) to Color(0xFF34C759)),
    "purple" to (L10n.t(S.ColorPurple) to Color(0xFF9747FF))
)

private val bgOptions: List<Pair<String, Pair<String, Color>>> = listOf(
    "auto" to (L10n.t(S.Auto) to Color(0xFF171719)),
    "black" to (L10n.t(S.ColorBlack) to Color(0xFF000000)),
    "gray" to (L10n.t(S.ColorGray) to Color(0xFF2B2B2B)),
    "white" to (L10n.t(S.ColorWhite) to Color(0xFFF6F6F6)),
    // v3.2.0 (his additions 1 & 2): frosted glass + blurred-wallpaper backgrounds.
    "glass" to (L10n.t(S.BgGlass) to Color(0xB3F2F2F7)),
    "frosted" to (L10n.t(S.BgFrosted) to Color(0xFF9AA7C7)),
    "wallblur" to (L10n.t(S.BgWallBlur) to Color(0xFF7E7EAA))
)

// Decorated OneUI widget accent choices (3.3.3): default yellow plus the watch app palette.
// Keys are the theme index as a string so ColorSection can be reused unchanged.
private val accentOptions: List<Pair<String, Pair<String, Color>>> = listOf(
    "0" to (L10n.t(S.ColorYellow) to Color(0xFFFFD85C)),
    "1" to (L10n.t(S.ColorTurquoise) to Color(0xFF44CCC0)),
    "2" to (L10n.t(S.ColorBlue) to Color(0xFF3E9FFE)),
    "3" to (L10n.t(S.ColorGreen) to Color(0xFF69D043))
)

@Composable
fun ColorSection(title: String, options: List<Pair<String, Pair<String, Color>>>, current: String, onPick: (String) -> Unit, splitAutoSwatch: Boolean = false, captions: Map<String, String> = emptyMap()) {
    val cs = MaterialTheme.colorScheme
    Text(title, color = cs.onSurfaceVariant, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    options.forEach { (key, labelAndColor) ->
        val (label, swatch) = labelAndColor
        val selected = key == current
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(if (selected) cs.surfaceVariant else Color.Transparent)
                .clickable { onPick(key) }
                .padding(horizontal = 12.dp, vertical = 10.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // v3.2.0 (his call): the bar's Auto is contrast-based (black bar on a light
            // widget background, white on dark) - the swatch shows both halves.
            if (splitAutoSwatch && key == "auto") {
                Box(
                    modifier = Modifier
                        .size(22.dp)
                        .clip(CircleShape)
                        .then(if (selected) Modifier.border(2.dp, cs.primary, CircleShape) else Modifier)
                ) {
                    Row(Modifier.fillMaxSize()) {
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color.Black))
                        Box(Modifier.weight(1f).fillMaxHeight().background(Color.White))
                    }
                }
            } else Box(
                modifier = Modifier
                    .size(22.dp)
                    .clip(CircleShape)
                    .background(swatch)
                    .then(if (selected) Modifier.border(2.dp, cs.primary, CircleShape) else Modifier.border(1.dp, cs.outline.copy(alpha = 0.7f), CircleShape))
            )
            Column {
                Text(
                    label,
                    color = if (selected) cs.onSurface else cs.onSurfaceVariant,
                    fontSize = 15.sp,
                    fontWeight = if (selected) FontWeight.Bold else FontWeight.Normal
                )
                captions[key]?.let { Text(it, color = cs.onSurfaceVariant, fontSize = 12.sp) }
            }
        }
    }
}

@Composable
fun PatternSection(selected: Int, onPick: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    val night = isSystemInDarkTheme()
    Text(L10n.t(S.WidgetPattern), color = cs.onSurfaceVariant, fontSize = 13.sp)
    Spacer(Modifier.height(8.dp))
    val ids = listOf(0) + com.example.lockwidgets.WidgetPatterns.IDS.toList()
    Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
      for (rowIds in ids.chunked(3)) Row(horizontalArrangement = Arrangement.spacedBy(10.dp)) {
        for (id in rowIds) {
            val sel = selected == id
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.plainClickable { onPick(id) }) {
                Box(
                    Modifier.size(56.dp).clip(RoundedCornerShape(14.dp))
                        .border(if (sel) 2.5.dp else 1.dp, if (sel) Color(0xFF377AFF) else cs.outlineVariant, RoundedCornerShape(14.dp)),
                    contentAlignment = Alignment.Center
                ) {
                    if (id == 0) Text(L10n.t(S.NoPattern), color = cs.onSurfaceVariant, fontSize = 12.sp)
                    else androidx.compose.foundation.Image(remember(id, night) { com.example.lockwidgets.WidgetPatterns.thumb(id, 168, night).asImageBitmap() }, null, Modifier.fillMaxSize())
                }
            }
        }
      }
    }
}

@Composable
fun ConfigScreen(current: String, currentBg: String, currentPattern: Int = 1, currentAccent: Int = 0, currentTapOpens: Boolean = true, showColors: Boolean = true, showPattern: Boolean = true, onApply: (String, String, Int, Int, Boolean) -> Unit, onNeedFilesPermission: () -> Unit = {}) {
    val cs = MaterialTheme.colorScheme
    // Taps only select; nothing is saved or closed until Apply (One UI style).
    var barSel by remember { mutableStateOf(current) }
    var bgSel by remember { mutableStateOf(currentBg) }
    var patSel by remember { mutableStateOf(currentPattern) }
    var accentSel by remember { mutableStateOf(currentAccent.toString()) }
    var tapOpens by remember { mutableStateOf(currentTapOpens) }
    Box(
        modifier = Modifier.fillMaxSize().background(cs.background).padding(24.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(24.dp))
                .background(cs.surface)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f, fill = false)
                    .verticalScroll(rememberScrollState())
                    .padding(start = 20.dp, top = 20.dp, end = 20.dp, bottom = 8.dp)
            ) {
                Text(L10n.t(S.WidgetSettings), color = cs.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))
                // The decorated OneUI widget has fixed bar/bg colours but a selectable accent
                // (3.3.3): the accent section goes ABOVE the pattern section (his call 22.10.2026).
                if (showColors) {
                    ColorSection(L10n.t(S.BarColor), options, barSel, { barSel = it }, splitAutoSwatch = true)
                    Spacer(Modifier.height(16.dp))
                    ColorSection(L10n.t(S.BgColor), bgOptions, bgSel, { bgSel = it; if (it == "wallblur") onNeedFilesPermission() }, captions = mapOf("wallblur" to L10n.t(S.NeedsFilesPerm)))
                    Spacer(Modifier.height(16.dp))
                }
                if (showPattern) {
                    ColorSection(L10n.t(S.WidgetAccent), accentOptions, accentSel, { accentSel = it })
                    Spacer(Modifier.height(16.dp))
                }
                // Only the decorated OneUI widget draws patterns; other widgets get colours only.
                if (showPattern) PatternSection(patSel) { patSel = it }
                // 3.4: what a tap on this Home widget does (per widget): open the app or nothing.
                Spacer(Modifier.height(16.dp))
                Text(L10n.t(S.WidgetTapTitle), color = cs.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(8.dp))
                com.example.ui.oneui.OneUISegmented(
                    options = listOf(L10n.t(S.WidgetTapOpenApp), L10n.t(S.WidgetTapNothing)),
                    selected = if (tapOpens) 0 else 1,
                    onSelect = { tapOpens = it == 0 }
                )
            }
            // One UI "Scan phone" style: one centered filled blue pill. Back discards.
            Box(Modifier.fillMaxWidth().padding(top = 12.dp, bottom = 24.dp), contentAlignment = Alignment.Center) {
                Box(
                    modifier = Modifier
                        .width(160.dp)
                        .height(52.dp)
                        .clip(RoundedCornerShape(26.dp))
                        .background(Color(0xFF377AFF))
                        .plainClickable { onApply(barSel, bgSel, patSel, accentSel.toIntOrNull() ?: 0, tapOpens) },
                    contentAlignment = Alignment.Center
                ) {
                    Text(L10n.t(S.Apply), color = Color.White, fontSize = 17.sp, fontWeight = FontWeight.Bold)
                }
            }
        }
    }
}

/** Configure screen shown when a widget is added: picks the progress-bar colour. */
class WidgetConfigActivity : ComponentActivity() {
    private var appWidgetId = 0

    private fun keyFor(base: String) = if (appWidgetId > 0) base + "_" + appWidgetId else base

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        L10n.init(this)
        setResult(RESULT_CANCELED)
        appWidgetId = intent?.getIntExtra("appWidgetId", 0) ?: 0
        val prov = if (appWidgetId > 0) AppWidgetManager.getInstance(this).getAppWidgetInfo(appWidgetId)?.provider?.className else null
        val decorated = prov == "com.example.widget.OneUIWidgetProvider" || intent.getBooleanExtra("debugOneUi", false)
        // 3.4: these home widgets had no settings screen; they get one with only the tap option (no fake colour controls).
        val tapOnly = prov in setOf(
            "com.example.widget.NextPrayerWidgetProvider", "com.example.widget.DayPrayerWidgetProvider",
            "com.example.widget.GregorianWidgetProvider", "com.example.widget.HijriWidgetProvider",
            "com.example.widget.PillWidgetProvider"
        )
        val sp = getSharedPreferences("fajr_prefs", MODE_PRIVATE)
        val curBar = (sp.getString(keyFor("widget_bar_color"), null)
            ?: sp.getString("widget_bar_color", "auto") ?: "auto").let { v -> if (options.any { o -> o.first == v }) v else "auto" }
        val curBg = (sp.getString(keyFor("widget_bg_color"), null)
            ?: sp.getString("widget_bg_color", "auto") ?: "auto").let { v -> if (bgOptions.any { o -> o.first == v }) v else "auto" }
        // Back on a NEW widget still adds it with its default values; on an existing one it only discards.
        val isNew = appWidgetId > 0 && !sp.contains(keyFor("widget_bar_color")) && !sp.contains(keyFor("widget_bg_color"))
        onBackPressedDispatcher.addCallback(this, object : androidx.activity.OnBackPressedCallback(true) {
            override fun handleOnBackPressed() {
                android.util.Log.i("WidgetConfig", "back isNew=" + isNew + " id=" + appWidgetId)
                if (isNew) {
                    WidgetUpdater.INSTANCE.updateAll(this@WidgetConfigActivity)
                    com.example.lockwidgets.HomeWidgets.updateAll(this@WidgetConfigActivity)
                    setResult(RESULT_OK, Intent().putExtra("appWidgetId", appWidgetId))
                }
                finish()
            }
        })
        setContent {
            com.example.ui.theme.MyApplicationTheme {
                ConfigScreen(
                    current = curBar,
                    currentBg = curBg,
                    currentPattern = sp.getInt(keyFor("widget_pattern"), 1),
                    currentAccent = sp.getInt(keyFor("widget_oneui_color"), 0),
                    showColors = !decorated && !tapOnly,
                    showPattern = decorated,
                    onNeedFilesPermission = { com.example.ui.PermissionFlow.requestFilesIfNeeded(this@WidgetConfigActivity) },
                    currentTapOpens = sp.getString(keyFor("widget_tap"), "open") != "none",
                    onApply = { bar, bg, pat, accent, tap ->
                        val ed = sp.edit()
                        if (!tapOnly) ed.putString(keyFor("widget_bar_color"), bar).putString(keyFor("widget_bg_color"), bg).putInt(keyFor("widget_pattern"), pat).putInt(keyFor("widget_oneui_color"), accent)
                        ed.putString(keyFor("widget_tap"), if (tap) "open" else "none").apply()
                        WidgetUpdater.INSTANCE.updateAll(this@WidgetConfigActivity)
                        com.example.lockwidgets.HomeWidgets.updateAll(this@WidgetConfigActivity)
                        setResult(RESULT_OK, Intent().putExtra("appWidgetId", appWidgetId))
                        finish()
                    }
                )
            }
        }
    }
}
