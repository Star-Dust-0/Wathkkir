package com.example.lockwidgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BlurMaskFilter;
import android.graphics.Canvas;
import android.graphics.LinearGradient;
import android.graphics.Paint;
import android.graphics.Path;
import android.graphics.PorterDuff;
import android.graphics.PorterDuffXfermode;
import android.graphics.RectF;
import android.graphics.Shader;
import android.graphics.Typeface;
import android.text.Layout;
import android.text.SpannableString;
import android.text.StaticLayout;
import android.text.TextDirectionHeuristics;
import android.text.TextPaint;
import android.text.style.ForegroundColorSpan;
import android.text.style.MetricAffectingSpan;
import com.example.lockwidgets.HomeWidgets;
import com.example.utils.Digits;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.Date;
import java.util.Locale;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
final class OneUIWidget {
    private static final int[] ACCENT_MAIN = {-10148, -12268352, -12673026, -9842621};
    private static final int[] ACCENT_DEEP = {-1333248, -15297384, -13270792, -16268444};

    /* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
    public static final class CustomTypefaceSpan extends MetricAffectingSpan {
        private final Typeface tf;

        public CustomTypefaceSpan(Typeface typeface) {
            this.tf = typeface;
        }

        @Override // android.text.style.CharacterStyle
        public void updateDrawState(TextPaint textPaint) {
            textPaint.setTypeface(this.tf);
        }

        @Override // android.text.style.MetricAffectingSpan
        public void updateMeasureState(TextPaint textPaint) {
            textPaint.setTypeface(this.tf);
        }
    }

    private OneUIWidget() {
    }

    public static int accentForWidget(Context context, int i) {
        if (i <= 0) {
            return 0;
        }
        int i2 = context.getSharedPreferences("fajr_prefs", 0).getInt("widget_oneui_color_" + i, 0);
        if (i2 < 1 || i2 > 3) {
            return 0;
        }
        return i2;
    }

    private static int blend(int i, int i2, float f) {
        int i3 = (i >> 16) & 255;
        int i4 = (i >> 8) & 255;
        return ((int) ((((i2 & 255) - (i & 255)) * f) + (i & 255))) | (-16777216) | (((int) (((((i2 >> 16) & 255) - i3) * f) + i3)) << 16) | (((int) (((((i2 >> 8) & 255) - i4) * f) + i4)) << 8);
    }

    private static float lerp(float f, float f2, float f3) {
        return ((f2 - f) * f3) + f;
    }

    private static void line(Canvas canvas, String str, float f, float f2, float f3, float f4, Typeface typeface, int i, Layout.Alignment alignment, float f5) {
        HomeWidgets.heroText(canvas, str, f, f2, f3, f4, typeface, i, alignment, f5);
    }

    private static void remaining(Canvas canvas, String str, String str2, float f, float f2, float f3, float f4, Typeface typeface, Typeface typeface2, int i, int i2, float f5) {
        Typeface typeface3;
        float f6;
        String str3 = str + str2;
        ThreadLocal<String> threadLocal = HomeWidgets.ovToken;
        if (threadLocal.get() == null || !str3.contains(threadLocal.get())) {
            typeface3 = typeface2;
        } else {
            typeface3 = typeface2;
            if (HomeWidgets.capture(str3, ((f3 / 2.0f) + f) * f5, f2 * f5, f3 * f5, HomeWidgets.fitSize(str3, typeface3, f4, f3, f5, 2), i2, 1)) {
                return;
            }
        }
        String b = Digits.forWidget(str3);
        SpannableString spannableString = new SpannableString(b);
        spannableString.setSpan(new ForegroundColorSpan(i), 0, str.length(), 33);
        spannableString.setSpan(new ForegroundColorSpan(i2), str.length(), b.length(), 33);
        spannableString.setSpan(new CustomTypefaceSpan(typeface3), 0, b.length(), 33);
        TextPaint textPaint = new TextPaint(1);
        textPaint.setTypeface(typeface3);
        textPaint.setTextSize(f4 * f5);
        textPaint.setColor(i);
        while (true) {
            f6 = f3 * f5;
            if (textPaint.measureText(b) <= f6 || textPaint.getTextSize() <= 8.0f * f5) {
                break;
            } else {
                textPaint.setTextSize(textPaint.getTextSize() - (0.5f * f5));
            }
        }
        StaticLayout build = StaticLayout.Builder.obtain(spannableString, 0, spannableString.length(), textPaint, Math.max(1, (int) f6)).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).setMaxLines(1).build();
        canvas.save();
        canvas.translate(f * f5, (f2 * f5) - (build.getHeight() / 2.0f));
        build.draw(canvas);
        canvas.restore();
    }

    public static Bitmap render(Context context, HomeWidgets.State state, int i, int i2, int i3) {
        Typeface typeface;
        float f;
        boolean z;
        int blend;
        int blend2;
        int blend3;
        int i4;
        int i5;
        int i6;
        int i7;
        String durText;
        float f2 = context.getResources().getDisplayMetrics().density;
        float oversample = HomeWidgets.oversample(context, i, i2, f2);
        float f3 = i;
        float f4 = i2;
        float f5 = f4 * f2;
        Bitmap createBitmap = Bitmap.createBitmap(Math.max(1, (int) (f3 * f2 * oversample)), Math.max(1, (int) (f5 * oversample)), Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        canvas.scale(oversample, oversample);
        boolean z2 = (context.getResources().getConfiguration().uiMode & 48) == 32;
        float max = Math.max(0.0f, Math.min(1.0f, (f4 - 80.0f) / 100.0f));
        float max2 = Math.max(6.0f, f3 * 0.02f);
        float min = Math.min(f4 * 0.93f, f3 * 0.225f);
        float f6 = (f3 - min) - max2;
        float min2 = Math.min(f4 * 0.3f, 34.0f);
        RectF rectF = new RectF(0.0f, 0.0f, f6, f4);
        RectF rectF2 = new RectF(f6 + max2, 0.0f, f3, f4);
        Typeface face = HomeWidgets.face(context, false, state.rtl);
        Typeface face2 = HomeWidgets.face(context, true, state.rtl);
        int i8 = z2 ? -13947856 : -1;
        int i9 = z2 ? -14539994 : -920843;
        int i10 = z2 ? -1 : -14935010;
        int i11 = z2 ? -4934470 : -9671564;
        int i12 = z2 ? -11908529 : -2171166;
        int accentForWidget = accentForWidget(context, i3);
        if (accentForWidget == 0) {
            i5 = z2 ? -10148 : -11165441;
            i4 = z2 ? -673792 : -13997072;
            blend = z2 ? -10148 : -6769;
            blend2 = z2 ? -1333248 : -14022;
            blend3 = z2 ? -14016000 : -12964352;
            typeface = face;
            f = f4;
            z = z2;
        } else {
            int i13 = ACCENT_MAIN[accentForWidget];
            typeface = face;
            int i14 = ACCENT_DEEP[accentForWidget];
            f = f4;
            if (z2) {
                z = z2;
                blend = i13;
            } else {
                z = z2;
                blend = blend(i13, -1, 0.38f);
            }
            blend2 = z ? i14 : blend(i13, i14, 0.55f);
            blend3 = blend(i14, -16777216, z ? 0.78f : 0.68f);
            i4 = i14;
            i5 = i13;
        }
        int i15 = blend;
        int i16 = blend2;
        int i17 = blend3;
        if (state.iqama && accentForWidget == 0) {
            i6 = -1723307;
            i7 = -1723307;
        } else {
            i6 = i5;
            i7 = i4;
        }
        Paint paint = new Paint(1);
        Shader.TileMode tileMode = Shader.TileMode.CLAMP;
        paint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, f5, i8, i9, tileMode));
        float f7 = min2 * f2;
        canvas.drawRoundRect(new RectF(rectF.left * f2, rectF.top * f2, rectF.right * f2, rectF.bottom * f2), f7, f7, paint);
        paint.setShader(null);
        float f8 = f6 * f2;
        int i18 = i12;
        canvas.saveLayer(0.0f, 0.0f, f8, f5, null);
        Path path = new Path();
        path.addRoundRect(new RectF(0.0f, 0.0f, f8, f5), f7, f7, Path.Direction.CW);
        canvas.clipPath(path);
        Paint paint2 = new Paint(1);
        paint2.setStyle(Paint.Style.STROKE);
        paint2.setStrokeWidth(Math.max(0.8f, f2 * 0.9f));
        paint2.setColor(z ? -1 : -8617066);
        paint2.setAlpha(z ? 48 : 70);
        WidgetPatterns.draw(canvas, WidgetPatterns.forWidget(context, i3), f8, f5, lerp(0.115f, 0.092f, max) * f * f2, paint2);
        Paint paint3 = new Paint();
        paint3.setShader(new LinearGradient(0.0f, f5, f8 * 0.9f, 0.0f, -16777216, 0, tileMode));
        paint3.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        canvas.drawRect(0.0f, 0.0f, f8, f5, paint3);
        canvas.restore();
        float lerp = lerp(0.21f, 0.2f, max) * f;
        float lerp2 = f * lerp(0.385f, 0.43f, max);
        float lerp3 = f * lerp(0.565f, 0.63f, max);
        float lerp4 = f * lerp(0.795f, 0.84f, max);
        float lerp5 = lerp(0.162f, 0.116f, max) * f;
        float lerp6 = lerp(0.152f, 0.096f, max) * f;
        float lerp7 = f * lerp(0.165f, 0.108f, max);
        float min3 = Math.min(24.0f, 0.06f * f6);
        float f9 = f6 - (min3 * 2.0f);
        float f10 = f9 * 0.46f;
        String str = state.targetName;
        String str2 = state.names[state.previous];
        String time = time(context, state, state.at);
        String time2 = time(context, state, state.prev);
        float f11 = min3 + f9;
        float f12 = f11 - f10;
        Layout.Alignment alignment = Layout.Alignment.ALIGN_OPPOSITE;
        Layout.Alignment alignment2 = Layout.Alignment.ALIGN_NORMAL;
        int i19 = i10;
        line(canvas, str, f12, lerp, f10, lerp5, face2, i19, alignment, f2);
        line(canvas, str2, min3, lerp, f10, lerp5, face2, i19, alignment2, f2);
        Typeface typeface2 = typeface;
        line(canvas, time, f12, lerp3, f10, lerp6, typeface2, i19, alignment, f2);
        line(canvas, time2, min3, lerp3, f10, lerp6, typeface2, i19, alignment2, f2);
        float max3 = Math.max(6.0f, lerp(0.125f, 0.1f, max) * f);
        long j = state.now;
        long j2 = state.prev;
        float max4 = Math.max(0.02f, Math.min(1.0f, (j - j2) / Math.max(1L, state.at - j2)));
        float f13 = min3 * f2;
        float f14 = max3 / 2.0f;
        RectF rectF3 = new RectF(f13, (lerp2 - f14) * f2, f11 * f2, (lerp2 + f14) * f2);
        paint.setColor(i18);
        float f15 = max3 * f2;
        float f16 = f15 / 2.0f;
        canvas.drawRoundRect(rectF3, f16, f16, paint);
        RectF rectF4 = new RectF(f13, rectF3.top, (min3 + (max4 * f9)) * f2, rectF3.bottom);
        Paint paint4 = new Paint(1);
        int i20 = i7;
        paint4.setColor(i20);
        paint4.setAlpha(z ? 70 : 60);
        paint4.setMaskFilter(new BlurMaskFilter(f15 * 0.9f, BlurMaskFilter.Blur.NORMAL));
        canvas.drawRoundRect(rectF4, f16, f16, paint4);
        paint.setShader(new LinearGradient(rectF4.left, 0.0f, rectF4.right, 0.0f, i6, i20, tileMode));
        canvas.drawRoundRect(rectF4, f16, f16, paint);
        paint.setShader(null);
        long max5 = Math.max(0L, (state.at - state.now) / 60000);
        String remPrefix = HomeWidgets.remPrefix(state.code, ": ");
        if (state.iqama) {
            durText = "\u2066" + HomeWidgets.mmss(state.at - state.now) + "\u2069";
        } else {
            durText = HomeWidgets.durText(max5, state.code);
        }
        String str3 = durText;
        HomeWidgets.ovSemi.set(Boolean.TRUE);
        remaining(canvas, remPrefix, str3, min3, lerp4, f9, lerp7, typeface2, face2, i11, i19, f2);
        RectF rectF5 = new RectF(rectF2.left * f2, 0.0f, rectF2.right * f2, f5);
        paint.setShader(new LinearGradient(0.0f, 0.0f, 0.0f, f5, i15, i16, tileMode));
        float f17 = 1.05f * f7;
        canvas.drawRoundRect(rectF5, f17, f17, paint);
        paint.setShader(null);
        float lerp8 = f * lerp(0.127f, 0.07f, max);
        float lerp9 = f * lerp(0.42f, 0.245f, max);
        float lerp10 = lerp(0.118f, 0.065f, max) * f;
        String str4 = state.day;
        float f18 = rectF2.left + 4.0f;
        float lerp11 = lerp(0.2f, 0.3f, max) * f;
        float f19 = min - 8.0f;
        Layout.Alignment alignment3 = Layout.Alignment.ALIGN_CENTER;
        line(canvas, str4, f18, lerp11, f19, lerp8, typeface2, i17, alignment3, f2);
        line(canvas, state.hday, rectF2.left + 4.0f, lerp(0.5f, 0.5f, max) * f, f19, lerp9, face2, i17, alignment3, f2);
        line(canvas, state.hmonth, rectF2.left + 4.0f, f * lerp(0.8f, 0.7f, max), f19, lerp10, typeface2, i17, alignment3, f2);
        return createBitmap;
    }

    private static String time(Context context, HomeWidgets.State state, long j) {
        if (!state.code.startsWith("ar")) {
            return HomeWidgets.tm(j, state.code);
        }
        Calendar calendar = Calendar.getInstance();
        calendar.setTimeInMillis(j);
        String str = calendar.get(9) == 0 ? "ص" : "م";
        return "\u2066" + new SimpleDateFormat("hh:mm", Locale.ENGLISH).format(new Date(j)) + "\u2069 " + str;
    }
}
