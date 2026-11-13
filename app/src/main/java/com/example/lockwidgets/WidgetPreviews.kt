package com.example.lockwidgets

import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.os.Build
import android.util.Log
import android.widget.RemoteViews

/**
 * Android 15+ generated ("live") widget picker previews. Each preview is produced by the same
 * drawing code as the real widget, so the picker always shows what the widget really looks like
 * (bars fill left to right). Older Android keeps the static preview images from res/drawable.
 */
object WidgetPreviews {
    private const val TAG = "WidgetPreviews"

    @Volatile
    private var running = false

    private fun res(c: Context, type: String, name: String): Int =
        c.resources.getIdentifier(name, type, c.packageName)

    /** Regenerates the previews once per app version, language and light/dark mode. */
    @JvmStatic
    fun maybeGenerate(ctx: Context) {
        if (Build.VERSION.SDK_INT < 35) return
        val c = ctx.applicationContext
        val key = pkgVersion(c).toString() + "|" + HomeWidgets.lang(c) + "|" +
            (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) + "|" +
            c.getSharedPreferences("app_prefs", 0).getString("digits_style", "latin") + "|p2"
        val sp = c.getSharedPreferences("preview_prefs", 0)
        if (key == sp.getString("generated", null)) return
        synchronized(WidgetPreviews::class.java) {
            if (running) return
            running = true
        }
        Thread({
            try {
                val ok = generate(c)
                Log.i(TAG, "generated previews ok=$ok")
                if (ok > 0) sp.edit().putString("generated", key).apply()
            } catch (t: Throwable) {
                Log.e(TAG, "preview generation failed", t)
            } finally {
                synchronized(WidgetPreviews::class.java) { running = false }
            }
        }, "widget-previews").start()
    }

    private fun pkgVersion(c: Context): Long =
        try {
            c.packageManager.getPackageInfo(c.packageName, 0).longVersionCode
        } catch (e: Exception) {
            0
        }

    /** Returns how many previews were accepted by the system. */
    @JvmStatic
    fun generate(c: Context): Int {
        com.example.utils.Digits.refresh(c)
        val m = AppWidgetManager.getInstance(c)
        val st = HomeWidgets.state(c)
        var ok = 0
        HomeWidgets.batchState.set(st)
        try {
            val kinds = arrayOf("Hero", "NextPrayer", "PrayerBar", "Schedule", "DayPrayer", "Pill", "Clock", "Hijri", "Gregorian", "OneUI")
            for (kind in kinds) {
                try {
                    // Sizes follow each widget's target cell span (cell ~80x90dp) so the preview has the same aspect as the picker box and is never stretched.
                    val wd: Int
                    val ht: Int
                    when (kind) {
                        "Hero", "Schedule", "OneUI" -> { wd = 330; ht = 90 }
                        "Clock" -> { wd = 240; ht = 90 }
                        "Hijri", "Gregorian" -> { wd = 82; ht = 90 }
                        "NextPrayer" -> { wd = 160; ht = 90 } // 3.3.3: default footprint now 2 wide x 1 tall (his spec)
                        else -> { wd = 160; ht = 82 } // 2x1: PrayerBar, DayPrayer, Pill
                    }
                    val v = RemoteViews(c.packageName, res(c, "layout", "home_prayer_art_fit"))
                    HomeWidgets.ovBegin()
                    val b = HomeWidgets.renderKind(c, kind, wd, ht, 0)
                    try { b.setDensity(1) } catch (e: Throwable) { Log.w("WidgetPreviews", "setDensity failed", e) }
                    v.setImageViewBitmap(res(c, "id", "home_prayer_image"), b)
                    val pd = c.resources.displayMetrics.density
                    HomeWidgets.ovApply(c, v, Math.round(wd * pd), Math.round(ht * pd))
                    if (set(m, c, "com.example.widget.${kind}WidgetProvider", v)) ok++
                } catch (t: Throwable) {
                    Log.e(TAG, "home preview failed: $kind", t)
                }
            }
        } finally {
            HomeWidgets.batchState.remove()
        }
        val locks = arrayOf(
            LockWidgets.Day::class.java, LockWidgets.Tasbih::class.java, LockWidgets.Hijri::class.java, LockWidgets.Gregorian::class.java,
            LockWidgets.Next::class.java, LockWidgets.Bar::class.java, LockWidgets.Countdown::class.java, LockWidgets.CountdownClean::class.java,
        )
        for (t in locks) {
            try {
                val v = RemoteViews(c.packageName, res(c, "layout", "lock_widget_image"))
                HomeWidgets.ovBegin()
                val b = LockWidgets.renderSize(c, t, 0, false)
                v.setImageViewBitmap(res(c, "id", "lock_widget_art"), b)
                if (set(m, c, t.name, v)) ok++
            } catch (e: Throwable) {
                Log.e(TAG, "lock preview failed: ${t.simpleName}", e)
            }
        }
        return ok
    }

    private fun set(m: AppWidgetManager, c: Context, cls: String, v: RemoteViews): Boolean {
        val cn = ComponentName(c, cls)
        var cats = 0
        for (i in m.getInstalledProvidersForPackage(c.packageName, null)) {
            if (i.provider == cn) cats = i.widgetCategory
        }
        if (cats == 0) cats = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN
        val r = m.setWidgetPreview(cn, cats, v)
        Log.i(TAG, "setWidgetPreview ${cls.substring(cls.lastIndexOf('.') + 1)} cats=$cats -> $r")
        return r
    }
}
