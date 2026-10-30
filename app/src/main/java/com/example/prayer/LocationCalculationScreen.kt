package com.example.prayer
import com.example.utils.plainClickable

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
import androidx.compose.material.icons.outlined.DateRange
import androidx.compose.material.icons.outlined.EditLocation
import androidx.compose.material.icons.outlined.GpsFixed
import androidx.compose.material.icons.outlined.LocationOn
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Tune
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.OutlinedTextField
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
    var editingCity by remember { mutableStateOf(false) }
    var cityEditText by remember { mutableStateOf("") }
    val aliasVersion by viewModel.aliasVersion.collectAsStateWithLifecycle()
    val cityAliased = remember(state.cityName, aliasVersion) { viewModel.isCurrentPlaceAliased() }

    LaunchedEffect(L10n.lang) { viewModel.refreshCityDisplay() }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        if (permissions.values.any { it }) viewModel.updateLocationFromGps()
    }

    fun recalc() {
        prefsTick++
        viewModel.refreshFajrTimeManually()
        try { com.example.prayer.alerts.PrayerAlertScheduler.scheduleAll(context) } catch (_: Throwable) {}
        try { WidgetUpdater.INSTANCE.updateAll(context) } catch (_: Throwable) {}
        try { LockWidgets.updateAll(context) } catch (_: Throwable) {}
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
            selected = if (PrayerAdjustments.asrHanafi(context)) 1 else 0,
            onDismiss = { showAsrDialog = false },
            onPick = { PrayerAdjustments.setAsrHanafi(context, it == 1); showAsrDialog = false; recalc() }
        )
    }
    if (showHighLatDialog) {
        OptionPickerDialog(
            title = L10n.t(S.HighLatRule),
            options = listOf(L10n.t(S.HighLatNone), L10n.t(S.HighLatMiddle), L10n.t(S.HighLatSeventh), L10n.t(S.HighLatAngle)),
            selected = PrayerAdjustments.highLatRule(context),
            onDismiss = { showHighLatDialog = false },
            onPick = { PrayerAdjustments.setHighLatRule(context, it); showHighLatDialog = false; recalc() }
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
                // City summary row: identical metrics to LocOptionRow above - 12dp insets,
                // icon on the same column axis as the radio rows' icons (RadioButton's
                // minimum-interactive slot is 48dp + 8dp spacer = 56dp), same text styles,
                // trailing change label like CalcRow.
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(14.dp))
                        .plainClickable(enabled = !state.useCurrentLocation) { showCityDialog = true }
                        .padding(12.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Spacer(Modifier.width(56.dp))
                    Icon(Icons.Outlined.LocationOn, null, tint = cs.primary)
                    Spacer(Modifier.width(10.dp))
                    if (editingCity) {
                        OutlinedTextField(
                            value = cityEditText,
                            onValueChange = { cityEditText = it },
                            modifier = Modifier.weight(1f),
                            singleLine = true
                        )
                        IconButton(onClick = {
                            if (viewModel.setCityAlias(cityEditText)) {
                                editingCity = false
                                recalc()
                                try { com.example.utils.WearSync.push(context, null) } catch (_: Throwable) {}
                            }
                        }) { Icon(Icons.Outlined.Check, L10n.t(S.Save), tint = cs.primary) }
                        IconButton(onClick = { editingCity = false }) { Icon(Icons.Outlined.Close, L10n.t(S.Cancel), tint = cs.onSurfaceVariant) }
                    } else {
                        Column(modifier = Modifier.weight(1f)) {
                            Text(L10n.t(S.CustomLocationNameHint), modifier = Modifier.padding(top = 2.dp), color = cs.onSurfaceVariant, fontSize = 13.sp, lineHeight = 18.sp)
                            Text(state.cityName, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        if (cityAliased) {
                            TextButton(onClick = { viewModel.clearCityAlias(); recalc(); try { com.example.utils.WearSync.push(context, null) } catch (_: Throwable) {} }) {
                                Text(L10n.t(S.ResetCityNameAuto), color = cs.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                        androidx.compose.material3.Surface(shape = androidx.compose.foundation.shape.CircleShape, color = cs.primary.copy(alpha = 0.12f), modifier = Modifier.padding(start = 8.dp)) {
                            IconButton(onClick = { cityEditText = state.cityName; editingCity = true }, modifier = Modifier.size(36.dp)) {
                                Icon(Icons.Outlined.Edit, L10n.t(S.EditCityName), tint = cs.primary, modifier = Modifier.size(18.dp))
                            }
                        }
                    }
                }
                if (state.useCurrentLocation && (!state.hasLocationPermission || !state.isGpsAvailable)) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 12.dp, end = 12.dp, bottom = 12.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Icon(Icons.Outlined.Warning, null, tint = cs.error, modifier = Modifier.size(20.dp))
                        Spacer(Modifier.width(8.dp))
                        Text(
                            if (!state.hasLocationPermission) L10n.t(S.NoLocPermission) else L10n.t(S.GpsOff),
                            modifier = Modifier.weight(1f),
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
                    value = L10n.t(if (remember(prefsTick) { PrayerAdjustments.asrHanafi(context) }) S.AsrHanafi else S.AsrShafii),
                    onClick = { showAsrDialog = true }
                )
                CalcRow(
                    icon = Icons.Outlined.Tune,
                    title = L10n.t(S.HighLatRule),
                    value = L10n.t(
                        when (remember(prefsTick) { PrayerAdjustments.highLatRule(context) }) {
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
                val offsets = remember(prefsTick) { PrayerAdjustments.offsetMinutes(context) }
                val names = listOf(S.PrayerFajr, S.PrayerShuruq, S.PrayerDhuhr, S.PrayerAsr, S.PrayerMaghrib, S.PrayerIsha)
                names.forEachIndexed { index, key ->
                    StepperRow(
                        title = L10n.t(key),
                        value = offsets[index],
                        unit = L10n.t(S.AdjMinShort),
                        onChange = { PrayerAdjustments.setOffsetMinutes(context, index, it); recalc() }
                    )
                }
                Spacer(Modifier.height(6.dp))
                StepperRow(
                    title = L10n.t(S.AdjHijri),
                    icon = Icons.Outlined.DateRange,
                    value = remember(prefsTick) { PrayerAdjustments.hijriOffsetDays(context) },
                    unit = L10n.t(S.AdjDayShort),
                    min = -2, max = 2,
                    onChange = { PrayerAdjustments.setHijriOffsetDays(context, it); recalc() }
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
        RadioButton(selected = selected, onClick = com.example.utils.AppHaptics.tap(onClick))
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
            .plainClickable(onClick = onClick)
            .padding(vertical = 10.dp, horizontal = 4.dp),
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
    Row(
        modifier = Modifier.fillMaxWidth().padding(vertical = 6.dp),
        horizontalArrangement = Arrangement.SpaceBetween,
        verticalAlignment = Alignment.CenterVertically
    ) {
        Row(modifier = Modifier.weight(1f), verticalAlignment = Alignment.CenterVertically) {
            if (icon != null) {
                Icon(icon, null, tint = cs.primary, modifier = Modifier.size(20.dp))
                Spacer(Modifier.width(8.dp))
            }
            Text(title, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
        }
        Row(verticalAlignment = Alignment.CenterVertically) {
            StepperButton(Icons.Outlined.Remove, value > min) { onChange(value - 1) }
            Text(
                text = (if (value > 0) "+$value" else "$value") + " " + unit,
                color = if (value != 0) cs.primary else cs.onSurfaceVariant,
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                modifier = Modifier.width(72.dp),
                textAlign = androidx.compose.ui.text.style.TextAlign.Center
            )
            StepperButton(Icons.Outlined.Add, value < max) { onChange(value + 1) }
        }
    }
}

@Composable
private fun StepperButton(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(34.dp)
            .clip(CircleShape)
            .background(if (enabled) cs.primary.copy(alpha = 0.12f) else cs.onSurface.copy(alpha = 0.05f))
    ) {
        Icon(icon, null, tint = if (enabled) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(18.dp))
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
