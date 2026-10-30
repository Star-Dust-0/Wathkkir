package com.example.fajr.sound

import android.content.Context
import android.media.MediaPlayer
import android.media.AudioAttributes
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.example.fajr.data.FajrPreferences
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S

// Secondary reconstruction of uncovered helper: original APK v6.e and i40.e,
// historical helper method signatures and FajrAlarmActivity playback path.
class RingtoneHelper(private val context: Context) {
    private var mediaPlayer: MediaPlayer? = null
    fun getValidRingtoneUri(prefs: FajrPreferences): Uri {
        if (prefs.ringtoneUri.isNotEmpty()) {
            val uri = Uri.parse(prefs.ringtoneUri)
            try { context.contentResolver.openFileDescriptor(uri, "r")?.close(); return uri } catch (_: Exception) {}
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI
    }
    fun getRingtoneTitle(uri: Uri): String {
        try {
            context.contentResolver.query(uri, null, null, null, null)?.use { cursor ->
                if (cursor.moveToFirst()) {
                    val col = cursor.getColumnIndex("_display_name")
                    if (col != -1) cursor.getString(col)?.takeIf { it.isNotEmpty() }?.let { return it }
                }
            }
            RingtoneManager.getRingtone(context, uri)?.getTitle(context)?.takeIf { it.isNotEmpty() }?.let { return it }
        } catch (e: Exception) { e.printStackTrace() }
        return L10n.t(S.DefaultAlarmTone)
    }
    fun playPreview(uri: Uri, volume: Float) {
        stopPreview()
        try {
            mediaPlayer = MediaPlayer().apply {
                setAudioAttributes(AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_ALARM).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build())
                setDataSource(context, uri); setVolume(volume, volume); prepare(); start()
            }
        } catch (e: Exception) { e.printStackTrace(); stopPreview() }
    }
    fun stopPreview() { try { mediaPlayer?.stop(); mediaPlayer?.release() } catch (_: Exception) {} finally { mediaPlayer = null } }
}
