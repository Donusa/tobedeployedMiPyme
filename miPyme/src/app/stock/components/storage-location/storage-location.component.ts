import { Component, OnInit } from '@angular/core';
import { StockService } from '../../../services/stock.service';
import { StorageLocation, Warehouse } from '../../../models/stock.models';

@Component({
  selector: 'app-storage-location',
  templateUrl: './storage-location.component.html',
  styleUrls: ['./storage-location.component.css']
})
export class StorageLocationComponent implements OnInit {
  locations: StorageLocation[] = [];
  warehouses: Warehouse[] = [];
  currentLocation: StorageLocation = this.getEmptyLocation();
  isEditing = false;

  constructor(private stockService: StockService) { }

  ngOnInit(): void {
    this.loadLocations();
    this.loadWarehouses();
  }

  loadLocations() {
    this.stockService.getLocations().subscribe(data => this.locations = data);
  }

  loadWarehouses() {
    this.stockService.getWarehouses().subscribe(data => this.warehouses = data);
  }

  getEmptyLocation(): StorageLocation {
    return {
        locationCode: '',
        warehouse: { warehouseCode: '', warehouseName: '' }
    } as StorageLocation;
  }

  save() {
    if (!this.currentLocation.warehouse || !this.currentLocation.warehouse.warehouseId) {
        alert('Debe seleccionar un depósito');
        return;
    }

    if (this.isEditing && this.currentLocation.storageLocationId) {
      this.stockService.updateLocation(this.currentLocation.storageLocationId, this.currentLocation).subscribe(() => {
        this.loadLocations();
        this.resetForm();
      });
    } else {
      this.stockService.createLocation(this.currentLocation).subscribe(() => {
        this.loadLocations();
        this.resetForm();
      });
    }
  }

  edit(location: StorageLocation) {
    this.currentLocation = { ...location };

    if (this.currentLocation.warehouse) {
        const wh = this.warehouses.find(w => w.warehouseId === this.currentLocation.warehouse.warehouseId);
        if (wh) {
            this.currentLocation.warehouse = wh;
        }
    }
    this.isEditing = true;
  }

  delete(id: number) {
    if(confirm('¿Está seguro de eliminar esta ubicación?')) {
        this.stockService.deleteLocation(id).subscribe(() => this.loadLocations());
    }
  }

  resetForm() {
    this.currentLocation = this.getEmptyLocation();
    this.isEditing = false;
  }
}
