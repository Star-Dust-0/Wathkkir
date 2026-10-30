package com.example.ui.oneui

import android.os.Build
import androidx.compose.foundation.background
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.foundation.border
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Shape
import androidx.compose.ui.unit.dp
import dev.chrisbanes.haze.HazeState
import dev.chrisbanes.haze.HazeStyle
import dev.chrisbanes.haze.HazeTint
import com.example.ThemeSettings
import dev.chrisbanes.haze.hazeEffect

val LocalHazeState = compositionLocalOf<HazeState?> { null }

val blurSupported: Boolean
    get() = Build.VERSION.SDK_INT >= 31

/** 1dp hairline border shared by every frosted surface in the app. */
@Composable
fun oneUIBorderColor(): Color =
    if (ThemeSettings.isDark()) Color.White.copy(alpha = 0.10f) else Color.Black.copy(alpha = 0.10f)

/**
 * v3.2.0 "one glass system": the frost material every card, box, bar and dialog in
 * the app shares (same recipe as the prayer-screen cards: blur + translucent tint +
 * hairline border + noise). Tint follows the theme so text stays legible in both
 * themes; alphaOverride for surfaces that need extra solidity (bars, tiles, dialogs).
 */
@Composable
fun themeFrostSpec(alphaOverride: Float? = null): FrostSpec {
    val dark = ThemeSettings.isDark()
    return FrostSpec(
        base = MaterialTheme.colorScheme.surfaceContainerHigh,
        alpha = alphaOverride ?: if (dark) 0.50f else 0.62f,
        border = oneUIBorderColor()
    )
}

/** Frosted-material spec (base tint, tint alpha, border) shared across surfaces. */
class FrostSpec(val base: Color, val alpha: Float, val border: Color)

/**
 * Same blur recipe as oneUIGlass (haze, 0.04 noise) but with an explicit tint alpha
 * and a subtle 1dp border - the v3.2.0 "one glass system" material used by the prayer
 * cards, and by the header pills / bottom bar / drawer while the prayer sky is showing.
 */
@Composable
fun Modifier.oneUIFrosted(shape: Shape, spec: FrostSpec, blurRadius: Int = 28): Modifier {
    val state = LocalHazeState.current
    return if (state != null && blurSupported) {
        this.clip(shape)
            .hazeEffect(
                state,
                HazeStyle(spec.base, HazeTint(spec.base.copy(alpha = spec.alpha)), blurRadius.dp, 0.04f)
            )
            .border(1.dp, spec.border, shape)
    } else {
        this.clip(shape).background(spec.base.copy(alpha = 0.96f))
    }
}

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

