import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface Company {
  companyId: string;
  ssoCode: string;
  businessName?: string;
  tradeName: string;
  cuit?: string;
  country?: string;
  province?: string;
  city?: string;
  companyEmail?: string;
  phone?: string;
  fiscalAddress?: string;
  tenantSchema: string;
  planStatus?: string;
  planTier?: string;
  validUntil?: string;
  graceUntil?: string;
}

export interface CompanyUpdateRequest {
  businessName?: string;
  tradeName?: string;
  cuit?: string;
  country?: string;
  province?: string;
  city?: string;
  companyEmail?: string;
  phone?: string;
  fiscalAddress?: string;
}

@Injectable({
  providedIn: 'root'
})
export class CompanyService {
  private apiUrl = `${environment.apiUrl}/api/companies`;

  constructor(private http: HttpClient) { }

  getMyCompany(): Observable<Company> {
    return this.http.get<Company>(`${this.apiUrl}/me`);
  }

  updateMyCompany(data: CompanyUpdateRequest): Observable<Company> {
    return this.http.put<Company>(`${this.apiUrl}/me`, data);
  }

  updateSchema(): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/api/schema/update`, {});
  }
}
