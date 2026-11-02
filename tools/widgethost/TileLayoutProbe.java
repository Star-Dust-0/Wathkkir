package com.example.widgethost;
import android.app.Instrumentation;import android.os.Bundle;import android.content.*;import java.lang.reflect.*;import java.util.*;
/** Verification-only foreign APK layout serialization. Explicit data/context fixture, not provider lifecycle. */
public class TileLayoutProbe extends Instrumentation {
 Bundle args;public void onCreate(Bundle a){super.onCreate(a);args=a;start();}
 public void onStart(){Bundle out=new Bundle();try{
  boolean original=args.getString("original","false").equals("true");String pkg=args.getString("pkg");
  Context target=getContext().createPackageContext(pkg,Context.CONTEXT_INCLUDE_CODE|Context.CONTEXT_IGNORE_SECURITY);
  final android.content.SharedPreferences fixture=getContext().getSharedPreferences("tile_fixture",0);fixture.edit().clear().putInt("theme",0).putInt("lang",2).commit();
  Context c=new ContextWrapper(target){public android.content.SharedPreferences getSharedPreferences(String n,int m){return n.equals("wear_prefs")?fixture:super.getSharedPreferences(n,m);}};
  ClassLoader cl=c.getClassLoader();Class<?> service=cl.loadClass("com.wathakker.wear.PrayerTileService");Object obj=service.getConstructor().newInstance();Method attach=ContextWrapper.class.getDeclaredMethod("attachBaseContext",Context.class);attach.setAccessible(true);attach.invoke(obj,c);
  long now=1793354400000L;Class<?> slot=cl.loadClass(original?"ii1":"com.wathakker.wear.Slot");Object prev,next;
  if(original){prev=slot.getConstructor(int.class,long.class,String.class).newInstance(0,now-3600000L,"Fajr");next=slot.getConstructor(int.class,long.class,String.class).newInstance(2,now+180000L,"Dhuhr");}
  else{prev=slot.getConstructor(int.class,String.class,long.class).newInstance(0,"Fajr",now-3600000L);next=slot.getConstructor(int.class,String.class,long.class).newInstance(2,"Dhuhr",now+180000L);}
  Class<?> state=cl.loadClass(original?"e21":"com.wathakker.wear.PrayerState");Object s=state.getConstructor(long.class,List.class,slot,slot,slot).newInstance(now,Arrays.asList(prev,next),prev,next,next);
  Class<?> iq=cl.loadClass(original?"c21":"com.wathakker.wear.Prayer$Iqama");Object element;
  if(original)element=service.getMethod("g",state,iq).invoke(obj,s,null);else element=service.getMethod("layout",state,iq,boolean.class).invoke(obj,s,null,false);
  Method proto=element.getClass().getMethod(original?"h":"toLayoutElementProto");proto.setAccessible(true);Object msg=proto.invoke(element);
  out.putString("proto",msg.toString());out.putString("class",msg.getClass().getName());
  for(Method m:msg.getClass().getMethods())if(m.getReturnType()==byte[].class&&m.getParameterCount()==0){try{m.setAccessible(true);byte[] data=(byte[])m.invoke(msg);out.putString("bytes",android.util.Base64.encodeToString(data,android.util.Base64.NO_WRAP));out.putString("bytes_method",m.getName());break;}catch(Throwable ignored){}}
  finish(0,out);
 }catch(Throwable e){out.putString("error",android.util.Log.getStackTraceString(e));finish(1,out);}}
}
