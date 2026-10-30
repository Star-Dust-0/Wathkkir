package com.example.utils

import com.example.ui.i18n.L10n
import com.example.ui.i18n.Lang

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.AnnotatedString
import androidx.compose.ui.text.SpanStyle
import androidx.compose.ui.text.buildAnnotatedString
import androidx.compose.ui.text.input.OffsetMapping
import androidx.compose.ui.text.input.TransformedText
import androidx.compose.ui.text.input.VisualTransformation

/**
 * Single place that decides how digits look. Choice: Hindi-Indic digits (١٢٣) or Latin (1234).
 * Applies only while the app language is Arabic. Phone app, home/lock widgets and previews use
 * this. Notifications and the watch do not.
 */
object Digits {
    private const val PREFS = "app_prefs"
    const val KEY = "digits_style"
    const val HINDI = "hindi"
    const val LATIN = "latin"

    private var hindiPref by mutableStateOf(false)

    /** Last values read for non-Compose callers (widgets): Arabic UI, and the digit choice. */
    @Volatile private var widgetArabic = false
    @Volatile private var widgetHindi = false

    fun init(context: Context) {
        hindiPref = context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getString(KEY, LATIN) == HINDI
    }

    fun set(context: Context, hindi: Boolean) {
        context.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putString(KEY, if (hindi) HINDI else LATIN).apply()
        hindiPref = hindi
    }

    /** True when the choice is Hindi digits (regardless of language, for the setting UI). */
    val isHindiChoice: Boolean get() = hindiPref

    /** The numbers setting applies only while the app language is Arabic. */
    val active: Boolean get() = com.example.ui.i18n.L10n.lang == com.example.ui.i18n.Lang.AR

    /** Re-read the setting for drawing code outside Compose (widgets, previews). Call at the start of a render batch. */
    @JvmStatic
    fun refresh(context: Context) {
        val p = context.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
        val lang = p.getString("language", "ar") ?: "ar"
        widgetArabic = lang.startsWith("ar")
        widgetHindi = p.getString(KEY, LATIN) == HINDI
    }

    @JvmStatic
    fun map(s: String): String = if (!widgetArabic) s else convert(s, widgetHindi)

    /** For widgets and previews, after refresh(). */
    @JvmStatic
    fun forWidget(s: String?): String? = if (s == null || !widgetArabic) s else convert(s, widgetHindi)

    @JvmStatic
    fun forWidget(s: CharSequence): CharSequence = if (!widgetArabic) s else convert(s.toString(), widgetHindi)

    /** For Compose text. In Arabic the chosen style is applied to every digit, so Latin and Hindi never mix. */
    fun ui(s: String): String = if (active) convert(s, hindiPref) else s

    fun ui(s: AnnotatedString): AnnotatedString {
        if (!active) return s
        val t = convert(s.text, hindiPref)
        return if (t == s.text) s else AnnotatedString(t, s.spanStyles, s.paragraphStyles)
    }

    /** One place that rewrites digits (1:1 characters): Latin 0-9 and Hindi-Indic U+0660..0669 (Persian/Urdu forms untouched). */
    private fun convert(s: String, hindi: Boolean): String {
        var needs = false
        for (c in s) if ((c in '0'..'9') || (c in '\u0660'..'\u0669')) { needs = true; break }
        if (!needs) return s
        val sb = StringBuilder(s.length)
        for (c in s) {
            sb.append(
                if (hindi && c in '0'..'9') ('\u0660' + (c - '0'))
                else if (!hindi && c in '\u0660'..'\u0669') ('0' + (c - '\u0660'))
                else c
            )
        }
        return sb.toString()
    }

    /** Shows Hindi digits in text fields without changing the typed value (1:1 characters). */
    val visual = VisualTransformation { text ->
        TransformedText(ui(text), OffsetMapping.Identity)
    }
}
