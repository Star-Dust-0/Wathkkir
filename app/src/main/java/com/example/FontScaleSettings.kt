package com.example

import android.content.Context
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** In-app text size multiplier, saved as "font_scale" (default 0.9). Rebuilt from the original (R8 name p80). */
object FontScaleSettings {
    val STEPS = floatArrayOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f)
    const val DEFAULT = 0.9f

    private var loaded = false
    private val _scale = MutableStateFlow(DEFAULT)
    val scale: StateFlow<Float> = _scale.asStateFlow()

    fun load(context: Context) {
        if (loaded) return
        loaded = true
        _scale.value = context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getFloat("font_scale", DEFAULT)
    }

    fun set(context: Context, value: Float) {
        loaded = true
        context.applicationContext.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).edit().putFloat("font_scale", value).apply()
        _scale.value = value
    }

    /** Index of the step closest to [value]. */
    fun nearest(value: Float): Int {
        var best = 0
        for (i in STEPS.indices) if (kotlin.math.abs(STEPS[i] - value) < kotlin.math.abs(STEPS[best] - value)) best = i
        return best
    }
}
