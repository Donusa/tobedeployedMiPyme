import { Component, EventEmitter, Input, Output } from '@angular/core';
import { Order } from '../../models/order.model';
import { OrdersService } from '../../services/orders.service';
import { finalize } from 'rxjs/operators';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-order-detail-modal',
  templateUrl: './order-detail-modal.component.html',
  styleUrls: ['./order-detail-modal.component.css']
})
export class OrderDetailModalComponent {
  @Input() order: Order | null = null;
  @Output() close = new EventEmitter<void>();
  loading = false;

  constructor(private ordersService: OrdersService) { }

  onClose() {
    this.close.emit();
  }


  formatCurrency(amount: number): string {
    return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(amount);
  }

  canRevert(): boolean {
    if (!this.order) return false;

    if (this.order.source !== 'mercadolibre' && this.order.source !== 'tiendanube') return false;
    return ['PACKED', 'SHIPPED', 'DELIVERED'].includes(this.order.status);
  }

  revertStatus() {
    if (!this.order || this.loading) return;

    this.loading = true;
    this.ordersService.revertStatus(this.order).pipe(
      finalize(() => this.loading = false)
    ).subscribe(success => {
      if (success) {

        if (this.order!.status === 'DELIVERED') this.order!.status = 'SHIPPED';
        else if (this.order!.status === 'SHIPPED') this.order!.status = 'PACKED';
        else if (this.order!.status === 'PACKED') this.order!.status = 'RECEIVED';

        Swal.fire('Estado Revertido', 'El pedido ha vuelto al estado anterior', 'success');
      } else {
        Swal.fire('Error', 'No se pudo revertir el estado', 'error');
      }
    });
  }

  markAsPaid() {
    if (!this.order || this.loading) return;


    if (this.order?.source === 'tiendanube') {
      Swal.fire('Info', 'El estado de pago de TiendaNube se actualiza automáticamente.', 'info');
      return;
    }

    Swal.fire({
      title: '¿Confirmar Pago?',
      text: "El pedido se marcará como pagado en TiendaNube.",
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#3085d6',
      cancelButtonColor: '#d33',
      confirmButtonText: 'Sí, confirmar pago'
    }).then((result) => {
      if (result.isConfirmed) {
        this.loading = true;
        console.log('Modal: calling markPaid for order', this.order!.id);
        this.ordersService.markPaid(this.order!).pipe(
          finalize(() => this.loading = false)
        ).subscribe({
          next: (success) => {
            console.log('Modal: markPaid result', success);
            if (success) {
              this.order!.paymentStatus = 'paid';
              Swal.fire('¡Pagado!', 'El pedido ha sido marcado como pagado.', 'success');
            } else {
              Swal.fire('Error', 'No se pudo actualizar el estado de pago.', 'error');
            }
          },
          error: (err) => {
            console.error('Modal: error in markPaid subscription', err);
            Swal.fire('Error', 'Ocurrió un error al procesar la solicitud', 'error');
          }
        });
      }
    });
  }
}
