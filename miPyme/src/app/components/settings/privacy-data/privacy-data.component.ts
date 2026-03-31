import { Component } from '@angular/core';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../services/auth.service';
import { CompanyService } from '../../../services/company.service';
import { NotificationService } from '../../../services/notification.service';

@Component({
  selector: 'app-privacy-data',
  templateUrl: './privacy-data.component.html',
  styleUrls: ['./privacy-data.component.css']
})
export class PrivacyDataComponent {

  isExporting = false;
  exportSuccess = false;
  exportError: string | null = null;

  constructor(
    private authService: AuthService,
    private companyService: CompanyService,
    private notificationService: NotificationService
  ) {}

  exportData(): void {
    this.isExporting = true;
    this.exportSuccess = false;
    this.exportError = null;

    forkJoin({
      profile:      this.authService.getProfile().pipe(catchError(() => of(null))),
      company:      this.companyService.getMyCompany().pipe(catchError(() => of(null))),
      sessions:     this.authService.getActiveSessions().pipe(catchError(() => of([]))),
      accessLogs:   this.authService.getAccessLogs().pipe(catchError(() => of([]))),
      notifications: this.notificationService.getAll().pipe(catchError(() => of([])))
    }).subscribe({
      next: ({ profile, company, sessions, accessLogs, notifications }) => {
        const preferencias: Record<string, any> = {};
        try {
          const themeRaw = localStorage.getItem('theme');
          if (themeRaw !== null) preferencias['tema'] = themeRaw;
          const langRaw = localStorage.getItem('lang');
          if (langRaw !== null) preferencias['idioma'] = langRaw;
          const loginRemember = localStorage.getItem('loginRemember');
          if (loginRemember !== null) preferencias['recordarSesion'] = loginRemember === 'true';
        } catch { }

        const exportPayload = {
          _meta: {
            generadoEn: new Date().toISOString(),
            nota: 'Este archivo contiene todos los datos personales y de actividad almacenados por MiPyme asociados a tu cuenta, en ejercicio del derecho a la transparencia y portabilidad de datos.'
          },
          perfil: profile ? {
            nombre: profile.name ?? null,
            email: profile.email ?? null,
            rol: this.authService.getRole(),
            permisos: this.authService.getPermissions()
          } : null,
          empresa: company ? {
            razonSocial:     (company as any).businessName ?? null,
            nombreComercial: (company as any).tradeName ?? null,
            cuit:            (company as any).cuit ?? null,
            pais:            (company as any).country ?? null,
            provincia:       (company as any).province ?? null,
            ciudad:          (company as any).city ?? null,
            rubro:           (company as any).industry ?? null,
            emailEmpresa:    (company as any).companyEmail ?? null,
            telefono:        (company as any).phone ?? null,
            domicilioFiscal: (company as any).fiscalAddress ?? null,
            codigoSSO:       (company as any).ssoCode ?? null
          } : null,
          sesionesActivas: Array.isArray(sessions) ? (sessions as any[]).map(s => ({
            id:               s.id,
            dispositivo:      s.deviceInfo ?? null,
            ip:               s.ipAddress ?? null,
            inicio:           s.createdAt ?? null,
            ultimaActividad:  s.lastActivity ?? null
          })) : [],
          historialAccesos: Array.isArray(accessLogs) ? (accessLogs as any[]).map(l => ({
            fecha:      l.accessedAt ?? l.timestamp ?? null,
            ip:         l.ipAddress ?? null,
            dispositivo: l.deviceInfo ?? null,
            exitoso:    l.success ?? null
          })) : [],
          notificaciones: Array.isArray(notifications) ? (notifications as any[]).map(n => ({
            id:      n.id,
            tipo:    n.type,
            mensaje: n.message ?? null,
            leida:   n.read,
            fecha:   n.createdAt ?? null
          })) : [],
          preferencias
        };

        const json = JSON.stringify(exportPayload, null, 2);
        const blob = new Blob([json], { type: 'application/json' });
        const url = URL.createObjectURL(blob);
        const link = document.createElement('a');
        link.href = url;
        const date = new Date().toISOString().slice(0, 10);
        link.download = `mipyme-datos-${date}.json`;
        link.click();
        URL.revokeObjectURL(url);

        this.isExporting = false;
        this.exportSuccess = true;
        setTimeout(() => this.exportSuccess = false, 5000);
      },
      error: (err) => {
        console.error('Error al exportar datos:', err);
        this.isExporting = false;
        this.exportError = 'No se pudo exportar los datos. Por favor intentá de nuevo más tarde.';
        setTimeout(() => this.exportError = null, 6000);
      }
    });
  }
}
