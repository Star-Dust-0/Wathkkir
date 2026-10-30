package com.example.ui.screens
import com.example.ui.theme.localizedTitle
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.Info
import androidx.compose.foundation.layout.padding

import com.example.ui.i18n.Lang

import android.content.ActivityNotFoundException
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.DarkMode
import androidx.compose.material.icons.outlined.MusicNote
import androidx.compose.material.icons.outlined.MoreTime
import androidx.compose.material.icons.outlined.Place
import androidx.compose.material.icons.outlined.TextFields
import androidx.compose.material.icons.outlined.Vibration
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material.icons.outlined.Widgets
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDialog
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.OneUISwitch
import com.example.ThemeSettings
import com.example.ui.oneui.SectionLabel
import com.example.ui.viewmodel.MainViewModel
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import androidx.compose.material.icons.outlined.Language

@Composable
fun SettingsScreen(
    viewModel: MainViewModel,
    onBack: () -> Unit,
    onNavigateToFajr: (() -> Unit)? = null,
    onNavigateToLocation: (() -> Unit)? = null,
    onNavigateToOptionalTimes: (() -> Unit)? = null,
    onNavigateToHijriConverter: (() -> Unit)? = null,
    onNavigateToAbout: (() -> Unit)? = null,
    onNavigateToWatch: (() -> Unit)? = null
) {
    val contextApp = LocalContext.current.applicationContext
    val vibration by viewModel.vibrationEnabled.collectAsState()
    val toneState by viewModel.notificationSound.collectAsState()
    val toneSummary = remember(toneState) { com.example.utils.NotifPrefs.toneTitle(contextApp) ?: L10n.t(S.Default) }
    val appFont by viewModel.appFont.collectAsState()
    var showFontPicker by remember { mutableStateOf(false) }
    val fontScale by com.example.FontScaleSettings.scale.collectAsState()
    var showThemePicker by remember { mutableStateOf(false) }
    var showLockWidgets by remember { mutableStateOf(false) }
    var showLanguagePicker by remember { mutableStateOf(false) }
    val themeMode by ThemeSettings.mode.collectAsState()
    val themeTitle = when (themeMode) {
        ThemeSettings.LIGHT -> L10n.t(S.Light)
        ThemeSettings.DARK -> L10n.t(S.Dark)
        else -> L10n.t(S.SystemAuto)
    }
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == -1) {
            @Suppress("DEPRECATION")
            val uri: Uri? = result.data?.getParcelableExtra("android.intent.extra.ringtone.PICKED_URI")
            if (uri != null) viewModel.updateNotificationSound(uri.toString())
        }
    }

    SettingsContent(
        vibration = vibration,
        fontTitle = appFont.localizedTitle(),
        themeTitle = themeTitle,
        onBack = onBack,
        onVibration = { viewModel.updateVibrationEnabled(it) },
        toneSummary = toneSummary,
        onPickFont = { showFontPicker = true },
        fontScale = fontScale,
        onScaleChange = { com.example.FontScaleSettings.set(contextApp, it) },
        onPickSound = {
            val intent = Intent("android.intent.action.RINGTONE_PICKER")
            intent.putExtra("android.intent.extra.ringtone.TYPE", 2)
            intent.putExtra("android.intent.extra.ringtone.SHOW_DEFAULT", true)
            intent.putExtra("android.intent.extra.ringtone.SHOW_SILENT", true)
            ringtoneLauncher.launch(intent)
        },
        onFajr = onNavigateToFajr,
        onLocation = onNavigateToLocation,
        onOptionalTimes = onNavigateToOptionalTimes,
        onHijriConverter = onNavigateToHijriConverter,
        onPickTheme = { showThemePicker = true },
        onLockWidgets = { showLockWidgets = true },
        onPickLanguage = { showLanguagePicker = true },
        onAbout = onNavigateToAbout,
        onWatch = onNavigateToWatch
    )

    if (showFontPicker) {
        FontPickerDialog(
            selected = appFont,
            onSelect = { viewModel.updateAppFont(it) },
            onDismiss = { showFontPicker = false }
        )
    }
    if (showThemePicker) {
        ThemePickerDialog(
            selected = themeMode,
            onSelect = { ThemeSettings.set(contextApp, it) },
            onDismiss = { showThemePicker = false }
        )
    }
    if (showLockWidgets) {
        LockWidgetsDialog { showLockWidgets = false }
    }
    if (showLanguagePicker) {
        LanguagePickerDialog(
            selected = L10n.lang,
            onSelect = { L10n.set(contextApp, it) },
            onDismiss = { showLanguagePicker = false }
        )
    }
}

@Composable
fun LockWidgetsDialog(onDismiss: () -> Unit) {
    val context: Context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    OneUIDialog(
        title = L10n.t(S.LockWidgets),
        onDismiss = onDismiss,
        onConfirm = {
            val launch = context.packageManager.getLaunchIntentForPackage("com.samsung.android.app.dressroom")
            if (launch != null) {
                try {
                    context.startActivity(launch)
                } catch (e: ActivityNotFoundException) {
                    Toast.makeText(context, L10n.t(S.LockWidgetsToast), Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, L10n.t(S.LockWidgetsToast), Toast.LENGTH_LONG).show()
            }
            onDismiss()
        },
        confirmText = L10n.t(S.OpenEditor),
        dismissText = L10n.t(S.Close)
    ) {
        Text(
            L10n.t(S.LockWidgetsDesc),
            color = cs.onSurface,
            fontSize = 16.sp,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(12.dp))
        Text(
            L10n.t(S.LockWidgetsSteps),
            color = cs.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(12.dp))
        Text(
            L10n.t(S.LockWidgetsHint),
            modifier = Modifier.fillMaxWidth(),
            color = cs.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}

@Composable
fun SettingsContent(
    vibration: Boolean,
    fontTitle: String,
    themeTitle: String,
    onBack: () -> Unit,
    onVibration: (Boolean) -> Unit,
    toneSummary: String,
    onPickFont: () -> Unit,
    fontScale: Float = 1f,
    onScaleChange: (Float) -> Unit = {},
    onPickSound: () -> Unit,
    onFajr: (() -> Unit)? = null,
    onLocation: (() -> Unit)? = null,
    onOptionalTimes: (() -> Unit)? = null,
    onHijriConverter: (() -> Unit)? = null,
    onPickTheme: () -> Unit = {},
    onLockWidgets: () -> Unit = {},
    onPickLanguage: () -> Unit = {},
    onAbout: (() -> Unit)? = null,
    onWatch: (() -> Unit)? = null
) {
    OneUIScreen(title = L10n.t(S.Settings), onBack = onBack) {
        if (onLocation != null || onFajr != null) {
            item(key = "loc-l") { SectionLabel(L10n.t(S.LocSection)) }
            item(key = "fajr") {
                OneUICard {
                    OneUIRow(
                        title = L10n.t(S.ChangeLocation),
                        summary = L10n.t(S.LocCalcSubtitle),
                        leading = { OneUIIconBadge(Icons.Outlined.Place) },
                        trailing = { ChevronIcon() },
                        onClick = onLocation ?: onFajr
                    )
                    if (onOptionalTimes != null) {
                        OneUIDivider(70.dp)
                        OneUIRow(
                            title = L10n.t(S.OptTimesTitle),
                            summary = L10n.t(S.OptTimesSub),
                            leading = { OneUIIconBadge(Icons.Outlined.MoreTime) },
                            trailing = { ChevronIcon() },
                            onClick = onOptionalTimes
                        )
                    }
                }
            }
        }
        item(key = "lock-l") { SectionLabel(L10n.t(S.LockScreenSection)) }
        item(key = "lock") {
            OneUICard {
                OneUIRow(
                    title = L10n.t(S.LockWidgets),
                    summary = L10n.t(S.LockWidgetsRowSub),
                    leading = { OneUIIconBadge(Icons.Outlined.Widgets) },
                    trailing = { ChevronIcon() },
                    onClick = onLockWidgets
                )
            }
        }
        item(key = "notif-l") { SectionLabel(L10n.t(S.Notifications)) }
        item(key = "notif") {
            OneUICard {
                OneUIRow(
                    title = L10n.t(S.NotifSound),
                    summary = toneSummary,
                    leading = { OneUIIconBadge(Icons.Outlined.MusicNote) },
                    trailing = { ChevronIcon() },
                    onClick = onPickSound
                )
                OneUIRow(
                    title = when (L10n.lang) { com.example.ui.i18n.Lang.AR -> "الاهتزاز للإشعارات"; com.example.ui.i18n.Lang.EN -> "Notification vibration"; else -> L10n.t(S.Vibration) },
                    leading = { OneUIIconBadge(Icons.Outlined.Vibration) },
                    trailing = { OneUISwitch(vibration, onVibration) },
                    onClick = com.example.utils.AppHaptics.tap { onVibration(!vibration) }
                )
                val hapticCtx = androidx.compose.ui.platform.LocalContext.current
                androidx.compose.runtime.LaunchedEffect(Unit) { com.example.utils.AppHaptics.load(hapticCtx) }
                val hapticOn = com.example.utils.AppHaptics.enabled
                OneUIRow(
                    title = if (L10n.lang == com.example.ui.i18n.Lang.AR) "الاستجابة اللمسية" else "Haptic feedback",
                    leading = { OneUIIconBadge(Icons.Outlined.TouchApp) },
                    trailing = { OneUISwitch(hapticOn, { com.example.utils.AppHaptics.set(hapticCtx, it) }) },
                    onClick = com.example.utils.AppHaptics.tap { com.example.utils.AppHaptics.set(hapticCtx, !hapticOn) }
                )
            }
        }
        item(key = "look-l") { SectionLabel(L10n.t(S.Appearance)) }
        item(key = "look") {
            OneUICard {
                OneUIRow(
                    title = L10n.t(S.AppFont),
                    summary = fontTitle,
                    leading = { OneUIIconBadge(Icons.Outlined.TextFields) },
                    trailing = { ChevronIcon() },
                    onClick = onPickFont
                )
                FontSizeSliderRow(current = fontScale, onScale = onScaleChange)
                OneUIDivider(70.dp)
                OneUIRow(
                    title = L10n.t(S.Appearance),
                    summary = themeTitle,
                    leading = { OneUIIconBadge(Icons.Outlined.DarkMode) },
                    trailing = { ChevronIcon() },
                    onClick = onPickTheme
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = L10n.t(S.Language),
                    summary = L10n.lang.nativeName,
                    leading = { OneUIIconBadge(Icons.Outlined.Language) },
                    trailing = { ChevronIcon() },
                    onClick = onPickLanguage
                )
                if (L10n.lang == com.example.ui.i18n.Lang.AR) {
                    OneUIDivider(70.dp)
                    DigitsStyleRow()
                }
            }
        }
        if (onWatch != null) {
            item(key = "watch-l") { SectionLabel(if (L10n.lang == com.example.ui.i18n.Lang.AR) "الساعة الذكية" else "Smartwatch") }
            item(key = "watch") {
                OneUICard {
                    OneUIRow(
                        title = if (L10n.lang == com.example.ui.i18n.Lang.AR) "إعدادات الساعة الذكية" else "Smartwatch settings",
                        summary = if (L10n.lang == com.example.ui.i18n.Lang.AR) "الإعدادات الخاصة بتطبيق وذكّر للساعة" else "Settings for the Wathkkir watch app",
                        leading = { OneUIIconBadge(androidx.compose.material.icons.Icons.Outlined.Watch) },
                        trailing = { ChevronIcon() },
                        onClick = onWatch
                    )
                }
            }
        }
        if (onAbout != null) {
            item(key = "about") {
                OneUICard {
                    OneUIRow(
                        title = L10n.t(S.About),
                        leading = { OneUIIconBadge(Icons.Outlined.Info) },
                        trailing = { ChevronIcon() },
                        onClick = onAbout
                    )
                }
            }
        }
    }
}


/** Numbers type (Arabic UI only): One UI segmented choice between Hindi-Indic and Latin digits. */
@androidx.compose.runtime.Composable
private fun DigitsStyleRow() {
    val cs = androidx.compose.material3.MaterialTheme.colorScheme
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val hindi = com.example.utils.Digits.isHindiChoice
    androidx.compose.foundation.layout.Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
        androidx.compose.material3.Text("نوع الأرقام", color = cs.onSurface, fontSize = 16.sp)
        androidx.compose.foundation.layout.Spacer(Modifier.height(10.dp))
        // 3.3.3: shared One UI segmented control (joined container, sliding blue indicator).
        // rawLabels: these two example labels stay literal under BOTH numeral settings - this
        // selector is the only place exempt from the global digits conversion (his spec).
        com.example.ui.oneui.OneUISegmented(
            options = listOf("أرقام هندية  ١٢٣", "أرقام عربية  1234"),
            selected = if (hindi) 0 else 1,
            onSelect = { com.example.utils.Digits.set(ctx, it == 0) },
            rawLabels = true
        )
    }
}
