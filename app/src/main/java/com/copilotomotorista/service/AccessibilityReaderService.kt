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
            Log.e("CopilotoService", "Erro na leitura de acessibilidade", e)
        } finally {
            rootNode.recycle()
        }
    }

    private fun scanNodeForOffers(node: AccessibilityNodeInfo) {
        val text = node.text?.toString() ?: ""

        if (text.contains("R$") && text.contains("km")) {
            val analysis = profitabilityUseCase.execute(32.0, 8.5)
            overlayWindowManager.showOverlay(
                analysisResult = analysis,
                grossFare = 32.0,
                onDismiss = {},
                onAccept = {}
            )
        }

        for (i in 0 until node.childCount) {
            val child = node.getChild(i)
            if (child != null) {
                scanNodeForOffers(child)
                child.recycle()
            }
        }
    }

    override fun onInterrupt() {
        overlayWindowManager.hideOverlay()
    }
}