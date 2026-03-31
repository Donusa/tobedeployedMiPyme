import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { Sale, CreateSaleRequest, SalesMetricsResponse } from '../models/sale.model';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SalesService {
  private apiUrl = `${environment.apiUrl}/api/sales`;

  constructor(private http: HttpClient, private authService: AuthService) { }

  private getHeaders() {
    return {
      'Authorization': `Bearer ${this.authService.getToken()}`
    };
  }

  getAll(): Observable<Sale[]> {
    return this.http.get<Sale[]>(this.apiUrl, { headers: this.getHeaders() });
  }

  createSale(request: CreateSaleRequest): Observable<Sale> {
    return this.http.post<Sale>(this.apiUrl, request, { headers: this.getHeaders() });
  }

  getMetrics(range: string, startDate?: string, endDate?: string): Observable<SalesMetricsResponse> {
    let params = `?range=${range}`;
    if (startDate) params += `&startDate=${startDate}`;
    if (endDate) params += `&endDate=${endDate}`;
    return this.http.get<SalesMetricsResponse>(`${this.apiUrl}/metrics${params}`, { headers: this.getHeaders() });
  }

  markAsFacturado(saleId: number): Observable<Sale> {
    return this.http.patch<Sale>(`${this.apiUrl}/${saleId}/facturado`, {}, { headers: this.getHeaders() });
  }
}
