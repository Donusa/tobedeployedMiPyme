import { NgModule } from '@angular/core';
import { SharedModule } from '../shared/shared.module';
import { EmployeesRoutingModule } from './employees-routing.module';
import { EmployeeListComponent } from './components/employee-list/employee-list.component';
import { EmployeeModalComponent } from './components/employee-modal/employee-modal.component';

@NgModule({
  declarations: [
    EmployeeListComponent,
    EmployeeModalComponent
  ],
  imports: [
    SharedModule,
    EmployeesRoutingModule
  ]
})
export class EmployeesModule { }
