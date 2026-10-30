import java.util.*;
import java.text.*;
import com.example.fajr.calculation.*;
public class PrayerLocationFixture {
 public static void main(String[] args) {
 TimeZone.setDefault(TimeZone.getTimeZone("Africa/Cairo"));
 Calendar c=Calendar.getInstance();c.clear();c.set(2026,9,30,12,0,0);
 double lat=(double)Float.parseFloat(args[0]),lon=(double)Float.parseFloat(args[1]);
 var t=PrayerTimesCalculator.INSTANCE.calculateAll(c,lat,lon,CalculationMethod.EGYPTIAN,1,3,new int[6]);
 var f=new SimpleDateFormat("HH:mm");
 System.out.println(lat+","+lon+" timezone="+c.getTimeZone().getID()+" offset="+c.getTimeZone().getOffset(c.getTimeInMillis())/3600000.0);
 for(long x:new long[]{t.getFajr(),t.getSunrise(),t.getDhuhr(),t.getAsr(),t.getMaghrib(),t.getIsha()})System.out.print(f.format(new Date(x))+" ");System.out.println();
 String actual=String.join(" ",java.util.Arrays.stream(new long[]{t.getFajr(),t.getSunrise(),t.getDhuhr(),t.getAsr(),t.getMaghrib(),t.getIsha()}).mapToObj(x->f.format(new Date(x))).toArray(String[]::new));
 if(args.length>2 && !actual.equals(args[2]))throw new AssertionError(actual);
 if(args.length>2)System.out.println("PASS exact six-prayer fixture");
 }
}
