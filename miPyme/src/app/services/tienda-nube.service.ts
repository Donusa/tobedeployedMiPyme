import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class TiendaNubeService {
  private apiUrl = `${environment.apiUrl}/api/tiendanube`;

  constructor(private http: HttpClient) { }

  exchangeToken(code: string, redirectUri?: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/token`, { code, redirectUri });
  }

  getConnectionStatus(): Observable<any> {
    return this.http.get(`${this.apiUrl}/status`);
  }

  getProducts(storeId: number, accessToken: string): Observable<any[]> {
    const headers = {
      'X-TiendaNube-Access-Token': accessToken
    };
    return this.http.get<any[]>(`${this.apiUrl}/${storeId}/products`, { headers });
  }

  previewSync(productIds: number[]): Observable<any[]> {
    return this.http.post<any[]>(`${this.apiUrl}/sync/preview`, productIds);
  }

  previewManualMatch(tnProductId: number, localProductId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/sync/preview-manual`, { tnProductId, localProductId });
  }

  previewBulkSync(requests: { tnProductId: number, localProductId: number | null, variantMapping?: any }[]): Observable<any[]> {
    return this.http.post<any[]>(`${this.apiUrl}/sync/preview-bulk`, requests);
  }

  executeSync(resolvedPreviews: any[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/sync/execute`, resolvedPreviews);
  }

  getLinkedLocalProducts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/products/linked`);
  }

  getOrders(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/orders`);
  }

  getFulfillmentOrders(orderId: number): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/orders/${orderId}/fulfillment-orders`);
  }

  updateFulfillmentOrder(orderId: number, fulfillmentId: number, data: any): Observable<any> {
    return this.http.patch<any>(`${this.apiUrl}/orders/${orderId}/fulfillment-orders/${fulfillmentId}`, data);
  }

  closeOrder(orderId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/orders/${orderId}/close`, {});
  }

  updateLocalStatus(orderId: number, status: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/orders/${orderId}/local-status`, { status });
  }

  unlinkProducts(productIds: number[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/products/unlink`, productIds);
  }

  publishProduct(localProductId: number, fields: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/products/publish`, { localProductId, fields });
  }

  updatePublishedProduct(localProductId: number, fields: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/products/${localProductId}`, fields);
  }

  markOrderAsPaid(orderId: number): Observable<void> {
    const url = `${this.apiUrl}/orders/${orderId}/mark-paid`;
    console.log('TiendaNubeService: Sending POST to', url);
    return this.http.post<void>(url, {});
  }

  getPublishedProduct(localProductId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/products/${localProductId}/published`);
  }
}
