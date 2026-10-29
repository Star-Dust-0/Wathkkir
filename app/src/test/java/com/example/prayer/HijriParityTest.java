package com.example.prayer;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import java.util.*;
import android.icu.util.IslamicCalendar;
import static org.junit.Assert.*;
/** Oracle is the exact ICU call sequence in original ce0.c; not an independent calendar algorithm. */
@RunWith(RobolectricTestRunner.class)
@Config(sdk=34)
public class HijriParityTest {
 @Test public void originalIcuSequenceMatchesAcrossZonesAndOffsets() {
  TimeZone prev=TimeZone.getDefault();int count=0;
  try {
   for(String zone:new String[]{"Africa/Cairo","Asia/Riyadh","Europe/London","Pacific/Kiritimati","America/New_York"}) {
    TimeZone.setDefault(TimeZone.getTimeZone(zone));
    for(int year:new int[]{2024,2025,2026,2027,2030})for(int month=0;month<12;month++)for(int day:new int[]{1,14,28})for(int hour:new int[]{0,23})for(int offset=-2;offset<=2;offset++){
     Calendar c=Calendar.getInstance();c.clear();c.set(year,month,day,hour,59);c.add(Calendar.DATE,offset);
     IslamicCalendar ic=new IslamicCalendar();ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);ic.setTime(new Date(c.getTimeInMillis()));
     kotlin.Triple<Integer,Integer,Integer> h=HijriDate.INSTANCE.hijriYmd(c);
     assertEquals((Integer)ic.get(1),h.getFirst());assertEquals((Integer)(ic.get(2)+1),h.getSecond());assertEquals((Integer)ic.get(5),h.getThird());count++;
    }
   }
   assertEquals(9000,count);
  } finally { TimeZone.setDefault(prev); }
 }
}
