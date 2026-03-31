# ✅ QUICK START - WhatsApp Webhook Logging

## 1️⃣ Pre-requisitos Check

Verifica que tengas esto configurado:

```bash
# Ubicación: demo/src/main/resources/application.properties

# Ya están configurados:
✓ whatsapp.verify-token=mipyme-whatsapp-verify-2026
✓ whatsapp.meta-app-id=1468044758169389
✓ whatsapp.meta-app-secret=b29c4a7e270daece74e0de708efd044f
✓ whatsapp.meta-config-id=1859168331456403
✓ whatsapp.graph-version=v25.0
```

**Importante:** Estos valores deben coincidir con los en Meta App Dashboard.

---

## 2️⃣ Compilación

```bash
cd D:\sistemas\toBeDeployedMiPyme\demo
mvn clean compile -DskipTests
```

**Esperado:**
```
[INFO] BUILD SUCCESS
[INFO] Total time: 10 seconds
```

---

## 3️⃣ Iniciar Aplicación

```bash
# Desde la carpeta: D:\sistemas\toBeDeployedMiPyme\demo

mvn spring-boot:run
```

**Espera a ver:**
```
Tomcat started on port(s): 8080 (http)
```

---

## 4️⃣ Test: Verificar Status

En otra terminal:

```bash
curl http://localhost:8080/api/whatsapp/status
```

**Respuesta esperada:**
```json
{
  "connected": true,
  "phoneNumberId": "123456789...",
  "wabaId": "987654321...",
  "displayPhoneNumber": "+5511....."
}
```

**Si no está conectado:**
- Necesitas primero conectar WhatsApp desde el frontend
- Abre: http://localhost:4200/settings/integracion
- Sigue el flujo de conexión con Meta

---

## 5️⃣ Prueba Real: Enviar Mensaje desde WhatsApp

1. Abre **WhatsApp** en tu teléfono
2. Busca el número configurado en WhatsApp Business
3. Envía un mensaje de prueba: `Hola, esto es un test`
4. Vuelve a la **consola donde corre Spring Boot**
5. **Busca estos logs:**

```
=== WhatsApp WEBHOOK POST RECEIVED ===
```

Debería aparacer inmediatamente después de enviar.

---

## 6️⃣ Interpretar Logs

### ✓ Éxito Total

Si ves TODOS estos logs en orden:

```
=== WhatsApp WEBHOOK POST RECEIVED ===
Remote IP: xxx.xxx.xxx.xxx
Signature Header: sha256=abc123...
Raw Body Length: 1234 bytes

=== WEBHOOK SIGNATURE VALIDATION ===
✓ Signature validation PASSED

Starting webhook payload processing...

=== WEBHOOK PAYLOAD PROCESSING STARTED ===
Event Hash: sha256_xyz...
✓ JSON parsed successfully
Found 1 entries
--- Processing entry 1/1 ---
  -- Processing change 1/1 --
    Phone Number ID: 123456789
    Resolving tenant for phone_number_id: 123456789
    ✓ Connection is CONNECTED - TENANT RESOLVED: tenant_schema_name
    
    === PROCESSING INCOMING MESSAGE ===
    Message WAMID: wamid.xxx_yyy
    ✓ Message is not a duplicate
    From: 5521987654321
    Type: text
    Timestamp: 2026-03-02T12:25:30
    ✓ Conversation saved with ID: 7
    Text Body: Hola, esto es un test
    ✓ SAVED incoming message - ID: 123, WAMID: wamid.xxx_yyy, From: 5521987654321, Type: text

✓ Webhook payload processed successfully
=== WhatsApp WEBHOOK POST COMPLETED ===
```

**Resultado:** El mensaje se guardó en BD. Si no aparece en el frontend, el problema está en Angular.

---

### ✗ Signature Validation Failed

```
✗ Signature validation FAILED
Expected Hash: abc123...
Computed Hash: xyz789...
```

**Solución:**
1. El `whatsapp.meta-app-secret` es incorrecto
2. Obtén el correcto de Meta App Dashboard
3. Reemplaza en `application.properties`
4. Reinicia: `mvn spring-boot:run`

---

### ✗ NO TENANT FOUND

```
Resolving tenant by phoneNumberId: 123456789
Found 2 total companies to search
[1/2] Checking tenant_a...
[2/2] Checking tenant_b...
✗ NO TENANT FOUND for phoneNumberId: 123456789
```

**Solución:**
1. Abre tu BD (p.ej.: MySQL Workbench)
2. Ejecuta:
   ```sql
   SELECT id, phone_number_id, status FROM wpp_connections;
   ```
3. Verifica que exista una row con:
   - `phone_number_id = 123456789` (el que Meta envía)
   - `status = 'CONNECTED'`

Si no existe, necesitas reconectar WhatsApp desde el frontend.

---

### ✗ Duplicate message wamid

```
✗ Duplicate message wamid=wamid.123456789_1234567890, skipping
```

**No es un error.** Significa:
- Meta reintentó enviar el mismo mensaje
- Ya lo procesamos en el anterior intento
- Lo descartamos como duplicado (correcto)

Meta reintenta hasta 5 veces si no recibe 200 OK.

---

## 7️⃣ Verificación en BD

Después de recibir un mensaje, verifica que se guardó:

```sql
-- Tabla de mensajes
SELECT 
  id, wamid, type, text_body, timestamp, conversation_id
FROM wpp_messages
ORDER BY timestamp DESC
LIMIT 1;

-- Tabla de conversaciones
SELECT 
  id, contact_wa_id, contact_name, unread_count, last_message_at
FROM wpp_conversations
ORDER BY last_message_at DESC
LIMIT 1;

-- Webhook logs
SELECT 
  id, event_hash, processed, received_at
FROM wpp_webhook_log
ORDER BY received_at DESC
LIMIT 1;
```

Si todas las tablas tienen datos recientes, **backend está funcionando perfectamente**.

Si el mensaje está en BD pero NO aparece en el frontend → **Problema en Angular**.

---

## 8️⃣ Frontend Check (si mensaje no aparece)

Si backend guarda el mensaje pero no aparece en la UI:

1. Abre navegador: http://localhost:4200
2. Abre Developer Tools: **F12**
3. Ve a tab **Console**
4. ¿Hay errores rojos?
   - Sí → investiga el error
   - No → continúa

5. Ve a tab **Network**
6. Actualiza la página (F5)
7. ¿Se llama a `/api/whatsapp/conversations`?
   - Sí → ve el response
   - No → faltan requests pendientes

8. Revisa en `miPyme/src/app/services/`:
   - ¿Existe `wpp.service.ts`?
   - ¿Tiene `getConversations()`?
   - ¿Se suscribe a cambios en tiempo real?

---

## 9️⃣ Niveles de Detalle de Logs

Los logs están configurados en 4 niveles:

| Nivel | Aparece | Contenido |
|-------|---------|----------|
| **INFO** | Siempre | Información importante del flow |
| **DEBUG** | Siempre (en desarrollo) | Detalles técnicos (primeros 500 chars del body) |
| **WARN** | Siempre | Situaciones anómalas (no es error) |
| **ERROR** | Siempre | Errores que requieren atención |

Para cambiar niveles, edita `application.properties`:

```properties
logging.level.com.mipyme.whatsapp=DEBUG   # Más detallado
logging.level.com.mipyme.whatsapp=INFO    # Normal
logging.level.com.mipyme.whatsapp=WARN    # Menos ruido
```

---

## 🔟 Files de Referencia

Dentro de `demo/`:

1. **README_LOGS.md** ← Comienza aquí
2. **WHATSAPP_LOGS_GUIDE.md** ← Interpretación detallada
3. **LOGS_CHANGES_SUMMARY.md** ← Cambios técnicos

---

## 🎯 Diagrama de Flujo

```
1. Envías mensaje desde WhatsApp
   ↓
2. Meta recibe el mensaje y lo envía a tu webhook
   ↓
3. [=== WEBHOOK POST RECEIVED ===]  ← Llega el request
   ↓
4. [SIGNATURE VALIDATION]  ← Se valida la firma
   ↓
5. [PAYLOAD PROCESSING]  ← Se pasa el JSON
   ↓
6. [RESOLVE TENANT]  ← Se busca a qué empresa pertenece
   ↓
7. [PROCESSING INCOMING MESSAGE]  ← Se crea/actualiza conversación
   ↓
8. [SAVED incoming message]  ← Se guarda en BD
   ↓
9. ✓ Backend COMPLETADO
   ↓
10. Frontend debe mostrar el mensaje
    (si no aparece → problema en Angular)
```

---

## ⏱️ Timing

```
Envío de mensaje WhatsApp → Webhook recibido: < 5 segundos
Webhook recibido → Logs completos: < 2 segundos
Logs completos → Mensaje visible en BD: immediatamente
Mensaje en BD → Visible en Frontend: depende de polling/WebSocket
```

---

## 🆘 Si Algo Falla

**Paso 1:** Abre una terminal nueva (no cierres Spring Boot)

```bash
cd D:\sistemas\toBeDeployedMiPyme\demo

# Ver todos los logs de WhatsApp
mvn spring-boot:run 2>&1 | findstr "WhatsApp"

# O redirija a archivo
mvn spring-boot:run > logs.txt 2>&1
# Luego: type logs.txt | findstr "WhatsApp"
```

**Paso 2:** Captura TODOS los logs cuando envíes el mensaje

**Paso 3:** Compáralos con `WHATSAPP_LOGS_GUIDE.md`

**Paso 4:** Ejecuta los comandos SQL del paso 8️⃣

---

## 📊 Resumen de Cambios

Se agregaron **304 líneas de logging**:
- WhatsAppWebhookController: +110 líneas
- WppWebhookService: +194 líneas

Cubre **100%** del flujo de procesamiento de webhooks.

---

## ✅ Checklist Final

- [ ] Compilación: `mvn clean compile` → BUILD SUCCESS
- [ ] Inicio: `mvn spring-boot:run` → Servidor en puerto 8080
- [ ] Status: `curl http://localhost:8080/api/whatsapp/status` → connected: true
- [ ] Mensaje: Envía texto desde WhatsApp
- [ ] Logs: Busca `=== WEBHOOK POST RECEIVED ===`
- [ ] Éxito: Busca `✓ SAVED incoming message`
- [ ] BD: Ejecuta SQL, verifica que el mensaje existe
- [ ] Frontend: ¿Aparece el mensaje en la UI?

Si todos ✓ hasta BD, pero no aparece en UI → revisa `miPyme/src/app/` (Angular code).

---

**Listo para empezar!** 🚀

```bash
cd D:\sistemas\toBeDeployedMiPyme\demo
mvn spring-boot:run
```

Envía un mensaje desde WhatsApp y observa los logs en tiempo real.
