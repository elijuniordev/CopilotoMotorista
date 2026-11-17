package com.copilotomotorista.service

/**
 * Filtro conservador entre OCR bruto e parser financeiro.
 *
 * O OCR pode encontrar preços em mapas, notificações, extratos e outras telas.
 * Só liberamos o parser quando há uma combinação de sinais de uma oferta real:
 * valor em moeda + estrutura de rota + vocabulário/indicadores da tela de ofertas.
 */
internal class OfferScreenClassifier {

    enum class ScreenKind {
        NOT_OFFER,
        SINGLE_OFFER,
        RADAR_CANDIDATE
    }

    data class Result(
        val kind: ScreenKind,
        val evidence: Set<String> = emptySet()
    )

    fun classify(rawText: String): Result {
        val text = rawText
            .lowercase()
            .replace(',', '.')
            .replace(WHITESPACE, " ")
        if (text.isBlank()) return Result(ScreenKind.NOT_OFFER)

        val fareCount = FARE_REGEX.findAll(text).count()
        val routeSegmentCount = ROUTE_SEGMENT_REGEX.findAll(text).count()
        val hasLabeledPickup = PICKUP_REGEX.containsMatchIn(text)
        val hasLabeledDestination = DESTINATION_REGEX.containsMatchIn(text)
        val hasPlatformOfferMarker = PLATFORM_OFFER_MARKER_REGEX.containsMatchIn(text)
        val hasFareRateMarker = FARE_RATE_MARKER_REGEX.containsMatchIn(text)
        val hasOfferAction = OFFER_ACTION_REGEX.containsMatchIn(text)
        val hasRadarLabel = RADAR_LABEL_REGEX.containsMatchIn(text)

        // Radar é uma tela com várias opções, não uma oferta exclusiva. Não a
        // enviamos ao parser de oferta única: isso evita um card inventado com
        // valores pertencentes a cartões diferentes.
        if (hasRadarLabel && hasOfferAction && fareCount >= 2 && routeSegmentCount >= 2) {
            return Result(
                ScreenKind.RADAR_CANDIDATE,
                setOf("radar_label", "multiple_fares", "multiple_route_segments", "offer_action")
            )
        }

        val hasRouteStructure = routeSegmentCount >= 2 ||
            (routeSegmentCount >= 1 && hasPlatformOfferMarker) ||
            (hasLabeledPickup && hasLabeledDestination && DISTANCE_OR_TIME_REGEX.containsMatchIn(text))
        val hasOfferContext = hasPlatformOfferMarker || hasFareRateMarker ||
            (hasLabeledPickup && hasLabeledDestination)

        if (fareCount >= 1 && hasRouteStructure && hasOfferContext) {
            val evidence = linkedSetOf<String>()
            evidence += "fare"
            if (routeSegmentCount >= 2) evidence += "route_segments"
            if (hasLabeledPickup) evidence += "pickup_label"
            if (hasLabeledDestination) evidence += "destination_label"
            if (hasPlatformOfferMarker) evidence += "platform_offer_marker"
            if (hasFareRateMarker) evidence += "fare_rate_marker"
            return Result(ScreenKind.SINGLE_OFFER, evidence)
        }

        return Result(ScreenKind.NOT_OFFER)
    }

    private companion object {
        val WHITESPACE = Regex("\\s+")
        val FARE_REGEX = Regex("""r\$\s*[0-9]+(?:\.[0-9]{3})*(?:[,.][0-9]{1,2})?""")
        val ROUTE_SEGMENT_REGEX = Regex(
            """[0-9]{1,3}\s*min(?:uto)?s?\s*(?:[:•·*e+«»~-]\s*|\(\s*)[0-9]+(?:[,.][0-9]+)?\s*(?:km|m)"""
        )
        val DISTANCE_OR_TIME_REGEX = Regex("""[0-9]+(?:[,.][0-9]+)?\s*km|[0-9]+\s*min""")
        val PICKUP_REGEX = Regex("""\b(embarque|busca|buscar|pickup|coleta)\b""")
        val DESTINATION_REGEX = Regex("""\b(destino|viagem|trajeto)\b""")
        val PLATFORM_OFFER_MARKER_REGEX = Regex(
            """\b(uberx|uber comfort|uber black|uberx exclusivo|exclusivo|priority|99pop|99plus|99 comfort|pgto\.? no app|verificado)\b"""
        )
        val FARE_RATE_MARKER_REGEX = Regex(
            """r\$?\s*/\s*km|r\$?\s*/\s*hora|est\.?\s*de\s*r\$|tarifa\s+base|din[aâ]mic[oa]"""
        )
        val OFFER_ACTION_REGEX = Regex("""\b(tenho interesse|ver oferta|selecionar oferta)\b""")
        val RADAR_LABEL_REGEX = Regex("""radar\s+(?:de\s+)?viagens""")
    }
}
