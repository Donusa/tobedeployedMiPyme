import { Component, OnInit, OnDestroy } from '@angular/core';
import { ActivatedRoute, Router } from '@angular/router';
import { Subscription } from 'rxjs';
import { PlanService, PlanType } from '../../services/plan.service';
import { PLAN_FEATURES, PlanDefinition } from '../../config/plan-features.config';

@Component({
    selector: 'app-plan-required',
    templateUrl: './plan-required.component.html',
    styleUrls: ['./plan-required.component.css']
})
export class PlanRequiredComponent implements OnInit, OnDestroy {
    requiredPlan: PlanType = 'pro';
    requiredPlanLabel = 'Pro';
    planDefinition: PlanDefinition | null = null;
    private sub!: Subscription;

    constructor(
        private route: ActivatedRoute,
        private router: Router,
        private planService: PlanService
    ) { }

    ngOnInit(): void {
        this.sub = this.route.queryParamMap.subscribe(params => {
            const plan = params.get('plan') as PlanType;
            if (plan) {
                this.requiredPlan = plan;
                this.requiredPlanLabel = this.planService.getRequiredPlanLabel(plan);
                this.planDefinition = PLAN_FEATURES[plan] ?? null;
            }
        });
    }

    ngOnDestroy(): void {
        this.sub?.unsubscribe();
    }

    goToPlans(): void {
        this.router.navigate(['/configuracion/facturacion/plan']);
    }
}
