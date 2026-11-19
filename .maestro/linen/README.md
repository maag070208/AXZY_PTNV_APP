# Maestro · Blancos (pedido por habitación)

Flujo completo del módulo Blancos en la app, con los **tres roles reales**:

1. **Supervisora**: check-out de la habitación (crea el pedido con la dotación) → agrega extras.
2. **Lavandería**: surte y marca «Listo».
3. **Auxiliar de piso**: «Recogí» → «Entregado» (con el sucio recogido).
4. **Supervisora**: abre el detalle (línea de tiempo «Pidió / Listo / Recogió / Entregado») y reporta la diferencia.

## Correr

```bash
# API local en :4001 + Postgres en docker + teléfono USB con la app debug instalada
./scripts/linen-e2e.sh
```

El script:
- deja la habitación **HN125** en `OCCUPIED` y borra sus tareas/pedidos abiertos **en la base local** (para que el check-out siempre cree un pedido nuevo);
- limpia los datos de la app y escribe la URL del API (`scripts/e2e.sh`);
- pasa los usuarios/contraseña por variables de entorno y corre `linen/linen-completo.yaml`.

Para no romper `./scripts/e2e.sh all`, este flujo vive en `linen/` (subcarpeta): necesita preparación de base y variables extra, así que **no** entra en la corrida masiva.

Variables (todas opcionales):

| Variable | Default | |
|---|---|---|
| `E2E_LINEN_ROOM` | `HN125` | habitación a desocupar |
| `E2E_LINEN_SUPER` | `super` | supervisora (HOUSEKEEPING_SUPERVISOR) |
| `E2E_LINEN_LAUNDRY` | `lava` | lavandería (HOTEL_LAUNDRY) |
| `E2E_LINEN_FLOOR` | `piso` | auxiliar de piso (FLOOR_ASSISTANT) |
| `E2E_LINEN_PASSWORD` | `1234` | contraseña de los tres |
| `PG_CONTAINER` | `cartas-postgres` | contenedor de Postgres local |

## Notas

- **Maestro no ve los `testTag` dentro de diálogos ni de menús emergentes** (van en otra ventana). Por eso el flujo usa textos («Agregar», «Reportar», «Entregado») y, para el campo de notas de la diferencia, toca el área del campo (`50%,69%` de la pantalla). Si cambia el teléfono o el layout del diálogo, ese punto es el primero que hay que ajustar.
- Los `testTag` de filas, botones de acción y check-out sí se usan (`linen.row.*`, `linen.action.*`, `linen.checkout.*`) porque están en la ventana principal.
- La preparación toca **solo la base local**: estado de HN125, tareas de limpieza abiertas y pedidos abiertos de esa habitación. No toca catálogos, stock ni otras habitaciones.
- Al terminar, la habitación queda en `CLEANING` y el pedido en `RECEIVED` (historial); la siguiente corrida la vuelve a preparar.
