package com.example.model

// ============================================================================
// DATOS ORIGINALES DEL PROYECTO: OriginalHabitData
// ============================================================================

/**
 * OriginalHabitData conserva los datos que se utilizaban antes de incorporar Room.
 *
 * - **¿Para qué sirve?** Permite importarlos como registros persistentes, editables y relacionados.
 * - **¿Cómo funciona?** Centraliza las listas originales; OriginalDataImport prepara su carga única
 *   sin utilizar estas listas como estado temporal de las pantallas.
 * - **¿Qué pasa si lo quitamos?** No habría una fuente estable para recuperar los datos originales.
 */
object OriginalHabitData {
    const val profileName = "Alex Rivero"
    const val profileAvatar = "https://images.unsplash.com/photo-1507003211169-0a1dd7228f2d?w=400"

    val habits = listOf(
        HabitItem(
            id = "1",
            title = "Beber Agua",
            subtitle = "8 vasos al día",
            category = "Hidratación",
            frequency = "Diario",
            streak = 7,
            isProgressive = true,
            isMicroHabit = true,
            currentProgress = 5,
            maxProgress = 8,
            tags = listOf("Micro", "Diario", "Hidratación")
        ),
        HabitItem(
            id = "2",
            title = "Leer 1 Página",
            subtitle = "Racha: 12",
            category = "Aprendizaje",
            frequency = "Diario",
            streak = 12,
            isCompleted = true,
            isMicroHabit = true,
            tags = listOf("Micro", "Lectura")
        ),
        HabitItem(
            id = "3",
            title = "Plancha 1 Min",
            subtitle = "Ritual Mañanero",
            category = "Ejercicio",
            frequency = "Diario",
            streak = 8,
            timeOrDuration = "1 Min",
            isMicroHabit = true,
            tags = listOf("Micro", "Fuerza")
        ),
        HabitItem(
            id = "4",
            title = "Meditación",
            subtitle = "10 min al día",
            category = "Mindfulness",
            frequency = "Diario",
            streak = 15,
            isCompleted = true,
            tags = listOf("Mindfulness")
        ),
        HabitItem(
            id = "5",
            title = "Caminar 10,000 Pasos",
            subtitle = "8,200 / 10,000",
            category = "Ejercicio",
            frequency = "Diario",
            streak = 4,
            isProgressive = true,
            currentProgress = 8200,
            maxProgress = 10000,
            tags = listOf("Salud", "Diario")
        ),
        HabitItem(
            id = "6",
            title = "Estudiar Código",
            subtitle = "45 min al día",
            category = "Productividad",
            frequency = "Diario",
            streak = 20,
            tags = listOf("Aprendizaje", "Código")
        ),
        HabitItem(
            id = "7",
            title = "Dormir 8 Horas",
            subtitle = "Meta nocturna",
            category = "Sueño",
            frequency = "Diario",
            streak = 9,
            isCompleted = true,
            tags = listOf("Sueño", "Salud")
        ),
        HabitItem(
            id = "8",
            title = "Comer 2 Frutas",
            subtitle = "Porción diaria",
            category = "Nutrición",
            frequency = "Diario",
            streak = 3,
            isProgressive = true,
            isMicroHabit = true,
            currentProgress = 1,
            maxProgress = 2,
            tags = listOf("Micro", "Nutrición")
        ),
        HabitItem(
            id = "9",
            title = "Escribir Diario",
            subtitle = "Reflexión nocturna",
            category = "Salud Mental",
            frequency = "Diario",
            streak = 7,
            tags = listOf("Bienestar")
        ),
        HabitItem(
            id = "10",
            title = "Ahorrar $5",
            subtitle = "Fondo personal",
            category = "Finanzas",
            frequency = "Semanal",
            streak = 2,
            isMicroHabit = true,
            tags = listOf("Micro", "Finanzas", "Ahorro")
        ),
        HabitItem(
            id = "11",
            title = "Práctica de Guitarra",
            subtitle = "20 min al día",
            category = "Arte y Creatividad",
            frequency = "Diario",
            streak = 5,
            tags = listOf("Música", "Arte")
        )
    )

    val history = listOf(
        HabitHistoryItem("h1", "Beber Agua", "Lun, Oct 21", true),
        HabitHistoryItem("h2", "Leer 1 Página", "Dom, Oct 20", true),
        HabitHistoryItem("h3", "Plancha 1 Min", "Dom, Oct 20", true),
        HabitHistoryItem("h4", "Meditación", "Sáb, Oct 19", false),
        HabitHistoryItem("h5", "Caminar 10,000 Pasos", "Vie, Oct 18", true),
        HabitHistoryItem("h6", "Estudiar Código", "Jue, Oct 17", true),
        HabitHistoryItem("h7", "Dormir 8 Horas", "Mié, Oct 16", true),
        HabitHistoryItem("h8", "Comer 2 Frutas", "Mar, Oct 15", true),
        HabitHistoryItem("h9", "Escribir Diario", "Lun, Oct 14", false),
        HabitHistoryItem("h10", "Ahorrar $5", "Dom, Oct 13", true),
        HabitHistoryItem("h11", "Aprender Idioma", "Sáb, Oct 12", true),
        HabitHistoryItem("h12", "Pintar o Dibujar", "Vie, Oct 11", true)
    )

    val posts = listOf(
        FeedPostItem(
            id = "p1",
            authorName = "Sofia Morales",
            authorAvatarUrl = "https://images.unsplash.com/photo-1494790108377-be9c29b29330?w=200",
            habitTitle = "Plancha 1 Min",
            habitCategory = "Ejercicio",
            content = "¡Completé mi micro hábito antes del desayuno! Día 14 consecutivo sin fallar 💪",
            postImageUrl = "https://images.unsplash.com/photo-1517838277536-f5f99be501cd?w=800", // Publicación 1 con foto
            timeAgo = "Hace 15 min",
            likesCount = 12,
            isLiked = false,
            streakCount = 14
        ),
        FeedPostItem(
            id = "p2",
            authorName = "Carlos Mendoza",
            authorAvatarUrl = "https://images.unsplash.com/photo-1500648767791-00dcc994a43e?w=200",
            habitTitle = "Beber Agua",
            habitCategory = "Hidratación",
            content = "Ya alcancé 6 de los 8 vasos recomendados. ¡Mantenerse bien hidratado durante el estudio cambia totalmente la concentración! 💧",
            postImageUrl = null, // Publicación 2 solo texto
            timeAgo = "Hace 1 hora",
            likesCount = 8,
            isLiked = true,
            streakCount = 7
        ),
        FeedPostItem(
            id = "p3",
            authorName = "Lucia Ramos",
            authorAvatarUrl = "https://images.unsplash.com/photo-1534528741775-53994a69daeb?w=200",
            habitTitle = "Meditación",
            habitCategory = "Mindfulness",
            content = "10 minutos de respiración consciente en el parque para empezar el día con paz mental 🧘‍♀️✨",
            postImageUrl = "https://images.unsplash.com/photo-1506126613408-eca07ce68773?w=800", // Publicación 3 con foto
            timeAgo = "Hace 3 horas",
            likesCount = 19,
            isLiked = false,
            streakCount = 21
        ),
        FeedPostItem(
            id = "p4",
            authorName = "Diego Castillo",
            authorAvatarUrl = "https://images.unsplash.com/photo-1522075469751-3a6694fb2f61?w=200",
            habitTitle = "Estudiar Código",
            habitCategory = "Productividad",
            content = "Terminé la práctica completa de Compose y Jetpack en Android Studio con Kotlin. ¡La consistencia diaria en pequeños bloques de 45 minutos rinde increíbles frutos!",
            postImageUrl = null, // Publicación 4 solo texto
            timeAgo = "Hace 5 horas",
            likesCount = 25,
            isLiked = true,
            streakCount = 9
        )
    )
}
