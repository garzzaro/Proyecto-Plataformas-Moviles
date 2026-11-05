package com.example.ui

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import android.content.Intent
import android.os.Build
import android.os.PowerManager
import android.view.WindowManager
import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import android.Manifest
import android.content.pm.PackageManager
import androidx.core.content.ContextCompat
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.CameraAlt
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.PhotoLibrary
import androidx.compose.material.icons.filled.Security
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material.icons.filled.Verified
import androidx.compose.material.icons.filled.Warning
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.LifecycleEventObserver
import com.example.model.HabitItem
import com.example.model.PhotoVerificationAnalyzer
import com.example.model.PhotoVerificationResult
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGrayLight
import com.example.ui.theme.TextLight
import com.example.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.delay

/**
 * Helper seguro para obtener la [Activity] anfitriona desde cualquier [Context] de Compose.
 */
internal tailrec fun findActivity(context: Context): Activity? = when (context) {
    is Activity -> context
    is ContextWrapper -> findActivity(context.baseContext)
    else -> null
}

/**
 * Tarjeta del Modo Enfoque integrada directamente en el TrackerScreen.
 *
 * Explica claramente qué es el Modo Enfoque y permite iniciar una sesión inmersiva
 * a pantalla completa con bloqueo de salida hasta que concluya el temporizador
 * o el usuario verifique el cumplimiento con una fotografía analizada en memoria RAM.
 */
@Composable
fun FocusModeTrackerCard(
    habits: List<HabitItem>,
    isDark: Boolean = isAppInDarkTheme,
    isEnglish: Boolean = false,
    onFocusCompleted: (HabitItem?, Int) -> Unit = { _, _ -> },
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val pendingHabits = remember(habits) { habits.filter { !it.isCompleted } }
    val selectableHabits = if (pendingHabits.isNotEmpty()) pendingHabits else habits

    var selectedHabitId by rememberSaveable {
        mutableStateOf(selectableHabits.firstOrNull()?.id ?: "")
    }
    val selectedHabit = remember(selectableHabits, selectedHabitId) {
        selectableHabits.firstOrNull { it.id == selectedHabitId } ?: selectableHabits.firstOrNull()
    }

    var selectedMinutes by rememberSaveable { mutableIntStateOf(15) }
    var isFocusLockActive by rememberSaveable { mutableStateOf(false) }
    var customFocusTopic by rememberSaveable { mutableStateOf("") }

    val durationOptions = remember(isEnglish) {
        listOf(
            1 to if (isEnglish) "1 min Demo" else "1 min Demo",
            5 to "5 min",
            15 to "15 min",
            25 to "25 min",
            45 to "45 min"
        )
    }

    Card(
        modifier = modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(
            modifier = Modifier.padding(18.dp),
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            // Encabezado con icono y título explicativo
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(
                    verticalAlignment = Alignment.CenterVertically,
                    modifier = Modifier.weight(1f)
                ) {
                    Box(
                        modifier = Modifier
                            .size(42.dp)
                            .background(
                                Brush.linearGradient(
                                    listOf(PrimaryTeal, Color(0xFF004D40))
                                ),
                                CircleShape
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Lock,
                            contentDescription = null,
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                    Spacer(modifier = Modifier.width(10.dp))
                    Column {
                        Text(
                            text = if (isEnglish) {
                                "Immersive Focus Mode (Lock Mode)"
                            } else {
                                "Modo Enfoque Inmersivo (Con Bloqueo)"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isDark) TextLight else TextDark
                        )
                        Text(
                            text = if (isEnglish) {
                                "What is Focus Mode? Distraction-free habit lock"
                            } else {
                                "¿Qué es el Modo Enfoque? Bloqueo productivo sin distracciones"
                            },
                            fontSize = 11.sp,
                            fontWeight = FontWeight.SemiBold,
                            color = PrimaryTeal
                        )
                    }
                }

                Icon(
                    imageVector = Icons.Default.Timer,
                    contentDescription = null,
                    tint = Color(0xFFFF9800),
                    modifier = Modifier.size(26.dp)
                )
            }

            // Explicación clara solicitada por el usuario
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color(0xFF0F172A) else Color(0xFFF0FDF4),
                        RoundedCornerShape(14.dp)
                    )
                    .border(
                        1.dp,
                        PrimaryTeal.copy(alpha = 0.25f),
                        RoundedCornerShape(14.dp)
                    )
                    .padding(12.dp)
            ) {
                Text(
                    text = if (isEnglish) {
                        "Productive concentration lock to fulfill your habits. Starting it activates a full-screen timer that blocks distractions and exiting the app until the time finishes or you verify your habit with a photo."
                    } else {
                        "Bloqueo productivo de concentración para cumplir tus hábitos. Al iniciarlo se activa un temporizador a pantalla completa que bloquea distracciones y salidas de la app hasta que termine el tiempo o verifiques tu hábito con una foto."
                    },
                    fontSize = 12.sp,
                    lineHeight = 17.sp,
                    color = if (isDark) Color(0xFFCBD5E1) else TextDark
                )
            }

            // 1. Campo para escribir libremente el tema de enfoque o elegir un hábito
            Text(
                text = if (isEnglish) "1. What will you focus on? (Type or select):" else "1. ¿En qué te vas a enfocar? (Escribe o elige):",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextLight else TextDark
            )

            OutlinedTextField(
                value = customFocusTopic,
                onValueChange = { customFocusTopic = it },
                placeholder = {
                    Text(
                        text = selectedHabit?.title?.let { if (isEnglish) "e.g., $it" else "ej., $it" }
                            ?: (if (isEnglish) "e.g., Study Math, Code app..." else "ej., Estudiar Matemáticas, Programar..."),
                        color = Color.Gray.copy(alpha = 0.7f),
                        fontSize = 13.sp
                    )
                },
                modifier = Modifier.fillMaxWidth(),
                singleLine = true,
                shape = RoundedCornerShape(12.dp),
                colors = OutlinedTextFieldDefaults.colors(
                    focusedBorderColor = PrimaryTeal,
                    unfocusedBorderColor = Color.Gray.copy(alpha = 0.35f),
                    focusedTextColor = if (isDark) TextLight else TextDark,
                    unfocusedTextColor = if (isDark) TextLight else TextDark
                )
            )

            // Selector rápido con hábitos existentes
            if (selectableHabits.isNotEmpty()) {
                LazyRow(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(horizontal = 2.dp)
                ) {
                    items(selectableHabits, key = { it.id }) { habit ->
                        val isSelected = selectedHabit?.id == habit.id || customFocusTopic.equals(habit.title, ignoreCase = true)
                        Box(
                            modifier = Modifier
                                .clip(RoundedCornerShape(12.dp))
                                .background(
                                    if (isSelected) PrimaryTeal
                                    else if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                                )
                                .border(
                                    width = if (isSelected) 1.5.dp else 1.dp,
                                    color = if (isSelected) SecondaryTeal else Color.Gray.copy(alpha = 0.25f),
                                    shape = RoundedCornerShape(12.dp)
                                )
                                .clickable {
                                    selectedHabitId = habit.id
                                    customFocusTopic = habit.title
                                }
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Text(
                                    text = if (habit.isMicroHabit) "⚡ ${habit.title}" else "🎯 ${habit.title}",
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else (if (isDark) TextLight else TextDark),
                                    maxLines = 1,
                                    overflow = TextOverflow.Ellipsis
                                )
                            }
                        }
                    }
                }
            }

            // Selector de duración
            Text(
                text = if (isEnglish) "2. Lock duration:" else "2. Duración del bloqueo:",
                fontSize = 12.sp,
                fontWeight = FontWeight.Bold,
                color = if (isDark) TextLight else TextDark
            )

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(6.dp)
            ) {
                durationOptions.forEach { (mins, label) ->
                    val active = selectedMinutes == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) PrimaryTeal
                                else if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                            )
                            .clickable { selectedMinutes = mins }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else (if (isDark) TextLight else TextDark),
                            textAlign = TextAlign.Center
                        )
                    }
                }
            }

            // Botón principal para iniciar el bloqueo inmersivo
            Button(
                onClick = { isFocusLockActive = true },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(50.dp),
                shape = RoundedCornerShape(14.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = PrimaryTeal,
                    contentColor = Color.White
                )
            ) {
                Text(
                    text = if (isEnglish) {
                        "🔒 Start Immersive Focus Mode"
                    } else {
                        "🔒 Iniciar Modo Enfoque Inmersivo"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 14.sp
                )
            }
        }
    }

    if (isFocusLockActive) {
        val topic = customFocusTopic.trim().ifEmpty {
            selectedHabit?.title ?: if (isEnglish) "Deep Focus" else "Enfoque Profundo"
        }
        FocusLockScreenOverlay(
            habit = selectedHabit,
            focusTitle = topic,
            durationMinutes = selectedMinutes,
            isEnglish = isEnglish,
            onUnlockedAndCompleted = { completedHabit, mins ->
                isFocusLockActive = false
                onFocusCompleted(completedHabit, mins)
                val msg = if (isEnglish) {
                    "Focus Mode unlocked! Habit verified (+25 XP)"
                } else {
                    "¡Modo Enfoque completado y desbloqueado! (+25 XP)"
                }
                Toast.makeText(context, msg, Toast.LENGTH_LONG).show()
            }
        )
    }
}

/**
 * Pantalla completa de Bloqueo de Modo Enfoque (`FocusLockScreenOverlay`).
 *
 * - Bloquea el botón Atrás (`BackHandler(enabled = true)`).
 * - Mantiene la pantalla activa y permite mostrar la app directamente al encender el dispositivo
 *   tras apagar la pantalla (`FLAG_KEEP_SCREEN_ON`, `setShowWhenLocked(true)`, `setTurnScreenOn(true)`).
 * - Activa Screen Pinning (`startLockTask()`) cuando el sistema lo permite y re-focaliza la tarea si
 *   el usuario intenta cambiar de app mientras la pantalla sigue interactiva.
 * - Solo se desbloquea cuando el contador llega a `00:00` o mediante una prueba fotográfica
 *   analizada en memoria RAM con [PhotoVerificationAnalyzer] (0 bytes guardados en BD).
 */
@Composable
fun FocusLockScreenOverlay(
    habit: HabitItem?,
    focusTitle: String = habit?.title ?: "Enfoque",
    durationMinutes: Int,
    isEnglish: Boolean = false,
    onUnlockedAndCompleted: (HabitItem?, Int) -> Unit
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current

    val totalSeconds = remember(durationMinutes) { maxOf(60, durationMinutes * 60) }
    var remainingSeconds by rememberSaveable(durationMinutes) { mutableIntStateOf(totalSeconds) }
    var verificationResult by remember { mutableStateOf<PhotoVerificationResult?>(null) }
    var showPhotoUnlockOptions by rememberSaveable { mutableStateOf(false) }

    // Bloqueo estricto del botón Back del sistema
    BackHandler(enabled = true) {
        val warn = if (isEnglish) {
            "🔒 Focus Mode is active. Finish the timer or verify with a photo to unlock."
        } else {
            "🔒 Modo Enfoque activo. Termina el tiempo o sube una foto de prueba para desbloquear."
        }
        Toast.makeText(context, warn, Toast.LENGTH_SHORT).show()
    }

    // Configuración de ventana y persistencia al apagar/encender pantalla (sin bloquear el SO ni congelar la tablet)
    DisposableEffect(lifecycleOwner, context) {
        val activity = findActivity(context)
        val window = activity?.window

        window?.addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
            activity?.setShowWhenLocked(true)
            activity?.setTurnScreenOn(true)
        } else {
            @Suppress("DEPRECATION")
            window?.addFlags(
                WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                    WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
            )
        }

        onDispose {
            window?.clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON)
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O_MR1) {
                activity?.setShowWhenLocked(false)
                activity?.setTurnScreenOn(false)
            } else {
                @Suppress("DEPRECATION")
                window?.clearFlags(
                    WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED or
                        WindowManager.LayoutParams.FLAG_TURN_SCREEN_ON
                )
            }
        }
    }

    // Cuenta regresiva del bloqueo
    LaunchedEffect(remainingSeconds) {
        if (remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            if (remainingSeconds == 0) {
                onUnlockedAndCompleted(habit, durationMinutes)
            }
        }
    }

    // Launchers de Cámara y Galería para desbloqueo por prueba fotográfica en RAM con solicitud segura de permisos
    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            val result = PhotoVerificationAnalyzer.analyzeBitmapAndDiscard(
                bitmap = bitmap,
                habitTitle = focusTitle
            )
            verificationResult = result
            if (result.isValid) {
                onUnlockedAndCompleted(habit, durationMinutes)
            }
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            runCatching { cameraLauncher.launch(null) }
                .onFailure {
                    Toast.makeText(context, if (isEnglish) "Could not open camera" else "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
                }
        } else {
            val msg = if (isEnglish) "Camera permission is required to take photo proof" else "Se requiere permiso de cámara para tomar la foto de prueba"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val launchCameraSafe: () -> Unit = {
        val hasCamPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCamPermission) {
            runCatching { cameraLauncher.launch(null) }
                .onFailure {
                    Toast.makeText(context, if (isEnglish) "Could not open camera" else "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
                }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            val result = PhotoVerificationAnalyzer.analyzeUriAndDiscard(
                context = context,
                uri = uri,
                habitTitle = focusTitle
            )
            verificationResult = result
            if (result.isValid) {
                onUnlockedAndCompleted(habit, durationMinutes)
            }
        }
    }

    val progressFraction by animateFloatAsState(
        targetValue = (remainingSeconds.toFloat() / totalSeconds.toFloat()).coerceIn(0f, 1f),
        animationSpec = tween(500),
        label = "focus_ring_progress"
    )

    val infiniteTransition = rememberInfiniteTransition(label = "focus_pulse")
    val pulseGlow by infiniteTransition.animateFloat(
        initialValue = 0.25f,
        targetValue = 0.65f,
        animationSpec = infiniteRepeatable(
            animation = tween(1800, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pulse_glow"
    )

    val mm = (remainingSeconds / 60).toString().padStart(2, '0')
    val ss = (remainingSeconds % 60).toString().padStart(2, '0')

    Dialog(
        onDismissRequest = { /* Bloqueado intencionalmente */ },
        properties = DialogProperties(
            usePlatformDefaultWidth = false,
            dismissOnBackPress = false,
            dismissOnClickOutside = false
        )
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = Color(0xFF090D16)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .verticalScroll(rememberScrollState())
                    .padding(horizontal = 24.dp, vertical = 28.dp),
                horizontalAlignment = Alignment.CenterHorizontally,
                verticalArrangement = Arrangement.SpaceBetween
            ) {
                // Parte superior: Estado de bloqueo
                Column(horizontalAlignment = Alignment.CenterHorizontally) {
                    Surface(
                        shape = RoundedCornerShape(50),
                        color = Color(0xFFEF4444).copy(alpha = 0.18f),
                        modifier = Modifier.border(
                            1.dp,
                            Color(0xFFEF4444).copy(alpha = 0.5f),
                            RoundedCornerShape(50)
                        )
                    ) {
                        Row(
                            modifier = Modifier.padding(horizontal = 16.dp, vertical = 8.dp),
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Text(
                                text = if (isEnglish) {
                                    "🔒 FOCUS MODE ACTIVE • Exit locked"
                                } else {
                                    "🔒 MODO ENFOQUE ACTIVO • Salida bloqueada"
                                },
                                color = Color(0xFFFCA5A5),
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 12.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    Text(
                        text = focusTitle,
                        color = Color.White,
                        fontSize = 22.sp,
                        fontWeight = FontWeight.ExtraBold,
                        textAlign = TextAlign.Center
                    )

                    Spacer(modifier = Modifier.height(6.dp))

                    Text(
                        text = if (isEnglish) {
                            "You can turn off the screen with the power button; when turned back on, you will remain right here until the timer finishes or you verify with a photo."
                        } else {
                            "Puedes apagar la pantalla con el botón físico; al encenderla seguirás aquí sin distracciones hasta que termine el tiempo o verifiques con una foto."
                        },
                        color = Color(0xFF94A3B8),
                        fontSize = 12.sp,
                        textAlign = TextAlign.Center,
                        lineHeight = 17.sp
                    )
                }

                // Parte central: Anillo de progreso animado y reloj gigante MM:SS
                Box(
                    contentAlignment = Alignment.Center,
                    modifier = Modifier
                        .size(250.dp)
                        .padding(vertical = 12.dp)
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        val strokeWidth = 14.dp.toPx()
                        val diameter = size.minDimension - strokeWidth
                        val topLeft = Offset(
                            (size.width - diameter) / 2f,
                            (size.height - diameter) / 2f
                        )

                        // Halo pulsante
                        drawCircle(
                            color = SecondaryTeal.copy(alpha = pulseGlow * 0.25f),
                            radius = diameter / 2f + 12f
                        )

                        // Pista base
                        drawArc(
                            color = Color(0xFF1E293B),
                            startAngle = 0f,
                            sweepAngle = 360f,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )

                        // Arco de progreso activo
                        drawArc(
                            brush = Brush.sweepGradient(
                                listOf(SecondaryTeal, PrimaryTeal, Color(0xFF38BDF8), SecondaryTeal)
                            ),
                            startAngle = -90f,
                            sweepAngle = 360f * progressFraction,
                            useCenter = false,
                            topLeft = topLeft,
                            size = Size(diameter, diameter),
                            style = Stroke(width = strokeWidth, cap = StrokeCap.Round)
                        )
                    }

                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = "$mm:$ss",
                            color = Color.White,
                            fontSize = 54.sp,
                            fontWeight = FontWeight.ExtraBold
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Text(
                            text = if (isEnglish) "REMAINING TIME" else "TIEMPO RESTANTE",
                            color = SecondaryTeal,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Parte inferior: Desbloqueo anticipado mediante Prueba (Foto de Verificación en RAM)
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalAlignment = Alignment.CenterHorizontally,
                    verticalArrangement = Arrangement.spacedBy(10.dp)
                ) {
                    verificationResult?.let { result ->
                        if (!result.isValid) {
                            Card(
                                modifier = Modifier.fillMaxWidth(),
                                shape = RoundedCornerShape(14.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = Color(0xFF7F1D1D).copy(alpha = 0.55f)
                                )
                            ) {
                                Text(
                                    text = if (isEnglish) result.summaryEn else result.summaryEs,
                                    color = Color(0xFFFECACA),
                                    fontSize = 12.sp,
                                    modifier = Modifier.padding(12.dp),
                                    textAlign = TextAlign.Center
                                )
                            }
                        }
                    }

                    Button(
                        onClick = { showPhotoUnlockOptions = !showPhotoUnlockOptions },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(52.dp),
                        shape = RoundedCornerShape(16.dp),
                        colors = ButtonDefaults.buttonColors(
                            containerColor = SecondaryTeal,
                            contentColor = Color(0xFF00251A)
                        )
                    ) {
                        Text(
                            text = if (isEnglish) {
                                "📸 Unlock Now with Proof (Verification Photo)"
                            } else {
                                "📸 Desbloquear Ahora con Prueba (Foto de Verificación)"
                            },
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 13.sp,
                            textAlign = TextAlign.Center
                        )
                    }

                    AnimatedVisibility(visible = showPhotoUnlockOptions) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(16.dp),
                            colors = CardDefaults.cardColors(containerColor = Color(0xFF1E293B))
                        ) {
                            Column(
                                modifier = Modifier.padding(14.dp),
                                verticalArrangement = Arrangement.spacedBy(10.dp)
                            ) {
                                Text(
                                    text = if (isEnglish) {
                                        "Take or select a photo proving you completed the habit. Analyzed in RAM and discarded immediately (0 KB stored in DB)."
                                    } else {
                                        "Toma o elige una foto de prueba de tu hábito. Se analizará en memoria RAM y se descartará al instante (0 KB guardados en la base de datos)."
                                    },
                                    color = Color(0xFFCBD5E1),
                                    fontSize = 11.sp,
                                    lineHeight = 16.sp
                                )

                                Row(
                                    modifier = Modifier.fillMaxWidth(),
                                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Button(
                                        onClick = { launchCameraSafe() },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp),
                                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.CameraAlt,
                                            contentDescription = null,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Camera" else "Cámara",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold
                                        )
                                    }

                                    OutlinedButton(
                                        onClick = { runCatching { galleryLauncher.launch("image/*") } },
                                        modifier = Modifier.weight(1f),
                                        shape = RoundedCornerShape(12.dp)
                                    ) {
                                        Icon(
                                            imageVector = Icons.Default.PhotoLibrary,
                                            contentDescription = null,
                                            tint = SecondaryTeal,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = if (isEnglish) "Gallery" else "Galería",
                                            fontSize = 12.sp,
                                            fontWeight = FontWeight.Bold,
                                            color = SecondaryTeal
                                        )
                                    }
                                }
                            }
                        }
                    }

                    Text(
                        text = if (isEnglish) {
                            "🛡️ Zero-Storage RAM Verification • Screen stays locked to HabitOcto"
                        } else {
                            "🛡️ Verificación en RAM (0 Bytes en BD) • Pantalla fijada en HabitOcto"
                        },
                        color = Color(0xFF64748B),
                        fontSize = 11.sp,
                        textAlign = TextAlign.Center
                    )
                }
            }
        }
    }
}

/**
 * Diálogo modal para verificar un Micro-Hábito mediante una foto analizada en memoria RAM
 * y descartada inmediatamente sin almacenar ni un solo byte en la base de datos.
 */
@Composable
fun MicroHabitPhotoVerificationDialog(
    habit: HabitItem,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onVerifiedComplete: (HabitItem) -> Unit
) {
    val context = LocalContext.current
    var verificationResult by remember { mutableStateOf<PhotoVerificationResult?>(null) }

    val cameraLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.TakePicturePreview()
    ) { bitmap ->
        if (bitmap != null) {
            verificationResult = PhotoVerificationAnalyzer.analyzeBitmapAndDiscard(
                bitmap = bitmap,
                habitTitle = habit.title
            )
        }
    }

    val cameraPermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) {
            runCatching { cameraLauncher.launch(null) }
                .onFailure {
                    Toast.makeText(context, if (isEnglish) "Could not open camera" else "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
                }
        } else {
            val msg = if (isEnglish) "Camera permission is required to take photo proof" else "Se requiere permiso de cámara para tomar la foto de prueba"
            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
        }
    }

    val launchCameraSafe: () -> Unit = {
        val hasCamPermission = ContextCompat.checkSelfPermission(
            context,
            Manifest.permission.CAMERA
        ) == PackageManager.PERMISSION_GRANTED

        if (hasCamPermission) {
            runCatching { cameraLauncher.launch(null) }
                .onFailure {
                    Toast.makeText(context, if (isEnglish) "Could not open camera" else "No se pudo abrir la cámara", Toast.LENGTH_SHORT).show()
                }
        } else {
            cameraPermissionLauncher.launch(Manifest.permission.CAMERA)
        }
    }

    val galleryLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.GetContent()
    ) { uri ->
        if (uri != null) {
            verificationResult = PhotoVerificationAnalyzer.analyzeUriAndDiscard(
                context = context,
                uri = uri,
                habitTitle = habit.title
            )
        }
    }

    AlertDialog(
        onDismissRequest = onDismiss,
        title = {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Icon(
                    imageVector = Icons.Default.Verified,
                    contentDescription = null,
                    tint = PrimaryTeal
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text(
                    text = if (isEnglish) {
                        "Verify Habit with Photo Proof"
                    } else {
                        "Verificar Hábito con Foto de Prueba"
                    },
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 17.sp
                )
            }
        },
        text = {
            Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                Text(
                    text = if (isEnglish) {
                        "To verify «${habit.title}», take or select a proof photo. The vision engine will analyze authenticity (lighting, sharpness, and color variety) in RAM and discard it immediately (0 KB saved in the database)."
                    } else {
                        "Para verificar «${habit.title}», toma o selecciona una foto de prueba. El motor de visión analizará la autenticidad (iluminación, nitidez y variedad cromática) en memoria RAM y la descartará inmediatamente (0 KB guardados en la base de datos)."
                    },
                    fontSize = 13.sp,
                    lineHeight = 18.sp
                )

                // Botones de captura
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { launchCameraSafe() },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                    ) {
                        Text(
                            text = if (isEnglish) "📷 Take Photo with Camera" else "📷 Tomar Foto con Cámara",
                            fontWeight = FontWeight.Bold,
                            color = Color.White
                        )
                    }

                    OutlinedButton(
                        onClick = { runCatching { galleryLauncher.launch("image/*") } },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Text(
                            text = if (isEnglish) "🖼️ Choose Proof Photo" else "🖼️ Elegir Foto de Prueba",
                            fontWeight = FontWeight.Bold,
                            color = PrimaryTeal
                        )
                    }
                }

                // Tarjeta de reporte tras analizar la foto en RAM
                verificationResult?.let { result ->
                    val statusColor = if (result.isValid) Color(0xFF16A34A) else Color(0xFFDC2626)
                    val bgReport = if (result.isValid) {
                        Color(0xFF16A34A).copy(alpha = 0.12f)
                    } else {
                        Color(0xFFDC2626).copy(alpha = 0.12f)
                    }

                    Card(
                        modifier = Modifier
                            .fillMaxWidth()
                            .border(1.5.dp, statusColor.copy(alpha = 0.5f), RoundedCornerShape(14.dp)),
                        shape = RoundedCornerShape(14.dp),
                        colors = CardDefaults.cardColors(containerColor = bgReport)
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            verticalArrangement = Arrangement.spacedBy(6.dp)
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = if (result.isValid) Icons.Default.CheckCircle else Icons.Default.Warning,
                                    contentDescription = null,
                                    tint = statusColor,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = if (result.isValid) {
                                        if (isEnglish) "✓ Authentic photo verified" else "✓ Foto auténtica verificada"
                                    } else {
                                        if (isEnglish) "⚠️ Photo rejected (try another)" else "⚠️ Foto rechazada (intenta otra)"
                                    },
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp,
                                    color = statusColor
                                )
                            }

                            Text(
                                text = if (isEnglish) {
                                    "Confidence: ${result.confidencePercent}% · Light: ${result.brightnessScore}% · Sharpness: ${result.sharpnessScore}%"
                                } else {
                                    "Confianza: ${result.confidencePercent}% · Luz: ${result.brightnessScore}% · Nitidez: ${result.sharpnessScore}%"
                                },
                                fontSize = 12.sp,
                                fontWeight = FontWeight.SemiBold
                            )

                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Security,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(15.dp)
                                )
                                Spacer(modifier = Modifier.width(4.dp))
                                Text(
                                    text = if (isEnglish) {
                                        "🛡️ 0 Bytes saved in Database (Analyzed in RAM & discarded)"
                                    } else {
                                        "🛡️ 0 Bytes guardados en Base de Datos (Analizada en RAM y descartada)"
                                    },
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal
                                )
                            }
                        }
                    }
                }
            }
        },
        confirmButton = {
            Button(
                onClick = { onVerifiedComplete(habit) },
                enabled = verificationResult?.isValid == true,
                colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
            ) {
                Text(
                    text = if (isEnglish) {
                        "✅ Confirm Micro-Habit Completed"
                    } else {
                        "✅ Confirmar Micro-Hábito Completado"
                    },
                    color = Color.White,
                    fontWeight = FontWeight.Bold
                )
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text(if (isEnglish) "Cancel" else "Cancelar")
            }
        }
    )
}
