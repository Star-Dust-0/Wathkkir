package com.example.ui.viewmodel

import android.app.Application
import android.content.Context
import android.content.SharedPreferences
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.DhikrAlarmScheduler
import com.example.data.AppDatabase
import com.example.data.Dhikr
import com.example.data.DhikrHistory
import com.example.data.DhikrRepository
import com.example.data.DhikrWithTags
import com.example.data.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** Rebuilt from the original 3.4.2 MainViewModel (R8 name mu0) and its coroutine helper classes. */
class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val tasbeehPrefs: SharedPreferences = application.getSharedPreferences("tasbeeh_prefs", Context.MODE_PRIVATE)
    private val appPrefs: SharedPreferences = application.getSharedPreferences("app_prefs", Context.MODE_PRIVATE)

    init { com.example.utils.NotifPrefs.migrate(application) }

    private val _vibrationEnabled = MutableStateFlow(appPrefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled: StateFlow<Boolean> = _vibrationEnabled.asStateFlow()

    private val _appFont = MutableStateFlow(
        appPrefs.getString("app_font", null)
            ?: if (appPrefs.contains("use_device_font") && appPrefs.getBoolean("use_device_font", false)) "system" else "app"
    )
    val appFont: StateFlow<String> = _appFont.asStateFlow()

    private val _notificationSound = MutableStateFlow(appPrefs.getString("notification_sound", "default") ?: "default")
    val notificationSound: StateFlow<String> = _notificationSound.asStateFlow()

    private val _searchQuery = MutableStateFlow("")
    val searchQuery: StateFlow<String> = _searchQuery.asStateFlow()

    private val _selectedTag = MutableStateFlow<Tag?>(null)
    val selectedTag: StateFlow<Tag?> = _selectedTag.asStateFlow()

    private val _selectedDhikrFromNotification = MutableStateFlow<Dhikr?>(null)
    val selectedDhikrFromNotification: StateFlow<Dhikr?> = _selectedDhikrFromNotification.asStateFlow()

    private val _tasbeehCount = MutableStateFlow(tasbeehPrefs.getInt("count", 0))
    val tasbeehCount: StateFlow<Int> = _tasbeehCount.asStateFlow()

    private val repository: DhikrRepository
    private val scheduler = DhikrAlarmScheduler(application)

    val allHistory: StateFlow<List<DhikrHistory>>
    val stats: StateFlow<DhikrStats>
    val allDhikr: StateFlow<List<Dhikr>>
    val favoriteDhikr: StateFlow<List<Dhikr>>
    val enabledDhikr: StateFlow<List<Dhikr>>
    val allTags: StateFlow<List<Tag>>
    val dhikrsWithTags: StateFlow<List<DhikrWithTags>>

    init {
        val db = AppDatabase.getDatabase(application)
        repository = DhikrRepository(db.dhikrDao(), db.tagDao(), db.historyDao())
        val started = SharingStarted.WhileSubscribed(5000L)
        allHistory = repository.allHistory.stateIn(viewModelScope, started, emptyList())
        stats = repository.allHistory.map(::computeStats).stateIn(viewModelScope, started, DhikrStats())
        allDhikr = repository.allDhikr.stateIn(viewModelScope, started, emptyList())
        favoriteDhikr = repository.favoriteDhikr.stateIn(viewModelScope, started, emptyList())
        enabledDhikr = repository.enabledDhikr.stateIn(viewModelScope, started, emptyList())
        allTags = repository.allTags.stateIn(viewModelScope, started, emptyList())
        dhikrsWithTags = repository.dhikrsWithTags.stateIn(viewModelScope, started, emptyList())
    }

    private fun computeStats(list: List<DhikrHistory>): DhikrStats {
        if (list.isEmpty()) return DhikrStats()
        val startOfToday = Calendar.getInstance().apply {
            set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0)
        }
        val today = startOfToday.timeInMillis
        val weekStart = (startOfToday.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis
        val monthStart = (startOfToday.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        var todayCount = 0
        var weekCount = 0
        var monthCount = 0
        val perDhikr = LinkedHashMap<Int, Int>()
        val dates = LinkedHashSet<String>()
        for (h in list) {
            if (h.timestamp >= today) todayCount++
            if (h.timestamp >= weekStart) weekCount++
            if (h.timestamp >= monthStart) monthCount++
            perDhikr[h.dhikrId] = (perDhikr[h.dhikrId] ?: 0) + 1
            dates.add(h.dateString)
        }
        val fmt = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val cursor = startOfToday.clone() as Calendar
        var day = fmt.format(cursor.time)
        if (!dates.contains(day)) {
            cursor.add(Calendar.DAY_OF_YEAR, -1)
            day = fmt.format(cursor.time)
        }
        var streak = 0
        while (dates.contains(day)) {
            streak++
            cursor.add(Calendar.DAY_OF_YEAR, -1)
            day = fmt.format(cursor.time)
        }
        val top = perDhikr.entries.map { it.key to it.value }.sortedByDescending { it.second }
        return DhikrStats(todayCount, weekCount, monthCount, list.size, streak, top)
    }

    /** Original method e(title, content, reminder): insert then schedule. */
    fun addDhikrWithSchedule(title: String, content: String, times: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val id = repository.insert(Dhikr(title = title, content = content, reminderTimes = times, isEnabled = true))
            scheduler.schedule(Dhikr(id = id.toInt(), title = title, content = content, reminderTimes = times, isEnabled = true))
        }
    }

    fun addDhikrWithScheduleAndTags(title: String, content: String, times: List<String>, tagIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val d = Dhikr(title = title, content = content, reminderTimes = times, isEnabled = true)
            val id = repository.insert(d).toInt()
            if (tagIds.isNotEmpty()) repository.updateDhikrTags(id, tagIds)
            scheduler.schedule(d.copy(id = id))
        }
    }

    /** Original method f(dhikr): toggle enabled, persist, (re)schedule or cancel. */
    fun toggleEnabled(dhikr: Dhikr) {
        viewModelScope.launch(Dispatchers.IO) {
            val enabled = !dhikr.isEnabled
            repository.update(dhikr.copy(isEnabled = enabled))
            if (enabled) scheduler.schedule(dhikr) else scheduler.cancel(dhikr)
        }
    }

    fun updateReminderTimes(dhikr: Dhikr, times: List<String>) {
        viewModelScope.launch(Dispatchers.IO) {
            val updated = dhikr.copy(reminderTimes = times, isEnabled = true)
            repository.update(updated)
            scheduler.schedule(updated)
        }
    }

    fun toggleVibration() {
        val v = !_vibrationEnabled.value
        _vibrationEnabled.value = v
        com.example.utils.NotifPrefs.setVibration(getApplication(), v)
    }

    fun updateNotificationSound(uri: String) {
        _notificationSound.value = uri
        val title = try { android.media.RingtoneManager.getRingtone(getApplication(), android.net.Uri.parse(uri))?.getTitle(getApplication()) } catch (_: Throwable) { null }
        com.example.utils.NotifPrefs.setTone(getApplication(), uri, title)
    }

    fun setSearchQuery(q: String) { _searchQuery.value = q }
    fun selectTag(tag: Tag?) { _selectedTag.value = tag }
    fun clearSelectedDhikrFromNotification() { _selectedDhikrFromNotification.value = null }

    fun selectDhikrFromNotification(id: Int) {
        viewModelScope.launch(Dispatchers.IO) { _selectedDhikrFromNotification.value = repository.getDhikrById(id) }
    }

    fun incrementTasbeeh() {
        val c = _tasbeehCount.value + 1
        _tasbeehCount.value = c
        tasbeehPrefs.edit().putInt("count", c).apply()
    }

    fun resetTasbeeh() {
        _tasbeehCount.value = 0
        tasbeehPrefs.edit().putInt("count", 0).apply()
    }

    fun markAsRead(id: Int) { viewModelScope.launch(Dispatchers.IO) { repository.markAsRead(id) } }
    fun addTag(name: String, colorHex: String, @Suppress("UNUSED_PARAMETER") extra: String? = null) { viewModelScope.launch(Dispatchers.IO) { repository.insertTag(Tag(name = name, colorHex = colorHex)) } }
    fun updateTag(tag: Tag) { viewModelScope.launch(Dispatchers.IO) { repository.updateTag(tag) } }
    fun deleteTag(tag: Tag) { viewModelScope.launch(Dispatchers.IO) { repository.deleteTag(tag) } }

    fun toggleFavorite(d: Dhikr) { viewModelScope.launch(Dispatchers.IO) { repository.update(d.copy(isFavorite = !d.isFavorite)) } }
    fun deleteDhikr(d: Dhikr) { viewModelScope.launch(Dispatchers.IO) { scheduler.cancel(d); repository.delete(d.id) } }
    fun updateTimes(d: Dhikr, times: List<String>) = updateReminderTimes(d, times)
    fun updateDhikrWithTags(d: Dhikr, title: String, content: String, tagIds: List<Long>) {
        viewModelScope.launch(Dispatchers.IO) {
            val u = d.copy(title = title, content = content)
            repository.update(u); repository.updateDhikrTags(u.id, tagIds)
        }
    }
    fun setSelectedTag(tag: Tag?) = selectTag(tag)
    fun updateVibrationEnabled(v: Boolean) { _vibrationEnabled.value = v; com.example.utils.NotifPrefs.setVibration(getApplication(), v) }
    fun updateAppFont(f: String) { _appFont.value = f; appPrefs.edit().putString("app_font", f).apply() }
}
