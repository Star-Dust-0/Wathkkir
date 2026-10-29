package com.wathakker.wear

import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.Robolectric
import org.robolectric.RobolectricTestRunner
import org.robolectric.Shadows.shadowOf
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [33])
class CirclesRecoveryTest {
    @Test fun complicationOpensItsOwnOverlayRatherThanRedirectingToTheList() {
        val controller = Robolectric.buildActivity(CirclesActivity::class.java).create()
        try {
            val activity = controller.get()
            assertFalse(activity.isFinishing)
            assertNull(shadowOf(activity).nextStartedActivity)
            val info = activity.packageManager.getActivityInfo(activity.componentName, 0)
            assertEquals(com.wathakker.wear.R.style.Theme_Wathakker_Transparent, info.theme)
        } finally { controller.destroy() }
    }
}
