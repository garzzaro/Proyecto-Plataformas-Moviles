package com.example.model

import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.GregorianCalendar
import java.util.Locale
import java.util.TimeZone

// ============================================================================
// FECHAS DEL SEGUIMIENTO: HabitDates
// ============================================================================

/**
 * HabitDates centraliza el manejo de fechas del seguimiento.
 *
 * - **¿Para qué sirve?** Define el día local y los límites semanales sin mezclar fechas con horas.
 * - **¿Cómo funciona?** Convierte la fecha civil del dispositivo a días enteros y usa Calendar para
 *   mantener compatibilidad desde Android 24.
 * - **¿Qué pasa si lo quitamos?** El historial podría cambiar de día por conversiones de zona horaria
 *   o cálculos inconsistentes.
 */
object HabitDates {
    private const val DAY_MS = 86_400_000L
    private val utc: TimeZone get() = TimeZone.getTimeZone("UTC")
    /** Devuelve la fecha local como día entero; no depende de la hora de la consulta. */
    fun today(now: Long = System.currentTimeMillis(), zone: TimeZone = TimeZone.getDefault()): Long {
        val local = Calendar.getInstance(zone).apply { timeInMillis = now }
        return GregorianCalendar(utc).apply {
            clear()
            set(local.get(Calendar.YEAR), local.get(Calendar.MONTH), local.get(Calendar.DAY_OF_MONTH))
        }.timeInMillis / DAY_MS
    }
    /** Localiza el lunes de la semana correspondiente a una fecha. */
    fun monday(day: Long): Long = day - Math.floorMod(day + 3, 7L)
    /** Convierte un día guardado a una etiqueta legible en español. */
    fun label(day: Long, pattern: String = "EEE, d MMM"): String =
        SimpleDateFormat(pattern, Locale.forLanguageTag("es-GT")).apply { timeZone = utc }.format(java.util.Date(day * DAY_MS))
}

// ============================================================================
// MODELOS DE ESTADÍSTICAS: DayProgress Y TrackingSummary
// ============================================================================

/**
 * DayProgress resume las metas activas de una fecha.
 *
 * - **¿Para qué sirve?** Proporciona el contador completados/programados y el porcentaje para las
 *   tarjetas y gráficas.
 * - **¿Cómo funciona?** Recibe cantidades calculadas desde los registros; si no hay metas, el
 *   porcentaje es cero.
 * - **¿Qué pasa si lo quitamos?** Cada componente tendría que recalcular sus cifras y podría mostrar
 *   resultados diferentes.
 */
data class DayProgress(val day: Long, val completed: Int, val scheduled: Int) {
    val percent: Int get() = if (scheduled == 0) 0 else (completed * 100.0 / scheduled).toInt()
}

/**
 * TrackingSummary reúne los indicadores visibles del seguimiento.
 *
 * - **¿Para qué sirve?** Comparte entre Tracker y Perfil las cifras semanales, actuales y de racha.
 * - **¿Cómo funciona?** Los contadores semanales usan la rutina activa; las rachas y logros históricos
 *   conservan los registros archivados.
 * - **¿Qué pasa si lo quitamos?** Las pantallas perderían una fuente común de estadísticas.
 */
data class TrackingSummary(
    val days: List<DayProgress> = emptyList(),
    val completedToday: Int = 0,
    val scheduledToday: Int = 0,
    val microStreak: Int = 0,
    val bestMicroStreak: Int = 0,
    val activeDays: Int = 0,
    val totalCompleted: Int = 0
) {
    val completedWeek: Int get() = days.sumOf { it.completed }
    val scheduledWeek: Int get() = days.sumOf { it.scheduled }
    val weekPercent: Int get() = if (scheduledWeek == 0) 0 else (completedWeek * 100.0 / scheduledWeek).toInt()
    val todayPercent: Int get() = if (scheduledToday == 0) 0 else (completedToday * 100.0 / scheduledToday).toInt()
}

// ============================================================================
// REGLAS DEL SEGUIMIENTO: HabitTracking
// ============================================================================

/**
 * HabitTracking contiene las reglas de cumplimiento y estadísticas.
 *
 * - **¿Para qué sirve?** Calcula los indicadores a partir de registros guardados, sin crear avances
 *   simulados.
 * - **¿Cómo funciona?** Usa funciones independientes de la interfaz y de Room para facilitar pruebas
 *   de fechas, archivo y rachas.
 * - **¿Qué pasa si lo quitamos?** Las reglas quedarían dispersas entre pantallas y sería más difícil
 *   mantener contadores coherentes.
 */
object HabitTracking {
    /**
     * missingRecords genera las ocurrencias pendientes de un hábito.
     *
     * - **¿Para qué sirve?** Incluye los períodos omitidos mientras la app estuvo cerrada sin duplicar
     *   los que ya están guardados.
     * - **¿Cómo funciona?** Comienza después del último período existente y crea ocurrencias diarias o
     *   semanales con avance cero.
     * - **¿Qué pasa si lo quitamos?** Faltarían registros para calcular el historial y los reinicios
     *   de período.
     */
    fun missingRecords(habit: HabitEntity, existing: List<HabitRecordEntity>, today: Long): List<HabitRecordEntity> {
        if (habit.archivedDay != null) return emptyList()
        var start = maxOf(habit.effectiveDay, existing.maxOfOrNull { it.periodEnd + 1 } ?: habit.effectiveDay)
        val result = mutableListOf<HabitRecordEntity>()
        while (start <= today) {
            val end = if (habit.frequency == "Semanal") HabitDates.monday(start) + 6 else start
            result += HabitRecordEntity(habit.id, start, end, habit.title, habit.isMicroHabit, habit.isProgressive, habit.timeOrDuration, habit.target, frequency = habit.frequency)
            start = end + 1
        }
        return result
    }

    /**
     * streak calcula días consecutivos con un logro.
     *
     * - **¿Para qué sirve?** Mantiene una racha que llegó hasta ayer mientras todavía puede
     *   completarse el día de hoy.
     * - **¿Cómo funciona?** Recorre hacia atrás un conjunto de fechas únicas; varias completaciones
     *   del mismo día cuentan una sola vez.
     * - **¿Qué pasa si lo quitamos?** La racha podría aumentar varias veces al día o romperse antes de
     *   que termine hoy.
     */
    fun streak(completedDays: Set<Long>, today: Long): Int {
        var cursor = if (today in completedDays) today else today - 1
        var streak = 0
        while (cursor in completedDays) { streak++; cursor-- }
        return streak
    }

    /**
     * summary reconstruye los indicadores desde los registros persistidos.
     *
     * - **¿Para qué sirve?** Actualiza el progreso semanal cuando se añade, completa o archiva un
     *   hábito.
     * - **¿Cómo funciona?** Filtra por activeIds al contar metas semanales y actuales; utiliza el
     *   historial completo para conservar las rachas y los logros.
     * - **¿Qué pasa si lo quitamos?** Un hábito eliminado seguiría sumándose al contador semanal o se
     *   perderían logros históricos.
     */
    fun summary(records: List<HabitRecordEntity>, activeIds: Set<String>, today: Long): TrackingSummary {
        val microDays = records.filter { it.isMicroHabit && it.completed }.mapNotNull { it.completedDay }.toSet()
        var best = 0
        var run = 0
        var previous: Long? = null
        microDays.sorted().forEach { day ->
            run = if (previous == day - 1) run + 1 else 1
            best = maxOf(best, run)
            previous = day
        }
        // Cada ocurrencia semanal cuenta una vez: al completarse o al vencer.
        // Mientras esté abierta y pendiente, se representa en la fecha de hoy.
        // El resumen semanal refleja la lista activa; los registros archivados siguen
        // disponibles para el historial y las rachas, pero no cuentan como metas actuales.
        val byDay = records.filter { it.habitId in activeIds && it.periodStart <= today }
            .groupBy { it.completedDay ?: minOf(it.periodEnd, today) }
        val current = records.filter { it.habitId in activeIds && today in it.periodStart..it.periodEnd }
        return TrackingSummary(
            days = (today - 6..today).map { day ->
                val rows = byDay[day].orEmpty()
                DayProgress(day, rows.count { it.completed }, rows.size)
            },
            completedToday = current.count { it.completed }, scheduledToday = current.size,
            microStreak = streak(microDays, today), bestMicroStreak = best,
            activeDays = records.mapNotNull { it.completedDay }.toSet().size,
            totalCompleted = records.count { it.completed }
        )
    }

    /**
     * habitStreak calcula la racha individual por períodos.
     *
     * - **¿Para qué sirve?** Muestra la continuidad de cada hábito sin confundir semanas con días.
     * - **¿Cómo funciona?** Recorre períodos contiguos de la misma frecuencia y permite que el período
     *   actual siga pendiente.
     * - **¿Qué pasa si lo quitamos?** Las tarjetas no podrían mostrar una racha individual coherente
     *   con sus registros.
     */
    fun habitStreak(records: List<HabitRecordEntity>, today: Long): Int {
        val sorted = records.sortedByDescending { it.periodStart }
        val relevant = if (sorted.firstOrNull()?.let { today in it.periodStart..it.periodEnd && !it.completed } == true) sorted.drop(1) else sorted
        var nextStart: Long? = null
        var count = 0
        for (record in relevant) {
            if (!record.completed || record.frequency != relevant.first().frequency || (nextStart != null && record.periodEnd + 1 != nextStart)) break
            count++
            nextStart = record.periodStart
        }
        return count
    }
}
