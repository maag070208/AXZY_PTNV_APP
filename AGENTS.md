# Puerto Nuevo App — Reglas de trabajo

## REGLA FUNDAMENTAL: reutilización y estandarización de componentes

Todo elemento de UI (cards, hovers/filas pulsables, inputs, chips, modales,
botones) DEBE usar los componentes estándar de `core/ui` y los tokens de
`core/theme`. **Prohibido reimplementar** estilos locales con `Modifier`
sueltos (`.background(...).clickable(...)`, `OutlinedTextField`, etc.).

Si algo no existe en el estándar: se crea UN componente reutilizable en
`core/ui` y se usa en todas partes. Nunca se parchea pantalla por pantalla.

### Componentes estándar (fuente de verdad)

- **Shapes** — `core/theme/Shape.kt` → `AppShape`:
  `card` (16dp), `row` (12dp), `icon` (12dp), `pill` (50%).
  No usar `RoundedCornerShape(<n>.dp)` en pantallas; usar `AppShape.*`.
- **Cards** — `core/ui/Components.kt`:
  - `AppCard(modifier, onClick?, shape, containerColor, borderColor, contentPadding, content)`
    tarjeta base; ya recorta el ripple a la forma. Úsala para cualquier card/fila-card.
  - `AppActionCard(label, icon, onClick, accent)` tile de accesos rápidos.
  - `AppSurfaceCard { ... }` tarjeta contenedora con borde.
  - `StatCard(value, label, color, icon, onClick?)`.
- **Filas/hover pulsables** — siempre `Modifier.clip(shape)` ANTES de
  `.background(...)` y `.clickable(...)`. El contenido debe llevar un padding
  tipo `p-2` (8dp) respecto al hover. Para filas dentro de cards usar
  `AppShape.row`.
- **Inputs** — `AppTextField`, `AppSearchField`, `SimpleDropdownField`.
  Prohibido `OutlinedTextField`/`BasicTextField` directo.
- **Chips** — `StatusChip(label, color)` (usa `AppShape.pill`). Los filtros
  clicables: `.clip(AppShape.pill).background(color, AppShape.pill).clickable{}`.
- **Modales** — `AppModal(...)`.
- **Colores** — `MaterialTheme.colorScheme.*` + `AppColors` (semánticos). No
  hardcodear colores.

### Checklist antes de dar por terminada una tarea de UI

1. ¿Existe ya un componente estándar para esto? Si sí, úsalo.
2. ¿Usaste `AppShape` en vez de radios hardcodeados?
3. ¿Los pulsables tienen `.clip(shape)` antes de `.clickable`?
4. ¿Los inputs son `AppTextField`?
5. Compila: `./gradlew :shared:compileCommonMainKotlinMetadata`
   (y `:androidApp:processDebugResources` si tocaste recursos).

## Iconos de app

- Android: `androidApp/src/main/res/mipmap-*` (`ic_launcher.png`,
  `ic_launcher_round.png`, capas adaptativas `ic_launcher_adaptive_fore/back.png`).
- iOS: `iosApp/iosApp/Assets.xcassets/AppIcon.appiconset/app-icon-1024.png`.

## Pruebas E2E (Maestro)

La app tiene flujos de UI en `.maestro/` que corren contra el **API local real** con el teléfono por USB:

```bash
cd PuertoNuevoApp
./scripts/e2e.sh                    # smoke
./scripts/e2e.sh inventory-returns.yaml
./scripts/e2e.sh all                # todos, en serie (cada flujo reinicia la app)
```

- El script hace `adb reverse tcp:4001`, limpia los datos de la app, escribe la URL del API en las preferencias (`ptnv_session`) y concede los permisos del sistema; así los flujos no pelean con teclado/autofill/diálogos.
- Credenciales por variables: `E2E_USER` / `E2E_PASSWORD` / `E2E_API_URL` (por defecto la cuenta ADMIN de pruebas sembrada en local).
- Los campos y acciones nuevas necesitan un `testTag` (`AppTextField(testTag = "...")`, `Modifier.testTag("...")` en filas de menú/shell) para que Maestro los seleccione por `id:` en vez de por texto/coordenadas.
- Regla del dueño: se corre **solo el flujo del cambio** mientras se desarrolla; la carpeta completa se corre al cerrar una fase o antes de entregar.
