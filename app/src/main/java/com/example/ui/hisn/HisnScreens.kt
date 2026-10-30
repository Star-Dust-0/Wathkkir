package com.example.ui.hisn
import com.example.ui.oneui.OneUIChipItem
import com.example.ui.oneui.OneUIChipGroup
import com.example.utils.plainClickable

import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import com.example.ThemeSettings

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.Info
import androidx.compose.material.icons.outlined.NotificationAdd
import androidx.compose.material.icons.outlined.Search
import kotlinx.coroutines.launch
import com.example.ui.oneui.OneUIIconAction
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.material.icons.outlined.StarBorder
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.hapticfeedback.HapticFeedbackType
import androidx.compose.ui.platform.LocalHapticFeedback
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.HisnChapter
import com.example.data.HisnItem
import com.example.ui.oneui.OneUIButton
import com.example.ui.oneui.OneUICard
import com.example.ui.oneui.OneUIChip
import com.example.ui.oneui.OneUIDivider
import com.example.ui.oneui.OneUIEmptyState
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.OneUISearchField
import com.example.ui.oneui.azkarCount

fun filterChapters(s: HisnListState, items: List<HisnItem>): List<HisnChapter> {
    val q = normalizeArabic(s.query.trim())
    if (q.isBlank()) {
        return if (s.onlyFavorites) s.chapters.filter { s.favoriteChapterIds.contains(it.id) } else s.chapters
    }
    // His spec 3.3.3: search always spans the FULL Hisn collection, even while the Favorites
    // filter is on; chapters whose items match count too. Matching favorites pin to the top
    // (only favorites that match the query), then the rest - both groups in collection order.
    val hitIds = items.filter { normalizeArabic(it.text).contains(q) }.map { it.chapterId }.toSet()
    val matches = s.chapters.filter { normalizeArabic(it.name).contains(q) || hitIds.contains(it.id) }
    return matches.sortedByDescending { s.favoriteChapterIds.contains(it.id) }
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
    val subtitle = if (state.chapters.isEmpty()) null else L10n.t(S.ChaptersFmt).format(state.chapters.size)
    val searchFocus = remember { androidx.compose.ui.focus.FocusRequester() }
    val hisnListState = androidx.compose.foundation.lazy.rememberLazyListState()
    val searchScope = androidx.compose.runtime.rememberCoroutineScope()
    OneUIScreen(
        title = L10n.t(S.HisnTab), subtitle = subtitle, onMenu = onMenu, listState = hisnListState,
        actions = {
            OneUIIconAction(Icons.Outlined.Search, L10n.t(S.SearchHisn)) {
                searchScope.launch {
                    hisnListState.animateScrollToItem(0)
                    try { searchFocus.requestFocus() } catch (_: Exception) {}
                }
            }
        }
    ) {
        item { OneUISearchField(state.query, onQuery, L10n.t(S.SearchHisn), focusRequester = searchFocus) }
        item {
            OneUIChipGroup(
                items = listOf(OneUIChipItem(L10n.t(S.All)), OneUIChipItem(L10n.t(S.Favorites))),
                selected = if (state.onlyFavorites) 1 else 0,
                onSelect = { onOnlyFavorites(it == 1) }
            )
        }
        if (filtered.isEmpty()) {
            item {
                OneUIEmptyState(
                    if (state.onlyFavorites && state.query.isBlank()) L10n.t(S.NoFavChapters) else L10n.t(S.NoResults),
                    Icons.Outlined.Search
                )
            }
        } else {
            item {
                OneUICard {
                    filtered.forEachIndexed { index, chapter ->
                        if (index > 0) OneUIDivider(16.dp)
                        ChapterRow(
                            chapter = chapter,
                            count = state.itemCounts[chapter.id] ?: 0,
                            favorite = state.favoriteChapterIds.contains(chapter.id),
                            read = state.readTodayIds.contains(chapter.id),
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
        modifier = Modifier
            .fillMaxWidth()
            .plainClickable(onClick = onClick)
            .heightIn(min = 64.dp)
            .padding(horizontal = 16.dp, vertical = 10.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Box(
            modifier = Modifier.size(36.dp).clip(CircleShape).background(cs.surfaceVariant),
            contentAlignment = Alignment.Center
        ) {
            Text(chapter.order.toString(), color = cs.onSurfaceVariant, style = MaterialTheme.typography.labelMedium)
        }
        Spacer(Modifier.width(14.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                chapter.name,
                color = cs.onSurface,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.bodyLarge
            )
            var meta = azkarCount(count)
            if (read) meta += L10n.t(S.ReadTodaySuffix)
            Text(
                meta,
                color = if (read) cs.primary else cs.onSurfaceVariant,
                style = MaterialTheme.typography.bodySmall
            )
        }
        Icon(
            if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
            L10n.t(S.Favorites),
            modifier = Modifier.size(26.dp).clip(CircleShape).clickable(onClick = onFavorite),
            tint = if (favorite) Color(0xFFFFC107) else cs.onSurfaceVariant
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
                if (favoriteChapter) Icons.Filled.Star else Icons.Outlined.StarBorder,
                L10n.t(S.Favorites),
                onToggleChapterFavorite
            )
        }
    ) {
        items(items, key = { it.id }) { item ->
            HisnItemCard(item, favoriteItemIds.contains(item.id)) { onToggleItemFavorite(item) }
        }
        item {
            Row(
                modifier = Modifier.fillMaxWidth().padding(top = 4.dp),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                OneUIButton(L10n.t(S.ReadDone), onMarkRead, Modifier.weight(1f), true, Icons.Outlined.Check)
                OneUIButton(L10n.t(S.AddToMyAzkar), onAddToMyAzkar, Modifier.weight(1f), false, Icons.Outlined.NotificationAdd)
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
    val meta = listOf(item.source, item.hokm).filter { it.isNotBlank() }.joinToString(" · ")
    val hasNotes = item.fadl.isNotBlank() || meta.isNotBlank()
    OneUICard(padding = androidx.compose.foundation.layout.PaddingValues(16.dp)) {
        Text(
            item.text,
            modifier = Modifier.fillMaxWidth(),
            color = cs.onSurface,
            fontSize = 20.sp,
            lineHeight = 36.sp,
            style = MaterialTheme.typography.bodyLarge
        )
        Spacer(Modifier.height(14.dp))
        Row(verticalAlignment = Alignment.CenterVertically) {
            val done = remaining == 0
            // 3.3.0: counter is light One UI blue (same as the bottom bar), turning light green when finished.
            val dk = com.example.ThemeSettings.isDark()
            val blue = Color(0xFF3D96FD) // sampled from the One UI RAM bar the user sent
            val greenBg = Color(0xFF24A890) // sampled from the One UI teal-green the user sent
            val greenFg = Color.White
            val chipBg by androidx.compose.animation.animateColorAsState(if (done) greenBg else blue, androidx.compose.animation.core.tween(350), label = "cntBg")
            val chipFg by androidx.compose.animation.animateColorAsState(if (done) greenFg else Color.White, androidx.compose.animation.core.tween(350), label = "cntFg")
            Row(
                modifier = Modifier
                    .height(40.dp)
                    .clip(CircleShape)
                    .background(chipBg)
                    .semantics { contentDescription = "عداد الذكر" }
                    .clickable {
                        if (com.example.utils.AppHaptics.enabled) haptic.performHapticFeedback(HapticFeedbackType.TextHandleMove)
                        remaining = if (remaining > 0) remaining - 1 else item.count
                    }
                    .padding(horizontal = 16.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                if (done) {
                    Icon(Icons.Outlined.Check, null, tint = chipFg, modifier = Modifier.size(18.dp))
                    Spacer(Modifier.width(8.dp))
                }
                val label = when {
                    done -> L10n.t(S.Done)
                    item.count > 1 -> "$remaining / ${item.count}"
                    else -> L10n.t(S.TapToCount)
                }
                Text(
                    label,
                    color = chipFg,
                    style = MaterialTheme.typography.labelLarge
                )
            }
            Spacer(Modifier.weight(1f))
            if (hasNotes) {
                Icon(
                    Icons.Outlined.Info,
                    if (L10n.t(S.Done) == L10n.t(S.Done)) "معلومات الذكر" else "Info",
                    tint = if (showFadl) cs.primary else cs.onSurfaceVariant,
                    modifier = Modifier
                        .size(40.dp)
                        .clip(CircleShape)
                        .clickable { showFadl = !showFadl }
                        .padding(8.dp)
                )
            }
            Icon(
                if (favorite) Icons.Filled.Star else Icons.Outlined.StarBorder,
                L10n.t(S.Favorites),
                tint = if (favorite) Color(0xFFFFC107) else cs.onSurfaceVariant,
                modifier = Modifier
                    .size(40.dp)
                    .clip(CircleShape)
                    .clickable(onClick = onFavorite)
                    .padding(8.dp)
            )
        }
        androidx.compose.animation.AnimatedVisibility(
            visible = showFadl && hasNotes,
            enter = androidx.compose.animation.expandVertically(androidx.compose.animation.core.tween(260)) + androidx.compose.animation.fadeIn(androidx.compose.animation.core.tween(260)),
            exit = androidx.compose.animation.shrinkVertically(androidx.compose.animation.core.tween(220)) + androidx.compose.animation.fadeOut(androidx.compose.animation.core.tween(180))
        ) {
            Column {
                if (item.fadl.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(item.fadl, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodyMedium)
                }
                if (meta.isNotBlank()) {
                    Spacer(Modifier.height(10.dp))
                    Text(meta, color = cs.onSurfaceVariant, style = MaterialTheme.typography.bodySmall)
                }
            }
        }
    }
}

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
        chapters,
        counts,
        favs.filter { it.itemId == 0 }.map { it.chapterId }.toSet(),
        read,
        query,
        only
    )
    HisnListContent(
        state = state,
        items = items,
        onQuery = { viewModel.setQuery(it) },
        onOnlyFavorites = { viewModel.setOnlyFavorites(it) },
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
    onAddToMyAzkar: (String, String) -> Unit
) {
    val chapters by viewModel.chapters.collectAsStateWithLifecycle()
    val all by viewModel.allItems.collectAsStateWithLifecycle()
    val favs by viewModel.favorites.collectAsStateWithLifecycle()
    val chapter = chapters.firstOrNull { it.id == chapterId } ?: return
    val items = remember(all, chapterId) { all.filter { it.chapterId == chapterId } }
    val favoriteChapter = favs.any { it.chapterId == chapterId && it.itemId == 0 }
    val favoriteItemIds = favs.filter { it.chapterId == chapterId && it.itemId != 0 }.map { it.itemId }.toSet()
    HisnChapterContent(
        chapter = chapter,
        items = items,
        favoriteChapter = favoriteChapter,
        favoriteItemIds = favoriteItemIds,
        onBack = onBack,
        onToggleChapterFavorite = { viewModel.toggleChapterFavorite(chapterId) },
        onToggleItemFavorite = { viewModel.toggleItemFavorite(chapterId, it.id) },
        onMarkRead = { viewModel.markChapterRead(chapterId) },
        onAddToMyAzkar = { onAddToMyAzkar(chapter.name, items.joinToString("\n\n") { it.text }) },
        alreadyAdded = chapter.name in onAddedTitles
    )
}
