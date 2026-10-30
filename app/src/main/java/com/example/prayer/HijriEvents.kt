package com.example.prayer

import android.app.AlarmManager
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import androidx.core.app.NotificationCompat
import com.example.fajr.calculation.PrayerAdjustments
import java.util.Calendar

/** Fixed-date Islamic occasions on the Hijri (Umm al-Qura, via ICU IslamicCalendar)
 *  calendar, honoring the user's day adjustment (PrayerAdjustments.hijriOffsetDays,
 *  same as the prayer screen header). Dates are the widely-observed fixed dates;
 *  actual observance can differ by ±1 day with moon sighting.
 *  Reminders: ordinary notifications at 09:00 local on the occasion day, off by
 *  default, scheduled with AlarmManager exact alarms (notification only). */
object HijriEvents {
    enum class Category { RELIGIOUS, EID, FASTING, RAMADAN }

    data class Occasion(
        val id: String,
        val nameAr: String,
        val nameEn: String,
        val hijriMonth: Int, // 1..12
        val hijriDay: Int,   // 1..30
        val category: Category
    )

    val FIXED = listOf(
        Occasion("new_year", "رأس السنة الهجرية", "Islamic New Year", 1, 1, Category.RELIGIOUS),
        Occasion("ashura", "يوم عاشوراء", "Day of Ashura", 1, 10, Category.FASTING),
        Occasion("mawlid", "المولد النبوي الشريف", "Prophet's Birthday (Mawlid)", 3, 12, Category.RELIGIOUS),
        Occasion("isra_miraj", "الإسراء والمعراج", "Isra and Mi'raj", 7, 27, Category.RELIGIOUS),
        Occasion("mid_shaban", "ليلة النصف من شعبان", "Mid-Sha'ban Night", 8, 15, Category.RELIGIOUS),
        Occasion("ramadan_start", "بداية شهر رمضان", "Start of Ramadan", 9, 1, Category.RAMADAN),
        Occasion("laylat_alqadr", "ليلة القدر (المرجّحة)", "Laylat al-Qadr (most likely)", 9, 27, Category.RAMADAN),
        Occasion("eid_fitr", "عيد الفطر المبارك", "Eid al-Fitr", 10, 1, Category.EID),
        Occasion("arafah", "يوم عرفة", "Day of Arafah", 12, 9, Category.FASTING),
        Occasion("eid_adha", "عيد الأضحى المبارك", "Eid al-Adha", 12, 10, Category.EID)
    )

    private fun whiteDays(month: Int): List<Occasion> = listOf(
        Occasion("white_${month}_13", "الأيام البيض (13)", "White Days (13)", month, 13, Category.FASTING),
        Occasion("white_${month}_14", "الأيام البيض (14)", "White Days (14)", month, 14, Category.FASTING),
        Occasion("white_${month}_15", "الأيام البيض (15)", "White Days (15)", month, 15, Category.FASTING)
    )

    fun name(o: Occasion, lang: String): String = if (lang == "ar") o.nameAr else o.nameEn

    /** Occasions for a Gregorian calendar day, after applying the user's Hijri offset. */
    fun forDay(context: Context, cal: Calendar): List<Occasion> {
        val off = PrayerAdjustments.hijriOffsetDays(context)
        val hcal = if (off != 0) (cal.clone() as Calendar).apply { add(Calendar.DATE, off) } else cal
        val h = HijriDate.INSTANCE.hijriYmd(hcal)
        val hm = h.component2()
        val hd = h.component3()
        val out = FIXED.filter { it.hijriMonth == hm && it.hijriDay == hd }.toMutableList()
        if (hd in 13..15) out.addAll(whiteDays(hm).filter { it.hijriDay == hd })
        return out
    }

    /** Reminder preference (off by default), keyed by occasion id. */
    private const val PREFS = "fajr_prefs"
    private const val KEY_ENABLED = "event_reminder_ids"

    fun isReminderOn(context: Context, id: String): Boolean =
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            .getStringSet(KEY_ENABLED, emptySet())?.contains(id) == true

    fun setReminder(context: Context, id: String, on: Boolean) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val cur = sp.getStringSet(KEY_ENABLED, emptySet())?.toMutableSet() ?: mutableSetOf()
        if (on) cur.add(id) else cur.remove(id)
        sp.edit().putStringSet(KEY_ENABLED, cur).apply()
        scheduleAll(context)
    }

    /** Schedules the next upcoming notification for every enabled occasion.
     *  Call on toggle and when the calendar screen opens (no boot hook yet). */
    fun scheduleAll(context: Context) {
        val sp = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val enabled = sp.getStringSet(KEY_ENABLED, emptySet()) ?: emptySet()
        val am = context.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        // cancel anything we scheduled before (request code = stable hash)
        val now = System.currentTimeMillis()
        val scan = Calendar.getInstance()
        val found = mutableMapOf<String, Long>()
        // find next occurrence of each enabled occasion within ~400 days
        for (i in 0..400) {
            val day = (scan.clone() as Calendar).apply { add(Calendar.DATE, i) }
            for (o in forDay(context, day)) {
                if (o.id in enabled && !found.containsKey(o.id)) {
                    val t = (day.clone() as Calendar).apply {
                        set(Calendar.HOUR_OF_DAY, 9); set(Calendar.MINUTE, 0)
                        set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
                    }.timeInMillis
                    if (t > now) found[o.id] = t
                }
            }
        }
        for (o in FIXED) {
            val pi = pending(context, o, null)
            am.cancel(pi)
            val t = found[o.id]
            if (t != null) {
                try {
                    am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pending(context, o, name(o, "ar")))
                } catch (_: SecurityException) {
                    am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, t, pending(context, o, name(o, "ar")))
                }
            }
        }
    }

    private fun pending(context: Context, o: Occasion, title: String?): PendingIntent {
        val i = Intent(context, EventReminderReceiver::class.java)
            .putExtra("event_name", title ?: o.nameAr)
        return PendingIntent.getBroadcast(
            context, 7000 + Math.abs(o.id.hashCode() % 500), i,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
    }
}

class EventReminderReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent) {
        val name = intent.getStringExtra("event_name") ?: return
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val chId = com.example.utils.NotifPrefs.configureChannel(context, nm, NotificationChannel("event_reminders_channel", "مناسبات هجرية", NotificationManager.IMPORTANCE_DEFAULT))
        nm.notify(
            (System.currentTimeMillis() % 100000).toInt(),
            NotificationCompat.Builder(context, chId)
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentTitle(context.getString(context.applicationInfo.labelRes))
                .setContentText(name)
                .setAutoCancel(true)
                .build()
        )
        HijriEvents.scheduleAll(context) // roll forward
    }
}
