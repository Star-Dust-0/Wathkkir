package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Sync
import androidx.compose.material.icons.outlined.Watch
import androidx.compose.material.icons.outlined.Download
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.AccessTime
import androidx.compose.material3.MaterialTheme
import com.example.ui.oneui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalUriHandler
import androidx.compose.ui.unit.dp
import com.example.ui.i18n.L10n
import com.example.ui.i18n.Lang
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.OneUISwitch
import com.example.ui.oneui.SectionLabel
import com.example.utils.WearSync
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

private const val WATCH_RELEASE_URL = "https://github.com/MohamedElnaggar00/Wathkkir/releases/tag/v3.3.0"

private fun w(ar: String, en: String) = if (L10n.lang == Lang.AR) ar else en

@Composable
fun WatchSettingsScreen(onBack: () -> Unit) {
    val ctx = LocalContext.current
    val uri = LocalUriHandler.current
    val cs = MaterialTheme.colorScheme
    var h24 by remember { mutableStateOf(WearSync.is24h(ctx)) }
    var last by remember { mutableLongStateOf(WearSync.lastSyncMillis(ctx)) }
    var watches by remember { mutableStateOf<List<String>?>(null) }
    var syncing by remember { mutableStateOf(false) }
    var failed by remember { mutableStateOf(false) }
    LaunchedEffect(Unit) { WearSync.connectedWatches(ctx) { watches = it } }

    fun fmt(ms: Long): String =
        if (ms <= 0L) w("لم تتم المزامنة بعد", "Not synced yet")
        else SimpleDateFormat("yyyy-MM-dd  hh:mm a", Locale.ENGLISH).format(Date(ms))

    OneUIScreen(title = w("إعدادات الساعة الذكية", "Smartwatch settings"), onBack = onBack) {
        item(key = "note") {
            OneUICard {
                OneUIRow(
                    title = w("ملاحظة", "Note"),
                    summary = w(
                        "تطبيق وذكّر للساعة مستقل عن تطبيق الهاتف، ويُثبَّت على الساعة بشكل منفصل. يعمل على ساعات Wear OS، ويظهر بالوضع الداكن فقط.",
                        "The Wathkkir watch app is separate from the phone app and is installed on the watch on its own. It runs on Wear OS watches and uses the dark theme only."
                    ),
                    leading = { OneUIIconBadge(Icons.Outlined.Watch) }
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = w("تحميل تطبيق الساعة", "Download the watch app"),
                    summary = w("ملف التثبيت في صفحة الإصدار على GitHub", "The install file is on the GitHub release page"),
                    leading = { OneUIIconBadge(Icons.Outlined.Download) },
                    onClick = { uri.openUri(WATCH_RELEASE_URL) }
                )
            }
        }
        item(key = "sync-l") { SectionLabel(w("المزامنة", "Sync")) }
        item(key = "sync") {
            OneUICard {
                OneUIRow(
                    title = w("حالة الاتصال", "Connection"),
                    summary = when {
                        watches == null -> w("جارٍ التحقق...", "Checking...")
                        watches!!.isEmpty() -> w("لا توجد ساعة متصلة حاليًا", "No watch is connected right now")
                        else -> w("متصلة: ", "Connected: ") + watches!!.joinToString("، ")
                    },
                    leading = { OneUIIconBadge(Icons.Outlined.Watch) }
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = w("آخر مزامنة", "Last sync"),
                    summary = fmt(last),
                    leading = { OneUIIconBadge(Icons.Outlined.Schedule) }
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = w("مزامنة الآن", "Sync now"),
                    summary = when {
                        syncing -> w("جارٍ الإرسال...", "Sending...")
                        failed -> w("تعذر الإرسال. تأكد من اتصال الساعة وحاول مرة أخرى.", "Could not send. Check the watch connection and try again.")
                        else -> w("إرسال الموقع وخيارات العرض إلى الساعة", "Send the location and display options to the watch")
                    },
                    leading = { OneUIIconBadge(Icons.Outlined.Sync) },
                    onClick = {
                        syncing = true; failed = false
                        WearSync.push(ctx) { ok ->
                            syncing = false; failed = !ok; last = WearSync.lastSyncMillis(ctx)
                            WearSync.connectedWatches(ctx) { watches = it }
                        }
                    }
                )
            }
        }
        item(key = "disp-l") { SectionLabel(w("العرض", "Display")) }
        item(key = "disp") {
            OneUICard {
                OneUIRow(
                    title = w("نظام 24 ساعة", "24-hour time"),
                    summary = w("عرض أوقات الصلاة على الساعة بنظام 24 ساعة", "Show prayer times on the watch in 24-hour format"),
                    leading = { OneUIIconBadge(Icons.Outlined.AccessTime) },
                    trailing = { OneUISwitch(h24, { h24 = it; WearSync.setHour24(ctx, it) }) },
                    onClick = com.example.utils.AppHaptics.tap { h24 = !h24; WearSync.setHour24(ctx, h24) }
                )
            }
        }
    }
}
