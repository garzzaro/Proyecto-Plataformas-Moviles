package com.example.model

import java.util.UUID

/** Valores del formulario. La UI captura texto; este modelo valida y construye el hábito. */
data class HabitDraft(
    val name: String,
    val category: String,
    val isDaily: Boolean,
    val isProgressive: Boolean,
    val isMicroHabit: Boolean,
    val goalText: String,
    val reminderTime: String,
    val reminderEnabled: Boolean,
    val duration: String
) {
    fun toHabitItem(previous: HabitItem? = null): HabitItem {
        require(name.isNotBlank()) { "Escribe el nombre del hábito" }
        require(Regex("([01][0-9]|2[0-3]):[0-5][0-9]").matches(reminderTime)) {
            "Usa una hora válida en formato HH:mm"
        }
        val goal = if (isProgressive) {
            requireNotNull(goalText.trim().toIntOrNull()?.takeIf { it > 0 }) {
                "La meta debe ser un número entero mayor que cero"
            }
        } else 1
        val frequency = if (isDaily) "Diario" else "Semanal"
        return HabitItem(
            id = previous?.id ?: UUID.randomUUID().toString(),
            title = name.trim(),
            subtitle = if (isProgressive) "$goal por ${if (isDaily) "día" else "semana"}"
                else if (isMicroHabit) "Micro hábito" else "Hábito ${if (isDaily) "diario" else "semanal"}",
            category = category,
            frequency = frequency,
            streak = previous?.streak ?: 0,
            isProgressive = isProgressive,
            currentProgress = previous?.currentProgress ?: 0,
            maxProgress = goal,
            progressUnit = previous?.progressUnit.orEmpty(),
            isCompleted = previous?.isCompleted ?: false,
            isMicroHabit = isMicroHabit,
            tags = buildList {
                if (isMicroHabit) add("Micro")
                add(frequency)
                add(category)
            },
            timeOrDuration = duration.trim(),
            categoryColorHex = previous?.categoryColorHex ?: 0xFF00796B,
            reminderTime = reminderTime,
            reminderEnabled = reminderEnabled
        )
    }
}
