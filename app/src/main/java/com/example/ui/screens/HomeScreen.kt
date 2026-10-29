package com.example.ui.screens

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.StarBorder
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
import com.example.data.Dhikr
import com.example.data.Tag
import com.example.ui.oneui.*
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.formatTimeStr12h

class DhikrDialogState {
    var times by mutableStateOf<Dhikr?>(null)
    var edit by mutableStateOf<Dhikr?>(null)
}

@Composable
fun DhikrDialogsHost(
    state: DhikrDialogState,
    viewModel: MainViewModel,
    allTags: List<Tag>,
    tagsMap: Map<Int, List<Tag>>
) {
    state.times?.let { dhikr ->
        DhikrMultipleTimesDialog(
            dhikr = dhikr,
            onDismiss = { state.times = null },
            onConfirm = { newTimes ->
                viewModel.updateTimes(dhikr, newTimes)
                state.times = null
            }
        )
    }
    state.edit?.let { dhikr ->
        val currentTagIds = (tagsMap[dhikr.id] ?: emptyList()).map { it.tagId }
        DhikrEditDialog(
            dhikr = dhikr,
            allTags = allTags,
            initialTagIds = currentTagIds,
            onDismiss = { state.edit = null },
            onConfirm = { newTitle, newContent, tagIds ->
                viewModel.updateDhikrWithTags(dhikr, newTitle, newContent, tagIds)
                state.edit = null
            },
            onCreateTag = { viewModel.addTag(it, "#008080") }
        )
    }
}

fun filterDhikr(
    all: List<Dhikr>,
    query: String,
    selectedTag: Tag?,
    tagsMap: Map<Int, List<Tag>>
): List<Dhikr> = all.filter { dhikr ->
    val tags = tagsMap[dhikr.id] ?: emptyList()
    val matchesTag = selectedTag == null || tags.any { it.tagId == selectedTag.tagId }
    val q = query.trim()
    val matchesQuery = q.isBlank() ||
            dhikr.title.contains(q, ignoreCase = true) ||
            dhikr.content.contains(q, ignoreCase = true) ||
            tags.any { it.name.contains(q, ignoreCase = true) }
    matchesTag && matchesQuery
}

@Composable
fun AzkarListScreen(viewModel: MainViewModel, onAdd: () -> Unit, onMenu: (() -> Unit)? = null) {
    val allDhikr by viewModel.allDhikr.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val dhikrsWithTags by viewModel.dhikrsWithTags.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val dialogs = remember { DhikrDialogState() }
    val tagsMap = remember(dhikrsWithTags) { dhikrsWithTags.associate { it.dhikr.id to it.tags } }
    val filtered = remember(allDhikr, query, selectedTag, tagsMap) { filterDhikr(allDhikr, query, selectedTag, tagsMap) }

    AzkarListContent(
        dhikrs = filtered,
        totalCount = allDhikr.size,
        allTags = allTags,
        tagsMap = tagsMap,
        selectedTag = selectedTag,
        query = query,
        onQuery = viewModel::setSearchQuery,
        onSelectTag = viewModel::setSelectedTag,
        onAdd = onAdd,
        onToggleEnabled = viewModel::toggleEnabled,
        onToggleFavorite = viewModel::toggleFavorite,
        onClickTimes = { dialogs.times = it },
        onEdit = { dialogs.edit = it },
        onDelete = viewModel::deleteDhikr,
        onMenu = onMenu
    )
    DhikrDialogsHost(dialogs, viewModel, allTags, tagsMap)
}

@Composable
fun AzkarListContent(
    dhikrs: List<Dhikr>,
    totalCount: Int,
    allTags: List<Tag>,
    tagsMap: Map<Int, List<Tag>>,
    selectedTag: Tag?,
    query: String,
    onQuery: (String) -> Unit,
    onSelectTag: (Tag?) -> Unit,
    onAdd: () -> Unit,
    onToggleEnabled: (Dhikr) -> Unit,
    onToggleFavorite: (Dhikr) -> Unit,
    onClickTimes: (Dhikr) -> Unit,
    onEdit: (Dhikr) -> Unit,
    onDelete: (Dhikr) -> Unit,
    onMenu: (() -> Unit)? = null
) {
    OneUIScreen(
        title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.AzkarTab),
        subtitle = azkarCount(totalCount),
        onMenu = onMenu,
        actions = {
            OneUIIconAction(Icons.Default.Add, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.AddDhikr), onAdd)
            OneUIIconAction(Icons.Default.Settings, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Settings), { com.example.ui.oneui.HeaderTools.nav?.navigate("settings") })
        }
    ) {
        item(key = "search") { OneUISearchField(query, onQuery, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.SearchAzkar)) }
        if (allTags.isNotEmpty()) {
            item(key = "chips") {
                LazyRow(horizontalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.fillMaxWidth()) {
                    item { OneUIChip(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.All), selectedTag == null, { onSelectTag(null) }) }
                    items(allTags, key = { it.tagId }) { tag ->
                        val sel = selectedTag?.tagId == tag.tagId
                        OneUIChip(tag.name, sel, { onSelectTag(if (sel) null else tag) }, dot = parseColorSafe(tag.colorHex))
                    }
                }
            }
        }
        if (dhikrs.isEmpty()) {
            item {
                OneUIEmptyState(
                    if (query.isBlank() && selectedTag == null) com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NoAzkar)
                    else if (query.isBlank()) "لا توجد أذكار تحت تصنيف \"${selectedTag?.name}\""
                    else "لا توجد نتائج تطابق \"$query\"",
                    Icons.Default.Search
                )
            }
        } else {
            dhikrCards(dhikrs, tagsMap, onToggleEnabled, onToggleFavorite, onClickTimes, onEdit, onDelete, onSelectTag, selectedTag)
        }
    }
}

fun LazyListScope.dhikrCards(
    dhikrs: List<Dhikr>,
    tagsMap: Map<Int, List<Tag>>,
    onToggleEnabled: (Dhikr) -> Unit,
    onToggleFavorite: (Dhikr) -> Unit,
    onClickTimes: (Dhikr) -> Unit,
    onEdit: (Dhikr) -> Unit,
    onDelete: (Dhikr) -> Unit,
    onSelectTag: ((Tag?) -> Unit)? = null,
    selectedTag: Tag? = null
) {
    items(dhikrs, key = { "d${it.id}" }) { dhikr ->
        DhikrItemCard(
            dhikr = dhikr,
            tags = tagsMap[dhikr.id] ?: emptyList(),
            onToggleEnabled = { onToggleEnabled(dhikr) },
            onToggleFavorite = { onToggleFavorite(dhikr) },
            onClickTimes = { onClickTimes(dhikr) },
            onEditClick = { onEdit(dhikr) },
            onDeleteClick = { onDelete(dhikr) },
            onTagClick = onSelectTag?.let { f -> { tag: Tag -> f(if (selectedTag?.tagId == tag.tagId) null else tag) } }
        )
    }
}

@Composable
fun DhikrItemCard(
    dhikr: Dhikr,
    tags: List<Tag> = emptyList(),
    onToggleEnabled: () -> Unit,
    onToggleFavorite: () -> Unit,
    onClickTimes: () -> Unit,
    onEditClick: () -> Unit,
    onDeleteClick: () -> Unit,
    onTagClick: ((Tag) -> Unit)? = null,
) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    OneUICard(padding = PaddingValues(start = 18.dp, end = 18.dp, top = 16.dp, bottom = 10.dp)) {
        Row(verticalAlignment = Alignment.CenterVertically) {
            Text(
                dhikr.title,
                style = MaterialTheme.typography.titleMedium,
                color = cs.onSurface,
                modifier = Modifier.weight(1f)
            )
            Spacer(Modifier.width(12.dp))
            OneUISwitch(dhikr.isEnabled, { onToggleEnabled() })
        }
        Spacer(Modifier.height(4.dp))
        val times = dhikr.reminderTimes.joinToString("، ") { formatTimeStr12h(it) }
        Row(
            Modifier.clip(RoundedCornerShape(8.dp)).clickable(onClick = onClickTimes).padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Outlined.Schedule, null,
                tint = if (dhikr.isEnabled) cs.primary else cs.onSurfaceVariant,
                modifier = Modifier.size(16.dp)
            )
            Spacer(Modifier.width(6.dp))
            Text(
                times,
                style = MaterialTheme.typography.bodySmall,
                color = if (dhikr.isEnabled) cs.primary else cs.onSurfaceVariant
            )
        }
        Spacer(Modifier.height(10.dp))
        Box(
            Modifier.fillMaxWidth().clip(RoundedCornerShape(16.dp)).background(cs.surfaceVariant).padding(14.dp)
        ) {
            Text(dhikr.content, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface)
        }
        Row(
            Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(Modifier.weight(1f), horizontalArrangement = Arrangement.spacedBy(10.dp), verticalAlignment = Alignment.CenterVertically) {
                tags.take(3).forEach { tag ->
                    Row(
                        Modifier.clip(CircleShape)
                            .then(if (onTagClick != null) Modifier.clickable { onTagClick(tag) } else Modifier)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(Modifier.size(8.dp).clip(CircleShape).background(parseColorSafe(tag.colorHex)))
                        Spacer(Modifier.width(5.dp))
                        Text(tag.name, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
                    }
                }
            }
            Icon(
                if (dhikr.isFavorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Favorites),
                tint = if (dhikr.isFavorite) Color(0xFFFFC107) else cs.onSurfaceVariant,
                modifier = Modifier.size(44.dp).clip(CircleShape).clickable(onClick = onToggleFavorite).padding(10.dp)
            )
            Box {
                Icon(
                    Icons.Default.MoreVert, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Options), tint = cs.onSurfaceVariant,
                    modifier = Modifier.size(44.dp).clip(CircleShape).clickable { menu = true }.padding(10.dp)
                )
                OneUIMenu(menu, { menu = false }) {
                    OneUIMenuItem(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.EditDhikrTags), { menu = false; onEditClick() }, Icons.Outlined.Edit)
                    OneUIMenuItem(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ReminderTimes), { menu = false; onClickTimes() }, Icons.Outlined.Schedule)
                    OneUIMenuItem(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Delete), { menu = false; onDeleteClick() }, Icons.Outlined.Delete, destructive = true)
                }
            }
        }
    }
}
