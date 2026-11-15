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
 * - **¿Para qué sirve?** Reúne hábitos, historial, estadísticas, perfil, amigos del ranking, idioma y controles temporales de
 *   navegación y diálogos.
 * - **¿Cómo funciona?** Los datos persistentes proceden del repositorio; selectedTab y los diálogos
 *   controlan la sesión de interfaz.
 * - **¿Qué pasa si lo quitamos?** Las pantallas tendrían estados separados que podrían quedar
 *   desactualizados.
 */
data class MainUiState(
    val habits: List<HabitItem> = emptyList(),
    val feedPosts: List<FeedPostItem> = emptyList(),
    val friends: List<FriendEntity> = emptyList(),
    val selectedTab: Int = 0,
    val isDarkMode: Boolean = false,
    val languageCode: String = "es",
    val isAddHabitDialogOpen: Boolean = false,
    val habitEditingTarget: HabitItem? = null,
    val history: List<HabitHistoryItem> = emptyList(),
    val summary: TrackingSummary = TrackingSummary(),
    val user: UserEntity? = null,
    val isLoading: Boolean = true,
    val isSaving: Boolean = false,
    val error: String? = null,
    val publicationVersion: Int = 0,
    val petHappinessBonus: Int = 0,
    val smartCoachTip: String = ""
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
class MainViewModel(
    private val repository: HabitRepository,
    private val cloudSync: CloudSyncManager? = null,
    private val appContext: Context? = null
) : ViewModel() {
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

    // Recupera lo guardado, detecta cambios de día y escucha cambios de la nube mientras el ViewModel sigue activo.
    init {
        refresh()
        viewModelScope.launch {
            while (true) {
                delay(30_000)
                if (HabitDates.today() != day) refresh()
            }
        }
        cloudSync?.let { sync ->
            viewModelScope.launch {
                sync.syncedFeedPosts.collect { snapshot?.let(::render) }
            }
            viewModelScope.launch {
                sync.syncedFriends.collect { snapshot?.let(::render) }
            }
            viewModelScope.launch {
                sync.petHappinessBonus.collect { snapshot?.let(::render) }
            }
            viewModelScope.launch {
                sync.currentSession.collect { session ->
                    if (session != null) {
                        runCatching { sync.refreshFriendsFeed() }
                    }
                    snapshot?.let(::render)
                }
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
     * - **¿Para qué sirve?** Reconstruye tarjetas, historial de siete días, publicaciones, amigos y
     *   estadísticas al cargar o recibir cambios.
     * - **¿Cómo funciona?** Separa los hábitos activos para los contadores y conserva todos los
     *   registros para el historial; filtra publicaciones con máximo 7 días de antigüedad y actualiza
     *   el estado sin reemplazar la pestaña ni el diálogo abiertos.
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
        val cloudFriendsByTag = cloudSync?.syncedFriends?.value.orEmpty().associateBy { it.normalizedTag }
        val mergedFriends = data.friends.map { localFriend ->
            val remote = cloudFriendsByTag[localFriend.friendCode]
            if (remote != null) {
                localFriend.copy(
                    displayName = remote.displayName.ifBlank { localFriend.displayName },
                    avatarUrl = remote.avatarUrl?.takeIf { !AvatarCatalog.isBrokenUrl(it) } ?: localFriend.avatarUrl,
                    score = maxOf(localFriend.score, remote.score),
                    completedHabits = maxOf(localFriend.completedHabits, remote.completedHabits),
                    currentStreak = maxOf(localFriend.currentStreak, remote.currentStreak),
                    petType = remote.petType.ifBlank { localFriend.petType },
                    petAccessory = remote.petAccessory.ifBlank { localFriend.petAccessory }
                )
            } else {
                localFriend
            }
        }

        val activeStreakCount = maxOf(summary.overallStreak, summary.microStreak)
        val myTag = cloudSync?.currentSession?.value?.normalizedTag
        val now = System.currentTimeMillis()
        val maxAgeMs = 7L * 24L * 60L * 60L * 1000L

        val localPosts = data.posts
            .filter { now - it.createdAt <= maxAgeMs }
            .sortedByDescending { it.createdAt }
            .map { post ->
                val ageMs = (now - post.createdAt).coerceAtLeast(0L)
                val minutes = (ageMs / 60_000L).coerceAtLeast(0L)
                val daysRemaining = (((maxAgeMs - ageMs) / 86_400_000L).toInt() + 1).coerceIn(1, 7)
                val isOwnPost = post.authorId == null || post.authorId == data.user?.id
                val author = if (isOwnPost) (data.user ?: authors[post.authorId]) else (authors[post.authorId] ?: data.user)
                val comments = HabitConverters.decodeComments(post.commentsJson)
                    .distinctBy { "${it.authorName.trim().lowercase()}|${it.content.trim()}" }
                FeedPostItem(
                    id = post.id,
                    authorName = author?.displayName ?: "Mi perfil",
                    authorAvatarUrl = author?.avatarUrl.orEmpty(),
                    habitTitle = post.habitTitle,
                    habitCategory = post.category,
                    content = post.content,
                    timeAgo = formatRelativeTime(minutes),
                    postImageUrl = post.imageUrl,
                    likesCount = post.baseLikesCount + if (post.isLiked) 1 else 0,
                    isLiked = post.isLiked,
                    streakCount = post.authorStreak ?: activeStreakCount,
                    isOwnPost = isOwnPost,
                    createdAtMillis = post.createdAt,
                    daysRemaining = daysRemaining,
                    comments = comments
                )
            }

        val friendTagsVisible = (mergedFriends.map { it.friendCode } + cloudFriendsByTag.keys + listOfNotNull(myTag)).toSet()
        val existingPostIds = localPosts.map { it.id }.toSet()
        val cloudPosts = cloudSync?.syncedFeedPosts?.value.orEmpty()
            .filter { (now - it.createdAtMillis) <= maxAgeMs && it.authorTag in friendTagsVisible }
            .filterNot { cp ->
                cp.id in existingPostIds || localPosts.any { lp ->
                    lp.content.trim() == cp.caption.trim() && kotlin.math.abs(lp.createdAtMillis - cp.createdAtMillis) < 30_000L
                }
            }
            .map { cp ->
                val minutes = ((now - cp.createdAtMillis).coerceAtLeast(0L) / 60_000L).coerceAtLeast(0L)
                val daysRemaining = (((maxAgeMs - (now - cp.createdAtMillis).coerceAtLeast(0L)) / 86_400_000L).toInt() + 1).coerceIn(1, 7)
                val remoteFriend = cloudFriendsByTag[cp.authorTag]
                val resolvedAvatar = cp.authorAvatarUrl?.takeIf { !AvatarCatalog.isBrokenUrl(it) }
                    ?: remoteFriend?.avatarUrl?.takeIf { !AvatarCatalog.isBrokenUrl(it) }
                    ?: mergedFriends.firstOrNull { it.friendCode == cp.authorTag }?.avatarUrl
                    ?: ""
                val isOwn = myTag != null && cp.authorTag == myTag
                FeedPostItem(
                    id = cp.id,
                    authorName = cp.authorName,
                    authorAvatarUrl = resolvedAvatar,
                    habitTitle = cp.habitTitle,
                    habitCategory = "Comunidad",
                    content = cp.caption,
                    timeAgo = formatRelativeTime(minutes),
                    postImageUrl = cp.imageUri,
                    likesCount = cp.likesCount,
                    isLiked = myTag != null && myTag in cp.likedByTags,
                    streakCount = remoteFriend?.currentStreak ?: activeStreakCount,
                    isOwnPost = isOwn,
                    createdAtMillis = cp.createdAtMillis,
                    daysRemaining = daysRemaining,
                    comments = cp.comments.distinctBy { "${it.authorName.trim().lowercase()}|${it.text.trim()}" }.map { c ->
                        val cMin = ((now - c.createdAtMillis).coerceAtLeast(0L) / 60_000L).coerceAtLeast(0L)
                        FeedCommentItem(
                            id = c.id,
                            postId = cp.id,
                            authorName = c.authorName,
                            content = c.text,
                            timeAgo = formatRelativeTime(cMin)
                        )
                    }
                )
            }

        val posts = (localPosts + cloudPosts).sortedByDescending { it.createdAtMillis }
        val lang = data.user?.languageCode ?: _uiState.value.languageCode
        val isEn = lang.equals("en", ignoreCase = true)
        val pendingCount = habits.count { !it.isCompleted }
        val nextMicro = habits.firstOrNull { !it.isCompleted && it.isMicroHabit } ?: habits.firstOrNull { !it.isCompleted }
        val coachTip = when {
            habits.isEmpty() -> if (isEn) "Create your first habit to start building momentum." else "Crea tu primer hábito para comenzar tu racha."
            pendingCount == 0 -> if (isEn) "All pending habits done today! Play the Sanctuary mini-game to boost pet happiness." else "¡Todos tus pendientes completados hoy! Juega el minijuego tipo Pou para alegrar a tu mascota."
            nextMicro != null -> if (isEn) "Coach tip: Start with \"${nextMicro.title}\" (${pendingCount} left) to earn pet XP & extend your ${activeStreakCount}d streak." else "Coach diario: Empieza con \"${nextMicro.title}\" (quedan $pendingCount) para subir XP real y tu racha de ${activeStreakCount}d."
            else -> ""
        }

        _uiState.update {
            it.copy(
                habits = habits,
                history = history,
                summary = summary,
                user = data.user,
                friends = mergedFriends,
                isDarkMode = data.user?.isDarkMode ?: it.isDarkMode,
                languageCode = lang,
                feedPosts = posts,
                isLoading = data.user == null,
                petHappinessBonus = cloudSync?.petHappinessBonus?.value ?: it.petHappinessBonus,
                smartCoachTip = coachTip
            )
        }
    }

    private fun formatRelativeTime(minutes: Long): String = when {
        minutes < 1 -> "Justo ahora"
        minutes < 60 -> "Hace $minutes min"
        minutes < 1440 -> "Hace ${minutes / 60} h"
        else -> "Hace ${minutes / 1440} días"
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

    /** Cambia entre las cuatro pestañas principales (0: Tracker, 1: Feed, 2: Mascota/Ranking, 3: Profile). */
    fun selectTab(tabIndex: Int) {
        if (tabIndex in 0..3) _uiState.update { it.copy(selectedTab = tabIndex) }
    }

    // ============================================================================
    // PREFERENCIA DE TEMA E IDIOMA (ES / EN)
    // ============================================================================

    fun toggleDarkMode() = write {
        repository.setDarkMode(!_uiState.value.isDarkMode)
    }

    fun setDarkMode(enabled: Boolean) = write {
        repository.setDarkMode(enabled)
    }

    fun setLanguage(languageCode: String) = write {
        repository.setLanguage(languageCode)
    }

    fun toggleLanguage() = write {
        val next = if (_uiState.value.languageCode.equals("en", ignoreCase = true)) "es" else "en"
        repository.setLanguage(next)
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
    // PUBLICACIONES, COMENTARIOS, PERFIL, MASCOTA, SESIÓN Y AMIGOS POR #CÓDIGO
    // ============================================================================

    fun toggleLike(postId: String) = write {
        val isLocal = snapshot?.posts?.any { it.id == postId } == true
        if (isLocal) {
            repository.toggleLike(postId)
        }
        cloudSync?.toggleLikeCloudFeedPost(postId)
    }

    /** Sube la imagen al CDN si aplica, guarda la publicación y la replica en el Feed compartido de amigos. */
    fun publishPost(content: String, category: String, imageUrl: String?) = write {
        val cloudImage = if (appContext != null && !imageUrl.isNullOrBlank()) {
            ImageOptimizer.uploadImageOrEncodeForCloud(appContext, imageUrl, isAvatar = false) ?: imageUrl
        } else {
            imageUrl
        }
        val persistedUrl = if (ImageOptimizer.isRemoteCdnUrl(cloudImage)) cloudImage else imageUrl
        repository.publish(content, category, persistedUrl)
        cloudSync?.publishCloudFeedPost(
            postId = "cloud_${System.currentTimeMillis()}",
            habitTitle = category.ifBlank { "Hábito diario" },
            caption = content.trim(),
            imageUri = cloudImage
        )
        _uiState.update { it.copy(publicationVersion = it.publicationVersion + 1) }
    }

    /** Elimina una publicación propia junto con todos sus comentarios y likes. */
    fun deleteOwnPost(postId: String) = write {
        repository.deleteOwnPost(postId)
    }

    /** Añade un comentario único a una publicación del Feed (local o compartida de amigo). */
    fun addComment(postId: String, content: String) = write {
        val clean = content.trim()
        if (clean.isEmpty()) return@write
        val author = _uiState.value.user?.displayName ?: cloudSync?.currentSession?.value?.displayName ?: "Mi perfil"
        val isLocal = snapshot?.posts?.any { it.id == postId } == true
        if (isLocal) {
            repository.addComment(postId, author, clean)
        }
        cloudSync?.addCommentToCloudFeedPost(
            postId = postId,
            commentId = "c_${System.currentTimeMillis()}",
            text = clean
        )
    }

    /** Ejecuta la confirmación de la pantalla después de persistir el perfil. */
    fun updateProfile(name: String, email: String?, onSaved: () -> Unit = {}) = write {
        repository.updateProfile(name, email)
        onSaved()
    }

    /** Persiste nombre, correo y avatar optimizado en Room y sincroniza CDN URL hacia la nube para amigos. */
    fun updateProfileWithAvatar(
        name: String,
        email: String?,
        avatarUrl: String?,
        onSaved: () -> Unit = {}
    ) = write {
        val cloudAvatar = if (appContext != null && !avatarUrl.isNullOrBlank()) {
            ImageOptimizer.uploadImageOrEncodeForCloud(appContext, avatarUrl, isAvatar = true) ?: avatarUrl
        } else {
            avatarUrl
        }
        val persistedAvatar = if (ImageOptimizer.isRemoteCdnUrl(cloudAvatar)) cloudAvatar else avatarUrl
        repository.updateProfileWithAvatar(name, email, persistedAvatar)
        if (cloudSync != null) {
            cloudSync.updateActiveSessionAvatar(cloudAvatar)
            val s = _uiState.value
            val pet = s.user?.let { PetCustomization.fromUser(it) } ?: PetCustomization()
            cloudSync.publishCurrentUserStats(
                completedHabits = s.summary.totalCompleted,
                currentStreak = maxOf(s.summary.overallStreak, s.summary.microStreak),
                petCustomization = pet,
                userNameOverride = name
            )
        }
        onSaved()
    }

    /** Actualiza únicamente el avatar optimizado del usuario local y publica la URL en la nube. */
    fun updateAvatar(avatarUrl: String?) = write {
        val cloudAvatar = if (appContext != null && !avatarUrl.isNullOrBlank()) {
            ImageOptimizer.uploadImageOrEncodeForCloud(appContext, avatarUrl, isAvatar = true) ?: avatarUrl
        } else {
            avatarUrl
        }
        val persistedAvatar = if (ImageOptimizer.isRemoteCdnUrl(cloudAvatar)) cloudAvatar else avatarUrl
        repository.updateAvatar(persistedAvatar)
        if (cloudSync != null) {
            cloudSync.updateActiveSessionAvatar(cloudAvatar)
            val s = _uiState.value
            val pet = s.user?.let { PetCustomization.fromUser(it) } ?: PetCustomization()
            cloudSync.publishCurrentUserStats(
                completedHabits = s.summary.totalCompleted,
                currentStreak = maxOf(s.summary.overallStreak, s.summary.microStreak),
                petCustomization = pet,
                userNameOverride = s.user?.displayName ?: cloudSync.currentSession.value?.displayName
            )
        }
    }

    /** Boost exclusivo de felicidad desde el minijuego tipo Pou (NO incrementa XP). */
    fun boostPetHappinessFromMiniGame(delta: Int) {
        cloudSync?.boostPetHappinessFromMiniGame(delta)
    }

    /** Fuerza actualización inmediata de amigos y feed compartido desde Firebase/REST. */
    fun refreshSocialCloud() {
        viewModelScope.launch {
            runCatching {
                cloudSync?.refreshFriendsRealtime()
                cloudSync?.refreshFriendsFeed()
            }
        }
    }

    /** Persiste la personalización de mascota (especie, nombre, accesorio y hábitat) en Room. */
    fun updatePetCustomization(
        petType: String,
        petName: String,
        accessory: String,
        habitat: String
    ) = write {
        repository.updatePetCustomization(
            petType = petType,
            petName = petName,
            accessory = accessory,
            habitat = habitat
        )
    }

    /** Inicia o registra sesión persistente con nombre, correo, código único de 3 dígitos (#XXX) y avatar opcional. */
    fun loginOrRegisterUser(
        name: String,
        email: String?,
        friendCode: String,
        avatarUrl: String? = null,
        onSaved: () -> Unit = {}
    ) = write {
        repository.loginOrRegisterUser(
            name = name,
            email = email,
            friendCode = friendCode
        )
        if (!avatarUrl.isNullOrBlank()) {
            repository.updateAvatar(avatarUrl)
        }
        onSaved()
    }

    /** Cierra la sesión activa en la entidad de usuario local. */
    fun logoutUser() = write {
        repository.logoutUser()
    }

    /** Vincula un amigo por su código de 3 dígitos (#XXX) en Room. */
    fun addFriendByCode(
        code: String,
        name: String? = null,
        score: Int? = null,
        habits: Int? = null,
        streak: Int? = null
    ) = write {
        repository.addFriendByCode(
            code = code,
            name = name,
            score = score,
            habits = habits,
            streak = streak
        )
    }

    // ============================================================================
    // CREACIÓN DEL VIEWMODEL CON EL REPOSITORIO LOCAL
    // ============================================================================

    companion object {
        /** Proporciona el repositorio de Room junto con CloudSyncManager compartido. */
        fun factory(context: Context): ViewModelProvider.Factory = object : ViewModelProvider.Factory {
            @Suppress("UNCHECKED_CAST")
            override fun <T : ViewModel> create(modelClass: Class<T>): T {
                require(modelClass.isAssignableFrom(MainViewModel::class.java))
                val appCtx = context.applicationContext
                return MainViewModel(
                    repository = RoomHabitRepository(HabitDatabase.get(appCtx), originalData = OriginalHabitData),
                    cloudSync = CloudSyncManager.getInstance(appCtx),
                    appContext = appCtx
                ) as T
            }
        }
    }
}
