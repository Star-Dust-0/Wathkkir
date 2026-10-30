package com.example.ui.screens

import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.i18n.Lang
import android.app.Activity
import android.content.ActivityNotFoundException
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.widget.Toast
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.ui.oneui.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun SettingsScreen(viewModel: MainViewModel, onBack: () -> Unit, onNavigateToFajr: (() -> Unit)? = null, onLocation: (() -> Unit)? = null, onOptionalTimes: (() -> Unit)? = null) {
    val vibration by viewModel.vibrationEnabled.collectAsState()
    val toneState by viewModel.notificationSound.collectAsState()
    val settingsCtx = androidx.compose.ui.platform.LocalContext.current
    val toneSummary = remember(toneState) { com.example.utils.NotifPrefs.toneTitle(settingsCtx) ?: L10n.t(S.Default) }
    val appFont by viewModel.appFont.collectAsState()
    var showFontPicker by remember { mutableStateOf(false) }
    var showLockWidgets by remember { mutableStateOf(false) }
    var showTheme by remember { mutableStateOf(false) }
    var showLanguage by remember { mutableStateOf(false) }
    val themeMode by com.example.ThemeSettings.mode.collectAsState()
    val fontScale by com.example.FontScaleSettings.scale.collectAsState()
    val ringtoneLauncher = rememberLauncherForActivityResult(ActivityResultContracts.StartActivityForResult()) { result ->
        if (result.resultCode == Activity.RESULT_OK) {
            val uri: Uri? = result.data?.getParcelableExtra(RingtoneManager.EXTRA_RINGTONE_PICKED_URI)
            if (uri != null) viewModel.updateNotificationSound(uri.toString())
        }
    }
    SettingsContent(
        vibration = vibration,
        fontTitle = com.example.ui.theme.FontChoice.fromId(appFont).title,
        onBack = onBack,
        onVibration = viewModel::updateVibrationEnabled,
        onPickFont = { showFontPicker = true },
        onPickSound = {
            ringtoneLauncher.launch(Intent(RingtoneManager.ACTION_RINGTONE_PICKER).apply {
                putExtra(RingtoneManager.EXTRA_RINGTONE_TYPE, RingtoneManager.TYPE_NOTIFICATION)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_DEFAULT, true)
                putExtra(RingtoneManager.EXTRA_RINGTONE_SHOW_SILENT, true)
            })
        },
        toneSummary = toneSummary,
        onLocation = onLocation,
        onOptionalTimes = onOptionalTimes,
        onLockWidgets = { showLockWidgets = true },
        themeTitle = L10n.t(when(themeMode){"light"->S.Light;"dark"->S.Dark;else->S.SystemAuto}),
        onTheme = { showTheme = true },
        onLanguage = { showLanguage = true },
        fontScale = fontScale,
        onFontScale = { com.example.FontScaleSettings.set(settingsCtx,it) },
        onWatch = { HeaderTools.nav?.navigate("watch_settings") },
        onAbout = { HeaderTools.nav?.navigate("about") }
    )

    if (showTheme) OneUIDialog(title=L10n.t(S.Appearance),onDismiss={showTheme=false},onConfirm=null,dismissText=L10n.t(S.Close)) {
        listOf("auto" to S.SystemAuto,"light" to S.Light,"dark" to S.Dark).forEach { (key,label) ->
            OneUIRow(L10n.t(label),onClick={
                settingsCtx.getSharedPreferences("app_prefs",0).edit().putString("theme_mode",key).apply()
                com.example.ThemeSettings.setMode(key);showTheme=false
            },trailing={if(themeMode==key) ChevronIcon()})
        }
    }
    if (showLanguage) OneUIDialog(title=L10n.t(S.Language),onDismiss={showLanguage=false},onConfirm=null,dismissText=L10n.t(S.Close)) {
        androidx.compose.foundation.lazy.LazyColumn {
            items(Lang.entries.size) { i -> val language=Lang.entries[i]
                OneUIRow(language.nativeName,onClick={L10n.set(settingsCtx,language);showLanguage=false
                    com.example.utils.WearSync.push(settingsCtx)
                    com.example.widget.WidgetUpdater.INSTANCE.updateAll(settingsCtx)
                },trailing={if(L10n.lang==language) ChevronIcon()})
            }
        }
    }
    if (showFontPicker) {
        FontPickerDialog(
            selected = com.example.ui.theme.FontChoice.fromId(appFont),
            onSelect = { viewModel.updateAppFont(it.id) },
            onDismiss = { showFontPicker = false }
        )
    }
    if (showLockWidgets) {
        LockWidgetsDialog(onDismiss = { showLockWidgets = false })
    }
}

/** Explains + launches Samsung's native lock screen editor so the user can pin
 *  any Wathakker widget in the dedicated under-clock area (One UI 8 / Android 16 QPR). */
@Composable
fun LockWidgetsDialog(onDismiss: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val toast = L10n.t(S.LockWidgetsToast)
    OneUIDialog(
        title = L10n.t(S.LockWidgets),
        onDismiss = onDismiss,
        onConfirm = {
            // Samsung "Wallpaper & style" (DressRoom) hosts the native lock screen editor on One UI 6+.
            val launch = context.packageManager.getLaunchIntentForPackage("com.samsung.android.app.dressroom")
            if (launch != null) {
                try { context.startActivity(launch) } catch (_: ActivityNotFoundException) {
                    Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
                }
            } else {
                Toast.makeText(context, toast, Toast.LENGTH_LONG).show()
            }
            onDismiss()
        },
        confirmText = L10n.t(S.OpenEditor),
        dismissText = L10n.t(S.Close)
    ) {
        Text(
            L10n.t(S.LockWidgetsDesc),
            style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, fontSize = 16.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            L10n.t(S.LockWidgetsSteps),
            style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, fontSize = 16.sp
        )
        Spacer(Modifier.height(14.dp))
        Text(
            L10n.t(S.LockWidgetsHint),
            style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

@Composable
fun SettingsContent(
    vibration: Boolean,
    fontTitle: String,
    onBack: () -> Unit,
    onVibration: (Boolean) -> Unit,
    toneSummary: String,
    onPickFont: () -> Unit,
    onPickSound: () -> Unit,
    onLocation: (() -> Unit)?,
    onOptionalTimes: (() -> Unit)?,
    onLockWidgets: () -> Unit = {},
    themeTitle: String = "",
    onTheme: () -> Unit = {},
    onLanguage: () -> Unit = {},
    fontScale: Float = com.example.FontScaleSettings.DEFAULT,
    onFontScale: (Float) -> Unit = {},
    onWatch: (() -> Unit)? = null,
    onAbout: (() -> Unit)? = null
) {
    val ctx = LocalContext.current
    LaunchedEffect(ctx) { com.example.utils.AppHaptics.load(ctx) }
    val hapticView = androidx.compose.ui.platform.LocalView.current
    val haptics = com.example.utils.AppHaptics.enabled
    // Original: Arabic and English carry their own literals, other languages use the table.
    val vibLabel = when (L10n.lang) { Lang.AR -> "الاهتزاز للإشعارات"; Lang.EN -> "Notification vibration"; else -> L10n.t(S.Vibration) }
    val hapticLabel = when (L10n.lang) { Lang.AR -> "الاستجابة اللمسية"; else -> "Haptic feedback" }
    OneUIScreen(title = L10n.t(S.Settings), onBack = onBack) {
        item(key = "loc-l") { SectionLabel(L10n.t(S.LocSection)) }
        item(key = "loc") {
            OneUICard {
                OneUIRow(L10n.t(S.ChangeLocation), summary = L10n.t(S.LocCalcSubtitle), onClick = { onLocation?.invoke() },
                    leading = { OneUIIconBadge(Icons.Outlined.LocationOn) }, trailing = { ChevronIcon() })
                OneUIDivider(inset = 70.dp)
                OneUIRow(L10n.t(S.OptTimesTitle), summary = L10n.t(S.OptTimesSub), onClick = { onOptionalTimes?.invoke() },
                    leading = { OneUIIconBadge(Icons.Outlined.MoreTime) }, trailing = { ChevronIcon() })
            }
        }
        item(key = "lock-l") { SectionLabel(L10n.t(S.LockScreenSection)) }
        item(key = "lock") {
            OneUICard {
                OneUIRow(L10n.t(S.LockWidgets), summary = L10n.t(S.LockWidgetsRowSub), onClick = onLockWidgets,
                    leading = { OneUIIconBadge(Icons.Outlined.Widgets) }, trailing = { ChevronIcon() })
            }
        }
        item(key = "notif-l") { SectionLabel(L10n.t(S.Notifications)) }
        item(key = "notif") {
            OneUICard {
                OneUIRow(L10n.t(S.NotifSound), summary = toneSummary, onClick = onPickSound,
                    leading = { OneUIIconBadge(Icons.Outlined.MusicNote) }, trailing = { ChevronIcon() })
                OneUIRow(vibLabel, leading = { OneUIIconBadge(Icons.Outlined.Vibration) },
                    trailing = { OneUISwitch(vibration, onVibration) }, onClick = { com.example.utils.AppHaptics.tick(hapticView); onVibration(!vibration) })
                OneUIRow(hapticLabel, leading = { OneUIIconBadge(Icons.Outlined.TouchApp) },
                    trailing = { OneUISwitch(haptics, { com.example.utils.AppHaptics.set(ctx, it) }) },
                    onClick = { com.example.utils.AppHaptics.tick(hapticView); com.example.utils.AppHaptics.set(ctx, !haptics) })
            }
        }
        item(key = "look-l") { SectionLabel(L10n.t(S.Appearance)) }
        item(key = "look") {
            OneUICard {
                OneUIRow(L10n.t(S.AppFont), summary = fontTitle, onClick = onPickFont,
                    leading = { OneUIIconBadge(Icons.Outlined.TextFields) }, trailing = { ChevronIcon() })
                FontSizeSliderRow(fontScale,onFontScale)
                OneUIDivider(70.dp)
                OneUIRow(L10n.t(S.Appearance),summary=themeTitle,onClick=onTheme,leading={OneUIIconBadge(Icons.Outlined.DarkMode)},trailing={ChevronIcon()})
                OneUIDivider(70.dp)
                OneUIRow(L10n.t(S.Language),summary=L10n.lang.nativeName,onClick=onLanguage,leading={OneUIIconBadge(Icons.Outlined.Language)},trailing={ChevronIcon()})
                if(L10n.lang==Lang.AR) {
                    OneUIDivider(70.dp)
                    Column(Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 12.dp)) {
                        Text("نوع الأرقام", style = MaterialTheme.typography.bodyLarge, fontSize = 16.sp)
                        Spacer(Modifier.height(10.dp))
                        OneUISegmented(options=listOf("أرقام هندية  ١٢٣","أرقام عربية  1234"),
                            selected=if(com.example.utils.Digits.isHindiChoice)0 else 1,
                            onSelect={com.example.utils.Digits.set(ctx,it==0);com.example.widget.WidgetUpdater.INSTANCE.updateAll(ctx)})
                    }
                }
            }
        }
        // Original rh0.d/rc0: the phone settings links to watch settings and About.
        onWatch?.let { open ->
            item(key="watch-l") { SectionLabel(if(L10n.lang==Lang.AR) "الساعة الذكية" else "Smartwatch") }
            item(key="watch") { OneUICard {
                OneUIRow(if(L10n.lang==Lang.AR) "إعدادات الساعة الذكية" else "Smartwatch settings",
                    summary=if(L10n.lang==Lang.AR) "الإعدادات الخاصة بتطبيق وذكّر للساعة" else "Settings for the Wathkkir watch app",
                    onClick=open,leading={OneUIIconBadge(Icons.Outlined.Watch)},trailing={ChevronIcon()})
            } }
        }
        onAbout?.let { open ->
            item(key="about") { OneUICard {
                OneUIRow(L10n.t(S.About),onClick=open,leading={OneUIIconBadge(Icons.Outlined.Info)},trailing={ChevronIcon()})
            } }
        }
    }
}
