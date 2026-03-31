import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { AuthService } from '../../services/auth.service';
import { environment } from '../../../environments/environment';
import {
  CajaDiariaResponse,
  CajaMovimientoResponse,
  CajaResumenResponse,
  CajaEgresoCategoria,
  CajaAuditLog,
  AbrirCajaRequest,
  CerrarCajaRequest,
  RegistrarEgresoRequest,
  RegistrarIngresoManualRequest,
  AnularMovimientoRequest
} from '../models/caja.models';
import { Sale } from '../../sales/models/sale.model';

@Injectable({
  providedIn: 'root'
})
export class CajaService {
  private apiUrl = `${environment.apiUrl}/api/cajas`;

  constructor(private http: HttpClient, private authService: AuthService) { }

  private getHeaders() {
    return {
      'Authorization': `Bearer ${this.authService.getToken()}`
    };
  }



  abrirCaja(request: AbrirCajaRequest): Observable<CajaDiariaResponse> {
    return this.http.post<CajaDiariaResponse>(`${this.apiUrl}/abrir`, request, { headers: this.getHeaders() });
  }

  cerrarCaja(cajaId: number, request: CerrarCajaRequest): Observable<CajaDiariaResponse> {
    return this.http.post<CajaDiariaResponse>(`${this.apiUrl}/${cajaId}/cerrar`, request, { headers: this.getHeaders() });
  }



  obtenerCajaActual(sucursalId: number): Observable<CajaDiariaResponse> {
    return this.http.get<CajaDiariaResponse>(`${this.apiUrl}/actual?sucursalId=${sucursalId}`, { headers: this.getHeaders() });
  }

  obtenerPorId(cajaId: number): Observable<CajaDiariaResponse> {
    return this.http.get<CajaDiariaResponse>(`${this.apiUrl}/${cajaId}`, { headers: this.getHeaders() });
  }

  obtenerResumen(cajaId: number): Observable<CajaResumenResponse> {
    return this.http.get<CajaResumenResponse>(`${this.apiUrl}/${cajaId}/resumen`, { headers: this.getHeaders() });
  }

  obtenerHistorial(sucursalId: number, desde?: string, hasta?: string): Observable<CajaDiariaResponse[]> {
    let params = `sucursalId=${sucursalId}`;
    if (desde) params += `&desde=${desde}`;
    if (hasta) params += `&hasta=${hasta}`;
    return this.http.get<CajaDiariaResponse[]>(`${this.apiUrl}/historial?${params}`, { headers: this.getHeaders() });
  }



  obtenerMovimientos(cajaId: number): Observable<CajaMovimientoResponse[]> {
    return this.http.get<CajaMovimientoResponse[]>(`${this.apiUrl}/${cajaId}/movimientos`, { headers: this.getHeaders() });
  }

  registrarEgreso(cajaId: number, request: RegistrarEgresoRequest): Observable<CajaMovimientoResponse> {
    return this.http.post<CajaMovimientoResponse>(`${this.apiUrl}/${cajaId}/egresos`, request, { headers: this.getHeaders() });
  }

  registrarIngreso(cajaId: number, request: RegistrarIngresoManualRequest): Observable<CajaMovimientoResponse> {
    return this.http.post<CajaMovimientoResponse>(`${this.apiUrl}/${cajaId}/ingresos`, request, { headers: this.getHeaders() });
  }

  anularMovimiento(cajaId: number, movimientoId: number, request: AnularMovimientoRequest): Observable<CajaMovimientoResponse> {
    return this.http.post<CajaMovimientoResponse>(
      `${this.apiUrl}/${cajaId}/movimientos/${movimientoId}/anular`, request, { headers: this.getHeaders() });
  }



  obtenerCategoriasEgreso(): Observable<CajaEgresoCategoria[]> {
    return this.http.get<CajaEgresoCategoria[]>(`${this.apiUrl}/categorias-egreso`, { headers: this.getHeaders() });
  }



  obtenerAuditoria(cajaId: number): Observable<CajaAuditLog[]> {
    return this.http.get<CajaAuditLog[]>(`${this.apiUrl}/${cajaId}/auditoria`, { headers: this.getHeaders() });
  }



  obtenerVentasDeCaja(cajaId: number): Observable<Sale[]> {
    return this.http.get<Sale[]>(`${this.apiUrl}/${cajaId}/ventas`, { headers: this.getHeaders() });
  }
}
