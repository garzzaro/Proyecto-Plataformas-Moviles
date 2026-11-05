package com.example.ui

import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.AvatarCatalog
import com.example.model.ImageOptimizationResult
import com.example.model.ImageOptimizer
import com.example.model.UserEntity
import com.example.ui.components.HabitImage
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal

// ============================================================================
// EDICIÓN DEL PERFIL Y FOTO OPTIMIZADA: ProfileEditDialog
// ============================================================================

/**
 * ProfileEditDialog permite modificar nombre, correo opcional y foto de perfil (desde galería,
 * cámara con optimización automática o avatares ilustrados predefinidos).
 */
@Composable
fun ProfileEditDialog(
    user: UserEntity?,
    onDismiss: () -> Unit,
    onSave: (String, String?) -> Unit,
    isSaving: Boolean = false,
    isEnglish: Boolean = false,
    onSaveWithAvatar: ((name: String, email: String?, avatarUrl: String?) -> Unit)? = null
) {
    val context = LocalContext.current
    var name by rememberSaveable { mutableStateOf(user?.displayName.orEmpty()) }
    var email by rememberSaveable { mutableStateOf(user?.email.orEmpty()) }
    var selectedAvatarUrl by rememberSaveable { mutableStateOf(user?.avatarUrl) }
    var optimizationResult by remember { mutableStateOf<ImageOptimizationResult?>(null) }
    var optimizerError by remember { mutableStateOf<String?>(null) }

    val validEmail = email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()

    // Selector de imagen desde Galería -> Optimización automática para Avatar
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                ImageOptimizer.optimizeImageFromUri(context, uri, forAvatar = true)
            }.onSuccess { result ->
                selectedAvatarUrl = result.optimizedUri
                optimizationResult = result
                optimizerError = null
            }.onFailure { err ->
                optimizerError = err.message ?: if (isEnglish) "Could not optimize image" else "No se pudo optimizar la imagen"
            }
        }
    }

    // Captura de Selfie con Cámara -> Optimización automática para Avatar
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            runCatching {
                ImageOptimizer.optimizeBitmapToInternalFile(context, bitmap, forAvatar = true)
            }.onSuccess { result ->
                selectedAvatarUrl = result.optimizedUri
                optimizationResult = result
                optimizerError = null
            }.onFailure { err ->
                optimizerError = err.message ?: if (isEnglish) "Could not optimize photo" else "No se pudo optimizar la foto"
            }
        }
    }

    AlertDialog(
        onDismissRequest = { if (!isSaving) onDismiss() },
        properties = DialogProperties(usePlatformDefaultWidth = false),
        title = {
            Text(
                text = if (isEnglish) "Edit profile & photo" else "Perfil local y foto",
                fontWeight = FontWeight.Bold
            )
        },
        text = {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(12.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Text(
                    text = if (isEnglish) {
                        "Your profile and optimized avatar are saved on this device and synced with your #Code."
                    } else {
                        "Tus datos se guardan en este dispositivo y se sincronizan con tu #Código."
                    },
                    fontSize = 12.sp,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.fillMaxWidth()
                )

                // Vista previa circular grande (96.dp) de la foto de perfil actual
                Box(
                    contentAlignment = Alignment.BottomEnd,
                    modifier = Modifier.padding(top = 4.dp)
                ) {
                    HabitImage(
                        model = selectedAvatarUrl ?: user?.avatarUrl ?: R.drawable.ic_pulpo,
                        contentDescription = if (isEnglish) "Current profile photo" else "Foto de perfil actual",
                        modifier = Modifier
                            .size(96.dp)
                            .clip(CircleShape)
                            .border(3.dp, PrimaryTeal, CircleShape),
                        contentScale = ContentScale.Crop
                    )

                    Box(
                        modifier = Modifier
                            .size(28.dp)
                            .background(SecondaryTeal, CircleShape)
                            .border(2.dp, MaterialTheme.colorScheme.surface, CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Check,
                            contentDescription = null,
                            tint = Color(0xFF00332C),
                            modifier = Modifier.size(16.dp)
                        )
                    }
                }

                // Etiqueta verde de resultado del módulo de optimización de imágenes
                optimizationResult?.let { result ->
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFF1B5E20).copy(alpha = 0.14f), RoundedCornerShape(10.dp))
                            .border(1.dp, Color(0xFF2E7D32).copy(alpha = 0.6f), RoundedCornerShape(10.dp))
                            .padding(horizontal = 10.dp, vertical = 6.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = "✅ ${ImageOptimizer.formatSizeLabel(result, isEnglish)}",
                            color = Color(0xFF2E7D32),
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            textAlign = TextAlign.Center
                        )
                    }
                }

                optimizerError?.let { errText ->
                    Text(
                        text = errText,
                        color = MaterialTheme.colorScheme.error,
                        fontSize = 11.sp
                    )
                }

                // Botones para subir o tomar foto propia con compresión automática
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    OutlinedButton(
                        enabled = !isSaving,
                        onClick = { galleryLauncher.launch("image/*") },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "🖼️ Upload Photo" else "🖼️ Subir de Galería",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }

                    OutlinedButton(
                        enabled = !isSaving,
                        onClick = { cameraLauncher.launch(null) },
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp),
                        contentPadding = PaddingValues(horizontal = 8.dp, vertical = 8.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "📷 Take Photo" else "📷 Tomar Selfie",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            maxLines = 1
                        )
                    }
                }

                // Fila rápida (LazyRow) de 6 avatares ilustrados predefinidos
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = if (isEnglish) "Or choose a preset avatar:" else "O elige un avatar predefinido:",
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(10.dp),
                        contentPadding = PaddingValues(horizontal = 2.dp)
                    ) {
                        items(AvatarCatalog.PRESET_AVATARS, key = { it.id }) { preset ->
                            val presetUrl = preset.remoteUrl
                            val isSelected = (presetUrl == null && selectedAvatarUrl.isNullOrBlank()) ||
                                (presetUrl != null && selectedAvatarUrl == presetUrl)

                            Box(
                                modifier = Modifier
                                    .size(48.dp)
                                    .clip(CircleShape)
                                    .border(
                                        width = if (isSelected) 2.5.dp else 1.dp,
                                        color = if (isSelected) PrimaryTeal else Color.Gray.copy(alpha = 0.35f),
                                        shape = CircleShape
                                    )
                                    .clickable(enabled = !isSaving) {
                                        selectedAvatarUrl = presetUrl
                                        optimizationResult = null
                                        optimizerError = null
                                    },
                                contentAlignment = Alignment.Center
                            ) {
                                HabitImage(
                                    model = presetUrl ?: R.drawable.ic_pulpo,
                                    contentDescription = preset.title,
                                    modifier = Modifier
                                        .size(44.dp)
                                        .clip(CircleShape),
                                    contentScale = ContentScale.Crop
                                )
                            }
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                OutlinedTextField(
                    enabled = !isSaving,
                    value = name,
                    onValueChange = { name = it },
                    label = { Text(if (isEnglish) "Name" else "Nombre") },
                    singleLine = true,
                    modifier = Modifier.fillMaxWidth()
                )

                OutlinedTextField(
                    enabled = !isSaving,
                    value = email,
                    onValueChange = { email = it },
                    label = { Text(if (isEnglish) "Email (optional)" else "Correo (opcional)") },
                    singleLine = true,
                    isError = !validEmail,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        },
        confirmButton = {
            TextButton(
                enabled = !isSaving && name.isNotBlank() && validEmail,
                onClick = {
                    val cleanName = name.trim()
                    val cleanEmail = email.trim().takeIf { it.isNotBlank() }
                    if (onSaveWithAvatar != null) {
                        onSaveWithAvatar(cleanName, cleanEmail, selectedAvatarUrl)
                    } else {
                        onSave(cleanName, cleanEmail)
                    }
                }
            ) {
                Text(if (isEnglish) "Save" else "Guardar")
            }
        },
        dismissButton = {
            TextButton(
                enabled = !isSaving,
                onClick = onDismiss
            ) {
                Text(if (isEnglish) "Cancel" else "Cancelar")
            }
        }
    )
}
