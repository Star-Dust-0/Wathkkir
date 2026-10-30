package com.example.lockwidgets;

import java.util.Collection;
import android.content.SharedPreferences;
import android.content.res.Resources;
import android.os.BaseBundle;
import java.util.Map;
import java.util.List;
import java.util.LinkedHashMap;
import java.util.Iterator;
import java.util.HashMap;
import com.example.fajr.data.FajrPreferences;
import android.text.TextDirectionHeuristics;
import android.text.TextDirectionHeuristic;
import android.text.Layout;
import android.os.ParcelFileDescriptor;
import android.os.Bundle;
import android.os.Build;
import android.icu.util.IslamicCalendar;
import android.graphics.drawable.Drawable;
import android.graphics.RadialGradient;
import android.graphics.BitmapFactory;
import android.content.ComponentName;
import android.appwidget.AppWidgetManager;
import android.app.WallpaperColors;
import android.app.PendingIntent;
import android.app.WallpaperManager;
import android.content.Context;
import android.content.Intent;
import android.content.res.AssetManager;
import android.graphics.Bitmap;
import android.graphics.BitmapShader;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Rect;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Shader.TileMode;
import android.graphics.Bitmap.Config;
import android.text.Layout.Alignment;
import android.graphics.Typeface;
import android.os.SystemClock;
import android.text.StaticLayout;
import android.text.TextPaint;
import android.util.DisplayMetrics;
import android.util.Log;
import android.util.SizeF;
import android.widget.RemoteViews;
import com.example.widget.WidgetRefreshReceiver;
import com.example.ui.i18n.L10n;

import com.example.utils.Str;
import com.example.ui.i18n.S;
import com.example.utils.Digits;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.Date;
import java.util.HashSet;
import java.util.Locale;
import java.util.Set;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class HomeWidgets {
    static volatile Typeface boldFace = null;
    public static boolean openPrayer = false;
    static volatile Typeface regularFace;
    static Bitmap wpLight;
    static Bitmap wpMesh;
    static Bitmap wpReal;
    static final ThreadLocal<State> batchState = new ThreadLocal<>();
    static final ThreadLocal<Boolean> ovWanted = new ThreadLocal<>();
    static final ThreadLocal<String> ovToken = new ThreadLocal<>();
    static final ThreadLocal<String> langForNative = new ThreadLocal<>();
    static final ThreadLocal<Long> ovBase = new ThreadLocal<>();
    static final ThreadLocal<Boolean> ovSemi = new ThreadLocal<>();
    static final ThreadLocal<float[]> ovGeo = new ThreadLocal<>();
    static final ThreadLocal<String> ovFmt = new ThreadLocal<>();
    static final Set<String> REFRESH_KINDS = new HashSet(Arrays.asList("Hero", "NextPrayer", "PrayerBar", "Clock", "Pill", "OneUI"));
    static final ThreadLocal<Float> osCap = new ThreadLocal<>();
    static int wpId = -9;
    static Bitmap wpBlur = null;
    static int wpMeshId = -2;
    static int wpRealId = -2;
    static int wpLightId = -2;

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static final class State {
        long at;
        String city;
        String code;
        String day;
        String gday;
        String gmonth;
        String greg;
        String gyear;
        String hday;
        String hijri;
        String hmonth;
        String hyear;
        boolean iqama;
        String[] names;
        int next;
        long now = System.currentTimeMillis();
        long prev;
        int previous;
        boolean rtl;
        String targetName;
        long[] ts;
    }

    public static String adhanWord(String str) {
        return str.startsWith("ar") ? "أذان" : str.startsWith("ur") ? "اذان" : "Adhan";
    }

    public static boolean ar(Context context) {
        return lang(context).startsWith("ar");
    }

    public static int avgColor(Bitmap bitmap) {
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        long j = 0;
        long j2 = 0;
        long j3 = 0;
        int i = 0;
        for (int i2 = 0; i2 < height; i2 += 2) {
            for (int i3 = 0; i3 < width; i3 += 2) {
                int pixel = bitmap.getPixel(i3, i2);
                j += (pixel >> 16) & 255;
                j2 += (pixel >> 8) & 255;
                j3 += pixel & 255;
                i++;
            }
        }
        long j4 = i;
        return (((int) (j / j4)) << 16) | (((int) (j2 / j4)) << 8) | ((int) (j3 / j4));
    }

    public static void bar(Canvas canvas, Paint paint, float f, float f2, float f3, float f4, float f5, float f6, int[] iArr, int i) {
        float f7;
        float f8;
        paint.setShader(null);
        paint.setColor(i);
        float f9 = f * f6;
        float f10 = f2 * f6;
        float f11 = (f2 + f4) * f6;
        float f12 = (f4 * f6) / 2.0f;
        canvas.drawRoundRect(new RectF(f9, f10, (f3 - f) * f6, f11), f12, f12, paint);
        float max = (Math.max(f4, Math.max(0.0f, Math.min(1.0f, f5)) * (f3 - (2.0f * f))) + f) * f6;
        int i2 = iArr[0];
        if (i2 != iArr[1]) {
            f8 = f9;
            f7 = max;
            paint.setShader(new LinearGradient(f8, 0.0f, max, 0.0f, iArr[0], iArr[1], Shader.TileMode.CLAMP));
        } else {
            f7 = max;
            f8 = f9;
            paint.setColor(i2);
        }
        canvas.drawRoundRect(new RectF(f8, f10, f7, f11), f12, f12, paint);
        paint.setShader(null);
    }

    public static int[] barFillColors(Context context, int i) {
        String str = null;
        if (i > 0) {
            str = context.getSharedPreferences("fajr_prefs", 0).getString("widget_bar_color_" + i, null);
        }
        if (str == null) {
            str = context.getSharedPreferences("fajr_prefs", 0).getString("widget_bar_color", "auto");
        }
        if ("amber".equals(str)) {
            return new int[]{-1723307, -1723307};
        }
        if ("blue".equals(str)) {
            return new int[]{-12476161, -12876802};
        }
        if ("green".equals(str)) {
            return new int[]{-10761904, -14367394};
        }
        if ("purple".equals(str)) {
            return new int[]{-6862849, -6862849};
        }
        int i2 = isLight(bgColor(context, i)) ? -16777216 : -1;
        return new int[]{i2, i2};
    }

    public static int bgColor(Context context, int i) {
        String str = null;
        if (i > 0) {
            str = context.getSharedPreferences("fajr_prefs", 0).getString("widget_bg_color_" + i, null);
        }
        if (str == null) {
            str = context.getSharedPreferences("fajr_prefs", 0).getString("widget_bg_color", "auto");
        }
        int i2 = "black".equals(str) ? 0 : "gray".equals(str) ? 2829099 : (!"white".equals(str) && (context.getResources().getConfiguration().uiMode & 48) == 32) ? 1513241 : 16185078;
        if ("glass".equals(str)) {
            return (context.getResources().getConfiguration().uiMode & 48) == 32 ? -1877730782 : -1712131337;
        }
        if (!"wallblur".equals(str) && !"frosted".equals(str)) {
            return i2 | (-654311424);
        }
        Bitmap wallBlur = wallBlur(context, "wallblur".equals(str));
        if (wallBlur != null) {
            return blendOver(avgColor(wallBlur), nightMode(context) ? -1206642142 : -1191774474) | (-16777216);
        }
        return ((context.getResources().getConfiguration().uiMode & 48) == 32 ? 1513241 : 16185078) | (-654311424);
    }

    public static long bitmapBudget(Context context) {
        DisplayMetrics displayMetrics = context.getResources().getDisplayMetrics();
        return (long) (displayMetrics.widthPixels * 3.0d * displayMetrics.heightPixels);
    }

    public static int blendOver(int i, int i2) {
        int i3 = i2 >>> 24;
        int i4 = 255 - i3;
        int i5 = ((((i2 >> 16) & 255) * i3) + (((i >> 16) & 255) * i4)) / 255;
        int i6 = ((((i2 >> 8) & 255) * i3) + (((i >> 8) & 255) * i4)) / 255;
        return (i5 << 16) | (i6 << 8) | ((((i2 & 255) * i3) + ((i & 255) * i4)) / 255);
    }

    public static void boxBlur(Bitmap bitmap, int i) {
        char c;
        char c2;
        char c3;
        char c4;
        char c5;
        char c6;
        int width = bitmap.getWidth();
        int height = bitmap.getHeight();
        int i2 = width * height;
        int[] iArr = new int[i2];
        bitmap.getPixels(iArr, 0, width, 0, 0, width, height);
        int[] iArr2 = new int[i2];
        int i3 = 0;
        while (true) {
            c = '\b';
            c2 = 16;
            c3 = 24;
            if (i3 >= height) {
                break;
            }
            for (int i4 = 0; i4 < width; i4++) {
                long j = 0;
                long j2 = 0;
                long j3 = 0;
                long j4 = 0;
                long j5 = 0;
                for (int i5 = -i; i5 <= i; i5++) {
                    int i6 = i4 + i5;
                    if (i6 >= 0 && i6 < width) {
                        int i7 = iArr[(i3 * width) + i6];
                        j += i7 >>> 24;
                        j3 += (i7 >> 16) & 255;
                        j4 += (i7 >> 8) & 255;
                        j5 += i7 & 255;
                        j2++;
                    }
                }
                iArr2[(i3 * width) + i4] = (int) (((j / j2) << 24) | ((j3 / j2) << 16) | ((j4 / j2) << 8) | (j5 / j2));
            }
            i3++;
        }
        for (int i8 = 0; i8 < height; i8++) {
            int i9 = 0;
            while (i9 < width) {
                int i10 = -i;
                long j6 = 0;
                long j7 = 0;
                long j8 = 0;
                long j9 = 0;
                long j10 = 0;
                while (i10 <= i) {
                    int i11 = i8 + i10;
                    if (i11 < 0 || i11 >= height) {
                        c4 = c;
                        c5 = c2;
                        c6 = c3;
                    } else {
                        int i12 = iArr2[(i11 * width) + i9];
                        c4 = c;
                        c5 = c2;
                        c6 = c3;
                        j6 += i12 >>> 24;
                        j8 += (i12 >> 16) & 255;
                        j9 += (i12 >> 8) & 255;
                        j10 += i12 & 255;
                        j7++;
                    }
                    i10++;
                    c = c4;
                    c2 = c5;
                    c3 = c6;
                }
                char c7 = c;
                char c8 = c2;
                iArr[(i8 * width) + i9] = (int) (((j6 / j7) << c3) | ((j8 / j7) << c8) | ((j9 / j7) << c7) | (j10 / j7));
                i9++;
                c = c7;
                c2 = c8;
            }
        }
        bitmap.setPixels(iArr, 0, width, 0, 0, width, height);
    }

    public static boolean capture(String str, float f, float f2, float f3, float f4, int i, int i2) {
        String str2 = ovToken.get();
        if (str2 != null) {
            ThreadLocal<float[]> threadLocal = ovGeo;
            if (threadLocal.get() == null && str != null && str.contains(str2)) {
                threadLocal.set(new float[]{f, f2, f3, f4, i, i2});
                ovFmt.set(str.replace(str2, "%s"));
                return true;
            }
        }
        return false;
    }

    public static Bitmap centerCrop(Bitmap bitmap, int i, int i2) {
        Bitmap createBitmap = Bitmap.createBitmap(i, i2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        float f = i;
        float f2 = i2;
        float max = Math.max(f / bitmap.getWidth(), f2 / bitmap.getHeight());
        float width = bitmap.getWidth() * max;
        float height = bitmap.getHeight() * max;
        canvas.drawBitmap(bitmap, (Rect) null, new RectF((f - width) / 2.0f, (f2 - height) / 2.0f, (f + width) / 2.0f, (f2 + height) / 2.0f), new Paint(2));
        return createBitmap;
    }

    public static ArrayList<SizeF> cornerSizes(int i, int i2, int i3, int i4) {
        ArrayList<SizeF> arrayList = new ArrayList<>();
        int[][] iArr = {new int[]{i, i3}, new int[]{i2, i3}, new int[]{i, i4}, new int[]{i2, i4}};
        for (int i5 = 0; i5 < 4; i5++) {
            int[] iArr2 = iArr[i5];
            SizeF sizeF = new SizeF(iArr2[0], iArr2[1]);
            if (!arrayList.contains(sizeF)) {
                arrayList.add(sizeF);
            }
        }
        return arrayList;
    }

    public static void debugOverrideMeshForReview(Context context, Bitmap bitmap) {
        int wallpaperId;
        wpMesh = bitmap;
        wpLight = null;
        wpLightId = -2;
        if (bitmap == null) {
            wpMeshId = -2;
            return;
        }
        try {
            wallpaperId = WallpaperManager.getInstance(context).getWallpaperId(1);
            wpMeshId = wallpaperId;
        } catch (Throwable unused) {
            wpMeshId = -1;
        }
    }

    public static String durText(long j, String str) {
        String str2;
        String str3;
        StringBuilder sb;
        String str4;
        String sb2;
        StringBuilder sb3;
        String str5;
        String sb4;
        long j2 = j / 60;
        long j3 = j % 60;
        if (str.startsWith("ar")) {
            if (j2 == 1) {
                sb2 = "ساعة";
            } else if (j2 == 2) {
                sb2 = "ساعتان";
            } else {
                if (j2 < 3 || j2 > 10) {
                    sb = new StringBuilder();
                    sb.append(j2);
                    str4 = " ساعة";
                } else {
                    sb = new StringBuilder();
                    sb.append(j2);
                    str4 = " ساعات";
                }
                sb.append(str4);
                sb2 = sb.toString();
            }
            if (j3 == 1) {
                sb4 = "دقيقة";
            } else if (j3 == 2) {
                sb4 = "دقيقتان";
            } else {
                if (j3 < 3 || j3 > 10) {
                    sb3 = new StringBuilder();
                    sb3.append(j3);
                    str5 = " دقيقة";
                } else {
                    sb3 = new StringBuilder();
                    sb3.append(j3);
                    str5 = " دقائق";
                }
                sb3.append(str5);
                sb4 = sb3.toString();
            }
            return (j2 <= 0 || j3 <= 0) ? j2 > 0 ? sb2 : j3 > 0 ? sb4 : "أقل من دقيقة" : Str.L(sb2, " و ", sb4);
        }
        if (str.startsWith("en")) {
            if (j2 > 0 && j3 > 0) {
                return j2 + "h " + j3 + "m";
            }
            if (j2 > 0) {
                StringBuilder sb5 = new StringBuilder();
                sb5.append(j2);
                sb5.append(j2 == 1 ? " hour" : " hours");
                return sb5.toString();
            }
            if (j3 <= 0) {
                return "<1 min";
            }
            StringBuilder sb6 = new StringBuilder();
            sb6.append(j3);
            sb6.append(j3 == 1 ? " minute" : " minutes");
            return sb6.toString();
        }
        if (str.startsWith("de")) {
            str2 = "Std.";
            str3 = "Min.";
        } else if (str.startsWith("tr")) {
            str2 = "sa";
            str3 = "dk";
        } else if (str.startsWith("id")) {
            str2 = "jam";
            str3 = "mnt";
        } else if (str.startsWith("ru")) {
            str2 = "ч";
            str3 = "мин";
        } else if (str.startsWith("ur")) {
            str2 = "گھنٹے";
            str3 = "منٹ";
        } else {
            str2 = "h";
            str3 = "min";
        }
        if (j2 > 0 && j3 > 0) {
            return j2 + " " + str2 + " " + j3 + " " + str3;
        }
        if (j2 > 0) {
            return j2 + " " + str2;
        }
        if (j3 <= 0) {
            return "<1 ".concat(str3);
        }
        return j3 + " " + str3;
    }

    public static Typeface face(Context context, boolean z, boolean z2) {
        if (!z2) {
            return Typeface.create(z ? "sans-serif" : "sans-serif-medium", z ? 1 : 0);
        }
        Typeface typeface = z ? boldFace : regularFace;
        if (typeface != null) {
            return typeface;
        }
        AssetManager assets = context.getAssets();
        StringBuilder sb = new StringBuilder("homefonts/expoarabic_");
        sb.append(z ? "semibold" : "medium");
        sb.append(".ttf");
        Typeface createFromAsset = Typeface.createFromAsset(assets, sb.toString());
        if (z) {
            boldFace = createFromAsset;
            return createFromAsset;
        }
        regularFace = createFromAsset;
        return createFromAsset;
    }

    public static float fitSize(String str, Typeface typeface, float f, float f2, float f3, int i) {
        try {
            String str2 = ovToken.get();
            if (str2 != null) {
                str = str.replace(str2, "00:00:00");
            }
            String b = Digits.forWidget(str);
            TextPaint textPaint = new TextPaint(1);
            textPaint.setTypeface(typeface);
            textPaint.setTextSize(f * f3);
            if (i == 0) {
                while (textPaint.measureText(b) > Math.max(1.0f, f2 - 2.0f) * f3 && textPaint.getTextSize() > f3 * 6.0f) {
                    textPaint.setTextSize(textPaint.getTextSize() - (0.25f * f3));
                }
                int i2 = (int) (f2 * f3);
                for (StaticLayout.Builder maxLines = StaticLayout.Builder.obtain(b, 0, b.length(), textPaint, Math.max(1, i2)).setIncludePad(false).setMaxLines(2); maxLines.build().getLineCount() > 1 && textPaint.getTextSize() > f3 * 6.0f; maxLines = StaticLayout.Builder.obtain(b, 0, b.length(), textPaint, Math.max(1, i2)).setIncludePad(false).setMaxLines(2)) {
                    textPaint.setTextSize(textPaint.getTextSize() - (f3 * 0.25f));
                }
            } else if (i == 1) {
                while (textPaint.measureText(b) > f2 * f3 && textPaint.getTextSize() > f3 * 8.0f * Math.min(1.0f, f / 12.0f)) {
                    textPaint.setTextSize(textPaint.getTextSize() - (f3 * 0.5f));
                }
            } else {
                while (textPaint.measureText(b) > f2 * f3 && textPaint.getTextSize() > f3 * 8.0f) {
                    textPaint.setTextSize(textPaint.getTextSize() - (f3 * 0.5f));
                }
            }
            return textPaint.getTextSize();
        } catch (Throwable unused) {
            return f * f3;
        }
    }

    public static boolean handleIntent(Intent intent) {
        boolean z = intent != null && "com.wathakker.locktest.OPEN_PRAYER".equals(intent.getAction());
        openPrayer = z;
        return z;
    }

    /* JADX WARN: Code restructure failed: missing block: B:31:0x00ae, code lost:
    
        if (r22 == android.text.Layout.Alignment.ALIGN_OPPOSITE) goto L34;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void heroText(Canvas canvas, String str, float f, float f2, float f3, float f4, Typeface typeface, int i, Layout.Alignment alignment, float f5) {
        Typeface typeface2;
        float f6;
        Layout.Alignment alignment2;
        float f7;
        int i2;
        ThreadLocal threadLocal = ovToken;
        if (threadLocal.get() == null || str == null || !str.contains((CharSequence) threadLocal.get())) {
            typeface2 = typeface;
        } else {
            float f8 = ((f3 / 2.0f) + f) * f5;
            float f9 = f2 * f5;
            float f10 = f3 * f5;
            float fitSize = fitSize(str, typeface, f4, f3, f5, 1);
            typeface2 = typeface;
            if (typeface == boldFace || typeface.isBold()) {
                f7 = f8;
                i2 = 1;
            } else {
                f7 = f8;
                i2 = 0;
            }
            if (capture(str, f7, f9, f10, fitSize, i, i2)) {
                return;
            }
        }
        String b = Digits.forWidget(str);
        TextPaint textPaint = new TextPaint(1);
        textPaint.setTypeface(typeface2);
        textPaint.setTextSize(f4 * f5);
        textPaint.setColor(i);
        while (true) {
            float measureText = textPaint.measureText(b);
            f6 = f3 * f5;
            if (measureText <= f6) {
                break;
            }
            if (textPaint.getTextSize() <= Math.min(1.0f, f4 / 12.0f) * 8.0f * f5) {
                break;
            } else {
                textPaint.setTextSize(textPaint.getTextSize() - (0.5f * f5));
            }
        }
        TextDirectionHeuristic textDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR;
        if (textDirectionHeuristic.isRtl(b, 0, b.length())) {
            alignment2 = Layout.Alignment.ALIGN_NORMAL;
            if (alignment == alignment2) {
                alignment2 = Layout.Alignment.ALIGN_OPPOSITE;
            }
            StaticLayout build = StaticLayout.Builder.obtain(b, 0, b.length(), textPaint, Math.max(1, (int) f6)).setAlignment(alignment2).setTextDirection(textDirectionHeuristic).setIncludePad(false).setMaxLines(1).build();
            canvas.save();
            canvas.translate(f * f5, (f2 * f5) - (build.getHeight() / 2.0f));
            build.draw(canvas);
            canvas.restore();
        }
        alignment2 = alignment;
        StaticLayout build2 = StaticLayout.Builder.obtain(b, 0, b.length(), textPaint, Math.max(1, (int) f6)).setAlignment(alignment2).setTextDirection(textDirectionHeuristic).setIncludePad(false).setMaxLines(1).build();
        canvas.save();
        canvas.translate(f * f5, (f2 * f5) - (build2.getHeight() / 2.0f));
        build2.draw(canvas);
        canvas.restore();
    }

    public static int initialPage() {
        return 0;
    }

    public static boolean isLight(int i) {
        return (((double) (i & 255)) * 0.114d) + ((((double) ((i >> 8) & 255)) * 0.587d) + (((double) ((i >> 16) & 255)) * 0.299d)) > 170.0d;
    }

    public static String lang(Context context) {
        String string = context.getSharedPreferences("app_prefs", 0).getString("language", "ar");
        return string == null ? "ar" : string;
    }

    public static boolean large(Context context) {
        return context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
    }

    /* JADX WARN: Removed duplicated region for block: B:22:0x0049  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static Bitmap lightWallBlur(Context context) {
        int i;
        Bitmap bitmap;
        Drawable drawable;
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        try {
            i = wallpaperManager.getWallpaperId(1);
        } catch (Throwable unused) {
            i = -1;
        }
        Bitmap bitmap2 = wpLight;
        if (bitmap2 != null && i == wpLightId) {
            return bitmap2;
        }
        try {
            drawable = wallpaperManager.getDrawable();
        } catch (Throwable unused2) {
            drawable = null;
        }
        if (drawable != null) {
            int intrinsicWidth = drawable.getIntrinsicWidth();
            int intrinsicHeight = drawable.getIntrinsicHeight();
            if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
                intrinsicWidth = 720;
                intrinsicHeight = 1280;
            }
            int max = Math.max(1, (intrinsicHeight * 96) / intrinsicWidth);
            bitmap = Bitmap.createBitmap(96, max, Bitmap.Config.ARGB_8888);
            try {
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, 96, max);
                drawable.draw(canvas);
            } catch (Throwable unused3) {
            }
            if (bitmap == null) {
                Bitmap wallBlur = wallBlur(context, false);
                if (wallBlur == null) {
                    return null;
                }
                bitmap = Bitmap.createScaledBitmap(wallBlur, 96, 64, true);
            }
            boxBlur(bitmap, 2);
            wpLight = bitmap;
            wpLightId = i;
            return bitmap;
        }
        bitmap = null;
        if (bitmap == null) {
        }
        boxBlur(bitmap, 2);
        wpLight = bitmap;
        wpLightId = i;
        return bitmap;
    }

    public static String mmss(long j) {
        long max = Math.max(0L, j / 1000);
        return String.format(Locale.ENGLISH, "%02d:%02d", Long.valueOf(max / 60), Long.valueOf(max % 60));
    }

    public static boolean nightMode(Context context) {
        return (context.getResources().getConfiguration().uiMode & 48) == 32;
    }

    public static PendingIntent open(Context context, boolean z) {
        return PendingIntent.getActivity(context, z ? 91212 : 91213, new Intent().setClassName(context.getPackageName(), "com.example.MainActivity").setAction(z ? "com.wathakker.locktest.OPEN_PRAYER" : "android.intent.action.MAIN").setFlags(603979776), 201326592);
    }

    /* JADX WARN: Code restructure failed: missing block: B:34:0x00a0, code lost:
    
        if (r7 > 0.0f) goto L35;
     */
    /* JADX WARN: Removed duplicated region for block: B:38:0x00d0  */
    /* JADX WARN: Removed duplicated region for block: B:41:0x00da  */
    /* JADX WARN: Removed duplicated region for block: B:45:0x00db  */
    /* JADX WARN: Removed duplicated region for block: B:46:0x00d3  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    // Reconstructed from the original 3.4.2 bytecode. Native countdowns must
    // be cleared after every render, including the exception path.
    public static void ovApply(Context context, RemoteViews views, int width, int height) {
        try {
            float[] geo = ovGeo.get();
            String format = ovFmt.get();
            Long base = ovBase.get();
            if (geo != null && format != null && base != null && Build.VERSION.SDK_INT >= 24) {
                int stub = res(context, "id", "ov_stub");
                if (large(context) && geo.length > 5 && geo[5] >= 0 &&
                        Build.VERSION.SDK_INT >= 26 && width > 0 && height > 0 && stub != 0) {
                    ovFrac(context, views, geo, format, base, width, height);
                    return;
                }
                if (stub != 0) views.setViewVisibility(stub, 8);
                boolean semi = geo.length > 5 && geo[5] >= 0 ? geo[5] > 0 :
                        geo[3] >= context.getResources().getDisplayMetrics().density * 20 || Boolean.TRUE.equals(ovSemi.get());
                int box = res(context, "id", "ov_box");
                int active = res(context, "id", semi ? "ov_chrono_semi" : "ov_chrono_med");
                int inactive = res(context, "id", semi ? "ov_chrono_med" : "ov_chrono_semi");
                views.setViewVisibility(box, 0);
                views.setViewVisibility(inactive, 8);
                views.setViewVisibility(active, 0);
                // Native durations may include hours. Fit the full numeric format, never wrap
                // into a second line below small widgets (Pill and OneUI especially).
                Paint nativePaint = new Paint(Paint.ANTI_ALIAS_FLAG);
                nativePaint.setTypeface(face(context, semi, true));
                float nativeSize = geo[3];
                nativePaint.setTextSize(nativeSize);
                String sample = format.replace("%s", "00:00:00");
                while(nativePaint.measureText(sample) > geo[2] && nativeSize > 6f) {
                    nativeSize -= .5f; nativePaint.setTextSize(nativeSize);
                }
                Paint.FontMetrics metrics=nativePaint.getFontMetrics();
                views.setViewPadding(box, Math.max(0, Math.round(geo[0] - geo[2] / 2)),
                        Math.max(0, Math.round(geo[1] - (metrics.descent-metrics.ascent)/2)),
                        Math.max(0, Math.round(width - geo[0] - geo[2] / 2)), 0);
                views.setTextViewTextSize(active, 0, nativeSize);
                views.setBoolean(active, "setSingleLine", true);
                views.setTextColor(active, (int) geo[4]);
                views.setChronometerCountDown(active, true);
                views.setChronometer(active, base, format, true);
            } else {
                int stub = res(context, "id", "ov_stub");
                int box = res(context, "id", "ov_box");
                if (stub != 0) views.setViewVisibility(stub, 8);
                if (box != 0) views.setViewVisibility(box, 8);
                for (String name : new String[]{"ov_chrono_med", "ov_chrono_semi"}) {
                    int id = res(context, "id", name);
                    if (id == 0) continue;
                    views.setViewVisibility(id, 8);
                    try { views.setChronometer(id, SystemClock.elapsedRealtime(), null, false); }
                    catch (Throwable ignored) { }
                }
            }
        } catch (Throwable failure) {
            Log.w("WathakkerHome", "overlay failed", failure);
        } finally {
            ovSemi.remove(); ovWanted.remove(); ovToken.remove();
            ovBase.remove(); ovGeo.remove(); ovFmt.remove(); langForNative.remove();
        }
    }

    public static void ovArm(boolean z, long j, long j2) {
        if (ovWanted.get() == null) {
            return;
        }
        // Native-first deviation: arm before prayer too. The bitmap text is only a
        // geometry token; RemoteViews replaces it with a host-ticking Chronometer.
        ovToken.set(z ? mmss(j - j2) : durText(Math.max(0L, (j - j2) / 60000L), langForNative.get() == null ? "en" : langForNative.get()));
        ovBase.set(Long.valueOf((j - System.currentTimeMillis()) + SystemClock.elapsedRealtime()));
    }

    public static void ovBegin() {
        ovSemi.remove();
        ovWanted.set(Boolean.TRUE);
        ovToken.remove();
        ovBase.remove();
        ovGeo.remove();
        ovFmt.remove();
    }

    public static void ovBits(Context context, RemoteViews remoteViews, String str, int i) {
        int max = Math.max(0, Math.min(8191, i));
        for (int i2 = 0; i2 < 13; i2++) {
            int res = res(context, "id", str + i2);
            if (res != 0) {
                remoteViews.setViewVisibility(res, ((max >> i2) & 1) != 0 ? 0 : 8);
            }
        }
    }

    public static void ovFrac(Context context, RemoteViews remoteViews, float[] fArr, String str, long j, int i, int i2) {
        Paint paint = new Paint();
        paint.setTypeface(face(context, fArr[5] > 0.0f, true));
        paint.setTextSize(fArr[3]);
        Paint.FontMetrics fontMetrics = paint.getFontMetrics();
        float f = fontMetrics.descent - fontMetrics.ascent;
        Log.i("WathakkerHome", "ovFrac cx=" + fArr[0] + " cy=" + fArr[1] + " w=" + fArr[2] + " S=" + fArr[3] + " semi=" + fArr[5] + " lh=" + f + " W=" + i + " H=" + i2);
        boolean z = context.getResources().getConfiguration().smallestScreenWidthDp >= 600;
        float f2 = i2;
        float min = Math.min(1.0f, f / f2);
        float f3 = i;
        float min2 = Math.min(1.0f, Math.max(0.02f, fArr[2] / f3));
        float f4 = 256.0f / min;
        float f5 = 256.0f / min2;
        float f6 = (z ? fArr[1] - (f / 2.0f) : fArr[1] - (fArr[3] * 0.62f)) / f2;
        float f7 = (fArr[0] / f3) - (min2 / 2.0f);
        int round = Math.round(Math.max(0.0f, f6) * f4);
        int round2 = Math.round(Math.max(0.0f, (f4 - 256.0f) - round));
        int round3 = Math.round(Math.max(0.0f, f7) * f5);
        int round4 = Math.round(Math.max(0.0f, (f5 - 256.0f) - round3));
        ovBits(context, remoteViews, "ov_vt", round);
        ovBits(context, remoteViews, "ov_vb", round2);
        ovBits(context, remoteViews, "ov_hl", round3);
        ovBits(context, remoteViews, "ov_hr", round4);
        boolean z2 = fArr[5] > 0.0f;
        int res = res(context, "id", z2 ? "ov_f_semi" : "ov_f_med");
        int res2 = res(context, "id", z2 ? "ov_f_med" : "ov_f_semi");
        int res3 = res(context, "id", "ov_stub");
        int res4 = res(context, "id", "ov_box");
        int res5 = res(context, "id", "ov_chrono_med");
        int res6 = res(context, "id", "ov_chrono_semi");
        if (res4 != 0) {
            remoteViews.setViewVisibility(res4, 8);
        }
        if (res5 != 0) {
            remoteViews.setViewVisibility(res5, 8);
        }
        if (res6 != 0) {
            remoteViews.setViewVisibility(res6, 8);
        }
        remoteViews.setViewVisibility(res3, 0);
        remoteViews.setViewVisibility(res2, 8);
        remoteViews.setViewVisibility(res, 0);
        remoteViews.setTextColor(res, (int) fArr[4]);
        remoteViews.setChronometerCountDown(res, true);
        remoteViews.setChronometer(res, j, str, true);
    }

    public static float oversample(Context context, int i, int i2, float f) {
        if (!large(context)) {
            return 1.0f;
        }
        Float f2 = osCap.get();
        float min = f2 != null ? Math.min(2.0f, f2.floatValue()) : 2.0f;
        long min2 = Math.min(6000000L, bitmapBudget(context));
        while (min > 1.0f) {
            if (i2 * f * min * i * f * min * 4.0f <= min2) {
                break;
            }
            min -= 0.25f;
        }
        return Math.max(1.0f, min);
    }

    public static void protoGlass(Context context, Canvas canvas, boolean z, float f, float f2, float f3) {
        Bitmap lightWallBlur;
        boolean nightMode = nightMode(context);
        if (z && (lightWallBlur = lightWallBlur(context)) != null) {
            Bitmap centerCrop = centerCrop(lightWallBlur, Math.max(1, (int) f), Math.max(1, (int) f2));
            Paint paint = new Paint(1);
            Shader.TileMode tileMode = Shader.TileMode.CLAMP;
            paint.setShader(new BitmapShader(centerCrop, tileMode, tileMode));
            canvas.drawRoundRect(new RectF(0.0f, 0.0f, f, f2), f3, f3, paint);
        }
        Paint paint2 = new Paint(1);
        paint2.setColor(nightMode ? -1877730782 : -1712131337);
        canvas.drawRoundRect(new RectF(0.0f, 0.0f, f, f2), f3, f3, paint2);
    }

    public static synchronized String refreshAllAndText(Context context, String str) {
        String remainingText;
        synchronized (HomeWidgets.class) {
            Digits.refresh(context);
            try {
                ThreadLocal<State> threadLocal = batchState;
                threadLocal.set(state(context));
                try {
                    updateBatch(context, false);
                    remainingText = remainingText(threadLocal.get(), str);
                    threadLocal.remove();
                    LockWidgets.schedule(context);
                    WidgetPreviews.maybeGenerate(context);
                } catch (Throwable th) {
                    batchState.remove();
                    LockWidgets.schedule(context);
                    WidgetPreviews.maybeGenerate(context);
                    throw th;
                }
            } catch (Exception e) {
                Log.e("WathakkerHome", "state() failed - tap refresh aborted", e);
                return remLabel(lang(context));
            }
        }
        return remainingText;
    }

    public static PendingIntent refreshIntent(Context context, int i) {
        return PendingIntent.getBroadcast(context, i, new Intent(context, (Class<?>) WidgetRefreshReceiver.class).setAction(WidgetRefreshReceiver.ACTION).putExtra("appWidgetId", i), 201326592);
    }

    public static String remLabel(String str) {
        return str.startsWith("ar") ? "المتبقي" : str.startsWith("en") ? "Remaining" : str.startsWith("fr") ? "Reste" : str.startsWith("de") ? "Verbleibend" : str.startsWith("es") ? "Quedan" : str.startsWith("it") ? "Rimangono" : str.startsWith("tr") ? "Kalan" : str.startsWith("ur") ? "باقی" : str.startsWith("id") ? "Tersisa" : str.startsWith("ru") ? "Осталось" : str.startsWith("pt") ? "Restam" : "Remaining";
    }

    public static String remPrefix(String str, String str2) {
        if (str.startsWith("ar")) {
            return "المتبقي : ";
        }
        return remLabel(str) + str2;
    }

    public static String remaining(long j, String str) {
        return remPrefix(str, " ") + durText(j, str);
    }

    public static String remainingText(State state, String str) {
        String remaining;
        if (state == null) {
            return "";
        }
        boolean equals = "Clock".equals(str);
        long j = state.at - state.now;
        if (equals) {
            long max = Math.max(0L, j / 1000);
            remaining = state.iqama ? String.format(Locale.ENGLISH, "%02d:%02d", Long.valueOf(max / 60), Long.valueOf(max % 60)) : String.format(Locale.ENGLISH, "%02d:%02d", Long.valueOf(max / 3600), Long.valueOf((max / 60) % 60));
        } else {
            long max2 = Math.max(0L, j / 60000);
            boolean z = state.iqama;
            String str2 = state.code;
            if (z) {
                remaining = remPrefix(str2, ": ") + "\u2066" + mmss(state.at - state.now) + "\u2069";
            } else {
                remaining = remaining(max2, str2);
            }
        }
        String b = Digits.forWidget(remaining);
        return b != null ? b : remaining;
    }

    public static Bitmap render(Context context, int i, int i2) {
        return renderKind(context, "Hero", i, i2, 0);
    }

    /*  JADX ERROR: Type inference failed
        jadx.core.utils.exceptions.JadxOverflowException: Type update terminated with stack overflow, arg: (r4v15 ??), method size: 2263
        	at jadx.core.utils.ErrorsCounter.addError(ErrorsCounter.java:59)
        	at jadx.core.utils.ErrorsCounter.error(ErrorsCounter.java:31)
        	at jadx.core.dex.attributes.nodes.NotificationAttrNode.addError(NotificationAttrNode.java:19)
        	at jadx.core.dex.visitors.typeinference.TypeInferenceVisitor.visit(TypeInferenceVisitor.java:77)
        */
   public static Bitmap renderKind(Context var0, String var1, int var2, int var3, int var4) {
      Exception var151 = null;
      Typeface var243 = null;
      State var242 = null;
      Layout.Alignment var163 = null;
      Layout.Alignment var168 = null;
      int[] var154 = null;
      String var267 = null;
      StringBuilder var159 = null;
      StringBuilder var261 = null;
      Layout.Alignment var161 = null;
      Layout.Alignment var253 = null;
      Layout.Alignment var270 = null;
      Layout.Alignment var271 = null;
      Typeface var162 = null;
      Exception var245 = null;
      String var260 = null;
      String var262 = null;
      String var266 = null;
      label1014:
      if (var1.equals("OneUI")) {
         State var34;
         try {
            var34 = batchState.get();
         } catch (Exception var150) {
            Log.e("WathakkerHome", "OneUI render failed", var150);
            break label1014;
         }

         State var33 = var34;
         if (var34 == null) {
            try {
               var33 = state(var0);
            } catch (Exception var149) {
               Log.e("WathakkerHome", "OneUI render failed", var149);
               break label1014;
            }
         }

         try {
            ovArm(var33.iqama, var33.at, var33.now);
            return com.example.lockwidgets.OneUIWidget.render(var0, var33, var2, var3, var4);
         } catch (Exception var148) {
            Log.e("WathakkerHome", "OneUI render failed", var148);
         }
      }

      float var5 = var0.getResources().getDisplayMetrics().density;
      float var6 = oversample(var0, var2, var3, var5);
      float var13 = var2;
      float var8 = var13 * var5;
      int var20 = Math.max(1, (int)(var8 * var6));
      float var11 = var3;
      float var7 = var11 * var5;
      Bitmap var39 = Bitmap.createBitmap(var20, Math.max(1, (int)(var7 * var6)), Config.ARGB_8888);
      Canvas var40 = new Canvas(var39);
      var40.scale(var6, var6);
      Paint var41 = new Paint(1);
      int var21 = bgColor(var0, var4);
      String var234;
      if (var4 > 0) {
         SharedPreferences var233 = var0.getSharedPreferences("fajr_prefs", 0);
         StringBuilder var246 = new StringBuilder("widget_bg_color_");
         var246.append(var4);
         var234 = var233.getString(var246.toString(), null);
      } else {
         var234 = null;
      }

      var6 = var5;
      String var247 = var234;
      if (var234 == null) {
         var247 = var0.getSharedPreferences("fajr_prefs", 0).getString("widget_bg_color", "auto");
      }

      float var9 = Math.min(24.0F, var11 / 4.0F) * var5;
      Bitmap var235;
      if (!"wallblur".equals(var247) && !"frosted".equals(var247)) {
         var235 = null;
      } else {
         var235 = wallBlur(var0, "wallblur".equals(var247));
      }

      if ("protoGlass1".equals(var247) || "protoGlass2".equals(var247)) {
         protoGlass(var0, var40, "protoGlass2".equals(var247), var8, var7, var9);
      } else if ("protoFrost1".equals(var247)) {
         Bitmap var236 = wallBlur(var0, false);
         if (var236 != null) {
            Bitmap var36 = centerCrop(var236, Math.max(1, (int)var8), Math.max(1, (int)var7));
            Paint var237 = new Paint(1);
            TileMode var35 = TileMode.CLAMP;
            var237.setShader(new BitmapShader(var36, var35, var35));
            var40.drawRoundRect(new RectF(0.0F, 0.0F, var8, var7), var9, var9, var237);
         }

         Paint var238 = new Paint(1);
         if (nightMode(var0)) {
            var20 = -1206642142;
         } else {
            var20 = -1191774474;
         }

         var238.setColor(var20);
         var40.drawRoundRect(new RectF(0.0F, 0.0F, var8, var7), var9, var9, var238);
      } else if (var235 != null) {
         Bitmap var257 = centerCrop(var235, Math.max(1, (int)var8), Math.max(1, (int)var7));
         Paint var268 = new Paint(1);
         TileMode var239 = TileMode.CLAMP;
         var268.setShader(new BitmapShader(var257, var239, var239));
         var40.drawRoundRect(new RectF(0.0F, 0.0F, var8, var7), var9, var9, var268);
         Paint var240 = new Paint(1);
         if (nightMode(var0)) {
            var20 = -1206642142;
         } else {
            var20 = -1191774474;
         }

         var240.setColor(var20);
         var40.drawRoundRect(new RectF(0.0F, 0.0F, var8, var7), var9, var9, var240);
      } else {
         var41.setColor(var21);
         var40.drawRoundRect(new RectF(0.0F, 0.0F, var8, var7), var9, var9, var41);
      }

      Typeface var258 = Typeface.DEFAULT;
      Typeface var241 = Typeface.DEFAULT_BOLD;
      boolean var31 = isLight(var21);
      if (var31) {
         var20 = -14935010;
      } else {
         var20 = -1;
      }

      if (var31) {
         var21 = -10263701;
      } else {
         var21 = -3552818;
      }

      int var22;
      if (var31) {
         var22 = -9539978;
      } else {
         var22 = -6908258;
      }

      int var24 = var22;
      if ("glass".equals(var247)) {
         var24 = var22;
         if (!var31) {
            var24 = -4539710;
         }
      }

      if (var31) {
         var22 = -4949474;
      } else {
         var22 = -1723307;
      }

      int var23;
      if (var31) {
         var23 = -3881779;
      } else {
         var23 = -13092802;
      }

      try {
      label986: {
         label1019: {
            
               var242 = batchState.get();
            

            label1045: {
               State var269 = var242;
               if (var242 == null) {
                  
                     var269 = state(var0);
                  
               }

               long var25;
               
                  var31 = var269.iqama;
                  var25 = var269.at;
               

               
                  ovArm(var31, var25, var269.now);
                  var243 = face(var0, false, var269.rtl);
               

               label1046: {
                  label967: {
                     label1047: {
                        label1052: {
                           Typeface var37;
                           
                              var37 = face(var0, true, var269.rtl);
                              var25 = var269.at;
                           

                           long var27;
                           
                              var27 = Math.max(0L, (var25 - var269.now) / 60000L);
                              var9 = Math.min(18.0F, var13 * 0.06F);
                           

                           float var12 = var13 - var9 * 2.0F;

                           long var29;
                           
                              var29 = var269.now;
                           

                           
                              var25 = var269.prev;
                           

                           var7 = (float)(var29 - var25);

                           
                              var29 = var269.at;
                           

                           float var14;
                           
                              var14 = var7 / (float)Math.max(1L, var29 - var25);
                              var31 = var269.iqama;
                           

                           String var38;
                           if (var31) {
                              
                                 StringBuilder var248 = new StringBuilder();
                                 var248.append(remPrefix(var269.code, ": "));
                                 var248.append("\u2066");
                                 var248.append(mmss(var269.at - var269.now));
                                 var248.append("\u2069");
                                 var38 = var248.toString();
                              
                           } else {
                              
                                 var38 = remaining(var27, var269.code);
                              
                           }

                           
                              var31 = var1.equals("Hero");
                           

                           if (var31) {
                              float var18;
                              float var196;
                              label659: {
                                 var18 = var11 * 0.389F;
                                 var196 = 0.57F * var11;
                                 if (var3 < 70) {
                                    
                                       var31 = large(var0);
                                    

                                    if (!var31) {
                                       var7 = 0.78F;
                                       break label659;
                                    }
                                 }

                                 var7 = 0.844F;
                              }

                              float var17 = var7 * var11;
                              var8 = 0.133F * var11;
                              float var202 = 0.22F * var12;

                              Alignment var274;
                              
                                 var274 = Alignment.ALIGN_CENTER;
                                 var1 = var269.targetName;
                              

                              var7 = var11 / 70.0F;

                              float var200;
                              
                                 var200 = Math.min(1.0F, var7);
                              

                              
                                 heroText(var40, var1, var9, var18, var202, var200 * 18.0F, var243, var20, var274, var6);
                                 var1 = var269.names[var269.previous];
                              

                              var200 = var13 - var9;
                              float var19 = var200 - var202;

                              
                                 heroText(var40, var1, var19, var18, var202, Math.min(1.0F, var7) * 18.0F, var243, var20, var274, var6);
                              

                              Typeface var256 = var243;
                              var3 = var20;

                              label615: {
                                 label1026: {
                                    
                                       var18 = Math.min(8.0F, var11 * 0.09F);
                                       var31 = var269.iqama;
                                    

                                    int[] var166;
                                    if (var31) {
                                       var166 = new int[]{-1723307, -1723307};
                                    } else {
                                       
                                          var166 = barFillColors(var0, var4);
                                       
                                    }

                                    
                                       bar(var40, var41, var9, var196, var13, var18, var14, var6, var166, var23);
                                    

                                    
                                       var1 = tm(var269.at, var269.code);
                                       var196 = Math.min(1.0F, var7);
                                       break label615;
                                    
                                 }

                              }

                              label599: {
                                 label1027: {
                                    
                                       heroText(var40, var1, var9, var17, var202, var196 * 18.0F, var37, var3, var274, var6);
                                       heroText(
                                          var40, tm(var269.prev, var269.code), var19, var17, var202, Math.min(1.0F, var7) * 18.0F, var37, var3, var274, var6
                                       );
                                    

                                    var196 = var12 * 0.48F;

                                    
                                       var267 = var269.hijri;
                                       var13 = Math.min(1.0F, var7);
                                       var168 = Alignment.ALIGN_NORMAL;
                                       break label599;
                                    
                                 }

                              }

                              
                                 heroText(var40, var267, var9, var8, var196, var13 * 12.0F, var256, var24, var168, var6);
                                 heroText(
                                    var40,
                                    var269.greg,
                                    var200 - var196,
                                    var8,
                                    var196,
                                    Math.min(1.0F, var7) * 12.0F,
                                    var256,
                                    var24,
                                    Alignment.ALIGN_OPPOSITE,
                                    var6
                                 );
                              

                              
                                 heroText(
                                    var40, var38, 0.24F * var12 + var9, 0.389F * var11, var12 * 0.52F, Math.min(1.0F, var7) * 15.0F, var37, var22, var274, var6
                                 );
                                 return var39;
                              
                           }

                           Typeface var249 = var243;
                           var3 = var20;
                           Context var259 = var0;

                           
                              var31 = var1.equals("NextPrayer");
                           

                           label1051: {
                              if (!var31) {
                                 
                                    if (!var1.equals("PrayerBar")) {
                                       break label1051;
                                    }
                                 
                              }

                              boolean var213 = false;
                              if (var13 < var11 * 1.45F) {
                                 var213 = true;
                              }

                              float var10;
                              if (var213) {
                                 
                                    var10 = Math.min(1.0F, var11 / 84.0F);
                                 
                              } else {
                                 var7 = var11 / 70.0F;

                                 
                                    var10 = Math.min(1.0F, var7);
                                 
                              }

                              float var15;
                              label916: {
                                 label915: {
                                    
                                       var15 = Math.min(10.0F, var11 * 0.11F);
                                       if (var1.equals("PrayerBar")) {
                                          break label915;
                                       }
                                    

                                    if (var213) {
                                       var7 = 0.12F;
                                    } else {
                                       var7 = 0.26F;
                                    }
                                    break label916;
                                 }

                                 var7 = 0.21F;
                              }

                              label909: {
                                 label908: {
                                    
                                       if (var1.equals("PrayerBar")) {
                                          break label908;
                                       }
                                    

                                    if (var213) {
                                       var8 = 0.31F;
                                    } else {
                                       var8 = 0.493F;
                                    }
                                    break label909;
                                 }

                                 var8 = 0.79F;
                              }

                              String var43;
                              
                                 var43 = var269.targetName;
                              

                              byte var210;
                              if (var213) {
                                 var210 = 13;
                              } else {
                                 var210 = 14;
                              }

                              float var16 = var210;

                              Alignment var42;
                              
                                 var42 = Alignment.ALIGN_CENTER;
                              

                              
                                 text(var40, var43, var9, var11 * var7, var12, var16 * var10, var249, var3, var42, var6);
                              

                              label843: {
                                 label842: {
                                    
                                       var43 = tm(var269.at, var269.code);
                                       if (var1.equals("PrayerBar")) {
                                          break label842;
                                       }
                                    

                                    if (var213) {
                                       var210 = 17;
                                    } else {
                                       var210 = 18;
                                    }
                                    break label843;
                                 }

                                 var210 = 16;
                              }

                              
                                 text(var40, var43, var9, var8 * var11, var12, var210 * var10, var37, var3, var42, var6);
                              

                              
                                 var31 = var1.equals("PrayerBar");
                              

                              if (var31) {
                                 label640: {
                                    
                                       if (var269.iqama) {
                                          var154 = new int[]{-1723307, -1723307};
                                          break label640;
                                       }
                                    

                                    
                                       var154 = barFillColors(var259, var4);
                                    
                                 }

                                 
                                    bar(var40, var41, var9, var11 * 0.422F, var13, var15, var14, var6, var154, var23);
                                    return var39;
                                 
                              }

                              if (!var213) {
                                 
                                    text(var40, var38, var9, var11 * 0.73F, var12, var10 * 12.0F, var249, var21, var42, var6);
                                    return var39;
                                 
                              }

                              
                                 var1 = remPrefix(var269.code, "").trim();
                              

                              var7 = var10 * 10.0F;

                              label649: {
                                 
                                    text(var40, var1, var9, var11 * 0.56F, var12, var7, var249, var21, var42, var6);
                                    if (var269.iqama) {
                                       var1 = mmss(var269.at - var269.now);
                                       break label649;
                                    }
                                 

                                 
                                    var1 = durText(var27, var269.code);
                                 
                              }

                              
                                 text(var40, var1, var9, var11 * 0.76F, var12, var7, var249, var21, var42, var6);
                                 return var39;
                              
                           }

                           
                              var31 = var1.equals("Schedule");
                           

                           if (var31) {
                              var8 = var12 / 6.0F;

                              for (int var212 = 0; var212 < 6; var212++) {
                                 label769: {
                                    label768: {
                                       
                                          if (var269.rtl) {
                                             break label768;
                                          }
                                       

                                       var7 = var212 * var8 + var9;
                                       break label769;
                                    }

                                    var7 = var13 - var9 - (var212 + 1) * var8;
                                 }

                                 
                                    var38 = var269.names[var212];
                                 

                                 var7 += 2.0F;
                                 float var195 = var8 - 4.0F;

                                 
                                    var163 = Alignment.ALIGN_CENTER;
                                 

                                 
                                    text(var40, var38, var7, var11 * 0.3F, var195, 13.0F, var249, var21, var163, var6);
                                 

                                 
                                    var38 = tm(var269.ts[var212], var269.code);
                                 

                                 
                                    text(var40, var38, var7, var11 * 0.69F, var195, 13.0F, var37, var3, var163, var6);
                                 
                              }

                              return var39;
                           }

                           
                              var31 = var1.equals("Clock");
                           

                           if (var31) {
                              float var194 = var12 * 0.23F;
                              var20 = 0;
                              var7 = var9;

                              for (var162 = var243; var20 < 2; var162 = var162) {
                                 if (var20 == 0) {
                                    var31 = true;
                                 } else {
                                    var31 = false;
                                 }

                                 boolean var32;
                                 
                                    var32 = var269.rtl;
                                 

                                 if (var31 == var32) {
                                    var8 = var13 - var7 - var194;
                                 } else {
                                    var8 = var7;
                                 }

                                 if (var20 == 0) {
                                    
                                       var234 = var269.targetName;
                                    
                                 } else {
                                    
                                       var234 = var269.names[var269.previous];
                                    
                                 }

                                 
                                    var253 = Alignment.ALIGN_CENTER;
                                 

                                 label798: {
                                    label1033: {
                                       
                                          text(var40, var234, var8, var11 * 0.28F, var194, 13.0F, var162, var21, var253, var6);
                                       

                                       if (var20 == 0) {
                                          
                                             var25 = var269.at;
                                          
                                       } else {
                                          
                                             var25 = var269.prev;
                                          
                                       }

                                       
                                          var266 = tm(var25, var269.code);
                                          break label798;
                                       
                                    }

                                 }

                                 var4 = var3;
                                 var3 = var3;

                                 
                                    text(var40, var266, var8, var11 * 0.7F, var194, 15.0F, var37, var4, var253, var6);
                                 

                                 var20++;
                                 var3 = var4;
                              }

                              var4 = var3;
                              var3 = var3;

                              
                                 var25 = Math.max(0L, (var269.at - var269.now) / 1000L);
                              

                              var3 = var4;

                              
                                 var31 = var269.iqama;
                              

                              if (var31) {
                                 var3 = var4;

                                 
                                    var247 = String.format(Locale.ENGLISH, "%02d:%02d", var25 / 60L, var25 % 60L);
                                 
                              } else {
                                 var3 = var4;

                                 
                                    var247 = ovToken.get() != null ? ovToken.get() : String.format(Locale.ENGLISH, "%02d:%02d", var25 / 3600L, var25 / 60L % 60L);
                                 
                              }

                              var3 = var4;

                              
                                 text(var40, var247, var7 + var194, var11 * 0.5F, var12 - var194 * 2.0F, 26.0F, var37, var4, Alignment.ALIGN_CENTER, var6);
                                 return var39;
                              
                           }

                           
                              var31 = var1.equals("DayPrayer");
                           

                           if (var31) {
                              
                                 var262 = var269.day;
                                 var7 = Math.min(1.0F, var11 / 60.0F);
                                 var161 = Alignment.ALIGN_CENTER;
                              

                              
                                 text(var40, var262, var9, var11 * 0.33F, var12, var7 * 20.0F, var249, var3, var161, var6);
                              

                              
                                 StringBuilder var263 = new StringBuilder();
                                 var263.append(var269.targetName);
                                 var263.append(" ");
                                 var263.append(tm(var269.at, var269.code));
                                 text(var40, var263.toString(), var9, var11 * 0.7F, var12, Math.min(1.0F, var11 / 70.0F) * 14.0F, var249, var21, var161, var6);
                                 return var39;
                              
                           }

                           
                              var31 = var1.equals("Pill");
                           

                           if (var31) {
                              label1037: {
                                 label1038: {
                                    
                                       if (var269.iqama) {
                                          break label1038;
                                       }
                                    

                                    label1039: {
                                       label1040: {
                                          
                                             if (!var269.rtl) {
                                                break label1040;
                                             }
                                          

                                          
                                             var159 = new StringBuilder();
                                          

                                          
                                             ;
                                          

                                          
                                             var159.append(adhanWord(var269.code));
                                          

                                          
                                             var159.append(" ");
                                          

                                          
                                             var159.append(var269.targetName);
                                             break label1039;
                                          
                                       }

                                       
                                          var159 = new StringBuilder();
                                       

                                       
                                          ;
                                       

                                       
                                          var159.append(var269.targetName);
                                       

                                       
                                          var159.append(" ");
                                       

                                       
                                          var159.append(adhanWord(var269.code));
                                       
                                    }

                                    
                                       var1 = var159.toString();
                                       break label1037;
                                    
                                 }

                                 
                                    var1 = var269.targetName;
                                 
                              }

                              
                                 var7 = Math.min(1.0F, var11 / 60.0F);
                              

                              
                                 var270 = Alignment.ALIGN_CENTER;
                              

                              
                                 text(var40, var1, var9, var11 * 0.32F, var12, var7 * 15.0F, var249, var3, var270, var6);
                              

                              var7 = var11 / 70.0F;

                              
                                 text(var40, var38, var9, var11 * 0.68F, var12, Math.min(1.0F, var7) * 12.0F, var249, var21, var270, var6);
                                 return var39;
                              
                           }

                           
                              var31 = var1.equals("Hijri");
                           

                           if (var31) {
                              
                                 var1 = var269.hday;
                              
                           } else {
                              
                                 var1 = var269.gday;
                              
                           }

                           var7 = var11 / 70.0F;

                           
                              var8 = Math.min(1.0F, var7);
                              var271 = Alignment.ALIGN_CENTER;
                           

                           var4 = var20;

                           
                              text(var40, var1, var9, var11 * 0.25F, var12, var8 * 26.0F, var37, var4, var271, var6);
                           

                           if (var31) {
                              var3 = var20;

                              
                                 var260 = var269.hmonth;
                              

                              var1 = var260;
                           } else {
                              
                                 var1 = var269.gmonth;
                              
                           }

                           
                              var8 = Math.min(1.0F, var7);
                           

                           
                              text(var40, var1, var9, var11 * 0.55F, var12, var8 * 14.0F, var249, var4, var271, var6);
                           

                           if (var31) {
                              label728: {
                                 label727: {
                                    
                                       var261 = new StringBuilder();
                                       var261.append(var269.hyear);
                                       if (var269.code.startsWith("ar")) {
                                          break label727;
                                       }
                                    

                                    var1 = " AH";
                                    break label728;
                                 }

                                 var1 = " هـ";
                              }

                              
                                 var261.append(var1);
                                 var1 = var261.toString();
                              
                           } else {
                              
                                 var1 = var269.gyear;
                              
                           }

                           
                              text(var40, var1, var9, var11 * 0.8F, var12, Math.min(1.0F, var7) * 12.0F, var249, var21, var271, var6);
                              return var39;
                           
                        }

                     }

                  }

               }

            }

         }

      }

      } catch (Exception varEx) {
         var151 = varEx;
         var3 = var20;
      }

      Log.e("WathakkerHome", "Render failed", var151);
      text(var40, com.example.lockwidgets.LockWidgets.errText(lang(var0)), 5.0F, var11 / 2.0F, var2 - 10, 12.0F, var243, var3, Alignment.ALIGN_CENTER, var5);
      return var39;
   }

    public static int res(Context context, String str, String str2) {
        return context.getResources().getIdentifier(str2, str, context.getPackageName());
    }

    public static boolean rtl(Context context) {
        String lang = lang(context);
        return lang.startsWith("ar") || lang.startsWith("ur");
    }

    public static SizeF slackKey(SizeF sizeF) {
        return new SizeF(Math.max(1.0f, sizeF.getWidth() - 3.0f), Math.max(1.0f, sizeF.getHeight() - 3.0f));
    }

    /* JADX WARN: Removed duplicated region for block: B:18:0x00e8  */
    /* JADX WARN: Removed duplicated region for block: B:21:0x016c  */
    /* JADX WARN: Removed duplicated region for block: B:25:0x00eb  */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static State state(Context context) {
        long j;
        int i;
        State state = new State();
        state.code = lang(context);
        state.rtl = rtl(context);
        L10n.INSTANCE.init(context);
        Locale locale = new Locale(state.code);
        Calendar calendar = Calendar.getInstance();
        state.ts = times(context, calendar);
        state.names = new String[]{L10n.INSTANCE.t(S.PrayerFajr), L10n.INSTANCE.t(S.PrayerShuruq), L10n.INSTANCE.t(S.PrayerDhuhr), L10n.INSTANCE.t(S.PrayerAsr), L10n.INSTANCE.t(S.PrayerMaghrib), L10n.INSTANCE.t(S.PrayerIsha)};
        if (calendar.get(7) == 6) {
            state.names[2] = L10n.INSTANCE.t(S.PrayerJumuah);
        }
        state.next = -1;
        int i2 = 0;
        while (true) {
            if (i2 < 6) {
                if (i2 != 1 && state.ts[i2] > state.now) {
                    state.next = i2;
                    break;
                }
                i2++;
            } else {
                break;
            }
        }
        int i3 = state.next;
        if (i3 < 0) {
            Calendar calendar2 = (Calendar) calendar.clone();
            calendar2.add(5, 1);
            state.at = times(context, calendar2)[0];
            state.next = 0;
            j = state.ts[5];
        } else {
            long[] jArr = state.ts;
            state.at = jArr[i3];
            int i4 = i3 == 2 ? 0 : i3 - 1;
            state.previous = i4;
            if (i3 != 0) {
                state.prev = jArr[i4];
                Calendar calendar3 = (Calendar) calendar.clone();
                calendar3.add(5, -1);
                Calendar calendar4 = (Calendar) calendar.clone();
                calendar4.add(5, 1);
                PrayerPhase resolve = PrayerPhase.resolve(state.ts, times(context, calendar3), times(context, calendar4), state.now, calendar.get(7) != 6, false);
                state.next = resolve.targetIndex;
                state.previous = resolve.previousIndex;
                state.at = resolve.target;
                state.prev = resolve.start;
                state.iqama = resolve.iqama;
                state.targetName = resolve.label(state.names, state.code);
                state.city = cityName(context, state.code);
                state.day = new SimpleDateFormat("EEEE", locale).format(new Date());
                IslamicCalendar islamicCalendar = new IslamicCalendar();
                islamicCalendar.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
                i = context.getSharedPreferences("fajr_prefs", 0).getInt("hijri_offset", 0);
                if (i != 0) {
                    islamicCalendar.add(5, i);
                }
                state.hday = "" + islamicCalendar.get(5);
                state.hyear = "" + islamicCalendar.get(1);
                state.hmonth = com.example.prayer.HijriDate.INSTANCE.monthName(islamicCalendar.get(2) + 1, state.code);
                state.hijri = state.hday + " " + state.hmonth;
                state.greg = new SimpleDateFormat("d MMMM", locale).format(new Date());
                state.gday = new SimpleDateFormat("d", Locale.ENGLISH).format(new Date());
                state.gmonth = new SimpleDateFormat("MMMM", locale).format(new Date());
                state.gyear = "" + calendar.get(1);
                return state;
            }
            Calendar calendar5 = (Calendar) calendar.clone();
            calendar5.add(5, -1);
            j = times(context, calendar5)[5];
        }
        state.prev = j;
        state.previous = 5;
        Calendar calendar32 = (Calendar) calendar.clone();
        calendar32.add(5, -1);
        Calendar calendar42 = (Calendar) calendar.clone();
        calendar42.add(5, 1);
        PrayerPhase resolve2 = PrayerPhase.resolve(state.ts, times(context, calendar32), times(context, calendar42), state.now, calendar.get(7) != 6, false);
        state.next = resolve2.targetIndex;
        state.previous = resolve2.previousIndex;
        state.at = resolve2.target;
        state.prev = resolve2.start;
        state.iqama = resolve2.iqama;
        state.targetName = resolve2.label(state.names, state.code);
        state.city = cityName(context, state.code);
        state.day = new SimpleDateFormat("EEEE", locale).format(new Date());
        IslamicCalendar islamicCalendar2 = new IslamicCalendar();
        islamicCalendar2.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA);
        i = context.getSharedPreferences("fajr_prefs", 0).getInt("hijri_offset", 0);
        if (i != 0) {
        }
        state.hday = "" + islamicCalendar2.get(5);
        state.hyear = "" + islamicCalendar2.get(1);
        state.hmonth = com.example.prayer.HijriDate.INSTANCE.monthName(islamicCalendar2.get(2) + 1, state.code);
        state.hijri = state.hday + " " + state.hmonth;
        state.greg = new SimpleDateFormat("d MMMM", locale).format(new Date());
        state.gday = new SimpleDateFormat("d", Locale.ENGLISH).format(new Date());
        state.gmonth = new SimpleDateFormat("MMMM", locale).format(new Date());
        state.gyear = "" + calendar.get(1);
        return state;
    }

    public static PendingIntent tapIntent(Context context, String str, int i) {
        boolean z = false;
        if (!"open".equals(context.getSharedPreferences("fajr_prefs", 0).getString("widget_tap_" + i, "open"))) {
            if (REFRESH_KINDS.contains(str)) {
                return refreshIntent(context, i);
            }
            return null;
        }
        if (!str.equals("Hijri") && !str.equals("Gregorian")) {
            z = true;
        }
        return open(context, z);
    }

    public static boolean tapOpens(Context context, int i) {
        return "open".equals(context.getSharedPreferences("fajr_prefs", 0).getString("widget_tap_" + i, "open"));
    }

    /* JADX WARN: Code restructure failed: missing block: B:39:0x00ab, code lost:
    
        if (r22 == android.text.Layout.Alignment.ALIGN_OPPOSITE) goto L34;
     */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void text(Canvas canvas, String str, float f, float f2, float f3, float f4, Typeface typeface, int i, Layout.Alignment alignment, float f5) {
        Typeface typeface2;
        Layout.Alignment alignment2;
        int i2;
        StaticLayout.Builder includePad;
        StaticLayout build;
        float f6;
        int i3;
        ThreadLocal threadLocal = ovToken;
        if (threadLocal.get() == null || str == null || !str.contains((CharSequence) threadLocal.get())) {
            typeface2 = typeface;
        } else {
            float f7 = ((f3 / 2.0f) + f) * f5;
            float f8 = f2 * f5;
            float f9 = f3 * f5;
            float fitSize = fitSize(str, typeface, f4, f3, f5, 0);
            typeface2 = typeface;
            if (typeface == boldFace || typeface.isBold()) {
                f6 = f7;
                i3 = 1;
            } else {
                f6 = f7;
                i3 = 0;
            }
            if (capture(str, f6, f8, f9, fitSize, i, i3)) {
                return;
            }
        }
        String b = Digits.forWidget(str);
        TextPaint textPaint = new TextPaint(1);
        textPaint.setTypeface(typeface2);
        textPaint.setTextSize(f4 * f5);
        textPaint.setColor(i);
        while (textPaint.measureText(b) > Math.max(1.0f, f3 - 2.0f) * f5 && textPaint.getTextSize() > f5 * 6.0f) {
            textPaint.setTextSize(textPaint.getTextSize() - (0.25f * f5));
        }
        TextDirectionHeuristic textDirectionHeuristic = TextDirectionHeuristics.FIRSTSTRONG_LTR;
        Layout.Alignment alignmentFinal = alignment;
        if (textDirectionHeuristic.isRtl(b, 0, b.length())) {
            alignmentFinal = Layout.Alignment.ALIGN_NORMAL;
            if (alignment == alignmentFinal) {
                alignmentFinal = Layout.Alignment.ALIGN_OPPOSITE;
            } else if (alignment == Layout.Alignment.ALIGN_OPPOSITE) {
                alignmentFinal = Layout.Alignment.ALIGN_NORMAL;
            }
        }
        int layoutWidth = (int) (f3 * f5);
        StaticLayout layout = StaticLayout.Builder.obtain(b, 0, b.length(), textPaint, Math.max(1, layoutWidth)).setAlignment(alignmentFinal).setTextDirection(textDirectionHeuristic).setIncludePad(false).setMaxLines(2).build();
        while (layout.getLineCount() > 1 && textPaint.getTextSize() > f5 * 6.0f) {
            textPaint.setTextSize(textPaint.getTextSize() - (f5 * 0.25f));
            layout = StaticLayout.Builder.obtain(b, 0, b.length(), textPaint, Math.max(1, layoutWidth)).setAlignment(alignmentFinal).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).setMaxLines(2).build();
        }
        canvas.save();
        canvas.translate(f * f5, (f2 * f5) - (layout.getHeight() / 2.0f));
        layout.draw(canvas);
        canvas.restore();
    }

    static String cityName(Context context, String str) {
        try {
            return (String) FajrPreferences.class.getMethod("getCityNameLocalized", String.class).invoke(FajrPreferences.class.getConstructor(Context.class).newInstance(context), str);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    public static long[] times(Context context, Calendar calendar) {
        return LockWidgets.timesCached(context, calendar);
    }

    public static String tm(long j, String str) {
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        return "\u2066" + new SimpleDateFormat("h:mm", Locale.ENGLISH).format(new Date(j)) + "\u2069 " + L10n.INSTANCE.t(calendar.get(9) == 0 ? S.AmMarker : S.PmMarker);
    }

    public static synchronized void updateAll(Context context, boolean z) {
        synchronized (HomeWidgets.class) {
            Digits.refresh(context);
            try {
                ThreadLocal<State> threadLocal = batchState;
                threadLocal.set(state(context));
                try {
                    updateBatch(context, z);
                    threadLocal.remove();
                    LockWidgets.schedule(context);
                    WidgetPreviews.maybeGenerate(context);
                } catch (Throwable th) {
                    batchState.remove();
                    LockWidgets.schedule(context);
                    WidgetPreviews.maybeGenerate(context);
                    throw th;
                }
            } catch (Exception e) {
                Log.e("WathakkerHome", "state() failed - widget update aborted", e);
            }
        }
    }

    /* JADX WARN: Can't wrap try/catch for region: R(40:18|(3:19|20|21)|(3:190|191|(37:195|196|197|(1:199)(3:200|(1:205)|206)|25|(1:189)(1:29)|30|31|32|33|34|35|(1:37)(1:182)|38|(1:40)(1:181)|41|42|43|(1:45)(1:176)|46|(1:48)(1:175)|49|50|51|(1:169)(1:55)|56|(4:58|59|60|(1:163)(17:65|66|67|68|69|70|71|(8:76|77|(1:155)(13:81|(2:83|(1:92)(3:85|(2:87|88)(2:90|91)|89))|154|93|94|(2:95|(7:97|(3:100|101|98)|102|103|104|105|(2:108|109)(1:107))(2:152|153))|110|111|112|(3:115|116|113)|117|118|119)|120|121|122|124|125)|156|77|(1:79)|155|120|121|122|124|125))(1:168)|164|156|77|(0)|155|120|121|122|124|125))|23|24|25|(1:27)|189|30|31|32|33|34|35|(0)(0)|38|(0)(0)|41|42|43|(0)(0)|46|(0)(0)|49|50|51|(1:53)|169|56|(0)(0)|164|156|77|(0)|155|120|121|122|124|125) */
    /* JADX WARN: Code restructure failed: missing block: B:126:0x033e, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:127:0x033f, code lost:
    
        r0.getMessage();
        android.util.Log.w("WathakkerHome", "update rejected, retrying single 1x bitmap: " + r9, r0);
        com.example.lockwidgets.HomeWidgets.osCap.set(java.lang.Float.valueOf(r29));
     */
    /* JADX WARN: Code restructure failed: missing block: B:130:0x0361, code lost:
    
        r1.updateAppWidget(r10, view(r32, r9, r10, r11, r12, r13));
     */
    /* JADX WARN: Code restructure failed: missing block: B:131:0x0368, code lost:
    
        com.example.lockwidgets.HomeWidgets.osCap.remove();
     */
    /* JADX WARN: Code restructure failed: missing block: B:134:0x036e, code lost:
    
        r0 = move-exception;
     */
    /* JADX WARN: Code restructure failed: missing block: B:135:0x036f, code lost:
    
        com.example.lockwidgets.HomeWidgets.osCap.remove();
     */
    /* JADX WARN: Code restructure failed: missing block: B:136:0x0374, code lost:
    
        throw r0;
     */
    /* JADX WARN: Code restructure failed: missing block: B:140:0x033c, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:171:0x0145, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:178:0x0375, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:179:0x0376, code lost:
    
        r28 = r12;
     */
    /* JADX WARN: Code restructure failed: missing block: B:184:0x037a, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:185:0x037b, code lost:
    
        r21 = r7;
        r24 = r8;
     */
    /* JADX WARN: Code restructure failed: missing block: B:187:0x0380, code lost:
    
        r0 = e;
     */
    /* JADX WARN: Code restructure failed: missing block: B:188:0x0381, code lost:
    
        r22 = r6;
     */
    /* JADX WARN: Removed duplicated region for block: B:168:0x0194  */
    /* JADX WARN: Removed duplicated region for block: B:175:0x0126  */
    /* JADX WARN: Removed duplicated region for block: B:176:0x0114  */
    /* JADX WARN: Removed duplicated region for block: B:181:0x00fc  */
    /* JADX WARN: Removed duplicated region for block: B:182:0x00ee  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x00eb  */
    /* JADX WARN: Removed duplicated region for block: B:40:0x00fa  */
    /* JADX WARN: Removed duplicated region for block: B:45:0x0111  */
    /* JADX WARN: Removed duplicated region for block: B:48:0x011e  */
    /* JADX WARN: Removed duplicated region for block: B:58:0x014f  */
    /* JADX WARN: Removed duplicated region for block: B:79:0x021f A[Catch: Exception -> 0x0184, TryCatch #13 {Exception -> 0x0184, blocks: (B:71:0x0173, B:76:0x017f, B:77:0x019b, B:79:0x021f, B:81:0x0225, B:83:0x0232, B:85:0x0244, B:89:0x025d, B:94:0x0277, B:97:0x0287, B:98:0x0291, B:100:0x0297), top: B:70:0x0173 }] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static void updateBatch(Context context, boolean z) {
        AppWidgetManager appWidgetManager = AppWidgetManager.getInstance(context);
        String[] kinds = {"Hero", "NextPrayer", "PrayerBar", "Schedule", "DayPrayer", "Pill", "Clock", "Hijri", "Gregorian", "OneUI"};
        for (int k = 0; k < 10; k++) {
            String kind = kinds[k];
            if (z && (kind.equals("Hijri") || kind.equals("Gregorian") || kind.equals("Schedule") || kind.equals("DayPrayer"))) {
                continue;
            }
            int[] ids = appWidgetManager.getAppWidgetIds(new ComponentName(context, Str.V("com.example.widget.", kind, "WidgetProvider")));
            for (int id : ids) {
                try {
                    Bundle options = appWidgetManager.getAppWidgetOptions(id);
                    int defW = (kind.startsWith("Hero") || kind.equals("Schedule") || kind.equals("OneUI")) ? 330
                            : kind.equals("Clock") ? 250
                            : (kind.equals("Hijri") || kind.equals("Gregorian")) ? 90 : 110;
                    boolean flag = kind.equals("OneUI") || large(context);
                    boolean portrait = context.getResources().getConfiguration().orientation != 2;
                    int w = Math.max(50, options.getInt(portrait ? "appWidgetMinWidth" : "appWidgetMaxWidth", defW));
                    int minH = kind.equals("Hero") ? 40 : 60;
                    int h = Math.max(minH, options.getInt(portrait ? "appWidgetMaxHeight" : "appWidgetMinHeight", 90));
                    ArrayList<SizeF> sizes = (large(context) && Build.VERSION.SDK_INT >= 31) ? options.getParcelableArrayList("appWidgetSizes") : null;
                    if (large(context) && Build.VERSION.SDK_INT >= 31 && (sizes == null || sizes.isEmpty())) {
                        int a = options.getInt("appWidgetMinWidth");
                        int b = options.getInt("appWidgetMaxWidth");
                        int c = options.getInt("appWidgetMinHeight");
                        int d = options.getInt("appWidgetMaxHeight");
                        if (a > 0 && b > 0 && c > 0 && d > 0) {
                            sizes = cornerSizes(a, b, c, d);
                        }
                    }
                    StringBuilder sb = new StringBuilder();
                    sb.append("size ").append(kind).append("#").append(id)
                            .append(" sw=").append(context.getResources().getConfiguration().smallestScreenWidthDp)
                            .append("dp density=").append(context.getResources().getDisplayMetrics().density)
                            .append(" min=").append(options.getInt("appWidgetMinWidth")).append("x").append(options.getInt("appWidgetMinHeight"))
                            .append(" max=").append(options.getInt("appWidgetMaxWidth")).append("x").append(options.getInt("appWidgetMaxHeight"))
                            .append(" sizes=").append(sizes)
                            .append(" used=").append(w).append("x").append(h);
                    Log.i("WathakkerHome", sb.toString());
                    RemoteViews views;
                    if (sizes == null || sizes.isEmpty()) {
                        views = view(context, kind, id, w, h, flag);
                    } else {
                        LinkedHashMap<SizeF, int[]> map = new LinkedHashMap<>();
                        int n = sizes.size();
                        for (int i = 0; i < n; i++) {
                            SizeF sizeF = sizes.get(i);
                            if (map.size() >= 4) {
                                break;
                            }
                            int sw = Math.max(50, Math.round(sizeF.getWidth()));
                            int sh = Math.max(minH, Math.round(sizeF.getHeight()));
                            map.put(slackKey(sizeF), new int[]{sw, sh});
                        }
                        float density = context.getResources().getDisplayMetrics().density;
                        float scale = 2.0f;
                        while (scale > 1.0f) {
                            double total = 0.0d;
                            for (int[] wh : map.values()) {
                                float fw = wh[0] * density * scale;
                                float fh = wh[1] * density * scale;
                                total += (double) (fh * fw * 4.0f);
                            }
                            if (total <= (double) bitmapBudget(context)) {
                                break;
                            }
                            scale -= 0.25f;
                        }
                        osCap.set(Float.valueOf(scale));
                        try {
                            HashMap<SizeF, RemoteViews> rv = new HashMap<>();
                            for (Map.Entry<SizeF, int[]> entry : map.entrySet()) {
                                rv.put(entry.getKey(), view(context, kind, id, entry.getValue()[0], entry.getValue()[1], flag));
                            }
                            osCap.remove();
                            views = new RemoteViews(rv);
                        } catch (Throwable th) {
                            osCap.remove();
                            throw th;
                        }
                    }
                    try {
                        appWidgetManager.updateAppWidget(id, views);
                    } catch (IllegalArgumentException e) {
                        Log.w("WathakkerHome", "update rejected, retrying single 1x bitmap: " + kind, e);
                        osCap.set(Float.valueOf(1.0f));
                        try {
                            appWidgetManager.updateAppWidget(id, view(context, kind, id, w, h, flag));
                        } finally {
                            osCap.remove();
                        }
                    }
                } catch (Exception e2) {
                    Log.e("WathakkerHome", "Update failed: " + kind, e2);
                }
            }
        }
    }

    public static RemoteViews view(Context context, String str, int i, int i2, int i3, boolean z) {
        RemoteViews remoteViews = new RemoteViews(context.getPackageName(), res(context, "layout", z ? "home_prayer_art_fit" : "home_prayer_art"));
        ovBegin();
        langForNative.set(lang(context));
        Bitmap renderKind = renderKind(context, str, i2, i3, i);
        if (z) {
            try {
                renderKind.setDensity(1);
            } catch (Throwable unused) {
            }
        }
        remoteViews.setImageViewBitmap(res(context, "id", "home_prayer_image"), renderKind);
        ovApply(context, remoteViews, (int) (i2 * context.getResources().getDisplayMetrics().density), (int) (i3 * context.getResources().getDisplayMetrics().density));
        int res = res(context, "id", "home_prayer_root");
        PendingIntent tapIntent = tapIntent(context, str, i);
        remoteViews.setOnClickPendingIntent(res(context, "id", "home_prayer_image"), tapIntent);
        if (z && res != 0) {
            remoteViews.setOnClickPendingIntent(res, tapIntent);
        }
        return remoteViews;
    }

    /* JADX WARN: Code restructure failed: missing block: B:40:0x00ec, code lost:
    
        r0 = r4.getWallpaperColors(1);
     */
    /* JADX WARN: Removed duplicated region for block: B:29:0x019b  */
    /* JADX WARN: Removed duplicated region for block: B:32:0x01ac  */
    /* JADX WARN: Removed duplicated region for block: B:34:0x01b1  */
    /* JADX WARN: Removed duplicated region for block: B:36:0x019e  */
    /* JADX WARN: Removed duplicated region for block: B:37:0x00e6 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /* JADX WARN: Removed duplicated region for block: B:65:0x00ca  */
    /* JADX WARN: Removed duplicated region for block: B:66:0x0078 A[EXC_TOP_SPLITTER, SYNTHETIC] */
    /*
        Code decompiled incorrectly, please refer to instructions dump.
        To view partially-correct add '--show-bad-code' argument
    */
    public static Bitmap wallBlur(Context context, boolean z) {
        int i;
        Bitmap bitmap;
        boolean z2;
        Drawable drawable;
        int i2;
        int i3;
        int i4;
        Bitmap bitmap2;
        Bitmap bitmap3;
        WallpaperManager wallpaperManager = WallpaperManager.getInstance(context);
        try {
            i = wallpaperManager.getWallpaperId(1);
        } catch (Throwable unused) {
            i = -1;
        }
        int i5 = i;
        if (z && (bitmap3 = wpReal) != null && i == wpRealId) {
            return bitmap3;
        }
        if (!z && (bitmap2 = wpMesh) != null && i5 == wpMeshId) {
            return bitmap2;
        }
        String str = "";
        if (z) {
            try {
                drawable = wallpaperManager.getDrawable();
            } catch (Throwable th) {
                str = "getDrawable:" + th;
                drawable = null;
            }
            if (drawable != null) {
                int intrinsicWidth = drawable.getIntrinsicWidth();
                int intrinsicHeight = drawable.getIntrinsicHeight();
                if (intrinsicWidth <= 0 || intrinsicHeight <= 0) {
                    intrinsicWidth = 720;
                    intrinsicHeight = 1280;
                }
                int max = Math.max(1, (intrinsicHeight * 48) / intrinsicWidth);
                bitmap = Bitmap.createBitmap(48, max, Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bitmap);
                drawable.setBounds(0, 0, 48, max);
                drawable.draw(canvas);
                String str2 = str;
                if (bitmap == null) {
                    try {
                        ParcelFileDescriptor g = wallpaperManager.getWallpaperFile(1);
                        if (g != null) {
                            BitmapFactory.Options options = new BitmapFactory.Options();
                            options.inSampleSize = 8;
                            Bitmap decodeFileDescriptor = BitmapFactory.decodeFileDescriptor(g.getFileDescriptor(), null, options);
                            g.close();
                            if (decodeFileDescriptor != null) {
                                bitmap = Bitmap.createScaledBitmap(decodeFileDescriptor, 48, Math.max(1, (decodeFileDescriptor.getHeight() * 48) / decodeFileDescriptor.getWidth()), true);
                            } else {
                                str2 = str2.concat("decode=null;");
                            }
                        } else {
                            str2 = str2.concat("file=null;");
                        }
                    } catch (Throwable th2) {
                        str = str2 + "getWallpaperFile:" + th2;
                    }
                }
                str = str2;
                if (bitmap != null) {
                    z2 = true;
                    if (bitmap != null && z) {
                        Log.i("WallBlur", "real image unavailable: ".concat(str));
                        return wallBlur(context, false);
                    }
                    if (bitmap == null) {
                        try {
                            if (Build.VERSION.SDK_INT >= 27) {
                                WallpaperColors a = wallpaperManager.getWallpaperColors(1);
                                if (a != null) {
                                    i2 = a.getPrimaryColor().toArgb();
                                    i3 = a.getSecondaryColor() != null ? a.getSecondaryColor().toArgb() : i2;
                                    i4 = a.getTertiaryColor() != null ? a.getTertiaryColor().toArgb() : i3;
                                    bitmap = Bitmap.createBitmap(48, 32, Bitmap.Config.ARGB_8888);
                                    Canvas canvas2 = new Canvas(bitmap);
                                    canvas2.drawColor(i2 | (-16777216));
                                    Paint paint = new Paint(1);
                                    int i6 = i3 | (-16777216);
                                    int i7 = i3 & 16777215;
                                    Shader.TileMode tileMode = Shader.TileMode.CLAMP;
                                    paint.setShader(new RadialGradient(5.7599998f, 4.8f, 29.76f, i6, i7, tileMode));
                                    canvas2.drawRect(0.0f, 0.0f, 48.0f, 32.0f, paint);
                                    paint.setShader(new RadialGradient(42.239998f, 28.8f, 28.800001f, i4 | (-16777216), i4 & 16777215, tileMode));
                                    canvas2.drawRect(0.0f, 0.0f, 48.0f, 32.0f, paint);
                                }
                            }
                            i2 = -10784858;
                            i3 = -7443536;
                            i4 = -11558720;
                            bitmap = Bitmap.createBitmap(48, 32, Bitmap.Config.ARGB_8888);
                            Canvas canvas22 = new Canvas(bitmap);
                            canvas22.drawColor(i2 | (-16777216));
                            Paint paint2 = new Paint(1);
                            int i62 = i3 | (-16777216);
                            int i72 = i3 & 16777215;
                            Shader.TileMode tileMode2 = Shader.TileMode.CLAMP;
                            paint2.setShader(new RadialGradient(5.7599998f, 4.8f, 29.76f, i62, i72, tileMode2));
                            canvas22.drawRect(0.0f, 0.0f, 48.0f, 32.0f, paint2);
                            paint2.setShader(new RadialGradient(42.239998f, 28.8f, 28.800001f, i4 | (-16777216), i4 & 16777215, tileMode2));
                            canvas22.drawRect(0.0f, 0.0f, 48.0f, 32.0f, paint2);
                        } catch (Throwable unused2) {
                            return null;
                        }
                    }
                    boxBlur(bitmap, 3);
                    boxBlur(bitmap, 3);
                    StringBuilder sb = new StringBuilder("real=");
                    sb.append(z);
                    sb.append(" source=");
                    sb.append(!z2 ? "wallpaper-image" : "wallpaper-colours");
                    Log.i("WallBlur", sb.toString());
                    if (!z2) {
                        wpReal = bitmap;
                        wpRealId = i5;
                    } else if (!z) {
                        wpMesh = bitmap;
                        wpMeshId = i5;
                    }
                    return bitmap;
                }
            } else {
                str = "getDrawable=null;";
                bitmap = null;
                String str22 = str;
                if (bitmap == null) {
                }
                str = str22;
                if (bitmap != null) {
                }
            }
        }
        z2 = false;
        bitmap = null;
        if (bitmap != null) {
        }
        if (bitmap == null) {
        }
        boxBlur(bitmap, 3);
        boxBlur(bitmap, 3);
        StringBuilder sb2 = new StringBuilder("real=");
        sb2.append(z);
        sb2.append(" source=");
        sb2.append(!z2 ? "wallpaper-image" : "wallpaper-colours");
        Log.i("WallBlur", sb2.toString());
        if (!z2) {
        }
        return bitmap;
    }

    public static synchronized void updateAll(Context context) {
        synchronized (HomeWidgets.class) {
            updateAll(context, false);
        }
    }

    public static boolean capture(String str, float f, float f2, float f3, float f4, int i) {
        return capture(str, f, f2, f3, f4, i, -1);
    }

    public static Typeface face(Context context, boolean z) {
        return face(context, z, rtl(context));
    }

    public static Bitmap wallBlur(Context context) {
        return wallBlur(context, false);
    }

    public static void updateBatch(Context context) {
        updateBatch(context, false);
    }
}
