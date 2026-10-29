package com.example;
import android.content.Context;
import org.robolectric.RuntimeEnvironment;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.robolectric.RobolectricTestRunner;
import org.robolectric.annotation.Config;
import org.robolectric.shadows.ShadowLog;
import com.example.lockwidgets.HomeWidgets;
@RunWith(RobolectricTestRunner.class)
@Config(sdk = {34})
@org.robolectric.annotation.GraphicsMode(org.robolectric.annotation.GraphicsMode.Mode.NATIVE)
public class WidgetStateTest {
    @Test public void state() throws Exception {
        Context c = RuntimeEnvironment.getApplication();
        c.getSharedPreferences("app_prefs", 0).edit().putString("language", "en").commit();
        try { Object s = HomeWidgets.state(c); System.out.println("STATE ok " + s); }
        catch (Throwable t) { System.out.println("STATE FAIL"); t.printStackTrace(System.out); }
        try { Object st = HomeWidgets.state(c); for (java.lang.reflect.Field f : st.getClass().getDeclaredFields()) { f.setAccessible(true); Object v=f.get(st); System.out.println("F "+f.getName()+"="+(v instanceof long[]?java.util.Arrays.toString((long[])v):v instanceof Object[]?java.util.Arrays.toString((Object[])v):v)); } } catch (Throwable t) { t.printStackTrace(System.out); }
        for (String k : new String[]{"Hero","NextPrayer","OneUI","Schedule","Clock","PrayerBar","DayPrayer","Pill","Hijri","Gregorian","HeroLarge","WideBar"}) { try { android.graphics.Bitmap b = HomeWidgets.renderKind(c, k, 866, 236, 2); System.out.println("RK "+k+" "+b); try (java.io.FileOutputStream o = new java.io.FileOutputStream("/tmp/rk-"+k+".png")) { b.compress(android.graphics.Bitmap.CompressFormat.PNG,100,o);}  } catch (Throwable t) { System.out.println("RK FAIL "+k); t.printStackTrace(System.out); } }
        try { HomeWidgets.updateAll(c); } catch (Throwable t) { t.printStackTrace(System.out); }
        for (ShadowLog.LogItem i : ShadowLog.getLogs()) if (i.type >= 5) { System.out.println("LOG " + i.tag + ": " + i.msg); if (i.throwable != null) i.throwable.printStackTrace(System.out); }
    }
}
