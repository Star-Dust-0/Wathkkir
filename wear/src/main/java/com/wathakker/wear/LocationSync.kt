package com.wathakker.wear

import android.content.Context
import com.google.android.gms.wearable.DataEvent
import com.google.android.gms.wearable.DataEventBuffer
import com.google.android.gms.wearable.DataMapItem
import com.google.android.gms.wearable.Wearable
import com.google.android.gms.wearable.WearableListenerService

object LocationSync {
    fun save(c: Context, lat: Double, lon: Double, city: String?, h24: Boolean = false, lang: String? = null) {
        if (lang != null) { c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).edit().putString("phone_lang", lang).apply(); Loc.refresh(c) }
        c.getSharedPreferences("wear_prefs", Context.MODE_PRIVATE).edit()
            .putLong("lat", java.lang.Double.doubleToLongBits(lat))
            .putLong("lon", java.lang.Double.doubleToLongBits(lon))
            .putString("city", city ?: "")
            .putBoolean("h24", h24)
            .apply()
        try { Adhan.schedule(c) } catch (_: Throwable) {}
        try { requestAllComplications(c) } catch (_: Throwable) {}
    }

    /** Reads the last item the phone sent (also works if the phone synced while the watch app was closed). */
    fun refresh(c: Context) {
        try {
            Wearable.getDataClient(c).dataItems.addOnSuccessListener { items ->
                try {
                    for (item in items) {
                        if (item.uri.path == "/wathakker/location") {
                            val m = DataMapItem.fromDataItem(item).dataMap
                            save(c, m.getDouble("lat"), m.getDouble("lon"), m.getString("city"), m.getBoolean("h24", false), m.getString("lang"))
                        }
                    }
                } finally { items.release() }
            }
        } catch (_: Throwable) {}
    }
}

class LocationListener : WearableListenerService() {
    override fun onDataChanged(events: DataEventBuffer) {
        for (e in events) {
            if (e.type == DataEvent.TYPE_CHANGED && e.dataItem.uri.path == "/wathakker/location") {
                val m = DataMapItem.fromDataItem(e.dataItem).dataMap
                LocationSync.save(this, m.getDouble("lat"), m.getDouble("lon"), m.getString("city"), m.getBoolean("h24", false), m.getString("lang"))
            }
        }
    }
}
