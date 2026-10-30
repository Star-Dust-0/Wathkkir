package com.example.lockwidgets;

import android.content.Context;
import android.content.res.Configuration;
import android.graphics.*;
import android.text.*;
import java.util.Locale;

/** One UI style prayer progress card with a yellow date tile (mockup v4). Drawn like the other home widgets. */
final class OneUIWidget {
    private OneUIWidget() {}

    private static float lerp(float a, float b, float t) { return a + (b - a) * t; }

    /** Per-channel ARGB blend: t=0 keeps a, t=1 gives b. */
    private static int blend(int a, int b, float t) {
        int r = (int) (((a >> 16) & 0xFF) + ((((b >> 16) & 0xFF) - ((a >> 16) & 0xFF)) * t));
        int g = (int) (((a >> 8) & 0xFF) + ((((b >> 8) & 0xFF) - ((a >> 8) & 0xFF)) * t));
        int bl = (int) ((a & 0xFF) + (((b & 0xFF) - (a & 0xFF)) * t));
        return 0xFF000000 | (r << 16) | (g << 8) | bl;
    }

    // Accent themes (3.3.3, his call 22.10.2026): 0 = the classic yellow (default, values
    // unchanged); 1-3 reuse the watch app palette pairs (wear/Settings.kt WTheme.defs:
    // main, deep) so the widget matches the watch app colours exactly.
    private static final int[] ACCENT_MAIN = {0xFFFFD85C, 0xFF44CCC0, 0xFF3E9FFE, 0xFF69D043};
    private static final int[] ACCENT_DEEP = {0xFFEBA800, 0xFF169498, 0xFF3580F8, 0xFF07C364};

    /** Selected accent for a widget instance: fajr_prefs widget_oneui_color_<id>, default 0 (yellow). */
    static int accentForWidget(android.content.Context c, int widgetId) {
        if (widgetId <= 0) return 0;
        int v = c.getSharedPreferences("fajr_prefs", android.content.Context.MODE_PRIVATE)
                .getInt("widget_oneui_color_" + widgetId, 0);
        return v >= 1 && v <= 3 ? v : 0;
    }

    static Bitmap render(Context c, HomeWidgets.State s, int wd, int ht, int widgetId) {
        float d = c.getResources().getDisplayMetrics().density;
        float os = HomeWidgets.oversample(c, wd, ht, d);
        Bitmap bmp = Bitmap.createBitmap(Math.max(1, (int) (wd * d * os)), Math.max(1, (int) (ht * d * os)), Bitmap.Config.ARGB_8888);
        Canvas cv = new Canvas(bmp);
        cv.scale(os, os);
        boolean night = (c.getResources().getConfiguration().uiMode & Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES;
        float W = wd, H = ht;
        float t = Math.max(0f, Math.min(1f, (H - 80f) / 100f)); // 0 = 4x1, 1 = 4x2
        float gap = Math.max(6f, W * 0.02f);
        float tileW = Math.min(H * 0.93f, W * 0.225f);
        // Large screens only: keep the date tile's shape (width/height = 0.93, the full-width look) when the card is width-limited, instead of letting it turn into a tall sliver; it is centred vertically.
        android.content.SharedPreferences dbg = c.getSharedPreferences("fajr_prefs", 0);
        boolean tileFix = HomeWidgets.large(c) && dbg.getBoolean("dbg_tilefix", true), textFix = HomeWidgets.large(c) && dbg.getBoolean("dbg_textfix", true);
        float tileH = tileFix ? Math.min(H, tileW / 0.8095f) : H; // 0.8095 = 68/84, the observed known-good 302x84 tile
        float tileTop = (H - tileH) / 2f;
        float cardW = W - tileW - gap;
        float corner = Math.min(H * 0.30f, 34f);
        RectF card = new RectF(0, 0, cardW, H);
        RectF tile = new RectF(cardW + gap, tileTop, W, tileTop + tileH);
        Typeface reg = HomeWidgets.face(c, false, s.rtl), bold = HomeWidgets.face(c, true, s.rtl);

        int cardTop = night ? 0xFF2B2C30 : 0xFFFFFFFF, cardBot = night ? 0xFF222326 : 0xFFF1F2F5;
        int ink = night ? 0xFFFFFFFF : 0xFF1C1C1E;
        int soft = night ? 0xFFB4B4BA : 0xFF6C6C74;
        int track = night ? 0xFF4A4A4F : 0xFFDEDEE2;
        int accent = accentForWidget(c, widgetId);
        int barA, barB, tA, tB, tInk;
        if (accent == 0) {
            // Classic yellow: the exact shipped values, including the blue day bar.
            barA = night ? 0xFFFFD85C : 0xFF55A0FF; barB = night ? 0xFFF5B800 : 0xFF2A6BF0;
            tA = night ? 0xFFFFD85C : 0xFFFFE58F; tB = night ? 0xFFEBA800 : 0xFFFFC93A;
            tInk = night ? 0xFF2A2200 : 0xFF3A2E00;
        } else {
            int m = ACCENT_MAIN[accent], deep = ACCENT_DEEP[accent];
            barA = m; barB = deep;
            // Day tile derived like the yellow one: lighter top, mid-toned bottom; ink is a
            // dark shade of the accent so the date stays readable on the coloured tile.
            tA = night ? m : blend(m, 0xFFFFFFFF, 0.38f);
            tB = night ? deep : blend(m, deep, 0.55f);
            tInk = blend(deep, 0xFF000000, night ? 0.78f : 0.68f);
        }
        // Iqama countdown: the bar takes the selected theme like everything else (his call
        // 22.10.2026 20:30). The flat amber override stays for the default yellow theme only,
        // where it is part of the shipped look.
        if (s.iqama && accent == 0) { barA = 0xFFE5B455; barB = 0xFFE5B455; }

        Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        // card
        p.setShader(new LinearGradient(0, 0, 0, H * d, cardTop, cardBot, Shader.TileMode.CLAMP));
        cv.drawRoundRect(new RectF(card.left * d, card.top * d, card.right * d, card.bottom * d), corner * d, corner * d, p);
        p.setShader(null);
        // pattern (default Rub el Hizb, per-widget setting), fading from the bottom-left corner
        cv.saveLayer(0, 0, cardW * d, H * d, null);
        Path clip = new Path();
        clip.addRoundRect(new RectF(0, 0, cardW * d, H * d), corner * d, corner * d, Path.Direction.CW);
        cv.clipPath(clip);
        Paint sp = new Paint(Paint.ANTI_ALIAS_FLAG);
        sp.setStyle(Paint.Style.STROKE);
        sp.setStrokeWidth(Math.max(0.8f, 0.9f * d));
        sp.setColor(night ? 0xFFFFFFFF : 0xFF7C8396);
        sp.setAlpha(night ? 48 : 70);
        float R = H * lerp(0.115f, 0.092f, t);
        WidgetPatterns.draw(cv, WidgetPatterns.forWidget(c, widgetId), cardW * d, H * d, R * d, sp);
        Paint fade = new Paint();
        fade.setShader(new LinearGradient(0, H * d, cardW * d * 0.9f, 0, 0xFF000000, 0x00000000, Shader.TileMode.CLAMP));
        fade.setXfermode(new PorterDuffXfermode(PorterDuff.Mode.DST_IN));
        cv.drawRect(0, 0, cardW * d, H * d, fade);
        cv.restore();

        // text rows (fractions of card height, interpolated between 4x1 and 4x2)
        float nameY = H * lerp(0.21f, 0.20f, t), barY = H * lerp(0.385f, 0.43f, t), timeY = H * lerp(0.565f, 0.63f, t), remY = H * lerp(0.795f, 0.84f, t);
        float nameSz = H * lerp(0.162f, 0.116f, t), timeSz = H * lerp(0.152f, 0.096f, t), remSz = H * lerp(0.165f, 0.108f, t);
        // Large screens only: when the card is narrower than the full-width one (resized down), scale the text with the card width so the rows keep their air. Full width (ratio >= 2.71) and phones are unchanged.
        if (textFix) { float nf = Math.max(0.78f, Math.min(1f, (cardW / H) / 2.71f)); nameSz *= nf; timeSz *= nf; remSz *= nf; }
        float padX = Math.min(24f, cardW * 0.06f), inner = cardW - 2 * padX, side = inner * 0.46f;
        boolean rtl = s.rtl;
        String nextName = s.targetName, prevName = s.names[s.previous];
        String nextTime = time(c, s, s.at), prevTime = time(c, s, s.prev);
        float xNext = padX + inner - side, xPrev = padX; // previous prayer on the left, next on the right, as in the mockup
        Layout.Alignment nextA = Layout.Alignment.ALIGN_OPPOSITE;
        Layout.Alignment prevA = Layout.Alignment.ALIGN_NORMAL;
        line(cv, nextName, xNext, nameY, side, nameSz, bold, ink, nextA, d);
        line(cv, prevName, xPrev, nameY, side, nameSz, bold, ink, prevA, d);
        line(cv, nextTime, xNext, timeY, side, timeSz, reg, ink, nextA, d);
        line(cv, prevTime, xPrev, timeY, side, timeSz, reg, ink, prevA, d);

        // progress bar
        float bh = Math.max(6f, H * lerp(0.125f, 0.10f, t));
        float progress = Math.max(0.02f, Math.min(1f, (s.now - s.prev) / (float) Math.max(1, s.at - s.prev)));
        RectF tr = new RectF(padX * d, (barY - bh / 2) * d, (padX + inner) * d, (barY + bh / 2) * d);
        p.setColor(track);
        cv.drawRoundRect(tr, bh * d / 2, bh * d / 2, p);
        float fw = inner * progress;
        RectF fr = new RectF(padX * d, tr.top, (padX + fw) * d, tr.bottom); // fills left to right
        Paint glow = new Paint(Paint.ANTI_ALIAS_FLAG);
        glow.setColor(barB);
        glow.setAlpha(night ? 70 : 60);
        glow.setMaskFilter(new BlurMaskFilter(bh * d * 0.9f, BlurMaskFilter.Blur.NORMAL));
        cv.drawRoundRect(fr, bh * d / 2, bh * d / 2, glow);
        p.setShader(new LinearGradient(fr.left, 0, fr.right, 0, barA, barB, Shader.TileMode.CLAMP));
        cv.drawRoundRect(fr, bh * d / 2, bh * d / 2, p);
        p.setShader(null);

        // remaining line: gray label + bold value (iqama: native ticking countdown replaces the value)
        long mins = Math.max(0, (s.at - s.now) / 60000);
        String prefix = HomeWidgets.remPrefix(s.code, ": ");
        String value = s.iqama ? "\u2066" + HomeWidgets.mmss(s.at - s.now) + "\u2069" : HomeWidgets.durText(mins, s.code);
        HomeWidgets.ovSemi.set(Boolean.TRUE);
        remaining(cv, prefix, value, padX, remY, inner, remSz, reg, bold, soft, ink, d);

        // date tile (accent colours resolved above)
        RectF tp = new RectF(tile.left * d, tile.top * d, tile.right * d, tile.bottom * d);
        p.setShader(new LinearGradient(0, tile.top * d, 0, tile.bottom * d, tA, tB, Shader.TileMode.CLAMP));
        cv.drawRoundRect(tp, corner * d * 1.05f, corner * d * 1.05f, p);
        p.setShader(null);
        float tcx = tile.left + tileW / 2;
        float wdSz = tileH * lerp(0.127f, 0.07f, t), numSz = tileH * lerp(0.42f, 0.245f, t), mSz = tileH * lerp(0.118f, 0.065f, t);
        line(cv, s.day, tile.left + 4, tileTop + tileH * lerp(0.20f, 0.30f, t), tileW - 8, wdSz, reg, tInk, Layout.Alignment.ALIGN_CENTER, d);
        line(cv, s.hday, tile.left + 4, tileTop + tileH * lerp(0.50f, 0.50f, t), tileW - 8, numSz, bold, tInk, Layout.Alignment.ALIGN_CENTER, d);
        line(cv, s.hmonth, tile.left + 4, tileTop + tileH * lerp(0.80f, 0.70f, t), tileW - 8, mSz, reg, tInk, Layout.Alignment.ALIGN_CENTER, d);
        return bmp;
    }

    private static String time(Context c, HomeWidgets.State s, long at) {
        if (!s.code.startsWith("ar")) return HomeWidgets.tm(at, s.code);
        java.util.Calendar cal = java.util.Calendar.getInstance();
        cal.setTimeInMillis(at);
        String m = cal.get(java.util.Calendar.AM_PM) == 0 ? "ص" : "م";
        String hm = new java.text.SimpleDateFormat("hh:mm", Locale.ENGLISH).format(new java.util.Date(at));
        return "\u2066" + hm + "\u2069 " + m;
    }

    private static void line(Canvas cv, String s, float x, float cy, float w, float size, Typeface f, int color, Layout.Alignment a, float d) {
        HomeWidgets.heroText(cv, s, x, cy, w, size, f, color, a, d);
    }

    private static void remaining(Canvas cv, String prefix, String value, float x, float cy, float w, float size, Typeface reg, Typeface bold, int soft, int ink, float d) {
        String full = prefix + value;
        if (HomeWidgets.ovToken.get() != null && full.contains(HomeWidgets.ovToken.get()) && HomeWidgets.capture(full, (x + w / 2) * d, cy * d, w * d, HomeWidgets.fitSize(full, bold, size, w, d, 2), ink, 1)) return;
        full = com.example.utils.Digits.forWidget(full);
        SpannableString sp = new SpannableString(full);
        sp.setSpan(new android.text.style.ForegroundColorSpan(soft), 0, prefix.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new android.text.style.ForegroundColorSpan(ink), prefix.length(), full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sp.setSpan(new CustomTypefaceSpan(bold), 0, full.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        TextPaint tp = new TextPaint(Paint.ANTI_ALIAS_FLAG);
        tp.setTypeface(bold);
        tp.setTextSize(size * d);
        tp.setColor(soft);
        while (tp.measureText(full) > w * d && tp.getTextSize() > 8 * d) tp.setTextSize(tp.getTextSize() - 0.5f * d);
        StaticLayout l = StaticLayout.Builder.obtain(sp, 0, sp.length(), tp, Math.max(1, (int) (w * d)))
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setIncludePad(false).setMaxLines(1).build();
        cv.save();
        cv.translate(x * d, cy * d - l.getHeight() / 2f);
        l.draw(cv);
        cv.restore();
    }

    private static final class CustomTypefaceSpan extends android.text.style.MetricAffectingSpan {
        private final Typeface tf;
        CustomTypefaceSpan(Typeface tf) { this.tf = tf; }
        @Override public void updateDrawState(TextPaint p) { p.setTypeface(tf); }
        @Override public void updateMeasureState(TextPaint p) { p.setTypeface(tf); }
    }
}
