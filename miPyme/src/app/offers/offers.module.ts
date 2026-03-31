import { NgModule } from '@angular/core';
import { CommonModule } from '@angular/common';
import { FormsModule, ReactiveFormsModule } from '@angular/forms';
import { OffersRoutingModule } from './offers-routing.module';
import { OffersListComponent } from './components/offers-list/offers-list.component';
import { OfferFormComponent } from './components/offer-form/offer-form.component';
import { SharedModule } from '../shared/shared.module';

@NgModule({
  declarations: [
    OffersListComponent,
    OfferFormComponent
  ],
  imports: [
    CommonModule,
    OffersRoutingModule,
    FormsModule,
    ReactiveFormsModule,
    SharedModule
  ]
})
export class OffersModule { }
