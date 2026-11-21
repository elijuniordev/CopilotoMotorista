package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Test

class ScreenTextSnapshotTest {

    @Test
    fun normalizeKeepsRepeatedRadarActionsAndMatchingFares() {
        val normalized = ScreenTextSnapshot.normalize(
            """
                Radar de Viagens
                R$ 12,50
                Tenho interesse
                R$ 12,50
                Tenho interesse
            """.trimIndent()
        )

        assertEquals(
            "Radar de Viagens\nR$ 12,50\nTenho interesse\nR$ 12,50\nTenho interesse",
            normalized
        )
    }

    @Test
    fun normalizeKeepsLineOrderWhileRemovingBlankLinesAndExtraSpacing() {
        val normalized = ScreenTextSnapshot.normalize(
            " UberX  \n  5 min   •   1,1 km  \n \n UberX  "
        )

        assertEquals("UberX\n5 min • 1,1 km\nUberX", normalized)
    }
}
