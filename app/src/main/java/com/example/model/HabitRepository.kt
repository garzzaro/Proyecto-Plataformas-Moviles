package com.example.model

import androidx.room.withTransaction
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import java.util.UUID

// ============================================================================
// LECTURA CONSOLIDADA: HabitSnapshot
// ============================================================================

/**
 * HabitSnapshot representa una lectura completa de los datos del usuario.
 *
 * - **¿Para qué sirve?** Transporta datos persistidos al ViewModel sin introducir lógica de pantalla
 *   en la base.
 * - **¿Cómo funciona?** Reúne perfil, definiciones, registros, publicaciones y amigos del ranking;
 *   también incluye los hábitos archivados para consultar el historial.
 * - **¿Qué pasa si lo quitamos?** La pantalla tendría que coordinar por separado los datos que forman
 *   sus estadísticas.
 */
data class HabitSnapshot(
    val user: UserEntity?,
    val habits: List<HabitEntity>,
    val records: List<HabitRecordEntity>,
    val posts: List<FeedPostEntity>,
    val authors: List<UserEntity> = emptyList(),
    val friends: List<FriendEntity> = emptyList()
)

// ============================================================================
// CONTRATO DE PERSISTENCIA: HabitRepository
// ============================================================================

/**
 * HabitRepository define el contrato de persistencia que utiliza la aplicación.
 *
 * - **¿Para qué sirve?** Desacopla el ViewModel de Room y prepara la integración de otra fuente de
 *   datos.
 * - **¿Cómo funciona?** Expone lecturas observables y acciones suspend para hábitos, perfil,
 *   mascota, sesión, amigos por código y publicaciones.
 * - **¿Qué pasa si lo quitamos?** Las pantallas dependerían directamente de las consultas y de una
 *   implementación concreta de almacenamiento.
 */
interface HabitRepository {
    val snapshots: Flow<HabitSnapshot>
    suspend fun refresh(today: Long)
    suspend fun save(habit: HabitItem, today: Long)
    suspend fun archive(id: String, today: Long)
    suspend fun setProgress(id: String, value: Int?, today: Long)
    suspend fun setDarkMode(enabled: Boolean)
    suspend fun updateProfile(name: String, email: String?)
    suspend fun publish(content: String, category: String, imageUrl: String?)
    suspend fun toggleLike(id: String)

    // Nuevas operaciones Room v3: mascota, sesión persistente y amigos por código #XXX
    suspend fun updatePetCustomization(
        petType: String,
        petName: String = "Octo",
        accessory: String = "none",
        habitat: String = "aquarium"
    ) = Unit

    suspend fun loginOrRegisterUser(
        name: String,
        email: String?,
        friendCode: String? = null
    ) = Unit

    suspend fun logoutUser() = Unit

    suspend fun addFriendByCode(
        code: String,
        name: String? = null,
        score: Int? = null,
        habits: Int? = null,
        streak: Int? = null
    ) = Unit

    // Nuevas operaciones Room v4: avatar optimizado, idioma, borrado de post propio y comentarios
    suspend fun updateProfileWithAvatar(name: String, email: String?, avatarUrl: String?) = updateProfile(name, email)

    suspend fun updateAvatar(avatarUrl: String?) = Unit

    suspend fun setLanguage(languageCode: String) = Unit

    suspend fun deleteOwnPost(postId: String) = Unit

    suspend fun addComment(postId: String, authorName: String, content: String) = Unit
}

// ============================================================================
// IMPLEMENTACIÓN LOCAL: RoomHabitRepository
// ============================================================================

/**
 * RoomHabitRepository implementa las operaciones locales de seguimiento.
 *
 * - **¿Para qué sirve?** Guarda los cambios y devuelve los datos del propietario sin depender de
 *   listas simuladas.
 * - **¿Cómo funciona?** Utiliza HabitDao y transacciones; el flujo de Room vuelve a emitir al guardar
 *   un hábito, personalizar la mascota, iniciar sesión o añadir un amigo por código.
 * - **¿Qué pasa si lo quitamos?** Las acciones del usuario no llegarían a la base ni actualizarían la
 *   pantalla.
 */
class RoomHabitRepository(
    private val db: HabitDatabase,
    private val userId: String = "local-user",
    private val originalData: OriginalHabitData? = null
) : HabitRepository {
    private val dao = db.habits()

    companion object {
        const val INITIAL_FRIENDS_IMPORT_NAME = "initial-friends-v1"
    }

    override val snapshots: Flow<HabitSnapshot> = dao.observeUser(userId).map { user ->
        HabitSnapshot(
            user = user?.user,
            habits = user?.habits.orEmpty().map { it.habit },
            records = user?.habits.orEmpty().flatMap { it.records },
            posts = user?.posts.orEmpty().map { it.post },
            authors = user?.posts.orEmpty().mapNotNull { it.author }.distinctBy { it.id },
            friends = user?.friends.orEmpty().sortedWith(compareByDescending<FriendEntity> { it.score }.thenByDescending { it.updatedAt })
        )
    }

    /**
     * materialize prepara los períodos que todavía no existen.
     *
     * - **¿Para qué sirve?** Completa los días o semanas transcurridos mientras la aplicación estuvo
     *   cerrada.
     * - **¿Cómo funciona?** Genera solo registros faltantes de hábitos activos y los inserta con
     *   IGNORE, conservando los avances previos.
     * - **¿Qué pasa si lo quitamos?** Faltarían días en el historial y no se podría detectar
     *   correctamente una interrupción de racha.
     */
    private suspend fun materialize(today: Long) {
        dao.activeHabits(userId).forEach { habit ->
            dao.insertRecords(HabitTracking.missingRecords(habit, dao.records(habit.id), today))
        }
    }

    /**
     * Garantiza que el usuario tenga un código único de 3 dígitos distinto de `"000"`.
     */
    private suspend fun ensureUserFriendCode() {
        val existingUser = dao.user(userId) ?: return
        if (existingUser.friendCode == "000" || existingUser.friendCode.isBlank()) {
            val generated = FriendDirectory.generateCodeForUser(userId, existingUser.displayName)
            dao.updateFriendCode(userId, generated)
        }
    }

    /**
     * refresh recupera el estado inicial sin reiniciar datos existentes.
     *
     * - **¿Para qué sirve?** Prepara el perfil local, el código `#XXX`, los amigos iniciales y las
     *   ocurrencias pendientes al abrir la app o cambiar de fecha.
     * - **¿Cómo funciona?** Inserta el perfil solo si falta, genera automáticamente `friendCode` si es `"000"`
     *   y crea períodos dentro de una transacción; no reemplaza cumplimientos.
     * - **¿Qué pasa si lo quitamos?** La pantalla podría cargar períodos incompletos o no tener un
     *   perfil al iniciar.
     */
    override suspend fun refresh(today: Long) = db.withTransaction {
        val defaultCode = FriendDirectory.generateCodeForUser(userId)
        dao.insertUser(UserEntity(id = userId, createdDay = today, friendCode = defaultCode))
        ensureUserFriendCode()

        originalData?.let { data ->
            // La marca se guarda al final de la misma transacción: no se duplica ni queda a medias.
            if (!dao.wasImported(userId, OriginalDataImport.NAME)) {
                val existing = dao.allHabits(userId)
                val records = existing.flatMap { dao.records(it.id) }
                val plan = OriginalDataImport.plan(data, userId, today, existing, records)
                plan.users.forEach { dao.insertUser(it) }
                plan.habits.forEach { dao.saveHabit(it) }
                dao.insertRecords(plan.records)
                dao.insertPosts(plan.posts)
                val profile = dao.user(userId)
                if (profile?.displayName == "Mi perfil" && profile.email == null && profile.avatarUrl == null) {
                    dao.setInitialProfile(userId, data.profileName, data.profileAvatar)
                }
                dao.markImported(DataImportEntity(userId, OriginalDataImport.NAME, today))
            }

            if (!dao.wasImported(userId, INITIAL_FRIENDS_IMPORT_NAME)) {
                val now = System.currentTimeMillis()
                val seededFriends = FriendDirectory.DEFAULT_FRIENDS.mapIndexed { idx, template ->
                    FriendEntity(
                        friendCode = template.friendCode,
                        userId = userId,
                        displayName = template.displayName,
                        avatarUrl = template.avatarUrl,
                        score = template.score,
                        completedHabits = template.completedHabits,
                        currentStreak = template.currentStreak,
                        petType = template.petType,
                        petAccessory = template.petAccessory,
                        updatedAt = now - idx * 60_000L
                    )
                }
                dao.insertFriends(seededFriends)
                dao.markImported(DataImportEntity(userId, INITIAL_FRIENDS_IMPORT_NAME, today))
            }
        }
        // Elimina automáticamente las publicaciones del Feed con más de 7 días de antigüedad
        dao.deletePostsOlderThan(System.currentTimeMillis() - 7L * 24L * 60L * 60L * 1000L)
        materialize(today)
    }

    /**
     * save crea o actualiza la definición de un hábito.
     *
     * - **¿Para qué sirve?** Persiste las ediciones respetando al propietario y los períodos
     *   históricos.
     * - **¿Cómo funciona?** Valida la meta, conserva las fechas originales y actualiza únicamente la
     *   ocurrencia abierta; la siguiente frecuencia entra en vigor al terminar ese período.
     * - **¿Qué pasa si lo quitamos?** Las ediciones se perderían o podrían alterar metas y títulos del
     *   historial.
     */
    override suspend fun save(habit: HabitItem, today: Long): Unit = db.withTransaction {
        require(habit.title.isNotBlank()) { "Escribe un nombre para el hábito" }
        require(!habit.isProgressive || habit.maxProgress > 0) { "La meta debe ser mayor que cero" }
        materialize(today)
        require(dao.habitOwner(habit.id)?.userId.let { it == null || it == userId }) { "Este hábito pertenece a otro perfil" }
        val old = dao.habit(habit.id, userId)
        require(old?.archivedDay == null) { "Este hábito ya fue eliminado" }
        val entity = HabitEntity(
            habit.id, userId, habit.title.trim(), habit.subtitle, habit.category, habit.frequency,
            habit.isProgressive, habit.isMicroHabit, if (habit.isProgressive) habit.maxProgress else 1,
            habit.progressUnit, habit.tags, habit.timeOrDuration, habit.categoryColorHex,
            old?.createdDay ?: today, old?.effectiveDay ?: today, habit.reminderTime, habit.reminderEnabled
        )
        dao.saveHabit(entity)
        materialize(today)
        // Solo se edita la ocurrencia abierta; el historial conserva su meta y título originales.
        dao.records(habit.id).firstOrNull { today in it.periodStart..it.periodEnd }?.let { record ->
            val progress = record.progress.coerceIn(0, entity.target)
            dao.saveRecord(
                record.copy(
                    title = entity.title,
                    isMicroHabit = entity.isMicroHabit,
                    isProgressive = entity.isProgressive,
                    timeOrDuration = entity.timeOrDuration,
                    target = entity.target,
                    progress = progress,
                    completedDay = if (progress >= entity.target) record.completedDay ?: today else null
                )
            )
        }
    }

    /**
     * archive retira un hábito de la rutina activa.
     *
     * - **¿Para qué sirve?** Permite eliminarlo de la pantalla conservando sus logros y registros
     *   históricos.
     * - **¿Cómo funciona?** Actualiza archivedDay en una transacción; el flujo reactivo recalcula los
     *   contadores usando los identificadores que siguen activos.
     * - **¿Qué pasa si lo quitamos?** Se perdería el historial al borrar físicamente el hábito o
     *   seguiría apareciendo en la rutina.
     */
    override suspend fun archive(id: String, today: Long): Unit = db.withTransaction {
        materialize(today)
        dao.habit(id, userId)?.let { dao.saveHabit(it.copy(archivedDay = today)) }
    }

    /**
     * setProgress guarda o deshace el avance del período actual.
     *
     * - **¿Para qué sirve?** Mantiene el cumplimiento al cerrar y volver a abrir la aplicación.
     * - **¿Cómo funciona?** Lee el registro guardado, valida el rango y actualiza avance y fecha de
     *   cumplimiento en la misma transacción.
     * - **¿Qué pasa si lo quitamos?** Los controles solo cambiarían la interfaz y se perdería el
     *   progreso al salir.
     */
    override suspend fun setProgress(id: String, value: Int?, today: Long) = db.withTransaction {
        materialize(today)
        val habit = dao.habit(id, userId) ?: error("No se encontró el hábito")
        check(habit.archivedDay == null) { "Este hábito ya fue eliminado" }
        val record = dao.records(id).first { today in it.periodStart..it.periodEnd }
        val progress = value ?: if (record.completed) 0 else if (habit.isProgressive) minOf(record.progress + 1, record.target) else record.target
        require(progress in 0..record.target) { "El avance debe estar entre 0 y ${record.target}" }
        dao.saveRecord(record.copy(progress = progress, completedDay = if (progress >= record.target) record.completedDay ?: today else null))
    }

    // ============================================================================
    // PREFERENCIAS, PERFIL, MASCOTA, SESIÓN Y PUBLICACIONES
    // ============================================================================

    /** Guarda el tema del perfil para recuperarlo al iniciar. */
    override suspend fun setDarkMode(enabled: Boolean) = dao.setDarkMode(userId, enabled)

    /** Actualiza nombre y correo del propietario sin cambiar su identificador. */
    override suspend fun updateProfile(name: String, email: String?) {
        require(name.isNotBlank()) { "Escribe tu nombre" }
        dao.updateProfile(userId, name.trim(), email?.trim()?.takeIf { it.isNotEmpty() })
        ensureUserFriendCode()
    }

    /** Crea una publicación local identificada por UUID; no simula una comunidad remota. */
    override suspend fun publish(content: String, category: String, imageUrl: String?) {
        require(content.isNotBlank() || !imageUrl.isNullOrBlank()) { "Escribe algo o selecciona una imagen antes de publicar" }
        dao.savePost(FeedPostEntity(UUID.randomUUID().toString(), userId, content.trim(), category, imageUrl, System.currentTimeMillis()))
    }

    /** Alterna la reacción guardada únicamente en una publicación del propietario. */
    override suspend fun toggleLike(id: String) = dao.toggleLike(id, userId)

    /**
     * Guarda/actualiza la personalización de mascota del usuario en Room.
     */
    override suspend fun updatePetCustomization(
        petType: String,
        petName: String,
        accessory: String,
        habitat: String
    ) = db.withTransaction {
        val resolvedType = PetType.fromId(petType).id
        val resolvedAccessory = PetAccessory.fromId(accessory).id
        val resolvedHabitat = PetHabitat.fromId(habitat).id
        val resolvedName = petName.trim().ifEmpty { PetType.fromId(resolvedType).displayName }
        dao.updatePetCustomization(
            userId = userId,
            petType = resolvedType,
            petName = resolvedName,
            accessory = resolvedAccessory,
            habitat = resolvedHabitat
        )
    }

    /**
     * Inicia o registra la sesión persistente del usuario, garantizando su código único de 3 dígitos (`#XXX`).
     */
    override suspend fun loginOrRegisterUser(
        name: String,
        email: String?,
        friendCode: String?
    ) = db.withTransaction {
        val cleanName = name.trim()
        require(cleanName.isNotBlank()) { "Escribe tu nombre para iniciar sesión" }
        val cleanEmail = email?.trim()?.takeIf { it.isNotEmpty() }
        val existingUser = dao.user(userId)
        val requestedCode = friendCode?.trim()?.removePrefix("#")
        val resolvedCode = when {
            requestedCode != null && Regex("^\\d{3}$").matches(requestedCode) && requestedCode != "000" -> requestedCode
            existingUser != null && Regex("^\\d{3}$").matches(existingUser.friendCode) && existingUser.friendCode != "000" -> existingUser.friendCode
            else -> FriendDirectory.generateCodeForUser(userId, cleanName)
        }
        dao.loginOrRegisterUser(
            userId = userId,
            name = cleanName,
            email = cleanEmail,
            friendCode = resolvedCode
        )
    }

    /**
     * Cierra la sesión activa conservando los hábitos y registros locales.
     */
    override suspend fun logoutUser() {
        dao.logoutUser(userId)
    }

    /**
     * Agrega o actualiza un amigo en el ranking usando su código único de 3 dígitos (`#XXX`).
     */
    override suspend fun addFriendByCode(
        code: String,
        name: String?,
        score: Int?,
        habits: Int?,
        streak: Int?
    ) = db.withTransaction {
        val cleanCode = code.trim().removePrefix("#")
        require(Regex("^\\d{3}$").matches(cleanCode) && cleanCode != "000") {
            "Ingresa un código de amigo válido de 3 dígitos (ej. 729)"
        }
        val currentUser = dao.user(userId)
        require(currentUser?.friendCode != cleanCode) {
            "No puedes agregarte a ti mismo (#$cleanCode)"
        }
        val friendEntity = FriendDirectory.createFriendEntity(
            userId = userId,
            rawCode = cleanCode,
            customName = name,
            customScore = score,
            customHabits = habits,
            customStreak = streak
        )
        dao.upsertFriend(friendEntity)
    }

    /**
     * Actualiza nombre, correo y avatar optimizado del perfil local.
     */
    override suspend fun updateProfileWithAvatar(name: String, email: String?, avatarUrl: String?) = db.withTransaction {
        require(name.isNotBlank()) { "Escribe tu nombre" }
        val cleanName = name.trim()
        val cleanEmail = email?.trim()?.takeIf { it.isNotEmpty() }
        val currentUser = dao.user(userId)
        val resolvedAvatar = avatarUrl?.trim()?.takeIf { it.isNotEmpty() } ?: currentUser?.avatarUrl
        dao.updateProfileWithAvatar(userId, cleanName, cleanEmail, resolvedAvatar)
        ensureUserFriendCode()
    }

    /**
     * Actualiza únicamente la foto de perfil optimizada del usuario.
     */
    override suspend fun updateAvatar(avatarUrl: String?) {
        dao.updateAvatar(userId, avatarUrl?.trim()?.takeIf { it.isNotEmpty() })
    }

    /**
     * Guarda el idioma preferido de la interfaz (`"es"` o `"en"`).
     */
    override suspend fun setLanguage(languageCode: String) {
        val normalized = if (languageCode.lowercase().startsWith("en")) "en" else "es"
        dao.updateLanguage(userId, normalized)
    }

    /**
     * Elimina una publicación propia del usuario junto con todos sus comentarios y likes.
     * Lanza [IllegalArgumentException] si el post pertenece a otro autor.
     */
    override suspend fun deleteOwnPost(postId: String): Unit = db.withTransaction {
        val post = dao.postById(postId) ?: return@withTransaction
        val isOwn = post.userId == userId && (post.authorId == null || post.authorId == userId)
        if (!isOwn) {
            throw IllegalArgumentException("Solo puedes borrar tus propias publicaciones")
        }
        dao.deleteOwnPost(postId, userId)
    }

    /**
     * Añade un comentario a una publicación del Feed y lo persiste en `commentsJson` dentro de la misma fila,
     * garantizando que al expirar (7 días) o borrarse el post se eliminen también sus comentarios.
     */
    override suspend fun addComment(postId: String, authorName: String, content: String): Unit = db.withTransaction {
        val cleanContent = content.trim()
        require(cleanContent.isNotBlank()) { "Escribe un comentario antes de enviar" }
        val post = dao.postById(postId) ?: error("No se encontró la publicación")
        val existingComments = HabitConverters.decodeComments(post.commentsJson)
        val newComment = FeedCommentItem(
            id = UUID.randomUUID().toString(),
            postId = postId,
            authorName = authorName.trim().ifEmpty { "Mi perfil" },
            content = cleanContent,
            timeAgo = "Ahora",
            createdAt = System.currentTimeMillis()
        )
        val updatedJson = HabitConverters.encodeComments(existingComments + newComment)
        dao.updatePostComments(postId, updatedJson)
    }
}
