package com.example.ui

import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectDragGestures
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.navigationBarsPadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Close
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.SportsEsports
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableLongStateOf
import androidx.compose.runtime.mutableStateListOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.layout.onSizeChanged
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.model.PetType
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextLight
import com.example.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.delay
import kotlin.math.abs
import kotlin.random.Random

private enum class FallingSnackKind(
    val label: String,
    val happinessDelta: Int,
    val isBad: Boolean,
    val color: Color,
    val accent: Color
) {
    APPLE("Manzana", 3, false, Color(0xFFEF4444), Color(0xFF22C55E)),
    BROCCOLI("Brócoli", 4, false, Color(0xFF16A34A), Color(0xFF86EFAC)),
    WATER("Agua", 3, false, Color(0xFF0EA5E9), Color(0xFFBAE6FD)),
    STAR("Estrella", 5, false, Color(0xFFF59E0B), Color(0xFFFEF08A)),
    JUNK("Chatarra", 0, true, Color(0xFF92400E), Color(0xFFFBBF24)),
    PHONE("Distracción", 0, true, Color(0xFF475569), Color(0xFFEF4444))
}

private data class FallingGameItem(
    val id: Long,
    val xFraction: Float,
    var yFraction: Float,
    val speedPerTick: Float,
    val kind: FallingSnackKind
)

/**
 * Minijuego interactivo estilo Pou ("Atrapa Snacks Saludables") en Canvas Compose.
 *
 * Regla estricta de dominio:
 * - Solo aumenta la FELICIDAD de la mascota (0 XP).
 * - La experiencia (XP) únicamente se obtiene al completar hábitos y micro-hábitos.
 */
@Composable
fun PetMiniGameModal(
    petName: String,
    petType: PetType = PetType.OCTO,
    isEnglish: Boolean = false,
    onDismiss: () -> Unit,
    onGameFinished: (happinessEarned: Int) -> Unit
) {
    val isDark = isAppInDarkTheme
    var scoreHappiness by remember { mutableIntStateOf(0) }
    var caughtCount by remember { mutableIntStateOf(0) }
    var lives by remember { mutableIntStateOf(3) }
    var secondsLeft by remember { mutableIntStateOf(30) }
    var isGameOver by remember { mutableStateOf(false) }
    var rewardCommitted by remember { mutableStateOf(false) }
    var feedbackBanner by remember {
        mutableStateOf(
            if (isEnglish) "Slide or tap to catch healthy snacks! (Happiness only, 0 XP)"
            else "¡Desliza o toca para atrapar snacks sanos! (Solo felicidad, 0 XP)"
        )
    }

    var catcherXFraction by remember { mutableFloatStateOf(0.5f) }
    var boardWidthPx by remember { mutableFloatStateOf(1f) }
    var nextItemId by remember { mutableLongStateOf(1L) }
    val fallingItems = remember { mutableStateListOf<FallingGameItem>() }

    fun resetGame() {
        scoreHappiness = 0
        caughtCount = 0
        lives = 3
        secondsLeft = 30
        isGameOver = false
        rewardCommitted = false
        catcherXFraction = 0.5f
        fallingItems.clear()
        feedbackBanner = if (isEnglish) "Go! Catch fruit, veggies, water & stars."
        else "¡Vamos! Atrapa fruta, verdura, agua y estrellas."
    }

    // Reloj de cuenta regresiva (30 segundos)
    LaunchedEffect(isGameOver) {
        if (isGameOver) return@LaunchedEffect
        while (secondsLeft > 0 && !isGameOver) {
            delay(1000L)
            secondsLeft -= 1
            if (secondsLeft <= 0) {
                isGameOver = true
            }
        }
    }

    // Bucle físico del Canvas (30 FPS)
    LaunchedEffect(isGameOver) {
        if (isGameOver) return@LaunchedEffect
        var tickCount = 0
        while (!isGameOver) {
            delay(33L)
            tickCount += 1

            // Generar nuevo objeto cada ~600 ms
            if (tickCount % 18 == 1 && fallingItems.size < 7) {
                val roll = Random.nextInt(100)
                val kind = when {
                    roll < 24 -> FallingSnackKind.APPLE
                    roll < 46 -> FallingSnackKind.BROCCOLI
                    roll < 66 -> FallingSnackKind.WATER
                    roll < 78 -> FallingSnackKind.STAR
                    roll < 90 -> FallingSnackKind.JUNK
                    else -> FallingSnackKind.PHONE
                }
                fallingItems.add(
                    FallingGameItem(
                        id = nextItemId++,
                        xFraction = 0.1f + Random.nextFloat() * 0.8f,
                        yFraction = -0.05f,
                        speedPerTick = 0.014f + Random.nextFloat() * 0.011f,
                        kind = kind
                    )
                )
            }

            // Actualizar caída y detectar colisiones con la mascota
            val toRemove = mutableListOf<FallingGameItem>()
            for (i in fallingItems.indices) {
                val item = fallingItems[i]
                val newY = item.yFraction + item.speedPerTick
                fallingItems[i] = item.copy(yFraction = newY)

                val inCatchZoneY = newY in 0.78f..0.93f
                val inCatchZoneX = abs(item.xFraction - catcherXFraction) <= 0.12f

                if (inCatchZoneY && inCatchZoneX) {
                    toRemove.add(fallingItems[i])
                    if (item.kind.isBad) {
                        lives = (lives - 1).coerceAtLeast(0)
                        scoreHappiness = (scoreHappiness - 2).coerceAtLeast(0)
                        feedbackBanner = if (isEnglish) "Oops! Avoided a distraction (-1 ❤️)"
                        else "¡Ups! Evita comida chatarra y distracciones (-1 ❤️)"
                        if (lives <= 0) {
                            isGameOver = true
                        }
                    } else {
                        caughtCount += 1
                        scoreHappiness = (scoreHappiness + item.kind.happinessDelta).coerceAtMost(35)
                        feedbackBanner = if (isEnglish) "+${item.kind.happinessDelta}% happiness (${item.kind.label})!"
                        else "¡+${item.kind.happinessDelta}% felicidad (${item.kind.label})!"
                    }
                } else if (newY > 1.05f) {
                    toRemove.add(fallingItems[i])
                }
            }
            if (toRemove.isNotEmpty()) {
                fallingItems.removeAll(toRemove)
            }
        }
    }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 20.dp)
                .navigationBarsPadding(),
            shape = RoundedCornerShape(26.dp),
            color = if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
        ) {
            Column(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(16.dp),
                verticalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Cabecera del minijuego
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    verticalAlignment = Alignment.CenterVertically,
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.SportsEsports,
                            contentDescription = null,
                            tint = PrimaryTeal,
                            modifier = Modifier.size(22.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Column {
                            Text(
                                text = if (isEnglish) "Pou Snack Catch • $petName" else "Atrapa Snacks • $petName",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 16.sp,
                                color = if (isDark) TextLight else TextDark
                            )
                            Text(
                                text = if (isEnglish) "Boosts Pet Happiness only • 0 XP"
                                else "Solo aumenta Felicidad • 0 XP",
                                fontSize = 11.sp,
                                color = SecondaryTeal,
                                fontWeight = FontWeight.SemiBold
                            )
                        }
                    }
                    IconButton(onClick = onDismiss) {
                        Icon(
                            imageVector = Icons.Default.Close,
                            contentDescription = "Cerrar",
                            tint = if (isDark) Color(0xFF94A3B8) else Color(0xFF475569)
                        )
                    }
                }

                // HUD: Vidas, Tiempo, Felicidad ganada
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(8.dp)
                ) {
                    MiniGameHudChip(
                        label = if (isEnglish) "Lives" else "Vidas",
                        value = "❤️".repeat(lives).ifEmpty { "💔" },
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )
                    MiniGameHudChip(
                        label = if (isEnglish) "Time" else "Tiempo",
                        value = "${secondsLeft}s",
                        modifier = Modifier.weight(1f),
                        isDark = isDark
                    )
                    MiniGameHudChip(
                        label = if (isEnglish) "Happiness" else "Felicidad",
                        value = "+$scoreHappiness%",
                        modifier = Modifier.weight(1f),
                        isDark = isDark,
                        highlight = true
                    )
                }

                // Área jugable tipo Pou
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(320.dp)
                        .clip(RoundedCornerShape(20.dp))
                        .background(
                            Brush.verticalGradient(
                                if (isDark) listOf(Color(0xFF0C4A6E), Color(0xFF134E4A))
                                else listOf(Color(0xFFE0F2FE), Color(0xFFCCFBF1))
                            )
                        )
                        .onSizeChanged { size ->
                            boardWidthPx = size.width.toFloat().coerceAtLeast(1f)
                        }
                        .pointerInput(isGameOver) {
                            detectTapGestures { tapOffset ->
                                if (!isGameOver) {
                                    catcherXFraction = (tapOffset.x / boardWidthPx).coerceIn(0.1f, 0.9f)
                                }
                            }
                        }
                        .pointerInput(isGameOver) {
                            detectDragGestures { change, dragAmount ->
                                change.consume()
                                if (!isGameOver) {
                                    catcherXFraction = (catcherXFraction + dragAmount.x / boardWidthPx)
                                        .coerceIn(0.1f, 0.9f)
                                }
                            }
                        }
                ) {
                    Canvas(modifier = Modifier.fillMaxSize()) {
                        // Suelo decorativo
                        drawRect(
                            color = Color(0xFF0F766E).copy(alpha = 0.25f),
                            topLeft = Offset(0f, size.height * 0.88f),
                            size = Size(size.width, size.height * 0.12f)
                        )

                        // Objetos que caen
                        for (item in fallingItems) {
                            val cx = item.xFraction * size.width
                            val cy = item.yFraction * size.height
                            drawFallingSnack(item.kind, Offset(cx, cy), radius = 22f)
                        }

                        // Personaje receptor en la parte inferior
                        val catcherX = catcherXFraction * size.width
                        val catcherY = size.height * 0.84f
                        drawMiniCatcherPet(
                            center = Offset(catcherX, catcherY),
                            petType = petType
                        )
                    }

                    if (isGameOver) {
                        Box(
                            modifier = Modifier
                                .fillMaxSize()
                                .background(Color.Black.copy(alpha = 0.62f)),
                            contentAlignment = Alignment.Center
                        ) {
                            Card(
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .padding(horizontal = 24.dp),
                                shape = RoundedCornerShape(20.dp),
                                colors = CardDefaults.cardColors(
                                    containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                                )
                            ) {
                                Column(
                                    modifier = Modifier.padding(18.dp),
                                    horizontalAlignment = Alignment.CenterHorizontally,
                                    verticalArrangement = Arrangement.spacedBy(8.dp)
                                ) {
                                    Text(
                                        text = if (isEnglish) "Round Finished!" else "¡Partida Terminada!",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 18.sp,
                                        color = if (isDark) TextLight else TextDark
                                    )
                                    Text(
                                        text = if (isEnglish) {
                                            "Caught $caughtCount snacks • +$scoreHappiness% Happiness (0 XP)"
                                        } else {
                                            "Atrapaste $caughtCount snacks • +$scoreHappiness% Felicidad (0 XP)"
                                        },
                                        fontSize = 13.sp,
                                        fontWeight = FontWeight.SemiBold,
                                        color = SecondaryTeal,
                                        textAlign = TextAlign.Center
                                    )
                                    Row(
                                        modifier = Modifier.fillMaxWidth(),
                                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                                    ) {
                                        TextButton(
                                            onClick = { resetGame() },
                                            modifier = Modifier.weight(1f)
                                        ) {
                                            Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (isEnglish) "Replay" else "Jugar otra vez")
                                        }
                                        Button(
                                            onClick = {
                                                if (!rewardCommitted) {
                                                    rewardCommitted = true
                                                    onGameFinished(scoreHappiness)
                                                }
                                                onDismiss()
                                            },
                                            modifier = Modifier.weight(1f),
                                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                                        ) {
                                            Icon(Icons.Default.Favorite, contentDescription = null, modifier = Modifier.size(16.dp))
                                            Spacer(Modifier.width(4.dp))
                                            Text(if (isEnglish) "Claim" else "Guardar")
                                        }
                                    }
                                }
                            }
                        }
                    }
                }

                // Banner informativo + Leyenda rápida
                Text(
                    text = feedbackBanner,
                    fontSize = 12.sp,
                    fontWeight = FontWeight.SemiBold,
                    color = if (isDark) Color(0xFFCBD5E1) else Color(0xFF334155),
                    textAlign = TextAlign.Center,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }
    }
}

@Composable
private fun MiniGameHudChip(
    label: String,
    value: String,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    highlight: Boolean = false
) {
    Surface(
        modifier = modifier,
        shape = RoundedCornerShape(12.dp),
        color = if (highlight) PrimaryTeal.copy(alpha = 0.16f)
        else if (isDark) Color(0xFF1E293B) else Color(0xFFE2E8F0)
    ) {
        Column(
            modifier = Modifier.padding(vertical = 6.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = label,
                fontSize = 10.sp,
                color = if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
            )
            Text(
                text = value,
                fontSize = 13.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (highlight) PrimaryTeal else if (isDark) TextLight else TextDark
            )
        }
    }
}

private fun DrawScope.drawFallingSnack(kind: FallingSnackKind, center: Offset, radius: Float) {
    when (kind) {
        FallingSnackKind.STAR -> {
            val starPath = Path().apply {
                moveTo(center.x, center.y - radius)
                lineTo(center.x + radius * 0.35f, center.y - radius * 0.3f)
                lineTo(center.x + radius, center.y - radius * 0.2f)
                lineTo(center.x + radius * 0.45f, center.y + radius * 0.28f)
                lineTo(center.x + radius * 0.65f, center.y + radius)
                lineTo(center.x, center.y + radius * 0.55f)
                lineTo(center.x - radius * 0.65f, center.y + radius)
                lineTo(center.x - radius * 0.45f, center.y + radius * 0.28f)
                lineTo(center.x - radius, center.y - radius * 0.2f)
                lineTo(center.x - radius * 0.35f, center.y - radius * 0.3f)
                close()
            }
            drawPath(starPath, color = kind.color)
            drawCircle(color = kind.accent, radius = radius * 0.28f, center = center)
        }
        FallingSnackKind.PHONE -> {
            drawRoundRect(
                color = kind.color,
                topLeft = Offset(center.x - radius * 0.65f, center.y - radius * 0.9f),
                size = Size(radius * 1.3f, radius * 1.8f),
                cornerRadius = CornerRadius(6f, 6f)
            )
            drawCircle(
                color = kind.accent,
                radius = radius * 0.25f,
                center = center
            )
        }
        else -> {
            drawCircle(color = kind.color, radius = radius, center = center)
            drawCircle(
                color = kind.accent,
                radius = radius * 0.45f,
                center = Offset(center.x - radius * 0.22f, center.y - radius * 0.28f)
            )
        }
    }
}

private fun DrawScope.drawMiniCatcherPet(center: Offset, petType: PetType) {
    val (bodyColor, glowColor) = when (petType) {
        PetType.OCTO -> Color(0xFF00BFA5) to Color(0xFF80CBC4)
        PetType.DRAGON -> Color(0xFFEF4444) to Color(0xFFFCA5A5)
        PetType.FOX -> Color(0xFFF97316) to Color(0xFFFED7AA)
        PetType.PANDA -> Color(0xFF334155) to Color(0xFFCBD5E1)
        PetType.AXOLOTL -> Color(0xFFEC4899) to Color(0xFFFBCFE8)
    }
    drawCircle(color = glowColor.copy(alpha = 0.28f), radius = 50f, center = center)
    drawRoundRect(
        color = bodyColor,
        topLeft = Offset(center.x - 42f, center.y - 34f),
        size = Size(84f, 68f),
        cornerRadius = CornerRadius(32f, 32f)
    )
    // Ojitos amigables estilo Pou
    drawCircle(Color.White, radius = 9f, center = Offset(center.x - 16f, center.y - 6f))
    drawCircle(Color.White, radius = 9f, center = Offset(center.x + 16f, center.y - 6f))
    drawCircle(Color(0xFF0F172A), radius = 4.5f, center = Offset(center.x - 15f, center.y - 6f))
    drawCircle(Color(0xFF0F172A), radius = 4.5f, center = Offset(center.x + 15f, center.y - 6f))
    drawArc(
        color = Color(0xFF0F172A),
        startAngle = 20f,
        sweepAngle = 140f,
        useCenter = false,
        topLeft = Offset(center.x - 12f, center.y + 4f),
        size = Size(24f, 14f)
    )
}
