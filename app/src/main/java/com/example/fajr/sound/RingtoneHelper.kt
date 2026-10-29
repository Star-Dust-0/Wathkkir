package com.example.fajr.sound

import android.content.Context
import android.media.RingtoneManager
import android.net.Uri
import android.provider.Settings
import com.example.fajr.data.FajrPreferences
import com.example.fajr.util.DeviceHelper

/** Ringtone title and a usable alarm uri (saved uri when still readable, else the device default alarm sound). */
class RingtoneHelper(private val context: Context) {
    fun getRingtoneTitle(uri: Uri): String = DeviceHelper(context).ringtoneTitle(uri)

    fun getValidRingtoneUri(prefs: FajrPreferences): Uri {
        val saved = prefs.ringtoneUri
        if (saved.isNotEmpty()) {
            val u = Uri.parse(saved)
            try { context.contentResolver.openFileDescriptor(u, "r")?.close(); return u } catch (_: Exception) {}
        }
        return RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            ?: RingtoneManager.getDefaultUri(RingtoneManager.TYPE_NOTIFICATION)
            ?: Settings.System.DEFAULT_ALARM_ALERT_URI
    }
}
