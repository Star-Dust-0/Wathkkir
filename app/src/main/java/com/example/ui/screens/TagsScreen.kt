package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.Label
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Tag
import com.example.ui.oneui.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun TagsScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val dhikrsWithTags by viewModel.dhikrsWithTags.collectAsStateWithLifecycle()
    var showAdd by remember { mutableStateOf(false) }
    var toEdit by remember { mutableStateOf<Tag?>(null) }
    var toDelete by remember { mutableStateOf<Tag?>(null) }
    val counts = remember(dhikrsWithTags) {
        val m = mutableMapOf<Long, Int>()
        dhikrsWithTags.forEach { dw -> dw.tags.forEach { t -> m[t.tagId] = (m[t.tagId] ?: 0) + 1 } }
        m
    }
    TagsContent(allTags, counts, onBack, onAdd = { showAdd = true }, onEdit = { toEdit = it }, onDelete = { toDelete = it })

    if (showAdd) TagAddEditDialog(null, { showAdd = false }) { name, color -> viewModel.addTag(name, color); showAdd = false }
    toEdit?.let { tag ->
        TagAddEditDialog(tag, { toEdit = null }) { name, color ->
            viewModel.updateTag(tag.copy(name = name, colorHex = color)); toEdit = null
        }
    }
    toDelete?.let { tag ->
        OneUIDialog(
            title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.DeleteTag),
            onDismiss = { toDelete = null },
            onConfirm = { viewModel.deleteTag(tag); toDelete = null },
            confirmText = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Delete)
        ) {
            Text(
                com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.DeleteTagMsg).format(tag.name),
                style = MaterialTheme.typography.bodyLarge, color = MaterialTheme.colorScheme.onSurfaceVariant
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
    OneUIScreen(
        title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Tags),
        subtitle = com.example.ui.i18n.L10n.tagsCount(tags.size),
        onBack = onBack,
        actions = { OneUIIconAction(Icons.Default.Add, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.AddTag), onAdd) }
    ) {
        if (tags.isEmpty()) {
            item { OneUIEmptyState(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NoTags), Icons.Default.Label) }
        } else {
            item(key = "group") {
                OneUICard {
                    tags.forEachIndexed { i, tag ->
                        if (i > 0) OneUIDivider(inset = 70.dp)
                        TagRow(tag, counts[tag.tagId] ?: 0, { onEdit(tag) }, { onDelete(tag) })
                    }
                }
            }
        }
    }
}

@Composable
private fun TagRow(tag: Tag, count: Int, onEdit: () -> Unit, onDelete: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    OneUIRow(
        modifier = Modifier.padding(vertical = 1.9.dp),
        title = tag.name,
        summary = azkarCount(count),
        onClick = onEdit,
        leading = {
            Box(Modifier.size(40.dp).clip(CircleShape).background(parseColorSafe(tag.colorHex).copy(alpha = 0.18f)), contentAlignment = Alignment.Center) {
                Box(Modifier.size(16.dp).clip(CircleShape).background(parseColorSafe(tag.colorHex)))
            }
        },
        trailing = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(Icons.Outlined.Edit, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Edit), tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onEdit).padding(9.dp))
                Icon(Icons.Outlined.Delete, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Delete), tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onDelete).padding(9.dp))
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
        title = if (tag == null) com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NewTag) else com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.EditTag),
        onDismiss = onDismiss,
        onConfirm = { if (name.isNotBlank()) onConfirm(name.trim(), color) },
        confirmEnabled = name.isNotBlank()
    ) {
        OneUITextField(name, { name = it }, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TagName))
        Spacer(Modifier.height(16.dp))
        Text(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Color), style = MaterialTheme.typography.titleSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
        Spacer(Modifier.height(10.dp))
        colors.chunked(6).forEach { rowColors ->
            Row(Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                rowColors.forEach { hex ->
                    val selected = color.equals(hex, true)
                    Box(
                        Modifier.size(36.dp).clip(CircleShape).background(parseColorSafe(hex))
                            .then(if (selected) Modifier.border(3.dp, MaterialTheme.colorScheme.onSurface, CircleShape) else Modifier)
                            .clickable { color = hex }
                    )
                }
            }
            Spacer(Modifier.height(10.dp))
        }
    }
}
