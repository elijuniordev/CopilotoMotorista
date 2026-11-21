package com.copilotomotorista.service

/**
 * Tracks foreground app transitions without inspecting text or personal content.
 *
 * The capture generation may change many times while a single ride app stays
 * in the foreground. Only a transition to another window/app invalidates an
 * already acquired screenshot, not repeated events from that same ride app.
 */
internal class OfferCapturePrivacyGate(
    private val supportedPackages: Set<String>
) {
    private var activePackage: String? = null
    private var lastInvalidatedGeneration: Long = -1L

    /**
     * Returns true when a new window invalidates pending screenshot work.
     * Window-content events can also come from background apps and must not
     * alone establish foreground ownership.
     */
    fun onWindowEvent(packageName: String?, eventType: Int, generation: Long): Boolean {
        if (eventType != WINDOW_STATE_CHANGED) return false
        val foregroundCandidate = packageName?.takeIf { it in supportedPackages }
        if (activePackage == foregroundCandidate) return false
        invalidate(generation)
        activePackage = foregroundCandidate
        return true
    }

    fun isAllowed(
        expectedPackage: String?,
        generation: Long,
        foregroundPackage: String?
    ): Boolean = expectedPackage != null &&
        expectedPackage in supportedPackages &&
        expectedPackage == activePackage &&
        expectedPackage == foregroundPackage &&
        generation > lastInvalidatedGeneration

    fun invalidate(generation: Long) {
        lastInvalidatedGeneration = maxOf(lastInvalidatedGeneration, generation)
        activePackage = null
    }

    companion object {
        // AccessibilityEvent.TYPE_WINDOW_STATE_CHANGED (pure JVM logic).
        const val WINDOW_STATE_CHANGED = 32
    }
}
