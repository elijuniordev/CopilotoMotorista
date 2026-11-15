package com.copilotomotorista

import com.copilotomotorista.domain.usecase.CalculateOfferProfitabilityUseCase
import com.copilotomotorista.domain.usecase.OfferViability
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CalculateOfferProfitabilityUseCaseTest {

    private val useCase = CalculateOfferProfitabilityUseCase()

    @Test
    fun calculatesCurrentBaselineForAnOffer() {
        val result = useCase.execute(grossFare = 32.0, totalDistanceKm = 8.5)

        assertEquals(1.275, result.energyCost, 0.0001)
        assertEquals(30.725, result.netProfit, 0.0001)
        assertEquals(30.725 / 8.5, result.earningsPerKm, 0.0001)
        assertEquals(OfferViability.EXCELLENT, result.viability)
        assertTrue(result.isViable)
    }

    @Test
    fun classifiesLowReturnOfferAsLoss() {
        val result = useCase.execute(grossFare = 8.0, totalDistanceKm = 10.0)

        assertEquals(OfferViability.LOSS, result.viability)
        assertFalse(result.isViable)
    }
}
