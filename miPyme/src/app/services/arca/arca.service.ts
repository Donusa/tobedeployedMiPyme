import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../../environments/environment';

export interface CsrResponse {
  csr: string;
  privateKey: string;
}

export interface ArcaConfig {
  id?: number;
  companyName?: string;
  cuit?: string;
  authToken?: string;
  tokenExpiration?: string;
  currentStep?: number;
  wizardCompleted?: boolean;
  complete?: boolean;
  environment?: 'HOMOLOGATION' | 'PRODUCTION';
}

@Injectable({
  providedIn: 'root'
})
export class ArcaService {
  private apiUrl = `${environment.apiUrl}/api/arca`;

  constructor(private http: HttpClient) { }

  getArcaConfig(): Observable<ArcaConfig> {
    return this.http.get<ArcaConfig>(`${this.apiUrl}/config`);
  }

  saveWizardState(currentStep: number, wizardCompleted: boolean): Observable<void> {
    return this.http.post<void>(`${this.apiUrl}/state`, { currentStep, wizardCompleted });
  }

  generateCsr(companyName: string, cuit: string): Observable<CsrResponse> {
    return this.http.post<CsrResponse>(`${this.apiUrl}/csr`, null, {
      params: { companyName, cuit }
    });
  }

  saveCertificate(certificate: string): Observable<void> {


    return this.http.post<void>(`${this.apiUrl}/certificate`, certificate);
  }

  downloadP12(password: string, certificateContent: string): Observable<Blob> {
    const body = { password, certificateContent };
    console.log('ArcaService: Downloading P12 with body', { password: '***', certificateContentLength: certificateContent?.length });

    return this.http.post(`${this.apiUrl}/p12`, body, {
      responseType: 'blob',
      headers: {
        'Content-Type': 'application/json'
      }
    });
  }

  activate(p12File: File, password: string): Observable<string> {
    const formData = new FormData();
    formData.append('file', p12File);
    formData.append('password', password);
    return this.http.post(`${this.apiUrl}/activate`, formData, { responseType: 'text' });
  }

  updateConfig(config: Partial<ArcaConfig>): Observable<ArcaConfig> {
    return this.http.put<ArcaConfig>(`${this.apiUrl}/config`, config);
  }

  deleteConfig(): Observable<void> {
    return this.http.delete<void>(`${this.apiUrl}/config`);
  }

  checkConnection(): Observable<string> {
    return this.http.get(`${this.apiUrl}/check-connection`, { responseType: 'text' });
  }
}
