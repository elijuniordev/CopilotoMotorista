package com.copilotomotorista.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CaptureDisplayConfigurationTest {
    private val landscape = CaptureDisplayConfiguration(
        width = 2400,
        height = 1080,
        densityDpi = 420
    )

    @Test
    fun rotationRequiresSurfaceUpdateWhenWidthAndHeightSwap() {
        val portrait = CaptureDisplayConfiguration(
            width = 1080,
            height = 2400,
            densityDpi = 420
        )

        assertTrue(portrait.requiresUpdateFrom(landscape))
    }

    @Test
    fun resolutionChangeRequiresSurfaceUpdate() {
        val resized = CaptureDisplayConfiguration(
            width = 2200,
            height = 1000,
            densityDpi = 420
        )

        assertTrue(resized.requiresUpdateFrom(landscape))
    }

    @Test
    fun densityChangeRequiresSurfaceUpdateEvenWhenDimensionsStaySame() {
        val higherDensity = landscape.copy(densityDpi = 440)

        assertTrue(higherDensity.requiresUpdateFrom(landscape))
    }

    @Test
    fun unchangedConfigurationDoesNotRequireSurfaceUpdate() {
        assertFalse(landscape.requiresUpdateFrom(landscape))
    }

    @Test
    fun invalidDimensionsOrDensityAreRejected() {
        assertFalse(CaptureDisplayConfiguration(0, 1080, 420).isValid)
        assertFalse(CaptureDisplayConfiguration(1080, -1, 420).isValid)
        assertFalse(CaptureDisplayConfiguration(1080, 2400, 0).isValid)
        assertFalse(CaptureDisplayConfiguration(0, 1080, 420).requiresUpdateFrom(landscape))
    }
}
