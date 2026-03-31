import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CajaService } from '../../services/caja.service';
import { CajaDiariaResponse, CajaResumenResponse } from '../../models/caja.models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-cerrar-caja',
  templateUrl: './cerrar-caja.component.html',
  styleUrls: ['./cerrar-caja.component.css']
})
export class CerrarCajaComponent implements OnInit {
  @Input() caja!: CajaDiariaResponse;
  @Input() resumen: CajaResumenResponse | null = null;
  @Output() cajaCerrada = new EventEmitter<CajaDiariaResponse>();
  @Output() cerrar = new EventEmitter<void>();

  montoContado: number | null = null;
  observacion = '';
  isSubmitting = false;

  efectivoEsperado = 0;
  diferencia: number | null = null;

  constructor(private cajaService: CajaService) { }

  ngOnInit() {
    if (this.resumen?.cajaFisica) {
      this.efectivoEsperado = this.resumen.cajaFisica.efectivoEsperado;
    }
  }

  calcularDiferencia() {
    if (this.montoContado != null) {
      this.diferencia = this.montoContado - this.efectivoEsperado;
    } else {
      this.diferencia = null;
    }
  }

  submit() {
    if (this.montoContado == null || this.montoContado < 0) {
      Swal.fire('Error', 'Ingresá el monto contado de efectivo', 'warning');
      return;
    }

    this.calcularDiferencia();

    if (this.diferencia !== 0 && (!this.observacion || this.observacion.trim() === '')) {
      Swal.fire('Observación requerida', 'Hay una diferencia de $' + this.diferencia?.toFixed(2) + '. Debés ingresar una observación.', 'warning');
      return;
    }

    Swal.fire({
      title: '¿Cerrar caja?',
      html: `<p>Efectivo esperado: <strong>$${this.efectivoEsperado.toFixed(2)}</strong></p>
             <p>Efectivo contado: <strong>$${this.montoContado.toFixed(2)}</strong></p>
             <p>Diferencia: <strong class="${this.diferencia !== 0 ? 'text-danger' : ''}">$${this.diferencia?.toFixed(2)}</strong></p>`,
      icon: 'question',
      showCancelButton: true,
      confirmButtonText: 'Sí, cerrar',
      cancelButtonText: 'Cancelar'
    }).then(result => {
      if (result.isConfirmed) {
        this.doSubmit();
      }
    });
  }

  private doSubmit() {
    this.isSubmitting = true;
    this.cajaService.cerrarCaja(this.caja.id, {
      montoContadoEfectivo: this.montoContado!,
      observacion: this.observacion || undefined
    }).subscribe({
      next: (caja) => {
        this.isSubmitting = false;
        this.cajaCerrada.emit(caja);
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = err.error || 'Error al cerrar caja';
        Swal.fire('Error', typeof msg === 'string' ? msg : JSON.stringify(msg), 'error');
      }
    });
  }
}
