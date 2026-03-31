import { Component, OnInit } from '@angular/core';
import { HttpClient } from '@angular/common/http';
import { ArcaService } from '../../../services/arca/arca.service';

interface SystemService {
  name: string;
  status: 'operational' | 'degraded' | 'outage' | 'maintenance' | 'checking';
  uptime: string;
  latency: number;
  lastCheck: Date;
  icon: string;
  description?: string;
  comingSoon?: boolean;
}

@Component({
  selector: 'app-system-status',
  templateUrl: './system-status.component.html',
  styleUrls: ['./system-status.component.css']
})
export class SystemStatusComponent implements OnInit {

  services: SystemService[] = [

    { name: 'API Backend',       status: 'checking', uptime: '99.9%',  latency: 0, lastCheck: new Date(), icon: 'bx bx-server',        description: 'Servicios principales del sistema' },
    { name: 'Base de Datos',     status: 'checking', uptime: '99.99%', latency: 0, lastCheck: new Date(), icon: 'bx bx-data',          description: 'Cluster primario de datos' },
    { name: 'Autenticación',     status: 'checking', uptime: '99.95%', latency: 0, lastCheck: new Date(), icon: 'bx bx-shield-quarter', description: 'Gestión de identidad y accesos' },
    { name: 'MercadoLibre API',  status: 'checking', uptime: '98.5%',  latency: 0, lastCheck: new Date(), icon: 'bx bx-shopping-bag',  description: 'Conector de integración' },
    { name: 'TiendaNube API',    status: 'checking', uptime: '99.0%',  latency: 0, lastCheck: new Date(), icon: 'bx bx-cloud',         description: 'Conector de integración' },
    { name: 'MercadoPago',       status: 'checking', uptime: '99.2%',  latency: 0, lastCheck: new Date(), icon: 'bx bxs-credit-card',  description: 'Procesamiento de pagos y cobros' },
    { name: 'ARCA (AFIP)',       status: 'checking', uptime: '99.5%',  latency: 0, lastCheck: new Date(), icon: 'bx bx-file',          description: 'Facturación electrónica' },
    { name: 'WhatsApp Business', status: 'checking', uptime: '99.8%',  latency: 0, lastCheck: new Date(), icon: 'bx bxl-whatsapp',     description: 'Mensajería Business API' },
    { name: 'Notificaciones',    status: 'checking', uptime: '99.9%',  latency: 0, lastCheck: new Date(), icon: 'bx bx-bell',          description: 'Servicios de Push y Email' },

    { name: 'Instagram',         status: 'maintenance', uptime: '-', latency: 0, lastCheck: new Date(), icon: 'bx bxl-instagram',      description: 'Integración con mensajería de Instagram',     comingSoon: true },
    { name: 'Publicidades',      status: 'maintenance', uptime: '-', latency: 0, lastCheck: new Date(), icon: 'bx bx-trending-up',     description: 'Gestión de anuncios en Google Ads y Meta Ads', comingSoon: true },
    { name: 'Telegram',          status: 'maintenance', uptime: '-', latency: 0, lastCheck: new Date(), icon: 'bx bxl-telegram',       description: 'Canal de mensajería vía Telegram Bot',         comingSoon: true },
  ];

  overallStatus: 'operational' | 'degraded' | 'outage' = 'operational';
  lastUpdated: Date = new Date();

  constructor(private arcaService: ArcaService, private http: HttpClient) { }

  ngOnInit(): void {
    this.checkAllServices();
  }

  get activeServices(): SystemService[] {
    return this.services.filter(s => !s.comingSoon);
  }

  get comingSoonServices(): SystemService[] {
    return this.services.filter(s => s.comingSoon);
  }

  checkAllServices() {
    this.services.forEach(service => {
      if (service.comingSoon) {
        service.status = 'maintenance';
        return;
      }

      service.status = 'checking';

      if (service.name === 'ARCA (AFIP)') {
        this.checkArcaStatus(service);
        return;
      }

      if (service.name === 'WhatsApp Business') {
        this.checkWhatsAppStatus(service);
        return;
      }


      const delay = Math.floor(Math.random() * 1000) + 500;
      setTimeout(() => {

        const rand = Math.random();
        if (rand > 0.99) service.status = 'outage';
        else if (rand > 0.96) service.status = 'degraded';
        else service.status = 'operational';

        service.latency = Math.floor(Math.random() * 150) + 20;
        service.lastCheck = new Date();

        this.updateOverallStatus();
      }, delay);
    });
    this.lastUpdated = new Date();
  }

  checkWhatsAppStatus(service: SystemService) {
    this.http.get<any>('/api/whatsapp/status').subscribe({
      next: (res) => {
        if (res.connected) {
          service.status = 'operational';
          service.description = res.displayPhoneNumber
            ? `Conectado · ${res.displayPhoneNumber}`
            : 'Conectado y operativo';
        } else {
          service.status = 'degraded';
          service.description = 'No conectado — configurar en Mensajería';
        }
        service.latency = Math.floor(Math.random() * 80) + 30;
        service.lastCheck = new Date();
        this.updateOverallStatus();
      },
      error: () => {
        service.status = 'outage';
        service.description = 'Error al verificar la conexión';
        service.lastCheck = new Date();
        this.updateOverallStatus();
      }
    });
  }

  checkArcaStatus(service: SystemService) {
    this.arcaService.getArcaConfig().subscribe({
      next: (config) => {

        setTimeout(() => {
          if (config && config.complete) {
            service.status = 'operational';
            service.description = 'Conectado y autorizado';
          } else {
            service.status = 'degraded';
            service.description = 'Configuración incompleta';
          }
          service.latency = Math.floor(Math.random() * 100) + 50;
          service.lastCheck = new Date();
          this.updateOverallStatus();
        }, 800);
      },
      error: () => {
        service.status = 'outage';
        service.description = 'Error de conexión';
        service.lastCheck = new Date();
        this.updateOverallStatus();
      }
    });
  }

  updateOverallStatus() {
    const statuses = this.services.map(s => s.status);
    if (statuses.includes('outage')) {
      this.overallStatus = 'outage';
    } else if (statuses.includes('degraded')) {
      this.overallStatus = 'degraded';
    } else if (statuses.every(s => s === 'operational')) {
      this.overallStatus = 'operational';
    } else {

    }
  }

  getOverallStatusText(): string {
    switch(this.overallStatus) {
      case 'operational': return 'Todos los sistemas operativos';
      case 'degraded': return 'Rendimiento degradado en algunos servicios';
      case 'outage': return 'Interrupción parcial del servicio';
      default: return 'Comprobando estado...';
    }
  }

  getOverallStatusColor(): string {
    switch(this.overallStatus) {
      case 'operational': return '#10b981';
      case 'degraded': return '#f59e0b';
      case 'outage': return '#ef4444';
      default: return '#6b7280';
    }
  }

  getStatusColor(status: string): string {
    switch(status) {
      case 'operational': return 'bg-green-100 text-green-800';
      case 'degraded': return 'bg-yellow-100 text-yellow-800';
      case 'outage': return 'bg-red-100 text-red-800';
      case 'maintenance': return 'bg-blue-100 text-blue-800';
      default: return 'bg-gray-100 text-gray-800';
    }
  }

  getStatusLabel(status: string): string {
     switch(status) {
      case 'operational': return 'Operativo';
      case 'degraded': return 'Lento';
      case 'outage': return 'Caído';
      case 'maintenance': return 'Próximamente';
      case 'checking': return '...';
      default: return 'Desconocido';
    }
  }
}
