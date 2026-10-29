package com.wathakker.wear

import android.content.Context
import com.example.fajr.calculation.CalculationMethod
import com.example.fajr.calculation.PrayerTimesCalculator
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import java.util.TimeZone
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.min
import kotlin.math.sin

/** One prayer time. [id] is 0 Fajr, 1 Sunrise, 2 Dhuhr, 3 Asr, 4 Maghrib, 5 Isha. */
data class Slot(val id: Int, val name: String, val at: Long)

class PrayerState(
    val now: Long,
    val today: List<Slot>,      // six slots for today
    val prev: Slot,             // last time that already passed
    val next: Slot,             // next time (may be tomorrow's Fajr)
    val nextFajrTomorrow: Slot
) {
    val progress: Float get() = max(0.02f, min(1f, (now - prev.at).toFloat() / max(1L, next.at - prev.at).toFloat()))
}

object Prayer {
    private val arNames = listOf("الفجر", "الشروق", "الظهر", "العصر", "المغرب", "العشاء")
    private val enNames = listOf("Fajr", "Sunrise", "Dhuhr", "Asr", "Maghrib", "Isha")
    val names: List<String> get() = if (Loc.en) enNames else arNames
    // Default location until a phone sync exists: Cairo.
    const val DEFAULT_LAT = 30.0444
    const val DEFAULT_LON = 31.2357
    const val DEFAULT_CITY = "القاهرة"

    private fun coords(c: Context): Pair<Double, Double> {
        val p = c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE)
        if (p.getInt("locmode", 0) == 1 && p.contains("gps_lat")) return Pair(
            java.lang.Double.longBitsToDouble(p.getLong("gps_lat", 0L)), java.lang.Double.longBitsToDouble(p.getLong("gps_lon", 0L)))
        return Pair(
            java.lang.Double.longBitsToDouble(p.getLong("lat", java.lang.Double.doubleToLongBits(DEFAULT_LAT))),
            java.lang.Double.longBitsToDouble(p.getLong("lon", java.lang.Double.doubleToLongBits(DEFAULT_LON)))
        )
    }

    private fun day(c: Context, offsetDays: Int, now: Long): List<Slot> {
        val (lat, lon) = coords(c)
        val cal = Calendar.getInstance(TimeZone.getDefault())
        cal.timeInMillis = now
        cal.add(Calendar.DAY_OF_YEAR, offsetDays)
        val d = PrayerTimesCalculator.INSTANCE.calculateAll(cal, lat, lon, CalculationMethod.EGYPTIAN, 1, 0, intArrayOf(0, 0, 0, 0, 0, 0))
        val t = longArrayOf(d.fajr, d.sunrise, d.dhuhr, d.asr, d.maghrib, d.isha)
        val fri = cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
        return t.mapIndexed { i, v -> Slot(i, if (i == 2 && fri) (if (Loc.en) "Jumuah" else "الجمعة") else names[i], v) }
    }

    /** One prayer's iqama window: [prayer] is the adhan slot, [at] is the iqama time. */
    data class Iqama(val prayer: Slot, val at: Long)

    /** Iqama offsets in minutes after adhan, matching the phone app (WidgetUpdater.iqamaOffsets); Friday Dhuhr excluded. */
    private val iqamaMin = mapOf(0 to 25, 2 to 20, 3 to 20, 4 to 10, 5 to 20)

    /** The active iqama window at [now], if any: after an adhan and until its iqama time. */
    fun iqamaNow(c: Context, now: Long = System.currentTimeMillis()): Iqama? {
        val cal = Calendar.getInstance(TimeZone.getDefault())
        for (off in -1..0) {
            for (sl in day(c, off, now)) {
                if (sl.id == 1 || sl.at > now) continue
                val mins = iqamaMin[sl.id] ?: continue
                cal.timeInMillis = sl.at
                if (sl.id == 2 && cal.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) continue
                val iq = sl.at + mins * 60000L
                if (now <= iq) return Iqama(sl, iq)
            }
        }
        return null
    }

    /** Next of the five prayers (no sunrise) strictly after [now]. */
    fun nextAdhan(c: Context, now: Long = System.currentTimeMillis()): Slot? {
        for (off in 0..2) for (sl in day(c, off, now)) if (sl.id != 1 && sl.at > now) return sl
        return null
    }

    @Volatile var h24 = false

    fun compute(c: Context, now: Long = System.currentTimeMillis()): PrayerState {
        h24 = c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).getBoolean("h24", false)
        Loc.refresh(c)
        val y = day(c, -1, now); val t = day(c, 0, now); val n = day(c, 1, now)
        val all = y + t + n
        val next = all.first { it.at > now }
        val prev = all.last { it.at <= now }
        return PrayerState(now, t, prev, next, n[0])
    }

    private fun fmt(p: String, ms: Long): String =
        SimpleDateFormat(p, Locale.ENGLISH).format(Date(ms))

    /** "05:32 ص" */
    fun time12(ms: Long): String {
        if (h24) return fmt("HH:mm", ms)
        val cal = Calendar.getInstance(); cal.timeInMillis = ms
        val am = cal.get(Calendar.HOUR_OF_DAY) < 12
        val ap = if (Loc.en) (if (am) "AM" else "PM") else (if (am) "ص" else "م")
        return fmt("hh:mm", ms) + " " + ap
    }

    fun time24(ms: Long): String = fmt("HH:mm", ms)

    /** HH:MM remaining. */
    fun hhmm(deltaMs: Long): String {
        val m = max(0L, deltaMs / 60000)
        return String.format(Locale.ENGLISH, "%02d:%02d", m / 60, m % 60)
    }

    private fun hours(h: Long) = when {
        h == 1L -> "ساعة"; h == 2L -> "ساعتان"; h in 3..10 -> "$h ساعات"; else -> "$h ساعة"
    }
    private fun mins(m: Long) = when {
        m == 1L -> "دقيقة"; m == 2L -> "دقيقتان"; m in 3..10 -> "$m دقائق"; else -> "$m دقيقة"
    }

    fun remainingText(deltaMs: Long): String {
        val total = max(0L, deltaMs / 60000)
        val h = total / 60; val m = total % 60
        if (Loc.en) return when {
            h == 0L -> "$m min"
            m == 0L -> "$h hr"
            else -> "$h hr $m min"
        }
        return when {
            h == 0L -> mins(m)
            m == 0L -> hours(h)
            else -> hours(h) + " و " + mins(m)
        }
    }

    private val enWeekdays = listOf("Sunday", "Monday", "Tuesday", "Wednesday", "Thursday", "Friday", "Saturday")
    private val enGreg = listOf("January", "February", "March", "April", "May", "June", "July", "August", "September", "October", "November", "December")
    private val enHijri = listOf("Muharram", "Safar", "Rabi al-Awwal", "Rabi al-Thani", "Jumada al-Awwal", "Jumada al-Thani", "Rajab", "Shaaban", "Ramadan", "Shawwal", "Dhul Qadah", "Dhul Hijjah")

    private val weekdays = listOf("الأحد", "الإثنين", "الثلاثاء", "الأربعاء", "الخميس", "الجمعة", "السبت")
    private val gregMonths = listOf("يناير", "فبراير", "مارس", "أبريل", "مايو", "يونيو", "يوليو", "أغسطس", "سبتمبر", "أكتوبر", "نوفمبر", "ديسمبر")
    private val hijriMonths = listOf("محرم", "صفر", "ربيع الأول", "ربيع الآخر", "جمادى الأولى", "جمادى الآخرة", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")

    fun weekday(ms: Long): String { val c = Calendar.getInstance(); c.timeInMillis = ms; val k = c.get(Calendar.DAY_OF_WEEK) - 1; return if (Loc.en) enWeekdays[k].take(3) else weekdays[k] }
    fun gregDate(ms: Long): String { val c = Calendar.getInstance(); c.timeInMillis = ms; return "${c.get(Calendar.DAY_OF_MONTH)} ${if (Loc.en) enGreg[c.get(Calendar.MONTH)].take(3) else gregMonths[c.get(Calendar.MONTH)]}" }
    fun hijri(ms: Long): String {
        val h = java.time.chrono.HijrahDate.from(java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()))
        return "${h.get(java.time.temporal.ChronoField.DAY_OF_MONTH)} ${(if (Loc.en) enHijri else hijriMonths)[h.get(java.time.temporal.ChronoField.MONTH_OF_YEAR) - 1]}"
    }

    private fun hijriDate(ms: Long) = java.time.chrono.HijrahDate.from(java.time.Instant.ofEpochMilli(ms).atZone(java.time.ZoneId.systemDefault()))
    fun hijriDay(ms: Long): String = hijriDate(ms).get(java.time.temporal.ChronoField.DAY_OF_MONTH).toString()
    fun hijriMonth(ms: Long): String = (if (Loc.en) enHijri else hijriMonths)[hijriDate(ms).get(java.time.temporal.ChronoField.MONTH_OF_YEAR) - 1]

    /** Initial great-circle bearing to the Kaaba, degrees 0..360. */
    fun qiblaBearing(c: Context): Float {
        val (lat, lon) = coords(c)
        val kLat = Math.toRadians(21.4225); val kLon = Math.toRadians(39.8262)
        val p1 = Math.toRadians(lat); val dl = kLon - Math.toRadians(lon)
        val y = sin(dl) * cos(kLat)
        val x = cos(p1) * sin(kLat) - sin(p1) * cos(kLat) * cos(dl)
        return ((Math.toDegrees(atan2(y, x)) + 360.0) % 360.0).toFloat()
    }

    private val dirs = listOf("شمال", "شمال شرق", "شرق", "جنوب شرق", "جنوب", "جنوب غرب", "غرب", "شمال غرب")
    private val dirsEn = listOf("N", "NE", "E", "SE", "S", "SW", "W", "NW")
    fun directionName(deg: Float): String = (if (Loc.en) dirsEn else dirs)[(((deg + 22.5f) % 360f) / 45f).toInt().coerceIn(0, 7)]
}
