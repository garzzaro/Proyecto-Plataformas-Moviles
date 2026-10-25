# Demo de Fase 2 · aproximadamente 10 minutos

## Plan y alcance · 1 minuto

Explicar el objetivo de persistir los datos del seguimiento. Abrir `AVANCE_FASE2.md`, identificar lo implementado y explicar pendientes. Recuperar antes de clase el plan y feedback anteriores.

## Flujo principal y persistencia · 4 minutos

1. Ejecutar la app en el emulador/dispositivo. No desinstalar ni usar Borrar datos durante la prueba.
2. En Tracker, crear `Demo Fase 2`, un hábito de cantidad con meta 5.
3. Registrar 3 y comprobar que la tarjeta muestra 3/5 sin recargar manualmente.
4. Editar el nombre a `Demo Fase 2 guardada` y guardar.
5. En Perfil, cambiar el nombre y alternar el tema.
6. En Feed, crear una publicación local y darle Me gusta.
7. Cerrar completamente: Ajustes de Android → Aplicaciones → HabitOcto → Forzar detención. Alternativa con el dispositivo conectado: `adb shell am force-stop com.aistudio.habitocto.profile`.
8. Abrir desde el lanzador. Verificar nombre del hábito, 3/5, perfil, tema, publicación y reacción.
9. Completar y comprobar contadores. Archivar el hábito y mostrar que desaparece de activos pero conserva historial.

Crear un dato nuevo permite distinguir la persistencia de una lista precargada. Las publicaciones y reacciones son locales. Probar vacío en otro emulador si no quieren alterar su rutina real.

## Recorrido por el código · 3 minutos

- `ui/AddHabitDialog.kt`: captura campos y emite `HabitDraft`.
- `viewmodel/MainViewModel.kt`: `saveHabitDraft()` valida, llama al repositorio y cierra tras guardar.
- `model/HabitRepository.kt`: contrato e implementación; `save()` escribe en una transacción.
- `model/HabitDatabase.kt`: entidades, DAO con `Flow` y `suspend`, singleton y migración.
- Regreso: `observeUser()` → `HabitSnapshot` → `render()` → `MainUiState` → Compose con ciclo de vida.
- Mostrar `HabitRepositoryTest`, `HabitMigrationTest` y `HabitViewModelTest`.

## Feedback y preguntas · 2 minutos

Mostrar comentarios reales y evidencia en `FEEDBACK.md`. Si no se recuperaron, explicar la limitación. Registrar feedback nuevo al terminar.

Practicar entre todos:

- ¿Por qué `remember` no conserva datos al terminar el proceso?
- ¿Qué contiene la entidad y qué contiene `MainUiState`?
- ¿Por qué usar repositorio en lugar de llamar al DAO desde el ViewModel?
- ¿Cómo se actualiza Compose después de escribir?
- ¿Qué evita repetir importaciones o duplicar registros de un período?
- ¿Por qué archivar conserva historial? ¿Cómo protege la migración los datos?
- ¿Cómo se distinguen un fallo de carga y uno de guardado?

## Evidencia manual

Guardar capturas antes y después de Forzar detención, dispositivo, versión Android, fecha y resultado. Esta guía no afirma que la prueba manual de la nueva versión ya se haya ejecutado.
