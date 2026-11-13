package com.example.lockwidgets

import android.app.AlarmManager
import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.appwidget.AppWidgetProviderInfo
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.PorterDuff
import android.graphics.PorterDuffColorFilter
import android.graphics.RectF
import android.graphics.Typeface
import android.icu.util.IslamicCalendar
import android.os.Bundle
import android.text.Layout
import android.text.StaticLayout
import android.text.TextDirectionHeuristics
import android.text.TextPaint
import android.text.TextUtils
import android.widget.RemoteViews
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale

/** Native port of Prayer-Sam-Widgets artwork and interactions into the recovered Wathakker app. */
open class LockWidgets : AppWidgetProvider() {
    class Day : LockWidgets()
    class Prayers : LockWidgets()
    class Tasbih : LockWidgets()
    class Hijri : LockWidgets()
    class Gregorian : LockWidgets()
    class Next : LockWidgets()
    class Bar : LockWidgets()
    class Countdown : LockWidgets()
    class CountdownClean : LockWidgets()

    override fun onUpdate(c: Context, m: AppWidgetManager, ids: IntArray) { for (id in ids) update(c, m, id, javaClass); schedule(c) }
    override fun onEnabled(c: Context) { schedule(c) }
    override fun onDisabled(c: Context) { schedule(c) }
    override fun onDeleted(c: Context, ids: IntArray) { for (id in ids) c.getSharedPreferences("lock_tasbih", 0).edit().remove("count_$id").apply(); schedule(c) }
    override fun onAppWidgetOptionsChanged(c: Context, m: AppWidgetManager, id: Int, options: Bundle) { update(c, m, id, javaClass) }
    override fun onReceive(c: Context, `in`: Intent) {
        val a = `in`.action
        if (action(c, "TAP") == a) {
            val id = `in`.getIntExtra(AppWidgetManager.EXTRA_APPWIDGET_ID, -1)
            val info = AppWidgetManager.getInstance(c).getAppWidgetInfo(id)
            if (info != null && info.provider == ComponentName(c, Tasbih::class.java)) { val p = c.getSharedPreferences("lock_tasbih", 0); val key = "count_$id"; p.edit().putInt(key, p.getInt(key, 0) + 1).apply(); update(c, AppWidgetManager.getInstance(c), id, Tasbih::class.java) }
            return
        }
        if (action(c, "TICK") == a || action(c, "BOUNDARY") == a) {
            android.util.Log.i("WathakkerLock", (if (action(c, "BOUNDARY") == a) "boundary fire" else "tick fire") + " t=" + System.currentTimeMillis())
            val full = action(c, "BOUNDARY") == a || phaseChanged(c); updateAllTick(c, full); HomeWidgets.updateAll(c, !full); try { com.example.widget.WidgetUpdater.updateFace(c) } catch (t: Throwable) { android.util.Log.w("WathakkerLock", "face widget update failed", t) }; schedule(c); return
        }
        if (Intent.ACTION_BOOT_COMPLETED == a || Intent.ACTION_TIME_CHANGED == a || Intent.ACTION_TIMEZONE_CHANGED == a || Intent.ACTION_DATE_CHANGED == a || Intent.ACTION_MY_PACKAGE_REPLACED == a) {
            updateAll(c); HomeWidgets.updateAll(c); try { com.example.widget.WidgetUpdater.updateAll(c) } catch (t: Throwable) { android.util.Log.w("WathakkerLock", "home widget updater failed", t) }; schedule(c); return
        }
        super.onReceive(c, `in`)
    }

    companion object {
        private val TYPES = arrayOf<Class<*>>(Day::class.java, Tasbih::class.java, Hijri::class.java, Gregorian::class.java, Next::class.java, Bar::class.java, Countdown::class.java, CountdownClean::class.java)
        private fun action(c: Context, suffix: String): String = c.packageName + ".lockwidgets." + suffix
        private fun res(c: Context, type: String, name: String): Int = c.resources.getIdentifier(name, type, c.packageName)
        private fun lang(c: Context): String = c.getSharedPreferences("app_prefs", 0).getString("language", "ar") ?: "ar"
        private fun rtl(c: Context): Boolean { val x = lang(c); return x.startsWith("ar") || x.startsWith("ur") }
        private fun arabic(c: Context): Boolean = lang(c).startsWith("ar")
        private var languageListener: SharedPreferences.OnSharedPreferenceChangeListener? = null
        private fun watchLanguage(c: Context) {
            if (languageListener == null) {
                val app = c.applicationContext
                languageListener = SharedPreferences.OnSharedPreferenceChangeListener { _, key -> if ("language" == key) updateAll(app) else if ("digits_style" == key) { updateAll(app); HomeWidgets.updateAll(app) } }
                app.getSharedPreferences("app_prefs", 0).registerOnSharedPreferenceChangeListener(languageListener)
            }
        }
        private fun locale(c: Context): Locale = Locale(lang(c))
        private fun pulse(c: Context): PendingIntent = PendingIntent.getBroadcast(c, 700001, Intent(c, LockWidgets::class.java).setAction(action(c, "TICK")), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private fun boundaryPi(c: Context): PendingIntent = PendingIntent.getBroadcast(c, 700002, Intent(c, LockWidgets::class.java).setAction(action(c, "BOUNDARY")), PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        private fun active(c: Context): Boolean { val m = AppWidgetManager.getInstance(c); for (t in TYPES) if (m.getAppWidgetIds(ComponentName(c, t)).isNotEmpty()) return true; return false }
        /** True while any lock/home widget instance exists; the per-minute pulse is armed only then. */
        @JvmStatic fun anyActive(c: Context): Boolean {
            var any = active(c); val m = AppWidgetManager.getInstance(c)
            for (k in arrayOf("Hero", "NextPrayer", "PrayerBar", "Clock", "Pill", "DayPrayer", "OneUI", "Gregorian", "Hijri", "Iqama", "IqamaSmall", "Schedule", "WideBar")) any = any or m.getAppWidgetIds(ComponentName(c, "com.example.widget.${k}WidgetProvider")).isNotEmpty()
            return any
        }
        @JvmStatic fun schedule(c: Context) {
            watchLanguage(c); val a = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
            if (!anyActive(c)) { a.cancel(pulse(c)); a.cancel(boundaryPi(c)); return }
            val now = System.currentTimeMillis(); val minute = (now / 60000 + 1) * 60000; var target = -1L
            try {
                val day = Calendar.getInstance()
                for (d in 0 until 2) {
                    val ts = timesCached(c, day); val friday = day.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY
                    for (i in 0 until 6) {
                        if (i == 1) continue
                        if (ts[i] > now && (target < 0 || ts[i] < target)) target = ts[i]
                        if (!(friday && i == 2)) { val iq = ts[i] + PrayerPhase.iqamaMinutes(i) * 60000L; if (iq > now && (target < 0 || iq < target)) target = iq }
                    }
                    day.add(Calendar.DATE, 1)
                }
            } catch (e: Exception) { android.util.Log.w("WathakkerLock", "Boundary scheduling fallback", e) }
            // Two independent alarms, re-evaluated at every schedule() call:
            // - pulse: non-wakeup minute alarm. Minute digits are only visible while a host is showing
            //   them, so while the device sleeps this defers and fires at the next wake.
            // - boundary: wakeup alarm at the next prayer/iqama instant, armed HERE rather than promoted
            //   by a later pulse delivery - a deferred non-wakeup pulse can never be the mechanism that
            //   was supposed to keep the rollover on time. Same PI identity each call, so no accumulation.
            // If target computation failed the old boundary PI stays pending; it fires one harmless
            // extra update and the next successful schedule() replaces it.
            // Regression diagnostics (within the staleness-fix scope): record which alarm branch actually
            // armed - exact vs the silent inexact SecurityException fallback - plus the exact-alarm access
            // state, so a stale device can be told apart from logcat: canExact=false or *Exact=false means
            // the OS denied exact alarms; otherwise deferral happened at delivery time (sleep/batching).
            val canExact = android.os.Build.VERSION.SDK_INT < 31 || a.canScheduleExactAlarms()
            var pulseExact = true; var boundExact = true
            try { a.setExactAndAllowWhileIdle(AlarmManager.RTC, minute, pulse(c)) } catch (e: SecurityException) { pulseExact = false; a.setAndAllowWhileIdle(AlarmManager.RTC, minute, pulse(c)) }
            if (target > 0) { try { a.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, boundaryPi(c)) } catch (e: SecurityException) { boundExact = false; a.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, target, boundaryPi(c)) } }
            android.util.Log.i("WathakkerLock", "arm next=$minute target=$target canExact=$canExact pulseExact=$pulseExact boundExact=$boundExact")
        }
        @JvmStatic fun updateAll(c: Context) { com.example.utils.Digits.refresh(c); watchLanguage(c); val m = AppWidgetManager.getInstance(c); for (t in TYPES) for (id in m.getAppWidgetIds(ComponentName(c, t))) update(c, m, id, t) }
        /** Minute-pulse update: countdown types every tick, static types only when the phase tracker flags a boundary. */
        @JvmStatic fun updateAllTick(c: Context, full: Boolean) { com.example.utils.Digits.refresh(c); watchLanguage(c); val m = AppWidgetManager.getInstance(c); for (t in TYPES) { val pulseOnly = (t == Day::class.java || t == Tasbih::class.java || t == Hijri::class.java || t == Gregorian::class.java || t == Prayers::class.java); if (pulseOnly && !full) continue; for (id in m.getAppWidgetIds(ComponentName(c, t))) update(c, m, id, t) } }
        private var lastDayKey = -1L; private var lastPhase = Int.MIN_VALUE; private var lastIqama = false
        /** True when the day, next-prayer index or iqama state changed since the previous tick (or on first tick after process start). */
        @Synchronized private fun phaseChanged(c: Context): Boolean {
            try {
                val today = Calendar.getInstance(); val ts = timesCached(c, today); val now = System.currentTimeMillis()
                val yesterday = today.clone() as Calendar; yesterday.add(Calendar.DAY_OF_MONTH, -1)
                val iqama = iqamaWindow(ts, timesCached(c, yesterday), now) != null
                var next = -1; for (i in 0 until 6) if (i != 1 && ts[i] > now) { next = i; break }; if (next < 0) next = 6
                val dayKey = today.get(Calendar.YEAR) * 1000L + today.get(Calendar.DAY_OF_YEAR)
                val changed = (lastPhase == Int.MIN_VALUE) || (dayKey != lastDayKey) || (next != lastPhase) || (iqama != lastIqama)
                lastDayKey = dayKey; lastPhase = next; lastIqama = iqama; return changed
            } catch (e: Exception) { android.util.Log.w("WathakkerLock", "phase tracking failed - full update", e); return true }
        }
        private var tcDay = -1L; private var tcLat = Double.NaN; private var tcLng = Double.NaN; private var tcMethod = -1; private var tcVal: LongArray? = null
        /** Per-day cache of the reflection-based prayer-time calculation, keyed on date + location + method. */
        @Synchronized @JvmStatic @Throws(Exception::class) fun timesCached(c: Context, day: Calendar): LongArray {
            val p = Class.forName("com.example.fajr.data.FajrPreferences"); val prefs = p.getConstructor(Context::class.java).newInstance(c)
            val lat = p.getMethod("getLatitude").invoke(prefs) as Double; val lng = p.getMethod("getLongitude").invoke(prefs) as Double
            val method = p.getMethod("getCalculationMethodId").invoke(prefs) as Int
            val key = day.get(Calendar.YEAR) * 1000L + day.get(Calendar.DAY_OF_YEAR)
            val cached = tcVal
            if (cached != null && key == tcDay && lat == tcLat && lng == tcLng && method == tcMethod) return cached.clone()
            val r = times(c, day); tcDay = key; tcLat = lat; tcLng = lng; tcMethod = method; tcVal = r; return r.clone()
        }
        private fun update(c: Context, m: AppWidgetManager, id: Int, type: Class<*>) {
            com.example.utils.Digits.refresh(c)
            val v = RemoteViews(c.packageName, res(c, "layout", "lock_widget_image"))
            val opts = m.getAppWidgetOptions(id); val min = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MIN_WIDTH, 123); val max = opts.getInt(AppWidgetManager.OPTION_APPWIDGET_MAX_WIDTH, min); val tiny = min > 0 && max <= 80 && (type == Hijri::class.java || type == Gregorian::class.java || type == Next::class.java)
            HomeWidgets.ovBegin(); val bmp = renderSize(c, type, id, tiny)
            v.setImageViewBitmap(res(c, "id", "lock_widget_art"), bmp); HomeWidgets.ovApply(c, v, bmp.width, bmp.height)
            v.setInt(res(c, "id", "lock_widget_art"), "setLayoutDirection", if (rtl(c)) android.view.View.LAYOUT_DIRECTION_RTL else android.view.View.LAYOUT_DIRECTION_LTR)
            v.setOnClickPendingIntent(res(c, "id", "lock_widget_art"), null)
            m.updateAppWidget(id, v)
        }
        /** Call r7's own calculator/preferences so all widgets agree with its prayer screen. */
        @Throws(Exception::class) private fun times(c: Context, day: Calendar): LongArray {
            val p = Class.forName("com.example.fajr.data.FajrPreferences"); val prefs = p.getConstructor(Context::class.java).newInstance(c)
            val lat = p.getMethod("getLatitude").invoke(prefs) as Double; val lng = p.getMethod("getLongitude").invoke(prefs) as Double
            val method = p.getMethod("getCalculationMethodId").invoke(prefs) as Int
            val meth = Class.forName("com.example.fajr.calculation.CalculationMethod")
            val companion: Any = try { meth.getField("Companion").get(null) } catch (nf: NoSuchFieldException) { meth.getField("INSTANCE").get(null) }
            val choice = companion.javaClass.getMethod("fromId", Int::class.javaPrimitiveType).invoke(companion, method)
            val calc = Class.forName("com.example.fajr.calculation.PrayerAdjustments")
            val result = calc.getMethod("calculateAll", android.content.Context::class.java, Calendar::class.java, Double::class.javaPrimitiveType, Double::class.javaPrimitiveType, meth).invoke(calc.getField("INSTANCE").get(null), c, day, lat, lng, choice)
            val getters = arrayOf("getFajr", "getSunrise", "getDhuhr", "getAsr", "getMaghrib", "getIsha"); val out = LongArray(6); for (i in 0 until 6) out[i] = result.javaClass.getMethod(getters[i]).invoke(result) as Long; return out
        }
        private fun time(value: Long, l: Locale): String = HomeWidgets.tm(value, l.language)
        private fun number(n: Long, l: Locale): String { val f = NumberFormat.getIntegerInstance(l); f.isGroupingUsed = false; return f.format(n) }
        private fun font(c: Context, file: String): Typeface = Typeface.createFromAsset(c.assets, "lockfonts/$file.ttf")
        private fun night(c: Context): Boolean = (c.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK) == android.content.res.Configuration.UI_MODE_NIGHT_YES
        private fun ink(c: Context): Int = if (night(c)) Color.WHITE else 0xFF1C1C1E.toInt()
        private fun inkSoft(c: Context): Int = if (night(c)) 0xE6FFFFFF.toInt() else 0xE61C1C1E.toInt()
        private fun track(c: Context): Int = if (night(c)) 0x40FFFFFF.toInt() else 0x40000000.toInt()
        private fun paint(font: Typeface, size: Float, d: Float, color: Int): TextPaint { val p = TextPaint(Paint.ANTI_ALIAS_FLAG); p.typeface = font; p.textSize = size * d; p.color = color; val shadow = if (Color.red(color) + Color.green(color) + Color.blue(color) > 384) 0x66000000 else 0x66FFFFFF; p.setShadowLayer(3 * d, 0f, d, shadow); return p }
        private fun lines(canvas: Canvas, w: Int, h: Int, d: Float, text: Array<String>, sizes: FloatArray, fonts: Array<Typeface>, INK: Int, SOFT: Int) {
            val ls = arrayOfNulls<StaticLayout>(text.size); var total = 0f
            val shown = arrayOfNulls<String>(text.size); for (i in text.indices) shown[i] = com.example.utils.Digits.forWidget(text[i] as String?) ?: text[i]
            for (i in text.indices) { val lp = paint(fonts[i], sizes[i], d, if (i == 0) INK else SOFT); val measured = lp.measureText(shown[i]); if (measured > w - 4 * d) lp.textSize = lp.textSize * (w - 4 * d) / measured; ls[i] = StaticLayout.Builder.obtain(shown[i]!!, 0, shown[i]!!.length, lp, w).setAlignment(Layout.Alignment.ALIGN_CENTER).setIncludePad(false).build(); total += ls[i]!!.height }
            total += (text.size - 1) * 2 * d; var y = Math.max(0f, (h - total) / 2)
            for (li in ls.indices) { val l = ls[li]!!; if (!HomeWidgets.capture(text[li], w / 2f, y + l.height / 2f, w - 4 * d, sizes[li] * d, if (li == 0) INK else SOFT)) { canvas.save(); canvas.translate(0f, y); l.draw(canvas); canvas.restore() }; y += l.height + 2 * d }
        }
        @JvmStatic fun render(c: Context, type: Class<*>, id: Int): Bitmap = renderSize(c, type, id, false)
        private fun centered(cv: Canvas, textIn: String, cx: Float, cy: Float, p: TextPaint, max: Float) {
            if (HomeWidgets.capture(textIn, cx, cy, max, p.textSize, p.color)) return
            var text = com.example.utils.Digits.forWidget(textIn as String?) ?: textIn
            val width = p.measureText(text); if (width > max) p.textSize = p.textSize * max / width
            val line = StaticLayout.Builder.obtain(text, 0, text.length, p, max.toInt()).setAlignment(Layout.Alignment.ALIGN_CENTER).setTextDirection(TextDirectionHeuristics.FIRSTSTRONG_LTR).setIncludePad(false).setMaxLines(1).setEllipsize(TextUtils.TruncateAt.END).build()
            cv.save(); cv.translate(cx - max / 2, cy - line.height / 2f); line.draw(cv); cv.restore()
        }
        @JvmStatic fun errText(code: String): String {
            if (code.startsWith("ar")) return "تعذر حساب الصلاة"; if (code.startsWith("fr")) return "Heure de prière indisponible"; if (code.startsWith("de")) return "Gebetszeit nicht verfügbar"; if (code.startsWith("es")) return "Hora de oración no disponible"; if (code.startsWith("it")) return "Ora della preghiera non disponibile"; if (code.startsWith("tr")) return "Namaz vakti hesaplanamadı"; if (code.startsWith("ur")) return "نماز کا وقت دستیاب نہیں"; if (code.startsWith("id")) return "Waktu salat tidak tersedia"; if (code.startsWith("ru")) return "Время молитвы недоступно"; if (code.startsWith("pt")) return "Hora de oração indisponível"; return "Prayer time unavailable"
        }
        @JvmStatic fun iqamaWindow(today: LongArray, yesterday: LongArray?, now: Long): LongArray? = PrayerPhase.window(today, yesterday, now, Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY)
        @JvmStatic fun renderSize(c: Context, type: Class<*>, id: Int, tiny: Boolean): Bitmap {
            val d = c.resources.displayMetrics.density; val wd = if (tiny) 54 else 123; val hd = 54
            val bmp = Bitmap.createBitmap((wd * d).toInt(), (hd * d).toInt(), Bitmap.Config.ARGB_8888); val cv = Canvas(bmp); val w = bmp.width; val h = bmp.height; val l = locale(c); val INK = ink(c); val SOFT = inkSoft(c); val TRACK = track(c); val ar = arabic(c); val rtl = rtl(c); val code = lang(c); val sysM = Typeface.create("sans-serif-medium", Typeface.NORMAL); val sysB = Typeface.create("sans-serif", Typeface.BOLD); com.example.ui.i18n.L10n.init(c)
            val amiri = font(c, "amiri_bold"); val medium = Typeface.createFromAsset(c.assets, "lockpersonal/expoarabic_medium.ttf"); val bold = Typeface.createFromAsset(c.assets, "lockpersonal/expoarabic_semibold.ttf"); val pad = (if (tiny) 6 else 12) * d
            if (type == Day::class.java) {
                if (ar) {
                    try { val art = BitmapFactory.decodeStream(c.assets.open("lockpersonal/day_" + Calendar.getInstance().get(Calendar.DAY_OF_WEEK) + ".png")); val ratio = Math.min((w - 2 * pad) / art.width, 34 * d / art.height); val aw = art.width * ratio; val ah = art.height * ratio; val ink = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG); ink.colorFilter = PorterDuffColorFilter(INK, PorterDuff.Mode.SRC_IN); cv.drawBitmap(art, null, RectF((w - aw) / 2, (h - ah) / 2, (w + aw) / 2, (h + ah) / 2), ink) } catch (ex: Exception) { throw RuntimeException(ex) }
                } else { val name = SimpleDateFormat("EEEE", l).format(Date()); centered(cv, name, w / 2f, h / 2f, paint(if (rtl) amiri else sysB, 29f, d, INK), w - 2 * pad) }
            }
            else if (type == Gregorian::class.java) { val now = Date(); val date = SimpleDateFormat(if (tiny) "M/yyyy" else "MMMM", l).format(now); val day = SimpleDateFormat("d", l).format(now); if (tiny) { lines(cv, w, h, d, arrayOf(day, date), floatArrayOf(18f, 9f), arrayOf(if (rtl) bold else sysB, if (rtl) medium else sysM), INK, SOFT) } else { lines(cv, w, h, d, arrayOf("$day $date", SimpleDateFormat("yyyy", l).format(now)), floatArrayOf(18f, 16f), arrayOf(if (rtl) bold else sysB, if (rtl) bold else sysB), INK, SOFT) } }
            else if (type == Hijri::class.java) {
                val cal = IslamicCalendar(); cal.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA); cal.timeInMillis = System.currentTimeMillis(); val hoff = c.getSharedPreferences("fajr_prefs", 0).getInt("hijri_offset", 0); if (hoff != 0) cal.add(Calendar.DATE, hoff)
                val month = com.example.prayer.HijriDate.monthName(cal.get(Calendar.MONTH) + 1, code); val day = number(cal.get(Calendar.DAY_OF_MONTH).toLong(), l); val year = number(cal.get(Calendar.YEAR).toLong(), l)
                if (tiny && ar) {
                    centered(cv, day, w / 2f, 16 * d, paint(bold, 18f, d, INK), w - 2 * pad)
                    try { val art = BitmapFactory.decodeStream(c.assets.open("lockpersonal/hijri_" + (cal.get(Calendar.MONTH) + 1) + ".png")); val ratio = Math.min((w - 2 * pad) / art.width, 18 * d / art.height); val aw = art.width * ratio; val ah = art.height * ratio; val ink = Paint(Paint.ANTI_ALIAS_FLAG or Paint.FILTER_BITMAP_FLAG); ink.colorFilter = PorterDuffColorFilter(INK, PorterDuff.Mode.SRC_IN); cv.drawBitmap(art, null, RectF((w - aw) / 2, 34 * d - ah / 2, (w + aw) / 2, 34 * d + ah / 2), ink) } catch (ex: Exception) { throw RuntimeException(ex) }
                } else { lines(cv, w, h, d, if (tiny) arrayOf(day, month) else arrayOf("$day $month", year), if (tiny) floatArrayOf(18f, 10f) else floatArrayOf(17f, 16f), if (tiny) arrayOf(if (rtl) bold else sysB, if (rtl) amiri else sysB) else arrayOf(if (rtl) bold else sysB, if (rtl) bold else sysB), INK, SOFT) }
            }
            else if (type == Tasbih::class.java) { val n = c.getSharedPreferences("lock_tasbih", 0).getInt("count_$id", 0); val dhikr = if (rtl) arrayOf("سُبْحَانَ اللَّه", "الْحَمْدُ لِلَّه", "اللَّهُ أَكْبَر") else arrayOf("SubhanAllah", "Alhamdulillah", "Allahu Akbar"); lines(cv, w, h, d, arrayOf(dhikr[(n / 33) % 3], number((n % 33).toLong(), l) + " / " + number(33, l)), floatArrayOf(17f, 13f), arrayOf(if (rtl) amiri else sysB, if (rtl) medium else sysM), INK, SOFT) }
            else {
                try {
                    val today = Calendar.getInstance(); val ts = timesCached(c, today); val now = System.currentTimeMillis(); var next = -1; for (i in 0 until 6) if (i != 1 && ts[i] > now) { next = i; break }
                    var at: Long; var prev: Long
                    if (next < 0) { val tomorrow = today.clone() as Calendar; tomorrow.add(Calendar.DAY_OF_MONTH, 1); at = timesCached(c, tomorrow)[0]; prev = ts[5]; next = 0 }
                    else { at = ts[next]; if (next > 0) prev = ts[if (next == 2) 0 else next - 1] else { val yesterday = today.clone() as Calendar; yesterday.add(Calendar.DAY_OF_MONTH, -1); prev = timesCached(c, yesterday)[5] } }
                    val lx = com.example.ui.i18n.L10n; val names = arrayOf(lx.t(com.example.ui.i18n.S.PrayerFajr), lx.t(com.example.ui.i18n.S.PrayerShuruq), lx.t(com.example.ui.i18n.S.PrayerDhuhr), lx.t(com.example.ui.i18n.S.PrayerAsr), lx.t(com.example.ui.i18n.S.PrayerMaghrib), lx.t(com.example.ui.i18n.S.PrayerIsha)); if (today.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) names[2] = lx.t(com.example.ui.i18n.S.PrayerJumuah)
                    var iqama = false
                    if (type == Bar::class.java || type == Next::class.java || type == Countdown::class.java || type == CountdownClean::class.java) { val yesterday = today.clone() as Calendar; yesterday.add(Calendar.DAY_OF_MONTH, -1); val window = iqamaWindow(ts, timesCached(c, yesterday), now); if (window != null) { iqama = true; next = window[0].toInt(); prev = window[1]; at = window[2] } }
                    HomeWidgets.ovArm(iqama, at, now)
                    val target = if (iqama) (if (code.startsWith("ar")) "إقامة الصلاة" else PrayerPhase.iqamaFor(code, names[next])) else names[next]
                    if (type == Prayers::class.java) {
                        val indexes = intArrayOf(0, 2, 3, 4, 5); val cell = (w - 2 * pad) / 5
                        for (j in 0 until 5) { val i = indexes[j]; val x = if (rtl) w - pad - cell * (j + .5f) else pad + cell * (j + .5f); centered(cv, names[i], x, 20 * d, paint(medium, 7f, d, INK), cell - 1 * d); centered(cv, time(ts[i], l), x, 34 * d, paint(bold, 8f, d, INK), cell - 1 * d); if (i == next) { val marker = Paint(Paint.ANTI_ALIAS_FLAG); marker.color = INK; cv.drawRoundRect(RectF(x - cell * .35f, 41 * d, x + cell * .35f, 42.5f * d), d, d, marker) } }
                    }
                    else if (type == Countdown::class.java || type == CountdownClean::class.java) {
                        centered(cv, target, if (rtl) w - pad - 25 * d else pad + 25 * d, 12 * d, paint(if (rtl) bold else sysB, 13f, d, INK), 48 * d)
                        centered(cv, time(at, l), if (rtl) pad + 26 * d else w - pad - 26 * d, 12 * d, paint(if (rtl) medium else sysM, 12f, d, INK), 54 * d)
                        val p = Paint(Paint.ANTI_ALIAS_FLAG); val top = 25 * d; val width = w - 2 * pad; val bh = 3 * d; p.color = TRACK; cv.drawRoundRect(RectF(pad, top, pad + width, top + bh), bh / 2, bh / 2, p); val fraction = Math.max(0f, Math.min(1f, (now - prev) / Math.max(1, at - prev).toFloat())); val fw = Math.max(bh, width * fraction); p.color = INK; cv.drawRoundRect(RectF(pad, top, pad + fw, top + bh), bh / 2, bh / 2, p)
                        HomeWidgets.ovSemi.set(true); val mins = Math.max(0, (at - now) / 60000); val remain = if (iqama) HomeWidgets.mmss(at - now) else String.format(Locale.ENGLISH, "%02d:%02d", mins / 60, mins % 60); centered(cv, (if (type == Countdown::class.java) HomeWidgets.remPrefix(code, ": ") else "") + "⁦" + remain + "⁩", w / 2f, 41 * d, paint(if (rtl) bold else sysB, 12f, d, INK), w - 2 * pad)
                    }
                    else if (type == Bar::class.java) {
                        centered(cv, target, w / 2f, 13 * d, paint(if (rtl) bold else sysB, 13f, d, INK), w - 2 * pad); centered(cv, time(at, l), w / 2f, 42 * d, paint(if (rtl) medium else sysM, 12f, d, INK), w - 2 * pad)
                        val p = Paint(Paint.ANTI_ALIAS_FLAG); val top = 26 * d; val width = w - 2 * pad; val bh = 4 * d; p.color = TRACK; cv.drawRoundRect(RectF(pad, top, pad + width, top + bh), bh / 2, bh / 2, p); val fraction = Math.max(0f, Math.min(1f, (now - prev) / Math.max(1, at - prev).toFloat())); val fw = Math.max(bh, width * fraction); p.color = INK; cv.drawRoundRect(RectF(pad, top, pad + fw, top + bh), bh / 2, bh / 2, p)
                    }
                    else { val minutes = Math.max(0, (at - now) / 60000); val countdown = if (iqama) HomeWidgets.mmss(at - now) else HomeWidgets.durText(minutes, code); val remaining = if (iqama) HomeWidgets.remPrefix(code, " ") + HomeWidgets.mmss(at - now) else HomeWidgets.remaining(minutes, code); lines(cv, w, h, d, if (tiny) arrayOf(target, time(at, l), countdown) else arrayOf(target + " " + time(at, l), remaining), if (tiny) floatArrayOf(12f, 11f, 9f) else floatArrayOf(13f, 9f), if (tiny) arrayOf(if (rtl) bold else sysB, if (rtl) medium else sysM, if (rtl) bold else sysB) else arrayOf(if (rtl) bold else sysB, if (rtl) bold else sysB), INK, SOFT) }
                } catch (ex: Exception) { android.util.Log.e("WathakkerLock", "Prayer bridge failed", ex); lines(cv, w, h, d, arrayOf(errText(code)), floatArrayOf(11f), arrayOf(if (rtl) medium else sysM), INK, SOFT) }
            }
            return bmp
        }
    }
}
