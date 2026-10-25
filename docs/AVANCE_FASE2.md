# Avance contra la planificación

Objetivo informado por el equipo: agregar una base de datos para guardar y recuperar los datos de HabitOcto. El equipo confirmó que la app recibida corre en su entorno. No se proporcionó un cronograma con fechas; la tabla registra el avance técnico, sin inventar compromisos anteriores.

| Tarea | Estado | Evidencia / siguiente acción |
| --- | --- | --- |
| Guardar hábitos y microhábitos | Implementada | Entidades, DAO y `RoomHabitRepository.save()`. |
| Guardar avances e historial | Implementada | `HabitRecordEntity`, transacciones y pruebas de archivo/reapertura. |
| Guardar perfil, tema, publicaciones y reacciones | Implementada | `users`, `feed_posts` y prueba de reapertura. |
| Actualizar pantallas desde Room | Implementada | `observeUser()` → repositorio → `StateFlow` → `collectAsStateWithLifecycle()`. |
| Separar responsabilidades MVVM/UDF | Implementada | `AppContainer`, `HabitDraft`, ViewModel y pantallas con callbacks. |
| Carga, vacío, éxito, error y reintento | Implementada | `dataState`, `DataStateContent` y pruebas de recuperación. |
| Conservar datos de la versión anterior | Implementada | `MIGRATION_1_2`, esquemas y prueba de migración. |
| Documentar arquitectura y uso de IA | Completada | `README.md`. |
| Demo de cerrar/reabrir la nueva versión | Pendiente del equipo | Seguir `DEMO_FASE2.md`. |
| Recuperar feedback de UI | Pendiente del equipo | Consultar al catedrático/integrantes y completar `FEEDBACK.md`. |
| PR a main y enlace en Canvas | Pendiente del equipo | Revisar rama, crear PR y publicar su enlace. |

## Ajustes del alcance

- El ZIP ya tenía persistencia. Se reutilizó esa base para conservar datos y evitar dos fuentes de verdad.
- Se completaron errores de carga, reintento y validación fuera de la UI porque el documento los exige.
- Se conserva la versión 2 de la base y la migración; no se borran datos ni se cambian dependencias.
- No hay información para afirmar adelantos/retrasos frente a la planificación original. Contrastar con el plan entregado si tenía fechas.
- Firebase y seguridad quedan para la siguiente fase mencionada en la consigna.

Antes de presentar: asignar responsables y fechas reales a los pendientes y comparar esta tabla con la planificación entregada al catedrático.
