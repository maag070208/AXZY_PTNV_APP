# APP_PARITY_PLAN — Paridad funcional Web → App (KMP)

Documento de **plan** (no código). Define qué falta para que `PuertoNuevoApp/` haga todo lo que hace `web/`, con el orden de trabajo y los criterios de aceptación. Fuente de verdad funcional: `web/`. Fuente de verdad de datos: `api/`. Shell ya rediseñado: `PuertoNuevoApp/shared/src/commonMain/kotlin/com/axzydev/puertonuevoapp/feature/home/AppShell.kt` + `core/nav/Screen.kt` + `core/nav/ScreenChrome.kt`.

## 1. Resumen

- **Paridad** = por cada pantalla de `web/src/app/App.tsx` y cada entrada de menú de `web/src/entities/permission/model/screens.ts` (`APP_SCREENS`), la app tiene una pantalla equivalente en `core/nav/Screen.kt`, alcanzable desde el shell con la misma compuerta de permiso, y puede ejecutar las acciones que el API ya expone.
- **Criterio de éxito**: (a) ninguna acción soportada por el API queda sin camino en la app para el rol que la tiene; (b) cada pantalla cumple la regla de reutilización de `PuertoNuevoApp/AGENTS.md`; (c) `./gradlew :shared:compileCommonMainKotlinMetadata` en verde; (d) el API sigue decidiendo — la app solo oculta (ver `core/session/AuthState.kt`).
- **Fuera de alcance de este plan**: **cocina** (apagada en `web/src/shared/config/features.ts` con `FEATURES.kitchen = false`; código y API completos, es fase 4), **escritorio/Electron** (`web/electron/`, instaladores, notificaciones nativas de macOS), **Langostin** (`/langostin` y el agente de `/tickets/new`), **hotel-store** y **mail** (tienen API en `api/src/modules/api.router.ts` pero **no** pantalla web: no son paridad), **i18n es/en** de la web (la app fija español) y **Ably en vivo** si se cierra con sondeo.
- Inventario verificado: **53 pantallas/rutas** → **10 ✅ completas · 18 🟡 parciales · 25 ⬜ no existen**.

## 2. Inventario de pantallas web → app

**Fuentes leídas**: `web/src/app/App.tsx` (rutas y `RequiresPermission`), `web/src/entities/permission/model/screens.ts` (`APP_SCREENS`: rutas, requisitos y `actions`), `web/src/app/guards/PrivateRoutes.tsx` (menú armado desde `APP_SCREENS`, notificaciones), `PuertoNuevoApp/shared/src/commonMain/kotlin/com/axzydev/puertonuevoapp/core/nav/Screen.kt` (37 destinos), `core/nav/ScreenChrome.kt`, `feature/home/AppShell.kt` (`AppNavHost`), `feature/**` de la app, `api/src/modules/api.router.ts` y cada `*/routes/*.routes.ts`, y `web/src/**/api/*.ts` para confirmar qué endpoint pega cada pantalla.

**Correcciones a los documentos previos** (verificadas en código; no se editan aquí):
- `PuertoNuevoApp/FEATURE_PARITY.md` §0 está obsoleto: la app **ya** consume el contrato vigente del API (`core/network/http/ApiConfig.kt` → `/api/v1`; `core/network/**/*Api.kt` usa `/inventory/devices`, `/hr/query`, `/access/query`, etc.). No hay “contrato divergente” pendiente.
- `FEATURE_PARITY.md` §3 está obsoleto: `SessionUser` ya no usa flags fijos sino permisos dinámicos (`core/session/AuthState.kt` + `core/session/PermissionKeys.kt`); el “rol `RECURSOS_HUMANOS` faltante” se resolvió vía `hr.records`.
- `PuertoNuevoApp/WEB_MODULES.md` describe la web **pre-rediseño** (`/inventario`, roles `GERENTE`/`JEFE_DE_AREA`): las rutas y permisos vigentes son los de `App.tsx`/`APP_SCREENS` que usa esta tabla.
- `PuertoNuevoApp/PLAN.md` dice “~30 destinos”: `Screen.kt` tiene **37**.

**Criterios de estado**: ✅ = hay pantalla equivalente y las acciones del API quedan cubiertas; 🟡 = la pantalla existe y navega, pero le falta al menos una acción o un dato de la web; ⬜ = no existe en la app.

La columna “¿Existe en la app?” da el destino de `core/nav/Screen.kt` (o “—” si no hay). Una fila agrupa rutas del mismo módulo (lista, alta, edición, detalle) cuando comparten pantalla y endpoints; si un dato no se pudo verificar en el código, la brecha lo dice explícitamente como “por confirmar”.

| Módulo | Pantalla web (ruta) | Permiso(s) web | ¿Existe en la app? | Estado | Brecha concreta | Endpoints que usa |
|---|---|---|---|---|---|---|
| Inicio | `/` | sesión (widgets por permiso) | `Screen.Home` | 🟡 | Clon bento hecho, con omisiones: tiles de nómina / horas extra / vínculo de relojes / asignar horarios no navegan; widget `people` omitido; sin Ably (refresco 60 s) | `GET /dashboard/{summary,hr-records,people,attendance-today,overtime-week,setup-gaps,tickets,tasks,my-equipment,access-today,system-health}`, `GET /inventory/dashboard` |
| Tickets | `/tickets` | sesión; acciones `tickets.create/edit/close/delete` | `Screen.TicketsList` | ✅ | — | `GET /tickets`, `POST /tickets/query`, `GET /tickets/filter-options`, `GET /tickets/categories` |
| Tickets | `/tickets/kanban` | sesión | `Screen.TicketsKanban` | ✅ | — | `GET /tickets/kanban`, `PUT /tickets/:id/assignments/:assignmentId` |
| Tickets | `/tickets/tasks` | `tasks.complete` | `Screen.AdminTasks` | ✅ | — | `GET /tickets/kanban`, `POST /tickets/:id/assignments` |
| Tickets | `/tickets/my-tasks` | `fixedRole: EMPLOYEE` | `Screen.MyTasks` | ✅ | — | `GET /tickets/kanban` (alcance propio) |
| Tickets | `/tickets/:id` | sesión (alcance `tickets.view` lo resuelve el API) | `Screen.TicketDetail` | 🟡 | Sin adjuntos (subir/descargar), sin PDF del ticket, sin timeline visual de tareas | `GET /tickets/:id`, `POST /tickets/:id/comments`, `GET/POST /tickets/:id/attachments`, `GET /tickets/:id/attachments/:attachmentId/download` |
| Tickets | `/tickets/new` (+ `/:id/edit`) | `tickets.create` / `tickets.edit` | `Screen.NewTicket`, `Screen.EditTicket` | 🟡 | Web captura con Langostin y permite “a nombre de otro” (ADMIN); la app usa formulario clásico sin adjuntos ni `onBehalfOfId` | `POST /tickets`, `PUT /tickets/:id`, `GET /users/employees`, `GET /tickets/categories`, `POST /tickets/:id/attachments` |
| Inventario | `/inventory` | `devices.view`; acción `inventory.audit` | `Screen.InventoryIndex` | 🟡 | Sin panel del auditor (8 reglas), sin dona por estado ni tabla por tipo; solo totales/ubicaciones/movimientos | `GET /inventory/dashboard`, `GET /inventory/audit` |
| Inventario | `/inventory/audit/mismatches` | `inventory.audit` | — | ⬜ | No existe pantalla | `POST /inventory/audit/mismatches/resolve` |
| Inventario | `/inventory/devices` | `devices.view` | `Screen.DevicesList` | 🟡 | Sin “Cargar Excel”; sin columnas con filtro/orden como la web | `GET /inventory/devices`, `GET /inventory/device-types` |
| Inventario | `/inventory/devices/new` | `devices.create` | `Screen.DeviceForm` | 🟡 | La app crea **1** dispositivo; la web da de alta **N unidades** con identidades por pieza (`unit-identities`) | `POST /inventory/devices`, `GET /inventory/device-types` |
| Inventario | `/inventory/devices/:id` | `devices.view` | `Screen.DeviceDetail` | 🟡 | Faltan unidades físicas (editar), disponibilidad/stock, kardex del dispositivo y **“Agregar unidades”** (`STOCK_IN`) | `GET /inventory/devices/:id`, `/units`, `/stock`, `/ledger`, `GET/PUT /inventory/units/:id`, `POST /inventory/movements` (`STOCK_IN`) |
| Inventario | `/inventory/devices/:id/edit` | `devices.edit` | `Screen.DeviceForm` | ✅ | — | `PUT /inventory/devices/:id` |
| Inventario | `/inventory/devices/import` | `devices.create` | — | ⬜ | No existe asistente subir→revisar→confirmar | `GET /inventory/devices/import/template`, `POST /inventory/devices/import/preview`, `POST /inventory/devices/import` |
| Inventario | `/inventory/device-types` | `devices.view` | `Screen.DeviceTypesList` + `DeviceTypeForm` | ✅ | — | `GET/POST/PUT/DELETE /inventory/device-types` |
| Inventario | `/inventory/movements` | `devices.view` | `Screen.InventoryMovements` | 🟡 | Sin revertir, sin comprobante/reporte PDF, sin filtros por columna | `GET /inventory/movements`, `POST /inventory/movements/:id/revert` |
| Inventario | `/inventory/movements/new` | `devices.edit`; alta `devices.create` | `Screen.NewInventoryMovement` | 🟡 | No cubre `STOCK_IN` con unidades nuevas, ni `Idempotency-Key` (la app no la manda) | `POST /inventory/movements`, `GET /inventory/devices`, `/inventory/units` |
| Inventario | `/inventory/loans` | `loans.view` | `Screen.CustodyLettersList` | ✅ | — | `GET /inventory/loans` |
| Inventario | `/inventory/loans/new`, `/inventory/loans/:id/edit` | `loans.create` / `loans.edit` | `Screen.CustodyLetterForm` | ✅ | — | `POST/PUT /inventory/loans`, `GET /users/employees`, `GET /inventory/devices` |
| Inventario | `/inventory/loans/:id` | `loans.view` | `Screen.CustodyLetterDetail` | 🟡 | Devolución simplificada (texto libre, sin condición por pieza ni parcial por unidad); sin PDF (vista en pantalla); “deshacer devolución” responde 501 en la app | `GET /inventory/loans/:id`, `POST /inventory/returns`, `POST /inventory/loans/:id/cancel` |
| Inventario | `/inventory/returns` | `loans.view` | — | ⬜ | No hay bitácora de devoluciones (solo se marcan desde la carta) | `GET /inventory/returns` |
| Inventario | `/inventory/returns/new` | `loans.edit` | — (modal en la carta) | 🟡 | Sin devolución parcial por unidad ni condición BUENO/ACEPTABLE/MALO/ROTO por renglón (ROTO→baja automática) | `POST /inventory/returns` |
| Reportes | `/reports` | `reports.view` | `Screen.Reports` | 🟡 | Solo 2 pestañas (Asignados, Dispositivos); faltan Salidas, resumen por periodo y export PDF/CSV | `POST /reports/query`, `/reports/assigned-devices`, `/reports/devices`, `/reports/period-summary`, `GET /reports/.csv` |
| Reportes | Salidas de material (pestaña dentro de `/reports`) | `reports.view`; alta `material_outputs.register` | `Screen.MaterialOutputsList` + `MaterialOutputForm` | 🟡 | La app tiene pantallas propias (la web no): faltan captura por lote y PDF | `GET/POST/PUT/DELETE /material-outputs`, `POST /material-outputs/batch`, `GET /material-outputs/suggestions` |
| RH | `/employees` | `hr.records`; acciones `users.create` | `Screen.EmployeesList` | ✅ | — | `POST /hr/query` |
| RH | `/employees/records` | `hr.records` | — | ⬜ | Sin tablero de expedientes (lista+detalle+avisos) | `POST /hr/query`, `POST /hr/notify-missing-records`, `POST /hr/:id/notify-missing-records` |
| RH | `/employees/:id` | `hr.records` | `Screen.EmployeeProfile` | 🟡 | Solo lectura: documentos sin subir/eliminar, sin foto, descuentos, avisos ni credencial descargable | `GET /hr/:id`, `/overview`, `/documents`, `/discounts`, `POST /hr/:id/photo`, `DELETE /hr/:id/documents/:docId` |
| RH | `/employees/:id/edit` | `hr.records` | `Screen.UserForm` (parcial) | 🟡 | La app edita cuenta + datos básicos; falta el asistente de expediente (domicilio, fiscales, médicos, descuentos) | `PATCH /hr/:id/profile`, `PUT /hr/:id/discounts` |
| RH | `/employees/disciplinary-reports` (`/:id`) | `hr.records`; acción `hr.disciplinary_reports` | — | ⬜ | No existen actas administrativas (alta, detalle, PDF) | `POST /hr/disciplinary-reports/query`, `GET/POST/DELETE /hr/disciplinary-reports*` |
| RH | `/employees/catalogs/documents` | `hr.records` | — | ⬜ | Catálogo de tipos de documento sin CRUD | `GET/POST/PATCH/DELETE /hr/catalogs/document-types` |
| Usuarios | `/users` | `users.view` (menú; ruta sin guard) | — | ⬜ | **No hay lista de cuentas** en la app; “Personal” usa `/hr/query` | `POST /users/query`, `PUT /users/:id/password`, `PATCH /users/:id/{deactivate,reactivate}` |
| Usuarios | `/users/new`, `/users/:id/edit` | `users.create` / `users.edit` | `Screen.UserForm` | ✅ | — | `POST /users`, `PUT /users/:id`, `GET /users/:id` |
| Usuarios | `/users/:id/history` | `users.view` | — | ⬜ | Sin historial de cuenta (la app tiene bitácora global, no por usuario) | `GET /users/:id/history` |
| Usuarios | `/users/import` | `users.create` | — | ⬜ | Sin importación Excel de usuarios | `POST /users/import` |
| Configuración | `/catalogs` | `catalogs.manage` | — (solo tipos de dispositivo) | ⬜ | Faltan 7 de 8 pestañas (solo existe el catálogo de tipos de dispositivo, fuera de esta pantalla): departamentos, subáreas, tipos de documento, géneros, tipos de sangre, categorías de ticket y notificaciones (`sys_config`) | `GET/PUT /sys-config`, `GET/POST/PATCH/DELETE /hr/catalogs/{document-types,genders,blood-types}`, `/tickets/categories` |
| Configuración | `/roles` | `roles.manage` | `Screen.RolesPermissions` | 🟡 | Solo pestañas Matriz y Catálogo; faltan Roles, Acceso por persona, Políticas ABAC, simulador y Actividad | `GET /permissions/admin`, `PUT /permissions/matrix`, `GET/POST/PATCH/DELETE /permissions/roles*`, `/permissions/policies*`, `/permissions/members*`, `POST /permissions/simulate`, `POST /permissions/activity/query` |
| Reloj | `/hr/time-clock` (asistencia) | `time_clock.view` | — | ⬜ | No existe módulo de reloj en la app | `POST /time-clock/query`, `GET /time-clock/status`, `GET /time-clock/summary` |
| Reloj | `/hr/time-clock/entries-exits` | `time_clock.view` | — | ⬜ | No existe | `POST /time-clock/report`, `POST /time-clock/report/export` |
| Reloj | `/hr/time-clock/roster` | `time_clock.view` | — | ⬜ | No existe | `GET /time-clock/roster`, `POST /time-clock/roster/run` |
| Reloj | `/hr/time-clock/employees` (+ `/differences`) | `time_clock.view` | — | ⬜ | No existe padrón del reloj ni diferencias | `POST /time-clock/employees/query`, `POST /time-clock/employees/link-suggested`, `GET /time-clock/employees/:userId/clock-status` |
| Reloj | `/time-clocks`, `/time-clocks/dashboard`, `/time-clocks/people`, `/time-clocks/inspect` | `time_clocks.manage` | — | ⬜ | Alta/baja de relojes, tablero e inspección Hikvision sin pantalla | `GET/POST/PATCH/DELETE /time-clock/clocks*`, `GET /time-clock/clocks/:serial/{inspect/*,events,people}`, `POST /time-clock/import`, `POST /time-clock/sync` |
| Horarios | `/schedules`, `/schedules/new`, `/schedules/:id/edit` | `schedules.view` | — | ⬜ | No existe administración de horarios | `GET/POST/PATCH/DELETE /schedules*` |
| Horarios | `/schedules/assign` | `schedules.view` | — | ⬜ | No existe asignación masiva | `POST /schedules/assignments`, `/assignments/remove`, `/assignments/query`, `GET /schedules/:id/assignees` |
| Horarios | `/schedules/overtime/approval` | `overtime.view` | — | ⬜ | No existe aprobación de tiempo extra | `POST /overtime/query`, `POST /overtime/approvals` |
| Horarios | `/schedules/payroll` | `payroll.view` | — | ⬜ | No existe prenómina | `POST /schedules/weekly-attendance`, `POST /schedules/overtime/export`, `POST /schedules/attendance/{time-clock,access}` |
| Acceso | `/access` | `access.log`; acción `access.void` | `Screen.AccessScan` + `Screen.AccessLog` | 🟡 | Sin anular registro ni estadísticas del día; se conserva escaneo QR + bitácora | `POST /access/lookup`, `/access/events`, `/access/query`, `GET /access/sites`, `POST /access/:id/void`, `POST /access/stats` |
| Acceso | `/access/report` | `access.log` | — | ⬜ | No existe reporte entradas/salidas por persona | `POST /access/report`, `POST /access/report/export` |
| Notificaciones | `/notifications` | sesión | `Screen.Notifications` | 🟡 | Sin Ably; la insignia se refresca al navegar (no hay sondeo) | `GET /notifications`, `/unread-count`, `POST /read-all`, `/:id/read`, `DELETE /:id` |
| Cocina (oculta) | `/kitchen`, `/kitchen/items`, `/kitchen/items/:id` | `kitchen.view` | — | ⬜ | Módulo apagado en web (`FEATURES.kitchen=false`); no entra ahora | `POST /kitchen/items/table`, `GET /kitchen/items/:id`, `GET /kitchen/restock`, `/alerts` |
| Cocina (oculta) | `/kitchen/lots`, `/movements`, `/stock-in`, `/stock-out`, `/count` | `kitchen.view/stock_in/stock_out/adjust` | — | ⬜ | Ídem (fase 4) | `POST /kitchen/lots/table`, `/movements/table`, `/movements/{stock-in,stock-out,adjustments}`, `/movements/fefo-preview`, `/movements/:id/reverse` |
| Cocina (oculta) | `/kitchen/restock`, `/purchase-orders*` | `kitchen.view`, `purchase_orders.*` | — | ⬜ | Ídem (fase 4) | `POST /kitchen/purchase-orders/table`, `/purchase-orders`, `/:id/{approve,send,receive,cancel}` |
| Cocina (oculta) | `/kitchen/invoices`, `/kitchen/suppliers` | `invoices.*`, `kitchen.manage` | — | ⬜ | Ídem (fase 4) | `POST /kitchen/invoices/table`, `/invoices`, `POST /suppliers/table`, `GET/POST/PATCH /kitchen/suppliers*` |
| Cocina (oculta) | `/kitchen/catalog`, `/kitchen/cost-centers` | `kitchen.manage`, `purchase_orders.view` | — | ⬜ | Catálogos de cocina (IVA, unidades, categorías) + centros de costo | `GET/POST/PATCH /kitchen/{tax-rates,units,categories,cost-centers}` |
| Langostin | `/langostin` | `system.configure` | — | ⬜ | Galería de la mascota: **no entra** (solo web) | — |

**Resumen del inventario: 53 filas · 10 ✅ · 18 🟡 · 25 ⬜.** Pantallas que la app tiene y la web no (no cuentan como brecha, pero sí son deuda a revisar): `MyCredential`, `AuditLogs`, `Locations*` (departamentos duplicados como “ubicaciones”, ver §7) y `MaterialOutputs*` (en la web es una pestaña de Reportes).

## 3. Brechas transversales

| Capacidad | Web (referencia) | App hoy | Estrategia / archivo |
|---|---|---|---|
| Notificaciones en vivo | Ably (`web/src/app/guards/useAblyNotifications.ts`, `web/src/shared/lib/ably.ts`) + notificación nativa en escritorio | Solo se refresca al navegar (`AppShell.kt`, `LaunchedEffect(current)`); el Inicio refresca cada 60 s (`feature/home/HomeScreen.kt`, `REFRESH_MS`) | Sondeo de `/notifications/unread-count` cada 30–60 s en `core/network/notifications/NotificationsApi.kt` + insignia; Ably solo si aparece SDK KMP estable (decisión abierta) |
| Permisos dinámicos | `usePermission` / `<RequiresPermission>` (`web/src/entities/user/model/usePermission.ts`, `web/src/app/guards/RequirePermission.tsx`) | `SessionUser.can/claves` (`core/session/AuthState.kt`) con subconjunto fijo en `core/session/PermissionKeys.kt` | Toda pantalla nueva agrega su clave a `PermissionKeys.kt` y su compuerta; nunca roles hardcodeados; el API revalida |
| Tablas server-side | `ITDataTable` + `tableRequest` (`web/src/shared/api/table.ts`), filtro y orden por columna | `core/ui/table/AppTableScreen.kt` + `PaginatedTableViewModel.kt`, usados solo por `feature/hr/EmployeesListScreen.kt` | Adoptar el componente en cada listado nuevo; un `TableFilterSpec` por columna con datos, como la web |
| Formularios con Langostin | Agente en `/tickets/new` y “Nueva tarea” (`web/src/features/ticket/ticket-detail/...`) | Formularios clásicos | Fuera de alcance por ahora; si se pide, chat + endpoints de Langostin (`api/src/modules/...`), no reimplementar prompts |
| Exportación PDF | Client-side con `@react-pdf/renderer` (`web/src/widgets/*-pdf`, `web/src/shared/pdf`) | Nada: vistas en pantalla (`CustodyLetterDetailScreen.kt` lo documenta) | PDF en móvil **no** se genera client-side; requiere endpoint nuevo en `api/` (p. ej. `GET /inventory/loans/:id/pdf`) o compartir el PDF de la web — decisión abierta |
| Exportación CSV | `GET /reports/.csv`, `POST /access/report/export`, `POST /time-clock/report/export`, `POST /schedules/overtime/export` | Nada (`feature/reports/ReportsViewModel.kt` lo deja fuera) | Descargar el CSV del API y compartir con el `sharing` de plataforma (expect/actual en `core/`) |
| Carga masiva por Excel | Asistentes subir→revisar→confirmar (`web/src/features/inventory/device-import`, `features/kitchen/import`, `/users/import`) | Nada; `core/network/http/ApiClient.kt` **no** soporta `multipart` | Extender `ApiClient` con `multipart/form-data`, añadir picker de archivos (expect/actual) y reutilizar el flujo de 3 pasos; plantillas ya las sirve el API |
| Adjuntos/multimedia | `POST /tickets/:id/attachments`, `POST /hr/:id/photo` (multipart) | Sin picker ni multipart (misma brecha que Excel) | Misma base técnica que Excel: resolver una vez y reutilizar (`core/ui/RemoteImage.kt` ya existe para mostrar) |
| Cocina oculta | `FEATURES.kitchen=false` → sin menú, sin tablero, rutas montadas | Sin módulo | Fase 4; no bloquear nada más por ella |
| Modo oscuro | Tema de la web con `localStorage["it-theme-dark-mode"]` | Solo claro (`core/theme/Theme.kt` sin `darkColorScheme`; ver `REDESIGN_BENTO.md`) | Fase 5: migrar `AppColors.*` fijos a tokens y activar `darkColorScheme` (no pantalla por pantalla) |
| Electron / escritorio | `web/electron/main.cts`, protocolo `app://ptnv`, notificaciones nativas | No aplica (Android/iOS) | Fuera de alcance; no portar |
| Sesión endurecida | Refresh tokens (`POST /auth/refresh`) y cambio forzado de contraseña (`mustChangePassword` en `PrivateRoutes.tsx`) | La app no usa `/auth/refresh` ni `/auth/change-password` | Brecha de seguridad: agregar ambos al login/`TokenStore` (`core/session/`, `core/network/auth/AuthApi.kt`) antes de producción |
| i18n es/en | i18next con namespaces | Español fijo | Fuera de alcance de paridad; si se pide, un solo catálogo de textos en `core/util/` |

## 4. Plan por fases

Cada fase es un milestone cerrable: no se abre la siguiente sin aceptación de la anterior.

### Fase 0 — Fundaciones que la app no tiene
- **Objetivo**: resolver una vez lo que bloquea varios módulos.
- **Incluye**: (a) tabla server-side reutilizable adoptada como estándar; (b) `multipart` + selector de archivo en `core/network/http/ApiClient.kt` + `core/`; (c) decisión PDF/CSV (endpoint nuevo vs compartir); (d) sondeo de no leídas en `AppShell.kt`; (e) `POST /auth/refresh` + cambio forzado de contraseña; (f) claves nuevas en `core/session/PermissionKeys.kt`.
- **Dependencias**: ninguna. Bloquea a las fases 1–3.
- **Aceptación**: una pantalla piloto (p. ej. Movimientos) usa `AppTableScreen` sin reimplementar nada; una subida multipart real contra el API devuelve 200; `:shared:compileCommonMainKotlinMetadata` verde.

### Fase 1 — Operación: inventario, tickets, reportes (mayor valor / menor riesgo)
- **Objetivo**: cerrar el ciclo de activos, que es el core del sistema.
- **Incluye**: detalle de dispositivo con unidades/stock/kardex y **“Agregar unidades”** (`STOCK_IN`); alta de N unidades con identidades; devoluciones (parcial por unidad + condición por renglón + bitácora `/inventory/returns`); revertir movimiento; auditor + mismatches; carga masiva de dispositivos; pestañas faltantes de reportes + CSV; adjuntos de tickets; alta de dispositivo multi-pieza.
- **Dependencias**: Fase 0 (tabla, multipart, CSV/PDF).
- **Aceptación**: cada acción de `web/src/pages/inventory/*` y `/reports` tiene camino en la app con la misma compuerta; `/inventory/audit` y `/inventory/returns` operables; 0 pantallas que llamen endpoints fuera de los listados en §2.

### Fase 2 — RH, asistencias y reloj
- **Objetivo**: cubrir el bloque más grande de funcionalidad faltante sin tocar inventario.
- **Incluye**: tablero de expedientes (`/employees/records`) + subir/eliminar documentos + avisos; edición de expediente (perfil, descuentos); actas administrativas + detalle; catálogo de tipos de documento; módulo reloj (`/hr/time-clock*`, `/time-clocks*`: asistencia, entradas/salidas, padrón, vínculos, relojes, inspección); horarios, asignación masiva, aprobación de horas extra y prenómina.
- **Dependencias**: Fase 0 (multipart para documentos/fotos); datos de prueba `npm run mock:attendance` / `npm run mock:access` en `api/`.
- **Aceptación**: RH puede dar de alta documentos y avisar faltantes desde el móvil; un supervisor consulta asistencia y aprueba horas extra; horarios y asignación masiva operables.

### Fase 3 — Administración: usuarios, roles, catálogos, configuración
- **Objetivo**: completar la consola de administración.
- **Incluye**: lista de cuentas `/users` (con diálogo de edición, como la web) + historial + importación; consola `/roles` completa (Roles, Acceso por persona, Políticas ABAC, simulador, Actividad); `/catalogs` completo (las 7 pestañas que faltan); `sys-config` de notificaciones.
- **Dependencias**: Fase 1 (tabla) y Fase 2 (`/hr/catalogs/*` ya en uso por el expediente).
- **Aceptación**: ADMIN puede crear/editar/desactivar cuentas, editar la matriz y políticas, y mantener todos los catálogos desde la app; ninguna pestaña de `/roles` queda fuera.

### Fase 4 — Cocina (solo si el dueño enciende `FEATURES.kitchen`)
- **Objetivo**: portar el módulo apagado.
- **Incluye**: artículos/lotes/movimientos/conteo, reabastecimiento y órdenes de compra, proveedores, facturas, catálogos (IVA, unidades, categorías) y centros de costo; PDF de OC.
- **Dependencias**: Fase 0 (PDF) y Fase 1 (tabla/import).
- **Aceptación**: mismas pantallas y permisos que `web/src/app/App.tsx` bajo `kitchen.*`; sin encender la bandera en web hasta que el dueño lo pida.

### Fase 5 — Pulido, estados y paridad de permisos
- **Objetivo**: cerrar diferencias de comportamiento y accesibilidad.
- **Incluye**: modo oscuro (tema, no pantallas), i18n si se pide, Langostin si se pide, estados vacío/error/carga homogéneos (`core/ui/BentoState.kt`), insignias de notificaciones, deep links desde notificación, accesibilidad (targets, contraste) y revisión de que cada `PermissionKeys` coincide con el catálogo del API.
- **Dependencias**: todas.
- **Aceptación**: checklist de §6 en verde en las pantallas tocadas; auditoría de aparición bento sin deuda nueva.

## 5. Orden recomendado de implementación

1. **Fundaciones (Fase 0)**: sin tabla, multipart y decisión de exportación, cada pantalla nueva paga el costo otra vez.
2. **Detalle de dispositivo + “Agregar unidades”**: es el corazón del sistema y hoy la app no puede ingresar inventario nuevo a un dispositivo existente.
3. **Alta de N unidades con identidades**: el caso real son lotes de equipos, no piezas sueltas.
4. **Devoluciones con condición por pieza + bitácora**: cierra el ciclo préstamo→devolución→baja, hoy simplificado a texto.
5. **Revertir movimiento + filtros de kardex**: evita correcciones por base de datos.
6. **Carga masiva de dispositivos (Excel)**: sustituye la captura pieza por pieza en el arranque de inventario.
7. **Auditor de inventario + mismatches**: da confianza en los saldos y ya existe en el API.
8. **Reportes: Salidas + periodo + CSV**: sin esto el móvil solo sirve para capturar, no para informar.
9. **Expedientes (tablero) + documentos**: desbloquea al área de RH, que hoy no puede subir nada.
10. **Actas administrativas**: requerimiento legal de RH y ya tiene API y PDF de referencia.
11. **Usuarios: lista + historial + importación**: la app no tiene lista de cuentas; hoy se administra solo desde la web.
12. **Catálogos (las 7 pestañas faltantes)**: elimina la dependencia de abrir la web para un alta trivial.
13. **Reloj: asistencia + entradas/salidas + padrón**: el bloque de mayor uso operativo después de inventario.
14. **Horarios + asignación + horas extra**: depende de que exista el reloj para que los números tengan sentido.
15. **Consola de roles completa (políticas/simulador) + cierre de Fase 5**: última porque requiere que existan las pantallas cuyos permisos se administran.

## 6. Reglas y verificación

Reglas duras (copiadas de `AGENTS.md` de la raíz y de `PuertoNuevoApp/AGENTS.md`, aplican a este trabajo):

- **Reutilización obligatoria**: todo lo de UI sale de `core/ui` y `core/theme`. Prohibido reimplementar estilos con `Modifier` sueltos, `OutlinedTextField`/`BasicTextField` directos o `RoundedCornerShape(<n>.dp)`: usar `AppCard`, `AppTextField`, `AppSearchField`, `StatusChip`, `AppModal`, `AppShape.*`, `AppBento`, `Bento*`. Si algo no existe, se crea **un** componente en `core/ui` y se usa en todas partes.
- **E2E solo del cambio**: correr únicamente el spec que cubre el ajuste; nunca la suite completa ni `npm test` “para confirmar”. Si el cambio no tiene spec, decirlo y ofrecer escribirlo.
- **Nada de capturas de pantalla** para revisar cómo quedó algo, salvo que el dueño lo pida explícitamente. No abrir el navegador ni leer imágenes para validar UI.
- **Nada de verificaciones largas** (builds completos, barridos de pantallas): si pasa de ~30 s, preguntar antes.
- **Compilar**: `./gradlew :shared:compileCommonMainKotlinMetadata`; si se tocaron recursos, además `:androidApp:processDebugResources`.
- **No tocar navegación/permisos sin necesidad**: `core/nav/Screen.kt`, `ScreenChrome.kt`, `feature/home/AppShell.kt` y `core/session/PermissionKeys.kt` se modifican solo para agregar la pantalla y su clave; cualquier cambio de compuerta se justifica en el mismo cambio y se refleja en la tabla de §2.
- **No inventar contrato**: si algo de la web no tiene endpoint en `api/src/modules/*/routes/*`, se anota en §7 como “por crear”; no se agregan rutas supuestas ni se toca `api/` desde esta línea de trabajo.
- **Una pantalla, una feature**: `feature/<dominio>/` con `UiState` + `ViewModel` + `Screen` (patrón de `feature/auth` y `feature/tickets`), sin DI framework: dependencias por constructor desde `AppContainer`. Las features no se importan entre sí.
- **Alcance del API**: toda tabla nueva usa `TableRequest`/`Query` y filtros validados por el API; nada de filtrar client-side una lista paginada.

Definición de terminado (por pantalla):

1. Entrada en `Screen.kt` + `chrome()` en `ScreenChrome.kt` + rama en `AppNavHost` de `AppShell.kt`, y entrada en el drawer/barra inferior solo si es destino de primer nivel.
2. Compuerta con `SessionUser.can(...)` usando una clave de `core/session/PermissionKeys.kt`; sin roles hardcodeados.
3. `UiState` + `ViewModel` + `Screen` con estados carga/vacío/error usando `core/ui/BentoState.kt` y `AppTableScreen` si es listado.
4. Acciones completas contra endpoints **existentes** (los de §2), con `401 → logout` y mensajes de error vía `networkMessage()`.
5. `./gradlew :shared:compileCommonMainKotlinMetadata` verde y checklist de `PuertoNuevoApp/AGENTS.md` (componentes estándar, `AppShape`, `.clip(shape)` antes de `.clickable`, inputs `AppTextField`) cumplido.
6. Fila de §2 actualizada en este documento (estado y brecha) en el mismo cambio que la implementación.

## 7. Riesgos y decisiones abiertas

| Riesgo / decisión | Impacto | Propuesta |
|---|---|---|
| Acciones web sin endpoint en el API (PDF de carta/movimiento/acta, deshacer devolución — la app ya devuelve 501) | Alto: bloquea paridad real de esas acciones | Crear endpoints en `api/` (p. ej. `GET /inventory/loans/:id/pdf`) o aceptar vista en pantalla + compartir; **decisión del dueño** |
| PDF y CSV son client-side en la web | Alto: en KMP no hay renderer equivalente | Servir el PDF/CSV desde el API y que la app lo abra/comparta; no portar `@react-pdf` |
| Archivos/Excel en móvil: `ApiClient` no soporta `multipart` y no hay picker | Alto: bloquea adjuntos, documentos, importaciones y fotos | Resolver en Fase 0 una sola vez (expect/actual + Ktor `formData`) |
| Pantallas demasiado densas para móvil (consola de roles, tablero de expedientes, kardex, nómina) | Medio: riesgo de copiar la web y que no se use | Adaptar por tareas (tarjetas/pasos) y no por columnas; el clon del Inicio ya omitió el tablero de expedientes por densidad (`REDESIGN_BENTO.md`) |
| Notificaciones: Ably vs sondeo vs push | Medio: sin push, un aviso puede tardar | Sondeo primero; push (FCM/APNs) requiere registrar token y mapear `notificationRoute` a `Screen` |
| Modo oscuro: tema claro único y `AppColors.*` fijos | Bajo, pero crece con cada pantalla nueva | Migrar a tokens antes de multiplicar pantallas (Fase 5) |
| `PermissionKeys.kt` es un subconjunto estático del catálogo dinámico del API | Medio: una clave nueva no se ve hasta recompilar | Toda clave nueva del catálogo que la app use entra en el mismo PR; revisar `GET /permissions/catalog` al cerrar cada fase |
| Duplicidad `Locations*` (departamentos mapeados como “ubicaciones”) frente a `Departments*` | Medio: dos modelos para lo mismo y dos pantallas | Unificar en departamentos/subáreas al tocar el módulo; no crear una tercera |
| Sin refresh token ni cambio forzado de contraseña en la app | Alto en producción: sesión que expira y contraseñas temporales | Fase 0 |
| Reloj/Hikvision: probar requiere hardware o datos mock | Medio: paridad “a ciegas” | Usar `npm run mock:attendance` y `npm run mock:access` del API; validar contra esos datos |
| `hotel-store` y `mail` tienen API sin pantalla web | Bajo: no son paridad | Mantenerlos fuera; si el dueño los quiere, es alcance nuevo, no paridad |
| Idioma: la app fija español | Bajo | Confirmar con el dueño si es requisito antes de invertir en i18n |
| Deep links: la app no los tiene (la web abre por ruta de notificación) | Medio: los avisos no llevan al lugar exacto | Al implementar push/sondeo, mapear tipo→`Screen` en un solo archivo (hoy el mapa vive en `feature/home/HomeScreen.kt`) |

## 8. Estado de implementación (bitácora)

> Actualizado por el Lead durante la ejecución. `✅` = hecho y compilando; `🟡` = en curso; `⬜` = pendiente. Cada pantalla nueva se valida con `./gradlew :shared:compileCommonMainKotlinMetadata` y, cuando aplica, con un flujo de Maestro (`.maestro/`).

### Fase 0 — Fundaciones ✅
- [x] Tabla server-side estándar (`core/ui/table`: `PaginatedTableViewModel` + `ClientTableViewModel`, filtros/orden/estados) y piloto **Kardex** (`InventoryMovementsScreen`).
- [x] Multipart + selector de archivos + compartir (`core/platform/{FilePicker,ShareFiles}`, `ApiClient.postMultipart/postBytes`, `AttachmentPicker`; Android real, iOS stub documentado).
- [x] CSV (`shareCsv`) y decisión PDF: **no** se genera PDF en el cliente; pendiente de endpoint en `api/` (§7).
- [x] Sondeo de notificaciones cada 45 s en `AppShell` (la web usa Ably).
- [x] Sesión endurecida: refresh token + `ensureFreshSession()` + cambio forzado de contraseña (`ChangePasswordScreen`).
- [x] Claves de permiso: `PermissionKeys` se sincronizó con el catálogo del API (42 claves nuevas).

### Fase 1 — Operación 🟡
- [x] Dispositivo: **unidades** (identidad editable), **stock** y **kardex** en el detalle; botón «Agregar unidades».
- [x] Alta de **N piezas con identidades** (`DeviceForm` + `DeviceCreateInput.initialQuantity/units`).
- [x] **Entrada de unidades** a un dispositivo existente (`DeviceStockInScreen`, `STOCK_IN` con identidades e `Idempotency-Key`).
- [x] **Devoluciones**: bandeja (`InventoryReturnsScreen`), devolución **parcial por pieza con condición** desde la carta; ROTO da de baja.
- [x] **Auditor de inventario** (`InventoryAuditScreen`, 8 reglas + conciliación de discrepancias).
- [x] **Carga masiva de dispositivos** por Excel (`InventoryImportScreen`: subir→revisar→confirmar, plantilla, re-preview por overrides, archivo repetido).
- [x] **Usuarios** (Fase 3 adelantada): lista de cuentas, historial, activar/desactivar, contraseña e importación.
- [ ] Revertir movimiento + `STOCK_IN` del alta genérica + entrada a Excel desde Dispositivos (`task-13` en curso).
- [ ] Reportes: pestañas faltantes + resumen por periodo + export CSV (`task-14` en curso).
- [ ] Adjuntos de ticket (`task-15` en curso).

### Fase 2 — RH, reloj y horarios 🟡
- [x] Capa de datos completa (`core/network/{hr,timeclock,schedules,catalogs}`).
- [ ] Expediente: documentos/foto/perfil/descuentos/avisos (`task-16` en curso).
- [ ] Tablero de expedientes + actas administrativas (pantallas creadas como esqueleto, pendiente implementación).
- [ ] Reloj: asistencia, entradas/salidas, rol de turnos, padrón y relojes (pantallas creadas como esqueleto, tareas asignadas).
- [ ] Horarios, asignación masiva, tiempo extra y pre nómina (pantallas creadas como esqueleto, tarea asignada).

### Fase 3 — Administración 🟡
- [x] Usuarios (lista, historial, importación) — adelantada.
- [ ] Consola `/roles` completa (Roles, Acceso por persona, Políticas ABAC, simulador, Actividad): la app hoy tiene Matriz y Catálogo.
- [ ] Catálogos (7 pestañas) + configuración de notificaciones (`sys-config`): pantalla `Screen.Catalogs` creada como esqueleto.

### E2E (Maestro)
- [x] Harness: `scripts/e2e.sh` (adb reverse + prefs del API + permisos del sistema) y flujos en `.maestro/`.
- [x] `login.yaml`, `smoke.yaml` (shell, drawer, Langostín + chat de ticket), `inventory-returns.yaml`, `inventory-audit.yaml`.
- [ ] Flujos por pantalla nueva (se agregan al implementar cada una) y `maestro test` de toda la carpeta al cierre de cada fase.

### Langostín (adicional al plan, pedido del dueño)
- [x] Botón central del shell + panel de ayuda contextual (37 pantallas) + alta de ticket en chat.
- [x] Mascota pixel-art y **escenas 8-bit** (`langostin-escenas`) animadas en el hero del panel (hotel/sistemas/navidad/playa/noche + badge de temporada).
- [ ] Globo proactivo, agentes de tarea/persona/acta/horario y guía del tablero.
