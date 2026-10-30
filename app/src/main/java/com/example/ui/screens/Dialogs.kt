package com.example.ui.screens
import com.example.utils.plainClickable

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Notifications
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.example.data.Dhikr
import com.example.data.Tag
import com.example.ui.components.WheelTimePicker
import com.example.ui.oneui.OneUIButton
import com.example.ui.oneui.OneUIChip
import com.example.ui.oneui.OneUIDialog
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUITextField
import com.example.ui.oneui.parseColorSafe
import com.example.utils.DateTimeUtilsKt
import java.util.Locale

@Composable
fun DhikrAddDialog(
    allTags: List<Tag> = emptyList(),
    onDismiss: () -> Unit,
    onConfirm: (String, String, List<String>, List<Long>) -> Unit,
    onCreateTag: ((String) -> Unit)? = null
) {
    var title by remember { mutableStateOf("") }
    var content by remember { mutableStateOf("") }
    var times by remember { mutableStateOf(listOf("09:00")) }
    val selected = remember { mutableStateListOf<Long>() }
    val valid = title.isNotBlank() && content.isNotBlank() && times.isNotEmpty()
    OneUIDialog(
        title = "ذكر جديد",
        onDismiss = onDismiss,
        onConfirm = {
            if (valid) onConfirm(title.trim(), content.trim(), times, selected.toList())
        },
        confirmEnabled = valid
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
    onConfirm: (String, String, List<Long>) -> Unit,
    onCreateTag: ((String) -> Unit)? = null
) {
    var title by remember { mutableStateOf(dhikr.title) }
    var content by remember { mutableStateOf(dhikr.content) }
    val selected = remember { mutableStateListOf<Long>().also { it.addAll(initialTagIds) } }
    val valid = title.isNotBlank() && content.isNotBlank()
    OneUIDialog(
        title = "تعديل الذكر",
        onDismiss = onDismiss,
        onConfirm = {
            if (valid) onConfirm(title.trim(), content.trim(), selected.toList())
        },
        confirmEnabled = valid
    ) {
        DhikrAddForm(title, { title = it }, content, { content = it }, allTags, selected, onCreateTag)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun ColumnScope.DhikrAddForm(
    title: String,
    onTitle: (String) -> Unit,
    content: String,
    onContent: (String) -> Unit,
    allTags: List<Tag>,
    selected: MutableList<Long>,
    onCreateTag: ((String) -> Unit)?
) {
    val cs = MaterialTheme.colorScheme
    var showNew by remember { mutableStateOf(false) }
    var newName by remember { mutableStateOf("") }
    OneUITextField(title, onTitle, "العنوان")
    Spacer(modifier = Modifier.height(10.dp))
    OneUITextField(content, onContent, "نص الذكر أو الدعاء", singleLine = false)
    Spacer(modifier = Modifier.height(18.dp))
    Row(verticalAlignment = Alignment.CenterVertically) {
        Text(
            "التصنيفات",
            modifier = Modifier.weight(1f),
            color = cs.onSurfaceVariant,
            style = MaterialTheme.typography.titleSmall
        )
        if (onCreateTag != null) {
            Text(
                if (showNew) "إلغاء" else "تصنيف جديد",
                modifier = Modifier
                    .clip(MaterialTheme.shapes.large)
                    .clickable { showNew = !showNew }
                    .padding(8.dp),
                color = cs.primary,
                style = MaterialTheme.typography.labelLarge
            )
        }
    }
    if (showNew && onCreateTag != null) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            OneUITextField(newName, { newName = it }, "اسم التصنيف", modifier = Modifier.weight(1f))
            Spacer(modifier = Modifier.width(8.dp))
            OneUIButton("إضافة", {
                if (newName.isNotBlank()) {
                    onCreateTag(newName.trim())
                    newName = ""
                    showNew = false
                }
            })
        }
        Spacer(modifier = Modifier.height(8.dp))
    }
    SimpleFlowRow(spacing = 8.dp) {
        allTags.forEach { tag ->
            val on = selected.contains(tag.tagId)
            OneUIChip(
                label = tag.name,
                selected = on,
                onClick = { if (on) selected.remove(tag.tagId) else selected.add(tag.tagId) },
                dot = parseColorSafe(tag.colorHex)
            )
        }
    }
}

@Composable
fun ColumnScope.TimesEditor(times: List<String>, onChange: (List<String>) -> Unit) {
    val cs = MaterialTheme.colorScheme
    Spacer(modifier = Modifier.height(18.dp))
    Text("أوقات التنبيه", color = cs.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
    Spacer(modifier = Modifier.height(6.dp))
    times.forEachIndexed { i, t ->
        if (i > 0) OneUIDivider(56.dp)
        TimeRow(
            timeStr = t,
            onTimeChange = { n -> onChange(times.toMutableList().also { it[i] = n }) },
            onDelete = { onChange(times.toMutableList().also { it.removeAt(i) }) }
        )
    }
    if (times.isNotEmpty()) OneUIDivider(0.dp)
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onChange(times + "12:00") }
            .padding(horizontal = 16.dp, vertical = 14.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Add, contentDescription = null, tint = cs.primary, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text("إضافة وقت تنبيه", color = cs.primary, style = MaterialTheme.typography.bodyLarge)
    }
}

@Composable
fun DhikrMultipleTimesDialog(dhikr: Dhikr, onDismiss: () -> Unit, onConfirm: (List<String>) -> Unit) {
    var times by remember { mutableStateOf(dhikr.reminderTimes) }
    OneUIDialog(
        title = "تكرار التنبيه",
        onDismiss = onDismiss,
        onConfirm = { if (times.isNotEmpty()) onConfirm(times) },
        confirmEnabled = times.isNotEmpty()
    ) {
        TimesEditor(times) { times = it }
    }
}

@Composable
fun TimeRow(timeStr: String, onTimeChange: (String) -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var showPicker by remember { mutableStateOf(false) }
    val parts = timeStr.split(":")
    val hour = parts.getOrNull(0)?.toIntOrNull() ?: 12
    val minute = parts.getOrNull(1)?.toIntOrNull() ?: 0
    var h by remember(timeStr) { mutableStateOf(hour) }
    var m by remember(timeStr) { mutableStateOf(minute) }
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .plainClickable { showPicker = true }
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Icon(Icons.Outlined.Notifications, contentDescription = null, tint = cs.onSurfaceVariant, modifier = Modifier.size(22.dp))
        Spacer(modifier = Modifier.width(12.dp))
        Text(
            DateTimeUtilsKt.formatTimeStr12h(timeStr),
            modifier = Modifier.weight(1f),
            color = cs.onSurface,
            style = MaterialTheme.typography.bodyLarge
        )
        Icon(
            Icons.Outlined.Delete, "حذف",
            modifier = Modifier
                .size(36.dp)
                .clip(MaterialTheme.shapes.large)
                .clickable(onClick = onDelete)
                .padding(7.dp),
            tint = cs.onSurfaceVariant
        )
    }
    if (showPicker) {
        OneUIDialog(
            title = "وقت التنبيه",
            onDismiss = { showPicker = false },
            onConfirm = {
                onTimeChange(String.format(Locale.US, "%02d:%02d", h, m))
                showPicker = false
            }
        ) {
            Box(modifier = Modifier.fillMaxWidth(), contentAlignment = Alignment.Center) {
                WheelTimePicker(hour, minute, { a, b ->
                    h = a
                    m = b
                })
            }
        }
    }
}

@Composable
fun DhikrDetailDialog(dhikr: Dhikr, onDismiss: () -> Unit, onMarkAsRead: (() -> Unit)? = null) {
    OneUIDialog(
        title = dhikr.title,
        onDismiss = onDismiss,
        onConfirm = onMarkAsRead?.let { cb ->
            {
                cb()
                onDismiss()
            }
        },
        confirmText = "تمت القراءة",
        dismissText = "إغلاق"
    ) {
        Text(dhikr.content, color = MaterialTheme.colorScheme.onSurface, style = MaterialTheme.typography.bodyLarge)
    }
}

@OptIn(ExperimentalLayoutApi::class)
@Composable
fun SimpleFlowRow(spacing: Dp, modifier: Modifier = Modifier, content: @Composable () -> Unit) {
    FlowRow(
        modifier = modifier,
        horizontalArrangement = Arrangement.spacedBy(spacing),
        verticalArrangement = Arrangement.spacedBy(spacing)
    ) { content() }
}
