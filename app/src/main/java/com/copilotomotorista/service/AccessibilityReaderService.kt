package com.copilotomotorista.service

import android.accessibilityservice.AccessibilityService
import android.util.Log
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import com.copilotomotorista.domain.usecase.CalculateOfferProfitabilityUseCase

class AccessibilityReaderService : AccessibilityService() {

    private lateinit var overlayWindowManager: OverlayWindowManager
    private lateinit var profitabilityUseCase: CalculateOfferProfitabilityUseCase

    override fun onCreate() {
        super.onCreate()
        overlayWindowManager = OverlayWindowManager(this)
        profitabilityUseCase = CalculateOfferProfitabilityUseCase()
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return
        try {
            scanNodeForOffers(rootNode)
        } catch (e: Exception) {
            Log.e("CopilotoService", "Erro ao processar nós de acessibilidade", e)
        } finally {
            rootNode.recycle()
        }
    }

    private fun scanNodeForOffers(node: AccessibilityNodeInfo) {
        val text = node.text?.toString() ?: ""

        // Heurística moderna de deteção de corrida (Preço + Distância)
        if (text.contains("R$") && text.contains("km")) {
            triggerSimulatedOffer(grossFare = 32.00, distanceKm = 8.5)
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                scanNodeForOffers(child)
                child.recycle()
            }
        }
    }

    private fun triggerSimulatedOffer(grossFare: Double, distanceKm: Double) {
        val analysis = profitabilityUseCase.execute(grossFare, distanceKm)

        overlayWindowManager.showOverlay(
            analysisResult = analysis,
            grossFare = grossFare,
            onDismiss = {
                Log.d("CopilotoService", "Oferta rejeitada pelo motorista")
            },
            onAccept = {
                Log.d("CopilotoService", "Oferta registada com sucesso")
            }
        )
    }

    override fun onInterrupt() {
        overlayWindowManager.hideOverlay()
    }
}