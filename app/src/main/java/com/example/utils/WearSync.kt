package com.example.utils

import android.content.Context
import com.example.fajr.data.FajrPreferences
import com.google.android.gms.wearable.PutDataMapRequest
import com.google.android.gms.wearable.Wearable

/** Sends the saved prayer location and watch display options to a paired watch (Data Layer item /wathakker/location). */
object WearSync {
    private const val PREFS = "wear_sync"

    fun is24h(c: Context): Boolean = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getBoolean("h24", false)
    fun setHour24(c: Context, v: Boolean) {
        c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putBoolean("h24", v).apply()
        push(c)
    }

    /** Time of the last successful hand-off to the Data Layer on this phone; 0 if never. */
    fun lastSyncMillis(c: Context): Long = c.getSharedPreferences(PREFS, Context.MODE_PRIVATE).getLong("last_sync", 0L)

    fun push(context: Context, onDone: ((Boolean) -> Unit)? = null) {
        try {
            val app = context.applicationContext
            val p = FajrPreferences(app)
            val lat = p.latitude
            val lon = p.longitude
            val city = p.cityName
            val h24 = is24h(app)
            val lang = app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("language", "ar") ?: "ar"
            // Skip the urgent Data Layer hand-off when nothing changed; the payload used to carry an
            // ever-changing timestamp, which forced an urgent sync on every app open.
            val signature = "$lat|$lon|$city|$h24|$lang"
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getString("last_sig", null) == signature) {
                onDone?.invoke(true)
                return
            }
            val req = PutDataMapRequest.create("/wathakker/location")
            req.dataMap.putDouble("lat", lat)
            req.dataMap.putDouble("lon", lon)
            req.dataMap.putString("city", city)
            req.dataMap.putBoolean("h24", h24)
            req.dataMap.putString("lang", lang)
            req.dataMap.putLong("ts", System.currentTimeMillis())
            Wearable.getDataClient(app).putDataItem(req.asPutDataRequest().setUrgent())
                .addOnSuccessListener {
                    prefs.edit().putLong("last_sync", System.currentTimeMillis()).putString("last_sig", signature).apply()
                    onDone?.invoke(true)
                }
                .addOnFailureListener { onDone?.invoke(false) }
        } catch (_: Throwable) {
            onDone?.invoke(false)
        }
    }

    /** Names of the currently connected watches (empty when none is connected). */
    fun connectedWatches(context: Context, onResult: (List<String>) -> Unit) {
        try {
            Wearable.getNodeClient(context.applicationContext).connectedNodes
                .addOnSuccessListener { nodes -> onResult(nodes.map { it.displayName }) }
                .addOnFailureListener { onResult(emptyList()) }
        } catch (_: Throwable) {
            onResult(emptyList())
        }
    }
}
