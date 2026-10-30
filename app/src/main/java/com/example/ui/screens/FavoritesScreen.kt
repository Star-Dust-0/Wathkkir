package com.example.ui.screens

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Star
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.compose.runtime.getValue
import com.example.data.Dhikr
import com.example.data.Tag
import com.example.ui.oneui.OneUIEmptyState
import com.example.ui.oneui.OneUIScreen
import com.example.ui.oneui.azkarCount
import com.example.ui.viewmodel.MainViewModel

@Composable
fun FavoritesScreen(viewModel: MainViewModel, onBack: () -> Unit) {
    val favorites by viewModel.favoriteDhikr.collectAsStateWithLifecycle()
    val allTags by viewModel.allTags.collectAsStateWithLifecycle()
    val dhikrsWithTags by viewModel.dhikrsWithTags.collectAsStateWithLifecycle()
    val dialogs = remember { DhikrDialogState() }
    val tagsMap = remember(dhikrsWithTags) { dhikrsWithTags.associate { it.dhikr.id to it.tags } }

    FavoritesContent(
        favorites = favorites,
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
    OneUIScreen(title = "المفضلة", subtitle = azkarCount(favorites.size), onBack = onBack) {
        if (favorites.isEmpty()) {
            item { OneUIEmptyState("لا توجد أذكار مفضلة", Icons.Outlined.Star) }
        } else {
            dhikrCards(favorites, tagsMap, onToggleEnabled, onToggleFavorite, onClickTimes, onEdit, onDelete)
        }
    }
}
