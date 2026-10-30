package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.runtime.*
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.example.data.Dhikr
import com.example.data.Tag
import com.example.ui.oneui.*
import com.example.ui.viewmodel.MainViewModel

@Composable
fun FavoritesScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val favoriteDhikr by viewModel.favoriteDhikr.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val dhikrsWithTags by viewModel.dhikrsWithTags.collectAsStateWithLifecycle()
    val dialogs = remember { DhikrDialogState() }
    val tagsMap = remember(dhikrsWithTags) { dhikrsWithTags.associate { it.dhikr.id to it.tags } }
    FavoritesContent(
        favorites = favoriteDhikr,
        tagsMap = tagsMap,
        onBack = onBack,
        onToggleEnabled = viewModel::toggleEnabled,
        onToggleFavorite = viewModel::toggleFavorite,
        onClickTimes = { dialogs.times = it },
        onEdit = { dialogs.edit = it },
        onDelete = viewModel::deleteDhikr
    )
    DhikrDialogsHost(dialogs, viewModel, allTags, tagsMap)
}

@Composable
fun FavoritesContent(
    favorites: List<Dhikr>,
    tagsMap: Map<Int, List<Tag>>,
    onBack: () -> Unit,
    onToggleEnabled: (Dhikr) -> Unit,
    onToggleFavorite: (Dhikr) -> Unit,
    onClickTimes: (Dhikr) -> Unit,
    onEdit: (Dhikr) -> Unit,
    onDelete: (Dhikr) -> Unit
) {
    OneUIScreen(title = com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.Favorites), subtitle = azkarCount(favorites.size), onBack = onBack) {
        if (favorites.isEmpty()) {
            item { OneUIEmptyState(com.example.ui.i18n.L10n.t(com.example.ui.i18n.S.NoFavorites), Icons.Default.Star) }
        } else {
            dhikrCards(favorites, tagsMap, onToggleEnabled, onToggleFavorite, onClickTimes, onEdit, onDelete)
        }
    }
}
