package com.wathakker.wear
import androidx.compose.ui.unit.dp

import android.app.AlarmManager
import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import kotlinx.coroutines.delay

/** One alert at the exact adhan time for the five prayers (no sunrise). One on/off switch in Settings. */
object Adhan {
    const val CHANNEL = "adhan_v1"
    const val SHOW_SECONDS = 5
    fun enabled(c: Context) = c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).getBoolean("adhan", true)
    fun setEnabled(c: Context, v: Boolean) { c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).edit().putBoolean("adhan", v).apply(); schedule(c) }
    /** Alert style: true = full-screen (current behavior, default), false = plain notification. */
    fun fullScreen(c: Context) = c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).getBoolean("adhan_fullscreen", true)
    fun setFullScreen(c: Context, v: Boolean) { c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).edit().putBoolean("adhan_fullscreen", v).apply() }

    private fun pi(c: Context, id: Int, at: Long, flags: Int) = PendingIntent.getBroadcast(
        c, 7001, Intent(c, AdhanReceiver::class.java).putExtra("id", id).putExtra("at", at), flags or PendingIntent.FLAG_IMMUTABLE)

    /** Cancels the previous alarm and sets the next one. Safe to call any time (app open, boot, settings change, location sync, after each alert). */
    fun schedule(c: Context) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        am.cancel(pi(c, 0, 0, PendingIntent.FLAG_UPDATE_CURRENT))
        if (!enabled(c)) return
        val n = Prayer.nextAdhan(c) ?: return
        val p = pi(c, n.id, n.at, PendingIntent.FLAG_UPDATE_CURRENT)
        try {
            am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n.at, p)
        } catch (_: SecurityException) { am.setAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, n.at, p) }
        c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).edit().putLong("adhan_next", n.at).putInt("adhan_next_id", n.id).apply()
    }

    /** Debug only: sets a real alarm for the Maghrib alert [sec] seconds from now, to test AlarmManager delivery. */
    fun testIn(c: Context, sec: Int) {
        val am = c.getSystemService(Context.ALARM_SERVICE) as AlarmManager
        val at = System.currentTimeMillis() + sec * 1000L
        am.setExactAndAllowWhileIdle(AlarmManager.RTC_WAKEUP, at, pi(c, 4, at, PendingIntent.FLAG_UPDATE_CURRENT))
    }

    fun fire(c: Context, id: Int, at: Long) {
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(NotificationChannel(CHANNEL, T("تنبيه الأذان", "Adhan alert"), NotificationManager.IMPORTANCE_HIGH).apply {
            enableVibration(true); vibrationPattern = longArrayOf(0, 400, 200, 400); setSound(null, null)
        })
        val i = Intent(c, AdhanActivity::class.java).putExtra("id", id).putExtra("at", at).addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
        val full = PendingIntent.getActivity(c, 7002, i, PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT)
        val name = Prayer.names[id.coerceIn(0, 5)]
        // The notification removes itself 5 minutes after it appears, in both styles, so it
        // never lingers in the list.
        val b = Notification.Builder(c, CHANNEL).setSmallIcon(R.mipmap.ic_launcher).setContentTitle(T("حان موعد أذان ", "Time for adhan: ") + name)
            .setCategory(Notification.CATEGORY_ALARM).setContentIntent(full).setAutoCancel(true)
            .setTimeoutAfter(300_000)
        if (fullScreen(c)) b.setFullScreenIntent(full, true)
        nm.notify(7000 + id, b.build())
        c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).edit().putLong("adhan_last_fire", System.currentTimeMillis()).apply()
        if (fullScreen(c)) { try { c.startActivity(i) } catch (_: Throwable) {} }
        (c.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator).vibrate(VibrationEffect.createWaveform(longArrayOf(0, 400, 200, 400, 200, 400), -1))
    }
}

class AdhanReceiver : BroadcastReceiver() {
    override fun onReceive(c: Context, i: Intent) {
        c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).edit().putLong("adhan_last_receive", System.currentTimeMillis()).apply()
        if (i.hasExtra("id")) {
            // ignore a stale alarm delivered long after its time (e.g. phone was off)
            val at = i.getLongExtra("at", 0L)
            if (Adhan.enabled(c) && System.currentTimeMillis() - at < 5 * 60_000L) Adhan.fire(c, i.getIntExtra("id", 0), at)
        }
        Adhan.schedule(c)
    }
}

class BootReceiver : BroadcastReceiver() {
    // BOOT_COMPLETED / MY_PACKAGE_REPLACED / TIME_SET / TIMEZONE_CHANGED (manifest). With
    // UPDATE_PERIOD_SECONDS=0 the host never re-queries on its own, so time and timezone
    // changes must push a refresh here; otherwise a baked countdown target could stay
    // stale for hours after travel or a clock change.
    override fun onReceive(c: Context, i: Intent) {
        android.util.Log.i("WathakkerCd", "bootreceiver action=" + i.action)
        requestAllComplications(c)
        Adhan.schedule(c)
    }
}

class AdhanActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setShowWhenLocked(true); setTurnScreenOn(true)
        window.addFlags(android.view.WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        Settings.load(this)
        val id = intent.getIntExtra("id", 0).coerceIn(0, 5)
        val at = intent.getLongExtra("at", System.currentTimeMillis())
        setContent { AdhanScreen(id, at) { finish() } }
    }
}

@androidx.compose.runtime.Composable
fun AdhanScreen(id: Int, at: Long, close: () -> Unit) {
    LaunchedEffect(Unit) { delay(Adhan.SHOW_SECONDS * 1000L); close() }
    CompositionLocalProvider(LocalLayoutDirection provides LayoutDirection.Rtl) {
        BoxWithConstraints(Modifier.fillMaxSize().background(Color.Black).clickable { close() }) {
            val w = maxWidth.value
            val t = WTheme.cur
            Canvas(Modifier.fillMaxSize()) {
                val m = size.minDimension; val sw = m * 0.035f
                drawCircle(Brush.radialGradient(listOf(t.main.copy(alpha = 0.28f), Color.Transparent), center, m * 0.55f), m * 0.55f, center)
                drawCircle(t.main.copy(alpha = 0.25f), m / 2 - sw, center, style = Stroke(sw * 0.4f))
                drawArc(t.brush(), -90f, 360f, false, Offset(sw, sw), androidx.compose.ui.geometry.Size(m - 2 * sw, m - 2 * sw), style = Stroke(sw, cap = StrokeCap.Round))
            }
            Column(Modifier.align(Alignment.Center), horizontalAlignment = Alignment.CenterHorizontally) {
                Text1(T("حان موعد أذان", "Time for adhan"), tx(w * 0.075f, Color(0xFFD0D0D4)))
                Spacer(Modifier.height((w * 0.02f).dp))
                Text1(Prayer.names[id], tx(w * 0.2f, Color.White, true))
                Spacer(Modifier.height((w * 0.03f).dp))
                Text1(Prayer.time12(at), tx(w * 0.085f, t.main))
            }
        }
    }
}
