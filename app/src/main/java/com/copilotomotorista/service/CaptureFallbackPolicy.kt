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
