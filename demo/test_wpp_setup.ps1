# Script para testear el endpoint de setup de WhatsApp
# Uso: .\test_wpp_setup.ps1 -WabaId "123456789" -PhoneNumberId "987654321" -AccessToken "token..."

param(
    [Parameter(Mandatory=$true)]
    [string]$WabaId,
    
    [Parameter(Mandatory=$true)]
    [string]$PhoneNumberId,
    
    [Parameter(Mandatory=$true)]
    [string]$AccessToken,
    
    [Parameter(Mandatory=$false)]
    [string]$DisplayPhoneNumber,
    
    [Parameter(Mandatory=$false)]
    [string]$ApiBaseUrl = "http://localhost:8080",
    
    [Parameter(Mandatory=$false)]
    [string]$AuthToken
)

$ErrorActionPreference = "Stop"

# Funciones para logging
function Write-Info {
    param([string]$Message)
    Write-Host "➜ $Message" -ForegroundColor Green
}

function Write-Warn {
    param([string]$Message)
    Write-Host "⚠ $Message" -ForegroundColor Yellow
}

function Write-Error {
    param([string]$Message)
    Write-Host "✗ $Message" -ForegroundColor Red
}

# Validar entrada
Write-Info "Configurando conexión WhatsApp..."
Write-Info "API: $ApiBaseUrl/api/wpp/connection/setup"
Write-Info "WABA ID: $WabaId"
Write-Info "Phone Number ID: $PhoneNumberId"
if ($DisplayPhoneNumber) {
    Write-Info "Display Phone: $DisplayPhoneNumber"
}
Write-Host ""

# Construir payload
$payload = @{
    wabaId = $WabaId
    phoneNumberId = $PhoneNumberId
    accessToken = $AccessToken
}

if ($DisplayPhoneNumber) {
    $payload["displayPhoneNumber"] = $DisplayPhoneNumber
}

$jsonPayload = $payload | ConvertTo-Json

# Construir headers
$headers = @{
    "Content-Type" = "application/json"
}

if ($AuthToken) {
    $headers["Authorization"] = "Bearer $AuthToken"
} else {
    Write-Warn "No se especificó AuthToken. La request podría fallar si se requiere autenticación."
}

# Ejecutar request
try {
    Write-Info "Enviando request..."
    Write-Host ""
    
    $response = Invoke-WebRequest `
        -Uri "$ApiBaseUrl/api/wpp/connection/setup" `
        -Method POST `
        -Headers $headers `
        -Body $jsonPayload
    
    Write-Info "Conexión configurada exitosamente! (HTTP $($response.StatusCode))"
    Write-Host ""
    
    # Parsear y mostrar respuesta
    $responseData = $response.Content | ConvertFrom-Json
    
    # Pretty print
    $responseData | Format-List
    
    Write-Host ""
    Write-Info "Connection ID: $($responseData.connectionId)"
    Write-Info "Status: $($responseData.status)"
    Write-Host ""
    
    Write-Info "Los datos de conexión están listos para usar."
    Write-Info "Ahora puedes enviar mensajes usando el endpoint /api/wpp/messages/send"
    
} catch {
    Write-Error "Error al configurar la conexión"
    
    if ($_.Exception.Response) {
        try {
            $errorContent = $_.Exception.Response.Content.ReadAsStringAsync().Result
            $errorData = $errorContent | ConvertFrom-Json
            Write-Host ""
            Write-Host "Error:" -ForegroundColor Red
            $errorData | Format-List
        } catch {
            Write-Host $_.Exception.Response.StatusCode
            Write-Host $errorContent
        }
    } else {
        Write-Host $_.Exception.Message
    }
    
    exit 1
}
