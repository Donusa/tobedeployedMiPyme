import { Component, Input, Output, EventEmitter, OnInit } from '@angular/core';
import { CajaService } from '../../services/caja.service';
import { CajaEgresoCategoria, MEDIOS_PAGO, TIPOS_EGRESO } from '../../models/caja.models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-registrar-egreso',
  templateUrl: './registrar-egreso.component.html',
  styleUrls: ['./registrar-egreso.component.css']
})
export class RegistrarEgresoComponent implements OnInit {
  @Input() cajaId!: number;
  @Output() egresoRegistrado = new EventEmitter<void>();
  @Output() cerrar = new EventEmitter<void>();

  categorias: CajaEgresoCategoria[] = [];
  mediosPago = MEDIOS_PAGO;
  tiposEgreso = TIPOS_EGRESO;

  categoriaId: number | null = null;
  monto: number | null = null;
  medioPago = 'EFECTIVO';
  tipo = 'GASTO_OPERATIVO';
  justificacion = '';
  comprobanteTexto = '';
  comprobanteNumero = '';
  observacion = '';
  isSubmitting = false;

  constructor(private cajaService: CajaService) { }

  ngOnInit() {
    this.cajaService.obtenerCategoriasEgreso().subscribe({
      next: (data) => this.categorias = data,
      error: () => { }
    });
  }

  submit() {
    if (!this.monto || this.monto <= 0) {
      Swal.fire('Error', 'El monto debe ser mayor a 0', 'warning');
      return;
    }
    if (!this.justificacion.trim()) {
      Swal.fire('Error', 'La justificación es obligatoria', 'warning');
      return;
    }
    if (!this.comprobanteTexto.trim()) {
      Swal.fire('Error', 'La descripción del comprobante es obligatoria', 'warning');
      return;
    }

    this.isSubmitting = true;
    this.cajaService.registrarEgreso(this.cajaId, {
      categoriaId: this.categoriaId || undefined,
      monto: this.monto,
      medioPago: this.medioPago,
      justificacion: this.justificacion,
      comprobanteTexto: this.comprobanteTexto,
      comprobanteNumero: this.comprobanteNumero || undefined,
      observacion: this.observacion || undefined,
      tipo: this.tipo
    }).subscribe({
      next: () => {
        this.isSubmitting = false;
        Swal.fire('Egreso registrado', 'El egreso se registró correctamente', 'success');
        this.egresoRegistrado.emit();
      },
      error: (err) => {
        this.isSubmitting = false;
        const msg = err.error || 'Error al registrar egreso';
        Swal.fire('Error', typeof msg === 'string' ? msg : JSON.stringify(msg), 'error');
      }
    });
  }
}
