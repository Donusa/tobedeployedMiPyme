import { Component, OnInit } from '@angular/core';
import { Router } from '@angular/router';
import { CajaService } from '../../services/caja.service';
import { StockService } from '../../../services/stock.service';
import { AuthService } from '../../../services/auth.service';
import { LayoutService } from '../../../services/layout.service';
import { Warehouse } from '../../../models/stock.models';
import {
  CajaDiariaResponse,
  CajaResumenResponse,
  CajaMovimientoResponse,
  ESTADOS_CAJA
} from '../../models/caja.models';
import { Sale } from '../../../sales/models/sale.model';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-caja-dashboard',
  templateUrl: './caja-dashboard.component.html',
  styleUrls: ['./caja-dashboard.component.css']
})
export class CajaDashboardComponent implements OnInit {

  sucursales: Warehouse[] = [];
  sucursalSeleccionada: number | null = null;
  cajaActual: CajaDiariaResponse | null = null;
  resumen: CajaResumenResponse | null = null;
  movimientos: CajaMovimientoResponse[] = [];
  historial: CajaDiariaResponse[] = [];
  ventas: Sale[] = [];
  isLoading = false;


  showAbrirCaja = false;
  showCerrarCaja = false;
  showRegistrarEgreso = false;
  showRegistrarIngreso = false;
  showMovimientos = false;
  showHistorial = false;


  activeTab: 'resumen' | 'movimientos' | 'ventas' | 'historial' = 'resumen';

  readonly estadosCaja = ESTADOS_CAJA;

  isAdmin = false;

  constructor(
    private cajaService: CajaService,
    private stockService: StockService,
    private authService: AuthService,
    public layoutService: LayoutService,
    private router: Router
  ) { }

  ngOnInit(): void {
    this.isAdmin = this.authService.getRole() === 'ADMIN';
    this.loadSucursales();
  }

  loadSucursales() {
    this.isLoading = true;
    this.stockService.getWarehouses().subscribe({
      next: (data) => {
        this.sucursales = data;
        if (this.sucursales.length === 1) {
          this.sucursalSeleccionada = this.sucursales[0].warehouseId!;
          this.onSucursalChange();
        } else {
          this.isLoading = false;
        }
      },
      error: () => {
        this.isLoading = false;
        Swal.fire('Error', 'No se pudieron cargar las sucursales', 'error');
      }
    });
  }

  onSucursalChange() {
    if (!this.sucursalSeleccionada) {
      this.cajaActual = null;
      this.resumen = null;
      return;
    }
    this.loadCajaActual();
  }

  loadCajaActual() {
    if (!this.sucursalSeleccionada) return;
    this.isLoading = true;
    this.cajaService.obtenerCajaActual(this.sucursalSeleccionada).subscribe({
      next: (caja) => {
        this.cajaActual = caja;
        this.isLoading = false;
        if (caja) {
          this.loadResumen();
          this.loadMovimientos();
          this.loadVentas();
        }
      },
      error: (err) => {

        if (err.status === 204 || err.status === 0) {
          this.cajaActual = null;
          this.resumen = null;
          this.movimientos = [];
        }
        this.isLoading = false;
      }
    });
  }

  loadResumen() {
    if (!this.cajaActual) return;
    this.cajaService.obtenerResumen(this.cajaActual.id).subscribe({
      next: (data) => this.resumen = data,
      error: () => { }
    });
  }

  loadMovimientos() {
    if (!this.cajaActual) return;
    this.cajaService.obtenerMovimientos(this.cajaActual.id).subscribe({
      next: (data) => this.movimientos = data,
      error: () => { }
    });
  }

  loadVentas() {
    if (!this.cajaActual) return;
    this.cajaService.obtenerVentasDeCaja(this.cajaActual.id).subscribe({
      next: (data) => this.ventas = data,
      error: () => { }
    });
  }

  loadHistorial() {
    if (!this.sucursalSeleccionada) return;
    this.cajaService.obtenerHistorial(this.sucursalSeleccionada).subscribe({
      next: (data) => this.historial = data,
      error: () => { }
    });
  }



  onAbrirCaja() {
    this.showAbrirCaja = true;
  }

  onCerrarCaja() {
    this.showCerrarCaja = true;
  }

  onRegistrarEgreso() {
    this.showRegistrarEgreso = true;
  }

  onRegistrarIngreso() {
    this.showRegistrarIngreso = true;
  }

  onNuevaVenta() {
    this.router.navigate(['/caja/nueva-venta'], {
      queryParams: { cajaId: this.cajaActual!.id }
    });
  }



  onCajaAbierta(caja: CajaDiariaResponse) {
    this.showAbrirCaja = false;
    this.cajaActual = caja;
    this.loadResumen();
    this.loadMovimientos();
    Swal.fire('Caja abierta', 'La caja se abrió correctamente', 'success');
  }

  onCajaCerrada(caja: CajaDiariaResponse) {
    this.showCerrarCaja = false;
    this.cajaActual = null;
    this.resumen = null;
    this.movimientos = [];
    this.ventas = [];
    Swal.fire('Caja cerrada', 'La caja se cerró correctamente', 'success');
  }

  onEgresoRegistrado() {
    this.showRegistrarEgreso = false;
    this.loadResumen();
    this.loadMovimientos();
  }

  onIngresoRegistrado() {
    this.showRegistrarIngreso = false;
    this.loadResumen();
    this.loadMovimientos();
  }

  onMovimientoAnulado() {
    this.loadResumen();
    this.loadMovimientos();
  }

  onCerrar() {
    this.showAbrirCaja = false;
    this.showCerrarCaja = false;
    this.showRegistrarEgreso = false;
    this.showRegistrarIngreso = false;
  }

  onTabChange(tab: 'resumen' | 'movimientos' | 'ventas' | 'historial') {
    this.activeTab = tab;
    if (tab === 'historial' && this.historial.length === 0) {
      this.loadHistorial();
    }
    if (tab === 'ventas') {
      this.loadVentas();
    }
  }

  getEstadoLabel(estado: string): string {
    return this.estadosCaja[estado]?.label || estado;
  }

  getEstadoClass(estado: string): string {
    return this.estadosCaja[estado]?.class || 'badge-neutral';
  }

  getSucursalNombre(): string {
    const s = this.sucursales.find(s => s.warehouseId === this.sucursalSeleccionada);
    return s?.warehouseName || '';
  }

  getTotalVentas(): number {
    return this.ventas.reduce((sum, v) => sum + (v.totalAmount || 0), 0);
  }

  private readonly medioPagoLabels: Record<string, string> = {
    EFECTIVO: 'Efectivo',
    DEBITO: 'Débito',
    CREDITO: 'Crédito',
    TRANSFERENCIA: 'Transferencia',
    QR: 'QR',
    CUENTA_CORRIENTE: 'Cuenta Corriente',
    MIXTO: 'Mixto',
    SIN_DEFINIR: 'Sin definir'
  };

  getMedioPagoLabel(key: string): string {
    return this.medioPagoLabels[key] || key;
  }
}
