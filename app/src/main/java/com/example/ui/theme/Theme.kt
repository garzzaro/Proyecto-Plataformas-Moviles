package com.example.ui.theme // Declara el paquete de la capa de tema visual del proyecto

import android.os.Build // Importa la clase Build para revisar la versión del sistema operativo Android en el dispositivo
import androidx.compose.foundation.isSystemInDarkTheme // Importa la función que detecta si el sistema operativo usa tema oscuro
import androidx.compose.material3.MaterialTheme // Importa la clase contenedora de temas de Material Design 3
import androidx.compose.material3.darkColorScheme // Importa la función constructora de paletas oscuras de Material 3
import androidx.compose.material3.dynamicDarkColorScheme // Importa el generador de esquema dinámico oscuro (Android 12+)
import androidx.compose.material3.dynamicLightColorScheme // Importa el generador de esquema dinámico claro (Android 12+)
import androidx.compose.material3.lightColorScheme // Importa la función constructora de paletas claras de Material 3
import androidx.compose.runtime.Composable // Importa la anotación que define funciones composables en Compose
import androidx.compose.ui.graphics.Color // Importa la clase que define los colores de interfaz
import androidx.compose.ui.platform.LocalContext // Importa el proveedor de contexto Android actual en Compose

// ============================================================================
// ESQUEMAS DE COLOR (TEMAS CLARO Y OSCURO)
// ============================================================================

/**
 * DarkColorScheme define la paleta de colores para el Modo Oscuro.
 *
 * - **¿Para qué sirve?** Asigna los colores que se utilizarán cuando el dispositivo esté en modo noche.
 * - **¿Cómo funciona?** Reemplaza las propiedades básicas de Material3 (`primary`, `background`, `surface`, etc.)
 *   con tonos de teales oscuros, grises y colores de texto claros para facilitar la lectura y evitar fatiga visual.
 * - **¿Qué pasa si lo quitamos?** Si se quita, la app no se adaptaría correctamente al modo noche, viéndose
 *   con fondo blanco a pesar de que el usuario tenga el modo oscuro activo.
 */
private val DarkColorScheme = darkColorScheme(
    primary = PrimaryTeal, // Asigna el Teal corporativo como color primario
    secondary = SecondaryTeal, // Asigna el Teal brillante como color secundario/acento
    background = ScreenBackgroundDark, // Aplica gris oscuro al fondo de pantalla
    surface = CardDark, // Aplica gris intermedio al fondo de las tarjetas
    onPrimary = Color.White, // Color blanco para texto e iconos encima de áreas primarias
    onSecondary = Color.Black, // Color negro para texto e iconos encima de áreas secundarias
    onBackground = TextLight, // Color gris muy claro para el texto principal
    onSurface = TextLight, // Color gris muy claro para textos dentro de tarjetas u hojas
)

/**
 * LightColorScheme define la paleta de colores para el Modo Claro.
 *
 * - **¿Para qué sirve?** Asigna los colores por defecto y diurnos del branding de la aplicación.
 * - **¿Cómo funciona?** Define fondos claros, textos oscuros y el color Teal insignia para acentos.
 * - **¿Qué pasa si lo quitamos?** La aplicación no tendría colores predeterminados para el modo claro clásico.
 */
private val LightColorScheme = lightColorScheme(
    primary = PrimaryTeal, // Asigna el Teal corporativo como color primario
    secondary = SecondaryTeal, // Asigna el Teal brillante como color secundario
    background = ScreenBackgroundLight, // Aplica un fondo gris claro/blanquecino
    surface = CardWhite, // Aplica fondo blanco a las tarjetas del perfil
    onPrimary = Color.White, // Color blanco para elementos sobre botones primarios
    onSecondary = Color.White, // Color blanco para elementos sobre componentes secundarios
    onBackground = TextDark, // Color gris oscuro/negro para textos sobre el fondo general
    onSurface = TextDark, // Color gris oscuro/negro para textos sobre las tarjetas
)

// ============================================================================
// PROVEEDOR DEL TEMA GENERAL: MyApplicationTheme
// ============================================================================

/**
 * MyApplicationTheme es el contenedor composable del tema general de Material 3.
 *
 * - **¿Para qué sirve?** Actúa como proveedor de contexto para inyectar colores y estilos tipográficos
 *   a todos los elementos visuales de la aplicación.
 * - **¿Cómo funciona?**
 *   1. Recibe el parámetro `darkTheme` para evaluar si debe renderizar en claro u oscuro.
 *   2. Permite usar colores dinámicos (Android 12+) si `dynamicColor` es `true`.
 *   3. Llama al componente fundamental `MaterialTheme` de Compose inyectando el `colorScheme` elegido,
 *      la configuración de fuentes `Typography` y dibuja el composable secundario `content`.
 * - **¿Qué pasa si lo quitamos?** Los componentes no podrán acceder a los colores personalizados ni
 *   estilos tipográficos del sistema, cayendo de vuelta a los estilos de color morado/lila por defecto de Compose.
 */
@Composable
fun MyApplicationTheme(
    darkTheme: Boolean = isSystemInDarkTheme(), // Detecta si el sistema de Android usa tema oscuro por defecto
    dynamicColor: Boolean = false, // Parámetro de control para habilitar/deshabilitar colores dinámicos de Android 12
    content: @Composable () -> Unit, // Representa la UI interna a la que se aplicará el tema (Lambda)
) {
    // Determina qué esquema de colores aplicar basándose en la configuración del dispositivo
    val colorScheme = when {
        // Si el color dinámico está activo y la versión del sistema es igual o mayor a Android 12 (API 31/S)
        dynamicColor && Build.VERSION.SDK_INT >= Build.VERSION_CODES.S -> {
            val context = LocalContext.current // Obtiene el contexto actual necesario para cargar recursos de Android 12+
            // Aplica paletas dinámicas basadas en el fondo de pantalla del celular del usuario
            if (darkTheme) dynamicDarkColorScheme(context) else dynamicLightColorScheme(context)
        }
        darkTheme -> DarkColorScheme // Si el sistema pide modo noche, carga la paleta oscura fija
        else -> LightColorScheme // En cualquier otro caso, carga la paleta clara por defecto
    }

    // Inyecta el esquema de colores, tipografías y el contenido hijo a la jerarquía de UI
    MaterialTheme(
        colorScheme = colorScheme, // Aplica el esquema de color resuelto
        typography = Typography, // Aplica el conjunto de tipografías definidas en Type.kt
        content = content // Renderiza la pantalla o componentes envueltos
    )
}
