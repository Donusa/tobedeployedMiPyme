import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { finalize } from 'rxjs/operators';
import { OrdersService } from '../../services/orders.service';
import { Order, OrderStatus, OrderSource } from '../../models/order.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-orders-list',
  templateUrl: './orders-list.component.html',
  styleUrls: ['./orders-list.component.css']
})
export class OrdersListComponent implements OnInit {
  orders: Order[] = [];
  filteredOrders: Order[] = [];
  isLoading = false;


  searchTerm = '';
  statusFilter: OrderStatus | 'all' = 'all';
  sourceFilter: OrderSource | 'all' = 'all';

  constructor(private ordersService: OrdersService, private router: Router) { }

  ngOnInit(): void {
    this.loadOrders();
  }

  loadOrders() {
    this.isLoading = true;
    this.ordersService.getAll().subscribe({
      next: (data) => {
        this.orders = data.sort((a, b) => {




          const isFinal = (status: string) => {
            const s = status.toLowerCase();
            return ['delivered', 'completed', 'cancelled', 'entregado', 'completado', 'cancelado'].includes(s);
          };

          const aFinal = isFinal(a.status);
          const bFinal = isFinal(b.status);

          if (aFinal && !bFinal) return 1;
          if (!aFinal && bFinal) return -1;


          return new Date(b.date).getTime() - new Date(a.date).getTime();
        });
        this.applyFilters();
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading orders', err);
        this.isLoading = false;
      }
    });
  }

  applyFilters() {
    this.filteredOrders = this.orders.filter(order => {
      const orderIdStr = order.id ? order.id.toString() : '';
      const customerName = order.customer && order.customer.name ? order.customer.name.toLowerCase() : '';
      const externalId = order.externalId ? order.externalId.toLowerCase() : '';
      const term = this.searchTerm ? this.searchTerm.toLowerCase() : '';

      const matchesSearch =
        orderIdStr.includes(term) ||
        customerName.includes(term) ||
        externalId.includes(term);

      const matchesStatus = this.statusFilter === 'all' || order.status === this.statusFilter;
      const matchesSource = this.sourceFilter === 'all' || order.source === this.sourceFilter;

      return matchesSearch && matchesStatus && matchesSource;
    });
  }

  getStatusClass(status: OrderStatus | string) {
    const s = status as string;
    switch (s) {
      case 'RECEIVED':
      case 'pending': return 'badge-warning';

      case 'PACKED':
      case 'processing': return 'badge-info';

      case 'SHIPPED':
      case 'shipped': return 'badge-primary';

      case 'DELIVERED':
      case 'delivered': return 'badge-success';

      case 'CANCELLED':
      case 'cancelled': return 'badge-danger';

      case 'COMPLETED':
      case 'paid': return 'badge-success';

      default: return 'badge-secondary';
    }
  }

  getStatusLabel(status: OrderStatus | string) {
    const s = status as string;
    switch (s) {
      case 'RECEIVED':
      case 'pending':
      case 'open':
      case 'paid':
        return 'Recibido';

      case 'PACKED':
      case 'processing':
      case 'ready_to_ship':
        return 'Empaquetado';

      case 'SHIPPED':
      case 'shipped':
        return 'Enviado';

      case 'DELIVERED':
      case 'delivered':
        return 'Entregado';

      case 'CANCELLED':
      case 'cancelled':
        return 'Cancelado';

      case 'COMPLETED':
        return 'Completado';

      default: return s;
    }
  }

  getSourceIcon(source: OrderSource) {
    switch (source) {
      case 'mercadolibre': return 'bx bxl-codepen';
      case 'tiendanube': return 'bx bx-cloud';
      case 'local': return 'bx bx-store';
      default: return 'bx bx-package';
    }
  }

  getSourceLabel(source: OrderSource) {
    switch (source) {
      case 'mercadolibre': return 'MercadoLibre';
      case 'tiendanube': return 'TiendaNube';
      case 'local': return 'Local';
      default: return source;
    }
  }

  loadingOrders: Set<number | string> = new Set();

  advanceStatus(order: Order) {
    if (this.loadingOrders.has(order.id)) return;

    let nextStatus: OrderStatus | null = null;


    const s = order.status as string;

    if (s === 'RECEIVED' || s === 'pending') nextStatus = 'PACKED';
    else if (s === 'PACKED' || s === 'processing') nextStatus = 'SHIPPED';
    else if (s === 'SHIPPED') nextStatus = 'DELIVERED';

    if (!nextStatus) return;


    if (s === 'CANCELLED' || s === 'cancelled') {
      Swal.fire('Error', 'No se puede avanzar un pedido cancelado', 'error');
      return;
    }


    if (nextStatus === 'SHIPPED') {
      this.loadingOrders.add(order.id);

      if (order.source === 'mercadolibre') {

        this.ordersService.markShipped(order).pipe(
          finalize(() => this.loadingOrders.delete(order.id))
        ).subscribe({
          next: (success) => {
            if (success) {
              order.status = 'SHIPPED';
              Swal.fire('Enviado', 'El pedido ha sido marcado como enviado', 'success');
            } else {
              Swal.fire('Error', 'No se pudo actualizar el estado en la plataforma', 'error');
            }
          },
          error: () => {
            Swal.fire('Error', 'Ocurrió un error al procesar la solicitud', 'error');
          }
        });
        return;
      }


      Swal.fire({
        title: 'Marcar como Enviado',
        html: `
                <input id="swal-tracking-code" class="swal2-input" placeholder="Código de Seguimiento">
                <input id="swal-tracking-url" class="swal2-input" placeholder="URL de Seguimiento (Opcional)">
            `,
        showCancelButton: true,
        confirmButtonText: 'Confirmar Envío',
        preConfirm: () => {
          return {
            code: (document.getElementById('swal-tracking-code') as HTMLInputElement).value,
            url: (document.getElementById('swal-tracking-url') as HTMLInputElement).value
          }
        }
      }).then((result) => {
        if (result.isConfirmed) {

          const tracking = result.value;
          this.ordersService.markShipped(order, tracking).pipe(
            finalize(() => this.loadingOrders.delete(order.id))
          ).subscribe({
            next: (success) => {
              if (success) {
                order.status = 'SHIPPED';
                Swal.fire('Enviado', 'El pedido ha sido marcado como enviado', 'success');
              } else {
                Swal.fire('Error', 'No se pudo actualizar el estado en la plataforma', 'error');
              }
            },
            error: () => {
              Swal.fire('Error', 'Ocurrió un error al procesar la solicitud', 'error');
            }
          });
        } else {
          this.loadingOrders.delete(order.id);
        }
      });
      return;
    }


    this.loadingOrders.add(order.id);
    let action$: any;
    if (nextStatus === 'PACKED') action$ = this.ordersService.markPacked(order);
    else if (nextStatus === 'DELIVERED') action$ = this.ordersService.markDelivered(order);

    if (action$) {
      action$.pipe(
        finalize(() => this.loadingOrders.delete(order.id))
      ).subscribe({
        next: (success: boolean) => {
          if (success) {
            order.status = nextStatus!;
            Swal.fire('Estado Actualizado', `El pedido ha pasado a ${this.getStatusLabel(nextStatus!)}`, 'success');
          } else {
            Swal.fire('Error', 'No se pudo actualizar el estado', 'error');
          }
        },
        error: () => {
          Swal.fire('Error', 'Ocurrió un error al procesar la solicitud', 'error');
        }
      });
    } else {
      this.loadingOrders.delete(order.id);
    }
  }

  markAsPaid(order: Order) {
    if (this.loadingOrders.has(order.id)) return;


    if (order.source === 'tiendanube') {
      Swal.fire('Info', 'El estado de pago de TiendaNube se actualiza automáticamente desde la plataforma.', 'info');
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
        this.loadingOrders.add(order.id);
        this.ordersService.markPaid(order).pipe(
          finalize(() => this.loadingOrders.delete(order.id))
        ).subscribe({
          next: (success) => {
            if (success) {
              order.paymentStatus = 'paid';
              Swal.fire('¡Pagado!', 'El pedido ha sido marcado como pagado.', 'success');
            } else {
              Swal.fire('Error', 'No se pudo actualizar el estado de pago.', 'error');
            }
          },
          error: () => {
            Swal.fire('Error', 'Ocurrió un error al procesar la solicitud', 'error');
          }
        });
      }
    });
  }

  selectedOrder: Order | null = null;

  viewDetail(order: Order) {
    this.selectedOrder = order;
  }

  closeDetail() {
    this.selectedOrder = null;
  }

  createNewOrder() {
    this.router.navigate(['/pedidos/nuevo']);
  }
}
