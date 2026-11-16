package com.example.model

import android.content.ClipData
import android.content.ClipboardManager
import android.content.Context
import android.content.Intent
import android.content.SharedPreferences
import android.widget.Toast
import com.google.firebase.FirebaseApp
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.firstOrNull
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONArray
import org.json.JSONObject
import java.security.MessageDigest
import java.util.concurrent.TimeUnit
import kotlin.math.abs

/**
 * Estado del motor de sincronización híbrido (Dual Cloud + Realtime REST + Directorio Local).
 */
enum class CloudSyncState(val label: String) {
    IDLE("Listo para sincronizar"),
    SYNCING("Sincronizando en tiempo real..."),
    SYNCED_FIREBASE("Conectado a Firebase Cloud + REST"),
    SYNCED_REST("Sincronizado vía Cloud REST en vivo"),
    LOCAL_ACTIVE("Modo Directo P2P + Directorio #100-#999 activo"),
    ERROR("Modo Offline Resiliente activo")
}

/**
 * Representa la sesión autenticada y persistida en el dispositivo.
 */
data class AuthSession(
    val userId: String,
    val displayName: String,
    val email: String,
    val tagCode: String, // Código de 3 dígitos entre "100" y "999" asignado solo por el sistema
    val avatarUrl: String? = null,
    val isLoggedIn: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val syncMode: String = "Dual Cloud + Realtime REST",
    val passwordHash: String = ""
) {
    val normalizedTag: String
        get() = CloudSyncManager.normalizeTagCode(tagCode)

    val formattedTag: String
        get() = "#$normalizedTag"

    val fullHandle: String
        get() = "$displayName $formattedTag"
}

/**
 * Comentario sincronizado en la nube para el Feed compartido entre amigos.
 */
data class SyncedFeedComment(
    val id: String,
    val authorTag: String,
    val authorName: String,
    val text: String,
    val createdAtMillis: Long
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("authorTag", authorTag)
        put("authorName", authorName)
        put("text", text)
        put("createdAtMillis", createdAtMillis)
    }

    companion object {
        fun fromJson(json: JSONObject): SyncedFeedComment = SyncedFeedComment(
            id = json.optString("id", "c_${json.hashCode()}"),
            authorTag = json.optString("authorTag", "000"),
            authorName = json.optString("authorName", "Amigo"),
            text = json.optString("text", ""),
            createdAtMillis = json.optLong("createdAtMillis", System.currentTimeMillis())
        )
    }
}

/**
 * Publicación sincronizada en el Feed compartido entre amigos.
 */
data class SyncedFeedPost(
    val id: String,
    val authorTag: String,
    val authorName: String,
    val authorAvatarUrl: String? = null,
    val habitTitle: String,
    val caption: String,
    val imageUri: String? = null,
    val likesCount: Int = 0,
    val likedByTags: List<String> = emptyList(),
    val comments: List<SyncedFeedComment> = emptyList(),
    val createdAtMillis: Long = System.currentTimeMillis()
) {
    fun toJson(): JSONObject = JSONObject().apply {
        put("id", id)
        put("authorTag", authorTag)
        put("authorName", authorName)
        put("authorAvatarUrl", authorAvatarUrl ?: "")
        put("habitTitle", habitTitle)
        put("caption", caption)
        put("imageUri", imageUri ?: "")
        put("likesCount", likesCount)
        put("likedByTags", JSONArray(likedByTags))
        put("comments", JSONArray().apply { comments.forEach { put(it.toJson()) } })
        put("createdAtMillis", createdAtMillis)
    }

    companion object {
        fun fromJson(json: JSONObject): SyncedFeedPost {
            val likedArr = json.optJSONArray("likedByTags")
            val likedList = buildList {
                if (likedArr != null) {
                    for (i in 0 until likedArr.length()) add(likedArr.optString(i))
                }
            }.filter { it.isNotBlank() }

            val commentsArr = json.optJSONArray("comments")
            val commentList = buildList {
                if (commentsArr != null) {
                    for (i in 0 until commentsArr.length()) {
                        commentsArr.optJSONObject(i)?.let { add(SyncedFeedComment.fromJson(it)) }
                    }
                }
            }.distinctBy { "${it.id}_${it.text.trim()}" }

            return SyncedFeedPost(
                id = json.optString("id", "post_${System.currentTimeMillis()}"),
                authorTag = CloudSyncManager.normalizeTagCode(json.optString("authorTag", "777")),
                authorName = json.optString("authorName", "Amigo"),
                authorAvatarUrl = json.optString("authorAvatarUrl", "").takeIf { it.isNotBlank() },
                habitTitle = json.optString("habitTitle", "Hábito diario"),
                caption = json.optString("caption", ""),
                imageUri = json.optString("imageUri", "").takeIf { it.isNotBlank() },
                likesCount = json.optInt("likesCount", 0).coerceAtLeast(0),
                likedByTags = likedList,
                comments = commentList,
                createdAtMillis = json.optLong("createdAtMillis", System.currentTimeMillis())
            )
        }
    }
}

/**
 * Perfil sincronizado de un usuario o amigo en tiempo real (puntos, hábitos, racha y mascota).
 */
data class SyncedUserProfile(
    val userId: String,
    val tagCode: String,
    val displayName: String,
    val email: String? = null,
    val avatarUrl: String? = null,
    val score: Int = 0,
    val completedHabits: Int = 0,
    val currentStreak: Int = 0,
    val petType: String = PetType.OCTO.id,
    val petAccessory: String = PetAccessory.NONE.id,
    val petHabitat: String = PetHabitat.AQUARIUM.id,
    val petLevel: Int = 1,
    val updatedAt: Long = System.currentTimeMillis(),
    val isOnline: Boolean = true,
    val passwordHash: String = ""
) {
    val normalizedTag: String
        get() = CloudSyncManager.normalizeTagCode(tagCode)

    val formattedTag: String
        get() = "#$normalizedTag"

    val displayNameWithTag: String
        get() = if (displayName.contains("#$normalizedTag")) displayName else "$displayName $formattedTag"

    val petEmoji: String
        get() = PetType.values().firstOrNull { it.id == petType }?.iconEmoji ?: "🐙"

    val accessoryEmoji: String
        get() = PetAccessory.values().firstOrNull { it.id == petAccessory }?.emoji.orEmpty()

    /**
     * Convierte este perfil sincronizado en un [RankingUser] para la tabla de posiciones.
     */
    fun toRankingUser(rankPosition: Int = 0, isCurrentUser: Boolean = false): RankingUser {
        val computedScore = if (score > 0) score else (completedHabits * 100) + (currentStreak * 50)
        val badgeText = buildString {
            append(petEmoji)
            if (accessoryEmoji.isNotEmpty()) append(accessoryEmoji)
        }.ifEmpty { "⚡" }

        return RankingUser(
            id = userId.ifBlank { "user_$normalizedTag" },
            name = displayNameWithTag,
            avatarUrl = avatarUrl,
            score = computedScore,
            completedHabits = completedHabits,
            currentStreak = currentStreak,
            rankPosition = rankPosition,
            isCurrentUser = isCurrentUser,
            badge = badgeText,
            isFriend = !isCurrentUser
        )
    }

    fun toJson(): JSONObject = JSONObject().apply {
        put("userId", userId)
        put("tagCode", normalizedTag)
        put("displayName", displayName)
        put("email", email ?: "")
        put("avatarUrl", avatarUrl ?: "")
        put("score", score)
        put("completedHabits", completedHabits)
        put("currentStreak", currentStreak)
        put("petType", petType)
        put("petAccessory", petAccessory)
        put("petHabitat", petHabitat)
        put("petLevel", petLevel)
        put("updatedAt", updatedAt)
        put("isOnline", isOnline)
        put("passwordHash", passwordHash)
    }

    companion object {
        fun fromJson(json: JSONObject): SyncedUserProfile {
            val rawTag = json.optString("tagCode", "777")
            val tag = CloudSyncManager.normalizeTagCode(rawTag)
            val habits = json.optInt("completedHabits", 0).coerceAtLeast(0)
            val streak = json.optInt("currentStreak", 0).coerceAtLeast(0)
            val rawScore = json.optInt("score", (habits * 100) + (streak * 50))
            return SyncedUserProfile(
                userId = json.optString("userId", "user_$tag"),
                tagCode = tag,
                displayName = json.optString("displayName", "HabitOcto #$tag").removeSuffix(" #$tag").trim(),
                email = json.optString("email", "").takeIf { it.isNotBlank() },
                avatarUrl = json.optString("avatarUrl", "").takeIf { it.isNotBlank() },
                score = rawScore,
                completedHabits = habits,
                currentStreak = streak,
                petType = json.optString("petType", PetType.OCTO.id),
                petAccessory = json.optString("petAccessory", PetAccessory.NONE.id),
                petHabitat = json.optString("petHabitat", PetHabitat.AQUARIUM.id),
                petLevel = json.optInt("petLevel", maxOf(1, habits / 10 + 1)),
                updatedAt = json.optLong("updatedAt", System.currentTimeMillis()),
                isOnline = json.optBoolean("isOnline", true),
                passwordHash = json.optString("passwordHash", "")
            )
        }
    }
}

/**
 * CloudSyncManager gestiona la autenticación persistente multi-cuenta y la sincronización
 * en tiempo real entre múltiples dispositivos mediante códigos únicos de 3 dígitos (#100 a #999).
 *
 * - **¿Para qué sirve?** Permite que múltiples compañeros o usuarios descarguen la app, cada uno tenga su cuenta
 *   independiente con su propio código de amigo (#XXX), puedan agregarse mutuamente y compitan en el Ranking en tiempo real.
 * - **¿Cómo funciona?**
 *   1. Arquitectura Dual Cloud: Detecta dinámicamente si Firebase está inicializado (`FirebaseApp.getApps`); si está activo,
 *      sincroniza con Firestore. Si el compañero compila sin `google-services.json`, conmuta sin errores al relay REST HTTP
 *      y al directorio determinista local con persistencia en SharedPreferences y Room.
 *   2. Genera o asigna un código de 3 dígitos (#100..#999) único a cada perfil.
 *   3. Mantiene corrutinas en segundo plano para actualizar amigos y rankings automáticamente.
 * - **¿Qué pasa si lo quitamos?** Todos los usuarios que descarguen el APK compartirían un perfil estático local sin poder
 *   agregarse como amigos, sin códigos de identificación y sin sincronización de ranking en tiempo real.
 */
class CloudSyncManager(context: Context) {

    private val appContext: Context = context.applicationContext
    private val prefs: SharedPreferences =
        appContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val isRobolectricTest: Boolean = runCatching {
        Class.forName("org.robolectric.Robolectric") != null
    }.getOrDefault(false)

    private val httpClient: OkHttpClient = OkHttpClient.Builder()
        .connectTimeout(4, TimeUnit.SECONDS)
        .readTimeout(4, TimeUnit.SECONDS)
        .writeTimeout(4, TimeUnit.SECONDS)
        .build()

    private val _currentSession = MutableStateFlow<AuthSession?>(null)
    val currentSession: StateFlow<AuthSession?> = _currentSession.asStateFlow()

    private val _savedSessions = MutableStateFlow<List<AuthSession>>(emptyList())
    val savedSessions: StateFlow<List<AuthSession>> = _savedSessions.asStateFlow()

    private val _syncedFriends = MutableStateFlow<List<SyncedUserProfile>>(emptyList())
    val syncedFriends: StateFlow<List<SyncedUserProfile>> = _syncedFriends.asStateFlow()

    private val _syncState = MutableStateFlow(CloudSyncState.IDLE)
    val syncState: StateFlow<CloudSyncState> = _syncState.asStateFlow()

    private val _lastSyncMessage = MutableStateFlow("Sincronización multi-dispositivo activa")
    val lastSyncMessage: StateFlow<String> = _lastSyncMessage.asStateFlow()

    private val _syncedFeedPosts = MutableStateFlow<List<SyncedFeedPost>>(emptyList())
    val syncedFeedPosts: StateFlow<List<SyncedFeedPost>> = _syncedFeedPosts.asStateFlow()

    private val _petHappinessBonus = MutableStateFlow(0)
    val petHappinessBonus: StateFlow<Int> = _petHappinessBonus.asStateFlow()

    // Caché en memoria de perfiles publicados por código #100..#999
    private val publishedDirectory = mutableMapOf<String, SyncedUserProfile>()
    private var realtimeSyncJob: Job? = null

    init {
        seedInitialDirectory()
        loadPersistedDirectory()
        loadPersistedSessions()
        loadPersistedFriends()
        loadPersistedFeedPosts()
        loadPetHappinessBonus()
        updateSyncModeBanner()
    }

    /**
     * Comprueba de manera 100% segura si Firebase Firestore REST está operativo en este entorno.
     * En pruebas unitarias Robolectric se mantiene desactivado para no depender de red real.
     */
    fun isFirebaseInitialized(): Boolean {
        if (isRobolectricTest) return false
        return !getFirebaseProjectId().isNullOrBlank()
    }

    /**
     * Obtiene el Project ID de Firebase desde `FirebaseApp` o usa el proyecto configurado de HabitOcto.
     */
    private fun getFirebaseProjectId(): String? {
        if (isRobolectricTest) return null
        return runCatching {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                FirebaseApp.getInstance().options.projectId?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        }.getOrNull() ?: DEFAULT_FIREBASE_PROJECT_ID
    }

    private fun updateSyncModeBanner() {
        val firebaseReady = isFirebaseInitialized()
        _syncState.value = if (firebaseReady) {
            CloudSyncState.SYNCED_FIREBASE
        } else {
            CloudSyncState.LOCAL_ACTIVE
        }
        _lastSyncMessage.value = if (firebaseReady) {
            "Firebase Cloud + REST Realtime activos"
        } else {
            "Dual REST Sync + Directorio #100-#999 activo (sin requerir google-services.json)"
        }
    }

    // ============================================================================
    // GESTIÓN DE SESIÓN Y AUTENTICACIÓN PERSISTENTE (ROOM + SHAREDPREFERENCES)
    // ============================================================================

    /**
     * Indica si hay una sesión activa o guardada en este dispositivo.
     */
    fun hasSavedSession(): Boolean = _currentSession.value != null || _savedSessions.value.isNotEmpty()

    /**
     * Devuelve la sesión activa o la última sesión guardada en el dispositivo.
     */
    fun getActiveOrLastSession(): AuthSession? = _currentSession.value ?: _savedSessions.value.firstOrNull()

    /**
     * Crea una cuenta nueva con contraseña obligatoria y #Tag único asignado exclusivamente por el sistema
     * (sin permitir que el usuario lo elija ni que se repita con perfiles existentes).
     */
    suspend fun registerAccount(
        name: String,
        email: String,
        password: String,
        avatarUrl: String? = null,
        completedHabits: Int = 0,
        currentStreak: Int = 0,
        petCustomization: PetCustomization = PetCustomization()
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanName = name.trim()
            val cleanEmail = email.trim().lowercase()
            require(cleanName.length >= 2) { "Ingresa tu nombre o alias (mínimo 2 caracteres)." }
            require(cleanEmail.contains("@") && cleanEmail.contains(".")) {
                "Ingresa un correo electrónico válido."
            }
            require(password.length >= 4) {
                "Crea una contraseña segura de al menos 4 caracteres."
            }

            // Verificar que el correo no exista ya en cuentas locales guardadas
            val duplicateEmail = _savedSessions.value.firstOrNull {
                it.email.equals(cleanEmail, ignoreCase = true)
            } ?: synchronized(publishedDirectory) {
                publishedDirectory.values.firstOrNull { it.email.equals(cleanEmail, ignoreCase = true) }
            }?.let { existing ->
                AuthSession(
                    userId = existing.userId,
                    displayName = existing.displayName,
                    email = existing.email ?: cleanEmail,
                    tagCode = existing.normalizedTag
                )
            }
            if (duplicateEmail != null) {
                error("Ya existe una cuenta con ese correo (${duplicateEmail.formattedTag}). Inicia sesión con tu contraseña.")
            }

            val assignedTag = assignUniqueSystemTag(cleanName, cleanEmail)
            val pwHash = hashPassword(password, cleanEmail)
            val resolvedAvatar = avatarUrl?.trim()?.takeIf { it.isNotBlank() && !AvatarCatalog.isBrokenUrl(it) }

            val userId = "local-user"
            val session = AuthSession(
                userId = userId,
                displayName = cleanName,
                email = cleanEmail,
                tagCode = assignedTag,
                avatarUrl = resolvedAvatar,
                isLoggedIn = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                syncMode = if (isFirebaseInitialized()) "Firebase + REST Sync" else "Dual Cloud REST + P2P",
                passwordHash = pwHash
            )
            persistSession(session)

            syncUserEntityInRoom(userId, cleanName, cleanEmail, assignedTag, resolvedAvatar)
            publishCurrentUserStats(
                completedHabits = completedHabits,
                currentStreak = currentStreak,
                petCustomization = petCustomization,
                userNameOverride = cleanName
            )
            seedPredefinedFriendsFeed()
            runCatching { refreshFriendsRealtime() }
            refreshFriendsFeed()
            session
        }
    }

    /**
     * Inicia sesión validando correo/usuario/#Tag y contraseña obligatoria (tanto en primer ingreso
     * como al cambiar de usuario dentro de la aplicación).
     */
    suspend fun authenticateAccount(
        identifier: String,
        password: String,
        completedHabits: Int = 0,
        currentStreak: Int = 0,
        petCustomization: PetCustomization = PetCustomization()
    ): Result<AuthSession> = withContext(Dispatchers.IO) {
        runCatching {
            val cleanId = identifier.trim()
            require(cleanId.isNotEmpty()) { "Ingresa tu correo, nombre o código #Tag." }
            require(password.length >= 4) { "Ingresa tu contraseña (mínimo 4 caracteres)." }

            val cleanTagCandidate = cleanId.removePrefix("#").trim()
            val localSession = _savedSessions.value.firstOrNull { s ->
                s.email.equals(cleanId, ignoreCase = true) ||
                    s.displayName.equals(cleanId, ignoreCase = true) ||
                    (isValidTagCode(cleanTagCandidate) && s.normalizedTag == normalizeTagCode(cleanTagCandidate))
            }

            val directoryMatch = if (localSession == null) {
                val byTag = if (isValidTagCode(cleanTagCandidate)) {
                    val norm = normalizeTagCode(cleanTagCandidate)
                    (if (isFirebaseInitialized()) fetchFromFirebaseFirestore(norm) else null)
                        ?: fetchFromSharedRestRelay(norm)
                        ?: synchronized(publishedDirectory) { publishedDirectory[norm] }
                } else null

                byTag ?: synchronized(publishedDirectory) {
                    publishedDirectory.values.firstOrNull { p ->
                        p.email.equals(cleanId, ignoreCase = true) ||
                            p.displayName.equals(cleanId, ignoreCase = true)
                    }
                }
            } else null

            val targetEmail = (localSession?.email ?: directoryMatch?.email)?.trim()?.lowercase()
                ?: error("Cuenta no encontrada en este dispositivo ni en la nube.")
            val targetName = localSession?.displayName ?: directoryMatch?.displayName ?: "Usuario HabitOcto"
            val targetTag = localSession?.normalizedTag ?: directoryMatch?.normalizedTag
                ?: assignUniqueSystemTagLocal(targetName, targetEmail)
            val storedHash = localSession?.passwordHash?.takeIf { it.isNotBlank() }
                ?: directoryMatch?.passwordHash?.takeIf { it.isNotBlank() }
                ?: ""
            val providedHash = hashPassword(password, targetEmail)

            if (storedHash.isNotBlank() && storedHash != providedHash) {
                error("Contraseña incorrecta. Verifica tus credenciales e intenta de nuevo.")
            }

            val finalHash = storedHash.ifBlank { providedHash }
            val resolvedAvatar = localSession?.avatarUrl ?: directoryMatch?.avatarUrl
            val authenticated = AuthSession(
                userId = "local-user",
                displayName = targetName,
                email = targetEmail,
                tagCode = targetTag,
                avatarUrl = resolvedAvatar,
                isLoggedIn = true,
                lastSyncTimestamp = System.currentTimeMillis(),
                syncMode = if (isFirebaseInitialized()) "Firebase + REST Sync" else "Dual Cloud REST + P2P",
                passwordHash = finalHash
            )
            persistSession(authenticated)
            syncUserEntityInRoom("local-user", targetName, targetEmail, targetTag, resolvedAvatar)
            publishCurrentUserStats(
                completedHabits = completedHabits,
                currentStreak = currentStreak,
                petCustomization = petCustomization,
                userNameOverride = targetName
            )
            runCatching { refreshFriendsRealtime() }
            refreshFriendsFeed()
            authenticated
        }
    }

    /**
     * Asigna un código #XXX (100..999) único asignado exclusivamente por el sistema,
     * comprobando sesiones locales, directorio y nube (Firestore / REST Relay).
     */
    suspend fun assignUniqueSystemTag(name: String, email: String): String = withContext(Dispatchers.IO) {
        val localTaken = collectLocallyTakenTags(excludeEmail = email)
        val seedNum = generateDeterministicTag(name, email).toIntOrNull() ?: 100
        for (offset in 0 until 900) {
            val candidate = (100 + ((seedNum - 100 + offset) % 900)).toString()
            if (candidate in localTaken) continue
            val remote = (if (isFirebaseInitialized()) fetchFromFirebaseFirestore(candidate) else null)
                ?: fetchFromSharedRestRelay(candidate)
            if (remote == null || remote.email.equals(email, ignoreCase = true)) {
                return@withContext candidate
            }
        }
        assignUniqueSystemTagLocal(name, email)
    }

    fun assignUniqueSystemTagLocal(name: String, email: String): String {
        val taken = collectLocallyTakenTags(excludeEmail = email)
        val seedNum = generateDeterministicTag(name, email).toIntOrNull() ?: 100
        for (offset in 0 until 900) {
            val candidate = (100 + ((seedNum - 100 + offset) % 900)).toString()
            if (candidate !in taken) return candidate
        }
        return normalizeTagCode(seedNum.toString())
    }

    private fun collectLocallyTakenTags(excludeEmail: String): Set<String> {
        val taken = mutableSetOf<String>()
        _savedSessions.value
            .filterNot { it.email.equals(excludeEmail, ignoreCase = true) }
            .forEach { taken += it.normalizedTag }
        _syncedFriends.value.forEach { taken += it.normalizedTag }
        synchronized(publishedDirectory) {
            publishedDirectory.values
                .filterNot { it.email.equals(excludeEmail, ignoreCase = true) }
                .forEach { taken += it.normalizedTag }
        }
        return taken
    }

    private suspend fun syncUserEntityInRoom(
        userId: String,
        name: String,
        email: String,
        tag: String,
        avatarUrl: String?
    ) {
        runCatching {
            val db = HabitDatabase.get(appContext)
            val dao = db.habits()
            val today = HabitDates.today()
            val existing = dao.observeUser(userId).firstOrNull()?.user
            dao.insertUser(
                UserEntity(
                    id = userId,
                    displayName = name,
                    email = email,
                    avatarUrl = avatarUrl ?: existing?.avatarUrl,
                    createdDay = existing?.createdDay ?: today,
                    friendCode = tag,
                    isLoggedIn = true,
                    petType = existing?.petType ?: PetType.OCTO.id,
                    petAccessory = existing?.petAccessory ?: PetAccessory.NONE.id,
                    petHabitat = existing?.petHabitat ?: PetHabitat.AQUARIUM.id
                )
            )
            dao.loginOrRegisterUser(userId, name, email, tag)
            if (!avatarUrl.isNullOrBlank()) {
                dao.updateAvatar(userId, avatarUrl)
            }
        }
    }

    /** Actualiza el avatar en la sesión activa y fuerza sincronización inmediata hacia la nube. */
    fun updateActiveSessionAvatar(avatarUrl: String?) {
        val active = _currentSession.value ?: return
        val cleanAvatar = avatarUrl?.trim()?.takeIf { it.isNotBlank() && !AvatarCatalog.isBrokenUrl(it) }
        val updated = active.copy(avatarUrl = cleanAvatar, lastSyncTimestamp = System.currentTimeMillis())
        persistSession(updated)
    }

    /**
     * Registra o inicia sesión asignando automáticamente un #Tag único del sistema si no se pasa uno.
     */
    suspend fun loginOrRegister(
        name: String,
        email: String,
        preferredTag: String? = null,
        completedHabits: Int = 0,
        currentStreak: Int = 0,
        petCustomization: PetCustomization = PetCustomization(),
        password: String = ""
    ): AuthSession = withContext(Dispatchers.IO) {
        val cleanName = name.trim().ifBlank { "Usuario" }
        val cleanEmail = email.trim().lowercase().ifBlank { "${cleanName.lowercase().replace(" ", ".")}@habitocto.app" }
        val existing = _savedSessions.value.firstOrNull {
            it.email.equals(cleanEmail, ignoreCase = true) ||
                (!preferredTag.isNullOrBlank() && isValidTagCode(preferredTag) && it.normalizedTag == normalizeTagCode(preferredTag))
        }
        val tag = when {
            existing != null -> existing.normalizedTag
            !preferredTag.isNullOrBlank() && isValidTagCode(preferredTag) && normalizeTagCode(preferredTag) !in collectLocallyTakenTags(cleanEmail) ->
                normalizeTagCode(preferredTag)
            else -> assignUniqueSystemTag(cleanName, cleanEmail)
        }
        val pwHash = if (password.length >= 4) hashPassword(password, cleanEmail) else (existing?.passwordHash ?: "")

        val userId = "local-user"
        val session = AuthSession(
            userId = userId,
            displayName = cleanName,
            email = cleanEmail,
            tagCode = tag,
            avatarUrl = existing?.avatarUrl,
            isLoggedIn = true,
            lastSyncTimestamp = System.currentTimeMillis(),
            syncMode = if (isFirebaseInitialized()) "Firebase + REST Sync" else "Dual Cloud REST + P2P",
            passwordHash = pwHash
        )

        persistSession(session)
        syncUserEntityInRoom(userId, cleanName, cleanEmail, tag, session.avatarUrl)

        // Publicar perfil inmediatamente en el directorio y nube
        publishCurrentUserStats(
            completedHabits = completedHabits,
            currentStreak = currentStreak,
            petCustomization = petCustomization,
            userNameOverride = cleanName
        )

        session
    }

    /**
     * Versión asíncrona no bloqueante para iniciar sesión o registrarse desde callbacks de Jetpack Compose.
     *
     * - **¿Para qué sirve?** Crea o autentica la cuenta del usuario en segundo plano sin congelar la interfaz gráfica.
     * - **¿Cómo funciona?** Genera inmediatamente una sesión local activa, persiste los datos en Room y SharedPreferences,
     *   ejecuta la sincronización en la nube con `Dispatchers.IO` y notifica mediante el callback `onComplete`.
     * - **¿Qué pasa si lo quitamos?** El registro o inicio de sesión bloquearía el hilo principal de la UI (ANR).
     */
    fun loginOrRegisterAsync(
        name: String,
        email: String,
        preferredTag: String? = null,
        completedHabits: Int = 0,
        currentStreak: Int = 0,
        petCustomization: PetCustomization = PetCustomization(),
        onComplete: (AuthSession) -> Unit = {}
    ) {
        val cleanName = name.trim().ifBlank { "Usuario" }
        val cleanEmail = email.trim().lowercase().ifBlank { "${cleanName.lowercase().replace(" ", ".")}@habitocto.app" }
        val tag = if (!preferredTag.isNullOrBlank() && isValidTagCode(preferredTag) &&
            normalizeTagCode(preferredTag) !in collectLocallyTakenTags(cleanEmail)
        ) {
            normalizeTagCode(preferredTag)
        } else {
            assignUniqueSystemTagLocal(cleanName, cleanEmail)
        }

        val immediateSession = AuthSession(
            userId = "local-user",
            displayName = cleanName,
            email = cleanEmail,
            tagCode = tag,
            isLoggedIn = true,
            lastSyncTimestamp = System.currentTimeMillis(),
            syncMode = if (isFirebaseInitialized()) "Firebase + REST Sync" else "Dual Cloud REST + P2P"
        )
        persistSession(immediateSession)
        onComplete(immediateSession)

        scope.launch {
            loginOrRegister(
                name = cleanName,
                email = cleanEmail,
                preferredTag = tag,
                completedHabits = completedHabits,
                currentStreak = currentStreak,
                petCustomization = petCustomization
            )
        }
    }

    /**
     * Continúa con una sesión previamente guardada en este dispositivo.
     *
     * - **¿Para qué sirve?** Permite al usuario reingresar de inmediato a su cuenta guardada sin tener que volver a escribir su nombre o correo.
     * - **¿Cómo funciona?** Carga la sesión desde el historial persistido, actualiza la marca de tiempo, reactiva la sesión en Room
     *   y notifica a los StateFlows reactivos de la aplicación.
     * - **¿Qué pasa si lo quitamos?** Cada vez que el usuario abra la app tendría que registrarse de nuevo desde cero.
     */
    fun continueWithSavedSession(
        session: AuthSession? = null,
        onComplete: (AuthSession) -> Unit = {}
    ): AuthSession? {
        val target = session ?: getActiveOrLastSession() ?: return null
        val activated = target.copy(isLoggedIn = true, lastSyncTimestamp = System.currentTimeMillis())
        persistSession(activated)

        scope.launch {
            runCatching {
                val db = HabitDatabase.get(appContext)
                val dao = db.habits()
                dao.insertUser(
                    UserEntity(
                        id = activated.userId,
                        displayName = activated.displayName,
                        email = activated.email,
                        createdDay = HabitDates.today()
                    )
                )
                dao.updateProfile(activated.userId, activated.displayName, activated.email)
            }
        }
        onComplete(activated)
        return activated
    }

    /**
     * Cierra la sesión activa permitiendo cambiar de cuenta o elegir otra guardada en el dispositivo.
     */
    fun logout() {
        _currentSession.value = null
        prefs.edit().remove(KEY_ACTIVE_SESSION).apply()
        _lastSyncMessage.value = "Sesión cerrada. Tus cuentas guardadas siguen disponibles en este dispositivo."
    }

    private fun persistSession(session: AuthSession) {
        _currentSession.value = session
        val updatedList = (_savedSessions.value.filterNot {
            it.tagCode == session.tagCode && it.displayName.equals(session.displayName, ignoreCase = true)
        } + session).sortedByDescending { it.lastSyncTimestamp }.take(8)
        _savedSessions.value = updatedList

        val activeJson = sessionToJson(session).toString()
        val array = JSONArray()
        updatedList.forEach { array.put(sessionToJson(it)) }

        prefs.edit()
            .putString(KEY_ACTIVE_SESSION, activeJson)
            .putString(KEY_SAVED_SESSIONS, array.toString())
            .apply()
    }

    private fun loadPersistedSessions() {
        runCatching {
            val savedRaw = prefs.getString(KEY_SAVED_SESSIONS, null)
            val list = mutableListOf<AuthSession>()
            if (!savedRaw.isNullOrBlank()) {
                val arr = JSONArray(savedRaw)
                for (i in 0 until arr.length()) {
                    list.add(jsonToSession(arr.getJSONObject(i)))
                }
            }
            _savedSessions.value = list

            val activeRaw = prefs.getString(KEY_ACTIVE_SESSION, null)
            if (!activeRaw.isNullOrBlank()) {
                val restored = jsonToSession(JSONObject(activeRaw))
                if (restored.isLoggedIn && restored.passwordHash.isNotBlank()) {
                    _currentSession.value = restored
                } else {
                    _currentSession.value = null
                    prefs.edit().remove(KEY_ACTIVE_SESSION).apply()
                }
            } else {
                // Sin sesión previa (primera instalación desde APK, repo o Android Studio):
                // permanecer sin autenticar para mostrar la pantalla de Login / Crear cuenta.
                _currentSession.value = null
            }
        }
    }

    private fun sessionToJson(session: AuthSession): JSONObject = JSONObject().apply {
        put("userId", session.userId)
        put("displayName", session.displayName)
        put("email", session.email)
        put("tagCode", session.normalizedTag)
        put("avatarUrl", session.avatarUrl ?: "")
        put("isLoggedIn", session.isLoggedIn)
        put("lastSyncTimestamp", session.lastSyncTimestamp)
        put("syncMode", session.syncMode)
        put("passwordHash", session.passwordHash)
    }

    private fun jsonToSession(json: JSONObject): AuthSession = AuthSession(
        userId = json.optString("userId", "local-user"),
        displayName = json.optString("displayName", "Usuario"),
        email = json.optString("email", ""),
        tagCode = normalizeTagCode(json.optString("tagCode", "100")),
        avatarUrl = json.optString("avatarUrl", "").takeIf { it.isNotBlank() },
        isLoggedIn = json.optBoolean("isLoggedIn", false),
        lastSyncTimestamp = json.optLong("lastSyncTimestamp", System.currentTimeMillis()),
        syncMode = json.optString("syncMode", "Dual Cloud + Realtime REST"),
        passwordHash = json.optString("passwordHash", "")
    )

    // ============================================================================
    // PUBLICACIÓN Y SINCRONIZACIÓN EN TIEMPO REAL (DUAL CLOUD + REST + DIRECTORIO)
    // ============================================================================

    /**
     * Publica las estadísticas actuales del usuario (puntos, hábitos completados, racha y mascota)
     * en el directorio local, en el endpoint REST compartido y en Firebase (si está disponible).
     */
    suspend fun publishCurrentUserStats(
        completedHabits: Int,
        currentStreak: Int,
        petCustomization: PetCustomization = PetCustomization(),
        userNameOverride: String? = null
    ): SyncedUserProfile = withContext(Dispatchers.IO) {
        val session = getActiveOrLastSession() ?: AuthSession(
            userId = "local-user",
            displayName = userNameOverride ?: "Suchini",
            email = "suchini@habitocto.app",
            tagCode = "777"
        )
        val effectiveName = userNameOverride?.takeIf { it.isNotBlank() } ?: session.displayName
        val tag = session.normalizedTag
        val score = (completedHabits * 100) + (currentStreak * 50)
        val level = maxOf(1, completedHabits / 10 + 1)

        val profile = SyncedUserProfile(
            userId = "user_$tag",
            tagCode = tag,
            displayName = effectiveName,
            email = session.email,
            avatarUrl = session.avatarUrl,
            score = score,
            completedHabits = completedHabits,
            currentStreak = currentStreak,
            petType = petCustomization.petType.id,
            petAccessory = petCustomization.accessory.id,
            petHabitat = petCustomization.habitat.id,
            petLevel = level,
            updatedAt = System.currentTimeMillis(),
            isOnline = true,
            passwordHash = session.passwordHash
        )

        // 1. Actualizar directorio local persistente
        synchronized(publishedDirectory) {
            publishedDirectory[tag] = profile
        }
        savePublishedDirectory()

        // 2. Intentar sincronizar en Firebase Firestore REST / SDK si FirebaseApp está activo
        val syncedFirebase = if (isFirebaseInitialized()) {
            pushToFirebaseFirestore(profile)
        } else {
            false
        }

        // 3. Intentar sincronizar en el relay REST JSON compartido con OkHttpClient
        val syncedRest = pushToSharedRestRelay(profile)

        _syncState.value = when {
            syncedFirebase -> CloudSyncState.SYNCED_FIREBASE
            syncedRest -> CloudSyncState.SYNCED_REST
            else -> CloudSyncState.LOCAL_ACTIVE
        }
        _lastSyncMessage.value = "Perfil ${profile.formattedTag} sincronizado (${profile.score} pts • racha ${profile.currentStreak}d)"

        profile
    }

    /**
     * Versión asíncrona segura para invocar desde Composables o ViewModels cada vez que cambian hábitos/racha.
     */
    fun syncUserStatsAsync(
        completedHabits: Int,
        currentStreak: Int,
        petCustomization: PetCustomization = PetCustomization(),
        userNameOverride: String? = null
    ) {
        scope.launch {
            runCatching {
                publishCurrentUserStats(
                    completedHabits = completedHabits,
                    currentStreak = currentStreak,
                    petCustomization = petCustomization,
                    userNameOverride = userNameOverride
                )
            }
        }
    }

    /**
     * Busca y agrega un amigo por su código de 3 dígitos (`#100` a `#999`) o por un Token de Sincronización
     * compartido por WhatsApp/Portapapeles (ej. `OCTO#452|Carlos|1420|11|8|dragon`).
     */
    suspend fun addFriendByCode(
        rawCodeOrToken: String,
        optionalCustomName: String? = null,
        optionalStreak: Int? = null,
        optionalHabits: Int? = null
    ): Result<SyncedUserProfile> = withContext(Dispatchers.IO) {
        runCatching {
            _syncState.value = CloudSyncState.SYNCING
            val trimmed = rawCodeOrToken.trim()
            require(trimmed.isNotEmpty()) { "Ingresa un código de 3 dígitos (ej. #204) o un código de invitación." }

            // Caso 1: Token de sincronización instantánea compartido (contiene '|')
            val parsedFromToken = parseShareSyncToken(trimmed)
            if (parsedFromToken != null) {
                registerSyncedFriend(parsedFromToken)
                _syncState.value = CloudSyncState.LOCAL_ACTIVE
                _lastSyncMessage.value = "¡Amigo ${parsedFromToken.displayNameWithTag} sincronizado al instante!"
                return@runCatching parsedFromToken
            }

            // Extraer código de 3 dígitos (#100 a #999)
            val extractedDigits = Regex("(\\d{3})").find(trimmed)?.groupValues?.get(1)
            val tagCode = when {
                extractedDigits != null -> normalizeTagCode(extractedDigits)
                else -> generateDeterministicTag(trimmed)
            }

            // 1. Intentar consultar en Firebase Cloud si está inicializado
            val fromFirebase = if (isFirebaseInitialized()) {
                fetchFromFirebaseFirestore(tagCode)
            } else {
                null
            }

            // 2. Intentar consultar en el relay REST JSON compartido con OkHttpClient
            val fromRest = fromFirebase ?: fetchFromSharedRestRelay(tagCode)

            // 3. Consultar en el directorio publicado o crear perfil básico de respaldo sin inventar nombres falsos
            val cachedProfile = fromRest ?: synchronized(publishedDirectory) { publishedDirectory[tagCode] }
            val fallbackName = optionalCustomName?.trim()?.takeIf { it.isNotBlank() } ?: "Amigo #$tagCode"
            val baseProfile = cachedProfile ?: SyncedUserProfile(
                userId = "user_$tagCode",
                tagCode = tagCode,
                displayName = fallbackName,
                score = 0,
                completedHabits = 0,
                currentStreak = 0
            )

            // Si el usuario proporcionó nombre o estadísticas personalizadas al agregar, se aplican
            val finalHabits = optionalHabits ?: baseProfile.completedHabits
            val finalStreak = optionalStreak ?: baseProfile.currentStreak
            val finalScore = (finalHabits * 100) + (finalStreak * 50)
            val finalName = optionalCustomName?.trim()?.takeIf { it.isNotBlank() } ?: baseProfile.displayName

            val resolvedProfile = baseProfile.copy(
                tagCode = tagCode,
                displayName = finalName,
                completedHabits = finalHabits,
                currentStreak = finalStreak,
                score = finalScore,
                petLevel = maxOf(1, finalHabits / 10 + 1),
                updatedAt = System.currentTimeMillis()
            )

            registerSyncedFriend(resolvedProfile)
            // Registrar vínculo mutuo bidireccional en Firestore para que ambos amigos vean el Feed compartido
            val myTag = _currentSession.value?.normalizedTag
            if (isFirebaseInitialized() && myTag != null && myTag != tagCode) {
                pushMutualFriendshipToFirestore(myTag = myTag, friendTag = tagCode)
            }
            // Solo publicamos al relay REST si no tenemos aún un perfil remoto real
            if (fromRest == null) {
                pushToSharedRestRelay(resolvedProfile)
            }
            runCatching { refreshFriendsFeed() }

            _syncState.value = if (fromFirebase != null) {
                CloudSyncState.SYNCED_FIREBASE
            } else if (fromRest != null) {
                CloudSyncState.SYNCED_REST
            } else {
                CloudSyncState.LOCAL_ACTIVE
            }
            _lastSyncMessage.value = "Amigo ${resolvedProfile.displayNameWithTag} sincronizado (${resolvedProfile.score} pts)"
            resolvedProfile
        }
    }

    /**
     * Agrega a un compañero como amigo mediante su código (#XXX) de forma asíncrona.
     *
     * - **¿Para qué sirve?** Permite conectar dos dispositivos para que los compañeros se vean mutuamente en el Ranking y en el feed.
     * - **¿Cómo funciona?** Normaliza el código ingresado (ej. "#105" -> "105"), consulta Firebase Firestore, el relay REST y el directorio
     *   local, guarda el amigo en la lista persistida y despacha el resultado en el hilo principal (`Dispatchers.Main`).
     * - **¿Qué pasa si lo quitamos?** El usuario no podría añadir amigos ni competir en el Ranking social.
     */
    fun addFriendByCodeAsync(
        rawCodeOrToken: String,
        optionalCustomName: String? = null,
        optionalStreak: Int? = null,
        optionalHabits: Int? = null,
        onResult: (Result<SyncedUserProfile>) -> Unit = {}
    ) {
        scope.launch {
            val result = addFriendByCode(
                rawCodeOrToken = rawCodeOrToken,
                optionalCustomName = optionalCustomName,
                optionalStreak = optionalStreak,
                optionalHabits = optionalHabits
            )
            withContext(Dispatchers.Main) {
                onResult(result)
            }
        }
    }

    /**
     * Actualiza en segundo plano todos los amigos agregados (y conexiones mutuas en la nube)
     * consultando Firebase/REST/Directorio, y refresca el Feed compartido.
     */
    suspend fun refreshFriendsRealtime(): List<SyncedUserProfile> = withContext(Dispatchers.IO) {
        val myTag = _currentSession.value?.normalizedTag
        val currentMap = _syncedFriends.value.associateBy { it.normalizedTag }
        val mutualTags = if (myTag != null && isFirebaseInitialized()) {
            fetchMutualFriendTagsFromFirestore(myTag)
        } else {
            emptyList()
        }
        val allTags = (currentMap.keys + mutualTags)
            .filterNot { it == myTag }
            .distinct()

        val updated = allTags.mapNotNull { tag ->
            val currentFriend = currentMap[tag]
            val remote = (if (isFirebaseInitialized()) fetchFromFirebaseFirestore(tag) else null)
                ?: fetchFromSharedRestRelay(tag)
                ?: synchronized(publishedDirectory) { publishedDirectory[tag] }
            if (remote != null) {
                synchronized(publishedDirectory) {
                    publishedDirectory[tag] = remote
                }
                remote
            } else {
                currentFriend
            }
        }.sortedByDescending { it.score }

        savePublishedDirectory()
        _syncedFriends.value = updated
        savePersistedFriends(updated)
        if (myTag != null && isFirebaseInitialized()) {
            syncOwnFriendLinksToFirestore(myTag, updated.map { it.normalizedTag })
        }
        runCatching { refreshFriendsFeed(extraFriendTags = updated.map { it.normalizedTag }.toSet()) }
        updated
    }

    /**
     * Inicia un ciclo de sincronización periódica mientras la pantalla de Ranking o Perfil está abierta.
     */
    fun startRealtimeSyncLoop(intervalMs: Long = 15_000L) {
        if (realtimeSyncJob?.isActive == true) return
        realtimeSyncJob = scope.launch {
            while (isActive) {
                runCatching { refreshFriendsRealtime() }
                delay(intervalMs)
            }
        }
    }

    fun stopRealtimeSyncLoop() {
        realtimeSyncJob?.cancel()
        realtimeSyncJob = null
    }

    /**
     * Elimina a un amigo sincronizado por su código `#XXX`.
     */
    fun removeFriendByTag(tagCode: String) {
        val normalized = normalizeTagCode(tagCode)
        val updated = _syncedFriends.value.filterNot { it.normalizedTag == normalized }
        _syncedFriends.value = updated
        savePersistedFriends(updated)
    }

    /**
     * Construye la lista completa y ordenada del Ranking combinando amigos sincronizados + usuario actual.
     *
     * - **¿Para qué sirve?** Genera la tabla de posiciones con medallas (oro, plata, bronce) para gamificar la consistencia de hábitos.
     * - **¿Cómo funciona?** Fusiona el perfil del usuario actual con todos sus amigos añadidos, calcula los puntos (XP) basados
     *   en hábitos completados y días de racha, ordena descendentemente y asigna las posiciones del 1 al N.
     * - **¿Qué pasa si lo quitamos?** La pantalla de Ranking aparecería vacía o desincronizada respecto a los logros de los amigos.
     */
    fun buildRealtimeRanking(
        currentUserName: String,
        currentUserCompleted: Int,
        currentUserStreak: Int,
        currentPet: PetCustomization = PetCustomization()
    ): List<RankingUser> {
        val session = getActiveOrLastSession()
        val myTag = session?.normalizedTag ?: generateDeterministicTag(currentUserName)
        val myCleanName = currentUserName.removeSuffix(" #$myTag").trim().ifBlank { "Suchini" }
        val myScore = (currentUserCompleted * 100) + (currentUserStreak * 50)
        val myBadge = buildString {
            append(currentPet.petType.iconEmoji)
            if (currentPet.accessory != PetAccessory.NONE) append(currentPet.accessory.emoji)
        }.ifEmpty { "⚡" }

        val currentUserRanking = RankingUser(
            id = "curr_$myTag",
            name = "$myCleanName #$myTag",
            avatarUrl = session?.avatarUrl,
            score = myScore,
            completedHabits = currentUserCompleted,
            currentStreak = currentUserStreak,
            rankPosition = 0,
            isCurrentUser = true,
            badge = myBadge,
            isFriend = false
        )

        val friendRankings = _syncedFriends.value
            .filterNot { it.normalizedTag == myTag }
            .map { it.toRankingUser(isCurrentUser = false) }

        return (friendRankings + currentUserRanking)
            .sortedWith(
                compareByDescending<RankingUser> { it.score }
                    .thenByDescending { it.currentStreak }
                    .thenBy { it.name }
            )
            .mapIndexed { index, item ->
                item.copy(rankPosition = index + 1)
            }
    }

    // ============================================================================
    // CLIENTES DE RED: FIREBASE FIRESTORE (SAFE) + OKHTTP REST RELAY
    // ============================================================================

    private fun pushToFirebaseFirestore(profile: SyncedUserProfile): Boolean {
        val projectId = getFirebaseProjectId() ?: return false
        return runCatching {
            val url =
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/habitocto_users/${profile.normalizedTag}"
            val fields = JSONObject().apply {
                put("userId", JSONObject().put("stringValue", profile.userId))
                put("tagCode", JSONObject().put("stringValue", profile.normalizedTag))
                put("displayName", JSONObject().put("stringValue", profile.displayName))
                put("email", JSONObject().put("stringValue", profile.email ?: ""))
                put("avatarUrl", JSONObject().put("stringValue", profile.avatarUrl ?: ""))
                put("passwordHash", JSONObject().put("stringValue", profile.passwordHash))
                put("score", JSONObject().put("integerValue", profile.score.toString()))
                put("completedHabits", JSONObject().put("integerValue", profile.completedHabits.toString()))
                put("currentStreak", JSONObject().put("integerValue", profile.currentStreak.toString()))
                put("petType", JSONObject().put("stringValue", profile.petType))
                put("petAccessory", JSONObject().put("stringValue", profile.petAccessory))
                put("petHabitat", JSONObject().put("stringValue", profile.petHabitat))
                put("petLevel", JSONObject().put("integerValue", profile.petLevel.toString()))
                put("updatedAt", JSONObject().put("integerValue", profile.updatedAt.toString()))
            }
            val bodyJson = JSONObject().put("fields", fields).toString()
            val request = Request.Builder()
                .url(url)
                .patch(bodyJson.toRequestBody(JSON_MEDIA_TYPE))
                .build()
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrDefault(false)
    }

    private fun fetchFromFirebaseFirestore(tagCode: String): SyncedUserProfile? {
        val projectId = getFirebaseProjectId() ?: return null
        return runCatching {
            val url =
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/habitocto_users/$tagCode"
            val request = Request.Builder().url(url).get().build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val body = response.body?.string() ?: return@use null
                val fields = JSONObject(body).optJSONObject("fields") ?: return@use null
                val docType = fields.optJSONObject("docType")?.optString("stringValue").orEmpty()
                if (docType.isNotBlank() && docType != "user_profile") return@use null
                val rawDisplayName = fields.optJSONObject("displayName")?.optString("stringValue")
                    ?.takeIf { it.isNotBlank() } ?: return@use null
                val habits = fields.optJSONObject("completedHabits")?.optString("integerValue")?.toIntOrNull() ?: 0
                val streak = fields.optJSONObject("currentStreak")?.optString("integerValue")?.toIntOrNull() ?: 0
                val score = fields.optJSONObject("score")?.optString("integerValue")?.toIntOrNull()
                    ?: ((habits * 100) + (streak * 50))
                SyncedUserProfile(
                    userId = fields.optJSONObject("userId")?.optString("stringValue") ?: "user_$tagCode",
                    tagCode = tagCode,
                    displayName = rawDisplayName,
                    email = fields.optJSONObject("email")?.optString("stringValue")?.takeIf { it.isNotBlank() },
                    avatarUrl = fields.optJSONObject("avatarUrl")?.optString("stringValue")?.takeIf { it.isNotBlank() },
                    score = score,
                    completedHabits = habits,
                    currentStreak = streak,
                    petType = fields.optJSONObject("petType")?.optString("stringValue") ?: PetType.OCTO.id,
                    petAccessory = fields.optJSONObject("petAccessory")?.optString("stringValue") ?: PetAccessory.NONE.id,
                    petHabitat = fields.optJSONObject("petHabitat")?.optString("stringValue") ?: PetHabitat.AQUARIUM.id,
                    petLevel = fields.optJSONObject("petLevel")?.optString("integerValue")?.toIntOrNull()
                        ?: maxOf(1, habits / 10 + 1),
                    updatedAt = fields.optJSONObject("updatedAt")?.optString("integerValue")?.toLongOrNull()
                        ?: System.currentTimeMillis(),
                    passwordHash = fields.optJSONObject("passwordHash")?.optString("stringValue").orEmpty()
                )
            }
        }.getOrNull()
    }

    /**
     * Sincroniza mediante `OkHttpClient` contra el relay REST de HabitOcto.
     * Todo fallo de red es capturado silenciosamente para garantizar cero crashes offline.
     */
    private fun pushToSharedRestRelay(profile: SyncedUserProfile): Boolean {
        val customEndpoint = prefs.getString(KEY_CUSTOM_REST_URL, null)
        if (isRobolectricTest && customEndpoint.isNullOrBlank()) return false
        val endpoint = customEndpoint ?: DEFAULT_REST_BASE_URL
        return runCatching {
            val url = "$endpoint/habitocto_tag_${profile.normalizedTag}"
            val request = Request.Builder()
                .url(url)
                .put(profile.toJson().toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(request).execute().use { response ->
                response.isSuccessful
            }
        }.getOrDefault(false)
    }

    private fun fetchFromSharedRestRelay(tagCode: String): SyncedUserProfile? {
        val customEndpoint = prefs.getString(KEY_CUSTOM_REST_URL, null)
        if (isRobolectricTest && customEndpoint.isNullOrBlank()) return null
        val endpoint = customEndpoint ?: DEFAULT_REST_BASE_URL
        return runCatching {
            val url = "$endpoint/habitocto_tag_$tagCode"
            val request = Request.Builder()
                .url(url)
                .get()
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(request).execute().use { response ->
                if (!response.isSuccessful) return@use null
                val raw = response.body?.string() ?: return@use null
                val json = JSONObject(raw)
                SyncedUserProfile.fromJson(json)
            }
        }.getOrNull()
    }

    // ============================================================================
    // DIRECTORIO PRE-SEMBRADO Y GENERADOR DETERMINISTA (#100 A #999)
    // ============================================================================

    private fun seedInitialDirectory() {
        val initialProfiles = listOf(
            SyncedUserProfile(
                userId = "u_101",
                tagCode = "101",
                displayName = "Sofía Mendoza",
                email = "sofia.mendoza@habitocto.app",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                score = 1850,
                completedHabits = 15,
                currentStreak = 7,
                petType = PetType.DRAGON.id,
                petAccessory = PetAccessory.GOLD_CROWN.id,
                petHabitat = PetHabitat.ZEN_GARDEN.id,
                petLevel = 2
            ),
            SyncedUserProfile(
                userId = "u_204",
                tagCode = "204",
                displayName = "Carlos Ruiz",
                email = "carlos.ruiz@habitocto.app",
                avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                score = 1420,
                completedHabits = 11,
                currentStreak = 6,
                petType = PetType.FOX.id,
                petAccessory = PetAccessory.HEADPHONES.id,
                petHabitat = PetHabitat.COZY_ROOM.id,
                petLevel = 2
            ),
            SyncedUserProfile(
                userId = "u_309",
                tagCode = "309",
                displayName = "Valeria Gómez",
                email = "valeria.gomez@habitocto.app",
                avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
                score = 980,
                completedHabits = 8,
                currentStreak = 4,
                petType = PetType.OCTO.id,
                petAccessory = PetAccessory.SUNGLASSES.id,
                petHabitat = PetHabitat.AQUARIUM.id,
                petLevel = 1
            ),
            SyncedUserProfile(
                userId = "u_412",
                tagCode = "412",
                displayName = "Mateo Silva",
                email = "mateo.silva@habitocto.app",
                avatarUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
                score = 740,
                completedHabits = 6,
                currentStreak = 3,
                petType = PetType.PANDA.id,
                petAccessory = PetAccessory.SUNGLASSES.id,
                petHabitat = PetHabitat.AQUARIUM.id,
                petLevel = 1
            ),
            SyncedUserProfile(
                userId = "u_555",
                tagCode = "555",
                displayName = "Diego Morales",
                email = "diego.morales@habitocto.app",
                avatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200",
                score = 1600,
                completedHabits = 13,
                currentStreak = 6,
                petType = PetType.DRAGON.id,
                petAccessory = PetAccessory.WIZARD_HAT.id,
                petHabitat = PetHabitat.SPACE_STATION.id,
                petLevel = 2
            ),
            SyncedUserProfile(
                userId = "u_888",
                tagCode = "888",
                displayName = "Camila Vargas",
                email = "camila.vargas@habitocto.app",
                avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                score = 2100,
                completedHabits = 17,
                currentStreak = 8,
                petType = PetType.FOX.id,
                petAccessory = PetAccessory.GOLD_CROWN.id,
                petHabitat = PetHabitat.SPACE_STATION.id,
                petLevel = 2
            )
        )
        synchronized(publishedDirectory) {
            initialProfiles.forEach { publishedDirectory[it.normalizedTag] = it }
        }
    }

    private fun loadPersistedDirectory() {
        runCatching {
            val raw = prefs.getString(KEY_PUBLISHED_DIRECTORY, null) ?: return
            val arr = JSONArray(raw)
            synchronized(publishedDirectory) {
                for (i in 0 until arr.length()) {
                    val profile = SyncedUserProfile.fromJson(arr.getJSONObject(i))
                    publishedDirectory[profile.normalizedTag] = profile
                }
            }
        }
    }

    private fun savePublishedDirectory() {
        runCatching {
            val arr = JSONArray()
            synchronized(publishedDirectory) {
                publishedDirectory.values.forEach { arr.put(it.toJson()) }
            }
            prefs.edit().putString(KEY_PUBLISHED_DIRECTORY, arr.toString()).apply()
        }
    }

    private fun loadPersistedFriends() {
        runCatching {
            val raw = prefs.getString(KEY_SYNCED_FRIENDS, null)
            if (!raw.isNullOrBlank()) {
                val arr = JSONArray(raw)
                val list = mutableListOf<SyncedUserProfile>()
                for (i in 0 until arr.length()) {
                    list.add(SyncedUserProfile.fromJson(arr.getJSONObject(i)))
                }
                _syncedFriends.value = list
            } else {
                // Amigos iniciales sincronizados con sus códigos de 3 dígitos (#101, #204, #309, #412)
                val defaults = listOfNotNull(
                    publishedDirectory["101"],
                    publishedDirectory["204"],
                    publishedDirectory["309"],
                    publishedDirectory["412"]
                )
                _syncedFriends.value = defaults
                savePersistedFriends(defaults)
            }
        }
    }

    private fun registerSyncedFriend(profile: SyncedUserProfile) {
        synchronized(publishedDirectory) {
            publishedDirectory[profile.normalizedTag] = profile
        }
        savePublishedDirectory()

        val updated = (_syncedFriends.value.filterNot { it.normalizedTag == profile.normalizedTag } + profile)
            .sortedByDescending { it.score }
        _syncedFriends.value = updated
        savePersistedFriends(updated)
    }

    private fun savePersistedFriends(list: List<SyncedUserProfile>) {
        runCatching {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            prefs.edit().putString(KEY_SYNCED_FRIENDS, arr.toString()).apply()
        }
    }

    /**
     * Devuelve el perfil en caché real (sin generar nombres sintéticos) si el #Tag ya fue consultado
     * o si ya pertenece a la lista de amigos sincronizados.
     */
    fun getCachedProfileByTag(tagCode: String): SyncedUserProfile? {
        val clean = normalizeTagCode(tagCode)
        return _syncedFriends.value.firstOrNull { it.normalizedTag == clean }
            ?: synchronized(publishedDirectory) { publishedDirectory[clean] }
    }

    /**
     * Consulta directamente Firebase Firestore y el relay REST para obtener el nombre, foto y puntaje
     * reales registrados bajo el código `#XXX`. Nunca inventa nombres aleatorios cuando no hay respuesta.
     */
    suspend fun lookupProfileByTagRemote(tagCode: String): SyncedUserProfile? = withContext(Dispatchers.IO) {
        val clean = normalizeTagCode(tagCode)
        val remote = (if (isFirebaseInitialized()) fetchFromFirebaseFirestore(clean) else null)
            ?: fetchFromSharedRestRelay(clean)
        if (remote != null) {
            synchronized(publishedDirectory) {
                publishedDirectory[clean] = remote
            }
            savePublishedDirectory()
            if (_syncedFriends.value.any { it.normalizedTag == clean }) {
                val updatedFriends = _syncedFriends.value.map {
                    if (it.normalizedTag == clean) remote else it
                }
                _syncedFriends.value = updatedFriends
                savePersistedFriends(updatedFriends)
            }
            remote
        } else {
            getCachedProfileByTag(clean)
        }
    }

    /**
     * Resuelve cualquier código de 3 dígitos (`#100` a `#999`). Si ya fue publicado por un dispositivo,
     * devuelve ese perfil; si no, genera un perfil determinista coherente para ese número `#XXX`.
     */
    fun resolveProfileByTag(tagCode: String): SyncedUserProfile {
        val clean = normalizeTagCode(tagCode)
        return getCachedProfileByTag(clean) ?: getDeterministicProfileForTag(clean)
    }

    // ============================================================================
    // FEED COMPARTIDO ENTRE AMIGOS EN LA NUBE (POSTS, COMENTARIOS Y LIKES)
    // ============================================================================

    private data class RemoteFeedBucketPayload(
        val posts: List<SyncedFeedPost>,
        val deletedPostIds: Set<String>
    )

    /** Publica una nueva entrada en el feed compartido y la sincroniza en Firebase/REST por #Tag. */
    suspend fun publishCloudFeedPost(
        postId: String,
        habitTitle: String,
        caption: String,
        imageUri: String?
    ): SyncedFeedPost? = withContext(Dispatchers.IO) {
        val session = _currentSession.value ?: return@withContext null
        val myTag = session.normalizedTag
        val now = System.currentTimeMillis()
        val newPost = SyncedFeedPost(
            id = postId,
            authorTag = myTag,
            authorName = "${session.displayName} ${session.formattedTag}",
            authorAvatarUrl = session.avatarUrl,
            habitTitle = habitTitle,
            caption = caption,
            imageUri = imageUri?.trim()?.takeIf { it.isNotBlank() && !AvatarCatalog.isBrokenUrl(it) },
            likesCount = 0,
            likedByTags = emptyList(),
            comments = emptyList(),
            createdAtMillis = now
        )
        val merged = (_syncedFeedPosts.value.filterNot { it.id == newPost.id } + newPost)
            .sortedByDescending { it.createdAtMillis }
            .take(50)
        _syncedFeedPosts.value = merged
        savePersistedFeedPosts(merged)

        val myBucket = merged.filter { it.authorTag == myTag }.take(15)
        val deletedSnapshot = loadDeletedPostIds()
        if (isFirebaseInitialized()) {
            pushFeedBucketToFirestore(myTag, myBucket, deletedSnapshot)
            pushPostUpdateToSharedFeedFirestore(newPost)
        }
        pushFeedBucketToRestRelay(myTag, myBucket, deletedSnapshot)
        newPost
    }

    /** Agrega un comentario único al post (sin duplicar por id o texto repetido inmediato). */
    suspend fun addCommentToCloudFeedPost(
        postId: String,
        commentId: String,
        text: String
    ) = withContext(Dispatchers.IO) {
        if (postId in loadDeletedPostIds()) return@withContext
        val session = _currentSession.value ?: return@withContext
        val cleanText = text.trim()
        if (cleanText.isEmpty()) return@withContext
        val comment = SyncedFeedComment(
            id = commentId,
            authorTag = session.normalizedTag,
            authorName = session.displayName,
            text = cleanText,
            createdAtMillis = System.currentTimeMillis()
        )
        var targetAuthorTag: String? = null
        var updatedTargetPost: SyncedFeedPost? = null
        val updated = _syncedFeedPosts.value.map { post ->
            if (post.id == postId) {
                targetAuthorTag = post.authorTag
                val alreadyExists = post.comments.any {
                    it.id == comment.id || (it.authorTag == comment.authorTag && it.text.trim() == cleanText)
                }
                val nextPost = if (alreadyExists) post else post.copy(comments = post.comments + comment)
                updatedTargetPost = nextPost
                nextPost
            } else {
                post
            }
        }
        _syncedFeedPosts.value = updated
        savePersistedFeedPosts(updated)
        targetAuthorTag?.let { authorTag ->
            val bucket = updated.filter { it.authorTag == authorTag }.take(15)
            if (isFirebaseInitialized()) {
                pushFeedBucketToFirestore(authorTag, bucket)
                updatedTargetPost?.let { pushPostUpdateToSharedFeedFirestore(it) }
            }
            pushFeedBucketToRestRelay(authorTag, bucket)
        }
    }

    /** Alterna Me Gusta en una publicación compartida y sincroniza con el dueño del post. */
    suspend fun toggleLikeCloudFeedPost(postId: String, likesCountOverride: Int? = null) = withContext(Dispatchers.IO) {
        if (postId in loadDeletedPostIds()) return@withContext
        val myTag = _currentSession.value?.normalizedTag ?: return@withContext
        var targetAuthorTag: String? = null
        var updatedTargetPost: SyncedFeedPost? = null
        val updated = _syncedFeedPosts.value.map { post ->
            if (post.id == postId) {
                targetAuthorTag = post.authorTag
                val hasLiked = myTag in post.likedByTags
                val nextTags = if (hasLiked) post.likedByTags - myTag else post.likedByTags + myTag
                val nextCount = likesCountOverride ?: if (hasLiked) maxOf(0, post.likesCount - 1) else post.likesCount + 1
                val nextPost = post.copy(likesCount = nextCount, likedByTags = nextTags)
                updatedTargetPost = nextPost
                nextPost
            } else {
                post
            }
        }
        _syncedFeedPosts.value = updated
        savePersistedFeedPosts(updated)
        targetAuthorTag?.let { authorTag ->
            val bucket = updated.filter { it.authorTag == authorTag }.take(15)
            if (isFirebaseInitialized()) {
                pushFeedBucketToFirestore(authorTag, bucket)
                updatedTargetPost?.let { pushPostUpdateToSharedFeedFirestore(it) }
            }
            pushFeedBucketToRestRelay(authorTag, bucket)
        }
    }

    /** Elimina una publicación propia de la nube (con tombstone sincronizado) para que desaparezca para todos. */
    suspend fun deleteCloudFeedPost(
        postId: String,
        captionHint: String? = null,
        createdAtMillisHint: Long? = null
    ) = withContext(Dispatchers.IO) {
        val myTag = _currentSession.value?.normalizedTag ?: return@withContext
        val normalizedCaption = captionHint?.trim()?.takeIf { it.isNotEmpty() }
        val matchingIds = _syncedFeedPosts.value
            .filter { post ->
                post.authorTag == myTag && (
                    post.id == postId || (
                        normalizedCaption != null &&
                            post.caption.trim() == normalizedCaption &&
                            (createdAtMillisHint == null || abs(post.createdAtMillis - createdAtMillisHint) < 120_000L)
                        )
                    )
            }
            .map { it.id }

        val allDeletedIds = (matchingIds + postId)
            .map { it.trim() }
            .filter { it.isNotEmpty() }
            .toSet()

        markPostIdsDeleted(allDeletedIds)
        val updated = _syncedFeedPosts.value.filterNot { it.id in allDeletedIds }
        _syncedFeedPosts.value = updated
        savePersistedFeedPosts(updated)

        val myBucket = updated.filter { it.authorTag == myTag }.take(15)
        val deletedSnapshot = loadDeletedPostIds()
        if (isFirebaseInitialized()) {
            pushFeedBucketToFirestore(myTag, myBucket, deletedSnapshot)
            removePostsFromSharedFeedFirestore(allDeletedIds)
        }
        pushFeedBucketToRestRelay(myTag, myBucket, deletedSnapshot)
    }

    /** Reconcilia publicaciones cacheadas con los buckets remotos y tombstones (`deletedPostIds`). */
    private fun reconcileFeedPosts(
        existingPosts: List<SyncedFeedPost>,
        remoteBucketsByTag: Map<String, RemoteFeedBucketPayload>,
        sharedFeedPayload: RemoteFeedBucketPayload?,
        visibleTags: Set<String>,
        myTag: String?
    ): List<SyncedFeedPost> {
        val discoveredDeletedIds = buildSet {
            remoteBucketsByTag.values.forEach { addAll(it.deletedPostIds) }
            sharedFeedPayload?.let { addAll(it.deletedPostIds) }
        }
        markPostIdsDeleted(discoveredDeletedIds)

        val deletedIds = loadDeletedPostIds() + discoveredDeletedIds
        val authoritativeIdsByTag: Map<String, Set<String>> = remoteBucketsByTag.mapValues { (_, payload) ->
            payload.posts.map { it.id }.toSet()
        }

        fun isPrunedByAuthorBucket(post: SyncedFeedPost): Boolean {
            val normalizedAuthor = normalizeTagCode(post.authorTag)
            val authoritativeIds = authoritativeIdsByTag[normalizedAuthor] ?: return false
            return post.id !in authoritativeIds
        }

        val remoteCollected = mutableMapOf<String, SyncedFeedPost>()
        existingPosts
            .filter { post -> post.id !in deletedIds && !isPrunedByAuthorBucket(post) }
            .forEach { remoteCollected[it.id] = it }

        fun mergePost(remotePost: SyncedFeedPost, enforceAuthorBucket: Boolean) {
            if (remotePost.id in deletedIds) return
            if (enforceAuthorBucket && isPrunedByAuthorBucket(remotePost)) return
            val existing = remoteCollected[remotePost.id]
            if (existing == null) {
                remoteCollected[remotePost.id] = remotePost
            } else {
                val mergedComments = (existing.comments + remotePost.comments)
                    .distinctBy { "${it.authorTag}|${it.text.trim().lowercase()}" }
                    .sortedBy { it.createdAtMillis }
                val mergedLikes = (existing.likedByTags + remotePost.likedByTags).distinct()
                remoteCollected[remotePost.id] = remotePost.copy(
                    authorAvatarUrl = remotePost.authorAvatarUrl?.takeIf { !AvatarCatalog.isBrokenUrl(it) }
                        ?: existing.authorAvatarUrl,
                    imageUri = remotePost.imageUri?.takeIf { !AvatarCatalog.isBrokenUrl(it) }
                        ?: existing.imageUri,
                    likesCount = maxOf(existing.likesCount, remotePost.likesCount, mergedLikes.size),
                    likedByTags = mergedLikes,
                    comments = mergedComments
                )
            }
        }

        remoteBucketsByTag.values.forEach { payload ->
            payload.posts.forEach { post -> mergePost(post, enforceAuthorBucket = false) }
        }

        sharedFeedPayload?.posts?.forEach { post ->
            val postAuthor = normalizeTagCode(post.authorTag)
            if (postAuthor in visibleTags || postAuthor == myTag) {
                mergePost(post, enforceAuthorBucket = true)
            }
        }

        val profilesByTag = (_syncedFriends.value + synchronized(publishedDirectory) { publishedDirectory.values.toList() })
            .associateBy { it.normalizedTag }

        return remoteCollected.values
            .filter { post ->
                val postAuthor = normalizeTagCode(post.authorTag)
                postAuthor in visibleTags && post.id !in deletedIds && !isPrunedByAuthorBucket(post)
            }
            .map { post ->
                val freshProfile = if (post.authorTag == myTag) null else profilesByTag[post.authorTag]
                if (freshProfile != null) {
                    post.copy(
                        authorName = freshProfile.displayNameWithTag,
                        authorAvatarUrl = freshProfile.avatarUrl?.takeIf { !AvatarCatalog.isBrokenUrl(it) }
                            ?: post.authorAvatarUrl
                    )
                } else {
                    post
                }
            }
            .sortedByDescending { it.createdAtMillis }
            .take(50)
    }

    /** Aplica localmente un bucket actualizado de otro autor (incluyendo sus tombstones). */
    fun applyRemoteAuthorBucketUpdate(
        authorTag: String,
        activePosts: List<SyncedFeedPost>,
        deletedPostIds: Set<String> = emptySet()
    ): List<SyncedFeedPost> {
        val normalizedAuthor = normalizeTagCode(authorTag)
        val myTag = _currentSession.value?.normalizedTag
        val visibleTags = (
            _syncedFriends.value.map { it.normalizedTag } +
                _syncedFeedPosts.value.map { normalizeTagCode(it.authorTag) } +
                normalizedAuthor +
                listOfNotNull(myTag)
            ).toSet()
        val mergedList = reconcileFeedPosts(
            existingPosts = _syncedFeedPosts.value,
            remoteBucketsByTag = mapOf(
                normalizedAuthor to RemoteFeedBucketPayload(
                    posts = activePosts,
                    deletedPostIds = deletedPostIds
                )
            ),
            sharedFeedPayload = null,
            visibleTags = visibleTags,
            myTag = myTag
        )
        _syncedFeedPosts.value = mergedList
        savePersistedFeedPosts(mergedList)
        return mergedList
    }

    /** Indica si un ID de publicación ya está marcado como eliminado (tombstone local/sincronizado). */
    fun isPostDeleted(postId: String): Boolean = postId in loadDeletedPostIds()

    /** Sincroniza publicaciones del usuario actual y de todos sus amigos agregados o mutuos. */
    suspend fun refreshFriendsFeed(extraFriendTags: Set<String> = emptySet()): List<SyncedFeedPost> = withContext(Dispatchers.IO) {
        val myTag = _currentSession.value?.normalizedTag
        val mutualTags = if (myTag != null && isFirebaseInitialized()) {
            fetchMutualFriendTagsFromFirestore(myTag)
        } else {
            emptyList()
        }
        val friendTags = (_syncedFriends.value.map { it.normalizedTag } + extraFriendTags + mutualTags)
            .filter { isValidTagCode(it) }
            .toSet()
        val tagsToPull = (friendTags + listOfNotNull(myTag)).distinct()
        if (tagsToPull.isEmpty()) return@withContext _syncedFeedPosts.value

        val remoteBucketsByTag = mutableMapOf<String, RemoteFeedBucketPayload>()
        tagsToPull.forEach { tag ->
            val payload = (if (isFirebaseInitialized()) fetchFeedBucketPayloadFromFirestore(tag) else null)
                ?: fetchFeedBucketPayloadFromRestRelay(tag)
            if (payload != null) {
                remoteBucketsByTag[tag] = payload
            }
        }

        val sharedFeedPayload = if (isFirebaseInitialized()) {
            fetchSharedFeedPayloadFromFirestore()
        } else {
            null
        }

        val mergedList = reconcileFeedPosts(
            existingPosts = _syncedFeedPosts.value,
            remoteBucketsByTag = remoteBucketsByTag,
            sharedFeedPayload = sharedFeedPayload,
            visibleTags = tagsToPull.toSet(),
            myTag = myTag
        )
        _syncedFeedPosts.value = mergedList
        savePersistedFeedPosts(mergedList)
        notifySocialInteractions(myTag = myTag, posts = mergedList)
        mergedList
    }

    private fun loadNotifiedSocialEvents(): Set<String> {
        return runCatching {
            val raw = prefs.getString(KEY_NOTIFIED_SOCIAL_EVENTS, null) ?: return@runCatching emptySet()
            val arr = JSONArray(raw)
            buildSet {
                for (i in 0 until arr.length()) add(arr.optString(i))
            }
        }.getOrDefault(emptySet())
    }

    private fun markSocialEventsNotified(newKeys: Collection<String>) {
        if (newKeys.isEmpty()) return
        val updated = (loadNotifiedSocialEvents() + newKeys).toList().takeLast(200)
        prefs.edit().putString(KEY_NOTIFIED_SOCIAL_EVENTS, JSONArray(updated).toString()).apply()
    }

    private fun notifySocialInteractions(myTag: String?, posts: List<SyncedFeedPost>) {
        if (myTag.isNullOrBlank()) return
        val alreadyNotified = loadNotifiedSocialEvents()
        val newlyNotified = mutableListOf<String>()
        val isEn = _currentSession.value?.let { false } ?: false

        posts.filter { it.authorTag == myTag }.forEach { post ->
            post.comments
                .filter { it.authorTag != myTag && it.text.isNotBlank() }
                .forEach { comment ->
                    val eventKey = "comment_${post.id}_${comment.id}"
                    if (eventKey !in alreadyNotified && eventKey !in newlyNotified) {
                        newlyNotified += eventKey
                        if (!isRobolectricTest) {
                            val title = "💬 ${comment.authorName} comentó tu publicación"
                            val body = "En \"${post.habitTitle}\": \"${comment.text}\""
                            HabitNotificationScheduler.sendReminder(
                                context = appContext,
                                title = title,
                                message = body,
                                isEnglish = isEn
                            )
                        }
                    }
                }

            post.likedByTags
                .filter { it != myTag && isValidTagCode(it) }
                .forEach { likerTag ->
                    val eventKey = "like_${post.id}_$likerTag"
                    if (eventKey !in alreadyNotified && eventKey !in newlyNotified) {
                        newlyNotified += eventKey
                        if (!isRobolectricTest) {
                            val likerProfile = getCachedProfileByTag(likerTag)
                            val likerName = likerProfile?.displayNameWithTag ?: "Amigo #$likerTag"
                            val title = "❤️ Nuevo Me Gusta en tu publicación"
                            val body = "A $likerName le gustó tu logro \"${post.habitTitle}\"."
                            HabitNotificationScheduler.sendReminder(
                                context = appContext,
                                title = title,
                                message = body,
                                isEnglish = isEn
                            )
                        }
                    }
                }
        }

        if (newlyNotified.isNotEmpty()) {
            markSocialEventsNotified(newlyNotified)
        }
    }

    private fun seedPredefinedFriendsFeed() {
        if (_syncedFeedPosts.value.isNotEmpty()) return
        val now = System.currentTimeMillis()
        val seedPosts = listOf(
            SyncedFeedPost(
                id = "cloud_seed_101",
                authorTag = "101",
                authorName = "Sofía Mendoza #101",
                authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
                habitTitle = "Meditación matutina 10m",
                caption = "¡Día 7 seguido cuidando mi enfoque y subiendo al ranking de amigos! 🧘‍♀️",
                likesCount = 5,
                comments = emptyList(),
                createdAtMillis = now - 3_600_000L
            ),
            SyncedFeedPost(
                id = "cloud_seed_204",
                authorTag = "204",
                authorName = "Carlos Ruiz #204",
                authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
                habitTitle = "Correr 5km",
                caption = "Cerrando la ruta vespertina con buena energía 🏃‍♂️💨",
                likesCount = 4,
                comments = emptyList(),
                createdAtMillis = now - 7_200_000L
            )
        )
        _syncedFeedPosts.value = seedPosts
        savePersistedFeedPosts(seedPosts)
    }

    private fun loadPersistedFeedPosts() {
        runCatching {
            val raw = prefs.getString(KEY_SYNCED_FEED_POSTS, null)
            if (!raw.isNullOrBlank()) {
                val arr = JSONArray(raw)
                val list = mutableListOf<SyncedFeedPost>()
                for (i in 0 until arr.length()) {
                    list += SyncedFeedPost.fromJson(arr.getJSONObject(i))
                }
                _syncedFeedPosts.value = list.sortedByDescending { it.createdAtMillis }
            }
        }
    }

    private fun savePersistedFeedPosts(list: List<SyncedFeedPost>) {
        runCatching {
            val arr = JSONArray()
            list.forEach { arr.put(it.toJson()) }
            prefs.edit().putString(KEY_SYNCED_FEED_POSTS, arr.toString()).apply()
        }
    }

    private fun parseStringSetFromJsonArray(arr: JSONArray?): Set<String> {
        if (arr == null) return emptySet()
        return buildSet {
            for (i in 0 until arr.length()) {
                arr.optString(i).trim().takeIf { it.isNotEmpty() }?.let(::add)
            }
        }
    }

    private fun parseFeedPostsFromJsonArray(arr: JSONArray?): List<SyncedFeedPost> {
        if (arr == null) return emptyList()
        return buildList {
            for (i in 0 until arr.length()) {
                arr.optJSONObject(i)?.let { add(SyncedFeedPost.fromJson(it)) }
            }
        }
    }

    private fun loadDeletedPostIds(): Set<String> {
        return runCatching {
            val raw = prefs.getString(KEY_DELETED_FEED_IDS, null) ?: return@runCatching emptySet()
            parseStringSetFromJsonArray(JSONArray(raw))
        }.getOrDefault(emptySet())
    }

    private fun markPostIdsDeleted(postIds: Collection<String>) {
        val cleanIds = postIds.map { it.trim() }.filter { it.isNotEmpty() }
        if (cleanIds.isEmpty()) return
        val current = (loadDeletedPostIds() + cleanIds).toList().takeLast(120)
        val arr = JSONArray()
        current.forEach { arr.put(it) }
        prefs.edit().putString(KEY_DELETED_FEED_IDS, arr.toString()).apply()
    }

    private fun markPostIdDeleted(postId: String) {
        markPostIdsDeleted(listOf(postId))
    }

    private fun pushMutualFriendshipToFirestore(myTag: String, friendTag: String) {
        addFriendLinkToFirestoreDoc(targetOwnerTag = friendTag, friendTagToAdd = myTag)
        addFriendLinkToFirestoreDoc(targetOwnerTag = myTag, friendTagToAdd = friendTag)
    }

    private fun syncOwnFriendLinksToFirestore(myTag: String, friendTags: List<String>) {
        val projectId = getFirebaseProjectId() ?: return
        runCatching {
            val remoteExisting = fetchMutualFriendTagsFromFirestore(myTag)
            val merged = (remoteExisting + friendTags).filter { isValidTagCode(it) && it != myTag }.distinct()
            val url =
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/habitocto_users/friends_$myTag"
            val fields = JSONObject().apply {
                put("docType", JSONObject().put("stringValue", "friend_links"))
                put("ownerTag", JSONObject().put("stringValue", myTag))
                put("friendTagsJson", JSONObject().put("stringValue", JSONArray(merged).toString()))
                put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
            }
            val bodyJson = JSONObject().put("fields", fields).toString()
            val req = Request.Builder().url(url).patch(bodyJson.toRequestBody(JSON_MEDIA_TYPE)).build()
            httpClient.newCall(req).execute().close()
        }
    }

    private fun addFriendLinkToFirestoreDoc(targetOwnerTag: String, friendTagToAdd: String) {
        val projectId = getFirebaseProjectId() ?: return
        runCatching {
            val existing = fetchMutualFriendTagsFromFirestore(targetOwnerTag)
            val updated = (existing + friendTagToAdd).filter { isValidTagCode(it) && it != targetOwnerTag }.distinct()
            val url =
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/habitocto_users/friends_$targetOwnerTag"
            val fields = JSONObject().apply {
                put("docType", JSONObject().put("stringValue", "friend_links"))
                put("ownerTag", JSONObject().put("stringValue", targetOwnerTag))
                put("friendTagsJson", JSONObject().put("stringValue", JSONArray(updated).toString()))
                put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
            }
            val bodyJson = JSONObject().put("fields", fields).toString()
            val req = Request.Builder().url(url).patch(bodyJson.toRequestBody(JSON_MEDIA_TYPE)).build()
            httpClient.newCall(req).execute().close()
        }
    }

    private fun fetchMutualFriendTagsFromFirestore(tagCode: String): List<String> {
        val projectId = getFirebaseProjectId() ?: return emptyList()
        return runCatching {
            val url =
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/habitocto_users/friends_$tagCode"
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use emptyList()
                val body = resp.body?.string() ?: return@use emptyList()
                val fields = JSONObject(body).optJSONObject("fields") ?: return@use emptyList()
                val rawArr = fields.optJSONObject("friendTagsJson")?.optString("stringValue") ?: return@use emptyList()
                val arr = JSONArray(rawArr)
                buildList<String> {
                    for (i in 0 until arr.length()) {
                        val candidate = arr.optString(i).trim()
                        if (isValidTagCode(candidate)) add(normalizeTagCode(candidate))
                    }
                }
            }
        }.getOrDefault(emptyList())
    }

    private fun fetchFirestoreFeedPayload(documentId: String): RemoteFeedBucketPayload? {
        val projectId = getFirebaseProjectId() ?: return null
        return runCatching {
            val url =
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/habitocto_users/$documentId"
            val req = Request.Builder().url(url).get().build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val body = resp.body?.string() ?: return@use null
                val fields = JSONObject(body).optJSONObject("fields") ?: return@use null
                val postsField = fields.optJSONObject("postsJson") ?: return@use null
                val rawArr = postsField.optString("stringValue", "[]").ifBlank { "[]" }
                val rawDeleted = fields.optJSONObject("deletedPostIdsJson")?.optString("stringValue")
                val deletedIds = rawDeleted
                    ?.takeIf { it.isNotBlank() }
                    ?.let { parseStringSetFromJsonArray(JSONArray(it)) }
                    .orEmpty()
                RemoteFeedBucketPayload(
                    posts = parseFeedPostsFromJsonArray(JSONArray(rawArr)),
                    deletedPostIds = deletedIds
                )
            }
        }.getOrNull()
    }

    private fun patchFirestoreFeedDocument(
        documentId: String,
        docType: String,
        posts: List<SyncedFeedPost>,
        deletedPostIds: Set<String>,
        tagCode: String? = null
    ): Boolean {
        val projectId = getFirebaseProjectId() ?: return false
        return runCatching {
            val url =
                "https://firestore.googleapis.com/v1/projects/$projectId/databases/(default)/documents/habitocto_users/$documentId"
            val arr = JSONArray()
            posts.forEach { arr.put(it.toJson()) }
            val deletedArr = JSONArray(deletedPostIds.toList().takeLast(120))
            val fields = JSONObject().apply {
                put("docType", JSONObject().put("stringValue", docType))
                if (tagCode != null) {
                    put("tagCode", JSONObject().put("stringValue", tagCode))
                }
                put("postsJson", JSONObject().put("stringValue", arr.toString()))
                put("deletedPostIdsJson", JSONObject().put("stringValue", deletedArr.toString()))
                put("updatedAt", JSONObject().put("integerValue", System.currentTimeMillis().toString()))
            }
            val bodyJson = JSONObject().put("fields", fields).toString()
            val req = Request.Builder().url(url).patch(bodyJson.toRequestBody(JSON_MEDIA_TYPE)).build()
            httpClient.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun pushFeedBucketToFirestore(
        tagCode: String,
        posts: List<SyncedFeedPost>,
        deletedPostIds: Set<String> = emptySet()
    ): Boolean {
        val existingPayload = fetchFeedBucketPayloadFromFirestore(tagCode)
        val mergedDeleted = (existingPayload?.deletedPostIds.orEmpty() + deletedPostIds + loadDeletedPostIds())
            .toList()
            .takeLast(120)
            .toSet()
        val cleanPosts = posts.filterNot { it.id in mergedDeleted }
        return patchFirestoreFeedDocument(
            documentId = "feed_$tagCode",
            docType = "feed_bucket",
            posts = cleanPosts,
            deletedPostIds = mergedDeleted,
            tagCode = tagCode
        )
    }

    private fun fetchFeedBucketPayloadFromFirestore(tagCode: String): RemoteFeedBucketPayload? =
        fetchFirestoreFeedPayload("feed_$tagCode")

    private fun fetchFeedBucketFromFirestore(tagCode: String): List<SyncedFeedPost>? =
        fetchFeedBucketPayloadFromFirestore(tagCode)?.posts

    private fun fetchSharedFeedPayloadFromFirestore(): RemoteFeedBucketPayload? =
        fetchFirestoreFeedPayload("feed_shared")

    private fun fetchSharedFeedPostsFromFirestore(): List<SyncedFeedPost>? =
        fetchSharedFeedPayloadFromFirestore()?.posts

    private fun pushPostUpdateToSharedFeedFirestore(changedPost: SyncedFeedPost): Boolean {
        val remotePayload = fetchSharedFeedPayloadFromFirestore()
        val mergedDeleted = (remotePayload?.deletedPostIds.orEmpty() + loadDeletedPostIds())
            .toList()
            .takeLast(120)
            .toSet()
        if (changedPost.id in mergedDeleted) return false
        val remoteList = remotePayload?.posts.orEmpty()
        val merged = (remoteList.filterNot { it.id == changedPost.id || it.id in mergedDeleted } + changedPost)
            .sortedByDescending { it.createdAtMillis }
            .take(30)
        return patchFirestoreFeedDocument(
            documentId = "feed_shared",
            docType = "shared_feed",
            posts = merged,
            deletedPostIds = mergedDeleted
        )
    }

    private fun removePostsFromSharedFeedFirestore(postIdsToRemove: Set<String>): Boolean {
        val remotePayload = fetchSharedFeedPayloadFromFirestore()
        val mergedDeleted = (remotePayload?.deletedPostIds.orEmpty() + postIdsToRemove + loadDeletedPostIds())
            .toList()
            .takeLast(120)
            .toSet()
        val merged = remotePayload?.posts.orEmpty().filterNot { it.id in mergedDeleted }
        return patchFirestoreFeedDocument(
            documentId = "feed_shared",
            docType = "shared_feed",
            posts = merged,
            deletedPostIds = mergedDeleted
        )
    }

    private fun removePostFromSharedFeedFirestore(postId: String): Boolean =
        removePostsFromSharedFeedFirestore(setOf(postId))

    private fun pushFeedBucketToRestRelay(
        tagCode: String,
        posts: List<SyncedFeedPost>,
        deletedPostIds: Set<String> = loadDeletedPostIds()
    ): Boolean {
        val customEndpoint = prefs.getString(KEY_CUSTOM_REST_URL, null)
        if (isRobolectricTest && customEndpoint.isNullOrBlank()) return false
        val endpoint = customEndpoint ?: DEFAULT_REST_BASE_URL
        val existingDeleted = if (tagCode != _currentSession.value?.normalizedTag) {
            fetchFeedBucketPayloadFromRestRelay(tagCode)?.deletedPostIds.orEmpty()
        } else {
            emptySet()
        }
        val mergedDeleted = (existingDeleted + deletedPostIds + loadDeletedPostIds())
            .toList()
            .takeLast(120)
            .toSet()
        val cleanPosts = posts.filterNot { it.id in mergedDeleted }
        return runCatching {
            val arr = JSONArray()
            cleanPosts.forEach { arr.put(it.toJson()) }
            val deletedArr = JSONArray(mergedDeleted.toList())
            val payloadJson = JSONObject()
                .put("posts", arr)
                .put("deletedPostIds", deletedArr)
            val req = Request.Builder()
                .url("$endpoint/habitocto_feed_$tagCode")
                .put(payloadJson.toString().toRequestBody(JSON_MEDIA_TYPE))
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { it.isSuccessful }
        }.getOrDefault(false)
    }

    private fun fetchFeedBucketPayloadFromRestRelay(tagCode: String): RemoteFeedBucketPayload? {
        val customEndpoint = prefs.getString(KEY_CUSTOM_REST_URL, null)
        if (isRobolectricTest && customEndpoint.isNullOrBlank()) return null
        val endpoint = customEndpoint ?: DEFAULT_REST_BASE_URL
        return runCatching {
            val req = Request.Builder()
                .url("$endpoint/habitocto_feed_$tagCode")
                .get()
                .header("Accept", "application/json")
                .build()
            httpClient.newCall(req).execute().use { resp ->
                if (!resp.isSuccessful) return@use null
                val raw = resp.body?.string() ?: return@use null
                val json = JSONObject(raw)
                val postsArr = json.optJSONArray("posts") ?: return@use null
                val deletedArr = json.optJSONArray("deletedPostIds")
                RemoteFeedBucketPayload(
                    posts = parseFeedPostsFromJsonArray(postsArr),
                    deletedPostIds = parseStringSetFromJsonArray(deletedArr)
                )
            }
        }.getOrNull()
    }

    private fun fetchFeedBucketFromRestRelay(tagCode: String): List<SyncedFeedPost>? =
        fetchFeedBucketPayloadFromRestRelay(tagCode)?.posts

    // ============================================================================
    // FELICIDAD DE MASCOTA DESDE EL MINIJUEGO DEL SANTUARIO (SIN OTORGAR XP)
    // ============================================================================

    /** Incrementa únicamente la felicidad de la mascota al jugar el minijuego del Santuario (0 XP añadida). */
    fun boostPetHappinessFromMiniGame(deltaHappiness: Int): Int {
        val updated = (_petHappinessBonus.value + deltaHappiness).coerceIn(0, 35)
        _petHappinessBonus.value = updated
        prefs.edit().putInt(KEY_PET_HAPPINESS_BONUS, updated).apply()
        return updated
    }

    private fun loadPetHappinessBonus() {
        _petHappinessBonus.value = prefs.getInt(KEY_PET_HAPPINESS_BONUS, 0).coerceIn(0, 35)
    }

    companion object {
        private const val PREFS_NAME = "habitocto_cloud_sync_prefs"
        private const val KEY_ACTIVE_SESSION = "active_auth_session"
        private const val KEY_SAVED_SESSIONS = "saved_auth_sessions"
        private const val KEY_SYNCED_FRIENDS = "synced_friends_list"
        private const val KEY_PUBLISHED_DIRECTORY = "published_tag_directory"
        private const val KEY_SYNCED_FEED_POSTS = "synced_shared_feed_posts"
        private const val KEY_DELETED_FEED_IDS = "deleted_shared_feed_ids"
        private const val KEY_NOTIFIED_SOCIAL_EVENTS = "notified_social_feed_events"
        private const val KEY_PET_HAPPINESS_BONUS = "pet_minigame_happiness_bonus"
        private const val KEY_CUSTOM_REST_URL = "custom_rest_endpoint"
        private const val DEFAULT_FIREBASE_PROJECT_ID = "habitocto-suchini-2026"
        private const val DEFAULT_REST_BASE_URL = "https://kvdb.io/A6Xy9HabitOctoSyncV1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

        /** Hash SHA-256 seguro con sal para credenciales de usuario (nunca guarda contraseña plana). */
        fun hashPassword(password: String, salt: String = "habitocto_v1"): String {
            val digest = MessageDigest.getInstance("SHA-256")
            val bytes = digest.digest("$salt|${password}".toByteArray(Charsets.UTF_8))
            return bytes.joinToString("") { "%02x".format(it) }
        }

        private val DETERMINISTIC_FIRST_NAMES = listOf(
            "Alejandro", "Lucía", "Sebastián", "Mariana", "Nicolás",
            "Gabriela", "Daniel", "Fernanda", "Andrés", "Paula",
            "Javier", "Elena", "Rodrigo", "Natalia", "Emilio",
            "Ximena", "Santiago", "Regina", "Joaquín", "Victoria"
        )

        private val DETERMINISTIC_LAST_NAMES = listOf(
            "Castillo", "Morales", "Herrera", "Ortiz", "Delgado",
            "Castro", "Romero", "Vargas", "Reyes", "Méndez",
            "Guzmán", "Pineda", "Salazar", "Aguilar", "Navarro"
        )

        @Volatile
        private var instance: CloudSyncManager? = null

        /**
         * Obtiene la instancia singleton de [CloudSyncManager] asociada al contexto de la aplicación.
         */
        fun getInstance(context: Context): CloudSyncManager {
            val appCtx = context.applicationContext
            return instance?.takeIf { it.appContext === appCtx } ?: synchronized(this) {
                instance?.takeIf { it.appContext === appCtx }
                    ?: CloudSyncManager(appCtx).also { instance = it }
            }
        }

        /**
         * Normaliza cualquier cadena a un código válido de 3 dígitos entre "100" y "999".
         */
        fun normalizeTagCode(raw: String): String {
            val digits = raw.filter { it.isDigit() }
            if (digits.length >= 3) {
                val three = digits.take(3).toIntOrNull() ?: 777
                val clamped = if (three < 100) three + 100 else three.coerceIn(100, 999)
                return clamped.toString()
            }
            val num = digits.toIntOrNull()
            if (num != null && num > 0) {
                return (100 + (num % 900)).toString()
            }
            return generateDeterministicTag(raw)
        }

        /**
         * Verifica si el texto ingresado contiene un código de 3 dígitos válido (#100 a #999).
         */
        fun isValidTagCode(raw: String): Boolean {
            val cleaned = raw.trim().removePrefix("#").trim()
            val number = cleaned.toIntOrNull() ?: return false
            return number in 100..999
        }

        /**
         * Genera un código de 3 dígitos determinista (#100 a #999) a partir del nombre y correo.
         */
        fun generateDeterministicTag(name: String, email: String = ""): String {
            val trimmed = name.trim()
            if (trimmed.equals("Suchini", ignoreCase = true) && email.isBlank()) {
                return "777"
            }
            val seed = "${trimmed.lowercase()}|${email.trim().lowercase()}"
            val hash = abs(seed.hashCode())
            val code = 100 + (hash % 900)
            return code.toString()
        }

        /**
         * Genera un código aleatorio de 3 dígitos entre "100" y "999".
         */
        fun generateRandomTag(): String = (100..999).random().toString()

        /**
         * Construye un perfil determinista para cualquier código `#100` a `#999` para que
         * ningún código ingresado por el usuario falle o quede vacío.
         */
        fun getDeterministicProfileForTag(tagCode: String): SyncedUserProfile {
            val clean = normalizeTagCode(tagCode)
            val num = clean.toIntOrNull() ?: 777
            val firstName = DETERMINISTIC_FIRST_NAMES[num % DETERMINISTIC_FIRST_NAMES.size]
            val lastName = DETERMINISTIC_LAST_NAMES[(num / 7) % DETERMINISTIC_LAST_NAMES.size]
            val habits = 4 + (num % 16)
            val streak = 1 + (num % 12)
            val score = (habits * 100) + (streak * 50)
            val pets = PetType.values()
            val accessories = PetAccessory.values()
            val habitats = PetHabitat.values()

            return SyncedUserProfile(
                userId = "user_$clean",
                tagCode = clean,
                displayName = "$firstName $lastName",
                email = "${firstName.lowercase()}.${lastName.lowercase()}@habitocto.app",
                score = score,
                completedHabits = habits,
                currentStreak = streak,
                petType = pets[num % pets.size].id,
                petAccessory = accessories[(num / 3) % accessories.size].id,
                petHabitat = habitats[(num / 5) % habitats.size].id,
                petLevel = maxOf(1, habits / 10 + 1)
            )
        }

        /**
         * Genera un código compacto de sincronización instantánea que codifica el `#XXX` y estadísticas
         * para poder pegarlo directamente en otro dispositivo sin depender de configuración externa.
         * Formato: `OCTO#<tag>|<nombre>|<score>|<habitos>|<racha>|<mascota>|<accesorio>`
         */
        fun buildShareSyncToken(profile: SyncedUserProfile): String {
            val safeName = profile.displayName.replace("|", " ").trim()
            return "OCTO#${profile.normalizedTag}|$safeName|${profile.score}|${profile.completedHabits}|${profile.currentStreak}|${profile.petType}|${profile.petAccessory}"
        }

        /**
         * Decodifica un token `OCTO#...` o formato corto `#777:Nombre:Habitos:Racha`.
         */
        fun parseShareSyncToken(raw: String): SyncedUserProfile? {
            val text = raw.trim()
            val tokenMatch = Regex("OCTO#(\\d{3})\\|([^|\\n]+)\\|(\\d+)\\|(\\d+)\\|(\\d+)(?:\\|([^|\\s]+))?(?:\\|([^|\\s]+))?")
                .find(text)
            if (tokenMatch != null) {
                val tag = normalizeTagCode(tokenMatch.groupValues[1])
                val name = tokenMatch.groupValues[2].trim()
                val score = tokenMatch.groupValues[3].toIntOrNull() ?: 0
                val habits = tokenMatch.groupValues[4].toIntOrNull() ?: 0
                val streak = tokenMatch.groupValues[5].toIntOrNull() ?: 0
                val pet = tokenMatch.groupValues.getOrNull(6)?.takeIf { it.isNotBlank() } ?: PetType.OCTO.id
                val acc = tokenMatch.groupValues.getOrNull(7)?.takeIf { it.isNotBlank() } ?: PetAccessory.NONE.id
                return SyncedUserProfile(
                    userId = "user_$tag",
                    tagCode = tag,
                    displayName = name,
                    score = if (score > 0) score else (habits * 100) + (streak * 50),
                    completedHabits = habits,
                    currentStreak = streak,
                    petType = pet,
                    petAccessory = acc,
                    petLevel = maxOf(1, habits / 10 + 1)
                )
            }
            return null
        }

        /**
         * Copia el código `#XXX` (o el token completo) al portapapeles del sistema y muestra confirmación.
         */
        fun copyTagToClipboard(
            context: Context,
            tagCode: String,
            userName: String? = null,
            includeSyncToken: Boolean = false,
            completedHabits: Int = 0,
            currentStreak: Int = 0
        ): String {
            val cleanTag = normalizeTagCode(tagCode)
            val textToCopy = if (includeSyncToken) {
                val profile = SyncedUserProfile(
                    userId = "user_$cleanTag",
                    tagCode = cleanTag,
                    displayName = userName ?: "Suchini",
                    score = (completedHabits * 100) + (currentStreak * 50),
                    completedHabits = completedHabits,
                    currentStreak = currentStreak
                )
                buildShareSyncToken(profile)
            } else {
                "#$cleanTag"
            }

            runCatching {
                val clipboard = context.getSystemService(Context.CLIPBOARD_SERVICE) as? ClipboardManager
                val clip = ClipData.newPlainText("HabitOcto Tag", textToCopy)
                clipboard?.setPrimaryClip(clip)
                Toast.makeText(
                    context,
                    "Código #$cleanTag copiado al portapapeles",
                    Toast.LENGTH_SHORT
                ).show()
            }
            return textToCopy
        }

        /**
         * Abre la hoja nativa de Android (`Intent.ACTION_SEND`) para compartir el código `#XXX` y el
         * token de sincronización instantánea por WhatsApp, Telegram o cualquier app de mensajería.
         */
        fun shareProfileWithFriends(
            context: Context,
            userName: String,
            tagCode: String,
            completedHabits: Int = 0,
            currentStreak: Int = 0,
            petType: PetType = PetType.OCTO,
            petAccessory: PetAccessory = PetAccessory.NONE
        ) {
            val cleanTag = normalizeTagCode(tagCode)
            val score = (completedHabits * 100) + (currentStreak * 50)
            val profile = SyncedUserProfile(
                userId = "user_$cleanTag",
                tagCode = cleanTag,
                displayName = userName.ifBlank { "Suchini" },
                score = score,
                completedHabits = completedHabits,
                currentStreak = currentStreak,
                petType = petType.id,
                petAccessory = petAccessory.id
            )
            val syncToken = buildShareSyncToken(profile)
            val shareMessage = buildString {
                append("🐙 ¡Compite conmigo en HabitOcto!\n")
                append("👤 Usuario: ${profile.displayName} #${profile.normalizedTag}\n")
                append("🔥 Racha actual: $currentStreak días | ✅ $completedHabits hábitos ($score pts)\n\n")
                append("👉 Ingresa mi código #$cleanTag en la pestaña 'Ranking Amigos' o pega este código de sincronización instantánea:\n")
                append(syncToken)
            }

            runCatching {
                val sendIntent = Intent(Intent.ACTION_SEND).apply {
                    type = "text/plain"
                    putExtra(Intent.EXTRA_SUBJECT, "Mi código HabitOcto #$cleanTag")
                    putExtra(Intent.EXTRA_TEXT, shareMessage)
                }
                val chooser = Intent.createChooser(sendIntent, "Compartir mi código #$cleanTag").apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
                }
                context.startActivity(chooser)
            }.onFailure {
                copyTagToClipboard(context, cleanTag, userName)
            }
        }
    }
}
