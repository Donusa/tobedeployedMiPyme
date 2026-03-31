import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { ActiveSession } from '../../../models/security.models';

@Component({
  selector: 'app-active-sessions',
  templateUrl: './active-sessions.component.html',
  styleUrls: ['./active-sessions.component.css']
})
export class ActiveSessionsComponent implements OnInit {
  sessions: ActiveSession[] = [];
  isLoading = true;
  showConfirmModal = false;
  sessionToRevoke: ActiveSession | null = null;

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.loadSessions();
  }

  loadSessions() {
    this.isLoading = true;
    const currentRefreshToken = this.authService.getRefreshToken();
    this.authService.getActiveSessions().subscribe({
      next: (data: ActiveSession[]) => {
        this.sessions = data.map(session => ({
          ...session,
          isCurrent: session.token === currentRefreshToken
        }));
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error fetching sessions', err);
        this.isLoading = false;
      }
    });
  }

  revokeSession(session: ActiveSession) {
    this.sessionToRevoke = session;
    this.showConfirmModal = true;
  }

  closeModal() {
    this.showConfirmModal = false;
    this.sessionToRevoke = null;
  }

  confirmRevoke() {
    if (!this.sessionToRevoke) return;

    this.authService.revokeSession(this.sessionToRevoke.id).subscribe({
      next: () => {
        if (this.sessionToRevoke?.isCurrent) {
          this.authService.logout();
          window.location.reload();
        } else {
          this.loadSessions();
        }
        this.closeModal();
      },
      error: (err: any) => {
        console.error('Error revoking session', err);
        alert('No se pudo cerrar la sesión.');
        this.closeModal();
      }
    });
  }

  getDeviceIcon(device: string): string {
    if (!device) return 'bx-question-mark';
    const d = device.toLowerCase();
    if (d.includes('mobile')) return 'bx-mobile';
    if (d.includes('tablet')) return 'bx-tablet';
    if (d.includes('mac') || d.includes('pc') || d.includes('windows') || d.includes('linux')) return 'bx-laptop';
    return 'bx-desktop';
  }
}
