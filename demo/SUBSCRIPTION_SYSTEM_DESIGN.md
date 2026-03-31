# Sistema de Suscripciones MiPyme — Diseño Operativo Completo

> Versión: 2.0  
> Última actualización: 2026-03-18  
> Pasarela de pago: Mercado Pago Suscripciones (preapproval API)

---

## A. Reglas de Negocio Consolidadas

### A.1 Jerarquía de Planes

| Plan       | Tier | Incluye          | Frecuencias      |
|------------|------|------------------|-------------------|
| Base       | 0    | —                | Mensual, Anual    |
| Pro        | 1    | Base             | Mensual, Anual    |
| Enterprise | 2    | Base + Pro       | Mensual, Anual    |

**Plan keys válidos:** `base`, `pro`, `enterprise`, `base-annual`, `pro-annual`, `enterprise-annual`

### A.2 Reglas de Upgrade de Plan

1. **Acceso inmediato:** al solicitar un upgrade, el usuario obtiene acceso al nuevo tier en el acto.
2. **Cobro diferencial (proration):** se cobra la diferencia proporcional por los días restantes del ciclo actual.
   - Ciclo mensual actual → se prorratea dentro del mes en curso.
   - Ciclo anual actual → se prorratea dentro del año en curso.
3. **Fórmula:** `diferencial = (precioNuevoPlan - precioActualPlan) × díasRestantes / díasEnCiclo`
4. **Mecanismo:** se genera un checkout de Mercado Pago (preference) por el monto diferencial. Si el pago falla o no se completa:
   - El acceso al nuevo tier **se mantiene** (UX sobre todo).
   - La proration queda en estado `PENDING` y se waivea automáticamente a las 48h.
   - El costo se absorbe en la próxima renovación con el nuevo plan.
5. **Migración de suscripción MP:** se cancela la suscripción antigua y se crea una nueva con el nuevo plan. El `billing_day` se configura según `validUntil` para evitar doble cobro.
6. **Próxima renovación:** queda configurada con el precio del nuevo plan.

### A.3 Reglas de Downgrade de Plan

1. **Acceso diferido:** el usuario conserva el acceso al tier superior hasta el final del período ya abonado (`validUntil`).
2. **Sin reembolso:** no hay devoluciones automáticas por tiempo no consumido.
3. **Programación:** el cambio queda programado con `pendingPlanKey` y `scheduledChangeType = DOWNGRADE`.
4. **Aplicación:** el scheduler nocturno aplica el cambio cuando `now >= validUntil`.
5. **Cancelable:** el usuario puede cancelar el cambio programado antes de que se aplique.

### A.4 Cambios de Frecuencia (sin cambio de plan)

1. **Mensual → Anual:** se programa para el próximo vencimiento (`validUntil`).
2. **Anual → Mensual:** se programa para el próximo vencimiento (`validUntil`).
3. **Sin cambio de acceso:** como no cambian funcionalidades, el acceso no se modifica.
4. **Sin doble cobro:** la nueva frecuencia entra en vigor en la siguiente renovación efectiva.
5. **Clasificación:** `FREQUENCY_ONLY`.

### A.5 Cambios Combinados (plan + frecuencia)

El cambio de plan y frecuencia se tratan como dos operaciones lógicas separadas:

1. **Upgrade + cambio frecuencia (COMBINED_UPGRADE):**
   - El tier sube → acceso inmediato al nuevo tier.
   - La proration se calcula sobre el ciclo ACTUAL (no el nuevo). Ejemplo: si estás en mensual, se prorratea el diferencial mensual, no anual.
   - La frecuencia nueva se programa para el próximo vencimiento.
   - Durante el período residual, el `billingCycle` sigue siendo el actual.
   - Al renovar, comienza la nueva frecuencia con el nuevo tier.

2. **Downgrade + cambio frecuencia (COMBINED_DOWNGRADE):**
   - El tier baja → acceso actual se mantiene hasta `validUntil`.
   - Todo se programa para el vencimiento (tanto tier como frecuencia).
   - `scheduledChangeType = COMBINED_DOWNGRADE`.

### A.6 Prueba Gratis (Trial)

1. **Duración:** 28 días, únicamente para el plan Pro.
2. **Acceso:** durante el trial, el usuario accede a todas las funcionalidades de Pro.
3. **Vencimiento sin pago:** al finalizar → `BLOCKED_TRIAL_EXPIRED`.
4. **Vista bloqueada:** orientada a activar/pagar suscripción.
5. **Conversión:** si el usuario pago dentro del trial, se marca como `TRIAL_CONVERTED` → `ACTIVE`.
6. **Unicidad:** un tenant solo puede usar el trial una vez (`trialEnd != null` marca que ya se usó).

### A.7 Pago Pendiente o Fallido — Período de Gracia

1. **Activación:** cuando MP reporta un pago fallido o la suscripción se pausa por falta de pago.
2. **Duración:** 5 días a partir de `validUntil`.
3. **Acceso:** completo durante los 5 días (estado `PAST_DUE_GRACE`).
4. **Notificaciones:** recordatorio diario por email durante la gracia.
5. **Regularización:** si un pago se acredita → vuelve a `ACTIVE`.
6. **Expiración de gracia:** si no se regulariza → `BLOCKED_PAYMENT_FAILED`.
7. **Vista bloqueada:** solo pantalla de regularización/pago.

### A.8 Suscripción Vencida

1. **Operatividad:** nula — no puede acceder a funcionalidades.
2. **Vista:** pantalla bloqueada con opción de regularizar/pagar.
3. **Estado:** `BLOCKED_EXPIRED`.
4. **Reactivación:** acreditar un nuevo pago → reactivación a `ACTIVE`.

### A.9 Cancelación

1. **Solicitud:** el usuario cancela; `cancelAtPeriodEnd = true`.
2. **Periodo residual:** acceso completo hasta `validUntil` (estado `ACTIVE_CANCEL_AT_PERIOD_END`).
3. **Vencimiento:** → `BLOCKED_CANCELED`.
4. **Mensaje diferenciado:** la vista bloqueada distingue entre "suscripción cancelada / no renovada" vs otras razones.
5. **Reactivación:** si acredita un nuevo pago → `ACTIVE` con el plan pagado.
6. **Reversión:** antes de vencer, puede revertir la cancelación. Se necesita crear nueva suscripción en MP.

### A.10 Mercado Pago — Mapeo de Estados

#### Estados de Suscripción (preapproval)

| Estado MP          | Estado Interno               | Acceso        |
|--------------------|------------------------------|---------------|
| `authorized`       | `ACTIVE`                     | Completo      |
| `active`           | `ACTIVE`                     | Completo      |
| `pending`          | `PENDING_ACTIVATION`         | Según context |
| `paused`           | `PAST_DUE_GRACE` o `SUSPENDED` | Según gracia |
| `cancelled`        | `ACTIVE_CANCEL_AT_PERIOD_END` o `BLOCKED_CANCELED` | Según período |

#### Estados de Pago (payment)

| Estado MP          | Acción Interna                          |
|--------------------|-----------------------------------------|
| `approved`         | Extender `validUntil`, confirmar proration si aplica |
| `pending`          | No cambiar acceso; esperar resolución   |
| `in_process`       | No cambiar acceso; esperar resolución   |
| `rejected`         | Si no hay período válido → gracia/bloqueo |
| `refunded`         | Recalcular entitlement                  |
| `charged_back`     | `BLOCKED` inmediato                     |
| `cancelled`        | Ignorar (no impacta acceso)             |

#### Eventos de Webhook

| Tipo                          | Procesador                  |
|-------------------------------|---------------------------  |
| `payment`                     | `MpPaymentProcessor`        |
| `subscription_preapproval`    | `MpSubscriptionProcessor`   |
| `subscription_preapproval_plan` | `MpSubscriptionProcessor` |
| `claim`                       | `MpClaimProcessor`          |
| `chargeback`                  | `MpChargebackProcessor`     |

---

## B. Matriz de Transiciones

### B.1 Upgrades — Mismo Ciclo

| Caso | Acceso | Suscripción | Cobro Ahora | Próxima Renovación | Tipo |
|------|--------|-------------|-------------|---------------------|------|
| Base mensual → Pro mensual | Inmediato a Pro | Nueva MP subscription Pro mensual | Diferencial prorrateado mensual | Pro mensual | Inmediato |
| Pro mensual → Enterprise mensual | Inmediato a Enterprise | Nueva MP subscription Enterprise mensual | Diferencial prorrateado mensual | Enterprise mensual | Inmediato |
| Base anual → Pro anual | Inmediato a Pro | Nueva MP subscription Pro anual | Diferencial prorrateado anual | Pro anual | Inmediato |
| Base anual → Enterprise anual | Inmediato a Enterprise | Nueva MP subscription Enterprise anual | Diferencial prorrateado anual | Enterprise anual | Inmediato |
| Pro anual → Enterprise anual | Inmediato a Enterprise | Nueva MP subscription Enterprise anual | Diferencial prorrateado anual | Enterprise anual | Inmediato |

### B.2 Downgrades — Mismo Ciclo

| Caso | Acceso | Suscripción | Cobro Ahora | Próxima Renovación | Tipo |
|------|--------|-------------|-------------|---------------------|------|
| Pro mensual → Base mensual | Mantiene Pro hasta validUntil | Se programa | Nada | Base mensual | Programado |
| Enterprise mensual → Pro mensual | Mantiene Enterprise hasta validUntil | Se programa | Nada | Pro mensual | Programado |
| Enterprise mensual → Base mensual | Mantiene Enterprise hasta validUntil | Se programa | Nada | Base mensual | Programado |
| Pro anual → Base anual | Mantiene Pro hasta validUntil | Se programa | Nada | Base anual | Programado |
| Enterprise anual → Pro anual | Mantiene Enterprise hasta validUntil | Se programa | Nada | Pro anual | Programado |
| Enterprise anual → Base anual | Mantiene Enterprise hasta validUntil | Se programa | Nada | Base anual | Programado |

### B.3 Cambio de Frecuencia — Mismo Plan

| Caso | Acceso | Suscripción | Cobro Ahora | Próxima Renovación | Tipo |
|------|--------|-------------|-------------|---------------------|------|
| Base mensual → Base anual | Sin cambio | Se programa | Nada | Base anual | Programado |
| Pro mensual → Pro anual | Sin cambio | Se programa | Nada | Pro anual | Programado |
| Enterprise mensual → Enterprise anual | Sin cambio | Se programa | Nada | Enterprise anual | Programado |
| Base anual → Base mensual | Sin cambio | Se programa | Nada | Base mensual | Programado |
| Pro anual → Pro mensual | Sin cambio | Se programa | Nada | Pro mensual | Programado |
| Enterprise anual → Enterprise mensual | Sin cambio | Se programa | Nada | Enterprise mensual | Programado |

### B.4 Upgrade + Cambio de Frecuencia

| Caso | Acceso | Suscripción | Cobro Ahora | Próxima Renovación | Tipo |
|------|--------|-------------|-------------|---------------------|------|
| Base mensual → Pro anual | Inmediato a Pro | Tier inmediato; frecuencia al vencer | Diferencial prorrateado **mensual** (Pro mensual - Base mensual) × díasRestantes/díasEnMes | Pro anual | Combinado |
| Base mensual → Enterprise anual | Inmediato a Enterprise | Tier inmediato; frecuencia al vencer | Diferencial prorrateado **mensual** (Enterprise mensual - Base mensual) × díasRestantes/díasEnMes | Enterprise anual | Combinado |
| Pro mensual → Enterprise anual | Inmediato a Enterprise | Tier inmediato; frecuencia al vencer | Diferencial prorrateado **mensual** (Enterprise mensual - Pro mensual) × díasRestantes/díasEnMes | Enterprise anual | Combinado |
| Base anual → Pro mensual | Inmediato a Pro | Tier inmediato; frecuencia al vencer | Diferencial prorrateado **anual** (Pro anual - Base anual) × díasRestantes/díasEnAño | Pro mensual | Combinado |
| Base anual → Enterprise mensual | Inmediato a Enterprise | Tier inmediato; frecuencia al vencer | Diferencial prorrateado **anual** (Enterprise anual - Base anual) × díasRestantes/díasEnAño | Enterprise mensual | Combinado |

### B.5 Downgrade + Cambio de Frecuencia

| Caso | Acceso | Suscripción | Cobro Ahora | Próxima Renovación | Tipo |
|------|--------|-------------|-------------|---------------------|------|
| Enterprise anual → Pro mensual | Mantiene Enterprise hasta validUntil | Todo al vencer | Nada | Pro mensual | Programado |
| Enterprise mensual → Base anual | Mantiene Enterprise hasta validUntil | Todo al vencer | Nada | Base anual | Programado |
| Pro anual → Base mensual | Mantiene Pro hasta validUntil | Todo al vencer | Nada | Base mensual | Programado |
| Pro mensual → Base anual | Mantiene Pro hasta validUntil | Todo al vencer | Nada | Base anual | Programado |

### B.6 Eventos de Ciclo de Vida

| Caso | Trigger | Acceso | Estado Resultante |
|------|---------|--------|-------------------|
| Trial → Pago recibido | Webhook payment.approved | Inmediato según plan pagado | ACTIVE |
| Trial → Vencido sin pago | Scheduler (trialEnd) | Bloqueado | BLOCKED_TRIAL_EXPIRED |
| Pago pendiente → Gracia | Webhook payment.rejected / sub.paused | Mantiene acceso 5 días | PAST_DUE_GRACE |
| Gracia → Regularizado | Webhook payment.approved | Acceso restaurado | ACTIVE |
| Gracia → Vencida | Scheduler (graceUntil) | Bloqueado | BLOCKED_PAYMENT_FAILED |
| Cancelación solicitada | Usuario | Acceso hasta validUntil | ACTIVE_CANCEL_AT_PERIOD_END |
| Cancelación → Fin período | Scheduler (validUntil) | Bloqueado | BLOCKED_CANCELED |
| Bloqueado → Pago nuevo | Webhook payment.approved | Acceso restaurado | ACTIVE |
| Contracargo | Webhook chargeback | Bloqueado inmediato | BLOCKED |
| Reclamo abierto | Webhook claim | Acceso restringido | IN_REVIEW |

---

## C. Modelo de Datos

### C.1 Company (entidad principal — schema mipyme)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `planTier` | VARCHAR(30) | `base`, `pro`, `enterprise` — tier de acceso actual |
| `planStatus` | ENUM(16) | Estado de la máquina de estados (ver D) |
| `billingCycle` | VARCHAR(10) | `monthly`, `annual` — frecuencia de facturación actual |
| `validUntil` | DATETIME(6) | Fin del período actual / próxima fecha de cobro |
| `graceUntil` | DATETIME(6) | Fin del período de gracia (validUntil + 5 días) |
| `currentPeriodStart` | DATETIME(6) | Inicio del ciclo de facturación actual |
| `pendingPlanKey` | VARCHAR(30) | Plan key programado (ej: `enterprise-annual`) |
| `scheduledChangeType` | VARCHAR(30) | Tipo de cambio programado |
| `cancelAtPeriodEnd` | BOOLEAN | `true` si la cancelación está programada |
| `trialEnd` | DATETIME(6) | Fin del trial de 28 días (null si no aplica) |
| `accessBlockedReason` | VARCHAR(40) | Razón del bloqueo (cuando aplica) |
| `prorationAmount` | DECIMAL(19,2) | Monto diferencial pendiente (ARS) |
| `prorationStatus` | VARCHAR(20) | `NONE`, `PENDING`, `PAID`, `WAIVED`, `FAILED` |
| `prorationPaymentId` | VARCHAR(100) | ID de pago MP confirmando proration |
| `version` | BIGINT | Optimistic locking |

### C.2 MpSubscription

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `subscriptionId` | VARCHAR(255) | ID de suscripción MP (UNIQUE) |
| `status` | VARCHAR(255) | Estado MP: authorized, paused, cancelled, pending |
| `amount` | DECIMAL(19,4) | Monto en ARS |
| `planTier` | VARCHAR(30) | Plan key completo (ej: `pro-annual`) |
| `tenantId` | VARCHAR(100) | Schema del tenant |
| `nextPaymentDate` | DATETIME(6) | Próxima fecha de cobro según MP |
| `lastChargedDate` | DATETIME(6) | Última fecha de cobro exitoso |
| `billingDay` | INT | Día del mes de facturación |
| `rawJson` | TEXT | JSON completo de MP |

### C.3 MpPayment

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `paymentId` | VARCHAR(255) | ID de pago MP (UNIQUE) |
| `status` | VARCHAR(255) | approved, pending, rejected, refunded, charged_back |
| `statusDetail` | VARCHAR(255) | Detalle del estado |
| `transactionAmount` | DECIMAL(19,4) | Monto |
| `tenantId` | VARCHAR(100) | Schema del tenant |
| `externalReference` | VARCHAR(255) | Referencia (tenantSchema o tenantSchema:PRORATION) |
| `rawJson` | TEXT | JSON completo |

### C.4 SubscriptionChangeAudit (inmutable)

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `tenantId` | VARCHAR(100) | Tenant |
| `changedAt` | DATETIME(6) | Timestamp del cambio |
| `changeType` | VARCHAR(30) | UPGRADE, DOWNGRADE, FREQUENCY_ONLY, COMBINED_*, CANCEL, REACTIVATE, TRIAL_START, TRIAL_CONVERTED, BLOCK, UNBLOCK |
| `fromPlanKey` | VARCHAR(30) | Plan anterior |
| `toPlanKey` | VARCHAR(30) | Plan nuevo |
| `scheduledEffectiveAt` | DATETIME(6) | Cuándo se aplicará (cambios diferidos) |
| `effectiveAt` | DATETIME(6) | Cuándo se aplicó realmente |
| `prorationAmount` | DECIMAL(19,2) | Monto diferencial |
| `prorationStatus` | VARCHAR(20) | NONE, PENDING, PAID, WAIVED, FAILED |
| `prorationPaymentId` | VARCHAR(100) | ID pago MP |
| `prorationCheckoutUrl` | VARCHAR(512) | URL checkout MP |
| `exchangeRateUsed` | DECIMAL(19,6) | Tipo de cambio usado |
| `initiatedBy` | VARCHAR(20) | USER, WEBHOOK, SCHEDULER, SYSTEM |
| `previousStatus` | VARCHAR(40) | Estado anterior |
| `newStatus` | VARCHAR(40) | Estado nuevo |
| `notes` | TEXT | Notas de debug |

### C.5 MpWebhookEvent

| Campo | Tipo | Descripción |
|-------|------|-------------|
| `type` | VARCHAR(100) | payment, subscription_preapproval, claim, chargeback |
| `action` | VARCHAR(100) | *.created, *.updated |
| `dataId` | VARCHAR(100) | ID del recurso |
| `status` | ENUM | RECEIVED, PROCESSING, PROCESSED, FAILED |
| `attempts` | INT | Intentos (max 5) |
| `rawBody` | TEXT | Payload completo |

---

## D. Máquina de Estados

### D.1 Estados

```
┌─────────────────────────────┐
│     PENDING_ACTIVATION      │  ← Cuenta nueva, sin trial ni pago
└──────┬──────────────────────┘
       │ startTrial()
       ▼
┌─────────────────────────────┐
│         TRIALING            │  ← Trial Pro activo (28 días)
└──────┬──────────┬───────────┘
       │ pago OK  │ trial vence
       ▼          ▼
┌──────────┐ ┌────────────────────┐
│  ACTIVE  │ │ BLOCKED_TRIAL_EXP  │
└──┬──┬──┬─┘ └────────────────────┘
   │  │  │
   │  │  └── cancelar → ACTIVE_CANCEL_AT_PERIOD_END
   │  │                         │
   │  │                    vence período
   │  │                         ▼
   │  │                  BLOCKED_CANCELED
   │  │
   │  └── cambio programado → ACTIVE_SCHEDULED_CHANGE
   │                                │
   │                           vence período
   │                                ▼
   │                          (nuevo plan: ACTIVE)
   │
   └── pago falla → PAST_DUE_GRACE
                         │         │
                    pago OK    gracia vence
                         │         │
                         ▼         ▼
                      ACTIVE   BLOCKED_PAYMENT_FAILED
```

### D.2 Estados Completos

| Estado | Acceso | Descripción |
|--------|--------|-------------|
| `PENDING_ACTIVATION` | ❌ | Cuenta nueva, sin subscripción |
| `TRIALING` | ✅ Pro | Trial activo |
| `ACTIVE` | ✅ Según plan | Suscripción activa y pagada |
| `ACTIVE_SCHEDULED_CHANGE` | ✅ Según plan actual | Downgrade/frecuencia programados |
| `ACTIVE_CANCEL_AT_PERIOD_END` | ✅ Según plan actual | Cancelación programada |
| `PAST_DUE_GRACE` | ✅ Según plan actual | Grace period (5 días) |
| `SUSPENDED` | ⚠️ Según contexto | Suscripción pausada en MP |
| `BLOCKED_TRIAL_EXPIRED` | ❌ | Trial venció sin pago |
| `BLOCKED_EXPIRED` | ❌ | Período venció sin renovación |
| `BLOCKED_CANCELED` | ❌ | Cancelación + período vencido |
| `BLOCKED_PAYMENT_FAILED` | ❌ | Grace period agotado |
| `BLOCKED` | ❌ | Contracargo activo |
| `IN_REVIEW` | ⚠️ Lectura | Reclamo abierto |

### D.3 Transiciones Válidas

```
PENDING_ACTIVATION → TRIALING (startTrial)
PENDING_ACTIVATION → ACTIVE (primer pago confirmado)

TRIALING → ACTIVE (pago confirmado)
TRIALING → BLOCKED_TRIAL_EXPIRED (trialEnd pasado)

ACTIVE → ACTIVE_SCHEDULED_CHANGE (downgrade/frecuencia programado)
ACTIVE → ACTIVE_CANCEL_AT_PERIOD_END (cancelación)
ACTIVE → PAST_DUE_GRACE (pago fallido)
ACTIVE → BLOCKED_EXPIRED (validUntil pasado sin renovación)
ACTIVE → BLOCKED (contracargo)
ACTIVE → IN_REVIEW (reclamo)
ACTIVE → SUSPENDED (pausa MP)

ACTIVE_SCHEDULED_CHANGE → ACTIVE (cambio cancelado por usuario)
ACTIVE_SCHEDULED_CHANGE → ACTIVE (cambio aplicado → nuevo plan activo)
ACTIVE_SCHEDULED_CHANGE → BLOCKED_EXPIRED (validUntil sin renovación)
ACTIVE_SCHEDULED_CHANGE → PAST_DUE_GRACE (pago fallido)

ACTIVE_CANCEL_AT_PERIOD_END → BLOCKED_CANCELED (validUntil pasado)
ACTIVE_CANCEL_AT_PERIOD_END → ACTIVE (reversión + nuevo pago)

PAST_DUE_GRACE → ACTIVE (pago confirmado)
PAST_DUE_GRACE → BLOCKED_PAYMENT_FAILED (gracia agotada)

BLOCKED_* → ACTIVE (nuevo pago confirmado)
BLOCKED_* → PENDING_ACTIVATION (sin datos de pago)

SUSPENDED → ACTIVE (suscripción reactivada en MP)
SUSPENDED → PAST_DUE_GRACE (si sigue pausada y validUntil cercano)

IN_REVIEW → ACTIVE (reclamo resuelto a favor)
IN_REVIEW → BLOCKED (reclamo convertido a contracargo)
```

---

## E. Reglas de Cálculo Económico

### E.1 Proration — Upgrade Mismo Ciclo

```
díasEnCiclo = días totales del ciclo actual (mes actual o año de facturación)
díasRestantes = max(0, ceil(validUntil - now) en días)

diferencial = (precioNuevoPlan_ARS - precioActualPlan_ARS) × díasRestantes / díasEnCiclo
```

**Ciclo mensual:** `díasEnCiclo = días en el mes de validUntil`  
**Ciclo anual:** `díasEnCiclo = días entre currentPeriodStart y validUntil` (fallback: 365/366)

### E.2 Proration — Upgrade Combinado (plan + frecuencia)

**Regla clave:** la proration se calcula siempre sobre el ciclo ACTUAL, usando los precios del ciclo ACTUAL.

Ejemplo: Base mensual ($9 USD) → Pro anual ($190 USD)
- Se prorratea usando precios MENSUALES: Pro mensual ($19 USD) - Base mensual ($9 USD)
- `diferencial = ($19 - $9) × rate_ARS × díasRestantes / díasEnMes`
- La frecuencia anual entra en vigor en la próxima renovación.

Ejemplo: Base anual ($90 USD) → Pro mensual ($19 USD)
- Se prorratea usando precios ANUALES: Pro anual ($190 USD) - Base anual ($90 USD)
- `diferencial = ($190 - $90) × rate_ARS × díasRestantes / díasEnAño`
- La frecuencia mensual entra en vigor en la próxima renovación.

### E.3 Redondeo

- Todos los montos se redondean a 2 decimales con `RoundingMode.HALF_UP`.
- Montos menores a ARS 1.00 se suprimen (se devuelve 0 — no vale la pena cobrar).

### E.4 Prevención de Doble Cobro

1. Al migrar la suscripción MP, se configura `billing_day` = día del mes de `validUntil`.
2. La suscripción antigua se cancela antes de crear la nueva.
3. El primer cobro de la nueva suscripción ocurre en `validUntil`, no inmediatamente.

### E.5 Prevención de Acceso Regalado

1. El upgrade da acceso inmediato PERO cobra el diferencial.
2. Si el diferencial no se paga en 48h, se waivea (el negocio absorbe un costo menor por UX).
3. El acceso nunca excede `validUntil` sin un próximo cobro configurado.
4. El trial tiene fecha fija de 28 días, no se reinicia ni se extiende.

### E.6 Deuda/Proration Pendiente

Mercado Pago no soporta "cobro parcial" dentro de una suscripción existente. Solución:
1. Se crea un checkout preference (pago único) por el monto diferencial.
2. El `external_reference` lleva `tenantSchema:PRORATION` para identificación.
3. El webhook de pago confirma → `prorationStatus = PAID`.
4. Si no se paga en 48h → `prorationStatus = WAIVED` (scheduler).

---

## F. Estrategia de Implementación

### F.1 Arquitectura de Servicios

```
┌──────────────────────────────────────────────────────────────┐
│                    MpSubscriptionController                  │
│  (REST API: subscribe, cancel, schedule-change, status)     │
└──────────────────────────┬───────────────────────────────────┘
                           │
              ┌────────────▼────────────┐
              │ SubscriptionChangeService│  ← Orquestador principal
              │  - requestChange()      │
              │  - cancelAtPeriodEnd()  │
              │  - startTrial()        │
              │  - confirmProration()   │
              └────┬────────┬──────────┘
                   │        │
         ┌─────────▼──┐  ┌─▼──────────────────┐
         │ Proration   │  │ EntitlementService  │
         │ Calculator  │  │  - recalculate()   │
         └─────────────┘  └────────────────────┘
                                    ▲
                                    │
┌───────────────────────────────────┼──────────────────────────┐
│            Webhook Pipeline       │                          │
│  Controller → Processor → PaymentProcessor/SubscriptionProc  │
└──────────────────────────────────────────────────────────────┘
                                    ▲
                                    │
┌───────────────────────────────────┼──────────────────────────┐
│     SubscriptionLifecycleScheduler (3 AM ART cron)           │
│  - Apply scheduled changes                                    │
│  - Expire trials                                              │
│  - Block grace expired                                        │
│  - Waive old prorations                                       │
│  - Send grace period reminders                                │
└──────────────────────────────────────────────────────────────┘
```

### F.2 Flujo: UI → Persistencia (Upgrade)

```
1. UI: POST /api/mercadopago/subscription/schedule-change {newPlanKey: "pro"}
2. Controller: valida tenant, delega a SubscriptionChangeService.requestChange()
3. SubscriptionChangeService:
   a. Carga Company (con @Version para optimistic locking)
   b. Clasifica cambio: UPGRADE
   c. Calcula proration con ProrationCalculatorService
   d. Aplica acceso inmediato: planTier = "pro", planStatus = ACTIVE
   e. Crea checkout preference MP para el diferencial
   f. Cancela suscripción MP antigua
   g. Crea nueva suscripción MP con nuevo plan
   h. Graba audit en SubscriptionChangeAudit
   i. Retorna ChangeResult con init_point
4. UI: redirige al usuario al checkout de proration
5. Webhook: payment.approved → MpPaymentProcessor confirma proration
```

### F.3 Flujo: Webhooks

```
1. MP envía POST /api/mercadopago/webhooks
2. MercadoPagoWebhookController:
   a. Valida HMAC-SHA256
   b. Persiste MpWebhookEvent (RECEIVED)
   c. Dispara procesamiento async
3. MpWebhookProcessor:
   a. Marca PROCESSING
   b. Rutea a procesador específico
   c. Marca PROCESSED/FAILED
4. MpPaymentProcessor / MpSubscriptionProcessor:
   a. Fetch datos de MP API
   b. Persiste/actualiza registro local
   c. Llama EntitlementService.recalculate()
5. EntitlementService:
   a. Evalúa prioridades (chargeback > claims > subscription > payment)
   b. Actualiza planStatus de Company
```

### F.4 Validaciones Previas

- Solo se permiten cambios en estados operativos (ACTIVE, TRIALING, ACTIVE_SCHEDULED_CHANGE).
- No se permiten cambios durante BLOCKED, IN_REVIEW, PAST_DUE_GRACE o BLOCKED_PAYMENT_FAILED.
- No se permite cambiar plan si hay cancelación pendiente (revertir primero).
- No se permite solicitar el plan que ya se tiene activo.
- Plan key debe estar en la lista de 6 válidos.

### F.5 Idempotencia

- **Webhooks:** deduplicación por unique constraint en (type, action, dataId, ts).
- **Retry:** webhooks fallidos se reintentan cada 10 min, max 5 intentos.
- **Proration confirm:** si ya está PAID, se ignora silenciosamente.
- **Trial start:** si ya está TRIALING, retorna sin error.
- **Recalculate:** 100% idempotente — mismos datos → mismo resultado.

### F.6 Concurrencia

- **Optimistic locking:** `@Version` en Company previene race conditions.
- **Async webhook processing:** cada evento se procesa independientemente.
- **Scheduler:** operaciones por tenant son independientes; falla de uno no afecta a otros.

### F.7 Auditoría

Todo cambio queda registrado en `subscription_change_audit` con:
- Quién lo inició (USER/WEBHOOK/SCHEDULER/SYSTEM)
- Estado anterior y nuevo
- Plan anterior y nuevo
- Monto de proration y su estado
- Tipo de cambio ARS/USD usado
- Timestamp exacto

---

## G. Reglas de Acceso por Estado

### G.1 Acceso Total
**Estados:** `ACTIVE`, `ACTIVE_SCHEDULED_CHANGE`, `ACTIVE_CANCEL_AT_PERIOD_END`, `TRIALING`

- Acceso completo a todas las funcionalidades del plan actual.
- Pueden ver/modificar datos.
- Pueden emitir facturas, gestionar inventario, etc.
- En TRIALING: acceso Pro.
- En ACTIVE_SCHEDULED_CHANGE: acceso al plan ACTUAL (no al pendiente).
- En ACTIVE_CANCEL_AT_PERIOD_END: acceso al plan actual hasta validUntil.

### G.2 Acceso Durante Gracia
**Estado:** `PAST_DUE_GRACE`

- Acceso completo operativo (mismas funcionalidades).
- Banner visible indicando pago pendiente y fecha de gracia.
- Botón prominente para regularizar/pagar.
- Recordatorio diario por email.

### G.3 Acceso Bloqueado — Regularización
**Estados:** `BLOCKED_EXPIRED`, `BLOCKED_PAYMENT_FAILED`

- Sin acceso operativo.
- Pantalla de bloqueo: "Tu suscripción venció / el pago falló".
- Botón "Regularizar pago" → checkout MP.
- Puede ver datos básicos de la empresa (nombre, plan).
- No puede operar (ventas, facturación, stock, etc.).

### G.4 Acceso Bloqueado — Cancelación
**Estado:** `BLOCKED_CANCELED`

- Sin acceso operativo.
- Pantalla de bloqueo: "Tu suscripción fue cancelada / no se renovó".
- Botón "Reactivar suscripción" → checkout MP.
- Mensaje diferenciado de los otros bloqueos.

### G.5 Acceso Bloqueado — Trial Expirado
**Estado:** `BLOCKED_TRIAL_EXPIRED`

- Sin acceso operativo.
- Pantalla de bloqueo: "Tu período de prueba terminó".
- Opciones: activar plan Base/Pro/Enterprise.
- Destacar ventajas de continuar.

### G.6 Acceso Bloqueado — Contracargo
**Estado:** `BLOCKED`

- Sin acceso operativo.
- Pantalla: "Tu cuenta está suspendida por un contracargo".
- Contactar soporte.

### G.7 Acceso Restringido — En Revisión
**Estado:** `IN_REVIEW`

- Acceso de solo lectura.
- Puede ver datos pero no crear/modificar.
- Banner: "Tu cuenta está en revisión por un reclamo".

### G.8 Acceso con Cambio Programado
**Estado:** `ACTIVE_SCHEDULED_CHANGE`

- Acceso completo al plan ACTUAL.
- Banner informativo: "Tu plan cambiará a X el DD/MM/YYYY".
- Opción para cancelar el cambio programado.

### G.9 Cuenta Pendiente de Activación
**Estado:** `PENDING_ACTIVATION`

- Sin acceso operativo.
- Pantalla de activación: elegir plan o iniciar trial.

---

## H. Casos de Prueba

### H.1 Upgrade — Casos Básicos

| # | Caso | Precondición | Acción | Resultado Esperado |
|---|------|-------------|--------|---------------------|
| 1 | Upgrade Base→Pro mensual | ACTIVE, Base, mensual, validUntil=+15d | requestChange("pro") | planTier=pro, ACTIVE, diferencial cobrado |
| 2 | Upgrade Pro→Enterprise mensual | ACTIVE, Pro, mensual, validUntil=+20d | requestChange("enterprise") | planTier=enterprise, ACTIVE, diferencial cobrado |
| 3 | Upgrade faltando 1 día | ACTIVE, Base, mensual, validUntil=+1d | requestChange("pro") | diferencial ≈ 1/30 del delta, acceso inmediato |
| 4 | Upgrade faltando minutos | ACTIVE, Base, mensual, validUntil=+0.01d | requestChange("pro") | daysRemaining=0, diferencial=0, acceso inmediato |

### H.2 Downgrade

| # | Caso | Precondición | Acción | Resultado Esperado |
|---|------|-------------|--------|---------------------|
| 5 | Downgrade Pro→Base mensual | ACTIVE, Pro, mensual | requestChange("base") | ACTIVE_SCHEDULED_CHANGE, pendingPlan=base |
| 6 | Downgrade antes del vencimiento | ACTIVE_SCHEDULED_CHANGE, pendingPlan=base | scheduler runs, now<validUntil | No aplica aún |
| 7 | Downgrade al vencimiento | ACTIVE_SCHEDULED_CHANGE, pendingPlan=base | scheduler runs, now>=validUntil | planTier=base, ACTIVE |

### H.3 Cambio de Frecuencia

| # | Caso | Precondición | Acción | Resultado Esperado |
|---|------|-------------|--------|---------------------|
| 8 | Mensual→Anual mismo plan | ACTIVE, Pro, mensual | requestChange("pro-annual") | ACTIVE_SCHEDULED_CHANGE, pendingPlan=pro-annual |
| 9 | Anual→Mensual mismo plan | ACTIVE, Pro, anual | requestChange("pro") | ACTIVE_SCHEDULED_CHANGE, pendingPlan=pro |

### H.4 Cambio Combinado

| # | Caso | Precondición | Acción | Resultado Esperado |
|---|------|-------------|--------|---------------------|
| 10 | Base mensual→Pro anual | ACTIVE, Base, mensual | requestChange("pro-annual") | planTier=pro, billingCycle=monthly (no cambia aún), pendingPlanKey=pro-annual, proration basada en precios mensuales |
| 11 | Enterprise anual→Pro mensual | ACTIVE, Enterprise, anual | requestChange("pro") | ACTIVE_SCHEDULED_CHANGE, pendingPlan=pro, todo diferido |

### H.5 Trial

| # | Caso | Precondición | Acción | Resultado Esperado |
|---|------|-------------|--------|---------------------|
| 12 | Iniciar trial | PENDING_ACTIVATION | startTrial() | TRIALING, planTier=pro, trialEnd=now+28d |
| 13 | Trial doble intento | TRIALING | startTrial() | Idempotente, sin error |
| 14 | Trial ya usado | ACTIVE (trialEnd!=null) | startTrial() | Error: trial ya utilizado |
| 15 | Trial vencido sin pago | TRIALING, trialEnd pasado | scheduler | BLOCKED_TRIAL_EXPIRED |
| 16 | Pago durante trial | TRIALING | webhook payment.approved | ACTIVE, trial consumido |

### H.6 Gracia y Pagos Fallidos

| # | Caso | Precondición | Acción | Resultado Esperado |
|---|------|-------------|--------|---------------------|
| 17 | Pago falla, inicia gracia | ACTIVE | webhook sub.paused | PAST_DUE_GRACE, graceUntil=validUntil+5d |
| 18 | Pago durante gracia | PAST_DUE_GRACE | webhook payment.approved | ACTIVE |
| 19 | Gracia vence | PAST_DUE_GRACE, graceUntil pasado | scheduler | BLOCKED_PAYMENT_FAILED |
| 20 | Pago fallido durante gracia | PAST_DUE_GRACE | webhook payment.rejected | Mantiene PAST_DUE_GRACE |

### H.7 Cancelación y Reactivación

| # | Caso | Precondición | Acción | Resultado Esperado |
|---|------|-------------|--------|---------------------|
| 21 | Cancelar suscripción | ACTIVE | cancelAtPeriodEnd() | ACTIVE_CANCEL_AT_PERIOD_END |
| 22 | Vence tras cancelación | ACTIVE_CANCEL_AT_PERIOD_END, validUntil pasado | scheduler | BLOCKED_CANCELED |
| 23 | Reactivar post-bloqueo | BLOCKED_CANCELED | webhook payment.approved | ACTIVE |
| 24 | Revertir cancelación | ACTIVE_CANCEL_AT_PERIOD_END | revertCancel() | Limpia flag, user needs new sub |

### H.8 Webhooks — Casos Borde

| # | Caso | Precondición | Acción | Resultado Esperado |
|---|------|-------------|--------|---------------------|
| 25 | Webhook duplicado | Webhook ya procesado | Mismo webhook | Deduplicado por unique constraint, ignorado |
| 26 | Webhook fuera de orden | payment.approved antes de sub.created | Procesar ambos | EntitlementService recalcula en ambos → resultado consistente |
| 27 | Webhook con tenantId inválido | externalReference corrupto | processPayment() | tenantId="UNRESOLVED", no recalcula |

### H.9 Cambios Programados — Sobreescritura

| # | Caso | Precondición | Acción | Resultado Esperado |
|---|------|-------------|--------|---------------------|
| 28 | Cambio programado + nuevo downgrade | ACTIVE_SCHEDULED_CHANGE, pending=base | requestChange("pro") | El nuevo cambio reemplaza al anterior (upgrade inmediato) |
| 29 | Cambio programado + cancelar cambio | ACTIVE_SCHEDULED_CHANGE | cancelScheduledChange() | ACTIVE, pendingPlanKey=null |
| 30 | Usuario bloqueado que regulariza | BLOCKED_PAYMENT_FAILED | webhook payment.approved | ACTIVE, acceso restaurado |

---

## I. Criterios No Negociables

1. **Nunca dar acceso menor** al último período ya pago antes de su vencimiento.
2. **Nunca dar acceso mayor indefinidamente** sin contemplar cobro (máximo 48h de proration waiver).
3. **Todo cambio trazable:** cada transición deja un registro en `subscription_change_audit`.
4. **Reconstruible:** cualquier decisión puede reconstruirse desde la BD + audit log.
5. **Idempotencia:** webhooks duplicados, retries y recálculos producen el mismo resultado.
6. **Desacople MP ↔ negocio:** las reglas internas no dependen del formato/timing exacto de MP.
7. **Concurrencia segura:** `@Version` en Company, processing async de webhooks.
8. **Grace period estricto:** exactamente 5 días, no más.
9. **Trial único:** un tenant solo puede usar el trial una vez, verificado por `trialEnd != null`.
10. **Auditability:** quién (user/webhook/scheduler/system), cuándo, qué, desde qué estado, hacia qué estado.
