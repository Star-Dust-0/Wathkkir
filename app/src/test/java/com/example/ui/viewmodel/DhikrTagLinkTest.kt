package com.example.ui.viewmodel

import android.app.Application
import com.example.data.AppDatabase
import com.example.data.Tag
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.asCoroutineDispatcher
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.assertTrue
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.concurrent.Executors

/**
 * Regression test for the lost tag link: Room's suspend insert never completes "immediately",
 * so the old resume-detection path always skipped updateDhikrTags. The fix links the tags as a
 * plain sequential step of the same coroutine; this test proves the link lands in the database.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DhikrTagLinkTest {

    private val mainDispatcher = Executors.newSingleThreadExecutor().asCoroutineDispatcher()

    @Before
    fun setMainDispatcher() {
        Dispatchers.setMain(mainDispatcher)
    }

    @After
    fun resetMainDispatcher() {
        Dispatchers.resetMain()
        mainDispatcher.close()
    }

    @Test
    fun tagsAreLinkedAfterTheSuspendingInsert() = runBlocking {
        val app: Application = RuntimeEnvironment.getApplication()
        val db = AppDatabase.getDatabase(app)
        val tagId = db.tagDao().insertTag(Tag(name = "morning-" + System.nanoTime()))
        val vm = MainViewModel(app)
        vm.addDhikrWithScheduleAndTags("test dhikr", "content", emptyList(), listOf(tagId))
        var linked = false
        for (i in 0 until 200) {
            delay(25)
            val dhikrs = db.dhikrDao().getEnabledDhikrSync()
            val mine = dhikrs.firstOrNull { it.title == "test dhikr" }
            if (mine != null && db.tagDao().getTagsForDhikrSync(mine.id).any { it.tagId == tagId }) {
                linked = true
                break
            }
        }
        assertTrue("tags must be linked to the new dhikr after the suspending insert", linked)
    }
}
