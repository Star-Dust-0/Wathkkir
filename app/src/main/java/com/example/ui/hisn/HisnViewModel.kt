package com.example.ui.hisn

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.*
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Strips tashkeel/tatweel and unifies alef/yaa/taa-marbuta so search works on vocalised text. */
fun normalizeArabic(s: String): String =
    s.replace(Regex("[\\u064B-\\u0652\\u0670\\u06D6-\\u06ED\\u0640]"), "")
        .replace(Regex("[\\u0622\\u0623\\u0625\\u0671]"), "\u0627")
        .replace('\u0649', '\u064A')
        .replace('\u0629', '\u0647')

class HisnViewModel(application: Application) : AndroidViewModel(application) {
    private val dao = AppDatabase.getDatabase(application).hisnDao()

    private val today: String get() = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date())

    private val _query = MutableStateFlow("")
    val query: StateFlow<String> = _query.asStateFlow()
    fun setQuery(q: String) { _query.value = q }

    private val _onlyFavorites = MutableStateFlow(false)
    val onlyFavorites: StateFlow<Boolean> = _onlyFavorites.asStateFlow()
    fun setOnlyFavorites(v: Boolean) { _onlyFavorites.value = v }

    val chapters: StateFlow<List<HisnChapter>> = dao.chapters()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val allItems: StateFlow<List<HisnItem>> = dao.allItems()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val itemCounts: StateFlow<Map<Int, Int>> = dao.itemCounts().map { l -> l.associate { it.chapterId to it.itemCount } }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyMap())
    val favorites: StateFlow<List<HisnFavorite>> = dao.favorites()
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptyList())
    val readToday: StateFlow<Set<Int>> = dao.chaptersReadOn(today).map { it.toSet() }
        .stateIn(viewModelScope, SharingStarted.WhileSubscribed(5000), emptySet())

    init {
        viewModelScope.launch(Dispatchers.IO) {
            try { HisnImporter.ensureImported(getApplication(), dao) } catch (e: Exception) { e.printStackTrace() }
        }
    }

    fun toggleChapterFavorite(chapterId: Int) = toggle(chapterId, 0)
    fun toggleItemFavorite(chapterId: Int, itemId: Int) = toggle(chapterId, itemId)

    private fun toggle(chapterId: Int, itemId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            val exists = favorites.value.any { it.chapterId == chapterId && it.itemId == itemId }
            if (exists) dao.removeFavorite(chapterId, itemId) else dao.addFavorite(HisnFavorite(chapterId = chapterId, itemId = itemId))
        }
    }

    fun markChapterRead(chapterId: Int) {
        viewModelScope.launch(Dispatchers.IO) {
            dao.insertRead(HisnRead(chapterId = chapterId, dateString = today, timestamp = System.currentTimeMillis()))
        }
    }
}
