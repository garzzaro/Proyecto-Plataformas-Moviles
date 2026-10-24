package com.example

import androidx.lifecycle.ViewModelStore
import com.example.model.*
import com.example.viewmodel.MainViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.*
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class HabitViewModelTest {
    private val dispatcher = StandardTestDispatcher()
    private val store = ViewModelStore()
    private lateinit var repo: FakeRepository
    private lateinit var vm: MainViewModel
    @Before fun setup() {
        Dispatchers.setMain(dispatcher)
        repo = FakeRepository()
        vm = MainViewModel(repo)
        store.put("main", vm)
    }
    @After fun cleanup() { store.clear(); Dispatchers.resetMain() }

    private fun trackedTest(body: suspend TestScope.() -> Unit) = runTest(dispatcher) {
        try { body() } finally { store.clear(); runCurrent() }
    }

    @Test fun `fresh storage loads empty state and seven actual dates`() = trackedTest {
        runCurrent()
        assertFalse(vm.uiState.value.isLoading)
        assertTrue(vm.uiState.value.habits.isEmpty())
        assertTrue(vm.uiState.value.feedPosts.isEmpty())
        assertEquals(7, vm.uiState.value.summary.days.size)
        assertEquals(0, vm.uiState.value.summary.microStreak)
    }

    @Test fun `failed save keeps the form open with a visible error`() = trackedTest {
        runCurrent()
        vm.openAddHabitDialog()
        repo.failSave = true
        vm.saveHabit(HabitItem("h", "Leer", "", "Aprendizaje"))
        runCurrent()
        assertTrue(vm.uiState.value.isAddHabitDialogOpen)
        assertEquals("No se pudo guardar", vm.uiState.value.error)
        assertFalse(vm.uiState.value.isSaving)
    }

    @Test fun `successful save closes the form while data updates preserve navigation`() = trackedTest {
        runCurrent()
        vm.selectTab(2)
        vm.openAddHabitDialog()
        vm.saveHabit(HabitItem("h", "Leer", "", "Aprendizaje"))
        runCurrent()
        assertFalse(vm.uiState.value.isAddHabitDialogOpen)
        repo.snapshots.value = repo.snapshots.value.copy(user = repo.snapshots.value.user!!.copy(displayName = "María"))
        runCurrent()
        assertEquals(2, vm.uiState.value.selectedTab)
        assertEquals("María", vm.uiState.value.user!!.displayName)
    }

    @Test fun `deleting one of two completed habits updates weekly UI and keeps its history`() = trackedTest {
        runCurrent()
        val today = HabitDates.today()
        val habits = listOf("first", "second").map { id ->
            HabitEntity(id, "u", "Leer", "", "Aprendizaje", "Diario", false, true,
                1, "", emptyList(), "", 0L, today, today)
        }
        val records = habits.map { habit ->
            HabitRecordEntity(habit.id, today, today, habit.title, true, false, "", 1, 1, today)
        }
        repo.snapshots.value = repo.snapshots.value.copy(habits = habits, records = records)
        runCurrent()
        assertEquals(2, vm.uiState.value.summary.completedWeek)
        assertEquals(2, vm.uiState.value.summary.scheduledWeek)

        vm.deleteHabit(vm.uiState.value.habits.single { it.id == "second" })
        runCurrent()
        assertEquals(1, vm.uiState.value.habits.size)
        assertEquals(1, vm.uiState.value.summary.completedWeek)
        assertEquals(1, vm.uiState.value.summary.scheduledWeek)
        assertEquals(2, vm.uiState.value.history.size)

        // Una nueva instancia reconstruye el mismo contador desde la lectura guardada.
        val reopened = MainViewModel(repo)
        store.put("reopened", reopened)
        runCurrent()
        assertEquals(1, reopened.uiState.value.summary.completedWeek)
        assertEquals(1, reopened.uiState.value.summary.scheduledWeek)
        assertEquals(2, reopened.uiState.value.history.size)
    }

    private class FakeRepository : HabitRepository {
        override val snapshots = MutableStateFlow(HabitSnapshot(null, emptyList(), emptyList(), emptyList()))
        var failSave = false
        override suspend fun refresh(today: Long) { snapshots.value = snapshots.value.copy(user = UserEntity("u", createdDay = today)) }
        override suspend fun save(habit: HabitItem, today: Long) { if (failSave) error("No se pudo guardar") }
        override suspend fun archive(id: String, today: Long) {
            snapshots.value = snapshots.value.copy(habits = snapshots.value.habits.map {
                if (it.id == id) it.copy(archivedDay = today) else it
            })
        }
        override suspend fun setProgress(id: String, value: Int?, today: Long) = Unit
        override suspend fun setDarkMode(enabled: Boolean) = Unit
        override suspend fun updateProfile(name: String, email: String?) = Unit
        override suspend fun publish(content: String, category: String, imageUrl: String?) = Unit
        override suspend fun toggleLike(id: String) = Unit
    }
}
