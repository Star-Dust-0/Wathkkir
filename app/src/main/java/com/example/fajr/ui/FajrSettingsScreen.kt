package com.example.fajr.ui

import android.Manifest
import android.app.Activity
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Alarm
import androidx.compose.material.icons.filled.AudioFile
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.EditLocation
import androidx.compose.material.icons.filled.GpsFixed
import androidx.compose.material.icons.filled.LocationOn
import androidx.compose.material.icons.filled.MusicNote
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Stop
import androidx.compose.material.icons.filled.Tune
import androidx.compose.material.icons.filled.Vibration
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fajr.calculation.CalculationMethod
import com.example.fajr.sound.RingtoneHelper
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDialog
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.OneUISwitch

@Composable
fun FajrSettingsScreen(viewModel: FajrViewModel, onBack: () -> Unit = {}) {
    val state by viewModel.uiState.collectAsStateWithLifecycle()
    val context = LocalContext.current

    var showCityDialog by remember { mutableStateOf(false) }
    var showMethodDialog by remember { mutableStateOf(false) }
    var isPlayingPreview by remember { mutableStateOf(false) }

    val locationPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val granted = permissions.values.any { it }
        if (granted) viewModel.updateLocationFromGps()
    }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            @Suppress("DEPRECATION")
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) {
                val title = RingtoneHelper(context).getRingtoneTitle(uri)
                viewModel.setRingtone(uri, title)
            }
        }
    }

    val audioPickerLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri: Uri? ->
        if (uri != null) {
            val title = RingtoneHelper(context).getRingtoneTitle(uri)
            viewModel.setRingtone(uri, title)
        }
    }

    DisposableEffect(Unit) {
        onDispose { viewModel.stopRingtonePreview() }
    }

    if (showCityDialog) {
        CitySearchDialog(
            onDismiss = { showCityDialog = false },
            onCitySelected = { city -> viewModel.setCity(city) }
        )
    }

    if (showMethodDialog) {
        CalculationMethodDialog(
            currentMethod = state.calculationMethod,
            onDismiss = { showMethodDialog = false },
            onMethodSelected = { method -> viewModel.setCalculationMethod(method) }
        )
    }

    FajrSettingsContent(
        state = state,
        onBack = onBack,
        onToggleAlarm = { viewModel.toggleAlarm(it) },
        onSelectGps = {
            viewModel.setUseCurrentLocation(true)
            if (!state.hasLocationPermission) {
                locationPermissionLauncher.launch(
                    arrayOf(
                        Manifest.permission.ACCESS_FINE_LOCATION,
                        Manifest.permission.ACCESS_COARSE_LOCATION
                    )
                )
            }
        },
        onSelectManualCity = { viewModel.setUseCurrentLocation(false) },
        onChangeCity = { showCityDialog = true },
        onChangeMethod = { showMethodDialog = true },
        onRefresh = { viewModel.refreshFajrTimeManually() },
        isPlayingPreview = isPlayingPreview,
        onTogglePreview = {
            if (isPlayingPreview) {
                viewModel.stopRingtonePreview()
                isPlayingPreview = false
            } else {
                val uri = RingtoneHelper(context).getValidRingtoneUri(com.example.fajr.data.FajrPreferences(context))
                viewModel.playRingtonePreview(uri)
                isPlayingPreview = true
            }
        },
        onPickSystemRingtone = {
            val intent = Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_ALARM)
                putExtra(RingtoneManager.EXTRA_RINGTONE_TITLE, "اختر نغمة المنبه")
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, false)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
            }
            ringtonePickerLauncher.launch(intent)
        },
        onPickAudioFile = { audioPickerLauncher.launch("audio/*") },
        onToggleVibration = { viewModel.toggleVibration(it) },
        onSetVolume = { viewModel.setAlarmVolume(it) },
        onSetMaxDuration = { viewModel.setMaxDuration(it) }
    )
}

@Composable
fun FajrSettingsContent(
    state: FajrSettingsState,
    onBack: () -> Unit = {},
    onToggleAlarm: (Boolean) -> Unit = {},
    onSelectGps: () -> Unit = {},
    onSelectManualCity: () -> Unit = {},
    onChangeCity: () -> Unit = {},
    onChangeMethod: () -> Unit = {},
    onRefresh: () -> Unit = {},
    isPlayingPreview: Boolean = false,
    onTogglePreview: () -> Unit = {},
    onPickSystemRingtone: () -> Unit = {},
    onPickAudioFile: () -> Unit = {},
    onToggleVibration: (Boolean) -> Unit = {},
    onSetVolume: (Int) -> Unit = {},
    onSetMaxDuration: (Int) -> Unit = {}
) {
    OneUIScreen(title = "منبه صلاة الفجر", onBack = onBack) {
        // Flat header: next fajr time + status, no card
        item {
            FajrHeader(
                nextFajrTimeFormatted = state.nextFajrTimeFormatted,
                isAlarmEnabled = state.isAlarmEnabled,
                cityName = state.cityName
            )
        }

        // Main alarm toggle
        item {
            OneUICard(padding = PaddingValues(18.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Box(
                            modifier = Modifier
                                .size(44.dp)
                                .clip(CircleShape)
                                .background(MaterialTheme.colorScheme.primary.copy(alpha = 0.15f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Alarm,
                                contentDescription = null,
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(24.dp)
                            )
                        }
                        Spacer(Modifier.width(14.dp))
                        Column {
                            Text(
                                text = "تفعيل منبه صلاة الفجر",
                                fontSize = 16.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onSurface
                            )
                            Text(
                                text = if (state.isAlarmEnabled) "المنبه يعطي تنبيهاً وقت الفجر" else "المنبه متوقف حالياً",
                                fontSize = 12.sp,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                    }
                    OneUISwitch(
                        checked = state.isAlarmEnabled,
                        onCheckedChange = onToggleAlarm
                    )
                }
            }
        }

        // Location mode
        item {
            OneUICard(padding = PaddingValues(18.dp)) {
                Text(
                    text = "طريقة تحديد الموقع",
                    fontSize = 15.sp,
                    fontWeight = FontWeight.Bold,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(12.dp))

                LocationOptionRow(
                    selected = state.useCurrentLocation,
                    icon = Icons.Default.GpsFixed,
                    title = "الموقع الحالي",
                    subtitle = "تحديث تلقائي لموعد الفجر حسب موقعك",
                    onClick = onSelectGps
                )
                Spacer(Modifier.height(8.dp))
                LocationOptionRow(
                    selected = !state.useCurrentLocation,
                    icon = Icons.Default.EditLocation,
                    title = "اختيار الدولة والمدينة",
                    subtitle = "اختيار مدينتك يدوياً بدون الحاجة إلى تحديد الموقع",
                    onClick = onSelectManualCity
                )

                Spacer(Modifier.height(14.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.LocationOn, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("المدينة الحالية", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.cityName, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    if (!state.useCurrentLocation) {
                        TextButton(onClick = onChangeCity) {
                            Text("تغيير المدينة", fontSize = 13.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                }

                if (state.useCurrentLocation && (!state.hasLocationPermission || !state.isGpsAvailable)) {
                    Spacer(Modifier.height(12.dp))
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Warning, contentDescription = null, tint = MaterialTheme.colorScheme.error)
                        Spacer(Modifier.width(10.dp))
                        Text(
                            text = if (!state.hasLocationPermission) "صلاحية الموقع غير ممنوحة. يرجى المتابعة لمنح الصلاحية."
                            else "خدمة الموقع (GPS) غير مفعلة في الهاتف.",
                            fontSize = 12.sp,
                            color = MaterialTheme.colorScheme.error
                        )
                    }
                }
            }
        }

        // Calculation method
        item {
            OneUICard(padding = PaddingValues(18.dp)) {
                Text("طريقة الحساب", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(10.dp))

                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable(onClick = onChangeMethod)
                        .padding(vertical = 6.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.Tune, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("طريقة الحساب المختارة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.calculationMethod.titleAr, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    Text("تغيير", fontSize = 13.sp, fontWeight = FontWeight.Bold, color = MaterialTheme.colorScheme.primary)
                }

                Spacer(Modifier.height(12.dp))

                OutlinedButton(
                    onClick = onRefresh,
                    enabled = !state.isRefreshing,
                    shape = RoundedCornerShape(16.dp),
                    modifier = Modifier.fillMaxWidth()
                ) {
                    if (state.isRefreshing) {
                        CircularProgressIndicator(modifier = Modifier.size(18.dp), strokeWidth = 2.dp)
                        Spacer(Modifier.width(8.dp))
                        Text("جاري تحديث الموعد...")
                    } else {
                        Icon(Icons.Default.Refresh, contentDescription = null)
                        Spacer(Modifier.width(8.dp))
                        Text("تحديث موعد الفجر الآن", fontWeight = FontWeight.Bold)
                    }
                }
            }
        }

        // Sound & ringtone
        item {
            OneUICard(padding = PaddingValues(18.dp)) {
                Text("نغمة المنبه والصوت", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Column {
                            Text("النغمة المختارة", fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
                            Text(state.ringtoneTitle, fontSize = 14.sp, fontWeight = FontWeight.Bold)
                        }
                    }
                    IconButton(onClick = onTogglePreview) {
                        Icon(
                            imageVector = if (isPlayingPreview) Icons.Default.Stop else Icons.Default.PlayArrow,
                            contentDescription = "Preview",
                            tint = MaterialTheme.colorScheme.primary,
                            modifier = Modifier.size(28.dp)
                        )
                    }
                }

                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onPickSystemRingtone,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.MusicNote, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("نغمات النظام", fontSize = 12.sp)
                    }
                    OutlinedButton(
                        onClick = onPickAudioFile,
                        shape = RoundedCornerShape(14.dp),
                        modifier = Modifier.weight(1f)
                    ) {
                        Icon(Icons.Default.AudioFile, contentDescription = null, modifier = Modifier.size(18.dp))
                        Spacer(Modifier.width(6.dp))
                        Text("ملف صوتي", fontSize = 12.sp)
                    }
                }
            }
        }

        // Extra settings
        item {
            OneUICard(padding = PaddingValues(18.dp)) {
                Text("إعدادات إضافية", fontSize = 15.sp, fontWeight = FontWeight.Bold)
                Spacer(Modifier.height(12.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Default.Vibration, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                        Spacer(Modifier.width(10.dp))
                        Text("الاهتزاز أثناء التنبيه", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    }
                    OneUISwitch(
                        checked = state.isVibrationEnabled,
                        onCheckedChange = onToggleVibration
                    )
                }

                Spacer(Modifier.height(16.dp))

                Column {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(Icons.Default.VolumeUp, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
                            Spacer(Modifier.width(10.dp))
                            Text("مستوى صوت المنبه", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                        }
                        Text("${state.alarmVolume}%", fontSize = 14.sp, fontWeight = FontWeight.Bold)
                    }
                    Slider(
                        value = state.alarmVolume.toFloat(),
                        onValueChange = { onSetVolume(it.toInt()) },
                        valueRange = 10f..100f
                    )
                }

                Spacer(Modifier.height(12.dp))

                Column {
                    Text("مدة الغفوة", fontSize = 14.sp, fontWeight = FontWeight.SemiBold)
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        listOf(1, 3, 5, 10).forEach { mins ->
                            FilterChip(
                                selected = state.maxDurationMinutes == mins,
                                onClick = { onSetMaxDuration(mins) },
                                label = { Text("$mins (د)", modifier = Modifier.fillMaxWidth(), textAlign = androidx.compose.ui.text.style.TextAlign.Center, maxLines = 1) },
                                modifier = Modifier.weight(1f)
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LocationOptionRow(
    selected: Boolean,
    icon: ImageVector,
    title: String,
    subtitle: String,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(14.dp))
            .clickable(onClick = onClick)
            .background(
                if (selected) MaterialTheme.colorScheme.primary.copy(alpha = 0.12f)
                else Color.Transparent
            )
            .padding(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Spacer(Modifier.width(8.dp))
        Icon(icon, contentDescription = null, tint = MaterialTheme.colorScheme.primary)
        Spacer(Modifier.width(10.dp))
        Column {
            Text(title, fontWeight = FontWeight.SemiBold, fontSize = 14.sp)
            Text(subtitle, fontSize = 11.sp, color = MaterialTheme.colorScheme.onSurfaceVariant)
        }
    }
}

@Composable
fun FajrHeader(
    nextFajrTimeFormatted: String,
    isAlarmEnabled: Boolean,
    cityName: String
) {
    Column(modifier = Modifier.fillMaxWidth().padding(horizontal = 4.dp)) {
        Text(
            text = "موعد صلاة الفجر القادم",
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(4.dp))
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Text(
                text = nextFajrTimeFormatted,
                fontSize = 30.sp,
                fontWeight = FontWeight.ExtraBold,
                color = MaterialTheme.colorScheme.primary
            )
            Surface(
                shape = RoundedCornerShape(12.dp),
                color = if (isAlarmEnabled) Color(0xFF10B981).copy(alpha = 0.15f) else Color.Gray.copy(alpha = 0.15f)
            ) {
                Text(
                    text = if (isAlarmEnabled) "المنبه مفعّل ✓" else "المنبه معطّل",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isAlarmEnabled) Color(0xFF10B981) else Color.Gray,
                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 6.dp)
                )
            }
        }
        Spacer(Modifier.height(2.dp))
        Text(
            text = cityName,
            fontSize = 12.sp,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}
