package com.example.data
import androidx.room.Room
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.flow.first
import org.junit.Test
import org.junit.Assert.*
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
@RunWith(RobolectricTestRunner::class)
@Config(sdk=[34])
class DatabaseRecoveryTest {
 @Test fun bundledHisnAndReminderFavoriteTagReadPersistenceWork()=runBlocking {
  val c=RuntimeEnvironment.getApplication()
  val db=Room.inMemoryDatabaseBuilder(c,AppDatabase::class.java).allowMainThreadQueries().build()
  try {
   HisnImporter.ensureImported(c,db.hisnDao())
   val chapters=db.hisnDao().chapters().first();val items=db.hisnDao().allItems().first()
   assertTrue(chapters.isNotEmpty());assertTrue(items.size>chapters.size)
   assertEquals(setOf(29,30,31,96),db.hisnDao().favorites().first().map{it.chapterId}.toSet())
   val initial=items.size;HisnImporter.ensureImported(c,db.hisnDao());assertEquals(initial,db.hisnDao().allItems().first().size)
   val repo=DhikrRepository(db.dhikrDao(),db.tagDao(),db.historyDao())
   val id=repo.insert(Dhikr(title="تجربة",content="ذكر",reminderTimes=listOf("09:00","14:30"),isEnabled=true)).toInt()
   val tag=repo.getOrCreateTagByName(" صباح ");assertEquals("صباح",tag.name)
   repo.updateDhikrTags(id,listOf(tag.tagId));repo.markAsRead(id)
   assertEquals(1,repo.getDhikrById(id)!!.readCount)
   assertEquals(listOf("09:00","14:30"),repo.getDhikrById(id)!!.reminderTimes)
   assertEquals(listOf(tag),db.tagDao().getTagsForDhikrSync(id))
   assertEquals(1,db.historyDao().getAllHistory().first().size)
   repo.delete(id);assertNull(repo.getDhikrById(id));assertTrue(db.tagDao().getTagsForDhikrSync(id).isEmpty())
  }finally{db.close()}
 }
}
