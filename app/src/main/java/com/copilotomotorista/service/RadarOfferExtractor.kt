package com.copilotomotorista.service

import com.copilotomotorista.domain.model.OfferInput

/**
 * Separa ofertas do Radar antes de chamar o parser de oferta única.
 *
 * O OCR local entrega texto em ordem de leitura, sem coordenadas confiáveis de
 * cartões. Por segurança, cada tarifa principal inicia um bloco e o bloco termina
 * antes da próxima tarifa. Só são devolvidos blocos completos e com ação de oferta;
 * se o OCR não preservar essa sequência, o resultado fica vazio em vez de combinar
 * valores e trajetos pertencentes a cartões diferentes.
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

        val anchors = lines.indices.filter { index -> isPrimaryFareAnchor(lines[index]) }
        if (anchors.size < MINIMUM_RADAR_OFFERS) return emptyList()

        return anchors.mapIndexedNotNull { anchorPosition, start ->
            val endExclusive = anchors.getOrNull(anchorPosition + 1) ?: lines.size
            val block = lines.subList(start, endExclusive)
            val blockText = block.joinToString("\n")
            if (!OFFER_ACTION_REGEX.containsMatchIn(blockText)) return@mapIndexedNotNull null

            val parsed = parser.parse(blockText)
            val offer = parsed.offer
            if (parsed.status != OfferTextParseStatus.COMPLETE || offer == null) {
                return@mapIndexedNotNull null
            }
            Candidate(
                position = anchorPosition + 1,
                rawText = blockText,
                offer = offer
            )
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

    private fun isPrimaryFareAnchor(line: String): Boolean {
        if (SECONDARY_MARKER_REGEX.containsMatchIn(line)) return false
        return PRIMARY_FARE_REGEX.findAll(line).count() == 1
    }

    private companion object {
        const val MINIMUM_RADAR_OFFERS = 2
        val WHITESPACE = Regex("\\s+")
        val PRIMARY_FARE_REGEX = Regex(
            """R\\$\\s*[0-9]+(?:\\.[0-9]{3})*(?:,[0-9]{1,2}|\\.[0-9]{1,2})?""",
            RegexOption.IGNORE_CASE
        )
        val SECONDARY_MARKER_REGEX = Regex(
            """(?:/\\s*km|por\\s*km|tarifa\\s+base|din[aâ]mic[oa]|b[oô]nus|adicional|inclu[ií]d[oa])""",
            RegexOption.IGNORE_CASE
        )
        val OFFER_ACTION_REGEX = Regex(
            """\\b(tenho\\s+interesse|ver\\s+oferta|selecionar\\s+oferta)\\b""",
            RegexOption.IGNORE_CASE
        )
    }
}
