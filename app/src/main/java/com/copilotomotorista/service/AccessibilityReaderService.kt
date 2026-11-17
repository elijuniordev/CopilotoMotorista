package com.copilotomotorista.service

import android.accessibilityservice.AccessibilityService
import android.accessibilityservice.AccessibilityServiceInfo
import android.graphics.Bitmap
import android.os.Build
import android.os.SystemClock
import android.util.Log
import android.view.Display
import android.view.accessibility.AccessibilityEvent
import androidx.core.content.ContextCompat
import com.copilotomotorista.data.AppContainer
import com.copilotomotorista.domain.model.OfferInput
import com.copilotomotorista.domain.model.TripRecord
import com.copilotomotorista.domain.usecase.EvaluateOfferUseCase
import com.copilotomotorista.domain.usecase.ResolveTripCostSnapshotUseCase
import com.copilotomotorista.presentation.overlay.RadarOfferDisplay
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.delay
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
    private lateinit var shiftSessionStore: ShiftSessionStore
    private lateinit var floatingControlWindowManager: FloatingControlWindowManager
    private val eventGate = AccessibilityEventGate(supportedPackages = SupportedRideApps.packages)
    private val offerTextParser = OfferTextParser()
    private val offerScreenClassifier = OfferScreenClassifier()
    private val radarOfferExtractor = RadarOfferExtractor()
    private val captureLatencyMetrics = OfferCaptureLatencyMetrics()
    private val evaluateOfferUseCase = EvaluateOfferUseCase()
    private val resolveTripCostSnapshotUseCase = ResolveTripCostSnapshotUseCase()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main.immediate)
    private var ocrRecognizer: LocalOcrTextRecognizer? = null
    private var screenshotInProgress = false
    private var lastScreenshotRequestAt = 0L
    private var serviceDestroyed = false
    private var lastOfferFingerprint: String? = null
    private var lastOfferShownAt = 0L
    private var currentOfferRecordId: String? = null
    private var captureGeneration: Long = 0L
    private val evidenceCaptureRequested = mutableSetOf<String>()
    private val pendingScreenshotWork = PendingScreenshotWorkCoordinator()
    private val pendingEvidenceByRecordId = mutableMapOf<String, String>()

    override fun onCreate() {
        super.onCreate()
        overlayWindowManager = OverlayWindowManager(this)
        appContainer = AppContainer(applicationContext)
        shiftSessionStore = ShiftSessionStore(applicationContext)
        floatingControlWindowManager = FloatingControlWindowManager(this, shiftSessionStore)
        ocrRecognizer = LocalOcrTextRecognizer()
    }

    override fun onServiceConnected() {
        super.onServiceConnected()
        serviceInfo = serviceInfo.apply {
            // A origem do evento é apenas um gatilho; não restringir por pacote.
            eventTypes = AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED or
                AccessibilityEvent.TYPE_WINDOW_CONTENT_CHANGED
            feedbackType = AccessibilityServiceInfo.FEEDBACK_GENERIC
            notificationTimeout = 100L
            flags = flags or AccessibilityServiceInfo.FLAG_RETRIEVE_INTERACTIVE_WINDOWS
        }
        shiftSessionStore.setMonitoringConnected(true)
        floatingControlWindowManager.hide()
        serviceScope.launch {
            while (!serviceDestroyed) {
                shiftSessionStore.heartbeat()
                val shift = shiftSessionStore.snapshot()
                if (shift.active) {
                    floatingControlWindowManager.show()
                } else {
                    floatingControlWindowManager.hide()
                }
                if (!shift.canProcessOffers) {
                    overlayWindowManager.hideOverlay()
                }
                delay(500L)
            }
        }
    }

    override fun onAccessibilityEvent(event: AccessibilityEvent?) {
        event ?: return
        val shift = shiftSessionStore.snapshot()
        if (shift.active) {
            floatingControlWindowManager.show()
        } else {
            floatingControlWindowManager.hide()
        }
        if (!shift.canProcessOffers) {
            overlayWindowManager.hideOverlay()
            return
        }
        val packageName = event.packageName?.toString()
        if (packageName == null || packageName == applicationContext.packageName) return
        // Package ID is used only to avoid capturing unrelated apps. Offer content
        // still comes exclusively from the visible screenshot + local OCR.
        if (!SupportedRideApps.contains(packageName)) return
        val accepted = eventGate.shouldAccept(
            packageName = packageName,
            eventType = event.eventType,
            nowMillis = SystemClock.uptimeMillis()
        )
        if (!accepted) return

        // Package name is only an optional platform hint, never an allow-list.
        val platformHint = when (packageName) {
            SupportedRideApps.UBER_DRIVER -> "uber"
            SupportedRideApps.NINETY_NINE_DRIVER -> "99"
            else -> "unknown"
        }
        val generation = ++captureGeneration
        val capturedAt = SystemClock.uptimeMillis()
        Log.d("OfferCapture", "screen_event type=${event.eventType} source=$packageName")
        // Visual screenshot + local OCR is the primary reader. Accessibility text
        // cannot prevent a screenshot or determine whether a ride offer exists.
        requestLocalOcr(capturedAt, platformHint, generation)
    }

    private fun processSnapshot(
        snapshot: ScreenTextSnapshot,
        platform: String,
        generation: Long
    ) {
        if (serviceDestroyed) return

        // O OCR pode ler valores e distâncias de mapas, notificações e telas
        // financeiras. Só encaminhamos ao parser quando a tela tem a estrutura
        // visual/textual típica de uma oferta de corrida.
        val screenClassification = offerScreenClassifier.classify(snapshot.text)
        if (screenClassification.kind == OfferScreenClassifier.ScreenKind.RADAR_CANDIDATE) {
            val candidates = radarOfferExtractor.extract(snapshot.text)
            Log.i("OfferCapture", "radar_candidates count=${candidates.size}")
            if (candidates.size >= 2) {
                showRadarCandidates(candidates, platform, generation)
            } else if (generation == captureGeneration) {
                overlayWindowManager.hideOverlay()
                lastOfferFingerprint = null
                currentOfferRecordId = null
            }
            return
        }
        if (screenClassification.kind != OfferScreenClassifier.ScreenKind.SINGLE_OFFER) {
            Log.d(
                "OfferCapture",
                "screen_rejected kind=${screenClassification.kind} evidence=${screenClassification.evidence}"
            )
            if (generation == captureGeneration) {
                overlayWindowManager.hideOverlay()
                lastOfferFingerprint = null
                currentOfferRecordId = null
            }
            return
        }
        Log.d(
            "OfferCapture",
            "screen_accepted evidence=${screenClassification.evidence}"
        )

        val parseStartedAt = SystemClock.uptimeMillis()
        val parsed = offerTextParser.parse(snapshot.text)
        val parseDurationMillis = SystemClock.uptimeMillis() - parseStartedAt
        Log.d(
            "OfferCapture",
            "offer_parse status=${parsed.status} missing=${parsed.missingFields.joinToString(",")} " +
                "ambiguous=${parsed.ambiguousFields.joinToString(",")} chars=${snapshot.text.length}"
        )
        if (parsed.status != OfferTextParseStatus.COMPLETE || parsed.offer == null) {
            if (generation != captureGeneration) return
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
            if (serviceDestroyed) return@launch
            if (recentRecord?.status == TRIP_STATUS_COMPLETED) {
                if (generation == captureGeneration) {
                    currentOfferRecordId = null
                    overlayWindowManager.hideOverlay()
                }
                return@launch
            }
            val recordId = recentRecord?.id ?: UUID.randomUUID().toString()
            val existingOffer = withContext(Dispatchers.IO) {
                appContainer.tripRecordRepository.getTrip(recordId)
            }

            // Save a complete offer before vehicle/configuration lookups. Those reads may
            // suspend while the ride app keeps emitting accessibility events.
            if (existingOffer != null && existingOffer.rawOfferText.isNullOrBlank()) {
                runCatching {
                    withContext(Dispatchers.IO) {
                        appContainer.tripRecordRepository.saveTrip(
                            existingOffer.copy(
                                rawOfferText = snapshot.text,
                                origin = existingOffer.origin ?: extractLabeledAddress(snapshot.text, true),
                                destination = existingOffer.destination ?: extractLabeledAddress(snapshot.text, false)
                            )
                        )
                    }
                }.onFailure { error ->
                    Log.e("OfferCapture", "Failed to enrich captured offer record", error)
                }
            }
            if (existingOffer == null) {
                val captured = TripRecord(
                    id = recordId,
                    platform = platform,
                    status = TRIP_STATUS_CAPTURED,
                    receivedFareCents = offer.receivedFareCents,
                    pickupDistanceKm = offer.pickupDistanceKm,
                    tripDistanceKm = offer.tripDistanceKm,
                    pickupTimeMinutes = offer.pickupTimeMinutes,
                    tripTimeMinutes = offer.tripTimeMinutes,
                    origin = extractLabeledAddress(snapshot.text, true),
                    destination = extractLabeledAddress(snapshot.text, false),
                    passengerRating = null,
                    paymentMethod = detectPaymentMethod(snapshot.text).takeUnless { it == "Pagamento não identificado" },
                    startedAtEpochMillis = null,
                    completedAtEpochMillis = null,
                    vehicleId = null,
                    evidenceUri = pendingEvidenceByRecordId[recordId],
                    notes = "Oferta capturada automaticamente.",
                    createdAtEpochMillis = nowEpochMillis,
                    operatingCostCentsSnapshot = null,
                    vehicleNameSnapshot = null,
                    rawOfferText = snapshot.text
                )
                val saved = runCatching {
                    withContext(Dispatchers.IO) {
                        appContainer.tripRecordRepository.saveTrip(captured)
                    }
                }
                if (saved.isFailure) {
                    Log.e("OfferCapture", "Failed to persist captured offer", saved.exceptionOrNull())
                    return@launch
                }
            }

            if (serviceDestroyed) return@launch
            if (generation == captureGeneration) {
                currentOfferRecordId = recordId
            }
            if (recentRecord?.evidenceUri == null && evidenceCaptureRequested.add(recordId)) {
                captureOfferEvidence(recordId)
            }

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
            val displayPreferences = withContext(Dispatchers.IO) {
                appContainer.preferences.overlayDisplayPreferences.first()
            }
            val analysis = runCatching {
                evaluateOfferUseCase.execute(currentOffer, vehicle, targets)
            }.getOrNull() ?: return@launch

            // Enrich the already-persisted capture with the vehicle cost snapshot.
            // This update is independent from whether this offer is still the newest
            // screen generation; only overlay presentation is generation-sensitive.
            runCatching {
                withContext(Dispatchers.IO) {
                    val capturedRecord = appContainer.tripRecordRepository.getTrip(recordId)
                    if (capturedRecord != null && capturedRecord.status == TRIP_STATUS_CAPTURED) {
                        appContainer.tripRecordRepository.saveTrip(
                            capturedRecord.copy(
                                vehicleId = vehicle.id,
                                operatingCostCentsSnapshot = analysis.operatingCostCents,
                                vehicleNameSnapshot = vehicle.name
                            )
                        )
                    }
                }
            }.onFailure { error ->
                Log.e("OfferCapture", "Failed to update captured offer cost snapshot", error)
            }

            if (serviceDestroyed || generation != captureGeneration) return@launch
            overlayWindowManager.showOverlay(
                analysisResult = analysis,
                grossFare = currentOffer.receivedFareCents / 100.0,
                isDemo = false,
                platform = if (platform == "uber") "Uber" else if (platform == "99") "99" else "Aplicativo",
                rideCategory = detectRideCategory(snapshot.text, platform),
                paymentMethod = detectPaymentMethod(snapshot.text),
                showRideCategory = displayPreferences.showRideCategory,
                showPaymentMethod = displayPreferences.showPaymentMethod,
                showDistanceAndTime = displayPreferences.showDistanceAndTime,
                onDismiss = {},
                onAccepted = {
                    saveConfirmedTrip(recordId, platform, currentOffer, vehicle.id, vehicle.name, analysis.operatingCostCents, TRIP_STATUS_ACCEPTED, detectPaymentMethod(snapshot.text), extractLabeledAddress(snapshot.text, true), extractLabeledAddress(snapshot.text, false))
                },
                onCompleted = {
                    saveConfirmedTrip(recordId, platform, currentOffer, vehicle.id, vehicle.name, analysis.operatingCostCents, TRIP_STATUS_COMPLETED, detectPaymentMethod(snapshot.text), extractLabeledAddress(snapshot.text, true), extractLabeledAddress(snapshot.text, false))
                },
                showCompleteAction = recentRecord?.status == "in_progress"
            )
            val overlayShownAt = SystemClock.uptimeMillis()
            val summary = captureLatencyMetrics.record(
                platform = platform,
                source = snapshot.source.name,
                durationMillis = overlayShownAt - snapshot.capturedAtMillis
            )
            if (summary != null) {
                Log.i(
                    "OfferCaptureMetrics",
                    "platform=${summary.platform} source=${summary.source} " +
                        "samples=${summary.count} p50Ms=${summary.p50Millis} " +
                        "p95Ms=${summary.p95Millis} parseMs=$parseDurationMillis"
                )
            }
        }
    }

    private fun showRadarCandidates(
        candidates: List<RadarOfferExtractor.Candidate>,
        platform: String,
        generation: Long
    ) {
        serviceScope.launch {
            val selected = withContext(Dispatchers.IO) {
                val vehicleId = appContainer.preferences.selectedVehicleId.first()
                val vehicle = vehicleId?.let { appContainer.vehicleRepository.getVehicle(it) }
                val targets = appContainer.preferences.financialTargets.first()
                vehicle?.let { it to targets }
            }
            if (serviceDestroyed || generation != captureGeneration) return@launch
            if (selected == null) {
                overlayWindowManager.hideOverlay()
                Log.w("OfferCapture", "radar_overlay_skipped reason=no_selected_vehicle")
                return@launch
            }
            val (vehicle, targets) = selected
            val analyzed = candidates.mapNotNull { candidate ->
                val result = runCatching {
                    evaluateOfferUseCase.execute(candidate.offer, vehicle, targets)
                }.getOrNull() ?: return@mapNotNull null
                RadarOfferDisplay(
                    position = candidate.position,
                    grossFare = candidate.offer.receivedFareCents / 100.0,
                    analysisResult = result
                )
            }
            if (analyzed.size < 2 || serviceDestroyed || generation != captureGeneration) {
                if (generation == captureGeneration) overlayWindowManager.hideOverlay()
                Log.w("OfferCapture", "radar_overlay_skipped reason=insufficient_analyzed_candidates count=${analyzed.size}")
                return@launch
            }
            overlayWindowManager.showRadarOverlay(
                offers = analyzed,
                platform = if (platform == "uber") "Uber" else if (platform == "99") "99" else "Aplicativo",
                onDismiss = {}
            )
            Log.i("OfferCapture", "radar_overlay_shown offers=${analyzed.size}")
        }
    }

    private fun detectRideCategory(text: String, platform: String): String {
        val patterns = if (platform == "uber") {
            listOf("UberX", "Uber Comfort", "Uber Black", "UberX Saver", "Uber Flash")
        } else {
            listOf("99Pop", "99Plus", "99Comfort", "99Moto", "99Entrega")
        }
        return patterns.firstOrNull { pattern ->
            Regex(Regex.escape(pattern), RegexOption.IGNORE_CASE).containsMatchIn(text)
        } ?: "Categoria não identificada"
    }

    private fun extractLabeledAddress(text: String, origin: Boolean): String? =
        OfferAddressExtractor.extract(text, origin)

    private fun detectPaymentMethod(text: String): String =
        PaymentMethodDetector.detect(text)

    private fun saveConfirmedTrip(
        recordId: String,
        platform: String,
        offer: OfferInput,
        vehicleId: String,
        vehicleName: String,
        operatingCostCents: Long,
        status: String,
        paymentMethod: String,
        detectedOrigin: String?,
        detectedDestination: String?
    ) {
        if (serviceDestroyed) return
        serviceScope.launch {
            val now = System.currentTimeMillis()
            val existing = withContext(Dispatchers.IO) { appContainer.tripRecordRepository.getTrip(recordId) }
            if (existing?.status == TRIP_STATUS_COMPLETED && status == TRIP_STATUS_ACCEPTED) return@launch
            val costSnapshot = resolveTripCostSnapshotUseCase.execute(existing, vehicleId, operatingCostCents, vehicleName)
            val record = TripRecord(
                id = recordId,
                platform = platform,
                status = status,
                receivedFareCents = offer.receivedFareCents,
                pickupDistanceKm = offer.pickupDistanceKm,
                tripDistanceKm = offer.tripDistanceKm,
                pickupTimeMinutes = offer.pickupTimeMinutes,
                tripTimeMinutes = offer.tripTimeMinutes,
                origin = existing?.origin ?: detectedOrigin,
                destination = existing?.destination ?: detectedDestination,
                passengerRating = null,
                paymentMethod = existing?.paymentMethod ?: paymentMethod.takeUnless { it == "Pagamento não identificado" },
                startedAtEpochMillis = existing?.startedAtEpochMillis
                    ?: if (status == TRIP_STATUS_COMPLETED) now else null,
                completedAtEpochMillis = if (status == TRIP_STATUS_COMPLETED) now else existing?.completedAtEpochMillis,
                vehicleId = costSnapshot.vehicleId,
                evidenceUri = existing?.evidenceUri ?: pendingEvidenceByRecordId[recordId],
                notes = "Registro confirmado manualmente pelo motorista.",
                createdAtEpochMillis = existing?.createdAtEpochMillis ?: now,
                operatingCostCentsSnapshot = costSnapshot.operatingCostCents,
                vehicleNameSnapshot = costSnapshot.vehicleName,
                rawOfferText = existing?.rawOfferText
            )
            val saved = runCatching {
                withContext(Dispatchers.IO) {
                    appContainer.tripRecordRepository.saveTrip(record)
                }
            }.isSuccess
            val shiftStartedAt = shiftSessionStore.snapshot().startedAtEpochMillis
            val tripStartedAt = existing?.startedAtEpochMillis ?: now
            if (saved && status == TRIP_STATUS_COMPLETED &&
                existing?.status == TRIP_STATUS_IN_PROGRESS &&
                shiftStartedAt > 0L && tripStartedAt >= shiftStartedAt
            ) {
                shiftSessionStore.recordCompletedTrip(recordId, offer.receivedFareCents)
            }
            if (saved && currentOfferRecordId == recordId) {
                overlayWindowManager.hideOverlay()
            }
        }
    }

    private fun captureOfferEvidence(recordId: String) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R || serviceDestroyed) return
        if (screenshotInProgress) {
            // OCR and evidence share Android's screenshot API. Never lose an evidence
            // request just because the OCR screenshot currently owns the capture slot.
            pendingScreenshotWork.enqueueEvidence(recordId)
            return
        }

        screenshotInProgress = true
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
                        val hardware = Bitmap.wrapHardwareBuffer(screenshot.hardwareBuffer, screenshot.colorSpace)
                        screenshot.hardwareBuffer.close()
                        val bitmap = hardware?.copy(Bitmap.Config.ARGB_8888, false)
                        hardware?.recycle()
                        if (bitmap == null) {
                            evidenceCaptureRequested.remove(recordId)
                            drainPendingScreenshotWork()
                            return
                        }
                        serviceScope.launch {
                            var evidenceSaved = false
                            try {
                                val path = withContext(Dispatchers.IO) {
                                    OfferEvidenceStorage.save(this@AccessibilityReaderService, bitmap)
                                }
                                if (path != null && !serviceDestroyed) {
                                    pendingEvidenceByRecordId[recordId] = path
                                    withContext(Dispatchers.IO) {
                                        val existing = appContainer.tripRecordRepository.getTrip(recordId)
                                        if (existing != null && existing.evidenceUri == null) {
                                            appContainer.tripRecordRepository.saveTrip(existing.copy(evidenceUri = path))
                                        }
                                    }
                                    evidenceSaved = true
                                }
                            } catch (_: Exception) {
                                // Keep the offer eligible for a later retry.
                            } finally {
                                if (!evidenceSaved) evidenceCaptureRequested.remove(recordId)
                                bitmap.recycle()
                                drainPendingScreenshotWork()
                            }
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        screenshotInProgress = false
                        evidenceCaptureRequested.remove(recordId)
                        drainPendingScreenshotWork()
                    }
                }
            )
        } catch (_: SecurityException) {
            screenshotInProgress = false
            evidenceCaptureRequested.remove(recordId)
            drainPendingScreenshotWork()
        } catch (_: IllegalStateException) {
            screenshotInProgress = false
            evidenceCaptureRequested.remove(recordId)
            drainPendingScreenshotWork()
        }
    }

    private fun drainPendingScreenshotWork() {
        if (screenshotInProgress || serviceDestroyed) return

        when (val nextWork = pendingScreenshotWork.pollNext(
            SystemClock.uptimeMillis(),
            OCR_MAX_RESULT_AGE_MILLIS
        )) {
            null -> return
            is PendingScreenshotWork.Ocr -> {
                val request = nextWork.request
                requestLocalOcr(request.requestedAtMillis, request.platform, request.generation)
                if (screenshotInProgress) return
                drainPendingScreenshotWork()
            }
            is PendingScreenshotWork.Evidence -> captureOfferEvidence(nextWork.recordId)
        }
    }

    private fun requestLocalOcr(
        requestedAtMillis: Long,
        platform: String,
        generation: Long
    ) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.R) {
            Log.w("OfferCapture", "visual_ocr_unavailable api=${Build.VERSION.SDK_INT}; Accessibility screenshot requires API 30+")
            return
        }
        if (serviceDestroyed || generation != captureGeneration) return
        if (screenshotInProgress) {
            pendingScreenshotWork.enqueueOcr(PendingOcrRequest(requestedAtMillis, platform, generation))
            return
        }
        val elapsedSinceLastCapture = requestedAtMillis - lastScreenshotRequestAt
        if (lastScreenshotRequestAt > 0L && elapsedSinceLastCapture < SCREENSHOT_DEBOUNCE_MILLIS) {
            val waitMillis = SCREENSHOT_DEBOUNCE_MILLIS - elapsedSinceLastCapture
            serviceScope.launch {
                delay(waitMillis)
                if (!serviceDestroyed && generation == captureGeneration) {
                    requestLocalOcr(SystemClock.uptimeMillis(), platform, generation)
                }
            }
            return
        }

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
                        if (hardwareBitmap == null) {
                            Log.w("OfferCapture", "visual_screenshot_bitmap_unavailable")
                            drainPendingScreenshotWork()
                            return
                        }
                        Log.d(
                            "OfferCapture",
                            "visual_screenshot_success width=${hardwareBitmap.width} height=${hardwareBitmap.height}"
                        )
                        val softwareBitmap = hardwareBitmap.copy(Bitmap.Config.ARGB_8888, false)
                        hardwareBitmap.recycle()
                        if (softwareBitmap == null) {
                            Log.w("OfferCapture", "visual_screenshot_software_bitmap_unavailable")
                            drainPendingScreenshotWork()
                            return
                        }

                        ocrRecognizer?.recognize(softwareBitmap) { recognizedText ->
                            val resultAge = SystemClock.uptimeMillis() - requestedAtMillis
                            if (!serviceDestroyed &&
                                resultAge in 0..OCR_MAX_RESULT_AGE_MILLIS &&
                                recognizedText.isNotBlank()
                            ) {
                                val normalizedText = ScreenTextSnapshot.normalize(recognizedText)
                                val detectedPlatform = when {
                                    Regex("\\buber\\b", RegexOption.IGNORE_CASE).containsMatchIn(normalizedText) -> "uber"
                                    Regex("\\b99(?:pop|plus|comfort|moto|entrega|taxi)\\b", RegexOption.IGNORE_CASE).containsMatchIn(normalizedText) -> "99"
                                    else -> platform
                                }
                                Log.d(
                                    "OfferCapture",
                                    "ocr_result chars=${normalizedText.length} platform=$detectedPlatform ageMs=$resultAge"
                                )
                                processSnapshot(
                                    ScreenTextSnapshot(
                                        text = normalizedText,
                                        source = ScreenTextSource.OCR,
                                        capturedAtMillis = requestedAtMillis
                                    ),
                                    detectedPlatform,
                                    generation
                                )
                            } else if (!serviceDestroyed && resultAge in 0..OCR_MAX_RESULT_AGE_MILLIS) {
                                Log.d("OfferCapture", "ocr_result_empty ageMs=$resultAge")
                            }
                            drainPendingScreenshotWork()
                        } ?: run {
                            softwareBitmap.recycle()
                            drainPendingScreenshotWork()
                        }
                    }

                    override fun onFailure(errorCode: Int) {
                        Log.w("OfferCapture", "visual_screenshot_failed code=$errorCode")
                        screenshotInProgress = false
                        drainPendingScreenshotWork()
                    }
                }
            )
        } catch (error: SecurityException) {
            Log.e("OfferCapture", "visual_screenshot_security_failure", error)
            screenshotInProgress = false
            drainPendingScreenshotWork()
        } catch (error: IllegalStateException) {
            Log.e("OfferCapture", "visual_screenshot_state_failure", error)
            screenshotInProgress = false
            drainPendingScreenshotWork()
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
        pendingScreenshotWork.clear()
        serviceScope.cancel()
        ocrRecognizer?.close()
        ocrRecognizer = null
        if (::overlayWindowManager.isInitialized) overlayWindowManager.hideOverlay()
        if (::floatingControlWindowManager.isInitialized) floatingControlWindowManager.hide()
        if (::shiftSessionStore.isInitialized) shiftSessionStore.setMonitoringConnected(false)
        super.onDestroy()
    }

    private companion object {
        const val SCREENSHOT_DEBOUNCE_MILLIS = 1_500L
        const val OCR_MAX_RESULT_AGE_MILLIS = 5_000L
        const val OFFER_REPEAT_SUPPRESSION_MILLIS = 15_000L
        const val DUPLICATE_OFFER_WINDOW_MILLIS = 120_000L
        const val TRIP_STATUS_CAPTURED = "captured"
        const val TRIP_STATUS_ACCEPTED = "accepted"
        const val TRIP_STATUS_IN_PROGRESS = "in_progress"
        const val TRIP_STATUS_COMPLETED = "completed"
    }
}
