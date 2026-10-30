@file:JvmName("DateTimeFunctions")
package com.example.utils

import java.util.Locale

/**
 * Utility function to format a 24-hour time string ("HH:mm") into 12-hour format ("hh:mm ص/م").
 */
fun formatTimeStr12h(timeStr: String): String {
    val parts = timeStr.split(":")
    if (parts.size != 2) return timeStr
    val hour = parts[0].toIntOrNull() ?: return timeStr
    val minute = parts[1].toIntOrNull() ?: return timeStr
    val isAm = hour < 12
    val displayHour = if (hour % 12 == 0) 12 else hour % 12
    val amPm = if (isAm) "ص" else "م"
    return String.format(Locale.getDefault(), "%02d:%02d %s", displayHour, minute, amPm)
}


object DateTimeUtilsKt { @JvmStatic fun formatTimeStr12h(s: String): String = com.example.utils.formatTimeStr12h(s) }
