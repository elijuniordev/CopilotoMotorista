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

        // Varre os nós de texto em busca de ofertas da Uber ou 99
        scanNodeForOffers(rootNode)
    }

    private fun scanNodeForOffers(node: AccessibilityNodeInfo) {
        val text = node.text?.toString() ?: ""

        // Detecção heurística de preço e distância na tela de oferta
        if (text.contains("R$") && text.contains("km")) {
            // Simulação controlada de oferta capturada para validação de bancada
            triggerSimulatedOffer(grossFare = 25.0, distanceKm = 7.5)
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

        // Exibe o overlay OLED de Cognição Zero por cima do app de transporte
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