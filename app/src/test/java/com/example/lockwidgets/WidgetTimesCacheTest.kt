package com.example.lockwidgets

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.annotation.Config
import java.util.Calendar

/**
 * The widget prayer-time cache must invalidate when ANY calculation input changes, not only
 * date/location/method. Regression test for the stale-widget bug: switching the Asr juristic
 * factor changed the app's prayer screen while widgets kept serving the cached old times.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class WidgetTimesCacheTest {

    private fun prefs() = RuntimeEnvironment.getApplication().getSharedPreferences("fajr_prefs", 0)

    @Test
    fun sameSettingsHitTheCache() {
        val app = RuntimeEnvironment.getApplication()
        val day = Calendar.getInstance()
        val a = LockWidgets.timesCached(app, day)
        val b = LockWidgets.timesCached(app, day)
        assertTrue(a.contentEquals(b))
    }

    @Test
    fun asrJuristicChangeInvalidatesTheCache() {
        val app = RuntimeEnvironment.getApplication()
        val day = Calendar.getInstance()
        val p = prefs()
        val old = p.getInt("asr_juristic", 0)
        try {
            val before = LockWidgets.timesCached(app, day)
            p.edit().putInt("asr_juristic", 1 - old).commit()
            val after = LockWidgets.timesCached(app, day)
            // Hanafi Asr differs from Shafi'i Asr, so a correct cache returns different times;
            // the old key (date/location/method only) would have returned the same cached array.
            assertFalse(before.contentEquals(after))
        } finally {
            p.edit().putInt("asr_juristic", old).commit()
        }
    }

    @Test
    fun manualAdjustmentChangeInvalidatesTheCache() {
        val app = RuntimeEnvironment.getApplication()
        val day = Calendar.getInstance()
        val p = prefs()
        val old = p.getInt("adj_fajr", 0)
        try {
            val before = LockWidgets.timesCached(app, day)
            p.edit().putInt("adj_fajr", old + 3).commit()
            val after = LockWidgets.timesCached(app, day)
            assertFalse(before.contentEquals(after))
            assertTrue(after[0] - before[0] == 3 * 60_000L)
        } finally {
            p.edit().putInt("adj_fajr", old).commit()
        }
    }
}
