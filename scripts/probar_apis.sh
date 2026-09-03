#!/usr/bin/env bash
# Ejercita, con curl, los cuatro servicios del proyecto (core-service + los tres BFF) usando
# datos reales del dataset oficial (bank_legacy_data, semana_3, cuenta_id=101 y cuenta_id=105
# tras la validacion/upsert aplicada por CargaDatosService: ver README.md, tabla de
# "Credenciales de demostracion"). Se usa como evidencia de ejecucion en
# .github/workflows/evidencia-ejecucion.yml y tambien sirve para probar el proyecto en un
# entorno local.
#
# Requiere: curl, jq. Se asume que los 4 servicios ya estan arriba en los puertos por defecto
# (core-service:8080, bff-web:8081, bff-mobile:8082, bff-atm:8083).
set -euo pipefail

CORE=http://localhost:8080
WEB=http://localhost:8081
MOBILE=http://localhost:8082
ATM=http://localhost:8083
CLAVE_INTERNA="clave-interna-banco-xyz-2026"

separador() { echo; echo "=== $1 ==="; }

separador "0. core-service NO debe responder sin la clave interna (principio central del BFF)"
curl -s -o /dev/null -w "GET /internal/cuentas SIN clave -> HTTP %{http_code} (se espera 403)\n" "$CORE/internal/cuentas"

separador "0.1 core-service SI responde con la clave interna (uso exclusivo de los BFF)"
curl -s -H "X-Internal-Api-Key: $CLAVE_INTERNA" "$CORE/internal/cuentas/101" | jq .

separador "1. BFF WEB: login (cuenta 101, titular 'John Doe') y consulta completa de la cuenta"
TOKEN_WEB=$(curl -s -X POST "$WEB/api/web/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"cuentaId":101,"nombre":"John Doe"}' | jq -r .token)
echo "Token web obtenido: ${TOKEN_WEB:0:24}..."

curl -s -H "Authorization: Bearer $TOKEN_WEB" "$WEB/api/web/cuentas/101" | jq .

separador "1.1 BFF WEB: historial de movimientos filtrado por tipo=deposito (interfaz compleja)"
curl -s -H "Authorization: Bearer $TOKEN_WEB" "$WEB/api/web/cuentas/101/movimientos?tipo=deposito" | jq .

separador "1.2 BFF WEB: un token valido NO puede consultar la cuenta de otra persona (autorizacion)"
curl -s -o /dev/null -w "GET /api/web/cuentas/105 con token de la cuenta 101 -> HTTP %{http_code} (se espera 403)\n" \
  -H "Authorization: Bearer $TOKEN_WEB" "$WEB/api/web/cuentas/105"

separador "2. BFF MOVIL: login (cuenta 101, PIN determinista) y resumen liviano"
TOKEN_MOBILE=$(curl -s -X POST "$MOBILE/api/mobile/auth/login" \
  -H "Content-Type: application/json" \
  -d '{"cuentaId":101,"pin":"7373"}' | jq -r .token)
echo "Token movil obtenido: ${TOKEN_MOBILE:0:24}..."

curl -s -H "Authorization: Bearer $TOKEN_MOBILE" "$MOBILE/api/mobile/cuentas/101/resumen" | jq .
echo "(Comparar el tamano de esta respuesta con la de BFF WEB del paso 1: no trae titular, edad ni historial completo)"

separador "3. BFF CAJERO: apertura de sesion con tarjeta+PIN (cuenta 105, 'Steve Rogers')"
SESION_JSON=$(curl -s -X POST "$ATM/api/atm/sesion" \
  -H "Content-Type: application/json" \
  -d '{"numeroTarjeta":"4915000000000105","pin":"7665"}')
echo "$SESION_JSON" | jq .
TOKEN_ATM=$(echo "$SESION_JSON" | jq -r .sessionToken)

separador "3.1 BFF CAJERO: consulta de saldo (respuesta minima, sin nombre ni historial)"
curl -s -H "X-Atm-Session: $TOKEN_ATM" "$ATM/api/atm/cuentas/105/saldo" | jq .

separador "3.2 BFF CAJERO: retiro de \$1.000 (operacion critica)"
curl -s -X POST "$ATM/api/atm/cuentas/105/retiro" \
  -H "Content-Type: application/json" \
  -H "X-Atm-Session: $TOKEN_ATM" \
  -d '{"monto":1000}' | jq .

separador "3.3 BFF CAJERO: la sesion se invalida tras el retiro (debe fallar con HTTP 401)"
curl -s -o /dev/null -w "GET /api/atm/cuentas/105/saldo reusando la sesion ya usada -> HTTP %{http_code} (se espera 401)\n" \
  -H "X-Atm-Session: $TOKEN_ATM" "$ATM/api/atm/cuentas/105/saldo"

separador "3.4 BFF CAJERO: un retiro que excede el limite maximo por operacion debe rechazarse"
SESION_JSON_2=$(curl -s -X POST "$ATM/api/atm/sesion" \
  -H "Content-Type: application/json" \
  -d '{"numeroTarjeta":"4915000000000105","pin":"7665"}')
TOKEN_ATM_2=$(echo "$SESION_JSON_2" | jq -r .sessionToken)
curl -s -X POST "$ATM/api/atm/cuentas/105/retiro" \
  -H "Content-Type: application/json" \
  -H "X-Atm-Session: $TOKEN_ATM_2" \
  -d '{"monto":999999}' | jq .

echo
echo "=== Fin de las pruebas ==="
