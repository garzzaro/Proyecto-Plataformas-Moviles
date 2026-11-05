package com.example.model

/**
 * Tipos de mascota disponibles para desbloquear o personalizar.
 */
enum class PetType(
    val id: String,
    val displayName: String,
    val description: String,
    val iconEmoji: String,
    val requiredLevel: Int
) {
    OCTO("octo", "Octo el Pulpo", "Compañero marino alegre y flexible.", "🐙", 1),
    DRAGON("dragon", "Ignis el Dragón", "Ardiente de energía y motivación.", "🐉", 2),
    FOX("fox", "Kitsune el Zorro", "Astuto y enfocado en la disciplina.", "🦊", 3),
    PANDA("panda", "Bambú el Panda", "Calmado, maestro del mindfulness.", "🐼", 4),
    AXOLOTL("axolotl", "Axolote Místico", "Místico guardián de la regeneración diaria.", "🦎", 5);

    companion object {
        fun fromId(id: String?): PetType =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: OCTO
    }
}

/**
 * Accesorios para vestir a la mascota en su hábitat.
 */
enum class PetAccessory(
    val id: String,
    val displayName: String,
    val emoji: String,
    val requiredStreak: Int
) {
    NONE("none", "Sin accesorio", "", 0),
    SUNGLASSES("sunglasses", "Gafas de Sol", "🕶️", 1),
    SCARF("scarf", "Bufanda Aventurera", "🧣", 2),
    HEADPHONES("headphones", "Audífonos Gamer", "🎧", 3),
    WIZARD_HAT("wizard_hat", "Sombrero Mágico", "🧙‍♂️", 5),
    GOLD_CROWN("crown", "Corona Dorada", "👑", 7);

    companion object {
        fun fromId(id: String?): PetAccessory =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: NONE
    }
}

/**
 * Fondos de hábitat/sala donde vive la mascota.
 */
enum class PetHabitat(
    val id: String,
    val displayName: String,
    val emoji: String,
    val requiredCompletedHabits: Int
) {
    AQUARIUM("aquarium", "Arrecife Coralino", "🌊", 0),
    ZEN_GARDEN("zen_garden", "Jardín Zen", "🎋", 10),
    COZY_ROOM("cozy_room", "Habitación Gamer", "🕹️", 20),
    SPACE_STATION("space", "Estación Cósmica", "🚀", 30);

    companion object {
        fun fromId(id: String?): PetHabitat =
            entries.firstOrNull { it.id.equals(id?.trim(), ignoreCase = true) } ?: AQUARIUM
    }
}

/**
 * Configuración guardada de la mascota del usuario.
 */
data class PetCustomization(
    val petType: PetType = PetType.OCTO,
    val accessory: PetAccessory = PetAccessory.NONE,
    val habitat: PetHabitat = PetHabitat.AQUARIUM,
    val petName: String = "Octo"
) {
    val customName: String get() = petName

    companion object {
        fun fromUser(user: UserEntity?): PetCustomization {
            if (user == null) return PetCustomization()
            val resolvedType = PetType.fromId(user.petType)
            val resolvedName = user.petName.takeIf { it.isNotBlank() } ?: resolvedType.displayName
            return PetCustomization(
                petType = resolvedType,
                accessory = PetAccessory.fromId(user.petAccessory),
                habitat = PetHabitat.fromId(user.petHabitat),
                petName = resolvedName
            )
        }
    }
}

/**
 * Estado general de la mascota para la vista.
 */
data class PetStatus(
    val name: String = "Octo",
    val customization: PetCustomization = PetCustomization(),
    val level: Int = 1,
    val experience: Int = 0,
    val maxExperience: Int = 100,
    val happinessPercent: Int = 85,
    val energyPercent: Int = 90,
    val currentStreak: Int = 0,
    val moodTitle: String = "¡Lleno de energía!",
    val moodDescription: String = "Octo está feliz porque estás manteniendo tus hábitos al día."
)

/**
 * Medallas y logros desbloqueables de gamificación.
 */
data class AchievementBadge(
    val id: String,
    val title: String,
    val description: String,
    val emoji: String,
    val isUnlocked: Boolean,
    val progressText: String = "",
    val accentHex: Long = 0xFF00796B
)

object AchievementCatalog {
    fun evaluate(
        totalCompleted: Int,
        currentStreak: Int,
        friendsCount: Int,
        focusSessionsCompleted: Int = 0,
        petLevel: Int = maxOf(1, totalCompleted / 10 + 1)
    ): List<AchievementBadge> = listOf(
        AchievementBadge(
            id = "first_step",
            title = "Primer Paso",
            description = "Completa tu primer hábito o microhábito.",
            emoji = "🌱",
            isUnlocked = totalCompleted >= 1,
            progressText = "${minOf(totalCompleted, 1)}/1 hábito",
            accentHex = 0xFF4CAF50
        ),
        AchievementBadge(
            id = "fire_streak_3",
            title = "Racha de Fuego",
            description = "Mantén 3 días consecutivos cumpliendo hábitos.",
            emoji = "🔥",
            isUnlocked = currentStreak >= 3,
            progressText = "${minOf(currentStreak, 3)}/3 días",
            accentHex = 0xFFFF6D00
        ),
        AchievementBadge(
            id = "weekly_master_7",
            title = "Maestro Semanal",
            description = "Alcanza 7 días seguidos de disciplina imparable.",
            emoji = "👑",
            isUnlocked = currentStreak >= 7,
            progressText = "${minOf(currentStreak, 7)}/7 días",
            accentHex = 0xFFFFB300
        ),
        AchievementBadge(
            id = "collector",
            title = "Coleccionista",
            description = "Alcanza el Nivel 2 o 5+ hábitos para desbloquear compañeros y hábitats.",
            emoji = "🎨",
            isUnlocked = petLevel >= 2 || totalCompleted >= 5,
            progressText = "Nivel $petLevel • $totalCompleted/5 háb.",
            accentHex = 0xFF9C27B0
        ),
        AchievementBadge(
            id = "social_leader",
            title = "Líder Social",
            description = "Compite con amigos usando tu código # de 3 dígitos.",
            emoji = "🤝",
            isUnlocked = friendsCount >= 1,
            progressText = "$friendsCount amigos activos",
            accentHex = 0xFF00ACC1
        ),
        AchievementBadge(
            id = "zen_focus",
            title = "Entrenador Zen",
            description = "Entrena con tu mascota en el Temporizador Pomodoro o supera 10 hábitos.",
            emoji = "🧘",
            isUnlocked = focusSessionsCompleted >= 1 || totalCompleted >= 10,
            progressText = if (focusSessionsCompleted >= 1) "¡Completado!" else "${minOf(totalCompleted, 10)}/10 háb.",
            accentHex = 0xFF26A69A
        )
    )
}

/**
 * Modelo para el ranking de hábitos entre amigos y comunidad.
 */
data class RankingUser(
    val id: String,
    val name: String,
    val avatarUrl: String? = null,
    val score: Int,
    val completedHabits: Int,
    val currentStreak: Int,
    val rankPosition: Int,
    val isCurrentUser: Boolean = false,
    val badge: String? = null,
    val isFriend: Boolean = true,
    val friendCode: String = "000",
    val petType: PetType = PetType.OCTO,
    val petAccessory: PetAccessory = PetAccessory.NONE
)

/**
 * Filtro de tiempo para la tabla de líderes.
 */
enum class RankingPeriod {
    SEMANAL,
    MENSUAL,
    GLOBAL
}
