package com.copilotomotorista.domain.usecase

import com.copilotomotorista.domain.model.OfferInput
import com.copilotomotorista.domain.model.Vehicle
import com.copilotomotorista.domain.model.VehicleEnergy
import com.copilotomotorista.domain.model.VehicleOperatingCosts

/** Compatibilidade temporária. Novos fluxos devem usar EvaluateOfferUseCase. */
class CalculateOfferProfitabilityUseCase {
    @Deprecated("Use EvaluateOfferUseCase com veículo e OfferInput configurados.")
    fun execute(grossFare: Double, totalDistanceKm: Double): OfferAnalysisResult {
        require(grossFare >= 0.0 && grossFare.isFinite()) { "Valor recebido inválido." }
        require(totalDistanceKm > 0.0 && totalDistanceKm.isFinite()) { "Distância inválida." }
        val vehicle = Vehicle(
            id = "legacy-estimate",
            name = "Estimativa temporária",
            energy = VehicleEnergy.Electric(15.0, 100L),
            operatingCosts = VehicleOperatingCosts(0L, 0L)
        )
        return EvaluateOfferUseCase().execute(
            OfferInput((grossFare * 100.0).toLong(), 0.0, totalDistanceKm, 0, 60),
            vehicle
        )
    }
}