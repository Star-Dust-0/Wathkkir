package com.example.widgethost;
import android.app.Instrumentation;import android.os.Bundle;import android.content.Context;import java.lang.reflect.*;import java.util.*;import org.json.*;
/** Isolated emulator-only original/recovered engine probe; no production APK changes. */
public class WatchEngineProbe extends Instrumentation {
 Bundle args;@Override public void onCreate(Bundle a){super.onCreate(a);args=a;start();}
 @Override public void onStart(){Bundle out=new Bundle();try{
 String pkg=args.getString("pkg");boolean original=args.getString("original","false").equals("true");
 Context c=getContext().createPackageContext(pkg,Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
 ClassLoader cl=c.getClassLoader();Class<?> cls=cl.loadClass(original?"d21":"com.wathakker.wear.Prayer");Object instance=original?null:cls.getField("INSTANCE").get(null);
 Method method=cls.getDeclaredMethod(original?"c":"day",Context.class,int.class,long.class);method.setAccessible(true);
 JSONArray all=new JSONArray();String[] zones={"Africa/Cairo","Europe/London","America/New_York","Australia/Sydney","UTC"};
 int[][] dates={{2026,2,8},{2026,2,29},{2026,3,5},{2026,9,4},{2026,9,25},{2026,9,29},{2026,9,30},{2026,10,1},{2028,1,29}};
 for(String zone:zones)for(int[] d:dates)for(int h:new int[]{0,1,2,3,12,23})for(int offset:new int[]{-1,0,1}){
 TimeZone.setDefault(TimeZone.getTimeZone(zone));Calendar cal=Calendar.getInstance();cal.clear();cal.set(d[0],d[1],d[2],h,30,0);long now=cal.getTimeInMillis();
 List<?> slots=(List<?>)method.invoke(instance,c,offset,now);JSONArray times=new JSONArray();for(Object s:slots){long at=original?s.getClass().getField("c").getLong(s):(Long)s.getClass().getMethod("getAt").invoke(s);times.put(at);}
 JSONObject item=new JSONObject();item.put("zone",zone);item.put("now",now);item.put("offset",offset);item.put("times",times);all.put(item);
 }
 out.putString("engine",all.toString());finish(0,out);
 }catch(Throwable e){out.putString("error",android.util.Log.getStackTraceString(e));finish(1,out);}}
}
