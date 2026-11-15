package com.copilotomotorista

import com.copilotomotorista.domain.model.PrivateTrip
import com.copilotomotorista.domain.usecase.PrivateTripRules
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class PrivateTripRulesTest {
    private fun validTrip() = PrivateTrip(
        id = "private-1", clientId = "client-1", title = "Transfer aeroporto",
        origin = "Osasco", destination = "GRU", scheduledAtEpochMillis = 1_800_000_000_000L,
        agreedFareCents = 18_000L, distanceKm = 42.5, durationMinutes = 55,
        status = "scheduled", paymentStatus = "pending", paymentMethod = "Pix",
        notes = "Aguardar na portaria", createdAtEpochMillis = 1_700_000_000_000L
    )

    @Test fun acceptsCompleteScheduledTrip() { PrivateTripRules.validate(validTrip()) }

    @Test fun rejectsBlankTitleAndNegativeFare() {
        assertTrue(runCatching { PrivateTripRules.validate(validTrip().copy(title = " ")) }.exceptionOrNull() is IllegalArgumentException)
        assertTrue(runCatching { PrivateTripRules.validate(validTrip().copy(agreedFareCents = -1L)) }.exceptionOrNull() is IllegalArgumentException)
    }

    @Test fun rejectsInvalidDistanceDurationAndStatuses() {
        assertTrue(runCatching { PrivateTripRules.validate(validTrip().copy(distanceKm = Double.NaN)) }.isFailure)
        assertTrue(runCatching { PrivateTripRules.validate(validTrip().copy(durationMinutes = -1)) }.isFailure)
        assertTrue(runCatching { PrivateTripRules.validate(validTrip().copy(status = "unknown")) }.isFailure)
    }

    @Test fun completionAndCancellationAreIdempotentAndPreservePayment() {
        val original = validTrip()
        val completed = PrivateTripRules.complete(original)
        assertEquals("completed", completed.status)
        assertEquals("pending", completed.paymentStatus)
        assertEquals(completed, PrivateTripRules.complete(completed))
        val cancelled = PrivateTripRules.cancel(original)
        assertEquals("cancelled", cancelled.status)
        assertEquals(cancelled, PrivateTripRules.cancel(cancelled))
    }

    @Test fun paymentCanBeMarkedReceivedAndReopened() {
        val paid = PrivateTripRules.setPaid(validTrip(), true)
        assertEquals("paid", paid.paymentStatus)
        assertEquals("pending", PrivateTripRules.setPaid(paid, false).paymentStatus)
    }
}
