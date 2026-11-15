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
import com.copilotomotorista.data.AppContainer
import com.copilotomotorista.domain.usecase.EvaluateOfferUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Captura texto das telas Uber/99 e só apresenta análise quando oferta,
 * veículo selecionado e parâmetros financeiros estão disponíveis.
 */
class AccessibilityReaderService : AccessibilityService() {

    private lateinit var overlayWindowManager: OverlayWindowManager
    private lateinit var appContainer: AppContainer
    private val eventGate = AccessibilityEventGate()
    private val nodeTextReader = AccessibilityNodeTextReader()
    private val offerTextParser = OfferTextParser()
    private val evaluateOfferUseCase = EvaluateOfferUseCase()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ocrRecognizer: LocalOcrTextRecognizer? = null
    private var screenshotInProgress = false
    private var lastScreenshotRequestAt = 0L
    private var serviceDestroyed = false
    private var lastOfferFingerprint: String? = null
    private var lastOfferShownAt = 0L

    override fun onCreate() {
        super.onCreate()
        overlayWindowManager = OverlayWindowManager(this)
        appContainer = AppContainer(applicationContext)
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
        val accepted = eventGate.shouldAccept(
            packageName = event.packageName?.toString(),
            eventType = event.eventType,
            nowMillis = SystemClock.uptimeMillis()
        )
        if (!accepted) return

        val capturedAt = SystemClock.uptimeMillis()
        val snapshot = nodeTextReader.read(rootInActiveWindow, capturedAt)
        if (snapshot.hasUsableText) {
            processSnapshot(snapshot)
        } else {
            requestLocalOcr(capturedAt)
        }
    }

    private fun processSnapshot(snapshot: ScreenTextSnapshot) {
        if (serviceDestroyed) return
        when (val parsed = offerTextParser.parse(snapshot.text)) {
            is OfferTextParseResult -> {
                if (parsed.status != OfferTextParseStatus.COMPLETE || parsed.offer == null) {
                    if (parsed.status == OfferTextParseStatus.NOT_OFFER) {
                        lastOfferFingerprint = null
                        overlayWindowManager.hideOverlay()
                    }
                    return
                }
                val fingerprint = listOf(
                    parsed.offer.receivedFareCents,
                    parsed.offer.pickupDistanceKm,
                    parsed.offer.tripDistanceKm,
                    parsed.offer.pickupTimeMinutes,
                    parsed.offer.tripTimeMinutes
                ).joinToString("|")
                val now = SystemClock.uptimeMillis()
                if (fingerprint == lastOfferFingerprint &&
                    now - lastOfferShownAt < OFFER_REPEAT_SUPPRESSION_MILLIS
                ) return

                lastOfferFingerprint = fingerprint
                lastOfferShownAt = now
                serviceScope.launch {
                    val selected = withContext(Dispatchers.IO) {
                        val vehicleId = appContainer.preferences.selectedVehicleId.first()
                        val vehicle = vehicleId?.let { appContainer.vehicleRepository.getVehicle(it) }
                        val targets = appContainer.preferences.financialTargets.first()
                        vehicle?.let { Triple(it, targets, parsed.offer) }
                    }
                    if (serviceDestroyed) return@launch
                    if (selected == null) {
                        overlayWindowManager.hideOverlay()
                        return@launch
                    }
                    val (vehicle, targets, offer) = selected
                    val analysis = runCatching {
                        evaluateOfferUseCase.execute(offer, vehicle, targets)
                    }.getOrNull() ?: return@launch
                    overlayWindowManager.showOverlay(
                        analysisResult = analysis,
                        grossFare = offer.receivedFareCents / 100.0,
                        isDemo = false,
                        onDismiss = {}
                    )
                }
            }
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
                        hardwareBitmap?.recycle()
                        if (softwareBitmap == null) return

                        ocrRecognizer?.recognize(softwareBitmap) { recognizedText ->
                            if (!serviceDestroyed && recognizedText.isNotBlank()) {
                                processSnapshot(
                                    ScreenTextSnapshot(
                                        text = ScreenTextSnapshot.normalize(recognizedText),
                                        source = ScreenTextSource.OCR,
                                        capturedAtMillis = SystemClock.uptimeMillis()
                                    )
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
        if (::overlayWindowManager.isInitialized) overlayWindowManager.hideOverlay()
    }

    override fun onDestroy() {
        serviceDestroyed = true
        screenshotInProgress = false
        serviceScope.cancel()
        ocrRecognizer?.close()
        ocrRecognizer = null
        if (::overlayWindowManager.isInitialized) overlayWindowManager.hideOverlay()
        super.onDestroy()
    }

    private companion object {
        const val SCREENSHOT_DEBOUNCE_MILLIS = 1_500L
        const val OFFER_REPEAT_SUPPRESSION_MILLIS = 15_000L
    }
}
