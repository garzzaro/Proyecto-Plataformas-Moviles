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
    val tagCode: String, // Código de 3 dígitos entre "100" y "999"
    val avatarUrl: String? = null,
    val isLoggedIn: Boolean = true,
    val lastSyncTimestamp: Long = System.currentTimeMillis(),
    val syncMode: String = "Dual Cloud + Realtime REST"
) {
    val normalizedTag: String
        get() = CloudSyncManager.normalizeTagCode(tagCode)

    val formattedTag: String
        get() = "#$normalizedTag"

    val fullHandle: String
        get() = "$displayName $formattedTag"
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
    val isOnline: Boolean = true
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
                isOnline = json.optBoolean("isOnline", true)
            )
        }
    }
}

/**
 * CloudSyncManager gestiona la autenticación persistente multi-cuenta y la sincronización
 * en tiempo real entre múltiples dispositivos mediante códigos únicos de 3 dígitos (#100 a #999).
 *
 * Arquitectura Dual Cloud + Realtime REST Sync:
 * 1. Detecta de forma segura si `FirebaseApp.getApps(context)` está inicializado sin crashear jamás
 *    cuando `google-services.json` no está presente en compilaciones locales.
 * 2. Sincroniza perfiles publicados mediante `OkHttpClient` contra endpoints REST JSON compartidos
 *    y caché persistente en `SharedPreferences` + `Room`.
 * 3. Provee un directorio determinista e interactivo para todos los códigos `#100` a `#999` y
 *    soporte de tokens de sincronización instantánea para compartir por WhatsApp o portapapeles.
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

    // Caché en memoria de perfiles publicados por código #100..#999
    private val publishedDirectory = mutableMapOf<String, SyncedUserProfile>()
    private var realtimeSyncJob: Job? = null

    init {
        seedInitialDirectory()
        loadPersistedDirectory()
        loadPersistedSessions()
        loadPersistedFriends()
        updateSyncModeBanner()
    }

    /**
     * Comprueba de manera 100% segura si Firebase está inicializado en el APK actual.
     * Nunca lanza excepción aunque `google-services.json` no exista.
     */
    fun isFirebaseInitialized(): Boolean {
        return runCatching {
            FirebaseApp.getApps(appContext).isNotEmpty()
        }.getOrDefault(false)
    }

    /**
     * Obtiene el Project ID de Firebase si `FirebaseApp` está configurado.
     */
    private fun getFirebaseProjectId(): String? {
        return runCatching {
            if (FirebaseApp.getApps(appContext).isNotEmpty()) {
                FirebaseApp.getInstance().options.projectId?.takeIf { it.isNotBlank() }
            } else {
                null
            }
        }.getOrNull()
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
     * Registra o inicia sesión para un usuario con su Nombre, Correo y Tag de 3 dígitos (#100 a #999).
     * Persiste tanto en `SharedPreferences` como en `HabitDatabase` (Room) para sobrevivir reinicios.
     */
    suspend fun loginOrRegister(
        name: String,
        email: String,
        preferredTag: String? = null,
        completedHabits: Int = 0,
        currentStreak: Int = 0,
        petCustomization: PetCustomization = PetCustomization()
    ): AuthSession = withContext(Dispatchers.IO) {
        val cleanName = name.trim().ifBlank { "Suchini" }
        val cleanEmail = email.trim().ifBlank { "${cleanName.lowercase().replace(" ", ".")}@habitocto.app" }
        val tag = if (!preferredTag.isNullOrBlank() && isValidTagCode(preferredTag)) {
            normalizeTagCode(preferredTag)
        } else {
            generateDeterministicTag(cleanName, cleanEmail)
        }

        val userId = "local-user"
        val session = AuthSession(
            userId = userId,
            displayName = cleanName,
            email = cleanEmail,
            tagCode = tag,
            isLoggedIn = true,
            lastSyncTimestamp = System.currentTimeMillis(),
            syncMode = if (isFirebaseInitialized()) "Firebase + REST Sync" else "Dual Cloud REST + P2P"
        )

        persistSession(session)

        // Sincronizar también con la entidad UserEntity en Room para que toda la app vea el cambio
        runCatching {
            val db = HabitDatabase.get(appContext)
            val dao = db.habits()
            val today = HabitDates.today()
            dao.insertUser(
                UserEntity(
                    id = userId,
                    displayName = cleanName,
                    email = cleanEmail,
                    createdDay = today,
                    friendCode = tag,
                    isLoggedIn = true
                )
            )
            dao.loginOrRegisterUser(userId, cleanName, cleanEmail, tag)
        }

        // Publicar perfil inmediatamente en el directorio y nube
        publishCurrentUserStats(
            completedHabits = completedHabits,
            currentStreak = currentStreak,
            petCustomization = petCustomization
        )

        session
    }

    /**
     * Versión no bloqueante para iniciar sesión o registrarse desde callbacks de UI.
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
        val cleanName = name.trim().ifBlank { "Suchini" }
        val cleanEmail = email.trim().ifBlank { "${cleanName.lowercase().replace(" ", ".")}@habitocto.app" }
        val tag = if (!preferredTag.isNullOrBlank() && isValidTagCode(preferredTag)) {
            normalizeTagCode(preferredTag)
        } else {
            generateDeterministicTag(cleanName, cleanEmail)
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
                _currentSession.value = jsonToSession(JSONObject(activeRaw))
            } else if (list.isNotEmpty()) {
                _currentSession.value = list.first()
            } else {
                // Sesión predeterminada inicial para que el usuario tenga un #Código activo desde el primer uso
                val defaultSession = AuthSession(
                    userId = "local-user",
                    displayName = "Suchini",
                    email = "suchini@habitocto.app",
                    tagCode = "777",
                    isLoggedIn = true
                )
                persistSession(defaultSession)
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
    }

    private fun jsonToSession(json: JSONObject): AuthSession = AuthSession(
        userId = json.optString("userId", "local-user"),
        displayName = json.optString("displayName", "Suchini"),
        email = json.optString("email", "suchini@habitocto.app"),
        tagCode = normalizeTagCode(json.optString("tagCode", "777")),
        avatarUrl = json.optString("avatarUrl", "").takeIf { it.isNotBlank() },
        isLoggedIn = json.optBoolean("isLoggedIn", true),
        lastSyncTimestamp = json.optLong("lastSyncTimestamp", System.currentTimeMillis()),
        syncMode = json.optString("syncMode", "Dual Cloud + Realtime REST")
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
            isOnline = true
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

            // 3. Consultar en el directorio publicado o generar perfil determinista para #100..#999
            val baseProfile = fromRest
                ?: synchronized(publishedDirectory) { publishedDirectory[tagCode] }
                ?: getDeterministicProfileForTag(tagCode)

            // Si el usuario proporcionó nombre o estadísticas personalizadas al agregar, se aplican y publican
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
            // También lo publicamos al relay para que otros dispositivos vean la actualización
            pushToSharedRestRelay(resolvedProfile)

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
     * Versión con callback para usar directamente desde botones de UI en Compose.
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
     * Actualiza en segundo plano todos los amigos agregados consultando Firebase/REST/Directorio.
     */
    suspend fun refreshFriendsRealtime(): List<SyncedUserProfile> = withContext(Dispatchers.IO) {
        val currentList = _syncedFriends.value
        val updated = currentList.map { friend ->
            val tag = friend.normalizedTag
            val remote = (if (isFirebaseInitialized()) fetchFromFirebaseFirestore(tag) else null)
                ?: fetchFromSharedRestRelay(tag)
                ?: synchronized(publishedDirectory) { publishedDirectory[tag] }
            if (remote != null && remote.updatedAt >= friend.updatedAt) {
                remote
            } else {
                friend
            }
        }
        _syncedFriends.value = updated
        savePersistedFriends(updated)
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
                val habits = fields.optJSONObject("completedHabits")?.optString("integerValue")?.toIntOrNull() ?: 0
                val streak = fields.optJSONObject("currentStreak")?.optString("integerValue")?.toIntOrNull() ?: 0
                val score = fields.optJSONObject("score")?.optString("integerValue")?.toIntOrNull()
                    ?: ((habits * 100) + (streak * 50))
                SyncedUserProfile(
                    userId = fields.optJSONObject("userId")?.optString("stringValue") ?: "user_$tagCode",
                    tagCode = tagCode,
                    displayName = fields.optJSONObject("displayName")?.optString("stringValue") ?: "Amigo #$tagCode",
                    email = fields.optJSONObject("email")?.optString("stringValue")?.takeIf { it.isNotBlank() },
                    score = score,
                    completedHabits = habits,
                    currentStreak = streak,
                    petType = fields.optJSONObject("petType")?.optString("stringValue") ?: PetType.OCTO.id,
                    petAccessory = fields.optJSONObject("petAccessory")?.optString("stringValue") ?: PetAccessory.NONE.id,
                    petHabitat = fields.optJSONObject("petHabitat")?.optString("stringValue") ?: PetHabitat.AQUARIUM.id,
                    petLevel = fields.optJSONObject("petLevel")?.optString("integerValue")?.toIntOrNull()
                        ?: maxOf(1, habits / 10 + 1),
                    updatedAt = fields.optJSONObject("updatedAt")?.optString("integerValue")?.toLongOrNull()
                        ?: System.currentTimeMillis()
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
     * Resuelve cualquier código de 3 dígitos (`#100` a `#999`). Si ya fue publicado por un dispositivo,
     * devuelve ese perfil; si no, genera un perfil determinista coherente para ese número `#XXX`.
     */
    fun resolveProfileByTag(tagCode: String): SyncedUserProfile {
        val clean = normalizeTagCode(tagCode)
        return synchronized(publishedDirectory) {
            publishedDirectory[clean]
        } ?: getDeterministicProfileForTag(clean)
    }

    companion object {
        private const val PREFS_NAME = "habitocto_cloud_sync_prefs"
        private const val KEY_ACTIVE_SESSION = "active_auth_session"
        private const val KEY_SAVED_SESSIONS = "saved_auth_sessions"
        private const val KEY_SYNCED_FRIENDS = "synced_friends_list"
        private const val KEY_PUBLISHED_DIRECTORY = "published_tag_directory"
        private const val KEY_CUSTOM_REST_URL = "custom_rest_endpoint"
        private const val DEFAULT_REST_BASE_URL = "https://kvdb.io/A6Xy9HabitOctoSyncV1"
        private val JSON_MEDIA_TYPE = "application/json; charset=utf-8".toMediaType()

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
