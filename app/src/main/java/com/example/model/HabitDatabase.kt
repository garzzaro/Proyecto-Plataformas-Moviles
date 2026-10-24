package com.example.model

import android.content.Context
import androidx.room.*
import androidx.room.migration.Migration
import androidx.sqlite.db.SupportSQLiteDatabase
import kotlinx.coroutines.flow.Flow
import org.json.JSONArray

// ============================================================================
// ENTIDAD DE USUARIO: UserEntity
// ============================================================================

/**
 * UserEntity representa el perfil persistente del dispositivo.
 *
 * - **¿Para qué sirve?** Conserva la identidad, los datos del perfil y la preferencia de tema al
 *   cerrar la app.
 * - **¿Cómo funciona?** Room usa id como clave primaria; los hábitos y publicaciones se relacionan con
 *   este usuario.
 * - **¿Qué pasa si lo quitamos?** No habría un propietario estable para los datos ni un perfil
 *   recuperable.
 */
@Entity(tableName = "users")
data class UserEntity(
    @PrimaryKey val id: String,
    val displayName: String = "Mi perfil",
    val email: String? = null,
    val avatarUrl: String? = null,
    val createdDay: Long,
    val isDarkMode: Boolean = false
)

// ============================================================================
// ENTIDAD DE HÁBITO: HabitEntity
// ============================================================================

/**
 * HabitEntity almacena la definición de cada hábito o microhábito.
 *
 * - **¿Para qué sirve?** Guarda su meta, frecuencia, categoría y preferencias sin mezclar la
 *   definición con el avance diario.
 * - **¿Cómo funciona?** Se vincula a users mediante userId; archivedDay permite retirarlo de la lista
 *   activa conservando sus registros.
 * - **¿Qué pasa si lo quitamos?** No se podrían recuperar los hábitos ni conservar sus relaciones
 *   históricas.
 */
@Entity(
    tableName = "habits",
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("userId")]
)
data class HabitEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val title: String,
    val subtitle: String,
    val category: String,
    val frequency: String,
    val isProgressive: Boolean,
    val isMicroHabit: Boolean,
    val target: Int,
    val progressUnit: String,
    val tags: List<String>,
    val timeOrDuration: String,
    val categoryColorHex: Long,
    val createdDay: Long,
    // Primera fecha de esta definición; los registros anteriores conservan su propia copia.
    val effectiveDay: Long,
    val reminderTime: String = "08:00",
    val reminderEnabled: Boolean = false,
    val archivedDay: Long? = null
)

// ============================================================================
// ENTIDAD DE CUMPLIMIENTO: HabitRecordEntity
// ============================================================================

/**
 * HabitRecordEntity representa una ocurrencia de cumplimiento de un hábito.
 *
 * - **¿Para qué sirve?** Conserva el avance y la meta de cada día o semana, incluso después de editar
 *   o archivar el hábito.
 * - **¿Cómo funciona?** La clave habitId/periodStart evita duplicados; la copia de la definición
 *   protege el historial y completedDay identifica el día real del logro.
 * - **¿Qué pasa si lo quitamos?** El avance dependería de la memoria y no se podrían calcular el
 *   historial ni las rachas.
 */
@Entity(
    tableName = "habit_records",
    primaryKeys = ["habitId", "periodStart"],
    foreignKeys = [ForeignKey(entity = HabitEntity::class, parentColumns = ["id"], childColumns = ["habitId"], onDelete = ForeignKey.RESTRICT)],
    indices = [Index("periodEnd"), Index("completedDay")]
)
data class HabitRecordEntity(
    val habitId: String,
    val periodStart: Long,
    val periodEnd: Long,
    // Copia de la definición de este período, también cuando todavía no tiene avance.
    val title: String,
    val isMicroHabit: Boolean,
    val isProgressive: Boolean,
    val timeOrDuration: String,
    val target: Int,
    val progress: Int = 0,
    val completedDay: Long? = null,
    val frequency: String = "Diario"
) {
    // Se calcula desde el avance: no se guarda un segundo estado que pueda quedar desactualizado.
    @get:Ignore val completed: Boolean get() = progress >= target
}

// ============================================================================
// ENTIDAD DE PUBLICACIÓN: FeedPostEntity
// ============================================================================

/**
 * FeedPostEntity representa una publicación local y su reacción.
 *
 * - **¿Para qué sirve?** Evita perder las publicaciones al salir y permite relacionarlas con el perfil
 *   de su autor.
 * - **¿Cómo funciona?** Guarda texto, categoría, fecha y referencia de imagen; userId mantiene la
 *   relación con users.
 * - **¿Qué pasa si lo quitamos?** El Feed volvería a depender de publicaciones simuladas o temporales.
 */
@Entity(
    tableName = "feed_posts",
    // El propietario del Feed y el autor de una publicación pueden ser usuarios distintos.
    foreignKeys = [
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT),
        ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["authorId"], onDelete = ForeignKey.RESTRICT)
    ],
    indices = [Index("userId"), Index("createdAt"), Index("authorId")]
)
data class FeedPostEntity(
    @PrimaryKey val id: String,
    val userId: String,
    val content: String,
    val category: String,
    val imageUrl: String?,
    val createdAt: Long,
    val isLiked: Boolean = false,
    val authorId: String? = null,
    @ColumnInfo(defaultValue = "0") val baseLikesCount: Int = 0,
    @ColumnInfo(defaultValue = "'Logro personal'") val habitTitle: String = "Logro personal",
    val authorStreak: Int? = null
)

// ============================================================================
// CONTROL DE IMPORTACIONES: DataImportEntity
// ============================================================================

/**
 * DataImportEntity registra las cargas iniciales ya realizadas para cada perfil.
 *
 * - **¿Para qué sirve?** Evita duplicar los datos originales o restaurar hábitos que el usuario borró.
 * - **¿Cómo funciona?** Guarda una marca única por usuario y versión de importación en la misma
 *   transacción que inserta los datos.
 * - **¿Qué pasa si lo quitamos?** La carga podría repetirse cada vez que se abre la aplicación.
 */
@Entity(
    tableName = "data_imports",
    primaryKeys = ["userId", "name"],
    foreignKeys = [ForeignKey(entity = UserEntity::class, parentColumns = ["id"], childColumns = ["userId"], onDelete = ForeignKey.RESTRICT)]
)
data class DataImportEntity(val userId: String, val name: String, val importedDay: Long)

// ============================================================================
// CONVERSORES DE DATOS: HabitConverters
// ============================================================================

/**
 * HabitConverters convierte las etiquetas entre Kotlin y SQLite.
 *
 * - **¿Para qué sirve?** Permite guardar listas de etiquetas sin perder comas, comillas ni caracteres
 *   especiales.
 * - **¿Cómo funciona?** Serializa List<String> como un arreglo JSON y reconstruye la lista al
 *   consultar Room.
 * - **¿Qué pasa si lo quitamos?** Room no podría guardar directamente las etiquetas de un hábito.
 */
class HabitConverters {
    @TypeConverter fun encodeTags(tags: List<String>): String = JSONArray(tags).toString()
    @TypeConverter fun decodeTags(value: String): List<String> {
        val array = JSONArray(value)
        return List(array.length()) { array.getString(it) }
    }
}

// ============================================================================
// RELACIONES DE LECTURA: UserWithData Y HabitWithRecords
// ============================================================================

/**
 * UserWithData agrupa el perfil y sus datos relacionados.
 *
 * - **¿Para qué sirve?** Entrega a la pantalla una lectura coherente del usuario, sus hábitos y sus
 *   publicaciones.
 * - **¿Cómo funciona?** Las relaciones de Room se leen mediante una consulta con @Transaction,
 *   incluyendo los registros de cada hábito.
 * - **¿Qué pasa si lo quitamos?** Habría que coordinar consultas separadas con riesgo de mostrar
 *   contadores desactualizados.
 */
data class UserWithData(
    @Embedded val user: UserEntity,
    @Relation(entity = HabitEntity::class, parentColumn = "id", entityColumn = "userId") val habits: List<HabitWithRecords>,
    @Relation(entity = FeedPostEntity::class, parentColumn = "id", entityColumn = "userId") val posts: List<PostWithAuthor>
)

/**
 * PostWithAuthor conserva la relación entre una publicación y el perfil de su autor.
 *
 * - **¿Para qué sirve?** Mantiene los autores originales sin atribuir todos los posts al perfil local.
 * - **¿Cómo funciona?** Room consulta users.id a través del authorId opcional del post.
 * - **¿Qué pasa si lo quitamos?** Se perderían el nombre y avatar de los autores al reconstruir el Feed.
 */
data class PostWithAuthor(
    @Embedded val post: FeedPostEntity,
    @Relation(parentColumn = "authorId", entityColumn = "id") val author: UserEntity?
)

/**
 * HabitWithRecords reúne un hábito con todas sus ocurrencias.
 *
 * - **¿Para qué sirve?** Permite conservar y consultar el historial aunque el hábito esté archivado.
 * - **¿Cómo funciona?** Relaciona habits.id con habit_records.habitId sin duplicar las filas
 *   persistidas.
 * - **¿Qué pasa si lo quitamos?** El resumen de pantalla no recibiría los registros asociados a cada
 *   hábito.
 */
data class HabitWithRecords(
    @Embedded val habit: HabitEntity,
    @Relation(parentColumn = "id", entityColumn = "habitId") val records: List<HabitRecordEntity>
)

// ============================================================================
// ACCESO A DATOS: HabitDao
// ============================================================================

/**
 * HabitDao define las consultas y escrituras autorizadas sobre Room.
 *
 * - **¿Para qué sirve?** Centraliza el acceso a perfiles, hábitos, cumplimientos y publicaciones.
 * - **¿Cómo funciona?** Expone Flow para cambios reactivos y funciones suspend para operaciones de
 *   base de datos fuera de la interfaz.
 * - **¿Qué pasa si lo quitamos?** El repositorio no podría consultar ni persistir los datos de la
 *   aplicación.
 */
@Dao
interface HabitDao {
    // ============================================================================
    // PREPARACIÓN Y CONTROL DE LA IMPORTACIÓN ORIGINAL
    // ============================================================================

    @Query("SELECT * FROM users WHERE id = :userId")
    suspend fun user(userId: String): UserEntity?

    @Query("SELECT * FROM habits WHERE userId = :userId")
    suspend fun allHabits(userId: String): List<HabitEntity>

    @Query("SELECT EXISTS(SELECT 1 FROM data_imports WHERE userId = :userId AND name = :name)")
    suspend fun wasImported(userId: String, name: String): Boolean

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun markImported(dataImport: DataImportEntity)

    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertPosts(posts: List<FeedPostEntity>)

    @Query("UPDATE users SET displayName = :name, avatarUrl = :avatarUrl WHERE id = :userId")
    suspend fun setInitialProfile(userId: String, name: String, avatarUrl: String?)

    // ============================================================================
    // CONSULTAS REACTIVAS Y LECTURAS POR PROPIETARIO
    // ============================================================================

    @Transaction
    @Query("SELECT * FROM users WHERE id = :userId")
    fun observeUser(userId: String): Flow<UserWithData?>

    @Query("SELECT r.* FROM habit_records r INNER JOIN habits h ON h.id = r.habitId WHERE h.userId = :userId ORDER BY r.periodStart DESC")
    fun observeRecords(userId: String): Flow<List<HabitRecordEntity>>

    @Query("SELECT * FROM habits WHERE userId = :userId AND archivedDay IS NULL")
    suspend fun activeHabits(userId: String): List<HabitEntity>

    @Query("SELECT * FROM habits WHERE id = :id AND userId = :userId")
    suspend fun habit(id: String, userId: String): HabitEntity?

    @Query("SELECT * FROM habits WHERE id = :id")
    suspend fun habitOwner(id: String): HabitEntity?

    @Query("SELECT * FROM habit_records WHERE habitId = :id ORDER BY periodStart DESC")
    suspend fun records(id: String): List<HabitRecordEntity>

    // ============================================================================
    // ESCRITURAS Y ACTUALIZACIONES PERSISTENTES
    // ============================================================================

    // IGNORE conserva el perfil existente; volver a cargar la app no lo reinicia.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertUser(user: UserEntity)

    @Upsert
    suspend fun saveHabit(habit: HabitEntity)

    // Crear períodos faltantes nunca reemplaza el avance de un registro existente.
    @Insert(onConflict = OnConflictStrategy.IGNORE)
    suspend fun insertRecords(records: List<HabitRecordEntity>)

    @Upsert
    suspend fun saveRecord(record: HabitRecordEntity)

    @Upsert
    suspend fun savePost(post: FeedPostEntity)

    @Query("UPDATE feed_posts SET isLiked = NOT isLiked WHERE id = :id AND userId = :userId")
    suspend fun toggleLike(id: String, userId: String)

    @Query("UPDATE users SET isDarkMode = :enabled WHERE id = :userId")
    suspend fun setDarkMode(userId: String, enabled: Boolean)

    @Query("UPDATE users SET displayName = :name, email = :email WHERE id = :userId")
    suspend fun updateProfile(userId: String, name: String, email: String?)
}

// ============================================================================
// BASE DE DATOS ROOM: HabitDatabase
// ============================================================================

/**
 * HabitDatabase configura la base local y sus entidades.
 *
 * - **¿Para qué sirve?** Mantiene los datos en el archivo habitocto.db y proporciona el DAO al
 *   repositorio.
 * - **¿Cómo funciona?** Comparte una instancia basada en applicationContext y exporta el esquema para
 *   preparar futuras migraciones.
 * - **¿Qué pasa si lo quitamos?** No existiría una conexión persistente y única para recuperar los
 *   datos al reabrir la app.
 */
@Database(entities = [UserEntity::class, HabitEntity::class, HabitRecordEntity::class, FeedPostEntity::class, DataImportEntity::class], version = 2, exportSchema = true)
@TypeConverters(HabitConverters::class)
abstract class HabitDatabase : RoomDatabase() {
    abstract fun habits(): HabitDao

    companion object {
        /**
         * MIGRATION_1_2 amplía la base para cargar los datos originales una sola vez.
         *
         * - **¿Para qué sirve?** Conserva los datos de la versión anterior al añadir autores y la
         *   marca de importación.
         * - **¿Cómo funciona?** Añade columnas opcionales o con valores por defecto, sus relaciones
         *   e índices y una tabla de control; no elimina ni recrea las tablas existentes.
         * - **¿Qué pasa si lo quitamos?** Una instalación con la versión 1 no podría abrir el nuevo
         *   esquema conservando sus datos.
         */
        val MIGRATION_1_2 = object : Migration(1, 2) {
            override fun migrate(db: SupportSQLiteDatabase) {
                db.execSQL("ALTER TABLE feed_posts ADD COLUMN authorId TEXT REFERENCES users(id) ON UPDATE NO ACTION ON DELETE RESTRICT")
                db.execSQL("ALTER TABLE feed_posts ADD COLUMN baseLikesCount INTEGER NOT NULL DEFAULT 0")
                db.execSQL("ALTER TABLE feed_posts ADD COLUMN habitTitle TEXT NOT NULL DEFAULT 'Logro personal'")
                db.execSQL("ALTER TABLE feed_posts ADD COLUMN authorStreak INTEGER")
                db.execSQL("CREATE INDEX IF NOT EXISTS index_feed_posts_authorId ON feed_posts(authorId)")
                db.execSQL("CREATE TABLE IF NOT EXISTS data_imports (userId TEXT NOT NULL, name TEXT NOT NULL, importedDay INTEGER NOT NULL, PRIMARY KEY(userId, name), FOREIGN KEY(userId) REFERENCES users(id) ON UPDATE NO ACTION ON DELETE RESTRICT)")
            }
        }
        @Volatile private var instance: HabitDatabase? = null
        fun get(context: Context): HabitDatabase = instance ?: synchronized(this) {
            instance ?: Room.databaseBuilder(context.applicationContext, HabitDatabase::class.java, "habitocto.db")
                .addMigrations(MIGRATION_1_2)
                // Al aumentar la versión, añadir migraciones explícitas; no borrar los datos del usuario.
                .build().also { instance = it }
        }
    }
}
