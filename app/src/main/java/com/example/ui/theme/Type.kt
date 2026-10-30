package com.example.ui.theme

import androidx.compose.material3.Typography
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.Font
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.TextUnit
import androidx.compose.ui.unit.sp
import com.example.R

enum class FontChoice(val id: String, val titleAr: String) {
    APP("app", "خط التطبيق"),
    APP_LIGHT("app_light", "خط التطبيق الخفيف"),
    APP_BOLD("app_bold", "خط التطبيق العريض"),
    SYSTEM("system", "خط النظام"),
    CAIRO("cairo", "خط القاهرة"),
    NASKH("naskh", "خط النسخ"),
    AMIRI("amiri", "خط أميري"),
    RUQAA("ruqaa", "خط الرقعة (للمناسبات)");

    companion object {
        fun fromId(id: String?): FontChoice = entries.firstOrNull { it.id == id } ?: APP
    }
}

val TajawalFontFamily = FontFamily(
    Font(R.font.tajawal, FontWeight.Normal),
    Font(R.font.tajawal_bold, FontWeight.Bold)
)
val TajawalLightFontFamily = FontFamily(
    Font(R.font.expoarabic_book, FontWeight.Normal),
    Font(R.font.expoarabic_medium, FontWeight.Bold)
)
val TajawalBoldFontFamily = FontFamily(
    Font(R.font.expoarabic_bold, FontWeight.Normal),
    Font(R.font.expoarabic_bold, FontWeight.Bold)
)
val CairoFontFamily = FontFamily(
    Font(R.font.cairo, FontWeight.Normal),
    Font(R.font.cairo_bold, FontWeight.Bold)
)
val NotoNaskhFontFamily = FontFamily(
    Font(R.font.noto_naskh, FontWeight.Normal),
    Font(R.font.noto_naskh_bold, FontWeight.Bold)
)
val AmiriFontFamily = FontFamily(
    Font(R.font.amiri, FontWeight.Normal),
    Font(R.font.amiri_bold, FontWeight.Bold)
)
val ArefRuqaaFontFamily = FontFamily(
    Font(R.font.aref_ruqaa, FontWeight.Normal),
    Font(R.font.aref_ruqaa_bold, FontWeight.Bold)
)

fun fontFamilyFor(choice: FontChoice): FontFamily? = when (choice) {
    FontChoice.APP -> TajawalFontFamily
    FontChoice.APP_LIGHT -> TajawalLightFontFamily
    FontChoice.APP_BOLD -> TajawalBoldFontFamily
    FontChoice.SYSTEM -> null
    FontChoice.CAIRO -> CairoFontFamily
    FontChoice.NASKH -> NotoNaskhFontFamily
    FontChoice.AMIRI -> AmiriFontFamily
    FontChoice.RUQAA -> ArefRuqaaFontFamily
}

private fun TextStyle.oneUi(size: TextUnit, line: TextUnit, bold: Boolean = false, family: FontFamily? = null): TextStyle =
    copy(
        fontSize = size,
        fontWeight = if (bold) FontWeight.Bold else FontWeight.Normal,
        fontFamily = family ?: fontFamily,
        letterSpacing = 0.sp,
        lineHeight = line
    )

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

fun getTypography(choice: FontChoice): Typography = oneUiTypography(fontFamilyFor(choice))
