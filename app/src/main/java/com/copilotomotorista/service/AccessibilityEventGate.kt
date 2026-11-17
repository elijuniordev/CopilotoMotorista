package com.copilotomotorista.service

/**
 * Filtra eventos antes de qualquer leitura da árvore de acessibilidade.
 * O relógio recebido deve ser monotônico (SystemClock.uptimeMillis).
 */
internal class AccessibilityEventGate(
    private val supportedPackages: Set<String>? = null,
    private val contentChangeDebounceMillis: Long = 350L
) {
    private val lastContentEventByPackage = mutableMapOf<String, Long>()

    init {
        require(contentChangeDebounceMillis >= 0L)
    }

    fun shouldAccept(
        packageName: String?,
        eventType: Int,
        nowMillis: Long
    ): Boolean {
        if (packageName.isNullOrBlank()) return false
        if (supportedPackages != null && packageName !in supportedPackages) return false

        return when (eventType) {
            TYPE_WINDOW_STATE_CHANGED -> true
            TYPE_WINDOW_CONTENT_CHANGED -> {
                val previous = lastContentEventByPackage[packageName]
                if (previous != null && nowMillis - previous < contentChangeDebounceMillis) {
                    false
                } else {
                    lastContentEventByPackage[packageName] = nowMillis
                    true
                }
            }
            else -> false
        }
    }

    companion object {
        // Valores definidos pela API Android AccessibilityEvent.
        const val TYPE_WINDOW_STATE_CHANGED = 32
        const val TYPE_WINDOW_CONTENT_CHANGED = 2048
    }
}
