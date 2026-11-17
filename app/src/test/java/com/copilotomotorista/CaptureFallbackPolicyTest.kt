package com.copilotomotorista

import com.copilotomotorista.service.PendingScreenshotWork
import com.copilotomotorista.service.PendingScreenshotWorkCoordinator
import com.copilotomotorista.service.OfferTextParseStatus
import com.copilotomotorista.service.ScreenTextSource
import com.copilotomotorista.service.shouldRequestOcrFallback
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureFallbackPolicyTest {
    @Test
    fun requestsOcrWhenAccessibilityTreeHasIncompleteOffer() {
        assertTrue(shouldRequestOcrFallback(ScreenTextSource.ACCESSIBILITY_TREE, OfferTextParseStatus.INCOMPLETE))
    }

    @Test
    fun doesNotRepeatOcrForOcrInputOrAmbiguousData() {
        assertFalse(shouldRequestOcrFallback(ScreenTextSource.OCR, OfferTextParseStatus.INCOMPLETE))
        assertFalse(shouldRequestOcrFallback(ScreenTextSource.ACCESSIBILITY_TREE, OfferTextParseStatus.AMBIGUOUS))
        assertFalse(shouldRequestOcrFallback(ScreenTextSource.ACCESSIBILITY_TREE, OfferTextParseStatus.NOT_OFFER))
    }

    @Test
    fun queuesEvidenceCapturesInOrderAndIgnoresDuplicates() {
        val coordinator = PendingScreenshotWorkCoordinator()

        coordinator.enqueueEvidence("ride-1")
        coordinator.enqueueEvidence("ride-2")
        coordinator.enqueueEvidence("ride-1")

        assertEquals(PendingScreenshotWork.Evidence("ride-1"), coordinator.pollNext(10_000L, 5_000L))
        assertEquals(PendingScreenshotWork.Evidence("ride-2"), coordinator.pollNext(10_000L, 5_000L))
        assertNull(coordinator.pollNext(10_000L, 5_000L))
    }

    @Test
    fun canClearPendingEvidenceWhenServiceIsDestroyed() {
        val coordinator = PendingScreenshotWorkCoordinator()
        coordinator.enqueueEvidence("ride-1")

        coordinator.clear()

        assertNull(coordinator.pollNext(10_000L, 5_000L))
    }
}
