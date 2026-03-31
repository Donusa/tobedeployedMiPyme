import { Component, Input, Output, EventEmitter } from '@angular/core';
import { CajaService } from '../../services/caja.service';
import { MEDIOS_PAGO } from '../../models/caja.models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-registrar-ingreso',
  templateUrl: './registrar-ingreso.component.html',
  styleUrls: ['./registrar-ingreso.component.css']
})
export class RegistrarIngresoComponent {
  @Input() cajaId!: number;
  @Output() ingresoRegistrado = new EventEmitter<void>();
  @Output() cerrar = new EventEmitter<void>();

  mediosPago = MEDIOS_PAGO;
  tiposIngreso = [
    { value: 'INGRESO_MANUAL', label: 'Ingreso manual' },
    { value: 'REPOSICION_CAMBIO', label: 'Reposición de cambio' }
  ];

  monto: number | null = null;
  medioPago = 'EFECTIVO';
  tipo = 'INGRESO_MANUAL';
  descripcion = '';
  isSubmitting = false;

  constructor(private cajaService: CajaService) { }

  submit() {
    if (!this.monto || this.monto <= 0) {
      Swal.fire('Error', 'El monto debe ser mayor a 0', 'warning');
      return;
    }
    if (!this.descripcion.trim()) {
      Swal.fire('Error', 'La descripción es obligatoria', 'warning');
      return;
    }

    this.isSubmitting = true;
    this.cajaService.registrarIngreso(this.cajaId, {
      monto: this.monto,
      medioPago: this.medioPago,
      descripcion: this.descripcion,
      tipo: this.tipo
    }).subscribe({
      next: () => {
        this.isSubmitting = false;
        Swal.fire('Ingreso registrado', 'El ingreso se registró correctamente', 'success');
        this.ingresoRegistrado.emit();
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = err.error || 'Error al registrar ingreso';
        Swal.fire('Error', typeof msg === 'string' ? msg : JSON.stringify(msg), 'error');
      }
    });
  }
}
