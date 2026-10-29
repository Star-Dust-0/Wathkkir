package com.example.ui.oneui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import dev.chrisbanes.haze.hazeEffect

val LocalHazeState = compositionLocalOf<HazeState?> { null }

val blurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= 31

@Composable
fun Modifier.oneUIGlass(
    shape: Shape,
    base: Color,
    alpha: Float = 0.72f,
    blurRadius: Int = 28
): Modifier {
    val state = LocalHazeState.current
    return if (state != null && blurSupported) {
        this.clip(shape).hazeEffect(
            state,
            HazeStyle(base, HazeTint(base.copy(alpha = alpha)), blurRadius.dp, 0.04f)
        )
    } else {
        this.clip(shape).background(base.copy(alpha = if (com.example.ThemeSettings.isDark()) 0.96f else 0.97f))
    }
}


