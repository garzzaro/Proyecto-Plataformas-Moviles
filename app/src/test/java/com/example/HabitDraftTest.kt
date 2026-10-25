package com.example

import com.example.model.HabitDraft
import com.example.model.HabitItem
import org.junit.Assert.*
import org.junit.Test

class HabitDraftTest {
    private val draft = HabitDraft(" Leer ", "Aprendizaje", true, true, true, "10", "08:00", false, " 5 min ")

    @Test fun `editing retains identity unit color and progress`() {
        val previous = HabitItem("id", "Antes", "", "Aprendizaje", currentProgress = 4,
            progressUnit = "páginas", categoryColorHex = 123L)
        val result = draft.toHabitItem(previous)
        assertEquals("id", result.id)
        assertEquals("Leer", result.title)
        assertEquals("5 min", result.timeOrDuration)
        assertEquals("páginas", result.progressUnit)
        assertEquals(123L, result.categoryColorHex)
        assertEquals(4, result.currentProgress)
        assertEquals(listOf("Micro", "Diario", "Aprendizaje"), result.tags)
    }

    @Test fun `new habits receive unique identifiers and weekly tags`() {
        val weekly = draft.copy(isDaily = false)
        assertNotEquals(weekly.toHabitItem().id, weekly.toHabitItem().id)
        assertEquals("Semanal", weekly.toHabitItem().frequency)
        assertTrue(weekly.toHabitItem().tags.contains("Semanal"))
    }

    @Test fun `invalid goals including integer overflow are rejected`() {
        listOf("", "0", "-1", "1.5", "abc", "99999999999999").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { draft.copy(goalText = value).toHabitItem() }
        }
    }

    @Test fun `invalid time and blank name are rejected`() {
        listOf("24:00", "12:60", "8:00", "abc").forEach { value ->
            assertThrows(IllegalArgumentException::class.java) { draft.copy(reminderTime = value).toHabitItem() }
        }
        assertThrows(IllegalArgumentException::class.java) { draft.copy(name = " ").toHabitItem() }
    }
}
