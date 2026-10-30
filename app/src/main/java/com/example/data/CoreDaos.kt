package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

// Secondary reconstruction: query text and operation semantics from original APK DAO implementations.
@Dao
interface DhikrDao {
    @Query("SELECT * FROM dhikr_table") fun getAllDhikr(): Flow<List<Dhikr>>
    @Query("SELECT * FROM dhikr_table WHERE isFavorite = 1") fun getFavoriteDhikr(): Flow<List<Dhikr>>
    @Query("SELECT * FROM dhikr_table WHERE isEnabled = 1") fun getEnabledDhikr(): Flow<List<Dhikr>>
    @Query("SELECT * FROM dhikr_table WHERE isEnabled = 1") suspend fun getEnabledDhikrSync(): List<Dhikr>
    @Query("SELECT * FROM dhikr_table WHERE id = :id") suspend fun getDhikrById(id: Int): Dhikr?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDhikr(dhikr: Dhikr): Long
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertAll(list: List<Dhikr>)
    @Update suspend fun updateDhikr(dhikr: Dhikr)
    @Query("DELETE FROM dhikr_table WHERE id = :id") suspend fun deleteDhikrById(id: Int)
    @Query("UPDATE dhikr_table SET readCount = readCount + 1, lastReadTime = :time WHERE id = :id") suspend fun markAsRead(id: Int, time: Long = System.currentTimeMillis())
}

@Dao
interface HistoryDao {
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertHistory(history: DhikrHistory)
    @Query("SELECT * FROM dhikr_history ORDER BY timestamp DESC") fun getAllHistory(): Flow<List<DhikrHistory>>
    @Query("SELECT COUNT(*) FROM dhikr_history WHERE timestamp >= :startTime") fun getCountSince(startTime: Long): Flow<Int>
    @Query("SELECT DISTINCT dateString FROM dhikr_history ORDER BY dateString DESC") fun getDistinctReadDates(): Flow<List<String>>
    @Query("SELECT COUNT(*) FROM dhikr_history") fun getTotalCount(): Flow<Int>
    @Query("DELETE FROM dhikr_history") suspend fun clearHistory()
}

@Dao
interface TagDao {
    @Query("SELECT * FROM tag_table ORDER BY name ASC") fun getAllTags(): Flow<List<Tag>>
    @Query("SELECT * FROM tag_table WHERE tagId = :id") suspend fun getTagById(id: Long): Tag?
    @Query("SELECT * FROM tag_table WHERE name = :name LIMIT 1") suspend fun getTagByName(name: String): Tag?
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertTag(tag: Tag): Long
    @Update suspend fun updateTag(tag: Tag)
    @Delete suspend fun deleteTag(tag: Tag)
    @Query("DELETE FROM tag_table WHERE tagId = :id") suspend fun deleteTagById(id: Long)
    @Query("DELETE FROM dhikr_tag_cross_ref WHERE tagId = :id") suspend fun deleteDhikrRefsForTag(id: Long)
    @Query("DELETE FROM dhikr_tag_cross_ref WHERE dhikrId = :id") suspend fun deleteTagsForDhikr(id: Int)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDhikrTagCrossRef(ref: DhikrTagCrossRef)
    @Insert(onConflict = OnConflictStrategy.REPLACE) suspend fun insertDhikrTagCrossRefs(refs: List<DhikrTagCrossRef>)
    @Transaction @Query("SELECT * FROM dhikr_table") fun getDhikrsWithTags(): Flow<List<DhikrWithTags>>
    @Transaction @Query("SELECT * FROM dhikr_table WHERE id = :id") suspend fun getDhikrWithTagsById(id: Int): DhikrWithTags?
    @Query("SELECT T.* FROM tag_table T INNER JOIN dhikr_tag_cross_ref R ON T.tagId = R.tagId WHERE R.dhikrId = :id") fun getTagsForDhikr(id: Int): Flow<List<Tag>>
    @Query("SELECT T.* FROM tag_table T INNER JOIN dhikr_tag_cross_ref R ON T.tagId = R.tagId WHERE R.dhikrId = :id") suspend fun getTagsForDhikrSync(id: Int): List<Tag>
    @Transaction suspend fun updateDhikrTags(id: Int, tags: List<Long>) {
        deleteTagsForDhikr(id)
        val refs = tags.map { DhikrTagCrossRef(id, it) }
        if (refs.isNotEmpty()) insertDhikrTagCrossRefs(refs)
    }
}
