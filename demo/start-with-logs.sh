#!/bin/bash
# Script para iniciar la aplicación MiPyme con WhatsApp Webhook Logging

echo "=========================================="
echo "MiPyme WhatsApp Webhook - Startup Script"
echo "=========================================="
echo ""
echo "Pasos:"
echo "1. Compilando aplicación..."
mvn clean compile -DskipTests

if [ $? -eq 0 ]; then
    echo ""
    echo "✓ Compilación exitosa"
    echo ""
    echo "2. Iniciando aplicación..."
    echo ""
    echo "Logs importante a buscar:"
    echo "  - '=== WhatsApp WEBHOOK POST RECEIVED ===' → Webhook llegó"
    echo "  - '✓ Signature validation PASSED' → Firma validada"
    echo "  - '✓ Connection is CONNECTED - TENANT RESOLVED' → Tenant encontrado"
    echo "  - '✓ SAVED incoming message' → Mensaje guardado"
    echo ""
    echo "=========================================="
    echo ""
    
    mvn spring-boot:run
else
    echo ""
    echo "✗ Error de compilación. Revisa los logs arriba."
    exit 1
fi
