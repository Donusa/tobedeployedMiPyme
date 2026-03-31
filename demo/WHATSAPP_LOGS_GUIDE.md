# Guía de Logs para WhatsApp Webhook - MiPyme

## Resumen de Cambios

Se han agregado logs exhaustivos en toda la cadena de procesamiento de webhooks de WhatsApp para diagnosticar por qué los mensajes no se están reflejando en el frontend.

---

## 1. LOGS EN WhatsAppWebhookController

### 1.1 Status Check - `/api/whatsapp/status`
**Propósito:** Verificar si hay una conexión de WhatsApp activa

```log
=== WhatsApp STATUS CHECK ===
✓ WhatsApp connection is CONNECTED
  Phone Number ID: 123456789
  WABA ID: 987654321
  Display Phone: +55 11 98765-4321
```

**Qué buscar:**
- Si NO ves `✓ WhatsApp connection is CONNECTED`, la integración no está configurada
- Verifica que `Phone Number ID` y `WABA ID` sean correctos

---

### 1.2 Webhook Verification - GET `/api/whatsapp/webhook`
**Propósito:** Responder al desafío de verificación de Meta cuando configuras el webhook

```log
=== WhatsApp WEBHOOK VERIFICATION INITIATED ===
Mode: subscribe
Challenge: abc123def456xyz
Verify Token Received: my_token_123
Verify Token Expected: my_token_123
Mode matches 'subscribe': true
Token matches: true
✓ WhatsApp webhook VERIFIED SUCCESSFULLY. Responding with challenge.
```

**Qué buscar:**
- Si ves `Token matches: false`, el token está mal configurado en Meta
- Si ves `Mode matches 'subscribe': false`, el parámetro `hub.mode` está incorrecto (no debería pasar)

---

### 1.3 Webhook POST Handler - POST `/api/whatsapp/webhook`  
**Propósito:** Recibir y validar los webhooks de Meta

```log
=== WhatsApp WEBHOOK POST RECEIVED ===
Remote IP: 192.168.1.100
Signature Header: sha256=abc123def456...
Raw Body Length: 1250 bytes
Raw Body (first 500 chars): {"entry":[{"id":"123456789","changes":[...

Validating webhook signature...

=== WEBHOOK SIGNATURE VALIDATION ===
Signature Header: sha256=abc123def456...
App Secret configured: true
Raw Body Length: 1250 bytes
Expected Hash: abc123def456...
Computed Hash: abc123def456...
✓ Signature validation PASSED

Starting webhook payload processing...
✓ Webhook payload processed successfully
=== WhatsApp WEBHOOK POST COMPLETED ===
```

**Qué buscar:**
- `✗ Signature validation FAILED`: El `meta-app-secret` en `application.properties` es incorrecto
  - Edita: `whatsapp.meta-app-secret` en properties
  - Obtén el valor correcto en Meta App Dashboard → Settings → Basic

- `Expected Hash` ≠ `Computed Hash`: Los datos se corrompieron en tránsito (muy raro)

---

## 2. LOGS EN WppWebhookService

### 2.1 Resolución de Tenant

Este es un paso CRÍTICO - sin resolver el tenant, no se guardan los mensajes.

```log
    Resolving tenant by phoneNumberId: 123456789
    Found 2 total companies to search
      [1/2] Checking company tenant: tenant_company_a
        ℹ Exception checking tenant tenant_company_a (tables might not exist): ...
      [2/2] Checking company tenant: tenant_company_b
        ✓ Found WppConnection - ID: 5, Status: CONNECTED
        ✓ Connection is CONNECTED - TENANT RESOLVED: tenant_company_b
```

**Qué buscar - CRÍTICO:**
- Si ves `✗ NO TENANT FOUND for phoneNumberId: 123456789`, el phoneNumberId no coincide
  - Verifica que en la BD, la tabla `wpp_connections` tenga el `phone_number_id` correcto
  - Consulta: `SELECT * FROM wpp_connections;`

---

### 2.2 Procesamiento de Payload

```log
=== WEBHOOK PAYLOAD PROCESSING STARTED ===
Event Hash: sha256_abc123...
✓ JSON parsed successfully
Found 3 entries
--- Processing entry 1/3 ---
  Found 1 changes in entry
  -- Processing change 1/1 --
    Phone Number ID: 123456789
    Resolving tenant for phone_number_id: 123456789
    ✓ Tenant resolved: tenant_company_b
    Checking for duplicate event...
    ✓ Event is not a duplicate, saving to webhook log
    ✓ Webhook log saved with ID: 42
    Processing 1 messages
    Processing 2 statuses
    Marking webhook as processed
    ✓ Webhook marked as processed
=== WEBHOOK PAYLOAD PROCESSING COMPLETED ===
```

**Qué buscar:**
- `✗ Duplicate webhook event`: El mismo evento llegó dos veces (Meta está reintentando)
- `Processing 0 messages`: No hay mensajes en el payload (podría ser solo cambios de estado)
- `Processing 0 statuses`: No hay actualizaciones de estado

---

### 2.3 Procesamiento de Mensajes Entrantes

```log
    === PROCESSING INCOMING MESSAGE ===
    Message WAMID: wamid.123456789_1234567890123456
    Checking for duplicate message (WAMID: wamid.123456789_1234567890123456)...
    ✓ Message is not a duplicate
    From: 5521987654321
    Type: text
    Timestamp: 2026-03-02T12:05:35
    Looking up or creating conversation for contact: 5521987654321
    ℹ Creating new conversation for contact: 5521987654321
    Looking for contact name in 1 contacts...
    ✓ Contact name found: Juan Jose
    Saving conversation | Unread Count: 1
    ✓ Conversation saved with ID: 15
    Text Body: Hola! Necesito ayuda con mi pedido
    ✓ SAVED incoming message - ID: 42, WAMID: wamid.123456789_1234567890123456, From: 5521987654321, Type: text
```

**Qué buscar - CRÍTICO:**
- `✗ Duplicate message wamid=...`: El mensaje ya existe en la BD
  - Consulta: `SELECT * FROM wpp_messages WHERE wamid = 'wamid.123456789_...';`
  
- Si NO ves este bloque: No hay mensajes en el payload
  - Verifica que WhatsApp está enviando mensajes correctamente
  - Comprueba que el webhook URL esté bien configurado en Meta

- `Message ID: 42` = el ID guardado en la BD
  - Verifica en: `SELECT * FROM wpp_messages WHERE id = 42;`

---

### 2.4 Procesamiento de Actualizaciones de Estado

```log
    === PROCESSING STATUS UPDATE ===
    Message WAMID: wamid.123456789_9876543210987654
    Status: sent
    Timestamp: 2026-03-02T12:05:36
    Checking for duplicate status...
    ✓ SAVED status update - WAMID: wamid.123456789_9876543210987654, Status: sent
```

Los estados pueden ser: `sent`, `delivered`, `read`, `failed`

**Estados críticos:**
- `Status: failed`: El mensaje no se entregó
  - Habrá un log adicional: `✗ Error in status update - Code: XXX, Message: ...`

---

## 3. PROBLEMA: Mensajes NO se ven en el Frontend

### Árbol de Diagnóstico

```
PROBLEMA: Conversación no aparece en el frontend

├─ PASO 1: ¿Llega el webhook a la aplicación?
│   └─ Busca en logs: "=== WhatsApp WEBHOOK POST RECEIVED ==="
│      ├─ NO → Verifica que webhook URL esté bien en Meta Dashboard
│      └─ SÍ → Continúa

├─ PASO 2: ¿Se valida la firma correctamente?
│   └─ Busca: "✓ Signature validation PASSED"
│      ├─ NO "✗ Signature validation FAILED" → Verifica `whatsapp.meta-app-secret`
│      └─ SÍ → Continúa

├─ PASO 3: ¿Se resuelve el tenant?
│   └─ Busca: "✓ Connection is CONNECTED - TENANT RESOLVED"
│      ├─ NO "✗ NO TENANT FOUND" → El phoneNumberId no coincide en la BD
│      └─ SÍ → Continúa

├─ PASO 4: ¿Se procesa el mensaje?
│   └─ Busca: "=== PROCESSING INCOMING MESSAGE ==="
│      ├─ NO → No hay mensajes en este webhook
│      └─ SÍ → Continúa

├─ PASO 5: ¿Se guarda el mensaje?
│   └─ Busca: "✓ SAVED incoming message"
│      ├─ NO "✗ Duplicate message wamid" → Mensaje ya existe (no error real)
│      └─ SÍ → El error está en el FRONTEND

└─ PASO 6: Frontend no muestra el mensaje
    └─ Comprueba en las logs del FRONTEND:
       ├─ ¿Se llama a `WppConversationService.getConversations()`?
       ├─ ¿Se actualiza la lista después de recibir el mensaje?
       └─ ¿Hay socket.io o WebSocket configurados para actualizar en tiempo real?
```

---

## 4. Comandos SQL para Diagnóstico

Ejecuta estos comandos en la BD para verificar que los datos se están guardando:

### Verificar conexión WhatsApp
```sql
SELECT * FROM wpp_connections WHERE status = 'CONNECTED';
```

Debería retornar 1 fila con:
- `phone_number_id`: El ID que Meta envía en el webhook
- `status`: CONNECTED
- `display_phone_number`: El número de WhatsApp

### Verificar webhook logs
```sql
SELECT * FROM wpp_webhook_log ORDER BY received_at DESC LIMIT 10;
```

Debería haber registros recientes

### Verificar conversaciones
```sql
SELECT * FROM wpp_conversations ORDER BY last_message_at DESC;
```

Si hay un mensaje, debería existir la conversación con:
- `contact_wa_id`: El número del contacto
- `contact_name`: Nombre del contacto
- `last_message_at`: Timestamp reciente
- `unread_count`: > 0

### Verificar mensajes
```sql
SELECT 
  wm.id, wm.wamid, wm.type, wm.text_body, 
  wm.timestamp, wc.contact_name, wc.contact_wa_id
FROM wpp_messages wm
JOIN wpp_conversations wc ON wm.conversation_id = wc.id
ORDER BY wm.timestamp DESC
LIMIT 10;
```

Si el mensaje llegó correctamente, deberías ver:
- `text_body`: El texto del mensaje
- `contact_name`: Nombre del contacto
- `timestamp`: Hora cuando llegó

---

## 5. Configuración Requerida en application.properties

Verifica que tengas:

```properties
# Webhook token para verificación
whatsapp.verify-token=your_verify_token_here

# App Secret de Meta (MUY IMPORTANTE)
whatsapp.meta-app-secret=your_app_secret_here
```

**¿Dónde obtenerlos?**
1. Ve a: https://developers.facebook.com/apps/
2. Selecciona tu aplicación
3. Settings → Basic
4. Copia "App Secret" → `whatsapp.meta-app-secret`
5. Ve a tu Webhook → Settings
6. Copia el "Verify Token" → `whatsapp.verify-token`

---

## 6. Frontend - Qué Verificar

Incluso si los logs del backend son perfectos, el frontend podría no actualizar:

### 1. ¿Se está llamando getConversations()?
```typescript
// En miPyme/src/app/services/wpp.service.ts
getConversations() {
  return this.http.get('/api/whatsapp/conversations');
}
```

### 2. ¿Se actualiza automáticamente después de nuevo mensaje?
Debería haber un mecanismo de:
- WebSocket escuchando eventos
- Socket.io actualizando la UI
- Poll cada X segundos

### 3. ¿La conversación se renderiza en el template?
Busca en componentes tipo `WppConversationListComponent`

---

## 7. Ejemplo de Logs Correctos (TODO FUNCIONANDO)

```
=== WhatsApp WEBHOOK POST RECEIVED ===
Remote IP: 190.123.45.67
Signature Header: sha256=abc123...
Raw Body Length: 1450 bytes

=== WEBHOOK SIGNATURE VALIDATION ===
✓ Signature validation PASSED

Starting webhook payload processing...
=== WEBHOOK PAYLOAD PROCESSING STARTED ===
Event Hash: sha256_def456...
✓ JSON parsed successfully
Found 1 entries
--- Processing entry 1/1 ---
  Found 1 changes in entry
  -- Processing change 1/1 --
    Phone Number ID: 123456789
    Resolving tenant for phone_number_id: 123456789
    ✓ Connection is CONNECTED - TENANT RESOLVED: tenant_mipyme_store

    === PROCESSING INCOMING MESSAGE ===
    Message WAMID: wamid.123456789_1709397935
    ✓ Message is not a duplicate
    From: 5521987654321
    Type: text
    Timestamp: 2026-03-02T12:05:35
    ℹ Creating new conversation for contact: 5521987654321
    ✓ Contact name found: María López
    ✓ Conversation saved with ID: 8
    Text Body: Hola! Me interesa el talle M
    ✓ SAVED incoming message - ID: 156, WAMID: wamid.123456789_1709397935, From: 5521987654321, Type: text

✓ Webhook payload processed successfully
=== WhatsApp WEBHOOK POST COMPLETED ===
```

**En este caso:** El mensaje se guardó exitosamente en la BD. Si no aparece en el frontend, es un problema del lado del cliente.

---

## 8. Próximos Pasos

1. **Inicia la aplicación:**
   ```bash
   cd demo
   mvn spring-boot:run
   ```

2. **Envía un mensaje desde WhatsApp** al número configurado

3. **Revisa los logs en tiempo real** (busca `=== WhatsApp` en la consola)

4. **Sigue el árbol de diagnóstico** (sección 3) para identificar dónde falla

5. **Ejecuta los comandos SQL** (sección 4) para verificar los datos

6. **Si todo en backend está OK**, el problema está en el frontend (WebSocket, actualización automática, etc.)

---

## Contacto

Si los logs muestran que el mensaje se guardó pero no aparece en el frontend, necesitamos revisar:
- El servicio WppConversationService
- El componente de lista de conversaciones
- Las subscripciones WebSocket/Socket.io
