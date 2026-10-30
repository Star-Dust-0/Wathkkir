package com.example.prayer

import android.content.Context
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.widthIn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Close
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.foundation.border
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EditLocation
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fajr.calculation.CalculationMethod
import com.example.fajr.calculation.PrayerAdjustments
import com.example.fajr.ui.CalculationMethodDialog
import com.example.fajr.ui.CitySearchDialog
import com.example.fajr.ui.FajrViewModel
import com.example.lockwidgets.LockWidgets
import kotlin.math.roundToInt
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDialog
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.SectionLabel
import com.example.widget.WidgetUpdater

/**
 * "Location & calculation" screen (3.1.0): location source + city, calculation method,
 * Asr juristic factor, high-latitude rule, per-prayer minute adjustments and Hijri offset.
 * Every change recalculates instantly: alarm reschedule + home/lock widget refresh.
 */
@Composable
fun LocationCalculationScreen(viewModel: FajrViewModel, onBack: () -> Unit) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context: Context = LocalContext.current
    var showCityDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var showAsrDialog by remember { mutableStateOf(false) }
    var showHighLatDialog by remember { mutableStateOf(false) }
    var prefsTick by remember { mutableIntStateOf(0) }

    LaunchedEffect(L10n.lang) { viewModel.refreshCityDisplay() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.updateLocationFromGps()
    }

    fun recalc() {
        prefsTick++
        viewModel.refreshFajrTimeManually()
        try { com.example.prayer.alerts.PrayerAlertScheduler.INSTANCE.scheduleAll(context) } catch (_: Throwable) {}
        try { WidgetUpdater.INSTANCE.updateAll(context) } catch (_: Throwable) {}
        try { LockWidgets.updateAll(context) } catch (_: Throwable) {}
        try { com.example.utils.WearSync.push(context) } catch (_: Throwable) {}
    }

    if (showCityDialog) {
        CitySearchDialog(
            onDismiss = { showCityDialog = false },
            onCitySelected = { viewModel.setCity(it); recalc() }
        )
    }
    if (showMethodDialog) {
        CalculationMethodDialog(
            currentMethod = state.calculationMethod,
            onDismiss = { showMethodDialog = false },
            onMethodSelected = { viewModel.setCalculationMethod(it); recalc() }
        )
    }
    if (showAsrDialog) {
        OptionPickerDialog(
            title = L10n.t(S.AsrJuristic),
            options = listOf(L10n.t(S.AsrShafii), L10n.t(S.AsrHanafi)),
            selected = if (PrayerAdjustments.INSTANCE.asrHanafi(context)) 1 else 0,
            onDismiss = { showAsrDialog = false },
            onPick = { PrayerAdjustments.INSTANCE.setAsrHanafi(context, it == 1); showAsrDialog = false; recalc() }
        )
    }
    if (showHighLatDialog) {
        OptionPickerDialog(
            title = L10n.t(S.HighLatRule),
            options = listOf(L10n.t(S.HighLatNone), L10n.t(S.HighLatMiddle), L10n.t(S.HighLatSeventh), L10n.t(S.HighLatAngle)),
            selected = PrayerAdjustments.INSTANCE.highLatRule(context),
            onDismiss = { showHighLatDialog = false },
            onPick = { PrayerAdjustments.INSTANCE.setHighLatRule(context, it); showHighLatDialog = false; recalc() }
        )
    }

    val cs = MaterialTheme.colorScheme
    OneUIScreen(title = L10n.t(S.LocCalcTitle), onBack = onBack) {
        // ---- A. Location ----
        item(key = "loc-label") { SectionLabel(L10n.t(S.LocSection)) }
        item(key = "loc") {
            OneUICard {
                LocOptionRow(
                    selected = state.useCurrentLocation,
                    icon = Icons.Outlined.GpsFixed,
                    title = L10n.t(S.CurrentLocation),
                    subtitle = L10n.t(S.AutoUpdateFajr),
                    onClick = {
                        viewModel.setUseCurrentLocation(true)
                        if (!state.hasLocationPermission) {
                            locationPermissionLauncher.launch(
                                arrayOf(
                                    "android.permission.ACCESS_FINE_LOCATION",
                                    "android.permission.ACCESS_COARSE_LOCATION"
                                )
                            )
                        }
                    }
                )
                LocOptionRow(
                    selected = !state.useCurrentLocation,
                    icon = Icons.Outlined.EditLocation,
                    title = L10n.t(S.PickCountryCity),
                    subtitle = L10n.t(S.PickCitySub),
                    onClick = { viewModel.setUseCurrentLocation(false) }
                )
                // Custom city name row (original): pin, hint + name, pencil -> inline editor with check / close.
                var editingName by remember { mutableStateOf(false) }
                var nameDraft by remember { mutableStateOf("") }
                val cityPrefs = remember { com.example.fajr.data.FajrPreferences(context) }
                Row(
                    modifier = Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 10.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(56.dp))
                    Icon(Icons.Outlined.LocationOn, null, tint = cs.primary)
                    Spacer(Modifier.width(10.dp))
                    if (!editingName) {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(L10n.t(S.CustomLocationNameHint), color = cs.onSurfaceVariant, fontSize = 13.sp)
                            Text(state.cityName, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Icon(
                            Icons.Outlined.Edit, L10n.t(S.EditCityName), tint = cs.primary.copy(alpha = 0.55f),
                            modifier = Modifier.size(47.dp / 1.0f).clip(CircleShape).background(cs.primary.copy(alpha = 0.07f))
                                .clickable { nameDraft = state.cityName; editingName = true }.padding(12.dp)
                        )
                    } else {
                        androidx.compose.foundation.text.BasicTextField(
                            value = nameDraft, onValueChange = { nameDraft = it }, singleLine = true,
                            textStyle = androidx.compose.ui.text.TextStyle(fontSize = 16.sp, color = cs.onSurface),
                            modifier = Modifier.weight(1f).border(1.dp, cs.outline, RoundedCornerShape(8.dp)).padding(horizontal = 14.dp, vertical = 14.dp)
                        )
                        Icon(
                            Icons.Outlined.Check, L10n.t(S.Save), tint = cs.primary,
                            modifier = Modifier.size(47.dp).clip(CircleShape).clickable {
                                if(viewModel.setCityAlias(nameDraft)) recalc()
                                editingName = false
                            }.padding(12.dp)
                        )
                        Icon(
                            Icons.Outlined.Close, L10n.t(S.Cancel), tint = cs.onSurfaceVariant,
                            modifier = Modifier.size(47.dp).clip(CircleShape).clickable { editingName = false }.padding(12.dp)
                        )
                    }
                }
                if (cityPrefs.cityNameArOrig != null) {
                    Text(L10n.t(S.ResetCityNameAuto),color=cs.primary,modifier=Modifier.padding(start=68.dp,top=4.dp,bottom=8.dp).clickable {viewModel.resetCityAlias();recalc()})
                }
                if (state.useCurrentLocation && (!state.hasLocationPermission || !state.isGpsAvailable)) {
                    Spacer(Modifier.height(8.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Warning, null, tint = cs.error)
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (!state.hasLocationPermission) L10n.t(S.NoLocPermission) else L10n.t(S.GpsOff),
                            color = cs.error,
                            fontSize = 12.sp
                        )
                    }
                }
            }
        }
        // ---- B. Calculation ----
        item(key = "calc-label") { SectionLabel(L10n.t(S.CalcSection)) }
        item(key = "calc") {
            OneUICard {
                CalcRow(
                    icon = Icons.Outlined.Tune,
                    title = L10n.t(S.CalcMethod),
                    value = state.calculationMethod.title(L10n.lang.code),
                    onClick = { showMethodDialog = true }
                )
                CalcRow(
                    icon = Icons.Outlined.Tune,
                    title = L10n.t(S.AsrJuristic),
                    value = L10n.t(if (remember(prefsTick) { PrayerAdjustments.INSTANCE.asrHanafi(context) }) S.AsrHanafi else S.AsrShafii),
                    onClick = { showAsrDialog = true }
                )
                CalcRow(
                    icon = Icons.Outlined.Tune,
                    title = L10n.t(S.HighLatRule),
                    value = L10n.t(
                        when (remember(prefsTick) { PrayerAdjustments.INSTANCE.highLatRule(context) }) {
                            1 -> S.HighLatMiddle
                            2 -> S.HighLatSeventh
                            3 -> S.HighLatAngle
                            else -> S.HighLatNone
                        }
                    ),
                    onClick = { showHighLatDialog = true }
                )
            }
        }
        // ---- C. Adjustments ----
        item(key = "adj-label") { SectionLabel(L10n.t(S.AdjSection)) }
        item(key = "adj") {
            OneUICard {
                Text(L10n.t(S.AdjNote), color = cs.onSurfaceVariant, fontSize = 12.sp)
                Spacer(Modifier.height(10.dp))
                val offsets = remember(prefsTick) { PrayerAdjustments.INSTANCE.offsetMinutes(context) }
                val names = listOf(S.PrayerFajr, S.PrayerShuruq, S.PrayerDhuhr, S.PrayerAsr, S.PrayerMaghrib, S.PrayerIsha)
                names.forEachIndexed { index, key ->
                    StepperRow(
                        title = L10n.t(key),
                        value = offsets[index],
                        unit = L10n.t(S.AdjMinShort),
                        onChange = { PrayerAdjustments.INSTANCE.setOffsetMinutes(context, index, it); recalc() }
                    )
                }
                Spacer(Modifier.height(6.dp))
                StepperRow(
                    title = L10n.t(S.AdjHijri),
                    icon = Icons.Outlined.DateRange,
                    value = remember(prefsTick) { PrayerAdjustments.INSTANCE.hijriOffsetDays(context) },
                    unit = L10n.t(S.AdjDayShort),
                    min = -2, max = 2,
                    onChange = { PrayerAdjustments.INSTANCE.setHijriOffsetDays(context, it); recalc() }
                )
            }
        }
    }
}

@Composable
private fun LocOptionRow(selected: Boolean, icon: ImageVector, title: String, subtitle: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(if (selected) cs.primary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Icon(icon, null, tint = cs.primary)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
            Text(subtitle, color = cs.onSurfaceVariant, fontSize = 11.sp)
        }
    }
}

@Composable
private fun CalcRow(icon: ImageVector, title: String, value: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .padding(vertical = 7.dp, horizontal = 4.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            Icon(icon, null, tint = cs.primary)
            Spacer(Modifier.width(8.dp))
            Column {
                Text(title, color = cs.onSurfaceVariant, fontSize = 11.sp)
                Text(value, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Bold)
            }
        }
        Text(L10n.t(S.Change), color = cs.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
    }
}

@Composable
private fun StepperRow(title: String, value: Int, unit: String, onChange: (Int) -> Unit, icon: ImageVector? = null, min: Int = -30, max: Int = 30) {
    val cs = MaterialTheme.colorScheme
    val view = androidx.compose.ui.platform.LocalView.current
    var live by remember(value) { mutableStateOf(value.toFloat()) }
    Column(Modifier.fillMaxWidth().padding(horizontal = 12.dp, vertical = 9.dp)) {
        Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween, verticalAlignment = Alignment.CenterVertically) {
            Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
                if (icon != null) {
                    Icon(icon, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                    Spacer(Modifier.width(8.dp))
                }
                Text(title, color = cs.onSurface, style = MaterialTheme.typography.bodyLarge)
            }
            val v = live.roundToInt()
            Text(
                text = "\u200e" + (if (v > 0) "+$v" else "$v") + " " + unit,
                color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium,
                modifier = Modifier.widthIn(min = 56.dp), textAlign = androidx.compose.ui.text.style.TextAlign.End
            )
        }
        androidx.compose.material3.Slider(
            value = live,
            onValueChange = {
                val n = it.roundToInt()
                if (n != live.roundToInt()) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
                live = it
            },
            onValueChangeFinished = { onChange(live.roundToInt()) },
            valueRange = min.toFloat()..max.toFloat(),
            steps = max - min - 1,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
private fun OptionPickerDialog(title: String, options: List<String>, selected: Int, onDismiss: () -> Unit, onPick: (Int) -> Unit) {
    val cs = MaterialTheme.colorScheme
    var pending by androidx.compose.runtime.remember { androidx.compose.runtime.mutableStateOf(selected) }
    OneUIDialog(title = title, onDismiss = onDismiss, onConfirm = { onPick(pending) }, confirmText = L10n.t(S.Apply)) {
        options.forEachIndexed { index, label ->
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(12.dp))
                    .clickable { pending = index }
                    .padding(12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(label, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                if (index == pending) {
                    Icon(Icons.Outlined.Check, null, tint = cs.primary)
                }
            }
        }
    }
}
