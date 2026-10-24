package com.example

import com.example.model.*
import org.junit.Assert.*
import org.junit.Test
import java.util.GregorianCalendar
import java.util.TimeZone

class HabitOriginalDataTest {
    private val today = GregorianCalendar(TimeZone.getTimeZone("UTC")).apply {
        clear()
        set(2026, GregorianCalendar.OCTOBER, 23)
    }.timeInMillis / 86_400_000L

    @Test fun `original habits and historical only habits are stored with proper status`() {
        val plan = OriginalDataImport.plan(OriginalHabitData, "u", today, emptyList())
        assertEquals(11, plan.habits.count { it.archivedDay == null })
        assertEquals(2, plan.habits.count { it.archivedDay != null })
        assertEquals(setOf("Aprender Idioma", "Pintar o Dibujar"), plan.habits.filter { it.archivedDay != null }.map { it.title }.toSet())
        assertEquals(plan.records.size, plan.records.map { it.habitId to it.periodStart }.toSet().size)
        assertTrue(plan.records.all { row -> plan.habits.any { it.id == row.habitId } })
    }

    @Test fun `original progress and completion states are preserved for the import day`() {
        val plan = OriginalDataImport.plan(OriginalHabitData, "u", today, emptyList())
        OriginalHabitData.habits.forEach { source ->
            val habit = plan.habits.single { it.title == source.title }
            val record = plan.records.single { it.habitId == habit.id && today in it.periodStart..it.periodEnd }
            val expected = if (source.isCompleted) source.maxProgress else source.currentProgress
            assertEquals(source.title, expected, record.progress)
            assertEquals(source.title, source.isCompleted, record.completed)
        }
    }

    @Test fun `all explicit historical rows have their original day month and status`() {
        val plan = OriginalDataImport.plan(OriginalHabitData, "u", today, emptyList())
        OriginalHabitData.history.forEach { source ->
            val habit = plan.habits.single { it.title == source.habitTitle }
            val day = source.dateText.substringAfterLast(' ').toInt()
            val date = today - (23 - day)
            val row = plan.records.single { it.habitId == habit.id && date in it.periodStart..it.periodEnd }
            assertEquals(source.habitTitle, source.isCompleted, row.completed)
            if (source.isCompleted) assertEquals(date, row.completedDay)
        }
        assertTrue(plan.records.all { it.periodStart <= today })
    }

    @Test fun `posts preserve their own authors reactions photos and habit titles`() {
        val plan = OriginalDataImport.plan(OriginalHabitData, "u", today, emptyList())
        assertEquals(4, plan.posts.size)
        assertEquals(4, plan.users.size)
        OriginalHabitData.posts.forEach { source ->
            val post = plan.posts.single { it.content == source.content }
            val author = plan.users.single { it.id == post.authorId }
            assertEquals(source.authorName, author.displayName)
            assertEquals(source.authorAvatarUrl, author.avatarUrl)
            assertEquals(source.habitTitle, post.habitTitle)
            assertEquals(source.postImageUrl, post.imageUrl)
            assertEquals(source.likesCount, post.baseLikesCount + if (post.isLiked) 1 else 0)
            assertEquals(source.streakCount, post.authorStreak)
        }
    }

    @Test fun `equivalent existing habits keep edited definitions and their current progress`() {
        val first = OriginalDataImport.plan(OriginalHabitData, "u", today, emptyList())
        val existing = first.habits.single { it.title == "Beber Agua" }.copy(id = "my-habit", target = 10)
        val current = HabitRecordEntity(existing.id, today, today, "Beber Agua", true, true, "", 10, 3)
        val plan = OriginalDataImport.plan(OriginalHabitData, "u", today, listOf(existing), listOf(current))
        assertTrue(plan.habits.none { it.title == "Beber Agua" })
        assertTrue(plan.records.none { it.habitId == existing.id && today in it.periodStart..it.periodEnd })
        assertEquals(10, existing.target)
        assertEquals(3, current.progress)
    }

    @Test fun `existing archived habits are not reactivated by matching original data`() {
        val first = OriginalDataImport.plan(OriginalHabitData, "u", today, emptyList())
        val archived = first.habits.single { it.title == "Beber Agua" }.copy(archivedDay = today)
        val plan = OriginalDataImport.plan(OriginalHabitData, "u", today, listOf(archived))
        assertTrue(plan.habits.none { it.title == archived.title })
        assertTrue(plan.records.none { it.habitId == archived.id && it.periodStart == today })
    }

    @Test fun `import identifiers are stable and isolated per owner`() {
        val first = OriginalDataImport.plan(OriginalHabitData, "u", today, emptyList())
        val repeated = OriginalDataImport.plan(OriginalHabitData, "u", today, emptyList())
        val other = OriginalDataImport.plan(OriginalHabitData, "other", today, emptyList())
        assertEquals(first.habits.map { it.id }, repeated.habits.map { it.id })
        assertEquals(first.posts.map { it.id }, repeated.posts.map { it.id })
        assertTrue(first.habits.map { it.id }.intersect(other.habits.map { it.id }.toSet()).isEmpty())
    }
}
