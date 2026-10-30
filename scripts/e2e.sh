#!/usr/bin/env bash
# Pruebas E2E de la app con Maestro contra el API local.
#
#   ./scripts/e2e.sh                 # corre el smoke
#   ./scripts/e2e.sh smoke.yaml      # un flujo de .maestro/
#   ./scripts/e2e.sh all             # todos los flujos, EN SERIE
#
# Requisitos: API local en :4001 (api/ → npm run dev), adb con el teléfono
# conectado y la app debug instalada (./gradlew :androidApp:installDebug).
set -euo pipefail
cd "$(dirname "$0")/.."

PKG="com.axzydev.puertonuevoapp"
API_URL="${E2E_API_URL:-http://localhost:4001/api/v1}"
USERNAME="${E2E_USER:-martin.amaro}"
PASSWORD="${E2E_PASSWORD:-123456}"
MAESTRO_ENV=(--env "E2E_API_URL=$API_URL" --env "E2E_USER=$USERNAME" --env "E2E_PASSWORD=$PASSWORD")

# Deja el teléfono listo para un flujo: sesión limpia, URL del API ya escrita en las
# preferencias (así el login no pelea con el teclado/autofill), permisos concedidos y
# sin diálogos del sistema colgando de la corrida anterior.
prepare_app() {
  adb reverse tcp:4001 tcp:4001 >/dev/null
  adb shell pm clear "$PKG" >/dev/null
  adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/ptnv_session.xml'" <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="server_url">$API_URL</string>
</map>
XML
  for perm in android.permission.CAMERA android.permission.ACCESS_FINE_LOCATION android.permission.ACCESS_COARSE_LOCATION android.permission.POST_NOTIFICATIONS; do
    adb shell pm grant "$PKG" "$perm" >/dev/null 2>&1 || true
  done
  adb shell input keyevent KEYCODE_BACK >/dev/null 2>&1 || true
  adb shell input keyevent KEYCODE_BACK >/dev/null 2>&1 || true
}

FLOW="${1:-smoke.yaml}"
if [ "$FLOW" = "all" ]; then
  status=0
  for flow in .maestro/*.yaml; do
    name=$(basename "$flow")
    [ "$name" = "login.yaml" ] && continue
    echo "=== $name ==="
    prepare_app
    if ! maestro test "${MAESTRO_ENV[@]}" "$flow"; then
      status=1
    fi
  done
  exit $status
fi

prepare_app
exec maestro test "${MAESTRO_ENV[@]}" ".maestro/$FLOW"
