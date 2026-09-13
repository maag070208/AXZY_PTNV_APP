# PuertoNuevoApp — Plan de desarrollo

App KMP (Compose Multiplatform) que replica el sistema completo de la web "Cartas Responsivas" (API: `../api`, Web: `../web`).

## Arquitectura

La app usa arquitectura KMP estándar (`core/` + `feature/<dominio>`). NO copiar FSD de la web al pie.

| Web (FSD) | App KMP (actual) | Estado |
|---|---|---|
| `shared/` | `core/` (network, session, nav, theme, util) | ✓ equivalente |
| `entities`+`features`+`widgets` | `feature/<dominio>` (tickets, cartas, devices…) | ✓ 1:1 con módulos API |
| Redux slices | AuthState/StateFlow + ViewModel | ✓ |
| HashRouter/PrivateRoutes | `Navigator` + `Screen` enum + `PlatformBackHandler` | ✓ |
| Axios client + session 401 | Ktor `ApiClient` + `TokenStore` (expect/actual) | ✓ |

Lo que une a web y app es el **contrato del API** (módulos, DTOs, JWT, tablas server-side `page/limit/filters/sort`, roles, Ably). Ya mapeado 1:1 en `core/network/*`.

NO se hereda de web: `axzy_ui_system` (CSS), Tailwind, `@react-pdf`, HashRouter, Redux.

## Estado actual

**Listo:** login con servidor editable sin recompilar (`ApiConfig.DEFAULT_BASE_URL`), shell responsive (drawer ≥700dp / bottom bar móvil), tickets completo (lista/detalle/nuevo/editar/kanban/mis tareas/admin), dispositivos, usuarios, departamentos, device types, empleados, inventario+movimientos+ubicaciones, salidas, cartas, reportes, notificaciones, auditoría. Ktor + kotlinx.serialization.

**Faltante vs web (paridad "todo el sistema"):**
- Realtime tickets/notificaciones (Ably) — sin SDK KMP maduro; opciones: SDK KMP alpha de Ably o poll en `NotificationsApi`.
- Push notifications (FCM Android / APNs iOS) — controlado por API.
- Cámara/fotos para adjuntos de tickets/inventario: picker multiplataforma (FileKit o expect/actual) + multipart.
- PDF de cartas: endpoint en API (`GET /cartas/:id/pdf`); la app solo abre la URL. Reusa lógica, evita motor PDF en cliente.
- Firma en cartas (canvas) si aplica.
- Offline/cache: `multiplatform-settings` + SQLDelight para catálogos (device types, locations, users).
- CI/CD y distribución: builds Android gradle + iOS Xcode, App Distribution/TestFlight.
- Tests: solo placeholders; empezar por repos + parse de tablas server-side.

## Fases

### Fase 0 — Paridad core (base sólida primero) ✅
- [x] Interceptor único de errores: `ApiException` tipado + 401 → logout automático (igual `web/src/shared/api/session.ts`).
- [x] DTOs: barrido vs schemas del API (crash fixes: DELETE `/locations/:id` → `{success}`; kanban `department` sin `id`; paridad `cartaContador`, `estado/locationId` en devices, `category/departmentId` en tickets, `deviceId` en salidas, `email`/`empresa`).
- [x] Helper `TableRequest`/`TableResponse` (espejo de `web/src/shared/api/table.ts`).
- [x] Errores de red visibles por UI: `AppSnackbar` central montado en el shell + `networkMessage()` amigable.
- Verificación: `:androidApp:assembleDebug` + `:shared:compileKotlinIosArm64` ✓

**Siguiente:** adoptar `AppSnackbar` en pantallas (sustituir `catch (e: Exception)` por `networkMessage(e)` + `AppSnackbar.showError`) y reusar `TableRequest` en los listados que el API sirva con POST/query.

**Verificación:** `./gradlew :androidApp:assembleDebug` y `./gradlew :shared:compileKotlinIosArm64`.

### Fase 1 — Datos/resiliencia
- `multiplatform-settings`: base URL + preferencias.
- SQLDelight: cache catálogos + tickets recientes; pull-to-refresh; badge de sin conexión.

### Fase 2 — Realtime + push
- Decidir mecanismo realtime (Ably KMP vs poll).
- Push FCM/APNs registrando token en API; badge de notificaciones.

### Fase 3 — Multimedia
- FileKit/expect-actual picker cámara+galería; subida multipart a tickets/inventario; firma.

### Fase 4 — PDFs
- Endpoint API `GET /cartas/:id/pdf`; app abre con visor externo.

### Fase 5 — UX móvil
- LazyColumn con paginación server-side (el API ya la tiene), infinite scroll, skeletons, dark mode, landscape.

### Fase 6 — Entrega
- CI (GitHub Actions): build APK release + iOS framework; App Distribution/TestFlight; versionado de `API_BASE_URL` por entorno.

Cada fase es un milestone independiente.