package com.copilotomotorista.service

import android.content.Context
import android.graphics.PixelFormat
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import com.copilotomotorista.domain.usecase.OfferAnalysisResult
import com.copilotomotorista.presentation.overlay.OfferOverlayUi

class OverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: View? = null

    fun showOverlay(
        analysisResult: OfferAnalysisResult,
        grossFare: Double,
        onDismiss: () -> Unit,
        onAccept: () -> Unit
    ) {
        if (overlayView != null) return

        val layoutParams = WindowManager.LayoutParams().apply {
            type = WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
            format = PixelFormat.TRANSLUCENT
            flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                    WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN or
                    WindowManager.LayoutParams.FLAG_WATCH_OUTSIDE_TOUCH

            width = WindowManager.LayoutParams.MATCH_PARENT
            height = WindowManager.LayoutParams.WRAP_CONTENT
            gravity = Gravity.TOP
            y = 100
        }

        val composeView = ComposeView(context).apply {
            setContent {
                OfferOverlayUi(
                    analysisResult = analysisResult,
                    grossFare = grossFare,
                    onDismiss = {
                        hideOverlay()
                        onDismiss()
                    },
                    onAcceptSimulated = {
                        hideOverlay()
                        onAccept()
                    }
                )
            }
        }

        overlayView = composeView

        try {
            windowManager.addView(composeView, layoutParams)
        } catch (e: Exception) {
            Log.e("OverlayManager", "Erro ao exibir overlay: ${e.localizedMessage}")
        }
    }

    fun hideOverlay() {
        overlayView?.let { view ->
            try {
                windowManager.removeView(view)
            } catch (e: Exception) {
                Log.e("OverlayManager", "Erro ao remover overlay: ${e.localizedMessage}")
            }
            overlayView = null
        }
    }
}