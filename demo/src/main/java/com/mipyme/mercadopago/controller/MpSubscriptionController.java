package com.mipyme.mercadopago.controller;

import com.mipyme.company.EntitlementService;
import com.mipyme.company.SubscriptionChangeService;
import com.mipyme.company.SubscriptionChangeService.ChangeResult;
import com.mipyme.mercadopago.model.MpPayment;
import com.mipyme.mercadopago.model.MpPlanConfig;
import com.mipyme.mercadopago.repository.MpPaymentRepository;
import com.mipyme.mercadopago.repository.MpPlanConfigRepository;
import com.mipyme.mercadopago.service.MpExchangeRateService;
import com.mipyme.mercadopago.service.MpExchangeRateService.PlanPricingInfo;
import com.mipyme.mercadopago.service.MpPlanProvisioningService;
import com.mipyme.mercadopago.service.MpPlanProvisioningService.PlanProvisionResult;
import com.mipyme.mercadopago.service.MpSubscriptionService;
import com.mipyme.mercadopago.service.MpSubscriptionService.SubscriptionStatusDto;
import com.mipyme.tenant.TenantContext;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/api/mercadopago")
public class MpSubscriptionController {

    private static final Logger logger = LoggerFactory.getLogger(MpSubscriptionController.class);

    private final MpSubscriptionService subscriptionService;
    private final MpPaymentRepository paymentRepository;
    private final EntitlementService entitlementService;
    private final MpExchangeRateService exchangeRateService;
    private final MpPlanProvisioningService provisioningService;
    private final MpPlanConfigRepository planConfigRepository;
    private final SubscriptionChangeService subscriptionChangeService;

    public MpSubscriptionController(MpSubscriptionService subscriptionService,
            MpPaymentRepository paymentRepository,
            EntitlementService entitlementService,
            MpExchangeRateService exchangeRateService,
            MpPlanProvisioningService provisioningService,
            MpPlanConfigRepository planConfigRepository,
            SubscriptionChangeService subscriptionChangeService) {
        this.subscriptionService = subscriptionService;
        this.paymentRepository = paymentRepository;
        this.entitlementService = entitlementService;
        this.exchangeRateService = exchangeRateService;
        this.provisioningService = provisioningService;
        this.planConfigRepository = planConfigRepository;
        this.subscriptionChangeService = subscriptionChangeService;
    }


    @PostMapping("/subscribe")
    public ResponseEntity<?> subscribe(@RequestBody SubscribeRequest request) {
        if (request.planType() == null || request.planType().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "planType is required"));
        }
        if (request.payerEmail() == null || request.payerEmail().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "payerEmail is required"));
        }

        try {
            String initPoint = subscriptionService.createSubscription(request.planType(), request.payerEmail());
            return ResponseEntity.ok(Map.of("initPoint", initPoint));
        } catch (IllegalStateException e) {
            logger.warn("Subscribe error: {}", e.getMessage());
            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error creating MP subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error creating subscription"));
        }
    }


    @GetMapping("/subscription/status")
    public ResponseEntity<SubscriptionStatusDto> getStatus() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        SubscriptionStatusDto dto = subscriptionService.getSubscriptionStatus(tenant);
        return ResponseEntity.ok(dto);
    }


    @PostMapping("/subscription/cancel")
    public ResponseEntity<?> cancel() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            subscriptionService.cancelSubscription(tenant);
            entitlementService.recalculate(tenant);
            return ResponseEntity.ok(Map.of("status", "cancelled"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error cancelling subscription"));
        }
    }


    @PostMapping("/subscription/revert-cancel")
    public ResponseEntity<?> revertCancel() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            subscriptionChangeService.revertCancelAtPeriodEnd(tenant);
            entitlementService.recalculate(tenant);
            return ResponseEntity.ok(Map.of("status", "reactivated"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error reverting cancel", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error reactivating subscription"));
        }
    }


    @PostMapping("/subscription/schedule-change")
    public ResponseEntity<?> scheduleChange(@RequestBody ScheduleChangeRequest request) {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        if (request.newPlanKey() == null || request.newPlanKey().isBlank()) {
            return ResponseEntity.badRequest().body(Map.of("error", "newPlanKey is required"));
        }
        try {
            ChangeResult result = subscriptionChangeService.requestChange(tenant, request.newPlanKey());
            return ResponseEntity.ok(result);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error requesting plan change", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error processing plan change"));
        }
    }


    @DeleteMapping("/subscription/schedule-change")
    public ResponseEntity<?> cancelScheduledChange() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            subscriptionService.cancelScheduledChange(tenant);
            return ResponseEntity.ok(Map.of("status", "cancelled"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error cancelling scheduled change", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error cancelling scheduled change"));
        }
    }


    @PostMapping("/subscription/pause")
    public ResponseEntity<?> pause() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        try {
            subscriptionService.pauseSubscription(tenant);
            entitlementService.recalculate(tenant);
            return ResponseEntity.ok(Map.of("status", "paused"));
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error pausing subscription", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error pausing subscription"));
        }
    }


    @GetMapping("/payments")
    public ResponseEntity<List<PaymentDto>> getPayments() {
        String tenant = TenantContext.getCurrentTenant();
        if (tenant == null) {
            return ResponseEntity.status(HttpStatus.UNAUTHORIZED).build();
        }
        List<MpPayment> payments = paymentRepository.findByTenantIdOrderByApprovedAtDesc(tenant);
        List<PaymentDto> dtos = payments.stream().map(p -> new PaymentDto(
                p.getPaymentId(),
                p.getStatus(),
                p.getStatusDetail(),
                p.getTransactionAmount() != null ? p.getTransactionAmount().toPlainString() : "0",
                p.getCurrencyId(),
                p.getApprovedAt() != null ? p.getApprovedAt().toString() : null,
                p.getPayerEmail())).toList();
        return ResponseEntity.ok(dtos);
    }


    @GetMapping("/pricing")
    public ResponseEntity<PlanPricingInfo> getPricing() {
        return ResponseEntity.ok(exchangeRateService.getPlanPricing());
    }


    @PostMapping("/admin/provision-plans")
    public ResponseEntity<?> provisionPlans() {
        try {
            List<PlanProvisionResult> results = provisioningService.provisionAllPlans();
            return ResponseEntity.ok(results);
        } catch (IllegalStateException e) {
            return ResponseEntity.status(HttpStatusCode.valueOf(422))
                    .body(Map.of("error", e.getMessage()));
        } catch (Exception e) {
            logger.error("Error provisioning plans", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(Map.of("error", "Error provisioning plans: " + e.getMessage()));
        }
    }


    @GetMapping("/admin/plans")
    public ResponseEntity<List<MpPlanConfig>> getPlans() {
        return ResponseEntity.ok(planConfigRepository.findAllByOrderByPlanKeyAsc());
    }

    public record SubscribeRequest(String planType, String payerEmail) {
    }

    public record ScheduleChangeRequest(String newPlanKey) {
    }

    public record PaymentDto(
            String paymentId,
            String status,
            String statusDetail,
            String amount,
            String currency,
            String approvedAt,
            String payerEmail) {
    }
}
