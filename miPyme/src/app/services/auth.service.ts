import { Injectable } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { Observable } from 'rxjs';
import { environment } from '../../environments/environment';
import { AccessLog, ActiveSession } from '../models/security.models';

@Injectable({
  providedIn: 'root'
})
export class AuthService {
  private apiUrl = `${environment.apiUrl}/api/auth`;
  private companiesUrl = `${environment.apiUrl}/api/companies`;

  constructor(private http: HttpClient) { }

  login(ssoCode: string, username: string, password: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/login`, { ssoCode, username, password });
  }

  verifyTwoFactor(ssoCode: string, username: string, code: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/verify-2fa`, { ssoCode, username, code });
  }

  forgotPassword(ssoCode: string, email: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/forgot-password`, { ssoCode, email });
  }

  verifyRecoveryCode(ssoCode: string, email: string, code: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/verify-recovery-code`, { ssoCode, email, code });
  }

  resetPassword(ssoCode: string, email: string, code: string, newPassword: string): Observable<any> {
    return this.http.post(`${this.apiUrl}/reset-password`, { ssoCode, email, code, newPassword });
  }

  register(data: any): Observable<any> {
    return this.http.post(`${this.companiesUrl}/register`, data);
  }

  getAvailableSso(base: string): Observable<{ssoCode: string}> {
    return this.http.get<{ssoCode: string}>(`${this.companiesUrl}/sso/next-available?base=${base}`);
  }

  searchLocations(query: string): Observable<any[]> {

    const url = `https://nominatim.openstreetmap.org/search?format=json&q=${encodeURIComponent(query)}&addressdetails=1&limit=5`;
    return this.http.get<any[]>(url);
  }

  getAccessLogs(): Observable<AccessLog[]> {
    return this.http.get<AccessLog[]>(`${environment.apiUrl}/api/me/access-logs`);
  }

  getActiveSessions(): Observable<ActiveSession[]> {
    return this.http.get<ActiveSession[]>(`${environment.apiUrl}/api/me/sessions`);
  }

  revokeSession(sessionId: number): Observable<void> {
    return this.http.delete<void>(`${environment.apiUrl}/api/me/sessions/${sessionId}`);
  }

  revokeOtherSessions(): Observable<void> {
    return this.http.post<void>(`${environment.apiUrl}/api/me/sessions/revoke-others`, {});
  }

  isLoggedIn(): boolean {
    const token = this.getToken();
    if (!token) {
      return false;
    }
    return !this.isTokenExpired(token);
  }

  getToken(): string | null {
    return localStorage.getItem('token');
  }

  getRefreshToken(): string | null {
    return localStorage.getItem('refreshToken');
  }

  isTokenExpired(token: string): boolean {
    try {
      const payload = this.getDecodedToken(token);
      if (payload?.exp) {
        return Date.now() >= payload.exp * 1000;
      }
      return false;
    } catch (e) {
      return true;
    }
  }


  getDecodedToken(rawToken?: string): any {
    const token = rawToken ?? this.getToken();
    if (!token) return null;
    try {
      const base64Url = token.split('.')[1];
      const base64 = base64Url.replace(/-/g, '+').replace(/_/g, '/');
      return JSON.parse(atob(base64));
    } catch {
      return null;
    }
  }

  getCompanyName(): string | null {
    return localStorage.getItem('companyName');
  }

  getUserName(): string | null {
    return localStorage.getItem('userName');
  }

  saveTokens(token: string, refreshToken: string, companyName?: string, userName?: string, permissions?: string, theme?: string, planTier?: string, planStatus?: string): void {
    localStorage.setItem('token', token);
    localStorage.setItem('refreshToken', refreshToken);
    if (companyName) {
      localStorage.setItem('companyName', companyName);
    }
    if (userName) {
      localStorage.setItem('userName', userName);
    }

    if (theme && userName) {
      localStorage.setItem(`app_theme_${userName}`, theme);
    }



  }

  getRole(): string | null {
    const payload = this.getDecodedToken();
    if (!payload) return null;
    try {

      let role = payload.role || payload.roles || payload.authority || payload.authorities || null;

      if (!role) return null;


      if (Array.isArray(role)) {
        if (role.length === 0) return null;

        if (typeof role[0] === 'string') {
          return role[0].toString().toUpperCase();
        }

        if (typeof role[0] === 'object' && role[0].authority) {
          return role[0].authority.toString().toUpperCase();
        }

        return role[0].toString().toUpperCase();
      }

      return role.toString().toUpperCase();
    } catch (e) {
      console.error('Error parsing token role:', e);
      return null;
    }
  }

  getPermissions(): string[] {


    const payload = this.getDecodedToken();
    const permString: string | undefined = payload?.permissions;
    if (!permString) return [];
    if (permString === 'ALL') return ['ALL'];
    return permString.split(',').map((p: string) => p.trim().toLowerCase());
  }

  hasPermission(permission: string): boolean {
    if (permission === 'always') return true;

    const role = this.getRole();

    if (role === 'ADMIN' || role === 'ROLE_ADMIN') return true;

    const perms = this.getPermissions();
    if (perms.includes('ALL') || perms.includes('all')) return true;


    return perms.includes(permission.toLowerCase());
  }

  refreshToken(): Observable<any> {
    const refreshToken = localStorage.getItem('refreshToken');
    return this.http.post(`${this.apiUrl}/refresh`, { refreshToken });
  }

  logout(): void {
    const refreshToken = localStorage.getItem('refreshToken');
    if (refreshToken) {

      this.http.post(`${this.apiUrl}/logout`, { refreshToken }).subscribe({
        next: () => console.log('Session invalidated'),
        error: (err) => console.warn('Could not invalidate session', err)
      });
    }

    localStorage.removeItem('token');
    localStorage.removeItem('refreshToken');
    localStorage.removeItem('companyName');
    localStorage.removeItem('userName');

    localStorage.removeItem('permissions');
    localStorage.removeItem('currentPlan');
    localStorage.removeItem('currentPlanStatus');
  }

  getProfile(): Observable<any> {
    return this.http.get(`${environment.apiUrl}/api/me`);
  }

  updateProfile(data: { name?: string; email?: string }): Observable<any> {
    return this.http.put(`${environment.apiUrl}/api/me`, data);
  }

  changeEmail(data: { currentEmail: string; newEmail: string; password: string }): Observable<any> {
    return this.http.post(`${environment.apiUrl}/api/me/email`, data);
  }

  changePassword(data: any): Observable<any> {
    return this.http.post(`${environment.apiUrl}/api/me/password`, data);
  }

  updatePreferences(data: { theme?: string }): Observable<void> {
    return this.http.put<void>(`${environment.apiUrl}/api/me/preferences`, data);
  }

  toggleTwoFactor(enabled: boolean): Observable<boolean> {
    return this.http.post<boolean>(`${environment.apiUrl}/api/me/2fa`, { enabled });
  }

  getTwoFactorStatus(): Observable<boolean> {
    return this.http.get<boolean>(`${environment.apiUrl}/api/me/2fa`);
  }
}
