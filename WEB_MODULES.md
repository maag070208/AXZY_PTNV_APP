# Módulos y funcionalidades — Web (Cartas Responsivas)

> Referencia de la aplicación web existente (`AXZY_PTNV_WEB`) para el proyecto móvil (`AXZY_PTNV_APP`).
> Fuente: revisión de `~/DEV/PTNV/RESPONSIVA/web/src` (FSD: app / entities / features / pages / widgets / shared).
> No implica migración 1:1; documenta qué existe y cómo funciona.

---

## 1. Stack y arquitectura

| Capa | Tecnología |
|------|------------|
| UI | React 19 |
| Build | Vite 6 + TypeScript 5.7 |
| Estado | Redux Toolkit 2 (`react-redux`) |
| Rutas | React Router 7 |
| Design system | `@axzydev/axzy_ui_system` 1.2.16 (componentes prefijo `IT*`) |
| i18n | i18next + react-i18next (es + en) |
| Realtime | Ably 2.27 |
| PDF | `@react-pdf/renderer` 4 |
| Validación | yup |
| HTTP | axios (`shared/api/client`) |
| Tests E2E | Playwright |

Estructura FSD:

```
src/
  app/        # bootstrap, store Redux, router (App.tsx), guards, index.css
  entities/   # API + modelos + slices por dominio
  features/   # casos de uso (model/use*.ts + ui/)
  widgets/    # composición compleja (PDFs, paneles, kanban)
  pages/      # rutas
  shared/     # api, ui, lib, i18n, pdf, utils, validation
```

Entidades existentes: `user`, `department`, `subarea`, `personal`, `inventario`, `salida`, `report`, `ticket`, `notification`, `dashboard`, `audit-log`, `sys-config`.

Slices Redux globales: `auth`, `notifications`, `tickets`.

Convenciones:
- Feature: `features/<dominio>/<caso-uso>/{model/use*.ts, ui/*.tsx, index.ts}`.
- Página: `pages/<dominio>/*Page.tsx` compone feature(s).
- Aliases: `@app`, `@entities`, `@features`, `@pages`, `@widgets`, `@shared`.
- i18n namespaces por dominio en `shared/i18n/locales/{es,en}/`.

---

## 2. Roles y permisos

Roles: `ADMIN`, `GERENTE`, `JEFE_DE_AREA`, `RECURSOS_HUMANOS`, `EMPLEADO`.

| Rol | Alcance |
|-----|---------|
| **ADMIN** | Control total: catálogos, usuarios, inventario, reportes, tickets, RRHH, dispositivos. Puede completar/cerrar tareas. |
| **GERENTE** | Supervisa tickets y tareas de su departamento. Crea/asigna tickets, completa tareas, cierra tickets. Ve dashboard admin. Sin inventario, sin expediente RRHH completo. |
| **JEFE_DE_AREA** | Seguimiento operativo de tickets de su área. Crea tickets, genera tareas, mueve tareas hasta revisión. |
| **RECURSOS_HUMANOS** | Administra expediente completo de Personal (médico, oficial, contacto, documentos). No administra tickets, inventario ni usuarios. |
| **EMPLEADO** | Ejecuta tareas asignadas. No crea tickets; solo ve tickets con tareas asignadas; mueve sus tareas hasta revisión. |

Gates en `app/guards/PrivateRoutes.tsx`:
- `isAdmin = ADMIN || GERENTE` → Inventario, Reportes, Catálogos, Usuarios, dashboard admin.
- `canManage = isAdmin || JEFE_DE_AREA`.
- `canManageHR = ADMIN || RECURSOS_HUMANOS` → expediente completo de personal (médico/oficial/contacto/documentos). GERENTE NO.
- `EMPLEADO` → `/tickets/mis-tareas` + notificaciones.
- `CatalogPage` redirige a no-ADMIN a `/`.

Guard: sin token → redirect a `/login`; con token sin user → `meThunk`; carga `unreadCount`.

---

## 3. Módulos

### 3.1 Auth
- Ruta: `/login` — `pages/auth/LoginPage`, `features/auth/login`.
- Login usuario/password, token + sesión (`shared/api/session`), `me`, logout.
- Endpoints: `POST /auth/login`, `GET /auth/me`.

### 3.2 Inicio / Dashboard
- Ruta: `/` — `pages/home/HomePage`, `features/home/admin-dashboard`.
- Home vacío para empleados; dashboard solo `ADMIN`/`GERENTE`.
- Panel en tiempo real: KPIs (dispositivos, tickets abiertos, cartas activas, salidas dañadas), donut dispositivos por estado, tickets por estado, actividad reciente (con links a ticket/préstamo/dispositivo), tareas resueltas, resolución promedio, eficiencia del equipo, tickets antiguos/urgentes.
- Endpoint: `GET /dashboard/summary`.

### 3.3 Inventario (solo ADMIN)
Rutas: `/inventario`, `/inventario/dispositivos`, `/inventario/dispositivos/nuevo`, `/inventario/dispositivos/:id`, `/inventario/dispositivos/:id/editar`, `/inventario/tipos`, `/inventario/movimientos`, `/inventario/movimientos/nuevo`, `/inventario/prestamos`, `/inventario/prestamos/nuevo`, `/inventario/prestamos/:id`, `/inventario/prestamos/:id/editar`, `/inventario/devoluciones`, `/inventario/devoluciones/nueva`.

- **Dashboard**: resumen por tipo (tipos, dispositivos, unidades activas, disponibles, prestadas, dañadas, mantenimiento, baja), movimientos recientes, distribución (donut), movimientos últimos 30 días. `GET /inventario/dashboard`.
- **Tipos de dispositivo**: CRUD con código, nombre, prefijo de folio, y campos configurables por unidad (serie, equipo, IP, MAC, SO, RAM, almacenamiento — mostrar/obligatorio).
- **Dispositivos**: CRUD por modelo/lote. Alta crea N unidades físicas (activo fijo) + movimiento ENTRADA. Detalle: existencias, unidades físicas (serie/MAC/IP/equipo/área/departamento), kardex. Importación desde Excel (modelo/descripción/cantidad/marca/tipo) con pasos upload→review→confirm.
- **Movimientos**: alta con tipos ENTRADA, BAJA, TRASPASO, AJUSTE_ENTRADA, AJUSTE_SALIDA, MANTENIMIENTO_ENTRADA, MANTENIMIENTO_SALIDA, PRESTAMO, DEVOLUCION, REVERSION. Historial con filtros, revertir, comprobante PDF y reporte PDF. Kardex por dispositivo.
- **Préstamos / Cartas responsivas**: crear carta (asignación a empleado o departamento/subárea), folio consecutivo, preview, PDF, editar (si tiene devoluciones solo se edita asignación), cancelar, historial de devoluciones.
- **Devoluciones**: parciales/totales, condición por ítem (BUENO, ACEPTABLE, MALO, ROTO); ROTO da de baja automática.
- Endpoints: `/inventario/tipos`, `/inventario/dispositivos`, `/inventario/dispositivos/:id/{existencias,unidades,kardex}`, `/inventario/unidades-fisicas/:id`, `/inventario/movimientos`, `/inventario/movimientos/:id/revertir`, `/inventario/prestamos`, `/inventario/prestamos/:id/cancelar`, `/inventario/devoluciones`, `/inventario/dashboard`.

### 3.4 Tareas / Tickets (todos)
Rutas: `/tickets`, `/tickets/kanban`, `/tickets/mis-tareas`, `/tickets/tareas`, `/tickets/nuevo`, `/tickets/:id`, `/tickets/:id/editar`.

- **Lista**: tabla con título, estado, prioridad, creado por, asignado a, días en espera (EXCELENTE/BUENO/REGULAR/MALO). Mover a papelera / eliminar definitivo.
- **Crear**: título, categoría (MANTENIMIENTO, EQUIPO, SISTEMA, OTRO), descripción, urgencia/prioridad (BAJA, MEDIA, ALTA, URGENTE), evidencias (imágenes/PDF).
- **Editar**: título, descripción, prioridad.
- **Detalle**: info, comentarios (con adjuntos), historial/timeline, grafo de tareas, kanban de tareas, panel manager (estado, categoría, departamento, responsable), eficacia de resolución, PDF. Estados: ABIERTO, EN_SEGUIMIENTO, CERRADO.
- **Tareas/asignaciones**: crear tarea (título, descripción, fechas inicio/fin, estado PENDIENTE/EN_PROGRESO/EN_REVISION/COMPLETADA, empleado), comentarios y adjuntos por asignación. Solo ADMIN/GERENTE pueden completar tareas.
- **Kanban**: tablero de tareas con columnas por estado, filtros por departamento, búsqueda, overdue.
- **Mis tareas** (EMPLEADO): tareas asignadas al usuario, overdue, acceso al tablero.
- **Admin tareas** (ADMIN/GERENTE): todas las tareas de todos los tickets.
- Endpoints: `/tickets`, `/tickets/kanban`, `/tickets/:id`, `/tickets/:id/{attachments,comments,assignments}`, `/tickets/:id/attachments/:attachmentId/download`, `/tickets/:id/assignments/:assignmentId/{attachments,comments}`.

### 3.5 Recursos Humanos / Personal (ADMIN + RECURSOS_HUMANOS)
Rutas: `/empleados`, `/empleados/:id`, `/empleados/:id/editar`, `/empleados/reportes`, `/empleados/reportes/:id`, `/empleados/catalogos/documentos`.

- **Lista de personal**: búsqueda, conteo, nuevo empleado.
- **Expediente (detalle)**: stepper Foto/Datos personales, Laboral, Contacto. Tabs: Personal, Oficial, Dirección, Contacto, Médica, Descuentos. Datos: nombre(s), apellidos, género, nacimiento, ingreso, departamento, subárea, puesto, empresa, RFC, CURP, NSS, dirección, celular personal/empresa, correo, contacto de emergencia (nombre/teléfono/parentesco), tipo de sangre, padecimiento, alergias. Descuentos (INFONAVIT, IMSS, Deudor Alimenticio). Foto, activar/desactivar.
- **Credencial**: generación y descarga de imagen (QR) desde el expediente.
- **Documentos**: estado de documentos por tipo (cargado/pendiente), subir/eliminar por tipo; catálogo de tipos de documento (CRUD).
- **Actas administrativas** (reportes de personal): crear (empleado, motivo INASISTENCIA/RETARDO/EBRIEDAD/CONDUCTA/INCUMPLIMIENTO/OTRO, fecha incidente, descripción, sanción), ver detalle, eliminar, PDF con firmas.
- Endpoints: `/personal/stats`, `/personal/:id`, `/personal/:id/{perfil,descuentos,foto,documentos}`, `/personal/catalogos/{tipos-documento,generos,tipos-sangre}`, `/personal/actas`, `/personal/actas/empleado/:id`, `/personal/actas/:id`.

### 3.6 Departamentos
- Rutas: `/departamentos`, `/departamentos/:id` — `pages/departments`, `features/department`.
- CRUD, soft-delete (desactivar) y eliminación definitiva; detalle con usuarios, subáreas, tickets y cartas responsivas del departamento.
- Endpoints: `/departments` (GET/POST/PUT/DELETE).

### 3.7 Subáreas
- Ruta: `/subareas` — `pages/subareas`, `features/subarea`.
- CRUD, soft-delete/reactivar; pertenecen a un departamento.
- Endpoints: `/subareas`.

### 3.8 Reportes (solo ADMIN)
- Ruta: `/reportes` — `pages/reports/ReportesPage`, `features/report/*`.
- Tabs:
  - **Asignados**: dispositivos asignados, días promedio, +30 días, origen (carta/movimiento), export PDF.
  - **Dispositivos**: totales, disponibles, asignados, bajas, +30 días, lotes, export PDF.
  - **Salidas**: bitácora de salida de material, export PDF.
- Endpoints: `/reports`, `/reports/asignados`, `/reports/devices`.

### 3.9 Usuarios (solo ADMIN)
Rutas: `/usuarios`, `/usuarios/nuevo`, `/usuarios/:id/editar`, `/usuarios/:id/historial`, `/usuarios/importar`.

- Lista con username, nombre, rol, no. empleado, departamento, subárea, puesto, estatus.
- Crear/editar: datos personales, credenciales (username, rol, password), organización (departamento, subárea). Guía de rol embebida.
- Cambio de contraseña, desactivar/reactivar, reasignar a Oficina, eliminar (soft / forzar).
- Historial del usuario (timeline + audit-log).
- Importar desde Excel (columnas Nombre del empleado / Nombre de usuario / Contraseña; se crean como EMPLEADO sin departamento).
- Endpoints: `/users`, `/users/:id`, `/users/:id/password`, `/users/:id/deactivate`, `/users/:id/reactivate`, `/users/empleados`, `/auth/*`.

### 3.10 Catálogos (solo ADMIN)
- Ruta: `/catalogos` — `pages/catalog/CatalogPage`, `widgets/catalog/tabs`.
- Tabs: Departamentos, Subáreas, Tipos de dispositivo, Tipos de documento, Géneros, Tipos de sangre, Notificaciones (sys-config).
- **Sys-config / Notificaciones**: correos destinatarios (CC) para notificaciones del sistema.
- Endpoints: `/sys-config`, `/sys-config/:key`.

### 3.11 Notificaciones (todos)
- Ruta: `/notificaciones` — `pages/notifications`, `features/notification/list`.
- Lista, no leídas, marcar leída / todas, borrar. Realtime vía Ably (`useAblyNotifications`) con toast. Badge de no leídas en topbar.
- Endpoints: `/notifications`, `/notifications/unread-count`, `/notifications/:id/read`, `/notifications/read-all`, `DELETE /notifications/:id`.

### 3.12 Salidas de material (sin página propia)
- Entidad `entities/salida` con CRUD: `GET/POST /salidas`, `POST /salidas/batch`, `PUT/DELETE /salidas/:id`, `GET /salidas/suggestions`.
- Se consume desde Reportes (tab Salidas) y desde movimientos/devoluciones. Motivos: DANADO, OBSOLETO, EXTRAVIO, OTRO.

### 3.13 Audit log
- Entidad `entities/audit-log`: `GET /audit`, `GET /audit/:id`. Usado en historial de usuario.

---

## 4. Widgets de PDF / render (`widgets/`)

| Widget | Función |
|--------|---------|
| `carta-responsiva` | Preview HTML + PDF de carta responsiva (TIC) |
| `acta-administrativa` | Preview + PDF de acta administrativa con firmas |
| `credencial-empleado` | Render PNG/QR (`buildQrPayload`, DPI, cardSpec) |
| `movimiento-pdf` | Comprobante de movimiento + reporte de movimientos |
| `reports` | PDFs: Asignados, Devices, Report, Salidas |
| `tickets` | ticket-pdf, attachments, detail modal, kanban-ui |
| `catalog` | Paneles de departamentos/subáreas + tabs |

---

## 5. Enumeraciones clave

- **Rol**: ADMIN, GERENTE, JEFE_DE_AREA, RECURSOS_HUMANOS, EMPLEADO.
- **Ticket estado**: ABIERTO, EN_SEGUIMIENTO, CERRADO.
- **Ticket prioridad**: BAJA, MEDIA, ALTA, URGENTE.
- **Ticket categoría**: MANTENIMIENTO, EQUIPO, SISTEMA, OTRO.
- **Tarea estado**: PENDIENTE, EN_PROGRESO, EN_REVISION, COMPLETADA.
- **Movimiento tipo**: ENTRADA, BAJA, TRASPASO, AJUSTE_ENTRADA, AJUSTE_SALIDA, MANTENIMIENTO_ENTRADA, MANTENIMIENTO_SALIDA, PRESTAMO, DEVOLUCION, REVERSION.
- **Condición**: BUENO, ACEPTABLE, MALO, ROTO.
- **Motivo salida**: DANADO, OBSOLETO, EXTRAVIO, OTRO.
- **Acta motivo**: INASISTENCIA, RETARDO, EBRIEDAD, CONDUCTA, INCUMPLIMIENTO, OTRO.
- **Dispositivo estado**: DISPONIBLE, ASIGNADO, BAJA.

---

## 6. Rutas completas

```
/login
/
/inventario
/inventario/dispositivos
/inventario/dispositivos/nuevo
/inventario/dispositivos/:id
/inventario/dispositivos/:id/editar
/inventario/tipos
/inventario/movimientos
/inventario/movimientos/nuevo
/inventario/prestamos
/inventario/prestamos/nuevo
/inventario/prestamos/:id
/inventario/prestamos/:id/editar
/inventario/devoluciones
/inventario/devoluciones/nueva
/tickets
/tickets/kanban
/tickets/mis-tareas
/tickets/tareas
/tickets/nuevo
/tickets/:id
/tickets/:id/editar
/departamentos
/departamentos/:id
/subareas
/empleados
/empleados/catalogos/documentos
/empleados/:id
/empleados/:id/editar
/empleados/reportes
/empleados/reportes/:id
/reportes
/usuarios
/usuarios/nuevo
/usuarios/:id/editar
/usuarios/:id/historial
/usuarios/importar
/catalogos
/notificaciones
```
