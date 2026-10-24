package com.example

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.After
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config
import java.util.UUID

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitRepositoryTest {
    private lateinit var db: HabitDatabase
    private lateinit var repo: RoomHabitRepository
    private lateinit var context: Context
    private val databaseName = "test-${UUID.randomUUID()}.db"
    private val today = 20_000L
    private fun item(id: String = "h", weekly: Boolean = false) = HabitItem(id, "Tomar agua", "", "Hidratación",
        frequency = if (weekly) "Semanal" else "Diario", isProgressive = true, maxProgress = 8, isMicroHabit = true,
        tags = listOf("Micro", "comillas \" y comas,", "ñ"))

    @Before fun setup() = runBlocking {
        context = ApplicationProvider.getApplicationContext()
        open()
        repo.refresh(today)
    }
    private fun open() {
        db = Room.databaseBuilder(context, HabitDatabase::class.java, databaseName).build()
        repo = RoomHabitRepository(db)
    }
    @After fun cleanup() { db.close(); context.deleteDatabase(databaseName) }

    @Test fun `progress profile theme and posts survive closing and reopening the database`() = runBlocking {
        repo.save(item(), today)
        repo.setProgress("h", 8, today)
        repo.updateProfile("María", "maria@example.com")
        repo.setDarkMode(true)
        repo.publish("Mi primer logro", "Hidratación", null)
        val post = repo.snapshots.first { it.posts.size == 1 }.posts.single()
        repo.toggleLike(post.id)
        db.close()
        open()
        val snapshot = repo.snapshots.first { it.user != null }
        assertEquals("María", snapshot.user!!.displayName)
        assertTrue(snapshot.user!!.isDarkMode)
        assertTrue(snapshot.posts.single().isLiked)
        assertEquals(8, snapshot.records.single().progress)
        assertEquals(item().tags, snapshot.habits.single().tags)
    }

    @Test fun `daily reset preserves seven days and undo does not change yesterday`() = runBlocking {
        repo.save(item(), today - 1)
        repo.setProgress("h", 8, today - 1)
        repo.refresh(today)
        assertEquals(0, db.habits().records("h").first().progress)
        repo.setProgress("h", 8, today)
        repo.setProgress("h", 0, today)
        val rows = db.habits().records("h")
        assertFalse(rows.first().completed)
        assertTrue(rows.last().completed)
        repo.refresh(today + 6)
        repo.refresh(today + 6)
        assertEquals(8, db.habits().records("h").size)
    }

    @Test fun `editing and archiving retain closed occurrence snapshots`() = runBlocking {
        repo.save(item(), today - 1)
        repo.setProgress("h", 8, today - 1)
        repo.save(item().copy(title = "Otra meta", maxProgress = 10), today)
        assertEquals("Tomar agua", db.habits().records("h").last().title)
        assertEquals(8, db.habits().records("h").last().target)
        repo.archive("h", today)
        assertTrue(db.habits().activeHabits("local-user").isEmpty())
        assertEquals(2, db.habits().records("h").size)
        repo.refresh(today + 10)
        assertEquals(2, db.habits().records("h").size)
    }

    @Test fun `weekly progress is retained all week and frequency changes at the next boundary`() = runBlocking {
        val monday = HabitDates.monday(today)
        repo.save(item(weekly = true), monday)
        repo.setProgress("h", 5, monday + 2)
        repo.save(item(weekly = false), monday + 3)
        repo.refresh(monday + 6)
        assertEquals(1, db.habits().records("h").size)
        assertEquals(5, db.habits().records("h").single().progress)
        assertEquals("Semanal", db.habits().records("h").single().frequency)
        repo.refresh(monday + 7)
        val rows = db.habits().records("h")
        assertEquals(2, rows.size)
        assertEquals("Diario", rows.first().frequency)
        assertEquals(0, rows.first().progress)
    }

    @Test fun `profiles are isolated and cannot take ownership of another habit`() = runBlocking {
        repo.save(item(), today)
        val other = RoomHabitRepository(db, "another-user")
        other.refresh(today)
        assertTrue(other.snapshots.first { it.user != null }.habits.isEmpty())
        try { other.save(item(), today); fail("Must not transfer another user's habit") } catch (_: IllegalArgumentException) { }
        assertEquals("local-user", db.habits().habitOwner("h")!!.userId)
    }

    @Test fun `invalid progress is rejected without losing the saved value`() = runBlocking {
        repo.save(item(), today)
        repo.setProgress("h", 5, today)
        try { repo.setProgress("h", 99, today); fail("Invalid progress") } catch (_: IllegalArgumentException) { }
        assertEquals(5, db.habits().records("h").single().progress)
    }

    @Test fun `archived habit stays excluded from weekly counters after database reopening`() = runBlocking {
        repo.save(item("first"), today)
        repo.save(item("second"), today)
        repo.setProgress("first", 8, today)
        repo.setProgress("second", 8, today)
        repo.archive("second", today)
        db.close()
        open()
        repo.refresh(today)

        val snapshot = repo.snapshots.first { it.user != null }
        val activeIds = snapshot.habits.filter { it.archivedDay == null }.map { it.id }.toSet()
        val summary = HabitTracking.summary(snapshot.records, activeIds, today)
        assertEquals(1, summary.completedWeek)
        assertEquals(1, summary.scheduledWeek)
        assertEquals(2, snapshot.records.size)
        assertEquals(1, snapshot.habits.count { it.archivedDay != null })
    }

    @Test fun `original data imports once and deleted or edited rows are not restored`() = runBlocking {
        val seeded = RoomHabitRepository(db, originalData = OriginalHabitData)
        seeded.refresh(today)
        val first = seeded.snapshots.first { it.posts.size == 4 }
        assertEquals(11, first.habits.count { it.archivedDay == null })
        assertEquals(4, first.authors.size)
        assertTrue(db.habits().wasImported("local-user", OriginalDataImport.NAME))
        val water = first.habits.single { it.title == "Beber Agua" }
        seeded.setProgress(water.id, 6, today)
        seeded.archive(water.id, today)
        val beforeReopen = seeded.snapshots.first { data -> data.habits.any { it.id == water.id && it.archivedDay != null } }
        db.close()
        open()
        RoomHabitRepository(db, originalData = OriginalHabitData).refresh(today + 1)
        val after = repo.snapshots.first { it.user != null }
        assertEquals(4, after.posts.size)
        assertEquals(10, after.habits.count { it.archivedDay == null })
        assertEquals(beforeReopen.habits.size, after.habits.size)
        assertEquals(6, after.records.single { it.habitId == water.id && today in it.periodStart..it.periodEnd }.progress)
    }

    @Test fun `original import keeps custom profile and already created habits`() = runBlocking {
        repo.updateProfile("Mi nombre", "mio@example.com")
        repo.save(item("custom").copy(title = "Mi propia meta"), today)
        repo.setProgress("custom", 4, today)
        RoomHabitRepository(db, originalData = OriginalHabitData).refresh(today)
        val snapshot = repo.snapshots.first { it.posts.size == 4 }
        assertEquals("Mi nombre", snapshot.user!!.displayName)
        assertEquals("mio@example.com", snapshot.user!!.email)
        assertEquals(12, snapshot.habits.count { it.archivedDay == null })
        assertEquals(4, snapshot.records.single { it.habitId == "custom" }.progress)
    }
}
