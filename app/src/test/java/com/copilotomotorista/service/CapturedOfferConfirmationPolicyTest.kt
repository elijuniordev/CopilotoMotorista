package com.copilotomotorista.service

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CapturedOfferConfirmationPolicyTest {
    @Test
    fun acceptsCapturedOfferButRejectsDeletedRecord() {
        assertTrue(CapturedOfferConfirmationPolicy.canApply("captured", "accepted"))
        assertTrue(CapturedOfferConfirmationPolicy.canApply("accepted", "accepted"))
        assertFalse(CapturedOfferConfirmationPolicy.canApply(null, "accepted"))
        assertFalse(CapturedOfferConfirmationPolicy.canApply(null, "completed"))
    }

    @Test
    fun completionRequiresAnAcceptedOrInProgressRecord() {
        assertTrue(CapturedOfferConfirmationPolicy.canApply("accepted", "completed"))
        assertTrue(CapturedOfferConfirmationPolicy.canApply("in_progress", "completed"))
        assertFalse(CapturedOfferConfirmationPolicy.canApply("captured", "completed"))
    }

    @Test
    fun staleOverlayCannotDowngradeOrRecompleteTrip() {
        assertFalse(CapturedOfferConfirmationPolicy.canApply("in_progress", "accepted"))
        assertFalse(CapturedOfferConfirmationPolicy.canApply("completed", "accepted"))
        assertFalse(CapturedOfferConfirmationPolicy.canApply("completed", "completed"))
        assertFalse(CapturedOfferConfirmationPolicy.canApply("cancelled", "accepted"))
        assertFalse(CapturedOfferConfirmationPolicy.canApply("accepted", "invalid"))
    }
}
