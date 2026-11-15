package com.copilotomotorista.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.view.accessibility.AccessibilityEvent
import android.os.SystemClock

/**
 * Fundação da captura de ofertas.
 *
 * Nesta subetapa o serviço apenas limita os eventos aos aplicativos suportados
 * e aplica debounce. A leitura de textos, OCR e interpretação serão adicionados
 * nas próximas subetapas, após testes próprios.
 */
class AccessibilityReaderService : AccessibilityService() {

    private lateinit var overlayWindowManager: OverlayWindowManager
    private val eventGate = AccessibilityEventGate()

    override fun onCreate() {
        super.onCreate()
        overlayWindowManager = OverlayWindowManager(this)
    }

    override fun onServiceConnected() {
        super.onServiceConnected()

        serviceInfo = serviceInfo.apply {
            packageNames = SupportedRideApps.packages.toTypedArray()
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100L
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return

        val packageName = event.packageName?.toString()
        val accepted = eventGate.shouldAccept(
            packageName = packageName,
            eventType = event.eventType,
            nowMillis = SystemClock.uptimeMillis()
        )
        if (!accepted) return

        // Ponto de entrada validado para as próximas etapas.
        // Nenhum texto é extraído e nenhuma oferta é inferida nesta subetapa.
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
