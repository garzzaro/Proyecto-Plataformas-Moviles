package com.example.viewmodel

import android.content.Context
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.model.*
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.Job
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

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
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val publicationVersion: Int = 0
)

// ============================================================================
// CONTROLADOR DE ESTADO: MainViewModel
// ============================================================================

/**
 * MainViewModel coordina la persistencia y el estado reactivo de la aplicación.
 *
 * - **¿Para qué sirve?** Conecta las acciones de las pantallas con el repositorio y conserva la
 *   navegación durante la sesión.
 * - **¿Cómo funciona?** Observa las lecturas de Room, reconstruye MainUiState y serializa las
 *   escrituras para evitar cambios simultáneos.
 * - **¿Qué pasa si lo quitamos?** La interfaz perdería su coordinación y no recibiría las
 *   actualizaciones de los datos guardados.
 */
class MainViewModel(private val repository: HabitRepository) : ViewModel() {
    // ============================================================================
    // ESTADO OBSERVABLE Y CONTROL DE OPERACIONES
    // ============================================================================

    private val _uiState = MutableStateFlow(MainUiState())
    val uiState: StateFlow<MainUiState> = _uiState.asStateFlow()
    private val writes = Mutex()
    private var snapshot: HabitSnapshot? = null
    private var day = HabitDates.today()
    private var observation: Job? = null

    // ============================================================================
    // CARGA INICIAL Y CAMBIO DE FECHA
    // ============================================================================

    // Recupera lo guardado y detecta cambios de día mientras el ViewModel sigue activo.
    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                if (HabitDates.today() != day) refresh()
            }
        }
    }

    /**
     * observe mantiene la interfaz conectada con los datos guardados.
     *
     * - **¿Para qué sirve?** Actualiza hábitos, historial y contadores cuando Room informa de una
     *   modificación.
     * - **¿Cómo funciona?** Mantiene una sola colección activa del repositorio y convierte los errores
     *   de carga en mensajes visibles.
     * - **¿Qué pasa si lo quitamos?** Archivar o completar un hábito podría dejar cifras antiguas en
     *   pantalla.
     */
    private fun observe() {
        if (observation?.isActive == true) return
        observation = viewModelScope.launch {
            repository.snapshots.catch { error ->
                _uiState.update { it.copy(error = error.message ?: "No se pudieron cargar tus datos") }
            }.collect { value -> snapshot = value; render(value) }
        }
    }

    /**
     * render transforma una lectura persistente en datos para las pantallas.
     *
     * - **¿Para qué sirve?** Reconstruye tarjetas, historial de siete días, publicaciones y
     *   estadísticas al cargar o recibir cambios.
     * - **¿Cómo funciona?** Separa los hábitos activos para los contadores y conserva todos los
     *   registros para el historial; actualiza el estado sin reemplazar la pestaña ni el diálogo
     *   abiertos.
     * - **¿Qué pasa si lo quitamos?** La pantalla no podría recuperar el progreso ni reaccionar a la
     *   eliminación de un hábito.
     */
    private fun render(data: HabitSnapshot) {
        val today = day
        // Los archivados conservan sus registros, pero dejan de pertenecer a la rutina activa.
        val active = data.habits.filter { it.archivedDay == null }.sortedWith(compareByDescending<HabitEntity> { it.createdDay }.thenBy { it.id })
        val recordsByHabit = data.records.groupBy { it.habitId }
        val habits = active.map { habit ->
            val rows = recordsByHabit[habit.id].orEmpty()
            val current = rows.firstOrNull { today in it.periodStart..it.periodEnd }
            HabitItem(habit.id, habit.title, habit.subtitle, habit.category, current?.frequency ?: habit.frequency,
                HabitTracking.habitStreak(rows, today), habit.isProgressive, current?.progress ?: 0,
                current?.target ?: habit.target, habit.progressUnit, current?.completed ?: false,
                habit.isMicroHabit, habit.tags, habit.timeOrDuration, habit.categoryColorHex,
                habit.reminderTime, habit.reminderEnabled, habit.frequency.takeIf { current != null && it != current.frequency })
        }
        // El historial conserva también los registros de hábitos retirados de la rutina.
        val history = data.records.mapNotNull { record ->
            val date = record.completedDay ?: minOf(record.periodEnd, today)
            if (date !in today - 6..today) return@mapNotNull null
            val period = if (record.periodEnd != record.periodStart) " · Semana ${HabitDates.label(record.periodStart, "d MMM")}–${HabitDates.label(record.periodEnd, "d MMM")}" else ""
            date to HabitHistoryItem("${record.habitId}:${record.periodStart}", record.title,
                HabitDates.label(date) + period, record.completed,
                if (record.isProgressive) "${record.progress}/${record.target}" else "",
                when { record.completed -> "Completado"; record.periodEnd < today -> "Sin completar"; record.progress > 0 -> "En progreso"; else -> "Pendiente" },
                record.isProgressive, record.timeOrDuration)
        }.sortedByDescending { it.first }.map { it.second }
        // El mismo conjunto de identificadores activos alimenta todos los contadores semanales.
        val summary = HabitTracking.summary(data.records, active.map { it.id }.toSet(), today)
        val authors = data.authors.associateBy { it.id }
        val posts = data.posts.sortedByDescending { it.createdAt }.map { post ->
            val minutes = ((System.currentTimeMillis() - post.createdAt) / 60_000).coerceAtLeast(0)
            val author = authors[post.authorId] ?: data.user
            FeedPostItem(post.id, author?.displayName ?: "Mi perfil", author?.avatarUrl.orEmpty(),
                post.habitTitle, post.category, post.content,
                when { minutes < 1 -> "Justo ahora"; minutes < 60 -> "Hace $minutes min"; minutes < 1440 -> "Hace ${minutes / 60} h"; else -> "Hace ${minutes / 1440} días" },
                post.imageUrl, post.baseLikesCount + if (post.isLiked) 1 else 0, post.isLiked,
                post.authorStreak ?: summary.microStreak)
        }
        _uiState.update { it.copy(habits = habits, history = history, summary = summary, user = data.user,
            isDarkMode = data.user?.isDarkMode ?: it.isDarkMode, feedPosts = posts, isLoading = data.user == null) }
    }

    /**
     * write ejecuta una operación de guardado con control de estado y errores.
     *
     * - **¿Para qué sirve?** Evita escrituras simultáneas y permite mostrar cuándo una operación está
     *   en curso.
     * - **¿Cómo funciona?** Usa viewModelScope y Mutex; conserva los datos del formulario ante un
     *   error y respeta la cancelación del ciclo de vida.
     * - **¿Qué pasa si lo quitamos?** Las operaciones podrían competir entre sí o fallar sin informar
     *   al usuario.
     */
    private fun write(action: suspend () -> Unit) {
        viewModelScope.launch {
            writes.withLock {
                _uiState.update { it.copy(isSaving = true, error = null) }
                try { action() }
                catch (cancelled: CancellationException) { throw cancelled }
                catch (error: Exception) { _uiState.update { it.copy(error = error.message ?: "No se pudo guardar. Inténtalo de nuevo.") } }
                finally { _uiState.update { it.copy(isSaving = false) } }
            }
        }
    }

    /**
     * refresh sincroniza la fecha y recupera los datos del repositorio.
     *
     * - **¿Para qué sirve?** Carga la información guardada al iniciar o al volver a la aplicación.
     * - **¿Cómo funciona?** Prepara los períodos faltantes sin borrar los existentes y activa la
     *   observación de datos.
     * - **¿Qué pasa si lo quitamos?** El progreso podría mostrarse con una fecha antigua o sin los
     *   cambios persistidos.
     */
    fun refresh() = write {
        val today = HabitDates.today()
        repository.refresh(today)
        day = today
        observe()
        snapshot?.let(::render)
    }
    // ============================================================================
    // NAVEGACIÓN Y MENSAJES DE LA INTERFAZ
    // ============================================================================

    /** Descarta el mensaje visible sin alterar los datos guardados. */
    fun clearError() {
        _uiState.update { it.copy(error = null) }
    }

    /** Cambia únicamente entre las tres pestañas existentes. */
    fun selectTab(tabIndex: Int) {
        if (tabIndex in 0..2) _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    // ============================================================================
    // PREFERENCIA DE TEMA
    // ============================================================================

    fun toggleDarkMode() = write {
        repository.setDarkMode(!_uiState.value.isDarkMode)
    }

    fun setDarkMode(enabled: Boolean) = write {
        repository.setDarkMode(enabled)
    }

    // ============================================================================
    // FORMULARIO Y SEGUIMIENTO DE HÁBITOS
    // ============================================================================

    /** Abre el formulario vacío para crear un hábito. */
    fun openAddHabitDialog() {
        _uiState.update { it.copy(isAddHabitDialogOpen = true, habitEditingTarget = null) }
    }

    /** Abre el mismo formulario con los valores del hábito seleccionado. */
    fun openEditHabitDialog(habit: HabitItem) {
        _uiState.update { it.copy(isAddHabitDialogOpen = true, habitEditingTarget = habit) }
    }

    fun closeAddHabitDialog() {
        _uiState.update { it.copy(isAddHabitDialogOpen = false, habitEditingTarget = null) }
    }

    /** Alterna el cumplimiento utilizando el registro del período guardado en Room. */
    fun toggleHabit(habit: HabitItem) = write {
        repository.setProgress(habit.id, null, HabitDates.today())
    }

    /** Guarda una cantidad acumulada o permite deshacer el avance. */
    fun setHabitProgress(habit: HabitItem, progress: Int) = write {
        repository.setProgress(habit.id, progress, HabitDates.today())
    }

    /** Cierra el formulario únicamente después de confirmar el guardado. */
    fun saveHabit(habit: HabitItem) = write {
        repository.save(habit, HabitDates.today())
        closeAddHabitDialog()
    }

    /** Archiva el hábito; Room vuelve a emitir y render actualiza el contador semanal. */
    fun deleteHabit(habit: HabitItem) = write {
        repository.archive(habit.id, HabitDates.today())
        closeAddHabitDialog()
    }

    // ============================================================================
    // PUBLICACIONES Y PERFIL LOCAL
    // ============================================================================

    fun toggleLike(postId: String) = write {
        repository.toggleLike(postId)
    }

    /** Notifica el éxito para limpiar el borrador solo cuando la publicación ya está guardada. */
    fun publishPost(content: String, category: String, imageUrl: String?) = write {
        repository.publish(content, category, imageUrl)
        _uiState.update { it.copy(publicationVersion = it.publicationVersion + 1) }
    }

    /** Ejecuta la confirmación de la pantalla después de persistir el perfil. */
    fun updateProfile(name: String, email: String?, onSaved: () -> Unit = {}) = write {
        repository.updateProfile(name, email)
        onSaved()
    }

    // ============================================================================
    // CREACIÓN DEL VIEWMODEL CON EL REPOSITORIO LOCAL
    // ============================================================================

    companion object {
        /** Proporciona el repositorio de Room usando el contexto de aplicación. */
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(MainViewModel::class.java))
                return MainViewModel(RoomHabitRepository(HabitDatabase.get(context), originalData = OriginalHabitData)) as T
            }
        }
    }
}
