package com.example.ui // Declaración del paquete al que pertenece este diálogo de formulario

import android.widget.Toast // Importa para mostrar mensajes flotantes en la pantalla
import androidx.compose.foundation.background // Importa modificador para pintar colores de fondo
import androidx.compose.foundation.border // Importa modificador para pintar bordes
import androidx.compose.foundation.clickable // Importa modificador para hacer clics en layouts
import androidx.compose.foundation.layout.Arrangement // Importa alineación de filas y columnas
import androidx.compose.foundation.layout.Box // Importa contenedor para apilar elementos Compose
import androidx.compose.foundation.layout.Column // Importa contenedor para alinear en vertical
import androidx.compose.foundation.layout.Row // Importa contenedor para alinear en horizontal
import androidx.compose.foundation.layout.Spacer // Importa componente separador de tamaño fijo
import androidx.compose.foundation.layout.fillMaxSize // Importa modificador para ocupar todo el tamaño
import androidx.compose.foundation.layout.fillMaxWidth // Importa modificador para ocupar el ancho total
import androidx.compose.foundation.layout.height // Importa modificador para altura fija
import androidx.compose.foundation.layout.padding // Importa modificador para márgenes internos
import androidx.compose.foundation.layout.size // Importa modificador para tamaños cuadrados
import androidx.compose.foundation.layout.width // Importa modificador para ancho fijo
import androidx.compose.foundation.lazy.LazyRow // Importa fila eficiente recicladora horizontal
import androidx.compose.foundation.lazy.items // Importa cargador dinámico de items con keys
import androidx.compose.foundation.rememberScrollState // Importa para recordar el estado de scroll
import androidx.compose.foundation.shape.CircleShape // Importa forma de círculo perfecto
import androidx.compose.foundation.shape.RoundedCornerShape // Importa forma con esquinas redondeadas
import androidx.compose.foundation.isSystemInDarkTheme // Importa detector de modo oscuro activo
import androidx.compose.foundation.verticalScroll // Importa modificador para scroll vertical
import androidx.compose.material3.Button // Importa botón interactivo de Material 3
import androidx.compose.material3.ButtonDefaults // Importa configuraciones por defecto de botones
import androidx.compose.material3.ExperimentalMaterial3Api // Importa anotación experimental de Material 3
import androidx.compose.material3.MaterialTheme // Importa acceso al tema activo de la app
import androidx.compose.material3.Icon // Importa componente gráfico para dibujar vectores
import androidx.compose.material3.OutlinedTextField // Importa campo de texto con borde delineado
import androidx.compose.material3.OutlinedTextFieldDefaults // Importa configuraciones de campos de texto
import androidx.compose.material3.Surface // Importa contenedor de elevaciones y sombras M3
import androidx.compose.material3.Switch // Importa interruptor deslizable activo/inactivo
import androidx.compose.material3.SwitchDefaults // Importa configuraciones de color de switches
import androidx.compose.material3.Text // Importa componente de texto de Material 3
import androidx.compose.material3.TextButton // Importa botón plano sin contornos
import androidx.compose.runtime.Composable // Importa anotación obligatoria de Compose
import androidx.compose.runtime.getValue // Importa delegado para lectura de estados
import androidx.compose.runtime.mutableStateOf // Importa inicializador de estados mutables
import androidx.compose.runtime.remember // Importa persistidor de estados en recomposición
import androidx.compose.runtime.setValue // Importa delegado para escritura de estados
import androidx.compose.ui.Alignment // Importa alineación espacial en Compose
import androidx.compose.ui.Modifier // Importa clase base para modificadores de UI
import androidx.compose.ui.draw.clip // Importa recortador de formas geométricas
import androidx.compose.ui.graphics.Color // Importa objeto para valores de color
import androidx.compose.ui.layout.ContentScale // Importa escala de ajuste de imágenes
import androidx.compose.ui.platform.LocalContext // Importa acceso al contexto de Android
import androidx.compose.ui.res.painterResource // Importa cargador de recursos locales
import androidx.compose.ui.text.font.FontWeight // Importa modificador de grosor de tipografía
import androidx.compose.ui.unit.dp // Importa unidad de pixeles independientes
import androidx.compose.ui.unit.sp // Importa unidad de tamaño de texto
import androidx.compose.ui.window.Dialog // Importa diálogo modal nativo inmersivo
import androidx.compose.ui.window.DialogProperties // Importa propiedades de configuración modal
import coil.compose.AsyncImage // Importa cargador asíncrono Coil
import com.example.R // Importa recursos auto-generados R
import com.example.model.HabitItem // Importa el modelo de datos de hábitos
import com.example.ui.theme.CardWhite // Importa color adaptable de tarjeta
import com.example.ui.theme.PrimaryTeal // Importa color Teal oscuro
import com.example.ui.theme.SecondaryTeal // Importa color turquesa brillante
import com.example.ui.theme.TextDark // Importa color adaptable para texto
import com.example.ui.components.ScreenBackground // Importa color adaptable para el fondo
import com.example.ui.components.TextGray // Importa color adaptable gris
import com.example.ui.components.getCategoryBgColor // Importa utilidad de color de categoría
import com.example.ui.components.getCategoryIcon // Importa utilidad de ícono de categoría

/**
 * CategoryOption define la estructura básica de una categoría seleccionable.
 */
data class CategoryOption( // Declara data class para opciones de categoría
    val id: String, // ID único del elemento
    val name: String // Nombre de la categoría
) // Fin de CategoryOption

// Lista de categorías estáticas para la creación de hábitos (mínimo de 10 elementos)
val expandedCategories = listOf( // Inicializa lista inmutable
    CategoryOption("1", "Hidratación"), // Categoría 1
    CategoryOption("2", "Aprendizaje"), // Categoría 2
    CategoryOption("3", "Ejercicio"), // Categoría 3
    CategoryOption("4", "Mindfulness"), // Categoría 4
    CategoryOption("5", "Productividad"), // Categoría 5
    CategoryOption("6", "Nutrición"), // Categoría 6
    CategoryOption("7", "Sueño"), // Categoría 7
    CategoryOption("8", "Arte y Creatividad"), // Categoría 8
    CategoryOption("9", "Finanzas"), // Categoría 9
    CategoryOption("10", "Salud Mental") // Categoría 10
) // Fin de expandedCategories

/**
 * AddHabitScreen es un diálogo a pantalla completa para la creación y configuración de nuevos hábitos.
 */
@OptIn(ExperimentalMaterial3Api::class) // Habilita APIs experimentales de Material 3
@Composable // Anotación de Composable
fun AddHabitScreen( // Firma de la función
    onDismiss: () -> Unit, // Callback para cerrar diálogo
    onSaveHabit: (HabitItem) -> Unit // Callback para pasar el hábito guardado
) { // Inicio cuerpo de función
    val context = LocalContext.current // Contexto del sistema de Android
    var habitName by remember { mutableStateOf("") } // Estado del nombre de hábito
    var selectedCategory by remember { mutableStateOf("Hidratación") } // Estado de categoría seleccionada
    var isDailyFrequency by remember { mutableStateOf(true) } // Estado del tipo de frecuencia
    var isProgressive by remember { mutableStateOf(false) } // Estado de hábito progresivo
    var dailyGoalText by remember { mutableStateOf("") } // Estado de la meta diaria en texto
    var reminderTime by remember { mutableStateOf("08:00 AM") } // Estado de la hora del recordatorio
    var isReminderEnabled by remember { mutableStateOf(true) } // Estado de interruptor de recordatorio
    var durationText by remember { mutableStateOf("00:30 horas") } // Estado del texto de duración

    Dialog( // Diálogo nativo de pantalla completa
        onDismissRequest = onDismiss, // Cierra al presionar fuera o atrás
        properties = DialogProperties(usePlatformDefaultWidth = false) // Deshabilita ancho predeterminado de Android
    ) { // Inicio diálogo
        Surface( // Contenedor plano inmersivo
            modifier = Modifier.fillMaxSize(), // Rellena toda la pantalla
            color = ScreenBackground // Color de fondo adaptable al tema
        ) { // Inicio Surface
            Column( // Contenedor vertical principal
                modifier = Modifier // Modificadores de la columna
                    .fillMaxSize() // Ocupa todo el espacio disponible
                    .padding(16.dp) // Relleno interno de 16dp
                    .verticalScroll(rememberScrollState()), // Habilita scroll vertical
                verticalArrangement = Arrangement.spacedBy(16.dp) // Espaciado de 16dp entre elementos
            ) { // Inicio Column
                // Cabecera: Cancelar - Añadir Nuevo Hábito
                Row( // Fila horizontal de cabecera
                    modifier = Modifier // Modificadores de Row
                        .fillMaxWidth() // Ancho completo
                        .padding(top = 8.dp), // Margen superior de 8dp
                    horizontalArrangement = Arrangement.SpaceBetween, // Separa los extremos de la cabecera
                    verticalAlignment = Alignment.CenterVertically // Centrado vertical de textos
                ) { // Inicio Row cabecera
                    TextButton(onClick = onDismiss) { // Botón interactivo de cancelación
                        Text( // Componente de texto para botón cancelar
                            text = "Cancelar", // Texto legible
                            color = MaterialTheme.colorScheme.primary, // Color primario
                            fontSize = 16.sp, // Tamaño de letra
                            fontWeight = FontWeight.Bold // Grosor negrita
                        ) // Fin de Text
                    } // Fin de TextButton

                    Text( // Título centrado del formulario
                        text = "Añadir Nuevo Hábito", // Texto
                        fontSize = 18.sp, // Tamaño de letra
                        fontWeight = FontWeight.Bold, // Grosor negrita
                        color = TextDark // Color adaptable según tema
                    ) // Fin de Text

                    Spacer(modifier = Modifier.width(48.dp)) // Espaciador invisible para alinear el título
                } // Fin de Row cabecera

                // Campo 1: Nombre del Hábito
                Column { // Columna para etiqueta y campo de texto
                    Text( // Etiqueta del campo
                        text = "Nombre del Hábito", // Texto informativo
                        fontSize = 14.sp, // Tamaño de letra
                        fontWeight = FontWeight.Bold, // Grosor negrita
                        color = TextDark // Color adaptable
                    ) // Fin de Text
                    Spacer(modifier = Modifier.height(6.dp)) // Espaciador de 6dp
                    OutlinedTextField( // Campo de entrada delineado
                        value = habitName, // Vincula variable de estado
                        onValueChange = { habitName = it }, // Actualiza estado en cambios
                        placeholder = { Text("ej., Leer un libro", color = Color.Gray) }, // Texto de ayuda gris
                        modifier = Modifier.fillMaxWidth(), // Ancho completo
                        shape = RoundedCornerShape(12.dp), // Esquinas redondeadas a 12dp
                        colors = OutlinedTextFieldDefaults.colors( // Define colores adaptables del campo
                            focusedContainerColor = CardWhite, // Fondo activo
                            unfocusedContainerColor = CardWhite, // Fondo inactivo
                            focusedBorderColor = MaterialTheme.colorScheme.primary, // Borde activo
                            unfocusedBorderColor = Color(0xFFE0E0E0) // Borde gris inactivo
                        ) // Fin de colors
                    ) // Fin de OutlinedTextField
                } // Fin de Column de campo 1

                // Campo 2: Categoría (LazyRow horizontal de 10 elementos con keys estables)
                Column { // Columna para etiqueta y selector
                    Text( // Etiqueta
                        text = "Categoría", // Texto informativo
                        fontSize = 14.sp, // Tamaño de letra
                        fontWeight = FontWeight.Bold, // Grosor
                        color = TextDark // Color
                    ) // Fin de Text
                    Spacer(modifier = Modifier.height(10.dp)) // Espaciador de 10dp

                    LazyRow( // Fila de desplazamiento horizontal de categorías
                        horizontalArrangement = Arrangement.spacedBy(16.dp) // Espaciado de 16dp
                    ) { // Inicio LazyRow
                        items(items = expandedCategories, key = { it.id }) { cat -> // Carga lista con keys estables
                            val isSelected = selectedCategory == cat.name // Evalúa si la categoría está seleccionada
                            Column( // Columna de categoría individual
                                horizontalAlignment = Alignment.CenterHorizontally, // Centrado horizontal
                                modifier = Modifier.clickable { selectedCategory = cat.name } // Actualiza selección en clic
                            ) { // Inicio Column categoría
                                Box( // Círculo de color de fondo
                                    modifier = Modifier // Modificadores de la burbuja
                                        .size(56.dp) // Tamaño cuadrado
                                        .background(getCategoryBgColor(cat.name), CircleShape) // Fondo circular
                                        .border( // Borde si está activo
                                            width = if (isSelected) 3.dp else 0.dp, // Grosor
                                            color = if (isSelected) MaterialTheme.colorScheme.primary else Color.Transparent, // Color
                                            shape = CircleShape // Circular
                                        ), // Fin de border
                                    contentAlignment = Alignment.Center // Centra el icono internamente
                                ) { // Inicio Box de burbuja
                                    Icon( // Ícono vectorial de la categoría
                                        imageVector = getCategoryIcon(cat.name), // Obtiene vector correspondiente
                                        contentDescription = cat.name, // Descripción
                                        tint = MaterialTheme.colorScheme.primary, // Color
                                        modifier = Modifier.size(28.dp) // Tamaño de 28dp
                                    ) // Fin de Icon
                                } // Fin de Box de burbuja
                                Spacer(modifier = Modifier.height(6.dp)) // Espaciador
                                Text( // Nombre legible de la categoría
                                    text = cat.name, // Texto
                                    fontSize = 12.sp, // Tamaño
                                    fontWeight = if (isSelected) FontWeight.Bold else FontWeight.Medium, // Negrita si activo
                                    color = if (isSelected) MaterialTheme.colorScheme.primary else TextDark // Color interactivo
                                ) // Fin de Text
                            } // Fin de Column categoría
                        } // Fin de items
                    } // Fin de LazyRow
                } // Fin de Column de campo 2

                // Campo 3: Frecuencia (Diario / Semanal)
                Column { // Columna para etiqueta y selector
                    Text( // Etiqueta
                        text = "Frecuencia", // Texto informativo
                        fontSize = 14.sp, // Tamaño
                        fontWeight = FontWeight.Bold, // Grosor
                        color = TextDark // Color
                    ) // Fin de Text
                    Spacer(modifier = Modifier.height(6.dp)) // Espaciador de 6dp
                    Row( // Fila contenedora de los dos selectores
                        modifier = Modifier // Modificadores
                            .fillMaxWidth() // Ancho completo
                            .background(if (isSystemInDarkTheme()) Color(0xFF383838) else Color(0xFFE0E0E0), RoundedCornerShape(12.dp)) // Fondo adaptable
                            .padding(4.dp) // Relleno mínimo
                    ) { // Inicio de Row frecuencia
                        Box( // Botón selector Diario
                            modifier = Modifier // Modificadores
                                .weight(1f) // Ocupa la mitad
                                .height(40.dp) // Altura de 40dp
                                .background( // Fondo de tarjeta activa o inactiva
                                    if (isDailyFrequency) CardWhite else Color.Transparent, // Color
                                    RoundedCornerShape(8.dp) // Esquinas redondeadas
                                ) // Fin de background
                                .clickable { isDailyFrequency = true }, // Cambia a Diario en clic
                            contentAlignment = Alignment.Center // Centrado interno
                        ) { // Inicio Box Diario
                            Text( // Texto Diario
                                text = "Diario", // Texto
                                fontWeight = FontWeight.Bold, // Negrita
                                color = TextDark // Color adaptable
                            ) // Fin de Text
                        } // Fin de Box Diario

                        Box( // Botón selector Semanal
                            modifier = Modifier // Modificadores
                                .weight(1f) // Ocupa la mitad
                                .height(40.dp) // Altura de 40dp
                                .background( // Fondo activo o inactivo
                                    if (!isDailyFrequency) CardWhite else Color.Transparent, // Color
                                    RoundedCornerShape(8.dp) // Redondeado
                                ) // Fin de background
                                .clickable { isDailyFrequency = false }, // Cambia a Semanal en clic
                            contentAlignment = Alignment.Center // Centrado
                        ) { // Inicio Box Semanal
                            Text( // Texto Semanal
                                text = "Semanal", // Texto
                                fontWeight = FontWeight.Bold, // Negrita
                                color = TextDark // Color
                            ) // Fin de Text
                        } // Fin de Box Semanal
                    } // Fin de Row frecuencia
                } // Fin de Column de campo 3

                // Campo 4: Hábito Progresivo (Switch)
                Row( // Fila horizontal de control progresivo
                    modifier = Modifier.fillMaxWidth(), // Ancho completo
                    horizontalArrangement = Arrangement.SpaceBetween, // Separa a los extremos
                    verticalAlignment = Alignment.CenterVertically // Centrado vertical
                ) { // Inicio de Row progresivo
                    Column(modifier = Modifier.weight(1f)) { // Columna para textos descriptivos
                        Text( // Etiqueta
                            text = "Hábito Progresivo", // Texto
                            fontSize = 14.sp, // Tamaño
                            fontWeight = FontWeight.Bold, // Negrita
                            color = TextDark // Color adaptable
                        ) // Fin de Text
                        Text( // Subtexto explicativo
                            text = "(ej., meta de 8 vasos de agua)", // Texto
                            fontSize = 12.sp, // Tamaño
                            color = TextGray // Gris adaptable
                        ) // Fin de Text
                    } // Fin de Column informativa
                    Switch( // Interruptor deslizable
                        checked = isProgressive, // Vincula estado
                        onCheckedChange = { isProgressive = it }, // Alterna valor
                        colors = SwitchDefaults.colors(checkedTrackColor = SecondaryTeal) // Color turquesa al activarse
                    ) // Fin de Switch
                } // Fin de Row progresivo

                // Campo condicional: Meta Diaria si el hábito es Progresivo
                if (isProgressive) { // Evalúa estado progresivo
                    Column { // Contenedor vertical
                        Text( // Etiqueta
                            text = "Meta Diaria", // Texto informativo
                            fontSize = 14.sp, // Tamaño
                            fontWeight = FontWeight.Bold, // Negrita
                            color = TextDark // Color adaptable
                        ) // Fin de Text
                        Spacer(modifier = Modifier.height(6.dp)) // Espaciador
                        OutlinedTextField( // Entrada de meta de progreso diaria
                            value = dailyGoalText, // Estado
                            onValueChange = { dailyGoalText = it }, // Actualiza estado
                            placeholder = { Text("ej., 8 vasos", color = Color.Gray) }, // Ayuda
                            modifier = Modifier.fillMaxWidth(), // Ancho completo
                            shape = RoundedCornerShape(12.dp), // Redondeado
                            colors = OutlinedTextFieldDefaults.colors( // Colores adaptables
                                focusedContainerColor = CardWhite, // Fondo
                                unfocusedContainerColor = CardWhite, // Fondo
                                focusedBorderColor = MaterialTheme.colorScheme.primary, // Borde
                                unfocusedBorderColor = Color(0xFFE0E0E0) // Borde
                            ) // Fin de colors
                        ) // Fin de OutlinedTextField
                    } // Fin de Column meta diaria
                } // Fin de condicional progresivo

                // Campo 5: Recordatorio
                Row( // Fila horizontal de hora de recordatorio
                    modifier = Modifier.fillMaxWidth(), // Ancho completo
                    horizontalArrangement = Arrangement.SpaceBetween, // Extremos
                    verticalAlignment = Alignment.CenterVertically // Centrado
                ) { // Inicio Row recordatorio
                    Column(modifier = Modifier.weight(1f)) { // Datos de hora
                        Text( // Etiqueta
                            text = "Hora de Recordatorio", // Texto
                            fontSize = 14.sp, // Tamaño
                            fontWeight = FontWeight.Bold, // Negrita
                            color = TextDark // Color
                        ) // Fin de Text
                        Spacer(modifier = Modifier.height(4.dp)) // Espaciador
                        Box( // Contenedor de texto de hora
                            modifier = Modifier // Modificadores
                                .background(Color(0xFFE8EAF6), RoundedCornerShape(8.dp)) // Fondo lila suave
                                .padding(horizontal = 12.dp, vertical = 8.dp) // Relleno
                        ) { // Inicio Box hora
                            Text(text = reminderTime, fontWeight = FontWeight.Bold, color = TextDark) // Muestra hora
                        } // Fin Box hora
                    } // Fin de Column datos
                    Switch( // Interruptor de recordatorio activo
                        checked = isReminderEnabled, // Vincula estado
                        onCheckedChange = { isReminderEnabled = it }, // Alterna valor
                        colors = SwitchDefaults.colors(checkedTrackColor = SecondaryTeal) // Color
                    ) // Fin de Switch
                } // Fin Row recordatorio

                // Campo 6: Duración estimada
                Column { // Contenedor vertical
                    Text( // Etiqueta
                        text = "Duración / Tiempo", // Texto informativo
                        fontSize = 14.sp, // Tamaño
                        fontWeight = FontWeight.Bold, // Negrita
                        color = TextDark // Color
                    ) // Fin de Text
                    Spacer(modifier = Modifier.height(6.dp)) // Espaciador
                    Box( // Contenedor del texto de duración
                        modifier = Modifier // Modificadores
                            .fillMaxWidth() // Ancho completo
                            .background(Color(0xFFE8EAF6), RoundedCornerShape(12.dp)) // Fondo
                            .padding(14.dp) // Relleno
                    ) { // Inicio Box duración
                        Text(text = durationText, fontWeight = FontWeight.Medium, color = TextDark) // Texto
                    } // Fin Box duración
                } // Fin de Column campo 6

                Spacer(modifier = Modifier.height(8.dp)) // Espacio vertical decorativo

                // Diálogo/Burbuja con la mascota Pulpo usando Coil y la foto local desde la URL de GitHub
                Row( // Fila horizontal para alinear la burbuja y el avatar del pulpo
                    modifier = Modifier.fillMaxWidth(), // Ancho completo
                    horizontalArrangement = Arrangement.End, // Alineado al extremo derecho
                    verticalAlignment = Alignment.Bottom // Alineación inferior
                ) { // Inicio de Row mascota
                    val isDark = isSystemInDarkTheme() // Comprueba tema oscuro del sistema
                    Box( // Burbuja de diálogo del pulpo
                        modifier = Modifier // Modificadores
                            .background(if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA), RoundedCornerShape(16.dp)) // Color adaptable
                            .padding(horizontal = 14.dp, vertical = 10.dp) // Relleno interno
                    ) { // Inicio Box burbuja
                        Text( // Texto motivacional de la mascota
                            text = "¡Construyamos un nuevo hábito!", // Mensaje en español
                            fontSize = 13.sp, // Tamaño de letra
                            fontWeight = FontWeight.Bold, // Negrita
                            color = if (isDark) SecondaryTeal else PrimaryTeal // Color adaptable
                        ) // Fin de Text
                    } // Fin de Box burbuja

                    Spacer(modifier = Modifier.width(8.dp)) // Espaciador de separación de 8dp

                     AsyncImage( // Muestra la foto de pulpo usando Coil desde la URL de GitHub
                        model = "https://raw.githubusercontent.com/garzzaro/Proyecto-Plataformas-Moviles/feature/lab2-lazy-list/app/src/main/res/drawable/pulpo.avif", // Carga la imagen de la mascota desde la URL de GitHub
                        placeholder = painterResource(id = R.drawable.ic_launcher_background), // Rúbrica: Placeholder de carga obligatorio
                        contentDescription = "Mascota Octo", // Descripción de accesibilidad
                        modifier = Modifier // Modificadores de diseño
                            .size(60.dp) // Tamaño cuadrado de 60dp
                            .clip(CircleShape), // Recorte circular
                        contentScale = ContentScale.Crop // Recorte proporcional
                    ) // Fin de AsyncImage
                } // Fin de Row mascota

                Spacer(modifier = Modifier.height(8.dp)) // Espaciador vertical decorativo

                // Botón Guardar
                Button( // Botón de confirmación y guardado
                    onClick = { // Acción interactiva al presionarlo
                        // Validación de nombre obligatorio
                        if (habitName.isBlank()) { // Evalúa si el texto está vacío
                            Toast.makeText(context, "Escribe el nombre del hábito", Toast.LENGTH_SHORT).show() // Muestra alerta
                            return@Button // Cancela la acción
                        } // Fin validación

                        // Instanciación del nuevo hábito inmutable
                        val newHabit = HabitItem( // Crea el objeto
                            id = System.currentTimeMillis().toString(), // ID único basado en milisegundos de reloj
                            title = habitName, // Nombre ingresado
                            subtitle = if (isProgressive) dailyGoalText.ifBlank { "Meta personalizada" } else "Hábito diario", // Meta descriptiva
                            category = selectedCategory, // Categoría activa
                            frequency = if (isDailyFrequency) "Diario" else "Semanal", // Tipo de frecuencia
                            streak = 1, // Racha inicial en 1
                            isProgressive = isProgressive, // Modo progresivo
                            currentProgress = if (isProgressive) 1 else 0, // Avance inicial
                            maxProgress = if (isProgressive) 8 else 1, // Límite de progreso
                            isCompleted = false, // Inicia sin completar el día
                            tags = listOf(if (isDailyFrequency) "Diario" else "Semanal", selectedCategory) // Etiquetas auto-generadas
                        ) // Fin instanciación

                        // Pasa el hábito y descarta modal
                        onSaveHabit(newHabit) // Envía el hábito al MainActivity
                        Toast.makeText(context, "¡Hábito '$habitName' guardado!", Toast.LENGTH_SHORT).show() // Feedback
                        onDismiss() // Cierra el modal de creación
                    }, // Fin click
                    modifier = Modifier // Modificadores del botón
                        .fillMaxWidth() // Ancho completo
                        .height(52.dp), // Altura fija de 52dp
                    colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal), // Fondo turquesa brillante
                    shape = RoundedCornerShape(26.dp) // Bordes ovalados a 26dp
                ) { // Cuerpo del botón
                    Text( // Texto del botón
                        text = "Guardar Hábito", // Texto legible
                        fontSize = 16.sp, // Tamaño de letra
                        fontWeight = FontWeight.Bold, // Letra gruesa
                        color = TextDark // Color adaptable según tema
                    ) // Fin de Text
                } // Fin de Button

                Spacer(modifier = Modifier.height(16.dp)) // Espaciador inferior final de seguridad
            } // Fin de Column principal
        } // Fin de Surface
    } // Fin de Dialog modal
} // Fin del composable AddHabitScreen
