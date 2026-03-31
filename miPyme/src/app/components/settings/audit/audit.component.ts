import { Component, OnInit } from '@angular/core';
import { AuthService } from '../../../services/auth.service';
import { AccessLog } from '../../../models/security.models';
import { LayoutService } from '../../../services/layout.service';
import { MobileCardColumn } from '../../../shared/components/mobile-card-list/mobile-card-list.models';

@Component({
  selector: 'app-audit',
  templateUrl: './audit.component.html',
  styleUrls: ['./audit.component.css']
})
export class AuditComponent implements OnInit {

  logs: AccessLog[] = [];
  filtered: AccessLog[] = [];
  paginated: AccessLog[] = [];

  isLoading = true;


  searchTerm = '';
  mediumFilter = '';
  availableMediums: string[] = [];


  sortField: keyof AccessLog = 'timestamp';
  sortAsc = false;


  pageSize = 10;
  currentPage = 1;
  totalPages = 1;


  totalLogs = 0;
  successCount = 0;
  uniqueDevices = 0;
  uniqueIps = 0;


  readonly mobileColumns: MobileCardColumn[] = [
    {
      field: 'timestamp', label: 'Fecha y hora', isPrimary: true,
      format: (v: any) => v ? new Date(v).toLocaleString('es-AR', { day: '2-digit', month: 'short', year: 'numeric', hour: '2-digit', minute: '2-digit' }) : '—'
    },
    { field: 'username',  label: 'Usuario' },
    {
      field: 'medium', label: 'Medio', isBadge: true,
      badgeClass: (log: AccessLog) => this.getMediumClass(log.medium)
    },
    { field: 'device',    label: 'Dispositivo', expandOnly: true },
    { field: 'ipAddress', label: 'IP',           expandOnly: true },
    { field: 'location',  label: 'Ubicación',    expandOnly: true }
  ];


  constructor(private authService: AuthService, public layoutService: LayoutService) {}

  ngOnInit(): void {
    this.loadLogs();
  }

  loadLogs(): void {
    this.isLoading = true;
    this.authService.getAccessLogs().subscribe({
      next: (data: AccessLog[]) => {
        this.logs = data;
        this.computeStats();
        this.extractFilters();
        this.applyFilters();
        this.isLoading = false;
      },
      error: () => {
        this.isLoading = false;
      }
    });
  }

  computeStats(): void {
    this.totalLogs = this.logs.length;

    const thirtyDaysAgo = new Date();
    thirtyDaysAgo.setDate(thirtyDaysAgo.getDate() - 30);
    this.successCount = this.logs.filter(l =>
      l.timestamp && new Date(l.timestamp) >= thirtyDaysAgo
    ).length;
    this.uniqueDevices = new Set(this.logs.map(l => l.device).filter(Boolean)).size;
    this.uniqueIps = new Set(this.logs.map(l => l.ipAddress).filter(Boolean)).size;
  }

  extractFilters(): void {
    this.availableMediums = [...new Set(this.logs.map(l => l.medium).filter(Boolean))];
  }

  applyFilters(): void {
    let result = [...this.logs];

    if (this.searchTerm.trim()) {
      const term = this.searchTerm.toLowerCase();
      result = result.filter(l =>
        (l.device?.toLowerCase().includes(term)) ||
        (l.ipAddress?.toLowerCase().includes(term)) ||
        (l.location?.toLowerCase().includes(term)) ||
        (l.username?.toLowerCase().includes(term))
      );
    }

    if (this.mediumFilter) {
      result = result.filter(l => l.medium === this.mediumFilter);
    }


    result.sort((a, b) => {
      const av = (a as any)[this.sortField] ?? '';
      const bv = (b as any)[this.sortField] ?? '';
      const cmp = av < bv ? -1 : av > bv ? 1 : 0;
      return this.sortAsc ? cmp : -cmp;
    });

    this.filtered = result;
    this.totalPages = Math.max(1, Math.ceil(result.length / this.pageSize));
    this.currentPage = 1;
    this.updatePage();
  }

  sortBy(field: keyof AccessLog): void {
    if (this.sortField === field) {
      this.sortAsc = !this.sortAsc;
    } else {
      this.sortField = field;
      this.sortAsc = false;
    }
    this.applyFilters();
  }

  getSortIcon(field: string): string {
    if (this.sortField !== field) return 'bx-sort';
    return this.sortAsc ? 'bx-sort-up' : 'bx-sort-down';
  }

  goToPage(page: number): void {
    if (page < 1 || page > this.totalPages) return;
    this.currentPage = page;
    this.updatePage();
  }

  updatePage(): void {
    const start = (this.currentPage - 1) * this.pageSize;
    this.paginated = this.filtered.slice(start, start + this.pageSize);
  }

  getDeviceIcon(device: string): string {
    if (!device) return 'bx-question-mark';
    const d = device.toLowerCase();
    if (d.includes('mobile') || d.includes('android') || d.includes('iphone')) return 'bx-mobile';
    if (d.includes('tablet') || d.includes('ipad')) return 'bx-tab';
    if (d.includes('mac') || d.includes('windows') || d.includes('linux')) return 'bx-laptop';
    return 'bx-desktop';
  }

  getMediumClass(medium: string): string {
    if (!medium) return 'medium-default';
    const m = medium.toLowerCase();
    if (m.includes('web') || m.includes('browser')) return 'medium-web';
    if (m.includes('mobile') || m.includes('app')) return 'medium-mobile';
    if (m.includes('api')) return 'medium-api';
    return 'medium-default';
  }
}
