package com.example.ui.theme
import androidx.compose.runtime.getValue
import androidx.compose.runtime.collectAsState

import android.app.Activity
import android.os.Build
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.material3.Shapes
import androidx.compose.material3.darkColorScheme
import androidx.compose.material3.dynamicDarkColorScheme
import androidx.compose.material3.dynamicLightColorScheme
import androidx.compose.material3.lightColorScheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.SideEffect
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.core.view.WindowCompat
import com.example.ui.i18n.L10n

private val DarkColorScheme = darkColorScheme(
    primary = OneUIAccentDark,
    onPrimary = Color.White,
    primaryContainer = Color(0x293E91FF).compositeOver(OneUISurfaceDark),
    onPrimaryContainer = OneUIAccentDark,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    secondaryContainer = OneUISurfaceVariantDark,
    onSecondaryContainer = Color(0xFFE0E0E0),
    tertiary = Color(0xFF8E8E93),
    background = OneUIBackgroundDark,
    onBackground = OneUITextPrimaryDark,
    surface = OneUISurfaceDark,
    onSurface = OneUITextPrimaryDark,
    surfaceVariant = OneUISurfaceVariantDark,
    onSurfaceVariant = OneUITextSecondaryDark,
    error = OneUIErrorDark,
    errorContainer = Color(0x29FF5A5F).compositeOver(OneUISurfaceDark),
    onErrorContainer = OneUIErrorDark,
    outline = OneUIDividerDark,
    outlineVariant = OneUIDividerDark,
    surfaceContainerHigh = OneUIPillDark
)

private val LightColorScheme = lightColorScheme(
    primary = OneUIAccentLight,
    onPrimary = Color.White,
    primaryContainer = Color(0x240381FE).compositeOver(OneUISurfaceLight),
    onPrimaryContainer = OneUIAccentLight,
    secondary = Color(0xFF8E8E93),
    onSecondary = Color.White,
    secondaryContainer = OneUISurfaceVariantLight,
    onSecondaryContainer = Color(0xFF333333),
    tertiary = Color(0xFF8E8E93),
    background = OneUIBackgroundLight,
    onBackground = OneUITextPrimaryLight,
    surface = OneUISurfaceLight,
    onSurface = OneUITextPrimaryLight,
    surfaceVariant = OneUISurfaceVariantLight,
    onSurfaceVariant = OneUITextSecondaryLight,
    error = OneUIErrorLight,
    errorContainer = Color(0x1FE5484D).compositeOver(OneUISurfaceLight),
    onErrorContainer = OneUIErrorLight,
    outline = OneUIDividerLight,
    outlineVariant = OneUIDividerLight,
    surfaceContainerHigh = OneUIPillLight
)

val OneUIShapes = Shapes(
    RoundedCornerShape(8.dp),
    RoundedCornerShape(12.dp),
    RoundedCornerShape(18.dp),
    RoundedCornerShape(26.dp),
    RoundedCornerShape(32.dp)
)

@Composable
fun MyApplicationTheme(
    fontChoice: FontChoice = FontChoice.APP,
    darkTheme: Boolean = isSystemInDarkTheme(),
    dynamicColor: Boolean = false,
    content: @Composable () -> Unit
) {
    val colorScheme = when {
        dynamicColor && Build.VERSION.SDK_INT >= 31 -> {
            val context = LocalContext.current
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme
        else -> LightColorScheme
    }
    val view = LocalView.current
    if (!view.isInEditMode) {
        SideEffect {
            val window = (view.context as? Activity)?.window
            if (window != null) {
                WindowCompat.getInsetsController(window, view).isAppearanceLightStatusBars = !darkTheme
            }
        }
    }
    MaterialTheme(
        colorScheme = colorScheme,
        shapes = OneUIShapes,
        typography = getTypography(fontChoice)
    ) {
        val appCtx = LocalContext.current.applicationContext
        androidx.compose.runtime.remember { com.example.FontScaleSettings.init(appCtx) }
        val userScale by com.example.FontScaleSettings.scale.collectAsState()
        val baseDensity = androidx.compose.ui.platform.LocalDensity.current
        CompositionLocalProvider(
            LocalLayoutDirection provides (if (L10n.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr),
            androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(baseDensity.density, baseDensity.fontScale * userScale),
            content = content
        )
    }
}


/**
 * Popup windows (menus, dialogs) must follow the in-app text size too. The density is rebuilt from the
 * device values plus the app multiplier, so the result is the same whether or not the window inherits it.
 */
@androidx.compose.runtime.Composable
fun AppFontScaled(content: @androidx.compose.runtime.Composable () -> Unit) {
    val ctx = androidx.compose.ui.platform.LocalContext.current
    val userScale by com.example.FontScaleSettings.scale.collectAsState()
    val dm = ctx.resources.displayMetrics
    val sys = ctx.resources.configuration.fontScale
    CompositionLocalProvider(
        androidx.compose.ui.platform.LocalDensity provides androidx.compose.ui.unit.Density(dm.density, sys * userScale),
        content = content
    )
}
