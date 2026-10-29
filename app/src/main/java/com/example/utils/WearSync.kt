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

    internal fun payload(app: Context): com.google.android.gms.wearable.DataMap {
        val p = FajrPreferences(app)
        return com.google.android.gms.wearable.DataMap().apply {
            putDouble("lat", p.latitude)
            putDouble("lon", p.longitude)
            putString("city", p.cityName)
            putBoolean("h24", is24h(app))
            putString("lang", app.getSharedPreferences("app_prefs", Context.MODE_PRIVATE).getString("language", "ar") ?: "ar")
            putLong("ts", System.currentTimeMillis())
        }
    }

    internal fun signature(data: com.google.android.gms.wearable.DataMap): String =
        "${data.getDouble("lat")}|${data.getDouble("lon")}|${data.getString("city")}|${data.getBoolean("h24")}|${data.getString("lang")}"

    fun push(context: Context, onDone: ((Boolean) -> Unit)? = null) {
        try {
            val app = context.applicationContext
            val data = payload(app)
            val sig = signature(data)
            val prefs = app.getSharedPreferences(PREFS, Context.MODE_PRIVATE)
            if (prefs.getString("last_sig", null) == sig) { onDone?.invoke(true); return }
            val req = PutDataMapRequest.create("/wathakker/location")
            req.dataMap.putAll(data)
            Wearable.getDataClient(app).putDataItem(req.asPutDataRequest().setUrgent())
                .addOnSuccessListener {
                    app.getSharedPreferences(PREFS, Context.MODE_PRIVATE).edit().putLong("last_sync", System.currentTimeMillis()).putString("last_sig", sig).apply()
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
