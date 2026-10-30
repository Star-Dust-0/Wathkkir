package com.example.utils

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue

/** 3.3.0: one switch for all in-app touch haptics (toggles, Hisn counter, Tasbeeh). Default on. */
object AppHaptics {
    private var loaded = false
    var enabled by mutableStateOf(true)
        private set

    fun tick(view: android.view.View) {
        load(view.context)
        if (enabled) view.performHapticFeedback(android.view.HapticFeedbackConstants.CLOCK_TICK)
    }

    fun load(ctx: Context) {
        if (loaded) return
        loaded = true
        enabled = ctx.applicationContext.getSharedPreferences("app_haptics", Context.MODE_PRIVATE).getBoolean("enabled", true)
    }

    fun set(ctx: Context, on: Boolean) {
        loaded = true
        enabled = on
        ctx.applicationContext.getSharedPreferences("app_haptics", Context.MODE_PRIVATE).edit().putBoolean("enabled", on).apply()
    }
}
