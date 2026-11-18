package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfferPipelineStageMetricsTest {
    @Test
    fun reportsStagePercentilesAfterConfiguredSampleCount() {
        val metrics = OfferPipelineStageMetrics(reportEvery = 4)
        assertNull(metrics.record("parse", "uber", "ocr", 10, true))
        assertNull(metrics.record("parse", "uber", "ocr", 20, true))
        assertNull(metrics.record("parse", "uber", "ocr", 30, true))
        val summary = metrics.record("parse", "uber", "ocr", 40, true)

        assertEquals("parse", summary?.stage)
        assertEquals("uber", summary?.platform)
        assertEquals("ocr", summary?.source)
        assertEquals("success", summary?.outcome)
        assertEquals(4, summary?.count)
        assertEquals(20L, summary?.p50Millis)
        assertEquals(40L, summary?.p95Millis)
    }

    @Test
    fun keepsStagesPlatformsSourcesAndOutcomesSeparate() {
        val metrics = OfferPipelineStageMetrics(reportEvery = 1)
        val parse = metrics.record("parse", "uber", "ocr", 25, true)
        val persistence = metrics.record("persist", "99", "ocr", 80, false)

        assertEquals("parse", parse?.stage)
        assertEquals("uber", parse?.platform)
        assertEquals("success", parse?.outcome)
        assertEquals("persist", persistence?.stage)
        assertEquals("99", persistence?.platform)
        assertEquals("failure", persistence?.outcome)
        assertEquals(25L, parse?.p95Millis)
        assertEquals(80L, persistence?.p95Millis)
    }

    @Test
    fun ignoresInvalidDurationsAndBlankStageNames() {
        val metrics = OfferPipelineStageMetrics(reportEvery = 1)
        assertNull(metrics.record("parse", "uber", "ocr", -1, true))
        assertNull(metrics.record(" ", "uber", "ocr", 10, true))
    }
}


    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveReportInterval() {
        OfferPipelineStageMetrics(reportEvery = 0)
    }

    @Test(expected = IllegalArgumentException::class)
    fun rejectsNonPositiveSampleCapacity() {
        OfferPipelineStageMetrics(maxSamplesPerRoute = 0)
    }
