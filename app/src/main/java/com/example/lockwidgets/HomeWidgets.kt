package com.example.lockwidgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.res.Configuration
import android.graphics.Bitmap
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.LinearGradient
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.Shader
import android.graphics.Typeface
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.widget.RemoteViews
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

object HomeWidgets {
    @JvmField var openPrayer = false
    @JvmField val batchState = ThreadLocal<State>()

    // ---- Live iqama countdown overlay: the bitmap skips the MM:SS text and a native Chronometer ticks it every second.
    @JvmField val ovWanted = ThreadLocal<Boolean>()
    @JvmField val ovToken = ThreadLocal<String?>()
    @JvmField val ovBase = ThreadLocal<Long?>()
    @JvmField val ovSemi = ThreadLocal<Boolean>()
    @JvmField val ovGeo = ThreadLocal<FloatArray?>()
    @JvmField val ovFmt = ThreadLocal<String?>()

    @JvmStatic fun ovBegin() { ovSemi.remove(); ovWanted.set(true); ovToken.remove(); ovBase.remove(); ovGeo.remove(); ovFmt.remove() }
    @JvmStatic fun ovArm(iqama: Boolean, at: Long, now: Long) { if (iqama && ovWanted.get() != null) { ovToken.set(mmss(at - now)); ovBase.set(android.os.SystemClock.elapsedRealtime() + (at - System.currentTimeMillis())) } }
    @JvmStatic fun capture(s: String?, cx: Float, cy: Float, w: Float, size: Float, color: Int): Boolean = capture(s, cx, cy, w, size, color, -1)
    @JvmStatic fun capture(s: String?, cx: Float, cy: Float, w: Float, size: Float, color: Int, semi: Int): Boolean {
        val t = ovToken.get()
        if (t == null || ovGeo.get() != null || s == null || !s.contains(t)) return false
        ovGeo.set(floatArrayOf(cx, cy, w, size, color.toFloat(), semi.toFloat()))
        ovFmt.set(s.replace(t, "%s"))
        return true
    }
    @JvmStatic fun ovBits(c: Context, v: RemoteViews, prefix: String, value: Int) {
        var `val` = Math.max(0, Math.min(8191, value))
        for (i in 0 until 13) { val id = res(c, "id", prefix + i); if (id != 0) v.setViewVisibility(id, if ((`val` shr i) and 1 != 0) android.view.View.VISIBLE else android.view.View.GONE) }
    }
    /** Scale-free overlay: the text box is the same fraction of the displayed widget as the bitmap's text line (height 1.2 x size, the Expo Arabic line height), so it follows whatever scale or letterbox the launcher applies to the image. Chronometer keeps ticking natively. */
    @JvmStatic fun ovFrac(c: Context, v: RemoteViews, g: FloatArray, f: String, base: Long, W: Int, H: Int) {
        val lp = android.graphics.Paint(); lp.typeface = face(c, g[5] > 0, true); lp.textSize = g[3]
        val fm = lp.fontMetrics; val lh = fm.descent - fm.ascent
        android.util.Log.i("WathakkerHome", "ovFrac cx=${g[0]} cy=${g[1]} w=${g[2]} S=${g[3]} semi=${g[5]} lh=$lh W=$W H=$H")
        val tab = c.resources.configuration.smallestScreenWidthDp >= 600
        val hf = Math.min(1f, lh / H); val wf = Math.min(1f, Math.max(0.02f, g[2] / W))
        val tv = 256f / hf; val th = 256f / wf
        val topf = (if (tab) g[1] - lh / 2f else g[1] - 0.62f * g[3]) / H; val leftf = g[0] / W - wf / 2f
        val vt = Math.round(Math.max(0f, topf) * tv); val vb = Math.round(Math.max(0f, tv - 256f - vt)); val hl = Math.round(Math.max(0f, leftf) * th); val hr = Math.round(Math.max(0f, th - 256f - hl))
        ovBits(c, v, "ov_vt", vt); ovBits(c, v, "ov_vb", vb); ovBits(c, v, "ov_hl", hl); ovBits(c, v, "ov_hr", hr)
        val semi = g[5] > 0
        val ch = res(c, "id", if (semi) "ov_f_semi" else "ov_f_med"); val other = res(c, "id", if (semi) "ov_f_med" else "ov_f_semi"); val stub = res(c, "id", "ov_stub")
        val b0 = res(c, "id", "ov_box"); val m0 = res(c, "id", "ov_chrono_med"); val s0 = res(c, "id", "ov_chrono_semi")
        if (b0 != 0) v.setViewVisibility(b0, android.view.View.GONE)
        if (m0 != 0) v.setViewVisibility(m0, android.view.View.GONE)
        if (s0 != 0) v.setViewVisibility(s0, android.view.View.GONE)
        v.setViewVisibility(stub, android.view.View.VISIBLE); v.setViewVisibility(other, android.view.View.GONE); v.setViewVisibility(ch, android.view.View.VISIBLE)
        v.setTextColor(ch, g[4].toInt()); v.setChronometerCountDown(ch, true); v.setChronometer(ch, base, f, true)
    }
    @JvmStatic fun ovApply(c: Context, v: RemoteViews, W: Int, H: Int) {
        try {
            val g = ovGeo.get(); val f = ovFmt.get(); val base = ovBase.get()
            if (g == null || f == null || base == null || android.os.Build.VERSION.SDK_INT < 24) {
                val st0 = res(c, "id", "ov_stub"); if (st0 != 0) v.setViewVisibility(st0, android.view.View.GONE)
                val b0 = res(c, "id", "ov_box"); val m0 = res(c, "id", "ov_chrono_med"); val s0 = res(c, "id", "ov_chrono_semi")
                if (b0 != 0) v.setViewVisibility(b0, android.view.View.GONE)
                if (m0 != 0) { v.setViewVisibility(m0, android.view.View.GONE); try { v.setChronometer(m0, android.os.SystemClock.elapsedRealtime(), null, false) } catch (t: Throwable) {} }
                if (s0 != 0) { v.setViewVisibility(s0, android.view.View.GONE); try { v.setChronometer(s0, android.os.SystemClock.elapsedRealtime(), null, false) } catch (t: Throwable) {} }
                return
            }
            if (large(c) && g.size > 5 && g[5] >= 0 && android.os.Build.VERSION.SDK_INT >= 26 && W > 0 && H > 0 && res(c, "id", "ov_stub") != 0) { ovFrac(c, v, g, f, base, W, H); return }
            /* phones use the exact 3.4.0 overlay below: One UI Home renders the scale-free autosize overlay ~40% small (position fine), while the fixed-pixel Chronometer matched the bitmap there; the fractional overlay stays the large-screen path (accepted tablet rendering). */
            val st1 = res(c, "id", "ov_stub"); if (st1 != 0) v.setViewVisibility(st1, android.view.View.GONE)
            val semi = if (g.size > 5 && g[5] >= 0) g[5] > 0 else (g[3] >= 20 * c.resources.displayMetrics.density || ovSemi.get() == true)
            val box = res(c, "id", "ov_box"); val ch = res(c, "id", if (semi) "ov_chrono_semi" else "ov_chrono_med"); val other = res(c, "id", if (semi) "ov_chrono_med" else "ov_chrono_semi")
            val left = Math.max(0, Math.round(g[0] - g[2] / 2)); val right = Math.max(0, Math.round(W - g[0] - g[2] / 2)); val top = Math.max(0, Math.round(g[1] - g[3] * 0.62f))
            v.setViewVisibility(box, android.view.View.VISIBLE); v.setViewVisibility(other, android.view.View.GONE); v.setViewVisibility(ch, android.view.View.VISIBLE)
            v.setViewPadding(box, left, top, right, 0); v.setTextViewTextSize(ch, android.util.TypedValue.COMPLEX_UNIT_PX, g[3]); v.setTextColor(ch, g[4].toInt()); v.setChronometerCountDown(ch, true); v.setChronometer(ch, base, f, true)
        } catch (e: Throwable) { android.util.Log.w("WathakkerHome", "overlay failed", e) }
        finally { ovSemi.remove(); ovWanted.remove(); ovToken.remove(); ovBase.remove(); ovGeo.remove(); ovFmt.remove() }
    }
    @Volatile @JvmField var regularFace: Typeface? = null
    @Volatile @JvmField var boldFace: Typeface? = null
    @JvmStatic fun initialPage(): Int = 0
    @JvmStatic fun mmss(ms: Long): String { val t = Math.max(0, ms / 1000); return String.format(Locale.ENGLISH, "%02d:%02d", t / 60, t % 60) }
    @JvmStatic fun handleIntent(i: Intent?): Boolean { openPrayer = i != null && "com.wathakker.locktest.OPEN_PRAYER" == i.action; return openPrayer }
    /** 3.4: per-widget tap response. Stored by the widget settings as widget_tap_<id> = "open" (default), "refresh" (3.4.2) or the legacy "none". */
    @JvmStatic fun tapOpens(c: Context, id: Int): Boolean = "open" == c.getSharedPreferences("fajr_prefs", 0).getString("widget_tap_$id", "open")
    /** 3.4.2: on Home widgets with a remaining-time/progress display the second tap option is "تحديث العرض" (refresh on tap + native Toast); the legacy "none" choice on those widgets upgrades to refresh. Date/schedule widgets keep the real "none" (no tap action). */
    @JvmField val REFRESH_KINDS: Set<String> = hashSetOf("Hero", "NextPrayer", "PrayerBar", "Clock", "Pill", "OneUI")
    @JvmStatic fun refreshIntent(c: Context, id: Int): PendingIntent { val i = Intent(c, com.example.widget.WidgetRefreshReceiver::class.java).setAction(com.example.widget.WidgetRefreshReceiver.ACTION).putExtra("appWidgetId", id); return PendingIntent.getBroadcast(c, id, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
    @JvmStatic fun tapIntent(c: Context, kind: String, id: Int): PendingIntent? { val tv = c.getSharedPreferences("fajr_prefs", 0).getString("widget_tap_$id", "open"); if ("open" == tv) return open(c, kind != "Hijri" && kind != "Gregorian"); if (REFRESH_KINDS.contains(kind)) return refreshIntent(c, id); return null }
    /** Tap-to-refresh: re-render every Home widget and return the tapped kind's remaining-time text computed from the SAME state, so the Toast matches the refreshed widget exactly. */
    @JvmStatic @Synchronized fun refreshAllAndText(c: Context, kind: String): String {
        com.example.utils.Digits.refresh(c)
        try { batchState.set(state(c)) } catch (e: Exception) { android.util.Log.e("WathakkerHome", "state() failed - tap refresh aborted", e); return remLabel(lang(c)) }
        try { updateBatch(c, false); return remainingText(batchState.get(), kind) }
        finally { batchState.remove(); LockWidgets.schedule(c); WidgetPreviews.maybeGenerate(c) }
    }
    /** The exact remaining-time string [kind] draws for state [s]: Clock shows its HH:MM countdown (MM:SS in iqama), the others the "المتبقي" line; the widget digit style is applied like the rendered text. */
    @JvmStatic fun remainingText(s: State?, kind: String): String {
        if (s == null) return ""
        val t: String
        if ("Clock" == kind) { val sec = Math.max(0, (s.at - s.now) / 1000); t = if (s.iqama) String.format(Locale.ENGLISH, "%02d:%02d", sec / 60, sec % 60) else String.format(Locale.ENGLISH, "%02d:%02d", sec / 3600, sec / 60 % 60) }
        else { val mins = Math.max(0, (s.at - s.now) / 60000); t = if (s.iqama) remPrefix(s.code, ": ") + "⁦" + mmss(s.at - s.now) + "⁩" else remaining(mins, s.code) }
        return com.example.utils.Digits.forWidget(t as String?) ?: t
    }
    @JvmStatic fun res(c: Context, t: String, n: String): Int = c.resources.getIdentifier(n, t, c.packageName)
    @JvmStatic fun open(c: Context, prayer: Boolean): PendingIntent { val i = Intent().setClassName(c.packageName, "com.example.MainActivity").setAction(if (prayer) "com.wathakker.locktest.OPEN_PRAYER" else Intent.ACTION_MAIN).setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP or Intent.FLAG_ACTIVITY_SINGLE_TOP); return PendingIntent.getActivity(c, if (prayer) 91212 else 91213, i, PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE) }
    @JvmStatic @Synchronized fun updateAll(c: Context) { updateAll(c, false) }
    @JvmStatic @Synchronized fun updateAll(c: Context, pulseOnly: Boolean) {
        com.example.utils.Digits.refresh(c)
        try { batchState.set(state(c)) } catch (e: Exception) { android.util.Log.e("WathakkerHome", "state() failed - widget update aborted", e); return }
        try { updateBatch(c, pulseOnly) } finally { batchState.remove(); LockWidgets.schedule(c); WidgetPreviews.maybeGenerate(c) }
    }
    /* 3.4: every Home widget is drawn at the size the launcher reports for the CURRENT orientation (landscape = MAX_WIDTH x MIN_HEIGHT, portrait = MIN_WIDTH x MAX_HEIGHT, per the AppWidgetManager docs), so the bitmap is at the displayed pixel size and is never stretched or upscaled. Before, only the decorated widget did this; the other nine used the portrait width and were stretched on a landscape tablet. */
    /** Large screens only (smallest width 600dp+, evaluated from the current configuration so display-size/DPI changes are followed): widgets are drawn at 2x the size the launcher reports (1x when that would be too big for a RemoteViews bitmap) and shrunk by the ImageView, so a launcher that shows the widget larger than it reports (zoomed widgets, density differences) is never upscaled. All geometry stays in the 1x pixel space. */
    @JvmStatic fun large(c: Context): Boolean = c.resources.configuration.smallestScreenWidthDp >= 600
    @JvmField val osCap = ThreadLocal<Float?>()
    /** Half of the framework's per-update bitmap allowance (about 6 x screen pixels in bytes), so an update is never rejected for memory. */
    @JvmStatic fun bitmapBudget(c: Context): Long { val dm = c.resources.displayMetrics; return (3.0 * dm.widthPixels * dm.heightPixels).toLong() }
    @JvmStatic fun oversample(c: Context, wd: Int, ht: Int, d: Float): Float {
        if (!large(c)) return 1f
        var os = 2f; val cap = osCap.get(); if (cap != null) os = Math.min(os, cap)
        val lim = Math.min(6000000L, bitmapBudget(c))
        while (os > 1f && (wd * d * os) * (ht * d * os) * 4 > lim) os -= 0.25f
        return Math.max(1f, os)
    }
    @JvmStatic fun view(c: Context, kind: String, id: Int, wd: Int, ht: Int, one: Boolean): RemoteViews {
        val v = RemoteViews(c.packageName, res(c, "layout", if (one) "home_prayer_art_fit" else "home_prayer_art"))
        ovBegin()
        val ob = renderKind(c, kind, wd, ht, id)
        if (one) try { ob.setDensity(1) } catch (e: Throwable) { android.util.Log.w("WathakkerHome", "setDensity failed", e) }
        v.setImageViewBitmap(res(c, "id", "home_prayer_image"), ob)
        ovApply(c, v, (wd * c.resources.displayMetrics.density).toInt(), (ht * c.resources.displayMetrics.density).toInt())
        val rt = res(c, "id", "home_prayer_root")
        val pi = tapIntent(c, kind, id)
        v.setOnClickPendingIntent(res(c, "id", "home_prayer_image"), pi)
        if (one && rt != 0) v.setOnClickPendingIntent(rt, pi)
        return v
    }
    @JvmStatic fun slackKey(z: android.util.SizeF): android.util.SizeF = android.util.SizeF(Math.max(1f, z.width - 3f), Math.max(1f, z.height - 3f))
    @JvmStatic fun cornerSizes(mnW: Int, mxW: Int, mnH: Int, mxH: Int): java.util.ArrayList<android.util.SizeF> {
        val r = java.util.ArrayList<android.util.SizeF>()
        val cs = arrayOf(intArrayOf(mnW, mnH), intArrayOf(mxW, mnH), intArrayOf(mnW, mxH), intArrayOf(mxW, mxH))
        for (q in cs) { val z = android.util.SizeF(q[0].toFloat(), q[1].toFloat()); if (!r.contains(z)) r.add(z) }
        return r
    }
    @JvmStatic fun updateBatch(c: Context) { updateBatch(c, false) }
    @JvmStatic fun updateBatch(c: Context, pulseOnly: Boolean) {
        android.util.Log.i("WathakkerHome", "updateBatch pulseOnly=$pulseOnly t=${System.currentTimeMillis()}")
        val m = AppWidgetManager.getInstance(c)
        val providers = arrayOf("Hero", "NextPrayer", "PrayerBar", "Schedule", "DayPrayer", "Pill", "Clock", "Hijri", "Gregorian", "OneUI")
        for (kind in providers) {
            if (pulseOnly && (kind == "Hijri" || kind == "Gregorian" || kind == "Schedule" || kind == "DayPrayer")) continue
            for (id in m.getAppWidgetIds(ComponentName(c, "com.example.widget.${kind}WidgetProvider"))) {
                try {
                    val b = m.getAppWidgetOptions(id)
                    val dw = if (kind.startsWith("Hero") || kind == "Schedule" || kind == "OneUI") 330 else if (kind == "Clock") 250 else if (kind == "Hijri" || kind == "Gregorian") 90 else 110
                    val dh = 90
                    val one = kind == "OneUI" || large(c)
                    val port = c.resources.configuration.orientation != Configuration.ORIENTATION_LANDSCAPE
                    val wd = Math.max(50, b.getInt(if (!port) AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH else AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, dw))
                    val ht = Math.max(if (kind == "Hero") 40 else 60, b.getInt(if (port) AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT else AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT, dh))
                    var sz: java.util.ArrayList<android.util.SizeF>? = if (large(c) && android.os.Build.VERSION.SDK_INT >= 31) b.getParcelableArrayList(AppWidgetManager.OPTION_APPWIDGET_SIZES) else null
                    if (large(c) && android.os.Build.VERSION.SDK_INT >= 31 && (sz == null || sz.isEmpty())) {
                        val mnW = b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH); val mxW = b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH); val mnH = b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT); val mxH = b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)
                        if (mnW > 0 && mxW > 0 && mnH > 0 && mxH > 0) sz = cornerSizes(mnW, mxW, mnH, mxH)
                    }
                    android.util.Log.i("WathakkerHome", "size $kind#$id sw=${c.resources.configuration.smallestScreenWidthDp}dp density=${c.resources.displayMetrics.density} min=${b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH)}x${b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_HEIGHT)} max=${b.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH)}x${b.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_HEIGHT)} sizes=$sz used=${wd}x$ht")
                    val v: RemoteViews
                    if (sz != null && !sz.isEmpty()) {
                        val want = java.util.LinkedHashMap<android.util.SizeF, IntArray>()
                        for (z in sz) { if (want.size >= 4) break; val zw = Math.max(50, Math.round(z.width)); val zh = Math.max(if (kind == "Hero") 40 else 60, Math.round(z.height)); want[slackKey(z)] = intArrayOf(zw, zh) }
                        val dd = c.resources.displayMetrics.density
                        var cap = 2f
                        while (cap > 1f) { var tot = 0.0; for (q in want.values) tot += (q[0] * dd * cap) * (q[1] * dd * cap) * 4; if (tot <= bitmapBudget(c)) break; cap -= 0.25f }
                        osCap.set(cap)
                        val mp = java.util.HashMap<android.util.SizeF, RemoteViews>()
                        try { for (en in want.entries) mp[en.key] = view(c, kind, id, en.value[0], en.value[1], one) } finally { osCap.remove() }
                        v = RemoteViews(mp)
                    } else v = view(c, kind, id, wd, ht, one)
                    try { m.updateAppWidget(id, v) }
                    catch (tooBig: IllegalArgumentException) {
                        android.util.Log.w("WathakkerHome", "update rejected, retrying single 1x bitmap: $kind", tooBig)
                        osCap.set(1f)
                        try { m.updateAppWidget(id, view(c, kind, id, wd, ht, one)) } finally { osCap.remove() }
                    }
                } catch (e: Exception) { android.util.Log.e("WathakkerHome", "Update failed: $kind", e) }
            }
        }
    }
    @JvmStatic @Throws(Exception::class) fun times(c: Context, day: Calendar): LongArray = LockWidgets.timesCached(c, day)
    @JvmStatic fun lang(c: Context): String = c.getSharedPreferences("app_prefs", 0).getString("language", "ar") ?: "ar"
    @JvmStatic fun ar(c: Context): Boolean = lang(c).startsWith("ar")
    @JvmStatic fun rtl(c: Context): Boolean { val x = lang(c); return x.startsWith("ar") || x.startsWith("ur") }
    @JvmStatic fun face(c: Context, bold: Boolean): Typeface = face(c, bold, rtl(c))
    @JvmStatic fun face(c: Context, bold: Boolean, rtl: Boolean): Typeface {
        if (!rtl) return Typeface.create(if (bold) "sans-serif" else "sans-serif-medium", if (bold) Typeface.BOLD else Typeface.NORMAL)
        var t = if (bold) boldFace else regularFace
        if (t == null) { t = Typeface.createFromAsset(c.assets, "homefonts/expoarabic_" + (if (bold) "semibold" else "medium") + ".ttf"); if (bold) boldFace = t else regularFace = t }
        return t!!
    }
    @JvmStatic fun fitSize(str: String, f: Typeface, size: Float, width: Float, d: Float, mode: Int): Float {
        try {
            var s = str
            val t = ovToken.get(); if (t != null) s = s.replace(t, "00:00")
            s = com.example.utils.Digits.forWidget(s as String?) ?: s
            val p = TextPaint(Paint.ANTI_ALIAS_FLAG); p.typeface = f; p.textSize = size * d
            if (mode == 0) {
                while (p.measureText(s) > Math.max(1f, width - 2) * d && p.textSize > 6 * d) p.textSize = p.textSize - .25f * d
                var l = StaticLayout.Builder.obtain(s, 0, s.length, p, Math.max(1, (width * d).toInt())).setIncludePad(false).setMaxLines(2).build()
                while (l.lineCount > 1 && p.textSize > 6 * d) { p.textSize = p.textSize - .25f * d; l = StaticLayout.Builder.obtain(s, 0, s.length, p, Math.max(1, (width * d).toInt())).setIncludePad(false).setMaxLines(2).build() }
            } else if (mode == 1) {
                while (p.measureText(s) > width * d && p.textSize > 8 * d * Math.min(1f, size / 12f)) p.textSize = p.textSize - .5f * d
            } else {
                while (p.measureText(s) > width * d && p.textSize > 8 * d) p.textSize = p.textSize - .5f * d
            }
            return p.textSize
        } catch (e: Throwable) { return size * d }
    }
    @JvmStatic fun text(cv: Canvas, str: String?, x: Float, y: Float, width: Float, size: Float, f: Typeface, color: Int, alignIn: Layout.Alignment, d: Float) {
        var s = str
        val tok = ovToken.get()
        if (tok != null && s != null && s.contains(tok) && capture(s, (x + width / 2) * d, y * d, width * d, fitSize(s, f, size, width, d, 0), color, if (f == boldFace || f.isBold) 1 else 0)) return
        s = (com.example.utils.Digits.forWidget(s as String?) ?: s) ?: ""
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG); p.typeface = f; p.textSize = size * d; p.color = color
        while (p.measureText(s) > Math.max(1f, width - 2) * d && p.textSize > 6 * d) p.textSize = p.textSize - .25f * d
        var align = alignIn
        if (TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(s, 0, s.length)) { if (align == Layout.Alignment.ALIGN_NORMAL) align = Layout.Alignment.ALIGN_OPPOSITE else if (align == Layout.Alignment.ALIGN_OPPOSITE) align = Layout.Alignment.ALIGN_NORMAL }
        var l = StaticLayout.Builder.obtain(s, 0, s.length, p, Math.max(1, (width * d).toInt())).setAlignment(align).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).setMaxLines(2).build()
        while (l.lineCount > 1 && p.textSize > 6 * d) { p.textSize = p.textSize - .25f * d; l = StaticLayout.Builder.obtain(s, 0, s.length, p, Math.max(1, (width * d).toInt())).setAlignment(align).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).setMaxLines(2).build() }
        cv.save(); cv.translate(x * d, y * d - l.height / 2f); l.draw(cv); cv.restore()
    }
    @JvmStatic fun heroText(cv: Canvas, str: String?, x: Float, y: Float, width: Float, size: Float, f: Typeface, color: Int, alignIn: Layout.Alignment, d: Float) {
        var s = str
        val tok = ovToken.get()
        if (tok != null && s != null && s.contains(tok) && capture(s, (x + width / 2) * d, y * d, width * d, fitSize(s, f, size, width, d, 1), color, if (f == boldFace || f.isBold) 1 else 0)) return
        s = (com.example.utils.Digits.forWidget(s as String?) ?: s) ?: ""
        val p = TextPaint(Paint.ANTI_ALIAS_FLAG); p.typeface = f; p.textSize = size * d; p.color = color
        while (p.measureText(s) > width * d && p.textSize > 8 * d * Math.min(1f, size / 12f)) p.textSize = p.textSize - .5f * d
        var align = alignIn
        if (TextDirectionHeuristics.FIRSTSTRONG_LTR.isRtl(s, 0, s.length)) { if (align == Layout.Alignment.ALIGN_NORMAL) align = Layout.Alignment.ALIGN_OPPOSITE else if (align == Layout.Alignment.ALIGN_OPPOSITE) align = Layout.Alignment.ALIGN_NORMAL }
        val l = StaticLayout.Builder.obtain(s, 0, s.length, p, Math.max(1, (width * d).toInt())).setAlignment(align).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).setMaxLines(1).build()
        cv.save(); cv.translate(x * d, y * d - l.height / 2f); l.draw(cv); cv.restore()
    }
    @JvmStatic fun tm(x: Long, code: String): String {
        val c = Calendar.getInstance(); c.timeInMillis = x
        val l10n = com.example.ui.i18n.L10n
        val marker = if (c.get(Calendar.AM_PM) == 0) l10n.t(com.example.ui.i18n.S.AmMarker) else l10n.t(com.example.ui.i18n.S.PmMarker)
        return "⁦" + SimpleDateFormat("h:mm", Locale.ENGLISH).format(Date(x)) + "⁩ " + marker
    }
    @JvmStatic fun durText(mins: Long, code: String): String {
        val h = mins / 60; val m = mins % 60
        if (code.startsWith("ar")) {
            val hp = if (h == 1L) "ساعة" else if (h == 2L) "ساعتان" else if (h in 3..10) "$h ساعات" else "$h ساعة"
            val mp = if (m == 1L) "دقيقة" else if (m == 2L) "دقيقتان" else if (m in 3..10) "$m دقائق" else "$m دقيقة"
            if (h > 0 && m > 0) return "$hp و $mp"; if (h > 0) return hp; if (m > 0) return mp; return "أقل من دقيقة"
        }
        if (code.startsWith("en")) {
            if (h > 0 && m > 0) return "${h}h ${m}m"; if (h > 0) return h.toString() + (if (h == 1L) " hour" else " hours"); if (m > 0) return m.toString() + (if (m == 1L) " minute" else " minutes"); return "<1 min"
        }
        val hu: String; val mu: String
        if (code.startsWith("de")) { hu = "Std."; mu = "Min." }
        else if (code.startsWith("tr")) { hu = "sa"; mu = "dk" }
        else if (code.startsWith("id")) { hu = "jam"; mu = "mnt" }
        else if (code.startsWith("ru")) { hu = "ч"; mu = "мин" }
        else if (code.startsWith("ur")) { hu = "گھنٹے"; mu = "منٹ" }
        else { hu = "h"; mu = "min" }
        if (h > 0 && m > 0) return "$h $hu $m $mu"; if (h > 0) return "$h $hu"; if (m > 0) return "$m $mu"; return "<1 $mu"
    }
    @JvmStatic fun adhanWord(code: String): String { if (code.startsWith("ar")) return "أذان"; if (code.startsWith("ur")) return "اذان"; return "Adhan" }
    @JvmStatic fun remPrefix(code: String, other: String): String = if (code.startsWith("ar")) "المتبقي : " else remLabel(code) + other
    @JvmStatic fun remaining(mins: Long, code: String): String = remPrefix(code, " ") + durText(mins, code)
    @JvmStatic fun remLabel(code: String): String {
        if (code.startsWith("ar")) return "المتبقي"; if (code.startsWith("en")) return "Remaining"; if (code.startsWith("fr")) return "Reste"; if (code.startsWith("de")) return "Verbleibend"; if (code.startsWith("es")) return "Quedan"; if (code.startsWith("it")) return "Rimangono"; if (code.startsWith("tr")) return "Kalan"; if (code.startsWith("ur")) return "باقی"; if (code.startsWith("id")) return "Tersisa"; if (code.startsWith("ru")) return "Осталось"; if (code.startsWith("pt")) return "Restam"; return "Remaining"
    }
    class State {
        @JvmField var now: Long = System.currentTimeMillis(); @JvmField var at: Long = 0; @JvmField var prev: Long = 0
        @JvmField var ts: LongArray = LongArray(0); @JvmField var next = 0; @JvmField var previous = 0
        @JvmField var names: Array<String> = arrayOf(); @JvmField var targetName: String = ""
        @JvmField var iqama = false
        @JvmField var city: String = ""; @JvmField var day: String = ""; @JvmField var hijri: String = ""; @JvmField var greg: String = ""; @JvmField var hmonth: String = ""; @JvmField var gmonth: String = ""; @JvmField var hday: String = ""; @JvmField var gday: String = ""; @JvmField var hyear: String = ""; @JvmField var gyear: String = ""; @JvmField var code: String = ""
        @JvmField var rtl = false
    }
    @JvmStatic @Throws(Exception::class) fun state(c: Context): State {
        val s = State(); s.code = lang(c); s.rtl = rtl(c)
        com.example.ui.i18n.L10n.init(c)
        val l = Locale(s.code)
        val day = Calendar.getInstance()
        s.ts = times(c, day)
        val lx = com.example.ui.i18n.L10n
        s.names = arrayOf(lx.t(com.example.ui.i18n.S.PrayerFajr), lx.t(com.example.ui.i18n.S.PrayerShuruq), lx.t(com.example.ui.i18n.S.PrayerDhuhr), lx.t(com.example.ui.i18n.S.PrayerAsr), lx.t(com.example.ui.i18n.S.PrayerMaghrib), lx.t(com.example.ui.i18n.S.PrayerIsha))
        if (day.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) s.names[2] = lx.t(com.example.ui.i18n.S.PrayerJumuah)
        s.next = -1
        for (i in 0 until 6) if (i != 1 && s.ts[i] > s.now) { s.next = i; break }
        if (s.next < 0) { val t = day.clone() as Calendar; t.add(Calendar.DATE, 1); s.at = times(c, t)[0]; s.next = 0; s.prev = s.ts[5]; s.previous = 5 }
        else {
            s.at = s.ts[s.next]; s.previous = if (s.next == 2) 0 else s.next - 1
            if (s.next == 0) { val y = day.clone() as Calendar; y.add(Calendar.DATE, -1); s.prev = times(c, y)[5]; s.previous = 5 } else s.prev = s.ts[s.previous]
        }
        val yd = day.clone() as Calendar; yd.add(Calendar.DATE, -1)
        val td = day.clone() as Calendar; td.add(Calendar.DATE, 1)
        val phase = PrayerPhase.resolve(s.ts, times(c, yd), times(c, td), s.now, day.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY, false)
        s.next = phase.targetIndex; s.previous = phase.previousIndex; s.at = phase.target; s.prev = phase.start; s.iqama = phase.iqama; s.targetName = phase.label(s.names, s.code)
        s.city = com.example.fajr.data.FajrPreferences(c).getCityNameLocalized(s.code)
        s.day = SimpleDateFormat("EEEE", l).format(Date())
        val hc = android.icu.util.IslamicCalendar(); hc.setCalculationType(android.icu.util.IslamicCalendar.CalculationType.ISLAMIC_UMALQURA)
        val hoff = c.getSharedPreferences("fajr_prefs", 0).getInt("hijri_offset", 0); if (hoff != 0) hc.add(Calendar.DATE, hoff)
        s.hday = "" + hc.get(Calendar.DATE); s.hyear = "" + hc.get(Calendar.YEAR)
        s.hmonth = com.example.prayer.HijriDate.monthName(hc.get(Calendar.MONTH) + 1, s.code)
        s.hijri = s.hday + " " + s.hmonth
        s.greg = SimpleDateFormat("d MMMM", l).format(Date()); s.gday = SimpleDateFormat("d", Locale.ENGLISH).format(Date()); s.gmonth = SimpleDateFormat("MMMM", l).format(Date()); s.gyear = "" + day.get(Calendar.YEAR)
        return s
    }
    @JvmStatic fun barFillColors(c: Context, id: Int): IntArray {
        var k = if (id > 0) c.getSharedPreferences("fajr_prefs", 0).getString("widget_bar_color_$id", null) else null
        if (k == null) k = c.getSharedPreferences("fajr_prefs", 0).getString("widget_bar_color", "auto")
        if ("amber" == k) return intArrayOf(0xFFE5B455.toInt(), 0xFFE5B455.toInt()); if ("blue" == k) return intArrayOf(0xFF41A0FF.toInt(), 0xFF3B83FE.toInt()); if ("green" == k) return intArrayOf(0xFF5BC950.toInt(), 0xFF24C55E.toInt()); if ("purple" == k) return intArrayOf(0xFF9747FF.toInt(), 0xFF9747FF.toInt())
        val autoCol = if (isLight(bgColor(c, id))) 0xFF000000.toInt() else 0xFFFFFFFF.toInt()
        return intArrayOf(autoCol, autoCol)
    }
    @JvmField var wpId = -9
    @JvmField var wpBlur: Bitmap? = null
    @JvmStatic fun nightMode(c: Context): Boolean = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
    /** Translucent tint laid over the blurred wallpaper so text stays readable: dark smoke at night, milky white by day. */
    @JvmStatic fun blendOver(base: Int, tint: Int): Int {
        val a = tint ushr 24
        val r = (((base shr 16) and 255) * (255 - a) + ((tint shr 16) and 255) * a) / 255
        val g = (((base shr 8) and 255) * (255 - a) + ((tint shr 8) and 255) * a) / 255
        val b = ((base and 255) * (255 - a) + (tint and 255) * a) / 255
        return (r shl 16) or (g shl 8) or b
    }
    @JvmStatic fun wallBlur(c: Context): Bitmap? = wallBlur(c, false)
    @JvmField var wpMesh: Bitmap? = null
    @JvmField var wpReal: Bitmap? = null
    @JvmField var wpMeshId = -2
    @JvmField var wpRealId = -2
    /** REVIEW-ONLY hook used by the debug GlassProtoCheck harness: inject the colour mesh a wallpaper
     *  would produce so the mock glass/frosted boards can be rendered over wallpapers the emulator
     *  does not have. Null clears the override so production state is untouched. Never called from
     *  release UI code. */
    @JvmStatic fun debugOverrideMeshForReview(c: Context, mesh: Bitmap?) {
        wpMesh = mesh; wpLight = null; wpLightId = -2
        if (mesh == null) { wpMeshId = -2; return }
        try { wpMeshId = android.app.WallpaperManager.getInstance(c).getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM) } catch (t: Throwable) { wpMeshId = -1 }
    }
    /** real=true: the actual wallpaper image. Verified on an API 34 emulator (glass_proto.txt) and per
     *  developer.android.com/reference/android/app/WallpaperManager: on Android 13 the system returns
     *  the DEFAULT wallpaper or throws SecurityException, on Android 14+ getWallpaperFile ALWAYS throws,
     *  and only MANAGE_EXTERNAL_STORAGE (Play-restricted, answer 10467955) or the privileged
     *  READ_WALLPAPER_INTERNAL unlock the real image - no normal runtime permission (incl.
     *  READ_MEDIA_IMAGES) helps. So real=true falls back to the colour mesh on modern Android.
     *  real=false: the frosted colour mesh only (WallpaperColors, permissionless). */
    @JvmStatic fun wallBlur(c: Context, real: Boolean): Bitmap? {
        val wm = android.app.WallpaperManager.getInstance(c)
        var id = -1; try { id = wm.getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM) } catch (t: Throwable) { android.util.Log.w("WathakkerHome", "wallpaperId failed", t) }
        if (real && wpReal != null && id == wpRealId) return wpReal
        if (!real && wpMesh != null && id == wpMeshId) return wpMesh
        var small: Bitmap? = null
        var fromImage = false
        // 1) the real wallpaper, when asked for and the system lets this app read it
        var why = ""
        if (real) {
            var full: Bitmap? = null
            try {
                val d = wm.drawable
                if (d != null) {
                    var w = d.intrinsicWidth; var h = d.intrinsicHeight; if (w <= 0 || h <= 0) { w = 720; h = 1280 }
                    val sw = 48; val sh = Math.max(1, 48 * h / w)
                    full = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                    val cv = Canvas(full); d.setBounds(0, 0, sw, sh); d.draw(cv)
                } else why += "getDrawable=null;"
            } catch (t: Throwable) { why += "getDrawable:$t"; full = null }
            if (full == null) try {
                val pfd = wm.getWallpaperFile(android.app.WallpaperManager.FLAG_SYSTEM)
                if (pfd != null) {
                    val o = android.graphics.BitmapFactory.Options(); o.inSampleSize = 8
                    val b = android.graphics.BitmapFactory.decodeFileDescriptor(pfd.fileDescriptor, null, o); pfd.close()
                    if (b != null) { val sw = 48; val sh = Math.max(1, 48 * b.height / b.width); full = Bitmap.createScaledBitmap(b, sw, sh, true) } else why += "decode=null;"
                } else why += "file=null;"
            } catch (t: Throwable) { why += "getWallpaperFile:$t" }
            if (full != null) { small = full; fromImage = true }
        }
        // 2) no wallpaper access (Android 13+ without storage permission): paint soft blobs from the wallpaper's own colours
        if (small == null && real) { android.util.Log.i("WallBlur", "real image unavailable: $why"); return wallBlur(c, false) }
        if (small == null) {
            try {
                var c1 = 0xFF5B6FA6.toInt(); var c2 = 0xFF8E6BB0.toInt(); var c3 = 0xFF4FA0C0.toInt()
                if (android.os.Build.VERSION.SDK_INT >= 27) {
                    val wc = wm.getWallpaperColors(android.app.WallpaperManager.FLAG_SYSTEM)
                    if (wc != null) { c1 = wc.primaryColor.toArgb(); c2 = if (wc.secondaryColor != null) wc.secondaryColor!!.toArgb() else c1; c3 = if (wc.tertiaryColor != null) wc.tertiaryColor!!.toArgb() else c2 }
                }
                val sw = 48; val sh = 32
                small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888)
                val cv = Canvas(small); cv.drawColor(0xFF000000.toInt() or c1)
                val p = android.graphics.Paint(android.graphics.Paint.ANTI_ALIAS_FLAG)
                p.shader = android.graphics.RadialGradient(sw * .12f, sh * .15f, sw * .62f, 0xFF000000.toInt() or c2, c2 and 0xFFFFFF, Shader.TileMode.CLAMP); cv.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), p)
                p.shader = android.graphics.RadialGradient(sw * .88f, sh * .9f, sw * .6f, 0xFF000000.toInt() or c3, c3 and 0xFFFFFF, Shader.TileMode.CLAMP); cv.drawRect(0f, 0f, sw.toFloat(), sh.toFloat(), p)
            } catch (t: Throwable) { return null }
        }
        // heavy blur: two box passes over the tiny bitmap, then it is scaled up at draw time
        boxBlur(small, 3); boxBlur(small, 3)
        android.util.Log.i("WallBlur", "real=$real source=" + (if (fromImage) "wallpaper-image" else "wallpaper-colours"))
        if (fromImage) { wpReal = small; wpRealId = id } else if (!real) { wpMesh = small; wpMeshId = id }
        return small
    }
    @JvmField var wpLight: Bitmap? = null
    @JvmField var wpLightId = -2
    /** Lighter blur for the glass (Morphe) prototype: larger thumb, one box pass - shapes stay readable. */
    @JvmStatic fun lightWallBlur(c: Context): Bitmap? {
        val wm = android.app.WallpaperManager.getInstance(c)
        var id = -1; try { id = wm.getWallpaperId(android.app.WallpaperManager.FLAG_SYSTEM) } catch (t: Throwable) { android.util.Log.w("WathakkerHome", "wallpaperId failed", t) }
        if (wpLight != null && id == wpLightId) return wpLight
        var small: Bitmap? = null
        try { val d = wm.drawable; if (d != null) { var w = d.intrinsicWidth; var h = d.intrinsicHeight; if (w <= 0 || h <= 0) { w = 720; h = 1280 }; val sw = 96; val sh = Math.max(1, 96 * h / w); small = Bitmap.createBitmap(sw, sh, Bitmap.Config.ARGB_8888); val cv = Canvas(small); d.setBounds(0, 0, sw, sh); d.draw(cv) } } catch (t: Throwable) { android.util.Log.w("WathakkerHome", "wallpaper drawable failed", t) }
        if (small == null) { val m = wallBlur(c, false) ?: return null; small = Bitmap.createScaledBitmap(m, 96, 64, true) }
        boxBlur(small, 2)
        wpLight = small; wpLightId = id
        return small
    }
    @JvmStatic fun protoGlass(c: Context, cv: Canvas, blur: Boolean, w: Float, h: Float, rad: Float) {
        val night = nightMode(c)
        if (blur) { val wb = lightWallBlur(c); if (wb != null) { val scaled = centerCrop(wb, Math.max(1, w.toInt()), Math.max(1, h.toInt())); val bp = Paint(Paint.ANTI_ALIAS_FLAG); bp.shader = android.graphics.BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP); cv.drawRoundRect(RectF(0f, 0f, w, h), rad, rad, bp) } }
        val p = Paint(Paint.ANTI_ALIAS_FLAG); p.color = if (night) 0x90141A22.toInt() else 0x99F2F2F7.toInt(); cv.drawRoundRect(RectF(0f, 0f, w, h), rad, rad, p)
    }
    @JvmStatic fun boxBlur(b: Bitmap, r: Int) {
        val w = b.width; val h = b.height; val px = IntArray(w * h); b.getPixels(px, 0, w, 0, 0, w, h); val out = IntArray(w * h)
        for (y in 0 until h) for (x in 0 until w) {
            var a = 0L; var rr = 0L; var g = 0L; var bb = 0L; var n = 0L
            for (dx in -r..r) { val xx = x + dx; if (xx < 0 || xx >= w) continue; val p = px[y * w + xx]; a += (p ushr 24).toLong(); rr += ((p shr 16) and 255).toLong(); g += ((p shr 8) and 255).toLong(); bb += (p and 255).toLong(); n++ }
            out[y * w + x] = ((a / n shl 24) or (rr / n shl 16) or (g / n shl 8) or (bb / n)).toInt()
        }
        for (y in 0 until h) for (x in 0 until w) {
            var a = 0L; var rr = 0L; var g = 0L; var bb = 0L; var n = 0L
            for (dy in -r..r) { val yy = y + dy; if (yy < 0 || yy >= h) continue; val p = out[yy * w + x]; a += (p ushr 24).toLong(); rr += ((p shr 16) and 255).toLong(); g += ((p shr 8) and 255).toLong(); bb += (p and 255).toLong(); n++ }
            px[y * w + x] = ((a / n shl 24) or (rr / n shl 16) or (g / n shl 8) or (bb / n)).toInt()
        }
        b.setPixels(px, 0, w, 0, 0, w, h)
    }
    @JvmStatic fun centerCrop(src: Bitmap, w: Int, h: Int): Bitmap {
        val out = Bitmap.createBitmap(w, h, Bitmap.Config.ARGB_8888)
        val cv = Canvas(out)
        val sc = Math.max(w.toFloat() / src.width, h.toFloat() / src.height)
        val dw = src.width * sc; val dh = src.height * sc
        val dst = android.graphics.RectF((w - dw) / 2f, (h - dh) / 2f, (w + dw) / 2f, (h + dh) / 2f)
        cv.drawBitmap(src, null, dst, Paint(Paint.FILTER_BITMAP_FLAG))
        return out
    }
    @JvmStatic fun avgColor(b: Bitmap): Int {
        val w = b.width; val h = b.height; var r = 0L; var g = 0L; var bb = 0L; var n = 0
        var y = 0; while (y < h) { var x = 0; while (x < w) { val p = b.getPixel(x, y); r += ((p shr 16) and 255).toLong(); g += ((p shr 8) and 255).toLong(); bb += (p and 255).toLong(); n++; x += 2 }; y += 2 }
        return ((r / n).toInt() shl 16) or ((g / n).toInt() shl 8) or (bb / n).toInt()
    }
    @JvmStatic fun bgColor(c: Context, id: Int): Int {
        var k = if (id > 0) c.getSharedPreferences("fajr_prefs", 0).getString("widget_bg_color_$id", null) else null
        if (k == null) k = c.getSharedPreferences("fajr_prefs", 0).getString("widget_bg_color", "auto")
        val rgb: Int
        if ("black" == k) rgb = 0x000000 else if ("gray" == k) rgb = 0x2b2b2b else if ("white" == k) rgb = 0xf6f6f6 else { val night = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES; rgb = if (night) 0x171719 else 0xf6f6f6 }
        if ("glass" == k) { val ng = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES; return if (ng) 0x90141A22.toInt() else 0x99F2F2F7.toInt() }
        /*3.3.3: adopted glass 1 (user pick 2026-10-23) - Morphe-like flat tint, night darkened to 0x90 alpha per his review*/
        if ("wallblur" == k || "frosted" == k) {
            val wb = wallBlur(c, "wallblur" == k)
            if (wb != null) return 0xFF000000.toInt() or blendOver(avgColor(wb), if (nightMode(c)) 0xB8141A22.toInt() else 0xB8F6F6F6.toInt())
            val ng = (c.resources.configuration.uiMode and Configuration.UI_MODE_NIGHT_MASK) == Configuration.UI_MODE_NIGHT_YES
            return 0xD9000000.toInt() or (if (ng) 0x171719 else 0xf6f6f6)
        }
        return 0xD9000000.toInt() or rgb
    }
    @JvmStatic fun isLight(bg: Int): Boolean { val r = (bg shr 16) and 255; val g = (bg shr 8) and 255; val b = bg and 255; return 0.299 * r + 0.587 * g + 0.114 * b > 170 }
    @JvmStatic fun bar(cv: Canvas, p: Paint, pad: Float, y: Float, wd: Float, bh: Float, fraction: Float, d: Float, fill: IntArray, track: Int) {
        p.shader = null; p.color = track; cv.drawRoundRect(RectF(pad * d, y * d, (wd - pad) * d, (y + bh) * d), bh * d / 2, bh * d / 2, p)
        val fw = Math.max(bh, (wd - 2 * pad) * Math.max(0f, Math.min(1f, fraction)))
        val x0 = pad * d; val x1 = (pad + fw) * d
        if (fill[0] != fill[1]) p.shader = LinearGradient(x0, 0f, x1, 0f, fill[0], fill[1], Shader.TileMode.CLAMP) else p.color = fill[0]
        cv.drawRoundRect(RectF(x0, y * d, x1, (y + bh) * d), bh * d / 2, bh * d / 2, p); p.shader = null
    }
    @JvmStatic fun render(c: Context, wd: Int, ht: Int): Bitmap = renderKind(c, "Hero", wd, ht, 0)
    @JvmStatic fun renderKind(c: Context, kind: String, wd: Int, ht: Int, id: Int): Bitmap {
        if (kind == "OneUI") {
            try { var os = batchState.get(); if (os == null) os = state(c); ovArm(os.iqama, os.at, os.now); return OneUIWidget.render(c, os, wd, ht, id) }
            catch (e: Exception) { android.util.Log.e("WathakkerHome", "OneUI render failed", e) }
        }
        val d = c.resources.displayMetrics.density
        val os = oversample(c, wd, ht, d)
        val b = Bitmap.createBitmap(Math.max(1, (wd * d * os).toInt()), Math.max(1, (ht * d * os).toInt()), Bitmap.Config.ARGB_8888)
        val cv = Canvas(b); cv.scale(os, os)
        val p = Paint(Paint.ANTI_ALIAS_FLAG)
        val bg = bgColor(c, id)
        var bgKey = if (id > 0) c.getSharedPreferences("fajr_prefs", 0).getString("widget_bg_color_$id", null) else null
        if (bgKey == null) bgKey = c.getSharedPreferences("fajr_prefs", 0).getString("widget_bg_color", "auto")
        val rad = Math.min(24f, ht / 4f) * d
        val wbm = if ("wallblur" == bgKey || "frosted" == bgKey) wallBlur(c, "wallblur" == bgKey) else null
        // 3.3.3 REVIEW PROTOTYPES (rendered only by the debug GlassProtoCheck harness; no settings UI can
        // set these keys, dormant in release): protoGlass1 = Morphe-like sharper glass (tint only, no
        // blur); protoGlass2 = same tint over a lightly blurred colour mesh (no real wallpaper read on
        // Android 13+); protoFrost1 = frosted colour mesh under a uniform milk scrim, no sheen/grain -
        // the light-mode legibility candidate. The existing "wallblur" path attempts the real-wallpaper
        // heavy-blur treatment and falls back to the mesh when the system blocks the read.
        if ("protoGlass1" == bgKey || "protoGlass2" == bgKey) { protoGlass(c, cv, "protoGlass2" == bgKey, wd * d, ht * d, rad) }
        else if ("protoFrost1" == bgKey) {
            val m = wallBlur(c, false)
            if (m != null) { val scaled = centerCrop(m, Math.max(1, (wd * d).toInt()), Math.max(1, (ht * d).toInt())); val bp = Paint(Paint.ANTI_ALIAS_FLAG); bp.shader = android.graphics.BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP); cv.drawRoundRect(RectF(0f, 0f, wd * d, ht * d), rad, rad, bp) }
            val sp = Paint(Paint.ANTI_ALIAS_FLAG); sp.color = if (nightMode(c)) 0xB8141A22.toInt() else 0xB8F6F6F6.toInt(); cv.drawRoundRect(RectF(0f, 0f, wd * d, ht * d), rad, rad, sp)
        }
        else if (wbm != null) {
            val scaled = centerCrop(wbm, Math.max(1, (wd * d).toInt()), Math.max(1, (ht * d).toInt()))
            val bp = Paint(Paint.ANTI_ALIAS_FLAG)
            bp.shader = android.graphics.BitmapShader(scaled, Shader.TileMode.CLAMP, Shader.TileMode.CLAMP)
            cv.drawRoundRect(RectF(0f, 0f, wd * d, ht * d), rad, rad, bp)
            val sp = Paint(Paint.ANTI_ALIAS_FLAG); sp.color = if (nightMode(c)) 0xB8141A22.toInt() else 0xB8F6F6F6.toInt(); cv.drawRoundRect(RectF(0f, 0f, wd * d, ht * d), rad, rad, sp)/*3.3.3: adopted frosted 2 (user pick 2026-10-23) - uniform milk scrim, no sheen/grain*/
        } else { p.color = bg; cv.drawRoundRect(RectF(0f, 0f, wd * d, ht * d), rad, rad, p) }
        // His call (build 53): no border on the glass widget background either - frosted fill only.
        var med = Typeface.DEFAULT; var semi = Typeface.DEFAULT_BOLD
        val light = isLight(bg); val white = if (light) 0xff1c1c1e.toInt() else Color.WHITE; val gray = if (light) 0xff63636b.toInt() else 0xffc9c9ce.toInt()
        var dateC = if (light) 0xff6e6e76.toInt() else 0xff96969e.toInt(); if ("glass" == bgKey && !light) dateC = 0xffbabac2.toInt()/*3.3.3 user review: dates need a lighter gray (not white) to read on dark glass*/
        val remainC = if (light) 0xffb47a1e.toInt() else 0xffe5b455.toInt(); val trackC = if (light) 0xffc4c4cd.toInt() else 0xff38383e.toInt()
        try {
            var s = batchState.get(); if (s == null) s = state(c)
            ovArm(s.iqama, s.at, s.now)
            med = face(c, false, s.rtl); semi = face(c, true, s.rtl)
            val mins = Math.max(0, (s.at - s.now) / 60000)
            val pad = Math.min(18f, wd * .06f); val ww = wd - 2 * pad
            val progress = (s.now - s.prev) / Math.max(1, s.at - s.prev).toFloat()
            val remain = if (s.iqama) remPrefix(s.code, ": ") + "⁦" + mmss(s.at - s.now) + "⁩" else remaining(mins, s.code)
            if (kind == "Hero") {
                val compact = true
                val nameY = if (compact) ht * .389f else ht * .283f; val barY = if (compact) ht * .57f else ht * .444f; val timeY = if (compact) ht * (if (ht < 70 && !large(c)) .78f else .844f) else ht * .602f; val dateY = if (compact) ht * .133f else ht * .75f; val countY = if (compact) ht * .389f else ht - 15f
                if (!compact) { heroText(cv, s.city, pad, 22f, ww * .70f, 14f, med, gray, Layout.Alignment.ALIGN_NORMAL, d); heroText(cv, s.day, wd - pad - ww * .28f, 22f, ww * .28f, 14f, med, gray, Layout.Alignment.ALIGN_OPPOSITE, d) }
                val side = if (compact) ww * .22f else ww * .4f
                val la = if (compact) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_NORMAL; val ra = if (compact) Layout.Alignment.ALIGN_CENTER else Layout.Alignment.ALIGN_OPPOSITE
                heroText(cv, s.targetName, pad, nameY, side, 18 * Math.min(1f, ht / 70f), med, white, la, d)
                heroText(cv, s.names[s.previous], wd - pad - side, nameY, side, 18 * Math.min(1f, ht / 70f), med, white, ra, d)
                bar(cv, p, pad, barY, wd.toFloat(), if (compact) Math.min(8f, ht * .09f) else 10f, progress, d, if (s.iqama) intArrayOf(0xFFE5B455.toInt(), 0xFFE5B455.toInt()) else barFillColors(c, id), trackC)
                heroText(cv, tm(s.at, s.code), pad, timeY, side, 18 * Math.min(1f, ht / 70f), semi, white, la, d)
                heroText(cv, tm(s.prev, s.code), wd - pad - side, timeY, side, 18 * Math.min(1f, ht / 70f), semi, white, ra, d)
                val dateWidth = ww * (if (compact) .48f else .48f)
                heroText(cv, s.hijri, pad, dateY, dateWidth, 12 * Math.min(1f, ht / 70f), med, dateC, Layout.Alignment.ALIGN_NORMAL, d)
                heroText(cv, s.greg, wd - pad - dateWidth, dateY, dateWidth, 12 * Math.min(1f, ht / 70f), med, dateC, Layout.Alignment.ALIGN_OPPOSITE, d)
                heroText(cv, remain, if (compact) pad + ww * .24f else pad, countY, if (compact) ww * .52f else ww, 15f * Math.min(1f, ht / 70f).toFloat(), semi, remainC, Layout.Alignment.ALIGN_CENTER, d)
            }
            else if (kind == "NextPrayer" || kind == "PrayerBar") {
                val square = wd < ht * 1.45f
                val sc = if (square) Math.min(1f, ht / 84f) else Math.min(1f, ht / 70f)
                val bh = Math.min(10f, ht * .11f)
                val nameY = if (kind == "PrayerBar") ht * .21f else if (square) ht * .12f else ht * .26f; val timeY = if (kind == "PrayerBar") ht * .79f else if (square) ht * .31f else ht * .493f
                text(cv, s.targetName, pad, nameY, ww, (if (square) 13 else 14) * sc, med, white, Layout.Alignment.ALIGN_CENTER, d)
                text(cv, tm(s.at, s.code), pad, timeY, ww, (if (kind == "PrayerBar") 16 else if (square) 17 else 18) * sc, semi, white, Layout.Alignment.ALIGN_CENTER, d)
                if (kind == "PrayerBar") bar(cv, p, pad, ht * .422f, wd.toFloat(), bh, progress, d, if (s.iqama) intArrayOf(0xFFE5B455.toInt(), 0xFFE5B455.toInt()) else barFillColors(c, id), trackC)
                else if (square) { text(cv, remPrefix(s.code, "").trim(), pad, ht * .56f, ww, 10 * sc, med, gray, Layout.Alignment.ALIGN_CENTER, d); text(cv, if (s.iqama) mmss(s.at - s.now) else durText(mins, s.code), pad, ht * .76f, ww, 10 * sc, med, gray, Layout.Alignment.ALIGN_CENTER, d) }
                else text(cv, remain, pad, ht * .73f, ww, 12 * sc, med, gray, Layout.Alignment.ALIGN_CENTER, d)
            }
            else if (kind == "Schedule") {
                val cell = ww / 6
                for (i in 0 until 6) { val x = if (s.rtl) wd - pad - cell * (i + 1) else pad + cell * i; text(cv, s.names[i], x + 2, ht * .30f, cell - 4, 13f, med, gray, Layout.Alignment.ALIGN_CENTER, d); text(cv, tm(s.ts[i], s.code), x + 2, ht * .69f, cell - 4, 13f, semi, white, Layout.Alignment.ALIGN_CENTER, d) }
            }
            else if (kind == "Clock") {
                val side = ww * .23f
                for (i in 0 until 2) { val x = if ((i == 0) == s.rtl) wd - pad - side else pad; text(cv, if (i == 0) s.targetName else s.names[s.previous], x, ht * .28f, side, 13f, med, gray, Layout.Alignment.ALIGN_CENTER, d); text(cv, tm(if (i == 0) s.at else s.prev, s.code), x, ht * .70f, side, 15f, semi, white, Layout.Alignment.ALIGN_CENTER, d) }
                val sec = Math.max(0, (s.at - s.now) / 1000)
                val clock = if (s.iqama) String.format(Locale.ENGLISH, "%02d:%02d", sec / 60, sec % 60) else String.format(Locale.ENGLISH, "%02d:%02d", sec / 3600, sec / 60 % 60)
                /*3.3.3 his spec: no seconds on the عداد الصلاة countdown - hours/minutes only; the iqama countdown keeps MM:SS (live Chronometer overlay).*/
                text(cv, clock, pad + side, ht * .5f, ww - 2 * side, 26f, semi, white, Layout.Alignment.ALIGN_CENTER, d)
            }
            else if (kind == "DayPrayer") { text(cv, s.day, pad, ht * .33f, ww, 20 * Math.min(1f, ht / 60f), med, white, Layout.Alignment.ALIGN_CENTER, d); text(cv, s.targetName + " " + tm(s.at, s.code), pad, ht * .70f, ww, 14 * Math.min(1f, ht / 70f), med, gray, Layout.Alignment.ALIGN_CENTER, d) }
            else if (kind == "Pill") { text(cv, if (s.iqama) s.targetName else (if (s.rtl) adhanWord(s.code) + " " + s.targetName else s.targetName + " " + adhanWord(s.code)), pad, ht * .32f, ww, 15 * Math.min(1f, ht / 60f), med, white, Layout.Alignment.ALIGN_CENTER, d); text(cv, remain, pad, ht * .68f, ww, 12 * Math.min(1f, ht / 70f), med, gray, Layout.Alignment.ALIGN_CENTER, d) }
            else {
                val hijri = kind == "Hijri"
                text(cv, if (hijri) s.hday else s.gday, pad, ht * .25f, ww, 26 * Math.min(1f, ht / 70f), semi, white, Layout.Alignment.ALIGN_CENTER, d)
                text(cv, if (hijri) s.hmonth else s.gmonth, pad, ht * .55f, ww, 14 * Math.min(1f, ht / 70f), med, white, Layout.Alignment.ALIGN_CENTER, d)
                text(cv, if (hijri) s.hyear + (if (s.code.startsWith("ar")) " هـ" else " AH") else s.gyear, pad, ht * .80f, ww, 12 * Math.min(1f, ht / 70f), med, gray, Layout.Alignment.ALIGN_CENTER, d)
            }
        } catch (e: Exception) { android.util.Log.e("WathakkerHome", "Render failed", e); text(cv, LockWidgets.errText(lang(c)), 5f, ht / 2f, wd - 10f, 12f, med, white, Layout.Alignment.ALIGN_CENTER, d) }
        return b
    }
}
