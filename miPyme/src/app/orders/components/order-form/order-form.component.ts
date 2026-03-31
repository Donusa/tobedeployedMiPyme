import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { OrdersService } from '../../services/orders.service';
import { Order } from '../../models/order.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-order-form',
  template: `
    <div class="page-container">
      <div class="page-header">
        <h1>Nuevo Pedido</h1>
      </div>

      <div class="form-card">
        <form (ngSubmit)="onSubmit()">
          <div class="form-group">
            <label>Cliente</label>
            <input type="text" [(ngModel)]="order.customer.name" name="customerName" required placeholder="Nombre del cliente">
          </div>

          <div class="form-group">
             <label>Producto (Simulado)</label>
             <input type="text" [(ngModel)]="tempProduct" name="productName" placeholder="Nombre del producto">
          </div>

          <div class="form-group">
             <label>Total</label>
             <input type="number" [(ngModel)]="order.total" name="total" required>
          </div>

          <div class="actions">
            <button type="button" class="btn-secondary" (click)="cancel()">Cancelar</button>
            <button type="submit" class="btn-primary">Crear Pedido</button>
          </div>
        </form>
      </div>
    </div>
  `,
  styles: [`
    .page-container { padding: 24px; background: #f3f4f6; min-height: 100vh; }
    .page-header h1 { font-size: 24px; font-weight: 700; color: #111827; margin: 0 0 24px 0; }
    .form-card { background: white; padding: 24px; border-radius: 8px; max-width: 600px; margin: 0 auto; box-shadow: 0 1px 3px rgba(0,0,0,0.05); }
    .form-group { margin-bottom: 16px; display: flex; flex-direction: column; gap: 8px; }
    label { font-weight: 600; color: #374151; font-size: 14px; }
    input { padding: 8px; border: 1px solid #ddd; border-radius: 4px; font-size: 14px; color: #111827; }
    .actions { display: flex; gap: 12px; justify-content: flex-end; margin-top: 24px; }
    .btn-primary { background: #3b82f6; color: white; border: none; padding: 8px 16px; border-radius: 4px; cursor: pointer; }
    .btn-primary:hover { background: #2563eb; }
    .btn-secondary { background: #fff; border: 1px solid #ddd; padding: 8px 16px; border-radius: 4px; cursor: pointer; color: #374151; }
    .btn-secondary:hover { background: #f9fafb; }

    /* Dark theme overrides */
    :host-context(body.dark-theme) .page-container { background: #111827; }
    :host-context(body.dark-theme) .page-header h1 { color: #f9fafb; }
    :host-context(body.dark-theme) .form-card { background: #1f2937; box-shadow: 0 1px 3px rgba(0,0,0,0.3); }
    :host-context(body.dark-theme) label { color: #d1d5db; }
    :host-context(body.dark-theme) input { background: #374151; color: #f9fafb; border-color: #4b5563; }
    :host-context(body.dark-theme) input::placeholder { color: #6b7280; }
    :host-context(body.dark-theme) input:focus { border-color: #60a5fa; outline-color: #60a5fa; }
    :host-context(body.dark-theme) .btn-primary { background: #3b82f6; color: #fff; }
    :host-context(body.dark-theme) .btn-primary:hover { background: #2563eb; }
    :host-context(body.dark-theme) .btn-secondary { background: #374151; border-color: #4b5563; color: #9ca3af; }
    :host-context(body.dark-theme) .btn-secondary:hover { background: #4b5563; border-color: #6b7280; color: #d1d5db; }
  `]
})
export class OrderFormComponent implements OnInit {
  order: any = {
    customer: {},
    items: [],
    total: 0,
    status: 'pending',
    source: 'local'
  };
  tempProduct = '';

  constructor(private ordersService: OrdersService, private router: Router) { }

  ngOnInit(): void {
  }

  onSubmit() {
    if (!this.order.customer.name || !this.order.total) {
        Swal.fire('Error', 'Complete los campos requeridos', 'error');
        return;
    }

    this.order.items.push({
        id: Math.random(),
        productName: this.tempProduct || 'Producto Genérico',
        quantity: 1,
        unitPrice: this.order.total,
        subtotal: this.order.total
    });

    this.ordersService.create(this.order).subscribe(() => {
        Swal.fire('Creado', 'Pedido creado exitosamente', 'success');
        this.router.navigate(['/pedidos']);
    });
  }

  cancel() {
    this.router.navigate(['/pedidos']);
  }
}
