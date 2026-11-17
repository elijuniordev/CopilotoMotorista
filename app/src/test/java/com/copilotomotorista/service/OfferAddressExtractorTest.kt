package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class OfferAddressExtractorTest {

    @Test
    fun extractsLabeledUberAddressesAndRemovesRepeatedRouteMetrics() {
        val text = """
            Embarque: R. Eduardo Carlos de França Turibio (30 min 15,9 km)
            Destino: R. Duarte da Costa, Parque Imperial (25 min 14,8 km)
            30 min: 15.9 km - Est. de R$1,78/km
        """.trimIndent()

        assertEquals(
            "R. Eduardo Carlos de França Turibio",
            OfferAddressExtractor.extract(text, origin = true)
        )
        assertEquals(
            "R. Duarte da Costa, Parque Imperial",
            OfferAddressExtractor.extract(text, origin = false)
        )
    }

    @Test
    fun extractsUnlabeled99AddressesFromLinesAfterPickupAndTripSegments() {
        val text = """
            2 min (242 m)
            Rua Achiles Beline, 760, Vila Pauliceia
            19 min (5,8 km)
            Burguesinha Brechó Chic | Osasco - SP
        """.trimIndent()

        assertEquals(
            "Rua Achiles Beline, 760, Vila Pauliceia",
            OfferAddressExtractor.extract(text, origin = true)
        )
        assertEquals(
            "Burguesinha Brechó Chic | Osasco - SP",
            OfferAddressExtractor.extract(text, origin = false)
        )
    }

    @Test
    fun doesNotTreatPassengerRatingOrFareMetadataAsAnAddress() {
        val text = """
            4,88 • 582 corridas • Cartão verif.
            R$ 19,00 • R$3,15/km
            2 min (242 m)
            4,98 (177)
            19 min (5,8 km)
        """.trimIndent()

        assertNull(OfferAddressExtractor.extract(text, origin = true))
        assertNull(OfferAddressExtractor.extract(text, origin = false))
    }

    @Test
    fun returnsNullWhenNoAddressOrRouteContextExists() {
        assertNull(
            OfferAddressExtractor.extract(
                "UberX\nR$ 20,00\nOferta exclusiva",
                origin = true
            )
        )
    }
}
