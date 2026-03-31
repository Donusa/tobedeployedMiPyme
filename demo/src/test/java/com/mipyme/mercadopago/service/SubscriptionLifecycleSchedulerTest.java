package com.mipyme.mercadopago.service;

import com.mipyme.company.Company;
import com.mipyme.company.CompanyRepository;
import com.mipyme.company.EntitlementService;
import com.mipyme.company.SubscriptionChangeService;
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
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class SubscriptionLifecycleSchedulerTest {

    @Mock private CompanyRepository companyRepository;
    @Mock private EntitlementService entitlementService;
    @Mock private SubscriptionChangeService subscriptionChangeService;
    @Mock private MpSubscriptionService subscriptionService;

    private SubscriptionLifecycleScheduler scheduler;

    @BeforeEach
    void setUp() {
        scheduler = new SubscriptionLifecycleScheduler(
                companyRepository, entitlementService,
                subscriptionChangeService, subscriptionService);
    }

    private Company makeCompany(String tenantSchema, String tier, Company.PlanStatus status) {
        Company company = new Company("id-" + tenantSchema, "Test", tenantSchema,
                Instant.now(), tenantSchema.substring(0, Math.min(6, tenantSchema.length())).toUpperCase(),
                null, null, null, null, null, null, "test@test.com", null, true);
        company.setPlanTier(tier);
        company.setPlanStatus(status);
        return company;
    }


    private void stubAllEmpty() {
        lenient().when(companyRepository.findByPendingPlanKeyIsNotNull()).thenReturn(Collections.emptyList());
        lenient().when(companyRepository.findByCancelAtPeriodEndTrueAndPlanStatusIn(any())).thenReturn(Collections.emptyList());
        lenient().when(companyRepository.findByPlanStatusIn(any())).thenReturn(Collections.emptyList());
        lenient().when(companyRepository.findByProrationStatus(any())).thenReturn(Collections.emptyList());
        lenient().when(companyRepository.findByTenantSchema(any())).thenReturn(Optional.empty());
    }





    @Nested
    class ScheduledChanges {

        @Test
        void appliesDowngradeWhenValidUntilPassed() {
            stubAllEmpty();
            Company company = makeCompany("tenant1", "pro", Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            company.setPendingPlanKey("base");
            company.setScheduledChangeType("DOWNGRADE");
            company.setBillingCycle("monthly");
            when(companyRepository.findByPendingPlanKeyIsNotNull()).thenReturn(List.of(company));
            when(companyRepository.findByTenantSchema("tenant1")).thenReturn(Optional.of(company));

            scheduler.runNightlyLifecycle();

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository, atLeastOnce()).save(captor.capture());
            Company saved = captor.getAllValues().stream()
                    .filter(c -> "tenant1".equals(c.getTenantSchema()))
                    .findFirst().orElseThrow();
            assertThat(saved.getPlanTier()).isEqualTo("base");
            assertThat(saved.getBillingCycle()).isEqualTo("monthly");
            assertThat(saved.getPendingPlanKey()).isNull();
            assertThat(saved.getScheduledChangeType()).isNull();
            assertThat(saved.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE);
        }

        @Test
        void appliesFrequencyChangeWhenValidUntilPassed() {
            stubAllEmpty();
            Company company = makeCompany("tenant2", "pro", Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
            company.setValidUntil(Instant.now().minus(2, ChronoUnit.HOURS));
            company.setPendingPlanKey("pro-annual");
            company.setScheduledChangeType("FREQUENCY_ONLY");
            company.setBillingCycle("monthly");
            when(companyRepository.findByPendingPlanKeyIsNotNull()).thenReturn(List.of(company));
            when(companyRepository.findByTenantSchema("tenant2")).thenReturn(Optional.of(company));

            scheduler.runNightlyLifecycle();

            ArgumentCaptor<Company> captor = ArgumentCaptor.forClass(Company.class);
            verify(companyRepository, atLeastOnce()).save(captor.capture());
            Company saved = captor.getAllValues().stream()
                    .filter(c -> "tenant2".equals(c.getTenantSchema()))
                    .findFirst().orElseThrow();
            assertThat(saved.getPlanTier()).isEqualTo("pro");
            assertThat(saved.getBillingCycle()).isEqualTo("annual");
        }

        @Test
        void doesNotApplyIfValidUntilStillInFuture() {
            stubAllEmpty();
            Company company = makeCompany("tenant3", "pro", Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
            company.setValidUntil(Instant.now().plus(5, ChronoUnit.DAYS));
            company.setPendingPlanKey("base-annual");
            when(companyRepository.findByPendingPlanKeyIsNotNull()).thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();


            assertThat(company.getPendingPlanKey()).isEqualTo("base-annual");

            verify(companyRepository, never()).save(company);
        }

        @Test
        void appliesWhenValidUntilIsNull() {
            stubAllEmpty();
            Company company = makeCompany("tenant4", "pro", Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE);
            company.setValidUntil(null);
            company.setPendingPlanKey("enterprise");
            company.setScheduledChangeType("UPGRADE_MIGRATE");
            when(companyRepository.findByPendingPlanKeyIsNotNull()).thenReturn(List.of(company));
            when(companyRepository.findByTenantSchema("tenant4")).thenReturn(Optional.of(company));

            scheduler.runNightlyLifecycle();

            assertThat(company.getPlanTier()).isEqualTo("enterprise");
            assertThat(company.getPendingPlanKey()).isNull();
        }
    }





    @Nested
    class CancelAtPeriodEnd {

        @Test
        void blocksCanceledWhenPeriodExpired() {
            stubAllEmpty();
            Company company = makeCompany("cancel1", "pro", Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
            company.setCancelAtPeriodEnd(true);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            when(companyRepository.findByCancelAtPeriodEndTrueAndPlanStatusIn(any()))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            assertThat(company.getPlanStatus()).isEqualTo(Company.PlanStatus.BLOCKED_CANCELED);
            assertThat(company.getAccessBlockedReason()).isEqualTo("SUBSCRIPTION_CANCELED");
            verify(companyRepository).save(company);
        }

        @Test
        void doesNotBlockIfPeriodStillActive() {
            stubAllEmpty();
            Company company = makeCompany("cancel2", "pro", Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
            company.setCancelAtPeriodEnd(true);
            company.setValidUntil(Instant.now().plus(10, ChronoUnit.DAYS));
            when(companyRepository.findByCancelAtPeriodEndTrueAndPlanStatusIn(any()))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            assertThat(company.getPlanStatus()).isEqualTo(Company.PlanStatus.ACTIVE_CANCEL_AT_PERIOD_END);
            verify(companyRepository, never()).save(company);
        }
    }





    @Nested
    class ExpiredPeriods {

        @Test
        void triggersRecalculateForExpiredActive() {
            stubAllEmpty();
            Company company = makeCompany("exp1", "pro", Company.PlanStatus.ACTIVE);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            when(companyRepository.findByPlanStatusIn(List.of(Company.PlanStatus.ACTIVE, Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE)))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            verify(entitlementService).recalculate("exp1");
        }

        @Test
        void doesNotRecalculateIfCancelAtPeriodEnd() {
            stubAllEmpty();
            Company company = makeCompany("exp2", "pro", Company.PlanStatus.ACTIVE);
            company.setValidUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            company.setCancelAtPeriodEnd(true);
            when(companyRepository.findByPlanStatusIn(List.of(Company.PlanStatus.ACTIVE, Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE)))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            verify(entitlementService, never()).recalculate("exp2");
        }

        @Test
        void doesNotRecalculateIfPeriodStillActive() {
            stubAllEmpty();
            Company company = makeCompany("exp3", "pro", Company.PlanStatus.ACTIVE);
            company.setValidUntil(Instant.now().plus(5, ChronoUnit.DAYS));
            when(companyRepository.findByPlanStatusIn(List.of(Company.PlanStatus.ACTIVE, Company.PlanStatus.ACTIVE_SCHEDULED_CHANGE)))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            verify(entitlementService, never()).recalculate(any());
        }
    }





    @Nested
    class GraceExpiry {

        @Test
        void blocksPaymentFailedWhenGraceExpired() {
            stubAllEmpty();
            Company company = makeCompany("grace1", "pro", Company.PlanStatus.PAST_DUE_GRACE);
            company.setGraceUntil(Instant.now().minus(1, ChronoUnit.DAYS));
            when(companyRepository.findByPlanStatusIn(List.of(Company.PlanStatus.PAST_DUE_GRACE)))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            assertThat(company.getPlanStatus()).isEqualTo(Company.PlanStatus.BLOCKED_PAYMENT_FAILED);
            assertThat(company.getAccessBlockedReason()).isEqualTo("PAYMENT_FAILED");
            verify(companyRepository).save(company);
        }

        @Test
        void doesNotBlockIfGraceStillActive() {
            stubAllEmpty();
            Company company = makeCompany("grace2", "pro", Company.PlanStatus.PAST_DUE_GRACE);
            company.setGraceUntil(Instant.now().plus(3, ChronoUnit.DAYS));
            when(companyRepository.findByPlanStatusIn(List.of(Company.PlanStatus.PAST_DUE_GRACE)))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            assertThat(company.getPlanStatus()).isEqualTo(Company.PlanStatus.PAST_DUE_GRACE);
            verify(companyRepository, never()).save(company);
        }
    }





    @Nested
    class TrialExpiry {

        @Test
        void blocksTrialExpiredWhenTrialEnded() {
            stubAllEmpty();
            Company company = makeCompany("trial1", "pro", Company.PlanStatus.TRIALING);
            company.setTrialEnd(Instant.now().minus(1, ChronoUnit.DAYS));
            when(companyRepository.findByPlanStatusIn(List.of(Company.PlanStatus.TRIALING)))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            assertThat(company.getPlanStatus()).isEqualTo(Company.PlanStatus.BLOCKED_TRIAL_EXPIRED);
            assertThat(company.getAccessBlockedReason()).isEqualTo("TRIAL_EXPIRED");
            verify(companyRepository).save(company);
        }

        @Test
        void doesNotBlockIfTrialStillRunning() {
            stubAllEmpty();
            Company company = makeCompany("trial2", "pro", Company.PlanStatus.TRIALING);
            company.setTrialEnd(Instant.now().plus(15, ChronoUnit.DAYS));
            when(companyRepository.findByPlanStatusIn(List.of(Company.PlanStatus.TRIALING)))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            assertThat(company.getPlanStatus()).isEqualTo(Company.PlanStatus.TRIALING);
            verify(companyRepository, never()).save(company);
        }
    }





    @Nested
    class OutstandingProrations {

        @Test
        void waivesPendingProrations() {
            stubAllEmpty();
            Company company = makeCompany("pror1", "pro", Company.PlanStatus.ACTIVE);
            company.setProrationStatus("PENDING");
            when(companyRepository.findByProrationStatus("PENDING"))
                    .thenReturn(List.of(company));

            scheduler.runNightlyLifecycle();

            verify(subscriptionChangeService).waveOutstandingProrations("pror1");
        }

        @Test
        void noPendingProrations_noCalls() {
            stubAllEmpty();

            scheduler.runNightlyLifecycle();

            verify(subscriptionChangeService, never()).waveOutstandingProrations(any());
        }
    }





    @Test
    void errorInOneCompanyDoesNotStopOthers() {
        stubAllEmpty();
        Company fail = makeCompany("fail1", "pro", Company.PlanStatus.PAST_DUE_GRACE);
        fail.setGraceUntil(Instant.now().minus(1, ChronoUnit.DAYS));
        Company ok = makeCompany("ok1", "base", Company.PlanStatus.PAST_DUE_GRACE);
        ok.setGraceUntil(Instant.now().minus(1, ChronoUnit.DAYS));

        when(companyRepository.findByPlanStatusIn(List.of(Company.PlanStatus.PAST_DUE_GRACE)))
                .thenReturn(List.of(fail, ok));


        doThrow(new RuntimeException("DB error")).when(companyRepository).save(fail);

        scheduler.runNightlyLifecycle();


        verify(companyRepository).save(ok);
        assertThat(ok.getPlanStatus()).isEqualTo(Company.PlanStatus.BLOCKED_PAYMENT_FAILED);
    }
}
