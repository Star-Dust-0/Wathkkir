package com.example.ui.theme

import com.example.ui.i18n.L10n
import com.example.ui.i18n.S

/** App font options saved as "app_font". Rebuilt from the original (R8 name o70). */
enum class FontChoice(val id: String, private val label: S) {
    APP("app", S.AppFont),
    APP_LIGHT("app_light", S.FontLight),
    APP_BOLD("app_bold", S.FontBold),
    SYSTEM("system", S.FontSystem),
    CAIRO("cairo", S.FontCairo),
    NASKH("naskh", S.FontNaskh),
    AMIRI("amiri", S.FontAmiri),
    RUQAA("ruqaa", S.FontRuqaa);

    /** Localized name (follows the current app language). */
    val title: String get() = L10n.t(label)

    companion object {
        fun fromId(id: String?): FontChoice = entries.firstOrNull { it.id == id } ?: APP
    }
}
