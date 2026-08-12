package com.example.ui // Declaración del paquete al que pertenece este diálogo

import android.widget.Toast // Importa para alertas flotantes de Android
import androidx.compose.foundation.background // Modificador de color de fondo
import androidx.compose.foundation.border // Modificador de borde
import androidx.compose.foundation.clickable // Modificador para hacer clics
import androidx.compose.foundation.layout.Arrangement // Configuración de alineamiento
import androidx.compose.foundation.layout.Box // Contenedor para apilar elementos
import androidx.compose.foundation.layout.Column // Contenedor vertical
import androidx.compose.foundation.layout.Row // Contenedor horizontal
import androidx.compose.foundation.layout.Spacer // Componente separador
import androidx.compose.foundation.layout.fillMaxSize // Ocupar tamaño máximo
import androidx.compose.foundation.layout.fillMaxWidth // Ocupar ancho máximo
import androidx.compose.foundation.layout.height // Altura fija
import androidx.compose.foundation.layout.padding // Márgenes internos
import androidx.compose.foundation.layout.size // Dimensiones cuadradas
import androidx.compose.foundation.layout.width // Ancho fijo
import androidx.compose.foundation.lazy.LazyRow // Fila eficiente con desplazamiento
import androidx.compose.foundation.lazy.items // Carga elementos dinámicos
import androidx.compose.foundation.rememberScrollState // Guarda estado de scroll
import androidx.compose.foundation.shape.CircleShape // Forma de círculo
import androidx.compose.foundation.shape.RoundedCornerShape // Forma redondeada
import androidx.compose.foundation.isSystemInDarkTheme // Detecta modo oscuro
import androidx.compose.foundation.verticalScroll // Modificador de scroll vertical
import androidx.compose.material3.Button // Botón de Material 3
import androidx.compose.material3.ButtonDefaults // Propiedades de botones
import androidx.compose.material3.ExperimentalMaterial3Api // Soporte experimental
import androidx.compose.material3.MaterialTheme // Acceso al tema actual
import androidx.compose.material3.Icon // Elemento de dibujo vectorial
import androidx.compose.material3.OutlinedTextField // Campo de texto delineado
import androidx.compose.material3.OutlinedTextFieldDefaults // Propiedades de campos
import androidx.compose.material3.Surface // Contenedor de elevaciones
import androidx.compose.material3.Switch // Control deslizable activo/inactivo
import androidx.compose.material3.SwitchDefaults // Colores de switches
import androidx.compose.material3.Text // Componente de texto
import androidx.compose.material3.TextButton // Botón plano
import androidx.compose.runtime.Composable // Anotación Compose
import androidx.compose.runtime.getValue // Delegado de lectura de estados
import androidx.compose.runtime.mutableStateOf // Inicialización de estados
import androidx.compose.runtime.remember // Persiste estado en recomposiciones
import androidx.compose.runtime.setValue // Delegado de escritura de estados
import androidx.compose.ui.Alignment // Alineación espacial
import androidx.compose.ui.Modifier // Modificador de Compose
import androidx.compose.ui.draw.clip // Modificador de recorte
import androidx.compose.ui.graphics.Color // Objeto de color
import androidx.compose.ui.layout.ContentScale // Escala de imagen
import androidx.compose.ui.platform.LocalContext // Contexto actual de Android
import androidx.compose.ui.res.painterResource // Cargador de drawable local
import androidx.compose.ui.text.font.FontWeight // Peso tipográfico
import androidx.compose.ui.unit.dp // Unidad de pixeles independientes
import androidx.compose.ui.unit.sp // Unidad para textos
import androidx.compose.ui.window.Dialog // Diálogo nativo modal
import androidx.compose.ui.window.DialogProperties // Configuración de diálogos
import coil.compose.AsyncImage // Cargador de imágenes Coil
import com.example.R // Recursos del proyecto
import com.example.model.HabitItem // Modelo de hábito
import com.example.ui.theme.CardWhite // Token de color
import com.example.ui.theme.PrimaryTeal // Token de color
import com.example.ui.theme.SecondaryTeal // Token de color
import com.example.ui.theme.TextDark // Token de color
import com.example.ui.components.ScreenBackground // Token de fondo
import com.example.ui.components.TextGray // Token de color
import com.example.ui.components.getCategoryBgColor // Utilidad de color
import com.example.ui.components.getCategoryIcon // Utilidad de ícono

/**
 * CategoryOption define la estructura básica de una categoría seleccionable.
 */
data class CategoryOption(
    val id: String, // ID único
    val name: String // Nombre de la categoría
)

// Lista de categorías estáticas para la creación de hábitos (mínimo de 10 elementos)
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
 * AddHabitScreen es un diálogo a pantalla completa para la creación y configuración de nuevos hábitos.
 *
 * - **¿Para qué sirve?** Captura los parámetros (nombre, categoría, frecuencia, progreso, etc.) y genera un objeto `HabitItem`.
 * - **¿Cómo funciona?**
 *   1. Utiliza estados locales reactivos (`remember { mutableStateOf(...) }`) para cada campo del formulario.
 *   2. Despliega un modal a pantalla completa usando el composable `Dialog`.
 *   3. Llama al callback `onSaveHabit` al presionar "Guardar" y valida que el nombre no esté en blanco.
 * - **¿Qué pasa si lo quitamos?** Los usuarios no podrían crear nuevos hábitos ni personalizarlos.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AddHabitScreen(
    onDismiss: () -> Unit, // Callback para cerrar el diálogo
    onSaveHabit: (HabitItem) -> Unit // Callback para pasar el hábito nuevo al MainActivity
) {
    val context = LocalContext.current // Contexto local de Android
    var habitName by remember { mutableStateOf("") } // Estado del campo Nombre del Hábito
    var selectedCategory by remember { mutableStateOf("Hidratación") } // Estado de la categoría activa
    var isDailyFrequency by remember { mutableStateOf(true) } // Estado del botón de frecuencia
    var isProgressive by remember { mutableStateOf(false) } // Estado para saber si es progresivo
    var dailyGoalText by remember { mutableStateOf("") } // Texto del campo de meta de progreso diaria
    var reminderTime by remember { mutableStateOf("08:00 AM") } // Hora seleccionada para el recordatorio
    var isReminderEnabled by remember { mutableStateOf(true) } // Interruptor de recordatorio activo
    var durationText by remember { mutableStateOf("00:30 horas") } // Texto de duración estimada

    // Crea el modal
    Dialog(
        onDismissRequest = onDismiss,
        properties = DialogProperties(usePlatformDefaultWidth = false) // Deshabilita ancho predeterminado para hacerlo inmersivo
    ) {
        Surface(
            modifier = Modifier.fillMaxSize(), // Rellena toda la pantalla
            color = ScreenBackground // Color de fondo dinámico adaptable
        ) {
            Column(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(16.dp) // Relleno simétrico múltiplo de 4dp
                    .verticalScroll(rememberScrollState()), // Habilita el desplazamiento vertical si la pantalla es muy pequeña
                verticalArrangement = Arrangement.spacedBy(16.dp) // Espaciado de 16dp
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
                            color = MaterialTheme.colorScheme.primary, // Teal interactivo
                            fontSize = 16.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }

                    Text(
                        text = "Añadir Nuevo Hábito",
                        fontSize = 18.sp,
                        fontWeight = FontWeight.Bold,
                        color = TextDark // Color según tema
                    )

                    Spacer(modifier = Modifier.width(48.dp)) // Espaciador decorativo para alinear al centro
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

                // Campo 2: Categoría (LazyRow horizontal de 10 elementos con keys estables)
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
                                        .background(getCategoryBgColor(cat.name), CircleShape) // Color según tema e ítem
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
                            .background(if (isSystemInDarkTheme()) Color(0xFF383838) else Color(0xFFE0E0E0), RoundedCornerShape(12.dp))
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

                // Campo 4: Hábito Progresivo (Interruptor Switch)
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

                // Campo condicional: Meta Diaria si el hábito es Progresivo
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

                // Campo 6: Duración estimada
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

                // Diálogo/Burbuja con la mascota Pulpo usando Coil y la foto local
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

                     AsyncImage( // Muestra la foto de pulpo local usando Coil desde la URL de GitHub
                        model = "https://raw.githubusercontent.com/garzzaro/Proyecto-Plataformas-Moviles/feature/lab2-lazy-list/app/src/main/res/drawable/pulpo.avif", // Carga la imagen de la mascota desde la URL de GitHub
                        placeholder = painterResource(id = R.drawable.ic_launcher_background), // Rúbrica: Placeholder de carga obligatorio
                        contentDescription = "Mascota Octo", // Descripción de accesibilidad
                        modifier = Modifier // Modificadores de diseño
                            .size(60.dp) // Tamaño cuadrado de 60dp
                            .clip(CircleShape), // Recorte circular
                        contentScale = ContentScale.Crop // Recorte proporcional
                    ) // Fin de AsyncImage
                } // Fin de Row

                Spacer(modifier = Modifier.height(8.dp))

                // Botón Guardar
                Button(
                    onClick = {
                        // Validación de nombre obligatorio
                        if (habitName.isBlank()) {
                            Toast.makeText(context, "Escribe el nombre del hábito", Toast.LENGTH_SHORT).show()
                            return@Button
                        }

                        // Instanciación del nuevo hábito inmutable
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

                        // Pasa el hábito y descarta modal
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
