#!/bin/bash

# Script para testear el endpoint de setup de WhatsApp
# Uso: ./test_wpp_setup.sh

set -e

# Configuración
API_BASE_URL="${API_BASE_URL:-http://localhost:8080}"
AUTH_TOKEN="${AUTH_TOKEN:-}"
ENDPOINT="/api/wpp/connection/setup"

# Colores para output
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
NC='\033[0m' # No Color

# Función para imprimir con color
print_info() {
    echo -e "${GREEN}➜${NC} $1"
}

print_error() {
    echo -e "${RED}✗${NC} $1"
}

print_warn() {
    echo -e "${YELLOW}⚠${NC} $1"
}

# Validar argumentos
if [ $# -lt 3 ]; then
    echo "Uso: $0 <wabaId> <phoneNumberId> <accessToken> [displayPhoneNumber]"
    echo ""
    echo "Ejemplo:"
    echo "  $0 123456789 987654321 'EAABsbCS1234567890...' '+5491234567890'"
    echo ""
    echo "Variables de entorno opcionales:"
    echo "  API_BASE_URL      - Base URL de la API (default: http://localhost:8080)"
    echo "  AUTH_TOKEN        - JWT token para autenticación"
    exit 1
fi

WABA_ID="$1"
PHONE_NUMBER_ID="$2"
ACCESS_TOKEN="$3"
DISPLAY_PHONE_NUMBER="${4:-}"

print_info "Configurando conexión WhatsApp..."
print_info "API: $API_BASE_URL$ENDPOINT"
print_info "WABA ID: $WABA_ID"
print_info "Phone Number ID: $PHONE_NUMBER_ID"
if [ -n "$DISPLAY_PHONE_NUMBER" ]; then
    print_info "Display Phone: $DISPLAY_PHONE_NUMBER"
fi
echo ""

# Construir JSON payload
JSON_PAYLOAD=$(cat <<EOF
{
  "wabaId": "$WABA_ID",
  "phoneNumberId": "$PHONE_NUMBER_ID",
  "accessToken": "$ACCESS_TOKEN"
EOF
)

if [ -n "$DISPLAY_PHONE_NUMBER" ]; then
    JSON_PAYLOAD=$(cat <<EOF
{
  "wabaId": "$WABA_ID",
  "phoneNumberId": "$PHONE_NUMBER_ID",
  "displayPhoneNumber": "$DISPLAY_PHONE_NUMBER",
  "accessToken": "$ACCESS_TOKEN"
}
EOF
)
else
    JSON_PAYLOAD="$JSON_PAYLOAD
}"
fi

# Construir headers
HEADERS=(
    "-H" "Content-Type: application/json"
)

if [ -n "$AUTH_TOKEN" ]; then
    HEADERS+=(
        "-H" "Authorization: Bearer $AUTH_TOKEN"
    )
else
    print_warn "No se especificó AUTH_TOKEN. La request podría fallar si se requiere autenticación."
fi

# Ejecutar request
print_info "Enviando request..."
echo ""

RESPONSE=$(curl -s -w "\n%{http_code}" -X POST \
    "${HEADERS[@]}" \
    -d "$JSON_PAYLOAD" \
    "$API_BASE_URL$ENDPOINT")

HTTP_CODE=$(echo "$RESPONSE" | tail -n1)
BODY=$(echo "$RESPONSE" | sed '$d')

# Parsear response
if [ "$HTTP_CODE" = "200" ]; then
    print_info "Conexión configurada exitosamente! (HTTP $HTTP_CODE)"
    echo ""
    
    # Pretty print JSON si jq está disponible
    if command -v jq &> /dev/null; then
        echo "$BODY" | jq '.'
    else
        echo "$BODY"
    fi
    
    # Extraer datos importantes
    if command -v jq &> /dev/null; then
        CONNECTION_ID=$(echo "$BODY" | jq -r '.connectionId')
        STATUS=$(echo "$BODY" | jq -r '.status')
        echo ""
        print_info "Connection ID: $CONNECTION_ID"
        print_info "Status: $STATUS"
    fi
else
    print_error "Error al configurar la conexión (HTTP $HTTP_CODE)"
    echo ""
    
    if command -v jq &> /dev/null; then
        echo "$BODY" | jq '.' 2>/dev/null || echo "$BODY"
    else
        echo "$BODY"
    fi
    
    exit 1
fi

echo ""
print_info "Los datos de conexión están listos para usar."
print_info "Ahora puedes enviar mensajes usando el endpoint /api/wpp/messages/send"
