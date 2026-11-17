package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingEvidenceCaptureQueueTest {

    private val coordinator = PendingScreenshotWorkCoordinator()

    @Test
    fun keepsEvidenceRequestsInArrivalOrderAndIgnoresRepeatedRecordIds() {
        coordinator.enqueueEvidence("record-a")
        coordinator.enqueueEvidence("record-b")
        coordinator.enqueueEvidence("record-a")
        coordinator.enqueueEvidence("record-c")
        coordinator.enqueueEvidence("record-b")

        assertEquals(PendingScreenshotWork.Evidence("record-a"), coordinator.pollNext(10_000L, 5_000L))
        assertEquals(PendingScreenshotWork.Evidence("record-b"), coordinator.pollNext(10_000L, 5_000L))
        assertEquals(PendingScreenshotWork.Evidence("record-c"), coordinator.pollNext(10_000L, 5_000L))
        assertNull(coordinator.pollNext(10_000L, 5_000L))
    }

    @Test
    fun retriesEvidenceAfterFailedCaptureIsDequeued() {
        coordinator.enqueueEvidence("record-a")
        assertEquals(PendingScreenshotWork.Evidence("record-a"), coordinator.pollNext(10_000L, 5_000L))

        // A failed screenshot can be requested again by a later service event.
        coordinator.enqueueEvidence("record-a")
        assertEquals(PendingScreenshotWork.Evidence("record-a"), coordinator.pollNext(10_000L, 5_000L))
    }

    @Test
    fun prioritizesPendingOcrBeforeEvidenceAfterScreenshotCompletes() {
        val ocr = PendingOcrRequest(9_500L, "uber", 7L)
        coordinator.enqueueEvidence("record-a")
        coordinator.enqueueOcr(ocr)

        assertEquals(PendingScreenshotWork.Ocr(ocr), coordinator.pollNext(10_000L, 5_000L))
        assertEquals(PendingScreenshotWork.Evidence("record-a"), coordinator.pollNext(10_000L, 5_000L))
    }

    @Test
    fun keepsOnlyMostRecentPendingOcrRequest() {
        val older = PendingOcrRequest(9_000L, "uber", 4L)
        val latest = PendingOcrRequest(9_500L, "99", 5L)
        coordinator.enqueueOcr(older)
        coordinator.enqueueOcr(latest)

        assertEquals(PendingScreenshotWork.Ocr(latest), coordinator.pollNext(10_000L, 5_000L))
        assertNull(coordinator.pollNext(10_000L, 5_000L))
    }

    @Test
    fun discardsExpiredOcrAndContinuesWithEvidence() {
        coordinator.enqueueOcr(PendingOcrRequest(1_000L, "uber", 2L))
        coordinator.enqueueEvidence("record-a")

        assertEquals(PendingScreenshotWork.Evidence("record-a"), coordinator.pollNext(10_000L, 5_000L))
        assertNull(coordinator.pollNext(10_000L, 5_000L))
    }

    @Test
    fun clearDiscardsPendingOcrAndEvidenceWhenServiceIsDestroyed() {
        coordinator.enqueueOcr(PendingOcrRequest(9_500L, "uber", 7L))
        coordinator.enqueueEvidence("record-a")
        coordinator.clear()

        assertNull(coordinator.pollNext(10_000L, 5_000L))
    }
}
