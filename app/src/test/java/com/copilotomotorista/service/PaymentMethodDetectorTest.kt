package com.copilotomotorista.service

import org.junit.Assert.assertEquals
import org.junit.Test

class PaymentMethodDetectorTest {

    @Test
    fun detectsPixWhenPresentedAsPaymentInformation() {
        assertEquals("Pix", PaymentMethodDetector.detect("Forma de pagamento: Pix"))
        assertEquals("Pix", PaymentMethodDetector.detect("Pix"))
    }

    @Test
    fun detectsCashWhenPresentedAsPaymentInformation() {
        assertEquals("Dinheiro", PaymentMethodDetector.detect("Pagamento em dinheiro"))
        assertEquals("Dinheiro", PaymentMethodDetector.detect("Cash"))
    }

    @Test
    fun detectsCardWhenPresentedAsPaymentInformation() {
        assertEquals("Cartão", PaymentMethodDetector.detect("Pagamento no cartão"))
        assertEquals("Cartão", PaymentMethodDetector.detect("Crédito"))
    }

    @Test
    fun doesNotMistakePassengerCardVerificationForRidePayment() {
        val text = """
            Pgto. no app
            4,88 • 582 corridas • Cartão verif.
            2 min (242 m)
            19 min (5,8 km)
        """.trimIndent()

        assertEquals("Pagamento não identificado", PaymentMethodDetector.detect(text))
    }

    @Test
    fun doesNotInferPaymentMethodFromUnrelatedScreenText() {
        assertEquals(
            "Pagamento não identificado",
            PaymentMethodDetector.detect("UberX\n4,98 • Cartão verificado\n8,5 km")
        )
    }
}
