package com.example.fajr.calculation

import java.util.Calendar
import java.util.TimeZone
import kotlin.math.abs
import kotlin.math.asin
import kotlin.math.atan
import kotlin.math.atan2
import kotlin.math.cos
import kotlin.math.floor
import kotlin.math.sin
import kotlin.math.tan

/**
 * Prayer-time engine (solar position, hour angles, high-latitude rules).
 * Hand-written Kotlin replacement for the recovered decompiled Java; the math is
 * unchanged and pinned by PrayerTimesCalculatorTest.
 *
 * High-latitude handling: when the sun never reaches the fajr/isha angle (far north
 * in summer) the old code silently returned a fixed 22.5° hour angle, which puts fajr
 * roughly 1.5h before solar noon. Now, when the angle is impossible, the chosen
 * high-latitude rule is applied, defaulting to "middle of the night" when the user
 * picked "none" (rule 0), so times stay sane instead of collapsing to the fallback.
 */
object PrayerTimesCalculator {

    data class DayPrayerTimes(
        val fajr: Long,
        val sunrise: Long,
        val dhuhr: Long,
        val asr: Long,
        val maghrib: Long,
        val isha: Long
    )

    private data class SolarParams(val transit: Double, val declination: Double)

    /** Rule used when the sun never reaches the fajr/isha angle: the user's rule,
     *  or middle-of-the-night when unset, never the old fixed-angle fallback. */
    private fun effectiveRule(highLatRule: Int): Int = if (highLatRule > 0) highLatRule else 1

    private fun fixAngle(a: Double): Double {
        val r = a % 360.0
        return if (r < 0.0) r + 360.0 else r
    }

    private fun floorMinute(ms: Long): Long = (ms / 60000) * 60000

    private fun roundMinute(ms: Long): Long = ((ms + 30000) / 60000) * 60000

    private fun highLatNightTime(time: Double, night: Double, angle: Double, rule: Int, after: Boolean): Double {
        val portion = night * when (rule) {
            1 -> 0.5
            2 -> 0.14285714285714285
            else -> angle / 60.0
        }
        return if (after) time + portion else time - portion
    }

    private fun hourAngleAboveHorizon(altitude: Double, latitude: Double, declination: Double): Double {
        val s = (sin(Math.toRadians(altitude)) - sin(Math.toRadians(declination)) * sin(Math.toRadians(latitude))) /
                (cos(Math.toRadians(declination)) * cos(Math.toRadians(latitude)))
        if (s < -1.0 || s > 1.0) return 22.5
        return Math.toDegrees(kotlin.math.acos(s))
    }

    private fun hourAngleBelowHorizon(angle: Double, latitude: Double, declination: Double): Double {
        val s = (-sin(Math.toRadians(angle)) - sin(Math.toRadians(declination)) * sin(Math.toRadians(latitude))) /
                (cos(Math.toRadians(declination)) * cos(Math.toRadians(latitude)))
        if (s < -1.0 || s > 1.0) return 22.5
        return Math.toDegrees(kotlin.math.acos(s))
    }

    private fun hoursToMillis(calendar: Calendar, hours: Double): Long {
        val c = calendar.clone() as Calendar
        val h = floor(hours).toInt()
        val mFloat = (hours - h) * 60.0
        val m = floor(mFloat).toInt()
        val s = floor((mFloat - m) * 60.0).toInt()
        c.set(Calendar.HOUR_OF_DAY, h)
        c.set(Calendar.MINUTE, m)
        c.set(Calendar.SECOND, s)
        c.set(Calendar.MILLISECOND, 0)
        return c.timeInMillis
    }

    private fun invalidBelowHorizon(angle: Double, latitude: Double, declination: Double): Boolean {
        val s = (-sin(Math.toRadians(angle)) - sin(Math.toRadians(declination)) * sin(Math.toRadians(latitude))) /
                (cos(Math.toRadians(declination)) * cos(Math.toRadians(latitude)))
        return s < -1.0 || s > 1.0
    }

    private fun solarParams(calendar: Calendar, longitude: Double): SolarParams {
        val year = calendar.get(Calendar.YEAR)
        val month = calendar.get(Calendar.MONTH) + 1
        val day = calendar.get(Calendar.DAY_OF_MONTH)
        val d = (367.0 * year - floor((floor((month + 9.0) / 12.0) + year) * 7.0 / 4.0) +
                floor(275.0 * month / 9.0) + day) - 730531.5
        val meanAnomaly = fixAngle(0.98560028 * d + 357.529)
        val meanLongitude = fixAngle(0.98564736 * d + 280.459)
        val eclipticLongitude = fixAngle(meanLongitude + 1.915 * sin(Math.toRadians(meanAnomaly)) +
                0.02 * sin(Math.toRadians(meanAnomaly * 2.0)))
        val obliquity = 23.439 - d * 3.6E-7
        val rightAscension = fixAngle(Math.toDegrees(atan2(
            sin(Math.toRadians(eclipticLongitude)) * cos(Math.toRadians(obliquity)),
            cos(Math.toRadians(eclipticLongitude))
        ))) / 15.0
        val transit = (((TimeZone.getDefault().getOffset(calendar.timeInMillis) / 3600000.0) * 15.0 - longitude) / 15.0 +
                12.0) - (meanLongitude / 15.0 - rightAscension)
        val declination = Math.toDegrees(asin(sin(Math.toRadians(eclipticLongitude)) * sin(Math.toRadians(obliquity))))
        return SolarParams(transit, declination)
    }

    fun calculateAll(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod,
        asrFactor: Int,
        highLatRule: Int,
        adjustments: IntArray?
    ): DayPrayerTimes {
        val sp = solarParams(calendar, longitude)
        val transit = sp.transit
        val declination = sp.declination
        val sunH = hourAngleBelowHorizon(0.833, latitude, declination)
        val sunriseUtc = transit - sunH / 15.0
        val maghribUtc = transit + sunH / 15.0
        val nightHours = 24.0 - (maghribUtc - sunriseUtc)
        val asrAltitude = Math.toDegrees(atan(1.0 / (tan(Math.toRadians(abs(latitude - declination))) + asrFactor)))
        val asrH = hourAngleAboveHorizon(asrAltitude, latitude, declination)
        val maghrib = roundMinute(hoursToMillis(calendar, maghribUtc))

        val ishaAngle = method.ishaAngle ?: 17.0
        val isha: Long = when {
            method.ishaMinutesAfterMaghrib != null ->
                maghrib + method.ishaMinutesAfterMaghrib!! * 60000L
            invalidBelowHorizon(ishaAngle, latitude, declination) ->
                roundMinute(hoursToMillis(calendar, highLatNightTime(maghribUtc, nightHours, ishaAngle, effectiveRule(highLatRule), true)))
            else -> {
                val ishaH = hourAngleBelowHorizon(ishaAngle, latitude, declination)
                roundMinute(hoursToMillis(calendar, ishaH / 15.0 + transit))
            }
        }

        val fajr: Long = if (invalidBelowHorizon(method.fajrAngle, latitude, declination)) {
            roundMinute(hoursToMillis(calendar, highLatNightTime(sunriseUtc, nightHours, method.fajrAngle, effectiveRule(highLatRule), false)))
        } else {
            val fajrH = hourAngleBelowHorizon(method.fajrAngle, latitude, declination)
            roundMinute(hoursToMillis(calendar, transit - fajrH / 15.0))
        }

        val sunrise = floorMinute(hoursToMillis(calendar, sunriseUtc))
        val dhuhr = roundMinute(hoursToMillis(calendar, transit))
        val asr = roundMinute(hoursToMillis(calendar, asrH / 15.0 + transit))

        var fajrR = fajr; var sunriseR = sunrise; var dhuhrR = dhuhr
        var asrR = asr; var maghribR = maghrib; var ishaR = isha
        if (adjustments != null && adjustments.size >= 6) {
            fajrR += adjustments[0] * 60000L
            sunriseR += adjustments[1] * 60000L
            dhuhrR += adjustments[2] * 60000L
            asrR += adjustments[3] * 60000L
            maghribR += adjustments[4] * 60000L
            ishaR += adjustments[5] * 60000L
        }
        return DayPrayerTimes(fajrR, sunriseR, dhuhrR, asrR, maghribR, ishaR)
    }

    fun calculateAll(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod = CalculationMethod.EGYPTIAN
    ): DayPrayerTimes = calculateAll(calendar, latitude, longitude, method, 1, 0, null)

    fun calculateFajrTime(
        calendar: Calendar,
        latitude: Double,
        longitude: Double,
        method: CalculationMethod = CalculationMethod.EGYPTIAN
    ): Long = calculateAll(calendar, latitude, longitude, method).fajr
}
