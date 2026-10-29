package com.example.data

import androidx.room.Dao
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import kotlinx.coroutines.flow.Flow

/**
 * Hisn el-Muslim content. Source: https://github.com/muslimpack/HisnElmoslem_App (GPL-3.0).
 * Imported once from assets/hisn_elmoslem.json.
 */
@Entity(tableName = "hisn_chapter")
data class HisnChapter(
    @PrimaryKey val id: Int,
    val order: Int,
    val name: String
)

@Entity(tableName = "hisn_item", indices = [Index(value = ["chapterId"])])
data class HisnItem(
    @PrimaryKey val id: Int,
    val chapterId: Int,
    val order: Int,
    val text: String,
    val count: Int,
    val fadl: String,
    val source: String,
    val hokm: String
)

/** itemId == 0 means the whole chapter is a favorite. */
@Entity(tableName = "hisn_favorite", indices = [Index(value = ["chapterId", "itemId"], unique = true)])
data class HisnFavorite(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Int,
    val itemId: Int = 0
)

@Entity(tableName = "hisn_read")
data class HisnRead(
    @PrimaryKey(autoGenerate = true) val id: Long = 0,
    val chapterId: Int,
    val dateString: String,
    val timestamp: Long
)

data class ChapterItemCount(val chapterId: Int, val itemCount: Int)

@Dao
interface HisnDao {
    @Query("SELECT * FROM hisn_chapter ORDER BY `order`")
    fun chapters(): Flow<List<HisnChapter>>

    @Query("SELECT * FROM hisn_item ORDER BY chapterId, `order`")
    fun allItems(): Flow<List<HisnItem>>

    @Query("SELECT chapterId, COUNT(*) AS itemCount FROM hisn_item GROUP BY chapterId")
    fun itemCounts(): Flow<List<ChapterItemCount>>

    @Query("SELECT COUNT(*) FROM hisn_chapter")
    suspend fun chapterCount(): Int

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertChapters(list: List<HisnChapter>)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertItems(list: List<HisnItem>)

    @Query("SELECT * FROM hisn_favorite")
    fun favorites(): Flow<List<HisnFavorite>>

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun addFavorite(fav: HisnFavorite)

    @Query("DELETE FROM hisn_favorite WHERE chapterId = :chapterId AND itemId = :itemId")
    suspend fun removeFavorite(chapterId: Int, itemId: Int)

    @Insert
    suspend fun insertRead(read: HisnRead)

    @Query("SELECT COUNT(*) FROM hisn_read WHERE chapterId = :chapterId AND dateString = :date")
    fun readToday(chapterId: Int, date: String): Flow<Int>

    @Query("SELECT chapterId FROM hisn_read WHERE dateString = :date GROUP BY chapterId")
    fun chaptersReadOn(date: String): Flow<List<Int>>
}
