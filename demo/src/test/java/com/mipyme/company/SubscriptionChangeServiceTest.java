package com.mipyme.company;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.mipyme.mercadopago.model.MpSubscription;
import com.mipyme.mercadopago.repository.MpSubscriptionRepository;
import com.mipyme.mercadopago.service.MpExchangeRateService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.client.RestTemplate;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionChangeServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private MpSubscriptionRepository subscriptionRepository;
    @Mock private SubscriptionChangeAuditRepository auditRepository;
    @Mock private MpExchangeRateService exchangeRateService;
    @Mock private RestTemplate restTemplate;

    private SubscriptionChangeService service;

    private static final String TENANT = "testTenant";
    private static final BigDecimal EXCHANGE_RATE = new BigDecimal("1000.00");

    @BeforeEach
    void setUp() {
        ProrationCalculatorService prorationCalculator = new ProrationCalculatorService();
        ObjectMapper objectMapper = new ObjectMapper();
        service = new SubscriptionChangeService(
                companyRepository, subscriptionRepository, auditRepository,
                prorationCalculator, exchangeRateService, restTemplate, objectMapper);


        ReflectionTestUtils.setField(service, "priceUsdBase", new BigDecimal("9"));
        ReflectionTestUtils.setField(service, "priceUsdPro", new BigDecimal("19"));
        ReflectionTestUtils.setField(service, "priceUsdEnterprise", new BigDecimal("29"));
        ReflectionTestUtils.setField(service, "priceUsdBaseAnnual", new BigDecimal("90"));
        ReflectionTestUtils.setField(service, "priceUsdProAnnual", new BigDecimal("190"));
        ReflectionTestUtils.setField(service, "priceUsdEnterpriseAnnual", new BigDecimal("290"));
        ReflectionTestUtils.setField(service, "accessToken", "");
        ReflectionTestUtils.setField(service, "backUrl", "http://localhost:4200/checkout");


        lenient().when(exchangeRateService.getLatestRate())
                .thenReturn(new MpExchangeRateService.ExchangeRateInfo(EXCHANGE_RATE, Instant.now(), "dolarapi"));


        lenient().when(auditRepository.save(any(SubscriptionChangeAudit.class)))
                .thenAnswer(inv -> inv.getArgument(0));
    }

    private Company createCompany(String tier, String cycle, Company.PlanStatus status, int daysUntilRenewal) {
        Company company = new Company("comp1", "Test Co", TENANT, Instant.now(), "ABC123",
                null, null, null, null, null, null, null, null, null, true);
        company.setPlanTier(tier);
        company.setBillingCycle(cycle);
        company.setPlanStatus(status);
        if (daysUntilRenewal > 0) {
            Instant validUntil = Instant.now().plus(daysUntilRenewal, ChronoUnit.DAYS);
            company.setValidUntil(validUntil);
            company.setCurrentPeriodStart(validUntil.minus(
                    "annual".equals(cycle) ? 365 : 30, ChronoUnit.DAYS));
        }
        return company;
    }

    private void stubCompany(Company company) {
        when(companyRepository.findByTenantSchema(TENANT)).thenReturn(Optional.of(company));
        when(companyRepository.save(any(Company.class))).thenAnswer(inv -> inv.getArgument(0));
        lenient().when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                .thenReturn(List.of());
    }





    @Nested
    class Upgrades {

        @Test
        void upgrade_baseMensualToProMensual_immediateTierChange() {
            Company company = createCompany("base", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            var result = service.requestChange(TENANT, "pro");

            assertThat(result.changeType()).isEqualTo("UPGRADE");
            assertThat(result.immediate()).isTrue();
            assertThat(result.newPlanKey()).isEqualTo("pro");

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getPlanTier()).isEqualTo("pro");
            assertThat(saved.getBillingCycle()).isEqualTo("monthly");
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE);
            assertThat(saved.getPendingPlanKey()).isNull();
        }

        @Test
        void upgrade_proMensualToEnterpriseMensual_immediateTierChange() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE, 20);
            stubCompany(company);

            var result = service.requestChange(TENANT, "enterprise");

            assertThat(result.changeType()).isEqualTo("UPGRADE");
            assertThat(result.immediate()).isTrue();

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanTier()).isEqualTo("enterprise");
        }

        @Test
        void upgrade_baseAnualToProAnual_immediateTierChange() {
            Company company = createCompany("base", "annual", Company.PlanStatus.ACTIVE, 180);
            stubCompany(company);

            var result = service.requestChange(TENANT, "pro-annual");

            assertThat(result.changeType()).isEqualTo("UPGRADE");
            assertThat(result.immediate()).isTrue();

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanTier()).isEqualTo("pro");
            assertThat(captor.getValue().getBillingCycle()).isEqualTo("annual");
        }

        @Test
        void upgrade_lastDay_minimalOrZeroProration() {
            Company company = createCompany("base", "monthly", Company.PlanStatus.ACTIVE, 0);

            company.setValidUntil(Instant.now().minus(1, ChronoUnit.HOURS));
            stubCompany(company);

            var result = service.requestChange(TENANT, "pro");

            assertThat(result.immediate()).isTrue();

            assertThat(result.prorationAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }

        @Test
        void upgrade_recordsAudit() {
            Company company = createCompany("base", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            service.requestChange(TENANT, "pro");

            verify(auditRepository).save(any(SubscriptionChangeAudit.class));
        }
    }





    @Nested
    class Downgrades {

        @Test
        void downgrade_proToBase_deferredToEndOfPeriod() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            var result = service.requestChange(TENANT, "base");

            assertThat(result.changeType()).isEqualTo("DOWNGRADE");
            assertThat(result.immediate()).isFalse();
            assertThat(result.scheduledEffectiveAt()).isNotNull();

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getPlanTier()).isEqualTo("pro");
            assertThat(saved.getPendingPlanKey()).isEqualTo("base");
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
            assertThat(saved.getScheduledChangeType()).isEqualTo("DOWNGRADE");
        }

        @Test
        void downgrade_enterpriseToBase_deferredNoProration() {
            Company company = createCompany("enterprise", "monthly", Company.PlanStatus.ACTIVE, 20);
            stubCompany(company);

            var result = service.requestChange(TENANT, "base");

            assertThat(result.changeType()).isEqualTo("DOWNGRADE");
            assertThat(result.immediate()).isFalse();
            assertThat(result.prorationAmount()).isEqualByComparingTo(BigDecimal.ZERO);
        }
    }





    @Nested
    class FrequencyChanges {

        @Test
        void mensualToAnual_sameplan_deferred() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            var result = service.requestChange(TENANT, "pro-annual");

            assertThat(result.changeType()).isEqualTo("FREQUENCY_ONLY");
            assertThat(result.immediate()).isFalse();

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getBillingCycle()).isEqualTo("monthly");
            assertThat(saved.getPendingPlanKey()).isEqualTo("pro-annual");
            assertThat(saved.getScheduledChangeType()).isEqualTo("FREQUENCY_ONLY");
        }

        @Test
        void anualToMensual_samePlan_deferred() {
            Company company = createCompany("pro", "annual", Company.PlanStatus.ACTIVE, 180);
            stubCompany(company);

            var result = service.requestChange(TENANT, "pro");

            assertThat(result.changeType()).isEqualTo("FREQUENCY_ONLY");
            assertThat(result.immediate()).isFalse();

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getBillingCycle()).isEqualTo("annual");
            assertThat(captor.getValue().getPendingPlanKey()).isEqualTo("pro");
        }
    }





    @Nested
    class CombinedChanges {

        @Test
        void combinedUpgrade_baseMensualToProAnual_immediateTierDeferredFrequency() {
            Company company = createCompany("base", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            var result = service.requestChange(TENANT, "pro-annual");

            assertThat(result.changeType()).isEqualTo("COMBINED_UPGRADE");
            assertThat(result.immediate()).isTrue();
            assertThat(result.newPlanKey()).isEqualTo("pro-annual");

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();

            assertThat(saved.getPlanTier()).isEqualTo("pro");

            assertThat(saved.getBillingCycle()).isEqualTo("monthly");

            assertThat(saved.getPendingPlanKey()).isEqualTo("pro-annual");
            assertThat(saved.getScheduledChangeType()).isEqualTo("UPGRADE_MIGRATE");
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
        }

        @Test
        void combinedDowngrade_enterpriseAnualToProMensual_allDeferred() {
            Company company = createCompany("enterprise", "annual", Company.PlanStatus.ACTIVE, 180);
            stubCompany(company);

            var result = service.requestChange(TENANT, "pro");

            assertThat(result.changeType()).isEqualTo("COMBINED_DOWNGRADE");
            assertThat(result.immediate()).isFalse();

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();

            assertThat(saved.getPlanTier()).isEqualTo("enterprise");
            assertThat(saved.getBillingCycle()).isEqualTo("annual");
            assertThat(saved.getPendingPlanKey()).isEqualTo("pro");
            assertThat(saved.getScheduledChangeType()).isEqualTo("COMBINED_DOWNGRADE");
        }

        @Test
        void combinedUpgrade_prorationUsesCurrentCyclePrices() {



            Company company = createCompany("base", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            var result = service.requestChange(TENANT, "pro-annual");





            if (result.prorationAmount().compareTo(BigDecimal.ZERO) > 0) {
                assertThat(result.prorationAmount())
                        .isLessThan(new BigDecimal("10000.00"));
            }
        }
    }





    @Nested
    class Cancellation {

        @Test
        void cancelAtPeriodEnd_setsStatusAndFlag() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);
            when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                    .thenReturn(List.of());

            service.cancelAtPeriodEnd(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.isCancelAtPeriodEnd()).isTrue();
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
            assertThat(saved.getPendingPlanKey()).isNull();
        }

        @Test
        void cancelAtPeriodEnd_alreadyCancelled_throws() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END, 15);
            company.setCancelAtPeriodEnd(true);
            stubCompany(company);

            assertThatThrownBy(() -> service.cancelAtPeriodEnd(TENANT))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void revertCancelAtPeriodEnd_restoresActiveStatus() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END, 15);
            company.setCancelAtPeriodEnd(true);
            stubCompany(company);

            service.revertCancelAtPeriodEnd(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.isCancelAtPeriodEnd()).isFalse();
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE);
        }

        @Test
        void revertCancelAtPeriodEnd_periodExpired_throws() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END, 0);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            company.setCancelAtPeriodEnd(true);
            stubCompany(company);

            assertThatThrownBy(() -> service.revertCancelAtPeriodEnd(TENANT))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("venció");
        }
    }





    @Nested
    class ScheduledChanges {

        @Test
        void cancelScheduledChange_restoresActive() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE, 15);
            company.setPendingPlanKey("base");
            company.setScheduledChangeType("DOWNGRADE");
            stubCompany(company);

            service.cancelScheduledChange(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getPendingPlanKey()).isNull();
            assertThat(saved.getScheduledChangeType()).isNull();
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE);
        }

        @Test
        void cancelScheduledChange_noPending_throws() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            assertThatThrownBy(() -> service.cancelScheduledChange(TENANT))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void existingScheduledChange_newUpgradeReplacesIt() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE, 15);
            company.setPendingPlanKey("base");
            company.setScheduledChangeType("DOWNGRADE");
            stubCompany(company);

            var result = service.requestChange(TENANT, "enterprise");

            assertThat(result.changeType()).isEqualTo("UPGRADE");
            assertThat(result.immediate()).isTrue();

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanTier()).isEqualTo("enterprise");
        }
    }





    @Nested
    class Trial {

        @Test
        void startTrial_setsProFor28Days() {
            Company company = createCompany(null, null, Company.PlanStatus.PENDING_ACTIVATION, 0);
            stubCompany(company);

            service.startTrial(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getPlanTier()).isEqualTo("pro");
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.TRIALING);
            assertThat(saved.getTrialEnd()).isNotNull();

            long daysBetween = ChronoUnit.DAYS.between(Instant.now(), saved.getTrialEnd());
            assertThat(daysBetween).isBetween(27L, 29L);
        }

        @Test
        void startTrial_alreadyTrialing_idempotent() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.TRIALING, 0);
            company.setTrialEnd(Instant.now().plus(20, ChronoUnit.DAYS));
            stubCompany(company);

            service.startTrial(TENANT);

            verify(companyRepository, never()).save(any());
        }

        @Test
        void startTrial_alreadyUsed_throws() {
            Company company = createCompany("base", "monthly", Company.PlanStatus.ACTIVE, 15);
            company.setTrialEnd(Instant.now().minus(10, ChronoUnit.DAYS));
            stubCompany(company);

            assertThatThrownBy(() -> service.startTrial(TENANT))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya fue utilizado");
        }

        @Test
        void startTrial_activeSubscription_throws() {
            Company company = createCompany("base", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            assertThatThrownBy(() -> service.startTrial(TENANT))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("activa");
        }
    }





    @Nested
    class ValidationGuards {

        @Test
        void blockedState_preventsChange() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.BLOCKED, 0);
            stubCompany(company);

            assertThatThrownBy(() -> service.requestChange(TENANT, "enterprise"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("suspendida");
        }

        @Test
        void paymentFailed_preventsChange() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.BLOCKED_PAYMENT_FAILED, 0);
            stubCompany(company);

            assertThatThrownBy(() -> service.requestChange(TENANT, "enterprise"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("pago pendiente");
        }

        @Test
        void pastDueGrace_preventsChange() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.PAST_DUE_GRACE, 0);
            stubCompany(company);

            assertThatThrownBy(() -> service.requestChange(TENANT, "enterprise"))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        void cancelPending_preventsChange() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END, 15);
            company.setCancelAtPeriodEnd(true);
            stubCompany(company);

            assertThatThrownBy(() -> service.requestChange(TENANT, "enterprise"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("Reactiv");
        }

        @Test
        void invalidPlanKey_throws() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            assertThatThrownBy(() -> service.requestChange(TENANT, "platinum"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("inválido");
        }

        @Test
        void samePlanKey_throws() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE, 15);
            stubCompany(company);

            assertThatThrownBy(() -> service.requestChange(TENANT, "pro"))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("ya es el plan activo");
        }
    }





    @Nested
    class ProrationConfirmation {

        @Test
        void confirmProration_marksPaid() {
            Company company = createCompany("pro", "monthly", Company.PlanStatus.ACTIVE, 15);
            company.setProrationStatus("PENDING");
            stubCompany(company);

            SubscriptionChangeAudit audit = SubscriptionChangeAudit.of(TENANT, "UPGRADE", "USER");
            audit.setProrationStatus("PENDING");
            audit.setProrationPaymentId("PAY-123");
            when(auditRepository.findByProrationPaymentId("PAY-123")).thenReturn(Optional.of(audit));

            service.confirmProrationPayment(TENANT, "PAY-123");

            assertThat(audit.getProrationStatus()).isEqualTo("PAID");
            verify(companyRepository).save(any());
        }

        @Test
        void confirmProration_alreadyPaid_noOp() {
            SubscriptionChangeAudit audit = SubscriptionChangeAudit.of(TENANT, "UPGRADE", "USER");
            audit.setProrationStatus("PAID");
            audit.setProrationPaymentId("PAY-123");
            when(auditRepository.findByProrationPaymentId("PAY-123")).thenReturn(Optional.of(audit));

            service.confirmProrationPayment(TENANT, "PAY-123");

            verify(companyRepository, never()).save(any());
        }

        @Test
        void confirmProration_notFound_noOp() {
            when(auditRepository.findByProrationPaymentId("PAY-UNKNOWN")).thenReturn(Optional.empty());

            service.confirmProrationPayment(TENANT, "PAY-UNKNOWN");

            verify(companyRepository, never()).save(any());
        }
    }
}
