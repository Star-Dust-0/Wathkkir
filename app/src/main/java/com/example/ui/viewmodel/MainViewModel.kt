package com.example.ui.viewmodel

import android.app.Application
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.alarm.AlarmScheduler
import com.example.data.*
import com.example.ui.theme.FontChoice
import com.example.utils.NotifPrefs
import com.example.utils.WearSync
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Secondary reconstruction of uncovered base: historical MainViewModel Java fragments
// and original APK mu0, ku0, iu0, ju0, lu0, f7, g7, sa, ms. Not the newer rebuilt tree.
data class DhikrStats(
    val todayCount: Int = 0, val weekCount: Int = 0, val monthCount: Int = 0,
    val totalCount: Int = 0, val streakDays: Int = 0,
    val topReadDhikr: List<Pair<Int, Int>> = emptyList()
)

class MainViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = application.getSharedPreferences("tasbeeh_prefs", 0)
    private val appPrefs = application.getSharedPreferences("app_prefs", 0)
    private val db = AppDatabase.getDatabase(application)
    private val repository = DhikrRepository(db.dhikrDao(), db.tagDao(), db.historyDao())
    private val alarmScheduler = AlarmScheduler(application)
    init { NotifPrefs.migrate(application) }
    private val _language = MutableStateFlow(appPrefs.getString("language", "ar") ?: "ar")
    val language = _language.asStateFlow()
    private val _vibrationEnabled = MutableStateFlow(appPrefs.getBoolean("vibration_enabled", true))
    val vibrationEnabled = _vibrationEnabled.asStateFlow()
    private val _useDeviceFont = MutableStateFlow(appPrefs.getBoolean("use_device_font", true))
    val useDeviceFont = _useDeviceFont.asStateFlow()
    private val _appFont = MutableStateFlow(FontChoice.fromId(appPrefs.getString("app_font", null)
        ?: if (appPrefs.contains("use_device_font") && appPrefs.getBoolean("use_device_font", false)) "system" else "app"))
    val appFont = _appFont.asStateFlow()
    private val _notificationSound = MutableStateFlow(appPrefs.getString("notification_sound", "default") ?: "default")
    val notificationSound = _notificationSound.asStateFlow()
    private val _searchQuery = MutableStateFlow("")
    val searchQuery = _searchQuery.asStateFlow()
    private val _selectedTag = MutableStateFlow<Tag?>(null)
    val selectedTag = _selectedTag.asStateFlow()
    private val _tasbeehCount = MutableStateFlow(prefs.getInt("count", 0))
    val tasbeehCount = _tasbeehCount.asStateFlow()
    private val _selectedDhikrFromNotification = MutableStateFlow<Dhikr?>(null)
    val selectedDhikrFromNotification = _selectedDhikrFromNotification.asStateFlow()
    private val sharing = SharingStarted.WhileSubscribed(5000)
    val allHistory = repository.allHistory.stateIn(viewModelScope, sharing, emptyList())
    val stats = repository.allHistory.map(::calculateStats).stateIn(viewModelScope, sharing, DhikrStats())
    val allDhikr = repository.allDhikr.stateIn(viewModelScope, sharing, emptyList())
    val favoriteDhikr = repository.favoriteDhikr.stateIn(viewModelScope, sharing, emptyList())
    val enabledDhikr = repository.enabledDhikr.stateIn(viewModelScope, sharing, emptyList())
    val allTags = repository.allTags.stateIn(viewModelScope, sharing, emptyList())
    val dhikrsWithTags = repository.dhikrsWithTags.stateIn(viewModelScope, sharing, emptyList())

    fun updateAppFont(choice: FontChoice) { _appFont.value = choice; appPrefs.edit().putString("app_font", choice.id).apply() }
    fun updateLanguage(lang: String) {
        _language.value = lang; appPrefs.edit().putString("language", lang).apply()
        WearSync.push(getApplication())
    }
    fun updateVibrationEnabled(enabled: Boolean) { _vibrationEnabled.value = enabled; NotifPrefs.setVibration(getApplication(), enabled) }
    fun updateUseDeviceFont(use: Boolean) { _useDeviceFont.value = use; appPrefs.edit().putBoolean("use_device_font", use).apply() }
    fun updateNotificationSound(sound: String) {
        _notificationSound.value = sound
        NotifPrefs.setTone(getApplication(), sound, NotifPrefs.titleFor(getApplication(), sound))
    }
    fun setSearchQuery(query: String) { _searchQuery.value = query }
    fun setSelectedTag(tag: Tag?) { _selectedTag.value = tag }
    fun toggleFavorite(dhikr: Dhikr) { viewModelScope.launch { repository.update(dhikr.copy(isFavorite = !dhikr.isFavorite)) } }
    fun toggleEnabled(dhikr: Dhikr) { viewModelScope.launch {
        val enabled = !dhikr.isEnabled
        repository.update(dhikr.copy(isEnabled = enabled))
        if (enabled) alarmScheduler.schedule(dhikr) else alarmScheduler.cancel(dhikr)
    } }
    fun updateTimes(dhikr: Dhikr, newTimes: List<String>) { viewModelScope.launch {
        val updated = dhikr.copy(reminderTimes = newTimes, isEnabled = true)
        repository.update(updated); alarmScheduler.schedule(updated)
    } }
    fun addDhikrWithSchedule(title: String, content: String, times: List<String>) { viewModelScope.launch {
        val dhikr = Dhikr(title = title, content = content, reminderTimes = times, isEnabled = true)
        val id = repository.insert(dhikr).toInt(); alarmScheduler.schedule(dhikr.copy(id = id))
    } }
    fun addDhikrWithScheduleAndTags(title: String, content: String, times: List<String>, tagIds: List<Long>) { viewModelScope.launch {
        val dhikr = Dhikr(title = title, content = content, reminderTimes = times, isEnabled = true)
        val id = repository.insert(dhikr).toInt(); alarmScheduler.schedule(dhikr.copy(id = id))
        if (tagIds.isNotEmpty()) repository.updateDhikrTags(id, tagIds)
    } }
    fun updateDhikrWithTags(dhikr: Dhikr, newTitle: String, newContent: String, tagIds: List<Long>) { viewModelScope.launch {
        val updated = dhikr.copy(title = newTitle, content = newContent)
        repository.update(updated); repository.updateDhikrTags(dhikr.id, tagIds)
        if (updated.isEnabled) alarmScheduler.schedule(updated)
    } }
    fun updateDhikrText(dhikr: Dhikr, newTitle: String, newContent: String) { viewModelScope.launch {
        val updated = dhikr.copy(title = newTitle, content = newContent)
        repository.update(updated); if (updated.isEnabled) alarmScheduler.schedule(updated)
    } }
    fun updateDhikrTags(dhikrId: Int, tagIds: List<Long>) { viewModelScope.launch { repository.updateDhikrTags(dhikrId, tagIds) } }
    fun addTag(name: String, colorHex: String = "#008080", onCreated: ((Tag?) -> Unit)? = null) { viewModelScope.launch {
        val tag = repository.getOrCreateTagByName(name, colorHex); onCreated?.invoke(tag)
    } }
    fun updateTag(tag: Tag) { viewModelScope.launch { repository.updateTag(tag) } }
    fun deleteTag(tag: Tag) { viewModelScope.launch {
        if (_selectedTag.value?.tagId == tag.tagId) _selectedTag.value = null
        repository.deleteTag(tag)
    } }
    fun deleteDhikr(dhikr: Dhikr) { viewModelScope.launch { repository.delete(dhikr.id); alarmScheduler.cancel(dhikr) } }
    fun incrementTasbeeh() { val n = _tasbeehCount.value + 1; _tasbeehCount.value = n; prefs.edit().putInt("count", n).apply() }
    fun resetTasbeeh() { _tasbeehCount.value = 0; prefs.edit().putInt("count", 0).apply() }
    fun onDhikrSelectedFromNotification(dhikrId: Int) { viewModelScope.launch { _selectedDhikrFromNotification.value = repository.getDhikrById(dhikrId) } }
    fun clearSelectedDhikrFromNotification() { _selectedDhikrFromNotification.value = null }
    fun markAsRead(dhikrId: Int) { viewModelScope.launch { repository.markAsRead(dhikrId) } }
    fun markAsRead(dhikr: Dhikr) = markAsRead(dhikr.id)

    private fun calculateStats(historyList: List<DhikrHistory>): DhikrStats {
        if (historyList.isEmpty()) return DhikrStats()
        val cal = Calendar.getInstance().apply { set(Calendar.HOUR_OF_DAY, 0); set(Calendar.MINUTE, 0); set(Calendar.SECOND, 0); set(Calendar.MILLISECOND, 0) }
        val today = cal.timeInMillis
        val week = (cal.clone() as Calendar).apply { add(Calendar.DAY_OF_YEAR, -6) }.timeInMillis
        val month = (cal.clone() as Calendar).apply { set(Calendar.DAY_OF_MONTH, 1) }.timeInMillis
        val counts = linkedMapOf<Int, Int>()
        val dates = linkedSetOf<String>()
        var tc = 0; var wc = 0; var mc = 0
        for (item in historyList) {
            if (item.timestamp >= today) tc++
            if (item.timestamp >= week) wc++
            if (item.timestamp >= month) mc++
            counts[item.dhikrId] = (counts[item.dhikrId] ?: 0) + 1
            dates.add(item.dateString)
        }
        val sdf = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        val check = cal.clone() as Calendar
        if (!dates.contains(sdf.format(check.time))) check.add(Calendar.DAY_OF_YEAR, -1)
        var streak = 0
        while (dates.contains(sdf.format(check.time))) { streak++; check.add(Calendar.DAY_OF_YEAR, -1) }
        return DhikrStats(tc, wc, mc, historyList.size, streak, counts.toList().sortedByDescending { it.second })
    }
}
