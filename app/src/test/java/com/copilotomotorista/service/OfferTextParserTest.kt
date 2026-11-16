package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertNull
import org.junit.Test

class OfferTextParserTest {

    private val parser = OfferTextParser()

    @Test
    fun parsesBrazilianOfferWithPickupAndTripDetails() {
        val result = parser.parse(
            """
            UberX
            R$ 32,50
            Busca: 2,0 km · 6 min
            Viagem: 8,5 km · 22 min
            """.trimIndent()
        )

        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        val offer = result.offer
        assertNotNull(offer)
        assertEquals(3250L, offer!!.receivedFareCents)
        assertEquals(2.0, offer.pickupDistanceKm, 0.001)
        assertEquals(8.5, offer.tripDistanceKm, 0.001)
        assertEquals(6, offer.pickupTimeMinutes)
        assertEquals(22, offer.tripTimeMinutes)
    }

    @Test
    fun parsesBrazilianCurrencyWithThousandsSeparator() {
        val result = parser.parse(
            """
            R$ 1.234,56
            Viagem: 10 km · 30 min
            """.trimIndent()
        )

        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        assertEquals(123456L, result.offer?.receivedFareCents)
    }

    @Test
    fun reportsIncompleteOfferWhenFareIsMissing() {
        val result = parser.parse("Viagem: 8,5 km · 22 min")

        assertEquals(OfferTextParseStatus.INCOMPLETE, result.status)
        assertNull(result.offer)
        assertEquals(setOf("valor recebido"), result.missingFields)
    }

    @Test
    fun reportsAmbiguousOfferWhenMoreThanOneFareIsVisible() {
        val result = parser.parse(
            """
            R$ 32,50
            R$ 35,00
            Viagem: 8,5 km · 22 min
            """.trimIndent()
        )

        assertEquals(OfferTextParseStatus.AMBIGUOUS, result.status)
        assertNull(result.offer)
        assertEquals(setOf("valor recebido"), result.ambiguousFields)
    }

    @Test
    fun ignoresTextThatDoesNotContainOfferClues() {
        val result = parser.parse("Bom dia! Como está o trânsito hoje?")

        assertEquals(OfferTextParseStatus.NOT_OFFER, result.status)
        assertNull(result.offer)
    }

    @Test
    fun reportsIncompleteOfferWhenTripDistanceOrTimeIsMissing() {
        val result = parser.parse("R$ 18,90\nViagem: 8,5 km")

        assertEquals(OfferTextParseStatus.INCOMPLETE, result.status)
        assertNull(result.offer)
        assertEquals(setOf("tempo da viagem"), result.missingFields)
    }
    @Test
    fun parses99ExclusiveOfferLayoutWithPickupMetersAndTripKilometers() {
        val result = parser.parse(
            """
            Pgto. no app
            R$19,00 ⚡1,5x
            R$3,15/km
            R$1,28 Tarifa base dinâmica incl.
            4,88 • 582 corridas • Cartão verif.
            2 min (242 m)
            Rua Achiles Beline, 760, Vila Pauliceia
            19 min (5,8 km)
            Burguesinha Brechó Chic | Osasco - SP
            """.trimIndent()
        )

        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        val offer = result.offer
        assertNotNull(offer)
        assertEquals(1900L, offer!!.receivedFareCents)
        assertEquals(0.242, offer.pickupDistanceKm, 0.001)
        assertEquals(5.8, offer.tripDistanceKm, 0.001)
        assertEquals(2, offer.pickupTimeMinutes)
        assertEquals(19, offer.tripTimeMinutes)
    }

    @Test
    fun parsesUberExclusiveOfferWithTotalPickupAndTripSegments() {
        val result = parser.parse(
            """
            UberX
            Exclusivo
            R$ 13,05
            20 min • 5.7 km • Est. de R$2,29/km
            +R$ 2,25 incluído
            5 min • 1.1 km
            Alameda Shallom Adonai, São Pedro, Osasco
            4,92 (177)
            15 minutos • 4.6 km
            R. Diogo Benites, S/Nº, Vila Osasco, Osasco
            """.trimIndent()
        )

        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        val offer = result.offer
        assertNotNull(offer)
        assertEquals(1305L, offer!!.receivedFareCents)
        assertEquals(1.1, offer.pickupDistanceKm, 0.001)
        assertEquals(4.6, offer.tripDistanceKm, 0.001)
        assertEquals(5, offer.pickupTimeMinutes)
        assertEquals(15, offer.tripTimeMinutes)
    }


    @Test
    fun parsesUberOcrLayoutWhereEmbarqueLineRepeatsTotalSummary() {
        val result = parser.parse(
            """
            R$ 28,34
            Embarque: R. Eduardo Carlos de França Turibio (30 min 15,9 km)
            Destino: R. Duarte da Costa, Parque Imperial (25 min 14,8 km)
            30 min: 15.9 km - Est. de R$1,78/km
            +R$2,75 incluído
            5 min - 1.1 km
            R. Eduardo Carlos de França Turibio, Padroeira, Osasco
            25 minutos * 14.8 km
            R. Duarte da Costa, Barueri
            """.trimIndent()
        )

        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        val offer = result.offer
        assertNotNull(offer)
        assertEquals(2834L, offer!!.receivedFareCents)
        assertEquals(1.1, offer.pickupDistanceKm, 0.001)
        assertEquals(14.8, offer.tripDistanceKm, 0.001)
        assertEquals(5, offer.pickupTimeMinutes)
        assertEquals(25, offer.tripTimeMinutes)
    }

    @Test
    fun parsesUberOcrSummaryWithColonAndAsteriskSeparators() {
        val result = parser.parse(
            """
            R$ 13,05
            20 min: 5.7 km - Est. de R$2,29/km
            +R$2,25 incluído
            5 min e 1.1 km
            Alameda Shallon Adonai, São Pedro, Osasco
            15 minutos * 4.6 km
            R. Diogo Benites, Vila Osasco
            """.trimIndent()
        )

        assertEquals(OfferTextParseStatus.COMPLETE, result.status)
        val offer = result.offer
        assertNotNull(offer)
        assertEquals(1305L, offer!!.receivedFareCents)
        assertEquals(1.1, offer.pickupDistanceKm, 0.001)
        assertEquals(4.6, offer.tripDistanceKm, 0.001)
        assertEquals(5, offer.pickupTimeMinutes)
        assertEquals(15, offer.tripTimeMinutes)
    }

}
