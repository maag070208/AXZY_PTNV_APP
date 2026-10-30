# REDESIGN_BENTO — Migración de la app al diseño bento

Referencia web: `BENTO_REDESIGN.md` §3 y `PLAN_REDISENO_PANTALLAS.md` §0.1 (raíz del monorepo).

## Tokens y componentes añadidos

- `core/theme/Bento.kt`: `AppBento` (canvas, field, ink/onInk, `tone()`), `BentoTone`
  { Plain, Sea, Lavender, Mint, Sun, Rose } y `BentoToneColors` (claro/oscuro por luminancia del esquema).
- `core/theme/Shape.kt`: `AppShape.bento` (24dp) y `AppShape.sheetTop` (28dp arriba).
- `core/theme/InputTokens.kt`: campos en píldora (48dp), `MultilineShape`, `RingWidth`.
- `core/ui/AppTextField.kt`: ahora look bento (relleno neutro, sin borde en reposo, aro primario en foco/error;
  multilínea con esquina 20dp). Misma API.
- `core/ui/Bento.kt`: `BentoTile`, `PillButton` (Primary/Secondary/Ink/Ghost, loading), `OnBrandChip`, `BentoSheet`.
- `core/ui/Components.kt`: `StatusChip(label, tone: BentoTone, leadingIcon?)` (sobrecarga pastel).
- `core/theme/Bento.kt`: `BentoTone.Brand` (mosaico hero, color primario) y `AppBento.series` (paleta de gráficas).
- `core/ui/Bento.kt` (además): `BentoTile` ahora admite `bigValue`, `valueColor`, `cornerIcon`, `watermark` (si es más alto que su
  contenido, el valor se pega abajo); `BentoMiniStat`, `BentoIconButton` (con insignia), `BentoNavItem`.
- `core/ui/BentoBottomBar.kt`: `BentoBottomBar(items, action?)` (reemplaza a `BentoFloatingNav`): barra blanca de 66dp a todo
  el ancho, esquinas superiores 28dp, sombra suave y **hairline** superior que sigue el contorno; destino activo con **píldora
  tinta** 40×28 detrás del ícono (ícono 24dp y etiqueta en tinta/negrita, inactivos gris al 75 %) y `avatarName` pinta el
  avatar del perfil con anillo tinta; `action` = botón central "Nuevo" de 58dp (acento sol) con **anillo blanco de 4dp** encajado
  en la muesca y sobresaliente de 32dp. `badge` opcional por destino. Respeta `navigationBarsPadding`; sin `action` los destinos
  se reparten parejo y siempre quedan centrados.
- `core/ui/BentoTopBar.kt`: header del shell (sustituye al `Material` `TopAppBar`): fila flush con el lienzo y controles
  circulares blancos flotantes — hamburguesa o atrás, marca o título + subtítulo de la sección, acción de la pantalla,
  campana con insignia y avatar. En el Inicio muestra el lockup "Puerto Nuevo · Hotel y Villas".
- `core/ui/langostin/LangostinMascot.kt`: mascota de Langostín (pixel-art 32x32 del sprite de la web rasterizado a
  `composeResources/drawable/langostin_idle_0..3.png`, 384px, fondo transparente) en un círculo blanco con sombra, anillo
  y emoji de la temporada del año (`LangostinSeason` + `langostinSeasonNow()` con las mismas fechas que la web;
  hoy Halloween 🎃). `animated` cicla los 4 cuadros cada 300 ms.
- `core/ui/BentoDrawer.kt`: drawer lateral de 312dp con esquinas derechas 28dp (`AppShape.sheetEnd`): hero `OceanBackdrop`
  (logo, usuario, chip de rol, @usuario, X), buscador del menú, cinco secciones (Principal, Operación, Inventario, Organización,
  Configuración), filas de 52dp con ícono en contenedor de 36dp, chevron animado, insignia rose de notificaciones, subnodos con
  riel+punto y pie rose de "Cerrar sesión". El grupo de la pantalla actual arranca abierto.
- `core/ui/BentoBits.kt`: `BentoWeekBars`/`BentoWeekDay` (barras de la semana, hoy resaltado), `BentoInsight` (nota al pie),
  `BentoMiniList` (mini lista con título; vacío con palomita). `BentoMiniStat` ganó `labelMaxLines`.
- `core/ui/BentoCharts.kt`: `rememberCountUp`, `BentoGauge` (semicírculo), `BentoSegments`, `BentoWave`, `BentoDonut`/`BentoSegment`,
  `BentoBarRow` (fila con barra), `BentoAvatar` (iniciales).
- `core/ui/BentoTabs.kt`: `BentoTabs`/`BentoTabItem` (píldoras, activa en tinta) y `BentoLivePill`.
- `core/ui/BentoFeed.kt`: `BentoFeed` (eventos agrupados por día con chip pastel).
- `core/ui/BentoState.kt`: `BentoState` / `BentoErrorState` (vacío, cargando, error como tarjeta bento).
- `core/util/LocalTime.kt`: fecha larga, saludo por hora, hora local y día relativo (expect `localOffsetMillis`, actuals Android/iOS).
- `core/ui/Brand.kt`: `BrandGradientBackdrop`, `BrandLogoBadge(floating = true)`.

Pendiente: el tema es solo claro (`PuertoNuevoTheme` sin `darkColorScheme`); los tokens bento ya
reconocen modo oscuro, pero activarlo en toda la app requiere migrar antes `AppColors.*` fijos.

## Pantallas

- ✅ `feature/auth` (login)
- ⬜ `feature/access`
- ⬜ `feature/audit`
- ⬜ `feature/credential`
- ⬜ `feature/custodyletters`
- ⬜ `feature/departments`
- ⬜ `feature/devices`
- ⬜ `feature/devicetypes`
- ✅ `feature/home` — **clon del Inicio de la web** (ver abajo); `AppShell` con `BentoBottomBar`
- ⬜ `feature/hr`
- ⬜ `feature/inventory`
- ⬜ `feature/materialoutputs`
- ⬜ `feature/notifications`
- ⬜ `feature/permissions`
- ⬜ `feature/reports`
- ⬜ `feature/roles`
- ⬜ `feature/tickets`
- ⬜ `feature/users`

## Inicio = clon de la web

Fuente: `web/src/pages/home/HomePage.tsx`, `features/home/**`, `entities/dashboard`, textos `es/{home,dashboard}.json`.
Pestañas (orden de la web, compuerta por permiso): Operación (`dashboard.view`|`tickets.view`|`tasks.view`), Personal
(`attendance.view`|`hr.records`|`overtime.view`), Reloj y accesos (`time_clock.link`|`access.scan`), Sistema (`system.configure`),
Mi espacio (sesión). Con una sola pestaña no hay barra. "En vivo" (solo `dashboard.view`) va en la línea de la fecha, no entre las
pestañas; las pestañas se desplazan en horizontal sin recortar etiquetas. La fecha es corta ("Jue 29 oct · Puerto Nuevo").
El Inicio se refresca cada 60 s (la web usa Ably; la app no).

Mosaicos por pestaña (✓ clonado / ✗ omitido):

- **Operación** (con `dashboard.view`, `GET /dashboard/summary` + `GET /inventory/dashboard`): ✓ Dispositivos (hero + medidor,
  Disponibles %, En préstamo), ✓ Cartas activas (olas, "de N cartas"), ✓ Salidas dañadas (Sin daños), ✓ Resolución promedio
  (por tarea), ✓ Tareas resueltas (N / total, pendientes), ✓ Actividad de la semana (barras, hoy resaltado), ✓ Eficiencia del
  equipo (dona + 4 barras + insight), ✓ Dispositivos por categoría (top 5), ✓ Actividad reciente (por día, scroll propio).
  Sin `dashboard.view`: ✓ widget Tickets y ✓ widget Tareas (alcance OWN/AREA/ALL, antiguos/atrasadas, por responsable/persona).
- **Personal** (con `hr.records`): ✓ Plantilla activa (hero + medidor + chips; cuenta expedientes de `/dashboard/hr-records`),
  ✓ Asistencia de hoy (6 contadores), ✓ Horas extra de la semana, ✓ Seguimiento del día (3 listas).
  ✗ Tablero de expedientes (tabla con filtros, detalle, subida de documentos y avisos): demasiado denso para el móvil; el hero abre Personal.
  Sin `hr.records`: ✓ widget Asistencia (propia/área/todos, dona, listas) y ✓ widget Horas extra.
- **Reloj y accesos**: ✓ Pendientes de configuración, ✓ Accesos de hoy (hero + medidor), ✓ En sitio, ✓ Últimos registros.
- **Sistema**: ✓ Salud del sistema (relojes al día en 2 h), ✓ Correos, ✓ Auditor de inventario, ✓ Relojes,
  ✓ Atajos (Roles y permisos, Catálogos, Auditoría de inventario); ✗ atajo "Relojes": la app no tiene esa pantalla.
- **Mi espacio**: ✓ Mi equipo asignado. (Se quitaron lo propio de la app: "Mi credencial", "Mis tareas pendientes" y los
  contadores de tickets abiertos/tareas del mosaico anterior, que la web no muestra en el Inicio.)
- Fuera de alcance: pestaña Cocina; widget `people` (Plantilla/cumpleaños) que la web reemplaza por el panel Personal.
- Los números/tiles sin pantalla equivalente en la app (nómina, aprobación de horas extra, vínculo de relojes, asignar horarios)
  no navegan.

## Header

`BentoTopBar` en `AppShell`, sobre el mismo color del lienzo (sin banda distinta), `statusBarsPadding()` y alto 64dp:

- Izquierda: círculo blanco con hamburguesa (abre el drawer) o flecha atrás cuando la pila tiene a dónde volver.
- Centro: en el Inicio, lockup de marca (logo + "Puerto Nuevo" + "Hotel y Villas"); en el resto, título de la pantalla y,
  cuando hay atrás, subtítulo con el módulo (Tickets, Inventario, Personal, Control de acceso).
- Derecha: acción registrada por la pantalla (`LocalTopBarActions`), campana con la insignia de no leídas y avatar (→ Mi
  credencial). La campana y el avatar ya no son exclusivos del Inicio: su saludo conserva solo el saludo, la fecha y "En vivo".

## Drawer

`BentoDrawer` en `AppShell` como `drawerContent` del `ModalNavigationDrawer` (el panel ya trae ancho, esquinas, sombra e
insets; no lleva `ModalDrawerSheet`): hero de marca con los datos del usuario y cierre, buscador, menú agrupado por secciones
y logout al pie. Mismos ítems, subítems y compuertas por permiso que la versión anterior. El grupo activo se abre solo y el
buscador filtra nodos e hijos.

## Barra inferior

`BentoBottomBar` en `AppShell`: Inicio, Tickets (`tickets.view`), uno o dos de Portería (`access.scan`) / Inventario
(`devices.view`) / Personal (`hr.records`) —los primeros permitidos, hasta 4 destinos en total— y Perfil (avatar → Mi credencial).
Botón central "Nuevo" → Nuevo ticket, solo con `tickets.create`; sin él los destinos se reparten parejo. La barra mide su
sobresaliente, así el `Scaffold` deja espacio y el contenido no queda tapado (el Inicio y la lista de Tickets suman holgura inferior).
El destino activo se marca con la píldora tinta (el mismo acento de navegación del resto de la app) y el botón central es el
único acento sol: no compite con los mosaicos de marca del Inicio.

## Langostín (asistente)

El botón central de la barra inferior ya no es "Nuevo": es **Langostín** (mascota con anillo/emoji de temporada, sin
permiso: solo sesión). Abre una hoja inferior de 93 % con scrim:

- Encabezado: mascota, "Langostín", punto verde "En línea · 🎃 Halloween", **reiniciar** y **cerrar**.
- Chip de contexto "Estás en: *Pantalla*" (se reinicia al cambiar de pantalla).
- Hilo de ayuda: saludo con el contexto de la pantalla y opciones en píldoras — **Ir a…** (solo rutas con permiso),
  **En esta pantalla** (preguntas que contesta en el hilo) y **Langostín** (formularios).
- **Nuevo ticket** (solo con `tickets.create`): chat de 4 pasos (asunto → detalle → categoría → urgencia) +
  confirmación, loader mínimo de 700 ms y `POST /tickets`; al terminar ofrece **Ver ticket**. Mismos datos que el alta
  de página; la API valida otra vez.
- Registro de ayuda: `feature/langostin/model/LangostinHelp.kt` (portado de `helpRegistry.ts`; una entrada por pantalla
  de la app + `default`, mínimo 3 opciones, toda ruta y permiso verificados contra `Screen`/`PermissionKeys`).
- Pendiente (siguiente iteración): globo proactivo en Tickets, formularios de tarea/persona/acta/horario y guía animada
  del tablero; y los resultados de la web que la app no tiene se explican como respuesta, no como navegación.
