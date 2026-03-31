# Nuevo Endpoint: Configuración de Conexión WhatsApp

## Resumen

Se ha agregado un nuevo endpoint REST que permite a los tenants cargar manualmente sus datos de conexión a WhatsApp (WABA ID, Phone Number ID y Access Token) para testing sin necesidad de seguir el flujo de Embedded Signup de Meta.

## Cambios Realizados

### 1. Modelo: `WppConnection.java`

**Cambio:** Se agregó soporte multi-tenant

```java
@Column(name = "tenant_id", nullable = false)
private String tenantId;
```

- Cada conexión está asociada a un tenant específico
- Los getters y setters para `tenantId` fueron agregados

**Ubicación:** `src/main/java/com/mipyme/whatsapp/model/WppConnection.java`

### 2. Servicio: `WppApiService.java`

**Cambio:** Se agregó el método `setupConnection()`

```java
@Transactional
public WppConnection setupConnection(String wabaId, String phoneNumberId, 
                                     String displayPhoneNumber, String accessToken)
```

**Características:**
- Valida que todos los campos requeridos estén presentes
- Obtiene el tenant context automáticamente
- Crea una nueva conexión con estado CONNECTED
- Cifra el access token usando AES-GCM-256 antes de guardarlo
- Retorna la conexión creada con todos los detalles

**Ubicación:** `src/main/java/com/mipyme/whatsapp/service/WppApiService.java`

### 3. Controlador: `WppApiController.java`

**Cambio:** Se agregó el endpoint POST `/api/wpp/connection/setup`

```java
@PostMapping("/connection/setup")
public ResponseEntity<Map<String, Object>> setupConnection(@RequestBody Map<String, String> payload)
```

**Responsabilidades:**
- Valida presencia de campos requeridos (wabaId, phoneNumberId, accessToken)
- Retorna respuesta informativa en caso de éxito
- Maneja excepciones y retorna mensajes de error claros

**Respuesta exitosa (200 OK):**
```json
{
  "success": true,
  "message": "WhatsApp connection configured successfully",
  "connectionId": 1,
  "wabaId": "123456789",
  "phoneNumberId": "987654321",
  "displayPhoneNumber": "+5491234567890",
  "status": "CONNECTED"
}
```

**Ubicación:** `src/main/java/com/mipyme/whatsapp/controller/WppApiController.java`

## Documentación

Se han creado tres archivos de documentación y testing:

### 1. `WPP_SETUP_GUIDE.md`
Guía completa con:
- Descripción del endpoint
- Ejemplos en múltiples lenguajes (cURL, PowerShell, JavaScript, TypeScript/Angular, Postman)
- Guía para obtener credenciales de Meta
- Troubleshooting
- Explicación de características de seguridad

### 2. `test_wpp_setup.sh` (Linux/Mac)
Script bash para testear el endpoint con:
- Validación de argumentos
- Soporte para variables de entorno
- Pretty printing de respuesta JSON
- Manejo de errores

**Uso:**
```bash
./test_wpp_setup.sh <wabaId> <phoneNumberId> <accessToken> [displayPhoneNumber]

# Ejemplo
./test_wpp_setup.sh 123456789 987654321 'EAABsbCS1234567890...' '+5491234567890'
```

### 3. `test_wpp_setup.ps1` (Windows)
Script PowerShell para testear el endpoint con:
- Parámetros nombrados
- Validación de entrada
- Pretty printing de respuesta
- Manejo de errores

**Uso:**
```powershell
.\test_wpp_setup.ps1 -WabaId "123456789" -PhoneNumberId "987654321" -AccessToken "EAABsbCS1234567890..."

# Con todos los parámetros
.\test_wpp_setup.ps1 `
  -WabaId "123456789" `
  -PhoneNumberId "987654321" `
  -AccessToken "EAABsbCS1234567890..." `
  -DisplayPhoneNumber "+5491234567890" `
  -ApiBaseUrl "http://localhost:8080" `
  -AuthToken "your-jwt-token"
```

## Flujo de Trabajo

1. **Obtener credenciales de Meta:**
   - WABA ID (WhatsApp Business Account ID)
   - Phone Number ID
   - Access Token válido

2. **Llamar el endpoint:**
   ```bash
   POST /api/wpp/connection/setup
   ```

3. **Respuesta exitosa:**
   - Connection ID retornado
   - Status = CONNECTED
   - Listo para usar

4. **Usar los endpoints existentes:**
   - `GET /api/wpp/conversations` - Listar conversaciones
   - `POST /api/wpp/messages/send` - Enviar mensajes
   - `GET /api/wpp/conversations/{id}/messages` - Obtener mensajes

## Seguridad

✅ **Access Token Cifrado:**
- Se cifra automáticamente con AES-GCM-256
- Nunca se retorna en la respuesta
- Incluye validación AAD con tenant ID y phone number ID

✅ **Isolamiento por Tenant:**
- Cada conexión está asociada a su tenant
- Los tokens se cifran con datos del tenant en el AAD

✅ **Sin Exposición de Credenciales:**
- El response JSON NO incluye el token
- Solo se muestran los IDs y el estado

## Próximos Pasos

El sistema está listo para:
1. Recibir mensajes entrantes (mediante webhooks)
2. Enviar mensajes de salida
3. Procesar estados de entrega
4. Gestionar conversaciones

## Archivos Modificados

- `src/main/java/com/mipyme/whatsapp/model/WppConnection.java` - ✅ Actualizado
- `src/main/java/com/mipyme/whatsapp/service/WppApiService.java` - ✅ Actualizado
- `src/main/java/com/mipyme/whatsapp/controller/WppApiController.java` - ✅ Actualizado
- `WPP_SETUP_GUIDE.md` - ✅ Nuevo
- `test_wpp_setup.sh` - ✅ Nuevo
- `test_wpp_setup.ps1` - ✅ Nuevo

## Notas

- No se requiere cambio en la base de datos (la migración de `tenant_id` debe ejecutarse)
- El endpoint está protected por autenticación (si aplica según tu configuración)
- El tenant context se obtiene automáticamente del contexto de Spring Security
- Compatible con el flujo existente de Embedded Signup (coexisten ambas formas)
