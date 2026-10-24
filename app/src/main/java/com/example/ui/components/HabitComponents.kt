package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.outlined.Add
import androidx.compose.material.icons.outlined.Check
import androidx.compose.material.icons.outlined.CheckCircle
import androidx.compose.material.icons.outlined.DeleteOutline
import androidx.compose.material.icons.outlined.Edit
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.FlashOn
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material.icons.automirrored.outlined.MenuBook
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.StrokeCap
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import com.example.R
import com.example.model.HabitHistoryItem
import com.example.model.HabitItem
import com.example.model.DayProgress
import com.example.model.HabitDates
import androidx.compose.foundation.layout.FlowRow
import androidx.compose.foundation.layout.ExperimentalLayoutApi
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.BorderGrayLight
import com.example.ui.theme.BorderInactiveDark
import com.example.ui.theme.BorderInactiveLight
import com.example.ui.theme.BorderTrackDark
import com.example.ui.theme.CardAccuracyDark
import com.example.ui.theme.CardAccuracyLight
import com.example.ui.theme.CardStreakDark
import com.example.ui.theme.CardStreakLight
import com.example.ui.theme.CategoryArtDark
import com.example.ui.theme.CategoryArtLight
import com.example.ui.theme.CategoryFinanceDark
import com.example.ui.theme.CategoryFinanceLight
import com.example.ui.theme.CategoryFitnessDark
import com.example.ui.theme.CategoryFitnessLight
import com.example.ui.theme.CategoryHydrationDark
import com.example.ui.theme.CategoryHydrationLight
import com.example.ui.theme.CategoryLearningDark
import com.example.ui.theme.CategoryLearningLight
import com.example.ui.theme.CategoryMentalDark
import com.example.ui.theme.CategoryMentalLight
import com.example.ui.theme.CategoryMindfulnessDark
import com.example.ui.theme.CategoryMindfulnessLight
import com.example.ui.theme.CategoryNutritionDark
import com.example.ui.theme.CategoryNutritionLight
import com.example.ui.theme.CategoryProductivityDark
import com.example.ui.theme.CategoryProductivityLight
import com.example.ui.theme.CategorySleepDark
import com.example.ui.theme.CategorySleepLight
import com.example.ui.theme.ChipBackgroundDark
import com.example.ui.theme.ChipBackgroundLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.LightTealBg
import com.example.ui.theme.NumberAccuracyDark
import com.example.ui.theme.NumberAccuracyLight
import com.example.ui.theme.NumberStreakDark
import com.example.ui.theme.NumberStreakLight
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SubtitleAccuracyDark
import com.example.ui.theme.SubtitleAccuracyLight
import com.example.ui.theme.SubtitleStreakDark
import com.example.ui.theme.SubtitleStreakLight
import com.example.ui.theme.TextGrayDark
import com.example.ui.theme.TextGrayLight
import com.example.ui.theme.TimerOrange

// ============================================================================
// VALORES DE COLOR DINÁMICOS COMPOSABLES (Para Soporte de Modo Oscuro Premium)
// ============================================================================

val TextGray: Color
    @Composable
    get() = if (isAppInDarkTheme) TextGrayDark else TextGrayLight

val ScreenBackground: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

val CardWhite: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val TextDark: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

val CardStreakBackground: Color
    @Composable
    get() = if (isAppInDarkTheme) CardStreakDark else CardStreakLight

val NumberStreakColor: Color
    @Composable
    get() = if (isAppInDarkTheme) NumberStreakDark else NumberStreakLight

val SubtitleStreakColor: Color
    @Composable
    get() = if (isAppInDarkTheme) SubtitleStreakDark else SubtitleStreakLight

val CardAccuracyBackground: Color
    @Composable
    get() = if (isAppInDarkTheme) CardAccuracyDark else CardAccuracyLight

val NumberAccuracyColor: Color
    @Composable
    get() = if (isAppInDarkTheme) NumberAccuracyDark else NumberAccuracyLight

val SubtitleAccuracyColor: Color
    @Composable
    get() = if (isAppInDarkTheme) SubtitleAccuracyDark else SubtitleAccuracyLight

// ============================================================================
// FUNCIONES DE UTILIDAD PARA HÁBITOS
// ============================================================================

/**
 * getCategoryIcon devuelve el ícono vectorial adecuado según el nombre de categoría ingresado.
 *
 * - **¿Para qué sirve?** Asocia visualmente cada categoría con su correspondiente icono semántico de Material Design.
 * - **¿Cómo funciona?** Evalúa la cadena de texto de la categoría ignorando mayúsculas y minúsculas.
 * - **¿Qué pasa si lo quitamos?** Todos los hábitos mostrarían el mismo icono genérico sin distinción gráfica.
 */
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "hidratación", "hydration" -> Icons.Outlined.WaterDrop
        "aprendizaje", "learning" -> Icons.AutoMirrored.Outlined.MenuBook
        "ejercicio", "fitness" -> Icons.Outlined.FitnessCenter
        "mindfulness" -> Icons.Outlined.SelfImprovement
        "productividad", "productivity" -> Icons.Outlined.WorkOutline
        "nutrición", "nutrition" -> Icons.Outlined.Restaurant
        "sueño", "sleep" -> Icons.Outlined.NightsStay
        "arte y creatividad", "art" -> Icons.Outlined.Palette
        "finanzas", "finance" -> Icons.Outlined.Payments
        "salud mental" -> Icons.Outlined.Psychology
        else -> Icons.Outlined.CheckCircle
    }
}

/**
 * getCategoryBgColor devuelve el color de fondo adaptado para la burbuja de la categoría.
 *
 * - **¿Para qué sirve?** Proporciona colores distintivos para cada categoría en modo diurno y nocturno.
 * - **¿Cómo funciona?** Utiliza las constantes centralizadas de [Color.kt] reaccionando a `isSystemInDarkTheme()`.
 * - **¿Qué pasa si lo quitamos?** Las tarjetas de categorías perderían su identificación visual cromática.
 */
@Composable
fun getCategoryBgColor(category: String): Color {
    val isDark = isAppInDarkTheme
    return when (category.lowercase()) {
        "hidratación", "hydration" -> if (isDark) CategoryHydrationDark else CategoryHydrationLight
        "aprendizaje", "learning" -> if (isDark) CategoryLearningDark else CategoryLearningLight
        "ejercicio", "fitness" -> if (isDark) CategoryFitnessDark else CategoryFitnessLight
        "mindfulness" -> if (isDark) CategoryMindfulnessDark else CategoryMindfulnessLight
        "productividad", "productivity" -> if (isDark) CategoryProductivityDark else CategoryProductivityLight
        "nutrición", "nutrition" -> if (isDark) CategoryNutritionDark else CategoryNutritionLight
        "sueño", "sleep" -> if (isDark) CategorySleepDark else CategorySleepLight
        "arte y creatividad", "art" -> if (isDark) CategoryArtDark else CategoryArtLight
        "finanzas", "finance" -> if (isDark) CategoryFinanceDark else CategoryFinanceLight
        "salud mental" -> if (isDark) CategoryMentalDark else CategoryMentalLight
        else -> if (isDark) CategoryHydrationDark else CategoryHydrationLight
    }
}

// ============================================================================
// COMPONENTE DE ÍTEM DE HÁBITO: HabitItemCard
// ============================================================================

/**
 * HabitItemCard es la tarjeta individual para cada hábito o micro hábito en la lista del Rastreador.
 *
 * - **¿Para qué sirve?** Muestra el estado del hábito, etiquetas, controles de completado y botones de edición/borrado.
 * - **¿Cómo funciona?**
 *   1. Dibuja el icono de la categoría (o el rayo distintivo si es un micro hábito).
 *   2. Despliega título, subtítulo descriptivo y chips de etiquetas.
 *   3. En modo lectura, renderiza barra de progreso, botón temporizador o checkbox interactivo.
 *   4. En modo edición (`isEditMode`), expone los botones de modificar y eliminar.
 * - **¿Qué pasa si lo quitamos?** No se podrían visualizar ni manipular los hábitos en el dashboard principal.
 */
@OptIn(ExperimentalLayoutApi::class)
@Composable
fun HabitItemCard(
    habit: HabitItem,
    onToggleComplete: (HabitItem) -> Unit,
    onClickDetail: (HabitItem) -> Unit,
    onEditHabit: ((HabitItem) -> Unit)? = null,
    onDeleteHabit: ((HabitItem) -> Unit)? = null,
    isEditMode: Boolean = false,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isAppInDarkTheme

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                if (isEditMode && onEditHabit != null) {
                    onEditHabit(habit)
                } else {
                    onClickDetail(habit)
                }
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(14.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Contenedor circular con el icono de categoría o rayo para micro hábitos
            Box(
                modifier = Modifier
                    .size(46.dp)
                    .background(getCategoryBgColor(habit.category), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = if (habit.isMicroHabit) Icons.Outlined.FlashOn else getCategoryIcon(habit.category),
                    contentDescription = habit.category,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(12.dp))

            // Textos informativos y etiquetas del hábito
            Column(
                modifier = Modifier.weight(1f)
            ) {
                FlowRow(
                    verticalArrangement = Arrangement.spacedBy(4.dp),
                    horizontalArrangement = Arrangement.spacedBy(6.dp)
                ) {
                    Text(
                        text = habit.title,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    // Insignia distintiva para micro hábitos de menos de 2 minutos
                    if (habit.isMicroHabit) {
                        Box(
                            modifier = Modifier
                                .background(SecondaryTeal.copy(alpha = 0.2f), RoundedCornerShape(6.dp))
                                .padding(horizontal = 6.dp, vertical = 2.dp)
                        ) {
                            Text(
                                text = "MICRO",
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.primary
                            )
                        }
                    }
                }

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = "${if (habit.isCompleted) "Completado" else if (habit.currentProgress > 0) "En progreso" else "Pendiente"} · Racha: ${habit.streak} ${if (habit.frequency == "Semanal") "sem." else "días"}",
                    fontSize = 12.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(6.dp))

                // Fila de etiquetas (Chips) del hábito
                FlowRow(
                    horizontalArrangement = Arrangement.spacedBy(6.dp),
                    verticalArrangement = Arrangement.spacedBy(4.dp)
                ) {
                    habit.tags.take(3).forEach { tag ->
                        val chipBg = if (isDark) ChipBackgroundDark else ChipBackgroundLight
                        val chipTextColor = if (isDark) Color.White else TextDark

                        Box(
                            modifier = Modifier
                                .background(chipBg, RoundedCornerShape(10.dp))
                                .padding(horizontal = 8.dp, vertical = 3.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 10.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Modo edición: Botones de editar y borrar
            if (isEditMode) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    if (onEditHabit != null) {
                        IconButton(
                            onClick = { onEditHabit(habit) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.Edit,
                                contentDescription = "Editar",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                    if (onDeleteHabit != null) {
                        IconButton(
                            onClick = { onDeleteHabit(habit) },
                            modifier = Modifier.size(36.dp)
                        ) {
                            Icon(
                                imageVector = Icons.Outlined.DeleteOutline,
                                contentDescription = "Eliminar",
                                tint = DangerRed,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            } else {
                HabitCompletionControl(habit = habit, onClick = { onToggleComplete(habit) })
            }
        }
    }
}

// ============================================================================
// COMPONENTE DE ÍTEM DE HISTORIAL: HabitHistoryItemRow
// ============================================================================

/**
 * HabitHistoryItemRow representa cada fila de historial de cumplimiento.
 *
 * - **¿Para qué sirve?** Muestra el registro histórico de un hábito completado con su fecha correspondiente.
 * - **¿Cómo funciona?** Dibuja una fila horizontal con check verde o gris adaptable y etiqueta temporal.
 * - **¿Qué pasa si lo quitamos?** La sección inferior de historial no tendría filas de datos renderizables.
 */
@Composable
fun HabitHistoryItemRow(
    item: HabitHistoryItem,
    onClick: () -> Unit,
    modifier: Modifier = Modifier
) {
    Row(
        modifier = modifier.fillMaxWidth().clickable(onClick = onClick).padding(12.dp),
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        HabitStatusIcon(item.isCompleted, item.isProgressive, item.timeOrDuration, item.statusText)
        Column(modifier = Modifier.weight(1f)) {
            Text(item.habitTitle, fontSize = 15.sp, fontWeight = FontWeight.Medium, color = TextDark)
            Text(item.dateText, fontSize = 12.sp, color = TextGray)
            Text(listOf(item.statusText, item.progressText).filter { it.isNotBlank() }.joinToString(" · "),
                fontSize = 12.sp, color = if (item.isCompleted) MaterialTheme.colorScheme.primary else TextGray)
        }
    }
}

// ============================================================================
// COMPONENTE BANNER DE MASCOTA: MascotBannerCard
// ============================================================================

/**
 * MascotBannerCard muestra a la mascota Octo felicitando al usuario por su progreso.
 *
 * - **¿Para qué sirve?** Gamifica la experiencia y motiva la constancia con refuerzo positivo.
 * - **¿Cómo funciona?** Renderiza la ilustración de Octo desde GitHub usando Coil y un botón de interacción.
 * - **¿Qué pasa si lo quitamos?** Se perdería el componente de mascota que refuerza la identidad de HabitOcto.
 */
@Composable
fun MascotBannerCard(
    onVisitMascotClick: () -> Unit,
    streak: Int = 0
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        HabitImage(
            model = "https://raw.githubusercontent.com/garzzaro/Proyecto-Plataformas-Moviles/feature/lab2-lazy-list/app/src/main/res/drawable/pulpo.avif",
            contentDescription = "Pulpo HabitOcto",
            modifier = Modifier
                .size(110.dp)
                .clip(CircleShape),
            contentScale = ContentScale.Crop
        )

        Spacer(modifier = Modifier.height(10.dp))

        Text(
            text = "¡Progreso Increíble!",
            fontSize = 22.sp,
            fontWeight = FontWeight.Bold,
            color = MaterialTheme.colorScheme.primary
        )

        Spacer(modifier = Modifier.height(4.dp))

        Text(
            text = if (streak > 0) "Llevas $streak días de racha completando microhábitos. ¡Sigue así!" else "Completa un microhábito hoy para empezar tu racha con Octo.",
            fontSize = 14.sp,
            color = TextDark,
            textAlign = TextAlign.Center,
            modifier = Modifier.padding(horizontal = 32.dp)
        )

        Spacer(modifier = Modifier.height(12.dp))

        Button(
            onClick = onVisitMascotClick,
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
            shape = RoundedCornerShape(24.dp),
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp)
        ) {
            Text(
                text = "Visitar Mascota",
                color = TextDark,
                fontWeight = FontWeight.Bold,
                fontSize = 14.sp,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp)
            )
        }
    }
}

// ============================================================================
// BOTÓN DE NUEVO HÁBITO: NewHabitButtonCard
// ============================================================================

/**
 * NewHabitButtonCard es el botón interactivo para abrir el formulario de hábitos.
 *
 * - **¿Para qué sirve?** Brinda un acceso visible y directo para crear un hábito o micro hábito.
 * - **¿Cómo funciona?** Tarjeta clicable con icono de suma en verde corporativo y texto claro.
 * - **¿Qué pasa si lo quitamos?** El usuario no tendría el botón principal para agregar nuevos hábitos en la pantalla.
 */
@Composable
fun NewHabitButtonCard(onClick: () -> Unit) {
    val isDark = isAppInDarkTheme
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) BorderGrayDark else BorderGrayLight),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(vertical = 16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Box(
                modifier = Modifier
                    .size(44.dp)
                    .background(MaterialTheme.colorScheme.primary, CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Add,
                    contentDescription = "Añadir Hábito",
                    tint = Color.White,
                    modifier = Modifier.size(24.dp)
                )
            }
            Spacer(modifier = Modifier.height(8.dp))
            Text(
                text = "Nuevo Hábito / Micro Hábito",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}

// ============================================================================
// PROGRESO SEMANAL: WeeklyProgressCard
// ============================================================================

/**
 * WeeklyProgressCard muestra el avance persistido de los últimos siete días.
 *
 * - **¿Para qué sirve?** Ofrece una vista rápida de la consistencia semanal del usuario.
 * - **¿Cómo funciona?** Recibe el resumen reconstruido desde Room para los hábitos activos;
 *   muestra completados/programados por fecha y un check al completar todas las metas del día.
 * - **¿Qué pasa si lo quitamos?** Se eliminaría el indicador semanal resumido del dashboard.
 */
@Composable
fun WeeklyProgressCard(progress: List<DayProgress> = emptyList()) {
    val isDark = isAppInDarkTheme
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Text(
                text = "Progreso Semanal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            val days = progress.map { HabitDates.label(it.day, "EEE") }
            val completed = progress.map { it.scheduled > 0 && it.completed == it.scheduled }

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { index, day ->
                    Column(horizontalAlignment = Alignment.CenterHorizontally) {
                        Text(
                            text = day,
                            fontSize = 12.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextGray
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        Box(
                            modifier = Modifier
                                .size(32.dp)
                                .background(
                                    color = if (completed[index]) MaterialTheme.colorScheme.primary else if (isDark) BorderGrayDark else BorderGrayLight,
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (completed[index]) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = "Todos los hábitos del día completados",
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                        Text("${progress[index].completed}/${progress[index].scheduled}", fontSize = 10.sp, color = TextGray)
                    }
                }
            }
        }
    }
}
