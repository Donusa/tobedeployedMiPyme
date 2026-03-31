import { Component, OnInit } from '@angular/core';
import { StockService } from '../../../services/stock.service';
import { Warehouse } from '../../../models/stock.models';
import Swal from 'sweetalert2';

@Component({
  selector: 'app-warehouse',
  templateUrl: './warehouse.component.html',
  styleUrls: ['./warehouse.component.css']
})
export class WarehouseComponent implements OnInit {
  warehouses: Warehouse[] = [];
  currentWarehouse: Warehouse = { warehouseCode: '', warehouseName: '', address: '' };
  isEditing = false;
  showForm = false;
  isSaving = false;
  isLoading = false;

  constructor(private stockService: StockService) { }

  ngOnInit(): void {
    this.loadWarehouses();
  }

  loadWarehouses() {
    this.isLoading = true;
    this.stockService.getWarehouses().subscribe({
      next: (data) => {
        this.warehouses = data;
        this.isLoading = false;
      },
      error: (err) => {
        this.isLoading = false;
        console.error('Error loading warehouses', err);
        Swal.fire('Error', 'No se pudieron cargar los depósitos', 'error');
      }
    });
  }

  save() {
    if (!this.currentWarehouse.warehouseName || !this.currentWarehouse.warehouseName.trim()) {
      Swal.fire('Atención', 'El nombre del depósito es obligatorio', 'warning');
      return;
    }

    this.isSaving = true;

    if (this.isEditing && this.currentWarehouse.warehouseId) {
      this.stockService.updateWarehouse(this.currentWarehouse.warehouseId, this.currentWarehouse).subscribe({
        next: () => {
          this.isSaving = false;
          this.loadWarehouses();
          this.resetForm();
          Swal.fire({ icon: 'success', title: 'Depósito actualizado', timer: 1500, showConfirmButton: false });
        },
        error: (err) => {
          this.isSaving = false;
          console.error('Error updating warehouse', err);
          const msg = typeof err.error === 'string' ? err.error : 'Error al actualizar el depósito';
          Swal.fire('Error', msg, 'error');
        }
      });
    } else {
      this.stockService.createWarehouse(this.currentWarehouse).subscribe({
        next: () => {
          this.isSaving = false;
          this.loadWarehouses();
          this.resetForm();
          Swal.fire({ icon: 'success', title: 'Depósito creado', timer: 1500, showConfirmButton: false });
        },
        error: (err) => {
          this.isSaving = false;
          console.error('Error creating warehouse', err);
          const msg = typeof err.error === 'string' ? err.error : 'Error al crear el depósito';
          Swal.fire('Error', msg, 'error');
        }
      });
    }
  }

  edit(warehouse: Warehouse) {
    this.currentWarehouse = { ...warehouse };
    this.isEditing = true;
  }

  delete(id: number) {
    Swal.fire({
      title: '¿Eliminar depósito?',
      text: 'Esta acción no se puede deshacer',
      icon: 'warning',
      showCancelButton: true,
      confirmButtonColor: '#d33',
      confirmButtonText: 'Eliminar',
      cancelButtonText: 'Cancelar'
    }).then((result) => {
      if (result.isConfirmed) {
        this.stockService.deleteWarehouse(id).subscribe({
          next: () => this.loadWarehouses(),
          error: (err) => {
            console.error('Error deleting warehouse', err);
            Swal.fire('Error', 'No se pudo eliminar el depósito', 'error');
          }
        });
      }
    });
  }

  resetForm() {
    this.currentWarehouse = { warehouseCode: '', warehouseName: '', address: '' };
    this.isEditing = false;
    this.showForm = false;
  }
}
