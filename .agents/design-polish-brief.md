# Brief: pase de diseño de pantallas (el dueño marcó TODOS los módulos)

El dueño revisó capturas (`screenshots/2026-10-30/`) y pidió rehacer/ajustar las pantallas de todos los módulos. Este brief es el criterio común: **el nivel de acabado del Inicio y de la web bento** debe verse en todas las pantallas.

## Reglas duras (no negociables)

- **No toques** `core/nav/**`, `feature/home/AppShell.kt`, `core/theme/**`, `App.kt`, `AppRoot.android.kt` ni `core/ui/**` (el Lead los administra). Si un componente compartido necesita un parámetro nuevo, pídelo al Lead; no lo edites.
- Usa solo `core/ui` + `core/theme`: `AppCard`, `PanelCard`/`AppSurfaceCard`, `BentoMiniStat`/`KpiTile`, `StatusChip(label, BentoTone)`, `BentoTabs`, `AppFilterChip`, `PillButton`, `AppTextField`, `AppSearchField`, `SimpleDropdownField`, `AppModal`, `BentoState`/`LoadingState`/`ErrorState`, `ProfileAvatar`/`BentoAvatar`.
- **Sin hex**, sin `RoundedCornerShape(n.dp)` (usa `AppShape`), sin `!bg-white`/bordes/sombras propias sobre tarjetas, sin paddings arbitrarios (`p-[18px]`).
- Fechas/horas y números con los helpers de `core/util` (`formatShortDate`, `formatLocalTime`, `formatMinutesAsHours`, `formatDateTime`); nunca ISO crudo en UI.
- Todo texto largo con `maxLines`/elipsis; targets ≥44dp; `contentDescription` en íconos accionables.
- **No cambies endpoints, permisos ni navegación**. La funcionalidad queda igual; solo presentación y jerarquía.
- Español; etiquetas en oración (mayúsculas solo en encabezados de tabla/sección).

## Lenguaje visual (lo que debe verse)

1. **Encabezado de pantalla**: título con jerarquía (grande/negrita), descripción corta de una línea en `onSurfaceVariant`, y la **acción primaria a la derecha** (píldora). Nunca acciones flotando sobre la lista.
2. **Franja de indicadores**: máximo 4 por fila, compactos (`BentoMiniStat`), tono por significado (Sea=volumen, Mint=bien, Sun=atención, Rose=problema, neutro si 0) y `labelMaxLines` corto; en 0, tono neutro.
3. **Paneles**: `PanelCard`/`AppCard` con padding 16–20dp, radio bento, separación 12–16dp; una idea por tarjeta; sin tarjetas dentro de tarjetas.
4. **Listas**: filas en `AppCard` con dos líneas (título + meta), avatar cuando es persona, chip de estado con `BentoStatus`/`BentoTone`, y acciones como `PillButton`/`BentoIconButton` (no `AppFilterChip` para acciones de fila). Paginación/“cargar más” con `PillButton`.
5. **Pestañas**: `BentoTabs` (ya tienen degradado de “hay más”); no re-implementes pills.
6. **Chips/filtros**: `AppFilterChip` (ya miden ≥40dp); los que no caben van en fila con scroll; nunca en dos columnas apretadas.
7. **Formularios**: por bloques con título, campos a 1 columna en móvil, botón primario al final; `BentoSheet`/`AppModal` para diálogos.
8. **Estados**: carga con `BentoState(loading=true)` o `LoadingState`; vacío con acción; error con reintento. Nunca spinners infinitos ni listas en blanco.
9. **Números grandes** (32–34dp, negrita tabular) y etiquetas 11–13dp; contexto a la derecha en `onSurfaceVariant`.

## Referencias para mirar antes de tocar

- Capturas actuales: `screenshots/2026-10-30/*.png` (una por pantalla; las de contacto en `_contactos/`).
- Web: `web/src/pages/**`, `web/src/features/**` (bento ya aplicado) y `DESIGN_SYSTEM.md` (§6, §7.1, §18), `BENTO_REDESIGN.md`.
- Inicio como vara: `feature/home/HomeScreen.kt` (mosaicos `BentoTile`, tabs, gauge/waves) — todas las pantallas deben verse de la misma familia.

## Entrega

1. Cambios por pantalla (qué se movió, qué se agrupó, qué se eliminó).
2. `./gradlew :shared:compileCommonMainKotlinMetadata :androidApp:compileDebugKotlin` verde.
3. Reporte corto al Lead: pantallas tocadas, decisiones visuales y cualquier cosa que requiera tocar `core/ui`.
4. Sin commits (el Lead captura y commitea).
