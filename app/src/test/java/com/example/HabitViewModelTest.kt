package com.example

import android.graphics.Bitmap
import android.graphics.Color
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
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@OptIn(ExperimentalCoroutinesApi::class)
@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
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

    @Test fun `profile tab 3 and social pet actions work end to end`() = trackedTest {
        runCurrent()
        vm.selectTab(3)
        assertEquals(3, vm.uiState.value.selectedTab)
        vm.selectTab(4)
        assertEquals(3, vm.uiState.value.selectedTab)

        vm.loginOrRegisterUser("Suchini", "suchini@habitocto.app", "777")
        vm.updatePetCustomization("dragon", "Draco", "gold_crown", "zen_garden")
        vm.addFriendByCode("204", "Carlos Ruiz", 1420, 11, 6)
        runCurrent()

        assertEquals("Suchini", vm.uiState.value.user!!.displayName)
        assertEquals("777", vm.uiState.value.user!!.friendCode)
        assertTrue(vm.uiState.value.user!!.isLoggedIn)
        assertEquals("dragon", vm.uiState.value.user!!.petType)
        assertEquals("Draco", vm.uiState.value.user!!.petName)
        assertEquals(1, vm.uiState.value.friends.size)
        assertEquals("204", vm.uiState.value.friends.single().friendCode)

        vm.logoutUser()
        runCurrent()
        assertFalse(vm.uiState.value.user!!.isLoggedIn)
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

    @Test fun `language toggle avatar update post comments and 7 day post lifecycle work end to end`() = trackedTest {
        runCurrent()
        assertEquals("es", vm.uiState.value.languageCode)

        vm.toggleLanguage()
        runCurrent()
        assertEquals("en", vm.uiState.value.languageCode)

        vm.setLanguage("es")
        runCurrent()
        assertEquals("es", vm.uiState.value.languageCode)

        var callbackInvoked = false
        vm.updateProfileWithAvatar("Suchini Pro", "suchini@uvg.edu.gt", "file:///internal/avatar_opt.jpg") {
            callbackInvoked = true
        }
        runCurrent()
        assertTrue(callbackInvoked)
        assertEquals("Suchini Pro", vm.uiState.value.user!!.displayName)
        assertEquals("file:///internal/avatar_opt.jpg", vm.uiState.value.user!!.avatarUrl)

        // Publicar un post propio y verificar comentarios + borrado
        vm.publishPost("Completé mi sesión de Modo Enfoque", "Productividad", "file:///internal/post_opt.jpg")
        runCurrent()
        assertEquals(1, vm.uiState.value.feedPosts.size)
        val ownPost = vm.uiState.value.feedPosts.single()
        assertTrue(ownPost.isOwnPost)
        assertEquals(7, ownPost.daysRemaining)

        vm.addComment(ownPost.id, "¡Increíble disciplina!")
        runCurrent()
        val updatedPost = vm.uiState.value.feedPosts.single()
        assertEquals(1, updatedPost.comments.size)
        assertEquals("¡Increíble disciplina!", updatedPost.comments.single().content)
        assertEquals("Suchini Pro", updatedPost.comments.single().authorName)

        vm.deleteOwnPost(ownPost.id)
        runCurrent()
        assertTrue(vm.uiState.value.feedPosts.isEmpty())
    }

    @Test fun `photo verification analyzer validates authentic bitmap in RAM with zero database storage`() {
        val validBitmap = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        for (y in 0 until 64) {
            for (x in 0 until 64) {
                val r = ((x * 4) + (y * 2)) % 256
                val g = ((x * 2) + (y * 3) + 40) % 256
                val b = ((x + y) * 3 + 80) % 256
                validBitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }
        val result = PhotoVerificationAnalyzer.analyzeBitmapAndDiscard(validBitmap, "Beber 1 vaso de agua")
        assertTrue(result.isValid)
        assertTrue(result.zeroDatabaseBytesConfirmed)
        assertTrue(result.confidencePercent >= 82)
        assertTrue(result.summaryEs.contains("0 B guardados en BD"))

        val darkBitmap = Bitmap.createBitmap(32, 32, Bitmap.Config.ARGB_8888)
        darkBitmap.eraseColor(Color.BLACK)
        val rejected = PhotoVerificationAnalyzer.analyzeBitmapAndDiscard(darkBitmap, "Leer")
        assertFalse(rejected.isValid)
        assertTrue(rejected.zeroDatabaseBytesConfirmed)
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
        override suspend fun updateProfileWithAvatar(name: String, email: String?, avatarUrl: String?) {
            val u = snapshots.value.user ?: return
            snapshots.value = snapshots.value.copy(
                user = u.copy(displayName = name, email = email, avatarUrl = avatarUrl ?: u.avatarUrl)
            )
        }
        override suspend fun updateAvatar(avatarUrl: String?) {
            val u = snapshots.value.user ?: return
            snapshots.value = snapshots.value.copy(user = u.copy(avatarUrl = avatarUrl))
        }
        override suspend fun setLanguage(languageCode: String) {
            val u = snapshots.value.user ?: return
            snapshots.value = snapshots.value.copy(user = u.copy(languageCode = languageCode))
        }
        override suspend fun publish(content: String, category: String, imageUrl: String?) {
            val post = FeedPostEntity(
                id = UUID.randomUUID().toString(),
                userId = "u",
                content = content,
                category = category,
                imageUrl = imageUrl,
                createdAt = System.currentTimeMillis(),
                authorId = "u"
            )
            snapshots.value = snapshots.value.copy(posts = listOf(post) + snapshots.value.posts)
        }
        override suspend fun deleteOwnPost(postId: String) {
            snapshots.value = snapshots.value.copy(
                posts = snapshots.value.posts.filterNot { it.id == postId && (it.authorId == null || it.authorId == "u") }
            )
        }
        override suspend fun addComment(postId: String, authorName: String, content: String) {
            snapshots.value = snapshots.value.copy(
                posts = snapshots.value.posts.map { post ->
                    if (post.id == postId) {
                        val existing = HabitConverters.decodeComments(post.commentsJson)
                        val updated = existing + FeedCommentItem(
                            id = UUID.randomUUID().toString(),
                            postId = postId,
                            authorName = authorName,
                            content = content,
                            timeAgo = "Ahora"
                        )
                        post.copy(commentsJson = HabitConverters.encodeComments(updated))
                    } else {
                        post
                    }
                }
            )
        }
        override suspend fun toggleLike(id: String) = Unit
        override suspend fun updatePetCustomization(petType: String, petName: String, accessory: String, habitat: String) {
            val u = snapshots.value.user ?: return
            snapshots.value = snapshots.value.copy(
                user = u.copy(petType = petType, petName = petName, petAccessory = accessory, petHabitat = habitat)
            )
        }
        override suspend fun loginOrRegisterUser(name: String, email: String?, friendCode: String?) {
            val u = snapshots.value.user ?: return
            snapshots.value = snapshots.value.copy(
                user = u.copy(displayName = name, email = email, friendCode = friendCode ?: "777", isLoggedIn = true)
            )
        }
        override suspend fun logoutUser() {
            val u = snapshots.value.user ?: return
            snapshots.value = snapshots.value.copy(user = u.copy(isLoggedIn = false))
        }
        override suspend fun addFriendByCode(code: String, name: String?, score: Int?, habits: Int?, streak: Int?) {
            val friend = FriendEntity(
                friendCode = code,
                userId = "u",
                displayName = name ?: "Amigo",
                score = score ?: 100,
                completedHabits = habits ?: 1,
                currentStreak = streak ?: 1,
                updatedAt = 1L
            )
            snapshots.value = snapshots.value.copy(friends = snapshots.value.friends + friend)
        }
    }
}
