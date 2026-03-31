import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface SubscriptionStatus {
  planStatus: string;
  validUntil: string | null;
  graceUntil: string | null;
  subscriptionStatus: string | null;
  subscriptionId: string | null;
  amount: string | null;
  planTier: string | null;
  billingCycle: 'monthly' | 'annual' | null;
  pendingPlanKey: string | null;

  accessBlockedReason: string | null;
  scheduledChangeType: string | null;
  cancelAtPeriodEnd: boolean;
  trialEnd: string | null;
  prorationAmount: string | null;
  prorationStatus: string | null;
}


export interface PlanChangeResult {

  changeType: string;

  immediate: boolean;
  newPlanKey: string;

  scheduledEffectiveAt: string | null;

  prorationAmount: string | null;

  prorationInitPoint: string | null;

  message: string;
}

export interface SubscribeResponse {
  initPoint: string;
}

export interface PaymentRecord {
  paymentId: string;
  status: string;
  statusDetail: string;
  amount: string;
  currency: string;
  approvedAt: string | null;
  payerEmail: string;
}

export interface PlanPrice {
  usd: number;
  ars: number;
}

export interface PlanPricingInfo {
  exchangeRate: number;
  rateUpdatedAt: string | null;
  monthly: { [key: string]: PlanPrice };
  annual: { [key: string]: PlanPrice };
}

@Injectable({
  providedIn: 'root'
})
export class MercadoPagoService {
  private apiUrl = `${environment.apiUrl}/api/mercadopago`;

  constructor(private http: HttpClient) { }

  subscribe(planType: string, payerEmail: string): Observable<SubscribeResponse> {
    return this.http.post<SubscribeResponse>(`${this.apiUrl}/subscribe`, { planType, payerEmail });
  }

  getSubscriptionStatus(): Observable<SubscriptionStatus> {
    return this.http.get<SubscriptionStatus>(`${this.apiUrl}/subscription/status`);
  }

  cancelSubscription(): Observable<any> {
    return this.http.post(`${this.apiUrl}/subscription/cancel`, {});
  }

  revertCancel(): Observable<{ status: string }> {
    return this.http.post<{ status: string }>(`${this.apiUrl}/subscription/revert-cancel`, {});
  }

  scheduleChange(newPlanKey: string): Observable<PlanChangeResult> {
    return this.http.post<PlanChangeResult>(`${this.apiUrl}/subscription/schedule-change`, { newPlanKey });
  }

  cancelScheduledChange(): Observable<any> {
    return this.http.delete(`${this.apiUrl}/subscription/schedule-change`);
  }

  pauseSubscription(): Observable<any> {
    return this.http.post(`${this.apiUrl}/subscription/pause`, {});
  }

  getPayments(): Observable<PaymentRecord[]> {
    return this.http.get<PaymentRecord[]>(`${this.apiUrl}/payments`);
  }

  getPricing(): Observable<PlanPricingInfo> {
    return this.http.get<PlanPricingInfo>(`${this.apiUrl}/pricing`);
  }
}
