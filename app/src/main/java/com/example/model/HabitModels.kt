package com.example.model // Declaración del paquete de modelos de datos del proyecto

/**
 * HabitItem representa la estructura inmutable de un hábito individual.
 *
 * - **¿Para qué sirve?** Sirve para almacenar toda la información del hábito en memoria (nombre, racha, progreso, etc.).
 * - **¿Cómo funciona?** Al ser una data class inmutable con propiedades "val", previene mutaciones inesperadas de estado,
 *   garantizando la estabilidad de recomposición exigida en Jetpack Compose.
 * - **¿Qué pasa si lo quitamos?** La aplicación no tendría forma de representar, guardar o transferir información de hábitos
 *   entre las diferentes pantallas.
 */
data class HabitItem( // Define la clase de datos para un hábito individual
    val id: String, // Identificador único del hábito (usado para bases de datos y keys estables)
    val title: String, // Título o nombre legible del hábito (ej. "Beber agua")
    val subtitle: String, // Subtítulo descriptivo o meta (ej. "8 vasos al día")
    val category: String, // Categoría a la que pertenece (ej. "Hidratación")
    val frequency: String = "Diario", // Frecuencia programada del hábito ("Diario" o "Semanal")
    val streak: Int = 0, // Racha de cumplimiento consecutivo del hábito
    val isProgressive: Boolean = false, // Determina si tiene una barra de progreso numérica
    val currentProgress: Int = 0, // Valor actual del progreso acumulado del día
    val maxProgress: Int = 1, // Meta máxima de progreso requerida para completarlo
    val progressUnit: String = "", // Unidad física del progreso (ej. "vasos")
    val isCompleted: Boolean = false, // Booleano que define si el hábito fue completado hoy
    val tags: List<String> = emptyList(), // Lista de etiquetas para filtros y chips visuales
    val timeOrDuration: String = "", // Hora de recordatorio o tiempo de duración estimable
    val categoryColorHex: Long = 0xFF00796B // Color asignable para destacar la categoría
) // Fin de la data class HabitItem

/**
 * HabitHistoryItem representa un registro histórico de cumplimiento del hábito.
 *
 * - **¿Para qué sirve?** Almacena el historial de qué hábito fue completado y en qué fecha.
 * - **¿Cómo funciona?** Se asocia mediante un título y una etiqueta de fecha para mostrarse en la lista de historial.
 * - **¿Qué pasa si lo quitamos?** No se podría renderizar el historial de hábitos en la pantalla principal.
 */
data class HabitHistoryItem( // Define la clase de datos para un registro del historial
    val id: String, // Identificador único del registro histórico
    val habitTitle: String, // Nombre del hábito que fue completado/registrado
    val dateText: String, // Fecha legible en formato de texto (ej. "Lun, Oct 21")
    val isCompleted: Boolean // Indica si el hábito se completó con éxito en esa fecha
) // Fin de la data class HabitHistoryItem

/**
 * CategoryItem define una categoría de hábito disponible.
 *
 * - **¿Para qué sirve?** Permite organizar y catalogar los hábitos por su área de enfoque.
 * - **¿Cómo funciona?** Vincula un nombre de categoría con un ícono vectorial y un color de fondo representativo.
 * - **¿Qué pasa si lo quitamos?** Las pantallas de visualización y de creación de hábitos no tendrían categorías dinámicas.
 */
data class CategoryItem( // Define la clase de datos para una categoría
    val id: String, // Identificador único de la categoría
    val name: String, // Nombre de la categoría (ej. "Hidratación")
    val iconName: String, // Nombre del ícono vectorial asociado
    val backgroundColorHex: Long // Color de fondo asignado en formato Hexadecimal
) // Fin de la data class CategoryItem

/**
 * UiState representa el patrón de estados para la pantalla.
 *
 * - **¿Para qué sirve?** Encapsula los estados del ciclo de datos: carga, éxito, vacío y error.
 * - **¿Cómo funciona?** Permite usar evaluación exhaustiva mediante bloques "when" en Compose para cambiar la UI reactivamente.
 * - **¿Qué pasa si lo quitamos?** Sería complejo gestionar de forma segura los estados de carga o errores de datos.
 */
sealed class UiState<out T> { // Define la interfaz sellada de estados de interfaz
    object Loading : UiState<Nothing>() // Estado que representa que se están cargando los datos
    data class Success<T>(val data: T) : UiState<T>() // Estado de éxito que transporta los datos cargados
    object Empty : UiState<Nothing>() // Estado que representa que no hay registros en la lista
    data class Error(val message: String) : UiState<Nothing>() // Estado que almacena un mensaje de error si ocurre un fallo
} // Fin del sealed class UiState
