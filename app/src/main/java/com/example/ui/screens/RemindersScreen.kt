package com.example.ui.screens

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.NotificationsOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Dhikr
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIEmptyState
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.OneUISwitch
import com.example.utils.DateTimeUtilsKt

@Composable
fun RemindersScreen(viewModel: com.example.ui.viewmodel.MainViewModel, onBack: () -> Unit) {
    val all by viewModel.allDhikr.collectAsStateWithLifecycle()
    RemindersContent(all = all, onBack = onBack, onToggle = viewModel::toggleEnabled)
}

@Composable
fun RemindersContent(all: List<Dhikr>, onBack: () -> Unit, onToggle: (Dhikr) -> Unit) {
    val active = all.count { it.isEnabled }
    OneUIScreen(title = "التنبيهات", subtitle = "$active من ${all.size} مفعّلة", onBack = onBack) {
        if (all.isEmpty()) {
            item { OneUIEmptyState("لا توجد تنبيهات", Icons.Outlined.NotificationsOff) }
        } else {
            val sorted = all.sortedBy { it.reminderTimes.firstOrNull() ?: "" }
            item(key = "group") {
                OneUICard {
                    sorted.forEachIndexed { index, dhikr ->
                        if (index > 0) OneUIDivider()
                        ReminderRow(dhikr) { onToggle(dhikr) }
                    }
                }
            }
        }
    }
}

@Composable
fun ReminderRow(dhikr: Dhikr, onToggle: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val first = dhikr.reminderTimes.firstOrNull()?.let { DateTimeUtilsKt.formatTimeStr12h(it) } ?: "--"
    val extra = if (dhikr.reminderTimes.size > 1) " +${dhikr.reminderTimes.size - 1}" else ""
    OneUIRow(
        title = dhikr.title,
        summary = dhikr.reminderTimes.joinToString(" · ") { DateTimeUtilsKt.formatTimeStr12h(it) },
        leading = {
            Column(modifier = Modifier.width(72.dp), horizontalAlignment = Alignment.CenterHorizontally) {
                Text(
                    first.substringBefore(' '),
                    color = if (dhikr.isEnabled) cs.onSurface else cs.onSurfaceVariant,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    first.substringAfter(' ', "") + extra,
                    color = cs.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
        },
        trailing = { OneUISwitch(dhikr.isEnabled, { onToggle() }) }
    )
}
