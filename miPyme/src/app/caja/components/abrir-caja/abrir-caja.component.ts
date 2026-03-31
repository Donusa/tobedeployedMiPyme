import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CajaService } from '../../services/caja.service';
import { CajaDiariaResponse } from '../../models/caja.models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-abrir-caja',
  templateUrl: './abrir-caja.component.html',
  styleUrls: ['./abrir-caja.component.css']
})
export class AbrirCajaComponent {
  @Input() sucursalId!: number;
  @Input() sucursalNombre = '';
  @Output() cajaAbierta = new EventEmitter<CajaDiariaResponse>();
  @Output() cerrar = new EventEmitter<void>();

  montoApertura: number | null = 0;
  observacion = '';
  isSubmitting = false;

  constructor(private cajaService: CajaService) { }

  submit() {
    if (this.montoApertura == null || this.montoApertura < 0) {
      Swal.fire('Error', 'El monto de apertura debe ser mayor o igual a 0', 'warning');
      return;
    }

    this.isSubmitting = true;
    this.cajaService.abrirCaja({
      sucursalId: this.sucursalId,
      montoAperturaEfectivo: this.montoApertura,
      observacion: this.observacion || undefined
    }).subscribe({
      next: (caja) => {
        this.isSubmitting = false;
        this.cajaAbierta.emit(caja);
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = err.error || 'Error al abrir caja';
        Swal.fire('Error', typeof msg === 'string' ? msg : JSON.stringify(msg), 'error');
      }
    });
  }
}
