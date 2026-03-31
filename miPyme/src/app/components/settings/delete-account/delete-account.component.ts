import { Component } from '@angular/core';
import { Router } from '@angular/router';
import { forkJoin, of } from 'rxjs';
import { catchError } from 'rxjs/operators';
import { AuthService } from '../../../services/auth.service';
import { CompanyService } from '../../../services/company.service';
import { NotificationService } from '../../../services/notification.service';
import { HttpClient } from '@angular/common/http';
import { environment } from '../../../../environments/environment';


const BACKUP_RETENTION_DAYS = 60;

@Component({
  selector: 'app-delete-account',
  templateUrl: './delete-account.component.html',
  styleUrls: ['./delete-account.component.css']
})
export class DeleteAccountComponent {

  confirmText = '';
  currentPassword = '';
  showPassword = false;


  step: 'idle' | 'backing-up' | 'deleting' | 'success' = 'idle';

  deleteError: string | null = null;
  backupError: string | null = null;


  backupExpiresAt: Date | null = null;

  get isDeleting(): boolean { return this.step === 'deleting'; }
  get isBackingUp(): boolean { return this.step === 'backing-up'; }
  get deleteSuccess(): boolean { return this.step === 'success'; }
  get isBusy(): boolean { return this.step !== 'idle'; }

  constructor(
    private authService: AuthService,
    private companyService: CompanyService,
    private notificationService: NotificationService,
    private http: HttpClient,
    private router: Router
  ) {}

  requestDelete(): void {
    if (this.confirmText !== 'ELIMINAR' || !this.currentPassword) return;

    this.step = 'backing-up';
    this.deleteError = null;
    this.backupError = null;


    forkJoin({
      profile:       this.authService.getProfile().pipe(catchError(() => of(null))),
      company:       this.companyService.getMyCompany().pipe(catchError(() => of(null))),
      sessions:      this.authService.getActiveSessions().pipe(catchError(() => of([]))),
      accessLogs:    this.authService.getAccessLogs().pipe(catchError(() => of([]))),
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

        const now = new Date();
        const expiresAt = new Date(now);
        expiresAt.setDate(expiresAt.getDate() + BACKUP_RETENTION_DAYS);

        const exportPayload = {
          _meta: {
            generadoEn: now.toISOString(),
            tipo: 'respaldo-previo-a-eliminacion',
            vigenciaServidor: expiresAt.toISOString(),
            nota: `Respaldo generado automáticamente antes de la eliminación de la cuenta. Los datos se conservarán en el servidor por ${BACKUP_RETENTION_DAYS} días (hasta ${expiresAt.toLocaleDateString('es-AR')}), transcurrido ese plazo serán eliminados de forma definitiva.`
          },
          perfil: profile ? {
            nombre:   (profile as any).name  ?? null,
            email:    (profile as any).email ?? null,
            rol:      this.authService.getRole(),
            permisos: this.authService.getPermissions()
          } : null,
          empresa: company ? {
            razonSocial:     (company as any).businessName  ?? null,
            nombreComercial: (company as any).tradeName     ?? null,
            cuit:            (company as any).cuit          ?? null,
            pais:            (company as any).country       ?? null,
            provincia:       (company as any).province      ?? null,
            ciudad:          (company as any).city          ?? null,
            rubro:           (company as any).industry      ?? null,
            emailEmpresa:    (company as any).companyEmail  ?? null,
            telefono:        (company as any).phone         ?? null,
            domicilioFiscal: (company as any).fiscalAddress ?? null,
            codigoSSO:       (company as any).ssoCode       ?? null
          } : null,
          sesionesActivas: Array.isArray(sessions) ? (sessions as any[]).map(s => ({
            id:              s.id,
            dispositivo:     s.deviceInfo    ?? null,
            ip:              s.ipAddress     ?? null,
            inicio:          s.createdAt     ?? null,
            ultimaActividad: s.lastActivity  ?? null
          })) : [],
          historialAccesos: Array.isArray(accessLogs) ? (accessLogs as any[]).map(l => ({
            fecha:       l.accessedAt ?? l.timestamp ?? null,
            ip:          l.ipAddress  ?? null,
            dispositivo: l.deviceInfo ?? null,
            exitoso:     l.success    ?? null
          })) : [],
          notificaciones: Array.isArray(notifications) ? (notifications as any[]).map(n => ({
            id:      n.id,
            tipo:    n.type,
            mensaje: n.message  ?? null,
            leida:   n.read,
            fecha:   n.createdAt ?? null
          })) : [],
          preferencias
        };


        this.triggerLocalDownload(exportPayload, now);


        this.http.post(`${environment.apiUrl}/api/me/backup`, {
          retentionDays: BACKUP_RETENTION_DAYS,
          data: exportPayload
        }).pipe(catchError(() => of(null))).subscribe(() => {


          this.backupExpiresAt = expiresAt;
          this.performDeletion();
        });
      },
      error: () => {

        this.step = 'idle';
        this.backupError = 'No se pudo generar el respaldo previo. Intentá de nuevo más tarde.';
      }
    });
  }

  private triggerLocalDownload(payload: object, date: Date): void {
    try {
      const json = JSON.stringify(payload, null, 2);
      const blob = new Blob([json], { type: 'application/json' });
      const url  = URL.createObjectURL(blob);
      const link = document.createElement('a');
      link.href  = url;
      link.download = `mipyme-respaldo-${date.toISOString().slice(0, 10)}.json`;
      link.click();
      URL.revokeObjectURL(url);
    } catch {  }
  }

  private performDeletion(): void {
    this.step = 'deleting';
    this.deleteError = null;

    this.http.delete(`${environment.apiUrl}/api/me`, {
      body: { password: this.currentPassword }
    }).subscribe({
      next: () => {
        this.step = 'success';
        setTimeout(() => {
          this.authService.logout();
          this.router.navigate(['/login']);
        }, 6000);
      },
      error: (err) => {
        this.step = 'idle';
        if (err.status === 401 || err.status === 403) {
          this.deleteError = 'Contraseña incorrecta. Por favor verificá e intentá de nuevo.';
        } else {
          this.deleteError = 'No se pudo procesar la solicitud. Intentá de nuevo más tarde.';
        }
      }
    });
  }
}
