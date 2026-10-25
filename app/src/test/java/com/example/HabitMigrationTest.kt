package com.example

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import com.example.model.HabitDatabase
import com.example.model.RoomHabitRepository
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.json.JSONObject
import org.junit.Assert.*
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class HabitMigrationTest {
    @Test fun `version one migrates through Room keeping profile habit progress and post`() = runBlocking {
        val context = ApplicationProvider.getApplicationContext<Context>()
        val name = "m.db"
        val file = context.getDatabasePath(name)
        file.parentFile!!.mkdirs()
        try {
            // Recrea la versión publicada usando su esquema exportado, sin duplicarlo a mano.
            val schema = javaClass.classLoader!!.getResourceAsStream("com.example.model.HabitDatabase/1.json")!!
                .bufferedReader().use { JSONObject(it.readText()).getJSONObject("database") }
            SQLiteDatabase.openOrCreateDatabase(file, null).use { old ->
                val entities = schema.getJSONArray("entities")
                for (i in 0 until entities.length()) {
                    val entity = entities.getJSONObject(i)
                    fun sql(value: String) = value.replace("\${TABLE_NAME}", entity.getString("tableName"))
                    old.execSQL(sql(entity.getString("createSql")))
                    val indices = entity.optJSONArray("indices")
                    if (indices != null) {
                        for (j in 0 until indices.length()) old.execSQL(sql(indices.getJSONObject(j).getString("createSql")))
                    }
                }
                val setup = schema.getJSONArray("setupQueries")
                for (i in 0 until setup.length()) old.execSQL(setup.getString(i))
                old.execSQL("INSERT INTO users VALUES ('local-user','María','maria@example.com',NULL,20000,1)")
                old.execSQL("INSERT INTO habits (id,userId,title,subtitle,category,frequency,isProgressive,isMicroHabit,target,progressUnit,tags,timeOrDuration,categoryColorHex,createdDay,effectiveDay,reminderTime,reminderEnabled,archivedDay) VALUES ('h','local-user','Leer','','Aprendizaje','Diario',1,1,10,'páginas','[]','5 min',0,20000,20000,'08:00',0,NULL)")
                old.execSQL("INSERT INTO habit_records (habitId,periodStart,periodEnd,title,isMicroHabit,isProgressive,timeOrDuration,target,progress,completedDay,frequency) VALUES ('h',20000,20000,'Leer',1,1,'5 min',10,7,NULL,'Diario')")
                old.execSQL("INSERT INTO feed_posts (id,userId,content,category,imageUrl,createdAt,isLiked) VALUES ('p','local-user','Mi logro','Aprendizaje',NULL,1000,1)")
                old.version = 1
            }
            // Abrir con Room ejecuta MIGRATION_1_2 y valida el esquema contra las entidades actuales.
            val db = Room.databaseBuilder(context, HabitDatabase::class.java, name)
                .addMigrations(HabitDatabase.MIGRATION_1_2).build()
            try {
                val snapshot = RoomHabitRepository(db).snapshots.first { it.user != null }
                assertEquals("María", snapshot.user!!.displayName)
                assertTrue(snapshot.user!!.isDarkMode)
                assertEquals("Leer", snapshot.habits.single().title)
                assertEquals(7, snapshot.records.single().progress)
                assertEquals("Mi logro", snapshot.posts.single().content)
                assertTrue(snapshot.posts.single().isLiked)
                assertEquals("Logro personal", snapshot.posts.single().habitTitle)
                assertFalse(db.habits().wasImported("local-user", "prueba"))
            } finally { db.close() }
        } finally { context.deleteDatabase(name) }
    }
}
