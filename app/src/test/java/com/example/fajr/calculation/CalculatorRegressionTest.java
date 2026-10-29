package com.example.fajr.calculation;
import java.util.Calendar;
import java.util.TimeZone;
import org.junit.Test;
import static org.junit.Assert.*;
public class CalculatorRegressionTest {
    @Test public void londonSummerFajrUsesSelectedNightPortion() {
        Calendar c=Calendar.getInstance(TimeZone.getTimeZone("Europe/London"));
        c.clear(); c.set(2026,5,21,12,0,0);
        long[] expected={1782045120000L,1782007320000L,1782016800000L,1782012660000L};
        for(int rule=0;rule<4;rule++) {
            PrayerTimesCalculator.DayPrayerTimes t=PrayerTimesCalculator.INSTANCE.calculateAll(
                c,51.5074,-.1278,CalculationMethod.MUSLIM_WORLD_LEAGUE,1,rule,new int[6]);
            assertEquals("high-latitude rule "+rule,expected[rule],t.getFajr());
        }
    }
}
