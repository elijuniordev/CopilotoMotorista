package com.copilotomotorista.service

/**
 * Immutable display metrics used to decide whether a MediaProjection surface
 * must be recreated after a rotation, resolution, or density change.
 */
internal data class CaptureDisplayConfiguration(
    val width: Int,
    val height: Int,
    val densityDpi: Int
) {
    val isValid: Boolean
        get() = width > 0 && height > 0 && densityDpi > 0

    fun requiresUpdateFrom(previous: CaptureDisplayConfiguration): Boolean =
        isValid && this != previous
}
