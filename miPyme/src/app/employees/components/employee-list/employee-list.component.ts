import { Component, OnInit } from '@angular/core';
import { EmployeeService } from '../../services/employee.service';
import { Employee } from '../../models/employee.model';
import { LayoutService } from '../../../services/layout.service';
import { StockService } from '../../../services/stock.service';
import { Warehouse } from '../../../models/stock.models';
import { MobileCardColumn, MobileCardAction } from '../../../shared/components/mobile-card-list/mobile-card-list.models';

@Component({
  selector: 'app-employee-list',
  templateUrl: './employee-list.component.html',
  styleUrls: ['./employee-list.component.css']
})
export class EmployeeListComponent implements OnInit {
  employees: Employee[] = [];
  warehouses: Warehouse[] = [];
  warehouseMap: Record<number, string> = {};
  isLoading = false;
  showModal = false;
  selectedEmployee: Employee | null = null;


  readonly mobileColumns: MobileCardColumn[] = [
    { field: 'name',  label: 'Nombre', isPrimary: true },
    { field: 'email', label: 'Email' },
    {
      field: 'permissions', label: 'Permisos',
      format: (v: string[]) => (v && v.length > 0) ? v.join(', ') : 'Sin permisos'
    },
    {
      field: 'warehouseIds', label: 'Sucursales',
      format: (v: number[]) => this.getWarehouseNames(v)
    }
  ];

  readonly mobileActions: MobileCardAction[] = [
    { label: 'Editar',    icon: 'bx bx-edit',       callback: (e: Employee) => this.openEditModal(e) },
    { label: 'Eliminar', icon: 'bx bx-trash',        variant: 'danger', callback: (e: Employee) => this.deleteEmployee(e.id) }
  ];


  constructor(private employeeService: EmployeeService, private stockService: StockService, public layoutService: LayoutService) { }

  ngOnInit(): void {
    this.stockService.getWarehouses().subscribe({
      next: (data) => {
        this.warehouses = data;
        this.warehouseMap = {};
        data.forEach(w => { if (w.warehouseId) this.warehouseMap[w.warehouseId] = w.warehouseName; });
        this.loadEmployees();
      },
      error: () => this.loadEmployees()
    });
  }

  getWarehouseNames(ids: number[]): string {
    if (!ids || ids.length === 0) return 'Sin sucursales';
    return ids.map(id => this.warehouseMap[id] || `#${id}`).join(', ');
  }

  loadEmployees(): void {
    this.isLoading = true;
    this.employeeService.getEmployees().subscribe({
      next: (data) => {
        this.employees = data;
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading employees', err);
        this.isLoading = false;
      }
    });
  }

  openCreateModal(): void {
    this.selectedEmployee = null;
    this.showModal = true;
  }

  openEditModal(employee: Employee): void {
    this.selectedEmployee = employee;
    this.showModal = true;
  }

  deleteEmployee(id: number): void {
    if (confirm('¿Está seguro de eliminar este empleado?')) {
      this.isLoading = true;
      this.employeeService.deleteEmployee(id).subscribe({
        next: () => {
          this.loadEmployees();
        },
        error: (err) => {
          console.error('Error deleting employee', err);
          this.isLoading = false;
        }
      });
    }
  }

  onModalClose(refresh: boolean): void {
    this.showModal = false;
    this.selectedEmployee = null;
    if (refresh) {
      this.loadEmployees();
    }
  }
}
