package com.example.prayer

import android.icu.util.IslamicCalendar
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.widthIn
import com.example.utils.plainClickable
import com.example.ui.oneui.OneUIButton
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.BasicTextField
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.RestartAlt
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.SolidColor
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.SectionLabel
import java.text.DateFormatSymbols
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

private const val G_YEAR_MIN = 1900
private const val G_YEAR_MAX = 2100
private const val H_YEAR_MIN = 1300
private const val H_YEAR_MAX = 1600

private fun newUmalqura(): IslamicCalendar {
    val ic = IslamicCalendar()
    ic.setCalculationType(IslamicCalendar.CalculationType.ISLAMIC_UMALQURA)
    return ic
}

private fun gregorianMaxDay(year: Int, month1: Int): Int {
    val cal = Calendar.getInstance()
    cal.set(year, month1 - 1, 1)
    return cal.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun hijriMaxDay(year: Int, month1: Int): Int {
    val ic = newUmalqura()
    ic.set(year, month1 - 1, 1)
    return ic.getActualMaximum(Calendar.DAY_OF_MONTH)
}

private fun hijriToGregorian(year: Int, month1: Int, day: Int): Calendar {
    val ic = newUmalqura()
    ic.set(year, month1 - 1, day)
    val cal = Calendar.getInstance()
    cal.time = ic.time
    return cal
}

private fun gregorianMonths(locale: Locale): List<String> =
    DateFormatSymbols.getInstance(locale).months.toList().filter { it.isNotEmpty() }

private fun weekdayOf(cal: Calendar, locale: Locale): String =
    SimpleDateFormat("EEEE", locale).format(cal.time)

private fun gregorianText(cal: Calendar, locale: Locale): String =
    SimpleDateFormat("d MMMM yyyy", locale).format(cal.time)

/** One UI filled rounded field for one date part. */
@Composable
private fun DateField(
    value: String,
    onChange: (String) -> Unit,
    label: String,
    modifier: Modifier = Modifier,
    numeric: Boolean = true
) {
    val cs = MaterialTheme.colorScheme
    Column(modifier = modifier) {
        Text(label, color = cs.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(14.dp))
                .background(cs.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 15.45.dp),
            contentAlignment = Alignment.Center
        ) {
            BasicTextField(
                value = value,
                onValueChange = onChange,
                singleLine = true,
                visualTransformation = com.example.utils.Digits.visual,
                textStyle = TextStyle(
                    color = cs.onSurface, fontSize = 16.sp,
                    fontWeight = FontWeight.Medium, textAlign = TextAlign.Center
                ),
                cursorBrush = SolidColor(cs.primary),
                keyboardOptions = if (numeric) KeyboardOptions(keyboardType = KeyboardType.Number) else KeyboardOptions.Default,
                modifier = Modifier.fillMaxWidth()
            )
        }
    }
}

/** Parses a month entry: a 1-12 number or a localized month name (case-insensitive). */
@Composable
private fun MonthField(value: String, onChange: (String) -> Unit, label: String, names: List<String>, modifier: Modifier = Modifier) =
    DateField(value, onChange, label, modifier, numeric = false)

private fun parseMonth(raw: String, names: List<String>): Int? {
    val t = raw.trim()
    if (t.isEmpty()) return null
    t.toIntOrNull()?.let { if (it in 1..12) return it }
    val idx = names.indexOfFirst { it.equals(t, ignoreCase = true) }
    return if (idx >= 0) idx + 1 else null
}

/** Hijri <-> Gregorian date converter: D/M/Y filled fields per direction,
 *  empty by default, result card appears when the input is a valid date,
 *  Today row fills the fields. */
@Composable
fun HijriConverterScreen(onBack: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    val langCode = L10n.lang.code
    val locale = remember(langCode) { Locale(langCode) }

    var hijriToG by remember { mutableStateOf(false) }
    var gDay by remember { mutableStateOf("") }
    var gMonth by remember { mutableStateOf("") }
    var gYear by remember { mutableStateOf("") }

    var hDay by remember { mutableStateOf("") }
    var hMonth by remember { mutableStateOf("") }
    var hYear by remember { mutableStateOf("") }

    val gMonths = gregorianMonths(locale)
    val hMonths = (1..12).map { HijriDate.monthName(it) }

    // G -> H result (null until fields form a valid Gregorian date)
    val gY = gYear.trim().toIntOrNull()
    val gM = parseMonth(gMonth, gMonths)
    val gD = gDay.trim().toIntOrNull()
    val gValid = gY != null && gY in G_YEAR_MIN..G_YEAR_MAX && gM != null &&
        gD != null && gD in 1..gregorianMaxDay(gY, gM)
    val resultHijriCal: Calendar? = if (gValid) {
        Calendar.getInstance().apply { set(gY!!, gM!! - 1, gD!!) }
    } else null

    // H -> G result
    val hY = hYear.trim().toIntOrNull()
    val hM = parseMonth(hMonth, hMonths)
    val hD = hDay.trim().toIntOrNull()
    val hValid = hY != null && hY in H_YEAR_MIN..H_YEAR_MAX && hM != null &&
        hD != null && hD in 1..hijriMaxDay(hY, hM)
    val resultGregCal: Calendar? = if (hValid) hijriToGregorian(hY!!, hM!!, hD!!) else null

    OneUIScreen(title = L10n.t(S.HijriConvTitle), onBack = onBack) {
        item(key = "explainer") {
            Text(
                L10n.t(S.HijriConvSub),
                color = cs.onSurfaceVariant, fontSize = 13.sp,
                modifier = Modifier.padding(horizontal = 20.dp, vertical = 8.dp)
            )
        }
        item(key = "direction") {
            // One UI segmented control (same pill as the Calendar / Events switch): two
            // exclusive directions, the box below shows the matching fields and month list.
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 20.dp, vertical = 6.dp)
                    .clip(RoundedCornerShape(24.dp))
                    .background(cs.surfaceVariant.copy(alpha = 0.55f))
                    .padding(4.dp),
                horizontalArrangement = Arrangement.Center
            ) {
                listOf(false to L10n.t(S.GregToHijri), true to L10n.t(S.HijriToGreg)).forEach { (dir, label) ->
                    val active = hijriToG == dir
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(20.dp))
                            .background(if (active) cs.surface else Color.Transparent)
                            .plainClickable { hijriToG = dir }
                            .padding(vertical = 9.dp, horizontal = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            label,
                            color = if (active) cs.onSurface else cs.onSurfaceVariant,
                            fontSize = 14.sp, lineHeight = 19.4.sp, fontWeight = if (active) FontWeight.Bold else FontWeight.Medium,
                            textAlign = TextAlign.Center, maxLines = 2
                        )
                    }
                }
            }
        }
        item(key = "box") {
            OneUICard {
                Column(Modifier.fillMaxWidth().padding(16.dp)) {
                    if (!hijriToG) {
                        Row(Modifier.fillMaxWidth()) {
                            DateField(gDay, { gDay = it.filter { ch -> ch.isDigit() }.take(2) }, L10n.t(S.DayLabel), Modifier.weight(1f))
                            Spacer(Modifier.width(10.dp))
                            MonthField(gMonth, { gMonth = it }, L10n.t(S.MonthLabel), gMonths, Modifier.weight(1.4f))
                            Spacer(Modifier.width(10.dp))
                            DateField(gYear, { gYear = it.filter { ch -> ch.isDigit() }.take(4) }, L10n.t(S.YearLabel), Modifier.weight(1f))
                        }
                        if (resultHijriCal != null) {
                            ConverterResult(
                                weekdayOf(resultHijriCal, locale),
                                HijriDate.format(resultHijriCal, langCode)
                            )
                        }
                    } else {
                        Row(Modifier.fillMaxWidth()) {
                            DateField(hDay, { hDay = it.filter { ch -> ch.isDigit() }.take(2) }, L10n.t(S.DayLabel), Modifier.weight(1f))
                            Spacer(Modifier.width(10.dp))
                            MonthField(hMonth, { hMonth = it }, L10n.t(S.MonthLabel), hMonths, Modifier.weight(1.4f))
                            Spacer(Modifier.width(10.dp))
                            DateField(hYear, { hYear = it.filter { ch -> ch.isDigit() }.take(4) }, L10n.t(S.YearLabel), Modifier.weight(1f))
                        }
                        if (resultGregCal != null) {
                            ConverterResult(weekdayOf(resultGregCal, locale), gregorianText(resultGregCal, locale))
                        }
                    }
                }
            }
        }
        item(key = "today") {
            Box(Modifier.fillMaxWidth().padding(top = 4.dp), contentAlignment = Alignment.Center) {
                OneUIButton(
                    text = L10n.t(S.Today),
                    filled = false,
                    modifier = Modifier.widthIn(min = 140.dp),
                    onClick = {
                        val now = Calendar.getInstance()
                        gDay = now.get(Calendar.DAY_OF_MONTH).toString()
                        gMonth = (now.get(Calendar.MONTH) + 1).toString()
                        gYear = now.get(Calendar.YEAR).toString()
                        val hy = HijriDate.hijriYmd(now)
                        hDay = hy.third!!.toString()
                        hMonth = hy.second.toString()
                        hYear = hy.first!!.toString()
                    }
                )
            }
        }
        item(key = "pad") { Spacer(Modifier.height(24.dp)) }
    }
}

@Composable
private fun ConverterResult(weekday: String, dateText: String) {
    val cs = MaterialTheme.colorScheme
    Column(
        modifier = Modifier.fillMaxWidth().padding(top = 18.dp, bottom = 6.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        Text(weekday, color = cs.onSurfaceVariant, fontSize = 14.sp, textAlign = TextAlign.Center)
        Spacer(Modifier.height(4.dp))
        Text(
            dateText,
            color = cs.primary,
            fontSize = 26.sp,
            fontWeight = FontWeight.Bold,
            textAlign = TextAlign.Center
        )
    }
}
