import { NgModule } from '@angular/core';
import { RouterModule, Routes } from '@angular/router';
import { CajaDashboardComponent } from './components/caja-dashboard/caja-dashboard.component';
import { NewSaleComponent } from './components/new-sale/new-sale.component';

const routes: Routes = [
  { path: '', component: CajaDashboardComponent },
  { path: 'nueva-venta', component: NewSaleComponent }
];

@NgModule({
  imports: [RouterModule.forChild(routes)],
  exports: [RouterModule]
})
export class CajaRoutingModule { }
