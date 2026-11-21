package com.copilotomotorista

import androidx.compose.ui.test.assertExists
import androidx.compose.ui.test.junit4.createAndroidComposeRule
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.compose.ui.test.performScrollTo
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * First-launch smoke tests executed on the CI emulators (API 26 and 35).
 *
 * Neither test grants overlay, accessibility or screen-capture permissions:
 * onboarding and the financial dashboard must be navigable before monitoring
 * is enabled. These checks do not substitute for real Uber/99 field tests.
 */
@RunWith(AndroidJUnit4::class)
class MainActivitySmokeInstrumentedTest {
    @get:Rule
    val composeRule = createAndroidComposeRule<MainActivity>()

    @Test
    fun freshLaunchShowsSetupAndCaptureDisclosure() {
        composeRule.onNodeWithText("Vamos preparar seu copiloto").assertExists()
        composeRule.onNodeWithText("ANTES DE ATIVAR O MONITORAMENTO").assertExists()
        composeRule.onNodeWithText("Abrir painel financeiro").assertExists()
    }

    @Test
    fun dashboardRemainsOpenAfterActivityRecreation() {
        composeRule.onNodeWithText("Abrir painel financeiro")
            .performScrollTo()
            .performClick()
        composeRule.onNodeWithText("Dirija melhor. Conquiste mais.").assertExists()

        // A rotation or OS activity recreation must not return the
        // driver to the permission onboarding screen.
        composeRule.activityRule.scenario.recreate()

        composeRule.onNodeWithText("Dirija melhor. Conquiste mais.").assertExists()
    }

    @Test
    fun financialDashboardOpensWithoutGrantingMonitoringPermissions() {
        composeRule.onNodeWithText("Abrir painel financeiro")
            .performScrollTo()
            .performClick()

        // The brand tagline is unique to the main navigation, unlike "Painel"
        // which can appear in both a tab label and screen contents.
        composeRule.onNodeWithText("Dirija melhor. Conquiste mais.").assertExists()
    }
}
