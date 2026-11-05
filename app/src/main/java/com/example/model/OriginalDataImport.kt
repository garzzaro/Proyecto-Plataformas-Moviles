package com.example.model

import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

// ============================================================================
// PLAN DE IMPORTACIÓN: OriginalImportPlan
// ============================================================================

/**
 * OriginalImportPlan reúne las filas que deben insertarse desde los datos originales.
 *
 * - **¿Para qué sirve?** Prepara relaciones, fechas y cantidades antes de escribir en Room.
 * - **¿Cómo funciona?** Se calcula sin modificar la base; el repositorio lo aplica en una transacción.
 * - **¿Qué pasa si lo quitamos?** La preparación de datos quedaría mezclada con las escrituras.
 */
data class OriginalImportPlan(
    val users: List<UserEntity>,
    val habits: List<HabitEntity>,
    val records: List<HabitRecordEntity>,
    val posts: List<FeedPostEntity>
)

// ============================================================================
// IMPORTACIÓN ÚNICA DE LOS DATOS ORIGINALES: OriginalDataImport
// ============================================================================

/**
 * OriginalDataImport convierte las antiguas listas de prueba en registros persistentes.
 *
 * - **¿Para qué sirve?** Recupera hábitos, cumplimientos y publicaciones con autores y cantidades.
 * - **¿Cómo funciona?** Genera identificadores estables, reutiliza hábitos equivalentes existentes y
 *   separa los datos del período actual de los registros históricos. Room inserta con IGNORE para
 *   conservar lo que el usuario ya haya guardado.
 * - **¿Qué pasa si lo quitamos?** Los datos originales no podrían incorporarse de forma relacionada
 *   y repetible sin duplicar o reemplazar datos existentes.
 */
object OriginalDataImport {
    const val NAME = "original-project-data-v1"

    /**
     * plan prepara la carga de los datos para un propietario.
     *
     * - **¿Para qué sirve?** Conserva los valores originales sin sobrescribir hábitos ya editados.
     * - **¿Cómo funciona?** Calcula filas con claves estables; los hábitos presentes solo en el
     *   historial se guardan archivados. Las rachas se obtienen después desde sus cumplimientos.
     * - **¿Qué pasa si lo quitamos?** Faltarían relaciones o se necesitarían cifras de pantalla fijas.
     */
    fun plan(
        data: OriginalHabitData,
        userId: String,
        today: Long,
        existing: List<HabitEntity>,
        existingRecords: List<HabitRecordEntity> = emptyList(),
        nowMillis: Long = System.currentTimeMillis()
    ): OriginalImportPlan {
        val prefix = "original:$userId:"
        val historyDates = data.history.associate { it.id to historyDay(it.dateText, today) }
        val sources = data.habits.toMutableList()
        // Estas dos entradas originales solo existían en el historial, no en la rutina activa.
        data.history.filter { row -> sources.none { key(it.title) == key(row.habitTitle) } }.forEach { row ->
            sources += HabitItem("history:${row.id}", row.habitTitle, "Registro importado",
                if (row.habitTitle == "Aprender Idioma") "Aprendizaje" else "Arte y Creatividad")
        }
        val added = mutableListOf<HabitEntity>()
        val mapped = mutableMapOf<String, HabitEntity>()
        val activeSourceTitles = data.habits.map { key(it.title) }.toSet()
        sources.forEach { source ->
            val id = prefix + "habit:" + source.id
            val match = existing.firstOrNull { it.id == id }
                ?: existing.firstOrNull {
                    key(it.title) == key(source.title) && it.category == source.category && it.frequency == source.frequency
                }
            val dates = data.history.filter { key(it.habitTitle) == key(source.title) }.map { historyDates.getValue(it.id) }
            val earliest = minOf(today, dates.minOrNull() ?: today)
            val firstDay = if (source.frequency == "Semanal" && earliest < today) HabitDates.monday(earliest) else earliest
            val entity = match ?: HabitEntity(
                id, userId, source.title, source.subtitle, source.category, source.frequency,
                source.isProgressive, source.isMicroHabit, source.maxProgress.coerceAtLeast(1),
                source.progressUnit, source.tags, source.timeOrDuration, source.categoryColorHex,
                firstDay, firstDay, source.reminderTime, source.reminderEnabled,
                archivedDay = if (key(source.title) in activeSourceTitles) null else today
            ).also(added::add)
            mapped[key(source.title)] = entity
        }

        val rows = linkedMapOf<Pair<String, Long>, HabitRecordEntity>()
        fun put(record: HabitRecordEntity) {
            // Una frecuencia editada puede tener límites distintos: tampoco debe solaparse.
            if (existingRecords.any { it.habitId == record.habitId && it.periodStart <= record.periodEnd && it.periodEnd >= record.periodStart }) return
            rows[record.habitId to record.periodStart] = record
        }
        // Rellena períodos intermedios con avance cero; nunca inventa cumplimientos para copiar rachas.
        added.filter { it.archivedDay == null }.forEach { habit ->
            HabitTracking.missingRecords(habit, emptyList(), today).forEach(::put)
        }
        data.history.forEach { source ->
            val habit = mapped.getValue(key(source.habitTitle))
            val date = historyDates.getValue(source.id)
            val definition = sources.first { key(it.title) == key(source.habitTitle) }
            val weekly = definition.frequency == "Semanal"
            val start = if (weekly) HabitDates.monday(date) else date
            val end = if (weekly) start + 6 else date
            val target = definition.maxProgress.coerceAtLeast(1)
            put(HabitRecordEntity(habit.id, start, end, source.habitTitle, definition.isMicroHabit,
                definition.isProgressive, definition.timeOrDuration, target,
                if (source.isCompleted) target else 0, if (source.isCompleted) date else null, definition.frequency))
        }
        data.habits.forEach { source ->
            val habit = mapped.getValue(key(source.title))
            if (habit.archivedDay != null) return@forEach
            // Un hábito reutilizado conserva su estado actual, incluso si todavía estaba pendiente.
            if (added.none { it.id == habit.id }) return@forEach
            val start = if (source.frequency == "Semanal") maxOf(habit.createdDay, HabitDates.monday(today)) else today
            val end = if (source.frequency == "Semanal") HabitDates.monday(today) + 6 else today
            val target = source.maxProgress.coerceAtLeast(1)
            val progress = when {
                source.isCompleted -> target
                source.isProgressive -> source.currentProgress.coerceIn(0, target)
                else -> 0
            }
            put(HabitRecordEntity(habit.id, start, end, source.title, source.isMicroHabit,
                source.isProgressive, source.timeOrDuration, target, progress,
                if (progress >= target) today else null, source.frequency))
        }

        val authors = data.posts.map { source ->
            UserEntity(prefix + "author:" + source.id, source.authorName,
                avatarUrl = source.authorAvatarUrl, createdDay = today)
        }
        val posts = data.posts.mapIndexed { index, source ->
            // Las referencias de tiempo originales eran relativas, por eso se anclan a la carga.
            val minutesAgo = when (source.id) { "p1" -> 15L; "p2" -> 60L; "p3" -> 180L; else -> 300L }
            FeedPostEntity(prefix + "post:" + source.id, userId, source.content, source.habitCategory,
                source.postImageUrl, nowMillis - minutesAgo * 60_000, source.isLiked, authors[index].id,
                (source.likesCount - if (source.isLiked) 1 else 0).coerceAtLeast(0), source.habitTitle, source.streakCount)
        }
        return OriginalImportPlan(authors, added, rows.values.toList(), posts)
    }

    private fun key(value: String): String = value.trim().lowercase(Locale.ROOT)

    /**
     * historyDay recupera la fecha de las etiquetas originales, que no incluían año.
     *
     * - **¿Para qué sirve?** Guarda fechas consultables en lugar de etiquetas de texto fijas.
     * - **¿Cómo funciona?** Conserva día y mes y elige su ocurrencia más reciente que no sea futura.
     * - **¿Qué pasa si lo quitamos?** El historial importado no podría ordenarse ni filtrarse por fecha.
     */
    private fun historyDay(label: String, today: Long): Long {
        val date = label.substringAfter(',').trim().split(Regex("\\s+"))
        val month = when (date.first().lowercase(Locale.ROOT)) {
            "oct" -> GregorianCalendar.OCTOBER
            else -> error("Mes de historial original no reconocido: $label")
        }
        val day = date.last().toInt()
        val year = HabitDates.label(today, "yyyy").toInt()
        fun epochDay(value: Int): Long = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
            clear()
            isLenient = false
            set(value, month, day)
        }.timeInMillis / 86_400_000L
        val candidate = epochDay(year)
        return if (candidate <= today) candidate else epochDay(year - 1)
    }
}
