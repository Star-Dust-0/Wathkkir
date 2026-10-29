package com.example.prayer

import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.provider.Settings
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
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
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.Remove
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Slider
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.foundation.background
import androidx.compose.material3.RadioButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.text.input.KeyboardType
import kotlin.math.roundToInt
import com.example.fajr.data.FajrPreferences
import com.example.fajr.sound.RingtoneHelper
import com.example.prayer.alerts.PrayerAlertScheduler
import com.example.prayer.alerts.PrayerAlertStore
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.oneui.OneUIDialog
import com.example.ui.oneui.OneUISwitch

/**
 * Per-prayer bell settings (long-press a bell on the Prayer screen): enable, alert type
 * (notification / full alarm), timing relative to adhan, iqama reminder. Below the divider,
 * this prayer's sound/vibration overrides, falling back to the app settings.
 */
@Composable
fun PrayerAlertDialog(prayer: Int, prayerName: String, onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    var tick by remember { mutableIntStateOf(0) }
    val snapshot = remember { PrayerAlertStore.INSTANCE.snapshot(context, prayer) }

    val ringtonePickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.StartActivityForResult()
    ) { result ->
        if (result.resultCode == -1) {
            @Suppress("DEPRECATION")
            val uri: android.net.Uri? = result.data?.getParcelableExtra("android.intent.extra.ringtone.PICKED_URI")
            if (uri != null) {
                val title = RingtoneHelper(context).getRingtoneTitle(uri)
                PrayerAlertStore.INSTANCE.setToneOverride(context, prayer, uri.toString(), title)
                tick++
            }
        }
    }
    val audioPickerLauncher = rememberLauncherForActivityResult(
        ActivityResultContracts.GetContent()
    ) { uri: android.net.Uri? ->
        if (uri != null) {
            val title = RingtoneHelper(context).getRingtoneTitle(uri)
            PrayerAlertStore.INSTANCE.setToneOverride(context, prayer, uri.toString(), title)
            tick++
        }
    }

    fun changed() {
        tick++
        PrayerAlertScheduler.INSTANCE.scheduleAll(context)
    }

    val prefs = remember(tick) { FajrPreferences(context) }
    val enabled = remember(tick) { PrayerAlertStore.INSTANCE.isEnabled(context, prayer) }
    val offset = remember(tick) { PrayerAlertStore.INSTANCE.offsetMinutes(context, prayer) }
    val iqama = remember(tick) { PrayerAlertStore.INSTANCE.iqamaReminder(context, prayer) }

    OneUIDialog(
        title = prayerName,
        onDismiss = {
            // Cancel: discard everything changed in this dialog
            PrayerAlertStore.INSTANCE.restore(context, prayer, snapshot)
            PrayerAlertScheduler.INSTANCE.scheduleAll(context)
            onDismiss()
        },
        onConfirm = onDismiss,
        confirmText = L10n.t(S.Apply)
    ) {
        Column {
            // Two clearly separate notifications: the base adhan-time notification, and an
            // optional ADDITIONAL early reminder (its switch reveals the exact-minute control).
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                    Icon(Icons.Outlined.Notifications, null, tint = cs.primary)
                    Spacer(Modifier.width(10.dp))
                    Column {
                        Text(L10n.t(S.AtAdhanRow), color = cs.onSurface, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                        Text(L10n.t(S.AtAdhanRowSub), color = cs.onSurfaceVariant, fontSize = 11.sp)
                    }
                }
                OneUISwitch(enabled, { PrayerAlertStore.INSTANCE.setEnabled(context, prayer, it); changed() })
            }
            if (enabled) {
                Spacer(Modifier.height(10.dp))
                // Early reminder: an ADDITION to the adhan-time notification, never a replacement.
                val earlyOn = offset > 0
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(Modifier.weight(1f)) {
                        Text(L10n.t(S.BeforeAdhanRow), color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                        Text(L10n.t(S.BeforeAdhanHelper), color = cs.onSurfaceVariant, fontSize = 11.sp)
                    }
                    OneUISwitch(earlyOn, { want ->
                        PrayerAlertStore.INSTANCE.setOffsetMinutes(context, prayer, if (want) (if (offset > 0) offset else 10) else 0)
                        changed()
                    })
                }
                if (earlyOn) {
                    Spacer(Modifier.height(6.dp))
                    // Exact lead time: One UI slider, 1..60 minutes; tap the value to type it.
                    var dragOffset by remember(tick) { mutableIntStateOf(offset) }
                    var showMinuteInput by remember { mutableStateOf(false) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Text(L10n.t(S.AlertTiming), color = cs.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                        Text(
                            L10n.t(S.MinBeforeFmt).format(dragOffset),
                            color = cs.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                            modifier = Modifier.clickable { showMinuteInput = true }
                        )
                    }
                    if (showMinuteInput) {
                        var text by remember { mutableStateOf(dragOffset.toString()) }
                        OneUIDialog(
                            title = L10n.t(S.AlertTiming),
                            onDismiss = { showMinuteInput = false },
                            onConfirm = {
                                val v = text.toIntOrNull()
                                if (v != null && v in 1..60) {
                                    dragOffset = v
                                    if (v != offset) {
                                        PrayerAlertStore.INSTANCE.setOffsetMinutes(context, prayer, v)
                                        changed()
                                    }
                                    showMinuteInput = false
                                }
                            },
                            confirmText = L10n.t(S.Save)
                        ) {
                            OutlinedTextField(
                                value = text,
                                onValueChange = { v -> text = v.filter { ch -> ch.isDigit() }.take(3) },
                                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                                singleLine = true,
                                modifier = Modifier.fillMaxWidth()
                            )
                        }
                    }
                    Slider(
                        value = dragOffset.toFloat(),
                        onValueChange = { dragOffset = it.roundToInt().coerceIn(1, 60) },
                        onValueChangeFinished = {
                            if (dragOffset != offset) {
                                PrayerAlertStore.INSTANCE.setOffsetMinutes(context, prayer, dragOffset)
                                changed()
                            }
                        },
                        valueRange = 1f..60f,
                        steps = 58,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
                // Iqama reminder (no iqama for Shuruq)
                if (prayer != 1) {
                    Spacer(Modifier.height(8.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(L10n.t(S.IqamaReminder), color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(L10n.t(S.IqamaReminderSub), color = cs.onSurfaceVariant, fontSize = 11.sp)
                        }
                        OneUISwitch(iqama, { PrayerAlertStore.INSTANCE.setIqamaReminder(context, prayer, it); changed() })
                    }
                }
                if (prayer != 1) {
                    Spacer(Modifier.height(8.dp))
                    val dndGranted = remember(tick) {
                        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                        nm.isNotificationPolicyAccessGranted
                    }
                    val dndOn = remember(tick) { PrayerAlertStore.INSTANCE.autoDnd(context, prayer) }
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(Modifier.weight(1f)) {
                            Text(L10n.t(S.AutoDnd), color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                            Text(L10n.t(S.AutoDndSub), color = cs.onSurfaceVariant, fontSize = 11.sp)
                        }
                        OneUISwitch(dndOn && dndGranted, { want ->
                            if (want && !dndGranted) {
                                try {
                                    context.startActivity(
                                        Intent(Settings.ACTION_NOTIFICATION_POLICY_ACCESS_SETTINGS)
                                            .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                                    )
                                } catch (_: Throwable) {}
                            } else {
                                PrayerAlertStore.INSTANCE.setAutoDnd(context, prayer, want)
                                changed()
                            }
                        })
                    }
                }
                Spacer(Modifier.height(12.dp))
                val hasOverride = remember(tick) { PrayerAlertStore.INSTANCE.hasOverride(context, prayer) }
                val english = L10n.lang != com.example.ui.i18n.Lang.AR
                Text(if (english) "This prayer's tone and vibration" else "نغمة واهتزاز هذه الصلاة", color = cs.onSurfaceVariant, fontSize = 12.sp, fontWeight = FontWeight.Bold)
                Text(if (hasOverride) {
                    if (english) "Custom for this prayer only" else "مخصص لهذه الصلاة فقط"
                } else {
                    if (english) "Follows app settings" else "يتبع إعدادات التطبيق"
                }, color = cs.onSurfaceVariant, fontSize = 13.sp)
                Spacer(Modifier.height(4.dp))
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .clip(RoundedCornerShape(12.dp))
                        .clickable {
                            val intent = Intent("android.intent.action.RINGTONE_PICKER")
                            intent.putExtra("android.intent.extra.ringtone.TYPE", 4)
                            intent.putExtra("android.intent.extra.ringtone.TITLE", L10n.t(S.PickRingtone))
                            intent.putExtra("android.intent.extra.ringtone.SHOW_SILENT", false)
                            intent.putExtra("android.intent.extra.ringtone.SHOW_DEFAULT", true)
                            ringtonePickerLauncher.launch(intent)
                        }
                        .padding(vertical = 8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(Icons.Outlined.MusicNote, null, tint = cs.primary)
                    Spacer(Modifier.width(10.dp))
                    Column(Modifier.weight(1f)) {
                        Text(L10n.t(S.SelectedTone), color = cs.onSurfaceVariant, fontSize = 11.sp)
                        Text((if (PrayerAlertStore.INSTANCE.toneOverride(context, prayer) != null) PrayerAlertStore.INSTANCE.toneOverrideTitle(context, prayer) else com.example.utils.NotifPrefs.toneTitle(context)) ?: L10n.t(S.Default), color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    Text(
                        L10n.t(S.AudioFile), color = cs.primary, fontSize = 12.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier
                            .clip(RoundedCornerShape(8.dp))
                            .clickable { audioPickerLauncher.launch("audio/*") }
                            .padding(6.dp)
                    )
                }
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(Icons.Outlined.Vibration, null, tint = cs.primary)
                        Spacer(Modifier.width(10.dp))
                        Text(L10n.t(S.VibrateDuringAlarm), color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
                    }
                    OneUISwitch(PrayerAlertStore.INSTANCE.effectiveVibration(context, prayer), { PrayerAlertStore.INSTANCE.setVibrationOverride(context, prayer, it); tick++ })
                }
                if (hasOverride) {
                    Text(
                        if (english) "Reset to app settings" else "العودة لإعدادات التطبيق",
                        color = cs.primary, fontSize = 13.sp, fontWeight = FontWeight.Bold,
                        modifier = Modifier.clip(RoundedCornerShape(8.dp)).clickable {
                            PrayerAlertStore.INSTANCE.clearOverride(context, prayer)
                            changed()
                        }.padding(6.dp)
                    )
                }
            }
        }
    }
}

@Composable
private fun AlertModeRow(selected: Boolean, icon: ImageVector, title: String, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick)
            .background(if (selected) cs.primary.copy(alpha = 0.12f) else Color.Transparent)
            .padding(horizontal = 8.dp, vertical = 8.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(selected = selected, onClick = onClick)
        Icon(icon, null, tint = cs.primary)
        Spacer(Modifier.width(8.dp))
        Text(title, color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
    }
}

@Composable
private fun RoundBtn(icon: ImageVector, enabled: Boolean, onClick: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    IconButton(
        onClick = onClick,
        enabled = enabled,
        modifier = Modifier
            .size(32.dp)
            .clip(CircleShape)
            .background(if (enabled) cs.primary.copy(alpha = 0.12f) else cs.onSurface.copy(alpha = 0.05f))
    ) {
        Icon(icon, null, tint = if (enabled) cs.primary else cs.onSurfaceVariant.copy(alpha = 0.4f), modifier = Modifier.size(16.dp))
    }
}
