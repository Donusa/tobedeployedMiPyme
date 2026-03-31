export interface CajaDiariaResponse {
  id: number;
  sucursalId: number;
  sucursalNombre: string;
  fechaOperativa: string;
  estado: string;
  usuarioApertura: string;
  usuarioResponsable: string;
  usuarioCierre?: string;
  fechaHoraApertura: string;
  fechaHoraCierre?: string;
  montoAperturaEfectivo: number;
  montoEsperadoEfectivo?: number;
  montoContadoEfectivo?: number;
  montoDiferencia?: number;
  observacionApertura?: string;
  observacionCierre?: string;
  requiereRevision: boolean;
}

export interface CajaMovimientoResponse {
  id: number;
  tipo: string;
  medioPago: string;
  monto: number;
  descripcion: string;
  justificacion?: string;
  comprobanteTexto?: string;
  comprobanteNumero?: string;
  referenciaTipo?: string;
  referenciaId?: string;
  categoriaEgreso?: string;
  usuarioCreador: string;
  fechaHoraCreacion: string;
  estado: string;
  observaciones?: string;
}

export interface CajaResumenResponse {
  cajaId: number;
  estado: string;
  totalVendido: number;
  totalCobrado: number;
  cantidadVentas: number;
  cantidadEgresos: number;
  cantidadDevoluciones: number;
  cantidadAnulaciones: number;
  resumenPorMedioPago: { [key: string]: number };
  cajaFisica?: CajaFisicaResumen;
}

export interface CajaFisicaResumen {
  aperturaEfectivo: number;
  ingresosEfectivo: number;
  egresosEfectivo: number;
  efectivoEsperado: number;
  efectivoContado?: number;
  diferencia?: number;
}

export interface CajaEgresoCategoria {
  categoriaEgresoId: number;
  nombre: string;
  descripcion?: string;
  activa: boolean;
}

export interface CajaAuditLog {
  auditLogId: number;
  cajaDiariaId: number;
  entidad: string;
  entidadId: number;
  accion: string;
  usuario: string;
  descripcion: string;
  fechaHora: string;
}



export interface AbrirCajaRequest {
  sucursalId: number;
  montoAperturaEfectivo: number;
  observacion?: string;
}

export interface CerrarCajaRequest {
  montoContadoEfectivo: number;
  observacion?: string;
}

export interface RegistrarEgresoRequest {
  categoriaId?: number;
  monto: number;
  medioPago: string;
  justificacion: string;
  comprobanteTexto: string;
  comprobanteNumero?: string;
  observacion?: string;
  tipo?: string;
}

export interface RegistrarIngresoManualRequest {
  monto: number;
  medioPago: string;
  descripcion: string;
  tipo?: string;
}

export interface AnularMovimientoRequest {
  motivo: string;
}



export const MEDIOS_PAGO = [
  { value: 'EFECTIVO', label: 'Efectivo' },
  { value: 'DEBITO', label: 'Débito' },
  { value: 'CREDITO', label: 'Crédito' },
  { value: 'TRANSFERENCIA', label: 'Transferencia' },
  { value: 'QR', label: 'QR' }
];

export const TIPOS_EGRESO = [
  { value: 'GASTO_OPERATIVO', label: 'Gasto operativo' },
  { value: 'PAGO_PROVEEDOR', label: 'Pago a proveedor' },
  { value: 'RETIRO_EFECTIVO', label: 'Retiro de efectivo' }
];

export const ESTADOS_CAJA: { [key: string]: { label: string; class: string } } = {
  ABIERTA: { label: 'Abierta', class: 'badge-success' },
  EN_RELEVO: { label: 'En relevo', class: 'badge-warning' },
  CERRADA: { label: 'Cerrada', class: 'badge-neutral' },
  CERRADA_CON_DIFERENCIA: { label: 'Cerrada c/diferencia', class: 'badge-danger' },
  BLOQUEADA: { label: 'Bloqueada', class: 'badge-danger' },
  ANULADA: { label: 'Anulada', class: 'badge-neutral' }
};

export const TIPOS_MOVIMIENTO: { [key: string]: { label: string; signo: '+' | '-' | '' } } = {
  APERTURA: { label: 'Apertura', signo: '' },
  VENTA: { label: 'Venta', signo: '+' },
  COBRO_CUENTA_CORRIENTE: { label: 'Cobro Cta. Cte.', signo: '+' },
  INGRESO_MANUAL: { label: 'Ingreso manual', signo: '+' },
  REPOSICION_CAMBIO: { label: 'Reposición cambio', signo: '+' },
  AJUSTE_POSITIVO: { label: 'Ajuste (+)', signo: '+' },
  GASTO_OPERATIVO: { label: 'Gasto operativo', signo: '-' },
  PAGO_PROVEEDOR: { label: 'Pago proveedor', signo: '-' },
  RETIRO_EFECTIVO: { label: 'Retiro efectivo', signo: '-' },
  DEVOLUCION_CLIENTE: { label: 'Devolución', signo: '-' },
  AJUSTE_NEGATIVO: { label: 'Ajuste (-)', signo: '-' },
  ANULACION: { label: 'Anulación', signo: '' },
  CIERRE: { label: 'Cierre', signo: '' }
};
