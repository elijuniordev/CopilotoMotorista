package com.copilotomotorista.service

import android.content.Context
import android.graphics.PixelFormat
import android.os.Build
import android.provider.Settings
import android.view.Gravity
import android.view.WindowManager
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.LifecycleRegistry
import androidx.lifecycle.ViewModelStore
import androidx.lifecycle.ViewModelStoreOwner
import androidx.lifecycle.setViewTreeLifecycleOwner
import androidx.lifecycle.setViewTreeViewModelStoreOwner
import androidx.savedstate.SavedStateRegistry
import androidx.savedstate.SavedStateRegistryController
import androidx.savedstate.SavedStateRegistryOwner
import androidx.savedstate.setViewTreeSavedStateRegistryOwner
import com.copilotomotorista.ui.theme.CopilotoMotoristaTheme
import kotlinx.coroutines.delay
import java.text.NumberFormat
import java.util.Locale

private class FloatingViewTreeOwner : LifecycleOwner, SavedStateRegistryOwner, ViewModelStoreOwner {
    private val registry = LifecycleRegistry(this)
    private val savedState = SavedStateRegistryController.create(this)
    override val lifecycle: Lifecycle get() = registry
    override val savedStateRegistry: SavedStateRegistry get() = savedState.savedStateRegistry
    override val viewModelStore = ViewModelStore()

    init {
        savedState.performAttach()
        savedState.performRestore(null)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_CREATE)
    }

    fun resume() {
        registry.handleLifecycleEvent(Lifecycle.Event.ON_START)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_RESUME)
    }

    fun destroy() {
        if (registry.currentState == Lifecycle.State.DESTROYED) return
        registry.handleLifecycleEvent(Lifecycle.Event.ON_PAUSE)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_STOP)
        registry.handleLifecycleEvent(Lifecycle.Event.ON_DESTROY)
        viewModelStore.clear()
    }
}

/** A janela do controle é independente da janela usada pelo card de ofertas. */
class FloatingControlWindowManager(
    private val context: Context,
    private val store: ShiftSessionStore
) {
    private val windowManager = context.getSystemService(Context.WINDOW_SERVICE) as WindowManager
    private var view: ComposeView? = null
    private var owner: FloatingViewTreeOwner? = null
    private var params: WindowManager.LayoutParams? = null

    fun show() {
        if (view != null || !Settings.canDrawOverlays(context)) return
        val windowType = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY
        } else WindowManager.LayoutParams.TYPE_PHONE

        val layout = WindowManager.LayoutParams(
            WindowManager.LayoutParams.WRAP_CONTENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
            windowType,
            WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE or
                WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN,
            PixelFormat.TRANSLUCENT
        ).apply {
            gravity = Gravity.END or Gravity.CENTER_VERTICAL
            x = 12
            y = 0
        }
        val treeOwner = FloatingViewTreeOwner()
        val composeView = ComposeView(context).apply {
            setViewTreeLifecycleOwner(treeOwner)
            setViewTreeSavedStateRegistryOwner(treeOwner)
            setViewTreeViewModelStoreOwner(treeOwner)
            setContent {
                CopilotoMotoristaTheme {
                    FloatingControlContent(
                        store = store,
                        onDrag = { dx, dy ->
                            layout.x = (layout.x - dx.toInt()).coerceAtLeast(0)
                            layout.y += dy.toInt()
                            runCatching { windowManager.updateViewLayout(this, layout) }
                        }
                    )
                }
            }
        }
        try {
            windowManager.addView(composeView, layout)
            treeOwner.resume()
            view = composeView
            owner = treeOwner
            params = layout
        } catch (_: SecurityException) {
            composeView.disposeComposition()
            treeOwner.destroy()
        } catch (_: WindowManager.BadTokenException) {
            composeView.disposeComposition()
            treeOwner.destroy()
        }
    }

    fun hide() {
        val current = view
        view = null
        params = null
        try {
            current?.let {
                it.disposeComposition()
                if (it.isAttachedToWindow) windowManager.removeView(it)
            }
        } catch (_: IllegalArgumentException) {
            // A janela já pode ter sido removida pelo Android.
        } finally {
            owner?.destroy()
            owner = null
        }
    }
}

@Composable
private fun FloatingControlContent(store: ShiftSessionStore, onDrag: (Float, Float) -> Unit) {
    var expanded by remember { mutableStateOf(false) }
    var snapshot by remember { mutableStateOf(store.snapshot()) }
    LaunchedEffect(Unit) {
        while (true) {
            snapshot = store.snapshot()
            delay(1_000L)
        }
    }
    val statusColor = when {
        !snapshot.monitoringConnected -> Color(0xFF64748B)
        snapshot.active && snapshot.paused -> Color(0xFFFACC15)
        snapshot.active -> Color(0xFF22C55E)
        else -> Color(0xFF3B82F6)
    }

    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(8.dp),
        modifier = Modifier.padding(6.dp)
    ) {
        if (expanded) {
            Card(
                modifier = Modifier.width(260.dp),
                shape = RoundedCornerShape(18.dp)
            ) {
                Column(
                    modifier = Modifier.padding(14.dp),
                    verticalArrangement = Arrangement.spacedBy(9.dp)
                ) {
                    Text("COPILOTO PRO", style = MaterialTheme.typography.labelMedium, color = MaterialTheme.colorScheme.primary, fontWeight = FontWeight.Bold)
                    Text(
                        when {
                            !snapshot.monitoringConnected -> "Monitoramento desconectado"
                            snapshot.active && snapshot.paused -> "Turno pausado"
                            snapshot.active -> "Turno ativo · monitorando"
                            else -> "Pronto para iniciar"
                        },
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Bold
                    )
                    Text("Turno: ${formatElapsed(snapshot.elapsedMillis())}", style = MaterialTheme.typography.bodySmall)
                    Text("${snapshot.completedTrips} corridas · ${formatMoney(snapshot.receivedFareCents)}", style = MaterialTheme.typography.bodySmall)
                    when {
                        !snapshot.monitoringConnected -> Text("Abra o Copiloto e reative o serviço de acessibilidade.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                        !snapshot.active -> Button(onClick = { store.start(); snapshot = store.snapshot() }, modifier = Modifier.fillMaxWidth()) { Text("Iniciar turno") }
                        snapshot.paused -> Button(onClick = { store.resume(); snapshot = store.snapshot() }, modifier = Modifier.fillMaxWidth()) { Text("Retomar turno") }
                        else -> OutlinedButton(onClick = { store.pause(); snapshot = store.snapshot() }, modifier = Modifier.fillMaxWidth()) { Text("Pausar turno") }
                    }
                    if (snapshot.active) {
                        OutlinedButton(onClick = { store.end(); snapshot = store.snapshot() }, modifier = Modifier.fillMaxWidth()) { Text("Encerrar turno") }
                    }
                    Text("Toque na bolha para recolher.", style = MaterialTheme.typography.labelSmall, color = MaterialTheme.colorScheme.onSurfaceVariant)
                }
            }
        }
        Surface(
            color = statusColor,
            shape = CircleShape,
            shadowElevation = 8.dp,
            modifier = Modifier
                .size(56.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        onDrag(amount.x, amount.y)
                    }
                }
                .clickable { expanded = !expanded }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Text("CP", color = Color(0xFF0B1220), fontWeight = FontWeight.ExtraBold)
            }
        }
    }
}

private fun formatElapsed(milliseconds: Long): String {
    val totalSeconds = milliseconds / 1_000L
    val hours = totalSeconds / 3_600L
    val minutes = (totalSeconds % 3_600L) / 60L
    return if (hours > 0) "${hours}h ${minutes}min" else "${minutes}min"
}

private fun formatMoney(cents: Long): String =
    NumberFormat.getCurrencyInstance(Locale.forLanguageTag("pt-BR")).format(cents / 100.0)
