package com.example.ui

import android.widget.Toast
import androidx.activity.compose.BackHandler
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
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
import androidx.compose.foundation.layout.heightIn
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentHeight
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Restaurant
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
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
import androidx.compose.ui.draw.scale
import androidx.compose.ui.draw.shadow
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.drawscope.DrawScope
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.AchievementBadge
import com.example.model.AchievementCatalog
import com.example.model.PetAccessory
import com.example.model.PetCustomization
import com.example.model.PetHabitat
import com.example.model.PetType
import com.example.ui.components.HabitImage
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGrayLight
import com.example.ui.theme.TextLight
import com.example.ui.theme.isAppInDarkTheme
import kotlinx.coroutines.delay
import kotlin.math.PI
import kotlin.math.sin

/**
 * Pantalla dedicada e inmersiva a pantalla completa para "Visitar Mascota" (El Espacio Propio / Santuario).
 *
 * Cumple el requerimiento del usuario:
 * - Abre un espacio aparte con su propio fondo único animado (Arrecife Coralino, Jardín Zen,
 *   Habitación Gamer, Estación Cósmica).
 * - Permite personalizar libremente el nombre, el animal (Octo, Ignis, Kitsune, Bambú, Axolote)
 *   y los accesorios visuales reales superpuestos en una única pieza central (Gafas de Sol, Bufanda,
 *   Audífonos, Sombrero de Mago, Corona).
 * - Incluye botones tipo cristal oscuro con iconos vectoriales nítidos (Alimentar, Acariciar, Entrenar).
 * - Muestra de forma permanente y clara cómo funcionan la Felicidad, la Energía y la Experiencia (XP).
 * - El Modo Enfoque con bloqueo ahora vive directamente en el TrackerScreen.
 */
@Composable
fun PetSanctuaryScreen(
    currentStreak: Int,
    totalCompleted: Int,
    completedToday: Int = 0,
    scheduledToday: Int = 1,
    friendsCount: Int = 4,
    initialCustomization: PetCustomization = PetCustomization(),
    onCustomizationChanged: (PetCustomization) -> Unit = {},
    onBack: () -> Unit
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme

    BackHandler(onBack = onBack)

    var customization by remember(initialCustomization) { mutableStateOf(initialCustomization) }
    var activeCustomTab by rememberSaveable { mutableIntStateOf(0) } // 0: Animal, 1: Accesorio, 2: Hábitat
    var showRenameDialog by rememberSaveable { mutableStateOf(false) }
    var renameDraft by rememberSaveable(customization.customName) { mutableStateOf(customization.customName) }

    // Bonificadores interactivos por alimentar, acariciar o entrenar en el Santuario
    var bonusHappiness by rememberSaveable { mutableIntStateOf(0) }
    var bonusEnergy by rememberSaveable { mutableIntStateOf(0) }
    var bonusXp by rememberSaveable { mutableIntStateOf(0) }
    var focusSessionsDone by rememberSaveable { mutableIntStateOf(0) }

    // Rebote animado de la mascota al interactuar
    var interactionMessage by remember { mutableStateOf<String?>(null) }
    var petBounceTrigger by remember { mutableStateOf(false) }

    LaunchedEffect(petBounceTrigger) {
        if (petBounceTrigger) {
            delay(450)
            petBounceTrigger = false
        }
    }

    // Cálculo de Nivel y XP
    val totalXpEarned = (totalCompleted * 10) + bonusXp
    val petLevel = maxOf(1, (totalXpEarned / 100) + 1)
    val xpInCurrentLevel = totalXpEarned % 100

    // Cálculo de Felicidad (50% base + 10% por día de racha, 40% si racha es 0) + bonus de cuidado
    val baseHappiness = if (currentStreak > 0) minOf(100, 50 + currentStreak * 10) else 40
    val happinessPercent = minOf(100, baseHappiness + bonusHappiness)

    // Cálculo de Energía (progreso del día + racha + bonus de entrenamiento)
    val safeScheduled = maxOf(1, scheduledToday)
    val dailyRatio = ((completedToday.coerceAtMost(safeScheduled) * 40) / safeScheduled)
    val baseEnergy = if (currentStreak > 0 || completedToday > 0) {
        minOf(100, 50 + dailyRatio + (currentStreak * 8))
    } else {
        40
    }
    val energyPercent = minOf(100, baseEnergy + bonusEnergy)

    val petDisplayName = customization.customName.ifBlank { customization.petType.displayName }

    val moodTitle = when {
        happinessPercent >= 95 && energyPercent >= 95 -> "¡$petDisplayName está en Estado Radiante! 👑"
        currentStreak >= 3 -> "¡$petDisplayName rebosa de alegría! 🔥"
        currentStreak >= 1 || bonusHappiness > 0 -> "¡$petDisplayName está feliz contigo! ✨"
        else -> "$petDisplayName espera tu primer hábito de hoy 🌱"
    }

    val moodSubtitle = interactionMessage ?: when {
        happinessPercent >= 95 -> "¡Felicidad al máximo! Tus puntos en el Ranking tienen bonificación activa."
        currentStreak >= 1 -> "Llevas $currentStreak días de racha. ¡Sigue alimentando su energía diaria!"
        else -> "Acércate a acariciarlo, aliméntalo o completa un microhábito para subir su ánimo."
    }

    // Animaciones continuas del santuario
    val infiniteTransition = rememberInfiniteTransition(label = "sanctuary_infinite")
    val floatOffset by infiniteTransition.animateFloat(
        initialValue = -10f,
        targetValue = 10f,
        animationSpec = infiniteRepeatable(
            animation = tween(2200, easing = FastOutSlowInEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "pet_float"
    )
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(6000, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "habitat_particles"
    )
    val bounceScale by animateFloatAsState(
        targetValue = if (petBounceTrigger) 1.14f else 1.0f,
        animationSpec = tween(280),
        label = "pet_bounce"
    )

    val achievements = remember(totalCompleted, currentStreak, friendsCount, focusSessionsDone, petLevel) {
        AchievementCatalog.evaluate(
            totalCompleted = totalCompleted,
            currentStreak = currentStreak,
            friendsCount = friendsCount,
            focusSessionsCompleted = focusSessionsDone,
            petLevel = petLevel
        )
    }

    Surface(
        modifier = Modifier.fillMaxSize(),
        color = if (isDark) Color(0xFF0B1320) else Color(0xFFF2F7F6)
    ) {
        LazyColumn(
            modifier = Modifier.fillMaxSize(),
            contentPadding = PaddingValues(bottom = 32.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            // =========================================================================
            // 1. ESCENARIO INMERSIVO DEL SANTUARIO CON FONDO ÚNICO ANIMADO Y MASCOTA
            // =========================================================================
            item {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .wrapContentHeight()
                        .heightIn(min = 520.dp)
                        .clip(RoundedCornerShape(bottomStart = 32.dp, bottomEnd = 32.dp))
                ) {
                    // Fondo único ilustrado y animado según el hábitat elegido
                    HabitatAnimatedBackground(
                        habitat = customization.habitat,
                        animProgress = particleProgress,
                        modifier = Modifier.matchParentSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(start = 20.dp, end = 20.dp, top = 16.dp, bottom = 24.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Barra superior del Santuario (Botón Regresar + Título Hábitat + Cambiar Nombre)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Surface(
                                onClick = onBack,
                                shape = CircleShape,
                                color = Color.Black.copy(alpha = 0.42f),
                                contentColor = Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 14.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Icon(
                                        imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                                        contentDescription = "Volver",
                                        modifier = Modifier.size(18.dp)
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Text(
                                        text = "Salir del Santuario",
                                        fontSize = 12.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                }
                            }

                            Surface(
                                onClick = {
                                    renameDraft = customization.customName
                                    showRenameDialog = true
                                },
                                shape = RoundedCornerShape(20.dp),
                                color = Color.Black.copy(alpha = 0.42f),
                                contentColor = Color.White
                            ) {
                                Row(
                                    modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                                    verticalAlignment = Alignment.CenterVertically
                                ) {
                                    Text(
                                        text = "${customization.habitat.emoji} ${customization.habitat.displayName}",
                                        fontSize = 11.sp,
                                        fontWeight = FontWeight.Bold
                                    )
                                    Spacer(modifier = Modifier.width(6.dp))
                                    Icon(
                                        imageVector = Icons.Default.Edit,
                                        contentDescription = "Renombrar mascota",
                                        modifier = Modifier.size(14.dp),
                                        tint = SecondaryTeal
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(10.dp))

                        // Píldora de Nivel y Nombre de la Mascota
                        Surface(
                            onClick = {
                                renameDraft = customization.customName
                                showRenameDialog = true
                            },
                            shape = RoundedCornerShape(50),
                            color = Color.White.copy(alpha = 0.16f)
                        ) {
                            Row(
                                modifier = Modifier.padding(horizontal = 16.dp, vertical = 6.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(
                                    text = "NIVEL $petLevel  •  ${petDisplayName.uppercase()}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 13.sp
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Icon(
                                    imageVector = Icons.Default.Edit,
                                    contentDescription = "Cambiar nombre",
                                    tint = Color.White.copy(alpha = 0.85f),
                                    modifier = Modifier.size(14.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // PERSONAJE CENTRAL CON MODELO HD + ACCESORIO ÚNICO CENTRADO
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(215.dp)
                                .offset(y = floatOffset.dp)
                                .scale(bounceScale)
                                .clickable {
                                    petBounceTrigger = true
                                    bonusHappiness = minOf(50, bonusHappiness + 5)
                                    interactionMessage = "¡A $petDisplayName le encantaron tus caricias! (+5% Felicidad)"
                                }
                        ) {
                            PetCharacterCanvasIllustration(
                                petType = customization.petType,
                                accessory = customization.accessory,
                                happinessPercent = happinessPercent,
                                energyPercent = energyPercent,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = moodTitle,
                            fontSize = 18.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = moodSubtitle,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.9f),
                            textAlign = TextAlign.Center,
                            maxLines = 2,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.padding(horizontal = 12.dp)
                        )

                        Spacer(modifier = Modifier.height(18.dp))

                        // BOTONES DE INTERACCIÓN EN VIVO EN EL SANTUARIO (TIPO CRISTAL OSCURO CON ÍCONOS NÍTIDOS)
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(10.dp)
                        ) {
                            SanctuaryActionButton(
                                icon = Icons.Default.Restaurant,
                                iconTint = Color(0xFFFF5252),
                                label = "Alimentar",
                                rewardBadge = "+8% ❤️",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    petBounceTrigger = true
                                    bonusHappiness = minOf(50, bonusHappiness + 8)
                                    bonusXp += 5
                                    interactionMessage = "¡Ñam! $petDisplayName comió su snack favorito (+8% ❤️, +5 XP)"
                                    Toast.makeText(context, "¡Alimentaste a $petDisplayName!", Toast.LENGTH_SHORT).show()
                                }
                            )
                            SanctuaryActionButton(
                                icon = Icons.Default.Favorite,
                                iconTint = Color(0xFFFF4081),
                                label = "Acariciar",
                                rewardBadge = "+5% ❤️",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    petBounceTrigger = true
                                    bonusHappiness = minOf(50, bonusHappiness + 5)
                                    interactionMessage = "¡$petDisplayName está ronroneando de felicidad! (+5% ❤️)"
                                }
                            )
                            SanctuaryActionButton(
                                icon = Icons.Default.FlashOn,
                                iconTint = Color(0xFFFFCA28),
                                label = "Entrenar",
                                rewardBadge = "+10% ⚡",
                                modifier = Modifier.weight(1f),
                                onClick = {
                                    petBounceTrigger = true
                                    bonusEnergy = minOf(50, bonusEnergy + 10)
                                    bonusXp += 10
                                    interactionMessage = "¡Gran entrenamiento con $petDisplayName! (+10% ⚡, +10 XP)"
                                    Toast.makeText(context, "¡Energía de $petDisplayName recargada!", Toast.LENGTH_SHORT).show()
                                }
                            )
                        }
                    }
                }
            }

            // =========================================================================
            // 2. BARRAS EN VIVO DE FELICIDAD, ENERGÍA Y EXPERIENCIA (XP)
            // =========================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
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
                        Text(
                            text = "Estado Vital de $petDisplayName",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 16.sp,
                            color = if (isDark) TextLight else TextDark
                        )

                        // Barra de Felicidad
                        VitalStatProgressRow(
                            emoji = "❤️",
                            label = "Felicidad (Constancia y Racha)",
                            percent = happinessPercent,
                            barColor = Color(0xFFE91E63),
                            badgeText = if (happinessPercent >= 80) "Muy Feliz" else if (happinessPercent >= 50) "Estable" else "Necesita mimo",
                            isDark = isDark
                        )

                        // Barra de Energía
                        VitalStatProgressRow(
                            emoji = "⚡",
                            label = "Energía (Hábitos de hoy y Entreno)",
                            percent = energyPercent,
                            barColor = Color(0xFFFFB300),
                            badgeText = if (energyPercent >= 85) "Radiante" else if (energyPercent >= 55) "Activo" else "Con sueño",
                            isDark = isDark
                        )

                        // Barra de Nivel / XP
                        VitalStatProgressRow(
                            emoji = "⭐",
                            label = "Experiencia (Nivel $petLevel → Nivel ${petLevel + 1})",
                            percent = xpInCurrentLevel,
                            barColor = PrimaryTeal,
                            badgeText = "$xpInCurrentLevel / 100 XP",
                            isDark = isDark
                        )
                    }
                }
            }

            // =========================================================================
            // 3. ESTUDIO DE PERSONALIZACIÓN: ANIMAL, ACCESORIOS Y FONDO ÚNICO
            // =========================================================================
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp),
                    shape = RoundedCornerShape(22.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = if (isDark) Color(0xFF1E293B) else Color.White
                    ),
                    elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
                ) {
                    Column(modifier = Modifier.padding(18.dp)) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Palette,
                                    contentDescription = null,
                                    tint = PrimaryTeal
                                )
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Personalizar Mascota y Fondo Único",
                                        fontWeight = FontWeight.ExtraBold,
                                        fontSize = 16.sp,
                                        color = if (isDark) TextLight else TextDark
                                    )
                                    Text(
                                        text = "Elige su especie, vístelo con un accesorio central y cambia su hábitat",
                                        fontSize = 12.sp,
                                        color = if (isDark) Color(0xFF94A3B8) else TextGrayLight
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Selector de pestañas: Animal | Accesorios | Fondo Único
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .background(
                                    if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                    RoundedCornerShape(12.dp)
                                )
                                .padding(4.dp),
                            horizontalArrangement = Arrangement.SpaceEvenly
                        ) {
                            listOf("🐾 Animal", "👑 Accesorios", "🏞️ Fondo Único").forEachIndexed { idx, tabTitle ->
                                val selected = activeCustomTab == idx
                                Box(
                                    modifier = Modifier
                                        .weight(1f)
                                        .clip(RoundedCornerShape(9.dp))
                                        .background(if (selected) PrimaryTeal else Color.Transparent)
                                        .clickable { activeCustomTab = idx }
                                        .padding(vertical = 8.dp),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Text(
                                        text = tabTitle,
                                        fontSize = 12.sp,
                                        fontWeight = if (selected) FontWeight.Bold else FontWeight.Medium,
                                        color = if (selected) Color.White else (if (isDark) Color(0xFFCBD5E1) else Color(0xFF475569))
                                    )
                                }
                            }
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        when (activeCustomTab) {
                            0 -> {
                                // Selección de Animal
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(PetType.entries) { pet ->
                                        val isSelected = customization.petType == pet
                                        SanctuaryOptionCard(
                                            title = pet.displayName,
                                            emoji = pet.iconEmoji,
                                            subtitle = pet.description,
                                            badge = if (petLevel >= pet.requiredLevel) "Disponible" else "Vista previa (Nv.${pet.requiredLevel})",
                                            isSelected = isSelected,
                                            isDark = isDark,
                                            onClick = {
                                                val updated = customization.copy(
                                                    petType = pet,
                                                    petName = if (customization.petName == customization.petType.displayName || customization.petName == "Octo") {
                                                        pet.displayName.split(" ").firstOrNull() ?: pet.displayName
                                                    } else customization.petName
                                                )
                                                customization = updated
                                                onCustomizationChanged(updated)
                                                petBounceTrigger = true
                                                interactionMessage = "¡Ahora tu compañero es ${pet.displayName}!"
                                            }
                                        )
                                    }
                                }
                            }
                            1 -> {
                                // Selección de Accesorios visuales reales (1 sola pieza central)
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(PetAccessory.entries) { acc ->
                                        val isSelected = customization.accessory == acc
                                        SanctuaryOptionCard(
                                            title = acc.displayName,
                                            emoji = if (acc == PetAccessory.NONE) "✨" else acc.emoji,
                                            subtitle = if (acc == PetAccessory.NONE) "Aspecto natural" else "Pieza única centrada",
                                            badge = if (currentStreak >= acc.requiredStreak) "Desbloqueado" else "Equipar (${acc.requiredStreak}d racha)",
                                            isSelected = isSelected,
                                            isDark = isDark,
                                            onClick = {
                                                val updated = customization.copy(accessory = acc)
                                                customization = updated
                                                onCustomizationChanged(updated)
                                                petBounceTrigger = true
                                                interactionMessage = if (acc == PetAccessory.NONE) {
                                                    "$petDisplayName se quitó los accesorios."
                                                } else {
                                                    "¡$petDisplayName se puso ${acc.displayName}!"
                                                }
                                            }
                                        )
                                    }
                                }
                            }
                            2 -> {
                                // Selección de Fondo Único / Hábitat
                                LazyRow(
                                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                                    contentPadding = PaddingValues(horizontal = 2.dp)
                                ) {
                                    items(PetHabitat.entries) { hab ->
                                        val isSelected = customization.habitat == hab
                                        SanctuaryOptionCard(
                                            title = hab.displayName,
                                            emoji = hab.emoji,
                                            subtitle = when (hab) {
                                                PetHabitat.AQUARIUM -> "Burbujas y corales marinos"
                                                PetHabitat.ZEN_GARDEN -> "Cerezos, bambú y paz interior"
                                                PetHabitat.COZY_ROOM -> "Luces neón y setup gamer"
                                                PetHabitat.SPACE_STATION -> "Estrellas fugaces y cosmos"
                                            },
                                            badge = if (totalCompleted >= hab.requiredCompletedHabits) "Desbloqueado" else "Visitar (${hab.requiredCompletedHabits} háb.)",
                                            isSelected = isSelected,
                                            isDark = isDark,
                                            onClick = {
                                                val updated = customization.copy(habitat = hab)
                                                customization = updated
                                                onCustomizationChanged(updated)
                                                petBounceTrigger = true
                                                interactionMessage = "¡Bienvenido al ${hab.displayName}!"
                                            }
                                        )
                                    }
                                }
                            }
                        }
                    }
                }
            }

            // =========================================================================
            // 4. EXPLICACIÓN VISIBLE Y PERMANENTE DE FELICIDAD, ENERGÍA Y NIVEL
            // =========================================================================
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    PetStatsExplanationCard(
                        currentStreak = currentStreak,
                        happinessPercent = happinessPercent,
                        energyPercent = energyPercent,
                        petLevel = petLevel,
                        isDark = isDark
                    )
                }
            }

            // =========================================================================
            // 5. MEDALLAS Y LOGROS DESBLOQUEABLES
            // =========================================================================
            item {
                Box(modifier = Modifier.padding(horizontal = 16.dp)) {
                    AchievementsShowcaseCard(
                        achievements = achievements,
                        isDark = isDark
                    )
                }
            }
        }
    }

    if (showRenameDialog) {
        AlertDialog(
            onDismissRequest = { showRenameDialog = false },
            title = { Text("Nombrar a tu Mascota", fontWeight = FontWeight.Bold) },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
                    Text("Elige un nombre único para tu compañero de hábitos:")
                    OutlinedTextField(
                        value = renameDraft,
                        onValueChange = { renameDraft = it.take(22) },
                        label = { Text("Nombre de la mascota") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val clean = renameDraft.trim().ifBlank { customization.petType.displayName }
                        val updated = customization.copy(petName = clean)
                        customization = updated
                        onCustomizationChanged(updated)
                        showRenameDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Guardar", color = Color.White)
                }
            },
            dismissButton = {
                TextButton(onClick = { showRenameDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Fondo único ilustrado con Compose Canvas y partículas en movimiento según el hábitat.
 */
@Composable
fun HabitatAnimatedBackground(
    habitat: PetHabitat,
    animProgress: Float,
    modifier: Modifier = Modifier
) {
    val backgroundBrush = when (habitat) {
        PetHabitat.AQUARIUM -> Brush.verticalGradient(
            listOf(Color(0xFF00796B), Color(0xFF004D40), Color(0xFF00251A))
        )
        PetHabitat.ZEN_GARDEN -> Brush.verticalGradient(
            listOf(Color(0xFF1B5E20), Color(0xFF2E7D32), Color(0xFF0D3311))
        )
        PetHabitat.COZY_ROOM -> Brush.verticalGradient(
            listOf(Color(0xFF311B92), Color(0xFF1A103C), Color(0xFF0D0628))
        )
        PetHabitat.SPACE_STATION -> Brush.verticalGradient(
            listOf(Color(0xFF0B1026), Color(0xFF10254C), Color(0xFF050711))
        )
    }

    Canvas(modifier = modifier.background(backgroundBrush)) {
        val w = size.width
        val h = size.height

        when (habitat) {
            PetHabitat.AQUARIUM -> {
                // Rayos de sol submarinos
                for (i in 0..3) {
                    val xStart = w * (0.15f + i * 0.25f)
                    drawLine(
                        color = Color(0xFF80CBC4).copy(alpha = 0.14f),
                        start = Offset(xStart, 0f),
                        end = Offset(xStart - 60f, h * 0.85f),
                        strokeWidth = 48f
                    )
                }
                // Suelo de arrecife y corales suaves
                drawArc(
                    color = Color(0xFF00332C),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(-w * 0.1f, h * 0.84f),
                    size = Size(w * 1.2f, h * 0.32f)
                )
                // Burbujas submarinas subiendo
                val bubbleSeeds = listOf(0.12f, 0.27f, 0.44f, 0.63f, 0.78f, 0.89f)
                bubbleSeeds.forEachIndexed { idx, xFactor ->
                    val phase = (animProgress + idx * 0.17f) % 1f
                    val yPos = h * (0.92f - phase * 0.85f)
                    val xWobble = sin((phase * 2 * PI + idx).toFloat()) * 14f
                    val radius = (10f + (idx % 3) * 6f)
                    drawCircle(
                        color = Color(0xFFB2EBF2).copy(alpha = 0.35f),
                        radius = radius,
                        center = Offset(w * xFactor + xWobble, yPos)
                    )
                    drawCircle(
                        color = Color.White.copy(alpha = 0.55f),
                        radius = radius,
                        center = Offset(w * xFactor + xWobble, yPos),
                        style = Stroke(width = 2f)
                    )
                }
            }

            PetHabitat.ZEN_GARDEN -> {
                // Sol/Luna suave Zen al fondo
                drawCircle(
                    color = Color(0xFFFFF59D).copy(alpha = 0.2f),
                    radius = w * 0.22f,
                    center = Offset(w * 0.8f, h * 0.22f)
                )
                // Colinas de jardín japonés
                drawArc(
                    color = Color(0xFF143D17),
                    startAngle = 180f,
                    sweepAngle = 180f,
                    useCenter = true,
                    topLeft = Offset(-w * 0.15f, h * 0.82f),
                    size = Size(w * 1.3f, h * 0.35f)
                )
                // Tallos de bambú laterales
                for (b in listOf(0.06f, 0.93f)) {
                    drawLine(
                        color = Color(0xFF66BB6A).copy(alpha = 0.35f),
                        start = Offset(w * b, h * 0.15f),
                        end = Offset(w * b, h),
                        strokeWidth = 14f,
                        cap = StrokeCap.Round
                    )
                }
                // Pétalos de cerezo (Sakura) flotando
                val petalSeeds = listOf(0.15f, 0.32f, 0.5f, 0.68f, 0.82f, 0.9f)
                petalSeeds.forEachIndexed { idx, seed ->
                    val phase = (animProgress + idx * 0.16f) % 1f
                    val xPos = (w * ((seed + phase * 0.25f) % 1f))
                    val yPos = h * (0.08f + phase * 0.82f)
                    drawOval(
                        color = Color(0xFFF8BBD0).copy(alpha = 0.65f),
                        topLeft = Offset(xPos, yPos),
                        size = Size(16f, 10f)
                    )
                }
            }

            PetHabitat.COZY_ROOM -> {
                // Luces LED Neón Gamer en el techo/pared
                drawLine(
                    brush = Brush.horizontalGradient(
                        listOf(Color(0xFF00E5FF), Color(0xFFD500F9), Color(0xFF00E676))
                    ),
                    start = Offset(w * 0.08f, h * 0.16f),
                    end = Offset(w * 0.92f, h * 0.16f),
                    strokeWidth = 6f,
                    cap = StrokeCap.Round
                )
                // Piso con cuadrícula synthwave/gamer sutil
                val floorTop = h * 0.80f
                drawRect(
                    color = Color(0xFF12082B),
                    topLeft = Offset(0f, floorTop),
                    size = Size(w, h - floorTop)
                )
                for (i in 0..6) {
                    val x = (w / 6f) * i
                    drawLine(
                        color = Color(0xFFD500F9).copy(alpha = 0.28f),
                        start = Offset(w * 0.5f, floorTop),
                        end = Offset(x, h),
                        strokeWidth = 2f
                    )
                }
                // Partículas de píxeles neón flotando
                val neonColors = listOf(Color(0xFF00E5FF), Color(0xFFFF4081), Color(0xFF7C4DFF), Color(0xFF00E676))
                for (idx in 0..5) {
                    val phase = (animProgress + idx * 0.18f) % 1f
                    val xPos = w * (0.14f + idx * 0.14f)
                    val yPos = h * (0.85f - phase * 0.68f)
                    drawRoundRect(
                        color = neonColors[idx % neonColors.size].copy(alpha = 0.55f),
                        topLeft = Offset(xPos, yPos),
                        size = Size(12f, 12f),
                        cornerRadius = CornerRadius(3f, 3f)
                    )
                }
            }

            PetHabitat.SPACE_STATION -> {
                // Planeta con anillos al fondo
                val planetCenter = Offset(w * 0.82f, h * 0.25f)
                drawCircle(
                    color = Color(0xFF5C6BC0).copy(alpha = 0.45f),
                    radius = 38f,
                    center = planetCenter
                )
                drawOval(
                    color = Color(0xFF80DEEA).copy(alpha = 0.5f),
                    topLeft = Offset(planetCenter.x - 62f, planetCenter.y - 12f),
                    size = Size(124f, 24f),
                    style = Stroke(width = 3f)
                )
                // Estrellas fijas y parpadeantes
                val starCoords = listOf(
                    0.12f to 0.18f, 0.24f to 0.35f, 0.38f to 0.14f,
                    0.55f to 0.28f, 0.68f to 0.16f, 0.18f to 0.62f,
                    0.85f to 0.58f, 0.42f to 0.68f
                )
                starCoords.forEachIndexed { idx, (sx, sy) ->
                    val twinkle = 0.35f + 0.65f * kotlin.math.abs(sin((animProgress * 2 * PI + idx).toFloat()))
                    drawCircle(
                        color = Color.White.copy(alpha = twinkle),
                        radius = if (idx % 2 == 0) 4.5f else 3f,
                        center = Offset(w * sx, h * sy)
                    )
                }
                // Estrella fugaz animada
                val shootPhase = (animProgress * 1.5f) % 1f
                if (shootPhase < 0.65f) {
                    val sx = w * (0.1f + shootPhase * 0.9f)
                    val sy = h * (0.12f + shootPhase * 0.45f)
                    drawLine(
                        brush = Brush.linearGradient(
                            listOf(Color.White, Color.Transparent)
                        ),
                        start = Offset(sx, sy),
                        end = Offset(sx - 55f, sy - 28f),
                        strokeWidth = 3.5f,
                        cap = StrokeCap.Round
                    )
                }
            }
        }
    }
}

/**
 * Devuelve el modelo/imagen oficial de alta definición para cada especie de mascota.
 * - `OCTO`: recurso oficial local `R.drawable.ic_pulpo`.
 * - Otras especies: arte oficial Google Noto Emoji HD 512px con respaldo vectorial instantáneo de cuerpo completo.
 */
private fun getOfficialPetArtworkModel(petType: PetType): Any = when (petType) {
    PetType.OCTO -> R.drawable.ic_pulpo
    PetType.DRAGON -> "https://fonts.gstatic.com/s/e/notoemoji/latest/1f409/512.png"
    PetType.FOX -> "https://fonts.gstatic.com/s/e/notoemoji/latest/1f98a/512.png"
    PetType.PANDA -> "https://fonts.gstatic.com/s/e/notoemoji/latest/1f43c/512.png"
    PetType.AXOLOTL -> "https://fonts.gstatic.com/s/e/notoemoji/latest/1f98e/512.png"
}

/**
 * Dibuja el animal seleccionado (Octo, Ignis el Dragón, Kitsune el Zorro, Bambú el Panda o Axolote Místico)
 * combinando modelos/imágenes oficiales limpios con ilustraciones vectoriales detalladas de cuerpo completo
 * y dibuja el accesorio seleccionado **ÚNICAMENTE UNA VEZ** centrado sobre la mascota.
 */
@Composable
fun PetCharacterCanvasIllustration(
    petType: PetType,
    accessory: PetAccessory,
    happinessPercent: Int,
    energyPercent: Int,
    modifier: Modifier = Modifier
) {
    val auraColor = when {
        energyPercent >= 90 -> Color(0xFFFFD54F)
        happinessPercent >= 80 -> SecondaryTeal
        else -> Color.White
    }

    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        // Halo brillante detrás de la mascota
        Box(
            modifier = Modifier
                .size(175.dp)
                .shadow(24.dp, CircleShape, ambientColor = auraColor, spotColor = auraColor)
                .background(
                    Brush.radialGradient(
                        listOf(auraColor.copy(alpha = 0.38f), Color.Transparent)
                    ),
                    CircleShape
                )
        )

        if (petType == PetType.OCTO) {
            // Octo usa el recurso oficial ic_pulpo enmarcado con anillo energético
            HabitImage(
                model = getOfficialPetArtworkModel(petType),
                contentDescription = petType.displayName,
                modifier = Modifier
                    .size(148.dp)
                    .clip(CircleShape)
                    .border(3.5.dp, auraColor, CircleShape),
                contentScale = ContentScale.Crop
            )
        } else {
            // Base circular con ilustración vectorial de cuerpo completo + modelo HD limpio
            Box(
                modifier = Modifier
                    .size(152.dp)
                    .clip(CircleShape)
                    .background(
                        Brush.radialGradient(
                            listOf(
                                Color(0xFF1E293B).copy(alpha = 0.85f),
                                Color(0xFF0F172A).copy(alpha = 0.92f)
                            )
                        ),
                        CircleShape
                    )
                    .border(3.5.dp, auraColor, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Canvas(modifier = Modifier.fillMaxSize()) {
                    drawCustomPetVector(petType = petType)
                }
            }
        }

        // Superposición vectorial de UNA SOLA PIEZA CENTRAL del accesorio elegido
        if (accessory != PetAccessory.NONE) {
            Canvas(modifier = Modifier.size(180.dp)) {
                drawPetAccessoryOverlay(accessory = accessory)
            }
        }
    }
}

/**
 * Ilustraciones vectoriales detalladas de cuerpo completo en Canvas para cada especie desbloqueable.
 */
private fun DrawScope.drawCustomPetVector(petType: PetType) {
    val cx = size.width / 2f
    val cy = size.height / 2f + 6f
    val r = size.minDimension * 0.28f

    when (petType) {
        PetType.OCTO -> {
            drawCircle(color = Color(0xFFFF7043), radius = r, center = Offset(cx, cy))
        }

        PetType.DRAGON -> {
            // Alas desplegadas de Ignis el Dragón
            val leftWing = Path().apply {
                moveTo(cx - r * 0.5f, cy + r * 0.1f)
                lineTo(cx - r * 1.55f, cy - r * 0.65f)
                lineTo(cx - r * 1.35f, cy + r * 0.55f)
                close()
            }
            val rightWing = Path().apply {
                moveTo(cx + r * 0.5f, cy + r * 0.1f)
                lineTo(cx + r * 1.55f, cy - r * 0.65f)
                lineTo(cx + r * 1.35f, cy + r * 0.55f)
                close()
            }
            drawPath(leftWing, color = Color(0xFFFF8F00))
            drawPath(rightWing, color = Color(0xFFFF8F00))

            // Cuerpo inferior y pancita escamada
            drawOval(
                color = Color(0xFFD32F2F),
                topLeft = Offset(cx - r * 0.85f, cy + r * 0.35f),
                size = Size(r * 1.7f, r * 1.25f)
            )
            drawOval(
                color = Color(0xFFFFCC80),
                topLeft = Offset(cx - r * 0.55f, cy + r * 0.48f),
                size = Size(r * 1.1f, r * 0.95f)
            )

            // Cuernos dorados superiores
            val leftHorn = Path().apply {
                moveTo(cx - r * 0.52f, cy - r * 0.72f)
                lineTo(cx - r * 0.85f, cy - r * 1.42f)
                lineTo(cx - r * 0.18f, cy - r * 0.88f)
                close()
            }
            val rightHorn = Path().apply {
                moveTo(cx + r * 0.52f, cy - r * 0.72f)
                lineTo(cx + r * 0.85f, cy - r * 1.42f)
                lineTo(cx + r * 0.18f, cy - r * 0.88f)
                close()
            }
            drawPath(leftHorn, color = Color(0xFFFFD54F))
            drawPath(rightHorn, color = Color(0xFFFFD54F))

            // Cabeza principal de Ignis
            drawCircle(color = Color(0xFFE53935), radius = r, center = Offset(cx, cy - r * 0.1f))
            // Ojos luminosos
            drawCircle(color = Color(0xFFFFF176), radius = r * 0.18f, center = Offset(cx - r * 0.36f, cy - r * 0.22f))
            drawCircle(color = Color(0xFFFFF176), radius = r * 0.18f, center = Offset(cx + r * 0.36f, cy - r * 0.22f))
            drawCircle(color = Color(0xFF212121), radius = r * 0.09f, center = Offset(cx - r * 0.36f, cy - r * 0.22f))
            drawCircle(color = Color(0xFF212121), radius = r * 0.09f, center = Offset(cx + r * 0.36f, cy - r * 0.22f))
        }

        PetType.FOX -> {
            // Cola esponjosa de Kitsune
            drawOval(
                color = Color(0xFFF57C00),
                topLeft = Offset(cx + r * 0.35f, cy + r * 0.25f),
                size = Size(r * 1.15f, r * 0.75f)
            )
            // Cuerpo sentado de Kitsune
            drawOval(
                color = Color(0xFFFF9800),
                topLeft = Offset(cx - r * 0.75f, cy + r * 0.35f),
                size = Size(r * 1.5f, r * 1.2f)
            )
            drawOval(
                color = Color.White,
                topLeft = Offset(cx - r * 0.45f, cy + r * 0.45f),
                size = Size(r * 0.9f, r * 0.85f)
            )
            // Orejas puntiagudas
            val leftEar = Path().apply {
                moveTo(cx - r * 0.75f, cy - r * 0.45f)
                lineTo(cx - r * 0.98f, cy - r * 1.42f)
                lineTo(cx - r * 0.18f, cy - r * 0.88f)
                close()
            }
            val rightEar = Path().apply {
                moveTo(cx + r * 0.75f, cy - r * 0.45f)
                lineTo(cx + r * 0.98f, cy - r * 1.42f)
                lineTo(cx + r * 0.18f, cy - r * 0.88f)
                close()
            }
            drawPath(leftEar, color = Color(0xFFE65100))
            drawPath(rightEar, color = Color(0xFFE65100))

            // Cabeza naranja y mejillas blancas
            drawCircle(color = Color(0xFFFF9800), radius = r, center = Offset(cx, cy - r * 0.1f))
            drawOval(
                color = Color.White,
                topLeft = Offset(cx - r * 0.75f, cy - r * 0.05f),
                size = Size(r * 1.5f, r * 0.8f)
            )
            // Ojos y nariz
            drawCircle(color = Color(0xFF263238), radius = r * 0.13f, center = Offset(cx - r * 0.38f, cy - r * 0.22f))
            drawCircle(color = Color(0xFF263238), radius = r * 0.13f, center = Offset(cx + r * 0.38f, cy - r * 0.22f))
            drawCircle(color = Color(0xFF3E2723), radius = r * 0.13f, center = Offset(cx, cy + r * 0.18f))
        }

        PetType.PANDA -> {
            // Cuerpo rechoncho de Bambú el Panda
            drawOval(
                color = Color(0xFF212121),
                topLeft = Offset(cx - r * 0.92f, cy + r * 0.30f),
                size = Size(r * 1.84f, r * 1.25f)
            )
            drawOval(
                color = Color(0xFFFAFAFA),
                topLeft = Offset(cx - r * 0.62f, cy + r * 0.45f),
                size = Size(r * 1.24f, r * 0.95f)
            )
            // Orejas redondas negras
            drawCircle(color = Color(0xFF212121), radius = r * 0.36f, center = Offset(cx - r * 0.72f, cy - r * 0.82f))
            drawCircle(color = Color(0xFF212121), radius = r * 0.36f, center = Offset(cx + r * 0.72f, cy - r * 0.82f))
            // Cabeza blanca
            drawCircle(color = Color(0xFFFAFAFA), radius = r, center = Offset(cx, cy - r * 0.12f))
            // Parches oculares negros
            drawOval(
                color = Color(0xFF212121),
                topLeft = Offset(cx - r * 0.64f, cy - r * 0.42f),
                size = Size(r * 0.46f, r * 0.36f)
            )
            drawOval(
                color = Color(0xFF212121),
                topLeft = Offset(cx + r * 0.18f, cy - r * 0.42f),
                size = Size(r * 0.46f, r * 0.36f)
            )
            // Pupilas brillantes y mejillas rosadas
            drawCircle(color = Color.White, radius = r * 0.08f, center = Offset(cx - r * 0.38f, cy - r * 0.25f))
            drawCircle(color = Color.White, radius = r * 0.08f, center = Offset(cx + r * 0.38f, cy - r * 0.25f))
            drawCircle(color = Color(0xFFF48FB1).copy(alpha = 0.65f), radius = r * 0.16f, center = Offset(cx - r * 0.55f, cy + r * 0.10f))
            drawCircle(color = Color(0xFFF48FB1).copy(alpha = 0.65f), radius = r * 0.16f, center = Offset(cx + r * 0.55f, cy + r * 0.10f))
            drawCircle(color = Color(0xFF212121), radius = r * 0.12f, center = Offset(cx, cy + r * 0.05f))
        }

        PetType.AXOLOTL -> {
            // Cuerpo acuático del Axolote Místico
            drawOval(
                color = Color(0xFFF48FB1),
                topLeft = Offset(cx - r * 0.72f, cy + r * 0.35f),
                size = Size(r * 1.44f, r * 1.15f)
            )
            // Branquias externas laterales suaves
            for (angleOffset in listOf(-22f, 0f, 22f)) {
                val yOff = angleOffset * 0.75f
                drawRoundRect(
                    color = Color(0xFFEC407A),
                    topLeft = Offset(cx - r * 1.38f, cy - r * 0.25f + yOff),
                    size = Size(r * 0.52f, r * 0.20f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
                drawRoundRect(
                    color = Color(0xFFEC407A),
                    topLeft = Offset(cx + r * 0.86f, cy - r * 0.25f + yOff),
                    size = Size(r * 0.52f, r * 0.20f),
                    cornerRadius = CornerRadius(12f, 12f)
                )
            }
            // Cabeza pastel del Axolote
            drawCircle(color = Color(0xFFF8BBD0), radius = r, center = Offset(cx, cy - r * 0.1f))
            drawCircle(color = Color(0xFF37474F), radius = r * 0.13f, center = Offset(cx - r * 0.38f, cy - r * 0.20f))
            drawCircle(color = Color(0xFF37474F), radius = r * 0.13f, center = Offset(cx + r * 0.38f, cy - r * 0.20f))
            drawCircle(color = Color.White, radius = r * 0.05f, center = Offset(cx - r * 0.40f, cy - r * 0.24f))
            drawCircle(color = Color.White, radius = r * 0.05f, center = Offset(cx + r * 0.36f, cy - r * 0.24f))
            // Sonrisa tierna
            drawArc(
                color = Color(0xFFAD1457),
                startAngle = 20f,
                sweepAngle = 140f,
                useCenter = false,
                topLeft = Offset(cx - r * 0.28f, cy - r * 0.06f),
                size = Size(r * 0.56f, r * 0.30f),
                style = Stroke(width = 4f, cap = StrokeCap.Round)
            )
        }
    }
}

/**
 * Dibuja el accesorio seleccionado en **UNA SOLA PIEZA CENTRAL** perfectamente posicionada sobre la mascota.
 */
private fun DrawScope.drawPetAccessoryOverlay(accessory: PetAccessory) {
    val cx = size.width / 2f
    val cy = size.height / 2f

    when (accessory) {
        PetAccessory.NONE -> Unit

        PetAccessory.SUNGLASSES -> {
            val eyeY = cy - size.height * 0.07f
            val lensW = size.width * 0.21f
            val lensH = size.height * 0.12f
            // Puente central y patillas
            drawLine(
                color = Color(0xFF0F172A),
                start = Offset(cx - lensW * 1.25f, eyeY),
                end = Offset(cx + lensW * 1.25f, eyeY),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
            // Lente izquierdo y derecho unidos en una sola montura
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(cx - lensW * 1.12f, eyeY - 4f),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(8f, 8f)
            )
            drawRoundRect(
                color = Color(0xFF1E293B),
                topLeft = Offset(cx + lensW * 0.12f, eyeY - 4f),
                size = Size(lensW, lensH),
                cornerRadius = CornerRadius(8f, 8f)
            )
            // Reflejo turquesa en los lentes
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(cx - lensW * 0.92f, eyeY + 4f),
                end = Offset(cx - lensW * 0.62f, eyeY + lensH - 6f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
            drawLine(
                color = Color(0xFF38BDF8),
                start = Offset(cx + lensW * 0.32f, eyeY + 4f),
                end = Offset(cx + lensW * 0.62f, eyeY + lensH - 6f),
                strokeWidth = 3f,
                cap = StrokeCap.Round
            )
        }

        PetAccessory.SCARF -> {
            val neckY = cy + size.height * 0.18f
            val scarfW = size.width * 0.52f
            // Banda central de la bufanda
            drawRoundRect(
                color = Color(0xFFD32F2F),
                topLeft = Offset(cx - scarfW / 2f, neckY),
                size = Size(scarfW, 24f),
                cornerRadius = CornerRadius(12f, 12f)
            )
            // Detalle dorado central
            drawLine(
                color = Color(0xFFFFD54F),
                start = Offset(cx - scarfW * 0.22f, neckY + 4f),
                end = Offset(cx - scarfW * 0.22f, neckY + 20f),
                strokeWidth = 5f
            )
            // Caída de la bufanda
            drawRoundRect(
                color = Color(0xFFC62828),
                topLeft = Offset(cx + scarfW * 0.10f, neckY + 10f),
                size = Size(22f, 36f),
                cornerRadius = CornerRadius(8f, 8f)
            )
        }

        PetAccessory.HEADPHONES -> {
            val arcRectTop = cy - size.height * 0.42f
            val arcW = size.width * 0.72f
            // Diadema superior unificada
            drawArc(
                color = Color(0xFF00E5FF),
                startAngle = 180f,
                sweepAngle = 180f,
                useCenter = false,
                topLeft = Offset(cx - arcW / 2f, arcRectTop),
                size = Size(arcW, size.height * 0.66f),
                style = Stroke(width = 10f, cap = StrokeCap.Round)
            )
            // Almohadillas de la diadema
            drawRoundRect(
                color = Color(0xFF311B92),
                topLeft = Offset(cx - arcW / 2f - 10f, cy - 24f),
                size = Size(22f, 44f),
                cornerRadius = CornerRadius(11f, 11f)
            )
            drawRoundRect(
                color = Color(0xFF311B92),
                topLeft = Offset(cx + arcW / 2f - 12f, cy - 24f),
                size = Size(22f, 44f),
                cornerRadius = CornerRadius(11f, 11f)
            )
        }

        PetAccessory.WIZARD_HAT -> {
            val hatBaseY = cy - size.height * 0.26f
            val conePath = Path().apply {
                moveTo(cx, cy - size.height * 0.50f)
                lineTo(cx - size.width * 0.22f, hatBaseY)
                lineTo(cx + size.width * 0.22f, hatBaseY)
                close()
            }
            drawPath(conePath, color = Color(0xFF4A148C))
            // Ala del sombrero centrada
            drawOval(
                color = Color(0xFF6A1B9A),
                topLeft = Offset(cx - size.width * 0.32f, hatBaseY - 8f),
                size = Size(size.width * 0.64f, 18f)
            )
            // Cinta dorada mágica
            drawLine(
                color = Color(0xFFFFD54F),
                start = Offset(cx - size.width * 0.18f, hatBaseY - 3f),
                end = Offset(cx + size.width * 0.18f, hatBaseY - 3f),
                strokeWidth = 6f,
                cap = StrokeCap.Round
            )
        }

        PetAccessory.GOLD_CROWN -> {
            val crownBaseY = cy - size.height * 0.27f
            val crownW = size.width * 0.36f
            val crownH = size.height * 0.16f
            val crownPath = Path().apply {
                moveTo(cx - crownW / 2f, crownBaseY)
                lineTo(cx - crownW * 0.44f, crownBaseY - crownH)
                lineTo(cx - crownW * 0.15f, crownBaseY - crownH * 0.45f)
                lineTo(cx, crownBaseY - crownH * 1.15f)
                lineTo(cx + crownW * 0.15f, crownBaseY - crownH * 0.45f)
                lineTo(cx + crownW * 0.44f, crownBaseY - crownH)
                lineTo(cx + crownW / 2f, crownBaseY)
                close()
            }
            drawPath(crownPath, color = Color(0xFFFFCA28))
            // Gema central rubí
            drawCircle(
                color = Color(0xFFE53935),
                radius = 5.5f,
                center = Offset(cx, crownBaseY - crownH * 0.38f)
            )
        }
    }
}

/**
 * Botón de acción del Santuario rediseñado tipo cristal oscuro con ícono vectorial nítido y chip de recompensa.
 */
@Composable
private fun SanctuaryActionButton(
    icon: ImageVector,
    iconTint: Color,
    label: String,
    rewardBadge: String,
    modifier: Modifier = Modifier,
    onClick: () -> Unit
) {
    Surface(
        onClick = onClick,
        modifier = modifier.border(
            width = 1.2.dp,
            color = iconTint.copy(alpha = 0.55f),
            shape = RoundedCornerShape(18.dp)
        ),
        shape = RoundedCornerShape(18.dp),
        color = Color(0xFF0F172A).copy(alpha = 0.72f),
        contentColor = Color.White
    ) {
        Column(
            modifier = Modifier.padding(vertical = 12.dp, horizontal = 8.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(6.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(38.dp)
                    .background(iconTint.copy(alpha = 0.20f), CircleShape)
                    .border(1.dp, iconTint.copy(alpha = 0.65f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = icon,
                    contentDescription = label,
                    tint = iconTint,
                    modifier = Modifier.size(20.dp)
                )
            }

            Text(
                text = label,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 13.sp,
                color = Color.White,
                maxLines = 1
            )

            Surface(
                shape = RoundedCornerShape(50),
                color = iconTint.copy(alpha = 0.22f)
            ) {
                Text(
                    text = rewardBadge,
                    fontSize = 10.sp,
                    color = Color.White,
                    fontWeight = FontWeight.Bold,
                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 2.dp)
                )
            }
        }
    }
}

@Composable
private fun VitalStatProgressRow(
    emoji: String,
    label: String,
    percent: Int,
    barColor: Color,
    badgeText: String,
    isDark: Boolean
) {
    Column(modifier = Modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(verticalAlignment = Alignment.CenterVertically) {
                Text(text = emoji, fontSize = 15.sp)
                Spacer(modifier = Modifier.width(6.dp))
                Text(
                    text = label,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Bold,
                    color = if (isDark) TextLight else TextDark
                )
            }
            Text(
                text = "$percent% • $badgeText",
                fontSize = 12.sp,
                fontWeight = FontWeight.ExtraBold,
                color = barColor
            )
        }
        Spacer(modifier = Modifier.height(6.dp))
        LinearProgressIndicator(
            progress = { (percent.coerceIn(0, 100)) / 100f },
            modifier = Modifier
                .fillMaxWidth()
                .height(9.dp)
                .clip(RoundedCornerShape(5.dp)),
            color = barColor,
            trackColor = if (isDark) Color(0xFF334155) else Color(0xFFE2E8F0)
        )
    }
}

@Composable
private fun SanctuaryOptionCard(
    title: String,
    emoji: String,
    subtitle: String,
    badge: String,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val borderColor = if (isSelected) PrimaryTeal else Color.Gray.copy(alpha = 0.25f)
    val bg = when {
        isSelected -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)
        else -> if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
    }

    Card(
        modifier = Modifier
            .width(150.dp)
            .border(if (isSelected) 2.5.dp else 1.dp, borderColor, RoundedCornerShape(16.dp))
            .clickable(onClick = onClick),
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = bg)
    ) {
        Column(
            modifier = Modifier.padding(12.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(PrimaryTeal.copy(alpha = 0.14f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(text = emoji, fontSize = 26.sp)
            }
            Spacer(modifier = Modifier.height(6.dp))
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 12.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isDark) TextLight else TextDark
            )
            Text(
                text = subtitle,
                fontSize = 10.sp,
                maxLines = 2,
                overflow = TextOverflow.Ellipsis,
                textAlign = TextAlign.Center,
                lineHeight = 13.sp,
                color = if (isDark) Color(0xFF94A3B8) else TextGrayLight,
                modifier = Modifier.height(28.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = if (isSelected) "✓ Equipado" else badge,
                fontSize = 10.sp,
                fontWeight = FontWeight.ExtraBold,
                color = if (isSelected) PrimaryTeal else Color(0xFF00ACC1)
            )
        }
    }
}

/**
 * Tarjeta Guía Visible y Permanente que explica al usuario cómo funcionan la Felicidad, la Energía y la XP.
 * Se muestra directamente en pantalla (sin esconderse solo en un botón pequeño).
 */
@Composable
fun PetStatsExplanationCard(
    currentStreak: Int,
    happinessPercent: Int,
    energyPercent: Int,
    petLevel: Int,
    isDark: Boolean
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
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
            Row(verticalAlignment = Alignment.CenterVertically) {
                Box(
                    modifier = Modifier
                        .size(36.dp)
                        .background(PrimaryTeal.copy(alpha = 0.15f), CircleShape),
                    contentAlignment = Alignment.Center
                ) {
                    Icon(
                        imageVector = Icons.Default.Info,
                        contentDescription = null,
                        tint = PrimaryTeal,
                        modifier = Modifier.size(20.dp)
                    )
                }
                Spacer(modifier = Modifier.width(10.dp))
                Column {
                    Text(
                        text = "¿Cómo funcionan la Felicidad, Energía y Nivel?",
                        fontWeight = FontWeight.ExtraBold,
                        fontSize = 15.sp,
                        color = if (isDark) TextLight else TextDark
                    )
                    Text(
                        text = "Guía rápida para cuidar y evolucionar a tu mascota",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else TextGrayLight
                    )
                }
            }

            // Bloque 1: Felicidad
            ExplanationRuleBlock(
                emoji = "❤️",
                title = "Felicidad (50% base + 10% por día de racha • Actual: $happinessPercent%)",
                description = "Sube cada día consecutivo que cumples al menos un microhábito o cuando alimentas y acaricias a tu mascota en el Santuario. Si pasas un día en cero, tu mascota se pone triste (baja a 40%).",
                accentColor = Color(0xFFE91E63),
                isDark = isDark
            )

            // Bloque 2: Energía
            ExplanationRuleBlock(
                emoji = "⚡",
                title = "Energía (Progreso diario + Entrenamiento • Actual: $energyPercent%)",
                description = "Sube conforme completas tus hábitos programados de hoy, mantienes tu racha ($currentStreak días) o entrenas en el Santuario y en el Modo Enfoque del Tracker. Al llegar al 100%, tu mascota entra en estado Radiante.",
                accentColor = Color(0xFFFFB300),
                isDark = isDark
            )

            // Bloque 3: Experiencia (XP) y Nivel
            ExplanationRuleBlock(
                emoji = "⭐",
                title = "Experiencia (XP) y Evolución (Nivel actual: $petLevel)",
                description = "Ganas +10 XP por cada hábito completado y +25 XP por sesión de enfoque en el Tracker. Cada 100 XP subes 1 Nivel para desbloquear nuevos compañeros (Dragón, Zorro, Panda, Axolote), hábitats y accesorios.",
                accentColor = PrimaryTeal,
                isDark = isDark
            )
        }
    }
}

@Composable
private fun ExplanationRuleBlock(
    emoji: String,
    title: String,
    description: String,
    accentColor: Color,
    isDark: Boolean
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .background(
                if (isDark) Color(0xFF0F172A) else accentColor.copy(alpha = 0.08f),
                RoundedCornerShape(14.dp)
            )
            .padding(12.dp),
        verticalAlignment = Alignment.Top
    ) {
        Text(text = emoji, fontSize = 20.sp)
        Spacer(modifier = Modifier.width(10.dp))
        Column {
            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 13.sp,
                color = accentColor
            )
            Spacer(modifier = Modifier.height(3.dp))
            Text(
                text = description,
                fontSize = 12.sp,
                lineHeight = 17.sp,
                color = if (isDark) Color(0xFFCBD5E1) else TextDark.copy(alpha = 0.85f)
            )
        }
    }
}

/**
 * Componente legado mantenido por compatibilidad de firma; el Modo Enfoque con bloqueo vive en TrackerScreen.
 */
@Composable
fun PomodoroFocusTimerCard(
    petName: String,
    petEmoji: String,
    isDark: Boolean,
    onSessionFinished: (Int) -> Unit
) {
    var selectedMinutes by rememberSaveable { mutableIntStateOf(5) }
    var remainingSeconds by rememberSaveable { mutableIntStateOf(5 * 60) }
    var isRunning by rememberSaveable { mutableStateOf(false) }

    LaunchedEffect(isRunning, remainingSeconds) {
        if (isRunning && remainingSeconds > 0) {
            delay(1000L)
            remainingSeconds -= 1
            if (remainingSeconds == 0) {
                isRunning = false
                onSessionFinished(selectedMinutes)
            }
        }
    }

    val mm = (remainingSeconds / 60).toString().padStart(2, '0')
    val ss = (remainingSeconds % 60).toString().padStart(2, '0')

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.Timer,
                        contentDescription = null,
                        tint = Color(0xFFFF9800)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Modo Enfoque • Entrenar con $petName",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (isDark) TextLight else TextDark
                        )
                        Text(
                            text = "Concéntrate en tu hábito actual y gana +15% Energía y +25 XP",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else TextGrayLight
                        )
                    }
                }
                Text(text = petEmoji, fontSize = 24.sp)
            }

            Spacer(modifier = Modifier.height(12.dp))

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                listOf(1 to "1m Demo", 5 to "5 min", 15 to "15 min", 25 to "25 min").forEach { (mins, label) ->
                    val active = selectedMinutes == mins
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(10.dp))
                            .background(
                                if (active) PrimaryTeal
                                else if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9)
                            )
                            .clickable(enabled = !isRunning) {
                                selectedMinutes = mins
                                remainingSeconds = mins * 60
                            }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = label,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (active) Color.White else (if (isDark) TextLight else TextDark)
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(14.dp))

            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .background(
                        if (isDark) Color(0xFF0F172A) else Color(0xFFF0FDF4),
                        RoundedCornerShape(16.dp)
                    )
                    .padding(horizontal = 16.dp, vertical = 12.dp),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "$mm:$ss",
                        fontSize = 30.sp,
                        fontWeight = FontWeight.ExtraBold,
                        color = PrimaryTeal
                    )
                    Text(
                        text = if (isRunning) "$petName está enfocado contigo..." else "Listo para iniciar sesión",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else TextGrayLight
                    )
                }

                Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                    Button(
                        onClick = { isRunning = !isRunning },
                        colors = ButtonDefaults.buttonColors(
                            containerColor = if (isRunning) Color(0xFFFF9800) else PrimaryTeal
                        ),
                        shape = RoundedCornerShape(12.dp)
                    ) {
                        Icon(
                            imageVector = if (isRunning) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(
                            text = if (isRunning) "Pausar" else "Iniciar",
                            fontWeight = FontWeight.Bold,
                            fontSize = 12.sp
                        )
                    }

                    IconButton(
                        onClick = {
                            isRunning = false
                            remainingSeconds = selectedMinutes * 60
                        }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Reiniciar reloj",
                            tint = if (isDark) TextLight else TextDark
                        )
                    }
                }
            }
        }
    }
}

/**
 * Mejora Autónoma: Galería de Medallas y Logros Desbloqueables.
 */
@Composable
fun AchievementsShowcaseCard(
    achievements: List<AchievementBadge>,
    isDark: Boolean
) {
    val unlockedCount = achievements.count { it.isUnlocked }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(22.dp),
        colors = CardDefaults.cardColors(
            containerColor = if (isDark) Color(0xFF1E293B) else Color.White
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(18.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.EmojiEvents,
                        contentDescription = null,
                        tint = Color(0xFFFFB300)
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Column {
                        Text(
                            text = "Medallas y Logros Desbloqueables",
                            fontWeight = FontWeight.ExtraBold,
                            fontSize = 15.sp,
                            color = if (isDark) TextLight else TextDark
                        )
                        Text(
                            text = "Has desbloqueado $unlockedCount de ${achievements.size} insignias",
                            fontSize = 11.sp,
                            color = if (isDark) Color(0xFF94A3B8) else TextGrayLight
                        )
                    }
                }
            }

            Spacer(modifier = Modifier.height(12.dp))

            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                contentPadding = PaddingValues(horizontal = 2.dp)
            ) {
                items(achievements, key = { it.id }) { badge ->
                    val accent = Color(badge.accentHex)
                    Card(
                        modifier = Modifier
                            .width(155.dp)
                            .border(
                                width = if (badge.isUnlocked) 2.dp else 1.dp,
                                color = if (badge.isUnlocked) accent else Color.Gray.copy(alpha = 0.25f),
                                shape = RoundedCornerShape(16.dp)
                            ),
                        shape = RoundedCornerShape(16.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = if (badge.isUnlocked) {
                                if (isDark) accent.copy(alpha = 0.16f) else accent.copy(alpha = 0.10f)
                            } else {
                                if (isDark) Color(0xFF0F172A) else Color(0xFFF8FAFC)
                            }
                        )
                    ) {
                        Column(
                            modifier = Modifier.padding(12.dp),
                            horizontalAlignment = Alignment.CenterHorizontally
                        ) {
                            Box(
                                modifier = Modifier
                                    .size(44.dp)
                                    .background(
                                        if (badge.isUnlocked) accent.copy(alpha = 0.22f)
                                        else Color.Gray.copy(alpha = 0.15f),
                                        CircleShape
                                    ),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = if (badge.isUnlocked) badge.emoji else "🔒",
                                    fontSize = 22.sp
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            Text(
                                text = badge.title,
                                fontWeight = FontWeight.Bold,
                                fontSize = 12.sp,
                                maxLines = 1,
                                color = if (isDark) TextLight else TextDark
                            )
                            Text(
                                text = badge.description,
                                fontSize = 10.sp,
                                lineHeight = 13.sp,
                                maxLines = 2,
                                overflow = TextOverflow.Ellipsis,
                                textAlign = TextAlign.Center,
                                color = if (isDark) Color(0xFF94A3B8) else TextGrayLight,
                                modifier = Modifier.height(28.dp)
                            )
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = if (badge.isUnlocked) "✓ Desbloqueado" else badge.progressText,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.ExtraBold,
                                color = if (badge.isUnlocked) accent else Color(0xFF94A3B8)
                            )
                        }
                    }
                }
            }
        }
    }
}
