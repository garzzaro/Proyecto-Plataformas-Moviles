# Arquitectura Backend, Concurrencia y Almacenamiento en la Nube
**Proyecto:** HabitOcto (Android)  
**Fecha:** 2026-11-04  
**Destinatario:** Suchini  

---

## 1. Decisión de Backend: Firebase vs Supabase vs Ktor/Node

| Criterio | **Firebase (Recomendado)** | **Supabase** | **Backend Propio (Ktor/Node)** |
| :--- | :--- | :--- | :--- |
| **Tiempo de Entrega / Integración** | **Inmediato**: El proyecto ya tiene `firebase-bom`, plugin Google Services y dependencias listas en `build.gradle.kts`. | Medio: Requiere SDK de Kotlin de Supabase o llamadas HTTP PostgREST. | Lento: Requiere programar servidor, endpoints, despliegue y base de datos. |
| **Sincronización Multi-usuario en Tiempo Real** | **Nativo**: Firestore `SnapshotListener` sincroniza cambios instantáneamente en todos los dispositivos conectados. | Nativo con WebSockets/Realtime Channels de PostgreSQL. | Requiere montar WebSockets o SSE manualmente. |
| **Soporte Offline** | **Integrado**: Firestore tiene caché local offline automática idéntica a Room. | Requiere manejo manual o capa SQLite intermedia. | Manual. |
| **Autenticación** | Firebase Auth (Google, Email/Password, Anónimo) integrado con Google Credential Manager. | Supabase Auth (GoTrue). | JWT/OAuth manual. |
| **Costo Free Tier** | Generoso (Spark Plan): 50k lecturas/día, 20k escrituras/día. | 500 MB DB, 50k usuarios activos mensuales. | Servidor VPS de pago. |

> **Veredicto:** **Firebase (Firestore + Firebase Auth)** es la opción técnica más sólida y rápida porque el proyecto ya tiene la infraestructura de dependencias montada y ofrece sincronización en tiempo real sin fricción.

---

## 2. Almacenamiento de Imágenes: ¿Cloudinary o Firebase Storage?

| Factor | **Cloudinary** | **Firebase Storage** |
| :--- | :--- | :--- |
| **Transformaciones al vuelo** | Extraordinario: redimensiona, recorta rostros (`c_thumb,g_face`), comprime a WebP automáticamente mediante URL. | Limitado: Guarda el archivo tal cual; requiere Cloud Functions o Resize Extension para miniaturas. |
| **Carga en la app** | Extremadamente ligera mediante Coil (`HabitImage`). | Requiere Storage SDK o URL de descarga pública. |
| **Seguridad** | Upload presets firmados o sin firmar con restricciones de tamaño/tipo MIME. | Security Rules basadas en UID de Firebase Auth. |

> **Veredicto:** 
> - Si se requiere **transformación automática de imágenes de perfil y feeds en miniatura sin costo de CPU**, **Cloudinary** es excelente mediante su API REST o SDK.
> - Si se prefiere **unificar todo en un solo ecosistema y proteger accesos por tokens de usuario**, **Firebase Storage** con Security Rules es la alternativa más limpia y nativa.

---

## 3. Manejo de Concurrencia y Sincronización Multi-Usuario

Para que múltiples personas usen la app a la vez y vean los progresos/rankings de amigos en tiempo real:

```
[ Dispositivo A (Suchini) ]                [ Dispositivo B (Amigo) ]
           │                                            │
           ▼                                            ▼
   [ Room DB Local ]                            [ Room DB Local ]
           │                                            │
           ▼ (Offline-First / Sync)                     ▼ (Offline-First / Sync)
   ┌────────────────────────────────────────────────────────────┐
   │                  Firebase Firestore Cloud                  │
   │  /users/{userId}                                          │
   │  /users/{userId}/habits/{habitId}                         │
   │  /users/{userId}/records/{period}                         │
   │  /rankings/{weekId}/participants/{userId}                 │
   │  /friendships/{friendshipId}                              │
   └────────────────────────────────────────────────────────────┘
```

### Reglas de Concurrencia:
1. **Transacciones de Firestore (`runTransaction`)**: Para rankings y conteo de puntos compartidos, evitando condiciones de carrera (*race conditions*).
2. **Escrituras con `FieldValue.serverTimestamp()`**: Garantiza que el orden de actividad no dependa del reloj del teléfono del usuario.
3. **Idempotencia de registros**: Las claves compuestas `userId_habitId_periodStart` impiden duplicaciones cuando dos dispositivos sincronizan con intermitencia de red.

---

## 4. Seguridad de la Aplicación

1. **Autenticación Fuerte**:
   - Cada usuario solo tiene permisos de escritura en su propio subárbol `/users/{userId}/**`.
2. **Firestore Security Rules**:
   ```javascript
   rules_version = '2';
   service cloud.firestore {
     match /databases/{database}/documents {
       // Perfil público legible por todos, editable sólo por el dueño
       match /users/{userId} {
         allow read: if request.auth != null;
         allow write: if request.auth != null && request.auth.uid == userId;
       }
       // Hábitos y registros privados
       match /users/{userId}/habits/{habitId} {
         allow read, write: if request.auth != null && request.auth.uid == userId;
       }
       // Rankings semanales: lectura pública, escritura sólo con avance válido
       match /rankings/{weekId}/participants/{userId} {
         allow read: if request.auth != null;
         allow write: if request.auth != null && request.auth.uid == userId 
                      && request.resource.data.score >= 0;
       }
     }
   }
   ```
3. **Validación de Archivos**:
   - En subida de imágenes: límite estricto de 5MB, tipos MIME permitidos (`image/jpeg`, `image/png`, `image/webp`).
   - Ocultar claves API en `.env` mediante el plugin `secrets-gradle-plugin` (ya configurado en el build).
