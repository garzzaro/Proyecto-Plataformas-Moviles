package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
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
import androidx.compose.foundation.isSystemInDarkTheme
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Icon
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.OutlinedTextFieldDefaults
import androidx.compose.material3.Surface
import androidx.compose.material3.Switch
import androidx.compose.material3.SwitchDefaults
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
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
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import coil.compose.AsyncImage
import com.example.R
import com.example.model.HabitItem
import com.example.ui.theme.CardWhite
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.TextDark
import com.example.ui.components.ScreenBackground
import com.example.ui.components.TextGray
import com.example.ui.components.getCategoryBgColor
import com.example.ui.components.getCategoryIcon

data class CategoryOption(
    val id: String,
    val name: String
)

val expandedCategories = listOf(
    CategoryOption("1", "Hidratación"),
    CategoryOption("2", "Aprendizaje"),
    CategoryOption("3", "Ejercicio"),
    CategoryOption("4", "Mindfulness"),
    CategoryOption("5", "Productividad"),
    CategoryOption("6", "Nutrición"),
    CategoryOption("7", "Sueño"),
    CategoryOption("8", "Arte y Creatividad"),
    CategoryOption("9", "Finanzas"),
    CategoryOption("10", "Salud Mental")
)

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    onDismiss: () -> Unit,
    onSaveHabit: (HabitItem) -> Unit
) {
    val context = LocalContext.current
    var habitName by remember { mutableStateOf("") }
    var selectedCategory by remember { mutableStateOf("Hidratación") }
    var isDailyFrequency by remember { mutableStateOf(true) }
    var isProgressive by remember { mutableStateOf(false) }
    var dailyGoalText by remember { mutableStateOf("") }
    var reminderTime by remember { mutableStateOf("08:00 AM") }
    var isReminderEnabled by remember { mutableStateOf(true) }
    var durationText by remember { mutableStateOf("00:30 horas") }

    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false)
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(),
            color = ScreenBackground
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp)
                    .verticalScroll(rememberScrollState()),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                // Cabecera: Cancelar - Añadir Nuevo Hábito
                Row(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(top = 8.dp),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    TextButton(onClick = onDismiss) {
                        Text(
                            text = "Cancelar",
                            color = MaterialTheme.colorScheme.primary,
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Añadir Nuevo Hábito",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Campo 1: Nombre del Hábito
                Column {
                    Text(
                        text = "Nombre del Hábito",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    OutlinedTextField(
                        value = habitName,
                        onValueChange = { habitName = it },
                        placeholder = { Text("ej., Leer un libro", color = Color.Gray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardWhite,
                            unfocusedContainerColor = CardWhite,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = Color(0xFFE0E0E0)
                        )
                    )
                }

                // Campo 2: Categoría (LazyRow con keys estables)
                Column {
                    Text(
                        text = "Categoría",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(10.dp))

                    LazyRow(
                        horizontalArrangement = Arrangement.spacedBy(16.dp)
                    ) {
                        items(items = expandedCategories, key = { it.id }) { cat ->
                            val isSelected = selectedCategory == cat.name
                            Column(
                                horizontalAlignment = Alignment.CenterHorizontally,
                                modifier = Modifier.clickable { selectedCategory = cat.name }
                            ) {
                                Box(
                                    modifier = Modifier
                                        .size(56.dp)
                                        .background(getCategoryBgColor(cat.name), CircleShape)
                                        .border(
                                            width = if (isSelected) 3.dp else 0.dp,
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent,
                                            shape = CircleShape
                                        ),
                                    contentAlignment = Alignment.Center
                                ) {
                                    Icon(
                                        imageVector = getCategoryIcon(cat.name),
                                        contentDescription = cat.name,
                                        tint = MaterialTheme.colorScheme.primary,
                                        modifier = Modifier.size(28.dp)
                                    )
                                }
                                Spacer(modifier = Modifier.height(6.dp))
                                Text(
                                    text = cat.name,
                                    fontSize = 12.sp,
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium,
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextDark
                                )
                            }
                        }
                    }
                }

                // Campo 3: Frecuencia (Diario / Semanal)
                Column {
                    Text(
                        text = "Frecuencia",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Row(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
                            .padding(4.dp)
                    ) {
                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (isDailyFrequency) CardWhite else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { isDailyFrequency = true },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Diario",
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }

                        Box(
                            modifier = Modifier
                                .weight(1f)
                                .height(40.dp)
                                .background(
                                    if (!isDailyFrequency) CardWhite else Color.Transparent,
                                    RoundedCornerShape(8.dp)
                                )
                                .clickable { isDailyFrequency = false },
                            contentAlignment = Alignment.Center
                        ) {
                            Text(
                                text = "Semanal",
                                fontWeight = FontWeight.Bold,
                                color = TextDark
                            )
                        }
                    }
                }

                // Campo 4: Hábito Progresivo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hábito Progresivo",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "(ej., meta de 8 vasos de agua)",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                    Switch(
                        checked = isProgressive,
                        onCheckedChange = { isProgressive = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = SecondaryTeal)
                    )
                }

                if (isProgressive) {
                    Column {
                        Text(
                            text = "Meta Diaria",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = dailyGoalText,
                            onValueChange = { dailyGoalText = it },
                            placeholder = { Text("ej., 8 vasos", color = Color.Gray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CardWhite,
                                unfocusedContainerColor = CardWhite,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = Color(0xFFE0E0E0)
                            )
                        )
                    }
                }

                // Campo 5: Recordatorio
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Hora de Recordatorio",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(4.dp))
                        Box(
                            modifier = Modifier
                                .background(Color(0xFFE8EAF6), RoundedCornerShape(8.dp))
                                .padding(horizontal = 12.dp, vertical = 8.dp)
                        ) {
                            Text(text = reminderTime, fontWeight = FontWeight.Bold, color = TextDark)
                        }
                    }
                    Switch(
                        checked = isReminderEnabled,
                        onCheckedChange = { isReminderEnabled = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = SecondaryTeal)
                    )
                }

                // Campo 6: Duración
                Column {
                    Text(
                        text = "Duración / Tiempo",
                        fontSize = 14.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                    Spacer(modifier = Modifier.height(6.dp))
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .background(Color(0xFFE8EAF6), RoundedCornerShape(12.dp))
                            .padding(14.dp)
                    ) {
                        Text(text = durationText, fontWeight = FontWeight.Medium, color = TextDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Burbuja del Pulpo usando la foto Pulpo con AsyncImage
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val isDark = isSystemInDarkTheme()
                    Box(
                        modifier = Modifier
                            .background(if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA), RoundedCornerShape(16.dp))
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = "¡Construyamos un nuevo hábito!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) SecondaryTeal else PrimaryTeal
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    AsyncImage(
                        model = R.drawable.pulpo,
                        placeholder = painterResource(id = R.drawable.ic_launcher_background),
                        contentDescription = "Mascota Octo",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón Guardar
                Button(
                    onClick = {
                        if (habitName.isBlank()) {
                            Toast.makeText(context, "Escribe el nombre del hábito", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val newHabit = HabitItem(
                            id = System.currentTimeMillis().toString(),
                            title = habitName,
                            subtitle = if (isProgressive) dailyGoalText.ifBlank { "Meta personalizada" } else "Hábito diario",
                            category = selectedCategory,
                            frequency = if (isDailyFrequency) "Diario" else "Semanal",
                            streak = 1,
                            isProgressive = isProgressive,
                            currentProgress = if (isProgressive) 1 else 0,
                            maxProgress = if (isProgressive) 8 else 1,
                            isCompleted = false,
                            tags = listOf(if (isDailyFrequency) "Diario" else "Semanal", selectedCategory)
                        )

                        onSaveHabit(newHabit)
                        Toast.makeText(context, "¡Hábito '$habitName' guardado!", Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = "Guardar Hábito",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
