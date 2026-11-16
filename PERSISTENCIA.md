# HabitOcto: seguimiento y persistencia local

Se conservan las carpetas y pantallas existentes. Los nuevos archivos de datos están en `model`, el estado continúa en `viewmodel/MainViewModel.kt` y la interfaz en `ui` y `ui/components`.

## Qué se guarda

La base Room `habitocto.db` tiene cinco tablas:

| Tabla | Contenido y relaciones |
| --- | --- |
| `users` | Perfil local, nombre, correo opcional, avatar opcional, fecha de creación y tema. |
| `habits` | Propietario, definición del hábito o microhábito, frecuencia, meta, categoría, etiquetas, duración, preferencias de recordatorio y fecha de archivo. |
| `habit_records` | Una ocurrencia por hábito y período, avance, meta, fecha de cumplimiento y copia de la definición correspondiente. |
| `feed_posts` | Propietario y autor relacionados con `users`, texto, categoría, imagen, fecha, reacciones y contexto del hábito. |
| `data_imports` | Marca única por usuario y carga original, para evitar duplicados o restaurar hábitos eliminados. |

Las claves foráneas impiden registros huérfanos. La clave compuesta `(habitId, periodStart)` evita duplicar cumplimientos del mismo período. Los identificadores nuevos de hábitos y publicaciones son UUID. El perfil inicial tiene el identificador estable `local-user`; esto representa un perfil del dispositivo, sin autenticación remota. El repositorio acepta otro identificador de usuario y limita las operaciones a su propietario.

Los hábitos eliminados se archivan, conservando sus registros. Los contadores, gráficos y porcentajes semanales se recalculan sobre los hábitos que siguen activos: eliminar uno de dos hábitos completados cambia `2/2` a `1/1`. El historial, las rachas y el total de logros conservan los registros archivados. Editar una definición no reescribe períodos cerrados. Las consultas de pantalla leen usuario, hábitos, registros y publicaciones en una transacción; las escrituras y la creación de períodos también usan transacciones. No se realizan consultas de base de datos en el hilo de la interfaz.

## Reglas de seguimiento

- **Diario:** un registro por fecha local. Al cambiar de día, el avance vuelve a cero sin modificar días anteriores.
- **Semanal:** una meta por semana de lunes a domingo. Si el hábito se crea a mitad de semana, el primer período comienza ese día. El avance se conserva hasta el domingo.
- **Cambio de frecuencia:** entra en vigor después del período abierto. No corta ni duplica el período existente.
- **Pendiente:** sin avance. **En progreso:** avance mayor que cero sin llegar a la meta. **Completado:** meta alcanzada. Los períodos vencidos sin completar se identifican como tales en el historial.
- Los hábitos de cantidad tienen entrada numérica, botones de incremento/decremento y opción de deshacer. La duración es estimada: su cumplimiento se confirma manualmente; no se simula un temporizador.
- **Racha general:** días consecutivos con al menos un microhábito completado. Varias completaciones el mismo día cuentan como un día. Hasta que termine hoy, se conserva la racha que llegó hasta ayer. Un día anterior sin microhábitos completados corta la racha.
- **Racha individual:** períodos consecutivos completados de la misma frecuencia; el período abierto pendiente no rompe la racha anterior.
- **Historial:** hoy y los seis días anteriores. Se conserva todo el historial en la base para funcionalidades futuras. Al reabrir la app, se crean los períodos pendientes de los días sin actividad, sin inventar avances.
- **Estadísticas:** metas actuales completadas/programadas, cumplimiento de los últimos siete días, días con actividad, racha actual y mejor racha de microhábitos. El nivel usa un paso por cada diez cumplimientos registrados.
- Cada ocurrencia semanal se cuenta una vez en el gráfico: en la fecha de cumplimiento, o en su fecha límite si sigue pendiente. Para la semana abierta pendiente se utiliza hoy. Por ello el gráfico representa ocurrencias, no duplica una meta semanal siete veces.
- Las fechas se almacenan como días civiles y las publicaciones como marcas de tiempo. Se utiliza la zona horaria del dispositivo. El estado se actualiza al volver a la app y al detectar un cambio de fecha.

## Pantallas y datos reales

Al abrir la aplicación después de esta actualización, se importan una sola vez los 11 hábitos originales, sus estados y avances actuales, las 12 entradas de historial y las 4 publicaciones con sus respectivos autores, imágenes y reacciones. Dos hábitos que solo figuraban en el historial se guardan archivados, sin añadirlos a la rutina activa. Las antiguas listas de las pantallas delegan en `OriginalHabitData`; las pantallas continúan leyendo su estado desde Room.

La importación se aplica en una transacción y registra la marca `original-project-data-v1`. Volver a ejecutar la aplicación no duplica los datos, no reinicia avances y no restaura hábitos que se hayan archivado después de la carga. Si ya existe un hábito equivalente, se conserva su definición y su progreso, y se evitan registros con períodos solapados. Los datos y el perfil personalizados se mantienen. El nombre y avatar del perfil original solo se incorporan si el perfil todavía conserva sus valores iniciales.

Las etiquetas históricas originales no incluían año: se conserva su día y mes y se elige la fecha más reciente que no sea futura al importar. Las horas relativas de las publicaciones se anclan al momento de esta carga. No se copian porcentajes ni rachas fijos de la antigua interfaz: se calculan desde los cumplimientos guardados. Las rachas incluidas en las publicaciones se conservan como parte de su contexto original.

Se conservan las tres pestañas. Cada una conserva su estado de desplazamiento; Volver desde Feed o Perfil regresa al seguimiento. El formulario valida nombres, metas enteras positivas y horas. Solo se cierra después de guardar correctamente; los errores se muestran y conservan el formulario abierto.

El perfil y el tema persisten. Las fotos del Feed se seleccionan desde el dispositivo usando el selector de documentos y un permiso de lectura persistente. Un proveedor que no concede ese permiso se rechaza con un mensaje. Las publicaciones y reacciones son locales; no se representan como una comunidad conectada.

Las preferencias de recordatorio se guardan, pero no se han programado notificaciones automáticas. Contraseñas y verificación en dos pasos requieren autenticación futura; la interfaz no presenta una verificación activa inexistente.

## Extensión de la base de datos

`HabitRepository` define el contrato que consume el ViewModel. `RoomHabitRepository` lo implementa localmente. Para añadir un servidor se puede implementar sincronización conservando este contrato y los componentes de pantalla. Los campos de presentación (`HabitItem`, `HabitHistoryItem` y `TrackingSummary`) continúan separados de las entidades persistentes.

Los esquemas de las versiones 1 y 2 están en `app/schemas/com.example.model.HabitDatabase/`. `MIGRATION_1_2` añade los campos y relaciones de autor y la tabla de importaciones, conservando todos los datos existentes. Al ampliar las entidades, incrementar `version` y definir una migración explícita o una migración automática compatible. Conservar el esquema anterior y probar la migración. No se usa `fallbackToDestructiveMigration` ni se reinicia la base para actualizarla. Referencia: [documentación de Room 2.x](https://developer.android.com/training/data-storage/room/v2).

## Verificación

Se añadieron pruebas para:

- Fechas locales, ventanas de siete días, rachas, días omitidos y metas semanales.
- Carga inicial, errores al guardar y conservación de navegación en el ViewModel.
- Reapertura de Room, persistencia de perfil/tema/publicaciones, archivo sin borrar historial, aislamiento de usuarios y cambios de frecuencia.
- Controles de cumplimiento, cantidades grandes y capturas de pantallas clara/oscura a 360 dp de ancho.
- Importación de las listas originales, fechas, autores y cantidades, reutilización de hábitos existentes, carga única y conservación de ediciones tras reabrir.

En Android Studio se pueden ejecutar `HabitTrackingTest`, `HabitViewModelTest`, `HabitOriginalDataTest`, `HabitRepositoryTest` y `HabitFrontendTest`. Por terminal:

```powershell
.\gradlew.bat :app:assembleDebug :app:lintDebug
.\gradlew.bat :app:testDebugUnitTest --tests "com.example.Habit*"
```

Resultados en esta sesión: compilación directa de todo el Kotlin y del código generado por Room; generación y validación de las consultas de Room con KSP; 21 pruebas de lógica, ViewModel e importación aprobadas; 20 comprobaciones SQLite de migración e integridad aprobadas, comparando el esquema migrado con el generado por Room y conservando los datos de la versión 1. Los cinco archivos de pruebas compilan.

La compilación completa con Gradle y las pruebas Android/Robolectric con capturas no pudieron ejecutarse en esta sesión por restricciones de acceso a archivos internos de Gradle y recursos Android. Se intentó la compilación con los recursos disponibles; no se cambiaron las versiones ni las rutas del SDK del proyecto. Queda pendiente ejecutar esos dos grupos de pruebas y revisar las capturas desde Android Studio antes de dar por validada la apariencia en un dispositivo.
