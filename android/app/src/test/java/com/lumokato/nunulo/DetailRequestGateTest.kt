package com.lumokato.nunulo

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class DetailRequestGateTest {
    @Test
    fun newestRequestForSelectedTargetIsCurrent() {
        val gate = DetailRequestGate()

        val request = gate.next()

        assertTrue(gate.isCurrent(request, selectedId = "record-1", targetId = "record-1"))
    }

    @Test
    fun openingAnotherTargetInvalidatesEarlierResponse() {
        val gate = DetailRequestGate()
        val earlier = gate.next()

        gate.next()

        assertFalse(gate.isCurrent(earlier, selectedId = "record-2", targetId = "record-1"))
    }

    @Test
    fun closingDetailInvalidatesInFlightResponse() {
        val gate = DetailRequestGate()
        val request = gate.next()

        gate.invalidate()

        assertFalse(gate.isCurrent(request, selectedId = null, targetId = "record-1"))
    }
}
