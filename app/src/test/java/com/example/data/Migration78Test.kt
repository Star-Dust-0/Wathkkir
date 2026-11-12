package com.example.data

import androidx.sqlite.db.SupportSQLiteDatabase
import androidx.sqlite.db.SupportSQLiteOpenHelper
import androidx.sqlite.db.framework.FrameworkSQLiteOpenHelperFactory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config

/**
 * Migration 7 -> 8 adds the Hisn el-Muslim tables. Runs against a real in-memory
 * SQLite under Robolectric: builds a minimal v7-shaped database, migrates, then
 * proves the new tables, their columns and the indexes exist and are usable.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class Migration78Test {

    private fun v7Database(): SupportSQLiteDatabase {
        val helper = FrameworkSQLiteOpenHelperFactory().create(
            SupportSQLiteOpenHelper.Configuration.builder(RuntimeEnvironment.getApplication())
                .name(null) // in-memory
                .callback(object : SupportSQLiteOpenHelper.Callback(7) {
                    override fun onConfigure(db: SupportSQLiteDatabase) {}
                    override fun onCreate(db: SupportSQLiteDatabase) {
                        // Minimal v7 shape: pre-Hisn tables only.
                        db.execSQL("CREATE TABLE IF NOT EXISTS `dhikr` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `title` TEXT NOT NULL)")
                    }
                    override fun onUpgrade(db: SupportSQLiteDatabase, oldVersion: Int, newVersion: Int) {}
                })
                .build()
        )
        return helper.writableDatabase
    }

    private fun exists(db: SupportSQLiteDatabase, type: String, name: String): Boolean =
        db.query("SELECT name FROM sqlite_master WHERE type=? AND name=?", arrayOf(type, name)).use {
            it.moveToFirst()
        }

    private fun columns(db: SupportSQLiteDatabase, table: String): Set<String> =
        db.query("PRAGMA table_info(`$table`)").use { c ->
            val out = mutableSetOf<String>()
            val idx = c.getColumnIndex("name")
            while (c.moveToNext()) out.add(c.getString(idx))
            out
        }

    @Test
    fun migrate78_createsHisnTables() {
        val db = v7Database()
        AppDatabase.MIGRATION_7_8.migrate(db)

        assertTrue(exists(db, "table", "hisn_chapter"))
        assertTrue(exists(db, "table", "hisn_item"))
        assertTrue(exists(db, "table", "hisn_favorite"))
        assertTrue(exists(db, "table", "hisn_read"))
        assertTrue(exists(db, "index", "index_hisn_item_chapterId"))
        assertTrue(exists(db, "index", "index_hisn_favorite_chapterId_itemId"))

        assertEquals(setOf("id", "order", "name"), columns(db, "hisn_chapter"))
        assertEquals(
            setOf("id", "chapterId", "order", "text", "count", "fadl", "source", "hokm"),
            columns(db, "hisn_item")
        )
        assertEquals(setOf("id", "chapterId", "itemId"), columns(db, "hisn_favorite"))
        assertEquals(setOf("id", "chapterId", "dateString", "timestamp"), columns(db, "hisn_read"))

        // Usable end to end: insert a favorite, unique index rejects the duplicate.
        db.execSQL("INSERT INTO hisn_favorite (chapterId, itemId) VALUES (1, 2)")
        var duplicateRejected = false
        try {
            db.execSQL("INSERT INTO hisn_favorite (chapterId, itemId) VALUES (1, 2)")
        } catch (e: android.database.SQLException) {
            duplicateRejected = true
        }
        assertTrue("unique favorite index must reject duplicates", duplicateRejected)

        // Migration is idempotent (IF NOT EXISTS): running it twice must not fail.
        AppDatabase.MIGRATION_7_8.migrate(db)
        db.close()
    }
}
