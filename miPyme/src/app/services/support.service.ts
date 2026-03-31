import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';

@Injectable({
  providedIn: 'root'
})
export class SupportService {
  private apiUrl = `${environment.apiUrl}/api/support`;

  constructor(private http: HttpClient) { }

  sendContactMessage(data: { name: string, email: string, subject: string, message: string }): Observable<any> {
    return this.http.post(`${this.apiUrl}/contact`, data);
  }
}
