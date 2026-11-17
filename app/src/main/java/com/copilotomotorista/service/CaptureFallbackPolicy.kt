package com.copilotomotorista.service

/**
 * OCR é uma segunda tentativa somente quando a árvore de acessibilidade
 * não trouxe os campos obrigatórios. Ambiguidade não é resolvida por palpite.
 */
internal fun shouldRequestOcrFallback(
    source: ScreenTextSource,
    status: OfferTextParseStatus
): Boolean =
    source == ScreenTextSource.ACCESSIBILITY_TREE &&
        status == OfferTextParseStatus.INCOMPLETE

internal data class PendingOcrRequest(
    val requestedAtMillis: Long,
    val platform: String,
    val generation: Long
)

internal sealed class PendingScreenshotWork {
    data class Ocr(val request: PendingOcrRequest) : PendingScreenshotWork()
    data class Evidence(val recordId: String) : PendingScreenshotWork()
}

/**
 * Coordinates queued screenshot work without Android dependencies.
 * OCR is prioritized over evidence, and only the most recent OCR fallback is kept.
 */
internal class PendingScreenshotWorkCoordinator {
    private val pendingEvidence = LinkedHashSet<String>()
    private var pendingOcr: PendingOcrRequest? = null

    fun enqueueEvidence(recordId: String) {
        pendingEvidence.add(recordId)
    }

    fun enqueueOcr(request: PendingOcrRequest) {
        pendingOcr = request
    }

    fun pollNext(nowMillis: Long, ocrMaxAgeMillis: Long): PendingScreenshotWork? {
        val ocr = pendingOcr
        pendingOcr = null
        if (ocr != null && nowMillis - ocr.requestedAtMillis <= ocrMaxAgeMillis) {
            return PendingScreenshotWork.Ocr(ocr)
        }

        val nextEvidence = pendingEvidence.firstOrNull() ?: return null
        pendingEvidence.remove(nextEvidence)
        return PendingScreenshotWork.Evidence(nextEvidence)
    }

    fun clear() {
        pendingEvidence.clear()
        pendingOcr = null
    }
}
