package com.mipyme.company;

import com.mipyme.mercadopago.model.MpChargeback;
import com.mipyme.mercadopago.model.MpClaim;
import com.mipyme.mercadopago.model.MpSubscription;
import com.mipyme.mercadopago.repository.MpChargebackRepository;
import com.mipyme.mercadopago.repository.MpClaimRepository;
import com.mipyme.mercadopago.repository.MpSubscriptionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EntitlementServiceTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private MpSubscriptionRepository subscriptionRepository;
    @Mock private MpClaimRepository claimRepository;
    @Mock private MpChargebackRepository chargebackRepository;
    @Mock private PlanLimitService planLimitService;

    private EntitlementService entitlementService;
    private static final String TENANT = "testTenant";

    @BeforeEach
    void setUp() {
        entitlementService = new EntitlementService(
                companyRepository, subscriptionRepository, claimRepository,
                chargebackRepository, planLimitService);
    }

    private Company createCompany(String tier, Company.PlanStatus status) {
        Company company = new Company("comp1", "Test Co", TENANT, Instant.now(), "TST123",
                null, null, null, null, null, null, null, null, true);
        company.setPlanTier(tier);
        company.setPlanStatus(status);
        return company;
    }

    private MpSubscription createMpSub(String status) {
        MpSubscription sub = new MpSubscription();
        sub.setSubscriptionId("SUB-123");
        sub.setStatus(status);
        sub.setTenantId(TENANT);
        sub.setPlanTier("pro");
        return sub;
    }


    private void stubDefaults(Company company) {
        when(companyRepository.findByTenantSchema(TENANT)).thenReturn(Optional.of(company));
        lenient().when(chargebackRepository.findByTenantIdAndStatusNot(eq(TENANT), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(claimRepository.findByTenantIdAndStatusNot(eq(TENANT), any()))
                .thenReturn(Collections.emptyList());
        lenient().when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                .thenReturn(Collections.emptyList());
    }





    @Test
    void activeChargeback_blocksImmediately() {
        Company company = createCompany("pro", Company.PlanStatus.ACTIVE);
        company.setValidUntil(Instant.now().plus(30, ChronoUnit.DAYS));
        when(companyRepository.findByTenantSchema(TENANT)).thenReturn(Optional.of(company));

        MpChargeback chargeback = new MpChargeback();
        chargeback.setTenantId(TENANT);
        chargeback.setStatus("open");
        chargeback.setDisputeId("CB-1");
        chargeback.setPaymentId("PAY-1");
        when(chargebackRepository.findByTenantIdAndStatusNot(TENANT, "resolved"))
                .thenReturn(List.of(chargeback));

        entitlementService.recalculate(TENANT);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue().getPlanStatus()).isEqualTo(Company.PlanStatus.BLOCKED);
        assertThat(captor.getValue().getAccessBlockedReason()).isEqualTo("CHARGEBACK");

        verify(planLimitService, never()).applyDowngradePolicies(any());
    }





    @Test
    void openClaim_setsInReview() {
        Company company = createCompany("pro", Company.PlanStatus.ACTIVE);
        company.setValidUntil(Instant.now().plus(30, ChronoUnit.DAYS));
        when(companyRepository.findByTenantSchema(TENANT)).thenReturn(Optional.of(company));
        when(chargebackRepository.findByTenantIdAndStatusNot(TENANT, "resolved"))
                .thenReturn(Collections.emptyList());

        MpClaim claim = new MpClaim();
        claim.setTenantId(TENANT);
        claim.setStatus("open");
        claim.setDisputeId("CLM-1");
        claim.setPaymentId("PAY-2");
        when(claimRepository.findByTenantIdAndStatusNot(TENANT, "resolved"))
                .thenReturn(List.of(claim));

        entitlementService.recalculate(TENANT);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        assertThat(captor.getValue().getPlanStatus()).isEqualTo(Company.PlanStatus.IN_REVIEW);

        verify(planLimitService, never()).applyDowngradePolicies(any());
    }





    @Nested
    class AuthorizedSubscription {

        @Test
        void authorized_setsActive() {
            Company company = createCompany("pro", Company.PlanStatus.PENDING_ACTIVATION);
            stubDefaults(company);
            MpSubscription sub = createMpSub("authorized");
            sub.setNextPaymentDate(Instant.now().plus(30, ChronoUnit.DAYS));
            when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                    .thenReturn(List.of(sub));

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE);
            assertThat(saved.getValidUntil()).isNotNull();
            assertThat(saved.getGraceUntil()).isNotNull();
        }

        @Test
        void authorized_withScheduledChange_keepsScheduledChangeStatus() {
            Company company = createCompany("pro", Company.PlanStatus.PENDING_ACTIVATION);
            company.setPendingPlanKey("base");
            stubDefaults(company);
            MpSubscription sub = createMpSub("authorized");
            sub.setNextPaymentDate(Instant.now().plus(30, ChronoUnit.DAYS));
            when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                    .thenReturn(List.of(sub));

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus())
                    .isEqualTo(Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
        }

        @Test
        void authorized_withCancelAtPeriodEnd_keepsCancelStatus() {
            Company company = createCompany("pro", Company.PlanStatus.PENDING_ACTIVATION);
            company.setCancelAtPeriodEnd(true);
            stubDefaults(company);
            MpSubscription sub = createMpSub("authorized");
            sub.setNextPaymentDate(Instant.now().plus(30, ChronoUnit.DAYS));
            when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                    .thenReturn(List.of(sub));

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus())
                    .isEqualTo(Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
        }

        @Test
        void authorized_alreadyActive_noStatusChange() {

            Company company = createCompany("pro", Company.PlanStatus.ACTIVE);
            company.setValidUntil(Instant.now().plus(10, ChronoUnit.DAYS));
            stubDefaults(company);
            MpSubscription sub = createMpSub("authorized");
            sub.setNextPaymentDate(Instant.now().plus(30, ChronoUnit.DAYS));
            when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                    .thenReturn(List.of(sub));

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE);

            assertThat(saved.getValidUntil()).isAfter(Instant.now().plus(29, ChronoUnit.DAYS));
        }
    }





    @Nested
    class CancelledSubscription {

        @Test
        void cancelled_withinPaidPeriod_keepsCancelAtPeriodEnd() {
            Company company = createCompany("pro", Company.PlanStatus.ACTIVE);
            company.setValidUntil(Instant.now().plus(10, ChronoUnit.DAYS));
            stubDefaults(company);
            MpSubscription sub = createMpSub("cancelled");
            when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                    .thenReturn(List.of(sub));

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
            assertThat(saved.isCancelAtPeriodEnd()).isTrue();
        }

        @Test
        void cancelled_periodExpired_blockedCanceled() {
            Company company = createCompany("pro", Company.PlanStatus.ACTIVE);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            stubDefaults(company);
            MpSubscription sub = createMpSub("cancelled");
            when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                    .thenReturn(List.of(sub));

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus())
                    .isEqualTo(Company.PlanStatus.BLOCKED_CANCELED);
        }
    }





    @Nested
    class GracePeriod {

        @Test
        void paused_withinPaidPeriod_entersPastDueGrace() {
            Company company = createCompany("pro", Company.PlanStatus.ACTIVE);
            company.setValidUntil(Instant.now().plus(2, ChronoUnit.DAYS));
            stubDefaults(company);
            MpSubscription sub = createMpSub("paused");
            when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                    .thenReturn(List.of(sub));

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            Company saved = captor.getValue();
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.PAST_DUE_GRACE);
            assertThat(saved.getGraceUntil()).isNotNull();
        }

        @Test
        void paused_periodExpired_noGraceSetYet_startsGrace() {
            Company company = createCompany("pro", Company.PlanStatus.ACTIVE);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));

            stubDefaults(company);
            MpSubscription sub = createMpSub("paused");
            when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                    .thenReturn(List.of(sub));

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());

            assertThat(captor.getValue().getPlanStatus()).isEqualTo(Company.PlanStatus.SUSPENDED);
        }

        @Test
        void pastDueGrace_graceExpired_blockedPaymentFailed() {
            Company company = createCompany("pro", Company.PlanStatus.PAST_DUE_GRACE);
            company.setValidUntil(Instant.now().minus(6, ChronoUnit.DAYS));
            company.setGraceUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            stubDefaults(company);

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus())
                    .isEqualTo(Company.PlanStatus.BLOCKED_PAYMENT_FAILED);
        }

        @Test
        void pastDueGrace_graceStillActive_staysInGrace() {
            Company company = createCompany("pro", Company.PlanStatus.PAST_DUE_GRACE);
            company.setValidUntil(Instant.now().minus(2, ChronoUnit.DAYS));
            company.setGraceUntil(Instant.now().plus(3, ChronoUnit.DAYS));
            stubDefaults(company);

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus()).isEqualTo(Company.PlanStatus.PAST_DUE_GRACE);
        }
    }





    @Nested
    class TrialExpiry {

        @Test
        void trial_expired_blocked() {
            Company company = createCompany("pro", Company.PlanStatus.TRIALING);
            company.setTrialEnd(Instant.now().minus(1, ChronoUnit.DAYS));
            stubDefaults(company);

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus())
                    .isEqualTo(Company.PlanStatus.BLOCKED_TRIAL_EXPIRED);
            assertThat(captor.getValue().getAccessBlockedReason()).isEqualTo("TRIAL_EXPIRED");
        }

        @Test
        void trial_notYetExpired_staysTrialing() {
            Company company = createCompany("pro", Company.PlanStatus.TRIALING);
            company.setTrialEnd(Instant.now().plus(10, ChronoUnit.DAYS));
            stubDefaults(company);

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus()).isEqualTo(Company.PlanStatus.TRIALING);
        }
    }





    @Nested
    class PeriodExpiry {

        @Test
        void activeScheduledChange_periodExpired_blockedExpired() {


            Company company = createCompany("pro", Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            company.setPendingPlanKey("base");
            stubDefaults(company);

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus()).isEqualTo(Company.PlanStatus.BLOCKED_EXPIRED);
        }

        @Test
        void cancelAtPeriodEnd_expired_blockedCanceled() {
            Company company = createCompany("pro", Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            company.setCancelAtPeriodEnd(true);
            stubDefaults(company);

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus())
                    .isEqualTo(Company.PlanStatus.BLOCKED_CANCELED);
        }

        @Test
        void active_noSubscription_expired_pendingActivation() {


            Company company = createCompany("pro", Company.PlanStatus.ACTIVE);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            stubDefaults(company);

            entitlementService.recalculate(TENANT);

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository).save(captor.capture());
            assertThat(captor.getValue().getPlanStatus()).isEqualTo(Company.PlanStatus.PENDING_ACTIVATION);
        }
    }





    @Test
    void authorizedSubscription_updatesTierAndCycleFromMpData() {
        Company company = createCompany("base", Company.PlanStatus.PAST_DUE_GRACE);
        stubDefaults(company);
        MpSubscription sub = createMpSub("authorized");
        sub.setPlanTier("enterprise-annual");
        sub.setNextPaymentDate(Instant.now().plus(365, ChronoUnit.DAYS));
        when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                .thenReturn(List.of(sub));

        entitlementService.recalculate(TENANT);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());
        Company saved = captor.getValue();
        assertThat(saved.getPlanTier()).isEqualTo("enterprise");
        assertThat(saved.getBillingCycle()).isEqualTo("annual");
        assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE);
    }

    @Test
    void authorizedSubscription_doesNotDowngradeTier() {

        Company company = createCompany("enterprise", Company.PlanStatus.PENDING_ACTIVATION);
        stubDefaults(company);
        MpSubscription sub = createMpSub("authorized");
        sub.setPlanTier("base");
        sub.setNextPaymentDate(Instant.now().plus(30, ChronoUnit.DAYS));
        when(subscriptionRepository.findByTenantIdOrderByUpdatedAtDesc(TENANT))
                .thenReturn(List.of(sub));

        entitlementService.recalculate(TENANT);

        ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
        verify(companyRepository).save(captor.capture());

        assertThat(captor.getValue().getPlanTier()).isEqualTo("enterprise");
    }





    @Test
    void companyNotFound_noError() {
        when(companyRepository.findByTenantSchema("unknown")).thenReturn(Optional.empty());
        when(companyRepository.findByCompanyId("unknown")).thenReturn(Optional.empty());

        entitlementService.recalculate("unknown");
        verify(companyRepository, never()).save(any());
    }
}
