package com.example.ui.hisn
import com.example.data.HisnChapter
// Secondary state reconstruction from historical HisnListContent/HisnListScreen constructor use.
data class HisnListState(val chapters: List<HisnChapter>, val itemCounts: Map<Int,Int>,
    val favoriteChapterIds: Set<Int>, val readTodayIds: Set<Int>, val query: String, val onlyFavorites: Boolean)
