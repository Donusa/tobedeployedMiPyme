import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { StockRoutingModule } from './stock-routing.module';
import { StockComponent } from './stock.component';
import { MeasurementUnitComponent } from './components/measurement-unit/measurement-unit.component';
import { ProductBrandComponent } from './components/product-brand/product-brand.component';
import { ProductCategoryComponent } from './components/product-category/product-category.component';
import { WarehouseComponent } from './components/warehouse/warehouse.component';
import { StorageLocationComponent } from './components/storage-location/storage-location.component';
import { ProductComponent } from './components/product/product.component';
import { InventoryComponent } from './components/inventory/inventory.component';
import { ProductFormComponent } from './components/product-form/product-form.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  declarations: [
    StockComponent,
    MeasurementUnitComponent,
    ProductBrandComponent,
    ProductCategoryComponent,
    WarehouseComponent,
    StorageLocationComponent,
    ProductComponent,
    InventoryComponent,
    ProductFormComponent
  ],
  imports: [
    CommonModule,
    StockRoutingModule,
    FormsModule,
    SharedModule
  ]
})
export class StockModule { }
