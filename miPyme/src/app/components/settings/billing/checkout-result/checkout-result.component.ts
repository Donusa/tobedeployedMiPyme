import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { MercadoPagoService, SubscriptionStatus } from '../../../../services/mercadopago.service';
import { PlanService, PlanType } from '../../../../services/plan.service';

@Component({
    selector: 'app-checkout-result',
    templateUrl: './checkout-result.component.html',
    styleUrls: ['./checkout-result.component.css']
})
export class CheckoutResultComponent implements OnInit, OnDestroy {
    status: 'loading' | 'success' | 'pending' | 'failure' = 'loading';
    subscriptionStatus: SubscriptionStatus | null = null;
    private pollTimer: ReturnType<typeof setInterval> | null = null;
    private pollAttempts = 0;
    private readonly MAX_POLL_ATTEMPTS = 10;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private mpService: MercadoPagoService,
        private planService: PlanService
    ) { }

    ngOnInit(): void {
        const params = this.route.snapshot.queryParamMap;
        const mpStatus = params.get('status');
        const preapprovalId = params.get('preapproval_id');

        if (mpStatus === 'authorized' || mpStatus === 'approved' || preapprovalId) {
            this.status = 'success';
        } else if (mpStatus === 'pending') {
            this.status = 'pending';
        } else {
            this.status = 'failure';
        }


        this.syncSubscriptionStatus();
        if (this.status === 'success' || this.status === 'pending') {
            this.pollTimer = setInterval(() => {
                this.pollAttempts++;
                if (this.pollAttempts >= this.MAX_POLL_ATTEMPTS) {
                    this.stopPolling();
                    return;
                }
                this.syncSubscriptionStatus();
            }, 3000);
        }
    }

    ngOnDestroy(): void {
        this.stopPolling();
    }

    private stopPolling(): void {
        if (this.pollTimer) {
            clearInterval(this.pollTimer);
            this.pollTimer = null;
        }
    }

    private syncSubscriptionStatus(): void {
        this.mpService.getSubscriptionStatus().subscribe({
            next: (sub) => {
                this.subscriptionStatus = sub;
                if (sub.planStatus) {
                    this.planService.setPlanStatus(sub.planStatus as any);
                }
                if (sub.planTier) {
                    const tier = sub.planTier.toLowerCase() as PlanType;
                    this.planService.setPlan(tier);
                }

                if (sub.subscriptionStatus === 'authorized' || sub.subscriptionStatus === 'active') {
                    this.status = 'success';
                    this.stopPolling();
                }
            }
        });

        this.planService.syncFromBackend();
    }

    goToPlans(): void {
        this.router.navigate(['/configuracion/facturacion/plan']);
    }

    goHome(): void {
        this.router.navigate(['/home']);
    }
}
