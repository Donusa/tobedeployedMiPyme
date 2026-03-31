import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export interface MercadoLibreConversation {
  id: number;
  sellerId: number;
  buyerId: number;
  status: string;
  substatus: string;
  blocked: boolean;
  lastMessageDate: string;
  unreadCount: number;
  messages?: MercadoLibreMessage[];
}

export interface MercadoLibreMessage {
  id: string;
  fromUserId: number;
  toUserId: number;
  text: string;
  status: string;
  dateCreated: string;
  dateRead: string;
  readByMe: boolean;
}

@Injectable({
  providedIn: 'root'
})
export class MercadoLibreService {
  private apiUrl = `${environment.apiUrl}/api/mercadolibre`;

  constructor(private http: HttpClient) { }

  getConversations(): Observable<MercadoLibreConversation[]> {
    return this.http.get<MercadoLibreConversation[]>(`${this.apiUrl}/conversations`);
  }

  getMessages(conversationId: number, opts?: { limit?: number; before?: string }): Observable<MercadoLibreMessage[]> {
    let url = `${this.apiUrl}/conversations/${conversationId}/messages`;
    const params: any = {};
    if (opts?.limit) params.limit = String(opts.limit);
    if (opts?.before) params.before = opts.before;
    return this.http.get<MercadoLibreMessage[]>(url, { params });
  }

  ensureConversation(conversationId: number): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/conversations/${conversationId}/ensure`, {});
  }

  sendMessage(conversationId: number, text: string): Observable<MercadoLibreMessage> {
    return this.http.post<MercadoLibreMessage>(`${this.apiUrl}/conversations/${conversationId}/messages`, text);
  }

  previewBulkSync(requests: { mlItemId: string, localProductId: number | null, createNew: boolean }[]): Observable<any[]> {
    return this.http.post<any[]>(`${this.apiUrl}/sync/preview-bulk`, requests);
  }

  executeSync(previews: any[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/sync/execute`, previews);
  }

  getOrders(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/orders`);
  }

  getLinkedLocalProducts(): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/products/linked`);
  }

  getShipment(shipmentId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/shipments/${shipmentId}`);
  }

  updateShipment(shipmentId: number, status: string, tracking?: { number: string, url: string }): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/shipments/${shipmentId}`, { status, ...tracking });
  }

  notifySeller(shipmentId: number, trackingNumber: string, trackingUrl: string): Observable<any> {

    return this.http.post<any>(`${this.apiUrl}/shipments/${shipmentId}/seller-notifications`, {
      tracking_number: trackingNumber,
      tracking_url: trackingUrl
    });
  }

  unlinkProducts(productIds: number[]): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/products/unlink`, productIds);
  }

  updateLocalStatus(orderId: string, status: string): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/orders/${orderId}/local-status`, { status });
  }

  publishProduct(localProductId: number, fields: any): Observable<any> {
    return this.http.post<any>(`${this.apiUrl}/products/publish`, { localProductId, fields });
  }

  updatePublishedProduct(localProductId: number, fields: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/products/${localProductId}`, fields);
  }

  getPublishedProduct(localProductId: number): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/products/${localProductId}/published`);
  }

  searchCategories(query: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/categories/search`, { params: { q: query } });
  }

  getCategories(parentId?: string): Observable<any[]> {
    const params: any = {};
    if (parentId) params.parentId = parentId;
    return this.http.get<any[]>(`${this.apiUrl}/categories`, { params });
  }

  getListingTypes(categoryId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/categories/${categoryId}/listing-types`);
  }

  getCategoryAttributes(categoryId: string): Observable<any[]> {
    return this.http.get<any[]>(`${this.apiUrl}/categories/${categoryId}/attributes`);
  }

  updateItemDirect(mlItemId: string, fields: any): Observable<any> {
    return this.http.put<any>(`${this.apiUrl}/items/${mlItemId}`, fields);
  }

  deleteItemDirect(mlItemId: string): Observable<any> {
    return this.http.delete<any>(`${this.apiUrl}/items/${mlItemId}`);
  }

  getConnectionStatus(): Observable<any> {
    return this.http.get<any>(`${this.apiUrl}/status`);
  }
}
