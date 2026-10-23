package com.example.viewmodel

import androidx.lifecycle.ViewModel
import com.example.model.FeedPostItem
import com.example.model.HabitItem
import com.example.ui.initialFeedPosts
import com.example.ui.initialHabitsList
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/**
 * MainUiState encapsula tod el estado reactivo unificado de la aplicación HabitOcto.
 *
 * - **¿Para qué sirve?** Representa una foto instantánea inmutable de las listas de hábitos,
 *   publicaciones del feed, tema seleccionado y control de diálogos.
 * - **¿Cómo funciona?** Al ser inmutable, previene modificaciones concurrentes accidentales y
 *   facilita la detección de cambios para la recomposición eficiente de Compose.
 */
data class MainUiState(
    val habits: List<HabitItem> = emptyList(),
    val feedPosts: List<FeedPostItem> = emptyList(),
    val selectedTab: Int = 0,
    val isDarkMode: Boolean = false,
    val isAddHabitDialogOpen: Boolean = false,
    val habitEditingTarget: HabitItem? = null
)

/**
 * MainViewModel administra la lógica de negocio y mantiene el estado unificado de la UI.
 *
 * - **¿Para qué sirve?** Aisla el estado reactivo de la capa de vista, sobreviviendo a cambios de
 *   configuración (como rotación de pantalla o cambio de tema claro/oscuro).
 * - **¿Cómo funciona?** Expone un `StateFlow<MainUiState>` de solo lectura y métodos para mutar
 *   el estado de hábitos, likes en el feed, publicaciones y modales.
 * - **¿Qué pasa si lo quitamos?** Los cambios realizados por el usuario (hábitos creados, likes o progreso)
 *   se perderían al rotar la pantalla o conmutar el modo oscuro.
 */
class MainViewModel : ViewModel() {

    private val _uiState = MutableStateFlow(
        MainUiState(
            habits = initialHabitsList,
            feedPosts = initialFeedPosts
        )
    )
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()

    // ------------------------------------------------------------------------
    // ACCIONES DE NAVEGACIÓN Y CONFIGURACIÓN
    // ------------------------------------------------------------------------

    /** Cambia la pestaña activa de la barra de navegación (0: Tracker, 1: Feed, 2: Profile) */
    fun selectTab(tabIndex: Int) {
        _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    /** Alterna el tema de la aplicación entre modo claro y modo noche */
    fun toggleDarkMode() {
        _uiState.update { it.copy(isDarkMode = !it.isDarkMode) }
    }

    /** Establece directamente el estado del modo oscuro */
    fun setDarkMode(enabled: Boolean) {
        _uiState.update { it.copy(isDarkMode = enabled) }
    }

    // ------------------------------------------------------------------------
    // ACCIONES DE HÁBITOS
    // ------------------------------------------------------------------------

    /** Abre el modal para crear un nuevo hábito */
    fun openAddHabitDialog() {
        _uiState.update {
            it.copy(
                isAddHabitDialogOpen = true,
                habitEditingTarget = null
            )
        }
    }

    /** Abre el modal para editar un hábito existente */
    fun openEditHabitDialog(habit: HabitItem) {
        _uiState.update {
            it.copy(
                isAddHabitDialogOpen = true,
                habitEditingTarget = habit
            )
        }
    }

    /** Cierra el modal de hábitos */
    fun closeAddHabitDialog() {
        _uiState.update {
            it.copy(
                isAddHabitDialogOpen = false,
                habitEditingTarget = null
            )
        }
    }

    /** Registra el avance o completa un hábito/micro hábito */
    fun toggleHabit(habitToToggle: HabitItem) {
        _uiState.update { state ->
            val updatedHabits = state.habits.map { current ->
                if (current.id == habitToToggle.id) {
                    if (current.isProgressive) {
                        val newProg = if (current.currentProgress >= current.maxProgress) 0 else current.currentProgress + 1
                        current.copy(
                            currentProgress = newProg,
                            isCompleted = newProg >= current.maxProgress
                        )
                    } else {
                        current.copy(isCompleted = !current.isCompleted)
                    }
                } else {
                    current
                }
            }
            state.copy(habits = updatedHabits)
        }
    }

    /** Guarda un hábito (lo actualiza si ya existe o lo agrega al inicio si es nuevo) */
    fun saveHabit(habit: HabitItem) {
        _uiState.update { state ->
            val index = state.habits.indexOfFirst { it.id == habit.id }
            val updatedHabits = if (index != -1) {
                state.habits.toMutableList().apply { set(index, habit) }
            } else {
                listOf(habit) + state.habits
            }
            state.copy(
                habits = updatedHabits,
                isAddHabitDialogOpen = false,
                habitEditingTarget = null
            )
        }
    }

    /** Elimina un hábito de la lista */
    fun deleteHabit(habitToDelete: HabitItem) {
        _uiState.update { state ->
            val updatedHabits = state.habits.filterNot { it.id == habitToDelete.id }
            state.copy(
                habits = updatedHabits,
                isAddHabitDialogOpen = false,
                habitEditingTarget = null
            )
        }
    }

    // ------------------------------------------------------------------------
    // ACCIONES DEL FEED
    // ------------------------------------------------------------------------

    /** Alterna la reacción de "Me gusta" en una publicación */
    fun toggleLike(postId: String) {
        _uiState.update { state ->
            val updatedPosts = state.feedPosts.map { current ->
                if (current.id == postId) {
                    val newLiked = !current.isLiked
                    val newCount = if (newLiked) current.likesCount + 1 else (current.likesCount - 1).coerceAtLeast(0)
                    current.copy(
                        isLiked = newLiked,
                        likesCount = newCount
                    )
                } else {
                    current
                }
            }
            state.copy(feedPosts = updatedPosts)
        }
    }

    /** Crea y añade una nueva publicación a la comunidad */
    fun publishPost(content: String, category: String, imageUrl: String?) {
        val newPost = FeedPostItem(
            id = "post_${System.currentTimeMillis()}",
            authorName = "Alex Rivero (Tú)",
            authorAvatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=200",
            habitTitle = "Logro personal",
            habitCategory = category,
            content = content,
            postImageUrl = imageUrl,
            timeAgo = "Justo ahora",
            likesCount = 0,
            isLiked = false,
            streakCount = 1
        )
        _uiState.update { state ->
            state.copy(feedPosts = listOf(newPost) + state.feedPosts)
        }
    }
}
