package com.example.ui.theme // Declara el paquete de la capa de diseño visual (UI Theme)

import androidx.compose.ui.graphics.Color // Importa la clase Color de Jetpack Compose para manejar colores en pantalla

/**
 * Color.kt alberga todos los tokens y definiciones de colores de la aplicación.
 *
 * - **¿Para qué sirve?** Almacena de forma centralizada los valores hexadecimales de color para
 *   evitar que estén dispersos (Hardcoded) por todo el código del proyecto.
 * - **¿Cómo funciona?** Crea variables inmutables (`val`) que instancian la clase `Color` de Compose
 *   con su valor ARGB hexadecimal.
 * - **¿Qué pasa si lo quitamos?** Si se elimina este archivo, el proyecto no compilará debido a la falta
 *   de referencias cromáticas para inicializar los temas claro/oscuro y colorear las tarjetas y textos.
 */

// Colores por defecto del proyecto base (plantilla predeterminada de Compose)
val Purple80 = Color(0xFFD0BCFF) // Tono lila claro para el tema oscuro por defecto
val PurpleGrey80 = Color(0xFFCCC2DC) // Gris lila claro por defecto
val Pink80 = Color(0xFFEFB8C8) // Rosa claro por defecto

val Purple40 = Color(0xFF6650a4) // Tono lila oscuro para el tema claro por defecto
val PurpleGrey40 = Color(0xFF625b71) // Gris lila oscuro por defecto
val Pink40 = Color(0xFF7D5260) // Rosa oscuro por defecto

// ============================================================================
// COLORES CORPORATIVOS E INSIGNIA DE HABITOCTO
// ============================================================================

// Color Primario (Teal/Verde Azulado): Aplicado en títulos principales, textos y botones destacados.
val PrimaryTeal = Color(0xFF00796B) // Color verde azulado oscuro para elementos principales

// Color Secundario (Teal Claro/Acento): Empleado en barras del gráfico, switches activos y bordes de avatares.
val SecondaryTeal = Color(0xFF26C6DA) // Acento celeste/turquesa brillante para realces

// Fondos Generales de Pantalla
val ScreenBackgroundLight = Color(0xFFF8F9FA) // Gris muy claro / Off-white (Modo Claro)
val ScreenBackgroundDark = Color(0xFF121212)  // Gris oscuro / Negro OLED (Modo Oscuro)

// Colores de Texto
val TextDark = Color(0xFF212121)      // Texto principal oscuro para alta legibilidad en modo claro.
val TextLight = Color(0xFFEEEEEE)     // Texto principal claro para alta legibilidad en modo oscuro.
val TextGrayLight = Color(0xFF757575) // Subtítulos/Grises para modo claro.
val TextGrayDark = Color(0xFFB0BEC5)  // Subtítulos/Grises para modo oscuro.

// ============================================================================
// COLORES ESPECÍFICOS PARA LAS TARJETAS MÓVILES
// ============================================================================

// Paleta de la Tarjeta "Current Streak" (Modo Claro - Naranjas suaves y cafés)
val CardStreakLight = Color(0xFFFFE0B2) // Fondo naranja pastel
val NumberStreakLight = Color(0xFF3E2723) // Número marrón oscuro
val SubtitleStreakLight = Color(0xFFA15D22) // Texto marrón intermedio

// Paleta de la Tarjeta "Current Streak" (Modo Oscuro - Tonos naranja apagados y marrones)
val CardStreakDark = Color(0xFF4E342E) // Fondo marrón oscuro/rojizo
val NumberStreakDark = Color(0xFFFFCC80) // Número naranja claro brillante
val SubtitleStreakDark = Color(0xFFFFB74D) // Subtítulo naranja suave

// Paleta de la Tarjeta "Accuracy" (Modo Claro - Lavandas y azules suaves)
val CardAccuracyLight = Color(0xFFE8EAF6) // Fondo lavanda/azul claro
val NumberAccuracyLight = Color(0xFF1A237E) // Número azul índigo oscuro
val SubtitleAccuracyLight = Color(0xFF3F51B5) // Subtítulo azul índigo intermedio

// Paleta de la Tarjeta "Accuracy" (Modo Oscuro - Tonos lavanda oscuros)
val CardAccuracyDark = Color(0xFF1A237E) // Fondo azul índigo oscuro
val NumberAccuracyDark = Color(0xFFC5CAE9) // Número lavanda muy claro
val SubtitleAccuracyDark = Color(0xFF9FA8DA) // Subtítulo lavanda intermedio

// ============================================================================
// ELEMENTOS ADICIONALES Y AUXILIARES
// ============================================================================

val BadgeOrange = Color(0xFFFF9800)    // Color naranja llamativo para la insignia de nivel.
val LightIconBoxBg = Color(0xFFE0F7FA) // Recuadro de icono azul/verde claro.
val DarkIconBoxBg = Color(0xFF004D40)  // Recuadro de icono azul/verde oscuro.

// Colores de Tarjetas Normales
val CardWhite = Color.White        // Fondo blanco de tarjetas en modo claro.
val CardDark = Color(0xFF1E1E1E)   // Fondo gris oscuro de tarjetas en modo oscuro.
