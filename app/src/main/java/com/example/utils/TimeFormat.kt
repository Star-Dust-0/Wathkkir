package com.example.utils

import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import java.util.Locale

/** "HH:mm" to "hh:mm AM/PM" with the localized marker (original y42.y). Returns the input when it does not parse. */
fun formatTimeStr12h(time: String): String {
    val parts = time.split(":")
    if (parts.size == 2) {
        val h = parts[0].toIntOrNull()
        val m = parts[1].toIntOrNull()
        if (h != null && m != null) {
            val h12 = (h % 12).let { if (it != 0) it else 12 }
            return String.format(Locale.getDefault(), "%02d:%02d %s", h12, m, L10n.t(if (h < 12) S.AmMarker else S.PmMarker))
        }
    }
    return time
}
