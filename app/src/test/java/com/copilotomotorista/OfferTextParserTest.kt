package com.copilotomotorista

import com.copilotomotorista.service.OfferTextParseStatus
import com.copilotomotorista.service.OfferTextParser
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OfferTextParserTest {
    private val parser = OfferTextParser()

    @Test
    fun parsesBrazilianCurrencyAndLabeledTripDetails() {
        val result = parser.parse(
            """
            R$ 32,50
            Busca: 1,2 km em 4 min
            Viagem: 8,5 km em 22 min
            """.trimIndent()
        )

        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        assertNotNull(result.offer)
        assertEquals(3250L, result.offer?.receivedFareCents)
        assertEquals(1.2, result.offer?.pickupDistanceKm ?: 0.0, 0.001)
        assertEquals(8.5, result.offer?.tripDistanceKm ?: 0.0, 0.001)
        assertEquals(4, result.offer?.pickupTimeMinutes)
        assertEquals(22, result.offer?.tripTimeMinutes)
    }

    @Test
    fun parsesDotThousandsAndCommaDecimals() {
        val result = parser.parse("Valor: R$ 1.234,56\nTrajeto: 12,4 km / 35 min")
        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        assertEquals(123456L, result.offer?.receivedFareCents)
        assertEquals(12.4, result.offer?.tripDistanceKm ?: 0.0, 0.001)
    }

    @Test
    fun parsesDotDecimalCurrencyWithoutTreatingDotAsThousandsSeparator() {
        val result = parser.parse("Valor: R$ 32.50\nViagem: 8.5 km em 22 min")
        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        assertEquals(3250L, result.offer?.receivedFareCents)
        assertEquals(8.5, result.offer?.tripDistanceKm ?: 0.0, 0.001)
    }

    @Test
    fun rejectsZeroTripDistanceAndTimeInsteadOfCreatingAnOffer() {
        val zeroDistance = parser.parse("R$ 20,00\nViagem: 0 km em 20 min")
        val zeroTime = parser.parse("R$ 20,00\nViagem: 8 km em 0 min")
        assertEquals(OfferTextParseStatus.INCOMPLETE, zeroDistance.status)
        assertNull(zeroDistance.offer)
        assertEquals(OfferTextParseStatus.INCOMPLETE, zeroTime.status)
        assertNull(zeroTime.offer)
    }

    @Test
    fun acceptsTripWithoutExplicitPickupAsZeroPickup() {
        val result = parser.parse("R$ 18,90\nViagem: 6,2 km em 18 min")
        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        assertEquals(0.0, result.offer?.pickupDistanceKm ?: -1.0, 0.0)
        assertEquals(0, result.offer?.pickupTimeMinutes)
    }

    @Test
    fun reportsMissingRequiredFieldsInsteadOfGuessing() {
        val result = parser.parse("R$ 24,00\n8,5 km\n22 min")
        assertEquals(OfferTextParseStatus.INCOMPLETE, result.status)
        assertNull(result.offer)
        assertEquals(setOf("distância da viagem", "tempo da viagem"), result.missingFields)
    }

    @Test
    fun reportsConflictingFareOrTripMeasurementsAsAmbiguous() {
        val result = parser.parse(
            "R$ 20,00\nR$ 25,00\nViagem: 8 km em 20 min\nDestino: 9 km em 22 min"
        )
        assertEquals(OfferTextParseStatus.AMBIGUOUS, result.status)
        assertNull(result.offer)
        assertEquals(setOf("valor recebido", "distância da viagem", "tempo da viagem"), result.ambiguousFields)
    }

    @Test
    fun ignoresUnrelatedScreenText() {
        val result = parser.parse("Uber\nAjuda\nConfigurações\nConta")
        assertEquals(OfferTextParseStatus.NOT_OFFER, result.status)
    }

    @Test
    fun doesNotTreatAnUnlabeledCurrencyAsACompleteOffer() {
        val result = parser.parse("R$ 30,00\n8 km\n20 min")
        assertEquals(OfferTextParseStatus.INCOMPLETE, result.status)
        assertNull(result.offer)
    }
}
