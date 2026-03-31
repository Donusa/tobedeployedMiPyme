import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { SalesService } from '../../sales/services/sales.service';
import { Sale } from '../../sales/models/sale.model';
import { ArcaService, InvoiceIndex } from '../../services/arca.service';
import Swal from 'sweetalert2';
import { LayoutService } from '../../services/layout.service';
import { MobileCardColumn, MobileCardAction } from '../../shared/components/mobile-card-list/mobile-card-list.models';

@Component({
  selector: 'app-facturacion',
  templateUrl: './facturacion.component.html',
  styleUrls: ['./facturacion.component.css']
})
export class FacturacionComponent implements OnInit {
  sales: Sale[] = [];
  isLoading = false;
  searchTerm = '';


  activeTab: 'ventas' | 'comprobantes' = 'ventas';


  invoices: InvoiceIndex[] = [];
  isLoadingInvoices = false;
  invoiceSearchTerm = '';
  invoiceDateFrom = '';
  invoiceDateTo = '';


  readonly mobileColumns: MobileCardColumn[] = [
    {
      field: 'saleDate', label: 'Fecha', isPrimary: true,
      format: (v: any) => v ? new Date(v).toLocaleDateString('es-AR', { day: '2-digit', month: 'short', year: 'numeric' }) : '—'
    },
    { field: 'saleId',      label: 'ID Venta',  format: (v: any) => `#${v}` },
    { field: 'createdBy',   label: 'Vendedor' },
    {
      field: 'totalAmount', label: 'Total',
      format: (v: any) => new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(v ?? 0)
    },
    {
      field: 'facturado', label: 'Estado', isBadge: true,
      format: (v: any) => v ? 'Facturado' : 'Pendiente',
      badgeClass: (item: Sale) => item.facturado ? 'badge-success' : 'badge-warning'
    }
  ];

  readonly mobileActions: MobileCardAction[] = [
    {
      label: 'Facturar', icon: 'bx bx-receipt',
      condition: (sale: Sale) => !sale.facturado,
      callback: (sale: Sale) => this.facturarVenta(sale)
    },
    {
      label: 'Ya facturado', icon: 'bx bx-check-circle',
      condition: (sale: Sale) => !!sale.facturado,
      variant: 'success',
      callback: (_sale: Sale) => {}
    }
  ];


  constructor(private salesService: SalesService, private router: Router, public layoutService: LayoutService, private arcaService: ArcaService) { }

  ngOnInit(): void {
    this.loadSales();

    const today = new Date();
    const thirtyDaysAgo = new Date(today);
    thirtyDaysAgo.setDate(today.getDate() - 30);
    this.invoiceDateTo = today.toISOString().substring(0, 10);
    this.invoiceDateFrom = thirtyDaysAgo.toISOString().substring(0, 10);
  }

  loadSales() {
    this.isLoading = true;
    this.salesService.getAll().subscribe({
      next: (data) => {
        this.sales = data.sort((a, b) => new Date(b.saleDate).getTime() - new Date(a.saleDate).getTime());
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading sales:', err);
        this.isLoading = false;
      }
    });
  }

  facturarVenta(sale: Sale) {
    if (sale.facturado) {
      Swal.fire({
        icon: 'info',
        title: 'Ya facturado',
        text: 'Esta venta ya ha sido facturada previamente.'
      });
      return;
    }

    this.router.navigate(['/facturacion/nueva'], { queryParams: { saleId: sale.saleId } });
  }

  facturarLibre() {
    this.router.navigate(['/facturacion/nueva']);
  }

  get filteredSales() {
    return this.sales.filter(sale =>
      sale.saleId.toString().includes(this.searchTerm) ||
      (sale.createdBy && sale.createdBy.toLowerCase().includes(this.searchTerm.toLowerCase()))
    );
  }



  switchTab(tab: 'ventas' | 'comprobantes') {
    this.activeTab = tab;
    if (tab === 'comprobantes' && this.invoices.length === 0) {
      this.loadInvoices();
    }
  }

  loadInvoices() {
    if (!this.invoiceDateFrom || !this.invoiceDateTo) return;
    this.isLoadingInvoices = true;
    this.arcaService.getInvoices(this.invoiceDateFrom, this.invoiceDateTo).subscribe({
      next: (data) => {
        this.invoices = data;
        this.isLoadingInvoices = false;
      },
      error: (err) => {
        console.error('Error loading invoices:', err);
        this.isLoadingInvoices = false;
      }
    });
  }

  get filteredInvoices() {
    if (!this.invoiceSearchTerm) return this.invoices;
    const term = this.invoiceSearchTerm.toLowerCase();
    return this.invoices.filter(inv =>
      inv.cbteNro.toString().includes(term) ||
      inv.cae?.toLowerCase().includes(term) ||
      inv.impTotal.toString().includes(term)
    );
  }

  viewInvoiceDetail(inv: InvoiceIndex) {
    Swal.fire({
      title: `${this.getCbteTipoLabel(inv.cbteTipo)} ${String(inv.ptoVta).padStart(4, '0')}-${String(inv.cbteNro).padStart(8, '0')}`,
      html: `
        <div style="text-align:left; font-size:0.9rem; line-height:1.6">
          <p><strong>Fecha:</strong> ${inv.cbteFch}</p>
          <p><strong>CUIT Emisor:</strong> ${inv.cuitEmisor}</p>
          <p><strong>CAE:</strong> <code>${inv.cae || '—'}</code></p>
          <p><strong>Vto CAE:</strong> ${inv.caeFchVto || '—'}</p>
          <hr>
          <p><strong>Neto Gravado:</strong> ${this.formatCurrency(inv.impNeto)}</p>
          <p><strong>IVA:</strong> ${this.formatCurrency(inv.impIva)}</p>
          <p><strong>Tributos:</strong> ${this.formatCurrency(inv.impTrib)}</p>
          <p><strong>No Gravado:</strong> ${this.formatCurrency(inv.impTotConc)}</p>
          <p><strong>Exento:</strong> ${this.formatCurrency(inv.impOpEx)}</p>
          <p><strong>Total:</strong> <b>${this.formatCurrency(inv.impTotal)}</b></p>
          ${inv.obsCodes ? `<hr><p><strong>Obs:</strong> [${inv.obsCodes}] ${inv.obsMsg || ''}</p>` : ''}
          ${inv.saleId ? `<p><strong>Venta asociada:</strong> #${inv.saleId}</p>` : ''}
        </div>
      `,
      width: 500,
      confirmButtonText: 'Cerrar'
    });
  }

  getCbteTipoLabel(tipo: number): string {
    switch (tipo) {
      case 1: return 'FC A';
      case 6: return 'FC B';
      case 11: return 'FC C';
      case 2: return 'ND A';
      case 7: return 'ND B';
      case 12: return 'ND C';
      case 3: return 'NC A';
      case 8: return 'NC B';
      case 13: return 'NC C';
      default: return `Tipo ${tipo}`;
    }
  }

  private formatCurrency(val: number): string {
    return new Intl.NumberFormat('es-AR', { style: 'currency', currency: 'ARS' }).format(val ?? 0);
  }
}
