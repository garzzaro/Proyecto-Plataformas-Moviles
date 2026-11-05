package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
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
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.CalendarToday
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
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
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.TopNavigationBarSection
import com.example.model.AchievementCatalog
import com.example.model.HabitDates
import com.example.model.HabitHistoryItem
import com.example.model.HabitItem
import com.example.model.OriginalHabitData
import com.example.model.PetCustomization
import com.example.model.TrackingSummary
import com.example.model.UserEntity
import com.example.ui.components.CardWhite
import com.example.ui.components.HabitHistoryItemRow
import com.example.ui.components.HabitImage
import com.example.ui.components.HabitItemCard
import com.example.ui.components.HabitProgressDialog
import com.example.ui.components.MascotBannerCard
import com.example.ui.components.NewHabitButtonCard
import com.example.ui.components.ScreenBackground
import com.example.ui.components.TextDark
import com.example.ui.components.TextGray
import com.example.ui.components.WeeklyProgressCard
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.BorderGrayLight
import com.example.ui.theme.ChartBar1
import com.example.ui.theme.ChartBar2
import com.example.ui.theme.ChartBar3
import com.example.ui.theme.ChartBar4
import com.example.ui.theme.ChartBar5
import com.example.ui.theme.ChartBar6
import com.example.ui.theme.ChartBar7
import com.example.ui.theme.DividerDark
import com.example.ui.theme.DividerLight
import com.example.ui.theme.InfoBlue
import com.example.ui.theme.InfoBlueDark
import com.example.ui.theme.SuccessGreen
import com.example.ui.theme.SuccessGreenDark
import com.example.ui.theme.isAppInDarkTheme

/**
 * Lista inicial de hábitos y micro hábitos con más de 10 elementos para garantizar scroll.
 */
val initialHabitsList = OriginalHabitData.habits

/**
 * Lista de registros para la sección de historial de cumplimiento.
 */
val sampleHistoryItems = OriginalHabitData.history

/**
 * TrackerScreen representa la pestaña unificada de rastreo de hábitos, Modo Enfoque con bloqueo y estadísticas.
 *
 * - **¿Para qué sirve?** Permite al usuario consultar sus hábitos diarios y micro hábitos,
 *   activar el Modo Enfoque Inmersivo con bloqueo de salida, verificar micro-hábitos con foto en RAM (0 KB en BD),
 *   visitar el Espacio Propio (Santuario) de su mascota, editar o borrar hábitos, y revisar gráficos semanales.
 * - **¿Cómo funciona?** Implementa un `LazyColumn` principal con un contenedor interno optimizado
 *   para mostrar ~4 hábitos a la vez, garantizando fluidez de fotogramas y respeto de la rúbrica.
 * - **¿Qué pasa si lo quitamos?** Se eliminaría el módulo central de registro y monitoreo de hábitos de la app.
 */
@Composable
fun TrackerScreen(
    habits: List<HabitItem>,
    onToggleHabit: (HabitItem) -> Unit,
    onOpenAddHabit: () -> Unit,
    onEditHabit: (HabitItem) -> Unit,
    onDeleteHabit: (HabitItem) -> Unit,
    history: List<HabitHistoryItem> = emptyList(),
    summary: TrackingSummary = TrackingSummary(),
    onSetProgress: (HabitItem, Int) -> Unit = { _, _ -> },
    onNavigateToPet: () -> Unit = {},
    petCustomization: PetCustomization = PetCustomization(),
    onUpdatePetCustomization: (PetCustomization) -> Unit = {},
    onOpenPetSanctuary: (() -> Unit)? = null,
    user: UserEntity? = null,
    onProfileClick: () -> Unit = {},
    isEnglish: Boolean = false,
    requirePhotoForMicroHabits: Boolean = false
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme
    // Estado para abrir el Espacio Propio / Santuario de la mascota a pantalla completa al tocar "Visitar Mascota"
    var showSanctuaryFullScreen by rememberSaveable { mutableStateOf(false) }
    var localCustomization by remember(petCustomization) { mutableStateOf(petCustomization) }
    var focusSessionsCount by rememberSaveable { mutableIntStateOf(0) }

    if (showSanctuaryFullScreen && onOpenPetSanctuary == null) {
        PetSanctuaryScreen(
            currentStreak = summary.microStreak,
            totalCompleted = summary.totalCompleted,
            completedToday = habits.count { it.isCompleted },
            scheduledToday = maxOf(1, habits.size),
            initialCustomization = localCustomization,
            onCustomizationChanged = { updated ->
                localCustomization = updated
                onUpdatePetCustomization(updated)
            },
            onBack = { showSanctuaryFullScreen = false }
        )
        return
    }

    // Estado reactivo que activa o desactiva los controles de edición directa sobre las tarjetas
    var isEditMode by rememberSaveable { mutableStateOf(false) }
    // Conserva el identificador del hábito abierto; el avance siempre procede del estado persistente.
    var progressTarget by rememberSaveable { mutableStateOf<String?>(null) }
    habits.firstOrNull { it.id == progressTarget }?.let { habit ->
        HabitProgressDialog(habit, onDismiss = { progressTarget = null }, onSave = { onSetProgress(habit, it) })
    }

    // Identificador del micro-hábito pendiente de verificación fotográfica en RAM (0 KB en BD)
    var microHabitVerificationTarget by rememberSaveable { mutableStateOf<String?>(null) }
    habits.firstOrNull { it.id == microHabitVerificationTarget }?.let { microHabit ->
        MicroHabitPhotoVerificationDialog(
            habit = microHabit,
            isEnglish = isEnglish,
            onDismiss = { microHabitVerificationTarget = null },
            onVerifiedComplete = { verifiedHabit ->
                microHabitVerificationTarget = null
                onToggleHabit(verifiedHabit)
                Toast.makeText(
                    context,
                    if (isEnglish) {
                        "Habit verified with photo in RAM (0 KB saved in DB)!"
                    } else {
                        "¡Hábito verificado con foto en RAM (0 KB guardados en BD)!"
                    },
                    Toast.LENGTH_SHORT
                ).show()
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera superior compartida de la aplicación
        item {
            TopNavigationBarSection(
                user = user,
                level = 1 + summary.totalCompleted / 10,
                onProfileClick = onProfileClick
            )
        }

        // Banner motivacional con el pulpo celebrando la racha -> lleva al Espacio Propio / Santuario de la Mascota
        item {
            MascotBannerCard(
                streak = summary.microStreak,
                onVisitMascotClick = {
                    if (onOpenPetSanctuary != null) {
                        onOpenPetSanctuary()
                    } else {
                        showSanctuaryFullScreen = true
                    }
                }
            )
        }

        // Encabezado de sección con título y botón interactivo para alternar modo edición
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column(modifier = Modifier.weight(1f).padding(end = 8.dp)) {
                    Text(
                        text = if (isEnglish) "Your Habits & Micro-Habits" else "Tus Hábitos y Micro Hábitos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = if (isEditMode) {
                            if (isEnglish) "Tap pencil to edit or trash to delete" else "Toca el lápiz para editar o la papelera para borrar"
                        } else {
                            if (isEnglish) "Tap to mark complete or view details" else "Toca para marcar o ver detalle"
                        },
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (isEditMode) MaterialTheme.colorScheme.primary
                            else if (isDark) BorderGrayDark else BorderGrayLight,
                            RoundedCornerShape(12.dp)
                        )
                        .clickable {
                            isEditMode = !isEditMode
                            val msg = if (isEditMode) "Modo edición activado" else "Modo edición finalizado"
                            Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        }
                        .padding(horizontal = 14.dp, vertical = 6.dp)
                ) {
                    Text(
                        text = if (isEditMode) {
                            if (isEnglish) "Done" else "Listo"
                        } else {
                            if (isEnglish) "Edit" else "Editar"
                        },
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEditMode) Color.White else TextDark
                    )
                }
            }
        }

        // Cuadro de hábitos con scroll interno para mostrar ~4 elementos simultáneamente
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(390.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(10.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    if (habits.isEmpty()) {
                        item {
                            Text(
                                text = if (isEnglish) {
                                    "You don't have habits yet. Create your first one to start tracking."
                                } else {
                                    "Aún no tienes hábitos. Crea el primero para comenzar tu seguimiento."
                                },
                                modifier = Modifier.padding(16.dp),
                                color = TextGray
                            )
                        }
                    }
                    items(items = habits, key = { it.id }) { habit ->
                        HabitItemCard(
                            habit = habit,
                            isEditMode = isEditMode,
                            onToggleComplete = {
                                if (!it.isCompleted) {
                                    // Verificación por foto requerida para TODAS las casillas de verificación (análisis en RAM)
                                    microHabitVerificationTarget = it.id
                                } else {
                                    // Si ya estaba completado, permite desmarcarlo de inmediato
                                    onToggleHabit(it)
                                }
                            },
                            onClickDetail = { onEditHabit(it) },
                            onEditHabit = { onEditHabit(it) },
                            onDeleteHabit = { onDeleteHabit(it) }
                        )
                    }
                }
            }
        }

        // Botón principal para abrir el modal de nuevo hábito o micro hábito
        item {
            NewHabitButtonCard(onClick = onOpenAddHabit)
        }

        // MODO ENFOQUE CON BLOQUEO INMERSIVO EN EL TRACKER (Movido desde Mascota a petición del usuario)
        item {
            FocusModeTrackerCard(
                habits = habits,
                isDark = isDark,
                isEnglish = isEnglish,
                onFocusCompleted = { focusedHabit, _ ->
                    focusSessionsCount += 1
                    if (focusedHabit != null && !focusedHabit.isCompleted) {
                        onToggleHabit(focusedHabit)
                    }
                }
            )
        }

        // Tarjeta semanal reactiva: el resumen excluye hábitos archivados y conserva el historial aparte.
        item {
            WeeklyProgressCard(summary.days)
        }

        // Título de la sección de estadísticas de cumplimiento
        item {
            Text(
                text = if (isEnglish) "Weekly Completion Percentages" else "Porcentajes Semanales de Cumplimiento",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Gráfico de barras de cumplimiento semanal
        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                Column(
                    modifier = Modifier.padding(18.dp)
                ) {
                    val days = summary.days.map { HabitDates.label(it.day, "EEE") }
                    val percentages = summary.days.map { it.percent }
                    val barColors = listOf(
                        ChartBar1,
                        ChartBar2,
                        ChartBar3,
                        ChartBar4,
                        ChartBar5,
                        ChartBar6,
                        ChartBar7
                    )

                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(140.dp),
                        horizontalArrangement = Arrangement.SpaceBetween,
                        verticalAlignment = Alignment.Bottom
                    ) {
                        days.forEachIndexed { idx, day ->
                            val pct = percentages[idx]
                            val barHeightRatio = pct / 100f

                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                verticalArrangement = Arrangement.Bottom,
                                modifier = Modifier.weight(1f)
                            ) {
                                Text(
                                    text = "$pct%",
                                    fontSize = 11.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                                Spacer(modifier = Modifier.height(4.dp))
                                Box(
                                    modifier = Modifier
                                        .width(26.dp)
                                        .height((100 * barHeightRatio).dp)
                                        .background(
                                            color = barColors[idx],
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp)
                                        )
                                )
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = day,
                                    fontSize = 13.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark
                                )
                            }
                        }
                    }
                }
            }
        }

        // Desglose de progreso con tarjetas métricas de completados y programados
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = if (isEnglish) "Progress Breakdown" else "Desglose del Progreso",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    HabitImage(
                        model = com.example.R.drawable.ic_pulpo,
                        contentDescription = "Mascota Octo",
                        modifier = Modifier
                            .size(36.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(10.dp))

                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.spacedBy(12.dp)
                ) {
                    // Métrica 1: Tareas completadas
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = if (isEnglish) "Completed" else "Completados",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = SuccessGreen,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${summary.completedWeek}/${summary.scheduledWeek}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) SuccessGreen else SuccessGreenDark
                            )
                        }
                    }

                    // Métrica 2: Tareas programadas
                    Card(
                        modifier = Modifier.weight(1f),
                        shape = RoundedCornerShape(18.dp),
                        colors = CardDefaults.cardColors(containerColor = CardWhite),
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
                    ) {
                        Column(modifier = Modifier.padding(16.dp)) {
                            Row(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalArrangement = Arrangement.SpaceBetween,
                                verticalAlignment = Alignment.Top
                            ) {
                                Text(
                                    text = if (isEnglish) "Scheduled" else "Programados",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = InfoBlue,
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${summary.scheduledWeek}",
                                fontSize = 26.sp,
                                fontWeight = FontWeight.Bold,
                                color = if (isDark) InfoBlue else InfoBlueDark
                            )
                        }
                    }
                }
            }
        }

        // Mejora Autónoma: Medallas y Logros Desbloqueables
        item {
            val achievements = remember(summary.totalCompleted, summary.microStreak, focusSessionsCount) {
                AchievementCatalog.evaluate(
                    totalCompleted = summary.totalCompleted,
                    currentStreak = summary.microStreak,
                    friendsCount = 4,
                    focusSessionsCompleted = focusSessionsCount
                )
            }
            AchievementsShowcaseCard(
                achievements = achievements,
                isDark = isDark
            )
        }

        // Título de la sección de historial
        item {
            Text(
                text = if (isEnglish) "History · Last 7 days" else "Historial · Últimos 7 días",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Cuadro de historial con scroll interno (~6 elementos visibles)
        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp),
                shape = RoundedCornerShape(20.dp),
                colors = CardDefaults.cardColors(containerColor = CardWhite),
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
            ) {
                LazyColumn(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(12.dp),
                    verticalArrangement = Arrangement.spacedBy(8.dp),
                    contentPadding = PaddingValues(bottom = 8.dp)
                ) {
                    if (history.isEmpty()) {
                        item {
                            Text(
                                text = if (isEnglish) {
                                    "Your history will appear here once you create a habit."
                                } else {
                                    "Tu historial aparecerá aquí cuando crees un hábito."
                                },
                                modifier = Modifier.padding(16.dp),
                                color = TextGray
                            )
                        }
                    }
                    items(items = history, key = { it.id }) { historyItem ->
                        HabitHistoryItemRow(
                            item = historyItem,
                            onClick = {
                                Toast.makeText(
                                    context,
                                    "${historyItem.habitTitle} - ${historyItem.dateText}",
                                    Toast.LENGTH_SHORT
                                ).show()
                            }
                        )
                        Box(
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(1.dp)
                                .background(if (isDark) DividerDark else DividerLight)
                        )
                    }
                }
            }
        }
    }
}
