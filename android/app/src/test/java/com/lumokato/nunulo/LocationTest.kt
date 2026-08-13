package com.lumokato.nunulo

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LocationTest {
    private val now = 1_000_000L

    @Test
    fun currentAccurateNetworkFixCanBeatInaccurateGpsFix() {
        val selected = chooseBestLocation(
            listOf(
                LocationFix(31.2, 121.4, "gps", now - 2_000, 260f, false),
                LocationFix(31.2, 121.4, "network", now - 1_000, 38f, false),
            ),
            now,
            coarseOnly = false,
            allowLastKnown = false,
        )

        assertEquals("network", selected?.provider)
    }

    @Test
    fun staleOrInaccurateCurrentFixIsRejected() {
        assertNull(
            chooseBestLocation(
                listOf(LocationFix(31.2, 121.4, "gps", now - CURRENT_LOCATION_MAX_AGE_MS - 1, 20f, false)),
                now,
                coarseOnly = false,
                allowLastKnown = false,
            )
        )
        assertNull(
            chooseBestLocation(
                listOf(LocationFix(31.2, 121.4, "gps", now, FINE_LOCATION_MAX_ACCURACY_METERS + 1, false)),
                now,
                coarseOnly = false,
                allowLastKnown = false,
            )
        )
    }

    @Test
    fun explicitlyAllowedLastKnownFixRemainsClearlyMarked() {
        val selected = chooseBestLocation(
            listOf(LocationFix(31.2, 121.4, "network", now - 10 * 60 * 1000, 80f, true)),
            now,
            coarseOnly = false,
            allowLastKnown = true,
        )

        assertEquals(true, selected?.isLastKnown)
    }

    @Test
    fun currentFixDoesNotGetLastKnownAgeAllowance() {
        assertNull(
            chooseBestLocation(
                listOf(LocationFix(31.2, 121.4, "gps", now - 10 * 60 * 1000, 20f, false)),
                now,
                coarseOnly = false,
                allowLastKnown = true,
            )
        )
    }
}
