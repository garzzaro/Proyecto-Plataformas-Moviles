package com.example.ui.components

import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.outlined.AddCircleOutline
import androidx.compose.material.icons.outlined.RadioButtonUnchecked
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.ui.unit.dp
import com.example.model.HabitItem

// ============================================================================
// ICONOS DE CUMPLIMIENTO: HabitStatusIcon
// ============================================================================

/**
 * HabitStatusIcon unifica los iconos de estado del seguimiento.
 *
 * - **¿Para qué sirve?** Mantiene la misma representación de cumplimiento en las tarjetas y el
 *   historial.
 * - **¿Cómo funciona?** Prioriza el check completado y diferencia cantidad, duración y confirmación
 *   simple cuando hay avance pendiente.
 * - **¿Qué pasa si lo quitamos?** Un mismo estado podría tener iconos diferentes según la pantalla.
 */
@Composable
fun HabitStatusIcon(completed: Boolean, progressive: Boolean, duration: String, description: String? = null) {
    Icon(
        imageVector = when {
            completed -> Icons.Filled.CheckCircle
            progressive -> Icons.Outlined.AddCircleOutline
            duration.isNotBlank() -> Icons.Outlined.Timer
            else -> Icons.Outlined.RadioButtonUnchecked
        },
        contentDescription = description,
        tint = if (completed) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurfaceVariant
    )
}

// ============================================================================
// CONTROL DE CUMPLIMIENTO: HabitCompletionControl
// ============================================================================

/**
 * HabitCompletionControl muestra la acción de cumplimiento de una tarjeta.
 *
 * - **¿Para qué sirve?** Permite registrar cambios y consultar el avance de una meta numérica.
 * - **¿Cómo funciona?** Combina el icono compartido con el contador y la barra; delega el guardado al
 *   callback de la pantalla.
 * - **¿Qué pasa si lo quitamos?** Las tarjetas perderían el control para registrar o consultar su
 *   progreso.
 */
@Composable
fun HabitCompletionControl(habit: HabitItem, onClick: () -> Unit) {
    Column(horizontalAlignment = androidx.compose.ui.Alignment.CenterHorizontally, modifier = Modifier.widthIn(max = 96.dp)) {
        IconButton(onClick = onClick, modifier = Modifier.size(48.dp)) {
            HabitStatusIcon(habit.isCompleted, habit.isProgressive, habit.timeOrDuration,
                if (habit.isCompleted) "Completado. Cambiar cumplimiento de ${habit.title}" else "Registrar avance de ${habit.title}")
        }
        if (habit.isProgressive) {
            Text("${habit.currentProgress}/${habit.maxProgress}", style = MaterialTheme.typography.labelSmall)
            LinearProgressIndicator(
                progress = { (habit.currentProgress.toFloat() / habit.maxProgress.coerceAtLeast(1)).coerceIn(0f, 1f) },
                modifier = Modifier.width(64.dp).padding(top = 4.dp)
            )
        }
    }
}

// ============================================================================
// DIÁLOGO DE AVANCE: HabitProgressDialog
// ============================================================================

/**
 * HabitProgressDialog permite registrar cantidades o confirmar hábitos con duración.
 *
 * - **¿Para qué sirve?** Admite metas grandes y ofrece una forma explícita de deshacer el avance.
 * - **¿Cómo funciona?** Conserva la entrada del formulario, valida el rango y entrega la cantidad al
 *   ViewModel para guardarla en Room.
 * - **¿Qué pasa si lo quitamos?** Los hábitos de cantidad dependerían de toques individuales y no
 *   tendrían una edición clara de avance.
 */
@Composable
fun HabitProgressDialog(habit: HabitItem, onDismiss: () -> Unit, onSave: (Int) -> Unit) {
    var value by rememberSaveable(habit.id, habit.currentProgress) { mutableStateOf(habit.currentProgress.toString()) }
    // Una entrada inválida no debe modificar el registro persistido.
    val amount = value.toIntOrNull()
    val valid = amount != null && amount in 0..habit.maxProgress
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(habit.title) },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                if (habit.isProgressive) {
                    Text("Registra el avance de ${if (habit.frequency == "Semanal") "esta semana" else "hoy"}. Meta: ${habit.maxProgress} ${habit.progressUnit}")
                    OutlinedTextField(value = value, onValueChange = { value = it }, singleLine = true,
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        label = { Text("Avance acumulado") }, isError = !valid,
                        supportingText = { if (!valid) Text("Ingresa un número entre 0 y ${habit.maxProgress}") })
                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedButton(onClick = { value = ((amount ?: 0) - 1).coerceAtLeast(0).toString() }) { Text("−1") }
                        OutlinedButton(onClick = { value = ((amount ?: 0).coerceAtMost(habit.maxProgress - 1) + 1).toString() }) { Text("+1") }
                    }
                } else {
                    // La duración es orientativa: el usuario confirma el cumplimiento al terminar.
                    Text("Duración estimada: ${habit.timeOrDuration}. Confirma el cumplimiento cuando termines.")
                }
                if (habit.currentProgress > 0) {
                    TextButton(onClick = { onSave(0); onDismiss() }) { Text("Deshacer avance") }
                }
            }
        },
        confirmButton = {
            TextButton(enabled = !habit.isProgressive || valid, onClick = {
                onSave(if (habit.isProgressive) amount!! else habit.maxProgress)
                onDismiss()
            }) { Text(if (habit.isProgressive) "Guardar avance" else "Marcar completado") }
        },
        dismissButton = { TextButton(onClick = onDismiss) { Text("Cancelar") } }
    )
}
