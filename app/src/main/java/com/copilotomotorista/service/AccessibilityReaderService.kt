package com.copilotomotorista.service

accessibilityService.AccessibilityService
view.accessibility.AccessibilityNodeInfo
content.Context
util.Log
domain.usecase.CalculateOfferProfitabilityUseCase

class AccessibilityReaderService : AccessibilityService() {

    private lateinit var overlayWindowManager: OverlayWindowManager
    private lateinit var profitabilityUseCase: CalculateOfferProfitabilityUseCase

    override fun onCreate() {
        super.onCreate()
        overlayWindowManager = OverlayWindowManager(this)
        profitabilityUseCase = CalculateOfferProfitabilityUseCase()
    }

    override fun onAccessibilityEvent(event: android.view.accessibility.AccessibilityEvent?) {
        val rootNode = rootInActiveWindow ?: return

        // Varre os nós de texto em busca de ofertas da Uber ou 99
        scanNodeForOffers(rootNode)
    }

    private fun scanNodeForOffers(node: AccessibilityNodeInfo) {
        val text = node.text?.toString() ?: ""

        // Exemplo de heurística de deteção de preço de corrida (ex: "R$ 24,50")
        if (text.contains("R$") && text.contains("km")) {
            // Lógica de extração de valores do texto capturado
            // Para testes imediatos, podemos simular uma oferta válida capturada:
            triggerSimulatedOffer(grossFare = 25.0, distanceKm = 7.5)
        }

        for (i in 0 until node.childCount) {
            node.getChild(i)?.let { child ->
                scanNodeForOffers(child)
                child.recycle()
            }
        }
    }

    private fun triggerSimulatedOffer(grossFare: Double, distanceKm: Double) {
        val analysis = profitabilityUseCase.execute(grossFare, distanceKm)

        // Exibe o overlay na tela por cima do app de transporte
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