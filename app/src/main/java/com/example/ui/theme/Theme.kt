package com.example.ui.theme

import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.graphics.compositeOver
import androidx.compose.ui.unit.dp

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

private fun fam(book: Int, bold: Int, medium: Int? = null) = androidx.compose.ui.text.font.FontFamily(
    androidx.compose.ui.text.font.Font(book, androidx.compose.ui.text.font.FontWeight.Normal),
    androidx.compose.ui.text.font.Font(bold, androidx.compose.ui.text.font.FontWeight.Bold)
)

/** Font family per choice (original z42.a). SYSTEM is null: the platform font. */
fun fontFamilyFor(choice: FontChoice): androidx.compose.ui.text.font.FontFamily? = when (choice) {
    FontChoice.APP -> TajawalFontFamily
    FontChoice.APP_LIGHT -> fam(com.example.R.font.expoarabic_book, com.example.R.font.expoarabic_medium)
    FontChoice.APP_BOLD -> fam(com.example.R.font.expoarabic_bold, com.example.R.font.expoarabic_bold)
    FontChoice.SYSTEM -> null
    FontChoice.CAIRO -> fam(com.example.R.font.cairo, com.example.R.font.cairo_bold)
    FontChoice.NASKH -> fam(com.example.R.font.noto_naskh, com.example.R.font.noto_naskh_bold)
    FontChoice.AMIRI -> fam(com.example.R.font.amiri, com.example.R.font.amiri_bold)
    FontChoice.RUQAA -> fam(com.example.R.font.aref_ruqaa, com.example.R.font.aref_ruqaa_bold)
}

private val Black = androidx.compose.ui.graphics.Color(0xFF000000)
private val White = androidx.compose.ui.graphics.Color(0xFFFFFFFF)
private val Accent = androidx.compose.ui.graphics.Color(0xFF3E91FF)
private val AccentLight = androidx.compose.ui.graphics.Color(0xFF0381FE)
private val Grey = androidx.compose.ui.graphics.Color(0xFF8E8E93)

// Colour schemes rebuilt from the original n22 static initialiser (role mapping inferred; verified by pixel diff).
private val DarkScheme = androidx.compose.material3.darkColorScheme(
    primary = Accent, onPrimary = White,
    primaryContainer = Accent.copy(alpha = 0x29 / 255f).compositeOver(androidx.compose.ui.graphics.Color(0xFF171719)),
    onPrimaryContainer = Accent,
    secondary = Grey, onSecondary = White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFF2B2B2B), onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFFE0E0E0),
    tertiary = Grey,
    background = Black, onBackground = White,
    surface = androidx.compose.ui.graphics.Color(0xFF171719), onSurface = White,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFF2B2B2B), onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFFA6A6A6),
    error = androidx.compose.ui.graphics.Color(0xFFFF5A5F),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFFF5A5F).copy(alpha = 0x29 / 255f).compositeOver(androidx.compose.ui.graphics.Color(0xFF171719)),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFFF5A5F),
    outline = androidx.compose.ui.graphics.Color(0x1FFFFFFF), outlineVariant = androidx.compose.ui.graphics.Color(0x1FFFFFFF),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFF333333),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFF2B2B2B)
)
val ForcedDarkScheme get() = DarkScheme
val ForcedLightScheme get() = LightScheme
private val LightScheme = androidx.compose.material3.lightColorScheme(
    primary = AccentLight, onPrimary = White,
    primaryContainer = AccentLight.copy(alpha = 0x24 / 255f).compositeOver(White),
    onPrimaryContainer = AccentLight,
    secondary = Grey, onSecondary = White,
    secondaryContainer = androidx.compose.ui.graphics.Color(0xFFF1F1F1), onSecondaryContainer = androidx.compose.ui.graphics.Color(0xFF333333),
    tertiary = Grey,
    background = androidx.compose.ui.graphics.Color(0xFFF6F6F6), onBackground = Black,
    surface = White, onSurface = Black,
    surfaceVariant = androidx.compose.ui.graphics.Color(0xFFF1F1F1), onSurfaceVariant = androidx.compose.ui.graphics.Color(0xFF6E6E6E),
    error = androidx.compose.ui.graphics.Color(0xFFE5484D),
    errorContainer = androidx.compose.ui.graphics.Color(0xFFE5484D).copy(alpha = 0x1F / 255f).compositeOver(White),
    onErrorContainer = androidx.compose.ui.graphics.Color(0xFFE5484D),
    outline = androidx.compose.ui.graphics.Color(0x1F000000), outlineVariant = androidx.compose.ui.graphics.Color(0x1F000000),
    surfaceContainer = androidx.compose.ui.graphics.Color(0xFFECECF0),
    surfaceContainerHigh = androidx.compose.ui.graphics.Color(0xFFF1F1F1)
)

private val AppShapes = androidx.compose.material3.Shapes(
    extraSmall = androidx.compose.foundation.shape.RoundedCornerShape(8.dp),
    small = androidx.compose.foundation.shape.RoundedCornerShape(12.dp),
    medium = androidx.compose.foundation.shape.RoundedCornerShape(18.dp),
    large = androidx.compose.foundation.shape.RoundedCornerShape(26.dp),
    extraLarge = androidx.compose.foundation.shape.RoundedCornerShape(32.dp)
)

/** App theme (original n22.b): light/dark scheme, font family, RTL from the language, app text scale. */
@androidx.compose.runtime.Composable
fun MyApplicationTheme(
    font: FontChoice = FontChoice.APP,
    content: @androidx.compose.runtime.Composable () -> Unit
) {
    com.example.FontScaleSettings.load(androidx.compose.ui.platform.LocalContext.current)
    val dark = com.example.ThemeSettings.isDark()
    val scheme = if (dark) DarkScheme else LightScheme
    androidx.compose.material3.MaterialTheme(
        colorScheme = scheme,
        typography = oneUiTypography(fontFamilyFor(font)),
        shapes = AppShapes
    ) {
        AppFontScaled {
            CompositionLocalProvider(
                androidx.compose.ui.platform.LocalLayoutDirection provides
                    if (com.example.ui.i18n.L10n.isRtl) androidx.compose.ui.unit.LayoutDirection.Rtl else androidx.compose.ui.unit.LayoutDirection.Ltr,
                content = content
            )
        }
    }
}
