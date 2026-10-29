package com.example.data

import android.content.Context
import androidx.room.Database
import androidx.room.Room
import androidx.room.RoomDatabase
import androidx.room.TypeConverters
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

@Database(
    entities = [
        Dhikr::class, Tag::class, DhikrTagCrossRef::class, DhikrHistory::class,
        HisnChapter::class, HisnItem::class, HisnFavorite::class, HisnRead::class
    ],
    version = 8,
    exportSchema = false
)
@TypeConverters(Converters::class)
abstract class AppDatabase : RoomDatabase() {
    abstract fun dhikrDao(): DhikrDao
    abstract fun hisnDao(): HisnDao
    abstract fun historyDao(): HistoryDao
    abstract fun tagDao(): TagDao

    companion object {
        @Volatile
        private var INSTANCE: AppDatabase? = null

        val MIGRATION_7_8 = object : Migration(7, 8) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("CREATE TABLE IF NOT EXISTS `hisn_chapter` (`id` INTEGER NOT NULL, `order` INTEGER NOT NULL, `name` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE TABLE IF NOT EXISTS `hisn_item` (`id` INTEGER NOT NULL, `chapterId` INTEGER NOT NULL, `order` INTEGER NOT NULL, `text` TEXT NOT NULL, `count` INTEGER NOT NULL, `fadl` TEXT NOT NULL, `source` TEXT NOT NULL, `hokm` TEXT NOT NULL, PRIMARY KEY(`id`))")
                db.execSQL("CREATE INDEX IF NOT EXISTS `index_hisn_item_chapterId` ON `hisn_item` (`chapterId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `hisn_favorite` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `chapterId` INTEGER NOT NULL, `itemId` INTEGER NOT NULL)")
                db.execSQL("CREATE UNIQUE INDEX IF NOT EXISTS `index_hisn_favorite_chapterId_itemId` ON `hisn_favorite` (`chapterId`, `itemId`)")
                db.execSQL("CREATE TABLE IF NOT EXISTS `hisn_read` (`id` INTEGER PRIMARY KEY AUTOINCREMENT NOT NULL, `chapterId` INTEGER NOT NULL, `dateString` TEXT NOT NULL, `timestamp` INTEGER NOT NULL)")
            }
        }

        private fun starter(title: String, content: String, time: String) =
            Dhikr(0, title, content, false, listOf(time), true, 0, 0L)

        private suspend fun seedDefaults(db: AppDatabase) {
            try {
                db.dhikrDao().insertAll(listOf(
            starter("سؤال العفو والعافية", "اللهم إنك عفو تحب العفو فاعف عني", "10:00"),
            starter("الثبات على الدين", "يا مقلب القلوب ثبت قلبي على دينك", "14:00"),
            starter("خير الدنيا والآخرة", "اللهم آتنا في الدنيا حسنة وفي الآخرة حسنة وقنا عذاب النار", "18:00"),
            starter("سؤال الهدى والتقى", "اللهم إني أسألك الهدى والتقى والعفاف والغنى", "08:00"),
            starter("الإعانة على الذكر", "اللهم أعني على ذكرك وشكرك وحسن عبادتك", "20:00")
                ))
                for (tag in listOf(
            Tag(name = "أذكار الصباح", colorHex = "#008080"),
            Tag(name = "أذكار المساء", colorHex = "#800080"),
            Tag(name = "أذكار النوم", colorHex = "#1E3A8A"),
            Tag(name = "أدعية عامة", colorHex = "#059669")
                )) {
                    db.tagDao().insertTag(tag)
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        fun getDatabase(context: Context): AppDatabase {
            val existing = INSTANCE
            if (existing != null) return existing
            synchronized(this) {
                val instance = Room.databaseBuilder(context.applicationContext, AppDatabase::class.java, "wazakkir_database")
                    .addMigrations(MIGRATION_7_8)
                    .fallbackToDestructiveMigration(false)
                    .addCallback(object : Callback() {
                        override fun onCreate(db: SupportSQLiteDatabase) {
                            CoroutineScope(Dispatchers.IO).launch { seedDefaults(INSTANCE!!) }
                        }
                    })
                    .build()
                INSTANCE = instance
                return instance
            }
        }
    }
}
