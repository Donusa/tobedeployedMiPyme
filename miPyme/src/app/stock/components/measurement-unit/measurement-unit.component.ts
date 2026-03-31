import { Component, OnInit } from '@angular/core';
import { StockService } from '../../../services/stock.service';
import { MeasurementUnit } from '../../../models/stock.models';

@Component({
  selector: 'app-measurement-unit',
  templateUrl: './measurement-unit.component.html',
  styleUrls: ['./measurement-unit.component.css']
})
export class MeasurementUnitComponent implements OnInit {
  units: MeasurementUnit[] = [];
  currentUnit: MeasurementUnit = { unitCode: '', unitName: '' };
  isEditing = false;

  constructor(private stockService: StockService) { }

  ngOnInit(): void {
    this.loadUnits();
  }

  loadUnits() {
    this.stockService.getMeasurementUnits().subscribe(data => this.units = data);
  }

  save() {
    if (this.isEditing) {
      this.stockService.updateMeasurementUnit(this.currentUnit.unitCode, this.currentUnit).subscribe(() => {
        this.loadUnits();
        this.resetForm();
      });
    } else {
      this.stockService.createMeasurementUnit(this.currentUnit).subscribe(() => {
        this.loadUnits();
        this.resetForm();
      });
    }
  }

  edit(unit: MeasurementUnit) {
    this.currentUnit = { ...unit };
    this.isEditing = true;
  }

  delete(id: string) {
    if(confirm('¿Está seguro de eliminar esta unidad?')) {
        this.stockService.deleteMeasurementUnit(id).subscribe(() => this.loadUnits());
    }
  }

  resetForm() {
    this.currentUnit = { unitCode: '', unitName: '' };
    this.isEditing = false;
  }
}
