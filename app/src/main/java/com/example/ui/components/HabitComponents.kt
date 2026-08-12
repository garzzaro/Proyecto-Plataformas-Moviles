package com.example.ui.components // Declaración del paquete de componentes de UI de la app

import android.widget.Toast // Importa Toasts para notificaciones flotantes de Android
import androidx.compose.foundation.Image // Componente para pintar imágenes básicas de Compose
import androidx.compose.foundation.background // Modificador para definir colores de fondo
import androidx.compose.foundation.border // Modificador para añadir bordes y contornos
import androidx.compose.foundation.clickable // Modificador para añadir interactividad al presionar
import androidx.compose.foundation.layout.Arrangement // Configuración de distribución horizontal/vertical
import androidx.compose.foundation.layout.Box // Contenedor para apilar elementos Compose uno sobre otro
import androidx.compose.foundation.layout.Column // Contenedor vertical secuencial de layouts
import androidx.compose.foundation.layout.Row // Contenedor horizontal secuencial de layouts
import androidx.compose.foundation.layout.Spacer // Componente separador de tamaño estático fijo
import androidx.compose.foundation.layout.fillMaxWidth // Modificador para ocupar el 100% del ancho horizontal
import androidx.compose.foundation.layout.height // Modificador para definir una altura fija
import androidx.compose.foundation.layout.padding // Modificador para aplicar rellenos y márgenes internos
import androidx.compose.foundation.layout.size // Modificador para definir dimensiones cuadradas fijas
import androidx.compose.foundation.layout.width // Modificador para definir un ancho fijo
import androidx.compose.foundation.shape.CircleShape // Recorte de figuras en círculos perfectos
import androidx.compose.foundation.shape.RoundedCornerShape // Recorte de figuras con esquinas redondeadas
import androidx.compose.material.icons.Icons // Contenedor global de iconos vectoriales de Google
import androidx.compose.material.icons.filled.Check // Icono relleno de una tilde checkmark
import androidx.compose.material.icons.outlined.Add // Icono delineado del símbolo de suma
import androidx.compose.material.icons.outlined.Check // Icono delineado de checkmark
import androidx.compose.material.icons.outlined.CheckCircle // Icono delineado de checkmark en círculo
import androidx.compose.material.icons.outlined.FitnessCenter // Icono delineado de mancuerna de deporte
import androidx.compose.material.icons.outlined.MenuBook // Icono delineado de un libro abierto
import androidx.compose.material.icons.outlined.NightsStay // Icono delineado de una luna y estrellas
import androidx.compose.material.icons.outlined.Palette // Icono delineado de paleta de arte
import androidx.compose.material.icons.outlined.Payments // Icono delineado de monedas apiladas
import androidx.compose.material.icons.outlined.Psychology // Icono delineado de cerebro
import androidx.compose.material.icons.outlined.Refresh // Icono delineado de flechas de actualización
import androidx.compose.material.icons.outlined.Restaurant // Icono delineado de tenedor y cuchillo
import androidx.compose.material.icons.outlined.SelfImprovement // Icono de persona meditando
import androidx.compose.material.icons.outlined.Timer // Icono delineado de cronómetro
import androidx.compose.material.icons.outlined.WaterDrop // Icono delineado de gota de agua
import androidx.compose.material.icons.outlined.WorkOutline // Icono delineado de maletín de trabajo
import androidx.compose.material3.Button // Botón interactivo principal de Material 3
import androidx.compose.material3.ButtonDefaults // Propiedades predeterminadas de botones de Compose
import androidx.compose.material3.Card // Tarjeta contenedora con elevación Material 3
import androidx.compose.material3.CardDefaults // Colores y elevaciones por defecto de tarjetas
import androidx.compose.material3.CircularProgressIndicator // Barra de progreso circular
import androidx.compose.material3.Icon // Componente para dibujar vectores gráficos vectoriales
import androidx.compose.material3.LinearProgressIndicator // Barra de progreso horizontal M3
import androidx.compose.material3.Text // Componente para renderizar textos en pantalla M3
import androidx.compose.runtime.Composable // Anotación que habilita la compilación Compose
import androidx.compose.ui.Alignment // Alineaciones en coordenadas espaciales
import androidx.compose.ui.Modifier // Modificador de Compose para atributos visuales
import androidx.compose.ui.draw.clip // Modificador para recortar formas del contenedor
import androidx.compose.ui.graphics.Color // Objeto de representación cromática de Compose
import androidx.compose.ui.graphics.StrokeCap // Define el estilo del fin de línea (redondo/plano)
import androidx.compose.ui.graphics.vector.ImageVector // Tipo nativo para vectores gráficos
import androidx.compose.ui.layout.ContentScale // Regla de escala de carga de imágenes
import androidx.compose.ui.platform.LocalContext // Obtiene el contexto actual del sistema
import androidx.compose.ui.res.painterResource // Cargador de drawable local del proyecto
import androidx.compose.ui.text.font.FontWeight // Modificador de grosor de tipografía
import androidx.compose.ui.text.style.TextAlign // Modificador de alineación de textos
import androidx.compose.ui.unit.dp // Pixeles independientes de densidad para tamaños
import androidx.compose.ui.unit.sp // Pixeles independientes de escala para textos
import coil.compose.AsyncImage // Cargador Coil de imágenes asíncronas
import com.example.R // Recursos locales auto-generados de la app
import com.example.model.HabitHistoryItem // Importa el modelo de datos de historial
import com.example.model.HabitItem // Importa el modelo de datos de hábitos
import com.example.ui.theme.* // Importa todos los colores y temas de la app
import androidx.compose.material3.MaterialTheme // Acceso a tokens de color de MaterialTheme
import androidx.compose.foundation.isSystemInDarkTheme // Detecta si el modo oscuro está activo en Android

// ============================================================================
// VALORES DE COLOR DINÁMICOS COMPOSABLES (Para Soporte de Modo Oscuro Premium)
// ============================================================================

val LightTealBg = Color(0xFFE0F7FA) // Declara color de fondo teal claro fijo

val TextGray: Color // Propiedad de color de texto gris adaptable
    @Composable // Anotación composable para leer el tema activo
    get() = if (isSystemInDarkTheme()) TextGrayDark else TextGrayLight // Retorna gris según el modo oscuro

val ScreenBackground: Color // Color de fondo de pantalla adaptable
    @Composable // Anotación composable
    get() = MaterialTheme.colorScheme.background // Retorna el fondo del esquema de color activo

val CardWhite: Color // Color de fondo de tarjetas adaptable
    @Composable // Anotación composable
    get() = MaterialTheme.colorScheme.surface // Retorna el color de superficie activo

val TextDark: Color // Color de texto principal adaptable
    @Composable // Anotación composable
    get() = MaterialTheme.colorScheme.onSurface // Retorna el color de texto sobre superficie activo

// Card mappings from theme resolved dynamically (Para la sección de Perfil)
val CardStreakBackground: Color // Color de fondo de racha adaptable
    @Composable // Anotación composable
    get() = if (isSystemInDarkTheme()) CardStreakDark else CardStreakLight // Retorna racha según modo oscuro

val NumberStreakColor: Color // Color de número de racha adaptable
    @Composable // Anotación composable
    get() = if (isSystemInDarkTheme()) NumberStreakDark else NumberStreakLight // Retorna número según tema

val SubtitleStreakColor: Color // Color de subtítulo de racha adaptable
    @Composable // Anotación composable
    get() = if (isSystemInDarkTheme()) SubtitleStreakDark else SubtitleStreakLight // Retorna subtítulo según tema

val CardAccuracyBackground: Color // Color de fondo de precisión adaptable
    @Composable // Anotación composable
    get() = if (isSystemInDarkTheme()) CardAccuracyDark else CardAccuracyLight // Retorna precisión según modo oscuro

val NumberAccuracyColor: Color // Color de número de precisión adaptable
    @Composable // Anotación composable
    get() = if (isSystemInDarkTheme()) NumberAccuracyDark else NumberAccuracyLight // Retorna número según tema

val SubtitleAccuracyColor: Color // Color de subtítulo de precisión adaptable
    @Composable // Anotación composable
    get() = if (isSystemInDarkTheme()) SubtitleAccuracyDark else SubtitleAccuracyLight // Retorna subtítulo según tema

// ============================================================================
// FUNCIONES DE UTILIDAD PARA HÁBITOS
// ============================================================================

/**
 * getCategoryIcon devuelve el ícono vectorial de Material según la categoría especificada.
 */
fun getCategoryIcon(category: String): ImageVector { // Firma de la función
    return when (category.lowercase()) { // Bloque selectivo basado en texto en minúsculas
        "hidratación", "hydration" -> Icons.Outlined.WaterDrop // Retorna gota de agua
        "aprendizaje", "learning" -> Icons.Outlined.MenuBook // Retorna libro
        "ejercicio", "fitness" -> Icons.Outlined.FitnessCenter // Retorna mancuerna
        "mindfulness" -> Icons.Outlined.SelfImprovement // Retorna meditación
        "productividad", "productivity" -> Icons.Outlined.WorkOutline // Retorna maletín
        "nutrición", "nutrition" -> Icons.Outlined.Restaurant // Retorna cubiertos
        "sueño", "sleep" -> Icons.Outlined.NightsStay // Retorna luna
        "arte y creatividad", "art" -> Icons.Outlined.Palette // Retorna paleta de arte
        "finanzas", "finance" -> Icons.Outlined.Payments // Retorna monedas
        "salud mental" -> Icons.Outlined.Psychology // Retorna cerebro
        else -> Icons.Outlined.CheckCircle // Retorna ícono por defecto
    } // Fin del bloque selectivo
} // Fin de la función getCategoryIcon

/**
 * getCategoryBgColor devuelve el color de fondo adaptado para la burbuja de la categoría.
 */
@Composable // Anotación composable para reaccionar al tema activo
fun getCategoryBgColor(category: String): Color { // Firma de la función
    val isDark = isSystemInDarkTheme() // Verifica si el sistema usa tema oscuro
    return when (category.lowercase()) { // Bloque selectivo
        "hidratación", "hydration" -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA) // Verde azulado
        "aprendizaje", "learning" -> if (isDark) Color(0xFF311B92) else Color(0xFFF3E5F5) // Morado profundo
        "ejercicio", "fitness" -> if (isDark) Color(0xFFE65100) else Color(0xFFFFE0B2) // Naranja oscuro
        "mindfulness" -> if (isDark) Color(0xFF880E4F) else Color(0xFFF8BBD0) // Rosa oscuro
        "productividad", "productivity" -> if (isDark) Color(0xFF1A237E) else Color(0xFFE8EAF6) // Azul rey
        "nutrición", "nutrition" -> if (isDark) Color(0xFF33691E) else Color(0xFFDCEDC8) // Verde hoja
        "sueño", "sleep" -> if (isDark) Color(0xFF4A148C) else Color(0xFFE1BEE7) // Lila oscuro
        "arte y creatividad", "art" -> if (isDark) Color(0xFFF57F17) else Color(0xFFFFF9C4) // Amarillo oscuro
        "finanzas", "finance" -> if (isDark) Color(0xFF1B5E20) else Color(0xFFC8E6C9) // Verde bosque
        "salud mental" -> if (isDark) Color(0xFF263238) else Color(0xFFCFD8DC) // Gris pizarra
        else -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA) // Caso base por defecto
    } // Fin del bloque selectivo
} // Fin de la función getCategoryBgColor

// ============================================================================
// COMPONENTE DE ÍTEM DE HÁBITO: HabitItemCard
// ============================================================================

/**
 * HabitItemCard es la tarjeta individual para cada hábito en la lista del Rastreador.
 */
@Composable // Anotación composable
fun HabitItemCard( // Firma de la función
    habit: HabitItem, // Objeto inmutable con los datos del hábito
    onToggleComplete: (HabitItem) -> Unit, // Callback para alternar estado
    onClickDetail: (HabitItem) -> Unit, // Callback para abrir detalles
    modifier: Modifier = Modifier // Modificador por defecto
) { // Inicio del cuerpo del composable
    val context = LocalContext.current // Contexto local de Android para mostrar Toasts
    val isDark = isSystemInDarkTheme() // Booleano para comprobar si el modo oscuro está activo

    Card( // Tarjeta contenedora de Material 3
        modifier = modifier // Aplica modificador
            .fillMaxWidth() // Ocupa el ancho total
            .clickable { // Añade clic interactivo a la tarjeta
                onClickDetail(habit) // Dispara el callback de detalle
                Toast.makeText(context, "Hábito: ${habit.title}", Toast.LENGTH_SHORT).show() // Muestra Toast
            }, // Fin clickable
        shape = RoundedCornerShape(20.dp), // Redondeado estético de esquinas a 20dp
        colors = CardDefaults.cardColors(containerColor = CardWhite), // Color adaptable de fondo
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Elevación nula para diseño minimalista
    ) { // Inicio del contenido de la tarjeta
        Row( // Alinea horizontalmente el ícono, textos y controles
            modifier = Modifier // Modificadores
                .fillMaxWidth() // Ancho completo
                .padding(16.dp), // Relleno interno de 16dp
            verticalAlignment = Alignment.CenterVertically // Centrado vertical de componentes
        ) { // Inicio Row
            // Círculo con ícono de categoría
            Box( // Contenedor circular
                modifier = Modifier // Modificadores
                    .size(48.dp) // Tamaño cuadrado de 48dp
                    .background(getCategoryBgColor(habit.category), CircleShape), // Color de fondo circular
                contentAlignment = Alignment.Center // Centra el icono internamente
            ) { // Inicio Box
                Icon( // Ícono vectorial
                    imageVector = getCategoryIcon(habit.category), // Obtiene el vector correspondiente
                    contentDescription = habit.category, // Descripción para lectores
                    tint = MaterialTheme.colorScheme.primary, // Color interactivo
                    modifier = Modifier.size(24.dp) // Tamaño del icono a 24dp
                ) // Fin Icon
            } // Fin Box

            Spacer(modifier = Modifier.width(14.dp)) // Espaciador de separación de 14dp

            // Detalles del hábito
            Column( // Columna de información textual
                modifier = Modifier.weight(1f) // Ocupa todo el ancho restante intermedio
            ) { // Inicio Column
                Text( // Título del hábito
                    text = habit.title, // Texto a pintar
                    fontSize = 16.sp, // Tamaño de letra
                    fontWeight = FontWeight.Bold, // Letra gruesa
                    color = TextDark // Color adaptable
                ) // Fin Text

                Spacer(modifier = Modifier.height(2.dp)) // Espacio vertical mínimo de 2dp

                Text( // Subtítulo del hábito
                    text = habit.subtitle, // Texto a pintar
                    fontSize = 13.sp, // Tamaño
                    fontWeight = FontWeight.Medium, // Peso intermedio
                    color = TextGray // Gris adaptable
                ) // Fin Text

                Spacer(modifier = Modifier.height(8.dp)) // Espacio vertical de 8dp previo a los chips

                // Etiquetas / Chips (múltiplos de 4dp)
                Row( // Disposición horizontal de los chips
                    horizontalArrangement = Arrangement.spacedBy(8.dp), // Separación de 8dp
                    verticalAlignment = Alignment.CenterVertically // Alineado central
                ) { // Inicio Row de chips
                    habit.tags.forEach { tag -> // Itera cada etiqueta del hábito
                        val chipBg = when (tag.lowercase()) { // Determina fondo de chip
                            "diario" -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA) // Verde
                            "hidratación" -> if (isDark) Color(0xFF1A237E) else Color(0xFFF3E5F5) // Azul
                            "alta intensidad" -> if (isDark) Color(0xFFE65100) else Color(0xFFFFE0B2) // Naranja
                            "fuerza" -> if (isDark) Color(0xFF004D40) else Color(0xFFE0F7FA) // Verde
                            else -> if (isDark) Color(0xFF303030) else Color(0xFFF1F3F4) // Gris
                        } // Fin selectivo
                        val chipTextColor = when (tag.lowercase()) { // Determina color del texto del chip
                            "alta intensidad" -> if (isDark) Color(0xFFFFCC80) else Color(0xFFA15D22) // Tono naranja
                            else -> if (isDark) Color.White else TextDark // Blanco u oscuro adaptable
                        } // Fin selectivo

                        Box( // Burbuja de chip individual
                            modifier = Modifier // Modificadores
                                .background(chipBg, RoundedCornerShape(12.dp)) // Fondo redondeado a 12dp
                                .padding(horizontal = 8.dp, vertical = 4.dp) // Relleno
                        ) { // Inicio Box chip
                            Text( // Texto del chip
                                text = tag, // Contenido de la etiqueta
                                fontSize = 11.sp, // Tamaño de letra
                                fontWeight = FontWeight.Bold, // Negrita
                                color = chipTextColor // Color de contraste
                            ) // Fin Text
                        } // Fin Box chip
                    } // Fin forEach
                } // Fin Row de chips
            } // Fin Column detalles

            Spacer(modifier = Modifier.width(8.dp)) // Espacio horizontal de 8dp

            // Control de Progreso o Checkbox
            Column( // Contenedor vertical derecho para controles
                horizontalAlignment = Alignment.End, // Alinea al extremo derecho
                verticalArrangement = Arrangement.Center // Centra verticalmente
            ) { // Inicio Column control
                if (habit.isProgressive) { // Caso A: Si el hábito es progresivo
                    Text( // Muestra el contador (ej. 3/8)
                        text = "${habit.currentProgress}/${habit.maxProgress}", // Progreso
                        fontSize = 16.sp, // Tamaño
                        fontWeight = FontWeight.Bold, // Negrita
                        color = TextDark // Color adaptable
                    ) // Fin Text
                    Spacer(modifier = Modifier.height(4.dp)) // Espacio vertical
                    LinearProgressIndicator( // Barra horizontal de progreso
                        progress = { habit.currentProgress.toFloat() / habit.maxProgress.toFloat() }, // Calcula avance
                        modifier = Modifier // Modificadores
                            .width(52.dp) // Ancho fijo
                            .height(8.dp) // Altura de 8dp
                            .clip(RoundedCornerShape(4.dp)), // Redondeado
                        color = MaterialTheme.colorScheme.primary, // Color primario
                        trackColor = if (isDark) Color(0xFF303030) else Color(0xFFE0E0E0), // Pista adaptable
                        strokeCap = StrokeCap.Round // Puntas redondeadas
                    ) // Fin progress indicator
                } else if (habit.timeOrDuration.isNotEmpty() && habit.timeOrDuration.contains("Min")) { // Caso B: Si es temporizado
                    Box( // Botón circular naranja para temporizador
                        modifier = Modifier // Modificadores
                            .size(38.dp) // Diámetro
                            .background(Color(0xFFFF9800), CircleShape) // Color naranja
                            .clickable { // Clic
                                onToggleComplete(habit) // Llama alternador
                            }, // Fin clickable
                        contentAlignment = Alignment.Center // Centrado
                    ) { // Inicio Box
                        Icon( // Icono de reloj
                            imageVector = Icons.Outlined.Timer, // Reloj
                            contentDescription = "Temporizador", // Descripción
                            tint = Color.White, // Blanco fijo
                            modifier = Modifier.size(20.dp) // Tamaño
                        ) // Fin Icon
                    } // Fin Box
                } else { // Caso C: Hábito simple con checkbox circular
                    Box( // Contenedor circular intermedio
                        modifier = Modifier // Modificadores
                            .size(38.dp) // Diámetro
                            .border( // Borde circular
                                width = 2.dp, // Grosor
                                color = if (habit.isCompleted) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF505050) else Color(0xFFD0D0D0), // Borde
                                shape = CircleShape // Circular
                            ) // Fin border
                            .background( // Fondo
                                color = if (habit.isCompleted) { // Color si está completado
                                    MaterialTheme.colorScheme.primary.copy(alpha = 0.2f) // Tono brillante translúcido
                                } else Color.Transparent, // Sin fondo si está vacío
                                shape = CircleShape // Circular
                            ) // Fin background
                            .clickable { // Clic
                                onToggleComplete(habit) // Alterna estado
                            }, // Fin clickable
                        contentAlignment = Alignment.Center // Centrado
                    ) { // Inicio Box check
                        if (habit.isCompleted) { // Si ya se completó dibuja la tilde
                            Icon( // Checkmark
                                imageVector = Icons.Filled.Check, // Ícono check
                                contentDescription = "Completado", // Descripción
                                tint = MaterialTheme.colorScheme.primary, // Color brillante
                                modifier = Modifier.size(20.dp) // Tamaño
                            ) // Fin Icon
                        } // Fin if completed
                    } // Fin Box check
                } // Fin condicional de controles
            } // Fin Column control
        } // Fin Row
    } // Fin Card
} // Fin composable HabitItemCard

// ============================================================================
// COMPONENTE DE ÍTEM DE HISTORIAL: HabitHistoryItemRow
// ============================================================================

/**
 * HabitHistoryItemRow representa cada fila de historial en la parte inferior.
 */
@Composable // Anotación composable
fun HabitHistoryItemRow( // Firma de la función
    item: HabitHistoryItem, // Registro del historial inmutable
    onClick: () -> Unit, // Callback al presionar
    modifier: Modifier = Modifier // Modificador por defecto
) { // Inicio de cuerpo
    val isDark = isSystemInDarkTheme() // Revisa si el sistema usa tema oscuro
    Row( // Disposición horizontal de tilde, título y fecha
        modifier = modifier // Aplica modificadores
            .fillMaxWidth() // Ancho completo
            .clickable { onClick() } // Clic interactivo
            .padding(vertical = 10.dp, horizontal = 12.dp), // Márgenes
        horizontalArrangement = Arrangement.SpaceBetween, // Separa los extremos
        verticalAlignment = Alignment.CenterVertically // Centrado vertical
    ) { // Inicio Row
        Row( // Contenedor horizontal interno
            verticalAlignment = Alignment.CenterVertically, // Centrado
            horizontalArrangement = Arrangement.spacedBy(12.dp) // Separación horizontal
        ) { // Inicio Row interna
            Box( // Círculo indicador de estado completado
                modifier = Modifier // Modificadores
                    .size(28.dp) // Diámetro
                    .background( // Color
                        color = if (item.isCompleted) Color(0xFF26A69A) else if (isDark) Color(0xFF383838) else Color(0xFFE0E0E0), // Color adaptable
                        shape = CircleShape // Circular
                    ), // Fin background
                contentAlignment = Alignment.Center // Centra la tilde
            ) { // Inicio Box
                Icon( // Icono de tilde delineada
                    imageVector = Icons.Outlined.Check, // Checkmark
                    contentDescription = null, // Sin descripción
                    tint = Color.White, // Blanco
                    modifier = Modifier.size(16.dp) // Tamaño
                ) // Fin Icon
            } // Fin Box

            Text( // Nombre del hábito registrado
                text = item.habitTitle, // Título
                fontSize = 15.sp, // Tamaño
                fontWeight = FontWeight.Medium, // Letra regular
                color = TextDark // Color adaptable
            ) // Fin Text
        } // Fin Row interna

        Text( // Fecha del registro
            text = item.dateText, // Fecha
            fontSize = 13.sp, // Tamaño
            fontWeight = FontWeight.Medium, // Peso
            color = TextGray // Gris adaptable
        ) // Fin Text
    } // Fin Row
} // Fin composable HabitHistoryItemRow

// ============================================================================
// COMPONENTE BANNER DE MASCOTA: MascotBannerCard
// ============================================================================

/**
 * MascotBannerCard muestra a la mascota Octo celebrando el progreso del usuario.
 */
@Composable // Anotación composable
fun MascotBannerCard( // Firma de la función
    onVisitMascotClick: () -> Unit // Callback de click en botón
) { // Inicio de cuerpo
    Column( // Alinea verticalmente los elementos motivacionales
        modifier = Modifier // Modificadores
            .fillMaxWidth() // Ancho completo
            .padding(vertical = 8.dp), // Relleno vertical
        horizontalAlignment = Alignment.CenterHorizontally // Centrado de componentes
    ) { // Inicio Column
        AsyncImage( // Muestra la foto del pulpo desde la URL de GitHub usando Coil
            model = "https://raw.githubusercontent.com/garzzaro/Proyecto-Plataformas-Moviles/feature/lab2-lazy-list/app/src/main/res/drawable/pulpo.avif", // Carga la imagen de la mascota desde la URL de GitHub
            placeholder = painterResource(id = R.drawable.ic_launcher_background), // Rúbrica: Placeholder de carga obligatorio
            contentDescription = "Pulpo HabitOcto", // Descripción de accesibilidad
            modifier = Modifier // Modificadores de diseño
                .size(110.dp) // Dimensiones cuadradas
                .clip(CircleShape), // Recorte circular
            contentScale = ContentScale.Crop // Recorte proporcional
        ) // Fin AsyncImage

        Spacer(modifier = Modifier.height(10.dp)) // Espacio vertical

        Text( // Título motivador
            text = "¡Progreso Increíble!", // Texto
            fontSize = 22.sp, // Tamaño
            fontWeight = FontWeight.Bold, // Negrita
            color = MaterialTheme.colorScheme.primary // Color de tema primario
        ) // Fin Text

        Spacer(modifier = Modifier.height(4.dp)) // Espacio mínimo

        Text( // Descripción secundaria
            text = "Has alcanzado tu meta de 7 días. ¡Octo está orgulloso!", // Subtexto
            fontSize = 14.sp, // Tamaño
            color = TextDark, // Color adaptable
            textAlign = TextAlign.Center, // Alineación de texto centrada
            modifier = Modifier.padding(horizontal = 32.dp) // Paddings laterales
        ) // Fin Text

        Spacer(modifier = Modifier.height(12.dp)) // Espacio vertical

        Button( // Botón interactivo para ver mascota
            onClick = onVisitMascotClick, // Asigna acción
            colors = ButtonDefaults.buttonColors(containerColor = SecondaryTeal), // Color de fondo
            shape = RoundedCornerShape(24.dp), // Redondeado ovalado
            elevation = ButtonDefaults.buttonElevation(defaultElevation = 2.dp) // Sombra leve
        ) { // Contenido del botón
            Text( // Texto interno del botón
                text = "Visitar Mascota", // Texto
                color = TextDark, // Color de texto adaptable
                fontWeight = FontWeight.Bold, // Letras gruesas
                fontSize = 14.sp, // Tamaño
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 2.dp) // Paddings
            ) // Fin Text
        } // Fin Button
    } // Fin Column
} // Fin composable MascotBannerCard

// ============================================================================
// BOTÓN DE NUEVO HÁBITO: NewHabitButtonCard
// ============================================================================

/**
 * NewHabitButtonCard es el botón interactivo para abrir el formulario de hábitos.
 */
@Composable // Anotación composable
fun NewHabitButtonCard(onClick: () -> Unit) { // Firma de la función
    val isDark = isSystemInDarkTheme() // Obtiene si el sistema usa modo oscuro
    Card( // Tarjeta contenedora
        modifier = Modifier // Modificadores
            .fillMaxWidth() // Ancho completo
            .clickable { onClick() }, // Abre formulario de hábitos al pulsarlo
        shape = RoundedCornerShape(20.dp), // Esquinas redondeadas
        colors = CardDefaults.cardColors(containerColor = CardWhite), // Fondo adaptable
        border = androidx.compose.foundation.BorderStroke(1.dp, if (isDark) Color(0xFF383838) else Color(0xFFE0E0E0)), // Contorno adaptable
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Elevación nula
    ) { // Inicio de tarjeta
        Column( // Centra el contenido en vertical
            modifier = Modifier // Modificadores
                .fillMaxWidth() // Ancho completo
                .padding(vertical = 16.dp), // Relleno vertical
            horizontalAlignment = Alignment.CenterHorizontally, // Centrado horizontal
            verticalArrangement = Arrangement.Center // Centrado vertical
        ) { // Inicio Column
            Box( // Círculo verde decorativo con signo más (+)
                modifier = Modifier // Modificadores
                    .size(44.dp) // Diámetro
                    .background(MaterialTheme.colorScheme.primary, CircleShape), // Círculo
                contentAlignment = Alignment.Center // Centra el ícono
            ) { // Inicio Box
                Icon( // Icono de suma (+)
                    imageVector = Icons.Outlined.Add, // Ícono
                    contentDescription = "Añadir Hábito", // Descripción
                    tint = Color.White, // Blanco
                    modifier = Modifier.size(24.dp) // Tamaño
                ) // Fin Icon
            } // Fin Box
            Spacer(modifier = Modifier.height(8.dp)) // Espacio
            Text( // Nombre de la acción
                text = "Nuevo Hábito", // Texto
                fontSize = 14.sp, // Tamaño
                fontWeight = FontWeight.Bold, // Negrita
                color = TextDark // Color adaptable
            ) // Fin Text
        } // Fin Column
    } // Fin Card
} // Fin composable NewHabitButtonCard

// ============================================================================
// PROGRESO SEMANAL: WeeklyProgressCard
// ============================================================================

/**
 * WeeklyProgressCard muestra los días de la semana con círculos de completado.
 */
@Composable // Anotación composable
fun WeeklyProgressCard() { // Firma de la función
    val isDark = isSystemInDarkTheme() // Verifica modo oscuro activo
    Card( // Tarjeta contenedora
        modifier = Modifier.fillMaxWidth(), // Ancho completo
        shape = RoundedCornerShape(20.dp), // Redondeado
        colors = CardDefaults.cardColors(containerColor = CardWhite), // Fondo adaptable
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Sin sombras
    ) { // Inicio tarjeta
        Column( // Organiza los elementos en vertical
            modifier = Modifier.padding(16.dp) // Relleno interno
        ) { // Inicio columna
            Text( // Título del progreso semanal
                text = "Progreso Semanal", // Texto
                fontSize = 16.sp, // Tamaño
                fontWeight = FontWeight.Bold, // Negrita
                color = TextDark // Color adaptable
            ) // Fin Text

            Spacer(modifier = Modifier.height(12.dp)) // Espacio de separación

            val days = listOf("L", "M", "M", "J", "V", "S", "D") // Lista de los 7 días de la semana
            val completed = listOf(true, true, true, true, true, true, false) // Estados de cumplimiento (6/7 completados)

            Row( // Disposición horizontal para mostrar los 7 días alineados
                modifier = Modifier.fillMaxWidth(), // Ancho completo
                horizontalArrangement = Arrangement.SpaceBetween // Espaciado equidistante
            ) { // Inicio Row de días
                days.forEachIndexed { index, day -> // Itera cada día con su índice
                    Column( // Columna para emparejar el texto del día arriba y el círculo abajo
                        horizontalAlignment = Alignment.CenterHorizontally // Centrado horizontal
                    ) { // Inicio columna día
                        Text( // Inicial del día (ej. L, M)
                            text = day, // Texto
                            fontSize = 12.sp, // Tamaño
                            fontWeight = FontWeight.Bold, // Negrita
                            color = TextGray // Gris adaptable
                        ) // Fin Text
                        Spacer(modifier = Modifier.height(6.dp)) // Espacio vertical
                        Box( // Burbuja de estado completado/inactivo
                            modifier = Modifier // Modificadores
                                .size(32.dp) // Diámetro del círculo
                                .background( // Color de fondo circular
                                    color = if (completed[index]) MaterialTheme.colorScheme.primary else if (isDark) Color(0xFF383838) else Color(0xFFE0E0E0), // Color
                                    shape = CircleShape // Circular
                                ), // Fin background
                            contentAlignment = Alignment.Center // Centrado de icono interno
                        ) { // Inicio Box círculo
                            if (completed[index]) { // Si fue completado pinta el checkmark
                                Icon( // Ícono de tilde check
                                    imageVector = Icons.Outlined.Check, // Vector
                                    contentDescription = null, // Sin descripción
                                    tint = Color.White, // Blanco fijo
                                    modifier = Modifier.size(18.dp) // Tamaño
                                ) // Fin Icon
                            } // Fin if completed
                        } // Fin Box círculo
                    } // Fin Column día
                } // Fin iteración
            } // Fin Row de días
        } // Fin Column
    } // Fin Card
} // Fin composable WeeklyProgressCard
