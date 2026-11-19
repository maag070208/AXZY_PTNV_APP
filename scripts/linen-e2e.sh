#!/usr/bin/env bash
# Flujo completo de Blancos en la app con Maestro (3 roles: supervisora → lavandería → piso).
#
#   ./scripts/linen-e2e.sh                            # prepara HN125 y corre el flujo completo
#   ./scripts/linen-e2e.sh linen/linen-completo.yaml  # (explícito) el mismo flujo
#
# Antes de correr: API local en :4001 (api/ → npm run dev), Postgres de desarrollo arriba
# (docker `cartas-postgres`) y el teléfono por USB con la app debug instalada
# (./gradlew :androidApp:installDebug).
#
# La preparación solo toca la base LOCAL: deja la habitación en OCCUPIED, sin tarea de
# limpieza abierta y sin pedido de blancos abierto, para que el check-out cree uno nuevo.
# Los usuarios de prueba son los del entorno local (super / lava / piso, contraseña 1234);
# se pueden cambiar con E2E_LINEN_SUPER, E2E_LINEN_LAUNDRY, E2E_LINEN_FLOOR,
# E2E_LINEN_PASSWORD y E2E_LINEN_ROOM.
set -euo pipefail
cd "$(dirname "$0")/.."

ROOM="${E2E_LINEN_ROOM:-HN125}"
export E2E_LINEN_ROOM="$ROOM"
export E2E_LINEN_SUPER="${E2E_LINEN_SUPER:-super}"
export E2E_LINEN_LAUNDRY="${E2E_LINEN_LAUNDRY:-lava}"
export E2E_LINEN_FLOOR="${E2E_LINEN_FLOOR:-piso}"
export E2E_LINEN_PASSWORD="${E2E_LINEN_PASSWORD:-1234}"

ENV_FILE="../api/.env"
DATABASE_URL="${DATABASE_URL:-$(grep -E '^DATABASE_URL=' "$ENV_FILE" | head -1 | cut -d= -f2-)}"
if [ -z "$DATABASE_URL" ]; then
  echo "No encontré DATABASE_URL (revisa $ENV_FILE)" >&2
  exit 1
fi

python3 - "$DATABASE_URL" "$ROOM" <<'PY'
import os, re, subprocess, sys
from urllib.parse import urlparse, unquote

url = urlparse(sys.argv[1])
room = sys.argv[2]
if not re.fullmatch(r"[A-Za-z0-9_-]{1,20}", room):
    sys.exit(f"Nombre de habitación inválido: {room!r}")
if url.hostname not in ("localhost", "127.0.0.1", "::1", "postgres", "db") and os.environ.get("E2E_ALLOW_REMOTE_DB") != "1":
    sys.exit(f"Se esperaba una base local y DATABASE_URL apunta a {url.hostname!r}. "
             "Si es intencional, exporta E2E_ALLOW_REMOTE_DB=1.")

container = os.environ.get("PG_CONTAINER", "cartas-postgres")
sql = """
UPDATE hotel_rooms SET status = 'OCCUPIED', "updatedAt" = now() WHERE number = :'room';
DELETE FROM hotel_supply_requests
 WHERE "roomId" = (SELECT id FROM hotel_rooms WHERE number = :'room')
   AND origin = 'ROOM_TURNOVER'
   AND status IN ('SUBMITTED', 'PARTIALLY_FULFILLED', 'READY_FOR_PICKUP', 'IN_TRANSIT');
DELETE FROM hotel_housekeeping_tasks
 WHERE "roomId" = (SELECT id FROM hotel_rooms WHERE number = :'room')
   AND status IN ('PENDING', 'IN_PROGRESS');
"""
cmd = [
    "docker", "exec", "-i", "-e", f"PGPASSWORD={unquote(url.password or '')}", container,
    "psql", "-U", unquote(url.username or "postgres"), "-d", url.path.lstrip("/"),
    "-v", "ON_ERROR_STOP=1", "-c", sql.replace(":'room'", f"'{room}'"),
]
result = subprocess.run(cmd, capture_output=True, text=True)
if result.returncode != 0:
    sys.stderr.write(result.stderr)
    sys.exit(f"No se pudo preparar la habitación {room} en la base local (contenedor {container}).")
print(f"[linen-e2e] {room} lista: OCCUPIED, sin tareas ni pedidos abiertos")
PY

exec ./scripts/e2e.sh "${1:-linen/linen-completo.yaml}"
