package com.example.ui.theme

import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.luminance

/**
 * Evaluador reactivo de modo oscuro activo en la jerarquía de Compose.
 * Comprueba la luminosidad de la superficie del tema actual, funcionando
 * correctamente tanto si el tema oscuro proviene del sistema de Android
 * como si fue alternado manualmente desde la aplicación.
 */
val isAppInDarkTheme: Boolean
    @Composable
    get() = MaterialTheme.colorScheme.surface.luminance() < 0.5f

/**
 * Color.kt centraliza todos los tokens y definiciones cromáticas de la aplicación HabitOcto.
 *
 * - **¿Para qué sirve?** Evita colores literales (hardcoded) en los archivos de interfaz,
 *   garantizando coherencia visual, facilidad de mantenimiento y soporte de temas claro/oscuro.
 * - **¿Cómo funciona?** Declara constantes `Color` inmutables con valores ARGB hexadecimales.
 * - **¿Qué pasa si lo quitamos?** Las pantallas y componentes no podrían resolver sus colores temáticos.
 */

// ============================================================================
// COLORES BASE Y TEMAS POR DEFECTO
// ============================================================================

val Purple80 = Color(0xFFD0BCFF)
val PurpleGrey80 = Color(0xFFCCC2DC)
val Pink80 = Color(0xFFEFB8C8)

val Purple40 = Color(0xFF6650a4)
val PurpleGrey40 = Color(0xFF625b71)
val Pink40 = Color(0xFF7D5260)

// ============================================================================
// COLORES CORPORATIVOS E INSIGNIA DE HABITOCTO
// ============================================================================

val PrimaryTeal = Color(0xFF00796B)
val SecondaryTeal = Color(0xFF26C6DA)
val DarkTealBackground = Color(0xFF004D40)
val DeepDarkTeal = Color(0xFF00251A)
val LightTealAccent = Color(0xFFB2DFDB)
val DarkTealText = Color(0xFF00363A)

// Fondos Generales de Pantalla
val ScreenBackgroundLight = Color(0xFFF8F9FA)
val ScreenBackgroundDark = Color(0xFF121212)

// Colores de Texto
val TextDark = Color(0xFF212121)
val TextLight = Color(0xFFEEEEEE)
val TextGrayLight = Color(0xFF757575)
val TextGrayDark = Color(0xFFB0BEC5)

// ============================================================================
// COLORES PARA TARJETAS Y CONTENEDORES
// ============================================================================

// Tarjeta Racha (Streak)
val CardStreakLight = Color(0xFFFFE0B2)
val NumberStreakLight = Color(0xFF3E2723)
val SubtitleStreakLight = Color(0xFFA15D22)

val CardStreakDark = Color(0xFF4E342E)
val NumberStreakDark = Color(0xFFFFCC80)
val SubtitleStreakDark = Color(0xFFFFB74D)

// Tarjeta Precisión (Accuracy)
val CardAccuracyLight = Color(0xFFE8EAF6)
val NumberAccuracyLight = Color(0xFF1A237E)
val SubtitleAccuracyLight = Color(0xFF3F51B5)

val CardAccuracyDark = Color(0xFF1A237E)
val NumberAccuracyDark = Color(0xFFC5CAE9)
val SubtitleAccuracyDark = Color(0xFF9FA8DA)

// Fondos de tarjetas
val CardWhite = Color.White
val CardDark = Color(0xFF1E1E1E)

// Insignias y contenedores de iconos
val BadgeOrange = Color(0xFFFF9800)
val LightIconBoxBg = Color(0xFFE0F7FA)
val LightTealBg = Color(0xFFE0F7FA)
val DarkIconBoxBg = Color(0xFF004D40)

// ============================================================================
// COLORES DE UI, BORDES, CHIPS Y ESTADOS
// ============================================================================

val BorderGrayLight = Color(0xFFE0E0E0)
val BorderGrayDark = Color(0xFF383838)
val BorderTrackDark = Color(0xFF303030)
val BorderInactiveDark = Color(0xFF505050)
val BorderInactiveLight = Color(0xFFD0D0D0)

val SurfaceDarkVariant = Color(0xFF263238)
val SurfaceLightVariant = Color(0xFFE8EAF6)
val ChipBackgroundLight = Color(0xFFF1F3F4)
val ChipBackgroundDark = Color(0xFF2C2C2C)
val DividerDark = Color(0xFF2C2C2C)
val DividerLight = Color(0xFFF0F0F0)

val DangerRed = Color(0xFFE53935)
val SuccessGreen = Color(0xFF4CAF50)
val SuccessGreenDark = Color(0xFF2E7D32)
val InfoBlue = Color(0xFF1976D2)
val InfoBlueDark = Color(0xFF1565C0)
val LikePink = Color(0xFFE91E63)
val TimerOrange = Color(0xFFFF9800)

// ============================================================================
// COLORES PARA CATEGORÍAS (CLARO Y OSCURO)
// ============================================================================

val CategoryHydrationLight = Color(0xFFE0F7FA)
val CategoryHydrationDark = Color(0xFF004D40)

val CategoryLearningLight = Color(0xFFF3E5F5)
val CategoryLearningDark = Color(0xFF311B92)

val CategoryFitnessLight = Color(0xFFFFE0B2)
val CategoryFitnessDark = Color(0xFFE65100)

val CategoryMindfulnessLight = Color(0xFFF8BBD0)
val CategoryMindfulnessDark = Color(0xFF880E4F)

val CategoryProductivityLight = Color(0xFFE8EAF6)
val CategoryProductivityDark = Color(0xFF1A237E)

val CategoryNutritionLight = Color(0xFFDCEDC8)
val CategoryNutritionDark = Color(0xFF33691E)

val CategorySleepLight = Color(0xFFE1BEE7)
val CategorySleepDark = Color(0xFF4A148C)

val CategoryArtLight = Color(0xFFFFF9C4)
val CategoryArtDark = Color(0xFFF57F17)

val CategoryFinanceLight = Color(0xFFC8E6C9)
val CategoryFinanceDark = Color(0xFF1B5E20)

val CategoryMentalLight = Color(0xFFCFD8DC)
val CategoryMentalDark = Color(0xFF263238)

// ============================================================================
// PALETA DEL GRÁFICO SEMANAL
// ============================================================================

val ChartBar1 = Color(0xFF42A5F5)
val ChartBar2 = Color(0xFF00695C)
val ChartBar3 = Color(0xFF00ACC1)
val ChartBar4 = Color(0xFF26A69A)
val ChartBar5 = Color(0xFFFB8C00)
val ChartBar6 = Color(0xFFFFB74D)
val ChartBar7 = Color(0xFFF57C00)
