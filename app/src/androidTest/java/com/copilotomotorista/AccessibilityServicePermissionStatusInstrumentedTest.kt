package com.copilotomotorista

import android.content.ComponentName
import android.content.Context
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.copilotomotorista.service.AccessibilityReaderService
import com.copilotomotorista.service.AccessibilityServicePermissionStatus
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccessibilityServicePermissionStatusInstrumentedTest {
    private val context: Context = ApplicationProvider.getApplicationContext()
    private val expected = ComponentName(context, AccessibilityReaderService::class.java)

    @Test
    fun recognizesFullAndShortAndroidComponentNames() {
        assertTrue(AccessibilityServicePermissionStatus.isEnabled(expected.flattenToString(), expected))
        assertTrue(AccessibilityServicePermissionStatus.isEnabled(expected.flattenToShortString(), expected))
        assertTrue(
            AccessibilityServicePermissionStatus.isEnabled(
                "com.another.app/.OtherService:${expected.flattenToShortString()}", expected
            )
        )
    }

    @Test
    fun rejectsMalformedOrOtherPackagesWithoutSubstringMatching() {
        assertFalse(AccessibilityServicePermissionStatus.isEnabled(null, expected))
        assertFalse(AccessibilityServicePermissionStatus.isEnabled("", expected))
        assertFalse(AccessibilityServicePermissionStatus.isEnabled("not_a_component", expected))
        assertFalse(
            AccessibilityServicePermissionStatus.isEnabled(
                "com.attacker.app/${expected.className}", expected
            )
        )
        assertFalse(
            AccessibilityServicePermissionStatus.isEnabled(
                "${expected.flattenToString()}.fake", expected
            )
        )
    }
}
