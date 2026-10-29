package com.example.lockwidgets;
import android.content.Context;
import android.appwidget.AppWidgetManager;
import android.content.ComponentName;
import android.content.pm.PackageManager;
import android.content.res.XmlResourceParser;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import static org.junit.Assert.*;
@RunWith(RobolectricTestRunner.class)
@Config(sdk={34})
public class ResourceParityTest {
 @Test public void nextPrayerPickerMatchesOriginalDimensionsAndHorizontalResize() throws Exception {
  Context c=RuntimeEnvironment.getApplication();
  int id=c.getResources().getIdentifier("widget_next_prayer_info","xml",c.getPackageName());
  try(XmlResourceParser x=c.getResources().getXml(id)){
   while(x.next()!=XmlResourceParser.START_TAG){}
   String ns="http://schemas.android.com/apk/res/android";
   assertEquals("140.0dip",x.getAttributeValue(ns,"minWidth"));
   assertEquals("60.0dip",x.getAttributeValue(ns,"minHeight"));
   assertEquals(1,x.getAttributeIntValue(ns,"resizeMode",-1));
   assertTrue(x.getAttributeResourceValue(ns,"previewImage",0)!=0);
   assertTrue(x.getAttributeResourceValue(ns,"previewLayout",0)!=0);
  }
 }
}
