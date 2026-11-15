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

        val fareCandidates = lines.flatMap {
            FARE_REGEX.findAll(it).mapNotNull { match -> parseCents(match.groupValues[1]) }.toList()
        }.distinct()
        val pickupLines = lines.filter { PICKUP_KEYWORDS.containsMatchIn(it) }
        val tripLines = lines.filter { TRIP_KEYWORDS.containsMatchIn(it) }

        val pickupDistance = extractUniqueNumber(pickupLines, DISTANCE_REGEX)
        val pickupTime = extractUniqueNumber(pickupLines, TIME_REGEX)
        val tripDistance = extractUniqueNumber(tripLines, DISTANCE_REGEX)
        val tripTime = extractUniqueNumber(tripLines, TIME_REGEX)

        val ambiguous = linkedSetOf<String>()
        if (fareCandidates.size > 1) ambiguous += "valor recebido"
        if (pickupDistance.ambiguous) ambiguous += "distância de busca"
        if (pickupTime.ambiguous) ambiguous += "tempo de busca"
        if (tripDistance.ambiguous) ambiguous += "distância da viagem"
        if (tripTime.ambiguous) ambiguous += "tempo da viagem"

        val hasOfferClues = fareCandidates.isNotEmpty() || pickupLines.isNotEmpty() || tripLines.isNotEmpty()
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
        BigDecimal(value.replace(".", "").replace(',', '.'))
            .movePointRight(2)
            .setScale(0, RoundingMode.HALF_UP)
            .longValueExact()
    }.getOrNull()

    private data class NumberCandidate(val value: Double?, val ambiguous: Boolean)

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val FARE_REGEX = Regex("""R\$\s*([0-9.]+(?:,[0-9]{1,2})?|[0-9]+(?:\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val DISTANCE_REGEX = Regex("""([0-9]+(?:[,.][0-9]+)?)\s*km\b""", RegexOption.IGNORE_CASE)
        val TIME_REGEX = Regex("""([0-9]+)\s*(?:min(?:uto)?s?)\b""", RegexOption.IGNORE_CASE)
        val PICKUP_KEYWORDS = Regex("""\b(busca|buscar|embarque|coleta|pickup|passageiro)\b""", RegexOption.IGNORE_CASE)
        val TRIP_KEYWORDS = Regex("""\b(viagem|trajeto|destino)\b""", RegexOption.IGNORE_CASE)
    }
}
