package com.example.ui
import com.example.utils.plainClickable

import android.Manifest
import android.content.Context
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.safeDrawingPadding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import com.example.fajr.calculation.PrayerAdjustments
import com.example.fajr.ui.CalculationMethodDialog
import com.example.fajr.ui.CitySearchDialog
import com.example.fajr.ui.FajrViewModel
import com.example.prayer.alerts.PrayerAlertScheduler
import com.example.prayer.alerts.PrayerAlertStore
import com.example.ui.i18n.L10n
import com.example.ui.i18n.Lang
import com.example.ui.oneui.OneUISwitch
import kotlinx.coroutines.delay
import java.util.Locale
import java.util.TimeZone

/**
 * First-launch welcome: language, how to get prayer times, calculation, location check,
 * per-prayer alerts, reminder type, then the notification and files permissions.
 * Shown once, only on a fresh install (anyone who already went through the old permission flow skips it).
 */
object Onboarding {
    private const val PREFS = "app_prefs"
    const val KEY_DONE = "onboarding_done"

    fun isPending(c: Context): Boolean {
        val sp = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        if (sp.getBoolean(KEY_DONE, false)) return false
        if (sp.getBoolean("perm_flow_done", false) || sp.contains("perm_stage")) return false
        return true
    }

    private fun t(ar: String, en: String) = if (L10n.lang == Lang.AR) ar else en

    private const val P_LANG = 0
    private const val P_METHOD = 1
    private const val P_LOCATING = 2
    private const val P_CALC = 3
    private const val P_CONFIRM = 4
    private const val P_ALERTS = 5
    private const val P_REMINDER = 6
    private const val DOTS = 7

    private fun deviceLang(): Lang {
        val code = Locale.getDefault().language
        return Lang.entries.firstOrNull { it.code == code } ?: Lang.EN
    }

    @Composable
    fun Host(onFinished: () -> Unit) {
        val ctx = LocalContext.current
        val sp = remember { ctx.getSharedPreferences(PREFS, Context.MODE_PRIVATE) }
        val vm: FajrViewModel = viewModel()
        val state by vm.uiState.collectAsStateWithLifecycle()
        val hadLang = remember { sp.contains("language") }
        var page by remember { mutableIntStateOf(sp.getInt("onb_page", P_LANG).coerceIn(0, DOTS - 1)) }
        var showCity by remember { mutableStateOf(false) }
        var showMethod by remember { mutableStateOf(false) }
        var viaGps by remember { mutableStateOf(false) }
        var hanafi by remember { mutableStateOf(PrayerAdjustments.asrHanafi(ctx)) }
        var reminder by remember { mutableIntStateOf(0) }
        var deviceSel by remember { mutableStateOf(!hadLang) }
        val alerts = remember { mutableStateListOf(true, true, true, true, true) }
        val prayerIdx = intArrayOf(0, 2, 3, 4, 5)

        fun go(p: Int) { page = p; sp.edit().putInt("onb_page", p).apply() }
        val notifLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { _ ->
            sp.edit().putBoolean(KEY_DONE, true).apply()
            onFinished()
        }
        fun finish() {
            for (i in prayerIdx.indices) PrayerAlertStore.setEnabled(ctx, prayerIdx[i], alerts[i])
            com.example.utils.NotifPrefs.setVibration(ctx, reminder == 0)
            try { PrayerAlertScheduler.scheduleAll(ctx) } catch (_: Throwable) {}
            // the location step was handled here, so the old flow starts at the files step
            sp.edit().putInt("perm_stage", 2).apply()
            if (Build.VERSION.SDK_INT >= 33) {
                try { notifLauncher.launch(Manifest.permission.POST_NOTIFICATIONS) } catch (_: Throwable) {
                    sp.edit().putBoolean(KEY_DONE, true).apply(); onFinished()
                }
            } else {
                sp.edit().putBoolean(KEY_DONE, true).apply(); onFinished()
            }
        }
        val locLauncher = rememberLauncherForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { r ->
            if (r.values.any { it }) { viaGps = true; go(P_LOCATING) } else { showCity = true }
        }

        val dir = if (L10n.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
        val cs = MaterialTheme.colorScheme
        CompositionLocalProvider(LocalLayoutDirection provides dir) {
            Box(
                Modifier.fillMaxSize().background(cs.background)
                    .clickable(interactionSource = remember { MutableInteractionSource() }, indication = null) {}
            ) {
                Column(Modifier.fillMaxSize().safeDrawingPadding().padding(horizontal = 24.dp)) {
                    Box(Modifier.weight(1f).fillMaxWidth(), contentAlignment = Alignment.Center) {
                        Column(Modifier.fillMaxWidth().verticalScroll(rememberScrollState()), horizontalAlignment = Alignment.CenterHorizontally) {
                            when (page) {
                                P_LANG -> {
                                    Title(t("اختر اللغة", "Choose your language"))
                                    Choice(t("لغة الجهاز", "Device language"), deviceSel) {
                                        deviceSel = true; L10n.set(ctx, deviceLang())
                                        sp.edit().remove("language").apply()
                                    }
                                    Lang.entries.forEach { l ->
                                        Choice(l.nativeName, !deviceSel && L10n.lang == l) { deviceSel = false; L10n.set(ctx, l) }
                                    }
                                }
                                P_METHOD -> {
                                    Title(t("احصل على مواقيت الصلاة", "Get your prayer times"))
                                    Section(t("مواقيت حسب الموقع", "Prayer times by location"), t("حساب المواقيت لأي مكان", "Calculate prayer times for any place"))
                                    PillButton(t("تحديد الموقع (GPS)", "Use my location (GPS)")) {
                                        try { locLauncher.launch(arrayOf(Manifest.permission.ACCESS_FINE_LOCATION, Manifest.permission.ACCESS_COARSE_LOCATION)) } catch (_: Throwable) { showCity = true }
                                    }
                                    PillButton(t("اختيار المدينة يدويًا", "Choose a city manually")) { showCity = true }
                                }
                                P_LOCATING -> {
                                    Title(t("جاري تحديد موقعك...", "Getting current location..."))
                                    CircularProgressIndicator(color = cs.primary)
                                    Spacer(Modifier.height(32.dp))
                                    TextButton(onClick = com.example.utils.AppHaptics.tap { go(P_METHOD) }) { Text(t("إلغاء", "Cancel"), color = cs.onBackground, fontSize = 18.sp) }
                                    LaunchedEffect(Unit) {
                                        val lat0 = state.latitude
                                        vm.updateLocationFromGps()
                                        var n = 0
                                        while (n < 24 && vm.uiState.value.latitude == lat0) { delay(500); n++ }
                                        delay(400)
                                        if (page == P_LOCATING) go(P_CALC)
                                    }
                                }
                                P_CALC -> {
                                    Title(t("طريقة الحساب", "Set calculation method"))
                                    Field(t("طريقة الحساب", "Calculation method"), if (L10n.lang == Lang.AR) state.calculationMethod.titleAr else state.calculationMethod.titleEn) { showMethod = true }
                                    Spacer(Modifier.height(8.dp))
                                    Text(t("حساب وقت العصر", "Asr calculation"), color = cs.onSurfaceVariant, fontSize = 15.sp, modifier = Modifier.fillMaxWidth().padding(start = 8.dp, bottom = 6.dp))
                                    Choice(t("شافعي ومالكي وحنبلي", "Shafi'i, Maliki, Hanbali"), !hanafi) { hanafi = false; PrayerAdjustments.setAsrHanafi(ctx, false) }
                                    Choice(t("حنفي", "Hanafi"), hanafi) { hanafi = true; PrayerAdjustments.setAsrHanafi(ctx, true) }
                                }
                                P_CONFIRM -> {
                                    Title(t("تأكد من ضبط موقعك", "Make sure your location is set correctly"))
                                    Field(t("اسم المكان", "Location name"), state.cityName.ifBlank { "-" }) { showCity = true }
                                    Spacer(Modifier.height(8.dp))
                                    Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
                                        Box(Modifier.weight(1f)) { Field(t("خط العرض", "Latitude"), "%.5f".format(Locale.US, state.latitude), null) }
                                        Box(Modifier.weight(1f)) { Field(t("خط الطول", "Longitude"), "%.5f".format(Locale.US, state.longitude), null) }
                                    }
                                    Spacer(Modifier.height(8.dp))
                                    val tz = TimeZone.getDefault()
                                    Field(t("المنطقة الزمنية", "Time zone"), tz.id + "  " + tz.getDisplayName(tz.inDaylightTime(java.util.Date()), TimeZone.SHORT, Locale.US), null)
                                }
                                P_ALERTS -> {
                                    Title(t("تفعيل التنبيهات", "Enable notifications"))
                                    val names = if (L10n.lang == Lang.AR) listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء") else listOf("Fajr", "Dhuhr", "Asr", "Maghrib", "Isha")
                                    names.forEachIndexed { i, n -> SwitchRow(n, alerts[i]) { alerts[i] = it } }
                                }
                                P_REMINDER -> {
                                    Title(t("نوع التذكير", "Reminder type"))
                                    Choice(t("إشعار بصوت واهتزاز", "Notification with sound and vibration"), reminder == 0) { reminder = 0 }
                                    Choice(t("إشعار بصوت فقط", "Notification with sound only"), reminder == 1) { reminder = 1 }
                                }
                            }
                        }
                    }
                    if (page != P_LOCATING) {
                        Dots(page)
                        Row(Modifier.fillMaxWidth().padding(top = 8.dp, bottom = 12.dp), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
                            if (page == P_LANG) Spacer(Modifier.width(1.dp)) else NavText(t("رجوع", "Back")) {
                                go(when (page) { P_CALC -> P_METHOD; else -> page - 1 })
                            }
                            if (page != P_METHOD) NavText(if (page == P_REMINDER) t("إنهاء", "Finish") else t("التالي", "Next")) {
                                if (page == P_REMINDER) finish() else go(page + 1)
                            }
                        }
                    }
                }
            }
            if (showCity) CitySearchDialog(onDismiss = { showCity = false }, onCitySelected = { vm.setCity(it); showCity = false; go(P_CALC) })
            if (showMethod) CalculationMethodDialog(currentMethod = state.calculationMethod, onDismiss = { showMethod = false }, onMethodSelected = { vm.setCalculationMethod(it); showMethod = false })
        }
    }

    @Composable
    private fun Title(text: String) {
        Text(text, color = MaterialTheme.colorScheme.onBackground, fontSize = 28.sp, fontWeight = FontWeight.Bold, textAlign = TextAlign.Center, lineHeight = 36.sp,
            modifier = Modifier.fillMaxWidth().padding(vertical = 28.dp))
    }

    @Composable
    private fun Section(title: String, sub: String) {
        Column(Modifier.fillMaxWidth().padding(horizontal = 8.dp, vertical = 6.dp)) {
            Text(title, color = MaterialTheme.colorScheme.onBackground, fontSize = 20.sp, fontWeight = FontWeight.SemiBold)
            Text(sub, color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 15.sp)
        }
    }

    private val capsule = RoundedCornerShape(50)

    @Composable
    private fun Choice(label: String, selected: Boolean, onClick: () -> Unit) {
        val cs = MaterialTheme.colorScheme
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp).height(56.dp).clip(capsule)
                .background(if (selected) cs.primaryContainer else cs.surface)
                .border(BorderStroke(1.dp, if (selected) cs.primary else cs.outline), capsule)
                .clickable(onClick = onClick).padding(horizontal = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            RadioButton(selected = selected, onClick = null)
            Spacer(Modifier.width(12.dp))
            Text(label, color = cs.onSurface, fontSize = 18.sp, modifier = Modifier.weight(1f))
        }
    }

    @Composable
    private fun SwitchRow(label: String, checked: Boolean, onChange: (Boolean) -> Unit) {
        val cs = MaterialTheme.colorScheme
        Row(
            Modifier.fillMaxWidth().padding(vertical = 5.dp).height(56.dp).clip(capsule)
                .background(cs.surface).border(BorderStroke(1.dp, cs.outline), capsule)
                .padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(label, color = cs.onSurface, fontSize = 18.sp, modifier = Modifier.weight(1f))
            OneUISwitch(checked = checked, onCheckedChange = onChange)
        }
    }

    @Composable
    private fun PillButton(label: String, onClick: () -> Unit) {
        val cs = MaterialTheme.colorScheme
        Box(
            Modifier.fillMaxWidth().padding(vertical = 5.dp).height(56.dp).clip(capsule)
                .background(cs.surface).border(BorderStroke(1.dp, cs.outline), capsule).clickable(onClick = onClick),
            contentAlignment = Alignment.Center
        ) { Text(label, color = cs.primary, fontSize = 18.sp, fontWeight = FontWeight.Medium) }
    }

    @Composable
    private fun Field(label: String, value: String, onClick: (() -> Unit)? = {}) {
        val cs = MaterialTheme.colorScheme
        Column(
            Modifier.fillMaxWidth().padding(vertical = 4.dp).clip(RoundedCornerShape(20.dp))
                .background(cs.surface).border(BorderStroke(1.dp, cs.outline), RoundedCornerShape(20.dp))
                .let { if (onClick != null) it.plainClickable(onClick = onClick) else it }
                .padding(horizontal = 16.dp, vertical = 12.dp)
        ) {
            Text(label, color = cs.onSurfaceVariant, fontSize = 13.sp)
            Text(value, color = cs.onSurface, fontSize = 17.sp, maxLines = 2)
        }
    }

    @Composable
    private fun NavText(label: String, onClick: () -> Unit) {
        TextButton(onClick = com.example.utils.AppHaptics.tap(onClick)) { Text(label, color = MaterialTheme.colorScheme.primary, fontSize = 20.sp, fontWeight = FontWeight.Bold) }
    }

    /** One UI style page indicator: small dots, the current page is a longer pill in the accent colour. */
    @Composable
    private fun Dots(page: Int) {
        val cs = MaterialTheme.colorScheme
        val shown = if (page > P_LOCATING) page - 1 else if (page == P_LOCATING) 1 else page
        Row(Modifier.fillMaxWidth().padding(top = 8.dp), horizontalArrangement = Arrangement.Center, verticalAlignment = Alignment.CenterVertically) {
            for (i in 0 until DOTS - 1) {
                val w by animateDpAsState(if (i == shown) 22.dp else 8.dp, label = "dot")
                Box(Modifier.padding(horizontal = 4.dp).width(w).height(8.dp).clip(CircleShape).background(if (i == shown) cs.primary else cs.outline))
            }
        }
    }
}
