package com.example.widgethost;

import android.app.Activity;
import android.appwidget.AppWidgetHost;
import android.appwidget.AppWidgetHostView;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProviderInfo;
import android.content.ComponentName;
import android.os.Bundle;
import android.util.Log;
import android.widget.LinearLayout;
import android.widget.ScrollView;
import android.view.ViewGroup;
import android.widget.TextView;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.os.Handler;
import android.os.Looper;
import java.util.ArrayList;
import java.util.List;

public class MainActivity extends Activity {
    private String PKG = "com.wathakker.app";
    private static final String[][] WIDGETS = {
        {"Hero", "330", "90"},
        {"HeroLarge", "330", "160"},
        {"Schedule", "330", "90"},
        {"Clock", "250", "90"},
        {"NextPrayer", "110", "90"},
        {"PrayerBar", "110", "90"},
        {"DayPrayer", "110", "90"},
        {"Pill", "110", "90"},
        {"Hijri", "90", "90"},
        {"Gregorian", "90", "90"},
    };

    private AppWidgetHost host;
    private final List<String> kinds = new ArrayList<>();
    private final List<AppWidgetHostView> views = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        if(getIntent().hasExtra("package")) PKG=getIntent().getStringExtra("package");
        float d = getResources().getDisplayMetrics().density;
        AppWidgetManager awm = AppWidgetManager.getInstance(this);
        host = new AppWidgetHost(getApplicationContext(), 1024);
        host.startListening();
        ScrollView sv = new ScrollView(this);
        LinearLayout col = new LinearLayout(this);
        col.setOrientation(LinearLayout.VERTICAL);
        sv.addView(col);
        String mode=getIntent().getStringExtra("mode");
        if ("previews".equals(mode)) {
            String[] cls = {"com.example.widget.HeroWidgetProvider","com.example.widget.HeroLargeWidgetProvider","com.example.widget.PrayerBarWidgetProvider","com.example.widget.DayPrayerWidgetProvider","com.example.widget.PillWidgetProvider","com.example.widget.NextPrayerWidgetProvider","com.example.widget.HijriWidgetProvider","com.example.widget.OneUIWidgetProvider","com.example.widget.ClockWidgetProvider","com.example.lockwidgets.LockWidgets$Bar","com.example.lockwidgets.LockWidgets$Countdown","com.example.lockwidgets.LockWidgets$CountdownClean","com.example.lockwidgets.LockWidgets$Next"};
            for (String k : cls) {
                TextView label = new TextView(this);
                ComponentName cn = new ComponentName(PKG, k);
                android.widget.RemoteViews rv = null; int usedCat = 0;
                try {
                    for (int cat : new int[]{1, 2, 3}) {
                        if (rv == null) { rv = awm.getWidgetPreview(cn, android.os.Process.myUserHandle(), cat); usedCat = cat; }
                        if (rv == null) { rv = awm.getWidgetPreview(cn, null, cat); usedCat = cat; }
                    }
                } catch (Throwable t) { Log.i("WidgetHost", "getWidgetPreview error " + k + " " + t); }
                label.setText(k.substring(k.lastIndexOf('.') + 1) + " generatedPreview=" + (rv != null) + " cat=" + usedCat);
                Log.i("WidgetHost", "preview " + k + " -> " + (rv != null));
                col.addView(label);
                if (rv != null) {
                    try {
                        android.widget.FrameLayout box = new android.widget.FrameLayout(this);
                        box.setBackgroundColor(0x33888888);
                        int cols = k.contains("HeroLarge")||k.contains("Hero")||k.contains("OneUI") ? 4 : k.contains("Clock") ? 3 : (k.contains("PrayerBar")||k.contains("DayPrayer")||k.contains("Pill")) ? 2 : 1;
                        int rows = k.contains("HeroLarge") ? 2 : 1;
                        android.view.View pv = rv.apply(getApplicationContext(), box);
                        box.addView(pv, new android.widget.FrameLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.MATCH_PARENT));
                        col.addView(box, new LinearLayout.LayoutParams((int)(cols*80*d), (int)(rows*90*d)));
                    } catch (Throwable t) { Log.i("WidgetHost", "apply error " + k + " " + t); }
                }
            }
            setContentView(sv);
            return;
        }
        if ("barsB".equals(mode)) sv.setBackgroundColor(0xFFC8C8C8);
        if ("frost".equals(mode)) sv.setBackground(new android.graphics.drawable.GradientDrawable(android.graphics.drawable.GradientDrawable.Orientation.TL_BR, new int[]{0xFF1E88E5,0xFFE91E63,0xFFFFB300}));
        String[][] selected="frost".equals(mode)?new String[][]{{"Hero","330","90"},{"NextPrayer","110","90"},{"PrayerBar","250","60"},{"Clock","250","90"}}:"barsA".equals(mode)?new String[][]{{"Hero","330","90"},{"HeroLarge","330","160"},{"NextPrayer","110","90"},{"PrayerBar","250","60"},{"WideBar","330","90"},{"OneUI","330","90"}}:"barsB".equals(mode)?new String[][]{{"Iqama","330","90"},{"IqamaSmall","110","90"},{"Lock:Countdown","123","54"},{"Lock:CountdownClean","123","54"},{"Lock:Bar","123","54"},{"Lock:Next","123","54"}}:"oneui".equals(mode)?new String[][]{{"OneUI","330","90"},{"OneUI","330","180"}}:"iqama".equals(mode)?new String[][]{{"Hero","330","90"},{"Lock:CountdownClean","123","54"},{"Lock:Countdown","123","54"},{"Lock:Bar","123","54"},{"Lock:Next","123","54"},{"Clock","250","90"}}:WIDGETS;
        for (String[] w : selected) {
            String kind = w[0];
            int wd = Integer.parseInt(w[1]), ht = Integer.parseInt(w[2]);
            TextView label = new TextView(this);
            col.addView(label);
            int id = host.allocateAppWidgetId();
            ComponentName cn = new ComponentName(PKG, kind.startsWith("Lock:") ? "com.example.lockwidgets.LockWidgets$"+kind.substring(5) : "com.example.widget." + kind + "WidgetProvider");
            boolean ok = awm.bindAppWidgetIdIfAllowed(id, cn, null);
            Log.i("WidgetHost", "bind " + kind + " id=" + id + " -> " + ok);
            label.setText(kind + " bind=" + ok);
            if (!ok) continue;
            AppWidgetProviderInfo info = awm.getAppWidgetInfo(id);
            if (info == null) { Log.i("WidgetHost", "no info for " + kind); continue; }
            AppWidgetHostView v = host.createView(getApplicationContext(), id, info);
            v.setAppWidget(id, info);
            // Match a launcher: this public overload subtracts host default padding
            // before sending provider content dimensions. Pass outer dimensions once.
            v.updateAppWidgetSize(null, wd, ht, wd, ht);
            col.addView(v, new LinearLayout.LayoutParams((int)(wd * d), (int)(ht * d)));
            kinds.add(kind);
            views.add(v);
        }
        setContentView(sv);
        // After updates settle, dump per-widget pixel stats: a blank/never-updated
        // widget (the R8 regression) is a uniform surface -> std near 0.
        new Handler(Looper.getMainLooper()).postDelayed(this::dumpPixelStats, 8000);
    }

    private void dumpPixelStats() {
        for (int i = 0; i < views.size(); i++) {
            AppWidgetHostView v = views.get(i);
            String kind = kinds.get(i);
            try {
                int w = v.getWidth(), h = v.getHeight();
                if (w <= 0 || h <= 0) { Log.i("WidgetPix", kind + " w=" + w + " h=" + h + " SKIP"); continue; }
                Bitmap b = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888);
                Canvas c = new Canvas(b);
                v.draw(c);
                int[] px = new int[w * h];
                b.getPixels(px, 0, w, 0, 0, w, h);
                long sum = 0, sum2 = 0; int nonbg = 0; int bg = px[0];
                for (int p : px) {
                    int lum = ((p >> 16) & 255) * 3 + ((p >> 8) & 255) * 6 + (p & 255);
                    sum += lum; sum2 += (long) lum * lum;
                    if (p != bg) nonbg++;
                }
                int n = px.length;
                double mean = (double) sum / n;
                double std = Math.sqrt(Math.max(0, (double) sum2 / n - mean * mean)) / 10.0;
                Log.i("WidgetPix", kind + " w=" + w + " h=" + h
                        + " std=" + String.format(java.util.Locale.US, "%.1f", std)
                        + " nonbg=" + (100 * nonbg / n) + "%");
            } catch (Throwable t) {
                Log.i("WidgetPix", kind + " ERROR " + t);
            }
        }
    }

    @Override
    protected void onDestroy() {
        try { if (host != null) host.stopListening(); } catch (Exception e) { }
        super.onDestroy();
    }
}
