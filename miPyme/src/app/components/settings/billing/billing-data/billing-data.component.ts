import { Component, OnInit } from '@angular/core';
import { CompanyService, Company, CompanyUpdateRequest } from '../../../../services/company.service';

@Component({
    selector: 'app-billing-data',
    templateUrl: './billing-data.component.html',
    styleUrls: ['./billing-data.component.css']
})
export class BillingDataComponent implements OnInit {
    companyData: Partial<Company> = {};
    isLoading = true;
    isSaving = false;
    saveSuccess = false;
    saveError = '';

    constructor(private companyService: CompanyService) { }

    ngOnInit(): void {
        this.companyService.getMyCompany().subscribe({
            next: (data: Company) => {
                this.companyData = data;
                this.isLoading = false;
            },
            error: (err: any) => {
                console.error('Error fetching company data', err);
                this.isLoading = false;
            }
        });
    }

    save(): void {
        this.isSaving = true;
        this.saveSuccess = false;
        this.saveError = '';

        const request: CompanyUpdateRequest = {
            businessName: this.companyData.businessName,
            cuit: this.companyData.cuit,
            fiscalAddress: this.companyData.fiscalAddress,
            city: this.companyData.city,
            province: this.companyData.province
        };

        this.companyService.updateMyCompany(request).subscribe({
            next: (updated: Company) => {
                this.companyData = updated;
                this.isSaving = false;
                this.saveSuccess = true;
                setTimeout(() => this.saveSuccess = false, 3000);
            },
            error: (err: any) => {
                this.isSaving = false;
                this.saveError = 'Error al guardar los datos. Intentá de nuevo.';
                setTimeout(() => this.saveError = '', 5000);
            }
        });
    }
}
