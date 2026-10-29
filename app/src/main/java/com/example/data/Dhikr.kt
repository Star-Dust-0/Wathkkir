package com.example.data

import androidx.room.ColumnInfo
import androidx.room.Dao
import androidx.room.Embedded
import androidx.room.Entity
import androidx.room.Index
import androidx.room.Insert
import androidx.room.Junction
import androidx.room.OnConflictStrategy
import androidx.room.PrimaryKey
import androidx.room.Query
import androidx.room.Relation
import androidx.room.Transaction
import androidx.room.Update
import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@Entity(
    tableName = "dhikr_table",
    indices = [Index(value = ["isFavorite"]), Index(value = ["isEnabled"])]
)
data class Dhikr(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val title: String,
    val content: String,
    val isFavorite: Boolean = false,
    val reminderTimes: List<String> = listOf("09:00"),
    val isEnabled: Boolean = false,
    val readCount: Int = 0,
    val lastReadTime: Long = 0L
)

@Entity(tableName = "tag_table")
data class Tag(
    @PrimaryKey(autoGenerate = true) val tagId: Long = 0,
    val name: String,
    val colorHex: String = "#008080"
)

@Entity(
    tableName = "dhikr_tag_cross_ref",
    primaryKeys = ["dhikrId", "tagId"],
    indices = [Index(value = ["dhikrId"]), Index(value = ["tagId"])]
)
data class DhikrTagCrossRef(val dhikrId: Int, val tagId: Long)

@Entity(tableName = "dhikr_history")
data class DhikrHistory(
    @PrimaryKey(autoGenerate = true) val historyId: Long = 0,
    val dhikrId: Int,
    val timestamp: Long = System.currentTimeMillis(),
    val dateString: String
)

data class DhikrWithTags(
    @Embedded val dhikr: Dhikr,
    @Relation(
        parentColumn = "id",
        entityColumn = "tagId",
        associateBy = Junction(value = DhikrTagCrossRef::class, parentColumn = "dhikrId", entityColumn = "tagId")
    )
    val tags: List<Tag> = emptyList()
)

class Converters {
    @androidx.room.TypeConverter
    fun fromString(value: String): List<String> = value.split(",").filter { it.isNotBlank() }

    @androidx.room.TypeConverter
    fun toString(list: List<String>): String = list.joinToString(",")
}

@Dao
interface DhikrDao {
    @Query("SELECT * FROM dhikr_table")
    fun getAllDhikr(): Flow<List<Dhikr>>

    @Query("SELECT * FROM dhikr_table WHERE isFavorite = 1")
    fun getFavoriteDhikr(): Flow<List<Dhikr>>

    @Query("SELECT * FROM dhikr_table WHERE isEnabled = 1")
    fun getEnabledDhikr(): Flow<List<Dhikr>>

    @Query("SELECT * FROM dhikr_table WHERE isEnabled = 1")
    suspend fun getEnabledDhikrSync(): List<Dhikr>

    @Query("SELECT * FROM dhikr_table WHERE id = :id")
    suspend fun getDhikrById(id: Int): Dhikr?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDhikr(dhikr: Dhikr): Long

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertAll(dhikrs: List<Dhikr>)

    @Update
    suspend fun updateDhikr(dhikr: Dhikr)

    @Query("DELETE FROM dhikr_table WHERE id = :id")
    suspend fun deleteDhikrById(id: Int)

    @Query("UPDATE dhikr_table SET readCount = readCount + 1, lastReadTime = :time WHERE id = :id")
    suspend fun markAsRead(id: Int, time: Long = System.currentTimeMillis())
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag_table ORDER BY name ASC")
    fun getAllTags(): Flow<List<Tag>>

    @Query("SELECT * FROM tag_table WHERE tagId = :tagId")
    suspend fun getTagById(tagId: Long): Tag?

    @Query("SELECT * FROM tag_table WHERE name = :name LIMIT 1")
    suspend fun getTagByName(name: String): Tag?

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertTag(tag: Tag): Long

    @Update
    suspend fun updateTag(tag: Tag)

    @androidx.room.Delete
    suspend fun deleteTag(tag: Tag)

    @Query("DELETE FROM tag_table WHERE tagId = :tagId")
    suspend fun deleteTagById(tagId: Long)

    @Query("DELETE FROM dhikr_tag_cross_ref WHERE tagId = :tagId")
    suspend fun deleteDhikrRefsForTag(tagId: Long)

    @Query("DELETE FROM dhikr_tag_cross_ref WHERE dhikrId = :dhikrId")
    suspend fun deleteTagsForDhikr(dhikrId: Int)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDhikrTagCrossRef(ref: DhikrTagCrossRef)

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertDhikrTagCrossRefs(refs: List<DhikrTagCrossRef>)

    @Query("SELECT T.* FROM tag_table T INNER JOIN dhikr_tag_cross_ref R ON T.tagId = R.tagId WHERE R.dhikrId = :dhikrId")
    fun getTagsForDhikr(dhikrId: Int): Flow<List<Tag>>

    @Query("SELECT T.* FROM tag_table T INNER JOIN dhikr_tag_cross_ref R ON T.tagId = R.tagId WHERE R.dhikrId = :dhikrId")
    suspend fun getTagsForDhikrSync(dhikrId: Int): List<Tag>

    @Transaction
    @Query("SELECT * FROM dhikr_table")
    fun getDhikrsWithTags(): Flow<List<DhikrWithTags>>

    @Transaction
    @Query("SELECT * FROM dhikr_table WHERE id = :dhikrId")
    suspend fun getDhikrWithTagsById(dhikrId: Int): DhikrWithTags?

    @Transaction
    suspend fun updateDhikrTags(dhikrId: Int, tagIds: List<Long>) {
        deleteTagsForDhikr(dhikrId)
        val refs = tagIds.map { DhikrTagCrossRef(dhikrId, it) }
        if (refs.isEmpty()) return
        insertDhikrTagCrossRefs(refs)
    }
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertHistory(history: DhikrHistory)

    @Query("SELECT * FROM dhikr_history ORDER BY timestamp DESC")
    fun getAllHistory(): Flow<List<DhikrHistory>>

    @Query("SELECT COUNT(*) FROM dhikr_history WHERE timestamp >= :since")
    fun getCountSince(since: Long): Flow<Int>

    @Query("SELECT COUNT(*) FROM dhikr_history")
    fun getTotalCount(): Flow<Int>

    @Query("SELECT DISTINCT dateString FROM dhikr_history ORDER BY dateString DESC")
    fun getDistinctReadDates(): Flow<List<String>>

    @Query("DELETE FROM dhikr_history")
    suspend fun clearHistory()
}

class DhikrRepository(
    private val dhikrDao: DhikrDao,
    private val tagDao: TagDao,
    private val historyDao: HistoryDao
) {
    val allDhikr: Flow<List<Dhikr>> = dhikrDao.getAllDhikr()
    val favoriteDhikr: Flow<List<Dhikr>> = dhikrDao.getFavoriteDhikr()
    val enabledDhikr: Flow<List<Dhikr>> = dhikrDao.getEnabledDhikr()
    val allTags: Flow<List<Tag>> = tagDao.getAllTags()
    val dhikrsWithTags: Flow<List<DhikrWithTags>> = tagDao.getDhikrsWithTags()
    val allHistory: Flow<List<DhikrHistory>> = historyDao.getAllHistory()
    val totalHistoryCount: Flow<Int> = historyDao.getTotalCount()
    val distinctReadDates: Flow<List<String>> = historyDao.getDistinctReadDates()

    suspend fun getDhikrById(id: Int): Dhikr? = dhikrDao.getDhikrById(id)

    suspend fun insert(dhikr: Dhikr): Long = dhikrDao.insertDhikr(dhikr)

    suspend fun update(dhikr: Dhikr) = dhikrDao.updateDhikr(dhikr)

    suspend fun delete(id: Int) {
        dhikrDao.deleteDhikrById(id)
        tagDao.deleteTagsForDhikr(id)
    }

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    suspend fun updateTag(tag: Tag) = tagDao.updateTag(tag)

    suspend fun deleteTag(tag: Tag) {
        tagDao.deleteDhikrRefsForTag(tag.tagId)
        tagDao.deleteTag(tag)
    }

    suspend fun updateDhikrTags(dhikrId: Int, tagIds: List<Long>) = tagDao.updateDhikrTags(dhikrId, tagIds)

    suspend fun getOrCreateTagByName(name: String, colorHex: String = "#008080"): Tag {
        val trimmed = name.trim()
        val existing = tagDao.getTagByName(trimmed)
        if (existing != null) return existing
        val tag = Tag(0, trimmed, colorHex)
        val id = tagDao.insertTag(tag)
        return Tag(id, tag.name, tag.colorHex)
    }

    suspend fun markAsRead(id: Int) {
        val now = System.currentTimeMillis()
        val date = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        dhikrDao.markAsRead(id, now)
        historyDao.insertHistory(DhikrHistory(0, id, now, date))
    }
}
