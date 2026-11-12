package com.example.prayer

import android.icu.util.IslamicCalendar
import java.util.Calendar
import java.util.Date

/** Hijri (Umm al-Qura) date helpers. */
object HijriDate {
    private val monthNames = listOf("محرم", "صفر", "ربيع الأول", "ربيع الثاني", "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان", "رمضان", "شوال", "ذو القعدة", "ذو الحجة")
    private val monthNamesEn = listOf("Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani", "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban", "Ramadan", "Shawwal", "Dhul-Qa'dah", "Dhul-Hijjah")

    fun hijriYmd(cal: Calendar): Triple<Int, Int, Int> {
        val ic = IslamicCalendar()
        ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA)
        ic.setTime(Date(cal.timeInMillis))
        return Triple(ic.get(Calendar.YEAR), ic.get(Calendar.MONTH) + 1, ic.get(Calendar.DAY_OF_MONTH))
    }

    fun monthName(month: Int): String = monthNames[(month - 1).coerceIn(0, 11)]

    fun monthName(month: Int, lang: String): String =
        if (lang == "ar") monthName(month) else monthNamesEn[(month - 1).coerceIn(0, 11)]

    fun format(cal: Calendar, lang: String): String {
        val (y, m, d) = hijriYmd(cal)
        return "$d ${monthName(m, lang)} $y" + (if (lang == "ar") " هـ" else " AH")
    }

    fun format(cal: Calendar): String {
        val (y, m, d) = hijriYmd(cal)
        return "$d ${monthName(m)} $y هـ"
    }
}
