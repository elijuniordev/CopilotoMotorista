package com.copilotomotorista.presentation.settings

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Switch
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp
import com.copilotomotorista.data.preferences.AppPreferencesDataStore
import com.copilotomotorista.data.preferences.OverlayDisplayPreferences
import kotlinx.coroutines.launch

@Composable
fun OverlaySettingsScreen(preferences: AppPreferencesDataStore) {
    val display by preferences.overlayDisplayPreferences.collectAsState(
        initial = OverlayDisplayPreferences()
    )
    val scope = rememberCoroutineScope()

    fun update(next: OverlayDisplayPreferences) {
        scope.launch { preferences.setOverlayDisplayPreferences(next) }
    }

    Column(
        modifier = Modifier.fillMaxSize().padding(20.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
            Text(
                "PERSONALIZAÇÃO",
                color = MaterialTheme.colorScheme.primary,
                style = MaterialTheme.typography.labelMedium
            )
            Text(
                "Card flutuante",
                color = MaterialTheme.colorScheme.onBackground,
                style = MaterialTheme.typography.headlineLarge
            )
            Text(
                "Escolha quais informações aparecem na análise das ofertas. As alterações ficam salvas neste aparelho.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                style = MaterialTheme.typography.bodyMedium
            )
        }

        DisplayPreferenceRow(
            title = "Categoria da corrida",
            description = "Exibe UberX, Comfort, 99Pop e outras categorias quando identificadas.",
            checked = display.showRideCategory,
            onCheckedChange = { update(display.copy(showRideCategory = it)) }
        )
        DisplayPreferenceRow(
            title = "Forma de pagamento",
            description = "Exibe dinheiro, cartão ou Pix quando a informação é identificada na tela.",
            checked = display.showPaymentMethod,
            onCheckedChange = { update(display.copy(showPaymentMethod = it)) }
        )
        DisplayPreferenceRow(
            title = "Distância e duração",
            description = "Exibe a distância total e o tempo estimado no rodapé do card.",
            checked = display.showDistanceAndTime,
            onCheckedChange = { update(display.copy(showDistanceAndTime = it)) }
        )
        Text(
            "Os indicadores de R$/km, R$/hora e lucro permanecem visíveis para preservar a análise principal da corrida.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodySmall
        )
    }
}

@Composable
private fun DisplayPreferenceRow(
    title: String,
    description: String,
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceContainer
        )
    ) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(16.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            Column(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(4.dp)
            ) {
                Text(
                    title,
                    color = MaterialTheme.colorScheme.onSurface,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    description,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    style = MaterialTheme.typography.bodySmall
                )
            }
            Switch(checked = checked, onCheckedChange = onCheckedChange)
        }
    }
}
