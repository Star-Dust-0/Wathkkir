package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.R

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Normal),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)

private fun TextStyle.oneUi(size: TextUnit, line: TextUnit, bold: Boolean = false, family: FontFamily? = null) = copy(
    fontFamily = family ?: fontFamily,
    fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
    fontSize = size,
    lineHeight = line,
    // Arabic letters must never be tracked: positive letter spacing breaks joining.
    letterSpacing = 0.sp
)

/** One UI type scale, zero tracking (Arabic safe). */
fun oneUiTypography(family: FontFamily?): Typography {
    val base = TextStyle.Default
    return Typography(
        displayLarge = base.oneUi(57.sp, 64.sp, false, family),
        displayMedium = base.oneUi(45.sp, 52.sp, false, family),
        displaySmall = base.oneUi(36.sp, 44.sp, false, family),
        headlineLarge = base.oneUi(34.sp, 42.sp, true, family),
        headlineMedium = base.oneUi(28.sp, 36.sp, true, family),
        headlineSmall = base.oneUi(24.sp, 32.sp, true, family),
        titleLarge = base.oneUi(20.sp, 28.sp, true, family),
        titleMedium = base.oneUi(17.sp, 24.sp, true, family),
        titleSmall = base.oneUi(15.sp, 22.sp, true, family),
        bodyLarge = base.oneUi(17.sp, 26.sp, false, family),
        bodyMedium = base.oneUi(15.sp, 22.sp, false, family),
        bodySmall = base.oneUi(13.sp, 18.sp, false, family),
        labelLarge = base.oneUi(15.sp, 20.sp, true, family),
        labelMedium = base.oneUi(13.sp, 18.sp, true, family),
        labelSmall = base.oneUi(12.sp, 16.sp, true, family)
    )
}

fun getTypography(useDeviceFont: Boolean): Typography =
    oneUiTypography(if (useDeviceFont) null else TajawalFontFamily)
