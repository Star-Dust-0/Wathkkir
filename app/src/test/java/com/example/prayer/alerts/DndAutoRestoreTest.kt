package com.example.prayer.alerts

import android.app.NotificationManager
import android.content.Context
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.RuntimeEnvironment
import org.robolectric.Shadows
import org.robolectric.annotation.Config

/**
 * Auto-DND must never rewrite the user's own notification mode. Covers the reported cases:
 * the DND-off alarm firing although the app never enabled DND (full-screen Fajr alarm suppresses
 * the adhan bell), restoring the mode the phone was in (not forcing INTERRUPTION_FILTER_ALL),
 * and leaving DND off when the user turned it off by hand during the iqama window.
 */
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class DndAutoRestoreTest {

    private val app: Context get() = RuntimeEnvironment.getApplication()
    private fun nm() = app.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

    @Before
    fun setUp() {
        Shadows.shadowOf(nm()).setNotificationPolicyAccessGranted(true)
        app.getSharedPreferences(PrayerAlertReceiver.DND_PREFS, Context.MODE_PRIVATE).edit().clear().commit()
        nm().setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
    }

    @Test
    fun dndOffWithoutPriorEnableLeavesTheUserModeUntouched() {
        nm().setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_PRIORITY) // the user's own mode
        PrayerAlertReceiver.restoreDnd(app)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, nm().currentInterruptionFilter)
    }

    @Test
    fun restorePutsBackTheModeThePhoneWasIn() {
        nm().setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE) // user had full DND on
        PrayerAlertReceiver.enableDnd(app)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_PRIORITY, nm().currentInterruptionFilter)
        PrayerAlertReceiver.restoreDnd(app)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE, nm().currentInterruptionFilter)
    }

    @Test
    fun userTurningDndOffByHandIsNotReEnabled() {
        nm().setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        PrayerAlertReceiver.enableDnd(app)
        nm().setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL) // user opens settings and turns DND off
        PrayerAlertReceiver.restoreDnd(app)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALL, nm().currentInterruptionFilter)
    }

    @Test
    fun userSwitchingToAlarmsOnlyIsNotOverwritten() {
        nm().setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        PrayerAlertReceiver.enableDnd(app)
        nm().setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALARMS) // user changes mode by hand
        PrayerAlertReceiver.restoreDnd(app)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_ALARMS, nm().currentInterruptionFilter)
    }

    @Test
    fun userSwitchingToTotalSilenceIsNotOverwritten() {
        nm().setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_ALL)
        PrayerAlertReceiver.enableDnd(app)
        nm().setInterruptionFilter(NotificationManager.INTERRUPTION_FILTER_NONE)
        PrayerAlertReceiver.restoreDnd(app)
        assertEquals(NotificationManager.INTERRUPTION_FILTER_NONE, nm().currentInterruptionFilter)
    }
}
