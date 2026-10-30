package com.example.data

import android.content.Context
import org.json.JSONObject

/** Seeds the Hisn al-Muslim tables from the bundled hisn_elmoslem.json on first run. */
object HisnImporter {
    suspend fun ensureImported(context: Context, dao: HisnDao) {
        if (dao.chapterCount() == 0) importAll(context, dao)
        seedDefaultFavorites(context, dao)
    }

    /** 3.3.0: morning (29), evening (30), sleep (31) and travel (96) adhkar are favorites by default.
     *  Additive and once only: existing favorites are never removed, and a favorite the user removes later stays removed. */
    private suspend fun seedDefaultFavorites(context: Context, dao: HisnDao) {
        val prefs = context.getSharedPreferences("hisn_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("defaults_v330", false)) return
        for (id in intArrayOf(29, 30, 31, 96)) {
            try { dao.addFavorite(HisnFavorite(0L, id, 0)) } catch (e: Exception) { e.printStackTrace() }
        }
        prefs.edit().putBoolean("defaults_v330", true).apply()
    }

    private suspend fun importAll(context: Context, dao: HisnDao) {

        val text = context.assets.open("hisn_elmoslem.json").bufferedReader().use { it.readText() }
        val root = JSONObject(text)
        val chapterArr = root.getJSONArray("chapters")
        val itemArr = root.getJSONArray("items")

        val chapters = (0 until chapterArr.length()).map { i ->
            val o = chapterArr.getJSONObject(i)
            HisnChapter(o.getInt("id"), o.getInt("order"), o.getString("name"))
        }
        dao.insertChapters(chapters)

        val items = (0 until itemArr.length()).map { i ->
            val o = itemArr.getJSONObject(i)
            HisnItem(
                o.getInt("id"),
                o.getInt("chapterId"),
                o.getInt("order"),
                o.getString("text"),
                o.optInt("count", 1).coerceAtLeast(1),
                o.optString("fadl", ""),
                o.optString("source", ""),
                o.optString("hokm", "")
            )
        }
        dao.insertItems(items)
    }
}
