package com.copilotomotorista.service

/**
 * Identifies a payment method only when the screen presents it as payment
 * information. Passenger verification badges must not be interpreted as
 * the method used to pay for the ride.
 */
internal object PaymentMethodDetector {

    fun detect(text: String): String {
        val relevantLines = text.lineSequence()
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .filter { line ->
                PAYMENT_CONTEXT_REGEX.containsMatchIn(line) ||
                    EXPLICIT_METHOD_LINE_REGEX.matches(line)
            }
            .joinToString(" ")
            .lowercase()

        return when {
            PIX_REGEX.containsMatchIn(relevantLines) -> "Pix"
            CASH_REGEX.containsMatchIn(relevantLines) -> "Dinheiro"
            CARD_REGEX.containsMatchIn(relevantLines) -> "Cartão"
            else -> "Pagamento não identificado"
        }
    }

    private val PAYMENT_CONTEXT_REGEX = Regex(
        """\b(pagamento|pgto\.?|forma\s+de\s+pagamento|recebimento|receber|pagar|pago)\b""",
        RegexOption.IGNORE_CASE
    )
    private val EXPLICIT_METHOD_LINE_REGEX = Regex(
        """\s*(pix|dinheiro|cash|cart[aã]o|cr[eé]dito|d[eé]bito)\s*[.!]?\s*""",
        RegexOption.IGNORE_CASE
    )
    private val PIX_REGEX = Regex("""\bpix\b""", RegexOption.IGNORE_CASE)
    private val CASH_REGEX = Regex("""\b(dinheiro|cash)\b""", RegexOption.IGNORE_CASE)
    private val CARD_REGEX = Regex(
        """\b(cart[aã]o|cr[eé]dito|d[eé]bito)\b""",
        RegexOption.IGNORE_CASE
    )
}
