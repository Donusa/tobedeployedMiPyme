# Resumen de Cambios - Logs de WhatsApp Webhook

## Archivos Modificados

### 1. WhatsAppWebhookController.java
**Ubicación:** `demo/src/main/java/com/mipyme/whatsapp/controller/WhatsAppWebhookController.java`

**Cambios:**

#### Status Endpoint (GET /api/whatsapp/status)
- ✅ Agregó log al inicio: `=== WhatsApp STATUS CHECK ===`
- ✅ Agregó logs cuando encontró conexión CONECTADA
- ✅ Agregó logs cuando NO encuentra conexión
- ✅ Logs muestran: Phone Number ID, WABA ID, Display Phone

#### Webhook Verification (GET /api/whatsapp/webhook)
- ✅ Agregó log detallado: `=== WhatsApp WEBHOOK VERIFICATION INITIATED ===`
- ✅ Logs de los parámetros recibidos: mode, challenge, token
- ✅ Logs de validaciones (mode matches 'subscribe', token matches)
- ✅ Log de éxito o fallo con detalles específicos

#### Webhook POST Handler (POST /api/whatsapp/webhook)
- ✅ Agregó log de inicio: `=== WhatsApp WEBHOOK POST RECEIVED ===`
- ✅ Log de IP remota del que envía el webhook
- ✅ Log del signature header recibido
- ✅ Log del tamaño del body
- ✅ Log de los primeros 500 caracteres del body (para debugging)
- ✅ Logs de validación de firma (✓ PASSED o ✗ FAILED)
- ✅ Logs durante procesamiento del payload
- ✅ Log de error detallado si algo falla (excepción, mensaje)
- ✅ Agregó log de fin: `=== WhatsApp WEBHOOK POST COMPLETED ===`

#### Disconnect (POST /api/whatsapp/disconnect)
- ✅ Agregó log: `=== WhatsApp DISCONNECT REQUESTED ===`
- ✅ Logs cuando conecta es encontrada
- ✅ Logs de actualización de estado y cache
- ✅ Logs de error detallados

#### Deauthorize (POST /api/whatsapp/deauthorize)
- ✅ Agregó log: `=== WhatsApp DEAUTHORIZE REQUEST RECEIVED ===`
- ✅ Logs de headers y body recibidos
- ✅ Log de confirmación de procesamiento

---

### 2. WppWebhookService.java
**Ubicación:** `demo/src/main/java/com/mipyme/whatsapp/service/WppWebhookService.java`

**Cambios:**

#### validateSignature()
- ✅ Agregó log: `=== WEBHOOK SIGNATURE VALIDATION ===`
- ✅ Log si header es NULL
- ✅ Log si no empieza con "sha256="
- ✅ Logs de app secret configured y raw body length
- ✅ Log del expected hash (desde header)
- ✅ Log del computed hash (calculado localmente)
- ✅ Log de comparación: ✓ PASSED o ✗ FAILED
- ✅ Logs de excepciones con tipo y detalles

#### processPayload()
- ✅ Agregó log: `=== WEBHOOK PAYLOAD PROCESSING STARTED ===`
- ✅ Log del event hash
- ✅ Log de JSON parsed successfully
- ✅ Log del número de entries encontradas
- ✅ Logs para cada entry procesada (1/N, 2/N, etc.)
- ✅ Logs para cada change dentro de entry
- ✅ Log del phone_number_id encontrado
- ✅ Logs de resolución de tenant (proceso y resultado)
- ✅ Log de verificación de duplicados
- ✅ Log cuando se guarda webhook log a DB
- ✅ Logs del número de mensajes y statuses procesados
- ✅ Log de actualización del status de procesado
- ✅ Log de fin: `=== WEBHOOK PAYLOAD PROCESSING COMPLETED ===`
- ✅ Logs de excepciones con tipo, mensaje y stack trace

#### processIncomingMessage()
- ✅ Agregó log: `=== PROCESSING INCOMING MESSAGE ===`
- ✅ Log del WAMID del mensaje
- ✅ Log si no hay ID (✗)
- ✅ Log de verificación de duplicados
- ✅ Logs de datos del mensaje: From, Type, Timestamp
- ✅ Logs de búsqueda/creación de conversación
- ✅ Logs de búsqueda de nombre del contacto
- ✅ Logs de guardado de conversación con ID
- ✅ Logs de extracción de texto o media
- ✅ Log final: `✓ SAVED incoming message` con detalles

#### processStatusUpdate()
- ✅ Agregó log: `=== PROCESSING STATUS UPDATE ===`
- ✅ Log del WAMID, status y timestamp
- ✅ Logs de verificación de duplicados
- ✅ Log de errores si existen
- ✅ Log final: `✓ SAVED status update` con detalles

#### resolveTenantByPhoneNumberId()
- ✅ Agregó log: `Resolving tenant by phoneNumberId`
- ✅ Log del número total de compañías a buscar
- ✅ Logs para cada compañía chequeada [N/total]
- ✅ Log si encontró WppConnection (con ID y status)
- ✅ Log si conexión está CONNECTED
- ✅ Log si hay excepciones (silenciosas)
- ✅ Log final si NO encontró tenant

---

## Niveles de Logging Utilizados

| Nivel | Uso | Ejemplos |
|-------|-----|----------|
| **INFO** | Información importante del flujo | Webhook recibido, mensaje guardado, tenant resuelto |
| **DEBUG** | Datos detallados para debugging | Primeros 500 chars del body, verificación paso a paso |
| **WARN** | Situaciones anómalas | Conexión no encontrada, signature inválida |
| **ERROR** | Errores que requieren atención | Excepciones durante procesamiento |

---

## Símbolos Usados en Logs

Para fácil identificación visual:

- `✓` = Éxito / Operación completada correctamente
- `✗` = Error / Operación fallida
- `ℹ` = Información / Noota informativa
- `⚠` = Advertencia / Situación sospechosa
- `===` = Marca de sección (inicio/fin de operación importante)
- `--` = Sub-sección de una operación mayor

**Ejemplo de estructura:**

```
=== WEBHOOK PROCESSING ===          ← Inicio de operación mayor
  Found 2 entries
  --- Entry 1/2 ---                ← Sub-sección
    Phone: 123456
    ✓ Success                       ← Resultado
  --- Entry 2/2 ---
    ✗ Error                         ← Error
=== COMPLETED ===                  ← Fin de operación
```

---

## Cómo Ver los Logs

### Opción 1: Terminal durante desarrollo
```bash
cd demo
mvn spring-boot:run
# Los logs aparecen directamente en la consola
```

### Opción 2: Archivo de log (si está configurado)
Busca en: `demo/logs/` o verifica `application.properties` para `logging.file.name`

### Opción 3: IDE (VS Code, IntelliJ)
Si ejecutas con debug, aparecen en la terminal integrada del IDE

### Filtrar logs específicos
En una terminal:
```bash
# Ver solo logs de WhatsApp
mvn spring-boot:run | grep "WhatsApp"

# Ver solo errores
mvn spring-boot:run | grep "ERROR"

# Ver con timestamps
mvn spring-boot:run | grep "WEBHOOK"
```

---

## Estadísticas de Cambios

| Componente | Líneas Originales | Líneas Nuevas | +Logs |
|------------|-------------------|---------------|-------|
| WhatsAppWebhookController | 155 | 265 | +110 líneas |
| WppWebhookService | 226 | 420 | +194 líneas |
| **TOTAL** | **381** | **685** | **+304 líneas** |

Aumento de cobertura de logs: **+80%**

---

## Testing Manual

Para verificar que los logs funcionan:

1. **Compila:**
   ```bash
   cd demo
   mvn clean compile -DskipTests
   ```

2. **Inicia la app:**
   ```bash
   mvn spring-boot:run
   ```

3. **Verifica Status:**
   ```bash
   curl http://localhost:8080/api/whatsapp/status
   ```
   Deberías ver en logs: `=== WhatsApp STATUS CHECK ===`

4. **Envía un mensaje desde WhatsApp real** al número configurado

5. **Revisa los logs** en la consola buscando las secciones:
   - `=== WhatsApp WEBHOOK POST RECEIVED ===`
   - `=== WEBHOOK SIGNATURE VALIDATION ===`
   - `=== WEBHOOK PAYLOAD PROCESSING STARTED ===`
   - `=== PROCESSING INCOMING MESSAGE ===`
   - `✓ SAVED incoming message`

---

## Notas Importantes

1. **No hay logs antes del webhook:** Si no ves nada cuando envías un mensaje:
   - El webhook URL no está bien configurado en Meta
   - Meta no está enviando el webhook a tu servidor
   - Firewall bloquea las conexiones desde Meta

2. **Logs muestran error de firma:** 
   - El `whatsapp.meta-app-secret` es incorrecto
   - Obtén el correcto de Meta App Dashboard

3. **Logs muestran tenant no encontrado:**
   - El `phone_number_id` que Meta envía no existe en tu BD
   - Verifica la tabla `wpp_connections`

4. **Logs muestran mensaje guardado pero no aparece en frontend:**
   - El problema está en Angular/Frontend
   - Verifica que haya actualización automática de conversaciones
   - Revisa WebSocket o polling del frontend

---

## Archivo de Referencia

Se creó también: `demo/WHATSAPP_LOGS_GUIDE.md`

Contiene: Guía completa de cómo interpretar cada log y diagrama de troubleshooting.
