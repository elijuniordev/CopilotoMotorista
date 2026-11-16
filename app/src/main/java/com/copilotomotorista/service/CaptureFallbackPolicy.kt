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

/** Keeps screenshot evidence requests in arrival order while another screenshot is running. */
internal class PendingEvidenceCaptureQueue {
    private val pendingRecordIds = LinkedHashSet<String>()

    fun enqueue(recordId: String) {
        pendingRecordIds.add(recordId)
    }

    fun pollNext(): String? {
        val next = pendingRecordIds.firstOrNull() ?: return null
        pendingRecordIds.remove(next)
        return next
    }

    fun clear() {
        pendingRecordIds.clear()
    }
}
