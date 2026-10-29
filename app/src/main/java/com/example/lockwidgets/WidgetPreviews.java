package com.example.lockwidgets;

import com.example.utils.Digits;
import com.example.lockwidgets.LockWidgets;
import android.graphics.Bitmap;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Build;
import android.util.Log;
import android.widget.RemoteViews;

import java.util.List;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class WidgetPreviews {
    private static final String TAG = "WidgetPreviews";
    private static boolean running;

    private WidgetPreviews() {
    }

    /* JADX WARN: Code restructure failed: missing block: B:28:0x0052, code lost:
    
        if (r11.equals("NextPrayer") != false) goto L13;
     */
    /* JADX WARN: Code restructure failed: missing block: B:46:0x008c, code lost:
    
        if (r11.equals("Schedule") != false) goto L33;
     */
    /* JADX WARN: Failed to restore switch over string. Please report as a decompilation issue */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static int generate(Context context) {
        Digits.refresh(context);
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        HomeWidgets.batchState.set(HomeWidgets.state(context));
        try {
            String[] strArr = {"Hero", "NextPrayer", "PrayerBar", "Schedule", "DayPrayer", "Pill", "Clock", "Hijri", "Gregorian", "OneUI"};
            int i = 0;
            for (int i2 = 0; i2 < 10; i2++) {
                String str = strArr[i2];
                try {
                    str.equals("OneUI");
                    int i3 = 82;
                    int i4 = 90;
                    switch (str.hashCode()) {
                        case -633276745:
                            break;
                        case -164960570:
                            break;
                        case 2245658:
                            break;
                        case 65203182:
                            if (str.equals("Clock")) {
                                i3 = 240;
                                break;
                            }
                            i4 = 82;
                            i3 = 160;
                            break;
                        case 69727072:
                            break;
                        case 76334938:
                            break;
                        case 641218778:
                            break;
                        default:
                            i4 = 82;
                            i3 = 160;
                            break;
                    }
                    RemoteViews remoteViews = new RemoteViews(context.getPackageName(), res(context, "layout", "home_prayer_art_fit"));
                    HomeWidgets.ovBegin();
                    Bitmap renderKind = HomeWidgets.renderKind(context, str, i3, i4, 0);
                    try {
                        renderKind.setDensity(1);
                    } catch (Throwable unused) {
                    }
                    remoteViews.setImageViewBitmap(res(context, "id", "home_prayer_image"), renderKind);
                    float f = context.getResources().getDisplayMetrics().density;
                    HomeWidgets.ovApply(context, remoteViews, Math.round(i3 * f), Math.round(i4 * f));
                    StringBuilder sb = new StringBuilder();
                    sb.append("com.example.widget.");
                    sb.append(str);
                    sb.append("WidgetProvider");
                    if (set(appWidgetManager, context, sb.toString(), remoteViews)) {
                        i++;
                    }
                } catch (Throwable th) {
                    StringBuilder sb2 = new StringBuilder();
                    sb2.append("home preview failed: ");
                    sb2.append(str);
                    Log.e(TAG, sb2.toString(), th);
                }
            }
            HomeWidgets.batchState.remove();
            Class[] clsArr = {LockWidgets.Day.class, LockWidgets.Tasbih.class, LockWidgets.Hijri.class, LockWidgets.Gregorian.class, LockWidgets.Next.class, LockWidgets.Bar.class, LockWidgets.Countdown.class, LockWidgets.CountdownClean.class};
            for (int i5 = 0; i5 < 8; i5++) {
                Class cls = clsArr[i5];
                try {
                    RemoteViews remoteViews2 = new RemoteViews(context.getPackageName(), res(context, "layout", "lock_widget_image"));
                    HomeWidgets.ovBegin();
                    remoteViews2.setImageViewBitmap(res(context, "id", "lock_widget_art"), LockWidgets.renderSize(context, cls, 0, false));
                    if (set(appWidgetManager, context, cls.getName(), remoteViews2)) {
                        i++;
                    }
                } catch (Throwable th2) {
                    Log.e(TAG, "lock preview failed: ".concat(cls.getSimpleName()), th2);
                }
            }
            return i;
        } catch (Throwable th3) {
            HomeWidgets.batchState.remove();
            throw th3;
        }
    }

    /* JADX INFO: Access modifiers changed from: private */
    public static /* synthetic */ void generateTask(Context context, SharedPreferences sharedPreferences, String str) {
        try {
            int generate = generate(context);
            Log.i(TAG, "generated previews ok=" + generate);
            if (generate > 0) {
                sharedPreferences.edit().putString("generated", str).apply();
            }
            synchronized (WidgetPreviews.class) {
                running = false;
            }
        } catch (Throwable th) {
            try {
                Log.e(TAG, "preview generation failed", th);
                synchronized (WidgetPreviews.class) {
                    running = false;
                }
            } catch (Throwable th2) {
                synchronized (WidgetPreviews.class) {
                    running = false;
                    throw th2;
                }
            }
        }
    }

    public static void maybeGenerate(Context context) {
        if (Build.VERSION.SDK_INT < 35) {
            return;
        }
        Context applicationContext = context.getApplicationContext();
        String str = pkgVersion(applicationContext) + "|" + HomeWidgets.lang(applicationContext) + "|" + (applicationContext.getResources().getConfiguration().uiMode & 48) + "|" + applicationContext.getSharedPreferences("app_prefs", 0).getString("digits_style", "latin") + "|p2";
        SharedPreferences sharedPreferences = applicationContext.getSharedPreferences("preview_prefs", 0);
        if (str.equals(sharedPreferences.getString("generated", null))) {
            return;
        }
        synchronized (WidgetPreviews.class) {
            try {
                if (running) {
                    return;
                }
                running = true;
                new Thread(() -> generateTask(applicationContext, sharedPreferences, str), "widget-previews").start();
            } catch (Throwable th) {
                throw th;
            }
        }
    }

    private static long pkgVersion(Context context) {
        long longVersionCode;
        try {
            longVersionCode = context.getPackageManager().getPackageInfo(context.getPackageName(), 0).getLongVersionCode();
            return longVersionCode;
        } catch (Exception unused) {
            return 0L;
        }
    }

    private static int res(Context context, String str, String str2) {
        return context.getResources().getIdentifier(str2, str, context.getPackageName());
    }

    private static boolean set(AppWidgetManager appWidgetManager, Context context, String str, RemoteViews remoteViews) {
        List<AppWidgetProviderInfo> installedProvidersForPackage;
        boolean widgetPreview;
        ComponentName componentName = new ComponentName(context, str);
        installedProvidersForPackage = appWidgetManager.getInstalledProvidersForPackage(context.getPackageName(), null);
        int i = 0;
        for (AppWidgetProviderInfo appWidgetProviderInfo : installedProvidersForPackage) {
            if (appWidgetProviderInfo.provider.equals(componentName)) {
                i = appWidgetProviderInfo.widgetCategory;
            }
        }
        if (i == 0) {
            i = 1;
        }
        widgetPreview = appWidgetManager.setWidgetPreview(componentName, i, remoteViews);
        Log.i(TAG, "setWidgetPreview " + str.substring(str.lastIndexOf(46) + 1) + " cats=" + i + " -> " + widgetPreview);
        return widgetPreview;
    }
}
