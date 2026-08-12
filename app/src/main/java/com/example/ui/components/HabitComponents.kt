package com.example.ui.components

import android.widget.Toast
import androidx.compose.foundation.Image
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
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
import androidx.compose.material.icons.outlined.FitnessCenter
import androidx.compose.material.icons.outlined.MenuBook
import androidx.compose.material.icons.outlined.NightsStay
import androidx.compose.material.icons.outlined.Palette
import androidx.compose.material.icons.outlined.Payments
import androidx.compose.material.icons.outlined.Psychology
import androidx.compose.material.icons.outlined.Refresh
import androidx.compose.material.icons.outlined.Restaurant
import androidx.compose.material.icons.outlined.SelfImprovement
import androidx.compose.material.icons.outlined.Timer
import androidx.compose.material.icons.outlined.WaterDrop
import androidx.compose.material.icons.outlined.WorkOutline
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
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
import coil.compose.AsyncImage
import com.example.R
import com.example.model.HabitHistoryItem
import com.example.model.HabitItem
import com.example.ui.theme.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme

// Colors resolved dynamically per theme
val LightTealBg = Color(0xFFE0F7FA)

val TextGray: Color
    @Composable
    get() = if (isSystemInDarkTheme()) TextGrayDark else TextGrayLight

val ScreenBackground: Color
    @Composable
    get() = MaterialTheme.colorScheme.background

val CardWhite: Color
    @Composable
    get() = MaterialTheme.colorScheme.surface

val TextDark: Color
    @Composable
    get() = MaterialTheme.colorScheme.onSurface

// Card mappings from theme resolved dynamically
val CardStreakBackground: Color
    @Composable
    get() = if (isSystemInDarkTheme()) CardStreakDark else CardStreakLight

val NumberStreakColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) NumberStreakDark else NumberStreakLight

val SubtitleStreakColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) SubtitleStreakDark else SubtitleStreakLight

val CardAccuracyBackground: Color
    @Composable
    get() = if (isSystemInDarkTheme()) CardAccuracyDark else CardAccuracyLight

val NumberAccuracyColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) NumberAccuracyDark else NumberAccuracyLight

val SubtitleAccuracyColor: Color
    @Composable
    get() = if (isSystemInDarkTheme()) SubtitleAccuracyDark else SubtitleAccuracyLight

// Obtener ícono por nombre de categoría
fun getCategoryIcon(category: String): ImageVector {
    return when (category.lowercase()) {
        "hidratación", "hydration" -> Icons.Outlined.WaterDrop
        "aprendizaje", "learning" -> Icons.Outlined.MenuBook
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

// Obtener color de fondo por categoría adaptado para modo claro y oscuro
@Composable
fun getCategoryBgColor(category: String): Color {
    val isDark = isSystemInDarkTheme()
    return when (category.lowercase()) {
        "hidratación", "hydration" -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA)
        "aprendizaje", "learning" -> if (isDark) Color(0xFF311B92) else Color(0xFFF3E5F5)
        "ejercicio", "fitness" -> if (isDark) Color(0xFFE65100) else Color(0xFFFFE0B2)
        "mindfulness" -> if (isDark) Color(0xFF880E4F) else Color(0xFFF8BBD0)
        "productividad", "productivity" -> if (isDark) Color(0xFF1A237E) else Color(0xFFE8EAF6)
        "nutrición", "nutrition" -> if (isDark) Color(0xFF33691E) else Color(0xFFDCEDC8)
        "sueño", "sleep" -> if (isDark) Color(0xFF4A148C) else Color(0xFFE1BEE7)
        "arte y creatividad", "art" -> if (isDark) Color(0xFFF57F17) else Color(0xFFFFF9C4)
        "finanzas", "finance" -> if (isDark) Color(0xFF1B5E20) else Color(0xFFC8E6C9)
        "salud mental" -> if (isDark) Color(0xFF263238) else Color(0xFFCFD8DC)
        else -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA)
    }
}

// ============================================================================
// COMPONENTE DE ÍTEM DE HÁBITO: HabitItemCard
// ============================================================================
@Composable
fun HabitItemCard(
    habit: HabitItem,
    onToggleComplete: (HabitItem) -> Unit,
    onClickDetail: (HabitItem) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val isDark = isSystemInDarkTheme()

    Card(
        modifier = modifier
            .fillMaxWidth()
            .clickable {
                onClickDetail(habit)
                Toast.makeText(context, "Hábito: ${habit.title}", Toast.LENGTH_SHORT).show()
            },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Círculo con ícono de categoría
            Box(
                modifier = Modifier
                    .size(48.dp)
                    .background(getCategoryBgColor(habit.category), CircleShape),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = getCategoryIcon(habit.category),
                    contentDescription = habit.category,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.size(24.dp)
                )
            }

            Spacer(modifier = Modifier.width(14.dp))

            // Detalles del hábito
            Column(
                modifier = Modifier.weight(1f)
            ) {
                Text(
                    text = habit.title,
                    fontSize = 16.sp,
                    fontWeight = FontWeight.Bold,
                    color = TextDark
                )

                Spacer(modifier = Modifier.height(2.dp))

                Text(
                    text = habit.subtitle,
                    fontSize = 13.sp,
                    fontWeight = FontWeight.Medium,
                    color = TextGray
                )

                Spacer(modifier = Modifier.height(8.dp))

                // Etiquetas / Chips (múltiplos de 4dp)
                Row(
                    horizontalArrangement = Arrangement.spacedBy(8.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    habit.tags.forEach { tag ->
                        val chipBg = when (tag.lowercase()) {
                            "diario" -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA)
                            "hidratación" -> if (isDark) Color(0xFF1A237E) else Color(0xFFF3E5F5)
                            "alta intensidad" -> if (isDark) Color(0xFFE65100) else Color(0xFFFFE0B2)
                            "fuerza" -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA)
                            else -> if (isDark) Color(0xFF303030) else Color(0xFFF1F3F4)
                        }
                        val chipTextColor = when (tag.lowercase()) {
                            "alta intensidad" -> if (isDark) Color(0xFFFFCC80) else Color(0xFFA15D22)
                            else -> if (isDark) Color.White else TextDark
                        }

                        Box(
                            modifier = Modifier
                                .background(chipBg, RoundedCornerShape(12.dp))
                                .padding(horizontal = 8.dp, vertical = 4.dp)
                        ) {
                            Text(
                                text = tag,
                                fontSize = 11.sp,
                                fontWeight = FontWeight.Bold,
                                color = chipTextColor
                            )
                        }
                    }
                }
            }

            Spacer(modifier = Modifier.width(8.dp))

            // Control de Progreso o Checkbox
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.Center
            ) {
                if (habit.isProgressive) {
                    Text(
                        text = "${habit.currentProgress}/${habit.maxProgress}",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    LinearProgressIndicator(
                        progress = { habit.currentProgress.toFloat() / habit.maxProgress.toFloat() },
                        modifier = Modifier
                            .width(52.dp)
                            .height(8.dp)
                            .clip(RoundedCornerShape(4.dp)),
                        color = MaterialTheme.colorScheme.primary,
                        trackColor = if (isDark) Color(0xFF303030) else Color(0xFFE0E0E0),
                        strokeCap = StrokeCap.Round
                    )
                } else if (habit.timeOrDuration.isNotEmpty() && habit.timeOrDuration.contains("Min")) {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .background(Color(0xFFFF9800), CircleShape)
                            .clickable {
                                onToggleComplete(habit)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Timer,
                            contentDescription = "Temporizador",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp)
                        )
                    }
                } else {
                    Box(
                        modifier = Modifier
                            .size(38.dp)
                            .border(
                                width = 2.dp,
                                color = if (habit.isCompleted) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF505050) else Color(0xFFD0D0D0),
                                shape = CircleShape
                            )
                            .background(
                                color = if (habit.isCompleted) {
                                    if (isDark) MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) else Color(0xFFE8F5E9)
                                } else Color.Transparent,
                                shape = CircleShape
                            )
                            .clickable {
                                onToggleComplete(habit)
                            },
                        contentAlignment = Alignment.Center
                    ) {
                        if (habit.isCompleted) {
                            Icon(
                                imageVector = Icons.Filled.Check,
                                contentDescription = "Completado",
                                tint = MaterialTheme.colorScheme.primary,
                                modifier = Modifier.size(20.dp)
                            )
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// COMPONENTE DE ÍTEM DE HISTORIAL: HabitHistoryItemRow
// ============================================================================
@Composable
fun HabitHistoryItemRow(
    item: HabitHistoryItem,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() }
            .padding(vertical = 12.dp, horizontal = 16.dp),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        Row(
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            Box(
                modifier = Modifier
                    .size(28.dp)
                    .background(
                        color = if (item.isCompleted) Color(0xFF26A69A) else Color(0xFFE0E0E0),
                        shape = CircleShape
                    ),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Outlined.Check,
                    contentDescription = null,
                    tint = Color.White,
                    modifier = Modifier.size(16.dp)
                )
            }

            Text(
                text = item.habitTitle,
                fontSize = 15.sp,
                fontWeight = FontWeight.Medium,
                color = TextDark
            )
        }

        Text(
            text = item.dateText,
            fontSize = 13.sp,
            fontWeight = FontWeight.Medium,
            color = TextGray
        )
    }
}

// ============================================================================
// COMPONENTE BANNER DE MASCOTA (Usando la foto Pulpo de descargas)
// ============================================================================
@Composable
fun MascotBannerCard(
    onVisitMascotClick: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 8.dp),
        horizontalAlignment = Alignment.CenterHorizontally
    ) {
        // Carga de la foto del pulpo con Coil (AsyncImage) y placeholder
        AsyncImage(
            model = R.drawable.pulpo,
            placeholder = painterResource(id = R.drawable.ic_launcher_background),
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
            text = "Has alcanzado tu meta de 7 días. ¡Octo está orgulloso!",
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
// BOTÓN "+ NUEVO HÁBITO"
// ============================================================================
@Composable
fun NewHabitButtonCard(onClick: () -> Unit) {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clickable { onClick() },
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF383838) else Color(0xFFE0E0E0)),
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
                text = "Nuevo Hábito",
                fontSize = 14.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )
        }
    }
}

// ============================================================================
// PROGRESO SEMANAL
// ============================================================================
@Composable
fun WeeklyProgressCard() {
    val isDark = isSystemInDarkTheme()
    Card(
        modifier = Modifier.fillMaxWidth(),
        shape = RoundedCornerShape(20.dp),
        colors = CardDefaults.cardColors(containerColor = CardWhite),
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
    ) {
        Column(
            modifier = Modifier.padding(16.dp)
        ) {
            Text(
                text = "Progreso Semanal",
                fontSize = 16.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark
            )

            Spacer(modifier = Modifier.height(12.dp))

            val days = listOf("L", "M", "M", "J", "V", "S", "D")
            val completed = listOf(true, true, true, true, true, true, false)

            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                days.forEachIndexed { index, day ->
                    Column(
                        horizontalAlignment = Alignment.CenterHorizontally
                    ) {
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
                                    color = if (completed[index]) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF383838) else Color(0xFFE0E0E0),
                                    shape = CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            if (completed[index]) {
                                Icon(
                                    imageVector = Icons.Outlined.Check,
                                    contentDescription = null,
                                    tint = Color.White,
                                    modifier = Modifier.size(18.dp)
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

// ============================================================================
// ESTADOS REQUERIDOS POR LA RÚBRICA (Carga, Vacío, Error)
// ============================================================================
@Composable
fun LoadingStateView() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .height(200.dp)
            .padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        CircularProgressIndicator(color = PrimaryTeal)
        Spacer(modifier = Modifier.height(16.dp))
        Text(
            text = "Cargando tus hábitos de HabitOcto...",
            fontSize = 14.sp,
            color = TextGray,
            fontWeight = FontWeight.Medium
        )
    }
}

@Composable
fun EmptyStateView(onAddClick: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        AsyncImage(
            model = R.drawable.pulpo,
            placeholder = painterResource(id = R.drawable.ic_launcher_background),
            contentDescription = "Octo Vacío",
            modifier = Modifier.size(80.dp).clip(CircleShape)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "No tienes hábitos registrados",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "¡Empieza creando tu primer hábito hoy mismo!",
            fontSize = 13.sp,
            color = TextGray,
            textAlign = TextAlign.Center
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onAddClick,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
        ) {
            Text("Crear Hábito", color = Color.White)
        }
    }
}

@Composable
fun ErrorStateView(onRetry: () -> Unit) {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Outlined.Refresh,
            contentDescription = "Error",
            tint = Color.Red,
            modifier = Modifier.size(48.dp)
        )
        Spacer(modifier = Modifier.height(12.dp))
        Text(
            text = "Error al cargar la lista",
            fontSize = 16.sp,
            fontWeight = FontWeight.Bold,
            color = TextDark
        )
        Spacer(modifier = Modifier.height(4.dp))
        Text(
            text = "Ocurrió un problema de red simulado.",
            fontSize = 13.sp,
            color = TextGray
        )
        Spacer(modifier = Modifier.height(16.dp))
        Button(
            onClick = onRetry,
            colors = ButtonDefaults.buttonColors(containerColor = PrimaryTeal)
        ) {
            Text("Reintentar", color = Color.White)
        }
    }
}
