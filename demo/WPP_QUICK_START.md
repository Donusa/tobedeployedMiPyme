# WhatsApp Setup - Quick Start Guide

## ¿Qué Es Este Endpoint?

Un nuevo endpoint que te permite cargar rápidamente tus credenciales de WhatsApp Business para testing sin necesidad de implementar el flujo de Meta Embedded Signup.

## El Endpoint

```
POST /api/wpp/connection/setup
```

## Pasos Rápidos

### Paso 1: Obtén Tus Credenciales de Meta

1. Ve a [Meta Business Suite](https://business.facebook.com/)
2. Busca tu cuenta de WhatsApp Business
3. Anota:
   - **WABA ID**: Encontrarás en "WhatsApp Business Accounts"
   - **Phone Number ID**: En la sección "Phone Numbers"
   - **Access Token**: En "Business Apps" → tu app → Integrations

### Paso 2: Configura la Conexión

Elige tu método preferido:

#### Opción A: cURL (Más Simple)

```bash
curl -X POST http://localhost:8080/api/wpp/connection/setup \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "wabaId": "123456789",
    "phoneNumberId": "987654321",
    "accessToken": "EAAV1234567890...",
    "displayPhoneNumber": "+5491234567890"
  }'
```

#### Opción B: PowerShell (Windows)

```powershell
.\test_wpp_setup.ps1 `
  -WabaId "123456789" `
  -PhoneNumberId "987654321" `
  -AccessToken "EAAV1234567890..." `
  -DisplayPhoneNumber "+5491234567890"
```

#### Opción C: Postman

1. Crear nuevo request POST
2. URL: `http://localhost:8080/api/wpp/connection/setup`
3. Headers: `Content-Type: application/json`
4. Body (JSON):
```json
{
  "wabaId": "123456789",
  "phoneNumberId": "987654321",
  "accessToken": "EAAV1234567890...",
  "displayPhoneNumber": "+5491234567890"
}
```

#### Opción D: Angular TypeScript

```typescript
// en tu componente
constructor(private http: HttpClient) {}

setupWhatsApp() {
  const credentials = {
    wabaId: "123456789",
    phoneNumberId: "987654321", 
    accessToken: "EAAV1234567890...",
    displayPhoneNumber: "+5491234567890"
  };

  this.http.post('/api/wpp/connection/setup', credentials)
    .subscribe(
      (response) => console.log('✓ Configurado!', response),
      (error) => console.error('✗ Error:', error)
    );
}
```

### Paso 3: Verifica la Respuesta

Si todo está bien, recibirás:

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

### Paso 4: ¡Listo!

Ahora puedes:
- Enviar mensajes con `POST /api/wpp/messages/send`
- Listar conversaciones con `GET /api/wpp/conversations`
- Recibir mensajes automáticamente (webhooks)

## Validación de Credenciales

| Campo | Requerido | Ejemplo | Notas |
|-------|-----------|---------|-------|
| `wabaId` | ✅ | `123456789` | Tu WhatsApp Business Account ID |
| `phoneNumberId` | ✅ | `987654321` | ID del número de teléfono registrado |
| `accessToken` | ✅ | `EAAV1234567890...` | Token de acceso de Meta (válido y no expirado) |
| `displayPhoneNumber` | ❌ | `+5491234567890` | Opcional, para referencia legible |

## Troubleshooting

### ❌ "Missing required field: wabaId"
**Solución:** Asegúrate de incluir wabaId en el body

### ❌ "Tenant context is required"
**Solución:** Debes estar autenticado como un usuario de un tenant válido (verifica tu JWT)

### ❌ HTTP 401 Unauthorized
**Solución:** Falta el Authorization header 
```bash
-H "Authorization: Bearer YOUR_JWT_TOKEN"
```

### ❌ Access Token Inválido (al intentar enviar mensajes después)
**Solución:** 
- Verifica que el token no haya expirado en Meta
- Regenera el token en Meta Business Suite
- Vuelve a correr el endpoint setup

## Características de Seguridad

✅ El token se cifra automáticamente (AES-256-GCM)  
✅ Nunca se retorna en las respuestas  
✅ Cada tenant tiene sus propias credenciales  
✅ Validación adicional mediante AAD (Associated Authenticated Data)  

## ¿Puedo Cambiar la Conexión Después?

Sí, ejecuta el endpoint nuevamente con nuevas credenciales y se actualizará automáticamente.

## APIs Relacionados

Después de configurar, puedes usar:

```
# Listar conversaciones
GET /api/wpp/conversations

# Enviar un mensaje
POST /api/wpp/messages/send
Body: { "to": "549XXXXXXXXX", "text": "Hola!" }

# Obtener mensajes de una conversación
GET /api/wpp/conversations/{id}/messages?page=0&size=50

# Marcar conversación como leída
POST /api/wpp/conversations/{id}/read
```

## ¿Dónde Encuentro Más Info?

- **Documentación Completa:** Ver `WPP_SETUP_GUIDE.md`
- **Registro de Cambios:** Ver `CHANGELOG_WPP_SETUP.md`
- **Migration SQL:** Ver `db_migration_wpp_tenant.sql`

## Preguntas Frecuentes

**¿Necesito Embedded Signup después de esto?**  
No, puedes testear sin él. El flujo de Embedded Signup sigue disponible para producción.

**¿Se guarda el token en texto plano?**  
No, se cifra automáticamente antes de guardarlo en la base de datos.

**¿Puedo tener múltiples conexiones por tenant?**  
Sí, pero solo la con status=CONNECTED se usa por defecto. Puedes extender el código para soportar múltiples.

**¿El token expira?**  
Los tokens de Meta expiran según su configuración. Si expira, solo regenera en Meta Business Suite y ejecuta el endpoint de nuevo.

---

**¿Necesitas más ayuda?** Ver la documentación completa en `WPP_SETUP_GUIDE.md`
