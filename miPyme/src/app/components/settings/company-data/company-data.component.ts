import { Component, OnInit } from '@angular/core';
import { CompanyService, Company, CompanyUpdateRequest } from '../../../services/company.service';

@Component({
  selector: 'app-company-data',
  templateUrl: './company-data.component.html',
  styleUrls: ['./company-data.component.css']
})
export class CompanyDataComponent implements OnInit {
  company: Company | null = null;
  isLoading = false;
  isSaving = false;
  successMessage = '';
  errorMessage = '';


  formData: CompanyUpdateRequest = {};

  constructor(private companyService: CompanyService) { }

  ngOnInit(): void {
    this.loadCompany();
  }

  loadCompany() {
    this.isLoading = true;
    this.errorMessage = '';
    this.companyService.getMyCompany().subscribe({
      next: (data) => {
        this.company = data;
        this.formData = {
          businessName: data.businessName,
          tradeName: data.tradeName,
          cuit: data.cuit,
          country: data.country,
          province: data.province,
          city: data.city,
          companyEmail: data.companyEmail,
          phone: data.phone,
          fiscalAddress: data.fiscalAddress
        };
        this.isLoading = false;
      },
      error: (err) => {
        console.error('Error loading company data', err);
        this.errorMessage = 'Error al cargar los datos de la empresa.';
        this.isLoading = false;
      }
    });
  }

  onSubmit() {
    this.isSaving = true;
    this.successMessage = '';
    this.errorMessage = '';

    this.companyService.updateMyCompany(this.formData).subscribe({
      next: (data) => {
        this.company = data;
        this.successMessage = 'Datos actualizados correctamente.';
        this.isSaving = false;
        setTimeout(() => this.successMessage = '', 3000);
      },
      error: (err) => {
        console.error('Error updating company data', err);
        if (err.status === 409) {
            this.errorMessage = 'El CUIT ya está registrado por otra empresa.';
        } else {
            this.errorMessage = 'Error al guardar los cambios.';
        }
        this.isSaving = false;
      }
    });
  }
}
