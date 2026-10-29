package com.example.prayer

import android.app.KeyguardManager
import android.app.NotificationManager
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.os.Build
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajr.data.FajrPreferences
import com.example.fajr.sound.RingtoneHelper
import com.example.prayer.alerts.PrayerAlertStore
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.oneui.Text
import com.example.ui.theme.MyApplicationTheme
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Full-screen adhan alert (per-prayer choice in the bell settings): same visual language as the
 * Fajr alarm screen, without snooze. Plays the prayer's effective tone at the shared volume and
 * finishes itself after the shared max duration; the posted notification removes itself after
 * 5 minutes regardless (set in PrayerAlertReceiver).
 */
class PrayerAlertActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var autoStop: Runnable? = null
    private var prayer = -1

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        L10n.init(this)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            (getSystemService(KEYGUARD_SERVICE) as KeyguardManager).requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(6815872)
        }
        prayer = intent.getIntExtra("prayer_index", -1)
        val nameKey = when (prayer) {
            0 -> S.PrayerFajr; 1 -> S.PrayerShuruq; 2 -> S.PrayerDhuhr
            3 -> S.PrayerAsr; 4 -> S.PrayerMaghrib; else -> S.PrayerIsha
        }
        val prayerName = L10n.t(nameKey)
        startAlertEffects()
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides if (L10n.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    PrayerAlertScreen(prayerName = prayerName, onDismiss = { stopAndFinish() })
                }
            }
        }
    }

    private fun startAlertEffects() {
        val prefs = FajrPreferences(this)
        try {
            val tone = if (prayer in 0..5) PrayerAlertStore.INSTANCE.effectiveTone(this, prayer) else ""
            val uri = if (tone.isBlank()) RingtoneHelper(this).getValidRingtoneUri(prefs) else android.net.Uri.parse(tone)
            val volume = prefs.alarmVolume / 100.0f
            val mp = MediaPlayer()
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION).build())
            mp.setDataSource(this, uri)
            mp.isLooping = true
            mp.setVolume(volume, volume)
            mp.prepare()
            mp.start()
            mediaPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
        }
        val vibrate = if (prayer in 0..5) PrayerAlertStore.INSTANCE.effectiveVibration(this, prayer) else prefs.isVibrationEnabled
        if (vibrate) {
            try {
                vibrator = if (Build.VERSION.SDK_INT >= 31) {
                    (getSystemService(VIBRATOR_MANAGER_SERVICE) as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService(VIBRATOR_SERVICE) as Vibrator
                }
                val pattern = longArrayOf(0, 800, 500, 800, 500)
                if (Build.VERSION.SDK_INT >= 26) {
                    vibrator?.vibrate(VibrationEffect.createWaveform(pattern, 0))
                } else {
                    @Suppress("DEPRECATION")
                    vibrator?.vibrate(pattern, 0)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }
        val r = Runnable { stopAndFinish() }
        autoStop = r
        handler.postDelayed(r, prefs.maxDurationMinutes * 60000L)
    }

    private fun stopAndFinish() {
        autoStop?.let { handler.removeCallbacks(it) }
        if (prayer in 0..5) {
            try {
                (getSystemService(NOTIFICATION_SERVICE) as NotificationManager).cancel(400 + prayer)
            } catch (_: Exception) {}
        }
        try {
            mediaPlayer?.let {
                if (it.isPlaying) it.stop()
                it.release()
            }
            mediaPlayer = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            vibrator?.cancel()
            vibrator = null
        } catch (e: Exception) {
            e.printStackTrace()
        }
        finish()
    }

    override fun onDestroy() {
        stopAndFinish()
        super.onDestroy()
    }
}

@Composable
fun PrayerAlertScreen(prayerName: String, onDismiss: () -> Unit) {
    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }

    LaunchedEffect(L10n.lang) {
        val loc = Locale.forLanguageTag(L10n.lang.code)
        val timeSdf = SimpleDateFormat("hh:mm", loc)
        val amPmSdf = if (L10n.lang.code == "ar") SimpleDateFormat("a", loc) else null
        val dateSdf = SimpleDateFormat("EEEE, d MMMM yyyy", loc)
        while (true) {
            val now = Date()
            currentTimeStr = timeSdf.format(now) + " " + (amPmSdf?.format(now) ?: if (java.util.Calendar.getInstance().get(java.util.Calendar.AM_PM) == 0) L10n.t(S.AmMarker) else L10n.t(S.PmMarker))
            currentDateStr = dateSdf.format(now)
            delay(1000L)
        }
    }

    Surface(modifier = Modifier.fillMaxSize(), color = Color(0xFF0F172A)) {
        Box(modifier = Modifier.fillMaxSize().padding(24.dp)) {
            Column(
                modifier = Modifier.fillMaxSize(),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                Spacer(modifier = Modifier.height(32.dp))
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Box(
                        modifier = Modifier
                            .size(90.dp)
                            .clip(CircleShape)
                            .background(Color(0xFF3B82F6).copy(alpha = 0.2f)),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Alarm,
                            contentDescription = null,
                            modifier = Modifier.size(50.dp),
                            tint = Color(0xFF60A5FA)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text(prayerName, color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(L10n.t(S.PrayerNowFmt).format(prayerName), color = Color(0xFF94A3B8), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(currentTimeStr, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                    Text(currentDateStr, color = Color(0xFFCBD5E1), fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Button(
                        onClick = onDismiss,
                        colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF3B82F6))
                    ) {
                        Icon(Icons.Outlined.NotificationsOff, null, tint = Color.White)
                        Spacer(modifier = Modifier.size(8.dp))
                        Text(L10n.t(S.StopAlarm), color = Color.White)
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}
