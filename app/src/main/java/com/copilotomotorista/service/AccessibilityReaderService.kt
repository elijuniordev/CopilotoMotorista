package com.copilotomotorista.service

import android.accessibilityservice.AccessibilityService
import android.view.accessibility.AccessibilityEvent

/**
 * Serviço preparado para a futura etapa de captura e interpretação das ofertas.
 *
 * Não tenta inferir uma corrida a partir de palavras isoladas nem apresenta valores
 * fixos como se fossem dados reais. A integração de leitura/OCR será adicionada
 * quando existir um parser validado para as telas dos aplicativos de corrida.
 */
class AccessibilityReaderService : AccessibilityService() {

    private lateinit var overlayWindowManager: OverlayWindowManager

    override fun onCreate() {
        super.onCreate()
        overlayWindowManager = OverlayWindowManager(this)
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        // Intencionalmente sem leitura automática nesta etapa.
    }

    override fun onInterrupt() {
        if (::overlayWindowManager.isInitialized) {
            overlayWindowManager.hideOverlay()
        }
    }

    override fun onDestroy() {
        if (::overlayWindowManager.isInitialized) {
            overlayWindowManager.hideOverlay()
        }
        super.onDestroy()
    }
}
