package com.example.data

import android.content.Context
import org.json.JSONObject

object HisnImporter {
    /** Imports the bundled Hisn el-Muslim content the first time (or if the tables are empty). */
    suspend fun ensureImported(context: Context, dao: HisnDao) {
        if (dao.chapterCount() > 0) { seedDefaultFavorites(context, dao); return }
        val text = context.assets.open("hisn_elmoslem.json").bufferedReader(Charsets.UTF_8).use { it.readText() }
        val root = JSONObject(text)
        val chapters = root.getJSONArray("chapters")
        val items = root.getJSONArray("items")
        dao.insertChapters(List(chapters.length()) { i ->
            val o = chapters.getJSONObject(i)
            HisnChapter(o.getInt("id"), o.getInt("order"), o.getString("name"))
        })
        dao.insertItems(List(items.length()) { i ->
            val o = items.getJSONObject(i)
            HisnItem(
                id = o.getInt("id"),
                chapterId = o.getInt("chapterId"),
                order = o.getInt("order"),
                text = o.getString("text"),
                count = o.optInt("count", 1).coerceAtLeast(1),
                fadl = o.optString("fadl", ""),
                source = o.optString("source", ""),
                hokm = o.optString("hokm", "")
            )
        })
        seedDefaultFavorites(context, dao)
    }

    /** Original: first run seeds the four default favourite chapters (pref hisn_prefs/defaults_v330). */
    suspend fun seedDefaultFavorites(context: Context, dao: HisnDao) {
        val prefs = context.getSharedPreferences("hisn_prefs", Context.MODE_PRIVATE)
        if (prefs.getBoolean("defaults_v330", false)) return
        try {
            for (id in intArrayOf(29, 30, 31, 96)) dao.addFavorite(HisnFavorite(chapterId = id, itemId = 0))
        } catch (_: Exception) { return }
        prefs.edit().putBoolean("defaults_v330", true).apply()
    }
}
