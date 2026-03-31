import { Injectable } from '@angular/core';
import { HttpClient, HttpHeaders } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

export type OfferStatus = 'DRAFT' | 'ACTIVE' | 'OUT_OF_SYNC' | 'ERROR' | 'REMOVED' | 'ENDED';
export type TnMode = 'VARIANT_PRICE' | 'CART_PROMO';

export interface Offer {
  offerId?: number;
  name: string;
  startDate?: string;
  endDate?: string;
  indefinite: boolean;
  targetType: 'CATEGORY' | 'BRAND' | 'SPECIFIC' | 'WAREHOUSE' | 'LOCATION';
  targetIds: number[];
  discountValue?: number;
  discountType?: 'PERCENTAGE' | 'FIXED_AMOUNT' | 'X_FOR_Y';
  buyQuantity?: number;
  payQuantity?: number;
  publishedTiendaNube?: boolean;
  publishedMercadoLibre?: boolean;
  hasTiendaNubeLinks?: boolean;
  hasMercadoLibreLinks?: boolean;

  status?: OfferStatus;
  tnMode?: TnMode;
  mlPromotionId?: string;
  mlOfferId?: string;
  mlPromotionType?: string;
  tnPromotionId?: string;
  lastSyncedAt?: string;
  errorMessage?: string;
  tnGateReason?: string;
  mlGateReason?: string;
}

export interface OfferActionResult {
  success: boolean;
  offerId?: number;
  channel?: string;
  message?: string;
  reason?: string;
  offer?: Offer;
}

export interface GateCheckResult {
  allowed: boolean;
  reason?: string;
}

@Injectable({
  providedIn: 'root'
})
export class OfferService {
  private apiUrl = `${environment.apiUrl}/api/stock/offers`;

  constructor(private http: HttpClient) { }

  private getHeaders(): HttpHeaders {
    const token = localStorage.getItem('token');
    return new HttpHeaders({
      'Authorization': `Bearer ${token}`
    });
  }

  getAll(): Observable<Offer[]> {
    return this.http.get<Offer[]>(this.apiUrl, { headers: this.getHeaders() });
  }

  getById(id: number): Observable<Offer> {
    return this.http.get<Offer>(`${this.apiUrl}/${id}`, { headers: this.getHeaders() });
  }

  create(offer: Offer): Observable<Offer> {
    return this.http.post<Offer>(this.apiUrl, offer, { headers: this.getHeaders() });
  }

  update(id: number, offer: Offer): Observable<Offer> {
    return this.http.put<Offer>(`${this.apiUrl}/${id}`, offer, { headers: this.getHeaders() });
  }

  delete(id: number): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/${id}`, { headers: this.getHeaders() });
  }

  publishTiendaNube(id: number): Observable<OfferActionResult> {
    return this.http.post<OfferActionResult>(`${this.apiUrl}/${id}/publish/tiendanube`, {}, { headers: this.getHeaders() });
  }

  unpublishTiendaNube(id: number): Observable<OfferActionResult> {
    return this.http.post<OfferActionResult>(`${this.apiUrl}/${id}/unpublish/tiendanube`, {}, { headers: this.getHeaders() });
  }

  publishMercadoLibre(id: number): Observable<OfferActionResult> {
    return this.http.post<OfferActionResult>(`${this.apiUrl}/${id}/publish/mercadolibre`, {}, { headers: this.getHeaders() });
  }

  unpublishMercadoLibre(id: number): Observable<OfferActionResult> {
    return this.http.post<OfferActionResult>(`${this.apiUrl}/${id}/unpublish/mercadolibre`, {}, { headers: this.getHeaders() });
  }

  syncTiendaNube(id: number): Observable<OfferActionResult> {
    return this.http.post<OfferActionResult>(`${this.apiUrl}/${id}/sync/tiendanube`, {}, { headers: this.getHeaders() });
  }

  syncMercadoLibre(id: number): Observable<OfferActionResult> {
    return this.http.post<OfferActionResult>(`${this.apiUrl}/${id}/sync/mercadolibre`, {}, { headers: this.getHeaders() });
  }

  checkGate(id: number, channel: 'TN' | 'ML'): Observable<GateCheckResult> {
    return this.http.get<GateCheckResult>(`${this.apiUrl}/${id}/gate/${channel}`, { headers: this.getHeaders() });
  }
}
