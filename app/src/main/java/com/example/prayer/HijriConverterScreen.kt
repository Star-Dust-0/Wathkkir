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
import androidx.compose.foundation.BorderStroke
import com.example.utils.clickable
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.KeyboardArrowDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.ui.graphics.Color
import androidx.compose.material3.MaterialTheme
import com.example.ui.oneui.Text
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
import androidx.compose.ui.unit.sp
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ThemeSettings
import com.example.ui.oneui.FrostSpec
import com.example.ui.oneui.oneUIFrosted
import com.example.ui.oneui.oneUIBorderColor
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
                .clip(RoundedCornerShape(14.dp)).background(cs.surfaceVariant.copy(alpha = 0.6f))
                .padding(horizontal = 12.dp, vertical = 12.dp),
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

/** One UI month spinner (v3.2.0, his call): month is picked ONLY from a
 *  drop-down list of month names - no typing. Same frosted field as DateField;
 *  the popup follows One UI: large rounded elevated card, hairline border,
 *  generous rows, selected item in One UI blue with a check at the end side. */
@Composable
private fun MonthField(
    selected: Int?,
    onPick: (Int) -> Unit,
    label: String,
    names: List<String>,
    modifier: Modifier = Modifier
) {
    val cs = MaterialTheme.colorScheme
    var open by remember { mutableStateOf(false) }
    val blue = if (ThemeSettings.isDark()) Color(0xFF78B8F8) else Color(0xFF2E6DB4)
    Column(modifier = modifier) {
        Text(label, color = cs.onSurfaceVariant, fontSize = 11.sp, fontWeight = FontWeight.Medium)
        Spacer(Modifier.height(4.dp))
        Box {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clip(RoundedCornerShape(14.dp)).background(cs.surfaceVariant.copy(alpha = 0.6f))
                    .clickable { open = true }
                    .padding(horizontal = 12.dp, vertical = 12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    selected?.let { names.getOrNull(it - 1) } ?: "",
                    color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Medium,
                    textAlign = TextAlign.Center, modifier = Modifier.weight(1f)
                )
                Icon(Icons.Outlined.KeyboardArrowDown, contentDescription = null,
                    tint = cs.onSurfaceVariant, modifier = Modifier.width(18.dp))
            }
            DropdownMenu(
                expanded = open,
                onDismissRequest = { open = false },
                shape = RoundedCornerShape(24.dp),
                containerColor = com.example.ui.oneui.oneUiMenuColor(),
                tonalElevation = 0.dp,
                shadowElevation = 12.dp,
                border = null
            ) {
                com.example.ui.theme.AppFontScaled { Column {
                names.forEachIndexed { i, name ->
                    val sel = selected == i + 1
                    DropdownMenuItem(
                        text = {
                            Text(
                                name,
                                color = if (sel) blue else cs.onSurface,
                                fontWeight = if (sel) FontWeight.Bold else FontWeight.Normal,
                                style = MaterialTheme.typography.bodyLarge
                            )
                        },
                        trailingIcon = if (sel) {
                            { Icon(Icons.Outlined.Check, contentDescription = null, tint = blue) }
                        } else null,
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(horizontal = 24.dp, vertical = 4.dp),
                        onClick = { onPick(i + 1); open = false }
                    )
                }
                } }
            }
        }
    }
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
    var gMonth by remember { mutableStateOf<Int?>(null) }
    var gYear by remember { mutableStateOf("") }

    var hDay by remember { mutableStateOf("") }
    var hMonth by remember { mutableStateOf<Int?>(null) }
    var hYear by remember { mutableStateOf("") }

    val gMonths = gregorianMonths(locale)
    val hMonths = (1..12).map { HijriDate.INSTANCE.monthName(it, langCode) }

    // G -> H result (null until fields form a valid Gregorian date)
    val gY = gYear.trim().toIntOrNull()
    val gM = gMonth
    val gD = gDay.trim().toIntOrNull()
    val gValid = gY != null && gY in G_YEAR_MIN..G_YEAR_MAX && gM != null &&
        gD != null && gD in 1..gregorianMaxDay(gY, gM)
    val resultHijriCal: Calendar? = if (gValid) {
        Calendar.getInstance().apply { set(gY!!, gM!! - 1, gD!!) }
    } else null

    // H -> G result
    val hY = hYear.trim().toIntOrNull()
    val hM = hMonth
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
            // 3.3.3: shared One UI segmented control (joined container, sliding blue indicator).
            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                com.example.ui.oneui.OneUISegmented(
                    options = listOf(L10n.t(S.GregToHijri), L10n.t(S.HijriToGreg)),
                    selected = if (hijriToG) 1 else 0,
                    onSelect = { hijriToG = it == 1 },
                    maxLines = 2
                )
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
                                HijriDate.INSTANCE.format(resultHijriCal, langCode)
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
                        gMonth = now.get(Calendar.MONTH) + 1
                        gYear = now.get(Calendar.YEAR).toString()
                        val hy = HijriDate.INSTANCE.hijriYmd(now)
                        hDay = hy.third!!.toString()
                        hMonth = hy.second
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
