package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Test

class OfferScreenClassifierTest {
    private val classifier = OfferScreenClassifier()

    @Test
    fun recognizesNinetyNineExclusiveOfferFromRealOcrShape() {
        val result = classifier.classify(
            """
            Pgto. no app
            R$8,94
            R$2,40/km
            R$1,28 Tarifa base dinâmica incl.
            4 min (1 km)
            10 min (2,7 km)
            """.trimIndent()
        )
        assertEquals(OfferScreenClassifier.ScreenKind.SINGLE_OFFER, result.kind)
    }

    @Test
    fun recognizesUberExclusiveOfferWithPickupAndTripSegments() {
        val result = classifier.classify(
            """
            UberX Exclusivo
            R$ 13,05
            20 min: 5,7 km - Est. de R$2,29/km
            +R$2,25 incluído
            5 min • 1,1 km
            15 minutos • 4,6 km
            """.trimIndent()
        )
        assertEquals(OfferScreenClassifier.ScreenKind.SINGLE_OFFER, result.kind)
    }

    @Test
    fun recognizesUberOfferWithLabeledPickupAndDestination() {
        val result = classifier.classify(
            """
            R$ 27,38
            R$/KM R$/HORA LÍQUIDO
            Embarque: R. Juan Vicente (6 min • 1,5 km)
            Destino: Não informado (23 min • 16,0 km)
            29 min • 17,5 km - Est. de R$1,56/km
            """.trimIndent()
        )
        assertEquals(OfferScreenClassifier.ScreenKind.SINGLE_OFFER, result.kind)
    }

    @Test
    fun rejectsUnrelatedScreenEvenWhenItContainsCurrencyAndDistances() {
        val result = classifier.classify(
            """
            Carteira
            Saldo R$ 120,00
            Histórico de pagamentos
            Entrega concluída em 12 min
            Distância percorrida 5 km
            Configurações e ajuda
            """.trimIndent()
        )
        assertEquals(OfferScreenClassifier.ScreenKind.NOT_OFFER, result.kind)
    }

    @Test
    fun rejectsMapAndRadarLabelWithoutAnOfferCardAction() {
        val result = classifier.classify(
            """
            Radar de Viagens
            Mapa de Osasco
            R$ 8,50
            Próxima viagem
            +R$ 2,75
            """.trimIndent()
        )
        assertEquals(OfferScreenClassifier.ScreenKind.NOT_OFFER, result.kind)
    }

    @Test
    fun keepsRadarAsSeparateScreenTypeInsteadOfParsingItAsOneOffer() {
        val result = classifier.classify(
            """
            Radar de Viagens
            R$ 12,50
            8 min • 2,1 km
            Tenho interesse
            R$ 18,90
            14 min • 5,2 km
            Tenho interesse
            """.trimIndent()
        )
        assertEquals(OfferScreenClassifier.ScreenKind.RADAR_CANDIDATE, result.kind)
    }
    @Test
    fun recognizesPartialNinetyNineAndUberOcrLayouts() {
        val ninetyNine = classifier.classify(
            """
            Pgto. no app
            R$8,94
            R$2,40/km
            R$1,28 Tarifa base dinâmica incl.
            10 min (2,7 km)
            """.trimIndent()
        )
        val uber = classifier.classify(
            """
            Exclusivo
            R$ 15,09
            18 min - 6,1 km - Est. de R$2,47/km
            +R$1,50 incluído
            4 min « 0,7 km
            14 minutos * 5,4 km
            """.trimIndent()
        )
        assertEquals(OfferScreenClassifier.ScreenKind.SINGLE_OFFER, ninetyNine.kind)
        assertEquals(OfferScreenClassifier.ScreenKind.SINGLE_OFFER, uber.kind)
    }

    @Test
    fun recognizesUberOfferUsingRPerKilometerLabelsWithoutAddressLabels() {
        val result = classifier.classify(
            """
            R$ 22,31
            R$/KM R$/HORA LÍQUIDO
            30 min: 14,5 km - Est. de R$1,54/km
            5 min - 1,3 km
            25 min • 13,2 km
            """.trimIndent()
        )
        assertEquals(OfferScreenClassifier.ScreenKind.SINGLE_OFFER, result.kind)
    }

}
