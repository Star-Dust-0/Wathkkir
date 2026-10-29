package com.example.ui.screens

import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material.icons.automirrored.filled.KeyboardArrowRight
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.*
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Dhikr
import com.example.ui.oneui.*
import com.example.ui.viewmodel.DhikrStats
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.formatTimeStr12h
import java.util.Calendar

data class DhikrInstance(val dhikr: Dhikr, val hour: Int, val minute: Int)

fun computeInstances(enabledDhikr: List<Dhikr>, now: Calendar = Calendar.getInstance()): Pair<DhikrInstance?, DhikrInstance?> {
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
fun DashboardScreen(viewModel: MainViewModel, onNavigate: (String) -> Unit, onMenu: (() -> Unit)? = null) {
    val enabledDhikr by viewModel.enabledDhikr.collectAsStateWithLifecycle()
    val stats by viewModel.stats.collectAsStateWithLifecycle()
    val (current, next) = remember(enabledDhikr) { computeInstances(enabledDhikr) }
    HomeContent(
        current = current,
        next = next,
        activeCount = enabledDhikr.size,
        stats = stats,
        onMarkRead = { viewModel.markAsRead(it.id) },
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
    onNavigate: (String) -> Unit,
    onMenu: (() -> Unit)? = null
) {
    OneUIScreen(
        title = "وَذكِّر",
        onMenu = onMenu,
        subtitle = if (activeCount == 0) com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NoActiveReminders) else "$activeCount ${if (activeCount in 3..10) "تنبيهات" else "تنبيهًا"} مفعلة"
    ) {
        item(key = "summary") {
            OneUICard {
                Row(Modifier.fillMaxWidth().padding(vertical = 18.dp)) {
                    SummaryCell(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Today), stats.todayCount.toString(), Modifier.weight(1f))
                    Box(Modifier.width(0.5.dp).height(44.dp).background(MaterialTheme.colorScheme.outline).align(Alignment.CenterVertically))
                    SummaryCell(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ThisWeek), stats.weekCount.toString(), Modifier.weight(1f))
                    Box(Modifier.width(0.5.dp).height(44.dp).background(MaterialTheme.colorScheme.outline).align(Alignment.CenterVertically))
                    SummaryCell(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.DayStreak), stats.streakDays.toString(), Modifier.weight(1f))
                }
            }
        }
        if (current != null) {
            item(key = "cur-l") { SectionLabel(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.CurrentReminder), accent = true) }
            item(key = "cur") { DhikrHighlightCard(current) { onMarkRead(current.dhikr) } }
        }
        if (next != null) {
            item(key = "next-l") { SectionLabel(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NextReminder)) }
            item(key = "next") { DhikrHighlightCard(next) { onMarkRead(next.dhikr) } }
        }
        if (current == null && next == null) {
            item { OneUIEmptyState("لا توجد أذكار مفعلة. فعّلها من قائمة الأذكار.", Icons.Default.NotificationsOff) }
        }
        item(key = "short-l") { SectionLabel(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Shortcuts)) }
        item(key = "short") {
            OneUICard {
                OneUIRow(
                    com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TasbeehTab), summary = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TasbeehCounter), onClick = { onNavigate("tasbeeh") },
                    leading = { OneUIIconBadge(Icons.Outlined.TouchApp, tint = androidx.compose.ui.graphics.Color(0xFF4DD0C4)) },
                    trailing = { ChevronIcon() }
                )
                OneUIDivider(inset = 70.dp)
                OneUIRow(
                    com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.HisnTab), summary = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.HisnSubtitle), onClick = { onNavigate("hisn") },
                    leading = { OneUIIconBadge(Icons.AutoMirrored.Outlined.MenuBook, tint = androidx.compose.ui.graphics.Color(0xFF6FA8FF)) },
                    trailing = { ChevronIcon() }
                )
                OneUIDivider(inset = 70.dp)
                OneUIRow(
                    com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Favorites), summary = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.FavSubtitle), onClick = { onNavigate("favorites") },
                    leading = { OneUIIconBadge(Icons.Outlined.Star, tint = androidx.compose.ui.graphics.Color(0xFFFFD54F)) },
                    trailing = { ChevronIcon() }
                )
            }
        }
    }
}

@Composable
private fun SummaryCell(label: String, value: String, modifier: Modifier) {
    Column(modifier, horizontalAlignment = Alignment.CenterHorizontally) {
        Text(value, style = MaterialTheme.typography.headlineMedium, color = MaterialTheme.colorScheme.onSurface)
        Text(label, style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
    }
}

@Composable
fun ChevronIcon() {
    // In RTL the "forward" chevron points left, matching Samsung's mirrored lists.
    Icon(
        Icons.AutoMirrored.Filled.KeyboardArrowRight,
        null,
        tint = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
    )
}

@Composable
fun DhikrHighlightCard(instance: DhikrInstance, onMarkAsRead: () -> Unit) {
    val dhikr = instance.dhikr
    val cs = MaterialTheme.colorScheme
    OneUICard(padding = PaddingValues(18.dp)) {
        Text(dhikr.title, style = MaterialTheme.typography.titleMedium, color = cs.onSurface)
        Spacer(Modifier.height(6.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            Icon(
                Icons.Filled.NotificationsActive, null,
                tint = cs.primary, modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                formatTimeStr12h(String.format("%02d:%02d", instance.hour, instance.minute)),
                style = MaterialTheme.typography.bodyMedium, color = cs.primary
            )
        }
        Spacer(Modifier.height(12.dp))
        Box(
            Modifier.fillMaxWidth()
                .background(cs.surfaceVariant, androidx.compose.foundation.shape.RoundedCornerShape(16.dp))
                .padding(14.dp)
        ) {
            Text(dhikr.content, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, maxLines = 3)
        }
        Spacer(Modifier.height(14.dp))
        OneUIButton(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ReadDone), onMarkAsRead, Modifier.align(Alignment.End), icon = Icons.Default.Check)
    }
}
