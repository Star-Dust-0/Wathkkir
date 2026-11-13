package com.example.widget

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.SystemClock
import android.util.SizeF
import android.widget.RemoteViews
import com.example.MainActivity
import com.example.R
import com.example.fajr.calculation.CalculationMethod
import com.example.fajr.calculation.PrayerAdjustments
import com.example.fajr.data.FajrPreferences
import com.example.lockwidgets.HomeWidgets
import com.example.prayer.HijriDate
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

object WidgetUpdater {
    private val iqamaOffsets = mapOf("الفجر" to 25, "الظهر" to 20, "العصر" to 20, "المغرب" to 10, "العشاء" to 20)
    private val barVariants = mapOf(
        R.layout.widget_hero to mapOf("amber" to R.layout.widget_hero_amber, "blue" to R.layout.widget_hero_blue, "green" to R.layout.widget_hero_green),
        R.layout.widget_hero_compact to mapOf("amber" to R.layout.widget_hero_compact_amber, "blue" to R.layout.widget_hero_compact_blue, "green" to R.layout.widget_hero_compact_green),
        R.layout.widget_iqama to mapOf("white" to R.layout.widget_iqama_white, "blue" to R.layout.widget_iqama_blue, "green" to R.layout.widget_iqama_green),
        R.layout.widget_iqama_small to mapOf("white" to R.layout.widget_iqama_small_white, "blue" to R.layout.widget_iqama_small_blue, "green" to R.layout.widget_iqama_small_green),
        R.layout.widget_prayer_bar to mapOf("amber" to R.layout.widget_prayer_bar_amber, "blue" to R.layout.widget_prayer_bar_blue, "green" to R.layout.widget_prayer_bar_green),
        R.layout.widget_wide_bar to mapOf("amber" to R.layout.widget_wide_bar_amber, "blue" to R.layout.widget_wide_bar_blue, "green" to R.layout.widget_wide_bar_green),
    )

    fun updateAll(context: Context) {
        HomeWidgets.updateAll(context)
        try { pushFaceWidget(context, FaceWidgetReceiver.PAGE_ID_PRAYER) } catch (e: Exception) {}
    }

    /** Face-widget push only - used by the minute pulse, which already updates home widgets itself. */
    fun updateFace(context: Context) {
        try { pushFaceWidget(context, FaceWidgetReceiver.PAGE_ID_PRAYER) } catch (e: Exception) { android.util.Log.w("WathakkerHome", "face widget push failed", e) }
    }

    private fun formatTime(millis: Long): String {
        val cal = Calendar.getInstance(); cal.timeInMillis = millis
        val lx0 = L10n
        val ampm = if (cal.get(Calendar.AM_PM) == 0) lx0.t(S.AmMarker) else lx0.t(S.PmMarker)
        return SimpleDateFormat("h:mm", Locale.US).format(cal.time) + " " + ampm
    }

    private fun formatTimeShort(millis: Long): String {
        val simpleDateFormat = SimpleDateFormat("h:mm", Locale.US)
        val calendar = Calendar.getInstance(); calendar.timeInMillis = millis
        return simpleDateFormat.format(calendar.time)
    }

    data class IqamaState(val prayerName: String, val adhanMillis: Long, val iqamaMillis: Long, val progressPct: Int, val nextIqamaMillis: Long)

    data class PrayerState(val names: List<String>, val times: List<Long>, val nextIndex: Int, val nextName: String, val nextMillis: Long, val prevName: String, val prevMillis: Long, val city: String)

    private class W(val name: String, val adhan: Long, val iqama: Long)

    private fun sortedEvents(first: W, middle: List<W>, last: W, comparator: Comparator<W>): List<W> {
        val all = ArrayList<W>(); all.add(first); all.addAll(middle); all.add(last); all.sortWith(comparator); return all
    }

    private fun applyToAll(context: Context, appWidgetManager: AppWidgetManager, cls: Class<*>, remoteViews: RemoteViews) {
        val appWidgetIds = appWidgetManager.getAppWidgetIds(ComponentName(context, cls))
        if (appWidgetIds.isNotEmpty()) appWidgetManager.updateAppWidget(appWidgetIds, remoteViews)
    }

    private fun barLayout(context: Context, i: Int): Int {
        val widgetBarColor = FajrPreferences(context).widgetBarColor
        return if (widgetBarColor == "default") i else barVariants[i]?.get(widgetBarColor) ?: i
    }

    private fun compactRemaining(j: Long, str: String): String {
        var i = (j / 60000).toInt(); if (i < 0) i = 0
        val i2 = i / 60; val i3 = i % 60
        if (str.startsWith("ar")) {
            val sb = StringBuilder()
            if (i2 > 0) { sb.append(i2); sb.append(" س ") }
            sb.append(i3); sb.append(" د")
            return sb.toString()
        }
        val str3: String; val str2: String
        if (str.startsWith("tr")) { str3 = "sa"; str2 = "dk" }
        else if (str.startsWith("id")) { str3 = "jam"; str2 = "mnt" }
        else if (str.startsWith("ru")) { str3 = "ч"; str2 = "мин" }
        else if (str.startsWith("ur")) { str3 = "گھنٹے"; str2 = "منٹ" }
        else { str2 = if (str.startsWith("en")) "m" else "min"; str3 = "h" }
        val sb3 = StringBuilder()
        if (i2 > 0) sb3.append(i2).append(" ").append(str3).append(" ")
        sb3.append(i3); sb3.append(" "); sb3.append(str2)
        return sb3.toString()
    }

    private fun computeIqamaState(context: Context, prayerState: PrayerState, j: Long): IqamaState? {
        val listAsList = listOf("الفجر", "الظهر", "العصر", "المغرب", "العشاء")
        val arrayList = ArrayList<W>()
        val size = prayerState.names.size
        val z = Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        var i = 0
        while (i < size) {
            val str = prayerState.names[i]
            if (!(!listAsList.contains(str) || (z && "الظهر" == str))) {
                arrayList.add(W(str, prayerState.times[i], iqamaOf(str, prayerState.times[i])))
            }
            i++
        }
        val fajrPreferences = FajrPreferences(context)
        val calculationMethodFromId = CalculationMethod.fromId(fajrPreferences.calculationMethodId)
        val calendar = Calendar.getInstance(); calendar.timeInMillis = j
        val calendar2 = calendar.clone() as Calendar; calendar2.add(Calendar.DAY_OF_YEAR, -1)
        val prayerAdjustments = PrayerAdjustments
        val dayPrayerTimesCalculateAll = prayerAdjustments.calculateAll(context, calendar2, fajrPreferences.latitude, fajrPreferences.longitude, calculationMethodFromId)
        val w2 = W("العشاء", dayPrayerTimesCalculateAll.isha, iqamaOf("العشاء", dayPrayerTimesCalculateAll.isha))
        val calendar3 = calendar.clone() as Calendar; calendar3.add(Calendar.DAY_OF_YEAR, 1)
        val dayPrayerTimesCalculateAll2 = prayerAdjustments.calculateAll(context, calendar3, fajrPreferences.latitude, fajrPreferences.longitude, calculationMethodFromId)
        val listT0 = sortedEvents(w2, arrayList, W("الفجر", dayPrayerTimesCalculateAll2.fajr, iqamaOf("الفجر", dayPrayerTimesCalculateAll2.fajr)), Comparator { a, b -> a.adhan.compareTo(b.adhan) })
        var w3: W? = listT0.lastOrNull { it.adhan <= j }
        var w: W? = null
        if (w3 != null) {
            if (j <= w3.iqama) {
                w = listT0.firstOrNull { it.adhan > w3.adhan }
            } else {
                w3 = listT0.firstOrNull { it.adhan > j }
                if (w3 != null) {
                    val w3f = w3
                    w = listT0.firstOrNull { it.adhan > w3f.adhan }
                }
            }
            if (w3 == null) return null
            var iqama = w3.iqama - w3.adhan
            if (iqama < 1) iqama = 1
            return IqamaState(w3.name, w3.adhan, w3.iqama, (((j - w3.adhan) * 100) / iqama).toInt().coerceIn(0, 100), w?.iqama ?: w3.iqama)
        }
        return null
    }

    private fun iqamaOf(str: String, j: Long): Long = ((iqamaOffsets[str] ?: 15) * 60000) + j

    private fun computeState(context: Context): PrayerState {
        val fajrPreferences = FajrPreferences(context)
        val calculationMethodFromId = CalculationMethod.fromId(fajrPreferences.calculationMethodId)
        val jCurrentTimeMillis = System.currentTimeMillis()
        val calendar = Calendar.getInstance(); calendar.timeInMillis = jCurrentTimeMillis
        val dayPrayerTimesCalculateAll = PrayerAdjustments.calculateAll(context, calendar, fajrPreferences.latitude, fajrPreferences.longitude, calculationMethodFromId)
        val listAsList = listOf(dayPrayerTimesCalculateAll.fajr, dayPrayerTimesCalculateAll.sunrise, dayPrayerTimesCalculateAll.dhuhr, dayPrayerTimesCalculateAll.asr, dayPrayerTimesCalculateAll.maghrib, dayPrayerTimesCalculateAll.isha)
        L10n.init(context)
        val str = L10n.lang.code
        val list = mutableListOf(L10n.t(S.PrayerFajr), L10n.t(S.PrayerShuruq), L10n.t(S.PrayerDhuhr), L10n.t(S.PrayerAsr), L10n.t(S.PrayerMaghrib), L10n.t(S.PrayerIsha))
        if (calendar.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) list[2] = L10n.t(S.PrayerJumuah)
        var i4 = listAsList.indexOfFirst { it > jCurrentTimeMillis }
        val j: Long; val str5: String; val i2: Int
        if (i4 == -1) {
            val calendar2 = calendar.clone() as Calendar; calendar2.add(Calendar.DAY_OF_YEAR, 1)
            j = PrayerAdjustments.calculateAll(context, calendar2, fajrPreferences.latitude, fajrPreferences.longitude, calculationMethodFromId).fajr
            str5 = list[0]; i2 = 0
        } else {
            j = listAsList[i4]; str5 = list[i4]; i2 = i4
        }
        val iNextIndex = listAsList.indexOfLast { it <= jCurrentTimeMillis }
        val jLongValue2: Long; val str3: String
        if (iNextIndex == -1) {
            val calendar3 = calendar.clone() as Calendar; calendar3.add(Calendar.DAY_OF_YEAR, -1)
            jLongValue2 = PrayerAdjustments.calculateAll(context, calendar3, fajrPreferences.latitude, fajrPreferences.longitude, calculationMethodFromId).isha
            str3 = list[5]
        } else {
            jLongValue2 = listAsList[iNextIndex]; str3 = list[iNextIndex]
        }
        return PrayerState(list, listAsList, i2, str5, j, str3, jLongValue2, fajrPreferences.getCityNameLocalized(str))
    }

    private fun faceRemaining(str: String, str2: String, str3: String): String {
        if (str3.startsWith("ar")) return "باقي على صلاة $str $str2"
        if (str3.startsWith("ur")) return "باقی $str $str2"
        if (str3.startsWith("fr")) return "$str dans $str2"
        if (str3.startsWith("de")) return "$str in $str2"
        if (str3.startsWith("es")) return "$str en $str2"
        if (str3.startsWith("it")) return "$str tra $str2"
        if (!str3.startsWith("tr")) {
            return if (str3.startsWith("id")) "$str dalam $str2" else if (str3.startsWith("ru")) "$str через $str2" else if (str3.startsWith("pt")) "$str em $str2" else "$str in $str2"
        }
        return "$str: $str2 kaldı"
    }

    private fun formatRemaining(j: Long): String {
        var i = (j / 60000).toInt(); if (i < 0) i = 0
        val i2 = i / 60; val i3 = i % 60
        if (i2 <= 0) return minutesText(i)
        var strL = "ساعتان"
        if (i3 == 0) {
            return if (i2 != 1) (if (i2 != 2) (if (i2 < 11) "$i2 ساعات" else "$i2 ساعة") else "ساعتان") else "ساعة واحدة"
        }
        if (i2 == 1) strL = "ساعة"
        else if (i2 != 2) strL = if (i2 < 11) "$i2 ساعات" else "$i2 ساعة"
        return strL + " و " + minutesText(i3)
    }

    private fun minutesText(i: Int): String {
        if (i == 1) return "دقيقة واحدة"
        if (i == 2) return "دقيقتان"
        return i.toString() + (if (3 > i || i >= 11) " دقيقة" else " دقائق")
    }

    private fun launchIntent(context: Context): PendingIntent {
        val intent = Intent(context, MainActivity::class.java)
        intent.flags = 335544320
        return PendingIntent.getActivity(context, 0, intent, 201326592)
    }

    private fun buildFace(context: Context, prayerState: PrayerState, str: String, i: Int, i2: Int): RemoteViews {
        val remoteViews = RemoteViews(context.packageName, i2)
        remoteViews.setTextViewText(R.id.facewidget_prayer_name, prayerState.nextName)
        remoteViews.setTextViewText(R.id.facewidget_prayer_time, formatTime(prayerState.nextMillis))
        remoteViews.setTextViewText(R.id.facewidget_remaining, str)
        remoteViews.setProgressBar(R.id.facewidget_progress, 100, i, false)
        remoteViews.setOnClickPendingIntent(R.id.facewidget_root, launchIntent(context))
        return remoteViews
    }

    private fun updateClockCountdown(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState, j: Long) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_clock)
        remoteViews.setTextViewText(R.id.widget_clock_prev_name, prayerState.prevName)
        remoteViews.setTextViewText(R.id.widget_clock_prev_time, formatTimeShort(prayerState.prevMillis))
        remoteViews.setTextViewText(R.id.widget_clock_next_name, prayerState.nextName)
        remoteViews.setTextViewText(R.id.widget_clock_next_time, formatTimeShort(prayerState.nextMillis))
        if (Build.VERSION.SDK_INT >= 24) remoteViews.setChronometerCountDown(R.id.widget_clock_countdown, true)
        remoteViews.setChronometer(R.id.widget_clock_countdown, SystemClock.elapsedRealtime() + (prayerState.nextMillis - j), "%s", true)
        remoteViews.setOnClickPendingIntent(R.id.widget_clock_countdown, launchIntent(context))
        applyToAll(context, appWidgetManager, ClockWidgetProvider::class.java, remoteViews)
    }

    private fun updateDayPrayer(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState, calendar: Calendar) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_day_prayer)
        remoteViews.setTextViewText(R.id.widget_day_name, SimpleDateFormat("EEEE", Locale("ar", "EG")).format(calendar.time))
        remoteViews.setTextViewText(R.id.widget_day_prayer, prayerState.nextName + " " + formatTime(prayerState.nextMillis))
        remoteViews.setOnClickPendingIntent(R.id.widget_day_name, launchIntent(context))
        applyToAll(context, appWidgetManager, DayPrayerWidgetProvider::class.java, remoteViews)
    }

    private fun updateGregorian(context: Context, appWidgetManager: AppWidgetManager, calendar: Calendar) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_gregorian)
        remoteViews.setTextViewText(R.id.widget_greg_day, calendar.get(Calendar.DAY_OF_MONTH).toString())
        remoteViews.setTextViewText(R.id.widget_greg_month, SimpleDateFormat("MMMM", Locale("ar", "EG")).format(calendar.time))
        remoteViews.setTextViewText(R.id.widget_greg_year, calendar.get(Calendar.YEAR).toString())
        remoteViews.setOnClickPendingIntent(R.id.widget_greg_day, launchIntent(context))
        applyToAll(context, appWidgetManager, GregorianWidgetProvider::class.java, remoteViews)
    }

    private fun updateHero(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState, j: Long) {
        var nextMillis = prayerState.nextMillis - prayerState.prevMillis
        if (nextMillis < 1) nextMillis = 1
        val iR = (((j - prayerState.prevMillis) * 100) / nextMillis).toInt().coerceIn(0, 100)
        val calendar = Calendar.getInstance(); calendar.timeInMillis = j
        var remoteViews = RemoteViews(context.packageName, barLayout(context, R.layout.widget_hero))
        remoteViews.setTextViewText(R.id.widget_hero_prev_name, prayerState.prevName)
        remoteViews.setTextViewText(R.id.widget_hero_next_name, prayerState.nextName)
        remoteViews.setTextViewText(R.id.widget_hero_prev_time, formatTimeShort(prayerState.prevMillis))
        remoteViews.setTextViewText(R.id.widget_hero_next_time, formatTimeShort(prayerState.nextMillis))
        remoteViews.setTextViewText(R.id.widget_hero_remaining, faceRemaining(prayerState.nextName, compactRemaining(prayerState.nextMillis - j), L10n.lang.code))
        remoteViews.setProgressBar(R.id.widget_hero_progress, 100, iR, false)
        var strA = HijriDate.format(calendar)
        if (strA.endsWith(" هـ")) strA = strA.substring(0, strA.length - 3)
        remoteViews.setTextViewText(R.id.widget_hero_hijri, strA)
        remoteViews.setTextViewText(R.id.widget_hero_greg, SimpleDateFormat("d MMMM yyyy", Locale("ar", "EG")).format(calendar.time))
        remoteViews.setOnClickPendingIntent(R.id.widget_hero_root, launchIntent(context))
        val remoteViews2 = RemoteViews(context.packageName, barLayout(context, R.layout.widget_hero_compact))
        remoteViews2.setTextViewText(R.id.widget_hero_compact_name, prayerState.nextName + " " + formatTime(prayerState.nextMillis))
        remoteViews2.setTextViewText(R.id.widget_hero_compact_remaining, "باقي " + compactRemaining(prayerState.nextMillis - j))
        remoteViews2.setProgressBar(R.id.widget_hero_compact_progress, 100, iR, false)
        remoteViews2.setOnClickPendingIntent(R.id.widget_hero_compact_root, launchIntent(context))
        if (Build.VERSION.SDK_INT >= 31) {
            remoteViews = RemoteViews(mapOf(SizeF(250.0f, 110.0f) to remoteViews, SizeF(250.0f, 40.0f) to remoteViews2))
        }
        applyToAll(context, appWidgetManager, HeroWidgetProvider::class.java, remoteViews)
    }

    private fun updateHijri(context: Context, appWidgetManager: AppWidgetManager, calendar: Calendar) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_hijri)
        val (year, month, day) = HijriDate.hijriYmd(calendar)
        remoteViews.setTextViewText(R.id.widget_hijri_day, day.toString())
        remoteViews.setTextViewText(R.id.widget_hijri_month, HijriDate.monthName(month))
        remoteViews.setTextViewText(R.id.widget_hijri_year, "$year هـ")
        remoteViews.setOnClickPendingIntent(R.id.widget_hijri_day, launchIntent(context))
        applyToAll(context, appWidgetManager, HijriWidgetProvider::class.java, remoteViews)
    }

    private fun updateIqama(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState, j: Long) {
        val iqamaState = computeIqamaState(context, prayerState, j) ?: return
        val remoteViews = RemoteViews(context.packageName, barLayout(context, R.layout.widget_iqama))
        remoteViews.setTextViewText(R.id.widget_iqama_countdown, HomeWidgets.mmss(iqamaState.iqamaMillis - j))
        remoteViews.setTextViewText(R.id.widget_iqama_adhan, iqamaState.prayerName + " " + formatTimeShort(iqamaState.adhanMillis))
        remoteViews.setTextViewText(R.id.widget_iqama_time, "الإقامة " + formatTimeShort(iqamaState.iqamaMillis))
        remoteViews.setTextViewText(R.id.widget_iqama_next, "الإقامة " + formatTimeShort(iqamaState.nextIqamaMillis))
        remoteViews.setProgressBar(R.id.widget_iqama_progress, 100, iqamaState.progressPct, false)
        remoteViews.setOnClickPendingIntent(R.id.widget_iqama_countdown, launchIntent(context))
        applyToAll(context, appWidgetManager, IqamaWidgetProvider::class.java, remoteViews)
    }

    private fun updateIqamaSmall(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState, j: Long) {
        val iqamaState = computeIqamaState(context, prayerState, j) ?: return
        val remoteViews = RemoteViews(context.packageName, barLayout(context, R.layout.widget_iqama_small))
        remoteViews.setTextViewText(R.id.widget_iqama_small_time, formatTimeShort(iqamaState.iqamaMillis))
        remoteViews.setProgressBar(R.id.widget_iqama_small_progress, 100, iqamaState.progressPct, false)
        remoteViews.setOnClickPendingIntent(R.id.widget_iqama_small_time, launchIntent(context))
        applyToAll(context, appWidgetManager, IqamaSmallWidgetProvider::class.java, remoteViews)
    }

    private fun updateNextPrayer(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState, j: Long) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_next_prayer)
        remoteViews.setTextViewText(R.id.widget_prayer_name, prayerState.nextName)
        remoteViews.setTextViewText(R.id.widget_prayer_time, formatTime(prayerState.nextMillis))
        if (Build.VERSION.SDK_INT >= 24) remoteViews.setChronometerCountDown(R.id.widget_countdown, true)
        remoteViews.setChronometer(R.id.widget_countdown, SystemClock.elapsedRealtime() + (prayerState.nextMillis - j), "باقي %s", true)
        remoteViews.setOnClickPendingIntent(R.id.widget_prayer_name, launchIntent(context))
        remoteViews.setOnClickPendingIntent(R.id.widget_prayer_time, launchIntent(context))
        applyToAll(context, appWidgetManager, NextPrayerWidgetProvider::class.java, remoteViews)
    }

    private fun updatePill(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState, j: Long) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_pill)
        remoteViews.setTextViewText(R.id.widget_pill_text, "أذان " + prayerState.nextName + " بعد " + formatRemaining(prayerState.nextMillis - j))
        remoteViews.setOnClickPendingIntent(R.id.widget_pill_text, launchIntent(context))
        applyToAll(context, appWidgetManager, PillWidgetProvider::class.java, remoteViews)
    }

    private fun updatePrayerBar(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState, j: Long) {
        val remoteViews = RemoteViews(context.packageName, barLayout(context, R.layout.widget_prayer_bar))
        remoteViews.setTextViewText(R.id.widget_bar_name, prayerState.nextName)
        remoteViews.setTextViewText(R.id.widget_bar_time, formatTime(prayerState.nextMillis))
        var nextMillis = prayerState.nextMillis - prayerState.prevMillis
        if (nextMillis < 1) nextMillis = 1
        remoteViews.setProgressBar(R.id.widget_bar_progress, 100, (((j - prayerState.prevMillis) * 100) / nextMillis).toInt().coerceIn(0, 100), false)
        remoteViews.setOnClickPendingIntent(R.id.widget_bar_name, launchIntent(context))
        remoteViews.setOnClickPendingIntent(R.id.widget_bar_time, launchIntent(context))
        applyToAll(context, appWidgetManager, PrayerBarWidgetProvider::class.java, remoteViews)
    }

    private fun updateSchedule(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState) {
        val remoteViews = RemoteViews(context.packageName, R.layout.widget_schedule)
        val listAsList = listOf(R.id.widget_name_0, R.id.widget_name_1, R.id.widget_name_2, R.id.widget_name_3, R.id.widget_name_4, R.id.widget_name_5)
        val listAsList2 = listOf(R.id.widget_time_0, R.id.widget_time_1, R.id.widget_time_2, R.id.widget_time_3, R.id.widget_time_4, R.id.widget_time_5)
        for (i in 0 until 6) {
            remoteViews.setTextViewText(listAsList[i], prayerState.names[i])
            remoteViews.setTextViewText(listAsList2[i], formatTimeShort(prayerState.times[i]))
            val i3 = if (i == prayerState.nextIndex) -12676609 else -1
            val i2 = if (i == prayerState.nextIndex) -12676609 else -5855578
            remoteViews.setTextColor(listAsList2[i], i3)
            remoteViews.setTextColor(listAsList[i], i2)
        }
        remoteViews.setOnClickPendingIntent(R.id.widget_name_0, launchIntent(context))
        applyToAll(context, appWidgetManager, ScheduleWidgetProvider::class.java, remoteViews)
    }

    private fun updateWideBar(context: Context, appWidgetManager: AppWidgetManager, prayerState: PrayerState, calendar: Calendar, j: Long) {
        val remoteViews = RemoteViews(context.packageName, barLayout(context, R.layout.widget_wide_bar))
        remoteViews.setTextViewText(R.id.widget_wide_day, SimpleDateFormat("EEEE", Locale("ar", "EG")).format(calendar.time))
        remoteViews.setTextViewText(R.id.widget_wide_city, prayerState.city)
        remoteViews.setTextViewText(R.id.widget_wide_next_name, prayerState.nextName)
        remoteViews.setTextViewText(R.id.widget_wide_prev_name, prayerState.prevName)
        remoteViews.setTextViewText(R.id.widget_wide_next_time, formatTimeShort(prayerState.nextMillis))
        remoteViews.setTextViewText(R.id.widget_wide_prev_time, formatTimeShort(prayerState.prevMillis))
        remoteViews.setTextViewText(R.id.widget_wide_remaining, "باقي على " + prayerState.nextName + " " + formatRemaining(prayerState.nextMillis - j))
        var nextMillis = prayerState.nextMillis - prayerState.prevMillis
        if (nextMillis < 1) nextMillis = 1
        remoteViews.setProgressBar(R.id.widget_wide_progress, 100, (((j - prayerState.prevMillis) * 100) / nextMillis).toInt().coerceIn(0, 100), false)
        var strA = HijriDate.format(calendar)
        if (strA.endsWith(" هـ")) strA = strA.substring(0, strA.length - 3)
        remoteViews.setTextViewText(R.id.widget_wide_hijri, strA)
        remoteViews.setTextViewText(R.id.widget_wide_greg, SimpleDateFormat("d MMMM yyyy", Locale("ar", "EG")).format(calendar.time))
        remoteViews.setOnClickPendingIntent(R.id.widget_wide_remaining, launchIntent(context))
        applyToAll(context, appWidgetManager, WideBarWidgetProvider::class.java, remoteViews)
    }

    fun pushFaceWidget(context: Context, str: String) {
        if (str == FaceWidgetReceiver.PAGE_ID_PRAYER) {
            val prayerState = computeState(context)
            val jCurrentTimeMillis = System.currentTimeMillis()
            var nextMillis = prayerState.nextMillis - prayerState.prevMillis
            if (nextMillis < 1) nextMillis = 1
            val iR = (((jCurrentTimeMillis - prayerState.prevMillis) * 100) / nextMillis).toInt().coerceIn(0, 100)
            val strFaceRemaining = faceRemaining(prayerState.nextName, compactRemaining(prayerState.nextMillis - jCurrentTimeMillis), L10n.lang.code)
            val intent = Intent(FaceWidgetReceiver.ACTION_RESPONSE)
            intent.setPackage("com.android.systemui")
            intent.putExtra("package", context.packageName)
            intent.putExtra("pageId", str)
            intent.putExtra("show", true)
            intent.putExtra("origin", buildFace(context, prayerState, strFaceRemaining, iR, R.layout.facewidget_prayer))
            intent.putExtra("aod", buildFace(context, prayerState, strFaceRemaining, iR, R.layout.facewidget_prayer_aod))
            context.sendBroadcast(intent)
        }
    }

    private fun compactRemaining(j: Long): String = compactRemaining(j, L10n.lang.code)
}
