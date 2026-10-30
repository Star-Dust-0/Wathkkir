package com.example.ui.screens

import android.Manifest
import android.app.AlarmManager
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import android.os.Build
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.VolumeUp
import androidx.compose.material.icons.outlined.Warning
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.fajr.sound.RingtoneHelper
import com.example.fajr.ui.FajrAlarmActivity
import com.example.fajr.ui.FajrViewModel
import com.example.ui.i18n.L10n
import com.example.ui.i18n.Lang
import com.example.ui.i18n.S
import com.example.ui.oneui.OneUIButton
import com.example.ui.oneui.OneUIChip
import com.example.ui.oneui.OneUIChipGroup
import com.example.ui.oneui.OneUIChipItem
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.OneUISwitch
import kotlin.math.roundToInt

private fun w(ar: String, en: String) = if (L10n.lang == Lang.AR) ar else en

/**
 * 3.3.0: the full-screen Fajr alarm (separate from the Fajr bell notification).
 * Enable, tone, volume, ring duration, vibration, and a test button. Uses the real scheduler.
 */
@Composable
fun FajrAlarmSettingsScreen(viewModel: FajrViewModel, onBack: () -> Unit) {
    val ctx = LocalContext.current
    val st by viewModel.uiState.collectAsStateWithLifecycle()
    var tick by remember { mutableIntStateOf(0) }
    val fp = remember { com.example.fajr.data.FajrPreferences(ctx) }
    var beforeMin by remember { mutableIntStateOf(fp.alarmBeforeMin) }
    var afterMin by remember { mutableIntStateOf(fp.alarmAfterMin) }
    var snoozeMin by remember { mutableIntStateOf(fp.snoozeMinutes) }
    fun reschedule() { try { com.example.fajr.alarm.FajrAlarmScheduler(ctx).scheduleNextFajrAlarm() } catch (_: Throwable) {} }
    var volume by remember(st.alarmVolume) { mutableIntStateOf(st.alarmVolume) }

    LaunchedEffect(Unit) { viewModel.loadSettings(); viewModel.refreshFajrTimeManually() }
    DisposableEffect(Unit) { onDispose { viewModel.stopRingtonePreview() } }
    val owner = LocalLifecycleOwner.current
    DisposableEffect(owner) {
        val obs = LifecycleEventObserver { _, e -> if (e == Lifecycle.Event.ON_RESUME) tick++ }
        owner.lifecycle.addObserver(obs)
        onDispose { owner.lifecycle.removeObserver(obs) }
    }

    val toneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { r ->
        if (r.resultCode == -1) {
            @Suppress("DEPRECATION")
            val uri: Uri? = r.data?.getParcelableExtra("android.intent.extra.ringtone.PICKED_URI")
            if (uri != null) viewModel.setRingtone(uri, RingtoneHelper(ctx).getRingtoneTitle(uri))
        }
    }
    val fileLauncher = rememberLauncherForActivityResult(ActivityResultContracts.GetContent()) { uri: Uri? ->
        if (uri != null) viewModel.setRingtone(uri, RingtoneHelper(ctx).getRingtoneTitle(uri))
    }
    val notifPerm = rememberLauncherForActivityResult(ActivityResultContracts.RequestPermission()) { tick++ }

    val enabled = st.isAlarmEnabled
    val needExact = remember(tick, enabled) {
        enabled && Build.VERSION.SDK_INT >= 31 &&
            !(ctx.getSystemService(Context.ALARM_SERVICE) as AlarmManager).canScheduleExactAlarms()
    }
    val needFullScreen = remember(tick, enabled) {
        enabled && Build.VERSION.SDK_INT >= 34 &&
            !(ctx.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).canUseFullScreenIntent()
    }
    val needNotif = remember(tick, enabled) {
        enabled && Build.VERSION.SDK_INT >= 33 &&
            ctx.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) != PackageManager.PERMISSION_GRANTED
    }

    OneUIScreen(title = L10n.t(S.FajrAlarm), onBack = onBack) {
        item(key = "fa-main") {
            OneUICard {
                OneUIRow(
                    title = L10n.t(S.FajrAlarm),
                    summary = if (enabled) L10n.t(S.AlarmOnDesc) else L10n.t(S.AlarmOffDesc),
                    leading = { OneUIIconBadge(Icons.Outlined.Alarm) },
                    trailing = { OneUISwitch(enabled, { viewModel.toggleAlarm(it) }) },
                    onClick = com.example.utils.AppHaptics.tap { viewModel.toggleAlarm(!enabled) }
                )
                if (enabled) {
                    OneUIDivider(70.dp)
                    OneUIRow(
                        title = L10n.t(S.NextFajrAt),
                        summary = st.nextFajrTimeFormatted ?: "",
                        leading = { OneUIIconBadge(Icons.Outlined.Schedule) }
                    )
                }
            }
        }
        item(key = "fa-note") {
            Text(
                w(
                    "منبه بملء الشاشة يرن عند الفجر، وهو منفصل عن تنبيه الفجر (الجرس) في شاشة الصلاة. عند تفعيله لا يصلك إشعار الفجر عند الأذان حتى لا يتكرر التنبيه، وتبقى تنبيهات ما قبل الأذان والإقامة كما هي.",
                    "A full-screen alarm that rings at Fajr. It is separate from the Fajr bell on the prayer screen. While it is on, the Fajr bell does not also notify at the adhan, so you are not alerted twice. Early and iqama bell alerts stay as they are."
                ),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                fontSize = 12.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 4.dp)
            )
        }
        if (needExact || needFullScreen || needNotif) {
            item(key = "fa-perm") {
                OneUICard {
                    if (needNotif) {
                        OneUIRow(
                            title = w("السماح بالإشعارات", "Allow notifications"),
                            summary = w("مطلوب لعرض المنبه", "Needed to show the alarm"),
                            leading = { OneUIIconBadge(Icons.Outlined.Warning) },
                            onClick = { if (Build.VERSION.SDK_INT >= 33) notifPerm.launch(Manifest.permission.POST_NOTIFICATIONS) }
                        )
                    }
                    if (needExact) {
                        if (needNotif) OneUIDivider(70.dp)
                        OneUIRow(
                            title = w("السماح بالمنبهات الدقيقة", "Allow exact alarms"),
                            summary = w("بدونه قد يتأخر المنبه", "Without it the alarm may be late"),
                            leading = { OneUIIconBadge(Icons.Outlined.Warning) },
                            onClick = {
                                try { ctx.startActivity(Intent(Settings.ACTION_REQUEST_SCHEDULE_EXACT_ALARM, Uri.parse("package:" + ctx.packageName))) } catch (_: Exception) {}
                            }
                        )
                    }
                    if (needFullScreen) {
                        if (needNotif || needExact) OneUIDivider(70.dp)
                        OneUIRow(
                            title = w("السماح بالعرض بملء الشاشة", "Allow full-screen alerts"),
                            summary = w("مطلوب لظهور المنبه فوق شاشة القفل", "Needed to show over the lock screen"),
                            leading = { OneUIIconBadge(Icons.Outlined.Warning) },
                            onClick = {
                                try { ctx.startActivity(Intent(Settings.ACTION_MANAGE_APP_USE_FULL_SCREEN_INTENT, Uri.parse("package:" + ctx.packageName))) } catch (_: Exception) {}
                            }
                        )
                    }
                }
            }
        }
        item(key = "fa-sound") {
            OneUICard {
                OneUIRow(
                    title = w("نغمة المنبه", "Alarm tone"),
                    summary = st.ringtoneTitle?.takeIf { it.isNotBlank() } ?: L10n.t(S.DefaultAlarmTone),
                    leading = { OneUIIconBadge(Icons.Outlined.MusicNote) },
                    onClick = {
                        val intent = Intent("android.intent.action.RINGTONE_PICKER")
                        intent.putExtra("android.intent.extra.ringtone.TYPE", 4)
                        intent.putExtra("android.intent.extra.ringtone.SHOW_DEFAULT", true)
                        intent.putExtra("android.intent.extra.ringtone.SHOW_SILENT", false)
                        toneLauncher.launch(intent)
                    }
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = L10n.t(S.AudioFile),
                    leading = { OneUIIconBadge(Icons.Outlined.MusicNote) },
                    onClick = { fileLauncher.launch("audio/*") }
                )
                OneUIDivider(70.dp)
                Column(Modifier.padding(horizontal = 16.dp, vertical = 10.dp)) {
                    Row(verticalAlignment = androidx.compose.ui.Alignment.CenterVertically) {
                        OneUIIconBadge(Icons.Outlined.VolumeUp)
                        Spacer(Modifier.padding(start = 12.dp))
                        Text(w("مستوى الصوت", "Volume") + "  " + volume + "%", color = MaterialTheme.colorScheme.onSurface)
                    }
                    Slider(
                        value = volume.toFloat(),
                        onValueChange = { volume = it.roundToInt() },
                        onValueChangeFinished = { viewModel.setAlarmVolume(volume) },
                        valueRange = 10f..100f,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                OneUIDivider(70.dp)
                OneUIRow(
                    title = L10n.t(S.VibrateDuringAlarm),
                    leading = { OneUIIconBadge(Icons.Outlined.Vibration) },
                    trailing = { OneUISwitch(st.isVibrationEnabled, { viewModel.toggleVibration(it) }) },
                    onClick = com.example.utils.AppHaptics.tap { viewModel.toggleVibration(!st.isVibrationEnabled) }
                )
            }
        }
        item(key = "fa-timing") {
            OneUICard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(w("وقت التنبيه", "Alarm timing"), color = MaterialTheme.colorScheme.onSurface)
                    Text(
                        w("يرن وقت الأذان دائماً. يمكنك إضافة تنبيه قبل الأذان و/أو بعده.", "Always rings at the adhan. You can add an alarm before and/or after it."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(w("قبل الأذان", "Before the adhan"), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    // 3.3.3: sliding-pill chip group (his spec: animation only, no joined container)
                    OneUIChipGroup(
                        items = intArrayOf(0, 5, 10, 15, 30, 45, 60).map { OneUIChipItem(if (it == 0) w("بدون", "Off") else L10n.minutesCount(it)) },
                        selected = intArrayOf(0, 5, 10, 15, 30, 45, 60).indexOfFirst { it == beforeMin },
                        onSelect = { i -> val m = intArrayOf(0, 5, 10, 15, 30, 45, 60)[i]; beforeMin = m; fp.alarmBeforeMin = m; reschedule() }
                    )
                    Spacer(Modifier.height(12.dp))
                    Text(w("بعد الأذان", "After the adhan"), color = MaterialTheme.colorScheme.onSurface, fontSize = 13.sp)
                    Spacer(Modifier.height(6.dp))
                    OneUIChipGroup(
                        items = intArrayOf(0, 5, 10, 15, 30, 45, 60).map { OneUIChipItem(if (it == 0) w("بدون", "Off") else L10n.minutesCount(it)) },
                        selected = intArrayOf(0, 5, 10, 15, 30, 45, 60).indexOfFirst { it == afterMin },
                        onSelect = { i -> val m = intArrayOf(0, 5, 10, 15, 30, 45, 60)[i]; afterMin = m; fp.alarmAfterMin = m; reschedule() }
                    )
                }
            }
        }
        item(key = "fa-dur") {
            OneUICard {
                Column(Modifier.padding(horizontal = 16.dp, vertical = 12.dp)) {
                    Text(
                        w("مدة الرنين قبل الإيقاف التلقائي (بالدقائق)", "Ring time before auto-stop (minutes)"),
                        color = MaterialTheme.colorScheme.onSurface
                    )
                    Text(
                        w("بعد الإيقاف التلقائي يعاد الرنين حتى ٣ مرات بعد مدة الغفوة.", "After an auto-stop it rings again up to 3 times, after the snooze length."),
                        color = MaterialTheme.colorScheme.onSurfaceVariant, fontSize = 12.sp
                    )
                    Spacer(Modifier.height(10.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in intArrayOf(1, 2, 3, 5, 10, 15)) {
                            item { OneUIChip(m.toString(), st.maxDurationMinutes == m, { viewModel.setMaxDuration(m) }) }
                        }
                    }
                    Spacer(Modifier.height(14.dp))
                    Text(w("مدة الغفوة (بالدقائق)", "Snooze length (minutes)"), color = MaterialTheme.colorScheme.onSurface)
                    Spacer(Modifier.height(8.dp))
                    LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        for (m in intArrayOf(5, 10, 15, 20, 30)) {
                            item { OneUIChip(m.toString(), snoozeMin == m, { snoozeMin = m; fp.snoozeMinutes = m }) }
                        }
                    }
                }
            }
        }
        item(key = "fa-test") {
            OneUIButton(
                w("تجربة المنبه الآن", "Test the alarm now"),
                {
                    ctx.startActivity(Intent(ctx, FajrAlarmActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK))
                },
                Modifier.fillMaxWidth(), false, Icons.Outlined.PlayArrow
            )
        }
    }
}
