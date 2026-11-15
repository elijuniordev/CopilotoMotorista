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
import com.copilotomotorista.domain.model.OfferInput
import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.usecase.EvaluateOfferUseCase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.util.UUID

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
    private var currentOfferRecordId: String? = null
    private var captureGeneration: Long = 0L

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
        val packageName = event.packageName?.toString()
        val accepted = eventGate.shouldAccept(
            packageName = packageName,
            eventType = event.eventType,
            nowMillis = SystemClock.uptimeMillis()
        )
        if (!accepted) return

        val platform = when (packageName) {
            SupportedRideApps.UBER_DRIVER -> "uber"
            SupportedRideApps.NINETY_NINE_DRIVER -> "99"
            else -> "unknown"
        }
        val generation = ++captureGeneration
        val capturedAt = SystemClock.uptimeMillis()
        val snapshot = nodeTextReader.read(rootInActiveWindow, capturedAt)
        if (snapshot.hasUsableText) {
            processSnapshot(snapshot, platform, generation)
        } else {
            requestLocalOcr(capturedAt, platform, generation)
        }
    }

    private fun processSnapshot(
        snapshot: ScreenTextSnapshot,
        platform: String,
        generation: Long
    ) {
        if (serviceDestroyed || generation != captureGeneration) return
        val parsed = offerTextParser.parse(snapshot.text)
        if (parsed.status != OfferTextParseStatus.COMPLETE || parsed.offer == null) {
            overlayWindowManager.hideOverlay()
            when (parsed.status) {
                OfferTextParseStatus.NOT_OFFER -> {
                    lastOfferFingerprint = null
                    currentOfferRecordId = null
                }
                OfferTextParseStatus.INCOMPLETE -> {
                    if (shouldRequestOcrFallback(snapshot.source, parsed.status)) {
                        requestLocalOcr(snapshot.capturedAtMillis, platform, generation)
                    }
                }
                OfferTextParseStatus.AMBIGUOUS,
                OfferTextParseStatus.COMPLETE -> Unit
            }
            return
        }

        val offer = parsed.offer
        val fingerprint = listOf(
            platform,
            offer.receivedFareCents,
            offer.pickupDistanceKm,
            offer.tripDistanceKm,
            offer.pickupTimeMinutes,
            offer.tripTimeMinutes
        ).joinToString("|")
        val now = SystemClock.uptimeMillis()
        if (fingerprint == lastOfferFingerprint &&
            now - lastOfferShownAt < OFFER_REPEAT_SUPPRESSION_MILLIS
        ) return

        lastOfferFingerprint = fingerprint
        lastOfferShownAt = now
        serviceScope.launch {
            val nowEpochMillis = System.currentTimeMillis()
            val recentRecord = withContext(Dispatchers.IO) {
                appContainer.tripRecordRepository.findRecentMatchingOffer(
                    platform, offer.receivedFareCents, offer.pickupDistanceKm, offer.tripDistanceKm,
                    offer.pickupTimeMinutes, offer.tripTimeMinutes,
                    nowEpochMillis - DUPLICATE_OFFER_WINDOW_MILLIS
                )
            }
            if (serviceDestroyed || generation != captureGeneration) return@launch
            if (recentRecord?.status == TRIP_STATUS_COMPLETED) {
                currentOfferRecordId = null
                overlayWindowManager.hideOverlay()
                return@launch
            }
            val recordId = recentRecord?.id ?: UUID.randomUUID().toString()
            currentOfferRecordId = recordId
            val selected = withContext(Dispatchers.IO) {
                val vehicleId = appContainer.preferences.selectedVehicleId.first()
                val vehicle = vehicleId?.let { appContainer.vehicleRepository.getVehicle(it) }
                val targets = appContainer.preferences.financialTargets.first()
                vehicle?.let { Triple(it, targets, offer) }
            }
            if (serviceDestroyed || generation != captureGeneration) return@launch
            if (selected == null) {
                overlayWindowManager.hideOverlay()
                return@launch
            }
            val (vehicle, targets, currentOffer) = selected
            val analysis = runCatching {
                evaluateOfferUseCase.execute(currentOffer, vehicle, targets)
            }.getOrNull() ?: return@launch
            if (serviceDestroyed || generation != captureGeneration) return@launch
            overlayWindowManager.showOverlay(
                analysisResult = analysis,
                grossFare = currentOffer.receivedFareCents / 100.0,
                isDemo = false,
                onDismiss = {},
                onAccepted = {
                    saveConfirmedTrip(recordId, platform, currentOffer, vehicle.id, vehicle.name, analysis.operatingCostCents, TRIP_STATUS_ACCEPTED)
                },
                onCompleted = {
                    saveConfirmedTrip(recordId, platform, currentOffer, vehicle.id, vehicle.name, analysis.operatingCostCents, TRIP_STATUS_COMPLETED)
                }
            )
        }
    }

    private fun saveConfirmedTrip(
        recordId: String,
        platform: String,
        offer: OfferInput,
        vehicleId: String,
        vehicleName: String,
        operatingCostCents: Long,
        status: String
    ) {
        if (serviceDestroyed) return
        serviceScope.launch {
            val now = System.currentTimeMillis()
            val existing = withContext(Dispatchers.IO) { appContainer.tripRecordRepository.getTrip(recordId) }
            if (existing?.status == TRIP_STATUS_COMPLETED && status == TRIP_STATUS_ACCEPTED) return@launch
            val record = TripRecord(
                id = recordId,
                platform = platform,
                status = status,
                receivedFareCents = offer.receivedFareCents,
                pickupDistanceKm = offer.pickupDistanceKm,
                tripDistanceKm = offer.tripDistanceKm,
                pickupTimeMinutes = offer.pickupTimeMinutes,
                tripTimeMinutes = offer.tripTimeMinutes,
                origin = null,
                destination = null,
                passengerRating = null,
                paymentMethod = null,
                startedAtEpochMillis = null,
                completedAtEpochMillis = if (status == TRIP_STATUS_COMPLETED) now else null,
                vehicleId = vehicleId,
                evidenceUri = null,
                notes = "Registro confirmado manualmente pelo motorista.",
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                operatingCostCentsSnapshot = operatingCostCents,
                vehicleNameSnapshot = vehicleName
            )
            val saved = runCatching {
                withContext(Dispatchers.IO) {
                    appContainer.tripRecordRepository.saveTrip(record)
                }
            }.isSuccess
            if (saved && currentOfferRecordId == recordId) {
                overlayWindowManager.hideOverlay()
            }
        }
    }

    private fun requestLocalOcr(
        requestedAtMillis: Long,
        platform: String,
        generation: Long
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) return
        if (screenshotInProgress || serviceDestroyed || generation != captureGeneration) return
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
                            val resultAge = SystemClock.uptimeMillis() - requestedAtMillis
                            if (!serviceDestroyed &&
                                generation == captureGeneration &&
                                resultAge in 0..OCR_MAX_RESULT_AGE_MILLIS &&
                                recognizedText.isNotBlank()
                            ) {
                                processSnapshot(
                                    ScreenTextSnapshot(
                                        text = ScreenTextSnapshot.normalize(recognizedText),
                                        source = ScreenTextSource.OCR,
                                        capturedAtMillis = SystemClock.uptimeMillis()
                                    ),
                                    platform,
                                    generation
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
        captureGeneration++
        if (::overlayWindowManager.isInitialized) overlayWindowManager.hideOverlay()
    }

    override fun onDestroy() {
        serviceDestroyed = true
        captureGeneration++
        screenshotInProgress = false
        serviceScope.cancel()
        ocrRecognizer?.close()
        ocrRecognizer = null
        if (::overlayWindowManager.isInitialized) overlayWindowManager.hideOverlay()
        super.onDestroy()
    }

    private companion object {
        const val SCREENSHOT_DEBOUNCE_MILLIS = 1_500L
        const val OCR_MAX_RESULT_AGE_MILLIS = 5_000L
        const val OFFER_REPEAT_SUPPRESSION_MILLIS = 15_000L
        const val DUPLICATE_OFFER_WINDOW_MILLIS = 120_000L
        const val TRIP_STATUS_ACCEPTED = "accepted"
        const val TRIP_STATUS_COMPLETED = "completed"
    }
}
