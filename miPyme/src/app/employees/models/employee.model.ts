export interface Employee {
  id: number;
  name: string;
  email: string;
  permissions: string[];
  warehouseIds: number[];
}

export interface EmployeeRequest {
  name: string;
  email: string;
  password?: string;
  permissions: string[];
  warehouseIds: number[];
}

export const PERMISSIONS_LIST = [
  'metricas',
  'stock',
  'ventas',
  'empleados',
  'mensajeria',
  'mercadolibre',
  'tiendanube',
  'arca',
  'caja'
];
