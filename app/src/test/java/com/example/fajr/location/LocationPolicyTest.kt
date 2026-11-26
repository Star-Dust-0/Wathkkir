package com.example.fajr.location

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

/** The freshness/accuracy gate that decides whether a fix may feed the prayer-time calculation. */
class LocationPolicyTest {

    @Test
    fun freshAccurateFixIsAccepted() {
        assertTrue(LocationPolicy.isAcceptable(ageMs = 0, hasAccuracy = true, accuracyM = 20f))
        assertTrue(LocationPolicy.isAcceptable(LocationPolicy.MAX_AGE_MS, true, LocationPolicy.MAX_ACCURACY_M))
    }

    @Test
    fun staleFixIsRejected() {
        assertFalse(LocationPolicy.isAcceptable(LocationPolicy.MAX_AGE_MS + 1, true, 10f))
    }

    @Test
    fun inaccurateFixIsRejected() {
        assertFalse(LocationPolicy.isAcceptable(ageMs = 60_000, hasAccuracy = true, accuracyM = LocationPolicy.MAX_ACCURACY_M + 1))
    }

    @Test
    fun fixWithoutAccuracyReadingIsJudgedOnAgeAlone() {
        assertTrue(LocationPolicy.isAcceptable(ageMs = 60_000, hasAccuracy = false, accuracyM = 0f))
        assertFalse(LocationPolicy.isAcceptable(LocationPolicy.MAX_AGE_MS + 60_000, false, 0f))
    }
}
