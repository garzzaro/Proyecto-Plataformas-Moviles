package com.example

import android.content.Context
import android.graphics.Bitmap
import android.graphics.Color
import androidx.test.core.app.ApplicationProvider
import com.example.model.*
import kotlinx.coroutines.runBlocking
import org.junit.Assert.*
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class HabitOctoComprehensiveTest {

    private lateinit var context: Context

    @Before
    fun setUp() {
        context = ApplicationProvider.getApplicationContext()
        PhotoVerificationAnalyzer.resetRecentHashes()
        // Limpiar SharedPreferences de CloudSyncManager
        context.getSharedPreferences("habitocto_cloud_sync_prefs", Context.MODE_PRIVATE)
            .edit()
            .clear()
            .commit()
    }

    // =========================================================================
    // a) PhotoVerificationAnalyzer Tests
    // =========================================================================
    @Test
    fun `colorful bitmap with gradient and spatial variance passes verification`() {
        val width = 64
        val height = 64
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                val r = (x * 4).coerceIn(0, 255)
                val g = (y * 4).coerceIn(0, 255)
                val b = ((x + y) * 2).coerceIn(0, 255)
                bitmap.setPixel(x, y, Color.rgb(r, g, b))
            }
        }

        val result = PhotoVerificationAnalyzer.analyzeBitmapAndDiscard(bitmap, "Agua", checkDuplicate = true)

        assertTrue("Colorful bitmap should be verified", result.isValid)
        assertTrue("Confidence percent should be within 82..99", result.confidencePercent in 82..99)
        assertTrue("Zero database bytes confirmed", result.zeroDatabaseBytesConfirmed)
        assertTrue("Dynamic range score should be > 0", result.dynamicRangeScore > 0)
        assertTrue("Quadrant texture score should be > 0", result.quadrantTextureScore > 0)
        assertFalse("Duplicate rejected should be false on first run", result.duplicateRejected)
    }

    @Test
    fun `completely black or solid plane bitmap is rejected`() {
        val bitmapBlack = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val resBlack = PhotoVerificationAnalyzer.analyzeBitmapAndDiscard(bitmapBlack, "Agua", checkDuplicate = true)
        assertFalse("Black bitmap should be rejected", resBlack.isValid)

        val bitmapSolid = Bitmap.createBitmap(64, 64, Bitmap.Config.ARGB_8888)
        val canvas = android.graphics.Canvas(bitmapSolid)
        canvas.drawColor(Color.rgb(120, 120, 120))
        val resSolid = PhotoVerificationAnalyzer.analyzeBitmapAndDiscard(bitmapSolid, "Agua", checkDuplicate = true)
        assertFalse("Solid plane bitmap should be rejected", resSolid.isValid)
    }

    @Test
    fun `consecutive identical bitmaps detect duplicate via dHash`() {
        val width = 64
        val height = 64
        val bitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        for (y in 0 until height) {
            for (x in 0 until width) {
                bitmap.setPixel(x, y, Color.rgb((x * 3) % 255, (y * 5) % 255, 100))
            }
        }

        val firstResult = PhotoVerificationAnalyzer.analyzeBitmapAndDiscard(bitmap, "Agua", checkDuplicate = true)
        assertTrue("First analysis should pass", firstResult.isValid)

        val secondResult = PhotoVerificationAnalyzer.analyzeBitmapAndDiscard(bitmap, "Agua", checkDuplicate = true)
        assertTrue("Duplicate rejected flag must be true", secondResult.duplicateRejected)
        assertFalse("Second analysis must not be valid (duplicate)", secondResult.isValid)
    }

    // =========================================================================
    // b) HabitTracking.summary(...) Tests
    // =========================================================================
    @Test
    fun `summary calculates overall and best streaks across all habits including micro and regular`() {
        val today = 20_005L
        val records = listOf(
            HabitRecordEntity("h1", today - 2, today - 2, "Diario", false, false, "", 1, 1, today - 2),
            HabitRecordEntity("h1", today - 1, today - 1, "Diario", false, false, "", 1, 1, today - 1),
            HabitRecordEntity("h2", today, today, "Diario", true, false, "", 1, 1, today)
        )

        val summary = HabitTracking.summary(records, setOf("h1", "h2"), today)

        assertEquals("Micro streak only counts micro-habits (today only)", 1, summary.microStreak)
        assertEquals("Overall streak should be 3 across regular + micro habits", 3, summary.overallStreak)
        assertEquals("Best overall streak should be 3", 3, summary.bestOverallStreak)
    }

    // =========================================================================
    // c) CloudSyncManager Authentication & Unique Tag Tests
    // =========================================================================
    @Test
    fun `initial clean session is null and registration assigns unique tag and hashes password`() = runBlocking {
        val manager = CloudSyncManager(context)
        assertNull("Initial session should be null", manager.currentSession.value)

        val regResult = manager.registerAccount("Suchini", "suchini@habitocto.com", "secure123")
        assertTrue("Registration should succeed", regResult.isSuccess)

        val session = regResult.getOrThrow()
        assertTrue("Is logged in", session.isLoggedIn)
        assertEquals("Suchini", session.displayName)
        assertEquals("suchini@habitocto.com", session.email)

        val tagNum = session.tagCode.toIntOrNull()
        assertNotNull("Tag code should be numeric", tagNum)
        assertTrue("Tag should be in range 100..999", tagNum != null && tagNum in 100..999)

        assertEquals("Password hash length should be 64 (SHA-256)", 64, session.passwordHash.length)
        assertNotEquals("Password should not be stored in plain text", "secure123", session.passwordHash)
    }

    @Test
    fun `registering second account with same name assigns different unique tag and rejects duplicate email`() = runBlocking {
        val manager = CloudSyncManager(context)
        val res1 = manager.registerAccount("UserOne", "user1@habitocto.com", "pass123")
        assertTrue(res1.isSuccess)
        val tag1 = res1.getOrThrow().tagCode

        // Mismo nombre con correo distinto asigna un #Tag irrepetible diferente
        val res2 = manager.registerAccount("UserOne", "user2@habitocto.com", "pass123")
        assertTrue(res2.isSuccess)
        val tag2 = res2.getOrThrow().tagCode

        assertNotEquals("Tags must be different for different accounts", tag1, tag2)

        // Mismo correo duplicado debe rechazarse
        val duplicateEmailResult = manager.registerAccount("OtraPersona", "user1@habitocto.com", "pass123")
        assertTrue("Duplicate email registration must fail", duplicateEmailResult.isFailure)
    }

    @Test
    fun `authenticateAccount fails with wrong password and succeeds with correct password`() = runBlocking {
        val manager = CloudSyncManager(context)
        val reg = manager.registerAccount("AuditUser", "audit@habitocto.com", "mypassword").getOrThrow()

        val authWrong = manager.authenticateAccount(reg.email, "wrongpass")
        assertTrue("Authentication with wrong password must fail", authWrong.isFailure)

        val authRight = manager.authenticateAccount(reg.email, "mypassword")
        assertTrue("Authentication with correct password must succeed", authRight.isSuccess)
        assertEquals("AuditUser", authRight.getOrThrow().displayName)
    }

    // =========================================================================
    // d) Shared Feed & Duplicate Comments Tests
    // =========================================================================
    @Test
    fun `publishCloudFeedPost and addCommentToCloudFeedPost ignore duplicate comments`() = runBlocking {
        val manager = CloudSyncManager(context)
        manager.registerAccount("Poster", "poster@habitocto.com", "pass123")

        val postResult = manager.publishCloudFeedPost(
            postId = "post_test_1",
            habitTitle = "Meditación",
            caption = "Completado hoy con éxito",
            imageUri = null
        )
        assertNotNull("Post publishing should succeed", postResult)

        val posts = manager.syncedFeedPosts.value
        assertFalse("Feed posts should not be empty", posts.isEmpty())
        val postId = postResult!!.id

        manager.addCommentToCloudFeedPost(postId, "c1", "¡Excelente trabajo!")
        // Intentar añadir exactamente el mismo comentario del mismo autor con otro id
        manager.addCommentToCloudFeedPost(postId, "c2", "¡Excelente trabajo!")

        val updatedPost = manager.syncedFeedPosts.value.first { it.id == postId }
        assertEquals("Duplicate comments must be filtered out, total count should be 1", 1, updatedPost.comments.size)
    }

    // =========================================================================
    // e) Pet Mini-Game Happiness vs XP Tests
    // =========================================================================
    @Test
    fun `boostPetHappinessFromMiniGame increases pet happiness bonus correctly`() = runBlocking {
        val manager = CloudSyncManager(context)
        assertEquals("Initial happiness bonus should be 0", 0, manager.petHappinessBonus.value)

        manager.boostPetHappinessFromMiniGame(12)
        assertEquals("Pet happiness bonus should be increased by 12", 12, manager.petHappinessBonus.value)

        manager.boostPetHappinessFromMiniGame(5)
        assertEquals("Pet happiness bonus should accumulate to 17", 17, manager.petHappinessBonus.value)
    }

    // =========================================================================
    // f) External CDN Helpers & Legacy Passwordless Session Rejection
    // =========================================================================
    @Test
    fun `isRemoteCdnUrl recognizes external HTTP and HTTPS URLs only`() {
        assertTrue(ImageOptimizer.isRemoteCdnUrl("https://iili.io/sample123.jpg"))
        assertTrue(ImageOptimizer.isRemoteCdnUrl("http://files.catbox.moe/sample123.jpg"))
        assertFalse(ImageOptimizer.isRemoteCdnUrl("data:image/jpeg;base64,/9j/4AAQ"))
        assertFalse(ImageOptimizer.isRemoteCdnUrl("file:///storage/emulated/0/photo.jpg"))
        assertFalse(ImageOptimizer.isRemoteCdnUrl(null))
    }

    @Test
    fun `legacy persisted active session without passwordHash is cleared on startup`() {
        val legacyJson = """{"userId":"local-user","displayName":"Legacy","email":"legacy@test.com","tagCode":"555","isLoggedIn":true,"passwordHash":""}"""
        context.getSharedPreferences("habitocto_cloud_sync_prefs", Context.MODE_PRIVATE)
            .edit()
            .putString("active_auth_session", legacyJson)
            .commit()

        val manager = CloudSyncManager(context)
        assertNull("Legacy session without passwordHash must not auto-login", manager.currentSession.value)
    }
}
