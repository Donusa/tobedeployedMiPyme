import { Component, OnInit } from '@angular/core';
import { SalesService } from '../../services/sales.service';
import { Sale } from '../../models/sale.model';
import { LayoutService } from '../../../services/layout.service';
import { MobileCardColumn, MobileCardAction } from '../../../shared/components/mobile-card-list/mobile-card-list.models';

@Component({
  selector: 'app-sales-list',
  templateUrl: './sales-list.component.html',
  styleUrls: ['./sales-list.component.css']
})
export class SalesListComponent implements OnInit {
  sales: Sale[] = [];
  selectedSale: Sale | null = null;
  isLoading = false;


  readonly mobileColumns: MobileCardColumn[] = [
    {
      field: 'saleDate', label: 'Fecha', isPrimary: true,
      format: (v: any) => v ? new Date(v).toLocaleDateString('es-AR', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
    },
    { field: 'saleId',     label: 'ID Venta',  format: (v: any) => `#${v}` },
    { field: 'createdBy',  label: 'Vendedor' },
    {
      field: 'facturado', label: 'Estado', isBadge: true,
      format: (v: any) => v ? 'Facturado' : 'Pendiente',
      badgeClass: (item: Sale) => item.facturado ? 'badge-success' : 'badge-warning'
    },
    {
      field: 'totalAmount', label: 'Total', expandOnly: false,
      format: (v: any) => new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(v ?? 0)
    }
  ];

  readonly mobileActions: MobileCardAction[] = [
    {
      label: 'Ver detalle', icon: 'bx bx-show',
      callback: (sale: Sale) => this.openDetail(sale)
    }
  ];


  constructor(private salesService: SalesService, public layoutService: LayoutService) { }

  ngOnInit(): void {
    console.log('SalesListComponent initialized');
    this.loadSales();
  }

  loadSales() {
    this.isLoading = true;
    this.salesService.getAll().subscribe({
      next: (data) => {
        console.log('Sales loaded:', data);
        this.sales = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading sales:', err);
        this.isLoading = false;
      }
    });
  }

  openDetail(sale: Sale) {
    this.selectedSale = sale;
  }

  closeDetail() {
    this.selectedSale = null;
  }
}
