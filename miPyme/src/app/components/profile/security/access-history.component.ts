import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { AccessLog } from '../../../models/security.models';

@Component({
  selector: 'app-access-history',
  templateUrl: './access-history.component.html',
  styleUrls: ['./access-history.component.css']
})
export class AccessHistoryComponent implements OnInit {
  logs: AccessLog[] = [];
  isLoading = true;

  constructor(private authService: AuthService) { }

  ngOnInit(): void {
    this.authService.getAccessLogs().subscribe({
      next: (data: AccessLog[]) => {
        this.logs = data;
        this.isLoading = false;
      },
      error: (err: any) => {
        console.error('Error fetching access logs', err);
        this.isLoading = false;
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
