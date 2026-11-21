package com.copilotomotorista.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class OverlayDisplayAttemptTest {
    @Test
    fun returnsTrueOnlyAfterAttachAndActivationSucceed() {
        var attached = false
        var activated = false
        var cleaned = false

        val displayed = OverlayDisplayAttempt.execute(
            attach = { attached = true },
            activate = { activated = true },
            cleanup = { cleaned = true }
        )

        assertTrue(displayed)
        assertTrue(attached)
        assertTrue(activated)
        assertFalse(cleaned)
    }

    @Test
    fun cleansUpAndReturnsFalseWhenAttachmentFails() {
        var cleaned = false
        val displayed = OverlayDisplayAttempt.execute(
            attach = { throw SecurityException("permission denied") },
            activate = { throw AssertionError("activation must not run") },
            cleanup = { cleaned = true }
        )

        assertFalse(displayed)
        assertTrue(cleaned)
    }

    @Test
    fun reportsFailureAndCleansUpWhenLifecycleActivationFails() {
        var cleanupCount = 0
        var reported: RuntimeException? = null
        val failure = IllegalStateException("lifecycle activation failed")

        val displayed = OverlayDisplayAttempt.execute(
            attach = {},
            activate = { throw failure },
            cleanup = { cleanupCount++ },
            onFailure = { reported = it }
        )

        assertFalse(displayed)
        org.junit.Assert.assertEquals(1, cleanupCount)
        org.junit.Assert.assertSame(failure, reported)
    }

    @Test
    fun cleanupFailureDoesNotCrashTheCaptureService() {
        var failureReported = false

        val displayed = OverlayDisplayAttempt.execute(
            attach = { throw SecurityException("overlay revoked") },
            activate = {},
            cleanup = { throw IllegalArgumentException("window already gone") },
            onFailure = { failureReported = true }
        )

        assertFalse(displayed)
        assertTrue(failureReported)
    }

    @Test
    fun cleansUpAndReturnsFalseWhenActivationFailsAfterAttachment() {
        var attached = false
        var cleaned = false
        val displayed = OverlayDisplayAttempt.execute(
            attach = { attached = true },
            activate = { throw IllegalStateException("lifecycle failure") },
            cleanup = { cleaned = true }
        )

        assertTrue(attached)
        assertFalse(displayed)
        assertTrue(cleaned)
    }
}
