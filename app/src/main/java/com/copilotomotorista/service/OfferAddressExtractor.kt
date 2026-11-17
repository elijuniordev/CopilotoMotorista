package com.copilotomotorista.service

/**
 * Extracts pickup and destination labels when available. For layouts that omit
 * labels (notably some 99 screens), it uses the line immediately following
 * each time/distance segment, but only when that line looks like a place.
 */
internal object OfferAddressExtractor {

    fun extract(text: String, origin: Boolean): String? {
        val lines = text.lineSequence()
            .map { it.trim().replace(WHITESPACE, " ") }
            .filter { it.isNotEmpty() }
            .toList()

        val labels = if (origin) ORIGIN_LABELS else DESTINATION_LABELS
        val labeled = lines.firstNotNullOfOrNull { line ->
            val normalized = line.lowercase()
            val label = labels.firstOrNull { normalized.startsWith(it) }
            label?.let { cleanAddress(line.substring(it.length)) }
                ?.takeIf(::isAddressLike)
        }
        if (labeled != null) return labeled

        val segmentLines = lines.indices.filter { index ->
            TIME_DISTANCE_REGEX.containsMatchIn(lines[index])
        }
        val segmentIndex = if (origin) segmentLines.firstOrNull() else segmentLines.lastOrNull()
        val candidate = segmentIndex?.let { lines.getOrNull(it + 1) }
        return candidate?.let(::cleanAddress)?.takeIf(::isAddressLike)
    }

    private fun cleanAddress(value: String): String? {
        val cleaned = value.trim()
            .replace(TRAILING_SEGMENT_REGEX, "")
            .trim(' ', '-', '•', '·', ':')
        return cleaned.takeIf { it.length >= MIN_ADDRESS_LENGTH }
    }

    private fun isAddressLike(value: String): Boolean {
        val normalized = value.lowercase()
        if (NON_ADDRESS_REGEX.containsMatchIn(normalized)) return false
        return ADDRESS_HINT_REGEX.containsMatchIn(normalized)
    }

    private val WHITESPACE = Regex("\\s+")
    private val ORIGIN_LABELS = listOf("origem:", "embarque:", "partida:", "buscar em:", "buscar:")
    private val DESTINATION_LABELS = listOf("destino:", "desembarque:", "chegada:", "deixar em:", "levar para:")
    private val TIME_DISTANCE_REGEX = Regex(
        """\\b[0-9]+\\s*min(?:uto)?s?\\s*(?:[:•·:*e-]\\s*|\\(\\s*)[0-9]+(?:[,.][0-9]+)?\\s*(?:m|km)""",
        RegexOption.IGNORE_CASE
    )
    private val TRAILING_SEGMENT_REGEX = Regex(
        """\\s*\\(\\s*[0-9]+\\s*min(?:uto)?s?\\s+[0-9]+(?:[,.][0-9]+)?\\s*(?:m|km)\\s*\\)\\s*$""",
        RegexOption.IGNORE_CASE
    )
    private val ADDRESS_HINT_REGEX = Regex(
        """\\b(rua|r\\.|avenida|av\\.?|estrada|travessa|alameda|praça|rodovia|vila|bairro|osasco|barueri|são paulo|sp)\\b|[,|]""",
        RegexOption.IGNORE_CASE
    )
    private val NON_ADDRESS_REGEX = Regex(
        """\\b(uberx|99pop|corridas|cart[aã]o verif|avaliação|minutos? restantes)\\b|^r\\$""",
        RegexOption.IGNORE_CASE
    )
    private const val MIN_ADDRESS_LENGTH = 8
}
