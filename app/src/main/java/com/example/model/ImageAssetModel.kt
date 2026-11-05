package com.example.model

import androidx.annotation.DrawableRes
import com.example.R

// ============================================================================
// SISTEMA ESTRUCTURADO DE IMÁGENES Y CATÁLOGO VISUAL
// ============================================================================

/**
 * Modelo estructurado para representar recursos gráficos locales, avatares remotos o vectores
 * con respaldo seguro al recurso local de Octo (`R.drawable.ic_pulpo`).
 */
data class ImageAssetModel(
    val id: String,
    val title: String,
    @param:DrawableRes val localResId: Int = R.drawable.ic_pulpo,
    val remoteUrl: String? = null,
    @param:DrawableRes val fallbackResId: Int = R.drawable.ic_pulpo,
    val accentColorHex: Long = 0xFF00796B
) {
    /**
     * Resuelve el modelo óptimo para Coil: descarta URLs vacías o enlaces antiguos rotos (`pulpo.avif`)
     * y entrega el recurso drawable local cuando corresponde.
     */
    fun resolveCoilData(): Any {
        val cleanUrl = remoteUrl?.trim()
        return if (!cleanUrl.isNullOrEmpty() && !AvatarCatalog.isBrokenUrl(cleanUrl)) {
            cleanUrl
        } else {
            localResId
        }
    }
}

/**
 * Catálogo centralizado de avatares e imágenes de la aplicación.
 * Evita enlaces rotos y ofrece resolución segura para usuarios, amigos y publicaciones.
 */
object AvatarCatalog {
    private const val BROKEN_PULPO_AVIF_FRAGMENT = "pulpo.avif"

    val APP_LOGO = ImageAssetModel(
        id = "app_logo_octo",
        title = "HabitOcto Logo",
        localResId = R.drawable.ic_pulpo,
        fallbackResId = R.drawable.ic_pulpo,
        accentColorHex = 0xFF00796B
    )

    val OCTO_FOREGROUND = ImageAssetModel(
        id = "octo_foreground",
        title = "Octo Foreground",
        localResId = R.drawable.ic_pulpo_foreground,
        fallbackResId = R.drawable.ic_pulpo,
        accentColorHex = 0xFF26A69A
    )

    val PRESET_AVATARS: List<ImageAssetModel> = listOf(
        APP_LOGO,
        ImageAssetModel(
            id = "avatar_alex",
            title = "Alex Rivero",
            localResId = R.drawable.ic_pulpo,
            remoteUrl = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400",
            accentColorHex = 0xFF00796B
        ),
        ImageAssetModel(
            id = "avatar_sofia",
            title = "Sofía Morales",
            localResId = R.drawable.ic_pulpo,
            remoteUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
            accentColorHex = 0xFFE91E63
        ),
        ImageAssetModel(
            id = "avatar_carlos",
            title = "Carlos Mendoza",
            localResId = R.drawable.ic_pulpo,
            remoteUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
            accentColorHex = 0xFF1E88E5
        ),
        ImageAssetModel(
            id = "avatar_lucia",
            title = "Lucía Ramos",
            localResId = R.drawable.ic_pulpo,
            remoteUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            accentColorHex = 0xFF8E24AA
        ),
        ImageAssetModel(
            id = "avatar_diego",
            title = "Diego Castillo",
            localResId = R.drawable.ic_pulpo,
            remoteUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200",
            accentColorHex = 0xFF43A047
        )
    )

    fun isBrokenUrl(url: String?): Boolean {
        if (url.isNullOrBlank()) return true
        return url.contains(BROKEN_PULPO_AVIF_FRAGMENT, ignoreCase = true)
    }

    fun appLogo(): ImageAssetModel = APP_LOGO

    fun resolveAvatar(
        avatarUrl: String?,
        @DrawableRes fallbackResId: Int = R.drawable.ic_pulpo
    ): ImageAssetModel {
        val clean = avatarUrl?.trim()
        if (clean.isNullOrEmpty() || isBrokenUrl(clean)) {
            return ImageAssetModel(
                id = "local_default_avatar",
                title = "Avatar HabitOcto",
                localResId = fallbackResId,
                remoteUrl = null,
                fallbackResId = fallbackResId
            )
        }
        return ImageAssetModel(
            id = "remote_avatar_${clean.hashCode()}",
            title = "Avatar de Usuario",
            localResId = fallbackResId,
            remoteUrl = clean,
            fallbackResId = fallbackResId
        )
    }

    fun sanitizeModel(model: Any?, @DrawableRes fallbackResId: Int = R.drawable.ic_pulpo): Any {
        return when (model) {
            null -> fallbackResId
            is ImageAssetModel -> model.resolveCoilData()
            is String -> {
                val trimmed = model.trim()
                if (trimmed.isEmpty() || isBrokenUrl(trimmed)) fallbackResId else trimmed
            }
            is Int -> if (model == R.drawable.ic_launcher_background) fallbackResId else model
            else -> model
        }
    }
}

/**
 * Modelo visual de cada mascota y hábitat para renderizado vectorial en Canvas/Compose y recursos locales.
 */
data class PetVisualModel(
    val petType: PetType,
    @param:DrawableRes val drawableResId: Int = R.drawable.ic_pulpo,
    val primaryColorHex: Long,
    val secondaryColorHex: Long,
    val auraColorHex: Long,
    val eyeColorHex: Long = 0xFFFFFFFF,
    val usesCustomCanvasVector: Boolean = false
)

fun PetType.visualModel(): PetVisualModel = when (this) {
    PetType.OCTO -> PetVisualModel(
        petType = PetType.OCTO,
        drawableResId = R.drawable.ic_pulpo,
        primaryColorHex = 0xFF26A69A,
        secondaryColorHex = 0xFF80CBC4,
        auraColorHex = 0xFF64FFDA,
        usesCustomCanvasVector = false
    )
    PetType.DRAGON -> PetVisualModel(
        petType = PetType.DRAGON,
        drawableResId = R.drawable.ic_pulpo_foreground,
        primaryColorHex = 0xFFFF6D00,
        secondaryColorHex = 0xFFFFD180,
        auraColorHex = 0xFFFFAB40,
        usesCustomCanvasVector = true
    )
    PetType.FOX -> PetVisualModel(
        petType = PetType.FOX,
        drawableResId = R.drawable.ic_pulpo_foreground,
        primaryColorHex = 0xFFF4511E,
        secondaryColorHex = 0xFFFFCCBC,
        auraColorHex = 0xFFFF8A65,
        usesCustomCanvasVector = true
    )
    PetType.PANDA -> PetVisualModel(
        petType = PetType.PANDA,
        drawableResId = R.drawable.ic_pulpo_foreground,
        primaryColorHex = 0xFF37474F,
        secondaryColorHex = 0xFFECEFF1,
        auraColorHex = 0xFFA5D6A7,
        usesCustomCanvasVector = true
    )
    else -> PetVisualModel(
        petType = this,
        drawableResId = R.drawable.ic_pulpo_foreground,
        primaryColorHex = 0xFFF06292,
        secondaryColorHex = 0xFFF8BBD0,
        auraColorHex = 0xFFFF80AB,
        usesCustomCanvasVector = true
    )
}

/**
 * Catálogo y generador determinístico de amigos por código de 3 dígitos (`#XXX`).
 */
object FriendDirectory {
    data class FriendTemplate(
        val friendCode: String,
        val displayName: String,
        val avatarUrl: String?,
        val completedHabits: Int,
        val currentStreak: Int,
        val petType: String,
        val petAccessory: String
    ) {
        val score: Int get() = (completedHabits * 100) + (currentStreak * 50)
    }

    val DEFAULT_FRIENDS: List<FriendTemplate> = listOf(
        FriendTemplate(
            friendCode = "729",
            displayName = "Sofía Mendoza",
            avatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
            completedHabits = 15,
            currentStreak = 7,
            petType = PetType.DRAGON.id,
            petAccessory = PetAccessory.GOLD_CROWN.id
        ),
        FriendTemplate(
            friendCode = "314",
            displayName = "Carlos Ruiz",
            avatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
            completedHabits = 11,
            currentStreak = 6,
            petType = PetType.FOX.id,
            petAccessory = PetAccessory.HEADPHONES.id
        ),
        FriendTemplate(
            friendCode = "856",
            displayName = "Valeria Gómez",
            avatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            completedHabits = 8,
            currentStreak = 4,
            petType = PetType.PANDA.id,
            petAccessory = PetAccessory.SUNGLASSES.id
        ),
        FriendTemplate(
            friendCode = "205",
            displayName = "Mateo Silva",
            avatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200",
            completedHabits = 6,
            currentStreak = 3,
            petType = PetType.OCTO.id,
            petAccessory = PetAccessory.NONE.id
        )
    )

    private val EXTRA_DIRECTORY: Map<String, FriendTemplate> = mapOf(
        "101" to FriendTemplate("101", "Andrea Morales", null, 14, 5, PetType.FOX.id, PetAccessory.WIZARD_HAT.id),
        "222" to FriendTemplate("222", "Diego Castillo", "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200", 12, 9, PetType.DRAGON.id, PetAccessory.HEADPHONES.id),
        "333" to FriendTemplate("333", "Lucía Ramos", "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200", 18, 11, PetType.PANDA.id, PetAccessory.GOLD_CROWN.id),
        "404" to FriendTemplate("404", "Javier Hernández", null, 9, 4, PetType.OCTO.id, PetAccessory.SUNGLASSES.id),
        "555" to FriendTemplate("555", "Camila Estrada", null, 16, 8, PetType.DRAGON.id, PetAccessory.WIZARD_HAT.id),
        "777" to FriendTemplate("777", "Fernando López", null, 20, 14, PetType.FOX.id, PetAccessory.GOLD_CROWN.id),
        "888" to FriendTemplate("888", "Gabriela Pineda", null, 13, 6, PetType.PANDA.id, PetAccessory.HEADPHONES.id),
        "999" to FriendTemplate("999", "Sebastián Cruz", null, 10, 5, PetType.OCTO.id, PetAccessory.SUNGLASSES.id)
    )

    private val SAMPLE_NAMES = listOf(
        "Alejandro Vega", "Mariana Solares", "Daniel Cabrera", "Paula Aguirre",
        "Rodrigo Marroquín", "Natalia Fuentes", "Esteban Arriola", "Valeska León",
        "Emilio Santizo", "Ximena Batres"
    )

    /**
     * Genera un código único determinístico o pseudoaleatorio de 3 dígitos (`100`..`999`).
     */
    fun generateCodeForUser(userId: String, seedName: String? = null): String {
        val base = ((seedName?.takeIf { it.isNotBlank() } ?: userId).hashCode() and 0x7FFFFFFF)
        val numeric = 100 + (base % 900)
        return numeric.toString().padStart(3, '0')
    }

    /**
     * Crea un `FriendEntity` a partir de un código de 3 dígitos y parámetros opcionales.
     */
    fun createFriendEntity(
        userId: String,
        rawCode: String,
        customName: String? = null,
        customScore: Int? = null,
        customHabits: Int? = null,
        customStreak: Int? = null,
        nowMillis: Long = System.currentTimeMillis()
    ): FriendEntity {
        val code = rawCode.trim().removePrefix("#")
        val preset = DEFAULT_FRIENDS.firstOrNull { it.friendCode == code } ?: EXTRA_DIRECTORY[code]
        val codeInt = code.toIntOrNull() ?: 500

        val resolvedName = customName?.trim()?.takeIf { it.isNotEmpty() }
            ?: preset?.displayName
            ?: "${SAMPLE_NAMES[codeInt % SAMPLE_NAMES.size]} (#$code)"

        val resolvedHabits = (customHabits ?: preset?.completedHabits ?: (4 + (codeInt % 15))).coerceAtLeast(0)
        val resolvedStreak = (customStreak ?: preset?.currentStreak ?: (1 + (codeInt % 10))).coerceAtLeast(0)
        val resolvedScore = (customScore ?: ((resolvedHabits * 100) + (resolvedStreak * 50))).coerceAtLeast(0)
        val resolvedPet = preset?.petType ?: PetType.entries[codeInt % PetType.entries.size].id
        val resolvedAcc = preset?.petAccessory ?: PetAccessory.entries[codeInt % PetAccessory.entries.size].id

        return FriendEntity(
            friendCode = code,
            userId = userId,
            displayName = resolvedName,
            avatarUrl = preset?.avatarUrl,
            score = resolvedScore,
            completedHabits = resolvedHabits,
            currentStreak = resolvedStreak,
            petType = resolvedPet,
            petAccessory = resolvedAcc,
            updatedAt = nowMillis
        )
    }
}
