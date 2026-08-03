package com.example.ui.theme // Declara el paquete donde reside este archivo de fuentes y tipografías

import androidx.compose.material3.Typography // Importa la clase principal de tipografía de Material Design 3
import androidx.compose.ui.text.TextStyle // Importa la clase que define los estilos individuales de texto
import androidx.compose.ui.text.font.FontFamily // Importa el cargador de familias tipográficas (ej. Sans-serif, Monospace)
import androidx.compose.ui.text.font.FontWeight // Importa los grosores de fuente (ej. Bold, Normal, Medium)
import androidx.compose.ui.unit.sp // Importa la unidad de medida "sp" (scale-independent pixels) para textos

// Definición e inicialización del conjunto de tipografías personalizadas basadas en Material 3
val Typography = Typography(
    // Estilo predeterminado para bloques grandes de texto del cuerpo (ej. párrafos principales)
    bodyLarge = TextStyle(
        fontFamily = FontFamily.Default, // Usa la tipografía predeterminada del sistema Android
        fontWeight = FontWeight.Normal, // Grosor normal de la letra
        fontSize = 16.sp, // Tamaño de letra establecido en 16 sp
        lineHeight = 24.sp, // Altura de línea de 24 sp para una lectura fluida
        letterSpacing = 0.5.sp, // Espaciado entre caracteres de 0.5 sp
    )
)
