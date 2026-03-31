import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CajaService } from '../../services/caja.service';
import { CajaMovimientoResponse, TIPOS_MOVIMIENTO } from '../../models/caja.models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-movimientos-list',
  templateUrl: './movimientos-list.component.html',
  styleUrls: ['./movimientos-list.component.css']
})
export class MovimientosListComponent {
  @Input() movimientos: CajaMovimientoResponse[] = [];
  @Input() cajaId!: number;
  @Input() cajaAbierta = false;
  @Input() isAdmin = false;
  @Output() movimientoAnulado = new EventEmitter<void>();

  readonly tiposMovimiento = TIPOS_MOVIMIENTO;

  constructor(private cajaService: CajaService) { }

  getTipoLabel(tipo: string): string {
    return this.tiposMovimiento[tipo]?.label || tipo;
  }

  getSigno(tipo: string): string {
    return this.tiposMovimiento[tipo]?.signo || '';
  }

  canAnular(mov: CajaMovimientoResponse): boolean {
    return this.cajaAbierta
      && mov.estado === 'ACTIVO'
      && mov.tipo !== 'APERTURA'
      && mov.tipo !== 'CIERRE'
      && mov.tipo !== 'ANULACION';
  }

  anular(mov: CajaMovimientoResponse) {
    Swal.fire({
      title: '¿Anular movimiento?',
      html: `<p>${this.getTipoLabel(mov.tipo)}: <strong>$${mov.monto.toFixed(2)}</strong></p>
             <p>${mov.descripcion}</p>`,
      input: 'text',
      inputLabel: 'Motivo de anulación',
      inputPlaceholder: 'Ingresá el motivo...',
      inputValidator: (value) => {
        if (!value || !value.trim()) return 'El motivo es obligatorio';
        return null;
      },
      icon: 'warning',
      showCancelButton: true,
      confirmButtonText: 'Anular',
      cancelButtonText: 'Cancelar',
      confirmButtonColor: '#ef4444'
    }).then(result => {
      if (result.isConfirmed && result.value) {
        this.cajaService.anularMovimiento(this.cajaId, mov.id, { motivo: result.value }).subscribe({
          next: () => {
            Swal.fire('Anulado', 'El movimiento fue anulado', 'success');
            this.movimientoAnulado.emit();
          },
          error: (err) => {
            const msg = err.error || 'Error al anular';
            Swal.fire('Error', typeof msg === 'string' ? msg : JSON.stringify(msg), 'error');
          }
        });
      }
    });
  }
}
