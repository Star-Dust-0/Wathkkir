package com.example.fajr.ui

import android.app.Application
import android.net.Uri
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fajr.alarm.FajrAlarmScheduler
import com.example.fajr.calculation.CalculationMethod
import com.example.fajr.calculation.PrayerAdjustments
import com.example.fajr.data.CityItem
import com.example.fajr.data.FajrPreferences
import com.example.fajr.location.LocationAliasStore
import com.example.fajr.location.LocationManagerHelper
import com.example.fajr.sound.RingtoneHelper
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.*
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

// Secondary reconstruction: historical Oct17-23 method bodies, original APK i40/h40/g40.
data class FajrSettingsState(
    val isAlarmEnabled: Boolean = false, val useCurrentLocation: Boolean = true,
    val cityName: String = "القاهرة، مصر", val latitude: Double = 30.0444, val longitude: Double = 31.2357,
    val nextFajrTimeFormatted: String = "--:--", val nextFajrMillis: Long = 0,
    val calculationMethod: CalculationMethod = CalculationMethod.EGYPTIAN,
    val ringtoneTitle: String = "نغمة المنبه الافتراضية", val isVibrationEnabled: Boolean = true,
    val maxDurationMinutes: Int = 5, val alarmVolume: Int = 80, val isRefreshing: Boolean = false,
    val isGpsAvailable: Boolean = true, val hasLocationPermission: Boolean = false
)

class FajrViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = FajrPreferences(application)
    private val scheduler = FajrAlarmScheduler(application)
    private val locationHelper = LocationManagerHelper(application)
    private val ringtoneHelper = RingtoneHelper(application)
    private val _uiState = MutableStateFlow(FajrSettingsState())
    val uiState = _uiState.asStateFlow()
    private val _aliasVersion = MutableStateFlow(0)
    val aliasVersion = _aliasVersion.asStateFlow()
    init { loadSettings() }
    fun loadSettings() {
        val method = CalculationMethod.INSTANCE.fromId(prefs.calculationMethodId)
        val uri = ringtoneHelper.getValidRingtoneUri(prefs)
        _uiState.value = FajrSettingsState(isAlarmEnabled = prefs.isAlarmEnabled, useCurrentLocation = prefs.useCurrentLocation,
            cityName = prefs.getCityNameLocalized(L10n.lang.code), latitude = prefs.latitude, longitude = prefs.longitude,
            calculationMethod = method, ringtoneTitle = ringtoneHelper.getRingtoneTitle(uri), isVibrationEnabled = prefs.isVibrationEnabled,
            maxDurationMinutes = prefs.maxDurationMinutes, alarmVolume = prefs.alarmVolume,
            isGpsAvailable = locationHelper.isGpsEnabled(), hasLocationPermission = locationHelper.hasLocationPermission())
        refreshFajrTimeInternal()
    }
    fun refreshCityDisplay() { _uiState.update { it.copy(cityName = prefs.getCityNameLocalized(L10n.lang.code)) } }
    fun isCurrentPlaceAliased(): Boolean = prefs.placeKey?.let { LocationAliasStore.aliasFor(getApplication(), it) != null } ?: false
    fun setCityAlias(alias: String?): Boolean {
        val trimmed = alias?.trim().orEmpty(); val key = prefs.placeKey ?: return false
        if (trimmed.isEmpty()) return false
        if (prefs.cityNameArOrig == null) prefs.setCityNamesOrig(prefs.getCityNameLocalized("ar"), prefs.getCityNameLocalized("en"))
        LocationAliasStore.put(getApplication(), key, trimmed); prefs.setCityNames(trimmed, trimmed)
        refreshCityDisplay(); _aliasVersion.value++; return true
    }
    fun clearCityAlias() {
        prefs.placeKey?.let { LocationAliasStore.remove(getApplication(), it) }
        val ar = prefs.cityNameArOrig
        if (ar != null) { prefs.setCityNames(ar, prefs.cityNameEnOrig ?: ar); prefs.clearCityNamesOrig(); refreshCityDisplay() }
        else if (prefs.useCurrentLocation) updateLocationFromGps()
        _aliasVersion.value++
    }
    fun toggleAlarm(enabled: Boolean) {
        prefs.setAlarmEnabled(enabled); _uiState.update { it.copy(isAlarmEnabled = enabled) }
        if (enabled) scheduler.scheduleNextFajrAlarm() else scheduler.cancelAllAlarms()
        refreshFajrTimeInternal()
    }
    fun setUseCurrentLocation(useGps: Boolean) {
        prefs.useCurrentLocation = useGps; _uiState.update { it.copy(useCurrentLocation = useGps) }
        if (useGps) updateLocationFromGps() else refreshFajrTimeInternal()
    }
    fun updateLocationFromGps() { viewModelScope.launch(Dispatchers.IO) {
        _uiState.update { it.copy(isRefreshing = true) }
        val permitted = locationHelper.hasLocationPermission(); val gps = locationHelper.isGpsEnabled()
        _uiState.update { it.copy(isGpsAvailable = gps, hasLocationPermission = permitted) }
        if (permitted && gps) {
            val loc = locationHelper.getLastKnownLocation()
            if (loc != null) {
                prefs.latitude = loc.latitude; prefs.longitude = loc.longitude
                var ar = locationHelper.getCityNameFromCoordinates(loc.latitude, loc.longitude, Locale("ar"))
                var en = locationHelper.getCityNameFromCoordinates(loc.latitude, loc.longitude, Locale.ENGLISH)
                val key = locationHelper.getPlaceKeyFromCoordinates(loc.latitude, loc.longitude)
                prefs.placeKey = key
                if (key != null) LocationAliasStore.aliasFor(getApplication(), key)?.let { ar = it; en = it }
                prefs.setCityNames(ar, en)
                _uiState.update { it.copy(cityName = if (L10n.lang.code == "ar") ar else en, latitude = loc.latitude,
                    longitude = loc.longitude, isGpsAvailable = true, hasLocationPermission = true) }
            }
        }
        refreshFajrTimeInternal(); _uiState.update { it.copy(isRefreshing = false) }
    } }
    fun setCity(city: CityItem) {
        prefs.useCurrentLocation = false; prefs.latitude = city.latitude; prefs.longitude = city.longitude
        var ar = city.cityNameAr + "، " + city.countryNameAr; var en = city.cityNameEn + ", " + city.countryNameEn
        val key = String.format(Locale.US, "m|%.4f|%.4f", city.latitude, city.longitude); prefs.placeKey = key
        LocationAliasStore.aliasFor(getApplication(), key)?.let { ar = it; en = it }
        prefs.setCityNames(ar, en); prefs.countryName = city.countryNameAr
        _uiState.update { it.copy(useCurrentLocation = false, cityName = if (L10n.lang.code == "ar") ar else en, latitude = city.latitude, longitude = city.longitude) }
        refreshFajrTimeInternal(); if (prefs.isAlarmEnabled) scheduler.scheduleNextFajrAlarm()
    }
    fun setCalculationMethod(method: CalculationMethod) {
        prefs.calculationMethodId = method.id; _uiState.update { it.copy(calculationMethod = method) }
        refreshFajrTimeInternal(); if (prefs.isAlarmEnabled) scheduler.scheduleNextFajrAlarm()
    }
    fun setRingtone(uri: Uri, title: String) { prefs.ringtoneUri = uri.toString(); prefs.ringtoneTitle = title; _uiState.update { it.copy(ringtoneTitle = title) } }
    fun toggleVibration(enabled: Boolean) { prefs.setVibrationEnabled(enabled); _uiState.update { it.copy(isVibrationEnabled = enabled) } }
    fun setMaxDuration(minutes: Int) { prefs.maxDurationMinutes = minutes; _uiState.update { it.copy(maxDurationMinutes = minutes) } }
    fun setAlarmVolume(volume: Int) { prefs.alarmVolume = volume; _uiState.update { it.copy(alarmVolume = volume) } }
    fun refreshFajrTimeManually() { viewModelScope.launch(Dispatchers.IO) {
        _uiState.update { it.copy(isRefreshing = true) }
        if (prefs.useCurrentLocation) updateLocationFromGps() else refreshFajrTimeInternal()
        if (prefs.isAlarmEnabled) scheduler.scheduleNextFajrAlarm()
        _uiState.update { it.copy(isRefreshing = false) }
    } }
    private fun refreshFajrTimeInternal() {
        val state = _uiState.value; val calendar = Calendar.getInstance()
        var time = PrayerAdjustments.fajrTime(getApplication(), calendar, state.latitude, state.longitude, state.calculationMethod)
        val tomorrow = time <= System.currentTimeMillis()
        if (tomorrow) { calendar.add(Calendar.DAY_OF_YEAR, 1); time = PrayerAdjustments.fajrTime(getApplication(), calendar, state.latitude, state.longitude, state.calculationMethod) }
        val fmt = Calendar.getInstance().apply { timeInMillis = time }
        val hm = SimpleDateFormat("hh:mm", Locale.US).format(fmt.time)
        val marker = L10n.t(if (fmt.get(Calendar.AM_PM) == 0) S.AmMarker else S.PmMarker)
        val prefix = L10n.t(if (tomorrow) S.TomorrowAt else S.TodayAt)
        _uiState.update { it.copy(nextFajrTimeFormatted = "$prefix $hm $marker", nextFajrMillis = time) }
    }
    fun playRingtonePreview(uri: Uri) { ringtoneHelper.playPreview(uri, _uiState.value.alarmVolume / 100f) }
    fun stopRingtonePreview() { ringtoneHelper.stopPreview() }
}
