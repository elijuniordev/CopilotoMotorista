package com.copilotomotorista

import com.copilotomotorista.service.AccessibilityEventGate
import com.copilotomotorista.service.SupportedRideApps
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class AccessibilityEventGateTest {

    private val gate = AccessibilityEventGate(supportedPackages = SupportedRideApps.packages)

    @Test
    fun acceptsWindowEventsOnlyFromSupportedRideApplications() {
        assertTrue(
            gate.shouldAccept(
                SupportedRideApps.UBER_DRIVER,
                AccessibilityEventGate.TYPE_WINDOW_STATE_CHANGED,
                1_000L
            )
        )
        assertTrue(
            gate.shouldAccept(
                SupportedRideApps.NINETY_NINE_DRIVER,
                AccessibilityEventGate.TYPE_WINDOW_STATE_CHANGED,
                1_000L
            )
        )
        assertFalse(
            gate.shouldAccept(
                "com.example.other",
                AccessibilityEventGate.TYPE_WINDOW_STATE_CHANGED,
                1_000L
            )
        )
        assertFalse(
            gate.shouldAccept(
                null,
                AccessibilityEventGate.TYPE_WINDOW_CONTENT_CHANGED,
                1_000L
            )
        )
    }

    @Test
    fun debouncesRepeatedContentChangesPerSupportedPackage() {
        val first = gate.shouldAccept(
            SupportedRideApps.UBER_DRIVER,
            AccessibilityEventGate.TYPE_WINDOW_CONTENT_CHANGED,
            1_000L
        )
        val repeated = gate.shouldAccept(
            SupportedRideApps.UBER_DRIVER,
            AccessibilityEventGate.TYPE_WINDOW_CONTENT_CHANGED,
            1_200L
        )
        val afterDebounce = gate.shouldAccept(
            SupportedRideApps.UBER_DRIVER,
            AccessibilityEventGate.TYPE_WINDOW_CONTENT_CHANGED,
            1_350L
        )

        assertTrue(first)
        assertFalse(repeated)
        assertTrue(afterDebounce)
    }

    @Test
    fun windowStateChangesAreNotBlockedByContentDebounce() {
        gate.shouldAccept(
            SupportedRideApps.UBER_DRIVER,
            AccessibilityEventGate.TYPE_WINDOW_CONTENT_CHANGED,
            1_000L
        )

        assertTrue(
            gate.shouldAccept(
                SupportedRideApps.UBER_DRIVER,
                AccessibilityEventGate.TYPE_WINDOW_STATE_CHANGED,
                1_050L
            )
        )
    }

    @Test
    fun ignoresUnsupportedEventTypes() {
        assertFalse(
            gate.shouldAccept(
                SupportedRideApps.UBER_DRIVER,
                1,
                1_000L
            )
        )
    }
}
