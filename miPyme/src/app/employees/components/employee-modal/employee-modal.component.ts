import { Component, EventEmitter, Input, OnInit, Output } from '@angular/core';
import { Employee, EmployeeRequest, PERMISSIONS_LIST } from '../../models/employee.model';
import { EmployeeService } from '../../services/employee.service';
import { StockService } from '../../../services/stock.service';
import { Warehouse } from '../../../models/stock.models';

@Component({
  selector: 'app-employee-modal',
  templateUrl: './employee-modal.component.html',
  styleUrls: ['./employee-modal.component.css']
})
export class EmployeeModalComponent implements OnInit {
  @Input() employee: Employee | null = null;
  @Output() close = new EventEmitter<boolean>();

  name = '';
  email = '';
  password = '';
  selectedPermissions: string[] = [];
  selectedWarehouseIds: number[] = [];
  availablePermissions = PERMISSIONS_LIST;
  warehouses: Warehouse[] = [];
  isLoading = false;
  error = '';

  constructor(private employeeService: EmployeeService, private stockService: StockService) { }

  ngOnInit(): void {
    if (this.employee) {
      this.name = this.employee.name;
      this.email = this.employee.email;
      this.selectedPermissions = [...this.employee.permissions];
      this.selectedWarehouseIds = [...(this.employee.warehouseIds || [])];
    }
    this.stockService.getWarehouses().subscribe({
      next: (data) => this.warehouses = data,
      error: (err) => console.error('Error loading warehouses', err)
    });
  }

  togglePermission(perm: string): void {
    const index = this.selectedPermissions.indexOf(perm);
    if (index >= 0) {
      this.selectedPermissions.splice(index, 1);
    } else {
      this.selectedPermissions.push(perm);
    }
  }

  isPermissionSelected(perm: string): boolean {
    return this.selectedPermissions.includes(perm);
  }

  toggleWarehouse(id: number): void {
    const index = this.selectedWarehouseIds.indexOf(id);
    if (index >= 0) {
      this.selectedWarehouseIds.splice(index, 1);
    } else {
      this.selectedWarehouseIds.push(id);
    }
  }

  isWarehouseSelected(id: number): boolean {
    return this.selectedWarehouseIds.includes(id);
  }

  save(): void {
    if (!this.name || !this.email) {
      this.error = 'Nombre y Email son obligatorios';
      return;
    }
    if (!this.employee && !this.password) {
      this.error = 'La contraseña es obligatoria para nuevos empleados';
      return;
    }

    this.isLoading = true;
    this.error = '';

    const request: EmployeeRequest = {
      name: this.name,
      email: this.email,
      password: this.password,
      permissions: this.selectedPermissions,
      warehouseIds: this.selectedWarehouseIds
    };

    if (this.employee) {
      this.employeeService.updateEmployee(this.employee.id, request).subscribe({
        next: () => {
          this.isLoading = false;
          this.close.emit(true);
        },
        error: (err) => {
          this.isLoading = false;
          this.error = 'Error al actualizar empleado';
          console.error(err);
        }
      });
    } else {
      this.employeeService.createEmployee(request).subscribe({
        next: () => {
          this.isLoading = false;
          this.close.emit(true);
        },
        error: (err) => {
          this.isLoading = false;
          if (err.status === 409) {
            this.error = 'El email ya está registrado';
          } else {
            this.error = 'Error al crear empleado';
          }
          console.error(err);
        }
      });
    }
  }

  cancel(): void {
    this.close.emit(false);
  }
}
