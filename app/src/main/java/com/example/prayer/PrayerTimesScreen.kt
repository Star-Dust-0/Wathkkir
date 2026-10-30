package com.example.prayer

import android.content.Context
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
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
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.FilterDrama
import androidx.compose.material.icons.outlined.Mosque
import androidx.compose.foundation.layout.offset
import com.example.ui.oneui.OneUIDimens
import androidx.compose.material.icons.outlined.LightMode
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.WbSunny
import androidx.compose.material.icons.outlined.WbTwilight
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableDoubleStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajr.calculation.CalculationMethod
import com.example.fajr.data.FajrPreferences
import androidx.compose.foundation.combinedClickable
import androidx.compose.material.icons.outlined.Brightness2
import androidx.compose.material.icons.outlined.Brightness3
import androidx.compose.material.icons.outlined.Brightness5
import androidx.compose.material.icons.outlined.Bedtime
import androidx.compose.material.icons.outlined.Explore
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material.icons.outlined.NotificationsOff
import com.example.prayer.alerts.PrayerAlertScheduler
import com.example.prayer.alerts.PrayerAlertStore
import com.example.fajr.calculation.PrayerAdjustments
import com.example.fajr.calculation.PrayerTimesCalculator
import com.example.fajr.ui.CitySearchDialog
import com.example.ui.oneui.oneUIGlass
import dev.chrisbanes.haze.hazeSource
import androidx.compose.material.icons.outlined.Settings
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIProgressBar
import com.example.ui.oneui.OneUIIconAction
import com.example.ui.oneui.OneUIScreen
import com.example.ThemeSettings
import com.example.ui.theme.OneUIActiveGreen
import com.example.ui.theme.OneUITimeCyan
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.i18n.Lang

@Composable
fun SavedCity(): String = FajrPreferences(LocalContext.current).cityName

@Composable
fun PrayerTimesScreen(onQibla: (() -> Unit)? = null, onMenu: (() -> Unit)? = null, onLocation: (() -> Unit)? = null) {
    val context: Context = LocalContext.current
    val prefs = remember { FajrPreferences(context) }
    var latitude by remember { mutableDoubleStateOf(prefs.latitude) }
    var longitude by remember { mutableDoubleStateOf(prefs.longitude) }
    var city by remember { mutableStateOf(prefs.cityName) }
    var country by remember { mutableStateOf(prefs.countryName) }
    var showCitySearch by remember { mutableStateOf(false) }
    var alertDialogPrayer by remember { mutableStateOf<Int?>(null) }
    var bellTick by remember { mutableIntStateOf(0) }
    // 1 Hz tick for the countdown only. The state object is passed down and read inside the
    // overview card, so only that card recomposes every second - not the whole screen.
    val nowState = remember { mutableLongStateOf(System.currentTimeMillis()) }
    LaunchedEffect(Unit) {
        while (true) {
            nowState.longValue = System.currentTimeMillis()
            delay(1000L)
        }
    }
    // Real refresh when returning from Settings / Location & calculation (city, method,
    // offsets, optional rows, bells) instead of relying on the per-second recomposition.
    var resumeTick by remember { mutableIntStateOf(0) }
    val lifecycleOwner = androidx.lifecycle.compose.LocalLifecycleOwner.current
    androidx.compose.runtime.DisposableEffect(lifecycleOwner) {
        val obs = androidx.lifecycle.LifecycleEventObserver { _, event ->
            if (event == androidx.lifecycle.Lifecycle.Event.ON_RESUME) resumeTick++
        }
        lifecycleOwner.lifecycle.addObserver(obs)
        onDispose { lifecycleOwner.lifecycle.removeObserver(obs) }
    }
    LaunchedEffect(resumeTick) {
        latitude = prefs.latitude
        longitude = prefs.longitude
        city = prefs.cityName
        country = prefs.countryName
    }

    val method = remember(resumeTick) { CalculationMethod.INSTANCE.fromId(prefs.calculationMethodId) }
    // Heavy astronomy math: once per day/location/method, not every second of countdown.
    val dayKey by remember {
        androidx.compose.runtime.derivedStateOf {
            val c = Calendar.getInstance().apply { timeInMillis = nowState.longValue }
            c.get(Calendar.YEAR) * 1000 + c.get(Calendar.DAY_OF_YEAR)
        }
    }
    val cal = remember(dayKey) { Calendar.getInstance() }
    val days = remember(dayKey, latitude, longitude, method, resumeTick) {
        val todayCal = Calendar.getInstance()
        val t = PrayerAdjustments.INSTANCE.calculateAll(context, todayCal, latitude, longitude, method)
        val tomorrowCal = todayCal.clone() as Calendar
        tomorrowCal.add(Calendar.DAY_OF_YEAR, 1)
        val tm = PrayerAdjustments.INSTANCE.calculateAll(context, tomorrowCal, latitude, longitude, method)
        val yesterdayCal = todayCal.clone() as Calendar
        yesterdayCal.add(Calendar.DAY_OF_YEAR, -1)
        val y = PrayerAdjustments.INSTANCE.calculateAll(context, yesterdayCal, latitude, longitude, method)
        Triple(t, tm, y)
    }
    val today = days.first
    val nightLen = (days.second.fajr - today.maghrib).coerceAtLeast(1L)
    val ordered: List<Triple<String, Long, ImageVector>> = listOf(
        Triple(L10n.t(S.PrayerFajr), today.fajr, Icons.Outlined.WbTwilight),
        Triple(L10n.t(S.PrayerShuruq), today.sunrise, Icons.Outlined.WbSunny),
        Triple(L10n.t(S.PrayerDhuhr), today.dhuhr, Icons.Outlined.LightMode),
        Triple(L10n.t(S.PrayerAsr), today.asr, Icons.Outlined.FilterDrama),
        Triple(L10n.t(S.PrayerMaghrib), today.maghrib, Icons.Outlined.NightsStay),
        Triple(L10n.t(S.PrayerIsha), today.isha, Icons.Outlined.DarkMode)
    )
    val isFriday = remember(dayKey) { Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY }
    fun arr(t: PrayerTimesCalculator.DayPrayerTimes) = longArrayOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha)
    val tsToday = remember(days) { arr(days.first) }
    val tsTomorrow = remember(days) { arr(days.second) }
    val tsYesterday = remember(days) { arr(days.third) }
    val phase by remember(days, isFriday) {
        androidx.compose.runtime.derivedStateOf {
            com.example.lockwidgets.PrayerPhase.resolve(tsToday, tsYesterday, tsTomorrow, nowState.longValue, isFriday, true)
        }
    }
    val prayerNames = remember(L10n.lang) {
        arrayOf(L10n.t(S.PrayerFajr), L10n.t(S.PrayerShuruq), L10n.t(S.PrayerDhuhr), L10n.t(S.PrayerAsr), L10n.t(S.PrayerMaghrib), L10n.t(S.PrayerIsha))
    }
    val phaseLabel = phase.label(prayerNames, L10n.lang.code)
    val next = Triple(phaseLabel, phase.target, Icons.Outlined.WbTwilight)
    val gregorian = remember(dayKey, L10n.lang) { if (L10n.lang == Lang.AR) SimpleDateFormat("EEEE، d MMMM yyyy", Locale.forLanguageTag("ar-u-nu-latn")).format(cal.time) else SimpleDateFormat("EEEE, d MMMM yyyy", Locale.forLanguageTag(L10n.lang.code)).format(cal.time) }
    val hijri = remember(dayKey, L10n.lang, resumeTick) {
        val off = PrayerAdjustments.INSTANCE.hijriOffsetDays(context)
        val hcal = if (off != 0) (cal.clone() as Calendar).apply { add(Calendar.DATE, off) } else cal
        HijriDate.format(hcal, L10n.lang.code)
    }

    val optionalOn = remember(resumeTick) {
        listOf("imsak", "duha", "midnight", "qiyam").map { PrayerAdjustments.INSTANCE.optionalRow(context, it) }
    }
    val displayRows = mutableListOf<Triple<String, Long, ImageVector>>()
    val rowBells = mutableListOf<Int?>()
    if (optionalOn[0]) {
        displayRows.add(Triple(L10n.t(S.Imsak), today.fajr - 10 * 60000L, Icons.Outlined.Brightness2)); rowBells.add(null)
    }
    ordered.forEachIndexed { i, t ->
        displayRows.add(t); rowBells.add(i)
        if (i == 1 && optionalOn[1]) {
            displayRows.add(Triple(L10n.t(S.Duha), today.sunrise + 15 * 60000L, Icons.Outlined.Brightness5)); rowBells.add(null)
        }
    }
    if (optionalOn[2]) {
        displayRows.add(Triple(L10n.t(S.Midnight), today.maghrib + nightLen / 2, Icons.Outlined.Bedtime)); rowBells.add(null)
    }
    if (optionalOn[3]) {
        displayRows.add(Triple(L10n.t(S.Qiyam), today.maghrib + nightLen * 2 / 3, Icons.Outlined.Brightness3)); rowBells.add(null)
    }

    PrayerTimesContent(
        city = city,
        country = country,
        hijriText = hijri,
        gregorianText = gregorian,
        nextPrayerName = next.first,
        nextPrayerTime = formatPrayerTime(next.second),
        prevMillis = phase.start,
        nextMillis = phase.target,
        nowState = nowState,
        iqama = phase.iqama,
        skyIndex = phase.previousIndex,
        highlightIndex = if (phase.iqama) -1 else phase.targetIndex,
        rows = displayRows.map { (name, time, icon) ->
            PrayerRowDisplay(name, formatPrayerTime(time), false, icon)
        },
        rowBells = rowBells,
        onChangeCity = { showCitySearch = true },
        onMenu = onMenu,
        onQibla = onQibla,
        bellStates = remember(bellTick, resumeTick, L10n.lang) { (0..5).map { PrayerAlertStore.INSTANCE.isEnabled(context, it) } },
        onBellClick = { idx ->
            PrayerAlertStore.INSTANCE.setEnabled(context, idx, !PrayerAlertStore.INSTANCE.isEnabled(context, idx))
            PrayerAlertScheduler.INSTANCE.scheduleAll(context)
            bellTick++
        },
        onBellLongClick = { idx -> alertDialogPrayer = idx }
    )


    alertDialogPrayer?.let { idx ->
        val names = listOf(S.PrayerFajr, S.PrayerShuruq, S.PrayerDhuhr, S.PrayerAsr, S.PrayerMaghrib, S.PrayerIsha)
        PrayerAlertDialog(
            prayer = idx,
            prayerName = L10n.t(names[idx]),
            onDismiss = { alertDialogPrayer = null; bellTick++ }
        )
    }

    if (showCitySearch) {
        CitySearchDialog(
            onDismiss = { showCitySearch = false },
            onCitySelected = { item ->
                prefs.latitude = item.latitude
                prefs.longitude = item.longitude
                prefs.cityName = item.nameAr
                prefs.countryName = item.countryAr
                prefs.setUseCurrentLocation(false)
                latitude = item.latitude
                longitude = item.longitude
                city = item.nameAr
                country = item.countryAr
                showCitySearch = false
            }
        )
    }
}

private fun formatPrayerTime(millis: Long): String {
    val cal = Calendar.getInstance()
    cal.timeInMillis = millis
    val ampm = if (cal.get(Calendar.AM_PM) == 0) L10n.t(S.AmMarker) else L10n.t(S.PmMarker)
    return SimpleDateFormat("hh:mm", Locale.US).format(cal.time) + " " + ampm
}

private fun formatCountdown(millis: Long): String {
    val totalSeconds = millis / 1000
    val h = totalSeconds / 3600
    val m = (totalSeconds % 3600) / 60
    val s = totalSeconds % 60
    return String.format("%d:%02d:%02d", h, m, s)
}

private val skyRes = intArrayOf(
    com.example.R.drawable.sky_fajr, com.example.R.drawable.sky_shuruq, com.example.R.drawable.sky_dhuhr,
    com.example.R.drawable.sky_asr, com.example.R.drawable.sky_maghrib, com.example.R.drawable.sky_isha
)

private val GlassBase = Color(0xFF17171B)
private val HighlightBg = Color(0xFFC2E8CE)
private val HighlightInk = Color(0xFF0D4A21)
private val HighlightCircle = Color(0xFFA4D5B2)
private val IqamaGold = Color(0xFFE5B455)
private val BarTrack = Color(0xFF38383E)
private val CountdownCyan = Color(0xFF59C8DC)

@Composable
private fun GlassCard(modifier: Modifier = Modifier, light: Boolean = false, content: @Composable androidx.compose.foundation.layout.ColumnScope.() -> Unit) {
    Column(
        modifier.fillMaxWidth().oneUIGlass(RoundedCornerShape(28.dp), if (light) Color.White else GlassBase, alpha = if (light) 0.55f else 0.46f, blurRadius = 24),
        content = content
    )
}

@OptIn(androidx.compose.foundation.ExperimentalFoundationApi::class)
@Composable
fun PrayerTimesContent(
    city: String,
    country: String,
    hijriText: String,
    gregorianText: String,
    nextPrayerName: String,
    nextPrayerTime: String,
    prevMillis: Long,
    nextMillis: Long,
    nowState: androidx.compose.runtime.State<Long>,
    rows: List<PrayerRowDisplay>,
    onChangeCity: () -> Unit,
    iqama: Boolean = false,
    skyIndex: Int = 5,
    highlightIndex: Int = -1,
    onMenu: (() -> Unit)? = null,
    onQibla: (() -> Unit)? = null,
    bellStates: List<Boolean> = List(6) { false },
    rowBells: List<Int?> = List(rows.size) { it },
    onBellClick: (Int) -> Unit = {},
    onBellLongClick: (Int) -> Unit = {}
) {
    // The prayer tab is always drawn on the dark sky scene, whatever the app theme is.
    // Original palette per sky phase: Fajr, Maghrib and Isha skies use light-on-dark; Shuruq, Dhuhr and Asr use dark ink on light glass.
    val lightSky = skyIndex.coerceIn(0, 5) in 1..3
    val outerHaze = com.example.ui.oneui.LocalHazeState.current
    val skyHaze = dev.chrisbanes.haze.rememberHazeState()
    androidx.compose.runtime.CompositionLocalProvider(com.example.ui.oneui.LocalHazeState provides skyHaze) {
    MaterialTheme(colorScheme = if (lightSky) com.example.ui.theme.ForcedLightScheme else com.example.ui.theme.ForcedDarkScheme, typography = MaterialTheme.typography, shapes = MaterialTheme.shapes) {
    val cs0 = MaterialTheme.colorScheme
    val cs = if (lightSky) cs0.copy(onSurface = Color(0xFF111114), onSurfaceVariant = Color(0xA81E1E22), primary = Color(0xFF0381FE)) else cs0
    val circleBg = if (lightSky) Color(0x6BFFFFFF) else Color.White.copy(alpha = 0.18f)
    val ovCircleBg = if (lightSky) Color(0x6BFFFFFF) else Color.White.copy(alpha = 0.12f)
    val dividerCol = if (lightSky) Color(0x1A000000) else Color.White.copy(alpha = 0.14f)
    val cdColor = if (lightSky) Color(0xFF1E8FA8) else CountdownCyan
    val trackCol = if (lightSky) Color(0xFFC4C4CD) else BarTrack
    val iconTint = if (lightSky) Color(0xFF111114) else Color.White.copy(alpha = 0.85f)
    val bellOffTint = if (lightSky) Color(0xFF111114).copy(alpha = 0.5f) else Color.White.copy(alpha = 0.5f)
    androidx.compose.runtime.SideEffect { com.example.ui.oneui.PrayerSky.mode = if (lightSky) 2 else 1 }
    androidx.compose.runtime.DisposableEffect(Unit) { onDispose { com.example.ui.oneui.PrayerSky.mode = 0 } }
    val twoCol = com.example.ui.oneui.isTwoPane()
    OneUIScreen(
        title = L10n.t(S.PrayerTimes),
        onMenu = onMenu,
        actions = {
            if (onQibla != null) OneUIIconAction(Icons.Outlined.Explore, L10n.t(S.QiblaTitle), onQibla)
            OneUIIconAction(Icons.Outlined.Settings, L10n.t(S.Settings), { com.example.ui.oneui.HeaderTools.nav?.navigate("settings") })
        },
        background = {
            androidx.compose.animation.Crossfade(
                targetState = skyIndex.coerceIn(0, 5),
                animationSpec = androidx.compose.animation.core.tween(1400),
                label = "sky"
            ) { i ->
                androidx.compose.foundation.Image(
                    painter = androidx.compose.ui.res.painterResource(skyRes[i]),
                    contentDescription = null,
                    contentScale = androidx.compose.ui.layout.ContentScale.Crop,
                    modifier = Modifier.fillMaxSize().hazeSource(skyHaze)
                        .then(if (outerHaze != null) Modifier.hazeSource(outerHaze) else Modifier)
                )
            }
        }
    ) {
        val overviewCard: @Composable () -> Unit = {
            GlassCard(light = lightSky) {
                Column(Modifier.padding(20.dp)) {
                    Text(hijriText, color = cs.onSurface, fontWeight = FontWeight.Bold, fontSize = 20.sp, lineHeight = 22.sp)
                    Spacer(Modifier.height(4.dp))
                    Text(gregorianText, color = cs.onSurfaceVariant, fontSize = 15.sp, lineHeight = 16.sp)
                    Spacer(Modifier.height(12.dp))
                    Row(
                        modifier = Modifier.clip(RoundedCornerShape(12.dp)).clickable(onClick = onChangeCity).padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.LocationOn, null, modifier = Modifier.size(40.dp).padding(11.dp), tint = cs.onSurfaceVariant)
                        Spacer(Modifier.width(6.dp))
                        Text(SavedCity(), color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelLarge)
                    }
                    OneUIDivider(0.dp)
                    Row(modifier = Modifier.fillMaxWidth().padding(vertical = 10.dp), verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier.size(40.dp).clip(CircleShape).background(ovCircleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(Icons.Outlined.Mosque, null, modifier = Modifier.size(22.dp), tint = OneUIActiveGreen)
                        }
                        Spacer(Modifier.width(12.dp))
                        Text(
                            if (iqama) com.example.lockwidgets.PrayerPhase.iqamaLabel(L10n.lang.code) else L10n.t(S.NextPrayer),
                            color = cs.onSurfaceVariant, style = MaterialTheme.typography.titleSmall
                        )
                    }
                    Spacer(Modifier.height(18.dp))
                    val now = nowState.value
                    val interval = (nextMillis - prevMillis).coerceAtLeast(1L)
                    val progressNow = ((now - prevMillis).toFloat() / interval.toFloat()).coerceIn(0f, 1f)
                    val remain = (nextMillis - now).coerceAtLeast(0L)
                    val countdown = if (iqama) formatMinSec(remain) else formatCountdown(remain)
                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.Bottom) {
                        com.example.ui.NativeCountdown(nextMillis, cdColor, 32f, bold = true)
                        Column(horizontalAlignment = Alignment.End) {
                            Text(nextPrayerName, color = cs.onSurface, fontWeight = FontWeight.Bold, style = MaterialTheme.typography.titleLarge)
                            Text(nextPrayerTime, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    OneUIProgressBar(
                        progressNow,
                        color = if (iqama) IqamaGold else OneUIActiveGreen,
                        trackColor = trackCol,
                        gradient = if (iqama) listOf(IqamaGold, IqamaGold) else listOf(Color(0xFF5BC950), Color(0xFF24C55E))
                    )
                }
            }
                }
        val listCard: @Composable () -> Unit = {
            GlassCard(light = lightSky) {
                rows.forEachIndexed { index, row ->
                    val bellIndex = rowBells.getOrNull(index)
                    val hl = bellIndex != null && bellIndex == highlightIndex
                    val prevHl = index > 0 && rowBells.getOrNull(index - 1).let { it != null && it == highlightIndex }
                    if (index > 0 && !hl && !prevHl) {
                        Box(Modifier.fillMaxWidth().padding(horizontal = 16.dp).height(1.dp).background(dividerCol))
                    }
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp, vertical = 4.35.dp)
                            .clip(RoundedCornerShape(29.dp))
                            .background(if (hl) HighlightBg else Color.Transparent)
                            .padding(start = 12.dp, end = 6.dp, top = 5.dp, bottom = 5.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            modifier = Modifier.size(38.dp).clip(CircleShape)
                                .background(if (hl) HighlightCircle else circleBg),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(row.icon, null, modifier = Modifier.size(20.dp), tint = if (hl) HighlightInk else iconTint)
                        }
                        Spacer(Modifier.width(14.dp))
                        Text(row.name, color = if (hl) HighlightInk else cs.onSurface, fontWeight = if (hl) FontWeight.ExtraBold else FontWeight.Normal, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.weight(1f))
                        val bellOn = bellIndex != null && bellStates.getOrElse(bellIndex) { false }
                        Text(row.timeText, color = if (hl) HighlightInk else cs.onSurface, fontWeight = if (hl) FontWeight.ExtraBold else FontWeight.Normal, style = MaterialTheme.typography.bodyLarge)
                        Spacer(Modifier.width(2.dp))
                        Box(
                            modifier = Modifier.size(48.dp).clip(CircleShape).then(
                                if (bellIndex != null) Modifier.combinedClickable(onClick = { onBellClick(bellIndex) }, onLongClick = { onBellLongClick(bellIndex) }) else Modifier
                            ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (bellIndex != null) {
                                Icon(
                                    if (bellOn) Icons.Outlined.NotificationsActive else Icons.Outlined.NotificationsOff,
                                    contentDescription = "bell_" + bellIndex,
                                    modifier = Modifier.size(19.dp),
                                    tint = if (bellOn) cs.primary else if (hl) HighlightInk.copy(alpha = 0.5f) else bellOffTint
                                )
                            }
                        }
                    }
                }
            }
                }
        if (twoCol) {
            // Large screens: overview on one side, prayer list on the other, equal widths.
            item(key = "prayer-wide") {
                Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(16.dp), verticalAlignment = Alignment.Top) {
                    Box(Modifier.weight(1f)) { overviewCard() }
                    Box(Modifier.weight(1f)) { listCard() }
                }
            }
        } else {
            item(key = "prayer-overview") { overviewCard() }
            item(key = "prayer-list") { listCard() }
        }
    }
    }
}

}

private fun formatMinSec(millis: Long): String {
    val t = millis / 1000
    return String.format("%02d:%02d", t / 60, t % 60)
}

data class PrayerRowDisplay(
    val name: String,
    val timeText: String,
    val isNext: Boolean,
    val icon: androidx.compose.ui.graphics.vector.ImageVector
)
