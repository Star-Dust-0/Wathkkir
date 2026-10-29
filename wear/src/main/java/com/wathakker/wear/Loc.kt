package com.wathakker.wear

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue

/** Bumped on every language change so composables that read it recompose. */
var langTick by mutableIntStateOf(0)

/** App language. Mode 0 follows the phone ("phone_lang", Arabic by default), 1 = Arabic, 2 = English. */
object Loc {
    @Volatile var en: Boolean = false

    fun mode(c: Context): Int = c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).getInt("lang", 0)

    fun save(c: Context, mode: Int) {
        c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).edit().putInt("lang", mode).apply()
        refresh(c)
    }

    fun refresh(c: Context) {
        en = when (mode(c)) {
            1 -> false
            2 -> true
            else -> c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).getString("phone_lang", "ar") == "en"
        }
        langTick++
    }
}

fun T(ar: String, en: String): String = if (Loc.en) en else ar
