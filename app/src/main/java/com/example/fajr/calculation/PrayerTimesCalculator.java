package com.example.fajr.calculation;

import java.util.Calendar;
import java.util.TimeZone;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class PrayerTimesCalculator {
    public static final int $stable = 0;
    public static final PrayerTimesCalculator INSTANCE = new PrayerTimesCalculator();

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static final class DayPrayerTimes {
        public static final int $stable = 0;
        private final long asr;
        private final long dhuhr;
        private final long fajr;
        private final long isha;
        private final long maghrib;
        private final long sunrise;

        public DayPrayerTimes(long j, long j2, long j3, long j4, long j5, long j6) {
            this.fajr = j;
            this.sunrise = j2;
            this.dhuhr = j3;
            this.asr = j4;
            this.maghrib = j5;
            this.isha = j6;
        }

        public static DayPrayerTimes copy$default(DayPrayerTimes dayPrayerTimes, long j, long j2, long j3, long j4, long j5, long j6, int i, Object obj) {
            if ((i & 1) != 0) {
                j = dayPrayerTimes.fajr;
            }
            return dayPrayerTimes.copy(j, (i & 2) != 0 ? dayPrayerTimes.sunrise : j2, (i & 4) != 0 ? dayPrayerTimes.dhuhr : j3, (i & 8) != 0 ? dayPrayerTimes.asr : j4, (i & 16) != 0 ? dayPrayerTimes.maghrib : j5, (i & 32) != 0 ? dayPrayerTimes.isha : j6);
        }

        public final DayPrayerTimes copy(long j, long j2, long j3, long j4, long j5, long j6) {
            return new DayPrayerTimes(j, j2, j3, j4, j5, j6);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof DayPrayerTimes)) {
                return false;
            }
            DayPrayerTimes dayPrayerTimes = (DayPrayerTimes) obj;
            return this.fajr == dayPrayerTimes.fajr && this.sunrise == dayPrayerTimes.sunrise && this.dhuhr == dayPrayerTimes.dhuhr && this.asr == dayPrayerTimes.asr && this.maghrib == dayPrayerTimes.maghrib && this.isha == dayPrayerTimes.isha;
        }

        public final long getAsr() {
            return this.asr;
        }

        public final long getDhuhr() {
            return this.dhuhr;
        }

        public final long getFajr() {
            return this.fajr;
        }

        public final long getIsha() {
            return this.isha;
        }

        public final long getMaghrib() {
            return this.maghrib;
        }

        public final long getSunrise() {
            return this.sunrise;
        }

        public int hashCode() {
            return Long.hashCode(this.isha) + ((Long.hashCode(this.maghrib) + ((Long.hashCode(this.asr) + ((Long.hashCode(this.dhuhr) + ((Long.hashCode(this.sunrise) + (Long.hashCode(this.fajr) * 31)) * 31)) * 31)) * 31)) * 31);
        }

        public String toString() {
            return "DayPrayerTimes(fajr=" + this.fajr + ", sunrise=" + this.sunrise + ", dhuhr=" + this.dhuhr + ", asr=" + this.asr + ", maghrib=" + this.maghrib + ", isha=" + this.isha + ")";
        }
    }

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static final class SolarParams {
        private final double declination;
        private final double transit;

        public SolarParams(double d, double d2) {
            this.transit = d;
            this.declination = d2;
        }

        public static SolarParams copy$default(SolarParams solarParams, double d, double d2, int i, Object obj) {
            if ((i & 1) != 0) {
                d = solarParams.transit;
            }
            if ((i & 2) != 0) {
                d2 = solarParams.declination;
            }
            return solarParams.copy(d, d2);
        }

        public final SolarParams copy(double d, double d2) {
            return new SolarParams(d, d2);
        }

        public boolean equals(Object obj) {
            if (this == obj) {
                return true;
            }
            if (!(obj instanceof SolarParams)) {
                return false;
            }
            SolarParams solarParams = (SolarParams) obj;
            return Double.compare(this.transit, solarParams.transit) == 0 && Double.compare(this.declination, solarParams.declination) == 0;
        }

        public final double getDeclination() {
            return this.declination;
        }

        public final double getTransit() {
            return this.transit;
        }

        public int hashCode() {
            return Double.hashCode(this.declination) + (Double.hashCode(this.transit) * 31);
        }

        public String toString() {
            return "SolarParams(transit=" + this.transit + ", declination=" + this.declination + ")";
        }
    }

    private PrayerTimesCalculator() {
    }

    public static DayPrayerTimes calculateAll$default(PrayerTimesCalculator prayerTimesCalculator, Calendar calendar, double d, double d2, CalculationMethod calculationMethod, int i, Object obj) {
        if ((i & 8) != 0) {
            calculationMethod = CalculationMethod.EGYPTIAN;
        }
        return prayerTimesCalculator.calculateAll(calendar, d, d2, calculationMethod);
    }

    public static long calculateFajrTime$default(PrayerTimesCalculator prayerTimesCalculator, Calendar calendar, double d, double d2, CalculationMethod calculationMethod, int i, Object obj) {
        if ((i & 8) != 0) {
            calculationMethod = CalculationMethod.EGYPTIAN;
        }
        return prayerTimesCalculator.calculateFajrTime(calendar, d, d2, calculationMethod);
    }

    private final double fixAngle(double d) {
        double d2 = d % 360.0d;
        return d2 < 0.0d ? d2 + 360.0d : d2;
    }

    private static long floorMinute(long j) {
        return (j / 60000) * 60000;
    }

    private final double highLatNightTime(double d, double d2, double d3, int i, boolean z) {
        double d4 = d2 * (i == 1 ? 0.5d : i == 2 ? 0.14285714285714285d : d3 / 60.0d);
        return z ? d4 + d : d - d4;
    }

    private final double hourAngleAboveHorizon(double d, double d2, double d3) {
        double sin = (Math.sin(Math.toRadians(d)) - (Math.sin(Math.toRadians(d3)) * Math.sin(Math.toRadians(d2)))) / (Math.cos(Math.toRadians(d3)) * Math.cos(Math.toRadians(d2)));
        if (-1.0d > sin || sin > 1.0d) {
            return 22.5d;
        }
        return Math.toDegrees(Math.acos(sin));
    }

    private final double hourAngleBelowHorizon(double d, double d2, double d3) {
        double sin = ((-Math.sin(Math.toRadians(d))) - (Math.sin(Math.toRadians(d3)) * Math.sin(Math.toRadians(d2)))) / (Math.cos(Math.toRadians(d3)) * Math.cos(Math.toRadians(d2)));
        if (-1.0d > sin || sin > 1.0d) {
            return 22.5d;
        }
        return Math.toDegrees(Math.acos(sin));
    }

    private final long hoursToMillis(Calendar calendar, double d) {
        Object clone = calendar.clone();
        clone.getClass();
        Calendar calendar2 = (Calendar) clone;
        int floor = (int) Math.floor(d);
        double d2 = (d - floor) * 60.0d;
        int floor2 = (int) Math.floor(d2);
        int floor3 = (int) Math.floor((d2 - floor2) * 60.0d);
        calendar2.set(11, floor);
        calendar2.set(12, floor2);
        calendar2.set(13, floor3);
        calendar2.set(14, 0);
        return calendar2.getTimeInMillis();
    }

    private final boolean invalidBelowHorizon(double d, double d2, double d3) {
        double sin = ((-Math.sin(Math.toRadians(d))) - (Math.sin(Math.toRadians(d3)) * Math.sin(Math.toRadians(d2)))) / (Math.cos(Math.toRadians(d3)) * Math.cos(Math.toRadians(d2)));
        return sin < -1.0d || sin > 1.0d;
    }

    private static long roundMinute(long j) {
        return ((j + 30000) / 60000) * 60000;
    }

    private final SolarParams solarParams(Calendar calendar, double d) {
        int i = calendar.get(1);
        int i2 = calendar.get(2) + 1;
        double d2 = i;
        double d3 = i2;
        double floor = ((Math.floor((d3 * 275.0d) / 9.0d) + ((367.0d * d2) - Math.floor(((Math.floor((d3 + 9.0d) / 12.0d) + d2) * 7.0d) / 4.0d))) + calendar.get(5)) - 730531.5d;
        double fixAngle = fixAngle((0.98560028d * floor) + 357.529d);
        double fixAngle2 = fixAngle((0.98564736d * floor) + 280.459d);
        double fixAngle3 = fixAngle((Math.sin(Math.toRadians(fixAngle * 2.0d)) * 0.02d) + (Math.sin(Math.toRadians(fixAngle)) * 1.915d) + fixAngle2);
        double d4 = 23.439d - (floor * 3.6E-7d);
        return new SolarParams((((((TimeZone.getDefault().getOffset(calendar.getTimeInMillis()) / 3600000.0d) * 15.0d) - d) / 15.0d) + 12.0d) - ((fixAngle2 / 15.0d) - (fixAngle(Math.toDegrees(Math.atan2(Math.sin(Math.toRadians(fixAngle3)) * Math.cos(Math.toRadians(d4)), Math.cos(Math.toRadians(fixAngle3))))) / 15.0d)), Math.toDegrees(Math.asin(Math.sin(Math.toRadians(fixAngle3)) * Math.sin(Math.toRadians(d4)))));
    }

    public final DayPrayerTimes calculateAll(Calendar calendar, double latitude, double longitude,
            CalculationMethod method, int asrFactor, int highLatitudeRule, int[] offsets) {
        calendar.getClass();
        method.getClass();
        SolarParams solar = solarParams(calendar, longitude);
        double transit = solar.getTransit();
        double declination = solar.getDeclination();
        double sunHourAngle = hourAngleBelowHorizon(0.833d, latitude, declination) / 15.0d;
        double sunriseHours = transit - sunHourAngle;
        double sunsetHours = transit + sunHourAngle;
        double nightHours = 24.0d - (sunsetHours - sunriseHours);
        double asrAngle = Math.toDegrees(Math.atan(1.0d /
                (Math.tan(Math.toRadians(Math.abs(latitude - declination))) + asrFactor)));
        long maghrib = roundMinute(hoursToMillis(calendar, sunsetHours));
        double ishaAngle = method.getIshaAngle() != null ? method.getIshaAngle() : 17.0d;
        long isha;
        if (method.getIshaMinutesAfterMaghrib() != null) {
            isha = maghrib + method.getIshaMinutesAfterMaghrib() * 60000L;
        } else if (invalidBelowHorizon(ishaAngle, latitude, declination) && highLatitudeRule > 0) {
            isha = roundMinute(hoursToMillis(calendar,
                    highLatNightTime(sunsetHours, nightHours, ishaAngle, highLatitudeRule, true)));
        } else {
            isha = roundMinute(hoursToMillis(calendar,
                    transit + hourAngleBelowHorizon(ishaAngle, latitude, declination) / 15.0d));
        }
        double fajrHours;
        // The original bytecode takes the night-portion branch when the angle is
        // unavailable AND a high-latitude rule is selected. JADX inverted/lost it.
        if (invalidBelowHorizon(method.getFajrAngle(), latitude, declination) && highLatitudeRule > 0) {
            fajrHours = highLatNightTime(sunriseHours, nightHours, method.getFajrAngle(), highLatitudeRule, false);
        } else {
            fajrHours = transit - hourAngleBelowHorizon(method.getFajrAngle(), latitude, declination) / 15.0d;
        }
        long fajr = roundMinute(hoursToMillis(calendar, fajrHours));
        long sunrise = floorMinute(hoursToMillis(calendar, sunriseHours));
        long dhuhr = roundMinute(hoursToMillis(calendar, transit));
        long asr = roundMinute(hoursToMillis(calendar,
                transit + hourAngleAboveHorizon(asrAngle, latitude, declination) / 15.0d));
        if (offsets != null && offsets.length >= 6) {
            fajr += offsets[0] * 60000L;
            sunrise += offsets[1] * 60000L;
            dhuhr += offsets[2] * 60000L;
            asr += offsets[3] * 60000L;
            maghrib += offsets[4] * 60000L;
            isha += offsets[5] * 60000L;
        }
        return new DayPrayerTimes(fajr, sunrise, dhuhr, asr, maghrib, isha);
    }

    public final long calculateFajrTime(Calendar calendar, double d, double d2, CalculationMethod calculationMethod) {
        calendar.getClass();
        calculationMethod.getClass();
        SolarParams solarParams = solarParams(calendar, d2);
        return roundMinute(hoursToMillis(calendar, solarParams.getTransit() - (hourAngleBelowHorizon(calculationMethod.getFajrAngle(), d, solarParams.getDeclination()) / 15.0d)));
    }

    public final DayPrayerTimes calculateAll(Calendar calendar, double d, double d2, CalculationMethod calculationMethod) {
        long roundMinute;
        calendar.getClass();
        calculationMethod.getClass();
        SolarParams solarParams = solarParams(calendar, d2);
        double transit = solarParams.getTransit();
        double declination = solarParams.getDeclination();
        double hourAngleBelowHorizon = hourAngleBelowHorizon(calculationMethod.getFajrAngle(), d, declination);
        double hourAngleBelowHorizon2 = hourAngleBelowHorizon(0.833d, d, declination);
        double hourAngleAboveHorizon = hourAngleAboveHorizon(Math.toDegrees(Math.atan(1.0d / (Math.tan(Math.toRadians(Math.abs(d - declination))) + 1.0d))), d, declination);
        double d3 = hourAngleBelowHorizon2 / 15.0d;
        long roundMinute2 = roundMinute(hoursToMillis(calendar, d3 + transit));
        if (calculationMethod.getIshaMinutesAfterMaghrib() != null) {
            roundMinute = roundMinute2 + (calculationMethod.getIshaMinutesAfterMaghrib().intValue() * 60000);
        } else {
            Double ishaAngle = calculationMethod.getIshaAngle();
            roundMinute = roundMinute(hoursToMillis(calendar, (hourAngleBelowHorizon(ishaAngle != null ? ishaAngle.doubleValue() : 17.0d, d, declination) / 15.0d) + transit));
        }
        return new DayPrayerTimes(roundMinute(hoursToMillis(calendar, transit - (hourAngleBelowHorizon / 15.0d))), floorMinute(hoursToMillis(calendar, transit - d3)), roundMinute(hoursToMillis(calendar, transit)), roundMinute(hoursToMillis(calendar, (hourAngleAboveHorizon / 15.0d) + transit)), roundMinute2, roundMinute);
    }
}
