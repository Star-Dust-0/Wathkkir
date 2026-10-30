package com.example.lockwidgets;

import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.graphics.Bitmap;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Android 15+ generated ("live") widget picker previews. Each preview is produced by the same
 * drawing code as the real widget, so the picker always shows what the widget really looks like
 * (bars fill left to right). Older Android keeps the static preview images from res/drawable.
 */
public final class WidgetPreviews {
    private static final String TAG = "WidgetPreviews";
    private static boolean running;

    private WidgetPreviews() {}

    private static int res(Context c, String type, String name) {
        return c.getResources().getIdentifier(name, type, c.getPackageName());
    }

    /** Regenerates the previews once per app version, language and light/dark mode. */
    public static void maybeGenerate(final Context ctx) {
        if (Build.VERSION.SDK_INT < 35) return;
        final Context c = ctx.getApplicationContext();
        final String key = pkgVersion(c) + "|" + HomeWidgets.lang(c) + "|"
                + (c.getResources().getConfiguration().uiMode & android.content.res.Configuration.UI_MODE_NIGHT_MASK) + "|" + c.getSharedPreferences("app_prefs", 0).getString("digits_style", "latin") + "|p2";
        final SharedPreferences sp = c.getSharedPreferences("preview_prefs", 0);
        if (key.equals(sp.getString("generated", null))) return;
        synchronized (WidgetPreviews.class) {
            if (running) return;
            running = true;
        }
        new Thread(() -> {
            try {
                int ok = generate(c);
                Log.i(TAG, "generated previews ok=" + ok);
                if (ok > 0) sp.edit().putString("generated", key).apply();
            } catch (Throwable t) {
                Log.e(TAG, "preview generation failed", t);
            } finally {
                synchronized (WidgetPreviews.class) { running = false; }
            }
        }, "widget-previews").start();
    }

    private static long pkgVersion(Context c) {
        try {
            return c.getPackageManager().getPackageInfo(c.getPackageName(), 0).getLongVersionCode();
        } catch (Exception e) {
            return 0;
        }
    }

    /** Returns how many previews were accepted by the system. */
    public static int generate(Context c) throws Exception {
        com.example.utils.Digits.refresh(c);
        AppWidgetManager m = AppWidgetManager.getInstance(c);
        HomeWidgets.State st = HomeWidgets.state(c);
        int ok = 0;
        HomeWidgets.batchState.set(st);
        try {
            String[] kinds = {"Hero", "NextPrayer", "PrayerBar", "Schedule", "DayPrayer", "Pill", "Clock", "Hijri", "Gregorian", "OneUI"};
            for (String kind : kinds) {
                try {
                    boolean one = kind.equals("OneUI");
                    // Sizes follow each widget's target cell span (cell ~80x90dp) so the preview has the same aspect as the picker box and is never stretched.
                    int wd, ht;
                    switch (kind) {
                        case "Hero": case "Schedule": case "OneUI": wd = 330; ht = 90; break;
                        case "Clock": wd = 240; ht = 90; break;
                        case "Hijri": case "Gregorian": wd = 82; ht = 90; break;
                        case "NextPrayer": wd = 160; ht = 90; break; // 3.3.3: default footprint now 2 wide x 1 tall (his spec)
                        default: wd = 160; ht = 82; break; // 2x1: PrayerBar, DayPrayer, Pill
                    }
                    RemoteViews v = new RemoteViews(c.getPackageName(), res(c, "layout", "home_prayer_art_fit"));
                    HomeWidgets.ovBegin();
                    Bitmap b = HomeWidgets.renderKind(c, kind, wd, ht, 0);
                    try { b.setDensity(1); } catch (Throwable e) { }
                    v.setImageViewBitmap(res(c, "id", "home_prayer_image"), b);
                    float pd = c.getResources().getDisplayMetrics().density;
                    HomeWidgets.ovApply(c, v, Math.round(wd * pd), Math.round(ht * pd));
                    if (set(m, c, "com.example.widget." + kind + "WidgetProvider", v)) ok++;
                } catch (Throwable t) {
                    Log.e(TAG, "home preview failed: " + kind, t);
                }
            }
        } finally {
            HomeWidgets.batchState.remove();
        }
        Class<?>[] locks = {LockWidgets.Day.class, LockWidgets.Tasbih.class, LockWidgets.Hijri.class, LockWidgets.Gregorian.class,
                LockWidgets.Next.class, LockWidgets.Bar.class, LockWidgets.Countdown.class, LockWidgets.CountdownClean.class};
        for (Class<?> t : locks) {
            try {
                RemoteViews v = new RemoteViews(c.getPackageName(), res(c, "layout", "lock_widget_image"));
                HomeWidgets.ovBegin();
                Bitmap b = LockWidgets.renderSize(c, t, 0, false);
                v.setImageViewBitmap(res(c, "id", "lock_widget_art"), b);
                if (set(m, c, t.getName(), v)) ok++;
            } catch (Throwable e) {
                Log.e(TAG, "lock preview failed: " + t.getSimpleName(), e);
            }
        }
        return ok;
    }

    private static boolean set(AppWidgetManager m, Context c, String cls, RemoteViews v) {
        ComponentName cn = new ComponentName(c, cls);
        int cats = 0;
        for (AppWidgetProviderInfo i : m.getInstalledProvidersForPackage(c.getPackageName(), null)) {
            if (i.provider.equals(cn)) cats = i.widgetCategory;
        }
        if (cats == 0) cats = AppWidgetProviderInfo.WIDGET_CATEGORY_HOME_SCREEN;
        boolean r = m.setWidgetPreview(cn, cats, v);
        Log.i(TAG, "setWidgetPreview " + cls.substring(cls.lastIndexOf('.') + 1) + " cats=" + cats + " -> " + r);
        return r;
    }
}
