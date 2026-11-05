package com.example.ui

import android.widget.Toast
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
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
import androidx.compose.foundation.layout.offset
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.CloudDone
import androidx.compose.material.icons.filled.ContentCopy
import androidx.compose.material.icons.filled.EmojiEvents
import androidx.compose.material.icons.filled.Favorite
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material.icons.automirrored.filled.HelpOutline
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.LocalFireDepartment
import androidx.compose.material.icons.filled.Lock
import androidx.compose.material.icons.filled.OpenInFull
import androidx.compose.material.icons.filled.Palette
import androidx.compose.material.icons.filled.PersonAdd
import androidx.compose.material.icons.filled.Pets
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Share
import androidx.compose.material.icons.filled.Tag
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Surface
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRow
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.model.AchievementCatalog
import com.example.model.CloudSyncManager
import com.example.model.PetAccessory
import com.example.model.PetCustomization
import com.example.model.PetHabitat
import com.example.model.PetStatus
import com.example.model.PetType
import com.example.model.RankingPeriod
import com.example.model.RankingUser
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextDark
import com.example.ui.theme.TextGrayLight
import com.example.ui.theme.TextLight
import com.example.ui.theme.isAppInDarkTheme

/**
 * Pantalla que combina la Vista de Mascota, acceso a su Espacio Propio (Santuario),
 * la Guía Visible de Felicidad y Energía, y el Ranking Social Multi-Usuario con `#` de 3 dígitos.
 */
@Composable
fun PetAndRankingScreen(
    currentStreak: Int,
    totalCompleted: Int,
    userName: String = "Suchini",
    userFriendCode: String = "777",
    completedToday: Int = 0,
    scheduledToday: Int = 1,
    initialCustomization: PetCustomization = PetCustomization(),
    externalFriends: List<RankingUser> = emptyList(),
    onUpdateCustomization: (PetCustomization) -> Unit = {},
    onAddFriendByCode: (code: String, name: String?, streak: Int?, habits: Int?) -> Unit = { _, _, _, _ -> },
    onOpenSanctuary: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val cloudSync = remember { CloudSyncManager.getInstance(context) }
    val activeSession by cloudSync.currentSession.collectAsState()
    val syncedFriends by cloudSync.syncedFriends.collectAsState()

    var selectedSection by rememberSaveable { mutableIntStateOf(0) }
    var internalShowSanctuary by rememberSaveable { mutableStateOf(false) }
    val isDark = isAppInDarkTheme

    // Estado de personalización sincronizado con el usuario
    var petCustomization by remember(initialCustomization) {
        mutableStateOf(initialCustomization)
    }

    // Sincronizar estadísticas del usuario actual en segundo plano
    val effectiveFriendCode = remember(userFriendCode, activeSession, userName) {
        val candidate = activeSession?.normalizedTag ?: userFriendCode
        if (candidate.isBlank() || candidate == "000") {
            CloudSyncManager.generateDeterministicTag(userName)
        } else {
            CloudSyncManager.normalizeTagCode(candidate)
        }
    }

    LaunchedEffect(totalCompleted, currentStreak, petCustomization, userName) {
        cloudSync.syncUserStatsAsync(
            completedHabits = totalCompleted,
            currentStreak = currentStreak,
            petCustomization = petCustomization,
            userNameOverride = userName
        )
    }

    // Modal de explicación adicional si el usuario toca el botón de ayuda
    var showHelpDialog by remember { mutableStateOf(false) }

    // Si el usuario abrió el Espacio Propio / Santuario desde aquí y no hay navegador externo, lo mostramos a pantalla completa
    if (internalShowSanctuary && onOpenSanctuary == null) {
        PetSanctuaryScreen(
            currentStreak = currentStreak,
            totalCompleted = totalCompleted,
            completedToday = completedToday,
            scheduledToday = scheduledToday,
            friendsCount = maxOf(syncedFriends.size, externalFriends.size),
            initialCustomization = petCustomization,
            onCustomizationChanged = { updated ->
                petCustomization = updated
                onUpdateCustomization(updated)
            },
            onBack = { internalShowSanctuary = false }
        )
        return
    }

    // Cálculo dinámico de nivel de mascota según hábitos completados (1 nivel cada 10 hábitos)
    val petLevel = maxOf(1, totalCompleted / 10 + 1)
    val xpInLevel = (totalCompleted % 10) * 10

    // Reglas claras de Felicidad y Energía:
    // - Felicidad: 50% base + 10% por día consecutivo de racha (baja a 40% si la racha está en 0).
    // - Energía: basada en el progreso del día (completedToday / scheduledToday) + racha activa.
    val happiness = if (currentStreak > 0) minOf(100, 50 + currentStreak * 10) else 40
    val safeScheduled = maxOf(1, scheduledToday)
    val dailyBonus = ((completedToday.coerceAtMost(safeScheduled) * 35) / safeScheduled)
    val energy = if (currentStreak > 0 || completedToday > 0) {
        minOf(100, 55 + dailyBonus + currentStreak * 8)
    } else {
        40
    }

    val petDisplayName = petCustomization.petName.ifBlank { petCustomization.petType.displayName }

    val moodTitle = when {
        currentStreak >= 7 -> "¡Modo Leyenda Activo! 👑"
        currentStreak >= 3 -> "¡$petDisplayName está radiante! 🔥"
        currentStreak >= 1 -> "¡$petDisplayName está muy contento! ✨"
        else -> "$petDisplayName tiene un poco de sueño 💤"
    }

    val moodDesc = when {
        currentStreak >= 7 -> "¡Racha colosal! Tu constancia de 7+ días ha desbloqueado las auras doradas."
        currentStreak >= 3 -> "¡3 o más días seguidos completando hábitos! La felicidad y energía están al máximo."
        currentStreak >= 1 -> "Buen trabajo hoy. Cada hábito cumplido alimenta la felicidad y fortalece la energía."
        else -> "Completa al menos un hábito hoy o visita su Santuario para darle energía y subir su felicidad."
    }

    val petStatus = remember(currentStreak, totalCompleted, petCustomization, happiness, energy) {
        PetStatus(
            name = petDisplayName,
            customization = petCustomization,
            level = petLevel,
            experience = xpInLevel,
            maxExperience = 100,
            happinessPercent = happiness,
            energyPercent = energy,
            currentStreak = currentStreak,
            moodTitle = moodTitle,
            moodDescription = moodDesc
        )
    }

    Column(
        modifier = modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        // Selector superior entre Mascota y Ranking
        TabRow(
            selectedTabIndex = selectedSection,
            containerColor = MaterialTheme.colorScheme.surface,
            contentColor = PrimaryTeal,
            indicator = { tabPositions ->
                TabRowDefaults.SecondaryIndicator(
                    Modifier.tabIndicatorOffset(tabPositions[selectedSection]),
                    color = PrimaryTeal
                )
            }
        ) {
            Tab(
                selected = selectedSection == 0,
                onClick = { selectedSection = 0 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.Pets,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Mi Mascota & Hábitat", fontWeight = FontWeight.Bold)
                    }
                }
            )
            Tab(
                selected = selectedSection == 1,
                onClick = { selectedSection = 1 },
                text = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Icon(
                            imageVector = Icons.Default.EmojiEvents,
                            contentDescription = null,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text("Ranking #Amigos", fontWeight = FontWeight.Bold)
                    }
                }
            )
        }

        if (selectedSection == 0) {
            PetDetailsView(
                petStatus = petStatus,
                isDark = isDark,
                currentStreak = currentStreak,
                totalCompleted = totalCompleted,
                friendsCount = maxOf(syncedFriends.size, externalFriends.size),
                onUpdateCustomization = { updated ->
                    petCustomization = updated
                    onUpdateCustomization(updated)
                },
                onShowHelp = { showHelpDialog = true },
                onEnterSanctuary = {
                    if (onOpenSanctuary != null) {
                        onOpenSanctuary()
                    } else {
                        internalShowSanctuary = true
                    }
                }
            )
        } else {
            RankingView(
                currentUserName = userName,
                currentUserTagCode = effectiveFriendCode,
                currentUserStreak = currentStreak,
                currentUserCompleted = totalCompleted,
                currentPet = petCustomization,
                externalFriends = externalFriends,
                onAddFriendByCode = onAddFriendByCode,
                isDark = isDark
            )
        }
    }

    // Modal Explicativo de Felicidad y Energía
    if (showHelpDialog) {
        AlertDialog(
            onDismissRequest = { showHelpDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.Info, contentDescription = null, tint = PrimaryTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("¿Cómo funcionan Felicidad y Energía?", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
                    Text(
                        text = "❤️ Felicidad (50% base + 10% por día de racha):",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFE91E63)
                    )
                    Text(
                        text = "Sube cada día consecutivo que cumples al menos un microhábito o cuando alimentas y acaricias a tu mascota en su Santuario. Si dejas pasar un día en cero, baja a 40%.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "⚡ Energía (Progreso del día + Entrenamiento):",
                        fontWeight = FontWeight.Bold,
                        color = Color(0xFFFFB300)
                    )
                    Text(
                        text = "Sube conforme completas tus hábitos programados de hoy y usas el Modo Enfoque en el Tracker. Al llegar al 100%, tu mascota entra en estado Radiante.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )

                    Text(
                        text = "⭐ Nivel y Experiencia (XP):",
                        fontWeight = FontWeight.Bold,
                        color = PrimaryTeal
                    )
                    Text(
                        text = "Ganas +10 XP por cada hábito completado. Cada 100 XP tu mascota sube 1 Nivel para desbloquear nuevos animales, hábitats y accesorios.",
                        fontSize = 13.sp,
                        lineHeight = 18.sp
                    )
                }
            },
            confirmButton = {
                Button(
                    onClick = { showHelpDialog = false },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("¡Entendido!", color = Color.White)
                }
            }
        )
    }
}

/**
 * Vista de Mascota con acceso al Santuario a pantalla completa, Personalización rápida,
 * Explicación permanente de Felicidad/Energía y Galería de Medallas.
 */
@Composable
fun PetDetailsView(
    petStatus: PetStatus,
    isDark: Boolean,
    currentStreak: Int,
    totalCompleted: Int,
    friendsCount: Int = 4,
    onUpdateCustomization: (PetCustomization) -> Unit,
    onShowHelp: () -> Unit,
    onEnterSanctuary: () -> Unit = {}
) {
    var activeCustomTab by rememberSaveable { mutableIntStateOf(0) } // 0: Animal, 1: Accesorio, 2: Hábitat

    val infiniteTransition = rememberInfiniteTransition(label = "pet_float")
    val offsetY by infiniteTransition.animateFloat(
        initialValue = -8f,
        targetValue = 8f,
        animationSpec = infiniteRepeatable(
            animation = tween(2000, easing = LinearEasing),
            repeatMode = RepeatMode.Reverse
        ),
        label = "float_animation"
    )
    val particleProgress by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(5500, easing = LinearEasing),
            repeatMode = RepeatMode.Restart
        ),
        label = "habitat_anim"
    )

    val achievements = remember(totalCompleted, currentStreak, friendsCount, petStatus.level) {
        AchievementCatalog.evaluate(
            totalCompleted = totalCompleted,
            currentStreak = currentStreak,
            friendsCount = friendsCount,
            petLevel = petStatus.level
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // 1. ESCENARIO PROPIO DE LA MASCOTA CON FONDO ÚNICO ANIMADO Y BOTÓN DE SANTUARIO COMPLETO
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(24.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                ) {
                    // Fondo único ilustrado en Canvas
                    HabitatAnimatedBackground(
                        habitat = petStatus.customization.habitat,
                        animProgress = particleProgress,
                        modifier = Modifier
                            .matchParentSize()
                    )

                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(22.dp),
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
                        // Header del Hábitat
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            Box(
                                modifier = Modifier
                                    .background(Color.Black.copy(alpha = 0.45f), RoundedCornerShape(12.dp))
                                    .padding(horizontal = 12.dp, vertical = 6.dp)
                            ) {
                                Text(
                                    text = "NIVEL ${petStatus.level} • ${petStatus.customization.habitat.displayName.uppercase()}",
                                    color = Color.White,
                                    fontWeight = FontWeight.ExtraBold,
                                    fontSize = 11.sp
                                )
                            }

                            IconButton(
                                onClick = onShowHelp,
                                modifier = Modifier
                                    .size(32.dp)
                                    .background(Color.White.copy(alpha = 0.22f), CircleShape)
                            ) {
                                Icon(
                                    imageVector = Icons.AutoMirrored.Filled.HelpOutline,
                                    contentDescription = "Explicación de Felicidad y Energía",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }

                        Spacer(modifier = Modifier.height(12.dp))

                        // Mascota ilustrada en Canvas + ic_pulpo con Accesorio real superpuesto
                        Box(
                            contentAlignment = Alignment.Center,
                            modifier = Modifier
                                .size(185.dp)
                                .offset(y = offsetY.dp)
                                .clickable { onEnterSanctuary() }
                        ) {
                            PetCharacterCanvasIllustration(
                                petType = petStatus.customization.petType,
                                accessory = petStatus.customization.accessory,
                                happinessPercent = petStatus.happinessPercent,
                                energyPercent = petStatus.energyPercent,
                                modifier = Modifier.fillMaxSize()
                            )
                        }

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = petStatus.moodTitle,
                            fontSize = 19.sp,
                            fontWeight = FontWeight.ExtraBold,
                            color = Color.White,
                            textAlign = TextAlign.Center
                        )

                        Spacer(modifier = Modifier.height(4.dp))

                        Text(
                            text = petStatus.moodDescription,
                            fontSize = 12.sp,
                            color = Color.White.copy(alpha = 0.88f),
                            textAlign = TextAlign.Center,
                            lineHeight = 16.sp,
                            modifier = Modifier.padding(horizontal = 8.dp)
                        )

                        Spacer(modifier = Modifier.height(14.dp))

                        // Botón destacado para entrar al Espacio Propio a Pantalla Completa (Santuario)
                        Button(
                            onClick = onEnterSanctuary,
                            colors = ButtonDefaults.buttonColors(
                                containerColor = SecondaryTeal,
                                contentColor = Color(0xFF00251A)
                            ),
                            shape = RoundedCornerShape(14.dp),
                            modifier = Modifier.fillMaxWidth()
                        ) {
                            Icon(
                                imageVector = Icons.Default.OpenInFull,
                                contentDescription = null,
                                modifier = Modifier.size(18.dp)
                            )
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Entrar al Espacio Propio de ${petStatus.name}",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 13.sp
                            )
                        }

                        Spacer(modifier = Modifier.height(14.dp))

                        // Barra de progreso de Nivel
                        Column(modifier = Modifier.fillMaxWidth()) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween
                            ) {
                                Text(
                                    text = "Experiencia al Nivel ${petStatus.level + 1}",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.SemiBold,
                                    color = Color.White.copy(alpha = 0.9f)
                                )
                                Text(
                                    text = "${petStatus.experience} / ${petStatus.maxExperience} XP",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = SecondaryTeal
                                )
                            }
                            Spacer(modifier = Modifier.height(6.dp))
                            LinearProgressIndicator(
                                progress = { petStatus.experience.toFloat() / petStatus.maxExperience.toFloat() },
                                modifier = Modifier
                                    .fillMaxWidth()
                                    .height(8.dp)
                                    .clip(RoundedCornerShape(4.dp)),
                                color = SecondaryTeal,
                                trackColor = Color.White.copy(alpha = 0.25f)
                            )
                        }
                    }
                }
            }
        }

        // 2. TARJETAS DE MÉTRICAS (Racha, Felicidad, Energía)
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(10.dp)
            ) {
                PetMetricCard(
                    title = "Racha",
                    value = "${petStatus.currentStreak} días",
                    icon = Icons.Default.LocalFireDepartment,
                    iconColor = Color(0xFFFF6D00),
                    containerColor = if (isDark) Color(0xFF3E2723) else Color(0xFFFFE0B2),
                    modifier = Modifier.weight(1f),
                    isDark = isDark,
                    onClick = onShowHelp
                )

                PetMetricCard(
                    title = "Felicidad",
                    value = "${petStatus.happinessPercent}%",
                    icon = Icons.Default.Favorite,
                    iconColor = Color(0xFFE91E63),
                    containerColor = if (isDark) Color(0xFF4A148C) else Color(0xFFFCE4EC),
                    modifier = Modifier.weight(1f),
                    isDark = isDark,
                    onClick = onShowHelp
                )

                PetMetricCard(
                    title = "Energía",
                    value = "${petStatus.energyPercent}%",
                    icon = Icons.Default.FlashOn,
                    iconColor = Color(0xFFFFD600),
                    containerColor = if (isDark) Color(0xFF263238) else Color(0xFFFFF9C4),
                    modifier = Modifier.weight(1f),
                    isDark = isDark,
                    onClick = onShowHelp
                )
            }
        }

        // 3. EXPLICACIÓN VISIBLE Y PERMANENTE DE FELICIDAD, ENERGÍA Y NIVEL
        item {
            PetStatsExplanationCard(
                currentStreak = currentStreak,
                happinessPercent = petStatus.happinessPercent,
                energyPercent = petStatus.energyPercent,
                petLevel = petStatus.level,
                isDark = isDark
            )
        }

        // 4. SECCIÓN DE PERSONALIZACIÓN DEL HÁBITAT, ANIMAL Y ACCESORIOS
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Palette, contentDescription = null, tint = PrimaryTeal)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Personalizar Animal, Accesorios y Fondo",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp,
                                color = if (isDark) TextLight else TextDark
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Selector de categoría de personalización
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(
                                if (isDark) Color(0xFF0F172A) else Color(0xFFF1F5F9),
                                RoundedCornerShape(10.dp)
                            )
                            .padding(3.dp),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        listOf("Compañero", "Accesorios", "Hábitat").forEachIndexed { index, name ->
                            val isSelected = activeCustomTab == index
                            Box(
                                modifier = Modifier
                                    .weight(1f)
                                    .clip(RoundedCornerShape(8.dp))
                                    .background(if (isSelected) PrimaryTeal else Color.Transparent)
                                    .clickable { activeCustomTab = index }
                                    .padding(vertical = 6.dp),
                                contentAlignment = Alignment.Center
                            ) {
                                Text(
                                    text = name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B))
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(14.dp))

                    when (activeCustomTab) {
                        0 -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                items(PetType.entries) { pet ->
                                    val isUnlocked = petStatus.level >= pet.requiredLevel
                                    val isSelected = petStatus.customization.petType == pet

                                    CustomizationCard(
                                        title = pet.displayName,
                                        iconText = pet.iconEmoji,
                                        subtitle = if (isUnlocked) "Disponible" else "Equipar (Nv.${pet.requiredLevel})",
                                        isUnlocked = true,
                                        isSelected = isSelected,
                                        isDark = isDark,
                                        onClick = {
                                            onUpdateCustomization(
                                                petStatus.customization.copy(
                                                    petType = pet,
                                                    petName = pet.displayName.split(" ").firstOrNull() ?: pet.displayName
                                                )
                                            )
                                        }
                                    )
                                }
                            }
                        }
                        1 -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                items(PetAccessory.entries) { acc ->
                                    val isUnlocked = currentStreak >= acc.requiredStreak
                                    val isSelected = petStatus.customization.accessory == acc

                                    CustomizationCard(
                                        title = acc.displayName,
                                        iconText = if (acc == PetAccessory.NONE) "✨" else acc.emoji,
                                        subtitle = if (isUnlocked) "Disponible" else "Equipar (${acc.requiredStreak}d)",
                                        isUnlocked = true,
                                        isSelected = isSelected,
                                        isDark = isDark,
                                        onClick = {
                                            onUpdateCustomization(petStatus.customization.copy(accessory = acc))
                                        }
                                    )
                                }
                            }
                        }
                        2 -> {
                            LazyRow(
                                horizontalArrangement = Arrangement.spacedBy(10.dp),
                                contentPadding = PaddingValues(2.dp)
                            ) {
                                items(PetHabitat.entries) { hab ->
                                    val isUnlocked = totalCompleted >= hab.requiredCompletedHabits
                                    val isSelected = petStatus.customization.habitat == hab

                                    CustomizationCard(
                                        title = hab.displayName,
                                        iconText = hab.emoji,
                                        subtitle = if (isUnlocked) "Disponible" else "Probar (${hab.requiredCompletedHabits} háb.)",
                                        isUnlocked = true,
                                        isSelected = isSelected,
                                        isDark = isDark,
                                        onClick = {
                                            onUpdateCustomization(petStatus.customization.copy(habitat = hab))
                                        }
                                    )
                                }
                            }
                        }
                    }
                }
            }
        }

        // 5. MEDALLAS Y LOGROS DESBLOQUEABLES
        item {
            AchievementsShowcaseCard(
                achievements = achievements,
                isDark = isDark
            )
        }
    }
}

/**
 * Tarjeta individual para seleccionar opciones de personalización.
 */
@Composable
fun CustomizationCard(
    title: String,
    iconText: String,
    subtitle: String,
    isUnlocked: Boolean,
    isSelected: Boolean,
    isDark: Boolean,
    onClick: () -> Unit
) {
    val borderColor = when {
        isSelected -> PrimaryTeal
        isUnlocked -> Color.Transparent
        else -> Color.Gray.copy(alpha = 0.3f)
    }

    val containerColor = when {
        isSelected -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)
        isUnlocked -> if (isDark) Color(0xFF1E293B) else Color(0xFFF8FAFC)
        else -> if (isDark) Color(0xFF0F172A).copy(alpha = 0.6f) else Color(0xFFE2E8F0).copy(alpha = 0.5f)
    }

    Card(
        modifier = Modifier
            .width(120.dp)
            .border(2.dp, borderColor, RoundedCornerShape(14.dp))
            .clickable(enabled = isUnlocked, onClick = onClick),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Box(
                modifier = Modifier.size(44.dp),
                contentAlignment = Alignment.Center
            ) {
                if (isUnlocked) {
                    Text(text = iconText, fontSize = 28.sp)
                } else {
                    Icon(
                        imageVector = Icons.Default.Lock,
                        contentDescription = "Bloqueado",
                        tint = Color.Gray,
                        modifier = Modifier.size(24.dp)
                    )
                }
            }

            Spacer(modifier = Modifier.height(4.dp))

            Text(
                text = title,
                fontWeight = FontWeight.Bold,
                fontSize = 11.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isDark) TextLight else TextDark
            )

            Text(
                text = subtitle,
                fontSize = 10.sp,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                color = if (isSelected) PrimaryTeal else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Normal
            )
        }
    }
}

/**
 * Tarjeta individual de métrica de la mascota con soporte para click explicativo.
 */
@Composable
fun PetMetricCard(
    title: String,
    value: String,
    icon: androidx.compose.ui.graphics.vector.ImageVector,
    iconColor: Color,
    containerColor: Color,
    modifier: Modifier = Modifier,
    isDark: Boolean,
    onClick: () -> Unit
) {
    Card(
        modifier = modifier.clickable { onClick() },
        shape = RoundedCornerShape(16.dp),
        colors = CardDefaults.cardColors(containerColor = containerColor),
        elevation = CardDefaults.cardElevation(defaultElevation = 1.dp)
    ) {
        Column(
            modifier = Modifier.padding(10.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Icon(
                imageVector = icon,
                contentDescription = title,
                tint = iconColor,
                modifier = Modifier.size(22.dp)
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = value,
                fontWeight = FontWeight.ExtraBold,
                fontSize = 14.sp,
                color = if (isDark) TextLight else TextDark
            )
            Text(
                text = title,
                fontSize = 11.sp,
                color = if (isDark) Color(0xFFCBD5E1) else TextGrayLight
            )
        }
    }
}

/**
 * Vista de Ranking Social Multi-Usuario con código `#` de 3 dígitos y sincronización en tiempo real via CloudSyncManager.
 */
@Composable
fun RankingView(
    currentUserName: String,
    currentUserTagCode: String = "777",
    currentUserStreak: Int,
    currentUserCompleted: Int,
    currentPet: PetCustomization = PetCustomization(),
    externalFriends: List<RankingUser> = emptyList(),
    onAddFriendByCode: (code: String, name: String?, streak: Int?, habits: Int?) -> Unit = { _, _, _, _ -> },
    isDark: Boolean
) {
    val context = LocalContext.current
    val cloudSync = remember { CloudSyncManager.getInstance(context) }
    val syncedFriends by cloudSync.syncedFriends.collectAsState()
    val syncState by cloudSync.syncState.collectAsState()
    val lastSyncMessage by cloudSync.lastSyncMessage.collectAsState()

    var selectedPeriod by rememberSaveable { mutableStateOf(RankingPeriod.SEMANAL) }
    var showAddFriendDialog by rememberSaveable { mutableStateOf(false) }
    var friendCodeInput by rememberSaveable { mutableStateOf("") }
    var newFriendName by rememberSaveable { mutableStateOf("") }
    var newFriendStreak by rememberSaveable { mutableStateOf("") }
    var newFriendHabits by rememberSaveable { mutableStateOf("") }

    // Activar bucle de sincronización mientras la pestaña de Ranking está visible
    DisposableEffect(cloudSync) {
        cloudSync.startRealtimeSyncLoop()
        onDispose { cloudSync.stopRealtimeSyncLoop() }
    }

    val normalizedMyTag = remember(currentUserTagCode) {
        CloudSyncManager.normalizeTagCode(currentUserTagCode)
    }

    // Construye la tabla de posiciones combinando amigos sincronizados + amigos de Room + usuario actual
    val allRankings = remember(
        currentUserName,
        normalizedMyTag,
        currentUserCompleted,
        currentUserStreak,
        currentPet,
        syncedFriends,
        externalFriends,
        selectedPeriod
    ) {
        val baseList = cloudSync.buildRealtimeRanking(
            currentUserName = currentUserName,
            currentUserCompleted = currentUserCompleted,
            currentUserStreak = currentUserStreak,
            currentPet = currentPet
        ).toMutableList()

        // Integrar también amigos guardados en Room que no estén aún en baseList
        externalFriends.forEach { ext ->
            val extDigits = Regex("(\\d{3})").find(ext.friendCode.ifBlank { ext.name })?.value
            val alreadyIn = baseList.any {
                (extDigits != null && it.name.contains("#$extDigits")) ||
                    it.name.equals(ext.name, ignoreCase = true)
            }
            if (!alreadyIn && !ext.isCurrentUser) {
                baseList.add(ext)
            }
        }

        val periodMultiplier = when (selectedPeriod) {
            RankingPeriod.SEMANAL -> 1
            RankingPeriod.MENSUAL -> 4
            RankingPeriod.GLOBAL -> 10
        }

        baseList
            .map { user ->
                if (periodMultiplier == 1 || user.isCurrentUser) user
                else user.copy(score = user.score * periodMultiplier)
            }
            .sortedWith(
                compareByDescending<RankingUser> { it.score }
                    .thenByDescending { it.currentStreak }
            )
            .mapIndexed { index, user ->
                user.copy(rankPosition = index + 1)
            }
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        verticalArrangement = Arrangement.spacedBy(14.dp)
    ) {
        // 1. TARJETA DESTACADA: "TU CÓDIGO DE AMIGO: #XXX" CON COPIAR Y COMPARTIR
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(
                    containerColor = if (isDark) Color(0xFF00363A) else Color(0xFFE0F2F1)
                ),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        Column(modifier = Modifier.weight(1f)) {
                            Row(verticalAlignment = Alignment.CenterVertically) {
                                Icon(
                                    imageVector = Icons.Default.Tag,
                                    contentDescription = null,
                                    tint = PrimaryTeal,
                                    modifier = Modifier.size(18.dp)
                                )
                                Spacer(modifier = Modifier.width(6.dp))
                                Text(
                                    text = "Tu Código Único de Amigo",
                                    fontWeight = FontWeight.Bold,
                                    fontSize = 13.sp,
                                    color = if (isDark) SecondaryTeal else PrimaryTeal
                                )
                            }
                            Spacer(modifier = Modifier.height(4.dp))
                            Text(
                                text = "$currentUserName #$normalizedMyTag",
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 22.sp,
                                color = if (isDark) Color.White else TextDark
                            )
                            Text(
                                text = "Pásale tu código #$normalizedMyTag a tus amigos para competir al mismo tiempo",
                                fontSize = 11.sp,
                                color = if (isDark) Color(0xFFB2DFDB) else Color(0xFF004D40)
                            )
                        }

                        // Badge grande del #XXX
                        Box(
                            modifier = Modifier
                                .background(PrimaryTeal, RoundedCornerShape(14.dp))
                                .padding(horizontal = 14.dp, vertical = 10.dp),
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "#$normalizedMyTag",
                                color = Color.White,
                                fontWeight = FontWeight.ExtraBold,
                                fontSize = 18.sp
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(12.dp))

                    // Botones de Copiar #Código, Compartir por WhatsApp y Sincronizar
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        OutlinedButton(
                            onClick = {
                                CloudSyncManager.copyTagToClipboard(
                                    context = context,
                                    tagCode = normalizedMyTag,
                                    userName = currentUserName,
                                    includeSyncToken = false,
                                    completedHabits = currentUserCompleted,
                                    currentStreak = currentUserStreak
                                )
                            },
                            modifier = Modifier.weight(1f),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.ContentCopy,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp),
                                tint = if (isDark) SecondaryTeal else PrimaryTeal
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Copiar #$normalizedMyTag",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) Color.White else PrimaryTeal
                            )
                        }

                        Button(
                            onClick = {
                                CloudSyncManager.shareProfileWithFriends(
                                    context = context,
                                    userName = currentUserName,
                                    tagCode = normalizedMyTag,
                                    completedHabits = currentUserCompleted,
                                    currentStreak = currentUserStreak,
                                    petType = currentPet.petType,
                                    petAccessory = currentPet.accessory
                                )
                            },
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                            shape = RoundedCornerShape(12.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Share,
                                contentDescription = null,
                                modifier = Modifier.size(16.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "Compartir",
                                fontSize = 12.sp,
                                fontWeight = FontWeight.Bold
                            )
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    // Estado de sincronización Cloud / REST en vivo
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically, modifier = Modifier.weight(1f)) {
                            Icon(
                                imageVector = Icons.Default.CloudDone,
                                contentDescription = null,
                                tint = if (isDark) SecondaryTeal else PrimaryTeal,
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(
                                text = "${syncState.label} • $lastSyncMessage",
                                fontSize = 10.sp,
                                maxLines = 1,
                                overflow = TextOverflow.Ellipsis,
                                color = if (isDark) Color(0xFFB2DFDB) else Color(0xFF004D40)
                            )
                        }
                        IconButton(
                            onClick = {
                                cloudSync.syncUserStatsAsync(
                                    completedHabits = currentUserCompleted,
                                    currentStreak = currentUserStreak,
                                    petCustomization = currentPet,
                                    userNameOverride = currentUserName
                                )
                                Toast.makeText(context, "Sincronizando tabla de amigos...", Toast.LENGTH_SHORT).show()
                            },
                            modifier = Modifier.size(26.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Default.Refresh,
                                contentDescription = "Actualizar ranking",
                                tint = if (isDark) SecondaryTeal else PrimaryTeal,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }
            }
        }

        // 2. SELECTOR DE PERÍODO: Semanal / Mensual / Global
        item {
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
                RankingPeriod.entries.forEach { period ->
                    val isSelected = selectedPeriod == period
                    Box(
                        modifier = Modifier
                            .weight(1f)
                            .clip(RoundedCornerShape(8.dp))
                            .background(if (isSelected) PrimaryTeal else Color.Transparent)
                            .clickable { selectedPeriod = period }
                            .padding(vertical = 8.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = period.name.lowercase().replaceFirstChar { it.uppercase() },
                            color = if (isSelected) Color.White else (if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)),
                            fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                            fontSize = 13.sp
                        )
                    }
                }
            }
        }

        // 3. PODIO DE LOS PRIMEROS 3 PUESTOS
        item {
            PodiumSection(topUsers = allRankings.take(3), isDark = isDark)
        }

        // 4. BARRA CON TÍTULO Y BOTÓN PARA AGREGAR AMIGO POR #CÓDIGO DE 3 DÍGITOS
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tabla de Posiciones (${allRankings.size})",
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        color = if (isDark) TextLight else TextDark
                    )
                    Text(
                        text = "Agrega amigos con su # de 3 dígitos (ej. #555, #888)",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFF94A3B8) else TextGrayLight
                    )
                }

                Button(
                    onClick = { showAddFriendDialog = true },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal),
                    shape = RoundedCornerShape(12.dp),
                    contentPadding = PaddingValues(horizontal = 12.dp, vertical = 8.dp)
                ) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, modifier = Modifier.size(16.dp))
                    Spacer(modifier = Modifier.width(6.dp))
                    Text("Agregar por #Código", fontSize = 12.sp, fontWeight = FontWeight.Bold)
                }
            }
        }

        // 5. LISTADO DE USUARIOS EN EL RANKING
        items(allRankings, key = { it.id }) { friend ->
            RankingItemRow(friend = friend, isDark = isDark)
        }
    }

    // Diálogo para agregar amigo por código # de 3 dígitos (100..999) o token compartido
    if (showAddFriendDialog) {
        val cleanDigits = friendCodeInput.filter { it.isDigit() }.take(3)
        val previewProfile = remember(cleanDigits) {
            if (cleanDigits.length == 3) {
                cloudSync.resolveProfileByTag(cleanDigits)
            } else null
        }

        AlertDialog(
            onDismissRequest = { showAddFriendDialog = false },
            title = {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(imageVector = Icons.Default.PersonAdd, contentDescription = null, tint = PrimaryTeal)
                    Spacer(modifier = Modifier.width(8.dp))
                    Text("Agregar Amigo por #Código", fontWeight = FontWeight.Bold, fontSize = 17.sp)
                }
            },
            text = {
                Column(verticalArrangement = Arrangement.spacedBy(10.dp)) {
                    Text(
                        text = "Ingresa el código # de 3 dígitos de tu amigo (ej. #555, #888, #333 o cualquier número de 3 dígitos) para sincronizarlo en tu tabla:",
                        fontSize = 12.sp,
                        lineHeight = 16.sp
                    )

                    OutlinedTextField(
                        value = friendCodeInput,
                        onValueChange = { friendCodeInput = it },
                        label = { Text("Código # de 3 dígitos (ej. 555 o #888)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    // Sugerencias rápidas de códigos activos para probar con un toque
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(6.dp)
                    ) {
                        listOf("555", "888", "333", "999").forEach { sampleTag ->
                            Surface(
                                onClick = { friendCodeInput = sampleTag },
                                shape = RoundedCornerShape(8.dp),
                                color = PrimaryTeal.copy(alpha = 0.14f)
                            ) {
                                Text(
                                    text = "#$sampleTag",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = PrimaryTeal,
                                    modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                                )
                            }
                        }
                    }

                    // Vista previa instantánea cuando se detectan 3 dígitos
                    if (previewProfile != null) {
                        Card(
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = CardDefaults.cardColors(
                                containerColor = PrimaryTeal.copy(alpha = 0.12f)
                            )
                        ) {
                            Row(
                                modifier = Modifier.padding(10.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Text(text = previewProfile.petEmoji, fontSize = 24.sp)
                                Spacer(modifier = Modifier.width(8.dp))
                                Column {
                                    Text(
                                        text = "Encontrado: ${previewProfile.displayNameWithTag}",
                                        fontWeight = FontWeight.Bold,
                                        fontSize = 12.sp
                                    )
                                    Text(
                                        text = "${previewProfile.score} pts • ${previewProfile.completedHabits} hábitos • 🔥 ${previewProfile.currentStreak}d racha",
                                        fontSize = 11.sp,
                                        color = PrimaryTeal
                                    )
                                }
                            }
                        }
                    }

                    OutlinedTextField(
                        value = newFriendName,
                        onValueChange = { newFriendName = it },
                        label = { Text("Nombre personalizado (opcional)") },
                        singleLine = true,
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(horizontalArrangement = Arrangement.spacedBy(8.dp)) {
                        OutlinedTextField(
                            value = newFriendStreak,
                            onValueChange = { newFriendStreak = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Racha (opc.)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                        OutlinedTextField(
                            value = newFriendHabits,
                            onValueChange = { newFriendHabits = it.filter { ch -> ch.isDigit() } },
                            label = { Text("Hábitos (opc.)") },
                            singleLine = true,
                            modifier = Modifier.weight(1f)
                        )
                    }
                }
            },
            confirmButton = {
                Button(
                    onClick = {
                        val codeToUse = friendCodeInput.trim().ifBlank { CloudSyncManager.generateRandomTag() }
                        val customName = newFriendName.trim().takeIf { it.isNotEmpty() }
                        val customStreak = newFriendStreak.toIntOrNull()
                        val customHabits = newFriendHabits.toIntOrNull()

                        cloudSync.addFriendByCodeAsync(
                            rawCodeOrToken = codeToUse,
                            optionalCustomName = customName,
                            optionalStreak = customStreak,
                            optionalHabits = customHabits
                        ) { result ->
                            result.onSuccess { added ->
                                onAddFriendByCode(added.normalizedTag, added.displayName, added.currentStreak, added.completedHabits)
                                Toast.makeText(
                                    context,
                                    "¡${added.displayNameWithTag} vinculado al Ranking!",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }.onFailure { err ->
                                Toast.makeText(
                                    context,
                                    err.message ?: "No se pudo agregar el código",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        }

                        friendCodeInput = ""
                        newFriendName = ""
                        newFriendStreak = ""
                        newFriendHabits = ""
                        showAddFriendDialog = false
                    },
                    colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
                ) {
                    Text("Vincular #Amigo", color = Color.White, fontWeight = FontWeight.Bold)
                }
            },
            dismissButton = {
                TextButton(onClick = { showAddFriendDialog = false }) {
                    Text("Cancelar")
                }
            }
        )
    }
}

/**
 * Podio gráfico para los 3 primeros lugares del ranking.
 */
@Composable
fun PodiumSection(topUsers: List<RankingUser>, isDark: Boolean) {
    if (topUsers.size < 3) return

    val first = topUsers.getOrNull(0) ?: return
    val second = topUsers.getOrNull(1) ?: return
    val third = topUsers.getOrNull(2) ?: return

    Row(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 12.dp),
        horizontalArrangement = Arrangement.SpaceEvenly,
        verticalAlignment = Alignment.Bottom
    ) {
        PodiumPillar(user = second, position = 2, height = 110, color = Color(0xFF90A4AE), isDark = isDark)
        PodiumPillar(user = first, position = 1, height = 140, color = Color(0xFFFFD54F), isDark = isDark)
        PodiumPillar(user = third, position = 3, height = 90, color = Color(0xFFFFB74D), isDark = isDark)
    }
}

@Composable
fun PodiumPillar(user: RankingUser, position: Int, height: Int, color: Color, isDark: Boolean) {
    Column(
        horizontalAlignment = Alignment.CenterHorizontally,
        modifier = Modifier.width(105.dp)
    ) {
        Box(
            modifier = Modifier
                .size(if (position == 1) 56.dp else 46.dp)
                .background(color.copy(alpha = 0.25f), CircleShape)
                .border(2.dp, color, CircleShape),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = user.name.firstOrNull()?.toString() ?: "?",
                fontWeight = FontWeight.Bold,
                fontSize = if (position == 1) 22.sp else 18.sp,
                color = if (isDark) TextLight else TextDark
            )
        }

        Spacer(modifier = Modifier.height(6.dp))

        Text(
            text = user.name.split(" ").firstOrNull() ?: "",
            fontWeight = FontWeight.Bold,
            fontSize = 12.sp,
            color = if (isDark) TextLight else TextDark,
            maxLines = 1,
            overflow = TextOverflow.Ellipsis
        )

        Text(
            text = "${user.score} pts",
            fontSize = 11.sp,
            fontWeight = FontWeight.SemiBold,
            color = PrimaryTeal
        )

        Spacer(modifier = Modifier.height(6.dp))

        Box(
            modifier = Modifier
                .width(80.dp)
                .height(height.dp)
                .background(
                    Brush.verticalGradient(
                        listOf(color, color.copy(alpha = 0.65f))
                    ),
                    shape = RoundedCornerShape(topStart = 12.dp, topEnd = 12.dp)
                ),
            contentAlignment = Alignment.Center
        ) {
            Text(
                text = "$position",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 28.sp,
                color = Color.White
            )
        }
    }
}

/**
 * Fila para cada usuario en la lista de posiciones.
 */
@Composable
fun RankingItemRow(friend: RankingUser, isDark: Boolean) {
    val isCurrentUser = friend.isCurrentUser
    val rowBg = when {
        isCurrentUser -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F2F1)
        else -> if (isDark) Color(0xFF1E293B) else Color(0xFFFFFFFF)
    }

    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(14.dp),
        colors = CardDefaults.cardColors(containerColor = rowBg),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isCurrentUser) 3.dp else 1.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = "#${friend.rankPosition}",
                fontWeight = FontWeight.ExtraBold,
                fontSize = 16.sp,
                color = when (friend.rankPosition) {
                    1 -> Color(0xFFFFB300)
                    2 -> Color(0xFF78909C)
                    3 -> Color(0xFFD84315)
                    else -> if (isDark) Color(0xFF94A3B8) else Color(0xFF64748B)
                },
                modifier = Modifier.width(36.dp)
            )

            Box(
                modifier = Modifier
                    .size(40.dp)
                    .background(PrimaryTeal.copy(alpha = 0.15f), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Text(
                    text = friend.name.firstOrNull()?.toString() ?: "U",
                    fontWeight = FontWeight.Bold,
                    color = PrimaryTeal
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            Column(modifier = Modifier.weight(1f)) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Text(
                        text = if (isCurrentUser) "${friend.name} (Tú)" else friend.name,
                        fontWeight = if (isCurrentUser) FontWeight.ExtraBold else FontWeight.SemiBold,
                        fontSize = 14.sp,
                        color = if (isDark) TextLight else TextDark,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis
                    )
                    if (friend.badge != null) {
                        Spacer(modifier = Modifier.width(4.dp))
                        Text(friend.badge, fontSize = 12.sp)
                    }
                }
                Spacer(modifier = Modifier.height(2.dp))
                Row(verticalAlignment = Alignment.CenterVertically) {
                    Icon(
                        imageVector = Icons.Default.LocalFireDepartment,
                        contentDescription = "Racha",
                        tint = Color(0xFFFF6D00),
                        modifier = Modifier.size(13.dp)
                    )
                    Spacer(modifier = Modifier.width(2.dp))
                    Text(
                        text = "${friend.currentStreak} días",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else TextGrayLight
                    )
                    Spacer(modifier = Modifier.width(8.dp))
                    Text(
                        text = "•  ${friend.completedHabits} hábitos",
                        fontSize = 11.sp,
                        color = if (isDark) Color(0xFFCBD5E1) else TextGrayLight
                    )
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = "${friend.score}",
                    fontWeight = FontWeight.ExtraBold,
                    fontSize = 16.sp,
                    color = PrimaryTeal
                )
                Text(
                    text = "puntos",
                    fontSize = 10.sp,
                    color = if (isDark) Color(0xFF94A3B8) else TextGrayLight
                )
            }
        }
    }
}
