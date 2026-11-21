package com.copilotomotorista.service

import com.copilotomotorista.domain.model.OfferInput

/**
 * Separa ofertas do Radar antes de chamar o parser de oferta única.
 *
 * O OCR local entrega texto em ordem de leitura, sem coordenadas confiáveis de
 * cartões. Cada tarifa principal inicia um bloco, encerrado antes da próxima tarifa.
 * Só devolvemos blocos completos e com ação de oferta; se o OCR não preservar essa
 * sequência, o resultado fica vazio em vez de combinar cartões diferentes.
 */
internal class RadarOfferExtractor(
    private val parser: OfferTextParser = OfferTextParser()
) {
    data class Candidate(
        val position: Int,
        val rawText: String,
        val offer: OfferInput
    )

    fun extract(rawText: String): List<Candidate> {
        val lines = rawText.lineSequence()
            .map { it.trim().replace(WHITESPACE, " ") }
            .filter { it.isNotEmpty() }
            .toList()
        if (lines.isEmpty()) return emptyList()

        val anchors = lines.indices.filter { isPrimaryFareAnchor(lines[it]) }
        if (anchors.size < MINIMUM_RADAR_OFFERS) return emptyList()

        return anchors.mapIndexedNotNull { anchorPosition, start ->
            val endExclusive = anchors.getOrNull(anchorPosition + 1) ?: lines.size
            val blockText = lines.subList(start, endExclusive).joinToString("\n")
            if (!OFFER_ACTION_REGEX.containsMatchIn(blockText)) return@mapIndexedNotNull null

            val parsed = parser.parse(blockText)
            val offer = parsed.offer
            if (parsed.status != OfferTextParseStatus.COMPLETE || offer == null) {
                return@mapIndexedNotNull null
            }
            Candidate(anchorPosition + 1, blockText, offer)
        }.distinctBy { candidate ->
            listOf(
                candidate.offer.receivedFareCents,
                candidate.offer.pickupDistanceKm,
                candidate.offer.tripDistanceKm,
                candidate.offer.pickupTimeMinutes,
                candidate.offer.tripTimeMinutes
            )
        }
    }

    companion object {
        /**
         * Count only main fare lines. Per-kilometre rates, fare-base figures
         * and bonuses must not be counted as separate Radar cards.
         * Share this check with the screen classifier so the two cannot drift.
         */
        internal fun countPrimaryFareAnchors(rawText: String): Int =
            rawText.lineSequence()
                .map { it.trim().replace(WHITESPACE, " ") }
                .count(::isPrimaryFareAnchor)

        private fun isPrimaryFareAnchor(line: String): Boolean {
            if (SECONDARY_MARKER_REGEX.containsMatchIn(line)) return false
            return PRIMARY_FARE_REGEX.findAll(line).count() == 1
        }

        private const val MINIMUM_RADAR_OFFERS = 2
        val WHITESPACE = Regex("\\s+")
        val PRIMARY_FARE_REGEX = Regex(
            """R\$\s*[0-9]+(?:\.[0-9]{3})*(?:,[0-9]{1,2}|\.[0-9]{1,2})?""",
            RegexOption.IGNORE_CASE
        )
        val SECONDARY_MARKER_REGEX = Regex(
            """(?:/\s*km|por\s*km|tarifa\s+base|din[aâ]mic[oa]|b[oô]nus|adicional|inclu[ií]d[oa])""",
            RegexOption.IGNORE_CASE
        )
        val OFFER_ACTION_REGEX = Regex(
            """\b(tenho\s+interesse|ver\s+oferta|selecionar\s+oferta)\b""",
            RegexOption.IGNORE_CASE
        )
    }
}
