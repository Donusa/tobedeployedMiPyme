import { inject } from '@angular/core';
import { Router } from '@angular/router';
import { AuthService } from '../services/auth.service';
import { PlanService } from '../services/plan.service';

export const authGuard = () => {
  const authService = inject(AuthService);
  const planService = inject(PlanService);
  const router = inject(Router);

  if (!authService.isLoggedIn()) {
    return router.parseUrl('/login');
  }


  const blockedStatuses = ['pending_payment', 'suspended', 'blocked'];
  const currentStatus = planService.getPlanStatus();
  if (blockedStatuses.includes(currentStatus)) {
    return router.parseUrl('/configuracion/facturacion/plan');
  }

  return true;
};

