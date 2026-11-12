package com.example.fajr.calculation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone
import kotlin.math.cos
import kotlin.math.sin

/**
 * Pins the recovered calculator's exact millisecond outputs (generated from the
 * decompiled implementation that shipped up to 3.4.2) so the Kotlin rewrite cannot
 * drift, and covers the high-latitude fallback: when the fajr/isha angle never
 * happens, rule "none" now behaves like "middle of the night" instead of returning
 * the old fixed 22.5 degree hour angle (which put fajr ~1.5h before solar noon).
 */
class PrayerTimesCalculatorTest {

    private fun invalidBelowHorizon(angle: Double, latitude: Double, declination: Double): Boolean {
        val s = (-sin(Math.toRadians(angle)) - sin(Math.toRadians(declination)) * sin(Math.toRadians(latitude))) /
                (cos(Math.toRadians(declination)) * cos(Math.toRadians(latitude)))
        return s < -1.0 || s > 1.0
    }

    private fun declination(calendar: Calendar): Double {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val d = (367.0 * year - Math.floor((Math.floor((month + 9.0) / 12.0) + year) * 7.0 / 4.0) +
                Math.floor(275.0 * month / 9.0) + day) - 730531.5
        val meanAnomaly = (0.98560028 * d + 357.529) % 360.0
        val meanLongitude = (0.98564736 * d + 280.459) % 360.0
        val eclipticLongitude = (meanLongitude + 1.915 * sin(Math.toRadians(meanAnomaly)) +
                0.02 * sin(Math.toRadians(meanAnomaly * 2.0))) % 360.0
        val obliquity = 23.439 - d * 3.6E-7
        return Math.toDegrees(kotlin.math.asin(sin(Math.toRadians(eclipticLongitude)) * sin(Math.toRadians(obliquity))))
    }

    private fun calendarAt(tz: TimeZone, y: Int, m: Int, d: Int): Calendar {
        val c = Calendar.getInstance(tz)
        c.clear()
        c.set(y, m - 1, d, 12, 0, 0)
        return c
    }

    @Test
    fun regressionMatchesRecoveredImplementation() {
        val lines = javaClass.classLoader!!.getResourceAsStream("prayer-times-pins.txt")!!
            .bufferedReader().readLines().filter { it.isNotBlank() }
        var checked = 0
        var skippedInvalid = 0
        for (line in lines) {
            val (meta, expected) = line.split(" -> ")
            val p = meta.split("|")
            val tz = TimeZone.getTimeZone(p[0])
            val method = CalculationMethod.valueOf(p[1])
            val lat = p[2].toDouble()
            val lon = p[3].toDouble()
            val asrFactor = p[4].toInt()
            val rule = p[5].toInt()
            val adj = if (p[6] == "null") null
                else p[6].removeSurrounding("[", "]").split(",").map { it.trim().toInt() }.toIntArray()
            val (y, mo, d) = p[7].split("-").map { it.toInt() }
            TimeZone.setDefault(tz)
            val cal = calendarAt(tz, y, mo, d)

            // Lines where the old code hit its broken fixed-angle fallback cannot be pinned
            // with rule 0: the fix deliberately changes exactly those outputs.
            if (rule == 0) {
                val decl = declination(cal)
                if (invalidBelowHorizon(method.fajrAngle, lat, decl) ||
                    invalidBelowHorizon(method.ishaAngle ?: 17.0, lat, decl)) {
                    skippedInvalid++
                    continue
                }
            }

            val t = PrayerTimesCalculator.calculateAll(cal, lat, lon, method, asrFactor, rule, adj)
            val actual = listOf(t.fajr, t.sunrise, t.dhuhr, t.asr, t.maghrib, t.isha).joinToString(",")
            assertEquals("mismatch on $line", expected, actual)
            checked++
        }
        assertTrue("expected a useful number of pinned cases", checked >= 60)
        assertTrue("expected some invalid high-lat cases in the pins", skippedInvalid >= 3)
    }

    @Test
    fun highLatitudeRuleNoneActsLikeMiddleOfNight() {
        val tz = TimeZone.getTimeZone("Europe/Oslo")
        TimeZone.setDefault(tz)
        // Tromso, 21 June: the sun never reaches the fajr/isha angle.
        val cal = calendarAt(tz, 2026, 6, 21)
        val auto = PrayerTimesCalculator.calculateAll(cal, 69.65, 18.96, CalculationMethod.EGYPTIAN, 1, 0, null)
        val middle = PrayerTimesCalculator.calculateAll(cal, 69.65, 18.96, CalculationMethod.EGYPTIAN, 1, 1, null)
        assertEquals(middle.fajr, auto.fajr)
        assertEquals(middle.isha, auto.isha)
        // Fajr/isha stay on their own sides of the day instead of the old inverted output.
        // (Sunrise/maghrib themselves are degenerate at midnight-sun latitudes because the
        // sun never sets there; that fallback predates this fix and is unchanged.)
        assertTrue(auto.fajr < auto.sunrise)
        assertTrue(auto.maghrib < auto.isha)
    }

    @Test
    fun highLatitudeExplicitRulesStillWin() {
        val tz = TimeZone.getTimeZone("Europe/Oslo")
        TimeZone.setDefault(tz)
        val cal = calendarAt(tz, 2026, 6, 21)
        val middle = PrayerTimesCalculator.calculateAll(cal, 69.65, 18.96, CalculationMethod.EGYPTIAN, 1, 1, null)
        val seventh = PrayerTimesCalculator.calculateAll(cal, 69.65, 18.96, CalculationMethod.EGYPTIAN, 1, 2, null)
        // Seventh-of-night gives fajr later than middle-of-night and isha earlier.
        assertTrue(seventh.fajr > middle.fajr)
        assertTrue(seventh.isha < middle.isha)
    }

    @Test
    fun cairoMatchesPublishedTimetableWithinTwoMinutes() {
        // Egyptian General Authority of Survey, Cairo 2026-01-15 (times within +/-2 min
        // are accepted because official tables round seconds differently).
        val tz = TimeZone.getTimeZone("Africa/Cairo")
        TimeZone.setDefault(tz)
        val cal = calendarAt(tz, 2026, 1, 15)
        val t = PrayerTimesCalculator.calculateAll(cal, 30.0444, 31.2357, CalculationMethod.EGYPTIAN, 1, 0, null)
        fun hm(ms: Long): Pair<Int, Int> {
            val c = Calendar.getInstance(tz).apply { timeInMillis = ms }
            return c.get(Calendar.HOUR_OF_DAY) to c.get(Calendar.MINUTE)
        }
        fun close(ms: Long, h: Int, m: Int, label: String) {
            val (ah, am) = hm(ms)
            val diff = Math.abs((ah * 60 + am) - (h * 60 + m))
            assertTrue("$label off by ${diff}min: ${ah}:${am} vs $h:$m", diff <= 2)
        }
        close(t.fajr, 5, 21, "fajr")
        close(t.sunrise, 6, 51, "sunrise")
        close(t.dhuhr, 12, 4, "dhuhr")
        close(t.asr, 14, 57, "asr")
        close(t.maghrib, 17, 17, "maghrib")
        close(t.isha, 18, 38, "isha")
    }
}
