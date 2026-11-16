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
import androidx.compose.material3.Card
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.Icon
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.TextButton
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.PlayArrow
import androidx.compose.material.icons.outlined.Pause
import androidx.compose.material.icons.outlined.Stop
import androidx.compose.material.icons.outlined.Refresh
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
import androidx.compose.ui.res.painterResource
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
import com.copilotomotorista.R
import com.copilotomotorista.ui.theme.CopilotoMotoristaTheme
import kotlinx.coroutines.delay

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
    var confirmEnd by remember { mutableStateOf(false) }
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
    Column(horizontalAlignment = Alignment.End, verticalArrangement = Arrangement.spacedBy(8.dp), modifier = Modifier.padding(6.dp)) {
        if (expanded) {
            Card(modifier = Modifier.width(176.dp), shape = RoundedCornerShape(18.dp)) {
                Column(modifier = Modifier.padding(10.dp), verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text(
                        when {
                            !snapshot.monitoringConnected -> "Sistema desconectado"
                            snapshot.active && snapshot.paused -> "Expediente pausado"
                            snapshot.active -> "Sistema funcionando"
                            else -> "Sistema pronto"
                        },
                        style = MaterialTheme.typography.labelMedium,
                        fontWeight = FontWeight.Bold
                    )
                    if (!snapshot.monitoringConnected) {
                        Text("Reative a Acessibilidade nas configurações do app.", style = MaterialTheme.typography.bodySmall, color = MaterialTheme.colorScheme.error)
                    } else if (!snapshot.active) {
                        ControlAction(Icons.Outlined.PlayArrow, "Iniciar") { store.start(); snapshot = store.snapshot() }
                    } else if (snapshot.paused) {
                        ControlAction(Icons.Outlined.Refresh, "Retomar") { store.resume(); snapshot = store.snapshot() }
                    } else {
                        ControlAction(Icons.Outlined.Pause, "Pausar") { store.pause(); snapshot = store.snapshot() }
                    }
                    if (snapshot.active) {
                        ControlAction(Icons.Outlined.Stop, "Finalizar", Color(0xFFEF4444)) { confirmEnd = true }
                    }
                }
            }
        }
        Surface(
            color = statusColor,
            shape = CircleShape,
            shadowElevation = 8.dp,
            modifier = Modifier.size(48.dp)
                .pointerInput(Unit) {
                    detectDragGestures { change, amount ->
                        change.consume()
                        onDrag(amount.x, amount.y)
                    }
                }
                .clickable { expanded = !expanded }
        ) {
            Box(contentAlignment = Alignment.Center) {
                Icon(painter = painterResource(R.drawable.copiloto_pro_mark), contentDescription = "Copiloto Pro", tint = Color.Unspecified, modifier = Modifier.size(25.dp))
            }
        }
    }
    if (confirmEnd) {
        AlertDialog(
            onDismissRequest = { confirmEnd = false },
            title = { Text("Finalizar expediente?") },
            text = { Text("O monitoramento das ofertas será interrompido até você iniciar outro expediente.") },
            confirmButton = {
                TextButton(onClick = {
                    store.end()
                    snapshot = store.snapshot()
                    confirmEnd = false
                    expanded = false
                }) { Text("Finalizar") }
            },
            dismissButton = { TextButton(onClick = { confirmEnd = false }) { Text("Continuar") } }
        )
    }
}

@Composable
private fun ControlAction(
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    label: String,
    tint: Color = Color(0xFF3B82F6),
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth().clickable(onClick = onClick).padding(vertical = 5.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(10.dp)
    ) {
        Surface(color = tint.copy(alpha = 0.14f), shape = CircleShape, modifier = Modifier.size(34.dp)) {
            Box(contentAlignment = Alignment.Center) { Icon(icon, contentDescription = label, tint = tint) }
        }
        Text(label, style = MaterialTheme.typography.labelLarge, fontWeight = FontWeight.SemiBold)
    }
}
