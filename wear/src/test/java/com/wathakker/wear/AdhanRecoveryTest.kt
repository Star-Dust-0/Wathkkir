package com.wathakker.wear

import android.app.NotificationManager
import android.content.Context
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class AdhanRecoveryTest {
    @Test fun alertHonorsFullScreenSettingAndOriginalTimeoutInBothLanguages() {
        val c = RuntimeEnvironment.getApplication()
        val nm = c.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager
        val app = shadowOf(c)
        for (lang in listOf(1, 2)) for (full in listOf(false, true)) {
            Loc.save(c, lang)
            Adhan.setFullScreen(c, full)
            Adhan.fire(c, 4, 100000L)
            val n = shadowOf(nm).getNotification(7004)
            assertNotNull(n)
            assertEquals(300000L, n.timeoutAfter)
            assertEquals(full, n.fullScreenIntent != null)
            assertEquals(if (lang == 2) "Time for adhan: Maghrib" else "حان موعد أذان المغرب", n.extras.getString("android.title"))
            val launched = app.nextStartedActivity
            assertEquals(full, launched != null)
            if (full) assertTrue(launched!!.component!!.className.endsWith("AdhanActivity"))
        }
    }
}
