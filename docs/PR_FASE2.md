# Fase 2: completar MVVM, estados y validación sobre Room

HabitOcto conserva la persistencia de hábitos, avances, perfil y publicaciones. La carga representa vacío, éxito y error además de carga, y permite recuperar una observación fallida sin dejar el indicador de progreso activo.

La creación de dependencias pasa a `AppContainer`; el ViewModel depende solo de `HabitRepository`. El formulario emite un borrador y delega su validación. Se conservan tablas y migración.

Se añaden pruebas de validación, errores/reintentos, emisiones de Room y migración 1→2. El README incluye diagrama, avance, uso de IA y guía de demo.

## Validación

Consultar `docs/VALIDACION.md` para los resultados reales. En la rama final ejecutar:

```powershell
.\gradlew.bat :app:assembleDebug :app:testDebugUnitTest :app:lintDebug
```

## Pendientes del equipo

- Registrar la demo de cerrar/reabrir en dispositivo/emulador con la versión nueva.
- Recuperar feedback anterior y completar evidencia.
- Contrastar avance con el calendario original si tenía fechas.
- Revisar todas las diferencias contra `main`, incluidos los avances anteriores de la rama de frontend.

Esta copia local no publica un PR. Crearlo hacia `main` y entregar su enlace en Canvas tras la revisión del equipo.
