package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil.compose.AsyncImage
import com.example.R
import com.example.TopNavigationBarSection
import com.example.model.HabitHistoryItem
import com.example.model.HabitItem
import com.example.ui.components.CardWhite
import com.example.ui.components.HabitHistoryItemRow
import com.example.ui.components.HabitItemCard
import com.example.ui.components.MascotBannerCard
import com.example.ui.components.NewHabitButtonCard
import com.example.ui.components.ScreenBackground
import com.example.ui.components.TextDark
import com.example.ui.components.TextGray
import com.example.ui.components.WeeklyProgressCard

// Lista inicial de hábitos y micro hábitos (data class inmutable, 11 elementos > 10 mínimos para scroll)
val initialHabitsList = listOf(
    HabitItem(
        id = "1",
        title = "Beber Agua",
        subtitle = "8 vasos al día",
        category = "Hidratación",
        frequency = "Diario",
        streak = 7,
        isProgressive = true,
        isMicroHabit = true,
        currentProgress = 5,
        maxProgress = 8,
        tags = listOf("Micro", "Diario", "Hidratación")
    ),
    HabitItem(
        id = "2",
        title = "Leer 1 Página",
        subtitle = "Racha: 12",
        category = "Aprendizaje",
        frequency = "Diario",
        streak = 12,
        isCompleted = true,
        isMicroHabit = true,
        tags = listOf("Micro", "Lectura")
    ),
    HabitItem(
        id = "3",
        title = "Plancha 1 Min",
        subtitle = "Ritual Mañanero",
        category = "Ejercicio",
        frequency = "Diario",
        streak = 8,
        timeOrDuration = "1 Min",
        isMicroHabit = true,
        tags = listOf("Micro", "Fuerza")
    ),
    HabitItem(
        id = "4",
        title = "Meditación",
        subtitle = "10 min al día",
        category = "Mindfulness",
        frequency = "Diario",
        streak = 15,
        isCompleted = true,
        tags = listOf("Mindfulness")
    ),
    HabitItem(
        id = "5",
        title = "Caminar 10,000 Pasos",
        subtitle = "8,200 / 10,000",
        category = "Ejercicio",
        frequency = "Diario",
        streak = 4,
        isProgressive = true,
        currentProgress = 8200,
        maxProgress = 10000,
        tags = listOf("Salud", "Diario")
    ),
    HabitItem(
        id = "6",
        title = "Estudiar Código",
        subtitle = "45 min al día",
        category = "Productividad",
        frequency = "Diario",
        streak = 20,
        tags = listOf("Aprendizaje", "Código")
    ),
    HabitItem(
        id = "7",
        title = "Dormir 8 Horas",
        subtitle = "Meta nocturna",
        category = "Sueño",
        frequency = "Diario",
        streak = 9,
        isCompleted = true,
        tags = listOf("Sueño", "Salud")
    ),
    HabitItem(
        id = "8",
        title = "Comer 2 Frutas",
        subtitle = "Porción diaria",
        category = "Nutrición",
        frequency = "Diario",
        streak = 3,
        isProgressive = true,
        isMicroHabit = true,
        currentProgress = 1,
        maxProgress = 2,
        tags = listOf("Micro", "Nutrición")
    ),
    HabitItem(
        id = "9",
        title = "Escribir Diario",
        subtitle = "Reflexión nocturna",
        category = "Salud Mental",
        frequency = "Diario",
        streak = 7,
        tags = listOf("Bienestar")
    ),
    HabitItem(
        id = "10",
        title = "Ahorrar $5",
        subtitle = "Fondo personal",
        category = "Finanzas",
        frequency = "Semanal",
        streak = 2,
        isMicroHabit = true,
        tags = listOf("Micro", "Finanzas", "Ahorro")
    ),
    HabitItem(
        id = "11",
        title = "Práctica de Guitarra",
        subtitle = "20 min al día",
        category = "Arte y Creatividad",
        frequency = "Diario",
        streak = 5,
        tags = listOf("Música", "Arte")
    )
)

// Lista de Historial de Hábitos
val sampleHistoryItems = listOf(
    HabitHistoryItem("h1", "Beber Agua", "Lun, Oct 21", true),
    HabitHistoryItem("h2", "Leer 1 Página", "Dom, Oct 20", true),
    HabitHistoryItem("h3", "Plancha 1 Min", "Dom, Oct 20", true),
    HabitHistoryItem("h4", "Meditación", "Sáb, Oct 19", false),
    HabitHistoryItem("h5", "Caminar 10,000 Pasos", "Vie, Oct 18", true),
    HabitHistoryItem("h6", "Estudiar Código", "Jue, Oct 17", true),
    HabitHistoryItem("h7", "Dormir 8 Horas", "Mié, Oct 16", true),
    HabitHistoryItem("h8", "Comer 2 Frutas", "Mar, Oct 15", true),
    HabitHistoryItem("h9", "Escribir Diario", "Lun, Oct 14", false),
    HabitHistoryItem("h10", "Ahorrar $5", "Dom, Oct 13", true),
    HabitHistoryItem("h11", "Aprender Idioma", "Sáb, Oct 12", true),
    HabitHistoryItem("h12", "Pintar o Dibujar", "Vie, Oct 11", true)
)

/**
 * TrackerScreen representa la pestaña de rastreo de hábitos y estadísticas del Lab 2.
 *
 * - **¿Para qué sirve?** Visualiza hábitos, micro hábitos, progreso semanal, estadísticas y modo edición/eliminación.
 * - **¿Cómo funciona?** Implementa un `LazyColumn` que agrupa el banner motivacional, el cuadro de hábitos interactivo
 *   con soporte de edición/borrado, desglose y el historial.
 * - **¿Qué pasa si lo quitamos?** Se eliminaría el módulo central de seguimiento de hábitos de la app.
 */
@Composable
fun TrackerScreen(
    habits: List<HabitItem>,
    onToggleHabit: (HabitItem) -> Unit,
    onOpenAddHabit: () -> Unit,
    onEditHabit: (HabitItem) -> Unit,
    onDeleteHabit: (HabitItem) -> Unit
) {
    val context = LocalContext.current
    var isEditMode by remember { mutableStateOf(false) }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(ScreenBackground),
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        // Cabecera superior compartida
        item {
            TopNavigationBarSection()
        }

        // Banner motivacional de Mascota
        item {
            MascotBannerCard(
                onVisitMascotClick = {
                    Toast.makeText(context, "¡Saludando a Octo! 🐙✨", Toast.LENGTH_SHORT).show()
                }
            )
        }

        // Encabezado de Sección: "Tus Hábitos" y botón de alternar "Editar / Listo"
        item {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Column {
                    Text(
                        text = "Tus Hábitos y Micro Hábitos",
                        fontSize = 20.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Text(
                        text = if (isEditMode) "Toca el lápiz para editar o la papelera para borrar" else "Toca para marcar o ver detalle",
                        fontSize = 12.sp,
                        color = TextGray
                    )
                }

                Box(
                    modifier = Modifier
                        .background(
                            if (isEditMode) MaterialTheme.colorScheme.primary
                            else if (isSystemInDarkTheme()) Color(0xFF383838) else Color(0xFFE0E0E0),
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
                        text = if (isEditMode) "Listo" else "Editar",
                        fontSize = 13.sp,
                        fontWeight = FontWeight.Bold,
                        color = if (isEditMode) Color.White else TextDark
                    )
                }
            }
        }

        // Cuadro de Hábitos con Scroll interno (~4 elementos visibles)
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
                    items(items = habits, key = { it.id }) { habit ->
                        HabitItemCard(
                            habit = habit,
                            isEditMode = isEditMode,
                            onToggleComplete = { onToggleHabit(it) },
                            onClickDetail = { onEditHabit(it) },
                            onEditHabit = { onEditHabit(it) },
                            onDeleteHabit = { onDeleteHabit(it) }
                        )
                    }
                }
            }
        }

        // Botón "+ Nuevo Hábito / Micro Hábito"
        item {
            NewHabitButtonCard(onClick = onOpenAddHabit)
        }

        // Progreso Semanal
        item {
            WeeklyProgressCard()
        }

        // Título Estadísticas
        item {
            Text(
                text = "Porcentajes Semanales de Cumplimiento",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Gráfico de Porcentajes Semanales
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
                    val days = listOf("L", "M", "M", "J", "V", "S", "D")
                    val percentages = listOf(85, 90, 75, 95, 80, 60, 70)
                    val barColors = listOf(
                        Color(0xFF42A5F5),
                        Color(0xFF00695C),
                        Color(0xFF00ACC1),
                        Color(0xFF26A69A),
                        Color(0xFFFB8C00),
                        Color(0xFFFFB74D),
                        Color(0xFFF57C00)
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

        // Desglose de Progreso
        item {
            Column {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Text(
                        text = "Desglose del Progreso",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    AsyncImage(
                        model = "https://raw.githubusercontent.com/garzzaro/Proyecto-Plataformas-Moviles/feature/lab2-lazy-list/app/src/main/res/drawable/pulpo.avif",
                        placeholder = painterResource(id = R.drawable.ic_launcher_background),
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
                    // Card 1: Completadas
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
                                    text = "Completados",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.CheckCircle,
                                    contentDescription = null,
                                    tint = Color(0xFF4CAF50),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "38/45",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF2E7D32)
                            )
                        }
                    }

                    // Card 2: Programadas
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
                                    text = "Programados",
                                    fontSize = 12.sp,
                                    fontWeight = FontWeight.Bold,
                                    color = TextDark,
                                    modifier = Modifier.weight(1f)
                                )
                                Icon(
                                    imageVector = Icons.Outlined.CalendarToday,
                                    contentDescription = null,
                                    tint = Color(0xFF1976D2),
                                    modifier = Modifier.size(20.dp)
                                )
                            }
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "45",
                                fontSize = 32.sp,
                                fontWeight = FontWeight.Bold,
                                color = Color(0xFF1565C0)
                            )
                        }
                    }
                }
            }
        }

        // Historial de Hábitos
        item {
            Text(
                text = "Historial de Hábitos",
                fontSize = 18.sp,
                fontWeight = FontWeight.Bold,
                color = TextDark,
                modifier = Modifier.padding(top = 8.dp)
            )
        }

        // Historial de Hábitos con Scroll interno (~6 elementos visibles)
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
                    items(items = sampleHistoryItems, key = { it.id }) { historyItem ->
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
                                .background(if (isSystemInDarkTheme()) Color(0xFF2C2C2C) else Color(0xFFF0F0F0))
                        )
                    }
                }
            }
        }
    }
}
