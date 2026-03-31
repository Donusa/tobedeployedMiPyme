import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { Routes, RouterModule } from '@angular/router';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { OrdersListComponent } from './components/orders-list/orders-list.component';
import { OrderFormComponent } from './components/order-form/order-form.component';
import { OrderDetailModalComponent } from './components/order-detail-modal/order-detail-modal.component';

const routes: Routes = [
  { path: '', component: OrdersListComponent },
  { path: 'nuevo', component: OrderFormComponent },

];

@NgModule({
  declarations: [
    OrdersListComponent,
    OrderFormComponent,
    OrderDetailModalComponent
  ],
  imports: [
    CommonModule,
    FormsModule,
    ReactiveFormsModule,
    RouterModule.forChild(routes)
  ]
})
export class OrdersModule { }
