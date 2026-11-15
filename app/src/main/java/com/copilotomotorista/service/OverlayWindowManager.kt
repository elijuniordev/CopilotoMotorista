package com.copilotomotorista.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.copilotomotorista.domain.usecase.OfferAnalysisResult
import com.copilotomotorista.presentation.overlay.OfferOverlayUi
import com.copilotomotorista.ui.theme.CopilotoMotoristaTheme

class OverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null

    fun showOverlay(
        analysisResult: OfferAnalysisResult,
        grossFare: Double,
        isDemo: Boolean = false,
        onDismiss: () -> Unit
    ) {
        hideOverlay()

        val type = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else {
            WindowManager.LayoutParams.TYPE_PHONE
        }

        val params = WindowManager.LayoutParams(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            type,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.TOP
            x = 0
            y = 72
        }

        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(null)
            setViewTreeSavedStateRegistryOwner(null)
            setContent {
                CopilotoMotoristaTheme {
                    OfferOverlayUi(
                        analysisResult = analysisResult,
                        grossFare = grossFare,
                        isDemo = isDemo,
                        onDismiss = {
                            hideOverlay()
                            onDismiss()
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(view, params)
            overlayView = view
        } catch (_: SecurityException) {
            overlayView = null
        } catch (_: WindowManager.BadTokenException) {
            overlayView = null
        }
    }

    fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (_: IllegalArgumentException) {
                // A view já pode ter sido removida pelo sistema.
            } finally {
                overlayView = null
            }
        }
    }
}
