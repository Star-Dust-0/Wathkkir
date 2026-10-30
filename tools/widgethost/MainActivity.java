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
        String operation=getIntent().getStringExtra("operation");
        if ("delete".equals(operation)) { host.deleteHost(); Log.i("WidgetRecoveryAudit","deleted host"); finish(); return; }
        host.deleteHost(); // Each scene owns a clean host, including deletion callbacks from the preceding scene.
        host = new AppWidgetHost(getApplicationContext(), 1024);host.startListening();

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
        String[][] selected="frost".equals(mode)?new String[][]{{"Hero","330","90"},{"NextPrayer","110","90"},{"PrayerBar","250","60"},{"Clock","250","90"}}:"barsA".equals(mode)?new String[][]{{"Hero","330","90"},{"HeroLarge","330","160"},{"NextPrayer","110","90"},{"PrayerBar","250","60"},{"WideBar","330","90"},{"OneUI","330","90"}}:"barsB".equals(mode)?new String[][]{{"Iqama","330","90"},{"IqamaSmall","110","90"},{"Lock:Countdown","123","54"},{"Lock:CountdownClean","123","54"},{"Lock:Bar","123","54"},{"Lock:Next","123","54"}}:"oneui".equals(mode)?new String[][]{{"OneUI","330","90"},{"OneUI","330","180"}}:"iqama".equals(mode)?new String[][]{{"Hero","330","90"},{"Lock:CountdownClean","123","54"},{"Lock:Countdown","123","54"},{"Lock:Bar","123","54"},{"Lock:Next","123","54"},{"Clock","250","90"}}:"pill".equals(mode)?new String[][]{{"Pill","110","90"}}:WIDGETS;

        if ("recover-homeA".equals(mode)) selected=new String[][]{{"Hero","330","90"},{"NextPrayer","160","90"},{"PrayerBar","160","82"},{"Clock","240","90"}};
        if ("recover-homeB".equals(mode)) selected=new String[][]{{"Schedule","330","90"},{"DayPrayer","160","82"},{"Pill","160","82"},{"OneUI","330","90"}};
        if ("recover-dates".equals(mode)) selected=new String[][]{{"Hijri","82","90"},{"Gregorian","82","90"}};
        if ("recover-lockA".equals(mode)) selected=new String[][]{{"Lock:Day","123","54"},{"Lock:Tasbih","123","54"},{"Lock:Hijri","123","54"},{"Lock:Gregorian","123","54"}};
        if ("recover-lockB".equals(mode)) selected=new String[][]{{"Lock:Next","123","54"},{"Lock:Bar","123","54"},{"Lock:Countdown","123","54"},{"Lock:CountdownClean","123","54"}};
        for (String[] w : selected) {
            String kind = w[0];
            int wd = Integer.parseInt(w[1]), ht = Integer.parseInt(w[2]);
            TextView label = new TextView(this);
            col.addView(label);
            int id = host.allocateAppWidgetId();
            ComponentName cn = new ComponentName(PKG, kind.startsWith("Lock:") ? "com.example.lockwidgets.LockWidgets$"+kind.substring(5) : "com.example.widget." + kind + "WidgetProvider");
            Bundle bindOptions = new Bundle();
            if (kind.startsWith("Lock:")) bindOptions.putInt(AppWidgetManager.OPTION_APPWIDGET_HOST_CATEGORY, AppWidgetProviderInfo.WIDGET_CATEGORY_KEYGUARD);
            boolean ok = awm.bindAppWidgetIdIfAllowed(id, cn, bindOptions);
            Log.i("WidgetHost", "bind " + kind + " id=" + id + " -> " + ok);
            label.setText(kind + " bind=" + ok);
            if (!ok) continue;
            AppWidgetProviderInfo info = awm.getAppWidgetInfo(id);
            if (info == null) { Log.i("WidgetHost", "no info for " + kind); continue; }
            AppWidgetHostView v = host.createView(getApplicationContext(), id, info);
            v.setAppWidget(id, info);
            // Match a launcher: this public overload subtracts host default padding
            // before sending provider content dimensions. Pass outer dimensions once.
            // Lock artwork has the fixed Samsung content size 123x54dp. This
            // generic host adds framework padding, so include that in its outer size.
            int outerW=wd, outerH=ht;
            if(kind.startsWith("Lock:")) {
                android.graphics.Rect pad=AppWidgetHostView.getDefaultPaddingForWidget(this,cn,null);
                outerW += Math.round((pad.left+pad.right)/d);
                outerH += Math.round((pad.top+pad.bottom)/d);
            }
            v.updateAppWidgetSize(null, outerW, outerH, outerW, outerH);
            col.addView(v, new LinearLayout.LayoutParams((int)(outerW * d), (int)(outerH * d)));
            kinds.add(kind);
            views.add(v);
            Log.i("WidgetRecoveryAudit","bound kind="+kind+" id="+id+" ok="+ok);
            if(kind.equals(getIntent().getStringExtra("tapKind"))) new Handler(Looper.getMainLooper()).postDelayed(()->{
                String viewId=kind.startsWith("Lock:")?"lock_widget_art":"home_prayer_image";
                android.view.View target=v.findViewById(foreignId(viewId));
                Log.i("WidgetRecoveryAudit","tap kind="+kind+" found="+(target!=null)+" clicked="+(target!=null&&target.performClick()));
            },9000);

        }
        setContentView(sv);
        registerReceiver(new android.content.BroadcastReceiver(){public void onReceive(android.content.Context c,android.content.Intent in){
            String kind=in.getStringExtra("kind");
            for(int z=0;z<kinds.size();z++)if(kinds.get(z).equals(kind)){
                String key=kind.startsWith("Lock:")?"lock_widget_art":"home_prayer_image";
                android.view.View target=clickableChild(views.get(z));
                Log.i("WidgetRecoveryAudit","tap kind="+kind+" found="+(target!=null)+" clicked="+(target!=null&&target.performClick()));
            }
        }},new android.content.IntentFilter("com.example.widgethost.CLICK"),android.content.Context.RECEIVER_EXPORTED);
        // After updates settle, dump per-widget pixel stats: a blank/never-updated
        // widget (the R8 regression) is a uniform surface -> std near 0.
        new Handler(Looper.getMainLooper()).postDelayed(this::dumpPixelStats, 6000);
        new Handler(Looper.getMainLooper()).postDelayed(this::dumpPixelStats, 11000);
    }

    private android.view.View clickableChild(android.view.View v) {
        if(v.hasOnClickListeners())return v;
        if(v instanceof android.view.ViewGroup){android.view.ViewGroup g=(android.view.ViewGroup)v;for(int i=0;i<g.getChildCount();i++){android.view.View x=clickableChild(g.getChildAt(i));if(x!=null)return x;}}
        return null;
    }
    private int foreignId(String name) { try { return getPackageManager().getResourcesForApplication(PKG).getIdentifier(name,"id",PKG); } catch(Exception e) { return 0; } }

    private void dumpNativeTimers(android.view.View view, String kind) {
        if (view instanceof android.widget.Chronometer) {
            android.widget.Chronometer c = (android.widget.Chronometer)view;
            Log.i("NativeTimerAudit", "kind="+kind+" visible="+c.isShown()+" countdown="+c.isCountDown()+" base="+c.getBase()+" text="+c.getText());
        }
        if (view instanceof android.view.ViewGroup) {
            android.view.ViewGroup g=(android.view.ViewGroup)view;
            for(int i=0;i<g.getChildCount();i++)dumpNativeTimers(g.getChildAt(i),kind);
        }
    }

    private void dumpPixelStats() {
        for (int i = 0; i < views.size(); i++) {
            AppWidgetHostView v = views.get(i);
            String kind = kinds.get(i);
            dumpNativeTimers(v, kind);
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
