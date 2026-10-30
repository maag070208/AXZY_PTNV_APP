#!/usr/bin/env bash
# Pruebas E2E de la app con Maestro contra el API local.
#
#   ./scripts/e2e.sh                 # corre el smoke
#   ./scripts/e2e.sh smoke.yaml      # un flujo de .maestro/
#   ./scripts/e2e.sh tests/tickets.yaml
#
# Requisitos: API local en :4001 (api/ → npm run dev), adb con el teléfono
# conectado y la app debug instalada (./gradlew :androidApp:installDebug).
set -euo pipefail
cd "$(dirname "$0")/.."

PKG="com.axzydev.puertonuevoapp"
APK_URL="${E2E_API_URL:-http://localhost:4001/api/v1}"
USERNAME="${E2E_USER:-martin.amaro}"
PASSWORD="${E2E_PASSWORD:-123456}"

# El teléfono alcanza el API de la Mac por el loopback de adb.
adb reverse tcp:4001 tcp:4001 >/dev/null
# Cada corrida empieza deslogueada y con la URL del API ya escrita en las
# preferencias (así el flujo no pelea con el teclado ni con el autofill).
adb shell pm clear "$PKG" >/dev/null
adb shell "run-as $PKG sh -c 'mkdir -p shared_prefs && cat > shared_prefs/ptnv_session.xml'" <<XML
<?xml version='1.0' encoding='utf-8' standalone='yes' ?>
<map>
    <string name="server_url">$APK_URL</string>
</map>
XML
# Permisos del sistema ya concedidos: el flujo no pelea con los diálogos de Android.
for perm in android.permission.CAMERA android.permission.ACCESS_FINE_LOCATION android.permission.ACCESS_COARSE_LOCATION android.permission.POST_NOTIFICATIONS; do
  adb shell pm grant "$PKG" "$perm" >/dev/null 2>&1 || true
done
# Cierra cualquier diálogo del sistema que haya quedado de la corrida anterior
# (p. ej. el "guardar contraseña" de Samsung Pass).
adb shell input keyevent KEYCODE_BACK >/dev/null 2>&1 || true
adb shell input keyevent KEYCODE_BACK >/dev/null 2>&1 || true

FLOW="${1:-smoke.yaml}"
exec maestro test \
  --env "E2E_API_URL=$APK_URL" \
  --env "E2E_USER=$USERNAME" \
  --env "E2E_PASSWORD=$PASSWORD" \
  ".maestro/$FLOW"
