package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import com.example.utils.clickable
import com.example.utils.plainClickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowLeft
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material.icons.outlined.NotificationsActive
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.fajr.calculation.PrayerAdjustments
import com.example.prayer.HijriDate
import com.example.prayer.HijriEvents
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ui.oneui.oneUIBorderColor
import com.example.ui.oneui.OneUIScreen
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** One UI-style Hijri calendar: month grid of rounded tinted cells (Gregorian
 *  number prominent, Hijri number small), Fri blue / Sat red weekend numbers,
 *  colored underline bars for event days, today highlighted, selected-day
 *  section with big number + weekday + events, month events list, plus an
 *  "Events" view with all upcoming occasions and per-event reminder bells. */
@Composable
fun HijriCalendarScreen(onMenu: (() -> Unit)? = null) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val isAr = L10n.lang.code == "ar"
    var tick by remember { mutableIntStateOf(0) } // refresh reminders/icons
    LaunchedEffect(Unit) { HijriEvents.scheduleAll(context) }

    val today = remember { Calendar.getInstance() }
    var viewMonth by remember { mutableStateOf((today.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }) }
    var selected by remember { mutableStateOf(today.clone() as Calendar) }
    var showEvents by remember { mutableStateOf(false) }

    val locale = if (isAr) Locale("ar", "EG") else Locale.forLanguageTag(L10n.lang.code)

    OneUIScreen(title = L10n.t(S.CalendarTab), onMenu = onMenu) {
        item(key = "toggle") {
            // 3.3.3: shared One UI segmented control (joined container, sliding blue indicator).
            Box(Modifier.padding(horizontal = 20.dp, vertical = 6.dp)) {
                com.example.ui.oneui.OneUISegmented(
                    options = listOf(L10n.t(S.CalendarTab), L10n.t(S.CalEvents)),
                    selected = if (showEvents) 1 else 0,
                    onSelect = { showEvents = it == 1 }
                )
            }
        }
        if (showEvents) {
            eventsViewItems(tick)
        } else {
            item(key = "month") {
                MonthView(
                    viewMonth = viewMonth,
                    selected = selected,
                    today = today,
                    locale = locale,
                    onPrev = {
                        viewMonth = (viewMonth.clone() as Calendar).apply { add(Calendar.MONTH, -1) }
                    },
                    onNext = {
                        viewMonth = (viewMonth.clone() as Calendar).apply { add(Calendar.MONTH, 1) }
                    },
                    onSelect = { selected = it }
                )
            }
            item(key = "dayevents") { SelectedDayEvents(selected, locale, tick) { tick++ } }
            item(key = "monthevents") { MonthEventsList(viewMonth, locale, tick) { tick++ } }
            item(key = "spacer") { Spacer(Modifier.height(90.dp)) }
        }
    }
}

@Composable
private fun MonthView(
    viewMonth: Calendar, selected: Calendar, today: Calendar, locale: Locale,
    onPrev: () -> Unit, onNext: () -> Unit, onSelect: (Calendar) -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val isAr = L10n.lang.code == "ar"

    // header: Hijri month of the 1st + Gregorian month range
    val off = PrayerAdjustments.hijriOffsetDays(context)
    val firstH = remember(viewMonth) {
        val c = if (off != 0) (viewMonth.clone() as Calendar).apply { add(Calendar.DATE, off) } else viewMonth
        HijriDate.INSTANCE.hijriYmd(c as Calendar)
    }
    val lastDay = (viewMonth.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, getActualMaximum(Calendar.DAY_OF_MONTH)) }
    val lastH = remember(viewMonth) {
        val c = if (off != 0) (lastDay.clone() as Calendar).apply { add(Calendar.DATE, off) } else lastDay
        HijriDate.INSTANCE.hijriYmd(c as Calendar)
    }
    val hijriTitle = remember(viewMonth, L10n.lang) {
        val m1 = HijriDate.INSTANCE.monthName(firstH.component2(), L10n.lang.code)
        val m2 = HijriDate.INSTANCE.monthName(lastH.component2(), L10n.lang.code)
        if (firstH.component2() == lastH.component2()) "$m1 ${firstH.component1()}" + (if (isAr) " هـ" else " AH")
        else "$m1 – $m2 ${lastH.component1()}" + (if (isAr) " هـ" else " AH")
    }
    val gregTitle = remember(viewMonth, L10n.lang) {
        val f = SimpleDateFormat("MMMM yyyy", locale)
        f.format(viewMonth.time)
    }

    Column(Modifier.fillMaxWidth().padding(horizontal = 14.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            IconButton(onClick = onPrev) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowLeft, null, tint = cs.onSurface)
            }
            Column(horizontalAlignment = Alignment.CenterHorizontally) {
                Text(hijriTitle, color = cs.onSurface, fontSize = 20.sp, fontWeight = FontWeight.Bold)
                Text(gregTitle, color = cs.onSurfaceVariant, fontSize = 13.sp)
            }
            IconButton(onClick = onNext) {
                Icon(Icons.AutoMirrored.Outlined.KeyboardArrowRight, null, tint = cs.onSurface)
            }
        }
        Spacer(Modifier.height(6.dp))
        // weekday header: Sunday-start; Friday blue, Saturday red (Egyptian weekend)
        val weekdays = listOf(
            L10n.t(S.WdSun), L10n.t(S.WdMon), L10n.t(S.WdTue), L10n.t(S.WdWed),
            L10n.t(S.WdThu), L10n.t(S.WdFri), L10n.t(S.WdSat)
        )
        Row(Modifier.fillMaxWidth()) {
            weekdays.forEachIndexed { i, wd ->
                val col = when (i) {
                    5 -> Color(0xFF3B82F6)
                    6 -> Color(0xFFEF4444)
                    else -> cs.onSurfaceVariant
                }
                Text(
                    wd, modifier = Modifier.weight(1f), textAlign = TextAlign.Center,
                    color = col, fontSize = 13.sp, fontWeight = FontWeight.Medium
                )
            }
        }
        Spacer(Modifier.height(6.dp))
        // 6x7 grid starting from the Sunday on/before day 1
        val gridStart = (viewMonth.clone() as Calendar).apply {
            while (get(Calendar.DAY_OF_WEEK) != Calendar.SUNDAY) add(Calendar.DATE, -1)
        }
        for (row in 0 until 6) {
            Row(Modifier.fillMaxWidth()) {
                for (col in 0 until 7) {
                    val day = (gridStart.clone() as Calendar).apply { add(Calendar.DATE, row * 7 + col) }
                    DayCell(
                        day = day,
                        inMonth = day.get(Calendar.MONTH) == viewMonth.get(Calendar.MONTH),
                        isToday = sameDay(day, today),
                        isSelected = sameDay(day, selected),
                        isFri = day.get(Calendar.DAY_OF_WEEK) == Calendar.FRIDAY,
                        isSat = day.get(Calendar.DAY_OF_WEEK) == Calendar.SATURDAY,
                        events = HijriEvents.forDay(context, day),
                        modifier = Modifier.weight(1f),
                        onClick = { onSelect(day) }
                    )
                }
            }
            Spacer(Modifier.height(3.dp))
        }
    }
}

@Composable
private fun DayCell(
    day: Calendar, inMonth: Boolean, isToday: Boolean, isSelected: Boolean,
    isFri: Boolean, isSat: Boolean, events: List<HijriEvents.Occasion>,
    modifier: Modifier, onClick: () -> Unit
) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val off = PrayerAdjustments.hijriOffsetDays(context)
    val hDay = remember(day) {
        val c = if (off != 0) (day.clone() as Calendar).apply { add(Calendar.DATE, off) } else day
        HijriDate.INSTANCE.hijriYmd(c as Calendar).component3()
    }
    val bg = when {
        isSelected -> cs.primary.copy(alpha = 0.28f)
        inMonth -> cs.surfaceVariant.copy(alpha = 0.45f)
        else -> Color.Transparent
    }
    val numColor = when {
        !inMonth -> cs.onSurfaceVariant.copy(alpha = 0.45f)
        isFri -> Color(0xFF3B82F6)
        isSat -> Color(0xFFEF4444)
        else -> cs.onSurface
    }
    Box(
        modifier = modifier
            .padding(horizontal = 1.5.dp)
            .clip(RoundedCornerShape(10.dp))
            .background(bg)
            .then(
                if (isToday) Modifier.border(2.dp, cs.primary, RoundedCornerShape(10.dp)) else Modifier
            )
            .clickable(onClick = onClick)
            .padding(vertical = 5.dp),
        contentAlignment = Alignment.Center
    ) {
        Column(horizontalAlignment = Alignment.CenterHorizontally) {
            Text(
                day.get(Calendar.DAY_OF_MONTH).toString(),
                color = numColor, fontSize = 15.sp,
                fontWeight = if (isToday || isSelected) FontWeight.Bold else FontWeight.Medium
            )
            Text(
                hDay.toString(),
                color = numColor.copy(alpha = 0.7f), fontSize = 10.sp
            )
            Spacer(Modifier.height(2.dp))
            Row(horizontalArrangement = Arrangement.Center, modifier = Modifier.height(4.dp)) {
                events.take(3).forEach { ev ->
                    Box(
                        Modifier
                            .padding(horizontal = 1.5.dp)
                            .size(width = 12.dp, height = 3.dp)
                            .clip(RoundedCornerShape(2.dp))
                            .background(eventColor(ev.category))
                    )
                }
            }
        }
    }
}

private fun eventColor(c: HijriEvents.Category): Color = when (c) {
    HijriEvents.Category.EID -> Color(0xFF22C55E)
    HijriEvents.Category.FASTING -> Color(0xFF3B82F6)
    HijriEvents.Category.RAMADAN -> Color(0xFFF0A835)
    HijriEvents.Category.RELIGIOUS -> Color(0xFFA855F7)
}

private fun sameDay(a: Calendar, b: Calendar): Boolean =
    a.get(Calendar.YEAR) == b.get(Calendar.YEAR) && a.get(Calendar.DAY_OF_YEAR) == b.get(Calendar.DAY_OF_YEAR)

@Composable
private fun EventRow(o: HijriEvents.Occasion, day: Calendar, locale: Locale, tick: Int, onToggle: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val on = HijriEvents.isReminderOn(context, o.id)
    val gDate = SimpleDateFormat("d MMM", locale).format(day.time)
    val off = PrayerAdjustments.hijriOffsetDays(context)
    val hcal = if (off != 0) (day.clone() as Calendar).apply { add(Calendar.DATE, off) } else day
    val hDate = HijriDate.INSTANCE.format(hcal as Calendar, L10n.lang.code)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp, vertical = 4.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(cs.surfaceVariant.copy(alpha = 0.45f))
            .padding(horizontal = 14.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier
                .size(width = 4.dp, height = 34.dp)
                .clip(RoundedCornerShape(2.dp))
                .background(eventColor(o.category))
        )
        Spacer(Modifier.size(10.dp))
        Column(Modifier.weight(1f)) {
            Text(HijriEvents.name(o, L10n.lang.code), color = cs.onSurface, fontSize = 14.sp, fontWeight = FontWeight.Medium)
            Text("$hDate · $gDate", color = cs.onSurfaceVariant, fontSize = 11.sp)
        }
        IconButton(onClick = com.example.utils.AppHaptics.tap {
            HijriEvents.setReminder(context, o.id, !on)
            onToggle()
        }) {
            Icon(
                if (on) Icons.Outlined.NotificationsActive else Icons.Outlined.Notifications,
                contentDescription = if (on) L10n.t(S.CalReminderOn) else L10n.t(S.CalReminderOff),
                tint = if (on) cs.primary else cs.onSurfaceVariant,
                modifier = Modifier.size(20.dp)
            )
        }
    }
}

@Composable
private fun SelectedDayEvents(selected: Calendar, locale: Locale, tick: Int, onToggle: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val events = remember(selected, tick) { HijriEvents.forDay(context, selected) }
    val weekday = SimpleDateFormat("EEEE", locale).format(selected.time)
    Column(Modifier.fillMaxWidth().padding(top = 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                selected.get(Calendar.DAY_OF_MONTH).toString(),
                color = cs.onSurface, fontSize = 40.sp, fontWeight = FontWeight.Bold
            )
            Spacer(Modifier.size(10.dp))
            Text(weekday, color = cs.onSurfaceVariant, fontSize = 16.sp)
        }
        Spacer(Modifier.height(6.dp))
        if (events.isEmpty()) {
            Text(
                L10n.t(S.CalNoEvents),
                color = cs.onSurfaceVariant, fontSize = 13.sp,
                modifier = Modifier.fillMaxWidth().padding(horizontal = 20.dp, vertical = 8.dp),
                textAlign = TextAlign.Center
            )
        } else {
            events.forEach { EventRow(it, selected, locale, tick, onToggle) }
        }
    }
}

@Composable
private fun MonthEventsList(viewMonth: Calendar, locale: Locale, tick: Int, onToggle: () -> Unit) {
    val context = LocalContext.current
    val cs = MaterialTheme.colorScheme
    val items = remember(viewMonth, tick) {
        val out = mutableListOf<Pair<HijriEvents.Occasion, Calendar>>()
        val d = (viewMonth.clone() as Calendar)
        val max = d.getActualMaximum(Calendar.DAY_OF_MONTH)
        for (i in 1..max) {
            d.set(Calendar.DAY_OF_MONTH, i)
            for (o in HijriEvents.forDay(context, d)) out.add(o to (d.clone() as Calendar))
        }
        out
    }
    if (items.isEmpty()) return
    Column(Modifier.fillMaxWidth().padding(top = 14.dp)) {
        Text(
            L10n.t(S.CalMonthEvents),
            color = cs.onSurface, fontSize = 16.sp, fontWeight = FontWeight.Bold,
            modifier = Modifier.padding(horizontal = 20.dp)
        )
        Spacer(Modifier.height(4.dp))
        items.forEach { (o, day) -> EventRow(o, day, locale, tick, onToggle) }
    }
}

private fun LazyListScope.eventsViewItems(tick: Int) {
    item(key = "eventslist") { EventsList(tick) }
}

@Composable
private fun EventsList(tick: Int) {
    val context = LocalContext.current
    val locale = if (L10n.lang.code == "ar") Locale("ar", "EG") else Locale.forLanguageTag(L10n.lang.code)
    var tickLocal by remember { mutableIntStateOf(0) }
    val upcoming = remember(tick, tickLocal) {
        val out = mutableListOf<Pair<HijriEvents.Occasion, Calendar>>()
        val base = Calendar.getInstance()
        for (i in 0..400) {
            val d = (base.clone() as Calendar).apply { add(Calendar.DATE, i) }
            for (o in HijriEvents.forDay(context, d)) {
                if (o.id.startsWith("white_")) continue
                out.add(o to d)
            }
            if (out.size >= 40) break
        }
        out
    }
    Column(Modifier.fillMaxWidth()) {
        upcoming.forEach { (o, day) -> EventRow(o, day, locale, tick + tickLocal) { tickLocal++ } }
    }
}
