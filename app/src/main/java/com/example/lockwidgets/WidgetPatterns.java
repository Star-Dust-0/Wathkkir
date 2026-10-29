package com.example.lockwidgets;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.Path;

/* compiled from: r8-map-id-fecc35cc6047ab9fdb22996bf50955d9790c9a22fb2ab87174fa2e6474f3808e */
/* loaded from: classes.dex */
public final class WidgetPatterns {
    public static final int[] IDS = {1, 8, 9, 10, 11};

    private WidgetPatterns() {
    }

    public static void draw(Canvas canvas, int i, float f, float f2, float f3, Paint paint) {
        if (i <= 0) {
            return;
        }
        float f4 = 0.7654f;
        int i2 = 8;
        int i3 = 1;
        float f5 = 2.0f;
        if (i == 1) {
            float f6 = f3 * 2.0f;
            for (float f7 = f2; f7 > (-f3); f7 -= f6) {
                for (float f8 = 0.0f; f8 < f + f3; f8 += f6) {
                    canvas.drawPath(star(f8, f7, 8, f3, f3 * 0.7654f), paint);
                }
            }
            return;
        }
        boolean z = i == 9;
        float f9 = z ? 1.732f * f3 : f3 * 2.0f;
        float f10 = z ? 1.5f * f3 : f3 * 2.0f;
        int i4 = ((int) (f2 / f10)) + 3;
        int i5 = ((int) (f / f9)) + 3;
        int i6 = 0;
        while (i6 <= i4) {
            float f11 = f4;
            int i7 = 0;
            while (i7 <= i5) {
                float f12 = f5;
                float f13 = (i7 * f9) + (((z || i == 11) && i6 % 2 == i3) ? f9 / f12 : 0.0f);
                float f14 = f2 - (i6 * f10);
                switch (i) {
                    case 8:
                        canvas.drawPath(star(f13, f14, 8, f3, f3 * f11), paint);
                        Path path = new Path();
                        float f15 = (f9 / f12) + f13;
                        float f16 = f14 - (f10 / f12);
                        float f17 = 0.3f * f3;
                        path.moveTo(f15, f16 - f17);
                        path.lineTo(f15 + f17, f16);
                        path.lineTo(f15, f16 + f17);
                        path.lineTo(f15 - f17, f16);
                        path.close();
                        canvas.drawPath(path, paint);
                        break;
                    case 9:
                        canvas.drawPath(star(f13, f14, 6, 0.98f * f3, 0.577f * f3), paint);
                        canvas.drawCircle(f13, f14, 0.16f * f3, paint);
                        break;
                    case 10:
                        canvas.drawPath(star(f13, f14, i2, f3, 0.541f * f3), paint);
                        canvas.drawPath(star(f13, f14, i2, 0.5f * f3, 0.27f * f3), paint);
                        break;
                    default:
                        canvas.drawPath(star(f13, f14, 12, f3, f3 * 0.5176f), paint);
                        canvas.drawCircle(f13, f14, 0.22f * f3, paint);
                        canvas.drawCircle(f13, f14, 0.62f * f3, paint);
                        break;
                }
                i7++;
                f5 = f12;
                i2 = 8;
                i3 = 1;
            }
            i6++;
            f4 = f11;
            i2 = 8;
            i3 = 1;
        }
    }

    public static int forWidget(Context context, int i) {
        int i2 = i > 0 ? context.getSharedPreferences("fajr_prefs", 0).getInt("widget_pattern_" + i, 1) : 1;
        for (int i3 : IDS) {
            if (i3 == i2) {
                return i2;
            }
        }
        return i2 == 0 ? 0 : 1;
    }

    private static Path star(float f, float f2, int i, float f3, float f4) {
        Path path = new Path();
        for (int i2 = 0; i2 < i * 2; i2++) {
            double d = ((i2 * 3.141592653589793d) / i) - 1.5707963267948966d;
            float f5 = i2 % 2 == 0 ? f3 : f4;
            float cos = (((float) Math.cos(d)) * f5) + f;
            float sin = (((float) Math.sin(d)) * f5) + f2;
            if (i2 == 0) {
                path.moveTo(cos, sin);
            } else {
                path.lineTo(cos, sin);
            }
        }
        path.close();
        return path;
    }

    public static Bitmap thumb(int i, int i2, boolean z) {
        Bitmap createBitmap = Bitmap.createBitmap(i2, i2, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(createBitmap);
        canvas.drawColor(z ? -13947856 : -920843);
        Paint paint = new Paint(1);
        paint.setStyle(Paint.Style.STROKE);
        float f = i2;
        paint.setStrokeWidth(Math.max(1.0f, 0.012f * f));
        paint.setColor(z ? -1 : -8617066);
        paint.setAlpha(z ? 150 : 170);
        draw(canvas, i, f, f, f * 0.2f, paint);
        return createBitmap;
    }
}
