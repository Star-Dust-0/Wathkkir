package com.example

import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/** Theme mode: "auto" (follow system), "light" or "dark", saved as "theme_mode". Rebuilt from the original (R8 name o22). */
object ThemeSettings {
    private val _mode = MutableStateFlow("auto")
    val mode: StateFlow<String> = _mode.asStateFlow()

    fun setMode(value: String) { _mode.value = value }

    @Composable
    fun isDark(): Boolean {
        val m by mode.collectAsState()
        val system = isSystemInDarkTheme()
        return when (m) {
            "dark" -> true
            "light" -> false
            else -> system
        }
    }
}
