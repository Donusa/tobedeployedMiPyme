import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { RouterModule } from '@angular/router';
import { StockMetricsComponent } from '../stock/components/stock-metrics/stock-metrics.component';
import { SpinnerComponent } from './components/spinner/spinner.component';
import { MobileCardListComponent } from './components/mobile-card-list/mobile-card-list.component';
import { ZXingScannerModule } from '@zxing/ngx-scanner';

@NgModule({
  declarations: [
    StockMetricsComponent,
    SpinnerComponent,
    MobileCardListComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule,
    ZXingScannerModule
  ],
  exports: [
    StockMetricsComponent,
    SpinnerComponent,
    MobileCardListComponent,
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    ZXingScannerModule
  ]
})
export class SharedModule { }
