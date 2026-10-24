package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.TimeZone

class HabitTrackingTest {
    private fun habit(day: Long, weekly: Boolean = false) = HabitEntity(
        "h", "u", "Agua", "", "Hidratación", if (weekly) "Semanal" else "Diario",
        true, true, 8, "vasos", listOf("Micro"), "", 0, day, day
    )
    private fun record(day: Long, completed: Boolean, micro: Boolean = true) =
        HabitRecordEntity("h", day, day, "Agua", micro, false, "", 1,
            if (completed) 1 else 0, if (completed) day else null)

    @Test fun `new habits have no invented progress and missed days are generated`() {
        val rows = HabitTracking.missingRecords(habit(20_000), emptyList(), 20_006)
        assertEquals(7, rows.size)
        assertTrue(rows.all { it.progress == 0 && it.completedDay == null && !it.completed })
        assertEquals(emptyList<HabitRecordEntity>(), HabitTracking.missingRecords(habit(20_000), rows, 20_006))
    }

    @Test fun `weekly habits have one occurrence per ISO week and reset on Monday`() {
        val monday = HabitDates.monday(20_000)
        val rows = HabitTracking.missingRecords(habit(monday + 2, true), emptyList(), monday + 14)
        assertEquals(listOf(monday + 2, monday + 7, monday + 14), rows.map { it.periodStart })
        assertEquals(listOf(monday + 6, monday + 13, monday + 20), rows.map { it.periodEnd })
    }

    @Test fun `micro streak counts days rather than number of completions and allows today pending`() {
        val today = 20_006L
        val rows = listOf(record(today - 2, true), record(today - 1, true),
            record(today - 1, true).copy(habitId = "other"), record(today, false), record(today, true, false))
        val summary = HabitTracking.summary(rows, setOf("h", "other"), today)
        assertEquals(2, summary.microStreak)
        assertEquals(2, summary.bestMicroStreak)
        assertEquals(7, summary.days.size)
        assertEquals(today - 6, summary.days.first().day)
        assertEquals(0, HabitTracking.summary(rows, setOf("h"), today + 1).microStreak)
    }

    @Test fun `weekly completion is counted once on its real completion date`() {
        val monday = HabitDates.monday(20_000)
        val row = record(monday, true).copy(periodEnd = monday + 6, completedDay = monday + 2, frequency = "Semanal")
        val summary = HabitTracking.summary(listOf(row), setOf("h"), monday + 6)
        assertEquals(1, summary.completedWeek)
        assertEquals(1, summary.scheduledWeek)
        assertEquals(1, summary.days.single { it.day == monday + 2 }.completed)
        assertEquals(1, summary.completedToday)
    }

    @Test fun `zero denominators and historic windows are bounded`() {
        val summary = HabitTracking.summary(listOf(record(10, true)), emptySet(), 20)
        assertEquals(0, summary.weekPercent)
        assertEquals(0, summary.todayPercent)
        assertEquals(0, summary.completedWeek)
        assertEquals(1, summary.totalCompleted)
    }

    @Test fun `archiving a completed habit updates weekly counter from two of two to one of one`() {
        val today = 20_000L
        val rows = listOf(record(today, true), record(today, true).copy(habitId = "second"))
        val before = HabitTracking.summary(rows, setOf("h", "second"), today)
        val after = HabitTracking.summary(rows, setOf("h"), today)

        assertEquals(2, before.completedWeek)
        assertEquals(2, before.scheduledWeek)
        assertEquals(1, after.completedWeek)
        assertEquals(1, after.scheduledWeek)
        assertEquals(DayProgress(today, 1, 1), after.days.last())
        // Archivar cambia el contador activo, sin borrar los logros del historial.
        assertEquals(2, rows.size)
        assertEquals(2, after.totalCompleted)
        assertEquals(1, after.microStreak)
    }

    @Test fun `archiving a pending habit updates denominator and percentages`() {
        val today = 20_000L
        val rows = listOf(record(today, true), record(today, false).copy(habitId = "pending"))
        val before = HabitTracking.summary(rows, setOf("h", "pending"), today)
        val after = HabitTracking.summary(rows, setOf("h"), today)
        assertEquals(50, before.weekPercent)
        assertEquals(100, after.weekPercent)
        assertEquals(1, after.scheduledWeek)
    }

    @Test fun `archiving all habits clears weekly counters and leaves stored history intact`() {
        val today = 20_000L
        val rows = listOf(record(today - 1, true), record(today, true))
        val summary = HabitTracking.summary(rows, emptySet(), today)
        assertEquals(0, summary.completedWeek)
        assertEquals(0, summary.scheduledWeek)
        assertTrue(summary.days.all { it.completed == 0 && it.scheduled == 0 && it.percent == 0 })
        assertEquals(2, summary.totalCompleted)
        assertEquals(2, summary.microStreak)
    }

    @Test fun `dates use the device timezone without daylight saving drift`() {
        val instant = 1_760_746_200_000L
        val guatemala = HabitDates.today(instant, TimeZone.getTimeZone("America/Guatemala"))
        val tokyo = HabitDates.today(instant, TimeZone.getTimeZone("Asia/Tokyo"))
        assertTrue(tokyo - guatemala in 0..1)
        assertEquals(0L, HabitDates.today(0, TimeZone.getTimeZone("UTC")))
        assertEquals(-1L, HabitDates.today(0, TimeZone.getTimeZone("America/Guatemala")))
        assertEquals("lun", HabitDates.label(HabitDates.monday(20_000), "EEE").trimEnd('.'))
    }

    @Test fun `habit streak stops at a missed period and survives a pending current day`() {
        val today = 20_000L
        assertEquals(2, HabitTracking.habitStreak(listOf(record(today, false), record(today - 1, true), record(today - 2, true)), today))
        assertEquals(1, HabitTracking.habitStreak(listOf(record(today, true), record(today - 1, false), record(today - 2, true)), today))
    }
}
