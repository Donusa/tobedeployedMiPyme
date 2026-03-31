import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable, map, catchError } from 'rxjs';
import { environment } from '../../environments/environment';
import { AuthService } from './auth.service';
import { Router } from '@angular/router';

export interface InvoiceResponse {
  success: boolean;
  resultado: string;
  cae: string;
  caeFchVto: string;
  cbteDesde: number;
  cbteHasta: number;
  puntoVenta: number;
  tipoComprobante: number;
  cbteFecha: string;
  observaciones: string[];
  errores: string[];
  invoiceIndexId: number | null;
}

export interface InvoiceIndex {
  id: number;
  cuitEmisor: string;
  ptoVta: number;
  cbteTipo: number;
  cbteNro: number;
  cbteFch: string;
  impTotal: number;
  impNeto: number;
  impIva: number;
  impTrib: number;
  impTotConc: number;
  impOpEx: number;
  cae: string;
  caeFchVto: string;
  resultado: string;
  obsCodes: string;
  obsMsg: string;
  saleId: number | null;
  createdAt: string;
}

export interface InvoiceDetailResponse {
  success: boolean;
  concepto: number;
  docTipo: number;
  docNro: string;
  cbteNro: number;
  cbteFch: string;
  impTotal: number;
  impTotConc: number;
  impNeto: number;
  impOpEx: number;
  impTrib: number;
  impIVA: number;
  monId: string;
  monCotiz: number;
  cae: string;
  caeFchVto: string;
  resultado: string;
  fchServDesde: string;
  fchServHasta: string;
  fchVtoPago: string;
  fchProceso: string;
  ptoVta: number;
  cbteTipo: number;
  ivaDetails: { id: number; baseImp: number; importe: number }[];
  tributoDetails: { id: number; desc: string; baseImp: number; alic: number; importe: number }[];
  errores: string[];
}

@Injectable({
  providedIn: 'root'
})
export class ArcaService {
  private apiUrl = `${environment.apiUrl}/api/wsfe`;

  constructor(private http: HttpClient, private authService: AuthService, private router: Router) { }

  private getHeaders() {
    return {
      'Authorization': `Bearer ${this.authService.getToken()}`
    };
  }


  getParam(type: string): Observable<any[]> {
    return this.http.get(`${this.apiUrl}/params/${type}`, {
      headers: this.getHeaders(),
      responseType: 'text'
    }).pipe(
      map(xml => this.parseSoapResponse(xml, type)),
      catchError(err => {
        if (err.status === 500 && (err.error?.includes('No hay un token válido') || err.error?.includes('decrypt'))) {

           this.router.navigate(['/configuracion/integraciones/arca']);
           throw new Error('Sesión AFIP caducada o inválida. Por favor renueve el certificado.');
        }
        throw err;
      })
    );
  }

  getLastVoucher(ptoVta: number, cbteTipo: number): Observable<number> {
    return this.http.get(`${this.apiUrl}/last-voucher?ptoVta=${ptoVta}&cbteTipo=${cbteTipo}`, {
      headers: this.getHeaders(),
      responseType: 'text'
    }).pipe(
      map(xml => {
        const match = xml.match(/<CbteNro>(\d+)<\/CbteNro>/);
        return match ? parseInt(match[1], 10) : 0;
      }),
      catchError(err => {
        if (err.status === 500 && (err.error?.includes('No hay un token válido') || err.error?.includes('decrypt'))) {
           this.router.navigate(['/configuracion/integraciones/arca']);
           throw new Error('Sesión AFIP caducada o inválida. Por favor renueve el certificado.');
        }
        throw err;
      })
    );
  }

  getCotizacion(monId: string): Observable<number> {
    return this.http.get(`${this.apiUrl}/cotizacion?monId=${monId}`, {
      headers: this.getHeaders(),
      responseType: 'text'
    }).pipe(
      map(xml => {
        const match = xml.match(/<MonCotiz>([\d.]+)<\/MonCotiz>/);
        return match ? parseFloat(match[1]) : 1;
      }),
      catchError(err => {
        if (err.status === 500 && (err.error?.includes('No hay un token válido') || err.error?.includes('decrypt'))) {
           this.router.navigate(['/configuracion/integraciones/arca']);
           throw new Error('Sesión AFIP caducada o inválida. Por favor renueve el certificado.');
        }
        throw err;
      })
    );
  }


  private parseSoapResponse(xml: string, type: string): any[] {
    const parser = new DOMParser();
    const doc = parser.parseFromString(xml, 'text/xml');
    const result: any[] = [];





    if (doc.getElementsByTagName('soap:Fault').length > 0 || doc.getElementsByTagName('faultcode').length > 0) {
        console.error('SOAP Fault detected in response');
        return [];
    }

    let tagName = '';
    let idTag = 'Id';
    let descTag = 'Desc';
    let parentTag = 'ResultGet';




    switch (type) {
      case 'ptos-venta':
        tagName = 'PtoVenta';
        idTag = 'Nro';
        descTag = 'EmisionTipo';
        break;
      case 'cbte-tipos':
        tagName = 'CbteTipo';
        break;
      case 'doc-tipos':
        tagName = 'DocTipo';
        break;
      case 'iva':
        tagName = 'IvaTipo';
        break;
      case 'monedas':
        tagName = 'Moneda';
        break;
      case 'tributos':
        tagName = 'TributoTipo';
        break;
      case 'condicion-iva':
        tagName = 'CondicionIvaReceptor';
        break;
    }


    const elements = doc.getElementsByTagName(tagName);


    if (elements.length === 0) {


    }

    for (let i = 0; i < elements.length; i++) {
      const el = elements[i];


      const getTagVal = (parent: Element, tag: string) => {
          const child = parent.getElementsByTagName(tag)[0];
          return child ? child.textContent : null;
      };

      const idVal = getTagVal(el, idTag);
      const descVal = getTagVal(el, descTag);

      if (idVal) {
        const item: any = {
          id: isNaN(Number(idVal)) ? idVal : Number(idVal),
          description: descVal || idVal
        };


        if (type === 'ptos-venta') {
            const blocked = getTagVal(el, 'Bloqueado');
            if (blocked === 'S') continue;

            item.description = `Punto de Venta ${item.id} (${item.description})`;
        }

        result.push(item);
      }
    }

    return result;
  }


  emitInvoice(invoiceData: any): Observable<InvoiceResponse> {
    return this.http.post<InvoiceResponse>(`${this.apiUrl}/authorize`, invoiceData, {
      headers: this.getHeaders()
    }).pipe(
      catchError(err => {
        if (err.status === 500 && (err.error?.includes?.('No hay un token válido') || err.error?.includes?.('decrypt'))) {
          this.router.navigate(['/configuracion/integraciones/arca']);
          throw new Error('Sesión AFIP caducada o inválida. Por favor renueve el certificado.');
        }
        throw err;
      })
    );
  }



  getInvoices(from: string, to: string, cbteTipo?: number, resultado?: string): Observable<InvoiceIndex[]> {
    let params = `from=${from}&to=${to}`;
    if (cbteTipo != null) params += `&cbteTipo=${cbteTipo}`;
    if (resultado) params += `&resultado=${resultado}`;
    return this.http.get<InvoiceIndex[]>(`${this.apiUrl}/invoices?${params}`, {
      headers: this.getHeaders()
    });
  }

  getInvoiceById(id: number): Observable<InvoiceIndex> {
    return this.http.get<InvoiceIndex>(`${this.apiUrl}/invoices/${id}`, {
      headers: this.getHeaders()
    });
  }

  getInvoiceDetail(id: number): Observable<InvoiceDetailResponse> {
    return this.http.get<InvoiceDetailResponse>(`${this.apiUrl}/invoices/${id}/detail`, {
      headers: this.getHeaders()
    });
  }

  exportInvoices(from: string, to: string, cbteTipo?: number, resultado?: string): Observable<InvoiceIndex[]> {
    let params = `from=${from}&to=${to}`;
    if (cbteTipo != null) params += `&cbteTipo=${cbteTipo}`;
    if (resultado) params += `&resultado=${resultado}`;
    return this.http.get<InvoiceIndex[]>(`${this.apiUrl}/invoices/export?${params}`, {
      headers: this.getHeaders()
    });
  }
}
