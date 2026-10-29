package com.example.fajr.ui

import android.app.Application
import android.location.Location
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.provider.Settings
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.fajr.alarm.FajrAlarmScheduler
import com.example.fajr.calculation.CalculationMethod
import com.example.fajr.calculation.PrayerAdjustments
import com.example.fajr.data.City
import com.example.fajr.data.FajrPreferences
import com.example.fajr.util.DeviceHelper
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import org.json.JSONObject
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Locale

/** State of the Fajr settings (original h40, "FajrSettingsState"). */
data class FajrSettingsState(
    val isAlarmEnabled: Boolean = false,
    val useCurrentLocation: Boolean = true,
    val cityName: String = "القاهرة، مصر",
    val latitude: Double = 30.0444,
    val longitude: Double = 31.2357,
    val nextFajrTimeFormatted: String = "--:--",
    val nextFajrMillis: Long = 0L,
    val calculationMethod: CalculationMethod = CalculationMethod.EGYPTIAN,
    val ringtoneTitle: String = "نغمة المنبه الافتراضية",
    val isVibrationEnabled: Boolean = true,
    val maxDurationMinutes: Int = 5,
    val alarmVolume: Int = 80,
    val isRefreshing: Boolean = false,
    val isGpsAvailable: Boolean = true,
    val hasLocationPermission: Boolean = false
)

/** Rebuilt from the original 3.4.2 class i40 ("FajrViewModel"). */
class FajrViewModel(application: Application) : AndroidViewModel(application) {
    private val prefs = FajrPreferences(application)
    private val scheduler = FajrAlarmScheduler(application)
    private val device = DeviceHelper(application)
    private val _uiState = MutableStateFlow(FajrSettingsState())
    val uiState: StateFlow<FajrSettingsState> = _uiState.asStateFlow()

    init { loadSettings() }

    private fun app(): Application = getApplication()

    /** Reads everything from preferences (original e). */
    fun loadSettings() {
        val method = CalculationMethod.INSTANCE.fromId(prefs.calculationMethodId)
        val uriStr = prefs.ringtoneUri
        val title: String = if (uriStr.isNotEmpty()) {
            val u = Uri.parse(uriStr)
            try { app().contentResolver.openFileDescriptor(u, "r")?.close() } catch (_: Exception) {}
            device.ringtoneTitle(u)
        } else {
            val u = RingtoneManager.getDefaultUri(4) ?: RingtoneManager.getDefaultUri(1) ?: Settings.System.DEFAULT_ALARM_ALERT_URI
            device.ringtoneTitle(u)
        }
        _uiState.value = FajrSettingsState(
            isAlarmEnabled = prefs.isAlarmEnabled,
            useCurrentLocation = prefs.useCurrentLocation,
            cityName = prefs.getCityNameLocalized(L10n.lang.code),
            latitude = prefs.latitude,
            longitude = prefs.longitude,
            calculationMethod = method,
            ringtoneTitle = title,
            isVibrationEnabled = prefs.isVibrationEnabled,
            maxDurationMinutes = prefs.maxDurationMinutes,
            alarmVolume = prefs.alarmVolume,
            isGpsAvailable = device.isGpsAvailable(),
            hasLocationPermission = device.hasLocationPermission()
        )
        refreshFajrTime()
    }

    /** Re-reads the localized city name (after a language change; original f). */
    fun refreshCityDisplay() {
        _uiState.value = _uiState.value.copy(cityName = prefs.getCityNameLocalized(L10n.lang.code))
    }

    /** Computes the next Fajr time and its label "Today/Tomorrow hh:mm AM/PM" (original g). */
    fun refreshFajrTime() {
        val now = System.currentTimeMillis()
        val cal = Calendar.getInstance()
        val st = _uiState.value
        var tomorrow = false
        var fajr = PrayerAdjustments.INSTANCE.fajrTime(app(), cal, st.latitude, st.longitude, st.calculationMethod)
        if (fajr <= now) {
            tomorrow = true
            cal.add(Calendar.DAY_OF_YEAR, 1)
            fajr = PrayerAdjustments.INSTANCE.fajrTime(app(), cal, st.latitude, st.longitude, st.calculationMethod)
        }
        val c2 = Calendar.getInstance().apply { timeInMillis = fajr }
        val hhmm = SimpleDateFormat("hh:mm", Locale.US).format(c2.time)
        val marker = L10n.t(if (c2.get(Calendar.AM_PM) == 0) S.AmMarker else S.PmMarker)
        val day = L10n.t(if (tomorrow) S.TomorrowAt else S.TodayAt)
        _uiState.value = _uiState.value.copy(nextFajrTimeFormatted = "$day $hhmm $marker", nextFajrMillis = fajr)
    }

    fun setCalculationMethod(method: CalculationMethod) {
        prefs.calculationMethodId = method.id
        _uiState.value = _uiState.value.copy(calculationMethod = method)
        refreshFajrTime()
        if (prefs.isAlarmEnabled) scheduler.rescheduleFromPrefs()
    }

    fun setCity(city: City) {
        prefs.useCurrentLocation = false
        prefs.latitude = city.latitude
        prefs.longitude = city.longitude
        var ar = city.nameAr + "، " + city.countryAr
        var en = city.nameEn + ", " + city.countryEn
        val key = String.format(Locale.US, "m|%.4f|%.4f", city.latitude, city.longitude)
        prefs.placeKey = key
        cityAlias(key)?.let { ar = it; en = it }
        prefs.setCityNames(ar, en)
        prefs.countryName = city.countryAr
        _uiState.value = _uiState.value.copy(
            cityName = if ("ar" == L10n.lang.code) ar else en,
            latitude = city.latitude, longitude = city.longitude
        )
        refreshFajrTime()
        if (prefs.isAlarmEnabled) scheduler.rescheduleFromPrefs()
    }

    private fun cityAlias(key: String): String? {
        val json = app().applicationContext.getSharedPreferences("fajr_prefs", 0).getString("city_aliases_json", null)
        val o = try { if (json == null) JSONObject() else JSONObject(json) } catch (_: Exception) { JSONObject() }
        return o.optString(key, "").ifEmpty { null }
    }

    fun setRingtone(uri: Uri, title: String) {
        prefs.ringtoneUri = uri.toString()
        prefs.ringtoneTitle = title
        _uiState.value = _uiState.value.copy(ringtoneTitle = title)
    }

    fun setUseCurrentLocation(value: Boolean) {
        prefs.useCurrentLocation = value
        _uiState.value = _uiState.value.copy(useCurrentLocation = value)
        if (value) updateLocationFromGps() else refreshFajrTime()
    }

    fun toggleAlarm(enabled: Boolean) {
        prefs.isAlarmEnabled = enabled
        _uiState.value = _uiState.value.copy(isAlarmEnabled = enabled)
        if (enabled) scheduler.rescheduleFromPrefs() else scheduler.cancelAll()
        refreshFajrTime()
    }

    fun toggleVibration(enabled: Boolean) {
        prefs.isVibrationEnabled = enabled
        _uiState.value = _uiState.value.copy(isVibrationEnabled = enabled)
    }

    /** Takes the last known GPS/network/passive fix and stores it as the current place (original g40 case 2). */
    @Suppress("MissingPermission")
    fun updateLocationFromGps() {
        viewModelScope.launch(Dispatchers.IO) {
            _uiState.value = _uiState.value.copy(isRefreshing = true)
            val perm = device.hasLocationPermission()
            val gps = device.isGpsAvailable()
            if (perm && gps) {
                var best: Location? = null
                try {
                    val lm = app().getSystemService(android.content.Context.LOCATION_SERVICE) as LocationManager
                    for (p in listOf("gps", "network", "passive")) {
                        val l = try { lm.getLastKnownLocation(p) } catch (_: Throwable) { null }
                        if (l != null && (best == null || l.time > best!!.time)) best = l
                    }
                } catch (t: Throwable) { t.printStackTrace() }
                val loc = best
                if (loc != null) {
                    prefs.latitude = loc.latitude
                    prefs.longitude = loc.longitude
                    var ar = device.placeName(loc.latitude, loc.longitude, Locale("ar"))
                    var en = device.placeName(loc.latitude, loc.longitude, Locale.ENGLISH)
                    val key = device.placeKey(loc.latitude, loc.longitude)
                    if (key != null) {
                        prefs.placeKey = key
                        cityAlias(key)?.let { ar = it; en = it }
                    } else prefs.placeKey = ""
                    prefs.setCityNames(ar, en)
                    _uiState.value = _uiState.value.copy(
                        cityName = if ("ar" == L10n.lang.code) ar else en,
                        latitude = loc.latitude, longitude = loc.longitude
                    )
                }
            }
            _uiState.value = _uiState.value.copy(isGpsAvailable = gps, hasLocationPermission = perm)
            refreshFajrTime()
            _uiState.value = _uiState.value.copy(isRefreshing = false)
        }
    }

    private var previewPlayer: android.media.MediaPlayer? = null
    fun refreshFajrTimeManually() = refreshFajrTime()
    fun setAlarmVolume(v: Int) {
        prefs.alarmVolume = v
        _uiState.value = _uiState.value.copy(alarmVolume = v)
    }
    fun setMaxDuration(m: Int) {
        prefs.maxDurationMinutes = m
        _uiState.value = _uiState.value.copy(maxDurationMinutes = m)
    }
    fun playRingtonePreview(uri: Uri) {
        stopRingtonePreview()
        try {
            previewPlayer = android.media.MediaPlayer().apply {
                setDataSource(app(), uri)
                setAudioAttributes(android.media.AudioAttributes.Builder().setUsage(android.media.AudioAttributes.USAGE_ALARM).build())
                prepare(); start()
            }
        } catch (t: Throwable) { t.printStackTrace() }
    }
    fun stopRingtonePreview() {
        try { previewPlayer?.stop(); previewPlayer?.release() } catch (_: Throwable) {}
        previewPlayer = null
    }
    override fun onCleared() { stopRingtonePreview() }
}
