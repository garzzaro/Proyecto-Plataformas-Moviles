package com.example.ui

import android.util.Patterns
import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.ui.unit.dp
import com.example.model.UserEntity

// ============================================================================
// EDICIÓN DEL PERFIL LOCAL: ProfileEditDialog
// ============================================================================

/**
 * ProfileEditDialog permite modificar los datos del perfil local.
 *
 * - **¿Para qué sirve?** Captura nombre y correo opcional sin representar una autenticación remota
 *   inexistente.
 * - **¿Cómo funciona?** Valida las entradas, mantiene el borrador y delega el guardado; bloquea el
 *   cierre mientras se confirma la escritura.
 * - **¿Qué pasa si lo quitamos?** El usuario no tendría una interfaz para modificar su perfil
 *   persistente.
 */
@Composable
fun ProfileEditDialog(user: UserEntity?, onDismiss: () -> Unit, onSave: (String, String?) -> Unit, isSaving: Boolean = false) {
    var name by rememberSaveable { mutableStateOf(user?.displayName.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(user?.email.orEmpty()) }
    val validEmail = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    AlertDialog(onDismissRequest = { if (!isSaving) onDismiss() },
        title = { Text("Perfil local") },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text("Tus datos se guardan en este dispositivo.")
                OutlinedTextField(enabled = !isSaving, value = name, onValueChange = { name = it }, label = { Text("Nombre") }, singleLine = true)
                OutlinedTextField(enabled = !isSaving, value = email, onValueChange = { email = it }, label = { Text("Correo (opcional)") }, singleLine = true, isError = !validEmail)
            }
        },
        confirmButton = { TextButton(enabled = !isSaving && name.isNotBlank() && validEmail, onClick = { onSave(name, email.takeIf { it.isNotBlank() }) }) { Text("Guardar") } },
        dismissButton = { TextButton(enabled = !isSaving, onClick = onDismiss) { Text("Cancelar") } })
}
