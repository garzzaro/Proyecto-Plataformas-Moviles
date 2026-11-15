package com.example.ui

import android.util.Patterns
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.AnimatedVisibility
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
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountCircle
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.Email
import androidx.compose.material.icons.filled.HelpOutline
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.SwapHoriz
import androidx.compose.material.icons.filled.Visibility
import androidx.compose.material.icons.filled.VisibilityOff
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.model.AuthSession
import com.example.model.AvatarCatalog
import com.example.model.CloudSyncManager
import com.example.model.HabitDatabase
import com.example.model.ImageOptimizationResult
import com.example.model.ImageOptimizer
import com.example.model.PetAccessory
import com.example.model.PetType
import com.example.ui.components.HabitImage
import com.example.ui.theme.DarkTealBackground
import com.example.ui.theme.DeepDarkTeal
import com.example.ui.theme.LightTealAccent
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGrayDark
import com.example.ui.theme.TextGrayLight
import com.example.ui.theme.TextLight
import com.example.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext

/**
 * Pantalla elegante de Inicio de Sesión, Registro Multi-Usuario y Foto Optimizada por Dispositivo para HabitOcto.
 *
 * - **¿Para qué sirve?** Brinda una interfaz intuitiva para que cualquier persona que instale el APK cree su propia cuenta
 *   personalizada, elija su foto de perfil optimizada con [ImageOptimizer], obtenga su código único (#100..#999) o alterne entre cuentas guardadas.
 * - **¿Cómo funciona?**
 *   1. Permite alternar entre pestañas de "Iniciar Sesión" y "Crear Cuenta".
 *   2. Integra selector de foto de perfil con compresión y recorte automático en formato cuadrado 384x384.
 *   3. Despliega la lista de cuentas guardadas en el dispositivo para conmutar de usuario con un solo toque.
 *   4. Persiste la sesión en Room y SharedPreferences mediante [CloudSyncManager] y sincroniza con Firestore/REST.
 * - **¿Qué pasa si lo quitamos?** Los nuevos usuarios no podrían registrarse ni personalizar su perfil, obligando a todos a compartir una única cuenta anónima.
 */
@Composable
fun AuthScreen(
    onAuthSuccess: (AuthSession) -> Unit,
    onDismiss: (() -> Unit)? = null,
    initialName: String = "",
    initialEmail: String = "",
    initialTag: String = "",
    initialAvatarUrl: String? = null,
    completedHabits: Int = 0,
    currentStreak: Int = 0,
    isEnglish: Boolean = false,
    allowRegistration: Boolean = true,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()
    val syncManager = remember { CloudSyncManager.getInstance(context) }
    val currentSession by syncManager.currentSession.collectAsState()
    val savedSessions by syncManager.savedSessions.collectAsState()
    val syncState by syncManager.syncState.collectAsState()
    val isDark = isAppInDarkTheme

    var isRegisterMode by rememberSaveable(allowRegistration) {
        mutableStateOf(allowRegistration && savedSessions.isEmpty())
    }
    var name by rememberSaveable {
        mutableStateOf(if (allowRegistration && savedSessions.isEmpty()) "" else initialName)
    }
    var email by rememberSaveable {
        mutableStateOf(if (allowRegistration && savedSessions.isEmpty()) "" else initialEmail)
    }
    var password by rememberSaveable { mutableStateOf("") }
    var showPassword by rememberSaveable { mutableStateOf(false) }
    var selectedAvatarUrl by rememberSaveable {
        mutableStateOf(initialAvatarUrl)
    }
    var optimizationResult by remember { mutableStateOf<ImageOptimizationResult?>(null) }
    var errorMessage by remember { mutableStateOf<String?>(null) }
    var isSubmitting by remember { mutableStateOf(false) }
    var showOnboardingGuide by rememberSaveable { mutableStateOf(false) }

    // Selector de imagen desde Galería con optimización automática
    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            runCatching {
                ImageOptimizer.optimizeImageFromUri(context, uri, forAvatar = true)
            }.onSuccess { result ->
                selectedAvatarUrl = result.optimizedUri
                optimizationResult = result
                errorMessage = null
            }.onFailure { err ->
                errorMessage = err.message ?: if (isEnglish) "Could not optimize image" else "No se pudo optimizar la imagen"
            }
        }
    }

    // Captura de Selfie con Cámara con optimización automática
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            runCatching {
                ImageOptimizer.optimizeBitmapToInternalFile(context, bitmap, forAvatar = true)
            }.onSuccess { result ->
                selectedAvatarUrl = result.optimizedUri
                optimizationResult = result
                errorMessage = null
            }.onFailure { err ->
                errorMessage = err.message ?: if (isEnglish) "Could not optimize photo" else "No se pudo optimizar la foto"
            }
        }
    }

    val cleanTagPreview = remember(name, email, initialTag, isRegisterMode) {
        if (!isRegisterMode && CloudSyncManager.isValidTagCode(initialTag)) {
            CloudSyncManager.normalizeTagCode(initialTag)
        } else if (name.isBlank() && email.isBlank()) {
            "---"
        } else {
            syncManager.assignUniqueSystemTagLocal(name, email)
        }
    }

    val isEmailValid = remember(email) {
        email.isBlank() || Patterns.EMAIL_ADDRESS.matcher(email.trim()).matches()
    }

    val headerGradient = Brush.verticalGradient(
        colors = if (isDark) {
            listOf(DeepDarkTeal, DarkTealBackground, Color(0xFF0F172A))
        } else {
            listOf(PrimaryTeal, DarkTealBackground)
        }
    )

    Column(
        modifier = modifier
            .fillMaxWidth()
            .then(
                if (onDismiss == null) Modifier.statusBarsPadding().navigationBarsPadding() else Modifier
            )
            .verticalScroll(rememberScrollState())
            .background(
                color = if (isDark) Color(0xFF121212) else Color(0xFFF8F9FA),
                shape = RoundedCornerShape(if (onDismiss == null) 0.dp else 28.dp)
            )
            .padding(bottom = 24.dp)
    ) {
        // Cabecera superior con identidad HabitOcto, foto circular y vista previa del Tag #XXX
        Box(
            modifier = Modifier
                .fillMaxWidth()
                .clip(RoundedCornerShape(topStart = 28.dp, topEnd = 28.dp))
                .background(headerGradient)
                .padding(horizontal = 24.dp, vertical = 22.dp)
        ) {
            Column(
                modifier = Modifier.fillMaxWidth(),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                // Insignia de estado Dual Cloud + REST
                Row(
                    modifier = Modifier
                        .background(Color.White.copy(alpha = 0.14f), RoundedCornerShape(50))
                        .padding(horizontal = 12.dp, vertical = 5.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Icon(
                        imageVector = Icons.Default.CloudDone,
                        contentDescription = null,
                        tint = SecondaryTeal,
                        modifier = Modifier.size(15.dp)
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = syncState.label,
                        color = Color.White,
                        fontSize = 11.sp,
                        fontWeight = FontWeight.SemiBold
                    )
                }

                Spacer(modifier = Modifier.height(12.dp))

                // Avatar circular en vivo en la cabecera
                HabitImage(
                    model = selectedAvatarUrl ?: R.drawable.ic_pulpo,
                    contentDescription = if (isEnglish) "Profile Avatar" else "Avatar de perfil",
                    modifier = Modifier
                        .size(74.dp)
                        .clip(CircleShape)
                        .border(2.5.dp, SecondaryTeal, CircleShape),
                    contentScale = ContentScale.Crop
                )

                Spacer(modifier = Modifier.height(10.dp))

                Text(
                    text = "🐙 HabitOcto Cloud ID",
                    color = Color.White,
                    fontSize = 21.sp,
                    fontWeight = FontWeight.ExtraBold
                )

                Spacer(modifier = Modifier.height(4.dp))

                Text(
                    text = if (isEnglish) {
                        "Your account and 3-digit #Code sync your habits, streak, and pet with friends in real time (habitocto-suchini-2026)."
                    } else {
                        "Tu cuenta y tu #Código de 3 dígitos sincronizan tus hábitos, racha y mascota con amigos en tiempo real (habitocto-suchini-2026)."
                    },
                    color = Color.White.copy(alpha = 0.85f),
                    fontSize = 12.sp,
                    textAlign = TextAlign.Center,
                    lineHeight = 16.sp
                )

                Spacer(modifier = Modifier.height(12.dp))

                // Vista previa en vivo del identificador único #XXX
                Box(
                    modifier = Modifier
                        .background(Color.Black.copy(alpha = 0.32f), RoundedCornerShape(16.dp))
                        .border(1.dp, SecondaryTeal.copy(alpha = 0.7f), RoundedCornerShape(16.dp))
                        .padding(horizontal = 18.dp, vertical = 10.dp)
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Text(
                            text = name.trim().ifBlank { if (isEnglish) "New Explorer" else "Nuevo Explorador" },
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Box(
                            modifier = Modifier
                                .background(SecondaryTeal, RoundedCornerShape(8.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = "#$cleanTagPreview",
                                color = DeepDarkTeal,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                    }
                }
            }
        }

        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 20.dp, vertical = 16.dp),
            verticalArrangement = Arrangement.spacedBy(14.dp)
        ) {
            // Botón rápido para ver guía "¿Qué pasa cuando comparto el APK o clonan el repo?"
            OutlinedButton(
                onClick = { showOnboardingGuide = true },
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.HelpOutline,
                    contentDescription = null,
                    tint = PrimaryTeal,
                    modifier = Modifier.size(16.dp)
                )
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = if (isEnglish) {
                        "Sharing the APK or cloning repo? See how accounts work"
                    } else {
                        "¿Pasaste el APK a un amigo o clonaron el repo? Ver guía"
                    },
                    fontSize = 11.sp,
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
            }

            // Selector entre Iniciar Sesión y Crear Cuenta (oculto al usar "Cambiar de Usuario" dentro de Perfil)
            if (allowRegistration) {
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .background(
                            if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0),
                            RoundedCornerShape(12.dp)
                        )
                        .padding(4.dp),
                    horizontalArrangement = Arrangement.SpaceEvenly
                ) {
                    val tabs = if (isEnglish) {
                        listOf("Sign In" to false, "Create Account" to true)
                    } else {
                        listOf("Iniciar Sesión" to false, "Crear Cuenta" to true)
                    }
                    tabs.forEach { (label, mode) ->
                        val selected = isRegisterMode == mode
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .clip(RoundedCornerShape(9.dp))
                                .background(if (selected) PrimaryTeal else Color.Transparent)
                                .clickable {
                                    isRegisterMode = mode
                                    errorMessage = null
                                }
                                .padding(vertical = 8.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = label,
                                fontSize = 12.sp,
                                fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                color = if (selected) Color.White else (if (isDark) TextGrayDark else TextGrayLight)
                            )
                        }
                    }
                }
            } else {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF00362C) else Color(0xFFE0F2F1)
                    )
                ) {
                    Text(
                        text = if (isEnglish) {
                            "🔒 Switching account requires username/email/#Tag and password."
                        } else {
                            "🔒 Cambiar de usuario requiere ingresar tu usuario/correo/#Tag y contraseña."
                        },
                        fontSize = 12.sp,
                        fontWeight = FontWeight.SemiBold,
                        color = if (isDark) LightTealAccent else DarkTealBackground,
                        modifier = Modifier.padding(12.dp)
                    )
                }
            }

            // Selector de Foto de Perfil Optimizada + Nube (solo al crear cuenta nueva)
            if (isRegisterMode && allowRegistration) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    )
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(12.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Profile photo (Cloud-Synced)" else "Foto de perfil (Sincronizada en la nube)",
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) TextLight else TextDark
                        )

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { galleryLauncher.launch("image/*") },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "🖼️ Gallery" else "🖼️ Galería",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }

                            OutlinedButton(
                                onClick = { cameraLauncher.launch(null) },
                                modifier = Modifier.weight(1f),
                                shape = RoundedCornerShape(10.dp),
                                contentPadding = PaddingValues(horizontal = 8.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) "📷 Selfie" else "📷 Selfie",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        optimizationResult?.let { result ->
                            Box(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .background(Color(0xFF1B5E20).copy(alpha = 0.14f), RoundedCornerShape(8.dp))
                                    .padding(horizontal = 8.dp, vertical = 4.dp)
                            ) {
                                Text(
                                    text = "✅ ${ImageOptimizer.formatSizeLabel(result, isEnglish)}",
                                    color = Color(0xFF2E7D32),
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold
                                )
                            }
                        }

                        LazyRow(
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(AvatarCatalog.PRESET_AVATARS, key = { it.id }) { preset ->
                                val presetUrl = preset.remoteUrl
                                val isChosen = (presetUrl == null && selectedAvatarUrl.isNullOrBlank()) ||
                                    (presetUrl != null && selectedAvatarUrl == presetUrl)
                                Box(
                                    modifier = Modifier
                                        .size(40.dp)
                                        .clip(CircleShape)
                                        .border(
                                            width = if (isChosen) 2.5.dp else 1.dp,
                                            color = if (isChosen) PrimaryTeal else Color.Gray.copy(alpha = 0.35f),
                                            shape = CircleShape
                                        )
                                        .clickable {
                                            selectedAvatarUrl = presetUrl
                                            optimizationResult = null
                                        },
                                    contentAlignment = Alignment.Center
                                ) {
                                    HabitImage(
                                        model = presetUrl ?: R.drawable.ic_pulpo,
                                        contentDescription = preset.title,
                                        modifier = Modifier
                                            .size(36.dp)
                                            .clip(CircleShape),
                                        contentScale = ContentScale.Crop
                                    )
                                }
                            }
                        }
                    }
                }
            }

            // Campos según modo: Registro (Nombre, Correo, Contraseña, #Tag del Sistema) vs Login (Usuario/Correo + Contraseña)
            OutlinedTextField(
                value = if (isRegisterMode) name else email.ifBlank { name },
                onValueChange = { raw ->
                    if (isRegisterMode) {
                        name = raw
                    } else {
                        email = raw
                        name = raw
                    }
                    errorMessage = null
                },
                label = {
                    Text(
                        if (isRegisterMode) {
                            if (isEnglish) "Username" else "Nombre de usuario"
                        } else {
                            if (isEnglish) "Username, Email or #Tag" else "Usuario, Correo o #Tag"
                        }
                    )
                },
                placeholder = {
                    Text(
                        if (isRegisterMode) {
                            if (isEnglish) "E.g. Alex" else "Ej. Alex Rivero"
                        } else {
                            if (isEnglish) "user@mail.com or #101" else "correo@ejemplo.com o #101"
                        }
                    )
                },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Person, contentDescription = null, tint = PrimaryTeal)
                },
                singleLine = true,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    focusedLabelColor = PrimaryTeal
                )
            )

            if (isRegisterMode && allowRegistration) {
                OutlinedTextField(
                    value = email,
                    onValueChange = {
                        email = it
                        errorMessage = null
                    },
                    label = { Text(if (isEnglish) "Email address" else "Correo electrónico") },
                    placeholder = { Text("ejemplo@habitocto.app") },
                    leadingIcon = {
                        Icon(imageVector = Icons.Default.Email, contentDescription = null, tint = PrimaryTeal)
                    },
                    singleLine = true,
                    isError = !isEmailValid,
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Email),
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = OutlinedTextFieldDefaults.colors(
                        focusedBorderColor = PrimaryTeal,
                        focusedLabelColor = PrimaryTeal
                    )
                )
            }

            // Contraseña obligatoria tanto para crear cuenta como para iniciar sesión o cambiar de usuario
            OutlinedTextField(
                value = password,
                onValueChange = {
                    password = it
                    errorMessage = null
                },
                label = { Text(if (isEnglish) "Password (min. 4 chars)" else "Contraseña (mín. 4 caracteres)") },
                placeholder = { Text("••••••") },
                leadingIcon = {
                    Icon(imageVector = Icons.Default.Lock, contentDescription = null, tint = PrimaryTeal)
                },
                trailingIcon = {
                    TextButton(onClick = { showPassword = !showPassword }) {
                        Icon(
                            imageVector = if (showPassword) Icons.Default.VisibilityOff else Icons.Default.Visibility,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                    }
                },
                visualTransformation = if (showPassword) VisualTransformation.None else PasswordVisualTransformation(),
                singleLine = true,
                keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Password),
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(14.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    focusedLabelColor = PrimaryTeal
                )
            )

            // Código #Tag único asignado exclusivamente por el sistema (no editable)
            if (isRegisterMode && allowRegistration) {
                Card(
                    modifier = Modifier.fillMaxWidth(),
                    shape = RoundedCornerShape(14.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF0F2924) else Color(0xFFE6F7F4)
                    )
                ) {
                    Row(
                        modifier = Modifier.padding(12.dp),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(10.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .background(PrimaryTeal, RoundedCornerShape(8.dp))
                                .padding(horizontal = 10.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = "#$cleanTagPreview",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 14.sp
                            )
                        }
                        Column(modifier = Modifier.weight(1f)) {
                            Text(
                                text = if (isEnglish) {
                                    "Unique #Tag assigned by system"
                                } else {
                                    "#Tag único asignado por el sistema"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) SecondaryTeal else DeepDarkTeal
                            )
                            Text(
                                text = if (isEnglish) {
                                    "Guaranteed non-repeatable across accounts; assigned automatically."
                                } else {
                                    "No es editable ni repetible; el sistema lo asignará único en la nube."
                                },
                                fontSize = 11.sp,
                                color = if (isDark) TextGrayDark else TextGrayLight
                            )
                        }
                    }
                }
            }

            AnimatedVisibility(visible = errorMessage != null) {
                Text(
                    text = errorMessage.orEmpty(),
                    color = MaterialTheme.colorScheme.error,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium
                )
            }

            // Cuentas guardadas: al tocar una, rellena el identificador y exige contraseña
            if (savedSessions.isNotEmpty() && !isRegisterMode) {
                HorizontalDivider(modifier = Modifier.padding(vertical = 4.dp))
                Text(
                    text = if (isEnglish) {
                        "Quick fill saved account (password required):"
                    } else {
                        "Seleccionar cuenta guardada (requiere contraseña):"
                    },
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) TextGrayDark else TextGrayLight
                )
                Column(verticalArrangement = Arrangement.spacedBy(6.dp)) {
                    savedSessions.take(4).forEach { saved ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .clip(RoundedCornerShape(10.dp))
                                .background(if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9))
                                .clickable {
                                    name = saved.displayName
                                    email = saved.email
                                    selectedAvatarUrl = saved.avatarUrl
                                    password = ""
                                    errorMessage = if (isEnglish) {
                                        "Enter password for ${saved.displayName} ${saved.formattedTag}"
                                    } else {
                                        "Ingresa la contraseña para ${saved.displayName} ${saved.formattedTag}"
                                    }
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Column {
                                Text(
                                    text = saved.displayName,
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) TextLight else TextDark
                                )
                                Text(
                                    text = saved.email,
                                    fontSize = 11.sp,
                                    color = if (isDark) TextGrayDark else TextGrayLight
                                )
                            }
                            Text(
                                text = saved.formattedTag,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp,
                                color = PrimaryTeal
                            )
                        }
                    }
                }
            }

            // Botón principal de Registro o Inicio de Sesión Autenticado
            Button(
                enabled = !isSubmitting,
                onClick = {
                    if (isRegisterMode && allowRegistration) {
                        if (name.trim().length < 2) {
                            errorMessage = if (isEnglish) "Enter at least 2 characters for your username." else "Escribe tu nombre de usuario (mín. 2 caracteres)."
                            return@Button
                        }
                        if (email.isBlank() || !isEmailValid) {
                            errorMessage = if (isEnglish) "Please enter a valid email address." else "Ingresa un correo electrónico válido."
                            return@Button
                        }
                        if (password.length < 4) {
                            errorMessage = if (isEnglish) "Password must have at least 4 characters." else "Crea una contraseña de al menos 4 caracteres."
                            return@Button
                        }
                        isSubmitting = true
                        val localAvatar = selectedAvatarUrl
                        coroutineScope.launch {
                            val cloudAvatar = if (!localAvatar.isNullOrBlank()) {
                                ImageOptimizer.uploadImageOrEncodeForCloud(context, localAvatar, isAvatar = true) ?: localAvatar
                            } else null
                            val res = syncManager.registerAccount(
                                name = name.trim(),
                                email = email.trim(),
                                password = password,
                                avatarUrl = cloudAvatar,
                                completedHabits = completedHabits,
                                currentStreak = currentStreak
                            )
                            isSubmitting = false
                            res.onSuccess { session ->
                                errorMessage = null
                                onAuthSuccess(session)
                            }.onFailure { err ->
                                errorMessage = err.message ?: if (isEnglish) "Could not register account." else "No se pudo crear la cuenta."
                            }
                        }
                    } else {
                        val identifier = email.ifBlank { name }.trim()
                        if (identifier.isEmpty()) {
                            errorMessage = if (isEnglish) "Enter your username, email or #Tag." else "Ingresa tu usuario, correo o #Tag."
                            return@Button
                        }
                        if (password.length < 4) {
                            errorMessage = if (isEnglish) "Enter your password (min. 4 chars)." else "Ingresa tu contraseña (mín. 4 caracteres)."
                            return@Button
                        }
                        isSubmitting = true
                        coroutineScope.launch {
                            val res = syncManager.authenticateAccount(
                                identifier = identifier,
                                password = password,
                                completedHabits = completedHabits,
                                currentStreak = currentStreak
                            )
                            isSubmitting = false
                            res.onSuccess { session ->
                                errorMessage = null
                                onAuthSuccess(session)
                            }.onFailure { err ->
                                errorMessage = err.message ?: if (isEnglish) "Invalid credentials." else "Credenciales incorrectas."
                            }
                        }
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                shape = RoundedCornerShape(14.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isRegisterMode && allowRegistration) {
                        if (isEnglish) "Create Account & Sync #$cleanTagPreview" else "Crear Cuenta y Sincronizar #$cleanTagPreview"
                    } else {
                        if (isEnglish) "Sign In with Password" else "Iniciar Sesión / Cambiar Usuario"
                    },
                    fontWeight = FontWeight.Bold,
                    fontSize = 14.sp,
                    color = Color.White
                )
            }

            if (onDismiss != null) {
                TextButton(
                    onClick = onDismiss,
                    modifier = Modifier.fillMaxWidth()
                ) {
                    Text(
                        text = if (isEnglish) "Close window" else "Cerrar ventana",
                        color = if (isDark) TextGrayDark else TextGrayLight
                    )
                }
            }
        }
    }

    if (showOnboardingGuide) {
        AccountOnboardingGuideDialog(
            isEnglish = isEnglish,
            onCreateMyAccount = {
                showOnboardingGuide = false
                if (allowRegistration) {
                    isRegisterMode = true
                    name = ""
                    email = ""
                    password = ""
                }
            },
            onDismiss = { showOnboardingGuide = false }
        )
    }
}

/**
 * Modal de Cambio de Cuenta para desplegarse sobre cualquier pantalla de HabitOcto (requiere contraseña).
 */
@Composable
fun AuthModalDialog(
    onDismiss: () -> Unit,
    onAuthSuccess: (AuthSession) -> Unit,
    initialName: String = "",
    initialEmail: String = "",
    initialTag: String = "",
    initialAvatarUrl: String? = null,
    completedHabits: Int = 0,
    currentStreak: Int = 0,
    isEnglish: Boolean = false,
    allowRegistration: Boolean = false
) {
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 24.dp),
            shape = RoundedCornerShape(28.dp),
            color = Color.Transparent
        ) {
            AuthScreen(
                onAuthSuccess = { session ->
                    onAuthSuccess(session)
                    onDismiss()
                },
                onDismiss = onDismiss,
                initialName = initialName,
                initialEmail = initialEmail,
                initialTag = initialTag,
                initialAvatarUrl = initialAvatarUrl,
                completedHabits = completedHabits,
                currentStreak = currentStreak,
                isEnglish = isEnglish,
                allowRegistration = allowRegistration
            )
        }
    }
}

/**
 * Diálogo interactivo de incorporación (Onboarding) para nuevos usuarios y compañeros de clase.
 *
 * - **¿Para qué sirve?** Explica de manera didáctica y visual cómo funciona HabitOcto cuando se comparte el APK o se clona el repositorio de GitHub.
 * - **¿Cómo funciona?** Detalla los 4 pasos clave: 1) Cuenta única con código #XXX, 2) Amigos y Ranking, 3) Feed social con fotos optimizadas y 4) Modo Enfoque inmersivo.
 * - **¿Qué pasa si lo quitamos?** Los compañeros que descarguen el proyecto no entenderían cómo ingresar con su propio perfil ni cómo agregar amigos.
 */
@Composable
fun AccountOnboardingGuideDialog(
    isEnglish: Boolean = false,
    onCreateMyAccount: () -> Unit,
    onDismiss: () -> Unit
) {
    val isDark = isAppInDarkTheme

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 18.dp, vertical = 24.dp),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF121824) else Color.White,
            tonalElevation = 8.dp
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .verticalScroll(rememberScrollState())
                    .padding(22.dp),
                verticalArrangement = Arrangement.spacedBy(14.dp)
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    Box(
                        modifier = Modifier
                            .size(44.dp)
                            .background(PrimaryTeal.copy(alpha = 0.15f), CircleShape),
                        contentAlignment = Alignment.Center
                    ) {
                        Text("🐙", fontSize = 22.sp)
                    }
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = if (isEnglish) {
                                "How Accounts Work When Sharing the App or Repo"
                            } else {
                                "¿Cómo funcionan las cuentas al compartir la App o el Repo?"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 17.sp,
                            color = if (isDark) TextLight else TextDark
                        )
                        Text(
                            text = "Firebase Cloud (habitocto-suchini-2026) + Room Local",
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "1. Installing the APK or cloning the repository"
                            } else {
                                "1. Cuando un amigo instala el APK o clona el repositorio"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryTeal
                        )
                        Text(
                            text = if (isEnglish) {
                                "On first install, the initial Sign In / Create Account gate asks for Name, Password, optional Photo and assigns a unique system #XXX code. Inside Profile you can only use 'Switch User' with username & password."
                            } else {
                                "Al instalar por primera vez o descargar el repo, aparece el inicio de sesión para crear cuenta con Nombre, Contraseña, Foto en la nube y #XXX único otorgado por el sistema. Dentro de Perfil solo se permite 'Cambiar de Usuario' con usuario y contraseña."
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = if (isDark) TextLight else TextDark
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "2. Real-time sync in Firebase Cloud Firestore"
                            } else {
                                "2. Sincronización en Firebase Cloud (habitocto-suchini-2026)"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryTeal
                        )
                        Text(
                            text = if (isEnglish) {
                                "Each user's #XXX code, score, streak, and pet are synced in real time via Firebase Cloud Firestore (habitocto-suchini-2026). Friends just enter your 3-digit #Code in the Pet Ranking tab to compete!"
                            } else {
                                "El código #XXX, puntaje, racha y mascota de cada amigo se sincronizan en Firebase Cloud Firestore (habitocto-suchini-2026). ¡Así pueden agregarse por #Código en el Ranking desde distintos teléfonos!"
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = if (isDark) TextLight else TextDark
                        )
                    }
                }

                Card(
                    shape = RoundedCornerShape(16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFF1F5F9)
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(14.dp),
                        verticalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "3. Persistent session per device"
                            } else {
                                "3. Sesión persistente por dispositivo"
                            },
                            fontWeight = FontWeight.Bold,
                            fontSize = 13.sp,
                            color = PrimaryTeal
                        )
                        Text(
                            text = if (isEnglish) {
                                "Once your friend creates or logs into their account, it stays automatically saved on their phone/tablet (in Room & SharedPreferences) without logging out when closing the app."
                            } else {
                                "Una vez creada o iniciada su cuenta, queda guardada automáticamente en ese teléfono o tablet sin cerrarse al salir de la app."
                            },
                            fontSize = 12.sp,
                            lineHeight = 17.sp,
                            color = if (isDark) TextLight else TextDark
                        )
                    }
                }

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    OutlinedButton(
                        onClick = onDismiss,
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(if (isEnglish) "Got it" else "Entendido")
                    }

                    Button(
                        onClick = onCreateMyAccount,
                        modifier = Modifier.weight(1f),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "Switch User" else "Cambiar de Usuario",
                            color = Color.White,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }
        }
    }
}

/**
 * Tarjeta de identidad en la nube y código de amigo (#XXX) para la pestaña de Perfil.
 *
 * - **¿Para qué sirve?** Muestra de forma destacada el código personal de 3 dígitos del usuario, el estado de conexión con Firebase/REST
 *   y las acciones para copiar el código al portapapeles o compartirlo vía WhatsApp/Telegram.
 * - **¿Cómo funciona?** Observa los `StateFlow` de [CloudSyncManager], renderiza una tarjeta con gradiente teal y expone accesos directos
 *   al portapapeles de Android (`ClipboardManager`) y diálogos de incorporación.
 * - **¿Qué pasa si lo quitamos?** El usuario no conocería su propio código de amigo (#XXX) ni tendría forma de compartirlo con sus compañeros.
 */
@Composable
fun ProfileCloudTagCard(
    userName: String,
    userEmail: String?,
    completedHabits: Int,
    currentStreak: Int,
    onOpenAuthModal: () -> Unit,
    isEnglish: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val syncManager = remember { CloudSyncManager.getInstance(context) }
    val session by syncManager.currentSession.collectAsState()
    val syncState by syncManager.syncState.collectAsState()
    val lastSyncMessage by syncManager.lastSyncMessage.collectAsState()
    val isDark = isAppInDarkTheme

    val effectiveName = userName.ifBlank { session?.displayName ?: "Suchini" }
    val effectiveTag = session?.normalizedTag ?: CloudSyncManager.generateDeterministicTag(effectiveName, userEmail.orEmpty())
    var copiedFeedback by remember { mutableStateOf(false) }
    var showOnboardingGuide by rememberSaveable { mutableStateOf(false) }

    Card(
        modifier = modifier
            .fillMaxWidth()
            .padding(horizontal = 20.dp),
        shape = RoundedCornerShape(24.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF00332C) else Color(0xFFE0F2F1)
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.CloudDone,
                            contentDescription = null,
                            tint = if (isDark) SecondaryTeal else PrimaryTeal,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(6.dp))
                        Text(
                            text = if (isEnglish) "YOUR MULTI-DEVICE #TAG" else "TU TAG MULTI-DISPOSITIVO",
                            style = MaterialTheme.typography.labelSmall,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) SecondaryTeal else PrimaryTeal
                        )
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = "$effectiveName #$effectiveTag",
                        style = MaterialTheme.typography.titleLarge,
                        fontWeight = FontWeight.ExtraBold,
                        color = if (isDark) TextLight else DarkTealBackground
                    )
                    Text(
                        text = lastSyncMessage.ifBlank { syncState.label },
                        style = MaterialTheme.typography.bodySmall,
                        color = if (isDark) LightTealAccent else PrimaryTeal
                    )
                }

                // Insignia grande con el código de 3 dígitos #XXX
                Box(
                    modifier = Modifier
                        .clip(RoundedCornerShape(16.dp))
                        .background(
                            Brush.linearGradient(
                                listOf(PrimaryTeal, DarkTealBackground)
                            )
                        )
                        .border(1.5.dp, SecondaryTeal, RoundedCornerShape(16.dp))
                        .clickable {
                            CloudSyncManager.copyTagToClipboard(
                                context = context,
                                tagCode = effectiveTag,
                                userName = effectiveName,
                                completedHabits = completedHabits,
                                currentStreak = currentStreak
                            )
                            copiedFeedback = true
                        }
                        .padding(horizontal = 16.dp, vertical = 10.dp),
                    contentAlignment = Alignment.Center
                ) {
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "#$effectiveTag",
                            color = Color.White,
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 20.sp
                        )
                        Text(
                            text = if (copiedFeedback) {
                                if (isEnglish) "Copied!" else "¡Copiado!"
                            } else {
                                if (isEnglish) "Tap to copy" else "Toca p/ copiar"
                            },
                            color = SecondaryTeal,
                            fontSize = 9.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }
            }

            // Botones de acción rápida: Copiar #Código y Compartir con amigos
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                Button(
                    onClick = {
                        CloudSyncManager.copyTagToClipboard(
                            context = context,
                            tagCode = effectiveTag,
                            userName = effectiveName,
                            completedHabits = completedHabits,
                            currentStreak = currentStreak
                        )
                        copiedFeedback = true
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.ContentCopy,
                        contentDescription = if (isEnglish) "Copy my #Code" else "Copiar mi #Código",
                        modifier = Modifier.size(16.dp),
                        tint = Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Copy my #$effectiveTag" else "Copiar mi #$effectiveTag",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = Color.White
                    )
                }

                Button(
                    onClick = {
                        CloudSyncManager.shareProfileWithFriends(
                            context = context,
                            userName = effectiveName,
                            tagCode = effectiveTag,
                            completedHabits = completedHabits,
                            currentStreak = currentStreak,
                            petType = PetType.OCTO,
                            petAccessory = PetAccessory.NONE
                        )
                    },
                    modifier = Modifier.weight(1f),
                    colors = ButtonDefaults.buttonColors(
                        containerColor = if (isDark) SecondaryTeal else DarkTealBackground
                    ),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 10.dp, vertical = 10.dp)
                ) {
                    Icon(
                        imageVector = Icons.Default.Share,
                        contentDescription = if (isEnglish) "Share with friends" else "Compartir con amigos",
                        modifier = Modifier.size(16.dp),
                        tint = if (isDark) DeepDarkTeal else Color.White
                    )
                    Spacer(modifier = Modifier.width(6.dp))
                    Text(
                        text = if (isEnglish) "Share with friends" else "Compartir con amigos",
                        fontSize = 12.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isDark) DeepDarkTeal else Color.White
                    )
                }
            }

            // Sección informativa clara sobre cómo cada amigo tiene su propia cuenta al instalar el APK o clonar el repo
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        color = if (isDark) Color(0xFF00241F) else Color.White.copy(alpha = 0.72f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .border(
                        width = 1.dp,
                        color = SecondaryTeal.copy(alpha = 0.45f),
                        shape = RoundedCornerShape(14.dp)
                    )
                    .clickable { showOnboardingGuide = true }
                    .padding(12.dp)
            ) {
                Column(verticalArrangement = Arrangement.spacedBy(4.dp)) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween,
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "👥 Sharing the APK or cloning the repo?"
                            } else {
                                "👥 ¿Le pasaste la app a un amigo o clonaron el repo?"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = if (isDark) SecondaryTeal else PrimaryTeal
                        )
                        Text(
                            text = if (isEnglish) "See guide →" else "Ver guía →",
                            fontSize = 10.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) LightTealAccent else PrimaryTeal
                        )
                    }
                    Text(
                        text = if (isEnglish) {
                            "New users register from the initial Login screen with password and an automatic unique #Tag. Use 'Switch User' below to sign into another account with credentials."
                        } else {
                            "Cada cuenta nueva se crea en la pantalla de inicio con contraseña y #Tag único del sistema. Usa 'Cambiar de Usuario' abajo ingresando usuario y contraseña."
                        },
                        fontSize = 11.sp,
                        lineHeight = 15.sp,
                        color = if (isDark) TextLight.copy(alpha = 0.9f) else DarkTealBackground
                    )
                }
            }

            // Botón exclusivo para Cambiar de Usuario (exige usuario y contraseña)
            OutlinedButton(
                onClick = onOpenAuthModal,
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(12.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SwapHoriz,
                    contentDescription = if (isEnglish) "Switch user" else "Cambiar de usuario",
                    tint = if (isDark) SecondaryTeal else PrimaryTeal,
                    modifier = Modifier.size(18.dp)
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnglish) "Switch User" else "Cambiar de Usuario",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) SecondaryTeal else PrimaryTeal
                )
            }
        }
    }

    if (showOnboardingGuide) {
        AccountOnboardingGuideDialog(
            isEnglish = isEnglish,
            onCreateMyAccount = {
                showOnboardingGuide = false
                onOpenAuthModal()
            },
            onDismiss = { showOnboardingGuide = false }
        )
    }
}
