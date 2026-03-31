import { Component, OnInit, OnDestroy } from '@angular/core';
import { PlanService, PlanType } from '../../../../services/plan.service';
import { MercadoPagoService, SubscriptionStatus, PlanPricingInfo, PlanPrice, PlanChangeResult } from '../../../../services/mercadopago.service';
import { CompanyService } from '../../../../services/company.service';
import { Subscription } from 'rxjs';
import { PLAN_FEATURES } from '../../../../config/plan-features.config';

interface PlanCard {
    id: PlanType;
    name: string;
    description: string;
    features: string[];
}

@Component({
    selector: 'app-billing-plan',
    templateUrl: './billing-plan.component.html',
    styleUrls: ['./billing-plan.component.css']
})
export class BillingPlanComponent implements OnInit, OnDestroy {
    currentPlan: PlanType = 'base';
    currentBillingCycle: 'monthly' | 'annual' = 'monthly';
    subscriptionStatus: SubscriptionStatus | null = null;
    pricing: PlanPricingInfo | null = null;
    billingCycle: 'monthly' | 'annual' = 'monthly';
    isLoading = false;
    isStatusLoading = true;
    errorMessage = '';
    successMessage = '';
    companyEmail = '';
    showCancelConfirm = false;
    private sub!: Subscription;

    plans: PlanCard[] = (
        ['base', 'pro', 'enterprise'] as PlanType[]
    ).map(id => ({
        id,
        name: PLAN_FEATURES[id].label,
        description: PLAN_FEATURES[id].description,
        features: PLAN_FEATURES[id].features,
    }));

    constructor(
        private planService: PlanService,
        private mpService: MercadoPagoService,
        private companyService: CompanyService
    ) { }

    ngOnInit(): void {
        this.sub = this.planService.currentPlan$.subscribe(plan => {
            this.currentPlan = plan;
        });


        this.planService.syncFromBackend();

        this.companyService.getMyCompany().subscribe({
            next: (company) => {
                this.companyEmail = company.companyEmail || '';
            }
        });

        this.loadStatus();

        this.mpService.getPricing().subscribe({
            next: (pricing) => {
                this.pricing = pricing;
            }
        });
    }

    private loadStatus(): void {
        this.isStatusLoading = true;
        this.mpService.getSubscriptionStatus().subscribe({
            next: (status) => {
                this.subscriptionStatus = status;
                this.isStatusLoading = false;
                if (status.planTier) {
                    const tier = status.planTier.toLowerCase() as PlanType;
                    this.planService.setPlan(tier);
                }
                if (status.billingCycle) {
                    this.currentBillingCycle = status.billingCycle;

                    this.billingCycle = status.billingCycle;
                }
            },
            error: () => {
                this.isStatusLoading = false;
            }
        });
    }

    ngOnDestroy(): void {
        this.sub?.unsubscribe();
    }

    getPrice(planId: string): PlanPrice | null {
        if (!this.pricing) return null;
        const prices = this.billingCycle === 'annual' ? this.pricing.annual : this.pricing.monthly;
        return prices[planId] || null;
    }

    formatUsd(planId: string): string {
        const price = this.getPrice(planId);
        if (!price) return '...';
        return `US$ ${price.usd}`;
    }

    formatArs(planId: string): string {
        const price = this.getPrice(planId);
        if (!price || !price.ars) return '';
        return `$ ${price.ars.toLocaleString('es-AR')} ARS`;
    }

    getPeriodLabel(): string {
        return this.billingCycle === 'annual' ? '/ año' : '/ mes';
    }

    selectPlan(planId: PlanType): void {
        if (!this.companyEmail) {
            this.errorMessage = 'Configurá un email de empresa antes de suscribirte.';
            return;
        }

        this.isLoading = true;
        this.errorMessage = '';
        this.successMessage = '';

        const planKey = this.billingCycle === 'annual' ? `${planId}-annual` : planId;


        if (this.isSubscriptionActive) {
            this.mpService.scheduleChange(planKey).subscribe({
                next: (result: PlanChangeResult) => {
                    this.isLoading = false;
                    if (result.immediate) {

                        if (result.prorationInitPoint) {
                            const arsLabel = result.prorationAmount
                                ? `$ ${parseFloat(result.prorationAmount).toLocaleString('es-AR')}`
                                : '';
                            this.successMessage = `\u00a1Acceso a ${this.formatPlanLabel(planKey)} activado! ${arsLabel ? 'Completá el pago del diferencial de ' + arsLabel + ' para confirmar.' : ''}`;

                            setTimeout(() => { window.location.href = result.prorationInitPoint!; }, 2000);
                        } else {
                            this.successMessage = `\u00a1Acceso a ${this.formatPlanLabel(planKey)} activado!`;
                        }
                    } else {

                        const dateLabel = result.scheduledEffectiveAt
                            ? new Date(result.scheduledEffectiveAt).toLocaleDateString('es-AR')
                            : 'fin del ciclo actual';
                        this.successMessage = `Cambio a ${this.formatPlanLabel(planKey)} programado para el ${dateLabel}.`;
                    }
                    this.loadStatus();
                },
                error: (err) => {
                    this.isLoading = false;
                    this.errorMessage = err.error?.error || 'Error al procesar el cambio. Intentá de nuevo.';
                }
            });
        } else {

            this.mpService.subscribe(planKey, this.companyEmail).subscribe({
                next: (res) => {
                    this.isLoading = false;
                    window.location.href = res.initPoint;
                },
                error: (err) => {
                    this.isLoading = false;
                    this.errorMessage = err.error?.error || 'Error al iniciar la suscripci\u00f3n. Intent\u00e1 de nuevo.';
                }
            });
        }
    }

    cancelScheduledChange(): void {
        this.isLoading = true;
        this.errorMessage = '';
        this.successMessage = '';
        this.mpService.cancelScheduledChange().subscribe({
            next: () => {
                this.isLoading = false;
                this.successMessage = 'Cambio programado cancelado. Tu suscripción actual continuará.';
                this.loadStatus();
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMessage = err.error?.error || 'Error al cancelar el cambio programado.';
            }
        });
    }

    requestCancelSubscription(): void {
        this.showCancelConfirm = true;
        this.errorMessage = '';
        this.successMessage = '';
    }

    dismissCancelConfirm(): void {
        this.showCancelConfirm = false;
    }

    cancelSubscription(): void {
        this.showCancelConfirm = false;
        this.isLoading = true;
        this.errorMessage = '';
        this.successMessage = '';
        this.mpService.cancelSubscription().subscribe({
            next: () => {
                this.isLoading = false;
                this.successMessage = 'Tu suscripci\u00f3n fue cancelada. Segu\u00eds teniendo acceso hasta el fin del per\u00edodo ya abonado.';
                this.planService.setPlanStatus('active_cancel_at_period_end');
                this.mpService.getSubscriptionStatus().subscribe(s => this.subscriptionStatus = s);
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMessage = err.error?.error || 'Error al cancelar la suscripci\u00f3n.';
            }
        });
    }

    revertCancel(): void {
        this.isLoading = true;
        this.errorMessage = '';
        this.successMessage = '';
        this.mpService.revertCancel().subscribe({
            next: () => {
                this.isLoading = false;
                this.successMessage = '¡Suscripción reactivada! El cobro automático continuará.';
                this.planService.setPlanStatus('active');
                this.loadStatus();
            },
            error: (err) => {
                this.isLoading = false;
                this.errorMessage = err.error?.error || 'Error al reactivar la suscripción.';
            }
        });
    }


    isCurrentPlan(planId: PlanType): boolean {
        return this.currentPlan === planId && this.billingCycle === this.currentBillingCycle;
    }


    isPendingChange(planId: PlanType): boolean {
        if (!this.subscriptionStatus?.pendingPlanKey) return false;
        const pendingBase = this.subscriptionStatus.pendingPlanKey.replace('-annual', '');
        const pendingCycle = this.subscriptionStatus.pendingPlanKey.endsWith('-annual') ? 'annual' : 'monthly';
        return pendingBase === planId && pendingCycle === this.billingCycle;
    }

    formatPlanLabel(planKey: string): string {
        if (planKey.endsWith('-annual')) {
            const base = planKey.replace('-annual', '');
            return base.charAt(0).toUpperCase() + base.slice(1) + ' Anual';
        }
        return planKey.charAt(0).toUpperCase() + planKey.slice(1) + ' Mensual';
    }

    get pendingPlanLabel(): string {
        return this.subscriptionStatus?.pendingPlanKey
            ? this.formatPlanLabel(this.subscriptionStatus.pendingPlanKey)
            : '';
    }

    isCurrentPlan_simple(planId: PlanType): boolean {
        return this.currentPlan === planId;
    }

    get isSubscriptionActive(): boolean {
        const mpStatus = this.subscriptionStatus?.subscriptionStatus;
        const planStatus = this.subscriptionStatus?.planStatus?.toLowerCase();
        const activePlanStatuses = [
            'active', 'active_scheduled_change', 'active_cancel_at_period_end',
            'past_due_grace', 'trialing', 'trial'
        ];
        return mpStatus === 'authorized' || mpStatus === 'active'
            || (!!planStatus && activePlanStatuses.includes(planStatus));
    }
}
