package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfferCaptureLatencyMetricsTest {
    @Test
    fun reportsPercentilesAfterConfiguredSampleCount() {
        val metrics = OfferCaptureLatencyMetrics(reportEvery = 4)
        assertNull(metrics.record("uber", "accessibility", 100))
        assertNull(metrics.record("uber", "accessibility", 200))
        assertNull(metrics.record("uber", "accessibility", 300))
        val summary = metrics.record("uber", "accessibility", 400)
        assertEquals(4, summary?.count)
        assertEquals(200L, summary?.p50Millis)
        assertEquals(400L, summary?.p95Millis)
    }

    @Test
    fun keepsPlatformsAndCaptureSourcesSeparate() {
        val metrics = OfferCaptureLatencyMetrics(reportEvery = 1)
        val uber = metrics.record("uber", "accessibility", 120)
        val nineNine = metrics.record("99", "ocr", 850)
        assertEquals("uber", uber?.platform)
        assertEquals("accessibility", uber?.source)
        assertEquals("99", nineNine?.platform)
        assertEquals("ocr", nineNine?.source)
        assertEquals(120L, uber?.p50Millis)
        assertEquals(850L, nineNine?.p95Millis)
    }

    @Test
    fun ignoresNegativeDurations() {
        val metrics = OfferCaptureLatencyMetrics(reportEvery = 1)
        assertNull(metrics.record("uber", "accessibility", -1))
    }
}
