package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.filled.NotificationsActive
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.automirrored.outlined.KeyboardArrowRight
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material.icons.outlined.Star
import androidx.compose.material.icons.outlined.TouchApp
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Dhikr
import com.example.ThemeSettings
import com.example.ui.oneui.oneUIBorderColor
import com.example.ui.oneui.oneUIFrosted
import com.example.ui.oneui.FrostSpec
import com.example.ui.oneui.OneUIButton
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIEmptyState
import com.example.ui.oneui.OneUIIconBadge
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.SectionLabel
import com.example.ui.theme.OneUIUpcomingAmber
import com.example.ui.viewmodel.DhikrStats
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.formatTimeStr12h
import java.util.Calendar

fun computeInstances(
    enabledDhikr: List<Dhikr>,
    now: Calendar = Calendar.getInstance()
): Pair<DhikrInstance?, DhikrInstance?> {
    val currentHour = now.get(Calendar.HOUR_OF_DAY)
    val currentMinute = now.get(Calendar.MINUTE)
    var next: DhikrInstance? = null
    var current: DhikrInstance? = null
    if (enabledDhikr.isNotEmpty()) {
        val instances = mutableListOf<DhikrInstance>()
        for (dhikr in enabledDhikr) {
            for (timeStr in dhikr.reminderTimes) {
                val parts = timeStr.split(":")
                if (parts.size == 2) {
                    val h = parts[0].toIntOrNull() ?: continue
                    val m = parts[1].toIntOrNull() ?: continue
                    instances.add(DhikrInstance(dhikr, h, m))
                }
            }
        }
        val sorted = instances.sortedWith(compareBy({ it.hour }, { it.minute }))
        for (instance in sorted) {
            if (instance.hour > currentHour || (instance.hour == currentHour && instance.minute > currentMinute)) {
                next = instance
                break
            }
            current = instance
        }
        if (next == null && sorted.isNotEmpty()) next = sorted.first()
        if (current == null && sorted.isNotEmpty()) current = sorted.last()
        if (next == current) next = null
    }
    return Pair(current, next)
}

@Composable
fun DashboardScreen(
    viewModel: MainViewModel,
    onNavigate: (String) -> Unit,
    onMenu: (() -> Unit)? = null
) {
    val enabledDhikr by viewModel.enabledDhikr.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val (current, next) = remember(enabledDhikr) { computeInstances(enabledDhikr) }
    HomeContent(
        current = current,
        next = next,
        activeCount = enabledDhikr.size,
        stats = stats,
        onMarkRead = { viewModel.markAsRead(it) },
        onNavigate = onNavigate,
        onMenu = onMenu
    )
}

@Composable
fun HomeContent(
    current: DhikrInstance?,
    next: DhikrInstance?,
    activeCount: Int,
    stats: DhikrStats,
    onMarkRead: (Dhikr) -> Unit,
    onNavigate: ((String) -> Unit)? = null,
    onMenu: (() -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    val subtitle = if (activeCount == 0) {
        "لا توجد تنبيهات مفعلة"
    } else {
        "$activeCount " + (if (activeCount in 3..10) "تنبيهات" else "تنبيهًا") + " مفعلة"
    }
    OneUIScreen(title = "وَذكِّر", subtitle = subtitle, onMenu = onMenu) {
        item(key = "summary") {
            OneUICard {
                Row(
                    modifier = Modifier.fillMaxWidth().padding(vertical = 18.dp),
                    horizontalArrangement = Arrangement.Start,
                    verticalAlignment = Alignment.Top
                ) {
                    SummaryCell("اليوم", stats.todayCount.toString(), Modifier.weight(1f))
                    Box(Modifier.width(0.5.dp).height(44.dp).background(cs.outline).align(Alignment.CenterVertically))
                    SummaryCell("هذا الأسبوع", stats.weekCount.toString(), Modifier.weight(1f))
                    Box(Modifier.width(0.5.dp).height(44.dp).background(cs.outline).align(Alignment.CenterVertically))
                    SummaryCell("أيام متتالية", stats.streakDays.toString(), Modifier.weight(1f))
                }
            }
        }
        if (current != null) {
            item(key = "cur-l") { SectionLabel("التذكير الحالي", accent = true) }
            item(key = "cur") {
                OneUICard { DhikrHighlightCard(current) { onMarkRead(current.dhikr) } }
            }
        }
        if (next != null) {
            item(key = "next-l") { SectionLabel("التذكير القادم") }
            item(key = "next") {
                OneUICard { DhikrHighlightCard(next) { onMarkRead(next.dhikr) } }
            }
        }
        if (current == null && next == null) {
            item {
                OneUIEmptyState("لا توجد أذكار مفعلة. فعّلها من قائمة الأذكار.", Icons.Outlined.NotificationsOff)
            }
        }
        item(key = "short-l") { SectionLabel("اختصارات") }
        item(key = "short") {
            OneUICard {
                OneUIRow(
                    title = "السبحة",
                    summary = "عداد التسبيح",
                    leading = { OneUIIconBadge(Icons.Outlined.TouchApp, Color(4283289796L)) },
                    trailing = { ChevronIcon() },
                    onClick = { onNavigate?.invoke("tasbeeh") }
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = "حصن المسلم",
                    summary = "أذكار وأدعية من الكتاب والسنة",
                    leading = { OneUIIconBadge(Icons.AutoMirrored.Outlined.MenuBook, Color(4285507839L)) },
                    trailing = { ChevronIcon() },
                    onClick = { onNavigate?.invoke("hisn") }
                )
                OneUIDivider(70.dp)
                OneUIRow(
                    title = "المفضلة",
                    summary = "الأذكار التي تحبها",
                    leading = { OneUIIconBadge(Icons.Outlined.Star, Color(4294956367L)) },
                    trailing = { ChevronIcon() },
                    onClick = { onNavigate?.invoke("favorites") }
                )
            }
        }
    }
}

@Composable
fun SummaryCell(label: String, value: String, modifier: Modifier = Modifier) {
    Column(modifier = modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.headlineSmall)
        Text(label, color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
    }
}

@Composable
fun ChevronIcon() {
    Icon(
        Icons.AutoMirrored.Outlined.KeyboardArrowRight,
        contentDescription = null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}

@Composable
fun DhikrHighlightCard(instance: DhikrInstance, onMarkAsRead: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val dhikr = instance.dhikr
    Column(modifier = Modifier.fillMaxWidth().padding(16.dp)) {
        Text(dhikr.title, color = cs.onSurface, style = MaterialTheme.typography.titleMedium)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(Icons.Filled.NotificationsActive, null, modifier = Modifier.size(16.dp), tint = OneUIUpcomingAmber)
            Spacer(Modifier.width(6.dp))
            Text(
                formatTimeStr12h(String.format("%02d:%02d", instance.hour, instance.minute)),
                color = OneUIUpcomingAmber,
                style = MaterialTheme.typography.bodyMedium
            )
        }
        Spacer(Modifier.height(12.dp))
        Column(
            modifier = Modifier.fillMaxWidth()
                .oneUIFrosted(RoundedCornerShape(16.dp), FrostSpec(cs.surfaceVariant, if (ThemeSettings.isDark()) 0.50f else 0.62f, oneUIBorderColor()), 16)
                .padding(14.dp)
        ) {
            Text(
                dhikr.content,
                color = cs.onSurface,
                maxLines = 3,
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(Modifier.height(14.dp))
            OneUIButton(
                text = "تمت القراءة",
                onClick = onMarkAsRead,
                modifier = Modifier.align(Alignment.End),
                filled = false,
                icon = Icons.Outlined.Check
            )
        }
    }
}
