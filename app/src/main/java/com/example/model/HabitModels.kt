package com.example.model

/**
 * HabitItem representa la estructura inmutable de un hábito individual o micro hábito.
 *
 * - **¿Para qué sirve?** Almacena en memoria toda la información del hábito (título, subtítulo, meta, etc.).
 * - **¿Cómo funciona?** Al ser inmutable con propiedades `val`, garantiza estabilidad en la recomposición de Compose.
 * - **¿Qué pasa si lo quitamos?** La aplicación no podría manipular ni transferir hábitos entre pantallas.
 */
data class HabitItem(
    val id: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val frequency: String = "Diario",
    val streak: Int = 0,
    val isProgressive: Boolean = false,
    val currentProgress: Int = 0,
    val maxProgress: Int = 1,
    val progressUnit: String = "",
    val isCompleted: Boolean = false,
    val isMicroHabit: Boolean = false,
    val tags: List<String> = emptyList(),
    val timeOrDuration: String = "",
    val categoryColorHex: Long = 0xFF00796B
)

/**
 * HabitHistoryItem representa un registro histórico de cumplimiento del hábito.
 */
data class HabitHistoryItem(
    val id: String,
    val habitTitle: String,
    val dateText: String,
    val isCompleted: Boolean
)

/**
 * FeedPostItem representa una publicación en el Feed de la comunidad.
 *
 * - **¿Para qué sirve?** Modela las interacciones sociales (logros, hábitos compartidos, comentarios y reacciones).
 * - **¿Cómo funciona?** Cada post contiene autor, avatar, hábito relacionado, contenido de texto, imagen adjunta, likes y timestamp.
 * - **¿Qué pasa si lo quitamos?** La pantalla de Feed no tendría un modelo de datos estructurado para mostrar posts.
 */
data class FeedPostItem(
    val id: String,
    val authorName: String,
    val authorAvatarUrl: String,
    val habitTitle: String,
    val habitCategory: String,
    val content: String,
    val timeAgo: String,
    val postImageUrl: String? = null, // Imagen adjunta en la publicación del feed
    val likesCount: Int = 0,
    val isLiked: Boolean = false,
    val streakCount: Int = 0
)

/**
 * CategoryItem define una categoría de hábito disponible.
 */
data class CategoryItem(
    val id: String,
    val name: String,
    val iconName: String,
    val backgroundColorHex: Long
)

/**
 * UiState representa el patrón sellado de estados para la UI.
 */
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    object Empty : UiState<Nothing>()
    data class Error(val message: String) : UiState<Nothing>()
}
