package com.copilotomotorista.service

import com.copilotomotorista.domain.model.OfferInput
import java.math.BigDecimal
import java.math.RoundingMode

enum class OfferTextParseStatus { NOT_OFFER, INCOMPLETE, AMBIGUOUS, COMPLETE }

data class OfferTextParseResult(
    val status: OfferTextParseStatus,
    val offer: OfferInput? = null,
    val missingFields: Set<String> = emptySet(),
    val ambiguousFields: Set<String> = emptySet()
)

class OfferTextParser {

    fun parse(rawText: String): OfferTextParseResult {
        val lines = rawText.lineSequence()
            .map { it.trim().replace(WHITESPACE, " ") }
            .filter { it.isNotEmpty() }
            .toList()

        if (lines.isEmpty()) return OfferTextParseResult(OfferTextParseStatus.NOT_OFFER)

        // Uber/99 podem mostrar, na mesma tela, valor da oferta, R$/km e
        // componentes da tarifa dinâmica. Só o valor principal é uma tarifa recebida.
        val fareCandidates = lines
            .flatMap(::extractPrimaryFareCandidates)
            .distinct()

        val pickupLines = lines.filter { PICKUP_KEYWORDS.containsMatchIn(it) }
        val tripLines = lines.filter { TRIP_KEYWORDS.containsMatchIn(it) }

        var pickupDistance = extractUniqueNumber(pickupLines, DISTANCE_REGEX)
        var pickupTime = extractUniqueNumber(pickupLines, TIME_REGEX)
        var tripDistance = extractUniqueNumber(tripLines, DISTANCE_REGEX)
        var tripTime = extractUniqueNumber(tripLines, TIME_REGEX)

        // Ofertas exclusivas 99: "2 min (242 m)" e "19 min (5,8 km)".
        // Uber: resumo total "20 min • 5.7 km", depois busca e trajeto separados.
        // OCR pode repetir o mesmo segmento em um resumo e em uma linha de endereço.
        // Normalizamos metros para km e removemos duplicatas antes de procurar pares.
        val segments = lines.mapNotNull { line ->
            UNLABELED_TIME_DISTANCE_REGEX.find(line)?.let { match ->
                val minutes = match.groupValues[1].toIntOrNull() ?: return@let null
                val distance = match.groupValues[2].replace(',', '.').toDoubleOrNull()
                    ?: return@let null
                val unit = match.groupValues[3].lowercase()
                ParsedSegment(minutes, if (unit == "m") distance / 1000.0 else distance, unit)
            }
        }.distinctBy { it.minutes to it.distanceKm }

        // Na tela de oferta da Uber, a linha "30 min: 15,9 km - Est. de ..."
        // representa o total desde a busca até o destino. As linhas seguintes
        // mostram busca e viagem separadamente. O texto OCR pode repetir o total
        // nas linhas "Embarque"; por isso priorizamos o par que soma o resumo.
        val totalSummary = lines.firstNotNullOfOrNull { line ->
            TOTAL_SUMMARY_REGEX.find(line)?.let { match ->
                ParsedSegment(
                    match.groupValues[1].toInt(),
                    match.groupValues[2].replace(',', '.').toDouble(),
                    "km"
                )
            }
        }
        val matchingSegmentPairs = totalSummary?.let { total ->
            segments.asSequence()
                .filterNot { segment ->
                    segment.minutes == total.minutes &&
                        kotlin.math.abs(segment.distanceKm - total.distanceKm) <= 0.05
                }
                .toList()
                .let { candidates ->
                    candidates.indices.asSequence().flatMap { first ->
                        (first + 1 until candidates.size).asSequence().map { second ->
                            candidates[first] to candidates[second]
                        }
                    }.filter { (pickup, trip) ->
                        kotlin.math.abs(total.minutes - pickup.minutes - trip.minutes) <= 1 &&
                            kotlin.math.abs(total.distanceKm - pickup.distanceKm - trip.distanceKm) <= 0.2
                    }.distinctBy { (pickup, trip) ->
                        listOf(pickup.minutes, pickup.distanceKm, trip.minutes, trip.distanceKm)
                    }.toList()
                }
        }.orEmpty()
        var segmentPairAmbiguous = matchingSegmentPairs.size > 1
        val segmentPair = matchingSegmentPairs.singleOrNull()

        if (segmentPair != null) {
            val (pickup, trip) = segmentPair
            pickupDistance = NumberCandidate(pickup.distanceKm, false)
            pickupTime = NumberCandidate(pickup.minutes.toDouble(), false)
            tripDistance = NumberCandidate(trip.distanceKm, false)
            tripTime = NumberCandidate(trip.minutes.toDouble(), false)
        } else if (!segmentPairAmbiguous) {
            val hasPickupDetails = pickupDistance.value != null || pickupTime.value != null
            val hasTripDetails = tripDistance.value != null || tripTime.value != null

            when {
                // Sem resumo total ou rótulos, só duas combinações distintas permitem
                // inferir com segurança: a primeira é busca e a segunda é viagem.
                segments.size == 2 -> {
                    val pickup = segments.first()
                    val trip = segments.last()
                    if (!hasPickupDetails) {
                        pickupDistance = NumberCandidate(pickup.distanceKm, false)
                        pickupTime = NumberCandidate(pickup.minutes.toDouble(), false)
                    }
                    if (!hasTripDetails) {
                        tripDistance = NumberCandidate(trip.distanceKm, false)
                        tripTime = NumberCandidate(trip.minutes.toDouble(), false)
                    }
                }
                // Com três ou mais segmentos sem rótulos/resumo, escolher first/last
                // seria arbitrário. Mantemos a captura para revisão em vez de inventar.
                segments.size > 2 && (!hasPickupDetails || !hasTripDetails) -> {
                    segmentPairAmbiguous = true
                }
            }
        }
        if (pickupDistance.value == null && pickupTime.value == null && segments.size == 1 && segments.single().unit == "m") {
            pickupDistance = NumberCandidate(segments.single().distanceKm, false)
            pickupTime = NumberCandidate(segments.single().minutes.toDouble(), false)
        }

        val ambiguous = linkedSetOf<String>()
        if (segmentPairAmbiguous) ambiguous += "segmentos de busca e viagem"
        if (fareCandidates.size > 1) ambiguous += "valor recebido"
        if (pickupDistance.ambiguous) ambiguous += "distância de busca"
        if (pickupTime.ambiguous) ambiguous += "tempo de busca"
        if (tripDistance.ambiguous) ambiguous += "distância da viagem"
        if (tripTime.ambiguous) ambiguous += "tempo da viagem"

        val hasOfferClues = fareCandidates.isNotEmpty() || pickupLines.isNotEmpty() ||
            tripLines.isNotEmpty() || segments.isNotEmpty()
        if (!hasOfferClues) return OfferTextParseResult(OfferTextParseStatus.NOT_OFFER)
        if (ambiguous.isNotEmpty()) {
            return OfferTextParseResult(OfferTextParseStatus.AMBIGUOUS, ambiguousFields = ambiguous)
        }

        val missing = linkedSetOf<String>()
        if (fareCandidates.isEmpty()) missing += "valor recebido"
        if (tripDistance.value == null) missing += "distância da viagem"
        if (tripTime.value == null) missing += "tempo da viagem"

        if (missing.isNotEmpty()) {
            return OfferTextParseResult(OfferTextParseStatus.INCOMPLETE, missingFields = missing)
        }

        val offer = runCatching {
            OfferInput(
                receivedFareCents = fareCandidates.single(),
                pickupDistanceKm = pickupDistance.value ?: 0.0,
                tripDistanceKm = tripDistance.value!!,
                pickupTimeMinutes = pickupTime.value?.toInt() ?: 0,
                tripTimeMinutes = tripTime.value!!.toInt()
            )
        }.getOrNull() ?: return OfferTextParseResult(
            OfferTextParseStatus.INCOMPLETE,
            missingFields = setOf("dados de distância ou tempo inválidos")
        )

        return OfferTextParseResult(OfferTextParseStatus.COMPLETE, offer = offer)
    }

    private fun extractPrimaryFareCandidates(line: String): List<Long> =
        FARE_REGEX.findAll(line).mapNotNull { match ->
            val before = line.substring(0, match.range.first).takeLast(48)
            val after = line.substring(match.range.last + 1).take(48)
            if (isSecondaryFareAmount(before, after)) null
            else parseCents(match.groupValues[1])
        }.toList()

    private fun isSecondaryFareAmount(before: String, after: String): Boolean =
        SECONDARY_FARE_BEFORE_REGEX.containsMatchIn(before) ||
            SECONDARY_FARE_AFTER_REGEX.containsMatchIn(after)

    private fun extractUniqueNumber(lines: List<String>, regex: Regex): NumberCandidate {
        val values = lines.flatMap { line ->
            regex.findAll(line).mapNotNull { match ->
                match.groupValues[1].replace(',', '.').toDoubleOrNull()
            }.toList()
        }.distinct()
        return when {
            values.size > 1 -> NumberCandidate(null, ambiguous = true)
            values.isEmpty() -> NumberCandidate(null, ambiguous = false)
            else -> NumberCandidate(values.single(), ambiguous = false)
        }
    }

    private fun parseCents(value: String): Long? = runCatching {
        val normalized = when {
            value.contains(',') -> value.replace(".", "").replace(',', '.')
            value.contains('.') && value.substringAfterLast('.').length in 1..2 -> value
            else -> value.replace(".", "")
        }
        BigDecimal(normalized)
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()

    private data class NumberCandidate(val value: Double?, val ambiguous: Boolean)
    private data class ParsedSegment(val minutes: Int, val distanceKm: Double, val unit: String)

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val FARE_REGEX = Regex("""R\$\s*([0-9.]+(?:,[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        // Classifica cada ocorrência de moeda isoladamente. Filtrar a linha inteira
        // descartava também o valor principal quando OCR concatenava oferta, R$/km
        // e componentes secundários na mesma linha.
        val SECONDARY_FARE_BEFORE_REGEX = Regex(
            """(?:tarifa\s+base|din[aâ]mic[oa]|b[oô]nus|adicional|estimad[oa]|est\.?\s*de)\s*[:：-]?\s*$""",
            RegexOption.IGNORE_CASE
        )
        val SECONDARY_FARE_AFTER_REGEX = Regex(
            """^\s*(?:/\s*km|por\s*km|km\b|tarifa\s+base|din[aâ]mic[oa]|inclu[ií]d[oa]|(?:de\s+)?b[oô]nus|(?:de\s+)?adicional)""",
            RegexOption.IGNORE_CASE
        )
        val DISTANCE_REGEX = Regex("""([0-9]+(?:[,.][0-9]+)?)\s*km\b""", RegexOption.IGNORE_CASE)
        val TIME_REGEX = Regex("""([0-9]+)\s*(?:min(?:uto)?s?)\b""", RegexOption.IGNORE_CASE)
        val UNLABELED_TIME_DISTANCE_REGEX = Regex(
            """([0-9]+)\s*min(?:uto)?s?\s*(?:[:•·:*e-]\s*|\(\s*)([0-9]+(?:[,.][0-9]+)?)\s*(m|km)\s*\)?""",
            RegexOption.IGNORE_CASE
        )
        val TOTAL_SUMMARY_REGEX = Regex(
            """([0-9]+)\s*min(?:uto)?s?\s*[:•·]\s*([0-9]+(?:[,.][0-9]+)?)\s*km\s*[-•·]\s*Est\.?\s*de\s*R\$""",
            RegexOption.IGNORE_CASE
        )
        val PICKUP_KEYWORDS = Regex("""\b(busca|buscar|embarque|coleta|pickup|passageiro)\b""", RegexOption.IGNORE_CASE)
        val TRIP_KEYWORDS = Regex("""\b(viagem|trajeto|destino)\b""", RegexOption.IGNORE_CASE)
    }
}
