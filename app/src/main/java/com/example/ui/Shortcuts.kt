package com.example.ui

import android.content.Context
import android.content.Intent
import android.net.Uri
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S

/** Launcher long-press shortcuts: Qibla, Fajr adhan alarm, date converter (same icons as the drawer). */
object ShortcutNav {
    /** Route requested by a shortcut; MainScreen navigates to it and clears it. */
    var route by mutableStateOf<String?>(null)

    fun fromIntent(i: Intent?) {
        val d = i?.data ?: return
        if (d.scheme == "wathakker" && d.host == "open") {
            val go = d.getQueryParameter("go")
            if (go == "qibla" || go == "fajr_alarm" || go == "hijri_converter") route = go
        }
    }

    fun publish(c: Context) {
        try {
            val app = c.applicationContext
            fun sc(id: String, label: String, icon: Int, go: String) = ShortcutInfoCompat.Builder(app, id)
                .setShortLabel(label).setLongLabel(label)
                .setIcon(IconCompat.createWithResource(app, icon))
                .setIntent(Intent(Intent.ACTION_VIEW, Uri.parse("wathakker://open?go=$go")).setClassName(app.packageName, "com.example.MainActivity"))
                .build()
            val list = listOf(
                sc("qibla", L10n.t(S.QiblaTitle), com.example.R.drawable.ic_shortcut_explore, "qibla"),
                sc("fajr_alarm", L10n.t(S.FajrAlarm), com.example.R.drawable.ic_shortcut_alarm, "fajr_alarm"),
                sc("hijri_converter", L10n.t(S.HijriConvTitle), com.example.R.drawable.ic_shortcut_date_range, "hijri_converter")
            )
            ShortcutManagerCompat.setDynamicShortcuts(app, list)
        } catch (_: Throwable) {}
    }
}
