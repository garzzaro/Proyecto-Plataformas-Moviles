package com.example.model

import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf

/**
 * Idiomas soportados por HabitOcto en tiempo real (Español e Inglés).
 */
enum class AppLanguage(val code: String, val label: String, val flag: String) {
    ES("es", "Español", "🇪🇸"),
    EN("en", "English", "🇺🇸");

    val isEnglish: Boolean
        get() = this == EN

    fun toggle(): AppLanguage = if (this == ES) EN else ES

    companion object {
        fun fromCode(code: String?): AppLanguage =
            if (code?.trim()?.lowercase() == "en") EN else ES
    }
}

/**
 * CompositionLocal para propagar el idioma activo a cualquier pantalla o componente de Compose.
 */
val LocalAppLanguage = compositionLocalOf { AppLanguage.ES }

/**
 * Diccionario estructurado bilingüe (ES / EN) para toda la interfaz de HabitOcto.
 */
data class BilingualStrings(
    val language: AppLanguage,
    // Pestañas de navegación inferior
    val tabTracker: String,
    val tabFeed: String,
    val tabPet: String,
    val tabProfile: String,
    // Cabecera y Centro de Notificaciones
    val notificationsTitle: String,
    val noPendingNotifications: String,
    val markRead: String,
    val markAllAsRead: String,
    val notificationsSubtitle: String,
    val closeLabel: String,
    // Perfil y Ajustes
    val editProfileAndPhoto: String,
    val editLocalProfile: String,
    val changeProfilePhoto: String,
    val uploadFromGallery: String,
    val takeSelfie: String,
    val darkModeTitle: String,
    val darkModeOnSubtitle: String,
    val darkModeOffSubtitle: String,
    val appLanguageTitle: String,
    val appLanguageSubtitle: String,
    val personalStatsTitle: String,
    val last7DaysLabel: String,
    val weeklyConsistency: String,
    val consistencyLabel: String,
    val currentStreakLabel: String,
    val currentProgressLabel: String,
    val accountDetailsTitle: String,
    val emailAddressLabel: String,
    val noEmailConfigured: String,
    val changeAction: String,
    val configureAction: String,
    val deviceSessionLabel: String,
    val multiDeviceSyncLabel: String,
    val logoutAction: String,
    val switchOrLogoutAccount: String,
    val accountAndCodeButton: String,
    // Cuentas multi-usuario al compartir el APK o clonar el repositorio
    val multiUserCardBadge: String,
    val multiUserCardTitle: String,
    val multiUserCardDescription: String,
    val multiUserGuideButton: String,
    val multiUserStep1Title: String,
    val multiUserStep1Body: String,
    val multiUserStep2Title: String,
    val multiUserStep2Body: String,
    val multiUserStep3Title: String,
    val multiUserStep3Body: String,
    // Estados generales
    val loadingData: String,
    val retryAction: String,
    val saveAction: String,
    val cancelAction: String
) {
    fun formatActiveDays(days: Int): String =
        if (language.isEnglish) "$days days with completed habits" else "$days días con hábitos completados"

    fun formatDaysCount(days: Int): String =
        if (language.isEnglish) "$days days" else "$days días"

    fun formatBestStreak(best: Int): String =
        if (language.isEnglish) "Best streak: $best days" else "Mejor racha: $best días"

    fun formatTodayGoals(completed: Int, scheduled: Int): String =
        if (language.isEnglish) "$completed/$scheduled current goals" else "$completed/$scheduled metas actuales"

    fun formatSavedSessionTag(tag: String, isLoggedIn: Boolean): String =
        if (language.isEnglish) {
            if (isLoggedIn) "Saved session (#$tag)" else "Active profile (#$tag)"
        } else {
            if (isLoggedIn) "Sesión guardada (#$tag)" else "Perfil activo (#$tag)"
        }

    fun formatSyncActiveWithCode(tag: String, active: Boolean): String =
        if (language.isEnglish) {
            if (active) "Active with code #$tag" else "Paused"
        } else {
            if (active) "Activa con código #$tag" else "Pausada"
        }
}

/**
 * Proveedor centralizado de textos bilingües (Español / Inglés).
 */
object AppStrings {
    val ES = BilingualStrings(
        language = AppLanguage.ES,
        tabTracker = "Tracker",
        tabFeed = "Feed",
        tabPet = "Mascota",
        tabProfile = "Perfil",
        notificationsTitle = "Notificaciones",
        noPendingNotifications = "Sin notificaciones pendientes",
        markRead = "Marcar leídas",
        markAllAsRead = "Marcar todas como leídas",
        notificationsSubtitle = "Alertas inteligentes de tus hábitos, mascota y amigos",
        closeLabel = "Cerrar",
        editProfileAndPhoto = "Editar perfil y foto",
        editLocalProfile = "Editar perfil y foto",
        changeProfilePhoto = "Cambiar foto de perfil",
        uploadFromGallery = "🖼️ Subir de Galería",
        takeSelfie = "📷 Tomar Selfie",
        darkModeTitle = "Modo Noche / Oscuro",
        darkModeOnSubtitle = "Activado (tema negro OLED)",
        darkModeOffSubtitle = "Desactivado (tema claro)",
        appLanguageTitle = "Idioma de la App",
        appLanguageSubtitle = "Alterna entre Español e Inglés al instante",
        personalStatsTitle = "Estadísticas Personales",
        last7DaysLabel = "ÚLTIMOS 7 DÍAS",
        weeklyConsistency = "Consistencia Semanal",
        consistencyLabel = "Consistencia",
        currentStreakLabel = "Racha Actual",
        currentProgressLabel = "Progreso Actual",
        accountDetailsTitle = "Datos de la Cuenta",
        emailAddressLabel = "DIRECCIÓN DE CORREO",
        noEmailConfigured = "Sin correo configurado",
        changeAction = "Cambiar",
        configureAction = "Configurar",
        deviceSessionLabel = "SESIÓN EN ESTE DISPOSITIVO",
        multiDeviceSyncLabel = "SINCRONIZACIÓN MULTI-DISPOSITIVO",
        logoutAction = "Cerrar sesión",
        switchOrLogoutAccount = "Cambiar de cuenta / Cerrar sesión",
        accountAndCodeButton = "Cuenta / Cambiar #Código",
        multiUserCardBadge = "¿COMPARTISTE LA APP O CLONASTE EL REPO?",
        multiUserCardTitle = "Cada amigo tiene su propia cuenta y #Código",
        multiUserCardDescription = "Si le pasas el APK a un amigo o alguien descarga el repositorio en Android, puede crear su propio perfil con foto, nombre y #Código único de 3 dígitos sincronizado en Firebase Cloud (habitocto-suchini-2026).",
        multiUserGuideButton = "Cómo funciona para amigos / nuevos usuarios",
        multiUserStep1Title = "1. Crear Mi Cuenta o Cambiar Usuario",
        multiUserStep1Body = "Al abrir la app en otro teléfono o emulador, basta con pulsar 'Crear Mi Cuenta / Cambiar Usuario' para registrar su propio nombre, correo, foto de perfil optimizada y elegir su código #XXX (100-999).",
        multiUserStep2Title = "2. Sincronización Real en Firebase Cloud",
        multiUserStep2Body = "Cada cuenta publica su #Código de 3 dígitos en Firebase Firestore (habitocto-suchini-2026) y REST en vivo. Así pueden agregarse mutuamente en la pestaña Mascota -> Ranking Social.",
        multiUserStep3Title = "3. Sesión Persistente por Dispositivo",
        multiUserStep3Body = "Una vez creada o iniciada la cuenta de tu amigo en su teléfono, queda guardada automáticamente en Room y SharedPreferences de ese dispositivo sin cerrarse al salir.",
        loadingData = "Cargando tus datos…",
        retryAction = "Reintentar",
        saveAction = "Guardar",
        cancelAction = "Cancelar"
    )

    val EN = BilingualStrings(
        language = AppLanguage.EN,
        tabTracker = "Tracker",
        tabFeed = "Feed",
        tabPet = "Pet",
        tabProfile = "Profile",
        notificationsTitle = "Notifications",
        noPendingNotifications = "No pending notifications",
        markRead = "Mark read",
        markAllAsRead = "Mark all as read",
        notificationsSubtitle = "Smart alerts for your habits, pet & friends",
        closeLabel = "Close",
        editProfileAndPhoto = "Edit profile & photo",
        editLocalProfile = "Edit profile & photo",
        changeProfilePhoto = "Change profile photo",
        uploadFromGallery = "🖼️ Upload Photo",
        takeSelfie = "📷 Take Photo",
        darkModeTitle = "Dark Mode",
        darkModeOnSubtitle = "Enabled (OLED dark theme)",
        darkModeOffSubtitle = "Disabled (light theme)",
        appLanguageTitle = "App Language",
        appLanguageSubtitle = "Switch between Spanish and English anytime",
        personalStatsTitle = "Personal Stats",
        last7DaysLabel = "LAST 7 DAYS",
        weeklyConsistency = "Weekly Consistency",
        consistencyLabel = "Weekly Consistency",
        currentStreakLabel = "Current Streak",
        currentProgressLabel = "Current Progress",
        accountDetailsTitle = "Account Details",
        emailAddressLabel = "EMAIL ADDRESS",
        noEmailConfigured = "No email configured",
        changeAction = "Change",
        configureAction = "Configure",
        deviceSessionLabel = "SESSION ON THIS DEVICE",
        multiDeviceSyncLabel = "MULTI-DEVICE CLOUD SYNC",
        logoutAction = "Log out",
        switchOrLogoutAccount = "Switch account / Log out",
        accountAndCodeButton = "Account / Change #Code",
        multiUserCardBadge = "SHARED THE APK OR CLONED THE REPO?",
        multiUserCardTitle = "Every friend gets their own account & #Code",
        multiUserCardDescription = "When a friend installs the APK or clones the repo on Android, they can tap 'Create My Account / Switch User' to set their own name, email, profile photo and unique 3-digit #Code synced with Firebase Cloud (habitocto-suchini-2026).",
        multiUserGuideButton = "How accounts work for friends",
        multiUserStep1Title = "1. Create My Account or Switch User",
        multiUserStep1Body = "When opening the app on a new phone, tap 'Create My Account / Switch User' to register your own name, email, optimized profile photo, and pick a unique 3-digit #XXX code (100-999).",
        multiUserStep2Title = "2. Live Firebase Cloud Sync",
        multiUserStep2Body = "Each account syncs its 3-digit #Code to Firebase Cloud Firestore (habitocto-suchini-2026) and live REST, allowing friends to add each other in the Pet -> Social Ranking tab.",
        multiUserStep3Title = "3. Persistent Device Session",
        multiUserStep3Body = "Once created or logged in, your account stays automatically saved on that phone/tablet without logging out when closing the app.",
        loadingData = "Loading your data…",
        retryAction = "Retry",
        saveAction = "Save",
        cancelAction = "Cancel"
    )

    fun get(language: AppLanguage): BilingualStrings =
        if (language == AppLanguage.EN) EN else ES

    fun get(isEnglish: Boolean): BilingualStrings =
        if (isEnglish) EN else ES

    fun fromCode(code: String?): BilingualStrings =
        get(AppLanguage.fromCode(code))

    @Composable
    fun current(): BilingualStrings = get(LocalAppLanguage.current)
}
