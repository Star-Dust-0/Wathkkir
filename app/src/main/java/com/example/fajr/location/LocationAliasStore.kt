package com.example.fajr.location

import android.content.Context
import org.json.JSONObject

/**
 * Per-location custom city-name aliases (the app's "memory" for user-renamed places).
 * Aliases are keyed by a stable structural place identity (see LocationManagerHelper#getPlaceKeyFromCoordinates
 * for GPS mode, or an exact-coordinate "m|lat|lon" key for manually picked cities) - never by the
 * mutable display string, so normal GPS refreshes keep working and the alias resurfaces on return.
 */
object LocationAliasStore {
    private const val PREFS = "fajr_prefs"
    private const val KEY = "city_aliases_json"

    private fun prefs(ctx: Context) = ctx.applicationContext.getSharedPreferences(PREFS, Context.MODE_PRIVATE)

    private fun table(ctx: Context): JSONObject {
        val raw = prefs(ctx).getString(KEY, null) ?: return JSONObject()
        return try { JSONObject(raw) } catch (_: Exception) { JSONObject() }
    }

    fun aliasFor(ctx: Context, key: String): String? {
        val v = table(ctx).optString(key, "")
        return v.ifEmpty { null }
    }

    fun put(ctx: Context, key: String, alias: String) {
        val obj = table(ctx)
        obj.put(key, alias)
        prefs(ctx).edit().putString(KEY, obj.toString()).apply()
    }

    fun remove(ctx: Context, key: String) {
        val obj = table(ctx)
        obj.remove(key)
        prefs(ctx).edit().putString(KEY, obj.toString()).apply()
    }
}
