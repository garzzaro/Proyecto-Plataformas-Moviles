package com.example.ui // Declaración del paquete de interfaz de usuario del proyecto

import android.widget.Toast // Importa la clase para mostrar mensajes emergentes rápidos
import androidx.compose.foundation.background // Modificador para definir colores de fondo
import androidx.compose.foundation.clickable // Modificador para que los elementos sean interactivos
import androidx.compose.foundation.layout.Arrangement // Configuración de alineación de filas y columnas
import androidx.compose.foundation.layout.Box // Contenedor básico para apilar elementos uno sobre otro
import androidx.compose.foundation.layout.Column // Contenedor vertical secuencial
import androidx.compose.foundation.layout.PaddingValues // Representación de paddings por dirección
import androidx.compose.foundation.layout.Row // Contenedor horizontal secuencial
import androidx.compose.foundation.layout.Spacer // Elemento para crear espacios de separación fijos
import androidx.compose.foundation.layout.fillMaxSize // Modificador para ocupar el 100% de la pantalla
import androidx.compose.foundation.layout.fillMaxWidth // Modificador para ocupar el ancho total horizontal
import androidx.compose.foundation.layout.height // Modificador para definir una altura fija
import androidx.compose.foundation.layout.padding // Modificador para márgenes y paddings
import androidx.compose.foundation.layout.size // Modificador para dimensiones cuadradas
import androidx.compose.foundation.layout.width // Modificador para definir un ancho fijo
import androidx.compose.foundation.lazy.LazyColumn // Columna eficiente recicladora para listas largas
import androidx.compose.foundation.lazy.items // Método para cargar listas dinámicas con keys
import androidx.compose.foundation.shape.CircleShape // Recorte en forma de círculo perfecto
import androidx.compose.foundation.shape.RoundedCornerShape // Recorte de esquinas redondeadas
import androidx.compose.material.icons.Icons // Contenedor de íconos por defecto de Google
import androidx.compose.material.icons.outlined.CalendarToday // Ícono de calendario delineado
import androidx.compose.material.icons.outlined.CheckCircle // Ícono de check en círculo
import androidx.compose.material3.Card // Componente de tarjeta de Material 3
import androidx.compose.material3.CardDefaults // Configuraciones por defecto de tarjetas
import androidx.compose.material3.Icon // Componente para dibujar vectores gráficos
import androidx.compose.material3.Text // Componente para mostrar textos en pantalla
import androidx.compose.runtime.Composable // Anotación Compose obligatoria
import androidx.compose.runtime.getValue // Delegado de lectura de estados de Compose
import androidx.compose.runtime.mutableStateOf // Inicialización de estados reactivos
import androidx.compose.runtime.remember // Persiste estado en recomposiciones de UI
import androidx.compose.runtime.setValue // Delegado de escritura de estados de Compose
import androidx.compose.ui.Alignment // Alineación y posiciones de Compose
import androidx.compose.foundation.isSystemInDarkTheme // Detecta si el modo oscuro está activo
import androidx.compose.ui.Modifier // Modificador de Compose para diseño y atributos
import androidx.compose.ui.draw.clip // Modificador para recortar formas externas
import androidx.compose.ui.graphics.Color // Representación de colores cromáticos
import androidx.compose.ui.layout.ContentScale // Método de ajuste de escala de imágenes
import androidx.compose.ui.platform.LocalContext // Acceso al contexto del sistema operativo
import androidx.compose.ui.res.painterResource // Cargador de drawables locales del proyecto
import androidx.compose.ui.text.font.FontWeight // Modificador de grosor de tipografía
import androidx.compose.ui.unit.dp // Pixeles independientes de densidad para tamaños
import androidx.compose.ui.unit.sp // Pixeles independientes de escala para textos
import coil.compose.AsyncImage // Cargador de imágenes asíncronas de Coil
import com.example.R // Recursos locales auto-generados de la app
import com.example.model.HabitHistoryItem // Importa el modelo de datos de historial
import com.example.model.HabitItem // Importa el modelo de datos de hábitos
import com.example.ui.theme.CardWhite // Importa color de tarjeta
import com.example.ui.components.HabitHistoryItemRow // Importa fila de historial
import com.example.ui.components.HabitItemCard // Importa tarjeta de hábitos
import com.example.ui.components.MascotBannerCard // Importa banner motivador
import com.example.ui.components.NewHabitButtonCard // Importa botón de nuevo hábito
import com.example.ui.theme.PrimaryTeal // Importa color Teal corporativo
import com.example.ui.theme.TextDark // Importa color oscuro de texto
import com.example.ui.components.ScreenBackground // Importa color de fondo de pantalla
import com.example.ui.components.TextGray // Importa color gris de texto
import com.example.ui.components.WeeklyProgressCard // Importa componente semanal
import com.example.TopNavigationBarSection // Importa la cabecera superior compartida desde el paquete principal

// Lista inicial de hábitos (data class inmutable, 11 elementos > 10 mínimos para scroll)
val initialHabitsList = listOf( // Declara lista estática de hábitos
    HabitItem( // Hábito 1
        id = "1", // Identificador
        title = "Beber Agua", // Nombre
        subtitle = "8 vasos al día", // Meta
        category = "Hidratación", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 7, // Racha
        isProgressive = true, // Es progresivo
        currentProgress = 5, // Progreso actual
        maxProgress = 8, // Meta máxima
        tags = listOf("Diario", "Hidratación") // Etiquetas
    ), // Fin hábito 1
    HabitItem( // Hábito 2
        id = "2", // Identificador
        title = "Leer 1 Página", // Nombre
        subtitle = "Racha: 12", // Subtítulo
        category = "Aprendizaje", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 12, // Racha
        isCompleted = true, // Completado
        tags = listOf("Lectura") // Etiquetas
    ), // Fin hábito 2
    HabitItem( // Hábito 3
        id = "3", // Identificador
        title = "Plancha 1 Min", // Nombre
        subtitle = "Ritual Mañanero", // Subtítulo
        category = "Ejercicio", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 8, // Racha
        timeOrDuration = "1 Min", // Duración
        tags = listOf("Alta Intensidad", "Fuerza") // Etiquetas
    ), // Fin hábito 3
    HabitItem( // Hábito 4
        id = "4", // Identificador
        title = "Meditación", // Nombre
        subtitle = "10 min al día", // Subtítulo
        category = "Mindfulness", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 15, // Racha
        isCompleted = true, // Completado
        tags = listOf("Mindfulness") // Etiquetas
    ), // Fin hábito 4
    HabitItem( // Hábito 5
        id = "5", // Identificador
        title = "Caminar 10,000 Pasos", // Nombre
        subtitle = "8,200 / 10,000", // Subtítulo
        category = "Ejercicio", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 4, // Racha
        isProgressive = true, // Es progresivo
        currentProgress = 8200, // Progreso actual
        maxProgress = 10000, // Meta máxima
        tags = listOf("Salud", "Diario") // Etiquetas
    ), // Fin hábito 5
    HabitItem( // Hábito 6
        id = "6", // Identificador
        title = "Estudiar Código", // Nombre
        subtitle = "45 min al día", // Subtítulo
        category = "Productividad", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 20, // Racha
        tags = listOf("Aprendizaje", "Código") // Etiquetas
    ), // Fin hábito 6
    HabitItem( // Hábito 7
        id = "7", // Identificador
        title = "Dormir 8 Horas", // Nombre
        subtitle = "Meta nocturna", // Subtítulo
        category = "Sueño", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 9, // Racha
        isCompleted = true, // Completado
        tags = listOf("Sueño", "Salud") // Etiquetas
    ), // Fin hábito 7
    HabitItem( // Hábito 8
        id = "8", // Identificador
        title = "Comer 2 Frutas", // Nombre
        subtitle = "Porción diaria", // Subtítulo
        category = "Nutrición", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 3, // Racha
        isProgressive = true, // Es progresivo
        currentProgress = 1, // Progreso
        maxProgress = 2, // Meta máxima
        tags = listOf("Nutrición") // Etiquetas
    ), // Fin hábito 8
    HabitItem( // Hábito 9
        id = "9", // Identificador
        title = "Escribir Diario", // Nombre
        subtitle = "Reflexión nocturna", // Subtítulo
        category = "Salud Mental", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 7, // Racha
        tags = listOf("Bienestar") // Etiquetas
    ), // Fin hábito 9
    HabitItem( // Hábito 10
        id = "10", // Identificador
        title = "Ahorrar $5", // Nombre
        subtitle = "Fondo personal", // Subtítulo
        category = "Finanzas", // Categoría
        frequency = "Semanal", // Frecuencia
        streak = 2, // Racha
        tags = listOf("Finanzas", "Ahorro") // Etiquetas
    ), // Fin hábito 10
    HabitItem( // Hábito 11
        id = "11", // Identificador
        title = "Práctica de Guitarra", // Nombre
        subtitle = "20 min al día", // Subtítulo
        category = "Arte y Creatividad", // Categoría
        frequency = "Diario", // Frecuencia
        streak = 5, // Racha
        tags = listOf("Música", "Arte") // Etiquetas
    ) // Fin hábito 11
) // Fin de la lista inicial

// Lista de Historial de Hábitos (12 elementos para scroll visible)
val sampleHistoryItems = listOf( // Declara lista estática del historial
    HabitHistoryItem("h1", "Beber Agua", "Lun, Oct 21", true), // Registro 1
    HabitHistoryItem("h2", "Leer 1 Página", "Dom, Oct 20", true), // Registro 2
    HabitHistoryItem("h3", "Plancha 1 Min", "Dom, Oct 20", true), // Registro 3
    HabitHistoryItem("h4", "Meditación", "Sáb, Oct 19", false), // Registro 4
    HabitHistoryItem("h5", "Caminar 10,000 Pasos", "Vie, Oct 18", true), // Registro 5
    HabitHistoryItem("h6", "Estudiar Código", "Jue, Oct 17", true), // Registro 6
    HabitHistoryItem("h7", "Dormir 8 Horas", "Mié, Oct 16", true), // Registro 7
    HabitHistoryItem("h8", "Comer 2 Frutas", "Mar, Oct 15", true), // Registro 8
    HabitHistoryItem("h9", "Escribir Diario", "Lun, Oct 14", false), // Registro 9
    HabitHistoryItem("h10", "Ahorrar $5", "Dom, Oct 13", true), // Registro 10
    HabitHistoryItem("h11", "Aprender Idioma", "Sáb, Oct 12", true), // Registro 11
    HabitHistoryItem("h12", "Pintar o Dibujar", "Vie, Oct 11", true) // Registro 12
) // Fin de la lista de historial

/**
 * TrackerScreen representa la pestaña unificada de rastreo de hábitos y estadísticas del Lab 2.
 */
@Composable // Anotación que indica que es un Composable de Compose
fun TrackerScreen( // Declaración de la pantalla del rastreador
    habits: List<HabitItem>, // Lista de hábitos activa enviada desde el Main
    onToggleHabit: (HabitItem) -> Unit, // Callback para alternar completado
    onOpenAddHabit: () -> Unit // Callback para abrir diálogo de nuevo hábito
) { // Inicio del cuerpo del composable
    val context = LocalContext.current // Obtiene contexto local de Android para Toasts

    LazyColumn( // Contenedor de desplazamiento vertical eficiente principal
        modifier = Modifier // Modificadores
            .fillMaxSize() // Ocupa toda la pantalla
            .background(ScreenBackground), // Fondo adaptable según modo claro/oscuro
        contentPadding = PaddingValues(horizontal = 20.dp, vertical = 12.dp), // Paddings simétricos
        verticalArrangement = Arrangement.spacedBy(16.dp) // Espaciado simétrico de 16dp entre cada bloque
    ) { // Inicio de items deslizables
        // 0. Cabecera superior compartida (Logo del pulpo y campana de notificaciones)
        item { // Ítem deslizable único para la cabecera
            TopNavigationBarSection() // Llama a la sección de cabecera superior
        } // Fin ítem deslizable

        // 1. Banner motivacional de Mascota
        item { // Ítem deslizable único
            MascotBannerCard( // Llama al banner de la mascota
                onVisitMascotClick = { // Acción al presionar el botón de visita
                    Toast.makeText(context, "¡Saludando a Octo! 🐙✨", Toast.LENGTH_SHORT).show() // Feedback
                } // Fin acción
            ) // Fin MascotBannerCard
        } // Fin ítem deslizable

        // 2. Encabezado de Sección: "Tus Hábitos" y "Editar"
        item { // Ítem deslizable único
            Row( // Disposición horizontal
                modifier = Modifier.fillMaxWidth(), // Ancho completo
                horizontalArrangement = Arrangement.SpaceBetween, // Separa los extremos
                verticalAlignment = Alignment.CenterVertically // Centrado vertical
            ) { // Inicio cuerpo Row
                Text( // Texto del encabezado
                    text = "Tus Hábitos", // Título
                    fontSize = 22.sp, // Tamaño
                    fontWeight = FontWeight.Bold, // Negrita
                    color = TextDark // Color según tema
                ) // Fin Text

                Box( // Botón para simular modo edición
                    modifier = Modifier // Modificadores
                        .background(if (isSystemInDarkTheme()) Color(0xFF383838) else Color(0xFFE0E0E0), RoundedCornerShape(12.dp)) // Fondo adaptable
                        .clickable { // Clic
                            Toast.makeText(context, "Modo edición activado", Toast.LENGTH_SHORT).show() // Feedback
                        } // Fin clic
                        .padding(horizontal = 14.dp, vertical = 6.dp) // Paddings
                ) { // Cuerpo Box
                    Text( // Texto interactivo
                        text = "Editar", // Etiqueta
                        fontSize = 13.sp, // Tamaño
                        fontWeight = FontWeight.Bold, // Negrita
                        color = TextDark // Color adaptable
                    ) // Fin Text
                } // Fin Box
            } // Fin Row
        } // Fin ítem deslizable

        // 3. Cuadro de Hábitos con Scroll interno para mostrar ~4 a la vez (Rúbrica de la Tarea)
        item { // Ítem deslizable único
            Card( // Tarjeta contenedora del scroll interno
                modifier = Modifier // Modificadores
                    .fillMaxWidth() // Ancho completo
                    .height(380.dp), // Altura fija calculada para mostrar exactamente 4 hábitos
                shape = RoundedCornerShape(20.dp), // Redondeado de esquinas
                colors = CardDefaults.cardColors(containerColor = CardWhite), // Fondo según tema
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Elevación sutil
            ) { // Cuerpo de la tarjeta
                LazyColumn( // Scroll interno
                    modifier = Modifier // Modificadores
                        .fillMaxSize() // Ocupa todo el espacio de la tarjeta
                        .padding(12.dp), // Relleno
                    verticalArrangement = Arrangement.spacedBy(10.dp), // Espacio de 10dp entre hábitos
                    contentPadding = PaddingValues(bottom = 8.dp) // Margen de seguridad inferior
                ) { // Cuerpo scroll interno
                    items(items = habits, key = { it.id }) { habit -> // Carga hábitos dinámicos con keys
                        HabitItemCard( // Tarjeta de cada hábito individual
                            habit = habit, // Datos
                            onToggleComplete = { onToggleHabit(it) }, // Acción de checkbox
                            onClickDetail = { // Acción de clic en tarjeta
                                Toast.makeText(context, "Detalle: ${it.title}", Toast.LENGTH_SHORT).show() // Feedback
                            } // Fin acción
                        ) // Fin HabitItemCard
                    } // Fin items
                } // Fin LazyColumn interno
            } // Fin Card
        } // Fin ítem deslizable

        // 4. Botón "+ Nuevo Hábito" (Por fuera del cuadro de hábitos)
        item { // Ítem deslizable único
            NewHabitButtonCard(onClick = onOpenAddHabit) // Llama al botón interactivo
        } // Fin ítem deslizable

        // 5. Progreso Semanal
        item { // Ítem deslizable único
            WeeklyProgressCard() // Llama al indicador semanal
        } // Fin ítem deslizable

        // ============================================================================
        // VISTAS DE ESTADÍSTICAS INTEGRADAS AL FINAL DE LA PANTALLA
        // ============================================================================
        
        // 6. Título Estadísticas
        item { // Ítem deslizable único
            Text( // Título general de estadísticas
                text = "Porcentajes Semanales de Cumplimiento", // Texto
                fontSize = 18.sp, // Tamaño
                fontWeight = FontWeight.Bold, // Negrita
                color = TextDark, // Color adaptable
                modifier = Modifier.padding(top = 8.dp) // Padding superior
            ) // Fin Text
        } // Fin ítem deslizable

        // 7. Gráfico de Porcentajes Semanales (Gráfico de barras decorativo)
        item { // Ítem deslizable único
            Card( // Tarjeta del gráfico
                modifier = Modifier.fillMaxWidth(), // Ancho completo
                shape = RoundedCornerShape(20.dp), // Esquinas redondeadas
                colors = CardDefaults.cardColors(containerColor = CardWhite), // Fondo según tema
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Elevación
            ) { // Cuerpo tarjeta
                Column( // Disposición vertical
                    modifier = Modifier.padding(18.dp) // Relleno interno
                ) { // Cuerpo columna
                    val days = listOf("L", "M", "M", "J", "V", "S", "D") // Lista de iniciales de días
                    val percentages = listOf(85, 90, 75, 95, 80, 60, 70) // Porcentajes simulados
                    val barColors = listOf( // Colores para cada barra
                        Color(0xFF42A5F5), // Azul
                        Color(0xFF00695C), // Teal oscuro
                        Color(0xFF00ACC1), // Turquesa
                        Color(0xFF26A69A), // Teal brillante
                        Color(0xFFFB8C00), // Naranja
                        Color(0xFFFFB74D), // Naranja claro
                        Color(0xFFF57C00) // Naranja oscuro
                    ) // Fin lista de colores

                    Row( // Disposición horizontal
                        modifier = Modifier // Modificadores
                            .fillMaxWidth() // Ancho completo
                            .height(140.dp), // Altura del gráfico
                        horizontalArrangement = Arrangement.SpaceBetween, // Alineado
                        verticalAlignment = Alignment.Bottom // Alinear al fondo para barras
                    ) { // Cuerpo Row
                        days.forEachIndexed { idx, day -> // Itera cada día e índice
                            val pct = percentages[idx] // Obtiene porcentaje
                            val barHeightRatio = pct / 100f // Ratio de altura

                            Column( // Columna de cada barra individual
                                horizontalAlignment = Alignment.CenterHorizontally, // Centrado horizontal
                                verticalArrangement = Arrangement.Bottom, // Alineado al fondo
                                modifier = Modifier.weight(1f) // Peso equitativo
                            ) { // Cuerpo columna barra
                                Text( // Muestra porcentaje de barra
                                    text = "$pct%", // Texto
                                    fontSize = 11.sp, // Tamaño
                                    fontWeight = FontWeight.Bold, // Negrita
                                    color = TextDark // Color adaptable
                                ) // Fin Text
                                Spacer(modifier = Modifier.height(4.dp)) // Espacio
                                Box( // Barra rectangular de progreso
                                    modifier = Modifier // Modificadores
                                        .width(26.dp) // Ancho fijo de barra
                                        .height((100 * barHeightRatio).dp) // Altura proporcional
                                        .background( // Color de fondo
                                            color = barColors[idx], // Color correspondiente
                                            shape = RoundedCornerShape(topStart = 6.dp, topEnd = 6.dp) // Redondeado superior
                                        ) // Fin background
                                ) // Fin Box
                                Spacer(modifier = Modifier.height(6.dp)) // Espacio
                                Text( // Inicial del día
                                    text = day, // Texto
                                    fontSize = 13.sp, // Tamaño
                                    fontWeight = FontWeight.Bold, // Negrita
                                    color = TextDark // Color adaptable
                                ) // Fin Text
                            } // Fin Column barra
                        } // Fin iteración
                    } // Fin Row
                } // Fin Column
            } // Fin Card
        } // Fin ítem deslizable

        // 8. Desglose (Breakdown) de Progreso
        item { // Ítem deslizable único
            Column { // Disposición vertical
                Row( // Disposición horizontal de título e ícono del pulpo
                    modifier = Modifier.fillMaxWidth(), // Ancho completo
                    horizontalArrangement = Arrangement.SpaceBetween, // Separa los extremos
                    verticalAlignment = Alignment.CenterVertically // Centrado vertical
                ) { // Cuerpo Row
                    Text( // Título del desglose
                        text = "Desglose del Progreso", // Texto
                        fontSize = 18.sp, // Tamaño
                        fontWeight = FontWeight.Bold, // Negrita
                        color = TextDark // Color adaptable
                    ) // Fin Text

                     AsyncImage( // Muestra la foto de pulpo local usando Coil desde la URL de GitHub
                        model = "https://raw.githubusercontent.com/garzzaro/Proyecto-Plataformas-Moviles/feature/lab2-lazy-list/app/src/main/res/drawable/pulpo.avif", // Carga la imagen de la mascota desde la URL de GitHub
                        placeholder = painterResource(id = R.drawable.ic_launcher_background), // Rúbrica: Placeholder de carga obligatorio
                        contentDescription = "Mascota Octo", // Descripción de accesibilidad
                        modifier = Modifier // Modificadores de diseño
                            .size(36.dp) // Tamaño cuadrado de 36dp
                            .clip(CircleShape), // Recorte circular
                        contentScale = ContentScale.Crop // Recorte proporcional
                    ) // Fin de AsyncImage
                } // Fin de Row

                Spacer(modifier = Modifier.height(10.dp)) // Espacio

                Row( // Fila para contener las dos tarjetas de métricas
                    modifier = Modifier.fillMaxWidth(), // Ancho completo
                    horizontalArrangement = Arrangement.spacedBy(12.dp) // Espaciado de 12dp
                ) { // Cuerpo Row
                    // Card 1: Completadas
                    Card( // Tarjeta izquierda
                        modifier = Modifier.weight(1f), // Ancho 50%
                        shape = RoundedCornerShape(18.dp), // Esquinas redondeadas
                        colors = CardDefaults.cardColors(containerColor = CardWhite), // Fondo adaptable
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Elevación
                    ) { // Cuerpo tarjeta
                        Column( // Disposición vertical
                            modifier = Modifier.padding(16.dp) // Relleno
                        ) { // Cuerpo columna
                            Row( // Fila de cabecera de tarjeta
                                modifier = Modifier.fillMaxWidth(), // Ancho completo
                                horizontalArrangement = Arrangement.SpaceBetween, // Separado
                                verticalAlignment = Alignment.Top // Alineado arriba
                            ) { // Cuerpo Row
                                Text( // Etiqueta completados
                                    text = "Completados", // Texto
                                    fontSize = 12.sp, // Tamaño
                                    fontWeight = FontWeight.Bold, // Negrita
                                    color = TextDark, // Color adaptable
                                    modifier = Modifier.weight(1f) // Ocupa espacio
                                ) // Fin Text
                                Icon( // Ícono verde de check en círculo
                                    imageVector = Icons.Outlined.CheckCircle, // Ícono
                                    contentDescription = null, // Sin descripción
                                    tint = Color(0xFF4CAF50), // Color verde
                                    modifier = Modifier.size(20.dp) // Tamaño
                                ) // Fin Icon
                            } // Fin Row
                            Spacer(modifier = Modifier.height(8.dp)) // Espacio
                            Text( // Número indicador de completados
                                text = "38/45", // Progreso
                                fontSize = 32.sp, // Tamaño grande
                                fontWeight = FontWeight.Bold, // Negrita
                                color = Color(0xFF2E7D32) // Color verde oscuro
                            ) // Fin Text
                        } // Fin Column
                    } // Fin Card completados

                    // Card 2: Programadas
                    Card( // Tarjeta derecha
                        modifier = Modifier.weight(1f), // Ancho 50%
                        shape = RoundedCornerShape(18.dp), // Redondeado
                        colors = CardDefaults.cardColors(containerColor = CardWhite), // Fondo adaptable
                        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Elevación
                    ) { // Cuerpo tarjeta
                        Column( // Disposición vertical
                            modifier = Modifier.padding(16.dp) // Relleno
                        ) { // Cuerpo columna
                            Row( // Fila cabecera tarjeta
                                modifier = Modifier.fillMaxWidth(), // Ancho completo
                                horizontalArrangement = Arrangement.SpaceBetween, // Separado
                                verticalAlignment = Alignment.Top // Alineación superior
                            ) { // Cuerpo Row
                                Text( // Etiqueta programados
                                    text = "Programados", // Texto
                                    fontSize = 12.sp, // Tamaño
                                    fontWeight = FontWeight.Bold, // Negrita
                                    color = TextDark, // Color adaptable
                                    modifier = Modifier.weight(1f) // Espacio
                                ) // Fin Text
                                Icon( // Ícono azul de calendario
                                    imageVector = Icons.Outlined.CalendarToday, // Calendario
                                    contentDescription = null, // Sin descripción
                                    tint = Color(0xFF1976D2), // Color azul
                                    modifier = Modifier.size(20.dp) // Tamaño
                                ) // Fin Icon
                            } // Fin Row
                            Spacer(modifier = Modifier.height(8.dp)) // Espacio
                            Text( // Número indicador de programados
                                text = "45", // Total
                                fontSize = 32.sp, // Tamaño grande
                                fontWeight = FontWeight.Bold, // Negrita
                                color = Color(0xFF1565C0) // Color azul oscuro
                            ) // Fin Text
                        } // Fin Column
                    } // Fin Card programados
                } // Fin Row tarjetas
            } // Fin Column general
        } // Fin ítem deslizable

        // 9. Historial de Hábitos (Título)
        item { // Ítem deslizable único
            Text( // Título del historial
                text = "Historial de Hábitos", // Texto
                fontSize = 18.sp, // Tamaño
                fontWeight = FontWeight.Bold, // Negrita
                color = TextDark, // Color adaptable
                modifier = Modifier.padding(top = 8.dp) // Padding superior
            ) // Fin Text
        } // Fin ítem deslizable

        // 10. Historial de Hábitos con Scroll interno para mostrar máximo 6 a la vez (Rúbrica de la Tarea)
        item { // Ítem deslizable único
            Card( // Tarjeta contenedora de la lista
                modifier = Modifier // Modificadores
                    .fillMaxWidth() // Ancho completo
                    .height(300.dp), // Altura fija calculada para mostrar exactamente 6 elementos
                shape = RoundedCornerShape(20.dp), // Redondeado
                colors = CardDefaults.cardColors(containerColor = CardWhite), // Fondo adaptable
                elevation = CardDefaults.cardElevation(defaultElevation = 2.dp) // Elevación
            ) { // Cuerpo tarjeta
                LazyColumn( // Scroll interno para historial
                    modifier = Modifier // Modificadores
                        .fillMaxSize() // Ocupa todo el contenedor
                        .padding(12.dp), // Relleno
                    verticalArrangement = Arrangement.spacedBy(8.dp), // Espaciado de 8dp
                    contentPadding = PaddingValues(bottom = 8.dp) // Margen inferior
                ) { // Cuerpo scroll interno
                    items(items = sampleHistoryItems, key = { it.id }) { historyItem -> // Carga dinámica con keys
                        HabitHistoryItemRow( // Renderiza cada fila de historial
                            item = historyItem, // Datos
                            onClick = { // Clic
                                Toast.makeText( // Muestra alerta
                                    context, // Contexto
                                    "${historyItem.habitTitle} - ${historyItem.dateText}", // Texto del Toast
                                    Toast.LENGTH_SHORT // Duración corta
                                ).show() // Muestra emergente
                            } // Fin acción
                        ) // Fin HabitHistoryItemRow
                        Box( // Línea divisora decorativa adaptable
                            modifier = Modifier // Modificadores
                                .fillMaxWidth() // Ancho completo
                                .height(1.dp) // Altura de 1dp
                                .background(if (isSystemInDarkTheme()) Color(0xFF2C2C2C) else Color(0xFFF0F0F0)) // Color adaptable
                        ) // Fin Box divisor
                    } // Fin items
                } // Fin LazyColumn interno
            } // Fin Card
        } // Fin ítem deslizable
    } // Fin LazyColumn principal de la pantalla
} // Fin composable TrackerScreen
