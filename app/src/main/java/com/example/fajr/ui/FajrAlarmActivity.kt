package com.example.fajr.ui

import com.example.prayer.PrayerAlertScreen

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

/** Full-screen Fajr alarm (original 3.4.2 FajrAlarmActivity): plays the ringtone, vibrates, stops on tap or after the max duration. */
class FajrAlarmActivity : ComponentActivity() {
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
        prayer = intent.getIntExtra("prayer_index", 0)
        val nameKey = when (prayer) {
            0 -> S.PrayerFajr; 1 -> S.PrayerShuruq; 2 -> S.PrayerDhuhr
            3 -> S.PrayerAsr; 4 -> S.PrayerMaghrib; else -> S.PrayerIsha
        }
        val prayerName = L10n.t(if (prayer == 2 && java.util.Calendar.getInstance().get(java.util.Calendar.DAY_OF_WEEK) == 6) S.PrayerJumuah else nameKey)
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
            val uri = RingtoneHelper(this).getValidRingtoneUri(prefs)
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
        val vibrate = prefs.isVibrationEnabled
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

