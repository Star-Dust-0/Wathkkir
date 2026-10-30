package com.example.prayer.alerts

import android.app.NotificationChannel
import java.util.Calendar
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S

/**
 * Fires a per-prayer bell. Bells are always ordinary notifications (no alarm UX):
 * the adhan bell posts a high-priority notification and the iqama bell a normal one
 * on its own channel; both follow the shared sound/vibration settings.
 * Always reschedules the next occurrence afterwards.
 */
class PrayerAlertReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        L10n.init(context)
        val prayer = intent.getIntExtra(PrayerAlertScheduler.EXTRA_PRAYER, -1)
        val kind = intent.getIntExtra(PrayerAlertScheduler.EXTRA_KIND, PrayerAlertScheduler.KIND_ADHAN)
        if (prayer !in 0..5) return

        if (kind == PrayerAlertScheduler.KIND_DND_OFF) {
            restoreDnd(context)
            return
        }
        // 3.3.0: while the full-screen Fajr alarm is on, the Fajr bell does not also notify at the adhan.
        if (prayer == 0 && kind == PrayerAlertScheduler.KIND_ADHAN &&
            com.example.fajr.data.FajrPreferences(context).isAlarmEnabled) {
            android.util.Log.i("FajrBellSuppressed", "fajr adhan bell skipped, full-screen alarm is on")
            PrayerAlertScheduler.scheduleAll(context)
            return
        }
        if (!PrayerAlertStore.isEnabled(context, prayer)) return

        val nameKey = when (prayer) {
            0 -> S.PrayerFajr; 1 -> S.PrayerShuruq; 2 -> S.PrayerDhuhr
            3 -> S.PrayerAsr; 4 -> S.PrayerMaghrib; else -> S.PrayerIsha
        }
        val prayerName = if (prayer == 2 && Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) L10n.t(S.PrayerJumuah) else L10n.t(nameKey)
        val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val isIqama = kind == PrayerAlertScheduler.KIND_IQAMA
        val isEarly = kind == PrayerAlertScheduler.KIND_EARLY
        val baseId = if (isIqama) IQAMA_CHANNEL else ALERT_CHANNEL
        // A prayer with its own tone/vibration gets its own channel, so it never touches the others.
        val channelBase = if (PrayerAlertStore.hasOverride(context, prayer)) baseId + "_p" + prayer else baseId
        val channelId = ensureChannel(context, nm, channelBase, prayer, prayerName)
        val openIntent = PendingIntent.getActivity(
            context, 400 + prayer + kind * 10,
            Intent(context, MainActivity::class.java)
                .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
        )
        val shuruq = prayer == 1
        val at = intent.getLongExtra(PrayerAlertScheduler.EXTRA_AT, 0L)
        val now = System.currentTimeMillis()
        val text = if (isIqama) {
            L10n.t(S.IqamaNowFmt).format(prayerName)
        } else if (isEarly) {
            if (shuruq) L10n.t(S.ShuruqApproaching) else L10n.t(S.PrayerApproachingFmt).format(prayerName)
        } else {
            if (shuruq) L10n.t(S.ShuruqNow) else L10n.t(S.PrayerNowFmt).format(prayerName)
        }
        if (isEarly && at <= now) return // too late to count down to anything; the adhan notification covers it
        val builder = NotificationCompat.Builder(context, channelId)
            .setSmallIcon(R.drawable.ic_stat_prayer)
            .setLargeIcon(android.graphics.BitmapFactory.decodeResource(context.resources, R.mipmap.ic_launcher))
            .setContentTitle(prayerName)
            .setContentText(text)
            .setPriority(if (isIqama) NotificationCompat.PRIORITY_DEFAULT else NotificationCompat.PRIORITY_HIGH)
            .setCategory(NotificationCompat.CATEGORY_REMINDER)
            .setContentIntent(openIntent)
            .setAutoCancel(true)
        if (isEarly) {
            // Native live countdown to the prayer; the notification removes itself at that moment
            // and the adhan notification (or nothing, if that bell is off) takes over.
            builder.setShowWhen(true).setWhen(at).setUsesChronometer(true).setChronometerCountDown(true)
                .setTimeoutAfter(at - now)
        }
        else {
            // Adhan and iqama notifications remove themselves 5 minutes after they appear. The system
            // enforces the timeout (API 26+), so it also works when the app process is gone. This applies
            // to the full-screen style too: the alert never lingers in the notification list.
            builder.setTimeoutAfter(5 * 60 * 1000L)
        }
        if (kind == PrayerAlertScheduler.KIND_ADHAN && PrayerAlertStore.fullScreen(context, prayer)) {
            val fsIntent = PendingIntent.getActivity(
                context, 600 + prayer,
                Intent(context, com.example.prayer.PrayerAlertActivity::class.java)
                    .putExtra(PrayerAlertScheduler.EXTRA_PRAYER, prayer)
                    .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP),
                PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE
            )
            builder.setCategory(NotificationCompat.CATEGORY_ALARM)
                .setFullScreenIntent(fsIntent, true)
            try {
                context.startActivity(
                    Intent(context, com.example.prayer.PrayerAlertActivity::class.java)
                        .putExtra(PrayerAlertScheduler.EXTRA_PRAYER, prayer)
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP)
                )
            } catch (_: Exception) {}
        }
        val notification = builder.build()
        if (kind == PrayerAlertScheduler.KIND_ADHAN) nm.cancel(400 + prayer + PrayerAlertScheduler.KIND_EARLY * 10)
        nm.notify(400 + prayer + kind * 10, notification)

        if (kind == PrayerAlertScheduler.KIND_ADHAN && PrayerAlertStore.autoDnd(context, prayer)) {
            enableDnd(context)
        }

        // Schedule the next occurrence (tomorrow or later today).
        PrayerAlertScheduler.scheduleAll(context)
    }

    /**
     * Creates the bell channels, applying the shared tone/vibration settings. Channel
     * sound/vibration is sticky after creation, so when those settings change the channel
     * is deleted and recreated (tracked by a stored signature).
     */
    internal fun ensureChannel(context: Context, nm: NotificationManager, channelId: String, prayer: Int, prayerName: String): String {
        if (Build.VERSION.SDK_INT < 26) return channelId
        val isIqama = channelId.startsWith(IQAMA_CHANNEL)
        val own = Regex(".*_p\\d$").matches(channelId)
        val base = L10n.t(if (isIqama) S.IqamaReminder else S.AdhanReminder)
        val name = if (own) "$base - $prayerName" else base
        val channel = NotificationChannel(
            channelId, name,
            if (isIqama) NotificationManager.IMPORTANCE_DEFAULT else NotificationManager.IMPORTANCE_HIGH
        )
        channel.setShowBadge(true)
        return if (own) {
            com.example.utils.NotifPrefs.configureChannel(
                context, nm, channel,
                PrayerAlertStore.effectiveTone(context, prayer), PrayerAlertStore.effectiveVibration(context, prayer)
            )
        } else {
            com.example.utils.NotifPrefs.configureChannel(context, nm, channel)
        }
    }

    private fun enableDnd(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY)
            }
        } catch (_: Throwable) {}
    }

    private fun restoreDnd(context: Context) {
        try {
            val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
            if (nm.isNotificationPolicyAccessGranted) {
                nm.setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
            }
        } catch (_: Throwable) {}
    }

    companion object {
        const val ALERT_CHANNEL = "prayer_alerts_channel"
        const val IQAMA_CHANNEL = "iqama_reminders_channel"
        private const val CHANNEL_PREFS = "bell_channels"

        /** Applies the current tone/vibration to the prayer channels right away (shared channels,
         *  plus one channel per prayer that has its own choice; stale per-prayer channels are removed). */
        fun refreshChannels(context: Context) {
            if (Build.VERSION.SDK_INT < 26) return
            try {
                L10n.init(context)
                val nm = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
                val r = PrayerAlertReceiver()
                r.ensureChannel(context, nm, ALERT_CHANNEL, 0, "")
                r.ensureChannel(context, nm, IQAMA_CHANNEL, 0, "")
                for (p in 0..5) {
                    if (PrayerAlertStore.hasOverride(context, p)) {
                        val nameKey = when (p) {
                            0 -> S.PrayerFajr; 1 -> S.PrayerShuruq; 2 -> S.PrayerDhuhr
                            3 -> S.PrayerAsr; 4 -> S.PrayerMaghrib; else -> S.PrayerIsha
                        }
                        r.ensureChannel(context, nm, ALERT_CHANNEL + "_p" + p, p, L10n.t(nameKey))
                        r.ensureChannel(context, nm, IQAMA_CHANNEL + "_p" + p, p, L10n.t(nameKey))
                    } else {
                        com.example.utils.NotifPrefs.deleteVariants(nm, ALERT_CHANNEL + "_p" + p, null)
                        com.example.utils.NotifPrefs.deleteVariants(nm, IQAMA_CHANNEL + "_p" + p, null)
                    }
                }
            } catch (t: Throwable) { android.util.Log.e("NotifPrefs", "prayer channels failed", t) }
        }
    }
}
