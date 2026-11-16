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
            .filterNot { NON_FARE_AMOUNT_REGEX.containsMatchIn(it) }
            .flatMap {
                FARE_REGEX.findAll(it).mapNotNull { match -> parseCents(match.groupValues[1]) }.toList()
            }.distinct()

        val pickupLines = lines.filter { PICKUP_KEYWORDS.containsMatchIn(it) }
        val tripLines = lines.filter { TRIP_KEYWORDS.containsMatchIn(it) }

        var pickupDistance = extractUniqueNumber(pickupLines, DISTANCE_REGEX)
        var pickupTime = extractUniqueNumber(pickupLines, TIME_REGEX)
        var tripDistance = extractUniqueNumber(tripLines, DISTANCE_REGEX)
        var tripTime = extractUniqueNumber(tripLines, TIME_REGEX)

        // Layout observado na oferta Uber: "2 min (242 m)" e "19 min (5,8 km)".
        // Quando os rótulos não existem, a primeira linha em metros é a busca
        // e a linha em quilômetros é o trajeto com passageiro.
        val unlabeledSegments = lines.mapNotNull { line ->
            UNLABELED_TIME_DISTANCE_REGEX.find(line)?.let { match ->
                val minutes = match.groupValues[1].toIntOrNull() ?: return@let null
                val distance = match.groupValues[2].replace(',', '.').toDoubleOrNull()
                    ?: return@let null
                val unit = match.groupValues[3].lowercase()
                ParsedSegment(minutes, if (unit == "m") distance / 1000.0 else distance, unit)
            }
        }
        if (pickupDistance.value == null && pickupTime.value == null) {
            val pickup = unlabeledSegments.firstOrNull { it.unit == "m" }
            if (pickup != null) {
                pickupDistance = NumberCandidate(pickup.distanceKm, false)
                pickupTime = NumberCandidate(pickup.minutes.toDouble(), false)
            }
        }
        if (tripDistance.value == null && tripTime.value == null) {
            val trip = unlabeledSegments.firstOrNull { it.unit == "km" }
            if (trip != null) {
                tripDistance = NumberCandidate(trip.distanceKm, false)
                tripTime = NumberCandidate(trip.minutes.toDouble(), false)
            }
        }

        val ambiguous = linkedSetOf<String>()
        if (fareCandidates.size > 1) ambiguous += "valor recebido"
        if (pickupDistance.ambiguous) ambiguous += "distância de busca"
        if (pickupTime.ambiguous) ambiguous += "tempo de busca"
        if (tripDistance.ambiguous) ambiguous += "distância da viagem"
        if (tripTime.ambiguous) ambiguous += "tempo da viagem"

        val hasOfferClues = fareCandidates.isNotEmpty() || pickupLines.isNotEmpty() ||
            tripLines.isNotEmpty() || unlabeledSegments.isNotEmpty()
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
        val FARE_REGEX = Regex("""R\\$\\s*([0-9.]+(?:,[0-9]{1,2})?|[0-9]+(?:\\.[0-9]{1,2})?)""", RegexOption.IGNORE_CASE)
        val NON_FARE_AMOUNT_REGEX = Regex(
            """R\\$\\s*[0-9.,]+\\s*/\\s*km|tarifa\\s+base|din[aâ]mica""",
            RegexOption.IGNORE_CASE
        )
        val DISTANCE_REGEX = Regex("""([0-9]+(?:[,.][0-9]+)?)\\s*km\\b""", RegexOption.IGNORE_CASE)
        val TIME_REGEX = Regex("""([0-9]+)\\s*(?:min(?:uto)?s?)\\b""", RegexOption.IGNORE_CASE)
        val UNLABELED_TIME_DISTANCE_REGEX = Regex(
            """([0-9]+)\\s*min(?:uto)?s?\\s*\\(\\s*([0-9]+(?:[,.][0-9]+)?)\\s*(m|km)\\s*\\)""",
            RegexOption.IGNORE_CASE
        )
        val PICKUP_KEYWORDS = Regex("""\\b(busca|buscar|embarque|coleta|pickup|passageiro)\\b""", RegexOption.IGNORE_CASE)
        val TRIP_KEYWORDS = Regex("""\\b(viagem|trajeto|destino)\\b""", RegexOption.IGNORE_CASE)
    }
}
