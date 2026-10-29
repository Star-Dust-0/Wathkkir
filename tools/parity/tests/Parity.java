import java.net.*;import java.io.*;import java.lang.reflect.*;import java.util.*;
public class Parity {
 static class Engine {
 ClassLoader c; Class<?> m,p,phase;Object instance;Method calc;
 Engine(String path)throws Exception {c=new URLClassLoader(new URL[]{new File(path).toURI().toURL()},ClassLoader.getPlatformClassLoader());m=c.loadClass("com.example.fajr.calculation.CalculationMethod");p=c.loadClass("com.example.fajr.calculation.PrayerTimesCalculator");phase=c.loadClass("com.example.lockwidgets.PrayerPhase");instance=p.getField("INSTANCE").get(null);calc=p.getMethod("calculateAll",Calendar.class,double.class,double.class,m,int.class,int.class,int[].class);}
 long[] times(Calendar cal,double lat,double lon,String meth,int asr,int high,int[] off)throws Exception {Object x=calc.invoke(instance,cal.clone(),lat,lon,m.getField(meth).get(null),asr,high,off);long[] r=new long[6];String[] n={"Fajr","Sunrise","Dhuhr","Asr","Maghrib","Isha"};for(int i=0;i<6;i++)r[i]=(Long)x.getClass().getMethod("get"+n[i]).invoke(x);return r;}
 String resolve(long[] t,long[] y,long[] next,long now,boolean fri,boolean sun)throws Exception {Object x=phase.getMethod("resolve",long[].class,long[].class,long[].class,long.class,boolean.class,boolean.class).invoke(null,t,y,next,now,fri,sun);String r="";for(String f:new String[]{"targetIndex","previousIndex","start","target","iqama"})r+=phase.getField(f).get(x)+",";return r;}
 }
 public static void main(String[] args)throws Exception {
 Engine a=new Engine(args[0]),b=new Engine(args[1]);String[] methods={"MUSLIM_WORLD_LEAGUE","EGYPTIAN","UMM_AL_QURA","ISNA","KARACHI","DUBAI","KUWAIT","QATAR"};
 double[][] places={{30.0444,31.2357},{21.4225,39.8262},{51.5074,-.1278},{40.7128,-74.006},{69.6492,18.9553},{-33.8688,151.2093},{0,0},{-33.9249,18.4241}};
 String[] zones={"Africa/Cairo","Asia/Riyadh","Europe/London","America/New_York","Europe/Oslo","Australia/Sydney","UTC","Africa/Johannesburg"};
 int[][] dates={{2026,0,1},{2026,2,20},{2026,5,21},{2026,8,23},{2026,9,29},{2026,9,30},{2026,11,21},{2028,1,29}};int[][] offsets={new int[6],{-30,-20,-10,10,20,30}};
 int cases=0,mismatch=0;for(int z=0;z<places.length;z++)for(int[] date:dates)for(String method:methods)for(int asr=1;asr<=2;asr++)for(int hi=0;hi<=3;hi++)for(int[] off:offsets){Calendar c=Calendar.getInstance(TimeZone.getTimeZone(zones[z]));c.clear();c.set(date[0],date[1],date[2],12,0,0);long[] x=a.times(c,places[z][0],places[z][1],method,asr,hi,off),y=b.times(c,places[z][0],places[z][1],method,asr,hi,off);cases++;if(!Arrays.equals(x,y)){mismatch++;if(mismatch<8)System.out.println("MISMATCH "+z+" "+Arrays.toString(date)+" "+method+" "+asr+" "+hi+" "+Arrays.toString(x)+" "+Arrays.toString(y));}}
 System.out.println("Prayer calculation cases="+cases+" outputs="+(cases*6)+" mismatches="+mismatch);
 long[] t={18000000,21600000,43200000,57600000,64800000,72000000},y=t.clone(),n=t.clone();for(int i=0;i<6;i++){y[i]-=86400000;n[i]+=86400000;}int pc=0,pm=0;for(boolean fri:new boolean[]{false,true})for(boolean sun:new boolean[]{false,true})for(long now=-3600000;now<=90000000;now+=30000){pc++;if(!a.resolve(t,y,n,now,fri,sun).equals(b.resolve(t,y,n,now,fri,sun)))pm++;}System.out.println("Prayer/iqama phase cases="+pc+" mismatches="+pm);if(mismatch+pm>0)System.exit(1);
 }
}
