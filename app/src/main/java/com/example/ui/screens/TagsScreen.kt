package com.example.ui.screens
import com.example.ui.i18n.S
import com.example.ui.i18n.L10n

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.automirrored.filled.Label
import androidx.compose.material3.Icon
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
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Tag
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIDialog
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIEmptyState
import com.example.ui.oneui.OneUIIconAction
import com.example.ui.oneui.OneUIRow
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.OneUITextField
import com.example.ui.oneui.azkarCount
import com.example.ui.oneui.parseColorSafe
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TagsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val dhikrsWithTags by viewModel.dhikrsWithTags.collectAsStateWithLifecycle()
    val counts = remember(dhikrsWithTags) {
        val map = LinkedHashMap<Long, Int>()
        dhikrsWithTags.forEach { dwt -> dwt.tags.forEach { map[it.tagId] = (map[it.tagId] ?: 0) + 1 } }
        map
    }
    var showAdd by remember { mutableStateOf(false) }
    var toEdit by remember { mutableStateOf<Tag?>(null) }
    var toDelete by remember { mutableStateOf<Tag?>(null) }

    TagsContent(
        tags = allTags,
        counts = counts,
        onBack = onBack,
        onAdd = { showAdd = true },
        onEdit = { toEdit = it },
        onDelete = { toDelete = it }
    )

    if (showAdd) {
        TagAddEditDialog(
            tag = null,
            onDismiss = { showAdd = false },
            onConfirm = { name, color ->
                viewModel.addTag(name, color)
                showAdd = false
            }
        )
    }
    toEdit?.let { tag ->
        TagAddEditDialog(
            tag = tag,
            onDismiss = { toEdit = null },
            onConfirm = { name, color ->
                viewModel.updateTag(tag.copy(name = name, colorHex = color))
                toEdit = null
            }
        )
    }
    toDelete?.let { tag ->
        OneUIDialog(
            title = L10n.t(S.DeleteTag),
            onDismiss = { toDelete = null },
            onConfirm = {
                viewModel.deleteTag(tag)
                toDelete = null
            },
            confirmText = L10n.t(S.Delete)
        ) {
            Text(
                "سيتم حذف التصنيف \"${tag.name}\" من جميع الأذكار. لن تُحذف الأذكار نفسها.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyLarge
            )
        }
    }
}

@Composable
fun TagsContent(
    tags: List<Tag>,
    counts: Map<Long, Int>,
    onBack: () -> Unit,
    onAdd: () -> Unit,
    onEdit: (Tag) -> Unit,
    onDelete: (Tag) -> Unit
) {
    val size = tags.size
    OneUIScreen(
        title = L10n.t(S.Tags),
        subtitle = "$size " + (if (size in 3..10) "تصنيفات" else "تصنيف"),
        onBack = onBack,
        actions = { OneUIIconAction(Icons.Outlined.Add, L10n.t(S.AddTag), onAdd) }
    ) {
        if (tags.isEmpty()) {
            item { OneUIEmptyState("لا توجد تصنيفات", Icons.AutoMirrored.Filled.Label) }
        } else {
            item(key = "group") {
                OneUICard {
                    tags.forEachIndexed { index, tag ->
                        if (index > 0) OneUIDivider(70.dp)
                        TagRow(tag, counts[tag.tagId] ?: 0, { onEdit(tag) }, { onDelete(tag) })
                    }
                }
            }
        }
    }
}

@Composable
fun TagRow(tag: Tag, count: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    val color = parseColorSafe(tag.colorHex)
    OneUIRow(
        title = tag.name,
        summary = azkarCount(count),
        leading = {
            Box(
                modifier = Modifier.size(40.dp).clip(CircleShape).background(color.copy(alpha = 0.18f)),
                contentAlignment = Alignment.Center
            ) {
                Box(modifier = Modifier.size(16.dp).clip(CircleShape).background(color))
            }
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    Icons.Outlined.Edit, L10n.t(S.Edit),
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onEdit).padding(9.dp),
                    tint = cs.onSurfaceVariant
                )
                Icon(
                    Icons.Outlined.Delete, L10n.t(S.Delete),
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onDelete).padding(9.dp),
                    tint = cs.onSurfaceVariant
                )
            }
        }
    )
}

@Composable
fun TagAddEditDialog(tag: Tag?, onDismiss: () -> Unit, onConfirm: (String, String) -> Unit) {
    var name by remember { mutableStateOf(tag?.name ?: "") }
    var color by remember { mutableStateOf(tag?.colorHex ?: "#008080") }
    val colors = listOf(
        "#008080", "#800080", "#1E3A8A", "#059669", "#DC2626", "#D97706",
        "#4F46E5", "#2563EB", "#0284C7", "#0D9488", "#16A34A", "#CA8A04"
    )
    OneUIDialog(
        title = if (tag == null) L10n.t(S.NewTag) else L10n.t(S.EditTag),
        onDismiss = onDismiss,
        onConfirm = { if (name.isNotBlank()) onConfirm(name.trim(), color) }
    ) {
        OneUITextField(name, { name = it }, L10n.t(S.TagName))
        Spacer(modifier = Modifier.height(16.dp))
        Text(L10n.t(S.Color), color = MaterialTheme.colorScheme.onSurfaceVariant, style = MaterialTheme.typography.titleSmall)
        Spacer(modifier = Modifier.height(10.dp))
        colors.chunked(6).forEach { rowColors ->
            Row(modifier = Modifier.fillMaxWidth().padding(bottom = 10.dp), horizontalArrangement = Arrangement.SpaceBetween) {
                rowColors.forEach { hex ->
                    val selected = color.equals(hex, ignoreCase = true)
                    Box(
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape)
                            .background(parseColorSafe(hex))
                            .then(
                                if (selected) Modifier.border(BorderStroke(3.dp, MaterialTheme.colorScheme.onSurface), CircleShape)
                                else Modifier
                            )
                            .clickable { color = hex }
                    )
                }
            }
        }
    }
}
