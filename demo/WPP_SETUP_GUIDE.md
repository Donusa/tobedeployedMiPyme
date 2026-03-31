# WhatsApp Connection Setup Guide

## Endpoint: POST `/api/wpp/connection/setup`

Este endpoint permite cargar los datos de conexión a WhatsApp para un tenant específico, permitiendo testing gratuito sin seguir el flujo de Embedded Signup de Meta.

### Campos Requeridos

- **wabaId** (string): WhatsApp Business Account ID de Meta
- **phoneNumberId** (string): Phone Number ID asociado a la cuenta
- **accessToken** (string): Access Token de Meta (obtenido desde Meta Business Suite)

### Campos Opcionales

- **displayPhoneNumber** (string): Número de teléfono legible (ej: +5491234567890)

## Ejemplos de Uso

### 1. cURL

```bash
curl -X POST http://localhost:8080/api/wpp/connection/setup \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer YOUR_JWT_TOKEN" \
  -d '{
    "wabaId": "123456789",
    "phoneNumberId": "987654321",
    "displayPhoneNumber": "+5491234567890",
    "accessToken": "EAABsbCS1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"
  }'
```

### 2. PowerShell

```powershell
$headers = @{
  'Content-Type' = 'application/json'
  'Authorization' = 'Bearer YOUR_JWT_TOKEN'
}

$body = @{
  wabaId = '123456789'
  phoneNumberId = '987654321'
  displayPhoneNumber = '+5491234567890'
  accessToken = 'EAABsbCS1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ'
} | ConvertTo-Json

Invoke-WebRequest -Uri 'http://localhost:8080/api/wpp/connection/setup' `
  -Method POST `
  -Headers $headers `
  -Body $body
```

### 3. JavaScript/Fetch

```javascript
const setupWhatsApp = async (credentials) => {
  const response = await fetch('/api/wpp/connection/setup', {
    method: 'POST',
    headers: {
      'Content-Type': 'application/json',
      'Authorization': `Bearer ${authToken}`
    },
    body: JSON.stringify(credentials)
  });

  if (!response.ok) {
    const error = await response.json();
    throw new Error(error.error);
  }

  return response.json();
};

// Uso
const result = await setupWhatsApp({
  wabaId: '123456789',
  phoneNumberId: '987654321',
  displayPhoneNumber: '+5491234567890',
  accessToken: 'EAABsbCS1234567890...'
});

console.log('Conexión configurada:', result);
```

### 4. TypeScript/Angular

```typescript
import { HttpClient } from '@angular/common/http';
import { Injectable } from '@angular/core';

@Injectable({ providedIn: 'root' })
export class WhatsAppSetupService {
  constructor(private http: HttpClient) {}

  setupConnection(credentials: WhatsAppCredentials) {
    return this.http.post<WhatsAppSetupResponse>(
      '/api/wpp/connection/setup',
      credentials
    );
  }
}

// Tipos
interface WhatsAppCredentials {
  wabaId: string;
  phoneNumberId: string;
  accessToken: string;
  displayPhoneNumber?: string;
}

interface WhatsAppSetupResponse {
  success: boolean;
  message: string;
  connectionId: number;
  wabaId: string;
  phoneNumberId: string;
  displayPhoneNumber: string;
  status: 'CONNECTED' | 'DISCONNECTED';
}

// Uso en componente
@Component({...})
export class WhatsAppSetupComponent {
  constructor(private setupService: WhatsAppSetupService) {}

  setupWhatsApp() {
    this.setupService.setupConnection({
      wabaId: '123456789',
      phoneNumberId: '987654321',
      displayPhoneNumber: '+5491234567890',
      accessToken: 'token_aqui'
    }).subscribe(
      (response) => {
        console.log('Configurado exitosamente:', response);
      },
      (error) => {
        console.error('Error:', error);
      }
    );
  }
}
```

### 5. Postman

1. **Crear nueva request:**
   - Method: POST
   - URL: `http://localhost:8080/api/wpp/connection/setup`

2. **Headers:**
   - Content-Type: application/json
   - Authorization: Bearer YOUR_JWT_TOKEN

3. **Body (raw JSON):**
```json
{
  "wabaId": "123456789",
  "phoneNumberId": "987654321",
  "displayPhoneNumber": "+5491234567890",
  "accessToken": "EAABsbCS1234567890ABCDEFGHIJKLMNOPQRSTUVWXYZ"
}
```

## Respuesta Exitosa (200 OK)

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

## Respuestas de Error

### Campo Requerido Faltante (400 Bad Request)

```json
{
  "error": "Missing required field: wabaId"
}
```

### Falta de Contexto de Tenant (400 Bad Request)

```json
{
  "error": "Tenant context is required"
}
```

## Características de Seguridad

1. **Cifrado de Token**: El access token se cifra bajo AES-GCM-256 antes de guardarse en la base de datos
2. **Isolamiento por Tenant**: Cada tenant tiene sus propias credenciales cifradas separadamente
3. **AAD (Additional Authenticated Data)**: El cifrado incluye validación adicional con tenant ID y phone number ID
4. **Sin Exposición de Credenciales**: El token nunca se retorna en la respuesta

## Cómo Obtener las Credenciales

### 1. Crear una App en Meta

1. Ir a [Meta Developers](https://developers.facebook.com/)
2. Crear una nueva app o usar una existente
3. Agregar producto "WhatsApp" a la app

### 2. Configurar WhatsApp Business Account

1. Ir a "App Roles" → "Roles"
2. Buscar tu cuenta de Meta Business
3. Asignar rol de administrador

### 3. Obtener el Access Token

1. En Meta Business Suite, ir a "Settings" → "Business Apps"
2. Seleccionar tu app
3. En "Accounts" → "WhatsApp Business Accounts"
4. El Token está en la línea de la app

### 4. Obtener WABA ID y Phone Number ID

1. La WABA ID la ves en WhatsApp Business Accounts
2. En "Phone Numbers", encontrarás el Phone Number ID

## Endpoints Relacionados

- `GET /api/wpp/conversations` - Listar conversaciones
- `GET /api/wpp/conversations/{id}/messages` - Obtener mensajes de una conversación
- `POST /api/wpp/messages/send` - Enviar mensaje
- `POST /api/wpp/conversations/{id}/read` - Marcar como leído

## Notas Importantes

- El token debe ser válido y no expirado
- El tenant context se obtiene automáticamente del JWT/contexto de sesión
- Una vez configurada la conexión, el sistema está listo para enviar/recibir mensajes
- La conexión se marca automáticamente como CONNECTED
- El cifrado del token ocurre automáticamente sin intervención del usuario

## Troubleshooting

### "Tenant context is required"
- Asegúrate de estar autenticado como usuario de un tenant válido
- Verifica que tu JWT contenga la información del tenant

### "Missing required field"
- Revisa que todos los campos requeridos estén presentes y no vacíos
- Valida que el JSON sea válido

### Error al enviar mensajes después de la configuración
- Verifica que el access token sea válido en Meta
- Confirma que el phoneNumberId sea correcto
- Verifica los logs del servidor para más detalles
