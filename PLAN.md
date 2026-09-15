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

**Paridad completa de módulos alcanzada (post-reset).** Tras el reset
arquitectónico (solo login+home), se re-agregaron **los 12 módulos** de la
web al patrón MVVM nuevo, en el orden documentado más abajo. Los ~30
destinos de `Screen.kt` tienen todos una pantalla real — cero
"próximamente".

**Vivo ahora (core):** `core/theme` (Brand + Color + Theme), `core/ui`
(Brand: logo/insignia/backdrop océano · Components · AppSnackbar), `core/nav`
(Navigator + `Screen` con ~30 destinos + `ScreenChrome` + `PlatformBackHandler`),
`core/session` (AuthRepository/TokenStore/expect-actual + `SessionUser.can*`
= matriz de autorización del API), `core/network` (arquitectura limpia, ver
abajo, un subpaquete por dominio), `core/di` (AppContainer = composition
root), `core/util` (DateFormat/Labels/CurrentTime expect-actual, sin
dependencias externas).

**Vivo ahora (features) — MVVM (UiState+ViewModel+Screen) en los 12 módulos:**
- `feature/auth` — login + hero brand.
- `feature/home` — saludo + resumen de inventario + grid de accesos rápidos
  a todos los módulos (gateado por rol).
- `feature/tickets` — lista/detalle/alta-edición, kanban de tareas,
  mis tareas/tareas del equipo.
- `feature/devices` + `feature/devicetypes` — CRUD completo, catálogo de
  tipos con `fieldConfig` por campo.
- `feature/departments`, `feature/users`, `feature/employees` — CRUD +
  wizard de alta de usuario + historial de actividad.
- `feature/inventory` — ubicaciones (+ sublugares), movimientos/kardex,
  resumen, registrar movimiento (incluye flujo de "malas condiciones").
- `feature/salidas` — bitácora F-SIS-0005.
- `feature/cartas` — CRUD, generar por tipo, marcar/deshacer devolución.
- `feature/reports`, `feature/notifications` (+ badge en AppShell),
  `feature/audit` (ADMIN).

Bottom-nav: 4 secciones (Home/Tickets/Devices/Users — Users solo si
`canManageCatalogs`); el resto cuelga de accesos rápidos en Home.

Verificación: `./gradlew :shared:compileAndroidMain` +
`:shared:compileKotlinIosSimulatorArm64` + `:androidApp:assembleDebug` ✓

## Arquitectura limpia — core/network

Capa de datos separada por responsabilidad: infra, dominio de datos (endpoints +
DTOs) y repositorios. Dependencias SIEMPRE hacia adentro (dominio → infra,
nunca al revés).

```
core/network/
  http/                    ← infraestructura (independiente de cualquier módulo)
    ApiClient.kt             HTTP generico (Ktor): body→DTO, errores tipados,
                             401 → onUnauthorized. Sin dependencias: recibe
                             proveedores getToken/getServerUrl (IoC, ver abajo).
    ApiConfig.kt             URL default + ApiException + networkMessage()
    ApiErrorBody.kt          shape del body de error del backend
  auth/                    ← modulo de datos auth (1:1 con endpoints del API)
    AuthApi.kt               AuthDtos.kt
  devices/                 ← modulo de datos devices
    DevicesApi.kt            DeviceDtos.kt
```

Reglas:
- Un `*Api` (endpoints) vive con su `*Dtos` en el mismo subpaquete de dominio.
- `ApiClient` NO conoce `TokenStore` ni dominio (inversión de dependencias):
  `AppContainer` le inyecta `getToken`/`getServerUrl`. El único acoplamiento
  permitido es 401 → `onUnauthorized` (logout global).
- Los ViewModels usan repositorios (p. ej. `AuthRepository`), no `*Api`
  directo — se agrega `DevicesRepository` cuando el home crezca.
- `TableRequest/TableResponse` (tablas server-side) vuelven con TicketsList
  dentro del subpaquete de su dominio.

**Módulos re-agregados (en orden), cada uno = MVVM + marca — TODOS ✅:**
1. ✅ `TicketsList` (patrón con paginación server-side) — kickoff del resto.
2. ✅ Devices + DeviceTypes (con DevicesApi.summary ya en uso en home).
3. ✅ Departments / Employees / Users.
4. ✅ Inventory + Locations + Movements.
5. ✅ Salidas / Cartas (+ generar por tipo) / Reports / Notifications / Audit.
6. ⬜ Realtime (Ably) y push — sigue pendiente, ver abajo.

**Faltante vs web (deliberadamente fuera de esta pasada — no es falta de
paridad de módulos, sino de capacidades transversales):**
- Realtime tickets/notificaciones (Ably) — sin SDK KMP maduro; opciones: SDK KMP alpha de Ably o poll en `NotificationsApi`.
- Push notifications (FCM Android / APNs iOS) — controlado por API.
- Cámara/fotos para adjuntos (tickets, historial de dispositivo): picker multiplataforma (FileKit o expect/actual) + multipart.
- Importación Excel (usuarios, dispositivos): mismo picker + multipart.
- PDF de cartas: se genera 100% client-side en la web (`@react-pdf/renderer`), sin endpoint en el API — la app ofrece una vista en pantalla de solo lectura en su lugar (`CartaDetailScreen`).
- Exportación CSV de reportes.
- Offline/cache: `multiplatform-settings` + SQLDelight para catálogos.
- CI/CD y distribución: builds Android gradle + iOS Xcode.
- Tests: repos + parse de tablas server-side.
- `DepartmentDetailScreen`/`LocationDetailScreen` muestran ubicaciones/tickets/cartas de solo lectura; agregar/quitar el vínculo departamento↔ubicación (`POST/DELETE /departments/:id/locations`) no tiene UI todavía.

## Fase Rebranding — "que se vea mamalona"

Objetivo: aplicar la identidad real de Puerto Nuevo (logo + colores) y nivelar
la UX de la app KMP con la web. Lo primero es la arquitectura de tema, NO
retocar pantalla por pantalla.

### Arquitectura del tema

| Capa | Archivo | Responsabilidad |
|---|---|---|
| Tokens de marca | `core/theme/Brand.kt` (nuevo) | Paleta EXTRAÍDA del logo (`extract` del PNG): azul océano primario, hielo claro, brick acento, neutros azulados. Nombres de rol (Primary/Container/Accent…), sin valores sueltos. |
| Tokens semánticos | `core/theme/Color.kt` | Estados (Success/Warning/Danger/Info/Purple) + helpers por dominio (ticket/device/inventory). Solo estado, no marca. |
| Capa compat | `AppColors` alias → tokens de marca | El valor vive en UN punto; al cambiar ahí cambia toda la app. Los 38 archivos existentes siguen compilando igual. |
| Tema M3 | `core/theme/Theme.kt` | `lightColorScheme(primary=Ocean, onPrimary=White, primaryContainer=Ice, onPrimaryContainer=MarineDark, secondary=Brick, background=TinteHielo…)`. Todo componente M3 y los tokens se propagan solos. |
| Marca (composables) | `core/ui/Brand.kt` (nuevo) | `BrandLogo(modifier)` (painterResource del PNG), `BrandHeader()` (logo+wordmark), `BrandTopBar()` coloreado. Reutilizable en login, shell, home. |
| Recurso | `composeResources/drawable/logo_puerto_nuevo.png` | PNG del logo (mismo de `web/public`), cargado con `Res.drawable`. |
| Pantallas | `feature/*` | Regla dura: usar `MaterialTheme.colorScheme.{primary,primaryContainer,secondary,surface…}` y `AppColors.*` SOLO para semánticos. Cero `Color(0xFF…)` suelto en pantallas. |

Diagrama de dependencias (mismo orden que hoy, no cambia):

```
core/theme/{Brand,Color,Theme}  ← nada por encima las importa directo salvo AppColors/theme
core/ui/{Brand,Components}      ← usa tokens
feature/*/Screen                ← usa MaterialTheme + AppColors(semánticos) + core/ui
App.kt (PuertoNuevoTheme)       ← monta el tema en la raíz
```

### Paleta extraída del logo (255px PNG, buckets dominantes)

| Rol | Hex | Muestra |
|---|---|---|
| Primary — Azul océano | `#03587D` | (3,88,125) |
| PrimaryContainer — Hielo | `#D8EBF5` | (216,235,245) |
| Ice medio | `#79A6B7` / `#B0CBD2` | degradados/trazos |
| MarineDark (onContainer) | `#013449` | texto sobre hielo |
| Accent — Brick | `#6B413F` | secundario/marcas |
| Background — Tinte hielo | `#F4F9FB` | fondo |
| Surface / SurfaceVariant | `#FFFFFF` / `#E8F0F4` | tarjetas |

Los semánticos actuales (Success `#06C167`, Warning, Danger, Info, Purple) se
conservan: la app los usa para estados reales de tickets/dispositivos.

### Pasos (milestones independientes, cada uno verifica con build)

1. **B1 tokens+theme** ✅: `core/theme/Brand.kt` (paleta del logo), `Theme.kt`
   recableado a `Brand`, `AppColors` = capa compat (marca en un solo punto).
   Toda la app repintó sin tocar las 38 pantallas.
2. **B2 assets+marca** ✅: `logo_puerto_nuevo.png` en composeResources/drawable;
   `core/ui/Brand.kt` con `BrandLogo`, `BrandLogoBadge`, `OceanBackdrop`.
   `packageOfResClass` fijado en shared/build.gradle.kts.
3. **B3 login** ✅: `feature/auth` a MVVM + hero brand (logo real, gradiente
   océano, olas). Patrón de referencia para el resto de pantallas.
4. **B4 shell** ✅: top bar de marca (logo + nombre + logout). El drawer/bottom
   bar multipantalla volverá con cada módulo re-agregado.
5. **B5 home** ✅: saludo + resumen de inventario en marca (StatCards).
6. **B6 sweep 38 archivos**: con el reset, casi todas las pantallas se
   re-escriben con `MaterialTheme.colorScheme.*`; la capa compat en `AppColors`
   se retira cuando deje de haber referencias `Emerald*`.
7. **B7 pulido**: espaciados, radios (shapes redondeados tipo logo), estados
   hover/selected consistentes.

Verificación continua: `./gradlew :shared:compileAndroidMain` (Android) y
`:shared:compileKotlinIosSimulatorArm64` (iOS).

## Fase MVVM — patrón de pantalla (app "fuerte")

Migración progresiva feature por feature. NO FSD de web; se conserva
`core/` + `feature/<dominio>`. Cada pantalla pasa a 3 archivos y la
lógica se vuelve testeable en `commonTest` (repos + ViewModel, sin UI).

Estructura por feature (referencia: `feature/auth` ya migrado):

```
feature/<dominio>/
  <X>UiState.kt      data class inmutable (loading/error/data…) + defaults
  <X>ViewModel.kt    ViewModel KMP (jetbrains androidx.lifecycle) + StateFlow
  <X>Screen.kt       UI tonta: viewModel { … } + collectAsState + eventos
```

Reglas:
- ViewModel multiplataforma REAL: `androidx.lifecycle.ViewModel` +
  `viewModelScope` + `viewModel { VM(deps) }` (de `org.jetbrains.androidx.lifecycle`
  2.11, ya en deps — funciona Android+iOS, sobrevive recomposition/config).
- ViewModel recibe repos por constructor desde `AppContainer` (`viewModel {
  LoginViewModel(AppContainer.authRepository) }`). Sin DI framework.
- Estados con `MutableStateFlow` + `.update {}`; UI lee con `collectAsState()`.
- En éxito de login el `AuthState` de la sesión cambia solo (App.kt decide la
  pantalla): el VM NO navega.
- Transiciones: `state.copy(...)` siempre; nunca mutar el state en UI.
- UI usa `MaterialTheme.colorScheme.*`; `AppColors.*` solo semánticos/estado.

Los 12 módulos ya migrados a este patrón (ver "Estado actual" arriba).

## Fases

### Fase 0 — Paridad core (base sólida primero) ✅
- [x] Interceptor único de errores: `ApiException` tipado + 401 → logout automático (igual `web/src/shared/api/session.ts`).
- [x] DTOs: barrido vs schemas del API vigente (contrastado contra `web/src/entities/*/api/*.ts` módulo por módulo, no solo contra la referencia pre-reset, porque el backend siguió evolucionando — ver notas de drift en cada commit de módulo).
- [x] Helper `TableRequest`/`TableResponse` (`core/network/http/Table.kt`, espejo de `web/src/shared/api/table.ts`), disponible en los 12 dominios.
- [x] Errores de red visibles por UI: `networkMessage()` en cada ViewModel → `uiState.error`.
- Verificación: `:androidApp:assembleDebug` + `:shared:compileKotlinIosSimulatorArm64` ✓

**Siguiente candidato real:** adoptar `TableRequest`/`query()` (ya definido en cada `*Api`) en los listados que hoy usan `list()` + filtro client-side, para paginación server-side real cuando los catálogos crezcan; y considerar `AppSnackbar` para errores no bloqueantes (hoy cada pantalla ya muestra su propio `ErrorState`/mensaje inline, que cubre el caso pero no es una notificación transitoria).

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