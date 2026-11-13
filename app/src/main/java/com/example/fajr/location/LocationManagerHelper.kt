package com.example.fajr.location

import android.content.Context
import android.location.Geocoder
import android.location.Location
import android.location.LocationManager
import android.util.Log
import androidx.core.content.ContextCompat
import java.util.Locale

class LocationManagerHelper(private val context: Context) {

    fun hasLocationPermission(): Boolean {
        val fine = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_FINE_LOCATION) == 0
        val coarse = ContextCompat.checkSelfPermission(context, android.Manifest.permission.ACCESS_COARSE_LOCATION) == 0
        return fine || coarse
    }

    fun isGpsEnabled(): Boolean {
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }

    fun getLastKnownLocation(): Location? {
        if (!hasLocationPermission()) return null
        val lm = context.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return null
        return try {
            var location: Location? = null
            if (lm.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                location = lm.getLastKnownLocation(LocationManager.GPS_PROVIDER)
            }
            if (location == null && lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)) {
                location = lm.getLastKnownLocation(LocationManager.NETWORK_PROVIDER)
            }
            if (location == null && lm.isProviderEnabled(LocationManager.PASSIVE_PROVIDER)) {
                return lm.getLastKnownLocation(LocationManager.PASSIVE_PROVIDER)
            }
            location
        } catch (e: SecurityException) {
            Log.w("LocationHelper", "location lookup failed", e)
            null
        }
    }

    fun getCityNameFromCoordinates(latitude: Double, longitude: Double): String =
        getCityNameFromCoordinates(latitude, longitude, Locale("ar"))

    fun getCityNameFromCoordinates(latitude: Double, longitude: Double, locale: Locale): String {
        val ar = locale.language == "ar"
        val unknown = if (ar) "موقعك الحالي" else "Current location"
        return try {
            val geocoder = Geocoder(context, locale)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (!addresses.isNullOrEmpty()) {
                val address = addresses[0]
                var city = address.locality
                if (city == null) city = address.subAdminArea
                if (city == null) city = address.adminArea
                val country = address.countryName
                if (!city.isNullOrEmpty() && !country.isNullOrEmpty()) {
                    return if (ar) "$city، $country" else "$city, $country"
                }
                if (country == null) {
                    return city ?: unknown
                }
                country
            } else {
                unknown + " (" + String.format(Locale.US, "%.2f, %.2f", latitude, longitude) + ")"
            }
        } catch (e: Exception) {
            Log.w("LocationHelper", "geocoder failed", e)
            unknown + " (" + String.format(Locale.US, "%.2f, %.2f", latitude, longitude) + ")"
        }
    }

    /** Identity of the geocoded place = EXACTLY the fields the display name is built from
     * (city = locality, else subAdminArea, else adminArea; country as locale-stable ISO code),
     * geocoded in English so the key survives UI-language switches. Null when the geocoder
     * cannot resolve - callers must skip alias save/apply in that case. */
    fun getPlaceKeyFromCoordinates(latitude: Double, longitude: Double): String? {
        return try {
            val geocoder = Geocoder(context, Locale.ENGLISH)
            val addresses = geocoder.getFromLocation(latitude, longitude, 1)
            if (addresses.isNullOrEmpty()) {
                null
            } else {
                val address = addresses[0]
                var city = address.locality
                if (city == null) city = address.subAdminArea
                if (city == null) city = address.adminArea
                val country = address.countryCode
                if (country == null && city == null) null else buildPlaceKey(country, city)
            }
        } catch (e: Exception) {
            Log.w("LocationHelper", "geocoder place key failed", e)
            null
        }
    }

    companion object {
        /** Production identity seam: same field selection and normalization as the display name. */
        @JvmStatic
        fun buildPlaceKey(countryCode: String?, cityField: String?): String =
            "g|" + normPlacePart(countryCode) + "|" + normPlacePart(cityField)

        private fun normPlacePart(s: String?): String =
            s?.trim()?.lowercase(Locale.US)?.replace(Regex("\\s+"), " ") ?: ""
    }
}
