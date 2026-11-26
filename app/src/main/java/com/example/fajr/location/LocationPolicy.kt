package com.example.fajr.location

/**
 * Freshness/accuracy gate a location fix must pass before it feeds the prayer-time
 * calculation. Pure JVM logic (unit-tested); Android callers supply the readings.
 *
 * A fix is acceptable when it is no older than [MAX_AGE_MS] and, when the provider
 * reports an accuracy, that accuracy is at least [MAX_ACCURACY_M]. Fixes without an
 * accuracy reading are judged on age alone.
 */
object LocationPolicy {
    /** Prayer times move slowly with position, so a fix up to 30 minutes old is fine. */
    const val MAX_AGE_MS = 30L * 60 * 1000
    /** 500 m: comfortably inside the distance that moves any prayer time by a minute. */
    const val MAX_ACCURACY_M = 500f

    fun isAcceptable(ageMs: Long, hasAccuracy: Boolean, accuracyM: Float): Boolean =
        ageMs <= MAX_AGE_MS && (!hasAccuracy || accuracyM <= MAX_ACCURACY_M)
}
