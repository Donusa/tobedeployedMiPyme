import { PlanType } from '../services/plan.service';

export interface PlanLimits {

  maxUsers: number;
  maxSucursales: number;
  maxDepositos: number;
  maxCajas: number;
}

export interface PlanDefinition {
  label: string;
  description: string;
  features: string[];
  limits: PlanLimits;
}


export const PLAN_FEATURES: Record<PlanType, PlanDefinition> = {
  base: {
    label: 'Base',
    description: 'Pensado para operar un local.',
    features: [
      'Dashboard básico (ventas del día, ventas del mes, stock bajo, productos más vendidos, estado de caja)',
      'Gestión de stock',
      'Registro de ventas',
      'Administración básica de empleados',
      'Caja diaria',
      'Perfil y cuenta',
      'Auditoría básica (accesos y sesiones)',
      'Límites: 1 sucursal · 1 depósito · hasta 3 usuarios · 1 caja activa',
    ],
    limits: { maxUsers: 3, maxSucursales: 1, maxDepositos: 1, maxCajas: 1 },
  },
  pro: {
    label: 'Pro',
    description: 'Pensado para vender en serio y unificar operación + canales.',
    features: [
      'Todo lo del plan Base',
      'Ofertas y promociones',
      'Gestión de pedidos',
      'Mensajería (WhatsApp, Mercado Libre, Instagram)',
      'Integración Mercado Libre · Tienda Nube · WhatsApp',
      'Facturación electrónica ARCA (emisión de facturas y tickets)',
      'Configuración de certificados ARCA',
      'Métricas intermedias (ventas por período, ventas por canal, top productos, top categorías, stock crítico, desempeño básico por vendedor, clientes frecuentes)',
      'Límites: hasta 2 sucursales · 1 depósito · hasta 5 usuarios · cajas ilimitadas',
    ],
    limits: { maxUsers: 5, maxSucursales: 2, maxDepositos: 1, maxCajas: -1 },
  },
  enterprise: {
    label: 'Enterprise',
    description: 'Pensado para negocios que ya escalan y necesitan control.',
    features: [
      'Todo lo del plan Pro',
      'Métricas avanzadas (rentabilidad por producto, margen por categoría, rotación, productos inmovilizados, comparación entre sucursales, valuación de inventario, ranking por canal, performance avanzada por empleado)',
      'Multi-sucursal real · Multi-depósito',
      'Roles y permisos avanzados por sucursal',
      'Auditoría avanzada y exportable (acciones sensibles + CSV/Excel)',
      'Soporte prioritario · Onboarding asistido',
      'Automatizaciones (próximamente)',
      'Límites: sucursales ilimitadas · depósitos ilimitados · usuarios ilimitados · cajas ilimitadas',
    ],
    limits: { maxUsers: -1, maxSucursales: -1, maxDepositos: -1, maxCajas: -1 },
  },
};
