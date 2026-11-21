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
    fun onWindowEvent(
        packageName: String?,
        eventType: Int,
        generation: Long,
        verifiedForegroundPackage: String? = null,
        ownPackage: String? = null
    ): Boolean {
        if (eventType != WINDOW_STATE_CHANGED) return false
        // A TYPE_WINDOW_STATE_CHANGED from our *own floating window* is not
        // a navigation away from Uber/99 when Android still verifies the ride
        // app as the active interactive root. A real switch to our own app
        // must invalidate the session, because its active root is no longer
        // the ride app.
        if (ownPackage != null && packageName == ownPackage &&
            activePackage != null && verifiedForegroundPackage == activePackage
        ) return false
        // A delayed state event from Uber/99 can arrive after Android has
        // already switched its interactive root to another app. In that case
        // the event's package must not re-authorize a capture session.
        if (verifiedForegroundPackage != null && verifiedForegroundPackage != packageName) {
            if (activePackage == null) return false
            invalidate(generation)
            return true
        }
        val foregroundCandidate = packageName?.takeIf { it in supportedPackages }
        if (activePackage == foregroundCandidate) return false
        invalidate(generation)
        activePackage = foregroundCandidate
        return true
    }

    /** Establish a session when accessibility was enabled mid-screen and the
     * active window package has already been verified independently. */
    fun onVerifiedForeground(packageName: String?, generation: Long): Boolean {
        if (packageName == null || packageName !in supportedPackages) return false
        if (activePackage == packageName) return false
        invalidate(generation)
        activePackage = packageName
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
