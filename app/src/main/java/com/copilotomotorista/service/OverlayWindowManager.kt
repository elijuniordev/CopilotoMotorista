package com.copilotomotorista.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.ui.platform.ComposeView
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import com.copilotomotorista.domain.usecase.OfferAnalysisResult
import com.copilotomotorista.presentation.overlay.OfferOverlayUi
import com.copilotomotorista.ui.theme.CopilotoMotoristaTheme

/**
 * Supplies the lifecycle owners required by ComposeView when it is hosted in
 * a WindowManager overlay rather than inside an Activity.
 */
private class OverlayViewTreeOwner :
    LifecycleOwner,
    SavedStateRegistryOwner,
    ViewModelStoreOwner {

    private val lifecycleRegistry = LifecycleRegistry(this)
    private val savedStateController = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle
        get() = lifecycleRegistry
    override val savedStateRegistry: SavedStateRegistry
        get() = savedStateController.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    init {
        savedStateController.performAttach()
        savedStateController.performRestore(null)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun moveToResumed() {
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        if (lifecycleRegistry.currentState == Lifecycle.State.DESTROYED) return
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        lifecycleRegistry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}

class OverlayWindowManager(private val context: Context) {

    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var overlayView: ComposeView? = null
    private var overlayOwner: OverlayViewTreeOwner? = null

    fun showOverlay(
        analysisResult: OfferAnalysisResult,
        grossFare: Double,
        isDemo: Boolean = false,
        platform: String = "Aplicativo",
        rideCategory: String = "Categoria não identificada",
        paymentMethod: String = "Pagamento não identificado",
        onDismiss: () -> Unit,
        onAccepted: () -> Unit = {},
        onCompleted: () -> Unit = {}
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

        val owner = OverlayViewTreeOwner()
        val view = ComposeView(context).apply {
            setViewTreeLifecycleOwner(owner)
            setViewTreeSavedStateRegistryOwner(owner)
            setViewTreeViewModelStoreOwner(owner)
            setContent {
                CopilotoMotoristaTheme {
                    OfferOverlayUi(
                        analysisResult = analysisResult,
                        grossFare = grossFare,
                        isDemo = isDemo,
                        platform = platform,
                        rideCategory = rideCategory,
                        paymentMethod = paymentMethod,
                        onDismiss = {
                            hideOverlay()
                            onDismiss()
                        },
                        onAccepted = {
                            onAccepted()
                            hideOverlay()
                        },
                        onCompleted = {
                            onCompleted()
                            hideOverlay()
                        }
                    )
                }
            }
        }

        try {
            windowManager.addView(view, params)
            owner.moveToResumed()
            overlayView = view
            overlayOwner = owner
        } catch (_: SecurityException) {
            view.disposeComposition()
            owner.destroy()
            overlayView = null
            overlayOwner = null
        } catch (_: WindowManager.BadTokenException) {
            view.disposeComposition()
            owner.destroy()
            overlayView = null
            overlayOwner = null
        } catch (exception: RuntimeException) {
            view.disposeComposition()
            owner.destroy()
            overlayView = null
            overlayOwner = null
            throw exception
        }
    }

    fun hideOverlay() {
        val view = overlayView
        overlayView = null
        try {
            view?.let {
                it.disposeComposition()
                if (it.isAttachedToWindow) {
                    windowManager.removeView(it)
                }
            }
        } catch (_: IllegalArgumentException) {
            // A view may already have been removed by the system.
        } finally {
            overlayOwner?.destroy()
            overlayOwner = null
        }
    }
}
