package com.example.widgethost;
import android.app.Instrumentation;
import android.os.Bundle;
import android.os.Looper;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
/** Isolated verification helper: requests OS GPS delivery; never sets app prefs or geocoder records. */
public final class GpsFeed extends Instrumentation {
  @Override public void onCreate(Bundle b) { super.onCreate(b); start(); }
  @Override public void onStart() {
    Bundle result=new Bundle();
    LocationManager lm=(LocationManager)getTargetContext().getSystemService(android.content.Context.LOCATION_SERVICE);
    LocationListener listener=new LocationListener(){ @Override public void onLocationChanged(Location location){} };
    try { lm.requestLocationUpdates(LocationManager.GPS_PROVIDER,0,0,listener,Looper.getMainLooper()); Thread.sleep(240000); result.putString("gps_feed","completed"); }
    catch(Exception e) {result.putString("gps_feed","error:"+e.toString());}
    finally {try{lm.removeUpdates(listener);}catch(Exception ignored){} }
    finish(0,result);
  }
}
