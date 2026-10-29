package com.example.ui.screens

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.itemsIndexed
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Notifications
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.example.data.Dhikr
import com.example.data.Tag
import com.example.ui.oneui.*
import com.example.utils.formatTimeStr12h

@Composable
fun DhikrAddDialog(
    allTags: List<Tag> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, times: List<String>, tagIds: List<Long>) -> Unit,
    onCreateTag: ((String) -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var times by remember { mutableStateOf(listOf("09:00")) }
    val selected = remember { mutableStateListOf<Long>() }
    OneUIDialog(
        title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NewDhikr), onDismiss = onDismiss,
        onConfirm = { if (title.isNotBlank() && content.isNotBlank() && times.isNotEmpty()) onConfirm(title.trim(), content.trim(), times, selected.toList()) },
        confirmEnabled = title.isNotBlank() && content.isNotBlank() && times.isNotEmpty()
    ) {
        DhikrAddForm(title, { title = it }, content, { content = it }, allTags, selected, onCreateTag)
        TimesEditor(times) { times = it }
    }
}

@Composable
fun DhikrEditDialog(
    dhikr: Dhikr,
    allTags: List<Tag> = emptyList(),
    initialTagIds: List<Long> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (title: String, content: String, tagIds: List<Long>) -> Unit,
    onCreateTag: ((String) -> Unit)? = null
) {
    var title by remember { mutableStateOf(dhikr.title) }
    var content by remember { mutableStateOf(dhikr.content) }
    val selected = remember { mutableStateListOf<Long>().apply { addAll(initialTagIds) } }
    OneUIDialog(
        title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.EditDhikr), onDismiss = onDismiss,
        onConfirm = { if (title.isNotBlank() && content.isNotBlank()) onConfirm(title.trim(), content.trim(), selected.toList()) },
        confirmEnabled = title.isNotBlank() && content.isNotBlank()
    ) {
        DhikrAddForm(title, { title = it }, content, { content = it }, allTags, selected, onCreateTag)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.DhikrAddForm(
    title: String, onTitle: (String) -> Unit,
    content: String, onContent: (String) -> Unit,
    allTags: List<Tag>, selected: MutableList<Long>,
    onCreateTag: ((String) -> Unit)?
) {
    val cs = MaterialTheme.colorScheme
    var showNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    OneUITextField(title, onTitle, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TitleLabel))
    Spacer(Modifier.height(9.dp))
    OneUITextField(content, onContent, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.DhikrText), singleLine = false)
    Spacer(Modifier.height(18.dp))
    Row(Modifier.fillMaxWidth(), verticalAlignment = Alignment.CenterVertically) {
        Text(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Tags), style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant, modifier = Modifier.weight(1f))
        if (onCreateTag != null) {
            Text(
                if (showNew) com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Cancel) else com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NewTag), style = MaterialTheme.typography.labelLarge, color = cs.primary,
                modifier = Modifier.clip(MaterialTheme.shapes.small).clickable { showNew = !showNew }.padding(8.dp)
            )
        }
    }
    if (showNew && onCreateTag != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OneUITextField(newName, { newName = it }, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TagName), modifier = Modifier.weight(1f))
            Spacer(Modifier.width(8.dp))
            OneUIButton(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Add), { if (newName.isNotBlank()) { onCreateTag(newName.trim()); newName = ""; showNew = false } }, filled = true)
        }
        Spacer(Modifier.height(8.dp))
    }
    SimpleFlowRow(spacing = 8.dp) {
        allTags.forEach { tag ->
            val on = selected.contains(tag.tagId)
            OneUIChip(tag.name, on, { if (on) selected.remove(tag.tagId) else selected.add(tag.tagId) }, dot = parseColorSafe(tag.colorHex))
        }
    }
}

@Composable
fun ColumnScope.TimesEditor(times: List<String>, onChange: (List<String>) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Spacer(Modifier.height(18.dp))
    Text(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ReminderTimes), style = MaterialTheme.typography.titleSmall, color = cs.onSurfaceVariant)
    Spacer(Modifier.height(6.dp))
    Column(Modifier.fillMaxWidth()) {
        times.forEachIndexed { i, t ->
            if (i > 0) OneUIDivider(inset = 56.dp)
            TimeRow(t, { n -> onChange(times.toMutableList().also { it[i] = n }) }, { onChange(times.toMutableList().also { it.removeAt(i) }) })
        }
        if (times.isNotEmpty()) OneUIDivider(inset = 0.dp)
        Row(
            Modifier.fillMaxWidth().clickable { onChange(times + "12:00") }.padding(horizontal = 16.dp, vertical = 14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(Icons.Default.Add, null, tint = cs.primary, modifier = Modifier.size(22.dp))
            Spacer(Modifier.width(12.dp))
            Text(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.AddReminderTime), style = MaterialTheme.typography.bodyLarge, color = cs.primary)
        }
    }
}

@Composable
fun DhikrMultipleTimesDialog(dhikr: Dhikr, onDismiss: () -> Unit, onConfirm: (List<String>) -> Unit) {
    var times by remember { mutableStateOf(dhikr.reminderTimes) }
    OneUIDialog(
        title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.RepeatReminder), onDismiss = onDismiss,
        onConfirm = { if (times.isNotEmpty()) onConfirm(times) }, confirmEnabled = times.isNotEmpty()
    ) { TimesEditor(times) { times = it } }
}

@Composable
fun TimeRow(timeStr: String, onTimeChange: (String) -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var showPicker by remember { mutableStateOf(false) }
    val parts = timeStr.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    Row(
        Modifier.fillMaxWidth().clickable { showPicker = true }.padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Notifications, null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(Modifier.width(12.dp))
        Text(formatTimeStr12h(timeStr), style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, modifier = Modifier.weight(1f))
        Icon(
            Icons.Outlined.Delete, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Delete), tint = cs.onSurfaceVariant,
            modifier = Modifier.size(36.dp).clip(MaterialTheme.shapes.large).clickable(onClick = onDelete).padding(7.dp)
        )
    }
    if (showPicker) {
        var h by remember { mutableStateOf(hour) }
        var m by remember { mutableStateOf(minute) }
        OneUIDialog(
            title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ReminderTime), onDismiss = { showPicker = false },
            onConfirm = { onTimeChange(String.format(java.util.Locale.US, "%02d:%02d", h, m)); showPicker = false }
        ) {
            Box(Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                com.example.ui.components.WheelTimePicker(initialHour24 = hour, initialMinute = minute, onTimeSelected = { a, b -> h = a; m = b })
            }
        }
    }
}

@Composable
fun DhikrDetailDialog(dhikr: Dhikr, onDismiss: () -> Unit, onMarkAsRead: (() -> Unit)? = null) {
    val cs = MaterialTheme.colorScheme
    OneUIDialog(
        title = dhikr.title, onDismiss = onDismiss,
        onConfirm = onMarkAsRead?.let { { it(); onDismiss() } },
        confirmText = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ReadDone), dismissText = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Close)
    ) {
        OneUICard(color = cs.surface, padding = PaddingValues(16.dp)) {
            Text(dhikr.content, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        }
    }
}


/**
 * Minimal wrapping row. Replaces androidx.compose.foundation.layout.FlowRow, whose
 * experimental API signature differs between the compile (1.7.x) and runtime (1.9.x)
 * compose versions on this project's classpath.
 */
@Composable
fun SimpleFlowRow(spacing: androidx.compose.ui.unit.Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    androidx.compose.ui.layout.Layout(content = content, modifier = modifier) { measurables, constraints ->
        val h = spacing.roundToPx()
        val v = spacing.roundToPx()
        val placeables = measurables.map { it.measure(constraints.copy(minWidth = 0, minHeight = 0)) }
        val rows = mutableListOf<MutableList<androidx.compose.ui.layout.Placeable>>()
        var cur = mutableListOf<androidx.compose.ui.layout.Placeable>()
        var w = 0
        for (pl in placeables) {
            if (cur.isNotEmpty() && w + h + pl.width > constraints.maxWidth) {
                rows.add(cur); cur = mutableListOf(); w = 0
            }
            if (cur.isNotEmpty()) w += h
            cur.add(pl); w += pl.width
        }
        if (cur.isNotEmpty()) rows.add(cur)
        val rowHeights = rows.map { r -> r.maxOf { it.height } }
        val totalH = (rowHeights.sum() + v * (rows.size - 1).coerceAtLeast(0)).coerceIn(constraints.minHeight, constraints.maxHeight)
        layout(constraints.maxWidth, totalH) {
            var y = 0
            rows.forEachIndexed { i, r ->
                var x = 0
                r.forEach { pl -> pl.placeRelative(x, y); x += pl.width + h }
                y += rowHeights[i] + v
            }
        }
    }
}
