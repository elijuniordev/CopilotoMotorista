package com.copilotomotorista.service

/**
 * Executes the two operations required for a visible overlay and guarantees
 * cleanup when either attachment or lifecycle activation fails.
 *
 * Kept free of Android dependencies so the success/failure contract can be
 * verified with local unit tests.
 */
internal object OverlayDisplayAttempt {
    fun execute(
        attach: () -> Unit,
        activate: () -> Unit,
        cleanup: () -> Unit,
        onFailure: (RuntimeException) -> Unit = {}
    ): Boolean {
        return try {
            attach()
            activate()
            true
        } catch (exception: RuntimeException) {
            runCatching(cleanup)
            runCatching { onFailure(exception) }
            false
        }
    }
}
