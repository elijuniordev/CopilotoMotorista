package com.copilotomotorista.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OfferCapturePrivacyGateTest {
    private val uber = "com.ubercab.driver"
    private val ninetyNine = "com.app99.driver"
    private val browser = "com.android.chrome"

    private fun newGate() = OfferCapturePrivacyGate(setOf(uber, ninetyNine))

    @Test
    fun sameAppContentUpdatesDoNotInvalidateOlderCompleteOfferSnapshots() {
        val gate = newGate()
        assertTrue(gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 0))
        assertTrue(gate.isAllowed(uber, 1, uber))
        assertFalse(gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 1))
        assertFalse(gate.onWindowEvent(uber, 2048, 2))
        assertTrue(gate.isAllowed(uber, 1, uber))
    }

    @Test
    fun switchingToAnotherAppInvalidatesQueuedAndCompletedOcrGenerations() {
        val gate = newGate()
        gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 0)
        assertTrue(gate.isAllowed(uber, 1, uber))
        assertTrue(gate.onWindowEvent(browser, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 2))
        assertFalse(gate.isAllowed(uber, 1, browser))
        assertFalse(gate.isAllowed(uber, 1, uber))
        assertFalse(gate.isAllowed(uber, 2, uber))
        assertTrue(gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 3))
        assertFalse(gate.isAllowed(uber, 1, uber))
        assertTrue(gate.isAllowed(uber, 4, uber))
    }

    @Test
    fun switchingBetweenRideAppsDoesNotMixTheirScreenshotSessions() {
        val gate = newGate()
        gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 0)
        assertTrue(gate.onWindowEvent(ninetyNine, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 2))
        assertFalse(gate.isAllowed(uber, 1, uber))
        assertFalse(gate.isAllowed(uber, 1, ninetyNine))
        assertTrue(gate.isAllowed(ninetyNine, 3, ninetyNine))
        assertFalse(gate.isAllowed(ninetyNine, 3, uber))
    }

    @Test
    fun eventPackageIsNotEnoughWhenForegroundWindowCannotBeVerified() {
        val gate = newGate()
        gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 0)
        assertFalse(gate.isAllowed(uber, 1, null))
        assertFalse(gate.isAllowed(uber, 1, browser))
        assertFalse(gate.isAllowed(browser, 1, browser))
        assertTrue(gate.isAllowed(uber, 1, uber))
    }

    @Test
    fun backgroundContentEventsCannotInvalidateOrAuthorizeForegroundApp() {
        val gate = newGate()
        gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 0)
        assertFalse(gate.onWindowEvent(browser, 2048, 1))
        assertTrue(gate.isAllowed(uber, 1, uber))
        assertFalse(gate.onVerifiedForeground(browser, 1))
        assertFalse(gate.isAllowed(browser, 2, browser))
    }

    @Test
    fun ownFloatingControlWindowDoesNotInvalidateVerifiedRideApp() {
        val gate = newGate()
        val ownPackage = "com.copilotomotorista"
        gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 0)
        assertFalse(
            gate.onWindowEvent(
                packageName = ownPackage,
                eventType = OfferCapturePrivacyGate.WINDOW_STATE_CHANGED,
                generation = 1,
                verifiedForegroundPackage = uber,
                ownPackage = ownPackage
            )
        )
        assertTrue(gate.isAllowed(uber, 1, uber))
    }

    @Test
    fun actualSwitchToOwnMainScreenInvalidatesCaptureSession() {
        val gate = newGate()
        val ownPackage = "com.copilotomotorista"
        gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 0)
        assertTrue(
            gate.onWindowEvent(
                packageName = ownPackage,
                eventType = OfferCapturePrivacyGate.WINDOW_STATE_CHANGED,
                generation = 1,
                verifiedForegroundPackage = ownPackage,
                ownPackage = ownPackage
            )
        )
        assertFalse(gate.isAllowed(uber, 2, uber))
    }

    @Test
    fun delayedUberStateEventCannotRestoreSessionWhenBrowserOwnsForeground() {
        val gate = newGate()
        gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 0)
        assertTrue(gate.isAllowed(uber, 1, uber))

        assertTrue(
            gate.onWindowEvent(
                packageName = uber,
                eventType = OfferCapturePrivacyGate.WINDOW_STATE_CHANGED,
                generation = 2,
                verifiedForegroundPackage = browser
            )
        )
        // Even if the foreground later returns to Uber without a fresh
        // accessibility event, old generations cannot be replayed.
        assertFalse(gate.isAllowed(uber, 3, uber))
        assertTrue(gate.onVerifiedForeground(uber, 3))
        assertTrue(gate.isAllowed(uber, 4, uber))
    }

    @Test
    fun staleWindowEventFromRideAppCannotReauthorizeAfterKnownAppSwitch() {
        val gate = newGate()
        gate.onWindowEvent(uber, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 0)
        gate.onWindowEvent(browser, OfferCapturePrivacyGate.WINDOW_STATE_CHANGED, 1)
        assertFalse(
            gate.onWindowEvent(
                packageName = uber,
                eventType = OfferCapturePrivacyGate.WINDOW_STATE_CHANGED,
                generation = 2,
                verifiedForegroundPackage = browser
            )
        )
        assertFalse(gate.isAllowed(uber, 3, uber))
        assertTrue(gate.onVerifiedForeground(uber, 3))
        assertTrue(gate.isAllowed(uber, 4, uber))
    }

    @Test
    fun verifiedContentEventCanStartSessionWithoutPriorWindowStateEvent() {
        val gate = newGate()
        assertFalse(gate.isAllowed(uber, 1, uber))
        assertTrue(gate.onVerifiedForeground(uber, 0))
        assertTrue(gate.isAllowed(uber, 1, uber))
        gate.invalidate(1)
        assertFalse(gate.isAllowed(uber, 2, uber))
    }
}
