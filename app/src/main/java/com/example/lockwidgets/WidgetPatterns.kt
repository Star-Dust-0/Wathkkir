package com.example.lockwidgets

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Paint
import android.graphics.Path
import kotlin.math.cos
import kotlin.math.max
import kotlin.math.sin

/** Background patterns for the prayer widget card: 1 Rub el Hizb grid (default), 8 stars and diamonds, 9 hex lattice, 10 crossed octagram, 11 round arabesque. 0 = none. */
object WidgetPatterns {
    @JvmField val IDS = intArrayOf(1, 8, 9, 10, 11)

    private fun star(cx: Float, cy: Float, n: Int, ro: Float, ri: Float): Path {
        val p = Path()
        for (i in 0 until 2 * n) {
            val a = -Math.PI / 2 + i * Math.PI / n
            val rr = if (i % 2 == 0) ro else ri
            val x = cx + (cos(a) * rr).toFloat()
            val y = cy + (sin(a) * rr).toFloat()
            if (i == 0) p.moveTo(x, y) else p.lineTo(x, y)
        }
        p.close()
        return p
    }

    /** Draws the pattern from the bottom-left corner over a w x h area (pixels). [ro] is the star radius in pixels. */
    @JvmStatic
    fun draw(cv: Canvas, pat: Int, w: Float, h: Float, ro: Float, p: Paint) {
        if (pat <= 0) return
        if (pat == 1) {
            val step = ro * 2f
            var cy = h
            while (cy > -ro) {
                var cx = 0f
                while (cx < w + ro) {
                    cv.drawPath(star(cx, cy, 8, ro, ro * 0.7654f), p)
                    cx += step
                }
                cy -= step
            }
            return
        }
        val hex = pat == 9
        val dx = if (hex) ro * 1.732f else ro * 2f
        val dy = if (hex) ro * 1.5f else ro * 2f
        val rows = (h / dy).toInt() + 3
        val cols = (w / dx).toInt() + 3
        for (row in 0..rows) {
            for (col in 0..cols) {
                val cx = col * dx + (if ((hex || pat == 11) && row % 2 == 1) dx / 2 else 0f)
                val cy = h - row * dy
                when (pat) {
                    8 -> {
                        cv.drawPath(star(cx, cy, 8, ro, ro * 0.7654f), p)
                        val dm = Path()
                        val qx = cx + dx / 2
                        val qy = cy - dy / 2
                        val q = ro * 0.30f
                        dm.moveTo(qx, qy - q); dm.lineTo(qx + q, qy); dm.lineTo(qx, qy + q); dm.lineTo(qx - q, qy); dm.close()
                        cv.drawPath(dm, p)
                    }
                    9 -> {
                        cv.drawPath(star(cx, cy, 6, ro * 0.98f, ro * 0.577f), p)
                        cv.drawCircle(cx, cy, ro * 0.16f, p)
                    }
                    10 -> {
                        cv.drawPath(star(cx, cy, 8, ro, ro * 0.541f), p)
                        cv.drawPath(star(cx, cy, 8, ro * 0.5f, ro * 0.27f), p)
                    }
                    else -> {
                        cv.drawPath(star(cx, cy, 12, ro, ro * 0.5176f), p)
                        cv.drawCircle(cx, cy, ro * 0.22f, p)
                        cv.drawCircle(cx, cy, ro * 0.62f, p)
                    }
                }
            }
        }
    }

    /** Small preview square for the settings screen. */
    @JvmStatic
    fun thumb(pat: Int, px: Int, night: Boolean): Bitmap {
        val b = Bitmap.createBitmap(px, px, Bitmap.Config.ARGB_8888)
        val cv = Canvas(b)
        cv.drawColor(if (night) 0xFF2B2C30.toInt() else 0xFFF1F2F5.toInt())
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        p.style = Paint.Style.STROKE
        p.strokeWidth = max(1f, px * 0.012f)
        p.color = if (night) 0xFFFFFFFF.toInt() else 0xFF7C8396.toInt()
        p.alpha = if (night) 150 else 170
        draw(cv, pat, px.toFloat(), px.toFloat(), px * 0.2f, p)
        return b
    }

    @JvmStatic
    fun forWidget(c: Context, widgetId: Int): Int {
        val sp = c.getSharedPreferences("fajr_prefs", Context.MODE_PRIVATE)
        val v = if (widgetId > 0) sp.getInt("widget_pattern_$widgetId", 1) else 1
        for (id in IDS) if (id == v) return v
        return if (v == 0) 0 else 1
    }
}
