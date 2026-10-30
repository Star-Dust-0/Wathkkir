package com.example.data

import kotlinx.coroutines.flow.Flow
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/** Single entry point to the dhikr, tag and read-history tables. */
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

    fun getHistoryCountSince(startTime: Long): Flow<Int> = historyDao.getCountSince(startTime)

    suspend fun getDhikrById(id: Int): Dhikr? = dhikrDao.getDhikrById(id)

    /** Counts a read: bumps the dhikr counter and appends a history row for today. */
    suspend fun markAsRead(id: Int) {
        val now = System.currentTimeMillis()
        val dateStr = SimpleDateFormat("yyyy-MM-dd", Locale.US).format(Date(now))
        dhikrDao.markAsRead(id, now)
        historyDao.insertHistory(DhikrHistory(0L, id, now, dateStr))
    }

    suspend fun insert(dhikr: Dhikr): Long = dhikrDao.insertDhikr(dhikr)

    suspend fun update(dhikr: Dhikr) {
        dhikrDao.updateDhikr(dhikr)
    }

    suspend fun delete(id: Int) {
        dhikrDao.deleteDhikrById(id)
        tagDao.deleteTagsForDhikr(id)
    }

    suspend fun insertTag(tag: Tag): Long = tagDao.insertTag(tag)

    suspend fun updateTag(tag: Tag) {
        tagDao.updateTag(tag)
    }

    suspend fun deleteTag(tag: Tag) {
        tagDao.deleteDhikrRefsForTag(tag.tagId)
        tagDao.deleteTag(tag)
    }

    suspend fun updateDhikrTags(dhikrId: Int, tagIds: List<Long>) {
        tagDao.updateDhikrTags(dhikrId, tagIds)
    }

    suspend fun getOrCreateTagByName(name: String, colorHex: String = "#008080"): Tag? {
        val trimmed = name.trim()
        tagDao.getTagByName(trimmed)?.let { return it }
        val newTag = Tag(0L, trimmed, colorHex)
        val id = tagDao.insertTag(newTag)
        return Tag(id, newTag.name, newTag.colorHex)
    }
}
