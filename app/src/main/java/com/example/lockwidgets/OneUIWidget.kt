package com.example.lockwidgets

import android.content.Context
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.BlurMaskFilter
import android.graphics.Canvas
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.Path
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.SpannableString
import android.text.Spanned
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import java.util.Locale

/** One UI style prayer progress card with a yellow date tile (mockup v4). Drawn like the other home widgets. */
internal object OneUIWidget {

    private fun lerp(a: Float, b: Float, t: Float): Float = a + (b - a) * t

    /** Per-channel ARGB blend: t=0 keeps a, t=1 gives b. */
    private fun blend(a: Int, b: Int, t: Float): Int {
        val r = (((a shr 16) and 0xFF) + (((b shr 16) and 0xFF) - ((a shr 16) and 0xFF)) * t).toInt()
        val g = (((a shr 8) and 0xFF) + (((b shr 8) and 0xFF) - ((a shr 8) and 0xFF)) * t).toInt()
        val bl = ((a and 0xFF) + (((b and 0xFF) - (a and 0xFF)) * t)).toInt()
        return 0xFF000000.toInt() or (r shl 16) or (g shl 8) or bl
    }

    // Accent themes (3.3.3, his call 22.10.2026): 0 = the classic yellow (default, values
    // unchanged); 1-3 reuse the watch app palette pairs (wear/Settings.kt WTheme.defs:
    // main, deep) so the widget matches the watch app colours exactly.
    private val ACCENT_MAIN = intArrayOf(0xFFFFD85C.toInt(), 0xFF44CCC0.toInt(), 0xFF3E9FFE.toInt(), 0xFF69D043.toInt())
    private val ACCENT_DEEP = intArrayOf(0xFFEBA800.toInt(), 0xFF169498.toInt(), 0xFF3580F8.toInt(), 0xFF07C364.toInt())

    /** Selected accent for a widget instance: fajr_prefs widget_oneui_color_<id>, default 0 (yellow). */
    fun accentForWidget(c: Context, widgetId: Int): Int {
        if (widgetId <= 0) return 0
        val v = c.getSharedPreferences("fajr_prefs", Context.MODE_PRIVATE).getInt("widget_oneui_color_$widgetId", 0)
        return if (v in 1..3) v else 0
    }

    fun render(c: Context, s: HomeWidgets.State, wd: Int, ht: Int, widgetId: Int): Bitmap {
        val d = c.resources.displayMetrics.density
        val os = HomeWidgets.oversample(c, wd, ht, d)
        val bmp = Bitmap.createBitmap(Math.max(1, (wd * d * os).toInt()), Math.max(1, (ht * d * os).toInt()), Bitmap.Config.ARGB_8888)
        val cv = Canvas(bmp)
        cv.scale(os, os)
        val night = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
        val W = wd.toFloat(); val H = ht.toFloat()
        val t = Math.max(0f, Math.min(1f, (H - 80f) / 100f)) // 0 = 4x1, 1 = 4x2
        val gap = Math.max(6f, W * 0.02f)
        val tileW = Math.min(H * 0.93f, W * 0.225f)
        // Large screens only: keep the date tile's shape (width/height = 0.93, the full-width look) when the card is width-limited, instead of letting it turn into a tall sliver; it is centred vertically.
        val dbg = c.getSharedPreferences("fajr_prefs", 0)
        val tileFix = HomeWidgets.large(c) && dbg.getBoolean("dbg_tilefix", true); val textFix = HomeWidgets.large(c) && dbg.getBoolean("dbg_textfix", true)
        val tileH = if (tileFix) Math.min(H, tileW / 0.8095f) else H // 0.8095 = 68/84, the observed known-good 302x84 tile
        val tileTop = (H - tileH) / 2f
        val cardW = W - tileW - gap
        val corner = Math.min(H * 0.30f, 34f)
        val card = RectF(0f, 0f, cardW, H)
        val tile = RectF(cardW + gap, tileTop, W, tileTop + tileH)
        val reg = HomeWidgets.face(c, false, s.rtl); val bold = HomeWidgets.face(c, true, s.rtl)

        val cardTop = if (night) 0xFF2B2C30.toInt() else 0xFFFFFFFF.toInt(); val cardBot = if (night) 0xFF222326.toInt() else 0xFFF1F2F5.toInt()
        val ink = if (night) 0xFFFFFFFF.toInt() else 0xFF1C1C1E.toInt()
        val soft = if (night) 0xFFB4B4BA.toInt() else 0xFF6C6C74.toInt()
        val track = if (night) 0xFF4A4A4F.toInt() else 0xFFDEDEE2.toInt()
        val accent = accentForWidget(c, widgetId)
        var barA: Int; var barB: Int; val tA: Int; val tB: Int; val tInk: Int
        if (accent == 0) {
            // Classic yellow: the exact shipped values, including the blue day bar.
            barA = if (night) 0xFFFFD85C.toInt() else 0xFF55A0FF.toInt(); barB = if (night) 0xFFF5B800.toInt() else 0xFF2A6BF0.toInt()
            tA = if (night) 0xFFFFD85C.toInt() else 0xFFFFE58F.toInt(); tB = if (night) 0xFFEBA800.toInt() else 0xFFFFC93A.toInt()
            tInk = if (night) 0xFF2A2200.toInt() else 0xFF3A2E00.toInt()
        } else {
            val m = ACCENT_MAIN[accent]; val deep = ACCENT_DEEP[accent]
            barA = m; barB = deep
            // Day tile derived like the yellow one: lighter top, mid-toned bottom; ink is a
            // dark shade of the accent so the date stays readable on the coloured tile.
            tA = if (night) m else blend(m, 0xFFFFFFFF.toInt(), 0.38f)
            tB = if (night) deep else blend(m, deep, 0.55f)
            tInk = blend(deep, 0xFF000000.toInt(), if (night) 0.78f else 0.68f)
        }
        // Iqama countdown: the bar takes the selected theme like everything else (his call
        // 22.10.2026 20:30). The flat amber override stays for the default yellow theme only,
        // where it is part of the shipped look.
        if (s.iqama && accent == 0) { barA = 0xFFE5B455.toInt(); barB = 0xFFE5B455.toInt() }

        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        // card
        p.shader = LinearGradient(0f, 0f, 0f, H * d, cardTop, cardBot, Shader.TileMode.CLAMP)
        cv.drawRoundRect(RectF(card.left * d, card.top * d, card.right * d, card.bottom * d), corner * d, corner * d, p)
        p.shader = null
        // pattern (default Rub el Hizb, per-widget setting), fading from the bottom-left corner
        cv.saveLayer(0f, 0f, cardW * d, H * d, null)
        val clip = Path()
        clip.addRoundRect(RectF(0f, 0f, cardW * d, H * d), corner * d, corner * d, Path.Direction.CW)
        cv.clipPath(clip)
        val sp = Paint(Paint.ANTI_ALIAS_FLAG)
        sp.style = Paint.Style.STROKE
        sp.strokeWidth = Math.max(0.8f, 0.9f * d)
        sp.color = if (night) 0xFFFFFFFF.toInt() else 0xFF7C8396.toInt()
        sp.alpha = if (night) 48 else 70
        val R = H * lerp(0.115f, 0.092f, t)
        WidgetPatterns.draw(cv, WidgetPatterns.forWidget(c, widgetId), cardW * d, H * d, R * d, sp)
        val fade = Paint()
        fade.shader = LinearGradient(0f, H * d, cardW * d * 0.9f, 0f, 0xFF000000.toInt(), 0x00000000, Shader.TileMode.CLAMP)
        fade.xfermode = PorterDuffXfermode(PorterDuff.Mode.DST_IN)
        cv.drawRect(0f, 0f, cardW * d, H * d, fade)
        cv.restore()

        // text rows (fractions of card height, interpolated between 4x1 and 4x2)
        val nameY = H * lerp(0.21f, 0.20f, t); val barY = H * lerp(0.385f, 0.43f, t); val timeY = H * lerp(0.565f, 0.63f, t); val remY = H * lerp(0.795f, 0.84f, t)
        var nameSz = H * lerp(0.162f, 0.116f, t); var timeSz = H * lerp(0.152f, 0.096f, t); var remSz = H * lerp(0.165f, 0.108f, t)
        // Large screens only: when the card is narrower than the full-width one (resized down), scale the text with the card width so the rows keep their air. Full width (ratio >= 2.71) and phones are unchanged.
        if (textFix) { val nf = Math.max(0.78f, Math.min(1f, (cardW / H) / 2.71f)); nameSz *= nf; timeSz *= nf; remSz *= nf }
        val padX = Math.min(24f, cardW * 0.06f); val inner = cardW - 2 * padX; val side = inner * 0.46f
        val nextName = s.targetName; val prevName = s.names[s.previous]
        val nextTime = time(c, s, s.at); val prevTime = time(c, s, s.prev)
        val xNext = padX + inner - side; val xPrev = padX // previous prayer on the left, next on the right, as in the mockup
        val nextA = Layout.Alignment.ALIGN_OPPOSITE
        val prevA = Layout.Alignment.ALIGN_NORMAL
        line(cv, nextName, xNext, nameY, side, nameSz, bold, ink, nextA, d)
        line(cv, prevName, xPrev, nameY, side, nameSz, bold, ink, prevA, d)
        line(cv, nextTime, xNext, timeY, side, timeSz, reg, ink, nextA, d)
        line(cv, prevTime, xPrev, timeY, side, timeSz, reg, ink, prevA, d)

        // progress bar
        val bh = Math.max(6f, H * lerp(0.125f, 0.10f, t))
        val progress = Math.max(0.02f, Math.min(1f, (s.now - s.prev) / Math.max(1, s.at - s.prev).toFloat()))
        val tr = RectF(padX * d, (barY - bh / 2) * d, (padX + inner) * d, (barY + bh / 2) * d)
        p.color = track
        cv.drawRoundRect(tr, bh * d / 2, bh * d / 2, p)
        val fw = inner * progress
        val fr = RectF(padX * d, tr.top, (padX + fw) * d, tr.bottom) // fills left to right
        val glow = Paint(Paint.ANTI_ALIAS_FLAG)
        glow.color = barB
        glow.alpha = if (night) 70 else 60
        glow.maskFilter = BlurMaskFilter(bh * d * 0.9f, BlurMaskFilter.Blur.NORMAL)
        cv.drawRoundRect(fr, bh * d / 2, bh * d / 2, glow)
        p.shader = LinearGradient(fr.left, 0f, fr.right, 0f, barA, barB, Shader.TileMode.CLAMP)
        cv.drawRoundRect(fr, bh * d / 2, bh * d / 2, p)
        p.shader = null

        // remaining line: gray label + bold value (iqama: native ticking countdown replaces the value)
        val mins = Math.max(0, (s.at - s.now) / 60000)
        val prefix = HomeWidgets.remPrefix(s.code, ": ")
        val value = if (s.iqama) "⁦" + HomeWidgets.mmss(s.at - s.now) + "⁩" else HomeWidgets.durText(mins, s.code)
        HomeWidgets.ovSemi.set(true)
        remaining(cv, prefix, value, padX, remY, inner, remSz, reg, bold, soft, ink, d)

        // date tile (accent colours resolved above)
        val tp = RectF(tile.left * d, tile.top * d, tile.right * d, tile.bottom * d)
        p.shader = LinearGradient(0f, tile.top * d, 0f, tile.bottom * d, tA, tB, Shader.TileMode.CLAMP)
        cv.drawRoundRect(tp, corner * d * 1.05f, corner * d * 1.05f, p)
        p.shader = null
        val wdSz = tileH * lerp(0.127f, 0.07f, t); val numSz = tileH * lerp(0.42f, 0.245f, t); val mSz = tileH * lerp(0.118f, 0.065f, t)
        line(cv, s.day, tile.left + 4, tileTop + tileH * lerp(0.20f, 0.30f, t), tileW - 8, wdSz, reg, tInk, Layout.Alignment.ALIGN_CENTER, d)
        line(cv, s.hday, tile.left + 4, tileTop + tileH * lerp(0.50f, 0.50f, t), tileW - 8, numSz, bold, tInk, Layout.Alignment.ALIGN_CENTER, d)
        line(cv, s.hmonth, tile.left + 4, tileTop + tileH * lerp(0.80f, 0.70f, t), tileW - 8, mSz, reg, tInk, Layout.Alignment.ALIGN_CENTER, d)
        return bmp
    }

    private fun time(c: Context, s: HomeWidgets.State, at: Long): String {
        if (!s.code.startsWith("ar")) return HomeWidgets.tm(at, s.code)
        val cal = java.util.Calendar.getInstance()
        cal.timeInMillis = at
        val m = if (cal.get(java.util.Calendar.AM_PM) == 0) "ص" else "م"
        val hm = java.text.SimpleDateFormat("hh:mm", Locale.ENGLISH).format(java.util.Date(at))
        return "⁦$hm⁩ $m"
    }

    private fun line(cv: Canvas, s: String, x: Float, cy: Float, w: Float, size: Float, f: Typeface, color: Int, a: Layout.Alignment, d: Float) {
        HomeWidgets.heroText(cv, s, x, cy, w, size, f, color, a, d)
    }

    private fun remaining(cv: Canvas, prefix: String, value: String, x: Float, cy: Float, w: Float, size: Float, reg: Typeface, bold: Typeface, soft: Int, ink: Int, d: Float) {
        var full = prefix + value
        val tok = HomeWidgets.ovToken.get()
        if (tok != null && full.contains(tok) && HomeWidgets.capture(full, (x + w / 2) * d, cy * d, w * d, HomeWidgets.fitSize(full, bold, size, w, d, 2), ink, 1)) return
        full = com.example.utils.Digits.forWidget(full as String?) ?: full
        val sp = SpannableString(full)
        sp.setSpan(android.text.style.ForegroundColorSpan(soft), 0, prefix.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.setSpan(android.text.style.ForegroundColorSpan(ink), prefix.length, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        sp.setSpan(CustomTypefaceSpan(bold), 0, full.length, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE)
        val tp = TextPaint(Paint.ANTI_ALIAS_FLAG)
        tp.typeface = bold
        tp.textSize = size * d
        tp.color = soft
        while (tp.measureText(full) > w * d && tp.textSize > 8 * d) tp.textSize = tp.textSize - 0.5f * d
        val l = StaticLayout.Builder.obtain(sp, 0, sp.length, tp, Math.max(1, (w * d).toInt()))
                .setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR)
                .setIncludePad(false).setMaxLines(1).build()
        cv.save()
        cv.translate(x * d, cy * d - l.height / 2f)
        l.draw(cv)
        cv.restore()
    }

    private class CustomTypefaceSpan(private val tf: Typeface) : android.text.style.MetricAffectingSpan() {
        override fun updateDrawState(p: TextPaint) { p.typeface = tf }
        override fun updateMeasureState(p: TextPaint) { p.typeface = tf }
    }
}
