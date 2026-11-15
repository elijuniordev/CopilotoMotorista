package com.copilotomotorista.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import android.view.accessibility.AccessibilityNodeInfo
import androidx.core.content.ContextCompat

/**
 * Captura texto das telas dos aplicativos de transporte.
 * A leitura da árvore é prioritária; OCR local é fallback quando o texto
 * disponibilizado pelo aplicativo é insuficiente e a API do Android permite.
 */
class AccessibilityReaderService : AccessibilityService() {

    private lateinit var overlayWindowManager: OverlayWindowManager
    private val eventGate = AccessibilityEventGate()
    private val nodeTextReader = AccessibilityNodeTextReader()
    private var ocrRecognizer: LocalOcrTextRecognizer? = null
    private var screenshotInProgress = false
    private var lastScreenshotRequestAt = 0L
    private var serviceDestroyed = false

    @Volatile
    private var latestScreenSnapshot: ScreenTextSnapshot? = null

    override fun onCreate() {
        super.onCreate()
        overlayWindowManager = OverlayWindowManager(this)
        ocrRecognizer = LocalOcrTextRecognizer()
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

        val capturedAt = SystemClock.uptimeMillis()
        val root: AccessibilityNodeInfo? = rootInActiveWindow
        val snapshot = nodeTextReader.read(root, capturedAt)
        latestScreenSnapshot = snapshot

        if (!snapshot.hasUsableText) {
            requestLocalOcr(capturedAt)
        }
    }

    private fun requestLocalOcr(requestedAtMillis: Long) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (screenshotInProgress || serviceDestroyed) return
        if (requestedAtMillis - lastScreenshotRequestAt < SCREENSHOT_DEBOUNCE_MILLIS) return

        screenshotInProgress = true
        lastScreenshotRequestAt = requestedAtMillis

        try {
            takeScreenshot(
                Display.DEFAULT_DISPLAY,
                ContextCompat.getMainExecutor(this),
                object : TakeScreenshotCallback {
                    override fun onSuccess(screenshot: ScreenshotResult) {
                        screenshotInProgress = false
                        if (serviceDestroyed) {
                            screenshot.hardwareBuffer.close()
                            return
                        }

                        val hardwareBitmap = Bitmap.wrapHardwareBuffer(
                            screenshot.hardwareBuffer,
                            screenshot.colorSpace
                        )
                        screenshot.hardwareBuffer.close()

                        val softwareBitmap = hardwareBitmap?.copy(Bitmap.Config.ARGB_8888, false)
                        if (softwareBitmap == null) {
                            hardwareBitmap?.recycle()
                            return
                        }
                        hardwareBitmap.recycle()

                        ocrRecognizer?.recognize(softwareBitmap) { recognizedText ->
                            if (!serviceDestroyed && recognizedText.isNotBlank()) {
                                latestScreenSnapshot = ScreenTextSnapshot(
                                    text = ScreenTextSnapshot.normalize(recognizedText),
                                    source = ScreenTextSource.OCR,
                                    capturedAtMillis = SystemClock.uptimeMillis()
                                )
                            }
                        } ?: softwareBitmap.recycle()
                    }

                    override fun onFailure(errorCode: Int) {
                        screenshotInProgress = false
                    }
                }
            )
        } catch (_: SecurityException) {
            screenshotInProgress = false
        } catch (_: IllegalStateException) {
            screenshotInProgress = false
        }
    }

    override fun onInterrupt() {
        if (::overlayWindowManager.isInitialized) {
            overlayWindowManager.hideOverlay()
        }
    }

    override fun onDestroy() {
        serviceDestroyed = true
        screenshotInProgress = false
        ocrRecognizer?.close()
        ocrRecognizer = null
        if (::overlayWindowManager.isInitialized) {
            overlayWindowManager.hideOverlay()
        }
        super.onDestroy()
    }

    private companion object {
        const val SCREENSHOT_DEBOUNCE_MILLIS = 1_500L
    }
}
