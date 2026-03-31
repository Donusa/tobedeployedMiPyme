import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { SalesRoutingModule } from './sales-routing.module';
import { SalesListComponent } from './components/list/sales-list.component';
import { SaleModalComponent } from './components/sale-modal/sale-modal.component';
import { SaleDetailModalComponent } from './components/sale-detail-modal/sale-detail-modal.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  declarations: [
    SalesListComponent,
    SaleModalComponent,
    SaleDetailModalComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    SalesRoutingModule
  ],
  exports: [
    SalesListComponent
  ]
})
export class SalesModule {
  constructor() {
    console.log('SalesModule loaded');
  }
}
