package com.example

import android.content.Context
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow

/** App-wide theme mode (auto/light/dark), persisted in app_prefs. */
object ThemeSettings {
    const val AUTO = "auto"
    const val LIGHT = "light"
    const val DARK = "dark"

    private const val PREFS = "app_prefs"
    private const val KEY = "theme_mode"

    private val _mode = MutableStateFlow(AUTO)
    val mode: StateFlow<String> = _mode

    fun init(context: Context) {
        _mode.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, AUTO) ?: AUTO
    }

    fun set(context: Context, value: String) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, value).apply()
        _mode.value = value
    }

    @Composable
    fun isDark(): Boolean {
        val m by mode.collectAsState()
        val system = isSystemInDarkTheme()
        return when (m) {
            DARK -> true
            LIGHT -> false
            else -> system
        }
    }
}


/** App-wide text size multiplier (on top of the system font scale), persisted in app_prefs. */
object FontScaleSettings {
    val STEPS = floatArrayOf(0.8f, 0.9f, 1.0f, 1.1f, 1.2f, 1.3f, 1.4f, 1.5f, 1.6f)
    private const val PREFS = "app_prefs"
    private const val KEY = "font_scale"

    /** Fresh-install default: step index 1 (0.9x) - second step from the small-A end, per Mohamed 22.10.2026. Existing installs keep their stored value. */
    const val DEFAULT = 0.9f
    private var loaded = false

    private val _scale = MutableStateFlow(DEFAULT)
    val scale: StateFlow<Float> = _scale

    fun init(context: Context) {
        if (loaded) return
        loaded = true
        _scale.value = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getFloat(KEY, DEFAULT)
    }

    fun set(context: Context, value: Float) {
        loaded = true
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putFloat(KEY, value).apply()
        _scale.value = value
    }

    fun nearest(value: Float): Int = STEPS.indices.minByOrNull { kotlin.math.abs(STEPS[it] - value) } ?: 2

    fun label(value: Float, ar: Boolean): String = when {
        value < 0.95f -> if (ar) "صغير" else "Small"
        value < 1.05f -> if (ar) "افتراضي" else "Default"
        value < 1.25f -> if (ar) "كبير" else "Large"
        else -> if (ar) "أكبر" else "Largest"
    }
}
