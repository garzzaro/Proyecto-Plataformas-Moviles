package com.example // Declaración del paquete al que pertenece este archivo fuente

import android.os.Bundle // Importa el contenedor para pasar datos entre actividades de Android
import android.widget.Toast // Importa la clase para mostrar mensajes emergentes en pantalla
import androidx.activity.ComponentActivity // Importa la clase base para actividades nativas compatibles con Compose
import androidx.activity.compose.setContent // Importa el método puente para usar Compose como vista de la actividad
import androidx.activity.enableEdgeToEdge // Importa la extensión para configurar el diseño inmersivo sin bordes
import androidx.compose.foundation.background // Importa el modificador para cambiar el color o dibujo de fondo
import androidx.compose.foundation.border // Importa el modificador para añadir contornos a los elementos visuales
import androidx.compose.foundation.clickable // Importa el modificador para hacer interactivos y pulsables los elementos
import androidx.compose.foundation.isSystemInDarkTheme // Importa la función que detecta si el modo oscuro está activo
import androidx.compose.foundation.layout.Arrangement // Importa la configuración para distribuir elementos en filas o columnas
import androidx.compose.foundation.layout.Box // Importa el contenedor básico de Compose para apilar elementos uno sobre otro
import androidx.compose.foundation.layout.Column // Importa el contenedor para alinear elementos secuencialmente en vertical
import androidx.compose.foundation.layout.Row // Importa el contenedor para alinear elementos secuencialmente en horizontal
import androidx.compose.foundation.layout.Spacer // Importa un componente vacío que crea espacios de separación fijos
import androidx.compose.foundation.layout.WindowInsets // Importa la clase para medir márgenes del sistema (status bar, nav bar)
import androidx.compose.foundation.layout.asPaddingValues // Convierte márgenes del sistema en Paddings utilizables en Compose
import androidx.compose.foundation.layout.fillMaxSize // Modificador para que un elemento ocupe todo el ancho y alto disponible
import androidx.compose.foundation.layout.fillMaxWidth // Modificador para que un elemento ocupe todo el ancho horizontal disponible
import androidx.compose.foundation.layout.height // Modificador para definir una altura fija para un componente
import androidx.compose.foundation.layout.navigationBars // Mide la barra de navegación física del sistema operativo Android
import androidx.compose.foundation.layout.offset // Modificador para desplazar un elemento en coordenadas X/Y relativas
import androidx.compose.foundation.layout.padding // Modificador para aplicar márgenes internos a un elemento visual
import androidx.compose.foundation.layout.size // Modificador para definir dimensiones cuadradas (ancho e igual alto) fijas
import androidx.compose.foundation.layout.statusBars // Mide la barra de estado superior (hora, notificaciones) de Android
import androidx.compose.foundation.layout.width // Modificador para definir un ancho fijo para un componente
import androidx.compose.foundation.lazy.LazyColumn // Importa la columna con reciclaje eficiente para listas muy largas
import androidx.compose.foundation.shape.CircleShape // Importa la forma geométrica predefinida circular
import androidx.compose.foundation.shape.RoundedCornerShape // Importa la forma geométrica con esquinas redondeadas ajustables
import androidx.compose.material.icons.Icons // Importa el contenedor global de iconos por defecto de Google
import androidx.compose.material.icons.filled.Person // Importa el icono relleno de una persona (Profile)
import androidx.compose.material.icons.outlined.Adjust // Importa el icono de anillo de ajuste/consistencia
import androidx.compose.material.icons.outlined.Email // Importa el icono delineado de un sobre de correo electrónico
import androidx.compose.material.icons.outlined.Group // Importa el icono delineado de grupo de personas (Feed)
import androidx.compose.material.icons.outlined.Lock // Importa el icono delineado de un candado cerrado
import androidx.compose.material.icons.outlined.Notifications // Importa el icono delineado de una campana
import androidx.compose.material.icons.outlined.Shield // Importa el icono delineado de un escudo de seguridad (2FA)
import androidx.compose.material3.Card // Importa la tarjeta contenedora Material Design 3
import androidx.compose.material3.CardDefaults // Importa las propiedades por defecto de tarjetas (colores, elevación)
import androidx.compose.material3.Icon // Importa el componente para renderizar gráficos vectoriales
import androidx.compose.material3.IconButton // Importa el componente de botón diseñado para contener un único icono
import androidx.compose.material3.MaterialTheme // Importa el acceso directo a los tokens de diseño del tema activo
import androidx.compose.material3.NavigationBar // Importa la barra de navegación inferior de Material Design 3
import androidx.compose.material3.NavigationBarItem // Importa cada elemento interactivo de la barra inferior de navegación
import androidx.compose.material3.NavigationBarItemDefaults // Importa los colores e indicadores por defecto de elementos nav
import androidx.compose.material3.Scaffold // Importa la estructura base de pantalla con soporte de barras superior/inferior
import androidx.compose.material3.Switch // Importa el control de alternancia encendido/apagado deslizable
import androidx.compose.material3.SwitchDefaults // Importa configuraciones y colores predeterminados para interruptores (Switches)
import androidx.compose.material3.Text // Importa el componente básico para pintar texto en pantalla
import androidx.compose.runtime.Composable // Importa la anotación que define funciones constructoras de UI
import androidx.compose.runtime.getValue // Importa el delegado de lectura para estados mutables de Compose
import androidx.compose.runtime.mutableIntStateOf // Inicializa estados mutables especializados en enteros
import androidx.compose.runtime.mutableStateOf // Inicializa estados mutables de propósito general
import androidx.compose.runtime.remember // Persiste un estado a través de las recomposiciones de la UI
import androidx.compose.runtime.setValue // Importa el delegado de escritura para estados mutables de Compose
import androidx.compose.ui.Alignment // Importa alineaciones de posición (Center, Start, End, etc.)
import androidx.compose.ui.Modifier // Importa la cadena de modificadores de comportamiento y diseño
import androidx.compose.ui.draw.clip // Modificador para recortar la forma visual externa de un elemento
import androidx.compose.ui.graphics.Color // Importa la representación cromática ARGB nativa en Compose
import androidx.compose.ui.layout.ContentScale // Define cómo se escala y recorta una imagen dentro de su contenedor
import androidx.compose.ui.platform.LocalContext // Obtiene el contexto actual del sistema Android
import androidx.compose.ui.text.font.FontWeight // Modifica el grosor tipográfico (normal, bold, medium, etc.)
import androidx.compose.ui.tooling.preview.Preview // Anotación para ver vistas previas de UI sin compilar la app completa
import androidx.compose.ui.unit.dp // Unidad de medida Density-Independent Pixels para márgenes y tamaños
import androidx.compose.ui.unit.sp // Unidad de medida Scale-Independent Pixels para textos del usuario
import coil.compose.AsyncImage // Importa el cargador de imágenes remotas (URLs) asíncronas de la librería Coil
import com.example.ui.theme.* // Importa todos los elementos de la carpeta de diseño (colores, tema, tipografías)

// ============================================================================
// CLASE PRINCIPAL: MainActivity (Lab #1: Layouts, Material 3 e imágenes con Coil)
// ============================================================================

/**
 * MainActivity es el punto de entrada principal para esta aplicación Android.
 *
 * - **¿Para qué sirve?** Sirve como el contenedor principal del ciclo de vida de la actividad que
 *   Android requiere para ejecutar el código nativo e inflar la UI.
 * - **¿Cómo funciona?** Al arrancar la aplicación, el sistema operativo Android llama al método
 *   [onCreate], habilitando soporte completo de renderizado de borde a borde (Edge-to-Edge) y
 *   estableciendo nuestro diseño composable principal envuelto en el tema de la aplicación.
 * - **¿Qué pasa si lo quitamos?** Si se elimina esta clase o su declaración en el AndroidManifest.xml,
 *   la aplicación no podrá iniciarse por el sistema operativo, resultando en un fallo inmediato (Crash)
 *   o en la imposibilidad de abrir la app al tocar su icono.
 */
class MainActivity : ComponentActivity() { // Declara la actividad principal heredando de la base de Compose
  override fun onCreate(savedInstanceState: Bundle?) { // Sobrescribe el método de inicio al crearse la actividad
    super.onCreate(savedInstanceState) // Llama a la inicialización nativa de la superclase

    // Habilita el renderizado de borde a borde.
    // - ¿Para qué sirve? Permite que la aplicación dibuje componentes debajo de las barras del sistema (estado y navegación).
    // - ¿Qué pasa si lo quitamos? Las barras superior e inferior volverían a ser opacas y el diseño no se extendería
    //   de manera inmersiva en la pantalla.
    enableEdgeToEdge() // Elimina las restricciones de barra de estado y navegación opacas

    // Define el árbol de UI declarativo usando Jetpack Compose.
    // - ¿Cómo funciona? Carga todos los componentes Compose dentro de la jerarquía de la pantalla.
    // - ¿Qué pasa si lo quitamos? La pantalla de la aplicación se quedará completamente vacía (en blanco o negro).
    setContent { // Inicia la inyección del árbol de UI
      // Wrapper del tema de la aplicación: proporciona los colores y tipografías a los componentes hijos.
      MyApplicationTheme { // Aplica nuestro tema personalizado
        HabitOctoProfileApp() // Llama a la UI principal
      }
    }
  }
}

// ============================================================================
// COMPOSABLE GENERAL DE LA PANTALLA: HabitOctoProfileApp
// ============================================================================

/**
 * HabitOctoProfileApp es el contenedor principal de la UI de la pantalla de perfil.
 *
 * - **¿Para qué sirve?** Estructura toda la pantalla usando el patrón Scaffold, controlando la barra de
 *   navegación inferior y el desplazamiento vertical de todas las secciones del perfil.
 * - **¿Cómo funciona?**
 *   1. Utiliza `remember` para persistir el estado de la pestaña seleccionada (`selectedTab`).
 *   2. Aplica un `Scaffold` con un fondo acorde al tema actual (claro/oscuro).
 *   3. En su `bottomBar`, dibuja una `NavigationBar` M3 con 3 pestañas ("Tracker", "Feed" y "Profile").
 *   4. En el área del contenido principal, dibuja un contenedor dinámico `LazyColumn` para optimizar el
 *      desplazamiento vertical eficiente y evitar lag o caídas de fotogramas al renderizar.
 * - **¿Qué pasa si lo quitamos?** Se perdería la estructura fundamental de la pantalla: desaparecería
 *   la barra de navegación inferior, los componentes visuales estarían sueltos y no habría desplazamiento.
 */
@Composable // Anotación Compose obligatoria
fun HabitOctoProfileApp() { // Función contenedora de la aplicación
  // Estado mutable que recuerda qué pestaña está seleccionada en el menú inferior.
  // - ¿Para qué sirve? Persiste la selección incluso si la pantalla se redibuja. Por defecto inicia en '2' (Profile).
  // - ¿Qué pasa si lo quitamos? No podríamos rastrear qué pestaña ha pulsado el usuario y la navegación inferior no respondería.
  var selectedTab by remember { mutableIntStateOf(2) } // Almacena el índice de la pestaña activa

  // Contexto local para mostrar notificaciones flotantes (Toasts).
  // - ¿Para qué sirve? Requerido para crear notificaciones emergentes en Android al pulsar elementos.
  // - ¿Qué pasa si lo quitamos? Las llamadas a 'Toast.makeText' fallarían al compilar por falta de contexto.
  val context = LocalContext.current // Acceso al contexto del sistema operativo Android

  // Variable booleana que detecta si el sistema operativo está usando tema oscuro o claro.
  // - ¿Para qué sirve? Permite a los componentes decidir qué colores alternativos usar.
  // - ¿Qué pasa si lo quitamos? La app no reaccionará dinámicamente a los cambios de tema claro y oscuro en tiempo real.
  val isDark = isSystemInDarkTheme() // Retorna true si el modo oscuro está activo en los ajustes del dispositivo

  Scaffold( // Contenedor de la estructura básica de la aplicación
    // Color de fondo adaptable de acuerdo al esquema de color activo del tema.
    containerColor = MaterialTheme.colorScheme.background, // Usa el color del tema actual

    // Estructura de navegación inferior.
    bottomBar = { // Define la sección inferior
      NavigationBar( // Barra inferior de navegación
        containerColor = MaterialTheme.colorScheme.background, // Sincroniza color con el fondo general
        contentColor = MaterialTheme.colorScheme.primary, // Define color primario para elementos
        tonalElevation = 2.dp, // Aplica sombra de elevación
        // Aplica un padding inferior para no colisionar con la barra de navegación del sistema Android física o gestual.
        modifier = Modifier.padding( // Aplica márgenes externos
          bottom = WindowInsets.navigationBars.asPaddingValues().calculateBottomPadding() // Mide y compensa la barra de Android
        )
      ) { // Bloque de botones internos
        val selectedColor = MaterialTheme.colorScheme.onBackground // Color de pestaña activa
        val unselectedColor = if (isDark) TextGrayDark else TextGrayLight // Color adaptable para pestaña inactiva
        val indicatorBg = MaterialTheme.colorScheme.secondary // Color de fondo del óvalo de selección activa

        // PESTAÑA 1: Tracker
        NavigationBarItem( // Primer botón
          selected = selectedTab == 0, // Verifica si está activa
          onClick = { // Acción al presionar
            selectedTab = 0 // Cambia el estado mutable
            Toast.makeText(context, "Tracker seleccionado", Toast.LENGTH_SHORT).show() // Mensaje flotante
          },
          icon = { // Icono a dibujar
            Icon( // Componente gráfico vectorial
              imageVector = Icons.Outlined.Adjust, // Asigna el icono de círculo
              contentDescription = "Tracker" // Texto descriptivo para lectores de pantalla
            )
          },
          label = { // Texto debajo del icono
            Text( // Componente de texto
              text = "Tracker", // Etiqueta
              style = MaterialTheme.typography.labelSmall, // Fuente pequeña
              fontWeight = if (selectedTab == 0) FontWeight.Bold else FontWeight.Medium // Negrita si está activa
            )
          },
          colors = NavigationBarItemDefaults.colors( // Configuración detallada de colores
            selectedIconColor = selectedColor, // Icono activo
            selectedTextColor = selectedColor, // Texto activo
            indicatorColor = indicatorBg, // Fondo óvalo
            unselectedIconColor = unselectedColor, // Icono inactivo
            unselectedTextColor = unselectedColor // Texto inactivo
          )
        )

        // PESTAÑA 2: Feed
        NavigationBarItem( // Segundo botón
          selected = selectedTab == 1, // Verifica si está activa
          onClick = { // Acción al presionar
            selectedTab = 1 // Cambia el estado mutable
            Toast.makeText(context, "Feed seleccionado", Toast.LENGTH_SHORT).show() // Mensaje emergente
          },
          icon = { // Icono
            Icon( // Componente
              imageVector = Icons.Outlined.Group, // Icono de grupo/comunidad
              contentDescription = "Feed" // Descripción
            )
          },
          label = { // Etiqueta
            Text( // Componente
              text = "Feed", // Título
              style = MaterialTheme.typography.labelSmall, // Estilo
              fontWeight = if (selectedTab == 1) FontWeight.Bold else FontWeight.Medium // Grosor de letra adaptable
            )
          },
          colors = NavigationBarItemDefaults.colors( // Aplica el mismo esquema de color
            selectedIconColor = selectedColor,
            selectedTextColor = selectedColor,
            indicatorColor = indicatorBg,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor
          )
        )

        // PESTAÑA 3: Profile (Pestaña del laboratorio actual)
        NavigationBarItem( // Tercer botón
          selected = selectedTab == 2, // Activo por defecto
          onClick = { // Acción al pulsar
            selectedTab = 2 // Cambia el estado a activo
          },
          icon = { // Icono
            Icon( // Componente
              imageVector = Icons.Default.Person, // Icono relleno de usuario
              contentDescription = "Profile" // Descripción
            )
          },
          label = { // Etiqueta
            Text( // Componente
              text = "Profile", // Texto
              style = MaterialTheme.typography.labelSmall, // Estilo tipográfico
              fontWeight = if (selectedTab == 2) FontWeight.Bold else FontWeight.Medium // Grosor adaptable
            )
          },
          colors = NavigationBarItemDefaults.colors( // Colores adaptables
            selectedIconColor = selectedColor,
            selectedTextColor = selectedColor,
            indicatorColor = indicatorBg,
            unselectedIconColor = unselectedColor,
            unselectedTextColor = unselectedColor
          )
        )
      }
    }
  ) { innerPadding -> // Recibe márgenes calculados por Scaffold para evitar solapamientos
    // LazyColumn: Contenedor vertical optimizado para listas de gran tamaño o contenido dinámico.
    // - ¿Para qué sirve? Renderiza de forma eficiente solo lo que está visible en pantalla, ahorrando RAM y batería.
    // - ¿Cómo funciona? Define los bloques interiores mediante 'item', permitiendo organizar la pantalla por secciones.
    // - ¿Qué pasa si lo quitamos? La pantalla no se podrá desplazar, provocando que los elementos inferiores queden inaccesibles.
    LazyColumn( // Columna recicladora
      modifier = Modifier // Inicia modificador
        .fillMaxSize() // Ocupa todo el espacio de pantalla
        .background(MaterialTheme.colorScheme.background) // Fondo adaptable
        .padding(innerPadding) // Aplica los márgenes de Scaffold
        .padding(bottom = 16.dp), // Margen inferior extra para fluidez visual
      verticalArrangement = Arrangement.spacedBy(16.dp) // Espaciado simétrico de 16dp entre secciones de la pantalla (múltiplo de 4).
    ) { // Contenedores de elementos deslizables de la lista
      // Sección 1: Cabecera superior (Logo y campana de notificaciones)
      item { // Primer elemento deslizable
        TopNavigationBarSection() // Llama a la cabecera
      }

      // Sección 2: Cabecera del perfil (Foto, nombre, nivel del usuario)
      item { // Segundo elemento deslizable
        ProfileHeaderSection() // Llama a la sección de perfil
      }

      // Sección 3: Métricas de consistencia, rachas y precisión
      item { // Tercer elemento deslizable
        PersonalStatsSection() // Llama al dashboard de métricas
      }

      // Sección 4: Datos de cuenta (Email, password y verificación de dos factores)
      item { // Cuarto elemento deslizable
        AccountDataSection() // Llama al panel de ajustes de seguridad
      }
    }
  }
}

// ============================================================================
// SECCIÓN 1: TopNavigationBarSection (Barra de estado superior de la aplicación)
// ============================================================================

/**
 * TopNavigationBarSection renderiza la cabecera superior de la aplicación.
 *
 * - **¿Para qué sirve?** Muestra el branding/nombre de la app ("HabitOcto") junto a su avatar insignia
 *   y un botón rápido para acceder a las notificaciones.
 * - **¿Cómo funciona?** Usa un `Row` horizontal alinear al centro.
 *   - A la izquierda dibuja una `AsyncImage` (cargada mediante Coil) y un `Text`.
 *   - A la derecha coloca un `IconButton` con un icono vectorial de campana.
 * - **¿Qué pasa si lo quitamos?** Se eliminaría el encabezado de la app, lo que reduciría la identidad
 *   visual de la marca y quitaría el acceso directo a las notificaciones.
 */
@Composable // Composable de interfaz de usuario
fun TopNavigationBarSection() { // Cabecera del logo y notificaciones
  val context = LocalContext.current // Captura contexto del sistema operativo Android

  Row( // Contenedor horizontal
    modifier = Modifier // Inicia modificador
      .fillMaxWidth() // Ancho completo
      .padding( // Espaciado exterior e interior
        top = WindowInsets.statusBars.asPaddingValues().calculateTopPadding() + 8.dp, // Evita solaparse con la barra de notificaciones superior física de Android.
        start = 20.dp, // Margen izquierdo
        end = 20.dp, // Margen derecho
        bottom = 8.dp // Margen inferior
      ),
    horizontalArrangement = Arrangement.SpaceBetween, // Separa la marca de la campana a los extremos
    verticalAlignment = Alignment.CenterVertically // Centra los elementos verticalmente
  ) {
    // Fila izquierda: Logo del pulpo (representado por una imagen) y el nombre "HabitOcto"
    Row( // Sub-fila izquierda
      verticalAlignment = Alignment.CenterVertically, // Centrado vertical de la imagen y el texto
      horizontalArrangement = Arrangement.spacedBy(12.dp) // Espacio interno de 12dp entre imagen y texto
    ) {
      AsyncImage( // Componente cargador de imágenes desde URL
        model = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=150", // Enlace de imagen remota
        contentDescription = "App Logo Avatar", // Descripción
        modifier = Modifier // Modificadores
          .size(38.dp) // Tamaño cuadrado de 38dp
          .clip(CircleShape), // Recorta en forma redonda para respetar el estándar visual.
        contentScale = ContentScale.Crop // Recorta proporcionalmente cubriendo el espacio
      )
      Text( // Componente de texto para marca
        text = "HabitOcto", // Nombre de la aplicación
        color = MaterialTheme.colorScheme.primary, // Color primario Teal
        style = MaterialTheme.typography.titleLarge, // Texto de tamaño título
        fontWeight = FontWeight.Bold // Fuente gruesa/negrita
      )
    }

    // Botón de notificaciones (Derecha)
    IconButton( // Componente de botón interactivo circular para icono
      onClick = { // Acción al hacer clic
        Toast.makeText(context, "Notificaciones", Toast.LENGTH_SHORT).show() // Muestra notificación flotante
      }
    ) {
      Icon( // Componente de icono vectorial
        imageVector = Icons.Outlined.Notifications, // Icono de campana delineada
        contentDescription = "Notifications", // Descripción
        tint = MaterialTheme.colorScheme.primary, // Tono principal
        modifier = Modifier.size(26.dp) // Tamaño de 26dp
      )
    }
  }
}

// ============================================================================
// SECCIÓN 2: ProfileHeaderSection (Cabecera de Perfil del Usuario)
// ============================================================================

/**
 * ProfileHeaderSection dibuja los datos de presentación del perfil.
 *
 * - **¿Para qué sirve?** Presenta la foto de perfil en alta resolución con un borde decorativo,
 *   un indicador flotante del nivel de ludificación (Level Badge), el nombre del usuario y su rol.
 * - **¿Cómo funciona?**
 *   - Utiliza una caja contenedora (`Box`) para superponer el badge naranja "LVL 12" sobre el borde
 *     inferior del avatar circular (`AsyncImage`).
 *   - Debajo, un `Column` organiza verticalmente el nombre y la descripción con espaciados en múltiplos de 4dp.
 * - **¿Qué pasa si lo quitamos?** La pantalla perdería la identidad del usuario activo; no se sabría
 *   quién ha iniciado sesión ni su progreso de juego de hábitos.
 */
@Composable // Composable de interfaz
fun ProfileHeaderSection() { // Cabecera del usuario
  val isDark = isSystemInDarkTheme() // Verifica si el sistema usa tema oscuro
  Column( // Contenedor vertical centrado
    modifier = Modifier // Modificador
      .fillMaxWidth() // Ancho completo
      .padding(top = 12.dp, bottom = 8.dp), // Espaciados vertical
    horizontalAlignment = Alignment.CenterHorizontally // Centra todo el bloque en horizontal
  ) {
    // Contenedor que superpone elementos (Avatar circular + Badge del nivel)
    Box( // Contenedor de superposición
      contentAlignment = Alignment.BottomCenter, // Alínea el badge al fondo y al centro sobre la imagen
      modifier = Modifier.padding(bottom = 6.dp) // Espaciado inferior de seguridad
    ) {
      // Imagen del perfil de Alex Rivero
      AsyncImage( // Carga imagen remota de perfil
        model = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400", // Foto de hombre de Unsplash
        contentDescription = "Profile Picture", // Descripción
        modifier = Modifier // Modificadores de la imagen
          .size(100.dp) // Dimensiones de la imagen
          .border(width = 3.dp, color = MaterialTheme.colorScheme.secondary, shape = CircleShape) // Borde del color secundario del tema.
          .clip(CircleShape), // Recorta la imagen final en un círculo perfecto
        contentScale = ContentScale.Crop // Ajuste proporcional
      )

      // Pastilla de Nivel (Badge)
      Box( // Contenedor decorativo de nivel
        modifier = Modifier // Modificadores
          .offset(y = 10.dp) // Desplaza el componente levemente hacia abajo para superponerlo estéticamente sobre la imagen.
          .background(color = BadgeOrange, shape = RoundedCornerShape(12.dp)) // Fondo naranja y esquinas redondeadas
          .padding(horizontal = 12.dp, vertical = 3.dp) // Relleno interno del texto del nivel
      ) {
        Text( // Componente de texto para nivel
          text = "LVL 12", // Texto a mostrar
          color = Color.White, // Letra de color blanco
          style = MaterialTheme.typography.labelSmall, // Estilo pequeño
          fontWeight = FontWeight.Bold // Letras gruesas
        )
      }
    }

    Spacer(modifier = Modifier.height(18.dp)) // Genera espacio para que el badge no choque con el texto de abajo

    // Nombre principal del usuario
    Text( // Componente
      text = "Alex Rivero", // Nombre
      style = MaterialTheme.typography.titleLarge, // Título grande
      fontWeight = FontWeight.Bold, // Letra en negrita
      color = MaterialTheme.colorScheme.onBackground // Color de texto acorde al tema actual
    )

    Spacer(modifier = Modifier.height(4.dp)) // Espacio mínimo de separación

    // Subtítulo e información secundaria
    Text( // Componente
      text = "Master of Focus • Joined March 2024", // Subtexto informativo
      style = MaterialTheme.typography.bodyMedium, // Estilo de cuerpo medio
      fontWeight = FontWeight.Medium, // Peso intermedio
      color = if (isDark) TextGrayDark else TextGrayLight // Aplica color gris adaptable según el tema del celular
    )
  }
}

// ============================================================================
// SECCIÓN 3: PersonalStatsSection (Dashboard de Estadísticas del Usuario)
// ============================================================================

/**
 * PersonalStatsSection renderiza un tablero modular que resume el rendimiento mensual del usuario.
 *
 * - **¿Para qué sirve?** Muestra visualmente métricas de consistencia general mediante un gráfico
 *   de barras vertical, días consecutivos de racha (Streak) y el porcentaje de precisión en los hábitos.
 * - **¿Cómo funciona?**
 *   - Implementa un `Column` contenedor.
 *   - Crea una tarjeta principal para "Consistency" que alberga una fila con el valor numérico, el icono,
 *     y un gráfico compuesto por una serie de rectángulos (`Box`) con alturas calculadas en `dp` para simular barras.
 *   - Abajo dibuja una fila (`Row`) con dos tarjetas con pesos equivalentes (`weight(1f)`) que contienen
 *     la racha actual y la precisión. Las tarjetas alternan fondos y colores acordes al modo claro u oscuro del tema.
 * - **¿Qué pasa si lo quitamos?** Se eliminaría el valor de gamificación y motivación principal de la
 *   aplicación; el usuario ya no tendría un resumen visual de sus resultados del mes.
 */
@Composable // Composable de UI
fun PersonalStatsSection() { // Sección de métricas
  val isDark = isSystemInDarkTheme() // Revisa si el sistema usa tema oscuro
  val textGray = if (isDark) TextGrayDark else TextGrayLight // Color adaptable para textos secundarios
  val iconBg = if (isDark) DarkIconBoxBg else LightIconBoxBg // Fondo adaptable para el contenedor del icono
  val cardStreakBg = if (isDark) CardStreakDark else CardStreakLight // Fondo de la tarjeta de racha (adaptable)
  val numberStreakColor = if (isDark) NumberStreakDark else NumberStreakLight // Color del número de racha
  val subtitleStreakColor = if (isDark) SubtitleStreakDark else SubtitleStreakLight // Color de subtítulos de racha
  val cardAccuracyBg = if (isDark) CardAccuracyDark else CardAccuracyLight // Fondo de la tarjeta de precisión (adaptable)
  val numberAccuracyColor = if (isDark) NumberAccuracyDark else NumberAccuracyLight // Color del número de precisión
  val subtitleAccuracyColor = if (isDark) SubtitleAccuracyDark else SubtitleAccuracyLight // Color del subtítulo de precisión

  Column( // Contenedor vertical
    modifier = Modifier // Modificadores
      .fillMaxWidth() // Ancho completo
      .padding(horizontal = 20.dp), // Margen izquierdo y derecho de 20dp
    verticalArrangement = Arrangement.spacedBy(12.dp) // Espacio vertical de 12dp entre elementos de la columna
  ) {
    // Encabezado de Estadísticas
    Row( // Fila de cabecera de la sección
      modifier = Modifier.fillMaxWidth(), // Ocupa todo el ancho
      horizontalArrangement = Arrangement.SpaceBetween, // Separa los textos a los extremos
      verticalAlignment = Alignment.Bottom // Alineación inferior
    ) {
      Text( // Título del dashboard
        text = "Personal Stats", // Nombre
        style = MaterialTheme.typography.titleMedium, // Título mediano
        fontWeight = FontWeight.Bold, // Negrita
        color = MaterialTheme.colorScheme.primary // Verde Teal
      )
      Text( // Período temporal
        text = "THIS MONTH", // Texto
        style = MaterialTheme.typography.labelSmall, // Fuente muy pequeña
        fontWeight = FontWeight.Bold, // Peso grueso
        color = textGray // Gris adaptable
      )
    }

    // TARJETA 1: Consistency (Consistencia general y gráfico de barras)
    Card( // Tarjeta Material 3
      modifier = Modifier.fillMaxWidth(), // Ancho completo
      shape = RoundedCornerShape(24.dp), // Esquinas fuertemente redondeadas
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Color adaptable
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Usa elevación tonal sobre sombra pura para Material 3.
    ) {
      Column( // Columna interna de la tarjeta
        modifier = Modifier.padding(18.dp) // Relleno interno
      ) {
        Row( // Fila interna
          modifier = Modifier.fillMaxWidth(), // Ancho completo
          horizontalArrangement = Arrangement.SpaceBetween, // Separa el texto del icono a los extremos
          verticalAlignment = Alignment.Top // Alinea al borde superior
        ) {
          Column { // Sub-columna de datos numéricos
            Text( // Nombre métrica
              text = "Consistency",
              style = MaterialTheme.typography.bodyMedium,
              fontWeight = FontWeight.Medium,
              color = textGray
            )
            Spacer(modifier = Modifier.height(4.dp)) // Espaciador
            Text( // Valor numérico principal
              text = "87%",
              style = MaterialTheme.typography.displayMedium,
              fontWeight = FontWeight.Bold,
              color = MaterialTheme.colorScheme.primary
            )
          }

          // Icono decorativo de consistencia
          Box( // Contenedor del icono
            modifier = Modifier // Modificadores
              .size(40.dp) // Tamaño cuadrado de 40dp
              .background(color = iconBg, shape = RoundedCornerShape(12.dp)), // Fondo redondeado adaptable
            contentAlignment = Alignment.Center // Centra el icono en la caja
          ) {
            Icon( // Componente
              imageVector = Icons.Outlined.Adjust, // Icono de anillo de precisión
              contentDescription = "Focus Icon", // Descripción
              tint = MaterialTheme.colorScheme.primary, // Color primario
              modifier = Modifier.size(24.dp) // Tamaño
            )
          }
        }

        Spacer(modifier = Modifier.height(16.dp)) // Separador vertical previo al gráfico

        // Gráfico de barras decorativo
        val barHeights = listOf(22.dp, 36.dp, 28.dp, 48.dp, 34.dp, 44.dp, 24.dp) // Alturas variables para simular progreso
        Row( // Contenedor horizontal de barras
          modifier = Modifier // Modificadores
            .fillMaxWidth() // Ancho completo
            .height(52.dp), // Altura fija del gráfico
          horizontalArrangement = Arrangement.spacedBy(8.dp), // Espaciado horizontal de 8dp entre cada columna del gráfico
          verticalAlignment = Alignment.Bottom // Alineación al fondo para que crezcan hacia arriba
        ) {
          barHeights.forEach { heightDp -> // Itera la lista de alturas
            Box( // Dibuja cada barra individualmente
              modifier = Modifier // Modificador
                .weight(1f) // Distribuye el ancho horizontal equitativamente entre las 7 columnas
                .height(heightDp) // Altura correspondiente de la lista
                .background(color = MaterialTheme.colorScheme.secondary, shape = RoundedCornerShape(topStart = 4.dp, topEnd = 4.dp)) // Redondea sólo las esquinas superiores de la barra
            )
          }
        }
      }
    }

    // Fila inferior de 2 tarjetas: Streak (Racha) y Accuracy (Precisión)
    Row( // Contenedor horizontal para emparejar tarjetas
      modifier = Modifier.fillMaxWidth(), // Ancho completo
      horizontalArrangement = Arrangement.spacedBy(12.dp) // Espaciado inter-tarjeta
    ) {
      // TARJETA DE RACHA (STREAK)
      Card( // Tarjeta izquierda
        modifier = Modifier.weight(1f), // Toma el 50% del ancho libre
        shape = RoundedCornerShape(24.dp), // Redondeado estético
        colors = CardDefaults.cardColors(containerColor = cardStreakBg), // Aplica la paleta naranja calculada
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Sin sombras oscuras M2
      ) {
        Column( // Contenedor vertical alineado
          modifier = Modifier // Modificadores
            .fillMaxWidth() // Rellena la tarjeta
            .padding(16.dp), // Relleno interno
          horizontalAlignment = Alignment.CenterHorizontally // Centrado de textos y número
        ) {
          Text( // Título
            text = "CURRENT STREAK", // Etiqueta
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = subtitleStreakColor // Color del tema de racha
          )
          Spacer(modifier = Modifier.height(4.dp)) // Separador
          Text( // Número
            text = "14", // Racha de días
            style = MaterialTheme.typography.displayMedium,
            fontWeight = FontWeight.Bold,
            color = numberStreakColor // Color del número destacado
          )
          Spacer(modifier = Modifier.height(2.dp)) // Separador
          Text( // Subtítulo descriptivo
            text = "Days Strong", // Días completados
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = subtitleStreakColor // Tono marrón o naranja suave
          )
        }
      }

      // TARJETA DE PRECISIÓN (ACCURACY)
      Card( // Tarjeta derecha
        modifier = Modifier.weight(1f), // Toma el otro 50% de ancho
        shape = RoundedCornerShape(24.dp), // Esquinas redondeadas
        colors = CardDefaults.cardColors(containerColor = cardAccuracyBg), // Carga gama lavanda
        elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Sin elevaciones antiguas
      ) {
        Column( // Contenedor vertical
          modifier = Modifier // Modificadores
            .fillMaxWidth() // Rellena la tarjeta
            .padding(16.dp), // Relleno
          horizontalAlignment = Alignment.CenterHorizontally // Centrado
        ) {
          Text( // Título
            text = "ACCURACY", // Etiqueta
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = subtitleAccuracyColor // Color lavanda/azul de racha
          )
          Spacer(modifier = Modifier.height(4.dp)) // Separador
          Row( // Alinea horizontalmente el número y el símbolo de porcentaje
            verticalAlignment = Alignment.Bottom // Alineación inferior
          ) {
            Text( // Número de precisión
              text = "92", // Porcentaje
              style = MaterialTheme.typography.displayMedium,
              fontWeight = FontWeight.Bold,
              color = numberAccuracyColor // Color del texto
            )
            Text( // Símbolo %
              text = "%", // Símbolo
              style = MaterialTheme.typography.titleLarge,
              fontWeight = FontWeight.Bold,
              color = numberAccuracyColor,
              modifier = Modifier.padding(bottom = 4.dp, start = 2.dp) // Ajusta posición alineado levemente arriba de la base
            )
          }
          Spacer(modifier = Modifier.height(2.dp)) // Separador
          Text( // Mensaje secundario
            text = "Habit Integrity", // Nombre del índice
            style = MaterialTheme.typography.bodySmall,
            fontWeight = FontWeight.Medium,
            color = subtitleAccuracyColor // Tono azul lavanda
          )
        }
      }
    }
  }
}

// ============================================================================
// SECCIÓN 4: AccountDataSection (Ajustes y Datos de Configuración de la Cuenta)
// ============================================================================

/**
 * AccountDataSection renderiza el panel de configuraciones y datos de la cuenta.
 *
 * - **¿Para qué sirve?** Permite gestionar y visualizar la dirección de correo electrónico vinculada,
 *   la contraseña encriptada (con botones interactivos de cambio/actualización) y activar/desactivar la
 *   verificación de dos factores (2FA).
 * - **¿Cómo funciona?** Dibuja tres tarjetas apiladas verticalmente.
 *   - Las tarjetas de "Email" y "Password" son clicables e implementan botones a la derecha que disparan notificaciones Toast.
 *   - La tarjeta de "Two-Factor Auth" gestiona un estado mutable (`isTwoFactorEnabled`) que cambia el valor del Switch
 *     e interactúa modificando los colores de la UI nativamente de manera animada.
 * - **¿Qué pasa si lo quitamos?** El usuario no tendría acceso a las opciones de seguridad básicas,
 *   impidiendo cambiar su correo, actualizar su contraseña o activar 2FA para proteger su perfil.
 */
@Composable // Composable de UI
fun AccountDataSection() { // Sección de configuración de cuenta
  val context = LocalContext.current // Contexto local de Android

  // Estado mutable que recuerda si el interruptor de 2FA está encendido o apagado.
  // - ¿Para qué sirve? Permite al usuario conmutar el estado de seguridad en tiempo real.
  // - ¿Qué pasa si lo quitamos? El switch de 2FA no recordaría su estado ni cambiaría de posición al tocarlo.
  var isTwoFactorEnabled by remember { mutableStateOf(true) } // Declara el estado mutable
  val isDark = isSystemInDarkTheme() // Verifica si está activo el modo oscuro
  val textGray = if (isDark) TextGrayDark else TextGrayLight // Selecciona color gris de texto

  Column( // Contenedor vertical
    modifier = Modifier // Modificadores
      .fillMaxWidth() // Ancho completo
      .padding(horizontal = 20.dp), // Espaciados horizontales
    verticalArrangement = Arrangement.spacedBy(8.dp) // Espaciado mínimo de 8dp entre filas
  ) {
    // Título de la sección
    Text( // Título general de seguridad
      text = "Account Data", // Cabecera
      style = MaterialTheme.typography.titleMedium, // Fuente mediana
      fontWeight = FontWeight.Bold, // Letra gruesa
      color = MaterialTheme.colorScheme.primary, // Color Teal
      modifier = Modifier.padding(vertical = 4.dp) // Padding interno
    )

    // FILA 1: Dirección de correo electrónico
    Card( // Tarjeta de Correo
      modifier = Modifier // Modificadores
        .fillMaxWidth() // Ancho completo
        .clickable { // Acción al presionar toda la tarjeta
          Toast.makeText(context, "Cambiar correo electrónico", Toast.LENGTH_SHORT).show() // Acción por defecto
        },
      shape = RoundedCornerShape(20.dp), // Esquinas redondeadas
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Color adaptable
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Sin sombras M2
    ) {
      Row( // Disposición interna en horizontal
        modifier = Modifier // Modificadores
          .fillMaxWidth() // Rellena tarjeta
          .padding(16.dp), // Padding
        verticalAlignment = Alignment.CenterVertically // Centrado vertical de iconos y textos
      ) {
        Icon( // Icono de sobre
          imageVector = Icons.Outlined.Email, // Icono de correo
          contentDescription = "Email", // Descripción
          tint = MaterialTheme.colorScheme.primary, // Color primario
          modifier = Modifier.size(22.dp) // Tamaño
        )
        Spacer(modifier = Modifier.width(14.dp)) // Separador horizontal
        Column( // Columna interna
          modifier = Modifier.weight(1f) // Ocupa todo el espacio intermedio sobrante
        ) {
          Text( // Título de la tarjeta
            text = "EMAIL ADDRESS", // Etiqueta
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textGray // Gris adaptable
          )
          Spacer(modifier = Modifier.height(2.dp)) // Separador vertical mínimo
          Text( // Correo censurado
            text = "alex.r****@gmail.com", // Dirección de correo
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface // Color según el tema actual
          )
        }
        Text( // Botón de acción rápido
          text = "Change", // Cambiar
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary, // Color Teal
          modifier = Modifier.clickable { // Acción interactiva al pulsarlo
            Toast.makeText(context, "Cambiar correo", Toast.LENGTH_SHORT).show() // Feedback
          }
        )
      }
    }

    // FILA 2: Contraseña
    Card( // Tarjeta de Contraseña
      modifier = Modifier // Modificadores
        .fillMaxWidth() // Ancho completo
        .clickable { // Clic en toda la fila
          Toast.makeText(context, "Actualizar contraseña", Toast.LENGTH_SHORT).show() // Feedback
        },
      shape = RoundedCornerShape(20.dp), // Esquinas
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Fondo
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Elevación
    ) {
      Row( // Fila horizontal
        modifier = Modifier // Modificadores
          .fillMaxWidth() // Ancho completo
          .padding(16.dp), // Relleno
        verticalAlignment = Alignment.CenterVertically // Alineado central
      ) {
        Icon( // Icono de candado
          imageVector = Icons.Outlined.Lock, // Candado delineado
          contentDescription = "Password", // Descripción
          tint = MaterialTheme.colorScheme.primary, // Color primario
          modifier = Modifier.size(22.dp) // Tamaño
        )
        Spacer(modifier = Modifier.width(14.dp)) // Espacio horizontal
        Column( // Columna de información
          modifier = Modifier.weight(1f) // Ocupa espacio medio
        ) {
          Text( // Título
            text = "PASSWORD", // Etiqueta
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textGray
          )
          Spacer(modifier = Modifier.height(2.dp)) // Espacio mínimo
          Text( // Puntos simulando contraseña oculta
            text = "••••••••••••", // Representación visual
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Medium,
            color = MaterialTheme.colorScheme.onSurface
          )
        }
        Text( // Botón de actualización
          text = "Update", // Actualizar
          style = MaterialTheme.typography.bodyLarge,
          fontWeight = FontWeight.Bold,
          color = MaterialTheme.colorScheme.primary, // Teal
          modifier = Modifier.clickable { // Clic
            Toast.makeText(context, "Actualizar contraseña", Toast.LENGTH_SHORT).show() // Feedback
          }
        )
      }
    }

    // FILA 3: Verificación en Dos Pasos (Two-Factor Auth)
    Card( // Tarjeta de 2FA
      modifier = Modifier.fillMaxWidth(), // Ancho completo
      shape = RoundedCornerShape(20.dp), // Redondeado
      colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surface), // Fondo adaptable
      elevation = CardDefaults.cardElevation(defaultElevation = 0.dp) // Elevación nula
    ) {
      Row( // Fila horizontal
        modifier = Modifier // Modificadores
          .fillMaxWidth() // Ancho completo
          .padding(16.dp), // Relleno interno
        verticalAlignment = Alignment.CenterVertically // Centrado vertical
      ) {
        Icon( // Icono de escudo
          imageVector = Icons.Outlined.Shield, // Escudo de seguridad
          contentDescription = "Two-Factor Auth", // Descripción
          tint = MaterialTheme.colorScheme.primary, // Color Teal
          modifier = Modifier.size(22.dp) // Tamaño
        )
        Spacer(modifier = Modifier.width(14.dp)) // Espaciador horizontal
        Column( // Columna de datos
          modifier = Modifier.weight(1f) // Ocupa espacio intermedio
        ) {
          Text( // Título
            text = "TWO-FACTOR AUTH", // Etiqueta
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold,
            color = textGray
          )
          Spacer(modifier = Modifier.height(2.dp)) // Espacio mínimo
          Text( // Estado de activación
            text = if (isTwoFactorEnabled) "Enabled" else "Disabled", // Evaluado dinámicamente
            style = MaterialTheme.typography.bodyLarge,
            fontWeight = FontWeight.Bold, // Negrita
            color = if (isTwoFactorEnabled) MaterialTheme.colorScheme.primary else textGray // Si está activo se pinta Teal, sino gris
          )
        }

        // Switch interactivo de alternancia
        Switch( // Interruptor deslizable
          checked = isTwoFactorEnabled, // Vincula su estado activo
          onCheckedChange = { isTwoFactorEnabled = it }, // Acción de alternar
          colors = SwitchDefaults.colors( // Personaliza los colores del switch
            checkedThumbColor = Color.White, // Color del círculo interior activo
            checkedTrackColor = MaterialTheme.colorScheme.secondary, // Fondo de barra activo (Teal brillante)
            uncheckedThumbColor = MaterialTheme.colorScheme.onSurface, // Color del círculo inactivo
            uncheckedTrackColor = MaterialTheme.colorScheme.background, // Fondo inactivo
            uncheckedBorderColor = textGray // Borde inactivo
          )
        )
      }
    }
  }
}

// ============================================================================
// PREVISUALIZACIONES (PREVIEWS)
// ============================================================================

/**
 * HabitOctoProfilePreview renderiza la previsualización del diseño en Modo Claro.
 *
 * - **¿Para qué sirve?** Permite a los desarrolladores ver los cambios visuales aplicados de
 *   inmediato dentro de la pestaña de diseño (Design View) de Android Studio sin necesidad de
 *   compilar y ejecutar la app en un emulador o dispositivo real.
 * - **¿Cómo funciona?** Llama a `HabitOctoProfileApp` envuelto en `MyApplicationTheme` enviándole
 *   por parámetro `darkTheme = false`.
 * - **¿Qué pasa si lo quitamos?** No podríamos ver una vista previa interactiva en vivo del modo claro
 *   en el editor visual del IDE.
 */
@Preview(showBackground = true) // Habilita vista previa con fondo
@Composable // Composable de previsualización
fun HabitOctoProfilePreview() { // Función
  MyApplicationTheme(darkTheme = false) { // Tema claro
    HabitOctoProfileApp() // Carga app
  }
}

/**
 * HabitOctoProfileDarkPreview renderiza la previsualización en Modo Oscuro.
 *
 * - **¿Para qué sirve?** Permite validar visualmente de forma inmediata cómo se adaptan todos
 *   los elementos y colores contrastantes bajo la paleta nocturna en la vista previa del IDE.
 * - **¿Cómo funciona?** Llama a `HabitOctoProfileApp` pasándole por parámetro `darkTheme = true`.
 * - **¿Qué pasa si lo quitamos?** Perderíamos la capacidad de verificar visualmente el modo oscuro
 *   desde el IDE sin tener que instalar la app.
 */
@Preview(showBackground = true) // Habilita vista previa con fondo oscuro
@Composable // Composable
fun HabitOctoProfileDarkPreview() { // Función
  MyApplicationTheme(darkTheme = true) { // Tema oscuro
    HabitOctoProfileApp() // Carga app
  }
}
