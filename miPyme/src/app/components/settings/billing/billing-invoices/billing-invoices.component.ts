import { Component, OnInit } from '@angular/core';
import { MercadoPagoService, PaymentRecord } from '../../../../services/mercadopago.service';

@Component({
    selector: 'app-billing-invoices',
    templateUrl: './billing-invoices.component.html',
    styleUrls: ['./billing-invoices.component.css']
})
export class BillingInvoicesComponent implements OnInit {
    payments: PaymentRecord[] = [];
    isLoading = true;

    constructor(private mpService: MercadoPagoService) { }

    ngOnInit(): void {
        this.mpService.getPayments().subscribe({
            next: (data) => {
                this.payments = data;
                this.isLoading = false;
            },
            error: () => {
                this.isLoading = false;
            }
        });
    }

    getStatusLabel(status: string): string {
        const labels: Record<string, string> = {
            approved: 'Aprobado',
            pending: 'Pendiente',
            rejected: 'Rechazado',
            cancelled: 'Cancelado',
            refunded: 'Reembolsado',
            charged_back: 'Contracargo'
        };
        return labels[status] || status;
    }

    getStatusClass(status: string): string {
        const classes: Record<string, string> = {
            approved: 'paid',
            pending: 'pending',
            rejected: 'rejected',
            cancelled: 'rejected',
            refunded: 'refunded'
        };
        return classes[status] || 'pending';
    }
}
