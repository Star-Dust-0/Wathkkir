package com.example.prayer

import android.icu.util.IslamicCalendar
import java.util.Calendar
import java.util.Date

/** Umm al-Qura Hijri conversion via ICU - matches Samsung system calendar exactly. */
object HijriDate {

    private val monthNames = listOf(
        "محرم", "صفر", "ربيع الأول", "ربيع الثاني",
        "جمادى الأولى", "جمادى الثانية", "رجب", "شعبان",
        "رمضان", "شوال", "ذو القعدة", "ذو الحجة"
    )

    /** Returns (year, month 1-12, day) of the Hijri date for the given Gregorian calendar day. */
    fun hijriYmd(cal: Calendar): Triple<Int, Int, Int> {
        val ic = IslamicCalendar().apply {
            setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA)
            time = Date(cal.timeInMillis)
        }
        return Triple(
            ic.get(IslamicCalendar.YEAR),
            ic.get(IslamicCalendar.MONTH) + 1,
            ic.get(IslamicCalendar.DAY_OF_MONTH)
        )
    }

    private val monthNamesEn = listOf(
        "Muharram", "Safar", "Rabi' al-Awwal", "Rabi' al-Thani",
        "Jumada al-Ula", "Jumada al-Akhirah", "Rajab", "Sha'ban",
        "Ramadan", "Shawwal", "Dhul-Qa'dah", "Dhul-Hijjah"
    )

    fun monthName(month: Int, lang: String = "ar"): String =
        (if (lang == "ar") monthNames else monthNamesEn)[(month - 1).coerceIn(0, 11)]

    /** e.g. "29 ربيع الثاني 1448 هـ" */
    fun format(cal: Calendar, lang: String = "ar"): String {
        val (y, m, d) = hijriYmd(cal)
        return if (lang == "ar") "$d ${monthName(m)} $y هـ" else "$d ${monthName(m, lang)} $y AH"
    }
}
