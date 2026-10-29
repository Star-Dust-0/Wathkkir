package com.example.fajr.util

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.location.Geocoder
import android.location.LocationManager
import android.media.RingtoneManager
import android.net.Uri
import com.example.ui.i18n.L10n
import com.example.ui.i18n.S
import java.util.Locale

/** Rebuilt from the original helper class v6: ringtone title, location permission and GPS state, reverse geocoding. */
class DeviceHelper(context: Context) {
    private val ctx: Context = context.applicationContext ?: context

    /** Localized current-place text, e.g. "Cairo, Egypt" (original v6.c). */
    fun placeName(lat: Double, lon: Double, locale: Locale): String {
        val ar = "ar" == locale.language
        val fallback = if (ar) "موقعك الحالي" else "Current location"
        return try {
            @Suppress("DEPRECATION")
            val list = Geocoder(ctx, locale).getFromLocation(lat, lon, 1)
            if (list.isNullOrEmpty()) return fallback + " (" + String.format(Locale.US, "%.2f, %.2f", lat, lon) + ")"
            val a = list[0]
            val city = a.locality ?: a.subAdminArea ?: a.adminArea
            val country = a.countryName
            if (!city.isNullOrEmpty() && !country.isNullOrEmpty()) {
                if (ar) "$city، $country" else "$city, $country"
            } else country ?: city ?: fallback
        } catch (e: Exception) {
            fallback + " (" + String.format(Locale.US, "%.2f, %.2f", lat, lon) + ")"
        }
    }

    /** Stable place key "g|country|city" from an English geocode (original v6.d). */
    fun placeKey(lat: Double, lon: Double): String? = try {
        @Suppress("DEPRECATION")
        val list = Geocoder(ctx, Locale.ENGLISH).getFromLocation(lat, lon, 1)
        if (list.isNullOrEmpty()) null else {
            val a = list[0]
            val city = a.locality ?: a.subAdminArea ?: a.adminArea
            val cc = a.countryCode
            if (cc == null && city == null) null
            else "g|" + (cc?.trim()?.lowercase(Locale.US)?.replace(Regex("\\s+"), " ") ?: "") + "|" +
                (city?.trim()?.lowercase(Locale.US)?.replace(Regex("\\s+"), " ") ?: "")
        }
    } catch (e: Exception) { null }

    /** Display name of a ringtone uri (original v6.e). */
    fun ringtoneTitle(uri: Uri): String {
        return try {
            ctx.contentResolver.query(uri, null, null, null, null)?.use { c ->
                if (c.moveToFirst()) {
                    val i = c.getColumnIndex("_display_name")
                    if (i != -1) c.getString(i)?.takeIf { it.isNotEmpty() }?.let { return it }
                }
            }
            val title = RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx)
            if (!title.isNullOrEmpty()) title else L10n.t(S.DefaultAlarmTone)
        } catch (e: Exception) { L10n.t(S.DefaultAlarmTone) }
    }

    fun hasLocationPermission(): Boolean =
        ctx.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION) == PackageManager.PERMISSION_GRANTED ||
            ctx.checkSelfPermission(Manifest.permission.ACCESS_COARSE_LOCATION) == PackageManager.PERMISSION_GRANTED

    fun isGpsAvailable(): Boolean {
        val lm = ctx.getSystemService(Context.LOCATION_SERVICE) as? LocationManager ?: return false
        return lm.isProviderEnabled(LocationManager.GPS_PROVIDER) || lm.isProviderEnabled(LocationManager.NETWORK_PROVIDER)
    }
}
