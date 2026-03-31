import { Component, OnInit } from '@angular/core';
import { MercadoPagoService, SubscriptionStatus } from '../../../../services/mercadopago.service';

@Component({
    selector: 'app-billing-payment',
    templateUrl: './billing-payment.component.html',
    styleUrls: ['./billing-payment.component.css']
})
export class BillingPaymentComponent implements OnInit {
    subscriptionStatus: SubscriptionStatus | null = null;
    isLoading = true;

    constructor(private mpService: MercadoPagoService) { }

    ngOnInit(): void {
        this.mpService.getSubscriptionStatus().subscribe({
            next: (status) => {
                this.subscriptionStatus = status;
                this.isLoading = false;
            },
            error: () => {
                this.isLoading = false;
            }
        });
    }

    getStatusLabel(status: string | null): string {
        if (!status) return 'Sin suscripción';
        const labels: Record<string, string> = {
            authorized: 'Activa',
            active: 'Activa',
            paused: 'Pausada',
            cancelled: 'Cancelada',
            pending: 'Pendiente'
        };
        return labels[status] || status;
    }

    getStatusClass(status: string | null): string {
        if (!status) return 'none';
        const classes: Record<string, string> = {
            authorized: 'active',
            active: 'active',
            paused: 'paused',
            cancelled: 'cancelled',
            pending: 'pending'
        };
        return classes[status] || 'none';
    }
}
