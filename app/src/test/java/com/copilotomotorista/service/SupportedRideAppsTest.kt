package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportedRideAppsTest {
    @Test
    fun officialDriverPackagesHaveStableAttribution() {
        assertEquals("uber", SupportedRideApps.platformForPackage(SupportedRideApps.UBER_DRIVER))
        assertEquals("99", SupportedRideApps.platformForPackage(SupportedRideApps.NINETY_NINE_DRIVER))
        assertTrue(SupportedRideApps.contains(SupportedRideApps.UBER_DRIVER))
        assertTrue(SupportedRideApps.contains(SupportedRideApps.NINETY_NINE_DRIVER))
    }

    @Test
    fun unrelatedAppsCannotBeUsedAsCaptureSources() {
        assertNull(SupportedRideApps.platformForPackage("com.android.chrome"))
        assertNull(SupportedRideApps.platformForPackage("com.copilotomotorista"))
        assertNull(SupportedRideApps.platformForPackage("com.ubercab.driver.fake"))
        assertNull(SupportedRideApps.platformForPackage(null))
        assertFalse(SupportedRideApps.contains(null))
        assertFalse(SupportedRideApps.contains("com.android.chrome"))
    }
}
