package com.copilotomotorista

import com.copilotomotorista.service.ScreenTextSnapshot
import com.copilotomotorista.service.ScreenTextSource
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class ScreenTextSnapshotTest {

    @Test
    fun normalizesWhitespaceAndRemovesDuplicateLines() {
        val normalized = ScreenTextSnapshot.normalize(
            "  R$ 32,50  \n\n  8,5 km  \nR$ 32,50"
        )

        assertEquals("R$ 32,50\n8,5 km", normalized)
    }

    @Test
    fun marksEmptyOrVeryShortTextAsInsufficient() {
        assertFalse(
            ScreenTextSnapshot("", ScreenTextSource.ACCESSIBILITY_TREE, 1L).hasUsableText
        )
        assertFalse(
            ScreenTextSnapshot("R$ 8", ScreenTextSource.ACCESSIBILITY_TREE, 1L).hasUsableText
        )
    }

    @Test
    fun acceptsMeaningfulScreenTextWithoutInterpretingItAsAnOffer() {
        val snapshot = ScreenTextSnapshot(
            "R$ 32,50\n8,5 km\n22 minutos",
            ScreenTextSource.ACCESSIBILITY_TREE,
            1L
        )

        assertTrue(snapshot.hasUsableText)
        assertEquals(ScreenTextSource.ACCESSIBILITY_TREE, snapshot.source)
    }
}
