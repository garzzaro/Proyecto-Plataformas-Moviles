package com.example.model

// Modelo de datos inmutable para un Hábito (data class con propiedades val)
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
    val tags: List<String> = emptyList(),
    val timeOrDuration: String = "",
    val categoryColorHex: Long = 0xFF00796B
)

// Modelo de datos inmutable para el Historial de Hábitos
data class HabitHistoryItem(
    val id: String,
    val habitTitle: String,
    val dateText: String,
    val isCompleted: Boolean
)

// Modelo para Categorías
data class CategoryItem(
    val id: String,
    val name: String,
    val iconName: String,
    val backgroundColorHex: Long
)

// Estados de la pantalla (Carga, Éxito, Vacío, Error)
sealed class UiState<out T> {
    object Loading : UiState<Nothing>()
    data class Success<T>(val data: T) : UiState<T>()
    object Empty : UiState<Nothing>()
    data class Error(val message: String) : UiState<Nothing>()
}
