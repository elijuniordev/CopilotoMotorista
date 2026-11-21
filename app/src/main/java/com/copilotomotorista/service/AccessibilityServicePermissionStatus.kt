package com.copilotomotorista.service

import android.content.ComponentName

/**
 * Android can serialize enabled accessibility services with a full or short
 * flattened class name. Compare parsed ComponentNames rather than strings, so
 * the setup screen does not show a false "permission missing" warning.
 */
internal object AccessibilityServicePermissionStatus {
    fun isEnabled(enabledServices: String?, expectedService: ComponentName): Boolean =
        enabledServices.orEmpty().split(':').any { entry ->
            ComponentName.unflattenFromString(entry.trim()) == expectedService
        }
}
