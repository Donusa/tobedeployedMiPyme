import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule } from '@angular/forms';

import { CajaRoutingModule } from './caja-routing.module';
import { SharedModule } from '../shared/shared.module';
import { CajaDashboardComponent } from './components/caja-dashboard/caja-dashboard.component';
import { AbrirCajaComponent } from './components/abrir-caja/abrir-caja.component';
import { CerrarCajaComponent } from './components/cerrar-caja/cerrar-caja.component';
import { RegistrarEgresoComponent } from './components/registrar-egreso/registrar-egreso.component';
import { RegistrarIngresoComponent } from './components/registrar-ingreso/registrar-ingreso.component';
import { MovimientosListComponent } from './components/movimientos-list/movimientos-list.component';
import { NewSaleComponent } from './components/new-sale/new-sale.component';
import { InvoicePreviewModalComponent } from './components/invoice-preview-modal/invoice-preview-modal.component';

@NgModule({
  declarations: [
    CajaDashboardComponent,
    AbrirCajaComponent,
    CerrarCajaComponent,
    RegistrarEgresoComponent,
    RegistrarIngresoComponent,
    MovimientosListComponent,
    NewSaleComponent,
    InvoicePreviewModalComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    SharedModule,
    CajaRoutingModule
  ]
})
export class CajaModule { }
