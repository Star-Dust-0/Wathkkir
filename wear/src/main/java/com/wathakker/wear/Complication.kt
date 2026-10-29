package com.wathakker.wear

import android.app.AlarmManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.util.Log
import androidx.wear.watchface.complications.data.ComplicationData
import androidx.wear.watchface.complications.data.ComplicationType
import androidx.wear.watchface.complications.data.PlainComplicationText
import androidx.wear.watchface.complications.data.RangedValueComplicationData
import androidx.wear.watchface.complications.data.CountDownTimeReference
import androidx.wear.watchface.complications.data.ShortTextComplicationData
import androidx.wear.watchface.complications.data.TimeDifferenceComplicationText
import androidx.wear.watchface.complications.data.TimeDifferenceStyle
import androidx.wear.watchface.complications.datasource.ComplicationDataSourceUpdateRequester
import androidx.wear.watchface.complications.datasource.ComplicationRequest
import androidx.wear.watchface.complications.datasource.SuspendingComplicationDataSourceService

/** Remaining time to the next prayer, with a progress ring on faces that draw one. Tapping opens the five prayer circles. */
open class PrayerComplicationService : SuspendingComplicationDataSourceService() {

    private fun tap(): PendingIntent = PendingIntent.getActivity(
        this, 0,
        Intent(this, CirclesActivity::class.java).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK),
        PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
    )

    /** 0 = remaining countdown (and ranged ring); other values are the extra types, see [buildKind]. */
    open val kind: Int get() = 0

    /** Provider class name used to cancel legacy minute-pulse PendingIntents. */
    open val selfName: String get() = javaClass.name

    /** Countdown text style; previews override to compare native renderings. */
    // Native STOPWATCH countdown "7:53": minute-precise in every locale (Latin digits via the
    // library's Locale.US format). SHORT_DUAL_UNIT lost the minutes under an Arabic system
    // locale (7-char dual limit -> hour-ceil single unit), so STOPWATCH replaced it.
    open val cdStyle: TimeDifferenceStyle get() = TimeDifferenceStyle.STOPWATCH

    fun build(type: ComplicationType, s: PrayerState, preview: Boolean = false, iq: Prayer.Iqama? = null): ComplicationData? = if (kind == 0) buildMain(type, s, preview, iq) else buildKind(kind, type, s, preview, iq)

    private fun txt(t: String) = PlainComplicationText.Builder(t).build()

    /** 1 greg date, 2 hijri date, 3 greg+hijri (long), 4 next prayer time, 8 upcoming prayer countdown. */
    fun buildKind(k: Int, type: ComplicationType, s: PrayerState, preview: Boolean = false, iq: Prayer.Iqama? = null): ComplicationData? {
        val cd = txt("-")
        fun short(text: androidx.wear.watchface.complications.data.ComplicationText, title: String?, d: String): ComplicationData =
            ShortTextComplicationData.Builder(text, txt(d)).apply { if (title != null) setTitle(txt(title)) }.setTapAction(tap()).build()
        if (k == 3) {
            if (type != ComplicationType.LONG_TEXT) return null
            return androidx.wear.watchface.complications.data.LongTextComplicationData.Builder(
                txt(Prayer.gregDate(s.now)), txt(T("التاريخ", "Date"))).setTitle(txt(Prayer.hijri(s.now))).setTapAction(tap()).build()
        }
        if (type != ComplicationType.SHORT_TEXT) return null
        val cal = java.util.Calendar.getInstance().apply { timeInMillis = s.now }
        // System-rendered countdown (no per-minute provider wakeups). The system renderer rounds the
        // remaining time UP to the smallest displayed unit (AOSP TimeDifferenceText.roundUpToUnit), so
        // the reference end is shifted one minute back: ceil(d-60s) equals the floor-to-minutes value
        // the rest of the app shows, changing at the same instants. MINUTES keeps seconds hidden;
        // displayAsNow=false renders "0 min" (localized) during the final minute like previewCountdown.
        fun down(at: Long) = nativeCountdown(at)
        return when (k) {
            1 -> short(txt(cal.get(java.util.Calendar.DAY_OF_MONTH).toString()), Prayer.gregDate(s.now).substringAfter(' '), T("التاريخ الميلادي", "Gregorian date"))
            2 -> short(txt(Prayer.hijriDay(s.now)), Prayer.hijriMonth(s.now), T("التاريخ الهجري", "Hijri date"))
            4 -> short(txt(Prayer.time12(s.next.at).substringBefore(' ')), s.next.name, T("موعد الصلاة القادمة", "Next prayer time"))
            8 -> if (iq != null) short(down(iq.at), T("الإقامة", "Iqama"), T("العد التنازلي للإقامة", "Iqama countdown"))
                 else short(down(s.next.at), s.next.name, T("العد التنازلي للصلاة القادمة", "Next prayer countdown"))
            else -> null
        }
    }

    fun buildMain(type: ComplicationType, s: PrayerState, preview: Boolean = false, iq: Prayer.Iqama? = null): ComplicationData? {
        // During an iqama window: title "إقامة <prayer>" and a floor-minute countdown to the iqama time.
        val countdown = nativeCountdown(iq?.at ?: s.next.at)
        val title = PlainComplicationText.Builder(if (iq != null) T("الإقامة", "Iqama") else s.next.name).build()
        val desc = PlainComplicationText.Builder(if (iq != null) T("الوقت المتبقي لإقامة ", "Time left until iqama of ") + iq.prayer.name else T("الوقت المتبقي لصلاة ", "Time left until ") + s.next.name).build()
        return when (type) {
            ComplicationType.RANGED_VALUE -> {
                val total = (((iq?.at ?: s.next.at) - (iq?.prayer?.at ?: s.prev.at)) / 60000f).coerceAtLeast(1f)
                val elapsed = ((s.now - (iq?.prayer?.at ?: s.prev.at)) / 60000f).coerceIn(0f, total)
                RangedValueComplicationData.Builder(elapsed, 0f, total, desc)
                    .setText(countdown).setTitle(title).setTapAction(tap()).build()
            }
            ComplicationType.SHORT_TEXT ->
                ShortTextComplicationData.Builder(countdown, desc).setTitle(title).setTapAction(tap()).build()
            else -> null
        }
    }

    override fun getPreviewData(type: ComplicationType): ComplicationData? = build(type, Prayer.compute(this), preview = true)

    /** Native countdown text; host renders and advances it without waking this provider. */
    internal fun nativeCountdown(target: Long) = TimeDifferenceComplicationText.Builder(
        cdStyle,
        CountDownTimeReference(java.time.Instant.ofEpochMilli(target - 60_000L))
    ).setMinimumTimeUnit(java.util.concurrent.TimeUnit.MINUTES).setDisplayAsNow(false).build()

    /** Push fresh data when the current countdown target passes (prayer or iqama time). */
    internal fun scheduleBoundary(target: Long) {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val sp = getSharedPreferences("cdb", Context.MODE_PRIVATE)
        val want = target + 2000
        val pending = sp.getLong("armed", 0L)
        // The boundary PI is shared across all providers via FLAG_UPDATE_CURRENT: arming a
        // later target would overwrite a pending earlier one, and across an adhan/iqama
        // crossover (a request computed just after a target passed, before the boundary
        // fired) that could postpone another class's refresh for hours. Only move it earlier.
        if (pending > System.currentTimeMillis() && pending <= want) {
            android.util.Log.i("WathakkerCd", "boundary skip pending=$pending want=$want")
            return
        }
        val pi = PendingIntent.getBroadcast(this, 8002,
            Intent(this, ComplicationBoundaryReceiver::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE)
        // The marker is written only after an alarm is actually scheduled, so a failed arm
        // can never suppress a later re-arm. (Exact -> inexact fallback on permission loss.)
        val armedOk = try { am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, want, pi); true }
        catch (_: SecurityException) {
            try { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, want, pi); true }
            catch (_: Throwable) { false }
        }
        if (armedOk) {
            sp.edit().putLong("armed", want).apply()
            android.util.Log.i("WathakkerCd", "boundary arm want=$want")
        } else {
            android.util.Log.i("WathakkerCd", "boundary arm FAILED want=$want")
        }
    }

    /** Battery-policy change from original 1.1.2: remove the old minute wake pulse.
     * The system's provider update period refreshes ranged values; text remains a
     * native TimeDifferenceComplicationText. Only prayer/iqama boundaries wake us.
     */
    internal fun cancelLegacyMinutePulse() {
        val am = getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(this, 8001,
            Intent(this, ComplicationTickReceiver::class.java).setAction("tick_" + selfName).putExtra("cls", selfName),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) { am.cancel(pi); pi.cancel() }
    }

    override suspend fun onComplicationRequest(request: ComplicationRequest): ComplicationData? {
        val s = Prayer.compute(this)
        val iq = Prayer.iqamaNow(this)
        Log.i("WathakkerCd", "request cls=$selfName id=${request.complicationInstanceId} type=${request.complicationType}")
        if (kind == 0 || kind == 8) {
            scheduleBoundary(iq?.at ?: s.next.at)
            cancelLegacyMinutePulse()
        }
        return build(request.complicationType, s, iq = iq)
    }
}

/** Compatibility receiver: discard a legacy minute pulse without requesting or rearming updates. */
class ComplicationTickReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        val cls = i.getStringExtra("cls") ?: return
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val pi = PendingIntent.getBroadcast(c, 8001,
            Intent(c, ComplicationTickReceiver::class.java).setAction("tick_" + cls).putExtra("cls", cls),
            PendingIntent.FLAG_NO_CREATE or PendingIntent.FLAG_IMMUTABLE)
        if (pi != null) { am.cancel(pi); pi.cancel() }
        Log.i("WathakkerCd", "legacy minute pulse discarded cls=$cls")
    }
}

/** Fires when a countdown target passes; pulls fresh data so the next prayer/iqama target is served. */
class ComplicationBoundaryReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        Log.i("WathakkerCd", "boundary fire")
        c.getSharedPreferences("cdb", 0).edit().remove("armed").apply()
        for (cls in listOf(PrayerComplicationService::class.java, UpcomingCountdownComplication::class.java, NextTimeComplication::class.java)) {
            ComplicationDataSourceUpdateRequester.create(c, ComponentName(c, cls)).requestUpdateAll()
        }
    }
}

class DateGregComplication : PrayerComplicationService() { override val kind get() = 1 }
class DateHijriComplication : PrayerComplicationService() { override val kind get() = 2 }
class DateBothComplication : PrayerComplicationService() { override val kind get() = 3 }
class NextTimeComplication : PrayerComplicationService() { override val kind get() = 4 }
class UpcomingCountdownComplication : PrayerComplicationService() { override val kind get() = 8 }

