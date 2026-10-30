package com.example.fajr.ui

import com.example.ui.i18n.L10n
import com.example.ui.i18n.S

import android.app.KeyguardManager
import java.util.Calendar
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
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import com.example.fajr.alarm.FajrAlarmScheduler
import com.example.fajr.data.FajrPreferences
import com.example.fajr.sound.RingtoneHelper
import com.example.ui.theme.MyApplicationTheme
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Alarm
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Snooze
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import com.example.ui.oneui.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Composable
fun FajrAlarmScreen(prayerName: String, onDismiss: () -> Unit, onSnooze: (Int) -> Unit) {
    var currentTimeStr by remember { mutableStateOf("") }
    var currentDateStr by remember { mutableStateOf("") }
    var showSnoozeOptions by remember { mutableStateOf(false) }
    val ctxAlarm = androidx.compose.ui.platform.LocalContext.current

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
                            contentDescription = "Fajr Alarm",
                            modifier = Modifier.size(50.dp),
                            tint = Color(0xFF60A5FA)
                        )
                    }
                    Spacer(modifier = Modifier.height(20.dp))
                    Text("صلاة الفجر", color = Color.White, fontSize = 32.sp, fontWeight = FontWeight.Bold)
                    Spacer(modifier = Modifier.height(8.dp))
                    Text("حان الآن موعد صلاة الفجر", color = Color(0xFF94A3B8), fontSize = 18.sp)
                    Spacer(modifier = Modifier.height(32.dp))
                    Text(currentTimeStr, color = Color.White, fontSize = 48.sp, fontWeight = FontWeight.ExtraBold)
                    Text(currentDateStr, color = Color(0xFFCBD5E1), fontSize = 16.sp)
                }
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    if (!showSnoozeOptions) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(12.dp)
                        ) {
                            OutlinedButton(
                                onClick = { onSnooze(com.example.fajr.data.FajrPreferences(ctxAlarm).snoozeMinutes) },
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(20.dp)
                            ) {
                                Icon(Icons.Outlined.Snooze, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("غفوة", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                            Button(
                                onClick = onDismiss,
                                modifier = Modifier.weight(1f).height(56.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = ButtonDefaults.buttonColors(containerColor = Color(0xFFEF4444))
                            ) {
                                Icon(Icons.Outlined.NotificationsOff, contentDescription = null, tint = Color.White)
                                Spacer(modifier = Modifier.width(8.dp))
                                Text("إيقاف المنبه", color = Color.White, fontSize = 16.sp, fontWeight = FontWeight.Bold)
                            }
                        }
                    } else {
                        Surface(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(24.dp),
                            color = Color(0xFF1E293B)
                        ) {
                            Column(
                                modifier = Modifier.padding(16.dp),
                                horizontalAlignment = Alignment.CenterHorizontally
                            ) {
                                Text("اختر مدة الغفوة (Snooze):", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold)
                                Spacer(modifier = Modifier.height(12.dp))
                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.SpaceEvenly
                                ) {
                                    listOf(5 to "5 دقائق", 10 to "10 دقائق", 15 to "15 دقيقة").forEach { (minutes, label) ->
                                        Button(
                                            onClick = { onSnooze(minutes) },
                                            colors = ButtonDefaults.buttonColors(containerColor = Color(0xFF334155))
                                        ) {
                                            Text(label, color = Color.White)
                                        }
                                    }
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(24.dp))
                }
            }
        }
    }
}


class FajrAlarmActivity : ComponentActivity() {
    private val handler = Handler(Looper.getMainLooper())
    private var mediaPlayer: MediaPlayer? = null
    private var vibrator: Vibrator? = null
    private var autoStopRunnable: Runnable? = null

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        L10n.init(this)
        if (Build.VERSION.SDK_INT >= 27) {
            setShowWhenLocked(true)
            setTurnScreenOn(true)
            val km = getSystemService("keyguard") as KeyguardManager
            km.requestDismissKeyguard(this, null)
        } else {
            @Suppress("DEPRECATION")
            window.addFlags(6815872)
        }
        val prefs = FajrPreferences(this)
        startAlarmEffects(prefs)
        val prayerIndex = intent.getIntExtra("prayer_index", 0)
        val nameKey = when (prayerIndex) {
            1 -> S.PrayerShuruq; 2 -> S.PrayerDhuhr; 3 -> S.PrayerAsr
            4 -> S.PrayerMaghrib; 5 -> S.PrayerIsha; else -> S.PrayerFajr
        }
        val prayerName = if (prayerIndex == 2 && Calendar.getInstance().get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY) L10n.t(S.PrayerJumuah) else L10n.t(nameKey)
        setContent {
            MyApplicationTheme {
                CompositionLocalProvider(LocalLayoutDirection provides if (L10n.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr) {
                    FajrAlarmScreen(
                        prayerName = prayerName,
                        onDismiss = { stopAndFinish() },
                        onSnooze = { minutes ->
                            val p = FajrPreferences(this@FajrAlarmActivity)
                            p.snoozeCount = p.snoozeCount + 1
                            FajrAlarmScheduler(this@FajrAlarmActivity).scheduleSnooze(minutes)
                            stopAndFinish()
                        }
                    )
                }
            }
        }
    }

    private fun startAlarmEffects(prefs: FajrPreferences) {
        try {
            val uri = RingtoneHelper(this).getValidRingtoneUri(prefs)
            val volume = prefs.alarmVolume / 100.0f
            val mp = MediaPlayer()
            mp.setAudioAttributes(AudioAttributes.Builder().setUsage(4).setContentType(2).build())
            mp.setDataSource(this, uri)
            mp.isLooping = true
            mp.setVolume(volume, volume)
            mp.prepare()
            mp.start()
            mediaPlayer = mp
        } catch (e: Exception) {
            e.printStackTrace()
        }
        if (prefs.isVibrationEnabled) {
            try {
                vibrator = if (Build.VERSION.SDK_INT >= 31) {
                    (getSystemService("vibrator_manager") as VibratorManager).defaultVibrator
                } else {
                    @Suppress("DEPRECATION")
                    getSystemService("vibrator") as Vibrator
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
        val r = Runnable { handleAutoStop() }
        autoStopRunnable = r
        handler.postDelayed(r, 60000L)
    }

    private fun handleAutoStop() {
        val prefs = FajrPreferences(this)
        if (prefs.snoozeCount < 3) {
            prefs.snoozeCount = prefs.snoozeCount + 1
            FajrAlarmScheduler(this).scheduleSnooze(prefs.snoozeMinutes)
        }
        stopAndFinish()
    }

    private fun stopAndFinish() {
        autoStopRunnable?.let { handler.removeCallbacks(it) }
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
