package com.example.ui.screens
import com.example.ui.i18n.S
import com.example.ui.i18n.L10n
import com.example.utils.plainClickable

import androidx.compose.foundation.background
import com.example.utils.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.MoreVert
import androidx.compose.material.icons.outlined.Schedule
import androidx.compose.material.icons.outlined.Search
import androidx.compose.material.icons.outlined.StarBorder
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
import com.example.data.Dhikr
import com.example.data.Tag
import com.example.ui.oneui.oneUIBorderColor
import com.example.ui.oneui.oneUIFrosted
import com.example.ThemeSettings
import com.example.ui.oneui.FrostSpec
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIChipGroup
import com.example.ui.oneui.OneUIChipItem
import com.example.ui.oneui.OneUIEmptyState
import com.example.ui.oneui.OneUIIconAction
import com.example.ui.oneui.OneUIMenu
import com.example.ui.oneui.OneUIMenuItem
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.OneUISearchField
import com.example.ui.oneui.OneUISwitch
import com.example.ui.oneui.azkarCount
import com.example.ui.oneui.parseColorSafe
import com.example.ui.viewmodel.MainViewModel
import com.example.utils.DateTimeUtilsKt

@Composable
fun DhikrDialogsHost(
    state: DhikrDialogState,
    viewModel: MainViewModel,
    allTags: List<Tag>,
    tagsMap: Map<Int, List<Tag>>
) {
    val times = state.times
    if (times != null) {
        DhikrMultipleTimesDialog(
            dhikr = times,
            onDismiss = { state.times = null },
            onConfirm = { newTimes ->
                viewModel.updateTimes(times, newTimes)
                state.times = null
            }
        )
    }
    val edit = state.edit
    if (edit != null) {
        val tagIds = (tagsMap[edit.id] ?: emptyList()).map { it.tagId }
        DhikrEditDialog(
            dhikr = edit,
            allTags = allTags,
            initialTagIds = tagIds,
            onDismiss = { state.edit = null },
            onConfirm = { newTitle, newContent, ids ->
                viewModel.updateDhikrWithTags(edit, newTitle, newContent, ids)
                state.edit = null
            },
            onCreateTag = { viewModel.addTag(it, "#008080", null) }
        )
    }
}

fun filterDhikr(
    all: List<Dhikr>,
    query: String,
    selectedTag: Tag?,
    tagsMap: Map<Int, List<Tag>>
): List<Dhikr> {
    val q = query.trim()
    return all.filter { dhikr ->
        val tags = tagsMap[dhikr.id] ?: emptyList()
        val tagOk = selectedTag == null || tags.any { it.tagId == selectedTag.tagId }
        val queryOk = q.isBlank() ||
            dhikr.title.contains(q, ignoreCase = true) ||
            dhikr.content.contains(q, ignoreCase = true) ||
            tags.any { it.name.contains(q, ignoreCase = true) }
        tagOk && queryOk
    }
}

@Composable
fun AzkarListScreen(
    viewModel: MainViewModel,
    onAdd: () -> Unit,
    onMenu: (() -> Unit)? = null
) {
    val allDhikr by viewModel.allDhikr.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val dhikrsWithTags by viewModel.dhikrsWithTags.collectAsStateWithLifecycle()
    val selectedTag by viewModel.selectedTag.collectAsStateWithLifecycle()
    val query by viewModel.searchQuery.collectAsStateWithLifecycle()
    val dialogs = remember { DhikrDialogState() }
    val tagsMap = remember(dhikrsWithTags) {
        dhikrsWithTags.associate { it.dhikr.id to it.tags }
    }
    val filtered = remember(allDhikr, query, selectedTag, tagsMap) {
        filterDhikr(allDhikr, query, selectedTag, tagsMap)
    }
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
        title = L10n.t(S.AzkarTab),
        subtitle = azkarCount(totalCount),
        onMenu = onMenu,
        actions = { OneUIIconAction(Icons.Outlined.Add, L10n.t(S.AddDhikr), onAdd) }
    ) {
        item(key = "search") {
            OneUISearchField(query, onQuery, L10n.t(S.SearchAzkar))
        }
        if (allTags.isNotEmpty()) {
            item(key = "chips") {
                // 3.3.3: sliding-pill chip group (his spec: animation only, no joined container);
                // tapping the selected tag still deselects back to All.
                OneUIChipGroup(
                    items = listOf(OneUIChipItem(L10n.t(S.All))) + allTags.map { OneUIChipItem(it.name, parseColorSafe(it.colorHex)) },
                    selected = allTags.indexOfFirst { it.tagId == selectedTag?.tagId }.let { if (it < 0) 0 else it + 1 },
                    onSelect = { i ->
                        if (i == 0) onSelectTag(null)
                        else {
                            val tag = allTags[i - 1]
                            onSelectTag(if (selectedTag?.tagId == tag.tagId) null else tag)
                        }
                    }
                )
            }
        }
        if (dhikrs.isEmpty()) {
            item {
                val text = when {
                    query.isBlank() && selectedTag == null -> L10n.t(S.NoAzkar)
                    query.isBlank() -> L10n.t(S.NoAzkarUnderTag).format(selectedTag?.name ?: "")
                    else -> L10n.t(S.NoResultsQuery).format(query)
                }
                OneUIEmptyState(text, Icons.Outlined.Search)
            }
        } else {
            dhikrCards(
                dhikrs, tagsMap, onToggleEnabled, onToggleFavorite, onClickTimes, onEdit, onDelete,
                onSelectTag, selectedTag
            )
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
    onTagClick: ((Tag?) -> Unit)? = null,
    selectedTag: Tag? = null
) {
    items(dhikrs, key = { it.id }) { dhikr ->
        val tags = tagsMap[dhikr.id] ?: emptyList()
        DhikrItemCard(
            dhikr = dhikr,
            tags = tags,
            onToggleEnabled = { onToggleEnabled(dhikr) },
            onToggleFavorite = { onToggleFavorite(dhikr) },
            onClickTimes = { onClickTimes(dhikr) },
            onEditClick = { onEdit(dhikr) },
            onDeleteClick = { onDelete(dhikr) },
            onTagClick = if (onTagClick != null) { tag -> onTagClick(tag) } else null
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
    onTagClick: ((Tag) -> Unit)? = null
) {
    val cs = MaterialTheme.colorScheme
    var menu by remember { mutableStateOf(false) }
    val enabled = dhikr.isEnabled
    val favorite = dhikr.isFavorite
    OneUICard(padding = PaddingValues(18.dp, 16.dp, 18.dp, 10.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                dhikr.title,
                modifier = Modifier.weight(1f),
                color = cs.onSurface,
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(Modifier.width(12.dp))
            OneUISwitch(enabled, { onToggleEnabled() })
        }
        Spacer(Modifier.height(4.dp))
        val times = dhikr.reminderTimes.joinToString("، ") { DateTimeUtilsKt.formatTimeStr12h(it) }
        Row(
            modifier = Modifier
                .clip(RoundedCornerShape(8.dp))
                .plainClickable(onClick = onClickTimes)
                .padding(vertical = 4.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            val tint = if (enabled) cs.primary else cs.onSurfaceVariant
            Icon(Icons.Outlined.Schedule, null, modifier = Modifier.size(16.dp), tint = tint)
            Spacer(Modifier.width(6.dp))
            Text(times, color = tint, style = MaterialTheme.typography.bodySmall)
        }
        Spacer(Modifier.height(10.dp))
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .oneUIFrosted(RoundedCornerShape(16.dp), FrostSpec(cs.surfaceVariant, if (ThemeSettings.isDark()) 0.50f else 0.62f, oneUIBorderColor()), 16)
                .padding(14.dp)
        ) {
            Text(dhikr.content, color = cs.onSurface, style = MaterialTheme.typography.bodyLarge)
        }
        Row(
            modifier = Modifier.fillMaxWidth().padding(top = 6.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                modifier = Modifier.weight(1f),
                horizontalArrangement = Arrangement.spacedBy(10.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                tags.forEach { tag ->
                    Row(
                        modifier = Modifier
                            .clip(CircleShape)
                            .then(if (onTagClick != null) Modifier.clickable { onTagClick(tag) } else Modifier)
                            .padding(vertical = 6.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Box(
                            Modifier.size(8.dp).clip(CircleShape).background(parseColorSafe(tag.colorHex))
                        )
                        Spacer(Modifier.width(5.dp))
                        Text(tag.name, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                    }
                }
            }
            Icon(
                if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                L10n.t(S.Favorites),
                modifier = Modifier
                    .clip(CircleShape)
                    .clickable(onClick = onToggleFavorite)
                    .padding(10.dp),
                tint = if (favorite) com.example.ui.theme.OneUIUpcomingAmber else cs.onSurfaceVariant
            )
            Box {
                Icon(
                    Icons.Outlined.MoreVert,
                    L10n.t(S.Options),
                    modifier = Modifier
                        .clip(CircleShape)
                        .plainClickable { menu = true }
                        .padding(10.dp),
                    tint = cs.onSurfaceVariant
                )
                OneUIMenu(menu, { menu = false }) {
                    OneUIMenuItem(L10n.t(S.EditDhikrTags), { menu = false; onEditClick() }, Icons.Outlined.Edit)
                    OneUIMenuItem(L10n.t(S.ReminderTimes), { menu = false; onClickTimes() }, Icons.Outlined.Schedule)
                    OneUIMenuItem(L10n.t(S.Delete), { menu = false; onDeleteClick() }, Icons.Outlined.Delete, true)
                }
            }
        }
    }
}
