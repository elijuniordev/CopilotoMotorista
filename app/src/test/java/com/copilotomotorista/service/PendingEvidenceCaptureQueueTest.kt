package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class PendingEvidenceCaptureQueueTest {

    private val queue = PendingEvidenceCaptureQueue()

    @Test
    fun keepsRequestsInArrivalOrderAndIgnoresRepeatedRecordIds() {
        queue.enqueue("record-a")
        queue.enqueue("record-b")
        queue.enqueue("record-a")
        queue.enqueue("record-c")
        queue.enqueue("record-b")

        assertEquals("record-a", queue.pollNext())
        assertEquals("record-b", queue.pollNext())
        assertEquals("record-c", queue.pollNext())
        assertNull(queue.pollNext())
    }

    @Test
    fun allowsRetryAfterARequestWasDequeuedAndCaptureFailed() {
        queue.enqueue("record-a")

        val failedRequest = queue.pollNext()
        assertEquals("record-a", failedRequest)

        // The service removes a failed capture from its in-flight guard and
        // enqueues it again when a later event requests evidence for that record.
        queue.enqueue(failedRequest!!)

        assertEquals("record-a", queue.pollNext())
        assertNull(queue.pollNext())
    }

    @Test
    fun drainsRequestsOneAtATimeAfterTheCurrentScreenshotCompletes() {
        queue.enqueue("record-a")
        queue.enqueue("record-b")
        queue.enqueue("record-c")

        val drained = mutableListOf<String>()
        while (true) {
            val next = queue.pollNext() ?: break
            drained += next
        }

        assertEquals(listOf("record-a", "record-b", "record-c"), drained)
        assertNull(queue.pollNext())
    }

    @Test
    fun clearDiscardsPendingRequestsWhenTheServiceIsDestroyed() {
        queue.enqueue("record-a")
        queue.enqueue("record-b")

        queue.clear()

        assertNull(queue.pollNext())
    }
}
