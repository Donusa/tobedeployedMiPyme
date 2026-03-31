import { Injectable, inject } from '@angular/core';
import { BehaviorSubject, firstValueFrom, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../environments/environment';
import { Router } from '@angular/router';
import { AuthService } from './auth.service';

export type PlanType = 'base' | 'pro' | 'enterprise';
export type PlanStatus =

    | 'pending_activation'
    | 'trialing'
    | 'active'
    | 'active_scheduled_change'
    | 'active_cancel_at_period_end'
    | 'past_due_grace'
    | 'suspended'
    | 'blocked_trial_expired'
    | 'blocked_expired'
    | 'blocked_canceled'
    | 'blocked_payment_failed'
    | 'blocked'
    | 'in_review'

    | 'trial'
    | 'pending_payment'
    | 'past_due';

const PLAN_HIERARCHY: Record<PlanType, number> = {
    base: 0,
    pro: 1,
    enterprise: 2
};

const PLAN_LABELS: Record<PlanType, string> = {
    base: 'Base',
    pro: 'Pro',
    enterprise: 'Enterprise'
};


const BLOCKED_STATUSES: PlanStatus[] = [
    'pending_payment', 'pending_activation',
    'suspended',
    'blocked', 'blocked_trial_expired', 'blocked_expired',
    'blocked_canceled', 'blocked_payment_failed'
];

@Injectable({
    providedIn: 'root'
})
export class PlanService {



    private authService = inject(AuthService);
    private http = inject(HttpClient);
    private router = inject(Router);

    private planSubject = new BehaviorSubject<PlanType>(this.loadPlan());
    currentPlan$ = this.planSubject.asObservable();

    private planStatusSubject = new BehaviorSubject<PlanStatus>(this.loadPlanStatus());
    planStatus$ = this.planStatusSubject.asObservable();

    getCurrentPlan(): PlanType {
        return this.planSubject.value;
    }

    getPlanStatus(): PlanStatus {
        return this.planStatusSubject.value;
    }


    isAccessBlocked(): boolean {
        return BLOCKED_STATUSES.includes(this.planStatusSubject.value);
    }


    syncFromBackend(redirectOnBlocked = false): Promise<void> {
        return firstValueFrom(
            this.http.get<any>(`${environment.apiUrl}/api/companies/me`).pipe(
                catchError(() => of(null))
            )
        ).then(company => {
            if (!company) return;
            if (company.planTier) {
                const tier = company.planTier.toLowerCase() as PlanType;
                if (PLAN_HIERARCHY[tier] !== undefined) {
                    this.planSubject.next(tier);
                }
            } else {

                this.planSubject.next('base');
            }
            if (company.planStatus) {
                const status = company.planStatus.toLowerCase() as PlanStatus;
                this.planStatusSubject.next(status);
                if (redirectOnBlocked && BLOCKED_STATUSES.includes(status)) {
                    this.router.navigate(['/configuracion/facturacion/plan']);
                }
            }
        });
    }



    setFromAuthResponse(planTier: string | null, planStatus: string | null): void {
        if (planTier) {
            const tier = planTier.toLowerCase() as PlanType;
            if (PLAN_HIERARCHY[tier] !== undefined) {
                this.planSubject.next(tier);
            }
        }
        if (planStatus) {
            this.planStatusSubject.next(planStatus.toLowerCase() as PlanStatus);
        }
    }


    setPlan(plan: PlanType, status?: PlanStatus): void {
        this.planSubject.next(plan);
        if (status !== undefined) {
            this.planStatusSubject.next(status);
        }
    }


    setPlanStatus(status: PlanStatus): void {
        this.planStatusSubject.next(status);
    }

    hasAccess(requiredPlan: PlanType): boolean {
        const currentLevel = PLAN_HIERARCHY[this.getCurrentPlan()];
        const requiredLevel = PLAN_HIERARCHY[requiredPlan];
        return currentLevel >= requiredLevel;
    }

    getRequiredPlanLabel(plan: PlanType): string {
        return PLAN_LABELS[plan] || plan;
    }


    private loadPlan(): PlanType {
        const payload = this.authService.getDecodedToken();
        if (payload?.planTier) {
            const tier = (payload.planTier as string).toLowerCase() as PlanType;
            if (PLAN_HIERARCHY[tier] !== undefined) return tier;
        }
        return 'base';
    }


    private loadPlanStatus(): PlanStatus {
        const payload = this.authService.getDecodedToken();
        if (payload?.planStatus) {
            const status = (payload.planStatus as string).toLowerCase() as PlanStatus;
            const valid: PlanStatus[] = [
                'pending_activation', 'trialing', 'active',
                'active_scheduled_change', 'active_cancel_at_period_end',
                'past_due_grace', 'suspended',
                'blocked_trial_expired', 'blocked_expired',
                'blocked_canceled', 'blocked_payment_failed',
                'blocked', 'in_review',

                'trial', 'pending_payment', 'past_due'
            ];
            if (valid.includes(status)) return status;
        }
        return 'pending_activation';
    }
}

