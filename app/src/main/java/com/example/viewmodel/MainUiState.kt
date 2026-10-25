package com.example.viewmodel

import com.example.model.*

// ============================================================================
// ESTADO DE LA INTERFAZ: MainUiState
// ============================================================================

/**
 * MainUiState representa el estado unificado de las pantallas de HabitOcto.
 *
 * - **¿Para qué sirve?** Reúne hábitos, historial, estadísticas, perfil y controles temporales de
 *   navegación y diálogos.
 * - **¿Cómo funciona?** Los datos persistentes proceden del repositorio; selectedTab y los diálogos
 *   controlan la sesión de interfaz.
 * - **¿Qué pasa si lo quitamos?** Las pantallas tendrían estados separados que podrían quedar
 *   desactualizados.
 */
data class MainUiState(
    val habits: List<HabitItem> = emptyList(),
    val feedPosts: List<FeedPostItem> = emptyList(),
    val selectedTab: Int = 0,
    val isDarkMode: Boolean = false,
    val isAddHabitDialogOpen: Boolean = false,
    val habitEditingTarget: HabitItem? = null,
    val history: List<HabitHistoryItem> = emptyList(),
    val summary: TrackingSummary = TrackingSummary(),
    val user: UserEntity? = null,
    val dataState: UiState<Unit> = UiState.Loading,
    val isSaving: Boolean = false,
    val error: String? = null,
    val publicationVersion: Int = 0
) {
    val isLoading: Boolean get() = dataState is UiState.Loading
}
