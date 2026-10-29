package com.example.ui.screens

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.NotificationsOff
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Dhikr
import com.example.ui.oneui.*
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.formatTimeStr12h

@Composable
fun RemindersScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val allDhikr by viewModel.allDhikr.collectAsStateWithLifecycle()
    RemindersContent(allDhikr, onBack, viewModel::toggleEnabled)
}

@Composable
fun RemindersContent(all: List<Dhikr>, onBack: () -> Unit, onToggle: (Dhikr) -> Unit) {
    val active = all.count { it.isEnabled }
    OneUIScreen(
        title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Reminders),
        subtitle = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ActiveOfTotalFmt).format(active, all.size),
        onBack = onBack
    ) {
        if (all.isEmpty()) {
            item { OneUIEmptyState(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NoReminders), Icons.Default.NotificationsOff) }
        } else {
            item(key = "group") {
                OneUICard {
                    all.sortedBy { it.reminderTimes.firstOrNull() ?: "99:99" }.forEachIndexed { i, dhikr ->
                        if (i > 0) OneUIDivider()
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
    val first = dhikr.reminderTimes.firstOrNull()?.let { formatTimeStr12h(it) } ?: "--"
    val extra = if (dhikr.reminderTimes.size > 1) " +${dhikr.reminderTimes.size - 1}" else ""
    OneUIRow(
        title = dhikr.title,
        summary = dhikr.reminderTimes.joinToString("، ") { formatTimeStr12h(it) },
        leading = {
            Column(horizontalAlignment = Alignment.CenterHorizontally, modifier = Modifier.width(72.dp)) {
                Text(
                    first.substringBefore(' '),
                    style = MaterialTheme.typography.titleMedium,
                    color = if (dhikr.isEnabled) cs.onSurface else cs.onSurfaceVariant
                )
                Text(
                    first.substringAfter(' ', "") + extra,
                    style = MaterialTheme.typography.bodySmall,
                    color = cs.onSurfaceVariant
                )
            }
        },
        trailing = { OneUISwitch(dhikr.isEnabled, { onToggle() }) },
        onClick = onToggle
    )
}
