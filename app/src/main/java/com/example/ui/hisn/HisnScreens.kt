package com.example.ui.hisn

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyListScope
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.MenuBook
import androidx.compose.material.icons.filled.*
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HisnChapter
import com.example.data.HisnItem
import com.example.ui.oneui.*

data class HisnListState(
    val chapters: List<HisnChapter>,
    val itemCounts: Map<Int, Int>,
    val favoriteChapterIds: Set<Int>,
    val readTodayIds: Set<Int>,
    val query: String,
    val onlyFavorites: Boolean
)

fun filterChapters(
    s: HisnListState,
    items: List<HisnItem>
): List<HisnChapter> {
    val q = normalizeArabic(s.query.trim())
    val list = s.chapters
    if (q.isBlank()) return if (s.onlyFavorites) list.filter { it.id in s.favoriteChapterIds } else list
    val hitIds = items.filter { normalizeArabic(it.text).contains(q) }.map { it.chapterId }.toSet()
    return list.filter { normalizeArabic(it.name).contains(q) || it.id in hitIds }
        .sortedByDescending { it.id in s.favoriteChapterIds }
}

@Composable
fun HisnListContent(
    state: HisnListState,
    items: List<HisnItem>,
    onQuery: (String) -> Unit,
    onOnlyFavorites: (Boolean) -> Unit,
    onOpen: (HisnChapter) -> Unit,
    onToggleFavorite: (HisnChapter) -> Unit,
    onMenu: (() -> Unit)? = null
) {
    val filtered = remember(state, items) { filterChapters(state, items) }
    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    OneUIScreen(
        onMenu = onMenu,
        actions = {
            OneUIIconAction(Icons.Default.Search, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.SearchHisn), { try { searchFocus.requestFocus() } catch (_: Exception) {} })
            OneUIIconAction(androidx.compose.material.icons.Icons.Default.Settings, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Settings), { com.example.ui.oneui.HeaderTools.nav?.navigate("settings") })
        },
        title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.HisnTab),
        subtitle = if (state.chapters.isEmpty()) null else com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ChaptersFmt).format(state.chapters.size)
    ) {
        item(key = "search") {
            OneUISearchField(state.query, onQuery, com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.SearchHisn), focusRequester = searchFocus)
        }
        item(key = "chips") {
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp),
                modifier = Modifier.fillMaxWidth()
            ) {
                item { OneUIChip(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.All), !state.onlyFavorites, { onOnlyFavorites(false) }) }
                item { OneUIChip(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Favorites), state.onlyFavorites, { onOnlyFavorites(true) }) }
            }
        }
        if (filtered.isEmpty()) {
            item { OneUIEmptyState(if (state.onlyFavorites) com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NoFavChapters) else com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NoResults), Icons.Default.Search) }
        } else {
            item(key = "group") {
                OneUICard {
                    filtered.forEachIndexed { index, chapter ->
                        if (index > 0) OneUIDivider(inset = 16.dp)
                        ChapterRow(
                            chapter = chapter,
                            count = state.itemCounts[chapter.id] ?: 0,
                            favorite = chapter.id in state.favoriteChapterIds,
                            read = chapter.id in state.readTodayIds,
                            onClick = { onOpen(chapter) },
                            onFavorite = { onToggleFavorite(chapter) }
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun ChapterRow(
    chapter: HisnChapter,
    count: Int,
    favorite: Boolean,
    read: Boolean,
    onClick: () -> Unit,
    onFavorite: () -> Unit
) {
    val cs = MaterialTheme.colorScheme
    Row(
        Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            Modifier.size(36.dp).clip(CircleShape).background(cs.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(
                "${chapter.order}",
                style = MaterialTheme.typography.labelMedium,
                color = cs.onSurfaceVariant
            )
        }
        Spacer(Modifier.width(14.dp))
        Column(Modifier.weight(1f)) {
            Text(chapter.name, style = MaterialTheme.typography.bodyLarge, color = cs.onSurface, maxLines = 2)
            Text(
                if (read) azkarCount(count) + com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ReadTodaySuffix) else azkarCount(count),
                style = MaterialTheme.typography.bodySmall,
                color = if (read) cs.primary else cs.onSurfaceVariant
            )
        }
        Icon(
            if (favorite) Icons.Default.Star else Icons.Outlined.StarBorder,
            contentDescription = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Favorites),
            tint = if (favorite) Color(0xFFFFC107) else cs.onSurfaceVariant,
            modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onFavorite)
        )
    }
}

@Composable
fun HisnChapterContent(
    chapter: HisnChapter,
    items: List<HisnItem>,
    favoriteChapter: Boolean,
    favoriteItemIds: Set<Int>,
    onBack: () -> Unit,
    onToggleChapterFavorite: () -> Unit,
    onToggleItemFavorite: (HisnItem) -> Unit,
    onMarkRead: () -> Unit,
    onAddToMyAzkar: () -> Unit,
    alreadyAdded: Boolean = false
) {
    var tapped by rememberSaveable(chapter.id) { mutableStateOf(false) }
    val added = tapped || alreadyAdded
    OneUIScreen(
        title = chapter.name,
        subtitle = azkarCount(items.size),
        onBack = onBack,
        actions = {
            OneUIIconAction(
                if (favoriteChapter) Icons.Default.Star else Icons.Outlined.StarBorder,
                com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Favorites),
                onToggleChapterFavorite
            )
        }
    ) {
        items(items, key = { it.id }) { item ->
            HisnItemCard(item, item.id in favoriteItemIds) { onToggleItemFavorite(item) }
        }
        item(key = "actions") {
            Row(Modifier.fillMaxWidth().padding(top = 4.dp), horizontalArrangement = Arrangement.spacedBy(10.dp)) {
                OneUIButton(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.ReadDone), onMarkRead, Modifier.weight(1f), filled = true, icon = Icons.Default.Check)
                OneUIButton(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.AddToMyAzkar), onAddToMyAzkar, Modifier.weight(1f), icon = Icons.Default.NotificationAdd)
            }
        }
    }
}

@Composable
fun HisnItemCard(item: HisnItem, favorite: Boolean, onFavorite: () -> Unit) {
    val cs = MaterialTheme.colorScheme
    var remaining by rememberSaveable(item.id) { mutableIntStateOf(item.count) }
    var showFadl by rememberSaveable(item.id) { mutableStateOf(false) }
    val haptic = LocalHapticFeedback.current
    OneUICard(padding = PaddingValues(16.dp)) {
        Text(
            item.text,
            style = MaterialTheme.typography.bodyLarge,
            fontSize = 20.sp,
            lineHeight = 36.sp,
            color = cs.onSurface,
            modifier = Modifier.fillMaxWidth()
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            // Counter pill: tap to count down
            val done = remaining == 0
            Row(
                Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(if (done) cs.primary.copy(alpha = 0.16f) else cs.surfaceVariant)
                    .clickable {
                        if (com.example.utils.AppHaptics.enabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        remaining = if (remaining > 0) remaining - 1 else item.count
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(
                    if (done) Icons.Default.Check else Icons.Default.TouchApp,
                    null,
                    tint = if (done) cs.primary else cs.onSurfaceVariant,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(Modifier.width(8.dp))
                Text(
                    if (done) com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Done) else if (item.count > 1) "$remaining / ${item.count}" else com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.TapToCount),
                    style = MaterialTheme.typography.labelLarge,
                    color = if (done) cs.primary else cs.onSurface
                )
            }
            Spacer(Modifier.weight(1f))
            if (item.fadl.isNotBlank()) {
                Icon(
                    Icons.Default.Info,
                    if (com.example.ui.i18n.L10n.lang == com.example.ui.i18n.Lang.AR) "معلومات الذكر" else "Info",
                    tint = if (showFadl) cs.primary else cs.onSurfaceVariant,
                    modifier = Modifier.size(40.dp).clip(CircleShape).clickable { showFadl = !showFadl }.padding(8.dp)
                )
            }
            Icon(
                if (favorite) Icons.Default.Star else Icons.Outlined.StarBorder,
                com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Favorites),
                tint = if (favorite) Color(0xFFFFC107) else cs.onSurfaceVariant,
                modifier = Modifier.size(40.dp).clip(CircleShape).clickable(onClick = onFavorite).padding(8.dp)
            )
        }
        if (showFadl && item.fadl.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(item.fadl, style = MaterialTheme.typography.bodyMedium, color = cs.onSurfaceVariant)
        }
        val meta = listOf(item.source, item.hokm).filter { it.isNotBlank() }.joinToString(" · ")
        if (meta.isNotBlank()) {
            Spacer(Modifier.height(10.dp))
            Text(meta, style = MaterialTheme.typography.bodySmall, color = cs.onSurfaceVariant)
        }
    }
}

// ---- ViewModel wrappers ----

@Composable
fun HisnListScreen(
    viewModel: HisnViewModel,
    onOpen: (HisnChapter) -> Unit,
    onMenu: (() -> Unit)? = null
) {
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val items by viewModel.allItems.collectAsStateWithLifecycle()
    val counts by viewModel.itemCounts.collectAsStateWithLifecycle()
    val favs by viewModel.favorites.collectAsStateWithLifecycle()
    val read by viewModel.readToday.collectAsStateWithLifecycle()
    val query by viewModel.query.collectAsStateWithLifecycle()
    val only by viewModel.onlyFavorites.collectAsStateWithLifecycle()
    val state = HisnListState(
        chapters, counts, favs.filter { it.itemId == 0 }.map { it.chapterId }.toSet(), read, query, only
    )
    HisnListContent(
        state = state,
        items = items,
        onQuery = viewModel::setQuery,
        onOnlyFavorites = viewModel::setOnlyFavorites,
        onOpen = onOpen,
        onToggleFavorite = { viewModel.toggleChapterFavorite(it.id) },
        onMenu = onMenu
    )
}

@Composable
fun HisnChapterScreen(
    chapterId: Int,
    viewModel: HisnViewModel,
    onBack: () -> Unit,
    onAddedTitles: Set<String> = emptySet(),
    onAddToMyAzkar: (title: String, content: String) -> Unit
) {
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val all by viewModel.allItems.collectAsStateWithLifecycle()
    val favs by viewModel.favorites.collectAsStateWithLifecycle()
    val chapter = chapters.firstOrNull { it.id == chapterId } ?: return
    val items = remember(all, chapterId) { all.filter { it.chapterId == chapterId } }
    HisnChapterContent(
        chapter = chapter,
        items = items,
        favoriteChapter = favs.any { it.chapterId == chapterId && it.itemId == 0 },
        favoriteItemIds = favs.filter { it.chapterId == chapterId && it.itemId != 0 }.map { it.itemId }.toSet(),
        onBack = onBack,
        onToggleChapterFavorite = { viewModel.toggleChapterFavorite(chapterId) },
        onToggleItemFavorite = { viewModel.toggleItemFavorite(chapterId, it.id) },
        onMarkRead = { viewModel.markChapterRead(chapterId) },
        onAddToMyAzkar = { onAddToMyAzkar(chapter.name, items.joinToString("\n\n") { it.text }) },
        alreadyAdded = chapter.name in onAddedTitles
    )
}
