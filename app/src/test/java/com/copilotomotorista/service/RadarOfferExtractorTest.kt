package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class RadarOfferExtractorTest {
    private val extractor = RadarOfferExtractor()

    @Test
    fun countsOnlyPrimaryFareAnchorsAndExcludesRatesBaseFaresAndBonuses() {
        val count = RadarOfferExtractor.countPrimaryFareAnchors(
            """
            Radar de Viagens
            R$ 12,50
            UberX
            Est. de R$ 2,20/km
            +R$ 1,00 incluído
            Tarifa base R$ 1,28
            Tenho interesse
            R$ 18,90
            R$ 3,15/km
            Ver oferta
            """.trimIndent()
        )

        assertEquals(2, count)
    }

    @Test
    fun doesNotCountTwoFareAmountsFromASingleRateAndOneCard() {
        val count = RadarOfferExtractor.countPrimaryFareAnchors(
            """
            Radar de Viagens
            R$ 12,50
            20 min • 5,7 km - Est. de R$ 2,20/km
            5 min • 1,1 km
            15 min • 4,6 km
            Tenho interesse
            """.trimIndent()
        )

        assertEquals(1, count)
    }

    @Test
    fun endToEndOcrNormalizationKeepsTwoSamePriceRadarCards() {
        val text = ScreenTextSnapshot.normalize(
            """
            Radar de Viagens
            R$ 12,50
            UberX
            20 min • 5,7 km - Est. de R$2,20/km
            +R$ 1,00 incluído
            5 min • 1,1 km
            15 min • 4,6 km
            Tenho interesse
            R$ 12,50
            UberX
            24 min • 7,0 km - Est. de R$1,79/km
            +R$ 1,00 incluído
            6 min • 1,5 km
            18 min • 5,5 km
            Tenho interesse
            """.trimIndent()
        )

        val classification = OfferScreenClassifier().classify(text)
        assertEquals(OfferScreenClassifier.ScreenKind.RADAR_CANDIDATE, classification.kind)

        val cards = extractor.extract(text)
        assertEquals(2, cards.size)
        assertEquals(listOf(1, 2), cards.map { it.position })
        assertEquals(listOf(1250L, 1250L), cards.map { it.offer.receivedFareCents })
        assertEquals(1.1, cards[0].offer.pickupDistanceKm, 0.001)
        assertEquals(1.5, cards[1].offer.pickupDistanceKm, 0.001)
    }

    @Test
    fun extractsTwoIndependentCompleteOffersFromRadarReadingOrder() {
        val candidates = extractor.extract(
            """
            Radar de Viagens
            R$ 12,50
            UberX
            20 min • 5,7 km - Est. de R$2,20/km
            +R$ 1,00 incluído
            5 min • 1,1 km
            15 min • 4,6 km
            Tenho interesse
            R$ 18,90
            UberX
            24 min • 7,0 km - Est. de R$2,70/km
            +R$ 2,00 incluído
            6 min • 1,5 km
            18 min • 5,5 km
            Tenho interesse
            """.trimIndent()
        )

        assertEquals(2, candidates.size)
        assertEquals(listOf(1, 2), candidates.map { it.position })
        assertEquals(1250L, candidates[0].offer.receivedFareCents)
        assertEquals(1890L, candidates[1].offer.receivedFareCents)
        assertEquals(1.1, candidates[0].offer.pickupDistanceKm, 0.001)
        assertEquals(4.6, candidates[0].offer.tripDistanceKm, 0.001)
        assertEquals(1.5, candidates[1].offer.pickupDistanceKm, 0.001)
        assertEquals(5.5, candidates[1].offer.tripDistanceKm, 0.001)
    }

    @Test
    fun rejectsSingleOfferScreenAndDoesNotPretendItIsRadar() {
        val candidates = extractor.extract(
            """
            R$ 13,05
            UberX Exclusivo
            20 min • 5,7 km - Est. de R$2,29/km
            +R$ 2,25 incluído
            5 min • 1,1 km
            15 min • 4,6 km
            Tenho interesse
            """.trimIndent()
        )

        assertTrue(candidates.isEmpty())
    }

    @Test
    fun skipsIncompleteRadarCardsWithoutCombiningTheirFields() {
        val candidates = extractor.extract(
            """
            Radar de Viagens
            R$ 10,00
            UberX
            8 min • 2,0 km
            Tenho interesse
            R$ 22,00
            UberX
            30 min • 8,0 km - Est. de R$2,75/km
            +R$ 1,00 incluído
            5 min • 1,0 km
            25 min • 7,0 km
            Tenho interesse
            """.trimIndent()
        )

        assertEquals(1, candidates.size)
        assertEquals(2200L, candidates.single().offer.receivedFareCents)
    }

    @Test
    fun ignoresSecondaryRateAndBonusAmountsAsCardBoundaries() {
        val candidates = extractor.extract(
            """
            R$ 15,00
            R$ 3,15/km
            Tarifa base R$ 1,28
            UberX
            20 min • 5,7 km - Est. de R$2,29/km
            +R$ 2,25 incluído
            5 min • 1,1 km
            15 min • 4,6 km
            Tenho interesse
            R$ 17,00
            R$ 3,40/km
            Tarifa base R$ 1,28
            UberX
            22 min • 6,0 km - Est. de R$2,50/km
            +R$ 1,50 incluído
            4 min • 0,9 km
            18 min • 5,1 km
            Ver oferta
            """.trimIndent()
        )

        assertEquals(2, candidates.size)
        assertEquals(listOf(1500L, 1700L), candidates.map { it.offer.receivedFareCents })
    }
}
