package com.example.ui.components

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.model.UiState

/** Solo representa estado y emite Reintentar. No obtiene ni modifica datos. */
@Composable
fun DataStateContent(state: UiState<Unit>, onRetry: () -> Unit, content: @Composable () -> Unit) {
    Surface(modifier = Modifier.fillMaxSize(), color = MaterialTheme.colorScheme.background) {
        when (state) {
            UiState.Loading -> Box(Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    CircularProgressIndicator()
                    Text("Cargando tus datos…")
                }
            }
            is UiState.Error -> Box(Modifier.fillMaxSize().padding(24.dp), contentAlignment = Alignment.Center) {
                Column(horizontalAlignment = Alignment.CenterHorizontally, verticalArrangement = Arrangement.spacedBy(16.dp)) {
                    Text("No se pudieron cargar tus datos", style = MaterialTheme.typography.titleLarge, textAlign = TextAlign.Center)
                    Text(state.message, textAlign = TextAlign.Center)
                    Button(onClick = onRetry) { Text("Reintentar") }
                }
            }
            // El vacío se representa en Tracker/Feed; Perfil y Crear hábito siguen disponibles.
            UiState.Empty, is UiState.Success -> content()
        }
    }
}
