package com.example

import android.app.Activity
import android.graphics.Bitmap
import android.os.Bundle
import com.example.lockwidgets.HomeWidgets
import java.io.File
import java.io.FileOutputStream

/** Debug-only render harness: draws every home widget kind in three background modes
 *  (explicit light, explicit dark, unset/default) using the real renderer, and saves PNGs. */
class WidgetRenderCheck : Activity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        com.example.utils.Digits.refresh(this)
        com.example.fajr.data.FajrPreferences(this).apply { latitude = 30.0444; longitude = 31.2357 }
        val sp = getSharedPreferences("fajr_prefs", 0)
        sp.edit()
            .putString("widget_bg_color_991", "white")
            .putString("widget_bar_color_991", "blue")
            .putString("widget_bg_color_992", "black")
            .remove("widget_bg_color_993")
            .remove("widget_bar_color_993")
            .apply()
        // External app-files dir can be missing when the AVD has no mounted sdcard
        // (vold: "Failed to find mounted volume"); fall back to internal storage.
        val out = getExternalFilesDir(null) ?: java.io.File(filesDir, "rendercheck")
        out.mkdirs()
        val dims = mapOf(
            "Hero" to (330 to 90), "HeroLarge" to (331 to 180), "Schedule" to (330 to 90),
            "Clock" to (250 to 90), "Hijri" to (90 to 90), "Gregorian" to (90 to 90),
            "NextPrayer" to (140 to 60), "PrayerBar" to (110 to 90),  // NextPrayer: match provider XML default 2x1 (min 140x60dip, targetCell 2x1)
            "DayPrayer" to (110 to 90), "Pill" to (110 to 90)
        )
        val kinds = listOf("Hero", "NextPrayer", "PrayerBar", "Schedule", "Clock", "DayPrayer", "Pill", "Hijri", "Gregorian")
        // Light-theme picker-preview generation: every kind at its preview-PNG aspect
        // (original previews were authored at density 2.625), light bg, DEFAULT bar.
        val sp2 = sp.edit().putString("widget_bg_color_994", "white").remove("widget_bar_color_994")
        sp2.apply()
        val previewDims = mapOf(
            "Hero" to (330 to 90), "HeroLarge" to (331 to 160), "Schedule" to (276 to 90),
            "Clock" to (203 to 90), "Hijri" to (90 to 90), "Gregorian" to (90 to 90),
            "NextPrayer" to (90 to 90), "PrayerBar" to (130 to 90),
            "DayPrayer" to (130 to 90), "Pill" to (130 to 90)
        )
        for (kind in previewDims.keys) {
            try {
                val (w, h) = previewDims[kind] ?: (110 to 90)
                // HeroLarge shares the Hero canvas (provider maps HeroLarge -> Hero), just taller.
                val rk = if (kind == "HeroLarge") "Hero" else kind
                val bmp = HomeWidgets.renderKind(this, rk, w, h, 994)
                FileOutputStream(File(out, "preview_light_${kind}.png")).use {
                    bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (e: Exception) {
                File(out, "preview_light_${kind}.err").writeText(e.toString())
            }
        }
        // Picker previews for the One UI widget: real renders (330x90 dp, card + date tile), both themes.
        File(out, "oneui_preview_started.txt").writeText("started")
        for ((tag, nightFlag) in listOf("light" to android.content.res.Configuration.UI_MODE_NIGHT_NO, "dark" to android.content.res.Configuration.UI_MODE_NIGHT_YES)) {
            try {
                val cfg = android.content.res.Configuration(resources.configuration)
                cfg.uiMode = (cfg.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK.inv()) or nightFlag
                val ctx = createConfigurationContext(cfg)
                val bmp = HomeWidgets.renderKind(ctx, "OneUI", 330, 90, 994)
                FileOutputStream(File(out, "oneui_preview_${tag}.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
            } catch (e: Throwable) {
                android.util.Log.e("WathakkerHome", "oneui preview failed", e)
                File(out, "oneui_preview_${tag}.err").writeText(e.toString())
            }
        }
        sp.edit()
            .putString("widget_bg_color_995", "glass").remove("widget_bar_color_995")
            .putString("widget_bg_color_996", "wallblur").remove("widget_bar_color_996")
            .apply()
        for ((id, tag) in listOf(991 to "light", 992 to "dark", 993 to "default", 995 to "glass", 996 to "wallblur")) {
            for (kind in kinds) {
                try {
                    val (w, h) = dims[kind] ?: (110 to 90)
                    val bmp = HomeWidgets.renderKind(this, kind, w, h, id)
                    FileOutputStream(File(out, "widget_${tag}_${kind}.png")).use {
                        bmp.compress(Bitmap.CompressFormat.PNG, 100, it)
                    }
                } catch (e: Exception) {
                    File(out, "widget_${tag}_${kind}.err").writeText(e.toString())
                }
            }
        }
        // 1x1 next-prayer cell at realistic sizes (spacing check)
        for ((id, tag) in listOf(991 to "light", 992 to "dark")) {
            for ((w, h) in listOf(70 to 70, 70 to 100, 74 to 90)) {
                try {
                    val bmp = HomeWidgets.renderKind(this, "NextPrayer", w, h, id)
                    FileOutputStream(File(out, "widget_${tag}_NextPrayer_${w}x${h}.png")).use { bmp.compress(Bitmap.CompressFormat.PNG, 100, it) }
                } catch (e: Exception) { File(out, "widget_${tag}_NextPrayer_${w}x${h}.err").writeText(e.toString()) }
            }
        }
        File(out, "widget_prefs.txt").writeText(sp.all.toSortedMap().toString())
        val c = java.util.Calendar.getInstance()
        val pref = com.example.fajr.data.FajrPreferences(this)
        val method = com.example.fajr.calculation.CalculationMethod.INSTANCE.fromId(pref.calculationMethodId)
        val pt = com.example.fajr.calculation.PrayerAdjustments.INSTANCE.calculateAll(this, c, pref.latitude, pref.longitude, method)
        File(out, "actual_prayers.txt").writeText(listOf(pt.fajr,pt.sunrise,pt.dhuhr,pt.asr,pt.maghrib,pt.isha).joinToString("\n"))
        for ((type, tag) in listOf(com.example.lockwidgets.LockWidgets.Bar::class.java to "Bar", com.example.lockwidgets.LockWidgets.Next::class.java to "Next", com.example.lockwidgets.LockWidgets.Countdown::class.java to "Countdown", com.example.lockwidgets.LockWidgets.CountdownClean::class.java to "CountdownClean")) {
            val bmp = com.example.lockwidgets.LockWidgets.renderSize(this, type, 0, false)
            FileOutputStream(File(out, "lock_${tag}.png")).use { bmp.compress(Bitmap.CompressFormat.PNG,100,it) }
        }
        finish()
    }
}
