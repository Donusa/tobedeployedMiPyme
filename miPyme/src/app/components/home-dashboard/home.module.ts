import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { HomeDashboardComponent } from './home-dashboard.component';
import { TrendChartComponent } from './trend-chart.component';
import { SharedModule } from '../../shared/shared.module';

@NgModule({
  declarations: [
    HomeDashboardComponent,
    TrendChartComponent
  ],
  imports: [
    CommonModule,
    SharedModule
  ],
  exports: [
    HomeDashboardComponent
  ]
})
export class HomeModule { }
