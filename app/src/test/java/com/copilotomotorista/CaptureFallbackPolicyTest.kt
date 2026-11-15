package com.copilotomotorista

import com.copilotomotorista.service.OfferTextParseStatus
import com.copilotomotorista.service.ScreenTextSource
import com.copilotomotorista.service.shouldRequestOcrFallback
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
}
