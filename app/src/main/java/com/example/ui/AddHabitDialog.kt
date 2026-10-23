package com.example.ui

import android.widget.Toast
import androidx.compose.foundation.background
import com.example.ui.theme.isAppInDarkTheme
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.isSystemInDarkTheme
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
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.outlined.Delete
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.example.R
import com.example.ui.components.HabitImage
import com.example.model.HabitItem
import com.example.ui.components.CardWhite
import com.example.ui.components.ScreenBackground
import com.example.ui.components.TextDark
import com.example.ui.components.TextGray
import com.example.ui.components.getCategoryBgColor
import com.example.ui.components.getCategoryIcon
import com.example.ui.theme.BorderGrayDark
import com.example.ui.theme.BorderGrayLight
import com.example.ui.theme.DangerRed
import com.example.ui.theme.DarkTealBackground
import com.example.ui.theme.LightIconBoxBg
import com.example.ui.theme.PrimaryTeal
import com.example.ui.theme.SecondaryTeal
import com.example.ui.theme.SurfaceDarkVariant
import com.example.ui.theme.SurfaceLightVariant

/**
 * CategoryOption define la estructura de una categoría seleccionable.
 */
data class CategoryOption(
    val id: String,
    val name: String
)

/**
 * Lista de categorías estáticas para clasificar hábitos.
 */
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

/**
 * AddHabitScreen es el formulario modal a pantalla completa para crear, modificar o eliminar hábitos y micro hábitos.
 *
 * - **¿Para qué sirve?** Permite al usuario capturar un nuevo hábito, editar uno existente o eliminarlo de su rutina.
 * - **¿Cómo funciona?**
 *   1. Recibe un hábito opcional `habitToEdit`. Si existe, inicializa los campos con sus valores actuales.
 *   2. Permite conmutar entre hábito convencional y micro hábito mediante un Switch.
 *   3. En modo edición, expone el botón de eliminación conectado a `onDeleteHabit`.
 *   4. Al pulsar Guardar, construye un `HabitItem` inmutable y lo entrega al callback `onSaveHabit`.
 * - **¿Qué pasa si lo quitamos?** El usuario no tendría ningún formulario para ingresar o ajustar hábitos en la aplicación.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    habitToEdit: HabitItem? = null,
    onDismiss: () -> Unit,
    onSaveHabit: (HabitItem) -> Unit,
    onDeleteHabit: ((HabitItem) -> Unit)? = null
) {
    // Contexto de Android para Toasts de validación y confirmación
    val context = LocalContext.current
    val isEditing = habitToEdit != null

    // Estados reactivos vinculados a los campos del formulario
    var habitName by remember { mutableStateOf(habitToEdit?.title ?: "") }
    var selectedCategory by remember { mutableStateOf(habitToEdit?.category ?: "Hidratación") }
    var isDailyFrequency by remember { mutableStateOf(habitToEdit?.frequency != "Semanal") }
    var isProgressive by remember { mutableStateOf(habitToEdit?.isProgressive ?: false) }
    var isMicroHabit by remember { mutableStateOf(habitToEdit?.isMicroHabit ?: false) }
    var dailyGoalText by remember { mutableStateOf(if (habitToEdit?.isProgressive == true) habitToEdit.maxProgress.toString() else "") }
    var reminderTime by remember { mutableStateOf("08:00 AM") }
    var isReminderEnabled by remember { mutableStateOf(true) }
    var durationText by remember { mutableStateOf(habitToEdit?.timeOrDuration?.ifEmpty { "00:30 horas" } ?: "00:30 horas") }

    // Diálogo nativo de pantalla completa inmersiva
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
                // Cabecera del formulario con botón Cancelar y título de acción
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
                        text = if (isEditing) "Editar Hábito" else "Añadir Nuevo Hábito",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )

                    Spacer(modifier = Modifier.width(48.dp))
                }

                // Campo 1: Nombre del hábito
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
                        placeholder = { Text("ej., Leer 1 Página", color = TextGray) },
                        modifier = Modifier.fillMaxWidth(),
                        shape = RoundedCornerShape(12.dp),
                        colors = OutlinedTextFieldDefaults.colors(
                            focusedContainerColor = CardWhite,
                            unfocusedContainerColor = CardWhite,
                            focusedBorderColor = MaterialTheme.colorScheme.primary,
                            unfocusedBorderColor = BorderGrayLight,
                            focusedTextColor = TextDark,
                            unfocusedTextColor = TextDark
                        )
                    )
                }

                // Switch de selección: Hábito estándar vs Micro Hábito
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "Micro Hábito Atómico",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Text(
                            text = "Acción rápida de menos de 2 minutos (ej. tomar agua, 1 plancha)",
                            fontSize = 12.sp,
                            color = TextGray
                        )
                    }
                    Switch(
                        checked = isMicroHabit,
                        onCheckedChange = { isMicroHabit = it },
                        colors = SwitchDefaults.colors(checkedTrackColor = SecondaryTeal)
                    )
                }

                // Campo 2: Selector horizontal de categorías
                Column {
                    Text(
                        text = stringResource(R.string.categor_a),
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
                            .background(
                                if (isAppInDarkTheme) BorderGrayDark else BorderGrayLight,
                                RoundedCornerShape(12.dp)
                            )
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

                // Campo 4: Interruptor de hábito progresivo con meta numérica
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
                            text = "Meta Diaria (Número)",
                            fontSize = 14.sp,
                            fontWeight = FontWeight.Bold,
                            color = TextDark
                        )
                        Spacer(modifier = Modifier.height(6.dp))
                        OutlinedTextField(
                            value = dailyGoalText,
                            onValueChange = { dailyGoalText = it },
                            placeholder = { Text("ej., 8", color = TextGray) },
                            modifier = Modifier.fillMaxWidth(),
                            shape = RoundedCornerShape(12.dp),
                            colors = OutlinedTextFieldDefaults.colors(
                                focusedContainerColor = CardWhite,
                                unfocusedContainerColor = CardWhite,
                                focusedBorderColor = MaterialTheme.colorScheme.primary,
                                unfocusedBorderColor = BorderGrayLight,
                                focusedTextColor = TextDark,
                                unfocusedTextColor = TextDark
                            )
                        )
                    }
                }

                // Campo 5: Hora de recordatorio
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
                                .background(
                                    if (isAppInDarkTheme) SurfaceDarkVariant else SurfaceLightVariant,
                                    RoundedCornerShape(8.dp)
                                )
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

                // Campo 6: Duración estimada del hábito
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
                            .background(
                                if (isAppInDarkTheme) SurfaceDarkVariant else SurfaceLightVariant,
                                RoundedCornerShape(12.dp)
                            )
                            .padding(14.dp)
                    ) {
                        Text(text = durationText, fontWeight = FontWeight.Medium, color = TextDark)
                    }
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Mensaje motivador con la mascota Pulpo
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.End,
                    verticalAlignment = Alignment.Bottom
                ) {
                    val isDark = isAppInDarkTheme
                    Box(
                        modifier = Modifier
                            .background(
                                if (isDark) DarkTealBackground else LightIconBoxBg,
                                RoundedCornerShape(16.dp)
                            )
                            .padding(horizontal = 14.dp, vertical = 10.dp)
                    ) {
                        Text(
                            text = if (isEditing) "¡Actualiza tu hábito con éxito!" else "¡Construyamos un nuevo hábito!",
                            fontSize = 13.sp,
                            fontWeight = FontWeight.Bold,
                            color = if (isDark) SecondaryTeal else PrimaryTeal
                        )
                    }

                    Spacer(modifier = Modifier.width(8.dp))

                    HabitImage(
                        model = "https://raw.githubusercontent.com/garzzaro/Proyecto-Plataformas-Moviles/feature/lab2-lazy-list/app/src/main/res/drawable/pulpo.avif",
                        contentDescription = "Mascota Octo",
                        modifier = Modifier
                            .size(60.dp)
                            .clip(CircleShape),
                        contentScale = ContentScale.Crop
                    )
                }

                Spacer(modifier = Modifier.height(8.dp))

                // Botón principal de guardado / actualización
                Button(
                    onClick = {
                        if (habitName.isBlank()) {
                            Toast.makeText(context, "Escribe el nombre del hábito", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        val parsedGoal = dailyGoalText.toIntOrNull() ?: 1
                        val updatedTags = mutableListOf(if (isDailyFrequency) "Diario" else "Semanal", selectedCategory)
                        if (isMicroHabit) updatedTags.add(0, "Micro")

                        val habitResult = HabitItem(
                            id = habitToEdit?.id ?: System.currentTimeMillis().toString(),
                            title = habitName,
                            subtitle = if (isProgressive) "$dailyGoalText al día" else if (isMicroHabit) "Micro hábito de 2 min" else "Hábito diario",
                            category = selectedCategory,
                            frequency = if (isDailyFrequency) "Diario" else "Semanal",
                            streak = habitToEdit?.streak ?: 1,
                            isProgressive = isProgressive,
                            isMicroHabit = isMicroHabit,
                            currentProgress = habitToEdit?.currentProgress ?: (if (isProgressive) 1 else 0),
                            maxProgress = if (isProgressive) parsedGoal.coerceAtLeast(1) else 1,
                            isCompleted = habitToEdit?.isCompleted ?: false,
                            tags = updatedTags,
                            timeOrDuration = if (isMicroHabit) "2 Min" else durationText
                        )

                        onSaveHabit(habitResult)
                        val msg = if (isEditing) "¡Hábito modificado!" else "¡Hábito guardado!"
                        Toast.makeText(context, msg, Toast.LENGTH_SHORT).show()
                        onDismiss()
                    },
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(52.dp),
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal),
                    shape = RoundedCornerShape(26.dp)
                ) {
                    Text(
                        text = if (isEditing) "Guardar Cambios" else "Guardar Hábito",
                        fontSize = 16.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark
                    )
                }

                // Botón secundario para eliminar hábito existente (Solo disponible en modo edición)
                if (isEditing && onDeleteHabit != null && habitToEdit != null) {
                    OutlinedButton(
                        onClick = {
                            onDeleteHabit(habitToEdit)
                            Toast.makeText(context, "Hábito eliminado", Toast.LENGTH_SHORT).show()
                            onDismiss()
                        },
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(50.dp),
                        colors = ButtonDefaults.outlinedButtonColors(
                            contentColor = DangerRed
                        ),
                        shape = RoundedCornerShape(25.dp)
                    ) {
                        Icon(
                            imageVector = Icons.Outlined.Delete,
                            contentDescription = "Eliminar",
                            tint = DangerRed,
                            modifier = Modifier.size(18.dp)
                        )
                        Spacer(modifier = Modifier.width(8.dp))
                        Text(
                            text = "Eliminar Hábito",
                            fontSize = 15.sp,
                            fontWeight = FontWeight.Bold,
                            color = DangerRed
                        )
                    }
                }

                Spacer(modifier = Modifier.height(16.dp))
            }
        }
    }
}
