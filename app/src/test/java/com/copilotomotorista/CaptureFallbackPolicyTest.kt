package com.copilotomotorista

import com.copilotomotorista.service.PendingEvidenceCaptureQueue
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
        val queue = PendingEvidenceCaptureQueue()

        queue.enqueue("ride-1")
        queue.enqueue("ride-2")
        queue.enqueue("ride-1")

        assertEquals("ride-1", queue.pollNext())
        assertEquals("ride-2", queue.pollNext())
        assertNull(queue.pollNext())
    }

    @Test
    fun canClearPendingEvidenceWhenServiceIsDestroyed() {
        val queue = PendingEvidenceCaptureQueue()
        queue.enqueue("ride-1")

        queue.clear()

        assertNull(queue.pollNext())
    }
}
