import { inject } from '@angular/core';
import { Router, ActivatedRouteSnapshot } from '@angular/router';
import { PlanService, PlanType } from '../services/plan.service';

export const planGuard = (route: ActivatedRouteSnapshot) => {
    const planService = inject(PlanService);
    const router = inject(Router);
    const requiredPlan = route.data['requiredPlan'] as PlanType | undefined;

    if (!requiredPlan) {
        return true;
    }

    if (planService.hasAccess(requiredPlan)) {
        return true;
    }


    return router.createUrlTree(['/plan-required'], {
        queryParams: { plan: requiredPlan }
    });
};
