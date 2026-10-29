package com.example.lockwidgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.view.View;
import android.widget.Chronometer;
import android.widget.RemoteViews;
import com.example.R;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.RuntimeEnvironment;
import org.robolectric.annotation.Config;
import org.robolectric.annotation.GraphicsMode;
import org.robolectric.shadows.ShadowLog;
import static org.junit.Assert.*;

@RunWith(RobolectricTestRunner.class)
@Config(sdk = 34)
@GraphicsMode(GraphicsMode.Mode.NATIVE)
public class WidgetRecoveryTest {
    private Context context(String language) {
        Context c = RuntimeEnvironment.getApplication();
        c.getSharedPreferences("app_prefs", 0).edit().putString("language", language).commit();
        c.getSharedPreferences("fajr_prefs", 0).edit().putString("language", language).commit();
        return c;
    }
    @Test public void lockWidgetsRenderInBothLanguagesAndSizes() throws Exception {
        for (String language : new String[]{"en", "ar"}) {
            Context c = context(language);
            for (Class<?> kind : new Class<?>[]{LockWidgets.Day.class, LockWidgets.Tasbih.class,
                    LockWidgets.Hijri.class, LockWidgets.Gregorian.class, LockWidgets.Next.class,
                    LockWidgets.Bar.class, LockWidgets.Countdown.class, LockWidgets.CountdownClean.class,
                    LockWidgets.Prayers.class}) {
                for (boolean small : new boolean[]{false, true}) {
                    ShadowLog.clear();
                    HomeWidgets.ovBegin();
                    Bitmap b = LockWidgets.renderSize(c, kind, 1, small);
                    assertNotNull(kind.getSimpleName(), b);
                    assertEquals(Math.round((small ? 54 : 123) * c.getResources().getDisplayMetrics().density), b.getWidth());
                    int visible = 0;
                    for (int y = 0; y < b.getHeight(); y++) for (int x = 0; x < b.getWidth(); x++)
                        if ((b.getPixel(x,y) >>> 24) > 0) visible++;
                    assertTrue(kind.getSimpleName()+" empty bitmap", visible > 20);
                    for (ShadowLog.LogItem log : ShadowLog.getLogs()) {
                        if (log.type >= 6 && log.tag.startsWith("Wathakker"))
                            fail(kind.getSimpleName()+": "+log.msg+" "+log.throwable);
                    }
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream("/tmp/lock-"+language+"-"+kind.getSimpleName()+"-"+small+".png")) {
                        b.compress(Bitmap.CompressFormat.PNG,100,out);
                    }
                }
            }
        }
    }
    @Test public void homeWidgetsRenderAtPhoneAndTabletSizes() throws Exception {
        for (String language : new String[]{"en", "ar"}) {
            Context c = context(language);
            for (String kind : new String[]{"Hero", "NextPrayer", "OneUI", "Schedule", "Clock", "PrayerBar", "DayPrayer", "Pill", "Hijri", "Gregorian", "HeroLarge", "WideBar"}) {
                for (int width : new int[]{360, 720}) {
                    ShadowLog.clear();
                    HomeWidgets.ovBegin();
                    Bitmap b = HomeWidgets.renderKind(c,kind,width,120,1);
                    assertNotNull(kind,b);
                    assertTrue(kind, b.getWidth() > 0 && b.getHeight() > 0);
                    for (ShadowLog.LogItem log : ShadowLog.getLogs()) {
                        if (log.type >= 6 && log.tag.startsWith("Wathakker")) fail(kind+": "+log.msg+" "+log.throwable);
                    }
                    try (java.io.FileOutputStream out = new java.io.FileOutputStream("/tmp/home-"+language+"-"+kind+"-"+width+".png")) {
                        b.compress(Bitmap.CompressFormat.PNG,100,out);
                    }
                }
            }
        }
    }
    @Test @Config(qualifiers = "sw720dp") public void tabletOverlayUsesFractionalNativeCountdown() {
        Context c = context("en");
        RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.lock_widget_image);
        HomeWidgets.ovBegin();
        HomeWidgets.ovBase.set(150000L);
        HomeWidgets.ovFmt.set("Remaining: %s");
        HomeWidgets.ovGeo.set(new float[]{100,54,150,18,0xff123456,1});
        HomeWidgets.ovApply(c,views,246,108);
        View result=views.apply(c,null);
        Chronometer chrono=result.findViewById(R.id.ov_f_semi);
        assertEquals(View.VISIBLE,chrono.getVisibility());
        assertTrue(chrono.isCountDown());
        assertEquals(150000L,chrono.getBase());
        assertEquals(View.GONE,result.findViewById(R.id.ov_box).getVisibility());
        assertNull(HomeWidgets.ovBase.get());
        assertNull(HomeWidgets.ovGeo.get());
    }
    @Test public void overlayFailureAlsoClearsRenderState() {
        Context c=context("en");
        HomeWidgets.ovBegin();
        HomeWidgets.ovBase.set(100000L);
        HomeWidgets.ovFmt.set("%s");
        HomeWidgets.ovGeo.set(new float[]{1});
        HomeWidgets.ovApply(c,new RemoteViews(c.getPackageName(), R.layout.lock_widget_image),123,54);
        assertNull(HomeWidgets.ovBase.get());
        assertNull(HomeWidgets.ovGeo.get());
        assertNull(HomeWidgets.ovFmt.get());
        assertNull(HomeWidgets.ovWanted.get());
    }
    @Test public void nativeOverlayRunsAndAlwaysClearsRenderState() {
        Context c = context("en");
        RemoteViews views = new RemoteViews(c.getPackageName(), R.layout.lock_widget_image);
        HomeWidgets.ovBegin();
        HomeWidgets.ovBase.set(100000L);
        HomeWidgets.ovFmt.set("%s");
        HomeWidgets.ovGeo.set(new float[]{60,27,100,18,0xff123456,0});
        HomeWidgets.ovApply(c, views, 123,54);
        View result = views.apply(c,null);
        Chronometer chrono = result.findViewById(R.id.ov_chrono_med);
        assertEquals(View.VISIBLE,chrono.getVisibility());
        assertTrue(chrono.isCountDown());
        assertEquals(100000L,chrono.getBase());
        assertNull(HomeWidgets.ovBase.get());
        assertNull(HomeWidgets.ovGeo.get());
        assertNull(HomeWidgets.ovFmt.get());
        assertNull(HomeWidgets.ovWanted.get());
        HomeWidgets.ovApply(c,views,123,54);
        result = views.apply(c,null);
        assertEquals(View.GONE,result.findViewById(R.id.ov_box).getVisibility());
    }
}
