# Paridad de funcionalidades — Web vs App (KMP)

> Objetivo: llevar **toda** la funcionalidad de la web (`AXZY_PTNV_WEB`) a la app (`AXZY_PTNV_APP`).
> Web = app de referencia completa. App = KMP (Compose Multiplatform) con módulos re-agregados.
> Leyenda: ✅ completo · 🟡 parcial · ❌ falta.

---

## 0. Aviso crítico — contrato del API divergente

La web y la app **no consumen el mismo contrato de API hoy**:

| Origen | Endpoints que usa |
|--------|-------------------|
| **Web** (`web/src/entities/*`) | `/inventario/*` (tipos, dispositivos, movimientos, prestamos, devoluciones, dashboard), `/personal/*`, `/departments/subareas`, `/sys-config`, `/reports`, `/users`, `/tickets`, `/salidas`, `/notifications`, `/audit`, `/auth` |
| **API implementada** (`api/src/modules`) | Igual que la web: `inventario`, `personal`, `departments`+`subarea`, `config`, `dashboard`, `reports`, `users`, `tickets`, `salidas`, `notifications`, `audit`, `auth` |
| **App** (`core/network/*`) | `/devices`, `/device-types`, `/locations`, `/cartas`, `/inventory`, `/departments`, `/users`, `/tickets`, `/salidas`, `/reports`, `/notifications`, `/audit`, `/auth` |

El contrato de la app corresponde a `API_DOCUMENTATION.md` (raíz) — que **no** está implementado en `api/` todavía. Es decir: **la app apunta a un contrato objetivo que el backend aún no expone.**

**Decisión pendiente:** refactorizar `api/` al contrato del `API_DOCUMENTATION.md` (para que web y app compartan) **o** adaptar la app al contrato actual (`/inventario`, `/personal`). Sin resolver esto, ninguna paridad es verificable end-to-end.

---

## 1. Matriz de paridad por módulo

| # | Módulo web | Web | App | Estado | Gaps en la app |
|---|-----------|:---:|:---:|:------:|----------------|
| 1 | **Auth / Login** | ✅ | ✅ | ✅ | Login + token + `me` + logout + URL de servidor editable. |
| 2 | **Inicio / Dashboard** | ✅ | 🟡 | 🟡 | Web: dashboard admin (KPIs, donuts, actividad reciente, eficiencia, tickets urgentes). App: solo resumen de dispositivos + accesos rápidos. Falta panel admin con métricas. |
| 3 | **Inventario — Tipos de dispositivo** | ✅ | ✅ | ✅ | CRUD + `fieldConfig` por campo (serie/IP/MAC/SO/RAM/almacenamiento). |
| 4 | **Inventario — Dispositivos** | ✅ | 🟡 | 🟡 | App: CRUD, detalle, lotes. Falta: importación Excel, disponibilidad por kardex (tiene endpoint `/devices/availability`), historial/comentarios de dispositivo, edición de unidades del lote. |
| 5 | **Inventario — Movimientos / Kardex** | ✅ | 🟡 | 🟡 | App: lista, kardex, registrar movimiento (incl. malas condiciones). Falta: revertir movimiento, comprobante PDF, reporte PDF, filtros por fecha. |
| 6 | **Inventario — Préstamos / Cartas responsivas** | ✅ | ✅ | ✅ | App: lista, detalle, alta/edición, generar por tipo, marcar/deshacer devolución. Falta: PDF real (solo vista en pantalla). |
| 7 | **Inventario — Devoluciones** | ✅ | 🟡 | 🟡 | Web: página dedicada de devoluciones (parcial/total, condición por ítem, ROTO→baja). App: cubierto vía devolución de carta. Falta vista de devoluciones como bitácora. |
| 8 | **Tickets** | ✅ | 🟡 | 🟡 | App: lista, detalle, alta/edición, kanban, mis tareas, tareas del equipo, cambio de estado, comentarios. Falta: adjuntos/evidencias (fotos/PDF), historial/timeline visual, grafo de tareas, panel manager completo, PDF. |
| 9 | **Recursos Humanos / Personal** | ✅ | ❌ | ❌ | **Gap grande.** App: solo `EmployeesListScreen` (lista). Web: expediente completo (datos personales, laboral, médico, oficial, dirección, contacto, descuentos), foto, activar/desactivar, documentos (subir/eliminar por tipo), catálogo de tipos de documento, actas administrativas (crear/ver/eliminar + PDF), reportes de personal, **credencial con QR**. |
| 10 | **Departamentos** | ✅ | 🟡 | 🟡 | App: lista + detalle (ubicaciones/tickets/cartas de solo lectura). Falta: alta/edición/eliminación de departamento, gestión de subáreas/vínculos desde el detalle. |
| 11 | **Subáreas** | ✅ | ❌ | ❌ | Web: módulo standalone. App: solo "sublugares" dentro de `locations`; sin CRUD de subáreas como la web. |
| 12 | **Reportes** | ✅ | 🟡 | 🟡 | App: tabs Asignados + Dispositivos. Falta: tab **Salidas**, exportación CSV. |
| 13 | **Usuarios** | ✅ | 🟡 | 🟡 | App: lista, alta/edición, historial, cambio de password. Falta: importación desde Excel, forzar eliminación, reasignar a Oficina, guía de rol en el form. |
| 14 | **Catálogos** | ✅ | ❌ | ❌ | Web: página única con Departamentos, Subáreas, Tipos de dispositivo, Tipos de documento, Géneros, Tipos de sangre, **Sys-config (notificaciones)**. App: solo tipos de dispositivo (feature aparte). Faltan géneros, tipos de sangre, tipos de documento, sys-config. |
| 15 | **Notificaciones** | ✅ | 🟡 | 🟡 | App: lista + badge. Falta: **realtime (Ably)**, push (FCM/APNs). |
| 16 | **Salidas de material** | ✅ | ✅ | ✅ | App: bitácora + alta. Falta PDF/CSV. |
| 17 | **Audit log / Historial** | ✅ | ✅ | ✅ | App: `AuditLogsScreen` (ADMIN) + historial de usuario. |

---

## 2. Capacidades transversales

| Capacidad | Web | App | Nota |
|-----------|:---:|:---:|------|
| Autenticación JWT + 401→logout | ✅ | ✅ | Paridad. |
| Roles: ADMIN, GERENTE, JEFE_DE_AREA, EMPLEADO | ✅ | ✅ | App no maneja `RECURSOS_HUMANOS`. |
| Rol `RECURSOS_HUMANOS` | ✅ | ❌ | Falta en `SessionUser` y gates. |
| i18n (es/en) | ✅ | ❌ | App hardcodea español. |
| Realtime (Ably) | ✅ | ❌ | Pendiente (PLAN Fase 2). |
| Push notifications | ❌ | ❌ | Ninguno (controlado por API). |
| Adjuntos/multimedia (cámara, galería, multipart) | ✅ | ❌ | Pendiente (PLAN Fase 3). |
| Importación Excel (usuarios, dispositivos) | ✅ | ❌ | Pendiente (PLAN Fase 3). |
| PDF (cartas, actas, tickets, movimientos, reportes, credencial) | ✅ | ❌ | Web genera client-side (`@react-pdf/renderer`). App: vistas read-only. |
| Export CSV de reportes | ✅ | ❌ | Backend ya expone `GET /reports/.csv`. |
| Tablas server-side (page/limit/filters/sort) | ✅ | 🟡 | App: helper `TableRequest` listo, no adoptado en todos los listados. |
| Offline/cache | ❌ | ❌ | PLAN Fase 1. |
| Dark mode | ❌ | ❌ | PLAN Fase 5. |

---

## 3. Rol `RECURSOS_HUMANOS` — detalle

Web:
- `canManageHR = ADMIN || RECURSOS_HUMANOS`.
- Acceso al expediente completo de Personal (médico, oficial, contacto de emergencia, documentos).
- No administra tickets, inventario ni usuarios.

App:
- `SessionUser` solo define `isAdmin`, `canManageCatalogs`, `canDeleteCartas`, `canRegisterMovement`, `canGenerateCartas`, `canCreateTicket`, `canDeleteTicket`, `canSeeAdminTasks`, `canSeeAudit`.
- **No existe** `canManageHR` ni el rol en la matriz → bloquea el módulo RRHH.

---

## 4. Resumen de gaps priorizados (para llegar a paridad)

### Bloqueantes
1. **Contrato API divergente** (§0) — decidir refactor `api/` vs adaptar app.
2. **Rol `RECURSOS_HUMANOS`** en `SessionUser` + gates.

### Módulos faltantes completos
3. **RRHH/Personal** — expediente, documentos, actas administrativas, credencial QR, reportes de personal. *(el gap más grande)*
4. **Catálogos** — géneros, tipos de sangre, tipos de documento, sys-config (destinatarios de notificaciones).
5. **Subáreas** como módulo CRUD standalone.

### Módulos parciales a completar
6. **Dashboard admin** — KPIs, donuts, actividad reciente, eficiencia, tickets urgentes.
7. **Tickets** — adjuntos/evidencias, historial/timeline, grafo de tareas, PDF.
8. **Dispositivos** — importación Excel, historial/comentarios, disponibilidad, edición de lote.
9. **Movimientos** — revertir, comprobante/reporte PDF, filtros por fecha.
10. **Usuarios** — importación Excel, forzar eliminación, reasignar a Oficina.
11. **Reportes** — tab Salidas + export CSV.
12. **Departamentos** — CRUD + gestión de subáreas/vínculos.

### Capacidades transversales
13. Realtime (Ably) + push.
14. Adjuntos/multimedia (cámara/galería/multipart).
15. PDFs (o endpoint API `GET /cartas/:id/pdf`).
16. i18n es/en.
17. Tablas server-side en todos los listados.

---

## 5. Mapa de features app ↔ módulos web

| Feature app (`shared/.../feature/`) | Módulo web equivalente |
|-------------------------------------|------------------------|
| `auth` | Auth |
| `home` (+ `AppShell`) | Inicio / Dashboard |
| `tickets` (list, detail, form, kanban, tasks) | Tickets |
| `devices` + `devicetypes` | Inventario → Dispositivos / Tipos |
| `inventory` (index, locations, movements, new movement) | Inventario → Movimientos/Kardex (+ Ubicaciones) |
| `cartas` (list, detail, form, generate) | Inventario → Préstamos / Cartas responsivas |
| `salidas` | Salidas de material |
| `employees` (solo lista) | Recursos Humanos / Personal *(incompleto)* |
| `departments` | Departamentos *(incompleto)* |
| `users` | Usuarios *(incompleto)* |
| `reports` | Reportes *(faltan tabs/CSV)* |
| `notifications` | Notificaciones *(sin realtime)* |
| `audit` | Audit log / Historial |
| — | Subáreas *(falta)* |
| — | Catálogos *(falta)* |
| — | RH: expediente/documentos/actas/credencial *(falta)* |

---

## 6. Rutas web de referencia (completas)

Ver `WEB_MODULES.md` en este mismo repo (sección 6) para las 36 rutas de la web y el detalle por módulo.
