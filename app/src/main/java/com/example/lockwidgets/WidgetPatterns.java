package com.example.lockwidgets;

import android.graphics.*;

/** Background patterns for the prayer widget card: 1 Rub el Hizb grid (default), 8 stars and diamonds, 9 hex lattice, 10 crossed octagram, 11 round arabesque. 0 = none. */
public final class WidgetPatterns {
    private WidgetPatterns() {}

    public static final int[] IDS = {1, 8, 9, 10, 11};

    private static Path star(float cx, float cy, int n, float ro, float ri) {
        Path p = new Path();
        for (int i = 0; i < 2 * n; i++) {
            double a = -Math.PI / 2 + i * Math.PI / n;
            float rr = (i % 2 == 0) ? ro : ri;
            float x = cx + (float) Math.cos(a) * rr, y = cy + (float) Math.sin(a) * rr;
            if (i == 0) p.moveTo(x, y); else p.lineTo(x, y);
        }
        p.close();
        return p;
    }

    /** Draws the pattern from the bottom-left corner over a w x h area (pixels). [ro] is the star radius in pixels. */
    public static void draw(Canvas cv, int pat, float w, float h, float ro, Paint p) {
        if (pat <= 0) return;
        if (pat == 1) {
            float step = ro * 2f;
            for (float cy = h; cy > -ro; cy -= step)
                for (float cx = 0; cx < w + ro; cx += step)
                    cv.drawPath(star(cx, cy, 8, ro, ro * 0.7654f), p);
            return;
        }
        boolean hex = pat == 9;
        float dx = hex ? ro * 1.732f : ro * 2f, dy = hex ? ro * 1.5f : ro * 2f;
        int rows = (int) (h / dy) + 3, cols = (int) (w / dx) + 3;
        for (int row = 0; row <= rows; row++) {
            for (int col = 0; col <= cols; col++) {
                float cx = col * dx + ((hex || pat == 11) && row % 2 == 1 ? dx / 2 : 0f);
                float cy = h - row * dy;
                switch (pat) {
                    case 8:
                        cv.drawPath(star(cx, cy, 8, ro, ro * 0.7654f), p);
                        Path dm = new Path(); float qx = cx + dx / 2, qy = cy - dy / 2, q = ro * 0.30f;
                        dm.moveTo(qx, qy - q); dm.lineTo(qx + q, qy); dm.lineTo(qx, qy + q); dm.lineTo(qx - q, qy); dm.close();
                        cv.drawPath(dm, p);
                        break;
                    case 9:
                        cv.drawPath(star(cx, cy, 6, ro * 0.98f, ro * 0.577f), p);
                        cv.drawCircle(cx, cy, ro * 0.16f, p);
                        break;
                    case 10:
                        cv.drawPath(star(cx, cy, 8, ro, ro * 0.541f), p);
                        cv.drawPath(star(cx, cy, 8, ro * 0.5f, ro * 0.27f), p);
                        break;
                    default:
                        cv.drawPath(star(cx, cy, 12, ro, ro * 0.5176f), p);
                        cv.drawCircle(cx, cy, ro * 0.22f, p);
                        cv.drawCircle(cx, cy, ro * 0.62f, p);
                }
            }
        }
    }

    /** Small preview square for the settings screen. */
    public static Bitmap thumb(int pat, int px, boolean night) {
        Bitmap b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(b);
        cv.drawColor(night ? 0xFF2B2C30 : 0xFFF1F2F5);
        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        p.setStyle(Paint.Style.STROKE);
        p.setStrokeWidth(Math.max(1f, px * 0.012f));
        p.setColor(night ? 0xFFFFFFFF : 0xFF7C8396);
        p.setAlpha(night ? 150 : 170);
        draw(cv, pat, px, px, px * 0.2f, p);
        return b;
    }

    public static int forWidget(android.content.Context c, int widgetId) {
        android.content.SharedPreferences sp = c.getSharedPreferences("fajr_prefs", android.content.Context.MODE_PRIVATE);
        int v = widgetId > 0 ? sp.getInt("widget_pattern_" + widgetId, 1) : 1;
        for (int id : IDS) if (id == v) return v;
        return v == 0 ? 0 : 1;
    }
}
